package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.OrderRequestAction;
import com.lambda.investing.model.trading.Verb;
import org.junit.Assert;
import org.junit.Test;

public class TWAPExecutorTest extends AbstractExecutorTest {

    private static final long TOTAL_DURATION_MS = 10_000L; // 10 seconds
    private static final int NUM_SLICES = 5;
    private static final double TOTAL_QTY = 10.0;

    private TWAPExecutor createTWAP() {
        return new TWAPExecutor(timeService, ALGO_INFO, instrument, connectorConfig,
                TOTAL_DURATION_MS, NUM_SLICES);
    }

    // ------------------------------------------------------------------
    // increasePosition
    // ------------------------------------------------------------------

    @Test
    public void testIncreasesPositionSendsFirstSlice() {
        TWAPExecutor executor = createTWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth); // seed lastDepth

        boolean result = executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        Assert.assertTrue("increasePosition should return true", result);
        Assert.assertTrue("executor should be in executing state", executor.isExecuting());

        // first slice should be sent immediately
        Assert.assertEquals("first slice order should be sent", 1, tradingEngine.getSentOrders().size());
        OrderRequest firstOrder = tradingEngine.getLastSentOrder();
        Assert.assertEquals("first slice verb should be Buy", Verb.Buy, firstOrder.getVerb());
        // slice qty = totalQty / numberOfSlices
        Assert.assertEquals("first slice qty", TOTAL_QTY / NUM_SLICES, firstOrder.getQuantity(), 0.001);
    }

    @Test
    public void testIncreasesPositionReturnsFalseWhenAlreadyExecuting() {
        TWAPExecutor executor = createTWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // second increasePosition while executing should return false
        boolean result = executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Sell, TOTAL_QTY, BEST_BID);
        Assert.assertFalse("second increasePosition should return false", result);
        // no extra order should have been sent
        Assert.assertEquals(1, tradingEngine.getSentOrders().size());
    }

    // ------------------------------------------------------------------
    // Slice scheduling
    // ------------------------------------------------------------------

    @Test
    public void testNextSliceIsSentAfterSliceInterval() {
        TWAPExecutor executor = createTWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);

        long t0 = timeService.getCurrentTimestamp();
        executor.increasePosition(t0, Verb.Buy, TOTAL_QTY, BEST_ASK);
        Assert.assertEquals(1, tradingEngine.getSentOrders().size());

        // Fill the first slice
        OrderRequest firstOrder = tradingEngine.getSentOrders().get(0);
        ExecutionReport cfFirst = createCFReport(firstOrder.getClientOrderId(), BEST_ASK, TOTAL_QTY / NUM_SLICES);
        executor.onExecutionReportUpdate(cfFirst);
        Assert.assertEquals("1 slice completed, still executing", 1, tradingEngine.getSentOrders().size());

        // Advance time past the second slice interval
        long sliceInterval = TOTAL_DURATION_MS / NUM_SLICES; // 2000 ms
        advanceTimeAndSendDepth(executor, t0 + sliceInterval + 1);

        // Second slice should now be sent
        Assert.assertEquals("second slice should be sent", 2, tradingEngine.getSentOrders().size());
    }

    @Test
    public void testAllSlicesCompleteExecution() {
        TWAPExecutor executor = createTWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);

        long t0 = timeService.getCurrentTimestamp();
        executor.increasePosition(t0, Verb.Buy, TOTAL_QTY, BEST_ASK);

        long sliceInterval = TOTAL_DURATION_MS / NUM_SLICES;
        for (int i = 0; i < NUM_SLICES; i++) {
            // Fill the active slice
            OrderRequest order = tradingEngine.getSentOrders().get(tradingEngine.getSentOrders().size() - 1);
            ExecutionReport cf = createCFReport(order.getClientOrderId(), BEST_ASK, TOTAL_QTY / NUM_SLICES);
            executor.onExecutionReportUpdate(cf);

            if (i < NUM_SLICES - 1) {
                // Advance time to trigger next slice
                advanceTimeAndSendDepth(executor, t0 + (long) (i + 1) * sliceInterval + 1);
            }
        }

        Assert.assertFalse("execution should be complete", executor.isExecuting());
    }

    // ------------------------------------------------------------------
    // cancelAll
    // ------------------------------------------------------------------

    @Test
    public void testCancelAllStopsExecution() {
        TWAPExecutor executor = createTWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        Assert.assertTrue(executor.isExecuting());
        executor.cancelAll();
        Assert.assertFalse("execution should be stopped after cancelAll", executor.isExecuting());
    }

    @Test
    public void testCancelAllSendsCancelOrder() {
        TWAPExecutor executor = createTWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);
        executor.increasePosition(timeService.getCurrentTimestamp(), Verb.Buy, TOTAL_QTY, BEST_ASK);

        // Acknowledge first slice so we have a confirmed order id
        OrderRequest firstSlice = tradingEngine.getLastSentOrder();
        ExecutionReport active = createActiveReport(firstSlice.getClientOrderId(), BEST_ASK, TOTAL_QTY / NUM_SLICES);
        executor.onExecutionReportUpdate(active);

        tradingEngine.clearSentOrders();
        executor.cancelAll();

        Assert.assertEquals("cancelAll should send one cancel request", 1, tradingEngine.getSentOrders().size());
        Assert.assertEquals("cancel request action must be Cancel",
                OrderRequestAction.Cancel, tradingEngine.getSentOrders().get(0).getOrderRequestAction());
    }

    // ------------------------------------------------------------------
    // Statistics
    // ------------------------------------------------------------------

    @Test
    public void testStatisticsAreRecordedOnCompletion() {
        TWAPExecutor executor = createTWAP();
        Depth initialDepth = createDepth(BEST_BID, BEST_ASK, timeService.getCurrentTimestamp());
        executor.onDepthUpdate(initialDepth);

        long t0 = timeService.getCurrentTimestamp();
        executor.increasePosition(t0, Verb.Buy, TOTAL_QTY, BEST_ASK);

        long sliceInterval = TOTAL_DURATION_MS / NUM_SLICES;
        for (int i = 0; i < NUM_SLICES; i++) {
            OrderRequest order = tradingEngine.getSentOrders().get(tradingEngine.getSentOrders().size() - 1);
            ExecutionReport cf = createCFReport(order.getClientOrderId(), BEST_ASK, TOTAL_QTY / NUM_SLICES);
            executor.onExecutionReportUpdate(cf);
            if (i < NUM_SLICES - 1) {
                advanceTimeAndSendDepth(executor, t0 + (long) (i + 1) * sliceInterval + 1);
            }
        }

        ExecutorStatistics stats = executor.executorStatistics;
        Assert.assertEquals("one completed execution should be recorded", 1, stats.getTotalExecutions());
        Assert.assertEquals("execution should be marked successful", 1, stats.getSuccessfulExecutions());
        Assert.assertEquals("no rejections", 0, stats.getRejectedExecutions());
    }

    // ------------------------------------------------------------------
    // Configuration validation
    // ------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testZeroSlicesThrows() {
        new TWAPExecutor(timeService, ALGO_INFO, instrument, connectorConfig, TOTAL_DURATION_MS, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroDurationThrows() {
        new TWAPExecutor(timeService, ALGO_INFO, instrument, connectorConfig, 0, NUM_SLICES);
    }
}
