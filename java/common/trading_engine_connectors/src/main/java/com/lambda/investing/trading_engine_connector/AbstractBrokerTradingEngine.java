package com.lambda.investing.trading_engine_connector;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.portfolio.Portfolio;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.OrderRequestAction;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import static com.lambda.investing.PrintUtils.PrintDate;
import static com.lambda.investing.model.Util.*;
import static com.lambda.investing.model.portfolio.Portfolio.REQUESTED_PORTFOLIO_INFO;


public abstract class AbstractBrokerTradingEngine implements TradingEngineConnector, ConnectorListener {
    protected static long WARN_LATENCY_ORDER_REQUEST_MS = 500;
    protected static int QUEUE_SIZE = 300;
    protected static String REJECT_ORIG_NOT_FOUND_FORMAT = "origClientOrderId %s not found for %s in %s";//origClientOrderId , action,instrument
    protected Logger logger = LogManager.getLogger(AbstractBrokerTradingEngine.class);
    protected ConnectorProvider orderRequestConnectorProvider;
    protected ConnectorConfiguration orderRequestConnectorConfiguration;

    protected ConnectorPublisher executionReportConnectorPublisher;
    protected ConnectorConfiguration executionReportConnectorConfiguration;

    protected Map<String, Map<ExecutionReportListener, String>> listenersManager;

    protected Portfolio portfolio;
    protected Queue<String> lastOrderRequestClOrdId;
    protected Queue<String> CfERNotified;

    public AbstractBrokerTradingEngine(ConnectorConfiguration orderRequestConnectorConfiguration,
                                       ConnectorProvider orderRequestConnectorProvider,
                                       ConnectorConfiguration executionReportConnectorConfiguration,
                                       ConnectorPublisher executionReportConnectorPublisher) {
        this.orderRequestConnectorConfiguration = orderRequestConnectorConfiguration;
        this.orderRequestConnectorProvider = orderRequestConnectorProvider;
        this.executionReportConnectorConfiguration = executionReportConnectorConfiguration;
        this.executionReportConnectorPublisher = executionReportConnectorPublisher;
        portfolio = new Portfolio();//from file
        listenersManager = new ConcurrentHashMap<>();
        lastOrderRequestClOrdId = EvictingQueue.create(QUEUE_SIZE);
        CfERNotified = EvictingQueue.create(QUEUE_SIZE);
    }

    @Override
    public boolean isBusy() {
        return false;
    }


    public void reset() {
    }

    public abstract void setDemoTrading();

    public void start() {
        //listening orderRequest
        this.orderRequestConnectorProvider.register(this.orderRequestConnectorConfiguration, this);

    }

    @Override
    public void register(String algorithmInfo, ExecutionReportListener executionReportListener) {
        //no sense on broker that are going to send the ER to connector publisher
    }

    @Override
    public void deregister(String id, ExecutionReportListener executionReportListener) {
        //no sense on broker that are going to send the ER to connector publisher
    }

    protected ExecutionReport createRejectionExecutionReport(OrderRequest orderRequest, String reason) {
        ExecutionReport executionReport = new ExecutionReport(orderRequest);
        if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {
            executionReport.setExecutionReportStatus(ExecutionReportStatus.CancelRejected);
        } else {

            executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
        }
        executionReport.setRejectReason(reason);
        return executionReport;
    }


    //called by extension when filled /partial filled
    @Override
    public void notifyExecutionReport(ExecutionReport executionReport) {
        boolean isKindOfFilled = ExecutionReport.isTradeStatus(executionReport);
        if (isKindOfFilled && CfERNotified.contains(executionReport.getClientOrderId())) {
            //already notified!
            logger.info("already notify Cf {} -> skip it", executionReport.getClientOrderId());
            return;
        }

        String id = executionReport.getAlgorithmInfo() + "." + TypeMessage.execution_report;

        if (executionReport.getExecutionReportStatus() == ExecutionReportStatus.CompletelyFilled) {
            CfERNotified.offer(executionReport.getClientOrderId());
        }

        logger.info("notifyExecutionReportById {} : {} ", id, executionReport);
        executionReport.setTimestampBrokerConnector(System.currentTimeMillis());//when broker notify
        this.executionReportConnectorPublisher
                .publish(executionReportConnectorConfiguration, TypeMessage.execution_report, id, executionReport);

    }

    //receiving OrderRequest
    @Override
    public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
                         TypeMessage typeMessage, Object content) {

        if (typeMessage.equals(TypeMessage.order_request)) {
//			OrderRequest orderRequest = fromJsonString(content, OrderRequest.class);
            OrderRequest orderRequest = fromObject(content, OrderRequest.class);
            orderRequest.setTimestampBrokerConnector(System.currentTimeMillis());

            if (lastOrderRequestClOrdId.contains(orderRequest.getClientOrderId())) {
                //
                logger.warn("order already processed {}-> reject", orderRequest.getClientOrderId());
                return;
            } else {
                lastOrderRequestClOrdId.offer(orderRequest.getClientOrderId());
            }

            System.out.println(Configuration.formatLog("onUpdate.orderRequest : {}", orderRequest));
            logger.info("onUpdate.orderRequest : {}", orderRequest);
            orderRequest.setTimestampBrokerConnector(System.currentTimeMillis());
            long latencyMs = System.currentTimeMillis() - orderRequest.getTimestampCreation();
            if (latencyMs > WARN_LATENCY_ORDER_REQUEST_MS) {
                String table = orderRequest.getLatenciesTable();
                logger.warn("OrderRequest {} with latency {} ms > {} from orderRequest from {} to {}\n{}", orderRequest, latencyMs, WARN_LATENCY_ORDER_REQUEST_MS, PrintDate(new Date(orderRequest.getTimestampCreation())), PrintDate(new Date()), table);
            }

            orderRequest(orderRequest);
        }

        if (typeMessage.equals(TypeMessage.execution_report)) {
//			ExecutionReport executionReport = fromJsonString(content, ExecutionReport.class);
            ExecutionReport executionReport = fromObject(content, ExecutionReport.class);

            System.out.println(Configuration.formatLog("onUpdate.execution_report  {}", executionReport));
            logger.info("onUpdate.execution_report  {}", executionReport);

            //but here is for brokers only----> not so much sense
            portfolio.updateTrade(executionReport);

            //			if (isPaperTrading && paperTradingEngine != null) {
            //				this.paperTradingEngine.notifyExecutionReport(executionReport);
            //			}
        }

        if (typeMessage.equals(TypeMessage.info)) {
            logger.info("onUpdate.info  {}", content);
            requestInfo(fromObject(content, String.class));
        }

    }

    protected void notifyInfo(String topic, String message) {
        logger.info("notifyInfo {} : {} ", topic, message);
        this.executionReportConnectorPublisher
                .publish(executionReportConnectorConfiguration, TypeMessage.info, TypeMessage.info.toString(), message);
    }

    @Override
    public void requestInfo(String info) {
        //algorithm.info
        logger.info("requestInfo: {} ", info);
        if (info.endsWith(REQUESTED_PORTFOLIO_INFO)) {
            //return portfolio on execution Report
            notifyInfo(info, toJsonString(portfolio));
        }
    }
}
