package com.lambda.investing.algorithmic_trading;


import com.lambda.investing.algorithmic_trading.market_making.avellaneda_stoikov.AlphaAvellanedaStoikov;
import com.lambda.investing.algorithmic_trading.market_making.avellaneda_stoikov.AvellanedaStoikov;
import com.lambda.investing.algorithmic_trading.market_making.constant_spread.AlphaConstantSpread;
import com.lambda.investing.algorithmic_trading.market_making.constant_spread.ConstantSpreadAlgorithm;
import com.lambda.investing.algorithmic_trading.market_making.constant_spread.LinearConstantSpreadAlgorithm;


import java.util.Map;

public class AlgorithmCreationUtils {

    /**
     * get algorithm by name and parameters only for Backtesting
     *
     * @param algorithmName
     * @param parameters
     * @return
     */
    public static Algorithm getAlgorithm(String algorithmName, Map<String, Object> parameters) {
        //for backtest only!!
        return getAlgorithm(null, algorithmName, parameters);
    }

    /**
     * Must return the same as in algorithm_enum.py
     * get algorithm by name and parameters
     *
     * @return
     */
    public static Algorithm getAlgorithm(
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
        if (algorithmName.startsWith("AlphaConstantSpread")) {
            System.out.println("ConstantDQNSpread backtest " + algorithmName);
            return new AlphaConstantSpread(algorithmConnectorConfiguration, algorithmName, parameters);
        }

        if (algorithmName.startsWith("AvellanedaStoikov")) {
            System.out.println("AvellanedaStoikov backtest " + algorithmName);
            return new AvellanedaStoikov(algorithmConnectorConfiguration, algorithmName, parameters);
        }
        if (algorithmName.startsWith("AlphaAvellanedaStoikov")) {
            System.out.println("AlphaAvellanedaStoikov backtest " + algorithmName);
            return new AlphaAvellanedaStoikov(algorithmConnectorConfiguration, algorithmName, parameters);
        }



        System.err.println("AlgorithmUtils :  algorithm " + algorithmName + " not found!");
        return null;
    }


}
