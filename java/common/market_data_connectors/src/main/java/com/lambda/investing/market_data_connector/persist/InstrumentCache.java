package com.lambda.investing.market_data_connector.persist;

import com.lambda.investing.TimeSeriesQueue;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public class InstrumentCache {
    private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(InstrumentCache.class);
    private Map<Long, Depth> depthCache;//last timestamp to depth
    private Map<Long, Trade> tradeCache;//last timestamp to trade
    private Instrument instrument;
    private long syncTradesWithDepthMaxMsSearch = 500;
    protected TimeSeriesQueue<Depth> depthSyncCache;
    private static int DEPTH_SYNC_CACHE_SIZE = 1000;
    private final Object lockDepthSyncCache = new Object();

    private AtomicInteger tradesReceived = new AtomicInteger(0);
    private AtomicInteger tradesSync = new AtomicInteger(0);

    public InstrumentCache(Instrument instrument) {
        this.instrument = instrument;
        depthCache = new ConcurrentHashMap<>();
        tradeCache = new ConcurrentHashMap<>();
        depthSyncCache = new TimeSeriesQueue(DEPTH_SYNC_CACHE_SIZE);
    }

    public double getTradesSyncRatio() {
        if (tradesReceived.get() == 0) {
            return 1.0;
        }
        return tradesSync.get() / (double) tradesReceived.get();
    }

    public void clearSyncRatio() {
        tradesReceived.set(0);
        tradesSync.set(0);
    }

    private Trade synchronizeTradePastDepth(Trade tradeToAllign, long maxMsSearch) {
        //iterate Depth from most recent to oldest
        double tradePrice = tradeToAllign.getPrice();
        if (depthSyncCache.isEmpty()) {
            return tradeToAllign;
        }
        for (Depth depth : new HashSet<>(depthSyncCache.getListFirstNewest())) {
            boolean depthIsAfterTrade = depth.getTimestamp() > tradeToAllign.getTimestamp();
            if (depthIsAfterTrade) {
                continue;
            }

            //possible to be in diferent order? better not to skip directly...
            boolean depthIsBeforeMaxMsSearch = depth.getTimestamp() < tradeToAllign.getTimestamp() - maxMsSearch;
            if (depthIsBeforeMaxMsSearch) {
                continue;
            }

            //copied logic from OrderMatchEngine.inferVerbFromTrade
            double bestBid = depth.getBids()[0];
            double bestAsk = depth.getAsks()[0];

            if (tradePrice <= bestBid) {
                tradeToAllign.setVerb(Verb.Sell);
//                logger.info("{} {} Trade at {} to {} aligned with depth from {} to {}", tradeToAllign.getVerb(), tradeToAllign.getInstrument(), tradePrice, bestBid, new Date(tradeToAllign.getTimestamp()), new Date(depth.getTimestamp()));
                tradeToAllign.setPrice(bestBid);
                tradeToAllign.setTimestamp(depth.getTimestamp() - 1);
                tradesSync.incrementAndGet();
                return tradeToAllign;
            }

            if (tradePrice >= bestAsk) {
                tradeToAllign.setVerb(Verb.Buy);
                tradeToAllign.setPrice(bestAsk);
//                logger.info("{} {} Trade at {} to {} aligned with depth from {} to {}", tradeToAllign.getVerb(), tradeToAllign.getInstrument(), tradePrice, bestAsk, new Date(tradeToAllign.getTimestamp()), new Date(depth.getTimestamp()));
                tradeToAllign.setTimestamp(depth.getTimestamp() - 1);
                tradesSync.incrementAndGet();
                return tradeToAllign;
            }

        }
//        logger.warn("{} {} Trade at {} not found depth to align in {} depths saved in cache  -> keep trade timestamp", tradeToAllign.getVerb(), tradeToAllign.getInstrument(), tradePrice, maxMsSearch);
        return tradeToAllign;
    }

    public synchronized void updateTrade(Trade trade) {

        if (syncTradesWithDepthMaxMsSearch > 0) {
            trade = synchronizeTradePastDepth(trade, syncTradesWithDepthMaxMsSearch);
        }
        tradesReceived.incrementAndGet();
        tradeCache.put(trade.getTimestamp(), trade);
    }

    public synchronized void updateDepth(Depth depth) {
        depthCache.put(depth.getTimestamp(), depth);

        if (syncTradesWithDepthMaxMsSearch > 0) {
            synchronized (lockDepthSyncCache) {
                depthSyncCache.offer(depth);
            }
        }

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