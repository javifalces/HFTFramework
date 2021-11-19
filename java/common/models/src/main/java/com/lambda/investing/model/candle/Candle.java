package com.lambda.investing.model.candle;

import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class Candle {

	private CandleType candleType;
	private String instrumentPk;
	private double high, low, open, close;

	public Candle(CandleType candleType, String instrumentPk, double open, double high, double low, double close) {
		this.instrumentPk = instrumentPk;
		this.candleType = candleType;
		this.high = high;
		this.low = low;
		this.open = open;
		this.close = close;
	}

}
