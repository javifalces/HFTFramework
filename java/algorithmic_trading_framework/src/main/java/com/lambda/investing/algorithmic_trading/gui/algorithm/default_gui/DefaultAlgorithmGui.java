package com.lambda.investing.algorithmic_trading.gui.algorithm.default_gui;


import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.lambda.investing.Configuration;
import com.lambda.investing.LambdaThreadFactory;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PortfolioSnapshot;
import com.lambda.investing.algorithmic_trading.gui.algorithm.AlgorithmGui;
import com.lambda.investing.algorithmic_trading.gui.algorithm.DepthTableModel;
import com.lambda.investing.algorithmic_trading.gui.timeseries.TickTimeSeries;
import com.lambda.investing.connector.ordinary.thread_pool.ThreadPoolExecutorChannels;
import com.lambda.investing.market_data_connector.parquet_file_reader.ParquetMarketDataConnectorPublisher;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import lombok.Getter;
import lombok.Setter;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartTheme;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import static com.lambda.investing.algorithmic_trading.gui.main.MainMenuGUI.IS_BACKTEST;
import static com.lambda.investing.model.Util.toJsonString;
import static com.lambda.investing.model.asset.Instrument.round;
import static com.lambda.investing.trading_engine_connector.paper.market.OrderbookManager.MARKET_MAKER_ALGORITHM_INFO;

/**
 * To compile from maven
 * "Generate GUI into:" from "Binary class files" -&gt; "Java source code" in the settings (found in Project|Settings|Editor|GUI Designer).
 */
@Getter
public class DefaultAlgorithmGui implements AlgorithmGui {
    @Getter
    @Setter
    private class DepthModel {
        DepthTableModel depthTableModel;
        JTable orderbookDepth;
    }

    private Logger logger = LogManager.getLogger(DefaultAlgorithmGui.class);
    private ConcurrentMap<String, DepthModel> depthTables;

    private Map<String, PnlSnapshot> lastPnlSnapshot;
    private double lastRealizedPnl = 0.0;
    private double lastUnrealizedPnl = 0.0;

    private JTable orderbookDepth;
    private JEditorPane pnlSnapshotUpdates;
    private JPanel panel;
    private JPanel marketDataPanelTick;
    private JPanel pnlPanelTick;
    private JPanel positionPanelTick;
    private JPanel pnlSnapshotUpdatesPanel;
    private JEditorPane lastTradesText;
    private JLabel Trades;
    private JEditorPane parametersText;
    private JSlider speedSlider;
    private JLabel SpeedText;
    private JTabbedPane depthTabs;
    private JPanel tab1;
    private Queue<Trade> tradesReceived = EvictingQueue.create(8);
    private Queue<Map<String, Object>> paramsUpdateReceived = EvictingQueue.create(8);

    private TickTimeSeries marketDataTimeSeries;
    private TickTimeSeries pnlTimeSeries;
    private TickTimeSeries positionTimeSeries;

    private static final long MARKET_DATA_MIN_TIME_MS = 500;


    private ThreadPoolExecutorChannels guiThreadPoolBuffered;
    private ThreadPoolExecutorChannels guiThreadPool;
    private static final int GUI_THREAD_POOL_SIZE_BUFFERED = 3;
    private static final int GUI_THREAD_POOL_SIZE = 3;
    private static final long TIMEOUT_UPDATE_PORTFOLIO_SECONDS = 60;
    private long lastUpdateTimestamp = 0L;
    private long lastUpdatePnlSnapshot = 0L;
    private Instrument instrument;

