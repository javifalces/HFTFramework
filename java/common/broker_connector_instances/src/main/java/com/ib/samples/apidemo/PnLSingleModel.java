/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.ib.client.Decimal;

public class PnLSingleModel extends AbstractTableModel {

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Pos";
            case 1:
                return "Daily PnL";
            case 2:
                return "Unrealized PnL";
            case 3:
                return "Realized PnL";
            case 4:
                return "Value";
            default:
                return super.getColumnName(column);
        }
    }

    static class Row {
        Decimal m_pos;
        double m_dailyPnL;
        double m_unrealizedPnL;
        double m_realizedPnL;
        double m_value;

        public Row(Decimal pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value) {
            m_pos = pos;
            m_dailyPnL = dailyPnL;
            m_unrealizedPnL = unrealizedPnL;
            m_realizedPnL = realizedPnL;
            m_value = value;
        }
    }

    List<Row> m_rows = new ArrayList<>();

    @Override
    public int getRowCount() {
        return m_rows.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Row r = m_rows.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return r.m_pos;
            case 1:
                return r.m_dailyPnL;
            case 2:
                return r.m_unrealizedPnL;
            case 3:
                return r.m_realizedPnL;
            case 4:
                return r.m_value;
            default:
                return null;
        }
    }

    public void addRow(Decimal pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value) {
        m_rows.add(new Row(pos, dailyPnL, unrealizedPnL, realizedPnL, value));

        fireTableDataChanged();
    }

}
