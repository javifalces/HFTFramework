package com.lambda.investing.backtest;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.MultiAlgorithm;
import com.lambda.investing.algorithmic_trading.provider.AlgorithmCreationUtils;
import com.lambda.investing.algorithmic_trading.utils.AlgorithmUtils;
import com.lambda.investing.algorithmic_trading.SingleInstrumentAlgorithm;
import com.lambda.investing.algorithmic_trading.factor_investing.AbstractFactorInvestingAlgorithm;
import com.lambda.investing.backtest_engine.BacktestConfiguration;
import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static com.lambda.investing.backtest_engine.BacktestConfiguration.dateFormatTime;
import static com.lambda.investing.model.Util.toJsonString;

@Setter
@ToString
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
public class InputConfiguration implements Cloneable {

    protected static Logger logger = LogManager.getLogger(InputConfiguration.class);

    private static int COUNTER_ALGORITHMS = -1;

    public Backtest backtest;

    @Getter
    private Algorithm algorithm;
    @Getter
    private List<Algorithm> algorithms;


    public InputConfiguration() {
    }

    public BacktestConfiguration getBacktestConfiguration() throws Exception {
        com.lambda.investing.algorithmic_trading.Algorithm algorithm = getConfiguredAlgorithm();
        return backtest.getBacktestConfiguration(algorithm);
    }

    private com.lambda.investing.algorithmic_trading.Algorithm getConfiguredAlgorithm() throws Exception {
        if (algorithm != null) {
            return algorithm.getAlgorithm();
        }
        if (algorithms == null || algorithms.isEmpty()) {
            throw new Exception("Algorithm not found");
        }
        List<com.lambda.investing.algorithmic_trading.Algorithm> configuredAlgorithms = new ArrayList<>();
        Set<String> instruments = new LinkedHashSet<>();
        for (Algorithm algorithmConfiguration : algorithms) {
            if (algorithmConfiguration == null) {
                continue;
            }
            com.lambda.investing.algorithmic_trading.Algorithm algorithm = algorithmConfiguration.getAlgorithm();
            configureSingleInstrumentAlgorithm(algorithm);
            configuredAlgorithms.add(algorithm);
            instruments.addAll(getInstrumentsFromParameters(algorithm.getParameters()));
        }
        if (configuredAlgorithms.isEmpty()) {
            throw new Exception("Algorithm not found");
        }
        if ((backtest.instrument == null || backtest.instrument.isEmpty()) && !instruments.isEmpty()) {
            backtest.instrument = String.join(",", instruments);
            logger.info("instrument not set in backtestConfiguration, loaded from algorithms parameters: {}", backtest.instrument);
        }
        if (configuredAlgorithms.size() == 1) {
            return configuredAlgorithms.get(0);
        }
        String compositeName = String.format("MultipleAlgorithms_%d", ++COUNTER_ALGORITHMS);
        return new MultiAlgorithm(compositeName, configuredAlgorithms);
    }

    private void configureSingleInstrumentAlgorithm(com.lambda.investing.algorithmic_trading.Algorithm algorithm) {
        if (!(algorithm instanceof SingleInstrumentAlgorithm)) {
            return;
        }
        Set<String> instruments = getInstrumentsFromParameters(algorithm.getParameters());
        if (instruments.isEmpty()) {
            return;
        }
        String instrumentPk = instruments.iterator().next();
        Instrument instrument = Instrument.getInstrument(instrumentPk);
        if (instrument != null) {
            ((SingleInstrumentAlgorithm) algorithm).setInstrument(instrument);
        }
    }

    private Set<String> getInstrumentsFromParameters(Map<String, Object> parameters) {
        Set<String> instruments = new LinkedHashSet<>();
        if (parameters == null) {
            return instruments;
        }
        Object instrumentPks = parameters.get("instrumentPks");
        if (instrumentPks != null) {
            for (String instrument : String.valueOf(instrumentPks).split(",")) {
                if (!instrument.trim().isEmpty() && !"null".equalsIgnoreCase(instrument.trim())) {
                    instruments.add(instrument.trim());
                }
            }
        }
        Object instrument = parameters.get("instrument");
        if (instrument != null) {
            for (String instrumentPk : String.valueOf(instrument).split(",")) {
                if (!instrumentPk.trim().isEmpty() && !"null".equalsIgnoreCase(instrumentPk.trim())) {
                    instruments.add(instrumentPk.trim());
                }
            }
        }
        return instruments;
    }

//    @Override
//    public String toString() {
//        return toJsonString(this);
//    }

    @Getter
    @Setter
    public class Backtest {
        private String startDate;//20201208
        private String endDate;//20201210
        private long delayOrderMs;//65
        private String latencyEngineType = Configuration.LATENCY_ENGINE_TYPE.toString();//fixed, poisson
        private boolean feesCommissionsIncluded = true;
        private long seed = 0;
        private String instrument;
        private String multithreadConfiguration = null;
        private int initialSleepSeconds = 3;
        private boolean searchMatchMarketTrades = false;//we are already synchronizing PersistorMarketDataConnector InstrumentCache
        private int uiWebPort;

        public Backtest() {
        }

