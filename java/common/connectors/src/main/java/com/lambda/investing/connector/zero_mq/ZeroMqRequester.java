package com.lambda.investing.connector.zero_mq;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorRequester;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZeroMqRequester implements ConnectorRequester {

	private static String KO_REQUEST = "KO";
	private static final Map<Integer, ZMQ.Socket> PORTS_TAKEN_REQ = new ConcurrentHashMap<>();
	Logger logger = LogManager.getLogger(ZeroMqRequester.class);

	private ZMQ.Socket getPublishSocket(ZeroMqConfiguration configuration) {
		//		http://zguide.zeromq.org/java:hwserver
		ZMQ.Socket publishSocket = null;
		if (!PORTS_TAKEN_REQ.containsKey(configuration.getPort())) {
			ZContext context = new ZContext();
			publishSocket = context.createSocket(ZMQ.REQ);
			publishSocket.setHWM(1);
			publishSocket.setLinger(0);
			String url = String.format("tcp://%s:%d", configuration.getHost(), configuration.getPort());
			logger.info("Connecting Requester socket {} : {}", url, configuration);
			PORTS_TAKEN_REQ.put(configuration.getPort(), publishSocket);
			publishSocket.connect(url);

		} else {
			publishSocket = PORTS_TAKEN_REQ.get(configuration.getPort());
		}

		return publishSocket;

	}

	private void reconnect(ZeroMqConfiguration zeroMqConfiguration, ZMQ.Socket socket) {
		String url = String.format("tcp://%s:%d", zeroMqConfiguration.getHost(), zeroMqConfiguration.getPort());
		socket.connect(url);

	}

	@Override public synchronized String request(ConnectorConfiguration connectorConfiguration, String message) {
		String received = KO_REQUEST;
		connectorConfiguration = connectorConfiguration instanceof ZeroMqConfiguration ?
				((ZeroMqConfiguration) connectorConfiguration) :
				null;
		if (connectorConfiguration == null) {
			logger.error("configuration is not ZeroMqConfiguration");
			return received;
		}
		ZeroMqConfiguration zeroMqConfiguration = (ZeroMqConfiguration) connectorConfiguration;
		ZMQ.Socket socket = getPublishSocket(zeroMqConfiguration);

		logger.debug("Sending to zeroMq {} :\n {}", zeroMqConfiguration.getTopic(), message);
		boolean output = false;

		try {
			output = socket.send(message.getBytes(ZMQ.CHARSET), 0);
			received = socket.recvStr();
		} catch (Exception ex) {
			logger.error("Error sending message {}\n{}", message, ex);
			reconnect(zeroMqConfiguration, socket);
		}
		return received;

	}
}
