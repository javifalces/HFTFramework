package com.lambda.investing.trading_engine_connector.xchange;

import com.lambda.investing.trading_engine_connector.TradingEngineConfiguration;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class KrakenTradingEngineConfiguration implements TradingEngineConfiguration {

	private String apiKey;
	private String secretKey;

	public KrakenTradingEngineConfiguration(String apiKey, String secretKey) {
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