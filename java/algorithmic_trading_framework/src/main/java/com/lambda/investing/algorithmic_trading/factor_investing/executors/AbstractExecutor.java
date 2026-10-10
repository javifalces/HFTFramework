package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.time_service.TimeServiceIfc;
import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.Verb;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class AbstractExecutor implements Executor, ExecutionReportListener, MarketDataListener {
    protected static Logger logger = LogManager.getLogger(AbstractExecutor.class);
    protected AlgorithmConnectorConfiguration algorithmConnectorConfiguration;
    protected TradingEngineConnector tradingEngineConnector;
    protected String algorithmInfo;
    protected Instrument instrument;
    protected boolean isExecuting;
    protected Date isExecutingSince;

    protected long timeoutIsExecutingMs = 30000;//30 seconds
    protected Depth lastDepth;
    protected TimeServiceIfc timeService;
    protected ExecutorStatistics executorStatistics;

    /**
     * All {@link ExecutorStatistics.ExecutionOutcome}s captured so far via {@link #notifyExecutionFinished(ExecutionReport)},
     * kept so aggregated (cumulative) custom columns can be reported in addition to the per-execution ones.
     */
    protected final List<ExecutorStatistics.ExecutionOutcome> executionOutcomes = new ArrayList<>();

    /**
     * Owning algorithm, wired via {@link #setAlgorithm(Algorithm)}. May stay {@code null} (e.g. in unit
     * tests that construct executors directly): in that case order requests are still sent to the trading
     * engine but not registered with any Algorithm, matching the pre-existing behaviour.
     */
    protected Algorithm algorithm;

    public AbstractExecutor(TimeServiceIfc timeServiceIfc, String algorithmInfo, Instrument instrument, AlgorithmConnectorConfiguration algorithmConnectorConfiguration) {
        this.timeService = timeServiceIfc;
        this.algorithmInfo = algorithmInfo;
        this.algorithmConnectorConfiguration = algorithmConnectorConfiguration;
        this.instrument = instrument;
        this.tradingEngineConnector = this.algorithmConnectorConfiguration.getTradingEngineConnector();
        this.tradingEngineConnector.register(algorithmInfo, this);
        this.algorithmConnectorConfiguration.getMarketDataProvider().register(this);
        isExecuting = false;
        this.executorStatistics = new ExecutorStatistics(algorithmInfo + "." + instrument.getPrimaryKey(), instrument);
    }

    @Override
    public void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
        //publish the execution-cost columns at 0 right away, so the GUI/dashboard/Prometheus columns exist
        //with a sane default instead of being absent until the first depth tick or execution fill.
        publishExecutionCostColumns();
    }

    /**
     * Clears the per-day captured {@link #executionOutcomes}, so aggregated custom columns
     * ({@code timeToExecuteMsMax}, {@code slippageCostAgg}, {@code feesCostAgg}) start fresh on new day.
     * Subclasses overriding this should call {@code super.reset()}.
     */
    @Override
    public void reset() {
        executionOutcomes.clear();
        //republish immediately so the aggregated columns drop back to 0 on reset instead of staying at
        //yesterday's values until the next depth tick or execution fill.
        publishExecutionCostColumns();
    }

    /**
     * Sends an order request to the trading engine, first registering it with the owning {@link #algorithm}
     * (if any) via {@link Algorithm#registerSentOrderRequest(OrderRequest)}. Executors bypass
     * {@link Algorithm#sendOrderRequest(OrderRequest)} (which would also register the order) because they
     * send/cancel/modify orders on their own execution schedule; without this explicit registration the
     * algorithm's {@code onExecutionReportUpdate} won't recognize the resulting fills and portfolio/pnl
     * tracking (netInvestment, realizedPnl, ...) stays at 0.0.
     */
    protected void sendOrderRequest(OrderRequest orderRequest) {
        if (algorithm != null) {
            algorithm.registerSentOrderRequest(orderRequest);
        }
        this.tradingEngineConnector.orderRequest(orderRequest);
    }

    public void setTimeoutIsExecutingMs(long timeoutIsExecutingMs) {
        this.timeoutIsExecutingMs = timeoutIsExecutingMs;
    }

    protected Date getCurrentTime() {
        return timeService.getCurrentTime();
    }

    public boolean isExecuting() {
        return isExecuting;
    }

    @Override
    public boolean onInfoUpdate(String header, Object message) {
        return false;
    }

    public abstract boolean increasePosition(long timestamp, Verb verb, double quantity, double price);

    public abstract boolean cancelAll();

    /**
     * Notifies the {@link ExecutorStatistics} that an execution has started.
     * Subclasses should call this after setting {@code isExecuting = true} and sending the order.
     *
     * @param verb      Buy or Sell
     * @param sentPrice price sent in the order request
     */
    protected void notifyExecutionStarted(Verb verb, double sentPrice) {
        double midPrice = (lastDepth != null) ? lastDepth.getMidPrice() : Double.NaN;
        executorStatistics.onExecutionStarted(timeService.getCurrentTimestamp(), verb, sentPrice, midPrice);
    }

    /**
     * Notifies the {@link ExecutorStatistics} that an execution has finished.
     * Subclasses should call this when they receive a terminal execution report (completely filled or rejected).
     * <p>
     * On a successful fill, the resulting {@link ExecutorStatistics.ExecutionOutcome} is captured in
     * {@link #executionOutcomes} and published as both per-execution ({@code timeToExecuteMs},
     * {@code slippageCost}, {@code feesCost}, {@code midPriceMovementCost}) and aggregated
     * ({@code timeToExecuteMsMax}, {@code slippageCostAgg}, {@code feesCostAgg},
     * {@code midPriceMovementCostAgg}) custom columns on the owning {@link #algorithm} (if any), so both the
     * latest fill and the running totals across all executions can be followed in live GUI/dashboard/Prometheus
     * reporting. {@code timeToExecuteMsMax} is the max {@code timeToExecuteMs} across all captured outcomes;
     * the other {@code *Agg} columns are cumulative sums.
     *
     * @param executionReport the terminal execution report
     */
    protected void notifyExecutionFinished(ExecutionReport executionReport) {
        double midPrice = (lastDepth != null) ? lastDepth.getMidPrice() : Double.NaN;
        ExecutorStatistics.ExecutionOutcome outcome = executorStatistics.onExecutionFinished(timeService.getCurrentTimestamp(), executionReport, midPrice);
        if (outcome != null) {
            executionOutcomes.add(outcome);
            publishExecutionCostColumns();
        }
    }

    /**
     * (Re)publishes the {@code timeToExecuteMsMax}/{@code slippageCostAgg}/{@code feesCostAgg}/
     * {@code midPriceMovementCostAgg}/{@code idealPnl} custom columns from the currently captured
     * {@link #executionOutcomes}.
     * <p>
     * Deliberately does NOT bail out when {@link #executionOutcomes} is empty: with no outcomes captured yet
     * (or right after {@link #reset()}), all aggregates are 0 and this still publishes that 0 baseline, so the
     * columns exist in the GUI/dashboard/Prometheus reporting instead of being absent until the first fill.
     * <p>
     * Called from {@link #setAlgorithm(Algorithm)} and {@link #reset()} (to publish/reset the 0 baseline), when
     * a new {@link ExecutorStatistics.ExecutionOutcome} is captured (so the aggregated execution-cost columns
     * themselves move), and on every {@link #onDepthUpdate(Depth)} (so {@code idealPnl}, which also depends on
     * the live mark-to-market {@code openPnl}, doesn't stay frozen at its value from the last fill between
     * executions while the market keeps moving).
     */
    private void publishExecutionCostColumns() {
        if (algorithm == null) {
            return;
        }
        String instrumentPk = instrument.getPrimaryKey();

        long timeToExecuteMsMax = 0L;
        double slippageCostAgg = 0.0;
        double feesCostAgg = 0.0;
        double midPriceMovementCostAgg = 0.0;
        for (ExecutorStatistics.ExecutionOutcome capturedOutcome : executionOutcomes) {
            //timeToExecuteMsMax tracks the worst-case (max) execution latency across all captured outcomes,
            //unlike the other *Agg columns which are cumulative sums of their per-execution costs.
            timeToExecuteMsMax = Math.max(timeToExecuteMsMax, capturedOutcome.getTimeToExecuteMs());
            slippageCostAgg += capturedOutcome.getSlippageCost();
            feesCostAgg += capturedOutcome.getFeesCost();
            midPriceMovementCostAgg += capturedOutcome.getMidPriceMovementCost();
        }
        algorithm.addCurrentCustomColumn(instrumentPk, "timeToExecuteMsAgg", (double) timeToExecuteMsMax);
        algorithm.addCurrentCustomColumn(instrumentPk, "slippageCostAgg", slippageCostAgg);
        algorithm.addCurrentCustomColumn(instrumentPk, "feesCostAgg", feesCostAgg);
        algorithm.addCurrentCustomColumn(instrumentPk, "midPriceMovementCostAgg", midPriceMovementCostAgg);
        //idealPnl adds back the execution costs (fees+slippage) incurred to reach the current position, so it
        //reflects the pnl we would have if fills had happened at the sent price with no fees. midPriceMovementCost
        //is pure market drift (already reflected in openPnl) and must NOT be added back here, otherwise it would
        //be double counted.
        //NOTE: must use this executor's own instrument unrealizedPnl, not
        //getPortfolioManager().getPortfolioSnapshot().getUnrealizedPnl(), which aggregates across ALL
        //instruments in the portfolio: using the portfolio-wide figure here made idealPnl wildly out of
        //scale (off by roughly the number of instruments) once an algorithm traded more than one instrument.
        PnlSnapshot pnlSnapshot = algorithm.getPortfolioManager().getLastPnlSnapshot(instrumentPk);
        double openPnl = (pnlSnapshot != null) ? pnlSnapshot.unrealizedPnl : 0.0;
        double idealPnl = openPnl + slippageCostAgg + feesCostAgg;
        algorithm.addCurrentCustomColumn(instrumentPk, "idealPnl", idealPnl);
    }

    @Override
    public String toString() {
        return "AbstractExecutor{" +
                "algorithmInfo='" + algorithmInfo + '\'' +
                ", instrument=" + instrument +
                '}';
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        if (depth.getInstrument().equals(instrument.getPrimaryKey())) {
            lastDepth = depth;
            //keep idealPnl (and the other execution-cost columns) marked-to-market: openPnl moves with every
            //depth tick, so without this refresh idealPnl would stay frozen at its value from the last fill.
            publishExecutionCostColumns();
        }

        if (isExecuting) {
            long elapsedMs = (timeService.getCurrentTimestamp() - isExecutingSince.getTime());
            if (elapsedMs > timeoutIsExecutingMs) {
                logger.warn("{} timeout isExecuting since {}  elapsed {}>{} timeout ms -> cancelAll ", getCurrentTime(), isExecutingSince, elapsedMs, timeoutIsExecutingMs);
                //cancel all
                cancelAll();
            }

        }
        return true;
    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        return false;
    }

    @Override
    public boolean onCommandUpdate(Command command) {
        return false;
    }


}
