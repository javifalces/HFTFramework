package com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.avellaneda_stoikov_q_learn.AvellanedaStoikovQLearn;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.DeepQLearning;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.IExplorationPolicy;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.exploration_policy.EpsilonGreedyExploration;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.AbstractState;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;

import java.io.IOException;
import java.util.Map;

public class AvellanedaStoikovDQNPrivate extends AvellanedaStoikovQLearn {

	private static int DEFAULT_MAX_BATCH_SIZE = (int) 1E3;
	private static int DEFAULT_EPOCH = (int) 50;
	private static int DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD = 500;
	private static int DEFAULT_TRAINING_TARGET_ITERATION_PERIOD = DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD * 5;

	private DeepQLearning memoryReplay;

	private int trainingStats = 0;
	private int epoch = DEFAULT_EPOCH;
	private int batchSize = 100;
	private int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;
	private int trainingPredictIterationPeriod = DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD;
	private int trainingTargetIterationPeriod = DEFAULT_TRAINING_TARGET_ITERATION_PERIOD;
	private MemoryReplayModel predictionModel;
	private MemoryReplayModel targetModel;

	//	private CandleManager candleManager;
	private boolean isRNN = false;

	public AvellanedaStoikovDQNPrivate(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
			String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters);
	}

	public AvellanedaStoikovDQNPrivate(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);

	}

	@Override public void setEpsilon(double epsilon) {
		super.setEpsilon(epsilon);

	}

	public void init() {
		super.init(false);
		//		predictionModel=new OnnxMemoryReplayModel(getPredictModelPath(),null);
		//		targetModel=new OnnxMemoryReplayModel(getTargetModelPath(),null);

		predictionModel = new Dl4jMemoryReplayModel(getPredictModelPath(), learningRate, 0.5, epoch, batchSize,
				maxBatchSize, 0.0001, 0, isRNN);
		targetModel = new Dl4jMemoryReplayModel(getTargetModelPath(), learningRate, 0.5, epoch, batchSize, maxBatchSize,
				0.0001, 0, isRNN);

		if (this.trainingStats > 0 && predictionModel instanceof Dl4jMemoryReplayModel) {
			((Dl4jMemoryReplayModel) predictionModel).setTrainingStats(true);
			((Dl4jMemoryReplayModel) targetModel).setTrainingStats(true);
		}

		IExplorationPolicy explorationPolicy = new EpsilonGreedyExploration(this.epsilon);
		try {
			memoryReplay = new DeepQLearning(this.state, this.avellanedaAction, explorationPolicy, maxBatchSize,
					this.predictionModel, this.targetModel, isRNN, discountFactor, learningRate);

			this.memoryReplay.loadMemory(getMemoryPath());

		} catch (Exception e) {
			logger.error("cant create DeepQLearning exporation policy is wrong?", e);
		}
		//		candleManager = new CandleManager(this.stateManager);

		//		stateManager = new StateManager(this);
		//		stateManager.setAbstractState(state);

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

	@Override public void setParameters(Map<String, Object> parameters) {
		super.setParameters(parameters);
		this.trainingStats = getParameterIntOrDefault(parameters, "trainingStats", 0);
		this.epoch = getParameterIntOrDefault(parameters, "epoch", DEFAULT_EPOCH);
		this.maxBatchSize = getParameterIntOrDefault(parameters, "maxBatchSize", DEFAULT_MAX_BATCH_SIZE);
		this.trainingPredictIterationPeriod = getParameterIntOrDefault(parameters, "trainingPredictIterationPeriod",
				DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD);
		this.trainingTargetIterationPeriod = getParameterIntOrDefault(parameters, "trainingTargetIterationPeriod",
				DEFAULT_TRAINING_TARGET_ITERATION_PERIOD);
	}

	public void updateMemoryReplay(double[] lastStateArr, int lastStatePosition, int lastAction, double rewardDelta,
			AbstractState newState) {
		if (memoryReplay == null) {
			logger.error("trying to update null memoryReplay!!");
			return;
		}

		memoryReplay.updateState(lastStateArr, lastActionQ, rewardDelta, this.state);
	}

	public int getNextAction(AbstractState state) {
		iterations++;
		if (trainingPredictIterationPeriod > 0 && (iterations % this.trainingPredictIterationPeriod) == 0) {
			System.out.println("training prediction model on " + iterations + " iteration");
			trainPrediction();

			if (!targetModel.isTrained()) {
				//copy first if not exist
				trainTarget();
			}
		}

		if (trainingTargetIterationPeriod > 0 && (iterations % this.trainingTargetIterationPeriod) == 0) {
			System.out.println("training target model on " + iterations + " iteration=> copy it");
			trainTarget();
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
		this.targetModel = this.predictionModel.cloneIt();
		this.targetModel.setModelPath(getTargetModelPath());
		this.targetModel.saveModel();
		memoryReplay.setTargetModel(this.targetModel);

	}

	@Override public boolean onCommandUpdate(Command command) {
		if (command.getMessage().equalsIgnoreCase(Command.ClassMessage.stop.name())) {

			try {
				memoryReplay.saveMemory(getMemoryPath());

			} catch (IOException e) {
				logger.error("cant save qMatrix ", e);
			}
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
		return super.onDepthUpdate(depth);

	}

	@Override public boolean onTradeUpdate(Trade trade) {
		return super.onTradeUpdate(trade);
	}
}
