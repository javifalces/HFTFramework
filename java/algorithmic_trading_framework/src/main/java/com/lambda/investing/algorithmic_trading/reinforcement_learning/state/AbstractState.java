package com.lambda.investing.algorithmic_trading.reinforcement_learning.state;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.action.AvellanedaAction;
import com.lambda.investing.data_manager.csv.CSVDataManager;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.curator.shaded.com.google.common.collect.Queues;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.paukov.combinatorics3.Generator;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.stream.Collectors;

import static com.lambda.investing.data_manager.csv.CSVDataManager.removeEmptyLines;
import static org.apache.commons.math3.util.CombinatoricsUtils.binomialCoefficientDouble;

/**
 * https://softwareengineering.stackexchange.com/questions/286822/fast-indexing-of-k-combinations
 */
@Getter public abstract class AbstractState {

	protected static Double MAX_NUMBER = 10.;
	protected static Double MIN_NUMBER = -10.;

	private static String SEPARATOR_CACHE = ",";
	protected Logger logger = LogManager.getLogger(AbstractState.class);
	protected double maxNumber, minNumber, decimalDen = -1;
	protected int valuesPerColum, numberOfDecimals, numberOfColumns;
	private int referenceIndex = -1;
	private BiMap<Integer, StateRow> stateIndexToArr;

	protected String[] columnsFilter = null;

	protected Queue<Trade> buyTrades, sellTrades;
	protected volatile Object lockTradesList = new Object();

	protected DumpCache dumpCache = new DumpCache();
	protected Map<String, Integer> columnStateToIndex = new HashMap<>();

	public void setColumnsFilter(String[] columnsFilter) {
		this.columnsFilter = columnsFilter;
		updateMaxHorizon();

		updateColumnStateIndex();

	}

	protected int getMaxHorizon(List<String> columnsFilter, String[] columns) {
		int maxSuffix = 0;
		List<String> columnsList = Arrays.asList(columns);
		for (String column : columnsFilter) {
			String[] split = column.split("_");
			if (split.length > 1) {
				try {
					int lastIndex = split.length - 1;
					//join all except last
					String prefix = Arrays.stream(split).limit(lastIndex).collect(Collectors.joining("_"));
					if (columnsList.contains(prefix)) {
						int suffix = Integer.parseInt(split[lastIndex]);
						if (suffix > maxSuffix) {
							maxSuffix = suffix;
						}
					}

				} catch (Exception e) {
					logger.error("Error parsing column {} ", column);
				}
			}
		}
		return maxSuffix;
	}

	protected void updateMaxHorizon() {
	}

	private void updateColumnStateIndex() {
		List<String> allColumns = getColumns();
		logger.info("updating column state index for {} columns", allColumns.size());
		int index = 0;
		for (String column : allColumns) {
			columnStateToIndex.put(column, index);
			index++;
		}
	}

	public abstract void calculateNumberOfColumns();

	public boolean isFiltered() {
		return columnsFilter != null && this.columnsFilter.length > 0;
	}

	public int getNumberOfColumns() {
		if (!isFiltered()) {
			if (numberOfColumns == 0) {
				calculateNumberOfColumns();
			}
			return numberOfColumns;
		} else {
			return this.columnsFilter.length;
		}
	}

	protected double[] getFilteredState(double[] inputState) {
		if (this.columnsFilter == null) {
			return inputState;
		}
		if (this.columnStateToIndex == null || this.columnStateToIndex.isEmpty()) {
			updateColumnStateIndex();
		}

		List<Double> filteredState = new ArrayList<>();
		for (String filteredColumn : columnsFilter) {
			double state = 0.0;
			try {
				int indexInAllStates = columnStateToIndex.get(filteredColumn);
				state = inputState[indexInAllStates];
			} catch (Exception e) {
				logger.error("error getting state for column " + filteredColumn + " from state [" + inputState.length + "] " + Arrays.toString(inputState) + " set to 0.0", e);
			}
			filteredState.add(state);
		}
		double[] output = ArrayUtils.toPrimitive(filteredState.toArray(new Double[0]));
		return output;
	}

