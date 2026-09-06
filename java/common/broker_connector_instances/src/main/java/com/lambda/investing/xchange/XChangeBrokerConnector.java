package com.lambda.investing.xchange;

import com.lambda.investing.Configuration;
import com.lambda.investing.model.asset.Instrument;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.service.marketdata.MarketDataService;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Getter
@Setter
public abstract class XChangeBrokerConnector {

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
    Set<Instrument> lastInstrumentSetSubscribed = null;

    public static Currency getCurrency(String currency) {
        return getCurrency(currency, true);
    }

    public static Currency getSearchCurrency(boolean startFirst, String symbol) {
        if (startFirst) {
            for (Currency currency : Currency.getAvailableCurrencies()) {
                if (symbol.startsWith(currency.getCurrencyCode())) {
                    stringToCurrency.put(symbol, currency);
                    return currency;
                }
            }
        } else {
            for (Currency currency : Currency.getAvailableCurrencies()) {
                if (symbol.endsWith(currency.getCurrencyCode())) {
                    stringToCurrency.put(symbol, currency);
                    return currency;
                }
            }
        }
        return null;
    }

    public static Currency getCurrency(String currency, boolean printError) {
        if (stringToCurrency.containsKey(currency.toUpperCase())) {
            return stringToCurrency.get(currency.toUpperCase());
        } else {
            for (Currency currencyIteration : Currency.getAvailableCurrencies()) {
                stringToCurrency.put(currencyIteration.getCurrencyCode().toUpperCase(), currencyIteration);
            }
            Currency currencyOut = stringToCurrency.get(currency.toUpperCase());
            if (currencyOut == null && printError) {
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
        Currency currencyBaseObj = getSearchCurrency(true, instrumentSymbol);
        Currency currencyQuoteObj = getSearchCurrency(false, instrumentSymbol);
        if (currencyBaseObj != null && currencyQuoteObj != null) {
            CurrencyPair pair = new CurrencyPair(currencyBaseObj, currencyQuoteObj);
            return pair;
        }
        System.err.println(Configuration.formatLog("Currency pair {} not found!!!", instrumentPk));
        return null;

    }

    /**
     * Returns only the last {@code visibleChars} characters of a secret, for safe logging
     * (never log full API secrets/private keys).
     */
    protected static String lastChars(String secret, int visibleChars) {
        if (secret == null) {
            return "null";
        }
        if (secret.length() <= visibleChars) {
            return secret;
        }
        return secret.substring(secret.length() - visibleChars);
    }


    /**
     * Connects the websocket subscribing to public (market data) and, when needed, authenticated
     * (user data: orders/userTrades/balances) channels.
     * <p>
     * Authenticated channels require Binance to open a user-data-stream (listenKey), which is only
     * needed by trading engines that listen to order/trade updates. Market-data-only publishers
     * must not request them to avoid unnecessary private API calls (and failures on accounts/keys
     * that cannot open a user-data-stream).
     */
    public synchronized void connectWebsocket(Set<Instrument> instrumentSet) {
        logger.info("connecting {} websocket apiKey={} secretKey=***{}", getClass().getSimpleName(),
                apiKey, lastChars(secretKey, 4));

        if (lastInstrumentSetSubscribed != null) {
            if (lastInstrumentSetSubscribed.containsAll(instrumentSet)) {
                if (this.getStreamingExchange().isAlive()) {
                    return;
                }
            } else {
                instrumentSet.addAll(lastInstrumentSetSubscribed);
            }
        }
        instrumentSet = instrumentSet.stream().distinct().collect(Collectors.toSet());

        StringBuilder symbolsList = new StringBuilder();
        ProductSubscription.ProductSubscriptionBuilder productSubscriptionBuilder = ProductSubscription.create();

        for (Instrument instrument : instrumentSet) {
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
        if (webSocketClient == null) {

            logger.error("webSocketClient is null");
            return;
        }
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
        lastInstrumentSetSubscribed = instrumentSet;
    }

}
