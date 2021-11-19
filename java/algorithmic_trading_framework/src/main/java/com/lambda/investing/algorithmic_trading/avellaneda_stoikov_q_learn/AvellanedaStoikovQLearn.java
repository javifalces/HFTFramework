package com.lambda.investing.algorithmic_trading.avellaneda_stoikov_q_learn;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.LogLevels;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.avellaneda_stoikov.AvellanedaStoikov;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreEnum;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreUtils;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.action.AvellanedaAction;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.IExplorationPolicy;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.QLearning;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.exploration_policy.EpsilonGreedyExploration;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.AbstractState;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.PrivateState;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.StateManager;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.messaging.Command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AvellanedaStoikovQLearn extends AvellanedaStoikov {

	protected List<Integer> actionHistoricalList = new ArrayList<>();
	protected static int DEFAULT_LAST_Q = -1;
	protected static String BASE_MEMORY_PATH = Configuration.OUTPUT_PATH + File.separator;
	protected double[] riskAversionAction, skewPricePctAction;
	protected double minPrivateState, maxPrivateState;
	protected int[] windowsTickAction;
	protected int horizonTicksPrivateState;
	protected int numberDecimalsPrivateState;
	protected int horizonMinMsTick;
	protected long timeHorizonSeconds;
	protected ScoreEnum scoreEnum;

	protected double epsilon;
	private QLearning memoryReplay;
	protected AbstractState state;
	protected AvellanedaAction avellanedaAction;

	protected StateManager stateManager;

	protected int lastStatePosition = DEFAULT_LAST_Q;
	protected double[] lastStateArr = null;
	protected int lastActionQ = DEFAULT_LAST_Q;
	protected long lastTimestampQ = DEFAULT_LAST_Q;
	protected double lastRewardQ = DEFAULT_LAST_Q;
	protected double discountFactor, learningRate;
	private Depth lastDepth = null;

	protected int iterations = 0;

	public AvellanedaStoikovQLearn(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
			String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters);
		setParameters(parameters);

	}

	public AvellanedaStoikovQLearn(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);
		setParameters(parameters);
	}

	protected String getMemoryPath() {
		return BASE_MEMORY_PATH + "qmatrix_" + algorithmInfo + ".csv";
	}

	private String getCachePermutationsPath() {
		return BASE_MEMORY_PATH + "permutationsStates_" + algorithmInfo + ".csv";
	}

	@Override public void setParameters(Map<String, Object> parameters) {
		fixParametersForStaticAvellanedaStoikov(parameters);
		super.setParameters(parameters);
		//get Parameters
		this.riskAversionAction = getParameterArrayDouble(parameters, "riskAversionAction");
		this.riskAversionAction = Arrays.stream(this.riskAversionAction).distinct().sorted().toArray();
		this.skewPricePctAction = getParameterArrayDouble(parameters, "skewPricePctAction");
		this.skewPricePctAction = Arrays.stream(this.skewPricePctAction).distinct().sorted().toArray();
		this.windowsTickAction = getParameterArrayInt(parameters, "windowsTickAction");
		this.windowsTickAction = Arrays.stream(this.windowsTickAction).distinct().sorted().toArray();

		this.minPrivateState = getParameterDouble(parameters, "minPrivateState");
		this.maxPrivateState = getParameterDouble(parameters, "maxPrivateState");

		this.discountFactor = getParameterDoubleOrDefault(parameters, "discountFactor", 0.95);
		this.learningRate = getParameterDoubleOrDefault(parameters, "learningRate", 0.25);

		this.horizonTicksPrivateState = getParameterInt(parameters, "horizonTicksPrivateState");
		this.numberDecimalsPrivateState = getParameterInt(parameters, "numberDecimalsPrivateState");
		this.horizonMinMsTick = getParameterInt(parameters, "horizonMinMsTick");
		this.timeHorizonSeconds = getParameterInt(parameters, "timeHorizonSeconds");
		this.scoreEnum = ScoreEnum.valueOf(getParameterString(parameters, "scoreEnum"));
		this.epsilon = getParameterDouble(parameters,
				"epsilon");//explore probability random.uniform(0, 1) < epsilon => explore
		setEpsilon(this.epsilon);
		this.state = new PrivateState(this.scoreEnum, this.numberDecimalsPrivateState, this.horizonTicksPrivateState,
				this.horizonMinMsTick, this.maxPrivateState, this.minPrivateState, this.quantity);

		this.avellanedaAction = new AvellanedaAction(this.windowsTickAction, this.riskAversionAction,
				this.skewPricePctAction);
		logger.info("[{}]set parameters  {}\n{}", getCurrentTime(), algorithmInfo);

	}

	private void fixParametersForStaticAvellanedaStoikov(Map<String, Object> parameters) {
		//returns most reactive -defenssive initially
		int[] windowsTickAction = getParameterArrayInt(parameters, "windowsTickAction");
		int minWindow = 999999999;
		for (int window : windowsTickAction) {
			minWindow = Math.min(window, minWindow);
		}
		parameters.put("window_tick", String.valueOf(minWindow));

		double[] riskAversionAction = getParameterArrayDouble(parameters, "riskAversionAction");
		double maxRiskAversion = 0.;
		for (double risk : riskAversionAction) {
			maxRiskAversion = Math.max(risk, maxRiskAversion);
		}
		parameters.put("risk_aversion", String.valueOf(maxRiskAversion));

	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
		if (this.memoryReplay != null) {
			IExplorationPolicy explorationPolicy = new EpsilonGreedyExploration(this.epsilon);
			this.memoryReplay.setExplorationPolicy(explorationPolicy);
		}

	}

	public void init(boolean withMemory) {
		super.init();
		if (withMemory) {
			//enumerate states
			this.state.enumerateStates(getCachePermutationsPath());

			IExplorationPolicy explorationPolicy = new EpsilonGreedyExploration(this.epsilon);
			this.memoryReplay = new QLearning(this.state.getNumberStates(), this.avellanedaAction.getNumberActions(),
					explorationPolicy, discountFactor, learningRate);
			this.memoryReplay.init(false);

			try {
				this.memoryReplay.loadMemory(getMemoryPath());
			} catch (IOException e) {
				logger.error("cant load memory {}", getMemoryPath());
			}
		}

		int maxWindowsTick = this.windowsTickAction[0];
		for (int i = 0; i < this.windowsTickAction.length; i++) {
			maxWindowsTick = Math.max(maxWindowsTick, this.windowsTickAction[i]);
		}

		//State Registration
		stateManager = new StateManager(this, state);

		//saving maximun of max prices
		super.setMidPricesQueue(maxWindowsTick);

	}

	public void init() {
		init(true);
	}

	protected boolean isActionSend() {
		return lastActionQ != DEFAULT_LAST_Q && lastTimestampQ != DEFAULT_LAST_Q && lastStateArr != null;
	}

	private void resetLastQValues() {
		lastRewardQ = DEFAULT_LAST_Q;
		lastActionQ = DEFAULT_LAST_Q;
		lastTimestampQ = DEFAULT_LAST_Q;
		lastStateArr = null;
		lastStatePosition = DEFAULT_LAST_Q;

	}

	public void updateMemoryReplay(double[] lastStateArr, int lastStatePosition, int lastAction, double rewardDelta,
			AbstractState newState) {
		int currentStateNew = newState.getCurrentStatePosition();
		if (memoryReplay == null) {
			logger.error("trying to update null memoryReplay!!");
			return;
		}
		memoryReplay.updateState(lastStatePosition, lastAction, rewardDelta, currentStateNew);
	}

	public int getNextAction(AbstractState state) {
		iterations++;
		int currentState = state.getCurrentStatePosition();
		return memoryReplay.GetAction(currentState);
	}

	private void updateCurrentCustomColumn(String instrumentPk) {
		//add custom columns to trade csv
		addCurrentCustomColumn(instrumentPk, "windowTick", (double) super.windowTick);
		addCurrentCustomColumn(instrumentPk, "riskAversion", super.riskAversion);
		addCurrentCustomColumn(instrumentPk, "skewPricePct", super.skewPricePct);
		addCurrentCustomColumn(instrumentPk, "iterations", (double) iterations);
		//		parameters
		try {
			addCurrentCustomColumn(instrumentPk, "bid", (double) this.lastDepth.getBestBid());
			addCurrentCustomColumn(instrumentPk, "ask", (double) this.lastDepth.getBestAsk());
			addCurrentCustomColumn(instrumentPk, "bid_qty", (double) this.lastDepth.getBestBidQty());
			addCurrentCustomColumn(instrumentPk, "ask_qty", (double) this.lastDepth.getBestAskQty());
			addCurrentCustomColumn(instrumentPk, "imbalance", (double) this.lastDepth.getImbalance());
			addCurrentCustomColumn(instrumentPk, "reward", (double) this.lastRewardQ);
		} catch (Exception e) {
		}
	}

	@Override public boolean onDepthUpdate(Depth depth) {
		if (lastDepth == null) {
			this.lastDepth = depth;
		}
		if (stateManager.isReady()) {
			if (!isActionSend()) {
				//getNext Action
				int action = getNextAction(this.state);
				double[] actionValues = null;
				try {
					actionValues = this.avellanedaAction.getAction(action);
				} catch (Exception e) {
					int newAction = getNextAction(this.state);
					logger.error("error getting action {} -> try another random {} !", action, newAction);

					actionValues = this.avellanedaAction.getAction(newAction);
				}

				lastDepth = depth;

				super.windowTick = (int) Math.round(actionValues[AvellanedaAction.WINDOWS_INDEX]);
				super.riskAversion = actionValues[AvellanedaAction.RISK_AVERSION_INDEX];
				super.skewPricePct = actionValues[AvellanedaAction.SKEW_PRICE_INDEX];
				if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
					logger.info("set action {}   -> windowTick={}  riskAversion={}  skewPricePct={} ", action,
							super.windowTick, super.riskAversion, super.skewPricePct);
				}
				actionHistoricalList.add(action);
				lastRewardQ = getCurrentReward();
				lastActionQ = action;
				lastTimestampQ = depth.getTimestamp();
				lastStatePosition = this.state.getCurrentStatePosition();
				lastStateArr = this.state.getCurrentStateRounded();
			} else {
				//action is send
				int secondsElapsed = (int) ((depth.getTimestamp() - lastTimestampQ) / 1000);
				if (secondsElapsed > timeHorizonSeconds) {
					//time to update q matrix
					double reward = getCurrentReward();
					if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
						logger.info(
								"action {}   -> windowTick={}  riskAversion={}  skewPricePct={} -> currentReward={}   previouslyReward={}  rewardDelta={} => ",
								lastActionQ, super.windowTick, super.riskAversion, super.skewPricePct, reward,
								lastRewardQ, reward - lastRewardQ);
					}

					updateMemoryReplay(lastStateArr, lastStatePosition, lastActionQ, reward - lastRewardQ, this.state);
					resetLastQValues();
				}
			}

		}
		updateCurrentCustomColumn(depth.getInstrument());

		return super.onDepthUpdate(depth);
	}

	protected double getCurrentReward() {
		PnlSnapshot lastPnlSnapshot = getLastPnlSnapshot(this.instrument.getPrimaryKey());
		return ScoreUtils.getReward(scoreEnum, lastPnlSnapshot);
	}

	@Override public boolean onCommandUpdate(Command command) {
		if (command.getMessage().equalsIgnoreCase(Command.ClassMessage.stop.name())) {
			if (memoryReplay != null) {
				try {
					memoryReplay.saveMemory(getMemoryPath());
				} catch (IOException e) {
					logger.error("cant save qMatrix ", e);
				}
			}

		}

		this.setPlotStopHistorical(false);
		//		this.setExitOnStop(true);

		boolean output = super.onCommandUpdate(command);

		return output;
	}
}
