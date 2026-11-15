package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.time_service.TimeServiceIfc;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;

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

    // Execution duration statistics (ms)
    private long completedExecutions;
    private long totalExecutionDurationMs;
    private int modifyCount;

    public static final int DEFAULT_MAX_REJECT_RETRIES = 3;
    private int maxRejectRetries = DEFAULT_MAX_REJECT_RETRIES;
    private int rejectRetryCount;
    // Order id the last request modified; null when the last request was a new order
    private String lastRequestModifiedId;
    private boolean marketForced;

    /**
     * Limit on how aggressive the order price may get when it is modified.
     * The cap is moved {@code ticksBeforeLimit} ticks back (less aggressive) from the reference.
     */
    public enum ModificationLimitMode {
        /**
         * Legacy behaviour: up to opposite best +/- 1 tick, market forced at deadline.
         */
        NONE,
        /**
         * Never beyond the limit price passed to increasePosition.
         */
        LIMIT_PRICE,
        /**
         * Never beyond this side's best (best bid for buy, best ask for sell).
         */
        BEST,
        /**
         * Never beyond the current midprice.
         */
        MID
    }

    @Getter
    private ModificationLimitMode modificationLimitMode = ModificationLimitMode.NONE;
    @Getter
    private int ticksBeforeLimit = 0;

    @Getter
    private double limitPrice = Double.NaN;

    public void setModificationLimit(ModificationLimitMode mode, int ticksBeforeLimit) {
        if (mode == null) throw new IllegalArgumentException("mode must not be null");
        if (ticksBeforeLimit < 0) throw new IllegalArgumentException("ticksBeforeLimit must be >= 0");
        this.modificationLimitMode = mode;
        this.ticksBeforeLimit = ticksBeforeLimit;
    }


    /**
     * Most aggressive price allowed: a maximum for buys, a minimum for sells.
     * NONE (or an invalid reference) falls back to opposite best +/- 1 tick.
     */
    private double getPriceBoundary() {
        boolean buy = currentVerb == Verb.Buy;
        double reference;
        switch (modificationLimitMode) {
            case LIMIT_PRICE:
                reference = limitPrice;
                break;
            case BEST:
                reference = buy ? lastDepth.getBestBid() : lastDepth.getBestAsk();
                break;
            case MID:
                reference = lastDepth.getMidPrice();
                break;
            default:
                reference = Double.NaN;
        }
        if (Double.isNaN(reference) || reference <= 0) {
            double best = buy ? lastDepth.getBestAsk() : lastDepth.getBestBid();
            return instrument.roundPrice(buy ? best + instrument.getPriceTick() : best - instrument.getPriceTick());
        }
        double offset = ticksBeforeLimit * instrument.getPriceTick();
        return instrument.roundPrice(buy ? reference - offset : reference + offset);
    }

    public void setMaxRejectRetries(int maxRejectRetries) {
        if (maxRejectRetries < 0) throw new IllegalArgumentException("maxRejectRetries must be >= 0");
        this.maxRejectRetries = maxRejectRetries;
    }

    public int getMaxRejectRetries() {
        return maxRejectRetries;
    }

    private String avgExecutionTimeDescription() {
        return completedExecutions == 0 ? "n/a"
                : String.format("%.0f ms (over %d executions)",
                (double) totalExecutionDurationMs / completedExecutions, completedExecutions);
    }

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
            logger.error("{} {} on {} can't increasePosition when isExecuting since {} [elapsed {} ms < timeout {} ms] timeoutExpectedIn={} ms, deadline(totalDuration)={} ms, avgExecutionTime={}, activeOrder={} confirmed={} activePrice={} remainingQty={} filledQty={} modifies={} requested {} qty={} price={}",
                    getCurrentTime(), this.instrument, this.algorithmInfo, isExecutingSince, elapsedMs, timeoutIsExecutingMs,
                    Math.max(timeoutIsExecutingMs - elapsedMs, 0), totalDurationMs, avgExecutionTimeDescription(),
                    activeClientOrderId, activeConfirmedClientOrderId, activeOrderPrice, currentQuantity,
                    totalFilledQuantity, modifyCount, verb, quantity, price);
            return false;
        }

        logger.info("{} {} on {} IS increasePosition requested {} qty={} price={} lastDepth(bid={} ask={} mid={}) totalDuration={} ms timeoutIsExecuting={} ms avgExecutionTime={}",
                getCurrentTime(), instrument, algorithmInfo, verb, quantity, price,
                lastDepth.getBestBid(), lastDepth.getBestAsk(), lastDepth.getMidPrice(),
                totalDurationMs, timeoutIsExecutingMs, avgExecutionTimeDescription());
        modifyCount = 0;
        rejectRetryCount = 0;
        lastRequestModifiedId = null;
        marketForced = false;
        currentQuantity = quantity;
        limitPrice = price;
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

        logger.info("{} {} IS increasePosition sent qty={} decisionMid={} urgency={} initialOrderPrice={} clientOrderId={} forcedMarketAt={} timeoutAt={}",
                getCurrentTime(), instrument, quantity, decisionMidPrice, baseUrgency, initialOrderPrice,
                activeClientOrderId,
                new java.util.Date(timestamp + totalDurationMs),
                new java.util.Date(isExecutingSince.getTime() + timeoutIsExecutingMs));
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
            // Clamp: do not pay more than the modification limit (default bestAsk + 1 tick)
            targetPrice = Math.min(targetPrice, getPriceBoundary());
        } else {
            targetPrice = decisionMidPrice - effectiveUrgency * halfSpread;
            // Clamp: do not sell below the modification limit (default bestBid - 1 tick)
            targetPrice = Math.max(targetPrice, getPriceBoundary());
        }

        return instrument.roundPrice(targetPrice);
    }

    private void sendOrder(long timestamp, double qty, double price) {
        OrderRequest orderRequest = OrderRequest.createLimitOrderRequest(
                timestamp, algorithmInfo, instrument, currentVerb, qty, price);
        activeClientOrderId = orderRequest.getClientOrderId();
        activeOrderPrice = price;
        lastRequestModifiedId = null;
        logger.info("{} {} IS new order {} {} qty={} price={}", getCurrentTime(), instrument,
                activeClientOrderId, currentVerb, qty, price);
        this.sendOrderRequest(orderRequest);
    }

    private void retryAfterReject(ExecutionReport executionReport) {
        long now = timeService.getCurrentTimestamp();
        if (marketForced) {
            rejectRetryCount++;
            logger.warn("{} {} IS market order REJECTED: {} -> retry {}/{}", getCurrentTime(), instrument,
                    executionReport.getRejectReason(), rejectRetryCount, maxRejectRetries);
            sendMarketOrder(now, null);
            return;
        }
        double price = computeOrderPrice(now);
        rejectRetryCount++;
        logger.warn("{} {} IS REJECTED: {} -> retry {}/{} qty={} price={}", getCurrentTime(), instrument,
                executionReport.getRejectReason(), rejectRetryCount, maxRejectRetries, currentQuantity, price);
        if (lastRequestModifiedId == null) {
            sendOrder(now, currentQuantity, price);
        } else {
            String idToModify = lastRequestModifiedId;
            OrderRequest modifyOrder = OrderRequest.modifyOrder(now, algorithmInfo, instrument,
                    currentVerb, currentQuantity, price, idToModify);
            logger.info("{} {} IS modifyOrder (retry) {} -> {} qty={} price={}", getCurrentTime(), instrument,
                    idToModify, modifyOrder.getClientOrderId(), currentQuantity, price);
            activeClientOrderId = modifyOrder.getClientOrderId();
            activeConfirmedClientOrderId = null;
            activeOrderPrice = price;
            this.sendOrderRequest(modifyOrder);
        }
    }

    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        if (!executionReport.getInstrument().equalsIgnoreCase(instrument.getPrimaryKey())) return false;
        logExecutionReport(executionReport);

        String clOrId = executionReport.getClientOrderId();
        if (activeClientOrderId == null || !clOrId.equals(activeClientOrderId)) {
            logger.debug("{} {} IS ignoring ER {} for {} (active={})", getCurrentTime(), instrument,
                    executionReport.getExecutionReportStatus(), clOrId, activeClientOrderId);
            return false;
        }

        ExecutionReportStatus status = executionReport.getExecutionReportStatus();
        logger.info("{} {} IS ER {} clOrdId={} price={} lastQty={} elapsed={} ms",
                getCurrentTime(), instrument, status, clOrId, executionReport.getPrice(),
                executionReport.getLastQuantity(), timeService.getCurrentTimestamp() - executionStartTimestampMs);

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
            if (rejectRetryCount < maxRejectRetries) {
                retryAfterReject(executionReport);
            } else {
                logger.warn("{} {} IS REJECTED: {} -> max retries {} reached, giving up", getCurrentTime(),
                        instrument, executionReport.getRejectReason(), maxRejectRetries);
                finishWithStatus(ExecutionReportStatus.Rejected);
            }
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
        if (!isExecuting || activeClientOrderId == null || marketForced) return output;

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

            logger.info("{} {} IS modifyOrder {} -> {} qty={} @{} (urgency driven)",
                    getCurrentTime(), instrument, idToModify,
                    modifyOrder.getClientOrderId(), currentQuantity, newPrice);

            modifyCount++;
            lastRequestModifiedId = idToModify;
            activeClientOrderId = modifyOrder.getClientOrderId();
            activeConfirmedClientOrderId = null;
            activeOrderPrice = newPrice;
            this.sendOrderRequest(modifyOrder);
        }

        // Deadline: cancel the passive order and send a real market order to guarantee execution
        long elapsed = now - executionStartTimestampMs;
        if (elapsed >= totalDurationMs && activeClientOrderId != null) {
            String idToCancel = (activeConfirmedClientOrderId != null)
                    ? activeConfirmedClientOrderId : activeClientOrderId;
            sendMarketOrder(depth.getTimestamp(), idToCancel);
        }

        return output;
    }

    private void sendMarketOrder(long timestamp, String idToCancel) {
        marketForced = true;
        if (idToCancel != null) {
            this.sendOrderRequest(OrderRequest.createCancel(timestamp, algorithmInfo, instrument, idToCancel));
        }
        OrderRequest marketOrder = OrderRequest.createMarketOrderRequest(
                timestamp, algorithmInfo, instrument, currentVerb, currentQuantity);
        logger.info("{} {} IS deadline – sending market order {} {} qty={} (cancelled {})",
                getCurrentTime(), instrument, marketOrder.getClientOrderId(), currentVerb, currentQuantity, idToCancel);
        // Cancel ERs carry another id and are ignored; fills match this market order id
        activeClientOrderId = marketOrder.getClientOrderId();
        activeConfirmedClientOrderId = null;
        lastRequestModifiedId = null;
        this.sendOrderRequest(marketOrder);
    }

    private void finishWithStatus(ExecutionReportStatus status) {
        long durationMs = timeService.getCurrentTimestamp() - executionStartTimestampMs;
        if (status == ExecutionReportStatus.CompletelyFilled) {
            completedExecutions++;
            totalExecutionDurationMs += durationMs;
        }
        logger.info("{} {} IS finished status={} duration={} ms (totalDuration={} ms, timeout={} ms) filled={} modifies={} avgExecutionTime={}",
                getCurrentTime(), instrument, status, durationMs, totalDurationMs, timeoutIsExecutingMs,
                totalFilledQuantity, modifyCount, avgExecutionTimeDescription());
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
            this.sendOrderRequest(cancel);
        } else if (activeClientOrderId != null) {
            logger.info("IS cancelling activeClientOrderId {}", activeClientOrderId);
            OrderRequest cancel = OrderRequest.createCancel(timestamp, algorithmInfo, instrument, activeClientOrderId);
            this.sendOrderRequest(cancel);
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
