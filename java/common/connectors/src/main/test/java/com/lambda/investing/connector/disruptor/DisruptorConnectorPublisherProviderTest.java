package com.lambda.investing.connector.disruptor;

import com.google.common.base.Stopwatch;
import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorPublisherProvider;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class DisruptorConnectorPublisherProviderTest implements ConnectorListener {
    DisruptorConnectorPublisherProvider disruptorConnectorPublisherProvider = null;
    ConnectorConfiguration connectorConfiguration = new OrdinaryConnectorConfiguration();
    List<ReceivedItem> lastItemsUpdate = new ArrayList();
    CountDownLatch waiter;

    @Override
    public void onUpdate(ConnectorConfiguration configuration, long timestampReceived, TypeMessage typeMessage, Object content) {
        lastItemsUpdate.add(new ReceivedItem(configuration, timestampReceived, typeMessage, content));
        if (waiter != null) {
            waiter.countDown();
        }
    }

    @AllArgsConstructor
    @Getter
    private class ReceivedItem {
        ConnectorConfiguration configuration;
        long timestampReceived;
        TypeMessage typeMessage;
        Object content;

        @Override
        public String toString() {
            return "ReceivedItem{" +
                    "timestampReceived=" + timestampReceived +
                    ", typeMessage=" + typeMessage +
                    ", content='" + content + '\'' +
                    '}';
        }
    }


    //sometimes we received two messages!
    @Test
    @RepeatedTest(25)
    public void testSendReceiveSimple() throws InterruptedException {
        Stopwatch timer = Stopwatch.createStarted();
        disruptorConnectorPublisherProvider = new DisruptorConnectorPublisherProvider(
                "junit_test", 4096, new BlockingWaitStrategy(), ProducerType.SINGLE);
        disruptorConnectorPublisherProvider.register(connectorConfiguration, this);

        String topic = "topic1";
        TypeMessage typeMessage = TypeMessage.info;
        String message = Configuration.formatLog("message_{}", System.currentTimeMillis());
        System.out.println(message);
        waiter = new CountDownLatch(1);
        lastItemsUpdate.clear();
        disruptorConnectorPublisherProvider.publish(connectorConfiguration, typeMessage, topic, message);
        waiter.await();

        if (lastItemsUpdate.size() > 1) {
            System.out.println(StringUtils.arrayToDelimitedString(lastItemsUpdate.toArray(), ","));
        }

        assertEquals(1, lastItemsUpdate.size());
        ReceivedItem itemReceived = lastItemsUpdate.get(0);
        assertEquals(typeMessage, itemReceived.getTypeMessage());
        assertEquals(message, itemReceived.getContent());
        System.out.println("Method took: " + timer.stop());
    }

    /**
     * Benchmarks Disruptor vs Ordinary connector latency.
     * <p>
     * Fixed issues vs naive approach:
     * 1. Listeners are deregistered after each iteration to avoid O(N) accumulated
     * listener overhead that inflated Disruptor timings.
     * 2. Runs are interleaved (alternating Disruptor / Ordinary per iteration) to
     * eliminate JVM warm-up bias (Disruptor was always measured cold first).
     * 3. A dedicated warm-up phase (fresh instances, no registered listeners) is
     * done before any measurement starts.
     */
    @Test
    public void testDisruptorVsOrdinaryLatency() throws InterruptedException {
        int warmupIterations = 50;
        int iterations = 200;
        String topic = "bench_topic";
        TypeMessage typeMessage = TypeMessage.info;

        ConnectorConfiguration disruptorCfg = new OrdinaryConnectorConfiguration();
        ConnectorConfiguration ordinaryCfg = new OrdinaryConnectorConfiguration();

        DisruptorConnectorPublisherProvider disruptor =
                new DisruptorConnectorPublisherProvider("bench_disruptor", 4096,
                        new BusySpinWaitStrategy(), ProducerType.SINGLE);
        OrdinaryConnectorPublisherProvider ordinary =
                new OrdinaryConnectorPublisherProvider("bench_ordinary", 1, Thread.NORM_PRIORITY);

        ConnectorPublisherAndProvider disruptorAdapter = new ConnectorPublisherAndProvider() {
            public void register(ConnectorConfiguration cfg, ConnectorListener l) {
                disruptor.register(cfg, l);
            }

            public void deregister(ConnectorConfiguration cfg, ConnectorListener l) {
                disruptor.deregister(cfg, l);
            }

            public boolean publish(ConnectorConfiguration cfg, TypeMessage tm, String t, java.io.Serializable m) {
                return disruptor.publish(cfg, tm, t, m);
            }
        };
        ConnectorPublisherAndProvider ordinaryAdapter = new ConnectorPublisherAndProvider() {
            public void register(ConnectorConfiguration cfg, ConnectorListener l) {
                ordinary.register(cfg, l);
            }

            public void deregister(ConnectorConfiguration cfg, ConnectorListener l) {
                ordinary.deregister(cfg, l);
            }

            public boolean publish(ConnectorConfiguration cfg, TypeMessage tm, String t, java.io.Serializable m) {
                return ordinary.publish(cfg, tm, t, m);
            }
        };

        // --- Warm-up: exercise both paths so JIT compiles everything ---
        for (int i = 0; i < warmupIterations; i++) {
            measureLatencyNanos(disruptorAdapter, disruptorCfg, typeMessage, topic, "warmup_" + i);
            measureLatencyNanos(ordinaryAdapter, ordinaryCfg, typeMessage, topic, "warmup_" + i);
        }

        // --- Interleaved measurement ---
        List<Long> disruptorTimes = new ArrayList<>(iterations);
        List<Long> ordinaryTimes = new ArrayList<>(iterations);

        for (int i = 0; i < iterations; i++) {
            // alternate which one goes first to cancel out any residual JIT / cache effects
            if (i % 2 == 0) {
                disruptorTimes.add(measureLatencyNanos(disruptorAdapter, disruptorCfg, typeMessage, topic, "msg_" + i));
                ordinaryTimes.add(measureLatencyNanos(ordinaryAdapter, ordinaryCfg, typeMessage, topic, "msg_" + i));
            } else {
                ordinaryTimes.add(measureLatencyNanos(ordinaryAdapter, ordinaryCfg, typeMessage, topic, "msg_" + i));
                disruptorTimes.add(measureLatencyNanos(disruptorAdapter, disruptorCfg, typeMessage, topic, "msg_" + i));
            }
        }

        // --- Stats (values are in µs) ---
        double disruptorMean = disruptorTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double ordinaryMean = ordinaryTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        Collections.sort(disruptorTimes);
        Collections.sort(ordinaryTimes);
        double disruptorMedian = median(disruptorTimes);
        double ordinaryMedian = median(ordinaryTimes);

        double meanDiffMs = (ordinaryMean - disruptorMean) / 1000.0;
        double medianDiffMs = (ordinaryMedian - disruptorMedian) / 1000.0;

        System.out.println("=== Disruptor (BusySpinWaitStrategy/SINGLE) vs Ordinary Latency Benchmark (" + iterations + " iterations, interleaved) ===");
        System.out.printf("Disruptor  - mean: %.3f µs, median: %.3f µs%n", disruptorMean, disruptorMedian);
        System.out.printf("Ordinary   - mean: %.3f µs, median: %.3f µs%n", ordinaryMean, ordinaryMedian);
        System.out.printf("Difference (Ordinary - Disruptor) -> mean: %.3f ms, median: %.3f ms%n", meanDiffMs, medianDiffMs);
        System.out.printf("Winner: %s%n", disruptorMean < ordinaryMean ? "Disruptor" : "Ordinary");
    }

    /**
     * Measures round-trip latency (publish → onUpdate callback) in microseconds.
     * Registers a single fresh listener, publishes, waits for the callback,
     * then immediately deregisters — keeping exactly 1 active listener per call.
     */
    private long measureLatencyNanos(ConnectorPublisherAndProvider provider,
                                     ConnectorConfiguration cfg,
                                     TypeMessage typeMessage,
                                     String topic,
                                     String message) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long[] holder = new long[1];
        ConnectorListener listener = (c, ts, tm, content) -> {
            holder[0] = System.nanoTime();
            latch.countDown();
        };
        provider.register(cfg, listener);
        long start = System.nanoTime();
        provider.publish(cfg, typeMessage, topic, message);
        latch.await(2, TimeUnit.SECONDS);
        provider.deregister(cfg, listener);
        return TimeUnit.NANOSECONDS.toMicros(holder[0] - start);
    }

    /**
     * Tiny helper interface so measureLatencyNanos accepts both provider types.
     */
    private interface ConnectorPublisherAndProvider {
        void register(ConnectorConfiguration cfg, ConnectorListener listener);

        void deregister(ConnectorConfiguration cfg, ConnectorListener listener);

        boolean publish(ConnectorConfiguration cfg, TypeMessage tm, String topic, java.io.Serializable msg);
    }

    private double median(List<Long> sorted) {
        int n = sorted.size();
        if (n == 0) return 0;
        if (n % 2 == 0) return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
        return sorted.get(n / 2);
    }
}
