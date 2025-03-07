/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import static com.ib.controller.Formats.fmtNz;
import static com.ib.controller.Formats.fmtPct;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.MarketDataType;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.client.Types.Right;
import com.ib.client.Types.SecType;
import com.ib.client.Util;
import com.ib.controller.ApiController.IContractDetailsHandler;
import com.ib.controller.ApiController.IOptHandler;
import com.ib.controller.ApiController.TopMktDataAdapter;

import com.ib.samples.apidemo.util.HtmlButton;
import com.ib.samples.apidemo.util.NewTabbedPanel;
import com.ib.samples.apidemo.util.NewTabbedPanel.NewTabPanel;
import com.ib.samples.apidemo.util.TCombo;
import com.ib.samples.apidemo.util.UpperField;
import com.ib.samples.apidemo.util.VerticalPanel;

class OptionChainsPanel extends JPanel {
    private Contract m_underContract = new Contract();
    private NewTabbedPanel m_tabbedPanel = new NewTabbedPanel();
    private JTextField m_optExch = new UpperField();
    private UpperField m_symbol = new UpperField();
    private TCombo<SecType> m_secType = new TCombo<>(SecType.values());
    private UpperField m_lastTradeDateOrContractMonth = new UpperField();
    private UpperField m_exchange = new UpperField();
    private UpperField m_currency = new UpperField();
    private JCheckBox m_snapshot = new JCheckBox();
    private TCombo<String> m_marketDataType = new TCombo<>(MarketDataType.getFields());

    OptionChainsPanel() {
        m_symbol.setText("IBKR");
        m_secType.setSelectedItem(SecType.STK);
        m_exchange.setText("SMART");
        m_lastTradeDateOrContractMonth.setText("20170616");
        m_currency.setText("USD");
        m_optExch.setText("SMART");
        m_marketDataType.setSelectedItem(MarketDataType.REALTIME);

        HtmlButton button = new HtmlButton("Go") {
            @Override
            protected void actionPerformed() {
                onAdd();
            }
        };

        m_marketDataType.addActionListener(event ->
                ApiDemo.INSTANCE.controller().reqMktDataType(MarketDataType.getField(m_marketDataType.getSelectedItem())));

        VerticalPanel topPanel = new VerticalPanel();
        topPanel.add("Symbol", m_symbol);
        topPanel.add("Currency", m_currency);
        topPanel.add("Underlying sec type", m_secType);
        topPanel.add("Underlying exchange", m_exchange, Box.createHorizontalStrut(30), button);
        topPanel.add("Last trade date or contract month", m_lastTradeDateOrContractMonth);
        topPanel.add("Option exchange", m_optExch);
        topPanel.add("Use snapshot data", m_snapshot);
        topPanel.add("Market data type", m_marketDataType);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(m_tabbedPanel);
    }

    void onAdd() {
        m_underContract.symbol(m_symbol.getText().toUpperCase());
        m_underContract.secType(m_secType.getSelectedItem());
        m_underContract.exchange(m_exchange.getText().toUpperCase());
        m_underContract.currency(m_currency.getText().toUpperCase());

        ApiDemo.INSTANCE.controller().reqContractDetails(m_underContract, this::onRecUnderDetails);
    }

    void onRecUnderDetails(List<ContractDetails> list) {
        if (list.size() != 1) {
            ApiDemo.INSTANCE.show("Error: " + list.size() + " underlying contracts returned");
            return;
        }

        // request option chains
        Contract optContract = new Contract();
        optContract.symbol(m_underContract.symbol());
        optContract.lastTradeDateOrContractMonth(m_lastTradeDateOrContractMonth.getText());
        optContract.currency(m_underContract.currency());
        optContract.exchange(m_optExch.getText());
        optContract.secType(SecType.OPT);

        final ChainPanel symbolPanel = new ChainPanel();
        m_tabbedPanel.addTab(optContract.symbol(), symbolPanel, true, true);

        ApiDemo.INSTANCE.controller().reqContractDetails(optContract, symbolPanel);
    }

    private class ChainPanel extends NewTabPanel implements IContractDetailsHandler, ActionListener {
        ChainModel m_putsModel = new ChainModel();
        JTable m_putsTable = new JTable(m_putsModel);
        ChainModel m_callsModel = new ChainModel();
        JTable m_callsTable = new JTable(m_callsModel);
        Timer m_timer = new Timer(800, this);
        JLabel m_labUnderPrice = new JLabel();
        transient TopMktDataAdapter m_stockListener = new TopMktDataAdapter() {
            @Override
            public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
                if (tickType == TickType.LAST || tickType == TickType.DELAYED_LAST) {
                    m_labUnderPrice.setText("" + price);
                }
            }
        };

        ChainPanel() {
            JScrollPane scrollPuts = new JScrollPane(m_putsTable);
            scrollPuts.setBorder(new TitledBorder("Puts"));

            JScrollPane scrollCalls = new JScrollPane(m_callsTable);
            scrollCalls.setBorder(new TitledBorder("Calls"));

            VerticalPanel underPanel = new VerticalPanel();
            underPanel.add("Underlying price", m_labUnderPrice);

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
            mainPanel.add(scrollCalls);
            mainPanel.add(scrollPuts);

            setLayout(new BorderLayout());
            add(underPanel, BorderLayout.NORTH);
            add(mainPanel);

            m_timer.start();

            ApiDemo.INSTANCE.controller().reqTopMktData(m_underContract, "", false, false, m_stockListener);
        }

