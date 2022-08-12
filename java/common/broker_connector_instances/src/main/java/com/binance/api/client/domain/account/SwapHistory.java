package com.binance.api.client.domain.account;

public class SwapHistory {

	private String quoteQty;
	private Long swapTime;
	private String swapId;
	private String price;
	private String fee;
	private String baseQty;
	private String baseAsset;
	private String quoteAsset;
	private SwapStatus status;

	public String getQuoteQty() {
		return quoteQty;
	}

	public void setQuoteQty(String quoteQty) {
		this.quoteQty = quoteQty;
	}

	public Long getSwapTime() {
		return swapTime;
	}

	public void setSwapTime(Long swapTime) {
		this.swapTime = swapTime;
	}

	public String getSwapId() {
		return swapId;
	}

	public void setSwapId(String swapId) {
		this.swapId = swapId;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getFee() {
		return fee;
	}

	public void setFee(String fee) {
		this.fee = fee;
	}

	public String getBaseQty() {
		return baseQty;
	}

	public void setBaseQty(String baseQty) {
		this.baseQty = baseQty;
	}

	public String getBaseAsset() {
		return baseAsset;
	}

	public void setBaseAsset(String baseAsset) {
		this.baseAsset = baseAsset;
	}

	public String getQuoteAsset() {
		return quoteAsset;
	}

	public void setQuoteAsset(String quoteAsset) {
		this.quoteAsset = quoteAsset;
	}

	public SwapStatus getStatus() {
		return status;
	}

	public void setStatus(SwapStatus status) {
		this.status = status;
	}

	@Override public String toString() {
		return "SwapHistory{" + "quoteQty='" + quoteQty + '\'' + ", swapTime=" + swapTime + ", swapId='" + swapId + '\''
				+ ", price='" + price + '\'' + ", fee='" + fee + '\'' + ", baseQty='" + baseQty + '\'' + ", baseAsset='"
				+ baseAsset + '\'' + ", quoteAsset='" + quoteAsset + '\'' + ", status='" + status + '\'' + '}';
	}
}
