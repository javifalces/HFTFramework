package com.lambda.investing.algorithmic_trading.observer.web;

import com.lambda.investing.algorithmic_trading.pnl_calculation.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PortfolioSnapshot;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.*;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Standalone runnable demo that starts a {@link WebAlgorithmObserver} and feeds it with
 * randomly generated market data, trades, execution reports, PnL snapshots, custom columns,
 * and messages.
 *
 * <p>Use this class to test / develop the frontend without needing a live algorithm:
 * <pre>
 *   mvn exec:java -Dexec.mainClass="com.lambda.investing.algorithmic_trading.observer.web.WebAppDemo"
 * </pre>
 * Then open {@code http://localhost:9001} in your browser (login: admin / admin).
 */
public class WebAppDemo {

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------

    /**
     * HTTP port the dashboard is served on.
     */
    private static final int PORT = 9001;

    /**
     * Algorithm name shown in the UI.
     */
    private static final String ALGO_INFO = "DemoAlgorithm";

    /**
     * Instruments that will be simulated.
     */
    private static final String INSTRUMENT_BTC = "BTCUSDT";
    private static final String INSTRUMENT_ETH = "ETHUSDT";

    /**
     * Tick interval in milliseconds.
     */
    private static final long TICK_INTERVAL_MS = 500;

    /**
     * PnL sampling interval sent to the backend history store (milliseconds).
     * Kept short in the demo so the timeline chart fills up quickly.
     */
    private static final long PNL_SAMPLE_INTERVAL_MS = 2_000;

    /**
     * Number of order-book levels to generate.
     */
    private static final int BOOK_LEVELS = 5;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private static final Random RNG = new Random();

    private double midBtc = 65_000.0;
    private double midEth = 3_500.0;

    private double realizedPnl = 0.0;
    private double unrealizedPnl = 0.0;
    private double netPosition = 0.0;   // BTC position
    private double totalFees = 0.0;

    /**
     * clientOrderId of the currently open (Active) simulated order, or {@code null}.
     */
    private String openOrderId = null;

    private final WebAlgorithmObserver observer;

    // -----------------------------------------------------------------------
    // Bootstrap
    // -----------------------------------------------------------------------

    public WebAppDemo() throws InterruptedException {
        this.observer = new WebAlgorithmObserver(PORT);
        this.observer.setPaperTrading(true);
        // Use a shorter PnL sample interval so the timeline chart fills up quickly in the demo
        this.observer.setPnlSampleIntervalMs(PNL_SAMPLE_INTERVAL_MS);
    }

    // -----------------------------------------------------------------------
    // Main entry point
    // -----------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║         WebAppDemo  –  frontend stress-test          ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║  Dashboard   → http://localhost:" + PORT + "                  ║");
        System.out.println("║  PnL history → http://localhost:" + PORT + "/api/pnl-history  ║");
        System.out.println("║  Login       → admin / admin                         ║");
        System.out.println("║  PnL sample interval: " + PNL_SAMPLE_INTERVAL_MS + " ms                     ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        WebAppDemo demo = new WebAppDemo();
        demo.run();
    }

    // -----------------------------------------------------------------------
    // Simulation loop
    // -----------------------------------------------------------------------

    private void run() {
        // Push initial params immediately so the UI shows something at first load
        publishParams();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WebAppDemo-tick");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::tick, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);

        // Block the main thread forever
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // -----------------------------------------------------------------------
    // Per-tick logic
    // -----------------------------------------------------------------------

    private void tick() {
        try {
            // --- Random walk mid prices ---
            midBtc = Math.max(1_000, midBtc + (RNG.nextDouble() - 0.5) * 200);
            midEth = Math.max(100, midEth + (RNG.nextDouble() - 0.5) * 20);

            // --- Always publish order-book snapshots ---
            publishDepth(INSTRUMENT_BTC, midBtc);
            publishDepth(INSTRUMENT_ETH, midEth);

            // --- Market trade ~40 % of ticks ---
            if (prob(0.40)) {
                publishTrade(INSTRUMENT_BTC, midBtc);
            }
            if (prob(0.20)) {
                publishTrade(INSTRUMENT_ETH, midEth);
            }

            // --- Execution-report lifecycle  ~25 % of ticks ---
            if (prob(0.25)) {
                publishExecutionReportCycle(INSTRUMENT_BTC, midBtc);
            }

            // --- Update unrealised PnL and publish portfolio ---
            unrealizedPnl = netPosition * midBtc * (RNG.nextDouble() - 0.45) * 0.01;
            publishPortfolio();

            // --- Custom indicator columns ~60 % of ticks ---
            if (prob(0.60)) {
                publishCustomColumns();
            }

            // --- Random message ~4 % of ticks ---
            if (prob(0.04)) {
                publishMessage();
            }

            // --- Refresh params ~2 % of ticks ---
            if (prob(0.02)) {
                publishParams();
            }

        } catch (Exception e) {
            System.err.println("[WebAppDemo] tick error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------
    // Publishers
    // -----------------------------------------------------------------------

    /**
     * Publishes a multi-level order-book snapshot for the given instrument.
     */
    private void publishDepth(String instrument, double mid) {
        double halfSpread = mid * 0.0002;   // 2 bps half-spread
        double levelStep = mid * 0.00015;  // distance between levels

        int levels = BOOK_LEVELS;
        double[] bids = new double[levels];
        double[] asks = new double[levels];
        double[] bidsQty = new double[levels];
        double[] asksQty = new double[levels];

        for (int i = 0; i < levels; i++) {
            bids[i] = mid - halfSpread - i * levelStep;
            asks[i] = mid + halfSpread + i * levelStep;
            bidsQty[i] = 0.05 + RNG.nextDouble() * 3.0;
            asksQty[i] = 0.05 + RNG.nextDouble() * 3.0;
        }

        Depth depth = Depth.getInstance();
        depth.setInstrument(instrument);
        depth.setTimestamp(System.currentTimeMillis());
        depth.setBids(bids);
        depth.setAsks(asks);
        depth.setBidsQuantities(bidsQty);
        depth.setAsksQuantities(asksQty);
        depth.setBidLevels(levels);
        depth.setAskLevels(levels);
        depth.setLevels(levels);

        observer.onUpdateDepth(ALGO_INFO, depth);
    }

    /**
     * Publishes a random market trade near {@code mid}.
     */
    private void publishTrade(String instrument, double mid) {
        Trade trade = Trade.getInstance();
        trade.setInstrument(instrument);
        trade.setTimestamp(System.currentTimeMillis());
        trade.setPrice(mid * (1.0 + (RNG.nextDouble() - 0.5) * 0.0004));
        trade.setQuantity(0.01 + RNG.nextDouble() * 1.0);
        trade.setVerb(RNG.nextBoolean() ? Verb.Buy : Verb.Sell);

        observer.onUpdateTrade(ALGO_INFO, trade);
    }

    /**
     * Simulates a simple order lifecycle:
     * <ul>
     *   <li>If no open order  → sends a new order (Active).</li>
     *   <li>If open order     → randomly CompletelyFilled / PartialFilled / Cancelled.</li>
     * </ul>
     */
    private void publishExecutionReportCycle(String instrument, double price) {
        Verb verb = RNG.nextBoolean() ? Verb.Buy : Verb.Sell;
        double qty = 0.01 + RNG.nextDouble() * 0.2;

        ExecutionReport er = new ExecutionReport();
        er.setInstrument(instrument);
        er.setAlgorithmInfo(ALGO_INFO);
        er.setTimestampCreation(System.currentTimeMillis());
        er.setVerb(verb);
        er.setQuantity(qty);
        er.setPrice(price);

        if (openOrderId == null) {
            // --- Place new order ---
            String clOrdId = "DEMO-" + Long.toHexString(System.currentTimeMillis()).toUpperCase();
            er.setClientOrderId(clOrdId);
            er.setExecutionReportStatus(ExecutionReportStatus.Active);
            openOrderId = clOrdId;

            observer.onOrderRequest(ALGO_INFO, toOrderRequest(er));
            observer.onExecutionReportUpdate(ALGO_INFO, er);

        } else {
            // --- Update existing order ---
            er.setClientOrderId(openOrderId);
            double r = RNG.nextDouble();

            if (r < 0.50) {
                // Completely filled
                er.setExecutionReportStatus(ExecutionReportStatus.CompletelyFilled);
                er.setQuantityFill(qty);
                er.setLastQuantity(qty);
                double sign = verb == Verb.Buy ? 1.0 : -1.0;
                netPosition += sign * qty;
                realizedPnl += sign * qty * price * (RNG.nextDouble() - 0.5) * 0.005;
                totalFees += price * qty * 0.0002;
                openOrderId = null;

            } else if (r < 0.75) {
                // Partially filled
                double fillQty = qty * (0.2 + RNG.nextDouble() * 0.4);
                er.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
                er.setQuantityFill(fillQty);
                er.setLastQuantity(fillQty);

            } else {
                // Cancelled
                er.setExecutionReportStatus(ExecutionReportStatus.Cancelled);
                openOrderId = null;
            }

            observer.onExecutionReportUpdate(ALGO_INFO, er);
        }
    }

    /**
     * Publishes a portfolio snapshot built from current simulated state.
     */
    private void publishPortfolio() {
        // BTC instrument PnL
        PnlSnapshot btcPnl = buildPnlSnapshot(INSTRUMENT_BTC,
                realizedPnl, unrealizedPnl, netPosition, totalFees, netPosition * midBtc);

        // ETH instrument PnL (smaller, decorrelated)
        double ethPos = netPosition * 5;
        double ethRPnl = realizedPnl * 0.3 + (RNG.nextDouble() - 0.5) * 10;
        double ethUPnl = unrealizedPnl * 0.3;
        PnlSnapshot ethPnl = buildPnlSnapshot(INSTRUMENT_ETH,
                ethRPnl, ethUPnl, ethPos, totalFees * 0.3, ethPos * midEth);

        Map<String, PnlSnapshot> map = new LinkedHashMap<>();
        map.put(INSTRUMENT_BTC, btcPnl);
        map.put(INSTRUMENT_ETH, ethPnl);

        PortfolioSnapshot portfolio = new PortfolioSnapshot(ALGO_INFO, map);
        observer.onUpdatePortfolioSnapshot(ALGO_INFO, portfolio);
        observer.onUpdatePnlSnapshot(ALGO_INFO, btcPnl);
    }

    /**
     * Publishes RSI, spread, and inventory-skew custom columns.
     */
    private void publishCustomColumns() {
        long ts = System.currentTimeMillis();
        observer.onCustomColumns(ts, ALGO_INFO, INSTRUMENT_BTC, "rsi", 20 + RNG.nextDouble() * 60);
        observer.onCustomColumns(ts, ALGO_INFO, INSTRUMENT_BTC, "spread_bps", 0.5 + RNG.nextDouble() * 4);
        observer.onCustomColumns(ts, ALGO_INFO, INSTRUMENT_BTC, "inv_skew", (RNG.nextDouble() - 0.5) * 2);
        observer.onCustomColumns(ts, ALGO_INFO, INSTRUMENT_ETH, "rsi", 20 + RNG.nextDouble() * 60);
        observer.onCustomColumns(ts, ALGO_INFO, INSTRUMENT_ETH, "spread_bps", 0.5 + RNG.nextDouble() * 4);
    }

    /**
     * Publishes a random informational message.
     */
    private void publishMessage() {
        String[] msgs = {
                "Risk limit approaching – reducing position",
                "Regime change detected: low-volatility",
                "Spread widening – pausing quoting",
                "Portfolio rebalanced",
                "New trading session started",
                "Funding rate spike detected",
                "Liquidity drop on BTC/USDT L2",
        };
        observer.onUpdateMessage(ALGO_INFO, "INFO", msgs[RNG.nextInt(msgs.length)]);
    }

    /**
     * Publishes the algorithm's parameter map.
     */
    private void publishParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("algorithm", ALGO_INFO);
        params.put("instruments", Arrays.asList(INSTRUMENT_BTC, INSTRUMENT_ETH));
        params.put("maxPosition", 1.0);
        params.put("riskAversion", Math.round((0.10 + RNG.nextDouble() * 0.40) * 1000.0) / 1000.0);
        params.put("targetSpreadBps", Math.round((1.5 + RNG.nextDouble() * 3.0) * 10.0) / 10.0);
        params.put("orderVolatility", Math.round((0.01 + RNG.nextDouble() * 0.04) * 10000.0) / 10000.0);
        params.put("paperTrading", true);
        params.put("demoMode", true);
        observer.onUpdateParams(ALGO_INFO, params);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static PnlSnapshot buildPnlSnapshot(String instrument,
                                                double rPnl, double uPnl, double pos, double fees, double investment) {
        PnlSnapshot s = new PnlSnapshot(instrument);
        s.realizedPnl = rPnl;
        s.unrealizedPnl = uPnl;
        s.totalPnl = rPnl + uPnl;
        s.netPosition = pos;
        s.totalFees = fees;
        s.netInvestment = investment;
        return s;
    }

    private static OrderRequest toOrderRequest(ExecutionReport er) {
        OrderRequest or = new OrderRequest();
        or.setInstrument(er.getInstrument());
        or.setAlgorithmInfo(er.getAlgorithmInfo());
        or.setClientOrderId(er.getClientOrderId());
        or.setVerb(er.getVerb());
        or.setPrice(er.getPrice());
        or.setQuantity(er.getQuantity());
        or.setOrderRequestAction(OrderRequestAction.Send);
        or.setOrderType(OrderType.Limit);
        or.setTimestampCreation(System.currentTimeMillis());
        return or;
    }

    /**
     * Returns {@code true} with the given probability (0 – 1).
     */
    private static boolean prob(double p) {
        return RNG.nextDouble() < p;
    }
}

