package com.binance.api.client.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility class
 */
public final class Util {

	/**
	 * List of fiat currencies.
	 */
	public static final List<String> FIAT_CURRENCY = Collections
			.unmodifiableList(Arrays.asList("USDT", "BUSD", "PAX", "TUSD", "USDC", "NGN", "RUB", "USDS, TRY"));

	public static final String BTC_TICKER = "BTC";

	private Util() {

	}

	/**
	 * Check if the ticker is fiat currency.
	 */
	public static boolean isFiatCurrency(String symbol) {
		for (String fiat : FIAT_CURRENCY) {
			if (symbol.equals(fiat))
				return true;
		}
		return false;
	}
}
