/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.GridBagConstraints;
import java.awt.Window;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class HistoricalDataDlg extends JDialogBox {
    private IBGridBagPanel m_panel = new IBGridBagPanel();
    private JTextField m_StartTime = new JTextField(22);
    private JTextField m_BackfillEndTime = new JTextField(22);
    private JTextField m_BackfillDuration = new JTextField("1 M");
    private JTextField m_BarSizeSetting = new JTextField("1 day");
    private JCheckBox m_UseRTH = new JCheckBox();
    private JTextField m_FormatDate = new JTextField("1");
    private JTextField m_WhatToShow = new JTextField("TRADES");
    private JCheckBox m_keepUpToDateCheckBox = new JCheckBox();
    private JCheckBox m_ignoreSize = new JCheckBox();
    private JTextField m_numberOfTicks = new JTextField("0");
    private JComboBox m_tickByTickTypeComboBox = new JComboBox(new Object[]{"Last", "AllLast", "BidAsk", "MidPoint"});

    public String startTime() {
        return m_StartTime.getText();
    }

    public String backfillEndTime() {
        return m_BackfillEndTime.getText();
    }

    public String backfillDuration() {
        return m_BackfillDuration.getText();
    }

    public String barSizeSetting() {
        return m_BarSizeSetting.getText();
    }

    public boolean useRTH() {
        return m_UseRTH.isSelected();
    }

    public int formatDate() {
        return Integer.parseInt(m_FormatDate.getText());
    }

    public String whatToshow() {
        return m_WhatToShow.getText();
    }

    public boolean keepUpToDate() {
        return m_keepUpToDateCheckBox.isSelected();
    }

    public boolean ignoreSize() {
        return m_ignoreSize.isSelected();
    }

    public int numberOfTicks() {
        return Integer.parseInt(m_numberOfTicks.getText());
    }

    public String tickByTickType() {
        return m_tickByTickTypeComboBox.getSelectedItem().toString();
    }

    private static final int COL1_WIDTH = 30;
    private static final int COL2_WIDTH = 100 - COL1_WIDTH;

    public HistoricalDataDlg(Window parent) {
        super(parent);

        m_panel.setBorder(BorderFactory.createTitledBorder("Historical Data Query"));

        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
        String dateTime = "" +
                gc.get(Calendar.YEAR) +
                pad(gc.get(Calendar.MONTH) + 1) +
                pad(gc.get(Calendar.DAY_OF_MONTH)) + " " +
                pad(gc.get(Calendar.HOUR_OF_DAY)) + ":" +
                pad(gc.get(Calendar.MINUTE)) + ":" +
                pad(gc.get(Calendar.SECOND)) + " " +
                gc.getTimeZone().getID();

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weighty = 100;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridheight = 1;

        m_BackfillEndTime.setText(dateTime);
        m_panel.addGBComponent(new JLabel("Start Date/Time"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        m_panel.addGBComponent(m_StartTime, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        m_panel.addGBComponent(new JLabel("End Date/Time"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        m_panel.addGBComponent(m_BackfillEndTime, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        m_panel.addGBComponent(new JLabel("Number of ticks"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        m_panel.addGBComponent(m_numberOfTicks, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        m_panel.addGBComponent(new JLabel("Duration"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        m_panel.addGBComponent(m_BackfillDuration, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        m_panel.addGBComponent(new JLabel("Bar Size Setting (1 to 11)"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        m_panel.addGBComponent(m_BarSizeSetting, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        m_panel.addGBComponent(new JLabel("What to Show"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        m_panel.addGBComponent(m_WhatToShow, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        m_panel.addGBComponent(new JLabel("Regular Trading Hours (1 or 0)"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        m_panel.addGBComponent(m_UseRTH, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        m_panel.addGBComponent(new JLabel("Date Format Style (1 or 2)"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        m_panel.addGBComponent(m_FormatDate, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        m_panel.addGBComponent(new JLabel("Keep up to date"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        m_panel.addGBComponent(m_keepUpToDateCheckBox, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        m_panel.addGBComponent(new JLabel("Ignore size"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        m_panel.addGBComponent(m_ignoreSize, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        m_panel.addGBComponent(new JLabel("Tick-By-Tick Type"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        m_panel.addGBComponent(m_tickByTickTypeComboBox, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);

        getContentPane().add(m_panel);
        pack();
    }

    private static String pad(int val) {
        return val < 10 ? "0" + val : "" + val;
    }

}
