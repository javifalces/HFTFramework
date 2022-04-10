package com.lambda.investing.algo_trading;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.AlgorithmUtils;
import com.lambda.investing.algorithmic_trading.SingleInstrumentAlgorithm;
import com.lambda.investing.algorithmic_trading.avellaneda_stoikov.AvellanedaStoikov;
import com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn.AvellanedaStoikovDQNMarket;
//import com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn.RSI;
//import com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn.SMACross;
import com.lambda.investing.algorithmic_trading.avellaneda_stoikov_q_learn.AvellanedaStoikovQLearn;
import com.lambda.investing.algorithmic_trading.constant_spread.ConstantSpreadAlgorithm;
import com.lambda.investing.algorithmic_trading.constant_spread.LinearConstantSpreadAlgorithm;
//import com.lambda.investing.algorithmic_trading.mean_reversion.DQNRSISideQuoting;
//import com.lambda.investing.algorithmic_trading.mean_reversion.RSISideQuoting;
//import com.lambda.investing.algorithmic_trading.statistical_arbitrage.StatisticalArbitrageAlgorithm;
//import com.lambda.investing.algorithmic_trading.statistical_arbitrage.StatisticalArbitrageQuotingAlgorithm;
import com.lambda.investing.backtest_engine.BacktestConfiguration;
import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter @ToString
/***
 * EXAMPLE
 *{
 * 	"backtest": {
 * 		"startDate": "20201208",
 * 		"endDate": "20201208",
 * 		"instrument": "btcusdt_binance"
 *        },
 * 	"algorithm": {
 * 		"algorithmName": "AvellanedaStoikov",
 * 		"parameters": {
 * 			"risk_aversion": "0.9",
 * 			"position_multiplier": "100",
 * 			"window_tick": "100",
 * 			"minutes_change_k": "10",
 * 			"quantity": "0.0001",
 * 			"k_default": "0.00769",
 * 			"spread_multiplier": "5.0",
 * 			"first_hour": "7",
 * 			"last_hour": "19"
 *        }
 *    }
 *
 * }
 *
 *
 */ public class AlgorithmConfiguration {

	private static int COUNTER_ALGORITHMS = -1;

	private String algorithmName;
	private Map<String, Object> parameters;

	/**
	 * Must return the same as in algorithm_enum.py
	 *
	 * @return
	 */
	public com.lambda.investing.algorithmic_trading.Algorithm getAlgorithm(
			AlgorithmConnectorConfiguration algorithmConnectorConfiguration) {
		Map<String, Object> parametersAsString = AlgorithmUtils.getParameters(parameters);
		return AlgorithmUtils.getAlgorithm(algorithmConnectorConfiguration, algorithmName, parametersAsString);
	}

}


