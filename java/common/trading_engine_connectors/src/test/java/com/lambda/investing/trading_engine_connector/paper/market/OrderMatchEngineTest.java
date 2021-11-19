package com.lambda.investing.trading_engine_connector.paper.market;

import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

@RunWith(MockitoJUnitRunner.class) public class OrderMatchEngineTest {

	private PaperTradingEngine paperTradingEngine;

	private String instrumentPk = "btcusd_binance";

	OrderMatchEngine orderMatchEngine;
	private Trade lastTradeListen;
	private Depth lastDepthListen;
	private ExecutionReport lastExecutionReportListen;
	private String algoId = "junitAlgo";

	@Before public void setUp() throws Exception {
		OrderMatchEngine.REFRESH_DEPTH_ORDER_REQUEST = true;
		OrderMatchEngine.REFRESH_DEPTH_ORDER_REQUEST = true;

		Orderbook orderbook = new Orderbook(0.00001);
		paperTradingEngine = Mockito.mock(PaperTradingEngine.class);
		lastTradeListen = null;
		lastDepthListen = null;
		//trade listener
		doAnswer(new Answer<Void>() {

			public Void answer(InvocationOnMock invocation) {
				//				Object[] args = invocation.getArguments();
				Trade trade = invocation.getArgumentAt(0, Trade.class);
				lastTradeListen = trade;
				return null;
			}
		}).when(paperTradingEngine).notifyTrade(any(Trade.class));

		doAnswer(new Answer<Void>() {

			public Void answer(InvocationOnMock invocation) {
				//				Object[] args = invocation.getArguments();
				Depth depth = invocation.getArgumentAt(0, Depth.class);
				lastDepthListen = depth;
				return null;
			}
		}).when(paperTradingEngine).notifyDepth(any(Depth.class));

		doAnswer(new Answer<Void>() {

			public Void answer(InvocationOnMock invocation) {
				//				Object[] args = invocation.getArguments();
				ExecutionReport executionReport = invocation.getArgumentAt(0, ExecutionReport.class);
				lastExecutionReportListen = executionReport;
				return null;
			}
		}).when(paperTradingEngine).notifyExecutionReport(any(ExecutionReport.class));

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
		executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
		return executionReport;
	}

	private Depth createDepth(double bestBid, double bestAsk, double bestBidQty, double bestAskQty) {
		Depth depth = new Depth();
		depth.setTimestamp(System.currentTimeMillis());
		depth.setInstrument(instrumentPk);
		depth.setLevels(1);
		Double[] asks = new Double[] { bestAsk, bestAsk + 0.01 };
		depth.setAsks(asks);

		Double[] bids = new Double[] { bestBid, bestBid - 0.01 };
		depth.setBids(bids);

		Double[] asksQ = new Double[] { bestAskQty, bestAskQty };
		depth.setAsksQuantities(asksQ);

		Double[] bidsQ = new Double[] { bestBidQty, bestBidQty };
		depth.setBidsQuantities(bidsQ);

		String[] algorithms = new String[] { Depth.ALGORITHM_INFO_MM, Depth.ALGORITHM_INFO_MM };
		depth.setAsksAlgorithmInfo(algorithms);
		depth.setBidsAlgorithmInfo(algorithms);
		depth.setLevelsFromData();
		return depth;

	}

	private Trade createTrade(double price, double quantity, Verb verb) {
		Trade trade = new Trade();
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
		return orderRequest;
	}

