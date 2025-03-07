/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import com.ib.client.Contract;
import com.ib.client.NewsProvider;
import com.ib.client.Util;
import com.ib.client.Types.SecType;
import com.ib.controller.ApiController.INewsArticleHandler;
import com.ib.controller.ApiController.INewsProvidersHandler;
import com.ib.controller.ApiController.ITickNewsHandler;
import com.ib.controller.ApiController.IHistoricalNewsHandler;

import com.ib.samples.apidemo.util.HtmlButton;
import com.ib.samples.apidemo.util.NewTabbedPanel;
import com.ib.samples.apidemo.util.NewTabbedPanel.NewTabPanel;
import com.ib.samples.apidemo.util.TCombo;
import com.ib.samples.apidemo.util.UpperField;
import com.ib.samples.apidemo.util.VerticalPanel;
import com.ib.samples.apidemo.util.VerticalPanel.HorzPanel;
import com.ib.samples.apidemo.util.VerticalPanel.StackPanel;

class NewsPanel extends JPanel {
    private final NewTabbedPanel m_requestPanels = new NewTabbedPanel();
    private final NewTabbedPanel m_resultsPanels = new NewTabbedPanel();

    private NewsArticleRequestPanel m_newsArticleRequestPanel = new NewsArticleRequestPanel();

    NewsPanel() {
        m_requestPanels.addTab("News Ticks", new NewsTicksRequestPanel());
        m_requestPanels.addTab("News Providers", new RequestNewsProvidersPanel());
        m_requestPanels.addTab("News Article", m_newsArticleRequestPanel);
        m_requestPanels.addTab("Historical News", new HistoricalNewsRequestPanel());

        setLayout(new BorderLayout());
        add(m_requestPanels, BorderLayout.NORTH);
        add(m_resultsPanels);
    }

    private static class RequestPanel extends JPanel {
        JTextField m_providerCode = new JTextField();
        JTextField m_articleId = new JTextField();
        JTextField m_path = new JTextField(System.getProperty("user.dir"));

        RequestPanel() {
            VerticalPanel p = new VerticalPanel();
            p.add("Provider Code", m_providerCode);
            m_providerCode.setColumns(20);
            p.add("Article Id", m_articleId);
            m_articleId.setColumns(20);

            JButton choosePathDialogButton = new JButton("...");
            JFileChooser chooser = new JFileChooser(m_path.getText());

            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            choosePathDialogButton.addActionListener(e -> m_path.setText(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().getPath() : m_path.getText()));

            HorzPanel pathPanel = new HorzPanel();

            pathPanel.add(m_path);
            pathPanel.add(choosePathDialogButton);

            p.add("Path to save binary/pdf", pathPanel);
            m_path.setColumns(20);

            setLayout(new BorderLayout());
            add(p);
        }

        public void setProviderCode(String v) {
            m_providerCode.setText(v);
        }

        public void setArticleId(String v) {
            m_articleId.setText(v);
        }

        @Override
        public Dimension getMaximumSize() {
            return super.getPreferredSize();
        }
    }

    class NewsArticleRequestPanel extends JPanel {
        final RequestPanel m_requestPanel = new RequestPanel();

        NewsArticleRequestPanel() {
            HtmlButton butReqNewsArticle = new HtmlButton("Request News Article") {
                @Override
                protected void actionPerformed() {
                    onReqNewsArticle();
                }
            };

            VerticalPanel butPanel = new VerticalPanel();
            butPanel.add(butReqNewsArticle);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(m_requestPanel);
            add(Box.createHorizontalStrut(20));
            add(butPanel);
        }

        void onReqNewsArticle() {
            NewsArticleResultsPanel panel = new NewsArticleResultsPanel();
            String providerCode = m_requestPanel.m_providerCode.getText().trim();
            String articleId = m_requestPanel.m_articleId.getText().trim();
            String path = m_requestPanel.m_path.getText().trim() + "\\" + articleId + ".pdf";
            panel.setPath(path);
            ApiDemo.INSTANCE.controller().reqNewsArticle(providerCode, articleId, panel);
            m_resultsPanels.addTab("News Article: " + providerCode + " " + articleId, panel, true, true);
        }
    }

    static class NewsArticleResultsPanel extends JPanel implements INewsArticleHandler {
        JLabel m_label = new JLabel();
        JTextArea m_text = new JTextArea();
        String m_path;

        NewsArticleResultsPanel() {
            JScrollPane scroll = new JScrollPane(m_text);

            setLayout(new BorderLayout());
            add(m_label, BorderLayout.NORTH);
            add(scroll);
        }

