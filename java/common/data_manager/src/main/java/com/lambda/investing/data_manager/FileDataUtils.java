package com.lambda.investing.data_manager;

import com.lambda.investing.Configuration;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;

import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.selection.Selection;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.lambda.investing.model.market_data.Depth.ALGORITHM_INFO_MM;

public class FileDataUtils {
    public static String TYPE_COLUMN = "type";

    public static String PATH_COLUMN = "path";

    public static String INSTRUMENT_COLUMN = "instrument_pk";

    public static final String DATE_COL_FILE = "C0";//"Timestamp";
    public static final String TIMESTAMP_COL = "timestamp";//"Timestamp";
    public static final String MS_TO_NEXT_UPDATE_COL = "ms_to_next_update";//"Timestamp";

    //	public static final String MARKET_COL = "Market";//orderbook or last_close
    //	public static final String ISIN_COL = "Isin";
    private static final long MAX_TIME_WAIT_MILLIS = 3000;
    private static DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private static DateTimeFormatter CSV_DATE_FORMAT_SHORT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    //	private static SimpleDateFormat CSV_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.z");//2019-10-16 11:32:27.195
    protected static final Logger logger = LogManager.getLogger(FileDataUtils.class);
    private static int TIMESTAMP_LENGTH = 13;
    public static Map<String, Instrument> PATH_TO_INSTRUMENT = new HashMap<>();

    public static Map<String, Object> getMap(Row row) {
        Map<String, Object> output = new HashMap<>();
        for (String column : row.columnNames()) {
            Object value = row.getObject(column);

            if (value instanceof String) {
                String valueStr = ((String) value).trim();
                if (!valueStr.isBlank()) {
                    output.put(column, valueStr);
                }
            } else if (value instanceof Integer) {
                output.put(column, ((Integer) value).doubleValue());
            } else if (value != null) {
                output.put(column, value);
            }

            if (column.equalsIgnoreCase(TIMESTAMP_COL)) {
                Long valueL = (Long) row.getObject(column);
                if (String.valueOf(valueL).length() == 10) {
                    output.put(column, (valueL * 1000));
                }
                //				output.put(column, (int) (valueL / 1000));//ion?
            }

        }

        return output;

    }

    public static LongColumn getTimestampColumnFromDate(Table output) {
        int timestampIndex = output.columnIndex(DATE_COL_FILE);
        TextColumn dateColumnString = (TextColumn) output.column(timestampIndex);
        List<LocalDateTime> dateFormat = new ArrayList<>();
        List<Long> timestamps = new ArrayList<>();
        //iterating to map it

        for (String dateString : dateColumnString) {
            LocalDateTime dateTime = null;
            try {
                dateTime = LocalDateTime.parse(dateString, CSV_DATE_FORMAT);
                dateFormat.add(dateTime);
            } catch (DateTimeParseException exception) {
                dateTime = LocalDateTime.parse(dateString, CSV_DATE_FORMAT_SHORT);
                dateFormat.add(dateTime);
            }
            timestamps.add(dateTime.toEpochSecond(ZoneOffset.UTC));
        }
        DateTimeColumn sc = DateTimeColumn.create(DATE_COL_FILE, dateFormat);
        output.removeColumns(DATE_COL_FILE);
        output.addColumns(sc);
        Long[] tsArr = timestamps.toArray(new Long[timestamps.size()]);
        LongColumn column = LongColumn.create(TIMESTAMP_COL, ArrayUtils.toPrimitive(tsArr));
        return column;
    }

    public static LongColumn getTimestampColumn(Table output) {
        int timestampIndex = output.columnIndex(DATE_COL_FILE);
        LongColumn timestampColumn = (LongColumn) output.column(timestampIndex);
        return timestampColumn;
    }

    public static Table readCSVRaw(String completePath) throws IOException {
        try {
            logger.debug("reading csv {}", completePath);
            File readFile = new File(completePath);
            if (!readFile.exists()) {
                throw new IOException("File " + completePath + " not found to read csv");
            }
            Table output = Table.read().csv(completePath);
            return output;
        } catch (Exception e) {
            logger.error("cant read csv {}", completePath, e);
            throw e;
        }
    }

