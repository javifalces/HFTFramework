/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.ib.client.Order;

import com.ib.samples.apidemo.util.UpperField;

public class ExtOrdDlg extends JDialog {
    public Order m_order = new Order();
    public boolean m_rc;

    private JTextField m_tif = new JTextField("DAY");
    private JTextField m_duration = new JTextField();
    private JTextField m_postToAts = new JTextField();
    private JTextField m_activeStartTime = new JTextField();
    private JTextField m_activeStopTime = new JTextField();
    private JTextField m_ocaGroup = new JTextField();
    private JTextField m_ocaType = new JTextField("0");

    private JTextField m_account = new JTextField();
    private JTextField m_modelCode = new JTextField();
    private JTextField m_settlingFirm = new JTextField();
    private JTextField m_clearingAccount = new JTextField();
    private JTextField m_clearingIntent = new JTextField();

    private JTextField m_openClose = new JTextField();
    private JTextField m_origin = new JTextField("1");
    private JTextField m_orderRef = new JTextField();
    private JTextField m_parentId = new JTextField("0");
    private JTextField m_transmit = new JTextField("1");
    private JTextField m_blockOrder = new JTextField("0");
    private JTextField m_sweepToFill = new JTextField("0");
    private JTextField m_displaySize = new JTextField("0");
    private JTextField m_triggerMethod = new JTextField("0");
    private JTextField m_outsideRth = new JTextField("0");
    private JTextField m_hidden = new JTextField("0");
    private JTextField m_discretionaryAmt = new JTextField("0");
    private JTextField m_shortSaleSlot = new JTextField("0");
    private JTextField m_designatedLocation = new JTextField();
    private JTextField m_exemptCode = new JTextField("-1");
    private JTextField m_rule80A = new JTextField();
    private JTextField m_allOrNone = new JTextField();
    private JTextField m_overridePercentageConstraints = new JTextField();
    private JTextField m_minQty = new JTextField();
    private JTextField m_percentOffset = new JTextField();
    private JTextField m_auctionStrategy = new JTextField("0");
    private JTextField m_startingPrice = new JTextField();
    private JTextField m_stockRefPrice = new JTextField();
    private JTextField m_delta = new JTextField();
    private JTextField m_BOXstockRangeLower = new JTextField();
    private JTextField m_BOXstockRangeUpper = new JTextField();

    private JTextField m_VOLVolatility = new JTextField();
    private JTextField m_VOLVolatilityType = new JTextField();
    private JTextField m_VOLDeltaNeutralOrderType = new JTextField();
    private JTextField m_VOLDeltaNeutralAuxPrice = new JTextField();
    private JTextField m_VOLDeltaNeutralConId = new JTextField();
    private JTextField m_VOLDeltaNeutralSettlingFirm = new JTextField();
    private JTextField m_VOLDeltaNeutralClearingAccount = new JTextField();
    private JTextField m_VOLDeltaNeutralClearingIntent = new JTextField();
    private JTextField m_VOLDeltaNeutralOpenClose = new JTextField();
    private JCheckBox m_VOLDeltaNeutralShortSale = new JCheckBox("VOL: Hedge Delta ShortSale", false);
    private JTextField m_VOLDeltaNeutralShortSaleSlot = new JTextField();
    private JTextField m_VOLDeltaNeutralDesignatedLocation = new JTextField();
    private JTextField m_VOLContinuousUpdate = new JTextField();
    private JTextField m_VOLReferencePriceType = new JTextField();
    private JTextField m_trailStopPrice = new JTextField();
    private JTextField m_trailingPercent = new JTextField();

    private JTextField m_scaleInitLevelSize = new JTextField();
    private JTextField m_scaleSubsLevelSize = new JTextField();
    private JTextField m_scalePriceIncrement = new JTextField();
    private JTextField m_scalePriceAdjustValue = new JTextField();
    private JTextField m_scalePriceAdjustInterval = new JTextField();
    private JTextField m_scaleProfitOffset = new JTextField();
    private JCheckBox m_scaleAutoReset = new JCheckBox("SCALE: Auto Reset", false);
    private JTextField m_scaleInitPosition = new JTextField();
    private JTextField m_scaleInitFillQty = new JTextField();
    private JCheckBox m_scaleRandomPercent = new JCheckBox("SCALE: Random Percent", false);
    private JTextField m_scaleTable = new JTextField();

