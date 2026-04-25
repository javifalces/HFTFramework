package com.lambda.investing.connector;

import com.lambda.investing.connector.disruptor.DisruptorConnectorPublisherProvider;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorPublisherProvider;
import com.lmax.disruptor.BlockingWaitStrategy;
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
     * Default ring-buffer size for Disruptor instances (must be a power of 2).
     */
    public static final int DEFAULT_RING_SIZE = 512;

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

    /**
     * Creates an {@link OrdinaryConnectorPublisherProvider} with a fixed thread pool
     * at {@link Thread#NORM_PRIORITY}.
     *
     * @param name           name used for the thread pool
     * @param publishThreads number of publisher threads
     * @return a configured {@link OrdinaryConnectorPublisherProvider}
     */
    public static OrdinaryConnectorPublisherProvider createOrdinary(String name, int publishThreads) {
        return createOrdinary(name, publishThreads, DEFAULT_PRIORITY);
    }

    /**
     * Creates an {@link OrdinaryConnectorPublisherProvider} that dispatches
     * synchronously (inline, no thread pool) at {@link Thread#NORM_PRIORITY}.
     *
     * @param name name used to identify this provider
     * @return a synchronous {@link OrdinaryConnectorPublisherProvider}
     */
    public static OrdinaryConnectorPublisherProvider createOrdinarySync(String name) {
        return createOrdinary(name, 0, DEFAULT_PRIORITY);
    }

    /**
     * Creates an {@link OrdinaryConnectorPublisherProvider} backed by a
     * cached (unbounded) thread pool at {@link Thread#NORM_PRIORITY}.
     *
     * @param name name used for the thread pool
     * @return a cached-pool {@link OrdinaryConnectorPublisherProvider}
     */
    public static OrdinaryConnectorPublisherProvider createOrdinaryCached(String name) {
        return createOrdinary(name, -1, DEFAULT_PRIORITY);
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
    public static DisruptorConnectorPublisherProvider createDisruptor(
            String name, int priority, int sizeRing,
            WaitStrategy waitStrategy, ProducerType producerType) {
        return new DisruptorConnectorPublisherProvider(name, priority, sizeRing, waitStrategy, producerType);
    }

    /**
     * Creates a {@link DisruptorConnectorPublisherProvider} at
     * {@link Thread#NORM_PRIORITY} with default ring size, a
     * {@link BlockingWaitStrategy} and {@link ProducerType#MULTI}.
     *
     * @param name name used for the disruptor threads
     * @return a default {@link DisruptorConnectorPublisherProvider}
     */
    public static DisruptorConnectorPublisherProvider createDisruptor(String name) {
        return createDisruptor(name, DEFAULT_PRIORITY, DEFAULT_RING_SIZE,
                new BlockingWaitStrategy(), ProducerType.MULTI);
    }

    /**
     * Creates a {@link DisruptorConnectorPublisherProvider} with a custom ring size
     * at {@link Thread#NORM_PRIORITY}, using a {@link BlockingWaitStrategy} and
     * {@link ProducerType#MULTI}.
     *
     * @param name     name used for the disruptor threads
     * @param sizeRing ring-buffer size (must be a power of 2)
     * @return a {@link DisruptorConnectorPublisherProvider}
     */
    public static DisruptorConnectorPublisherProvider createDisruptor(String name, int sizeRing) {
        return createDisruptor(name, DEFAULT_PRIORITY, sizeRing,
                new BlockingWaitStrategy(), ProducerType.MULTI);
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
    public static DisruptorConnectorPublisherProvider createDisruptorLowLatency(String name, int sizeRing) {
        return createDisruptor(name, Thread.MAX_PRIORITY, sizeRing,
                new BusySpinWaitStrategy(), ProducerType.SINGLE);
    }

    /**
     * Creates a low-latency {@link DisruptorConnectorPublisherProvider} using a
     * {@link BusySpinWaitStrategy}, {@link ProducerType#SINGLE} and the default ring
     * size at {@link Thread#MAX_PRIORITY}.
     *
     * @param name name used for the disruptor thread
     * @return a low-latency single-producer {@link DisruptorConnectorPublisherProvider}
     */
    public static DisruptorConnectorPublisherProvider createDisruptorLowLatency(String name) {
        return createDisruptorLowLatency(name, DEFAULT_RING_SIZE);
    }

    /**
     * Creates a throughput-optimised {@link DisruptorConnectorPublisherProvider} using
     * a {@link YieldingWaitStrategy} and {@link ProducerType#MULTI}.
     *
     * @param name     name used for the disruptor threads
     * @param sizeRing ring-buffer size (must be a power of 2)
     * @return a high-throughput multi-producer {@link DisruptorConnectorPublisherProvider}
     */
    public static DisruptorConnectorPublisherProvider createDisruptorHighThroughput(String name, int sizeRing) {
        return createDisruptor(name, DEFAULT_PRIORITY, sizeRing,
                new YieldingWaitStrategy(), ProducerType.MULTI);
    }

    /**
     * Creates a throughput-optimised {@link DisruptorConnectorPublisherProvider} using
     * a {@link YieldingWaitStrategy}, {@link ProducerType#MULTI} and the default ring
     * size.
     *
     * @param name name used for the disruptor threads
     * @return a high-throughput multi-producer {@link DisruptorConnectorPublisherProvider}
     */
    public static DisruptorConnectorPublisherProvider createDisruptorHighThroughput(String name) {
        return createDisruptorHighThroughput(name, DEFAULT_RING_SIZE);
    }
}

