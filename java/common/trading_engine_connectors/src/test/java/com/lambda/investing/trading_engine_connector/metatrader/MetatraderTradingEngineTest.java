package com.lambda.investing.trading_engine_connector.metatrader;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorPublisherProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqPublisher;
import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.market_data_connector.metatrader.MetatraderMarketDataPublisher;
import com.lambda.investing.metatrader.MetatraderZeroBrokerConnector;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.AbstractBrokerTradingEngine;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.lambda.investing.market_data_connector.AbstractMarketDataProvider.GSON;
import static com.lambda.investing.model.portfolio.Portfolio.GSON_STRING;

@Ignore("need to open metatrader for testing")
public class MetatraderTradingEngineTest {

	protected static String ALGORITHM_INFO = "JUNIT_MetatraderTradingEngineTest";
	protected static String INSTRUMENT_PK = "gbpusd_darwinex";

	protected static String HOST = "localhost";
	protected static int marketDataPort = 666;
	protected static int tradingEnginePort = 677;

	protected static String MT_HOST = "localhost";
	protected static int MT_PORT_PUBLISHER = 32770;
	protected static int MT_PORT_PUSH = 32769;
	protected static int MT_PORT_PULL = 32768;

	protected AbstractBrokerTradingEngine tradingEngine;
	protected List<ExecutionReport> lastExecutionReport = new ArrayList<>();
	protected AtomicLong counterER = new AtomicLong(0);

	protected Listener listener = new Listener();

	private class Listener implements ConnectorListener {

		@Override
		public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
							 TypeMessage typeMessage, String content) {
			if (typeMessage.equals(TypeMessage.execution_report)) {
				ExecutionReport executionReport = GSON.fromJson(content, ExecutionReport.class);
				onExecutionReportUpdate(executionReport);
			}
		}

