package com.binance.api.client.domain.event;

import com.binance.api.client.constant.BinanceApiConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Account update event which will reflect the balance changes of the account.
 * <p>
 * This event is embedded as part of a user data update event.
 *
 * @see UserDataUpdateEvent
 */
@JsonIgnoreProperties(ignoreUnknown = true) public class BalanceUpdateEvent {

	@JsonProperty("e") private String eventType;

	@JsonProperty("E") private long eventTime;

	@JsonProperty("a") private String asset;

	@JsonProperty("d") private String balanceDelta;

	@JsonProperty("T") private Long clearTime;

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

	public String getAsset() {
		return asset;
	}

	public void setAsset(String asset) {
		this.asset = asset;
	}

	public String getBalanceDelta() {
		return balanceDelta;
	}

	public void setBalanceDelta(String balanceDelta) {
		this.balanceDelta = balanceDelta;
	}

	public Long getClearTime() {
		return clearTime;
	}

	public void setClearTime(Long clearTime) {
		this.clearTime = clearTime;
	}

	@Override public String toString() {
		return new ToStringBuilder(this, BinanceApiConstants.TO_STRING_BUILDER_STYLE).append("eventType", eventType)
				.append("eventTime", eventTime).append("balances", asset).append("balanceDelta", balanceDelta)
				.append("clearTime", clearTime).toString();
	}
}
