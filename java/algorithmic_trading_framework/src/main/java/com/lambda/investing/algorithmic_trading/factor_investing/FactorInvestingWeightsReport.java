package com.lambda.investing.algorithmic_trading.factor_investing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Periodic CSV report for {@link AbstractFactorInvestingAlgorithm} implementations: one row per
 * instrument each time {@link #report} is invoked (wall-clock scheduled in live trading, and after
 * every {@code onWeightsUpdate} otherwise), so weight/expected-position/current-position and
 * investment per instrument, plus total investment, can be followed and audited over time.
 * <p>
 * Relies on log4j2 configuration; the logger name is this class' FQCN, following the same
 * dedicated-appender pattern as {@link com.lambda.investing.algorithmic_trading.observer.LiveTradeReport}.
 */
public class FactorInvestingWeightsReport {

    public static class InstrumentReportRow {
        public final double weight;
        public final double expectedPosition;
        public final double currentPosition;
        public final double investment;

        public InstrumentReportRow(double weight, double expectedPosition, double currentPosition,
                                   double investment) {
            this.weight = weight;
            this.expectedPosition = expectedPosition;
            this.currentPosition = currentPosition;
            this.investment = investment;
        }
    }

    private static final Logger reportLogger = LogManager.getLogger(FactorInvestingWeightsReport.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    private static final String CSV_HEADER = "timestamp,date,modelName,instrumentPk,weight,expectedPosition,currentPosition,investment,totalInvestment,capital";

    private boolean headerWritten = false;

    public void report(long timestamp, String modelName, double capital, double totalInvestment,
                       Map<String, InstrumentReportRow> rowsPerInstrument) {
        if (rowsPerInstrument.isEmpty()) {
            return;
        }
        writeHeaderIfNeeded();
        String date = DATE_FORMATTER.format(Instant.ofEpochMilli(timestamp));
        for (Map.Entry<String, InstrumentReportRow> entry : rowsPerInstrument.entrySet()) {
            InstrumentReportRow row = entry.getValue();
            reportLogger.info("{},{},{},{},{},{},{},{},{},{}", timestamp, date, modelName, entry.getKey(),
                    row.weight, row.expectedPosition, row.currentPosition, row.investment, totalInvestment,
                    capital);
        }
    }

    private void writeHeaderIfNeeded() {
        if (!headerWritten) {
            String logFileName = getReportLogFileName();
            if (logFileName != null) {
                File logFile = new File(logFileName);
                if (logFile.exists() && logFile.length() > 0) {
                    //file already has content -> don't duplicate the header
                    headerWritten = true;
                    return;
                }
            }
            reportLogger.info(CSV_HEADER);
            headerWritten = true;
        }
    }

    private String getReportLogFileName() {
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            org.apache.logging.log4j.core.Logger coreLogger = context.getLogger(FactorInvestingWeightsReport.class.getName());
            for (var entry : coreLogger.getAppenders().entrySet()) {
                var appender = entry.getValue();
                if (appender instanceof RollingFileAppender) {
                    return ((RollingFileAppender) appender).getFileName();
                } else if (appender instanceof FileAppender) {
                    return ((FileAppender) appender).getFileName();
                }
            }
        } catch (Exception e) {
            reportLogger.debug("Could not determine log file name, will write header: {}", e.getMessage());
        }
        return null;
    }
}
