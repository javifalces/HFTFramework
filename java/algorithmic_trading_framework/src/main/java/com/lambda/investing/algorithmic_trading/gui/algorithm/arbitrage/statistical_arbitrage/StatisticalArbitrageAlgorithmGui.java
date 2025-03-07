package com.lambda.investing.algorithmic_trading.gui.algorithm.arbitrage.statistical_arbitrage;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.lambda.investing.ArrayUtils;
import com.lambda.investing.Configuration;
import com.lambda.investing.TimeSeriesQueue;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.gui.algorithm.AlgorithmGui;
import com.lambda.investing.algorithmic_trading.gui.algorithm.DepthTableModel;
import com.lambda.investing.algorithmic_trading.gui.algorithm.market_making.MarketMakingAlgorithmGui;
import com.lambda.investing.algorithmic_trading.gui.timeseries.TickTimeSeries;
import com.lambda.investing.connector.ordinary.thread_pool.ThreadPoolExecutorChannels;
import com.lambda.investing.market_data_connector.parquet_file_reader.ParquetMarketDataConnectorPublisher;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import lombok.Setter;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartTheme;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;

import static com.lambda.investing.ArrayUtils.ArrayReverse;
import static com.lambda.investing.algorithmic_trading.AlgorithmParameters.*;
import static com.lambda.investing.algorithmic_trading.gui.main.MainMenuGUI.IS_BACKTEST;
import static com.lambda.investing.model.Util.toJsonString;
import static com.lambda.investing.model.asset.Instrument.round;
import static com.lambda.investing.model.trading.ExecutionReport.liveStatus;
import static com.lambda.investing.model.trading.ExecutionReport.removedStatus;
import static com.lambda.investing.trading_engine_connector.paper.market.OrderbookManager.MARKET_MAKER_ALGORITHM_INFO;

/**
 * To compile from maven
 * "Generate GUI into:" from "Binary class files" --> "Java source code" in the settings (found in Project|Settings|Editor|GUI Designer).
 */
@Getter
public class StatisticalArbitrageAlgorithmGui implements AlgorithmGui {
    @Getter
    @Setter
    private class DepthModel {
        DepthTableModel depthTableModel;
        JTable orderbookDepth;
    }

    private Logger logger = LogManager.getLogger(StatisticalArbitrageAlgorithmGui.class);
    private ConcurrentMap<String, DepthModel> depthTables;

    private double zscoreBuy;
    private double zscoreSell;
    private double zscoreMid;
    private double zscoreEntryBuy;
    private double zscoreExitBuy;
    private double zscoreEntrySell;
    private double zscoreExitSell;


    private JTable orderbookDepth;
    private JEditorPane pnlSnapshotUpdates;
    private JPanel panel;
    private JPanel zscoresPanelTick;
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
    private TickTimeSeries zscoreTimeSeries;


    private static final long MARKET_DATA_MIN_TIME_MS = 500;
    private Map<String, PnlSnapshot> lastPnlSnapshot;
    private ConcurrentMap<String, Long> lastMarketDepthUpdateTimestamps;

    private ThreadPoolExecutorChannels guiThreadPoolBuffered;
    private ThreadPoolExecutorChannels guiThreadPool;
    private static final int GUI_THREAD_POOL_SIZE_BUFFERED = 3;
    private static final int GUI_THREAD_POOL_SIZE = 3;
    private static final long TIMEOUT_UPDATE_PORTFOLIO_SECONDS = 60;
    private long lastUpdateTimestamp = 0L;
    private long lastUpdatePnlSnapshot = 0L;

    private double lastRealizedPnl = 0.0;
    private double lastUnrealizedPnl = 0.0;
    private Map<String, TimeSeriesQueue<Double>> midPrices;
    private ConcurrentMap<String, Double> lastMidPrices;

