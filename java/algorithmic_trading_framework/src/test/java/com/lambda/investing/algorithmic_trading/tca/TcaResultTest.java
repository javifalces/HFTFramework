package com.lambda.investing.algorithmic_trading.tca;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TcaResult} and the TCA computation pipeline.
 */
class TcaResultTest {

    // -----------------------------------------------------------------------
    // Helper factories
    // -----------------------------------------------------------------------

    private static ParentOrder buyOrder(long qty, double arrivalPrice) {
        return new ParentOrder(
                "ORDER-BUY-1", Side.BUY, "BTCUSD",
                qty, arrivalPrice, Double.NaN,
                1_000_000L, 1_001_000L,
                AlgoType.IS, 0.0);
    }

    private static ParentOrder sellOrder(long qty, double arrivalPrice) {
        return new ParentOrder(
                "ORDER-SELL-1", Side.SELL, "BTCUSD",
                qty, arrivalPrice, Double.NaN,
                1_000_000L, 1_001_000L,
                AlgoType.IS, 0.0);
    }

    private static ChildFill fill(long ts, double price, long qty, double spread,
            double mid, double mid1s, double bidVol, double askVol) {
        return new ChildFill("ORDER-BUY-1", ts, price, qty, spread, mid, mid1s, bidVol, askVol, "VENUE_A");
    }

    // -----------------------------------------------------------------------
    // Test 1: IS calculation for a fully filled BUY order
    // -----------------------------------------------------------------------

    /**
     * A BUY order where the average fill is above the arrival price incurs a positive IS cost.
     *
     * <p>avgFillPrice = 100.5 (50 @ 100.0 + 50 @ 101.0 = 10050 / 100)
     * IS_bps = +1 * (100.5 - 100.0) / 100.0 * 10000 = +50 bps
     */
    @Test
    void isCalculation_fullyFilledBuyOrder() {
        ParentOrder order = buyOrder(100, 100.0);

        List<ChildFill> fills = Arrays.asList(
                fill(1_000L, 100.0, 50, 0.1, 100.0, 100.05, 100, 80),
                fill(2_000L, 101.0, 50, 0.1, 101.0, 101.05, 90, 70));

        TcaResult result = TcaResult.fromFills(order, fills, null);

        assertEquals(1.0, result.getFillRate(), 1e-9, "fill rate should be 1.0");
        assertEquals(100.5, result.getAvgFillPrice(), 1e-9, "avg fill price");
        assertEquals(50.0, result.getImplementationShortfall(), 1e-6,
                "IS should be +50 bps for BUY above arrival");
    }

    // -----------------------------------------------------------------------
    // Test 2: IS opportunity cost for a partially filled SELL order
    // -----------------------------------------------------------------------

    /**
     * A SELL order that is only half-filled incurs opportunity cost if the mid moves above the
     * arrival price (unfavourable for a seller who missed part of the order).
     *
     * <p>arrivalPrice = 200.0; cancellationPrice (last fill mid) = 202.0; unfilledQty = 50
     * opportunityCostBps = -1 * (202 - 200) / 200 * 10000 * (50/100) = -50 bps
     */
    @Test
    void opportunityCost_partiallyFilledSellOrder() {
        ParentOrder order = sellOrder(100, 200.0);

        // Only 50 units filled; mid at last fill = 202.0
        List<ChildFill> fills = Collections.singletonList(
                fill(1_000L, 199.5, 50, 0.2, 202.0, 202.1, 80, 90));

        TcaResult result = TcaResult.fromFills(order, fills, null);

        assertEquals(0.5, result.getFillRate(), 1e-9, "fill rate should be 0.5");
        // opportunity cost = -1 * (202 - 200)/200 * 10000 * 0.5 = -50 bps
        assertEquals(-50.0, result.getOpportunityCostBps(), 1e-6,
                "opportunity cost for partial SELL when mid rose");
    }

