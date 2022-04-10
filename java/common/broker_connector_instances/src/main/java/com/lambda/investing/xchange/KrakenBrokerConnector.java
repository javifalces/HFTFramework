package com.lambda.investing.xchange;

import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange;
import lombok.Getter;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.kraken.KrakenExchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter public class KrakenBrokerConnector extends XChangeBrokerConnector {

	private static Map<String, KrakenBrokerConnector> instances = new ConcurrentHashMap<>();

	public static KrakenBrokerConnector getInstance(String apiKey, String secretKey) {

		String key = apiKey + secretKey;
		KrakenBrokerConnector krakenBrokerConnector = new KrakenBrokerConnector(apiKey, secretKey);

		KrakenBrokerConnector output = instances.getOrDefault(key, krakenBrokerConnector);
		instances.put(key, output);
		return output;
	}

	private KrakenBrokerConnector(String apiKey, String secretKey) {
		this.apiKey = apiKey;
		this.secretKey = secretKey;
		streamingExchange = StreamingExchangeFactory.INSTANCE.createExchange(KrakenStreamingExchange.class);
		exchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class);
		marketDataService = exchange.getMarketDataService();

		setPrivateAccountInfo();

	}

	@Override protected void setPrivateAccountInfo() {
		exchangeSpecification = streamingExchange.getDefaultExchangeSpecification();
		exchangeSpecification.setUserName(userName);
		exchangeSpecification.setApiKey(apiKey);
		exchangeSpecification.setSecretKey(secretKey);

		///recreate with api accounts
		streamingExchange = (KrakenStreamingExchange) StreamingExchangeFactory.INSTANCE
				.createExchange(exchangeSpecification);
		exchange = ExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
		marketDataService = exchange.getMarketDataService();

	}

	public void resetClient() {
		webSocketClient.disconnect().subscribe(() -> logger.info("Disconnected from the Exchange"));
		streamingExchange.disconnect();

		connectWebsocket(lastInstrumentListSubscribed);
		streamingExchange = StreamingExchangeFactory.INSTANCE.createExchange(KrakenStreamingExchange.class);
		exchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class);
		marketDataService = exchange.getMarketDataService();

	}
}
