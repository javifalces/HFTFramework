/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.samples.rfq;

import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderType;

public class RfqOrder extends Order {

    public RfqOrder(int clientId, int id, int size) {

        clientId(clientId);
        orderId(id);
        permId(id);
        totalQuantity(Decimal.get(size));
        orderType(OrderType.QUOTE);

        /*
         * Note: this will be overridden by the backend
         *       because it could not keep such order
         *       (and it does not make too much sense)
         */
        transmit(false);
    }
}

