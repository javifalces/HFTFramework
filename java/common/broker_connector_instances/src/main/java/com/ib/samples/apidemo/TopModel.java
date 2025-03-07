/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import static com.ib.controller.Formats.fmt;
import static com.ib.controller.Formats.fmt8;
import static com.ib.controller.Formats.fmtPct;
import static com.ib.controller.Formats.fmtTime;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.MarketDataType;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.client.Util;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.ib.controller.Formats;

class TopModel extends AbstractTableModel {
    private List<TopRow> m_rows = new ArrayList<>();
    private MarketDataPanel m_parentPanel;
    private static final int CANCEL_CHBX_COL_INDEX = 28;
    String m_genericTicks = "";

    TopModel(MarketDataPanel parentPanel) {
        m_parentPanel = parentPanel;
    }

    void setGenericTicks(String genericTicks) {
        m_genericTicks = genericTicks;
    }

    void addRow(Contract contract) {
        TopRow row = new TopRow(this, contract.textDescription(), m_parentPanel);
        m_rows.add(row);
        ApiDemo.INSTANCE.controller().reqTopMktData(contract, m_genericTicks, false, false, row);
        fireTableRowsInserted(m_rows.size() - 1, m_rows.size() - 1);
    }

    void addRow(TopRow row) {
        m_rows.add(row);
        fireTableRowsInserted(m_rows.size() - 1, m_rows.size() - 1);
    }

    void removeSelectedRows() {
        for (int rowIndex = m_rows.size() - 1; rowIndex >= 0; rowIndex--) {
            if (m_rows.get(rowIndex).m_cancel) {
                ApiDemo.INSTANCE.controller().cancelTopMktData(m_rows.get(rowIndex));
                m_rows.remove(rowIndex);
            }
        }
        fireTableDataChanged();
    }

