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

public class ZeroMqProvider implements ConnectorProvider {

    private static Integer THREADS_ON_UPDATE = 3;

    private ZeroMqConfiguration zeroMqConfiguration;
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

    private ZeroMqProvider(ZeroMqConfiguration zeroMqConfiguration, int threadsListening) {
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


        this.threadsListening = threadsListening;
        //ThreadPool initialiting

        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setNameFormat("ZeroMqProvider-OnUpdate-%d");
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
            socketSub.bind(url);
        } else {
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
            //ACK REP publisher
            if (!isServer) {
                String urlAck = String.format("%s://*:%d", this.zeroMqConfiguration.getProtocol(), this.zeroMqConfiguration.getPort() + 1);
                this.socketReq.connect(urlAck);
            } else {
                String urlAck = String.format("%s://%s:%d", this.zeroMqConfiguration.getProtocol(), this.zeroMqConfiguration.getHost(), this.zeroMqConfiguration.getPort() + 1);
                this.socketReq.bind(urlAck);
            }
        }

        //Receiver thread
        threadReceiver = new ZeroMqThreadReceiver(this.zeroMqConfiguration);

        this.thread = new Thread(threadReceiver,
                "zeroMq receiver " + zeroMqConfiguration.getHost() + ":" + zeroMqConfiguration.getPort() + "("
                        + zeroMqConfiguration.getTopic() + ")");
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

    protected void onUpdate(TypeMessage typeMessage, String message, String topic, long timestamp) throws IOException {
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
        ZMQ.Socket subscribeSocket = null;
        subscribeSocket = context.createSocket(ZMQ.SUB);
        subscribeSocket.setHWM(1);
        subscribeSocket.setLinger(0);

        if (isServer) {
            url = configuration.getBindUrl();
            logger.info("Creating Sub server socket {} ", url);
            subscribeSocket.bind(url);
        } else {
            url = configuration.getUrl();
            logger.info("Connecting Sub socket {} ", url);
            //		logger.info("Starting connecting to messages on socket {}", url);
            subscribeSocket.connect(url);
        }

        return subscribeSocket;

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

        @Override
        public void run() {
            while (running.get()) {
                try {
                    // Read envelope with topic
                    synchronized (socketSub) {
                        ZMsg zMsg = ZMsg.recvMsg(socketSub);
                        //Read message contents
                        String topic = zMsg.popString();
                        String message = zMsg.popString();
                        try {

                            treatMessage(topic, message);

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
                            logger.error("exception reading zeroMq \ntopic:{}\nmessage:{}\n", topic, message, e);
                            e.printStackTrace();
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
