/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.DeltaNeutralContract;
import com.ib.client.MarketDataType;
import com.ib.client.Order;
import com.ib.client.TagValue;
import com.ib.client.Types.UsePriceMgmtAlgo;

import com.ib.samples.apidemo.AdjustedPanel;
import com.ib.samples.apidemo.ConditionsPanel;
import com.ib.samples.apidemo.OnOKPanel;
import com.ib.samples.apidemo.PegBenchPanel;
import com.ib.samples.apidemo.util.TCombo;

public class OrderDlg extends JDialog {
    private static final String ALL_GENERIC_TICK_TAGS = "100,101,104,106,165,221,232,236,258,293,294,295,318,411,460,619";
    private static final int OPERATION_INSERT = 0;
    private static final int OPERATION_UPDATE = 1;
    private static final int OPERATION_DELETE = 2;

    private static final int SIDE_ASK = 0;
    private static final int SIDE_BID = 1;

    public boolean m_rc;
    private int m_id;
    public int m_marketDepthRows;
    private Contract m_contract = new Contract();
    public Order m_order = new Order();
    public DeltaNeutralContract m_deltaNeutralContract = new DeltaNeutralContract();
    public int m_exerciseAction;
    public int m_exerciseQuantity;
    public int m_override;
    public int m_marketDataType;
    private String m_optionsDlgTitle;
    private List<TagValue> m_options = new ArrayList<>();

    private JTextField m_Id = new JTextField("0");
    private JTextField m_conId = new JTextField();
    private JTextField m_symbol = new JTextField("SPY");
    private JTextField m_secType = new JTextField("STK");
    private JTextField m_lastTradeDateOrContractMonth = new JTextField();
    private JTextField m_strike = new JTextField("0");
    private JTextField m_right = new JTextField();
    private JTextField m_multiplier = new JTextField("");
    private JTextField m_exchange = new JTextField("SMART");
    private JTextField m_primaryExch = new JTextField("ARCA");
    private JTextField m_currency = new JTextField("USD");
    private JTextField m_localSymbol = new JTextField();
    private JTextField m_tradingClass = new JTextField();
    private JTextField m_includeExpired = new JTextField("0");
    private JTextField m_secIdType = new JTextField();
    private JTextField m_secId = new JTextField();
    private JTextField m_issuerId = new JTextField();
    private JTextField m_action = new JTextField("BUY");
    private JTextField m_totalQuantity = new JTextField("10");
    private JTextField m_orderType = new JTextField("LMT");
    private JTextField m_lmtPrice = new JTextField("40");
    private JTextField m_auxPrice = new JTextField("0");
    private JTextField m_goodAfterTime = new JTextField();
    private JTextField m_goodTillDate = new JTextField();
    private JTextField m_cashQty = new JTextField();
    private JTextField m_marketDepthRowTextField = new JTextField("20");
    private JCheckBox m_smartDepth = new JCheckBox("SMART Depth", true);
    private JTextField m_genericTicksTextField = new JTextField(ALL_GENERIC_TICK_TAGS);
    private JCheckBox m_snapshotMktDataTextField = new JCheckBox("Snapshot", false);
    private JCheckBox m_regSnapshotMktDataTextField = new JCheckBox("Regulatory Snapshot", false);
    private JTextField m_exerciseActionTextField = new JTextField("1");
    private JTextField m_exerciseQuantityTextField = new JTextField("1");
    private JTextField m_overrideTextField = new JTextField("0");
    private JComboBox<String> m_marketDataTypeCombo = new JComboBox<>(MarketDataType.getFields());
    private TCombo<UsePriceMgmtAlgo> m_usePriceMgmtAlgo = new TCombo<>(UsePriceMgmtAlgo.values());

    private JButton m_sharesAlloc = new JButton("FA Allocation Info...");
    private JButton m_comboLegs = new JButton("Combo Legs");
    private JButton m_btnDeltaNeutralContract = new JButton("Delta Neutral");
    private JButton m_btnAlgoParams = new JButton("Algo Params");
    private JButton m_btnSmartComboRoutingParams = new JButton("Smart Combo Routing Params");
    private JButton m_btnOptions = new JButton("Options");
    private JButton m_btnConditions = new JButton("Conditions");
    private JButton m_btnPeg2Bench = new JButton("Pegged to benchmark");
    private JButton m_btnAdjStop = new JButton("Adjustable stops");
    private JButton m_btnHistoricalData = new JButton("Historical Data Query");
    private HistoricalDataDlg m_historicalDataDlg = new HistoricalDataDlg(this);

