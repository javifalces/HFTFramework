package com.lambda.investing.connector.ordinary;

import com.lambda.investing.connector.AbstractConnectorPublisherConfiguration;
import com.lambda.investing.connector.ConnectorConfiguration;

public class OrdinaryConnectorConfiguration extends AbstractConnectorPublisherConfiguration {
	@Override public String getConnectionConfiguration() {
		return "ordinary listener";
	}

}


