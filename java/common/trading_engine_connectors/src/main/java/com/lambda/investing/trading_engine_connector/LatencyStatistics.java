package com.lambda.investing.trading_engine_connector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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


    private void printCurrentStatistics() {
        if (topicToLatency.size() > 0) {
            for (Map.Entry<String, List<Long>> entry : topicToLatency.entrySet()) {
                String topic = entry.getKey();
                List<Long> latency = entry.getValue();
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
                    logger.info("\tLatency {}:\tsize:{}\tmean(ms):{}\t50pct:{}\t75pct:{}\t90pct:{}\t95pct:{}\t99pct:{}\tmax:{}", topic, counter, mean,
                            percentile50, percentile75, percentile90, percentile95, percentile99, maxLatency);
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
