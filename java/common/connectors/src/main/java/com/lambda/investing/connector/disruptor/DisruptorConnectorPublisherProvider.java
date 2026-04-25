package com.lambda.investing.connector.disruptor;

import com.lambda.investing.LambdaThreadFactory;
import com.lambda.investing.connector.AbstractConnectorPublisherProvider;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DisruptorConnectorPublisherProvider extends AbstractConnectorPublisherProvider
        implements EventHandler<DisruptorConnectorPublisherProvider.DisruptorMessageObject> {

    @NoArgsConstructor
    @Getter
    @Setter
    public static class DisruptorMessageObject {
        ConnectorConfiguration connectorConfiguration;
        TypeMessage typeMessage;
        String topic;
        Serializable message;
    }

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
     *
     * @param name         name of the threadpool
     * @param sizeRing     size of ring
     * @param waitStrategy disruptor wait strategy (e.g. BlockingWaitStrategy, BusySpinWaitStrategy) when the ring is full
     * @param producerType disruptor producer type (SINGLE lock-free, MULTI CAS)
     *                     ProducerType.MULTI : has a CAS protection system in ringBuffer next sequence
     *                     ProducerType.SINGLE : in lock free
     */
    public DisruptorConnectorPublisherProvider(String name, int sizeRing, WaitStrategy waitStrategy, ProducerType producerType) {
        super();
        isStart = false;
        this.name = name;
        this.sizeRing = sizeRing;
        this.waitStrategy = waitStrategy;
        this.producerType = producerType;
        namedThreadFactory = LambdaThreadFactory.createThreadFactory(this.name, Thread.NORM_PRIORITY);
        setDisruptor();
    }

    public DisruptorConnectorPublisherProvider(String name, Integer priority, int sizeRing, WaitStrategy waitStrategy, ProducerType producerType) {
        super();
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

    private void start() {
        ringBuffer = disruptor.start();
        isStart = true;
    }

    @Override
    public void onEvent(DisruptorMessageObject event, long sequence, boolean endOfBatch) {
        Map<ConnectorListener, String> listeners = listenerManager
                .getOrDefault(event.getConnectorConfiguration(), new ConcurrentHashMap<>());
        Set<ConnectorListener> listenersSet = listeners.keySet();
        _notify(event.getConnectorConfiguration(), event.getTypeMessage(), event.getTopic(), event.getMessage(), listenersSet);
    }

    @Override
    public boolean publish(ConnectorConfiguration connectorConfiguration, TypeMessage typeMessage,
                           String topic, Serializable message) {
        if (!isStart) {
            start();
        }
        long sequenceId = ringBuffer.next(); // if multi, is locked => get the space in the ringBuffer
        DisruptorMessageObject disruptorMessageObject = ringBuffer.get(sequenceId);
        disruptorMessageObject.setConnectorConfiguration(connectorConfiguration);
        disruptorMessageObject.setMessage(message);
        disruptorMessageObject.setTopic(topic);
        disruptorMessageObject.setTypeMessage(typeMessage);
        ringBuffer.publish(sequenceId);
        return true;
    }
}
