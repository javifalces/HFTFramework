/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import static com.ib.controller.Formats.fmt0;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.*;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import com.ib.client.Types.SecType;
import com.ib.client.Util;
import com.ib.controller.ApiController.IAccountHandler;
import com.ib.controller.MarketValueTag;
import com.ib.controller.Position;

import com.ib.samples.apidemo.util.NewTabbedPanel;
import com.ib.samples.apidemo.util.NewTabbedPanel.INewTab;

public class AccountInfoPanel extends JPanel implements INewTab, IAccountHandler {
    private DefaultListModel<String> m_acctList = new DefaultListModel<>();
    private JList<String> m_accounts = new JList<>(m_acctList);
    private String m_selAcct = "";
    private MarginModel m_marginModel = new MarginModel();
    private PortfolioModel m_portfolioModel = new PortfolioModel();
    private MktValModel m_mktValModel = new MktValModel();
    private JLabel m_lastUpdated = new JLabel();

    AccountInfoPanel() {
        m_lastUpdated.setHorizontalAlignment(SwingConstants.RIGHT);

        m_accounts.setPreferredSize(new Dimension(10000, 100));
        JScrollPane acctScroll = new JScrollPane(m_accounts);
        acctScroll.setBorder(new TitledBorder("Select Account"));

        JScrollPane marginScroll = new JScrollPane(new Table(m_marginModel));
        JScrollPane mvScroll = new JScrollPane(new Table(m_mktValModel, 2));
        JScrollPane portScroll = new JScrollPane(new Table(m_portfolioModel));

        NewTabbedPanel tabbedPanel = new NewTabbedPanel();
        tabbedPanel.addTab("Balances and Margin", marginScroll);
        tabbedPanel.addTab("Market Value", mvScroll);
        tabbedPanel.addTab("Portfolio", portScroll);
        tabbedPanel.addTab("Account Summary", new AccountSummaryPanel());
        tabbedPanel.addTab("Market Value Summary", new MarketValueSummaryPanel());
        tabbedPanel.addTab("Positions (all accounts)", new PositionsPanel());
        tabbedPanel.addTab("Family Codes", new FamilyCodesPanel());
        tabbedPanel.addTab("User Info", new UserInfoPanel());

        setLayout(new BorderLayout());
        add(acctScroll, BorderLayout.NORTH);
        add(tabbedPanel);
        add(m_lastUpdated, BorderLayout.SOUTH);

        m_accounts.addListSelectionListener(e -> onChanged());
    }

    /**
     * Called when the tab is first visited.
     */
    @Override
    public void activated() {
        for (String account : ApiDemo.INSTANCE.accountList()) {
            m_acctList.addElement(account);
        }

        if (ApiDemo.INSTANCE.accountList().size() == 1) {
            m_accounts.setSelectedIndex(0);
        }
    }

    /**
     * Called when the tab is closed by clicking the X.
     */
    @Override
    public void closed() {
    }

    protected synchronized void onChanged() {
        int i = m_accounts.getSelectedIndex();
        if (i != -1) {
            String selAcct = m_acctList.get(i);
            if (!selAcct.equals(m_selAcct)) {
                m_selAcct = selAcct;
                m_marginModel.clear();
                m_mktValModel.clear();
                m_portfolioModel.clear();
                ApiDemo.INSTANCE.controller().reqAccountUpdates(true, m_selAcct, this);
            }
        }
    }

    /**
     * Receive account value.
     */
    public synchronized void accountValue(String account, String tag, String value, String currency) {
        if (account.equals(m_selAcct)) {
            try {
                MarketValueTag mvTag = MarketValueTag.valueOf(tag);
                m_mktValModel.handle(account, currency, mvTag, value);
            } catch (Exception e) {
                m_marginModel.handle(tag, value, currency, account);
            }
        }
    }

    /**
     * Receive position.
     */
    public synchronized void updatePortfolio(Position position) {
        if (position.account().equals(m_selAcct)) {
            m_portfolioModel.update(position);
        }
    }

