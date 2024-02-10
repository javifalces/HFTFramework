package com.lambda.investing.algorithmic_trading.market_making.reinforcement_learning;

import com.lambda.investing.ArrayUtils;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.market_making.MarketMakingAlgorithm;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreEnum;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.SingleInstrumentRLAlgorithm;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.MarketState;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.MultiMarketState;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.StateManager;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public abstract class RLAbstractMarketMaking extends SingleInstrumentRLAlgorithm {

    public static boolean DEFAULT_STOP_ACTION_FILLED = false;


    ///dqn fields
    //state
    protected StateManager stateManager;

    //reward


    protected int numberDecimalsMarketState, numberDecimalsCandleState, numberDecimalsPrivateState;
    protected double minMarketState, maxMarketState, minCandleState, maxCandleState, minPrivateState, maxPrivateState;
    protected int horizonTicksMarketState, horizonCandlesState, horizonTicksPrivateState;
    protected long horizonMinMsTick;

    protected String[] stateColumnsFilter = null;


    protected Depth lastDepth = null;


    protected MarketMakingAlgorithm algorithm;

    protected static int DEFAULT_QUEUE_TRADE_SIZE_MINUTES = 60;
    protected Queue<Double> queueTrades;
    protected Queue<Candle> queueTradeCandles;

    protected int stepSeconds;

    protected boolean stopActionOnFilled = DEFAULT_STOP_ACTION_FILLED;


    public RLAbstractMarketMaking(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
                                  String algorithmInfo, Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);
        logger = LogManager.getLogger(RLAbstractMarketMaking.class);

        constructorAbstract(parameters);
    }

    public void setMarketMakerAlgorithm(MarketMakingAlgorithm algorithm, Map<String, Object> parameters) {
        this.algorithm = algorithm;

        //avoid double printing
        this.algorithm.setVerbose(false);
        this.algorithm.setSaveBacktestOutputTrades(false);
        this.algorithm.setPrintSummaryBacktest(false);

        this.algorithm.constructorForAbstract(algorithmConnectorConfiguration, algorithmInfo, parameters);
        this.algorithm.setParameters(parameters);
        this.algorithm.setUiEnabled(false);
    }


    //abstract methods
    protected abstract void updateCurrentCustomColumn(String instrumentPk);


    public void resetAlgorithm() {
        super.resetAlgorithm();
        if (isTraining()) {
            iterations.set(0);
            stateManager.reset();
        }
    }


    public void setParameters(Map<String, Object> parameters) {
        super.setParameters(parameters);
        this.parameters = parameters;

        this.stopActionOnFilled = getParameterIntOrDefault(parameters, "stopActionOnFilled", DEFAULT_STOP_ACTION_FILLED ? 1 : 0) == 1;
        this.stepSeconds = getParameterInt(parameters, "stepSeconds");
//        this.epsilon = getParameterDouble(parameters, "epsilon");

        // NN parameters
        this.scoreEnum = ScoreEnum.valueOf(getParameterString(parameters, "scoreEnum"));


        ///STATE configuration discretize
        this.horizonMinMsTick = getParameterInt(parameters, "horizonMinMsTick");

        this.minPrivateState = getParameterDoubleOrDefault(parameters, "minPrivateState", -1);
        this.maxPrivateState = getParameterDoubleOrDefault(parameters, "maxPrivateState", -1);
        this.horizonTicksPrivateState = getParameterInt(parameters, "horizonTicksPrivateState");
        this.numberDecimalsPrivateState = getParameterIntOrDefault(parameters, "numberDecimalsPrivateState", -1);

        this.minMarketState = getParameterDoubleOrDefault(parameters, "minMarketState", -1);
        this.maxMarketState = getParameterDoubleOrDefault(parameters, "maxMarketState", -1);
        this.horizonTicksMarketState = getParameterIntOrDefault(parameters, "horizonTicksMarketState",
                this.horizonTicksPrivateState);
        this.numberDecimalsMarketState = getParameterIntOrDefault(parameters, "numberDecimalsMarketState", -1);

        this.minCandleState = getParameterDoubleOrDefault(parameters, "minCandleState", -1);
        this.maxCandleState = getParameterDoubleOrDefault(parameters, "maxCandleState", -1);
        this.horizonCandlesState = getParameterIntOrDefault(parameters, "horizonCandlesState", 1);
        this.numberDecimalsCandleState = getParameterIntOrDefault(parameters, "numberDecimalsCandleState", -1);


        this.firstHourOperatingIncluded = getParameterIntOrDefault(parameters, "firstHour",
                FIRST_HOUR_DEFAULT);//UTC time 6 -9
        this.lastHourOperatingIncluded = getParameterIntOrDefault(parameters, "lastHour",
                LAST_HOUR_DEFAULT);//UTC  18-21

        if (this.firstHourOperatingIncluded != FIRST_HOUR_DEFAULT
                || this.lastHourOperatingIncluded != LAST_HOUR_DEFAULT) {
            if (isVerbose()) {
                logger.info("Current time {} in utc is {}", new Date(), Instant.now());
                logger.info("start UTC hour at {} and finished on {} ,both included", this.firstHourOperatingIncluded,
                        this.lastHourOperatingIncluded);
            }
        }

    }


    /**
     * Get the columns to be used in the state filter the market states and add the private states that are required
     *
     * @param stateColumnsFilter
     * @return
     */
    private String[] getFilteredStates(String[] stateColumnsFilter) {
        //add private and individuals
        if (stateColumnsFilter != null && stateColumnsFilter.length > 0) {
            List<String> privateStatesList = (((MarketState) this.state).getPrivateColumns());
            List<String> individualsStateList = (((MarketState) this.state).getIndividualColumns());
            List<String> columnsFilter = (Arrays.asList(stateColumnsFilter));
            //detect suffix number of columnsFilter and get the maximun one


            List<String> output = new ArrayList<>();

            output.addAll(privateStatesList);
            output.addAll(individualsStateList);
            output.addAll(columnsFilter);

            //drop duplicates
            output = output.stream().distinct().collect(Collectors.toList());

            String[] stateColumnsFilterOut = new String[output.size()];
            stateColumnsFilterOut = output.toArray(stateColumnsFilterOut);
            return stateColumnsFilterOut;
        }
        return null;
    }

    @Override
    public void setInstrument(Instrument instrument) {
        algorithm.setInstrument(instrument);
        super.setInstrument(instrument);
        setState();
        this.state.setNumberOfDecimals(instrument.getNumberDecimalsPrice());
    }


    protected void setState() {
        double quantity = (algorithm.quantityBuy + algorithm.quantitySell) / 2;
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

            List<String> otherInstrumentsStr = ArrayUtils
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
        setFilteredState();
        printState();
    }


    protected void constructorAbstract(Map<String, Object> parameters) {
        algoQuotesEnabled = false;
        queueTrades = EvictingQueue.create(DEFAULT_QUEUE_TRADE_SIZE_MINUTES);
        queueTradeCandles = EvictingQueue.create(DEFAULT_QUEUE_TRADE_SIZE_MINUTES);
        cfTradesProcessed = EvictingQueue.create(DEFAULT_QUEUE_CF_TRADE);
        setParameters(parameters);
    }

    public synchronized void init() {
        super.init();
        initAlgorithm();
        stateManager = new StateManager(this, state);
    }

    protected void initAlgorithm() {
        this.algorithm.setAlgorithmConnectorConfiguration(algorithmConnectorConfiguration);
        this.algorithm.init();

        //unregister market data to call from here
        this.algorithmConnectorConfiguration.getMarketDataProvider().deregister(this.algorithm);
    }

    protected double getPosition() {
        return getAlgorithmPosition(instrument);
    }

    //LISTENERS
    @Override
    public void onUpdateCandle(Candle candle) {
        super.onUpdateCandle(candle);
        algorithm.onUpdateCandle(candle);//first underlying algo

    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        if (!trade.getInstrument().equalsIgnoreCase(instrument.getPrimaryKey())) {
            return false;
        }
        boolean output = super.onTradeUpdate(trade);
        algorithm.onTradeUpdate(trade);
        return output;
    }

    @Override
    public boolean isReady() {
        boolean weAreReady = super.isReady();
        boolean mmIsReady = algorithm != null && algorithm.isReady();
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
            synchronized (this) {
                if (timestampStartStep != DEFAULT_LAST_Q) {
                    int millisElapsed = (int) (depth.getTimestamp() - timestampStartStep);
                    if (millisElapsed > stepSeconds * 1000) {
                        endIterationSaveReward(getCurrentReward(), millisElapsed);
                    }
                }

            }

        }
        updateCurrentCustomColumn(depth.getInstrument());

        boolean output = super.onDepthUpdate(depth);
        algorithm.onDepthUpdate(depth);

        return output;
    }

    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        boolean output = true;
        try {
            output = super.onExecutionReportUpdate(executionReport);
            algorithm.onExecutionReportUpdate(executionReport);

        } catch (Exception e) {
            logger.error("error treating ER ", e);
            output = false;
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

        if (isFilled && (this.getPosition() == 0 || stopActionOnFilled)) {
            long currentTime = Math.max(executionReport.getTimestampCreation(), getCurrentTimestamp());
            long msElapsed = currentTime - timestampStartStep;
            if (msElapsed == 0) {
                logger.warn("ER received in the same ms as send action!!");
            }
            logger.info("[{}] ER filled received and stopActionOnFilled", this.getCurrentTime());
            //time to update q matrix
            if (isReady()) {
                endIterationSaveReward(getCurrentReward(), msElapsed);
            } else {
                logger.warn("ER received filled but state is not ready");
            }
        }

        return output;
    }

    protected void endIterationSaveReward(double reward, long msElapsed) {
        onFinishedIteration(msElapsed, reward, state.getCurrentStateRounded());
        resetLastQValues();
    }

    private void resetLastQValues() {
        //no required restart variables
        timestampStartStep = DEFAULT_LAST_Q;//avoid double finish iteration

    }


    @Override
    public boolean onCommandUpdate(Command command) {
        if (command.getMessage().equalsIgnoreCase(Command.ClassMessage.stop.name())) {
            logger.info("finished with {} iterations", iterations);
        }

        this.setPlotStopHistorical(false);

        boolean output = super.onCommandUpdate(command);

        algorithm.onCommandUpdate(command);
        return output;
    }

    @Override
    public void setExitOnStop(boolean exitOnStop) {
        super.setExitOnStop(exitOnStop);
        algorithm.setExitOnStop(exitOnStop);
    }

    @Override
    public void stop() {
        algorithm.stop();
        super.stop();
    }

    @Override
    public void start() {
        algorithm.start();
        super.start();
    }

    @Override
    protected double getLastMidPrice() {
        if (lastDepth == null) {
            return 0;
        }
        return lastDepth.getMidPrice();
    }

    @Override
    protected double getQuantityMean() {
        return (algorithm.getQuantityBuy() + algorithm.getQuantitySell()) / 2;
    }


}
