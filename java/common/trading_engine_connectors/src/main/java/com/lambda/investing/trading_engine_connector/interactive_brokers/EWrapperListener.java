package com.lambda.investing.trading_engine_connector.interactive_brokers;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.lambda.investing.interactive_brokers.listener.IBListener;
import lombok.Getter;

@Getter
public class EWrapperListener extends IBListener {
    private InteractiveBrokersTradingEngine interactiveBrokersTradingEngine;


    public EWrapperListener(InteractiveBrokersTradingEngine interactiveBrokersTradingEngine) {
        super();
        this.interactiveBrokersTradingEngine = interactiveBrokersTradingEngine;
    }


    @Override
    public void completedOrder(Contract contract, Order order, OrderState orderState) {
        interactiveBrokersTradingEngine.completedOrder(contract, order, orderState);
    }

    @Override
    public void completedOrdersEnd() {
        interactiveBrokersTradingEngine.completedOrdersEnd();
    }

    @Override
    public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
        interactiveBrokersTradingEngine.orderStatus(orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld, mktCapPrice);
    }

    @Override
    public void replaceFAEnd(int reqId, String text) {

    }

    @Override
    public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
        interactiveBrokersTradingEngine.error(id, errorCode, errorMsg, advancedOrderRejectJson);
    }


}
