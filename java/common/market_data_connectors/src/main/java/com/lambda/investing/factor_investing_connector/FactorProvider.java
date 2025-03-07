package com.lambda.investing.factor_investing_connector;

import com.lambda.investing.market_data_connector.MarketDataListener;

public interface FactorProvider {

    void register(FactorListener listener);

    void deregister(FactorListener listener);

    void reset();

}
