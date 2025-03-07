/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextField;

import com.ib.samples.apidemo.util.UpperField;
import com.ib.samples.apidemo.util.VerticalPanel;

public class SmartComponentsParamsReqDlg extends JDialog {

    private int m_id;
    private String m_BBOExchange;
    private boolean m_isOk;

    final private UpperField m_idFld = new UpperField("0");
    final private JTextField m_BBOExchangeFld = new JTextField();


    SmartComponentsParamsReqDlg(SampleFrame owner) {
        super(owner);

        VerticalPanel paramsPanel = new VerticalPanel();
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");

        ok.addActionListener(e -> onOK());
        cancel.addActionListener(e -> onCancel());

        paramsPanel.add("Req Id", m_idFld);
        paramsPanel.add("BBO Exchange", m_BBOExchangeFld);
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
        m_BBOExchange = m_BBOExchangeFld.getText();
        m_id = m_idFld.getInt();
        m_isOk = true;

        dispose();
    }

    public boolean isOK() {
        return m_isOk;
    }

    String BBOExchange() {
        return m_BBOExchange;
    }

    public int id() {
        return m_id;
    }
}
