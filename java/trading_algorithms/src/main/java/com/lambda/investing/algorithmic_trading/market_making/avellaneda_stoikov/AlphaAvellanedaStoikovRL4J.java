package com.lambda.investing.algorithmic_trading.market_making.avellaneda_stoikov;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.LogLevels;
import com.lambda.investing.algorithmic_trading.market_making.reinforcement_learning.DQNAbstractMarketMakingMDP;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.action.AvellanedaAction;
import org.apache.logging.log4j.LogManager;
import org.deeplearning4j.rl4j.learning.configuration.LearningConfiguration;
import org.deeplearning4j.rl4j.learning.configuration.QLearningConfiguration;
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning;
import org.deeplearning4j.rl4j.network.configuration.DQNDenseNetworkConfiguration;
import org.deeplearning4j.rl4j.network.configuration.NetworkConfiguration;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.learning.config.Nesterovs;
import weka.dl4j.updater.Updater;

import java.util.Map;

public class AlphaAvellanedaStoikovRL4J extends DQNAbstractMarketMakingMDP {
    protected AvellanedaStoikov algorithm;

    public AlphaAvellanedaStoikovRL4J(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo,
                                      Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);

        logger = LogManager.getLogger(AlphaAvellanedaStoikovRL4J.class);

    }


    @Override
    public String printAlgo() {
        return String
                .format("%s  \n\treinforcementLearningType=%s\n\triskAversion=%.3f\n\tquantity=%.3f\n\twindowTick=%d\n\tkDefault=%.3f\n\taDefault=%.3f\n\tfirstHourOperatingIncluded=%d\n\tlastHourOperatingIncluded=%d\n\tminutesChangeK=%d\n\tprivate_state  min=%.7f max=%.7f numberDecimals=%d horizon=%d\n\tmarket_state  min=%.7f max=%.7f numberDecimals=%d horizon=%d\n\tcandle_state  min=%.7f max=%.7f numberDecimals=%d horizon=%d\n\tscore_enum:%s\n\tmaxBatchSize:%d\n\titeration_train_predict:%d\n\titeration_train_target:%d",
                        algorithmInfo, reinforcementLearningType, algorithm.riskAversion, algorithm.quantity,
                        algorithm.windowTick, algorithm.kDefault, algorithm.aDefault, firstHourOperatingIncluded,
                        lastHourOperatingIncluded, algorithm.minutesChangeK, minPrivateState, maxPrivateState,
                        numberDecimalsPrivateState, horizonTicksPrivateState, minMarketState, maxMarketState,
                        numberDecimalsMarketState, horizonTicksMarketState, minCandleState, maxCandleState,
                        numberDecimalsCandleState, horizonCandlesState, scoreEnum, maxBatchSize,
                        trainingPredictIterationPeriod, trainingTargetIterationPeriod);
    }

    @Override
    protected void updateCurrentCustomColumn(String instrumentPk) {
        //add custom columns to trade csv
        addCurrentCustomColumn(instrumentPk, "windowTick", (double) algorithm.windowTick);
        addCurrentCustomColumn(instrumentPk, "riskAversion", algorithm.riskAversion);
        addCurrentCustomColumn(instrumentPk, "skewPricePct", algorithm.skewPricePct);
        if (algorithm.kDefault != null && algorithm.kDefault != AvellanedaStoikov.NON_SET_PARAMETER) {
            addCurrentCustomColumn(instrumentPk, "kDefault", algorithm.kDefault);
        }
        if (algorithm.aDefault != null && algorithm.aDefault != AvellanedaStoikov.NON_SET_PARAMETER) {
            addCurrentCustomColumn(instrumentPk, "aDefault", algorithm.aDefault);
        }
        addCurrentCustomColumn(instrumentPk, "iterations", (double) iterations);
        //		parameters
        try {
            addCurrentCustomColumn(instrumentPk, "bid", (double) this.lastDepth.getBestBid());
            addCurrentCustomColumn(instrumentPk, "ask", (double) this.lastDepth.getBestAsk());
            addCurrentCustomColumn(instrumentPk, "bid_qty", (double) this.lastDepth.getBestBidQty());
            addCurrentCustomColumn(instrumentPk, "ask_qty", (double) this.lastDepth.getBestAskQty());
            addCurrentCustomColumn(instrumentPk, "imbalance", (double) this.lastDepth.getImbalance());
            addCurrentCustomColumn(instrumentPk, "reward", (double) lastRewardReceived);
        } catch (Exception e) {
        }
    }

    @Override
    protected void setAction(double[] actionValues, int actionNumber) {
        algorithm.skewPricePct = actionValues[AvellanedaAction.SKEW_PRICE_INDEX];
        algorithm.windowTick = (int) Math.round(actionValues[AvellanedaAction.WINDOWS_INDEX]);
        algorithm.riskAversion = actionValues[AvellanedaAction.RISK_AVERSION_INDEX];
        algorithm.kDefault = actionValues[AvellanedaAction.K_DEFAULT_INDEX];
        algorithm.aDefault = actionValues[AvellanedaAction.A_DEFAULT_INDEX];
        if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
            logger.info(
                    "[{}][iteration {}]set action {}  reward:{}  ->riskAversion={}  windowTick={}  skewPricePct={} kDefault={} aDefault={}",
                    this.getCurrentTime(), this.iterations, actionNumber, lastRewardReceived, algorithm.riskAversion,
                    algorithm.windowTick, algorithm.skewPricePct, algorithm.kDefault, algorithm.aDefault);
        }

    }

    @Override
    protected void onFinishedIteration(long msElapsed, double deltaRewardNormalized, double reward) {

        if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
            int action = beforeActionSnapshot != null ? beforeActionSnapshot.getAction() : 0;
            logger.info(
                    "[{}][iteration {}]action {} finished  {} ms later   reward:{}->riskAversion={}  windowTick={}  skewPricePct={} kDefault={} aDefault{}-> currentReward={}   previouslyReward={}  rewardDeltaNormalized={}",
                    this.getCurrentTime(), this.iterations - 1, action, msElapsed, getCurrentReward(),
                    algorithm.riskAversion, algorithm.windowTick, algorithm.skewPricePct, algorithm.kDefault,
                    algorithm.aDefault, reward, lastRewardReceived, deltaRewardNormalized);
        }

    }


    @Override
    public void setParameters(Map<String, Object> parameters) {

        super.setParameters(parameters);
        //ACTION configuration
        double[] riskAversionAction = getParameterArrayDouble(parameters, "riskAversionAction");
        int[] windowsTickAction = getParameterArrayInt(parameters, "windowsTickAction");
        double[] skewPricePctAction = getParameterArrayDouble(parameters, "skewPricePctAction");
        double[] kDefaultActionInput = getParameterArrayDouble(parameters, "kDefaultAction");
        double[] kDefaultAction = new double[]{AvellanedaStoikov.NON_SET_PARAMETER};
        if (kDefaultActionInput != null) {
            kDefaultAction = kDefaultActionInput;
        }

        double[] aDefaultActionInput = getParameterArrayDouble(parameters, "aDefaultAction");
        double[] aDefaultAction = new double[]{AvellanedaStoikov.NON_SET_PARAMETER};
        if (aDefaultActionInput != null) {
            aDefaultAction = aDefaultActionInput;
        }

        this.action = new AvellanedaAction(windowsTickAction, riskAversionAction, skewPricePctAction, kDefaultAction,
                aDefaultAction);

        double maxRiskAversion = Doubles.max(riskAversionAction);
        int maxWindowsTick = Ints.max(windowsTickAction);
        double minK = Doubles.min(kDefaultAction);
        double maxA = Doubles.max(aDefaultAction);

        //creation of the algorithm
        parameters.put("riskAversion", maxRiskAversion);
        parameters.put("windowTick", maxWindowsTick);
        parameters.put("kDefault", minK);
        parameters.put("aDefault", maxA);


        //first underlying algo
        algorithm = new AvellanedaStoikov(algorithmConnectorConfiguration, algorithmInfo, parameters);
        setMarketMakerAlgorithm(algorithm, parameters);


        logger.info("[{}] initial values   {}\n riskAversion:{} windowTick:{} skewPricePct:{} kDefault:{} aDefault:{}",
                getCurrentTime(), algorithmInfo, algorithm.riskAversion, algorithm.windowTick, algorithm.skewPricePct,
                algorithm.kDefault, algorithm.aDefault);

        logger.info("[{}]set parameters  {}", getCurrentTime(), algorithmInfo);


        algorithmNotifier.notifyObserversOnUpdateParams(this.parameters);

    }


    //Rl4j methods

    public NetworkConfiguration getNetworkConfiguration() {
        int hiddenNodes = (int) Math.round((states + actions) * hiddenSizeNodesMultiplier);
        IUpdater updater = new Adam(learningRateNN);
        if (momentumNesterov != 0) {
            updater = new Nesterovs(learningRateNN, momentumNesterov);
        }
        return DQNDenseNetworkConfiguration.builder()
                .numHiddenNodes(hiddenNodes)
                .numLayers(numLayers)
                .l2(l2)
                .updater(updater).build();

    }

    public LearningConfiguration getLearningConfiguration() {
        QLearningConfiguration qLearningConfiguration = new QLearningConfiguration();
        qLearningConfiguration.setSeed(seed);
        qLearningConfiguration.setBatchSize(batchSize);
        qLearningConfiguration.setExpRepMaxSize(maxBatchSize);
        qLearningConfiguration.setGamma(discountFactor);// Gamma parameter used for discounted rewards
        qLearningConfiguration.setErrorClamp(1.0); //Prevent the new Q-Value from being farther than errorClamp away from the previous value. Double.NaN will result in no clamping
        qLearningConfiguration.setRewardFactor(learningRate);// reward scaling
        qLearningConfiguration.setDoubleDQN(true);
        qLearningConfiguration.setTargetDqnUpdateFreq(targetTrainSteps);// target update (hard)
        qLearningConfiguration.setMaxEpochStep(Integer.MAX_VALUE);/// we want to wait to finish backtest ....so maximun
        qLearningConfiguration.setMaxStep(this.maxSteps);
        qLearningConfiguration.setMinEpsilon(this.epsilon);
        qLearningConfiguration.setEpsilonNbStep(this.maxSteps);//The number of steps to anneal epsilon to its minimum value.
        qLearningConfiguration.setUpdateStart(10);// The number of steps to initially wait for until samplling batches from experience replay buffer
        return qLearningConfiguration;


    }


}
