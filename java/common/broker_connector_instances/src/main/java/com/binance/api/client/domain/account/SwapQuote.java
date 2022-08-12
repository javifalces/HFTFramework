package com.binance.api.client.domain.account;

public class SwapQuote {

	private String quoteQty;
	private String price;
	private String fee;
	private String baseQty;
	private String baseAsset;
	private String slippage;
	private String quoteAsset;

	public String getQuoteQty() {
		return quoteQty;
	}

	public void setQuoteQty(String quoteQty) {
		this.quoteQty = quoteQty;
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

	public String getSlippage() {
		return slippage;
	}

	public void setSlippage(String slippage) {
		this.slippage = slippage;
	}

	public String getQuoteAsset() {
		return quoteAsset;
	}

	public void setQuoteAsset(String quoteAsset) {
		this.quoteAsset = quoteAsset;
	}

	@Override public String toString() {
		return "SwapQuote{" + "quoteQty='" + quoteQty + '\'' + ", price='" + price + '\'' + ", fee='" + fee + '\''
				+ ", baseQty='" + baseQty + '\'' + ", baseAsset='" + baseAsset + '\'' + ", slippage='" + slippage + '\''
				+ ", quoteAsset='" + quoteAsset + '\'' + '}';
	}
}

