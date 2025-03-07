package com.lambda.investing.xchange;

import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.bybit.BybitStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange;
import lombok.Getter;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.bybit.BybitExchange;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.bybit.dto.BybitCategory;
import org.knowm.xchange.bybit.dto.account.walletbalance.BybitAccountType;

import static org.knowm.xchange.Exchange.USE_SANDBOX;
import static org.knowm.xchange.bybit.BybitExchange.SPECIFIC_PARAM_ACCOUNT_TYPE;

@Getter
public class BybitBrokerConnector extends XChangeBrokerConnector {
    private static final boolean DEFAULT_USE_SANDBOX = false;
    private static final boolean DEFAULT_REMOTE_METADATA = false;

    private static Map<String, BybitBrokerConnector> instances = new ConcurrentHashMap<>();

    public static BybitBrokerConnector getInstance(String apiKey, String secretKey) {

        String key = apiKey + secretKey;
        BybitBrokerConnector output = instances.getOrDefault(key, new BybitBrokerConnector(apiKey, secretKey));
        instances.put(key, output);


        return output;
    }

    private BybitBrokerConnector(String apiKey, String secretKey) {
        this.apiKey = apiKey;//+4X63
        this.secretKey = secretKey;//

        setPrivateAccountInfo();

    }

    @Override
    protected void setPrivateAccountInfo() {
        ExchangeSpecification exchangeSpecification =
                new BybitExchange().getDefaultExchangeSpecification();
        exchangeSpecification.setApiKey(System.getProperty(this.apiKey));
        exchangeSpecification.setSecretKey(System.getProperty(this.secretKey));
        exchangeSpecification.setShouldLoadRemoteMetaData(DEFAULT_REMOTE_METADATA);
//        exchangeSpecification.setExchangeSpecificParametersItem(SPECIFIC_PARAM_ACCOUNT_TYPE,
//                BybitAccountType.UNIFIED);
//        exchangeSpecification.setExchangeSpecificParametersItem(
//                SPECIFIC_PARAM_ACCOUNT_TYPE, BybitAccountType.UNIFIED);
        exchangeSpecification.setExchangeSpecificParametersItem(USE_SANDBOX, DEFAULT_USE_SANDBOX);
        exchange = ExchangeFactory.INSTANCE.createExchange(
                exchangeSpecification);

        marketDataService = exchange.getMarketDataService();

        ExchangeSpecification streamingExchangeSpecification =
                new BybitStreamingExchange().getDefaultExchangeSpecification();
        streamingExchangeSpecification.setApiKey(System.getProperty(this.apiKey));
        streamingExchangeSpecification.setSecretKey(System.getProperty(this.secretKey));
        streamingExchangeSpecification.setShouldLoadRemoteMetaData(DEFAULT_REMOTE_METADATA);
        streamingExchangeSpecification.setExchangeSpecificParametersItem(USE_SANDBOX, DEFAULT_USE_SANDBOX);
        streamingExchangeSpecification.setExchangeSpecificParametersItem(
                BybitStreamingExchange.EXCHANGE_TYPE, BybitCategory.LINEAR);
        streamingExchange = StreamingExchangeFactory.INSTANCE.createExchange(streamingExchangeSpecification);


    }

    public void resetClient() {
        webSocketClient.disconnect().subscribe(() -> logger.info("Disconnected from the Exchange"));
        streamingExchange.disconnect();

        connectWebsocket(lastInstrumentListSubscribed);
//		streamingExchange = StreamingExchangeFactory.INSTANCE.createExchange(KrakenStreamingExchange.class);
        exchange = ExchangeFactory.INSTANCE.createExchange(BybitExchange.class);
        marketDataService = exchange.getMarketDataService();

    }
}
