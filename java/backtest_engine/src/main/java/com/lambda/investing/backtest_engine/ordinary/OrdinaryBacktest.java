package com.lambda.investing.backtest_engine.ordinary;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lambda.investing.Configuration;
import com.lambda.investing.backtest_engine.AbstractBacktest;
import com.lambda.investing.backtest_engine.BacktestConfiguration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorPublisherProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.market_data_connector.MarketDataConnectorPublisherListener;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.market_data_connector.csv_file_reader.CSVMarketDataConnectorPublisher;
import com.lambda.investing.market_data_connector.ordinary.OrdinaryMarketDataProvider;
import com.lambda.investing.market_data_connector.parquet_file_reader.ParquetMarketDataConnectorPublisher;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;
import com.lambda.investing.trading_engine_connector.ordinary.OrdinaryTradingEngine;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class OrdinaryBacktest extends AbstractBacktest {

    private OrdinaryBacktestRLGym ordinaryBacktestRLGym;
    private boolean isSingleThread = false;

    private OrdinaryConnectorConfiguration ordinaryConnectorConfiguration = new OrdinaryConnectorConfiguration();

    public OrdinaryBacktest(BacktestConfiguration backtestConfiguration) throws Exception {
        super(backtestConfiguration);

        if (Configuration.MULTITHREADING_CORE.equals(Configuration.MULTITHREAD_CONFIGURATION.SINGLE_THREADING)) {
            System.out.println("SINGLE_THREADING detected from Configuration on OrdinaryBacktest => setSingleThread");
            setSingleThread(true);
        }

        if (Configuration.isDebugging()) {
            System.out.println("DEBUGGING DETECTED");
            logger.info("DEBUGGING DETECTED");
        }

        if (isOrdinaryBacktestRLGym()) {
            logger.info("OrdinaryBacktestRLGym detected {}", backtestConfiguration.getRLZeroMqConfiguration());
            try {
                ordinaryBacktestRLGym = new OrdinaryBacktestRLGym(this);
            } catch (Exception e) {
                logger.error("Error in OrdinaryBacktestRLGym {}", e);
                System.err.println("Error in OrdinaryBacktestRLGym " + e);
                e.printStackTrace();
            }
        }

    }

    private boolean isOrdinaryBacktestRLGym() {
        //is configuration for RLGym
        ZeroMqConfiguration rlGymConfig = backtestConfiguration.getRLZeroMqConfiguration();
        //is strategy valid
        boolean isStrategyValid = OrdinaryBacktestRLGym.IsRLAlgorithm(backtestConfiguration);

        if (isStrategyValid && rlGymConfig == null) {
            logger.error("check if you have set rlGymConfig in backtestConfiguration");
        }

        return rlGymConfig != null && isStrategyValid;
    }

    public void registerEndOfFile(MarketDataConnectorPublisherListener marketDataConnectorPublisherListener) {
        this.paperConnectorMarketDataAndExecutionReportPublisher.register(marketDataConnectorPublisherListener);

        if (ordinaryMarketDataConnectorPublisher instanceof CSVMarketDataConnectorPublisher) {
            CSVMarketDataConnectorPublisher csvMarketDataConnectorPublisher = (CSVMarketDataConnectorPublisher) this.ordinaryMarketDataConnectorPublisher;
            csvMarketDataConnectorPublisher.register(marketDataConnectorPublisherListener);
        }
        if (ordinaryMarketDataConnectorPublisher instanceof ParquetMarketDataConnectorPublisher) {
            ParquetMarketDataConnectorPublisher parquetMarketDataConnectorPublisher = (ParquetMarketDataConnectorPublisher) this.ordinaryMarketDataConnectorPublisher;
            parquetMarketDataConnectorPublisher.register(marketDataConnectorPublisherListener);
        }

    }

    public void setSingleThread(boolean singleThread) {
        isSingleThread = singleThread;
        if (singleThread) {
            logger.info("setSingleThread Configuration");
            Configuration.BACKTEST_THREADS_PUBLISHING_ORDER_REQUEST = 0;////TODO change to more ms on PaperTradingEngine
            Configuration.BACKTEST_THREADS_PUBLISHING_MARKETDATA = 0;
            Configuration.BACKTEST_THREADS_PUBLISHING_EXECUTION_REPORTS = 0;
            Configuration.BACKTEST_THREADS_LISTENING_EXECUTION_REPORTS = 0;
            Configuration.BACKTEST_THREADS_LISTENING_ORDER_REQUEST = 0;
        }
    }

    @Override
    protected void constructPaperExecutionReportConnectorPublisher() {
        paperTradingEngine = new PaperTradingEngine(paperTradingEngineConnector, ordinaryMarketDataConnectorProvider,
                backtestOrderRequestProvider, tradingEngineConnectorConfiguration);
        paperTradingEngine.setDelayOrderRequestPoissonMs(Configuration.DELAY_ORDER_BACKTEST_MS);//setting delay 65 ms
        paperTradingEngine.setBacktest(true);

        paperTradingEngineConnector = getPaperTradingEngineConnector();

        paperTradingEngine.setTradingEngineConnector(paperTradingEngineConnector);
    }


    @Override
    protected void afterConstructor() {
        super.afterConstructor();

        //register rest of provides
        if (backtestMarketDataAndExecutionReportPublisher instanceof OrdinaryConnectorPublisherProvider) {
            OrdinaryConnectorPublisherProvider ordinaryConnectorPublisherProvider = (OrdinaryConnectorPublisherProvider) backtestMarketDataAndExecutionReportPublisher;

            if (paperTradingEngineConnector instanceof OrdinaryTradingEngine) {
                OrdinaryTradingEngine ordinaryTradingEngine = (OrdinaryTradingEngine) paperTradingEngineConnector;
                ordinaryConnectorPublisherProvider
                        .register(new OrdinaryConnectorConfiguration(), ordinaryTradingEngine);
            }

            if (algorithmMarketDataProvider instanceof OrdinaryMarketDataProvider) {
                OrdinaryMarketDataProvider ordinaryMarketDataProvider = (OrdinaryMarketDataProvider) algorithmMarketDataProvider;
                ordinaryConnectorPublisherProvider
                        .register(new OrdinaryConnectorConfiguration(), ordinaryMarketDataProvider);
            }
        }
        if (ordinaryBacktestRLGym != null) {
            ordinaryBacktestRLGym.init();
        }


    }

    @Override
    protected MarketDataProvider getAlgorithmMarketDataProvider() {
        //		if (ordinaryMarketDataConnectorProvider!=null){
        //			return ordinaryMarketDataConnectorProvider;
        //		}
        //		OrdinaryConnectorPublisherProvider ordinaryConnectorPublisherProvider = new OrdinaryConnectorPublisherProvider(
        //				"backtest_md_publisher", BACKTEST_THREADS_PUBLISHING_MARKET_DATA_FILE, Thread.MIN_PRIORITY);
        //		OrdinaryConnectorConfiguration ordinaryConnectorConfiguration = new OrdinaryConnectorConfiguration();
        //		MarketDataProvider marketDataProvider = new OrdinaryMarketDataProvider(ordinaryConnectorPublisherProvider,
        //				ordinaryConnectorConfiguration);

        MarketDataProvider marketDataProvider = paperTradingEngine.getMarketDataProviderIn();

        return marketDataProvider;
    }

    @Override
    protected TradingEngineConnector getPaperTradingEngineConnector() {
        return new OrdinaryTradingEngine((OrdinaryConnectorPublisherProvider) backtestOrderRequestProvider,
                paperTradingEngine, Configuration.BACKTEST_THREADS_PUBLISHING_ORDER_REQUEST,
                Configuration.BACKTEST_THREADS_LISTENING_EXECUTION_REPORTS, Thread.MAX_PRIORITY, Thread.NORM_PRIORITY);
    }

    @Override
    protected ConnectorProvider getBacktestOrderRequestProvider() {
        OrdinaryConnectorPublisherProvider ordinaryConnectorPublisherProvider = new OrdinaryConnectorPublisherProvider(
                "orderRequestConnectorProvider", Configuration.BACKTEST_THREADS_LISTENING_ORDER_REQUEST,
                Thread.NORM_PRIORITY);
        return ordinaryConnectorPublisherProvider;
    }

    protected void readFiles() {
        if (algorithmMarketDataProvider instanceof OrdinaryMarketDataProvider) {
            OrdinaryMarketDataProvider ordinaryMarketDataProvider = (OrdinaryMarketDataProvider) algorithmMarketDataProvider;
            ordinaryMarketDataProvider.init();
        }

        if (paperTradingEngineConnector instanceof OrdinaryTradingEngine) {
            OrdinaryTradingEngine ordinaryTradingEngine = (OrdinaryTradingEngine) paperTradingEngineConnector;
            //			ordinaryTradingEngine.();
        }

        if (ordinaryMarketDataConnectorPublisher instanceof CSVMarketDataConnectorPublisher) {
            CSVMarketDataConnectorPublisher csvMarketDataConnectorPublisher = (CSVMarketDataConnectorPublisher) this.ordinaryMarketDataConnectorPublisher;
            csvMarketDataConnectorPublisher.init();
        } else if (ordinaryMarketDataConnectorPublisher instanceof ParquetMarketDataConnectorPublisher) {
            ParquetMarketDataConnectorPublisher parquetMarketDataConnectorPublisher = (ParquetMarketDataConnectorPublisher) this.ordinaryMarketDataConnectorPublisher;
            parquetMarketDataConnectorPublisher.init();
        } else {
            logger.error(
                    "cant read files : ordinaryMarketDataConnectorPublisher in CSVZeroMqBacktest is not CSVMarketDataConnectorPublisher");
        }
    }

    @Override
    protected ConnectorConfiguration getMarketDataConnectorConfiguration() {
        return ordinaryConnectorConfiguration;
    }

    @Override
    protected ConnectorConfiguration getTradingEngineConnectorConfiguration() {
        return ordinaryConnectorConfiguration;
    }

    @Override
    protected ConnectorPublisher getBacktestMarketDataAndExecutionReportConnectorPublisher() {
        OrdinaryConnectorPublisherProvider ordinaryConnectorPublisherProvider = new OrdinaryConnectorPublisherProvider(
                "marketDataPublisherPublisherProvider", Configuration.BACKTEST_THREADS_PUBLISHING_MARKETDATA, Thread.MIN_PRIORITY);

        if (Configuration.BACKTEST_THREADS_PUBLISHING_EXECUTION_REPORTS != 0) {
            Map<TypeMessage, ThreadPoolExecutor> routingMap = new HashMap<>();

            //ER has max priority on threadpools
            ThreadPoolExecutor erThreadPoolExecutor = (ThreadPoolExecutor) Executors
                    .newFixedThreadPool(Configuration.BACKTEST_THREADS_PUBLISHING_EXECUTION_REPORTS);
            ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
            threadFactoryBuilder.setNameFormat("ExecutionReportPublisher -%d").build();
            threadFactoryBuilder.setPriority(Thread.MAX_PRIORITY);

            erThreadPoolExecutor.setThreadFactory(threadFactoryBuilder.build());
            //executionReports on a differentThreadPool
            routingMap.put(TypeMessage.execution_report, erThreadPoolExecutor);
            ordinaryConnectorPublisherProvider.setRoutingPool(routingMap);
        }

        return ordinaryConnectorPublisherProvider;
    }
}
