package com.lambda.investing.algorithmic_trading.tca;

/**
 * Order side: BUY lifts the offer; SELL hits the bid.
 */
public enum Side {
    BUY,
    SELL;

    /**
     * Returns {@code +1} for BUY and {@code -1} for SELL.
     *
     * <p>This sign convention ensures that a positive metric value always represents a cost
     * (unfavourable outcome) for the aggressor.
     *
     * @return side sign
     */
    public int sign() {
        return this == BUY ? 1 : -1;
    }
}
