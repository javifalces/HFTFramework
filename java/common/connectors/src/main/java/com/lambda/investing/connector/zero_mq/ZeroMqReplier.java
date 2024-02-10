package com.lambda.investing.connector.zero_mq;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.*;
import com.lambda.investing.model.messaging.TopicUtils;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZeroMqReplier {
    private static final Map<Integer, ZMQ.Socket> PORTS_TAKEN_REQ = new ConcurrentHashMap<>();
    Logger logger = LogManager.getLogger(ZeroMqReplier.class);

    private final ZMQ.Socket socket;
    private final ZeroMqConfiguration zeroMqConfiguration;
    private final ConnectorReplier connectorReplier;
    private final ZContext context = ZeroMqConfiguration.GetZContext();

    public ZeroMqReplier(ZeroMqConfiguration configuration, ConnectorReplier replier) {
        zeroMqConfiguration = configuration;
        connectorReplier = replier;
        socket = getSocket(zeroMqConfiguration);
    }

    private synchronized ZMQ.Socket getSocket(ZeroMqConfiguration configuration) {
        //		http://zguide.zeromq.org/java:hwserver
        ZMQ.Socket listenSocket = null;
        if (!PORTS_TAKEN_REQ.containsKey(configuration.getPort())) {
            try {
                listenSocket = context.createSocket(ZMQ.REP);

//                listenSocket.setHWM(1);
                listenSocket.setLinger(0);

                String url = configuration.getBindUrl();
                logger.info("Creating Req socket {}", url);
                PORTS_TAKEN_REQ.put(configuration.getPort(), listenSocket);
                listenSocket.bind(url);
            } catch (Exception e) {
                String messageError = Configuration.formatLog("Error creating Req socket {}  {}", configuration, e.getMessage());
                logger.error("{}", messageError, e);
                System.err.println(messageError);
                e.printStackTrace();
                System.exit(1);
            }


        } else {
            listenSocket = PORTS_TAKEN_REQ.get(configuration.getPort());
        }
        return listenSocket;
    }

    public void start() throws IOException {
        Thread thread = new Thread(new ZeroMqThreadReceiver(zeroMqConfiguration), "GymActionListener");
        thread.start();
    }

    private class ZeroMqThreadReceiver implements Runnable {

        private ZeroMqConfiguration zeroMqConfiguration;
        final AtomicBoolean running = new AtomicBoolean(false);

        public ZeroMqThreadReceiver(ZeroMqConfiguration zeroMqConfiguration) {
            this.zeroMqConfiguration = zeroMqConfiguration;
            running.set(true);
        }

        @Override
        public void run() {
            while (running.get()) {
                try {
                    // Read envelope with topic
                    synchronized (socket) {
                        ZMsg zMsg = ZMsg.recvMsg(socket);
                        //Read message contents
//						String topic = zMsg.popString();
                        String message = zMsg.popString();
                        try {
                            String reply = connectorReplier.reply(System.currentTimeMillis(), message);
                            //reply
                            socket.send(reply.getBytes(), 0);

                        } catch (Exception e) {
                            logger.error("exception reading zeroMq \nmessage:{}\n", message, e);
                            e.printStackTrace();
                        }
                    }

                } catch (Exception e) {
                    logger.error("error reading ZeroMQ message ", e);

                } finally {
                    Thread.onSpinWait();//to not occupy the cpu
                }
                //
            }
            System.err.print("end of zeroMQProvider?");

        }
    }


}

