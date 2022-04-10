package com.lambda.investing.xchange;

import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import lombok.Getter;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.coinbasepro.CoinbaseProExchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter public class CoinbaseBrokerConnector extends XChangeBrokerConnector {

	private static Map<String, CoinbaseBrokerConnector> instances = new ConcurrentHashMap<>();

	public static CoinbaseBrokerConnector getInstance(String apiKey, String secretKey) {

		String key = apiKey + secretKey;
		CoinbaseBrokerConnector coinbaseBrokerConnector = new CoinbaseBrokerConnector(apiKey, secretKey);

		CoinbaseBrokerConnector output = instances.getOrDefault(key, coinbaseBrokerConnector);
		instances.put(key, output);

		return output;
	}

	private CoinbaseBrokerConnector(String apiKey, String secretKey) {
		this.apiKey = apiKey;
		this.secretKey = secretKey;
		streamingExchange = StreamingExchangeFactory.INSTANCE
				.createExchange(CoinbaseProStreamingExchange.class.getName());
		exchange = ExchangeFactory.INSTANCE.createExchange(CoinbaseProExchange.class.getName());
		marketDataService = exchange.getMarketDataService();

		setPrivateAccountInfo();

	}

	@Override protected void setPrivateAccountInfo() {
		exchangeSpecification = streamingExchange.getDefaultExchangeSpecification();
		exchangeSpecification.setUserName(userName);
		exchangeSpecification.setApiKey(apiKey);
		exchangeSpecification.setSecretKey(secretKey);

		///recreate with api accounts
		streamingExchange = (CoinbaseProStreamingExchange) StreamingExchangeFactory.INSTANCE
				.createExchange(exchangeSpecification);
		exchange = ExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
		marketDataService = exchange.getMarketDataService();

	}

	public void resetClient() {
		webSocketClient.disconnect().subscribe(() -> logger.info("Disconnected from the Exchange"));
		streamingExchange.disconnect();
		//connect again
		connectWebsocket(lastInstrumentListSubscribed);
		streamingExchange = StreamingExchangeFactory.INSTANCE.createExchange(CoinbaseProStreamingExchange.class);
		exchange = ExchangeFactory.INSTANCE.createExchange(CoinbaseProExchange.class);
		marketDataService = exchange.getMarketDataService();

	}
}
