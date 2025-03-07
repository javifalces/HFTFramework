/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import static com.ib.client.Util.lookupContract;
import static com.ib.controller.Formats.fmt;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.SoftDollarTier;
import com.ib.client.TagValue;
import com.ib.client.Types.Action;
import com.ib.client.Types.AlgoParam;
import com.ib.client.Types.AlgoStrategy;
import com.ib.client.Types.ComboParam;
import com.ib.client.Types.HedgeType;
import com.ib.client.Types.Method;
import com.ib.client.Types.OcaType;
import com.ib.client.Types.ReferencePriceType;
import com.ib.client.Types.Rule80A;
import com.ib.client.Types.TimeInForce;
import com.ib.client.Types.TriggerMethod;
import com.ib.client.Types.UsePriceMgmtAlgo;
import com.ib.client.Types.VolatilityType;
import com.ib.controller.ApiController.IOrderCancelHandler;
import com.ib.controller.ApiController.IOrderHandler;

import com.ib.samples.apidemo.util.HtmlButton;
import com.ib.samples.apidemo.util.NewTabbedPanel;
import com.ib.samples.apidemo.util.TCombo;
import com.ib.samples.apidemo.util.UpperField;
import com.ib.samples.apidemo.util.Util;
import com.ib.samples.apidemo.util.VerticalPanel;
import com.ib.samples.apidemo.util.VerticalPanel.HorzPanel;
import com.ib.samples.apidemo.util.VerticalPanel.StackPanel;

class TicketDlg extends JDialog {
    private boolean m_editContract;
    private final Contract m_contract;
    private final Order m_order;
    private final ContractPanel m_contractPanel;
    private final OrderPanel m_orderPanel;
    private final AdvisorTicketPanel m_advisorPanel;
    private final MiscTicketPanel m_attribTicketPanel;
    private final VolatilityTicketPanel m_volPanel;
    private final ComboTicketPanel m_comboPanel;
    private final AlgoPanel m_algoPanel;
    private final ScalePanel m_scalePanel;
    private final PegBenchPanel m_pegBenchPanel;
    private final PegBestPegMidPanel m_pegBestPegMidPanel;
    private final AdjustedPanel m_adjustedPanel;
    private final ConditionsPanel m_conditionPanel;

    TicketDlg(Contract contract, Order order) {
        this(contract, order, false);
    }

    TicketDlg(Contract contract, Order order, boolean cancel) {
        super(ApiDemo.INSTANCE.frame());

        if (contract == null) {
            contract = new Contract();
            m_editContract = true;
        }

        if (order == null) {
            order = new Order();
            order.totalQuantity(Decimal.ONE_HUNDRED);
            order.lmtPrice(1);
        }

        m_contract = contract;
        m_order = order;

        m_contractPanel = new ContractPanel(m_contract);
        m_pegBenchPanel = new PegBenchPanel(this, m_order,
                c -> lookupContract(ApiDemo.INSTANCE.controller(), c));
        m_pegBestPegMidPanel = new PegBestPegMidPanel(this, m_order);
        m_advisorPanel = new AdvisorTicketPanel();
        m_attribTicketPanel = new MiscTicketPanel();
        m_volPanel = new VolatilityTicketPanel();
        m_comboPanel = new ComboTicketPanel();
        m_algoPanel = new AlgoPanel();
        m_scalePanel = new ScalePanel();
        m_orderPanel = new OrderPanel();
        m_adjustedPanel = new AdjustedPanel(this, m_order);
        m_conditionPanel = new ConditionsPanel(this, m_order,
                c -> lookupContract(ApiDemo.INSTANCE.controller(), c));

        HtmlButton transmitOrder = new HtmlButton(cancel ? "Cancel Order" : "Transmit Order") {
            @Override
            public void actionPerformed() {
                if (cancel) {
                    onCancelOrder();
                } else {
                    onTransmitOrder();
                }
            }
        };

        HtmlButton checkMargin = new HtmlButton("Check Margin") {
            @Override
            public void actionPerformed() {
                onCheckMargin();
            }
        };

        HtmlButton close = new HtmlButton("Close") {
            @Override
            public void actionPerformed() {
                dispose();
            }
        };

        NewTabbedPanel tabbedPanel = new NewTabbedPanel(true);
        if (m_editContract) {
            tabbedPanel.addTab("Contract", m_contractPanel);
        }
        tabbedPanel.addTab("Order", m_orderPanel);
        tabbedPanel.addTab("Pegged to benchmark", m_pegBenchPanel);
        tabbedPanel.addTab("Peg Best / Peg Mid", m_pegBestPegMidPanel);
        tabbedPanel.addTab("Adjustable stops", m_adjustedPanel);
        tabbedPanel.addTab("Misc", m_attribTicketPanel);
        tabbedPanel.addTab("Advisor", m_advisorPanel);
        tabbedPanel.addTab("Volatility", m_volPanel);
        if (m_contract.isCombo()) {
            tabbedPanel.addTab("Combo", m_comboPanel);
        }
        tabbedPanel.addTab("Scale", m_scalePanel);
        tabbedPanel.addTab("IB Algo", m_algoPanel);
        tabbedPanel.addTab("Conditions", m_conditionPanel);

        JPanel buts = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        buts.add(transmitOrder);
        buts.add(checkMargin);
        buts.add(close);

        // check-margin is for new orders only
        if (m_order.orderId() != 0) {
            checkMargin.setVisible(false);
        }

        add(tabbedPanel);
        add(buts, BorderLayout.SOUTH);

        setLocation(200, 200);
        pack();
        Util.closeOnEsc(this);
    }

