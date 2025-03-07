/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo.util;

import javax.swing.*;

public class TCombo<T> extends JComboBox<T> {
    @SafeVarargs
    public TCombo(T... items) {
        super(items);
    }

    public String getText() {
        return getSelectedItem() == null ? null : getSelectedItem().toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getSelectedItem() {
        return (T) super.getSelectedItem();
    }
}