        private List<Instrument> getInstrumentList(com.lambda.investing.algorithmic_trading.Algorithm algorithm) throws Exception {

            if (instrument == null || instrument.isEmpty()) {
                // Try to load instrument(s) from algorithm parameters.
                // Priority: "instrumentPks" (array or comma-separated) then "instrument" (backward compat).
                Map<String, Object> algoParams = algorithm.getParameters();
                String paramInstrument = null;
                if (algoParams != null && algoParams.containsKey("instrumentPks")) {
                    // AlgorithmUtils.getParameters() already converted List -> comma-separated string
                    paramInstrument = String.valueOf(algoParams.get("instrumentPks"));
                } else if (algoParams != null && algoParams.containsKey("instrument")) {
                    paramInstrument = String.valueOf(algoParams.get("instrument"));
                }
                if (paramInstrument != null && !paramInstrument.equalsIgnoreCase("null") && !paramInstrument.isEmpty()) {
                    instrument = paramInstrument;
                    logger.info("instrument not set in backtestConfiguration, loaded from algorithm parameters: {}", instrument);
                }
            }
            if (instrument == null || instrument.isEmpty()) {
                throw new Exception("Instrument not found in backtestConfiguration or algorithm parameters");
            }
            List<Instrument> instrumentList = new ArrayList<>();
            String[] instrumentPKs = instrument.split(",");
            for (String instrumentPk : instrumentPKs) {
                if (instrumentPk.trim().isEmpty()) {
                    continue;
                }
                Instrument instrumentObject = Instrument.getInstrument(instrumentPk.trim());
                if (instrumentObject == null) {
                    throw new Exception("InstrumentPK " + instrumentPk + " not found");
                }
                instrumentList.add(instrumentObject);

                if (algorithm instanceof SingleInstrumentAlgorithm) {
                    ((SingleInstrumentAlgorithm) algorithm).setInstrument(instrumentObject);
                }
            }

            //add the rest of instruments in case needed
            Set<Instrument> algoInstrumentSet = algorithm.getInstruments();

            //factor investing only requires instruments from model! instrument from backtestConfiguration is ignored
            if (algorithm instanceof AbstractFactorInvestingAlgorithm) {
                instrumentList.clear();
            }


            for (Instrument instrument : algoInstrumentSet) {
                if (instrument != null && !instrumentList.contains(instrument)) {
                    instrumentList.add(instrument);
                }
            }

            //ad hedge manager rest of insturments
            if (algorithm.getHedgeManager() != null) {
                //adding
                Set<Instrument> instrumentSet = algorithm.getHedgeManager().getInstrumentsHedgeList();
                for (Instrument hedgeInstrument : instrumentSet) {
                    if (hedgeInstrument != null && !instrumentList.contains(hedgeInstrument)) {
                        instrumentList.add(hedgeInstrument);
                    }
                }
                if (instrumentSet.size() > 0) {
                    logger.info("adding {} HedgeManager instruments to backtestConfiguration -> {}",
                            instrumentSet.size(), instrumentList.size());
                }
            }
            return instrumentList;
        }


        public BacktestConfiguration getBacktestConfiguration(
                com.lambda.investing.algorithmic_trading.Algorithm algorithm) throws Exception {

            if (algorithm == null) {
                throw new Exception("Algorithm not found");
            }
            List<Instrument> instrumentList = getInstrumentList(algorithm);

            algorithm.setPlotStopHistorical(false);
            BacktestConfiguration backtestConfiguration = new BacktestConfiguration();
            backtestConfiguration.setAlgorithm(algorithm);
            dateFormatTime.setTimeZone(TimeZone.getTimeZone("GMT"));

            backtestConfiguration.setStartTime(startDate);
            backtestConfiguration.setEndTime(endDate);
            backtestConfiguration.setDelayOrderMs(delayOrderMs);
            backtestConfiguration.setInitialSleepSeconds(initialSleepSeconds);
            backtestConfiguration.setLatencyEngineType(latencyEngineType);


            backtestConfiguration.setFeesCommissionsIncluded(feesCommissionsIncluded);
            if (seed != 0) {
                backtestConfiguration.setSeed(seed);
            }
            backtestConfiguration.setInstruments(instrumentList);
            if (multithreadConfiguration != null) {
                backtestConfiguration.setMultithreadConfiguration(multithreadConfiguration);
            }

            //Already Synchronizing in PersistorMarketDataConnector InstrumentCache
//            backtestConfiguration.setSearchMatchMarketTrades(searchMatchMarketTrades);
//            if (!searchMatchMarketTrades) {
//                logger.info("searchMatchMarketTrades is disabled");
//                Configuration.BACKTEST_SYNCHRONIZED_TRADES_DEPTH_MAX_MS = 0;
//            }
            backtestConfiguration.setBacktestSource("parquet");
            backtestConfiguration.setSpeed(-1);
            backtestConfiguration.setBacktestExternalConnection("ordinary");

            return backtestConfiguration;
        }

        @Override
        public String toString() {
            return toJsonString(this);
        }
    }

    @Getter
    @Setter
    private class Algorithm {
        private String algorithmName;
        private Map<String, Object> parameters;

        /**
         * Must return the same as in algorithm_enum.py
         *
         * @return
         */
        public com.lambda.investing.algorithmic_trading.Algorithm getAlgorithm() {
            return AlgorithmCreationUtils.getInstance().getAlgorithm(null, algorithmName, AlgorithmUtils.getParameters(parameters));
        }

        public Algorithm() {
        }

//        @Override
//        public String toString() {
//            return toJsonString(this);
//        }
    }

    public Object clone() throws CloneNotSupportedException {
        InputConfiguration output = new InputConfiguration();
        output.setBacktest(this.backtest);
        output.setAlgorithm(this.algorithm);
        output.setAlgorithms(this.algorithms);
        return output;
    }

}
