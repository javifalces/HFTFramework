/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import com.ib.client.MarginCondition;

public class MarginConditionPanel extends OperatorConditionPanel<MarginCondition> {

    MarginConditionPanel(MarginCondition condition) {
        super(condition);

        m_value.setText(condition().percent());

        add("Operator", m_operator);
        add("Cushion (%)", m_value);
    }

    public MarginCondition onOK() {
        super.onOK();
        condition().percent(m_value.getInt());

        return condition();
    }
}
