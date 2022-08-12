package com.lambda.investing.model.market_data;

import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class DepthParquet extends CSVable {

	private static int MAX_LEVELS_PARQUET = 5;
	//	private String instrumentPK;
	private Long timestamp;
	private Double bidQuantity0, bidQuantity1, bidQuantity2, bidQuantity3, bidQuantity4;
	private Double askQuantity0, askQuantity1, askQuantity2, askQuantity3, askQuantity4;
	private Double bidPrice0, bidPrice1, bidPrice2, bidPrice3, bidPrice4;
	private Double askPrice0, askPrice1, askPrice2, askPrice3, askPrice4;
	//	private int levels;

	public int getLevels() {
		return MAX_LEVELS_PARQUET;
	}
	public DepthParquet() {
	}

	public DepthParquet(long timestamp, Double bidQuantity0, Double bidQuantity1, Double bidQuantity2,
			Double bidQuantity3, Double bidQuantity4, Double askQuantity0, Double askQuantity1, Double askQuantity2,
			Double askQuantity3, Double askQuantity4, Double bidPrice0, Double bidPrice1, Double bidPrice2,
			Double bidPrice3, Double bidPrice4, Double askPrice0, Double askPrice1, Double askPrice2, Double askPrice3,
			Double askPrice4) {
		this.timestamp = timestamp;
		this.bidQuantity0 = bidQuantity0;
		this.bidQuantity1 = bidQuantity1;
		this.bidQuantity2 = bidQuantity2;
		this.bidQuantity3 = bidQuantity3;
		this.bidQuantity4 = bidQuantity4;
		this.askQuantity0 = askQuantity0;
		this.askQuantity1 = askQuantity1;
		this.askQuantity2 = askQuantity2;
		this.askQuantity3 = askQuantity3;
		this.askQuantity4 = askQuantity4;
		this.bidPrice0 = bidPrice0;
		this.bidPrice1 = bidPrice1;
		this.bidPrice2 = bidPrice2;
		this.bidPrice3 = bidPrice3;
		this.bidPrice4 = bidPrice4;
		this.askPrice0 = askPrice0;
		this.askPrice1 = askPrice1;
		this.askPrice2 = askPrice2;
		this.askPrice3 = askPrice3;
		this.askPrice4 = askPrice4;
	}

	public DepthParquet(Depth depth) {
		this.timestamp = depth.getTimestamp();
		int levels = getLevels();
		switch (levels) {
			//			case 6:
			//				this.bidQuantity5 = depth.getBidsQuantities()[5];
			//				this.askQuantity5 = depth.getAsksQuantities()[5];
			//				this.askPrice5 = depth.getAsks()[5];
			//				this.bidPrice5 = depth.getBids()[5];
			case 5:
				this.bidQuantity4 = depth.getBidsQuantities()[4];
				this.askQuantity4 = depth.getAsksQuantities()[4];
				this.askPrice4 = depth.getAsks()[4];
				this.bidPrice4 = depth.getBids()[4];
			case 4:
				this.bidQuantity3 = depth.getBidsQuantities()[3];
				this.askQuantity3 = depth.getAsksQuantities()[3];
				this.askPrice3 = depth.getAsks()[3];
				this.bidPrice3 = depth.getBids()[3];
			case 3:
				this.bidQuantity2 = depth.getBidsQuantities()[2];
				this.askQuantity2 = depth.getAsksQuantities()[2];
				this.askPrice2 = depth.getAsks()[2];
				this.bidPrice2 = depth.getBids()[2];
			case 2:
				this.bidQuantity1 = depth.getBidsQuantities()[1];
				this.askQuantity1 = depth.getAsksQuantities()[1];
				this.askPrice1 = depth.getAsks()[1];
				this.bidPrice1 = depth.getBids()[1];
			case 1:
				this.bidQuantity0 = depth.getBidsQuantities()[0];
				this.askQuantity0 = depth.getAsksQuantities()[0];
				this.askPrice0 = depth.getAsks()[0];
				this.bidPrice0 = depth.getBids()[0];

		}

	}

	@Override public String toCSV(boolean withHeader) {
		return null;
	}

	@Override public Object getParquetObject() {
		return null;
	}
}

