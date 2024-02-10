package com.lambda.investing.trading_engine_connector;

import com.lambda.investing.model.trading.ExecutionReport;

public interface ExecutionReportListener {

    boolean onExecutionReportUpdate(ExecutionReport executionReport);

    boolean onInfoUpdate(String header, String message);

}
