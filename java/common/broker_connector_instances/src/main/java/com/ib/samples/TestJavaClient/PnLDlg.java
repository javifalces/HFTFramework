/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.GridLayout;

import javax.swing.*;

public class PnLDlg extends JDialogBox {

    int m_reqId;
    String m_account;
    String m_modelCode;

    private JTextField m_reqIdField = new JTextField();
    private JTextField m_accountField = new JTextField();
    private JTextField m_modelCodeField = new JTextField();

    protected JPanel m_editsPanel = new JPanel(new GridLayout(0, 1));

    public PnLDlg(JFrame parent) {
        super(parent);

        m_editsPanel.add(new JLabel("Req id"));
        m_editsPanel.add(m_reqIdField);

        m_editsPanel.add(new JLabel("Account"));
        m_editsPanel.add(m_accountField);

        m_editsPanel.add(new JLabel("Model code"));
        m_editsPanel.add(m_modelCodeField);

        getContentPane().add(m_editsPanel);
        pack();
    }

    @Override
    protected void onOk() {
        try {
            m_reqId = Integer.parseInt(m_reqIdField.getText());
            m_account = m_accountField.getText();
            m_modelCode = m_modelCodeField.getText();
        } finally {
            super.onOk();
        }
    }

}
