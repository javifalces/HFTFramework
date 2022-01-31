package com.lambda.investing.algorithmic_trading.reinforcement_learning;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.*;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.action.AbstractAction;
//import com.lambda.investing.algorithmic_trading.reinforcement_learning.actor_critic.CustomActorCritic;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.DeepQLearning;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.IExplorationPolicy;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.exploration_policy.EpsilonGreedyExploration;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.AbstractState;
//import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.DiscreteTAState;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.MarketState;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.StateManager;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public abstract class DQNAbstractMarketMaking extends SingleInstrumentAlgorithm {

	public static boolean DELTA_REWARD = true;
	public static boolean SAVE_ZERO_REWARDS = false;//save memory when no reward!

	protected static int DEFAULT_MAX_BATCH_SIZE = (int) 1E3;
	protected static int DEFAULT_EPOCH = (int) 50;
	protected static int DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD = -1;
	protected static int DEFAULT_TRAINING_TARGET_ITERATION_PERIOD = DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD * 5;

	public static double NO_TRADE_ACTION_REWARD = DeepQLearning.DEFAULT_PREDICTION_ACTION_SCORE;//0.0
	protected static String BASE_MEMORY_PATH = Configuration.OUTPUT_PATH + File.separator;
	protected static int DEFAULT_LAST_Q = -1;//action 0

	///dqn fields
	//state
	protected AbstractState state;
	protected StateManager stateManager;

	protected int lastStatePosition = DEFAULT_LAST_Q;
	protected double[] lastStateArr = null;

	//reward
	protected ScoreEnum scoreEnum;
	protected double lastRewardQ = DEFAULT_LAST_Q;

	//agent
	protected double epsilon;
	protected DeepQLearning memoryReplay;//can be custom actor critic
	protected int trainingStats = 0;

	protected int trainingPredictIterationPeriod = DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD;
	protected int trainingTargetIterationPeriod = DEFAULT_TRAINING_TARGET_ITERATION_PERIOD;
	private MemoryReplayModel predictionModel;
	private MemoryReplayModel targetModel;

	protected int numberDecimalsMarketState, numberDecimalsCandleState, numberDecimalsPrivateState;
	protected double minMarketState, maxMarketState, minCandleState, maxCandleState, minPrivateState, maxPrivateState;
	protected int horizonTicksMarketState, horizonCandlesState, horizonTicksPrivateState;
	protected long horizonMinMsTick;

	protected String[] stateColumnsFilter = null;
	protected double l1, l2 = 0.;
	protected Date lastDateTrainPredict = null;
	protected Date lastDateTrainTarget = null;

	protected int lastActionQ = DEFAULT_LAST_Q;
	protected long lastTimestampQ = DEFAULT_LAST_Q;
	protected double discountFactor, learningRate;
	protected Depth lastDepth = null;
	protected int iterations = 0;

	protected int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;

	protected AbstractAction action;
	protected List<Integer> actionHistoricalList = new ArrayList<>();
	protected boolean isRNN = false;

	protected MarketMakingAlgorithm marketMakingAlgorithm;
	protected ReinforcementLearningType reinforcementLearningType;

	protected static int DEFAULT_QUEUE_TRADE_SIZE_MINUTES = 60;
	protected Queue<Double> queueTrades;
	protected Queue<Candle> queueTradeCandles;

	protected int timeHorizonSeconds;

	public DQNAbstractMarketMaking(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
			String algorithmInfo, Map<String, Object> parameters, ReinforcementLearningType reinforcementLearningType) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters);
		logger = LogManager.getLogger(DQNAbstractMarketMaking.class);
		this.reinforcementLearningType = reinforcementLearningType;
		constructorAbstract(parameters);
		createModels(parameters);
	}

	public DQNAbstractMarketMaking(String algorithmInfo, Map<String, Object> parameters,
			ReinforcementLearningType reinforcementLearningType) {
		super(algorithmInfo, parameters);
		logger = LogManager.getLogger(DQNAbstractMarketMaking.class);
		this.reinforcementLearningType = reinforcementLearningType;
		constructorAbstract(parameters);
		createModels(parameters);
	}

	//abstract methods
	protected abstract void updateCurrentCustomColumn(String instrumentPk);

	protected abstract void setAction(double[] actionValues, int actionNumber);

	protected abstract void onFinishedIteration(long msElapsed, double deltaRewardNormalized, double reward);

	public abstract void setParameters(Map<String, Object> parameters);

	//	@Override public void setCandleSideRules(Candle candle) {
	//		marketMakingAlgorithm.setCandleSideRules(candle);
	//	}

	@Override public void setInstrument(Instrument instrument) {
		marketMakingAlgorithm.setInstrument(instrument);
		super.setInstrument(instrument);
		//		if (this.state instanceof DiscreteTAState) {
		//			((DiscreteTAState) this.state).setInstrument(instrument);
		//		}
		this.state.setNumberOfDecimals(instrument.getNumberDecimalsPrice());
	}

	public void setAlgorithm(MarketMakingAlgorithm algorithm) {
		this.marketMakingAlgorithm = algorithm;
	}

	protected void setDQNMemoryReplay() throws Exception {
		IExplorationPolicy explorationPolicy = new EpsilonGreedyExploration(this.epsilon);
		switch (this.reinforcementLearningType) {
			case double_deep_q_learn:
				memoryReplay = new DeepQLearning(this.state, this.action, explorationPolicy, this.maxBatchSize,
						this.predictionModel, this.targetModel, isRNN, discountFactor, learningRate,
						trainingPredictIterationPeriod, trainingTargetIterationPeriod);
				break;
			//			case reinforce:
			//				memoryReplay = new CustomActorCritic(this.state, this.action, explorationPolicy, this.maxBatchSize,
			//						this.predictionModel, this.targetModel, isRNN, discountFactor, learningRate,
			//						trainingPredictIterationPeriod, trainingTargetIterationPeriod);
			//				break;
			case q_learn:
				memoryReplay = new DeepQLearning(this.state, this.action, explorationPolicy, this.maxBatchSize,
						this.predictionModel, this.targetModel, isRNN, discountFactor, learningRate,
						trainingPredictIterationPeriod, trainingTargetIterationPeriod);
				memoryReplay.asQLearn = true;
				break;

			default:
				System.err.println(Configuration
						.formatLog("reinforcementLearningType {} not defined! error", reinforcementLearningType));
				throw new Exception(Configuration
						.formatLog("reinforcementLearningType {} not defined! error", reinforcementLearningType));
		}

	}

	public void setMarketMakerParameters(Map<String, Object> parameters) {
		this.parameters = parameters;

		this.marketMakingAlgorithm.constructorForAbstract(algorithmConnectorConfiguration, algorithmInfo, parameters);
		this.marketMakingAlgorithm.setParameters(parameters);

		this.timeHorizonSeconds = getParameterInt(parameters, "timeHorizonSeconds");
		this.epsilon = getParameterDouble(parameters, "epsilon");

		// NN parameters
		this.scoreEnum = ScoreEnum.valueOf(getParameterString(parameters, "scoreEnum"));
		this.trainingStats = getParameterIntOrDefault(parameters, "trainingStats", 0);

		//deep RL
		this.trainingPredictIterationPeriod = getParameterIntOrDefault(parameters, "trainingPredictIterationPeriod",
				DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD);
		this.trainingTargetIterationPeriod = getParameterIntOrDefault(parameters, "trainingTargetIterationPeriod",
				DEFAULT_TRAINING_TARGET_ITERATION_PERIOD);

		///STATE configuration discretize
		this.horizonMinMsTick = getParameterInt(parameters, "horizonMinMsTick");

		this.minPrivateState = getParameterDouble(parameters, "minPrivateState");
		this.maxPrivateState = getParameterDouble(parameters, "maxPrivateState");
		this.horizonTicksPrivateState = getParameterInt(parameters, "horizonTicksPrivateState");
		this.numberDecimalsPrivateState = getParameterInt(parameters, "numberDecimalsPrivateState");

		this.minMarketState = getParameterDoubleOrDefault(parameters, "minMarketState", -1);
		this.maxMarketState = getParameterDoubleOrDefault(parameters, "maxMarketState", -1);
		this.horizonTicksMarketState = getParameterIntOrDefault(parameters, "horizonTicksMarketState",
				this.horizonTicksPrivateState);
		this.numberDecimalsMarketState = getParameterIntOrDefault(parameters, "numberDecimalsMarketState", 0);

		this.minCandleState = getParameterDoubleOrDefault(parameters, "minCandleState", -1);
		this.maxCandleState = getParameterDoubleOrDefault(parameters, "maxCandleState", -1);
		this.horizonCandlesState = getParameterIntOrDefault(parameters, "horizonCandlesState", 1);
		this.numberDecimalsCandleState = getParameterIntOrDefault(parameters, "numberDecimalsCandleState", 0);

		double quantity = (marketMakingAlgorithm.quantityBuy + marketMakingAlgorithm.quantitySell) / 2;
		this.state = new MarketState(this.scoreEnum, this.horizonTicksPrivateState, this.horizonTicksMarketState,
				this.horizonCandlesState, this.horizonMinMsTick, this.horizonMinMsTick, this.numberDecimalsPrivateState,
				this.numberDecimalsMarketState, this.numberDecimalsCandleState, this.minPrivateState,
				this.maxPrivateState, this.minMarketState, this.maxMarketState, this.minCandleState,
				this.maxCandleState, quantity, CandleType.time_1_min);

		this.stateColumnsFilter = getParameterArrayString(parameters, "stateColumnsFilter");
		if (this.stateColumnsFilter != null && this.stateColumnsFilter.length > 0) {
			Set<String> privateStatesList = new HashSet<>(((MarketState) this.state).getPrivateColumns());
			Set<String> columnsFilter = new HashSet<>(Arrays.asList(this.stateColumnsFilter));
			columnsFilter.addAll(privateStatesList);
			stateColumnsFilter = new String[columnsFilter.size()];
			stateColumnsFilter = columnsFilter.toArray(stateColumnsFilter);

			logger.info("filtering state columns : {}", Arrays.toString(stateColumnsFilter));
			this.state.setColumnsFilter(stateColumnsFilter);
		} else {
			logger.info("no filtering states");
		}
		String isRNNStr = getParameterStringOrDefault(parameters, "isRNN", "False");
		isRNN = isRNNStr.toLowerCase().equals("true");

		int states = this.state.getNumberOfColumns();
		int actions = this.action.getNumberActions();

		this.parameters = parameters;
		this.firstHourOperatingIncluded = getParameterIntOrDefault(parameters, "first_hour",
				FIRST_HOUR_DEFAULT);//UTC time 6 -9
		this.lastHourOperatingIncluded = getParameterIntOrDefault(parameters, "last_hour",
				LAST_HOUR_DEFAULT);//UTC  18-21

		if (this.firstHourOperatingIncluded != FIRST_HOUR_DEFAULT
				|| this.lastHourOperatingIncluded != LAST_HOUR_DEFAULT) {
			logger.info("Current time {} in utc is {}", new Date(), Instant.now());
			logger.info("start UTC hour at {} and finished on {} ,both included", this.firstHourOperatingIncluded,
					this.lastHourOperatingIncluded);

		}

		System.out.println(Configuration
				.formatLog("Starting {} with {} state columns and {} action reward columns", algorithmInfo, states,
						actions));
		logger.info(Configuration
				.formatLog("Starting {} with {} state columns and {} action reward columns", algorithmInfo, states,
						actions));

		algorithmNotifier.notifyObserversOnUpdateParams(this.parameters);

	}

	private void createModels(Map<String, Object> parameters) {
		//done in constructor get the needed parameters
		int epoch = getParameterIntOrDefault(parameters, "epoch", DEFAULT_EPOCH);
		this.maxBatchSize = getParameterIntOrDefault(parameters, "maxBatchSize", DEFAULT_MAX_BATCH_SIZE);
		double l1 = getParameterDoubleOrDefault(parameters, "l1", 0.);
		double l2 = getParameterDoubleOrDefault(parameters, "l2", 0.);
		//		double learningRate = getParameterDoubleOrDefault(parameters, "learningRate", 0.);
		double learningRateNN = getParameterDoubleOrDefault(parameters, "learningRateNN", 0.1);
		this.discountFactor = getParameterDoubleOrDefault(parameters, "discountFactor", 0.95);
		learningRate = getParameterDoubleOrDefault(parameters, "learningRate", 0.25);
		int batchSize = getParameterIntOrDefault(parameters, "batchSize", maxBatchSize / 10);
		double momentumNesterov = getParameterDoubleOrDefault(parameters, "momentumNesterov", discountFactor);

		switch (reinforcementLearningType) {
			case reinforce:
				predictionModel = new Dl4jClassificationMemoryReplayModel(getPredictModelPath(), learningRateNN,
						momentumNesterov, epoch, batchSize, maxBatchSize, l2, l1, isRNN);
				targetModel = new Dl4jMemoryReplayModel(getTargetModelPath(), learningRateNN, momentumNesterov, epoch,
						batchSize, maxBatchSize, l2, l1, isRNN);
				break;
			case double_deep_q_learn:
				predictionModel = new Dl4jMemoryReplayModel(getPredictModelPath(), learningRateNN, momentumNesterov,
						epoch, batchSize, maxBatchSize, l2, l1, isRNN);
				targetModel = new Dl4jMemoryReplayModel(getTargetModelPath(), learningRateNN, momentumNesterov, epoch,
						batchSize, maxBatchSize, l2, l1, isRNN);
				break;
			case q_learn:
				break;
			default:
				logger.error("reinforcementLearningType {} not found type to create model", reinforcementLearningType);

		}

	}

	protected void constructorAbstract(Map<String, Object> parameters) {

		if (reinforcementLearningType == ReinforcementLearningType.q_learn) {
			System.out.println("Mark as QLearn!");
			logger.info("mark as qlearn algorithm");
			predictionModel = null;
			targetModel = null;
		}

		queueTrades = EvictingQueue.create(DEFAULT_QUEUE_TRADE_SIZE_MINUTES);
		queueTradeCandles = EvictingQueue.create(DEFAULT_QUEUE_TRADE_SIZE_MINUTES);
		cfTradesProcessed = EvictingQueue.create(DEFAULT_QUEUE_CF_TRADE);
		setParameters(parameters);
	}

	protected String getPredictModelPath() {
		return BASE_MEMORY_PATH + "predict_model_" + algorithmInfo + ".model";
	}

	protected String getMemoryPath() {
		return BASE_MEMORY_PATH + "memoryReplay_" + algorithmInfo + ".csv";
	}

	protected String getTargetModelPath() {
		return BASE_MEMORY_PATH + "target_model_" + algorithmInfo + ".model";
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
		if (this.memoryReplay != null) {
			IExplorationPolicy explorationPolicy = new EpsilonGreedyExploration(this.epsilon);
			this.memoryReplay.setExplorationPolicy(explorationPolicy);
		}

	}

	private void resetLastQValues() {
		lastActionQ = DEFAULT_LAST_Q;
		lastTimestampQ = DEFAULT_LAST_Q;
		lastStateArr = null;
		lastStatePosition = DEFAULT_LAST_Q;

	}

	public synchronized void init() {
		super.init();

		stateManager = new StateManager(this, state);
		boolean isQLearn = reinforcementLearningType == ReinforcementLearningType.q_learn;

		if (!isQLearn && this.trainingStats > 0 && predictionModel instanceof Dl4jMemoryReplayModel) {
			((Dl4jMemoryReplayModel) predictionModel).setTrainingStats(true);
			((Dl4jMemoryReplayModel) targetModel).setTrainingStats(true);
		}

		try {
			setDQNMemoryReplay();
			if (seed != 0) {
				memoryReplay.setSeed(seed);
				if (!isQLearn) {
					predictionModel.setSeed(seed);
					targetModel.setSeed(seed);
				}
				logger.info("{} memoryReplay with seed {}", algorithmInfo, seed);
				System.out.println(algorithmInfo + " memoryReplay with seed " + seed);
			} else {
				logger.info("{} memoryReplay with random seed", algorithmInfo);
				System.out.println(algorithmInfo + " memoryReplay with random seed ");
			}

			String memoryPath = getMemoryPath();
			File file = new File(memoryPath);
			if (!file.exists()) {
				if (seed == 0) {
					System.out.println(
							algorithmInfo + " memory not found -> start empty with random seed (" + seed + ")");
				} else {
					System.out.println(algorithmInfo + " memory not found -> start empty with fixed seed " + seed);
				}

			}

			this.memoryReplay.loadMemory(memoryPath);

		} catch (Exception e) {
			logger.error("cant create DeepQLearning exploration policy is wrong?", e);
			System.err.println("Cant load DeepQLearning! => memory replay is null!");
			System.exit(-1);
		}
		memoryReplay.asQLearn = reinforcementLearningType == ReinforcementLearningType.q_learn;
		setEpsilon(this.epsilon);
		initAlgorithm();

		logger.info("Starting with {} state columns and {} action columns -> memory replay buffer with {} columns",
				this.state.getNumberOfColumns(), this.action.getNumberActions(), memoryReplay.getNumberOfColumns());

	}

	protected void initAlgorithm() {
		this.marketMakingAlgorithm.setAlgorithmConnectorConfiguration(algorithmConnectorConfiguration);
		this.marketMakingAlgorithm.init();

		//unregister market data to call from here
		this.algorithmConnectorConfiguration.getMarketDataProvider().deregister(this.marketMakingAlgorithm);
	}

	protected double getPosition() {
		return getAlgorithmPosition(instrument);
	}

	//LISTENERS
	@Override public void onUpdateCandle(Candle candle) {
		super.onUpdateCandle(candle);
		marketMakingAlgorithm.onUpdateCandle(candle);//first underlying algo

	}

	@Override public boolean onTradeUpdate(Trade trade) {
		if (!trade.getInstrument().equalsIgnoreCase(instrument.getPrimaryKey())) {
			return false;
		}
		boolean output = super.onTradeUpdate(trade);
		marketMakingAlgorithm.onTradeUpdate(trade);
		return output;
	}

	@Override public boolean isReady() {
		boolean weAreReady = super.isReady();
		boolean mmIsReady = marketMakingAlgorithm != null && marketMakingAlgorithm.isReady();
		return weAreReady && mmIsReady;
	}

	@Override public boolean onDepthUpdate(Depth depth) {

		if (!depth.getInstrument().equals(instrument.getPrimaryKey())) {
			return false;
		}
		if (lastDepth == null) {
			this.lastDepth = depth;
		}

		if (stateManager != null && stateManager.isReady() && isReady()) {
			synchronized (this) {
				if (!isActionSend()) {
					//getNext Action
					if (iterations == 0) {
						logger.info("[{}] First iteration *****", getCurrentTime());
					}
					int action = getNextAction(this.state);

					double[] actionValues = null;
					try {
						actionValues = this.action.getAction(action);
					} catch (Exception e) {
						int newAction = getNextAction(this.state);
						logger.error("error getting action {} -> try another random {} !", action, newAction);
						actionValues = this.action.getAction(newAction);
					}

					lastDepth = depth;

					setAction(actionValues, action);//
					actionHistoricalList.add(action);
					lastRewardQ = getCurrentReward();
					lastActionQ = action;
					lastTimestampQ = depth.getTimestamp();
					lastStatePosition = this.state.getCurrentStatePosition();
					lastStateArr = this.state.getCurrentStateRounded();
				} else {
					int milisElapsed = (int) (depth.getTimestamp() - lastTimestampQ);
					if (milisElapsed > timeHorizonSeconds * 1000) {
						double reward = getCurrentReward();
						double rewardDelta = reward;
						if (DELTA_REWARD) {
							rewardDelta = reward - lastRewardQ;
						}
						double qtyMean =
								(marketMakingAlgorithm.getQuantityBuy() + marketMakingAlgorithm.getQuantitySell()) / 2;

						rewardDelta = rewardDelta / qtyMean;
						onFinishedIteration(milisElapsed, rewardDelta, reward);
						if (SAVE_ZERO_REWARDS || (!SAVE_ZERO_REWARDS && rewardDelta != 0)) {
							updateMemoryReplay(lastStateArr, lastStatePosition, lastActionQ, rewardDelta, this.state);
						}

						resetLastQValues();
					}
				}
			}

		}
		updateCurrentCustomColumn(depth.getInstrument());

		boolean output = super.onDepthUpdate(depth);
		marketMakingAlgorithm.onDepthUpdate(depth);

		return output;
	}

	@Override public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
		boolean output = true;
		try {
			output = super.onExecutionReportUpdate(executionReport);
			marketMakingAlgorithm.onExecutionReportUpdate(executionReport);

		} catch (Exception e) {
			logger.error("error treating ER ", e);
			output = false;
		}

		if (!isBacktest) {
			//			System.out.println("ER received " + executionReport);
		}

		//treat rejections before super!!
		boolean isRejected = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected);

		boolean isFilled = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)
				|| executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled);

		if (isFilled) {
			logger.info("[{}] ER filled received : {}", this.getCurrentTime(), executionReport);
		}

		if (isFilled && this.getPosition() == 0) {
			long msElapsed = executionReport.getTimestampCreation() - lastTimestampQ;
			//			int secondsElapsed = (int) ((msElapsed) / 1000);
			if (msElapsed == 0) {
				logger.warn("ER received in the same ms as send action!!");
			}
			//time to update q matrix
			double reward = getCurrentReward();
			double deltaReward = reward;
			if (DELTA_REWARD) {
				deltaReward = reward - lastRewardQ;
			}
			double qtyMean = (marketMakingAlgorithm.getQuantityBuy() + marketMakingAlgorithm.getQuantitySell()) / 2;
			double deltaRewardNormalized = deltaReward / qtyMean;//to avoid differences on uptades based on qty!
			onFinishedIteration(msElapsed, deltaRewardNormalized, reward);

			updateMemoryReplay(lastStateArr, lastStatePosition, lastActionQ, deltaRewardNormalized, this.state);
			resetLastQValues();

		}

		return output;
	}

	@Override public boolean onCommandUpdate(Command command) {
		if (command.getMessage().equalsIgnoreCase(Command.ClassMessage.stop.name())) {
			logger.info("finished with {} iterations", iterations);
			if (memoryReplay != null) {
				try {

					if (IterationsPeriodTime.isPeriodicalPeriod(this.trainingPredictIterationPeriod)
							&& this.trainingPredictIterationPeriod == IterationsPeriodTime.END_OF_SESSION.getValue()) {
						logger.info("end of session training on  memoryReplay of {} rows ",
								this.memoryReplay.getMemoryReplaySize());
						trainPrediction();
					}

					if (IterationsPeriodTime.isPeriodicalPeriod(this.trainingTargetIterationPeriod)
							&& this.trainingTargetIterationPeriod == IterationsPeriodTime.END_OF_SESSION.getValue()) {
						logger.info("end of session training on  memoryReplay of {} rows ",
								this.memoryReplay.getMemoryReplaySize());

						trainTarget();
					}

					memoryReplay.saveMemory(getMemoryPath());
				} catch (IOException e) {
					logger.error("cant save memoryReplay ", e);
				}
			}
			//force last save
			if (memoryReplay instanceof DeepQLearning) {
				((DeepQLearning) memoryReplay).commandStopReceived();
			}

			Integer[] actionHistoricalArray = new Integer[actionHistoricalList.size()];
			actionHistoricalArray = actionHistoricalList.toArray(new Integer[actionHistoricalList.size()]);

			logger.info("historical action List\n{}", ArrayUtils.toStringArray(actionHistoricalArray));
			logger.info("exploreActionsPct={}", memoryReplay.getExplorePct());
		}

		this.setPlotStopHistorical(false);
		//		this.setExitOnStop(true);
		boolean output = super.onCommandUpdate(command);

		marketMakingAlgorithm.onCommandUpdate(command);
		return output;
	}

	public void stop() {
		if (!isBacktest) {
			//train on live trading end of session
			if (IterationsPeriodTime.isPeriodicalPeriod(this.trainingPredictIterationPeriod)
					&& this.trainingPredictIterationPeriod == IterationsPeriodTime.END_OF_SESSION.getValue()) {
				logger.info("end of day live trading training on  memoryReplay of {} rows ",
						this.memoryReplay.getMemoryReplaySize());
				trainPrediction();
			}

			if (IterationsPeriodTime.isPeriodicalPeriod(this.trainingTargetIterationPeriod)
					&& this.trainingTargetIterationPeriod == IterationsPeriodTime.END_OF_SESSION.getValue()) {
				logger.info("end of day live trading training on  memoryReplay of {} rows ",
						this.memoryReplay.getMemoryReplaySize());
				trainTarget();
			}

		}
		marketMakingAlgorithm.stop();
		super.stop();
	}

	public void start() {
		marketMakingAlgorithm.start();
		super.start();
	}

	//training
	protected double[][] getInput() {
		double[][] input = null;
		switch (this.reinforcementLearningType) {
			//			case reinforce:
			//				input = ((CustomActorCritic) memoryReplay).getInputTrainClassif();
			//				break;

			default:
				//			case double_deep_q_learn:
				input = memoryReplay.getInputTrain();
				break;

		}
		return input;

	}

	protected double[][] getTarget() {
		double[][] target = null;
		switch (this.reinforcementLearningType) {
			//			case reinforce:
			//				target = ((CustomActorCritic) memoryReplay).getTargetTrainClassif();
			//				break;

			default:
				//			case double_deep_q_learn:
				target = memoryReplay.getTargetTrain();
				break;

		}
		return target;

	}

	private void trainPrediction() {
		if (reinforcementLearningType == ReinforcementLearningType.q_learn) {
			logger.info("no training in QLearn");
			return;
		}

		double[][] input = getInput();
		double[][] target = getTarget();

		assert input.length == target.length;
		System.out.println(
				"training prediction " + reinforcementLearningType + " model on " + input.length + " rows matrix");

		this.predictionModel.train(input, target);
		memoryReplay.setPredictModel(this.predictionModel);
		lastDateTrainPredict = this.getCurrentTime();

	}

	private void trainTarget() {
		if (reinforcementLearningType == ReinforcementLearningType.q_learn) {
			logger.info("no training in QLearn");
			return;
		}
		switch (this.reinforcementLearningType) {
			case reinforce:
				double[][] input = memoryReplay.getInputTrain();
				double[][] target = memoryReplay.getTargetTrain();
				assert input.length == target.length;
				System.out.println(
						"training target " + reinforcementLearningType + " model on " + input.length + " rows matrix");

				this.targetModel.train(input, target);
				memoryReplay.setTargetModel(this.predictionModel);
				lastDateTrainTarget = this.getCurrentTime();
			default:
				System.out.println("cloning target " + reinforcementLearningType + " model from predict");
				if (!predictionModel.isTrained()) {
					logger.warn("training predict before!!! ");
					System.out.println("training predict " + reinforcementLearningType + " to clone later");
					trainPrediction();
				}

				this.targetModel = this.predictionModel.cloneIt(targetModel.getModelPath());
				memoryReplay.setTargetModel(this.targetModel);
				lastDateTrainTarget = this.getCurrentTime();
				this.targetModel.saveModel();

		}
	}

	//// iterations
	public void updateMemoryReplay(double[] lastStateRoundedArr, int lastStatePosition, int lastAction,
			double rewardDelta, AbstractState newState) {
		if (memoryReplay == null) {
			logger.error("trying to update null memoryReplay!!");
			return;
		}
		if (lastStateRoundedArr == null) {
			logger.warn("updateMemoryReplay without previous state!!-> skip this update with rewardDelta {} ",
					rewardDelta);
			return;
		}

		memoryReplay.updateState(getCurrentTime(), lastStateRoundedArr, lastActionQ, rewardDelta, this.state);
	}

	protected double getCurrentReward() {
		PnlSnapshot lastPnlSnapshot = getLastPnlSnapshot(this.instrument.getPrimaryKey());
		return ScoreUtils.getReward(scoreEnum, lastPnlSnapshot);
	}

	protected boolean isActionSend() {
		return lastActionQ != DEFAULT_LAST_Q && lastTimestampQ != DEFAULT_LAST_Q && lastStateArr != null;
	}

	public int getNextAction(AbstractState state) {
		iterations++;

		if (lastDateTrainPredict == null) {
			lastDateTrainPredict = this.getCurrentTime();//initial
			System.out.println("set initial lastDateTrainPredict to " + lastDateTrainPredict + " time");
			logger.info("set initial lastDateTrainPredict to {}", lastDateTrainPredict);
		}

		if (lastDateTrainTarget == null) {
			lastDateTrainTarget = this.getCurrentTime();//initial
			System.out.println("set initial lastDateTrainTarget to " + lastDateTrainTarget + " time");
			logger.info("set initial lastDateTrainTarget to {}", lastDateTrainTarget);
		}

		return memoryReplay.GetAction(state);
	}

}
