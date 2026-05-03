package com.lambda.investing.algorithmic_trading.tca;

import java.util.Optional;

/**
 * Represents a parent (algo) order submitted to an execution algorithm.
 *
 * <p>All monetary values are in the instrument's native price units. Timestamps are epoch
 * milliseconds (UTC).
 */
public class ParentOrder {

    private final String orderId;
    private final Side side;
    private final String symbol;
    private final long quantity;
    private final double arrivalPrice;
    private final double limitPrice;
    private final long decisionTimestampMs;
    private final long submissionTimestampMs;
    private final AlgoType algoType;
    private final double targetParticipationRate;

    /**
     * Creates a new {@code ParentOrder}.
     *
     * @param orderId               unique order identifier
     * @param side                  BUY or SELL
     * @param symbol                instrument ticker / primary key
     * @param quantity              total intended quantity
     * @param arrivalPrice          decision/arrival price used as IS benchmark
     * @param limitPrice            optional price limit (use {@link Double#NaN} when absent)
     * @param decisionTimestampMs   epoch millis at investment decision
     * @param submissionTimestampMs epoch millis at order submission
     * @param algoType              execution algorithm type
     * @param targetParticipationRate target participation rate [0.0–1.0]; relevant for POV only
     */
    public ParentOrder(
            String orderId,
            Side side,
            String symbol,
            long quantity,
            double arrivalPrice,
            double limitPrice,
            long decisionTimestampMs,
            long submissionTimestampMs,
            AlgoType algoType,
            double targetParticipationRate) {
        this.orderId = orderId;
        this.side = side;
        this.symbol = symbol;
        this.quantity = quantity;
        this.arrivalPrice = arrivalPrice;
        this.limitPrice = limitPrice;
        this.decisionTimestampMs = decisionTimestampMs;
        this.submissionTimestampMs = submissionTimestampMs;
        this.algoType = algoType;
        this.targetParticipationRate = targetParticipationRate;
    }

    /** Returns the unique order identifier. */
    public String getOrderId() {
        return orderId;
    }

    /** Returns the order side (BUY or SELL). */
    public Side getSide() {
        return side;
    }

    /** Returns the instrument symbol. */
    public String getSymbol() {
        return symbol;
    }

    /** Returns the total intended quantity for this parent order. */
    public long getQuantity() {
        return quantity;
    }

    /** Returns the arrival/decision price used as the IS benchmark. */
    public double getArrivalPrice() {
        return arrivalPrice;
    }

    /**
     * Returns the optional price limit.
     *
     * @return an {@link Optional} containing the limit price, or empty when not specified
     */
    public Optional<Double> getLimitPrice() {
        return Double.isNaN(limitPrice) ? Optional.empty() : Optional.of(limitPrice);
    }

    /** Returns the epoch millis at investment decision. */
    public long getDecisionTimestampMs() {
        return decisionTimestampMs;
    }

    /** Returns the epoch millis at order submission. */
    public long getSubmissionTimestampMs() {
        return submissionTimestampMs;
    }

    /** Returns the execution algorithm type. */
    public AlgoType getAlgoType() {
        return algoType;
    }

    /**
     * Returns the target participation rate for POV orders.
     *
     * @return target participation rate [0.0–1.0]
     */
    public double getTargetParticipationRate() {
        return targetParticipationRate;
    }
}
