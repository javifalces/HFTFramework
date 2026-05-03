package com.lambda.investing.connector.disruptor;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.model.CSVable;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

/**
 * Shared low-latency ring-buffer infrastructure for ZeroMq inbound connectors.
 *
 * <p>Encapsulates the LMAX Disruptor lifecycle, the single-producer hot path
 * ({@link #publish}), and a configurable startup warmup cycle that drives
 * synthetic events through the full pipeline to prime the JIT compiler and
 * pre-touch ring-buffer memory pages before the first real message arrives.
 *
 * <h2>Multiple consumers on a shared ring buffer</h2>
 * Multiple connectors with different concerns (market-data, execution-reports)
 * may share the <em>same</em> ring buffer by calling
 *  with the same {@code consumerThreadName}.
 * Each connector registers its callback via
 * {@link #addConsumer(EventConsumer, TypeMessage...)} with a set of
 * {@link TypeMessage} types it cares about; events are routed exclusively to
 * matching consumers so unrelated messages incur zero processing cost.
 *
 * <pre>{@code
 * // Both connectors share one ring buffer
 * DisruptorConnectorHelper helper = DisruptorConnectorHelper.getInstance("zmq-shared");
 * helper.init();   // idempotent – starts the ring buffer only once
 * helper.addConsumer(this::processDepth, TypeMessage.depth, TypeMessage.trade);
 *
 * // From a second connector:
 * DisruptorConnectorHelper helper = DisruptorConnectorHelper.getInstance("zmq-shared");
 * helper.init();   // no-op – ring buffer already running
 * helper.addConsumer(this::processER, TypeMessage.execution_report, TypeMessage.info);
 * }</pre>
 *
 * <h2>Thread safety</h2>
 * {@link #publish} is designed for a single producer thread (ZeroMq I/O callback).
 * Consumer registrations are stored in a {@link CopyOnWriteArrayList} so
 * {@link #addConsumer} is safe to call at any time.
 */
public class DisruptorConnectorHelper {

    // -----------------------------------------------------------------------
    // Singleton registry – one instance per consumer-thread name
    // -----------------------------------------------------------------------

    private static final ConcurrentHashMap<String, DisruptorConnectorHelper> INSTANCES =
            new ConcurrentHashMap<>();

    /**
     * Returns the singleton {@link DisruptorConnectorHelper} for the given
     * {@code consumerThreadName}, creating it on the first call.
     * Multiple connectors that pass the same name share one ring buffer.
     *
     * @param consumerThreadName Unique name that identifies this Disruptor instance
     * (e.g. {@code "zmq-shared-disruptor"}).
     */
    private static final Logger STATIC_LOGGER = LogManager.getLogger(DisruptorConnectorHelper.class);

    public static DisruptorConnectorHelper getInstance(String consumerThreadName, Configuration.ConnectorProviderType connectorProviderType) {
        final boolean[] created = {false};
        DisruptorConnectorHelper helper = INSTANCES.computeIfAbsent(consumerThreadName, name -> {
            created[0] = true;
            return new DisruptorConnectorHelper(name, connectorProviderType);
        });
        if (created[0]) {
            STATIC_LOGGER.info("DisruptorConnectorHelper [{}] – new instance created", consumerThreadName);
        } else {
            STATIC_LOGGER.info("DisruptorConnectorHelper [{}] – reusing existing instance (consumers={})",
                    consumerThreadName, helper.consumers.size());
        }
        return helper;
    }

    /**
     * Removes and shuts down the singleton for {@code consumerThreadName}.
     * Useful in tests that need a clean state between runs.
     */
    public static void removeInstance(String consumerThreadName) {
        DisruptorConnectorHelper helper = INSTANCES.remove(consumerThreadName);
        if (helper != null) {
            helper.shutdown();
        }
    }

    // -----------------------------------------------------------------------
    // Public constants – safe to reuse in connector implementations
    // -----------------------------------------------------------------------

    /**
     * Sentinel instrument tag used exclusively by warmup events.
     */
    public static final String WARMUP_INSTRUMENT = "__WARMUP__";
    public static final String WARMUP_HOST = "localhost";
    public static final int WARMUP_PORT = 0;

    // -----------------------------------------------------------------------
    // Consumer callback contract
    // -----------------------------------------------------------------------

    /**
     * Callback invoked on the Disruptor consumer thread for every dequeued event
     * whose {@link TypeMessage} matches the subscription registered via
     * {@link #addConsumer(EventConsumer, TypeMessage...)}.
     */
    @FunctionalInterface
    public interface EventConsumer {
        void consume(ConnectorConfiguration configuration,
                     long timestampReceived,
                     TypeMessage typeMessage,
                     Object content);
    }

    // -----------------------------------------------------------------------
    // Per-consumer registration (consumer + optional type filter)
    // -----------------------------------------------------------------------

    private static final class ConsumerRegistration {
        final EventConsumer consumer;
        /**
         * Empty means "accept all TypeMessages".
         */
        final Set<TypeMessage> interestedTypes;

