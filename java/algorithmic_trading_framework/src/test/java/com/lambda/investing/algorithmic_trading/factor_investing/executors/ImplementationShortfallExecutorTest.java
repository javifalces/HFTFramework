package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.OrderRequestAction;
import com.lambda.investing.model.trading.Verb;
import org.junit.Assert;
import org.junit.Test;

public class ImplementationShortfallExecutorTest extends AbstractExecutorTest {

    private static final long TOTAL_DURATION_MS = 10_000L;
    private static final double TOTAL_QTY = 5.0;

    private ImplementationShortfallExecutor createIS(double urgency) {
        return new ImplementationShortfallExecutor(timeService, ALGO_INFO, instrument,
                connectorConfig, TOTAL_DURATION_MS, urgency);
    }

    // ------------------------------------------------------------------
    // increasePosition
    // ------------------------------------------------------------------

    @Test
    public void testIncreasesPositionSendsInitialOrder() {
        ImplementationShortfallExecutor executor = createIS(0.0);
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);

        boolean result = executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        Assert.assertTrue("increasePosition should return true", result);
        Assert.assertTrue("executor should be executing", executor.isExecuting());
        Assert.assertEquals("one order should be sent immediately", 1, tradingEngine.getSentOrders().size());
    }

    @Test
    public void testDecisionPriceIsSetToMidPriceAtArrival() {
        ImplementationShortfallExecutor executor = createIS(0.5);
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);

        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        double expectedMid = (BEST_BID + BEST_ASK) / 2.0;
        Assert.assertEquals("decision midprice should equal arrival mid",
                expectedMid, executor.getDecisionMidPrice(), PRICE_TICK);
    }

    @Test
    public void testIncreasesPositionReturnsFalseWhenAlreadyExecuting() {
        ImplementationShortfallExecutor executor = createIS(0.5);
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        boolean result = executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Sell, TOTAL_QTY, BEST_BID);
        Assert.assertFalse("second increasePosition while executing should return false", result);
        Assert.assertEquals(1, tradingEngine.getSentOrders().size());
    }

    // ------------------------------------------------------------------
    // Order price vs urgency
    // ------------------------------------------------------------------

    @Test
    public void testPassiveUrgencySendsOrderBelowBestAsk() {
        // urgency=0: initial order price should be at (or below) best ask
        ImplementationShortfallExecutor executor = createIS(0.0);
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        OrderRequest order = tradingEngine.getLastSentOrder();
        Assert.assertTrue("passive buy order price should be at or below best ask",
                order.getPrice() <= BEST_ASK + PRICE_TICK);
    }

    @Test
    public void testAggressiveUrgencySendsOrderAtOrAboveMid() {
        // urgency=1: initial order price should be aggressive (near or at best ask for buy)
        ImplementationShortfallExecutor executor = createIS(1.0);
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        OrderRequest order = tradingEngine.getLastSentOrder();
        double mid = (BEST_BID + BEST_ASK) / 2.0;
        Assert.assertTrue("aggressive buy order price should be above midprice",
                order.getPrice() >= mid);
    }

    // ------------------------------------------------------------------
    // Order modification over time
    // ------------------------------------------------------------------

    @Test
    public void testOrderIsModifiedAsTimePassesAndUrgencyIncreases() {
        // urgency=0: initial price is passive; after half the duration it should be more aggressive
        ImplementationShortfallExecutor executor = createIS(0.0);
        long t0 = timeService.getCurrentTimestamp();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, t0);
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(t0, Verb.Buy, TOTAL_QTY, BEST_ASK);

        OrderRequest firstOrder = tradingEngine.getLastSentOrder();
        double initialPrice = firstOrder.getPrice();

        // Acknowledge the order so we have a confirmed ID
        ExecutionReport active = createActiveReport(firstOrder.getClientOrderId(), initialPrice, TOTAL_QTY);
        executor.onExecutionReportUpdate(active);

        tradingEngine.clearSentOrders();

        // Advance time to near the deadline (90% of total duration) – urgency should be ~0.9
        advanceTimeAndSendDepth(executor, t0 + (long) (TOTAL_DURATION_MS * 0.9));

        // Expect at least one modify order to have been sent
        boolean modifyOrSendFound = tradingEngine.getSentOrders().stream()
                .anyMatch(o -> o.getOrderRequestAction() == OrderRequestAction.Modify
                        || o.getOrderRequestAction() == OrderRequestAction.Send);
        Assert.assertTrue("order should be modified as urgency increases over time", modifyOrSendFound);

        if (!tradingEngine.getSentOrders().isEmpty()) {
            double newPrice = tradingEngine.getLastSentOrder().getPrice();
            Assert.assertTrue("price should move toward or beyond initial price as urgency grows",
                    newPrice >= initialPrice - PRICE_TICK);
        }
    }

    // ------------------------------------------------------------------
    // Execution completion
    // ------------------------------------------------------------------

    @Test
    public void testExecutionCompletesOnCompletelyFilled() {
        ImplementationShortfallExecutor executor = createIS(0.5);
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        OrderRequest order = tradingEngine.getLastSentOrder();
        ExecutionReport cf = createCFReport(order.getClientOrderId(), BEST_ASK, TOTAL_QTY);
        executor.onExecutionReportUpdate(cf);

        Assert.assertFalse("execution should complete on CF", executor.isExecuting());
    }

    @Test
    public void testRejectedOrderEndsExecution() {
        ImplementationShortfallExecutor executor = createIS(0.5);
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

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
        ImplementationShortfallExecutor executor = createIS(0.5);
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        Assert.assertTrue(executor.isExecuting());
        executor.cancelAll();
        Assert.assertFalse("execution should stop after cancelAll", executor.isExecuting());
    }

    @Test
    public void testCancelAllSendsCancelRequestForActiveOrder() {
        ImplementationShortfallExecutor executor = createIS(0.5);
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // Acknowledge the order
        OrderRequest order = tradingEngine.getLastSentOrder();
        ExecutionReport active = createActiveReport(order.getClientOrderId(), BEST_ASK, TOTAL_QTY);
        executor.onExecutionReportUpdate(active);

        tradingEngine.clearSentOrders();
        executor.cancelAll();

        Assert.assertEquals("cancelAll should send one cancel request", 1, tradingEngine.getSentOrders().size());
        Assert.assertEquals(OrderRequestAction.Cancel,
                tradingEngine.getSentOrders().get(0).getOrderRequestAction());
    }

    // ------------------------------------------------------------------
    // Configuration validation
    // ------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testUrgencyAbove1Throws() {
        createIS(1.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeUrgencyThrows() {
        createIS(-0.1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroDurationThrows() {
        new ImplementationShortfallExecutor(timeService, ALGO_INFO, instrument, connectorConfig, 0, 0.5);
    }

    // ------------------------------------------------------------------
    // Statistics
    // ------------------------------------------------------------------

    @Test
    public void testStatisticsRecordedOnSuccessfulCompletion() {
        ImplementationShortfallExecutor executor = createIS(0.5);
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        OrderRequest order = tradingEngine.getLastSentOrder();
        ExecutionReport cf = createCFReport(order.getClientOrderId(), BEST_ASK, TOTAL_QTY);
        executor.onExecutionReportUpdate(cf);

        ExecutorStatistics stats = executor.executorStatistics;
        Assert.assertEquals(1, stats.getTotalExecutions());
        Assert.assertEquals(1, stats.getSuccessfulExecutions());
        Assert.assertEquals(0, stats.getRejectedExecutions());
    }
}
