package com.lambda.investing.connector.zero_mq;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.disruptor.DisruptorConnectorHelper;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Low-latency variant of {@link ZeroMqProvider} that interposes an LMAX Disruptor
 * ring-buffer between the ZeroMq I/O thread and the registered
 * {@link com.lambda.investing.connector.ConnectorListener}s.
 *
 * <h2>Architecture</h2>
 * <pre>
 *  ZeroMq I/O thread
 *    └─ onUpdate(TypeMessage, Object, String, long)
 *             │
 *             ▼  DisruptorConnectorHelper.publish()  (ring-buffer write, ~50 ns)
 *                     │
 *                     ▼  [zmq-provider-disruptor thread]
 *                 dispatchToListeners()
 *                     │
 *                     ▼
 *                 ZeroMqProvider.onUpdate(TypeMessage, Object, String, long)
 *                     └─ notifies all registered ConnectorListeners
 * </pre>
 *
 * <p>The shared ring-buffer infrastructure ({@link DisruptorConnectorHelper}) is
 *
 * <h2>Allocation profile (steady-state hot path)</h2>
 * <ul>
 *   <li>Per-topic {@link ZeroMqConfiguration} objects are cached in
 *       {@link #topicConfigCache}; after the first message per topic no allocation
 *       occurs in {@link #onUpdate(TypeMessage, Object, String, long)}.
 *   <li>The Disruptor slot itself is pre-allocated – only four references are written.
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <pre>{@code
 * ZeroMqProviderDisruptor provider = new ZeroMqProviderDisruptor(cfg, 1, false);
 * provider.register(cfg, myListener);
 * provider.start();   // initialises Disruptor + warmup, then starts ZeroMq listener
 * }</pre>
 */
public class ZeroMqProviderDisruptor extends ZeroMqProvider {

    private static final Logger logger = LogManager.getLogger(ZeroMqProviderDisruptor.class);

    // -----------------------------------------------------------------------
    // Disruptor – delegated to the shared helper
    // -----------------------------------------------------------------------

    private DisruptorConnectorHelper helper;
    private final String disruptorThreadName;
    private final Configuration.ConnectorProviderType connectorProviderType;

    /**
     * Per-topic {@link ZeroMqConfiguration} cache.
     * Topics are finite (typically 1–5 per connector); caching avoids per-message
     * allocation on the producer hot path while preserving topic metadata for consumers.
     */
    private final ConcurrentHashMap<String, ZeroMqConfiguration> topicConfigCache =
            new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Full constructor.
     *
     * @param zeroMqConfiguration   ZeroMq configuration for the subscription socket.
     * @param threadsListening      Number of ZeroMq listener threads
     *                              (≥1 for fixed pool; ≤0 for cached pool).
     * @param isServer              {@code true} to bind (server/publisher side);
     *                              {@code false} to connect (client/subscriber side).
     * @param connectorProviderType Disruptor wait strategy:
     *                              {@link Configuration.ConnectorProviderType#DISRUPTOR_LOW_LATENCY}
     *                              (BusySpin, lowest latency, dedicates one core) or
     *                              {@link Configuration.ConnectorProviderType#DISRUPTOR_HIGH_THROUGHPUT}
     *                              (Yielding, lower CPU at slightly higher latency).
     */
    public ZeroMqProviderDisruptor(ZeroMqConfiguration zeroMqConfiguration,
                                   int threadsListening,
                                   boolean isServer,
                                   Configuration.ConnectorProviderType connectorProviderType) {
        super(zeroMqConfiguration, threadsListening);
        setServer(isServer);
        this.disruptorThreadName = "zmq-provider-disruptor-" + zeroMqConfiguration;
        this.connectorProviderType = connectorProviderType;
    }

    /**
     * Convenience constructor – defaults to
     * {@link Configuration.ConnectorProviderType#DISRUPTOR_LOW_LATENCY}.
     *
     * @param zeroMqConfiguration ZeroMq configuration for the subscription socket.
     * @param threadsListening    Number of ZeroMq listener threads.
     * @param isServer            {@code true} to bind; {@code false} to connect.
     */
    public ZeroMqProviderDisruptor(ZeroMqConfiguration zeroMqConfiguration,
                                   int threadsListening,
                                   boolean isServer) {
        this(zeroMqConfiguration, threadsListening, isServer,
                Configuration.ConnectorProviderType.DISRUPTOR_LOW_LATENCY);
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Initialises the LMAX Disruptor ring-buffer and consumer thread, then runs
     * the startup warmup cycle.
     *
     * <p>Extracted so that subclasses (e.g. test fixtures) can start the Disruptor
     * without also bringing up the ZeroMq provider:
     * <pre>{@code
     * @Override public void start() { initDisruptor(); }
     * }</pre>
     */
    protected void initDisruptor() {
        helper = DisruptorConnectorHelper.getInstance(disruptorThreadName, connectorProviderType);
        helper.init();
        // Accept ALL TypeMessage types – this is a general-purpose provider.
        // Downstream ConnectorListeners apply their own filtering.
        helper.addConsumer(this::dispatchToListeners);
        warmupDisruptor();
    }

    /**
     * Primes the JIT and pre-touches ring-buffer memory by publishing synthetic
     * {@link TypeMessage#info} events through the full pipeline.
     * Blocks until all warmup events are drained so no event leaks to real listeners.
     *
     * <p>Override in tests or subclasses to skip warmup:
     * <pre>{@code @Override protected void warmupDisruptor() { } }</pre>
     */
    protected void warmupDisruptor() {
        ZeroMqConfiguration warmupCfg = new ZeroMqConfiguration(
                DisruptorConnectorHelper.WARMUP_HOST,
                DisruptorConnectorHelper.WARMUP_PORT,
                DisruptorConnectorHelper.WARMUP_INSTRUMENT);
        // Use TypeMessage.info as a lightweight, allocation-free warmup content.
        helper.warmup(warmupCfg, TypeMessage.info, i -> "warmup_" + i);
    }

    /**
     * Initialises the Disruptor consumer thread <em>before</em> the ZeroMq provider
     * so the ring buffer is ready to drain as soon as the first message arrives.
     */
    @Override
    public void start() {
        initDisruptor();
        super.start();
    }

    /**
     * Drains any remaining events and then shuts down the Disruptor consumer thread.
     */
    public void stop() {
        if (helper != null) {
            helper.shutdown();
        }
    }

    // -----------------------------------------------------------------------
    // Producer – hot path (ZeroMq I/O thread)
    // -----------------------------------------------------------------------

    /**
     * Hot path: encodes the topic into a cached {@link ZeroMqConfiguration} and
     * delegates to {@link DisruptorConnectorHelper#publish} – no business logic,
     * no allocation after the first message per topic.
     *
     * <p>Falls back to synchronous {@code super.onUpdate} if the Disruptor has not
     * been initialised yet (prevents dropped messages during startup race).
     *
     * @param typeMessage Type of the inbound ZeroMq message.
     * @param message     Deserialised message payload.
     * @param topic       ZeroMq topic string on which the message arrived.
     * @param timestamp   Arrival timestamp in milliseconds (from ZeroMq receive loop).
     */
    @Override
    protected void onUpdate(TypeMessage typeMessage,
                            Object message,
                            String topic,
                            long timestamp) throws IOException {
        if (helper == null || !helper.isReady()) {
            // Fallback: process synchronously if Disruptor is not yet initialised.
            super.onUpdate(typeMessage, message, topic, timestamp);
            return;
        }

        // Resolve (or create on first use) the per-topic configuration.
        // computeIfAbsent is allocation-free once all active topics are cached.
        ZeroMqConfiguration topicCfg = topicConfigCache.computeIfAbsent(topic, t -> {
            ZeroMqConfiguration base = getZeroMqConfiguration();
            return new ZeroMqConfiguration(base.getHost(), base.getPort(), t);
        });

        helper.publish(topicCfg, timestamp, typeMessage, message);
    }

    // -----------------------------------------------------------------------
    // Consumer – dedicated zmq-provider-disruptor thread
    // -----------------------------------------------------------------------

    /**
     * Invoked on the Disruptor consumer thread for every dequeued event.
     * Extracts the topic from the {@link ZeroMqConfiguration} slot and
     * delegates to the parent's listener fan-out logic.
     *
     * <p>Warmup events tagged with
     * {@link DisruptorConnectorHelper#WARMUP_INSTRUMENT} are silently discarded
     * so they never reach registered {@link com.lambda.investing.connector.ConnectorListener}s.
     *
     * @param configuration     {@link ZeroMqConfiguration} carrying the topic.
     * @param timestampReceived Arrival timestamp in milliseconds.
     * @param typeMessage       Message type.
     * @param content           Deserialised message payload.
     */
    private void dispatchToListeners(ConnectorConfiguration configuration,
                                     long timestampReceived,
                                     TypeMessage typeMessage,
                                     Object content) {
        String topic = "";
        if (configuration instanceof ZeroMqConfiguration zmqCfg) {
            topic = zmqCfg.getTopic();
        }

        // Silently drop warmup sentinel events – no registered listener should see them.
        if (DisruptorConnectorHelper.WARMUP_INSTRUMENT.equals(topic)) {
            return;
        }

        try {
            super.onUpdate(typeMessage, content, topic, timestampReceived);
        } catch (IOException e) {
            logger.error("[{}] Error dispatching ZeroMq event (typeMessage={}, topic={})",
                    disruptorThreadName, typeMessage, topic, e);
        }
    }
}
