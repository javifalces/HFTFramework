/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JDialog;

import com.ib.samples.apidemo.util.UpperField;
import com.ib.samples.apidemo.util.VerticalPanel;

public class SecDefOptParamsReqDlg extends JDialog {
    private final UpperField m_idFld = new UpperField("0");
    private final UpperField m_underlyingSymbolFld = new UpperField();
    private final UpperField m_futFopExchangeFld = new UpperField();
    private final UpperField m_underlyingSecTypeFld = new UpperField();
    private final UpperField m_underlyingConIdFld = new UpperField();
    private int m_id;
    private String m_underlyingSymbol;
    private String m_futFopExchange;
    private String m_underlyingSecType;
    private int m_underlyingConId;
    private boolean m_isOk;

    SecDefOptParamsReqDlg(SampleFrame owner) {
        super(owner);

        VerticalPanel paramsPanel = new VerticalPanel();
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");

        ok.addActionListener(e -> onOK());
        cancel.addActionListener(e -> onCancel());

        paramsPanel.add("Req Id", m_idFld);
        paramsPanel.add("Underlying symbol", m_underlyingSymbolFld);
        paramsPanel.add("FUT-FOP exchange", m_futFopExchangeFld);
        paramsPanel.add("Underlying security type", m_underlyingSecTypeFld);
        paramsPanel.add("Underlying contract id", m_underlyingConIdFld);
        paramsPanel.add(ok);
        paramsPanel.add(cancel);
        setLayout(new BorderLayout());
        add(paramsPanel, BorderLayout.NORTH);
        pack();
    }

    protected void onCancel() {
        m_isOk = false;

        dispose();
    }

    protected void onOK() {
        m_id = m_idFld.getInt();
        m_underlyingSymbol = m_underlyingSymbolFld.getText();
        m_futFopExchange = m_futFopExchangeFld.getText();
        m_underlyingSecType = m_underlyingSecTypeFld.getText();
        m_underlyingConId = m_underlyingConIdFld.getInt();
        m_isOk = true;

        dispose();
    }

    public boolean isOK() {
        return m_isOk;
    }

    public int id() {
        return m_id;
    }

    public String underlyingSecType() {
        return m_underlyingSecType;
    }

    public String underlyingSymbol() {
        return m_underlyingSymbol;
    }

    public String futFopExchange() {
        return m_futFopExchange;
    }

    public int underlyingConId() {
        return m_underlyingConId;
    }

}
