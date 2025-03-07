/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import com.ib.client.ContractLookuper;
import com.ib.client.PercentChangeCondition;

public class PercentConditionPanel extends ContractConditionPanel<PercentChangeCondition> {

    PercentConditionPanel(PercentChangeCondition condition, ContractLookuper lookuper) {
        super(condition, lookuper);

        m_value.setText(condition().changePercent());

        add("Operator", m_operator);
        add("Percentage Change", m_value);
    }


    public PercentChangeCondition onOK() {
        super.onOK();
        condition().changePercent(m_value.getDouble());

        return condition();
    }
}
