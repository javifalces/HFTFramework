package com.lambda.investing.connector.zero_mq;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.model.messaging.TopicUtils;
import com.lambda.investing.model.messaging.TypeMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import com.lambda.investing.model.asset.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
public class ZeroMqConfiguration implements ConnectorConfiguration {
	private static final Logger logger = LogManager.getLogger(ZeroMqConfiguration.class);
	private String protocol = "tcp";//ipc or tcp
	private String host;
	private String topic;
	private int port;
	private String ipAddress;
	boolean ipcEnabled;
	/**
	 * ZeroMQ high-water-mark applied to the socket(s) created for this configuration
	 * (see {@code ZeroMqProvider#getSubscribeSocket} / {@code ZeroMqPublisher#getPublishSocket}).
	 * <p>
	 * Defaults to {@code 1} — the historical value, appropriate for market-data streams where
	 * dropping a stale tick under load is an acceptable trade-off for low latency. Trading
	 * channels (order requests / execution reports) must never silently drop a message this way,
	 * so connectors that own those channels (e.g. {@code AbstractBrokerTradingEngine},
	 * {@code ZeroMqTradingEngineConnector}) raise this value before any socket is created.
	 */
	private int hwm = 1;
	private static ZContext Z_CONTEXT;
	private static final Object lockContext = new Object();

	public ZeroMqConfiguration() {
	}

	public static ZContext GetZContext() {
		synchronized (lockContext) {
			if (Z_CONTEXT == null) {
				Z_CONTEXT = new ZContext();
			}
			return Z_CONTEXT;
		}
	}

	;

	public ZeroMqConfiguration(ZeroMqConfiguration zeroMqConfiguration) {
		this.host = zeroMqConfiguration.getHost();
		this.topic = zeroMqConfiguration.getTopic();
		this.port = zeroMqConfiguration.getPort();
		this.protocol = zeroMqConfiguration.getProtocol();
		this.ipAddress = zeroMqConfiguration.getIpAddress();
		this.hwm = zeroMqConfiguration.getHwm();
		if (zeroMqConfiguration.ipcEnabled) {
			this.ipcEnabled = true;
		}
	}

	public ZeroMqConfiguration(String host, int port, String topic) {
		this.host = host;
		this.topic = topic;
		this.port = port;
	}

	public ZeroMqConfiguration(String protocol, String host, int port, String topic) {
		this.protocol = protocol;
		this.host = host;
		this.topic = topic;
		this.port = port;
		if (protocol.equalsIgnoreCase("ipc")) {
			this.ipcEnabled = true;
		}
	}

	/**
	 * Configures this endpoint to use ZeroMQ IPC (Unix domain sockets / named pipes).
	 *
	 * @param directory Base directory for socket files (e.g. "/home/user/lambdaIPC/marketdata").
	 *                  The actual socket file will be {@code directory/zmq.sock}.
	 *                  The directory is created automatically if absent.
	 *                  <p>
	 *                  NOTE: jeromq supports IPC on Windows only from version 0.5.0+.
	 *                  With jeromq < 0.5.0 on Windows, use TCP instead.
	 */
	public void setIpc(String directory) {
		if (directory == null || directory.isEmpty()) {
			return;
		}

		this.protocol = "ipc";
		this.host = directory; // kept for logging / ACK helpers
		this.ipcEnabled = true;
		// Create the *parent* directory so ZeroMQ can place a socket FILE inside it.
		// IMPORTANT: do NOT call mkdirs() on the socket file path itself – if a directory
		// already exists at that path ZeroMQ cannot create the socket file there.
		// Build the ipc:// address pointing to a socket FILE inside the directory.
		// Normalise separators to forward-slash so the address is valid on all platforms.
		String normalised = directory.replace('\\', '/');
		String socketFile = normalised + "/zmq.sock";
		this.ipAddress = socketFile.startsWith("/") ? "ipc://" + socketFile : "ipc:///" + socketFile;
		logger.info("IPC configured: directory={}, socket={}", directory, this.ipAddress);
	}

	public String getUrl() {
		if (ipAddress != null) {
			return ipAddress;
		}

		String url = String.format("%s://%s:%d", getProtocol(), getHost(), getPort());
		return url;
	}

	public String getBindUrl() {
		if (ipAddress != null) {
			// IPC uses file paths – no wildcard; bind and connect use the same path
			return ipAddress;
		}

		String url = String.format("%s://*:%d", getProtocol(), getPort());
		return url;
	}

	/**
	 * Returns the URL to use for the ACK (REP/REQ) companion socket.
	 * For TCP this is port+1; for IPC this is a separate socket FILE (ack.sock) in the same directory.
	 */
	public String getAckBindUrl() {
		if (!ipcEnabled) {
			return String.format("%s://*:%d", getProtocol(), getPort() + 1);
		} else {
			// The parent directory already exists (created in setIpc).
			// Use a socket FILE – do NOT create a directory at this path.
			String normalised = getHost().replace('\\', '/');
			String socketFile = normalised + "/ack.sock";
			return socketFile.startsWith("/") ? "ipc://" + socketFile : "ipc:///" + socketFile;
		}
	}

	public String getAckConnectUrl() {
		if (!ipcEnabled) {
			return String.format("%s://%s:%d", getProtocol(), getHost(), getPort() + 1);
		} else {
			// The parent directory already exists (created in setIpc).
			// Use a socket FILE – do NOT create a directory at this path.
			String normalised = getHost().replace('\\', '/');
			String socketFile = normalised + "/ack.sock";
			return socketFile.startsWith("/") ? "ipc://" + socketFile : "ipc:///" + socketFile;
		}
	}

	/**
	 * For creation of a list of topics of the instrument
	 *
	 * @param host host
	 * @param port port
	 * @param instrument instrument
	 * @return list
	 */
	public static List<ZeroMqConfiguration> getMarketDataZeroMqConfiguration(String host, int port, Instrument instrument) {
		List<ZeroMqConfiguration> output = new ArrayList<>();

		for(TypeMessage typeMessage:TypeMessage.values()) {
			output.add(new ZeroMqConfiguration(host, port, TopicUtils.getTopic(instrument, typeMessage)));
		}

		return output;
	}

	@Override public String getConnectionConfiguration() {
		String out = String.format("%s:%s", this.host, this.port);
		if (this.topic != null && !this.topic.trim().equalsIgnoreCase("")) {
			out += " on topic " + this.topic;
		}
		return out;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ZeroMqConfiguration that = (ZeroMqConfiguration) o;
		return port == that.port && Objects.equals(host, that.host) && Objects.equals(protocol, that.protocol);
	}

	@Override public int hashCode() {
		return Objects.hash(protocol, host, port);
	}

}
