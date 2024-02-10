package com.lambda.investing.connector.ordinary;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class OrdinaryConnectorPublisherProvider implements ConnectorPublisher, ConnectorProvider {

	private Map<ConnectorConfiguration, Map<ConnectorListener, String>> listenerManager;
	private Map<ConnectorConfiguration, AtomicInteger> counterMessagesSent;
	private Map<ConnectorConfiguration, AtomicInteger> counterMessagesNotSent;
	Logger logger = LogManager.getLogger(OrdinaryConnectorPublisherProvider.class);
	ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
			.setNameFormat("OrdinaryConnectorPublisherProvider -%d")
			.build();

	ThreadPoolExecutor senderPool;
	private Integer priority = null;


	private int publishThreads;
	private String name;

	private Map<TypeMessage, ThreadPoolExecutor> typeOfMessageToThreads = new HashMap<>();

	/**
	 * @param name            name of the threadpool
	 * @param publishThreads  number of publishThreads that publish to register ConnectorListeners <1 is going to cached
	 * @param publishPriority publishPriority of the thread pool
	 */
	public OrdinaryConnectorPublisherProvider(String name, int publishThreads, Integer publishPriority) {
		listenerManager = new HashMap<>();
		counterMessagesSent = new HashMap<>();
		counterMessagesNotSent = new HashMap<>();

		//thread pool name
		this.name = name;
		initSenderPool(publishThreads, publishPriority);
	}

	protected void initSenderPool(int publishThreads, Integer priority) {
		//TODO change it to maintain order by instrument
		this.publishThreads = publishThreads;
		ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
		threadFactoryBuilder.setNameFormat(this.name + " -%d").build();
		threadFactoryBuilder.setPriority(priority);
		namedThreadFactory = threadFactoryBuilder.build();
		if (this.publishThreads < 0) {
			//infinite
			senderPool = (ThreadPoolExecutor) Executors.newCachedThreadPool(namedThreadFactory);
		}
		if (this.publishThreads > 0) {
			senderPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.publishThreads, namedThreadFactory);
		}

	}

	/**
	 * Can route  to a different ThreadPoolExecutor depending of TypeMessage
	 *
	 * @param typeOfMessageToThreads map with routing table
	 */
	public void setRoutingPool(Map<TypeMessage, ThreadPoolExecutor> typeOfMessageToThreads) {
		this.typeOfMessageToThreads = typeOfMessageToThreads;
	}

	@Override
	public void register(ConnectorConfiguration configuration, ConnectorListener listener) {
		Map<ConnectorListener, String> listeners = listenerManager
				.getOrDefault(configuration, new ConcurrentHashMap<>());
		listeners.put(listener, "");
		listenerManager.put(configuration, listeners);

	}

	@Override public void deregister(ConnectorConfiguration configuration, ConnectorListener listener) {
		Map<ConnectorListener, String> listeners = listenerManager
				.getOrDefault(configuration, new ConcurrentHashMap<>());
		listeners.remove(listener);
		listenerManager.put(configuration, listeners);
	}

	private void _notify(ConnectorConfiguration connectorConfiguration, TypeMessage typeMessage, String topic,
						 String message, Set<ConnectorListener> listenerList) {
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

	@Override public boolean publish(ConnectorConfiguration connectorConfiguration, TypeMessage typeMessage,
									 String topic, String message) {
		Map<ConnectorListener, String> listeners = listenerManager
				.getOrDefault(connectorConfiguration, new HashMap<>());

		ThreadPoolExecutor threadPoolExecutor = typeOfMessageToThreads.getOrDefault(typeMessage, this.senderPool);
		if (threadPoolExecutor == null || publishThreads == 0) {
			_notify(connectorConfiguration, typeMessage, topic, message, listeners.keySet());
		} else {
			threadPoolExecutor.submit(() -> {
				_notify(connectorConfiguration, typeMessage, topic, message, listeners.keySet());
			});

		}
		return true;
	}

	@Override public int getMessagesSent(ConnectorConfiguration configuration) {
		if (counterMessagesSent.containsKey(configuration)) {
			return counterMessagesSent.get(configuration).get();
		} else {
			return 0;
		}
	}

	@Override public int getMessagesFailed(ConnectorConfiguration configuration) {
		if (counterMessagesNotSent.containsKey(configuration)) {
			return counterMessagesNotSent.get(configuration).get();
		} else {
			return 0;
		}
	}

}
