package com.lambda.investing.algorithmic_trading.candle_manager;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

import static com.lambda.investing.algorithmic_trading.candle_manager.CandleFromTickUpdater.*;

@Getter
public class CandleFromTickUpdaterInstrument {

    private Date lastTimestampMinuteTradeCandle = null;
    private double maxPriceMinuteTrade = DEFAULT_MAX_PRICE;
    private double minPriceMinuteTrade = DEFAULT_MIN_PRICE;
    private double openPriceMinuteTrade = -1.;

    private Date lastTimestampHourTradeCandle = null;
    private double maxPriceHourTrade = DEFAULT_MAX_PRICE;
    private double minPriceHourTrade = DEFAULT_MIN_PRICE;
    private double openPriceHourTrade = -1.;

    private Date lastTimestampMinuteMidCandle = null;
    private Date lastTimestampMinuteBidCandle = null;
    private Date lastTimestampMinuteAskCandle = null;
    private double maxPriceMinuteMid = DEFAULT_MAX_PRICE;
    private double minPriceMinuteMid = DEFAULT_MIN_PRICE;
    private double openPriceMinuteMid = -1.;

    private double maxPriceMinuteBid = DEFAULT_MAX_PRICE;
    private double minPriceMinuteBid = DEFAULT_MIN_PRICE;
    private double openPriceMinuteBid = -1.;

    private double maxPriceMinuteAsk = DEFAULT_MAX_PRICE;
    private double minPriceMinuteAsk = DEFAULT_MIN_PRICE;
    private double openPriceMinuteAsk = -1.;


    private double lastCumVolumeCandle = DEFAULT_MIN_PRICE;
    private double maxPriceVolumeDepthCandle = DEFAULT_MAX_PRICE;
    private double minPriceVolumeDepthCandle = DEFAULT_MIN_PRICE;
    private double maxVolumeDepthCandle = DEFAULT_MAX_VOLUME;
    private double minVolumeDepthCandle = DEFAULT_MAX_VOLUME;

    private double openPriceVolumeDepthCandle = -1.;
    private double openVolumeVolumeDepthCandle = -1.;


    private final String instrumentPk;
    private Instrument instrument;
    private Depth lastDepth = null;
    private TimeCandleManager timeCandleManager;
    private boolean TimeCandleClosed = false;
    @Setter
    protected List<CandleListener> observers;
    @Setter
    private double volumeThreshold;

    private int secondsThreshold;

    @Getter
    @Setter
    public boolean leaderThatNotifyTheRest = false;//to avoid cyclical calls

    public CandleFromTickUpdaterInstrument(List<CandleListener> observers, String instrumentPk, int secondsThreshold, double volumeThreshold) {
        this.instrumentPk = instrumentPk;
        this.instrument = Instrument.getInstrument(this.instrumentPk);
        this.volumeThreshold = volumeThreshold;
        this.secondsThreshold = secondsThreshold;
        this.timeCandleManager = new TimeCandleManager(instrument, secondsThreshold);
        this.observers = observers;
    }

    public void setSecondsThreshold(int secondsThreshold) {
        this.secondsThreshold = secondsThreshold;
        this.timeCandleManager = new TimeCandleManager(instrument, this.secondsThreshold);
    }

    private void generateTradeMinuteCandle(Trade trade) {
        Date date = new Date(trade.getTimestamp());
        if (openPriceMinuteTrade == -1) {
            //first candle
            openPriceMinuteTrade = trade.getPrice();
            maxPriceMinuteTrade = trade.getPrice();
            minPriceMinuteTrade = trade.getPrice();
            lastTimestampMinuteTradeCandle = date;
            return;
        }

        maxPriceMinuteTrade = Math.max(maxPriceMinuteTrade, trade.getPrice());
        minPriceMinuteTrade = Math.min(minPriceMinuteTrade, trade.getPrice());
        assert maxPriceMinuteTrade >= minPriceMinuteTrade;
        assert maxPriceMinuteTrade >= openPriceMinuteTrade;
        assert maxPriceMinuteTrade >= trade.getPrice();
        assert minPriceMinuteTrade <= openPriceMinuteTrade;
        assert minPriceMinuteTrade <= trade.getPrice();

        Candle candle = new Candle(CandleType.time_1_min, instrumentPk, openPriceMinuteTrade, maxPriceMinuteTrade,
                minPriceMinuteTrade, trade.getPrice(), trade.getTimestamp());
        notifyListeners(candle);
        //		algorithmToNotify.onUpdateCandle(candle);
        lastTimestampMinuteTradeCandle = date;
        openPriceMinuteTrade = trade.getPrice();
        maxPriceMinuteTrade = trade.getPrice();
        minPriceMinuteTrade = trade.getPrice();
    }