    public DefaultAlgorithmGui(Instrument instrument, ChartTheme theme) {
        this.instrument = instrument;
        depthTables = new ConcurrentHashMap<>();
        lastPnlSnapshot = new HashMap<>();
        marketDataTimeSeries = new TickTimeSeries(theme, marketDataPanelTick, "Best Bid/Ask", "Date", "Price");
        pnlTimeSeries = new TickTimeSeries(theme, pnlPanelTick, "Profit & Loss", "Date", "PnL");
        positionTimeSeries = new TickTimeSeries(theme, positionPanelTick, "Position", "Date", "Position");
        initializeThreadpool(instrument);
        initializeSpeedSlider();
        updatePnlSnapshot(new PnlSnapshot(this.instrument.getPrimaryKey()));//initial update
        depthTabs.remove(0);//remove na tab
    }


    private void initializeDepthTable(String instrument) {
        DepthModel model = new DepthModel();
        model.depthTableModel = new DepthTableModel();
        model.orderbookDepth = new JTable(model.depthTableModel);
        depthTables.put(instrument, model);
        depthTabs.add(instrument, model.orderbookDepth);
    }


    private void initializeSpeedSlider() {
        if (!IS_BACKTEST) {
            speedSlider.setVisible(false);
            return;
        }
        speedSlider.addChangeListener(this::speedSliderListener);

    }


    private void initializeThreadpool(Instrument instrument) {
        ThreadFactory namedThreadFactory = LambdaThreadFactory.createThreadFactory(instrument.getPrimaryKey() + "_MarketMakingAlgorithmGuiBuffered", Thread.MIN_PRIORITY);

        guiThreadPoolBuffered = new ThreadPoolExecutorChannels(null, 1, GUI_THREAD_POOL_SIZE_BUFFERED, 60, TimeUnit.SECONDS
                , new LinkedBlockingQueue<Runnable>(), namedThreadFactory, true);

        ThreadFactory namedThreadFactory1 = LambdaThreadFactory.createThreadFactory(instrument.getPrimaryKey() + "_MarketMakingAlgorithmGui", Thread.MIN_PRIORITY);

        guiThreadPool = new ThreadPoolExecutorChannels("MarketMakingAlgorithmGui", 1, GUI_THREAD_POOL_SIZE, 60, TimeUnit.SECONDS
                , new LinkedBlockingQueue<Runnable>(), namedThreadFactory1, false);

//        guiThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(GUI_THREAD_POOL_SIZE, namedThreadFactory);


    }


    private void updateGUI(Runnable runnable) {
        updateGUI(runnable, null);
    }

    private void updateGUI(Runnable runnable, String channel) {
        try {
//        runnable.run();
//        SwingUtilities.invokeAndWait(runnable);
            if (channel != null) {
                guiThreadPoolBuffered.execute(runnable, channel);
            } else {
                guiThreadPool.execute(runnable);
            }
        } catch (Exception e) {
            logger.error("error plotting ", e);
        }

    }

    //listeners observer
    private void speedSliderListener(ChangeEvent e) {
        int speed = speedSlider.getValue();
        String messagePrint = "Speed: " + speed;
        if (speed == 0) {
            messagePrint += " (Paused)";
            ParquetMarketDataConnectorPublisher.setPauseTradingEngine(true);
        } else {
            ParquetMarketDataConnectorPublisher.setPauseTradingEngine(false);
        }

        if (speed >= 100) {
            messagePrint = "Speed: max";
            ParquetMarketDataConnectorPublisher.setSpeed(-1);
        }
        if (speed > 0 && speed < 100) {
            int newSpeed = (int) Math.round(Math.exp(speed / 10.0));
            messagePrint = "Speed: " + newSpeed;
            ParquetMarketDataConnectorPublisher.setSpeed(newSpeed);
        }
        SpeedText.setText(messagePrint);
    }

