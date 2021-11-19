package com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn.Dl4jMemoryReplayModel;
import com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn.MemoryReplayModel;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.action.AbstractAction;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.exploration_policy.EpsilonGreedyExploration;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.AbstractState;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;

@Getter @Setter public class DeepQLearning extends QLearning {

	public static boolean USE_AS_QLEARN = false;

	public static boolean TRAIN_FROM_FILE_MODEL = false;
	private static Comparator<double[]> ARRAY_COMPARATOR = Doubles.lexicographicalComparator();
	protected static Logger logger = LogManager.getLogger(DeepQLearning.class);
	public static double DEFAULT_PREDICTION_ACTION_SCORE = 0;
	private BiMap<StateRow, Integer> indexToStateCache;

	private static int MAX_MEMORY_SIZE = (int) 1E6;
	private double epsilon;
	private Random r = new Random();
	private AbstractState state;
	private AbstractAction action;
	private int memoryReplayIndex, maxMemorySize, memoryReplaySize;
	private StateRow[] stateRowSet;
	private MemoryReplayModel predictModel, targetModel;
	private boolean isRNN;
	private double[] defaultActionPredictScore;

	/**
	 * Initializes a new instance of the QLearning class.
	 *
	 * @param state             class states.
	 * @param action            class actions.
	 * @param explorationPolicy not used
	 */
	public DeepQLearning(AbstractState state, AbstractAction action, IExplorationPolicy explorationPolicy,
			int maxMemorySize, MemoryReplayModel predictModel, MemoryReplayModel targetModel, boolean isRNN,
			double discountFactor, double learningRate) throws Exception {
		super(Integer.MAX_VALUE, action.getNumberActions(), explorationPolicy, discountFactor, learningRate);
		if (maxMemorySize <= 0) {
			maxMemorySize = MAX_MEMORY_SIZE;
		}
		this.indexToStateCache = Maps.synchronizedBiMap(HashBiMap.create());
		this.isRNN = isRNN;
		this.maxMemorySize = maxMemorySize;
		this.state = state;
		this.action = action;
		this.stateRowSet = new StateRow[this.maxMemorySize];
		if (!USE_AS_QLEARN) {
			this.predictModel = predictModel;
			this.targetModel = targetModel;
		}
		if (!(explorationPolicy instanceof EpsilonGreedyExploration)) {
			throw new Exception("DeepQLearning only available with epsilon greedy!");
		}

		// create Q-array
		int numberOfColumns =
				getStateColumns() + action.getNumberActions() + getStateColumns();//state action next_state
		memoryReplay = new double[this.maxMemorySize][numberOfColumns];
		epsilon = ((EpsilonGreedyExploration) explorationPolicy).getEpsilon();
	}

	public void setSeed(long seed) {
		logger.info("setting seed to {}", seed);
		r = new Random(seed);

	}

