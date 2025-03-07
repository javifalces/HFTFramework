/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

class MarketRuleDlg extends JDialog {
    private JTextField m_marketRuleId = new JTextField("0");
    private boolean m_rc;

    int m_retMarketRuleId;

    boolean rc() {
        return m_rc;
    }

    MarketRuleDlg(JFrame parent) {
        super(parent, true);

        // create button panel
        JPanel buttonPanel = new JPanel();
        JButton btnOk = new JButton("OK");
        buttonPanel.add(btnOk);
        JButton btnCancel = new JButton("Cancel");
        buttonPanel.add(btnCancel);

        // create action listeners
        btnOk.addActionListener(e -> onOk());
        btnCancel.addActionListener(e -> onCancel());

        // create mid summary panel
        JPanel midPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        midPanel.add(new JLabel("Market Rule Id"));
        midPanel.add(m_marketRuleId);

        // create dlg box
        getContentPane().add(midPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        setTitle("Market Rule Request");
        pack();
    }

    void onOk() {
        m_rc = false;

        try {
            m_retMarketRuleId = Integer.parseInt(m_marketRuleId.getText());
        } catch (Exception e) {
            Main.inform(this, "Error - " + e);
            return;
        }

        m_rc = true;
        setVisible(false);
    }

    void onCancel() {
        m_rc = false;
        setVisible(false);
    }
}
