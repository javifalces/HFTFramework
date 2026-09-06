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
     * Safe wrapper around {@link StreamingExchange#isAlive()}.
     * <p>
     * Some implementations (e.g. {@code BinanceStreamingExchange}) throw a {@link NullPointerException}
     * when {@code isAlive()} is invoked before the underlying streaming service has ever been created,
     * i.e. before the first successful {@code connect()}. In that situation the exchange is simply not
     * connected, so treat the NPE as "not alive" instead of propagating it.
     */
    protected static boolean isAlive(StreamingExchange streamingExchange) {
        try {
            return streamingExchange.isAlive();
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Whether the connected exchange (including its authenticated user-data channels, when an
     * API key is set) is reporting itself as alive. Callers that need authenticated channels
     * (e.g. user trades/order changes) should check this before subscribing: a successful
     * {@link #connectWebsocket} does not guarantee the authenticated login has completed, since
     * Binance's user-data-stream login happens asynchronously right after the socket connects.
     */
    public boolean isConnectionAlive() {
        return isAlive(this.getStreamingExchange());
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
                if (isAlive(this.getStreamingExchange())) {
                    return;
                }
            } else {
                instrumentSet.addAll(lastInstrumentSetSubscribed);
            }
        }


        StringBuilder symbolsList = new StringBuilder();
        ProductSubscription.ProductSubscriptionBuilder productSubscriptionBuilder = ProductSubscription.create();

        for (Instrument instrument : instrumentSet) {
            symbolsList.append(instrument.getPrimaryKey().toLowerCase());
            symbolToInstrument.put(instrument.getSymbol().toLowerCase(), instrument);
            symbolsList.append(',');

            CurrencyPair currencyPair = XChangeBrokerConnector.getCurrencyPair(instrument.getPrimaryKey());
            // connectWebsocket() can be invoked more than once on the same shared/singleton connector
            // (e.g. once from the market data publisher and once from the trading engine, or again when
            // isAlive() falsely reports "not alive" - see isAlive() javadoc). Guard against re-adding the
            // same pair, otherwise every subsequent call duplicates the subscriptions (and, downstream,
            // every onUserTrades/onOrderChange/getTrades/getOrderBook subscription attempt) for each pair.
            if (!pairs.contains(currencyPair)) {
                pairs.add(currencyPair);
            }
            currencyPairToInstrument.put(currencyPair, instrument);

            // Only subscribe to the channels actually consumed (market data + user order/trade updates).
            // NOTE: avoid ProductSubscriptionBuilder#addAll(): it also subscribes to funding rates and
            // balances, which are unused here and, for funding rates on Binance spot pairs, NPE because
            // the spot exchange has no BinanceFuturesAuthenticated REST client configured.
            productSubscriptionBuilder.addOrderbook(currencyPair);
            productSubscriptionBuilder.addTrades(currencyPair);
            productSubscriptionBuilder.addUserTrades(currencyPair);
            productSubscriptionBuilder.addOrders(currencyPair);
        }

        logger.info("subscribing to websocket on symbols {}", symbolsList.toString());

        webSocketClient = this.getStreamingExchange();
        if (webSocketClient == null) {

            logger.error("webSocketClient is null");
            return;
        }
        if (!isAlive(webSocketClient)) {
            logger.info("connecting websocket ");
            connect(webSocketClient, productSubscriptionBuilder);
        } else {
            logger.info("disconnecting previous websocket ....");
            webSocketClient.disconnect();

            while (isAlive(webSocketClient)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            logger.info("connecting websocket ...");
            connect(webSocketClient, productSubscriptionBuilder);
        }
        lastInstrumentSetSubscribed = instrumentSet;
    }

    /**
     * Connects a (possibly already connected) {@link StreamingExchange}, tolerating implementations
     * whose {@link StreamingExchange#isAlive()} unreliably reports {@code false} for an already
     * connected exchange.
     * <p>
     * {@code BinanceStreamingExchange.isAlive()} requires the authenticated user-data services to be
     * non-null and authorized once an API key is set; those services are only created when the
     * {@code ed25519} exchange-specific parameter is enabled, which this connector does not set. As a
     * result {@code isAlive()} can NPE (see {@link #isAlive}) right after a successful connect, making
     * this method believe a (re)connect is needed. Since the underlying exchange only supports a single
     * connection, retrying then throws {@link UnsupportedOperationException}, which is safe to ignore:
     * it means the exchange is already connected (e.g. shared singleton with the market data publisher).
     */
    private void connect(StreamingExchange streamingExchange,
                         ProductSubscription.ProductSubscriptionBuilder productSubscriptionBuilder) {
        try {
            streamingExchange.connect(productSubscriptionBuilder.build()).blockingAwait();
        } catch (UnsupportedOperationException e) {
            logger.warn("{} is already connected (isAlive() falsely reported not-alive): {}",
                    streamingExchange.getClass().getSimpleName(), e.getMessage());
        }
    }

}
