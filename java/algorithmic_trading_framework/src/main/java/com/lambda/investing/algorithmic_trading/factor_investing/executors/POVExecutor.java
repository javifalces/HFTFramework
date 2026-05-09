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
 * Percentage of Volume (POV) Executor.
 * <p>
 * POV is a pure volume-driven participation algorithm.  Unlike VWAP, it has <em>no</em>
 * predetermined time window — it runs until the full target quantity is filled.  On every
 * observed market trade the executor accumulates a participation quota:
 * <pre>
 *   pendingChildQty += tradeQty * participationRate
 * </pre>
 * When the accumulated quota exceeds {@code minChildOrderQty} <em>and</em> no child order is
 * currently active, a limit order is sent at the current best price.
 *
 * <h3>Key differences from {@link VWAPExecutor}</h3>
 * <ul>
 *   <li>No {@code totalDurationMs} time constraint.</li>
 *   <li>No time-proportional catch-up fallback — execution is <em>only</em> driven by observed
 *       market volume (plus the inherited {@link AbstractExecutor#timeoutIsExecutingMs} safety
 *       timeout).</li>
 *   <li>Optional {@code maxParticipationRate} cap: clips each per-trade contribution so the
 *       executor never participates at more than a given fraction of any single trade.</li>
 * </ul>
 *
 * <h3>Completion</h3>
 * Execution ends when:
 * <ul>
 *   <li>The accumulated fill reaches {@code totalQuantity}.</li>
 *   <li>A child order is rejected (terminal failure).</li>
 *   <li>The inherited depth-based timeout fires and {@link #cancelAll()} is called.</li>
 * </ul>
 */
public class POVExecutor extends AbstractExecutor {

    // Configuration
    private final double participationRate;  // [0, 1] – fraction of each observed market trade to execute
    private final double minChildOrderQty;   // minimum size for a single child order
    private final double maxParticipationRate; // [participationRate, 1] – per-trade cap (0 = no cap)

    // State per execution
    private Verb currentVerb;
    private double totalQuantity;

    // Volume / fill tracking
    private double pendingChildQty;       // accumulated participation quota not yet sent
    private double totalFilledQuantity;
    private double totalFilledValue;      // price * qty for VWAP reporting in statistics

    // Active child-order tracking
    private String activeClientOrderId;
    private String activeConfirmedClientOrderId;

    /**
     * Constructs a POV executor without a per-trade rate cap.
     *
     * @param participationRate fraction [0, 1] of each observed market trade to participate in
     */
    public POVExecutor(TimeServiceIfc timeServiceIfc, String algorithmInfo, Instrument instrument,
                       AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
                       double participationRate) {
        this(timeServiceIfc, algorithmInfo, instrument, algorithmConnectorConfiguration,
                participationRate, 0.0, 0.0);
    }

    /**
     * Full constructor.
     *
     * @param participationRate    fraction [0, 1] of each observed market trade to participate in
     * @param minChildOrderQty     minimum size for a child order (0 → instrument quantity tick)
     * @param maxParticipationRate per-trade participation cap (0 → same as {@code participationRate})
     */
    public POVExecutor(TimeServiceIfc timeServiceIfc, String algorithmInfo, Instrument instrument,
                       AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
                       double participationRate, double minChildOrderQty, double maxParticipationRate) {
        super(timeServiceIfc, algorithmInfo, instrument, algorithmConnectorConfiguration);
        if (participationRate <= 0.0 || participationRate > 1.0) {
            throw new IllegalArgumentException("participationRate must be in (0, 1]");
        }
        if (maxParticipationRate != 0.0 && maxParticipationRate < participationRate) {
            throw new IllegalArgumentException("maxParticipationRate must be >= participationRate or 0");
        }
        this.participationRate = participationRate;
        this.minChildOrderQty = (minChildOrderQty > 0) ? minChildOrderQty : instrument.getQuantityTick();
        this.maxParticipationRate = (maxParticipationRate > 0) ? maxParticipationRate : participationRate;
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

        isExecuting = true;
        isExecutingSince = getCurrentTime();

        double sentPrice = verb == Verb.Buy ? lastDepth.getBestAsk() : lastDepth.getBestBid();
        notifyExecutionStarted(verb, sentPrice);

        logger.info("{} {} POV increasePosition qty={} participationRate={} maxParticipationRate={} minChildOrderQty={}",
                getCurrentTime(), instrument, quantity, participationRate, maxParticipationRate, minChildOrderQty);
        return true;
    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        if (!isExecuting) return false;
        if (!trade.getInstrument().equalsIgnoreCase(instrument.getPrimaryKey())) return false;

        double marketTradeQty = trade.getQuantity();

        // Cap the effective participation rate per trade to avoid excessive impact
        double effectiveRate = Math.min(participationRate, maxParticipationRate);
        double participationQty = marketTradeQty * effectiveRate;
        pendingChildQty += participationQty;

        logger.debug("{} {} POV observed trade qty={} effectiveRate={} pendingChildQty={}",
                getCurrentTime(), instrument, marketTradeQty, effectiveRate, pendingChildQty);

        tryFlushPendingChildOrder(timeService.getCurrentTimestamp());
        return true;
    }

    /**
     * Sends a child order if there is enough pending quantity and no active order is outstanding.
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

        logger.info("{} {} POV child order {}@{} verb={} filled={}/{}",
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

            logger.info("{} {} POV child CF {}@{} totalFilled={}/{}",
                    getCurrentTime(), instrument,
                    executionReport.getQuantityFill(), executionReport.getPrice(),
                    totalFilledQuantity, totalQuantity);

            if (totalFilledQuantity >= totalQuantity - instrument.getQuantityTick()) {
                finishWithStatus(ExecutionReportStatus.CompletelyFilled);
            } else {
                // Resume participation: flush any queued pending qty
                tryFlushPendingChildOrder(timeService.getCurrentTimestamp());
            }
        }

        if (status == ExecutionReportStatus.Rejected) {
            logger.warn("{} {} POV child REJECTED: {}", getCurrentTime(), instrument, executionReport.getRejectReason());
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
        // Delegates timeout logic to super; no time-based catch-up in POV
        return super.onDepthUpdate(depth);
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
            logger.info("POV cancelling activeConfirmedClientOrderId {}", activeConfirmedClientOrderId);
            OrderRequest cancel = OrderRequest.createCancel(timestamp, algorithmInfo, instrument, activeConfirmedClientOrderId);
            this.tradingEngineConnector.orderRequest(cancel);
        } else if (activeClientOrderId != null) {
            logger.info("POV cancelling activeClientOrderId {}", activeClientOrderId);
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

    public double getMaxParticipationRate() {
        return maxParticipationRate;
    }
}
