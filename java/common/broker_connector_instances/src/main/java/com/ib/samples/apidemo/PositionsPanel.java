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

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.controller.ApiController.IPositionHandler;
import com.ib.controller.Formats;

import com.ib.samples.apidemo.AccountInfoPanel.Table;
import com.ib.samples.apidemo.util.HtmlButton;
import com.ib.samples.apidemo.util.NewTabbedPanel.NewTabPanel;
import com.ib.samples.apidemo.util.VerticalPanel;


public class PositionsPanel extends NewTabPanel {
    private PositionModel m_model = new PositionModel();
    private boolean m_complete;

    PositionsPanel() {
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

        JTable table = new Table(m_model, 2);
        JScrollPane scroll = new JScrollPane(table);

        setLayout(new BorderLayout());
        add(scroll);
        add(buts, BorderLayout.EAST);
    }

    /**
     * Called when the tab is first visited. Sends request for all positions.
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
        ApiDemo.INSTANCE.controller().reqPositions(m_model);
    }

    private void desubscribe() {
        ApiDemo.INSTANCE.controller().cancelPositions(m_model);
        m_model.clear();
    }

    private class PositionModel extends AbstractTableModel implements IPositionHandler {
        Map<PositionKey, PositionRow> m_map = new HashMap<>();
        List<PositionRow> m_list = new ArrayList<>();

        @Override
        public void position(String account, Contract contract, Decimal position, double avgCost) {
            PositionKey key = new PositionKey(account, contract.conid());
            PositionRow row = m_map.get(key);
            if (row == null) {
                row = new PositionRow();
                m_map.put(key, row);
                m_list.add(row);
            }
            row.update(account, contract, position, avgCost);

            if (m_complete) {
                m_model.fireTableDataChanged();
            }
        }

        @Override
        public void positionEnd() {
            m_model.fireTableDataChanged();
            m_complete = true;
        }

        public void clear() {
            m_map.clear();
            m_list.clear();
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return m_map.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return "Account";
                case 1:
                    return "Contract";
                case 2:
                    return "Position";
                case 3:
                    return "Avg Cost";
                default:
                    return null;
            }
        }

        @Override
        public Object getValueAt(int rowIn, int col) {
            PositionRow row = m_list.get(rowIn);

            switch (col) {
                case 0:
                    return row.m_account;
                case 1:
                    return row.m_contract.textDescription();
                case 2:
                    return row.m_position;
                case 3:
                    return Formats.fmt(row.m_avgCost);
                default:
                    return null;
            }
        }
    }

    private static class PositionKey {
        String m_account;
        int m_conid;

        PositionKey(String account, int conid) {
            m_account = account;
            m_conid = conid;
        }

        @Override
        public int hashCode() {
            return m_account.hashCode() + m_conid;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PositionKey)) {
                return false;
            }
            PositionKey other = (PositionKey) obj;
            return m_account.equals(other.m_account) && m_conid == other.m_conid;
        }
    }

    private static class PositionRow {
        String m_account;
        Contract m_contract;
        Decimal m_position;
        double m_avgCost;

        void update(String account, Contract contract, Decimal position, double avgCost) {
            m_account = account;
            m_contract = contract;
            m_position = position;
            m_avgCost = avgCost;
        }
    }
}
