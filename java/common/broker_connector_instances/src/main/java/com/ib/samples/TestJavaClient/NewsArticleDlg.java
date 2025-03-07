/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.ib.client.TagValue;

public class NewsArticleDlg extends JDialog {

    public boolean m_rc;

    private JTextField m_requestId = new JTextField("0");
    private JTextField m_providerCode = new JTextField();
    private JTextField m_articleId = new JTextField();
    private JTextField m_path = new JTextField(System.getProperty("user.dir"));
    private List<TagValue> m_options = new ArrayList<>();

    int m_retRequestId;
    String m_retProviderCode;
    String m_retArticleId;
    String m_retPath;

    NewsArticleDlg(JFrame owner) {
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
        midPanel.add(new JLabel("Request Id"));
        midPanel.add(m_requestId);
        midPanel.add(new JLabel("Provider Code"));
        midPanel.add(m_providerCode);
        midPanel.add(new JLabel("Article Id"));
        midPanel.add(m_articleId);
        midPanel.add(new JLabel("Path to save binary/pdf"));
        midPanel.add(m_path);

        JButton choosePathDialogButton = new JButton("...");
        JFileChooser chooser = new JFileChooser(m_path.getText());

        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        choosePathDialogButton.addActionListener(e -> m_path.setText(chooser.showOpenDialog(midPanel) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().getPath() : m_path.getText()));

        midPanel.add(choosePathDialogButton);

        // misc options button
        JButton btnOptions = new JButton("Misc Options");
        midPanel.add(btnOptions);
        btnOptions.addActionListener(e -> onBtnOptions());

        // create dlg box
        getContentPane().add(midPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        setTitle("Request News Article");
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
            m_retProviderCode = m_providerCode.getText().trim();
            m_retArticleId = m_articleId.getText().trim();
            m_retPath = m_path.getText().trim() + "\\" + m_retArticleId + ".pdf";
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