	public int getDumpSize() {
		return dumpCache.getSize();
	}

	public int getDumpColumns() {
		return dumpCache.getNumberOfColumns();
	}

	public List<String> getFilteredColumns() {
		return getColumns();
	}

	/***
	 * Get column names before filtering ITS important to get in the same order as the state , because we filter same order
	 * @return
	 */
	public abstract List<String> getColumns();

	public abstract int getNumberStates();

	public abstract void reset();

	public abstract boolean isReady();

	/**
	 * @return state array filtered but not rounded
	 */
	public abstract double[] getCurrentState();

	public double[] getCurrentStateSafe() {
		try {
			return getCurrentState();
		} catch (Exception e) {
			int numberOfColumns = getNumberOfColumns();
			logger.error("error getting current state -> return all zeros array of size {}", numberOfColumns, e);
			double[] zeros = new double[numberOfColumns];
			return zeros;
		}
	}
	public abstract int getCurrentStatePosition();

	public abstract void enumerateStates(String cachePermutationsPath);

	public AbstractState(int numberOfDecimals) {
		this.numberOfDecimals = numberOfDecimals;
		//		this.numberOfColumns=numberOfColumns;
		if (numberOfDecimals > 0) {
			decimalDen = Math.pow(10, numberOfDecimals);
		}
		stateIndexToArr = HashBiMap.create();

		//trades
		buyTrades = Queues.synchronizedQueue(EvictingQueue.create(200));
		sellTrades = Queues.synchronizedQueue(EvictingQueue.create(200));

	}

	protected void cleanTrades() {
		synchronized (lockTradesList) {
			buyTrades.clear();
			sellTrades.clear();
		}
		//		tradePrices= Queues.synchronizedQueue(EvictingQueue.create(200));
		//		tradeSizes= Queues.synchronizedQueue(EvictingQueue.create(200));
		//		tradeVerb=Queues.synchronizedQueue(EvictingQueue.create(200));
	}

	public void setNumberOfDecimals(int numberOfDecimals) {
		this.numberOfDecimals = numberOfDecimals;
	}

	public void loadCacheFile(String filePath) {
		stateIndexToArr = loadMapFromFile(filePath);
	}

	public void saveCacheFile(String filePath) {
		saveMapToFile(stateIndexToArr, filePath);
	}

	public void deleteCacheFile(String filePath) {
		File file = new File(filePath);
		file.delete();
	}

	protected void fillCache(String cachePermutationsPath) {
		stateIndexToArr = loadMapFromFile(cachePermutationsPath);
		if (stateIndexToArr.size() > 0) {
			logger.info("load cache permutations {}  of size {}", cachePermutationsPath, stateIndexToArr.size());
			return;
		}
		logger.info("Calculating cachePermutations to save in " + cachePermutationsPath);

		double[] array = new double[numberOfColumns];

		List<Double> values = new ArrayList<>();
		for (int i = 0; i < valuesPerColum; i++) {
			values.add(getColumnValue(i));
		}

		assert values.contains(-5E-7);

		System.out.println(
				"calculating permutations on " + values.size() + " permutations taken by " + numberOfColumns + " -> "
						+ getNumberStates() + " possibilities to save on " + cachePermutationsPath);
		long start = System.currentTimeMillis();
		List<List<Double>> permutations = Generator.permutation(values).withRepetitions(numberOfColumns).stream()
				.collect(Collectors.<List<Double>>toList());
		long elapsedMinutes = (System.currentTimeMillis() - start) / (1000 * 60);
		System.out.println("done in " + elapsedMinutes + " minutes");
		for (List<Double> arrayPermuta : permutations) {
			double[] arrayValues = ArrayUtils.toPrimitive(arrayPermuta.toArray(new Double[numberOfColumns]));
			stateIndexToArr.put(permutations.indexOf(arrayPermuta), new StateRow(arrayValues));
		}

		//		double sumPosition = 0.;
		//		double maxPosition = maxNumber * numberOfColumns;
		//		while (sumPosition < maxPosition) {
		//			int calculatedPos = getStateFromArray(array);
		//			incrementArray(array, 1);
		//
		//			sumPosition = 0.;
		//			for (int column = 0; column < array.length; column++) {
		//				sumPosition += array[column];
		//			}
		//
		//		}

		saveCacheFile(cachePermutationsPath);
		logger.info("saved cache permutations {}  of size {}", cachePermutationsPath, stateIndexToArr.size());

	}

