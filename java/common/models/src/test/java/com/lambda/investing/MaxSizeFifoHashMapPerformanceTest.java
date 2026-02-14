package com.lambda.investing;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.Random;

/**
 * Performance test for MaxSizeFifoHashMap optimizations.
 * Tests the impact of StampedLock optimistic reads in high-frequency scenarios.
 */
public class MaxSizeFifoHashMapPerformanceTest {

    private static final int MAP_SIZE = 10000;
    private static final int NUM_THREADS = 8;
    private static final int OPERATIONS_PER_THREAD = 100_000;

    @Test
    public void testReadHeavyWorkload() throws InterruptedException {
        MaxSizeFifoHashMap<Integer, String> map = new MaxSizeFifoHashMap<>(MAP_SIZE);

        // Pre-populate the map
        for (int i = 0; i < MAP_SIZE / 2; i++) {
            map.put(i, "Value-" + i);
        }

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        Random random = new Random();

        long startTime = System.nanoTime();

        // Simulate read-heavy workload (90% reads, 10% writes)
        for (int t = 0; t < NUM_THREADS; t++) {
            executor.submit(() -> {
                Random threadRandom = new Random(random.nextInt());
                for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                    int key = threadRandom.nextInt(MAP_SIZE);

                    if (threadRandom.nextInt(10) < 9) {
                        // Read operation (90%)
                        map.get(key);
                    } else {
                        // Write operation (10%)
                        map.put(key, "Value-" + key);
                    }
                }
                latch.countDown();
            });
        }

        latch.await();
        long endTime = System.nanoTime();
        executor.shutdown();

        long durationMs = (endTime - startTime) / 1_000_000;
        long totalOps = (long) NUM_THREADS * OPERATIONS_PER_THREAD;
        double opsPerSecond = (totalOps * 1000.0) / durationMs;
        double avgLatencyNs = (double) (endTime - startTime) / totalOps;

        System.out.println("=== MaxSizeFifoHashMap Performance Test (Read-Heavy) ===");
        System.out.println("Map Size: " + MAP_SIZE);
        System.out.println("Threads: " + NUM_THREADS);
        System.out.println("Operations per thread: " + OPERATIONS_PER_THREAD);
        System.out.println("Total operations: " + totalOps);
        System.out.println("Duration: " + durationMs + " ms");
        System.out.println("Throughput: " + String.format("%.2f", opsPerSecond) + " ops/sec");
        System.out.println("Average latency: " + String.format("%.2f", avgLatencyNs) + " ns");
        System.out.println("Final map size: " + map.size());
    }

    @Test
    public void testWriteHeavyWorkload() throws InterruptedException {
        MaxSizeFifoHashMap<Integer, String> map = new MaxSizeFifoHashMap<>(MAP_SIZE);

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        Random random = new Random();

        long startTime = System.nanoTime();

        // Simulate write-heavy workload (30% reads, 70% writes)
        for (int t = 0; t < NUM_THREADS; t++) {
            executor.submit(() -> {
                Random threadRandom = new Random(random.nextInt());
                for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                    int key = threadRandom.nextInt(MAP_SIZE);

                    if (threadRandom.nextInt(10) < 3) {
                        // Read operation (30%)
                        map.get(key);
                    } else {
                        // Write operation (70%)
                        map.put(key, "Value-" + key);
                    }
                }
                latch.countDown();
            });
        }

        latch.await();
        long endTime = System.nanoTime();
        executor.shutdown();

        long durationMs = (endTime - startTime) / 1_000_000;
        long totalOps = (long) NUM_THREADS * OPERATIONS_PER_THREAD;
        double opsPerSecond = (totalOps * 1000.0) / durationMs;
        double avgLatencyNs = (double) (endTime - startTime) / totalOps;

        System.out.println("=== MaxSizeFifoHashMap Performance Test (Write-Heavy) ===");
        System.out.println("Map Size: " + MAP_SIZE);
        System.out.println("Threads: " + NUM_THREADS);
        System.out.println("Operations per thread: " + OPERATIONS_PER_THREAD);
        System.out.println("Total operations: " + totalOps);
        System.out.println("Duration: " + durationMs + " ms");
        System.out.println("Throughput: " + String.format("%.2f", opsPerSecond) + " ops/sec");
        System.out.println("Average latency: " + String.format("%.2f", avgLatencyNs) + " ns");
        System.out.println("Final map size: " + map.size());
    }

    @Test
    public void testBatchPutPerformance() {
        MaxSizeFifoHashMap<Integer, String> map = new MaxSizeFifoHashMap<>(MAP_SIZE);
        int batchSize = 1000;

        // Test individual puts
        long startTime = System.nanoTime();
        for (int i = 0; i < batchSize; i++) {
            map.put(i, "Value-" + i);
        }
        long individualPutTime = System.nanoTime() - startTime;

        map.clear();

        // Test batch put
        ConcurrentHashMap<Integer, String> batch = new ConcurrentHashMap<>();
        for (int i = 0; i < batchSize; i++) {
            batch.put(i, "Value-" + i);
        }

        startTime = System.nanoTime();
        map.putAll(batch);
        long batchPutTime = System.nanoTime() - startTime;

        System.out.println("=== MaxSizeFifoHashMap Batch Put Performance ===");
        System.out.println("Batch size: " + batchSize);
        System.out.println("Individual put() calls: " + individualPutTime / 1_000 + " μs");
        System.out.println("Single putAll() call: " + batchPutTime / 1_000 + " μs");
        System.out.println("Speedup: " + String.format("%.2f", (double) individualPutTime / batchPutTime) + "x");
    }

    @Test
    public void testLazyRemovePerformance() throws InterruptedException {
        MaxSizeFifoHashMap<Integer, String> map = new MaxSizeFifoHashMap<>(MAP_SIZE);

        // Pre-populate
        for (int i = 0; i < 100; i++) {
            map.put(i, "Value-" + i);
        }

        long startTime = System.nanoTime();

        // Schedule lazy removals
        for (int i = 0; i < 100; i++) {
            map.lazyRemove(i, 100); // Remove after 100ms
        }

        long scheduleTime = System.nanoTime() - startTime;

        System.out.println("=== MaxSizeFifoHashMap Lazy Remove Performance ===");
        System.out.println("Scheduled 100 lazy removals in: " + scheduleTime / 1_000 + " μs");
        System.out.println("Average scheduling time: " + scheduleTime / 100 / 1_000 + " μs per removal");
        System.out.println("Pending deletions: " + map.getPendingDeletionsCount());

        // Wait for deletions to complete
        Thread.sleep(150);

        System.out.println("Map size after deletions: " + map.size());
        System.out.println("Pending deletions after completion: " + map.getPendingDeletionsCount());
    }
}

