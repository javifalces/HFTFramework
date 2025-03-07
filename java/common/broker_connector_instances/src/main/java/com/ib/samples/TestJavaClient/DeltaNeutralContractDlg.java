/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.ib.client.DeltaNeutralContract;


public class DeltaNeutralContractDlg extends JDialog {
    private DeltaNeutralContract m_deltaNeutralContract;

    private JTextField m_txtConId = new JTextField();
    private JTextField m_txtDelta = new JTextField();
    private JTextField m_txtPrice = new JTextField();

    private boolean m_ok = false;
    private boolean m_reset = false;

    private static final int COL1_WIDTH = 30;
    private static final int COL2_WIDTH = 100 - COL1_WIDTH;

    DeltaNeutralContractDlg(DeltaNeutralContract deltaNeutralContract, JDialog owner) {
        super(owner, true);

        m_deltaNeutralContract = deltaNeutralContract;

        // create button panel
        JPanel buttonPanel = new JPanel();
        JButton btnOk = new JButton("OK");
        buttonPanel.add(btnOk);
        JButton btnReset = new JButton("Reset");
        buttonPanel.add(btnReset);
        JButton btnCancel = new JButton("Cancel");
        buttonPanel.add(btnCancel);

        // create action listeners
        btnOk.addActionListener(e -> onOk());
        btnReset.addActionListener(e -> onReset());
        btnCancel.addActionListener(e -> onCancel());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weighty = 100;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridheight = 1;

        // create mid panel
        IBGridBagPanel midPanel = new IBGridBagPanel();
        midPanel.setBorder(BorderFactory.createTitledBorder("Delta-Neutral Contract"));
        addGBComponent(midPanel, new JLabel("Contract Id"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(midPanel, m_txtConId, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(midPanel, new JLabel("Delta"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(midPanel, m_txtDelta, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(midPanel, new JLabel("Price"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(midPanel, m_txtPrice, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);


        m_txtConId.setText(Integer.toString(m_deltaNeutralContract.conid()));
        m_txtDelta.setText(Double.toString(m_deltaNeutralContract.delta()));
        m_txtPrice.setText(Double.toString(m_deltaNeutralContract.price()));

        // create dlg box
        getContentPane().add(midPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        setTitle("Delta Neutral");
        pack();
    }


    private void onOk() {

        try {
            int conId = Integer.parseInt(m_txtConId.getText());
            double delta = Double.parseDouble(m_txtDelta.getText());
            double price = Double.parseDouble(m_txtPrice.getText());

            m_deltaNeutralContract.conid(conId);
            m_deltaNeutralContract.delta(delta);
            m_deltaNeutralContract.price(price);
            m_ok = true;
            setVisible(false);
        } catch (Exception e) {
            Main.inform(this, "Error - " + e);
        }
    }

    private void onReset() {
        m_deltaNeutralContract.conid(0);
        m_deltaNeutralContract.delta(0);
        m_deltaNeutralContract.price(0);
        m_reset = true;
        setVisible(false);
    }

    private void onCancel() {
        setVisible(false);
    }

    public boolean ok() {
        return m_ok;
    }

    public boolean reset() {
        return m_reset;
    }

    private static void addGBComponent(IBGridBagPanel panel, Component comp,
                                       GridBagConstraints gbc, int weightx, int gridwidth) {
        gbc.weightx = weightx;
        gbc.gridwidth = gridwidth;
        panel.setConstraints(comp, gbc);
        panel.add(comp, gbc);
    }
}
