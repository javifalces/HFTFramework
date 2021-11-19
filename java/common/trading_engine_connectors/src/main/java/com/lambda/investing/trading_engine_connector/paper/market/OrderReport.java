package com.lambda.investing.trading_engine_connector.paper.market;

import java.util.ArrayList;

public class OrderReport {

	/*
	 * Return after an order is submitted to the lob. Contains:
	 * 	- trades:
	 *
	 * 	- orderInBook
	 */
	private ArrayList<Trade> trades = new ArrayList<Trade>();
	private boolean orderInBook = false;
	private OrderOrderbook order;

	public OrderReport(ArrayList<Trade> trades, boolean orderInBook) {
		this.trades = trades;
		this.orderInBook = orderInBook;
	}

	public OrderOrderbook getOrder() {
		return order;
	}

	public void setOrder(OrderOrderbook order) {
		this.order = order;
	}

	public ArrayList<Trade> getTrades() {
		return trades;
	}

	public boolean isOrderInBook() {
		return orderInBook;
	}

	public String toString() {
		String retString = "--- Order Report ---:\nTrades:\n";
		for (Trade t : trades) {
			retString += ("\n" + t.toString());
		}
		retString += ("order in book? " + orderInBook + "\n");
		if (order != null) {
			retString += ("\nOrders:\n");
			retString += (order.toString());
		}
		return retString + "\n--------------------------";
	}

}
