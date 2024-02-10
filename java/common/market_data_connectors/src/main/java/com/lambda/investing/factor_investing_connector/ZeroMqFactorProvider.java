package com.lambda.investing.factor_investing_connector;

import com.google.gson.reflect.TypeToken;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.util.Map;

public class ZeroMqFactorProvider extends AbstractFactorProvider implements ConnectorListener {
    Logger logger = LogManager.getLogger(ZeroMqFactorProvider.class);

    private ZeroMqConfiguration zeroMqConfiguration;
    private ZeroMqProvider zeroMqProvider;

    private static Type TYPE_RECEIVED = new TypeToken<Map<String, Double>>() {
    }.getType();

    public ZeroMqFactorProvider(ZeroMqConfiguration zeroMqConfiguration, int threadsListening) {
        if (zeroMqConfiguration.getHost() == null || zeroMqConfiguration.getHost().equalsIgnoreCase("null")) {
            logger.warn("not creating ZeroMqFactorProvider because zeroMqConfiguration host is null");
            return;
        }
        this.zeroMqConfiguration = new ZeroMqConfiguration(zeroMqConfiguration);
        zeroMqProvider = ZeroMqProvider.getInstance(this.zeroMqConfiguration, threadsListening);
        zeroMqProvider.register(this.zeroMqConfiguration, this);
        logger.info("Listening FactorData {}   in {}}", this.zeroMqConfiguration.getTopic(),
                this.zeroMqConfiguration.getUrl());
    }

    public void start() {
        if (zeroMqProvider == null) {
            return;
        }
        this.statisticsReceived = null;
        zeroMqProvider.start(true, true);
    }

    protected Map<String, Double> getFactors(String jsonContent) {
        Map<String, Double> factorsReceived = GSON.fromJson(jsonContent, TYPE_RECEIVED);
        boolean hasNulls = factorsReceived.containsValue(null);
        if (hasNulls) {
            return null;
        }
        return factorsReceived;
    }

    @Override
    public void onUpdate(ConnectorConfiguration configuration, long timestampReceived, TypeMessage typeMessage, String content) {
        ZeroMqConfiguration zeroMqConfigurationReceived = (ZeroMqConfiguration) configuration;
        String topicReceived = zeroMqConfigurationReceived.getTopic();

        if (typeMessage != TypeMessage.factor) {
            logger.warn("received a not {} message on ZeroMqFactorProvider {}", TypeMessage.factor, zeroMqConfiguration.getPort());
            return;
        }

        if (statisticsReceived != null)
            statisticsReceived.addStatistics(topicReceived);
        //{"btcusdt_binance":-0.25,"ethbtc_binance":0.25,"btceur_binance":-0.25,"ethusdt_binance":0.25}
        //{"ethbtc_binance":0.25,"ethusdt_binance":0.25,"btceur_binance":-0.25,"btcusdt_binance":-0.25}
        ///transform json received into Map<string,double>
        Map<String, Double> factorsReceived = getFactors(content);
        if (factorsReceived == null) {
            logger.warn("Received factors {} with nulls-> {}", topicReceived, content);
        }
        //notify to listeners
        notifyFactor(System.currentTimeMillis(), factorsReceived);
    }
}