    private JButton m_ok = new JButton("OK");
    private JButton m_cancel = new JButton("Cancel");
    private SampleFrame m_parent;

    private String m_faGroup;
    private String m_faProfile;
    private String m_faMethod;
    private String m_faPercentage;
    public String m_genericTicks;
    public boolean m_snapshotMktData;
    public boolean m_reqSnapshotMktData;

    private static final int COL1_WIDTH = 30;
    private static final int COL2_WIDTH = 100 - COL1_WIDTH;

    public void faGroup(String s) {
        m_faGroup = s;
    }

    public void faProfile(String s) {
        m_faProfile = s;
    }

    public void faMethod(String s) {
        m_faMethod = s;
    }

    public void faPercentage(String s) {
        m_faPercentage = s;
    }

    OrderDlg(SampleFrame owner) {
        super(owner, true);

        m_parent = owner;
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

        pId.addGBComponent(new JLabel("Id"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pId.addGBComponent(m_Id, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);

        // create contract panel
        IBGridBagPanel pContractDetails = new IBGridBagPanel();
        pContractDetails.setBorder(BorderFactory.createTitledBorder("Contract Info"));
        pContractDetails.addGBComponent(new JLabel("Contract Id"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_conId, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Symbol"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_symbol, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Security Type"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_secType, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Last trade date or contract month"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_lastTradeDateOrContractMonth, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Strike"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_strike, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Put/Call"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_right, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Option Multiplier"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_multiplier, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Exchange"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_exchange, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Primary Exchange"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_primaryExch, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Currency"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_currency, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Local Symbol"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_localSymbol, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Trading Class"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_tradingClass, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Include Expired"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_includeExpired, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Sec Id Type"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_secIdType, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Sec Id"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_secId, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pContractDetails.addGBComponent(new JLabel("Issuer Id"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pContractDetails.addGBComponent(m_issuerId, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);

        // create order panel
        IBGridBagPanel pOrderDetails = new IBGridBagPanel();
        pOrderDetails.setBorder(BorderFactory.createTitledBorder("Order Info"));
        pOrderDetails.addGBComponent(new JLabel("Action"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pOrderDetails.addGBComponent(m_action, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pOrderDetails.addGBComponent(new JLabel("Total Order Size"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pOrderDetails.addGBComponent(m_totalQuantity, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pOrderDetails.addGBComponent(new JLabel("Order Type"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pOrderDetails.addGBComponent(m_orderType, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pOrderDetails.addGBComponent(new JLabel("Lmt Price / Option Price / Stop Price / Volatility"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pOrderDetails.addGBComponent(m_lmtPrice, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pOrderDetails.addGBComponent(new JLabel("Aux Price / Underlying Price"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pOrderDetails.addGBComponent(m_auxPrice, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pOrderDetails.addGBComponent(new JLabel("Good After Time"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pOrderDetails.addGBComponent(m_goodAfterTime, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pOrderDetails.addGBComponent(new JLabel("Good Till Date"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pOrderDetails.addGBComponent(m_goodTillDate, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pOrderDetails.addGBComponent(new JLabel("Cash Quantity"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pOrderDetails.addGBComponent(m_cashQty, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pOrderDetails.addGBComponent(new JLabel("Use Price Management Algo"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pOrderDetails.addGBComponent(m_usePriceMgmtAlgo, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);

        // create marketDepth panel
        IBGridBagPanel pMarketDepth = new IBGridBagPanel();
        pMarketDepth.setBorder(BorderFactory.createTitledBorder("Market Depth"));
        pMarketDepth.addGBComponent(new JLabel("Number of Rows"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pMarketDepth.addGBComponent(m_marketDepthRowTextField, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pMarketDepth.addGBComponent(m_smartDepth, gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);

        // create marketData panel
        IBGridBagPanel pMarketData = new IBGridBagPanel();
        pMarketData.setBorder(BorderFactory.createTitledBorder("Market Data"));
        pMarketData.addGBComponent(new JLabel("Generic Tick Tags"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pMarketData.addGBComponent(m_genericTicksTextField, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pMarketData.addGBComponent(m_snapshotMktDataTextField, gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pMarketData.addGBComponent(m_regSnapshotMktDataTextField, gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);

        // create options exercise panel
        IBGridBagPanel pOptionsExercise = new IBGridBagPanel();
        pOptionsExercise.setBorder(BorderFactory.createTitledBorder("Options Exercise"));
        pOptionsExercise.addGBComponent(new JLabel("Action (1 or 2)"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pOptionsExercise.addGBComponent(m_exerciseActionTextField, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pOptionsExercise.addGBComponent(new JLabel("Number of Contracts"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pOptionsExercise.addGBComponent(m_exerciseQuantityTextField, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);
        pOptionsExercise.addGBComponent(new JLabel("Override (0 or 1)"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pOptionsExercise.addGBComponent(m_overrideTextField, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);


        // create marketDataType panel
        IBGridBagPanel pMarketDataType = new IBGridBagPanel();
        pMarketDataType.setBorder(BorderFactory.createTitledBorder("Market Data Type"));
        pMarketDataType.addGBComponent(new JLabel("Market Data Type"), gbc, COL1_WIDTH, GridBagConstraints.RELATIVE);
        pMarketDataType.addGBComponent(m_marketDataTypeCombo, gbc, COL2_WIDTH, GridBagConstraints.REMAINDER);

        // create mid Panel
        JPanel pMidPanel = new JPanel();
        pMidPanel.setLayout(new BoxLayout(pMidPanel, BoxLayout.Y_AXIS));
        pMidPanel.add(pContractDetails, BorderLayout.CENTER);
        pMidPanel.add(pOrderDetails, BorderLayout.CENTER);
        pMidPanel.add(pMarketDepth, BorderLayout.CENTER);
        pMidPanel.add(pMarketData, BorderLayout.CENTER);
        pMidPanel.add(pOptionsExercise, BorderLayout.CENTER);
        pMidPanel.add(pMarketDataType, BorderLayout.CENTER);

        // create order button panel
        JPanel pOrderButtonPanel = new JPanel();
        pOrderButtonPanel.add(m_sharesAlloc);
        pOrderButtonPanel.add(m_comboLegs);
        pOrderButtonPanel.add(m_btnDeltaNeutralContract);
        pOrderButtonPanel.add(m_btnAlgoParams);
        pOrderButtonPanel.add(m_btnSmartComboRoutingParams);

        pMidPanel.add(pOrderButtonPanel, BorderLayout.CENTER);

        JPanel pOrderButtonPanel2 = new JPanel();
        pOrderButtonPanel2.add(m_btnOptions);
        pOrderButtonPanel2.add(m_btnConditions);
        pOrderButtonPanel2.add(m_btnPeg2Bench);
        pOrderButtonPanel2.add(m_btnAdjStop);
        pOrderButtonPanel2.add(m_btnHistoricalData);

        pMidPanel.add(pOrderButtonPanel2, BorderLayout.CENTER);

        // create button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(m_ok);
        buttonPanel.add(m_cancel);

        // create action listeners
        m_btnHistoricalData.addActionListener(e -> onHistoricalData());
        m_btnPeg2Bench.addActionListener(e -> onBtnPeg2Bench());
        m_btnAdjStop.addActionListener(e -> onBtnAdjStop());
        m_btnConditions.addActionListener(e -> onBtnConditions());
        m_sharesAlloc.addActionListener(e -> onSharesAlloc());

        m_comboLegs.addActionListener(e -> onAddComboLegs());
        m_btnDeltaNeutralContract.addActionListener(e -> onBtnDeltaNeutralContract());
        m_btnAlgoParams.addActionListener(e -> onBtnAlgoParams());
        m_btnSmartComboRoutingParams.addActionListener(e -> onBtnSmartComboRoutingParams());
        m_btnOptions.addActionListener(e -> onBtnOptions());
        m_ok.addActionListener(e -> onOk());
        m_cancel.addActionListener(e -> onCancel());

        // create top panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(pId);
        topPanel.add(pMidPanel);

        // create dlg box
        getContentPane().add(topPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        JScrollPane scroller = new JScrollPane(topPanel);
        this.add(scroller, BorderLayout.CENTER);

        pack();
    }

    private void onHistoricalData() {
        m_historicalDataDlg.setVisible(true);

    }

    void onBtnAdjStop() {
        showModalPanelDialog(param -> new AdjustedPanel((JDialog) param, m_order));
    }

    void onBtnPeg2Bench() {
        showModalPanelDialog(param -> new PegBenchPanel((JDialog) param, m_order,
                contract -> {
                    try {
                        return m_parent.lookupContract(contract);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return new ArrayList<>();
                }));
    }


    void onBtnConditions() {
        showModalPanelDialog(param -> new ConditionsPanel((JDialog) param, m_order,
                contract -> {
                    try {
                        return m_parent.lookupContract(contract);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return new ArrayList<>();
                }));
    }

    interface CallableWithParam {
        Object call(Object param);
    }

    private void showModalPanelDialog(CallableWithParam panelCreator) {
        JDialog dialog = new JDialog();
        OnOKPanel panel = (OnOKPanel) panelCreator.call(dialog);

        dialog.add(panel);
        dialog.pack();
        dialog.setModal(true);
        dialog.setVisible(true);
        panel.onOK();
    }

    private static String pad(int val) {
        return val < 10 ? "0" + val : "" + val;
    }

    void onSharesAlloc() {
        if (!m_parent.m_bIsFAAccount) {
            return;
        }

        FAAllocationInfoDlg dlg = new FAAllocationInfoDlg(this);

        // show the combo leg dialog
        dlg.setVisible(true);
    }

    void onAddComboLegs() {
        ComboLegDlg comboLegDlg = new ComboLegDlg(
                m_contract.comboLegs(), m_order.orderComboLegs(), m_exchange.getText(), this);

        // show the combo leg dialog
        comboLegDlg.setVisible(true);
    }

    void onBtnDeltaNeutralContract() {
        DeltaNeutralContractDlg deltaNeutralContractDlg = new DeltaNeutralContractDlg(m_deltaNeutralContract, this);

        // show delta neutral dialog
        deltaNeutralContractDlg.setVisible(true);
        if (deltaNeutralContractDlg.ok()) {
            m_contract.deltaNeutralContract(m_deltaNeutralContract);
        } else if (deltaNeutralContractDlg.reset()) {
            m_contract.deltaNeutralContract(null);
        }
    }

    void onBtnAlgoParams() {
        AlgoParamsDlg algoParamsDlg = new AlgoParamsDlg(m_order, this);

        // show delta neutral dialog
        algoParamsDlg.setVisible(true);
    }

    void onBtnSmartComboRoutingParams() {
        SmartComboRoutingParamsDlg smartComboRoutingParamsDlg = new SmartComboRoutingParamsDlg("Smart Combo Routing Params", m_order.smartComboRoutingParams(), this);

        // show smart combo routing params dialog
        smartComboRoutingParamsDlg.setVisible(true);

        m_order.smartComboRoutingParams(smartComboRoutingParamsDlg.smartComboRoutingParams());
    }

    void onBtnOptions() {
        SmartComboRoutingParamsDlg smartComboRoutingParamsDlg = new SmartComboRoutingParamsDlg(m_optionsDlgTitle, m_options, this);

        // show smart combo routing params dialog
        smartComboRoutingParamsDlg.setVisible(true);

        m_options = smartComboRoutingParamsDlg.smartComboRoutingParams();
    }

    void onOk() {
        m_rc = false;

        try {
            // set id
            m_id = Integer.parseInt(m_Id.getText());

            // set contract fields
            m_contract.conid(ParseInt(m_conId.getText(), 0));
            m_contract.symbol(m_symbol.getText());
            m_contract.secType(m_secType.getText());
            m_contract.lastTradeDateOrContractMonth(m_lastTradeDateOrContractMonth.getText());
            m_contract.strike(ParseDouble(m_strike.getText(), 0.0));
            m_contract.right(m_right.getText());
            m_contract.multiplier(m_multiplier.getText());
            m_contract.exchange(m_exchange.getText());
            m_contract.primaryExch(m_primaryExch.getText());
            m_contract.currency(m_currency.getText());
            m_contract.localSymbol(m_localSymbol.getText());
            m_contract.tradingClass(m_tradingClass.getText());
            try {
                int includeExpired = Integer.parseInt(m_includeExpired.getText());
                m_contract.includeExpired(includeExpired == 1);
            } catch (NumberFormatException ex) {
                m_contract.includeExpired(false);
            }
            m_contract.secIdType(m_secIdType.getText());
            m_contract.secId(m_secId.getText());
            m_contract.issuerId(m_issuerId.getText());

            // set order fields
            m_order.action(m_action.getText());
            m_order.totalQuantity(Decimal.parse(m_totalQuantity.getText().trim()));
            m_order.orderType(m_orderType.getText());
            m_order.lmtPrice(parseStringToMaxDouble(m_lmtPrice.getText()));
            m_order.auxPrice(parseStringToMaxDouble(m_auxPrice.getText()));
            m_order.goodAfterTime(m_goodAfterTime.getText());
            m_order.goodTillDate(m_goodTillDate.getText());
            m_order.cashQty(parseStringToMaxDouble(m_cashQty.getText()));

            m_order.faGroup(m_faGroup);
            m_order.faProfile(m_faProfile);
            m_order.faMethod(m_faMethod);
            m_order.faPercentage(m_faPercentage);

            m_order.usePriceMgmtAlgo(m_usePriceMgmtAlgo.getSelectedItem().toBoolean());

            // set historical data fields
            m_exerciseAction = Integer.parseInt(m_exerciseActionTextField.getText());
            m_exerciseQuantity = Integer.parseInt(m_exerciseQuantityTextField.getText());
            m_override = Integer.parseInt(m_overrideTextField.getText());

            // set market depth rows
            m_marketDepthRows = Integer.parseInt(m_marketDepthRowTextField.getText());
            m_genericTicks = m_genericTicksTextField.getText();
            m_snapshotMktData = m_snapshotMktDataTextField.isSelected();
            m_reqSnapshotMktData = m_regSnapshotMktDataTextField.isSelected();

            m_marketDataType = m_marketDataTypeCombo.getSelectedIndex() + 1;
        } catch (Exception e) {
            Main.inform(this, "Error - " + e);
            return;
        }

        m_rc = true;
        setVisible(false);
    }

    void onCancel() {
        m_rc = false;
        setVisible(false);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            m_rc = false;
        }
        super.setVisible(b);
    }

    void setIdAtLeast(int id) {
        try {
            // set id field to at least id
            int curId = Integer.parseInt(m_Id.getText());
            if (curId < id) {
                m_Id.setText(String.valueOf(id));
            }
        } catch (Exception e) {
            Main.inform(this, "Error - " + e);
        }
    }

    private static int ParseInt(String text, int defValue) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    private static double ParseDouble(String text, double defValue) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    private static double parseStringToMaxDouble(String value) {
        if (value.trim().length() == 0) {
            return Double.MAX_VALUE;
        }
        return Double.parseDouble(value);
    }

    void init(String btnText, boolean btnEnabled, String dlgTitle, List<TagValue> options) {
        init(btnText, btnEnabled);
        m_options = options;
        m_optionsDlgTitle = dlgTitle;

        pack();
    }

    void init(String btnText, boolean btnEnabled) {
        m_btnOptions.setText(btnText);
        m_btnOptions.setEnabled(btnEnabled);
    }

    List<TagValue> options() {
        return m_options;
    }

    public boolean ignoreSize() {
        return m_historicalDataDlg.ignoreSize();
    }

    public int numberOfTicks() {
        return m_historicalDataDlg.numberOfTicks();
    }

    public String startDateTime() {
        return m_historicalDataDlg.startTime();
    }

    String backfillEndTime() {
        return m_historicalDataDlg.backfillEndTime();
    }

    String backfillDuration() {
        return m_historicalDataDlg.backfillDuration();
    }

    String barSizeSetting() {
        return m_historicalDataDlg.barSizeSetting();
    }

    int useRTH() {
        return m_historicalDataDlg.useRTH() ? 1 : 0;
    }

    int formatDate() {
        return m_historicalDataDlg.formatDate();
    }

    String whatToShow() {
        return m_historicalDataDlg.whatToshow();
    }

    boolean keepUpToDate() {
        return m_historicalDataDlg.keepUpToDate();
    }

    public Contract contract() {
        return m_contract;
    }

    public int id() {
        return m_id;
    }

    String tickByTickType() {
        return m_historicalDataDlg.tickByTickType();
    }

    boolean isSmartDepth() {
        return m_smartDepth.isSelected();
    }

}
