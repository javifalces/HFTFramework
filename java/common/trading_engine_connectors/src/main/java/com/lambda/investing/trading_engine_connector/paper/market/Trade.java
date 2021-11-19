package com.lambda.investing.trading_engine_connector.paper.market;

public class Trade {

	private long timestamp;
	private double price;
	private double qty;
	private int provider;
	private int taker;
	private int buyer;
	private int seller;
	private int orderHit;
	private String buyerAlgorithmInfo;
	private String sellerAlgorithmInfo;
	private String buyerClientOrderId;
	private String sellerClientOrderId;

	public Trade(long time, double price, double qty, int provider, int taker, int buyer, int seller, int orderHit,
			String buyerAlgorithmInfo, String sellerAlgorithmInfo, String buyerClientOrderId,
			String sellerClientOrderId) {
		this.timestamp = time;
		this.price = price;
		this.qty = qty;
		this.provider = provider;
		this.taker = taker;
		this.buyer = buyer;
		this.seller = seller;
		this.orderHit = orderHit; // the qId of the order that was in the book
		this.buyerAlgorithmInfo = buyerAlgorithmInfo;
		this.sellerAlgorithmInfo = sellerAlgorithmInfo;
		this.buyerClientOrderId = buyerClientOrderId;
		this.sellerClientOrderId = sellerClientOrderId;
	}

	@Override public String toString() {
		return ("\n| TRADE\tt= " + timestamp + "\tprice = " + price + "\tquantity = " + qty + "\tProvider = " + provider
				+ "\tTaker = " + taker + "\tBuyer = " + buyer + "\tSeller = " + seller + "\tBuyerAlgo = "
				+ buyerAlgorithmInfo + "\tSellerAlgo = " + sellerAlgorithmInfo + "\tBuyerClOrdId = "
				+ buyerClientOrderId + "\tSellerClOrdId = " + sellerClientOrderId);
	}

	public String toCSV() {
		return (timestamp + ", " + price + ", " + qty + ", " + provider + ", " + taker + ", " + buyer + ", " + seller
				+ "\n");
	}

	public String getBuyerClientOrderId() {
		return buyerClientOrderId;
	}

	public String getSellerClientOrderId() {
		return sellerClientOrderId;
	}

	public String getBuyerAlgorithmInfo() {
		return buyerAlgorithmInfo;
	}

	public String getSellerAlgorithmInfo() {
		return sellerAlgorithmInfo;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public double getPrice() {
		return price;
	}

	public double getQty() {
		return qty;
	}

	public int getProvider() {
		return provider;
	}

	public int getTaker() {
		return taker;
	}

	public int getBuyer() {
		return buyer;
	}

	public int getSeller() {
		return seller;
	}

	public int getOrderHit() {
		return orderHit;
	}
}
