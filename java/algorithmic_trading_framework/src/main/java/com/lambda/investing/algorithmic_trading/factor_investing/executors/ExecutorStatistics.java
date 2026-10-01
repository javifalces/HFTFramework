package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.Verb;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks and logs statistics for each execution performed by an {@link AbstractExecutor}.
 * <p>
 * Recorded metrics per execution:
 * <ul>
 *   <li>Time to execute (ms) – from order sent to completely filled</li>
 *   <li>Slippage (price ticks) – filled price vs. sent price</li>
 *   <li>Midprice movement (price ticks) – change in midprice during execution</li>
 *   <li>Slippage cost (quote currency) – money cost of the slippage vs. sent price</li>
 *   <li>Fees cost (quote currency) – exchange fees paid for the fill</li>
 *   <li>Open PnL (quote currency) – running PnL drag from execution costs, i.e. {@code -(cumulative fees
 *       cost) - (cumulative slippage cost)} across all executions recorded so far</li>
 *   <li>Quantity filled</li>
 *   <li>Success / rejection counts</li>
 * </ul>
 * Aggregate statistics are logged after every execution and at a configurable periodic interval.
 */
public class ExecutorStatistics {

    private static final long DEFAULT_LOG_INTERVAL_MS = 60_000L;

    protected Logger logger = LogManager.getLogger(ExecutorStatistics.class);

    private final String header;
    private final Instrument instrument;
    private final long logIntervalMs;

    // Per-execution start state
    private long executionStartTimestampMs;
    private double midPriceAtStart;
    private double sentPrice;
    private Verb verb;
    private boolean isTaker;

    // Accumulated statistics (reset between log intervals)
    private final List<Long> executionTimesMs = new ArrayList<>();
    private final List<Double> slippagesTicks = new ArrayList<>();
    private final List<Double> midPriceMovementsTicks = new ArrayList<>();
    private final List<Double> quantitiesFilled = new ArrayList<>();
    private final List<Double> slippageCosts = new ArrayList<>();
    private final List<Double> feesCosts = new ArrayList<>();

    // Running totals used to compute openPnl
    private double cumulativeSlippageCost = 0.0;
    private double cumulativeFeesCost = 0.0;

    private int totalExecutions = 0;
    private int successfulExecutions = 0;
    private int rejectedExecutions = 0;

    private long lastLogTimestampMs = 0;

    public ExecutorStatistics(String header, Instrument instrument) {
        this(header, instrument, DEFAULT_LOG_INTERVAL_MS);
    }

    public ExecutorStatistics(String header, Instrument instrument, long logIntervalMs) {
        this.header = header;
        this.instrument = instrument;
        this.logIntervalMs = logIntervalMs;
    }

    /**
     * Called when an execution starts (i.e. the order has been sent to the exchange).
     * Equivalent to {@link #onExecutionStarted(long, Verb, double, double, boolean)} assuming a taker fill.
     *
     * @param timestampMs current time in milliseconds
     * @param verb        Buy or Sell
     * @param sentPrice   price included in the order request (limit price, or best bid/ask for market orders)
     * @param midPrice    midprice at the time the order was sent
     */
    public synchronized void onExecutionStarted(long timestampMs, Verb verb, double sentPrice, double midPrice) {
        onExecutionStarted(timestampMs, verb, sentPrice, midPrice, true);
    }

    /**
     * Called when an execution starts (i.e. the order has been sent to the exchange).
     *
     * @param timestampMs current time in milliseconds
     * @param verb        Buy or Sell
     * @param sentPrice   price included in the order request (limit price, or best bid/ask for market orders)
     * @param midPrice    midprice at the time the order was sent
     * @param isTaker     whether the resulting fill is expected to pay taker fees (true) or maker fees (false)
     */
    public synchronized void onExecutionStarted(long timestampMs, Verb verb, double sentPrice, double midPrice, boolean isTaker) {
        this.executionStartTimestampMs = timestampMs;
        this.midPriceAtStart = midPrice;
        this.sentPrice = sentPrice;
        this.verb = verb;
        this.isTaker = isTaker;
    }