    private JTextField m_hedgeType = new JTextField();
    private JTextField m_hedgeParam = new JTextField();
    private JCheckBox m_optOutSmartRoutingCheckBox = new JCheckBox("Opting out of SMART routing", false);
    private JCheckBox m_solicited = new JCheckBox("Solicited", false);
    private JCheckBox m_randomizeSize = new JCheckBox("Randomize size", false);
    private JCheckBox m_randomizePrice = new JCheckBox("Randomize price", false);
    private UpperField m_mifid2DecisionMaker = new UpperField();
    private UpperField m_mifid2DecisionAlgo = new UpperField();
    private UpperField m_mifid2ExecutionTrader = new UpperField();
    private UpperField m_mifid2ExecutionAlgo = new UpperField();
    private JCheckBox m_dontUseAutoPriceForHedge = new JCheckBox("Don't use auto price for hedge", false);
    private JCheckBox m_isOmsConainer = new JCheckBox("OMS Container", false);
    private JCheckBox m_discretionaryUpToLimitPrice = new JCheckBox("Relative discretionary", false);
    private JCheckBox m_notHeld = new JCheckBox("Not held", false);
    private JCheckBox m_autoCancelParent = new JCheckBox("Auto Cancel Parent", false);
    private JTextField m_advancedErrorOverride = new JTextField();
    private JTextField m_manualOrderTime = new JTextField();
    private JTextField m_manualOrderCancelTime = new JTextField();
    private JTextField m_minTradeQty = new JTextField();
    private JTextField m_minCompeteSize = new JTextField();
    private JTextField m_competeAgainstBestOffset = new JTextField();
    private JCheckBox m_competeAgainstBestOffsetUpToMid = new JCheckBox("Compete Against Best Offset Up To Mid", false);
    private JTextField m_midOffsetAtWhole = new JTextField();
    private JTextField m_midOffsetAtHalf = new JTextField();

