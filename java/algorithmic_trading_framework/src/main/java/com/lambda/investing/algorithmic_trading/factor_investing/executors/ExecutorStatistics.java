package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.Verb;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks and logs statistics for each execution performed by an {@link AbstractExecutor}.
 * <p>
 * Recorded metrics per execution:
 * <ul>
 *   <li>Time to execute (ms) – from order sent to completely filled</li>
 *   <li>Slippage (price ticks) – filled price vs. sent price</li>
 *   <li>Midprice movement (price ticks) – change in midprice during execution</li>
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

    // Accumulated statistics (reset between log intervals)
    private final List<Long> executionTimesMs = new ArrayList<>();
    private final List<Double> slippagesTicks = new ArrayList<>();
    private final List<Double> midPriceMovementsTicks = new ArrayList<>();
    private final List<Double> quantitiesFilled = new ArrayList<>();

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
     *
     * @param timestampMs current time in milliseconds
     * @param verb        Buy or Sell
     * @param sentPrice   price included in the order request (limit price, or best bid/ask for market orders)
     * @param midPrice    midprice at the time the order was sent
     */
    public synchronized void onExecutionStarted(long timestampMs, Verb verb, double sentPrice, double midPrice) {
        this.executionStartTimestampMs = timestampMs;
        this.midPriceAtStart = midPrice;
        this.sentPrice = sentPrice;
        this.verb = verb;
    }

    /**
     * Called when an execution finishes (completely filled or rejected).
     *
     * @param timestampMs       current time in milliseconds
     * @param executionReport   the final execution report
     * @param midPriceAtFill    midprice at the time of fill / rejection
     */
    public synchronized void onExecutionFinished(long timestampMs, ExecutionReport executionReport, double midPriceAtFill) {
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

        if (success) {
            double filledPrice = executionReport.getPrice();
            double priceTick = instrument.getPriceTick();

            // Slippage in ticks: positive means filled at a worse price than sent
            double slippage = filledPrice - sentPrice;
            if (verb == Verb.Sell) {
                slippage = -slippage;
            }
            double slippageTicks = slippage / priceTick;
            slippagesTicks.add(slippageTicks);

            // Midprice movement in ticks: positive means market moved against us during execution
            double midPriceMovement = midPriceAtFill - midPriceAtStart;
            if (verb == Verb.Sell) {
                midPriceMovement = -midPriceMovement;
            }
            double midPriceMovementTicks = midPriceMovement / priceTick;
            midPriceMovementsTicks.add(midPriceMovementTicks);

            logger.info("[{}] execution finished: verb={} qty={} sentPrice={} filledPrice={} slippage(ticks)={} midPriceMovement(ticks)={} timeToExecute(ms)={}",
                    header, verb, quantityFill, sentPrice, filledPrice,
                    String.format("%.2f", slippageTicks),
                    String.format("%.2f", midPriceMovementTicks),
                    timeToExecuteMs);
        } else {
            logger.warn("[{}] execution rejected: verb={} qty={} sentPrice={} reason={} timeToExecute(ms)={}",
                    header, verb, quantityFill, sentPrice,
                    executionReport.getRejectReason(), timeToExecuteMs);
        }

        maybeLogAggregateStatistics(timestampMs);
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

        logger.info("[{}] ExecutorStatistics: total={} success={} rejected={}\tavgTime(ms)={}\tmaxTime(ms)={}\tavgQty={}{}{}",
                header, totalExecutions, successfulExecutions, rejectedExecutions,
                String.format("%.1f", avgTimeMs), maxTimeMs, String.format("%.4f", avgQty),
                slippageStats, midPriceStats);
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
        return executionTimesMs;
    }

    public List<Double> getSlippagesTicks() {
        return slippagesTicks;
    }

    public List<Double> getMidPriceMovementsTicks() {
        return midPriceMovementsTicks;
    }

    public List<Double> getQuantitiesFilled() {
        return quantitiesFilled;
    }
}
