package com.lambda.investing.market_data_connector.binance;

import com.lambda.investing.market_data_connector.MarketDataConfiguration;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class BinanceMarketDataConfiguration implements MarketDataConfiguration {

	private String apiKey;
	private String secretKey;

	public BinanceMarketDataConfiguration(String apiKey, String secretKey) {
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
