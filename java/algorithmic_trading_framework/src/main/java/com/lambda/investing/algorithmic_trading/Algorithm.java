package com.lambda.investing.algorithmic_trading;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.candle_manager.CandleFromTickUpdater;
import com.lambda.investing.algorithmic_trading.candle_manager.CandleListener;
import com.lambda.investing.algorithmic_trading.gui.main.MainMenuGUI;
import com.lambda.investing.algorithmic_trading.hedging.HedgeManager;
import com.lambda.investing.algorithmic_trading.hedging.NoHedgeManager;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.SingleInstrumentRLAlgorithm;
import com.lambda.investing.connector.ThreadUtils;
import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.market_data_connector.Statistics;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.portfolio.Portfolio;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartTheme;


import javax.swing.*;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lambda.investing.Configuration.*;
import static com.lambda.investing.model.Util.fromJsonString;
import static com.lambda.investing.model.portfolio.Portfolio.*;
import static org.jfree.chart.ChartFactory.getChartTheme;

public abstract class Algorithm extends AlgorithmParameters implements MarketDataListener, ExecutionReportListener, CandleListener {


    protected static long TIMEOUT_WAIT_DONE_SECONDS = 15;
    protected static int DEFAULT_QUEUE_CF_TRADE = 20;
    protected static int DEFAULT_QUEUE_HISTORICAL_ORDER_REQUEST = 20;
    protected static int DEFAULT_QUEUE_HISTORICAL_TRADES = 5;
    protected static long WARN_LATENCY_ORDER_REQUEST_MS = 500;
    protected Queue<String> cfTradesProcessed;

    protected Queue<ExecutionReport> erTradesProcessed;

    protected Map<String, OrderRequest> historicalOrdersRequestSent;


    public static int LOG_LEVEL = LogLevels.ALL_ITERATION_LOG.ordinal();//0 is s
    private static String REJECTION_NOT_FOUND = "not found for";
    private static Long STATISTICS_PRINT_SECONDS = 60L;
    private List<AlgorithmObserver> algorithmObservers;
    protected static String BASE_PATH_OUTPUT = Configuration.OUTPUT_PATH;
    protected static Integer FIRST_HOUR_DEFAULT = -1;
    protected static Integer LAST_HOUR_DEFAULT = 25;
    protected static DateFormat DAY_STR_DATE_FORMAT = new SimpleDateFormat("yyyymmdd");

    private static final String SEND_STATS = "->";
    private static final String RECEIVE_STATS = "<-";

    protected Integer firstHourOperatingIncluded = FIRST_HOUR_DEFAULT;//starting
    protected Integer lastHourOperatingIncluded = LAST_HOUR_DEFAULT;//stopping

    private AtomicInteger depthReceived = new AtomicInteger(0);
    private AtomicInteger tradeReceived = new AtomicInteger(0);

    protected Logger logger = LogManager.getLogger(Algorithm.class);

    @Getter
    @Setter
    protected boolean verbose = true;

    protected AlgorithmConnectorConfiguration algorithmConnectorConfiguration; //must be private because send orders must pass from here except for Portfolio
    protected String algorithmInfo;

    protected final Object lockLatchPosition = new Object();
    protected CountDownLatch lastPositionUpdateCountDown = new CountDownLatch(1);

    public String getAlgorithmInfo() {
        return algorithmInfo;
    }

    protected Map<String, InstrumentManager> instrumentToManager;

    protected long seed = 0;

    protected Map<String, Double> algorithmPosition;

    protected HedgeManager hedgeManager = new NoHedgeManager();

    protected ExecutionReportManager executionReportManager;

    @Getter
    protected AlgorithmType algorithmType = AlgorithmType.MarketMaking;

    public QuoteManager getQuoteManager(String instrumentPk) {

        QuoteManager quoteManager = instrumentQuoteManagerMap.get(instrumentPk);
        if (quoteManager == null) {
            quoteManager = new QuoteManager(this, Instrument.getInstrument(instrumentPk));
            instrumentQuoteManagerMap.put(instrumentPk, quoteManager);
        }
        return instrumentQuoteManagerMap.get(instrumentPk);
    }

    public boolean isRlAlgorithm() {
        return this instanceof SingleInstrumentRLAlgorithm;
    }


    private Map<String, Object> defaultParameters;
    protected Map<String, Object> parameters;

    protected Statistics statistics;
    protected LatencyStatistics latencyStatistics;
    protected SlippageStatistics slippageStatistics;

    public void setWaitDone(boolean waitDone) {
        this.waitDone = waitDone;
    }

    public PnlSnapshot getLastPnlSnapshot(String instrumentPk) {
        return portfolioManager.getLastPnlSnapshot(instrumentPk);
    }

    public void addCurrentCustomColumn(String instrumentPk, String key, Double value) {
        portfolioManager.addCurrentCustomColumn(instrumentPk, key, value);
        algorithmNotifier.notifyObserversCustomColumns(getCurrentTimestamp(), instrumentPk, key, value);
    }

    public PortfolioManager getPortfolioManager() {
        return portfolioManager;
    }

    protected PortfolioManager portfolioManager;
    protected AlgorithmNotifier algorithmNotifier;

    protected boolean isBacktest = false;
    protected boolean uiStarted = false;
    protected ChartTheme theme;

    protected boolean uiEnabled = false;
    protected MainMenuGUI algorithmicTradingGUI;
    protected boolean saveBacktestOutputTrades = true;

    protected boolean printSummaryBacktest = true;
    protected boolean isPaper = false;
    protected TimeServiceIfc timeService;
    protected AlgorithmState algorithmState = AlgorithmState.NOT_INITIALIZED;
    protected String summaryResultsAppend = null;

    public boolean isReady() {
        return getAlgorithmState().equals(AlgorithmState.STARTED);
    }

    public AlgorithmState getAlgorithmState() {
        return algorithmState;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Map<String, ExecutionReport> clientOrderIdLastCompletelyFillReceived;
    private static int HISTORICAL_TRADES_SAVE = 50;
    private boolean plotStopHistorical = false;
    protected boolean exitOnStop = true;

    protected boolean waitDone = false;
    private int lastCurrentDay = 0;

    protected boolean algoQuotesEnabled = true;
    //	private List<String> pendingToRemoveClientOrderId;
    private Map<String, QuoteManager> instrumentQuoteManagerMap = new ConcurrentHashMap<>();

    private Map<String, OrderRequest> clientOrderIdToCancelWhenActive = new ConcurrentHashMap<>();
    private CandleFromTickUpdater candleFromTickUpdater;
    public static final Object EXECUTION_REPORT_LOCK = new Object();
    protected Set<Instrument> instruments = new HashSet<>();

    public Algorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo,
                     Map<String, Object> parameters) {
        constructorForAbstract(algorithmConnectorConfiguration, algorithmInfo, parameters);
    }

    //for backtesting
    public Algorithm(String algorithmInfo, Map<String, Object> parameters) {
        constructorForAbstract(null, algorithmInfo, parameters);
    }

    public void setHedgeManager(HedgeManager hedgeManager) {
        this.hedgeManager = hedgeManager;
    }

    public HedgeManager getHedgeManager() {
        return hedgeManager;
    }


    public void setSaveBacktestOutputTrades(boolean saveBacktestOutputTrades) {
        this.saveBacktestOutputTrades = saveBacktestOutputTrades;
    }

    public void setPrintSummaryBacktest(boolean printSummaryBacktest) {
        this.printSummaryBacktest = printSummaryBacktest;
    }

    public CandleFromTickUpdater getCandleFromTickUpdater() {
        return candleFromTickUpdater;
    }


