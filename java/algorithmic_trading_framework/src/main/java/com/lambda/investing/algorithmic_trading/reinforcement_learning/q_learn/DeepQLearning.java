package com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.lambda.investing.algorithmic_trading.IterationsPeriodTime;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.MemoryReplayModel;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.TrainNNUtils;
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

import static com.lambda.investing.algorithmic_trading.reinforcement_learning.TrainNNUtils.argmax;
import static com.lambda.investing.algorithmic_trading.reinforcement_learning.TrainNNUtils.getColumnsArray;

@Getter @Setter public class DeepQLearning extends QLearning {

	public static double EXPLORE_MODE_TRESHOLD = 0.99;
	public static boolean SAVE_ZERO_REWARDS = false;

	protected double cumReward = 0.0;
	protected int rewardCounter = 0;

	protected static boolean SHUFFLE_LOADING_ROWS = true;//if true will load in randomOrder from file to avoid discarting the same rows always!
	public boolean asQLearn = false;

	private static Comparator<double[]> ARRAY_COMPARATOR = Doubles.lexicographicalComparator();
	protected static Logger logger = LogManager.getLogger(DeepQLearning.class);
	public static double DEFAULT_PREDICTION_ACTION_SCORE = 0;
	private BiMap<StateRow, Integer> indexToStateCache;

	private static int MAX_MEMORY_SIZE = (int) 1E6;
	protected double epsilon;
	protected Random r = new Random();
	private AbstractState state;
	protected AbstractAction action;
	private int memoryReplayIndex, maxMemorySize, memoryReplaySize;
	private StateRow[] stateRowSet;
	protected MemoryReplayModel predictModel, targetModel;
	private boolean isRNN;
	private double[] defaultActionPredictScore;

	//learn periods
	private int iterations = 0;
	protected int trainingPredictIterationPeriod;
	protected int trainingTargetIterationPeriod;
	private Date lastDateTrainPredict = null;
	private Date lastDateTrainTarget = null;

	protected int counterExplore = 0;
	protected int counterExploit = 0;

