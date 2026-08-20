package com.lambda.investing.trading_engine_connector.paper;

import com.lambda.investing.ArrayUtils;
import com.lambda.investing.Configuration;
import com.lambda.investing.LambdaThreadFactory;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import com.lambda.investing.market_data_connector.AbstractMarketDataConnectorPublisher;
import com.lambda.investing.market_data_connector.AbstractMarketDataProvider;
import com.lambda.investing.market_data_connector.MarketDataConnectorPublisher;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.portfolio.Portfolio;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.trading_engine_connector.AbstractPaperExecutionReportConnectorPublisher;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;
import com.lambda.investing.trading_engine_connector.paper.latency.FixedLatencyEngine;
import com.lambda.investing.trading_engine_connector.paper.latency.LatencyEngine;
import com.lambda.investing.trading_engine_connector.paper.latency.LatencyEngineFactory;
import com.lambda.investing.trading_engine_connector.paper.market.OrderMatchEngine;
import com.lambda.investing.trading_engine_connector.paper.market.Orderbook;
import com.lambda.investing.trading_engine_connector.paper.market.OrderbookManager;
import lombok.Setter;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.File;
import java.util.*;
import java.util.concurrent.*;

import static com.lambda.investing.Configuration.DELAY_ORDER_BACKTEST_MS;
import static com.lambda.investing.Configuration.RANDOM_SEED;
import static com.lambda.investing.model.Util.toJsonString;
import static com.lambda.investing.model.portfolio.Portfolio.REQUESTED_PORTFOLIO_INFO;
import static com.lambda.investing.model.portfolio.Portfolio.REQUESTED_POSITION_INFO;
import static com.lambda.investing.trading_engine_connector.AbstractTradingEngineConnector.ALL_ALGORITHMS_SUBSCRIPTION;


