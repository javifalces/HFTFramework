package com.lambda.investing.algo_trading;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZeroMqTradingConfiguration {

	private String tradeEngineHost;
	private String marketDataHost;
	private String factorPublisherHost;

	private int tradeEnginePort;
	private int marketDataPort;
	private int factorPublisherPort;

	private boolean paperTrading;
	private boolean demoTrading;
	private String[] instrumentPks;

	private AlgorithmConfiguration algorithm;

	public ZeroMqTradingConfiguration() {
	}
}
