package com.lambda.investing;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import io.prometheus.client.hotspot.DefaultExports;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Singleton that pushes Prometheus metrics to an existing Prometheus Pushgateway.
 * <p>
 * Enabled by setting {@code PROMETHEUS_HOST} and {@code PROMETHEUS_PORT} (environment variables
 * or JVM system properties). When enabled, metrics are pushed periodically to
 * {@code http://<PROMETHEUS_HOST>:<PROMETHEUS_PORT>} so that a Prometheus server
 * already running can scrape them from the Pushgateway.
 * </p>
 * <p>
 * JVM / process performance metrics (heap usage, GC, threads, CPU time, …) are exported
 * automatically via the {@code simpleclient_hotspot} default exports.
 * </p>
 */
public class PrometheusMetricsExporter {

    private static final Logger logger = LogManager.getLogger(PrometheusMetricsExporter.class);

    /**
     * How often metrics are pushed to the Pushgateway (in seconds).
     */
    private static final long PUSH_INTERVAL_SECONDS = 15;

    /**
     * Job name used to identify this process in the Pushgateway.
     */
    private static final String JOB_NAME = Configuration.LOG_APP_NAME;

    private static volatile PrometheusMetricsExporter INSTANCE;

    private final CollectorRegistry registry;
    private PushGateway pushGateway;
    private ScheduledExecutorService scheduler;
    private final boolean enabled;

    private PrometheusMetricsExporter() {
        this.registry = CollectorRegistry.defaultRegistry;


        if (Configuration.PROMETHEUS_PORT == null || Configuration.PROMETHEUS_PORT.isEmpty()) {
            this.enabled = false;
            return;
        }

        String host = Configuration.PROMETHEUS_HOST;
        String portStr = Configuration.PROMETHEUS_PORT;

        if (host != null && !host.isEmpty() && portStr != null && !portStr.isEmpty()) {
            int port;
            try {
                port = Integer.parseInt(portStr.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid PROMETHEUS_PORT value '{}'; Prometheus exporter disabled.", portStr);
                this.enabled = false;
                return;
            }

            // Register JVM / process metrics (memory, GC, threads, CPU, file descriptors)
            DefaultExports.initialize();

            // Check connectivity before registering
            if (!isReachable(host.trim(), port)) {
                String warning = "[PrometheusMetricsExporter] WARNING: Prometheus Pushgateway "
                        + host.trim() + ":" + port + " is not reachable — exporter disabled.";
                System.err.println(warning);
                logger.warn("Prometheus Pushgateway {}:{} is not reachable — exporter disabled.", host.trim(), port);
                this.enabled = false;
                return;
            }

            String address = host.trim() + ":" + port;
            String fullUrl = "http://" + address;
            this.pushGateway = new PushGateway(address);

            // Push once immediately to validate connectivity
            try {
                pushGateway.pushAdd(registry, JOB_NAME);
                String msg = "Prometheus metrics push to Pushgateway " + fullUrl + " (job='" + JOB_NAME + "') — OK";
                logger.info(msg);
                System.out.println(msg);
            } catch (IOException e) {
                logger.warn("Initial push to Prometheus Pushgateway {} failed: {}", fullUrl, e.getMessage());
            }

            // Schedule periodic pushes
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "prometheus-push");
                t.setDaemon(true);
                return t;
            });
            this.scheduler.scheduleAtFixedRate(() -> {
                try {
                    pushGateway.pushAdd(registry, JOB_NAME);
                } catch (IOException e) {
                    logger.warn("Failed to push metrics to Prometheus Pushgateway {}: {}", fullUrl, e.getMessage());
                }
            }, PUSH_INTERVAL_SECONDS, PUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);

            this.enabled = true;
        } else {
            this.enabled = false;
            logger.debug("PROMETHEUS_HOST / PROMETHEUS_PORT not set; Prometheus exporter disabled.");
        }
    }

    /**
     * Returns the singleton instance, initialising it on the first call.
     */
    public static PrometheusMetricsExporter getInstance() {
        if (INSTANCE == null) {
            synchronized (PrometheusMetricsExporter.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PrometheusMetricsExporter();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Returns {@code true} when the Prometheus exporter is running.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the Prometheus {@link CollectorRegistry} used for all metrics.
     */
    public CollectorRegistry getRegistry() {
        return registry;
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

    /**
     * Stops the push scheduler and deletes the job from the Pushgateway (if running).
     * Normally called only in tests or on application shutdown.
     */
    public void stop() {
        synchronized (PrometheusMetricsExporter.class) {
            if (scheduler != null) {
                scheduler.shutdownNow();
                scheduler = null;
            }
            if (pushGateway != null) {
                try {
                    pushGateway.delete(JOB_NAME);
                } catch (IOException e) {
                    logger.warn("Failed to delete Pushgateway job '{}': {}", JOB_NAME, e.getMessage());
                }
                pushGateway = null;
            }
            // Reset singleton so it can be re-created in tests
            INSTANCE = null;
        }
    }
}
