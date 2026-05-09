package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.time_service.TimeServiceIfc;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.Verb;

/**
 * Volume Weighted Average Price (VWAP) Executor.
 * <p>
 * Participates in market volume at a configurable rate ({@code participationRate}).
 * Each time a market trade is observed via {@link #onTradeUpdate}, the executor accumulates
 * a participation quota ({@code tradeQty * participationRate}). When the accumulated quota
 * reaches a minimum child-order size, a limit order is submitted at the current best price.
 * <p>
 * A time-based fallback ensures execution progresses even in low-volume markets:
 * at each depth update the executor checks whether its time-proportional target is below
 * the actual filled amount, and if so sends a catch-up order.
 * <p>
 * Execution statistics track the overall execution from arrival to final fill.
 */
public class VWAPExecutor extends AbstractExecutor {

    // Configuration
    private final long totalDurationMs;
    private final double participationRate; // [0,1] – fraction of observed market volume to execute
    private final double minChildOrderQty;  // minimum size for a single child order

    // State per execution
    private long executionStartTimestampMs;
    private Verb currentVerb;
    private double totalQuantity;

    // Volume / fill tracking
    private double pendingChildQty;     // accumulated participation quota not yet sent
    private double totalFilledQuantity;
    private double totalFilledValue;    // for VWAP calculation

    // Active child-order tracking
    private String activeClientOrderId;
    private String activeConfirmedClientOrderId;

    public VWAPExecutor(TimeServiceIfc timeServiceIfc, String algorithmInfo, Instrument instrument,
                        AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
                        long totalDurationMs, double participationRate) {
        this(timeServiceIfc, algorithmInfo, instrument, algorithmConnectorConfiguration,
                totalDurationMs, participationRate, 0.0);
    }

    /**
     * @param totalDurationMs     total time window for execution in milliseconds
     * @param participationRate   fraction [0, 1] of observed market volume to participate in
     * @param minChildOrderQty    minimum quantity for a single child order (use 0 to default to instrument qty tick)
     */
    public VWAPExecutor(TimeServiceIfc timeServiceIfc, String algorithmInfo, Instrument instrument,
                        AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
                        long totalDurationMs, double participationRate, double minChildOrderQty) {
        super(timeServiceIfc, algorithmInfo, instrument, algorithmConnectorConfiguration);
        if (participationRate <= 0.0 || participationRate > 1.0) {
            throw new IllegalArgumentException("participationRate must be in (0, 1]");
        }
        if (totalDurationMs <= 0) throw new IllegalArgumentException("totalDurationMs must be positive");
        this.totalDurationMs = totalDurationMs;
        this.participationRate = participationRate;
        this.minChildOrderQty = (minChildOrderQty > 0) ? minChildOrderQty : instrument.getQuantityTick();
    }

    @Override
    public boolean increasePosition(long timestamp, Verb verb, double quantity, double price) {
        if (isExecuting) {
            long elapsedMs = (timestamp - isExecutingSince.getTime());
            logger.error("{} {} on {} can't increasePosition when isExecuting since {} [{}< timeout {} ms]",
                    getCurrentTime(), this.instrument, this.algorithmInfo, isExecutingSince, elapsedMs, timeoutIsExecutingMs);
            return false;
        }

        currentVerb = verb;
        totalQuantity = quantity;
        pendingChildQty = 0.0;
        totalFilledQuantity = 0.0;
        totalFilledValue = 0.0;
        activeClientOrderId = null;
        activeConfirmedClientOrderId = null;
        executionStartTimestampMs = timestamp;

        isExecuting = true;
        isExecutingSince = getCurrentTime();

        double sentPrice = verb == Verb.Buy ? lastDepth.getBestAsk() : lastDepth.getBestBid();
        notifyExecutionStarted(verb, sentPrice);

        logger.info("{} {} VWAP increasePosition qty={} participationRate={} totalDurationMs={}",
                getCurrentTime(), instrument, quantity, participationRate, totalDurationMs);
        return true;
    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        if (!isExecuting) return false;
        if (!trade.getInstrument().equalsIgnoreCase(instrument.getPrimaryKey())) return false;

        double marketTradeQty = trade.getQuantity();
        double participationQty = marketTradeQty * participationRate;
        pendingChildQty += participationQty;

        logger.debug("{} {} VWAP observed trade qty={} pendingChildQty={}",
                getCurrentTime(), instrument, marketTradeQty, pendingChildQty);

        tryFlushPendingChildOrder(timeService.getCurrentTimestamp());
        return true;
    }

    /**
     * Sends a child order if there is enough pending quantity and no active order.
     */
    private void tryFlushPendingChildOrder(long timestamp) {
        if (activeClientOrderId != null) return; // wait for current child to complete

        double remaining = totalQuantity - totalFilledQuantity;
        if (remaining <= 0) return;

        if (pendingChildQty >= minChildOrderQty) {
            double orderQty = Math.min(pendingChildQty, remaining);
            orderQty = instrument.roundQty(orderQty);
            if (orderQty <= 0) return;

            pendingChildQty -= orderQty;
            sendChildOrder(timestamp, orderQty);
        }
    }

