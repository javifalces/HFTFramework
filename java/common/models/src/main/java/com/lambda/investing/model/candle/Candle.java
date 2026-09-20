package com.lambda.investing.model.candle;

import com.alibaba.fastjson2.annotation.JSONField;
import com.lambda.investing.model.CSVable;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.TradeParquet;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

import static com.lambda.investing.model.Util.getDatePythonUTC;
import static com.lambda.investing.model.Util.toJsonString;

@Getter
@Setter
public class Candle extends CSVable {

	public static String REQUESTED_CANDLES_INFO = "candles";

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

	public static StringBuilder headerCSV() {
		//			,ask0,ask1,ask2,ask3,ask4,ask_quantity0,ask_quantity1,ask_quantity2,ask_quantity3,ask_quantity4,bid0,bid1,bid2,bid3,bid4,bid_quantity0,bid_quantity1,bid_quantity2,bid_quantity3,bid_quantity4

		StringBuilder stringBuffer = new StringBuilder();
		return stringBuffer
				.append(",date_time,tick_num,open,high,low,close,volume,cum_buy_volume,cum_ticks,cum_dollar_value,datetime");

	}

	@Override
	public String toCSV(boolean withHeader) {
		StringBuilder stringBuffer = new StringBuilder();
		if (withHeader) {
			stringBuffer.append(headerCSV());
			stringBuffer.append(System.lineSeparator());
		}

		stringBuffer.append(getDatePythonUTC(timestamp));//1714348560
		stringBuffer.append(',');
		stringBuffer.append(0);//tick_num
		stringBuffer.append(',');
		stringBuffer.append(open);
		stringBuffer.append(',');
		stringBuffer.append(high);
		stringBuffer.append(',');
		stringBuffer.append(low);
		stringBuffer.append(',');
		stringBuffer.append(close);
		stringBuffer.append(',');
		stringBuffer.append(closeVolume);
		stringBuffer.append(',');
		stringBuffer.append(closeVolume - openVolume); //cum_buy_volume
		stringBuffer.append(',');
		stringBuffer.append(0); //cum_ticks
		stringBuffer.append(',');
		stringBuffer.append(0); //cum_dollar_value
		stringBuffer.append(',');
		stringBuffer.append(new Date(timestamp));//29/04/2024

		return stringBuffer.toString();
	}

	@JSONField(serialize = false, deserialize = false)
	@Override
	public Object getParquetObject() {
		return this;
	}

	@Override
	public String toString() {
		return toJsonString(this);
	}

}
