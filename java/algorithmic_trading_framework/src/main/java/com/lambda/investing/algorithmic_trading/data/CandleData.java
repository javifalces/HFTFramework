package com.lambda.investing.algorithmic_trading.data;

import com.lambda.investing.Configuration;
import com.lambda.investing.data_manager.DataManager;
import com.lambda.investing.data_manager.parquet.TableSawParquetDataManager;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.io.File;
import java.util.*;

public class CandleData {

    private String baseCandlePath;
    private DataManager dataManager;

    public CandleData() {
        this.baseCandlePath = Configuration.getDataPath();
        TableSawParquetDataManager dataManager = new TableSawParquetDataManager();
        dataManager.setTimestampCol("date_time");
        this.dataManager = dataManager;
    }

    private String getBaseCandlePath(String instrumentPk, CandleType candleType, int secondsCandle) {
        //X:\type=candle_midpricetime_MIN10\instrument=audusd_darwinex\date=20240809
        int minutesCandle = secondsCandle / 60;
        switch (candleType) {

            case mid_time_seconds_threshold:
                return baseCandlePath + File.separator + "type=candle_midpricetime_MIN" + minutesCandle + File.separator + "instrument=" + instrumentPk
                        + File.separator;

            case bid_time_seconds_threshold:
                return baseCandlePath + File.separator + "type=candle_bidpricetime_MIN" + minutesCandle + File.separator + "instrument=" + instrumentPk
                        + File.separator;
            case ask_time_seconds_threshold:
                return baseCandlePath + File.separator + "type=candle_askpricetime_MIN" + minutesCandle + File.separator + "instrument=" + instrumentPk
                        + File.separator;
        }
        //default case
        return baseCandlePath + File.separator + "type=" + candleType.name() + File.separator + "instrument=" + instrumentPk
                + File.separator;

    }

    private List<String> getCandleFiles(String instrumentPk, CandleType candleType, int secondsCandle, Date startDate, Date endDate) {
        String basePath = getBaseCandlePath(instrumentPk, candleType, secondsCandle);
        List<String> candleFiles = new ArrayList<>();
        if (startDate != null && endDate != null) {
            Date dateTemp = new Date(startDate.getTime());
            while (dateTemp.getTime() <= endDate.getTime()) {
                String dateStr = Configuration.getDateFormat(dateTemp);
                String candlePath = basePath + "date=" + dateStr + File.separator + "data.parquet";
                candleFiles.add(candlePath);
                dateTemp.setTime(dateTemp.getTime() + 24 * 60 * 60 * 1000); // Increment by one day
            }

        }
        return candleFiles;
    }

    public Map<String, List<Candle>> downloadCandles(Date startDate, Date endDate, Set<String> instruments, CandleType candleType, int secondsCandles) {
        Map<String, List<String>> candleFilesToGet = new HashMap<>();
        for (String instrumentPk : instruments) {
            List<String> candleFiles = getCandleFiles(instrumentPk, candleType, secondsCandles, startDate, endDate);
            candleFilesToGet.putIfAbsent(instrumentPk, new ArrayList<>());
            candleFilesToGet.get(instrumentPk).addAll(candleFiles);
        }
        Map<String, List<Candle>> candles = new HashMap<>();
        for (String instrumentPk : candleFilesToGet.keySet()) {
            for (String candleFile : candleFilesToGet.get(instrumentPk)) {
                File file = new File(candleFile);
                if (file.exists()) {
                    try {
                        Table table = this.dataManager.getData(candleFile, Candle.class);
                        if (table == null || table.isEmpty()) {
                            continue;
                        }
                        // Convert Table to List<Candle>
                        for (int rowNumber = 0; rowNumber < table.rowCount(); rowNumber++) {
                            Row row = table.row(rowNumber);
                            // Create a Candle object from the row
                            long timestamp = 0;
                            try {
                                timestamp = row.getLong("date_time") * 1000L; // Assuming date_time is in seconds, convert to milliseconds
                            } catch (IllegalArgumentException e) {
                                timestamp = (long) row.getDouble("date_time") * 1000L;
                            }
                            long openTimestamp = timestamp;
                            long closeTimestamp = timestamp;
                            if (table.columnNames().contains("openTimestamp")) {
                                openTimestamp = getTimeMillis(row, "openTimestamp", timestamp);
                            } else if (table.columnNames().contains("openTime")) {
                                openTimestamp = getTimeMillis(row, "openTime", timestamp);
                            }
                            if (table.columnNames().contains("closeTimestamp")) {
                                closeTimestamp = getTimeMillis(row, "closeTimestamp", timestamp);
                            } else if (table.columnNames().contains("closeTime")) {
                                closeTimestamp = getTimeMillis(row, "closeTime", timestamp);
                            }
                            Date dateTime = new Date(timestamp);

                            if (dateTime.before(startDate) || dateTime.after(endDate)) {
                                continue; // Skip candles outside the date range
                            }
                            Candle candle = new Candle(candleType,
                                    instrumentPk,
                                    row.getDouble("open"),
                                    row.getDouble("high"),
                                    row.getDouble("low"),
                                    row.getDouble("close"),
                                    timestamp,
                                    openTimestamp,
                                    closeTimestamp);
                            candles.putIfAbsent(instrumentPk, new ArrayList<>());
                            candles.get(candle.getInstrumentPk()).add(candle);
                        }
                    } catch (Exception e) {
                        System.err.println("Error reading candle file: " + candleFile + " - " + e.getMessage());
                    }
                }
            }
        }
        return candles;
    }

    private long getTimeMillis(Row row, String columnName, long fallback) {
        try {
            long value = row.getLong(columnName);
            return Math.abs(value) < 100_000_000_000L ? value * 1000L : value;
        } catch (IllegalArgumentException e) {
            try {
                double value = row.getDouble(columnName);
                long timestamp = (long) value;
                return Math.abs(timestamp) < 100_000_000_000L ? timestamp * 1000L : timestamp;
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }
    }

}
