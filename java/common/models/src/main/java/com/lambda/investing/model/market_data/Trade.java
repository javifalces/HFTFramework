package com.lambda.investing.model.market_data;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import static com.lambda.investing.model.Util.GSON_STRING;
import static com.lambda.investing.model.Util.getDatePythonUTC;

@Getter @Setter public class Trade extends CSVable {

	private String id;
	private String instrument;
	private Long timestamp;
	private Double quantity, price;//TODO change to Bigdecimal
	private String algorithmInfo;//just for backtesting
	private Verb verb;
	private long timeToNextUpdateMs = Long.MIN_VALUE;

	private String generateId() {
		return UUID.randomUUID().toString();
	}

	public Trade() {
		id = generateId();
	}

	public Trade(TradeParquet tradeParquet, Instrument instrument) {
		this.instrument = instrument.getPrimaryKey();
		this.timestamp = tradeParquet.getTimestamp();
		this.quantity = tradeParquet.getQuantity();
		this.price = tradeParquet.getPrice();
		this.id = generateId();

	}

	public Trade(ExecutionReport executionReport) {
		this.instrument = executionReport.getInstrument();
		this.timestamp = executionReport.getTimestampCreation();
		this.quantity = executionReport.getLastQuantity();
		this.price = executionReport.getPrice();
		this.algorithmInfo = executionReport.getAlgorithmInfo();
		this.verb = Verb.OtherSideVerb(executionReport.getVerb());//our buy , is a sell for the market
		id = generateId();

	}

	public boolean isTradeValid(Depth lastDepth) {
		boolean priceIsInBounds = true;
		if (lastDepth != null) {
			try {
				Double worstAsk = lastDepth.getAsks()[lastDepth.getAskLevels() - 1];
				Double worstBid = lastDepth.getBids()[lastDepth.getBidLevels() - 1];
				if (!Double.isNaN(worstAsk) && !Double.isNaN(worstBid)) {
					priceIsInBounds = this.price < worstAsk && this.price > worstBid;
				}
			} catch (Exception e) {
				priceIsInBounds = true;
			}
		}
		return priceIsInBounds;
	}

	@Override public String toString() {
		return GSON_STRING.toJson(this);
	}

	public static StringBuilder headerCSV() {
		//,price,quantity
		StringBuilder stringBuffer = new StringBuilder();
		return stringBuffer.append(",timestamp,price,quantity");
	}

	public String toCSV(boolean withHeader) {
		StringBuilder stringBuffer = new StringBuilder();
		if (withHeader) {
			//,price,quantity
			stringBuffer.append(headerCSV());
			stringBuffer.append(System.lineSeparator());
		}
		//2019-11-09 08:42:24.142302
		stringBuffer.append(getDatePythonUTC(timestamp));
		stringBuffer.append(",");
		stringBuffer.append(timestamp);
		stringBuffer.append(",");
		stringBuffer.append(price);
		stringBuffer.append(",");
		stringBuffer.append(quantity);
		return stringBuffer.toString();
	}

	@Override public Object getParquetObject() {
		return new TradeParquet(this);
	}
}
