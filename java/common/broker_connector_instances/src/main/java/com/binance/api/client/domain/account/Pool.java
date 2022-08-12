package com.binance.api.client.domain.account;

import java.util.List;

public class Pool {

	private String poolId;
	private String poolName;
	private List<String> assets;

	public String getPoolId() {
		return poolId;
	}

	public void setPoolId(String poolId) {
		this.poolId = poolId;
	}

	public List<String> getAssets() {
		return assets;
	}

	public void setAssets(List<String> assets) {
		this.assets = assets;
	}

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	@Override public String toString() {
		return "Pool{" + "poolId='" + poolId + '\'' + ", poolName='" + poolName + '\'' + ", assets=" + assets + '}';
	}
}
