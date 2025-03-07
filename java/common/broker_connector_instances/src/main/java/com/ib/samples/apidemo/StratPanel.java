/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import com.ib.controller.ApiController.IRealTimeBarHandler;
import com.ib.controller.Bar;

import com.ib.samples.apidemo.OrdersPanel.OrdersModel;
import com.ib.samples.apidemo.util.HtmlButton;
import com.ib.samples.apidemo.util.TCombo;
import com.ib.samples.apidemo.util.UpperField;
import com.ib.samples.apidemo.util.VerticalPanel.BorderPanel;
import com.ib.samples.apidemo.util.VerticalPanel.HorzPanel;
import com.ib.samples.apidemo.util.VerticalPanel.StackPanel;


public class StratPanel extends StackPanel implements IHistoricalDataHandler, IRealTimeBarHandler {
    final private Contract m_contract = new Contract();
    final private ContractPanel m_contractPanel = new ContractPanel(m_contract);
    final private UpperField m_shares = new UpperField();
    final private UpperField m_pct1 = new UpperField();
    final private UpperField m_pct2 = new UpperField();
    final private OrdersModel m_ordersModel = new OrdersModel();
    final private TCombo<BarSize> m_barSize = new TCombo<>(BarSize.values());
    final private UpperField m_bars = new UpperField();
    final private List<Bar> m_rows = new ArrayList<>();
    final private Chart m_chart = new Chart(m_rows);
    private boolean m_req;

    private static Component sp(int n) {
        return Box.createHorizontalStrut(n);
    }

    StratPanel() {
        m_contractPanel.setBorder(new TitledBorder("Define Contract"));

        JPanel p1 = new HPanel();
        add(p1, "Go long", sp(5), m_shares, sp(5), "shares when ask goes above SMA by", sp(5), m_pct1, "%");

        JPanel p2 = new HPanel();
        add(p2, "Go flat when bid goes below SMA by", sp(5), m_pct2, "%");

        JPanel p3 = new HPanel();
        add(p3, "SMA bar size:", sp(5), m_barSize, sp(20), "SMA number of bars", sp(5), m_bars);

        HtmlButton start = new HtmlButton("Start") {
            @Override
            protected void actionPerformed() {
                onStart();
            }
        };

        HtmlButton stop = new HtmlButton("Stop") {
            @Override
            protected void actionPerformed() {
                onStop();
            }
        };

        JPanel buts = new JPanel();
        buts.add(start);
        buts.add(Box.createHorizontalStrut(30));
        buts.add(stop);

        StackPanel rightPanel = new StackPanel();
        rightPanel.setBorder(new TitledBorder("Define Strategy"));
        rightPanel.add(p1);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(p2);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(p3);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(buts);

        JScrollPane chartScroll = new JScrollPane(m_chart);
        m_chart.setBorder(new TitledBorder("chart"));
        chartScroll.setBorder(new TitledBorder("chart scroll"));

        HorzPanel horzPanel = new HorzPanel();
        horzPanel.add(m_contractPanel);
        horzPanel.add(rightPanel);

        BorderPanel topPanel = new BorderPanel();
        topPanel.add(horzPanel, BorderLayout.WEST);
        topPanel.add(chartScroll);

        JTable ordersTable = new JTable(m_ordersModel);
        JScrollPane ordersScroll = new JScrollPane(ordersTable);
        ordersScroll.setBorder(new TitledBorder("Orders"));

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(topPanel);
        add(ordersScroll);
        add(new TradesPanel());
    }

    protected void onStart() {
        m_contractPanel.onOK();
        ApiDemo.INSTANCE.controller().reqRealTimeBars(m_contract, WhatToShow.TRADES, false, this);
    }

    @Override
    public void realtimeBar(Bar bar) {
        if (!m_req) {
            BarSize barSize = m_barSize.getSelectedItem();
            QueryLength queryLength = getQueryLength(barSize);
            if (queryLength == null) return;
            String date = Bar.format(bar.time() * 1000);
            int duration = m_bars.getInt() * queryLength.m_units;
            ApiDemo.INSTANCE.controller().reqHistoricalData(m_contract, date, duration, queryLength.m_unit, barSize, WhatToShow.TRADES, false, false, this);
            m_req = true;
        }
        addBar(bar);
        m_chart.repaint();
    }

    private Map<Long, Bar> m_map = new TreeMap<>();

    @Override
    public void historicalData(Bar bar) {
        System.out.println(bar);
        addBar(bar);
    }

    private void addBar(Bar bar) {
        m_map.put(bar.time(), bar);
        m_rows.clear();
        m_rows.addAll(m_map.values());
    }

    @Override
    public void historicalDataEnd() {
        m_chart.repaint();
    }

    static class QueryLength {
        int m_units;
        DurationUnit m_unit;

        QueryLength(int units, DurationUnit unit) {
            m_units = units;
            m_unit = unit;
        }
    }

    protected void onStop() {
        ApiDemo.INSTANCE.controller().cancelRealtimeBars(this);
        ApiDemo.INSTANCE.controller().cancelHistoricalData(this);
    }

    void add(JPanel p, Object... objs) {
        for (Object obj : objs) {
            if (obj instanceof String) {
                p.add(new JLabel((String) obj));
            } else {
                p.add((Component) obj);
            }
        }
    }

    class HPanel extends HorzPanel {
        @Override
        public Dimension getMaximumSize() {
            return super.getPreferredSize();
        }
    }

    private static QueryLength getQueryLength(BarSize barSize) {
        switch (barSize) {
            case _1_secs:
                return new QueryLength(1, DurationUnit.SECOND);
            case _5_secs:
                return new QueryLength(5, DurationUnit.SECOND);
            case _10_secs:
                return new QueryLength(10, DurationUnit.SECOND);
            case _15_secs:
                return new QueryLength(15, DurationUnit.SECOND);
            case _30_secs:
                return new QueryLength(30, DurationUnit.SECOND);
            case _1_min:
                return new QueryLength(60, DurationUnit.SECOND);
            case _2_mins:
                return new QueryLength(120, DurationUnit.SECOND);
            case _3_mins:
                return new QueryLength(180, DurationUnit.SECOND);
            case _5_mins:
                return new QueryLength(300, DurationUnit.SECOND);
            case _10_mins:
                return new QueryLength(600, DurationUnit.SECOND);
            case _15_mins:
                return new QueryLength(900, DurationUnit.SECOND);
            case _20_mins:
                return new QueryLength(1200, DurationUnit.SECOND);
            case _30_mins:
                return new QueryLength(1800, DurationUnit.SECOND);
            case _1_hour:
                return new QueryLength(3600, DurationUnit.SECOND);
            case _4_hours:
                return new QueryLength(14400, DurationUnit.SECOND);
            case _1_day:
                return new QueryLength(1, DurationUnit.DAY);
            case _1_week:
                return new QueryLength(1, DurationUnit.WEEK);
            default:
                return null;
        }
    }
}
