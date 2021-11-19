package com.lambda.investing.connector.zero_mq;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.model.messaging.TopicUtils;
import com.lambda.investing.model.messaging.TypeMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import com.lambda.investing.model.asset.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter @Setter @ToString public class ZeroMqConfiguration implements ConnectorConfiguration {

	private String host;
	private String topic;
	private int port;

	public ZeroMqConfiguration() {
	}

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

	/**
	 * For creation of a list of topics of the instrument
	 *
	 * @param host
	 * @param port
	 * @param instrument
	 * @return
	 */
	public static List<ZeroMqConfiguration> getMarketDataZeroMqConfiguration(String host, int port,
			Instrument instrument) {
		List<ZeroMqConfiguration> output = new ArrayList<>();

		for (TypeMessage typeMessage : TypeMessage.values()) {
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
		return port == that.port && Objects.equals(host, that.host);
	}

	@Override public int hashCode() {
		return Objects.hash(host, port);
	}
}
