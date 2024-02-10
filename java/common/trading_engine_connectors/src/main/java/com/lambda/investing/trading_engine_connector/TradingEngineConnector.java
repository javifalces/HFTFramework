package com.lambda.investing.trading_engine_connector;

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

    boolean isBusy();

}
