package com.lambda.investing.connector;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.disruptor.DisruptorConnectorPublisherProvider;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorPublisherProvider;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * Factory class for creating {@link OrdinaryConnectorPublisherProvider} and
 * {@link DisruptorConnectorPublisherProvider} instances with common configurations.
 */
public final class ConnectorPublisherProviderFactory {



    /**
     * Default thread priority used when none is specified.
     */
    public static final int DEFAULT_PRIORITY = Thread.NORM_PRIORITY;

    private ConnectorPublisherProviderFactory() {
        // utility class – do not instantiate
    }

    // -------------------------------------------------------------------------
    // OrdinaryConnectorPublisherProvider factories
    // -------------------------------------------------------------------------

    /**
     * Creates an {@link OrdinaryConnectorPublisherProvider} with a fixed thread pool.
     *
     * @param name           name used for the thread pool
     * @param publishThreads number of publisher threads ({@code < 0} → cached pool,
     *                       {@code 0} → synchronous/inline, {@code > 0} → fixed pool)
     * @param priority       thread priority
     * @return a configured {@link OrdinaryConnectorPublisherProvider}
     */
    public static OrdinaryConnectorPublisherProvider createOrdinary(String name, int publishThreads, int priority) {
        return new OrdinaryConnectorPublisherProvider(name, publishThreads, priority);
    }


    public static AbstractConnectorPublisherProvider createConnectorPublisherProvider(Configuration.ConnectorPublisherProviderType type, String name, int publishThreads, int priority) {
        switch (type) {
            case DISRUPTOR_HIGH_THROUGHPUT:
                return createDisruptorHighThroughput(name, Configuration.DISRUPTOR_RING_BUFFER_SIZE);
            case DISRUPTOR_LOW_LATENCY:
                return createDisruptorLowLatency(name, Configuration.DISRUPTOR_RING_BUFFER_SIZE);
            case ORDINARY:
                return createOrdinary(name, publishThreads, priority);
            default:
                throw new IllegalArgumentException("Unknown connector publisher provider: " + Configuration.BACKTEST_CONNECTOR_PUBLISHER_PROVIDER);
        }

    }

    // -------------------------------------------------------------------------
    // DisruptorConnectorPublisherProvider factories
    // -------------------------------------------------------------------------

    /**
     * Creates a fully-configured {@link DisruptorConnectorPublisherProvider}.
     *
     * @param name         name used for the disruptor threads
     * @param priority     thread priority
     * @param sizeRing     ring-buffer size (must be a power of 2)
     * @param waitStrategy disruptor {@link WaitStrategy}
     * @param producerType {@link ProducerType#SINGLE} (lock-free) or
     *                     {@link ProducerType#MULTI} (CAS-protected)
     * @return a configured {@link DisruptorConnectorPublisherProvider}
     */
    private static DisruptorConnectorPublisherProvider createDisruptor(
            String name, int priority, int sizeRing,
            WaitStrategy waitStrategy, ProducerType producerType) {
        return new DisruptorConnectorPublisherProvider(name, priority, sizeRing, waitStrategy, producerType);
    }


    /**
     * Creates a low-latency {@link DisruptorConnectorPublisherProvider} using a
     * {@link BusySpinWaitStrategy} and {@link ProducerType#SINGLE} at
     * {@link Thread#MAX_PRIORITY}.
     *
     * @param name     name used for the disruptor thread
     * @param sizeRing ring-buffer size (must be a power of 2)
     * @return a low-latency single-producer {@link DisruptorConnectorPublisherProvider}
     */
    private static DisruptorConnectorPublisherProvider createDisruptorLowLatency(String name, int sizeRing) {
        return createDisruptor(name, Thread.MAX_PRIORITY, sizeRing,
                new BusySpinWaitStrategy(), ProducerType.SINGLE);
    }


    /**
     * Creates a throughput-optimised {@link DisruptorConnectorPublisherProvider} using
     * a {@link YieldingWaitStrategy} and {@link ProducerType#MULTI}.
     *
     * @param name     name used for the disruptor threads
     * @param sizeRing ring-buffer size (must be a power of 2)
     * @return a high-throughput multi-producer {@link DisruptorConnectorPublisherProvider}
     */
    private static DisruptorConnectorPublisherProvider createDisruptorHighThroughput(String name, int sizeRing) {
        return createDisruptor(name, DEFAULT_PRIORITY, sizeRing,
                new YieldingWaitStrategy(), ProducerType.MULTI);
    }


}

