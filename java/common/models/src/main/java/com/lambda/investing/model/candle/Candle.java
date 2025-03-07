package com.lambda.investing.model.candle;

import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter @Setter public class Candle {

	private CandleType candleType;
	private String instrumentPk;
	private double high, low, open, close;
	private double highVolume, lowVolume, openVolume, closeVolume;
	private long timestamp;

	public Candle(CandleType candleType, String instrumentPk, double open, double high, double low, double close,
				  double highVolume, double lowVolume, double openVolume, double closeVolume, long timestamp) {
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
		this.timestamp = timestamp;

	}

	public Candle(CandleType candleType, String instrumentPk, double open, double high, double low, double close, long timestamp) {
		this.instrumentPk = instrumentPk;
		this.candleType = candleType;
		this.high = high;
		this.low = low;
		this.open = open;
		this.close = close;
		this.timestamp = timestamp;
	}

}
