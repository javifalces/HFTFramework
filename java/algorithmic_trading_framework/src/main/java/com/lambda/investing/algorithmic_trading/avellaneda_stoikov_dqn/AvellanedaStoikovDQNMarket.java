package com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.IterationsPeriodTime;
import com.lambda.investing.algorithmic_trading.avellaneda_stoikov_q_learn.AvellanedaStoikovQLearn;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.Dl4jMemoryReplayModel;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.MemoryReplayModel;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.DeepQLearning;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.IExplorationPolicy;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.exploration_policy.EpsilonGreedyExploration;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.AbstractState;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.MarketState;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.*;

public class AvellanedaStoikovDQNMarket extends AvellanedaStoikovQLearn {

	private static int DEFAULT_MAX_BATCH_SIZE = (int) 1E3;
	private static int DEFAULT_EPOCH = (int) 100;
	private static int DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD = -1;
	private static int DEFAULT_TRAINING_TARGET_ITERATION_PERIOD = DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD * 5;

	private DeepQLearning memoryReplay;

	private int trainingStats = 0;
	private int epoch = DEFAULT_EPOCH;
	private int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;
	private int trainingPredictIterationPeriod = DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD;
	private int trainingTargetIterationPeriod = DEFAULT_TRAINING_TARGET_ITERATION_PERIOD;
	private MemoryReplayModel predictionModel;
	private MemoryReplayModel targetModel;

	private int numberDecimalsMarketState, numberDecimalsCandleState;
	private double minMarketState, maxMarketState, minCandleState, maxCandleState;
	private int horizonTicksMarketState, horizonCandlesState;
	private String[] stateColumnsFilter = null;
	private double l1, l2 = 0.;

	//	private CandleManager candleManager;
	private boolean isRNN = false;
	private Date lastDateTrainPredict = null;
	private Date lastDateTrainTarget = null;