	private OrderRequest cancelOrderRequest(OrderRequest orderRequestOrig) {
		OrderRequest orderRequest = (OrderRequest) orderRequestOrig.clone();
		orderRequest.setOrderRequestAction(OrderRequestAction.Cancel);
		orderRequest.setOrigClientOrderId(orderRequestOrig.getClientOrderId());
		orderRequest.setClientOrderId(UUID.randomUUID().toString());
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

	@Test public void refreshAlgoAndFilledWithTradeSameSide2() {
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

	@Test public void refreshAlgoMidPriceAndFilledWithTrade() {
		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 5, 6);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		OrderRequest orderRequest = createOrderRequest(90, 3, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);
		Assert.assertEquals(ExecutionReportStatus.Active, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(90, lastExecutionReportListen.getPrice(), 0.001);

		lastExecutionReportListen = null;
		lastDepthListen = null;
		lastTradeListen = null;
		Trade trade = createTrade(91.0, 1.0, Verb.Sell);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(trade.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);
		Assert.assertNull(lastExecutionReportListen);
		Assert.assertNull(lastDepthListen);

		lastTradeListen = null;
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

		lastTradeListen = null;
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

		lastTradeListen = null;
		trade = createTrade(88.0, 1.0, Verb.Sell);//we are going to fill the rest
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(orderRequest.getPrice(), lastTradeListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity(), 0.0001);
		Assert.assertNotNull(lastExecutionReportListen);
		Assert.assertEquals(ExecutionReportStatus.CompletellyFilled,
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
		Assert.assertEquals(trade.getPrice(), lastTradeListen.getPrice());
		Assert.assertEquals(trade.getQuantity(), lastTradeListen.getQuantity());
	}

	@Test public void refreshAlgoAndFilledWithTradeSells() {
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

		trade = createTrade(85.0, 1.0, Verb.Sell);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.0001);
		Assert.assertEquals(2, lastExecutionReportListen.getQuantityFill(), 0.0001);

		trade = createTrade(85.0, 1.0, Verb.Sell);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(ExecutionReportStatus.CompletellyFilled,
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

		trade = createTrade(95.0, 1.0, Verb.Buy);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(ExecutionReportStatus.PartialFilled, lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(orderRequest.getPrice(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(trade.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.0001);
		Assert.assertEquals(2, lastExecutionReportListen.getQuantityFill(), 0.0001);

		trade = createTrade(95.0, 1.0, Verb.Buy);
		orderMatchEngine.refreshFillMarketTrade(trade);
		Assert.assertEquals(ExecutionReportStatus.CompletellyFilled,
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
		Assert.assertEquals(ExecutionReportStatus.CompletellyFilled,
				lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(95, lastExecutionReportListen.getPrice(), 0.001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getQuantityFill(), 0.001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getQuantity(), 0.001);

		Assert.assertEquals(depth.getAsks()[1], lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(depth.getAsksQuantities()[1], lastDepthListen.getBestAskQty(), 0.0001);

		orderMatchEngine.refreshMarketMakerDepth(depth);
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

	@Test public void refreshAlgoAndFilledWithDepthBuy() {
		lastDepthListen = null;
		Depth depth = createDepth(85, 95, 5, 5);
		orderMatchEngine.refreshMarketMakerDepth(depth);
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);

		OrderRequest orderRequest = createOrderRequest(86, 3, Verb.Buy);
		orderMatchEngine.orderRequest(orderRequest);

		depth = createDepth(80, 85, 5, 3);
		lastDepthListen = null;
		lastExecutionReportListen = null;
		lastTradeListen = null;
		orderMatchEngine.refreshMarketMakerDepth(depth);

		//check ER
		Assert.assertEquals(ExecutionReportStatus.CompletellyFilled,
				lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(depth.getBestAsk(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.0001);

		//check depth
		Assert.assertEquals(depth.getBestBid(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getAsks()[1], lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(depth.getAsksQuantities()[1], lastDepthListen.getBestAskQty(), 0.0001);

		//check trade
		Assert.assertEquals(orderRequest.getQuantity(), lastTradeListen.getQuantity(), 0.001);
		Assert.assertEquals(85, lastTradeListen.getPrice(), 0.001);
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
		Assert.assertEquals(ExecutionReportStatus.CompletellyFilled,
				lastExecutionReportListen.getExecutionReportStatus());
		Assert.assertEquals(depth.getBestBid(), lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertEquals(orderRequest.getQuantity(), lastExecutionReportListen.getLastQuantity(), 0.0001);

		//check depth
		Assert.assertEquals(depth.getBestAsk(), lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(depth.getBids()[1], lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(depth.getBidsQuantities()[1], lastDepthListen.getBestBidQty(), 0.0001);

		//check trade
		Assert.assertEquals(orderRequest.getQuantity(), lastTradeListen.getQuantity(), 0.001);
		Assert.assertEquals(98, lastTradeListen.getPrice(), 0.001);
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
		Assert.assertEquals(depth.getAsks()[1], lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(2, lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertEquals(1, lastExecutionReportListen.getLastQuantity(), 0.0001);

		//check depth
		Assert.assertEquals(orderRequest.getPrice(), lastDepthListen.getBestBid(), 0.0001);
		Assert.assertEquals(0.0, lastDepthListen.getAskLevels(), 0.0001);
		Assert.assertEquals(1, lastDepthListen.getBestBidQty(), 0.0001);

		//check trade
		Assert.assertEquals(1, lastTradeListen.getQuantity(), 0.001);
		Assert.assertEquals(depth.getAsks()[1], lastTradeListen.getPrice(), 0.001);
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
		Assert.assertEquals(depth.getBids()[1], lastExecutionReportListen.getPrice(), 0.0001);
		Assert.assertEquals(4, lastExecutionReportListen.getQuantityFill(), 0.0001);
		Assert.assertEquals(2, lastExecutionReportListen.getLastQuantity(), 0.0001);

		//check depth
		Assert.assertEquals(orderRequest.getPrice(), lastDepthListen.getBestAsk(), 0.0001);
		Assert.assertEquals(0.0, lastDepthListen.getBidLevels(), 0.0001);
		Assert.assertEquals(1, lastDepthListen.getBestAskQty(), 0.0001);

		//check trade
		Assert.assertEquals(2, lastTradeListen.getQuantity(), 0.001);
		Assert.assertEquals(depth.getBids()[1], lastTradeListen.getPrice(), 0.001);
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
}