    private void onTransmitOrder() {
        scrape();

        // close window right away for mods
        if (m_order.orderId() != 0) {
            dispose();
        }

        ApiDemo.INSTANCE.controller().placeOrModifyOrder(m_contract, m_order, new IOrderHandler() {
            @Override
            public void orderState(OrderState orderState) {
                ApiDemo.INSTANCE.controller().removeOrderHandler(this);
                SwingUtilities.invokeLater(() -> dispose());
            }

            @Override
            public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
            }

            @Override
            public void handle(int errorCode, final String errorMsg) {
                m_order.orderId(0);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(TicketDlg.this, errorMsg));
            }
        });
    }

    private void onCancelOrder() {
        String manualOrderCancelTime = m_orderPanel.m_manualOrderCancelTime.getText();

        if (m_order != null) {
            ApiDemo.INSTANCE.controller().cancelOrder(m_order.orderId(), manualOrderCancelTime, new IOrderCancelHandler() {
                @Override
                public void orderStatus(String orderStatus) {
                    ApiDemo.INSTANCE.controller().removeOrderCancelHandler(this);
                    SwingUtilities.invokeLater(() -> dispose());
                }

                @Override
                public void handle(int errorCode, final String errorMsg) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(TicketDlg.this, errorMsg));
                }
            });
        }
    }

    private void onCheckMargin() {
        scrape();

        m_order.whatIf(true);
        ApiDemo.INSTANCE.controller().placeOrModifyOrder(m_contract, m_order, new IOrderHandler() {
            @Override
            public void orderState(final OrderState orderState) {
                SwingUtilities.invokeLater(() -> displayMargin(orderState));
            }

            @Override
            public void handle(int errorCode, final String errorMsg) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(TicketDlg.this, errorMsg));
            }

            @Override
            public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
            }
        });

        m_order.whatIf(false);
        m_order.orderId(0);
    }

    private void displayMargin(OrderState orderState) {
        String str = String.format("Current:%nEquity with loan: %s%nInitial margin: %s%nMaintenance margin: %s%n%n"
                        + "Change:%nEquity with loan: %s%nInitial margin: %s%nMaintenance margin: %s%n%n"
                        + "Post-Trade:%nEquity with loan: %s%nInitial margin: %s%nMaintenance margin: %s%n%n",
                orderState.equityWithLoanBefore() != null ? fmt(Double.parseDouble(orderState.equityWithLoanBefore())) : "",
                orderState.initMarginBefore() != null ? fmt(Double.parseDouble(orderState.initMarginBefore())) : "",
                orderState.maintMarginBefore() != null ? fmt(Double.parseDouble(orderState.maintMarginBefore())) : "",
                orderState.equityWithLoanChange() != null ? fmt(Double.parseDouble(orderState.equityWithLoanChange())) : "",
                orderState.initMarginChange() != null ? fmt(Double.parseDouble(orderState.initMarginChange())) : "",
                orderState.maintMarginChange() != null ? fmt(Double.parseDouble(orderState.maintMarginChange())) : "",
                fmt(Double.parseDouble(orderState.equityWithLoanAfter())),
                fmt(Double.parseDouble(orderState.initMarginAfter())),
                fmt(Double.parseDouble(orderState.maintMarginAfter()))
        );

        JOptionPane.showMessageDialog(this, str, "Margin Requirements", JOptionPane.INFORMATION_MESSAGE);
    }

    private void scrape() {
        m_order.smartComboRoutingParams().clear();

        if (m_editContract) {
            m_contractPanel.onOK();
        }
        m_orderPanel.onOK();
        m_advisorPanel.onOK();
        m_attribTicketPanel.onOK();
        m_volPanel.onOK();
        m_algoPanel.onOK();
        m_scalePanel.onOK();
        if (m_contract.isCombo()) {
            m_comboPanel.onOK();
        }
        m_pegBenchPanel.onOK();
        m_pegBestPegMidPanel.onOK();
        m_adjustedPanel.onOK();
        m_conditionPanel.onOK();
    }

    enum AmntUnit {
        Amnt("amnt", 0),
        Percent("%", 100);

        String m_text;
        int m_val;

        AmntUnit(String txt, int v) {
            m_text = txt;
            m_val = v;
        }

        public static AmntUnit fromInt(int i) {
            for (AmntUnit v : AmntUnit.values())
                if (v.m_val == i)
                    return v;

            return Amnt;
        }

        @Override
        public String toString() {
            return m_text;
        }
    }

    class OrderPanel extends VerticalPanel {
        final TCombo<String> m_account = new TCombo<>(ApiDemo.INSTANCE.accountList().toArray(new String[0]));
        final TCombo<Action> m_action = new TCombo<>(Action.values());
        final JTextField m_modelCode = new JTextField();
        final UpperField m_quantity = new UpperField("100");
        final UpperField m_cashQty = new UpperField();
        final UpperField m_displaySize = new UpperField();
        final TCombo<OrderType> m_orderType = new TCombo<>(OrderType.values());
        final UpperField m_lmtPrice = new UpperField("200");
        final UpperField m_auxPrice = new UpperField();
        final TCombo<TimeInForce> m_tif = new TCombo<>(TimeInForce.values());
        final JCheckBox m_nonGuaranteed = new JCheckBox();
        final UpperField m_lmtPriceOffset = new UpperField();
        final UpperField m_triggerPrice = new UpperField();
        final UpperField m_mifid2DecisionMaker = new UpperField();
        final UpperField m_mifid2DecisionAlgo = new UpperField();
        final UpperField m_mifid2ExecutionTrader = new UpperField();
        final UpperField m_mifid2ExecutionAlgo = new UpperField();
        final TCombo<UsePriceMgmtAlgo> m_usePriceMgmtAlgo = new TCombo<>(UsePriceMgmtAlgo.values());
        final JTextField m_advancedErrorOverride = new JTextField();
        final JTextField m_manualOrderTime = new JTextField();
        final JTextField m_manualOrderCancelTime = new JTextField();

        OrderPanel() {
            m_orderType.removeItemAt(0); // remove None

            m_account.setSelectedItem(m_order.account() != null ? m_order.account() : ApiDemo.INSTANCE.accountList().get(0));
            m_modelCode.setText(m_order.modelCode());
            m_action.setSelectedItem(m_order.action());
            m_quantity.setText(m_order.totalQuantity().toString());
            m_cashQty.setText(m_order.cashQty());
            m_displaySize.setText(m_order.displaySize());
            m_orderType.setSelectedItem(m_order.orderType());
            m_lmtPrice.setText(m_order.lmtPrice());
            m_auxPrice.setText(m_order.auxPrice());
            m_tif.setSelectedItem(m_order.tif());
            m_nonGuaranteed.setSelected(getVal(ComboParam.NonGuaranteed).equals("1"));
            m_lmtPriceOffset.setText(m_order.lmtPriceOffset());
            m_triggerPrice.setText(m_order.triggerPrice());
            m_mifid2DecisionMaker.setText(m_order.mifid2DecisionMaker());
            m_mifid2DecisionAlgo.setText(m_order.mifid2DecisionAlgo());
            m_mifid2ExecutionTrader.setText(m_order.mifid2ExecutionTrader());
            m_mifid2ExecutionAlgo.setText(m_order.mifid2ExecutionAlgo());
            m_usePriceMgmtAlgo.setSelectedIndex(m_order.usePriceMgmtAlgo() == null ? 0 : m_order.usePriceMgmtAlgo() ? 2 : 1);

            add("Account", m_account);

            m_modelCode.setColumns(7);

            add("Model code", m_modelCode);
            add("Action", m_action);
            add("Quantity", m_quantity);
            add("Cash Qty", m_cashQty);
            add("Display size", m_displaySize);
            add("Order type", m_orderType);
            add("Limit price", m_lmtPrice);
            add("Limit price offset", m_lmtPriceOffset);
            add("Trigger price", m_triggerPrice);
            add("Aux price", m_auxPrice);
            add("Time-in-force", m_tif);
            add("MiFID II Decision Maker", m_mifid2DecisionMaker);
            add("MiFID II Decision Algo", m_mifid2DecisionAlgo);
            add("MiFID II Execution Trader", m_mifid2ExecutionTrader);
            add("MiFID II Execution Algo", m_mifid2ExecutionAlgo);

            if (m_contract.isCombo()) {
                add("Non-guaranteed", m_nonGuaranteed);
            }

            add("Use Price Management Algo", m_usePriceMgmtAlgo);
            add("Advanced Error Override", m_advancedErrorOverride);
            add("Manual Order Time", m_manualOrderTime);
            add("Manual Order Cancel Time", m_manualOrderCancelTime);
        }

        private void onOK() {
            m_order.account(m_account.getText().toUpperCase());
            m_order.modelCode(m_modelCode.getText().trim());
            m_order.action(m_action.getSelectedItem());
            m_order.totalQuantity(Decimal.parse(m_quantity.getText().trim()));
            m_order.cashQty(m_cashQty.getDouble());
            m_order.displaySize(m_displaySize.getInt());
            m_order.orderType(m_orderType.getSelectedItem());
            m_order.lmtPrice(m_lmtPrice.getDouble());
            m_order.auxPrice(m_auxPrice.getDouble());
            m_order.tif(m_tif.getSelectedItem());
            m_order.lmtPriceOffset(m_lmtPriceOffset.getDouble());
            m_order.triggerPrice(m_triggerPrice.getDouble());
            m_order.mifid2DecisionMaker(m_mifid2DecisionMaker.getText());
            m_order.mifid2DecisionAlgo(m_mifid2DecisionAlgo.getText());
            m_order.mifid2ExecutionTrader(m_mifid2ExecutionTrader.getText());
            m_order.mifid2ExecutionAlgo(m_mifid2ExecutionAlgo.getText());
            m_order.usePriceMgmtAlgo(m_usePriceMgmtAlgo.getSelectedItem().toBoolean());
            m_order.advancedErrorOverride(m_advancedErrorOverride.getText());
            m_order.manualOrderTime(m_manualOrderTime.getText());

            if (m_contract.isCombo()) {
                TagValue tv = new TagValue(ComboParam.NonGuaranteed.toString(), m_nonGuaranteed.isSelected() ? "1" : "0");
                m_order.smartComboRoutingParams().add(tv);
            }
        }
    }

    class AdvisorTicketPanel extends VerticalPanel {
        final UpperField m_faGroup = new UpperField();
        final TCombo<Method> m_faMethod = new TCombo<>(Method.values());
        final UpperField m_faPercentage = new UpperField();
        final UpperField m_faProfile = new UpperField();

        AdvisorTicketPanel() {
            m_faGroup.setText(m_order.faGroup());
            m_faMethod.setSelectedItem(m_order.faMethod());
            m_faPercentage.setText(m_order.faPercentage());
            m_faProfile.setText(m_order.faProfile());

            add("Group", m_faGroup);
            add("Method", m_faMethod);
            add("Percentage", m_faPercentage);
            add(Box.createVerticalStrut(10));
            add("--or--");
            add(Box.createVerticalStrut(10));
            add("Profile", m_faProfile);
        }

        void onOK() {
            m_order.faGroup(m_faGroup.getText());
            m_order.faMethod(m_faMethod.getSelectedItem());
            m_order.faPercentage(m_faPercentage.getText());
            m_order.faProfile(m_faProfile.getText());
        }
    }

    class MiscTicketPanel extends StackPanel {
        final JTextField m_goodAfter = new JTextField();
        final JTextField m_goodTil = new JTextField();
        final UpperField m_duration = new UpperField();
        final UpperField m_postToAts = new UpperField();
        final JTextField m_orderRef = new JTextField(7);
        final JTextField m_ocaGroup = new JTextField(7);
        final UpperField m_minQty = new UpperField();
        final UpperField m_percentOffset = new UpperField();
        final UpperField m_trailingStopPrice = new UpperField();
        final UpperField m_trailingPercent = new UpperField();
        final UpperField m_discretionaryAmt = new UpperField();
        final UpperField m_algoId = new UpperField();
        final UpperField m_extOperator = new UpperField();
        final TCombo<SoftDollarTier> m_softDollarTiers = new TCombo<>();

        final TCombo<OcaType> m_ocaType = new TCombo<>(OcaType.values());
        final TCombo<Rule80A> m_rule80A = new TCombo<>(Rule80A.values());
        final TCombo<TriggerMethod> m_trigger = new TCombo<>(TriggerMethod.values());

        final TCombo<HedgeType> m_hedgeType = new TCombo<>(HedgeType.values());
        final UpperField m_hedgeParam = new UpperField();

        final JCheckBox m_blockOrder = new JCheckBox();
        final JCheckBox m_sweepToFill = new JCheckBox();
        final JCheckBox m_hidden = new JCheckBox();
        final JCheckBox m_outsideRth = new JCheckBox();
        final JCheckBox m_allOrNone = new JCheckBox();
        final JCheckBox m_overrideConstraints = new JCheckBox();
        final JCheckBox m_notHeld = new JCheckBox();
        final JCheckBox m_transmit = new JCheckBox();
        final JCheckBox m_optOutSmartRouting = new JCheckBox();
        final JCheckBox m_dontUseAutoPriceForHedge = new JCheckBox();
        final JCheckBox m_omsContainer = new JCheckBox();
        final JCheckBox m_discretionaryUpToLimitPrice = new JCheckBox();
        final JCheckBox m_autoCancelParent = new JCheckBox();
        final JCheckBox m_solicited = new JCheckBox();


        MiscTicketPanel() {
            VerticalPanel top = new VerticalPanel();
            top.add("Order ref", m_orderRef);
            top.add("Min Qty", m_minQty);
            top.add("Good after", m_goodAfter);
            top.add("Good until", m_goodTil);
            top.add("Duration", m_duration);
            top.add("Post To Ats", m_postToAts);
            top.add("Rule 80A", m_rule80A);
            top.add("Trigger method", m_trigger);
            top.add("Percent Offset", m_percentOffset);
            top.add("Trail order stop price", m_trailingStopPrice);
            top.add("Trailing percent", m_trailingPercent);
            top.add("Discretionary amount", m_discretionaryAmt);
            top.add("Algo Id", m_algoId);
            top.add("OCA group and type", m_ocaGroup, m_ocaType);
            top.add("Hedge type and param", m_hedgeType, m_hedgeParam);
            top.add("Ext operator", m_extOperator);
            top.add("Soft dollar tier", m_softDollarTiers);

            VerticalPanel left = new VerticalPanel();
            left.add("Not held", m_notHeld);
            left.add("Block order", m_blockOrder);
            left.add("Sweep-to-fill", m_sweepToFill);
            left.add("Hidden", m_hidden);
            left.add("Fill outside RTH", m_outsideRth);
            left.add("All-or-none", m_allOrNone);
            left.add("Relative discretionary", m_discretionaryUpToLimitPrice);

            VerticalPanel right = new VerticalPanel();
            right.add("Override constraints", m_overrideConstraints);
            right.add("Opt out SMART routing", m_optOutSmartRouting);
            right.add("Don't use auto price for hedge", m_dontUseAutoPriceForHedge);
            right.add("Transmit", m_transmit);
            right.add("OMS Container", m_omsContainer);
            right.add("Auto Cancel Parent", m_autoCancelParent);
            right.add("Solicited", m_solicited);

            HorzPanel checks = new HorzPanel();
            checks.add(left);
            checks.add(Box.createHorizontalGlue());
            checks.add(right);
            checks.add(Box.createHorizontalGlue());

            add(top);
            add(Box.createVerticalStrut(20));
            add(checks);

            m_minQty.setText(m_order.minQty());
            m_goodAfter.setText(m_order.goodAfterTime());
            m_goodTil.setText(m_order.goodTillDate());
            m_duration.setText(m_order.duration());
            m_postToAts.setText(m_order.postToAts());
            m_orderRef.setText(m_order.orderRef());
            m_ocaGroup.setText(m_order.ocaGroup());
            m_ocaType.setSelectedItem(m_order.ocaType());
            m_rule80A.setSelectedItem(m_order.rule80A());
            m_trigger.setSelectedItem(m_order.triggerMethod());
            m_blockOrder.setSelected(m_order.blockOrder());
            m_sweepToFill.setSelected(m_order.sweepToFill());
            m_hidden.setSelected(m_order.hidden());
            m_outsideRth.setSelected(m_order.outsideRth());
            m_allOrNone.setSelected(m_order.allOrNone());
            m_overrideConstraints.setSelected(m_order.overridePercentageConstraints());
            m_hedgeType.setSelectedItem(m_order.hedgeType());
            m_hedgeParam.setText(m_order.hedgeParam());
            m_notHeld.setSelected(m_order.notHeld());
            m_percentOffset.setText(m_order.percentOffset());
            m_trailingStopPrice.setText(m_order.trailStopPrice());
            m_trailingPercent.setText(m_order.trailingPercent());
            m_discretionaryAmt.setText(m_order.discretionaryAmt());
            m_optOutSmartRouting.setSelected(m_order.optOutSmartRouting());
            m_algoId.setText(m_order.algoId());
            m_transmit.setSelected(true);
            m_extOperator.setText(m_order.extOperator());
            m_softDollarTiers.removeAllItems();
            m_dontUseAutoPriceForHedge.setSelected(m_order.dontUseAutoPriceForHedge());
            m_omsContainer.setSelected(m_order.isOmsContainer());
            m_discretionaryUpToLimitPrice.setSelected(m_order.discretionaryUpToLimitPrice());
            m_autoCancelParent.setSelected(m_order.autoCancelParent());
            m_solicited.setSelected(m_order.solicited());

            ApiDemo.INSTANCE.controller().reqSoftDollarTiers(tiers -> {
                m_softDollarTiers.invalidate();
                m_softDollarTiers.removeAllItems();
                m_softDollarTiers.addItem(new SoftDollarTier("", "", ""));

                for (SoftDollarTier tier : tiers) {
                    m_softDollarTiers.addItem(tier);
                }
            });

        }

        void onOK() {
            m_order.minQty(m_minQty.getInt());
            m_order.goodAfterTime(m_goodAfter.getText());
            m_order.goodTillDate(m_goodTil.getText());
            m_order.duration(m_duration.getInt());
            m_order.postToAts(m_postToAts.getInt());
            m_order.orderRef(m_orderRef.getText());
            m_order.ocaGroup(m_ocaGroup.getText());
            m_order.ocaType(m_ocaType.getSelectedItem());
            m_order.rule80A(m_rule80A.getSelectedItem());
            m_order.triggerMethod(m_trigger.getSelectedItem());
            m_order.sweepToFill(m_sweepToFill.isSelected());
            m_order.hidden(m_hidden.isSelected());
            m_order.outsideRth(m_outsideRth.isSelected());
            m_order.allOrNone(m_allOrNone.isSelected());
            m_order.overridePercentageConstraints(m_overrideConstraints.isSelected());
            m_order.hedgeType(m_hedgeType.getSelectedItem());
            m_order.hedgeParam(m_hedgeParam.getText());
            m_order.notHeld(m_notHeld.isSelected());
            m_order.percentOffset(m_percentOffset.getDouble());
            m_order.trailStopPrice(m_trailingStopPrice.getDouble());
            m_order.trailingPercent(m_trailingPercent.getDouble());
            m_order.discretionaryAmt(m_discretionaryAmt.getDouble());
            m_order.optOutSmartRouting(m_optOutSmartRouting.isSelected());
            m_order.algoId(m_algoId.getText());
            m_order.transmit(m_transmit.isSelected());
            m_order.extOperator(m_extOperator.getText());
            m_order.softDollarTier(m_softDollarTiers.getSelectedItem());
            m_order.dontUseAutoPriceForHedge(m_dontUseAutoPriceForHedge.isSelected());
            m_order.isOmsContainer(m_omsContainer.isSelected());
            m_order.discretionaryUpToLimitPrice(m_discretionaryUpToLimitPrice.isSelected());
            m_order.autoCancelParent(m_autoCancelParent.isSelected());
            m_order.solicited(m_solicited.isSelected());
        }
    }

    class VolatilityTicketPanel extends VerticalPanel {
        final UpperField m_volatility = new UpperField();
        final TCombo<VolatilityType> m_volatilityType = new TCombo<>(VolatilityType.values());
        final JCheckBox m_continuousUpdate = new JCheckBox();
        final TCombo<ReferencePriceType> m_referencePriceType = new TCombo<>(ReferencePriceType.values());
        final TCombo<OrderType> m_deltaNeutralOrderType = new TCombo<>(OrderType.values());
        final UpperField m_deltaNeutralAuxPrice = new UpperField();
        final UpperField m_deltaNeutralConId = new UpperField();
        final UpperField m_upper = new UpperField();
        final UpperField m_lower = new UpperField();

        VolatilityTicketPanel() {
            add("Volatility", m_volatility, m_volatilityType);
            add("Continuously update price", m_continuousUpdate);
            add("Option reference price", m_referencePriceType);
            add("Hedge order type", m_deltaNeutralOrderType);
            add("Hedge order aux price", m_deltaNeutralAuxPrice);
            add("Hedge contract conid", m_deltaNeutralConId);
            add("Stock range - upper", m_upper);
            add("Stock range - lower", m_lower);

            m_volatility.setText(m_order.volatility());
            m_volatilityType.setSelectedItem(m_order.volatilityType());
            m_continuousUpdate.setSelected(m_order.continuousUpdate() != 0);
            m_referencePriceType.setSelectedItem(m_order.referencePriceType());
            m_deltaNeutralOrderType.setSelectedItem(m_order.deltaNeutralOrderType());
            m_deltaNeutralAuxPrice.setText(m_order.deltaNeutralAuxPrice());
            m_deltaNeutralConId.setText(m_order.deltaNeutralConId());
            m_upper.setText(m_order.stockRangeUpper());
            m_lower.setText(m_order.stockRangeLower());
        }

        void onOK() {
            m_order.volatility(m_volatility.getDouble());
            m_order.volatilityType(m_volatilityType.getSelectedItem());
            m_order.continuousUpdate(m_continuousUpdate.isSelected() ? 1 : 0);
            m_order.referencePriceType(m_referencePriceType.getSelectedItem());
            m_order.deltaNeutralOrderType(m_deltaNeutralOrderType.getSelectedItem());
            m_order.deltaNeutralAuxPrice(m_deltaNeutralAuxPrice.getDouble());
            m_order.deltaNeutralConId(m_deltaNeutralConId.getInt());
            m_order.stockRangeUpper(m_upper.getDouble());
            m_order.stockRangeLower(m_lower.getDouble());
        }
    }

    /**
     * This panel edits all ComboParam values except for Non-Guaranteed.
     * That one goes on main panel because it applies to all combo orders.
     */
    class ComboTicketPanel extends VerticalPanel {
        final UpperField[] m_fields = new UpperField[ComboParam.values().length];

        ComboTicketPanel() {
            for (ComboParam param : ComboParam.values()) {
                if (param == ComboParam.NonGuaranteed) {
                    continue;
                }
                UpperField field = new UpperField();
                m_fields[param.ordinal()] = field;
                add(param.toString(), field);
                field.setText(getVal(param));
            }
        }

        void onOK() {
            for (ComboParam param : ComboParam.values()) {
                if (param == ComboParam.NonGuaranteed) {
                    continue;
                }
                UpperField field = m_fields[param.ordinal()];
                String val = field.getText();
                if (val != null && val.length() > 0) {
                    TagValue tv = new TagValue(param.toString(), val);
                    m_order.smartComboRoutingParams().add(tv);
                }
            }
        }
    }

    class AlgoPanel extends VerticalPanel {
        final TCombo<AlgoStrategy> m_strategy = new TCombo<>(AlgoStrategy.values());
        final UpperField[] m_params = new UpperField[AlgoParam.values().length];

        AlgoPanel() {
            add("Algo strategy", m_strategy);

            for (AlgoParam param : AlgoParam.values()) {
                int i = param.ordinal();
                m_params[i] = new UpperField(11, true);
                add(param.toString(), m_params[param.ordinal()]);
            }

            m_strategy.setSelectedItem(m_order.algoStrategy());

            if (m_order.algoParams() != null) {
                for (TagValue tagVal : m_order.algoParams()) {
                    AlgoParam param = AlgoParam.valueOf(tagVal.m_tag);
                    m_params[param.ordinal()].setText(tagVal.m_value);
                }
            }

            m_strategy.addActionListener(e -> onSelChanged());

            onSelChanged();
        }

        void onSelChanged() {
            for (UpperField m_param : m_params) {
                m_param.setEnabled(false);
            }

            AlgoStrategy strategy = m_strategy.getSelectedItem();
            if (strategy != null) {
                for (AlgoParam param : strategy.params()) {
                    m_params[param.ordinal()].setEnabled(true);
                }
            }
        }

        void onOK() {
            m_order.algoStrategy(m_strategy.getSelectedItem());

            m_order.algoParams().clear();
            for (AlgoParam param : AlgoParam.values()) {
                String val = m_params[param.ordinal()].getText();
                if (val != null && val.length() > 0) {
                    m_order.algoParams().add(new TagValue(param.toString(), val));
                }
            }
        }
    }

    class ScalePanel extends VerticalPanel {
        UpperField m_initLevelSize = new UpperField();
        UpperField m_subsLevelSize = new UpperField();
        UpperField m_priceIncrement = new UpperField();
        UpperField m_priceAdjustValue = new UpperField();
        UpperField m_priceAdjustInterval = new UpperField();
        UpperField m_profitOffset = new UpperField();
        JCheckBox m_autoReset = new JCheckBox();
        UpperField m_initPosition = new UpperField();
        UpperField m_initFillQty = new UpperField();
        JCheckBox m_randomPercent = new JCheckBox();
        UpperField m_table = new UpperField();

        ScalePanel() {
            m_initLevelSize.setText(m_order.scaleInitLevelSize());
            m_subsLevelSize.setText(m_order.scaleSubsLevelSize());
            m_priceIncrement.setText(m_order.scalePriceIncrement());
            m_priceAdjustValue.setText(m_order.scalePriceAdjustValue());
            m_priceAdjustInterval.setText(m_order.scalePriceAdjustInterval());
            m_profitOffset.setText(m_order.scaleProfitOffset());
            m_autoReset.setSelected(m_order.scaleAutoReset());
            m_initPosition.setText(m_order.scaleInitPosition());
            m_initFillQty.setText(m_order.scaleInitFillQty());
            m_randomPercent.setSelected(m_order.scaleRandomPercent());
            m_table.setText(m_order.scaleTable());

            add("Initial comp size", m_initLevelSize);
            add("Subsequent comp size", m_subsLevelSize);
            add("Randomize size", m_randomPercent);
            add(Box.createVerticalStrut(10));
            add("Price increment", m_priceIncrement);
            add("Profit offset", m_profitOffset);
            add("Auto-reset", m_autoReset);
            add(Box.createVerticalStrut(10));
            add("Initial position", m_initPosition);
            add("Filled init comp size", m_initFillQty);
            add(Box.createVerticalStrut(10));
            add("Increase price by", m_priceAdjustValue, new JLabel("every"), m_priceAdjustInterval, new JLabel("seconds"));
            add(Box.createVerticalStrut(10));
            add("Manual table", m_table);
        }

        void onOK() {
            m_order.scaleInitLevelSize(m_initLevelSize.getInt());
            m_order.scaleSubsLevelSize(m_subsLevelSize.getInt());
            m_order.scalePriceIncrement(m_priceIncrement.getDouble());
            m_order.scalePriceAdjustValue(m_priceAdjustValue.getDouble());
            m_order.scalePriceAdjustInterval(m_priceAdjustInterval.getInt());
            m_order.scaleProfitOffset(m_profitOffset.getDouble());
            m_order.scaleAutoReset(m_autoReset.isSelected());
            m_order.scaleInitPosition(m_initPosition.getInt());
            m_order.scaleInitFillQty(m_initFillQty.getInt());
            m_order.scaleRandomPercent(m_randomPercent.isSelected());
            m_order.scaleTable(m_table.getText());
        }
    }

    private String getVal(ComboParam param) {
        if (m_order.smartComboRoutingParams() != null) {
            for (TagValue tv : m_order.smartComboRoutingParams()) {
                if (tv.m_tag.equals(param.toString())) {
                    return tv.m_value;
                }
            }
        }
        return "";
    }
}
