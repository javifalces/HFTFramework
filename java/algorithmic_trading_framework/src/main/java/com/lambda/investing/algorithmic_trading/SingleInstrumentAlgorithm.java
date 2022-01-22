package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.Configuration;
//import com.lambda.investing.algorithmic_trading.hedging.LinearRegressionHedgeManager;
import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

@Getter @Setter public abstract class SingleInstrumentAlgorithm extends Algorithm {

	protected Instrument instrument;
	protected boolean enableAutoHedger;

	public SingleInstrumentAlgorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
			String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters);
	}

	public SingleInstrumentAlgorithm(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);
	}

	public InstrumentManager getInstrumentManager() {
		return getInstrumentManager(instrument.getPrimaryKey());
	}

	public void setInstrument(Instrument instrument) {
		this.instrument = instrument;

		//configure autohedger
		if (enableAutoHedger) {
			String hedgePath = Configuration.OUTPUT_PATH + File.separator + String
					.format("hedge_%s.json", getInstrument().getPrimaryKey());
			//			try {
			//				LinearRegressionHedgeManager hedgeManager = new LinearRegressionHedgeManager(hedgePath);
			//				setHedgeManager(hedgeManager);
			//			} catch (FileNotFoundException e) {
			//				System.err.println(
			//						"Error creating LinearRegressionHedgeManager looking for " + hedgePath + " disable AH");
			//				logger.error("Error creating LinearRegressionHedgeManager looking for " + hedgePath + " disable AH");
			//			}
		}

	}
}
