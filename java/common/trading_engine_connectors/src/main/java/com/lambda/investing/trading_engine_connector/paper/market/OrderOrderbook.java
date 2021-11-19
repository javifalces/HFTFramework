package com.lambda.investing.trading_engine_connector.paper.market;

import java.util.Date;

public class OrderOrderbook {

	private long timestamp;
	private boolean limit;
	private double quantity;
	private String side;
	private double price;
	private int nextOrderOrderId;
	private int orderId;
	private OrderOrderbook nextOrder;
	private OrderOrderbook prevOrder;
	private OrderList oL;
	private String algorithmInfo;
	private String clientOrderId;

	public OrderOrderbook(long time, boolean limit, double quantity, int orderId, String side, Double price,
			String algorithmInfo, String clientOrderId) {

		this.timestamp = time;
		this.limit = limit;
		this.side = side;
		this.quantity = quantity;
		if (price != null) {
			this.price = (double) price;
		}
		this.orderId = orderId;
		this.algorithmInfo = algorithmInfo;
		this.clientOrderId = clientOrderId;
	}

	public void updateQty(double qty, long tstamp) {
		if ((qty > this.quantity) && (this.oL.getTailOrder() != this)) {
			// Move order to the end of the list. i.e. loses time priority
			this.oL.moveTail(this);
			this.timestamp = tstamp;
		}
		oL.setVolume(oL.getVolume() - (this.quantity - qty));
		this.quantity = qty;
	}

	public String toString() {
		return "[" + this.clientOrderId + "] - " + algorithmInfo + " \t" + Double.toString(quantity) + "@" + Double
				.toString(price) + "\ttime=" + new Date(timestamp) + "\tnextOrderOrderId=" + Integer
				.toString(nextOrderOrderId) + "\torderId=" + Integer.toString(orderId);
	}

	public String getClientOrderId() {
		return clientOrderId;
	}

	public String getAlgorithmInfo() {
		return algorithmInfo;
	}

	// Getters and Setters
	public OrderOrderbook getNextOrder() {
		return nextOrder;
	}

	public void setNextOrder(OrderOrderbook nextOrder) {
		this.nextOrder = nextOrder;
	}

	public OrderOrderbook getPrevOrder() {
		return prevOrder;
	}

	public void setPrevOrder(OrderOrderbook prevOrder) {
		this.prevOrder = prevOrder;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public double getQuantity() {
		return quantity;
	}

	public void setQuantity(double quantity) {
		this.quantity = quantity;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public int getNextOrderOrderId() {
		return nextOrderOrderId;
	}

	public void setNextOrderOrderId(int nextOrderOrderId) {
		this.nextOrderOrderId = nextOrderOrderId;
	}

	public int getOrderId() {
		return orderId;
	}

	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}

	public OrderList getoL() {
		return oL;
	}

	public boolean isLimit() {
		return limit;
	}

	public String getSide() {
		return side;
	}

	public void setoL(OrderList oL) {
		this.oL = oL;
	}

}
