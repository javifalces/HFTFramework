package com.lambda.investing.market_data_connector;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ZeroMqMarketDataConnectorFactory {
    private static final Logger logger =
            LogManager.getLogger(ZeroMqMarketDataConnectorFactory.class);

    public static ZeroMqMarketDataConnector create(ZeroMqConfiguration zeroMqConfiguration, int threadsListening) {
        switch (Configuration.LIVE_CONNECTOR_PROVIDER) {
            case ORDINARY -> {
                return new ZeroMqMarketDataConnector(zeroMqConfiguration, threadsListening);
            }
            case DISRUPTOR_LOW_LATENCY, DISRUPTOR_HIGH_THROUGHPUT -> {
                return new ZeroMqMarketDataConnectorDisruptor(zeroMqConfiguration, threadsListening, Configuration.LIVE_CONNECTOR_PROVIDER);
            }

            default -> {
                throw new IllegalArgumentException("Unsupported connector publisher provider type: " + Configuration.LIVE_CONNECTOR_PROVIDER);
            }

        }

    }
}