		public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
			lastExecutionReport.add(executionReport);
			counterER.incrementAndGet();
			return true;
		}

	}


	public MetatraderTradingEngineTest() throws InterruptedException {
		ConnectorConfiguration orderRequestConfiguration = new ZeroMqConfiguration();
		((ZeroMqConfiguration) orderRequestConfiguration).setHost(HOST);
		((ZeroMqConfiguration) orderRequestConfiguration).setPort(tradingEnginePort);

		ConnectorProvider orderRequestConnectorProvider = ZeroMqProvider
				.getInstance((ZeroMqConfiguration) orderRequestConfiguration, 0);
		((ZeroMqProvider) orderRequestConnectorProvider).start();

//		ZEROMQ publisher
//		ConnectorConfiguration marketDataConfiguration = new ZeroMqConfiguration();
//		((ZeroMqConfiguration) marketDataConfiguration).setHost(HOST);
//		((ZeroMqConfiguration) marketDataConfiguration).setPort(marketDataPort);
//		ConnectorPublisher marketDataPublisher = new ZeroMqPublisher("metatraderMarketDataConnectorPublisher", 0);

		OrdinaryConnectorConfiguration marketDataConfiguration = new OrdinaryConnectorConfiguration();
		ConnectorPublisher marketDataPublisher = new OrdinaryConnectorPublisherProvider("metatraderMarketDataConnectorPublisher", 0, Thread.NORM_PRIORITY);


		MetatraderZeroBrokerConnector metatraderZeroBrokerConnector = MetatraderZeroBrokerConnector
				.getInstance(MT_HOST, MT_PORT_PUBLISHER, MT_PORT_PUSH, MT_PORT_PULL);


		MetatraderMarketDataPublisher metatraderMarketDataPublisher = new MetatraderMarketDataPublisher(
				marketDataConfiguration, marketDataPublisher, metatraderZeroBrokerConnector);
		metatraderMarketDataPublisher.setBroker("darwinex");
		metatraderMarketDataPublisher.init();

		tradingEngine = new MetatraderTradingEngine(orderRequestConfiguration, orderRequestConnectorProvider,
				marketDataConfiguration, marketDataPublisher, metatraderZeroBrokerConnector);
		((MetatraderTradingEngine) tradingEngine).setBroker("darwinex");
		tradingEngine.start();

//		OrdinaryConnectorPublisherProvider marketDataProvider = new OrdinaryConnectorPublisherProvider(,0);
		((OrdinaryConnectorPublisherProvider) marketDataPublisher).register(marketDataConfiguration, listener);

//
//		ConnectorProvider marketDataProvider = ZeroMqProvider
//				.getInstance((ZeroMqConfiguration) marketDataConfiguration, 0);
//
//		marketDataProvider.register(marketDataConfiguration, listener);
//
//
//		((ZeroMqProvider) marketDataProvider).start();


	}

	@Before
	public void setUp() throws Exception {
		lastExecutionReport.clear();
		counterER = new AtomicLong(0);
		((MetatraderTradingEngine) tradingEngine).setNettingByEngine(true);
	}

	private double getPosition() {
		((MetatraderTradingEngine) tradingEngine).getPositionMT(true);
		double position = ((MetatraderTradingEngine) tradingEngine).getPosition(INSTRUMENT_PK);
		return position;
	}

	protected String generateClientOrderId() {
		return UUID.randomUUID().toString();
	}

	private OrderRequest createMarketOrderRequest(Verb verb, double quantity) {
		///controled market request
		String newClientOrderId = this.generateClientOrderId();
		OrderRequest output = new OrderRequest();
		output.setAlgorithmInfo(ALGORITHM_INFO);
		output.setInstrument(INSTRUMENT_PK);
		output.setVerb(verb);
		output.setOrderRequestAction(OrderRequestAction.Send);
		output.setClientOrderId(newClientOrderId);
		output.setQuantity(quantity);
		//		output.setPrice(price);
		output.setTimestampCreation(System.currentTimeMillis());
		output.setOrderType(OrderType.Market);//limit for quoting
		output.setMarketOrderType(MarketOrderType.FAS);//default FAS
		return output;
	}

	private OrderRequest createLimitOrderRequest(Verb verb, double quantity, double price) {
		///controled market request
		String newClientOrderId = this.generateClientOrderId();
		OrderRequest output = new OrderRequest();
		output.setAlgorithmInfo(ALGORITHM_INFO);
		output.setInstrument(INSTRUMENT_PK);
		output.setVerb(verb);
		output.setOrderRequestAction(OrderRequestAction.Send);
		output.setClientOrderId(newClientOrderId);
		output.setQuantity(quantity);
		output.setPrice(price);
		output.setTimestampCreation(System.currentTimeMillis());
		output.setOrderType(OrderType.Limit);//limit for quoting
		output.setMarketOrderType(MarketOrderType.FAS);//default FAS
		return output;
	}

	private void resetPositionToZero() {
		double position = getPosition();
		if (position != 0) {
			Verb verb = position > 0 ? Verb.Sell : Verb.Buy;

			OrderRequest orderRequest = createMarketOrderRequest(verb, position);
			tradingEngine.orderRequest(orderRequest);
			while (lastExecutionReport.size() < 2) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private List<ExecutionReport> changePosition(double positionDelta) {

		Verb verb = positionDelta > 0 ? Verb.Buy : Verb.Sell;
		OrderRequest orderRequest = createMarketOrderRequest(verb, Math.abs(positionDelta));
		lastExecutionReport.clear();
		tradingEngine.orderRequest(orderRequest);
		while (lastExecutionReport.size() < 2) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return new ArrayList<>(lastExecutionReport);
	}

	@Ignore
	@Test
	public void testLimitOrder() {
		Verb verb = Verb.Buy;
		OrderRequest orderRequest = createLimitOrderRequest(verb, 0.01, 1.3825);
		tradingEngine.orderRequest(orderRequest);

		while (lastExecutionReport.size() == 0) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
		Assert.assertNotNull(last);
		Assert.assertEquals(last.getVerb(), verb);
	}


	@Test
	public void testMktOrder() {
		Verb verb = Verb.Buy;
		double position = getPosition();
		OrderRequest orderRequest = createMarketOrderRequest(verb, 0.01);
		tradingEngine.orderRequest(orderRequest);
		while (lastExecutionReport.size() < 2) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
		Assert.assertNotNull(last);
		Assert.assertEquals(last.getVerb(), verb);
		Assert.assertEquals(ExecutionReportStatus.CompletellyFilled, last.getExecutionReportStatus());

		ExecutionReport first = lastExecutionReport.get(0);
		Assert.assertNotNull(first);
		Assert.assertEquals(first.getVerb(), verb);
		Assert.assertEquals(ExecutionReportStatus.Active, first.getExecutionReportStatus());

		Assert.assertEquals(position + orderRequest.getQuantity(), getPosition(), 1E-6);

	}

	@Test
	public void testNettingBuyMktOrder() {
		resetPositionToZero();
		double initialPosition = getPosition();
		double lots = 0.01;
		changePosition(lots);
		Assert.assertEquals(initialPosition + lots, getPosition(), 1E-3);

		List<ExecutionReport> lastExecutionReport = changePosition(-lots);
		ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
		Assert.assertNotNull(last);
		Assert.assertEquals(last.getVerb(), Verb.Sell);
		Assert.assertEquals(initialPosition, getPosition(), 1E-3);
	}

	@Test
	public void testNettingBuyTwiceMktOrder() {
		resetPositionToZero();
		double initialPosition = getPosition();
		double lots = 0.01;
		changePosition(lots);
		Assert.assertEquals(initialPosition + lots, getPosition(), 1E-3);
		changePosition(lots);
		Assert.assertEquals(initialPosition + 2 * lots, getPosition(), 1E-3);

		List<ExecutionReport> lastExecutionReport = changePosition(-lots);
		ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
		Assert.assertNotNull(last);
		Assert.assertEquals(last.getVerb(), Verb.Sell);
		Assert.assertEquals(initialPosition + lots, getPosition(), 1E-3);

		List<ExecutionReport> lastExecutionReport1 = changePosition(-lots);
		ExecutionReport last1 = lastExecutionReport1.get(lastExecutionReport.size() - 1);
		Assert.assertNotNull(last1);
		Assert.assertEquals(last1.getVerb(), Verb.Sell);
		Assert.assertEquals(initialPosition, getPosition(), 1E-3);
	}


	@Test
	public void testNettingBuyIncreaseSlowMktOrder() {
		resetPositionToZero();
		double initialPosition = getPosition();
		double lots = 0.01;
		int stepsIncrease = 5;
		for (int i = 0; i < stepsIncrease; i++) {
			changePosition(lots);
		}
		Assert.assertEquals(initialPosition + lots * stepsIncrease, getPosition(), 1E-3);
		changePosition(-(lots * stepsIncrease));
		Assert.assertEquals(initialPosition, getPosition(), 1E-3);
	}

	@Test
	public void testNettingBuyDecreaseSlowMktOrder() {
		resetPositionToZero();
		double initialPosition = getPosition();
		double lots = 0.01;
		int stepsIncrease = 5;
		changePosition((lots * stepsIncrease));
		Assert.assertEquals(initialPosition + lots * stepsIncrease, getPosition(), 1E-3);
		for (int i = 0; i < stepsIncrease; i++) {
			changePosition(-lots);
		}
		Assert.assertEquals(initialPosition, getPosition(), 1E-3);
	}

	@Test
	public void testNettingBuyDecreaseBiggerMktOrder() {
		resetPositionToZero();
		double initialPosition = getPosition();
		double lots = 0.01;
		int stepsIncrease = 5;
		changePosition((lots));
		//increase 0.01
		Assert.assertEquals(initialPosition + lots, getPosition(), 1E-3);

		//reduce 0.05
		List<ExecutionReport> lastExecutionReport = changePosition((-stepsIncrease * lots));

		//position must be -0.04
		//close the previus one and leave one stepIncreaseLess
		Assert.assertEquals(initialPosition - ((stepsIncrease - 1) * lots), getPosition(), 1E-3);
		ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
		Assert.assertNotNull(last);
		Assert.assertEquals(last.getVerb(), Verb.Sell);

//		Assert.assertEquals(stepsIncrease * lots, last.getLastQuantity(), 1E-3);
//		Assert.assertEquals(ExecutionReportStatus.CompletellyFilled, last.getExecutionReportStatus());

		resetPositionToZero();
	}

	@Test
	public void testNettingBuyAndReducingWitDifferentTranches() {
		resetPositionToZero();
		double initialPosition = getPosition();

		changePosition((0.02));
		changePosition((0.02));
		changePosition((0.01));
		Assert.assertEquals(initialPosition + 0.05, getPosition(), 1E-3);

		List<ExecutionReport> lastExecutionReport = changePosition(-0.03);
		//close the previous one and leave one stepIncreaseLess
		Assert.assertEquals(initialPosition + 0.02, getPosition(), 1E-3);
		ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
		Assert.assertNotNull(last);
		Assert.assertEquals(last.getVerb(), Verb.Sell);
//		Assert.assertEquals(0.03, last.getLastQuantity(), 1E-3);
//		Assert.assertEquals(ExecutionReportStatus.CompletellyFilled, last.getExecutionReportStatus());

		resetPositionToZero();
	}
}
