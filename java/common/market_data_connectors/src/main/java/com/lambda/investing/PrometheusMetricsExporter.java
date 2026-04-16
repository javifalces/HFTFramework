package com.lambda.investing;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Singleton that manages the Prometheus HTTP metrics endpoint.
 * <p>
 * Enabled by setting the {@code PROMETHEUS_PORT} environment variable (or Java system property)
 * to a valid port number. When enabled, a lightweight HTTP server is started on that port so
 * that a Prometheus scraper (or a Grafana agent) can pull metrics from {@code /metrics}.
 * </p>
 * <p>
 * JVM / process performance metrics (heap usage, GC, threads, CPU time, …) are exported
 * automatically via the {@code simpleclient_hotspot} default exports.
 * </p>
 */
public class PrometheusMetricsExporter {

    private static final Logger logger = LogManager.getLogger(PrometheusMetricsExporter.class);

    private static volatile PrometheusMetricsExporter INSTANCE;

    private final CollectorRegistry registry;
    private HTTPServer httpServer;
    private final boolean enabled;

    private PrometheusMetricsExporter() {
        this.registry = CollectorRegistry.defaultRegistry;

        // Initialise Loki log shipping if LOKI_URL is configured
        LokiLogAppender.initializeLoki();

        String portStr = Configuration.PROMETHEUS_PORT;
        if (portStr != null && !portStr.isEmpty()) {
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
            String displayHost = "0.0.0.0"; // Listen on all interfaces
            boolean started = false;
            try {
                HTTPServer.Builder builder = new HTTPServer.Builder()
                        .withPort(port)
                        .withRegistry(registry);
                this.httpServer = builder.build();
                started = true;

                logger.info("Prometheus metrics HTTP server started on {}:{} ->  http://localhost:{}", displayHost, port, port);
                System.out.println("Prometheus metrics HTTP server started on " + displayHost + ":" + port + " ->  http://localhost:" + port);
            } catch (IOException e) {
                logger.error("Failed to start Prometheus metrics HTTP server on {}:{}", displayHost, port, e);
            }
            this.enabled = started;
        } else {
            this.enabled = false;
            logger.debug("PROMETHEUS_PORT not set; Prometheus exporter disabled.");
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
     * Stops the HTTP server (if running). Normally called only in tests.
     */
    public void stop() {
        synchronized (PrometheusMetricsExporter.class) {
            if (httpServer != null) {
                httpServer.close();
                httpServer = null;
            }
            // Reset singleton so it can be re-created in tests
            INSTANCE = null;
        }
    }
}
