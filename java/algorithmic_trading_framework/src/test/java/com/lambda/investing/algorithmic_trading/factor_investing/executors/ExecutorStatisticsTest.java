package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.OrderType;
import com.lambda.investing.model.trading.Verb;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

public class ExecutorStatisticsTest {

    private static final String INSTRUMENT_PK = "btceur_binance";
    private static final String ALGO_INFO = "testAlgo";
    private static final double PRICE_TICK = 0.01;

    private Instrument instrument;
    private ExecutorStatistics stats;

    @Before
    public void setUp() {
        instrument = new Instrument();
        instrument.setPrimaryKey(INSTRUMENT_PK);
        instrument.setMarket("binance");
        instrument.setPriceTick(PRICE_TICK);
        instrument.addMap();

        // Use a very large log interval so aggregate stats are not printed automatically during tests
        stats = new ExecutorStatistics(ALGO_INFO, instrument, Long.MAX_VALUE);
    }

    private ExecutionReport createExecutionReport(Verb verb, double price, double quantity, ExecutionReportStatus status) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setOrderType(OrderType.Limit);
        orderRequest.setVerb(verb);
        orderRequest.setPrice(price);
        orderRequest.setQuantity(quantity);
        orderRequest.setInstrument(INSTRUMENT_PK);
        orderRequest.setAlgorithmInfo(ALGO_INFO);
        orderRequest.setClientOrderId(UUID.randomUUID().toString());

