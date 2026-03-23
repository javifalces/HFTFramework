package com.lambda.investing;

import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import io.prometheus.client.Gauge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static com.lambda.investing.PrintUtils.PrintDate;

public class LatencyStatistics implements Runnable {
    private static boolean RESET_STATISTICS_PER_UPDATE = true;
    private long sleepMs;
    private boolean enable;

    private Map<String, String> keyToTopic;
    private Map<String, Long> keyToStartDate;
    private Map<String, List<Long>> topicToLatency;
    private String header;
    protected Logger logger = LogManager.getLogger(LatencyStatistics.class);

    // Track daily maximum statistics by basePrefix and topic
    private Map<String, Map<String, DailyMaxStats>> basePrefixToTopicToDailyMaxStats;

    // Prometheus gauges for latency percentiles (created lazily, keyed by metric name)
    private Map<String, Gauge> prometheusGauges;
    // Track metric names for which registration has already failed to avoid repeated log noise
    private Set<String> prometheusGaugesFailed;
    private boolean prometheusEnabled;

    // Inner class to hold daily maximum statistics
    private static class DailyMaxStats {
        double maxMean = 0.0;
        double maxPercentile50 = 0.0;
        double maxPercentile75 = 0.0;
        double maxPercentile90 = 0.0;
        double maxPercentile95 = 0.0;
        double maxPercentile99 = 0.0;
        double maxLatency = 0.0;

        void updateIfGreater(double mean, double p50, double p75, double p90, double p95, double p99, double max) {
            if (mean > maxMean) maxMean = mean;
            if (p50 > maxPercentile50) maxPercentile50 = p50;
            if (p75 > maxPercentile75) maxPercentile75 = p75;
            if (p90 > maxPercentile90) maxPercentile90 = p90;
            if (p95 > maxPercentile95) maxPercentile95 = p95;
            if (p99 > maxPercentile99) maxPercentile99 = p99;
            if (max > maxLatency) maxLatency = max;
        }
    }

