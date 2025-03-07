/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.ContractLookuper;

import com.ib.samples.apidemo.util.HtmlButton;
import com.ib.samples.apidemo.util.Util;

public class ContractSearchDlg extends JDialog {

    private final ContractPanel m_contractPanel;
    private Contract m_contract = new Contract();
    private final DefaultListModel<Contract> m_contractList = new DefaultListModel<>();
    private final JList<Contract> m_contracts = new JList<>(m_contractList);
    private final ContractLookuper m_lookuper;

    private List<ContractDetails> lookupContract() {
        //return com.ib.client.Util.lookupContract(ApiDemo.INSTANCE.controller(), m_contract);
        return m_lookuper.lookupContract(m_contract);
    }

    ContractSearchDlg(int conId, String exchange, ContractLookuper lookuper) {
        m_lookuper = lookuper;

        if (conId > 0 && !exchange.isEmpty()) {
            m_contract.conid(conId);
            m_contract.exchange(exchange);

            List<ContractDetails> list = lookupContract();

            if (!list.isEmpty()) {
                m_contract = list.get(0).contract();
            }
        }

        m_contractPanel = new ContractPanel(m_contract);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));

        buttons.add(new HtmlButton("search") {
            @Override
            protected void actionPerformed() {
                onSearch();
            }
        });

        m_contracts.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> onGetListCellRenderer(value, isSelected));

        m_contracts.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    dispose();
                }
            }
        });

        add(m_contractPanel, BorderLayout.NORTH);
        add(buttons, BorderLayout.CENTER);
        add(m_contracts, BorderLayout.SOUTH);

        pack();
        setModalityType(ModalityType.APPLICATION_MODAL);
        Util.closeOnEsc(this);
    }

    public boolean isSucceeded() {
        return m_contractList.size() > 0 && m_contracts.getSelectedIndex() >= 0;
    }

    private Contract selectedContract() {
        if (m_contractList.isEmpty() || m_contracts.getSelectedIndex() < 0)
            return m_contract;

        return (m_contractList.get(m_contracts.getSelectedIndex()));
    }

    int refConId() {
        return selectedContract().conid();
    }

    String refExchId() {
        return selectedContract().exchange();
    }

    String refContract() {
        if (selectedContract().conid() == 0 || selectedContract().exchange().isEmpty())
            return null;

        return selectedContract().symbol() + " " + selectedContract().exchange() + " " + selectedContract().secType() + " " + selectedContract().currency();
    }

    private void onSearch() {
        m_contractPanel.onOK();

        m_contract.conid(0);
        m_contract.tradingClass(null);

        List<ContractDetails> list = lookupContract();

        m_contractList.clear();

        for (ContractDetails el : list) {
            m_contractList.addElement(el.contract());
        }

        m_contracts.updateUI();
        pack();
    }

    private Component onGetListCellRenderer(Contract value, boolean isSelected) {
        JPanel panel = new JPanel();

        panel.add(new JLabel(value.symbol()));
        panel.add(new JLabel(value.exchange()));
        panel.add(new JLabel(value.secType().toString()));
        panel.add(new JLabel(value.currency()));

        if (isSelected) {
            panel.setBackground(Color.LIGHT_GRAY);
        }

        return panel;
    }
}