    void desubscribe() {
        for (TopRow row : m_rows) {
            ApiDemo.INSTANCE.controller().cancelTopMktData(row);
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case CANCEL_CHBX_COL_INDEX:
                return Boolean.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == CANCEL_CHBX_COL_INDEX;
    }

    @Override
    public int getRowCount() {
        return m_rows.size();
    }

    @Override
    public int getColumnCount() {
        return 29;
    }

    @Override
    public String getColumnName(int col) {
        switch (col) {
            case 0:
                return "Description";
            case 1:
                return "Bid Size";
            case 2:
                return "Bid";
            case 3:
                return "Bid Mask";
            case 4:
                return "Bid Can Auto Execute";
            case 5:
                return "Bid Past Limit";
            case 6:
                return "Pre Open Bid";
            case 7:
                return "Ask";
            case 8:
                return "Ask Size";
            case 9:
                return "Ask Mask";
            case 10:
                return "Ask Can Auto Execute";
            case 11:
                return "Ask Past Limit";
            case 12:
                return "Pre Open Ask";
            case 13:
                return "Last";
            case 14:
                return "Time";
            case 15:
                return "Change";
            case 16:
                return "Volume";
            case 17:
                return "Min Tick";
            case 18:
                return "BBO Exchange";
            case 19:
                return "Snapshot Permissions";
            case 20:
                return "Close";
            case 21:
                return "Open";
            case 22:
                return "Market Data Type";
            case 23:
                return "Futures Open Interest";
            case 24:
                return "Avg Opt Volume";
            case 25:
                return "Shortable Shares";
            case 26:
                return "Estimated IPO Midpoint";
            case 27:
                return "Final IPO Last";
            case CANCEL_CHBX_COL_INDEX:
                return "Cancel";

            default:
                return null;
        }
    }

    @Override
    public Object getValueAt(int rowIn, int col) {
        TopRow row = m_rows.get(rowIn);
        switch (col) {
            case 0:
                return row.m_description;
            case 1:
                return row.m_bidSize;
            case 2:
                return fmt(row.m_bid);
            case 3:
                return Util.IntMaxString(row.m_bidMask);
            case 4:
                return row.m_bidCanAutoExecute;
            case 5:
                return row.m_bidPastLimit;
            case 6:
                return row.m_preOpenBid;
            case 7:
                return fmt(row.m_ask);
            case 8:
                return row.m_askSize;
            case 9:
                return Util.IntMaxString(row.m_askMask);
            case 10:
                return row.m_askCanAutoExecute;
            case 11:
                return row.m_askPastLimit;
            case 12:
                return row.m_preOpenAsk;
            case 13:
                return fmt(row.m_last);
            case 14:
                return fmtTime(row.m_lastTime);
            case 15:
                return row.change();
            case 16:
                return row.m_volume;
            case 17:
                return fmt8(row.m_minTick);
            case 18:
                return row.m_bboExch;
            case 19:
                return Util.IntMaxString(row.m_snapshotPermissions);
            case 20:
                return fmt(row.m_close);
            case 21:
                return fmt(row.m_open);
            case 22:
                return row.m_marketDataType;
            case 23:
                return row.m_futuresOpenInterest;
            case 24:
                return row.m_avgOptVolume;
            case 25:
                return row.m_shortableShares;
            case 26:
                return row.m_estimatedIPOMidpoint;
            case 27:
                return row.m_finalIPOLast;

            case CANCEL_CHBX_COL_INDEX:
                return row.m_cancel;
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIn, int col) {
        TopRow row = m_rows.get(rowIn);
        switch (col) {
            case CANCEL_CHBX_COL_INDEX:
                row.m_cancel = (Boolean) aValue;
                break;
            default:
                break;
        }
    }

    public void cancel(int i) {
        ApiDemo.INSTANCE.controller().cancelTopMktData(m_rows.get(i));
    }

    static class TopRow extends TopMktDataAdapter {
        AbstractTableModel m_model;
        MarketDataPanel m_parentPanel;
        String m_description;
        double m_bid;
        double m_ask;
        double m_last;
        long m_lastTime;
        Decimal m_bidSize;
        Decimal m_askSize;
        double m_close;
        Decimal m_volume;
        double m_open;
        boolean m_cancel;
        String m_marketDataType = MarketDataType.getField(MarketDataType.REALTIME);
        boolean m_frozen;
        boolean m_bidCanAutoExecute, m_askCanAutoExecute;
        boolean m_bidPastLimit, m_askPastLimit;
        boolean m_preOpenBid, m_preOpenAsk;
        double m_minTick;
        String m_bboExch;
        int m_snapshotPermissions;
        int m_bidMask, m_askMask;
        Decimal m_futuresOpenInterest;
        Decimal m_avgOptVolume;
        Decimal m_shortableShares;
        double m_estimatedIPOMidpoint;
        double m_finalIPOLast;

        TopRow(AbstractTableModel model, String description, MarketDataPanel parentPanel) {
            m_model = model;
            m_description = description;
            m_parentPanel = parentPanel;
        }

        public String change() {
            return m_close == 0 ? null : fmtPct((m_last - m_close) / m_close);
        }

        @Override
        public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
            if (m_marketDataType.equalsIgnoreCase(MarketDataType.getField(MarketDataType.REALTIME)) &&
                    (tickType == TickType.DELAYED_BID ||
                            tickType == TickType.DELAYED_ASK ||
                            tickType == TickType.DELAYED_LAST ||
                            tickType == TickType.DELAYED_CLOSE ||
                            tickType == TickType.DELAYED_OPEN)) {
                m_marketDataType = MarketDataType.getField(MarketDataType.DELAYED);
            }

            switch (tickType) {
                case BID:
                case DELAYED_BID:
                    m_bid = price;
                    m_bidCanAutoExecute = attribs.canAutoExecute();
                    m_bidPastLimit = attribs.pastLimit();
                    m_preOpenBid = attribs.preOpen();
                    break;
                case ASK:
                case DELAYED_ASK:
                    m_ask = price;
                    m_askCanAutoExecute = attribs.canAutoExecute();
                    m_askPastLimit = attribs.pastLimit();
                    m_preOpenAsk = attribs.preOpen();
                    break;
                case LAST:
                case DELAYED_LAST:
                    m_last = price;
                    break;
                case CLOSE:
                case DELAYED_CLOSE:
                    m_close = price;
                    break;
                case OPEN:
                case DELAYED_OPEN:
                    m_open = price;
                    break;
                case ESTIMATED_IPO_MIDPOINT:
                    m_estimatedIPOMidpoint = price;
                    break;
                case FINAL_IPO_LAST:
                    m_finalIPOLast = price;
                    break;
                default:
                    break;
            }
            m_model.fireTableDataChanged(); // should use a timer to be more efficient
        }

        @Override
        public void tickSize(TickType tickType, Decimal size) {
            if (m_marketDataType.equalsIgnoreCase(MarketDataType.getField(MarketDataType.REALTIME)) &&
                    (tickType == TickType.DELAYED_BID_SIZE ||
                            tickType == TickType.DELAYED_ASK_SIZE ||
                            tickType == TickType.DELAYED_VOLUME)) {
                m_marketDataType = MarketDataType.getField(MarketDataType.DELAYED);
            }

            switch (tickType) {
                case BID_SIZE:
                case DELAYED_BID_SIZE:
                    m_bidSize = size;
                    break;
                case ASK_SIZE:
                case DELAYED_ASK_SIZE:
                    m_askSize = size;
                    break;
                case VOLUME:
                case DELAYED_VOLUME:
                    m_volume = size;
                    break;
                case FUTURES_OPEN_INTEREST:
                    m_futuresOpenInterest = size;
                    break;
                case AVG_OPT_VOLUME:
                    m_avgOptVolume = size;
                    break;
                case SHORTABLE_SHARES:
                    m_shortableShares = size;
                    break;
                default:
                    break;
            }
            m_model.fireTableDataChanged();
        }

        @Override
        public void tickString(TickType tickType, String value) {
            switch (tickType) {
                case LAST_TIMESTAMP:
                case DELAYED_LAST_TIMESTAMP:
                    m_lastTime = Long.parseLong(value) * 1000;
                    break;
                default:
                    break;
            }
        }

        @Override
        public void marketDataType(int marketDataType) {
            m_marketDataType = MarketDataType.getField(marketDataType);
            m_model.fireTableDataChanged();
        }

        @Override
        public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {
            m_minTick = minTick;
            m_bboExch = bboExchange;
            m_snapshotPermissions = snapshotPermissions;

            m_parentPanel.addBboExch(bboExchange);
            m_model.fireTableDataChanged();
        }
    }
}