    public static Table readCSV(String completePath) throws IOException {
        try {
            Table output = readCSVRaw(completePath);
            if (!output.columnNames().contains(TIMESTAMP_COL)) {
                //old format create timestamp from date
                LongColumn column = getTimestampColumnFromDate(output);
                output.addColumns(column);
            }
            output = output.sortAscendingOn(TIMESTAMP_COL);
            return output;
        } catch (Exception e) {
            logger.error("cant read csv {}", completePath, e);
            throw e;
        }
    }


    private static Table AddColumnsEmpty(Table source, Table target) {
        Table emptySource = source.emptyCopy();
        for (Column in : source.columns()) {
            if (target.containsColumn(in.name())) {
                continue;
            }
            //add this new column to output
            Column out = emptySource.column(in.name());
            target = target.addColumns(out);
        }
        return target;
    }

    public static Table createTableMerged(Date startTime, Date endTime, List<Table> tablesInput) {
        long start = new Date().getTime();
        Table output = null;
        for (Table table : tablesInput) {
            if (table == null) {
                continue;
            }
            if (output == null) {
                output = table;
            } else {
                output = AddColumnsEmpty(table, output);
                table = AddColumnsEmpty(output, table);

                output = output.append(table);
            }
        }
        if (output == null) {
            System.err.println("createTableMerged: no data to merge");

            return null;
        }
        int rowsBefore = output.rowCount();
        output = output.setName("merged");
        output = output.sortAscendingOn(TIMESTAMP_COL);
        output = output.where(output.longColumn(TIMESTAMP_COL).isBetweenInclusive(startTime.getTime(), endTime.getTime()));
        if (output.rowCount() == 0) {
            System.err.println(Configuration.formatLog("createTableMerged: no data between {} and {} before had {} rows ", startTime.toString(), endTime.toString(), rowsBefore));
            return null;
        }
        long end = new Date().getTime();
        long elapsedSeconds = (end - start) / 1000;
        System.out.println(Configuration.formatLog("Merged {} tables from {} to {} with {} rows  in {} seconds", tablesInput.size(), startTime.toString(), endTime.toString(), output.rowCount(), elapsedSeconds));
        return output;

    }

    @Getter
    private static class TupleTradeCache {

        private long timestamp;
        private double price;

        public TupleTradeCache(long timestamp, double price) {
            this.timestamp = timestamp;
            this.price = price;
        }
    }

    public static Table AdjustTradeTimestamp(Table mergedTable, long maxMsSearch) {
        long start = new Date().getTime();
        StringColumn columnType = mergedTable.stringColumn(TYPE_COLUMN);
        Selection selectionType = columnType.isEqualTo("trade");

        Table tradesTable = mergedTable.where(selectionType);
        int foundDepthTrades = 0;
        List<Long> adjustedTimestampList = new ArrayList<>();

        Map<TupleTradeCache, Long> cacheMap = new HashMap<>();

        if (tradesTable.rowCount() > 1E5) {
            System.out.println("AdjustTradeTimestamp: too many trades to adjust, skipping");
            return mergedTable;
        }

        try (ProgressBar pb = new ProgressBar("AdjustTradeTimestamp", tradesTable.rowCount(), 1000, System.err,
                ProgressBarStyle.ASCII, "", 1L, false, (DecimalFormat) null, ChronoUnit.SECONDS, 0L,
                Duration.ZERO)) {

            for (int rowIndex = 0; rowIndex < tradesTable.rowCount(); rowIndex++) {
                Row row = tradesTable.row(rowIndex);
                long tradeTimestamp = row.getLong(TIMESTAMP_COL);
                String instrumentPk = row.getString(INSTRUMENT_COLUMN);
                Instrument instrument = Instrument.getInstrument(instrumentPk);
                Trade trade = FileDataUtils.createTrade(row, instrument, ALGORITHM_INFO_MM);

                TupleTradeCache key = new TupleTradeCache(tradeTimestamp, trade.getPrice());
                long adjustedTimestamp = -1;
                if (!cacheMap.containsKey(key)) {
                    adjustedTimestamp = AdjustTradeTimestamp(tradeTimestamp, mergedTable, trade, maxMsSearch);
                    cacheMap.put(key, adjustedTimestamp);
                } else {
                    adjustedTimestamp = cacheMap.get(key);
                }

                if (adjustedTimestamp != -1) {
                    row.setLong(TIMESTAMP_COL, adjustedTimestamp);//new column
                    adjustedTimestampList.add(tradeTimestamp - adjustedTimestamp);
                    foundDepthTrades++;
                }
                if (rowIndex % 10 == 0) {
                    double percent = (foundDepthTrades * 100.0) / ((double) rowIndex);
                    pb.setExtraMessage(String.format("%.2f found", percent));
                }

                pb.step();
            }
        } catch (Exception e) {
            logger.error("unknown error AdjustTradeTimestamp progress", e);
        }
        double percent = (foundDepthTrades * 100.0) / ((double) tradesTable.rowCount());
        double meanAdjusted = adjustedTimestampList.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long secondsElapsed = (new Date().getTime() - start) / 1000;
        String message = String.format("Adjusted %d/%d trades in %d seconds :  mean slippage %.2f ms from %d/%d synchronized with depth %.2f pct", tradesTable.rowCount(), mergedTable.rowCount(), secondsElapsed, meanAdjusted, foundDepthTrades, tradesTable.rowCount(), percent);
        logger.info(message);
        System.out.println(message);

        return mergedTable;


    }

