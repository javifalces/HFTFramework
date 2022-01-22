package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.algorithmic_trading.avellaneda_stoikov.AvellanedaStoikov;
import com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn.AvellanedaStoikovDQNMarket;
//import com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn.RSI;
//import com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn.SMACross;
import com.lambda.investing.algorithmic_trading.avellaneda_stoikov_q_learn.AvellanedaStoikovQLearn;
import com.lambda.investing.algorithmic_trading.constant_spread.ConstantSpreadAlgorithm;
import com.lambda.investing.algorithmic_trading.constant_spread.LinearConstantSpreadAlgorithm;
//import com.lambda.investing.algorithmic_trading.mean_reversion.DQNRSISideQuoting;
//import com.lambda.investing.algorithmic_trading.mean_reversion.RSISideQuoting;
//import com.lambda.investing.algorithmic_trading.portfolio.FixedPortfolioAlgorithm;
//import com.lambda.investing.algorithmic_trading.portfolio.OnnxPortfolioAlgorithm;
//import com.lambda.investing.algorithmic_trading.statistical_arbitrage.StatisticalArbitrageQuotingAlgorithm;
//import com.lambda.investing.algorithmic_trading.trend_following.DQNMovingAverageSideQuoting;
//import com.lambda.investing.algorithmic_trading.trend_following.MovingAverageSideQuoting;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlgorithmUtils {

	public static Map<String, Object> getParameters(Map<String, Object> inputParameters) {
		Map<String, Object> output = new HashMap<>();
		for (String parameterKey : inputParameters.keySet()) {
			Object value = inputParameters.get(parameterKey);

			if (value instanceof List) {
				String commaSeparated = (String) ((List) value).stream().map(String::valueOf)
						.collect(Collectors.joining(","));

				output.put(parameterKey, commaSeparated);

			} else {
				output.put(parameterKey, String.valueOf(value));
			}

		}
		return output;
	}

	/**
	 * get algorithm by name and parameters only for Backtesting
	 *
	 * @param algorithmName
	 * @param parameters
	 * @return
	 */
	public static com.lambda.investing.algorithmic_trading.Algorithm getAlgorithm(String algorithmName,
			Map<String, Object> parameters) {
		//for backtest only!!
		return getAlgorithm(null, algorithmName, parameters);
	}

	/**
	 * Must return the same as in algorithm_enum.py
	 * get algorithm by name and parameters
	 *
	 * @return
	 */
	public static com.lambda.investing.algorithmic_trading.Algorithm getAlgorithm(
			AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmName,
			Map<String, Object> parameters) {

		//market making algorithms -> Phd
		if (algorithmName.startsWith("ConstantSpread")) {
			System.out.println("ConstantSpread backtest " + algorithmName);
			return new ConstantSpreadAlgorithm(algorithmConnectorConfiguration, algorithmName, parameters);
		}
		if (algorithmName.startsWith("LinearConstantSpread")) {
			System.out.println("LinearConstantSpread backtest " + algorithmName);
			return new LinearConstantSpreadAlgorithm(algorithmConnectorConfiguration, algorithmName, parameters);
		}

		if (algorithmName.startsWith("AvellanedaStoikov")) {
			System.out.println("AvellanedaStoikov backtest " + algorithmName);
			return new AvellanedaStoikov(algorithmConnectorConfiguration, algorithmName, parameters);
		}

		if (algorithmName.startsWith("AvellanedaDQN")) {
			System.out.println("AvellanedaDQN backtest " + algorithmName);
			return new AvellanedaStoikovDQNMarket(algorithmConnectorConfiguration, algorithmName, parameters);
		}

		if (algorithmName.startsWith("ConstantSpread")) {
			System.out.println("ConstantSpread backtest " + algorithmName);
			return new ConstantSpreadAlgorithm(algorithmConnectorConfiguration, algorithmName, parameters);
		}

		if (algorithmName.startsWith("AvellanedaQ")) {
			System.out.println("AvellanedaQ backtest " + algorithmName);
			return new AvellanedaStoikovQLearn(algorithmConnectorConfiguration, algorithmName, parameters);
		}

		///// Directional Algos
		//		if (algorithmName.startsWith("SMACross")) {
		//			System.out.println("SMACross backtest " + algorithmName);
		//			return new SMACross(algorithmConnectorConfiguration, algorithmName, parameters);
		//		}
		//
		//		if (algorithmName.startsWith("StatArb")) {
		//			System.out.println("StatArb backtest " + algorithmName);
		//			//				return new StatisticalArbitrageAlgorithm(algorithmName, parameters);
		//			return new StatisticalArbitrageQuotingAlgorithm(algorithmConnectorConfiguration, algorithmName, parameters);
		//		}
		//
		//		if (algorithmName.startsWith("RSISideQuoting")) {
		//			System.out.println("RSISideQuoting backtest " + algorithmName);
		//			//				return new StatisticalArbitrageAlgorithm(algorithmName, parameters);
		//			return new RSISideQuoting(algorithmConnectorConfiguration, algorithmName, parameters);
		//		}
		//		if (algorithmName.startsWith("DQNRSISideQuoting")) {
		//			System.out.println("DQNRSISideQuoting backtest " + algorithmName);
		//			//				return new StatisticalArbitrageAlgorithm(algorithmName, parameters);
		//			return new DQNRSISideQuoting(algorithmConnectorConfiguration, algorithmName, parameters);
		//		}
		//
		//		if (algorithmName.startsWith("MovingAverage")) {
		//			System.out.println("MovingAverageSideQuoting backtest " + algorithmName);
		//			//				return new StatisticalArbitrageAlgorithm(algorithmName, parameters);
		//			return new MovingAverageSideQuoting(algorithmConnectorConfiguration, algorithmName, parameters);
		//		}
		//		if (algorithmName.startsWith("DQNMovingAverage")) {
		//			System.out.println("DQNMovingAverageSideQuoting backtest " + algorithmName);
		//			//				return new StatisticalArbitrageAlgorithm(algorithmName, parameters);
		//			return new DQNMovingAverageSideQuoting(algorithmConnectorConfiguration, algorithmName, parameters);
		//		}
		//		if (algorithmName.startsWith("FixedPortfolioAlgorithm")) {
		//			System.out.println("FixedPortfolioAlgorithm backtest " + algorithmName);
		//			//				return new StatisticalArbitrageAlgorithm(algorithmName, parameters);
		//			return new FixedPortfolioAlgorithm(algorithmConnectorConfiguration, algorithmName, parameters);
		//		}
		//
		//		if (algorithmName.startsWith("OnnxPortfolioAlgorithm")) {
		//			System.out.println("OnnxPortfolioAlgorithm backtest " + algorithmName);
		//			//				return new StatisticalArbitrageAlgorithm(algorithmName, parameters);
		//			return new OnnxPortfolioAlgorithm(algorithmConnectorConfiguration, algorithmName, parameters);
		//		}

		System.err.println("algorithm " + algorithmName + " not found!");
		return null;
	}

	public static class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {

		private final int maxSize;

		public MaxSizeHashMap(int maxSize) {
			this.maxSize = maxSize;
		}

		@Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
			return size() > maxSize;
		}
	}

}