    /**
     * Receive time of last update.
     */
    public void accountTime(String timeStamp) {
        m_lastUpdated.setText("Last updated: " + timeStamp + "       ");
    }

    public void accountDownloadEnd(String account) {
    }

    private static class MarginModel extends AbstractTableModel {
        Map<MarginRowKey, MarginRow> m_map = new HashMap<>();
        List<MarginRow> m_list = new ArrayList<>();

        void clear() {
            m_map.clear();
            m_list.clear();
        }

        public void handle(String tag, String value, String currency, String account) {
            // useless
            if (tag.equals("Currency")) {
                return;
            }

            int type = 0; // 0=whole acct; 1=securities; 2=commodities

            // "Securities" segment?
            if (tag.endsWith("-S")) {
                tag = tag.substring(0, tag.length() - 2);
                type = 1;
            }

            // "Commodities" segment?
            else if (tag.endsWith("-C")) {
                tag = tag.substring(0, tag.length() - 2);
                type = 2;
            }

            MarginRowKey key = new MarginRowKey(tag, currency);
            MarginRow row = m_map.get(key);

            if (row == null) {
                // don't add new rows with a value of zero
                if (isZero(value)) {
                    return;
                }

                row = new MarginRow(tag, currency);
                m_map.put(key, row);
                m_list.add(row);
                Collections.sort(m_list);
            }

            switch (type) {
                case 0:
                    row.m_val = value;
                    break;
                case 1:
                    row.m_secVal = value;
                    break;
                case 2:
                    row.m_comVal = value;
                    break;
                default:
                    row.m_val = value;
                    break;
            }

            SwingUtilities.invokeLater(this::fireTableDataChanged);
        }

        @Override
        public int getRowCount() {
            return m_list.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return "Tag";
                case 1:
                    return "Account Value";
                case 2:
                    return "Securities Value";
                case 3:
                    return "Commodities Value";
                default:
                    return null;
            }
        }

