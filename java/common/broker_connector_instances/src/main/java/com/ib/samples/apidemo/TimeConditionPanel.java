/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import com.ib.client.TimeCondition;

public class TimeConditionPanel extends OperatorConditionPanel<TimeCondition> {

    TimeConditionPanel(TimeCondition condition) {
        super(condition);

        m_value.setText(condition().time());

        add("Operator", m_operator);
        add("Time", m_value);
    }

    @Override
    public TimeCondition onOK() {
        super.onOK();
        condition().time(m_value.getText());

        return condition();
    }
}
