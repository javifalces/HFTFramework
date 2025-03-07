package com.lambda.investing.connector;

import com.lambda.investing.model.messaging.TypeMessage;

public interface ConnectorPublisher {

	boolean publish(ConnectorConfiguration connectorConfiguration, TypeMessage typeMessage, String topic,
			String message);

	int getMessagesSent(ConnectorConfiguration configuration);
	int getMessagesFailed(ConnectorConfiguration configuration);

}