    private void generateMidSecondsCandle(Depth depth) {

        Date date = new Date(depth.getTimestamp());
        if (openPriceMinuteMid == -1) {
            //first candle
            openPriceMinuteMid = depth.getMidPrice();
            maxPriceMinuteMid = depth.getMidPrice();
            minPriceMinuteMid = depth.getMidPrice();
            lastTimestampMinuteMidCandle = date;
            return;
        }

        maxPriceMinuteMid = Math.max(maxPriceMinuteMid, depth.getMidPrice());
        minPriceMinuteMid = Math.min(minPriceMinuteMid, depth.getMidPrice());
        assert maxPriceMinuteMid >= minPriceMinuteMid;
        assert maxPriceMinuteMid >= openPriceMinuteMid;
        assert maxPriceMinuteMid >= depth.getMidPrice();
        assert minPriceMinuteMid <= openPriceMinuteMid;
        assert minPriceMinuteMid <= depth.getMidPrice();

        Candle candle = new Candle(CandleType.mid_time_seconds_threshold, instrumentPk, openPriceMinuteMid,
                maxPriceMinuteMid, minPriceMinuteMid, depth.getMidPrice(), depth.getTimestamp());
        //		algorithmToNotify.onUpdateCandle(candle);
        notifyListeners(candle);
        lastTimestampMinuteMidCandle = date;
        openPriceMinuteMid = depth.getMidPrice();
        maxPriceMinuteMid = depth.getMidPrice();
        minPriceMinuteMid = depth.getMidPrice();
    }

    private void generateBidSecondsCandle(Depth depth) {

        Date date = new Date(depth.getTimestamp());
        if (openPriceMinuteBid == -1) {
            //first candle
            openPriceMinuteBid = depth.getBestBid();
            maxPriceMinuteBid = depth.getBestBid();
            minPriceMinuteBid = depth.getBestBid();
            lastTimestampMinuteBidCandle = date;
            return;
        }

        maxPriceMinuteBid = Math.max(maxPriceMinuteBid, depth.getBestBid());
        minPriceMinuteBid = Math.min(minPriceMinuteBid, depth.getBestBid());
        assert maxPriceMinuteBid >= minPriceMinuteBid;
        assert maxPriceMinuteBid >= openPriceMinuteBid;
        assert maxPriceMinuteBid >= depth.getBestBid();
        assert minPriceMinuteBid <= openPriceMinuteBid;
        assert minPriceMinuteBid <= depth.getBestBid();

        Candle candle = new Candle(CandleType.bid_time_seconds_threshold, instrumentPk, openPriceMinuteBid,
                maxPriceMinuteBid, minPriceMinuteBid, depth.getBestBid(), depth.getTimestamp());

        //		algorithmToNotify.onUpdateCandle(candle);
        notifyListeners(candle);
        lastTimestampMinuteBidCandle = date;
        openPriceMinuteBid = depth.getBestBid();
        maxPriceMinuteBid = depth.getBestBid();
        minPriceMinuteBid = depth.getBestBid();
    }

    private void generateAskSecondsCandle(Depth depth) {

        Date date = new Date(depth.getTimestamp());
        if (openPriceMinuteAsk == -1) {
            //first candle
            openPriceMinuteAsk = depth.getBestAsk();
            maxPriceMinuteAsk = depth.getBestAsk();
            minPriceMinuteAsk = depth.getBestAsk();
            lastTimestampMinuteAskCandle = date;
            return;
        }

        maxPriceMinuteAsk = Math.max(maxPriceMinuteAsk, depth.getBestAsk());
        minPriceMinuteAsk = Math.min(minPriceMinuteAsk, depth.getBestAsk());
        assert maxPriceMinuteAsk >= minPriceMinuteAsk;
        assert maxPriceMinuteAsk >= openPriceMinuteAsk;
        assert maxPriceMinuteAsk >= depth.getBestAsk();
        assert minPriceMinuteAsk <= openPriceMinuteAsk;
        assert minPriceMinuteAsk <= depth.getBestAsk();

        Candle candle = new Candle(CandleType.ask_time_seconds_threshold, instrumentPk, openPriceMinuteAsk,
                maxPriceMinuteAsk, minPriceMinuteAsk, depth.getBestAsk(), depth.getTimestamp());

        //		algorithmToNotify.onUpdateCandle(candle);
        notifyListeners(candle);
        lastTimestampMinuteAskCandle = date;
        openPriceMinuteAsk = depth.getBestAsk();
        maxPriceMinuteAsk = depth.getBestAsk();
        minPriceMinuteAsk = depth.getBestAsk();
    }

