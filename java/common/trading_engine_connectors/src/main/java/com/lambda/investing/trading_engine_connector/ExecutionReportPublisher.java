package com.lambda.investing.trading_engine_connector;

import com.lambda.investing.model.trading.ExecutionReport;

public interface ExecutionReportPublisher {

	void notifyExecutionReport(ExecutionReport executionReport);

}
