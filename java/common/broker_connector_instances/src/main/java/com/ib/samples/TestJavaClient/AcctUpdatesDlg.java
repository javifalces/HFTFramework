/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */


package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

class AcctUpdatesDlg extends JDialog {
    private JTextField m_txtAcctCode = new JTextField(10);
    String m_acctCode;
    boolean m_subscribe = false;
    boolean m_rc;

    AcctUpdatesDlg(SampleFrame owner) {
        super(owner, true);

        setTitle("Account Updates (FA Customers only)");
        setSize(200, 300);

        m_txtAcctCode.setMaximumSize(m_txtAcctCode.getPreferredSize());


        Box row1 = Box.createHorizontalBox();
        row1.add(new JLabel(" Enter the account code for the FA managed \n account you wish to receive updates for :"));

        Box row2 = Box.createHorizontalBox();
        row2.add(new JLabel("Account Code :"));
        row2.add(Box.createHorizontalStrut(10));
        row2.add(m_txtAcctCode);

        Box row3 = Box.createHorizontalBox();
        JButton btnSubscribe = new JButton("Subscribe");
        row3.add(btnSubscribe);
        row3.add(Box.createHorizontalStrut(10));
        JButton btnUnSubscribe = new JButton("UnSubscribe");
        row3.add(btnUnSubscribe);


        Box vbox = Box.createVerticalBox();
        vbox.add(Box.createVerticalStrut(10));
        vbox.add(row1);
        vbox.add(Box.createVerticalStrut(10));
        vbox.add(row2);
        vbox.add(Box.createVerticalStrut(10));
        vbox.add(row3);
        vbox.add(Box.createVerticalStrut(10));

        // create account chooser panel
        JPanel acctChooserPanel = new JPanel();
        acctChooserPanel.setBorder(BorderFactory.createTitledBorder(""));
        acctChooserPanel.add(vbox);

        // create button panel
        JPanel buttonPanel = new JPanel();
        JButton btnClose = new JButton("Close");
        buttonPanel.add(btnClose);

        // create action listeners
        btnSubscribe.addActionListener(e -> onSubscribe());
        btnUnSubscribe.addActionListener(e -> onUnSubscribe());
        btnClose.addActionListener(e -> onClose());

        // create dlg box
        getContentPane().add(acctChooserPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();
    }

    private void onSubscribe() {
        m_subscribe = true;
        m_acctCode = m_txtAcctCode.getText();

        m_rc = true;
        setVisible(false);
    }

    private void onUnSubscribe() {
        m_subscribe = false;
        m_acctCode = m_txtAcctCode.getText();

        m_rc = true;
        setVisible(false);
    }

    private void onClose() {
        m_acctCode = "";
        m_rc = false;
        setVisible(false);
    }
}
