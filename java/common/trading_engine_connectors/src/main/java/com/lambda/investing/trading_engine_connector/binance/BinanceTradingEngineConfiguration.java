package com.lambda.investing.trading_engine_connector.binance;

import com.lambda.investing.trading_engine_connector.TradingEngineConfiguration;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class BinanceTradingEngineConfiguration implements TradingEngineConfiguration {

	private String apiKey;
	private String secretKey;

	public BinanceTradingEngineConfiguration(String apiKey, String secretKey) {
		this.apiKey = apiKey;
		this.secretKey = secretKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
}