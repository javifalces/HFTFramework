package com.lambda.investing.model.candle;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class Candle {

	private CandleType candleType;
	private String instrumentPk;
	private double high, low, open, close;
	private double highVolume, lowVolume, openVolume, closeVolume;

	public Candle(CandleType candleType, String instrumentPk, double open, double high, double low, double close,
			double highVolume, double lowVolume, double openVolume, double closeVolume) {
		this.instrumentPk = instrumentPk;
		this.candleType = candleType;
		this.high = high;
		this.low = low;
		this.open = open;
		this.close = close;

		this.highVolume = highVolume;
		this.lowVolume = lowVolume;
		this.openVolume = openVolume;
		this.closeVolume = closeVolume;

	}

	public Candle(CandleType candleType, String instrumentPk, double open, double high, double low, double close) {
		this.instrumentPk = instrumentPk;
		this.candleType = candleType;
		this.high = high;
		this.low = low;
		this.open = open;
		this.close = close;
	}

}
