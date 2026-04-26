package com.lambda.investing.market_data_connector;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Low-latency variant of {@link ZeroMqMarketDataConnector} that interposes an
 * LMAX Disruptor ring-buffer between the ZeroMq I/O thread and the business
 * logic.
 *
 * <p>The ZeroMq callback ({@link #onUpdate}) is the <em>producer</em>: it only
 * writes four references into a pre-allocated ring-buffer slot and publishes the
 * sequence – typically tens of nanoseconds.  All decoding and listener
 * notification happens on the dedicated {@code zeromq-disruptor-consumer} thread
 * via {@link ZeroMqMarketDataConnector#processUpdate}.
 *
 * <p>Use {@code ZeroMqMarketDataConnectorFactory} to obtain an instance.
 */
public class ZeroMqMarketDataConnectorDisruptor extends ZeroMqMarketDataConnector {

    private static final Logger logger = LogManager.getLogger(ZeroMqMarketDataConnectorDisruptor.class);

    // -----------------------------------------------------------------------
    // Disruptor wiring
    // -----------------------------------------------------------------------

    /**
     * Pre-allocated ring-buffer slot.
     */
    private static final class DisruptorEvent {
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

    /**
     * Ring-buffer size – must be a power of two.
     * 1 024 slots covers typical ZeroMQ burst throughput; raise to 4 096 for
     * very high-frequency feeds.
     */
    private static final int RING_BUFFER_SIZE = Configuration.DISRUPTOR_RING_BUFFER_SIZE;

    private Disruptor<DisruptorEvent> disruptor;
    private RingBuffer<DisruptorEvent> ringBuffer;

    // -----------------------------------------------------------------------
    // Constructors – mirror the parent constructors
    // -----------------------------------------------------------------------

    public ZeroMqMarketDataConnectorDisruptor(ZeroMqConfiguration zeroMqConfiguration, int threadsListening) {
        super(zeroMqConfiguration, threadsListening);
    }

    public ZeroMqMarketDataConnectorDisruptor(ZeroMqConfiguration zeroMqConfigurationIn,
                                              List<Instrument> instruments,
                                              int threadsListening) {
        super(zeroMqConfigurationIn, instruments, threadsListening);
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Starts the Disruptor consumer thread <em>before</em> the ZeroMq provider
     * so the ring buffer is ready as soon as the first message arrives.
     */
    @Override
    public void start() {
        ThreadFactory consumerThreadFactory = r -> {
            Thread t = new Thread(r, "zeromq-disruptor-consumer");
            t.setDaemon(true);
            return t;
        };

        disruptor = new Disruptor<>(
                EVENT_FACTORY,
                RING_BUFFER_SIZE,
                consumerThreadFactory,
                ProducerType.SINGLE,        // ZeroMq callback is single-threaded
                new BusySpinWaitStrategy()  // lowest latency – spins one CPU core
        );

        disruptor.handleEventsWith(this::dispatchEvent);
        ringBuffer = disruptor.start();

        logger.info("Disruptor started (ringBufferSize={}, waitStrategy=BusySpin)", RING_BUFFER_SIZE);

        // Starts statisticsReceived reset + zeroMqProvider
        super.start();
    }

    /**
     * Drains any remaining events then shuts down the consumer thread.
     */
    public void stop() {
        if (disruptor != null) {
            disruptor.shutdown();
        }
    }

    // -----------------------------------------------------------------------
    // Producer – hot path (ZeroMq I/O thread)
    // -----------------------------------------------------------------------

    /**
     * Hot path: grabs the next ring-buffer sequence, copies four references
     * into the pre-allocated slot, and publishes.  No allocation, no business
     * logic.
     */
    @Override
    public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
                         TypeMessage typeMessage, Object content) {
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
    }

    // -----------------------------------------------------------------------
    // Consumer – dedicated zeromq-disruptor-consumer thread
    // -----------------------------------------------------------------------

    /**
     * Delegates to {@link ZeroMqMarketDataConnector#processUpdate} which holds
     * all decoding and listener-notification logic.
     */
    private void dispatchEvent(DisruptorEvent slot, long sequence, boolean endOfBatch) {
        try {
            processUpdate(slot.configuration, slot.timestampReceived, slot.typeMessage, slot.content);
        } catch (Exception e) {
            logger.error("Error in Disruptor consumer dispatching typeMessage={}", slot.typeMessage, e);
        } finally {
            slot.reset();
        }
    }
}


