/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import com.ib.samples.apidemo.util.NewTabbedPanel;

public class OptionsPanel extends NewTabbedPanel {
    private final OptionChainsPanel m_optionChains = new OptionChainsPanel();
    private final ExercisePanel m_exercisePanel = new ExercisePanel();

    OptionsPanel() {
        NewTabbedPanel tabs = this;
        tabs.addTab("Option Chains", m_optionChains);
        tabs.addTab("Option Exercise", m_exercisePanel);
    }
}
