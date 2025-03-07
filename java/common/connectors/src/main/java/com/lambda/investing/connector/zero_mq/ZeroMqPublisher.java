package com.lambda.investing.connector.zero_mq;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ZeroMqPublisher implements ConnectorPublisher {

    private static final int COMMAND_RETRIES_PUBLISH = 1;//zero MQ will retry until ACK
    private static final Map<Integer, ZMQ.Socket> PORTS_TAKEN_PUB = new ConcurrentHashMap<>();
    private static final Map<Integer, ZMQ.Socket> PORTS_TAKEN_REQ_ACK = new ConcurrentHashMap<>();
    Logger logger = LogManager.getLogger(ZeroMqPublisher.class);
    private int OKReceived = 0;

    Map<ZeroMqConfiguration, AtomicInteger> counterMessagesSent = new HashMap<>();
    Map<ZeroMqConfiguration, AtomicInteger> counterMessagesNotSent = new HashMap<>();

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
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(this.name + "-%d").build();
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
        ZMQ.Socket publishSocket = null;
        ZMQ.Socket reqSocket = null;
        if (!PORTS_TAKEN_PUB.containsKey(configuration.getPort())) {
            publishSocket = context.createSocket(ZMQ.PUB);
            publishSocket.setHWM(1);
            publishSocket.setLinger(0);
            if (isServer) {
                String url = configuration.getBindUrl();
                logger.info("Creating PUB server socket {} : {}", url, configuration);
                PORTS_TAKEN_PUB.put(configuration.getPort(), publishSocket);
                publishSocket.bind(url);
            } else {
                String url = configuration.getUrl();
                logger.info("Connecting PUB socket {} : {}", url, configuration);
                PORTS_TAKEN_PUB.put(configuration.getPort(), publishSocket);
                publishSocket.connect(url);
            }

            ZContext contextAck = ZeroMqConfiguration.GetZContext();
            reqSocket = contextAck.createSocket(ZMQ.REP);
            reqSocket.setHWM(1);
            reqSocket.setLinger(0);
            if (isServer) {
                String urlAck = String.format("%s://*:%d", configuration.getProtocol(), configuration.getPort() + 1);
                logger.info("Creating REQ server socket {} ", urlAck);
                PORTS_TAKEN_REQ_ACK.put(configuration.getPort() + 1, publishSocket);
                reqSocket.bind(urlAck);
            } else {
                String urlAck = String.format("%s://%s:%d", configuration.getProtocol(), configuration.getHost(), configuration.getPort() + 1);
                logger.info("Connecting REQ socket {} ", urlAck);
                PORTS_TAKEN_REQ_ACK.put(configuration.getPort() + 1, publishSocket);
                reqSocket.connect(urlAck);
            }


            new Thread(new ZeroMqAckReqProvider(reqSocket), "listen_req_" + configuration.getPort() + 1).start();


        } else {
            publishSocket = PORTS_TAKEN_PUB.get(configuration.getPort());
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
                                     String topic, String message) {
        if (!(connectorConfiguration instanceof ZeroMqConfiguration)) {
            logger.error("configuration is not ZeroMqConfiguration");
            return false;
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

    private synchronized void send(String message, ZeroMqConfiguration configuration, String topic, long timestamp,
                                   ZMQ.Socket socket) {
        if (!counterMessagesSent.containsKey(configuration)) {
            counterMessagesSent.put(configuration, new AtomicInteger(0));
        }
        if (!counterMessagesNotSent.containsKey(configuration)) {
            counterMessagesNotSent.put(configuration, new AtomicInteger(0));
        }
        if ((topic.trim().length() == 0) || (message.trim().length() == 0))
            return;

        logger.debug("Sending to zeroMq {} :\n {}", topic, message);
        //		return socket.send(message.getBytes(ZMQ.CHARSET));
        long elapsed = System.currentTimeMillis() - timestamp;
        logger.debug("[ZEROMQ]Took {} ms to process message", elapsed);

        //		boolean output = ZMsg.newStringMsg(topic, message).send(socket);
        boolean output = socket.sendMore(topic);
        output &= socket.send(message);

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

                } catch (Exception e) {
                    logger.error("error receiving ACK publisher", e);
                }

            }
        }
    }
}
