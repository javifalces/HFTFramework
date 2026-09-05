package com.lambda.investing.market_data_connector.xchange;

import com.lambda.investing.market_data_connector.MarketDataConfiguration;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class BinanceXchangeMarketDataConfiguration implements MarketDataConfiguration {

	private String apiKey;
	private String secretKey;

	public BinanceXchangeMarketDataConfiguration(String apiKey, String secretKey) {
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