    // -----------------------------------------------------------------------
    // Test 3: VWAP benchmark calculation over 5 fills with different volumes
    // -----------------------------------------------------------------------

    /**
     * Verifies that VWAP slippage is correctly computed from a market-data snapshot.
     *
     * <p>Snapshot: prices = [99, 100, 101, 102, 103], volumes = [10, 20, 30, 20, 10]
     * VWAP = (99*10 + 100*20 + 101*30 + 102*20 + 103*10) / 90 = 9090 / 90 = 101.0
     *
     * <p>avgFillPrice = 101.5 (above VWAP for a BUY → positive slippage)
     * vwapSlippage = +1 * (101.5 - 101.0) / 101.0 * 10000 ≈ 49.50 bps
     */
    @Test
    void vwapSlippage_fiveFillsWithDifferentVolumes() {
        ParentOrder order = new ParentOrder(
                "ORDER-VWAP-1", Side.BUY, "ETHUSD",
                90, 101.0, Double.NaN,
                0L, 0L, AlgoType.VWAP, 0.0);

        List<ChildFill> fills = Arrays.asList(
                fill(1_000L, 101.5, 18, 0.05, 101.0, 101.0, 100, 100),
                fill(2_000L, 101.5, 18, 0.05, 101.0, 101.0, 100, 100),
                fill(3_000L, 101.5, 18, 0.05, 101.0, 101.0, 100, 100),
                fill(4_000L, 101.5, 18, 0.05, 101.0, 101.0, 100, 100),
                fill(5_000L, 101.5, 18, 0.05, 101.0, 101.0, 100, 100));

        MarketDataSnapshot snapshot = new MarketDataSnapshot(
                Arrays.asList(1_000L, 2_000L, 3_000L, 4_000L, 5_000L),
                Arrays.asList(99.0, 100.0, 101.0, 102.0, 103.0),
                Arrays.asList(10L, 20L, 30L, 20L, 10L));

        TcaResult result = TcaResult.fromFills(order, fills, snapshot);

        assertEquals(101.5, result.getAvgFillPrice(), 1e-9, "avg fill price");
        double expectedVwap = (99.0 * 10 + 100.0 * 20 + 101.0 * 30 + 102.0 * 20 + 103.0 * 10)
                / 90.0;
        double expectedVwapSlippage = (101.5 - expectedVwap) / expectedVwap * 10_000.0;
        assertEquals(expectedVwapSlippage, result.getVwapSlippage(), 1e-4, "VWAP slippage bps");
    }

    // -----------------------------------------------------------------------
    // Test 4: Spread capture – passive (positive) vs aggressive (negative)
    // -----------------------------------------------------------------------

    /**
     * A passive BUY fill at the bid earns half the spread → spread capture ≈ +1.
     * spread = 0.10, mid = 100.05, fillPrice = 100.00 → capture = (mid - fill) * 1 / (0.5 * 0.10)
     *                                                            = 0.05 / 0.05 = 1.0
     */
    @Test
    void spreadCapture_positivForPassiveFill() {
        ParentOrder order = buyOrder(1, 100.0);

        List<ChildFill> fills = Collections.singletonList(
                fill(1_000L, 100.0, 1, 0.10, 100.05, 100.05, 100, 100));

        TcaResult result = TcaResult.fromFills(order, fills, null);

        assertEquals(1.0, result.getAvgSpreadCapture(), 1e-9,
                "passive fill at bid should earn full half-spread → +1.0");
    }

    /**
     * An aggressive BUY fill at the ask pays the spread → spread capture ≈ -1.
     * spread = 0.10, mid = 100.05, fillPrice = 100.10 → capture = (mid - fill) * 1 / (0.5 * 0.10)
     *                                                             = -0.05 / 0.05 = -1.0
     */
    @Test
    void spreadCapture_negativeForAggressiveFill() {
        ParentOrder order = buyOrder(1, 100.0);

        List<ChildFill> fills = Collections.singletonList(
                fill(1_000L, 100.10, 1, 0.10, 100.05, 100.05, 100, 100));

        TcaResult result = TcaResult.fromFills(order, fills, null);

        assertEquals(-1.0, result.getAvgSpreadCapture(), 1e-9,
                "aggressive fill at ask should pay full half-spread → -1.0");
    }

