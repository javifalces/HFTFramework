package com.lambda.investing.algorithmic_trading.factor_investing;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.factor_investing.executors.Executor;
import com.lambda.investing.algorithmic_trading.factor_investing.executors.SniperExecutor;
import com.lambda.investing.model.asset.Instrument;

import java.util.Map;

public class SniperFactorInvestingAlgorithm extends AbstractFactorInvestingAlgorithm {
    protected long timeStepMs;
    protected int steps;

    public SniperFactorInvestingAlgorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo, Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);
        this.timeStepMs = getParameterInt(parameters, "timeStepMs");
        this.steps = getParameterInt(parameters, "steps");
    }


    @Override
    protected double getPriceIncreasePosition(String instrumentPk) {
        //get last price -> close price
        return getPrice(instrumentPk);
    }

    @Override
    protected Executor createExecutor(Instrument instrument) {
        return new SniperExecutor(timeService, this.algorithmInfo, instrument, this.algorithmConnectorConfiguration, this.timeStepMs, this.steps);
    }

}
