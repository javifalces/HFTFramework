package com.lambda.investing.algorithmic_trading.factor_investing;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.factor_investing.executors.Executor;
import com.lambda.investing.algorithmic_trading.factor_investing.executors.MarketExecutor;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.trading.Verb;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;

import java.util.Map;

public class MarketFactorInvestingAlgorithm extends AbstractFactorInvestingAlgorithm {
    public MarketFactorInvestingAlgorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo, Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);
    }


    @Override
    protected Executor createExecutor(Instrument instrument) {
        return new MarketExecutor(timeService, this.algorithmInfo, instrument, this.algorithmConnectorConfiguration);
    }

}
