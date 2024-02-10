package com.lambda.investing.connector;

public interface ConnectorReplier {
    String reply(long timestampReceived, String content);

}
