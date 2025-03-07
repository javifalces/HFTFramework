/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.ib.client.ScannerSubscription;
import com.ib.client.TagValue;

public class ScannerDlg extends JDialog {
    public static final int NO_SELECTION = 0;
    public static final int SUBSCRIBE_SELECTION = 1;
    public static final int CANCEL_SELECTION = 2;
    public static final int REQUEST_PARAMETERS_SELECTION = 3;

    public int m_userSelection = NO_SELECTION;
    public int m_id;
    public ScannerSubscription m_subscription = new ScannerSubscription();
    private List<TagValue> m_scannerSubscriptionOptions = new ArrayList<>();
    private List<TagValue> m_scannerFilterOptions = new ArrayList<>();

    private JTextField m_Id = new JTextField("0");
    private JTextField m_numberOfRows = new JTextField("10");
    private JTextField m_instrument = new JTextField("STK");
    private JTextField m_locationCode = new JTextField("STK.US.MAJOR");
    private JTextField m_scanCode = new JTextField("HIGH_OPT_VOLUME_PUT_CALL_RATIO");
    private JTextField m_abovePrice = new JTextField("3");
    private JTextField m_belowPrice = new JTextField();
    private JTextField m_aboveVolume = new JTextField("0");
    private JTextField m_averageOptionVolumeAbove = new JTextField("0");
    private JTextField m_marketCapAbove = new JTextField("100000000");
    private JTextField m_marketCapBelow = new JTextField();
    private JTextField m_moodyRatingAbove = new JTextField();
    private JTextField m_moodyRatingBelow = new JTextField();
    private JTextField m_spRatingAbove = new JTextField();
    private JTextField m_spRatingBelow = new JTextField();
    private JTextField m_maturityDateAbove = new JTextField();
    private JTextField m_maturityDateBelow = new JTextField();
    private JTextField m_couponRateAbove = new JTextField();
    private JTextField m_couponRateBelow = new JTextField();
    private JTextField m_excludeConvertible = new JTextField("0");
    private JTextField m_scannerSettingPairs = new JTextField("Annual,true");
    private JTextField m_stockTypeFilter = new JTextField("ALL");

    private JButton m_requestParameters = new JButton("Request Parameters");
    private JButton m_subscribe = new JButton("Subscribe");
    private JButton m_cancel = new JButton("Cancel Subscription");
    private JButton m_options = new JButton("Options");
    private JButton m_filterOptions = new JButton("Filter");

    private static final int COL1_WIDTH = 30;
    private static final int COL2_WIDTH = 100 - COL1_WIDTH;

    List<TagValue> scannerSubscriptionOptions() {
        return m_scannerSubscriptionOptions;
    }

    List<TagValue> scannerFilterOptions() {
        return m_scannerFilterOptions;
    }

    private static void addGBComponent(IBGridBagPanel panel, Component comp,
                                       GridBagConstraints gbc, int weightx, int gridwidth) {
        gbc.weightx = weightx;
        gbc.gridwidth = gridwidth;
        panel.setConstraints(comp, gbc);
        panel.add(comp, gbc);
    }

