package com.lambda.investing.connector.zero_mq;

import com.lambda.investing.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Factory for creating {@link ZeroMqProvider} instances.
 *
 * <p>Examines {@link Configuration#LIVE_CONNECTOR_PROVIDER} and returns the
 * appropriate implementation:
 * <ul>
 *   <li>{@link Configuration.ConnectorProviderType#ORDINARY} →
 *       {@link ZeroMqProvider} (standard, synchronous dispatch)
 *   <li>{@link Configuration.ConnectorProviderType#DISRUPTOR_LOW_LATENCY} or
 *       {@link Configuration.ConnectorProviderType#DISRUPTOR_HIGH_THROUGHPUT} →
 *       {@link ZeroMqProviderDisruptor} (LMAX ring-buffer, off-thread dispatch)
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ZeroMqProvider provider = ZeroMqProviderFactory.create(cfg, 1, false);
 * provider.register(cfg, myListener);
 * provider.start();
 * }</pre>
 */
public class ZeroMqProviderFactory {

    private static final Logger logger = LogManager.getLogger(ZeroMqProviderFactory.class);

    private ZeroMqProviderFactory() {
        // utility class – do not instantiate
    }

    /**
     * Creates a {@link ZeroMqProvider} (or its Disruptor variant) based on
     * {@link Configuration#LIVE_CONNECTOR_PROVIDER}.
     *
     * @param zeroMqConfiguration ZeroMq socket configuration.
     * @param threadsListening    Number of ZeroMq listener threads.
     * @param isServer            {@code true} to bind; {@code false} to connect.
     * @return A fully constructed (but not yet started) {@link ZeroMqProvider}.
     */
    public static ZeroMqProvider create(ZeroMqConfiguration zeroMqConfiguration,
                                        int threadsListening,
                                        boolean isServer) {
        return create(zeroMqConfiguration, threadsListening, isServer,
                Configuration.LIVE_CONNECTOR_PROVIDER);
    }

    public static ZeroMqProvider create(ZeroMqConfiguration zeroMqConfiguration,
                                        int threadsListening
    ) {
        return create(zeroMqConfiguration, threadsListening, false,
                Configuration.LIVE_CONNECTOR_PROVIDER);
    }

    /**
     * Creates a {@link ZeroMqProvider} (or its Disruptor variant) for the
     * explicitly supplied {@code connectorProviderType}.
     *
     * @param zeroMqConfiguration   ZeroMq socket configuration.
     * @param threadsListening      Number of ZeroMq listener threads.
     * @param isServer              {@code true} to bind; {@code false} to connect.
     * @param connectorProviderType Provider type override.
     * @return A fully constructed (but not yet started) {@link ZeroMqProvider}.
     * @throws IllegalArgumentException if {@code connectorProviderType} is not supported.
     */
    public static ZeroMqProvider create(ZeroMqConfiguration zeroMqConfiguration,
                                        int threadsListening,
                                        boolean isServer,
                                        Configuration.ConnectorProviderType connectorProviderType) {
        switch (connectorProviderType) {
            case ORDINARY -> {
                logger.info("ZeroMqProviderFactory – creating ordinary ZeroMqProvider ({})", zeroMqConfiguration);
                return ZeroMqProvider.getInstance(zeroMqConfiguration, threadsListening, isServer);
            }
            case DISRUPTOR_LOW_LATENCY, DISRUPTOR_HIGH_THROUGHPUT -> {
                logger.info("ZeroMqProviderFactory – creating ZeroMqProviderDisruptor ({}, strategy={})",
                        zeroMqConfiguration, connectorProviderType);
                return ZeroMqProviderDisruptor.getInstance(zeroMqConfiguration, threadsListening, isServer,
                        connectorProviderType);
            }
            default -> throw new IllegalArgumentException(
                    "ZeroMqProviderFactory: unsupported ConnectorProviderType: " + connectorProviderType);
        }
    }

    public static ZeroMqProvider create(ZeroMqConfiguration zeroMqConfiguration, String s, Boolean aBoolean, String s1) {
        Configuration.ConnectorProviderType connectorProviderType = Configuration.ConnectorProviderType.valueOf(s1);
        return create(zeroMqConfiguration, 1, aBoolean, connectorProviderType);
    }
}
