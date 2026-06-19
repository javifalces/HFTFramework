package com.lambda.investing.connector.zero_mq;


import com.lambda.investing.LambdaThreadFactory;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.model.messaging.TopicUtils;
import com.lambda.investing.model.messaging.TypeMessage;
import lombok.Getter;
import org.apache.commons.lang3.SerializationUtils;
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

public class ZeroMqProvider implements ConnectorProvider {

    @Getter
    protected ZeroMqConfiguration zeroMqConfiguration;
    Logger logger = LogManager.getLogger(ZeroMqProvider.class);
    private Map<ConnectorListener, ConnectorConfiguration> listenerManager;

    private ZeroMqThreadReceiver threadReceiver;
    private Thread thread;
    private static final Map<Integer, ZMQ.Socket> PORTS_TAKEN_SUB = new ConcurrentHashMap<>();
    private ThreadPoolExecutor onUpdateExecutorService;

    private long sleepMsBetweenMessages = 0;
    protected List<String> topicListSubscribed;
    private static Map<ZeroMqConfiguration, ZeroMqProvider> INSTANCES = new ConcurrentHashMap<>();
    String url;
    private ZMQ.Socket socketSub;
    private ZMQ.Socket socketReq;//forACks
    protected int threadsListening;

    protected boolean parsedObjects = true;
    ZContext context;
    private final Map<String, Object> topicLocks = new ConcurrentHashMap<>();
    private static boolean DEFAULT_SERVER = false;
    private boolean isServer = DEFAULT_SERVER;

    public static ZeroMqProvider getInstance(ZeroMqConfiguration zeroMqConfiguration, int threadsListening, boolean isServer) {
        ZeroMqProvider output = INSTANCES
                .getOrDefault(zeroMqConfiguration, new ZeroMqProvider(zeroMqConfiguration, threadsListening));
        output.setServer(isServer);
        INSTANCES.put(zeroMqConfiguration, output);

        //subscribe to topic
        String topic = zeroMqConfiguration.getTopic();
        if (topic == null) {
            topic = "";
        }
        output.subscribeTopic(topic);

        return output;
    }

    public static ZeroMqProvider getInstance(ZeroMqConfiguration zeroMqConfiguration, int threadsListening) {
        return getInstance(zeroMqConfiguration, threadsListening, DEFAULT_SERVER);
    }

    public void setServer(boolean server) {
        isServer = server;
        if (isServer) {
            url = zeroMqConfiguration.getBindUrl();
        } else {
            url = zeroMqConfiguration.getUrl();
        }
    }

    public void setParsedObjects(boolean parsedObjects) {
        this.parsedObjects = parsedObjects;
    }

    protected ZeroMqProvider(ZeroMqConfiguration zeroMqConfiguration, int threadsListening) {
        this.zeroMqConfiguration = zeroMqConfiguration;
        listenerManager = new ConcurrentHashMap<>();
        topicListSubscribed = new ArrayList<>();
        //socket of zero mq
        context = ZeroMqConfiguration.GetZContext();//create here to avoid remove by GC

        this.socketSub = getSubscribeSocket(zeroMqConfiguration);

        //ACK socket
        this.socketReq = context.createSocket(ZMQ.REQ);
        socketReq.setHWM(1);
        socketReq.setLinger(0);

        //ThreadPool initialiting
        ThreadFactory namedThreadFactory = LambdaThreadFactory.createThreadFactory("ZeroMqProvider-OnUpdate", Thread.NORM_PRIORITY);
        this.threadsListening = threadsListening;
        if (this.threadsListening > 0) {
            onUpdateExecutorService = (ThreadPoolExecutor) Executors
                    .newFixedThreadPool(this.threadsListening, namedThreadFactory);
        }
        if (this.threadsListening < 0) {
            onUpdateExecutorService = (ThreadPoolExecutor) Executors.newCachedThreadPool(namedThreadFactory);
        }


    }

    public void subscribeTopic(String topic) {
        if (topicListSubscribed.size() == 0)
            topicListSubscribed.add(topic);
    }

    public void setSleepMsBetweenMessages(long sleepMsBetweenMessages) {
        this.sleepMsBetweenMessages = sleepMsBetweenMessages;
    }

    public void start() {
        start(true, true);
    }

    public void start(boolean hardTopicFilter, boolean sendAck) {

        if (isServer) {
            logger.info("Binding SUB socket to {}", url);
            socketSub.bind(url);
        } else {
            logger.info("Connecting SUB socket to {}", url);
            socketSub.connect(url);
        }


        if (hardTopicFilter) {
            if (topicListSubscribed.size() == 0) {
                logger.error("Starting without topics subscribed!");
            }
            for (String topic : topicListSubscribed) {
                String topicSuffix = "";
                if (!topic.isEmpty()) {
                    topicSuffix = "to " + topic;
                }
                logger.info("SUB (server {}) {} {}", isServer, url, topicSuffix);
                socketSub.subscribe(topic.getBytes(ZMQ.CHARSET));
            }
        } else {
            logger.info("SUB (server {}){} to {}", isServer, url, "all -> filtering on listener");
            socketSub.subscribe(" ".getBytes(ZMQ.CHARSET));
        }
        if (sendAck) {
            //ACK REP publisher – use config helpers so IPC addresses are handled correctly
            if (!isServer) {
                String urlAck = this.zeroMqConfiguration.getAckConnectUrl();
                logger.info("ACK REQ connect {}", urlAck);
                this.socketReq.connect(urlAck);
            } else {
                String urlAck = this.zeroMqConfiguration.getAckBindUrl();
                logger.info("ACK REQ bind {}", urlAck);
                this.socketReq.bind(urlAck);
            }
        }

        //Receiver thread
        threadReceiver = new ZeroMqThreadReceiver(this.zeroMqConfiguration);

        this.thread = new Thread(threadReceiver,
                "ZeroMqProvider -> " + zeroMqConfiguration.getHost() + ":" + zeroMqConfiguration.getPort());
        this.thread.start();

    }

