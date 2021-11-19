package com.lambda.investing.connector;

public interface ConnectorProvider {

	void register(ConnectorConfiguration configuration, ConnectorListener listener);

	void deregister(ConnectorConfiguration configuration, ConnectorListener listener);

}
