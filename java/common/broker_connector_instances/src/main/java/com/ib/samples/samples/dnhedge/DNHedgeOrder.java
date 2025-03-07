/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.samples.dnhedge;

import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.VolatilityType;

public class DNHedgeOrder extends Order {
    public DNHedgeOrder(int clientId, int id, int size, String account,
                        String settlingFirm, int underConId, String designatedLocation) {
        clientId(clientId);
        orderId(id);
        permId(id);

        account(account);
        clearingIntent("AWAY");
        settlingFirm(settlingFirm);

        orderType(OrderType.VOL);
        action(Action.BUY);
        totalQuantity(Decimal.get(size));

        volatility(0.1);
        volatilityType(VolatilityType.Daily);
        continuousUpdate(1);
        deltaNeutralOrderType(OrderType.LMT);

        deltaNeutralConId(underConId);
        deltaNeutralOpenClose("O");
        deltaNeutralShortSale(true);

        deltaNeutralDesignatedLocation(designatedLocation);
        deltaNeutralShortSaleSlot(deltaNeutralDesignatedLocation().length() == 0 ? 1 : 2);
    }
}
