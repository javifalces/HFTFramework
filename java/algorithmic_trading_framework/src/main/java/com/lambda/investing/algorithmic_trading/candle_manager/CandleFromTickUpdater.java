package com.lambda.investing.algorithmic_trading.candle_manager;


import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.StateManager;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CandleFromTickUpdater {

    protected static double DEFAULT_MAX_PRICE = -9E9;
    protected static double DEFAULT_MIN_PRICE = 9E9;
    protected static double DEFAULT_MAX_VOLUME = -9E9;
    protected static double DEFAULT_MIN_VOLUME = 9E9;

    public static double VOLUME_THRESHOLD_DEFAULT = 200E6;//200M
    public static int SECONDS_THRESHOLD_DEFAULT = 56;//to be faster than the rest
    protected List<CandleListener> observers;
    protected Map<String, CandleFromTickUpdaterInstrument> instrumentPkToTickCreator;
    protected double volumeThreshold = VOLUME_THRESHOLD_DEFAULT;

    protected int secondsThreshold = SECONDS_THRESHOLD_DEFAULT;
    protected static Logger logger = LogManager.getLogger(CandleFromTickUpdater.class);

    public CandleFromTickUpdater() {
        observers = new ArrayList<>();
        instrumentPkToTickCreator = new ConcurrentHashMap();
        logger = LogManager.getLogger(CandleFromTickUpdater.class);
    }

    public void setVolumeThreshold(double volumeThreshold) {
        logger.info("set volumeThreshold candles={} ", volumeThreshold);
        this.volumeThreshold = volumeThreshold;
        for (CandleFromTickUpdaterInstrument candleFromTickUpdaterInstrument : instrumentPkToTickCreator.values()) {
            candleFromTickUpdaterInstrument.setVolumeThreshold(volumeThreshold);
        }
    }

    public void setSecondsThreshold(int secondsThreshold) {
        logger.info("set setSecondsThreshold time={} ", secondsThreshold);
        this.secondsThreshold = secondsThreshold;
        for (CandleFromTickUpdaterInstrument candleFromTickUpdaterInstrument : instrumentPkToTickCreator.values()) {
            candleFromTickUpdaterInstrument.setSecondsThreshold(secondsThreshold);
        }

    }


    public double getVolumeThreshold() {
        return volumeThreshold;
    }

    public int getSecondsThreshold() {
        return secondsThreshold;
    }

    public void register(CandleListener candleListener) {

        observers.add(candleListener);

        if (observers.size() > 1) {
            int initialSize = observers.size();
            //register in order -> first all the non-algorithms
            List<CandleListener> listenersOut = new ArrayList<>();
            for (CandleListener candleListener1 : observers) {
                //if it is an algorithm, then skip it
                if (candleListener1 instanceof Algorithm) {
                    continue;
                }
                listenersOut.add(candleListener1);
            }
            //then the algorithms
            for (CandleListener candleListener1 : observers) {
                if (!listenersOut.contains(candleListener1)) {
                    listenersOut.add(candleListener1);
                }
            }
            assert initialSize == listenersOut.size();
            this.observers = listenersOut;
        }
        //register inside
        for (CandleFromTickUpdaterInstrument candleFromTickUpdaterInstrument : instrumentPkToTickCreator.values()) {
            candleFromTickUpdaterInstrument.setObservers(observers);
        }

    }


    public boolean onDepthUpdate(Depth depth) {
        try {
            CandleFromTickUpdaterInstrument candleFromTickUpdaterInstrument = instrumentPkToTickCreator
                    .getOrDefault(depth.getInstrument(), new CandleFromTickUpdaterInstrument(observers, depth.getInstrument(), secondsThreshold, volumeThreshold));
            instrumentPkToTickCreator.put(depth.getInstrument(), candleFromTickUpdaterInstrument);

            //Close the candle detected warn the rest of the instruments
            if (candleFromTickUpdaterInstrument.isNewCandle(depth)) {
                notifyRestOfInstrumentsTimeCandleClosed(candleFromTickUpdaterInstrument, depth.getTimestamp());
            }

            //Open new candle with last update!
            boolean output = candleFromTickUpdaterInstrument.onDepthUpdate(depth);
            return output;
        } catch (Exception e) {
            logger.error("Error onDepthUpdate depth={} e={}", depth, e);
            System.err.println("Error onDepthUpdate depth={} e={}" + depth + e);
            e.printStackTrace();
            return false;
        }
    }

    private void notifyRestOfInstrumentsTimeCandleClosed(CandleFromTickUpdaterInstrument candleFromTickUpdaterInstrument, long timestamp) {
        candleFromTickUpdaterInstrument.leaderThatNotifyTheRest = true;//to avoid cyclical calls

        //notify the rest!
        for (CandleFromTickUpdaterInstrument candleFromTickUpdaterInstrument1 : instrumentPkToTickCreator.values()) {
            if (candleFromTickUpdaterInstrument1.getInstrumentPk().equals(candleFromTickUpdaterInstrument.getInstrumentPk())) {
                //not notify the same instrument that closed the candle -> already did it
                continue;
            }
            Depth lastDepth = candleFromTickUpdaterInstrument1.getLastDepth();
            if (lastDepth == null) {
                continue;
            }
            if (lastDepth.getTimestamp() > timestamp) {
                //could happpen?
                logger.error(" strange error lastDepth.getTimestamp() > timestamp => lastDepth.getTimestamp()={} timestamp={}", lastDepth.getTimestamp(), timestamp);
                continue;
            }
            lastDepth.setTimestamp(timestamp);
            candleFromTickUpdaterInstrument1.onDepthUpdate(lastDepth);
        }
        candleFromTickUpdaterInstrument.leaderThatNotifyTheRest = false;
    }

    public boolean onTradeUpdate(Trade trade) {
        CandleFromTickUpdaterInstrument candleFromTickUpdaterInstrument = instrumentPkToTickCreator
                .getOrDefault(trade.getInstrument(), new CandleFromTickUpdaterInstrument(observers, trade.getInstrument(), secondsThreshold, volumeThreshold));
        instrumentPkToTickCreator.put(trade.getInstrument(), candleFromTickUpdaterInstrument);
        return candleFromTickUpdaterInstrument.onTradeUpdate(trade);
    }


}