    private static long AdjustTradeTimestamp(long tradeTimestamp, Table output, Trade trade, long maxMsSearch) {

        //if price is the same and timestamp is the same or less than current timestamp
        String instrumentPk = trade.getInstrument();
        Instrument instrument = Instrument.getInstrument(instrumentPk);

        //query depth data closest to trade sorted from closest to farthest
        StringColumn columnType = output.stringColumn(TYPE_COLUMN);
        Selection selectionType = columnType.isEqualTo("depth");

        StringColumn columnInstrument = output.stringColumn(INSTRUMENT_COLUMN);
        Selection selectionInstrument = columnInstrument.isEqualTo(instrumentPk);

        LongColumn columnTimestamp = output.longColumn(TIMESTAMP_COL);
        Selection selectionTimestamp = columnTimestamp.isBetweenInclusive(tradeTimestamp - maxMsSearch, tradeTimestamp);

        Table subsampleSortedDescending = output.where(selectionType.and(selectionInstrument).and(selectionTimestamp)).
                sortDescendingOn(TIMESTAMP_COL);
        subsampleSortedDescending = subsampleSortedDescending.dropDuplicateRows();

        if (subsampleSortedDescending.rowCount() == 0) {
            return -1;
        }

        //just in case.... avoid long loads
        long maxRows = Math.min(maxMsSearch * 2, subsampleSortedDescending.rowCount());
        if (maxRows < subsampleSortedDescending.rowCount()) {
            System.out.println(Configuration.formatLog("WARNING : on trade {} there are more than {} depth candidates to adjust  ", trade, maxMsSearch * 2));
            subsampleSortedDescending = subsampleSortedDescending.sampleN((int) maxRows - 1);
        }


        double tradePrice = trade.getPrice();
        Verb tradeVerb = trade.getVerb();
        boolean foundIt = false;
        long newTradeTimestamp = tradeTimestamp;
        for (Row entry : subsampleSortedDescending) {
            //check if entry is depth
            try {
                Date depthDate = new Date((long) entry.getLong(TIMESTAMP_COL));
                Depth depth;
                try {
                    depth = createDepth(entry, instrument, ALGORITHM_INFO_MM);
                } catch (NullPointerException e) {
                    continue;
                }
                double bestBid = depth.getBestBid();
                double bestAsk = depth.getBestAsk();
                boolean tradePriceIsBest = false;
                if (tradeVerb != null) {
                    if (tradeVerb == Verb.Buy) {
                        //hit the bid trade
                        tradePriceIsBest = (bestBid == tradePrice);
                    }
                    if (tradeVerb == Verb.Sell) {
                        //lift the offer trade
                        tradePriceIsBest = (bestAsk == tradePrice);
                    }
                } else {
                    tradePriceIsBest = (bestAsk == tradePrice || bestBid == tradePrice);
                }


                if (tradePriceIsBest) {
                    //adjust timestamp
                    newTradeTimestamp = depthDate.getTime() - 1;
                    foundIt = true;
                    break;
                }

            } catch (Exception e) {
                logger.error("Error adjusting timestamp for trade searching {} ms :{}", maxMsSearch, trade, e);
            }
        }

        if (!foundIt) {
//			logger.warn("Could not adjust timestamp for trade searching {} ms :{}",maxMsSearch, trade);
            return -1;
        }

        return newTradeTimestamp;
    }


