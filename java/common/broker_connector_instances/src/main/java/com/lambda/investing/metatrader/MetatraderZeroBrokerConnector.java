package com.lambda.investing.metatrader;

import com.binance.api.client.BinanceApiClientFactory;
import com.lambda.investing.binance.BinanceBrokerConnector;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import lombok.Getter;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter

public class MetatraderZeroBrokerConnector {

	private String host;
	/*
	same configuration as in metatrader
	input int PUSH_PORT = 32769;
	input int PULL_PORT = 32768;
	input int PUB_PORT=32770;
	 */
	private int portPublisher;//used in mt to publish market data
	private int portPush;//used in mt to listen to Orders
	private int portPull;//used in mt to execution reports orders

	private ZeroMqConfiguration publisherZeroMqConfiguration;
	private ZeroMqProvider publisherProvider;

	//	public static NumberFormat NUMBER_FORMAT = NumberFormat
	//			.getInstance(Locale.US);//US has dot instead of commas in decimals

	private static Map<String, MetatraderZeroBrokerConnector> instances = new ConcurrentHashMap<>();

	public static MetatraderZeroBrokerConnector getInstance(String host, int portPublisher, int portPush,
			int portPull) {
		String key = host + "_" + String.valueOf(portPublisher) + "_" + String.valueOf(portPush) + "_" + String
				.valueOf(portPull);
		MetatraderZeroBrokerConnector output = instances
				.getOrDefault(key, new MetatraderZeroBrokerConnector(host, portPublisher, portPush, portPull));
		instances.put(key, output);
		return output;
	}

	private MetatraderZeroBrokerConnector(String host, int portPublisher, int portPush, int portPull) {
		this.host = host;
		this.portPublisher = portPublisher;
		this.portPush = portPush;
		this.portPull = portPull;

		this.publisherZeroMqConfiguration = new ZeroMqConfiguration(host, portPublisher, "");
		this.publisherProvider = ZeroMqProvider.getInstance(this.publisherZeroMqConfiguration, 0);
		this.publisherProvider.setParsedObjects(false);

	}

}