        void setPath(String path) {
            m_path = path;
        }

        @Override
        public void newsArticle(int articleType, String articleText) {
            if (articleType == 0) {
                m_label.setText("Article type is text or html");
                m_text.setText(articleText);
            } else if (articleType == 1) {
                m_label.setText("Article type is binary/pdf");
                if (articleType == 1) {
                    try {
                        byte[] bytes = Base64.getDecoder().decode(articleText);
                        FileOutputStream fos = new FileOutputStream(m_path);
                        fos.write(bytes);
                        fos.close();
                        m_text.setText("Binary/pdf article was saved to " + m_path);
                    } catch (IOException ex) {
                        m_text.setText("Binary/pdf article was not saved to " + m_path + " due to error: " + ex.getMessage());
                    }
                }

            }
        }
    }

    private class RequestNewsProvidersPanel extends JPanel {
        RequestNewsProvidersPanel() {
            HtmlButton requestNewsProvidersButton = new HtmlButton("Request NewsProviders") {
                @Override
                protected void actionPerformed() {
                    onRequestNewsProviders();
                }
            };

            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(requestNewsProvidersButton);
        }

        void onRequestNewsProviders() {
            NewsProvidersPanel newsProvidersPanel = new NewsProvidersPanel();
            m_resultsPanels.addTab("News Providers", newsProvidersPanel, true, true);
            ApiDemo.INSTANCE.controller().reqNewsProviders(newsProvidersPanel);
        }
    }

    static class NewsProvidersPanel extends NewTabPanel implements INewsProvidersHandler {
        final NewsProvidersModel m_model = new NewsProvidersModel();
        final List<NewsProvidersRow> m_rows = new ArrayList<>();

        NewsProvidersPanel() {
            JTable table = new JTable(m_model);
            JScrollPane scroll = new JScrollPane(table);
            setLayout(new BorderLayout());
            add(scroll);
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
        public void closed() { /* noop */ }

        @Override
        public void newsProviders(NewsProvider[] newsProviders) {
            for (NewsProvider newsProvider : newsProviders) {
                NewsProvidersRow newsProvidersRow = new NewsProvidersRow(
                        newsProvider.providerCode(),
                        newsProvider.providerName()
                );
                m_rows.add(newsProvidersRow);
            }
            fire();
        }

        private void fire() {
            SwingUtilities.invokeLater(() -> {
                m_model.fireTableRowsInserted(m_rows.size() - 1, m_rows.size() - 1);
                revalidate();
            });
        }

        class NewsProvidersModel extends AbstractTableModel {
            @Override
            public int getRowCount() {
                return m_rows.size();
            }

            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public String getColumnName(int col) {
                switch (col) {
                    case 0:
                        return "Provider Code";
                    case 1:
                        return "Provider Name";
                    default:
                        return null;
                }
            }

            @Override
            public Object getValueAt(int rowIn, int col) {
                NewsProvidersRow newsProvidersRow = m_rows.get(rowIn);
                switch (col) {
                    case 0:
                        return newsProvidersRow.m_providerCode;
                    case 1:
                        return newsProvidersRow.m_providerName;
                    default:
                        return null;
                }
            }
        }

        static class NewsProvidersRow {
            String m_providerCode;
            String m_providerName;

            NewsProvidersRow(String providerCode, String providerName) {
                update(providerCode, providerName);
            }

            void update(String providerCode, String providerName) {
                m_providerCode = providerCode;
                m_providerName = providerName;
            }
        }
    }

    class NewsTicksRequestPanel extends JPanel {
        private UpperField m_symbol = new UpperField();
        private TCombo<SecType> m_secType = new TCombo<>(SecType.values());
        private UpperField m_exchange = new UpperField();
        private UpperField m_primExchange = new UpperField();
        private UpperField m_currency = new UpperField();

        NewsTicksRequestPanel() {
            m_symbol.setText("IBKR");
            m_secType.setSelectedItem(SecType.STK);
            m_exchange.setText("SMART");
            m_primExchange.setText("NYSE");
            m_currency.setText("USD");

            HtmlButton but = new HtmlButton("Request News Ticks") {
                @Override
                protected void actionPerformed() {
                    onRequestNewsTicks();
                }
            };

            VerticalPanel topPanel = new VerticalPanel();
            topPanel.add("Symbol", m_symbol);
            topPanel.add("Sec Type", m_secType);
            topPanel.add("Exchange", m_exchange, Box.createHorizontalStrut(30), but);
            topPanel.add("Prim Exch", m_primExchange);
            topPanel.add("Currency", m_currency);
            setLayout(new BorderLayout());
            add(topPanel, BorderLayout.NORTH);
        }

