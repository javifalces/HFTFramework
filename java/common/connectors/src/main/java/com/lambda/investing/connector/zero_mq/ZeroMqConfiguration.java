package com.lambda.investing.connector.zero_mq;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.model.messaging.TopicUtils;
import com.lambda.investing.model.messaging.TypeMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import com.lambda.investing.model.asset.*;
import org.zeromq.ZContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
public class ZeroMqConfiguration implements ConnectorConfiguration {
	private String protocol = "tcp";
	private String host;
	private String topic;
	private int port;
	private String ipAddress;
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
	}

	public void setIpc(String address) {
		this.protocol = "ipc";
		this.host = address;//for print
		this.ipAddress = Configuration.formatLog("ipc:///{}", address);
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
			return String.format("%s/*", this.ipAddress);
		}

		String url = String.format("%s://*:%d", getProtocol(), getPort());
		return url;
	}

	/**
	 * For creation of a list of topics of the instrument
	 *
	 * @param host
	 * @param port
	 * @param instrument
	 * @return
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