	public static boolean trainOnData(String memoryPath, int actionColumns, int stateColumns, String outputModelPath,
			double learningRateNN, double momentumNesterov, int nEpoch, int batchSize, int maxBatchSize, double l2,
			double l1, int trainingStats, boolean isRNN, boolean isHyperParameterTuning, int rnnHorizon)
			throws IOException {
		if (USE_AS_QLEARN) {
			System.out.println("not training using as QLearn!");
			return true;
		}
		File file = new File(memoryPath);
		if (!file.exists()) {
			System.err.println(memoryPath + " not exist to train");
			return false;
		}
		double[][] memoryData = loadCSV(memoryPath, stateColumns);
		//check load dimension
		int columnsRead = memoryData[0].length;
		assert columnsRead == (stateColumns * 2) + actionColumns;

		if (batchSize <= 0 && memoryData != null) {
			batchSize = Math.min(512, memoryData.length / 2);
		}

		Dl4jMemoryReplayModel memoryReplayModel = new Dl4jMemoryReplayModel(outputModelPath, learningRateNN,
				momentumNesterov, nEpoch, batchSize, maxBatchSize, l2, l1, TRAIN_FROM_FILE_MODEL, isRNN);

		if (trainingStats != 0) {
			memoryReplayModel.setTrainingStats(true);
		}
		if (isHyperParameterTuning) {
			System.out.println("Hyperparameter tuning detected!  activate EARLY_STOPPING and disable training stats");
			Dl4jMemoryReplayModel.HYPERPARAMETER_TUNING = true;
			Dl4jMemoryReplayModel.EARLY_STOPPING = true;
			memoryReplayModel.setTrainingStats(false);
		}

		double[][] x = getColumnsArray(memoryData, 0, stateColumns);
		double[][] y = getColumnsArray(memoryData, stateColumns, stateColumns + actionColumns);
		logger.info("starting training model with {} epoch on {} batch", nEpoch, batchSize);
		//		System.out.println("training on data with   rows:"+x.length+"  columns:"+x[0].length+"  epochs:"+nEpoch+"  batchSize:"+batchSize+"  maxBatchSize:"+maxBatchSize);
		long start = System.currentTimeMillis();
		memoryReplayModel.train(x, y);
		long elapsed = (System.currentTimeMillis() - start) / (1000 * 60);
		logger.info("trained finished on {} minutes ,saving model {}", elapsed, outputModelPath);
		memoryReplayModel.saveModel();
		return true;

	}

	public void setPredictModel(MemoryReplayModel predictModel) {
		this.predictModel = predictModel;
	}

	public void setTargetModel(MemoryReplayModel targetModel) {
		this.targetModel = targetModel;
	}

	private int getStateColumns() {
		return state.getNumberOfColumns();
	}

	public void saveMemory(String filepath) throws IOException {
		if (memoryReplayIndex <= 0) {
			logger.warn("no data in DeepQlearning memoryReplay to save!");
			return;
		}
		File file = new File(filepath);
		file.getParentFile().mkdirs();
		StringBuilder outputString = new StringBuilder();
		for (int row = 0; row < memoryReplayIndex; row++) {
			for (int column = 0; column < memoryReplay[row].length; column++) {
				outputString.append(memoryReplay[row][column]);
				outputString.append(CSV_SEPARATOR);
			}
			outputString = outputString
					.delete(outputString.lastIndexOf(CSV_SEPARATOR), outputString.length());//remove last csv separator
			outputString.append(System.lineSeparator());
		}

		outputString = outputString.delete(outputString.lastIndexOf(System.lineSeparator()),
				outputString.length());//remove last line separator

		String content = outputString.toString();
		FileWriter fileWriter = new FileWriter(filepath);
		BufferedWriter writer = new BufferedWriter(fileWriter);
		try {
			writer.write(content);
			logger.info("saved memory replay size of  {}/{} rows and {} states-actions-next-states to {}",
					memoryReplaySize, maxMemorySize, memoryReplay[0].length, filepath);
			System.out.println("saved memory " + memoryReplaySize + " rows into " + filepath);
		} catch (Exception e) {
			logger.error("error saving memory replay to file {} ", filepath, e);
		} finally {
			writer.close();
		}
	}

