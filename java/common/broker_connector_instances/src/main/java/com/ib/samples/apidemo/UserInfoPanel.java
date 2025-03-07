/* Copyright (C) 2021 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.ib.controller.ApiController.IUserInfoHandler;

import com.ib.samples.apidemo.AccountInfoPanel.Table;
import com.ib.samples.apidemo.util.HtmlButton;
import com.ib.samples.apidemo.util.NewTabbedPanel.NewTabPanel;
import com.ib.samples.apidemo.util.VerticalPanel;


public class UserInfoPanel extends NewTabPanel {
    private UserInfoModel m_model = new UserInfoModel();

    UserInfoPanel() {
        HtmlButton requestUserInfoButton = new HtmlButton("Request User Info") {
            protected void actionPerformed() {
                requestUserInfo();
            }
        };

        HtmlButton clearUserInfoButton = new HtmlButton("Clear User Info") {
            protected void actionPerformed() {
                clearUserInfo();
            }
        };

        JPanel buts = new VerticalPanel();
        buts.add(requestUserInfoButton);
        buts.add(clearUserInfoButton);

        JTable table = new Table(m_model, 2);
        JScrollPane scroll = new JScrollPane(table);

        setLayout(new BorderLayout());
        add(scroll);
        add(buts, BorderLayout.EAST);
    }

    /**
     * Called when the tab is first visited.
     */
    @Override
    public void activated() { /* noop */ }

    /**
     * Called when the tab is closed by clicking the X.
     */
    @Override
    public void closed() {
        clearUserInfo();
    }

    private void requestUserInfo() {
        ApiDemo.INSTANCE.controller().reqUserInfo(0, m_model);
    }

    private void clearUserInfo() {
        m_model.clear();
    }

    private class UserInfoModel extends AbstractTableModel implements IUserInfoHandler {
        List<UserInfoRow> m_list = new ArrayList<>();

        @Override
        public void userInfo(int reqId, String whiteBrandingId) {
            UserInfoRow row = new UserInfoRow();
            m_list.add(row);
            row.update(whiteBrandingId);
            m_model.fireTableDataChanged();
        }

        public void clear() {
            m_list.clear();
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return m_list.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return "White Branding Id";
                default:
                    return null;
            }
        }

        @Override
        public Object getValueAt(int rowIn, int col) {
            UserInfoRow row = m_list.get(rowIn);

            switch (col) {
                case 0:
                    return row.m_whiteBrandingId;
                default:
                    return null;
            }
        }
    }

    private static class UserInfoRow {
        String m_whiteBrandingId;

        void update(String whiteBrandingId) {
            m_whiteBrandingId = whiteBrandingId;
        }
    }
}
