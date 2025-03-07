package com.lambda.investing.trading_engine_connector.paper.market;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OrderList implements Iterable<OrderOrderbook>, Iterator<OrderOrderbook> {

    /*
     * This class create a sorted, iterable list or orders for each price  level
     * in the order tree.
     */
    @Getter
    private OrderOrderbook headOrder = null;
    ;
    @Getter
    @Setter
    private OrderOrderbook tailOrder = null;
    @Getter
    private int length = 0;
    @Setter
    @Getter
    private double volume = 0;    // Total volume at this price level
    private OrderOrderbook last = null;

    private Map<String, String> algorithmsInfo = new ConcurrentHashMap<String, String>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    //


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


    public void appendOrder(OrderOrderbook incomingOrder) {
        lock.writeLock().lock();
        try {
            if (length == 0) {
                headOrder = tailOrder = incomingOrder;
            } else {
                incomingOrder.setPrevOrder(tailOrder);
                tailOrder.setNextOrder(incomingOrder);
                tailOrder = incomingOrder;
            }
            algorithmsInfo.put(incomingOrder.getAlgorithmInfo(), "");
            length++;
            volume += incomingOrder.getQuantity();
        } finally {
            lock.writeLock().unlock();
        }
    }


    public void removeOrder(OrderOrderbook order) {
        lock.writeLock().lock();
        try {
            volume -= order.getQuantity();
            length--;
            if (length == 0) {
                headOrder = tailOrder = null;
                return;
            }
            if (order.getPrevOrder() != null) {
                order.getPrevOrder().setNextOrder(order.getNextOrder());
            } else {
                headOrder = order.getNextOrder();
            }
            if (order.getNextOrder() != null) {
                order.getNextOrder().setPrevOrder(order.getPrevOrder());
            } else {
                tailOrder = order.getPrevOrder();
            }
            algorithmsInfo.remove(order.getAlgorithmInfo());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void moveTail(OrderOrderbook order) {
        /*
         * Move 'order' to the tail of the list (after modification for e.g.)
         */
        lock.writeLock().lock();
        try {
            if (order == tailOrder) return; // Already at tail
            if (order.getPrevOrder() != null) {
                order.getPrevOrder().setNextOrder(order.getNextOrder());
            } else {
                headOrder = order.getNextOrder();
            }
            if (order.getNextOrder() != null) {
                order.getNextOrder().setPrevOrder(order.getPrevOrder());
            }
            tailOrder.setNextOrder(order);
            order.setPrevOrder(tailOrder);
            tailOrder = order;
            order.setNextOrder(null);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String toString() {
        String outString = "";
        for (OrderOrderbook o : this) {
            outString += ("| " + o.toString() + "\n");
        }
        return outString;
    }


    public String getAlgorithms() {
        return String.join(",", algorithmsInfo.keySet());
    }

    public List<String> getAlgorithmsList() {
        return new ArrayList<>(algorithmsInfo.keySet());
    }


    // This method implements Iterable.
    public Iterator<OrderOrderbook> iterator() {
        return new OrderIterator(headOrder);
    }

    private class OrderIterator implements Iterator<OrderOrderbook> {
        private OrderOrderbook current;

        public OrderIterator(OrderOrderbook start) {
            current = start;
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public OrderOrderbook next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            OrderOrderbook temp = current;
            current = current.getNextOrder();
            return temp;
        }
    }
}