	private static double[][] loadCSV(String filepath, int columnsStates) throws IOException {
		//only used on trainOnData
		File file = new File(filepath);
		if (!file.exists()) {
			logger.warn("memory not found {}-> start empty", filepath);
			return null;
		}

		BufferedReader csvReader = new BufferedReader(new FileReader(filepath));
		// we don't know the amount of data ahead of time so we use lists

		Map<Integer, List<Double>> colMap = new HashMap<>();
		String row;
		int rowsTotal = 0;

		while ((row = csvReader.readLine()) != null) {
			String[] data = row.split(CSV_SEPARATOR);
			double[] stateRow = new double[columnsStates];
			for (int column = 0; column < data.length; column++) {
				List<Double> columnList = colMap.getOrDefault(column, new ArrayList<>());
				double value = Double.parseDouble(data[column]);
				columnList.add(value);
				colMap.put(column, columnList);

				if (column < stateRow.length) {
					stateRow[column] = value;
				}

			}

			rowsTotal++;
		}
		csvReader.close();
		int columnsTotal = colMap.size();

		//transform colMap into array
		double[][] loadedQvalues = new double[rowsTotal][columnsTotal];//states rows , actions columns
		int rowsFilled = 0;
		for (int column : colMap.keySet()) {
			List<Double> rows = colMap.get(column);
			int rowIter = 0;
			for (double rowVal : rows) {
				loadedQvalues[rowIter][column] = rowVal;
				rowsFilled = rowIter;
				rowIter++;

			}
		}

		//		loadedQvalues=ArrayUtils.subarray(loadedQvalues, 0, rowsTotal);

		System.out.println(
				String.format("loaded a memory replay of %d/%d rows-states   and %d states-actions-next-states",
						rowsFilled, loadedQvalues.length, loadedQvalues[0].length));

		return loadedQvalues;

	}

	public void loadMemory(String filepath) throws IOException {
		File file = new File(filepath);
		if (!file.exists()) {
			logger.warn("memory not found {}-> start empty", filepath);
			return;
		}
		BufferedReader csvReader = new BufferedReader(new FileReader(filepath));
		// we don't know the amount of data ahead of time so we use lists

		Map<Integer, List<Double>> colMap = new HashMap<>();
		String row;
		int rowsTotal = 0;

		while ((row = csvReader.readLine()) != null) {
			String[] data = row.split(CSV_SEPARATOR);
			double[] stateRow = new double[state.getNumberOfColumns()];
			for (int column = 0; column < data.length; column++) {
				List<Double> columnList = colMap.getOrDefault(column, new ArrayList<>());
				double value = Double.parseDouble(data[column]);
				columnList.add(value);
				colMap.put(column, columnList);

				if (column < stateRow.length) {
					stateRow[column] = value;
				}

			}
			try {
				stateRowSet[rowsTotal] = new StateRow(stateRow);
				indexToStateCache.put(stateRowSet[rowsTotal], rowsTotal);
			} catch (IndexOutOfBoundsException e) {
				logger.warn("IndexOutOfBoundsException loading memory -> loading first {} rows -> set as index ,size",
						rowsTotal);
				memoryReplayIndex = -1;
				break;
			} catch (IllegalArgumentException ex) {
				logger.warn("IllegalArgumentException   value already present , loading memory -> skip row", ex);
				continue;
			}
			memoryReplaySize = rowsTotal;//starts at 0
			memoryReplayIndex = rowsTotal;
			rowsTotal++;
		}
		csvReader.close();
		int columnsTotal = colMap.size();

		int actionColumns = columnsTotal - 2 * state.getNumberOfColumns();
		if (actionColumns != action.getNumberActions()) {
			System.err.println(
					"cant load " + filepath + " action columns " + actionColumns + " are not equal! to number actions "
							+ action.getNumberActions() + " -> starting from empty memory");
			logger.error("cant load " + filepath
							+ "file action columns  {} (colTotal({})-2*statColumns({})) are not equal to action columns on params {}!->"
							+ " starting from empty memory", actionColumns, columnsTotal, state.getNumberOfColumns(),
					action.getNumberActions());
			return;
		}

		//transform colMap into array

		double[][] loadedQvalues = new double[this.maxMemorySize][columnsTotal];//states rows , actions columns
		for (int column : colMap.keySet()) {
			List<Double> rows = colMap.get(column);
			int rowIter = 0;
			for (double rowVal : rows) {
				try {
					loadedQvalues[rowIter][column] = rowVal;
					rowIter++;
				} catch (IndexOutOfBoundsException e) {
					this.memoryReplaySize = rowIter;//starts at 1
					break;
				}
			}
		}

		this.memoryReplay = loadedQvalues;
		System.out.println(String.format(
				"loaded a memory replay of %d/%d rows-states and %d states-actions-next-states on a %d maxMemorySize and index start on %d",
				rowsTotal, this.memoryReplay.length, this.memoryReplay[0].length, this.maxMemorySize,
				this.memoryReplayIndex));

		logger.info(String.format(
				"loaded a memory replay of %d/%d rows-states and %d states-actions-next-states on a %d maxMemorySize and index start on %d from %s",
				rowsTotal, this.memoryReplay.length, this.memoryReplay[0].length, this.maxMemorySize,
				this.memoryReplayIndex, filepath));

		iterateIndex();

	}