	public AvellanedaStoikovDQNMarket(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
			String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters);
		createModels(parameters);
	}

	public AvellanedaStoikovDQNMarket(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);
		createModels(parameters);
	}

	private void createModels(Map<String, Object> parameters) {
		//done in constructor get the needed parameters
		setParameters(parameters);////again to not overide last constructor
		int epoch = getParameterIntOrDefault(parameters, "epoch", DEFAULT_EPOCH);
		int maxBatchSize = getParameterIntOrDefault(parameters, "maxBatchSize", DEFAULT_MAX_BATCH_SIZE);
		double l1 = getParameterDoubleOrDefault(parameters, "l1", 0.);
		double l2 = getParameterDoubleOrDefault(parameters, "l2", 0.);
		double learningRate = getParameterDoubleOrDefault(parameters, "learningRate", 0.);
		double learningRateNN = getParameterDoubleOrDefault(parameters, "learningRateNN", 0.1);
		double discountFactor = getParameterDoubleOrDefault(parameters, "discountFactor", 0.5);
		int batchSize = getParameterIntOrDefault(parameters, "batchSize", maxBatchSize / 10);
		double momentumNesterov = getParameterDoubleOrDefault(parameters, "momentumNesterov", discountFactor);

		predictionModel = new Dl4jMemoryReplayModel(getPredictModelPath(), learningRateNN, momentumNesterov, epoch,
				batchSize, maxBatchSize, l2, l1, isRNN);

		targetModel = new Dl4jMemoryReplayModel(getTargetModelPath(), learningRateNN, momentumNesterov, epoch,
				batchSize, maxBatchSize, l2, l1, isRNN);

	}

	@Override public void setEpsilon(double epsilon) {
		super.setEpsilon(epsilon);

	}

	public void init() {
		super.init(false);
		//		candleManager = new CandleManager(this.stateManager);

		if (this.trainingStats > 0 && predictionModel instanceof Dl4jMemoryReplayModel) {
			((Dl4jMemoryReplayModel) predictionModel).setTrainingStats(true);
			((Dl4jMemoryReplayModel) targetModel).setTrainingStats(true);
		}

		IExplorationPolicy explorationPolicy = new EpsilonGreedyExploration(this.epsilon);
		try {
			memoryReplay = new DeepQLearning(this.state, this.avellanedaAction, explorationPolicy,
					predictionModel.getMaxBatchSize(), this.predictionModel, this.targetModel, isRNN, discountFactor,
					learningRate, this.trainingPredictIterationPeriod, this.trainingTargetIterationPeriod);

			if (seed != 0) {
				memoryReplay.setSeed(seed);
				predictionModel.setSeed(seed);
				targetModel.setSeed(seed);
				logger.info("{} memoryReplay with seed {}", algorithmInfo, seed);
				System.out.println(algorithmInfo + " memoryReplay with seed " + seed);
			} else {
				logger.info("{} memoryReplay with random seed {}", algorithmInfo);
				System.out.println(algorithmInfo + " memoryReplay with random seed ");
			}

			this.memoryReplay.loadMemory(getMemoryPath());

		} catch (Exception e) {
			logger.error("cant create DeepQLearning exploration policy is wrong?", e);
			System.err.println("Cant load DeepQLearning! => memory replay is null!");
			System.exit(-1);
		}

		logger.info("init for {} state columns and {} action columns", this.state.getNumberOfColumns(),
				this.avellanedaAction.getNumberActions());

	}

	protected String getMemoryPath() {
		return BASE_MEMORY_PATH + "memoryReplay_" + algorithmInfo + ".csv";
	}

	protected String getPredictModelPath() {
		return BASE_MEMORY_PATH + "predict_model_" + algorithmInfo + ".model";
	}

	protected String getTargetModelPath() {
		return BASE_MEMORY_PATH + "target_model_" + algorithmInfo + ".model";
	}

	@Override public String printAlgo() {
		return String
				.format("%s  \n\triskAversion=%.3f\n\tquantity=%.3f\n\twindowTick=%d\n\tfirstHourOperatingIncluded=%d\n\tlastHourOperatingIncluded=%d\n\tminutesChangeK=%d\n\tprivate_state  min=%.7f max=%.7f numberDecimals=%d horizon=%d\n\tmarket_state  min=%.7f max=%.7f numberDecimals=%d horizon=%d\n\tcandle_state  min=%.7f max=%.7f numberDecimals=%d horizon=%d\n\tscore_enum:%s\n\tmaxBatchSize:%d\n\titeration_train_predict:%d\n\titeration_train_target:%d",
						algorithmInfo, riskAversion, quantity, windowTick, firstHourOperatingIncluded,
						lastHourOperatingIncluded, minutesChangeK, minPrivateState, maxPrivateState,
						numberDecimalsPrivateState, horizonTicksPrivateState, minMarketState, maxMarketState,
						numberDecimalsMarketState, horizonTicksMarketState, minCandleState, maxCandleState,
						numberDecimalsCandleState, horizonCandlesState, scoreEnum, maxBatchSize,
						trainingPredictIterationPeriod, trainingTargetIterationPeriod);
	}

	@Override public void setParameters(Map<String, Object> parameters) {
		super.setParameters(parameters);
		this.trainingStats = getParameterIntOrDefault(parameters, "trainingStats", 0);
		this.epoch = getParameterIntOrDefault(parameters, "epoch", DEFAULT_EPOCH);
		this.maxBatchSize = getParameterIntOrDefault(parameters, "maxBatchSize", DEFAULT_MAX_BATCH_SIZE);

		this.trainingPredictIterationPeriod = getParameterIntOrDefault(parameters, "trainingPredictIterationPeriod",
				DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD);
		this.trainingTargetIterationPeriod = getParameterIntOrDefault(parameters, "trainingTargetIterationPeriod",
				DEFAULT_TRAINING_TARGET_ITERATION_PERIOD);

		this.numberDecimalsMarketState = getParameterIntOrDefault(parameters, "numberDecimalsMarketState", 0);
		this.numberDecimalsCandleState = getParameterIntOrDefault(parameters, "numberDecimalsCandleState", 0);

		this.horizonTicksMarketState = getParameterIntOrDefault(parameters, "horizonTicksMarketState",
				this.horizonTicksPrivateState);
		this.horizonCandlesState = getParameterIntOrDefault(parameters, "horizonCandlesState", 1);

		this.minMarketState = getParameterDoubleOrDefault(parameters, "minMarketState", -1);
		this.maxMarketState = getParameterDoubleOrDefault(parameters, "maxMarketState", -1);
		this.minCandleState = getParameterDoubleOrDefault(parameters, "minCandleState", -1);
		this.maxCandleState = getParameterDoubleOrDefault(parameters, "maxCandleState", -1);
		this.l1 = getParameterDoubleOrDefault(parameters, "l1", 0.);
		this.l2 = getParameterDoubleOrDefault(parameters, "l2", 0.);

		this.stateColumnsFilter = getParameterArrayString(parameters, "stateColumnsFilter");

		String isRNNStr = getParameterStringOrDefault(parameters, "isRNN", "False");
		isRNN = isRNNStr.toLowerCase().equals("true");
		//		if (isRNN) {
		//			//modify to normalize all horizon ticks and remove candles
		//			int maxHorizonSet = Math.max(this.horizonTicksMarketState, this.horizonTicksPrivateState);
		//			this.horizonTicksMarketState = maxHorizonSet;
		//			this.horizonTicksPrivateState = maxHorizonSet;
		//			//remove columns filter
		//			this.stateColumnsFilter = null;
		//			//remove candle states
		//			this.horizonCandlesState = 0;
		//		}

		this.state = new MarketState(this.instrument, this.scoreEnum, this.horizonTicksPrivateState,
				this.horizonTicksMarketState, this.horizonCandlesState, this.horizonMinMsTick, this.horizonMinMsTick,
				this.numberDecimalsPrivateState, this.numberDecimalsMarketState, this.numberDecimalsCandleState,
				this.minPrivateState, this.maxPrivateState, this.minMarketState, this.maxMarketState,
				this.minCandleState, this.maxCandleState, this.quantity, CandleType.time_1_min);

		if (this.stateColumnsFilter != null && this.stateColumnsFilter.length > 0) {
			Set<String> privateStatesList = new HashSet<>(((MarketState) this.state).getPrivateColumns());
			Set<String> columnsFilter = new HashSet<>(Arrays.asList(this.stateColumnsFilter));
			columnsFilter.addAll(privateStatesList);
			stateColumnsFilter = new String[columnsFilter.size()];
			stateColumnsFilter = columnsFilter.toArray(stateColumnsFilter);

			logger.info("filtering state columns : {}", Arrays.toString(stateColumnsFilter));
			this.state.setColumnsFilter(stateColumnsFilter);
		}

		logger.info("[{}]set parameters  {}\n{}", getCurrentTime(), algorithmInfo);

	}

	public void updateMemoryReplay(double[] lastStateRoundedArr, int lastStatePosition, int lastAction,
			double rewardDelta, AbstractState newState) {
		if (memoryReplay == null) {
			logger.error("trying to update null memoryReplay!!");
			return;
		}

		memoryReplay.updateState(getCurrentTime(), lastStateRoundedArr, lastActionQ, rewardDelta, this.state);
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

	private void trainPrediction() {
		double[][] input = memoryReplay.getInputTrain();
		double[][] target = memoryReplay.getTargetTrain();
		assert input.length == target.length;
		this.predictionModel.train(input, target);
		memoryReplay.setPredictModel(this.predictionModel);

	}

	private void trainTarget() {
		this.targetModel = this.predictionModel.cloneIt(targetModel.getModelPath());
		this.targetModel.saveModel();
		memoryReplay.setTargetModel(this.targetModel);

	}

	@Override public boolean onCommandUpdate(Command command) {
		if (command.getMessage().equalsIgnoreCase(Command.ClassMessage.stop.name())) {

			try {
				//train if end of session
				if (IterationsPeriodTime.isPeriodicalPeriod(this.trainingPredictIterationPeriod)
						&& this.trainingPredictIterationPeriod == IterationsPeriodTime.END_OF_SESSION.getValue()) {
					logger.info("end of session training on  memoryReplay of {} rows ",
							this.memoryReplay.getMemoryReplaySize());
					trainPrediction();
					trainTarget();
				}
				//force last save
				if (memoryReplay instanceof DeepQLearning) {
					((DeepQLearning) memoryReplay).commandStopReceived();
				}

				logger.info("saving memoryReplay of {} rows  into {}", this.memoryReplay.getMemoryReplaySize(),
						getMemoryPath());
				memoryReplay.saveMemory(getMemoryPath());

			} catch (IOException e) {
				logger.error("cant save memoryReplay in {} ", getMemoryPath(), e);
			}

			Integer[] actionHistoricalArray = new Integer[actionHistoricalList.size()];
			actionHistoricalArray = actionHistoricalList.toArray(new Integer[actionHistoricalList.size()]);

			logger.info("historical action List\n{}", ArrayUtils.toStringArray(actionHistoricalArray));

			logger.info("exploreActionsPct={}", memoryReplay.getExplorePct());
			//			logger.info("training model...");
			//			trainPrediction();
			//			trainTarget();

		}

		this.setPlotStopHistorical(false);
		//		this.setExitOnStop(true);

		boolean output = super.onCommandUpdate(command);

		return output;
	}

	@Override public boolean onDepthUpdate(Depth depth) {
		if (!depth.getInstrument().equalsIgnoreCase(instrument.getPrimaryKey())) {
			return false;
		}

		return super.onDepthUpdate(depth);

	}

	@Override public boolean onTradeUpdate(Trade trade) {
		if (!trade.getInstrument().equalsIgnoreCase(instrument.getPrimaryKey())) {
			return false;
		}
		return super.onTradeUpdate(trade);
	}
}