        void onRequestNewsTicks() {
            Contract contract = new Contract();
            contract.symbol(m_symbol.getText().toUpperCase());
            contract.secType(m_secType.getSelectedItem());
            contract.exchange(m_exchange.getText().toUpperCase());
            contract.primaryExch(m_primExchange.getText().toUpperCase());
            contract.currency(m_currency.getText().toUpperCase());

            NewsTicksResultsPanel panel = new NewsTicksResultsPanel();
            m_resultsPanels.addTab("News Ticks: " + contract.symbol(), panel, true, true);
            ApiDemo.INSTANCE.controller().reqNewsTicks(contract, panel);
        }
    }

    class NewsTicksResultsPanel extends NewTabPanel implements ITickNewsHandler {
        final NewsTicksModel m_model = new NewsTicksModel();
        final List<NewsTickRow> m_rows = new ArrayList<>();

        NewsTicksResultsPanel() {
            JTable table = new JTable(m_model);
            table.getSelectionModel().addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                    NewsTickRow newsTickRow = m_rows.get(table.getSelectedRow());
                    if (newsTickRow.m_providerCode.length() > 0 && newsTickRow.m_articleId.length() > 0) {
                        m_requestPanels.select("News Article");
                        m_newsArticleRequestPanel.m_requestPanel.setProviderCode(newsTickRow.m_providerCode);
                        m_newsArticleRequestPanel.m_requestPanel.setArticleId(newsTickRow.m_articleId);
                    }
                }
            });
            table.getColumnModel().getColumn(3).setMinWidth(550);
            JScrollPane scroll = new JScrollPane(table);
            setLayout(new BorderLayout());
            add(scroll);
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
        public void closed() { /* noop */ }

        @Override
        public void tickNews(long timeStamp, String providerCode, String articleId, String headline, String extraData) {
            NewsTickRow newsTickRow = new NewsTickRow(timeStamp, providerCode, articleId, headline, extraData);
            m_rows.add(newsTickRow);
            fire();
        }

        private void fire() {
            SwingUtilities.invokeLater(() -> {
                m_model.fireTableRowsInserted(m_rows.size() - 1, m_rows.size() - 1);
                revalidate();
                repaint();
            });
        }

        class NewsTicksModel extends AbstractTableModel {
            @Override
            public int getRowCount() {
                return m_rows.size();
            }

            @Override
            public int getColumnCount() {
                return 5;
            }

            @Override
            public String getColumnName(int col) {
                switch (col) {
                    case 0:
                        return "Time Stamp";
                    case 1:
                        return "Provider Code";
                    case 2:
                        return "Article Id";
                    case 3:
                        return "Headline";
                    case 4:
                        return "Extra Data";
                    default:
                        return null;
                }
            }

            @Override
            public Object getValueAt(int rowIn, int col) {
                NewsTickRow newsTickRow = m_rows.get(rowIn);
                switch (col) {
                    case 0:
                        return newsTickRow.m_timeStamp;
                    case 1:
                        return newsTickRow.m_providerCode;
                    case 2:
                        return newsTickRow.m_articleId;
                    case 3:
                        return newsTickRow.m_headline;
                    case 4:
                        return newsTickRow.m_extraData;
                    default:
                        return null;
                }
            }
        }

        class NewsTickRow {
            String m_timeStamp;
            String m_providerCode;
            String m_articleId;
            String m_headline;
            String m_extraData;

            NewsTickRow(long timeStamp, String providerCode, String articleId, String headline, String extraData) {
                update(timeStamp, providerCode, articleId, headline, extraData);
            }

