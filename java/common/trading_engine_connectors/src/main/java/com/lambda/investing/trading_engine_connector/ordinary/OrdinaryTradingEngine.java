package com.lambda.investing.trading_engine_connector.ordinary;


import com.lambda.investing.Configuration;
import com.lambda.investing.LambdaThreadFactory;
import com.lambda.investing.connector.AbstractConnectorPublisherConfiguration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.market_data_connector.MarketDataConnectorPublisherListener;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;


import static com.lambda.investing.model.Util.fromObject;
import static net.openhft.affinity.AffinityStrategies.*;

import java.util.Map;
import java.util.concurrent.*;

import static com.lambda.investing.Configuration.logger;
import static com.lambda.investing.model.Util.fromJsonString;
import static com.lambda.investing.trading_engine_connector.AbstractTradingEngineConnector.ALL_ALGORITHMS_SUBSCRIPTION;

public class OrdinaryTradingEngine implements TradingEngineConnector, ConnectorListener {
    public static long DEFAULT_TIMEOUT_TERMINATION_POOL_MS = 60000;
    private ConnectorProvider executionReportOrderRequestConnectorProvider;
    private PaperTradingEngine paperTradingEngineConnector;

    private AbstractConnectorPublisherConfiguration ordinaryConnectorConfiguration = new OrdinaryConnectorConfiguration();
    protected Map<String, Map<ExecutionReportListener, String>> listenersManager;

    private boolean killSenderPool = false, killReceiverPool = false;

    private int threadsSendOrderRequest, threadsListeningExecutionReports;

    ThreadFactory namedThreadFactoryOrderRequest = LambdaThreadFactory.createThreadFactory("OrdinaryTradingEngine-OrderRequest");

    ThreadFactory namedThreadFactoryExecutionReport = LambdaThreadFactory.createThreadFactory("OrdinaryTradingEngine-ExecutionReport");

    ThreadPoolExecutor senderPool, receiverPool;


    public OrdinaryTradingEngine(ConnectorProvider executionReportOrderRequestConnectorProvider,
                                 PaperTradingEngine paperTradingEngineConnector, int threadsSendOrderRequest,
                                 int threadsListeningExecutionReports, int priorityOrderRequest, int priorityExecutionReport) {
        this.executionReportOrderRequestConnectorProvider = executionReportOrderRequestConnectorProvider;
        this.paperTradingEngineConnector = paperTradingEngineConnector;
        listenersManager = new ConcurrentHashMap<>();

        this.threadsSendOrderRequest = threadsSendOrderRequest;
        initSenderPool();

        this.threadsListeningExecutionReports = threadsListeningExecutionReports;
        initReceiverPool();

    }

    public boolean isBusy() {
        boolean senderBusy = senderPool != null && senderPool.getQueue().size() > Configuration.BACKTEST_BUSY_THREADPOOL_TRESHOLD;
        boolean receiverBusy = receiverPool != null && receiverPool.getQueue().size() > Configuration.BACKTEST_BUSY_THREADPOOL_TRESHOLD;
        return senderBusy || receiverBusy;
    }


    private void initSenderPool() {
        if (this.threadsSendOrderRequest > 0) {
            senderPool = (ThreadPoolExecutor) Executors
                    .newFixedThreadPool(this.threadsSendOrderRequest, namedThreadFactoryOrderRequest);
        }
        if (this.threadsSendOrderRequest < 0) {
            senderPool = (ThreadPoolExecutor) Executors.newCachedThreadPool(namedThreadFactoryOrderRequest);
        }
    }

    private void initReceiverPool() {
        if (this.threadsListeningExecutionReports > 0) {
            receiverPool = (ThreadPoolExecutor) Executors
                    .newFixedThreadPool(this.threadsListeningExecutionReports, namedThreadFactoryExecutionReport);
        }

    }

    @Override
    public void register(String algorithmInfo, ExecutionReportListener executionReportListener) {
        Map<ExecutionReportListener, String> insideMap = listenersManager
                .getOrDefault(algorithmInfo, new ConcurrentHashMap<>());
        insideMap.put(executionReportListener, "");
        listenersManager.put(algorithmInfo, insideMap);
    }

    @Override
    public void deregister(String id, ExecutionReportListener executionReportListener) {
        this.executionReportOrderRequestConnectorProvider.deregister(ordinaryConnectorConfiguration, this);
    }

    @Override
    public boolean orderRequest(OrderRequest orderRequest) {
        orderRequest.setTimestampAlgoConnector(System.currentTimeMillis());
        if (this.threadsSendOrderRequest == 0) {
            return paperTradingEngineConnector.orderRequest(orderRequest);
        } else {
            senderPool.submit(() -> {
                if (killSenderPool) {
                    return;
                }
                paperTradingEngineConnector.orderRequest(orderRequest);
            });

            return true;
        }
    }

    @Override
    public void requestInfo(String info) {
        if (this.threadsSendOrderRequest == 0) {
            paperTradingEngineConnector.requestInfo(info);
        } else {
            senderPool.submit(() -> {
                if (killSenderPool) {
                    return;
                }
                paperTradingEngineConnector.requestInfo(info);
            });
        }
    }

    @Override
    public void reset() {

        if (senderPool != null) {
            try {
                killSenderPool = true;
                senderPool.shutdown();
                senderPool.awaitTermination(DEFAULT_TIMEOUT_TERMINATION_POOL_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.error("timeout waiting finished senderPool :{}", senderPool.toString());
            } finally {
                killSenderPool = false;
                initSenderPool();
            }
        }
        if (receiverPool != null) {
            try {
                killReceiverPool = true;
                receiverPool.shutdown();
                receiverPool.awaitTermination(DEFAULT_TIMEOUT_TERMINATION_POOL_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.error("timeout waiting finished receiverPool: {}", receiverPool.toString());
            } finally {
                killReceiverPool = false;
                initReceiverPool();
            }
        }
        paperTradingEngineConnector.reset();
    }

    @Override
    public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
                         TypeMessage typeMessage, Object content) {
        if (this.threadsListeningExecutionReports == 0) {
            _onUpdate(configuration, timestampReceived, typeMessage, content);
        } else {
            receiverPool.submit(() -> {
                if (killReceiverPool) {
                    return;
                }
                _onUpdate(configuration, timestampReceived, typeMessage, content);
            });

            if (receiverPool.getCorePoolSize() > 10) {
                logger.warn("receiverPool {} corePoolSize = {} > 10", receiverPool.toString(), receiverPool.getPoolSize());
            }

        }

    }

    public void notifyExecutionReport(ExecutionReport executionReport) {
        String algorithmInfo = executionReport.getAlgorithmInfo();
        Map<ExecutionReportListener, String> insideMap = listenersManager.getOrDefault(algorithmInfo, new ConcurrentHashMap<>());
        if (insideMap.size() > 0) {
            for (ExecutionReportListener executionReportListener : insideMap.keySet()) {
                executionReportListener.onExecutionReportUpdate(executionReport);
            }
        }
    }

    private void _onUpdate(ConnectorConfiguration configuration, long timestampReceived, TypeMessage typeMessage, Object content) {

        if (typeMessage == null) {
            logger.warn("_onUpdate received null typeMessage, ignoring message content={}", content);
            return;
        }

        if (typeMessage.equals(TypeMessage.execution_report)) {
            ExecutionReport executionReport = fromObject(content, ExecutionReport.class);
            executionReport.setTimestampAlgoConnector(System.currentTimeMillis());
            notifyExecutionReport(executionReport);
        }

        if (typeMessage.equals(TypeMessage.info)) {
            //TODO something with this info message
        }


    }


}
