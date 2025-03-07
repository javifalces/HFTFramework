package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.TimeServiceIfc;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.Verb;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;

public class MarketExecutor extends AbstractExecutor {

    protected String lastClientOrderIdSent;
    protected String lastClientOrderIdConfirmed;

    public MarketExecutor(TimeServiceIfc timeServiceIfc, String algorithmInfo, Instrument instrument, AlgorithmConnectorConfiguration tradingEngineConnector) {
        super(timeServiceIfc, algorithmInfo, instrument, tradingEngineConnector);
    }

    @Override
    public boolean increasePosition(long timestamp, Verb verb, double quantity, double price) {
        //add logic to follow order until the end of execution
        if (isExecuting) {
            long elapsedMs = (timestamp - isExecutingSince.getTime());
            logger.error("{} {} on {} can't increasePosition when isExecuting since {} [{}< timeout {} ms]", getCurrentTime(), this.instrument, this.algorithmInfo, isExecutingSince, elapsedMs, timeoutIsExecutingMs);
            return false;
        }
        OrderRequest orderRequest = OrderRequest.createMarketOrderRequest(timestamp, this.algorithmInfo, this.instrument, verb, quantity);
        lastClientOrderIdSent = orderRequest.getClientOrderId();

        isExecuting = true;
        isExecutingSince = getCurrentTime();

        double bid = lastDepth.getBestBid();
        double ask = lastDepth.getBestAsk();
        logger.info("{} {} [bid:{} ask:{}] increasePosition {} {}@{} of verb {}", getCurrentTime(), instrument, bid, ask, orderRequest.getOrderType().toString(), quantity, price, verb);
        this.tradingEngineConnector.orderRequest(orderRequest);
        return true;
    }

    @Override
    public boolean cancelAll() {
        long timestamp = timeService.getCurrentTimestamp();
        boolean sendSameAsConfirmed = lastClientOrderIdConfirmed != null && lastClientOrderIdSent != null && lastClientOrderIdConfirmed.equals(lastClientOrderIdSent);

        if (lastClientOrderIdConfirmed != null || lastClientOrderIdSent != null) {
            if (lastClientOrderIdConfirmed != null) {
                logger.info("cancelling lastClientOrderIdConfirmed {}", lastClientOrderIdConfirmed);
                OrderRequest cancelOrderRequest = OrderRequest.createCancel(timestamp, this.algorithmInfo, this.instrument, lastClientOrderIdConfirmed);
                this.tradingEngineConnector.orderRequest(cancelOrderRequest);
            }
            if (lastClientOrderIdSent != null && !sendSameAsConfirmed) {
                logger.info("cancelling lastClientOrderIdSent {}", lastClientOrderIdSent);
                OrderRequest cancelOrderRequest1 = OrderRequest.createCancel(timestamp, this.algorithmInfo, this.instrument, lastClientOrderIdSent);
                this.tradingEngineConnector.orderRequest(cancelOrderRequest1);
            }
        }
        finish();
        return true;
    }


    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {

        if (!executionReport.getInstrument().equalsIgnoreCase(this.instrument.getPrimaryKey())) {
            return false;
        }

        String clOrIdReceived = executionReport.getClientOrderId();
        if (!clOrIdReceived.equals(lastClientOrderIdSent)) {
            logger.error("{} {} {} received ER ClientOrderId: {} not the same as lastClientOrderIdSent:{}", getCurrentTime(), instrument, toString(), clOrIdReceived, lastClientOrderIdSent);
            return false;
        }
        logger.info("{} onExecutionReportUpdate: {} ", getCurrentTime(), executionReport.toString());

        boolean isConfirmed = executionReport.getExecutionReportStatus() == ExecutionReportStatus.Active || executionReport.getExecutionReportStatus() == ExecutionReportStatus.PartialFilled;
        if (isConfirmed) {
            lastClientOrderIdConfirmed = clOrIdReceived;
        }

        boolean isRejected = executionReport.getExecutionReportStatus() == ExecutionReportStatus.Rejected;
        boolean isCF = executionReport.getExecutionReportStatus() == ExecutionReportStatus.CompletellyFilled;
        boolean isFinished = isCF || isRejected;
        if (isFinished) {
            if (isCF) {
                logger.info("{} executing finished success  {} {}@{}", getCurrentTime(), instrument, executionReport.getQuantityFill(), executionReport.getPrice());
            }
            if (isRejected) {
                logger.warn("{} executing finished rejected  {} {}@{} because {}", getCurrentTime(), instrument, executionReport.getQuantityFill(), executionReport.getPrice(), executionReport.getRejectReason());

            }
            finish();
        }
        return true;
    }

    private void finish() {
        isExecuting = false;
        lastClientOrderIdSent = null;
        lastClientOrderIdConfirmed = null;
    }


}