    public void updateDepth(Depth depth) {
        try {
            lastUpdateTimestamp = Math.max(depth.getTimestamp(), lastUpdateTimestamp);
            boolean updateTimeSeries = MARKET_DATA_MIN_TIME_MS > 0 && (depth.getTimestamp() - marketDataTimeSeries.getLastTimestamp()) > MARKET_DATA_MIN_TIME_MS;
            if (!depthTables.containsKey(depth.getInstrument())) {
                initializeDepthTable(depth.getInstrument());

            }

            Runnable runnable = new Runnable() {
                public void run() {
                    //update orderbook table
                    DepthTableModel depthTable = depthTables.get(depth.getInstrument()).depthTableModel;
                    depthTable.updateDepth(depth);

                    //update timeseries tab
                    if (depth.getInstrument().equals(instrument.getPrimaryKey())) {
                        marketDataTimeSeries.updateTimeSerie(TickTimeSeries.ASK_SERIE, depth.getTimestamp(), depth.getBestAsk());
                        marketDataTimeSeries.updateTimeSerie(TickTimeSeries.BID_SERIE, depth.getTimestamp(), depth.getBestBid());
                    }


                    boolean refreshPnl = depth.getTimestamp() - lastUpdatePnlSnapshot > TIMEOUT_UPDATE_PORTFOLIO_SECONDS * 1000;
                    if (refreshPnl) {
                        updatePnlTimeSerie(depth.getTimestamp());
                        lastUpdatePnlSnapshot = depth.getTimestamp();
                    }

                }
            };
            if (updateTimeSeries) {
                updateGUI(runnable, depth.getInstrument());
            }

        } catch (Exception e) {
            logger.error("error plotting ", e);
        }

    }


    public void updateExecutionReport(ExecutionReport executionReport) {
        lastUpdateTimestamp = Math.max(executionReport.getTimestampCreation(), lastUpdateTimestamp);

        try {

            boolean updateTimeSeries = ExecutionReport.isLiveStatus(executionReport) || ExecutionReport.isRemovedStatus(executionReport);
            boolean isTrade = ExecutionReport.isTradeStatus(executionReport);

            if (updateTimeSeries) {

                Runnable runnable = new Runnable() {
                    public void run() {
                        //update table
                        DepthTableModel depthTable = depthTables.get(executionReport.getInstrument()).depthTableModel;
                        depthTable.updateExecutionReport(executionReport);

                        //TODO something faster
                        //update timeseries tab
//                    if (liveStatus.contains(executionReport.getExecutionReportStatus())) {
//                        String series = executionReport.getVerb() == Verb.Buy ? "quoted_bid" : "quoted_ask";
//                        marketDataTimeSeries.updateTimeSerie(series, executionReport.getTimestampCreation(), executionReport.getPrice());
//                    }
                    }

                };

                updateGUI(runnable);


            }

            if (isTrade) {
                Trade trade = Trade.getInstance();
                trade.setTradeFromExecutionReport(executionReport);
                updateTrade(trade);
            }


        } catch (Exception e) {
            logger.error("error plotting ", e);
        }


    }

    @Override
    public void updateCustomColumn(long timestamp, String instrumentPk, String key, Double value) {

    }

    private void updatePnlTimeSerie(long timestamp) {
        double unrealizedPnl = 0.0;
        double realizedPnl = 0.0;
        double fees = 0.0;
        int trades = 0;
        int tradesAggressor = 0;
        StringBuffer textInstrument = new StringBuffer();
        for (Map.Entry<String, PnlSnapshot> entry : lastPnlSnapshot.entrySet()) {
            String instrumentPk = entry.getKey();
            if (instrumentPk == null) {
                continue;
            }

            PnlSnapshot pnlSnapshot1 = entry.getValue();
            textInstrument.append(formatPnlSnapshot(pnlSnapshot1));
            textInstrument.append("\n");
            unrealizedPnl += pnlSnapshot1.getTotalPnl();
            realizedPnl += pnlSnapshot1.getRealizedPnl();
            fees += pnlSnapshot1.getTotalFees();
            trades += pnlSnapshot1.getNumberOfTrades().get();
            tradesAggressor += pnlSnapshot1.getNumberOfAggressorTrades().get();


            positionTimeSeries.updateTimeSerie("position " + instrumentPk, timestamp, pnlSnapshot1.getNetPosition());
            lastUpdatePnlSnapshot = Math.max(lastUpdatePnlSnapshot, timestamp);
        }
        textInstrument.append("Total Unrealized Pnl: " + round(unrealizedPnl, 2) + "\n");
        textInstrument.append("Total Realized Pnl: " + round(realizedPnl, 2) + "\n");
        textInstrument.append("Total Fees: " + round(fees, 2) + "\n");
        textInstrument.append("Trades: " + trades + " (agg: " + tradesAggressor + ") \n");

        pnlSnapshotUpdates.setText(textInstrument.toString());
        pnlTimeSeries.updateTimeSerie("Total Unrealized Pnl", timestamp, unrealizedPnl);
        pnlTimeSeries.updateTimeSerie("Total Realized Pnl", timestamp, realizedPnl);
    }

