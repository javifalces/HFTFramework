package com.lambda.investing.xchange;

import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import lombok.Getter;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.binance.BinanceExchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter public class BinanceXchangeBrokerConnector extends XChangeBrokerConnector {

	private static Map<String, BinanceXchangeBrokerConnector> instances = new ConcurrentHashMap<>();

	public static BinanceXchangeBrokerConnector getInstance(String apiKey, String secretKey) {

		String key = apiKey + secretKey;
		BinanceXchangeBrokerConnector coinbaseBrokerConnector = new BinanceXchangeBrokerConnector(apiKey, secretKey);

		BinanceXchangeBrokerConnector output = instances.getOrDefault(key, coinbaseBrokerConnector);
		instances.put(key, output);

		return output;
	}

	private BinanceXchangeBrokerConnector(String apiKey, String secretKey) {
		this.apiKey = apiKey;
		this.secretKey = secretKey;
		streamingExchange = StreamingExchangeFactory.INSTANCE.createExchange(BinanceStreamingExchange.class.getName());
		exchange = ExchangeFactory.INSTANCE.createExchange(BinanceExchange.class.getName());
		marketDataService = exchange.getMarketDataService();
		setPrivateAccountInfo();

	}

	@Override protected void setPrivateAccountInfo() {
		exchangeSpecification = streamingExchange.getDefaultExchangeSpecification();

		exchangeSpecification.setExchangeSpecificParametersItem("Binance_Orderbook_Use_Higher_Frequency",
				true);//USE_HIGHER_UPDATE_FREQUENCY
		exchangeSpecification
				.setExchangeSpecificParametersItem("Binance_Ticker_Use_Realtime", true);// USE_REALTIME_BOOK_TICKER

		exchangeSpecification.setUserName(userName);
		exchangeSpecification.setApiKey(apiKey);
		exchangeSpecification.setSecretKey(secretKey);

		///recreate with api accounts
		streamingExchange = (BinanceStreamingExchange) StreamingExchangeFactory.INSTANCE
				.createExchange(exchangeSpecification);
		exchange = ExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
		marketDataService = exchange.getMarketDataService();

	}

	public void resetClient() {
		webSocketClient.disconnect().subscribe(() -> logger.info("Disconnected from the Exchange"));
		streamingExchange.disconnect();

		connectWebsocket(lastInstrumentListSubscribed);
		streamingExchange = StreamingExchangeFactory.INSTANCE.createExchange(BinanceStreamingExchange.class);
		exchange = ExchangeFactory.INSTANCE.createExchange(BinanceExchange.class);
		marketDataService = exchange.getMarketDataService();

	}
}
