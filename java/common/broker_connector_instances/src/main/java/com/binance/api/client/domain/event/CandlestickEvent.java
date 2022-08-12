package com.binance.api.client.domain.event;

import com.binance.api.client.constant.BinanceApiConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * An interval candlestick for a symbol providing informations on price that can be used to produce candlestick charts.
 */
@JsonDeserialize(using = CandlestickEventDeserializer.class) @JsonSerialize(using = CandlestickEventSerializer.class) @JsonIgnoreProperties(ignoreUnknown = true) public class CandlestickEvent {

	private String eventType;

	private long eventTime;

	private String symbol;

	private Long openTime;

	private String open;

	private String high;

	private String low;

	private String close;

	private String volume;

	private Long closeTime;

	private String intervalId;

	private Long firstTradeId;

	private Long lastTradeId;

	private String quoteAssetVolume;

	private Long numberOfTrades;

	private String takerBuyBaseAssetVolume;

	private String takerBuyQuoteAssetVolume;

	private Boolean isBarFinal;

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

	public Long getOpenTime() {
		return openTime;
	}

	public void setOpenTime(Long openTime) {
		this.openTime = openTime;
	}

	public String getOpen() {
		return open;
	}

	public void setOpen(String open) {
		this.open = open;
	}

	public String getHigh() {
		return high;
	}

	public void setHigh(String high) {
		this.high = high;
	}

	public String getLow() {
		return low;
	}

	public void setLow(String low) {
		this.low = low;
	}

	public String getClose() {
		return close;
	}

	public void setClose(String close) {
		this.close = close;
	}

	public String getVolume() {
		return volume;
	}

	public void setVolume(String volume) {
		this.volume = volume;
	}

	public Long getCloseTime() {
		return closeTime;
	}

	public void setCloseTime(Long closeTime) {
		this.closeTime = closeTime;
	}

	public String getIntervalId() {
		return intervalId;
	}

	public void setIntervalId(String intervalId) {
		this.intervalId = intervalId;
	}

	public Long getFirstTradeId() {
		return firstTradeId;
	}

	public void setFirstTradeId(Long firstTradeId) {
		this.firstTradeId = firstTradeId;
	}

	public Long getLastTradeId() {
		return lastTradeId;
	}

	public void setLastTradeId(Long lastTradeId) {
		this.lastTradeId = lastTradeId;
	}

	public String getQuoteAssetVolume() {
		return quoteAssetVolume;
	}

	public void setQuoteAssetVolume(String quoteAssetVolume) {
		this.quoteAssetVolume = quoteAssetVolume;
	}

	public Long getNumberOfTrades() {
		return numberOfTrades;
	}

	public void setNumberOfTrades(Long numberOfTrades) {
		this.numberOfTrades = numberOfTrades;
	}

	public String getTakerBuyBaseAssetVolume() {
		return takerBuyBaseAssetVolume;
	}

	public void setTakerBuyBaseAssetVolume(String takerBuyBaseAssetVolume) {
		this.takerBuyBaseAssetVolume = takerBuyBaseAssetVolume;
	}

	public String getTakerBuyQuoteAssetVolume() {
		return takerBuyQuoteAssetVolume;
	}

	public void setTakerBuyQuoteAssetVolume(String takerBuyQuoteAssetVolume) {
		this.takerBuyQuoteAssetVolume = takerBuyQuoteAssetVolume;
	}

	public Boolean getBarFinal() {
		return isBarFinal;
	}

	public void setBarFinal(Boolean barFinal) {
		isBarFinal = barFinal;
	}

	@Override public String toString() {
		return new ToStringBuilder(this, BinanceApiConstants.TO_STRING_BUILDER_STYLE).append("eventType", eventType)
				.append("eventTime", eventTime).append("symbol", symbol).append("openTime", openTime)
				.append("open", open).append("high", high).append("low", low).append("close", close)
				.append("volume", volume).append("closeTime", closeTime).append("intervalId", intervalId)
				.append("firstTradeId", firstTradeId).append("lastTradeId", lastTradeId)
				.append("quoteAssetVolume", quoteAssetVolume).append("numberOfTrades", numberOfTrades)
				.append("takerBuyBaseAssetVolume", takerBuyBaseAssetVolume)
				.append("takerBuyQuoteAssetVolume", takerBuyQuoteAssetVolume).append("isBarFinal", isBarFinal)
				.toString();
	}
}