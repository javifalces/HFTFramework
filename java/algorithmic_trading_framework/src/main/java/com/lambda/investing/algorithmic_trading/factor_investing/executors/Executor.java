package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.model.trading.Verb;

/**
 * Created by Javi
 * Executor interface to be implemented by all the executors per instrument
 */
public interface Executor {
    boolean increasePosition(long timestamp, Verb verb, double quantity, double price);

    boolean isExecuting();

    /**
     * Wires the owning {@link Algorithm} so order requests sent directly to the trading engine by this
     * executor get registered with it (see {@link Algorithm#registerSentOrderRequest(com.lambda.investing.model.trading.OrderRequest)}),
     * otherwise the algorithm's {@code onExecutionReportUpdate} won't recognize the resulting fills and
     * portfolio/pnl tracking (netInvestment, realizedPnl, ...) stays at 0.0.
     *
     * @param algorithm the algorithm that created/owns this executor, may be {@code null} in tests
     */
    void setAlgorithm(Algorithm algorithm);

}
