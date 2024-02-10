package com.lambda.investing.connector;

public interface ConnectorProvider {

	void register(ConnectorConfiguration configuration, com.lambda.investing.connector.ConnectorListener listener);

	void deregister(ConnectorConfiguration configuration, ConnectorListener listener);


}
