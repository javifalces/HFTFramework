package com.lambda.investing.algorithmic_trading.observer;

import com.lambda.investing.algorithmic_trading.AlgorithmObserver;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PortfolioSnapshot;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;

import java.io.File;
import java.util.Map;

/**
 * AlgorithmObserver implementation that logs execution reports with trade status to a CSV file.
 * This observer is designed for live trading (when isBacktest is false) to track all trades
 * executed by the algorithm in a structured CSV format.
 * <p>
 * The CSV file includes all fields from the ExecutionReport for comprehensive trade tracking.
 * Only ExecutionReports with trade status (CompletelyFilled or PartialFilled) are logged.
 *
 * <p><b>Configuration:</b></p>
 * This observer relies on log4j2 configuration. The logger name follows the pattern:
 * <pre>com.lambda.investing.TradeReport</pre>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // In your live trading setup, after creating the algorithm instance:
 * Algorithm algorithm = AlgorithmCreationUtils.getInstance()
 *     .getAlgorithm(algorithmConnectorConfiguration, algorithmName, parameters);
 *
 * // Register the LiveCSVObserver only for live trading (not backtest)
 * if (!algorithm.isBacktest()) {
 *     LiveCSVObserver csvObserver = new LiveCSVObserver(algorithm.getAlgorithmInfo());
 *     algorithm.register(csvObserver);
 * }
 *
 * // Initialize and start the algorithm
 * algorithm.init();
 * algorithm.start();
 * }</pre>
 *
 * <p>The generated CSV file will be located at:</p>
 * <pre>
 * ${log.path}/trades_${algorithmInfo}_${timestamp}.csv
 * </pre>
 *
 * <p>The CSV format includes the following columns:</p>
 * <ul>
 *   <li>timestampCreation - when the order was created</li>
 *   <li>date - human-readable date</li>
 *   <li>instrument - trading instrument</li>
 *   <li>verb - BUY or SELL</li>
 *   <li>executionReportStatus - execution status</li>
 *   <li>price - execution price</li>
 *   <li>quantity - order quantity</li>
 *   <li>lastQuantity - last filled quantity</li>
 *   <li>quantityFill - total filled quantity</li>
 *   <li>clientOrderId - client order identifier</li>
 *   <li>algorithmInfo - algorithm identifier</li>
 * </ul>
 *
 * @see AlgorithmObserver
 * @see ExecutionReport
 */
public class LiveTradeReport implements AlgorithmObserver {

    private static final Logger tradeLogger = LogManager.getLogger(LiveTradeReport.class);

    private boolean headerWritten = false;

    /**
     * Constructor that initializes the CSV observer.
     */
    public LiveTradeReport() {
    }

    /**
     * Writes the CSV header if it hasn't been written yet.
     * Checks if the log file already exists and has content - if so, skips writing the header.
     */
    private void writeHeaderIfNeeded() {
        if (!headerWritten) {
            // Check if the log file already exists and has content
            String logFileName = getTradeLogFileName();
            if (logFileName != null) {
                File logFile = new File(logFileName);
                if (logFile.exists() && logFile.length() > 0) {
                    // File exists and is not empty, skip writing header
                    System.out.println("WARNING: LiveCSVObserver Trade log file already exists and is not empty, skipping header write: " + logFileName);
                    headerWritten = true;
                    return;
                }
            }

            // File doesn't exist or is empty, write the header
            tradeLogger.info(ExecutionReport.getCSVHeader());
            headerWritten = true;
        }
    }

    /**
     * Gets the current log file name from the tradeLogger's appenders.
     *
     * @return the log file name, or null if not found
     */
    private String getTradeLogFileName() {
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            org.apache.logging.log4j.core.Logger coreLogger = context.getLogger("com.lambda.investing.algorithmic_trading.observer.LiveTradeReport");

            // Try to find the RollingFileAppender
            var appenders = coreLogger.getAppenders();
            for (var entry : appenders.entrySet()) {
                var appender = entry.getValue();
                if (appender instanceof RollingFileAppender) {
                    return ((RollingFileAppender) appender).getFileName();
                } else if (appender instanceof FileAppender) {
                    return ((FileAppender) appender).getFileName();
                }
            }
        } catch (Exception e) {
            // If we can't determine the file name, just return null and write the header
            tradeLogger.debug("Could not determine log file name, will write header: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void onExecutionReportUpdate(String algorithmInfo, ExecutionReport executionReport) {
        // Only log execution reports that have trade status (CompletelyFilled or PartialFilled)
        if (ExecutionReport.isTradeStatus(executionReport)) {
            writeHeaderIfNeeded();
            tradeLogger.info(executionReport.toCSVString());
        }
    }

    // Empty implementations for other observer methods - we only care about execution reports
    @Override
    public void onUpdateDepth(String algorithmInfo, Depth depth) {
        // Not used
    }

    @Override
    public void onUpdatePnlSnapshot(String algorithmInfo, PnlSnapshot pnlSnapshot) {
        // Not used
    }

    @Override
    public void onUpdatePortfolioSnapshot(String algorithmInfo, PortfolioSnapshot portfolioSnapshot) {
        // Not used
    }

    @Override
    public void onUpdateTrade(String algorithmInfo, Trade trade) {
        // Not used
    }

    @Override
    public void onUpdateParams(String algorithmInfo, Map<String, Object> newParams) {
        // Not used
    }

    @Override
    public void onUpdateMessage(String algorithmInfo, String name, String body) {
        // Not used
    }

    @Override
    public void onOrderRequest(String algorithmInfo, OrderRequest orderRequest) {
        // Not used
    }

    @Override
    public void onCustomColumns(long timestamp, String algorithmInfo, String instrumentPk, String key, Double value) {
        // Not used
    }
}
