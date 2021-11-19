package com.lambda.investing.connector.ordinary;

import com.lambda.investing.connector.ConnectorConfiguration;

public class OrdinaryConnectorConfiguration implements ConnectorConfiguration {

	@Override public String getConnectionConfiguration() {
		return "ordinary listener";
	}
	//dummy class

	///is always equal
	@Override public int hashCode() {
		return 1;
	}

	@Override public boolean equals(Object obj) {
		return true;
	}
}


