package com.lambda.investing.model.market_data;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class TradeParquet extends CSVable {

	private Long timestamp;
	private Double quantity, price;//TODO change to Bigdecimal

	public TradeParquet() {
	}

	public TradeParquet(Trade trade) {
		this.timestamp = trade.getTimestamp();
		this.quantity = trade.getQuantity();
		this.price = trade.getPrice();
	}

	public TradeParquet(long timestamp, Double quantity, Double price) {
		this.timestamp = timestamp;
		this.quantity = quantity;
		this.price = price;
	}

	@Override public String toCSV(boolean withHeader) {
		return null;
	}

	@Override public Object getParquetObject() {
		return null;
	}
}
