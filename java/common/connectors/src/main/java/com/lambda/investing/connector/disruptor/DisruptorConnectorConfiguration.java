package com.lambda.investing.connector.disruptor;

import com.lambda.investing.connector.AbstractConnectorPublisherConfiguration;
import com.lambda.investing.connector.ConnectorConfiguration;

public class DisruptorConnectorConfiguration extends AbstractConnectorPublisherConfiguration {
	@Override public String getConnectionConfiguration() {
		return "disruptor listener";
	}
}
