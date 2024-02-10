package com.lambda.investing.backtest;

import com.lambda.investing.algorithmic_trading.AlgorithmCreationUtils;
import com.lambda.investing.algorithmic_trading.AlgorithmUtils;
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

import static com.lambda.investing.backtest.App.GSON;
import static com.lambda.investing.backtest_engine.BacktestConfiguration.dateFormatTime;

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

    public InputConfiguration() {
    }

    public BacktestConfiguration getBacktestConfiguration() throws Exception {
        return backtest.getBacktestConfiguration(algorithm.getAlgorithm());
    }

    @Getter
    @Setter
    public class Backtest {
        private String startDate;//20201208
        private String endDate;//20201210
        private long delayOrderMs;//65
        private boolean feesCommissionsIncluded = true;
        private long seed = 0;
        private String instrument;
        private String multithreadConfiguration = null;
        private int initialSleepSeconds = 3;

        public Backtest() {
        }

        private List<Instrument> getInstrumentList(com.lambda.investing.algorithmic_trading.Algorithm algorithm) throws Exception {

            Instrument instrumentObject = Instrument.getInstrument(instrument);
            if (instrumentObject == null) {
                throw new Exception("InstrumentPK " + instrument + " not found");
            }
            if (algorithm instanceof SingleInstrumentAlgorithm) {
                ((SingleInstrumentAlgorithm) algorithm).setInstrument(instrumentObject);
            }

            List<Instrument> instrumentList = new ArrayList<>();
            instrumentList.add(instrumentObject);

            //add the rest of instruments in case needed
            Set<Instrument> algoInstrumentSet = algorithm.getInstruments();

            //factor investing only requires instruments from model! instrument from backtestConfiguration is ignored
            if (algorithm instanceof AbstractFactorInvestingAlgorithm) {
                instrumentList.clear();
            }


            for (Instrument instrument : algoInstrumentSet) {
                if (!instrumentList.contains(instrument)) {
                    instrumentList.add(instrument);
                }
            }

            //ad hedge manager rest of insturments
            if (algorithm.getHedgeManager() != null) {
                //adding
                Set<Instrument> instrumentSet = algorithm.getHedgeManager().getInstrumentsHedgeList();
                Set<Instrument> instrumentSetSource = new HashSet<>(instrumentList);
                instrumentSetSource.addAll(instrumentSet);
                instrumentList = new ArrayList<>(instrumentSetSource);
                if (instrumentSet.size() > 0) {
                    logger.info("adding {} HedgeManager instruments to backtestConfiguration -> {}",
                            instrumentSet.size(), instrumentList.size());
                }
            }
            return instrumentList;
        }


        public BacktestConfiguration getBacktestConfiguration(
                com.lambda.investing.algorithmic_trading.Algorithm algorithm) throws Exception {
            List<Instrument> instrumentList = getInstrumentList(algorithm);

            algorithm.setPlotStopHistorical(false);
            BacktestConfiguration backtestConfiguration = new BacktestConfiguration();
            backtestConfiguration.setAlgorithm(algorithm);
            dateFormatTime.setTimeZone(TimeZone.getTimeZone("GMT"));

            backtestConfiguration.setStartTime(startDate);
            backtestConfiguration.setEndTime(endDate);
            backtestConfiguration.setDelayOrderMs(delayOrderMs);
            backtestConfiguration.setInitialSleepSeconds(initialSleepSeconds);

            backtestConfiguration.setFeesCommissionsIncluded(feesCommissionsIncluded);
            if (seed != 0) {
                backtestConfiguration.setSeed(seed);
            }
            backtestConfiguration.setInstruments(instrumentList);
            if (multithreadConfiguration != null) {
                backtestConfiguration.setMultithreadConfiguration(multithreadConfiguration);
            }
            backtestConfiguration.setBacktestSource("parquet");
            backtestConfiguration.setSpeed(-1);
            backtestConfiguration.setBacktestExternalConnection("ordinary");

            return backtestConfiguration;
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
            return AlgorithmCreationUtils.getAlgorithm(null, algorithmName, AlgorithmUtils.getParameters(parameters));
        }

        public Algorithm() {
        }
    }

    public Object clone() throws CloneNotSupportedException {
        InputConfiguration output = new InputConfiguration();
        output.setBacktest(this.backtest);
        output.setAlgorithm(this.algorithm);
        return output;
    }

    @Override
    public String toString() {
        return GSON.toJson(this);
    }
}
