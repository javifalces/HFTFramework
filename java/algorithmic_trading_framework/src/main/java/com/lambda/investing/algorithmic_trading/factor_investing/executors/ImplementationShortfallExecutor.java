package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.time_service.TimeServiceIfc;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.Verb;

/**
 * Implementation Shortfall (IS) Executor.
 * <p>
 * Implementation Shortfall minimises the difference between the <em>decision price</em>
 * (the midprice at the moment {@link #increasePosition} is called) and the final
 * volume-weighted average fill price.  The algorithm balances two opposing costs:
 * <ul>
 *   <li><b>Market-impact cost</b> – trading too aggressively moves the market against you.</li>
 *   <li><b>Timing risk</b> – waiting too long exposes you to adverse price movement.</li>
 * </ul>
 *
 * <h3>Strategy</h3>
 * A single order is placed at an urgency-weighted price between the passive side (arrival
 * midprice) and the aggressive side (best ask/bid).  The effective urgency grows with:
 * <ol>
 *   <li>Base urgency set by the caller ({@code baseUrgency}).</li>
 *   <li>Time elapsed as a fraction of {@code totalDurationMs}.</li>
 *   <li>Adverse midprice movement (market moving against us accelerates urgency).</li>
 * </ol>
 * The order price is recalculated on every depth update and the active order is modified
 * when the effective urgency changes materially (by at least one price tick).
 *
 * <h3>Parameters</h3>
 * <ul>
 *   <li>{@code baseUrgency} [0, 1]: 0 = initially very passive (arrival price), 1 = immediately aggressive.</li>
 *   <li>{@code totalDurationMs}: the execution must complete within this window.</li>
 * </ul>
 */
public class ImplementationShortfallExecutor extends AbstractExecutor {

    // Configuration
    private final long totalDurationMs;
    private final double baseUrgency; // [0, 1]

    // Execution state
    private long executionStartTimestampMs;
    private Verb currentVerb;
    private double currentQuantity;
    private double decisionMidPrice; // midprice at time of increasePosition call

    // Fill tracking
    private double totalFilledQuantity;
    private double totalFilledValue;

    // Active order tracking
    private String activeClientOrderId;
    private String activeConfirmedClientOrderId;
    private double activeOrderPrice;

    public ImplementationShortfallExecutor(TimeServiceIfc timeServiceIfc, String algorithmInfo,
                                           Instrument instrument,
                                           AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
                                           long totalDurationMs, double baseUrgency) {
        super(timeServiceIfc, algorithmInfo, instrument, algorithmConnectorConfiguration);
        if (baseUrgency < 0.0 || baseUrgency > 1.0) {
            throw new IllegalArgumentException("baseUrgency must be in [0, 1]");
        }
        if (totalDurationMs <= 0) throw new IllegalArgumentException("totalDurationMs must be positive");
        this.totalDurationMs = totalDurationMs;
        this.baseUrgency = baseUrgency;
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
        currentQuantity = quantity;
        totalFilledQuantity = 0.0;
        totalFilledValue = 0.0;
        activeClientOrderId = null;
        activeConfirmedClientOrderId = null;
        executionStartTimestampMs = timestamp;

        decisionMidPrice = lastDepth.getMidPrice();

        isExecuting = true;
        isExecutingSince = getCurrentTime();

        notifyExecutionStarted(verb, decisionMidPrice);

        double initialOrderPrice = computeOrderPrice(timestamp);
        sendOrder(timestamp, quantity, initialOrderPrice);

        logger.info("{} {} IS increasePosition qty={} decisionMid={} urgency={} initialOrderPrice={}",
                getCurrentTime(), instrument, quantity, decisionMidPrice, baseUrgency, initialOrderPrice);
        return true;
    }

    /**
     * Computes the effective urgency at the given time and returns the target order price.
     * <p>
     * effectiveUrgency = clamp(baseUrgency + timeFraction * (1 - baseUrgency) + adverseMidMovement, 0, 1)
     * orderPrice = decisionMidPrice + effectiveUrgency * spread/2   (for buy)
     */
    private double computeOrderPrice(long nowMs) {
        long elapsed = nowMs - executionStartTimestampMs;
        double timeFraction = Math.min((double) elapsed / totalDurationMs, 1.0);

        // Base urgency rises linearly toward 1.0 over the total duration
        double effectiveUrgency = baseUrgency + timeFraction * (1.0 - baseUrgency);

        // Adverse midprice movement adds extra urgency (normalised to half-spread units)
        double currentMid = lastDepth.getMidPrice();
        double spread = Math.max(lastDepth.getSpread(), instrument.getPriceTick());
        double midMovement = currentMid - decisionMidPrice;
        if (currentVerb == Verb.Sell) {
            midMovement = -midMovement;
        }
        // Adverse movement (positive) maps to additional urgency
        double adverseUrgencyBoost = midMovement / spread;
        effectiveUrgency = Math.min(effectiveUrgency + Math.max(adverseUrgencyBoost, 0.0), 1.0);

        // Price = decision_mid + urgency * halfSpread (for buy, this moves toward bestAsk)
        double halfSpread = spread / 2.0;
        double targetPrice;
        if (currentVerb == Verb.Buy) {
            targetPrice = decisionMidPrice + effectiveUrgency * halfSpread;
            // Clamp: do not pay more than bestAsk + a few ticks at urgency=1
            double maxPrice = lastDepth.getBestAsk() + instrument.getPriceTick();
            targetPrice = Math.min(targetPrice, maxPrice);
        } else {
            targetPrice = decisionMidPrice - effectiveUrgency * halfSpread;
            // Clamp: do not sell below bestBid - a few ticks at urgency=1
            double minPrice = lastDepth.getBestBid() - instrument.getPriceTick();
            targetPrice = Math.max(targetPrice, minPrice);
        }

        return instrument.roundPrice(targetPrice);
    }

