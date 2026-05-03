package com.lambda.investing.algorithmic_trading.tca;

import com.lambda.investing.algorithmic_trading.utils.TimeseriesUtils;

import java.util.List;
import java.util.Optional;

/**
 * Immutable result object that holds all TCA metrics for a single parent order.
 *
 * <p>Create instances via the static factory method
 * {@link #fromFills(ParentOrder, List, MarketDataSnapshot)}.
 *
 * <p>All slippage / cost metrics are expressed in <em>basis points</em> (bps) with the sign
 * convention: <strong>positive = cost / unfavourable</strong> for the aggressor.
 */
public class TcaResult {

    // -----------------------------------------------------------------------
    // Named constants – no magic numbers
    // -----------------------------------------------------------------------
    private static final double BASIS_POINTS_FACTOR = 10_000.0;
    private static final double HALF_SPREAD_DIVISOR = 0.5;

    // -----------------------------------------------------------------------
    // Identity fields
    // -----------------------------------------------------------------------
    private final String orderId;
    private final String symbol;
    private final Side side;
    private final AlgoType algoType;

    // -----------------------------------------------------------------------
    // Price benchmarks (bps)
    // -----------------------------------------------------------------------
    private final double implementationShortfall;
    private final double vwapSlippage;
    private final double twapSlippage;

    // -----------------------------------------------------------------------
    // Cost decomposition (bps)
    // -----------------------------------------------------------------------
    private final double delayCostBps;
    private final double marketImpactBps;
    private final double opportunityCostBps;

    // -----------------------------------------------------------------------
    // Fill quality
    // -----------------------------------------------------------------------
    private final double fillRate;
    private final double avgFillPrice;
    private final long totalFilledQuantity;
    private final long totalExecutionTimeMs;

    // -----------------------------------------------------------------------
    // Spread & adverse selection
    // -----------------------------------------------------------------------
    private final double avgSpreadCapture;
    private final double avgAdverseSelectionBps;

    // -----------------------------------------------------------------------
    // Market microstructure
    // -----------------------------------------------------------------------
    private final double avgOBI;
    /** Present only when {@link AlgoType#POV} is used; otherwise empty. */
    private final Optional<Double> povDeviation;

    private final long computedAtMs;