        /**
         * Called when the tab is first visited.
         */
        @Override
        public void activated() {
        }

        /**
         * Called when the tab is closed by clicking the X.
         */
        @Override
        public void closed() {
            ApiDemo.INSTANCE.controller().cancelTopMktData(m_stockListener);
            m_putsModel.desubscribe();
            m_callsModel.desubscribe();
            m_timer.stop();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            m_putsModel.fireTableDataChanged();
            m_callsModel.fireTableDataChanged();
        }

        @Override
        public void contractDetails(List<ContractDetails> list) {
            for (ContractDetails data : list) {
                Contract contract = data.contract();

                if (contract.right() == Right.Put) {
                    m_putsModel.addRow(contract, m_snapshot.isSelected());
                } else {
                    m_callsModel.addRow(contract, m_snapshot.isSelected());
                }
            }
            m_putsModel.sort();
            m_callsModel.sort();
        }

        private class ChainModel extends AbstractTableModel {
            Comparator<ChainRow> c = (o1, o2) -> {
                int rc = o1.m_c.lastTradeDateOrContractMonth().compareTo(o2.m_c.lastTradeDateOrContractMonth());
                if (rc == 0) {
                    rc = Double.compare(o1.m_c.strike(), o2.m_c.strike());
                }
                return rc;
            };

            List<ChainRow> m_list = new ArrayList<>();

            void desubscribe() {
                for (ChainRow row : m_list) {
                    ApiDemo.INSTANCE.controller().cancelOptionMktData(row);
                }
            }

            @Override
            public int getRowCount() {
                return m_list.size();
            }

            void sort() {
                m_list.sort(c);
                fireTableDataChanged();
            }

            void addRow(Contract contract, boolean snapshot) {
                ChainRow row = new ChainRow(contract);
                m_list.add(row);

                ApiDemo.INSTANCE.controller().reqOptionMktData(contract, "", snapshot, false, row);

                if (snapshot) {
                    com.ib.samples.apidemo.util.Util.sleep(11); // try to avoid pacing violation at TWS
                }
            }

            @Override
            public int getColumnCount() {
                return m_snapshot.isSelected() ? 11 : 10;
            }

            @Override
            public String getColumnName(int col) {
                switch (col) {
                    case 0:
                        return "Last trade date";
                    case 1:
                        return "Strike";
                    case 2:
                        return "Bid";
                    case 3:
                        return "Ask";
                    case 4:
                        return "TickAttrib";
                    case 5:
                        return "Imp Vol";
                    case 6:
                        return "Delta";
                    case 7:
                        return "Gamma";
                    case 8:
                        return "Vega";
                    case 9:
                        return "Theta";
                    default:
                        return null;
                }
            }

            @Override
            public Object getValueAt(int rowIn, int col) {
                ChainRow row = m_list.get(rowIn);
                switch (col) {
                    case 0:
                        return row.m_c.lastTradeDateOrContractMonth();
                    case 1:
                        return Util.DoubleMaxString(row.m_c.strike());
                    case 2:
                        return fmtNz(row.m_bid);
                    case 3:
                        return fmtNz(row.m_ask);
                    case 4:
                        return Util.IntMaxString(row.m_tickAttrib);
                    case 5:
                        return fmtPct(row.m_impVol);
                    case 6:
                        return fmtNz(row.m_delta);
                    case 7:
                        return fmtNz(row.m_gamma);
                    case 8:
                        return fmtNz(row.m_vega);
                    case 9:
                        return fmtNz(row.m_theta);
                    case 10:
                        return row.m_done ? "*" : null;
                    default:
                        return null;
                }
            }

            private class ChainRow extends TopMktDataAdapter implements IOptHandler {
                Contract m_c;
                double m_bid;
                double m_ask;
                int m_tickAttrib;
                double m_impVol;
                double m_delta;
                double m_gamma;
                double m_vega;
                double m_theta;
                boolean m_done;

                ChainRow(Contract contract) {
                    m_c = contract;
                }

                @Override
                public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
                    switch (tickType) {
                        case BID:
                        case DELAYED_BID:
                            m_bid = price;
                            break;
                        case ASK:
                        case DELAYED_ASK:
                            m_ask = price;
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void tickOptionComputation(TickType tickType, int tickAttrib, double impVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {
                    if (tickType == TickType.MODEL_OPTION || tickType == TickType.DELAYED_MODEL_OPTION) {
                        m_tickAttrib = tickAttrib;
                        m_impVol = impVol;
                        m_delta = delta;
                        m_gamma = gamma;
                        m_vega = vega;
                        m_theta = theta;
                    }
                }

                @Override
                public void tickSnapshotEnd() {
                    m_done = true;
                }
            }
        }
    }
}