	public void setNumberOfColumns(int numberOfColumns) {
		this.numberOfColumns = numberOfColumns;
	}

	protected int getColumnPosition(double roundedNumber) {
		double value = roundedNumber + Math.floor(minNumber);
		int positionInColumn = (int) Math.round(value * decimalDen);
		return positionInColumn;
	}

	protected double getColumnValue(int position) {
		double value = position / decimalDen + minNumber;
		value = Math.min(value, maxNumber);
		value = Math.max(value, minNumber);
		value = round(value, decimalDen);

		return value;
	}

	public static double round(double input, double decimalDen) {
		return ((Math.round(input * decimalDen)) / decimalDen);
	}
	//	public static double roundBD(double input, int numberOfDecimals)
	//	{
	//		BigDecimal in= new BigDecimal(input);
	//		in=in.setScale(numberOfDecimals, BigDecimal.ROUND_HALF_UP);
	//		return in.doubleValue();
	//	}

	protected static double[] getRoundedState(double[] state, double maxNumber, double minNumber, int numDecimals) {
		if (maxNumber == -1 && minNumber == -1 && numDecimals == -1) {
			//directly
			return state;
		}

		double[] stateRound = new double[state.length];
		double decimalDen = 0;
		if (numDecimals > 0) {
			decimalDen = Math.pow(10, numDecimals);
		}
		for (int column = 0; column < state.length; column++) {
			double value = state[column];
			if (maxNumber > minNumber) {
				value = Math.min(maxNumber, state[column]);
				value = Math.max(minNumber, value);
			}
			if (decimalDen != 0) {
				value = round(value, decimalDen);
			}
			stateRound[column] = value;
		}
		return stateRound;

	}

	protected double[] getRoundedState(double[] state) {
		return getRoundedState(state, maxNumber, minNumber, numberOfDecimals);
	}

	//	private double[] getRoundedStateBD(double[] state) {
	//		double[] stateRound = new double[state.length];
	//		for (int column = 0; column < state.length; column++) {
	//			double value = Math.min(maxNumber, state[column]);
	//			value = Math.max(minNumber, value);
	//			value = roundBD(value ,numberOfDecimals);
	//			stateRound[column] = value;
	//		}
	//		return stateRound;
	//
	//	}

	/////

	public double[] getCurrentStateRounded() {
		double[] outputArr = getCurrentStateSafe();
		if (outputArr == null) {
			return outputArr;
		}
		return getRoundedState(outputArr);
	}

	protected int getStateFromArray(double[] state) {
		double[] stateRounded = getRoundedState(state);
		Integer cachePos = stateIndexToArr.inverse().get(new StateRow(stateRounded));
		if (cachePos != null) {
			return cachePos;
		}
		//		double[] stateRounded2 = getRoundedStateBD(state);
		//		Integer cachePos2 = stateIndexToArr.inverse().get(new StateRow(stateRounded2));
		//		if (cachePos2 != null) {
		//			return cachePos2;
		//		}

		//		logger.error("cant calculate , only working in cache mode!");
		return -1;

		//		int out = 0;
		//		int sumStatesIndex = 0;
		//		for (int columnState = 0; columnState < state.length; columnState++) {
		//			double positionColumn = 0;
		//			try {
		//				int columnIndex = getColumnPosition(stateRounded[columnState]);
		//				sumStatesIndex += columnIndex;
		//				positionColumn = binomialCoefficientDouble(valuesPerColum - columnIndex, columnState + 1);
		//
		////				permutation with repeatition
		////				positionColumn = Math.pow(valuesPerColum-columnIndex,columnState);
		//
		//			} catch (NumberIsTooLargeException ex) {
		//			}
		//
		//			out += positionColumn;
		//
		//		}

		//		if (sumStatesIndex==0){
		//			referenceIndex=out;
		//		}
		//		if (referenceIndex<0){
		//			logger.warn("reference index is <0 => calculating reference");
		//			getStateFromArray(new double[numberOfColumns]);
		//		}
		//		out = out-referenceIndex;

		//		stateIndexToArr.put((int) out, new StateRow(stateRounded));
		//		return (int) out;
	}

