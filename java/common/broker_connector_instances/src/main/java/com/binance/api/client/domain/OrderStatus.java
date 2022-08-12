package com.binance.api.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Status of a submitted order.
 */
@JsonIgnoreProperties(ignoreUnknown = true) public enum OrderStatus {
	NEW, PARTIALLY_FILLED, FILLED, CANCELED, PENDING_CANCEL, REJECTED, EXPIRED
}
