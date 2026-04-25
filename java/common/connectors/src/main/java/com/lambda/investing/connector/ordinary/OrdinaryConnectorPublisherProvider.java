package com.lambda.investing.connector.ordinary;

import com.lambda.investing.LambdaThreadFactory;
import com.lambda.investing.connector.AbstractConnectorPublisherProvider;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.model.messaging.TypeMessage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class OrdinaryConnectorPublisherProvider extends AbstractConnectorPublisherProvider {

    ThreadPoolExecutor senderPool;

    private int publishThreads;

    private Map<TypeMessage, ThreadPoolExecutor> typeOfMessageToThreads = new HashMap<>();

    /**
     * @param name            name of the threadpool
     * @param publishThreads  number of publishThreads that publish to register ConnectorListeners lower than 1 is going to cached
     * @param publishPriority publishPriority of the thread pool
     */
    public OrdinaryConnectorPublisherProvider(String name, int publishThreads, Integer publishPriority) {
        super();
        this.name = name;
        initSenderPool(publishThreads, publishPriority);
    }

    protected void initSenderPool(int publishThreads, Integer priority) {
        this.publishThreads = publishThreads;
        namedThreadFactory = LambdaThreadFactory.createThreadFactory(this.name, priority);
        if (this.publishThreads < 0) {
            senderPool = (ThreadPoolExecutor) Executors.newCachedThreadPool(namedThreadFactory);
        }
        if (this.publishThreads > 0) {
            senderPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.publishThreads, namedThreadFactory);
        }
    }

    /**
     * Can route to a different ThreadPoolExecutor depending on TypeMessage
     *
     * @param typeOfMessageToThreads map with routing table
     */
    public void setRoutingPool(Map<TypeMessage, ThreadPoolExecutor> typeOfMessageToThreads) {
        this.typeOfMessageToThreads = typeOfMessageToThreads;
    }

    @Override
    public boolean publish(ConnectorConfiguration connectorConfiguration, TypeMessage typeMessage,
                           String topic, Serializable message) {
        Map<ConnectorListener, String> listeners = listenerManager
                .getOrDefault(connectorConfiguration, new HashMap<>());

        ThreadPoolExecutor threadPoolExecutor = typeOfMessageToThreads.getOrDefault(typeMessage, this.senderPool);
        if (threadPoolExecutor == null || publishThreads == 0) {
            _notify(connectorConfiguration, typeMessage, topic, message, listeners.keySet());
        } else {
            threadPoolExecutor.submit(() ->
                    _notify(connectorConfiguration, typeMessage, topic, message, listeners.keySet()));
        }
        return true;
    }
}