    // -----------------------------------------------------------------------
    // Private constructor – use fromFills()
    // -----------------------------------------------------------------------
    @SuppressWarnings("java:S107") // builder-like, many params intentional
    private TcaResult(
            String orderId,
            String symbol,
            Side side,
            AlgoType algoType,
            double implementationShortfall,
            double vwapSlippage,
            double twapSlippage,
            double delayCostBps,
            double marketImpactBps,
            double opportunityCostBps,
            double fillRate,
            double avgFillPrice,
            long totalFilledQuantity,
            long totalExecutionTimeMs,
            double avgSpreadCapture,
            double avgAdverseSelectionBps,
            double avgOBI,
            Optional<Double> povDeviation,
            long computedAtMs) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.algoType = algoType;
        this.implementationShortfall = implementationShortfall;
        this.vwapSlippage = vwapSlippage;
        this.twapSlippage = twapSlippage;
        this.delayCostBps = delayCostBps;
        this.marketImpactBps = marketImpactBps;
        this.opportunityCostBps = opportunityCostBps;
        this.fillRate = fillRate;
        this.avgFillPrice = avgFillPrice;
        this.totalFilledQuantity = totalFilledQuantity;
        this.totalExecutionTimeMs = totalExecutionTimeMs;
        this.avgSpreadCapture = avgSpreadCapture;
        this.avgAdverseSelectionBps = avgAdverseSelectionBps;
        this.avgOBI = avgOBI;
        this.povDeviation = povDeviation;
        this.computedAtMs = computedAtMs;
    }

    // -----------------------------------------------------------------------
    // Static factory
    // -----------------------------------------------------------------------

    /**
     * Runs the full TCA computation pipeline and returns a {@link TcaResult}.
     *
     * <p>Edge-case handling:
     * <ul>
     *   <li>A {@code null} or empty {@code fills} list returns a zero-value result.</li>
     *   <li>A {@code null} {@code snapshot} causes VWAP / TWAP slippage to be {@code 0.0}.</li>
     *   <li>Zero total quantity in {@code order} returns a zero-value result.</li>
     * </ul>
     *
     * @param order    the parent order metadata (must not be {@code null})
     * @param fills    child-order fills; may be {@code null} or empty
     * @param snapshot market-data snapshot for VWAP/TWAP benchmark; may be {@code null}
     * @return a fully populated {@link TcaResult}
     */
    public static TcaResult fromFills(
            ParentOrder order, List<ChildFill> fills, MarketDataSnapshot snapshot) {

        long computedAtMs = System.currentTimeMillis();

        if (order.getQuantity() == 0 || fills == null || fills.isEmpty()) {
            return zeroResult(order, computedAtMs);
        }

        int sideSign = order.getSide().sign();
        long totalQty = order.getQuantity();

        // --- fill quality ---------------------------------------------------
        long filledQty = computeTotalFilledQuantity(fills);
        double fillRate = (double) filledQty / totalQty;
        double avgFillPrice = computeAvgFillPrice(fills);

        long firstFillTs = fills.get(0).getTimestampMs();
        long lastFillTs = fills.get(fills.size() - 1).getTimestampMs();
        long totalExecutionTimeMs = lastFillTs - firstFillTs;

        // --- benchmarks (bps) -----------------------------------------------
        double arrivalPrice = order.getArrivalPrice();
        double isBps = sideSign
                * ((avgFillPrice - arrivalPrice) / arrivalPrice)
                * BASIS_POINTS_FACTOR;

        double intervalVwap = (snapshot != null)
                ? TimeseriesUtils.computeVwap(snapshot.getPrices(), snapshot.getVolumes())
                : avgFillPrice;
        double vwapSlippage = sideSign
                * ((avgFillPrice - intervalVwap) / intervalVwap)
                * BASIS_POINTS_FACTOR;

        double intervalTwap = (snapshot != null)
                ? TimeseriesUtils.computeTwap(snapshot.getPrices())
                : avgFillPrice;
        double twapSlippage = sideSign
                * ((avgFillPrice - intervalTwap) / intervalTwap)
                * BASIS_POINTS_FACTOR;

        // --- cost decomposition (bps) ----------------------------------------
        // Delay cost: price drift from decision to submission
        double submissionPrice = arrivalPrice; // approximation; no mid at submission in model
        double delayCostBps = (submissionPrice - arrivalPrice) / arrivalPrice
                * sideSign * BASIS_POINTS_FACTOR;

        // Market impact: avg fill vs. pre-trade mid (first fill's mid)
        double preMidPrice = fills.get(0).getMidPriceAtFill();
        double marketImpactBps = sideSign
                * ((avgFillPrice - preMidPrice) / preMidPrice)
                * BASIS_POINTS_FACTOR;

        // Opportunity cost: cost of leaving quantity unfilled
        long unfilledQty = totalQty - filledQty;
        double opportunityCostBps = 0.0;
        if (unfilledQty > 0) {
            // Use last fill mid as the cancellation price proxy
            double cancellationPrice = fills.get(fills.size() - 1).getMidPriceAtFill();
            opportunityCostBps = sideSign
                    * ((cancellationPrice - arrivalPrice) / arrivalPrice)
                    * BASIS_POINTS_FACTOR
                    * ((double) unfilledQty / totalQty);
        }

        // --- spread & adverse selection (bps) --------------------------------
        double avgSpreadCapture = computeAvgSpreadCapture(fills, sideSign);
        double avgAdverseSelectionBps = computeAvgAdverseSelectionBps(fills, sideSign);

        // --- market microstructure -------------------------------------------
        double avgOBI = computeAvgOBI(fills);

        Optional<Double> povDeviation = Optional.empty();
        if (order.getAlgoType() == AlgoType.POV && snapshot != null) {
            long totalMarketVolume = snapshot.getVolumes().stream().mapToLong(Long::longValue).sum();
            if (totalMarketVolume > 0) {
                double actualParticipationRate = (double) filledQty / totalMarketVolume;
                povDeviation = Optional.of(
                        Math.abs(order.getTargetParticipationRate() - actualParticipationRate));
            }
        }

        return new TcaResult(
                order.getOrderId(),
                order.getSymbol(),
                order.getSide(),
                order.getAlgoType(),
                isBps,
                vwapSlippage,
                twapSlippage,
                delayCostBps,
                marketImpactBps,
                opportunityCostBps,
                fillRate,
                avgFillPrice,
                filledQty,
                totalExecutionTimeMs,
                avgSpreadCapture,
                avgAdverseSelectionBps,
                avgOBI,
                povDeviation,
                computedAtMs);
    }

    // -----------------------------------------------------------------------
    // Private computation helpers
    // -----------------------------------------------------------------------

    /**
     * Computes the volume-weighted average fill price across all fills.
     *
     * @param fills non-empty list of child fills
     * @return VWAP of fills
     */
    private static double computeAvgFillPrice(List<ChildFill> fills) {
        double notional = 0.0;
        long totalQty = 0;
        for (ChildFill fill : fills) {
            notional += fill.getFillPrice() * fill.getFillQuantity();
            totalQty += fill.getFillQuantity();
        }
        return totalQty == 0 ? 0.0 : notional / totalQty;
    }

    /**
     * Sums the filled quantities across all child fills.
     *
     * @param fills non-empty list of child fills
     * @return total filled quantity
     */
    private static long computeTotalFilledQuantity(List<ChildFill> fills) {
        long total = 0;
        for (ChildFill fill : fills) {
            total += fill.getFillQuantity();
        }
        return total;
    }

    /**
     * Computes the average spread-capture score across all fills.
     *
     * <p>Formula per fill:
     * {@code (midPriceAtFill - fillPrice) * sideSign / (HALF_SPREAD_DIVISOR * spreadAtFill)}
     *
     * <p>Positive = spread earned (passive fill); negative = spread paid (aggressive fill).
     *
     * @param fills    non-empty list of child fills
     * @param sideSign {@code +1} for BUY, {@code -1} for SELL
     * @return mean spread-capture score
     */
    private static double computeAvgSpreadCapture(List<ChildFill> fills, int sideSign) {
        double sum = 0.0;
        int count = 0;
        for (ChildFill fill : fills) {
            double spread = fill.getSpreadAtFill();
            if (spread > 0) {
                sum += (fill.getMidPriceAtFill() - fill.getFillPrice())
                        * sideSign
                        / (HALF_SPREAD_DIVISOR * spread);
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    /**
     * Computes the average adverse-selection in basis points.
     *
     * <p>Formula per fill:
     * {@code sideSign * (midPrice1sPostFill - midPriceAtFill) / midPriceAtFill * BASIS_POINTS_FACTOR}
     *
     * <p>Positive = mid moved against the trader after the fill (adverse).
     *
     * @param fills    non-empty list of child fills
     * @param sideSign {@code +1} for BUY, {@code -1} for SELL
     * @return mean adverse-selection in bps
     */
    private static double computeAvgAdverseSelectionBps(List<ChildFill> fills, int sideSign) {
        double sum = 0.0;
        for (ChildFill fill : fills) {
            sum += (fill.getMidPrice1sPostFill() - fill.getMidPriceAtFill())
                    / fill.getMidPriceAtFill();
        }
        return sideSign * (sum / fills.size()) * BASIS_POINTS_FACTOR;
    }

    /**
     * Computes the average Order Book Imbalance (OBI) across all fills.
     *
     * <p>Formula per fill: {@code (bidVolume - askVolume) / (bidVolume + askVolume)}
     *
     * @param fills non-empty list of child fills
     * @return mean OBI in {@code [-1, +1]}
     */
    private static double computeAvgOBI(List<ChildFill> fills) {
        double sum = 0.0;
        int count = 0;
        for (ChildFill fill : fills) {
            double total = fill.getBidVolume() + fill.getAskVolume();
            if (total > 0) {
                sum += (fill.getBidVolume() - fill.getAskVolume()) / total;
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    /**
     * Returns a zero-value {@link TcaResult} for orders that cannot be analysed (e.g. zero
     * quantity, null or empty fills).
     *
     * @param order        parent order for identity fields
     * @param computedAtMs epoch millis at computation time
     * @return zero-value result
     */
    private static TcaResult zeroResult(ParentOrder order, long computedAtMs) {
        return new TcaResult(
                order.getOrderId(),
                order.getSymbol(),
                order.getSide(),
                order.getAlgoType(),
                0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                0.0, 0.0, 0L, 0L,
                0.0, 0.0,
                0.0, Optional.empty(),
                computedAtMs);
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /** Returns the parent order identifier. */
    public String getOrderId() {
        return orderId;
    }

    /** Returns the instrument symbol. */
    public String getSymbol() {
        return symbol;
    }

    /** Returns the order side. */
    public Side getSide() {
        return side;
    }

    /** Returns the execution algorithm type. */
    public AlgoType getAlgoType() {
        return algoType;
    }

    /** Returns the Implementation Shortfall in basis points. */
    public double getImplementationShortfall() {
        return implementationShortfall;
    }

    /** Returns the VWAP slippage in basis points. */
    public double getVwapSlippage() {
        return vwapSlippage;
    }

    /** Returns the TWAP slippage in basis points. */
    public double getTwapSlippage() {
        return twapSlippage;
    }

    /** Returns the delay cost in basis points. */
    public double getDelayCostBps() {
        return delayCostBps;
    }

    /** Returns the market-impact cost in basis points. */
    public double getMarketImpactBps() {
        return marketImpactBps;
    }

    /** Returns the opportunity cost in basis points. */
    public double getOpportunityCostBps() {
        return opportunityCostBps;
    }

    /** Returns the fill rate in {@code [0.0, 1.0]}. */
    public double getFillRate() {
        return fillRate;
    }

    /** Returns the volume-weighted average fill price. */
    public double getAvgFillPrice() {
        return avgFillPrice;
    }

    /** Returns the total filled quantity. */
    public long getTotalFilledQuantity() {
        return totalFilledQuantity;
    }

    /** Returns the total execution duration in milliseconds. */
    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }

    /** Returns the average spread-capture score. */
    public double getAvgSpreadCapture() {
        return avgSpreadCapture;
    }

    /** Returns the average adverse-selection in basis points. */
    public double getAvgAdverseSelectionBps() {
        return avgAdverseSelectionBps;
    }

    /** Returns the average Order Book Imbalance. */
    public double getAvgOBI() {
        return avgOBI;
    }

    /**
     * Returns the absolute deviation between target and actual participation rate for POV orders.
     *
     * @return an {@link Optional} containing the deviation, or empty for non-POV orders
     */
    public Optional<Double> getPovDeviation() {
        return povDeviation;
    }

    /** Returns the epoch millis when this result was computed. */
    public long getComputedAtMs() {
        return computedAtMs;
    }

    @Override
    public String toString() {
        return "TcaResult{"
                + "orderId='" + orderId + '\''
                + ", symbol='" + symbol + '\''
                + ", side=" + side
                + ", algoType=" + algoType
                + ", IS=" + implementationShortfall + "bps"
                + ", vwapSlippage=" + vwapSlippage + "bps"
                + ", twapSlippage=" + twapSlippage + "bps"
                + ", delayCost=" + delayCostBps + "bps"
                + ", marketImpact=" + marketImpactBps + "bps"
                + ", opportunityCost=" + opportunityCostBps + "bps"
                + ", fillRate=" + fillRate
                + ", avgFillPrice=" + avgFillPrice
                + ", totalFilledQty=" + totalFilledQuantity
                + ", execTimeMs=" + totalExecutionTimeMs
                + ", spreadCapture=" + avgSpreadCapture
                + ", adverseSelection=" + avgAdverseSelectionBps + "bps"
                + ", OBI=" + avgOBI
                + ", povDeviation=" + povDeviation
                + ", computedAtMs=" + computedAtMs
                + '}';
    }
}
