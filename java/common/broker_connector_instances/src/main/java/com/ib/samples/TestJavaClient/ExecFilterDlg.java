/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.ib.client.ExecutionFilter;

public class ExecFilterDlg extends JDialog {

    int m_reqId;
    ExecutionFilter m_execFilter = new ExecutionFilter();
    public boolean m_rc;

    private JTextField m_reqIdTxt = new JTextField("0");
    private JTextField m_clientID = new JTextField("0");
    private JTextField m_acctCode = new JTextField();
    private JTextField m_time = new JTextField();
    private JTextField m_symbol = new JTextField();
    private JTextField m_secType = new JTextField();
    private JTextField m_exchange = new JTextField();
    private JTextField m_action = new JTextField();

    ExecFilterDlg(JFrame owner) {
        super(owner, true);

        setTitle("Execution Report Filter");

        // create extended order attributes panel
        JPanel execRptFilterPanel = new JPanel(new GridLayout(0, 2, 7, 7));
        execRptFilterPanel.setBorder(BorderFactory.createTitledBorder("Filter Criteria"));
        execRptFilterPanel.add(new JLabel("Request ID:"));
        execRptFilterPanel.add(m_reqIdTxt);
        execRptFilterPanel.add(new JLabel("Client ID:"));
        execRptFilterPanel.add(m_clientID);
        execRptFilterPanel.add(new JLabel("Account Code:"));
        execRptFilterPanel.add(m_acctCode);
        execRptFilterPanel.add(new JLabel("Time :"));
        execRptFilterPanel.add(m_time);
        execRptFilterPanel.add(new JLabel("Symbol :"));
        execRptFilterPanel.add(m_symbol);
        execRptFilterPanel.add(new JLabel("SecType :"));
        execRptFilterPanel.add(m_secType);
        execRptFilterPanel.add(new JLabel("Exchange :"));
        execRptFilterPanel.add(m_exchange);
        execRptFilterPanel.add(new JLabel("Action :"));
        execRptFilterPanel.add(m_action);

        // create button panel
        JPanel buttonPanel = new JPanel();
        JButton btnOk = new JButton("OK");
        buttonPanel.add(btnOk);
        JButton btnCancel = new JButton("Cancel");
        buttonPanel.add(btnCancel);

        // create action listeners
        btnOk.addActionListener(e -> onOk());
        btnCancel.addActionListener(e -> onCancel());

        // create dlg box
        getContentPane().add(execRptFilterPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();
    }

    void onOk() {
        m_rc = false;

        try {

            m_reqId = Integer.parseInt(m_reqIdTxt.getText());

            // set extended order fields
            String clientId = m_clientID.getText();
            if (clientId.equals("")) {
                m_execFilter.clientId(0);
            } else {
                m_execFilter.clientId(Integer.parseInt(clientId));
            }
            m_execFilter.acctCode(m_acctCode.getText());
            m_execFilter.time(m_time.getText());
            m_execFilter.symbol(m_symbol.getText());
            m_execFilter.secType(m_secType.getText());
            m_execFilter.exchange(m_exchange.getText());
            m_execFilter.side(m_action.getText());
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
