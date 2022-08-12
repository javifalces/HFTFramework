package com.lambda.investing.algorithmic_trading.reinforcement_learning.state;

import com.lambda.investing.Configuration;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

@Getter @Setter class DumpCache {

	public static String CSV_SEPARATOR = ",";
	protected Logger logger = LogManager.getLogger(DumpCache.class);
	private NavigableMap<Long, RowDump> dumpMap;//last timestamp to trade
	private RowDump lastRow;

	public DumpCache() {
		this.dumpMap = Collections.synchronizedNavigableMap(new TreeMap<>());
	}

	public int getSize() {
		return this.dumpMap.size();
	}

	public int getNumberOfColumns() {
		return this.lastRow.getNumberColumns();
	}

	public void addRow(long timestamp, double[] startState, double startPrice, long[] timestampsEndPrices,
			double[] endPrices) {
		lastRow = new RowDump(timestamp, startState, startPrice, timestampsEndPrices, endPrices);
		this.dumpMap.put(lastRow.getTimestamp(), lastRow);

	}

	public String getCsvFileContent() {
		if (lastRow == null) {
			logger.warn("DumpCache is empty! nothing to return!");
			return null;
		}
		logger.info("transforming dumpMap size {} to string", dumpMap.size());
		StringBuilder fileContent = new StringBuilder();
		fileContent.append(lastRow.getHeader());
		fileContent.append(System.lineSeparator());
		for (RowDump rowDump : dumpMap.values()) {
			fileContent.append(rowDump.getRowCSV());
			fileContent.append(System.lineSeparator());
		}

		fileContent = fileContent.delete(fileContent.lastIndexOf(System.lineSeparator()),
				fileContent.length());//remove last line separator
		return fileContent.toString();

	}

	public void loadFromFile(String dataPath, int statesSize, int endSize) throws IOException {
		File file = new File(dataPath);
		if (!file.exists()) {
			logger.warn("dump file not found {}-> start empty", dataPath);
			return;
		}
		BufferedReader csvReader = new BufferedReader(new FileReader(dataPath));

		///Shuffle rows reading
		int numberOfRows = 0;

		String row;
		try {
			while ((row = csvReader.readLine()) != null) {
				if (numberOfRows == 0) {
					numberOfRows++;
					continue;
				}
				String[] data = row.split(CSV_SEPARATOR);
				//				if(data.length!=getNumberOfColumns()){
				//					logger.warn("data columns in {} {} != {} numberOfColums -> not loading file",dataPath,data.length,getNumberOfColumns());
				//					return;
				//				}

				long timestamp = Long.parseLong(data[0]);
				double[] startState = new double[statesSize];
				int offset = 1;
				for (int column = offset; column < statesSize + offset; column++) {
					startState[column - offset] = Double.parseDouble(data[column]);
				}

				offset += statesSize;
				double startPrice = Double.parseDouble(data[offset]);

				offset += 1;
				double[] endPrices = new double[endSize];
				for (int column = offset; column < endSize + offset; column++) {
					endPrices[column - offset] = Double.parseDouble(data[column]);
				}

				offset += endSize;
				long[] timestampEndPrices = new long[endSize];
				for (int column = offset; column < endSize + offset; column++) {
					timestampEndPrices[column - offset] = Long.parseLong(data[column]);
				}

				addRow(timestamp, startState, startPrice, timestampEndPrices, endPrices);
				numberOfRows++;
			}
		} catch (IOException ex) {
			logger.error("error reading row {} on {}", numberOfRows, dataPath, ex);
		}

	}

	@Getter @Setter private class RowDump {

		long timestamp;
		double[] startState;
		double startPrice;

		long[] timestampsEndPrices;
		double[] endPrices;

		public RowDump(long timestamp, double[] startState, double startPrice, long[] timestampsEndPrices,
				double[] endPrices) {
			this.timestamp = timestamp;
			this.startState = startState;
			this.startPrice = startPrice;
			this.timestampsEndPrices = timestampsEndPrices;
			this.endPrices = endPrices;
		}

		public int getNumberColumns() {
			return startState.length + endPrices.length + timestampsEndPrices.length + 2;
		}

		public String getHeader() {
			StringBuilder statesStr = new StringBuilder();
			for (int i = 0; i < startState.length; i++) {
				statesStr.append("state_").append(String.valueOf(i)).append(CSV_SEPARATOR);
			}

			StringBuilder endPricesStr = new StringBuilder();
			for (int i = 0; i < endPrices.length; i++) {
				endPricesStr.append("endPrice_").append(String.valueOf(i)).append(CSV_SEPARATOR);
			}

			StringBuilder endTimestampsStr = new StringBuilder();
			for (int i = 0; i < timestampsEndPrices.length; i++) {
				endTimestampsStr.append("endTimestamp_").append(String.valueOf(i)).append(CSV_SEPARATOR);
			}

			String header = Configuration
					.formatLog("timestamp,{}startPrice,{}{}", statesStr.toString(), endPricesStr.toString(),
							endTimestampsStr.toString());
			//remove last comma
			header = header.substring(0, header.length() - 1);
			return header;
		}

		public String getRowCSV() {
			StringBuilder output = new StringBuilder();
			output.append(timestamp).append(CSV_SEPARATOR);

			for (int i = 0; i < startState.length; i++) {
				output.append(startState[i]).append(CSV_SEPARATOR);
			}
			output.append(startPrice).append(CSV_SEPARATOR);

			for (int i = 0; i < endPrices.length; i++) {
				output.append(endPrices[i]).append(CSV_SEPARATOR);
			}
			for (int i = 0; i < timestampsEndPrices.length; i++) {
				output.append(timestampsEndPrices[i]).append(CSV_SEPARATOR);
			}
			String outputStr = output.toString().substring(0, output.length() - 1);
			return outputStr;
		}

	}

}