	public void incrementArray(double[] inputArray, int increment) {
		int columnTry = 0;
		double valueToPut = 0;
		if (increment > 0) {
			//check we are in maximun
			double sum = 0;
			for (int columns = 0; columns < numberOfColumns; columns++) {
				sum += inputArray[columns];
			}
			if (sum == maxNumber * numberOfColumns) {
				for (int columns = 0; columns < numberOfColumns; columns++) {
					inputArray[columns] = minNumber;
				}
				return;
			}

			while (inputArray[columnTry] == maxNumber) {
				columnTry++;
				for (int columns = columnTry - 1; columns >= 0; columns--) {
					inputArray[columns] = minNumber;
				}
			}

			int positionOfThisValue = getColumnPosition(inputArray[columnTry]);
			valueToPut = getColumnValue(positionOfThisValue + 1);
			inputArray[columnTry] = valueToPut;

		}

		if (increment < 0) {
			//check we are in min
			double sum = 0;
			for (int columns = 0; columns < numberOfColumns; columns++) {
				sum += inputArray[columns];
			}
			if (sum == minNumber * numberOfColumns) {
				for (int columns = 0; columns < numberOfColumns; columns++) {
					inputArray[columns] = maxNumber;
				}
				return;
			}

			while (inputArray[columnTry] == minNumber) {
				columnTry++;
				for (int columns = columnTry - 1; columns >= 0; columns--) {
					inputArray[columns] = minNumber;
				}
			}

			int positionOfThisValue = getColumnPosition(inputArray[columnTry]);
			valueToPut = getColumnValue(positionOfThisValue - 1);
			inputArray[columnTry] = valueToPut;

		}

	}

	protected double[] getState(int statePosition) {
		if (numberOfColumns == 0) {
			logger.error("need to set number of columns first!");
		}
		//working stimation

		double[] stateRow = stateIndexToArr.get(statePosition).inputArray;
		if (stateRow != null) {
			return stateRow;
		}

		int closestCachePosition = 9999999;
		if (stateIndexToArr.size() == 0) {
			double[] zeroArra = new double[numberOfColumns];
			closestCachePosition = getStateFromArray(zeroArra);
		}

		for (int stateCache : stateIndexToArr.keySet()) {
			if (Math.abs(stateCache - statePosition) < closestCachePosition) {
				closestCachePosition = stateCache;
			}
		}
		double[] closestArray = stateIndexToArr.get(closestCachePosition).inputArray;
		double[] input = closestArray;

		while (closestCachePosition != statePosition) {

			if (closestCachePosition < statePosition) {
				incrementArray(input, -1);
			} else {
				//increment  one position
				incrementArray(input, 1);
			}

			closestCachePosition = getStateFromArray(input);

		}

		double[] output = input;
		return output;

	}

	private static String getStringRow(double[] input) {
		StringBuilder outSB = new StringBuilder();

		for (int column = 0; column < input.length; column++) {
			outSB.append(input[column]);
			outSB.append(SEPARATOR_CACHE);
		}

		return outSB.toString();
	}

