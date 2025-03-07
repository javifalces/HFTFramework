/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class PositionsDlg extends JDialog {

    public boolean m_rc;

    private JTextField m_id = new JTextField("0");
    private JTextField m_account = new JTextField();
    private JTextField m_modelCode = new JTextField();
    private JCheckBox m_ledgerAndNLV = new JCheckBox("LedgerAndNLV", false);

    int m_retId;
    String m_retAccount;
    String m_retModelCode;
    boolean m_retLedgerAndNLV;

    PositionsDlg(JFrame owner) {
        super(owner, true);

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
        midPanel.add(new JLabel("Id"));
        midPanel.add(m_id);
        midPanel.add(new JLabel("Account"));
        midPanel.add(m_account);
        midPanel.add(new JLabel("Model Code"));
        midPanel.add(m_modelCode);
        midPanel.add(m_ledgerAndNLV);

        // create dlg box
        getContentPane().add(midPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        setTitle("Request Positions/Account Updates");
        pack();
    }

    void onOk() {
        m_rc = false;

        try {
            m_retId = Integer.parseInt(m_id.getText());
            m_retAccount = m_account.getText().trim();
            m_retModelCode = m_modelCode.getText().trim();
            m_retLedgerAndNLV = m_ledgerAndNLV.isSelected();
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