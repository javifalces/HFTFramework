package com.lambda.investing.model;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.DepthParquet;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.market_data.TradeParquet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Date;

import static com.lambda.investing.PrintUtils.PrintDate;

public abstract class CSVable implements Serializable {

    public static Logger logger = LogManager.getLogger(CSVable.class);

    public abstract String toCSV(boolean withHeader);

    public abstract Object getParquetObject();

    public static CSVable getCSVAble(Object parquetObject, Instrument instrument) {
        if (parquetObject instanceof DepthParquet) {
            Depth depth = Depth.getInstance();
            depth.setDepthFromParquet((DepthParquet) parquetObject, instrument);
            return depth;
        }
        if (parquetObject instanceof TradeParquet) {
            Trade trade = Trade.getInstance();
            trade.setTradeFromParquet((TradeParquet) parquetObject, instrument);
            return trade;
        }

        logger.error("getCSVAble parquetObject not recognized! return null");
        //TODO something better
        return null;
    }

    public static String getLatenciesTable(long timestamp, long timestampBrokerConnector, long timestampAlgoConnector, long timestampStrategy) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-30s %-30s %-20s\n", "Event", "Timestamp", "Latency (ms)"));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("%-30s %-30s %-20d\n", "timestamp", PrintDate(new Date(timestamp)), 0));
        long lastReference = timestamp;
        if (timestampBrokerConnector > 0) {
            sb.append(String.format("%-30s %-30s %-20d\n", "timestampBrokerConnector", PrintDate(new Date(timestampBrokerConnector)), timestampBrokerConnector - lastReference));
            lastReference = timestampBrokerConnector;
        }
        if (timestampAlgoConnector > 0) {
            sb.append(String.format("%-30s %-30s %-20d\n", "timestampAlgoConnector", PrintDate(new Date(timestampAlgoConnector)), timestampAlgoConnector - lastReference));
            lastReference = timestampAlgoConnector;
        }
        if (timestampStrategy > 0) {
            sb.append(String.format("%-30s %-30s %-20d\n", "timestampAlgoConnector", PrintDate(new Date(timestampAlgoConnector)), timestampAlgoConnector - lastReference));
            lastReference = timestampStrategy;
        }
        sb.append(String.format("%-30s %-30s %-20d\n", "now", PrintDate(new Date()), System.currentTimeMillis() - lastReference));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("%-30s %-30s %-20d\n", "Total", "", System.currentTimeMillis() - timestamp));


        return sb.toString();
    }

}
