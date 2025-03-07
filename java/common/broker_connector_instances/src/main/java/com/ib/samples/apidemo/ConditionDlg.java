/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ib.samples.apidemo;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ib.client.ContractLookuper;
import com.ib.client.ExecutionCondition;
import com.ib.client.MarginCondition;
import com.ib.client.OrderCondition;
import com.ib.client.OrderConditionType;
import com.ib.client.PercentChangeCondition;
import com.ib.client.PriceCondition;
import com.ib.client.TimeCondition;
import com.ib.client.VolumeCondition;

import com.ib.samples.apidemo.util.HtmlButton;

/**
 * @author mvorobyev
 */
public class ConditionDlg extends JDialog implements ChangeListener, ActionListener {

    private JPanel m_conditionPanel;
    private JPanel m_conditionTypePanel;
    private JRadioButton m_rbMargin;
    private JRadioButton m_rbPercent;
    private JRadioButton m_rbPrice;
    private JRadioButton m_rbTime;
    private JRadioButton m_rbTrade;
    private JRadioButton m_rbVolume;
    private OrderCondition m_condition;
    private OnOKPanel m_conditionSubPanel;

    private ContractLookuper m_lookuper;

    ConditionDlg(OrderCondition condition, ContractLookuper lookuper) {
        initComponents();

        m_condition = condition;
        m_lookuper = lookuper;

        switch (m_condition.type()) {
            case Execution:
                m_rbTrade.setSelected(true);
                break;

            case Margin:
                m_rbMargin.setSelected(true);
                break;

            case PercentChange:
                m_rbPercent.setSelected(true);
                break;

            case Price:
                m_rbPrice.setSelected(true);
                break;

            case Time:
                m_rbTime.setSelected(true);
                break;

            case Volume:
                m_rbVolume.setSelected(true);
                break;
        }
    }

    private boolean m_isCanceled;

    public boolean isCanceled() {
        return m_isCanceled;
    }

    @Override
    public void setVisible(boolean arg0) {
        m_isCanceled = true;

        super.setVisible(arg0);
    }

    private void initComponents() {

        JTabbedPane tabbedPane = new JTabbedPane();
        m_conditionTypePanel = new JPanel();
        m_rbPrice = new JRadioButton();
        m_rbMargin = new JRadioButton();
        m_rbTrade = new JRadioButton();
        m_rbTime = new JRadioButton();
        m_rbVolume = new JRadioButton();
        m_rbPercent = new JRadioButton();
        m_conditionPanel = new JPanel();

        ButtonGroup group = new ButtonGroup();

        group.add(m_rbMargin);
        group.add(m_rbPrice);
        group.add(m_rbPercent);
        group.add(m_rbTime);
        group.add(m_rbTrade);
        group.add(m_rbVolume);

        m_rbPrice.setText("Price");
        m_rbPrice.addChangeListener(this);

        m_rbMargin.setText("Margin Cushion");
        m_rbMargin.addChangeListener(this);

        m_rbTrade.setText("Trade");
        m_rbTrade.addChangeListener(this);

        m_rbTime.setText("Time");
        m_rbTime.addChangeListener(this);

        m_rbVolume.setText("Volume");
        m_rbVolume.addChangeListener(this);

        m_rbPercent.setText("Percent Change");
        m_rbPercent.addChangeListener(this);

        GroupLayout jConditionTypePanelLayout = new GroupLayout(m_conditionTypePanel);
        m_conditionTypePanel.setLayout(jConditionTypePanelLayout);
        jConditionTypePanelLayout.setHorizontalGroup(
                jConditionTypePanelLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(jConditionTypePanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jConditionTypePanelLayout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(m_rbTrade)
                                        .addComponent(m_rbMargin)
                                        .addComponent(m_rbPrice))
                                .addPreferredGap(ComponentPlacement.RELATED, 31, Short.MAX_VALUE)
                                .addGroup(jConditionTypePanelLayout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(m_rbVolume)
                                        .addComponent(m_rbPercent)
                                        .addComponent(m_rbTime))
                                .addContainerGap())
        );
        jConditionTypePanelLayout.setVerticalGroup(
                jConditionTypePanelLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(jConditionTypePanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jConditionTypePanelLayout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(m_rbPrice)
                                        .addComponent(m_rbTime))
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addGroup(jConditionTypePanelLayout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(m_rbMargin)
                                        .addComponent(m_rbVolume))
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addGroup(jConditionTypePanelLayout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(m_rbTrade)
                                        .addComponent(m_rbPercent))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabbedPane.addTab("Condition type", m_conditionTypePanel);
        tabbedPane.addTab("Condition", m_conditionPanel);

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        GroupLayout layout = new GroupLayout(getContentPane());

        mainPanel.add(tabbedPane);
        buttons.add(
                new HtmlButton("Apply") {
                    protected void actionPerformed() {
                        m_isCanceled = false;

                        m_conditionSubPanel.onOK();
                        dispose();
                    }
                });
        mainPanel.add(buttons, BorderLayout.SOUTH);

        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(mainPanel)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(mainPanel)
        );

        pack();
        setModalityType(ModalityType.APPLICATION_MODAL);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        m_conditionPanel.removeAll();

        if (m_rbMargin.isSelected()) {
            m_conditionSubPanel = new MarginConditionPanel((MarginCondition) instantiateCondition(MarginCondition.conditionType));
        }

        if (m_rbPercent.isSelected()) {
            m_conditionSubPanel = new PercentConditionPanel((PercentChangeCondition) instantiateCondition(PercentChangeCondition.conditionType), m_lookuper);
        }

        if (m_rbPrice.isSelected()) {
            m_conditionSubPanel = new PriceConditionPanel((PriceCondition) instantiateCondition(PriceCondition.conditionType), m_lookuper);
        }

        if (m_rbTime.isSelected()) {
            m_conditionSubPanel = new TimeConditionPanel((TimeCondition) instantiateCondition(TimeCondition.conditionType));
        }

        if (m_rbTrade.isSelected()) {
            m_conditionSubPanel = new TradeConditionPanel((ExecutionCondition) instantiateCondition(ExecutionCondition.conditionType));
        }

        if (m_rbVolume.isSelected()) {
            m_conditionSubPanel = new VolumeConditionPanel((VolumeCondition) instantiateCondition(VolumeCondition.conditionType), m_lookuper);
        }

        m_conditionPanel.add(m_conditionSubPanel);
        pack();
    }

    private OrderCondition instantiateCondition(OrderConditionType type) {
        if (m_condition.type() != type) {
            m_condition = OrderCondition.create(type);
        }
        return m_condition;
    }

    public OrderCondition condition() {
        return m_condition;
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub

    }

}
