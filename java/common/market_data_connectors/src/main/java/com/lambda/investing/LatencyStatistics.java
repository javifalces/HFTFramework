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
    protected static long WARN_LATENCY_ORDER_REQUEST_MS = 500;
    protected static long WARN_LATENCY_MARKET_DATA_MS = 500;
    protected static long WARN_LATENCY_EXECUTION_REPORT_MS = 500;

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
        long depthTimestamp = depth.getTimestamp();
        long latencyMs = currentTime - depthTimestamp;
        try {
            addLatencyStatistics("depth." + depth.getInstrument() + "." + algorithmInfo, latencyMs);
            if (latencyMs > WARN_LATENCY_MARKET_DATA_MS) {
                String tableLatencies = depth.getLatenciesTable();
                logger.warn("Depth {} with latency {} ms > {} from current time from {} to {}\n{}",
                        depth.getInstrument(), latencyMs, WARN_LATENCY_MARKET_DATA_MS,
                        PrintDate(new Date(depthTimestamp)), PrintDate(new Date(currentTime)), tableLatencies);
            }

        } catch (Exception e) {
            logger.error("error addDepthLatencyStatistics latency statistics", e);
        }
    }

    public void addTradeLatencyStatistics(String algorithmInfo, long currentTime, Trade trade) {
        long tradeTimestamp = trade.getTimestamp();
        long latencyMs = currentTime - tradeTimestamp;
        try {
            addLatencyStatistics("trade." + trade.getInstrument() + "." + algorithmInfo, latencyMs);
            if (latencyMs > WARN_LATENCY_MARKET_DATA_MS) {
                String tableLatencies = trade.getLatenciesTable();
                logger.warn("Trade {} with latency {} ms > {} from current time from {} to {}\n{}",
                        trade.getInstrument(), latencyMs, WARN_LATENCY_MARKET_DATA_MS,
                        PrintDate(new Date(trade.getTimestamp())), PrintDate(new Date(currentTime)), tableLatencies);
            }

        } catch (Exception e) {
            logger.error("error addTradeLatencyStatistics latency statistics", e);
        }
    }

    public void addExecutionReportLatencyStatistics(String algorithmInfo, long currentTime, ExecutionReport executionReport) {
        long executionReportTimestampCreation = executionReport.getTimestampCreation();
        long latencyMs = currentTime - executionReportTimestampCreation;
        try {
            addLatencyStatistics("executionReport." + executionReport.getInstrument() + "." + algorithmInfo, latencyMs);
            if (latencyMs > WARN_LATENCY_EXECUTION_REPORT_MS) {
                String tableLatencies = executionReport.getLatenciesTable();
                logger.warn("ExecutionReport {} with latency {} ms > {} from current time from {} to {}\n{}",
                        executionReport.getInstrument(), latencyMs, WARN_LATENCY_EXECUTION_REPORT_MS,
                        PrintDate(new Date(executionReport.getTimestampCreation())), PrintDate(new Date(currentTime)), tableLatencies);
            }
        } catch (Exception e) {
            logger.error("error addExecutionReportLatencyStatistics latency statistics", e);
        }
    }

    public void addOrderRequestLatencyStatistics(String algorithmInfo, long currentTime, OrderRequest orderRequest) {
        long orderRequestTimestampCreation = orderRequest.getTimestampCreation();
        long latencyMs = currentTime - orderRequestTimestampCreation;
        try {
            addLatencyStatistics("orderRequest." + orderRequest.getInstrument() + "." + algorithmInfo, latencyMs);
            if (latencyMs > WARN_LATENCY_ORDER_REQUEST_MS) {
                String table = orderRequest.getLatenciesTable();
                logger.warn("OrderRequest {} with latency {} ms > {} from creation from {} to {}\n{}", orderRequest, latencyMs, WARN_LATENCY_ORDER_REQUEST_MS, PrintDate(new Date(orderRequest.getTimestampCreation())), PrintDate(new Date(currentTime)), table);
            }
        } catch (Exception e) {
            logger.error("error addOrderRequestLatencyStatistics latency statistics", e);
        }
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
