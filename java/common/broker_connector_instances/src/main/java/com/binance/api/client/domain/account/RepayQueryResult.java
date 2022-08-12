package com.binance.api.client.domain.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * History of account withdrawals.
 *
 * @see Withdraw
 */
@JsonIgnoreProperties(ignoreUnknown = true) public class RepayQueryResult {

	private int total;
	private List<Repay> rows;

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public List<Repay> getRows() {
		return rows;
	}

	public void setRows(List<Repay> rows) {
		this.rows = rows;
	}

	@Override public String toString() {
		return "RepayQueryResult{" + "total=" + total + ", rows=" + rows + '}';
	}
}
