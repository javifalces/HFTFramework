package com.lambda.investing.trading_engine_connector.metatrader;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqPublisher;
import com.lambda.investing.market_data_connector.metatrader.MetatraderMarketDataPublisher;
import com.lambda.investing.metatrader.MetatraderZeroBrokerConnector;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.AbstractBrokerTradingEngine;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.lambda.investing.market_data_connector.AbstractMarketDataProvider.GSON;

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
	protected ExecutionReport lastExecutionReport;
	protected AtomicLong counterER = new AtomicLong(0);

	public MetatraderTradingEngineTest() {

		ConnectorConfiguration orderRequestConfiguration = new ZeroMqConfiguration();
		((ZeroMqConfiguration) orderRequestConfiguration).setHost(HOST);
		((ZeroMqConfiguration) orderRequestConfiguration).setPort(tradingEnginePort);

		ConnectorProvider orderRequestConnectorProvider = ZeroMqProvider
				.getInstance((ZeroMqConfiguration) orderRequestConfiguration, 1);
		((ZeroMqProvider) orderRequestConnectorProvider).start();

		ConnectorConfiguration marketDataConfiguration = new ZeroMqConfiguration();
		((ZeroMqConfiguration) marketDataConfiguration).setHost(HOST);
		((ZeroMqConfiguration) marketDataConfiguration).setPort(marketDataPort);

		ConnectorPublisher marketDataPublisher = new ZeroMqPublisher("metatraderMarketDataConnectorPublisher", 1);

		MetatraderZeroBrokerConnector metatraderZeroBrokerConnector = MetatraderZeroBrokerConnector
				.getInstance(MT_HOST, MT_PORT_PUBLISHER, MT_PORT_PUSH, MT_PORT_PULL);
		MetatraderMarketDataPublisher metatraderMarketDataPublisher = new MetatraderMarketDataPublisher(
				marketDataConfiguration, marketDataPublisher, metatraderZeroBrokerConnector);
		metatraderMarketDataPublisher.setBroker("darwinex");
		metatraderMarketDataPublisher.init();

		tradingEngine = new MetatraderTradingEngine(orderRequestConfiguration, orderRequestConnectorProvider,
				marketDataConfiguration, marketDataPublisher, metatraderZeroBrokerConnector);

		tradingEngine.start();
		Listener listener = new Listener();

		ConnectorProvider marketDataProvider = ZeroMqProvider
				.getInstance((ZeroMqConfiguration) marketDataConfiguration, 1);
		marketDataProvider.register(marketDataConfiguration, listener);
		((ZeroMqProvider) marketDataProvider).start();

	}

	@Before public void setUp() throws Exception {
		lastExecutionReport = null;
		counterER = new AtomicLong(0);
	}

	private class Listener implements ConnectorListener {

		public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
			lastExecutionReport = executionReport;
			return true;
		}

		@Override public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
				TypeMessage typeMessage, String content) {
			if (typeMessage.equals(TypeMessage.execution_report)) {
				ExecutionReport executionReport = GSON.fromJson(content, ExecutionReport.class);
				onExecutionReportUpdate(executionReport);
			}
		}
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

	@Ignore @Test public void testLimitOrder() {
		Verb verb = Verb.Buy;
		OrderRequest orderRequest = createLimitOrderRequest(verb, 0.01, 1.3825);
		tradingEngine.orderRequest(orderRequest);

		while (lastExecutionReport == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		Assert.assertNotNull(lastExecutionReport);
		Assert.assertEquals(lastExecutionReport.getVerb(), verb);
	}

	@Ignore @Test public void testMktOrder() {
		Verb verb = Verb.Buy;
		OrderRequest orderRequest = createMarketOrderRequest(verb, 0.01);
		tradingEngine.orderRequest(orderRequest);

		while (lastExecutionReport == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		Assert.assertNotNull(lastExecutionReport);
		Assert.assertEquals(lastExecutionReport.getVerb(), verb);
	}

	@Ignore @Test public void testNettingMktOrder() throws InterruptedException {
		((MetatraderTradingEngine) tradingEngine).setNettingByEngine(true);

		Verb verb = Verb.Buy;
		OrderRequest orderRequest = createMarketOrderRequest(verb, 0.01);
		String entryClorId = orderRequest.getClientOrderId();
		Thread.sleep(1500);
		tradingEngine.orderRequest(orderRequest);

		while (lastExecutionReport == null) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		//		Assert.assertNotNull(lastExecutionReport);
		//		Assert.assertEquals(lastExecutionReport.getVerb(), verb);

		//		Thread.sleep(350);

		lastExecutionReport = null;
		Verb verb2 = Verb.Sell;
		OrderRequest orderRequestClose = createMarketOrderRequest(verb2, 0.01);
		orderRequestClose.setOrigClientOrderId(entryClorId);
		((MetatraderTradingEngine) tradingEngine).closeTrade(orderRequestClose);
		while (lastExecutionReport == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		Assert.assertNotNull(lastExecutionReport);
		Assert.assertEquals(lastExecutionReport.getVerb(), verb2);

	}

}
