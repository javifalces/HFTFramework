package com.lambda.investing.algo_trading;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;

import com.lambda.investing.algorithmic_trading.AlgorithmCreationUtils;
import com.lambda.investing.algorithmic_trading.AlgorithmUtils;
import lombok.Getter;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;


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
 */

@Getter
@Setter
@NoArgsConstructor
@ToString
public class AlgorithmConfiguration {

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
		return AlgorithmCreationUtils.getAlgorithm(algorithmConnectorConfiguration, algorithmName, parametersAsString);
	}


}


