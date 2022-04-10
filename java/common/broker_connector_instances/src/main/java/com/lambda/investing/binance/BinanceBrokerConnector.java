package com.lambda.investing.binance;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import lombok.Getter;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter public class BinanceBrokerConnector {

	private BinanceApiClientFactory factory;
	public static NumberFormat NUMBER_FORMAT = NumberFormat
			.getInstance(Locale.US);//US has dot instead of commas in decimals

	private static Map<String, BinanceBrokerConnector> instances = new ConcurrentHashMap<>();

	public static BinanceBrokerConnector getInstance(String apiKey, String secretKey) {
		String key = apiKey + secretKey;
		BinanceBrokerConnector output = instances.getOrDefault(key, new BinanceBrokerConnector(apiKey, secretKey));
		instances.put(key, output);
		return output;
	}

	private BinanceApiWebSocketClient webSocketClient;
	private BinanceApiAsyncRestClient asyncRestClient;
	private BinanceApiRestClient restClient;
	private String apiKey, secretKey;

	/**
	 * @param apiKey    4xCBC1cEehCKph4HGA9DTzi9x3190L606HiH2CYWXMQB53K69mczn4rlqZZiXBKV
	 * @param secretKey tvmuJGAVZymGrtR17UiojsXxqseoJHeBVTe0KfpiWX4xH0Y1LorGKL8KV9lfeGnM
	 */
	private BinanceBrokerConnector(String apiKey, String secretKey) {
		this.apiKey = apiKey;
		this.secretKey = secretKey;

		resetClient();
	}

	public void resetClient() {
		factory = BinanceApiClientFactory.newInstance(apiKey, secretKey);
		this.restClient = factory.newRestClient();
		this.webSocketClient = factory.newWebSocketClient();
		this.asyncRestClient = factory.newAsyncRestClient();
	}

}
