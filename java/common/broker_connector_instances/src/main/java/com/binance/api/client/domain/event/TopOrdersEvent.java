package com.binance.api.client.domain.event;

import com.binance.api.client.constant.BinanceApiConstants;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

/**
 * Top bids and asks from Partial Book Depth event
 */
@JsonIgnoreProperties(ignoreUnknown = true) public class TopOrdersEvent {

	private Long lastUpdateId;
	private List<OrderBookEntry> bids;
	private List<OrderBookEntry> asks;

	public Long getLastUpdateId() {
		return lastUpdateId;
	}

	public void setLastUpdateId(Long lastUpdateId) {
		this.lastUpdateId = lastUpdateId;
	}

	public List<OrderBookEntry> getBids() {
		return bids;
	}

	public void setBids(List<OrderBookEntry> bids) {
		this.bids = bids;
	}

	public List<OrderBookEntry> getAsks() {
		return asks;
	}

	public void setAsks(List<OrderBookEntry> asks) {
		this.asks = asks;
	}

	@Override public String toString() {
		return new ToStringBuilder(this, BinanceApiConstants.TO_STRING_BUILDER_STYLE)
				.append("lastUpdateId", lastUpdateId).append("bids", bids).append("asks", asks).toString();
	}
}
