package com.lambda.investing.trading_engine_connector;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.disruptor.DisruptorConnectorHelper;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Low-latency variant of {@link ZeroMqTradingEngineConnector} that interposes
 * an LMAX Disruptor ring-buffer between the ZeroMq I/O thread and the execution
 * report / info update business logic.
 *
 * <h2>Architecture</h2>
 * <pre>
 *  ZeroMq I/O thread
 *    └─ onUpdate()  ──▶  DisruptorConnectorHelper.publish()  (ring-buffer write, ~50 ns)
 *                            │
 *                            ▼  [zmq-te-disruptor thread]
 *                        dispatchToParent()
 *                            │
 *                            ▼
 *                        AbstractTradingEngineConnector.onUpdate()
 *                            ├─ notifyExecutionReport()
 *                            └─ notifyInfo()
 * </pre>
 *
 * <p>The shared ring-buffer infrastructure ({@link DisruptorConnectorHelper}) is
 * identical to the one used by
 * {@link com.lambda.investing.market_data_connector.ZeroMqMarketDataConnectorDisruptor}.
 *
 * <h2>Lifecycle</h2>
 * <pre>{@code
 * ZeroMqTradingEngineConnectorDisruptor connector = new ZeroMqTradingEngineConnectorDisruptor(...);
 * connector.start();          // initialises Disruptor + warmup, then starts ZeroMq listener
 * connector.register(...);    // register listeners AFTER start() returns
 * }</pre>
 *
 * <h2>Stopping</h2>
 * Call {@link #stop()} to drain remaining events and shut down the consumer thread.
 */
public class ZeroMqTradingEngineConnectorDisruptor extends ZeroMqTradingEngineConnector {

    private static final Logger logger =
            LogManager.getLogger(ZeroMqTradingEngineConnectorDisruptor.class);


    // -----------------------------------------------------------------------
    // Disruptor – delegated to the shared helper
    // -----------------------------------------------------------------------

    private DisruptorConnectorHelper helper;
    private String disruptorThreadName;
    private Configuration.ConnectorProviderType connectorProviderType;

    // -----------------------------------------------------------------------
    // Constructor – mirrors the parent constructor
    // -----------------------------------------------------------------------

    /**
     * @param name                                        Connector / algorithm name.
     * @param threadsPublish                              ZeroMq publisher thread count.
     * @param threadsListen                               ZeroMq listener thread count.
     * @param zeroMqConfigurationExecutionReportListening ZeroMq config for inbound ER.
     * @param zeroMqConfigurationOrderRequest             ZeroMq config for outbound orders.
     */
    public ZeroMqTradingEngineConnectorDisruptor(
            String name,
            int threadsPublish,
            int threadsListen,
            ZeroMqConfiguration zeroMqConfigurationExecutionReportListening,
            ZeroMqConfiguration zeroMqConfigurationOrderRequest,
            Configuration.ConnectorProviderType connectorProviderType
    ) {
        super(name, threadsPublish, threadsListen,
                zeroMqConfigurationExecutionReportListening,
                zeroMqConfigurationOrderRequest);
        disruptorThreadName = zeroMqConfigurationExecutionReportListening.toString();
        this.connectorProviderType = connectorProviderType;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Initialises the LMAX Disruptor ring-buffer + consumer thread, then runs
     * the startup warmup cycle.
     *
     * <p>Extracted so that test fixtures can start the Disruptor without also
     * bringing up the ZeroMq provider:
     * <pre>{@code
     * @Override public void start() { initDisruptor(); }
     * }</pre>
     */
    protected void initDisruptor() {
        helper = DisruptorConnectorHelper.getInstance(disruptorThreadName, connectorProviderType);
        helper.init();
        helper.addConsumer(this::dispatchToParent,
                TypeMessage.execution_report, TypeMessage.info);
        warmupDisruptor();
    }

    /**
     * Primes the JIT and pre-touches ring-buffer memory by publishing synthetic
     * {@link TypeMessage#execution_report} events through the full pipeline.
     * Blocks until all warmup events are drained so that no event leaks to real
     * {@link ExecutionReportListener}s.
     *
     * <p>Override in tests or subclasses to skip warmup:
     * <pre>{@code @Override protected void warmupDisruptor() { } }</pre>
     */
    protected void warmupDisruptor() {
        helper.warmup(
                new ZeroMqConfiguration(DisruptorConnectorHelper.WARMUP_HOST,
                        DisruptorConnectorHelper.WARMUP_PORT,
                        DisruptorConnectorHelper.WARMUP_INSTRUMENT),
                TypeMessage.execution_report,
                this::buildWarmupExecutionReport
        );
    }

    /**
     * Starts the Disruptor consumer thread <em>before</em> the ZeroMq provider
     * so the ring buffer is ready as soon as the first execution report arrives.
     */
    @Override
    public void start() {
        initDisruptor();
        // Registers ZeroMq ER listener and starts the provider
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
    public void onUpdate(ConnectorConfiguration configuration,
                         long timestampReceived,
                         TypeMessage typeMessage,
                         Object content) {
        if (helper == null || !helper.isReady()) {
            // Fallback to synchronous processing before Disruptor is initialised.
            super.onUpdate(configuration, timestampReceived, typeMessage, content);
            return;
        }
        helper.publish(configuration, timestampReceived, typeMessage, content);
    }

    // -----------------------------------------------------------------------
    // Consumer – dedicated zmq-te-disruptor thread
    // -----------------------------------------------------------------------

    /**
     * Bridge to {@link AbstractTradingEngineConnector#onUpdate} invoked on the
     * consumer thread.  A private method is required because {@code super.method}
     * cannot be referenced directly inside a lambda.
     */
    private void dispatchToParent(ConnectorConfiguration configuration,
                                  long timestampReceived,
                                  TypeMessage typeMessage,
                                  Object content) {
        super.onUpdate(configuration, timestampReceived, typeMessage, content);
    }

    // -----------------------------------------------------------------------
    // Warmup content builder
    // -----------------------------------------------------------------------

    /**
     * Builds a minimal synthetic {@link ExecutionReport} for the warmup cycle.
     * Uses {@link ExecutionReportStatus#Active} so it does not get added to the
     * CF-trade deduplication list.
     * The sentinel {@link DisruptorConnectorHelper#WARMUP_INSTRUMENT} tag means
     * no registered listener will ever match this event even if listeners are
     * registered before {@link #start()} returns.
     */
    private ExecutionReport buildWarmupExecutionReport(int i) {
        ExecutionReport er = new ExecutionReport();
        er.setAlgorithmInfo(DisruptorConnectorHelper.WARMUP_INSTRUMENT);
        er.setInstrument(DisruptorConnectorHelper.WARMUP_INSTRUMENT);
        er.setClientOrderId("warmup_" + i);
        er.setExecutionReportStatus(ExecutionReportStatus.Active);
        er.setTimestampCreation(System.currentTimeMillis());
        return er;
    }
}