    public void updatePnlSnapshot(PnlSnapshot pnlSnapshot) {
        lastUpdateTimestamp = Math.max(pnlSnapshot.getLastTimestampUpdate(), lastUpdateTimestamp);
        String instrumentPk = pnlSnapshot.getInstrumentPk();
        lastPnlSnapshot.put(instrumentPk, pnlSnapshot);
        Runnable runnable = new Runnable() {
            public void run() {
                updatePnlTimeSerie(lastUpdateTimestamp);
            }
        };
        updateGUI(runnable);

    }

    @Override
    public void updatePortfolioSnapshot(PortfolioSnapshot portfolioSnapshot) {

    }


    private String formatPnlSnapshot(PnlSnapshot pnlSnapshot) {
        String output = Configuration.formatLog("" +
                        "{}\n" +
                        "\tLastUpdate:{}\n" +
                        "\tTrades: {} (agg: {})\n" +
                        "\tUnrealized Pnl: {}\n" +
                        "\tRealized Pnl: {}\n" +
                        "\tFees: {}\n" +
                        "\tPosition: {}\n" +
                        "\tLast :{} {}@{}" +
                        "",
                pnlSnapshot.getInstrumentPk(),
                new Date(pnlSnapshot.getLastTimestampUpdate()).toString(),
                pnlSnapshot.getNumberOfTrades(),
                pnlSnapshot.getNumberOfAggressorTrades(),
                round(pnlSnapshot.getTotalPnl(), 2),
                round(pnlSnapshot.getRealizedPnl(), 2),
                round(pnlSnapshot.getTotalFees(), 2),
                round(pnlSnapshot.getNetPosition(), 4),
                pnlSnapshot.getLastVerb(),
                pnlSnapshot.getLastQuantity(),
                pnlSnapshot.getLastPrice()

        );
        return output;
    }

    public void updateTrade(Trade trade) {
        lastUpdateTimestamp = Math.max(trade.getTimestamp(), lastUpdateTimestamp);
        tradesReceived.offer(trade);
        Runnable runnable = new Runnable() {
            public void run() {
                List<Trade> tradesTemp = new ArrayList<>(tradesReceived);
                Collections.reverse(tradesTemp);
                StringBuilder output = new StringBuilder();
                for (Trade trade : tradesTemp) {
                    if (trade != null) {
                        String formatTrade = formatTrade(trade);
                        if (formatTrade != null) {
                            output.append(formatTrade);
                            output.append("\n");
                        }

                    }
                }
                lastTradesText.setText(output.toString());
            }
        };
        updateGUI(runnable);
    }

    private String formatTrade(Trade trade) {
        if (trade == null) {
            logger.error("formatTrade with null trade");
            return null;
        }
        try {

            String output = Configuration.formatLog("" +
                    "[{}] {} {} {}@{}", new Date(trade.getTimestamp()), trade.getVerb(), trade.getInstrument(), trade.getQuantity(), trade.getPrice());

            if (trade.getAlgorithmInfo() != null && !trade.getAlgorithmInfo().equals(MARKET_MAKER_ALGORITHM_INFO)) {
                output += " [algo]";//only in backtest we will have algo info
            }

            return output;
        } catch (Exception e) {
            logger.error("error formatting trade", e);
            return null;
        }
    }

