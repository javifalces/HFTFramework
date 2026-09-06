package com.lambda.investing.trading_engine_connector.ordinary;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorPublisherProvider;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.market_data_connector.ZeroMqMarketDataConnector;
import com.lambda.investing.market_data_connector.ordinary.OrdinaryMarketDataProvider;
import com.lambda.investing.model.messaging.TopicUtils;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.trading_engine_connector.AbstractTradingEngineConnector;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import com.lambda.investing.trading_engine_connector.paper.PaperConnectorPublisher;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process (same JVM) counterpart of {@link com.lambda.investing.trading_engine_connector.ZeroMqTradingEngineConnector}.
 * <p>
 * Instead of publishing {@link OrderRequest}s / listening for {@link com.lambda.investing.model.trading.ExecutionReport}s
 * over ZeroMQ sockets, it talks directly to an {@link OrdinaryConnectorPublisherProvider} shared with a broker engine
 * (e.g. {@link com.lambda.investing.trading_engine_connector.xchange.XChangeTradingEngine}) running in the same
 * process: order requests are published on the "order request" provider/configuration pair (the broker engine is
 * registered as a listener on it), and execution reports are received by registering this connector as a listener on
 * the "execution report" provider/configuration pair (the broker engine publishes on it).
 */
public class OrdinaryTradingEngineConnector extends AbstractTradingEngineConnector {

    private final OrdinaryConnectorPublisherProvider orderRequestConnectorPublisherProvider;
    private final ConnectorConfiguration orderRequestConnectorConfiguration;

    private final OrdinaryConnectorPublisherProvider executionReportConnectorPublisherProvider;
    private final ConnectorConfiguration executionReportConnectorConfiguration;

    /***
     * In-process trading engine connector for generic brokers wired via Ordinary connectors.
     *
     * @param name                                        name of this connector
     * @param orderRequestConnectorPublisherProvider       provider used to publish {@link OrderRequest}s; the broker
     *                                                     engine must be registered as a listener on it
     * @param orderRequestConnectorConfiguration           configuration identifying the order-request channel
     * @param executionReportConnectorPublisherProvider    provider the broker engine publishes execution reports on;
     *                                                     this connector registers itself as a listener on it
     * @param executionReportConnectorConfiguration        configuration identifying the execution-report channel
     */
    public OrdinaryTradingEngineConnector(String name,
                                          OrdinaryConnectorPublisherProvider orderRequestConnectorPublisherProvider,
                                          ConnectorConfiguration orderRequestConnectorConfiguration,
                                          OrdinaryConnectorPublisherProvider executionReportConnectorPublisherProvider,
                                          ConnectorConfiguration executionReportConnectorConfiguration) {
        super(name);
        this.orderRequestConnectorPublisherProvider = orderRequestConnectorPublisherProvider;
        this.orderRequestConnectorConfiguration = orderRequestConnectorConfiguration;
        this.executionReportConnectorPublisherProvider = executionReportConnectorPublisherProvider;
        this.executionReportConnectorConfiguration = executionReportConnectorConfiguration;
    }

    public void start() {
        executionReportConnectorPublisherProvider.register(executionReportConnectorConfiguration, this);
        logger.info("Listening ExecutionReports in-process on {}", executionReportConnectorConfiguration);
        logger.info("Publishing OrderRequests in-process on {}", orderRequestConnectorConfiguration);
    }

    @Override
    public void register(String algorithmInfo, ExecutionReportListener executionReportListener) {
        Map<ExecutionReportListener, String> insideMap = listenersManager
                .getOrDefault(algorithmInfo, new ConcurrentHashMap<>());
        insideMap.put(executionReportListener, "");
        listenersManager.put(algorithmInfo, insideMap);
    }

    @Override
    public boolean orderRequest(OrderRequest orderRequest) {
        if (isPaperTrading && paperTradingEngine != null) {
            return this.paperTradingEngine.orderRequest(orderRequest);
        }
        String topic = TopicUtils.getTopic(orderRequest.getInstrument(), TypeMessage.order_request);
        return this.orderRequestConnectorPublisherProvider
                .publish(orderRequestConnectorConfiguration, TypeMessage.order_request, topic, orderRequest);
    }

    @Override
    public void requestInfo(String info) {
        if (isPaperTrading && paperTradingEngine != null) {
            this.paperTradingEngine.requestInfo(info);//simulate portfolio and position
            return;
        }
        this.orderRequestConnectorPublisherProvider
                .publish(orderRequestConnectorConfiguration, TypeMessage.info, TypeMessage.info.toString(), info);
    }

    @Override
    public void setPaperTrading(MarketDataProvider marketDataProvider) {
        logger.info("#### PAPER TRADING {}", this.name);
        if (!(marketDataProvider instanceof OrdinaryMarketDataProvider) && !(marketDataProvider instanceof ZeroMqMarketDataConnector)) {
            logger.error(
                    "can't be paper trading on other type of MarketDataProvider as ZeroMqMarketDataConnector or OrdinaryMarketDataProvider");
            return;
        }

        paperTradingEngine = new PaperTradingEngine(this, marketDataProvider,
                orderRequestConnectorPublisherProvider, orderRequestConnectorConfiguration);
        paperTradingEngine.setPaperTrading(true);
        paperTradingEngine.setBacktest(false);

        PaperConnectorPublisher paperConnectorPublisher = new PaperConnectorPublisher(
                executionReportConnectorConfiguration, executionReportConnectorPublisherProvider);
        paperTradingEngine.setPaperConnectorMarketDataAndExecutionReportPublisher(paperConnectorPublisher);

        this.isPaperTrading = true;

        if (this.instrumentList != null) {
            initPaperTrading();
        }
    }

}