    private void sendOrder(long timestamp, double qty, double price) {
        OrderRequest orderRequest = OrderRequest.createLimitOrderRequest(
                timestamp, algorithmInfo, instrument, currentVerb, qty, price);
        activeClientOrderId = orderRequest.getClientOrderId();
        activeOrderPrice = price;
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
            if (status == ExecutionReportStatus.PartialFilled && executionReport.getLastQuantity() > 0) {
                totalFilledQuantity += executionReport.getLastQuantity();
                totalFilledValue += executionReport.getPrice() * executionReport.getLastQuantity();
                currentQuantity -= executionReport.getLastQuantity();
                logger.info("{} {} IS partial fill {}@{} totalFilled={}/{}",
                        getCurrentTime(), instrument, executionReport.getLastQuantity(),
                        executionReport.getPrice(), totalFilledQuantity,
                        totalFilledQuantity + currentQuantity);
            }
        }

        if (status == ExecutionReportStatus.CompletelyFilled) {
            // Accumulate the final-fill increment (lastQuantity) alongside any earlier partial fills
            double lastQty = executionReport.getLastQuantity() > 0
                    ? executionReport.getLastQuantity()
                    : executionReport.getQuantityFill() - totalFilledQuantity;
            totalFilledQuantity += lastQty;
            totalFilledValue += executionReport.getPrice() * lastQty;

            logger.info("{} {} IS CF {}@{} decisionMid={} IS={} ticks",
                    getCurrentTime(), instrument,
                    executionReport.getQuantityFill(), executionReport.getPrice(),
                    decisionMidPrice,
                    (executionReport.getPrice() - decisionMidPrice) / instrument.getPriceTick());
            finishWithStatus(ExecutionReportStatus.CompletelyFilled);
        }

        if (status == ExecutionReportStatus.Rejected) {
            logger.warn("{} {} IS REJECTED: {}", getCurrentTime(), instrument, executionReport.getRejectReason());
            finishWithStatus(ExecutionReportStatus.Rejected);
        }

        if (status == ExecutionReportStatus.Cancelled) {
            // Expected when we modify the order
            activeClientOrderId = null;
            activeConfirmedClientOrderId = null;
        }

        return true;
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        boolean output = super.onDepthUpdate(depth);
        if (!isExecuting || activeClientOrderId == null) return output;

        long now = timeService.getCurrentTimestamp();
        double newPrice = computeOrderPrice(now);

        // Modify order only if price has changed by at least one tick (use tick-integer comparison to avoid float errors)
        long newPriceTicks = Math.round(newPrice / instrument.getPriceTick());
        long activePriceTicks = Math.round(activeOrderPrice / instrument.getPriceTick());
        if (Math.abs(newPriceTicks - activePriceTicks) >= 1) {
            String idToModify = (activeConfirmedClientOrderId != null)
                    ? activeConfirmedClientOrderId : activeClientOrderId;

            OrderRequest modifyOrder = OrderRequest.modifyOrder(
                    depth.getTimestamp(), algorithmInfo, instrument,
                    currentVerb, currentQuantity, newPrice, idToModify);

            logger.info("{} {} IS modifyOrder {} -> {} @{} (urgency driven)",
                    getCurrentTime(), instrument, idToModify,
                    modifyOrder.getClientOrderId(), newPrice);

            activeClientOrderId = modifyOrder.getClientOrderId();
            activeConfirmedClientOrderId = null;
            activeOrderPrice = newPrice;
            this.tradingEngineConnector.orderRequest(modifyOrder);
        }

        // Force market execution at end of window
        long elapsed = now - executionStartTimestampMs;
        if (elapsed >= totalDurationMs && activeClientOrderId != null) {
            double marketPrice = currentVerb == Verb.Buy
                    ? depth.getBestAsk() + instrument.getPriceTick()
                    : depth.getBestBid() - instrument.getPriceTick();
            marketPrice = instrument.roundPrice(marketPrice);

            String idToModify = (activeConfirmedClientOrderId != null)
                    ? activeConfirmedClientOrderId : activeClientOrderId;

            OrderRequest finalOrder = OrderRequest.modifyOrder(
                    depth.getTimestamp(), algorithmInfo, instrument,
                    currentVerb, currentQuantity, marketPrice, idToModify);

            logger.info("{} {} IS deadline – forcing market execution {} @{}",
                    getCurrentTime(), instrument, finalOrder.getClientOrderId(), marketPrice);

            activeClientOrderId = finalOrder.getClientOrderId();
            activeConfirmedClientOrderId = null;
            activeOrderPrice = marketPrice;
            this.tradingEngineConnector.orderRequest(finalOrder);
        }

        return output;
    }

    private void finishWithStatus(ExecutionReportStatus status) {
        double avgFillPrice = (totalFilledQuantity > 0) ? totalFilledValue / totalFilledQuantity : decisionMidPrice;
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
            logger.info("IS cancelling activeConfirmedClientOrderId {}", activeConfirmedClientOrderId);
            OrderRequest cancel = OrderRequest.createCancel(timestamp, algorithmInfo, instrument, activeConfirmedClientOrderId);
            this.tradingEngineConnector.orderRequest(cancel);
        } else if (activeClientOrderId != null) {
            logger.info("IS cancelling activeClientOrderId {}", activeClientOrderId);
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
    }

    public double getBaseUrgency() {
        return baseUrgency;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public double getDecisionMidPrice() {
        return decisionMidPrice;
    }
}
