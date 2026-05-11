package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.algorithmic_trading.gui.main.MainMenuGUI;
import com.lambda.investing.algorithmic_trading.hedging.LinearRegressionHedgeManager;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import lombok.Setter;

import java.io.FileNotFoundException;
import java.util.Map;

@Getter
@Setter
public abstract class SingleInstrumentAlgorithm extends Algorithm {

    protected Instrument instrument;
    protected boolean enableAutoHedger;
    protected String syntheticInstrumentFile;
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
            String syntheticInstrumentPath = getSyntheticInstrumentPath();
            if (syntheticInstrumentPath != null && !syntheticInstrumentPath.isEmpty()) {
                try {
                    LinearRegressionHedgeManager hedgeManager = new LinearRegressionHedgeManager(this.instrument, syntheticInstrumentFile);
                    setHedgeManager(hedgeManager);
                    instruments.addAll(hedgeManager.getInstrumentsHedgeList());
                } catch (FileNotFoundException e) {
                    System.err.println(
                            "Error creating LinearRegressionHedgeManager looking for " + syntheticInstrumentFile + " disable AH");
                    logger.error("Error creating LinearRegressionHedgeManager looking for " + syntheticInstrumentFile + " disable AH");
                }
            }
        }

    }


    @Override
    public void setParameters(Map<String, Object> parameters) {
        super.setParameters(parameters);
        syntheticInstrumentFile = getSyntheticInstrumentPath();
        enableAutoHedger = syntheticInstrumentFile != null;
        setInstrument();
    }

    private void setInstrument() {
        if (instrumentPks != null && instrumentPks.length == 1) {
            Instrument instrument1 = Instrument.getInstrument(instrumentPks[0]);
            if (instrument1 == null) {
                logger.warn("Instrument {} not found in universe, can't set it for SingleInstrumentAlgorithm", instrumentPks[0]);
                throw new IllegalArgumentException("Instrument " + instrumentPks[0] + " not found in universe, can't set it for SingleInstrumentAlgorithm");
            }
            setInstrument(instrument1);
        } else {
            logger.warn("SingleInstrumentAlgorithm received more than 1 instrumentPks or null, disable auto set instrument! instrumentPks: {}", instrumentPks);
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

    protected Verb inferVerb(Trade trade) {
        return inferVerb(trade, instrument);
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
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        boolean output = super.onExecutionReportUpdate(executionReport);
        if (enableAutoHedger && hedgeManager != null) {
            if (instrument.getPrimaryKey().equalsIgnoreCase(executionReport.getInstrument())) {
                boolean isTrade = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletelyFilled)
                        || executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled);

                if (isTrade) {
                    hedgeManager.hedge(this, instrument, executionReport.getLastQuantity(), executionReport.getVerb());
                }


            }
        }
        return output;
    }

    @Override
    protected void startUI() {
        super.startUI();
        algorithmicTradingGUI = new MainMenuGUI(theme, isPaper, this);
        MainMenuGUI.IS_BACKTEST = isBacktest;
        algorithmicTradingGUI.start();
    }


}
