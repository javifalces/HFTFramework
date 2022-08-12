package com.lambda.investing.trading_engine_connector;

import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.market_data_connector.MarketDataConnectorPublisher;
import com.lambda.investing.model.portfolio.Portfolio;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;

/**
 * Interface than send the orders and is going to notify Execution reports to ExecutionReportListener
 */
public interface TradingEngineConnector {

    void register(String id, ExecutionReportListener executionReportListener);

    void deregister(String id, ExecutionReportListener executionReportListener);

    boolean orderRequest(OrderRequest orderRequest);

    void requestInfo(String info);

    void reset();

}
