package com.lambda.investing.connector;

import com.lambda.investing.model.messaging.TypeMessage;

public interface ConnectorListener {

	void onUpdate(ConnectorConfiguration configuration, long timestampReceived, TypeMessage typeMessage,
			String content);

}
