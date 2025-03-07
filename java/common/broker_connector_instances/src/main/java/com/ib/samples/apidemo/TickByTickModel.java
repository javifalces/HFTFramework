/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.ib.client.TickByTick;
import com.ib.client.Util;
import com.ib.client.Types.TickByTickType;

class TickByTickModel extends AbstractTableModel {

    private List<TickByTick> m_rows;
    private TickByTickType m_tickType;

    public TickByTickModel(List<TickByTick> rows, TickByTickType tickType) {
        m_rows = rows;
        m_tickType = tickType;
    }

    @Override
    public int getRowCount() {
        return m_rows.size();
    }

    @Override
    public int getColumnCount() {
        int columnCount = 0;
        switch (m_tickType) {
            case None:
                break;
            case Last:
            case AllLast:
                columnCount = 6;
                break;
            case BidAsk:
                columnCount = 6;
                break;
            case MidPoint:
                columnCount = 2;
                break;
        }
        return columnCount;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TickByTick row = m_rows.get(rowIndex);

        switch (m_tickType) {
            case None:
                break;
            case Last:
            case AllLast:
                switch (columnIndex) {
                    case 0:
                        return Util.UnixSecondsToString(row.time(), "yyyyMMdd-HH:mm:ss");
                    case 1:
                        return row.price();
                    case 2:
                        return row.size();
                    case 3:
                        return row.tickAttribLastStr();
                    case 4:
                        return row.exchange();
                    case 5:
                        return row.specialConditions();
                }
                break;
            case BidAsk:
                switch (columnIndex) {
                    case 0:
                        return Util.UnixSecondsToString(row.time(), "yyyyMMdd-HH:mm:ss");
                    case 1:
                        return row.bidPrice();
                    case 2:
                        return row.bidSize();
                    case 3:
                        return row.askPrice();
                    case 4:
                        return row.askSize();
                    case 5:
                        return row.tickAttribBidAskStr();
                }
                break;
            case MidPoint:
                switch (columnIndex) {
                    case 0:
                        return Util.UnixSecondsToString(row.time(), "yyyyMMdd-HH:mm:ss");
                    case 1:
                        return row.midPoint();
                }
                break;
        }

        return null;
    }

    @Override
    public String getColumnName(int column) {
        switch (m_tickType) {
            case None:
                break;
            case Last:
            case AllLast:
                switch (column) {
                    case 0:
                        return "Time";
                    case 1:
                        return "Price";
                    case 2:
                        return "Size";
                    case 3:
                        return "Last Tick Attribs";
                    case 4:
                        return "Exchange";
                    case 5:
                        return "Spec Cond";
                }
                break;
            case BidAsk:
                switch (column) {
                    case 0:
                        return "Time";
                    case 1:
                        return "Bid Price";
                    case 2:
                        return "Bid Size";
                    case 3:
                        return "Ask Price";
                    case 4:
                        return "Ask Size";
                    case 5:
                        return "Bid/Ask Tick Attribs";
                }
                break;
            case MidPoint:
                switch (column) {
                    case 0:
                        return "Time";
                    case 1:
                        return "Mid Point";
                }
                break;
        }

        return super.getColumnName(column);
    }

}
