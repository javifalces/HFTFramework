/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo.util;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class UpperField extends JTextField {
    int m_ival;
    double m_dval;

    public UpperField() {
        this(null);
    }

    public UpperField(boolean allowLowerCase) {
        this(null, allowLowerCase);
    }

    public UpperField(int i) {
        this(null, i, false);
    }

    public UpperField(int i, boolean allowLowerCase) {
        this(null, i, allowLowerCase);
    }

    public UpperField(String s) {
        this(s, 7, false);
    }

    public UpperField(String s, boolean allowLowerCase) {
        this(s, 7, allowLowerCase);
    }

    public UpperField(String s, int i) {
        this(s, i, false);
    }

    public UpperField(String s, int i, boolean allowLowerCase) {
        super(i);

        if (!allowLowerCase) {
            setDocument(new PlainDocument() {
                @Override
                public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                    super.insertString(offs, str.toUpperCase(), a);
                }
            });
        }

        setText(s);
    }

    public void setText(double v) {
        if (v == Double.MAX_VALUE || v == 0) {
            m_dval = v;
            setText(null);
        } else {
            super.setText("" + v);
        }
    }

    public void setText(int v) {
        if (v == Integer.MAX_VALUE || v == 0) {
            m_ival = v;
            setText(null);
        } else {
            super.setText("" + v);
        }
    }

    public double getDouble() {
        try {
            String str = super.getText();
            return str == null || str.length() == 0
                    ? m_dval : Double.parseDouble(super.getText().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public int getInt() {
        try {
            String str = super.getText();
            return str == null || str.length() == 0
                    ? m_ival : Integer.parseInt(super.getText().trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
