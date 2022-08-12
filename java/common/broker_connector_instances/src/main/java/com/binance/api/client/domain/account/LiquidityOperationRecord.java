package com.binance.api.client.domain.account;

import com.binance.api.client.domain.LiquidityOperationRecordStatus;

public class LiquidityOperationRecord {

	private String poolId;
	private String operationId;
	private String updateTime;
	private String operation;
	private String shareAmount;
	private String poolName;
	private LiquidityOperationRecordStatus status;

	public String getPoolId() {
		return poolId;
	}

	public void setPoolId(String poolId) {
		this.poolId = poolId;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getShareAmount() {
		return shareAmount;
	}

	public void setShareAmount(String shareAmount) {
		this.shareAmount = shareAmount;
	}

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	public LiquidityOperationRecordStatus getStatus() {
		return status;
	}

	public void setStatus(LiquidityOperationRecordStatus status) {
		this.status = status;
	}

	public String getOperationId() {
		return operationId;
	}

	public void setOperationId(String operationId) {
		this.operationId = operationId;
	}

	@Override public String toString() {
		return "LiquidityOperationRecord{" + "poolId='" + poolId + '\'' + ", operationId='" + operationId + '\''
				+ ", updateTime='" + updateTime + '\'' + ", operation='" + operation + '\'' + ", shareAmount='"
				+ shareAmount + '\'' + ", poolName='" + poolName + '\'' + ", status='" + status + '\'' + '}';
	}
}
