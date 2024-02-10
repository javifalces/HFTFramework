package com.lambda.investing.algorithmic_trading.market_making.avellaneda_stoikov;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.LogLevels;
import com.lambda.investing.algorithmic_trading.market_making.reinforcement_learning.RLAbstractMarketMaking;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.action.AvellanedaAction;
import org.apache.logging.log4j.LogManager;

import java.util.HashMap;
import java.util.Map;

public class AlphaAvellanedaStoikov extends RLAbstractMarketMaking {

    protected AvellanedaStoikov algorithm;

    public AlphaAvellanedaStoikov(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo,
                                  Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);

        logger = LogManager.getLogger(AlphaAvellanedaStoikov.class);

    }


    @Override
    public String printAlgo() {
        return String
                .format("%s  \n\triskAversion=%.3f\n\tquantity=%.3f\n\tmidpricePeriodWindow=%d\n\tmidpricePeriodSeconds=%d\n\tkDefault=%.3f\n\taDefault=%.3f\n\tfirstHourOperatingIncluded=%d\n\tlastHourOperatingIncluded=%d\n\tchangeKPeriodSeconds=%d\n\tprivate_state  min=%.7f max=%.7f numberDecimals=%d horizon=%d\n\tmarket_state  min=%.7f max=%.7f numberDecimals=%d horizon=%d\n\tcandle_state  min=%.7f max=%.7f numberDecimals=%d horizon=%d\n\tscore_enum:%s",
                        algorithmInfo, algorithm.riskAversion, algorithm.quantity,
                        algorithm.midpricePeriodSeconds, algorithm.midpricePeriodWindow, algorithm.kDefault, algorithm.aDefault, firstHourOperatingIncluded,
                        lastHourOperatingIncluded, algorithm.changeKPeriodSeconds, minPrivateState, maxPrivateState,
                        numberDecimalsPrivateState, horizonTicksPrivateState, minMarketState, maxMarketState,
                        numberDecimalsMarketState, horizonTicksMarketState, minCandleState, maxCandleState,
                        numberDecimalsCandleState, horizonCandlesState, scoreEnum
                );
    }

    @Override
    protected void updateCurrentCustomColumn(String instrumentPk) {
        //add custom columns to trade csv
        addCurrentCustomColumn(instrumentPk, "midpricePeriodWindow", (double) algorithm.midpricePeriodWindow);
        addCurrentCustomColumn(instrumentPk, "changeKPeriodSeconds", (double) algorithm.changeKPeriodSeconds);
        addCurrentCustomColumn(instrumentPk, "riskAversion", algorithm.riskAversion);
        addCurrentCustomColumn(instrumentPk, "skew", algorithm.skew);
        if (algorithm.kDefault != null && algorithm.kDefault != AvellanedaStoikov.NON_SET_PARAMETER) {
            addCurrentCustomColumn(instrumentPk, "kDefault", algorithm.kDefault);
        }
        if (algorithm.aDefault != null && algorithm.aDefault != AvellanedaStoikov.NON_SET_PARAMETER) {
            addCurrentCustomColumn(instrumentPk, "aDefault", algorithm.aDefault);
        }
        addCurrentCustomColumn(instrumentPk, "iterations", (double) iterations.get());
        //		parameters
        try {
            addCurrentCustomColumn(instrumentPk, "bid", (double) this.lastDepth.getBestBid());
            addCurrentCustomColumn(instrumentPk, "ask", (double) this.lastDepth.getBestAsk());
            addCurrentCustomColumn(instrumentPk, "bid_qty", (double) this.lastDepth.getBestBidQty());
            addCurrentCustomColumn(instrumentPk, "ask_qty", (double) this.lastDepth.getBestAskQty());
            addCurrentCustomColumn(instrumentPk, "imbalance", (double) this.lastDepth.getImbalance());
            addCurrentCustomColumn(instrumentPk, "reward", (double) this.rewardStartStep);
        } catch (Exception e) {
        }
    }

    @Override
    public void setAction(double[] actionValues) {
        super.setAction(actionValues);

        actionValues = GetActionValues(actionValues);

        if (!Double.isNaN(actionValues[AvellanedaAction.SKEW_PRICE_INDEX]))
            algorithm.skew = actionValues[AvellanedaAction.SKEW_PRICE_INDEX];
        if (!Double.isNaN(actionValues[AvellanedaAction.WINDOWS_INDEX]))
            algorithm.midpricePeriodWindow = (int) Math.round(actionValues[AvellanedaAction.WINDOWS_INDEX]);
        if (!Double.isNaN(actionValues[AvellanedaAction.RISK_AVERSION_INDEX]))
            algorithm.riskAversion = actionValues[AvellanedaAction.RISK_AVERSION_INDEX];
        if (!Double.isNaN(actionValues[AvellanedaAction.K_DEFAULT_INDEX]))
            algorithm.kDefault = actionValues[AvellanedaAction.K_DEFAULT_INDEX];
        if (!Double.isNaN(actionValues[AvellanedaAction.A_DEFAULT_INDEX]))
            algorithm.aDefault = actionValues[AvellanedaAction.A_DEFAULT_INDEX];

        if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
            //// Here the action is not finished
            logger.info(
                    "[{}][iteration {} start] actionReceived: {} reward:{}  ->riskAversion={}  midpricePeriodWindow={}  skew={} kDefault={} aDefault={}",
                    this.getCurrentTime(), this.iterations.get() - 1, getLastActionString(), getCurrentReward(), algorithm.riskAversion,
                    algorithm.midpricePeriodWindow, algorithm.skew, algorithm.kDefault, algorithm.aDefault);
        }
        notifyParameters();
    }

    @Override
    protected void notifyParameters() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("skew", algorithm.skew);
        parameters.put("midpricePeriodWindow", algorithm.midpricePeriodWindow);
        parameters.put("changeKPeriodSeconds", algorithm.changeKPeriodSeconds);
        parameters.put("riskAversion", algorithm.riskAversion);
        parameters.put("kDefault", algorithm.kDefault);
        parameters.put("aDefault", algorithm.aDefault);

        algorithmNotifier.notifyObserversOnUpdateParams(parameters);
    }

    @Override
    protected boolean onFinishedIteration(long msElapsed, double reward, double[] state) {
        if (super.onFinishedIteration(msElapsed, reward, state)) {
            if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
                logger.info(
                        "[{}][iteration {} end] onFinishedIteration {} ms later reward={} -> riskAversion={} midpricePeriodWindow={}({} s) changeKPeriodSeconds={} skew={} kDefault={} aDefault={}",
                        this.getCurrentTime(), this.iterations.get() - 1, msElapsed, getLastReward(),
                        algorithm.riskAversion, algorithm.midpricePeriodWindow, algorithm.midpricePeriodSeconds, algorithm.changeKPeriodSeconds, algorithm.skew, algorithm.kDefault,
                        algorithm.aDefault);
            }
        }
        return true;
    }


    @Override
    public void setParameters(Map<String, Object> parameters) {

        super.setParameters(parameters);
        //ACTION configuration
        double[] riskAversionAction = getParameterArrayDouble(parameters, "riskAversionAction");
        int[] midpricePeriodWindowAction = getParameterArrayInt(parameters, "midpricePeriodWindowAction");

        double[] skewAction = getParameterArrayDouble(parameters, "skewAction");
        double[] kDefaultActionInput = getParameterArrayDouble(parameters, "kDefaultAction");
        double[] kDefaultAction = new double[]{AvellanedaStoikov.NON_SET_PARAMETER};
        boolean isMinusOneK = kDefaultActionInput != null && kDefaultActionInput.length == 1 && kDefaultActionInput[0] == -1;
        if (isMinusOneK) {
            kDefaultActionInput = kDefaultAction;
        }

        if (kDefaultActionInput != null) {
            kDefaultAction = kDefaultActionInput;
        }


        double[] aDefaultActionInput = getParameterArrayDouble(parameters, "aDefaultAction");
        double[] aDefaultAction = new double[]{AvellanedaStoikov.NON_SET_PARAMETER};
        boolean isMinusOneA = aDefaultActionInput != null && aDefaultActionInput.length == 1 && aDefaultActionInput[0] == -1;
        if (isMinusOneA) {
            aDefaultActionInput = aDefaultAction;
        }

        if (aDefaultActionInput != null) {
            aDefaultAction = aDefaultActionInput;
        }

        int[] changeKPeriodSecondsAction = getParameterArrayInt(parameters, "changeKPeriodSecondsAction");

        this.action = new AvellanedaAction(midpricePeriodWindowAction, riskAversionAction, skewAction, kDefaultAction,
                aDefaultAction, changeKPeriodSecondsAction);

        double maxRiskAversion = Doubles.max(riskAversionAction);
        int maxWindowsTick = Ints.max(midpricePeriodWindowAction);
        int maxchangeKPeriodSecondsAction = Ints.max(changeKPeriodSecondsAction);
        double minK = Doubles.min(kDefaultAction);
        double maxA = Doubles.max(aDefaultAction);

        //creation of the algorithm
        parameters.put("riskAversion", maxRiskAversion);
        parameters.put("midpricePeriodWindow", maxWindowsTick);
        parameters.put("changeKPeriodSeconds", maxchangeKPeriodSecondsAction);
        parameters.put("kDefault", minK);
        parameters.put("aDefault", maxA);


        //first underlying algo
        algorithm = new AvellanedaStoikov(algorithmConnectorConfiguration, algorithmInfo, parameters);
        setMarketMakerAlgorithm(algorithm, parameters);


        logger.info("[{}] initial values   {}\n riskAversion:{} midpricePeriodWindow:{} changeKPeriodSeconds:{} skew:{} kDefault:{} aDefault:{}",
                getCurrentTime(), algorithmInfo, algorithm.riskAversion, algorithm.midpricePeriodWindow, algorithm.changeKPeriodSeconds, algorithm.skew,
                algorithm.kDefault, algorithm.aDefault);

        logger.info("[{}]set parameters  {}", getCurrentTime(), algorithmInfo);
        algorithmNotifier.notifyObserversOnUpdateParams(this.parameters);

    }

}
