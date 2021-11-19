package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.trading_engine_connector.AbstractBrokerTradingEngine;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class AlgorithmConnectorConfiguration {

	private TradingEngineConnector tradingEngineConnector;
	private MarketDataProvider marketDataProvider;

	public AlgorithmConnectorConfiguration(TradingEngineConnector tradingEngineConnector,
			MarketDataProvider marketDataProvider) {
		this.tradingEngineConnector = tradingEngineConnector;
		this.marketDataProvider = marketDataProvider;
	}

}
