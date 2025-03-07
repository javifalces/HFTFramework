package com.lambda.investing.market_data_connector;


import com.lambda.investing.connector.zero_mq.ZeroMqPublisher;
import com.lambda.investing.market_data_connector.ordinary.OrdinaryMarketDataProvider;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractMarketDataProvider implements MarketDataProvider {

    protected static int QUEUE_FINAL_STATES_SIZE = 300;
    public static boolean CHECK_TIMESTAMPS_RECEIVED = true;//checking if the last timestamp is this one!
    private Map<String, Long> lastDepthReceived;
    private Map<String, Long> lastTradeSentReceived;

    protected Statistics statisticsReceived;//= new Statistics("Data received", 15 * 1000);

    protected Logger logger = LogManager.getLogger(AbstractMarketDataProvider.class);
    protected Map<MarketDataListener, String> listenersManager;

    public AbstractMarketDataProvider() {
        listenersManager = new HashMap<>();
        lastDepthReceived = new HashMap<>();
        lastTradeSentReceived = new HashMap<>();
    }

    public void reset() {
        lastDepthReceived.clear();
        lastTradeSentReceived.clear();
    }

    public void setStatisticsReceived(Statistics statisticsReceived) {
        this.statisticsReceived = statisticsReceived;
    }

    @Override
    public void register(MarketDataListener listener) {
        listenersManager.put(listener, "");
    }


    @Override public void deregister(MarketDataListener listener) {
        listenersManager.remove(listener);
    }

    public void notifyDepth(Depth depth) {
        if (CHECK_TIMESTAMPS_RECEIVED && depth.getTimestamp() < lastDepthReceived
                .getOrDefault(depth.getInstrument(), 0L)) {
            //not the last snapshot
            return;
        }

        Set<MarketDataListener> listeners = listenersManager.keySet();
        for (MarketDataListener marketDataListener : listeners) {
            marketDataListener.onDepthUpdate(depth);
        }

        lastDepthReceived.put(depth.getInstrument(), depth.getTimestamp());

    }

    public void notifyTrade(Trade trade) {
        if (CHECK_TIMESTAMPS_RECEIVED && trade.getTimestamp() < lastTradeSentReceived
                .getOrDefault(trade.getInstrument(), 0L)) {
            //not the last snapshot
            return;
        }
        Set<MarketDataListener> listeners = listenersManager.keySet();
        for (MarketDataListener marketDataListener : listeners) {
            marketDataListener.onTradeUpdate(trade);
        }

        lastTradeSentReceived.put(trade.getInstrument(), trade.getTimestamp());

    }

    public void notifyCommand(Command command) {
        Set<MarketDataListener> listeners = listenersManager.keySet();
        for (MarketDataListener marketDataListener : listeners) {
            marketDataListener.onCommandUpdate(command);
        }

    }


    public void notifyInfo(String header, String message) {
        Set<MarketDataListener> listeners = listenersManager.keySet();
        for (MarketDataListener marketDataListener : listeners) {
            marketDataListener.onInfoUpdate(header, message);
        }

    }


}