    public StatisticalArbitrageAlgorithmGui(ChartTheme theme) {
        depthTables = new ConcurrentHashMap<>();

        lastPnlSnapshot = new HashMap<>();
        marketDataTimeSeries = new TickTimeSeries(theme, marketDataPanelTick, "Cum.MidReturns", "Date", "Price");
        zscoreTimeSeries = new TickTimeSeries(theme, zscoresPanelTick, "ZScore", "Date", "ZScore");
        pnlTimeSeries = new TickTimeSeries(theme, pnlPanelTick, "Profit & Loss", "Date", "PnL");
        positionTimeSeries = new TickTimeSeries(theme, positionPanelTick, "Position", "Date", "Position");
        midPrices = new HashMap<>();
        lastMidPrices = new ConcurrentHashMap<String, Double>();
        lastMarketDepthUpdateTimestamps = new ConcurrentHashMap<>();

//        zscoreTimeSeries = new TickTimeSeries(theme, zscoresPanelTick, "ZScore", "Date", "ZScore");
        initializeThreadpool();
        initializeSpeedSlider();
        updatePnlSnapshot(new PnlSnapshot());//initial update
        depthTabs.remove(0);//remove na tab


    }


    private void initializeSpeedSlider() {
        if (!IS_BACKTEST) {
            speedSlider.setVisible(false);
            return;
        }
        speedSlider.addChangeListener(this::speedSliderListener);

    }


