package com.lambda.investing.algorithmic_trading.gui.algorithm;

import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;

import java.util.Map;

public interface AlgorithmGui {
    void updateDepth(Depth depth);

    void updatePnlSnapshot(PnlSnapshot pnlSnapshot);

    void updateTrade(Trade trade);

    void updateParams(Map<String, Object> newParams);

    void updateMessage(String name, String body);

    void updateOrderRequest(OrderRequest orderRequest);

    void updateExecutionReport(ExecutionReport executionReport);

    void updateCustomColumn(long timestamp, String instrumentPk, String key, Double value);
}
