package com.lambda.investing.trading_engine_connector.paper.market;

import com.lambda.investing.Configuration;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class OrderMatchEngineTest {

	private TestPaperTradingEngine paperTradingEngine;


	private String instrumentPk = "btcusd_binance";

	OrderMatchEngine orderMatchEngine;
	private Trade lastTradeListen;
	private Depth lastDepthListen;
	private List<ExecutionReport> lastExecutionReportListenList;
	private ExecutionReport lastExecutionReportListen;
	private final String algoId = "junitAlgo";

	// Test implementation of PaperTradingEngine
	private class TestPaperTradingEngine extends PaperTradingEngine {
		public TestPaperTradingEngine() {
			super(null, null, null, null);
		}

		@Override
		public void notifyTrade(Trade trade) {
			lastTradeListen = trade;
		}

		@Override
		public void notifyDepth(Depth depth) {
			lastDepthListen = depth;
		}

		@Override
		public void notifyExecutionReport(ExecutionReport executionReport) {
			lastExecutionReportListenList.add(executionReport);
			lastExecutionReportListen = executionReport;
		}
	}

	@Before
	public void setUp() {
		Configuration.BACKTEST_REFRESH_DEPTH_ORDER_REQUEST = true;
		Configuration.BACKTEST_REFRESH_DEPTH_TRADES = true;
		Configuration.REFRESH_DEPTH_ORDER_REQUEST_MS = 0;

		lastExecutionReportListenList = new ArrayList<>();
		Orderbook orderbook = new Orderbook(0.00001);
		paperTradingEngine = new TestPaperTradingEngine();
		lastTradeListen = null;
		lastDepthListen = null;

		orderMatchEngine = new OrderMatchEngine(orderbook, paperTradingEngine, instrumentPk);
	}

	private ExecutionReport createExecutionReport(Verb verb, double price, double quantity) {
		OrderRequest orderRequest = new OrderRequest();
		orderRequest.setOrderType(OrderType.Limit);
		orderRequest.setVerb(verb);
		orderRequest.setPrice(price);
		orderRequest.setQuantity(quantity);
		orderRequest.setQuantity(quantity);
		orderRequest.setInstrument(instrumentPk);
		orderRequest.setClientOrderId(UUID.randomUUID().toString());

		ExecutionReport executionReport = new ExecutionReport(orderRequest);
		executionReport.setLastQuantity(quantity);
		executionReport.setTimestampCreation(System.currentTimeMillis());
        executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletelyFilled);
		return executionReport;
	}

	private Depth createDepth(double bestBid, double bestAsk, double bestBidQty, double bestAskQty) {
		Depth depth = Depth.getInstance();
		depth.setTimestamp(System.currentTimeMillis());
		depth.setInstrument(instrumentPk);
		depth.setLevels(1);
		double[] asks = new double[]{bestAsk, bestAsk + 0.01};
		depth.setAsks(asks);

		double[] bids = new double[]{bestBid, bestBid - 0.01};
		depth.setBids(bids);

		double[] asksQ = new double[]{bestAskQty, bestAskQty};
		depth.setAsksQuantities(asksQ);

		double[] bidsQ = new double[]{bestBidQty, bestBidQty};
		depth.setBidsQuantities(bidsQ);


		String[] algorithms = new String[]{Depth.ALGORITHM_INFO_MM, Depth.ALGORITHM_INFO_MM};
		List<String>[] algorithmsList = new List[]{Arrays.asList(algorithms), Arrays.asList(algorithms)};
		depth.setAsksAlgorithmInfo(algorithmsList);
		depth.setBidsAlgorithmInfo(algorithmsList);

		depth.setLevelsFromData();
		return depth;

	}

	private Trade createTrade(double price, double quantity, Verb verb) {
		Trade trade = Trade.getInstance();
		trade.setTimestamp(System.currentTimeMillis());
		trade.setInstrument(instrumentPk);
		trade.setPrice(price);
		trade.setQuantity(quantity);
		trade.setVerb(verb);
		trade.setAlgorithmInfo(Depth.ALGORITHM_INFO_MM);
		return trade;
	}

	private OrderRequest createOrderRequest(double price, double quantity, Verb verb) {
		OrderRequest orderRequest = new OrderRequest();
		orderRequest.setOrderType(OrderType.Limit);
		orderRequest.setVerb(verb);
		orderRequest.setPrice(price);
		orderRequest.setQuantity(quantity);
		orderRequest.setInstrument(instrumentPk);
		orderRequest.setClientOrderId(UUID.randomUUID().toString());
		orderRequest.setAlgorithmInfo(algoId);
		orderRequest.setOrderRequestAction(OrderRequestAction.Send);
		orderRequest.setTimestampCreation(System.currentTimeMillis());
		return orderRequest;

	}

	private OrderRequest modifyOrderRequest(OrderRequest orderRequestOrig, double price, double quantity, Verb verb) {
		OrderRequest orderRequest = (OrderRequest) orderRequestOrig.clone();
		orderRequest.setOrderRequestAction(OrderRequestAction.Modify);
		orderRequest.setOrigClientOrderId(orderRequestOrig.getClientOrderId());
		orderRequest.setClientOrderId(UUID.randomUUID().toString());

		orderRequest.setVerb(verb);
		orderRequest.setPrice(price);
		orderRequest.setQuantity(quantity);
		orderRequest.setTimestampCreation(System.currentTimeMillis());
		return orderRequest;
	}

	private OrderRequest cancelOrderRequest(OrderRequest orderRequestOrig) {
		OrderRequest orderRequest = (OrderRequest) orderRequestOrig.clone();
		orderRequest.setOrderRequestAction(OrderRequestAction.Cancel);
		orderRequest.setOrigClientOrderId(orderRequestOrig.getClientOrderId());
		orderRequest.setClientOrderId(UUID.randomUUID().toString());
		orderRequest.setTimestampCreation(System.currentTimeMillis());
		return orderRequest;
	}

	@Test public void refreshFillMarketTradeTest() {
		Trade trade = createTrade(95.0, 5.0, Verb.Buy);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(trade, lastTradeListen);
	}

	@Test public void refreshMarketMakerDepthTest() {
		Depth depth = createDepth(85, 95, 5, 6);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		Depth depth2 = createDepth(82, 90, 2, 2);
		orderMatchEngine.refreshMarketMakerDepth(depth2);
		Assert.assertEquals(depth2.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth2.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);
	}

	@Test public void refreshAlgoAndFilledWithTradeSameSide() {
		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 5, 6);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		OrderRequest orderRequest = createOrderRequest(86, 3, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(86, lastExecutionReportListen.getPrice(), 0.001);

		lastExecutionReportListen = null;
		lastDepthListen = null;
		lastTradeListen = null;
		Trade trade = createTrade(90.0, 1.0, Verb.Buy);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(trade.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);
		Assert.assertNull(lastExecutionReportListen);
		Assert.assertNull(lastDepthListen);

	}

	@Test
	public void refreshAlgoAndFilledWithTradeThatNotFillAnything() {
		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 5, 6);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		OrderRequest orderRequest = createOrderRequest(95, 3, Verb.Sell);
		orderMatchEngine.orderRequest(orderRequest);
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(95, lastExecutionReportListen.getPrice(), 0.001);

		lastExecutionReportListen = null;
		lastDepthListen = null;
		lastTradeListen = null;
		Trade trade = createTrade(95.0, 1.0, Verb.Sell);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(trade.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);
		Assert.assertNull(lastExecutionReportListen);
		Assert.assertNull(lastDepthListen);

	}

	@Test
	public void refreshAlgoMidPriceAndFilledWithTradeSell() {
		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 5, 6);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		OrderRequest orderRequest = createOrderRequest(90, 3, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(90, lastExecutionReportListen.getPrice(), 0.001);
		//order is the best bid now
		Assert.assertEquals(orderRequest.getPrice(), lastDepthListen.getBestBid(), 0.001);
		Assert.assertEquals(orderRequest.getQuantity(), lastDepthListen.getBestBidQty(), 0.001);
		Assert.assertEquals(3, lastDepthListen.getBidLevels(), 0.001);

		//this trade is not crossing
		lastExecutionReportListen = null;
		lastDepthListen = null;
		lastTradeListen = null;
		Trade trade = createTrade(91.0, 1.0, Verb.Sell);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(trade.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);
		Assert.assertNull(lastExecutionReportListen);
		Assert.assertNull(lastDepthListen);

		//this trade crossing with the order at 90
		lastTradeListen = null;
		lastDepthListen = null;
		lastExecutionReportListen = null;
		trade = createTrade(90.0, 1.0, Verb.Sell);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(trade.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);
		Assert.assertNotNull(lastExecutionReportListen);
		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertNotNull(lastDepthListen);
		Assert.assertEquals(lastExecutionReportListen.getQuantity() - lastExecutionReportListen.getQuantityFill(),
				lastDepthListen.getBestBidQty(), 0.0001);
		Assert.assertEquals(lastTradeListen.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);

		//this trade crossing with the order at 90
		lastTradeListen = null;
		lastDepthListen = null;
		lastExecutionReportListen = null;
		trade = createTrade(89.0, 1.0, Verb.Sell);//orderRequestPrice will remain
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(orderRequest.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);
		Assert.assertNotNull(lastExecutionReportListen);
		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertNotNull(lastDepthListen);
		Assert.assertEquals(lastExecutionReportListen.getQuantity() - lastExecutionReportListen.getQuantityFill(),
				lastDepthListen.getBestBidQty(), 0.0001);
		Assert.assertEquals(lastTradeListen.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);

		//this trade crossing with the order at 90
		lastTradeListen = null;
		lastDepthListen = null;
		lastExecutionReportListen = null;
		trade = createTrade(88.0, 1.0, Verb.Sell);//we are going to fill the rest
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(orderRequest.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);
		Assert.assertNotNull(lastExecutionReportListen);
        Assert.assertEquals(ExecutionReportStatus.CompletelyFilled,
				lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertNotNull(lastDepthListen);
		Assert.assertEquals(0.0, lastExecutionReportListen.getQuantity() - lastExecutionReportListen.getQuantityFill(),
				0.0001);
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);

		//we are in the second row
		depth = createDepth(85, 95, 5, 6);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		orderRequest = createOrderRequest(80, 3, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(80, lastExecutionReportListen.getPrice(), 0.001);
		lastTradeListen = null;
		lastExecutionReportListen = null;
		lastDepthListen = null;
		trade = createTrade(88.0, 1.0, Verb.Sell);//we are going to fill the rest
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertNull(lastDepthListen);
		Assert.assertNull(lastExecutionReportListen);
		Assert.assertNotNull(lastTradeListen);
		Assert.assertEquals(trade.getPrice(), lastTradeListen.getPrice(), 0.001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.001);
	}

	@Test
	public void refreshAlgoMidPriceAndFilledWithTradeBuy() {
		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 5, 6);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		OrderRequest orderRequest = createOrderRequest(90, 3, Verb.Sell);
		orderMatchEngine.orderRequest(orderRequest);
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(90, lastExecutionReportListen.getPrice(), 0.001);
		//order is the best bid now
		Assert.assertEquals(orderRequest.getPrice(), lastDepthListen.getBestAsk(), 0.001);
		Assert.assertEquals(orderRequest.getQuantity(), lastDepthListen.getBestAskQty(), 0.001);
		Assert.assertEquals(3, lastDepthListen.getAskLevels(), 0.001);

		//this trade is not crossing
		lastExecutionReportListen = null;
		lastDepthListen = null;
		lastTradeListen = null;
		Trade trade = createTrade(89.0, 1.0, Verb.Buy);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(trade.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);
		Assert.assertNull(lastExecutionReportListen);
		Assert.assertNull(lastDepthListen);

		//this trade crossing with the order at 90
		lastTradeListen = null;
		lastDepthListen = null;
		lastExecutionReportListen = null;
		trade = createTrade(90.0, 1.0, Verb.Buy);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(trade.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);
		Assert.assertNotNull(lastExecutionReportListen);
		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertNotNull(lastDepthListen);
		Assert.assertEquals(lastExecutionReportListen.getQuantity() - lastExecutionReportListen.getQuantityFill(),
				lastDepthListen.getBestAskQty(), 0.0001);
		Assert.assertEquals(lastTradeListen.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertFalse(lastExecutionReportListen.isAggressor());

		//this trade crossing with the order at 90
		lastTradeListen = null;
		lastDepthListen = null;
		lastExecutionReportListen = null;
		trade = createTrade(91.0, 1.0, Verb.Buy);//orderRequestPrice will remain
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(orderRequest.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);
		Assert.assertNotNull(lastExecutionReportListen);
		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertNotNull(lastDepthListen);
		Assert.assertEquals(lastExecutionReportListen.getQuantity() - lastExecutionReportListen.getQuantityFill(),
				lastDepthListen.getBestAskQty(), 0.0001);
		Assert.assertEquals(lastTradeListen.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertFalse(lastExecutionReportListen.isAggressor());

		//this trade crossing with the order at 90
		lastTradeListen = null;
		lastDepthListen = null;
		lastExecutionReportListen = null;
		trade = createTrade(92.0, 1.0, Verb.Buy);//we are going to fill the rest
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(orderRequest.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);
		Assert.assertNotNull(lastExecutionReportListen);
        Assert.assertEquals(ExecutionReportStatus.CompletelyFilled,
				lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertNotNull(lastDepthListen);
		Assert.assertEquals(0.0, lastExecutionReportListen.getQuantity() - lastExecutionReportListen.getQuantityFill(),
				0.0001);
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertFalse(lastExecutionReportListen.isAggressor());

	}

	@Test
	public void refreshDepthRelativeTest() {
		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 5, 6);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		lastDepthListen = null;
		Depth depth1 = createDepth(84, 96, 5, 6);
		orderMatchEngine.refreshMarketMakerDepth(depth1);
		Assert.assertEquals(depth1.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth1.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		lastDepthListen = null;
		Depth depth2 = createDepth(84, 96, 6, 9);
		orderMatchEngine.refreshMarketMakerDepth(depth2);

		Assert.assertEquals(depth2.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth2.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(depth2.getBestBidQty(), lastDepthListen.getBestBidQty(), 0.0001);
		Assert.assertEquals(depth2.getBestAskQty(), lastDepthListen.getBestAskQty(), 0.0001);

		lastDepthListen = null;
		orderMatchEngine.refreshMarketMakerDepth(depth2);
		Assert.assertNotNull(lastDepthListen);

	}

	@Test
	public void refreshAlgoAndFilledWithTradeSells() {
		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 5, 6);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		OrderRequest orderRequest = createOrderRequest(86, 3, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(86, lastExecutionReportListen.getPrice(), 0.001);

		Assert.assertEquals(orderRequest.getPrice(), lastDepthListen.getBestBid(), 0.001);
		Assert.assertEquals(orderRequest.getQuantity(), lastDepthListen.getBestBidQty(), 0.001);
		Assert.assertEquals(3, lastDepthListen.getBidLevels(), 0.001);
		Assert.assertEquals(2, lastDepthListen.getAskLevels(), 0.001);

		Trade trade = createTrade(85.0, 1.0, Verb.Sell);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(orderRequest.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);

		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertFalse(lastExecutionReportListen.isAggressor());

		trade = createTrade(85.0, 1.0, Verb.Sell);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.0001);
		Assert.assertEquals(2, lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertFalse(lastExecutionReportListen.isAggressor());

		trade = createTrade(85.0, 1.0, Verb.Sell);
		orderMatchEngine.refreshFillMarketTrade(trade);
        Assert.assertEquals(ExecutionReportStatus.CompletelyFilled,
				lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.0001);
		Assert.assertEquals(3, lastExecutionReportListen.getQuantityFill(), 0.0001);

		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestBidQty(), lastDepthListen.getBestBidQty(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(depth.getBestAskQty(), lastDepthListen.getBestAskQty(), 0.0001);

		//just notify this trade! -> depth has to be the same
		lastExecutionReportListen = null;
		lastDepthListen = null;
		lastTradeListen = null;
		trade = createTrade(85.0, 1.0, Verb.Sell);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertNull(lastExecutionReportListen);
		Assert.assertNull(lastDepthListen);
		Assert.assertEquals(trade, lastTradeListen);

	}

	@Test public void refreshAlgoAndFilledWithTradeBuys() {
		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 5, 6);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		OrderRequest orderRequest = createOrderRequest(94, 3, Verb.Sell);
		orderMatchEngine.orderRequest(orderRequest);
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(94, lastExecutionReportListen.getPrice(), 0.001);

		Assert.assertEquals(orderRequest.getPrice(), lastDepthListen.getBestAsk(), 0.001);
		Assert.assertEquals(orderRequest.getQuantity(), lastDepthListen.getBestAskQty(), 0.001);
		Assert.assertEquals(2, lastDepthListen.getBidLevels(), 0.001);
		Assert.assertEquals(3, lastDepthListen.getAskLevels(), 0.001);

		Trade trade = createTrade(95.0, 1.0, Verb.Buy);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(orderRequest.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);

		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertEquals(false, lastExecutionReportListen.isAggressor());

		trade = createTrade(95.0, 1.0, Verb.Buy);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.0001);
		Assert.assertEquals(2, lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertFalse(lastExecutionReportListen.isAggressor());

		trade = createTrade(95.0, 1.0, Verb.Buy);
		orderMatchEngine.refreshFillMarketTrade(trade);
        Assert.assertEquals(ExecutionReportStatus.CompletelyFilled,
				lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.0001);
		Assert.assertEquals(3, lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertFalse(lastExecutionReportListen.isAggressor());

		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestBidQty(), lastDepthListen.getBestBidQty(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(depth.getBestAskQty(), lastDepthListen.getBestAskQty(), 0.0001);

		//just notify this trade! -> depth has to be the same
		lastExecutionReportListen = null;
		lastDepthListen = null;
		lastTradeListen = null;
		trade = createTrade(95.0, 1.0, Verb.Buy);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertNull(lastExecutionReportListen);
		Assert.assertNull(lastDepthListen);
		Assert.assertEquals(trade, lastTradeListen);
	}

	@Test public void algoBuyAggressive() {

		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 5, 6);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		lastDepthListen = null;
		OrderRequest orderRequest = createOrderRequest(99, 6, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);
        Assert.assertEquals(ExecutionReportStatus.CompletelyFilled,
				lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(95, lastExecutionReportListen.getPrice(), 0.001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getQuantityFill(), 0.001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getQuantity(), 0.001);

		Assert.assertEquals(depth.getAsks()[1], lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(depth.getAsksQuantities()[1], lastDepthListen.getBestAskQty(), 0.0001);

		Depth depth2 = createDepth(85, 95, 10, 12);//we increase size of the depth
		orderMatchEngine.refreshMarketMakerDepth(depth2);

		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		orderRequest = createOrderRequest(95, 8, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);
		//ER check
		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(95, lastExecutionReportListen.getPrice(), 0.001);
		Assert.assertEquals(6, lastExecutionReportListen.getLastQuantity(), 0.001);
		Assert.assertEquals(6, lastExecutionReportListen.getQuantityFill(), 0.001);
		Assert.assertEquals(8, lastExecutionReportListen.getQuantity(), 0.001);

		//DEPTH check
		Assert.assertEquals(orderRequest.getPrice(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(orderRequest.getQuantity() - lastExecutionReportListen.getQuantityFill(),
				lastDepthListen.getBestBidQty(), 0.0001);

		//TRADE check
		Assert.assertEquals(6, lastTradeListen.getQuantity(), 0.001);
		Assert.assertEquals(orderRequest.getPrice(), lastTradeListen.getPrice(), 0.001);

	}


	@Test
	public void refreshAlgoAndFilledWithDepthBuy() throws InterruptedException {
		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 5, 5);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		OrderRequest orderRequest = createOrderRequest(86, 3, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);
		Thread.sleep(100);//wait for ER to be processed
		depth = createDepth(80, 85, 5, 3);
		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		orderMatchEngine.refreshMarketMakerDepth(depth);

		//check ER
        Assert.assertEquals(ExecutionReportStatus.CompletelyFilled,
				lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.0001);
		Assert.assertEquals(false, lastExecutionReportListen.isAggressor());

		//check depth
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getAsks()[1], lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(depth.getAsksQuantities()[1], lastDepthListen.getBestAskQty(), 0.0001);

		//check trade
		Assert.assertEquals(orderRequest.getQuantity(), lastTradeListen.getQuantity(), 0.001);
		Assert.assertEquals(orderRequest.getPrice(), lastTradeListen.getPrice(), 0.001);
	}

	@Test public void refreshAlgoAndFilledWithDepthSell() {
		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 5, 5);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		OrderRequest orderRequest = createOrderRequest(96, 5, Verb.Sell);
		orderMatchEngine.orderRequest(orderRequest);

		depth = createDepth(98, 100, 5, 5);
		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		orderMatchEngine.refreshMarketMakerDepth(depth);

		//check ER
        Assert.assertEquals(ExecutionReportStatus.CompletelyFilled,
				lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.0001);
		Assert.assertEquals(false, lastExecutionReportListen.isAggressor());

		//check depth
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(depth.getBids()[1], lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBidsQuantities()[1], lastDepthListen.getBestBidQty(), 0.0001);

		//check trade
		Assert.assertEquals(orderRequest.getQuantity(), lastTradeListen.getQuantity(), 0.001);
		Assert.assertEquals(orderRequest.getPrice(), lastTradeListen.getPrice(), 0.001);
	}

	@Test public void refreshAlgoAndFilledCompletelyBuy() {
		//our order is going to buy completely ask side
		OrderRequest orderRequest = createOrderRequest(86, 3, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);
		Depth depth = createDepth(80, 85, 5, 1);
		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		orderMatchEngine.refreshMarketMakerDepth(depth);

		//check ER
		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);//market maker
		Assert.assertEquals(2, lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertEquals(1, lastExecutionReportListen.getLastQuantity(), 0.0001);
		Assert.assertEquals(false, lastExecutionReportListen.isAggressor());

		//check depth
		Assert.assertEquals(orderRequest.getPrice(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(0.0, lastDepthListen.getAskLevels(), 0.0001);
		Assert.assertEquals(1, lastDepthListen.getBestBidQty(), 0.0001);

		//check trade
		Assert.assertEquals(1, lastTradeListen.getQuantity(), 0.001);
		Assert.assertEquals(orderRequest.getPrice(), lastTradeListen.getPrice(), 0.001);
	}

	@Test public void refreshAlgoAndFilledCompletelySell() {
		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 2, 2);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		OrderRequest orderRequest = createOrderRequest(96, 5, Verb.Sell);
		orderMatchEngine.orderRequest(orderRequest);

		depth = createDepth(98, 100, 2, 5);
		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		orderMatchEngine.refreshMarketMakerDepth(depth);

		//check ER
		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(4, lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertEquals(2, lastExecutionReportListen.getLastQuantity(), 0.0001);
		Assert.assertEquals(false, lastExecutionReportListen.isAggressor());

		//check depth
		Assert.assertEquals(orderRequest.getPrice(), lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(0.0, lastDepthListen.getBidLevels(), 0.0001);
		Assert.assertEquals(1, lastDepthListen.getBestAskQty(), 0.0001);

		//check trade
		Assert.assertEquals(2, lastTradeListen.getQuantity(), 0.001);
		Assert.assertEquals(orderRequest.getPrice(), lastTradeListen.getPrice(), 0.001);
	}

	@Test public void modifyCancelOrdersBuy() {
		Depth depth = createDepth(85, 95, 2, 2);
		orderMatchEngine.refreshMarketMakerDepth(depth);

		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		OrderRequest orderRequest = createOrderRequest(83, 5, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);

		//check ER passive
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(lastExecutionReportListen.getPrice(), orderRequest.getPrice(), 0.0001);
		Assert.assertEquals(lastExecutionReportListen.getQuantity(), orderRequest.getQuantity(), 0.0001);
		//check depth
		Assert.assertEquals(lastDepthListen.getBidLevels(), 3, 0.0001);
		Assert.assertEquals(lastDepthListen.getBids()[2], orderRequest.getPrice(), 0.0001);

		//modify!
		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		orderRequest = modifyOrderRequest(orderRequest, 82.5, 5, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);
		//check ER passive
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(lastExecutionReportListen.getPrice(), orderRequest.getPrice(), 0.0001);
		Assert.assertEquals(lastExecutionReportListen.getQuantity(), orderRequest.getQuantity(), 0.0001);
		//check depth
		Assert.assertEquals(lastDepthListen.getBidLevels(), 3, 0.0001);
		Assert.assertEquals(lastDepthListen.getBids()[2], orderRequest.getPrice(), 0.0001);

		//modify!
		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		orderRequest = modifyOrderRequest(orderRequest, 85, 5, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);
		//check ER passive
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(lastExecutionReportListen.getPrice(), orderRequest.getPrice(), 0.0001);
		Assert.assertEquals(lastExecutionReportListen.getQuantity(), orderRequest.getQuantity(), 0.0001);
		//check depth
		Assert.assertEquals(2, lastDepthListen.getBidLevels(), 0.0001);
		Assert.assertEquals(lastDepthListen.getBestBid(), orderRequest.getPrice(), 0.0001);
		Assert.assertEquals(7, lastDepthListen.getBestBidQty(), 0.0001);

		//cancel!
		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		orderRequest = cancelOrderRequest(orderRequest);
		orderMatchEngine.orderRequest(orderRequest);
		//check ER passive
		Assert.assertEquals(ExecutionReportStatus.Cancelled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(2, lastDepthListen.getBidLevels(), 0.0001);
		Assert.assertEquals(lastDepthListen.getBestBid(), orderRequest.getPrice(), 0.0001);
		Assert.assertEquals(2, lastDepthListen.getBestBidQty(), 0.0001);

	}

	@Test public void modifyCancelOrdersSell() {
		Depth depth = createDepth(75, 81, 2, 2);
		orderMatchEngine.refreshMarketMakerDepth(depth);

		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		OrderRequest orderRequest = createOrderRequest(83, 5, Verb.Sell);
		orderMatchEngine.orderRequest(orderRequest);

		//check ER passive
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(lastExecutionReportListen.getPrice(), orderRequest.getPrice(), 0.0001);
		Assert.assertEquals(lastExecutionReportListen.getQuantity(), orderRequest.getQuantity(), 0.0001);
		//check depth
		Assert.assertEquals(lastDepthListen.getAskLevels(), 3, 0.0001);
		Assert.assertEquals(lastDepthListen.getAsks()[2], orderRequest.getPrice(), 0.0001);

		//modify!
		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		orderRequest = modifyOrderRequest(orderRequest, 82.5, 5, Verb.Sell);
		orderMatchEngine.orderRequest(orderRequest);
		//check ER passive
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(lastExecutionReportListen.getPrice(), orderRequest.getPrice(), 0.0001);
		Assert.assertEquals(lastExecutionReportListen.getQuantity(), orderRequest.getQuantity(), 0.0001);
		//check depth
		Assert.assertEquals(lastDepthListen.getAskLevels(), 3, 0.0001);
		Assert.assertEquals(lastDepthListen.getAsks()[2], orderRequest.getPrice(), 0.0001);

		//modify!
		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		orderRequest = modifyOrderRequest(orderRequest, 81, 5, Verb.Sell);
		orderMatchEngine.orderRequest(orderRequest);
		//check ER passive
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(lastExecutionReportListen.getPrice(), orderRequest.getPrice(), 0.0001);
		Assert.assertEquals(lastExecutionReportListen.getQuantity(), orderRequest.getQuantity(), 0.0001);
		//check depth
		Assert.assertEquals(2, lastDepthListen.getAskLevels(), 0.0001);
		Assert.assertEquals(lastDepthListen.getBestAsk(), orderRequest.getPrice(), 0.0001);
		Assert.assertEquals(7, lastDepthListen.getBestAskQty(), 0.0001);

		//cancel!
		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		orderRequest = cancelOrderRequest(orderRequest);
		orderMatchEngine.orderRequest(orderRequest);
		//check ER passive
		Assert.assertEquals(ExecutionReportStatus.Cancelled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(2, lastDepthListen.getAskLevels(), 0.0001);
		Assert.assertEquals(lastDepthListen.getBestAsk(), orderRequest.getPrice(), 0.0001);
		Assert.assertEquals(2, lastDepthListen.getBestAskQty(), 0.0001);

	}

	@Test public void rejectionNotFound() {
		Depth depth = createDepth(75, 81, 2, 2);
		orderMatchEngine.refreshMarketMakerDepth(depth);

		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		OrderRequest orderRequest = createOrderRequest(83, 5, Verb.Sell);
		orderRequest.setOrderRequestAction(OrderRequestAction.Cancel);
		orderMatchEngine.orderRequest(orderRequest);

		Assert.assertEquals(ExecutionReportStatus.CancelRejected, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertNull(lastDepthListen);

		lastExecutionReportListen = null;
		orderRequest.setOrderRequestAction(OrderRequestAction.Modify);
		orderRequest.setOrigClientOrderId("fail");
		orderRequest.setClientOrderId("other");
		orderMatchEngine.orderRequest(orderRequest);
		Assert.assertEquals(ExecutionReportStatus.Rejected, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertNull(lastDepthListen);

	}

	@Test public void orderModifyCancelOnOrderbook() {
		Depth depth = createDepth(75, 81, 2, 2);
		orderMatchEngine.refreshMarketMakerDepth(depth);

		int bidLevels = lastDepthListen.getBidLevels();
		int askLevels = lastDepthListen.getAskLevels();
		Assert.assertEquals(bidLevels, askLevels);
		lastExecutionReportListen = null;
		lastTradeListen = null;
		lastDepthListen = null;
		OrderRequest orderRequest = createOrderRequest(70, 5, Verb.Buy);
		orderRequest.setOrderRequestAction(OrderRequestAction.Send);
		orderMatchEngine.orderRequest(orderRequest);

		int bidLevels2 = lastDepthListen.getBidLevels();
		Assert.assertEquals(lastDepthListen.getAskLevels(), askLevels);
		Assert.assertEquals(bidLevels + 1, bidLevels2);

		lastExecutionReportListen = null;
		lastTradeListen = null;
		lastDepthListen = null;
		OrderRequest orderRequest2 = modifyOrderRequest(orderRequest, 69, 4, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest2);

		int bidLevels3 = lastDepthListen.getBidLevels();
		Assert.assertEquals(lastDepthListen.getAskLevels(), askLevels);
		Assert.assertEquals(bidLevels2, bidLevels3);
		Assert.assertEquals(bidLevels + 1, bidLevels3);

		lastExecutionReportListen = null;
		lastTradeListen = null;
		lastDepthListen = null;
		OrderRequest orderRequest3 = cancelOrderRequest(orderRequest2);
		orderMatchEngine.orderRequest(orderRequest3);
		int bidLevels4 = lastDepthListen.getBidLevels();
		Assert.assertEquals(lastDepthListen.getAskLevels(), askLevels);
		Assert.assertEquals(bidLevels, bidLevels4);

	}

	@Test
	public void refreshAlgoAndFilledVeryBigMarketOrder() {
		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 2, 2);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);
		lastExecutionReportListenList.clear();
		OrderRequest orderRequest = createOrderRequest(96, 10, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);


		//check ER
		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());

		Assert.assertEquals(4, lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertEquals(2, lastExecutionReportListen.getLastQuantity(), 0.0001);
		Assert.assertEquals(true, lastExecutionReportListen.isAggressor());

		//check depth
		Assert.assertEquals(Double.MAX_VALUE, lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(orderRequest.getPrice(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(orderRequest.getQuantity() - lastExecutionReportListen.getQuantityFill(), lastDepthListen.getBestBidQty(), 0.0001);
		Assert.assertEquals(3, lastDepthListen.getBidLevels(), 0.0001);
		Assert.assertEquals(0, lastDepthListen.getBestAskQty(), 0.0001);

		//check trade
		Assert.assertEquals(2, lastTradeListen.getQuantity(), 0.001);
		Assert.assertEquals(depth.getAsks()[1], lastTradeListen.getPrice(), 0.001);

	}

	@Test
	public void orderInLevelRelativeNotFilled() {
		lastDepthListen = null;
		lastTradeListen = null;
		lastExecutionReportListen = null;

		Depth depth = createDepth(85, 95, 2, 2);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(depth.getLevels(), 2, 0.0001);
		lastDepthListen = null;

		lastExecutionReportListenList.clear();
		OrderRequest orderRequest = createOrderRequest(85, 1, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(depth.getLevels(), 2, 0.0001);
		Assert.assertEquals(depth.getBestBidQty() + orderRequest.getQuantity(), lastDepthListen.getBestBidQty(), 0.0001);

		lastExecutionReportListen = null;

		Trade trade = createTrade(85, 2, Verb.Sell);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertNull(lastExecutionReportListen);


		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertNull(lastExecutionReportListen);

		Trade trade1 = createTrade(85, 3, Verb.Sell);
		orderMatchEngine.refreshFillMarketTrade(trade1);
        Assert.assertEquals(ExecutionReportStatus.CompletelyFilled, lastExecutionReportListen.getExecutionReportStatus());

	}

}
