package com.binance.api.client.domain.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) public class CrossMarginAssets {

	public String assetFullName;
	public String assetName;
	public boolean isBorrowable;
	public boolean isMortgageable;
	public String userMinBorrow;
	public String userMinRepay;

	public String getAssetFullName() {
		return assetFullName;
	}

	public void setAssetFullName(String assetFullName) {
		this.assetFullName = assetFullName;
	}

	public String getAssetName() {
		return assetName;
	}

	public void setAssetName(String assetName) {
		this.assetName = assetName;
	}

	public boolean isBorrowable() {
		return isBorrowable;
	}

	public void setBorrowable(boolean borrowable) {
		isBorrowable = borrowable;
	}

	public boolean isMortgageable() {
		return isMortgageable;
	}

	public void setMortgageable(boolean mortgageable) {
		isMortgageable = mortgageable;
	}

	public String getUserMinBorrow() {
		return userMinBorrow;
	}

	public void setUserMinBorrow(String userMinBorrow) {
		this.userMinBorrow = userMinBorrow;
	}

	public String getUserMinRepay() {
		return userMinRepay;
	}

	public void setUserMinRepay(String userMinRepay) {
		this.userMinRepay = userMinRepay;
	}

	@Override public String toString() {
		return "CrossMarginAssets{" + "assetFullName='" + assetFullName + '\'' + ", assetName='" + assetName + '\''
				+ ", isBorrowable=" + isBorrowable + ", isMortgageable=" + isMortgageable + ", userMinBorrow='"
				+ userMinBorrow + '\'' + ", userMinRepay='" + userMinRepay + '\'' + '}';
	}
}
