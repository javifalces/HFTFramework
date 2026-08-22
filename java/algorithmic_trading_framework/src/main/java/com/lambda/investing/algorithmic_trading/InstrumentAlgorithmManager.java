package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.Configuration;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import lombok.Setter;
import net.openhft.affinity.AffinityLock;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class InstrumentAlgorithmManager {

    private static int BUFFER_CF_TRADES = 60;
    protected Logger logger = LogManager.getLogger(InstrumentAlgorithmManager.class);

    /**
     * One shared {@link InstrumentOrderWatchdog} background thread per instrument primary key, reused by
     * every {@code InstrumentOrderManager} instance created for that instrument (e.g. the same
     * instrument traded/hedged by several algorithm instances in a {@link MultiAlgorithm}).
     * Without this, each instance used to spin up its own dedicated busy-spin thread
     * ({@code Thread.onSpinWait()} loop) named "{instrumentPk}_instrumentOrderManager",
     * wasting a CPU core per duplicate instance for the same instrument.
     */
    private static final Map<String, InstrumentOrderWatchdog> SHARED_INSTRUMENT_ORDER_WATCHDOGS = new ConcurrentHashMap<>();

    private Instrument instrument;
    private Map<String, ExecutionReport> allActiveOrders;//clientOrderId to Active execution report
    private Map<String, OrderRequest> allRequestOrders; // clientOrderId to orderRequest
    private Queue<String> cfTradesReceived = EvictingQueue.create(BUFFER_CF_TRADES);//clientOrderId or trades
    private Depth lastDepth;
    private Trade lastTrade;


    private Map<Verb, Long> lastTradeTimestamp;

    public InstrumentAlgorithmManager(Instrument instrument, boolean isBacktest) {
        this.instrument = instrument;
        reset();
        if (!isBacktest) {
            registerInSharedMapManager();
        }
    }

    /**
     * Registers this instance in the shared {@link InstrumentOrderWatchdog} for its instrument primary key,
     * creating (and starting) that manager's thread the first time the instrument is seen.
     * Subsequent {@code InstrumentOrderManager} instances for the same instrument reuse the
     * already-running thread instead of starting a new one.
     */
    private void registerInSharedMapManager() {
        String instrumentPk = instrument.getPrimaryKey();
        InstrumentOrderWatchdog instrumentOrderWatchdog = SHARED_INSTRUMENT_ORDER_WATCHDOGS.computeIfAbsent(instrumentPk, pk -> {
            InstrumentOrderWatchdog newInstrumentOrderWatchdog = new InstrumentOrderWatchdog(pk);
            Thread thread = new Thread(newInstrumentOrderWatchdog::runAffinity, pk + "_instrumentOrderWatchdog");
            thread.setDaemon(true);
            thread.start();
            return newInstrumentOrderWatchdog;
        });
        instrumentOrderWatchdog.register(this);
    }

    public void reset() {
        allActiveOrders = new ConcurrentHashMap<>();
        allRequestOrders = new ConcurrentHashMap<>();
        cfTradesReceived = EvictingQueue.create(BUFFER_CF_TRADES);
        lastTradeTimestamp = new ConcurrentHashMap<>();
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

    /**
     * Shared background worker, one per instrument primary key, that periodically reconciles
     * every {@link InstrumentAlgorithmManager} instance registered for that instrument (there can be
     * more than one when several algorithm instances trade/hedge the same instrument).
     */
    private static class InstrumentOrderWatchdog implements Runnable {

        private static final Logger LOGGER = LogManager.getLogger(InstrumentOrderWatchdog.class);

        private final String instrumentPk;
        private final List<InstrumentAlgorithmManager> managedInstances = new CopyOnWriteArrayList<>();
        private volatile boolean enable = true;

        private InstrumentOrderWatchdog(String instrumentPk) {
            this.instrumentPk = instrumentPk;
        }

        private void register(InstrumentAlgorithmManager instrumentAlgorithmManager) {
            managedInstances.add(instrumentAlgorithmManager);
        }

        public void runAffinity() {
            try (AffinityLock al = AffinityLock.acquireLock(Configuration.GET_AFFINITY_CPUS())) {
                run();
            } catch (Configuration.LambdaConfigurationException e) {
                run();
            } catch (Exception e) {
                LOGGER.warn("error AffinityLock ", e);
                if (Configuration.IS_LINUX) {
                    System.err.println("error AffinityLock  -> " + e.toString());
                }
                run();
            }
        }

        @Override
        public void run() {
            while (enable) {
                for (InstrumentAlgorithmManager instrumentAlgorithmManager : managedInstances) {
                    reconcile(instrumentAlgorithmManager);
                }
                Thread.onSpinWait();//to not occupy the cpu
//					Thread.sleep(10);
            }
        }

        private void reconcile(InstrumentAlgorithmManager instrumentAlgorithmManager) {
            Map<String, OrderRequest> requestOrdersCopy = new ConcurrentHashMap<>(instrumentAlgorithmManager.getAllRequestOrders());
            boolean foundErrorsRequest = false;
            List<String> cfTrades = null;
            List<String> cfRequestsClOrdId = new ArrayList<>();
            //checking requestOrderMap is okey
            if (!requestOrdersCopy.isEmpty()) {
                //check with active

                for (String activeOrdersClientOrderId : instrumentAlgorithmManager.getAllActiveOrders().keySet()) {
                    if (requestOrdersCopy.containsKey(activeOrdersClientOrderId)) {
                        //remove it
                        requestOrdersCopy.remove(activeOrdersClientOrderId);
                        cfRequestsClOrdId.add(activeOrdersClientOrderId);
                        foundErrorsRequest = true;
                    }
                }
                //check with trades
                cfTrades = new ArrayList<>(instrumentAlgorithmManager.getCfTradesReceived());
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
                    LOGGER.warn("Found requests in {} already received as active/trade , clean it: {}", instrumentPk, cfRequestsClOrdIdString);
                    instrumentAlgorithmManager.setAllRequestOrders(requestOrdersCopy);
                }
            }
            //
            boolean foundErrorsActive = false;
            Map<String, ExecutionReport> activeOrdersCopy = new ConcurrentHashMap<>(instrumentAlgorithmManager.getAllActiveOrders());
            List<String> cfTradesClOrdId = new ArrayList<>();
            if (!activeOrdersCopy.isEmpty()) {
                if (cfTrades == null) {
                    cfTrades = new ArrayList<>(instrumentAlgorithmManager.getCfTradesReceived());
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

                LOGGER.warn("Found active in clOrdId {} already traded , clean it: {}", instrumentPk, cfTradesClOrdIdString);
                instrumentAlgorithmManager.setAllActiveOrders(activeOrdersCopy);
            }
        }
    }
}

