package com.lambda.investing.factor_investing_connector;

public interface FactorProvider {

    void register(FactorListener listener);

    void deregister(FactorListener listener);

    void reset();

}
