/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.PriceIncrement;
import com.ib.client.Types.FundamentalType;
import com.ib.controller.ApiController.IContractDetailsHandler;
import com.ib.controller.ApiController.IFundamentalsHandler;
import com.ib.controller.ApiController.IMarketRuleHandler;

import com.ib.samples.apidemo.util.HtmlButton;
import com.ib.samples.apidemo.util.NewTabbedPanel;
import com.ib.samples.apidemo.util.NewTabbedPanel.INewTab;
import com.ib.samples.apidemo.util.TCombo;
import com.ib.samples.apidemo.util.VerticalPanel;

class ContractInfoPanel extends JPanel {
    private final Contract m_contract = new Contract();
    private final NewTabbedPanel m_resultsPanels = new NewTabbedPanel();
    private static Set<Integer> m_marketRuleIds = new HashSet<>();
    private final MarketRuleRequestPanel m_marketRuleRequestPanel = new MarketRuleRequestPanel();

    ContractInfoPanel() {
        NewTabbedPanel m_requestPanels = new NewTabbedPanel();
        m_requestPanels.addTab("Contract details", new DetailsRequestPanel());
        m_requestPanels.addTab("Fundamentals", new FundaRequestPanel());
        m_requestPanels.addTab("Market Rules", m_marketRuleRequestPanel);

        setLayout(new BorderLayout());
        add(m_requestPanels, BorderLayout.NORTH);
        add(m_resultsPanels);
    }

    class DetailsRequestPanel extends JPanel {
        ContractPanel m_contractPanel = new ContractPanel(m_contract);

        DetailsRequestPanel() {
            HtmlButton but = new HtmlButton("Query") {
                @Override
                protected void actionPerformed() {
                    onQuery();
                }
            };

            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(m_contractPanel);
            add(Box.createHorizontalStrut(20));
            add(but);
        }

        void onQuery() {
            m_contractPanel.onOK();

            DetailsResultsPanel panel = new DetailsResultsPanel();
            m_resultsPanels.addTab(m_contract.symbol() + " " + "Description", panel, true, true);
            ApiDemo.INSTANCE.controller().reqContractDetails(m_contract, panel);
        }
    }

    class DetailsResultsPanel extends JPanel implements IContractDetailsHandler {
        JLabel m_label = new JLabel();
        JTextArea m_text = new JTextArea();

        DetailsResultsPanel() {
            JScrollPane scroll = new JScrollPane(m_text);

            setLayout(new BorderLayout());
            add(m_label, BorderLayout.NORTH);
            add(scroll);
        }

        @Override
        public void contractDetails(List<ContractDetails> list) {
            // set label
            if (list.size() == 0) {
                m_label.setText("No matching contracts were found");
            } else if (list.size() > 1) {
                m_label.setText(list.size() + " contracts returned; showing first contract only");
            } else {
                m_label.setText(null);
            }

            // set text
            if (list.size() == 0) {
                m_text.setText(null);
            } else {
                m_text.setText(list.get(0).toString());
            }
            if (list.size() > 0 && list.get(0).marketRuleIds() != null) {
                for (String s : list.get(0).marketRuleIds().split(",")) {
                    m_marketRuleIds.add(Integer.parseInt(s));
                }
                m_marketRuleRequestPanel.m_marketRuleIdCombo.setModel(new DefaultComboBoxModel<>(m_marketRuleIds.toArray(new Integer[m_marketRuleIds.size()])));
            }
        }
    }

    public class FundaRequestPanel extends JPanel {
        ContractPanel m_contractPanel = new ContractPanel(m_contract);
        TCombo<FundamentalType> m_type = new TCombo<>(FundamentalType.values());

        FundaRequestPanel() {
            HtmlButton but = new HtmlButton("Query") {
                @Override
                protected void actionPerformed() {
                    onQuery();
                }
            };

            VerticalPanel rightPanel = new VerticalPanel();
            rightPanel.add("Report type", m_type);

            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(m_contractPanel);
            add(Box.createHorizontalStrut(20));
            add(rightPanel);
            add(Box.createHorizontalStrut(10));
            add(but);
        }

