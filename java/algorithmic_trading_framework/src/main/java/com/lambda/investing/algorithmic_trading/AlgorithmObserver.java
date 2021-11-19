package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;

import java.util.Map;

public interface AlgorithmObserver {

	void onUpdateDepth(String algorithmInfo, Depth depth);

	void onUpdateTrade(String algorithmInfo, PnlSnapshot pnlSnapshot);

	void onUpdateClose(String algorithmInfo, Trade trade);

	void onUpdateParams(String algorithmInfo, Map<String, Object> newParams);

	void onUpdateMessage(String algorithmInfo, String name, String body);

	void onOrderRequest(String algorithmInfo, OrderRequest orderRequest);

	void onExecutionReportUpdate(String algorithmInfo, ExecutionReport executionReport);
}
