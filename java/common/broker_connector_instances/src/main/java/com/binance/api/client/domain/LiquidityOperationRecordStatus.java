package com.binance.api.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonIgnoreProperties(ignoreUnknown = true) public enum LiquidityOperationRecordStatus {
	PENDING, SUCCESS, FAILED;

	@JsonValue public int toValue() {
		return ordinal();
	}
}