        void onQuery() {
            m_contractPanel.onOK();
            FundaResultPanel panel = new FundaResultPanel();
            FundamentalType type = m_type.getSelectedItem();
            m_resultsPanels.addTab(m_contract.symbol() + " " + type, panel, true, true);
            ApiDemo.INSTANCE.controller().reqFundamentals(m_contract, type, panel);
        }
    }

    class FundaResultPanel extends JPanel implements INewTab, IFundamentalsHandler {
        String m_data;
        JTextArea m_text = new JTextArea();

        FundaResultPanel() {
            HtmlButton b = new HtmlButton("View in browser") {
                @Override
                protected void actionPerformed() {
                    onView();
                }
            };

            JScrollPane scroll = new JScrollPane(m_text);
            setLayout(new BorderLayout());
            add(scroll);
            add(b, BorderLayout.EAST);
        }

        void onView() {
            try {
                File file = File.createTempFile("tws", ".xml");
                try (PrintStream ps = new PrintStream(file, "UTF-8")) {
                    ps.println(m_text.getText());
                }
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Called when the tab is first visited.
         */
        @Override
        public void activated() {
            ApiDemo.INSTANCE.controller().reqFundamentals(m_contract, FundamentalType.ReportRatios, this);
        }

        /**
         * Called when the tab is closed by clicking the X.
         */
        @Override
        public void closed() {
        }

        @Override
        public void fundamentals(String str) {
            m_data = str;
            m_text.setText(str);
        }
    }

    class MarketRuleRequestPanel extends JPanel {
        JComboBox<Integer> m_marketRuleIdCombo = new JComboBox<>();

        MarketRuleRequestPanel() {
            m_marketRuleIdCombo.setPreferredSize(new Dimension(130, 20));
            m_marketRuleIdCombo.setEditable(true);

            HtmlButton but = new HtmlButton("Request Market Rule") {
                @Override
                protected void actionPerformed() {
                    onRequestMarketRule();
                }
            };

            VerticalPanel paramsPanel = new VerticalPanel();
            paramsPanel.add("Market Rule Id", m_marketRuleIdCombo, Box.createHorizontalStrut(100), but);
            setLayout(new BorderLayout());
            add(paramsPanel, BorderLayout.NORTH);
        }

        void onRequestMarketRule() {
            MarketRuleResultsPanel panel = new MarketRuleResultsPanel();
            final Object item = m_marketRuleIdCombo.getEditor().getItem();
            if (item != null) {
                final String itemString = item.toString();
                if (!itemString.isEmpty()) {
                    try {
                        int marketRuleId = Integer.parseInt(itemString);
                        m_resultsPanels.addTab("Market Rule Id: " + itemString, panel, true, true);
                        ApiDemo.INSTANCE.controller().reqMarketRule(marketRuleId, panel);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    static class MarketRuleResultsPanel extends JPanel implements IMarketRuleHandler {
        JLabel m_label = new JLabel();
        JTextArea m_text = new JTextArea();

        MarketRuleResultsPanel() {
            JScrollPane scroll = new JScrollPane(m_text);

            setLayout(new BorderLayout());
            add(m_label, BorderLayout.NORTH);
            add(scroll);
        }

        @Override
        public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {
            // set text
            if (priceIncrements.length == 0) {
                m_text.setText(null);
            } else {
                StringBuilder sb = new StringBuilder(256);
                DecimalFormat df = new DecimalFormat("#.#");
                df.setMaximumFractionDigits(340);

                sb.append("Market Rule Id: ").append(marketRuleId).append("\n");
                for (PriceIncrement priceIncrement : priceIncrements) {
                    sb.append("Low Edge: ").append(df.format(priceIncrement.lowEdge())).append(", ")
                            .append("Increment: ").append(df.format(priceIncrement.increment())).append("\n");
                }
                m_text.setText(sb.toString());
            }
        }
    }
}
