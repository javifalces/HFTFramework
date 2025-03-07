/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.ib.controller.AccountSummaryTag;
import com.ib.controller.ApiController.IAccountSummaryHandler;
import com.ib.controller.Formats;

import com.ib.samples.apidemo.AccountInfoPanel.Table;
import com.ib.samples.apidemo.util.HtmlButton;
import com.ib.samples.apidemo.util.NewTabbedPanel.NewTabPanel;
import com.ib.samples.apidemo.util.VerticalPanel;


public class AccountSummaryPanel extends NewTabPanel {
    private SummaryModel m_model = new SummaryModel();

    AccountSummaryPanel() {
        HtmlButton sub = new HtmlButton("Subscribe") {
            protected void actionPerformed() {
                subscribe();
            }
        };

        HtmlButton desub = new HtmlButton("Desubscribe") {
            protected void actionPerformed() {
                desubscribe();
            }
        };

        JPanel buts = new VerticalPanel();
        buts.add(sub);
        buts.add(desub);

        JTable table = new Table(m_model);
        JScrollPane scroll = new JScrollPane(table);

        setLayout(new BorderLayout());
        add(scroll);
        add(buts, BorderLayout.EAST);
    }

    /**
     * Called when the tab is first visited.
     */
    @Override
    public void activated() {
        subscribe();
    }

    /**
     * Called when the tab is closed by clicking the X.
     */
    @Override
    public void closed() {
        desubscribe();
    }

    private void subscribe() {
        ApiDemo.INSTANCE.controller().reqAccountSummary("All", AccountSummaryTag.values(), m_model);
    }

    private void desubscribe() {
        ApiDemo.INSTANCE.controller().cancelAccountSummary(m_model);
        m_model.clear();
    }

    private static class SummaryModel extends AbstractTableModel implements IAccountSummaryHandler {
        List<SummaryRow> m_rows = new ArrayList<>();
        Map<String, SummaryRow> m_map = new HashMap<>();
        boolean m_complete;

        public void clear() {
            ApiDemo.INSTANCE.controller().cancelAccountSummary(this);
            m_rows.clear();
            m_map.clear();
            m_complete = false;
            fireTableDataChanged();
        }

        @Override
        public void accountSummary(String account, AccountSummaryTag tag, String value, String currency) {
            SummaryRow row = m_map.get(account);
            if (row == null) {
                row = new SummaryRow();
                m_map.put(account, row);
                m_rows.add(row);
            }
            row.update(account, tag, value);

            if (m_complete) {
                fireTableDataChanged();
            }
        }

        @Override
        public void accountSummaryEnd() {
            fireTableDataChanged();
            m_complete = true;
        }

        @Override
        public int getRowCount() {
            return m_rows.size();
        }

        @Override
        public int getColumnCount() {
            return AccountSummaryTag.values().length + 1; // add one for Account column
        }

        @Override
        public String getColumnName(int col) {
            if (col == 0) {
                return "Account";
            }
            return AccountSummaryTag.values()[col - 1].toString();
        }

        @Override
        public Object getValueAt(int rowIn, int col) {
            SummaryRow row = m_rows.get(rowIn);

            if (col == 0) {
                return row.m_account;
            }

            AccountSummaryTag tag = AccountSummaryTag.values()[col - 1];
            String val = row.m_map.get(tag);

            switch (tag) {
                case Cushion:
                    return fmtPct(val);
                case LookAheadNextChange:
                    return fmtTime(val);
                default:
                    return AccountInfoPanel.format(val, null);
            }
        }

        String fmtPct(String val) {
            return val == null || val.length() == 0 ? null : Formats.fmtPct(Double.parseDouble(val));
        }

        String fmtTime(String val) {
            return val == null || val.length() == 0 || val.equals("0") ? null : Formats.fmtDate(Long.parseLong(val) * 1000);
        }
    }

    private static class SummaryRow {
        String m_account;
        Map<AccountSummaryTag, String> m_map = new HashMap<>();

        public void update(String account, AccountSummaryTag tag, String value) {
            m_account = account;
            m_map.put(tag, value);
        }
    }
}
