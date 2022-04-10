package com.lambda.investing.algorithmic_trading.avellaneda_stoikov;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.LogLevels;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.DQNAbstractMarketMaking;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ReinforcementLearningType;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.action.AvellanedaAction;
import org.apache.logging.log4j.LogManager;

import java.util.Map;

public class AlphaAvellanedaStoikov extends DQNAbstractMarketMaking {

	protected static ReinforcementLearningType RL_TYPE = ReinforcementLearningType.double_deep_q_learn;
	protected AvellanedaStoikov algorithm;

	public AlphaAvellanedaStoikov(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo,
			Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters, RL_TYPE);
		logger = LogManager.getLogger(AlphaAvellanedaStoikov.class);
	}

	public AlphaAvellanedaStoikov(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters, RL_TYPE);
		logger = LogManager.getLogger(AlphaAvellanedaStoikov.class);
	}

	@Override public String printAlgo() {
		return String
				.format("%s  \n\triskAversion=%.3f\n\tquantity=%.3f\n\twindowTick=%d\n\tfirstHourOperatingIncluded=%d\n\tlastHourOperatingIncluded=%d\n\tminutesChangeK=%d\n\tprivate_state  min=%.7f max=%.7f numberDecimals=%d horizon=%d\n\tmarket_state  min=%.7f max=%.7f numberDecimals=%d horizon=%d\n\tcandle_state  min=%.7f max=%.7f numberDecimals=%d horizon=%d\n\tscore_enum:%s\n\tmaxBatchSize:%d\n\titeration_train_predict:%d\n\titeration_train_target:%d",
						algorithmInfo, algorithm.riskAversion, algorithm.quantity, algorithm.windowTick,
						firstHourOperatingIncluded, lastHourOperatingIncluded, algorithm.minutesChangeK,
						minPrivateState, maxPrivateState, numberDecimalsPrivateState, horizonTicksPrivateState,
						minMarketState, maxMarketState, numberDecimalsMarketState, horizonTicksMarketState,
						minCandleState, maxCandleState, numberDecimalsCandleState, horizonCandlesState, scoreEnum,
						maxBatchSize, trainingPredictIterationPeriod, trainingTargetIterationPeriod);
	}

	@Override protected void updateCurrentCustomColumn(String instrumentPk) {
		//add custom columns to trade csv
		addCurrentCustomColumn(instrumentPk, "windowTick", (double) algorithm.windowTick);
		addCurrentCustomColumn(instrumentPk, "riskAversion", algorithm.riskAversion);
		addCurrentCustomColumn(instrumentPk, "skewPricePct", algorithm.skewPricePct);
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

	@Override protected void setAction(double[] actionValues, int actionNumber) {
		algorithm.skewPricePct = actionValues[AvellanedaAction.SKEW_PRICE_INDEX];
		algorithm.windowTick = (int) Math.round(actionValues[AvellanedaAction.WINDOWS_INDEX]);
		algorithm.riskAversion = actionValues[AvellanedaAction.RISK_AVERSION_INDEX];
		if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
			logger.info("[{}][iteration {}]set action {}  reward:{}  ->riskAversion={}  windowTick={}  skewPricePct={}",
					this.getCurrentTime(), this.iterations, actionNumber, getCurrentReward(), algorithm.riskAversion,
					algorithm.windowTick, algorithm.skewPricePct);
		}

	}

	@Override protected void onFinishedIteration(long msElapsed, double deltaRewardNormalized, double reward) {

		if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
			logger.info(
					"[{}][iteration {}]action {} finished  {} ms later   reward:{}->riskAversion={}  windowTick={}  skewPricePct={} -> currentReward={}   previouslyReward={}  rewardDeltaNormalized={}",
					this.getCurrentTime(), this.iterations, lastActionQ, msElapsed, getCurrentReward(),
					algorithm.riskAversion, algorithm.windowTick, algorithm.skewPricePct, reward, lastRewardQ,
					deltaRewardNormalized);
		}

	}

	@Override public void setParameters(Map<String, Object> parameters) {

		//ACTION configuration
		double[] riskAversionAction = getParameterArrayDouble(parameters, "riskAversionAction");
		int[] windowsTickAction = getParameterArrayInt(parameters, "windowsTickAction");
		double[] skewPricePctAction = getParameterArrayDouble(parameters, "skewPricePctAction");
		this.action = new AvellanedaAction(windowsTickAction, riskAversionAction, skewPricePctAction);
		double maxRiskAversion = Doubles.max(riskAversionAction);
		int maxWindowsTick = Ints.max(windowsTickAction);

		//creation of the algorithm
		parameters.put("risk_aversion", maxRiskAversion);
		parameters.put("window_tick", maxWindowsTick);
		algorithm = new AvellanedaStoikov(algorithmConnectorConfiguration, algorithmInfo, parameters);
		setAlgorithm(algorithm);

		logger.info("[{}] initial values   {}\n riskAversion:{} windowTick:{} skewPricePct:{}", getCurrentTime(),
				algorithmInfo, algorithm.riskAversion, algorithm.windowTick, algorithm.skewPricePct);

		//STATE configuration
		setMarketMakerParameters(parameters);

		logger.info("[{}]set parameters  {}", getCurrentTime(), algorithmInfo);

	}

}
