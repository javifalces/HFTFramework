package com.lambda.investing.algorithmic_trading.market_making.reinforcement_learning;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.*;
import com.lambda.investing.algorithmic_trading.market_making.MarketMakingAlgorithm;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.*;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.action.AbstractAction;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.environment.EnvironmentDiscreteActionSpace;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.*;
import com.lambda.investing.market_data_connector.parquet_file_reader.ParquetMarketDataConnectorPublisher;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.learning.configuration.LearningConfiguration;
import org.deeplearning4j.rl4j.network.configuration.NetworkConfiguration;
import org.deeplearning4j.rl4j.space.ArrayObservationSpace;
import org.deeplearning4j.rl4j.space.Box;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.space.ObservationSpace;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public abstract class DQNAbstractMarketMakingMDP extends SingleInstrumentAlgorithm implements RL4JAlgoritm {
    public static boolean DELTA_REWARD = true;
    public static boolean DEFAULT_STOP_ACTION_FILLED = false;
    protected static int DEFAULT_MAX_BATCH_SIZE = (int) 1E3;
    protected static int DEFAULT_EPOCH = (int) 50;
    protected static int DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD = -1;
    protected static int DEFAULT_TRAINING_TARGET_ITERATION_PERIOD = DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD * 5;

    public boolean isDone() {
        return isDone;
    }

    protected static String BASE_MEMORY_PATH = Configuration.OUTPUT_PATH + File.separator;
    protected static int DEFAULT_LAST_Q = -1;//action 0

    ///dqn fields
    //state
    protected AbstractState state;
    protected StateManager stateManager;

    protected boolean isDone = false;
    //reward
    protected ScoreEnum scoreEnum;


    //agent
    protected double epsilon;
    protected int maxSteps;
    protected int targetTrainSteps;
    protected int trainingStats = 0;//UI training enable

    protected int trainingPredictIterationPeriod = DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD;
    protected int trainingTargetIterationPeriod = DEFAULT_TRAINING_TARGET_ITERATION_PERIOD;

    protected int numberDecimalsMarketState, numberDecimalsCandleState, numberDecimalsPrivateState;
    protected double minMarketState, maxMarketState, minCandleState, maxCandleState, minPrivateState, maxPrivateState;
    protected int horizonTicksMarketState, horizonCandlesState, horizonTicksPrivateState;
    protected long horizonMinMsTick;

    protected String[] stateColumnsFilter = null;
    protected double l1, l2 = 0.;

    protected Date lastDateTrainPredict = null;
    protected Date lastDateTrainTarget = null;

    protected BeforeActionSnapshot beforeActionSnapshot = null;

    protected double discountFactor, learningRate, learningRateNN;
    protected Depth lastDepth = null;
    protected int iterations = 0;
    protected int trainingIterations = 0;
    protected int iterationsStarting = 0;

    protected int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;
    protected int batchSize, epoch, numLayers, maxTrainingIterations;
    protected boolean forceTraining = false;
    protected double momentumNesterov;


    protected AbstractAction action;
    protected List<Integer> actionHistoricalList = new ArrayList<>();


    protected MarketMakingAlgorithm marketMakingAlgorithm;
    protected ReinforcementLearningType reinforcementLearningType;


    protected int timeHorizonSeconds;

    public void setDone(boolean done) {
        isDone = done;
    }


    protected double hiddenSizeNodesMultiplier = 2.0;
    //Parametertuning values step 1

    protected boolean stopActionOnFilled = DEFAULT_STOP_ACTION_FILLED;


    //// RL4j variables
    protected boolean environmentStarted = false;
    protected EnvironmentDiscreteActionSpace environment;


    protected int states;
    protected int actions;

    protected volatile CountDownLatch latch = null;
    protected final Object lockLatch = new Object();
    protected double lastRewardReceived;
    protected boolean waitingReset = false;

    //// RL4j variables


    public DQNAbstractMarketMakingMDP(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
                                      String algorithmInfo, Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);
        logger = LogManager.getLogger(DQNAbstractMarketMakingMDP.class);
        this.setExitOnStop(false);

        constructorAbstract(parameters);
        createModels(parameters);

    }


    //abstract methods
    protected abstract void updateCurrentCustomColumn(String instrumentPk);

    protected abstract void setAction(double[] actionValues, int actionNumber);

    protected abstract void onFinishedIteration(long msElapsed, double deltaRewardNormalized, double reward);


    public void setMarketMakerAlgorithm(MarketMakingAlgorithm marketMakingAlgorithm, Map<String, Object> parameters) {
        this.marketMakingAlgorithm = marketMakingAlgorithm;
        setAlgorithm(this.marketMakingAlgorithm);
        this.marketMakingAlgorithm.constructorForAbstract(algorithmConnectorConfiguration, algorithmInfo, parameters);
        this.marketMakingAlgorithm.setParameters(parameters);
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;

        reinforcementLearningType = ReinforcementLearningType.valueOf(
                getParameterStringOrDefault(parameters, "reinforcementLearningType",
                        ReinforcementLearningType.double_deep_q_learn.toString()));
        this.stopActionOnFilled = getParameterIntOrDefault(parameters, "stopActionOnFilled", 0) == 1;
        this.timeHorizonSeconds = getParameterInt(parameters, "timeHorizonSeconds");
        this.epsilon = getParameterDouble(parameters, "epsilon");

        // NN parameters
        this.scoreEnum = ScoreEnum.valueOf(getParameterString(parameters, "scoreEnum"));
        this.trainingStats = getParameterIntOrDefault(parameters, "trainingStats", "trainingStatsUI", 0);

        //deep RL
        this.trainingPredictIterationPeriod = getParameterIntOrDefault(parameters, "trainingPredictIterationPeriod",
                DEFAULT_TRAINING_PREDICT_ITERATION_PERIOD);
        this.trainingTargetIterationPeriod = getParameterIntOrDefault(parameters, "trainingTargetIterationPeriod",
                DEFAULT_TRAINING_TARGET_ITERATION_PERIOD);

        ///STATE configuration discretize
        this.horizonMinMsTick = getParameterInt(parameters, "horizonMinMsTick");

        this.minPrivateState = getParameterDouble(parameters, "minPrivateState");
        this.maxPrivateState = getParameterDouble(parameters, "maxPrivateState");
        this.horizonTicksPrivateState = getParameterInt(parameters, "horizonTicksPrivateState");
        this.numberDecimalsPrivateState = getParameterInt(parameters, "numberDecimalsPrivateState");

        this.minMarketState = getParameterDoubleOrDefault(parameters, "minMarketState", -1);
        this.maxMarketState = getParameterDoubleOrDefault(parameters, "maxMarketState", -1);
        this.horizonTicksMarketState = getParameterIntOrDefault(parameters, "horizonTicksMarketState",
                this.horizonTicksPrivateState);
        this.numberDecimalsMarketState = getParameterIntOrDefault(parameters, "numberDecimalsMarketState", 0);

        this.minCandleState = getParameterDoubleOrDefault(parameters, "minCandleState", -1);
        this.maxCandleState = getParameterDoubleOrDefault(parameters, "maxCandleState", -1);
        this.horizonCandlesState = getParameterIntOrDefault(parameters, "horizonCandlesState", 1);
        this.numberDecimalsCandleState = getParameterIntOrDefault(parameters, "numberDecimalsCandleState", 0);


        this.firstHourOperatingIncluded = getParameterIntOrDefault(parameters, "firstHour", "first_hour",
                FIRST_HOUR_DEFAULT);//UTC time 6 -9
        this.lastHourOperatingIncluded = getParameterIntOrDefault(parameters, "lastHour", "last_hour",
                LAST_HOUR_DEFAULT);//UTC  18-21

        if (this.firstHourOperatingIncluded != FIRST_HOUR_DEFAULT
                || this.lastHourOperatingIncluded != LAST_HOUR_DEFAULT) {
            logger.info("Current time {} in utc is {}", new Date(), Instant.now());
            logger.info("start UTC hour at {} and finished on {} ,both included", this.firstHourOperatingIncluded,
                    this.lastHourOperatingIncluded);

        }

        this.hiddenSizeNodesMultiplier = getParameterDoubleOrDefault(parameters, "hiddenSizeNodesMultiplier", 2.0);


    }


    @Override
    public void setInstrument(Instrument instrument) {
        marketMakingAlgorithm.setInstrument(instrument);
        super.setInstrument(instrument);

        setState();

        if (this.state instanceof DiscreteTAState) {
            ((DiscreteTAState) this.state).setInstrument(instrument);
        }
        this.state.setNumberOfDecimals(instrument.getNumberDecimalsPrice());
    }

    public void setAlgorithm(MarketMakingAlgorithm marketMakingAlgorithm) {
        this.marketMakingAlgorithm = marketMakingAlgorithm;
        this.marketMakingAlgorithm.setExitOnStop(false);
    }

    public int getMaxTrainingIterations() {
        return maxTrainingIterations;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public boolean isForceTraining() {
        return forceTraining;
    }


    protected void setState() {
        double quantity = (marketMakingAlgorithm.quantityBuy + marketMakingAlgorithm.quantitySell) / 2;

        String[] multiMarketOtherInstruments = getParameterArrayString(parameters, "otherInstrumentsStates");
        int[] otherInstrumentsMsPeriods = getParameterArrayInt(parameters, "otherInstrumentsMsPeriods");
        if (otherInstrumentsMsPeriods == null) {
            otherInstrumentsMsPeriods = new int[]{1000, 5000, 30000};
        }

        if (multiMarketOtherInstruments == null || multiMarketOtherInstruments.length == 0) {
            this.state = new MarketState(this.instrument, this.scoreEnum, this.horizonTicksPrivateState,
                    this.horizonTicksMarketState, this.horizonCandlesState, this.horizonMinMsTick,
                    this.horizonMinMsTick, this.numberDecimalsPrivateState, this.numberDecimalsMarketState,
                    this.numberDecimalsCandleState, this.minPrivateState, this.maxPrivateState, this.minMarketState,
                    this.maxMarketState, this.minCandleState, this.maxCandleState, quantity, CandleType.time_1_min, lastHourOperatingIncluded);
        } else {
            double minMultiMarketState = getParameterDoubleOrDefault(parameters, "minMultiMarketState", -1);
            double maxMultiMarketState = getParameterDoubleOrDefault(parameters, "maxMultiMarketState", -1);
            int numberDecimalsMultiMarketState = getParameterIntOrDefault(parameters, "numberDecimalsMultiMarketState",
                    -1);

            List<String> otherInstrumentsStr = com.lambda.investing.algorithmic_trading.ArrayUtils
                    .StringArrayList(multiMarketOtherInstruments);
            List<Instrument> otherInstruments = new ArrayList<>();

            if (this.getInstrument() != null) {
                instruments.add(this.getInstrument());
            }

            for (String instrument : otherInstrumentsStr) {
                Instrument instrument1 = Instrument.getInstrument(instrument);
                otherInstruments.add(Instrument.getInstrument(instrument));
                instruments.add(instrument1);
            }

            this.state = new MultiMarketState(this.instrument, otherInstruments, this.scoreEnum,
                    this.horizonTicksPrivateState, this.horizonTicksMarketState, this.horizonCandlesState,
                    this.horizonMinMsTick, this.horizonMinMsTick, this.numberDecimalsPrivateState,
                    this.numberDecimalsMarketState, this.numberDecimalsCandleState, this.minPrivateState,
                    this.maxPrivateState, this.minMarketState, this.maxMarketState, this.minCandleState,
                    this.maxCandleState, quantity, CandleType.time_1_min, numberDecimalsMultiMarketState,
                    minMultiMarketState, maxMultiMarketState, otherInstrumentsMsPeriods, lastHourOperatingIncluded);

        }

        this.stateColumnsFilter = getParameterArrayString(parameters, "stateColumnsFilter");
        String[] stateColumnsFilterOut = getFilteredStates(this.stateColumnsFilter);
        if (stateColumnsFilterOut != null) {
            this.stateColumnsFilter = stateColumnsFilterOut;
            this.state.setColumnsFilter(stateColumnsFilter);
            logger.info("filtering state columns {}: {}", this.state.getColumns(), Arrays.toString(stateColumnsFilter));
        } else {
            logger.info("no filtering state columns {}", this.state.getColumns());
        }


        states = this.state.getNumberOfColumns();
        actions = this.action.getNumberActions();
        createEnvironment(states, actions);

        System.out.println(Configuration
                .formatLog("Starting {} with {} state columns and {} action reward columns", algorithmInfo, states,
                        actions));
        logger.info(Configuration
                .formatLog("Starting {} with {} state columns and {} action reward columns", algorithmInfo, states,
                        actions));
    }

    private String[] getFilteredStates(String[] stateColumnsFilter) {
        //add private and individuals
        if (stateColumnsFilter != null && stateColumnsFilter.length > 0) {
            Set<String> privateStatesList = new HashSet<>(((MarketState) this.state).getPrivateColumns());
            Set<String> individualsStateList = new HashSet<>(((MarketState) this.state).getIndividualColumns());
            Set<String> columnsFilter = new HashSet<>(Arrays.asList(stateColumnsFilter));
            columnsFilter.addAll(privateStatesList);
            columnsFilter.addAll(individualsStateList);

            String[] stateColumnsFilterOut = new String[columnsFilter.size()];
            stateColumnsFilterOut = columnsFilter.toArray(stateColumnsFilterOut);
            return stateColumnsFilterOut;
        }
        return null;
    }

    private void createEnvironment(int states, int actions) {
        //creation env models
        DiscreteSpace actionSpace = new DiscreteSpace(actions);
        ObservationSpace<Box> observationSpace = new ArrayObservationSpace(new int[]{states});


        environment = new EnvironmentDiscreteActionSpace(this, actionSpace, observationSpace);

    }


    private void createModels(Map<String, Object> parameters) {
        //done in constructor get the needed parameters
        epoch = getParameterIntOrDefault(parameters, "epoch", DEFAULT_EPOCH);
        maxBatchSize = getParameterIntOrDefault(parameters, "maxBatchSize", DEFAULT_MAX_BATCH_SIZE);
        l1 = getParameterDoubleOrDefault(parameters, "l1", 0.);
        l2 = getParameterDoubleOrDefault(parameters, "l2", 0.);
        //		double learningRate = getParameterDoubleOrDefault(parameters, "learningRate", 0.);
        learningRateNN = getParameterDoubleOrDefault(parameters, "learningRateNN", 0.1);
        this.discountFactor = getParameterDoubleOrDefault(parameters, "discountFactor", 0.95);
        learningRate = getParameterDoubleOrDefault(parameters, "learningRate", 0.25);
        batchSize = getParameterIntOrDefault(parameters, "batchSize", maxBatchSize / 10);
        numLayers = getParameterIntOrDefault(parameters, "numLayers", 3);
        maxTrainingIterations = getParameterIntOrDefault(parameters, "maxTrainingIterations", 100);
        maxSteps = getParameterIntOrDefault(parameters, "maxSteps", 5000);
        targetTrainSteps = getParameterIntOrDefault(parameters, "targetTrainSteps", 1000);
        momentumNesterov = getParameterDoubleOrDefault(parameters, "momentumNesterov", discountFactor);
        forceTraining = getParameterIntOrDefault(parameters, "forceTraining", 0) == 1;

        setHiddenSizeNodesMultiplier(hiddenSizeNodesMultiplier);


    }

    public String getPolicyPath() {
        return BASE_MEMORY_PATH + "policy_" + algorithmInfo + ".model";
    }

    public abstract LearningConfiguration getLearningConfiguration();

    public abstract NetworkConfiguration getNetworkConfiguration();

    public void setHiddenSizeNodesMultiplier(double hiddenSizeNodesMultiplier) {
        this.hiddenSizeNodesMultiplier = hiddenSizeNodesMultiplier;
        logger.info("set hiddenSizeNodesMultiplier to {}", this.hiddenSizeNodesMultiplier);
    }


    protected void constructorAbstract(Map<String, Object> parameters) {

        if (reinforcementLearningType == ReinforcementLearningType.q_learn) {
            System.out.println("Mark as QLearn!");
            logger.info("mark as qlearn algorithm");
        }

        cfTradesProcessed = EvictingQueue.create(DEFAULT_QUEUE_CF_TRADE);
        setParameters(parameters);
    }


    private void resetLastQValues() {
        beforeActionSnapshot = null;
    }

    public synchronized void init() {
        super.init();
        initAlgorithm();
        stateManager = new StateManager(this, state);
        startEnvironment();
    }

    protected void initAlgorithm() {
        this.marketMakingAlgorithm.setAlgorithmConnectorConfiguration(algorithmConnectorConfiguration);
        this.marketMakingAlgorithm.init();

        //unregister market data to call from here
        this.algorithmConnectorConfiguration.getMarketDataProvider().deregister(this.marketMakingAlgorithm);

    }

    protected double getPosition() {
        return getAlgorithmPosition(instrument);
    }

    //LISTENERS
    @Override
    public void onUpdateCandle(Candle candle) {
        super.onUpdateCandle(candle);
        marketMakingAlgorithm.onUpdateCandle(candle);//first underlying algo

    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        if (!trade.getInstrument().equalsIgnoreCase(instrument.getPrimaryKey())) {
            return false;
        }
        boolean output = super.onTradeUpdate(trade);

        if (stateManager != null && stateManager.isReady() && isReady()) {
            if (isActionSend()) {
                int millisElapsed = (int) (getCurrentTimestamp() - beforeActionSnapshot.getTimestamp());
                if (millisElapsed > timeHorizonSeconds * 1000) {
//                    logger.info("{} endIteration {} due to timeout {} ms", getCurrentTime(), iterations, millisElapsed);
                    endIteration();
                }
            }
        }

        marketMakingAlgorithm.onTradeUpdate(trade);
        return output;
    }

    @Override
    public boolean isReady() {
        boolean weAreReady = super.isReady();
        boolean mmIsReady = marketMakingAlgorithm != null && marketMakingAlgorithm.isReady();
        return weAreReady && mmIsReady;
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {

        if (!depth.getInstrument().equals(instrument.getPrimaryKey())) {
            return false;
        }
        if (lastDepth == null) {
            this.lastDepth = depth;
        }

        if (stateManager != null && stateManager.isReady() && isReady()) {
            if (isActionSend()) {
                int millisElapsed = (int) (getCurrentTimestamp() - beforeActionSnapshot.getTimestamp());
                if (millisElapsed > timeHorizonSeconds * 1000) {
//                    logger.info("{} endIteration {} due to timeout {} ms", getCurrentTime(), iterations, millisElapsed);
                    endIteration();
                }
            }
        }
        updateCurrentCustomColumn(depth.getInstrument());

        boolean output = super.onDepthUpdate(depth);
        marketMakingAlgorithm.onDepthUpdate(depth);

        return output;
    }

    public void startEnvironment() {
        if (!environmentStarted) {
            environmentStarted = true;
            environment.start();//train or load it if exist

        }
    }

    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        boolean output = true;
        try {
            output = super.onExecutionReportUpdate(executionReport);
            marketMakingAlgorithm.onExecutionReportUpdate(executionReport);

        } catch (Exception e) {
            logger.error("error treating ER ", e);
            output = false;
        }

        if (stateManager != null && stateManager.isReady() && isReady()) {
            if (isActionSend()) {
                int millisElapsed = (int) (getCurrentTimestamp() - beforeActionSnapshot.getTimestamp());
                if (millisElapsed > timeHorizonSeconds * 1000) {
//                    logger.info("{} endIteration {} due to timeout {} ms", getCurrentTime(), iterations, millisElapsed);
                    endIteration();
                }
            }
        }


        if (!isBacktest) {
            //			System.out.println("ER received " + executionReport);
        }

        //treat rejections before super!!
        boolean isRejected = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected);

        boolean isFilled = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)
                || executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled);

        if (isFilled) {
            logger.info("[{}] ER filled received : {}", this.getCurrentTime(), executionReport);
        }

        if (beforeActionSnapshot != null && isFilled && (this.getPosition() == 0 || stopActionOnFilled)) {
            long msElapsed = executionReport.getTimestampCreation() - beforeActionSnapshot.getTimestamp();
            //			int secondsElapsed = (int) ((msElapsed) / 1000);
            if (msElapsed == 0) {
                logger.warn("ER received in the same ms as send action!!");
            }
//            logger.info("{} endIteration {} due to Filled", getCurrentTime(), iterations);
            //time to update q matrix
            endIteration();
        }

        return output;
    }

    protected void endIteration() {
        stepFinished(this.state);
        long timestampSent = beforeActionSnapshot != null ? beforeActionSnapshot.getTimestamp() : 0;
        double reward = beforeActionSnapshot != null ? beforeActionSnapshot.getReward() : 0;
        long msElapsed = getCurrentTimestamp() - timestampSent;
        double deltaRewardNormalized = (lastRewardReceived - reward) / ((marketMakingAlgorithm.getQuantityBuy() + marketMakingAlgorithm.getQuantitySell()) / 2);
        onFinishedIteration(msElapsed, deltaRewardNormalized, lastRewardReceived);
        resetLastQValues();
    }

    @Override
    public boolean onCommandUpdate(Command command) {
        if (command.getMessage().equalsIgnoreCase(Command.ClassMessage.stop.name())) {
            logger.info("finished with {} iterations", iterations);

            logger.info("historical action List\n{}", com.lambda.investing.algorithmic_trading.ArrayUtils.PrintArrayListString(actionHistoricalList,","));

            if (isEndOfBacktestDay()) {
                onEndOfBacktestDay();
            }
        }

        this.setPlotStopHistorical(false);
        boolean output = super.onCommandUpdate(command);
        boolean isNotStop = !command.getMessage().equalsIgnoreCase(Command.ClassMessage.stop.name());
        if (isNotStop) {
            //we dont want to comunicate stop to underlying
            marketMakingAlgorithm.onCommandUpdate(command);
        } else {

        }

        return output;
    }

    public void setSaveBacktestOutputTrades(boolean saveBacktestOutputTrades) {
        super.setSaveBacktestOutputTrades(saveBacktestOutputTrades);
        if (this.marketMakingAlgorithm != null) {
            marketMakingAlgorithm.setSaveBacktestOutputTrades(saveBacktestOutputTrades);
        }
    }

    public void setPlotStopHistorical(boolean plotStopHistorical) {
        super.setPlotStopHistorical(plotStopHistorical);
        if (this.marketMakingAlgorithm != null) {
            marketMakingAlgorithm.setPlotStopHistorical(plotStopHistorical);
        }
    }

    public void onEndOfBacktestDay() {

        setDone(true);

        logger.info("{} end of backtest {} after {} iterations due to end of day", getCurrentTime(), trainingIterations, iterations);
        stepFinished(state);//just in case we are waiting yet

        if (iterations == 0) {
            logger.warn("{} end of backtest {} with no iterations!! -> dont force step", getCurrentTime(), trainingIterations);
        } else {
            int initialIterations = iterations;
            //wait to finish last iteration
            while (initialIterations == iterations) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        state.reset();//reset it here to avoid start very fast on next iteration in training
        resetAlgorithm();

    }

    public void restartPortfolioManager() {
        //restart rewards!
        portfolioManager.reset();
        if (marketMakingAlgorithm != null) {
            marketMakingAlgorithm.getPortfolioManager().reset();
        }
    }

    public void stop() {
        marketMakingAlgorithm.stop();
        super.stop();
    }

    public void start() {
        marketMakingAlgorithm.start();
        super.start();
    }


    protected double getCurrentReward() {
        PnlSnapshot lastPnlSnapshot = getLastPnlSnapshot(this.instrument.getPrimaryKey());
        if (lastPnlSnapshot == null) {
            logger.warn("lastPnlSnapshot is null to getCurrentReward -> return 0.0");
            return 0.0;
        }

        double reward = ScoreUtils.getReward(scoreEnum, lastPnlSnapshot);
        return reward;
    }

    protected boolean isActionSend() {
        return beforeActionSnapshot != null;
    }


    public void stepFinished(AbstractState state) {
        // will call it end of step!
        lastRewardReceived = getCurrentReward();
        iterations++;

        unlockCountDownLatch();


    }

    @Override
    public void resetAlgorithm() {
        super.resetAlgorithm();
        if (marketMakingAlgorithm != null) {
            marketMakingAlgorithm.resetAlgorithm();
        }
        state.reset();
        stateManager.reset();
        waitingReset = false;
        iterations = 0;
        iterationsStarting = 0;
    }

    @Override
    public void IsTraining() {
        setSaveBacktestOutputTrades(false);//for faster backtests
    }

    //on every iteration
    @Override
    public Box reset() {
        // reset backtest after initialized->need to return an initial state
        restartPortfolioManager();

        Box output = null;
        output = getCurrentState();

        return output;
    }

    @Override
    public void close() {
    }

    public Box getCurrentState() {
        double[] currentState = new double[states];
        if (state.isReady()) {
            currentState = state.getCurrentStateRounded();
        }
        if (currentState == null) {
            logger.warn("something goes wrong , return all zeros state");
            currentState = new double[states];
        }
        return new Box(currentState);
    }

    private void lockCountDownLatch() {
        synchronized (lockLatch) {
            if (latch == null || latch.getCount() == 0) {
                latch = new CountDownLatch(1);
            } else {
                logger.warn("lockCountDownLatch set action with already set latch count={}", latch.getCount());
            }
        }
    }

    private void unlockCountDownLatch() {
        synchronized (lockLatch) {
            if (latch != null) {
                if (latch.getCount() == 0) {
                    logger.warn("unlockCountDownLatch step finished without waiting action returned latch.getCount:{}", latch.getCount());
                    return;
                }
                latch.countDown();
            }
        }
    }

    public StepReply<Box> step(Integer action) {
        continueBacktest();
        iterationsStarting++;

        if (!state.isReady() || waitingReset) {
            return new StepReply<>(getCurrentState(), 0, false, null);
        }


        if (iterations == 0) {
            logger.info("[{}] First iteration *****", getCurrentTime());
        }


        //set the action
        double[] actionValues = null;
        try {
            actionValues = this.action.getAction(action);
        } catch (Exception e) {
            logger.error("error getting action!", e);
        }
        actionHistoricalList.add(action);

        double reward = 0.0;
        try {
            reward = getCurrentReward();
        } catch (Exception e) {
            logger.warn("error getting reward before setting action -> {}", e.getMessage());

        }
        beforeActionSnapshot = new BeforeActionSnapshot(action, getCurrentTimestamp(), reward, state.getCurrentStateRounded());

        lockCountDownLatch();

        setAction(actionValues, action);

        if (!waitFinishedStep(5)) {
            //timeout appears
            return new StepReply<>(getCurrentState(), 0, isDone, null);
        }


        Box currentState = getCurrentState();
        //wait the latch countdown
        double rewardReturned = lastRewardReceived;

        if (DELTA_REWARD) {
            double lastReward = beforeActionSnapshot != null ? beforeActionSnapshot.getReward() : DEFAULT_LAST_Q;
            if (lastReward == DEFAULT_LAST_Q) {
                lastReward = 0;
            }
            rewardReturned = lastRewardReceived - lastReward;
        }
        stopBacktest();
        return new StepReply<>(currentState, rewardReturned, isDone, null);

    }

    private void stopBacktest() {
        if (isBacktest) {
            ParquetMarketDataConnectorPublisher.PAUSE = true;
        }
    }

    private void continueBacktest() {
        if (isBacktest) {
            ParquetMarketDataConnectorPublisher.PAUSE = false;
        }
    }

    private boolean waitFinishedStep(int secondsTimeout) {
        try {
            latch.await(secondsTimeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("waiting reward took more than 5 seconds -> return generic answer 0 reward");
            return false;

        }
        return true;
    }

    public void finishedTrainingIteration() {
        unlockCountDownLatch();
        resetAlgorithm();
        trainingIterations++;
        waitingReset = true;
    }


    @AllArgsConstructor
    @Getter
    @Setter
    protected class BeforeActionSnapshot {
        protected int action = DEFAULT_LAST_Q;
        protected long timestamp = DEFAULT_LAST_Q;
        protected double reward = DEFAULT_LAST_Q;
        protected double[] state;


    }

}



