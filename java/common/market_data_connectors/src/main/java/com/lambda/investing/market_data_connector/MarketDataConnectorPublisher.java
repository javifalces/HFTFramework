package com.lambda.investing.market_data_connector;

import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;

public interface MarketDataConnectorPublisher {

	void start();
	void stop();

	void notifyDepth(String topic,Depth depth);
	void notifyTrade(String topic,Trade trade);

}
