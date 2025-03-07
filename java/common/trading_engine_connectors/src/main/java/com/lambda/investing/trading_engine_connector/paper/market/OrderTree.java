package com.lambda.investing.trading_engine_connector.paper.market;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class creates a sorted, iterable list of orders for each price level in the order tree.
 * Represents one of the sides of the orderbook , Bid or Ask with a list of orders at each price level.
 */

public class OrderTree {
    Logger logger = LogManager.getLogger(OrderTree.class);
    private final NavigableMap<Double, OrderList> priceTree = new ConcurrentSkipListMap<>();
    private final Map<Double, OrderList> priceMap = new ConcurrentHashMap<>();
    @Getter
    private final Map<Integer, OrderOrderbook> orderMap = new ConcurrentHashMap<>();
    private final Map<String, OrderOrderbook> clientOrderIdOrderMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> clientOrderIdToOrderId = new ConcurrentHashMap<>();
    private final Map<Integer, String> orderIdToClientOrderId = new ConcurrentHashMap<>();
    @Getter
    private volatile int volume;
    @Getter
    private volatile int nOrders;
    @Getter
    private volatile int depth;
    private final ReadWriteLock treeLock = new ReentrantReadWriteLock();


    public OrderTree() {
        reset();
    }

    public void reset() {
        treeLock.writeLock().lock();
        try {
            priceTree.clear();
            priceMap.clear();
            orderMap.clear();
            clientOrderIdOrderMap.clear();
            clientOrderIdToOrderId.clear();
            orderIdToClientOrderId.clear();
            volume = 0;
            nOrders = 0;
            depth = 0;
        } finally {
            treeLock.writeLock().unlock();
        }
    }


    public List<Double> getPriceTreeList(boolean descending) {
        treeLock.readLock().lock();
        try {
            if (descending) {
                return new ArrayList<>(priceTree.descendingKeySet());
            } else {
                return new ArrayList<>(priceTree.keySet());
            }
        } finally {
            treeLock.readLock().unlock();
        }
    }

    public Integer length() {
        return orderMap.size();
    }

    public OrderList getPriceList(double price) {
        return priceMap.get(price);
    }

    public OrderOrderbook getOrder(int id) {
        return orderMap.get(id);
    }

    public OrderOrderbook getOrder(String clientOrderId) {
        return clientOrderIdOrderMap.get(clientOrderId);
    }

    public void createPrice(double price) {
        treeLock.writeLock().lock();
        try {
            depth++;
            OrderList newList = new OrderList();
            priceTree.put(price, newList);
            priceMap.put(price, newList);
        } finally {
            treeLock.writeLock().unlock();
        }
    }

    public void removePrice(double price) {
        treeLock.writeLock().lock();
        try {
            depth--;
            priceTree.remove(price);
            priceMap.remove(price);
        } finally {
            treeLock.writeLock().unlock();
        }
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
        treeLock.writeLock().lock();
        try {
            int quoteID = quote.getNextOrderOrderId();
            double quotePrice = quote.getPrice();
            if (orderExists(quoteID)) {
                removeOrderByID(quoteID);
            }
            nOrders++;
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
        } finally {
            treeLock.writeLock().unlock();
        }
    }

    public void updateOrderQty(double qty, int qId) {
        treeLock.writeLock().lock();
        try {
            OrderOrderbook order = orderMap.get(qId);
            double originalVol = order.getQuantity();
            order.updateQty(qty, order.getTimestamp());
            volume += (order.getQuantity() - originalVol);
            clientOrderIdOrderMap.put(order.getClientOrderId(), order); // Update it
        } finally {
            treeLock.writeLock().unlock();
        }
    }

    public void updateOrder(OrderOrderbook orderUpdate) {
        treeLock.writeLock().lock();
        try {
            int idNum = orderUpdate.getNextOrderOrderId();
            double price = orderUpdate.getPrice();
            OrderOrderbook order = orderMap.get(idNum);
            double originalVol = order.getQuantity();
            if (price != order.getPrice()) {
                // Price has been updated
                OrderList tempOL = priceMap.get(order.getPrice());
                tempOL.removeOrder(order);
                if (tempOL.getLength() == 0) {
                    removePrice(order.getPrice());
                }
                insertOrder(orderUpdate);
            } else {
                // The quantity has changed
                order.updateQty(orderUpdate.getQuantity(), orderUpdate.getTimestamp());
            }
            clientOrderIdOrderMap.put(order.getClientOrderId(), order); // Update it
            volume += (order.getQuantity() - originalVol);
        } finally {
            treeLock.writeLock().unlock();
        }
    }

    public boolean removeOrderByID(int id) {
        treeLock.writeLock().lock();
        try {
            OrderOrderbook order = orderMap.get(id);
            if (order == null) {
                return false;
            }
            nOrders--;
            volume -= order.getQuantity();
            order.getoL().removeOrder(order);
            if (order.getoL().getLength() == 0) {
                removePrice(order.getPrice());
            }
            orderMap.remove(id);
            String clientOrderId = orderIdToClientOrderId.get(id);
            clientOrderIdOrderMap.remove(clientOrderId);
            clientOrderIdToOrderId.remove(clientOrderId);
            orderIdToClientOrderId.remove(id);
            return true;
        } finally {
            treeLock.writeLock().unlock();
        }
    }

    public void removeOrderByClientOrderId(String origClientOrderId) {
        treeLock.writeLock().lock();
        try {
            Integer id = clientOrderIdToOrderId.get(origClientOrderId);
            if (id != null) {
                removeOrderByID(id);
            }
        } finally {
            treeLock.writeLock().unlock();
        }
    }

    public Double maxPrice() {
        if (depth > 0) {
            return priceTree.lastKey();
        } else {
            return null;
        }
    }

    public Double minPrice() {
        if (depth > 0) {
            return priceTree.firstKey();
        } else {
            return null;
        }
    }

    public OrderList maxPriceList() {
        if (depth > 0) {
            return getPriceList(maxPrice());
        } else {
            return null;
        }
    }

    public OrderList minPriceList() {
        if (depth > 0) {
            return getPriceList(minPrice());
        } else {
            return null;
        }
    }

    public String toString() {
        StringBuilder outString = new StringBuilder("| The Book:\n" + "| Max price = " + maxPrice() + "\n| Min price = " + minPrice()
                + "\n| Volume in book = " + getVolume() + "\n| Depth of book = " + getDepth() + "\n| Orders in book = "
                + getNOrders() + "\n| Length of tree = " + length() + "\n");
        for (Map.Entry<Double, OrderList> entry : priceTree.entrySet()) {
            outString.append(entry.getValue().toString());
            outString.append("|\n");
        }
        return outString.toString();
    }

}