    /**
     * Called when an execution finishes (completely filled or rejected).
     *
     * @param timestampMs       current time in milliseconds
     * @param executionReport   the final execution report
     * @param midPriceAtFill    midprice at the time of fill / rejection
     * @return the metrics computed for this execution, or {@code null} if the execution was not successfully filled
     */
    public synchronized ExecutionOutcome onExecutionFinished(long timestampMs, ExecutionReport executionReport, double midPriceAtFill) {
        totalExecutions++;
        boolean success = executionReport.getExecutionReportStatus() == ExecutionReportStatus.CompletelyFilled;
        if (success) {
            successfulExecutions++;
        } else {
            rejectedExecutions++;
        }

        long timeToExecuteMs = timestampMs - executionStartTimestampMs;
        executionTimesMs.add(timeToExecuteMs);

        double quantityFill = executionReport.getQuantityFill();
        quantitiesFilled.add(quantityFill);

        ExecutionOutcome outcome = null;
        if (success) {
            double filledPrice = executionReport.getPrice();
            double priceTick = instrument.getPriceTick();
            double quantityMultiplier = instrument.getQuantityMultiplier();

            // Slippage in ticks/money: positive means filled at a worse price than sent
            double slippage = filledPrice - sentPrice;
            if (verb == Verb.Sell) {
                slippage = -slippage;
            }
            double slippageTicks = slippage / priceTick;
            slippagesTicks.add(slippageTicks);
            double slippageCost = slippage * quantityFill * quantityMultiplier;
            slippageCosts.add(slippageCost);

            // Fees paid for this fill (always a positive cost)
            double feesCost = instrument.calculateFee(isTaker, filledPrice, quantityFill);
            feesCosts.add(feesCost);

            // Midprice movement in ticks: positive means market moved against us during execution
            // Only recorded when both midprice snapshots are available
            String midPriceMovementStr = "n/a";
            if (!Double.isNaN(midPriceAtFill) && !Double.isNaN(midPriceAtStart)) {
                double midPriceMovement = midPriceAtFill - midPriceAtStart;
                if (verb == Verb.Sell) {
                    midPriceMovement = -midPriceMovement;
                }
                double midPriceMovementTicks = midPriceMovement / priceTick;
                midPriceMovementsTicks.add(midPriceMovementTicks);
                midPriceMovementStr = String.format("%.2f", midPriceMovementTicks);
            }


            logger.info("[{}] execution finished: verb={} qty={} sentPrice={} filledPrice={} slippage(ticks)={} midPriceMovement(ticks)={} slippageCost={} feesCost={} timeToExecute(ms)={}",
                    header, verb, quantityFill, sentPrice, filledPrice,
                    String.format("%.2f", slippageTicks),
                    midPriceMovementStr,
                    String.format("%.4f", slippageCost),
                    String.format("%.4f", feesCost),
                    timeToExecuteMs);

            outcome = new ExecutionOutcome(timeToExecuteMs, slippageCost, feesCost);
        } else {
            logger.warn("[{}] execution rejected: verb={} qty={} sentPrice={} reason={} timeToExecute(ms)={}",
                    header, verb, quantityFill, sentPrice,
                    executionReport.getRejectReason(), timeToExecuteMs);
        }

        maybeLogAggregateStatistics(timestampMs);
        return outcome;
    }

    private void maybeLogAggregateStatistics(long currentTimestampMs) {
        if (currentTimestampMs - lastLogTimestampMs >= logIntervalMs) {
            logAggregateStatistics();
            lastLogTimestampMs = currentTimestampMs;
        }
    }

