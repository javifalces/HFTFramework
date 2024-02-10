package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.model.trading.Verb;

/**
 * Created by Javi
 * Executor interface to be implemented by all the executors per instrument
 */
public interface Executor {
    boolean increasePosition(long timestamp, Verb verb, double quantity, double price);

    boolean isExecuting();

}
