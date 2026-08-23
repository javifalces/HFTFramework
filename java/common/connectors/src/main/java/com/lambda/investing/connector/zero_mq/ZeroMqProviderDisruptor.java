package com.lambda.investing.connector.zero_mq;

import com.lambda.investing.Configuration;
import com.lambda.investing.LambdaThreadFactory;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.disruptor.DisruptorConnectorHelper;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Low-latency variant of {@link ZeroMqProvider} that replaces the
 * {@code ThreadPoolExecutor} dispatch path with an LMAX Disruptor ring-buffer.
 *
 * <h2>Threading architecture</h2>
 * <pre>
 *  ZeroMq kernel buffer
 *    └─ ZeroMqThreadReceiver  (inherited – owns the SUB socket, same as ZeroMqProvider)
 *             │  synchronized(socketSub) – minimal lock: read bytes + timestamp only
 *             │  deserialize (on receiver thread, no pool – use threadsListening=0)
 *             │
 *             ▼  onUpdate() override → DisruptorConnectorHelper.publish()  (~50 ns, no alloc)
 *                     │
 *                     ▼  [zmq-provider-disruptor thread]  (BusySpin or Yielding wait strategy)
 *                 per-listener EventConsumer
 *                     └─ ConnectorListener.onUpdate(...)
 * </pre>
 *
 * <h2>Relationship to ZeroMqProvider</h2>
 * {@code ZeroMqThreadReceiver} is still required here – it is the sole owner of the ZMQ
 * SUB socket and must keep it drained to avoid HWM-triggered message drops.  What this
 * subclass replaces is the <em>dispatch</em> step: instead of submitting
 * deserialization + listener delivery to a {@code ThreadPoolExecutor}, the receiver
 * thread publishes directly to the Disruptor ring-buffer and returns to the socket
 * immediately.
 *
 * <p><strong>Always construct with {@code threadsListening=0}</strong> so the parent
 * does not create a {@code ThreadPoolExecutor}.  With a pool active the path becomes
 * {@code socket → thread pool → Disruptor → listener}, adding an unnecessary hop and
 * defeating the latency goal.
 *
 * <h2>Allocation profile (steady-state hot path)</h2>
 * <ul>
 *   <li>Per-topic {@link ZeroMqConfiguration} objects are cached in
 *       {@link #topicConfigCache}; after the first message per topic no allocation
 *       occurs in {@link #onUpdate(TypeMessage, Object, String, long)}.
 *   <li>The Disruptor slot itself is pre-allocated – only four references are written
 *       per publish.
 * </ul>
 *
 * <h2>Wait strategies</h2>
 * <ul>
 *   <li>{@link com.lambda.investing.Configuration.ConnectorProviderType#DISRUPTOR_LOW_LATENCY}
 *       – BusySpin; lowest latency, dedicates one core to the consumer thread.</li>
 *   <li>{@link com.lambda.investing.Configuration.ConnectorProviderType#DISRUPTOR_HIGH_THROUGHPUT}
 *       – Yielding; lower CPU at slightly higher latency.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <pre>{@code
 * ZeroMqProviderDisruptor provider = new ZeroMqProviderDisruptor(cfg, 0, false);
 * provider.register(cfg, myListener);
 * provider.start();   // initialises Disruptor + warmup, then starts ZeroMqThreadReceiver
 * }</pre>
 */
public class ZeroMqProviderDisruptor extends ZeroMqProvider {

    private static final Logger logger = LogManager.getLogger(ZeroMqProviderDisruptor.class);

    /**
     * Keyed ONLY by the {@link ZeroMqConfiguration} (whose {@code equals}/{@code hashCode} are
     * based on host+port+protocol – see {@link ZeroMqConfiguration#equals}), NOT by
     * {@code threadsListening}/{@code isServer}/{@code connectorProviderType}.
     * <p>
     * A physical ZeroMq endpoint (host:port) must have exactly ONE socket / receiver thread.
     * Previously the cache key also included {@code threadsListening}, so two call sites
     * requesting the same endpoint with different {@code threadsListening} values (e.g.
     * {@code ZeroMqMarketDataConnector} with {@code threadsListening=1} and
     * {@code ZeroMqTradingEngineConnector} with {@code threadsListening=0}, both listening on
     * the shared {@code marketDataAndERconnectorConfiguration}) produced two distinct
     * {@code ZeroMqProviderDisruptor} instances, each opening its own SUB socket and
     * {@code ZeroMqThreadReceiver} thread (visible as two threads named
     * {@code "zeroMqThreadReceiverThreadName (0)-> host:port"} and
     * {@code "zeroMqThreadReceiverThreadName (1)-> host:port"}), double-processing every message.
     */
    private static final Map<ZeroMqConfiguration, ZeroMqProviderDisruptor> INSTANCES = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Factory methods
    // -----------------------------------------------------------------------

    /**
     * Returns a cached or new instance for the given configuration.
     * Key is the {@link ZeroMqConfiguration} itself (host+port+protocol identity) so that
     * every caller asking for the same physical endpoint reuses the same socket/receiver
     * thread, regardless of the {@code threadsListening}/{@code isServer}/
     * {@code connectorProviderType} values passed by later callers (first caller wins; a
     * warning is logged if a later call requests different parameters).
     */
    public static ZeroMqProviderDisruptor getInstance(ZeroMqConfiguration zeroMqConfiguration,
                                                      int threadsListening,
                                                      boolean isServer,
                                                      Configuration.ConnectorProviderType connectorProviderType) {
        ZeroMqProviderDisruptor output = INSTANCES.computeIfAbsent(zeroMqConfiguration,
                k -> new ZeroMqProviderDisruptor(zeroMqConfiguration, threadsListening, isServer, connectorProviderType));

        boolean differentParams = output.threadsListening != threadsListening
                || output.isServerFlag() != isServer
                || output.connectorProviderType != connectorProviderType;
        if (differentParams) {
            logger.warn(
                    "Reusing existing ZeroMqProviderDisruptor for {} created with (threadsListening={}, isServer={}, connectorProviderType={}) "
                            + "– ignoring differing parameters requested here (threadsListening={}, isServer={}, connectorProviderType={}) "
                            + "to avoid opening a second socket/receiver thread for the same endpoint",
                    zeroMqConfiguration, output.threadsListening, output.isServerFlag(), output.connectorProviderType,
                    threadsListening, isServer, connectorProviderType);
        }
        return output;
    }

    /**
     * Returns a cached or new instance defaulting to
     * {@link Configuration.ConnectorProviderType#DISRUPTOR_LOW_LATENCY}.
     */
    public static ZeroMqProviderDisruptor getInstance(ZeroMqConfiguration zeroMqConfiguration,
                                                      int threadsListening,
                                                      boolean isServer) {
        return getInstance(zeroMqConfiguration, threadsListening, isServer,
                Configuration.ConnectorProviderType.DISRUPTOR_LOW_LATENCY);
    }


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

    /**
     * Maps each registered {@link ConnectorListener} to its corresponding
     * ring-buffer {@link DisruptorConnectorHelper.EventConsumer} so it can be
     * removed on {@link #deregister}.
     */
    private final ConcurrentHashMap<ConnectorListener, DisruptorConnectorHelper.EventConsumer> listenerConsumers =
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
    protected ZeroMqProviderDisruptor(ZeroMqConfiguration zeroMqConfiguration,
                                   int threadsListening,
                                   boolean isServer,
                                   Configuration.ConnectorProviderType connectorProviderType) {
        super(zeroMqConfiguration, threadsListening);
        setServer(isServer);
        this.disruptorThreadName = "ZeroMqProviderDisruptor-" + zeroMqConfiguration.getPort();
        this.connectorProviderType = connectorProviderType;
    }

    protected void initializeThreadPool(int threadsListening) {
//        initDisruptor();
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
     * <p>Any listeners registered <em>before</em> {@link #start()} are wired to
     * the ring buffer here.  Listeners registered after {@link #start()} are
     * added immediately inside {@link #register}.
     */
    protected void initDisruptor() {
        helper = DisruptorConnectorHelper.getInstance(disruptorThreadName, connectorProviderType);
        helper.init();
        // Wire every listener that was registered before start() was called.
        for (DisruptorConnectorHelper.EventConsumer consumer : listenerConsumers.values()) {
            helper.addConsumer(consumer);
        }
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
     *
     * <p><strong>Important:</strong> this overrides the 2-argument form so that both
     * {@code start()} and {@code start(true, true)} (as called by
     * {@link com.lambda.investing.market_data_connector.ZeroMqMarketDataConnector#start()})
     * go through the same initialisation code path.  The previous no-arg override was
     * silently bypassed whenever callers used {@code start(boolean, boolean)} directly,
     * leaving {@code topicListSubscribed} empty and the Disruptor uninitialised.
     */
    @Override
    public void start(boolean hardTopicFilter, boolean sendAck) {
        if (helper != null && helper.isReady()) {
            logger.debug("ZeroMqProviderDisruptor already started for {} – skipping duplicate start",
                    disruptorThreadName);
            return;
        }
        initDisruptor();
        String topic = zeroMqConfiguration.getTopic();
        if (topic == null) {
            topic = "";
        }
        subscribeTopic(topic);

        super.start(hardTopicFilter, sendAck);
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
    // Consumer – per-listener ring-buffer consumers
    // -----------------------------------------------------------------------

    /**
     * Builds a ring-buffer {@link DisruptorConnectorHelper.EventConsumer} that
     * routes events to a single {@link ConnectorListener}.
     *
     * <ul>
     *   <li>Warmup sentinel events are silently discarded.</li>
     *   <li>The topic embedded in the incoming {@link ZeroMqConfiguration} is
     *       forwarded to the listener so it receives the same enriched
     *       configuration that {@link ZeroMqProvider#onUpdate} would provide.</li>
     * </ul>
     */
    private DisruptorConnectorHelper.EventConsumer createConsumerFor(ConnectorListener listener) {
        return (cfg, timestampReceived, typeMessage, content) -> {
            // Discard warmup sentinel events.
            if (cfg instanceof ZeroMqConfiguration zmqCfg
                    && DisruptorConnectorHelper.WARMUP_INSTRUMENT.equals(zmqCfg.getTopic())) {
                return;
            }
            try {
                listener.onUpdate(cfg, timestampReceived, typeMessage, content);
            } catch (Exception e) {
                logger.error("[{}] Listener error (typeMessage={})", disruptorThreadName, typeMessage, e);
            }
        };
    }

    // -----------------------------------------------------------------------
    // ConnectorProvider – register / deregister
    // -----------------------------------------------------------------------

    /**
     * Registers {@code listener} with the parent listener map <em>and</em> adds a
     * dedicated ring-buffer consumer so events flow directly from the Disruptor
     * consumer thread to this listener without an intermediate fan-out step.
     *
     * <p>If the Disruptor has not been initialised yet (i.e. {@link #start()} has
     * not been called), the consumer is queued in {@link #listenerConsumers} and
     * wired to the ring buffer during {@link #initDisruptor()}.
     */
    @Override
    public void register(ConnectorConfiguration configuration, ConnectorListener listener) {
        super.register(configuration, listener);
        DisruptorConnectorHelper.EventConsumer consumer = createConsumerFor(listener);
        listenerConsumers.put(listener, consumer);
        if (helper != null && helper.isReady()) {
            helper.addConsumer(consumer);
        }
        // If helper is not yet ready, initDisruptor() will call helper.addConsumer for all
        // pending entries in listenerConsumers.
    }

    /**
     * Deregisters {@code listener} from both the parent listener map and the
     * ring-buffer consumer list so no further events are delivered to it.
     */
    @Override
    public void deregister(ConnectorConfiguration configuration, ConnectorListener listener) {
        super.deregister(configuration, listener);
        DisruptorConnectorHelper.EventConsumer consumer = listenerConsumers.remove(listener);
        if (consumer != null && helper != null) {
            helper.removeConsumer(consumer);
        }
    }
}
