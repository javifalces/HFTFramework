package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.TimeServiceIfc;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.Verb;

public class SniperExecutor extends AbstractExecutor {

    protected String lastClientOrderIdSent;
    protected String lastClientOrderIdConfirmed;
    protected long timeStepMs;
    protected int steps;
    protected int currentStep;

    protected int numberOfSteps;

    protected OrderRequest lastOrderSent;
    protected ExecutionReport lastExecutionReportActive;

    protected double maxSlippageMarketTicks = 3;

    protected int succeededExecution = 0;
    protected int failedExecution = 0;

    protected boolean isAggressive = false;


    public SniperExecutor(TimeServiceIfc timeServiceIfc, String algorithmInfo, Instrument instrument, AlgorithmConnectorConfiguration tradingEngineConnector, long timeStepMs, int steps) {
        super(timeServiceIfc, algorithmInfo, instrument, tradingEngineConnector);
        this.timeStepMs = timeStepMs;
        this.steps = steps;
    }

    @Override
    public boolean increasePosition(long timestamp, Verb verb, double quantity, double price) {
        //add logic to follow order until the end of execution
        if (isExecuting) {
            long elapsedMs = (timestamp - isExecutingSince.getTime());
            logger.error("{} {} on {} can't increasePosition when isExecuting since {} [{}< timeout {} ms]", getCurrentTime(), this.instrument, this.algorithmInfo, isExecutingSince, elapsedMs, timeoutIsExecutingMs);
            return false;
        }
        currentStep = 0;
        double marketPrice = verb == Verb.Buy ? lastDepth.getBestAsk() : lastDepth.getBestBid();
        int priceSteps = (int) Math.floor((marketPrice - price) / instrument.getPriceStep());
        numberOfSteps = priceSteps;

        OrderRequest orderRequest = OrderRequest.createLimitOrderRequest(timestamp, this.algorithmInfo, this.instrument, verb, quantity, price);
        lastClientOrderIdSent = orderRequest.getClientOrderId();
        lastOrderSent = orderRequest;

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
        boolean sendSameAsConfirmed = lastClientOrderIdConfirmed != null && lastClientOrderIdConfirmed.equals(lastClientOrderIdSent);

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
        currentStep = 0;
        numberOfSteps = 0;
        isAggressive = false;
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

        if (executionReport.getExecutionReportStatus() == ExecutionReportStatus.Active) {
            lastExecutionReportActive = executionReport;
        }

        boolean isRejected = executionReport.getExecutionReportStatus() == ExecutionReportStatus.Rejected;
        boolean isCF = executionReport.getExecutionReportStatus() == ExecutionReportStatus.CompletellyFilled;
        boolean isFinished = isCF || isRejected;
        if (isFinished) {
            if (isCF) {
                logger.info("{} {} executing finished isAggressive:{}  {}@{}", getCurrentTime(), instrument, isAggressive, executionReport.getQuantityFill(), executionReport.getPrice());
                if (isAggressive) {
                    failedExecution++;
                } else {
                    succeededExecution++;
                }

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
        currentStep = 0;
        numberOfSteps = 0;
        isAggressive = false;
    }


    @Override
    public boolean onDepthUpdate(Depth depth) {
        boolean output = super.onDepthUpdate(depth);
        if (isExecuting) {
            long elapsedMs = (timeService.getCurrentTimestamp() - isExecutingSince.getTime());
            if (elapsedMs > timeStepMs) {
                currentStep++;
                if (currentStep < numberOfSteps) {
                    Verb lastVerb = lastOrderSent.getVerb();
                    double incrementPrice = instrument.getPriceStep() * currentStep;
                    double newPrice = lastVerb == Verb.Buy ? lastOrderSent.getPrice() + incrementPrice : lastOrderSent.getPrice() - incrementPrice;
                    newPrice = instrument.roundPrice(newPrice);
                    if (Math.abs(newPrice - lastOrderSent.getPrice()) < instrument.getPriceStep()) {
                        //we need more steps to change price -> wait
                        isExecutingSince = getCurrentTime();
                        logger.info("{} {} waiting next step ", getCurrentTime(), instrument);
                        return output;
                    }

                    OrderRequest modifyOrder = OrderRequest.modifyOrder(depth.getTimestamp(), algorithmInfo, instrument, lastVerb, lastOrderSent.getQuantity(), newPrice, lastOrderSent.getClientOrderId());
                    lastClientOrderIdSent = modifyOrder.getClientOrderId();
                    lastOrderSent = modifyOrder;
                    isAggressive = false;
                    logger.info("{} {} step {}->{} [bid:{} ask:{}] modifyOrder {} {}@{} of verb {}",
                            getCurrentTime(), instrument, modifyOrder.getOrigClientOrderId(),
                            modifyOrder.getClientOrderId(), depth.getBestBid(), depth.getBestAsk(),
                            modifyOrder.getOrderType().toString(), modifyOrder.getQuantity(), modifyOrder.getPrice(),
                            modifyOrder.getVerb());
                    this.tradingEngineConnector.orderRequest(modifyOrder);
                    isExecutingSince = getCurrentTime();
                } else {
                    //timeout -> market execution
                    Verb lastVerb = lastOrderSent.getVerb();
                    double incrementPrice = instrument.getPriceStep() * maxSlippageMarketTicks;
                    double newPrice = lastVerb == Verb.Buy ? depth.getBestAsk() + incrementPrice : depth.getBestBid() - incrementPrice;
                    newPrice = instrument.roundPrice(newPrice);

                    OrderRequest modifyOrder = OrderRequest.modifyOrder(depth.getTimestamp(), algorithmInfo,
                            instrument, lastVerb, lastOrderSent.getQuantity(), newPrice,
                            lastOrderSent.getClientOrderId());
                    lastClientOrderIdSent = modifyOrder.getClientOrderId();
                    lastOrderSent = modifyOrder;
                    isAggressive = true;
                    logger.info("{} {} lastStep Market {}->{} [bid:{} ask:{}] modifyOrder {} {}@{} of verb {}",
                            getCurrentTime(), instrument, modifyOrder.getOrigClientOrderId(),
                            modifyOrder.getClientOrderId(),
                            depth.getBestBid(), depth.getBestAsk(),
                            modifyOrder.getOrderType().toString(), modifyOrder.getQuantity(), modifyOrder.getPrice(),
                            modifyOrder.getVerb());
                    this.tradingEngineConnector.orderRequest(modifyOrder);
                    isExecutingSince = getCurrentTime();

                }
            }
        }

        return output;
    }
}
