package com.lambda.investing.algorithmic_trading.tester;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PnlSnapshot;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.Verb;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LookForwardBiasAlgorithm extends Algorithm {

    private long lastDepthUpdateTime;
    private List<Candle> candles; // Map of instrument primary key to list of candles
    private int candlesIterator = 0;
    protected int secondsCandles;
    protected volatile boolean weAreInTheMarket = false;
    protected double quantity;
    protected Instrument instrument;


    public LookForwardBiasAlgorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo, Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);
        setParameters(parameters);
    }

    public LookForwardBiasAlgorithm(String algorithmInfo, Map<String, Object> parameters) {
        super(algorithmInfo, parameters);
        setParameters(parameters);
    }

    @Override
    public String printAlgo() {
        return String
                .format("%s  \n\tmainInstrument=%s\n\tquantity=%.3f\n\tfirstHourOperatingIncluded=%d\n\tlastHourOperatingIncluded=%d",
                        algorithmInfo, instrument, quantity, firstHourOperatingIncluded,
                        lastHourOperatingIncluded);
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        super.setParameters(parameters);
        this.secondsCandles = getParameterIntOrDefault(parameters, "secondsCandles", 60);
        this.quantity = getParameterDouble(parameters, "quantity");
        getCandleFromTickUpdater().setSecondsThreshold(secondsCandles);

        String instrumentPK = this.instrumentPks[0];
        if (instrumentPK != null) {
            Instrument instrument = Instrument.getInstrument(instrumentPK);
            if (instrument == null) {
                logger.error("instrument not found on {}", instrumentPK);
                System.err.println("instrument not found on " + instrumentPK);
            }
            this.instrument = instrument;
        }
        instruments.add(instrument);
    }

    private void synchronizeCandlesIterator() {
        if (candles == null || candles.isEmpty()) {
            candlesIterator = 0;
            return;
        }
        if (candlesIterator >= candles.size()) {
            candlesIterator = 0; // Reset to the beginning
        }
        for (Candle candle : candles) {
            if (candle.getTimestamp() > getCurrentTimestamp()) {
                break; // We found a candle that is after the last depth update
            }
            candlesIterator++;
        }

    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        boolean output = super.onDepthUpdate(depth);
        if (lastDepthUpdateTime == 0) {
            // First time we receive a depth update, we need to download the candles
            Date startDate = new Date(depth.getTimestamp());
            Date endDate = new Date(startDate.getTime()); // One day later
            endDate.setHours(23);
            endDate.setMinutes(59);
            endDate.setSeconds(59);

            Set<Instrument> instruments = Set.of(Instrument.getInstrument(depth.getInstrument()));
            logger.info("Downloading candles for instrument {} from {} to {}", depth.getInstrument(), startDate, endDate);
            Map<String, List<Candle>> candleMap = downloadCandles(startDate, endDate, instruments, CandleType.mid_time_seconds_threshold, secondsCandles);
            if (candleMap == null || candleMap.isEmpty()) {
                logger.error("No candles found for instrument {}", depth.getInstrument());
                System.err.println("No candles found for instrument " + depth.getInstrument());
                return false; // No candles to process
            }
            candles = candleMap.get(depth.getInstrument());
            synchronizeCandlesIterator();
        }

        lastDepthUpdateTime = depth.getTimestamp();
        return output;
    }


    @Override
    public void onCandleUpdate(Candle candle) {
        super.onCandleUpdate(candle);
        if (!candle.getCandleType().equals(CandleType.mid_time_seconds_threshold)) {
            return; // We only care about mid price candles
        }

        if (candles == null || candles.isEmpty()) {
            logger.warn("No candles available to process for instrument {}", candle.getInstrumentPk());
            return; // No candles to process
        }
        if (candlesIterator >= candles.size()) {
            // We have reached the end of the candles list, reset the iterator
            candlesIterator = 0;
        }
        Candle futureCandle = candles.get(candlesIterator);

        Date futureCandleDate = new Date(futureCandle.getTimestamp());
        Date currentCandleDate = new Date(candle.getTimestamp());
        while (futureCandleDate.getTime() <= currentCandleDate.getTime()) {
            // We are not ready to process this candle yet, we need to wait for the future candle
            candlesIterator++;
            if (candlesIterator >= candles.size()) {
                // We have reached the end of the candles list, reset the iterator
                candlesIterator = 0;
                logger.warn("No more candles to process, resetting iterator");
                return; // No more candles to process
            }

            futureCandle = candles.get(candlesIterator);
            futureCandleDate = new Date(futureCandle.getTimestamp());

        }

        Instrument instrument = Instrument.getInstrument(candle.getInstrumentPk());
        Verb currentActionVerb = Verb.NotSet;
        double position = this.getPosition(instrument);
        if (position > 0) {
            currentActionVerb = Verb.Buy;
        } else if (position < 0) {
            currentActionVerb = Verb.Sell;
        } else {
            currentActionVerb = Verb.NotSet;
        }

        double quantity = this.quantity;
        Verb actionVerb = Verb.NotSet;

        if (Math.abs(futureCandle.getOpen() - candle.getClose()) > instrument.getPriceStep() * 10) {
            logger.warn("[{}] Price step is too high for instrument {}. Skipping candle processing.", getCurrentTime(), instrument.getSymbol());
            return; // Skip processing this candle if the price step is too high
        }
        boolean buyWorthIt = buyWorthIt(candle, 15); // We need to cover the taker fee for both buy and sell
        boolean weMustSell = futureCandle.getClose() <= candle.getClose();
        if (buyWorthIt && currentActionVerb != Verb.Buy) {
            // buy it
            actionVerb = Verb.Buy;
            logger.info("[{}] Buying instrument {} at price {} prediction is higher at {} [{}] ", getCurrentTime(), instrument.getSymbol(), candle.getClose(), futureCandle.getClose(), futureCandleDate);
//            if(currentActionVerb == Verb.Sell) {
//                // if we are selling, we need to close the position first
//                quantity*=2; // double the quantity to close the position
//            }
        } else if (!weMustSell && currentActionVerb == Verb.Buy) {
            // buy it
            actionVerb = Verb.NotSet;
            logger.info("[{}] Hold instrument {} at price {} prediction is higher at {} [{}] ", getCurrentTime(), instrument.getSymbol(), candle.getClose(), futureCandle.getClose(), futureCandleDate);
//            if(currentActionVerb == Verb.Sell) {
//                // if we are selling, we need to close the position first
//                quantity*=2; // double the quantity to close the position
//            }
        } else if (weMustSell && currentActionVerb == Verb.Buy) {
            // sell it
            actionVerb = Verb.Sell;
            logger.info("[{}] Closing instrument {} at price {} prediction is lower at {} [{}]", getCurrentTime(), instrument.getSymbol(), candle.getClose(), futureCandle.getClose(), futureCandleDate);
//            if(currentActionVerb == Verb.Buy) {
//                // if we are selling, we need to close the position first
//                quantity*=2; // double the quantity to close the position
//            }
        }


        if (actionVerb != Verb.NotSet) {
            // We have an action to take
            OrderRequest marketOrderRequest = createMarketOrderRequest(instrument, actionVerb, quantity);
            try {
                double marketPrice = marketOrderRequest.getVerb() == Verb.Buy ? getLastDepth(instrument).getBestAsk() : getLastDepth(instrument).getBestBid();
                logger.info("[{}]{}", getCurrentTime(), String.format("%s [%s]  %s %.5f@%.5f", marketOrderRequest.getOrderRequestAction(),
                        marketOrderRequest.getClientOrderId(), marketOrderRequest.getVerb(), marketOrderRequest.getQuantity(), marketPrice));

                sendOrderRequest(marketOrderRequest);
            } catch (LambdaTradingException e) {
                logger.error("Error sending order request: {}", e.getMessage());
            }
        }

        candlesIterator++;

    }

    private boolean buyWorthIt(Candle candle, int maxCandleLookAhead) {
//        Candle futureCandle = candles.get(candlesIterator);
//        return ((futureCandle.getClose() / candle.getClose())-1)*100> instrument.getTakerFeePct() * 2;

        int firstCandleIndex = candlesIterator;
        double lastClose = candle.getClose();
        int candlesAhead = 0;
        for (candlesAhead = 0; candlesAhead < maxCandleLookAhead && firstCandleIndex + candlesAhead < candles.size(); candlesAhead++) {
            Candle futureCandle = candles.get(firstCandleIndex + candlesAhead);
            if (futureCandle.getClose() <= lastClose) {
                // We found a future candle that is higher than the current candle
                break;
            }

            lastClose = futureCandle.getClose();
        }
        boolean output = ((lastClose / candle.getClose()) - 1) * 100 > instrument.getTakerFeePct() * 2;
        if (output) {
            double profit = ((lastClose / candle.getClose()) - 1) * 100;
            logger.info("[{}] Buy worth it for instrument {} in {} candles: current close {}, future close {}, profit {}%",
                    getCurrentTime(), instrument.getSymbol(), candlesAhead, candle.getClose(), lastClose, String.format("%.2f", profit));
        }
        return output;

    }

    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        boolean output = super.onExecutionReportUpdate(executionReport);
        logger.info("[{}]Execution report update: {}", getCurrentTime(), executionReport);
        // log current pnl position
        if (executionReport.getExecutionReportStatus() == ExecutionReportStatus.CompletelyFilled
                || executionReport.getExecutionReportStatus() == ExecutionReportStatus.PartialFilled) {
            PnlSnapshot pnlSnapshot = portfolioManager.getLastPnlSnapshot(instrument.getPrimaryKey());
            if (pnlSnapshot != null) {
                double pnl = pnlSnapshot.getTotalPnl();
                double unrealizedPnl = pnlSnapshot.getUnrealizedPnl();
                double totalFees = pnlSnapshot.getTotalFees();
                double unrealizedFees = pnlSnapshot.getUnrealizedFees();
                logger.info("[{}]PnL snapshot for instrument {}: Total PnL: {}, Unrealized PnL: {}, Total Fees: {}, Unrealized Fees: {}",
                        getCurrentTime(), instrument.getSymbol(), String.format("%.2f", pnl), String.format("%.2f", unrealizedPnl),
                        String.format("%.2f", totalFees), String.format("%.2f", unrealizedFees));
            } else {
                logger.warn("[{}]No PnL snapshot available for instrument {}", getCurrentTime(), instrument.getSymbol());
            }
        }

        return output;
    }
}