public class PaperTradingEngine extends AbstractPaperExecutionReportConnectorPublisher
        implements TradingEngineConnector {

    public static boolean USE_ORDER_MATCHING_ENGINE = true;

    private static String FORMAT_PORTFOLIO =
            Configuration.OUTPUT_PATH + File.separator + "%s_paperTradingEngine_position.json";
    private static final boolean NOTIFY_MARKET_TRADES_NOT_EXECUTED = true;
    private Logger logger = LogManager.getLogger(PaperTradingEngine.class);

    private MarketDataProvider marketDataProvider;
    private MarketMakerMarketBacktestDataAlgorithm marketMakerMarketBacktestDataAlgorithm;
    private ConnectorProvider orderRequestConnectorProvider;
    private ConnectorConfiguration orderRequestConnectorConfiguration;

    private PaperConnectorPublisher paperConnectorMarketDataAndExecutionReportPublisher;
    private PaperConnectorOrderRequestListener paperConnectorOrderRequestListener;

    private List<Instrument> instrumentsList = new ArrayList<>();
    private Map<String, OrderbookManager> orderbookManagerMap;
    MarketDataProviderIn marketDataProviderIn;

    Map<String, Portfolio> portfolioMap;

    protected LatencyEngine orderRequestLatencyEngine = new FixedLatencyEngine(
            DELAY_ORDER_BACKTEST_MS);// //change to more ms on OrdinaryBacktest
    protected LatencyEngine marketDataLatencyEngine = new FixedLatencyEngine(0);
    protected LatencyEngine executionReportLatencyEngine = new FixedLatencyEngine(0);

    @Setter
    private boolean isBacktest = false;
    private boolean isPaperTrading = false;
    private boolean rejectAllOrders = false;

    public List<Instrument> getInstrumentsList() {
        return instrumentsList;
    }

    public MarketDataProvider getMarketDataProviderIn() {
        return marketDataProviderIn;
    }

    public void setOrderRequestLatencyEngine(LatencyEngine orderRequestLatencyEngine) {
        this.orderRequestLatencyEngine = orderRequestLatencyEngine;
    }

    public void setExecutionReportLatencyEngine(LatencyEngine executionReportLatencyEngine) {
        this.executionReportLatencyEngine = executionReportLatencyEngine;
    }


    @Override
    public boolean isBusy() {
        return getTradingEngineConnector().isBusy() || marketDataProviderIn.isBusy();
    }

    public PaperTradingEngine(TradingEngineConnector tradingEngineConnector, MarketDataProvider marketDataProvider,
                              ConnectorProvider orderRequestConnectorProvider,
                              ConnectorConfiguration orderRequestConnectorConfiguration) {
        super(tradingEngineConnector);
        this.marketDataProvider = marketDataProvider;
        this.marketMakerMarketBacktestDataAlgorithm = new MarketMakerMarketBacktestDataAlgorithm(this);
        this.orderRequestConnectorProvider = orderRequestConnectorProvider;
        this.orderRequestConnectorConfiguration = orderRequestConnectorConfiguration;

        //portfolio file not on the broker side

        //listen on this side
        portfolioMap = new HashMap<>();

        this.paperConnectorOrderRequestListener = new PaperConnectorOrderRequestListener(this,
                this.orderRequestConnectorProvider, this.orderRequestConnectorConfiguration);

        if (Configuration.isDebugging()) {
            logger.info("debugging detected => set latencies engines to no delay! ");
            setOrderRequestLatencyEngine(new FixedLatencyEngine(0));
            setExecutionReportLatencyEngine(new FixedLatencyEngine(0));
            marketDataLatencyEngine = new FixedLatencyEngine(0);
        }

        marketDataProviderIn = new MarketDataProviderIn(executionReportLatencyEngine, marketDataLatencyEngine,
                Configuration.BACKTEST_THREADS_PUBLISHING_MARKETDATA,
                Configuration.BACKTEST_THREADS_PUBLISHING_EXECUTION_REPORTS);

        if (Configuration.MULTITHREADING_CORE.equals(Configuration.MULTITHREAD_CONFIGURATION.SINGLE_THREADING)) {
            orderRequestLatencyEngine = LatencyEngineFactory.getLatencyEngine(Configuration.LATENCY_ENGINE_TYPE, 0, RANDOM_SEED);
        }
    }

    public void setDelayOrderRequestMs(long orderRequestMs) {
        orderRequestLatencyEngine = LatencyEngineFactory.getLatencyEngine(Configuration.LATENCY_ENGINE_TYPE, orderRequestMs, RANDOM_SEED);
    }

    public void setPaperTrading(boolean paperTrading) {
        isPaperTrading = paperTrading;
        orderRequestLatencyEngine.setIsPaperTrading(isPaperTrading);
        marketDataLatencyEngine.setIsPaperTrading(isPaperTrading);
        executionReportLatencyEngine.setIsPaperTrading(isPaperTrading);
    }


    public MarketMakerMarketBacktestDataAlgorithm getMarketMakerMarketDataExecutionReportListener() {
        return marketMakerMarketBacktestDataAlgorithm;
    }

    public void init() {
        //subscribe to data
        this.paperConnectorOrderRequestListener.start();
        this.marketDataProvider.register(this.marketMakerMarketBacktestDataAlgorithm);

        this.marketDataProviderIn.registerExecutionReport(marketMakerMarketBacktestDataAlgorithm);
        this.register(ALL_ALGORITHMS_SUBSCRIPTION, this.marketMakerMarketBacktestDataAlgorithm);
        this.orderRequestConnectorProvider
                .register(this.orderRequestConnectorConfiguration, this.paperConnectorOrderRequestListener);

        logger.info("Starting PaperTrading Engine publishing md/er on {}   listening Orders on {}",
                //MD configuration
                this.paperConnectorMarketDataAndExecutionReportPublisher.getConnectorConfiguration()
                        .getConnectionConfiguration(),
                this.orderRequestConnectorConfiguration.getConnectionConfiguration());

        //TODO something more generic on not ZeroMq
        if (this.orderRequestConnectorProvider instanceof ZeroMqProvider) {
            ZeroMqProvider orderRequestConnectorProviderZero = (ZeroMqProvider) this.orderRequestConnectorProvider;
            orderRequestConnectorProviderZero.start(false, false, "OrderRequestPaperTradingProvider");//subscribed to all topics on that port
        }
    }

    public void setInstrumentsList(List<Instrument> instrumentsList) {
        this.instrumentsList = instrumentsList;
        //
        logger.info("creating {} orderbooks: {}", instrumentsList.size(), ArrayUtils.PrintArrayListString(instrumentsList, ","));
        orderbookManagerMap = new HashMap<>(instrumentsList.size());
        for (Instrument instrument : instrumentsList) {
            if (instrument == null) {
                logger.warn("PaperTradingEngine instrument is null -> not creating orderbook");
                continue;
            }
            Orderbook orderbook = new Orderbook(instrument.getPriceTick());

            OrderbookManager orderbookManager = null;
            if (!USE_ORDER_MATCHING_ENGINE) {
                orderbookManager = new OrderbookManager(orderbook, this, instrument.getPrimaryKey());
            } else {
                orderbookManager = new OrderMatchEngine(orderbook, this, instrument.getPrimaryKey());

            }
            orderbookManagerMap.put(instrument.getPrimaryKey(), orderbookManager);
        }
        Configuration.BACKTEST_BUSY_THREADPOOL_TRESHOLD *= instrumentsList.size();//multiply this

    }

    public void setInstrumentsList(Instrument instrument) {
        this.instrumentsList = new ArrayList<>();
        this.instrumentsList.add(instrument);
        setInstrumentsList(this.instrumentsList);

    }

    public void setPaperConnectorMarketDataAndExecutionReportPublisher(
            PaperConnectorPublisher paperConnectorMarketDataAndExecutionReportPublisher) {
        this.paperConnectorMarketDataAndExecutionReportPublisher = paperConnectorMarketDataAndExecutionReportPublisher;
    }

    private String getTopic(String instrumentPk) {
        return instrumentPk;
    }

    private String getTopic(Instrument instrument) {
        return instrument.getPrimaryKey();
    }

    public void notifyCommand(Command command) {
        String topic = "command";
        logger.debug("Notifying command -> \n{}", command.getMessage());

        if (command.getMessage().equalsIgnoreCase(Command.ClassMessage.finishedBacktest.name())) {
            logger.info("Backtest finished -> reject all orders");
            rejectAllOrders = true;
        }

        this.marketDataProviderIn.notifyCommand(command);
    }

    protected void updateLatencyEngineTime(long timestamp, long nextUpdateMs) {
        Date date = new Date(timestamp);
        orderRequestLatencyEngine.setTime(date);
        if (nextUpdateMs != Long.MIN_VALUE) {
            orderRequestLatencyEngine.setNextUpdateMs(nextUpdateMs);
        }
        marketDataProviderIn.updateLatencyEngineTime(date, timestamp, nextUpdateMs);
    }

    /**
     * Publish the new depth to paper
     *
     * @param depth
     */
    public void notifyDepth(Depth depth) {
        if (!depth.isDepthFilled()) {
            //stop here
//            logger.debug("");
        }
        updateLatencyEngineTime(depth.getTimestamp(), depth.getTimeToNextUpdateMs());
        //Orderbook is filled -> notify the rest of algos
        Instrument instrument = Instrument.getInstrument(depth.getInstrument());
        String topic = getTopic(instrument);
//        logger.debug("Notifying depth -> \n{}", depth.toString());//to much time consuming!
        this.marketDataProviderIn.notifyDepth(depth);
    }

    /**
     * Publish the last trade to paper
     *
     * @param trade
     */
    public void notifyTrade(Trade trade) {
        if (trade.getQuantity() > 0) {
            String topic = getTopic(trade.getInstrument());
//            logger.debug("Notifying trade -> \n{}", trade.toString());//to much time consuming!
            updateLatencyEngineTime(trade.getTimestamp(), trade.getTimeToNextUpdateMs());
            //trade is filled notify the rest
            this.marketDataProviderIn.notifyTrade(trade);
        }

    }

    private void pauseBacktest() {
        AbstractMarketDataConnectorPublisher.setPauseTradingEngine(true);
    }

    private void resumeBacktest() {
        AbstractMarketDataConnectorPublisher.setPauseTradingEngine(false);
    }

    public void notifyExecutionReport(ExecutionReport executionReport) {
        logger.debug("Notifying execution report -> \n{}", executionReport.toString());
        this.marketDataProviderIn.notifyExecutionReport(executionReport);
        resumeBacktest();//pause backtest to avoid long md processing on our mock backtest
        Portfolio portfolio = null;
        if (portfolioMap.containsKey(executionReport.getAlgorithmInfo())) {
            portfolio = portfolioMap.get(executionReport.getAlgorithmInfo());
        } else {
            portfolio = Portfolio
                    .getPortfolio(String.format(FORMAT_PORTFOLIO, executionReport.getAlgorithmInfo()), isBacktest, isPaperTrading);
        }
        portfolio.updateTrade(executionReport);
        portfolioMap.put(executionReport.getAlgorithmInfo(), portfolio);

    }

    public MarketDataConnectorPublisher getMarketDataConnectorPublisher() {
        return paperConnectorMarketDataAndExecutionReportPublisher;
    }

    @Override
    public boolean cancelAll(Instrument instrument) {
        return false;
    }


    @Override
    public List<OrderRequest> activeOrders() {
        return null;
    }

    public boolean orderRequest(OrderRequest orderRequest) {
        //Send orders to the virtual orderbook
        OrderbookManager orderbookManager = orderbookManagerMap.get(orderRequest.getInstrument());
        if (orderbookManager == null) {
            logger.error("trying to send orderRequest on {} not found in manager", orderRequest.getInstrument());
            return false;
        }

        if (rejectAllOrders) {
            logger.info("Rejecting orderRequest due to rejectAllOrders {}", orderRequest);
            orderbookManager.notifyExecutionReportReject(orderRequest, "PaperTradingEngine rejectAllOrders");
            return true;
        }

        orderRequestLatencyEngine.delay(orderRequestLatencyEngine.getCurrentTime());

        boolean output = false;
        try {
            pauseBacktest();//pause backtest to avoid long md processing on our mock backtest

            orderRequest.setTimestampCreation(
                    orderRequestLatencyEngine.getCurrentTime().getTime());//update OrderRequestTime if required


            output = orderbookManager.orderRequest(orderRequest);

        } catch (Exception e) {
            logger.error("Error on orderRequest {}", orderRequest, e);
        }

        return output;
    }

    @Override
    public void requestInfo(String info) {
        if (info.endsWith(REQUESTED_PORTFOLIO_INFO)) {
            //return portfolio on execution Report
            String algorithmInfo = info.split("[.]")[0];
//            Portfolio portfolio = portfolioMap.getOrDefault(algorithmInfo,
//                    Portfolio.getPortfolio(String.format(FORMAT_PORTFOLIO, algorithmInfo), isBacktest, isPaperTrading));
            Portfolio portfolio = portfolioMap.getOrDefault(algorithmInfo,
                    new Portfolio());
            portfolioMap.put(algorithmInfo, portfolio);

            String header = Configuration.formatLog("{}.{}", REQUESTED_PORTFOLIO_INFO, algorithmInfo);
            this.marketDataProviderIn.notifyInfo(header, toJsonString(portfolio));
        }
        if (info.endsWith(REQUESTED_POSITION_INFO)) {
            String algorithmInfo = info.split("[.]")[0];
            //get position of the instrument from portfolio
//            Portfolio portfolio = portfolioMap.getOrDefault(algorithmInfo,
//                    Portfolio.getPortfolio(String.format(FORMAT_PORTFOLIO, algorithmInfo), isBacktest, isPaperTrading));
            Portfolio portfolio = portfolioMap.getOrDefault(algorithmInfo,
                    new Portfolio());
            portfolioMap.put(algorithmInfo, portfolio);


            String header = Configuration.formatLog("{}.{}", REQUESTED_POSITION_INFO, algorithmInfo);
            this.marketDataProviderIn.notifyInfo(header, toJsonString(portfolio.getPositions()));
        }
    }

    @Override
    public void reset() {
        if (orderbookManagerMap != null) {
            for (OrderbookManager orderbookManager : orderbookManagerMap.values()) {
                orderbookManager.reset();
            }
        }
        orderRequestLatencyEngine.reset();
        marketDataLatencyEngine.reset();
        executionReportLatencyEngine.reset();
        rejectAllOrders = false;
    }

    public void fillOrderbook(Depth depth) {
        OrderbookManager orderbookManager = orderbookManagerMap.get(depth.getInstrument());
        if (orderbookManager == null) {
            return;
        }
        orderbookManager.refreshMarketMakerDepth(depth);
    }

    public boolean fillMarketTrade(Trade trade) {
        OrderbookManager orderbookManager = orderbookManagerMap.get(trade.getInstrument());
        boolean isNotifiedByExecution = false;
        if (orderbookManager != null) {
            isNotifiedByExecution = orderbookManager.refreshFillMarketTrade(trade);
        }
        if (NOTIFY_MARKET_TRADES_NOT_EXECUTED && !isNotifiedByExecution) {
            //notify by market
            //			logger.debug("orderbook fill trade ->  {}",trade.toString());
            notifyTrade(trade);
        }
        return true;
    }

    private class MarketDataProviderIn extends AbstractMarketDataProvider {

        LatencyEngine executionReportLatencyEngine = null;
        LatencyEngine marketDataLatencyEngine = null;

        private int threadsPublishingMd, threadsPublishingER;

        ThreadFactory namedThreadFactoryMarketData = LambdaThreadFactory.createThreadFactory("MarketDataProviderIn-MarketData");
        ThreadFactory namedThreadFactoryExecutionReport = LambdaThreadFactory.createThreadFactory("MarketDataProviderIn-ExecutionReport");

        ThreadPoolExecutor marketDataPool, executionReportPool;
        boolean killMarketDataPool = false, killExecutionReportPool = false;
        protected Map<ExecutionReportListener, String> executionReportListenersManager;//for backtesting

        protected Queue<String> lastActiveClOrdId;
        protected Queue<String> lastCfClOrdId;
        protected Queue<String> lastRejClOrdId;


        public MarketDataProviderIn(LatencyEngine executionReportLatencyEngine, LatencyEngine marketDataLatencyEngine,
                                    int threadsPublishingMd, int threadsPublishingER) {
            this.executionReportLatencyEngine = executionReportLatencyEngine;
            this.marketDataLatencyEngine = marketDataLatencyEngine;

            this.threadsPublishingER = threadsPublishingER;
            this.threadsPublishingMd = threadsPublishingMd;

            executionReportListenersManager = new HashMap<>();

            lastActiveClOrdId = EvictingQueue.create(QUEUE_FINAL_STATES_SIZE);
            lastCfClOrdId = EvictingQueue.create(QUEUE_FINAL_STATES_SIZE);
            lastRejClOrdId = EvictingQueue.create(QUEUE_FINAL_STATES_SIZE);

            initExecutionReportPool();
            initMarketDataPool();

        }

        public boolean isBusy() {
            boolean marketPoolBusy = marketDataPool != null && marketDataPool.getQueue().size() > Configuration.BACKTEST_BUSY_THREADPOOL_TRESHOLD;
            boolean executionReportBusy = executionReportPool != null && executionReportPool.getQueue().size() > Configuration.BACKTEST_BUSY_THREADPOOL_TRESHOLD;
            return marketPoolBusy || executionReportBusy;
        }


        private void initMarketDataPool() {
            if (this.threadsPublishingMd > 0) {
                marketDataPool = (ThreadPoolExecutor) Executors
                        .newFixedThreadPool(this.threadsPublishingMd, namedThreadFactoryMarketData);
            }
            if (this.threadsPublishingMd < 0) {
                marketDataPool = (ThreadPoolExecutor) Executors.newCachedThreadPool(namedThreadFactoryMarketData);
            }
        }

        private void initExecutionReportPool() {
            if (this.threadsPublishingER > 0) {
                executionReportPool = (ThreadPoolExecutor) Executors
                        .newFixedThreadPool(this.threadsPublishingER, namedThreadFactoryExecutionReport);
            }
            if (this.threadsPublishingER < 0) {
                executionReportPool = (ThreadPoolExecutor) Executors
                        .newCachedThreadPool(namedThreadFactoryExecutionReport);
            }
        }

        public void registerExecutionReport(ExecutionReportListener listener) {
            executionReportListenersManager.put(listener, "");
        }

        @Override
        public void notifyDepth(Depth depth) {
            if (threadsPublishingMd == 0) {
                super.notifyDepth(depth);
            } else {
                marketDataPool.submit(() -> {
                    if (killMarketDataPool) {
                        return;
                    }
                    marketDataLatencyEngine.delay(depth.getDate());
                    super.notifyDepth(depth);
                });
            }

        }

        @Override
        public void notifyTrade(Trade trade) {
            if (threadsPublishingMd == 0) {
                super.notifyTrade(trade);
            } else {
                marketDataPool.submit(() -> {
                    if (killMarketDataPool) {
                        return;
                    }
                    marketDataLatencyEngine.delay(trade.getDate());
                    super.notifyTrade(trade);
                });
            }

        }


        protected boolean checkER(ExecutionReport executionReport) {
            /// void double notifications
            Queue<String> checkList = lastActiveClOrdId;
            if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Active)) {
                checkList = lastActiveClOrdId;
            } else if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletelyFilled)) {
                checkList = lastCfClOrdId;
            } else if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected) || executionReport
                    .getExecutionReportStatus().equals(ExecutionReportStatus.CancelRejected)) {
                checkList = lastRejClOrdId;
            } else {
                //partials filled dont check it
                return true;
            }

            if (checkList.size() > QUEUE_FINAL_STATES_SIZE * 2) {
                logger.warn("something is wrong on checkER queues!!! {} >{} return as valid", checkList.size(),
                        QUEUE_FINAL_STATES_SIZE);
                return true;
            }

            if (checkList.contains(executionReport.getClientOrderId())) {
                //already processed
                return false;
            } else {
                checkList.offer(executionReport.getClientOrderId());
                return true;
            }

        }

        public void notifyExecutionReport(ExecutionReport executionReport) {
//            if(checkER(executionReport)) {
            TradingEngineConnector tradingEngineConnector = getTradingEngineConnector();
            if (threadsPublishingER == 0) {
                tradingEngineConnector.notifyExecutionReport(executionReport);
            } else {
                executionReportPool.submit(() -> {
                    if (killExecutionReportPool) {
                        return;
                    }
                    marketDataLatencyEngine.delay(new Date(executionReport.getTimestampCreation()));
                    tradingEngineConnector.notifyExecutionReport(executionReport);
                });
            }
//            }
        }

        protected void updateLatencyEngineTime(Date date, long timestamp, long nextUpdateMs) {
            marketDataLatencyEngine.setTime(date);
            if (nextUpdateMs != Long.MIN_VALUE) {
                marketDataLatencyEngine.setNextUpdateMs(nextUpdateMs);
            }

            executionReportLatencyEngine.setTime(date);
            if (nextUpdateMs != Long.MIN_VALUE) {
                executionReportLatencyEngine.setNextUpdateMs(nextUpdateMs);
            }
        }

        public void reset() {
            if (executionReportPool != null) {
                try {
                    killExecutionReportPool = true;
                    executionReportPool.shutdown();
                    executionReportPool.awaitTermination(60000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    logger.error("timeout waiting finished executionReportPool :{}", executionReportPool.toString());
                } finally {
                    killExecutionReportPool = false;
                    initExecutionReportPool();
                }
            }

            if (marketDataPool != null) {
                try {
                    killMarketDataPool = true;
                    marketDataPool.shutdown();
                    marketDataPool.awaitTermination(60000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    logger.error("timeout waiting finished executionReportPool :{}", marketDataPool.toString());
                } finally {
                    killMarketDataPool = false;
                    initMarketDataPool();
                }
            }

            orderRequestLatencyEngine.setTime(new Date());//trick to unblock it

            marketDataLatencyEngine.reset();
            executionReportLatencyEngine.reset();
            marketDataProvider.reset();//reset the market data provider sequence numbers
            getTradingEngineConnector().reset();
            orderRequestLatencyEngine.reset();
            super.reset();
        }
    }

}
