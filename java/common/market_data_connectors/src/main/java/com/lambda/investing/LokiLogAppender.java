package com.lambda.investing;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.ThresholdFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
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
 *
 * <p><strong>Declarative (log4j2.xml) usage</strong> — recommended:<br>
 * Add {@code packages="com.lambda.investing"} to the {@code <configuration>} element and declare
 * the appender in the {@code <appenders>} section using the two separate env-var properties:
 * <pre>{@code
 * <LokiAppender name="loki"
 *               host="${env:LOKI_HOST:-}"
 *               port="${env:LOKI_PORT:-3100}"
 *               appName="${sys:log.appName:-hft-framework}"/>
 * }</pre>
 * The appender is a no-op when {@code LOKI_HOST} is not set, so it is safe to include in the XML
 * unconditionally; logging still works without Loki present.
 *
 * <p><strong>Programmatic fallback</strong>:<br>
 * Call {@link #initializeLoki()} once at application startup. It reads {@code LOKI_HOST} +
 * {@code LOKI_PORT} (or the legacy {@code LOKI_URL}) and registers itself on the root logger if
 * Loki has not already been configured via XML. It is idempotent and safe to call multiple times.
 *
 * <p>Log events are enqueued and forwarded by a dedicated daemon thread every
 * {@value #FLUSH_INTERVAL_MS} ms or whenever {@value #BATCH_SIZE} events accumulate, whichever
 * comes first.
 */
@Plugin(name = "LokiAppender", category = "Core", elementType = "appender", printObject = true)
public class LokiLogAppender extends AbstractAppender {

    // ── env-var names ────────────────────────────────────────────────────────
    /** Default appender name used in both XML config and programmatic duplicate-detection. */
    static final String DEFAULT_APPENDER_NAME = "loki";

    static final int DEFAULT_LOKI_PORT = 3100;
    static final int BATCH_SIZE = 100;
    static final long FLUSH_INTERVAL_MS = 1_000L;
    static final int MAX_QUEUE_SIZE = 10_000;
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 3_000;

    private static volatile boolean initialized = false;

    // ── instance fields ──────────────────────────────────────────────────────
    private final String lokiPushUrl;
    private final String appName;
    private final BlockingQueue<LogEvent> queue;
    private final AtomicBoolean running;
    /** Guarantees strictly-increasing nanosecond timestamps required by Loki. */
    private final AtomicLong lastNanos = new AtomicLong(0);
    private Thread flushThread;
    private final boolean enabled;

    // ── Log4j2 plugin factory (called from log4j2.xml) ───────────────────────

    /**
     * Log4j2 plugin factory — called when the framework reads a {@code <LokiAppender>} element
     * from {@code log4j2.xml}.  The {@code host} and {@code port} values are resolved by Log4j2
     * from the environment before this method is called, so an empty {@code host} means
     * {@code LOKI_HOST} was not set and the appender should be disabled (no-op).
     */
    @PluginFactory
    public static LokiLogAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("host") String host,
            @PluginAttribute(value = "port", defaultInt = DEFAULT_LOKI_PORT) int port,
            @PluginAttribute("appName") String appName) {

        if (name == null || name.isEmpty()) {
            name = DEFAULT_APPENDER_NAME;
        }

        boolean active = host != null && !host.isEmpty();
        if (active && !isReachable(host, port)) {
            System.err.println("[LokiLogAppender] WARNING: Loki host " + host + ":" + port
                    + " is not reachable — appender disabled.");
            active = false;
        }
        String lokiUrl = active ? "http://" + host + ":" + port : null;
        String resolvedAppName = (appName != null && !appName.isEmpty()) ? appName : "hft-framework";

        LokiLogAppender appender = new LokiLogAppender(name, lokiUrl, resolvedAppName, active);
        if (active) {
            // Mark as initialized so initializeLoki() won't add a duplicate programmatic appender
            initialized = true;
            System.out.println("Loki log appender configured via log4j2.xml → "
                    + appender.lokiPushUrl + " (app=" + resolvedAppName + ")");
        }
        return appender;
    }

    // ── construction ─────────────────────────────────────────────────────────

    private LokiLogAppender(String name, String lokiUrl, String appName, boolean enabled) {
        super(
                name,
                enabled
                        ? ThresholdFilter.createFilter(Level.INFO, Filter.Result.ACCEPT, Filter.Result.DENY)
                        : ThresholdFilter.createFilter(Level.OFF, Filter.Result.DENY, Filter.Result.DENY),
                null,
                true,
                Property.EMPTY_ARRAY
        );
        this.lokiPushUrl = (lokiUrl != null) ? lokiUrl.replaceAll("/+$", "") + "/loki/api/v1/push" : null;
        this.appName = (appName != null && !appName.isEmpty()) ? appName : "hft-framework";
        this.enabled = enabled;

        if (enabled) {
            this.queue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
            this.running = new AtomicBoolean(true);
        } else {
            this.queue = null;
            this.running = null;
        }
    }

    // ── AbstractAppender lifecycle ────────────────────────────────────────────

    @Override
    public void start() {
        super.start();
        if (enabled && flushThread == null) {
            flushThread = new Thread(this::flushLoop, "loki-log-flush");
            flushThread.setDaemon(true);
            flushThread.start();
        }
    }

    @Override
    public void stop() {
        if (running != null) {
            running.set(false);
        }
        super.stop();
    }

    // ── public API ───────────────────────────────────────────────────────────

    /**
     * Reads {@code LOKI_HOST} + {@code LOKI_PORT} (or the legacy {@code LOKI_URL}) and, when set,
     * registers this appender programmatically on the Log4j2 root logger at INFO level.
     *
     * <p>If Loki was already configured via {@code log4j2.xml} (detected by checking for an
     * existing appender named {@code "loki"}), this method is a no-op to prevent duplicate
     * registration. Safe to call multiple times.
     */
    public static synchronized void initializeLoki() {
        if (initialized) {
            return;
        }

        if (Configuration.LOKI_PORT == null || Configuration.LOKI_PORT.isEmpty()) {
            return;
        }
        String lokiUrl = "http://" + Configuration.LOKI_HOST + ":" + Configuration.LOKI_PORT;

        // Check connectivity before registering
        try {
            int port = Integer.parseInt(Configuration.LOKI_PORT.trim());
            if (!isReachable(Configuration.LOKI_HOST, port)) {
                System.err.println("[LokiLogAppender] WARNING: Loki host "
                        + Configuration.LOKI_HOST + ":" + port
                        + " is not reachable — skipping registration.");
                return;
            }
        } catch (NumberFormatException e) {
            System.err.println("[LokiLogAppender] WARNING: Invalid LOKI_PORT value '"
                    + Configuration.LOKI_PORT + "' — skipping registration.");
            return;
        }

        String appName = Configuration.LOG_APP_NAME;

        // If log4j2.xml already declared a LokiAppender, skip programmatic registration
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();
        if (config.getAppenders().containsKey(DEFAULT_APPENDER_NAME)) {
            initialized = true;
            return;
        }

        // Programmatic registration on root logger
        LokiLogAppender appender = new LokiLogAppender(DEFAULT_APPENDER_NAME, lokiUrl, appName, true);
        appender.start();
        config.addAppender(appender);
        config.getRootLogger().addAppender(appender, Level.INFO, null);
        ctx.updateLoggers();

        initialized = true;
        System.out.println("Loki log appender configured (programmatic) → "
                + appender.lokiPushUrl + " (app=" + appName + ")");
    }

    // ── AbstractAppender ─────────────────────────────────────────────────────

    @Override
    public void append(LogEvent event) {
        if (!enabled || queue == null) {
            return;
        }
        // toImmutable() is required: Log4j2 reuses event objects after the call returns
        queue.offer(event.toImmutable());
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

    /**
     * Returns {@code true} when a TCP connection to {@code host:port} can be established within 2 s.
     */
    private static boolean isReachable(String host, int port) {
        try (Socket s = new Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), 2_000);
            return true;
        } catch (IOException e) {
            return false;
        }
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
