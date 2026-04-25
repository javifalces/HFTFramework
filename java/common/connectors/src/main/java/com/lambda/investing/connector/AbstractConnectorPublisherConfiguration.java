package com.lambda.investing.connector;

public abstract class AbstractConnectorPublisherConfiguration implements ConnectorConfiguration {

    //dummy class

    /// is always equal
    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object obj) {
        return true;
    }
}


