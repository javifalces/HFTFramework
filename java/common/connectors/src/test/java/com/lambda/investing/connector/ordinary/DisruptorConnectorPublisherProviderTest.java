package com.lambda.investing.connector.ordinary;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.*;
import com.lambda.investing.connector.disruptor.DisruptorConnectorConfiguration;
import com.lambda.investing.connector.disruptor.DisruptorConnectorPublisherProvider;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.Verb;
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
                ConnectorPublisherProviderFactory.createOrdinary("test-ordinary", 1, DEFAULT_PRIORITY);
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
                ConnectorPublisherProviderFactory.createOrdinary("ordinary-cmp", 1, DEFAULT_PRIORITY);
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

    /**
     * Verifies that both DisruptorConnectorPublisherProvider and OrdinaryConnectorPublisherProvider
     * deliver a Depth and a Trade object that are equal to the ones that were sent.
     */
    @Test
    void testDisruptorAndOrdinaryProduceSameDepthAndTrade() throws InterruptedException {
        // Build a Depth with 1 level
        Depth sentDepth = Depth.getInstance();
        sentDepth.setInstrument("BTCUSDT");
        sentDepth.setTimestamp(1_000_000L);
        sentDepth.setBids(new double[]{100.0});
        sentDepth.setAsks(new double[]{101.0});
        sentDepth.setBidsQuantities(new double[]{10.0});
        sentDepth.setAsksQuantities(new double[]{20.0});
        sentDepth.setLevels(1);
        sentDepth.setBidLevels(1);
        sentDepth.setAskLevels(1);

        // Build a Trade
        Trade sentTrade = Trade.getInstance();
        sentTrade.setInstrument("BTCUSDT");
        sentTrade.setTimestamp(2_000_000L);
        sentTrade.setPrice(100.5);
        sentTrade.setQuantity(5.0);
        sentTrade.setVerb(Verb.Buy);

        // 2 messages per provider (Depth + Trade)
        int expectedMessages = 2;

        AbstractConnectorPublisherProvider ordinary =
                ConnectorPublisherProviderFactory.createOrdinary("ordinary-depth-trade", 1, DEFAULT_PRIORITY);
        DisruptorConnectorPublisherProvider disruptor =
                new DisruptorConnectorPublisherProvider("disruptor-depth-trade", 512, new BusySpinWaitStrategy(), ProducerType.SINGLE);

        RecordingListener ordinaryListener = new RecordingListener(expectedMessages);
        RecordingListener disruptorListener = new RecordingListener(expectedMessages);

        ordinary.register(ordinaryConfig, ordinaryListener);
        disruptor.register(disruptorConfig, disruptorListener);

        ordinary.publish(ordinaryConfig, TypeMessage.depth, TOPIC, sentDepth);
        ordinary.publish(ordinaryConfig, TypeMessage.trade, TOPIC, sentTrade);
        disruptor.publish(disruptorConfig, TypeMessage.depth, TOPIC, sentDepth);
        disruptor.publish(disruptorConfig, TypeMessage.trade, TOPIC, sentTrade);

        assertTrue(ordinaryListener.awaitMessages(5), "Ordinary provider did not deliver Depth+Trade in time");
        assertTrue(disruptorListener.awaitMessages(5), "Disruptor provider did not deliver Depth+Trade in time");

        assertEquals(expectedMessages, ordinaryListener.received.size());
        assertEquals(expectedMessages, disruptorListener.received.size());

        // Extract Depth and Trade from each listener's received list
        Depth ordinaryDepth = ordinaryListener.received.stream()
                .filter(o -> o instanceof Depth).map(o -> (Depth) o).findFirst().orElse(null);
        Trade ordinaryTrade = ordinaryListener.received.stream()
                .filter(o -> o instanceof Trade).map(o -> (Trade) o).findFirst().orElse(null);
        Depth disruptorDepth = disruptorListener.received.stream()
                .filter(o -> o instanceof Depth).map(o -> (Depth) o).findFirst().orElse(null);
        Trade disruptorTrade = disruptorListener.received.stream()
                .filter(o -> o instanceof Trade).map(o -> (Trade) o).findFirst().orElse(null);

        assertNotNull(ordinaryDepth, "Ordinary listener did not receive a Depth");
        assertNotNull(ordinaryTrade, "Ordinary listener did not receive a Trade");
        assertNotNull(disruptorDepth, "Disruptor listener did not receive a Depth");
        assertNotNull(disruptorTrade, "Disruptor listener did not receive a Trade");

        // Depth equality
        assertTrue(sentDepth.equalsContent(ordinaryDepth),
                "Ordinary Depth does not match the sent Depth");
        assertTrue(sentDepth.equalsContent(disruptorDepth),
                "Disruptor Depth does not match the sent Depth");
        assertTrue(ordinaryDepth.equalsContent(disruptorDepth),
                "Ordinary and Disruptor Depth objects are not equal");

        // Trade equality (compare key fields since Trade has no custom equals)
        assertEquals(sentTrade.getId(), ordinaryTrade.getId());
        assertEquals(sentTrade.getId(), disruptorTrade.getId());
        assertEquals(sentTrade.getInstrument(), ordinaryTrade.getInstrument());
        assertEquals(sentTrade.getInstrument(), disruptorTrade.getInstrument());
        assertEquals(sentTrade.getPrice(), ordinaryTrade.getPrice(), 1e-9);
        assertEquals(sentTrade.getPrice(), disruptorTrade.getPrice(), 1e-9);
        assertEquals(sentTrade.getQuantity(), ordinaryTrade.getQuantity(), 1e-9);
        assertEquals(sentTrade.getQuantity(), disruptorTrade.getQuantity(), 1e-9);
        assertEquals(sentTrade.getVerb(), ordinaryTrade.getVerb());
        assertEquals(sentTrade.getVerb(), disruptorTrade.getVerb());
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
