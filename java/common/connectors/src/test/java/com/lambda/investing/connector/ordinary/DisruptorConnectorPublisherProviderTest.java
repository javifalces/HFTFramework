package com.lambda.investing.connector.ordinary;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.*;
import com.lambda.investing.connector.disruptor.DisruptorConnectorConfiguration;
import com.lambda.investing.connector.disruptor.DisruptorConnectorPublisherProvider;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.lambda.investing.connector.ConnectorPublisherProviderFactory.DEFAULT_PRIORITY;
import static org.junit.jupiter.api.Assertions.*;

class DisruptorConnectorPublisherProviderTest {

    private static final int MESSAGES = 10;
    private static final String TOPIC = "test-topic";

    private AbstractConnectorPublisherConfiguration ordinaryConfig;
    private AbstractConnectorPublisherConfiguration disruptorConfig;

    @BeforeEach
    void setUp() {
        ordinaryConfig = new OrdinaryConnectorConfiguration();
        disruptorConfig = new DisruptorConnectorConfiguration();
    }

    /**
     * Simple listener that records the messages received.
     */
    static class RecordingListener implements ConnectorListener {
        final List<Object> received = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch;

        RecordingListener(int expected) {
            latch = new CountDownLatch(expected);
        }

        @Override
        public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
                             TypeMessage typeMessage, Object content) {
            received.add(content);
            latch.countDown();
        }

