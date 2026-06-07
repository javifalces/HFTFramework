package com.lambda.investing.algorithmic_trading.pnl_calculation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains a per-algorithm registry of {@link PortfolioSnapshot} objects and provides
 * an aggregated, per-instrument view by summing PnL contributions across every tracked
 * algorithm.
 *
 * <p>Usage:
 * <pre>{@code
 * // On each PORTFOLIO_SNAPSHOT event:
 * aggregator.update(algorithmInfo, portfolioSnapshot);
 *
 * // Retrieve the complete per-instrument aggregate (all algorithms summed):
 * Map<String, Map<String, Object>> byInstr = aggregator.getAggregatedByInstrument();
 * }</pre>
 *
 * <p>When a new snapshot arrives for an already-known algorithm the old entry is replaced,
 * and the aggregate is recomputed so that the result is always consistent with the latest
 * snapshots.
 *
 * <p>Thread-safe: all mutation methods are {@code synchronized}; the read-only
 * {@link #getPortfolioByAlgo()} returns an unmodifiable view.
 */
public class MultiAlgoPortfolioAggregator {

    /**
     * Latest portfolio snapshot per algorithm.
     * Key is {@code algorithmInfo}; the empty string {@code ""} is used for unnamed algorithms.
     */
    private final Map<String, PortfolioSnapshot> portfolioByAlgo = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Mutation
    // -----------------------------------------------------------------------

    /**
     * Registers or replaces the portfolio snapshot for the given algorithm.
     *
     * @param algorithmInfo algorithm identifier; {@code null} is treated as {@code ""}
     * @param snapshot      the latest snapshot emitted by that algorithm (must not be {@code null})
     */
    public synchronized void update(String algorithmInfo, PortfolioSnapshot snapshot) {
        if (snapshot == null) return;
        String key = algorithmInfo != null ? algorithmInfo : "";
        portfolioByAlgo.put(key, snapshot);
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    /**
     * Returns an unmodifiable view of the per-algorithm portfolio map.
     * Useful for serialisation or inspection by the caller.
     */
    public Map<String, PortfolioSnapshot> getPortfolioByAlgo() {
        return Collections.unmodifiableMap(portfolioByAlgo);
    }


    /**
     * Builds and returns a complete aggregated {@link PortfolioSnapshot} object that sums
     * all PnL data across every algorithm and every instrument.  The returned snapshot
     * behaves like a single-algorithm portfolio but with data that reflects all known
     * algorithms combined.
     *
     * <p>The construction process:
     * <ol>
     *   <li>Creates synthetic {@link PnlSnapshot} objects for each instrument, with summed
     *       PnL fields ({@code realizedPnl}, {@code unrealizedPnl}, {@code totalPnl},
     *       {@code netPosition}, {@code totalFees}, {@code netInvestment})</li>
     *   <li>Wraps these in a new {@link PortfolioSnapshot} with {@code algorithmInfo = null}
     *       (or a marker to indicate this is a cross-algorithm view)</li>
     *   <li>The portfolio-level totals are automatically computed from the per-instrument
     *       snapshots during construction</li>
     * </ol>
     *
     * @return a new {@link PortfolioSnapshot} containing aggregated data from all algorithms,
     * or an empty snapshot if no algorithms have registered yet
     */
    public synchronized PortfolioSnapshot getAggregatedPortfolioSnapshot() {
        Map<String, PnlSnapshot> aggregatedInstruments = new LinkedHashMap<>();

        for (PortfolioSnapshot ps : portfolioByAlgo.values()) {
            if (ps.getInstrumentPnlSnapshotMap() == null) continue;

            for (Map.Entry<String, PnlSnapshot> entry : ps.getInstrumentPnlSnapshotMap().entrySet()) {
                String instr = entry.getKey();
                PnlSnapshot s = entry.getValue();
                if (s == null) continue;

                // Create or retrieve the aggregated snapshot for this instrument
                PnlSnapshot agg = aggregatedInstruments.computeIfAbsent(instr, k -> new PnlSnapshot(k));

                // Sum the PnL scalar fields
                agg.realizedPnl += s.realizedPnl;
                agg.unrealizedPnl += s.unrealizedPnl;
                agg.totalPnl += s.totalPnl;
                agg.netPosition += s.netPosition;
                agg.totalFees += s.totalFees;
                agg.netInvestment += s.netInvestment;
                agg.realizedFees += s.realizedFees;
                agg.unrealizedFees += s.unrealizedFees;

                agg.numberOfAggressedTrades.set(agg.numberOfAggressedTrades.addAndGet(s.numberOfAggressedTrades.get()));
                agg.numberOfAggressorTrades.set(agg.numberOfAggressorTrades.addAndGet(s.numberOfAggressorTrades.get()));
                agg.numberOfTrades.set(agg.numberOfTrades.addAndGet(s.numberOfTrades.get()));

                // Track the maximum timestamp across all algorithms for the same instrument
                long aggTs = agg.getLastTimestampUpdate();
                long sTs = s.getLastTimestampUpdate();
                if (sTs > aggTs) {
                    agg.setLastTimestampUpdate(sTs);
                }
            }
        }

        // Create and return the aggregated PortfolioSnapshot
        // Use null algorithmInfo to indicate this is a synthetic cross-algorithm view
        return new PortfolioSnapshot(null, aggregatedInstruments);
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} when no algorithm has registered a snapshot yet.
     */
    public boolean isEmpty() {
        return portfolioByAlgo.isEmpty();
    }

    /**
     * Returns the number of algorithms currently tracked.
     */
    public int size() {
        return portfolioByAlgo.size();
    }
}

