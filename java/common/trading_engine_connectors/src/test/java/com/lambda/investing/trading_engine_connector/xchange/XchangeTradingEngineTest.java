package com.lambda.investing.trading_engine_connector.xchange;

import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqPublisher;
import com.lambda.investing.market_data_connector.MarketDataConfiguration;
import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.market_data_connector.ZeroMqMarketDataConnector;
import com.lambda.investing.market_data_connector.xchange.BinanceXchangeMarketDataConfiguration;
import com.lambda.investing.market_data_connector.xchange.XChangeMarketDataPublisher;
import com.lambda.investing.model.asset.Currency;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;
import io.vavr.Tuple;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.lambda.investing.Configuration.RANDOM_GENERATOR;

@Ignore public class XchangeTradingEngineTest extends TestCase {

	String algorithmInfo = "junitTest";
	ZeroMqConfiguration mqConfigurationPublisher = new ZeroMqConfiguration("localhost", 55, null);
	ZeroMqConfiguration mqConfigurationSubscriber = new ZeroMqConfiguration("localhost", 56,
			null);//listening OrderRequest
	int threadsPublish = 1;

	int threadsListen = 1;

	protected static Depth lastDepth = null;
	protected static Trade lastTrade = null;
	protected static ExecutionReport lastER = null;

	public void setUp() throws Exception {
		//		coinbase.apikey=0fc81f2daaf56301808b8fe01b68f8c2
		//		coinbase.secretkey=KXYMAQOtblVyAP4WSFj1WdSpfcwf7eEo7AV61G1ChAMz5CMgJ2n7qs1p+498DTJ9Gn+VrnrC09zNu8y/GT/jtQ==
		//				kraken.apikey=yuagc5Bvbjx9thMIjcLZz2qtGR8w1k0t0VwmdYgnKCTAFZTkILv8mSbb
		//		kraken.secretkey=Psh4NO5H2u6ELzcB9r4qijfBIeT718M44I9wT3fBIO8nGd7Px/AY9syEJoA5LD1e5XQh7OYsZ6S+ulCI+WHtzA==
		//				binance.apikey=lBXdwzS9iIdYgGpnGSXD1HqpsqQyJxnysMDMtwQVgovTVcAdAyEdxq0GmRlaNonT
		//		binance.secretkey=VOW8frSSLBcEmXeGWJIf3lX4lejIcfwD8WnHMZYdxSTqfaZSbMhcG9KnifHVCcDk
		super.setUp();
	}

	public void tearDown() throws Exception {
		lastDepth = null;
		lastTrade = null;
		lastER = null;
	}

	public List<Instrument> getInstrument(String market) {
		Instrument instrument = new Instrument();
		instrument.setMarket(market);
		instrument.setCurrency(Currency.USDT);
		instrument.setSymbol("btcusdt");
		instrument.setPriceTick(0.01);
		instrument.setPriceStep(0.01);
		instrument.setQuantityTick(0.00001);
		List<Instrument> output = new ArrayList<>();
		output.add(instrument);
		return output;
	}

