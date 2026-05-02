package com.lambda.investing.trading_engine_connector;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.market_data_connector.ZeroMqMarketDataConnectorDisruptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ZeroMqTradingEngineConnectorFactory {
    private static final Logger logger =
            LogManager.getLogger(ZeroMqTradingEngineConnectorFactory.class);

    public static ZeroMqTradingEngineConnector create(String name, int threadsPublish, int threadsListen,
                                                      ZeroMqConfiguration zeroMqConfigurationExecutionReportListening,
                                                      ZeroMqConfiguration zeroMqConfigurationOrderRequest) {
        switch (Configuration.LIVE_CONNECTOR_PROVIDER) {
            case ORDINARY -> {
                return new ZeroMqTradingEngineConnector(name, threadsPublish, threadsListen, zeroMqConfigurationExecutionReportListening, zeroMqConfigurationOrderRequest);
            }
            case DISRUPTOR_LOW_LATENCY, DISRUPTOR_HIGH_THROUGHPUT -> {
                return new ZeroMqTradingEngineConnectorDisruptor(name, threadsPublish, threadsListen, zeroMqConfigurationExecutionReportListening, zeroMqConfigurationOrderRequest, Configuration.LIVE_CONNECTOR_PROVIDER);
            }
            default -> {
                throw new IllegalArgumentException("Unsupported connector publisher provider type: " + Configuration.LIVE_CONNECTOR_PROVIDER);
            }

        }

    }

}