    private void generateDepthVolumeCandle(Depth depth) {

        Date date = new Date(depth.getTimestamp());
        if (openPriceVolumeDepthCandle == -1) {
            //first candle
            openPriceVolumeDepthCandle = depth.getMidPrice();
            maxPriceVolumeDepthCandle = depth.getMidPrice();
            minPriceVolumeDepthCandle = depth.getMidPrice();

            openVolumeVolumeDepthCandle = depth.getTotalVolume();
            maxVolumeDepthCandle = depth.getTotalVolume();
            minVolumeDepthCandle = depth.getTotalVolume();

            lastCumVolumeCandle = 0;//restart
            return;
        }

        maxPriceVolumeDepthCandle = Math.max(maxPriceVolumeDepthCandle, depth.getMidPrice());
        minPriceVolumeDepthCandle = Math.min(minPriceVolumeDepthCandle, depth.getMidPrice());

        maxVolumeDepthCandle = Math.max(maxVolumeDepthCandle, depth.getTotalVolume());
        minVolumeDepthCandle = Math.max(minVolumeDepthCandle, depth.getTotalVolume());

        assert maxPriceVolumeDepthCandle >= minPriceVolumeDepthCandle;
        assert maxPriceVolumeDepthCandle >= openPriceVolumeDepthCandle;
        assert maxPriceVolumeDepthCandle >= depth.getMidPrice();
        assert maxPriceVolumeDepthCandle <= openPriceVolumeDepthCandle;
        assert maxPriceVolumeDepthCandle <= depth.getMidPrice();

        assert maxVolumeDepthCandle >= depth.getTotalVolume();
        assert maxVolumeDepthCandle >= minVolumeDepthCandle;
        assert maxVolumeDepthCandle >= openVolumeVolumeDepthCandle;
        assert minVolumeDepthCandle <= depth.getTotalVolume();
        assert minVolumeDepthCandle <= openVolumeVolumeDepthCandle;

        Candle candle = new Candle(CandleType.volume_threshold_depth, instrumentPk, openPriceVolumeDepthCandle,
                maxPriceVolumeDepthCandle, minPriceVolumeDepthCandle, depth.getMidPrice(), maxVolumeDepthCandle,
                minVolumeDepthCandle, openVolumeVolumeDepthCandle, depth.getTotalVolume(), depth.getTimestamp());

        //		algorithmToNotify.onUpdateCandle(candle);
        notifyListeners(candle);
        lastCumVolumeCandle = 0;
        openVolumeVolumeDepthCandle = depth.getTotalVolume();
        maxVolumeDepthCandle = depth.getTotalVolume();
        minVolumeDepthCandle = depth.getTotalVolume();

        openPriceVolumeDepthCandle = depth.getMidPrice();
        maxPriceVolumeDepthCandle = depth.getMidPrice();
        minPriceVolumeDepthCandle = depth.getMidPrice();
    }

    private void generateTradeHourCandle(Trade trade) {

        Date date = new Date(trade.getTimestamp());
        if (openPriceHourTrade == -1) {
            //first candle
            openPriceHourTrade = trade.getPrice();
            maxPriceHourTrade = trade.getPrice();
            minPriceHourTrade = trade.getPrice();
            lastTimestampHourTradeCandle = date;
            return;
        }

        maxPriceHourTrade = Math.max(maxPriceHourTrade, trade.getPrice());
        minPriceHourTrade = Math.min(minPriceHourTrade, trade.getPrice());
        assert maxPriceHourTrade >= minPriceHourTrade;
        assert maxPriceHourTrade >= openPriceHourTrade;
        assert maxPriceHourTrade >= trade.getPrice();
        assert minPriceHourTrade <= openPriceHourTrade;
        assert minPriceHourTrade <= trade.getPrice();

        Candle candle = new Candle(CandleType.time_1_hour, instrumentPk, openPriceHourTrade, maxPriceHourTrade,
                minPriceHourTrade, trade.getPrice(), trade.getTimestamp());

        //		algorithmToNotify.onUpdateCandle(candle);
        notifyListeners(candle);
        lastTimestampHourTradeCandle = date;
        openPriceHourTrade = trade.getPrice();
        maxPriceHourTrade = trade.getPrice();
        minPriceHourTrade = trade.getPrice();
    }

