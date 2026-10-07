package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.algorithmic_trading.utils.TimeseriesUtils;
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
 *   <li>Slippage (price ticks / bps) – filled price vs. sent price</li>
 *   <li>Implementation shortfall (price ticks / bps / quote currency) – filled price vs. the arrival
 *       midprice (the price prevailing when the order was sent), the industry-standard TCA benchmark
 *       (Perold, 1988). Unlike slippage-vs-sent-price, this also captures the cost of the limit price
 *       chosen by the executor relative to the market at decision time</li>
 *   <li>Midprice movement (price ticks / bps / quote currency) – pure market-drift between the start and
 *       end of the execution, independent of the executor's own price choice (complements slippage /
 *       implementation shortfall in TCA cost decomposition)</li>
 *   <li>Slippage cost (quote currency) – money cost of the slippage vs. sent price</li>
 *   <li>Fees cost (quote currency) – exchange fees paid for the fill, split by maker/taker</li>
 *   <li>All-in cost (bps of notional) – {@code (slippageCost + feesCost) / notional * 10,000}, the
 *       headline TCA number combining market-impact/timing cost and explicit fees</li>
 *   <li>Fill ratio – quantity filled vs. quantity originally requested on the order</li>
 *   <li>Open PnL (quote currency) – running PnL drag from execution costs, i.e. {@code -(cumulative fees
 *       cost) - (cumulative slippage cost)} across all executions recorded so far</li>
 *   <li>Quantity filled / notional traded</li>
 *   <li>Success / rejection counts, and maker vs. taker fill counts</li>
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
    private final List<Double> slippagesBps = new ArrayList<>();
    private final List<Double> midPriceMovementsTicks = new ArrayList<>();
    private final List<Double> midPriceMovementsBps = new ArrayList<>();
    private final List<Double> midPriceMovementCosts = new ArrayList<>();
    private final List<Double> quantitiesFilled = new ArrayList<>();
    private final List<Double> slippageCosts = new ArrayList<>();
    private final List<Double> feesCosts = new ArrayList<>();
    private final List<Double> implementationShortfallsBps = new ArrayList<>();
    private final List<Double> implementationShortfallCosts = new ArrayList<>();
    private final List<Double> fillRatios = new ArrayList<>();
    private final List<Double> notionalsTraded = new ArrayList<>();
    private final List<Double> allInCostsBps = new ArrayList<>();

    private int totalExecutions = 0;
    private int successfulExecutions = 0;
    private int rejectedExecutions = 0;
    private int makerFills = 0;
    private int takerFills = 0;
    private double makerFeesCost = 0.0;
    private double takerFeesCost = 0.0;

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
        recordFillRatio(quantityFill, executionReport.getQuantity());

        ExecutionOutcome outcome = null;
        if (success) {
            outcome = recordSuccessfulExecution(executionReport, midPriceAtFill, quantityFill, timeToExecuteMs);
        } else {
            logger.warn("[{}] execution rejected: verb={} qty={} sentPrice={} reason={} timeToExecute(ms)={}",
                    header, verb, quantityFill, sentPrice,
                    executionReport.getRejectReason(), timeToExecuteMs);
        }

        maybeLogAggregateStatistics(timestampMs);
        return outcome;
    }

    /**
     * Computes and records every per-execution metric for a completely filled execution, logs a single-line
     * summary of them, and returns the subset of metrics ({@link ExecutionOutcome}) that callers publish as
     * custom columns / dashboards.
     */
    private ExecutionOutcome recordSuccessfulExecution(ExecutionReport executionReport, double midPriceAtFill,
                                                       double quantityFill, long timeToExecuteMs) {
        double filledPrice = executionReport.getPrice();
        double priceTick = instrument.getPriceTick();
        double quantityMultiplier = instrument.getQuantityMultiplier();
        double notional = filledPrice * quantityFill * quantityMultiplier;
        notionalsTraded.add(notional);

        PriceDiffMetric slippage = recordSlippage(filledPrice, quantityFill, quantityMultiplier, priceTick);
        PriceDiffMetric implementationShortfall =
                recordImplementationShortfall(filledPrice, quantityFill, quantityMultiplier, priceTick);
        double feesCost = recordFees(filledPrice, quantityFill);
        double allInCostBps = recordAllInCost(notional, slippage.getCost(), feesCost);
        PriceDiffMetric midPriceMovement =
                recordMidPriceMovement(midPriceAtFill, quantityFill, quantityMultiplier, priceTick);

        logExecutionSummary(quantityFill, filledPrice, slippage, implementationShortfall, midPriceMovement,
                feesCost, allInCostBps, timeToExecuteMs);

        double midPriceMovementCost = midPriceMovement != null ? midPriceMovement.getCost() : 0.0;
        return new ExecutionOutcome(timeToExecuteMs, slippage.getCost(), feesCost, midPriceMovementCost);
    }

    /**
     * Records the fill ratio (quantity filled vs. quantity originally requested on the order), when the
     * requested quantity is known. A ratio of {@code 1.0} means the order was fully filled.
     */
    private void recordFillRatio(double quantityFill, double requestedQuantity) {
        if (!Double.isNaN(requestedQuantity) && requestedQuantity > 0) {
            fillRatios.add(Math.min(quantityFill / requestedQuantity, 1.0));
        }
    }

    /**
     * Slippage: filled price vs. the price sent on the order (limit price chosen by the executor, or best
     * bid/ask for market orders). Positive means filled at a worse price than sent.
     */
    private PriceDiffMetric recordSlippage(double filledPrice, double quantityFill, double quantityMultiplier, double priceTick) {
        PriceDiffMetric slippage = PriceDiffMetric.of(verb, sentPrice, filledPrice, quantityFill, quantityMultiplier, priceTick);
        slippagesTicks.add(slippage.getTicks());
        slippagesBps.add(slippage.getBps());
        slippageCosts.add(slippage.getCost());
        return slippage;
    }

    /**
     * Implementation shortfall vs. the arrival midprice (industry-standard TCA benchmark, Perold 1988):
     * positive means the fill was worse than the market price prevailing when the order was sent, capturing
     * both the limit-price choice and any timing/market-impact cost.
     *
     * @return the computed metric, or {@code null} if the arrival midprice is unavailable
     */
    private PriceDiffMetric recordImplementationShortfall(double filledPrice, double quantityFill, double quantityMultiplier, double priceTick) {
        if (Double.isNaN(midPriceAtStart)) {
            return null;
        }
        PriceDiffMetric implementationShortfall =
                PriceDiffMetric.of(verb, midPriceAtStart, filledPrice, quantityFill, quantityMultiplier, priceTick);
        implementationShortfallsBps.add(implementationShortfall.getBps());
        implementationShortfallCosts.add(implementationShortfall.getCost());
        return implementationShortfall;
    }

    /**
     * Midprice movement from the start of the execution (arrival) to the end of the execution (fill):
     * positive means the market moved against us (adverse) while the order was working. This is a pure
     * market-drift metric (independent of the executor's own price choice / slippage) and is the standard
     * TCA decomposition counterpart to slippage / implementation shortfall.
     *
     * @return the computed metric, or {@code null} if either midprice snapshot is unavailable
     */
    private PriceDiffMetric recordMidPriceMovement(double midPriceAtFill, double quantityFill, double quantityMultiplier, double priceTick) {
        if (Double.isNaN(midPriceAtFill) || Double.isNaN(midPriceAtStart)) {
            return null;
        }
        PriceDiffMetric midPriceMovement =
                PriceDiffMetric.of(verb, midPriceAtStart, midPriceAtFill, quantityFill, quantityMultiplier, priceTick);
        midPriceMovementsTicks.add(midPriceMovement.getTicks());
        midPriceMovementsBps.add(midPriceMovement.getBps());
        midPriceMovementCosts.add(midPriceMovement.getCost());
        return midPriceMovement;
    }

    /**
     * Fees paid for this fill (always a positive cost), tracked in total and split by maker vs. taker fills.
     */
    private double recordFees(double filledPrice, double quantityFill) {
        double feesCost = instrument.calculateFee(isTaker, filledPrice, quantityFill);
        feesCosts.add(feesCost);
        if (isTaker) {
            takerFills++;
            takerFeesCost += feesCost;
        } else {
            makerFills++;
            makerFeesCost += feesCost;
        }
        return feesCost;
    }

    /**
     * All-in cost in bps of notional: combines market-impact/timing cost (slippage) and explicit fees, the
     * headline number used in post-trade TCA reports.
     */
    private double recordAllInCost(double notional, double slippageCost, double feesCost) {
        double allInCostBps = notional != 0 ? ((slippageCost + feesCost) / notional) * 10_000.0 : 0.0;
        allInCostsBps.add(allInCostBps);
        return allInCostBps;
    }

    private void logExecutionSummary(double quantityFill, double filledPrice, PriceDiffMetric slippage,
                                     PriceDiffMetric implementationShortfall, PriceDiffMetric midPriceMovement,
                                     double feesCost, double allInCostBps, long timeToExecuteMs) {
        String implShortfallStr = implementationShortfall != null
                ? String.format("%.2f", implementationShortfall.getBps()) : "n/a";
        String midPriceMovementStr = midPriceMovement != null ? midPriceMovement.format() : "n/a";

        logger.info("[{}] execution finished: verb={} qty={} sentPrice={} filledPrice={} slippage(ticks)={} slippage(bps)={} implShortfall(bps)={} midPriceMovement={} slippageCost={} feesCost={} allInCost(bps)={} timeToExecute(ms)={}",
                header, verb, quantityFill, sentPrice, filledPrice,
                String.format("%.2f", slippage.getTicks()),
                String.format("%.2f", slippage.getBps()),
                implShortfallStr,
                midPriceMovementStr,
                String.format("%.4f", slippage.getCost()),
                String.format("%.4f", feesCost),
                String.format("%.2f", allInCostBps),
                timeToExecuteMs);
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

        logger.info("[{}] ExecutorStatistics: total={} success={} rejected={}\t{}\t{}{}{}{}{}{}{}",
                header, totalExecutions, successfulExecutions, rejectedExecutions,
                formatTimingSummary(), formatFillQuantitySummary(),
                formatSlippageSummary(), formatImplementationShortfallSummary(), formatMidPriceMovementSummary(),
                formatCostSummary(), formatAllInCostSummary(), formatFillRatioSummary());
    }

    private String formatTimingSummary() {
        double avgTimeMs = TimeseriesUtils.GetAverageLong(executionTimesMs);
        long maxTimeMs = TimeseriesUtils.GetMaxLong(executionTimesMs);
        double medianTimeMs = TimeseriesUtils.GetPercentile(executionTimesMs, 0.50);
        double p95TimeMs = TimeseriesUtils.GetPercentile(executionTimesMs, 0.95);
        return String.format("avgTime(ms)=%.1f\tmedianTime(ms)=%.1f\tp95Time(ms)=%.1f\tmaxTime(ms)=%d",
                avgTimeMs, medianTimeMs, p95TimeMs, maxTimeMs);
    }

    private String formatFillQuantitySummary() {
        return String.format("avgQty=%.4f", TimeseriesUtils.GetAverage(quantitiesFilled));
    }

    private String formatSlippageSummary() {
        if (slippagesTicks.isEmpty()) {
            return "";
        }
        return String.format("\tslippage(ticks): avg=%.2f max=%.2f\tslippage(bps): avg=%.2f",
                TimeseriesUtils.GetAverage(slippagesTicks), TimeseriesUtils.GetMax(slippagesTicks),
                TimeseriesUtils.GetAverage(slippagesBps));
    }

    private String formatImplementationShortfallSummary() {
        if (implementationShortfallsBps.isEmpty()) {
            return "";
        }
        return String.format("\timplShortfall(bps): avg=%.2f\timplShortfallCost: total=%.4f",
                TimeseriesUtils.GetAverage(implementationShortfallsBps), TimeseriesUtils.GetSum(implementationShortfallCosts));
    }

    private String formatMidPriceMovementSummary() {
        if (midPriceMovementsTicks.isEmpty()) {
            return "";
        }
        return String.format("\tmidPriceMovement(ticks): avg=%.2f max=%.2f\tmidPriceMovement(bps): avg=%.2f\tmidPriceMovementCost: total=%.4f",
                TimeseriesUtils.GetAverage(midPriceMovementsTicks), TimeseriesUtils.GetMax(midPriceMovementsTicks),
                TimeseriesUtils.GetAverage(midPriceMovementsBps), TimeseriesUtils.GetSum(midPriceMovementCosts));
    }

    private String formatCostSummary() {
        if (slippageCosts.isEmpty() && feesCosts.isEmpty()) {
            return "";
        }
        String slippageCostStats = String.format("\tslippageCost: total=%.4f avg=%.4f",
                TimeseriesUtils.GetSum(slippageCosts), TimeseriesUtils.GetAverage(slippageCosts));
        String feesCostStats = String.format("\tfeesCost: total=%.4f avg=%.4f\tmakerFills=%d(%.4f) takerFills=%d(%.4f)",
                TimeseriesUtils.GetSum(feesCosts), TimeseriesUtils.GetAverage(feesCosts),
                makerFills, makerFeesCost, takerFills, takerFeesCost);
        return slippageCostStats + feesCostStats;
    }

    private String formatAllInCostSummary() {
        if (allInCostsBps.isEmpty()) {
            return "";
        }
        return String.format("\tallInCost(bps): avg=%.2f\tnotionalTraded: total=%.2f",
                TimeseriesUtils.GetAverage(allInCostsBps), TimeseriesUtils.GetSum(notionalsTraded));
    }

    private String formatFillRatioSummary() {
        if (fillRatios.isEmpty()) {
            return "";
        }
        return String.format("\tfillRatio: avg=%.4f", TimeseriesUtils.GetAverage(fillRatios));
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

    public List<Double> getMidPriceMovementsBps() {
        return Collections.unmodifiableList(midPriceMovementsBps);
    }

    public List<Double> getMidPriceMovementCosts() {
        return Collections.unmodifiableList(midPriceMovementCosts);
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

    public List<Double> getSlippagesBps() {
        return Collections.unmodifiableList(slippagesBps);
    }

    public List<Double> getImplementationShortfallsBps() {
        return Collections.unmodifiableList(implementationShortfallsBps);
    }

    public List<Double> getImplementationShortfallCosts() {
        return Collections.unmodifiableList(implementationShortfallCosts);
    }

    public List<Double> getFillRatios() {
        return Collections.unmodifiableList(fillRatios);
    }

    public List<Double> getNotionalsTraded() {
        return Collections.unmodifiableList(notionalsTraded);
    }

    public List<Double> getAllInCostsBps() {
        return Collections.unmodifiableList(allInCostsBps);
    }

    public int getMakerFills() {
        return makerFills;
    }

    public int getTakerFills() {
        return takerFills;
    }

    public double getMakerFeesCost() {
        return makerFeesCost;
    }

    public double getTakerFeesCost() {
        return takerFeesCost;
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
        private final double midPriceMovementCost;


        /**
         * @param midPriceMovementCost money cost attributable purely to the market moving between the start
         *                             and the end of the execution (independent of slippage vs. sent price)
         */
        public ExecutionOutcome(long timeToExecuteMs, double slippageCost, double feesCost, double midPriceMovementCost) {
            this.timeToExecuteMs = timeToExecuteMs;
            this.slippageCost = slippageCost;
            this.feesCost = feesCost;
            this.midPriceMovementCost = midPriceMovementCost;
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

        public double getMidPriceMovementCost() {
            return midPriceMovementCost;
        }

    }
}
