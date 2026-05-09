package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.time_service.TimeServiceIfc;
import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.Verb;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

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
     *
     * @param executionReport the terminal execution report
     */
    protected void notifyExecutionFinished(ExecutionReport executionReport) {
        double midPrice = (lastDepth != null) ? lastDepth.getMidPrice() : Double.NaN;
        executorStatistics.onExecutionFinished(timeService.getCurrentTimestamp(), executionReport, midPrice);
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
