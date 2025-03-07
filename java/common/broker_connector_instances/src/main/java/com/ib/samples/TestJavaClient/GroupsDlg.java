/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.ib.client.EClient;


public class GroupsDlg extends JDialog {

    public boolean m_rc;

    public int m_reqId;

    private JTextField m_txtReqId = new JTextField("0");
    private JTextField m_txtContractInfo = new JTextField("");

    private JButton m_btnQueryDisplayGroups = new JButton("Query Display Groups");
    private JButton m_btnSubscribeToGroupEvents = new JButton("Subscribe To Group Events");
    private JButton m_btnUnsubscribeFromGroupEvents = new JButton("Unsubscribe From Group Events");
    private JButton m_btnUpdateDisplayGroup = new JButton("Update Display Group");

    private JComboBox<String> m_cmbDisplayGroups = new JComboBox<>();

    private IBTextPanel m_txtGroupMessages = new IBTextPanel("Group Messages", false);

    private EClient m_client;

    GroupsDlg(SampleFrame owner, EClient client) {
        super(owner, true);

        m_client = client;

        setTitle("Display Groups");

        // create display groups panel
        JPanel groupsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        groupsPanel.add(new JLabel("Request ID:"));
        groupsPanel.add(m_txtReqId);
        groupsPanel.add(m_btnQueryDisplayGroups);
        groupsPanel.add(new JLabel(""));
        groupsPanel.add(new JLabel("Display Groups"));
        groupsPanel.add(m_cmbDisplayGroups);
        groupsPanel.add(m_btnSubscribeToGroupEvents);
        groupsPanel.add(m_btnUnsubscribeFromGroupEvents);
        groupsPanel.add(m_btnUpdateDisplayGroup);
        groupsPanel.add(new JLabel(""));
        groupsPanel.add(new JLabel("Contract Info"));
        groupsPanel.add(m_txtContractInfo);

        JPanel messagesPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        Dimension d = messagesPanel.getSize();
        d.height += 250;
        messagesPanel.setPreferredSize(d);
        messagesPanel.add(m_txtGroupMessages);

        // create button panel
        JPanel buttonPanel = new JPanel();
        JButton btnReset = new JButton("Reset");
        buttonPanel.add(btnReset);
        JButton btnClose = new JButton("Close");
        buttonPanel.add(btnClose);

        m_cmbDisplayGroups.setEnabled(false);
        m_btnSubscribeToGroupEvents.setEnabled(false);
        m_btnUnsubscribeFromGroupEvents.setEnabled(false);
        m_btnUpdateDisplayGroup.setEnabled(false);
        m_txtContractInfo.setEnabled(false);

        // create action listeners
        m_btnQueryDisplayGroups.addActionListener(e -> onQueryDisplayGroups());
        m_btnSubscribeToGroupEvents.addActionListener(e -> onSubscribeToGroupEvents());
        m_btnUnsubscribeFromGroupEvents.addActionListener(e -> onUnsubscribeFromGroupEvents());
        m_btnUpdateDisplayGroup.addActionListener(e -> onUpdateDisplayGroup());
        btnReset.addActionListener(e -> onReset());
        btnClose.addActionListener(e -> onClose());

        // create dlg box
        getContentPane().add(groupsPanel, BorderLayout.NORTH);
        getContentPane().add(messagesPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();
    }

    void onQueryDisplayGroups() {

        try {

            m_cmbDisplayGroups.removeAllItems();
            enableFields(false);

            int reqId = Integer.parseInt(m_txtReqId.getText());

            m_txtGroupMessages.add("Querying display groups reqId=" + reqId + " ...");

            m_client.queryDisplayGroups(reqId);
        } catch (Exception e) {
            Main.inform(this, "Error - " + e);
        }

    }

    void onSubscribeToGroupEvents() {

        try {

            int reqId = Integer.parseInt(m_txtReqId.getText());
            int groupId = Integer.parseInt(String.valueOf(m_cmbDisplayGroups.getSelectedItem()));

            m_txtGroupMessages.add("Subscribing to group events reqId=" + reqId + " groupId=" + groupId + " ...");

            m_client.subscribeToGroupEvents(reqId, groupId);
        } catch (Exception e) {
            Main.inform(this, "Error - " + e);
        }
    }

    void onUnsubscribeFromGroupEvents() {

        try {
            int reqId = Integer.parseInt(m_txtReqId.getText());

            m_txtGroupMessages.add("Unsubscribing from group events reqId=" + reqId + " ...");

            m_client.unsubscribeFromGroupEvents(reqId);
        } catch (Exception e) {
            Main.inform(this, "Error - " + e);
        }
    }

    void onUpdateDisplayGroup() {

        try {
            int reqId = Integer.parseInt(m_txtReqId.getText());

            String contractInfo = m_txtContractInfo.getText();

            if (!contractInfo.isEmpty()) {
                m_txtGroupMessages.add("Updating display group reqId=" + reqId + " contractInfo=" + contractInfo + " ...");

                m_client.updateDisplayGroup(reqId, contractInfo);
            }
        } catch (Exception e) {
            Main.inform(this, "Error - " + e);
        }
    }

    void onReset() {
        m_cmbDisplayGroups.removeAllItems();
        m_txtGroupMessages.clear();
        m_txtContractInfo.setText("");
        enableFields(false);
    }

    void onClose() {
        m_rc = false;
        setVisible(false);
    }

    void displayGroupList(int reqId, String groups) {

        if (groups != null) {
            enableFields(true);

            String[] groupArray = groups.split("[|]");

            for (String group : groupArray) {
                m_cmbDisplayGroups.addItem(group);
            }

            m_cmbDisplayGroups.setSelectedIndex(0);

            m_txtGroupMessages.add("Display groups: reqId=" + reqId + " groups=" + groups);
        } else {
            m_txtGroupMessages.add("Display groups: reqId=" + reqId + " groups=<empty>");
        }
    }

    void displayGroupUpdated(int reqId, String contractInfo) {
        m_txtGroupMessages.add("Display group updated: reqId=" + reqId + " contractInfo=" + contractInfo);
    }

    void enableFields(boolean enable) {
        m_cmbDisplayGroups.setEnabled(enable);
        m_btnSubscribeToGroupEvents.setEnabled(enable);
        m_btnUnsubscribeFromGroupEvents.setEnabled(enable);
        m_btnUpdateDisplayGroup.setEnabled(enable);
        m_txtContractInfo.setEnabled(enable);
    }

}
