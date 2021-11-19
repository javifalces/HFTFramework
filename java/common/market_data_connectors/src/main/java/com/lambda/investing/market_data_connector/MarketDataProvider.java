package com.lambda.investing.market_data_connector;

public interface MarketDataProvider {

	void register(MarketDataListener listener);

	void deregister(MarketDataListener listener);

}