	public Depth updateMD(String market) {
		//publisher
		List<Instrument> instruments = getInstrument(market);
		Instrument instrument = instruments.get(0);

		//subscriber

		lastDepth = null;
		lastTrade = null;
		ZeroMqMarketDataConnector marketDataProvider = new ZeroMqMarketDataConnector(mqConfigurationPublisher,
				threadsListen);

		marketDataProvider.setListenER(true);

		marketDataProvider.register(new MarketDataListener() {

			@Override public boolean onDepthUpdate(Depth depth) {
				//				System.out.println("depth received " + depth.getInstrument());
				lastDepth = depth;
				return true;
			}

			@Override public boolean onTradeUpdate(Trade trade) {
				//				System.out.println("trade received " + trade.getInstrument());
				lastTrade = trade;
				return true;
			}

			@Override public boolean onCommandUpdate(Command command) {
				System.out.println("command received " + command.getMessage());
				return true;
			}

			@Override public boolean onInfoUpdate(String header, String message) {
				System.out.println("info received " + header);
				return true;
			}
		});
		marketDataProvider.start();
		///
		System.out.println("waiting md on " + instrument);
		while (lastDepth == null) {
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("received  md on " + instrument);
		return lastDepth;
	}

	protected String generateClientOrderId() {
		byte[] dataInput = new byte[10];
		RANDOM_GENERATOR.nextBytes(dataInput);
		return UUID.nameUUIDFromBytes(dataInput).toString();
	}

	public OrderRequest createCancel(Instrument instrument, String origClientOrderId) {
		OrderRequest cancelOrderRequest = new OrderRequest();
		cancelOrderRequest.setOrderRequestAction(OrderRequestAction.Cancel);
		cancelOrderRequest.setOrigClientOrderId(origClientOrderId);
		cancelOrderRequest.setAlgorithmInfo(algorithmInfo);
		cancelOrderRequest.setClientOrderId(generateClientOrderId());
		cancelOrderRequest.setInstrument(instrument.getPrimaryKey());
		cancelOrderRequest.setTimestampCreation(System.currentTimeMillis());
		return cancelOrderRequest;
	}

	protected OrderRequest createLimitOrderRequest(Instrument instrument, Verb verb, double price, double quantity) {
		String newClientOrderId = generateClientOrderId();
		OrderRequest output = new OrderRequest();
		output.setAlgorithmInfo(algorithmInfo);
		output.setInstrument(instrument.getPrimaryKey());
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

	protected void launchOrders(Verb verb, Instrument instrument, TradingEngineConnector xChangeTradingEngine) {
		double qtySend = instrument.getQuantityStep() * 100;
		int firstStepsFar = 40;

		int incrementMultiplier = 1;
		if (verb.equals(Verb.Buy)) {
			incrementMultiplier = -1;
		}

		double price = instrument.getPriceIncrement(lastDepth.getMidPrice(), incrementMultiplier * firstStepsFar);
		OrderRequest orderRequest = createLimitOrderRequest(instrument, Verb.Buy, price, qtySend);
		System.out.println("sending OrderRequest " + orderRequest);
		lastER = null;
		xChangeTradingEngine.orderRequest(orderRequest);
		while (lastER == null) {
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Assert.assertNotNull(lastER);
		Assert.assertEquals(lastER.getPrice(), orderRequest.getPrice(), 0.001);
		Assert.assertEquals(lastER.getQuantity(), orderRequest.getQuantity(), 0.001);
		Assert.assertEquals(lastER.getExecutionReportStatus(), ExecutionReportStatus.Active);

		//replace it
		OrderRequest replaceOrder = (OrderRequest) orderRequest.clone();
		replaceOrder.setOrderRequestAction(OrderRequestAction.Modify);
		replaceOrder.setOrigClientOrderId(lastER.getClientOrderId());
		String newClientOrderId = generateClientOrderId();
		replaceOrder.setClientOrderId(newClientOrderId);

		double newPrice = instrument
				.getPriceIncrement(lastDepth.getMidPrice(), incrementMultiplier * (firstStepsFar - 5));
		replaceOrder.setPrice(newPrice);
		lastER = null;
		xChangeTradingEngine.orderRequest(replaceOrder);
		while (lastER == null) {
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Assert.assertEquals(lastER.getPrice(), replaceOrder.getPrice(), 0.001);
		Assert.assertEquals(lastER.getQuantity(), replaceOrder.getQuantity(), 0.001);
		Assert.assertEquals(lastER.getExecutionReportStatus(), ExecutionReportStatus.Active);

		//cancel
		OrderRequest cancelOrder = createCancel(instrument, newClientOrderId);
		cancelOrder.setOrderRequestAction(OrderRequestAction.Cancel);
		cancelOrder.setOrigClientOrderId(lastER.getClientOrderId());
		lastER = null;
		xChangeTradingEngine.orderRequest(cancelOrder);
		while (lastER == null) {
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Assert.assertEquals(lastER.getExecutionReportStatus(), ExecutionReportStatus.Cancelled);

	}

	@Test public void testBinance() {
		String market = "binance";
		String binanceApikey = "lBXdwzS9iIdYgGpnGSXD1HqpsqQyJxnysMDMtwQVgovTVcAdAyEdxq0GmRlaNonT";
		String binanceSecretKey = "VOW8frSSLBcEmXeGWJIf3lX4lejIcfwD8WnHMZYdxSTqfaZSbMhcG9KnifHVCcDk";

		MarketDataConfiguration marketDataConfiguration = new BinanceXchangeMarketDataConfiguration(binanceApikey,
				binanceSecretKey);

		ZeroMqPublisher mdZeroMqPublisher = new ZeroMqPublisher("testPublisher", threadsPublish);
		XChangeMarketDataPublisher marketDataPublisher = new XChangeMarketDataPublisher(market + "Test",
				mqConfigurationPublisher, mdZeroMqPublisher, marketDataConfiguration, getInstrument(market));
		marketDataPublisher.start();

		Depth depth = updateMD(market);

		List<Instrument> instruments = getInstrument(market);
		Instrument instrument = instruments.get(0);

		BinanceXchangeTradingEngineConfiguration tradingEngineConfiguration = new BinanceXchangeTradingEngineConfiguration(
				binanceApikey, binanceSecretKey);

		ZeroMqProvider zeroMqMdProvider = ZeroMqProvider.getInstance(mqConfigurationPublisher, threadsListen);

		ZeroMqPublisher zeroMqPublisher = mdZeroMqPublisher;//same as md for ER

		XChangeTradingEngine xChangeTradingEngine = new XChangeTradingEngine(mqConfigurationSubscriber,
				zeroMqMdProvider, mqConfigurationPublisher, zeroMqPublisher, tradingEngineConfiguration, instruments);
		xChangeTradingEngine.start();

		xChangeTradingEngine.register(algorithmInfo, new ExecutionReportListener() {

			@Override public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
				System.out.println("ExecutionReport received  on tradingEngine" + executionReport.getInstrument());
				lastER = executionReport;
				return true;
			}

			@Override public boolean onInfoUpdate(String header, String message) {
				System.out.println("info received  on tradingEngine" + header);
				return true;
			}
		});
		launchOrders(Verb.Buy, instrument, xChangeTradingEngine);

	}

}
