/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JDialog;

class FinancialAdvisorDlg extends JDialog {
    private static final int DIALOG_WIDTH = 500;
    private static final int EDITOR_HEIGHT = 240;
    private IBTextPanel groupTextEditor = new IBTextPanel("Groups", true);
    private IBTextPanel profileTextEditor = new IBTextPanel("Allocation Profiles", true);
    private IBTextPanel aliasTextEditor = new IBTextPanel("Aliases", true);
    String groupsXML;
    String profilesXML;
    String aliasesXML;
    boolean m_rc = false;

    FinancialAdvisorDlg(Frame owner) {
        super(owner, "Financial Advisor", true);

        IBGridBagPanel editPanel = new IBGridBagPanel();

        editPanel.SetObjectPlacement(groupTextEditor, 0, 0);
        editPanel.SetObjectPlacement(profileTextEditor, 0, 1);
        editPanel.SetObjectPlacement(aliasTextEditor, 0, 2);
        Dimension editPanelSizeDimension =
                new Dimension(DIALOG_WIDTH, 3 * EDITOR_HEIGHT);
        editPanel.setPreferredSize(editPanelSizeDimension);

        IBGridBagPanel buttonPanel = new IBGridBagPanel();
        JButton btnOk = new JButton("OK");
        buttonPanel.add(btnOk);
        JButton btnCancel = new JButton("Cancel");
        buttonPanel.add(btnCancel);

        // create action listeners
        btnOk.addActionListener(e -> onOk());
        btnCancel.addActionListener(e -> onCancel());

        //setTitle( "Financial Advisor");
        getContentPane().add(editPanel, BorderLayout.NORTH);
        getContentPane().add(buttonPanel, BorderLayout.CENTER);
        pack();
    }

    void receiveInitialXML(String p_groupsXML, String p_profilesXML, String p_aliasesXML) {
        groupTextEditor.setTextDetabbed(p_groupsXML);
        profileTextEditor.setTextDetabbed(p_profilesXML);
        aliasTextEditor.setTextDetabbed(p_aliasesXML);
    }

    void onOk() {
        m_rc = true;
        groupsXML = groupTextEditor.getText();
        profilesXML = profileTextEditor.getText();
        aliasesXML = aliasTextEditor.getText();
        setVisible(false);
    }

    void onCancel() {
        m_rc = false;
        setVisible(false);
    }
}