        boolean awaitMessages(long timeoutSeconds) throws InterruptedException {
            return latch.await(timeoutSeconds, TimeUnit.SECONDS);
        }
    }

    @Test
    void testDisruptorDeliversAllMessages() throws InterruptedException {
        DisruptorConnectorPublisherProvider provider =
                new DisruptorConnectorPublisherProvider("test", 512, new BusySpinWaitStrategy(), ProducerType.SINGLE);
        RecordingListener listener = new RecordingListener(MESSAGES);
        provider.register(disruptorConfig, listener);

        for (int i = 0; i < MESSAGES; i++) {
            provider.publish(disruptorConfig, TypeMessage.depth, TOPIC, "msg-" + i);
        }

        assertTrue(listener.awaitMessages(5), "Not all messages were delivered in time");
        assertEquals(MESSAGES, listener.received.size());
    }

    @Test
    void testDisruptorCountsMessagesSent() throws InterruptedException {
        DisruptorConnectorPublisherProvider provider =
                new DisruptorConnectorPublisherProvider("test-count", 512, new BusySpinWaitStrategy(), ProducerType.SINGLE);
        RecordingListener listener = new RecordingListener(MESSAGES);
        provider.register(disruptorConfig, listener);

        for (int i = 0; i < MESSAGES; i++) {
            provider.publish(disruptorConfig, TypeMessage.depth, TOPIC, "msg-" + i);
        }

        assertTrue(listener.awaitMessages(5));
        assertEquals(MESSAGES, provider.getMessagesSent(disruptorConfig));
        assertEquals(0, provider.getMessagesFailed(disruptorConfig));
    }

    @Test
    void testDisruptorPublishReturnTrue() {
        DisruptorConnectorPublisherProvider provider =
                new DisruptorConnectorPublisherProvider("test-return", 512, new BusySpinWaitStrategy(), ProducerType.SINGLE);
        RecordingListener listener = new RecordingListener(1);
        provider.register(disruptorConfig, listener);

        boolean result = provider.publish(disruptorConfig, TypeMessage.depth, TOPIC, "hello");
        assertTrue(result);
    }

    @Test
    void testOrdinaryDeliversAllMessages() throws InterruptedException {
        AbstractConnectorPublisherProvider provider =
                ConnectorPublisherProviderFactory.createConnectorPublisherProvider(Configuration.CONNECTOR_PUBLISHER_PROVIDER, "test-ordinary", 1, DEFAULT_PRIORITY);
        RecordingListener listener = new RecordingListener(MESSAGES);
        provider.register(ordinaryConfig, listener);

        for (int i = 0; i < MESSAGES; i++) {
            provider.publish(ordinaryConfig, TypeMessage.depth, TOPIC, "msg-" + i);
        }

        assertTrue(listener.awaitMessages(5), "Not all messages were delivered in time");
        assertEquals(MESSAGES, listener.received.size());
    }

    /**
     * Verifies that DisruptorConnectorPublisherProvider delivers the same messages
     * and the same number of messages as OrdinaryConnectorPublisherProvider.
     */
    @Test
    void testDisruptorAndOrdinaryProduceSameResults() throws InterruptedException {
        AbstractConnectorPublisherProvider ordinary =
                ConnectorPublisherProviderFactory.createConnectorPublisherProvider(Configuration.CONNECTOR_PUBLISHER_PROVIDER, "ordinary-cmp", 1, DEFAULT_PRIORITY);
        DisruptorConnectorPublisherProvider disruptor =
                new DisruptorConnectorPublisherProvider("disruptor-cmp", 512, new BusySpinWaitStrategy(), ProducerType.SINGLE);

        RecordingListener ordinaryListener = new RecordingListener(MESSAGES);
        RecordingListener disruptorListener = new RecordingListener(MESSAGES);

        ordinary.register(ordinaryConfig, ordinaryListener);
        disruptor.register(disruptorConfig, disruptorListener);

        List<String> sentMessages = new ArrayList<>();
        for (int i = 0; i < MESSAGES; i++) {
            String msg = "shared-msg-" + i;
            sentMessages.add(msg);
            ordinary.publish(ordinaryConfig, TypeMessage.depth, TOPIC, msg);
            disruptor.publish(disruptorConfig, TypeMessage.depth, TOPIC, msg);
        }

        assertTrue(ordinaryListener.awaitMessages(5), "Ordinary provider did not deliver all messages in time");
        assertTrue(disruptorListener.awaitMessages(5), "Disruptor provider did not deliver all messages in time");

        assertEquals(MESSAGES, ordinaryListener.received.size());
        assertEquals(MESSAGES, disruptorListener.received.size());

        assertEquals(ordinary.getMessagesSent(ordinaryConfig), disruptor.getMessagesSent(disruptorConfig));
        assertEquals(ordinary.getMessagesFailed(ordinaryConfig), disruptor.getMessagesFailed(disruptorConfig));

        // Verify same message content (order may differ, so compare sorted sets)
        List<String> ordinaryMessages = new ArrayList<>();
        for (Object m : ordinaryListener.received) {
            ordinaryMessages.add((String) m);
        }
        List<String> disruptorMessages = new ArrayList<>();
        for (Object m : disruptorListener.received) {
            disruptorMessages.add((String) m);
        }
        Collections.sort(ordinaryMessages);
        Collections.sort(disruptorMessages);
        assertEquals(ordinaryMessages, disruptorMessages);
    }

    @Test
    void testDisruptorDeregister() throws InterruptedException {
        DisruptorConnectorPublisherProvider provider =
                new DisruptorConnectorPublisherProvider("test-deregister", 512, new BusySpinWaitStrategy(), ProducerType.SINGLE);
        RecordingListener listener = new RecordingListener(MESSAGES);
        provider.register(disruptorConfig, listener);

        // Publish half the messages
        for (int i = 0; i < MESSAGES / 2; i++) {
            provider.publish(disruptorConfig, TypeMessage.depth, TOPIC, "msg-" + i);
        }
        assertTrue(listener.awaitMessages(5));

        // Deregister and publish more
        provider.deregister(disruptorConfig, listener);
        for (int i = MESSAGES / 2; i < MESSAGES; i++) {
            provider.publish(disruptorConfig, TypeMessage.depth, TOPIC, "msg-" + i);
        }

        // The original listener should NOT receive more messages
        assertEquals(MESSAGES / 2, listener.received.size());
    }

    @Test
    void testDisruptorConfigEquality() {
        DisruptorConnectorConfiguration config1 = new DisruptorConnectorConfiguration();
        DisruptorConnectorConfiguration config2 = new DisruptorConnectorConfiguration();
        // Intentionally always equal (same pattern as OrdinaryConnectorConfiguration)
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
        assertNotNull(config1.getConnectionConfiguration());
        // Different configuration types are NOT considered equal to each other
        OrdinaryConnectorConfiguration ordinaryConfig = new OrdinaryConnectorConfiguration();
        assertNotEquals(config1.getClass(), ordinaryConfig.getClass());
    }
}