    protected void notifyListeners(Candle candle) {
        for (CandleListener candleListener : observers) {
            candleListener.onCandleUpdate(candle);
        }
    }

    public boolean isNewCandle(Depth depth) {
        Date dateTest = new Date(depth.getTimestamp());
        return timeCandleManager.isTimeNewCandle(dateTest);
    }


    public synchronized boolean onDepthUpdate(Depth depth) {
        //minute candle
        if (!depth.getInstrument().equals(instrumentPk)) {
            return false;
        }
        timeCandleManager.onDepthUpdate(depth);
        boolean endOfCandlePeriod = timeCandleManager.isNewCandle();
        if (endOfCandlePeriod && !leaderThatNotifyTheRest) {//to be earlier than the rest
            TimeCandleClosed = true;
            generateBidSecondsCandle(depth);
            generateAskSecondsCandle(depth);
            generateMidSecondsCandle(depth);
        } else {
            TimeCandleClosed = false;
            try {
                maxPriceMinuteMid = Math.max(maxPriceMinuteMid, depth.getMidPrice());
                minPriceMinuteMid = Math.min(minPriceMinuteMid, depth.getMidPrice());
                maxPriceMinuteAsk = Math.max(maxPriceMinuteAsk, depth.getBestAsk());
                minPriceMinuteAsk = Math.min(minPriceMinuteAsk, depth.getBestAsk());
                maxPriceMinuteBid = Math.max(maxPriceMinuteBid, depth.getBestBid());
                minPriceMinuteBid = Math.min(minPriceMinuteBid, depth.getBestBid());
            } catch (IndexOutOfBoundsException e) {

            }
        }

        //// volume candle
        if (lastCumVolumeCandle == DEFAULT_MIN_PRICE) {
            //initial update
            generateDepthVolumeCandle(depth);
        }

        if (instrument.isFX()) {
            double totalVolume = depth.getBestAskQty() + depth.getBestBidQty();
            lastCumVolumeCandle += totalVolume;
            if (lastCumVolumeCandle > volumeThreshold) {
                generateDepthVolumeCandle(depth);
                lastCumVolumeCandle = 0;//just in case
            }
        }

        lastDepth = depth;
        return true;
    }

    public boolean onTradeUpdate(Trade trade) {
        if (!trade.getInstrument().equals(instrumentPk)) {
            return false;
        }
        Date date = new Date(trade.getTimestamp());

        //minute candle
        if (lastTimestampMinuteTradeCandle == null || date.getMinutes() != lastTimestampMinuteTradeCandle
                .getMinutes()) {
            generateTradeMinuteCandle(trade);
        } else {
            maxPriceMinuteTrade = Math.max(maxPriceMinuteTrade, trade.getPrice());
            minPriceMinuteTrade = Math.min(minPriceMinuteTrade, trade.getPrice());
        }

        //hour candle
        if (lastTimestampHourTradeCandle == null || date.getHours() != lastTimestampHourTradeCandle.getHours()) {
            generateTradeHourCandle(trade);
        } else {
            maxPriceHourTrade = Math.max(maxPriceHourTrade, trade.getPrice());
            minPriceHourTrade = Math.min(minPriceHourTrade, trade.getPrice());
        }

        //volume candle

        boolean weCreatedFirstDepth = lastCumVolumeCandle != DEFAULT_MIN_PRICE;
        if (!instrument.isFX() && weCreatedFirstDepth) {
            double totalVolume = trade.getQuantity();
            if (totalVolume <= 0) {
                logger.warn("ignored trade volume {}<=0  {}", totalVolume, trade);
            } else {
                lastCumVolumeCandle += totalVolume;
                if (lastCumVolumeCandle > volumeThreshold) {

                    generateDepthVolumeCandle(lastDepth);
                    lastCumVolumeCandle = 0;//just in case
                }
            }
        }

        return true;
    }
}