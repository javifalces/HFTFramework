package com.lambda.investing.algorithmic_trading.candle_manager;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;

import java.util.*;

import static com.lambda.investing.algorithmic_trading.candle_manager.CandleFromTickUpdater.logger;

public class TimeCandleManager {

    private Instrument instrument;
    protected int secondsThreshold;

    private Depth lastDepth = null;


    private Date startOfDayDate = null;
    private List<Date> candleTimes = new ArrayList<>();
    private List<Date> completeCandleTimes = new ArrayList<>();
    private Date nextCandleTimeKey = null;
    private int currentCandleTimesIndex = 0;

    public TimeCandleManager(Instrument instrument, int secondsThreshold) {
        this.instrument = instrument;
        this.secondsThreshold = secondsThreshold;
    }

    private void setCandleTimes() {
        Date iterDate = new Date(startOfDayDate.getTime());
        candleTimes.clear();
        nextCandleTimeKey = null;
        while (iterDate.getDay() == startOfDayDate.getDay()) {
            candleTimes.add(iterDate);
            Date nextDate = new Date(iterDate.getTime() + secondsThreshold * 1000);
            iterDate = nextDate;
        }

        //add next day just in case
        candleTimes.add(iterDate);

    }

    private void iterLastNextCandleTime(Date date) {
        do {
            currentCandleTimesIndex++;//start -1
            if (currentCandleTimesIndex >= candleTimes.size()) {
                logger.warn("currentCandleTimesIndex {} >=candleTimes.size() {}  searching {} -> currentCandleTimesIndex=0", currentCandleTimesIndex, candleTimes.size(), date);
                currentCandleTimesIndex = 0;
            }
            nextCandleTimeKey = candleTimes.get(currentCandleTimesIndex);
        } while (nextCandleTimeKey.getTime() < date.getTime());//just in case we are starting in the middle of the day or big gap

//        if (candleTimes.size() > currentCandleTimesIndex + 1) {
//            nextCandleTime = candleTimes.get(currentCandleTimesIndex+1 );
//        } else {
//            logger.warn("candleTimes.size()<=currentCandleTimesIndex+1 => nextCandleTime= {}", nextCandleTimeKey);
//        }
    }

    public void onDepthUpdate(Depth depth) {
        Date date = new Date(depth.getTimestamp());
        boolean isNewDay = startOfDayDate == null || (date.getDay() > startOfDayDate.getDay());
        if (isNewDay) {
            startOfDayDate = new Date(date.getYear(), date.getMonth(), date.getDate(), 0, 0, 0);
            setCandleTimes();
        }

        if (nextCandleTimeKey == null) {
            iterLastNextCandleTime(date);
        }


        lastDepth = depth;
    }


    public boolean isNewCandle() {
        if (lastDepth == null) {
            return false;
        }
        Date date = new Date(lastDepth.getTimestamp());
        boolean currentTimeIsAfterLastCandleTime = isTimeNewCandle(date);
        if (currentTimeIsAfterLastCandleTime) {
            completeCandleTimes.add(nextCandleTimeKey);
            iterLastNextCandleTime(date);
            return true;
        }

        return false;
    }

    public boolean isTimeNewCandle(Date date) {
        if (nextCandleTimeKey == null) {
            return false;
        }
        boolean currentTimeIsAfterLastCandleTime = date.getTime() >= nextCandleTimeKey.getTime();
        return currentTimeIsAfterLastCandleTime;
    }


}
