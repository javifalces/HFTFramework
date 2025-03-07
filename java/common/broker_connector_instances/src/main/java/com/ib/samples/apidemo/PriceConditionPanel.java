/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import com.ib.client.ContractLookuper;
import com.ib.client.PriceCondition;

import com.ib.samples.apidemo.util.TCombo;

public class PriceConditionPanel extends ContractConditionPanel<PriceCondition> {

    enum Method {
        Default(0),
        DoubleBidAsk(1),
        Last(2),
        DoubleLast(3),
        BidAsk(4),
        LastBidAsk(7),
        MidPoint(8);

        private static String[] names = new String[]{"default", "double bid/ask", "last", "double last", "bid/ask", "", "", "last of bid/ask", "mid-point"};
        private int m_value;

        Method(int v) {
            m_value = v;
        }

        public int value() {
            return m_value;
        }

        @Override
        public String toString() {
            if (m_value < 0 || m_value >= names.length)
                return super.toString();

            return names[m_value];
        }

        static Method fromInt(int i) {
            for (Method m : Method.values()) {
                if (m.value() == i)
                    return m;
            }

            return null;
        }
    }

    final private TCombo<Method> m_method = new TCombo<>(Method.values());

    PriceConditionPanel(PriceCondition condition, ContractLookuper lookuper) {
        super(condition, lookuper);

        m_method.setSelectedItem(Method.fromInt(condition().triggerMethod()));
        m_value.setText(condition().price());

        add("Method", m_method);
        add("Operator", m_operator);
        add("Price", m_value);
    }

    @Override
    public PriceCondition onOK() {
        super.onOK();
        condition().price(m_value.getDouble());
        condition().triggerMethod(m_method.getSelectedItem().value());

        return condition();
    }

}
