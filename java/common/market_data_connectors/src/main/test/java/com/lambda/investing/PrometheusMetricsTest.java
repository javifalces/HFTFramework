package com.lambda.investing;

import io.prometheus.client.CollectorRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PrometheusMetricsTest {

    @Before
    public void setUp() {
        // Ensure any previous singleton state is cleared
        PrometheusMetricsExporter exporter = PrometheusMetricsExporter.getInstance();
        exporter.stop();
        // Clear registry to avoid "already registered" errors between tests
        CollectorRegistry.defaultRegistry.clear();
    }

    @After
    public void tearDown() {
        PrometheusMetricsExporter exporter = PrometheusMetricsExporter.getInstance();
        exporter.stop();
        CollectorRegistry.defaultRegistry.clear();
    }

    // ------------------------------------------------------------------ exporter disabled by default

    @Test
    public void testExporterDisabledWhenNoEnvVar() {
        // PROMETHEUS_PORT is not set in the test environment
        PrometheusMetricsExporter exporter = PrometheusMetricsExporter.getInstance();
        assertFalse("Exporter should be disabled when PROMETHEUS_PORT is not set",
                exporter.isEnabled());
    }

    // ------------------------------------------------------------------ Statistics (no Prometheus server)

    @Test
    public void testStatisticsTracksCountersInMemory() {
        // Statistics works independently of Prometheus
        Statistics stats = new Statistics("test_header", 0); // sleepMs=0 → no background thread

        // addStatistics should increment without throwing
        stats.addStatistics("depth.BTCUSD");
        stats.addStatistics("depth.BTCUSD");
        stats.addStatistics("trade.BTCUSD");

        // setStatistics should not throw
        stats.setStatistics("depth.BTCUSD", 99);
    }

    // ------------------------------------------------------------------ LatencyStatistics (no Prometheus server)

    @Test
    public void testLatencyStatisticsAddsLatency() {
        LatencyStatistics ls = new LatencyStatistics("test_latency_header", 0);

        // addLatencyStatistics should not throw
        ls.addLatencyStatistics("depth.BTCUSD.TOTAL", 5L);
        ls.addLatencyStatistics("depth.BTCUSD.TOTAL", 10L);
        ls.addLatencyStatistics("depth.BTCUSD.TOTAL", 15L);

        // startKey / stopKey round-trip
        ls.startKeyStatistics("myTopic", "key1", 1000L);
        ls.stopKeyStatistics("key1", 2000L);
    }

    // ------------------------------------------------------------------ Multiple Statistics instances

    @Test
    public void testMultipleStatisticsInstancesDontConflict() {
        // Two independent Statistics instances with different headers should not interfere
        Statistics stats1 = new Statistics("header_one", 0);
        Statistics stats2 = new Statistics("header_two", 0);

        stats1.addStatistics("topic.A");
        stats2.addStatistics("topic.A");
        stats2.addStatistics("topic.B");

        // No exception means test passes
    }

    // ------------------------------------------------------------------ LatencyStatistics with multiple topics

    @Test
    public void testLatencyStatisticsDoesNotThrowWithMultipleTopics() {
        LatencyStatistics ls = new LatencyStatistics("latency_multi_test", 0);

        for (int i = 1; i <= 50; i++) {
            ls.addLatencyStatistics("executionReport.ALGO1.TOTAL", (long) i);
            ls.addLatencyStatistics("depth.BTCUSD.toBrokerConnector", (long) i * 2);
        }
    }

    // ------------------------------------------------------------------ Prometheus name sanitisation

    @Test
    public void testPrometheusNameSanitisation() {
        // Ensure that headers with special characters don't blow up Statistics
        Statistics stats = new Statistics("Data received!", 0);
        stats.addStatistics("topic-with-dashes");
    }
}
