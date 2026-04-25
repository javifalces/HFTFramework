package com.lambda.investing.connector;

import com.lambda.investing.LambdaThreadFactory;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class providing common state and behaviour shared by
 * {@link com.lambda.investing.connector.ordinary.OrdinaryConnectorPublisherProvider} and
 * {@link com.lambda.investing.connector.disruptor.DisruptorConnectorPublisherProvider}.
 */
public abstract class AbstractConnectorPublisherProvider implements ConnectorPublisher, ConnectorProvider {

    protected Map<ConnectorConfiguration, Map<ConnectorListener, String>> listenerManager;
    protected Map<ConnectorConfiguration, AtomicInteger> counterMessagesSent;
    protected Map<ConnectorConfiguration, AtomicInteger> counterMessagesNotSent;

    protected Logger logger = LogManager.getLogger(getClass());
    protected ThreadFactory namedThreadFactory = LambdaThreadFactory.createThreadFactory("AbstractConnectorPublisherProvider");

    protected String name;

    protected AbstractConnectorPublisherProvider() {
        listenerManager = new ConcurrentHashMap<>();
        counterMessagesSent = new ConcurrentHashMap<>();
        counterMessagesNotSent = new ConcurrentHashMap<>();
    }

    @Override
    public void register(ConnectorConfiguration configuration, ConnectorListener listener) {
        Map<ConnectorListener, String> listeners = listenerManager
                .getOrDefault(configuration, new ConcurrentHashMap<>());
        listeners.put(listener, "");
        listenerManager.put(configuration, listeners);
    }

    @Override
    public void deregister(ConnectorConfiguration configuration, ConnectorListener listener) {
        Map<ConnectorListener, String> listeners = listenerManager
                .getOrDefault(configuration, new ConcurrentHashMap<>());
        listeners.remove(listener);
        listenerManager.put(configuration, listeners);
    }

    protected void _notify(ConnectorConfiguration connectorConfiguration, TypeMessage typeMessage, String topic,
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
            counterMessagesSent.get(connectorConfiguration).incrementAndGet();
        } else {
            counterMessagesNotSent.get(connectorConfiguration).incrementAndGet();
        }
    }

    @Override
    public abstract boolean publish(ConnectorConfiguration connectorConfiguration, TypeMessage typeMessage,
                                    String topic, Serializable message);

    @Override
    public int getMessagesSent(ConnectorConfiguration configuration) {
        if (counterMessagesSent.containsKey(configuration)) {
            return counterMessagesSent.get(configuration).get();
        }
        return 0;
    }

    @Override
    public int getMessagesFailed(ConnectorConfiguration configuration) {
        if (counterMessagesNotSent.containsKey(configuration)) {
            return counterMessagesNotSent.get(configuration).get();
        }
        return 0;
    }
}

