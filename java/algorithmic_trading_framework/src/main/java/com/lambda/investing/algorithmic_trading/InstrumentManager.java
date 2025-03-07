package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import lombok.Setter;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class InstrumentManager {

    private static int BUFFER_CF_TRADES = 60;
    protected Logger logger = LogManager.getLogger(InstrumentManager.class);
    private Instrument instrument;
    private Map<String, ExecutionReport> allActiveOrders;//clientOrderId to Active execution report
    private Map<String, OrderRequest> allRequestOrders; // clientOrderId to orderRequest
    private Queue<String> cfTradesReceived = EvictingQueue.create(BUFFER_CF_TRADES);//clientOrderId or trades
    private Depth lastDepth;
    private Trade lastTrade;
    private double position;

    private Map<Verb, Long> lastTradeTimestamp;

    public InstrumentManager(Instrument instrument, boolean isBacktest) {
        this.instrument = instrument;
        if (!isBacktest) {
            new Thread(new MapManager(), instrument.getPrimaryKey() + "_instrumentManager").start();
        }

        reset();
    }

    public void reset() {
        allActiveOrders = new ConcurrentHashMap<>();
        allRequestOrders = new ConcurrentHashMap<>();
        cfTradesReceived = EvictingQueue.create(BUFFER_CF_TRADES);
        lastTradeTimestamp = new ConcurrentHashMap<>();

        //daily restart?
        position = 0;
    }

    public synchronized void setAllActiveOrders(Map<String, ExecutionReport> allActiveOrders) {
        this.allActiveOrders = allActiveOrders;
    }

    public synchronized void setAllRequestOrders(Map<String, OrderRequest> allRequestOrders) {
        this.allRequestOrders = allRequestOrders;
    }

    public synchronized void setCfTradesReceived(Queue<String> cfTradesReceived) {
        this.cfTradesReceived = cfTradesReceived;
    }

    private class MapManager implements Runnable {

        private boolean enable = true;

        @Override
        public void run() {
            while (enable) {
                Map<String, OrderRequest> requestOrdersCopy = new ConcurrentHashMap<>(getAllRequestOrders());
                boolean foundErrorsRequest = false;
                List<String> cfTrades = null;
                List<String> cfRequestsClOrdId = new ArrayList<>();
                //checking requestOrderMap is okey
                if (requestOrdersCopy.size() > 0) {
                    //check with active

                    for (String activeOrdersClientOrderId : getAllActiveOrders().keySet()) {
                        if (requestOrdersCopy.containsKey(activeOrdersClientOrderId)) {
                            //remove it
                            requestOrdersCopy.remove(activeOrdersClientOrderId);
                            cfRequestsClOrdId.add(activeOrdersClientOrderId);
                            foundErrorsRequest = true;
                        }
                    }
                    //check with trades
                    cfTrades = new ArrayList<>(getCfTradesReceived());
                    for (String cfTradesClientOrderId : cfTrades) {
                        if (requestOrdersCopy.containsKey(cfTradesClientOrderId)) {
                            //remove it
                            requestOrdersCopy.remove(cfTradesClientOrderId);
                            cfRequestsClOrdId.add(cfTradesClientOrderId);
                            foundErrorsRequest = true;
                        }
                    }

                    if (foundErrorsRequest) {
                        //correct it
                        //string of comma separated cfRequestsClOrdId
                        String cfRequestsClOrdIdString = String.join(",", cfRequestsClOrdId);
                        logger.warn("Found requests in {} already received as active/trade , clean it: {}", instrument.getPrimaryKey(), cfRequestsClOrdIdString);
                        setAllRequestOrders(requestOrdersCopy);
                    }
                }
                //
                boolean foundErrorsActive = false;
                Map<String, ExecutionReport> activeOrdersCopy = new ConcurrentHashMap<>(getAllActiveOrders());
                List<String> cfTradesClOrdId = new ArrayList<>();
                if (activeOrdersCopy.size() > 0) {
                    if (cfTrades == null) {
                        cfTrades = new ArrayList<>(getCfTradesReceived());
                    }

                    for (String cfClientOrderId : cfTrades) {
                        if (activeOrdersCopy.containsKey(cfClientOrderId)) {
                            activeOrdersCopy.remove(cfClientOrderId);
                            cfTradesClOrdId.add(cfClientOrderId);
                            foundErrorsActive = true;
                        }
                    }
                }
                if (foundErrorsActive) {
                    //correct it
                    //string of comma separated cfTradesClOrdId
                    String cfTradesClOrdIdString = String.join(",", cfTradesClOrdId);

                    logger.warn("Found active in clOrdId {} already traded , clean it: {}", instrument.getPrimaryKey(), cfTradesClOrdIdString);
                    setAllActiveOrders(activeOrdersCopy);
                }

                Thread.onSpinWait();//to not occupy the cpu
//					Thread.sleep(10);
            }
        }
    }
}
