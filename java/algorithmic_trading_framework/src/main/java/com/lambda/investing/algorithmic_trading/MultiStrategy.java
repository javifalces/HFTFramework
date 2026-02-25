package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.algorithmic_trading.hedging.HedgeManager;
import com.lambda.investing.algorithmic_trading.provider.AlgorithmCreationUtils;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MultiStrategy - A strategy that contains a list of SingleInstrumentAlgorithm of the same type
 * with the same parameters but different instruments.
 * <p>
 * Algorithm name format: MultiStrategy_SingleStrategyName_name
 * Example: MultiStrategy_LinearConstantSpread_example
 * <p>
 * The number of SingleInstrumentAlgorithms will match the number of InstrumentPKs provided.
 * Each sub-algorithm will be assigned one instrument.
 */
public class MultiStrategy extends Algorithm {

    protected Logger logger = LogManager.getLogger(MultiStrategy.class);

    protected List<SingleInstrumentAlgorithm> algorithmsList = new ArrayList<>();
    protected Map<String, SingleInstrumentAlgorithm> instrumentToAlgorithm = new HashMap<>();
    protected String singleStrategyType;

    @Setter
    protected String[] instrumentPKs;

    public MultiStrategy(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
                         String algorithmInfo, Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);
        setParameters(parameters);
    }

    public MultiStrategy(String algorithmInfo, Map<String, Object> parameters) {
        super(algorithmInfo, parameters);
        setParameters(parameters);
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        super.setParameters(parameters);

        // Parse the algorithm name to extract the single strategy type
        // Format: MultiStrategy_SingleStrategyName_name
        parseSingleStrategyType();

        // Get the list of instruments
        String instrumentPKsStr = getParameterString(parameters, "instrument");
        if (instrumentPKsStr == null || instrumentPKsStr.isEmpty()) {
            logger.error("MultiStrategy requires 'instrument' parameter with comma-separated instrument list");
            return;
        }

        instrumentPKs = instrumentPKsStr.split(",");
        for (int i = 0; i < instrumentPKs.length; i++) {
            instrumentPKs[i] = instrumentPKs[i].trim();
        }

        // Create one SingleInstrumentAlgorithm per instrument with same parameters
        createSubAlgorithms(parameters);

        logger.info("MultiStrategy created with {} sub-algorithms of type {}",
                algorithmsList.size(), singleStrategyType);
    }

    /**
     * Parse the algorithm name to extract the single strategy type
     * Format: MultiStrategy_SingleStrategyName_name -> SingleStrategyName
     */
    private void parseSingleStrategyType() {
        String algoInfo = this.algorithmInfo;
        if (!algoInfo.startsWith("MultiStrategy_")) {
            logger.error("MultiStrategy algorithm name must start with 'MultiStrategy_'");
            return;
        }

        // Remove "MultiStrategy_" prefix
        String remainder = algoInfo.substring("MultiStrategy_".length());

        // Extract the SingleStrategyName (everything before the last underscore + name)
        int lastUnderscore = remainder.lastIndexOf('_');
        if (lastUnderscore > 0) {
            singleStrategyType = remainder.substring(0, lastUnderscore);
        } else {
            singleStrategyType = remainder;
        }

        logger.info("Parsed single strategy type: {} from algorithmInfo: {}", singleStrategyType, algoInfo);
    }

    /**
     * Create one SingleInstrumentAlgorithm per instrument
     */
    private void createSubAlgorithms(Map<String, Object> parameters) {
        // Create a copy of parameters for each sub-algorithm
        Map<String, Object> subAlgoParams = new HashMap<>(parameters);

        for (int i = 0; i < instrumentPKs.length; i++) {
            String instrumentPK = instrumentPKs[i];

            // Set the instrument for this sub-algorithm
            subAlgoParams.put("instrument", instrumentPK);

            // Create unique algorithm name: SingleStrategyType_instrumentPK_index
            String subAlgoName = singleStrategyType + "_" + instrumentPK + "_" + i;

            // Create the sub-algorithm
            SingleInstrumentAlgorithm subAlgo = (SingleInstrumentAlgorithm)
                    AlgorithmCreationUtils.getInstance().getAlgorithm(subAlgoName, new HashMap<>(subAlgoParams));

            if (subAlgo == null) {
                logger.error("Failed to create sub-algorithm {} for instrument {}", subAlgoName, instrumentPK);
                continue;
            }

            // Set the instrument
            Instrument instrument = Instrument.getInstrument(instrumentPK);
            if (instrument == null) {
                logger.error("Failed to get instrument: {}", instrumentPK);
                continue;
            }

            subAlgo.setInstrument(instrument);

            // Add to our lists
            algorithmsList.add(subAlgo);
            instrumentToAlgorithm.put(instrumentPK, subAlgo);

            // Also add instruments to this algorithm's instrument set
            instruments.add(instrument);

            logger.info("Created sub-algorithm {} for instrument {}", subAlgoName, instrumentPK);
        }
    }

    @Override
    public void setAlgorithmConnectorConfiguration(AlgorithmConnectorConfiguration algorithmConnectorConfiguration) {
        super.setAlgorithmConnectorConfiguration(algorithmConnectorConfiguration);
        for (SingleInstrumentAlgorithm subAlgo : algorithmsList) {
            subAlgo.setAlgorithmConnectorConfiguration(algorithmConnectorConfiguration);
        }
    }

    @Override
    public void init() {
        super.init();
        for (SingleInstrumentAlgorithm subAlgo : algorithmsList) {
            subAlgo.init();
            subAlgo.setExitOnStop(false);
        }
    }

    @Override
    public void start() {
        super.start();
        for (SingleInstrumentAlgorithm subAlgo : algorithmsList) {
            subAlgo.start();
        }
    }

    @Override
    public void stop() {
        for (SingleInstrumentAlgorithm subAlgo : algorithmsList) {
            subAlgo.stop();
        }
        super.stop();
    }

    @Override
    public String printAlgo() {
        StringBuilder info = new StringBuilder();
        info.append(String.format("MultiStrategy[%s](%d instruments): ",
                singleStrategyType, algorithmsList.size()));
        for (int i = 0; i < instrumentPKs.length; i++) {
            if (i > 0) info.append(", ");
            info.append(instrumentPKs[i]);
        }
        return info.toString();
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        // First, let the parent process the depth
        boolean parentResult = super.onDepthUpdate(depth);

        // Forward to the appropriate sub-algorithm based on instrument
        SingleInstrumentAlgorithm subAlgo = instrumentToAlgorithm.get(depth.getInstrument());
        if (subAlgo != null) {
            return subAlgo.onDepthUpdate(depth);
        }

        return parentResult;
    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        boolean parentResult = super.onTradeUpdate(trade);

        SingleInstrumentAlgorithm subAlgo = instrumentToAlgorithm.get(trade.getInstrument());
        if (subAlgo != null) {
            return subAlgo.onTradeUpdate(trade);
        }

        return parentResult;
    }

    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        // Forward to all sub-algorithms - they will filter by their algorithmInfo
        boolean result = false;
        for (SingleInstrumentAlgorithm subAlgo : algorithmsList) {
            if (executionReport.getAlgorithmInfo().equals(subAlgo.getAlgorithmInfo())) {
                result = subAlgo.onExecutionReportUpdate(executionReport) || result;
            }
        }
        return result;
    }

    @Override
    public void onCandleUpdate(Candle candle) {
        SingleInstrumentAlgorithm subAlgo = instrumentToAlgorithm.get(candle.getInstrumentPk());
        if (subAlgo != null) {
            subAlgo.onCandleUpdate(candle);
        }
    }

    @Override
    public boolean onCommandUpdate(Command command) {
        boolean result = super.onCommandUpdate(command);
        for (SingleInstrumentAlgorithm subAlgo : algorithmsList) {
            result = subAlgo.onCommandUpdate(command) || result;
        }
        return result;
    }

    @Override
    public void setHedgeManager(HedgeManager hedgeManager) {
        super.setHedgeManager(hedgeManager);
        for (SingleInstrumentAlgorithm subAlgo : algorithmsList) {
            subAlgo.setHedgeManager(hedgeManager);
        }
    }
}
