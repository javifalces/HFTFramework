package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.OrderRequestAction;
import com.lambda.investing.model.trading.Verb;
import org.junit.Assert;
import org.junit.Test;

public class VWAPExecutorTest extends AbstractExecutorTest {

    private static final long TOTAL_DURATION_MS = 10_000L;
    private static final double PARTICIPATION_RATE = 0.1; // 10%
    private static final double TOTAL_QTY = 5.0;

    private VWAPExecutor createVWAP() {
        // Use a known minChildOrderQty matching QTY_TICK so that even small participation triggers an order
        return new VWAPExecutor(timeService, ALGO_INFO, instrument, connectorConfig,
                TOTAL_DURATION_MS, PARTICIPATION_RATE, QTY_TICK);
    }

    // ------------------------------------------------------------------
    // increasePosition
    // ------------------------------------------------------------------

    @Test
    public void testIncreasesPositionSetsExecutingState() {
        VWAPExecutor executor = createVWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);

        boolean result = executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        Assert.assertTrue("increasePosition should return true", result);
        Assert.assertTrue("executor should be in executing state", executor.isExecuting());
        // No order sent yet – waiting for volume
        Assert.assertEquals("no order sent before any volume observed", 0, tradingEngine.getSentOrders().size());
    }

    @Test
    public void testIncreasesPositionReturnsFalseWhenAlreadyExecuting() {
        VWAPExecutor executor = createVWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        boolean result = executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Sell, TOTAL_QTY, BEST_BID);
        Assert.assertFalse("second increasePosition while executing should return false", result);
    }

    // ------------------------------------------------------------------
    // Volume participation
    // ------------------------------------------------------------------

    @Test
    public void testMarketTradeTriggersChildOrder() {
        VWAPExecutor executor = createVWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // Observe a large market trade – participation qty = 100 * 0.1 = 10.0, but capped to remaining qty (5.0)
        Trade largeTrade = createTrade(BEST_ASK, 100.0, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(largeTrade);

        Assert.assertEquals("one child order should be sent after trade", 1, tradingEngine.getSentOrders().size());
        OrderRequest childOrder = tradingEngine.getLastSentOrder();
        Assert.assertEquals("child order verb should match increasePosition verb", Verb.Buy, childOrder.getVerb());
        Assert.assertTrue("child order price should be at best ask", childOrder.getPrice() >= BEST_ASK - PRICE_TICK);
    }

    @Test
    public void testSmallTradeAccumulatesUntilMinQtyReached() {
        VWAPExecutor executor = createVWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // First very small trade: participation = 0.001 * 0.1 = 0.0001 < minChildQty
        Trade tiny = createTrade(BEST_ASK, 0.001, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(tiny);
        Assert.assertEquals("too small to trigger order yet", 0, tradingEngine.getSentOrders().size());

        // Second small trade that pushes accumulated qty over minChildOrderQty
        Trade large = createTrade(BEST_ASK, 1.0, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(large);
        Assert.assertEquals("order should be sent once accumulation >= minChildOrderQty",
                1, tradingEngine.getSentOrders().size());
    }

    // ------------------------------------------------------------------
    // Time-based fallback
    // ------------------------------------------------------------------

    @Test
    public void testTimeBasedFallbackSendsOrderWhenNoVolume() {
        VWAPExecutor executor = createVWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        long t0 = timeService.getCurrentTimestamp();
        executor.increasePosition(t0, Verb.Buy, TOTAL_QTY, BEST_ASK);
        Assert.assertEquals(0, tradingEngine.getSentOrders().size());

        // Advance time to 50% of total duration without any trades observed
        advanceTimeAndSendDepth(executor, t0 + TOTAL_DURATION_MS / 2 + 1);

        // 50% time elapsed → expect a catch-up order for ~50% of total qty
        Assert.assertEquals("time-based fallback should send a catch-up order", 1, tradingEngine.getSentOrders().size());
        OrderRequest catchUp = tradingEngine.getLastSentOrder();
        Assert.assertTrue("catch-up order qty > 0", catchUp.getQuantity() > 0);
    }

    // ------------------------------------------------------------------
    // Execution completion
    // ------------------------------------------------------------------

    @Test
    public void testExecutionCompletesAfterAllQuantityFilled() {
        VWAPExecutor executor = createVWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // Trigger one large trade to get a child order for full qty
        Trade bigTrade = createTrade(BEST_ASK, TOTAL_QTY / PARTICIPATION_RATE + 1, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(bigTrade);

        Assert.assertEquals("child order should be sent", 1, tradingEngine.getSentOrders().size());

        // Fill the child order completely
        OrderRequest childOrder = tradingEngine.getLastSentOrder();
        ExecutionReport cf = createCFReport(childOrder.getClientOrderId(), BEST_ASK, TOTAL_QTY);
        executor.onExecutionReportUpdate(cf);

        Assert.assertFalse("execution should be complete after full fill", executor.isExecuting());
    }

    // ------------------------------------------------------------------
    // cancelAll
    // ------------------------------------------------------------------

    @Test
    public void testCancelAllStopsExecution() {
        VWAPExecutor executor = createVWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        Assert.assertTrue(executor.isExecuting());
        executor.cancelAll();
        Assert.assertFalse("execution should stop after cancelAll", executor.isExecuting());
    }

    @Test
    public void testCancelAllSendsCancelWhenOrderActive() {
        VWAPExecutor executor = createVWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // Trigger child order
        Trade bigTrade = createTrade(BEST_ASK, 100.0, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(bigTrade);
        Assert.assertEquals(1, tradingEngine.getSentOrders().size());

        tradingEngine.clearSentOrders();
        executor.cancelAll();

        Assert.assertEquals("cancelAll should send cancel request", 1, tradingEngine.getSentOrders().size());
        Assert.assertEquals(OrderRequestAction.Cancel,
                tradingEngine.getSentOrders().get(0).getOrderRequestAction());
    }

    // ------------------------------------------------------------------
    // Configuration validation
    // ------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testZeroParticipationRateThrows() {
        new VWAPExecutor(timeService, ALGO_INFO, instrument, connectorConfig, TOTAL_DURATION_MS, 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeDurationThrows() {
        new VWAPExecutor(timeService, ALGO_INFO, instrument, connectorConfig, -1, PARTICIPATION_RATE);
    }

    // ------------------------------------------------------------------
    // Statistics
    // ------------------------------------------------------------------

    @Test
    public void testStatisticsRecordedOnCompletion() {
        VWAPExecutor executor = createVWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // trigger child order
        Trade bigTrade = createTrade(BEST_ASK, TOTAL_QTY / PARTICIPATION_RATE + 10, timeService.getCurrentTimestamp());
        executor.onTradeUpdate(bigTrade);

        OrderRequest childOrder = tradingEngine.getLastSentOrder();
        ExecutionReport cf = createCFReport(childOrder.getClientOrderId(), BEST_ASK, TOTAL_QTY);
        executor.onExecutionReportUpdate(cf);

        ExecutorStatistics stats = executor.executorStatistics;
        Assert.assertEquals(1, stats.getTotalExecutions());
        Assert.assertEquals(1, stats.getSuccessfulExecutions());
    }
}
