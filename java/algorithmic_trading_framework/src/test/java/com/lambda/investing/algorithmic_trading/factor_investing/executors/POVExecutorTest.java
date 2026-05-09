package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.OrderRequestAction;
import com.lambda.investing.model.trading.Verb;
import org.junit.Assert;
import org.junit.Test;

public class POVExecutorTest extends AbstractExecutorTest {

    private static final double PARTICIPATION_RATE = 0.1; // 10%
    private static final double TOTAL_QTY = 5.0;

    private POVExecutor createPOV() {
        // minChildOrderQty = QTY_TICK so that even small accumulated participation triggers an order
        return new POVExecutor(timeService, ALGO_INFO, instrument, connectorConfig,
                PARTICIPATION_RATE, QTY_TICK, 0.0);
    }

    // ------------------------------------------------------------------
    // increasePosition
    // ------------------------------------------------------------------

    @Test
    public void testIncreasesPositionSetsExecutingState() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);

        boolean result = executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        Assert.assertTrue("increasePosition should return true", result);
        Assert.assertTrue("executor should be in executing state", executor.isExecuting());
        // POV sends NO order at start — it waits for market volume
        Assert.assertEquals("no order sent before any volume observed", 0, tradingEngine.getSentOrders().size());
    }

    @Test
    public void testIncreasesPositionReturnsFalseWhenAlreadyExecuting() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        boolean result = executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Sell, TOTAL_QTY, BEST_BID);
        Assert.assertFalse("second increasePosition while executing should return false", result);
        Assert.assertEquals("no extra orders sent", 0, tradingEngine.getSentOrders().size());
    }

    // ------------------------------------------------------------------
    // Volume participation
    // ------------------------------------------------------------------

    @Test
    public void testMarketTradeTriggersChildOrder() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // Trade of 100 → participation = 100 * 0.1 = 10 > minChildOrderQty, capped at remaining qty (5)
        Trade largeTrade = createTrade(BEST_ASK, 100.0, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(largeTrade);

        Assert.assertEquals("one child order should be sent after trade", 1, tradingEngine.getSentOrders().size());
        OrderRequest childOrder = tradingEngine.getLastSentOrder();
        Assert.assertEquals("child order verb should match increasePosition verb", Verb.Buy, childOrder.getVerb());
        Assert.assertTrue("child order quantity should be capped at remaining total",
                childOrder.getQuantity() <= TOTAL_QTY + QTY_TICK);
    }

    @Test
    public void testChildOrderPriceIsAtBestAskForBuy() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        Trade trade = createTrade(BEST_ASK, 100.0, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(trade);

        Assert.assertEquals("one child order sent", 1, tradingEngine.getSentOrders().size());
        OrderRequest order = tradingEngine.getLastSentOrder();
        Assert.assertEquals("Buy order should be at best ask", BEST_ASK, order.getPrice(), PRICE_TICK);
    }

    @Test
    public void testChildOrderPriceIsAtBestBidForSell() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Sell, TOTAL_QTY, BEST_BID);

        Trade trade = createTrade(BEST_BID, 100.0, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(trade);

        Assert.assertEquals("one child order sent", 1, tradingEngine.getSentOrders().size());
        OrderRequest order = tradingEngine.getLastSentOrder();
        Assert.assertEquals("Sell order should be at best bid", BEST_BID, order.getPrice(), PRICE_TICK);
    }

    @Test
    public void testSmallTradeAccumulatesBeforeOrderIsSent() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // Very small trade: 0.001 * 0.1 = 0.0001 < minChildOrderQty (0.001)
        Trade tiny = createTrade(BEST_ASK, 0.001, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(tiny);
        Assert.assertEquals("tiny trade should not yet trigger an order", 0, tradingEngine.getSentOrders().size());

        // Larger trade that pushes pending over minChildOrderQty
        Trade larger = createTrade(BEST_ASK, 1.0, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(larger);
        Assert.assertEquals("order should be sent once accumulated qty >= minChildOrderQty",
                1, tradingEngine.getSentOrders().size());
    }

    @Test
    public void testMultipleTradesAccumulate() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // Each tiny trade of 0.005 contributes 0.005 * 0.1 = 0.0005 — below minChildOrderQty (0.001)
        for (int i = 0; i < 3; i++) {
            Trade t = createTrade(BEST_ASK, 0.005, timeService.getCurrentTimestamp());
            executor.onTradeUpdate(t);
        }
        // 3 * 0.0005 = 0.0015 — still below minChildOrderQty for the last individual trade,
        // but we've accumulated enough after the 2nd trade (0.001) and the 3rd should trigger.
        // Let's clear and try again with truly sub-threshold trades
        // Reset: use 6 trades of 0.001 each → contribution per trade = 0.0001, well below 0.001
        executor.cancelAll();
        tradingEngine.clearSentOrders();

        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        for (int i = 0; i < 5; i++) {
            Trade t = createTrade(BEST_ASK, 0.001, timeService.getCurrentTimestamp());
            executor.onTradeUpdate(t);
        }
        // 5 * (0.001 * 0.1) = 5 * 0.0001 = 0.0005 < minChildOrderQty (0.001)
        Assert.assertEquals("sub-threshold accumulation should not trigger order", 0, tradingEngine.getSentOrders().size());

        // One larger trade that pushes over the threshold
        Trade pushTrade = createTrade(BEST_ASK, 10.0, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(pushTrade);
        Assert.assertEquals("order should be sent after enough volume accumulates",
                1, tradingEngine.getSentOrders().size());
    }

    // ------------------------------------------------------------------
    // No time-based fallback
    // ------------------------------------------------------------------

    @Test
    public void testNoOrderSentByTimeAloneWhenNoVolumeObserved() {
        // Increase the timeout so the AbstractExecutor safety net doesn't interfere
        POVExecutor executor = createPOV();
        executor.setTimeoutIsExecutingMs(Long.MAX_VALUE);

        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        long t0 = timeService.getCurrentTimestamp();
        executor.increasePosition(t0, Verb.Buy, TOTAL_QTY, BEST_ASK);

        // Advance time substantially — POV should NOT send any order without volume
        advanceTimeAndSendDepth(executor, t0 + 60_000L); // +60 seconds
        advanceTimeAndSendDepth(executor, t0 + 120_000L); // +2 minutes

        Assert.assertEquals("POV must not send orders based on time alone", 0, tradingEngine.getSentOrders().size());
        Assert.assertTrue("executor should still be executing (waiting for volume)", executor.isExecuting());
    }

    // ------------------------------------------------------------------
    // Execution completion
    // ------------------------------------------------------------------

    @Test
    public void testExecutionCompletesAfterFullFill() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // Trigger child order via large trade
        Trade bigTrade = createTrade(BEST_ASK, TOTAL_QTY / PARTICIPATION_RATE + 10, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(bigTrade);

        Assert.assertEquals(1, tradingEngine.getSentOrders().size());

        // Completely fill the child order
        OrderRequest childOrder = tradingEngine.getLastSentOrder();
        ExecutionReport cf = createCFReport(childOrder.getClientOrderId(), BEST_ASK, TOTAL_QTY);
        executor.onExecutionReportUpdate(cf);

        Assert.assertFalse("execution should complete after full fill", executor.isExecuting());
    }

    @Test
    public void testPartialFillsThenFullFillCompletes() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        double halfQty = TOTAL_QTY / 2;

        // --- First wave: fill half ---
        Trade trade1 = createTrade(BEST_ASK, TOTAL_QTY / PARTICIPATION_RATE + 10, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(trade1);

        OrderRequest firstOrder = tradingEngine.getLastSentOrder();
        ExecutionReport cf1 = createCFReport(firstOrder.getClientOrderId(), BEST_ASK, halfQty);
        executor.onExecutionReportUpdate(cf1);

        Assert.assertTrue("should still be executing after partial total fill", executor.isExecuting());

        // --- Second wave: fill remaining half ---
        Trade trade2 = createTrade(BEST_ASK, TOTAL_QTY / PARTICIPATION_RATE + 10, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(trade2);

        OrderRequest secondOrder = tradingEngine.getSentOrders().get(tradingEngine.getSentOrders().size() - 1);
        ExecutionReport cf2 = createCFReport(secondOrder.getClientOrderId(), BEST_ASK, halfQty);
        executor.onExecutionReportUpdate(cf2);

        Assert.assertFalse("execution should complete after remaining qty filled", executor.isExecuting());
    }

    @Test
    public void testRejectedOrderEndsExecution() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        Trade bigTrade = createTrade(BEST_ASK, 100.0, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(bigTrade);

        OrderRequest order = tradingEngine.getLastSentOrder();
        ExecutionReport rejected = createRejectedReport(order.getClientOrderId(), "InsufficientFunds");
        executor.onExecutionReportUpdate(rejected);

        Assert.assertFalse("execution should end on Rejected", executor.isExecuting());
    }

    // ------------------------------------------------------------------
    // cancelAll
    // ------------------------------------------------------------------

    @Test
    public void testCancelAllStopsExecution() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        Assert.assertTrue(executor.isExecuting());
        executor.cancelAll();
        Assert.assertFalse("execution should stop after cancelAll", executor.isExecuting());
    }

    @Test
    public void testCancelAllSendsCancelWhenActiveOrderConfirmed() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // Trigger a child order
        Trade bigTrade = createTrade(BEST_ASK, 100.0, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(bigTrade);
        Assert.assertEquals(1, tradingEngine.getSentOrders().size());

        // Acknowledge order so we have a confirmed ID
        OrderRequest order = tradingEngine.getLastSentOrder();
        ExecutionReport active = createActiveReport(order.getClientOrderId(), BEST_ASK, TOTAL_QTY);
        executor.onExecutionReportUpdate(active);

        tradingEngine.clearSentOrders();
        executor.cancelAll();

        Assert.assertEquals("cancelAll should send one cancel request", 1, tradingEngine.getSentOrders().size());
        Assert.assertEquals(OrderRequestAction.Cancel,
                tradingEngine.getSentOrders().get(0).getOrderRequestAction());
    }

    @Test
    public void testCancelAllWithNoActiveOrderStillStopsExecution() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // No trades — no order sent
        executor.cancelAll();

        Assert.assertEquals("no cancel needed when no active order", 0, tradingEngine.getSentOrders().size());
        Assert.assertFalse("execution should stop", executor.isExecuting());
    }

    // ------------------------------------------------------------------
    // maxParticipationRate cap
    // ------------------------------------------------------------------

    @Test
    public void testMaxParticipationRateDefaultsToParticipationRate() {
        // When maxParticipationRate is 0 (default), it is set equal to participationRate
        POVExecutor executor = new POVExecutor(timeService, ALGO_INFO, instrument, connectorConfig,
                PARTICIPATION_RATE, QTY_TICK, 0.0);
        Assert.assertEquals("maxParticipationRate should default to participationRate",
                PARTICIPATION_RATE, executor.getMaxParticipationRate(), 1e-9);
    }

    @Test
    public void testMaxParticipationRateAboveParticipationRateIsValid() {
        // maxParticipationRate > participationRate is valid and accepted without exception
        POVExecutor executor = new POVExecutor(timeService, ALGO_INFO, instrument, connectorConfig,
                PARTICIPATION_RATE, QTY_TICK, 0.5);
        Assert.assertEquals(0.5, executor.getMaxParticipationRate(), 1e-9);
    }

    // ------------------------------------------------------------------
    // Configuration validation
    // ------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testZeroParticipationRateThrows() {
        new POVExecutor(timeService, ALGO_INFO, instrument, connectorConfig, 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParticipationRateAbove1Throws() {
        new POVExecutor(timeService, ALGO_INFO, instrument, connectorConfig, 1.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxRateBelowParticipationRateThrows() {
        new POVExecutor(timeService, ALGO_INFO, instrument, connectorConfig, 0.5, QTY_TICK, 0.3);
    }

    // ------------------------------------------------------------------
    // Statistics
    // ------------------------------------------------------------------

    @Test
    public void testStatisticsRecordedOnSuccessfulCompletion() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        Trade bigTrade = createTrade(BEST_ASK, TOTAL_QTY / PARTICIPATION_RATE + 10, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(bigTrade);

        OrderRequest childOrder = tradingEngine.getLastSentOrder();
        ExecutionReport cf = createCFReport(childOrder.getClientOrderId(), BEST_ASK, TOTAL_QTY);
        executor.onExecutionReportUpdate(cf);

        ExecutorStatistics stats = executor.executorStatistics;
        Assert.assertEquals("one execution recorded", 1, stats.getTotalExecutions());
        Assert.assertEquals("execution should be recorded as successful", 1, stats.getSuccessfulExecutions());
        Assert.assertEquals("no rejections", 0, stats.getRejectedExecutions());
    }

    @Test
    public void testStatisticsRecordedOnRejection() {
        POVExecutor executor = createPOV();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        Trade bigTrade = createTrade(BEST_ASK, 100.0, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(bigTrade);

        OrderRequest order = tradingEngine.getLastSentOrder();
        ExecutionReport rejected = createRejectedReport(order.getClientOrderId(), "InsufficientFunds");
        executor.onExecutionReportUpdate(rejected);

        ExecutorStatistics stats = executor.executorStatistics;
        Assert.assertEquals("one execution recorded", 1, stats.getTotalExecutions());
        Assert.assertEquals("no successes", 0, stats.getSuccessfulExecutions());
        Assert.assertEquals("one rejection recorded", 1, stats.getRejectedExecutions());
    }
}