	private void iterateIndex() {
		if (memoryReplayIndex >= maxMemorySize - 1) {
			//complete memory => restart from beginning
			memoryReplayIndex = 0;
		} else {
			memoryReplayIndex++;
		}

		this.memoryReplaySize++;//0-49
		this.memoryReplaySize = Math.min(memoryReplaySize, maxMemorySize);

	}

	public double[] getRewards(double[] stateToSearch) {
		int indexOfState = stateExistRow(stateToSearch);
		if (indexOfState < 0) {
			//not existing
			return null;
		} else {
			double[] actionArr = new double[getActions()];
			double[] arrayRow = memoryReplay[indexOfState];
			double[] stateArr = ArrayUtils.subarray(memoryReplay[indexOfState], 0, state.getNumberOfColumns());
			actionArr = ArrayUtils.subarray(memoryReplay[indexOfState], state.getNumberOfColumns(),
					state.getNumberOfColumns() + this.action.getNumberActions());
			return actionArr;
		}
	}

	/**
	 * PREDICT Network
	 *
	 * @param state
	 * @return
	 */
	public double[] getPredict(double[] state) {
		if (USE_AS_QLEARN) {
			//get direclty from memory replay
			return getRewards(state);
		}
		double[] output = this.predictModel.predict(state);
		return output;
		//		if (output==null){
		//			if (defaultActionPredictScore==null){
		//				defaultActionPredictScore = new double[getActions()];
		//				Arrays.fill(defaultActionPredictScore, DEFAULT_PREDICTION_ACTION_SCORE);
		//			}
		//			output = defaultActionPredictScore.clone();
		//			return output;
		//		}else{
		//			return output;
		//		}
	}