    @Override
    public void register(ConnectorConfiguration configuration, ConnectorListener listener) {
        listenerManager.put(listener, configuration);
    }

    @Override
    public void deregister(ConnectorConfiguration configuration, ConnectorListener listener) {
        listenerManager.remove(listener);
    }

    private void answerRep(String message) {
        this.socketReq.send(message);
        String reply = this.socketReq.recvStr(0);
        //		String replyStr=new String(reply);

    }

    protected void onUpdate(TypeMessage typeMessage, Object message, String topic, long timestamp) throws IOException {
        if (typeMessage != null && typeMessage.equals(TypeMessage.command)) {
            answerRep("OK");
        }

        for (Map.Entry<ConnectorListener, ConnectorConfiguration> entry : listenerManager.entrySet()) {
            ConnectorListener listener = entry.getKey();
            ConnectorConfiguration configuration = entry.getValue();
            if (this.parsedObjects && configuration instanceof ZeroMqConfiguration) {
                //add topic
                ZeroMqConfiguration zeroMqConfiguration = (ZeroMqConfiguration) configuration;
                zeroMqConfiguration.setTopic(topic);
                configuration = zeroMqConfiguration;
            }

            listener.onUpdate(configuration, timestamp, typeMessage, message);

        }
    }

    private ZMQ.Socket getSubscribeSocket(ZeroMqConfiguration configuration) {
        //		http://zguide.zeromq.org/java:psenvsub
        // Only CREATE and configure the socket here.
        // Bind/connect is deferred to start() which is called AFTER setServer(),
        // so that isServer has its final value before we touch the transport layer.
        // (Previously this method also called bind/connect, causing a double
        //  bind/connect when start() ran, and using the wrong server role because
        //  setServer() had not yet been invoked at construction time.)
        ZMQ.Socket subscribeSocket = context.createSocket(ZMQ.SUB);
        subscribeSocket.setHWM(1);
        subscribeSocket.setLinger(0);

        return subscribeSocket;

    }


    private class ZeroMqThreadReceiver implements Runnable {

        private ZeroMqConfiguration zeroMqConfiguration;
        final AtomicBoolean running = new AtomicBoolean(false);

        public ZeroMqThreadReceiver(ZeroMqConfiguration zeroMqConfiguration) {
            this.zeroMqConfiguration = zeroMqConfiguration;
            running.set(true);
        }

        private void treatMessage(String topic, Object message, long timestampReceived) {
            // Get or create a lock object for this specific topic
            Object topicLock = topicLocks.computeIfAbsent(topic, k -> new Object());

            synchronized (topicLock) {
                if (!parsedObjects) {
                    try {
                        onUpdate(null, topic, topic, timestampReceived);
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
                        onUpdate(typeMessage, message, topic, timestampReceived);
                    }
                } catch (IOException e) {
                    logger.error("Error receiving topic {}  message {}", topic, message, e);
                }
            }
        }

        @Override
        public void run() {
            while (running.get()) {
                try {
                    String topic = null;
                    byte[] messageBytes = null;
                    long timestampReceived = 0;

                    // Minimal lock scope: only socket I/O to minimise time the socket is held
                    synchronized (socketSub) {
                        ZMsg zMsg = ZMsg.recvMsg(socketSub);
                        if (zMsg == null) {
                            logger.warn("Received null ZMsg – skipping");
                            continue;
                        }
                        topic = zMsg.popString();
                        org.zeromq.ZFrame payloadFrame = zMsg.pop();
                        if (payloadFrame == null) {
                            logger.warn("ZMsg has no payload frame (topic='{}') – skipping", topic);
                            continue;
                        }
                        messageBytes = payloadFrame.getData();
                        // Capture timestamp immediately after bytes are read from socket,
                        // before any deserialization, so timestampAlgoConnector reflects true arrival
                        timestampReceived = System.currentTimeMillis();
                    }

                    // Deserialization and listener dispatch run outside the socket lock.
                    // When a thread pool is configured (threadsListening > 0 or < 0) the receiver
                    // thread returns to the socket immediately, reducing head-of-line blocking.
                    // Capture effectively-final copies for use in the lambda below.
                    final String finalTopic = topic;
                    final byte[] finalMessageBytes = messageBytes;
                    final long finalTimestampReceived = timestampReceived;

                    if (onUpdateExecutorService != null) {
                        onUpdateExecutorService.submit(() -> {
                            try {
                                Object objMessage = SerializationUtils.deserialize(finalMessageBytes);
                                treatMessage(finalTopic, objMessage, finalTimestampReceived);
                            } catch (Exception e) {
                                logger.error("exception processing zeroMq message in thread pool topic:{}", finalTopic, e);
                            }
                        });
                    } else {
                        Object objMessage = SerializationUtils.deserialize(finalMessageBytes);
                        try {
                            treatMessage(finalTopic, objMessage, finalTimestampReceived);
                        } catch (Exception e) {
                            logger.error("exception processing zeroMq message \ntopic:{}\n", finalTopic, e);
                        }
                    }

                } catch (Exception e) {
                    logger.error("error reading ZeroMQ message ", e);

                } finally {
                    Thread.onSpinWait();//to not occupy the cpu
                }
            }
            System.err.print("end of zeroMQProvider?");

        }
    }
}
