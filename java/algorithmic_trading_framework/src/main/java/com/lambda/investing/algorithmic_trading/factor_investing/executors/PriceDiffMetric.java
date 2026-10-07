package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.model.trading.Verb;

/**
 * Value object capturing a directional price difference (e.g. slippage, implementation shortfall,
 * midprice movement) expressed in the three units commonly reported in post-trade TCA: price ticks,
 * basis points, and money cost.
 * <p>
 * All {@link ExecutorStatistics} price-comparison metrics share the same sign convention: a positive
 * value means the comparison was <b>adverse</b> (worse than the reference price) for the side traded,
 * e.g. a buy filled above its reference price, or a sell filled below it.
 */
public final class PriceDiffMetric {

    private final double ticks;
    private final double bps;
    private final double cost;

    private PriceDiffMetric(double ticks, double bps, double cost) {
        this.ticks = ticks;
        this.bps = bps;
        this.cost = cost;
    }

    /**
     * Computes the signed difference between {@code comparePrice} and {@code referencePrice}, oriented so a
     * positive result is always adverse for {@code verb}, then expresses it in ticks, bps (of
     * {@code referencePrice}) and money cost (of the filled quantity).
     *
     * @param verb               Buy or Sell of the execution
     * @param referencePrice     benchmark price (e.g. sent price, or arrival midprice)
     * @param comparePrice       price being compared against the benchmark (e.g. filled price, or midprice at fill)
     * @param quantityFill       quantity filled, used to turn the price difference into a money cost
     * @param quantityMultiplier contract/lot size multiplier of the instrument
     * @param priceTick          instrument price tick size, used to express the difference in ticks
     */
    public static PriceDiffMetric of(Verb verb, double referencePrice, double comparePrice,
                                     double quantityFill, double quantityMultiplier, double priceTick) {
        double diff = comparePrice - referencePrice;
        if (verb == Verb.Sell) {
            diff = -diff;
        }
        double ticks = priceTick != 0 ? diff / priceTick : 0.0;
        double bps = referencePrice != 0 ? (diff / referencePrice) * 10_000.0 : 0.0;
        double cost = diff * quantityFill * quantityMultiplier;
        return new PriceDiffMetric(ticks, bps, cost);
    }

    public double getTicks() {
        return ticks;
    }

    public double getBps() {
        return bps;
    }

    public double getCost() {
        return cost;
    }

    /**
     * Human-readable rendering used in per-execution log lines, e.g. {@code "2.00(ticks)/2.00(bps)/0.2000(cost)"}.
     */
    public String format() {
        return String.format("%.2f(ticks)/%.2f(bps)/%.4f(cost)", ticks, bps, cost);
    }

    @Override
    public String toString() {
        return format();
    }
}
