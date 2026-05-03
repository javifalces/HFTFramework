package com.lambda.investing.algorithmic_trading.tca;

/**
 * Represents a single child-order fill (execution) associated with a parent order.
 *
 * <p>All monetary values are in the instrument's native price units. Timestamps are epoch
 * milliseconds (UTC).
 */
public class ChildFill {

    private final String parentOrderId;
    private final long timestampMs;
    private final double fillPrice;
    private final long fillQuantity;
    private final double spreadAtFill;
    private final double midPriceAtFill;
    private final double midPrice1sPostFill;
    private final double bidVolume;
    private final double askVolume;
    private final String venue;

    /**
     * Creates a new {@code ChildFill}.
     *
     * @param parentOrderId     identifier of the parent order this fill belongs to
     * @param timestampMs       epoch millis when the fill occurred
     * @param fillPrice         execution price of this fill
     * @param fillQuantity      number of units filled
     * @param spreadAtFill      bid-ask spread at the time of the fill
     * @param midPriceAtFill    mid-price at the time of the fill
     * @param midPrice1sPostFill mid-price one second after the fill (adverse selection proxy)
     * @param bidVolume         best-bid volume at the time of the fill
     * @param askVolume         best-ask volume at the time of the fill
     * @param venue             execution venue identifier
     */
    public ChildFill(
            String parentOrderId,
            long timestampMs,
            double fillPrice,
            long fillQuantity,
            double spreadAtFill,
            double midPriceAtFill,
            double midPrice1sPostFill,
            double bidVolume,
            double askVolume,
            String venue) {
        this.parentOrderId = parentOrderId;
        this.timestampMs = timestampMs;
        this.fillPrice = fillPrice;
        this.fillQuantity = fillQuantity;
        this.spreadAtFill = spreadAtFill;
        this.midPriceAtFill = midPriceAtFill;
        this.midPrice1sPostFill = midPrice1sPostFill;
        this.bidVolume = bidVolume;
        this.askVolume = askVolume;
        this.venue = venue;
    }

    /** Returns the parent order identifier for this fill. */
    public String getParentOrderId() {
        return parentOrderId;
    }

    /** Returns the epoch millis when the fill occurred. */
    public long getTimestampMs() {
        return timestampMs;
    }

    /** Returns the execution price of this fill. */
    public double getFillPrice() {
        return fillPrice;
    }

    /** Returns the number of units filled. */
    public long getFillQuantity() {
        return fillQuantity;
    }

    /** Returns the bid-ask spread at the time of the fill. */
    public double getSpreadAtFill() {
        return spreadAtFill;
    }

    /** Returns the mid-price at the time of the fill. */
    public double getMidPriceAtFill() {
        return midPriceAtFill;
    }

    /** Returns the mid-price one second after the fill. */
    public double getMidPrice1sPostFill() {
        return midPrice1sPostFill;
    }

    /** Returns the best-bid volume at the time of the fill. */
    public double getBidVolume() {
        return bidVolume;
    }

    /** Returns the best-ask volume at the time of the fill. */
    public double getAskVolume() {
        return askVolume;
    }

    /** Returns the execution venue identifier. */
    public String getVenue() {
        return venue;
    }
}
