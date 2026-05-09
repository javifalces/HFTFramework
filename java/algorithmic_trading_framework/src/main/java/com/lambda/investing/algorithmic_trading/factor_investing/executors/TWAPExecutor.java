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
 * Time Weighted Average Price (TWAP) Executor.
 * <p>
 * Divides the total order into {@code numberOfSlices} equal parts and submits one
 * limit-order slice per scheduled time interval ({@code totalDurationMs / numberOfSlices}).
 * Each slice targets the best available price (best ask for buy, best bid for sell).
 * A slice that is not filled within its interval is cancelled and re-submitted at the
 * updated best price at the next interval boundary.
 * <p>
 * Execution statistics are reported for the overall execution (arrival price vs VWAP fill price).
 */
public class TWAPExecutor extends AbstractExecutor {

    // Configuration
    private final long totalDurationMs;
    private final int numberOfSlices;

    // State per execution
    private long executionStartTimestampMs;
    private Verb currentVerb;
    private double totalQuantity;
    private double sliceQuantity;
    private int slicesSent;        // how many slices have been scheduled so far
    private int slicesCompleted;   // how many slices have been completely filled
    private int slicesFailed;      // how many slices were cancelled due to interval expiry (unfilled)

    // Accumulated fill tracking (used to synthesize final ER)
    private double totalFilledQuantity;
    private double totalFilledValue; // price * qty for each fill

    // Active child-order tracking
    private String activeClientOrderId;
    private String activeConfirmedClientOrderId;

    public TWAPExecutor(TimeServiceIfc timeServiceIfc, String algorithmInfo, Instrument instrument,
                        AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
                        long totalDurationMs, int numberOfSlices) {
        super(timeServiceIfc, algorithmInfo, instrument, algorithmConnectorConfiguration);
        if (numberOfSlices <= 0) throw new IllegalArgumentException("numberOfSlices must be positive");
        if (totalDurationMs <= 0) throw new IllegalArgumentException("totalDurationMs must be positive");
        this.totalDurationMs = totalDurationMs;
        this.numberOfSlices = numberOfSlices;
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
        sliceQuantity = quantity / numberOfSlices;
        slicesSent = 0;
        slicesCompleted = 0;
        slicesFailed = 0;
        totalFilledQuantity = 0.0;
        totalFilledValue = 0.0;
        activeClientOrderId = null;
        activeConfirmedClientOrderId = null;
        executionStartTimestampMs = timestamp;

        isExecuting = true;
        isExecutingSince = getCurrentTime();

        double sentPrice = verb == Verb.Buy ? lastDepth.getBestAsk() : lastDepth.getBestBid();
        notifyExecutionStarted(verb, sentPrice);

        sendNextSlice(timestamp);
        return true;
    }

    /**
     * Returns the target time (ms) at which the Nth slice (0-indexed) should be sent.
     */
    private long sliceScheduledTime(int sliceIndex) {
        return executionStartTimestampMs + (long) sliceIndex * (totalDurationMs / numberOfSlices);
    }

    /**
     * Computes the quantity for a slice. The last slice picks up any rounding remainder.
     */
    private double sliceQty(int sliceIndex) {
        if (sliceIndex == numberOfSlices - 1) {
            return totalQuantity - sliceQuantity * (numberOfSlices - 1);
        }
        return sliceQuantity;
    }

    private void sendNextSlice(long timestamp) {
        if (slicesSent >= numberOfSlices) return;

        double qty = sliceQty(slicesSent);
        if (qty <= 0) {
            slicesSent++;
            checkAllSlicesDone();
            return;
        }

        double slicePrice = currentVerb == Verb.Buy ? lastDepth.getBestAsk() : lastDepth.getBestBid();
        slicePrice = instrument.roundPrice(slicePrice);

        OrderRequest orderRequest = OrderRequest.createLimitOrderRequest(
                timestamp, algorithmInfo, instrument, currentVerb, qty, slicePrice);
        activeClientOrderId = orderRequest.getClientOrderId();
        slicesSent++;

        logger.info("{} {} TWAP slice {}/{} {}@{} verb={}",
                getCurrentTime(), instrument, slicesSent, numberOfSlices, qty, slicePrice, currentVerb);
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
            slicesCompleted++;
            activeClientOrderId = null;
            activeConfirmedClientOrderId = null;

            logger.info("{} {} TWAP slice {}/{} CF {}@{}",
                    getCurrentTime(), instrument, slicesCompleted, numberOfSlices,
                    executionReport.getQuantityFill(), executionReport.getPrice());
            checkAllSlicesDone();
        }