    private void configureIsPaper() {
        if (this.algorithmConnectorConfiguration
                .getTradingEngineConnector() instanceof AbstractBrokerTradingEngine) {
            AbstractBrokerTradingEngine abstractBrokerTradingEngine = (AbstractBrokerTradingEngine) this.algorithmConnectorConfiguration
                    .getTradingEngineConnector();
            isPaper = abstractBrokerTradingEngine.getPaperTradingEngine() != null;
        }
        if (this.algorithmConnectorConfiguration
                .getTradingEngineConnector() instanceof ZeroMqTradingEngineConnector) {
            ZeroMqTradingEngineConnector zeroMqTradingEngineConnector = (ZeroMqTradingEngineConnector) this.algorithmConnectorConfiguration
                    .getTradingEngineConnector();
            isPaper = zeroMqTradingEngineConnector.isPaperTrading();
        }

    }

    public void constructorForAbstract(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
                                       String algorithmInfo, Map<String, Object> parameters) {

        executionReportManager = new ExecutionReportManager();
        candleFromTickUpdater = new CandleFromTickUpdater();
        candleFromTickUpdater.register(this);
        algorithmPosition = new HashMap<>();

        algorithmObservers = new ArrayList<>();
        cfTradesProcessed = EvictingQueue.create(DEFAULT_QUEUE_CF_TRADE);
        erTradesProcessed = EvictingQueue.create(DEFAULT_QUEUE_HISTORICAL_TRADES);
        historicalOrdersRequestSent = new AlgorithmUtils.MaxSizeHashMap<String, OrderRequest>(
                DEFAULT_QUEUE_HISTORICAL_ORDER_REQUEST);

        if (algorithmConnectorConfiguration != null) {
            isBacktest = false;
            this.algorithmConnectorConfiguration = algorithmConnectorConfiguration;
            configureIsPaper();
        } else {
            if (isVerbose()) {
                logger.info("BACKTEST detected in {} -> Backtest TimeService", algorithmInfo);
            }
            isBacktest = true;
        }
        timeService = new BacktestTimeService("UTC");

        if (!isBacktest) {
            this.statistics = new Statistics(algorithmInfo, STATISTICS_PRINT_SECONDS * 1000);
            this.latencyStatistics = new LatencyStatistics(algorithmInfo, STATISTICS_PRINT_SECONDS * 1000);
            this.slippageStatistics = new SlippageStatistics(algorithmInfo, STATISTICS_PRINT_SECONDS * 1000);
        }
        this.algorithmInfo = algorithmInfo;
        this.parameters = parameters;
        this.defaultParameters = new ConcurrentHashMap<>(parameters);
        instrumentQuoteManagerMap = new ConcurrentHashMap<>();

        this.instrumentToManager = new ConcurrentHashMap<>();

        this.portfolioManager = new PortfolioManager(this);

        clientOrderIdLastCompletelyFillReceived = Collections
                .synchronizedMap(new LinkedHashMap<String, ExecutionReport>(HISTORICAL_TRADES_SAVE) {

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, ExecutionReport> entry) {
                        return size() > HISTORICAL_TRADES_SAVE;
                    }
                });
        algorithmNotifier = new AlgorithmNotifier(this, Configuration.THREADS_NOTIFY_ALGORITHM_OBSERVERS);

    }

    public void setPlotStopHistorical(boolean plotStopHistorical) {
        this.plotStopHistorical = plotStopHistorical;
    }

    public void setExitOnStop(boolean exitOnStop) {
//        if (isVerbose()) {
//            logger.info("Set {} exitOnStop to {}", algorithmInfo, exitOnStop);
//        }
//        this.exitOnStop = exitOnStop;
    }

    public InstrumentManager getInstrumentManager(String instrumentPk) {
        InstrumentManager instrumentManager = instrumentToManager.get(instrumentPk);
        if (instrumentManager == null) {
            instrumentManager = new InstrumentManager(Instrument.getInstrument(instrumentPk), isBacktest);
            instrumentToManager.put(instrumentPk, instrumentManager);
        }
        return instrumentToManager.get(instrumentPk);
    }

    public Map<String, ExecutionReport> getActiveOrders(Instrument instrument) {
        InstrumentManager instrumentManager = getInstrumentManager(instrument.getPrimaryKey());
        return instrumentManager.getAllActiveOrders();
    }

    public Map<String, OrderRequest> getRequestOrders(Instrument instrument) {
        InstrumentManager instrumentManager = getInstrumentManager(instrument.getPrimaryKey());
        return instrumentManager.getAllRequestOrders();
    }

    public void setAllRequestOrders(Instrument instrument, Map<String, OrderRequest> requestOrders) {
        InstrumentManager instrumentManager = getInstrumentManager(instrument.getPrimaryKey());
        instrumentManager.setAllRequestOrders(requestOrders);
    }

    public void register(AlgorithmObserver algorithmObserver) {
        algorithmObservers.add(algorithmObserver);
        algorithmNotifier.notifyLastParams();
    }

    public void deregister(AlgorithmObserver algorithmObserver) {
        algorithmObservers.remove(algorithmObserver);
    }

    public List<AlgorithmObserver> getAlgorithmObservers() {
        return algorithmObservers;
    }

    public void setAlgorithmConnectorConfiguration(AlgorithmConnectorConfiguration algorithmConnectorConfiguration) {
        this.algorithmConnectorConfiguration = algorithmConnectorConfiguration;
    }

    //Parameter settings
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
        this.firstHourOperatingIncluded = getParameterIntOrDefault(parameters, "firstHour", "first_hour",
                FIRST_HOUR_DEFAULT);//UTC time 6 -9
        this.lastHourOperatingIncluded = getParameterIntOrDefault(parameters, "lastHour", "last_hour",
                LAST_HOUR_DEFAULT);//UTC  18-21

        if (this.firstHourOperatingIncluded != FIRST_HOUR_DEFAULT
                || this.lastHourOperatingIncluded != LAST_HOUR_DEFAULT) {
            if (isVerbose()) {
                logger.info("Current time {} in utc is {}", new Date(), Instant.now());
                logger.info("start UTC hour at {} and finished on {} ,both included", this.firstHourOperatingIncluded,
                        this.lastHourOperatingIncluded);
            }

        }

        this.seed = getParameterIntOrDefault(parameters, "seed", 0);//load it here for initialization
        if (this.seed != 0) {
            setSeed(this.seed);
        }

        uiEnabled = getParameterIntOrDefault(parameters, "ui", 0) == 1;
        if (uiEnabled) {
            System.out.println("UI ENABLED");
            logger.info("UI ENABLED");
        }

        algorithmNotifier.notifyObserversOnUpdateParams(this.parameters);

    }

    public void setUiEnabled(boolean uiEnabled) {
        this.uiEnabled = uiEnabled;
    }

    protected void setSeed(long seed) {
        logger.info("setting seed from algorithm to {}", seed);
        SET_RANDOM_SEED(seed);
    }

    public void setParameter(String name, Object value) {
        this.parameters.put(name, value);
        algorithmNotifier.notifyObserversOnUpdateParams(this.parameters);
    }

    public abstract String printAlgo();

    public void init() {
        if (algorithmState.getNumber() < 0) {
            algorithmState = AlgorithmState.INITIALIZING;
            if (isVerbose())
                logger.info("[{}]Initializing algorithm {}", getCurrentTime(), algorithmInfo);

            if (algorithmConnectorConfiguration == null) {
                logger.error("can't initialize {} without AlgorithmConnectorConfiguration set", algorithmInfo);
            }
            this.algorithmConnectorConfiguration.getTradingEngineConnector().register(this.algorithmInfo, this);
            this.algorithmConnectorConfiguration.getMarketDataProvider().register(this);

            //send request
            requestInfo(this.algorithmInfo + "." + REQUESTED_POSITION_INFO);

            algorithmState = AlgorithmState.INITIALIZED;
            if (isVerbose())
                logger.info("[{}]initialized  {}", getCurrentTime(), algorithmInfo);

        } else {
            logger.warn("trying to init already initialized {}", algorithmInfo);
        }
    }

    protected void setPortfolio(Portfolio portfolio) {
        if (portfolio == null) {
            logger.warn("initial portfolio is null on {} -> not setting it", this.algorithmInfo);
        } else {
            portfolioManager.setPortfolio(portfolio);
        }
    }

    //PostContruct and destroy resset
    public void start() {
        if (algorithmState.equals(AlgorithmState.INITIALIZED) || algorithmState.equals(AlgorithmState.STOPPED)) {
            algorithmState = AlgorithmState.STARTING;
            if (isVerbose()) {
                logger.info("[{}] Start algorithm {}", getCurrentTime(), algorithmInfo);
            }
            algorithmState = AlgorithmState.STARTED;

            //set initial portfolio things from file
            //			Portfolio portfolio = algorithmConnectorConfiguration.getTradingEngineConnector().getPortfolio();
            //			setPortfolio(portfolio);

            //send request
            requestInfo(this.algorithmInfo + "." + REQUESTED_PORTFOLIO_INFO);

            if (uiEnabled && !uiStarted) {
                //start UI
                startUI();
            }
        }
    }

    protected void startUI() {
        setTheme();
        uiStarted = true;
    }

    private void setTheme() {
        //https://www.formdev.com/flatlaf/themes/
        FlatLaf flatLaf = null;
        if (isBacktest) {
            flatLaf = new FlatDarculaLaf();
        } else {
            flatLaf = new FlatIntelliJLaf();
        }

        try {
            UIManager.setLookAndFeel(flatLaf);
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
        theme = getChartTheme();
        ChartFactory.setChartTheme(theme);
    }

    public void stop() {
        if (algorithmState.equals(AlgorithmState.STARTED)) {
            algorithmState = AlgorithmState.STOPPING;
            if (isVerbose()) {
                logger.info("[{}] Stop received  {}", getCurrentTime(), algorithmInfo);
            }
            algorithmState = AlgorithmState.STOPPED;

            cancelAllInstruments();//out of market

            if (!isBacktest) {
                //save trades
                saveLiveOutputTrades();
            }

        }


    }


    private void saveLiveOutputTrades() {
        String todayStr = getCurrentDayStr();
        try {
            String basePath = BASE_PATH_OUTPUT + File.separator + "live_trades_table_" + algorithmInfo + "_"
                    + "_" + todayStr;
            portfolioManager.getTradesTableAndSave(basePath);
            System.out.println("STOP algo ->  saved trades at " + basePath + "_(instrument_pk).csv");
            logger.info("Stop live trading algo {} saving trades at {}_(instrument_pk).csv", algorithmInfo,
                    basePath);
            printSummaryResults();

        } catch (Exception e) {
            logger.error("something goes wrong on {} stop function", e);
            System.err.println(Configuration
                    .formatLog("something goes wrong on {} stop function", e.getMessage()));
        }

    }

    public Depth getLastDepth(Instrument instrument) {
        return getInstrumentManager(instrument.getPrimaryKey()).getLastDepth();
    }


    /**
     * Method that will be called every day and should restart all to start new fresh day
     */
    public void resetAlgorithm() {
        logger.info("[{}]Reset algorithm {}", getCurrentTime(), algorithmInfo);
        //		this.portfolioManager.reset();//will reset trades!

        for (QuoteManager quoteManager : instrumentQuoteManagerMap.values()) {
            quoteManager.reset();
        }

        clientOrderIdLastCompletelyFillReceived.clear();
        clientOrderIdToCancelWhenActive.clear();
        depthReceived = new AtomicInteger(0);
        tradeReceived = new AtomicInteger(0);

        if (algorithmConnectorConfiguration != null) {
            algorithmConnectorConfiguration.getMarketDataProvider().reset();
            algorithmConnectorConfiguration.getTradingEngineConnector().reset();
        }
        timeService.reset();
        algorithmState = AlgorithmState.INITIALIZED;
        lastCurrentDay = 0;
        portfolioManager.reset();

        for (InstrumentManager instrumentManager : instrumentToManager.values()) {
            instrumentManager.reset();
        }
    }


    //trading

    protected String generateClientOrderId() {
        byte[] dataInput = new byte[10];
        RANDOM_GENERATOR.nextBytes(dataInput);
        return UUID.nameUUIDFromBytes(dataInput).toString();
    }

    protected void requestInfo(String info) {
        algorithmConnectorConfiguration.getTradingEngineConnector().requestInfo(info);
    }

    public OrderRequest createCancel(Instrument instrument, String origClientOrderId) {
        OrderRequest cancelOrderRequest = new OrderRequest();
        cancelOrderRequest.setOrderRequestAction(OrderRequestAction.Cancel);
        cancelOrderRequest.setOrigClientOrderId(origClientOrderId);
        cancelOrderRequest.setAlgorithmInfo(algorithmInfo);
        cancelOrderRequest.setClientOrderId(generateClientOrderId());
        cancelOrderRequest.setInstrument(instrument.getPrimaryKey());
        cancelOrderRequest.setTimestampCreation(getCurrentTimestamp());
        if (latencyStatistics != null) {
            latencyStatistics.startKeyStatistics("cancel", cancelOrderRequest.getClientOrderId(), cancelOrderRequest.getTimestampCreation());
        }
        return cancelOrderRequest;
    }

    public void cancelAllVerb(Instrument instrument, Verb verb) {
        InstrumentManager instrumentManager = getInstrumentManager(instrument.getPrimaryKey());
        Map<String, ExecutionReport> instrumentActiveOrders = instrumentManager.getAllActiveOrders();

        if (instrumentActiveOrders.size() > 0 && LOG_LEVEL > LogLevels.DISABLE.ordinal()) {
            logger.info("cancelAll verb {}  {} active orders {}", verb, instrument, instrumentActiveOrders.size());
        }
        for (String clientOrderId : instrumentActiveOrders.keySet()) {
            ExecutionReport executionReport = instrumentActiveOrders.get(clientOrderId);
            if (!executionReport.getVerb().equals(verb)) {
                continue;
            }

            OrderRequest cancelOrderRequest = createCancel(instrument, clientOrderId);
            this.algorithmConnectorConfiguration.getTradingEngineConnector().orderRequest(cancelOrderRequest);
        }

        //save requested orders to cancel after active received
        Map<String, OrderRequest> requestOrders = instrumentManager.getAllRequestOrders();

        try {
            for (String clientOrderId : requestOrders.keySet()) {
                OrderRequest orderRequest = requestOrders.get(clientOrderId);
                if (orderRequest.getVerb().equals(verb)) {
                    clientOrderIdToCancelWhenActive.put(clientOrderId, orderRequest);
                }
            }
        } catch (Exception e) {
            logger.error("error on cancelAllVerb ", e);
            e.printStackTrace();
        }

    }

    private void cancelAllInstruments() {
        for (InstrumentManager instrumentManager : instrumentToManager.values()) {
            Instrument instrument = instrumentManager.getInstrument();
            try {
                getQuoteManager(instrument.getPrimaryKey()).unquote();
            } catch (LambdaTradingException e) {
                logger.error("can't unquote instrument {} on stop algorithm", instrument.getPrimaryKey(), e);
            }
            cancelAll(instrument);
        }
    }

    public void cancelAll(Instrument instrument) {
        InstrumentManager instrumentManager = getInstrumentManager(instrument.getPrimaryKey());
        Map<String, ExecutionReport> instrumentActiveOrders = instrumentManager.getAllActiveOrders();

        if (instrumentActiveOrders.size() > 0 && LOG_LEVEL > LogLevels.DISABLE.ordinal()) {
            logger.info("cancelAll {} active orders {}", instrument, instrumentActiveOrders.size());
        }
        for (String clientOrderId : instrumentActiveOrders.keySet()) {
            OrderRequest cancelOrderRequest = createCancel(instrument, clientOrderId);
            this.algorithmConnectorConfiguration.getTradingEngineConnector().orderRequest(cancelOrderRequest);
        }

        //save requested orders to cancel after active received
        Map<String, OrderRequest> requestOrders = instrumentManager.getAllRequestOrders();
        try {
            for (Map.Entry<String, OrderRequest> entry : requestOrders.entrySet()) {
                String clientOrderId = entry.getKey();
                OrderRequest requestOrder = entry.getValue();
                if (clientOrderId != null && requestOrder != null) {
                    clientOrderIdToCancelWhenActive.put(clientOrderId, requestOrder);
                }

            }
        } catch (Exception e) {
            logger.error("error on cancelAll ", e);
            e.printStackTrace();
        }

    }

    public QuoteRequest createQuoteRequest(Instrument instrument) {
        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setReferenceTimestamp(getLastDepth(instrument).getTimestamp());
        quoteRequest.setAlgorithmInfo(algorithmInfo);
        quoteRequest.setInstrument(instrument);
        return quoteRequest;
    }

    public boolean inOperationalTime() {
        int hour = getCurrentTimeHour();
        if (hour >= firstHourOperatingIncluded && hour <= lastHourOperatingIncluded) {
            return true;
        }
        return false;
    }

    public boolean isEndOfBacktestDay() {
        int hour = getCurrentTimeHour();
        if (hour >= firstHourOperatingIncluded && hour > lastHourOperatingIncluded) {
            return true;
        }
        return false;


    }

    protected boolean checkOperationalTime() {
        if (inOperationalTime()) {
            if (getAlgorithmState().equals(AlgorithmState.STOPPED)) {
                logger.info(
                        "[{}] inOperationalTime firstHourOperatingIncluded:{} lastHourOperatingIncluded:{} => start ",
                        getCurrentTime(), firstHourOperatingIncluded, lastHourOperatingIncluded);
            }
            start();
            return true;
        } else {
            if (getAlgorithmState().equals(AlgorithmState.STARTED)) {
                logger.info(
                        "[{}] not inOperationalTime firstHourOperatingIncluded:{} lastHourOperatingIncluded:{} => stop ",
                        getCurrentTime(), firstHourOperatingIncluded, lastHourOperatingIncluded);
            }
            stop();


        }

        //check change of day
        if (lastCurrentDay == 0) {
            lastCurrentDay = getCurrentTimeDay();
        } else if (lastCurrentDay != getCurrentTimeDay()) {
            logger.info("{} change of day detected {} - {}", getCurrentTime(), lastCurrentDay, getCurrentTimeDay());
            lastCurrentDay = getCurrentTimeDay();
            onNewDay();
        }

        return false;
    }

    protected void onFinishedBacktest() {
        if (isBacktest) {
            if (saveBacktestOutputTrades) {
                saveBacktestTrades();
            }

            if (printSummaryBacktest) {
                printSummaryResults();
            }

            if (plotStopHistorical) {
                plotBacktestResults();
            }

            if (exitOnStop) {
                if (waitDone) {
                    logger.info("waitDone to exit backtest...");
                    System.out.println("waitDone to exit backtest...");
                    waitDoneState();
                }
                System.out.println("Exit on stop in backtest");
                logger.info("Exit on stop in backtest");
                System.exit(0);
            }
        } else {
            logger.warn("onFinishedBacktest called but is not backtest");
            System.out.println("WARNING onFinishedBacktest called but is not backtest");
        }

    }

    protected void waitDoneState() {
        Date startWaiting = new Date();
        while (waitDone) {
            Thread.onSpinWait();
            long elapsed = new Date().getTime() - startWaiting.getTime();
            if (elapsed > TIMEOUT_WAIT_DONE_SECONDS * 1000) {
                logger.error("waitDoneState timeout {} seconds elapsed", TIMEOUT_WAIT_DONE_SECONDS);
                System.out.println("waitDoneState timeout " + TIMEOUT_WAIT_DONE_SECONDS + " seconds elapsed");
                break;
            }
        }

    }

    protected void onNewDay() {
        //reset the algo
        resetAlgorithm();
    }

    private void updateAllActiveOrders(ExecutionReport executionReport) {
        if (getAlgorithmState() == AlgorithmState.STOPPED) {
            return;
        }//required to not update when we are resetting RL
        String instrumentPk = executionReport.getInstrument();
        InstrumentManager instrumentManager = getInstrumentManager(instrumentPk);

        Queue<String> tradesInstrument = instrumentManager.getCfTradesReceived();


        Map<String, OrderRequest> instrumentSendOrders = instrumentManager.getAllRequestOrders();

        boolean isActive =
                executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Active) || executionReport
                        .getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled);

        boolean orderRequestNew = instrumentSendOrders.containsKey(executionReport.getClientOrderId()) && instrumentSendOrders
                .get(executionReport.getClientOrderId()).getOrderRequestAction().equals(OrderRequestAction.Send);

        boolean isNewRejected = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected) && orderRequestNew;
        boolean isInactive =
                executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Cancelled) || isNewRejected || executionReport
                        .getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled);

        boolean isCancelRejected = executionReport.getExecutionReportStatus()
                .equals(ExecutionReportStatus.CancelRejected);
        //todo search on active to delete in case
        boolean isFilled = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled)
                || executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled);

        //remove from requestOrders
        //		if (!instrumentSendOrders.containsKey(executionReport.getClientOrderId())) {
        //			logger.warn("received ER very fast {}    {}", executionReport.getClientOrderId(),executionReport.getExecutionReportStatus());
        //			pendingToRemoveClientOrderId.add(executionReport.getClientOrderId());
        //		}
        instrumentSendOrders.remove(executionReport.getClientOrderId());
        setAllRequestOrders(instrumentManager.getInstrument(), instrumentSendOrders);

        if (isActive) {
            boolean wasACfTrade = tradesInstrument.contains(executionReport.getClientOrderId());
            if (!wasACfTrade) {

                Map<String, ExecutionReport> instrumentActiveOrders = instrumentManager.getAllActiveOrders();

                instrumentActiveOrders.put(executionReport.getClientOrderId(), executionReport);
                //remove in case of modify!
                if (executionReport.getOrigClientOrderId() != null) {
                    instrumentActiveOrders.remove(executionReport.getOrigClientOrderId());
                }
                //update it
                instrumentManager.setAllActiveOrders(instrumentActiveOrders);
                if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
                    logger.debug("ER {} received active  {} ", executionReport.getClientOrderId(),
                            executionReport.getVerb());
                }

                if (clientOrderIdToCancelWhenActive.containsKey(executionReport.getClientOrderId())) {
                    if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
                        logger.debug("ER {} detected to be canceled", executionReport.getClientOrderId());
                    }
                    OrderRequest cancelRequest = createCancel(instrumentManager.getInstrument(),
                            executionReport.getClientOrderId());
                    try {
                        sendOrderRequest(cancelRequest);
                        clientOrderIdToCancelWhenActive.remove(executionReport.getClientOrderId());
                    } catch (LambdaTradingException e) {
                        logger.error("can't cancel waiting order {}", executionReport.getClientOrderId());
                    }
                }

            } else {
                if (LOG_LEVEL > LogLevels.ALL_ITERATION_LOG.ordinal()) {
                    logger.warn("received active {} of previous Cf trade => ignore",
                            executionReport.getClientOrderId());
                }
            }
        }

        if (isInactive) {
            if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)) {
                tradesInstrument.offer(executionReport.getClientOrderId());
                instrumentManager.setCfTradesReceived(tradesInstrument);
            }

            Map<String, ExecutionReport> instrumentActiveOrders = instrumentManager.getAllActiveOrders();
            instrumentActiveOrders.remove(executionReport.getClientOrderId());
            //just in case
            if (executionReport.getOrigClientOrderId() != null) {
                instrumentActiveOrders.remove(executionReport.getOrigClientOrderId());
            }
            instrumentManager.setAllActiveOrders(instrumentActiveOrders);
            if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
                logger.debug("ER {} received inactive ", executionReport.getClientOrderId());
            }

        }

        if (isCancelRejected) {
            Map<String, ExecutionReport> instrumentActiveOrders = instrumentManager.getAllActiveOrders();
            if (executionReport.getRejectReason().contains(REJECTION_NOT_FOUND) && instrumentActiveOrders
                    .containsKey(executionReport.getOrigClientOrderId())) {
                //remove it from active
                instrumentActiveOrders.remove(executionReport.getOrigClientOrderId());
                instrumentActiveOrders.remove(executionReport.getClientOrderId());//just in case
                instrumentManager.setAllActiveOrders(instrumentActiveOrders);
            }
            if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
                logger.debug("ER {} cancel rejected on {} ", executionReport.getClientOrderId(),
                        executionReport.getOrigClientOrderId());
            }

        }

        if (isFilled) {
            if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
                logger.debug("ER filled {} {}    {}@{} ", executionReport.getClientOrderId(),
                        executionReport.getExecutionReportStatus(), executionReport.getLastQuantity(),
                        executionReport.getPrice());
            }

            Map<Verb, Long> currentLastTradeTimestamp = instrumentManager.getLastTradeTimestamp();
            currentLastTradeTimestamp.put(executionReport.getVerb(), getCurrentTimestamp());
            instrumentManager.setLastTradeTimestamp(currentLastTradeTimestamp);
        }

    }

    private OrderRequest checkOrderRequest(OrderRequest orderRequest) throws LambdaTradingException {
        String instrumentPk = orderRequest.getInstrument();
        InstrumentManager instrumentManager = getInstrumentManager(instrumentPk);

        //Check order request
        if (orderRequest.getClientOrderId() == null) {
            orderRequest.setClientOrderId(generateClientOrderId());
        }

        if (orderRequest.getAlgorithmInfo() == null) {
            orderRequest.setAlgorithmInfo(algorithmInfo);
        }

        if (orderRequest.getOrderRequestAction() == null) {
            throw new LambdaTradingException("OrderRequest without action");
        }

        if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Send) || orderRequest.getOrderRequestAction()
                .equals(OrderRequestAction.Modify)) {
            if (orderRequest.getOrderType() == null) {
                throw new LambdaTradingException(
                        String.format("%s OrderRequest %s without ordertype", orderRequest.getClientOrderId(),
                                orderRequest.getOrderRequestAction().name()));
            }

            if (orderRequest.getVerb() == null) {
                throw new LambdaTradingException(
                        String.format("OrderRequest %s without verb", orderRequest.getOrderRequestAction().name()));
            }
            if (orderRequest.getOrderType() != null && !orderRequest.getOrderType().equals(OrderType.Market) && (
                    orderRequest.getPrice() == OrderRequest.NOT_SET_PRICE_VALUE)) {
                throw new LambdaTradingException(
                        String.format("%s OrderRequest %s %s without price!", orderRequest.getClientOrderId(),
                                orderRequest.getOrderRequestAction().name(), orderRequest.getOrderType().toString()));
            }
            if (orderRequest.getQuantity() <= 0) {
                throw new LambdaTradingException(String.format("%s OrderRequest  %s without valid quantity %.3f",
                        orderRequest.getClientOrderId(), orderRequest.getOrderRequestAction().name(),
                        orderRequest.getQuantity()));
            }


        }

        boolean needOrigClientOrdId =
                orderRequest.getOrderRequestAction().equals(OrderRequestAction.Modify) || orderRequest
                        .getOrderRequestAction().equals(OrderRequestAction.Cancel);

        if (needOrigClientOrdId && orderRequest.getOrigClientOrderId() == null) {
            String message = String
                    .format("%s is a %s need a OrigClientOrderId !=null in %s", orderRequest.getClientOrderId(),
                            orderRequest.getOrderRequestAction(), algorithmInfo);
            throw new LambdaTradingException(message);
        }

        if (needOrigClientOrdId && orderRequest.getOrigClientOrderId() != null) {
            String origClientOrderId = orderRequest.getOrigClientOrderId();
            Map<String, ExecutionReport> instrumentActiveOrders = instrumentManager.getAllActiveOrders();
            boolean isConfirmed = instrumentActiveOrders.containsKey(origClientOrderId);
            if (!isConfirmed) {
                String message = String
                        .format("%s is a %s need a OrigClientOrderId[%s] active in %s", orderRequest.getClientOrderId(),
                                orderRequest.getOrderRequestAction(), orderRequest.getOrigClientOrderId(),
                                algorithmInfo);
                throw new LambdaTradingException(message);
            }

        }

        //check latency statistics

        if (orderRequest.getReferenceTimestamp() != 0) {
            long latencyMs = getCurrentTime().getTime() - orderRequest.getReferenceTimestamp();
            if (latencyMs > WARN_LATENCY_ORDER_REQUEST_MS) {
                logger.warn("OrderRequest {} with latency {} ms > {} from depth reference", orderRequest, latencyMs, WARN_LATENCY_ORDER_REQUEST_MS);
                if (!isBacktest) {
                    System.err.println(Configuration.formatLog("OrderRequest {} with latency {} ms > {} from depth reference", orderRequest, latencyMs, WARN_LATENCY_ORDER_REQUEST_MS));
                }
            }
        }

        return orderRequest;
    }

    public void sendQuoteRequest(QuoteRequest quoteRequest) throws LambdaTradingException {
        if (quoteRequest.getQuoteRequestAction().equals(QuoteRequestAction.On) && !getAlgorithmState()
                .equals(AlgorithmState.STARTED)) {
            throw new LambdaTradingException("can't quote with algo not started");
        }
        QuoteManager quoteManager = getQuoteManager(quoteRequest.getInstrument().getPrimaryKey());
        quoteRequest.setBidPrice(quoteRequest.getInstrument().roundPrice(quoteRequest.getBidPrice()));
        quoteRequest.setAskPrice(quoteRequest.getInstrument().roundPrice(quoteRequest.getAskPrice()));
        //TODO DELETE
//        if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
//            double bestBid = getLastDepth(quoteRequest.getInstrument()).getBestBid();
//            double bestAsk = getLastDepth(quoteRequest.getInstrument()).getBestAsk();
//            logger.info("[{} bid:{} ask:{}] send quote -> {} ", getCurrentTime(), bestBid, bestAsk, quoteRequest.toString());
//        }

        quoteManager.quoteRequest(quoteRequest);
    }

    private void addStatistics(String topic) {
        if (this.statistics != null) {
            this.statistics.addStatistics(topic);
        }
    }


    protected void retryExecutionReportRejected(ExecutionReport executionReport, long delayMs) {
        boolean isRejected = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected);
        if (isRejected) {
            logger.info("ER rej received {}", executionReport.getRejectReason());
            // retry last action in another thread
            String instrumentPk = executionReport.getInstrument();
            InstrumentManager instrumentManager = getInstrumentManager(instrumentPk);
            Map<String, OrderRequest> instrumentSendOrders = instrumentManager.getAllRequestOrders();
            OrderRequest orderRequest = instrumentSendOrders.get(executionReport.getClientOrderId());
            //null pointer!!!!
            if (orderRequest != null) {
                orderRequest.setClientOrderId(generateClientOrderId());

                Runnable methodRun = new Runnable() {

                    @Override
                    public void run() {
                        try {
                            sendOrderRequest(orderRequest);
                        } catch (LambdaTradingException e) {
                            e.printStackTrace();
                            logger.error("error retrying to send {}", orderRequest, e);
                        }
                    }
                };

                ThreadUtils.schedule("retry_rejected", methodRun, delayMs);
            } else {
                System.err.println(Configuration.formatLog("{} not found to retry!   instrument orders map of size {}",
                        executionReport.getClientOrderId(), instrumentSendOrders.size()));
                logger.warn("{} not found to retry!   instrument orders map of size {}",
                        executionReport.getClientOrderId(), instrumentSendOrders.size());
            }
        }
    }

    public OrderRequest getOrderRequestHistorical(String clientOrderId) {
        return historicalOrdersRequestSent.get(clientOrderId);
    }

    public void sendOrderRequest(OrderRequest orderRequest) throws LambdaTradingException {
        if (!algorithmState.equals(AlgorithmState.STARTED) && !orderRequest.getOrderRequestAction()
                .equals(OrderRequestAction.Cancel)) {
            //cancel can be sent_
            throw new LambdaTradingException(
                    "can't send new/modify order with algo " + this.algorithmInfo + " not started");
        }

        String instrumentPk = orderRequest.getInstrument();
        InstrumentManager instrumentManager = getInstrumentManager(instrumentPk);

        orderRequest = checkOrderRequest(orderRequest);
        orderRequest.setTimestampCreation(getCurrentTimestamp());

        //updating the OrderRequestMap before sending
        Instrument instrumentOrder = instrumentManager.getInstrument();
        orderRequest.setPrice(instrumentOrder.roundPrice(orderRequest.getPrice()));

        Map<String, OrderRequest> instrumentSendOrders = instrumentManager.getAllRequestOrders();
        //		if(pendingToRemoveClientOrderId.contains(orderRequest.getClientOrderId())){
        //			pendingToRemoveClientOrderId.remove(orderRequest.getClientOrderId());
        //		}else {
        instrumentSendOrders.put(orderRequest.getClientOrderId(), orderRequest);
        setAllRequestOrders(instrumentOrder, instrumentSendOrders);
        //		}

        historicalOrdersRequestSent.put(orderRequest.getClientOrderId(), orderRequest);
        if (latencyStatistics != null) {
            try {
                String keyStatistic = Configuration.formatLog("{} {}", orderRequest.getOrderRequestAction().name(), orderRequest.getOrderType().name());
                latencyStatistics.startKeyStatistics(keyStatistic, orderRequest.getClientOrderId(), orderRequest.getTimestampCreation());
            } catch (Exception e) {
                logger.error("error starting latency statistics", e);
            }
        }
        if (slippageStatistics != null && orderRequest.getOrderType().equals(OrderType.Market)) {
            try {
                String instrument = orderRequest.getInstrument();
                Instrument instrument1 = Instrument.getInstrument(instrument);
                Verb sideToGet = orderRequest.getVerb();
                double marketPrice = getLastDepth(instrument1).getMidPrice();
                if (sideToGet.equals(Verb.Buy)) {
                    marketPrice = getLastDepth(instrument1).getBestAsk();
                }
                if (sideToGet.equals(Verb.Sell)) {
                    marketPrice = getLastDepth(instrument1).getBestBid();
                }
                slippageStatistics.registerPriceSent(sideToGet, instrument1, orderRequest.getClientOrderId(), marketPrice);
            } catch (Exception e) {
                logger.error("error registering slippage", e);
            }
        }

        this.algorithmConnectorConfiguration.getTradingEngineConnector().orderRequest(orderRequest);

        addStatistics("orderRequest." + orderRequest.getOrderRequestAction().name() + "." + orderRequest.getInstrument() + " " + SEND_STATS);
        algorithmNotifier.notifyObserversOnOrderRequest(orderRequest);
    }

    protected long generateRandomSeed() {
        return System.currentTimeMillis() + (RANDOM_GENERATOR).nextLong();
    }

    public void onCandleUpdate(Candle candle) {
        long timestamp = candle.getTimestamp();
        if (timestamp != 0 && isBacktest) {
            timeService.setCurrentTimestamp(timestamp);
        }
        addStatistics(RECEIVE_STATS + " candle." + candle.getCandleType().name() + "." + candle.getInstrumentPk());
    }

    private boolean depthTimestampAlreadyProccess(Depth depth) {
        long timestamp = depth.getTimestamp();
        if (isBacktest && instrumentToManager != null && instrumentToManager.containsKey(depth.getInstrument())) {
            InstrumentManager instrumentManager = getInstrumentManager(depth.getInstrument());
            Depth lastDepth = instrumentManager.getLastDepth();
            if (lastDepth == null) {
                return false;
            }
            long lastDepthTimestamp = lastDepth.getTimestamp();
            if (timestamp <= lastDepthTimestamp) {
                //avoid self updates from orderbook with same timestamp
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        long timestamp = depth.getTimestamp();
        try {
            if (depthTimestampAlreadyProccess(depth)) {
                //avoid self updates from orderbook with same timestamp
                return false;
            }

            try {
                candleFromTickUpdater.onDepthUpdate(depth);
            } catch (IndexOutOfBoundsException e) {
                //no one of the sides
            }
            if (timestamp != 0 && isBacktest) {
                timeService.setCurrentTimestamp(timestamp);
            }
            if (!isBacktest) {
                timeService.setCurrentTimestamp(new Date().getTime());
            }

            //check depth
        } catch (Exception e) {
            logger.warn("error capture onDepthUpdate on algorithm {} ", this.algorithmInfo, e);
        }
        depth = removeMe(depth);
        checkDepth(depth);
        //update cache
        portfolioManager.updateDepth(depth);
        algorithmNotifier.notifyObserversOnUpdateDepth(depth);//to stateManager - to state

        if (!checkOperationalTime()) {
            return false;
        }

        InstrumentManager instrumentManager = getInstrumentManager(depth.getInstrument());
        instrumentManager.setLastDepth(depth);
        addStatistics(RECEIVE_STATS + " depth." + depth.getInstrument());
        depthReceived.incrementAndGet();

        hedgeManager.onDepthUpdate(depth);
        return true;
    }

    private Depth removeMe(Depth depth) {

        String instrumentPk = depth.getInstrument();
        InstrumentManager instrumentManager = getInstrumentManager(instrumentPk);
        Map<String, ExecutionReport> activeOrdersByClordID = instrumentManager.getAllActiveOrders();
        if (activeOrdersByClordID == null || activeOrdersByClordID.size() == 0) {
            return depth;
        }
        try {
            Depth output = (Depth) depth.clone();
            for (ExecutionReport executionReport : activeOrdersByClordID.values()) {
                double price = executionReport.getPrice();
                double quantity = executionReport.getQuantity();
                Verb verb = executionReport.getVerb();
                //if verb is Buy remove that price from the depth in the bid side
                output.removeOrder(price, quantity, verb, getAlgorithmInfo());
            }
            return output;

        } catch (CloneNotSupportedException e) {
            logger.error("error cloning depth", e);
            return depth;
        }
    }

    private void checkDepth(Depth depth) {
        if (depth.isDepthFilled() && depth.getBestAsk() < depth.getBestBid()) {
            logger.warn("ask {} is lower than bid {}", depth.getBestAsk(), depth.getBestBid());
        }
    }


    @Override
    public boolean onTradeUpdate(Trade trade) {
        long timestamp = trade.getTimestamp();
        candleFromTickUpdater.onTradeUpdate(trade);
        if (timestamp != 0 && isBacktest) {
            timeService.setCurrentTimestamp(timestamp);
        }
        if (!isBacktest) {
            timeService.setCurrentTimestamp(new Date().getTime());
        }
        if (!checkOperationalTime()) {
            return false;
        }

        if (isMyLastTrade(trade)) {
            return false;
        }

        //update cache
        InstrumentManager instrumentManager = getInstrumentManager(trade.getInstrument());
        instrumentManager.setLastTrade(trade);

        addStatistics(RECEIVE_STATS + " trade." + trade.getInstrument());
        tradeReceived.incrementAndGet();
        algorithmNotifier.notifyObserversOnUpdatePnlSnapshot(trade);
        hedgeManager.onTradeUpdate(trade);
        return true;
    }

    private boolean isMyLastTrade(Trade trade) {
        for (ExecutionReport executionReport : erTradesProcessed) {
            //first is the last one
            boolean sameInstrument = executionReport.getInstrument().equals(trade.getInstrument());
            boolean samePrice = executionReport.getPrice() == trade.getPrice();
            boolean sameQuantity = executionReport.getLastQuantity() == trade.getQuantity();
            boolean sameTrade = sameInstrument && samePrice && sameQuantity;
            if (sameTrade) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCommandUpdate(Command command) {
        //Command received from backtest
        boolean isNotStop = !command.getMessage().equalsIgnoreCase(Command.ClassMessage.stop.name());
        boolean isStart = command.getMessage().equalsIgnoreCase(Command.ClassMessage.start.name());
        long timestamp = command.getTimestamp();
        if (isVerbose())
            logger.info("[{}] command received {} with timestamp {}[{}]  ", getCurrentTime(), command.getMessage(), new Date(timestamp), timestamp);
        if (timestamp != 0 && isBacktest && isStart) {
            timeService.reset();
            timeService.setCurrentTimestamp(timestamp);
            if (isVerbose())
                logger.info("Start backtest set timestamp to {} -> {}", new Date(timestamp), getCurrentTime());

            for (QuoteManager quoteManager : instrumentQuoteManagerMap.values()) {
                quoteManager.reset();
            }

        }

        if (!isBacktest) {
            timeService.setCurrentTimestamp(new Date().getTime());
        }


        if (command.getMessage().equalsIgnoreCase(Command.ClassMessage.stop.name())) {
            stop();
        }

        if (command.getMessage().equalsIgnoreCase(Command.ClassMessage.finishedBacktest.name())) {
            onFinishedBacktest();
        }

        if (command.getMessage().equalsIgnoreCase(Command.ClassMessage.start.name())) {
            start();
        }

        addStatistics(RECEIVE_STATS + " command." + command.getMessage());

        return true;
    }

    private void plotBacktestResults() {
        try {
            for (InstrumentManager instrumentManager : instrumentToManager.values()) {
                Instrument instrument = instrumentManager.getInstrument();
                portfolioManager.plotHistorical(instrument);
            }
        } catch (Exception e) {
            logger.error("can't plot historical ", e);
        }
    }

    private void saveBacktestTrades() {
        String basePath = BASE_PATH_OUTPUT + File.separator + "trades_table_" + algorithmInfo;
        logger.info("{} saving {} trades in {}_(instrument_pk).csv", getCurrentTime(), portfolioManager.numberOfTrades, basePath);

        portfolioManager.getTradesTableAndSave(basePath);
        logger.info("{} saved in {}_(instrument_pk).csv", getCurrentTime(), portfolioManager.numberOfTrades, basePath);
    }

    protected void printSummaryResults() {
        double totalPnl = 0.0;
        double realizedPnl = 0.0;
        double unrealizedPnl = 0.0;

        double totalFees = 0.0;
        double realizedFees = 0.0;
        double unrealizedFees = 0.0;
        int totalTrades = 0;
        Set<Instrument> instruments = getInstruments();

        for (Instrument instrument : instruments) {
            String output = (portfolioManager.summary(instrument));
            if (instruments.size() == 1) {
                if (summaryResultsAppend != null && !summaryResultsAppend.isEmpty()) {
                    output += "\n\t" + summaryResultsAppend;
                }
            }

            System.out.println(Configuration.formatLog("{}", instrument.getPrimaryKey()));
            System.out.println(output);
            logger.info("{}\n{}", instrument.getPrimaryKey(), output);

            PnlSnapshot pnlSnapshot = portfolioManager.getLastPnlSnapshot(instrument.getPrimaryKey());
            if (pnlSnapshot != null) {
                totalPnl += pnlSnapshot.getTotalPnl();
                realizedPnl += pnlSnapshot.getRealizedPnl();
                unrealizedPnl += pnlSnapshot.getUnrealizedPnl();
                totalFees += pnlSnapshot.totalFees;
                realizedFees += pnlSnapshot.realizedFees;
                unrealizedFees += pnlSnapshot.unrealizedFees;
                totalTrades += pnlSnapshot.getNumberOfTrades().get();
            } else {
                logger.warn("pnlSnapshot is null for {}", instrument.getPrimaryKey());
                System.out.println("pnlSnapshot is null for " + instrument.getPrimaryKey());
            }
        }
        if (instruments.size() > 1) {
            String header = Configuration.formatLog("\n********\nTOTAL: {} instruments\n********", instruments.size());

            String body = String
                    .format("\ttrades:%d  totalPnl:%.3f totalFees:%.3f\n\trealizedPnl:%.3f  realizedFees:%.3f \n\tunrealizedPnl:%.3f  unrealizedFees:%.3f ",
                            totalTrades, totalPnl,
                            totalFees, realizedPnl, realizedFees,
                            unrealizedPnl, unrealizedFees);

            if (summaryResultsAppend != null && !summaryResultsAppend.isEmpty()) {
                body += "\n\t" + summaryResultsAppend;
            }

            logger.info(header);
            logger.info(body);

            System.out.println(header);
            System.out.println(body);


        }
    }

    public Set<Instrument> getInstruments() {
        return instruments;
    }

    /**
     * Has to be called by the algo extending
     *
     * @param executionReport
     */
    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        synchronized (EXECUTION_REPORT_LOCK) {
            if (!executionReport.getAlgorithmInfo().equalsIgnoreCase(getAlgorithmInfo())) {
                //is not mine
                return true;
            }
            boolean hasPriority = executionReportManager.isNewStatus(executionReport);
            if (!hasPriority) {
                //already processed
                return false;
            }

            if (latencyStatistics != null) {
                latencyStatistics.stopKeyStatistics(executionReport.getClientOrderId(), executionReport.getTimestampCreation());
            }

            updateAllActiveOrders(executionReport);

            boolean isTrade = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)
                    || executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled);
            if (isTrade) {

                if (slippageStatistics != null) {
                    slippageStatistics.registerPriceExecuted(executionReport.getClientOrderId(), executionReport.getPrice());
                }

                erTradesProcessed.offer(executionReport);
                if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)) {
                    if (cfTradesProcessed.contains(executionReport.getClientOrderId())) {
                        //already processed
                        return false;
                    }
                    cfTradesProcessed.offer(executionReport.getClientOrderId());
                }

                addToPersist(executionReport);
                addPosition(executionReport);
                if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)) {
                    clientOrderIdLastCompletelyFillReceived.put(executionReport.getClientOrderId(), executionReport);
                }
            }

            if (algoQuotesEnabled) {
                getQuoteManager(executionReport.getInstrument()).onExecutionReportUpdate(executionReport);
                addStatistics(RECEIVE_STATS + " executionReport." + executionReport.getExecutionReportStatus().name() + "." + executionReport.getInstrument());
            }

            algorithmNotifier.notifyObserversonExecutionReportUpdate(executionReport);

            hedgeManager.onExecutionReportUpdate(executionReport);

            return true;
        }

    }

    private void addPosition(ExecutionReport executionReport) {
        InstrumentManager instrumentManager = getInstrumentManager(executionReport.getInstrument());
        double previousPosition = instrumentManager.getPosition();
        double lastQty = executionReport.getLastQuantity();
        if (executionReport.getVerb().equals(Verb.Sell)) {
            lastQty = lastQty * -1;
        }
        double newPosition = previousPosition + lastQty;
        instrumentManager.setPosition(newPosition);

        double myPosition = algorithmPosition.getOrDefault(executionReport.getInstrument(), 0.0);
        myPosition += lastQty;
        algorithmPosition.put(executionReport.getInstrument(), myPosition);
    }

    protected double getPosition(Instrument instrument) {
        return getInstrumentManager(instrument.getPrimaryKey()).getPosition();
    }

    protected double getAlgorithmPosition(Instrument instrument) {
        return algorithmPosition.getOrDefault(instrument.getPrimaryKey(), 0.0);
    }

    public OrderRequest createMarketOrderRequest(Instrument instrument, Verb verb, double quantity) {
        ///controled market request
        String newClientOrderId = this.generateClientOrderId();
        OrderRequest output = new OrderRequest();
        output.setAlgorithmInfo(algorithmInfo);
        output.setInstrument(instrument.getPrimaryKey());
        output.setVerb(verb);
        output.setOrderRequestAction(OrderRequestAction.Send);
        output.setClientOrderId(newClientOrderId);
        output.setQuantity(quantity);
        //		Depth lastDepth = getLastDepth(instrument);
        //		if (verb.equals(Verb.Sell)){
        //			output.setPrice(lastDepth.getBestBid()-lastDepth.getSpread());
        //		}
        //		if (verb.equals(Verb.Buy)){
        //			output.setPrice(lastDepth.getBestAsk()+lastDepth.getSpread());
        //		}

        output.setTimestampCreation(getCurrentTimestamp());
        output.setOrderType(OrderType.Market);//limit for quoting
        output.setMarketOrderType(MarketOrderType.FAS);//default FAS
        return output;
    }

    protected OrderRequest createLimitOrderRequest(Instrument instrument, Verb verb, double price, double quantity) {
        String newClientOrderId = generateClientOrderId();
        OrderRequest output = new OrderRequest();
        output.setAlgorithmInfo(algorithmInfo);
        output.setInstrument(instrument.getPrimaryKey());
        output.setVerb(verb);
        output.setOrderRequestAction(OrderRequestAction.Send);
        output.setClientOrderId(newClientOrderId);
        output.setQuantity(quantity);
        output.setPrice(price);

        output.setTimestampCreation(getCurrentTimestamp());

        output.setOrderType(OrderType.Limit);//limit for quoting
        output.setMarketOrderType(MarketOrderType.FAS);//default FAS

        return output;
    }

    protected void addToPersist(ExecutionReport executionReport) {
        PnlSnapshot pnlSnapshot = portfolioManager.addTrade(executionReport);
        algorithmNotifier.notifyObserversOnUpdatePnlSnapshot(pnlSnapshot);
        if (!isBacktest) {
            printRowTrade(executionReport);
            //			System.out.println(
            //					String.format("%s   %.4f@%.4f  ->\n %s", executionReport.getVerb(), executionReport.getQuantity(),
            //							executionReport.getPrice(), executionReport));
            //			plotResultsTrade();
        }
    }

    protected void printRowTrade(ExecutionReport executionReport) {

        for (String instrumentPk : instrumentToManager.keySet()) {
            PnlSnapshot pnlSnapshot = portfolioManager.getLastPnlSnapshot(instrumentPk);
            if (pnlSnapshot == null || pnlSnapshot.numberOfTrades.get() == 0) {
                continue;
            }
            String output = String
                    .format("\r %s  trades:%d  position:%.3f totalPnl:%.3f realizedPnl:%.3f unrealizedPnl:%.3f  ",
                            instrumentPk, pnlSnapshot.numberOfTrades.get(), pnlSnapshot.netPosition,
                            pnlSnapshot.totalPnl, pnlSnapshot.realizedPnl, pnlSnapshot.unrealizedPnl);
            if (isBacktest) {
                System.out.print(output);
            } else {
                System.out.println(output);
            }

        }
    }

    public Date getCurrentTime() {
        return timeService.getCurrentTime();
    }

    public String getCurrentDayStr() {
        return DAY_STR_DATE_FORMAT.format(getCurrentTime());
    }

    public long getCurrentTimestamp() {
        if (!this.isBacktest) {
            long currentTime = System.currentTimeMillis();
            timeService.setCurrentTimestamp(currentTime);
        }
        return timeService.getCurrentTimestamp();
    }

    protected int getCurrentTimeHour() {
        return timeService.getCurrentTimeHour();
    }

    protected int getCurrentTimeDay() {
        return timeService.getCurrentTimeDay();
    }

    protected int getCurrentTimeMinute() {
        return timeService.getCurrentTimeMinute();
    }

    public boolean onPosition(Map<String, Double> positions) {
        algorithmPosition = positions;//override it
        Set<String> instrumentPks = instrumentToManager.keySet();
        for (String instrumentPK : instrumentPks) {
            Instrument instrument = Instrument.getInstrument(instrumentPK);
            if (instrument == null) {
                logger.warn("received position of instrumentPK not found {}", instrumentPK);
                continue;
            }
            InstrumentManager instrumentManager = getInstrumentManager(instrumentPK);
            double position = positions.getOrDefault(instrumentPK, 0.0);
            logger.info("onPosition {} = {}", instrumentPK, position);
            if (!isBacktest) {
                System.out.println(Configuration.formatLog("onPosition {} = {}", instrumentPK, position));
            }
            instrumentManager.setPosition(position);
        }

        synchronized (lockLatchPosition) {
            if (lastPositionUpdateCountDown != null) {
                lastPositionUpdateCountDown.countDown();
            }
        }

        return true;
    }

    protected void requestUpdatePosition(boolean synchronous) {
        if (isBacktest) {
            return;
        }
        if (synchronous) {
            synchronized (lockLatchPosition) {
                if (lastPositionUpdateCountDown == null || lastPositionUpdateCountDown.getCount() == 0) {
                    lastPositionUpdateCountDown = new CountDownLatch(1);
                }
            }
        }
        requestInfo(algorithmInfo + "." + REQUESTED_POSITION_INFO);

        if (synchronous) {
            try {
                lastPositionUpdateCountDown.await();
            } catch (Exception e) {
                ;
            }
        }

    }

    @Override
    public boolean onInfoUpdate(String header, String message) {
        if (header.startsWith(REQUESTED_POSITION_INFO)) {
            logger.info("received position from broker {}", message);
            Map<String, Double> positions = fromJsonString(message, Map.class);
            return onPosition(positions);
        }
        if (header.endsWith(REQUESTED_PORTFOLIO_INFO)) {
            Portfolio portfolio = fromJsonString(message, Portfolio.class);
            if (isBacktest) {
                return true;
            }
            logger.info("received portfolio from broker -> set");
            setPortfolio(portfolio);
            return true;
        }
        logger.warn("unknown onInfoUpdate header {} -> return false", header);
        return false;
    }
}