        ConsumerRegistration(EventConsumer consumer, TypeMessage[] types) {
            this.consumer = consumer;
            if (types == null || types.length == 0) {
                this.interestedTypes = Collections.emptySet();
            } else {
                this.interestedTypes = EnumSet.copyOf(Arrays.asList(types));
            }
        }

        boolean accepts(TypeMessage type) {
            return interestedTypes.isEmpty() || interestedTypes.contains(type);
        }
    }

    // -----------------------------------------------------------------------
    // Ring-buffer slot (pre-allocated, reused, never GC'd)
    // -----------------------------------------------------------------------

    static final class DisruptorEvent {
        ConnectorConfiguration configuration;
        long timestampReceived;
        TypeMessage typeMessage;
        Object content;

        void reset() {
            configuration = null;
            typeMessage = null;
            content = null;
        }
    }

    private static final EventFactory<DisruptorEvent> EVENT_FACTORY = DisruptorEvent::new;

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------

    private static final int RING_BUFFER_SIZE = Configuration.DISRUPTOR_RING_BUFFER_SIZE;
    private static final int WARMUP_ITERATIONS = Configuration.DISRUPTOR_WARMUP_ITERATIONS;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final Logger logger = LogManager.getLogger(DisruptorConnectorHelper.class);
    private final String consumerThreadName;

    /**
     * Thread-safe list of registered consumers; iterated on every event dispatch.
     */
    private final CopyOnWriteArrayList<ConsumerRegistration> consumers =
            new CopyOnWriteArrayList<>();

    private Disruptor<DisruptorEvent> disruptor;
    private RingBuffer<DisruptorEvent> ringBuffer;

    /**
     * Non-null only while the startup warmup drain is in progress.
     */
    private volatile CountDownLatch warmupLatch;
    private Configuration.ConnectorProviderType connectorProviderType;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Private – use .
     */
    private DisruptorConnectorHelper(String consumerThreadName, Configuration.ConnectorProviderType connectorProviderType) {
        this.consumerThreadName = consumerThreadName;
        this.connectorProviderType = connectorProviderType;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Starts the Disruptor ring-buffer and the dedicated consumer thread.
     * <strong>Idempotent</strong> – subsequent calls on an already-running
     * instance are a no-op, making it safe for multiple connectors that share
     * the same singleton to each call {@code init()} independently.
     *
     * <p>Call {@link #addConsumer} <em>after</em> this method to register event
     * handlers before publishing live messages.
     */
    public synchronized void init() {
        if (ringBuffer != null) {
            logger.debug("Disruptor [{}] already initialised – skipping", consumerThreadName);
            return;
        }

        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, consumerThreadName);
            t.setDaemon(true);
            return t;
        };

        switch (connectorProviderType) {
            case DISRUPTOR_LOW_LATENCY -> {
                disruptor = new Disruptor<>(
                        EVENT_FACTORY,
                        RING_BUFFER_SIZE,
                        threadFactory,
                        ProducerType.SINGLE,        // ZeroMq I/O callback is single-threaded
                        new BusySpinWaitStrategy()  // lowest latency – dedicates one CPU core
                );
            }
            case DISRUPTOR_HIGH_THROUGHPUT -> {
                disruptor = new Disruptor<>(
                        EVENT_FACTORY,
                        RING_BUFFER_SIZE,
                        threadFactory,
                        ProducerType.MULTI,         // supports multiple producer threads if needed
                        new com.lmax.disruptor.YieldingWaitStrategy()  // good latency with less CPU than BusySpin
                );
            }
            default ->
                    throw new IllegalArgumentException("DisruptorConnectorHelper Unsupported ConnectorProviderType: " + connectorProviderType);
        }


        disruptor.handleEventsWith(this::dispatchEvent);
        ringBuffer = disruptor.start();

        logger.info("Disruptor started (consumerThread={}, ringBufferSize={}, waitStrategy=BusySpin)",
                consumerThreadName, RING_BUFFER_SIZE);
    }

    /**
     * Registers a new event consumer.
     *
     * @param consumer        Callback invoked on the consumer thread.
     * @param interestedTypes One or more {@link TypeMessage} values this consumer
     *                        cares about.  Pass <em>no arguments</em> (or an empty
     *                        array) to receive <em>all</em> message types.
     */
    public void addConsumer(EventConsumer consumer, TypeMessage... interestedTypes) {
        consumers.add(new ConsumerRegistration(consumer, interestedTypes));
        logger.info("Disruptor [{}] consumer registered (filter={})",
                consumerThreadName,
                interestedTypes.length == 0 ? "ALL" : Arrays.toString(interestedTypes));
    }

