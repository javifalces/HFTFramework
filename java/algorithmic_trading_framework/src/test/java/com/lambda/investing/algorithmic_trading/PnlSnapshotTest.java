package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class PnlSnapshotTest {

	String instrumentPk = "btceur_binance";

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

		Double[] bids = new Double[] { bestBid, bestAsk - 0.01 };
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

	@Test public void testPnlUpdatesClosePosition() {
		PnlSnapshot pnlSnapshot = new PnlSnapshot();
		//buy 100@101
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Buy, 101, 100));
		Assert.assertEquals(10100, pnlSnapshot.netInvestment, 0.0001);
		Assert.assertEquals(0.0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(0.0, pnlSnapshot.unrealizedPnl, 0.0001);

		//update depth to see unrealized changed
		pnlSnapshot.updateDepth(createDepth(99, 101, 500, 500));
		Assert.assertEquals(0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(-200, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(-200, pnlSnapshot.totalPnl, 0.0001);
		Assert.assertEquals(10100, pnlSnapshot.netInvestment, 0.0001);

		//sell 100@99
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Sell, 99, 100));
		Assert.assertEquals(-200, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(0, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(0, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(-200, pnlSnapshot.totalPnl, 0.0001);
		Assert.assertEquals(0.0, pnlSnapshot.netInvestment, 0.0001);
	}

	@Test public void testPnlOrderedUpdatesClosePosition() {
		PnlSnapshot pnlSnapshot = new PnlSnapshotOrders();
		//buy 100@101
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Buy, 101, 100));
		Assert.assertEquals(10100, pnlSnapshot.netInvestment, 0.0001);
		Assert.assertEquals(0.0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(0.0, pnlSnapshot.unrealizedPnl, 0.0001);

		//update depth to see unrealized changed
		pnlSnapshot.updateDepth(createDepth(99, 101, 500, 500));
		Assert.assertEquals(0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(-200, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(-200, pnlSnapshot.totalPnl, 0.0001);
		Assert.assertEquals(10100, pnlSnapshot.netInvestment, 0.0001);

		//sell 100@99
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Sell, 99, 100));
		Assert.assertEquals(-200, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(0, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(0, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(-200, pnlSnapshot.totalPnl, 0.0001);
		Assert.assertEquals(0.0, pnlSnapshot.netInvestment, 0.0001);
	}

	@Test public void testPnlOrderedUpdatesClosePositionInvert() {
		PnlSnapshot pnlSnapshot = new PnlSnapshotOrders();
		//sell 100@101
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Sell, 101, 100));
		Assert.assertEquals(10100, pnlSnapshot.netInvestment, 0.0001);
		Assert.assertEquals(-100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(0.0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(0.0, pnlSnapshot.unrealizedPnl, 0.0001);

		//update depth to see unrealized changed
		pnlSnapshot.updateDepth(createDepth(99, 101, 500, 500));
		Assert.assertEquals(0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(-100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(0, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(0, pnlSnapshot.totalPnl, 0.0001);
		Assert.assertEquals(10100, pnlSnapshot.netInvestment, 0.0001);

		//sell 100@99
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Buy, 99, 100));
		Assert.assertEquals(200, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(0, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(0, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(200, pnlSnapshot.totalPnl, 0.0001);
		Assert.assertEquals(0.0, pnlSnapshot.netInvestment, 0.0001);
	}

	@Test public void testPnlUpdatesOpenPosition() {
		PnlSnapshot pnlSnapshot = new PnlSnapshot();
		//buy 100@101
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Buy, 101, 100));

		/// last depth received best bid 99
		pnlSnapshot.updateDepth(createDepth(99, 101, 500, 500));

		Assert.assertEquals(0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(-200, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(-200, pnlSnapshot.totalPnl, 0.0001);
		Assert.assertEquals(10100, pnlSnapshot.netInvestment, 0.0001);

		//sell 100@99
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Sell, 99, 100));

	}

	@Test public void testPnlOrderedUpdatesOpenPosition() {
		PnlSnapshotOrders pnlSnapshot = new PnlSnapshotOrders();
		//buy 100@101
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Buy, 101, 100));

		/// last depth received best bid 99
		pnlSnapshot.updateDepth(createDepth(99, 101, 500, 500));

		Assert.assertEquals(0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(-200, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(-200, pnlSnapshot.totalPnl, 0.0001);
		Assert.assertEquals(10100, pnlSnapshot.netInvestment, 0.0001);

		//sell 100@99
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Sell, 99, 100));
	}

	@Test public void testPnlUpdatesIncreasingOpenPosition() {
		PnlSnapshot pnlSnapshot = new PnlSnapshot();
		//buy 100@101
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Buy, 101, 100));
		/// last depth received best bid 99
		pnlSnapshot.updateDepth(createDepth(99, 103, 500, 500));
		//buy 100@103
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Buy, 103, 100));

		Assert.assertEquals(0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(200, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(-600, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(-600, pnlSnapshot.totalPnl, 0.0001);

		/// last depth received best bid 105
		pnlSnapshot.updateDepth(createDepth(105, 107, 500, 500));
		Assert.assertEquals(0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(200, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(600, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(600, pnlSnapshot.totalPnl, 0.0001);

		//sell 100@105
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Sell, 105, 100));
		Assert.assertEquals(300, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(300, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(600, pnlSnapshot.totalPnl, 0.0001);

		/// last depth received best bid 105
		pnlSnapshot.updateDepth(createDepth(102, 107, 500, 500));
		Assert.assertEquals(300, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(0.0, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(300, pnlSnapshot.totalPnl, 0.0001);

	}

	@Test public void testPnlOrderedUpdatesIncreasingOpenPosition() {
		PnlSnapshotOrders pnlSnapshot = new PnlSnapshotOrders();
		//buy 100@101
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Buy, 101, 100));
		/// last depth received best bid 99
		pnlSnapshot.updateDepth(createDepth(99, 103, 500, 500));
		//buy 100@103
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Buy, 103, 100));

		Assert.assertEquals(0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(200, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(-600, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(-600, pnlSnapshot.totalPnl, 0.0001);

		/// last depth received best bid 105
		pnlSnapshot.updateDepth(createDepth(105, 107, 500, 500));
		Assert.assertEquals(0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(200, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(600, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(600, pnlSnapshot.totalPnl, 0.0001);

		//sell 100@105
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Sell, 105, 100));
		Assert.assertEquals(101, pnlSnapshot.avgOpenPrice, 0.0001);
		Assert.assertEquals(200, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(400, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(600, pnlSnapshot.totalPnl, 0.0001);

		/// last depth received best bid 105
		pnlSnapshot.updateDepth(createDepth(102, 107, 500, 500));
		Assert.assertEquals(200, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(100, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(300, pnlSnapshot.totalPnl, 0.0001);

	}

	@Test public void testPnlOrderedUpdatesIncreasingOpenPositionInvert() {
		PnlSnapshotOrders pnlSnapshot = new PnlSnapshotOrders();
		//sell 100@101
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Sell, 101, 100));
		/// last depth received best bid 99
		pnlSnapshot.updateDepth(createDepth(99, 103, 500, 500));
		//sell 100@103
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Sell, 103, 100));
		Assert.assertEquals(102, pnlSnapshot.avgOpenPrice, 0.0001);

		Assert.assertEquals(0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(-200, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(-200, pnlSnapshot.unrealizedPnl, 0.0001);
		Assert.assertEquals(-200, pnlSnapshot.totalPnl, 0.0001);

		/// last depth received best bid 105
		pnlSnapshot.updateDepth(createDepth(105, 107, 500, 500));
		Assert.assertEquals(0, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(-200, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(-1000, pnlSnapshot.unrealizedPnl, 0.0001);
		Assert.assertEquals(-1000, pnlSnapshot.totalPnl, 0.0001);

		//buy 100@105 -> close 101 position   103 remains
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Buy, 105, 100));
		Assert.assertEquals(-400, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(103, pnlSnapshot.avgOpenPrice, 0.0001);
		Assert.assertEquals(-100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(-400, pnlSnapshot.unrealizedPnl, 0.0001);
		Assert.assertEquals(-800, pnlSnapshot.totalPnl, 0.0001);

		/// last depth received best bid 105
		pnlSnapshot.updateDepth(createDepth(102, 105, 500, 500));
		Assert.assertEquals(-400, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(103, pnlSnapshot.avgOpenPrice, 0.0001);
		Assert.assertEquals(-100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(-200, pnlSnapshot.unrealizedPnl, 0.0001);//we dont have anything open
		Assert.assertEquals(-600, pnlSnapshot.totalPnl, 0.0001);

	}

	@Test public void testPnlUpdatesChangeSide() {
		PnlSnapshot pnlSnapshot = new PnlSnapshot();
		//buy 100@101
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Buy, 101, 100));
		/// last depth received best bid 99
		pnlSnapshot.updateDepth(createDepth(99, 103, 500, 500));

		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Sell, 99, 200));

		Assert.assertEquals(-200, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(-100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(-200, pnlSnapshot.unrealizedPnl, 0.01);//we dont have anything open
		Assert.assertEquals(-400, pnlSnapshot.totalPnl, 0.01);

	}

	@Test public void testPnlOrdersUpdatesChangeSide() {
		PnlSnapshotOrders pnlSnapshot = new PnlSnapshotOrders();
		//buy 100@101
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Buy, 101, 100));
		/// last depth received best bid 99
		pnlSnapshot.updateDepth(createDepth(99, 103, 500, 500));

		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Sell, 99, 200));

		Assert.assertEquals(-200, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(-100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(-400, pnlSnapshot.unrealizedPnl, 0.01);//we dont have anything open
		Assert.assertEquals(-600, pnlSnapshot.totalPnl, 0.01);

	}

	@Test public void testPnlOrdersUpdatesChangeSideInvert() {
		PnlSnapshotOrders pnlSnapshot = new PnlSnapshotOrders();
		//buy 100@101
		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Sell, 101, 100));
		/// last depth received best bid 99
		pnlSnapshot.updateDepth(createDepth(99, 103, 500, 500));

		pnlSnapshot.updateExecutionReport(createExecutionReport(Verb.Buy, 99, 200));

		Assert.assertEquals(200, pnlSnapshot.realizedPnl, 0.0001);
		Assert.assertEquals(100, pnlSnapshot.netPosition, 0.0001);
		Assert.assertEquals(99, pnlSnapshot.avgOpenPrice, 0.0001);

		Assert.assertEquals(0, pnlSnapshot.unrealizedPnl, 0.01);
		Assert.assertEquals(200, pnlSnapshot.totalPnl, 0.01);

	}

}