    /**
     * Logs aggregate statistics for all executions recorded so far.
     */
    public synchronized void logAggregateStatistics() {
        if (totalExecutions == 0) {
            return;
        }

        double avgTimeMs = executionTimesMs.stream().mapToLong(l -> l).average().orElse(0.0);
        long maxTimeMs = executionTimesMs.stream().mapToLong(l -> l).max().orElse(0L);

        double avgQty = quantitiesFilled.stream().mapToDouble(d -> d).average().orElse(0.0);

        String slippageStats = "";
        if (!slippagesTicks.isEmpty()) {
            double avgSlippage = slippagesTicks.stream().mapToDouble(d -> d).average().orElse(0.0);
            double maxSlippage = slippagesTicks.stream().mapToDouble(d -> d).max().orElse(0.0);
            slippageStats = String.format("\tslippage(ticks): avg=%.2f max=%.2f", avgSlippage, maxSlippage);
        }

        String midPriceStats = "";
        if (!midPriceMovementsTicks.isEmpty()) {
            double avgMidPriceMovement = midPriceMovementsTicks.stream().mapToDouble(d -> d).average().orElse(0.0);
            double maxMidPriceMovement = midPriceMovementsTicks.stream().mapToDouble(d -> d).max().orElse(0.0);
            midPriceStats = String.format("\tmidPriceMovement(ticks): avg=%.2f max=%.2f", avgMidPriceMovement, maxMidPriceMovement);
        }

        String slippageCostStats = "";
        if (!slippageCosts.isEmpty()) {
            double totalSlippageCost = slippageCosts.stream().mapToDouble(d -> d).sum();
            double avgSlippageCost = slippageCosts.stream().mapToDouble(d -> d).average().orElse(0.0);
            slippageCostStats = String.format("\tslippageCost: total=%.4f avg=%.4f", totalSlippageCost, avgSlippageCost);
        }

        String feesCostStats = "";
        if (!feesCosts.isEmpty()) {
            double totalFeesCost = feesCosts.stream().mapToDouble(d -> d).sum();
            double avgFeesCost = feesCosts.stream().mapToDouble(d -> d).average().orElse(0.0);
            feesCostStats = String.format("\tfeesCost: total=%.4f avg=%.4f", totalFeesCost, avgFeesCost);
        }


        logger.info("[{}] ExecutorStatistics: total={} success={} rejected={}\tavgTime(ms)={}\tmaxTime(ms)={}\tavgQty={}{}{}{}{}",
                header, totalExecutions, successfulExecutions, rejectedExecutions,
                String.format("%.1f", avgTimeMs), maxTimeMs, String.format("%.4f", avgQty),
                slippageStats, midPriceStats, slippageCostStats, feesCostStats);
    }

    public int getTotalExecutions() {
        return totalExecutions;
    }

    public int getSuccessfulExecutions() {
        return successfulExecutions;
    }

    public int getRejectedExecutions() {
        return rejectedExecutions;
    }

    public List<Long> getExecutionTimesMs() {
        return Collections.unmodifiableList(executionTimesMs);
    }

    public List<Double> getSlippagesTicks() {
        return Collections.unmodifiableList(slippagesTicks);
    }

    public List<Double> getMidPriceMovementsTicks() {
        return Collections.unmodifiableList(midPriceMovementsTicks);
    }

    public List<Double> getQuantitiesFilled() {
        return Collections.unmodifiableList(quantitiesFilled);
    }

    public List<Double> getSlippageCosts() {
        return Collections.unmodifiableList(slippageCosts);
    }

    public List<Double> getFeesCosts() {
        return Collections.unmodifiableList(feesCosts);
    }

    /**
     * Metrics computed for a single successful execution, returned by
     * {@link #onExecutionFinished(long, ExecutionReport, double)} so callers (e.g. {@link AbstractExecutor})
     * can publish them as custom columns / dashboards.
     */
    public static class ExecutionOutcome {
        private final long timeToExecuteMs;
        private final double slippageCost;
        private final double feesCost;

        public ExecutionOutcome(long timeToExecuteMs, double slippageCost, double feesCost) {
            this.timeToExecuteMs = timeToExecuteMs;
            this.slippageCost = slippageCost;
            this.feesCost = feesCost;
        }

        public long getTimeToExecuteMs() {
            return timeToExecuteMs;
        }

        public double getSlippageCost() {
            return slippageCost;
        }

        public double getFeesCost() {
            return feesCost;
        }

    }
}
