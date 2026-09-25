package com.lambda.investing.trading_engine_connector.kraken;

import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class KrakenCandleConverterTest {

    private static final String SAMPLE_OHLC_JSON = "{\"error\":[],\"result\":{\"XXBTZUSD\":["
            + "[1688671200,\"30306.1\",\"30306.2\",\"30305.7\",\"30305.7\",\"30306.1\",\"3.39243896\",23],"
            + "[1688671260,\"30304.5\",\"30304.5\",\"30300.0\",\"30300.0\",\"30300.0\",\"4.42996871\",18]"
            + "],\"last\":1688671260}}";

    @Test
    public void toKrakenPair_mapsBtcToXbt() {
        assertEquals("XBTEUR", KrakenCandleConverter.toKrakenPair("btceur"));
        assertEquals("XBTUSD", KrakenCandleConverter.toKrakenPair("btcusd"));
    }

    @Test
    public void toKrakenPair_unknownInstrument_returnsNull() {
        assertNull(KrakenCandleConverter.toKrakenPair("not_a_real_symbol"));
    }

    @Test
    public void resolveIntervalMinutes_roundsToNearestSupportedInterval() {
        assertEquals(1, KrakenCandleConverter.resolveIntervalMinutes(30));
        assertEquals(5, KrakenCandleConverter.resolveIntervalMinutes(300));
        assertEquals(60, KrakenCandleConverter.resolveIntervalMinutes(3600));
        assertEquals(1440, KrakenCandleConverter.resolveIntervalMinutes(86400));
    }

    @Test
    public void toCandles_mapsOhlcRowsToCandles() {
        KrakenOhlcResponse response = KrakenOhlcResponse.parse(SAMPLE_OHLC_JSON);
        List<Candle> candles = KrakenCandleConverter.toCandles(response, CandleType.time_1_min, "btcusd", 1);

        assertEquals(2, candles.size());
        Candle first = candles.get(0);
        assertEquals(1688671260000L, first.getTimestamp());
        assertEquals(30306.1, first.getOpen(), 1e-9);
        assertEquals(30306.2, first.getHigh(), 1e-9);
        assertEquals(30305.7, first.getLow(), 1e-9);
        assertEquals(30305.7, first.getClose(), 1e-9);
        assertEquals(3.39243896, first.getCloseVolume(), 1e-9);
        assertEquals("btcusd", first.getInstrumentPk());
        assertEquals(CandleType.time_1_min, first.getCandleType());
    }
}
