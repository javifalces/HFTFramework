package com.lambda.investing.connector.zero_mq;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.model.messaging.TypeMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Latency benchmark comparing {@link ZeroMqProvider} (synchronous,
 * caller blocks for the full processing time) against
 * {@link ZeroMqProviderDisruptor} (LMAX Disruptor, caller returns
 * after a single ring-buffer write).
 *
 * <h2>Two latency metrics are measured</h2>
 * <ol>
 *   <li><strong>Producer latency</strong> – how long the "ZeroMq I/O thread"
 *       (the caller of {@code onUpdate}) is blocked.
 *       This is the number that matters for live trading because a slow
 *       producer misses the next market-data message.
 *       <ul>
 *         <li>Plain    → full {@code onUpdate()} duration (includes
 *             all {@link ConnectorListener} callbacks)</li>
 *         <li>Disruptor → just ring-buffer write only (~100–300 ns)</li>
 *       </ul>
 *   </li>
 *   <li><strong>End-to-end latency</strong> – time from {@code onUpdate} injection
 *       to the {@link ConnectorListener#onUpdate} callback.  Plain is
 *       synchronous, so this equals producer latency.  Disruptor adds async
 *       dispatch overhead, so this is larger – that is expected and normal.
 *   </li>
 * </ol>
 *
 * <h2>Slow-consumer scenario</h2>
 * A listener that burns {@value SLOW_CONSUMER_DELAY_NS} ns per message is
 * registered.  For the plain provider the producer is blocked for that delay
 * on every message.  For the Disruptor the producer still returns in &lt; 1 µs.
 */
public class ZeroMqProviderLatencyTest {

    // -----------------------------------------------------------------------
    // Benchmark parameters
    // -----------------------------------------------------------------------

    private static final int WARMUP_MESSAGES = 2_000;
    private static final int BENCH_MESSAGES = 10_000;

    /**
     * Batch size for the slow-consumer scenario.
     * Must stay below RING_BUFFER_SIZE (default 512) so the Disruptor ring
     * never fills up during the producer loop; otherwise the producer would
     * spin-wait on the ring buffer and inflate its latency.
     */
    private static final int SLOW_BATCH = 200;

    /**
     * Simulated "heavy business logic" delay inside the listener – 200 µs.
     * Chosen so the difference is dramatic but the test finishes quickly.
     */
    private static final long SLOW_CONSUMER_DELAY_NS = 200_000L;

    private static final String TOPIC = "depth.BTCUSD_TEST";

    private static final ZeroMqConfiguration DUMMY_CFG =
            new ZeroMqConfiguration("localhost", 19998, null);

    // -----------------------------------------------------------------------
    // Test doubles  (no real ZeroMq socket I/O)
    // -----------------------------------------------------------------------

    /**
     * Plain provider test double: skips ZeroMq socket binding/connecting,
     * exposes the protected {@code onUpdate} for direct injection.
     */
    static class TestPlainProvider extends ZeroMqProvider {
        TestPlainProvider() {
            super(DUMMY_CFG, 0);
        }

        @Override
        public void start() { /* skip ZeroMq bind/connect */ }

        /**
         * Directly inject a message as if it arrived from the ZeroMq thread.
         */
        void inject(TypeMessage type, Object msg, long timestampNs) throws IOException {
            onUpdate(type, msg, TOPIC, timestampNs);
        }
    }

    /**
     * Disruptor provider test double: initialises only the Disruptor ring-buffer,
     * skips ZeroMq socket binding/connecting and skips the built-in warmup
     * (the test performs its own warmup pass).
     */
    static class TestDisruptorProvider extends ZeroMqProviderDisruptor {
        TestDisruptorProvider() {
            super(DUMMY_CFG, 0, false, Configuration.ConnectorProviderType.DISRUPTOR_LOW_LATENCY);
        }

        @Override
        public void start() {
            initDisruptor(); /* skip ZeroMq */
        }

        /**
         * Skip built-in warmup – the test performs its own warm-up pass.
         */
        @Override
        protected void warmupDisruptor() { /* no-op */ }

        /**
         * Directly inject a message as if it arrived from the ZeroMq thread.
         */
        void inject(TypeMessage type, Object msg, long timestampNs) throws IOException {
            onUpdate(type, msg, TOPIC, timestampNs);
        }
    }

    // -----------------------------------------------------------------------
    // Listeners
    // -----------------------------------------------------------------------

    /**
     * Captures end-to-end latency via the {@code timestampReceived} parameter
     * (populated with {@code System.nanoTime()} by the injector).
     */
    static class E2ELatencyConnectorListener implements ConnectorListener {
        final List<Long> latenciesNs = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch latch;

        E2ELatencyConnectorListener(int n) {
            latch = new CountDownLatch(n);
        }

        @Override
        public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
                             TypeMessage typeMessage, Object content) {
            latenciesNs.add(System.nanoTime() - timestampReceived);
            latch.countDown();
        }

        boolean await(long t, TimeUnit u) throws InterruptedException {
            return latch.await(t, u);
        }
    }

    /**
     * Same as {@link E2ELatencyConnectorListener} but burns {@code delayNs} nanoseconds
     * per message to simulate expensive business logic.
     */
    static class SlowE2ELatencyConnectorListener extends E2ELatencyConnectorListener {
        private final long delayNs;

        SlowE2ELatencyConnectorListener(int n, long delayNs) {
            super(n);
            this.delayNs = delayNs;
        }

        @Override
        public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
                             TypeMessage typeMessage, Object content) {
            long deadline = System.nanoTime() + delayNs;
            while (System.nanoTime() < deadline) { /* busy-spin */ }
            latenciesNs.add(System.nanoTime() - timestampReceived);
            latch.countDown();
        }
    }

    // -----------------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------------

    private TestPlainProvider plainProvider;
    private TestDisruptorProvider disruptorProvider;

    @Before
    public void setUp() {
        plainProvider = new TestPlainProvider();
        plainProvider.start();
        disruptorProvider = new TestDisruptorProvider();
        disruptorProvider.start();
    }

    @After
    public void tearDown() {
        disruptorProvider.stop();
    }

    // -----------------------------------------------------------------------
    // Core benchmark helpers  (return both producer + e2e latencies)
    // -----------------------------------------------------------------------

    /**
     * Runs the plain provider benchmark.
     * Producer and e2e are identical because processing is synchronous.
     *
     * @return [0] = producer latencies, [1] = e2e latencies (same list)
     */
    @SuppressWarnings("unchecked")
    private List<Long>[] benchmarkPlain(int count, E2ELatencyConnectorListener listener)
            throws Exception {
        List<Long> producerNs = new ArrayList<>(count);
        plainProvider.register(DUMMY_CFG, listener);
        try {
            for (int i = 0; i < count; i++) {
                long t0 = System.nanoTime();
                plainProvider.inject(TypeMessage.depth, "msg_" + i, t0);
                producerNs.add(System.nanoTime() - t0); // caller unblocked after full listener fan-out
            }
        } finally {
            plainProvider.deregister(DUMMY_CFG, listener);
        }
        return new List[]{producerNs, listener.latenciesNs};
    }

    /**
     * Runs the Disruptor provider benchmark.
     *
     * @return [0] = producer latencies (ring-buffer write only),
     *         [1] = end-to-end latencies (submission → listener callback)
     */
    @SuppressWarnings("unchecked")
    private List<Long>[] benchmarkDisruptor(int count, E2ELatencyConnectorListener listener)
            throws Exception {
        List<Long> producerNs = new ArrayList<>(count);
        disruptorProvider.register(DUMMY_CFG, listener);
        try {
            for (int i = 0; i < count; i++) {
                long t0 = System.nanoTime();
                disruptorProvider.inject(TypeMessage.depth, "msg_" + i, t0);
                producerNs.add(System.nanoTime() - t0); // caller unblocked after ring-buffer write
            }
            if (!listener.await(30, TimeUnit.SECONDS)) {
                System.err.println("[WARN] Disruptor consumer did not drain within 30 s");
            }
        } finally {
            disruptorProvider.deregister(DUMMY_CFG, listener);
        }
        return new List[]{producerNs, listener.latenciesNs};
    }

    // -----------------------------------------------------------------------
    // Statistics helpers
    // -----------------------------------------------------------------------

    private static long percentile(long[] sorted, double pct) {
        int idx = (int) Math.ceil(pct / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
    }

    private static void printStats(String label, List<Long> samples) {
        if (samples.isEmpty()) {
            System.out.printf("  %s – NO DATA%n", label);
            return;
        }
        long[] s = samples.stream().mapToLong(Long::longValue).sorted().toArray();
        double mean = samples.stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.printf(
                "  %-46s  mean=%7.0f  p50=%7d  p75=%7d  p90=%7d  p99=%7d  max=%8d  (ns)%n",
                label, mean,
                percentile(s, 50), percentile(s, 75), percentile(s, 90), percentile(s, 99), s[s.length - 1]);
    }

    private static void printSection(String title) {
        System.out.println();
        System.out.println("  ── " + title + " ──");
        System.out.printf("  %-46s  %7s  %7s  %7s  %7s  %7s  %8s%n",
                "", "mean", "p50", "p75", "p90", "p99", "max");
    }

    // -----------------------------------------------------------------------
    // Test
    // -----------------------------------------------------------------------

    @Test
    public void compareProcessingLatency() throws Exception {

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   ZeroMqProvider vs ZeroMqProviderDisruptor – Latency Benchmark  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");

        // ── warm-up ─────────────────────────────────────────────────────────
        System.out.printf("%n  Warm-up: %,d messages × 2 providers ...%n", WARMUP_MESSAGES);
        benchmarkPlain(WARMUP_MESSAGES, new E2ELatencyConnectorListener(WARMUP_MESSAGES));
        benchmarkDisruptor(WARMUP_MESSAGES, new E2ELatencyConnectorListener(WARMUP_MESSAGES));
        System.out.printf("  Done.%n");

        // ── fast-consumer benchmark ──────────────────────────────────────────
        System.out.printf("%n  Benchmark: %,d messages, fast consumer%n", BENCH_MESSAGES);

        E2ELatencyConnectorListener plainFastE2E = new E2ELatencyConnectorListener(BENCH_MESSAGES);
        E2ELatencyConnectorListener disruptorFastE2E = new E2ELatencyConnectorListener(BENCH_MESSAGES);

        List<Long>[] plainFast = benchmarkPlain(BENCH_MESSAGES, plainFastE2E);
        List<Long>[] disruptorFast = benchmarkDisruptor(BENCH_MESSAGES, disruptorFastE2E);

        List<Long> plainProducer = plainFast[0];
        List<Long> plainE2E = plainFast[1];         // same as producer (sync)
        List<Long> disruptorProducer = disruptorFast[0];
        List<Long> disruptorE2E = disruptorFast[1];

        printSection("PRODUCER latency  (how long the I/O thread is blocked)");
        printStats("Plain    onUpdate() incl. listener fan-out", plainProducer);
        printStats("Disruptor onUpdate()  ring-buffer write", disruptorProducer);

        printSection("END-TO-END latency  (submission → ConnectorListener.onUpdate)");
        printStats("Plain    (sync: producer == e2e)", plainE2E);
        printStats("Disruptor (async dispatch overhead)", disruptorE2E);

        // ── slow-consumer benchmark ──────────────────────────────────────────
        System.out.printf("%n  Benchmark: %,d messages, slow consumer (%,d ns / msg)%n",
                SLOW_BATCH, SLOW_CONSUMER_DELAY_NS);

        SlowE2ELatencyConnectorListener plainSlowE2E =
                new SlowE2ELatencyConnectorListener(SLOW_BATCH, SLOW_CONSUMER_DELAY_NS);
        SlowE2ELatencyConnectorListener disruptorSlowE2E =
                new SlowE2ELatencyConnectorListener(SLOW_BATCH, SLOW_CONSUMER_DELAY_NS);

        List<Long>[] plainSlow = benchmarkPlain(SLOW_BATCH, plainSlowE2E);
        List<Long>[] disruptorSlow = benchmarkDisruptor(SLOW_BATCH, disruptorSlowE2E);

        printSection("PRODUCER latency with SLOW consumer  ← key Disruptor advantage");
        printStats("Plain    blocked by slow listener", plainSlow[0]);
        printStats("Disruptor decoupled from slow listener", disruptorSlow[0]);

        // ── summary ──────────────────────────────────────────────────────────
        double plainProdMean = plainProducer.stream().mapToLong(Long::longValue).average().orElse(1);
        double disruptorProdMean = disruptorProducer.stream().mapToLong(Long::longValue).average().orElse(1);
        double plainSlowMean = plainSlow[0].stream().mapToLong(Long::longValue).average().orElse(1);
        double disruptorSlowMean = disruptorSlow[0].stream().mapToLong(Long::longValue).average().orElse(1);

        System.out.println();
        System.out.println("  ── Summary ──");
        System.out.printf("  Fast consumer – producer latency:  Plain=%,.0f ns   Disruptor=%,.0f ns%n",
                plainProdMean, disruptorProdMean);
        if (disruptorProdMean < plainProdMean)
            System.out.printf("    → Disruptor producer is %.1fx faster than plain%n",
                    plainProdMean / disruptorProdMean);
        else
            System.out.printf("    → Plain producer is %.1fx faster than Disruptor (fast-consumer case is expected)%n",
                    disruptorProdMean / plainProdMean);

        System.out.printf("  Slow consumer – producer latency:  Plain=%,.0f ns   Disruptor=%,.0f ns%n",
                plainSlowMean, disruptorSlowMean);
        if (disruptorSlowMean < plainSlowMean)
            System.out.printf("    → Disruptor producer is %.1fx faster than plain (Disruptor's real advantage!)%n",
                    plainSlowMean / disruptorSlowMean);

        System.out.println();
        System.out.println("  Note: Plain provider e2e == producer (synchronous call).");
        System.out.println("        Disruptor e2e > producer by design (async dispatch).");
        System.out.println("        The producer latency metric is what matters for HFT I/O threads.");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");

        // ── assertions ───────────────────────────────────────────────────────
        Assert.assertEquals("Plain fast: sample count", BENCH_MESSAGES, plainProducer.size());
        Assert.assertEquals("Disruptor fast: sample count", BENCH_MESSAGES, disruptorProducer.size());
        Assert.assertEquals("Plain slow: sample count", SLOW_BATCH, plainSlow[0].size());
        Assert.assertEquals("Disruptor slow: sample count", SLOW_BATCH, disruptorSlow[0].size());

        // With a 200 µs slow consumer the Disruptor producer must be at least 10× faster
        Assert.assertTrue(
                "Disruptor producer should be at least 10× faster than plain under slow consumer",
                plainSlowMean / disruptorSlowMean >= 10.0);
    }
}