    // -----------------------------------------------------------------------
    // Test 5: Adverse selection – adverse vs favourable mid movement
    // -----------------------------------------------------------------------

    /**
     * BUY fill: mid moves up after fill → adverse (positive adverseSelectionBps).
     * midAtFill = 100.0, mid1s = 101.0 → return = 0.01 → bps = +100 bps
     */
    @Test
    void adverseSelection_adverseMidMovement() {
        ParentOrder order = buyOrder(1, 100.0);

        List<ChildFill> fills = Collections.singletonList(
                fill(1_000L, 100.0, 1, 0.10, 100.0, 101.0, 100, 100));

        TcaResult result = TcaResult.fromFills(order, fills, null);

        // sideSign=+1 * (101 - 100)/100 * 10000 = +100 bps
        assertEquals(100.0, result.getAvgAdverseSelectionBps(), 1e-6,
                "mid rising after BUY fill = adverse → +100 bps");
    }

    /**
     * BUY fill: mid moves down after fill → favourable (negative adverseSelectionBps).
     * midAtFill = 100.0, mid1s = 99.0 → return = -0.01 → bps = -100 bps
     */
    @Test
    void adverseSelection_favourableMidMovement() {
        ParentOrder order = buyOrder(1, 100.0);

        List<ChildFill> fills = Collections.singletonList(
                fill(1_000L, 100.0, 1, 0.10, 100.0, 99.0, 100, 100));

        TcaResult result = TcaResult.fromFills(order, fills, null);

        // sideSign=+1 * (99 - 100)/100 * 10000 = -100 bps
        assertEquals(-100.0, result.getAvgAdverseSelectionBps(), 1e-6,
                "mid falling after BUY fill = favourable → -100 bps");
    }

    // -----------------------------------------------------------------------
    // Edge case tests
    // -----------------------------------------------------------------------

    /** Null fills list → zero-value result without exception. */
    @Test
    void edgeCase_nullFills_returnsZeroResult() {
        ParentOrder order = buyOrder(100, 100.0);
        TcaResult result = TcaResult.fromFills(order, null, null);

        assertEquals(0.0, result.getFillRate(), 1e-9);
        assertEquals(0.0, result.getImplementationShortfall(), 1e-9);
        assertEquals(0L, result.getTotalFilledQuantity());
    }

    /** Zero quantity order → zero-value result without exception. */
    @Test
    void edgeCase_zeroQuantityOrder_returnsZeroResult() {
        ParentOrder order = new ParentOrder(
                "ZERO-QTY", Side.BUY, "BTCUSD", 0, 100.0, Double.NaN,
                0L, 0L, AlgoType.IS, 0.0);
        TcaResult result = TcaResult.fromFills(
                order, Collections.singletonList(fill(1_000L, 100.0, 0, 0.1, 100.0, 100.0, 100, 100)), null);

        assertEquals(0.0, result.getFillRate(), 1e-9);
    }

    /** Single fill → execution time should be zero. */
    @Test
    void edgeCase_singleFill_executionTimeIsZero() {
        ParentOrder order = buyOrder(10, 100.0);
        List<ChildFill> fills = Collections.singletonList(
                fill(5_000L, 100.0, 10, 0.05, 100.0, 100.0, 50, 50));

        TcaResult result = TcaResult.fromFills(order, fills, null);

        assertEquals(0L, result.getTotalExecutionTimeMs(),
                "single fill → execution time = 0 ms");
        assertEquals(1.0, result.getFillRate(), 1e-9);
    }
}
