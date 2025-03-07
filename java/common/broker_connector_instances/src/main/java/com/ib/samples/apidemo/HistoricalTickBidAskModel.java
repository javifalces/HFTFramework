/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.ib.client.HistoricalTickBidAsk;

class HistoricalTickBidAskModel extends AbstractTableModel {

    private List<HistoricalTickBidAsk> m_rows;

    public HistoricalTickBidAskModel(List<HistoricalTickBidAsk> rows) {
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
        HistoricalTickBidAsk row = m_rows.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return row.time();
            case 1:
                return row.priceBid();
            case 2:
                return row.priceAsk();
            case 3:
                return row.sizeBid();
            case 4:
                return row.sizeAsk();
            case 5:
                return row.tickAttribBidAsk().toString();
        }

        return null;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Time";
            case 1:
                return "Price Bid";
            case 2:
                return "Price Ask";
            case 3:
                return "Size Bid";
            case 4:
                return "Size Ask";
            case 5:
                return "Bid/Ask Tick Attribs";
        }

        return super.getColumnName(column);
    }

}