        @Override
        public Object getValueAt(int rowIn, int col) {
            MarginRow row = m_list.get(rowIn);

            switch (col) {
                case 0:
                    return row.m_tag;
                case 1:
                    return format(row.m_val, row.m_currency);
                case 2:
                    return format(row.m_secVal, row.m_currency);
                case 3:
                    return format(row.m_comVal, row.m_currency);
                default:
                    return null;
            }
        }
    }

    private static class MarginRow implements Comparable<MarginRow> {
        String m_tag;
        String m_currency;
        String m_val;
        String m_secVal;
        String m_comVal;

        MarginRow(String tag, String cur) {
            m_tag = tag;
            m_currency = cur;
        }

        @Override
        public int compareTo(MarginRow o) {
            return m_tag.compareTo(o.m_tag);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof MarginRow && compareTo((MarginRow) obj) == 0);
        }

        @Override
        public int hashCode() {
            return m_tag != null ? m_tag.hashCode() : 0;
        }
    }

    private static class MarginRowKey {
        String m_tag;
        String m_currency;

        MarginRowKey(String key, String currency) {
            m_tag = key;
            m_currency = currency;
        }

        @Override
        public int hashCode() {
            int cur = m_currency != null ? m_currency.hashCode() : 0;
            return m_tag.hashCode() + cur;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MarginRowKey)) {
                return false;
            }
            MarginRowKey other = (MarginRowKey) obj;
            return m_tag.equals(other.m_tag) && Objects.equals(m_currency, other.m_currency);
        }
    }

    static class MktValModel extends AbstractTableModel {
        private Map<String, MktValRow> m_map = new HashMap<>();
        private List<MktValRow> m_list = new ArrayList<>();

        void handle(String account, String currency, MarketValueTag mvTag, String value) {
            String key = account + currency;
            MktValRow row = m_map.get(key);
            if (row == null) {
                row = new MktValRow(account, currency);
                m_map.put(key, row);
                m_list.add(row);
            }
            row.set(mvTag, value);
            fireTableDataChanged();
        }

        void clear() {
            m_map.clear();
            m_list.clear();
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return m_list.size();
        }

        @Override
        public int getColumnCount() {
            return MarketValueTag.values().length + 2;
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return "Account";
                case 1:
                    return "Currency";
                default:
                    return MarketValueTag.get(col - 2).toString();
            }
        }

        @Override
        public Object getValueAt(int rowIn, int col) {
            MktValRow row = m_list.get(rowIn);
            switch (col) {
                case 0:
                    return row.m_account;
                case 1:
                    return row.m_currency;
                default:
                    return format(row.get(MarketValueTag.get(col - 2)), null);
            }
        }
    }

    private static class MktValRow {
        String m_account;
        String m_currency;
        Map<MarketValueTag, String> m_map = new HashMap<>();

        MktValRow(String account, String currency) {
            m_account = account;
            m_currency = currency;
        }

        public String get(MarketValueTag tag) {
            return m_map.get(tag);
        }

        public void set(MarketValueTag tag, String value) {
            m_map.put(tag, value);
        }
    }

    /**
     * Shared with ExercisePanel.
     */
    static class PortfolioModel extends AbstractTableModel {
        private Map<Integer, Position> m_portfolioMap = new HashMap<>();
        private List<Integer> m_positions = new ArrayList<>(); // must store key because Position is overwritten

        void clear() {
            m_positions.clear();
            m_portfolioMap.clear();
        }

        Position getPosition(int i) {
            return m_portfolioMap.get(m_positions.get(i));
        }

        public void update(Position position) {
            // skip fake FX positions
            if (position.contract().secType() == SecType.CASH) {
                return;
            }

            if (!m_portfolioMap.containsKey(position.conid()) && !position.position().isZero()) {
                m_positions.add(position.conid());
            }
            m_portfolioMap.put(position.conid(), position);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return m_positions.size();
        }

        @Override
        public int getColumnCount() {
            return 7;
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return "Description";
                case 1:
                    return "Position";
                case 2:
                    return "Price";
                case 3:
                    return "Value";
                case 4:
                    return "Avg Cost";
                case 5:
                    return "Unreal Pnl";
                case 6:
                    return "Real Pnl";
                default:
                    return null;
            }
        }

        @Override
        public Object getValueAt(int row, int col) {
            Position pos = getPosition(row);
            switch (col) {
                case 0:
                    return pos.contract().textDescription();
                case 1:
                    return pos.position();
                case 2:
                    return Util.DoubleMaxString(pos.marketPrice());
                case 3:
                    return format("" + pos.marketValue(), null);
                case 4:
                    return Util.DoubleMaxString(pos.averageCost());
                case 5:
                    return Util.DoubleMaxString(pos.unrealPnl());
                case 6:
                    return Util.DoubleMaxString(pos.realPnl());
                default:
                    return null;
            }
        }
    }

    private static boolean isZero(String value) {
        try {
            return Double.parseDouble(value) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * If val is a number, format it with commas and no decimals.
     */
    static String format(String val, String currency) {
        if (val == null || val.length() == 0) {
            return null;
        }

        try {
            double dub = Double.parseDouble(val);
            val = fmt0(dub);
        } catch (Exception ignored) {
        }

        return currency != null && currency.length() > 0
                ? val + " " + currency : val;
    }

    /**
     * Table where first n columns are left-justified, all other columns are right-justified.
     */
    static class Table extends JTable {
        private int m_n;

        public Table(AbstractTableModel model) {
            this(model, 1);
        }

        public Table(AbstractTableModel model, int n) {
            super(model);
            m_n = n;
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int col) {
            TableCellRenderer rend = super.getCellRenderer(row, col);
            ((JLabel) rend).setHorizontalAlignment(col < m_n ? SwingConstants.LEFT : SwingConstants.RIGHT);
            return rend;
        }
    }
}
