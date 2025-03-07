/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;

import com.ib.client.TagValue;

public class SmartComboRoutingParamsDlg extends JDialog {
    private JTextField m_tag = new JTextField("");
    private JTextField m_value = new JTextField("");

    private SmartComboRoutingParamModel m_paramModel = new SmartComboRoutingParamModel();
    private JTable m_paramTable = new JTable(m_paramModel);
    private JScrollPane m_paramPane = new JScrollPane(m_paramTable);
    private List<TagValue> m_smartComboRoutingParams = new ArrayList<>();

    public SmartComboRoutingParamModel paramModel() {
        return m_paramModel;
    }

    public List<TagValue> smartComboRoutingParams() {
        return m_smartComboRoutingParams;
    }

    public SmartComboRoutingParamsDlg(String dlgTitle, List<TagValue> smartComboRoutingParams, Window owner) {
        super(owner, dlgTitle, ModalityType.APPLICATION_MODAL);

        setTitle(dlgTitle);

        // create smart combo routing params panel
        JPanel pParamList = new JPanel(new GridLayout(0, 1, 10, 10));
        pParamList.setBorder(BorderFactory.createTitledBorder("Parameters"));

        m_smartComboRoutingParams = smartComboRoutingParams;
        if (smartComboRoutingParams != null) {
            m_paramModel.smartComboRoutingParams().addAll(smartComboRoutingParams);
        }
        JScrollPane paramPane = new JScrollPane(m_paramTable);
        pParamList.add(paramPane);

        // create add/remove panel
        JPanel pParamListControl = new JPanel(new GridLayout(0, 2, 10, 10));
        pParamListControl.setBorder(BorderFactory.createTitledBorder("Add / Remove"));
        pParamListControl.add(new JLabel("Param:"));
        pParamListControl.add(m_tag);
        pParamListControl.add(new JLabel("Value:"));
        pParamListControl.add(m_value);
        JButton btnAddParam = new JButton("Add");
        pParamListControl.add(btnAddParam);
        JButton btnRemoveParam = new JButton("Remove");
        pParamListControl.add(btnRemoveParam);

        // create button panel
        JPanel buttonPanel = new JPanel();
        JButton btnOk = new JButton("OK");
        buttonPanel.add(btnOk);
        JButton btnCancel = new JButton("Cancel");
        buttonPanel.add(btnCancel);

        // create wrapper panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(pParamList);
        topPanel.add(pParamListControl);

        // create dlg box
        getContentPane().add(topPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // create action listeners
        btnAddParam.addActionListener(e -> onAddParam());
        btnRemoveParam.addActionListener(e -> onRemoveParam());
        btnOk.addActionListener(e -> onOk());
        btnCancel.addActionListener(e -> onCancel());

        setSize(250, 600);
        centerOnOwner(this);
    }

    void onAddParam() {
        try {
            String tag = m_tag.getText();
            String value = m_value.getText();

            m_paramModel.addParam(new TagValue(tag, value));
        } catch (Exception e) {
            reportError("Error - ", e);
        }
    }

    void onRemoveParam() {
        try {
            if (m_paramTable.getSelectedRowCount() != 0) {
                int[] rows = m_paramTable.getSelectedRows();
                for (int i = rows.length - 1; i >= 0; i--) {
                    m_paramModel.removeParam(rows[i]);
                }
            }
        } catch (Exception e) {
            reportError("Error - ", e);
        }
    }

    void onOk() {
        List<TagValue> smartComboRoutingParams = m_paramModel.smartComboRoutingParams();

        m_smartComboRoutingParams = smartComboRoutingParams.isEmpty() ? null : smartComboRoutingParams;

        setVisible(false);
    }

    void onCancel() {
        setVisible(false);
    }

    void reportError(String msg, Exception e) {
        Main.inform(this, msg + " --" + e);
    }

    private static void centerOnOwner(Window window) {
        Window owner = window.getOwner();
        if (owner == null) {
            return;
        }
        int x = owner.getX() + ((owner.getWidth() - window.getWidth()) / 2);
        int y = owner.getY() + ((owner.getHeight() - window.getHeight()) / 2);
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        window.setLocation(x, y);
    }
}

class SmartComboRoutingParamModel extends AbstractTableModel {
    private List<TagValue> m_allData = new ArrayList<>();

    synchronized void addParam(TagValue tagValue) {
        m_allData.add(tagValue);
        fireTableDataChanged();
    }

    synchronized void removeParam(int index) {
        m_allData.remove(index);
        fireTableDataChanged();
    }

    synchronized public void reset() {
        m_allData.clear();
        fireTableDataChanged();
    }

    synchronized public int getRowCount() {
        return m_allData.size();
    }

    synchronized public int getColumnCount() {
        return 2;
    }

    synchronized public Object getValueAt(int r, int c) {
        TagValue tagValue = m_allData.get(r);

        switch (c) {
            case 0:
                return tagValue.m_tag;
            case 1:
                return tagValue.m_value;
            default:
                return "";
        }

    }

    public boolean isCellEditable(int r, int c) {
        return false;
    }

    public String getColumnName(int c) {
        switch (c) {
            case 0:
                return "Param";
            case 1:
                return "Value";
            default:
                return null;
        }
    }

    public List<TagValue> smartComboRoutingParams() {
        return m_allData;
    }
}
