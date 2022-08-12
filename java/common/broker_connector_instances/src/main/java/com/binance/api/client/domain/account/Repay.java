package com.binance.api.client.domain.account;

import com.binance.api.client.domain.LoanStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) public class Repay {

	private String amount;
	private String asset;
	private String interest;
	private String principal;
	LoanStatus status;
	private long timestamp;
	private String txId;

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getAsset() {
		return asset;
	}

	public void setAsset(String asset) {
		this.asset = asset;
	}

	public String getInterest() {
		return interest;
	}

	public void setInterest(String interest) {
		this.interest = interest;
	}

	public String getPrincipal() {
		return principal;
	}

	public void setPrincipal(String principal) {
		this.principal = principal;
	}

	public LoanStatus getStatus() {
		return status;
	}

	public void setStatus(LoanStatus status) {
		this.status = status;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getTxId() {
		return txId;
	}

	public void setTxId(String txId) {
		this.txId = txId;
	}

	@Override public String toString() {
		return "Repay{" + "amount='" + amount + '\'' + ", asset='" + asset + '\'' + ", interest='" + interest + '\''
				+ ", principal='" + principal + '\'' + ", status=" + status + ", timestamp=" + timestamp + ", txId='"
				+ txId + '\'' + '}';
	}
}
