package com.lambda.investing.algorithmic_trading.constant_spread;

import com.google.common.primitives.Ints;
import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.LogLevels;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.DQNAbstractMarketMaking;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ReinforcementLearningType;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreEnum;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.action.ConstantSpreadAction;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.MarketState;
import com.lambda.investing.model.candle.CandleType;
import org.apache.logging.log4j.LogManager;

import java.util.Map;

public class AlphaConstantSpread extends DQNAbstractMarketMaking {

	protected static ReinforcementLearningType RL_TYPE = ReinforcementLearningType.double_deep_q_learn;
	protected ConstantSpreadAlgorithm algorithm;

	public AlphaConstantSpread(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo,
			Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters, RL_TYPE);
		logger = LogManager.getLogger(AlphaConstantSpread.class);
	}

	public AlphaConstantSpread(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters, RL_TYPE);
		logger = LogManager.getLogger(AlphaConstantSpread.class);
	}

	@Override public void setParameters(Map<String, Object> parameters) {

		//ACTION configuration
		int[] levels = getParameterArrayInt(parameters, "levelAction");
		int[] skewLevels = getParameterArrayInt(parameters, "skewLevelAction");
		this.action = new ConstantSpreadAction(levels, skewLevels);
		//initial values to underlying
		//creation of the algorithm
		parameters.put("level", Ints.max(levels));
		parameters.put("skewLevel", 0);

		algorithm = new ConstantSpreadAlgorithm(algorithmConnectorConfiguration, algorithmInfo, parameters);
		setAlgorithm(algorithm);

		logger.info("[{}] initial values   {}\n level:{} skewLevel:{}", getCurrentTime(), algorithmInfo,
				algorithm.level, algorithm.skewLevel);

		//STATE configuration
		setMarketMakerParameters(parameters);

		logger.info("[{}]set parameters  {}", getCurrentTime(), algorithmInfo);

	}

	@Override public String printAlgo() {
		return String
				.format("%s  quantityBuy=%.5f quantitySell=%.5f   ConstantDQNSpreadAlgorithm level=%d  skew_level=%d",
						algorithmInfo, algorithm.quantityBuy, algorithm.quantitySell, algorithm.level,
						algorithm.skewLevel);
	}

	@Override protected void updateCurrentCustomColumn(String instrumentPk) {
		addCurrentCustomColumn(instrumentPk, "level", (double) algorithm.level);
		addCurrentCustomColumn(instrumentPk, "skewLevel", (double) algorithm.skewLevel);
		addCurrentCustomColumn(instrumentPk, "iterations", (double) iterations);
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
		algorithm.level = (int) Math.round(actionValues[ConstantSpreadAction.LEVEL_INDEX]);
		algorithm.skewLevel = (int) Math.round(actionValues[ConstantSpreadAction.SKEW_LEVEL_INDEX]);
		if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
			logger.info("[{}][iteration {}]set action {}  reward:{}  ->level={}  skewLevel={}", this.getCurrentTime(),
					this.iterations, actionNumber, getCurrentReward(), algorithm.level, algorithm.skewLevel);
		}

	}

	@Override protected void onFinishedIteration(long msElapsed, double deltaRewardNormalized, double reward) {
		if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
			logger.info(
					"[{}][iteration {}]action {} finished  {} ms later  reward:{}  -> level={}  skewLevel={}  -> currentReward={}   previouslyReward={}  rewardDeltaNormalized={}",
					this.getCurrentTime(), iterations, lastActionQ, msElapsed, getCurrentReward(), algorithm.level,
					algorithm.skewLevel, reward, lastRewardQ, deltaRewardNormalized);
		}
	}

}