    public ScannerDlg(SampleFrame owner) {
        super(owner, true);

        setTitle("Sample");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weighty = 100;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridheight = 1;
        // create id panel
        IBGridBagPanel pId = new IBGridBagPanel();
        pId.setBorder(BorderFactory.createTitledBorder("Message Id"));

        addGBComponent(pId, new JLabel("Id"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pId, m_Id, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);

        // create contract panel
        IBGridBagPanel pSubscriptionDetails = new IBGridBagPanel();
        pSubscriptionDetails.setBorder(BorderFactory.createTitledBorder("Subscription Info"));
        addGBComponent(pSubscriptionDetails, new JLabel("Number of Rows"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_numberOfRows, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Instrument"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_instrument, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Location Code"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_locationCode, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Scan Code"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_scanCode, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Above Price"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_abovePrice, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Below Price"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_belowPrice, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Above Volume"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_aboveVolume, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Avg Option Volume Above"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_averageOptionVolumeAbove, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Market Cap Above"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_marketCapAbove, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Market Cap Below"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_marketCapBelow, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Moody Rating Above"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_moodyRatingAbove, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Moody Rating Below"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_moodyRatingBelow, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("S & P Rating Above"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_spRatingAbove, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("S & P Rating Below"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_spRatingBelow, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Maturity Date Above"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_maturityDateAbove, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Maturity Date Below"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_maturityDateBelow, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Coupon Rate Above"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_couponRateAbove, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Coupon Rate Below"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_couponRateBelow, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Exclude Convertible"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_excludeConvertible, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Scanner Setting Pairs"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_scannerSettingPairs, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        addGBComponent(pSubscriptionDetails, new JLabel("Stock Type Filter"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        addGBComponent(pSubscriptionDetails, m_stockTypeFilter, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);

        // create button panel
        JPanel buttonPanel = new JPanel();

        buttonPanel.add(m_requestParameters);
        buttonPanel.add(m_subscribe);
        buttonPanel.add(m_cancel);
        buttonPanel.add(m_options);
        buttonPanel.add(m_filterOptions);

        m_requestParameters.addActionListener(e -> onRequestParameters());
        m_subscribe.addActionListener(e -> onSubscribe());
        m_cancel.addActionListener(e -> onCancelSubscription());
        m_options.addActionListener(e -> onOptions());
        m_filterOptions.addActionListener(e -> onFilter());

        // create top panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(pId);
        topPanel.add(pSubscriptionDetails);

        // create dlg box
        getContentPane().add(topPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
    }

    private void onFilter() {
        SmartComboRoutingParamsDlg smartComboRoutingParamsDlg = new SmartComboRoutingParamsDlg("Scanner Subscription Filter Options", m_scannerFilterOptions, this);

        // show smart combo routing params dialog
        smartComboRoutingParamsDlg.setVisible(true);

        m_scannerFilterOptions = smartComboRoutingParamsDlg.smartComboRoutingParams();
    }

    private static double parseDouble(JTextField textfield) {
        try {
            return Double.parseDouble(textfield.getText().trim());
        } catch (Exception ex) {
            return Double.MAX_VALUE;
        }
    }

    private static int parseInt(JTextField textfield) {
        try {
            return Integer.parseInt(textfield.getText().trim());
        } catch (Exception ex) {
            return Integer.MAX_VALUE;
        }
    }

    private void onSubscribe() {
        m_userSelection = NO_SELECTION;

        try {
            // set id
            m_id = Integer.parseInt(m_Id.getText().trim());
            m_subscription.numberOfRows(parseInt(m_numberOfRows));
            m_subscription.instrument(m_instrument.getText().trim());
            m_subscription.locationCode(m_locationCode.getText().trim());
            m_subscription.scanCode(m_scanCode.getText().trim());
            m_subscription.abovePrice(parseDouble(m_abovePrice));
            m_subscription.belowPrice(parseDouble(m_belowPrice));
            m_subscription.aboveVolume(parseInt(m_aboveVolume));
            int avgOptVolume = parseInt(m_averageOptionVolumeAbove);
            // with Integer.MAX_VALUE creates filter in TWS
            m_subscription.averageOptionVolumeAbove(avgOptVolume != Integer.MAX_VALUE ? avgOptVolume : Integer.MIN_VALUE);
            m_subscription.marketCapAbove(parseDouble(m_marketCapAbove));
            m_subscription.marketCapBelow(parseDouble(m_marketCapBelow));
            m_subscription.moodyRatingAbove(m_moodyRatingAbove.getText().trim());
            m_subscription.moodyRatingBelow(m_moodyRatingBelow.getText().trim());
            m_subscription.spRatingAbove(m_spRatingAbove.getText().trim());
            m_subscription.spRatingBelow(m_spRatingBelow.getText().trim());
            m_subscription.maturityDateAbove(m_maturityDateAbove.getText().trim());
            m_subscription.maturityDateBelow(m_maturityDateBelow.getText().trim());
            m_subscription.couponRateAbove(parseDouble(m_couponRateAbove));
            m_subscription.couponRateBelow(parseDouble(m_couponRateBelow));
            m_subscription.excludeConvertible(Boolean.parseBoolean(m_excludeConvertible.getText().trim()));
            m_subscription.scannerSettingPairs(m_scannerSettingPairs.getText().trim());
            //           m_subscription.stockTypeFilter(m_stockTypeFilter.getText().trim()); Peter ???
        } catch (Exception e) {
            Main.inform(this, "Error - " + e);
            return;
        }

        m_userSelection = SUBSCRIBE_SELECTION;
        setVisible(false);
    }

    private void onRequestParameters() {
        m_userSelection = REQUEST_PARAMETERS_SELECTION;
        setVisible(false);
    }

    private void onCancelSubscription() {
        m_userSelection = CANCEL_SELECTION;
        m_id = Integer.parseInt(m_Id.getText().trim());
        setVisible(false);
    }

    private void onOptions() {
        SmartComboRoutingParamsDlg smartComboRoutingParamsDlg = new SmartComboRoutingParamsDlg("Scanner Subscription Options", m_scannerSubscriptionOptions, this);

        // show smart combo routing params dialog
        smartComboRoutingParamsDlg.setVisible(true);

        m_scannerSubscriptionOptions = smartComboRoutingParamsDlg.smartComboRoutingParams();
    }

    @Override
    public void setVisible(final boolean b) {
        if (b) {
            m_userSelection = NO_SELECTION;
        }
        super.setVisible(b);
    }
}
