package com.binance.api.client.domain.event;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.binance.api.client.constant.BinanceApiConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * BookTickerEvent event for a symbol. Pushes any update to the best bid or
 * ask's price or quantity in real-time for a specified symbol.
 */
@JsonIgnoreProperties(ignoreUnknown = true) public class BookTickerEvent {

	@JsonProperty("u") private long updateId;

	@JsonProperty("s") private String symbol;

	@JsonProperty("b") private String bidPrice;

	@JsonProperty("B") private String bidQuantity;

	@JsonProperty("a") private String askPrice;

	@JsonProperty("A") private String askQuantity;

	public BookTickerEvent() {
		super();
	}

	public BookTickerEvent(long updateId, String symbol, String bidPrice, String bidQuantity, String askPrice,
			String askQuantity) {
		super();
		this.updateId = updateId;
		this.symbol = symbol;
		this.bidPrice = bidPrice;
		this.bidQuantity = bidQuantity;
		this.askPrice = askPrice;
		this.askQuantity = askQuantity;
	}

	public BookTickerEvent(String symbol, String bidPrice, String bidQuantity, String askPrice, String askQuantity) {
		super();
		this.symbol = symbol;
		this.bidPrice = bidPrice;
		this.bidQuantity = bidQuantity;
		this.askPrice = askPrice;
		this.askQuantity = askQuantity;
	}

	public long getUpdateId() {
		return updateId;
	}

	public void setUpdateId(long updateId) {
		this.updateId = updateId;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getBidPrice() {
		return bidPrice;
	}

	public void setBidPrice(String bidPrice) {
		this.bidPrice = bidPrice;
	}

	public String getBidQuantity() {
		return bidQuantity;
	}

	public void setBidQuantity(String bidQuantity) {
		this.bidQuantity = bidQuantity;
	}

	public String getAskPrice() {
		return askPrice;
	}

	public void setAskPrice(String askPrice) {
		this.askPrice = askPrice;
	}

	public String getAskQuantity() {
		return askQuantity;
	}

	public void setAskQuantity(String askQuantity) {
		this.askQuantity = askQuantity;
	}

	@Override public String toString() {
		return new ToStringBuilder(this, BinanceApiConstants.TO_STRING_BUILDER_STYLE).append("eventType", "BookTicker")
				.append("updateId", updateId).append("symbol", symbol).append("bidPrice", bidPrice)
				.append("bidQuantity", bidQuantity).append("askPrice", askPrice).append("askQuantity", askQuantity)
				.toString();
	}
}