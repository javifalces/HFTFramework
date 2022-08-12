package com.binance.api.client.domain.account;

import com.binance.api.client.constant.BinanceApiConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * An asset balance in an Account.
 *
 * @see Account
 */
@JsonIgnoreProperties(ignoreUnknown = true) public class MarginAssetBalance {

	public static MarginAssetBalance of(final String asset) {
		final MarginAssetBalance marginAssetBalance = new MarginAssetBalance();

		marginAssetBalance.setAsset(asset);

		return marginAssetBalance;
	}

	/**
	 * Asset symbol.
	 */
	private String asset;

	private String borrowed = "0";

	/**
	 * Available balance.
	 */
	private String free = "0";

	private String interest = "0";

	/**
	 * Locked by open orders.
	 */
	private String locked = "0";

	private String netAsset = "0";

	public String getAsset() {
		return asset;
	}

	public void setAsset(String asset) {
		this.asset = asset;
	}

	public String getBorrowed() {
		return borrowed;
	}

	public void setBorrowed(String borrowed) {
		this.borrowed = borrowed;
	}

	public String getFree() {
		return free;
	}

	public void setFree(String free) {
		this.free = free;
	}

	public String getInterest() {
		return interest;
	}

	public void setInterest(String interest) {
		this.interest = interest;
	}

	public String getLocked() {
		return locked;
	}

	public void setLocked(String locked) {
		this.locked = locked;
	}

	public String getNetAsset() {
		return netAsset;
	}

	public void setNetAsset(String netAsset) {
		this.netAsset = netAsset;
	}

	@Override public String toString() {
		return new ToStringBuilder(this, BinanceApiConstants.TO_STRING_BUILDER_STYLE).append("asset", asset)
				.append("borrowed", borrowed).append("free", free).append("interest", interest).append("locked", locked)
				.append("netAsset", netAsset).toString();
	}
}
