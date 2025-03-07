package com.lambda.investing.interactive_brokers;

import com.ib.client.*;
import com.lambda.investing.interactive_brokers.listener.IBListener;
import com.lambda.investing.model.Market;
import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lambda.investing.interactive_brokers.ContractFactory.createContract;

@Getter

public class InteractiveBrokersBrokerConnector extends IBListener {
    public static List<String> INTERACTIVE_BROKERS_MARKETS_LIST = Arrays.asList(new String[]{Market.Idealpro.name().toLowerCase(), Market.Smart.name().toLowerCase(), Market.Arca.name().toLowerCase(), Market.Paxos.name().toLowerCase()});
    private static String GENERIC_TICK_LIST = "100,101,104,106,165,221,232,236,258,293,294,295,318,411,460,619";
    private String ibHost;
    private int ibPort;
    private int ibClientId;

    @Setter
    private boolean smartDepth = true;

    @Setter
    private boolean subscribeAllDepth = false;//need to pay it

    @Getter
    private EClientSocket clientSocket;

    private EJavaSignal signalListener = new EJavaSignal();
    private EReader reader;

    private List<EWrapper> iblisteners = new ArrayList<>();

    private static AtomicInteger tickerId = new AtomicInteger(0);
    private List<Integer> SubscriptionsDepthTickerId = new ArrayList<>();
    private List<Integer> SubscriptionsTickTickerId = new ArrayList<>();

    private Map<Integer, String> TickerIdInstrumentPk;
    private Map<String, Contract> InstrumentPkContractMap;

    protected static Logger logger = LogManager.getLogger(InteractiveBrokersBrokerConnector.class);
    private static InteractiveBrokersBrokerConnector instance;

    /**
     * Singleton class that connects to Interactive Brokers to configure , you need to open TWS or Gateway click settings and enable API connections
     * Settings/API/Settings/Enable ActiveX and Socket Clients and check the port is the same as the port in the configuration
     * https://interactivebrokers.github.io/tws-api/classIBApi_1_1EClient.html#a3ab310450f1261accd706f69766b2263
     *
     * @param ibHost
     * @param ibPort
     * @param ibClientId
     * @return
     */
    public static InteractiveBrokersBrokerConnector getInstance(String ibHost, int ibPort, int ibClientId) {

        if (instance == null) {
            instance = new InteractiveBrokersBrokerConnector(ibHost, ibPort, ibClientId);
        }
        return instance;
    }

    private InteractiveBrokersBrokerConnector(String ibHost, int ibPort, int ibClientId) {
        this.ibHost = ibHost;
        this.ibPort = ibPort;
        this.ibClientId = ibClientId;

        TickerIdInstrumentPk = new ConcurrentHashMap<>();
        InstrumentPkContractMap = new ConcurrentHashMap<>();
    }

    public void connect() {
        if (clientSocket != null && clientSocket.isConnected()) {
            return;
        }
        this.clientSocket = new EClientSocket(this, this.signalListener);
        logger.info("Connecting to Interactive Brokers in host: " + ibHost + " port: " + ibPort + " clientId: " + ibClientId);
        clientSocket.eConnect(ibHost, ibPort, ibClientId);
        if (!waitConnection(10000)) {
            logger.error("Failed to connect to Interactive Brokers in host: " + ibHost + " port: " + ibPort + " clientId: " + ibClientId);

        }

        //add subscription reader
        reader = new EReader(clientSocket, signalListener);
        reader.start();

        new Thread(() -> {
            processMessages();
        }).start();

    }

    public void init() {
        connect();
    }


    private void processMessages() {
        while (clientSocket.isConnected()) {
            signalListener.waitForSignal();
            try {
                reader.processMsgs();
            } catch (Exception e) {
                error(e);
            }
        }
    }

    public void register(EWrapper listener) {
        iblisteners.add(listener);
    }


    public void deinit() {
        unsubscribeAllDepth();
        disconnect();
    }

    public Contract getContract(String instrumentPk) throws Exception {
        Instrument instrument = Instrument.getInstrument(instrumentPk);
        if (instrument == null) {
            logger.error("Instrument not found in Interactive Brokers in host: " + ibHost + " port: " + ibPort + " clientId: " + ibClientId + " instrumentPk: " + instrumentPk);
            throw new Exception("Instrument not found in Interactive Brokers in host: " + ibHost + " port: " + ibPort + " clientId: " + ibClientId + " instrumentPk: " + instrumentPk);
        }
        if (!InstrumentPkContractMap.containsKey(instrumentPk)) {
            Contract contract = createContract(instrument);
            InstrumentPkContractMap.put(instrumentPk, contract);
        }
        return InstrumentPkContractMap.get(instrumentPk);
    }

