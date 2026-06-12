package com.lambda.investing.algorithmic_trading.gui.main;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.lambda.investing.algorithmic_trading.*;
import com.lambda.investing.algorithmic_trading.gui.algorithm.AlgorithmGui;
import com.lambda.investing.algorithmic_trading.gui.algorithm.default_gui.DefaultAlgorithmGui;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PortfolioSnapshot;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import org.jfree.chart.ChartTheme;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * To compile from maven
 * "Generate GUI into:" from "Binary class files" -&gt; "Java source code" in the settings (found in Project|Settings|Editor|GUI Designer).
 */
public class MainMenuGUI extends JFrame implements AlgorithmObserver {
    private JTabbedPane depthTabs;
    private JPanel panel1;
    private JLabel textFieldPaperTrading;

    public static boolean IS_BACKTEST = true;

    private static String TITLE = "[%s]Lambda Algotrading  %d algorithms";
    private List<Algorithm> algorithmsList;
    private Map<String, AlgorithmGui> algorithmsMap;

    private boolean isPaperTrading = false;
    protected ChartTheme theme;

    public MainMenuGUI(ChartTheme theme, boolean isPaperTrading, List<Algorithm> algorithmsList) {
        //		https://www.fdi.ucm.es/profesor/jpavon/poo/tema6resumido.pdf
        super(String.format(TITLE, new Date(), algorithmsList.size()));
        this.theme = theme;
        this.algorithmsList = algorithmsList;
        algorithmsMap = new ConcurrentHashMap<>();
        this.isPaperTrading = isPaperTrading;
    }

    public MainMenuGUI(ChartTheme theme, boolean isPaperTrading, Algorithm algorithm) {
        super(String.format(TITLE, new Date(), 1));
        this.theme = theme;
        this.algorithmsList = new ArrayList<>();
        this.algorithmsList.add(algorithm);
        algorithmsMap = new ConcurrentHashMap<>();
        this.isPaperTrading = isPaperTrading;
    }

    private void startGUI() {
        textFieldPaperTrading.setVisible(isPaperTrading);  // Control visibility
        this.add(panel1);  // Add the parent panel with both components
        setDefaultCloseOperation(EXIT_ON_CLOSE);

    }


    public void start() {
        try {
            startGUI();
            depthTabs.remove(0);//remove na tab

            for (Algorithm algorithm : algorithmsList) {

                AlgorithmGui algorithmGui = algorithm.getAlgorithmGui(theme);
                if (algorithmGui != null) {
                    depthTabs.add(algorithm.getAlgorithmInfo(), algorithmGui.getPanel());
                    algorithmsMap.put(algorithm.getAlgorithmInfo(), algorithmGui);
                } else {
                    DefaultAlgorithmGui defaultAlgorithmGui = new DefaultAlgorithmGui(((SingleInstrumentAlgorithm) algorithm).getInstrument(), theme);
                    depthTabs.add(algorithm.getAlgorithmInfo(), defaultAlgorithmGui.getPanel());
                    algorithmsMap.put(algorithm.getAlgorithmInfo(), defaultAlgorithmGui);
                }

                algorithm.register(this);
            }
            this.pack();
            setSize(1024, 768);
            setVisible(true);
        } catch (Exception e) {
            System.err.println("Error starting GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTitle(Date date) {
        setTitle(String.format(TITLE, date, algorithmsList.size()));
    }

    @Override
    public void onUpdateDepth(String algorithmInfo, Depth depth) {
        updateTitle(depth.getDate());
        algorithmsMap.get(algorithmInfo).updateDepth(depth);
    }

    @Override
    public void onUpdatePnlSnapshot(String algorithmInfo, PnlSnapshot pnlSnapshot) {
        algorithmsMap.get(algorithmInfo).updatePnlSnapshot(pnlSnapshot);
    }

    @Override
    public void onUpdatePortfolioSnapshot(String algorithmInfo, PortfolioSnapshot portfolioSnapshot) {
        algorithmsMap.get(algorithmInfo).updatePortfolioSnapshot(portfolioSnapshot);
    }

    @Override
    public void onUpdateTrade(String algorithmInfo, Trade trade) {
        updateTitle(trade.getDate());
        algorithmsMap.get(algorithmInfo).updateTrade(trade);
    }

    @Override
    public void onUpdateParams(String algorithmInfo, Map<String, Object> newParams) {
        algorithmsMap.get(algorithmInfo).updateParams(newParams);
    }

    @Override
    public void onUpdateMessage(String algorithmInfo, String name, String body) {
        algorithmsMap.get(algorithmInfo).updateMessage(name, body);
    }

    @Override
    public void onOrderRequest(String algorithmInfo, OrderRequest orderRequest) {
        algorithmsMap.get(algorithmInfo).updateOrderRequest(orderRequest);
    }

    @Override
    public void onExecutionReportUpdate(String algorithmInfo, ExecutionReport executionReport) {
        algorithmsMap.get(algorithmInfo).updateExecutionReport(executionReport);
    }

    @Override
    public void onCustomColumns(long timestamp, String algorithmInfo, String instrumentPk, String key, Double value) {
        algorithmsMap.get(algorithmInfo).updateCustomColumn(timestamp, instrumentPk, key, value);
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
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setEnabled(false);
        depthTabs = new JTabbedPane();
        depthTabs.setEnabled(true);
        panel1.add(depthTabs, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(1280, 1024), null, 1, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.setEnabled(false);
        depthTabs.addTab("na", panel2);
        depthTabs.setEnabledAt(0, false);
        textFieldPaperTrading = new JLabel();
        textFieldPaperTrading.setBackground(new Color(-6149600));
        textFieldPaperTrading.setEnabled(false);
        textFieldPaperTrading.setForeground(new Color(-460552));
        textFieldPaperTrading.setHorizontalAlignment(0);
        textFieldPaperTrading.setOpaque(true);
        textFieldPaperTrading.setText("-= PAPER TRADING =-");
        panel1.add(textFieldPaperTrading, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 20), new Dimension(-1, 20), null, 0, false));
    }

    /**
     * Returns the root component.
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