	public static void saveMapToFile(BiMap<Integer, StateRow> map, String filePath) {
		//new file object
		File file = new File(filePath);
		if (file.getParentFile() != null) {
			file.getParentFile().mkdirs();
		}
		BufferedWriter bf = null;
		try {
			//create new BufferedWriter for the output file
			bf = new BufferedWriter(new FileWriter(file));

			//iterate map entries
			for (Map.Entry<Integer, StateRow> entry : map.entrySet()) {
				//put key and value separated by a colon
				bf.write(entry.getKey() + ":" + getStringRow(entry.getValue().inputArray));
				//new line
				bf.newLine();
			}

			bf.flush();

		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			try {
				//always close the writer
				bf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public static BiMap<Integer, StateRow> loadMapFromFile(String filePath) {
		BiMap<Integer, StateRow> output = HashBiMap.create();
		File file = new File(filePath);
		if (file.exists()) {
			try {

				BufferedReader csvReader = new BufferedReader(new FileReader(filePath));
				String row;
				while ((row = csvReader.readLine()) != null) {
					String[] data = row.split(":");
					int index = Integer.valueOf(data[0]);
					String[] stringsSplitted = data[1].split(SEPARATOR_CACHE);
					double[] arrayValues = new double[stringsSplitted.length];
					int counter = 0;
					for (String element : stringsSplitted) {
						arrayValues[counter] = Double.valueOf(element);
						counter++;
					}
					output.put(index, new StateRow(arrayValues));

				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return output;

	}

	public abstract void updateCandle(Candle candle);

	public abstract void updateTrade(Trade trade);

	public abstract void updatePrivateState(PnlSnapshot pnlSnapshot);

	public abstract void updateDepthState(Depth depth);

	//trades results
	protected void updateTradesBuffer(Trade trade) {
		synchronized (lockTradesList) {
			if (trade.getVerb().equals(Verb.Buy)) {
				buyTrades.add(trade);
			} else if (trade.getVerb().equals(Verb.Sell)) {
				sellTrades.add(trade);

			} else {
				logger.warn("trying to add trade with unknown verb {} :{}", trade.getVerb(), trade);
			}
		}
	}

	protected List<Trade> getBuyTrades() {
		return new ArrayList<>(buyTrades);
	}

	protected List<Trade> getSellTrades() {
		return new ArrayList<>(sellTrades);
	}

	protected double getSignedTransactionVolume() {
		double totalVolume = 0.0;
		for (Trade trade : getBuyTrades()) {
			totalVolume += trade.getQuantity();
		}
		for (Trade trade : getSellTrades()) {
			totalVolume -= trade.getQuantity();
		}
		return totalVolume;
	}

	protected double getSignedTransaction() {
		double totalTransactions = 0.0;
		for (Trade trade : getBuyTrades()) {
			totalTransactions++;

		}
		for (Trade trade : getSellTrades()) {
			totalTransactions--;
		}
		return totalTransactions;
	}

	public static class StateRow {

		private double[] inputArray;

		public StateRow(double... inputs) {
			this.inputArray = inputs;
		}

		@Override public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof StateRow))
				return false;
			StateRow stateRow = (StateRow) o;
			return Arrays.equals(inputArray, stateRow.inputArray);
		}

		@Override public int hashCode() {
			return Arrays.hashCode(inputArray);
		}
	}

	public void addRowDump(long startTimestamp, double[] state, double startPrice, long[] timestampsEndPrices,
						   double[] endPrices) {
		dumpCache.addRow(startTimestamp, state, startPrice, timestampsEndPrices, endPrices);
	}

	public boolean loadPreviousDumpData(String dataPath, int endSize) {
		try {
			System.out.println("loading dumpCache from " + dataPath + " ...");
			dumpCache.loadFromFile(dataPath, getNumberOfColumns(), endSize);
			System.out.println("loaded");
			return true;
		} catch (Exception e) {
			System.err.println("error loading previousDump " + dataPath);
			logger.error("error loading previousDump  onf {} columns and {} seconds {}", getColumns(), endSize,
					dataPath, e);
			return false;
		}
	}

	public boolean persistDumpData(String dataPath) {
		String textToWriteTrade = dumpCache.getCsvFileContent();
		try {
			if (textToWriteTrade.trim().length() > 0) {
				CSVDataManager.saveCSV(dataPath, removeEmptyLines(textToWriteTrade));
			}
			return true;
		} catch (IOException e) {
			logger.error("{} can't be write it!", dataPath);
			return false;
		}

	}

}
