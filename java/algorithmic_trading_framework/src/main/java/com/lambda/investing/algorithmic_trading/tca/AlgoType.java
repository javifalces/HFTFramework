package com.lambda.investing.algorithmic_trading.tca;

/**
 * Execution algorithm type used when submitting a parent order.
 */
public enum AlgoType {
    /** Time-Weighted Average Price execution. */
    TWAP,
    /** Volume-Weighted Average Price execution. */
    VWAP,
    /** Percentage of Volume execution. */
    POV,
    /** Implementation Shortfall execution. */
    IS
}
