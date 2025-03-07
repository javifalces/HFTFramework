package com.lambda.investing.trading_engine_connector.paper.market;

import com.google.common.util.concurrent.AtomicDouble;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class OrderOrderbook {

    private volatile long timestamp;
    private final boolean limit;
    private final AtomicDouble quantity = new AtomicDouble();
    private final String side;
    private final AtomicReference<Double> price = new AtomicReference<>();
    private final AtomicInteger nextOrderOrderId = new AtomicInteger();
    private final AtomicInteger orderId = new AtomicInteger();
    private final AtomicReference<OrderOrderbook> nextOrder = new AtomicReference<>();
    private final AtomicReference<OrderOrderbook> prevOrder = new AtomicReference<>();
    private volatile OrderList oL; // Assuming OrderList is thread-safe or not shared across threads
    private final String algorithmInfo;
    private final String clientOrderId;

    public OrderOrderbook(long time, boolean limit, double quantity, int orderId, String side, Double price,
                          String algorithmInfo, String clientOrderId) {
        this.timestamp = time;
        this.limit = limit;
        this.side = side;
        this.quantity.set(quantity);
        this.price.set(price);
        this.orderId.set(orderId);
        this.algorithmInfo = algorithmInfo;
        this.clientOrderId = clientOrderId;
    }

    public void updateQty(double qty, long tstamp) {
        if ((qty > this.quantity.get()) && (this.oL.getTailOrder() != this)) {
            // Move order to the end of the list. i.e., loses time priority
            this.oL.moveTail(this);
            this.timestamp = tstamp;
        }
        oL.setVolume(oL.getVolume() - (this.quantity.get() - qty));
        this.quantity.set(qty);
    }

    @Override
    public String toString() {
        return "[" + this.clientOrderId + "] - " + algorithmInfo + " \t" + Double.toString(quantity.get()) + "@" + Double
                .toString(price.get()) + "\ttime=" + timestamp
                + "\tnextOrderOrderId=" + nextOrderOrderId.get() + "\torderId=" + orderId.get();
    }

    // Getters and Setters with thread-safe types
    public String getClientOrderId() {
        return clientOrderId;
    }

    public String getAlgorithmInfo() {
        return algorithmInfo;
    }

    public OrderOrderbook getNextOrder() {
        return nextOrder.get();
    }

    public void setNextOrder(OrderOrderbook nextOrder) {
        this.nextOrder.set(nextOrder);
    }

    public OrderOrderbook getPrevOrder() {
        return prevOrder.get();
    }

    public void setPrevOrder(OrderOrderbook prevOrder) {
        this.prevOrder.set(prevOrder);
    }
    // Note: Only showing a couple as an example, apply similar changes to others as needed

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getQuantity() {
        return quantity.get();
    }

    public void setQuantity(double quantity) {
        this.quantity.set(quantity);
    }

    public double getPrice() {
        return price.get();
    }

    public void setPrice(double price) {
        this.price.set(price);
    }

    public int getNextOrderOrderId() {
        return nextOrderOrderId.get();
    }

    public void setNextOrderOrderId(int nextOrderOrderId) {
        this.nextOrderOrderId.set(nextOrderOrderId);
    }


    public int getOrderId() {
        return orderId.get();
    }

    public void setOrderId(int orderId) {
        this.orderId.set(orderId);
    }

    public boolean isLimit() {
        return limit;
    }

    public String getSide() {
        return side;
    }

    public OrderList getoL() {
        return this.oL;
    }

    public void setoL(OrderList oL) {
        this.oL = oL;
    }

}