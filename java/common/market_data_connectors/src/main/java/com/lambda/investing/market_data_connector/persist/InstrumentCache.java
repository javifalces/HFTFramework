package com.lambda.investing.market_data_connector.persist;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class InstrumentCache {
    private Map<Long, Depth> depthCache;//last timestamp to depth
    private Map<Long, Trade> tradeCache;//last timestamp to trade
    private Instrument instrument;

    public InstrumentCache(Instrument instrument) {
        this.instrument = instrument;
        depthCache = new ConcurrentHashMap<>();
        tradeCache = new ConcurrentHashMap<>();
    }

    public synchronized void updateTrade(Trade trade) {
        tradeCache.put(trade.getTimestamp(), trade);
    }

    public synchronized void updateDepth(Depth depth) {
        depthCache.put(depth.getTimestamp(), depth);
    }

    public synchronized void cleanDepth(List<Long> timestamps) {
        for (Long timestamp : timestamps) {
            depthCache.remove(timestamp);
        }
    }

    public synchronized void cleanTrade(List<Long> timestamps) {
        for (Long timestamp : timestamps) {
            tradeCache.remove(timestamp);
        }
    }

    public static synchronized InstrumentCache copyOf(InstrumentCache instrumentCache) {
        //deep copy
        InstrumentCache copy = new InstrumentCache(instrumentCache.getInstrument());
//        copy.depthCache.putAll(instrumentCache.getDepthCache());
//        copy.tradeCache.putAll(instrumentCache.getTradeCache());
        return copy;
    }
}