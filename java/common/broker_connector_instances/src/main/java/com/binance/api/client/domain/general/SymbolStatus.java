package com.binance.api.client.domain.general;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Status of a symbol on the exchange.
 */
@JsonIgnoreProperties(ignoreUnknown = true) public enum SymbolStatus {
	PRE_TRADING, TRADING, POST_TRADING, END_OF_DAY, HALT, AUCTION_MATCH, BREAK;
}