	/**
	 * Initializes a new instance of the QLearning class.
	 *
	 * @param state             class states.
	 * @param action            class actions.
	 * @param explorationPolicy not used
	 */
	public DeepQLearning(AbstractState state, AbstractAction action, IExplorationPolicy explorationPolicy,
			int maxMemorySize, MemoryReplayModel predictModel, MemoryReplayModel targetModel, boolean isRNN,
			double discountFactor, double learningRate, int trainingPredictIterationPeriod,
			int trainingTargetIterationPeriod) throws Exception {
		super(Integer.MAX_VALUE, action.getNumberActions(), explorationPolicy, discountFactor, learningRate);
		if (maxMemorySize <= 0) {
			maxMemorySize = MAX_MEMORY_SIZE;
		}
		this.indexToStateCache = Maps.synchronizedBiMap(HashBiMap.create(this.maxMemorySize));
		this.isRNN = isRNN;
		this.maxMemorySize = maxMemorySize;
		this.state = state;
		this.action = action;
		this.stateRowSet = new StateRow[this.maxMemorySize];
		if (!asQLearn) {
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
		this.trainingPredictIterationPeriod = trainingPredictIterationPeriod;
		this.trainingTargetIterationPeriod = trainingTargetIterationPeriod;
	}

	public void setSeed(long seed) {
		logger.info("setting seed to {}", seed);
		r = new Random(seed);

	}

	public void setPredictModel(MemoryReplayModel predictModel) {
		this.predictModel = predictModel;
	}

	public void setTargetModel(MemoryReplayModel targetModel) {
		this.targetModel = targetModel;
	}

	protected int getStateColumns() {
		return state.getNumberOfColumns();
	}

	public void saveMemory(String filepath) throws IOException {
		synchronized (lockSave) {
			if (memoryReplaySize <= 0) {
				logger.warn("no data in DeepQlearning memoryReplay to save!");
				return;
			}
			File file = new File(filepath);
			file.getParentFile().mkdirs();
			StringBuilder outputString = new StringBuilder();
			for (int row = 0; row < memoryReplaySize; row++) {
				for (int column = 0; column < memoryReplay[row].length; column++) {
					outputString.append(memoryReplay[row][column]);
					outputString.append(CSV_SEPARATOR);
				}
				outputString = outputString.delete(outputString.lastIndexOf(CSV_SEPARATOR),
						outputString.length());//remove last csv separator
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
	}

	private Integer[] getRandomPositions(int maxSize) {
		Integer[] output = new Integer[maxSize];
		for (int i = 0; i < output.length; i++) {
			output[i] = i;
		}
		List<Integer> outputList = Arrays.asList(output);
		Collections.shuffle(outputList);
		output = outputList.toArray(output);
		return output;
	}

	public void loadMemory(String filepath) throws IOException {
		synchronized (lockSave) {
			File file = new File(filepath);
			if (!file.exists()) {
				logger.warn("memory not found {}-> start empty", filepath);
				return;
			}
			BufferedReader csvReader = new BufferedReader(new FileReader(filepath));
			// we don't know the amount of data ahead of time so we use lists

			///Shuffle rows reading
			int numberOfRows = 0;
			String row;
			try {
				while ((row = csvReader.readLine()) != null) {
					numberOfRows++;
				}
			} catch (IOException ex) {
				logger.error("error reading row {} on {}", numberOfRows, filepath, ex);
			}
			if (numberOfRows > maxMemorySize) {
				logger.info("extending memory size to {}", numberOfRows);
				maxMemorySize = numberOfRows;
				this.indexToStateCache = Maps.synchronizedBiMap(HashBiMap.create(this.maxMemorySize));
				this.stateRowSet = new StateRow[this.maxMemorySize];

			}
			Integer[] randomPositions = getRandomPositions(numberOfRows);
			csvReader.close();

			//read the csv in order
			Map<Integer, List<Double>> colMap = new HashMap<>();
			int rowsTotal = 0;
			csvReader = new BufferedReader(new FileReader(filepath));
			try {
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
						int randomPos = rowsTotal;
						if (SHUFFLE_LOADING_ROWS) {
							//randomize position on reading due to binary search sorted
							randomPos = randomPositions[rowsTotal];
						}

						stateRowSet[randomPos] = new StateRow(stateRow);
						indexToStateCache.put(stateRowSet[randomPos], randomPos);

					} catch (IndexOutOfBoundsException e) {
						logger.warn(
								"IndexOutOfBoundsException loading memory -> loading first {} rows -> set as index ,size",
								rowsTotal);
						continue;
					} catch (IllegalArgumentException ex) {
						logger.warn(
								"IllegalArgumentException   value already present , loading memory {} -> skip row {}",
								filepath, ex.getMessage());
						continue;
					}
					memoryReplaySize = rowsTotal;//starts at 0
					memoryReplayIndex = rowsTotal;
					rowsTotal++;
				}
			} catch (IOException e) {
				logger.error("error reading row {} on {}", numberOfRows, filepath, e);
			}
			csvReader.close();
			int columnsTotal = colMap.size();

			int actionColumns = columnsTotal - 2 * state.getNumberOfColumns();
			if (actionColumns != action.getNumberActions()) {
				System.err.println("cant load " + filepath + " action columns " + actionColumns
						+ " are not equal! to number actions " + action.getNumberActions()
						+ " -> starting from empty memory");
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
						int posRow = rowIter;
						if (SHUFFLE_LOADING_ROWS) {
							//randomize position on reading due to binary search sorted
							posRow = randomPositions[rowIter];
						}
						loadedQvalues[posRow][column] = rowVal;
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
					rowsTotal, this.memoryReplay.length - 1, this.memoryReplay[0].length, this.maxMemorySize,
					this.memoryReplayIndex));

			logger.info(String.format(
					"loaded a memory replay of %d/%d rows-states and %d states-actions-next-states on a %d maxMemorySize and index start on %d from %s",
					rowsTotal, this.memoryReplay.length - 1, this.memoryReplay[0].length, this.maxMemorySize,
					this.memoryReplayIndex + 1, filepath));

			iterateIndex();
		}
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

	/**
	 * Return rewards from memoryReplay
	 *
	 * @param stateToSearch
	 * @return
	 */
	public double[] getRewards(double[] stateToSearch) {
		//		return predictModel.predict(stateToSearch);
		int indexOfState = stateExistRow(stateToSearch);
		if (indexOfState < 0) {
			//not existing
			return null;
		} else {
			double[] actionArr = new double[getActions()];
			double[] arrayRow = memoryReplay[indexOfState];
			double[] stateArr = ArrayUtils.subarray(arrayRow, 0, state.getNumberOfColumns());
			assert ArrayUtils.isEquals(stateArr, stateToSearch);
			actionArr = ArrayUtils.subarray(arrayRow, state.getNumberOfColumns(),
					state.getNumberOfColumns() + this.action.getNumberActions());
			return actionArr;
		}
	}

	/**
	 * PREDICT Network used to choose actions (dqn) and current q value
	 *
	 * @param state
	 * @return
	 */
	public double[] getPredictOutput(double[] state) {
		if (asQLearn) {
			//get direclty from memory replay -> where we saved q values!
			return getRewards(state);
		}
		double[] output = this.predictModel.predict(state);
		return output;
	}

	/**
	 * TARGET network used in q value formula to stimate next state action q value
	 *
	 * @param state
	 * @return
	 */
	public double[] getTargetOutput(double[] state) {
		if (asQLearn) {
			//get direclty from memory replay
			return getRewards(state);
		}
		double[] actionArr = this.targetModel.predict(state);
		return actionArr;
	}

	/**
	 * from TARGET network
	 *
	 * @param state
	 * @return
	 */
	public double getTargetNextStateBestReward(double[] state) {
		try {
			double[] actionArr = getTargetOutput(state);
			if (actionArr == null) {
				return Double.NaN;
			}
			return Doubles.max(actionArr);
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	protected boolean isSameDouble(double a, double b, double tolerance) {
		return Math.abs(a - b) < tolerance;
	}

	public int GetAction(AbstractState lastState) {
		double[] currentState = lastState.getCurrentStateRounded();
		double[] actionScoreEstimation = getPredictOutput(currentState);

		int greedyAction = -1;
		int index = 0;
		List<Integer> actionsWithSameBestScore = new ArrayList<>();
		List<Integer> actionsWithoutSameBestScore = new ArrayList<>();
		List<Integer> actionsSameAsDefault = new ArrayList<>();
		if (actionScoreEstimation != null) {
			double bestScore = Doubles.max(actionScoreEstimation);
			greedyAction = argmax(actionScoreEstimation);
			for (double score : actionScoreEstimation) {
				boolean isSameScoreAsBest = isSameDouble(score, bestScore, 1E-8);
				if (isSameScoreAsBest) {
					actionsWithSameBestScore.add(index);
				} else {
					actionsWithoutSameBestScore.add(index);
				}
				boolean isSameScoreAsDefault = isSameDouble(score, DEFAULT_PREDICTION_ACTION_SCORE, 1E-8);
				if (isSameScoreAsDefault) {
					actionsSameAsDefault.add(index);
				}

				index++;
			}
			if (actionsWithSameBestScore.size() > 1) {
				if (isSameDouble(bestScore, DEFAULT_PREDICTION_ACTION_SCORE, 1E-8)) {
					logger.info(
							"more than one actionsWithSameBestScore when bestScore is same as default {}-> explore on {} bestValues",
							bestScore, actionsWithSameBestScore.size());
				} else {
					logger.warn(
							"more than one actionsWithSameBestScore when bestScore is not default {}-> explore on {} bestValues",
							bestScore, actionsWithSameBestScore.size());
				}
				int randomAction = r.nextInt(actionsWithSameBestScore.size());
				greedyAction = actionsWithSameBestScore.get(randomAction);
			}
		}
		// try to do exploration
		double randomNumber = r.nextDouble();

		boolean weAreOverExploring = (getExplorePct()) > this.epsilon * 1.05;//ideally we shouldn't force it
		if (isAsQLearn() && weAreOverExploring) {
			//force exploit if we have best value and under exploit
			randomNumber = Double.MAX_VALUE;
		}
		boolean explore = greedyAction == -1 || randomNumber < epsilon;
		if (explore) {
			//fix with one action!!

			//random from all
			int numberActions = getActions();
			int randomAction = 0;
			if (numberActions > 1) {
				randomAction = r.nextInt(numberActions);
			}

			//if we have it -> explore other actions
			if (epsilon > EXPLORE_MODE_TRESHOLD && actionScoreEstimation != null && actionsSameAsDefault.size() > 0) {
				//randomize on nonBest
				int randomActionExplore = r.nextInt(actionsSameAsDefault.size());
				randomAction = actionsSameAsDefault.get(randomActionExplore);
			}

			//why?
			//			if (randomAction >= greedyAction)
			//				randomAction++;
			counterExplore++;
			return randomAction;
		}
		counterExploit++;
		return greedyAction;

	}

	public double getExplorePct() {
		double output = (double) (counterExplore) / (double) (counterExplore + counterExploit);
		return output;
	}

	public int GetAction(int state) {
		System.err.println("GetAction:int not used in dqn");
		return -1;
	}

	public void updateState(int previousState, int action, double reward, int nextState) {
		System.err.println("updateState:int not used in dqn");
	}

	protected int getNextIndexMemoryReplay() {
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

	/**
	 * bellman equation -> dynamic Q_(i+1) (s,a)=Predict(s,a)*(1-α)+α[R(s,a)+γ_d (Target⁡(s^',a^' )]
	 *
	 * @param previousStateArr
	 * @param reward
	 * @param currentQValue
	 * @param predictedQValue
	 * @return updated q value
	 */
	public double calculateQValue(double[] previousStateArr, double reward, double currentQValue,
			double predictedQValue) {
		//for q learn apporach
		if (currentQValue != DEFAULT_PREDICTION_ACTION_SCORE) {
			return currentQValue * (1.0 - learningRate) + (learningRate * (reward + discountFactor * predictedQValue));
		} else {
			return reward + discountFactor * predictedQValue;
		}
	}

	//	protected Double getQValue(double[] state, int action) {
	//		if (asQLearn) {
	//			//get direclty from memory replay
	//			double[] actions = getRewards(state);
	//			if (actions != null) {
	//				return actions[action];
	//			}
	//			return DEFAULT_PREDICTION_ACTION_SCORE;
	//		}
	//		double[] targetQValue = getPredictOutput(state);//from predict
	//		if (targetQValue != null) {
	//			return targetQValue[action];
	//		}
	//		return null;
	//	}

	public void updateState(Date currentTime, double[] previousStateRoundedArr, int action, double reward,
			AbstractState nextState) {

		if (!SAVE_ZERO_REWARDS && (reward == 0 || Double.isInfinite(reward))) {
			//not save it
			return;
		}

		cumReward += reward;
		rewardCounter++;

		synchronized (lockSave) {
			boolean isNewRow = true;
			int indexOfState = stateExistRow(previousStateRoundedArr);
			if (indexOfState > -1) {
				//exist state
				isNewRow = false;
			}

			double[] actionArr = new double[getActions()];
			Arrays.fill(actionArr, DEFAULT_PREDICTION_ACTION_SCORE);

			// previous state's action estimations
			try {
				actionArr[action] = reward;
				if (isAsQLearn()) {
					//save directly qLearn
					if (indexOfState >= 0) {
						actionArr = getRewards(previousStateRoundedArr);
					}
					double predictedQValue = DEFAULT_PREDICTION_ACTION_SCORE;
					double[] nextQValues = getRewards(nextState.getCurrentStateRounded());
					if (nextQValues != null) {
						predictedQValue = Doubles.max(nextQValues);
					}
					actionArr[action] = this
							.calculateQValue(previousStateRoundedArr, reward, actionArr[action], predictedQValue);
				}
			} catch (IndexOutOfBoundsException e) {
				System.err.println(
						"Trying to save action index " + action + " in an actions array of len " + actionArr.length);
				throw e;
			}

			if (isNewRow) {
				double[] nextStateArr = nextState.getCurrentStateRounded();
				double[] newRow = ArrayUtils.addAll(previousStateRoundedArr, actionArr);
				newRow = ArrayUtils.addAll(newRow, nextStateArr);
				try {
					this.memoryReplay[this.memoryReplayIndex] = newRow;
					this.stateRowSet[this.memoryReplayIndex] = new StateRow(previousStateRoundedArr);
					indexToStateCache.inverse().remove(this.memoryReplayIndex);
					indexToStateCache.put(this.stateRowSet[this.memoryReplayIndex], this.memoryReplayIndex);
				} catch (IndexOutOfBoundsException e) {
					logger.error(
							"out of bounds error updating memoryReplay with memoryReplayIndex {} and maxMemorySize {} -> save to zero",
							memoryReplayIndex, maxMemorySize);
					this.memoryReplayIndex = 0;

					this.memoryReplay[this.memoryReplayIndex] = newRow;
					this.stateRowSet[this.memoryReplayIndex] = new StateRow(
							previousStateRoundedArr);//override also states
				}
				iterateIndex();

			} else {
				//add to memoryReplay
				//				double[] nextStateArr = ArrayUtils.subarray(memoryReplay[indexOfState],
				//						state.getNumberOfColumns() + this.action.getNumberActions(), memoryReplay[0].length);
				double[] nextStateArr = nextState.getCurrentStateRounded();
				double[] updateRow = ArrayUtils.addAll(previousStateRoundedArr, actionArr);
				updateRow = ArrayUtils.addAll(updateRow, nextStateArr);
				try {
					this.memoryReplay[indexOfState] = updateRow;
				} catch (Exception e) {
					logger.error("error updating memory replay buffer ", e);
				}

			}

			this.learn(currentTime, previousStateRoundedArr, nextState.getCurrentStateRounded(), action, reward);
			iterations++;
		}

	}

	protected boolean trainPrediction() {
		if (asQLearn) {
			return true;
		}
		double[][] input = getInputTrain();
		double[][] target = getTargetTrain();
		assert input.length == target.length;
		boolean output = this.predictModel.train(input, target);
		setPredictModel(this.predictModel);
		return output;
	}

	protected boolean trainTarget() {
		if (asQLearn) {
			return true;
		}

		if (this.predictModel != null && this.predictModel.isTrained()) {
			logger.info("training target=>clone predict");
			targetModel = this.predictModel.cloneIt(targetModel.getModelPath());
			targetModel.saveModel();
			return true;
		}
		return false;

	}

	protected void updateGradientsPredict() {
		//		https://github.com/dalmia/udacity-deep-reinforcement-learning/blob/409f220d5d024c48b4ba3a3baaba5f10882a9581/2%20-%20Value-based%20methods/Project-Navigation/dqn_agent.py#L81
		trainPrediction();
	}

	protected void updateGradientsTarget() {
		trainTarget();
	}

	public void commandStopReceived() {
		//called in stop command end of backtests!
		double meanReward = cumReward / rewardCounter;
		System.out.println("meanReward= " + String.valueOf(meanReward));
		logger.info("meanReward= {}", meanReward);

	}

	protected boolean learnLogic(Date currentTime, double[] state, double[] nextState, int action, double reward) {
		//save it on a list
		if (lastDateTrainPredict == null) {
			lastDateTrainPredict = currentTime;
		}
		if (lastDateTrainTarget == null) {
			lastDateTrainTarget = currentTime;
		}
		boolean clearList = false;
		if (this.trainingPredictIterationPeriod > 0 && this.iterations % this.trainingPredictIterationPeriod == 0) {
			if (predictModel == null || !predictModel.isTrained() || predictModel.predict(state) == null) {
				logger.info("initial training predict model-> is not trained");
				this.trainPrediction();
			}

			if (targetModel == null || !targetModel.isTrained() || targetModel.predict(state) == null) {
				logger.info("initial training target model-> is not trained");
				this.trainTarget();
			}
			if (predictModel == null || targetModel == null || !predictModel.isTrained() || !targetModel.isTrained()) {
			} else {
				System.out.println("training predict model on " + this.iterations + " iteration");
				logger.info("training predict model on {} iteration", this.iterations);
				clearList = true;
				updateGradientsPredict();
			}
		}

		//periodical update predict gradient
		if (IterationsPeriodTime.isPeriodicalPeriod(this.trainingPredictIterationPeriod)) {
			IterationsPeriodTime periodTime = IterationsPeriodTime.valueOf(this.trainingPredictIterationPeriod);
			if (lastDateTrainPredict != null && periodTime.hasPassed(lastDateTrainPredict, currentTime)) {
				if (predictModel == null || !predictModel.isTrained() || predictModel.predict(state) == null) {
					logger.info("initial training predict model-> is not trained");
					this.trainPrediction();

				}

				if (targetModel == null || !targetModel.isTrained() || targetModel.predict(state) == null) {
					logger.info("initial training target model-> is not trained");
					this.trainTarget();
				}
				if (predictModel == null || targetModel == null || !predictModel.isTrained() || !targetModel
						.isTrained()) {
					//something was wrong try later
					System.out.println("cant train predict model on " + currentTime + " time");
					logger.info("cant train predict model on {} time", currentTime);
					lastDateTrainPredict = currentTime;
				} else {
					System.out.println("training predict model on " + currentTime + " time");
					logger.info("training predict model on {} time", currentTime);
					clearList = true;
					updateGradientsPredict();
					lastDateTrainPredict = currentTime;
				}
			}

		}

		if (this.trainingTargetIterationPeriod > 0 && this.iterations % this.trainingTargetIterationPeriod == 0) {
			if (predictModel == null || targetModel == null || !predictModel.isTrained() || !targetModel.isTrained()
					|| predictModel.predict(state) == null || targetModel.predict(state) == null) {
			} else {
				System.out.println("training target model on " + this.iterations + " iteration");
				logger.info("training target model on {} iteration", this.iterations);
				clearList = true;
				updateGradientsTarget();
			}
		}
		//periodical update target
		if (IterationsPeriodTime.isPeriodicalPeriod(trainingTargetIterationPeriod)) {
			IterationsPeriodTime periodTime = IterationsPeriodTime.valueOf(trainingTargetIterationPeriod);
			if (lastDateTrainTarget != null && periodTime.hasPassed(lastDateTrainTarget, currentTime)) {
				if (predictModel == null || targetModel == null || !predictModel.isTrained() || !targetModel.isTrained()
						|| clearList || predictModel.predict(state) == null || targetModel.predict(state) == null) {
					//something was wrong try later
					System.out.println("cant train target model on " + currentTime + " time");
					logger.info("cant train target model on {} time", currentTime);
					lastDateTrainTarget = currentTime;
				} else {
					//train it
					System.out.println("training target model on " + currentTime + " time");
					logger.info("training target model on {} time", currentTime);
					clearList = true;
					updateGradientsTarget();
					lastDateTrainTarget = currentTime;
				}
			}
		}

		return clearList;
	}

	protected void learn(Date currentTime, double[] state, double[] nextState, int action, double reward) {
		learnLogic(currentTime, state, nextState, action, reward);
	}

	protected double[][] getArrayValid() {
		double[][] validArr = ArrayUtils
				.subarray(memoryReplay, 0, getNextIndexMemoryReplay());//we are going to clean it
		return TrainNNUtils.getArrayValid(validArr, getStateColumns(), action.getNumberActions(), false);
	}

	public double[][] getInputTrain() {
		double[][] validArr = getArrayValid();
		logger.info("training input array of {} rows and {} columns", validArr.length, getStateColumns());
		return getInputTrainValues(validArr);
	}

	protected double[][] getInputTrainValues(double[][] allMemory) {
		return getColumnsArray(allMemory, 0, getStateColumns());
	}

	protected double[][] getTargetTrainValues(double[][] states, double[][] nextStates, double[][] rewardArr) {
		assert states[0].length == getStateColumns();
		assert nextStates[0].length == getStateColumns();
		assert rewardArr[0].length == getActions();

		double[][] target = new double[rewardArr.length][rewardArr[0].length];

		if (predictModel == null || !predictModel.isTrained()) {
			logger.warn("predictModel is not trained and required to getTargetTrainValues");
		}
		if (targetModel == null || !targetModel.isTrained()) {
			logger.warn("targetModel is not trained and required to getTargetTrainValues");
		}
		for (int row = 0; row < rewardArr.length; row++) {
			double[] nextState = nextStates[row];
			double[] reward = rewardArr[row];
			double[] state = states[row];

			double[] qTarget = predictModel.predict(state);
			if (qTarget == null) {
				qTarget = reward;
			}

			int actionChosen = -1;
			for (int column = 0; column < qTarget.length; column++) {
				if (Double.isFinite(reward[column]) && reward[column] != DEFAULT_PREDICTION_ACTION_SCORE) {
					qTarget[column] = reward[column];
					actionChosen = column;
				}
			}
			if (actionChosen < 0) {
				logger.warn("actionChosen cant be found to train on new QValue -> not correction row");
			}

			double discount = discountFactor;
			double[] nextRewards = targetModel.predict(nextState);
			double bestNextReward = 0.0;
			if (nextRewards != null) {
				bestNextReward = Doubles.max(nextRewards);
			}

			for (int column = 0; column < qTarget.length; column++) {
				if (column == actionChosen) {
					target[row][column] = reward[column] + discount * bestNextReward;
				} else {
					target[row][column] = qTarget[column];
				}
			}
		}
		return target;
	}

	public double[][] getTargetTrain() {
		double[][] validArr = getArrayValid();

		logger.info("training target array of {} rows and {} columns", validArr.length, action.getNumberActions());
		double[][] rewardArr = getColumnsArray(validArr, getStateColumns(),
				getStateColumns() + action.getNumberActions());
		double[][] states = getColumnsArray(validArr, 0, getStateColumns());
		double[][] nextStates = getColumnsArray(validArr, getStateColumns() + action.getNumberActions(),
				validArr[0].length);

		return getTargetTrainValues(states, nextStates, rewardArr);

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