    /**
     * Removes a previously registered event consumer by identity.
     * Safe to call at any time; the change takes effect on the next event dispatch.
     *
     * @param consumer The consumer instance to remove (matched by reference equality).
     */
    public void removeConsumer(EventConsumer consumer) {
        boolean removed = consumers.removeIf(reg -> reg.consumer == consumer);
        if (removed) {
            logger.info("Disruptor [{}] consumer deregistered", consumerThreadName);
        } else {
            logger.warn("Disruptor [{}] removeConsumer: consumer not found", consumerThreadName);
        }
    }

    /**
     * Primes the JIT by publishing {@code DISRUPTOR_WARMUP_ITERATIONS} synthetic events
     * through the full pipeline and blocking until the consumer thread has drained them.
     *
     * <p>Call this <em>after</em> {@link #init} and {@link #addConsumer} and
     * <em>before</em> registering real listeners so that no warmup event leaks
     * to business logic.
     *
     * @param warmupCfg      {@link ZeroMqConfiguration} to tag warmup events with.
     * @param warmupType     {@link TypeMessage} to use for warmup events
     *                       (should match the primary type of the registering connector).
     * @param contentFactory Factory called once per warmup iteration.
     */
    public void warmup(ZeroMqConfiguration warmupCfg,
                       TypeMessage warmupType,
                       IntFunction<Object> contentFactory) {
        if (WARMUP_ITERATIONS <= 0) {
            return;
        }

        logger.info("Disruptor warmup starting (thread={}, type={}, {} events) ...",
                consumerThreadName, warmupType, WARMUP_ITERATIONS);
        final long warmupStart = System.nanoTime();

        // Latch counted down once per warmup event by dispatchEvent().
        final CountDownLatch latch = new CountDownLatch(WARMUP_ITERATIONS);
        warmupLatch = latch;

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Object content = contentFactory.apply(i);
            final long seq = ringBuffer.next();
            try {
                final DisruptorEvent slot = ringBuffer.get(seq);
                slot.configuration = warmupCfg;
                slot.timestampReceived = System.nanoTime();
                slot.typeMessage = warmupType;
                slot.content = content;
            } finally {
                ringBuffer.publish(seq);

                if (content instanceof Depth depth) {
                    depth.delete();
                }

                if (content instanceof Trade trade) {
                    trade.delete();
                }

            }
        }

        // Block until the consumer thread has processed every warmup event.
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                logger.warn("Disruptor warmup [{}] did not drain within 10 s ({} events still pending)",
                        consumerThreadName, latch.getCount());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Disruptor warmup [{}] interrupted", consumerThreadName);
        } finally {
            warmupLatch = null;
        }

        final long elapsedMs = (System.nanoTime() - warmupStart) / 1_000_000L;
        logger.info("Disruptor warmup complete (thread={}, type={}, {} events in {} ms)",
                consumerThreadName, warmupType, WARMUP_ITERATIONS, elapsedMs);
    }

    /**
     * Gracefully shuts down the Disruptor, draining any in-flight events.
     */
    public void shutdown() {
        if (disruptor != null) {
            disruptor.shutdown();
        }
    }

    /**
     * Returns {@code true} once {@link #init} has been called successfully.
     */
    public boolean isReady() {
        return ringBuffer != null;
    }

    // -----------------------------------------------------------------------
    // Producer – hot path (ZeroMq I/O thread)
    // -----------------------------------------------------------------------

    /**
     * Publishes an inbound ZeroMq event to the ring buffer.
     * Hot path: grabs the next sequence, writes four references into the
     * pre-allocated slot, and publishes in O(1) with no allocation.
     *
     * @return {@code false} if the disruptor has not been initialised yet.
     */
    public boolean publish(ConnectorConfiguration configuration,
                           long timestampReceived,
                           TypeMessage typeMessage,
                           Object content) {
        if (ringBuffer == null) {
            return false;
        }
        final long sequence = ringBuffer.next();
        try {
            final DisruptorEvent slot = ringBuffer.get(sequence);
            slot.configuration = configuration;
            slot.timestampReceived = timestampReceived;
            slot.typeMessage = typeMessage;
            slot.content = content;
        } finally {
            ringBuffer.publish(sequence);
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Consumer – dedicated consumer thread
    // -----------------------------------------------------------------------

    private void dispatchEvent(DisruptorEvent slot, long sequence, boolean endOfBatch) {
        try {
            // Fan-out to every consumer whose type filter matches this event.
            // CopyOnWriteArrayList iteration is allocation-free after the snapshot.
            for (ConsumerRegistration reg : consumers) {
                if (reg.accepts(slot.typeMessage)) {
                    try {
                        reg.consumer.consume(slot.configuration, slot.timestampReceived,
                                slot.typeMessage, slot.content);
                    } catch (Exception e) {
                        logger.error("[{}] Consumer error dispatching typeMessage={}",
                                consumerThreadName, slot.typeMessage, e);
                    }
                }
            }
        } finally {
            // Count down the warmup latch.  On the live path this is a single
            // volatile read (warmupLatch == null) → negligible overhead.
            final CountDownLatch latch = warmupLatch;
            if (latch != null) {
                latch.countDown();
            }
            slot.reset();
        }
    }
}
