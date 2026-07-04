package com.lambda.investing.model.portfolio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.lambda.investing.model.Util.fromJsonString;
import static com.lambda.investing.model.Util.toJsonString;

@Getter
@Setter
public class Portfolio implements Runnable {

    private static Map<String, Portfolio> pathToInstance = new HashMap<>();
    public static String REQUESTED_PORTFOLIO_INFO = "portfolio";
    public static String REQUESTED_POSITION_INFO = "position";
    protected static Logger logger = LogManager.getLogger(Portfolio.class);
    private Map<String, PortfolioInstrument> portfolioInstruments;
    private transient String path;// portfolio.json in a file path to persist
    private transient String tradesPath;// portfolio.trades.csv in a file path to persist
    //	private double openPnl=0.;
    //	private double closePnl=0.;
    //	private double totalPnl=0.;
    private long timestampLastUpdate = 0;
    private boolean autosave = false;
    private long lastSaveTimestamp = 0;


    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.alibaba.fastjson2.annotation.JSONField(serialize = false, deserialize = false)
    private transient SortedSet<ExecutionReport> executionReportsTrades;

    public static Portfolio getPortfolio(String path, boolean isBacktest, boolean isPaperTrading) {
        boolean isLiveTrading = !isBacktest && !isPaperTrading;

        if (isPaperTrading) {
            path = path.replace(".json", "_paper.json");
        }

        if (pathToInstance.containsKey(path)) {
            return pathToInstance.get(path);
        }
        Portfolio portfolio = new Portfolio();//backtest is not persisting it
        File portfolioFile = new File(path);

        if (!isBacktest) {
            //load existing position if live trading
            if (isLiveTrading && portfolioFile.exists()) {
                //read it
                try {
                    String fileContent = new String(Files.readAllBytes(Paths.get(path)));
                    portfolio = fromJsonString(fileContent, Portfolio.class);
                    portfolio.setPath(path);

                    new Thread(portfolio, "portfolio_autosave").start();

                } catch (IOException e) {
                    logger.error("error reading portfolio json from {}-> close app to not override anything", path, e);
                    System.exit(-1);
                }

            } else {
                //create new instance
                portfolio = new Portfolio(path);
            }

        }


        pathToInstance.put(path, portfolio);
        return portfolio;
    }

    public static Portfolio getPortfolio(String path) {
        return getPortfolio(path, false, false);
    }

    public Portfolio() {
        //calling directly here will not save into path
        portfolioInstruments = new HashMap<>();
        executionReportsTrades = new TreeSet<>(Comparator.comparingLong(ExecutionReport::getTimestampCreation));
        this.autosave = false;
    }

    private Portfolio(String path) {
        //calling directly here will not save into path
        portfolioInstruments = new HashMap<>();
        executionReportsTrades = new TreeSet<>(Comparator.comparingLong(ExecutionReport::getTimestampCreation));
        setPath(path);
        //call autosave
        if (this.path != null) {
            this.autosave = true;
            new Thread(this, "portfolio_autosave").start();
        }
    }

    public void setPath(String path) {
        this.path = path;
        if (path != null) {
            this.tradesPath = this.path.replace(".json", ".trades.csv");
            logger.info("Portfolio autosave portfolio to {} and trades to {}", this.path, this.tradesPath);
        }
    }

    public void clear() {
        portfolioInstruments.clear();
        executionReportsTrades.clear();
        timestampLastUpdate = 0;
        lastSaveTimestamp = 0;
    }

    public void updateTrade(ExecutionReport executionReport) {
        if (ExecutionReport.isTradeStatus(executionReport)) {
            //only in trades
            PortfolioInstrument portfolioInstrument = portfolioInstruments.getOrDefault(executionReport.getInstrument(),
                    new PortfolioInstrument(executionReport.getInstrument()));
            portfolioInstrument.updateTrade(executionReport);
            portfolioInstruments.put(executionReport.getInstrument(), portfolioInstrument);
            executionReportsTrades.add(executionReport);
            timestampLastUpdate = executionReport.getTimestampCreation();
        }
    }

    private void savePortfolio() {
        String content = toJsonString(this);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            logger.error("error saving portfolio json {} in {}", content, path, e);
        }
    }

    private void saveTrades() {
        if (tradesPath == null) {
            return;
        }
        try {

            if (executionReportsTrades.isEmpty()) {
                return;
            }

            List<ExecutionReport> allExecutionReports = new ArrayList<>(executionReportsTrades);//copy to avoid concurrency issues
            BufferedWriter writer = new BufferedWriter(new FileWriter(tradesPath));
            writer.write(ExecutionReport.getCSVHeader() + "\n");
            for (ExecutionReport executionReport : allExecutionReports) {
                writer.write(executionReport.toCSVString() + "\n");
            }

            writer.close();
        } catch (IOException e) {
            logger.error("error saving trades csv in {}", tradesPath, e);
        }
    }

    public Map<String, Double> getPositions() {
        Map<String, Double> positions = new HashMap<>();
        for (Map.Entry<String, PortfolioInstrument> entry : portfolioInstruments.entrySet()) {
            positions.put(entry.getKey(), entry.getValue().getPosition());
        }
        return positions;
    }

    @Override
    public void run() {
        while (this.autosave) {

            //saving if updates
            if (lastSaveTimestamp != timestampLastUpdate) {
                savePortfolio();
                saveTrades();
                lastSaveTimestamp = timestampLastUpdate;
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
