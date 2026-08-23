package com.lambda.investing.connector.zero_mq;


import com.lambda.investing.LambdaThreadFactory;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ZeroMqPublisher implements ConnectorPublisher {

    private static final int COMMAND_RETRIES_PUBLISH = 1;//zero MQ will retry until ACK
    // Keyed by the socket URL string (not port integer) so that IPC configurations
    // with port=0 don't all collide on the same key.
    private static final Map<String, ZMQ.Socket> PORTS_TAKEN_PUB = new ConcurrentHashMap<>();
    private static final Map<String, ZMQ.Socket> PORTS_TAKEN_REQ_ACK = new ConcurrentHashMap<>();
    Logger logger = LogManager.getLogger(ZeroMqPublisher.class);
    private int OKReceived = 0;

    //ConcurrentHashMap: send() is no longer synchronized on `this` (see below), so these
    //counters can now be updated concurrently by different sockets/configurations.
    Map<ZeroMqConfiguration, AtomicInteger> counterMessagesSent = new ConcurrentHashMap<>();
    Map<ZeroMqConfiguration, AtomicInteger> counterMessagesNotSent = new ConcurrentHashMap<>();

    private ExecutorService poolExecutor;
    private String name;
    private int threads;

    private final Object lock = new Object();

    ZContext context;

    private static boolean DEFAULT_SERVER = true;
    private boolean isServer = DEFAULT_SERVER;

    public ZeroMqPublisher(String name, int threads) {

        //ThreadPool initialiting
        this.name = name;
        this.threads = threads;
        ThreadFactory namedThreadFactory = LambdaThreadFactory.createThreadFactory(this.name);
        if (this.threads < 0) {
            poolExecutor = Executors.newCachedThreadPool(namedThreadFactory);
        }
        if (this.threads > 0) {
            poolExecutor = Executors.newFixedThreadPool(this.threads, namedThreadFactory);
        }
        context = ZeroMqConfiguration.GetZContext();//create here to avoid remove by GC

    }

    public void setServer(boolean server) {
        isServer = server;
    }

    public int getOKReceived() {
        return OKReceived;
    }

    private ZMQ.Socket getPublishSocket(ZeroMqConfiguration configuration) {
        //		http://zguide.zeromq.org/java:hwserver
        // Use the socket URL as the cache key so that IPC configurations (port=0)
        // don't all collide on the same map entry.
        String pubKey = isServer ? configuration.getBindUrl() : configuration.getUrl();
        String ackKey = isServer ? configuration.getAckBindUrl() : configuration.getAckConnectUrl();

        ZMQ.Socket publishSocket = null;
        if (!PORTS_TAKEN_PUB.containsKey(pubKey)) {
            publishSocket = context.createSocket(ZMQ.PUB);
            publishSocket.setHWM(1);
            publishSocket.setLinger(0);
            if (isServer) {
                String url = configuration.getBindUrl();
                logger.info("Creating PUB server socket {} : {}", url, configuration);
                PORTS_TAKEN_PUB.put(pubKey, publishSocket);
                publishSocket.bind(url);
            } else {
                String url = configuration.getUrl();
                logger.info("Connecting PUB socket {} : {}", url, configuration);
                PORTS_TAKEN_PUB.put(pubKey, publishSocket);
                publishSocket.connect(url);
            }
        } else {
            publishSocket = PORTS_TAKEN_PUB.get(pubKey);
        }

        // ACK socket creation guarded separately to avoid double-bind
        if (!PORTS_TAKEN_REQ_ACK.containsKey(ackKey)) {
            ZContext contextAck = ZeroMqConfiguration.GetZContext();
            ZMQ.Socket reqSocket = contextAck.createSocket(ZMQ.REP);
            reqSocket.setHWM(1);
            reqSocket.setLinger(0);
            if (isServer) {
                String urlAck = configuration.getAckBindUrl();
                logger.info("Creating REP server socket {} ", urlAck);
                PORTS_TAKEN_REQ_ACK.put(ackKey, reqSocket);
                reqSocket.bind(urlAck);
            } else {
                String urlAck = configuration.getAckConnectUrl();
                logger.info("Connecting REP socket {} ", urlAck);
                PORTS_TAKEN_REQ_ACK.put(ackKey, reqSocket);
                reqSocket.connect(urlAck);
            }

            new Thread(new ZeroMqAckReqProvider(reqSocket), "ZeroMqAckReqProvider -> " + ackKey).start();
        }

        return publishSocket;

    }

    @Override public int getMessagesSent(ConnectorConfiguration configuration) {
        ZeroMqConfiguration zeroMqConfiguration = (ZeroMqConfiguration) configuration;
        if (counterMessagesSent.containsKey(zeroMqConfiguration)) {
            return counterMessagesSent.get(zeroMqConfiguration).get();
        } else {
            return 0;
        }
    }

    @Override public int getMessagesFailed(ConnectorConfiguration configuration) {
        ZeroMqConfiguration zeroMqConfiguration = (ZeroMqConfiguration) configuration;
        if (counterMessagesNotSent.containsKey(zeroMqConfiguration)) {
            return counterMessagesNotSent.get(zeroMqConfiguration).get();
        } else {
            return 0;
        }
    }

    @Override public boolean publish(ConnectorConfiguration connectorConfiguration, TypeMessage typeMessage,
                                     String topic, Serializable message) {
        if (!(connectorConfiguration instanceof ZeroMqConfiguration)) {
            logger.error("configuration is not ZeroMqConfiguration");
            return false;
        }
        if (typeMessage == null) {
            logger.warn("typeMessage is null with topic: {} message: {}", topic, message);
        }
        int retries = 1;
        if (typeMessage.equals(TypeMessage.command)) {
            retries = COMMAND_RETRIES_PUBLISH;
        }


        ZeroMqConfiguration zeroMqConfiguration = (ZeroMqConfiguration) connectorConfiguration;
        ZMQ.Socket socket = getPublishSocket(zeroMqConfiguration);
        synchronized (socket) {
            for (int counter = 0; counter < retries; counter++) {
                try {
                    //ZeroMq cant shared threads to send messages
                    send(message, zeroMqConfiguration, topic, System.currentTimeMillis(), socket);

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

    //NOTE: not synchronized here on purpose. The caller (publish()) already holds
    //synchronized(socket) for the minimum required scope; synchronizing this method on
    //`this` as well would additionally serialize sends across *every* ZeroMqConfiguration
    //using this publisher (even ones with independent sockets), adding needless lock
    //contention on the order-request hot path without any extra safety.
    private void send(Serializable message, ZeroMqConfiguration configuration, String topic, long timestamp,
                                   ZMQ.Socket socket) {
        counterMessagesSent.computeIfAbsent(configuration, cfg -> new AtomicInteger(0));
        counterMessagesNotSent.computeIfAbsent(configuration, cfg -> new AtomicInteger(0));
        boolean messageIsStringEmpty = message instanceof String && ((String) message).trim().length() == 0;
        if ((topic.trim().length() == 0) || messageIsStringEmpty)
            return;

        logger.debug("Sending to zeroMq {} :\n {}", topic, message);
        //		return socket.send(message.getBytes(ZMQ.CHARSET));
        long elapsed = System.currentTimeMillis() - timestamp;
        logger.debug("[ZEROMQ]Took {} ms to process message", elapsed);

        //		boolean output = ZMsg.newStringMsg(topic, message).send(socket);
        boolean output = socket.sendMore(topic);
        //send message as byteArray
        output &= socket.send(SerializationUtils.serialize(message));

        if (output) {
            AtomicInteger prevCount = counterMessagesSent.get(configuration);
            prevCount.incrementAndGet();
            counterMessagesSent.put(configuration, prevCount);
        } else {
            AtomicInteger prevCount = counterMessagesNotSent.get(configuration);
            prevCount.incrementAndGet();
            counterMessagesNotSent.put(configuration, prevCount);
        }

    }

    private class ZeroMqAckReqProvider implements Runnable {

        //Not on the hot/low-latency path (just an ACK side-channel), so a small sleep
        //between iterations is fine and avoids burning a CPU core spinning if the
        //socket keeps throwing (e.g. context torn down) or replying instantly in a tight loop.
        private static final long SLEEP_MS_AFTER_ACK = 1L;
        private static final long SLEEP_MS_AFTER_ERROR = 50L;

        private ZMQ.Socket repSocket;

        public ZeroMqAckReqProvider(ZMQ.Socket repSocket) {
            this.repSocket = repSocket;
        }

        @Override public void run() {
            while (true) {
                try {
                    ZMsg zMsg = ZMsg.recvMsg(this.repSocket);
                    String message = zMsg.popString();
                    if (message.equalsIgnoreCase("OK")) {
                        OKReceived++;
                    } else {
                        logger.warn("not OK received ACK");
                    }

                    String reply = "OK OK";
                    this.repSocket.send(reply.getBytes(), 0);
                    Thread.sleep(SLEEP_MS_AFTER_ACK);

                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    logger.error("error receiving ACK publisher", e);
                    try {
                        //back off on repeated errors instead of tight-looping/log-flooding
                        Thread.sleep(SLEEP_MS_AFTER_ERROR);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

            }
        }
    }
}
