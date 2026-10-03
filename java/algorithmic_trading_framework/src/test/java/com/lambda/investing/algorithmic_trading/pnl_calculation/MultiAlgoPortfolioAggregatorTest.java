package com.lambda.investing.algorithmic_trading.pnl_calculation;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Regression test: {@link PnlSnapshot#clone()} used to drop the trade counters, so the aggregated
 * snapshot broadcast to the web dashboard always reported 0 trades / 0 aggressor trades.
 */
public class MultiAlgoPortfolioAggregatorTest {

    private static final String INSTRUMENT_PK = "btceur_kraken";

    private static PnlSnapshot snapshot(int trades, int aggressor, int aggressed) {
        PnlSnapshot s = new PnlSnapshot(INSTRUMENT_PK);
        s.numberOfTrades.set(trades);
        s.numberOfAggressorTrades.set(aggressor);
        s.numberOfAggressedTrades.set(aggressed);
        s.netPosition = 1.0;
        return s;
    }

    private static PortfolioSnapshot portfolio(String algo, PnlSnapshot s) {
        Map<String, PnlSnapshot> map = new HashMap<>();
        map.put(INSTRUMENT_PK, s);
        return new PortfolioSnapshot(algo, map);
    }

    @Test
    public void singleAlgoKeepsTradeCounters() {
        MultiAlgoPortfolioAggregator aggregator = new MultiAlgoPortfolioAggregator();
        aggregator.update("algo1", portfolio("algo1", snapshot(5, 3, 2)));

        PnlSnapshot agg = aggregator.getAggregatedPortfolioSnapshot().getInstrumentPnlSnapshotMap().get(INSTRUMENT_PK);
        Assert.assertEquals(5, agg.numberOfTrades.get());
        Assert.assertEquals(3, agg.numberOfAggressorTrades.get());
        Assert.assertEquals(2, agg.numberOfAggressedTrades.get());
    }

    @Test
    public void multiAlgoSumsTradeCountersWithoutMutatingSources() {
        MultiAlgoPortfolioAggregator aggregator = new MultiAlgoPortfolioAggregator();
        PnlSnapshot first = snapshot(5, 3, 2);
        PnlSnapshot second = snapshot(4, 1, 3);
        aggregator.update("algo1", portfolio("algo1", first));
        aggregator.update("algo2", portfolio("algo2", second));

        PnlSnapshot agg = aggregator.getAggregatedPortfolioSnapshot().getInstrumentPnlSnapshotMap().get(INSTRUMENT_PK);
        Assert.assertEquals(9, agg.numberOfTrades.get());
        Assert.assertEquals(4, agg.numberOfAggressorTrades.get());
        Assert.assertEquals(5, agg.numberOfAggressedTrades.get());

        // calling twice must not double count (clone must not share AtomicIntegers with the source)
        PnlSnapshot agg2 = aggregator.getAggregatedPortfolioSnapshot().getInstrumentPnlSnapshotMap().get(INSTRUMENT_PK);
        Assert.assertEquals(9, agg2.numberOfTrades.get());
        Assert.assertEquals(5, first.numberOfTrades.get());
        Assert.assertEquals(4, second.numberOfTrades.get());
    }
}
