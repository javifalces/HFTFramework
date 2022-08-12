package com.binance.api.client.domain.event;

import com.binance.api.client.constant.BinanceApiConstants;
import com.binance.api.client.domain.market.AggTrade;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * An aggregated trade event for a symbol.
 */
@JsonIgnoreProperties(ignoreUnknown = true) public class AggTradeEvent extends AggTrade {

	@JsonProperty("e") private String eventType;

	@JsonProperty("E") private long eventTime;

	@JsonProperty("s") private String symbol;

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public long getEventTime() {
		return eventTime;
	}

	public void setEventTime(long eventTime) {
		this.eventTime = eventTime;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	@Override public String toString() {
		return new ToStringBuilder(this, BinanceApiConstants.TO_STRING_BUILDER_STYLE).append("eventType", eventType)
				.append("eventTime", eventTime).append("symbol", symbol).append("aggTrade", super.toString())
				.toString();
	}
}
