package com.binance.api.client.domain.account;

import com.binance.api.client.constant.BinanceApiConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

/**
 * Account information.
 */
@JsonIgnoreProperties public class MarginAccount {

	private boolean borrowEnabled;

	private String marginLevel;

	private String totalAssetOfBtc;

	private String totalLiabilityOfBtc;

	private String totalNetAssetOfBtc;

	private boolean tradeEnabled;

	private boolean transferEnabled;

	private List<MarginAssetBalance> userAssets;

	public boolean isBorrowEnabled() {
		return borrowEnabled;
	}

	public void setBorrowEnabled(boolean borrowEnabled) {
		this.borrowEnabled = borrowEnabled;
	}

	public String getMarginLevel() {
		return marginLevel;
	}

	public void setMarginLevel(String marginLevel) {
		this.marginLevel = marginLevel;
	}

	public String getTotalAssetOfBtc() {
		return totalAssetOfBtc;
	}

	public void setTotalAssetOfBtc(String totalAssetOfBtc) {
		this.totalAssetOfBtc = totalAssetOfBtc;
	}

	public String getTotalLiabilityOfBtc() {
		return totalLiabilityOfBtc;
	}

	public void setTotalLiabilityOfBtc(String totalLiabilityOfBtc) {
		this.totalLiabilityOfBtc = totalLiabilityOfBtc;
	}

	public String getTotalNetAssetOfBtc() {
		return totalNetAssetOfBtc;
	}

	public void setTotalNetAssetOfBtc(String totalNetAssetOfBtc) {
		this.totalNetAssetOfBtc = totalNetAssetOfBtc;
	}

	public boolean isTradeEnabled() {
		return tradeEnabled;
	}

	public void setTradeEnabled(boolean tradeEnabled) {
		this.tradeEnabled = tradeEnabled;
	}

	public boolean isTransferEnabled() {
		return transferEnabled;
	}

	public void setTransferEnabled(boolean transferEnabled) {
		this.transferEnabled = transferEnabled;
	}

	public List<MarginAssetBalance> getUserAssets() {
		return userAssets;
	}

	public void setUserAssets(List<MarginAssetBalance> userAssets) {
		this.userAssets = userAssets;
	}

	/**
	 * Returns the asset balance for a given symbol.
	 *
	 * @param symbol asset symbol to obtain the balances from
	 * @return an asset balance for the given symbol which can be 0 in case the symbol has no balance in the account
	 */
	public MarginAssetBalance getAssetBalance(final String symbol) {
		return userAssets.stream().filter(marginAssetBalance -> marginAssetBalance.getAsset().equals(symbol))
				.findFirst().orElse(MarginAssetBalance.of(symbol));
	}

	@Override public String toString() {
		return new ToStringBuilder(this, BinanceApiConstants.TO_STRING_BUILDER_STYLE)
				.append("borrowEnabled", borrowEnabled).append("marginLevel", marginLevel)
				.append("totalAssetOfBtc", totalAssetOfBtc).append("totalLiabilityOfBtc", totalLiabilityOfBtc)
				.append("totalNetAssetOfBtc", totalNetAssetOfBtc).append("tradeEnabled", tradeEnabled)
				.append("transferEnabled", transferEnabled).append("userAssets", userAssets).toString();
	}
}
