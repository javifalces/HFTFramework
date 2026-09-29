package com.lambda.investing.algorithmic_trading.pnl_calculation;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.OrderType;
import com.lambda.investing.model.trading.Verb;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

/**
 * Regression test for the "pnl never updates in PortfolioManager.getPortfolioSnapshot()" bug:
 * {@link PortfolioManager} used to build its aggregate {@link PortfolioSnapshot} exactly once, in
 * {@link PortfolioManager#reset()}, while {@code instrumentPnlSnapshotMap} was still empty. Every
 * later call to {@link PortfolioManager#getPortfolioSnapshot()} kept returning that same stale,
 * all-zero snapshot forever, even though the individual {@link PnlSnapshot}s were being updated
 * correctly on every depth/trade. This silently broke factor-update pnl logging and RL reward
 * scoring (both read {@code getPortfolioSnapshot()}).
 */
public class PortfolioManagerPnlSnapshotTest {

    private static final String INSTRUMENT_PK = "btceur_kraken";
    private static final String ALGO_INFO = "testPortfolioManagerPnl";

    private static class DummyAlgorithm extends Algorithm {
        DummyAlgorithm(String algorithmInfo) {
            super(algorithmInfo, new HashMap<>());
        }

        @Override
        public void init() {
            //no-op: avoid the heavyweight default wiring, only PortfolioManager is needed for this test
        }

        @Override
        public boolean onDepthUpdate(Depth depth) {
            return true;
        }

        @Override
        public boolean onTradeUpdate(Trade trade) {
            return true;
        }

        @Override
        public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
            return true;
        }

        @Override
        public String printAlgo() {
            return getAlgorithmInfo();
        }
    }

    private Depth createDepth(double bestBid, double bestAsk, long timestampMs) {
        //PnlSnapshotOrders.updateOpenPosition() only recalculates the mark-to-market price when the
        //depth reports more than one level, so real (non single-level) depths are used here, matching
        //how PnlSnapshotTest#createDepth builds its fixtures.
        Depth depth = Depth.getInstance();
        depth.setInstrument(INSTRUMENT_PK);
        depth.setTimestamp(timestampMs);
        depth.setBids(new double[]{bestBid, bestBid - 0.01});
        depth.setAsks(new double[]{bestAsk, bestAsk + 0.01});
        depth.setBidsQuantities(new double[]{1.0, 1.0});
        depth.setAsksQuantities(new double[]{1.0, 1.0});
        depth.setLevelsFromData();
        return depth;
    }

    private ExecutionReport createFilledBuy(double price, double quantity, long timestampMs) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setOrderType(OrderType.Limit);
        orderRequest.setVerb(Verb.Buy);
        orderRequest.setPrice(price);
        orderRequest.setQuantity(quantity);
        orderRequest.setInstrument(INSTRUMENT_PK);
        orderRequest.setAlgorithmInfo(ALGO_INFO);
        orderRequest.setClientOrderId(UUID.randomUUID().toString());

        ExecutionReport executionReport = new ExecutionReport(orderRequest);
        executionReport.setLastQuantity(quantity);
        executionReport.setTimestampCreation(timestampMs);
        executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletelyFilled);
        return executionReport;
    }

    @Test
    public void getPortfolioSnapshotReflectsLiveUpdatesNotJustStartupState() {
        if (Instrument.getInstrument(INSTRUMENT_PK) == null) {
            Instrument instrument = new Instrument();
            instrument.setPrimaryKey(INSTRUMENT_PK);
            instrument.setMarket("kraken");
            instrument.setPriceTick(0.01);
            instrument.setQuantityTick(0.00001);
            instrument.addMap();
        }

        DummyAlgorithm algorithm = new DummyAlgorithm(ALGO_INFO);
        PortfolioManager portfolioManager = algorithm.getPortfolioManager();

        //right after construction (reset()), with no trades/depths yet, the snapshot must be all-zero
        PortfolioSnapshot initialSnapshot = portfolioManager.getPortfolioSnapshot();
        Assert.assertEquals(0.0, initialSnapshot.unrealizedPnl, 1e-9);
        Assert.assertEquals(0.0, initialSnapshot.netPosition, 1e-9);

        long t0 = 1_700_000_000_000L;
        //buy 1 unit @ 100
        portfolioManager.addTrade(createFilledBuy(100.0, 1.0, t0));

        //price moves up to 110/110.02 -> should create a positive unrealized pnl
        //(the trade above already set lastTimestampUpdate=t0, so this depth update must be spaced
        //by more than Configuration.PORTFOLIO_MANAGER_UPDATE_FREQUENCY_MS (15s) or PortfolioManager
        //will intentionally throttle it and only remember the raw depth, without recalculating pnl)
        portfolioManager.updateDepth(createDepth(110.0, 110.02, t0 + 20_000));

        PortfolioSnapshot afterFirstMove = portfolioManager.getPortfolioSnapshot();
        Assert.assertEquals(1.0, afterFirstMove.netPosition, 1e-9);
        Assert.assertTrue("unrealized pnl must reflect the price move, not stay stuck at the startup value (0)",
                afterFirstMove.unrealizedPnl > 0.0);

        //price moves further (beyond the portfolio-manager update-frequency throttle) -> pnl must change again
        portfolioManager.updateDepth(createDepth(120.0, 120.02, t0 + 40_000));
        PortfolioSnapshot afterSecondMove = portfolioManager.getPortfolioSnapshot();

        Assert.assertTrue("getPortfolioSnapshot() must be recomputed on every call, not cached/stale",
                afterSecondMove.unrealizedPnl > afterFirstMove.unrealizedPnl);
    }
}
