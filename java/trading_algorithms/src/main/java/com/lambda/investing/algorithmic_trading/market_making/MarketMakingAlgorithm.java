package com.lambda.investing.algorithmic_trading.market_making;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.SingleInstrumentAlgorithm;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter @Setter public abstract class MarketMakingAlgorithm extends SingleInstrumentAlgorithm {

	public double quantityBuy;
	public double quantitySell;

	public MarketMakingAlgorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo,
                                 Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);
    }

	public MarketMakingAlgorithm(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);
	}

}