            void update(long timeStamp, String providerCode, String articleId, String headline, String extraData) {
                m_timeStamp = Util.UnixMillisecondsToString(timeStamp, "yyyyMMdd-HH:mm:ss");
                m_providerCode = providerCode;
                m_articleId = articleId;
                m_headline = headline;
                m_extraData = extraData;
            }
        }
    }

    class HistoricalNewsRequestPanel extends JPanel {
        protected UpperField m_conId = new UpperField("8314");
        protected JTextField m_providerCodes = new JTextField("BZ+FLY");
        protected JTextField m_startDateTime = new JTextField();
        protected JTextField m_endDateTime = new JTextField();
        protected UpperField m_totalResults = new UpperField("10");

        HistoricalNewsRequestPanel() {

            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.0");
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -3);
            m_endDateTime.setText(df.format(cal.getTime()));
            cal.add(Calendar.DATE, -1);
            m_startDateTime.setText(df.format(cal.getTime()));

            HtmlButton but = new HtmlButton("Request Historical News") {
                @Override
                protected void actionPerformed() {
                    onRequestHistoricalNews();
                }
            };

            VerticalPanel topPanel = new VerticalPanel();
            topPanel.add("Contract Id", m_conId);
            topPanel.add("Provider Codes", m_providerCodes);
            topPanel.add("Start Date/Time", m_startDateTime, Box.createHorizontalStrut(30), but);
            topPanel.add("End Date/Time", m_endDateTime);
            topPanel.add("Total Results", m_totalResults);

            setLayout(new BorderLayout());
            add(topPanel, BorderLayout.NORTH);
        }

        void onRequestHistoricalNews() {
            HistoricalNewsResultsPanel panel = new HistoricalNewsResultsPanel();
            m_resultsPanels.addTab("Hist News: " + m_conId.getText(), panel, true, true);
            ApiDemo.INSTANCE.controller().reqHistoricalNews(m_conId.getInt(), m_providerCodes.getText(),
                    m_startDateTime.getText(), m_endDateTime.getText(), m_totalResults.getInt(), panel);
        }
    }

    class HistoricalNewsResultsPanel extends NewTabPanel implements IHistoricalNewsHandler {
        final HistoricalNewsModel m_model = new HistoricalNewsModel();
        final List<HistoricalNewsRow> m_rows = new ArrayList<>();

        HistoricalNewsResultsPanel() {
            JTable table = new JTable(m_model);
            table.getSelectionModel().addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                    HistoricalNewsRow historicalNewsRow = m_rows.get(table.getSelectedRow());
                    if (historicalNewsRow.m_providerCode.length() > 0 && historicalNewsRow.m_articleId.length() > 0) {
                        m_requestPanels.select("News Article");
                        m_newsArticleRequestPanel.m_requestPanel.setProviderCode(historicalNewsRow.m_providerCode);
                        m_newsArticleRequestPanel.m_requestPanel.setArticleId(historicalNewsRow.m_articleId);
                    }
                }
            });
            table.getColumnModel().getColumn(3).setMinWidth(650);
            JScrollPane scroll = new JScrollPane(table);
            setLayout(new BorderLayout());
            add(scroll);
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
        public void closed() { /* noop */ }

        @Override
        public void historicalNews(String time, String providerCode, String articleId, String headline) {
            HistoricalNewsRow historicalNewsRow = new HistoricalNewsRow(time, providerCode, articleId, headline);
            m_rows.add(historicalNewsRow);
        }

        @Override
        public void historicalNewsEnd(boolean hasMore) {
            if (hasMore) {
                HistoricalNewsRow historicalNewsRow = new HistoricalNewsRow("", "", "", "has more ...");
                m_rows.add(historicalNewsRow);
            }
            fire();
        }

        private void fire() {
            SwingUtilities.invokeLater(() -> {
                m_model.fireTableRowsInserted(m_rows.size() - 1, m_rows.size() - 1);
                revalidate();
                repaint();
            });
        }

        class HistoricalNewsModel extends AbstractTableModel {
            @Override
            public int getRowCount() {
                return m_rows.size();
            }

            @Override
            public int getColumnCount() {
                return 4;
            }

            @Override
            public String getColumnName(int col) {
                switch (col) {
                    case 0:
                        return "Time";
                    case 1:
                        return "Provider Code";
                    case 2:
                        return "Article Id";
                    case 3:
                        return "Headline";
                    default:
                        return null;
                }
            }

            @Override
            public Object getValueAt(int rowIn, int col) {
                HistoricalNewsRow historicalNewsRow = m_rows.get(rowIn);
                switch (col) {
                    case 0:
                        return historicalNewsRow.m_time;
                    case 1:
                        return historicalNewsRow.m_providerCode;
                    case 2:
                        return historicalNewsRow.m_articleId;
                    case 3:
                        return historicalNewsRow.m_headline;
                    default:
                        return null;
                }
            }
        }

        class HistoricalNewsRow {
            String m_time;
            String m_providerCode;
            String m_articleId;
            String m_headline;

            HistoricalNewsRow(String time, String providerCode, String articleId, String headline) {
                update(time, providerCode, articleId, headline);
            }

            void update(String time, String providerCode, String articleId, String headline) {
                m_time = time;
                m_providerCode = providerCode;
                m_articleId = articleId;
                m_headline = headline;
            }
        }
    }
}
