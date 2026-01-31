package com.lambda.investing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

                if (topic.length() > 50) {
                    String suffixAfterDash = "";
                    int lastDashIndex = topic.lastIndexOf("-");
                    if (lastDashIndex != -1 && lastDashIndex + 1 < topic.length()) {
                        suffixAfterDash = topic.substring(lastDashIndex);
                    }
                    topic = topic.substring(0, 50) + "...-" + suffixAfterDash;
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
