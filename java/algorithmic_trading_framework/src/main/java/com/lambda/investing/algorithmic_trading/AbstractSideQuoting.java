package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.Configuration;
import com.lambda.investing.TimeSeriesQueue;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.*;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractSideQuoting extends SingleInstrumentAlgorithm {

    protected enum Style {
        aggressive,//market orders
        passive,//on the mid price
        level,//based on levelToQuote
    }


    protected static Logger logger = LogManager.getLogger(AbstractSideQuoting.class);
    protected static int DEFAULT_QUEUE_TRADE_SIZE_MINUTES = 128;

    @Getter
    public double quantityBuy;
    @Getter
    public double quantitySell;

    protected Verb verb;
    protected Map<Verb, Boolean> entryWaitingER = new ConcurrentHashMap<>();
    protected Map<Verb, Boolean> exitWaitingER = new ConcurrentHashMap<>();
    ;

    private Map<Verb, Boolean> sideActive = new ConcurrentHashMap<>();
    protected TimeSeriesQueue<Double> queueCloseCandles;
    protected TimeSeriesQueue<Candle> queueCandles;

    private double lastValidSpread, lastValidMid = 0.01;
    protected boolean isReady = false;
    protected boolean alphaIsReady = true;
    protected Verb onlyEntryVerb = null;
    //parameters
    protected int levelToQuote = 1;
    protected boolean changeSide = false;//will change verb of the outputs if true
    protected Verb lastFilledVerb = null;
    protected String style = "aggressive";//passive conservative
    protected boolean exitAggressive = true;
    protected CandleType candleTypeBusiness = CandleType.mid_time_seconds_threshold;

    protected long lastTimestampSendOrderRequest = 0;

    public abstract String printAlgo();

    public abstract void checkBoundaries();

    public abstract void setCandleSideRules(double candle);

    public void setAlphaReady(boolean ready) {
        alphaIsReady = ready;
    }

    public AbstractSideQuoting(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo,
                               Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);
        constructorAbstract(parameters);
    }

    public AbstractSideQuoting(String algorithmInfo, Map<String, Object> parameters) {
        super(algorithmInfo, parameters);
        constructorAbstract(parameters);
    }


    public void setLevelToQuote(int levelToQuote) {
        this.levelToQuote = levelToQuote;
    }

    public void setCandleTypeBusiness(CandleType candleTypeBusiness) {
        this.candleTypeBusiness = candleTypeBusiness;
    }

    public boolean isReady() {
        return isReady && alphaIsReady;
    }

    protected void constructorAbstract(Map<String, Object> parameters) {

        queueCloseCandles = new TimeSeriesQueue(DEFAULT_QUEUE_TRADE_SIZE_MINUTES);
        queueCandles = new TimeSeriesQueue(DEFAULT_QUEUE_TRADE_SIZE_MINUTES);
        setParameters(parameters);
        setSide(null);//to start without side chosen
        isReady = false;
    }

    @Override public void init() {
        super.init();
        getQuoteManager(this.instrument.getPrimaryKey()).setStopOnCf(true);
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    public void setParameters(Map<String, Object> parameters) {
        super.setParameters(parameters);

        double quantityGeneral = getParameterDoubleOrDefault(parameters, "quantity", -1);
        if (quantityGeneral > 0) {
            quantityBuy = getParameterDoubleOrDefault(parameters, "quantityBuy", quantityGeneral);
            quantitySell = getParameterDoubleOrDefault(parameters, "quantitySell", quantityGeneral);
        } else {
            quantityBuy = getParameterDouble(parameters, "quantityBuy");
            quantitySell = getParameterDouble(parameters, "quantitySell");
        }

        levelToQuote = getParameterIntOrDefault(parameters, "levelToQuote", 1);
        String verbInput = getParameterString(parameters, "verb");

        if (verbInput != null) {
            onlyEntryVerb = Verb.valueOf(verbInput);
            logger.info("Setting onlyEntry verb of {}", onlyEntryVerb);
            System.out.println("Setting onlyEntry verb of " + onlyEntryVerb);
        }

    }



    @Override public void onUpdateCandle(Candle candle) {

        if (!candleTypeBusiness.equals(candle.getCandleType()) || !candle.getInstrumentPk()
                .equals(this.instrument.getPrimaryKey())) {
            //skip other type of candles
            return;
        }

        queueCloseCandles.offer(candle.getClose());
        queueCandles.offer(candle);
        setCandleSideRules(candle.getClose());
    }

    public void setChangeSide(boolean changeSide) {
        this.changeSide = changeSide;

        //		if (this.changeSide && this.onlyEntryVerb != null) {
        //			//change onlyEntry Verb
        //			this.onlyEntryVerb=Verb.OtherSideVerb(this.onlyEntryVerb);
        //		}

    }

    public Verb getSide() {
        Verb output = this.verb;
        if (changeSide) {
            output = Verb.OtherSideVerb(this.verb);
        }
        return output;
    }

    public void setSide(Verb verb) {
        if (verb == null) {
            if (!style.equalsIgnoreCase(Style.aggressive.toString())) {
                if (this.verb != null) {
                    logger.info("[{}]disable both sides", getCurrentTime());
                }
                this.verb = null;

                sideActive.put(Verb.Buy, false);
                sideActive.put(Verb.Sell, false);
                if (instrument != null) {
                    Depth depth = getLastDepth(instrument);
                    if (depth != null) {
                        updateQuote(depth);
                    }
                }

            }
            //			sideActive.clear();
            //			this.spreadMultiplier = this.spreadMultiplierDefault * 10;
        } else {
            this.verb = verb;//verb is always set as original
            if (!style.equalsIgnoreCase(Style.aggressive.toString())) {
                logger.info("[{}] enable only verb {}", getCurrentTime(), verb);
            }
            if (verb.equals(Verb.Buy)) {
                sideActive.put(Verb.Buy, true);
                sideActive.put(Verb.Sell, false);
            }

            if (verb.equals(Verb.Sell)) {
                sideActive.put(Verb.Sell, true);
                sideActive.put(Verb.Buy, false);
            }
        }
    }

    protected boolean isWaitingExit() {
        return (this.exitWaitingER.getOrDefault(Verb.Buy, false) || this.exitWaitingER.getOrDefault(Verb.Sell, false));
    }

    protected boolean isWaitingEntry() {
        return (this.entryWaitingER.getOrDefault(Verb.Buy, false) || this.entryWaitingER
                .getOrDefault(Verb.Sell, false));
    }


    protected void closePosition() {

        //		Verb closeVerb = Verb.OtherSideVerb(verb);
        double position = this.getPosition();
        if (position == 0) {
            logger.warn("trying to close already closed position {}", position);
            return;
        }
        Verb closeVerb = position > 0 ? Verb.Sell : Verb.Buy;

        //avoid close with our position counter
        double quantityQuoted = position > 0 ? quantityBuy : quantitySell;
        if (changeSide) {
            quantityQuoted = position > 0 ? quantitySell : quantityBuy;
        }

        double quantity = Math.min(Math.abs(position), quantityQuoted);
        setSide(null);
        this.verb = null;//just in case with aggressive
        OrderRequest orderRequest = createMarketOrderRequest(instrument, closeVerb, quantity);

        try {
            exitWaitingER.put(closeVerb, true);
            this.lastFilledVerb = null;
            this.sendOrderRequest(orderRequest);
        } catch (LambdaTradingException e) {
            logger.error("error sending {} order on {}", closeVerb, instrument, e);
        }
    }

    protected double getPosition(boolean changeSide) {
        double output = getPosition();
        if (changeSide) {
            output = -1 * output;
        }
        return output;
    }

    protected double getPosition() {
        return getAlgorithmPosition(instrument);
    }

    protected void entryAgressive(Verb entryVerb) {
        double qty = quantitySell;
        Verb verbToSend = verb;
        if (entryVerb.equals(Verb.Sell)) {
            qty = quantitySell;
            if (changeSide) {
                qty = quantityBuy;
                verbToSend = Verb.Buy;
            }

        }
        if (entryVerb.equals(Verb.Buy)) {
            qty = quantityBuy;
            if (changeSide) {
                qty = quantitySell;
                verbToSend = Verb.Sell;
            }
        }

        OrderRequest marketOrder = createMarketOrderRequest(this.getInstrument(), verbToSend, qty);
        try {
            entryWaitingER.put(verbToSend, true);
            sendOrderRequest(marketOrder);
            //			verb=null;
        } catch (LambdaTradingException e) {
            logger.error("can't sent marketOrder Entry SELL ", e);
        }

    }



    protected double GetLevelPrice(Verb verb, Depth depth) {
        double output = depth.getMidPrice();
        if (verb.equals(Verb.Buy)) {
            if (levelToQuote >= 0) {
                //level
                int levelBid = Math.max(Math.min(depth.getBidLevels(), levelToQuote), 1);
                output = depth.getBids()[levelBid - 1];
            }

            if (levelToQuote < 0) {
                //cross the spread
                output = depth.getBestAsk() + instrument.getPriceTick() * 5;
            }
        }

        if (verb.equals(Verb.Sell)) {
            if (levelToQuote >= 0) {
                //conservative
                int levelAsk = Math.max(Math.min(depth.getAskLevels(), levelToQuote), 1);
                output = depth.getAsks()[levelAsk - 1];
            }
            if (levelToQuote < 0) {
                //cross the spread
                output = depth.getBestBid() - instrument.getPriceTick() * 5;
            }
        }

        return output;
    }

    private void updateQuote(Depth depth) {
        double bidQuantity = 0;
        double askQuantity = 0;
        double askPrice = 0;
        double bidPrice = 0;
        //create quote request
        QuoteRequest quoteRequest = createQuoteRequest(this.instrument);
        quoteRequest.setQuoteRequestAction(QuoteRequestAction.On);
        String freeText = null;
        if (levelToQuote <= 0) {
            freeText = "market";
        }//for metatrader
        if (verb == null) {
            quoteRequest.setQuoteRequestAction(QuoteRequestAction.Off);
        } else {
            if (verb.equals(Verb.Sell)) {
                if (style.equalsIgnoreCase(Style.level.toString())) {
                    //					int levelAsk = Math.max(depth.getAskLevels(), levelToQuote);
                    //					askPrice = depth.getAsks()[levelAsk - 1];
                    askPrice = GetLevelPrice(verb, depth);
                } else if (style.equalsIgnoreCase(Style.passive.toString())) {
                    askPrice = depth.getMidPrice();
                } else {
                    System.err.println("style not found   aggressive ,level or passive  !! " + style);
                    logger.error("style not found  aggressive ,level or passive !! " + style);
                }
                askQuantity = this.quantitySell;
            }
            if (verb.equals(Verb.Buy)) {
                if (style.equalsIgnoreCase(Style.level.toString())) {
                    //					int levelBid = Math.max(depth.getBidLevels(), levelToQuote);
                    //					bidPrice = depth.getBids()[levelBid - 1];
                    bidPrice = GetLevelPrice(verb, depth);

                } else if (style.equalsIgnoreCase(Style.passive.toString())) {
                    bidPrice = depth.getMidPrice();
                } else {
                    System.err.println("style not found  aggressive ,level or passive !! " + style);
                    logger.error("style not found  aggressive ,level or passive !! " + style);
                }
                bidQuantity = this.quantityBuy;
            }

            //			askPrice = Precision.round(askPrice, instrument.getNumberDecimalsPrice());
            //			bidPrice = Precision.round(bidPrice, instrument.getNumberDecimalsPrice());

            //Check not crossing the mid price!
            //			askPrice = Math.max(askPrice, depth.getMidPrice() + instrument.getPriceTick());
            //			bidPrice = Math.min(bidPrice, depth.getMidPrice() - instrument.getPriceTick());

            //			Check worst price
            //			double maxAskPrice = depth.getMidPrice() + MAX_TICKS_MIDPRICE_PRICE_DEV * instrument.getPriceTick();
            //			askPrice = Math.min(askPrice, maxAskPrice);
            //			double minBidPrice = depth.getMidPrice() - MAX_TICKS_MIDPRICE_PRICE_DEV * instrument.getPriceTick();
            //			bidPrice = Math.max(bidPrice, minBidPrice);

        }
        quoteRequest.setBidPrice(bidPrice);
        quoteRequest.setAskPrice(askPrice);
        quoteRequest.setBidQuantity(bidQuantity);
        quoteRequest.setAskQuantity(askQuantity);
        quoteRequest.setFreeText(freeText);

        try {
            sendQuoteRequest(quoteRequest);
            //				logger.info("quoting  {} bid {}@{}   ask {}@{}", instrument.getPrimaryKey(), quantity, bidPrice,
            //						quantity, askPrice);
        } catch (LambdaTradingException e) {
            logger.error("can't quote {} bid {}@{}   ask {}@{}", instrument.getPrimaryKey(), quantityBuy, bidPrice,
                    quantitySell, askPrice, e);
        }

    }

    private void disableQuote() throws LambdaTradingException {
        //disable quoting if we are not aggressive
        QuoteRequest quoteRequest = createQuoteRequest(this.getInstrument());
        quoteRequest.setQuoteRequestAction(QuoteRequestAction.Off);
        sendQuoteRequest(quoteRequest);
    }

    @Override public boolean onDepthUpdate(Depth depth) {
        if (!depth.getInstrument().equals(instrument.getPrimaryKey())) {
            return false;
        }

        if (!super.onDepthUpdate(depth) || !depth.isDepthFilled()) {
            stop();
            return false;
        } else {
            start();
        }

        try {
            double currentSpread = 0;
            double midPrice = 0;
            try {
                currentSpread = depth.getSpread();
                midPrice = depth.getMidPrice();
            } catch (Exception e) {
                return false;
            }

            setCandleSideRules(midPrice);

            if (currentSpread == 0) {
                currentSpread = lastValidSpread;
            } else {
                lastValidSpread = currentSpread;
            }

            if (midPrice == 0) {
                midPrice = lastValidMid;
            } else {
                lastValidMid = midPrice;
            }

            if (!style.equalsIgnoreCase(Style.aggressive.toString())) {
                updateQuote(depth);
            }

        } catch (Exception e) {
            logger.error("error onDepth  : ", e);
        }

        return true;
    }

    @Override public void sendOrderRequest(OrderRequest orderRequest) throws LambdaTradingException {
        logger.info("[{}] sendOrderRequest {} {}  -> {}", getCurrentTime(), orderRequest.getOrderRequestAction(),
                orderRequest.getClientOrderId(), orderRequest);
        lastTimestampSendOrderRequest = getCurrentTimestamp();
        super.sendOrderRequest(orderRequest);

    }

    @Override public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        boolean continueProcessing = super.onExecutionReportUpdate(executionReport);
        if (!continueProcessing) {
            return false;
        }

        Verb ErVerb = executionReport.getVerb();
        boolean isTrade = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)
                || executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled);
        boolean isRejected = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected);
        if (style.equalsIgnoreCase(Style.aggressive.toString()) && isTrade) {
            entryWaitingER.put(ErVerb, false);
        }
        if (exitAggressive && isTrade) {
            exitWaitingER.put(ErVerb, false);
        }

        if (style.equalsIgnoreCase(Style.aggressive.toString()) && executionReport.getExecutionReportStatus()
                .equals(ExecutionReportStatus.Active)) {
            //ignore actives
            return true;
        }
        if (isRejected) {
            logger.warn("order rejected {}", executionReport.getClientOrderId());
            if (!isBacktest) {
                System.err.println(Configuration
                        .formatLog("Order rejected {} {}-> {}", executionReport.getClientOrderId(),
                                executionReport.getRejectReason(), executionReport));
            }
            //CHECK if its valid something in this case
            if (style.equalsIgnoreCase(Style.aggressive.toString())) {
                entryWaitingER.put(ErVerb, false);
            }
            if (exitAggressive) {
                exitWaitingER.put(ErVerb, false);
            }
        }

        //		logger.info("onExecutionReportUpdate  {}  {}:  {}", executionReport.getExecutionReportStatus(),
        //				executionReport.getClientOrderId(), executionReport.getRejectReason());

        if (isTrade) {
            try {
                //				logger.info("{} received {}  {}@{}",executionReport.getExecutionReportStatus(),executionReport.getVerb(),executionReport.getLastQuantity(),executionReport.getPrice());
                if (!style.equalsIgnoreCase(Style.aggressive.toString())) {
                    //disable quoting if we are not aggressive
                    disableQuote();
                }

                //				logger.info("unquoting because of trade in {} {}", executionReport.getVerb(),
                //						executionReport.getClientOrderId());
                //				if (!isBacktest && this.verb == null) {
                //					//on backtest is very possible to be here to to single thread!
                //					logger.warn(
                //							"Possible error on trading engine!!! double ER and this.verb is null -> ignore {} we are {}",
                //							executionReport.getClientOrderId(), this.lastFilledVerb);
                //					return true;
                //				}

                Verb verb = ErVerb;
                if (verb == null) {
                    ;//closePosition?
                }
                if (this.changeSide) {
                    lastFilledVerb = Verb.OtherSideVerb(verb);
                } else {
                    lastFilledVerb = verb;
                }
                this.verb = null;

            } catch (LambdaTradingException e) {
                logger.error("can't unquote {}", instrument.getPrimaryKey(), e);
            }
        }
        return true;
    }

    @Override protected void addToPersist(ExecutionReport executionReport) {
        super.addToPersist(executionReport);
    }
}
