package com.lambda.investing.xchange;

import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import org.knowm.xchange.ExchangeFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static info.bitrich.xchangestream.binance.BinanceStreamingExchange.USE_REALTIME_BOOK_TICKER;
import static info.bitrich.xchangestream.binance.BinanceStreamingExchange.USE_HIGHER_UPDATE_FREQUENCY;

@Getter public class BinanceXchangeBrokerConnector extends XChangeBrokerConnector {

	private static Map<String, BinanceXchangeBrokerConnector> instances = new ConcurrentHashMap<>();

	public static BinanceXchangeBrokerConnector getInstance(String apiKey, String secretKey) {
		String key = apiKey + secretKey;
		return instances.computeIfAbsent(key, k -> new BinanceXchangeBrokerConnector(apiKey, secretKey));
	}

	private BinanceXchangeBrokerConnector(String apiKey, String secretKey) {
		this.apiKey = apiKey;
		this.secretKey = secretKey;
		// Don't pre-create exchanges with a default specification here: setPrivateAccountInfo()
		// below creates the real streaming/REST exchanges from the configured specification.
		// Each exchange creation triggers a remote exchangeInfo() call (BinanceExchange.remoteInit),
		// so creating them twice needlessly doubles requests to Binance and can trip its rate
		// limiting/IP ban (HTTP 418).
		setPrivateAccountInfo();
	}

	@Override protected void setPrivateAccountInfo() {
		// Build the default spec from a bare (unapplied) exchange instance: applySpecification()
		// on a real exchange triggers a remote exchangeInfo() call (BinanceExchange.remoteInit),
		// so we must avoid instantiating a fully-applied exchange just to read its default spec.
		exchangeSpecification = ExchangeFactory.INSTANCE
				.createExchangeWithoutSpecification(BinanceStreamingExchange.class)
				.getDefaultExchangeSpecification();

		exchangeSpecification.setExchangeSpecificParametersItem(USE_HIGHER_UPDATE_FREQUENCY,
				true);//USE_HIGHER_UPDATE_FREQUENCY
		exchangeSpecification
				.setExchangeSpecificParametersItem(USE_REALTIME_BOOK_TICKER, 100);// USE_REALTIME_BOOK_TICKER

		exchangeSpecification.setUserName(userName);
		exchangeSpecification.setApiKey(apiKey);
		exchangeSpecification.setSecretKey(secretKey);
		exchangeSpecification.setExchangeSpecificParametersItem("apiKeyType", "ed25519");
		exchangeSpecification.setExchangeSpecificParametersItem("ed25519", true);


		//REST exchange (orders, account, market data)
		streamingExchange = (BinanceStreamingExchange) StreamingExchangeFactory.INSTANCE
				.createExchange(exchangeSpecification);
		exchange = ExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
		marketDataService = exchange.getMarketDataService();


	}

	public void resetClient() {
		Set<Instrument> instrumentsToResubscribe = lastInstrumentSetSubscribed;
		webSocketClient.disconnect().blockingAwait();

		// Recreate the streaming/REST exchanges *before* reconnecting: BinanceStreamingExchange only
		// supports a single connection per instance, so reusing the just-disconnected instance to
		// connectWebsocket() below would keep operating on a stale/dead channel and never re-authenticate.
		streamingExchange = (BinanceStreamingExchange) StreamingExchangeFactory.INSTANCE
				.createExchange(exchangeSpecification);
		exchange = ExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
		marketDataService = exchange.getMarketDataService();

		// Force a full reconnect on the fresh exchange instance: isAlive() would otherwise short-circuit
		// since lastInstrumentSetSubscribed still matches the requested instruments.
		lastInstrumentSetSubscribed = null;
		connectWebsocket(instrumentsToResubscribe);
	}
}