    public void updateParams(Map<String, Object> newParams) {
        paramsUpdateReceived.offer(newParams);
        Runnable runnable = new Runnable() {
            public void run() {
                List<Map<String, Object>> paramsTemp = new ArrayList<>(paramsUpdateReceived);
                Collections.reverse(paramsTemp);
                StringBuilder output = new StringBuilder();
                for (Map<String, Object> parameter : paramsTemp) {
                    String message = toJsonString(parameter);
                    output.append("[" + new Date(lastUpdateTimestamp).toString() + "]");
                    output.append(message);
                    output.append("\n");
                }
                parametersText.setText(output.toString());
            }
        };
        updateGUI(runnable);

    }

    public void updateMessage(String name, String body) {
    }

    public void updateOrderRequest(OrderRequest orderRequest) {
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel = new JPanel();
        panel.setLayout(new GridLayoutManager(14, 3, new Insets(0, 0, 0, 0), -1, -1));
        marketDataPanelTick = new JPanel();
        marketDataPanelTick.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(marketDataPanelTick, new GridConstraints(10, 0, 4, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(700, 500), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        marketDataPanelTick.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel.add(spacer1, new GridConstraints(12, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        pnlPanelTick = new JPanel();
        pnlPanelTick.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(pnlPanelTick, new GridConstraints(11, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(324, 462), null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel.add(spacer2, new GridConstraints(10, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        positionPanelTick = new JPanel();
        positionPanelTick.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(positionPanelTick, new GridConstraints(13, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(324, 462), null, 0, false));
        final Spacer spacer3 = new Spacer();
        positionPanelTick.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        positionPanelTick.add(spacer4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel.add(spacer5, new GridConstraints(2, 1, 12, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel.add(spacer6, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        pnlSnapshotUpdatesPanel = new JPanel();
        pnlSnapshotUpdatesPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(pnlSnapshotUpdatesPanel, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        pnlSnapshotUpdates = new JEditorPane();
        pnlSnapshotUpdates.setText("Portfolio");
        pnlSnapshotUpdatesPanel.add(pnlSnapshotUpdates, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(324, 100), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Portfolio");
        pnlSnapshotUpdatesPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer7 = new Spacer();
        panel.add(spacer7, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(panel1, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setHorizontalScrollBarPolicy(31);
        scrollPane2.setVerticalScrollBarPolicy(21);
        panel1.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        lastTradesText = new JEditorPane();
        lastTradesText.setEditable(false);
        scrollPane2.setViewportView(lastTradesText);
        Trades = new JLabel();
        Trades.setText("Trades");
        panel.add(Trades, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Depth");
        panel.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer8 = new Spacer();
        panel.add(spacer8, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Parameters");
        panel.add(label3, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        parametersText = new JEditorPane();
        panel.add(parametersText, new GridConstraints(8, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(panel2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        speedSlider = new JSlider();
        speedSlider.setValue(100);
        panel2.add(speedSlider, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        SpeedText = new JLabel();
        SpeedText.setText("Speed: max");
        panel2.add(SpeedText, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        depthTabs = new JTabbedPane();
        panel.add(depthTabs, new GridConstraints(1, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        tab1 = new JPanel();
        tab1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        depthTabs.addTab("Untitled", tab1);
        final JScrollPane scrollPane3 = new JScrollPane();
        tab1.add(scrollPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        orderbookDepth = new JTable();
        orderbookDepth.setShowVerticalLines(false);
        scrollPane3.setViewportView(orderbookDepth);
    }

    /**
     * Returns the root component.
     */
    public JComponent $$$getRootComponent$$$() {
        return panel;
    }


}
