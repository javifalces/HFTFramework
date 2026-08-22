package com.lambda.investing.connector.zero_mq;


import com.lambda.investing.Configuration;
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
import org.zeromq.ZMQException;
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

/**
 * ZeroMQ SUB-socket provider that receives messages and dispatches them to registered
 * {@link ConnectorListener}s.
 *
 * <h2>Threading architecture</h2>
 * <pre>
 *  ZeroMq kernel buffer
 *    └─ ZeroMqThreadReceiver  (one dedicated thread, owns the SUB socket)
 *             │  synchronized(socketSub) – minimal lock: read bytes + timestamp only
 *             │
 *             ├─ threadsListening == 0  →  deserialize + treatMessage on this thread
 *             │                            (synchronous; fine for low-volume use)
 *             │
 *             └─ threadsListening != 0  →  submit(deserialize + treatMessage)
 *                        │                 to onUpdateExecutorService
 *                        ▼
 *                ThreadPoolExecutor  (fixed if >0, cached if <0)
 *                        │
 *                        └─ per-topic synchronized(topicLock)
 *                                   └─ onUpdate() → ConnectorListener.onUpdate(...)
 * </pre>
 *
 * <h2>Why a dedicated receiver thread?</h2>
 * ZeroMQ sockets are <em>not thread-safe</em>; {@code ZMsg.recvMsg()} blocks until a
 * message arrives.  {@code ZeroMqThreadReceiver} dedicates a single thread to that
 * blocking call so the application thread is never tied up waiting on I/O.  The lock
 * scope around the socket is intentionally minimal – only raw byte extraction and
 * timestamping – so the thread returns to the socket as fast as possible.  This matters
 * because {@code socketSub.setHWM(1)}: if the socket is not drained quickly enough the
 * ZMQ kernel will start dropping messages.
 *
 * <h2>Why offload to a thread pool?</h2>
 * Two operations on the hot path are expensive relative to socket I/O:
 * <ol>
 *   <li>{@code SerializationUtils.deserialize} – allocates, does reflection-based I/O.</li>
 *   <li>{@code ConnectorListener.onUpdate} – may run algorithm logic or order management.</li>
 * </ol>
 * Running both on the receiver thread causes head-of-line blocking: one slow listener
 * stalls every subsequent message.  The thread pool moves that work off the receiver
 * thread so it returns to the socket immediately after submission.
 * Per-topic {@code synchronized(topicLock)} in {@code treatMessage} preserves in-order
 * delivery within a topic even when multiple pool threads are active.
 *
 * <h2>Low-latency alternative</h2>
 * {@link ZeroMqProviderDisruptor} keeps {@code ZeroMqThreadReceiver} for socket I/O but
 * replaces the thread pool with an LMAX Disruptor ring-buffer (~50 ns publish, no
 * steady-state allocation).  When using that subclass pass {@code threadsListening=0} to
 * suppress the thread pool; the receiver thread will publish directly to the ring-buffer.
 */
public class ZeroMqProvider implements ConnectorProvider {

    @Getter
    protected ZeroMqConfiguration zeroMqConfiguration;
    Logger logger = LogManager.getLogger(ZeroMqProvider.class);
    private Map<ConnectorListener, ConnectorConfiguration> listenerManager;

    private ZeroMqThreadReceiver threadReceiver;
    private Thread thread;
    private static final Map<Integer, ZMQ.Socket> PORTS_TAKEN_SUB = new ConcurrentHashMap<>();
    /**
     * Dispatches deserialization + listener delivery off the receiver thread.
     * Null when {@code threadsListening == 0} (synchronous mode).
     * Fixed-size when {@code threadsListening > 0}; cached (unbounded) when {@code < 0}.
     */
    private ThreadPoolExecutor onUpdateExecutorService;

    private long sleepMsBetweenMessages = 0;
    protected List<String> topicListSubscribed;
    private static Map<ZeroMqConfiguration, ZeroMqProvider> INSTANCES = new ConcurrentHashMap<>();
    String url;
    protected ZMQ.Socket socketSub;
    private ZMQ.Socket socketReq;//forACks
    protected int threadsListening;

