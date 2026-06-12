package com.lambda.investing;

import com.lambda.investing.connector.disruptor.DisruptorConnectorHelper;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Statistics implements Runnable {

    private static boolean RESET_STATISTICS_PER_UPDATE = true;
    private long sleepMs;
    private boolean enable;
    private Map<String, Long> topicToCounter;
    private Map<String, Long> topicToTotalCounter;
    private String header;
    protected Logger logger = LogManager.getLogger(Statistics.class);

    // Prometheus metrics (lazily initialised when Prometheus is enabled)
    private Counter prometheusCounter;
    private Gauge prometheusTotal;
    private final String prometheusPrefix;

    public Statistics(String header, long sleepMs) {
        this.header = header;
        this.sleepMs = sleepMs;
        topicToCounter = new ConcurrentHashMap<>();
        topicToTotalCounter = new ConcurrentHashMap<>();
        enable = true;
        this.prometheusPrefix = toPrometheusName("statistics_" + header);
        initPrometheusMetrics();
        if (sleepMs > 0) {
            Thread thread = new Thread(this, "Statistics_" + header);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    /**
     * Sanitises a string so it can be used as a Prometheus metric name.
     * Prometheus names must match {@code [a-zA-Z_:][a-zA-Z0-9_:]*}.
     */
    private static String toPrometheusName(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9_:]", "_").toLowerCase();
    }

    private void initPrometheusMetrics() {
        PrometheusMetricsExporter exporter = PrometheusMetricsExporter.getInstance();
        if (!exporter.isEnabled()) {
            return;
        }
        try {
            prometheusCounter = Counter.build()
                    .name(prometheusPrefix + "_count")
                    .help("Per-topic message count for " + header)
                    .labelNames("topic")
                    .register(exporter.getRegistry());

            prometheusTotal = Gauge.build()
                    .name(prometheusPrefix + "_total")
                    .help("Per-topic cumulative total count for " + header)
                    .labelNames("topic")
                    .register(exporter.getRegistry());
        } catch (Exception e) {
            logger.warn("Could not register Prometheus metrics for Statistics '{}': {}", header, e.getMessage());
        }
    }

    public void addStatistics(String topic) {
        long counter = topicToCounter.getOrDefault(topic, 0L);
        long newCounter = counter + 1;
        topicToCounter.put(topic, newCounter);
        long newTotal = topicToTotalCounter.getOrDefault(topic, 0L) + 1;
        topicToTotalCounter.put(topic, newTotal);
        // Push to Prometheus immediately so scrapes are always up-to-date
        if (prometheusCounter != null) {
            prometheusCounter.labels(topic).inc();
        }
        if (prometheusTotal != null) {
            prometheusTotal.labels(topic).set(newTotal);
        }
    }

    public void setStatistics(String topic, long counter) {
        topicToCounter.put(topic, counter);
        topicToTotalCounter.put(topic, counter);
        if (prometheusTotal != null) {
            prometheusTotal.labels(topic).set(counter);
        }
    }

    private void printCurrentStatistics() {
        if (topicToCounter.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("******** %s ********\n", header));

            for (Map.Entry<String, Long> entry : topicToCounter.entrySet()) {
                long totalCounter = topicToTotalCounter.getOrDefault(entry.getKey(), 0L);
                String topic = entry.getKey();
                if (topic.length() > 40) {
                    String suffixAfterDash = "";
                    int lastDashIndex = topic.lastIndexOf("-");
                    if (lastDashIndex != -1 && lastDashIndex + 1 < topic.length()) {
                        suffixAfterDash = topic.substring(lastDashIndex);
                    }
                    topic = topic.substring(0, 35) + "...-" + suffixAfterDash;
                }
                sb.append(String.format("\t%s:\t%d\ttotal:%d\n", topic, entry.getValue(), totalCounter));
            }

            sb.append(String.format("Depth.Pool: %s\n", Depth.logPool()));
            sb.append(String.format("Trade.Pool: %s\n", Trade.logPool()));
            if (!DisruptorConnectorHelper.isEmpty()) {
                sb.append(DisruptorConnectorHelper.getAllInstancesStatus()).append("\n");
            }
            sb.append("****************");

            logger.info(sb.toString());

            if (RESET_STATISTICS_PER_UPDATE) {
                topicToCounter.clear();
            }

        }
    }


    @Override
    public void run() {

        while (enable) {


            printCurrentStatistics();

            try {
                Thread.sleep(this.sleepMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


}