    public LatencyStatistics(String header, long sleepMs) {
        this.header = header;
        this.sleepMs = sleepMs;
        keyToStartDate = new ConcurrentHashMap<>();
        keyToTopic = new ConcurrentHashMap<>();
        topicToLatency = new ConcurrentHashMap<>();
        basePrefixToTopicToDailyMaxStats = new ConcurrentHashMap<>();
        prometheusGauges = new ConcurrentHashMap<>();
        prometheusGaugesFailed = new ConcurrentSkipListSet<>();
        prometheusEnabled = PrometheusMetricsExporter.getInstance().isEnabled();
        enable = true;
        if (sleepMs > 0) {
            Thread thread = new Thread(this, "LatencyStatistics");
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    /**
     * Sanitises a string so it can be used as a Prometheus metric name.
     */
    private static String toPrometheusName(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9_:]", "_").toLowerCase();
    }

    /**
     * Returns (creating if needed) a Prometheus Gauge with the given metric name and help text.
     * The gauge uses a single label {@code topic} to distinguish different subsections.
     * Failed registrations are tracked to avoid repeated log noise.
     */
    private Gauge getOrCreateGauge(String metricName, String help) {
        Gauge existing = prometheusGauges.get(metricName);
        if (existing != null) {
            return existing;
        }
        if (prometheusGaugesFailed.contains(metricName)) {
            return null;
        }
        try {
            Gauge gauge = Gauge.build()
                    .name(metricName)
                    .help(help)
                    .labelNames("topic")
                    .register(PrometheusMetricsExporter.getInstance().getRegistry());
            prometheusGauges.put(metricName, gauge);
            return gauge;
        } catch (Exception e) {
            logger.warn("Could not register Prometheus gauge '{}': {}", metricName, e.getMessage());
            prometheusGaugesFailed.add(metricName);
            return null;
        }
    }

    public void addLatencyStatistics(String key, long latency) {
        List<Long> slippages = topicToLatency.getOrDefault(key, new ArrayList<>());
        slippages.add(latency);
        topicToLatency.put(key, slippages);
    }

    public void addDepthLatencyStatistics(String algorithmInfo, long currentTime, Depth depth) {
        try {
            // Add sub-statistics for internal latencies
            String prefix = "depth." + algorithmInfo;
            addInternalLatencyStatistics(prefix, depth.getTimestamp(), depth.getTimestampBrokerConnector(),
                    depth.getTimestampAlgoConnector(), depth.getTimestampStrategy());

        } catch (Exception e) {
            logger.error("error addDepthLatencyStatistics latency statistics", e);
        }
    }

    public void addTradeLatencyStatistics(String algorithmInfo, long currentTime, Trade trade) {

        try {
            // Add sub-statistics for internal latencies
            String prefix = "trade." + algorithmInfo;
            addInternalLatencyStatistics(prefix, trade.getTimestamp(), trade.getTimestampBrokerConnector(),
                    trade.getTimestampAlgoConnector(), trade.getTimestampStrategy());

        } catch (Exception e) {
            logger.error("error addTradeLatencyStatistics latency statistics", e);
        }
    }

    public void addExecutionReportLatencyStatistics(String algorithmInfo, long currentTime, ExecutionReport executionReport) {
        try {
            // Add sub-statistics for internal latencies
            String prefix = "executionReport." + algorithmInfo;
            addInternalLatencyStatistics(prefix, executionReport.getTimestampCreation(),
                    executionReport.getTimestampBrokerConnector(),
                    executionReport.getTimestampAlgoConnector(),
                    executionReport.getTimestampStrategy());

        } catch (Exception e) {
            logger.error("error addExecutionReportLatencyStatistics latency statistics", e);
        }
    }

    public void addOrderRequestLatencyStatistics(String algorithmInfo, long currentTime, OrderRequest orderRequest) {
        try {
            String prefix = "orderRequest." + algorithmInfo;
            addOrderRequestOutboundLatencyStatistics(prefix, currentTime, orderRequest);

        } catch (Exception e) {
            logger.error("error addOrderRequestLatencyStatistics latency statistics", e);
        }
    }

    /**
     * Adds latency statistics for order requests generated in the strategy going out to the market.
     * Tracks the latency from strategy creation through algoConnector and brokerConnector to current time.
     * This is the reverse direction compared to addOrderRequestLatencyStatistics (outbound vs inbound).
     *
     * @param prefix       The prefix for the statistics key (e.g., "depth.BTCUSD.algorithmInfo")
     * @param currentTime  The current time (before sending to market)
     * @param orderRequest The order request being sent
     */
    public void addOrderRequestOutboundLatencyStatistics(String prefix, long currentTime, OrderRequest orderRequest) {
        try {

            // Track latency from strategy -> algoConnector -> brokerConnector -> now (before market)
            addInternalLatencyStatistics(prefix, orderRequest.getTimestampCreation(),
                    orderRequest.getTimestampAlgoConnector(),
                    orderRequest.getTimestampBrokerConnector(),
                    currentTime);

        } catch (Exception e) {
            logger.error("error addOrderRequestOutboundLatencyStatistics latency statistics", e);
        }
    }

    /**
     * Adds sub-statistics for internal latency breakdown across different stages.
     * This matches the structure of getLatenciesTable() to track latencies between:
     * - timestamp to timestampBrokerConnector
     * - timestampBrokerConnector to timestampAlgoConnector
     * - timestampAlgoConnector to timestampStrategy
     * - timestampStrategy to now
     *
     * @param prefix                   The prefix for the statistics key (e.g., "depth.BTCUSD.algorithmInfo")
     * @param timestamp                The initial timestamp
     * @param timestampBrokerConnector The broker connector timestamp
     * @param timestampAlgoConnector   The algo connector timestamp
     * @param timestampStrategy        The strategy timestamp
     */
    private void addInternalLatencyStatistics(String prefix, long timestamp,
                                              long timestampBrokerConnector,
                                              long timestampAlgoConnector,
                                              long timestampStrategy) {
        long lastReference = timestamp;
        long totalLatency = 0;

        // Track latency from timestamp to timestampBrokerConnector
        if (timestampBrokerConnector > 0) {
            long latency = timestampBrokerConnector - lastReference;
            addLatencyStatistics(prefix + ".toBrokerConnector", latency);
            totalLatency += latency;
            lastReference = timestampBrokerConnector;
        }

        // Track latency from timestampBrokerConnector to timestampAlgoConnector
        if (timestampAlgoConnector > 0) {
            long latency = timestampAlgoConnector - lastReference;
            addLatencyStatistics(prefix + ".toAlgoConnector", latency);
            totalLatency += latency;
            lastReference = timestampAlgoConnector;
        }

        // Track latency from timestampAlgoConnector to timestampStrategy
        if (timestampStrategy > 0) {
            long latency = timestampStrategy - lastReference;
            addLatencyStatistics(prefix + ".toStrategy", latency);
            totalLatency += latency;
            lastReference = timestampStrategy;
        }

        // Track latency from last timestamp to now
        long currentTime = System.currentTimeMillis();
        long latency = currentTime - lastReference;
        addLatencyStatistics(prefix + ".toNow", latency);
        totalLatency += latency;

        // Track total latency
        addLatencyStatistics(prefix + ".TOTAL", totalLatency);
    }





    public void startKeyStatistics(String topic, String key, long start) {
        keyToStartDate.put(key, start);
        keyToTopic.put(key, topic);
        topicToLatency.put(topic, new ArrayList<>());
    }

    public void stopKeyStatistics(String key, Long stop) {

        Long start = keyToStartDate.get(key);
        if (start == null) {
            return;
        }

        String topic = keyToTopic.get(key);
        if (topic == null) {
            return;
        }


        List<Long> slippages = topicToLatency.get(topic);
        if (slippages == null) {
            return;
        }

        long diff = stop - start;
        slippages.add(diff);
        topicToLatency.put(topic, slippages);
        keyToStartDate.remove(key);//avoid double counting

    }


    private synchronized void printCurrentStatistics() {
        if (topicToLatency.size() > 0) {
            // Create a deep copy of the map with copied lists to avoid ConcurrentModificationException
            Map<String, List<Long>> snapshot = new ConcurrentHashMap<>();
            for (Map.Entry<String, List<Long>> entry : topicToLatency.entrySet()) {
                snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }

            // Group statistics by their base prefix
            Map<String, Map<String, List<Long>>> groupedStats = groupStatisticsByPrefix(snapshot);

            // Print each group
            for (Map.Entry<String, Map<String, List<Long>>> groupEntry : groupedStats.entrySet()) {
                String basePrefix = groupEntry.getKey();
                Map<String, List<Long>> subsections = groupEntry.getValue();
                printGroupedStatistics(basePrefix, subsections);
            }

            if (RESET_STATISTICS_PER_UPDATE) {
                topicToLatency.clear();
                keyToTopic.clear();
                keyToStartDate.clear();
            }
        }
    }

    /**
     * Groups statistics by their base prefix, separating the subsection suffixes.
     * For example, "depth.BTCUSD.toBrokerConnector" becomes base="depth.BTCUSD", subsection="toBrokerConnector"
     */
    private Map<String, Map<String, List<Long>>> groupStatisticsByPrefix(Map<String, List<Long>> snapshot) {
        Map<String, Map<String, List<Long>>> groupedStats = new LinkedHashMap<>();

        for (Map.Entry<String, List<Long>> entry : snapshot.entrySet()) {
            String fullTopic = entry.getKey();
            String basePrefix;
            String subsection;

            // Check if this is a subsection statistic
            if (fullTopic.contains(".toBrokerConnector") || fullTopic.contains(".toAlgoConnector") ||
                    fullTopic.contains(".toStrategy") || fullTopic.contains(".toNow") || fullTopic.contains(".TOTAL")) {

                int lastDotIndex = fullTopic.lastIndexOf(".");
                basePrefix = fullTopic.substring(0, lastDotIndex);
                subsection = fullTopic.substring(lastDotIndex + 1);
            } else {
                basePrefix = fullTopic;
                subsection = null;
            }

            groupedStats.computeIfAbsent(basePrefix, k -> new LinkedHashMap<>()).put(subsection, entry.getValue());
        }

        return groupedStats;
    }

    /**
     * Prints a group of statistics with a header and subsections in the correct order.
     * For outbound metrics (orderRequest), the order is reversed to reflect the outbound flow.
     */
    private void printGroupedStatistics(String basePrefix, Map<String, List<Long>> subsections) {
        // If there are subsections, print the header first
        if (subsections.size() > 1 && subsections.containsKey("TOTAL")) {
            // Print header with base prefix
            logger.info("═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════");
            logger.info("  {}", basePrefix);
            logger.info("───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────");

            // Determine the order based on whether this is an outbound (orderRequest) metric
            boolean isOutbound = basePrefix.startsWith("orderRequest");
            String[] order = getSubsectionOrder(isOutbound);

            // Print subsections in specific order: TOTAL first, then others
            for (String subsectionName : order) {
                if (subsections.containsKey(subsectionName)) {
                    printLatencyLine(basePrefix, subsectionName, subsections.get(subsectionName), true);
                }
            }

            // Print any other subsections not in the order list
            for (Map.Entry<String, List<Long>> subsectionEntry : subsections.entrySet()) {
                String subsectionName = subsectionEntry.getKey();
                if (!Arrays.asList(order).contains(subsectionName)) {
                    printLatencyLine(basePrefix, subsectionName, subsectionEntry.getValue(), true);
                }
            }
        } else {
            // Print standalone statistics (without subsections)
            for (Map.Entry<String, List<Long>> subsectionEntry : subsections.entrySet()) {
                String displayName = subsectionEntry.getKey() == null ? basePrefix : basePrefix + "." + subsectionEntry.getKey();
                printLatencyLine(basePrefix, displayName, subsectionEntry.getValue(), false);
            }
        }
    }

    /**
     * Returns the correct order of subsections based on whether the metric is inbound or outbound.
     * Inbound (depth, trade, executionReport): market → broker → algo → strategy
     * Outbound (orderRequest): strategy → algo → broker → market
     */
    private String[] getSubsectionOrder(boolean isOutbound) {
        if (isOutbound) {
            // For outbound: from strategy creation to market (reversed)
            return new String[]{"TOTAL", "toAlgoConnector", "toBrokerConnector", "toNow"};
        } else {
            // For inbound: from market to strategy (normal)
            return new String[]{"TOTAL", "toBrokerConnector", "toAlgoConnector", "toStrategy", "toNow"};
        }
    }

    private void printLatencyLine(String basePrefix, String topic, List<Long> latency, boolean isSubsection) {
        int counter = latency.size();
        if (counter > 0) {
            double mean = latency.stream().mapToLong(a -> a).average().orElse(0.0);
            double maxLatency = latency.stream().mapToLong(a -> a).max().orElse(0);

            // Calculate percentiles
            double percentile50 = latency.stream().sorted().skip((long) (latency.size() * 0.5)).findFirst().orElse(0L);
            double percentile75 = latency.stream().sorted().skip((long) (latency.size() * 0.75)).findFirst().orElse(0L);
            double percentile90 = latency.stream().sorted().skip((long) (latency.size() * 0.9)).findFirst().orElse(0L);
            double percentile95 = latency.stream().sorted().skip((long) (latency.size() * 0.95)).findFirst().orElse(0L);
            double percentile99 = latency.stream().sorted().skip((long) (latency.size() * 0.99)).findFirst().orElse(0L);

            // Get or create daily max stats for this basePrefix and topic
            Map<String, DailyMaxStats> topicToDailyMaxStats = basePrefixToTopicToDailyMaxStats.computeIfAbsent(
                    basePrefix, k -> new ConcurrentHashMap<>());
            DailyMaxStats dailyMaxStats = topicToDailyMaxStats.computeIfAbsent(topic, k -> new DailyMaxStats());

            // Update daily max stats with current values
            dailyMaxStats.updateIfGreater(mean, percentile50, percentile75, percentile90, percentile95, percentile99, maxLatency);

            // Export to Prometheus when enabled
            if (prometheusEnabled) {
                publishLatencyToPrometheus(topic, counter, mean, percentile50, percentile75,
                        percentile90, percentile95, percentile99, maxLatency);
            }

            // Format with indentation for subsections
            String indent = isSubsection ? "    " : "";
            String displayTopic = topic;

            // Truncate long topic names
            if (displayTopic.length() > 50 && !isSubsection) {
                String suffixAfterDash = "";
                int lastDashIndex = displayTopic.lastIndexOf("-");
                if (lastDashIndex != -1 && lastDashIndex + 1 < displayTopic.length()) {
                    suffixAfterDash = displayTopic.substring(lastDashIndex);
                }
                displayTopic = displayTopic.substring(0, 35) + "...-" + suffixAfterDash;
            }

            // Highlight TOTAL with special formatting
            String prefix = topic.equals("TOTAL") ? "  ► " : indent + "    ";
            String topicPadded = String.format("%-40s", displayTopic);

            logger.info("{}{}  size:{}\tmean(ms):{}[{}]\t50pct:{}[{}]\t75pct:{}[{}]\t90pct:{}[{}]\t95pct:{}[{}]\t99pct:{}[{}]\tmax:{}[{}]",
                    prefix, topicPadded, counter,
                    String.format("%.2f", mean), String.format("%.2f", dailyMaxStats.maxMean),
                    String.format("%.2f", percentile50), String.format("%.2f", dailyMaxStats.maxPercentile50),
                    String.format("%.2f", percentile75), String.format("%.2f", dailyMaxStats.maxPercentile75),
                    String.format("%.2f", percentile90), String.format("%.2f", dailyMaxStats.maxPercentile90),
                    String.format("%.2f", percentile95), String.format("%.2f", dailyMaxStats.maxPercentile95),
                    String.format("%.2f", percentile99), String.format("%.2f", dailyMaxStats.maxPercentile99),
                    String.format("%.2f", maxLatency), String.format("%.2f", dailyMaxStats.maxLatency));
        }
    }

    /**
     * Pushes latency statistics for a given topic to Prometheus gauges.
     * Each metric is named {@code latency_<sanitised_topic>_<stat>_ms} and uses a {@code topic}
     * label so all per-topic gauges share the same Prometheus metric family.
     */
    private void publishLatencyToPrometheus(String topic, int sampleCount,
                                            double mean, double p50, double p75,
                                            double p90, double p95, double p99, double max) {
        try {
            String labelValue = topic;

            setGauge("latency_count", "Number of latency samples", labelValue, sampleCount);
            setGauge("latency_mean_ms", "Mean latency in milliseconds", labelValue, mean);
            setGauge("latency_p50_ms", "50th percentile latency in milliseconds", labelValue, p50);
            setGauge("latency_p75_ms", "75th percentile latency in milliseconds", labelValue, p75);
            setGauge("latency_p90_ms", "90th percentile latency in milliseconds", labelValue, p90);
            setGauge("latency_p95_ms", "95th percentile latency in milliseconds", labelValue, p95);
            setGauge("latency_p99_ms", "99th percentile latency in milliseconds", labelValue, p99);
            setGauge("latency_max_ms", "Maximum latency in milliseconds", labelValue, max);
        } catch (Exception e) {
            logger.warn("Failed to publish latency metrics to Prometheus for topic '{}': {}", topic, e.getMessage());
        }
    }

    private void setGauge(String metricName, String help, String labelValue, double value) {
        Gauge gauge = getOrCreateGauge(metricName, help);
        if (gauge != null) {
            gauge.labels(labelValue).set(value);
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
