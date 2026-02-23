package com.lambda.investing.algorithmic_trading.observer.pushbullet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambda.investing.algorithmic_trading.Algorithm;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PushbulletMessageReader monitors incoming Pushbullet messages and triggers actions based on message content.
 * When a message with "kill" is received, it will call the algorithm's stop() method (kill switch).
 * <p>
 * This class implements the Listener Pattern, allowing multiple listeners to be registered to receive
 * Pushbullet messages. Each listener implementing {@link PushbulletMessageListener} will receive the
 * title and body of incoming messages.
 * <p>
 * Example usage:
 * <pre>{@code
 * String pushbulletToken = "your-pushbullet-token";
 * PushbulletMessageReader reader = new PushbulletMessageReader(algorithm, pushbulletToken);
 *
 * // Register custom listeners
 * reader.registerListener(new MyCustomListener());
 *
 * reader.start(); // Start monitoring for messages
 * // ... algorithm runs ...
 * reader.stop(); // Stop monitoring when done
 * }</pre>
 */
public class PushbulletMessageReader {

    protected Logger logger = LogManager.getLogger(PushbulletMessageReader.class);
    private final String pushbulletToken;
    private final Algorithm algorithm;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    // List of registered message listeners
    private final List<PushbulletMessageListener> listeners;

    // Track the last processed push timestamp to avoid processing duplicates
    private double lastProcessedTimestamp;

    // Polling interval in seconds
    private static final long DEFAULT_POLL_INTERVAL_SECONDS = 5;
    private long pollIntervalSeconds;

    // Pushbullet API endpoints
    private static final String PUSHBULLET_API_BASE = "https://api.pushbullet.com/v2";
    private static final String PUSHES_ENDPOINT = PUSHBULLET_API_BASE + "/pushes";

    /**
     * Constructor with default polling interval (5 seconds)
     *
     * @param algorithm       The algorithm to control
     * @param pushbulletToken The Pushbullet API token
     */
    public PushbulletMessageReader(Algorithm algorithm, String pushbulletToken) {
        this(algorithm, pushbulletToken, DEFAULT_POLL_INTERVAL_SECONDS);
    }

    /**
     * Constructor with custom polling interval
     *
     * @param algorithm           The algorithm to control
     * @param pushbulletToken     The Pushbullet API token
     * @param pollIntervalSeconds How often to poll for new messages (in seconds)
     */
    public PushbulletMessageReader(Algorithm algorithm, String pushbulletToken, long pollIntervalSeconds) {
        this.pushbulletToken = pushbulletToken;
        this.algorithm = algorithm;
        this.pollIntervalSeconds = pollIntervalSeconds;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.isRunning = new AtomicBoolean(false);
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClients.createDefault();
        this.lastProcessedTimestamp = 0;
        this.listeners = new ArrayList<>();

        logger.info("Pushbullet message reader initialized for algorithm: {}", algorithm.getAlgorithmInfo());
    }

    /**
     * Start monitoring for Pushbullet messages
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            logger.info("Starting Pushbullet message reader with poll interval of {} seconds", pollIntervalSeconds);

            // Initialize the timestamp to current time to avoid processing old messages
            if (lastProcessedTimestamp == 0) {
                lastProcessedTimestamp = System.currentTimeMillis() / 1000.0;
                logger.info("Initialized last processed timestamp to: {}", lastProcessedTimestamp);
            }

            // Schedule the polling task
            scheduler.scheduleAtFixedRate(
                    this::checkForMessages,
                    0, // Initial delay
                    pollIntervalSeconds,
                    TimeUnit.SECONDS
            );
        } else {
            logger.warn("Pushbullet message reader is already running");
        }
    }

    /**
     * Stop monitoring for Pushbullet messages
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            logger.info("Stopping Pushbullet message reader");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                httpClient.close();
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Error closing HTTP client: {}", e.getMessage());
            }
        }
    }

    /**
     * Check for new Pushbullet messages
     */
    private void checkForMessages() {
        try {
            // Create HTTP GET request
            HttpGet httpGet = new HttpGet(PUSHES_ENDPOINT + "?modified_after=" + lastProcessedTimestamp);
            httpGet.addHeader("Access-Token", pushbulletToken);
            httpGet.addHeader("Content-Type", "application/json");

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != 200) {
                    logger.error("Failed to fetch pushes. Status code: {}", statusCode);
                    return;
                }

                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return;
                }

                String responseBody = EntityUtils.toString(entity);
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode pushes = root.get("pushes");

                if (pushes == null || !pushes.isArray() || pushes.isEmpty()) {
                    return;
                }

                // Process pushes (they come in reverse chronological order from API)
                for (JsonNode push : pushes) {
                    processPush(push);
                }
            }

        } catch (Exception e) {
            logger.error("Error checking for Pushbullet messages: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a single push message
     *
     * @param pushNode The push JSON node to process
     */
    private void processPush(JsonNode pushNode) {
        try {
            // Get push properties
            boolean active = pushNode.has("active") && pushNode.get("active").asBoolean();
            boolean dismissed = pushNode.has("dismissed") && pushNode.get("dismissed").asBoolean();
            String type = pushNode.has("type") ? pushNode.get("type").asText() : null;
            double modified = pushNode.has("modified") ? pushNode.get("modified").asDouble() : 0;

            // Update last processed timestamp
            if (modified > lastProcessedTimestamp) {
                lastProcessedTimestamp = modified;
            }

            // Skip dismissed or inactive pushes
            if (!active || dismissed) {
                return;
            }

            // Only process note type pushes
            if (!"note".equals(type)) {
                return;
            }

            String title = pushNode.has("title") ? pushNode.get("title").asText() : "";
            String body = pushNode.has("body") ? pushNode.get("body").asText() : "";

            logger.info("Received Pushbullet message - Title: '{}', Body: '{}'", title, body);

            // Notify all registered listeners
            notifyListeners(title, body);

        } catch (Exception e) {
            logger.error("Error processing push: {}", e.getMessage(), e);
        }
    }


    /**
     * Check if the reader is currently running
     *
     * @return true if running
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Set the polling interval (only takes effect after restart)
     *
     * @param pollIntervalSeconds New polling interval in seconds
     */
    public void setPollIntervalSeconds(long pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    /**
     * Register a listener to receive Pushbullet messages
     *
     * @param listener The listener to register
     */
    public void registerListener(PushbulletMessageListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            logger.info("Registered Pushbullet message listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Unregister a listener from receiving Pushbullet messages
     *
     * @param listener The listener to unregister
     */
    public void unregisterListener(PushbulletMessageListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            logger.info("Unregistered Pushbullet message listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Notify all registered listeners of a new message
     *
     * @param title The message title
     * @param body  The message body
     */
    private void notifyListeners(String title, String body) {
        for (PushbulletMessageListener listener : listeners) {
            try {
                listener.onPushbulletMessage(title, body);
            } catch (Exception e) {
                logger.error("Error notifying listener {}: {}", listener.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }
}












