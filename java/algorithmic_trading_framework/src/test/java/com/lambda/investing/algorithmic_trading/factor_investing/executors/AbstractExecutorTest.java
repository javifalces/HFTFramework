package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.time_service.TimeService;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.OrderRequestAction;
import com.lambda.investing.model.trading.OrderType;
import com.lambda.investing.model.trading.Verb;
import org.junit.Before;

import java.util.UUID;

/**
 * Base class providing shared setup (instrument, depth, stubs, time service)
 * for executor unit tests.
 */
public abstract class AbstractExecutorTest {

    protected static final String INSTRUMENT_PK = "btcusd_binance";
    protected static final String ALGO_INFO = "testAlgo";
    protected static final double PRICE_TICK = 0.01;
    protected static final double QTY_TICK = 0.001;
    protected static final double BEST_BID = 100.00;
    protected static final double BEST_ASK = 100.02;

    protected Instrument instrument;
    protected TimeService timeService;
    protected ExecutorTestUtils.StubTradingEngineConnector tradingEngine;
    protected ExecutorTestUtils.StubMarketDataProvider marketDataProvider;
    protected AlgorithmConnectorConfiguration connectorConfig;

    @Before
    public void setUpBase() {
        instrument = new Instrument();
        instrument.setPrimaryKey(INSTRUMENT_PK);
        instrument.setMarket("binance");
        instrument.setPriceTick(PRICE_TICK);
        instrument.setQuantityTick(QTY_TICK);
        instrument.addMap();

        timeService = new TimeService();
        timeService.setCurrentTimestamp(1_000_000L);

        tradingEngine = new ExecutorTestUtils.StubTradingEngineConnector();
        marketDataProvider = new ExecutorTestUtils.StubMarketDataProvider();
        connectorConfig = new AlgorithmConnectorConfiguration(tradingEngine, marketDataProvider);
    }

    /** Creates a simple one-level Depth snapshot for the test instrument. */
    protected Depth createDepth(double bid, double ask, long timestampMs) {
        Depth depth = Depth.getInstance();
        depth.setInstrument(INSTRUMENT_PK);
        depth.setTimestamp(timestampMs);

        double[] bids = {bid};
        double[] bidsQty = {10.0};
        double[] asks = {ask};
        double[] asksQty = {10.0};
        depth.setBids(bids);
        depth.setBidsQuantities(bidsQty);
        depth.setBidLevels(bids.length);
        depth.setAsks(asks);
        depth.setAsksQuantities(asksQty);
        depth.setAskLevels(asks.length);
        depth.setLevels(1);
        depth.setLevelsFromData();
        return depth;
    }

    /** Creates a market Trade event for the test instrument. */
    protected Trade createTrade(double price, double qty, long timestampMs) {
        Trade trade = Trade.getInstance();
        trade.setInstrument(INSTRUMENT_PK);
        trade.setPrice(price);
        trade.setQuantity(qty);
        trade.setTimestamp(timestampMs);
        return trade;
    }

    /** Creates an ExecutionReport that simulates a completely-filled event. */
    protected ExecutionReport createCFReport(String clientOrderId, double price, double qty) {
        OrderRequest req = new OrderRequest();
        req.setOrderType(OrderType.Limit);
        req.setVerb(Verb.Buy);
        req.setPrice(price);
        req.setQuantity(qty);
        req.setInstrument(INSTRUMENT_PK);
        req.setAlgorithmInfo(ALGO_INFO);
        req.setClientOrderId(clientOrderId);

        ExecutionReport er = new ExecutionReport(req);
        er.setLastQuantity(qty);
        er.setQuantityFill(qty);
        er.setPrice(price);
        er.setTimestampCreation(timeService.getCurrentTimestamp());
        er.setExecutionReportStatus(ExecutionReportStatus.CompletelyFilled);
        return er;
    }

    /** Creates a rejected ExecutionReport. */
    protected ExecutionReport createRejectedReport(String clientOrderId, String reason) {
        OrderRequest req = new OrderRequest();
        req.setOrderType(OrderType.Limit);
        req.setVerb(Verb.Buy);
        req.setPrice(0.0);
        req.setQuantity(1.0);
        req.setInstrument(INSTRUMENT_PK);
        req.setAlgorithmInfo(ALGO_INFO);
        req.setClientOrderId(clientOrderId);

        ExecutionReport er = new ExecutionReport(req);
        er.setLastQuantity(0.0);
        er.setQuantityFill(0.0);
        er.setPrice(0.0);
        er.setRejectReason(reason);
        er.setTimestampCreation(timeService.getCurrentTimestamp());
        er.setExecutionReportStatus(ExecutionReportStatus.Rejected);
        return er;
    }

    /** Creates an Active (acknowledged) ExecutionReport. */
    protected ExecutionReport createActiveReport(String clientOrderId, double price, double qty) {
        OrderRequest req = new OrderRequest();
        req.setOrderType(OrderType.Limit);
        req.setVerb(Verb.Buy);
        req.setPrice(price);
        req.setQuantity(qty);
        req.setInstrument(INSTRUMENT_PK);
        req.setAlgorithmInfo(ALGO_INFO);
        req.setClientOrderId(clientOrderId);

        ExecutionReport er = new ExecutionReport(req);
        er.setLastQuantity(0.0);
        er.setQuantityFill(0.0);
        er.setPrice(price);
        er.setTimestampCreation(timeService.getCurrentTimestamp());
        er.setExecutionReportStatus(ExecutionReportStatus.Active);
        return er;
    }

    /**
     * Advances the time service and sends a depth update to the executor.
     * Returns the depth snapshot that was sent.
     */
    protected Depth advanceTimeAndSendDepth(AbstractExecutor executor, long newTimestampMs) {
        timeService.setCurrentTimestamp(newTimestampMs);
        Depth depth = createDepth(BEST_BID, BEST_ASK, newTimestampMs);
        executor.onDepthUpdate(depth);
        return depth;
    }
}
