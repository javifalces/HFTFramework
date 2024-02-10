package com.lambda.investing.connector.zero_mq;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ZeroMqPusher implements ConnectorPublisher {

    private static String KO_REQUEST = "KO";
    private static final Map<Integer, ZMQ.Socket> PORTS_TAKEN_PUSH = new ConcurrentHashMap<>();
    Logger logger = LogManager.getLogger(ZeroMqPusher.class);

    Map<com.lambda.investing.connector.zero_mq.ZeroMqConfiguration, AtomicInteger> counterMessagesSent = new HashMap<>();
    Map<com.lambda.investing.connector.zero_mq.ZeroMqConfiguration, AtomicInteger> counterMessagesNotSent = new HashMap<>();

    private ExecutorService poolExecutor;
    private String name;
    private int threads;

    private Map<com.lambda.investing.connector.zero_mq.ZeroMqConfiguration, ZeroMqPuller> zeroMqPullerMap;
    private ZeroMqPusherListener zeroMqPusherListener = new ZeroMqPusherListener();
    private final Object lock = new Object();

    public ZeroMqPusher(String name, int threads) {

        //ThreadPool initialiting
        this.name = name;
        this.threads = threads;
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(this.name + "-%d").build();
        if (this.threads < 0) {
            poolExecutor = Executors.newCachedThreadPool(namedThreadFactory);
        }
        if (this.threads > 0) {
            poolExecutor = Executors.newFixedThreadPool(this.threads, namedThreadFactory);
        }

        zeroMqPullerMap = new HashMap<>();

    }

    @Override
    public int getMessagesSent(ConnectorConfiguration configuration) {
        com.lambda.investing.connector.zero_mq.ZeroMqConfiguration zeroMqConfiguration = (com.lambda.investing.connector.zero_mq.ZeroMqConfiguration) configuration;
        if (counterMessagesSent.containsKey(zeroMqConfiguration)) {
            return counterMessagesSent.get(zeroMqConfiguration).get();
        } else {
            return 0;
        }
    }

    @Override
    public int getMessagesFailed(ConnectorConfiguration configuration) {
        com.lambda.investing.connector.zero_mq.ZeroMqConfiguration zeroMqConfiguration = (com.lambda.investing.connector.zero_mq.ZeroMqConfiguration) configuration;
        if (counterMessagesNotSent.containsKey(zeroMqConfiguration)) {
            return counterMessagesNotSent.get(zeroMqConfiguration).get();
        } else {
            return 0;
        }
    }

    private ZMQ.Socket getPushSocket(com.lambda.investing.connector.zero_mq.ZeroMqConfiguration configuration) {
        //		http://zguide.zeromq.org/java:hwserver
        //		ZMQ.Socket publishSocket = null;
        //		ZMQ.Socket reqSocket = null;
        //		if (!PORTS_TAKEN_PUSH.containsKey(configuration.getPort())) {
        //			ZContext context = new ZContext();
        //			publishSocket = context.createSocket(ZMQ.PUSH);
        //			String url = String.format("tcp://*:%d", configuration.getPort());
        //			logger.info("Creating Push socket {} : {}", url, configuration);
        //			PORTS_TAKEN_PUSH.put(configuration.getPort(), publishSocket);
        //			publishSocket.bind(url);
        //		} else {
        //			publishSocket = PORTS_TAKEN_PUSH.get(configuration.getPort());
        //		}
        //
        //		return publishSocket;

        //		http://zguide.zeromq.org/java:psenvsub
        ZMQ.Socket pushSocket = null;
        if (!PORTS_TAKEN_PUSH.containsKey(configuration.getPort())) {
            ZContext context = com.lambda.investing.connector.zero_mq.ZeroMqConfiguration.GetZContext();
            pushSocket = context.createSocket(ZMQ.PUSH);
            pushSocket.setHWM(1);
            pushSocket.setLinger(0);
            String url = configuration.getUrl();
            //		logger.info("Starting connecting to messages on socket {}", url);

            pushSocket.connect(url);

            if (!this.zeroMqPullerMap.containsKey(configuration)) {
                ZeroMqPuller zeroMqPuller = ZeroMqPuller.getInstance(configuration, 0);
                zeroMqPuller.start();
                zeroMqPuller.register(configuration, zeroMqPusherListener);
                zeroMqPullerMap.put(configuration, zeroMqPuller);
            }
            PORTS_TAKEN_PUSH.put(configuration.getPort(), pushSocket);
        } else {
            pushSocket = PORTS_TAKEN_PUSH.get(configuration.getPort());
        }
        return pushSocket;

    }

    @Override
    public boolean publish(ConnectorConfiguration connectorConfiguration, TypeMessage typeMessage,
                           String topic, String message) {
        if (!(connectorConfiguration instanceof com.lambda.investing.connector.zero_mq.ZeroMqConfiguration)) {
            logger.error("configuration is not ZeroMqConfiguration");
            return false;
        }
        int retries = 1;

        com.lambda.investing.connector.zero_mq.ZeroMqConfiguration zeroMqConfiguration = (ZeroMqConfiguration) connectorConfiguration;
        ZMQ.Socket socket = getPushSocket(zeroMqConfiguration);
        synchronized (socket) {
            for (int counter = 0; counter < retries; counter++) {
                try {
                    //ZeroMq cant shared threads to send messages
                    send(message, System.currentTimeMillis(), socket);

                    //				if (this.threads == 0) {
                    //					send(message, zeroMqConfiguration, topic, System.currentTimeMillis(), socket);
                    //				} else {
                    //					this.poolExecutor.submit(new Runnable() {
                    //
                    //						public void run() {
                    //							send(message, zeroMqConfiguration, topic, System.currentTimeMillis(), socket);
                    //						}
                    //					});
                    //				}
                } catch (Exception exception) {
                    logger.error("Error sending message {} : ", message, exception);
                    return false;
                }
            }
        }
        return true;

    }

    private synchronized void send(String message, long timestamp,
                                   ZMQ.Socket socket) {
        logger.debug("Sending to zeroMq push :\n {}", message);
        //		return socket.send(message.getBytes(ZMQ.CHARSET));
        long elapsed = System.currentTimeMillis() - timestamp;
        logger.debug("[ZEROMQ]Took {} ms to process message", elapsed);

        //		boolean output = ZMsg.newStringMsg(topic, message).send(socket);
        boolean output = socket.send(message);

    }

    private class ZeroMqPusherListener implements ConnectorListener {

        @Override
        public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
                             TypeMessage typeMessage, String content) {
            //listening
            logger.debug("listening to zeroMq push :\n {}", content);
        }
    }
}
