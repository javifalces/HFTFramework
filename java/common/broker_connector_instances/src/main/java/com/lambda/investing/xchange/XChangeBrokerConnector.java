package com.lambda.investing.xchange;

import com.binance.api.client.BinanceApiClientFactory;
import com.lambda.investing.Configuration;
import com.lambda.investing.model.asset.Instrument;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.bitmex.BitmexExchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.service.marketdata.MarketDataService;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Getter @Setter public abstract class XChangeBrokerConnector {

	protected Logger logger = LogManager.getLogger(XChangeBrokerConnector.class);
	protected StreamingExchange streamingExchange;
	protected Exchange exchange;
	protected ExchangeSpecification exchangeSpecification;

	protected MarketDataService marketDataService;

	protected static NumberFormat NUMBER_FORMAT = NumberFormat
			.getInstance(Locale.US);//US has dot instead of commas in decimals

	protected static Map<String, XChangeBrokerConnector> instances = new ConcurrentHashMap<>();

	protected String userName, apiKey, secretKey;

	protected abstract void setPrivateAccountInfo();

	public abstract void resetClient();

	protected static Map<String, Currency> stringToCurrency = new HashMap<>();
	protected static Map<String, CurrencyPair> stringToCurrencyPair = new HashMap<>();
	protected StreamingExchange webSocketClient;
	protected Map<String, Instrument> symbolToInstrument = new ConcurrentHashMap<>();
	protected List<CurrencyPair> pairs = new ArrayList<>();
	Map<CurrencyPair, Instrument> currencyPairToInstrument = new HashMap<>();
	List<Instrument> lastInstrumentListSubscribed = null;

	public static Currency getCurrency(String currency) {
		if (stringToCurrency.containsKey(currency.toUpperCase())) {
			return stringToCurrency.get(currency.toUpperCase());
		} else {
			for (Currency currencyIteration : Currency.getAvailableCurrencies()) {
				stringToCurrency.put(currencyIteration.getCurrencyCode().toUpperCase(), currencyIteration);
			}
			Currency currencyOut = stringToCurrency.get(currency.toUpperCase());
			if (currencyOut == null) {
				System.err.println(Configuration.formatLog("Currency {} not found!!!", currency));
			}
			return currencyOut;
		}
	}

	/**
	 * @param instrumentPk BTCUSDT_coinbase
	 * @return
	 */
	public static CurrencyPair getCurrencyPair(String instrumentPk) {
		String instrumentSymbol = instrumentPk.toUpperCase();
		if (instrumentPk.contains("_")) {
			instrumentSymbol = instrumentPk.split("_")[0].toUpperCase();
		}
		String currency1Str = instrumentSymbol.substring(0, 3);
		String currency2Str = instrumentSymbol.substring(3, instrumentSymbol.length());

		Currency currency1 = getCurrency(currency1Str);
		Currency currency2 = getCurrency(currency2Str);

		CurrencyPair pair = new CurrencyPair(currency1, currency2);
		return pair;
	}

	public synchronized void connectWebsocket(List<Instrument> instrumentList) {
		if (lastInstrumentListSubscribed != null) {
			if (lastInstrumentListSubscribed.containsAll(instrumentList)) {
				if (this.getStreamingExchange().isAlive()) {
					return;
				}
			} else {
				instrumentList.addAll(lastInstrumentListSubscribed);
			}
		}
		instrumentList = instrumentList.stream().distinct().collect(Collectors.toList());

		StringBuilder symbolsList = new StringBuilder();
		ProductSubscription.ProductSubscriptionBuilder productSubscriptionBuilder = ProductSubscription.create();

		for (Instrument instrument : instrumentList) {
			symbolsList.append(instrument.getPrimaryKey().toLowerCase());
			symbolToInstrument.put(instrument.getSymbol().toLowerCase(), instrument);
			symbolsList.append(',');

			CurrencyPair currencyPair = XChangeBrokerConnector.getCurrencyPair(instrument.getPrimaryKey());
			pairs.add(currencyPair);
			currencyPairToInstrument.put(currencyPair, instrument);

			productSubscriptionBuilder.addAll(currencyPair);
			//
			//			productSubscriptionBuilder.addUserTrades(currencyPair);
			//			productSubscriptionBuilder.addTrades(currencyPair);
			//			productSubscriptionBuilder.addTicker(currencyPair);
			//			productSubscriptionBuilder.addOrderbook(currencyPair);

		}

		logger.info("subscribing to websocket on symbols {}", symbolsList.toString());

		webSocketClient = this.getStreamingExchange();
		if (!webSocketClient.isAlive()) {
			logger.info("connecting websocket ");
			webSocketClient.connect(productSubscriptionBuilder.build()).blockingAwait();
		} else {
			logger.info("disconnecting previous websocket ....");
			webSocketClient.disconnect();

			while (webSocketClient.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			logger.info("connecting websocket ...");
			webSocketClient.connect(productSubscriptionBuilder.build()).blockingAwait();
		}
		lastInstrumentListSubscribed = instrumentList;
	}

}
