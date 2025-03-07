package com.lambda.investing.trading_engine_connector.paper.market;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderTreeTest {

    private OrderTree orderTree;

    @BeforeEach
    void setUp() {
        orderTree = new OrderTree();
    }

    @Test
    void testReset() {
        orderTree.insertOrder(new OrderOrderbook(1, true, 100, 1, "bid", 10.0, "", ""));
        orderTree.reset();
        assertEquals(0, orderTree.getVolume());
        assertEquals(0, orderTree.getNOrders());
        assertEquals(0, orderTree.getDepth());
    }

    @Test
    void testInsertAndRemoveOrder() {
        OrderOrderbook order = new OrderOrderbook(1, true, 100, 1, "bid", 10.0, "", "");
        orderTree.insertOrder(order);
        assertEquals(1, orderTree.getNOrders());
        assertTrue(orderTree.orderExists(order.getNextOrderOrderId()));

        orderTree.removeOrderByID(order.getNextOrderOrderId());
        assertFalse(orderTree.orderExists(order.getNextOrderOrderId()));
    }

    @Test
    void testPriceListOperations() {
        orderTree.createPrice(10.0);
        assertTrue(orderTree.priceExists(10.0));

        orderTree.removePrice(10.0);
        assertFalse(orderTree.priceExists(10.0));
    }

    @Test
    void testOrderUpdate() {
        OrderOrderbook order = new OrderOrderbook(1, true, 100, 1, "bid", 10.0, "", "");
        orderTree.insertOrder(order);
        orderTree.updateOrderQty(150, order.getNextOrderOrderId());
        OrderOrderbook updatedOrder = orderTree.getOrder(order.getNextOrderOrderId());
        assertEquals(150, updatedOrder.getQuantity());

        OrderOrderbook newOrder = new OrderOrderbook(2, true, 100, 2, "bid", 11.0, "", "");
        orderTree.updateOrder(newOrder);
        assertNotNull(orderTree.getOrder(newOrder.getNextOrderOrderId()));
    }

    @Test
    void testMinMaxPrices() {
        OrderOrderbook order1 = new OrderOrderbook(1, true, 100, 1, "bid", 10.0, "algo", "0");
        OrderOrderbook order2 = new OrderOrderbook(2, true, 100, 2, "bid", 9.0, "algo", "2");
        order1.setNextOrder(order2);
        order1.setNextOrderOrderId(order2.getOrderId());

        orderTree.insertOrder(order1);
        assertEquals(10.0, orderTree.minPrice());
        assertEquals(10.0, orderTree.maxPrice());

        orderTree.insertOrder(order2);

        assertEquals(9.0, orderTree.minPrice());
        assertEquals(10.0, orderTree.maxPrice());
    }

    @Test
    void testVolumeAndDepth() {
        OrderOrderbook order1 = new OrderOrderbook(1, true, 100, 1, "bid", 10.0, "algo", "0");
        OrderOrderbook order2 = new OrderOrderbook(2, true, 100, 2, "bid", 9.0, "algo", "2");
        order1.setNextOrder(order2);
        order1.setNextOrderOrderId(order2.getOrderId());

        orderTree.insertOrder(order1);
        assertEquals(10.0, orderTree.minPrice());
        assertEquals(10.0, orderTree.maxPrice());

        orderTree.insertOrder(order2);


        assertEquals(200, orderTree.getVolume());
        assertEquals(2, orderTree.getDepth());
    }

    @Test
    void testToString() {
        orderTree.insertOrder(new OrderOrderbook(1, true, 100, 1, "bid", 10.0, "", ""));
        String treeString = orderTree.toString();
        assertTrue(treeString.contains("Max price = 10.0"));
        assertTrue(treeString.contains("Volume in book = 100"));
    }
}