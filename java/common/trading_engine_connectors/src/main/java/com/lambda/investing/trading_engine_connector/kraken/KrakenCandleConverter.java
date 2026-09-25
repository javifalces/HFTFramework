package com.lambda.investing.trading_engine_connector.kraken;

import com.alibaba.fastjson2.JSONArray;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.xchange.XChangeBrokerConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.CurrencyPair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts between HFTFramework instrument primary keys/{@link Candle} and Kraken's public REST OHLC
 * representation.
 */
public class KrakenCandleConverter {

    private static final Logger logger = LogManager.getLogger(KrakenCandleConverter.class);

    //Kraken's classic asset codes rename some currencies for its pair symbology (eg. BTC -> XBT).
    private static final Map<String, String> CURRENCY_CODE_ALIASES = Map.of("BTC", "XBT");

    //Only these minute intervals are accepted by Kraken's /public/OHLC endpoint.
    private static final int[] SUPPORTED_INTERVAL_MINUTES = {1, 5, 15, 30, 60, 240, 1440, 10080, 21600};// 1m, 5m, 15m, 30m, 1h, 4h, 1d, 1w, 15d

    private KrakenCandleConverter() {
    }

    /**
     * @return the Kraken asset pair code for the instrument (eg. "XBTEUR"), or null if it can't be mapped.
     */
    public static String toKrakenPair(String instrumentPk) {
        CurrencyPair currencyPair = XChangeBrokerConnector.getCurrencyPair(instrumentPk);
        if (currencyPair == null) {
            return null;
        }
        return toKrakenCurrencyCode(currencyPair.getBase().getCurrencyCode()) + toKrakenCurrencyCode(
                currencyPair.getCounter().getCurrencyCode());
    }

    private static String toKrakenCurrencyCode(String currencyCode) {
        return CURRENCY_CODE_ALIASES.getOrDefault(currencyCode, currencyCode);
    }

    /**
     * Rounds the requested candle width (in seconds) to the nearest interval Kraken's OHLC endpoint supports.
     * Logs an error when the requested width doesn't exactly match a supported interval.
     */
    public static int resolveIntervalMinutes(int secondsCandles) {
        int requestedMinutes = Math.max(1, secondsCandles / 60);
        int closest = SUPPORTED_INTERVAL_MINUTES[0];
        int closestDiff = Math.abs(requestedMinutes - closest);
        for (int candidateMinutes : SUPPORTED_INTERVAL_MINUTES) {
            int diff = Math.abs(requestedMinutes - candidateMinutes);
            if (diff < closestDiff) {
                closest = candidateMinutes;
                closestDiff = diff;
            }
        }
        if (closestDiff != 0) {
            logger.error(
                    "secondsCandles {} ({} minutes) is not a Kraken-supported OHLC interval {} -> falling back to closest interval {} minutes",
                    secondsCandles, requestedMinutes, SUPPORTED_INTERVAL_MINUTES, closest);
        }
        return closest;
    }

    /**
     * Maps the OHLC rows of a {@link KrakenOhlcResponse} to {@link Candle}s.
     * Row shape: [time, open, high, low, close, vwap, volume, count]. Kraken doesn't report per-tick
     * open/high/low/close volumes, so the traded volume of the bar is used for all volume fields.
     * Kraken's "time" field is the bar's <b>open</b> timestamp; the live {@code CandleFromTickUpdaterInstrument}
     * stamps candles with their <b>close</b> timestamp, so {@code intervalMinutes} is added to keep both aligned.
     */
    public static List<Candle> toCandles(KrakenOhlcResponse response, CandleType candleType, String instrumentPk,
                                         int intervalMinutes) {
        List<Candle> candles = new ArrayList<>();
        JSONArray rawCandles = response.getCandles();
        if (rawCandles == null) {
            return candles;
        }
        long periodMillis = intervalMinutes * 60_000L;
        for (int i = 0; i < rawCandles.size(); i++) {
            JSONArray entry = rawCandles.getJSONArray(i);
            long timestampMillis = entry.getLongValue(0) * 1000L + periodMillis;
            double open = entry.getDoubleValue(1);
            double high = entry.getDoubleValue(2);
            double low = entry.getDoubleValue(3);
            double close = entry.getDoubleValue(4);
            double volume = entry.getDoubleValue(6);
            candles.add(new Candle(candleType, instrumentPk, open, high, low, close, volume, volume, volume, volume,
                    timestampMillis));
        }
        return candles;
    }
}
