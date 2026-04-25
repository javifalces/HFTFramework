package com.lambda.investing.connector.disruptor;


import com.lambda.investing.LambdaThreadFactory;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.lmax.disruptor.dsl.Disruptor;

public class DisruptorConnectorPublisherProvider implements ConnectorPublisher, ConnectorProvider, EventHandler<DisruptorConnectorPublisherProvider.DisruptorMessageObject> {


    @NoArgsConstructor
    @Getter
    @Setter
    public static class DisruptorMessageObject {
        ConnectorConfiguration connectorConfiguration;
        TypeMessage typeMessage;
        String topic;
        Serializable message;
    }

    private Map<ConnectorConfiguration, Map<ConnectorListener, String>> listenerManager;
    private Map<ConnectorConfiguration, AtomicInteger> counterMessagesSent;
    private Map<ConnectorConfiguration, AtomicInteger> counterMessagesNotSent;
    Logger logger = LogManager.getLogger(DisruptorConnectorPublisherProvider.class);
    ThreadFactory namedThreadFactory = LambdaThreadFactory.createThreadFactory("DisruptorConnectorPublisherProvider");

    private String name;

    protected Disruptor<DisruptorMessageObject> disruptor;
    protected RingBuffer<DisruptorMessageObject> ringBuffer;

    protected int sizeRing = 512;
    protected boolean isStart = false;
    protected WaitStrategy waitStrategy = new BlockingWaitStrategy();
    protected ProducerType producerType = ProducerType.MULTI;

    /**
     * https://www.baeldung.com/lmax-disruptor-concurrency
     * https://github.com/trevorbernard/disruptor-examples/tree/master/src/main/java/com/trevorbernard/disruptor/examples
     * https://stackoverflow.com/questions/44893194/why-is-disruptor-slower-with-smaller-ring-buffer
     * @param name         name of the threadpool
     * @param sizeRing     size of ring
     * @param waitStrategy disruptor wait strategy (e.g. BlockingWaitStrategy, BusySpinWaitStrategy) when the ring is full
     * @param producerType disruptor producer type (SINGLE lock-free, MULTI CAS)
     * ProducerType.MULTI : has a CAS protection system in ringBuffer next sequence
     * ProducerType.SINGLE : in lock free
     */
    public DisruptorConnectorPublisherProvider(String name, int sizeRing, WaitStrategy waitStrategy, ProducerType producerType) {
        listenerManager = new ConcurrentHashMap<>();
        counterMessagesSent = new ConcurrentHashMap<>();
        counterMessagesNotSent = new ConcurrentHashMap<>();

        isStart = false;
        this.name = name;
        this.sizeRing = sizeRing;
        this.waitStrategy = waitStrategy;
        this.producerType = producerType;

        namedThreadFactory = LambdaThreadFactory.createThreadFactory(this.name, Thread.NORM_PRIORITY);
        setDisruptor();
    }

    public DisruptorConnectorPublisherProvider(String name, Integer priority, int sizeRing, WaitStrategy waitStrategy, ProducerType producerType) {
        listenerManager = new ConcurrentHashMap<>();
        counterMessagesSent = new ConcurrentHashMap<>();
        counterMessagesNotSent = new ConcurrentHashMap<>();

        this.name = name;
        this.sizeRing = sizeRing;
        this.waitStrategy = waitStrategy;
        this.producerType = producerType;
        namedThreadFactory = LambdaThreadFactory.createThreadFactory(this.name, priority);
        setDisruptor();
    }
    private void setDisruptor() {
        disruptor = new Disruptor<>(DisruptorMessageObject::new,
                sizeRing,
                namedThreadFactory,
                producerType,
                waitStrategy);
        disruptor.handleEventsWith(this);


    }


    @Override
    public void register(ConnectorConfiguration configuration, ConnectorListener listener) {
        Map<ConnectorListener, String> listeners = listenerManager
                .getOrDefault(configuration, new ConcurrentHashMap<>());
        listeners.put(listener, "");
        listenerManager.put(configuration, listeners);
    }

    private void start() {
        ringBuffer = disruptor.start();
        isStart = true;
    }


    @Override
    public void onEvent(DisruptorMessageObject event, long sequence, boolean endOfBatch) {
        Map<ConnectorListener, String> listeners = listenerManager.getOrDefault(event.getConnectorConfiguration(), new ConcurrentHashMap<>());
        Set<ConnectorListener> listenersSet = listeners.keySet();
        _notify(event.getConnectorConfiguration(), event.getTypeMessage(), event.getTopic(), event.getMessage(), listenersSet);
    }


    @Override
    public void deregister(ConnectorConfiguration configuration, ConnectorListener listener) {
        Map<ConnectorListener, String> listeners = listenerManager
                .getOrDefault(configuration, new ConcurrentHashMap<>());
        listeners.remove(listener);
        listenerManager.put(configuration, listeners);
    }

    private void _notify(ConnectorConfiguration connectorConfiguration, TypeMessage typeMessage, String topic,
                         Object message, Set<ConnectorListener> listenerList) {
        boolean output = true;
        try {
            for (ConnectorListener listener : listenerList) {
                listener.onUpdate(connectorConfiguration, System.currentTimeMillis(), typeMessage, message);
            }
        } catch (Exception ex) {
            logger.error("error notifying {}:{} \n{} ", topic, message, ExceptionUtils.getStackTrace(ex), ex);
            output = false;
        }

        if (!counterMessagesSent.containsKey(connectorConfiguration)) {
            counterMessagesSent.put(connectorConfiguration, new AtomicInteger(0));
        }
        if (!counterMessagesNotSent.containsKey(connectorConfiguration)) {
            counterMessagesNotSent.put(connectorConfiguration, new AtomicInteger(0));
        }

        if (output) {
            AtomicInteger prevCount = counterMessagesSent.get(connectorConfiguration);
            prevCount.incrementAndGet();
            counterMessagesSent.put(connectorConfiguration, prevCount);
        } else {
            AtomicInteger prevCount = counterMessagesNotSent.get(connectorConfiguration);
            prevCount.incrementAndGet();
            counterMessagesNotSent.put(connectorConfiguration, prevCount);
        }

    }

    @Override
    public boolean publish(ConnectorConfiguration connectorConfiguration, TypeMessage typeMessage,
                           String topic, Serializable message) {
        if (!isStart) {
            start();
        }
        long sequenceId = ringBuffer.next();//if multi , is locked => get the space in the ringBuffer
        DisruptorMessageObject disruptorMessageObject = ringBuffer.get(sequenceId);
        disruptorMessageObject.setConnectorConfiguration(connectorConfiguration);
        disruptorMessageObject.setMessage(message);
        disruptorMessageObject.setTopic(topic);
        disruptorMessageObject.setTypeMessage(typeMessage);
        ringBuffer.publish(sequenceId);

        return true;
    }

    @Override
    public int getMessagesSent(ConnectorConfiguration configuration) {
        if (counterMessagesSent.containsKey(configuration)) {
            return counterMessagesSent.get(configuration).get();
        } else {
            return 0;
        }
    }

    @Override
    public int getMessagesFailed(ConnectorConfiguration configuration) {
        if (counterMessagesNotSent.containsKey(configuration)) {
            return counterMessagesNotSent.get(configuration).get();
        } else {
            return 0;
        }
    }

}