    public static void sleepDifference(Table readingTable, int rowIndex, Long currentTimestamp,
                                       int speedMultiplier) {
        if (speedMultiplier < 0) {
            return;
        }
        try {
            Row nextEntry = readingTable.row(rowIndex + 1);
            if (nextEntry != null) {
                long nextTimeStamp = nextEntry.getLong(TIMESTAMP_COL);
                //TODO take a look on speedMultiplier>0
                long waitMillis = ((nextTimeStamp - currentTimestamp) / speedMultiplier);
                if (waitMillis > MAX_TIME_WAIT_MILLIS) {
                    //					logger.warn("Waiting {} miliseconds  more than {} miliseconds -> reduce",waitMillis, MAX_TIME_WAIT_MILLIS);
                    waitMillis = MAX_TIME_WAIT_MILLIS;
                }
                Thread.sleep(waitMillis);
            }
        } catch (InterruptedException e) {
            logger.error("cant wait next row ", e);
        }
    }

    private static long getTimestamp(Map<String, Object> mapToUpdate) {
        Object value = mapToUpdate.get(TIMESTAMP_COL);
        if (value instanceof Long) {
            return (Long) value;
        }
        Double valuedob = (Double) value;
        return (valuedob.longValue());

    }

    public static Depth createDepth(Row row, Instrument instrument, String algorithmInfoDepth) {
        Depth depth = new Depth();
        Map<String, Object> mapToUpdate = getMap(row);

        //		Instrument instrument = CSVFileConfiguration.getInstrument();
        int levels = mapToUpdate.size() / 4;
        depth.setInstrument(instrument.getPrimaryKey());

        Double[] asks = new Double[levels];
        Double[] bids = new Double[levels];
        Double[] asksQty = new Double[levels];
        Double[] bidsQty = new Double[levels];
        List<String>[] algorithmInfo = new List[levels];

        for (int level = 0; level < levels; level++) {
            asks[level] = 0.0;
            bids[level] = 0.0;
            asksQty[level] = 0.0;
            bidsQty[level] = 0.0;
            algorithmInfo[level] = new ArrayList<>() {
            };


            try {
                boolean isAsk = mapToUpdate.containsKey("ask" + String.valueOf(level));
                boolean isBid = mapToUpdate.containsKey("bid" + String.valueOf(level));
                if (isAsk) {
                    double ask = (double) mapToUpdate.get("ask" + String.valueOf(level));
                    double askQty = (double) mapToUpdate.get("ask_quantity" + String.valueOf(level));
                    asks[level] = ask;
                    asksQty[level] = askQty;
                } else {
                    if (level == 0) {
                        asks[level] = Double.MAX_VALUE;
                        asksQty[level] = 0.0;
                    }
                }

                if (isBid) {
                    double bid = (double) mapToUpdate.get("bid" + String.valueOf(level));
                    double bidQty = (double) mapToUpdate.get("bid_quantity" + String.valueOf(level));
                    bids[level] = bid;
                    bidsQty[level] = bidQty;
                } else {
                    if (level == 0) {
                        bids[level] = Double.MIN_VALUE;
                        bidsQty[level] = 0.0;
                    }
                }

                algorithmInfo[level].add(algorithmInfoDepth);

            } catch (Exception e) {
                if (level == 0) {
                    //level 0 requires all data!
                    throw e;
                }
            }

        }
        depth.setTimestamp(getTimestamp(mapToUpdate));
        depth.setAsks(asks);
        depth.setBids(bids);
        depth.setAsksAlgorithmInfo(algorithmInfo);
        depth.setBidsAlgorithmInfo(algorithmInfo);
        depth.setAsksQuantities(asksQty);
        depth.setBidsQuantities(bidsQty);
        depth.setLevelsFromData();
        return depth;
    }

    public static Trade createTrade(Row row, Instrument instrument, String algorithmInfo) {
        Trade trade = new Trade();
        Map<String, Object> mapToUpdate = getMap(row);
        trade.setPrice((double) mapToUpdate.get("price"));
        trade.setQuantity((double) mapToUpdate.get("quantity"));
        trade.setInstrument(instrument.getPrimaryKey());
        trade.setTimestamp(getTimestamp(mapToUpdate));
        trade.setAlgorithmInfo(algorithmInfo);
        return trade;
    }

}
