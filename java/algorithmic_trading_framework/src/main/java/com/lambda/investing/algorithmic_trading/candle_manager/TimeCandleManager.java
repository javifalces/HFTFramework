package com.lambda.investing.algorithmic_trading.candle_manager;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;

import java.util.*;

/**
 * Tracks time-based candle boundaries for a single {@link Instrument} using a fixed
 * {@code secondsThreshold} interval.
 * <p>
 * How the boundary list is built and walked:
 * <ul>
 *   <li>{@link #candleTimes} is <b>not</b> pre-computed for a whole day/week up front anymore.
 *       {@link #setCandleTimes()} just (re)seeds it with a single boundary — midnight of the new
 *       {@link #startOfDayDate} — whenever a new day is detected.</li>
 *   <li>{@link #currentCandleTimesIndex} points at the next boundary to close in
 *       {@link #candleTimes}. Each time a candle closes, {@link #iterLastNextCandleTime(Date)}
 *       advances the index (skipping over gaps if ticks arrived late/out of order) and, as soon as
 *       the index would run past the end of the list, computes the next boundary on demand
 *       ({@code lastBoundary + secondsThreshold}) and appends it there and then. Boundaries are
 *       therefore always generated just-in-time as they are consumed, one step ahead, instead of a
 *       fixed-size list that can be exhausted.</li>
 *   <li>Once {@link #iterLastNextCandleTime(Date)} finds the current boundary, it drops the
 *       already-consumed entries from {@link #candleTimes} so the list never grows unbounded.</li>
 * </ul>
 * Because boundaries are grown on demand rather than capped to a pre-computed window, there is no
 * upper limit to "hit" and the previous wrap-around/reset-to-0 WARN is no longer needed.
 */
public class TimeCandleManager {

    private Instrument instrument;
    protected int secondsThreshold;

    private Depth lastDepth = null;

    /**
     * Start of the current UTC calendar day (00:00:00), recomputed whenever a new day is detected.
     */
    private Date startOfDayDate = null;
    /** Boundary timestamps generated so far, spaced {@code secondsThreshold} apart; grown on demand
     *  by {@link #iterLastNextCandleTime(Date)} and trimmed once consumed, so it never holds more
     *  than a couple of entries at a time. */
    private List<Date> candleTimes = new ArrayList<>();
    private List<Date> completeCandleTimes = new ArrayList<>();
    /** The next boundary timestamp at/after which the current candle is considered closed. */
    private Date nextCandleTimeKey = null;
    /** Index into {@link #candleTimes} for {@link #nextCandleTimeKey}. */
    private int currentCandleTimesIndex = 0;

    public TimeCandleManager(Instrument instrument, int secondsThreshold) {
        this.instrument = instrument;
        this.secondsThreshold = secondsThreshold;
    }

    /**
     * Reseeds {@link #candleTimes} with a single boundary — midnight of the new
     * {@link #startOfDayDate} — and resets the walk state ({@link #nextCandleTimeKey},
     * {@link #currentCandleTimesIndex}) so {@link #iterLastNextCandleTime(Date)} starts searching
     * from that new anchor. Subsequent boundaries are generated lazily, one at a time, as they are
     * consumed (see {@link #iterLastNextCandleTime(Date)}), instead of being pre-computed here.
     */
    private void setCandleTimes() {
        candleTimes.clear();
        nextCandleTimeKey = null;
        currentCandleTimesIndex = -1;//reset so iterLastNextCandleTime starts at 0 for the new day
        candleTimes.add(new Date(startOfDayDate.getTime()));//seed with the first boundary of the day
    }

    /**
     * Advances {@link #currentCandleTimesIndex}/{@link #nextCandleTimeKey} forward through
     * {@link #candleTimes} until it finds the next boundary that is still in the future relative
     * to {@code date} (skipping over any boundaries already passed, e.g. after a gap in ticks).
     * <p>
     * Boundaries are created just-in-time: as soon as the index would run past the end of
     * {@link #candleTimes}, the next boundary ({@code lastBoundary + secondsThreshold}) is computed
     * and appended right there, so the list is always extended exactly as it is consumed and there
     * is no fixed limit to run past. Once the current boundary is found, already-consumed entries
     * are dropped from the front of the list to keep it small.
     */
    private void iterLastNextCandleTime(Date date) {
        do {
            currentCandleTimesIndex++;//start -1
            if (currentCandleTimesIndex >= candleTimes.size()) {
                //grow the list on demand instead of wrapping around a fixed-size window
                Date lastCandleTime = candleTimes.get(candleTimes.size() - 1);
                Date newCandleTime = new Date(lastCandleTime.getTime() + secondsThreshold * 1000L);
                candleTimes.add(newCandleTime);
            }
            nextCandleTimeKey = candleTimes.get(currentCandleTimesIndex);
        } while (nextCandleTimeKey.getTime() < date.getTime());//just in case we are starting in the middle of the day or big gap

        //drop already-consumed boundaries now that the current one was found, keeping the list small
        if (currentCandleTimesIndex > 0) {
            candleTimes.subList(0, currentCandleTimesIndex).clear();
            currentCandleTimesIndex = 0;
        }
    }

    public void onDepthUpdate(Depth depth) {
        Date date = depth.getDate();
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
