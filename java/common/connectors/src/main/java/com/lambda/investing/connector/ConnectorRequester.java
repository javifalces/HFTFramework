package com.lambda.investing.connector;

public interface ConnectorRequester {

	String request(ConnectorConfiguration connectorConfiguration, String message);

}
