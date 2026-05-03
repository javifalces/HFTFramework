package com.lambda.investing.algorithmic_trading.tca;

import java.util.Collections;
import java.util.List;

/**
 * A snapshot of market data (price, volume, timestamps) used to compute VWAP / TWAP benchmarks.
 *
 * <p>All three lists must have the same size and be ordered chronologically.
 * Timestamps are epoch milliseconds (UTC).
 */
public class MarketDataSnapshot {

    private final List<Long> timestampsMs;
    private final List<Double> prices;
    private final List<Long> volumes;

    /**
     * Creates a new {@code MarketDataSnapshot}.
     *
     * @param timestampsMs epoch-millis timestamps (chronologically ordered, non-null)
     * @param prices       corresponding mid/trade prices (non-null, same size as {@code timestampsMs})
     * @param volumes      corresponding traded volumes (non-null, same size as {@code timestampsMs})
     * @throws IllegalArgumentException when the lists have different sizes or are empty
     */
    public MarketDataSnapshot(List<Long> timestampsMs, List<Double> prices, List<Long> volumes) {
        if (timestampsMs == null || prices == null || volumes == null) {
            throw new IllegalArgumentException("MarketDataSnapshot lists must not be null");
        }
        if (timestampsMs.size() != prices.size() || prices.size() != volumes.size()) {
            throw new IllegalArgumentException(
                    "MarketDataSnapshot lists must all have the same size");
        }
        this.timestampsMs = Collections.unmodifiableList(timestampsMs);
        this.prices = Collections.unmodifiableList(prices);
        this.volumes = Collections.unmodifiableList(volumes);
    }

    /** Returns the list of epoch-millis timestamps. */
    public List<Long> getTimestampsMs() {
        return timestampsMs;
    }

    /** Returns the list of prices corresponding to each timestamp. */
    public List<Double> getPrices() {
        return prices;
    }

    /** Returns the list of volumes corresponding to each timestamp. */
    public List<Long> getVolumes() {
        return volumes;
    }

    /** Returns the number of data points in this snapshot. */
    public int size() {
        return timestampsMs.size();
    }
}