        if (status == ExecutionReportStatus.Rejected) {
            logger.warn("{} {} TWAP slice {}/{} REJECTED: {}",
                    getCurrentTime(), instrument, slicesSent, numberOfSlices, executionReport.getRejectReason());
            finishWithStatus(ExecutionReportStatus.Rejected);
        }

        if (status == ExecutionReportStatus.Cancelled) {
            // Slice was cancelled (possibly by us during cancelAll or re-scheduling)
            activeClientOrderId = null;
            activeConfirmedClientOrderId = null;
        }

        return true;
    }

    private void checkAllSlicesDone() {
        if ((slicesCompleted + slicesFailed) >= numberOfSlices) {
            finishWithStatus(ExecutionReportStatus.CompletelyFilled);
        }
        // else next slice will be sent on the next scheduled interval in onDepthUpdate
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
            logger.info("TWAP cancelling activeConfirmedClientOrderId {}", activeConfirmedClientOrderId);
            OrderRequest cancel = OrderRequest.createCancel(timestamp, algorithmInfo, instrument, activeConfirmedClientOrderId);
            this.tradingEngineConnector.orderRequest(cancel);
        } else if (activeClientOrderId != null) {
            logger.info("TWAP cancelling activeClientOrderId {}", activeClientOrderId);
            OrderRequest cancel = OrderRequest.createCancel(timestamp, algorithmInfo, instrument, activeClientOrderId);
            this.tradingEngineConnector.orderRequest(cancel);
        }
        finish();
        return true;
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        boolean output = super.onDepthUpdate(depth);
        if (!isExecuting) return output;

        long now = timeService.getCurrentTimestamp();

        // Check if time to send the next slice (no active order + time interval passed)
        if (activeClientOrderId == null && slicesSent < numberOfSlices) {
            long nextSliceTime = sliceScheduledTime(slicesSent);
            if (now >= nextSliceTime) {
                sendNextSlice(depth.getTimestamp());
            }
        }

        // Cancel and re-submit current slice if its interval has expired
        if (activeClientOrderId != null) {
            long currentSliceStartTime = sliceScheduledTime(slicesSent - 1);
            long sliceInterval = totalDurationMs / numberOfSlices;
            boolean sliceIntervalExpired = (now - currentSliceStartTime) >= sliceInterval;
            boolean moreSlicesRemaining = slicesSent < numberOfSlices;

            if (sliceIntervalExpired && moreSlicesRemaining) {
                // Cancel the unfilled slice (increment slicesFailed, NOT slicesCompleted)
                String idToCancel = (activeConfirmedClientOrderId != null) ? activeConfirmedClientOrderId : activeClientOrderId;
                logger.info("{} {} TWAP slice interval expired, cancelling {} and re-submitting",
                        getCurrentTime(), instrument, idToCancel);
                OrderRequest cancel = OrderRequest.createCancel(depth.getTimestamp(), algorithmInfo, instrument, idToCancel);
                this.tradingEngineConnector.orderRequest(cancel);
                activeClientOrderId = null;
                activeConfirmedClientOrderId = null;
                slicesFailed++;
                sendNextSlice(depth.getTimestamp());
            }
        }

        return output;
    }

    private void finish() {
        isExecuting = false;
        activeClientOrderId = null;
        activeConfirmedClientOrderId = null;
    }

    public int getNumberOfSlices() {
        return numberOfSlices;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }
}
