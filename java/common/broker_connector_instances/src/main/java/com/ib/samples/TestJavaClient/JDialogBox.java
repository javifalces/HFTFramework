/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class JDialogBox extends JDialog {

    private boolean m_isOk = false;

    public boolean isOk() {
        return m_isOk;
    }

    public JDialogBox(Window parent) {
        super(parent, ModalityType.APPLICATION_MODAL);

        JPanel buttonPanel = new JPanel();
        JButton btnOk = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");

        btnOk.addActionListener(e -> onOk());
        btnCancel.addActionListener(e -> onCancel());

        buttonPanel.add(btnOk);
        buttonPanel.add(btnCancel);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();
    }

    protected void onOk() {
        m_isOk = true;

        setVisible(false);
    }

    protected void onCancel() {
        m_isOk = false;

        setVisible(false);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            m_isOk = false;
        }

        super.setVisible(b);
    }

}
