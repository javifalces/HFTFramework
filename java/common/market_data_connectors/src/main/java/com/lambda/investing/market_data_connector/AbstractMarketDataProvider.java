package com.lambda.investing.market_data_connector;


import com.lambda.investing.LatencyStatistics;
import com.lambda.investing.Statistics;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public abstract class AbstractMarketDataProvider implements MarketDataProvider {

    protected static int QUEUE_FINAL_STATES_SIZE = 300;
    public static boolean CHECK_TIMESTAMPS_RECEIVED = true;//checking if the last timestamp is this one!
    private Map<String, Long> lastDepthReceived;
    private Map<String, Long> lastTradeSentReceived;

    protected Statistics statisticsReceived;//= new Statistics("Data received", 15 * 1000);
    protected LatencyStatistics latencyStatistics;
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

    public void setLatencyStatistics(LatencyStatistics latencyStatistics) {
        this.latencyStatistics = latencyStatistics;
    }

    @Override
    public void register(MarketDataListener listener) {
        listenersManager.put(listener, "");
    }


    @Override
    public void deregister(MarketDataListener listener) {
        listenersManager.remove(listener);
    }

    public void notifyDepth(Depth depth) {
        if (CHECK_TIMESTAMPS_RECEIVED && depth.getTimestamp() < lastDepthReceived
                .getOrDefault(depth.getInstrument(), 0L)) {
            //not the last snapshot
            depth.delete();
            return;
        }


        Set<MarketDataListener> listeners = listenersManager.keySet();
        if (!listeners.isEmpty()) {
            try {
                //if we dont clone the depth reference is modified and the strategy will not work
                Depth depthCloned = (Depth) depth.clone();//clone to avoid future changes object affects strategy
                for (MarketDataListener marketDataListener : listeners) {
                    marketDataListener.onDepthUpdate(depthCloned);
                }
            } catch (Exception e) {
                logger.error("Error notifyDepth depth", e);
                System.err.println("Error notifyDepth depth");
                e.printStackTrace();
            }
        }
        lastDepthReceived.put(depth.getInstrument(), depth.getTimestamp());
        depth.delete();
    }


    public void notifyTrade(Trade trade) {
        if (CHECK_TIMESTAMPS_RECEIVED && trade.getTimestamp() < lastTradeSentReceived
                .getOrDefault(trade.getInstrument(), 0L)) {
            //not the last snapshot
            trade.delete();
            return;
        }
        Set<MarketDataListener> listeners = listenersManager.keySet();
        if (!listeners.isEmpty()) {
            try {
                Trade tradeClone = (Trade) trade.clone();//clone to avoid future changes object affects strategy
                for (MarketDataListener marketDataListener : listeners) {
                    marketDataListener.onTradeUpdate(tradeClone);
                }
            } catch (Exception e) {
                logger.error("Error cloning trade", e);
                System.err.println("Error cloning trade");
                e.printStackTrace();
            }
        }
        lastTradeSentReceived.put(trade.getInstrument(), trade.getTimestamp());
        trade.delete();

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
