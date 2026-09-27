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

	/**
	 * Each {@code KrakenBrokerConnector} owns its own {@code streamingExchange}/{@code exchange}
	 * (and therefore its own nonce factory). Kraken's REST API rejects a request as
	 * {@code NonceException: EAPI:Invalid nonce} if it sees a nonce lower than one already used for
	 * the same API key, so two independent connector instances sharing one API key must never both
	 * be "live" - the previous implementation always eagerly constructed a brand-new connector (with
	 * its own nonce factory) before consulting the cache, which is not only wasteful but, under
	 * concurrent callers (eg. market-data publisher and trading engine beans initializing around the
	 * same time), race-prone: two threads could each see an empty cache slot and each "win" with a
	 * different object, leaving one caller's requests permanently interleaving nonces against the
	 * other's. Compute-if-absent under a lock so exactly one instance is ever created per key.
	 */
	public static KrakenBrokerConnector getInstance(String apiKey, String secretKey) {
		String key = apiKey + secretKey;
		KrakenBrokerConnector output = instances.get(key);
		if (output != null) {
			return output;
		}
		synchronized (instances) {
			output = instances.get(key);
			if (output == null) {
				output = new KrakenBrokerConnector(apiKey, secretKey);
				instances.put(key, output);
			}
		}
		return output;
	}

	private KrakenBrokerConnector(String apiKey, String secretKey) {
		this.apiKey = apiKey;//tGdJZa9Z7Yjxxxxx+4X63
		this.secretKey = secretKey;//FL8iRtxhh9xxxx
		streamingExchange = StreamingExchangeFactory.INSTANCE.createExchange(KrakenStreamingExchange.class);
		exchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class);
		marketDataService = exchange.getMarketDataService();

		setPrivateAccountInfo();

	}

	/**
	 * Kraken's WS v1 streaming client fires every channel of the {@code ProductSubscription} passed
	 * to {@code connect()} as one burst of "subscribe" frames. With enough instruments this burst
	 * exceeds Kraken's message-rate limit and Kraken silently rejects a random subset of them
	 * ({@code {"event":"subscriptionStatus","status":"error","errorMessage":"Exceeded msg rate"}}),
	 * which looked like some instruments simply never receiving market data. Kraken fully supports
	 * subscribing per pair/channel dynamically after the connection is up, which is exactly what
	 * {@code XChangeMarketDataPublisher}/{@code XChangeTradingEngine} already do right after
	 * {@code connectWebsocket()} returns, so skip the redundant, burst-prone pre-subscription here.
	 */
	@Override
	protected boolean shouldPreSubscribeChannels() {
		return false;
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

		connectWebsocket(lastInstrumentSetSubscribed);
		streamingExchange = StreamingExchangeFactory.INSTANCE.createExchange(KrakenStreamingExchange.class);
		exchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class);
		marketDataService = exchange.getMarketDataService();

	}
}