	/**
	 * TARGET network
	 *
	 * @param state
	 * @return
	 */
	public double getPredictNextStateBestReward(double[] state) {
		if (USE_AS_QLEARN) {
			//get direclty from memory replay
			double[] actions = getRewards(state);
			double bestScore = Double.NaN;
			if (actions != null) {
				bestScore = Doubles.max(actions);
			}
			return bestScore;

		}
		try {
			double[] actionArr = this.targetModel.predict(state);
			if (actionArr == null) {
				return Double.NaN;
			}
			return Doubles.max(actionArr);
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	private boolean isSameDouble(double a, double b, double tolerance) {
		return Math.abs(a - b) < tolerance;
	}

	public int GetAction(AbstractState lastState) {
		double[] currentState = lastState.getCurrentStateRounded();
		double[] actionScoreEstimation = getPredict(currentState);

		int greedyAction = -1;
		int index = 0;
		List<Integer> actionsWithDefaultScore = new ArrayList<>();
		if (actionScoreEstimation != null) {
			double bestScore = Doubles.max(actionScoreEstimation);
			for (double score : actionScoreEstimation) {
				boolean isSameScoreAsDefault = isSameDouble(score, DEFAULT_PREDICTION_ACTION_SCORE, 1E-8);
				if (!isSameScoreAsDefault && score >= bestScore) {
					greedyAction = index;
				}
				if (isSameScoreAsDefault) {
					actionsWithDefaultScore.add(index);
				}
				index++;
			}
			if (greedyAction == -1) {
				logger.warn("Strange greedy action is still -1 when bestScore is {}-> explore on defaultValues",
						bestScore);
				int randomAction = r.nextInt(actionsWithDefaultScore.size());
				greedyAction = actionsWithDefaultScore.get(randomAction);
			}
		}
		// try to do exploration

		if (greedyAction == -1 || r.nextDouble() < epsilon) {
			//fix with one action!!
			int numberActions = getActions();
			int randomAction = 0;
			if (numberActions > 1) {
				randomAction = r.nextInt(numberActions);
			}

			//why?
			//			if (randomAction >= greedyAction)
			//				randomAction++;

			return randomAction;
		}

		return greedyAction;

	}

	public int GetAction(int state) {
		System.err.println("GetAction:int not used in dqn");
		return -1;
	}

	public void updateState(int previousState, int action, double reward, int nextState) {
		System.err.println("updateState:int not used in dqn");
	}

	private int getNextIndexMemoryReplay() {
		return this.memoryReplaySize;
		//		if (this.memoryReplaySize > this.maxMemorySize) {
		//			return this.maxMemorySize;
		//		} else {
		//			return this.memoryReplaySize;
		//		}

	}

	private StateRow[] getSubsetAllStates() {
		int limit = getNextIndexMemoryReplay();
		//		if (limit < this.maxMemorySize) {
		//			limit = limit - 1;//not all updated yet and pointing to the next row-> discard it
		//		}

		while (true) {
			StateRow[] subset = ArrayUtils
					.subarray(stateRowSet, 0, limit);//include the end limit because is not updated yet!
			if (subset == null || subset.length == 0) {
				return null;
			}
			return subset;
			//			try {
			//				Arrays.sort(subset);
			//				return subset;
			//			} catch (NullPointerException e) {
			//				logger.error(
			//						"NullPointerException Arrays.sort strange with limit:{}  memoryReplaySize:{}  maxMemorySize:{} -> less limit",
			//						limit, memoryReplaySize, maxMemorySize, e);
			//				limit--;
			//			}

		}
	}

	private int directorySearch(double[] state) {
		StateRow rowToSearch = new StateRow(state);
		return indexToStateCache.getOrDefault(rowToSearch, -1);
		//		if (indexToStateCache.containsKey(rowToSearch)) {
		//			return indexToStateCache.get(rowToSearch);
		//		}else{
		//			return -1;
		//		}
		//		int output = -1;
		//		StateRow[] subset = getSubsetAllStates();
		//		if (subset == null || subset.length == 0) {
		//			return output;
		//		}
		//
		//		for (int i = 0; i < subset.length; i++) {
		//			StateRow stateRow = subset[i];
		//			if (rowToSearch.equals(stateRow)) {
		//				//add to cache
		//				indexToStateCache.put(rowToSearch, i);
		//				return i;
		//			}
		//		}
		//		return output;
	}

	public int stateExistRow(double[] previousStateArr) {
		if (this.memoryReplaySize == 0) {
			return -1;
		}

		StateRow[] subset = getSubsetAllStates();
		if (subset == null || subset.length == 0) {
			return -1;
		}

		//		int indexOfState = Arrays.binarySearch(subset, new StateRow(previousStateArr));
		int indexOfState = directorySearch(previousStateArr);

		if (indexOfState > 0 && !ArrayUtils.isEquals(previousStateArr, subset[indexOfState].inputArray)) {
			//different state checking ,prevent mix them-> not found
			return -1;
		}

		//		if (indexOfState > 0 && LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
		//			logger.info("state found {} -> {}", indexOfState, ArrayUtils.toString(previousStateArr));
		//		}
		//		if (indexOfState <= 0 && LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
		//			logger.info("state not found  -> {}", ArrayUtils.toString(previousStateArr));
		//		}

		return indexOfState;

	}

	public double[] getState(int indexOfState) {
		double[] stateArr = ArrayUtils.subarray(memoryReplay[indexOfState], 0, state.getNumberOfColumns());
		return stateArr;
	}

	public void updateState(double[] previousStateArr, int action, double reward, AbstractState nextState) {
		boolean isNewRow = true;
		int indexOfState = stateExistRow(previousStateArr);
		if (indexOfState > -1) {
			//exist state
			isNewRow = false;
		}
		//		if (isNewRow && reward == DEFAULT_PREDICTION_ACTION_SCORE) {
		//			//not add this rows
		//			return;
		//		}

		double[] actionArr = new double[getActions()];
		if (!isNewRow) {
			actionArr = ArrayUtils.subarray(memoryReplay[indexOfState], state.getNumberOfColumns(),
					state.getNumberOfColumns() + this.action.getNumberActions());
		}
		double maxNextExpectedReward = getPredictNextStateBestReward(
				nextState.getCurrentStateRounded());//from target network

		if (Double.isNaN(maxNextExpectedReward)) {
			//			logger.error("something is wrong on prediciton model => expected next reward nan");
			//			System.err.println("something is wrong on prediction model => expected next reward nan");
			maxNextExpectedReward = DEFAULT_PREDICTION_ACTION_SCORE;
		}

		// previous state's action estimations
		try {
			double qValue = reward;
			if (actionArr[action] != DEFAULT_PREDICTION_ACTION_SCORE) {
				// update expexted summary reward of the previous state using Bellman Dynamic equation
				qValue = actionArr[action] * (1.0 - learningRate) + (learningRate * (reward
						+ discountFactor * maxNextExpectedReward));
				actionArr[action] = qValue;
			}
			actionArr[action] = qValue;

		} catch (IndexOutOfBoundsException e) {
			System.err.println(
					"Trying to save action index " + action + " in an actions array of len " + actionArr.length);
			throw e;
		}

		if (isNewRow) {
			double[] nextStateArr = nextState.getCurrentStateRounded();
			double[] newRow = ArrayUtils.addAll(previousStateArr, actionArr);
			newRow = ArrayUtils.addAll(newRow, nextStateArr);
			try {
				this.memoryReplay[this.memoryReplayIndex] = newRow;
				this.stateRowSet[this.memoryReplayIndex] = new StateRow(previousStateArr);
				indexToStateCache.inverse().remove(this.memoryReplayIndex);
				indexToStateCache.put(this.stateRowSet[this.memoryReplayIndex], this.memoryReplayIndex);
			} catch (IndexOutOfBoundsException e) {
				logger.error(
						"out of bounds error updating memoryReplay with memoryReplayIndex {} and maxMemorySize {} -> save to zero",
						memoryReplayIndex, maxMemorySize);
				this.memoryReplayIndex = 0;

				this.memoryReplay[this.memoryReplayIndex] = newRow;
				this.stateRowSet[this.memoryReplayIndex] = new StateRow(previousStateArr);//override also states
			}
			iterateIndex();

		} else {
			//add to memoryReplay
			double[] nextStateArr = ArrayUtils
					.subarray(memoryReplay[indexOfState], state.getNumberOfColumns() + this.action.getNumberActions(),
							memoryReplay[0].length);
			double[] updateRow = ArrayUtils.addAll(previousStateArr, actionArr);
			updateRow = ArrayUtils.addAll(updateRow, nextStateArr);
			try {
				this.memoryReplay[indexOfState] = updateRow;
			} catch (Exception e) {
				logger.error("error updating memory replay buffer ", e);
			}

		}

	}

	public static double[][] getColumnsArray(double[][] input, int firstColumn, int lastColumn) {
		double[][] output = new double[input.length][lastColumn - firstColumn];
		for (int row = 0; row < input.length; row++) {
			for (int column = firstColumn; column < lastColumn; column++) {
				// index starts from 0
				output[row][column - firstColumn] = input[row][column];
			}

		}
		return output;
	}

	private double[][] getArrayValid() {
		double[][] validArr = ArrayUtils
				.subarray(memoryReplay, 0, getNextIndexMemoryReplay());//we are going to clean it

		double[][] targetRaw = getColumnsArray(validArr, getStateColumns(),
				getStateColumns() + action.getNumberActions());

		//clean validArr where all targets are ==
		double[][] outputArr = validArr.clone();
		int rowsDeleted = 0;
		for (int row = 0; row < targetRaw.length; row++) {
			double sumRewardsState = 0.0;
			for (int column = 0; column < targetRaw[row].length; column++) {
				sumRewardsState += targetRaw[row][column];
			}
			if (sumRewardsState == DEFAULT_PREDICTION_ACTION_SCORE) {
				int indexToDelete = row - rowsDeleted;
				outputArr = ArrayUtils.remove(outputArr, indexToDelete);
				rowsDeleted++;
			}
		}

		return outputArr;

	}

	public double[][] getInputTrain() {
		double[][] validArr = getArrayValid();
		logger.info("training input array of {} rows and {} columns", validArr.length, getStateColumns());
		return getColumnsArray(validArr, 0, getStateColumns());
	}

	public static int argmax(double[] array) {
		double max = array[0];
		int re = 0;
		for (int i = 1; i < array.length; i++) {
			if (array[i] > max) {
				max = array[i];
				re = i;
			}
		}
		return re;
	}

	public static int argmin(double[] array) {
		double min = array[0];
		int re = 0;
		for (int i = 1; i < array.length; i++) {
			if (array[i] < min) {
				min = array[i];
				re = i;
			}
		}
		return re;
	}

	private double[][] getTargetClassification(double[][] targetArr) {
		double[][] targetArrOutput = new double[targetArr.length][targetArr[0].length];
		for (int row = 0; row < targetArr.length; row++) {
			double bestColumn = Doubles.max(targetArr[row]);
			int bestAction = argmax(targetArr[row]);

			double worstColumn = Doubles.min(targetArr[row]);
			int worstAction = argmin(targetArr[row]);

			boolean bestIsPositive = true;
			boolean worstIsNegative = true;
			int numberOfColumns = targetArr[row].length;
			if (bestColumn == DEFAULT_PREDICTION_ACTION_SCORE) {
				bestIsPositive = false;
			}
			if (worstColumn == DEFAULT_PREDICTION_ACTION_SCORE) {
				worstIsNegative = false;
			}

			for (int column = 0; column < targetArr[row].length; column++) {
				if (bestIsPositive) {
					if (targetArr[row][column] == bestColumn) {
						targetArrOutput[row][column] = 1.0;
					} else {
						//maintain negative values
						targetArrOutput[row][column] = targetArr[row][column];
					}
				} else {
					if (targetArr[row][column] == bestColumn) {
						targetArrOutput[row][column] = 1.0 / (double) (numberOfColumns);
					} else {
						//maintain negative values
						targetArrOutput[row][column] = targetArr[row][column];
					}
				}
			}

			if (worstIsNegative) {
				targetArrOutput[row][worstAction] = -1.0;
			}

		}
		return targetArrOutput;

	}

	public double[][] getTargetTrain() {
		double[][] validArr = getArrayValid();
		logger.info("training target array of {} rows and {} columns", validArr.length, action.getNumberActions());
		double[][] targetArr = getColumnsArray(validArr, getStateColumns(),
				getStateColumns() + action.getNumberActions());
		return targetArr;
		//		return getTargetClassification(targetArr);

	}

	/***
	 * Class used to have index of the states saved
	 */
	private class StateRow implements Comparable<StateRow> {

		private double[] inputArray;

		public StateRow(double[] inputArray) {
			this.inputArray = inputArray.clone();
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

		@Override public int compareTo(StateRow o) {
			if (o.equals(this.inputArray)) {
				return 0;
			}
			return ARRAY_COMPARATOR.compare(inputArray, o.inputArray);
		}

	}

}