        ExecutionReport er = new ExecutionReport(orderRequest);
        er.setLastQuantity(quantity);
        er.setQuantityFill(quantity);
        er.setTimestampCreation(System.currentTimeMillis());
        er.setExecutionReportStatus(status);
        return er;
    }

    @Test
    public void testInitialCountsAreZero() {
        Assert.assertEquals(0, stats.getTotalExecutions());
        Assert.assertEquals(0, stats.getSuccessfulExecutions());
        Assert.assertEquals(0, stats.getRejectedExecutions());
    }

    @Test
    public void testSuccessfulBuyExecutionIsRecorded() {
        long startTs = 1000L;
        long endTs = 1500L;
        double sentPrice = 100.00;
        double filledPrice = 100.02;
        double midPriceAtStart = 99.99;
        double midPriceAtFill = 100.01;
        double quantity = 10.0;

        stats.onExecutionStarted(startTs, Verb.Buy, sentPrice, midPriceAtStart);

        ExecutionReport er = createExecutionReport(Verb.Buy, filledPrice, quantity, ExecutionReportStatus.CompletelyFilled);
        stats.onExecutionFinished(endTs, er, midPriceAtFill);

        Assert.assertEquals(1, stats.getTotalExecutions());
        Assert.assertEquals(1, stats.getSuccessfulExecutions());
        Assert.assertEquals(0, stats.getRejectedExecutions());

        Assert.assertEquals(1, stats.getExecutionTimesMs().size());
        Assert.assertEquals(endTs - startTs, (long) stats.getExecutionTimesMs().get(0));

        Assert.assertEquals(1, stats.getSlippagesTicks().size());
        // slippage = (filledPrice - sentPrice) / priceTick = (100.02 - 100.00) / 0.01 = 2.0
        Assert.assertEquals(2.0, stats.getSlippagesTicks().get(0), 0.001);

        Assert.assertEquals(1, stats.getMidPriceMovementsTicks().size());
        // midPriceMovement = (midPriceAtFill - midPriceAtStart) / priceTick = (100.01 - 99.99) / 0.01 = 2.0
        Assert.assertEquals(2.0, stats.getMidPriceMovementsTicks().get(0), 0.001);

        Assert.assertEquals(1, stats.getQuantitiesFilled().size());
        Assert.assertEquals(quantity, stats.getQuantitiesFilled().get(0), 0.0001);
    }

    @Test
    public void testSuccessfulSellExecutionIsRecorded() {
        long startTs = 2000L;
        long endTs = 2300L;
        double sentPrice = 100.00;
        double filledPrice = 99.97;  // fill at lower price for sell -> worse fill
        double midPriceAtStart = 100.01;
        double midPriceAtFill = 99.98;
        double quantity = 5.0;

        stats.onExecutionStarted(startTs, Verb.Sell, sentPrice, midPriceAtStart);

        ExecutionReport er = createExecutionReport(Verb.Sell, filledPrice, quantity, ExecutionReportStatus.CompletelyFilled);
        stats.onExecutionFinished(endTs, er, midPriceAtFill);

        Assert.assertEquals(1, stats.getTotalExecutions());
        Assert.assertEquals(1, stats.getSuccessfulExecutions());

        // slippage for sell = -(filledPrice - sentPrice) / priceTick = -(99.97 - 100.00) / 0.01 = 3.0
        Assert.assertEquals(3.0, stats.getSlippagesTicks().get(0), 0.001);

        // midPriceMovement for sell = -(midPriceAtFill - midPriceAtStart) / priceTick = -(99.98 - 100.01) / 0.01 = 3.0
        Assert.assertEquals(3.0, stats.getMidPriceMovementsTicks().get(0), 0.001);
    }

    @Test
    public void testRejectedExecutionIsRecorded() {
        long startTs = 3000L;
        long endTs = 3100L;
        double sentPrice = 100.00;
        double midPriceAtStart = 100.00;
        double midPriceAtFill = 100.00;
        double quantity = 5.0;

        stats.onExecutionStarted(startTs, Verb.Buy, sentPrice, midPriceAtStart);

        ExecutionReport er = createExecutionReport(Verb.Buy, 0.0, quantity, ExecutionReportStatus.Rejected);
        er.setRejectReason("InsufficientFunds");
        stats.onExecutionFinished(endTs, er, midPriceAtFill);

        Assert.assertEquals(1, stats.getTotalExecutions());
        Assert.assertEquals(0, stats.getSuccessfulExecutions());
        Assert.assertEquals(1, stats.getRejectedExecutions());

        // No slippage or midprice movement tracked for rejections
        Assert.assertEquals(0, stats.getSlippagesTicks().size());
        Assert.assertEquals(0, stats.getMidPriceMovementsTicks().size());

        Assert.assertEquals(1, stats.getExecutionTimesMs().size());
        Assert.assertEquals(endTs - startTs, (long) stats.getExecutionTimesMs().get(0));
    }

    @Test
    public void testMultipleExecutionsAccumulate() {
        // First execution
        stats.onExecutionStarted(1000L, Verb.Buy, 100.00, 99.99);
        ExecutionReport er1 = createExecutionReport(Verb.Buy, 100.01, 10.0, ExecutionReportStatus.CompletelyFilled);
        stats.onExecutionFinished(1200L, er1, 100.00);

        // Second execution
        stats.onExecutionStarted(2000L, Verb.Sell, 100.05, 100.06);
        ExecutionReport er2 = createExecutionReport(Verb.Sell, 100.03, 8.0, ExecutionReportStatus.CompletelyFilled);
        stats.onExecutionFinished(2400L, er2, 100.04);

        // Third execution (rejected)
        stats.onExecutionStarted(3000L, Verb.Buy, 101.00, 101.00);
        ExecutionReport er3 = createExecutionReport(Verb.Buy, 0.0, 5.0, ExecutionReportStatus.Rejected);
        stats.onExecutionFinished(3050L, er3, 101.00);

        Assert.assertEquals(3, stats.getTotalExecutions());
        Assert.assertEquals(2, stats.getSuccessfulExecutions());
        Assert.assertEquals(1, stats.getRejectedExecutions());

        Assert.assertEquals(3, stats.getExecutionTimesMs().size());
        Assert.assertEquals(2, stats.getSlippagesTicks().size());
        Assert.assertEquals(2, stats.getMidPriceMovementsTicks().size());
        Assert.assertEquals(3, stats.getQuantitiesFilled().size());
    }
}
