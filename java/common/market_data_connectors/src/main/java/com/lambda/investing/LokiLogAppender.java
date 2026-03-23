package com.lambda.investing;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.filter.ThresholdFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Log4j2 appender that ships log entries to a Loki push API endpoint in batches.
 * <p>
 * Activated by setting the {@code LOKI_URL} environment variable (or Java system property)
 * to the base URL of the Loki instance, e.g. {@code http://localhost:3100}.
 * The appender POSTs to {@code <LOKI_URL>/loki/api/v1/push} using Loki's native JSON format.
 * </p>
 * <p>
 * Activation is programmatic — no changes to existing {@code log4j2.xml} files are required.
 * Call {@link #initializeLoki()} once at application startup (it is idempotent).
 * </p>
 * <p>
 * Log events are enqueued and forwarded by a dedicated daemon thread every
 * {@value #FLUSH_INTERVAL_MS} ms or whenever {@value #BATCH_SIZE} events accumulate,
 * whichever comes first. This keeps the hot logging path allocation-free and non-blocking.
 * </p>
 */
public class LokiLogAppender extends AbstractAppender {

    private static final String LOKI_URL_ENV = "LOKI_URL";
    private static final String APP_NAME_ENV = "APP_NAME";

    static final int BATCH_SIZE = 100;
    static final long FLUSH_INTERVAL_MS = 1_000L;
    static final int MAX_QUEUE_SIZE = 10_000;
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 3_000;

    private static volatile boolean initialized = false;

    private final String lokiPushUrl;
    private final String appName;
    private final BlockingQueue<LogEvent> queue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final AtomicBoolean running = new AtomicBoolean(true);
    /** Guarantees strictly-increasing nanosecond timestamps required by Loki. */
    private final AtomicLong lastNanos = new AtomicLong(0);
    private final Thread flushThread;

    // ── construction ────────────────────────────────────────────────────────

    private LokiLogAppender(String lokiBaseUrl, String appName) {
        super(
                "LokiAppender",
                ThresholdFilter.createFilter(Level.INFO, Filter.Result.ACCEPT, Filter.Result.DENY),
                null,
                true,
                Property.EMPTY_ARRAY
        );
        this.lokiPushUrl = lokiBaseUrl.replaceAll("/+$", "") + "/loki/api/v1/push";
        this.appName = (appName != null && !appName.isEmpty()) ? appName : "hft-framework";
        this.flushThread = new Thread(this::flushLoop, "loki-log-flush");
        this.flushThread.setDaemon(true);
    }

    // ── public API ───────────────────────────────────────────────────────────

    /**
     * Reads {@code LOKI_URL} (env var or system property) and, when set, registers this
     * appender on the Log4j2 root logger at INFO level. Safe to call multiple times.
     */
    public static synchronized void initializeLoki() {
        if (initialized) {
            return;
        }

        String lokiUrl = Configuration.getEnvOrDefault(LOKI_URL_ENV, "");
        if (lokiUrl == null || lokiUrl.isEmpty()) {
            return;
        }

        String appName = Configuration.getEnvOrDefault(APP_NAME_ENV,
                System.getProperty("log.appName", "hft-framework"));

        LokiLogAppender appender = new LokiLogAppender(lokiUrl, appName);
        appender.start();
        appender.flushThread.start();

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();
        config.addAppender(appender);
        config.getRootLogger().addAppender(appender, Level.INFO, null);
        ctx.updateLoggers();

        initialized = true;
        // Use stdout to avoid recursive logging through this very appender
        System.out.println("Loki log appender configured → " + appender.lokiPushUrl
                + " (app=" + appender.appName + ")");
    }

    // ── AbstractAppender ─────────────────────────────────────────────────────

    @Override
    public void append(LogEvent event) {
        // toImmutable() is required: Log4j2 reuses event objects after the call returns
        queue.offer(event.toImmutable());
    }

    @Override
    public void stop() {
        running.set(false);
        super.stop();
    }

    // ── background flush thread ──────────────────────────────────────────────

    private void flushLoop() {
        List<LogEvent> batch = new ArrayList<>(BATCH_SIZE);
        while (running.get() || !queue.isEmpty()) {
            try {
                LogEvent head = queue.poll(FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
                if (head != null) {
                    batch.add(head);
                    queue.drainTo(batch, BATCH_SIZE - 1);
                }
                if (!batch.isEmpty()) {
                    sendBatch(batch);
                    batch.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Swallow to prevent log storms; print type for debugging without recursing into logging
                System.err.println("[LokiLogAppender] flush error: " + e.getClass().getName() + ": " + e.getMessage());
                batch.clear();
            }
        }
    }

    // ── HTTP push ────────────────────────────────────────────────────────────

    private void sendBatch(List<LogEvent> events) {
        byte[] body = buildLokiJson(events).getBytes(StandardCharsets.UTF_8);
        try {
            URL url = new URL(lokiPushUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            int code = conn.getResponseCode();
            if (code >= 400) {
                System.err.println("[LokiLogAppender] push failed: HTTP " + code
                        + " — " + lokiPushUrl);
            }
            conn.disconnect();
        } catch (IOException e) {
            System.err.println("[LokiLogAppender] push error: " + e.getMessage());
        }
    }

    // ── JSON builder ─────────────────────────────────────────────────────────

    /**
     * Builds the Loki push API JSON body.
     * Events are grouped by log level so that Grafana/LogQL can filter by
     * the {@code level} stream label without scanning log content.
     */
    String buildLokiJson(List<LogEvent> events) {
        // Group by level to create separate Loki streams per level
        Map<String, List<LogEvent>> byLevel = new LinkedHashMap<>();
        for (LogEvent e : events) {
            byLevel.computeIfAbsent(e.getLevel().name(), k -> new ArrayList<>()).add(e);
        }

        StringBuilder sb = new StringBuilder("{\"streams\":[");
        boolean firstStream = true;
        for (Map.Entry<String, List<LogEvent>> entry : byLevel.entrySet()) {
            if (!firstStream) {
                sb.append(",");
            }
            firstStream = false;
            sb.append("{\"stream\":{\"app\":\"").append(escapeJson(appName))
              .append("\",\"level\":\"").append(escapeJson(entry.getKey()))
              .append("\"},\"values\":[");

            boolean first = true;
            for (LogEvent e : entry.getValue()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                long nanos = uniqueNanos(e);
                sb.append("[\"").append(nanos).append("\",\"")
                  .append(escapeJson(formatMessage(e))).append("\"]");
            }
            sb.append("]}");
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * Returns a nanosecond timestamp guaranteed to be strictly greater than any
     * previously returned value, as required by Loki.
     */
    private long uniqueNanos(LogEvent e) {
        long eventNanos = e.getInstant().getEpochSecond() * 1_000_000_000L
                + e.getInstant().getNanoOfSecond();
        return lastNanos.updateAndGet(prev -> Math.max(prev + 1, eventNanos));
    }

    private static String formatMessage(LogEvent e) {
        String loggerName = e.getLoggerName();
        int dot = loggerName.lastIndexOf('.');
        String shortLogger = dot >= 0 ? loggerName.substring(dot + 1) : loggerName;

        StringBuilder sb = new StringBuilder();
        sb.append("[").append(shortLogger).append("] ")
          .append(e.getMessage().getFormattedMessage());
        if (e.getThrown() != null) {
            sb.append(" | ").append(formatThrowable(e.getThrown()));
        }
        return sb.toString();
    }

    /** Formats throwable as "ClassName: message at TopFrame" — compact but useful for log search. */
    private static String formatThrowable(Throwable t) {
        StringBuilder sb = new StringBuilder(t.toString());
        StackTraceElement[] frames = t.getStackTrace();
        if (frames != null && frames.length > 0) {
            sb.append(" at ").append(frames[0]);
        }
        if (t.getCause() != null) {
            sb.append(" caused by ").append(t.getCause().toString());
        }
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
