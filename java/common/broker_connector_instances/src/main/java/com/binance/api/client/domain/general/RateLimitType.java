package com.binance.api.client.domain.general;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Rate limiters.
 */
@JsonIgnoreProperties(ignoreUnknown = true) public enum RateLimitType {
	REQUEST_WEIGHT, ORDERS
}
