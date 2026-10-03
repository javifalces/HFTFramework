package com.lambda.investing.algorithmic_trading.candle_manager;

import com.lambda.investing.model.asset.Currency;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the "missing first candle after start/restart" bug: the very first
 * {@code secondsThreshold} boundary crossed after the algorithm starts must still emit a candle,
 * not be silently swallowed while priming internal state.
 */
class CandleFromTickUpdaterInstrumentTest {

    private static final String INSTRUMENT_PK = "btceur_binance";
    private static final int SECONDS_THRESHOLD = 4 * 60 * 60; //4h, same as the live factor-investing setup

    private List<Candle> receivedCandles;
    private CandleFromTickUpdaterInstrument candleFromTickUpdaterInstrument;

    @BeforeEach
    void setUp() {
        if (Instrument.getInstrument(INSTRUMENT_PK) == null) {
            Instrument instrument = new Instrument();
            instrument.setPrimaryKey(INSTRUMENT_PK);
            instrument.setSymbol("btceur");
            instrument.setMarket("binance");
            instrument.setCurrency(Currency.EUR);
            instrument.setPriceTick(0.01);
            instrument.setQuantityTick(0.00001);
            instrument.setMakerFeePct(0.1);
            instrument.setTakerFeePct(0.1);
            instrument.addMap();
        }
        receivedCandles = new ArrayList<>();
        List<CandleListener> observers = new ArrayList<>();
        observers.add(candle -> receivedCandles.add(candle));
        candleFromTickUpdaterInstrument = new CandleFromTickUpdaterInstrument(observers, INSTRUMENT_PK,
                SECONDS_THRESHOLD, CandleFromTickUpdater.VOLUME_THRESHOLD_DEFAULT);
    }

    private Depth depthAt(long timestamp, double bid, double ask) {
        Depth depth = Depth.getInstance();
        depth.setInstrument(INSTRUMENT_PK);
        depth.setBids(new double[]{bid});
        depth.setAsks(new double[]{ask});
        depth.setBidsQuantities(new double[]{1.0});
        depth.setAsksQuantities(new double[]{1.0});
        depth.setTimestamp(timestamp);
        return depth;
    }

    /**
     * Reproduces the reported incident: the algorithm (re)starts mid-period (e.g. 05:47 for a 4h
     * candle whose first live boundary is 08:00), ticks flow in for ~2h until the boundary is
     * crossed. Before the fix, the first ever close would only "seed" open/high/low and return
     * without emitting a candle -> the whole first period was lost. After the fix, that first
     * boundary must emit exactly one mid/bid/ask candle each.
     */
    @Test
    void firstBoundaryAfterStartIsNotSkipped() {
        long startupTime = 1_759_132_020_000L; //Tue Sep 29 2026 05:47:00 UTC
        long firstBoundary = 1_759_132_020_000L + (2 * 60 + 13) * 60 * 1000L; //~08:00:00 UTC

        //ticks flowing in before the boundary: only prime state, must not emit anything yet
        candleFromTickUpdaterInstrument.onDepthUpdate(depthAt(startupTime, 100.0, 101.0));
        candleFromTickUpdaterInstrument.onDepthUpdate(depthAt(startupTime + 60_000, 99.0, 100.0));
        candleFromTickUpdaterInstrument.onDepthUpdate(depthAt(startupTime + 120_000, 102.0, 103.0));
        assertTrue(receivedCandles.isEmpty(), "no candle should be emitted before the first boundary is crossed");

        //this tick crosses the first live boundary -> must emit bid/ask/mid candles (in that order)
        candleFromTickUpdaterInstrument.onDepthUpdate(depthAt(firstBoundary, 105.0, 106.0));

        List<CandleType> emittedTypes = new ArrayList<>();
        for (Candle candle : receivedCandles) {
            emittedTypes.add(candle.getCandleType());
        }
        assertEquals(3, receivedCandles.size(), "the first boundary must emit bid/ask/mid candles, not be swallowed");
        assertTrue(emittedTypes.contains(CandleType.bid_time_seconds_threshold));
        assertTrue(emittedTypes.contains(CandleType.ask_time_seconds_threshold));
        assertTrue(emittedTypes.contains(CandleType.mid_time_seconds_threshold));

        for (Candle candle : receivedCandles) {
            if (candle.getCandleType() == CandleType.mid_time_seconds_threshold) {
                //open must reflect the first observed tick (100.5), not a degenerate single-price candle
                assertEquals(100.5, candle.getOpen(), 1e-9);
                assertEquals(105.5, candle.getHigh(), 1e-9); //max across the whole period, including the closing tick
                assertEquals(99.5, candle.getLow(), 1e-9); //from the (99.0,100.0) tick mid=99.5
                assertEquals(105.5, candle.getClose(), 1e-9); //closing tick mid=105.5
            }
        }

        //next boundary (12:00) must also emit candles as normal
        receivedCandles.clear();
        long secondBoundary = firstBoundary + SECONDS_THRESHOLD * 1000L;
        candleFromTickUpdaterInstrument.onDepthUpdate(depthAt(secondBoundary, 110.0, 111.0));
        assertEquals(3, receivedCandles.size(), "subsequent boundaries must keep emitting candles as before");
    }
}
