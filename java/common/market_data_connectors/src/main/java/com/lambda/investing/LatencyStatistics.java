package com.lambda.investing;

import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    public LatencyStatistics(String header, long sleepMs) {
        this.header = header;
        this.sleepMs = sleepMs;
        keyToStartDate = new ConcurrentHashMap<>();
        keyToTopic = new ConcurrentHashMap<>();
        topicToLatency = new ConcurrentHashMap<>();
        enable = true;
        if (sleepMs > 0) {
            Thread thread = new Thread(this, "LatencyStatistics");
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
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

        // Track latency from timestamp to timestampBrokerConnector
        if (timestampBrokerConnector > 0) {
            long latency = timestampBrokerConnector - lastReference;
            addLatencyStatistics(prefix + ".toBrokerConnector", latency);
            lastReference = timestampBrokerConnector;
        }

        // Track latency from timestampBrokerConnector to timestampAlgoConnector
        if (timestampAlgoConnector > 0) {
            long latency = timestampAlgoConnector - lastReference;
            addLatencyStatistics(prefix + ".toAlgoConnector", latency);
            lastReference = timestampAlgoConnector;
        }

        // Track latency from timestampAlgoConnector to timestampStrategy
        if (timestampStrategy > 0) {
            long latency = timestampStrategy - lastReference;
            addLatencyStatistics(prefix + ".toStrategy", latency);
            lastReference = timestampStrategy;
        }

        // Track latency from last timestamp to now
        long currentTime = System.currentTimeMillis();
        long latency = currentTime - lastReference;
        addLatencyStatistics(prefix + ".toNow", latency);
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
            Map<String, List<Long>> snapshot = new ConcurrentHashMap<>(topicToLatency);

            for (Map.Entry<String, List<Long>> entry : snapshot.entrySet()) {
                String topic = entry.getKey();
                // if length.topic>50 reduce it after poing to 50 chars and add last characters after -

                if (topic.length() > 40) {
                    String suffixAfterDash = "";
                    int lastDashIndex = topic.lastIndexOf("-");
                    if (lastDashIndex != -1 && lastDashIndex + 1 < topic.length()) {
                        suffixAfterDash = topic.substring(lastDashIndex);
                    }
                    topic = topic.substring(0, 35) + "...-" + suffixAfterDash;
                }
                List<Long> latency = new ArrayList<>(entry.getValue());//copy to avoud concurrent modification
                int counter = latency.size();
                if (counter > 0) {
                    double mean = latency.stream().mapToLong(a -> a).average().orElse(0.0);
                    double maxLatency = latency.stream().mapToLong(a -> a).max().orElse(0);
                    //get percentile 50 75 90 95 99
                    double percentile50 = 0;
                    double percentile75 = 0;
                    double percentile90 = 0;
                    double percentile95 = 0;
                    double percentile99 = 0;

                    if (latency.size() > 0) {
                        percentile50 = latency.stream().sorted().skip((long) (latency.size() * 0.5)).findFirst().orElse(0L);
                        percentile75 = latency.stream().sorted().skip((long) (latency.size() * 0.75)).findFirst().orElse(0L);
                        percentile90 = latency.stream().sorted().skip((long) (latency.size() * 0.9)).findFirst().orElse(0L);
                        percentile95 = latency.stream().sorted().skip((long) (latency.size() * 0.95)).findFirst().orElse(0L);
                        percentile99 = latency.stream().sorted().skip((long) (latency.size() * 0.99)).findFirst().orElse(0L);
                    }
                    //print average and percentiles
                    String topicPadded = String.format("%-60s", topic);
                    logger.info("\tLatency {}:\tsize:{}\tmean(ms):{}\t50pct:{}\t75pct:{}\t90pct:{}\t95pct:{}\t99pct:{}\tmax:{}",
                            topicPadded, counter,
                            String.format("%.2f", mean),
                            String.format("%.2f", percentile50),
                            String.format("%.2f", percentile75),
                            String.format("%.2f", percentile90),
                            String.format("%.2f", percentile95),
                            String.format("%.2f", percentile99),
                            String.format("%.2f", maxLatency));
                }
            }

            if (RESET_STATISTICS_PER_UPDATE) {
                topicToLatency.clear();
                keyToTopic.clear();
                keyToStartDate.clear();
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
