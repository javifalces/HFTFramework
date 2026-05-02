package com.lambda.investing.market_data_connector;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.market_data.Trade;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Latency benchmark comparing {@link ZeroMqMarketDataConnector} (synchronous,
 * caller blocks for the full processing time) against
 * {@link ZeroMqMarketDataConnectorDisruptor} (LMAX Disruptor, caller returns
 * after a single ring-buffer write).
 *
 * <h2>Two latency metrics are measured</h2>
 * <ol>
 *   <li><strong>Producer latency</strong> – how long the "ZeroMq I/O thread"
 *       (the caller of {@code onUpdate}/{@code processUpdate}) is blocked.
 *       This is the number that matters for live trading because a slow
 *       producer misses the next market-data message.
 *       <ul>
 *         <li>Plain    → full {@code processUpdate()} duration (includes
 *             deserialisation + all listener callbacks)</li>
 *         <li>Disruptor → just {@code onUpdate()} duration (ring-buffer
 *             write only; ~100–300 ns)</li>
 *       </ul>
 *   </li>
 *   <li><strong>End-to-end latency</strong> – time from {@code onUpdate} call
 *       to the {@link MarketDataListener#onDepthUpdate} callback.  Plain is
 *       synchronous, so this equals producer latency.  Disruptor adds async
 *       dispatch overhead, so this is larger – that is expected and normal.
 *   </li>
 * </ol>
 *
 * <h2>Slow-consumer scenario</h2>
 * A listener that burns {@value SLOW_CONSUMER_DELAY_NS} ns per message is
 * registered.  For the plain connector the producer is blocked for that delay
 * on every message.  For the Disruptor the producer still returns in < 1 µs.
 */
public class ZeroMqMarketDataConnectorLatencyTest {

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

    private static final String INSTRUMENT = "BTCUSD_TEST";

    private static final ZeroMqConfiguration DUMMY_CFG =
            new ZeroMqConfiguration("localhost", 19999, null);

    // -----------------------------------------------------------------------
    // Inner test-doubles  (no real ZeroMq socket)
    // -----------------------------------------------------------------------

    static class TestConnector extends ZeroMqMarketDataConnector {
        TestConnector() {
            super(DUMMY_CFG, 0);
        }

        @Override
        public void start() { /* skip ZeroMq */ }
    }

    static class TestDisruptorConnector extends ZeroMqMarketDataConnectorDisruptor {
        TestDisruptorConnector() {
            super(DUMMY_CFG, 0, Configuration.ConnectorProviderType.DISRUPTOR_LOW_LATENCY);
        }

        @Override
        public void start() {
            initDisruptor(); /* skip ZeroMq */
        }
    }

    // -----------------------------------------------------------------------
    // Listeners
    // -----------------------------------------------------------------------

    /**
     * Captures end-to-end latency via {@code depth.getTimestampAlgoConnector()}.
     */
    static class E2ELatencyListener implements MarketDataListener {
        final List<Long> latenciesNs = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch latch;

        E2ELatencyListener(int n) {
            latch = new CountDownLatch(n); }

        @Override
        public boolean onDepthUpdate(Depth depth) {
            latenciesNs.add(System.nanoTime() - depth.getTimestampAlgoConnector());
            latch.countDown();
            return true;
        }

        @Override
        public boolean onTradeUpdate(Trade t) {
            return false;
        }

        @Override
        public boolean onCommandUpdate(Command c) {
            return false;
        }

        @Override
        public boolean onInfoUpdate(String h, Object m) {
            return false;
        }

        boolean await(long t, TimeUnit u) throws InterruptedException {
            return latch.await(t, u); }
    }

    /**
     * Same as {@link E2ELatencyListener} but burns {@code delayNs} nanoseconds
     * per message to simulate expensive business logic.
     */
    static class SlowE2ELatencyListener extends E2ELatencyListener {
        private final long delayNs;

        SlowE2ELatencyListener(int n, long delayNs) {
            super(n);
            this.delayNs = delayNs;
        }

        @Override
        public boolean onDepthUpdate(Depth depth) {
            long deadline = System.nanoTime() + delayNs;
            while (System.nanoTime() < deadline) { /* busy-spin */ }
            latenciesNs.add(System.nanoTime() - depth.getTimestampAlgoConnector());
            latch.countDown();
            return true;
        }
    }

    // -----------------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------------

    private TestConnector plainConnector;
    private TestDisruptorConnector disruptorConnector;

    @Before
    public void setUp() {
        AbstractMarketDataProvider.CHECK_TIMESTAMPS_RECEIVED = false;
        plainConnector = new TestConnector();
        plainConnector.start();
        disruptorConnector = new TestDisruptorConnector();
        disruptorConnector.start();
    }

    @After
    public void tearDown() {
        disruptorConnector.stop(); }

    // -----------------------------------------------------------------------
    // Depth factory
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Depth buildDepth(long seqNum) {
        Depth depth = Depth.getInstance();
        depth.setInstrument(INSTRUMENT);
        depth.setTimestamp(seqNum);
        depth.setTimestampBrokerConnector(seqNum);
        depth.setLevels(1);
        depth.setBids(new double[]{100.0});
        depth.setAsks(new double[]{100.1});
        depth.setBidsQuantities(new double[]{1.0});
        depth.setAsksQuantities(new double[]{1.0});
        List<String>[] algo = new List[]{Arrays.asList(Depth.ALGORITHM_INFO_MM) };
        depth.setBidsAlgorithmInfo(algo);
        depth.setAsksAlgorithmInfo(algo);
        depth.setLevelsFromData();
        return depth;
    }

    // -----------------------------------------------------------------------
    // Core benchmark helpers  (return both producer + e2e latencies)
    // -----------------------------------------------------------------------

    /**
     * Runs the plain connector benchmark.
     * Producer and e2e are identical because processing is synchronous.
     *
     * @return [0] = producer/e2e latencies (same list)
     */
    private List<Long>[] benchmarkPlain(int count, E2ELatencyListener listener)
            throws InterruptedException {
        List<Long> producerNs = new ArrayList<>(count);
        plainConnector.register(listener);
        try {
            for (int i = 0; i < count; i++) {
                Depth d = buildDepth(i + 1);
                long t0 = System.nanoTime();
                plainConnector.processUpdate(DUMMY_CFG, t0, TypeMessage.depth, d);
                producerNs.add(System.nanoTime() - t0);  // caller unblocked after full processing
            }
        } finally {
            plainConnector.deregister(listener);
        }
        return new List[]{producerNs, listener.latenciesNs };
    }

    /**
     * Runs the Disruptor benchmark.
     *
     * @return [0] = producer latencies (ring-buffer write only),
     *         [1] = end-to-end latencies (submission → listener callback)
     */
    private List<Long>[] benchmarkDisruptor(int count, E2ELatencyListener listener)
            throws InterruptedException {
        List<Long> producerNs = new ArrayList<>(count);
        disruptorConnector.register(listener);
        try {
            for (int i = 0; i < count; i++) {
                Depth d = buildDepth(i + 1);
                long t0 = System.nanoTime();
                disruptorConnector.onUpdate(DUMMY_CFG, t0, TypeMessage.depth, d);
                producerNs.add(System.nanoTime() - t0);  // caller unblocked after ring-buffer write
            }
            if (!listener.await(30, TimeUnit.SECONDS)) {
                System.err.println("[WARN] Disruptor consumer did not drain within 30 s");
            }
        } finally {
            disruptorConnector.deregister(listener);
        }
        return new List[]{producerNs, listener.latenciesNs };
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
            System.out.printf("  %s – NO DATA%n", label); return; }
        long[] s = samples.stream().mapToLong(Long::longValue).sorted().toArray();
        double mean = samples.stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.printf(
                "  %-46s  mean=%7.0f  p50=%7d  p75=%7d  p90=%7d  p99=%7d  max=%8d  (ns)%n",
                label, mean,
                percentile(s, 50), percentile(s, 75), percentile(s, 90), percentile(s, 99), s[s.length-1]);
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
    public void compareProcessingLatency() throws InterruptedException {

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║   ZeroMqMarketDataConnector vs Disruptor – Latency Benchmark ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        // ── warm-up ──────────────────────────────────────────────────────────
        System.out.printf("%n  Warm-up: %,d messages × 2 connectors ...%n", WARMUP_MESSAGES);
        benchmarkPlain(WARMUP_MESSAGES, new E2ELatencyListener(WARMUP_MESSAGES));
        benchmarkDisruptor(WARMUP_MESSAGES, new E2ELatencyListener(WARMUP_MESSAGES));
        System.out.printf("  Done.%n");

        // ── fast-consumer benchmark ──────────────────────────────────────────
        System.out.printf("%n  Benchmark: %,d messages, fast consumer%n", BENCH_MESSAGES);

        E2ELatencyListener plainFastE2E = new E2ELatencyListener(BENCH_MESSAGES);
        E2ELatencyListener disruptorFastE2E = new E2ELatencyListener(BENCH_MESSAGES);

        List<Long>[] plainFast = benchmarkPlain(BENCH_MESSAGES, plainFastE2E);
        List<Long>[] disruptorFast = benchmarkDisruptor(BENCH_MESSAGES, disruptorFastE2E);

        List<Long> plainProducer = plainFast[0];
        List<Long> plainE2E = plainFast[1];       // same as producer (sync)
        List<Long> disruptorProducer = disruptorFast[0];
        List<Long> disruptorE2E = disruptorFast[1];

        printSection("PRODUCER latency  (how long the I/O thread is blocked)");
        printStats("Plain    processUpdate() incl. listener", plainProducer);
        printStats("Disruptor onUpdate()  ring-buffer write", disruptorProducer);

        printSection("END-TO-END latency  (submission → MarketDataListener.onDepthUpdate)");
        printStats("Plain    (sync: producer == e2e)", plainE2E);
        printStats("Disruptor (async dispatch overhead)", disruptorE2E);

        // ── slow-consumer benchmark ──────────────────────────────────────────
        System.out.printf("%n  Benchmark: %,d messages, slow consumer (%,d ns / msg)%n",
                SLOW_BATCH, SLOW_CONSUMER_DELAY_NS);

        SlowE2ELatencyListener plainSlowE2E = new SlowE2ELatencyListener(SLOW_BATCH, SLOW_CONSUMER_DELAY_NS);
        SlowE2ELatencyListener disruptorSlowE2E = new SlowE2ELatencyListener(SLOW_BATCH, SLOW_CONSUMER_DELAY_NS);

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
        System.out.println("  Note: Plain connector e2e == producer (synchronous call).");
        System.out.println("        Disruptor e2e > producer by design (async dispatch).");
        System.out.println("        The producer latency metric is what matters for HFT I/O threads.");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

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

