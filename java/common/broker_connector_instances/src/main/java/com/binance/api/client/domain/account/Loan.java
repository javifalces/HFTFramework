package com.binance.api.client.domain.account;

import com.binance.api.client.domain.LoanStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents an executed trade history item.
 */
@JsonIgnoreProperties(ignoreUnknown = true) public class Loan {

	private String asset;
	private String principal;
	private long timestamp;
	private LoanStatus status;

	public String getAsset() {
		return asset;
	}

	public void setAsset(String asset) {
		this.asset = asset;
	}

	public String getPrincipal() {
		return principal;
	}

	public void setPrincipal(String principal) {
		this.principal = principal;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public LoanStatus getStatus() {
		return status;
	}

	public void setStatus(LoanStatus status) {
		this.status = status;
	}
}
