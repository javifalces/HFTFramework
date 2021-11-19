package com.lambda.investing.connector.zero_mq;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.model.messaging.TopicUtils;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZeroMqPuller implements ConnectorProvider {

	private static Integer THREADS_ON_UPDATE = 3;

	private ZeroMqConfiguration zeroMqConfiguration;
	Logger logger = LogManager.getLogger(ZeroMqPuller.class);
	private Map<ConnectorListener, ConnectorConfiguration> listenerManager;

	private ZeroMqThreadReceiver threadReceiver;
	private Thread thread;
	private static final Map<Integer, ZMQ.Socket> PORTS_TAKEN_PULL = new ConcurrentHashMap<>();
	private ThreadPoolExecutor onUpdateExecutorService;

	private long sleepMsBetweenMessages = 0;
	protected List<String> topicListSubscribed;
	private static Map<ZeroMqConfiguration, ZeroMqPuller> INSTANCES = new ConcurrentHashMap<>();
	String url;
	private ZMQ.Socket socketPull;
	protected int threadsListening;

	protected boolean parsedObjects = true;

	public static ZeroMqPuller getInstance(ZeroMqConfiguration zeroMqConfiguration, int threadsListening) {
		ZeroMqPuller output = INSTANCES
				.getOrDefault(zeroMqConfiguration, new ZeroMqPuller(zeroMqConfiguration, threadsListening));
		INSTANCES.put(zeroMqConfiguration, output);

		return output;
	}

	public void setParsedObjects(boolean parsedObjects) {
		this.parsedObjects = parsedObjects;
	}

	private ZeroMqPuller(ZeroMqConfiguration zeroMqConfiguration, int threadsListening) {
		this.zeroMqConfiguration = zeroMqConfiguration;
		listenerManager = new ConcurrentHashMap<>();
		topicListSubscribed = new ArrayList<>();
		//socket of zero mq

		this.socketPull = getPullSocket(zeroMqConfiguration);

		this.threadsListening = threadsListening;
		//ThreadPool initialiting

		ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
		threadFactoryBuilder.setNameFormat("ZeroMqPuller-OnUpdate-%d");
		threadFactoryBuilder.setPriority(Thread.NORM_PRIORITY);
		ThreadFactory namedThreadFactory = threadFactoryBuilder.build();

		this.threadsListening = threadsListening;
		if (this.threadsListening > 0) {
			onUpdateExecutorService = (ThreadPoolExecutor) Executors
					.newFixedThreadPool(this.threadsListening, namedThreadFactory);
		}
		if (this.threadsListening < 0) {
			onUpdateExecutorService = (ThreadPoolExecutor) Executors.newCachedThreadPool(namedThreadFactory);
		}

	}

	public void setSleepMsBetweenMessages(long sleepMsBetweenMessages) {
		this.sleepMsBetweenMessages = sleepMsBetweenMessages;
	}

	//	public void start() {
	//		start(true);
	//	}

	public void start() {
		boolean isConnected = socketPull.connect(url);

		//		if (hardTopicFilter) {
		//			if (topicListSubscribed.size() == 0) {
		//				logger.error("Starting without topics subscribed!");
		//			}
		//			for (String topic : topicListSubscribed) {
		//				logger.info("PULL {} to {}", url, topic);
		//				socketPull.subscribe(topic.getBytes(ZMQ.CHARSET));
		//			}
		//		} else {
		//			logger.info("PULL {} to {}", url, "all -> filtering on listener");
		//			socketPull.subscribe("");
		//		}

		//Receiver thread
		threadReceiver = new ZeroMqThreadReceiver(this.zeroMqConfiguration);

		this.thread = new Thread(threadReceiver,
				"zeroMq puller " + zeroMqConfiguration.getHost() + ":" + zeroMqConfiguration.getPort());
		this.thread.start();

	}

	@Override public void register(ConnectorConfiguration configuration, ConnectorListener listener) {
		listenerManager.put(listener, configuration);
	}

	@Override public void deregister(ConnectorConfiguration configuration, ConnectorListener listener) {
		listenerManager.remove(listener);
	}

	protected void onUpdate(TypeMessage typeMessage, String message, String topic, long timestamp) throws IOException {

		for (Map.Entry<ConnectorListener, ConnectorConfiguration> entry : listenerManager.entrySet()) {
			ConnectorListener listener = entry.getKey();
			ConnectorConfiguration configuration = entry.getValue();
			if (configuration instanceof ZeroMqConfiguration) {
				//add topic
				ZeroMqConfiguration zeroMqConfiguration = (ZeroMqConfiguration) configuration;
				zeroMqConfiguration.setTopic(topic);
				configuration = zeroMqConfiguration;
			}
			listener.onUpdate(configuration, timestamp, typeMessage, message);

		}
	}

	private ZMQ.Socket getPullSocket(ZeroMqConfiguration configuration) {
		//		http://zguide.zeromq.org/java:psenvsub
		ZMQ.Socket pullSocket = null;
		ZContext context = new ZContext();
		pullSocket = context.createSocket(ZMQ.PULL);
		pullSocket.setHWM(1);
		pullSocket.setLinger(0);
		url = String.format("tcp://%s:%d", configuration.getHost(), configuration.getPort());
		//		logger.info("Starting connecting to messages on socket {}", url);

		pullSocket.connect(url);

		return pullSocket;

	}

	private class ZeroMqThreadReceiver implements Runnable {

		private ZeroMqConfiguration zeroMqConfiguration;
		final AtomicBoolean running = new AtomicBoolean(false);

		public ZeroMqThreadReceiver(ZeroMqConfiguration zeroMqConfiguration) {
			this.zeroMqConfiguration = zeroMqConfiguration;
			running.set(true);
		}

		private synchronized void treatMessage(String topic, String message) {
			if (!parsedObjects) {
				try {
					onUpdate(null, topic, topic, System.currentTimeMillis());
				} catch (Exception e) {
					logger.error("Error reading nonParseZeroMq ", e);
				}
				return;

			}
			boolean isInTopicListSubscribed = topicListSubscribed.contains(topic);
			boolean subscribedToAll =
					topicListSubscribed.size() == 1 && (topicListSubscribed.get(0).equalsIgnoreCase(""));

			if (!isInTopicListSubscribed && !subscribedToAll) {
				logger.warn("discard not on our topic list\ntopic: {}\nmessage:{}", topic, message);
				return;
			}
			logger.debug("receive from topic {}  message  {}", topic, message);

			try {
				TypeMessage typeMessage = TopicUtils.getTypeMessage(topic);
				if (typeMessage == null) {
					logger.error("discarded no type found\ntopic:{}\nmessage:{}", topic, message);
				} else {
					onUpdate(typeMessage, message, topic, System.currentTimeMillis());
				}
			} catch (IOException e) {
				logger.error("Error receiving topic {}  message {}", topic, message, e);
			}
		}

		@Override public void run() {
			while (running.get()) {
				try {
					// Read envelope with topic
					synchronized (socketPull) {
						ZMsg zMsg = ZMsg.recvMsg(socketPull);
						//Read message contents
						String message = zMsg.popString();
						try {
							//							{'_response': 'TRADING_IS_NOT_ALLOWED__ABORTED_COMMAND'}  32769
							treatMessage(message, message);

							//						if (threadsListening != 0) {
							//							onUpdateExecutorService.submit(new Runnable() {
							//
							//								public void run() {
							//									treatMessage(topic, message);
							//								}
							//							});
							//						} else {
							//							treatMessage(topic, message);
							//						}

						} catch (Exception e) {
							logger.error("exception reading zeroMq message:{}", message, e);
							e.printStackTrace();
						}
					}
					try {
						Thread.sleep(sleepMsBetweenMessages);
					} catch (InterruptedException e) {
						logger.error("cant sleep ZeroMqProvider ", e);
					}
				} catch (Exception e) {
					logger.error("error reading ZeroMQ message ", e);

				}
			}
			System.err.print("end of zeroMQProvider?");

		}
	}
}