    private void sendChildOrder(long timestamp, double qty) {
        double price = currentVerb == Verb.Buy ? lastDepth.getBestAsk() : lastDepth.getBestBid();
        price = instrument.roundPrice(price);

        OrderRequest orderRequest = OrderRequest.createLimitOrderRequest(
                timestamp, algorithmInfo, instrument, currentVerb, qty, price);
        activeClientOrderId = orderRequest.getClientOrderId();

        logger.info("{} {} VWAP child order {}@{} verb={} filled={}/{}",
                getCurrentTime(), instrument, qty, price, currentVerb,
                totalFilledQuantity, totalQuantity);
        this.tradingEngineConnector.orderRequest(orderRequest);
    }

    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        if (!executionReport.getInstrument().equalsIgnoreCase(instrument.getPrimaryKey())) return false;

        String clOrId = executionReport.getClientOrderId();
        if (activeClientOrderId == null || !clOrId.equals(activeClientOrderId)) return false;

        ExecutionReportStatus status = executionReport.getExecutionReportStatus();

        boolean isConfirmed = status == ExecutionReportStatus.Active || status == ExecutionReportStatus.PartialFilled;
        if (isConfirmed) {
            activeConfirmedClientOrderId = clOrId;
        }

        if (status == ExecutionReportStatus.CompletelyFilled) {
            totalFilledQuantity += executionReport.getQuantityFill();
            totalFilledValue += executionReport.getPrice() * executionReport.getQuantityFill();
            activeClientOrderId = null;
            activeConfirmedClientOrderId = null;

            logger.info("{} {} VWAP child CF {}@{} totalFilled={}/{}",
                    getCurrentTime(), instrument,
                    executionReport.getQuantityFill(), executionReport.getPrice(),
                    totalFilledQuantity, totalQuantity);

            if (totalFilledQuantity >= totalQuantity - instrument.getQuantityTick()) {
                finishWithStatus(ExecutionReportStatus.CompletelyFilled);
            } else {
                tryFlushPendingChildOrder(timeService.getCurrentTimestamp());
            }
        }

        if (status == ExecutionReportStatus.Rejected) {
            logger.warn("{} {} VWAP child REJECTED: {}", getCurrentTime(), instrument, executionReport.getRejectReason());
            finishWithStatus(ExecutionReportStatus.Rejected);
        }

        if (status == ExecutionReportStatus.Cancelled) {
            activeClientOrderId = null;
            activeConfirmedClientOrderId = null;
        }

        return true;
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        boolean output = super.onDepthUpdate(depth);
        if (!isExecuting) return output;

        long now = timeService.getCurrentTimestamp();
        long elapsed = now - executionStartTimestampMs;

        // Time-based fallback: if we are behind our time-proportional target, send catch-up order
        if (activeClientOrderId == null) {
            double timeFraction = Math.min((double) elapsed / totalDurationMs, 1.0);
            double timeBasedTarget = totalQuantity * timeFraction;
            double deficit = timeBasedTarget - totalFilledQuantity - pendingChildQty;

            if (deficit >= minChildOrderQty) {
                pendingChildQty += deficit;
                logger.debug("{} {} VWAP time-based catch-up: timeFraction={} deficit={}",
                        getCurrentTime(), instrument, timeFraction, deficit);
                tryFlushPendingChildOrder(depth.getTimestamp());
            }
        }

        // Deadline: ensure everything is filled by end of window
        if (elapsed >= totalDurationMs && activeClientOrderId == null) {
            double remaining = totalQuantity - totalFilledQuantity;
            if (remaining > instrument.getQuantityTick()) {
                logger.info("{} {} VWAP deadline reached, sending remaining {}",
                        getCurrentTime(), instrument, remaining);
                sendChildOrder(depth.getTimestamp(), remaining);
            } else if (remaining <= instrument.getQuantityTick()) {
                finishWithStatus(ExecutionReportStatus.CompletelyFilled);
            }
        }

        return output;
    }

    private void finishWithStatus(ExecutionReportStatus status) {
        double avgFillPrice = (totalFilledQuantity > 0) ? totalFilledValue / totalFilledQuantity : 0.0;
        ExecutionReport syntheticEr = new ExecutionReport();
        syntheticEr.setInstrument(instrument.getPrimaryKey());
        syntheticEr.setVerb(currentVerb);
        syntheticEr.setPrice(avgFillPrice);
        syntheticEr.setQuantityFill(totalFilledQuantity);
        syntheticEr.setExecutionReportStatus(status);
        syntheticEr.setTimestampCreation(timeService.getCurrentTimestamp());

        notifyExecutionFinished(syntheticEr);
        finish();
    }

    @Override
    public boolean cancelAll() {
        long timestamp = timeService.getCurrentTimestamp();
        if (activeConfirmedClientOrderId != null) {
            logger.info("VWAP cancelling activeConfirmedClientOrderId {}", activeConfirmedClientOrderId);
            OrderRequest cancel = OrderRequest.createCancel(timestamp, algorithmInfo, instrument, activeConfirmedClientOrderId);
            this.tradingEngineConnector.orderRequest(cancel);
        } else if (activeClientOrderId != null) {
            logger.info("VWAP cancelling activeClientOrderId {}", activeClientOrderId);
            OrderRequest cancel = OrderRequest.createCancel(timestamp, algorithmInfo, instrument, activeClientOrderId);
            this.tradingEngineConnector.orderRequest(cancel);
        }
        finish();
        return true;
    }

    private void finish() {
        isExecuting = false;
        activeClientOrderId = null;
        activeConfirmedClientOrderId = null;
        pendingChildQty = 0.0;
    }

    public double getParticipationRate() {
        return participationRate;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }
}