    protected boolean parsedObjects = true;
    ZContext context;
    private final Map<String, Object> topicLocks = new ConcurrentHashMap<>();
    private static boolean DEFAULT_SERVER = false;
    private boolean isServer = DEFAULT_SERVER;

    public static ZeroMqProvider getInstance(ZeroMqConfiguration zeroMqConfiguration, int threadsListening, boolean isServer) {
        ZeroMqProvider output = INSTANCES.computeIfAbsent(zeroMqConfiguration,
                cfg -> new ZeroMqProvider(cfg, threadsListening));
        output.setServer(isServer);

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

    /**
     * Exposes the current server/client role for subclasses (e.g. {@link ZeroMqProviderDisruptor}
     * needs it to detect/log when a cached instance is being reused with different parameters).
     */
    protected boolean isServerFlag() {
        return isServer;
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

    public void start() {
        start(true, true);
    }
    public void start(boolean hardTopicFilter, boolean sendAck) {
        String threadName = Configuration.formatLog("zeroMqThreadReceiver({})-> {}:{}", this.threadsListening, zeroMqConfiguration.getHost(), zeroMqConfiguration.getPort());
        start(hardTopicFilter, sendAck, threadName);
    }

    public void start(boolean hardTopicFilter, boolean sendAck, String zeroMqThreadReceiverThreadName) {
        if (thread != null && thread.isAlive()) {
            logger.debug("ZeroMqProvider receiver thread already running for {}:{} – skipping duplicate start",
                    zeroMqConfiguration.getHost(), zeroMqConfiguration.getPort());
            return;
        }

        setupSocket(hardTopicFilter, sendAck);

        // Receiver thread
        threadReceiver = new ZeroMqThreadReceiver(this.zeroMqConfiguration);
        this.thread = new Thread(threadReceiver, zeroMqThreadReceiverThreadName);
        this.thread.start();
    }

    /**
     * Binds/connects the SUB socket, subscribes to topics, and sets up the ACK socket.
     * Does <em>not</em> start a receiver thread – that is left to the caller.
     * Subclasses that provide their own receive loop (e.g. {@link ZeroMqProviderDisruptor})
     * call this method instead of the full {@link #start(boolean, boolean, String)}.
     */
    protected void setupSocket(boolean hardTopicFilter, boolean sendAck) {
        if (isServer) {
            logger.info("Binding SUB socket to {}", url);
            try {
                socketSub.bind(url);
            } catch (ZMQException e) {
                if (e.getErrorCode() == 48) { // EADDRINUSE
                    logger.error("Address already in use: {}", url);
                    System.err.println("Address already in use: " + url);
                }
                throw e;
            }
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
            // ACK REP publisher – use config helpers so IPC addresses are handled correctly
            if (!isServer) {
                String urlAck = this.zeroMqConfiguration.getAckConnectUrl();
                logger.info("ACK REQ connect {}", urlAck);
                this.socketReq.connect(urlAck);
            } else {
                String urlAck = this.zeroMqConfiguration.getAckBindUrl();
                logger.info("ACK REQ bind {}", urlAck);
                try {
                    this.socketReq.bind(urlAck);
                } catch (ZMQException e) {
                    if (e.getErrorCode() == 48) { // EADDRINUSE
                        logger.error("Address already in use: {}", urlAck);
                        System.err.println("Address already in use: " + urlAck);
                    }
                    throw e;
                }
            }
        }
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


    /**
     * The sole owner of the ZMQ SUB socket.
     *
     * <p>Runs a tight receive loop: acquires {@code socketSub} lock for the minimum time
     * needed to extract raw bytes and capture the arrival timestamp, then either processes
     * inline (no pool) or submits to {@link #onUpdateExecutorService} and immediately
     * returns to the socket.  This keeps the socket drained and avoids HWM-triggered drops.
     */
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
