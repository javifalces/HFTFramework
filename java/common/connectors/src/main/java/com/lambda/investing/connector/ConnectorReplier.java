package com.lambda.investing.connector;

import com.lambda.investing.model.messaging.TypeMessage;

public interface ConnectorReplier {
    String reply(long timestampReceived, String content);

}
