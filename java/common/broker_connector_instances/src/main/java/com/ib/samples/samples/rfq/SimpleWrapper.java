/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.samples.rfq;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.SwingUtilities;

import com.ib.client.*;


public class SimpleWrapper implements EWrapper {
    private static final int MAX_MESSAGES = 1000000;
    private final static SimpleDateFormat m_df = new SimpleDateFormat("HH:mm:ss");

    // main client
    private EJavaSignal m_signal = new EJavaSignal();
    private EClientSocket m_client = new EClientSocket(this, m_signal);

    // utils
    private long ts;
    private PrintStream m_output;
    private int m_outputCounter = 0;
    private int m_messageCounter;

    protected EClientSocket client() {
        return m_client;
    }

    protected SimpleWrapper() {
        initNextOutput();
        attachDisconnectHook(this);
    }

    public void connect() {
        connect(1);
    }

    public void connect(int clientId) {
        String host = System.getProperty("jts.host");
        host = host != null ? host : "";
        m_client.eConnect(host, 7497, clientId);

        final EReader reader = new EReader(m_client, m_signal);

        reader.start();

        new Thread(() -> {
            while (m_client.isConnected()) {
                m_signal.waitForSignal();
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        try {
                            reader.processMsgs();
                        } catch (IOException e) {
                            error(e);
                        }
                    });
                } catch (Exception e) {
                    error(e);
                }
            }
        }).start();
    }

    public void disconnect() {
        m_client.eDisconnect();
    }

    /* ***************************************************************
     * AnyWrapper
     *****************************************************************/

    public void error(Exception e) {
        e.printStackTrace(m_output);
    }

    public void error(String str) {
        m_output.println(str);
    }

    public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
        String str = "Error id=" + id + " code=" + errorCode + " msg=" + errorMsg;
        if (advancedOrderRejectJson != null) {
            str += (" advancedOrderRejectJson=" + advancedOrderRejectJson);
        }
        logIn(str);
    }

    public void connectionClosed() {
        m_output.println("--------------------- CLOSED ---------------------");
    }

    /* ***************************************************************
     * EWrapper
     *****************************************************************/

    public void tickPrice(int tickerId, int field, double price, TickAttrib attribs) {
        logIn("tickPrice");
    }

    public void tickSize(int tickerId, int field, Decimal size) {
        logIn("tickSize");
    }

    public void tickGeneric(int tickerId, int tickType, double value) {
        logIn("tickGeneric");
    }

    public void tickString(int tickerId, int tickType, String value) {
        logIn("tickString");
    }

    public void tickSnapshotEnd(int tickerId) {
        logIn("tickSnapshotEnd");
    }

    public void tickOptionComputation(int tickerId, int field, int tickAttrib, double impliedVol,
                                      double delta, double optPrice, double pvDividend,
                                      double gamma, double vega, double theta, double undPrice) {
        logIn("tickOptionComputation");
    }

    public void tickEFP(int tickerId, int tickType, double basisPoints,
                        String formattedBasisPoints, double impliedFuture, int holdDays,
                        String futureLastTradeDate, double dividendImpact, double dividendsToLastTradeDate) {
        logIn("tickEFP");
    }

    public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining,
                            double avgFillPrice, int permId, int parentId, double lastFillPrice,
                            int clientId, String whyHeld, double mktCapPrice) {
        logIn("orderStatus");
    }

    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
        logIn("openOrder");
    }

    public void openOrderEnd() {
        logIn("openOrderEnd");
    }

    public void updateAccountValue(String key, String value, String currency, String accountName) {
        logIn("updateAccountValue");
    }

    public void updatePortfolio(Contract contract, Decimal position, double marketPrice, double marketValue,
                                double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
        logIn("updatePortfolio");
    }

    public void updateAccountTime(String timeStamp) {
        logIn("updateAccountTime");
    }

    public void accountDownloadEnd(String accountName) {
        logIn("accountDownloadEnd");
    }

    public void nextValidId(int orderId) {
        logIn("nextValidId");
    }

    public void contractDetails(int reqId, ContractDetails contractDetails) {
        logIn("contractDetails");
    }

    public void contractDetailsEnd(int reqId) {
        logIn("contractDetailsEnd");
    }

    public void bondContractDetails(int reqId, ContractDetails contractDetails) {
        logIn("bondContractDetails");
    }

    public void execDetails(int reqId, Contract contract, Execution execution) {
        logIn("execDetails");
    }

    public void execDetailsEnd(int reqId) {
        logIn("execDetailsEnd");
    }

    public void updateMktDepth(int tickerId, int position, int operation, int side, double price, Decimal size) {
        logIn("updateMktDepth");
    }

    public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation,
                                 int side, double price, Decimal size, boolean isSmartDepth) {
        logIn("updateMktDepthL2");
    }

    public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {
        logIn("updateNewsBulletin");
    }

    public void managedAccounts(String accountsList) {
        logIn("managedAccounts");
    }

    public void receiveFA(int faDataType, String xml) {
        logIn("receiveFA");
    }

    public void historicalData(int reqId, Bar bar) {
        logIn("historicalData");
    }

    public void scannerParameters(String xml) {
        logIn("scannerParameters");
    }

    public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance,
                            String benchmark, String projection, String legsStr) {
        logIn("scannerData");
    }

    public void scannerDataEnd(int reqId) {
        logIn("scannerDataEnd");
    }

    public void realtimeBar(int reqId, long time, double open, double high, double low, double close,
                            Decimal volume, Decimal wap, int count) {
        logIn("realtimeBar");
    }

    public void currentTime(long millis) {
        logIn("currentTime");
    }

    public void fundamentalData(int reqId, String data) {
        logIn("fundamentalData");
    }

    public void deltaNeutralValidation(int reqId, DeltaNeutralContract deltaNeutralContract) {
        logIn("deltaNeutralValidation");
    }

    public void marketDataType(int reqId, int marketDataType) {
        logIn("marketDataType");
    }

    public void commissionReport(CommissionReport commissionReport) {
        logIn("commissionReport");
    }


    public void position(String account, Contract contract, Decimal pos, double avgCost) {
        logIn("position");
    }

    public void positionEnd() {
        logIn("positionEnd");
    }

    public void accountSummary(int reqId, String account, String tag, String value, String currency) {
        logIn("accountSummary");
    }

    public void accountSummaryEnd(int reqId) {
        logIn("accountSummaryEnd");
    }

    public void verifyMessageAPI(String apiData) {
        logIn("verifyMessageAPI");
    }

    public void verifyCompleted(boolean isSuccessful, String errorText) {
        logIn("verifyCompleted");
    }

    public void verifyAndAuthMessageAPI(String apiData, String xyzChallenge) {
        logIn("verifyAndAuthMessageAPI");
    }

    public void verifyAndAuthCompleted(boolean isSuccessful, String errorText) {
        logIn("verifyAndAuthCompleted");
    }

    public void displayGroupList(int reqId, String groups) {
        logIn("displayGroupList");
    }

    public void displayGroupUpdated(int reqId, String contractInfo) {
        logIn("displayGroupUpdated");
    }

    public void positionMulti(int reqId, String account, String modelCode, Contract contract, Decimal pos, double avgCost) {
        logIn("positionMulti");
    }

    public void positionMultiEnd(int reqId) {
        logIn("positionMultiEnd");
    }

    public void accountUpdateMulti(int reqId, String account, String modelCode, String key, String value, String currency) {
        logIn("accountUpdateMulti");
    }

    public void accountUpdateMultiEnd(int reqId) {
        logIn("accountUpdateMultiEnd");
    }

    /* ***************************************************************
     * Helpers
     *****************************************************************/
    protected void logIn(String method) {
        m_messageCounter++;
        if (m_messageCounter == MAX_MESSAGES) {
            m_output.close();
            initNextOutput();
            m_messageCounter = 0;
        }
        m_output.println("[W] > " + method);
    }

    protected static void consoleMsg(String str) {
        System.out.println(Thread.currentThread().getName() + " (" + tsStr() + "): " + str);
    }

    protected static String tsStr() {
        synchronized (m_df) {
            return m_df.format(new Date());
        }
    }

    protected static void sleepSec(int sec) {
        sleep(sec * 1000);
    }

    private static void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void swStart() {
        ts = System.currentTimeMillis();
    }

    protected void swStop() {
        long dt = System.currentTimeMillis() - ts;
        m_output.println("[API]" + " Time=" + dt);
    }

    private void initNextOutput() {
        try {
            m_output = new PrintStream(new File("sysout_" + (++m_outputCounter) + ".log"), "UTF-8");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static void attachDisconnectHook(final SimpleWrapper ut) {
        Runtime.getRuntime().addShutdownHook(new Thread(ut::disconnect));
    }

    public void connectAck() {
        m_client.startAPI();
    }

    @Override
    public void securityDefinitionOptionalParameter(int reqId, String exchange, int underlyingConId, String tradingClass,
                                                    String multiplier, Set<String> expirations, Set<Double> strikes) {
        // TODO Auto-generated method stub

    }

    @Override
    public void securityDefinitionOptionalParameterEnd(int reqId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {
        // TODO Auto-generated method stub

    }

    @Override
    public void familyCodes(FamilyCode[] familyCodes) {
        // TODO Auto-generated method stub

    }

    @Override
    public void symbolSamples(int reqId, ContractDescription[] contractDescriptions) {
        // TODO Auto-generated method stub

    }

    @Override
    public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {
        // TODO Auto-generated method stub

    }

    @Override
    public void tickNews(int tickerId, long timeStamp, String providerCode, String articleId, String headline,
                         String extraData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void smartComponents(int reqId, Map<Integer, Entry<String, Character>> theMap) {
        // TODO Auto-generated method stub

    }

    @Override
    public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {
        // TODO Auto-generated method stub

    }

    @Override
    public void newsProviders(NewsProvider[] newsProviders) {
        // TODO Auto-generated method stub

    }

    @Override
    public void newsArticle(int requestId, int articleType, String articleText) {
        // TODO Auto-generated method stub

    }

    @Override
    public void historicalNews(int requestId, String time, String providerCode, String articleId, String headline) {
        // TODO Auto-generated method stub

    }

    @Override
    public void historicalNewsEnd(int requestId, boolean hasMore) {
        // TODO Auto-generated method stub

    }

    @Override
    public void headTimestamp(int reqId, String headTimestamp) {
        // TODO Auto-generated method stub

    }

    @Override
    public void histogramData(int reqId, List<HistogramEntry> items) {
        // TODO Auto-generated method stub

    }

    @Override
    public void historicalDataUpdate(int reqId, Bar bar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {
        // TODO Auto-generated method stub

    }

    @Override
    public void rerouteMktDataReq(int reqId, int conId, String exchange) {
        // TODO Auto-generated method stub

    }

    @Override
    public void rerouteMktDepthReq(int reqId, int conId, String exchange) {
        // TODO Auto-generated method stub

    }

    @Override
    public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {
        // TODO Auto-generated method stub

    }

    @Override
    public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void historicalTicks(int reqId, List<HistoricalTick> ticks, boolean last) {
        // TODO Auto-generated method stub

    }

    @Override
    public void historicalTicksBidAsk(int reqId, List<HistoricalTickBidAsk> ticks, boolean done) {
        // TODO Auto-generated method stub

    }


    @Override
    public void historicalTicksLast(int reqId, List<HistoricalTickLast> ticks, boolean done) {
        // TODO Auto-generated method stub

    }

    @Override
    public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size, TickAttribLast tickAttribLast,
                                  String exchange, String specialConditions) {
        // TODO Auto-generated method stub
    }

    @Override
    public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice, Decimal bidSize, Decimal askSize,
                                 TickAttribBidAsk tickAttribBidAsk) {
        // TODO Auto-generated method stub
    }

    @Override
    public void tickByTickMidPoint(int reqId, long time, double midPoint) {
        // TODO Auto-generated method stub
    }

    @Override
    public void orderBound(long orderId, int apiClientId, int apiOrderId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void completedOrder(Contract contract, Order order, OrderState orderState) {
        // TODO Auto-generated method stub
    }

    @Override
    public void completedOrdersEnd() {
        // TODO Auto-generated method stub
    }

    @Override
    public void replaceFAEnd(int reqId, String text) {
        // TODO Auto-generated method stub
    }

    @Override
    public void wshMetaData(int reqId, String dataJson) {
        // TODO Auto-generated method stub

    }

    @Override
    public void wshEventData(int reqId, String dataJson) {
        // TODO Auto-generated method stub

    }

    @Override
    public void historicalSchedule(int reqId, String startDateTime, String endDateTime, String timeZone, List<HistoricalSession> sessions) {
        // TODO Auto-generated method stub

    }

    @Override
    public void userInfo(int reqId, String whiteBrandingId) {
        // TODO Auto-generated method stub

    }
}
