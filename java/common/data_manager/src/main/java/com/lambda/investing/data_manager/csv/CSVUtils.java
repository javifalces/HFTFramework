package com.lambda.investing.data_manager.csv;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.LongStream;

public class CSVUtils {

	public static final String DATE_COL_FILE = "C0";//"Timestamp";
	public static final String TIMESTAMP_COL = "timestamp";//"Timestamp";
	//	public static final String MARKET_COL = "Market";//orderbook or last_close
	//	public static final String ISIN_COL = "Isin";
	private static final long MAX_TIME_WAIT_MILLIS = 3000;
	private static DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
	private static DateTimeFormatter CSV_DATE_FORMAT_SHORT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	//	private static SimpleDateFormat CSV_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.z");//2019-10-16 11:32:27.195
	protected static final Logger logger = LogManager.getLogger(CSVUtils.class);

	public static Map<String, Instrument> PATH_TO_INSTRUMENT = new HashMap<>();

	public static Map<String, Object> getMap(Row row) {
		Map<String, Object> output = new HashMap<>();
		for (String column : row.columnNames()) {
			Object value = row.getObject(column);

			if (value != null) {

				try {
					String valueStr = ((String) value).trim();
					if (valueStr.isEmpty()) {
						continue;
					}
				} catch (Exception e) {
					;
				}
				try {
					int valueint = ((int) value);
					double valueDouble = (double) valueint;
					output.put(column, valueDouble);
					continue;
				} catch (Exception e) {
					;
				}

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

	public static Table readCSV(String completePath) throws IOException {
		try {
			logger.debug("reading csv {}", completePath);
			File readFile = new File(completePath);
			if (!readFile.exists()) {
				throw new IOException("File " + completePath + " not found to read csv");
			}
			Table output = Table.read().csv(completePath);
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

	/**
	 * @param tablesInput
	 * @return Map ordered by timestamp with a pair object name to row
	 */
	public static NavigableMap<Long, NameRowPair> mergeTables(Date startTime, Date endTime, Table... tablesInput) {
		NavigableMap<Long, NameRowPair> output = new TreeMap<>();
		for (Table table : tablesInput) {
			if (table == null) {
				continue;
			}
			for (Row row : table) {
				long timeColumn = row.getLong(TIMESTAMP_COL);
				if ((startTime.getTime() == endTime.getTime()) || (timeColumn >= startTime.getTime()
						&& timeColumn <= endTime.getTime())) {
					NameRowPair typeRow = new NameRowPair(table.name(), new Row(table, row.getRowNumber()),
							PATH_TO_INSTRUMENT.get(table.name()));
					output.put((long) timeColumn, typeRow);
				}
			}

		}

		return output;

	}

	public static NavigableMap<Long, NameRowPair> mergeTables(Date startTime, Date endTime, List<Table> inputTables) {
		//todo cache it?
		NavigableMap<Long, NameRowPair> output = new TreeMap<>();//is sorted
		for (Table table : inputTables) {
			if (table == null) {
				continue;
			}
			for (Row row : table) {
				long timeColumn = 0L;
				try {
					timeColumn = row.getLong(TIMESTAMP_COL);
				} catch (IllegalArgumentException e) {
					timeColumn = Long.valueOf(row.getInt(TIMESTAMP_COL));
				}
				if (timeColumn == 0L) {
					logger.error("error reading timestamp in table {}", table.name());
				}

				//adjusting timestamp
				if (String.valueOf(timeColumn).length() == 10) {
					timeColumn = timeColumn * 1000;
				}
				if ((startTime.getTime() == endTime.getTime()) || (timeColumn >= startTime.getTime()
						&& timeColumn <= endTime.getTime())) {
					NameRowPair typeRow = new NameRowPair(table.name(), new Row(table, row.getRowNumber()),
							PATH_TO_INSTRUMENT.get(table.name()));
					output.put((long) timeColumn, typeRow);
				}
			}

		}
		if (output.size() == 0) {
			System.err.println("Merging files result of empty table-> take another dates/instruments");
		}

		return output;

	}

	public static void sleepDifference(NavigableMap<Long, NameRowPair> readingTable, Long currentTimestamp,
			int speedMultiplier) {
		if (speedMultiplier < 0) {
			return;
		}
		try {
			Map.Entry<Long, NameRowPair> nextEntry = readingTable.higherEntry(currentTimestamp);
			if (nextEntry != null) {
				long nextTimeStamp = readingTable.higherEntry(currentTimestamp).getKey();
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
		String[] algorithmInfo = new String[levels];

		for (int level = 0; level < levels; level++) {
			double ask = (double) mapToUpdate.get("ask" + String.valueOf(level));
			double bid = (double) mapToUpdate.get("bid" + String.valueOf(level));
			double askQty = (double) mapToUpdate.get("ask_quantity" + String.valueOf(level));
			double bidQty = (double) mapToUpdate.get("bid_quantity" + String.valueOf(level));
			asks[level] = ask;
			bids[level] = bid;
			asksQty[level] = askQty;
			bidsQty[level] = bidQty;
			algorithmInfo[level] = algorithmInfoDepth;
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

	public static Trade createTrade(Row row, Instrument instrument, String algorithmInfoDepth) {
		Trade trade = new Trade();
		Map<String, Object> mapToUpdate = getMap(row);
		trade.setPrice((double) mapToUpdate.get("price"));
		trade.setQuantity((double) mapToUpdate.get("quantity"));
		trade.setInstrument(instrument.getPrimaryKey());
		trade.setTimestamp(getTimestamp(mapToUpdate));
		trade.setAlgorithmInfo(algorithmInfoDepth);
		return trade;
	}

	public static class NameRowPair {

		private String name;
		private Row row;
		private Instrument instrument;

		public NameRowPair(String name, Row row, Instrument instrument) {
			this.name = name;
			this.row = row;
			this.instrument = instrument;
		}

		public String getName() {
			return name;
		}

		public Row getRow() {
			return row;
		}

		public Instrument getInstrument() {
			return instrument;
		}
	}
}
