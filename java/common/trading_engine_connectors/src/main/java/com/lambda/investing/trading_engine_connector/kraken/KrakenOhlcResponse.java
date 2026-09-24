package com.lambda.investing.trading_engine_connector.kraken;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.Collections;
import java.util.List;

/**
 * Parsed payload of Kraken's public {@code GET /0/public/OHLC} endpoint.
 * <p>
 * Response shape: {@code {"error":[...], "result": {"<PAIRKEY>": [[time,open,high,low,close,vwap,volume,count], ...], "last": <unixSeconds>}}}
 *
 * @see <a href="https://docs.kraken.com/api/docs/rest-api/get-ohlc-data">Kraken OHLC docs</a>
 */
public class KrakenOhlcResponse {

    private final List<Object> error;
    private final String pairKey;
    private final JSONArray candles;
    private final long last;

    private KrakenOhlcResponse(List<Object> error, String pairKey, JSONArray candles, long last) {
        this.error = error;
        this.pairKey = pairKey;
        this.candles = candles;
        this.last = last;
    }

    public static KrakenOhlcResponse parse(String json) {
        JSONObject root = JSON.parseObject(json);
        JSONArray error = root.getJSONArray("error");
        JSONObject result = root.getJSONObject("result");
        if (result == null) {
            return new KrakenOhlcResponse(error, null, new JSONArray(), 0L);
        }
        long last = result.getLongValue("last");
        //the pair key is dynamic (eg. "XXBTZUSD", "XBTEUR", ...) -> the only other top level key is "last"
        String pairKey = result.keySet().stream().filter(key -> !"last".equals(key)).findFirst().orElse(null);
        JSONArray candles = pairKey == null ? new JSONArray() : result.getJSONArray(pairKey);
        return new KrakenOhlcResponse(error, pairKey, candles == null ? new JSONArray() : candles, last);
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    public List<Object> getError() {
        return error == null ? Collections.emptyList() : error;
    }

    public String getPairKey() {
        return pairKey;
    }

    public JSONArray getCandles() {
        return candles;
    }

    public long getLast() {
        return last;
    }
}
