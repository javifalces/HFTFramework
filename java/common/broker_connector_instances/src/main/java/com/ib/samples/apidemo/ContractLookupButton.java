/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.awt.event.MouseEvent;

import com.ib.client.ContractLookuper;

import com.ib.samples.apidemo.util.HtmlButton;

public abstract class ContractLookupButton extends HtmlButton {

    final ContractSearchDlg m_contractSearchdlg;

    public ContractLookupButton(int conId, String exchange, ContractLookuper lookuper) {
        super("");

        m_contractSearchdlg = new ContractSearchDlg(conId, exchange, lookuper);

        setText(m_contractSearchdlg.refContract() == null ? "search..." : m_contractSearchdlg.refContract());
    }

    @Override
    protected void onClicked(MouseEvent e) {
        m_contractSearchdlg.setLocationRelativeTo(this.getParent());
        m_contractSearchdlg.setVisible(true);

        setText(m_contractSearchdlg.refContract() == null ? "search..." : m_contractSearchdlg.refContract());
        actionPerformed();
        actionPerformed(m_contractSearchdlg.refConId(), m_contractSearchdlg.refExchId());
    }

    protected abstract void actionPerformed(int refConId, String refExchId);
}