    private void initializeThreadpool() {

        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setNameFormat("instrument_StatisticalArbitrageAlgorithmGuiBuffered" + "-%d");
        threadFactoryBuilder.setPriority(Thread.MIN_PRIORITY);
        ThreadFactory namedThreadFactory = threadFactoryBuilder.build();

        guiThreadPoolBuffered = new ThreadPoolExecutorChannels(null, 1, GUI_THREAD_POOL_SIZE_BUFFERED, 60, TimeUnit.SECONDS
                , new LinkedBlockingQueue<Runnable>(), namedThreadFactory, true);


        ThreadFactoryBuilder threadFactoryBuilder1 = new ThreadFactoryBuilder();
        threadFactoryBuilder1.setNameFormat("instrument_StatisticalArbitrageAlgorithmGui" + "-%d");
        threadFactoryBuilder1.setPriority(Thread.MIN_PRIORITY);
        ThreadFactory namedThreadFactory1 = threadFactoryBuilder1.build();

        guiThreadPool = new ThreadPoolExecutorChannels("StatisticalArbitrageAlgorithmGui", 1, GUI_THREAD_POOL_SIZE, 60, TimeUnit.SECONDS
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

    public double GetMidPriceNormalized(Depth depth) {
        //update timeseries tab

        if (!midPrices.containsKey(depth.getInstrument())) {
            midPrices.put(depth.getInstrument(), new TimeSeriesQueue<>(5));
        }
        if (!depth.isDepthFilled()) {
            return Double.NaN;
        }
        TimeSeriesQueue<Double> midPricesSeries = midPrices.get(depth.getInstrument());
        midPricesSeries.offer(depth.getMidPrice());
        if (midPricesSeries.size() < 2) {
            return Double.NaN;
        }
        TimeSeriesQueue<Double> returns = TimeSeriesQueue.pctChange(midPricesSeries);
        double lastMidPrice = lastMidPrices.getOrDefault(depth.getInstrument(), .0);
        double output = lastMidPrice + returns.getNewest();
        lastMidPrices.put(depth.getInstrument(), output);
        return output;
    }

    private void initializeDepthTable(String instrument) {
        DepthModel model = new DepthModel();
        model.depthTableModel = new DepthTableModel();
        model.orderbookDepth = new JTable(model.depthTableModel);
        depthTables.put(instrument, model);
        depthTabs.add(instrument, model.orderbookDepth);
    }

    public void updateDepth(Depth depth) {
        try {
            lastUpdateTimestamp = Math.max(depth.getTimestamp(), lastUpdateTimestamp);
            long lastDepthUpdate = lastMarketDepthUpdateTimestamps.getOrDefault(depth.getInstrument(), 0L);
            boolean updateTimeSeries = MARKET_DATA_MIN_TIME_MS > 0 && (depth.getTimestamp() - lastDepthUpdate) > MARKET_DATA_MIN_TIME_MS;
            if (!depthTables.containsKey(depth.getInstrument())) {
                initializeDepthTable(depth.getInstrument());
            }

            Runnable runnable = new Runnable() {
                public void run() {
                    //update orderbook table
                    DepthTableModel depthTable = depthTables.get(depth.getInstrument()).depthTableModel;
                    depthTable.updateDepth(depth);
                    double midPrice = GetMidPriceNormalized(depth);
                    if (Double.isNaN(midPrice)) {
                        return;
                    }
                    marketDataTimeSeries.updateTimeSerie(depth.getInstrument(), depth.getTimestamp(), GetMidPriceNormalized(depth));

                    boolean refreshPnl = depth.getTimestamp() - lastUpdatePnlSnapshot > TIMEOUT_UPDATE_PORTFOLIO_SECONDS * 1000;
                    if (refreshPnl) {
                        updatePnlTimeSerie(depth.getTimestamp());
                        lastUpdatePnlSnapshot = depth.getTimestamp();
                    }

                }
            };
            if (updateTimeSeries) {
                updateGUI(runnable, depth.getInstrument());
                lastMarketDepthUpdateTimestamps.put(depth.getInstrument(), lastUpdateTimestamp);
            }

        } catch (Exception e) {
            logger.error("error plotting ", e);
        }

    }


    public void updateExecutionReport(ExecutionReport executionReport) {
        lastUpdateTimestamp = Math.max(executionReport.getTimestampCreation(), lastUpdateTimestamp);

        try {

            boolean updateTimeSeries = liveStatus.contains(executionReport.getExecutionReportStatus()) || removedStatus.contains(executionReport.getExecutionReportStatus());
            boolean isTrade = executionReport.getExecutionReportStatus() == ExecutionReportStatus.CompletellyFilled || executionReport.getExecutionReportStatus() == ExecutionReportStatus.PartialFilled;

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
                Trade trade = new Trade(executionReport);
                updateTrade(trade);
            }


        } catch (Exception e) {
            logger.error("error plotting ", e);
        }


    }

    @Override
    public void updateCustomColumn(long timestamp, String instrumentPk, String key, Double value) {
        lastUpdateTimestamp = Math.max(timestamp, lastUpdateTimestamp);
        boolean updateTimeSeries = MARKET_DATA_MIN_TIME_MS > 0 && (timestamp - zscoreTimeSeries.getLastTimestamp()) > MARKET_DATA_MIN_TIME_MS;
        if (key.equals("zscore_buy")) {
            zscoreBuy = value;
        }
        if (key.equals("zscore_sell")) {
            zscoreSell = value;
        }
        if (key.equals("zscore_mid")) {
            Runnable runnable = new Runnable() {
                public void run() {
                    zscoreMid = value;
                    double valuePlot = Math.min(Math.max(zscoreMid, -4), 4);//between -4 and 4
                    zscoreTimeSeries.updateTimeSerie("ZScore", timestamp, valuePlot);
                    zscoreTimeSeries.updateTimeSerie("Entry Buy", timestamp, zscoreEntryBuy);
                    zscoreTimeSeries.updateTimeSerie("Exit Buy", timestamp, zscoreExitBuy);
                    zscoreTimeSeries.updateTimeSerie("Entry Sell", timestamp, zscoreEntrySell);
                    zscoreTimeSeries.updateTimeSerie("Exit Sell", timestamp, zscoreExitSell);
                }
            };
            if (updateTimeSeries) {
                updateGUI(runnable, "zscore");
            }


        }


    }

    private void updatePnlTimeSerie(long timestamp) {
        double unrealizedPnl = 0.0;
        double realizedPnl = 0.0;
        double fees = 0.0;
        int trades = 0;
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


            positionTimeSeries.updateTimeSerie("position " + instrumentPk, timestamp, pnlSnapshot1.getNetPosition());
            lastUpdatePnlSnapshot = Math.max(lastUpdatePnlSnapshot, timestamp);
        }
        textInstrument.append("Total Unrealized Pnl: " + round(unrealizedPnl, 2) + "\n");
        textInstrument.append("Total Realized Pnl: " + round(realizedPnl, 2) + "\n");
        textInstrument.append("Total Fees: " + round(fees, 2) + "\n");
        textInstrument.append("Trades: " + trades + "\n");

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

    private String formatPnlSnapshot(PnlSnapshot pnlSnapshot) {
        String output = Configuration.formatLog("" +
                        "{}\n" +
                        "\tLastUpdate:{}\n" +
                        "\tTrades: {}\n" +
                        "\tUnrealized Pnl: {}\n" +
                        "\tRealized Pnl: {}\n" +
                        "\tFees: {}\n" +
                        "\tPosition: {}\n" +
                        "\tLast :{} {}@{}" +
                        "",
                pnlSnapshot.getInstrumentPk(),
                new Date(pnlSnapshot.getLastTimestampUpdate()).toString(),
                pnlSnapshot.getNumberOfTrades(),
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
            if (!trade.getAlgorithmInfo().equals(MARKET_MAKER_ALGORITHM_INFO)) {
                output += " [algo]";
            }

            return output;
        } catch (Exception e) {
            logger.error("error formatting trade", e);
            return null;
        }
    }

    public void updateParams(Map<String, Object> newParams) {
        paramsUpdateReceived.offer(newParams);
        zscoreEntryBuy = getParameterDouble(newParams, "zscoreEntryBuy");
        zscoreExitBuy = getParameterDouble(newParams, "zscoreExitBuy");
        zscoreEntrySell = getParameterDouble(newParams, "zscoreEntrySell");
        zscoreExitSell = getParameterDouble(newParams, "zscoreExitSell");
        String instrumentMain = getParameterString(newParams, "instrument");
        initializeDepthTable(instrumentMain);

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
        panel.setLayout(new GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
        marketDataPanelTick = new JPanel();
        marketDataPanelTick.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(marketDataPanelTick, new GridConstraints(4, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(429, 500), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        marketDataPanelTick.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        pnlPanelTick = new JPanel();
        pnlPanelTick.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(pnlPanelTick, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(324, 462), null, 0, false));
        positionPanelTick = new JPanel();
        positionPanelTick.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(positionPanelTick, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(324, 462), null, 0, false));
        pnlSnapshotUpdatesPanel = new JPanel();
        pnlSnapshotUpdatesPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(pnlSnapshotUpdatesPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        pnlSnapshotUpdatesPanel.add(scrollPane2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        pnlSnapshotUpdates = new JEditorPane();
        pnlSnapshotUpdates.setText("Portfolio");
        scrollPane2.setViewportView(pnlSnapshotUpdates);
        final JLabel label1 = new JLabel();
        label1.setText("Portfolio");
        pnlSnapshotUpdatesPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(panel1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(429, 27), null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        scrollPane3.setHorizontalScrollBarPolicy(31);
        scrollPane3.setVerticalScrollBarPolicy(21);
        panel1.add(scrollPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        lastTradesText = new JEditorPane();
        lastTradesText.setEditable(false);
        scrollPane3.setViewportView(lastTradesText);
        Trades = new JLabel();
        Trades.setText("Trades");
        panel.add(Trades, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(429, 17), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Depth");
        panel.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(429, 17), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Parameters");
        panel.add(label3, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        speedSlider = new JSlider();
        speedSlider.setValue(100);
        panel2.add(speedSlider, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        SpeedText = new JLabel();
        SpeedText.setText("Speed: max");
        panel2.add(SpeedText, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        depthTabs = new JTabbedPane();
        panel.add(depthTabs, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(429, 200), null, 0, false));
        tab1 = new JPanel();
        tab1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        depthTabs.addTab("Untitled", tab1);
        final JScrollPane scrollPane4 = new JScrollPane();
        tab1.add(scrollPane4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        orderbookDepth = new JTable();
        orderbookDepth.setShowVerticalLines(false);
        scrollPane4.setViewportView(orderbookDepth);
        zscoresPanelTick = new JPanel();
        zscoresPanelTick.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(zscoresPanelTick, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(429, 24), null, 0, false));
        final JScrollPane scrollPane5 = new JScrollPane();
        zscoresPanelTick.add(scrollPane5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane6 = new JScrollPane();
        panel.add(scrollPane6, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        parametersText = new JEditorPane();
        scrollPane6.setViewportView(parametersText);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel;
    }


}
