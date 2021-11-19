package com.lambda.investing.connector.ordinary;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lambda.investing.connector.*;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class OrdinaryConnectorPublisherProvider implements ConnectorPublisher, ConnectorProvider {

	private Map<ConnectorConfiguration, Map<ConnectorListener, String>> listenerManager;
	private Map<ConnectorConfiguration, AtomicInteger> counterMessagesSent;
	private Map<ConnectorConfiguration, AtomicInteger> counterMessagesNotSent;
	Logger logger = LogManager.getLogger(OrdinaryConnectorPublisherProvider.class);
	ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
			.setNameFormat("OrdinaryConnectorPublisherProvider -%d").build();

	ThreadPoolExecutor senderPool;
	private Integer priority = null;

	private int threads;
	private String name;

	private Map<TypeMessage, ThreadPoolExecutor> typeOfMessageToThreads = new HashMap<>();

	/**
	 * @param name    name of the threadpool
	 * @param threads number of threads that publish to register ConnectorListeners
	 */
	public OrdinaryConnectorPublisherProvider(String name, int threads) {
		listenerManager = new ConcurrentHashMap<>();
		counterMessagesSent = new ConcurrentHashMap<>();
		counterMessagesNotSent = new ConcurrentHashMap<>();

		//thread pool name
		this.name = name;
		this.threads = threads;

		ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
		threadFactoryBuilder.setNameFormat(this.name + " -%d").build();
		threadFactoryBuilder.setPriority(Thread.NORM_PRIORITY);
		namedThreadFactory = threadFactoryBuilder.build();
		if (this.threads < 0) {
			//infinite
			senderPool = (ThreadPoolExecutor) Executors.newCachedThreadPool(namedThreadFactory);
		}
		if (this.threads > 0) {
			senderPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.threads, namedThreadFactory);
		}
	}

	public OrdinaryConnectorPublisherProvider(String name, int threads, Integer priority) {
		listenerManager = new ConcurrentHashMap<>();
		counterMessagesSent = new ConcurrentHashMap<>();
		counterMessagesNotSent = new ConcurrentHashMap<>();

		//thread pool name
		this.name = name;
		this.threads = threads;

		ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
		threadFactoryBuilder.setNameFormat(this.name + " -%d").build();
		threadFactoryBuilder.setPriority(priority);
		namedThreadFactory = threadFactoryBuilder.build();
		if (this.threads < 0) {
			//infinite
			senderPool = (ThreadPoolExecutor) Executors.newCachedThreadPool(namedThreadFactory);
		}
		if (this.threads > 0) {
			senderPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.threads, namedThreadFactory);
		}

	}

	public void setRoutingPool(Map<TypeMessage, ThreadPoolExecutor> typeOfMessageToThreads) {
		this.typeOfMessageToThreads = typeOfMessageToThreads;
	}

	@Override public void register(ConnectorConfiguration configuration, ConnectorListener listener) {
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
			logger.error("error notifying {} :{} ", topic, message, ex);
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
				.getOrDefault(connectorConfiguration, new ConcurrentHashMap<>());

		ThreadPoolExecutor threadPoolExecutor = typeOfMessageToThreads.get(typeMessage);

		if (threadPoolExecutor == null && threads == 0) {
			_notify(connectorConfiguration, typeMessage, topic, message, listeners.keySet());
		} else {
			if (threadPoolExecutor == null) {
				threadPoolExecutor = this.senderPool;
			}
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
