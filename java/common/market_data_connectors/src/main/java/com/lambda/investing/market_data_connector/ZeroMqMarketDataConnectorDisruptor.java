package com.lambda.investing.market_data_connector;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.disruptor.DisruptorConnectorHelper;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Low-latency variant of {@link ZeroMqMarketDataConnector} that interposes an
 * LMAX Disruptor ring-buffer between the ZeroMq I/O thread and the business
 * logic.
 *
 * <p>The ZeroMq callback ({@link #onUpdate}) is the <em>producer</em>: it only
 * writes four references into a pre-allocated ring-buffer slot and publishes the
 * sequence – typically tens of nanoseconds.  All decoding and listener
 * notification happens on the dedicated {@code zmq-md-disruptor} thread via
 * {@link ZeroMqMarketDataConnector#processUpdate}.
 *
 * <p>The shared ring-buffer infrastructure lives in
 * {@link DisruptorConnectorHelper}; the identical helper is reused by
 * {@code ZeroMqTradingEngineConnectorDisruptor} in the trading-engine-connectors module.
 *
 * <p>Use {@code ZeroMqMarketDataConnectorFactory} to obtain an instance.
 */
public class ZeroMqMarketDataConnectorDisruptor extends ZeroMqMarketDataConnector {

    private static final Logger logger = LogManager.getLogger(ZeroMqMarketDataConnectorDisruptor.class);


    // -----------------------------------------------------------------------
    // Disruptor – delegated to the shared helper
    // -----------------------------------------------------------------------

    private DisruptorConnectorHelper helper;
    private String disruptorThreadName;
    private Configuration.ConnectorProviderType connectorProviderType;

    // -----------------------------------------------------------------------
    // Constructors – mirror the parent constructors
    // -----------------------------------------------------------------------

    public ZeroMqMarketDataConnectorDisruptor(ZeroMqConfiguration zeroMqConfiguration, int threadsListening, Configuration.ConnectorProviderType connectorProviderType) {
        super(zeroMqConfiguration, threadsListening);
        disruptorThreadName = zeroMqConfiguration.toString();
        this.connectorProviderType = connectorProviderType;
    }

    public ZeroMqMarketDataConnectorDisruptor(ZeroMqConfiguration zeroMqConfigurationIn,
                                              List<Instrument> instruments,
                                              int threadsListening, Configuration.ConnectorProviderType connectorProviderType) {
        super(zeroMqConfigurationIn, instruments, threadsListening);
        disruptorThreadName = zeroMqConfigurationIn.toString();
        this.connectorProviderType = connectorProviderType;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Initialises and starts the LMAX Disruptor ring-buffer and consumer thread,
     * then runs the warmup cycle.
     * Extracted so that subclasses (e.g. test fixtures) can start the Disruptor
     * without also bringing up the ZeroMq provider.
     */
    protected void initDisruptor() {
        helper = DisruptorConnectorHelper.getInstance(disruptorThreadName, connectorProviderType);
        helper.init();
        helper.addConsumer(this::processUpdate,
                TypeMessage.depth, TypeMessage.trade, TypeMessage.command);
        warmupDisruptor();
    }

    /**
     * Primes the JIT and pre-touches ring-buffer memory by publishing synthetic
     * {@link TypeMessage#depth} events through the full pipeline.
     * Blocks until all warmup events are drained so no event leaks to real listeners.
     *
     * <p>Override in tests or subclasses to skip warmup:
     * <pre>{@code @Override protected void warmupDisruptor() { } }</pre>
     */
    protected void warmupDisruptor() {
        helper.warmup(
                new ZeroMqConfiguration(DisruptorConnectorHelper.WARMUP_HOST,
                        DisruptorConnectorHelper.WARMUP_PORT,
                        DisruptorConnectorHelper.WARMUP_INSTRUMENT),
                TypeMessage.depth,
                this::buildWarmupDepth
        );
        helper.warmup(
                new ZeroMqConfiguration(DisruptorConnectorHelper.WARMUP_HOST,
                        DisruptorConnectorHelper.WARMUP_PORT,
                        DisruptorConnectorHelper.WARMUP_INSTRUMENT),
                TypeMessage.trade,
                this::buildWarmupTrade
        );
    }

    /**
     * Starts the Disruptor consumer thread <em>before</em> the ZeroMq provider
     * so the ring buffer is ready as soon as the first message arrives.
     */
    @Override
    public void start() {
        initDisruptor();
        // Starts statisticsReceived reset + zeroMqProvider
        super.start();
    }

    /**
     * Drains any remaining events then shuts down the consumer thread.
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
     * Hot path: delegates to {@link DisruptorConnectorHelper#publish} which
     * grabs the next ring-buffer sequence, copies four references into the
     * pre-allocated slot, and publishes.  No allocation, no business logic.
     */
    @Override
    public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
                         TypeMessage typeMessage, Object content) {
        if (helper == null || !helper.isReady()) {
            return;
        }
        helper.publish(configuration, timestampReceived, typeMessage, content);
    }

    // -----------------------------------------------------------------------
    // Warmup content builder
    // -----------------------------------------------------------------------

    /**
     * Builds a minimal synthetic {@link Depth} for the warmup cycle.
     * The sentinel instrument {@link DisruptorConnectorHelper#WARMUP_INSTRUMENT}
     * ensures these events are invisible to downstream strategies.
     */
    private Depth buildWarmupDepth(int i) {
        Depth depth = Depth.getInstancePool();
        depth.setInstrument(DisruptorConnectorHelper.WARMUP_INSTRUMENT);
        depth.setTimestamp(i + 1L);
        depth.setLevels(1);
        depth.setBids(new double[]{1.0});
        depth.setAsks(new double[]{1.01});
        depth.setBidsQuantities(new double[]{1.0});
        depth.setAsksQuantities(new double[]{1.0});
        depth.setLevelsFromData();
        return depth;
    }

    /**
     * Builds a minimal synthetic {@link Trade} for the warmup cycle.
     * The sentinel instrument {@link DisruptorConnectorHelper#WARMUP_INSTRUMENT}
     * ensures these events are invisible to downstream strategies.
     */
    private Trade buildWarmupTrade(int i) {
        Trade trade = Trade.getInstancePool();
        trade.setInstrument(DisruptorConnectorHelper.WARMUP_INSTRUMENT);
        trade.setTimestamp(i + 1L);
        trade.setPrice(1.0);
        trade.setQuantity(1.0);
        return trade;
    }
}
