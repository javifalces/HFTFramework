package com.lambda.investing.algorithmic_trading.provider;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.MultiStrategy;
import com.lambda.investing.algorithmic_trading.factor_investing.MarketFactorInvestingAlgorithm;
import com.lambda.investing.algorithmic_trading.factor_investing.SniperFactorInvestingAlgorithm;
import com.lambda.investing.algorithmic_trading.market_making.avellaneda_stoikov.AlphaAvellanedaStoikov;
import com.lambda.investing.algorithmic_trading.market_making.avellaneda_stoikov.AvellanedaStoikov;
import com.lambda.investing.algorithmic_trading.market_making.constant_spread.AlphaConstantSpread;
import com.lambda.investing.algorithmic_trading.market_making.constant_spread.ConstantSpreadAlgorithm;
import com.lambda.investing.algorithmic_trading.market_making.constant_spread.LinearConstantSpreadAlgorithm;
import com.lambda.investing.algorithmic_trading.tester.LookForwardBiasAlgorithm;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@Component
public class TradingAlgorithmsProvider implements AlgorithmProvider {
    private enum AlgorithmType {
        CONSTANT_SPREAD("ConstantSpread"),
        LINEAR_CONSTANT_SPREAD("LinearConstantSpread"),
        ALPHA_CONSTANT_SPREAD("AlphaConstantSpread"),
        AVELLANEDA_STOIKOV("AvellanedaStoikov"),
        ALPHA_AVELLANEDA_STOIKOV("AlphaAvellanedaStoikov"),
        LOOK_FORWARD_BIAS("LookForwardBiasAlgorithm"),
        MARKET_FACTOR_INVESTING("MarketFactorInvestingAlgorithm"),
        SNIPER_FACTOR_INVESTING("SniperFactorInvestingAlgorithm"),
        MULTI_STRATEGY("MultiStrategy");

        private final String prefix;

        AlgorithmType(String prefix) {
            this.prefix = prefix;
        }

        public static AlgorithmType fromString(String algorithmName) {
            for (AlgorithmType type : values()) {
                if (algorithmName.startsWith(type.prefix)) {
                    return type;
                }
            }
            return null;
        }

        public Algorithm createAlgorithm(AlgorithmConnectorConfiguration config, String algorithmName, Map<String, Object> parameters) {
            System.out.println(prefix + " createAlgorithm " + algorithmName);
            switch (this) {
                case CONSTANT_SPREAD:
                    return new ConstantSpreadAlgorithm(config, algorithmName, parameters);
                case LINEAR_CONSTANT_SPREAD:
                    return new LinearConstantSpreadAlgorithm(config, algorithmName, parameters);
                case ALPHA_CONSTANT_SPREAD:
                    return new AlphaConstantSpread(config, algorithmName, parameters);
                case AVELLANEDA_STOIKOV:
                    return new AvellanedaStoikov(config, algorithmName, parameters);
                case ALPHA_AVELLANEDA_STOIKOV:
                    return new AlphaAvellanedaStoikov(config, algorithmName, parameters);
                case LOOK_FORWARD_BIAS:
                    return new LookForwardBiasAlgorithm(config, algorithmName, parameters);
                case MARKET_FACTOR_INVESTING:
                    return new MarketFactorInvestingAlgorithm(config, algorithmName, parameters);
                case SNIPER_FACTOR_INVESTING:
                    return new SniperFactorInvestingAlgorithm(config, algorithmName, parameters);
                case MULTI_STRATEGY:
                    return new MultiStrategy(config, algorithmName, parameters);
                default:
                    throw new IllegalArgumentException("Unexpected value: " + this);
            }
        }
    }

    @PostConstruct
    public void init() {
        AlgorithmCreationUtils.getInstance().addProvider(this);
    }

    @Override
    public Algorithm createAlgorithm(AlgorithmConnectorConfiguration config, String algorithmName, Map<String, Object> parameters) {
        AlgorithmType type = AlgorithmType.fromString(algorithmName);
        if (type != null && supports(algorithmName)) {
            return type.createAlgorithm(config, algorithmName, parameters);
        }
        return null;
    }

    @Override
    public boolean supports(String algorithmName) {
        AlgorithmType type = AlgorithmType.fromString(algorithmName);
        return type != null;
    }
}