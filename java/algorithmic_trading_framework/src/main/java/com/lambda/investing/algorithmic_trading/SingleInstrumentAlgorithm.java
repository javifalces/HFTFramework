package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.gui.main.MainMenuGUI;
import com.lambda.investing.algorithmic_trading.hedging.LinearRegressionHedgeManager;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

@Getter @Setter public abstract class SingleInstrumentAlgorithm extends Algorithm {

	protected Instrument instrument;
	protected boolean enableAutoHedger;

	protected int minLevelsDepth = 0;//<0 means disable check
	protected double minVolumeDepth = -5;//<0 means disable check
	protected double maxVolumeDepth = -5;//<0 means disable check

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
		instruments.add(instrument);

		//configure autohedger
		if (enableAutoHedger) {
			String hedgePath = Configuration.OUTPUT_PATH + File.separator + String
					.format("hedge_%s.json", getInstrument().getPrimaryKey());
			try {
				LinearRegressionHedgeManager hedgeManager = new LinearRegressionHedgeManager(hedgePath);
				setHedgeManager(hedgeManager);
			} catch (FileNotFoundException e) {
				System.err.println(
						"Error creating LinearRegressionHedgeManager looking for " + hedgePath + " disable AH");
				logger.error("Error creating LinearRegressionHedgeManager looking for " + hedgePath + " disable AH");
			}
		}

	}

	/**
	 * @param depth     depth to check
	 * @param minLevels number of min levels of both sides to be valid
	 * @param minVol    if zero , check is disable
	 * @param maxVol    if zero , check is disable
	 * @return if false , we are going to disable the algorithm
	 */
	protected boolean validDepth(Depth depth, int minLevels, double minVol, double maxVol) {
		if (!checkDepth(depth, minLevels, minVol, maxVol)) {
			if (this.getAlgorithmState().equals(AlgorithmState.STARTED) || this.getAlgorithmState()
					.equals(AlgorithmState.STARTING)) {
				this.stop();
			}
			return false;
		} else if (inOperationalTime()) {
			if (this.getAlgorithmState().equals(AlgorithmState.STOPPED) || this.getAlgorithmState()
					.equals(AlgorithmState.STOPPING)) {
				this.start();
			}
			return true;
		} else {
			//			logger.warn("{} valid Depth buy not in operational time! return as true valid",getCurrentTime());
			return true;
		}
	}

	/**
	 * @param depth     depth to check
	 * @param minLevels if lower or equal to zero , check is dissable, number of max levels of both sides to be valid
	 * @param minVol    if lower or equal to zero , check is disable
	 * @param maxVol    if lower or equal to zero , check is disable
	 * @return false if any of the checks is not passing
	 */
	private boolean checkDepth(Depth depth, int minLevels, double minVol, double maxVol) {
		if (minLevels > 0 && depth.getLevels() < minLevels) {
			logger.info("{}<{} level threshold -> depth is wrong", depth.getLevels(), minLevels);
			return false;
		}
		if (minVol > 0 && depth.getTotalVolume() < minVol) {
			logger.info("{}< {} volume threshold -> depth is wrong", depth.getTotalVolume(), minVol);
			return false;
		}
		if (maxVol > 0 && depth.getTotalVolume() > maxVol) {
			logger.info("{}> {} volume threshold -> depth is wrong", depth.getTotalVolume(), maxVol);
			return false;
		}
		return true;
	}

	@Override
	public boolean onDepthUpdate(Depth depth) {
		boolean outputSuper = super.onDepthUpdate(depth);
		if (!validDepth(depth, minLevelsDepth, minVolumeDepth, maxVolumeDepth)) {
			return false;
		}
		return outputSuper;
	}

	@Override
	protected void startUI() {
		super.startUI();
		algorithmicTradingGUI = new MainMenuGUI(theme, this);
		MainMenuGUI.IS_BACKTEST = isBacktest;
		algorithmicTradingGUI.start();
	}


}
