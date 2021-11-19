package com.lambda.investing.market_data_connector.mock;

import com.lambda.investing.market_data_connector.MarketDataConfiguration;
import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class MockMarketDataConfiguration implements MarketDataConfiguration {

	private Instrument instrument;

	private int levels;

	private double startMidPrice = 100.;
	private double startMidQuantity = 10.;
	private double probabilityTrade = 0.1;

	private double delta;
	private double deltaQuantity;

	public MockMarketDataConfiguration(Instrument instrument, int levels, double delta, double deltaQuantity) {
		this.instrument = instrument;
		this.levels = levels;
		this.delta = delta;
		this.deltaQuantity = deltaQuantity;
	}

}
