package com.lambda.investing.trading_engine_connector.paper.market;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class OrderTree {

	Logger logger = LogManager.getLogger(OrderTree.class);
	NavigableMap<Double, OrderList> priceTree = new TreeMap<Double, OrderList>();
	//	TreeMap<Double, OrderList> priceTree = new TreeMap<Double, OrderList>();
	Map<Double, OrderList> priceMap = new ConcurrentHashMap<Double, OrderList>();
	;
	Map<Integer, OrderOrderbook> orderMap = new ConcurrentHashMap<Integer, OrderOrderbook>();
	Map<String, OrderOrderbook> clientOrderIdOrderMap = new ConcurrentHashMap<String, OrderOrderbook>();
	Map<String, Integer> clientOrderIdToOrderId = new ConcurrentHashMap<String, Integer>();
	Map<Integer, String> orderIdToClientOrderId = new ConcurrentHashMap<Integer, String>();
	int volume;
	int nOrders;
	int depth;

	public OrderTree() {
		reset();
	}

	public synchronized void reset() {
		priceTree.clear();
		priceMap.clear();
		orderMap.clear();
		clientOrderIdOrderMap.clear();
		orderIdToClientOrderId.clear();
		volume = 0;
		nOrders = 0;
		depth = 0;
	}

	public synchronized List<Double> getPriceTreeList(boolean descending) {
		//		TreeMap<Double, OrderList> priceTreeCopy = new TreeMap<Double, OrderList>(priceTree);
		if (descending) {
			return new ArrayList<>(priceTree.descendingKeySet());
		} else {
			return new ArrayList<>(priceTree.keySet());
		}
	}

	public Integer length() {
		return orderMap.size();
	}

	public OrderList getPriceList(double price) {
		/*
		 * Returns the OrderList object associated with 'price'
		 */
		return priceMap.get(price);
	}

	public OrderOrderbook getOrder(int id) {
		/*
		 * Returns the order given the order id
		 */
		return orderMap.get(id);
	}

	public OrderOrderbook getOrder(String clientOrderId) {
		/*
		 * Returns the order given the order id
		 */
		return clientOrderIdOrderMap.get(clientOrderId);
	}

	public synchronized void createPrice(double price) {
		depth += 1;
		OrderList newList = new OrderList();
		priceTree.put(price, newList);
		priceMap.put(price, newList);
	}

	public synchronized void removePrice(double price) {
		depth -= 1;
		priceTree.remove(price);
		priceMap.remove(price);
	}

	public boolean priceExists(double price) {
		return priceMap.containsKey(price);
	}

	public boolean orderExists(int id) {
		return orderMap.containsKey(id);
	}

	public boolean orderExists(String clientOrderId) {
		return clientOrderIdOrderMap.containsKey(clientOrderId);
	}

	public void insertOrder(OrderOrderbook quote) {
		int quoteID = quote.getNextOrderOrderId();
		double quotePrice = quote.getPrice();
		if (orderExists(quoteID)) {
			removeOrderByID(quoteID);
		}
		nOrders += 1;
		if (!priceExists(quotePrice)) {
			createPrice(quotePrice);
		}
		quote.setoL(priceMap.get(quotePrice));
		priceMap.get(quotePrice).appendOrder(quote);
		orderMap.put(quoteID, quote);
		clientOrderIdOrderMap.put(quote.getClientOrderId(), quote);
		clientOrderIdToOrderId.put(quote.getClientOrderId(), quote.getNextOrderOrderId());
		orderIdToClientOrderId.put(quote.getNextOrderOrderId(), quote.getClientOrderId());
		volume += quote.getQuantity();
	}

	public void updateOrderQty(double qty, int qId) {
		OrderOrderbook order = this.orderMap.get(qId);
		double originalVol = order.getQuantity();
		order.updateQty(qty, order.getTimestamp());
		this.volume += (order.getQuantity() - originalVol);
		this.clientOrderIdOrderMap.put(order.getClientOrderId(), order);//update it
	}

	public void updateOrder(OrderOrderbook orderUpdate) {
		int idNum = orderUpdate.getNextOrderOrderId();
		double price = orderUpdate.getPrice();
		OrderOrderbook order = this.orderMap.get(idNum);
		double originalVol = order.getQuantity();
		if (price != order.getPrice()) {
			// Price has been updated
			OrderList tempOL = this.priceMap.get(order.getPrice());
			tempOL.removeOrder(order);
			if (tempOL.getLength() == 0) {
				removePrice(order.getPrice());
			}
			insertOrder(orderUpdate);
		} else {
			// The quantity has changed
			order.updateQty(orderUpdate.getQuantity(), orderUpdate.getTimestamp());
		}
		this.clientOrderIdOrderMap.put(order.getClientOrderId(), order);//update it
		this.volume += (order.getQuantity() - originalVol);
	}

	public boolean removeOrderByID(int id) {
		OrderOrderbook order = orderMap.get(id);
		if (order == null) {
			return false;
		}
		this.nOrders -= 1;
		this.volume -= order.getQuantity();
		order.getoL().removeOrder(order);
		if (order.getoL().getLength() == 0) {
			this.removePrice(order.getPrice());
		}
		this.orderMap.remove(id);
		String clientOrderId = this.orderIdToClientOrderId.get(id);
		this.clientOrderIdOrderMap.remove(clientOrderId);
		return true;
	}

	public void removeOrderByClientOrderId(String origClientOrderId) {
		OrderOrderbook order = clientOrderIdOrderMap.get(origClientOrderId);
		Integer id = clientOrderIdToOrderId.get(origClientOrderId);
		if (!removeOrderByID(order.getNextOrderOrderId())) {
			if (!removeOrderByID(id)) {
				if (!removeOrderByID(order.getOrderId())) {
					//					logger.warn("cant cancel {} in {} side",origClientOrderId,order.getSide());
				}
			}
		}
	}


	public Double maxPrice() {
		if (this.depth > 0) {
			return this.priceTree.lastKey();
		} else {
			return null;
		}
	}

	public Double minPrice() {
		if (this.depth > 0) {
			return this.priceTree.firstKey();
		} else {
			return null;
		}
	}

	public OrderList maxPriceList() {
		if (this.depth > 0) {
			return this.getPriceList(maxPrice());
		} else {
			return null;
		}
	}

	public OrderList minPriceList() {
		if (this.depth > 0) {
			return this.getPriceList(minPrice());
		} else {
			return null;
		}
	}

	public String toString() {
		String outString = "| The Book:\n" + "| Max price = " + maxPrice() + "\n| Min price = " + minPrice()
				+ "\n| Volume in book = " + getVolume() + "\n| Depth of book = " + getDepth() + "\n| Orders in book = "
				+ getnOrders() + "\n| Length of tree = " + length() + "\n";
		for (Map.Entry<Double, OrderList> entry : this.priceTree.entrySet()) {
			outString += entry.getValue().toString();
			outString += ("|\n");
		}
		return outString;
	}

	public Integer getVolume() {
		return volume;
	}

	public Integer getnOrders() {
		return nOrders;
	}

	public Integer getDepth() {
		return depth;
	}

}