    ExtOrdDlg(OrderDlg owner) {
        super(owner, true);

        setTitle("Sample");

        // create extended order attributes panel
        JPanel extOrderDetailsPanel = new JPanel(new GridLayout(0, 6, 5, 5));
        extOrderDetailsPanel.setBorder(BorderFactory.createTitledBorder("Extended Order Info"));
        extOrderDetailsPanel.add(new JLabel("TIF"));
        extOrderDetailsPanel.add(m_tif);
        extOrderDetailsPanel.add(new JLabel("Duration"));
        extOrderDetailsPanel.add(m_duration);
        extOrderDetailsPanel.add(new JLabel("Post To ATS"));
        extOrderDetailsPanel.add(m_postToAts);
        extOrderDetailsPanel.add(new JLabel("Active Start Time"));
        extOrderDetailsPanel.add(m_activeStartTime);
        extOrderDetailsPanel.add(new JLabel("Active Stop Time"));
        extOrderDetailsPanel.add(m_activeStopTime);
        extOrderDetailsPanel.add(new JLabel("OCA Group"));
        extOrderDetailsPanel.add(m_ocaGroup);
        extOrderDetailsPanel.add(new JLabel("OCA Type"));
        extOrderDetailsPanel.add(m_ocaType);

        extOrderDetailsPanel.add(new JLabel("Account"));
        extOrderDetailsPanel.add(m_account);
        extOrderDetailsPanel.add(new JLabel("Model Code"));
        extOrderDetailsPanel.add(m_modelCode);
        extOrderDetailsPanel.add(new JLabel("Settling Firm"));
        extOrderDetailsPanel.add(m_settlingFirm);
        extOrderDetailsPanel.add(new JLabel("Clearing Account"));
        extOrderDetailsPanel.add(m_clearingAccount);
        extOrderDetailsPanel.add(new JLabel("Clearing Intent"));
        extOrderDetailsPanel.add(m_clearingIntent);

        extOrderDetailsPanel.add(new JLabel("Open/Close"));
        extOrderDetailsPanel.add(m_openClose);
        extOrderDetailsPanel.add(new JLabel("Origin"));
        extOrderDetailsPanel.add(m_origin);
        extOrderDetailsPanel.add(new JLabel("OrderRef"));
        extOrderDetailsPanel.add(m_orderRef);
        extOrderDetailsPanel.add(new JLabel("Parent Id"));
        extOrderDetailsPanel.add(m_parentId);
        extOrderDetailsPanel.add(new JLabel("Transmit"));
        extOrderDetailsPanel.add(m_transmit);
        extOrderDetailsPanel.add(new JLabel("Block Order"));
        extOrderDetailsPanel.add(m_blockOrder);
        extOrderDetailsPanel.add(new JLabel("Sweep To Fill"));
        extOrderDetailsPanel.add(m_sweepToFill);
        extOrderDetailsPanel.add(new JLabel("Display Size"));
        extOrderDetailsPanel.add(m_displaySize);
        extOrderDetailsPanel.add(new JLabel("Trigger Method"));
        extOrderDetailsPanel.add(m_triggerMethod);
        extOrderDetailsPanel.add(new JLabel("Outside Regular Trading Hours"));
        extOrderDetailsPanel.add(m_outsideRth);
        extOrderDetailsPanel.add(new JLabel("Hidden"));
        extOrderDetailsPanel.add(m_hidden);
        extOrderDetailsPanel.add(new JLabel("Discretionary Amt"));
        extOrderDetailsPanel.add(m_discretionaryAmt);
        extOrderDetailsPanel.add(new JLabel("Trail Stop Price"));
        extOrderDetailsPanel.add(m_trailStopPrice);
        extOrderDetailsPanel.add(new JLabel("Trailing Percent"));
        extOrderDetailsPanel.add(m_trailingPercent);
        extOrderDetailsPanel.add(new JLabel("Institutional Short Sale Slot"));
        extOrderDetailsPanel.add(m_shortSaleSlot);
        extOrderDetailsPanel.add(new JLabel("Institutional Designated Location"));
        extOrderDetailsPanel.add(m_designatedLocation);
        extOrderDetailsPanel.add(new JLabel("Exempt Code"));
        extOrderDetailsPanel.add(m_exemptCode);
        extOrderDetailsPanel.add(new JLabel("Rule 80 A"));
        extOrderDetailsPanel.add(m_rule80A);

        extOrderDetailsPanel.add(new JLabel("All or None"));
        extOrderDetailsPanel.add(m_allOrNone);
        extOrderDetailsPanel.add(new JLabel("Override Percentage Constraints"));
        extOrderDetailsPanel.add(m_overridePercentageConstraints);
        extOrderDetailsPanel.add(new JLabel("Minimum Quantity"));
        extOrderDetailsPanel.add(m_minQty);
        extOrderDetailsPanel.add(new JLabel("Percent Offset"));
        extOrderDetailsPanel.add(m_percentOffset);
        extOrderDetailsPanel.add(new JLabel(""));
        extOrderDetailsPanel.add(new JLabel(""));
        extOrderDetailsPanel.add(new JLabel("BOX: Auction Strategy"));
        extOrderDetailsPanel.add(m_auctionStrategy);
        extOrderDetailsPanel.add(new JLabel("BOX: Starting Price"));
        extOrderDetailsPanel.add(m_startingPrice);
        extOrderDetailsPanel.add(new JLabel("BOX: Stock Reference Price"));
        extOrderDetailsPanel.add(m_stockRefPrice);
        extOrderDetailsPanel.add(new JLabel("BOX: Delta"));
        extOrderDetailsPanel.add(m_delta);
        extOrderDetailsPanel.add(new JLabel("BOX or VOL: Stock Range Lower"));
        extOrderDetailsPanel.add(m_BOXstockRangeLower);
        extOrderDetailsPanel.add(new JLabel("BOX or VOL: Stock Range Upper"));
        extOrderDetailsPanel.add(m_BOXstockRangeUpper);

        extOrderDetailsPanel.add(new JLabel("VOL: Volatility"));
        extOrderDetailsPanel.add(m_VOLVolatility);
        extOrderDetailsPanel.add(new JLabel("VOL: Volatility Type (1 or 2)"));
        extOrderDetailsPanel.add(m_VOLVolatilityType);
        extOrderDetailsPanel.add(new JLabel("VOL: Hedge Delta Order Type"));
        extOrderDetailsPanel.add(m_VOLDeltaNeutralOrderType);
        extOrderDetailsPanel.add(new JLabel("VOL: Hedge Delta Aux Price"));
        extOrderDetailsPanel.add(m_VOLDeltaNeutralAuxPrice);
        extOrderDetailsPanel.add(new JLabel("VOL: Hedge Delta Contract Id"));
        extOrderDetailsPanel.add(m_VOLDeltaNeutralConId);
        extOrderDetailsPanel.add(new JLabel("VOL: Hedge Delta Settling Firm"));
        extOrderDetailsPanel.add(m_VOLDeltaNeutralSettlingFirm);
        extOrderDetailsPanel.add(new JLabel("VOL: Hedge Delta Clearing Account"));
        extOrderDetailsPanel.add(m_VOLDeltaNeutralClearingAccount);
        extOrderDetailsPanel.add(new JLabel("VOL: Hedge Delta Clearing Intent"));
        extOrderDetailsPanel.add(m_VOLDeltaNeutralClearingIntent);
        extOrderDetailsPanel.add(new JLabel("VOL: Hedge Delta Open Close"));
        extOrderDetailsPanel.add(m_VOLDeltaNeutralOpenClose);
        extOrderDetailsPanel.add(m_VOLDeltaNeutralShortSale);
        extOrderDetailsPanel.add(new JLabel(""));
        extOrderDetailsPanel.add(new JLabel("VOL: Hedge Delta Short Sale Slot"));
        extOrderDetailsPanel.add(m_VOLDeltaNeutralShortSaleSlot);
        extOrderDetailsPanel.add(new JLabel("VOL: Hedge Delta Designated Location"));
        extOrderDetailsPanel.add(m_VOLDeltaNeutralDesignatedLocation);
        extOrderDetailsPanel.add(new JLabel("VOL: Continuously Update Price (0 or 1)"));
        extOrderDetailsPanel.add(m_VOLContinuousUpdate);
        extOrderDetailsPanel.add(new JLabel("VOL: Reference Price Type (1 or 2)"));
        extOrderDetailsPanel.add(m_VOLReferencePriceType);

        extOrderDetailsPanel.add(new JLabel("SCALE: Init Level Size"));
        extOrderDetailsPanel.add(m_scaleInitLevelSize);
        extOrderDetailsPanel.add(new JLabel("SCALE: Subs Level Size"));
        extOrderDetailsPanel.add(m_scaleSubsLevelSize);
        extOrderDetailsPanel.add(new JLabel("SCALE: Price Increment"));
        extOrderDetailsPanel.add(m_scalePriceIncrement);
        extOrderDetailsPanel.add(new JLabel("SCALE: Price Adjust Value"));
        extOrderDetailsPanel.add(m_scalePriceAdjustValue);
        extOrderDetailsPanel.add(new JLabel("SCALE: Price Adjust Interval"));
        extOrderDetailsPanel.add(m_scalePriceAdjustInterval);
        extOrderDetailsPanel.add(new JLabel("SCALE: Profit Offset"));
        extOrderDetailsPanel.add(m_scaleProfitOffset);
        extOrderDetailsPanel.add(m_scaleAutoReset);
        extOrderDetailsPanel.add(new JLabel(""));
        extOrderDetailsPanel.add(new JLabel("SCALE: Init Position"));
        extOrderDetailsPanel.add(m_scaleInitPosition);
        extOrderDetailsPanel.add(new JLabel("SCALE: Init Fill Qty"));
        extOrderDetailsPanel.add(m_scaleInitFillQty);
        extOrderDetailsPanel.add(m_scaleRandomPercent);
        extOrderDetailsPanel.add(new JLabel(""));
        extOrderDetailsPanel.add(new JLabel("SCALE: Scale Table"));
        extOrderDetailsPanel.add(m_scaleTable);

        extOrderDetailsPanel.add(new JLabel("HEDGE: Type"));
        extOrderDetailsPanel.add(m_hedgeType);
        extOrderDetailsPanel.add(new JLabel("HEDGE: Param"));
        extOrderDetailsPanel.add(m_hedgeParam);
        extOrderDetailsPanel.add(m_optOutSmartRoutingCheckBox);
        extOrderDetailsPanel.add(m_solicited);
        extOrderDetailsPanel.add(m_randomizeSize);
        extOrderDetailsPanel.add(m_randomizePrice);

        extOrderDetailsPanel.add(new JLabel("MiFID II Decision Maker"));
        extOrderDetailsPanel.add(m_mifid2DecisionMaker);
        extOrderDetailsPanel.add(new JLabel("MiFID II Decision Algo"));
        extOrderDetailsPanel.add(m_mifid2DecisionAlgo);

        extOrderDetailsPanel.add(new JLabel("MiFID II Execution Trader"));
        extOrderDetailsPanel.add(m_mifid2ExecutionTrader);
        extOrderDetailsPanel.add(new JLabel("MiFID II Execution Algo"));
        extOrderDetailsPanel.add(m_mifid2ExecutionAlgo);

        extOrderDetailsPanel.add(new JLabel("Advanced error override"));
        extOrderDetailsPanel.add(m_advancedErrorOverride);
        extOrderDetailsPanel.add(m_dontUseAutoPriceForHedge);
        extOrderDetailsPanel.add(m_isOmsConainer);
        extOrderDetailsPanel.add(m_discretionaryUpToLimitPrice);
        extOrderDetailsPanel.add(m_notHeld);
        extOrderDetailsPanel.add(m_autoCancelParent);
        extOrderDetailsPanel.add(new JLabel(""));
        extOrderDetailsPanel.add(new JLabel("Manual Order Time"));
        extOrderDetailsPanel.add(m_manualOrderTime);
        extOrderDetailsPanel.add(new JLabel("Manual Order Cancel Time"));
        extOrderDetailsPanel.add(m_manualOrderCancelTime);
        extOrderDetailsPanel.add(new JLabel(""));
        extOrderDetailsPanel.add(new JLabel(""));
        extOrderDetailsPanel.add(new JLabel("Min Trade Qty"));
        extOrderDetailsPanel.add(m_minTradeQty);
        extOrderDetailsPanel.add(new JLabel("Min Compete Size"));
        extOrderDetailsPanel.add(m_minCompeteSize);
        extOrderDetailsPanel.add(new JLabel("Compete Against Best Offset"));
        extOrderDetailsPanel.add(m_competeAgainstBestOffset);
        extOrderDetailsPanel.add(m_competeAgainstBestOffsetUpToMid);
        extOrderDetailsPanel.add(new JLabel(""));
        extOrderDetailsPanel.add(new JLabel("Mid Offset At Whole"));
        extOrderDetailsPanel.add(m_midOffsetAtWhole);
        extOrderDetailsPanel.add(new JLabel("Mid Offset At Half"));
        extOrderDetailsPanel.add(m_midOffsetAtHalf);

        // add listeners
        m_competeAgainstBestOffsetUpToMid.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                m_competeAgainstBestOffset.setEnabled(e.getStateChange() != ItemEvent.SELECTED);
            }
        });

        // create button panel
        JPanel buttonPanel = new JPanel();
        JButton btnOk = new JButton("OK");
        buttonPanel.add(btnOk);
        JButton btnCancel = new JButton("Cancel");
        buttonPanel.add(btnCancel);

        // create action listeners
        btnOk.addActionListener(e -> onOk());
        btnCancel.addActionListener(e -> onCancel());

        // create dlg box
        getContentPane().add(extOrderDetailsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        JScrollPane scroller = new JScrollPane(extOrderDetailsPanel);
        this.add(scroller, BorderLayout.CENTER);

        pack();
    }

    void onOk() {
        m_rc = false;

        try {
            // set extended order fields
            m_order.tif(m_tif.getText().trim());
            m_order.duration(parseMaxInt(m_duration));
            m_order.postToAts(parseMaxInt(m_postToAts));
            m_order.activeStartTime(m_activeStartTime.getText().trim());
            m_order.activeStopTime(m_activeStopTime.getText().trim());
            m_order.ocaGroup(m_ocaGroup.getText().trim());
            m_order.ocaType(parseInt(m_ocaType));

            m_order.account(m_account.getText().trim());
            m_order.modelCode(m_modelCode.getText().trim());
            m_order.settlingFirm(m_settlingFirm.getText().trim());
            m_order.clearingAccount(m_clearingAccount.getText().trim());
            m_order.clearingIntent(m_clearingIntent.getText().trim());

            m_order.openClose(m_openClose.getText().trim());
            m_order.origin(parseInt(m_origin));
            m_order.orderRef(m_orderRef.getText().trim());
            m_order.parentId(parseInt(m_parentId));
            m_order.transmit(parseInt(m_transmit) != 0);
            m_order.blockOrder(parseInt(m_blockOrder) != 0);
            m_order.sweepToFill(parseInt(m_sweepToFill) != 0);
            m_order.displaySize(parseInt(m_displaySize));
            m_order.triggerMethod(parseInt(m_triggerMethod));
            m_order.outsideRth(parseInt(m_outsideRth) != 0);
            m_order.hidden(parseInt(m_hidden) != 0);
            m_order.discretionaryAmt(parseDouble(m_discretionaryAmt));
            m_order.shortSaleSlot(parseInt(m_shortSaleSlot));
            m_order.designatedLocation(m_designatedLocation.getText().trim());
            m_order.exemptCode(Integer.parseInt(m_exemptCode.getText().length() != 0 ? m_exemptCode.getText() : "-1"));
            m_order.rule80A(m_rule80A.getText().trim());
            m_order.allOrNone(parseInt(m_allOrNone) != 0);
            m_order.minQty(parseMaxInt(m_minQty));
            m_order.overridePercentageConstraints(parseInt(m_overridePercentageConstraints) != 0);
            m_order.percentOffset(parseMaxDouble(m_percentOffset));
            m_order.optOutSmartRouting(m_optOutSmartRoutingCheckBox.isSelected());
            m_order.solicited(m_solicited.isSelected());
            m_order.auctionStrategy(parseInt(m_auctionStrategy));
            m_order.startingPrice(parseMaxDouble(m_startingPrice));
            m_order.stockRefPrice(parseMaxDouble(m_stockRefPrice));
            m_order.delta(parseMaxDouble(m_delta));
            m_order.stockRangeLower(parseMaxDouble(m_BOXstockRangeLower));
            m_order.stockRangeUpper(parseMaxDouble(m_BOXstockRangeUpper));
            m_order.volatility(parseMaxDouble(m_VOLVolatility));
            m_order.volatilityType(parseMaxInt(m_VOLVolatilityType));
            m_order.deltaNeutralOrderType(m_VOLDeltaNeutralOrderType.getText().trim());
            m_order.deltaNeutralAuxPrice(parseMaxDouble(m_VOLDeltaNeutralAuxPrice));
            m_order.deltaNeutralConId(parseInt(m_VOLDeltaNeutralConId));
            m_order.deltaNeutralSettlingFirm(m_VOLDeltaNeutralSettlingFirm.getText().trim());
            m_order.deltaNeutralClearingAccount(m_VOLDeltaNeutralClearingAccount.getText().trim());
            m_order.deltaNeutralClearingIntent(m_VOLDeltaNeutralClearingIntent.getText().trim());
            m_order.deltaNeutralOpenClose(m_VOLDeltaNeutralOpenClose.getText().trim());
            m_order.deltaNeutralShortSale(m_VOLDeltaNeutralShortSale.isSelected());
            m_order.deltaNeutralShortSaleSlot(parseInt(m_VOLDeltaNeutralShortSaleSlot));
            m_order.deltaNeutralDesignatedLocation(m_VOLDeltaNeutralDesignatedLocation.getText().trim());
            m_order.continuousUpdate(parseInt(m_VOLContinuousUpdate));
            m_order.referencePriceType(parseMaxInt(m_VOLReferencePriceType));
            m_order.trailStopPrice(parseMaxDouble(m_trailStopPrice));
            m_order.trailingPercent(parseMaxDouble(m_trailingPercent));

            m_order.scaleInitLevelSize(parseMaxInt(m_scaleInitLevelSize));
            m_order.scaleSubsLevelSize(parseMaxInt(m_scaleSubsLevelSize));
            m_order.scalePriceIncrement(parseMaxDouble(m_scalePriceIncrement));
            m_order.scalePriceAdjustValue(parseMaxDouble(m_scalePriceAdjustValue));
            m_order.scalePriceAdjustInterval(parseMaxInt(m_scalePriceAdjustInterval));
            m_order.scaleProfitOffset(parseMaxDouble(m_scaleProfitOffset));
            m_order.scaleAutoReset(m_scaleAutoReset.isSelected());
            m_order.scaleInitPosition(parseMaxInt(m_scaleInitPosition));
            m_order.scaleInitFillQty(parseMaxInt(m_scaleInitFillQty));
            m_order.scaleRandomPercent(m_scaleRandomPercent.isSelected());
            m_order.scaleTable(m_scaleTable.getText().trim());
            m_order.hedgeType(m_hedgeType.getText().trim());
            m_order.hedgeParam(m_hedgeParam.getText().trim());

            m_order.randomizePrice(m_randomizePrice.isSelected());
            m_order.randomizeSize(m_randomizeSize.isSelected());

            m_order.mifid2DecisionMaker(m_mifid2DecisionMaker.getText());
            m_order.mifid2DecisionAlgo(m_mifid2DecisionAlgo.getText());
            m_order.mifid2ExecutionTrader(m_mifid2ExecutionTrader.getText());
            m_order.mifid2ExecutionAlgo(m_mifid2ExecutionAlgo.getText());
            m_order.dontUseAutoPriceForHedge(m_dontUseAutoPriceForHedge.isSelected());
            m_order.isOmsContainer(m_isOmsConainer.isSelected());
            m_order.discretionaryUpToLimitPrice(m_discretionaryUpToLimitPrice.isSelected());
            m_order.notHeld(m_notHeld.isSelected());
            m_order.autoCancelParent(m_autoCancelParent.isSelected());
            m_order.advancedErrorOverride(m_advancedErrorOverride.getText());
            m_order.manualOrderTime(m_manualOrderTime.getText());
            m_order.minTradeQty(parseMaxInt(m_minTradeQty));
            m_order.minCompeteSize(parseMaxInt(m_minCompeteSize));
            m_order.competeAgainstBestOffset(m_competeAgainstBestOffsetUpToMid.isSelected() ?
                    Order.COMPETE_AGAINST_BEST_OFFSET_UP_TO_MID : parseMaxDouble(m_competeAgainstBestOffset));
            m_order.midOffsetAtWhole(parseMaxDouble(m_midOffsetAtWhole));
            m_order.midOffsetAtHalf(parseMaxDouble(m_midOffsetAtHalf));
        } catch (Exception e) {
            Main.inform(this, "Error - " + e);
            return;
        }

        m_rc = true;
        setVisible(false);
    }

    private static int parseMaxInt(JTextField textField) {
        String text = textField.getText().trim();
        if (text.length() == 0) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(text);
    }

    private static double parseMaxDouble(JTextField textField) {
        String text = textField.getText().trim();
        if (text.length() == 0) {
            return Double.MAX_VALUE;
        }
        return Double.parseDouble(text);
    }

    private static int parseInt(JTextField textField) {
        String text = textField.getText().trim();
        if (text.length() == 0) {
            return 0;
        }
        return Integer.parseInt(text);
    }

    private static double parseDouble(JTextField textField) {
        String text = textField.getText().trim();
        if (text.length() == 0) {
            return 0;
        }
        return Double.parseDouble(text);
    }

    void onCancel() {
        m_rc = false;
        setVisible(false);
    }

    public String manualOrderCancelTime() {
        return m_manualOrderCancelTime.getText();
    }
}
