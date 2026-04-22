package com.lambda.investing.connector.ordinary;

import com.lambda.investing.connector.ConnectorConfiguration;

public class DisruptorConnectorConfiguration implements ConnectorConfiguration {

	@Override public String getConnectionConfiguration() {
		return "disruptor listener";
	}

	///is always equal
	@Override public int hashCode() {
		return 1;
	}

	@Override public boolean equals(Object obj) {
		return true;
	}
}
