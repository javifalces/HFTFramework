/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.ib.client.HistoricalTick;
import com.ib.client.HistoricalTickBidAsk;
import com.ib.client.HistoricalTickLast;
import com.ib.controller.ApiController.IHistoricalTickHandler;

import com.ib.samples.apidemo.util.NewTabbedPanel.NewTabPanel;
import com.ib.samples.apidemo.util.VerticalPanel.StackPanel;

class HistoricalTickResultsPanel extends NewTabPanel implements IHistoricalTickHandler {

    HistoricalTickResultsPanel() {
        m_table = new JTable(m_tickModel);
        JScrollPane scroll = new JScrollPane(m_table) {
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();

                d.width = 500;

                return d;
            }
        };

        setLayout(new GridLayout());
        StackPanel hTicksPanel = new StackPanel();

        hTicksPanel.add(new JLabel("Historical ticks:"));
        hTicksPanel.add(scroll, BorderLayout.WEST);
        add(hTicksPanel);
    }

    private final List<HistoricalTick> m_historicalTickRows = new ArrayList<>();
    private final HistoricalTickModel m_tickModel = new HistoricalTickModel(m_historicalTickRows);

    @Override
    public void historicalTick(int reqId, List<HistoricalTick> ticks) {
        for (HistoricalTick tick : ticks) {
            m_historicalTickRows.add(tick);
        }

        m_table.setModel(m_tickModel);
        m_tickModel.fireTableDataChanged();
    }

    private final List<HistoricalTickBidAsk> m_historicalTickBidAsk = new ArrayList<>();
    private final HistoricalTickBidAskModel m_tickBidAskModel = new HistoricalTickBidAskModel(m_historicalTickBidAsk);

    @Override
    public void historicalTickBidAsk(int reqId, List<HistoricalTickBidAsk> ticks) {
        for (HistoricalTickBidAsk tick : ticks) {
            m_historicalTickBidAsk.add(tick);
        }

        m_table.setModel(m_tickBidAskModel);
        m_tickBidAskModel.fireTableDataChanged();
    }

    private final List<HistoricalTickLast> m_historicalTickLast = new ArrayList<>();
    private final HistoricalTickLastModel m_tickLastModel = new HistoricalTickLastModel(m_historicalTickLast);
    private JTable m_table;

    @Override
    public void historicalTickLast(int reqId, List<HistoricalTickLast> ticks) {
        for (HistoricalTickLast tick : ticks) {
            m_historicalTickLast.add(tick);
        }

        m_table.setModel(m_tickLastModel);
        m_tickLastModel.fireTableDataChanged();
    }

    @Override
    public void activated() {
    }

    @Override
    public void closed() {
    }

}