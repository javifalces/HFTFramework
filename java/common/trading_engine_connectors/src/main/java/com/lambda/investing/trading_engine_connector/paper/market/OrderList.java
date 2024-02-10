package com.lambda.investing.trading_engine_connector.paper.market;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderList implements Iterable<OrderOrderbook>, Iterator<OrderOrderbook> {

	/*
	 * This class create a sorted, iterable list or orders for each price  level
	 * in the order tree.
	 */
	private OrderOrderbook headOrder = null;
	;
	private OrderOrderbook tailOrder = null;
	;
	private int length = 0;
	private double volume = 0;    // Total volume at this price level
	private OrderOrderbook last = null;

	private Map<String, String> algorithmsInfo = new ConcurrentHashMap<String, String>();

	// The next three methods implement Iterator.
	public boolean hasNext() {
		if (this.last == null) {
			return false;
		}
		return true;
	}

	public OrderOrderbook next() {
		if (this.last == null) {
			throw new NoSuchElementException();
		}
		OrderOrderbook returnVal = this.last;
		this.last = this.last.getNextOrder();
		return returnVal;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	// This method implements Iterable.
	public Iterator<OrderOrderbook> iterator() {
		this.last = headOrder;
		return this;
	}

	public void appendOrder(OrderOrderbook incomingOrder) {
		if (length == 0) {
			incomingOrder.setNextOrder(null);
			incomingOrder.setPrevOrder(null);
			headOrder = incomingOrder;
			tailOrder = incomingOrder;

		} else {
			incomingOrder.setPrevOrder(tailOrder);
			incomingOrder.setNextOrder(null);
			tailOrder.setNextOrder(incomingOrder);
			tailOrder = incomingOrder;
		}
		algorithmsInfo.put(incomingOrder.getAlgorithmInfo(), "");
		length += 1;
		volume += incomingOrder.getQuantity();
	}

	public void removeOrder(OrderOrderbook order) {
		this.volume -= order.getQuantity();
		this.length -= 1;
		if (this.length == 0) {
			return;
		}
		OrderOrderbook tempNextOrder = order.getNextOrder();
		OrderOrderbook tempPrevOrder = order.getPrevOrder();
		if ((tempNextOrder != null) && (tempPrevOrder != null)) {
			tempNextOrder.setPrevOrder(tempPrevOrder);
			tempPrevOrder.setNextOrder(tempNextOrder);
		} else if (tempNextOrder != null) {
			tempNextOrder.setPrevOrder(null);
			this.headOrder = tempNextOrder;
		} else if (tempPrevOrder != null) {
			tempPrevOrder.setNextOrder(null);
			this.tailOrder = tempPrevOrder;
		}
		algorithmsInfo.remove(order.getAlgorithmInfo());
	}

	public void moveTail(OrderOrderbook order) {
		/*
		 * Move 'order' to the tail of the list (after modification for e.g.)
		 */
		if (order.getPrevOrder() != null) {
			order.getPrevOrder().setNextOrder(order.getNextOrder());
		} else {
			// Update head order
			this.headOrder = order.getNextOrder();
		}
		order.getNextOrder().setPrevOrder(order.getPrevOrder());
		// Set the previous tail's next order to this order
		this.tailOrder.setNextOrder(order);
		order.setPrevOrder(this.tailOrder);
		this.tailOrder = order;
		order.setNextOrder(null);
	}

	public String toString() {
		String outString = "";
		for (OrderOrderbook o : this) {
			outString += ("| " + o.toString() + "\n");
		}
		return outString;
	}

	public Integer getLength() {
		return length;
	}

	public OrderOrderbook getHeadOrder() {
		return headOrder;
	}

	public OrderOrderbook getTailOrder() {
		return tailOrder;
	}

	public void setTailOrder(OrderOrderbook tailOrder) {
		this.tailOrder = tailOrder;
	}

	public double getVolume() {
		return volume;
	}

	public void setVolume(double volume) {
		this.volume = volume;
	}

	public String getAlgorithms() {
		return String.join(",", algorithmsInfo.keySet());
	}

	public List<String> getAlgorithmsList() {
		return new ArrayList<>(algorithmsInfo.keySet());
	}

}