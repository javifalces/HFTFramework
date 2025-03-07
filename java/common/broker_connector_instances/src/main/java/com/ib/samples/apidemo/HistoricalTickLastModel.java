/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.ib.client.HistoricalTickLast;

class HistoricalTickLastModel extends AbstractTableModel {

    private List<HistoricalTickLast> m_rows;

    public HistoricalTickLastModel(List<HistoricalTickLast> rows) {
        m_rows = rows;
    }

    @Override
    public int getRowCount() {
        return m_rows.size();
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        HistoricalTickLast row = m_rows.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return row.time();
            case 1:
                return row.price();
            case 2:
                return row.size();
            case 3:
                return row.exchange();
            case 4:
                return row.specialConditions();
            case 5:
                return row.tickAttribLast().toString();
        }
        return null;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Time";
            case 1:
                return "Price";
            case 2:
                return "Size";
            case 3:
                return "Exchange";
            case 4:
                return "Special conditions";
            case 5:
                return "Last Tick Attribs";
        }
        return super.getColumnName(column);
    }

}
