/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class PnLModel extends AbstractTableModel {

    private class PnLItem {

        public PnLItem(double dailyPnL, double unrealizedPnL, double realizedPnL) {
            m_dailyPnL = dailyPnL;
            m_unrealizedPnL = unrealizedPnL;
            m_realizedPnL = realizedPnL;
        }

        public double dailyPnL() {
            return m_dailyPnL;
        }

        public double unrealizedPnL() {
            return m_unrealizedPnL;
        }

        public double realizedPnL() {
            return m_realizedPnL;
        }

        private double m_dailyPnL;
        private double m_unrealizedPnL;
        private double m_realizedPnL;

    }

    private List<PnLItem> m_rows = new ArrayList<>();
    private static Dictionary<Integer, String> columnNames = new Hashtable<>();

    static {
        columnNames.put(0, "Daily Pnl");
        columnNames.put(1, "Unrealized PnL");
        columnNames.put(2, "Realized PnL");
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
    }

    @Override
    public int getRowCount() {
        return m_rows.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PnLItem item = m_rows.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return item.dailyPnL();

            case 1:
                return item.unrealizedPnL();

            case 2:
                return item.realizedPnL();
        }

        return null;
    }

    public void addRow(double dailyPnL, double unrealizedPnL, double realizedPnL) {
        m_rows.add(new PnLItem(dailyPnL, unrealizedPnL, realizedPnL));

        fireTableDataChanged();
    }

}
