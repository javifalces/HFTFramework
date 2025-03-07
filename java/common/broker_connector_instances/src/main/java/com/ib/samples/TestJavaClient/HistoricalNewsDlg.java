/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.ib.client.TagValue;

public class HistoricalNewsDlg extends JDialog {

    public boolean m_rc;

    private JTextField m_requestId = new JTextField("0");
    private JTextField m_conId = new JTextField("8314");
    private JTextField m_providerCodes = new JTextField("BZ+FLY");
    private JTextField m_startDateTime = new JTextField();
    private JTextField m_endDateTime = new JTextField();
    private JTextField m_totalResults = new JTextField("10");
    private List<TagValue> m_options = new ArrayList<>();

    int m_retRequestId;
    int m_retConId;
    String m_retProviderCodes;
    String m_retStartDateTime;
    String m_retEndDateTime;
    int m_retTotalResults;

    HistoricalNewsDlg(JFrame owner) {
        super(owner, true);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.0");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -3);
        m_endDateTime.setText(df.format(cal.getTime()));
        cal.add(Calendar.DATE, -1);
        m_startDateTime.setText(df.format(cal.getTime()));

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
        midPanel.add(new JLabel("Request Id"));
        midPanel.add(m_requestId);
        midPanel.add(new JLabel("Con Id"));
        midPanel.add(m_conId);
        midPanel.add(new JLabel("Provider Codes"));
        midPanel.add(m_providerCodes);
        midPanel.add(new JLabel("Start Date/Time (yyyy-MM-dd HH:mm:ss.0)"));
        midPanel.add(m_startDateTime);
        midPanel.add(new JLabel("End Date/Time (yyyy-MM-dd HH:mm:ss.0)"));
        midPanel.add(m_endDateTime);
        midPanel.add(new JLabel("Total Results"));
        midPanel.add(m_totalResults);

        // misc options button
        JButton btnOptions = new JButton("Misc Options");
        midPanel.add(btnOptions);
        btnOptions.addActionListener(e -> onBtnOptions());

        // create dlg box
        getContentPane().add(midPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        setTitle("Request Historical News");
        pack();
    }

    void init(List<TagValue> options) {
        m_options = options;
    }

    void onBtnOptions() {
        SmartComboRoutingParamsDlg smartComboRoutingParamsDlg = new SmartComboRoutingParamsDlg("Misc Options", m_options, this);

        // show smart combo routing params dialog
        smartComboRoutingParamsDlg.setVisible(true);

        m_options = smartComboRoutingParamsDlg.smartComboRoutingParams();
    }

    List<TagValue> getOptions() {
        return m_options;
    }

    void onOk() {
        m_rc = false;

        try {
            m_retRequestId = Integer.parseInt(m_requestId.getText());
            m_retConId = Integer.parseInt(m_conId.getText());
            m_retProviderCodes = m_providerCodes.getText().trim();
            m_retStartDateTime = m_startDateTime.getText().trim();
            m_retEndDateTime = m_endDateTime.getText().trim();
            m_retTotalResults = Integer.parseInt(m_totalResults.getText());
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