    public void subscribeMarketData(String instrumentPk) throws Exception {
        Contract contract = getContract(instrumentPk);


        if (subscribeAllDepth) {
            List<TagValue> mktDepthOptions = new ArrayList<>();
            int tickerId1 = this.tickerId.incrementAndGet();

            TickerIdInstrumentPk.put(tickerId1, instrumentPk);
            SubscriptionsDepthTickerId.add(tickerId1);
            clientSocket.reqMktDepth(tickerId1, contract, 20, smartDepth, mktDepthOptions);
        }

        int tickerId2 = this.tickerId.incrementAndGet();
        TickerIdInstrumentPk.put(tickerId2, instrumentPk);
        SubscriptionsTickTickerId.add(tickerId2);
        clientSocket.reqTickByTickData(tickerId2, contract, "BidAsk", 1, true);
//
        int tickerId3 = this.tickerId.incrementAndGet();
        TickerIdInstrumentPk.put(tickerId3, instrumentPk);
        SubscriptionsTickTickerId.add(tickerId3);
        clientSocket.reqTickByTickData(tickerId3, contract, "Last", 1, true);

    }

    public void unsubscribeAllDepth() {
        for (Integer tickerId : SubscriptionsDepthTickerId) {
            clientSocket.cancelMktDepth(tickerId, smartDepth);
        }
        for (Integer tickerId : SubscriptionsTickTickerId) {
            clientSocket.cancelTickByTickData(tickerId);
        }
        for (Integer tickerId : SubscriptionsTickTickerId) {
            clientSocket.cancelMktData(tickerId);
        }
    }

    public void disconnect() {
        if (clientSocket != null && clientSocket.isConnected()) {
            clientSocket.eDisconnect();
        }
    }


    private boolean waitConnection(int timeoutMs) {
        long start = System.currentTimeMillis();
        while (!clientSocket.isConnected() && System.currentTimeMillis() - start < timeoutMs) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return clientSocket.isConnected();
    }

    public boolean isConnected() {
        return clientSocket.isConnected();
    }

    public String getInstrumentPk(int tickerId) {
        return TickerIdInstrumentPk.get(tickerId);
    }

    @Override
    public void tickSnapshotEnd(int reqId) {

    }

    @Override
    public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice, Decimal bidSize, Decimal askSize, TickAttribBidAsk tickAttribBidAsk) {
        for (EWrapper listener : iblisteners) {
            listener.tickByTickBidAsk(reqId, time, bidPrice, askPrice, bidSize, askSize, tickAttribBidAsk);
        }
    }

    @Override
    public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size, TickAttribLast tickAttribLast, String exchange, String specialConditions) {
        for (EWrapper listener : iblisteners) {
            listener.tickByTickAllLast(reqId, tickType, time, price, size, tickAttribLast, exchange, specialConditions);
        }
    }


    @Override
    public void error(Exception e) {
        super.error(e);
        for (EWrapper listener : iblisteners) {
            listener.error(e);
        }
    }

    @Override
    public void error(String str) {
        super.error(str);
        for (EWrapper listener : iblisteners) {
            listener.error(str);
        }
    }

    @Override
    public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
        super.error(id, errorCode, errorMsg, advancedOrderRejectJson);
        for (EWrapper listener : iblisteners) {
            listener.error(id, errorCode, errorMsg, advancedOrderRejectJson);
        }
    }

    @Override
    public void connectionClosed() {
        super.connectionClosed();

        for (EWrapper listener : iblisteners) {
            listener.connectionClosed();
        }
    }

    @Override
    public void connectAck() {
        super.connectAck();
        if (clientSocket.isAsyncEConnect())
            clientSocket.startAPI();
        for (EWrapper listener : iblisteners) {
            listener.connectAck();
        }
    }

    @Override
    public void completedOrdersEnd() {
        for (EWrapper listener : iblisteners) {
            listener.completedOrdersEnd();
        }
    }

    @Override
    public void completedOrder(Contract contract, Order order, OrderState orderState) {
        for (EWrapper listener : iblisteners) {
            listener.completedOrder(contract, order, orderState);
        }
    }

    @Override
    public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
        for (EWrapper listener : iblisteners) {
            listener.orderStatus(orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld, mktCapPrice);
        }
    }

    @Override
    public void position(String account, Contract contract, Decimal pos, double avgCost) {
        for (EWrapper listener : iblisteners) {
            listener.position(account, contract, pos, avgCost);
        }
    }

    @Override
    public void realtimeBar(int reqId, long time, double open, double high, double low, double close, Decimal volume, Decimal wap, int count) {
        for (EWrapper listener : iblisteners) {
            listener.realtimeBar(reqId, time, open, high, low, close, volume, wap, count);
        }
    }

    @Override
    public void historicalData(int reqId, Bar bar) {
        for (EWrapper listener : iblisteners) {
            listener.historicalData(reqId, bar);
        }
    }

    @Override
    public void updateMktDepth(int tickerId, int position, int operation, int side, double price, Decimal size) {
        for (EWrapper listener : iblisteners) {
            listener.updateMktDepth(tickerId, position, operation, side, price, size);
        }
    }

    @Override
    public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side, double price, Decimal size, boolean isSmartDepth) {
        for (EWrapper listener : iblisteners) {
            listener.updateMktDepthL2(tickerId, position, marketMaker, operation, side, price, size, isSmartDepth);
        }
    }


}
