package com.lambda.investing.market_data_connector.xchange;

import com.lambda.investing.market_data_connector.MarketDataConfiguration;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class KrakenMarketDataConfiguration implements MarketDataConfiguration {

	private String apiKey;//c39e5863fc6488b64f4ff4715295767d
	private String secretKey;//5T6YdCjE9HjYGulFh16Zf6XQ62M8cHZ9gQIJrrckrwX83O4tYwtsBH4Z52UwkSViHV3CewwYTxmh9xZ6vmLB+g==

	public KrakenMarketDataConfiguration(String apiKey, String secretKey) {
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