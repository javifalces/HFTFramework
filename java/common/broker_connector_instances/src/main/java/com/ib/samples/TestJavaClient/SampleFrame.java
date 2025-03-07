/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.swing.*;

import com.ib.client.*;

class SampleFrame extends JFrame implements EWrapper {
    private static final int NOT_AN_FA_ACCOUNT_ERROR = 321;
    private int faErrorCodes[] = {503, 504, 505, 522, 1100, NOT_AN_FA_ACCOUNT_ERROR};
    private boolean faError;

    private EJavaSignal m_signal = new EJavaSignal();
    private EClientSocket m_client = new EClientSocket(this, m_signal);
    private EReader m_reader;
    private IBTextPanel m_tickers = new IBTextPanel("Market and Historical Data", false);
    private IBTextPanel m_TWS = new IBTextPanel("TWS Server Responses", false);
    private IBTextPanel m_errors = new IBTextPanel("Errors and Messages", false);
    private OrderDlg m_orderDlg = new OrderDlg(this);
    private ExtOrdDlg m_extOrdDlg = new ExtOrdDlg(m_orderDlg);
    private AccountDlg m_acctDlg = new AccountDlg(this);
    private Map<Integer, MktDepthDlg> m_mapRequestToMktDepthDlg = new HashMap<>();
    private Map<Integer, MktDepthDlg> m_mapRequestToSmartDepthDlg = new HashMap<>();
    private NewsBulletinDlg m_newsBulletinDlg = new NewsBulletinDlg(this);
    private ScannerDlg m_scannerDlg = new ScannerDlg(this);
    private GroupsDlg m_groupsDlg;
    private SecDefOptParamsReqDlg m_secDefOptParamsReq = new SecDefOptParamsReqDlg(this);
    private SmartComponentsParamsReqDlg m_smartComponentsParamsReq = new SmartComponentsParamsReqDlg(this);
    private HistoricalNewsDlg m_historicalNewsDlg = new HistoricalNewsDlg(this);
    private NewsArticleDlg m_newsArticleDlg = new NewsArticleDlg(this);
    private MarketRuleDlg m_marketRuleDlg = new MarketRuleDlg(this);
    private PnLDlg m_pnlDlg = new PnLDlg(this);
    private PnLSingleDlg m_pnlSingleDlg = new PnLSingleDlg(this);
    private WSHDlg m_wshMetaDlg = new WSHDlg(this, false);
    private WSHDlg m_wshEventDlg = new WSHDlg(this, true);

    private List<TagValue> m_mktDataOptions = new ArrayList<>();
    private List<TagValue> m_chartOptions = new ArrayList<>();
    private List<TagValue> m_mktDepthOptions = new ArrayList<>();
    private List<TagValue> m_realTimeBarsOptions = new ArrayList<>();
    private List<TagValue> m_historicalNewsOptions = new ArrayList<>();
    private List<TagValue> m_newsArticleOptions = new ArrayList<>();

    private String faGroupXML;
    private String faProfilesXML;
    private String faAliasesXML;
    String m_FAAcctCodes;
    boolean m_bIsFAAccount = false;

    private boolean m_disconnectInProgress = false;

    SampleFrame() {
        JPanel scrollingWindowDisplayPanel = new JPanel(new GridLayout(0, 1));
        scrollingWindowDisplayPanel.add(m_tickers);
        scrollingWindowDisplayPanel.add(m_TWS);
        scrollingWindowDisplayPanel.add(m_errors);

        JPanel buttonPanel = createButtonPanel();

        getContentPane().add(scrollingWindowDisplayPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.EAST);
        setSize(900, 800);
        setTitle("Sample");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        m_groupsDlg = new GroupsDlg(this, m_client);
    }

    interface ContractDetailsCallback {
        void onContractDetails(ContractDetails contractDetails);

        void onContractDetailsEnd();

        void onError(int errorCode, String errorMsg);
    }

    private final Map<Integer, ContractDetailsCallback> m_callbackMap = new HashMap<>();

    List<ContractDetails> lookupContract(Contract contract) throws InterruptedException {
        final CompletableFuture<List<ContractDetails>> future = new CompletableFuture<>();

        synchronized (m_callbackMap) {
            m_callbackMap.put(m_orderDlg.id(), new ContractDetailsCallback() {

                private final List<ContractDetails> list = new ArrayList<>();

                @Override
                public void onError(int errorCode, String errorMsg) {
                    future.complete(list);
                }

                @Override
                public void onContractDetailsEnd() {
                    future.complete(list);
                }

                @Override
                public void onContractDetails(ContractDetails contractDetails) {
                    list.add(contractDetails);
                }
            });
        }
        m_client.reqContractDetails(m_orderDlg.id(), contract);
        try {
            return future.get();
        } catch (final ExecutionException e) {
            return null;
        } finally {
            synchronized (m_callbackMap) {
                m_callbackMap.remove(m_orderDlg.id());
            }
        }
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1));
        JButton butConnect = new JButton("Connect");
        butConnect.addActionListener(e -> onConnect());
        JButton butDisconnect = new JButton("Disconnect");
        butDisconnect.addActionListener(e -> onDisconnect());
        JButton butMktData = new JButton("Req Mkt Data");
        butMktData.addActionListener(e -> onReqMktData());
        JButton butCancelMktData = new JButton("Cancel Mkt Data");
        butCancelMktData.addActionListener(e -> onCancelMktData());
        JButton butMktDepth = new JButton("Req Mkt Depth");
        butMktDepth.addActionListener(e -> onReqMktDepth());
        JButton butCancelMktDepth = new JButton("Cancel Mkt Depth");
        butCancelMktDepth.addActionListener(e -> onCancelMktDepth());
        JButton butHistoricalData = new JButton("Historical Data");
        butHistoricalData.addActionListener(e -> onHistoricalData());
        JButton butCancelHistoricalData = new JButton("Cancel Hist. Data");
        butCancelHistoricalData.addActionListener(e -> onCancelHistoricalData());
        JButton butFundamentalData = new JButton("Fundamental Data");
        butFundamentalData.addActionListener(e -> onFundamentalData());
        JButton butCancelFundamentalData = new JButton("Cancel Fund. Data");
        butCancelFundamentalData.addActionListener(e -> onCancelFundamentalData());
        JButton butRealTimeBars = new JButton("Req Real Time Bars");
        butRealTimeBars.addActionListener(e -> onReqRealTimeBars());
        JButton butCancelRealTimeBars = new JButton("Cancel Real Time Bars");
        butCancelRealTimeBars.addActionListener(e -> onCancelRealTimeBars());
        JButton butCurrentTime = new JButton("Req Current Time");
        butCurrentTime.addActionListener(e -> onReqCurrentTime());
        JButton butScanner = new JButton("Market Scanner");
        butScanner.addActionListener(e -> onScanner());
        JButton butOpenOrders = new JButton("Req Open Orders");
        butOpenOrders.addActionListener(e -> onReqOpenOrders());
        JButton butCalculateImpliedVolatility = new JButton("Calculate Implied Volatility");
        butCalculateImpliedVolatility.addActionListener(e -> onCalculateImpliedVolatility());
        JButton butCancelCalculateImpliedVolatility = new JButton("Cancel Calc Impl Volatility");
        butCancelCalculateImpliedVolatility.addActionListener(e -> onCancelCalculateImpliedVolatility());
        JButton butCalculateOptionPrice = new JButton("Calculate Option Price");
        butCalculateOptionPrice.addActionListener(e -> onCalculateOptionPrice());
        JButton butCancelCalculateOptionPrice = new JButton("Cancel Calc Opt Price");
        butCancelCalculateOptionPrice.addActionListener(e -> onCancelCalculateOptionPrice());
        JButton butWhatIfOrder = new JButton("What If");
        butWhatIfOrder.addActionListener(e -> onWhatIfOrder());
        JButton butPlaceOrder = new JButton("Place Order");
        butPlaceOrder.addActionListener(e -> onPlaceOrder());
        JButton butCancelOrder = new JButton("Cancel Order");
        butCancelOrder.addActionListener(e -> onCancelOrder());
        JButton butExerciseOptions = new JButton("Exercise Options");
        butExerciseOptions.addActionListener(e -> onExerciseOptions());
        JButton butExtendedOrder = new JButton("Extended");
        butExtendedOrder.addActionListener(e -> onExtendedOrder());
        JButton butAcctData = new JButton("Req Acct Data");
        butAcctData.addActionListener(e -> onReqAcctData());
        JButton butContractData = new JButton("Req Contract Data");
        butContractData.addActionListener(e -> onReqContractData());
        JButton butExecutions = new JButton("Req Executions");
        butExecutions.addActionListener(e -> onReqExecutions());
        JButton butNewsBulletins = new JButton("Req News Bulletins");
        butNewsBulletins.addActionListener(e -> onReqNewsBulletins());
        JButton butServerLogging = new JButton("Server Logging");
        butServerLogging.addActionListener(e -> onServerLogging());
        JButton butAllOpenOrders = new JButton("Req All Open Orders");
        butAllOpenOrders.addActionListener(e -> onReqAllOpenOrders());
        JButton butAutoOpenOrders = new JButton("Req Auto Open Orders");
        butAutoOpenOrders.addActionListener(e -> onReqAutoOpenOrders());
        JButton butManagedAccts = new JButton("Req Accounts");
        butManagedAccts.addActionListener(e -> onReqManagedAccts());
        JButton butFinancialAdvisor = new JButton("Financial Advisor");
        butFinancialAdvisor.addActionListener(e -> onFinancialAdvisor());
        JButton butGlobalCancel = new JButton("Global Cancel");
        butGlobalCancel.addActionListener(e -> onGlobalCancel());
        JButton butReqMarketDataType = new JButton("Req Market Data Type");
        butReqMarketDataType.addActionListener(e -> onReqMarketDataType());

        JButton butRequestPositions = new JButton("Request Positions");
        butRequestPositions.addActionListener(e -> onRequestPositions());
        JButton butCancelPositions = new JButton("Cancel Positions");
        butCancelPositions.addActionListener(e -> onCancelPositions());
        JButton butRequestAccountSummary = new JButton("Request Account Summary");
        butRequestAccountSummary.addActionListener(e -> onRequestAccountSummary());
        JButton butCancelAccountSummary = new JButton("Cancel Account Summary");
        butCancelAccountSummary.addActionListener(e -> onCancelAccountSummary());
        JButton butRequestPositionsMulti = new JButton("Request Positions Multi");
        butRequestPositionsMulti.addActionListener(e -> onRequestPositionsMulti());
        JButton butCancelPositionsMulti = new JButton("Cancel Positions Multi");
        butCancelPositionsMulti.addActionListener(e -> onCancelPositionsMulti());
        JButton butRequestAccountUpdatesMulti = new JButton("Request Account Updates Multi");
        butRequestAccountUpdatesMulti.addActionListener(e -> onRequestAccountUpdatesMulti());
        JButton butCancelAccountUpdatesMulti = new JButton("Cancel Account Updates Multi");
        butCancelAccountUpdatesMulti.addActionListener(e -> onCancelAccountUpdatesMulti());
        JButton butRequestSecurityDefinitionOptionParameters = new JButton("Request Security Definition Option Parameters");
        butRequestSecurityDefinitionOptionParameters.addActionListener(e -> onRequestSecurityDefinitionOptionParameters());
        JButton butGroups = new JButton("Groups");
        butGroups.addActionListener(e -> onGroups());
        JButton butRequestFamilyCodes = new JButton("Request Family Codes");
        butRequestFamilyCodes.addActionListener(e -> onRequestFamilyCodes());
        JButton butRequestMatchingSymbols = new JButton("Request Matching Symbols");
        butRequestMatchingSymbols.addActionListener(e -> onRequestMatchingSymbols());
        JButton butReqMktDepthExchanges = new JButton("Req Mkt Depth Exchanges");
        butReqMktDepthExchanges.addActionListener(e -> onReqMktDepthExchanges());
        JButton butReqSmartComponents = new JButton("Req Smart Components");
        butReqSmartComponents.addActionListener(e -> onReqSmartComponents());
        JButton butRequestNewsProviders = new JButton("Request News Providers");
        butRequestNewsProviders.addActionListener(e -> onRequestNewsProviders());
        JButton butReqNewsArticle = new JButton("Req News Article");
        butReqNewsArticle.addActionListener(e -> onReqNewsArticle());
        JButton butReqHistoricalNews = new JButton("Req Historical News");
        butReqHistoricalNews.addActionListener(e -> onReqHistoricalNews());
        JButton butHeadTimestamp = new JButton("Req Head Time Stamp");
        butHeadTimestamp.addActionListener(e -> onHeadTimestamp());
        JButton butHistogram = new JButton("Req Histogram");
        butHistogram.addActionListener(e -> onHistogram());
        JButton butHistogramCancel = new JButton("Cancel Histogram");
        butHistogramCancel.addActionListener(e -> onHistogramCancel());
        JButton butReqMarketRule = new JButton("Req Market Rule");
        butReqMarketRule.addActionListener(e -> onReqMarketRule());
        JButton butReqPnL = new JButton("Req PnL");
        butReqPnL.addActionListener(e -> onReqPnL());
        JButton butCancelPnL = new JButton("Cancel PnL");
        butCancelPnL.addActionListener(e -> onCancelPnL());
        JButton butReqPnLSingle = new JButton("Req PnL Single");
        butReqPnLSingle.addActionListener(e -> onReqPnLSingle());
        JButton butCancelPnLSingle = new JButton("Cancel PnL Single");
        butCancelPnLSingle.addActionListener(e -> onCancelPnLSingle());
        JButton butReqHistoricalTicks = new JButton("Req Historical Ticks");
        butReqHistoricalTicks.addActionListener(e -> onReqHistoricalTicks());
        JButton butReqTickByTickData = new JButton("Req Tick-By-Tick");
        butReqTickByTickData.addActionListener(e -> onReqTickByTickData());
        JButton butCancelTickByTickData = new JButton("Cancel Tick-By-Tick");
        butCancelTickByTickData.addActionListener(e -> onCancelTickByTickData());
        JButton butReqCompletedOrders = new JButton("Req Completed Orders");
        butReqCompletedOrders.addActionListener(e -> onReqCompletedOrders());
        JButton butReqAllCompletedOrders = new JButton("Req All Completed Orders");
        butReqAllCompletedOrders.addActionListener(e -> onReqAllCompletedOrders());
        JButton butReqWshMetaData = new JButton("Req WSH Meta Data");
        butReqWshMetaData.addActionListener(e -> onReqWshMetaData());
        JButton butCancelWshMetaData = new JButton("Cancel WSH Meta Data");
        butCancelWshMetaData.addActionListener(e -> onCancelWshMetaData());
        JButton butReqWshEventData = new JButton("Req WSH Event Data");
        butReqWshEventData.addActionListener(e -> onReqWshEventData());
        JButton butCancelWshEventData = new JButton("Cancel WSH Event Data");
        butCancelWshEventData.addActionListener(e -> onCancelWshEventData());
        JButton butReqUserInfo = new JButton("Req User Info");
        butReqUserInfo.addActionListener(e -> onReqUserInfo());

        JButton butClear = new JButton("Clear");
        butClear.addActionListener(e -> onClear());
        JButton butClose = new JButton("Close");
        butClose.addActionListener(e -> onClose());

        buttonPanel.add(new JPanel());

        BtnPairSlot pairSlot = new BtnPairSlot(buttonPanel);

        pairSlot.add(butConnect, butDisconnect);
        pairSlot.add(butMktData, butCancelMktData);
        pairSlot.add(butMktDepth, butCancelMktDepth);
        pairSlot.add(butHistoricalData, butCancelHistoricalData);
        pairSlot.add(butFundamentalData, butCancelFundamentalData);
        pairSlot.add(butRealTimeBars, butCancelRealTimeBars);
        pairSlot.add(butRealTimeBars, butCancelRealTimeBars);
        pairSlot.add(butScanner, butCurrentTime);
        pairSlot.add(butCalculateImpliedVolatility, butCancelCalculateImpliedVolatility);
        pairSlot.add(butCalculateOptionPrice, butCancelCalculateOptionPrice);

        buttonPanel.add(new JPanel());
        buttonPanel.add(butWhatIfOrder);
        pairSlot.add(butPlaceOrder, butCancelOrder);
        buttonPanel.add(butExerciseOptions);
        buttonPanel.add(butExtendedOrder);

        buttonPanel.add(new JPanel());
        buttonPanel.add(butContractData);
        buttonPanel.add(butOpenOrders);
        buttonPanel.add(butAllOpenOrders);
        buttonPanel.add(butAutoOpenOrders);
        buttonPanel.add(butAcctData);
        buttonPanel.add(butExecutions);
        buttonPanel.add(butNewsBulletins);
        buttonPanel.add(butServerLogging);
        buttonPanel.add(butManagedAccts);
        buttonPanel.add(butFinancialAdvisor);
        buttonPanel.add(butGlobalCancel);
        buttonPanel.add(butReqMarketDataType);

        pairSlot.add(butRequestPositions, butCancelPositions);
        pairSlot.add(butRequestAccountSummary, butCancelAccountSummary);
        pairSlot.add(butRequestPositionsMulti, butCancelPositionsMulti);
        pairSlot.add(butRequestAccountUpdatesMulti, butCancelAccountUpdatesMulti);

        buttonPanel.add(butRequestSecurityDefinitionOptionParameters);
        buttonPanel.add(butGroups);
        buttonPanel.add(butRequestFamilyCodes);
        buttonPanel.add(butRequestMatchingSymbols);
        buttonPanel.add(butReqMktDepthExchanges);
        buttonPanel.add(butReqSmartComponents);
        buttonPanel.add(butRequestNewsProviders);
        buttonPanel.add(butReqNewsArticle);
        buttonPanel.add(butReqHistoricalNews);
        buttonPanel.add(butHeadTimestamp);

        pairSlot.add(butHistogram, butHistogramCancel);
        buttonPanel.add(butReqMarketRule);
        pairSlot.add(butReqPnL, butCancelPnL);
        pairSlot.add(butReqPnLSingle, butCancelPnLSingle);
        pairSlot.add(butReqHistoricalTicks, butReqUserInfo);
        pairSlot.add(butReqTickByTickData, butCancelTickByTickData);
        pairSlot.add(butReqCompletedOrders, butReqAllCompletedOrders);
        pairSlot.add(butReqWshMetaData, butCancelWshMetaData);
        pairSlot.add(butReqWshEventData, butCancelWshEventData);

        buttonPanel.add(new JPanel());
        pairSlot.add(butClear, butClose);

        return buttonPanel;
    }

    private void onCancelWshEventData() {
        m_client.cancelWshMetaData(m_wshEventDlg.m_reqId);
    }

    private void onReqWshEventData() {
        m_wshEventDlg.setVisible(true);

        if (!m_wshEventDlg.isOk()) {
            return;
        }

        m_client.reqWshEventData(m_wshEventDlg.m_reqId, m_wshEventDlg.m_wshEventData);
    }

    private void onCancelWshMetaData() {
        m_client.cancelWshMetaData(m_wshMetaDlg.m_reqId);
    }

    private void onReqWshMetaData() {
        m_wshMetaDlg.setVisible(true);

        if (!m_wshMetaDlg.isOk()) {
            return;
        }

        m_client.reqWshMetaData(m_wshMetaDlg.m_reqId);
    }

    private void onReqUserInfo() {
        m_client.reqUserInfo(0);
    }

    class BtnPairSlot {

        private JPanel m_parentPanel;

        public BtnPairSlot(JPanel parentPanel) {
            m_parentPanel = parentPanel;
        }

        public void add(JButton left, JButton right) {
            JPanel subPanel = new JPanel(new GridLayout(0, 2));

            subPanel.add(left);
            subPanel.add(right);
            m_parentPanel.add(subPanel);
        }

    }

    private void onReqCompletedOrders() {
        m_client.reqCompletedOrders(true);
    }

    private void onReqAllCompletedOrders() {
        m_client.reqCompletedOrders(false);
    }

    private void onReqTickByTickData() {
        m_orderDlg.init("Request Tick-By-Tick Data", true);
        m_orderDlg.setVisible(true);

        if (m_orderDlg.m_rc) {
            m_client.reqTickByTickData(m_orderDlg.id(), m_orderDlg.contract(), m_orderDlg.tickByTickType(), m_orderDlg.numberOfTicks(),
                    m_orderDlg.ignoreSize());
        }
    }

    private void onCancelTickByTickData() {
        m_orderDlg.init("Cancel Tick-By-Tick Data", true);
        m_orderDlg.setVisible(true);

        if (m_orderDlg.m_rc) {
            m_client.cancelTickByTickData(m_orderDlg.id());
        }
    }

    private void onReqHistoricalTicks() {
        m_orderDlg.init("Misc Options", true);
        m_orderDlg.setVisible(true);

        if (m_orderDlg.m_rc) {
            m_client.reqHistoricalTicks(m_orderDlg.id(), m_orderDlg.contract(), m_orderDlg.startDateTime(),
                    m_orderDlg.backfillEndTime(), m_orderDlg.numberOfTicks(), m_orderDlg.whatToShow(),
                    m_orderDlg.useRTH(), m_orderDlg.ignoreSize(),
                    m_orderDlg.options());
        }
    }

    private void onCancelPnLSingle() {
        m_client.cancelPnLSingle(m_pnlSingleDlg.m_reqId);
    }

    private void onCancelPnL() {
        m_client.cancelPnL(m_pnlDlg.m_reqId);
    }

    private void onReqPnLSingle() {
        m_pnlSingleDlg.setVisible(true);

        if (!m_pnlSingleDlg.isOk()) {
            return;
        }

        m_client.reqPnLSingle(m_pnlSingleDlg.m_reqId, m_pnlSingleDlg.m_account, m_pnlSingleDlg.m_modelCode,
                m_pnlSingleDlg.m_conId);
    }

    private void onReqPnL() {
        m_pnlDlg.setVisible(true);

        if (!m_pnlDlg.isOk()) {
            return;
        }

        m_client.reqPnL(m_pnlDlg.m_reqId, m_pnlDlg.m_account, m_pnlDlg.m_modelCode);
    }

    private void onReqMarketRule() {
        // run m_marketRulerDlg
        m_marketRuleDlg.setVisible(true);
        if (!m_marketRuleDlg.rc()) {
            return;
        }

        m_client.reqMarketRule(m_marketRuleDlg.m_retMarketRuleId);
    }

    private void onHistogramCancel() {
        m_client.cancelHistogramData(m_orderDlg.id());
    }

    private void onHistogram() {
        // run m_orderDlg
        m_orderDlg.init("Chart Options", true, "Chart Options", m_chartOptions);

        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        m_client.reqHistogramData(m_orderDlg.id(), m_orderDlg.contract(), m_orderDlg.useRTH() == 1, m_orderDlg.backfillDuration());
    }

    private void onReqSmartComponents() {
        m_smartComponentsParamsReq.setModal(true);
        m_smartComponentsParamsReq.setVisible(true);

        int id = m_smartComponentsParamsReq.id();
        String bboExchange = m_smartComponentsParamsReq.BBOExchange();

        if (m_smartComponentsParamsReq.isOK()) {
            m_client.reqSmartComponents(id, bboExchange);
        }
    }

    private void onReqMktDepthExchanges() {
        m_client.reqMktDepthExchanges();
    }

    private void onRequestSecurityDefinitionOptionParameters() {
        m_secDefOptParamsReq.setModal(true);
        m_secDefOptParamsReq.setVisible(true);

        String underlyingSymbol = m_secDefOptParamsReq.underlyingSymbol();
        String futFopExchange = m_secDefOptParamsReq.futFopExchange();
        String underlyingSecType = m_secDefOptParamsReq.underlyingSecType();
        int underlyingConId = m_secDefOptParamsReq.underlyingConId();

        if (m_secDefOptParamsReq.isOK()) {
            m_client.reqSecDefOptParams(m_secDefOptParamsReq.id(), underlyingSymbol, futFopExchange,/* currency,*/ underlyingSecType, underlyingConId);
        }
    }

    private void onConnect() {
        if (m_client.isConnected())
            return;
        m_bIsFAAccount = false;
        // get connection parameters
        ConnectDlg dlg = new ConnectDlg(this);
        dlg.setVisible(true);
        if (!dlg.m_rc) {
            return;
        }

        // connect to TWS
        m_disconnectInProgress = false;

        m_client.optionalCapabilities(dlg.m_retOptCapts);
        m_client.eConnect(dlg.m_retIpAddress, dlg.m_retPort, dlg.m_retClientId);
        if (m_client.isConnected()) {
            m_TWS.add("Connected to Tws server version " +
                    m_client.serverVersion() + " at " +
                    m_client.getTwsConnectionTime());
        }

        m_reader = new EReader(m_client, m_signal);

        m_reader.start();

        new Thread(() -> {
            processMessages();

            int i = 0;
            System.out.println(i);
        }).start();
    }

    private void processMessages() {

        while (m_client.isConnected()) {
            m_signal.waitForSignal();
            try {
                m_reader.processMsgs();
            } catch (Exception e) {
                error(e);
            }
        }
    }

    private void onDisconnect() {
        // disconnect from TWS
        m_disconnectInProgress = true;
        m_client.eDisconnect();
    }

    private void onReqMktData() {

        // run m_orderDlg
        m_orderDlg.init("Mkt Data Options", true, "Market Data Options", m_mktDataOptions);

        m_orderDlg.setVisible(true);

        if (!m_orderDlg.m_rc) {
            return;
        }

        m_mktDataOptions = m_orderDlg.options();

        // req mkt data
        m_client.reqMktData(m_orderDlg.id(), m_orderDlg.contract(),
                m_orderDlg.m_genericTicks, m_orderDlg.m_snapshotMktData, m_orderDlg.m_reqSnapshotMktData, m_mktDataOptions);
    }

    private void onReqRealTimeBars() {
        // run m_orderDlg
        m_orderDlg.init("RTB Options", true, "Real Time Bars Options", m_realTimeBarsOptions);

        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }
        m_realTimeBarsOptions = m_orderDlg.options();

        // req real time bars
        m_client.reqRealTimeBars(m_orderDlg.id(), m_orderDlg.contract(),
                5 /* TODO: parse and use m_orderDlg.m_barSizeSetting */,
                m_orderDlg.whatToShow(), m_orderDlg.useRTH() > 0, m_realTimeBarsOptions);
    }

    private void onCancelRealTimeBars() {
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }
        // cancel market data
        m_client.cancelRealTimeBars(m_orderDlg.id());
    }

    private void onScanner() {
        m_scannerDlg.setVisible(true);

        if (m_scannerDlg.m_userSelection == ScannerDlg.CANCEL_SELECTION) {
            m_client.cancelScannerSubscription(m_scannerDlg.m_id);
        } else if (m_scannerDlg.m_userSelection == ScannerDlg.SUBSCRIBE_SELECTION) {
            m_client.reqScannerSubscription(m_scannerDlg.m_id,
                    m_scannerDlg.m_subscription, m_scannerDlg.scannerSubscriptionOptions(), m_scannerDlg.scannerFilterOptions());
        } else if (m_scannerDlg.m_userSelection == ScannerDlg.REQUEST_PARAMETERS_SELECTION) {
            m_client.reqScannerParameters();
        }
    }

    private void onReqCurrentTime() {
        m_client.reqCurrentTime();
    }

    private void onHeadTimestamp() {

        // run m_orderDlg
        m_orderDlg.init("Chart Options", true, "Chart Options", m_chartOptions);

        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        // req head timestamp
        m_client.reqHeadTimestamp(m_orderDlg.id(), m_orderDlg.contract(), m_orderDlg.whatToShow(),
                m_orderDlg.useRTH(), m_orderDlg.formatDate());
    }

    private void onHistoricalData() {

        // run m_orderDlg
        m_orderDlg.init("Chart Options", true, "Chart Options", m_chartOptions);

        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        m_chartOptions = m_orderDlg.options();

        // req historical data
        m_client.reqHistoricalData(m_orderDlg.id(), m_orderDlg.contract(),
                m_orderDlg.backfillEndTime(), m_orderDlg.backfillDuration(),
                m_orderDlg.barSizeSetting(), m_orderDlg.whatToShow(),
                m_orderDlg.useRTH(), m_orderDlg.formatDate(), m_orderDlg.keepUpToDate(), m_chartOptions);
    }

    private void onCancelHistoricalData() {
        // run m_orderDlg
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        // cancel historical data
        m_client.cancelHistoricalData(m_orderDlg.id());
    }

    private void onFundamentalData() {
        // run m_orderDlg
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }
        m_client.reqFundamentalData(m_orderDlg.id(), m_orderDlg.contract(),
                /* reportType */ m_orderDlg.whatToShow(), null);
    }

    private void onCancelFundamentalData() {
        // run m_orderDlg
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        m_client.cancelFundamentalData(m_orderDlg.id());
    }

    private void onReqContractData() {
        // run m_orderDlg
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        // req mkt data
        m_client.reqContractDetails(m_orderDlg.id(), m_orderDlg.contract());
    }

    private void onReqMktDepth() {
        // run m_orderDlg
        m_orderDlg.init("Mkt Depth Options", true, "Market Depth Options", m_mktDepthOptions);

        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }
        m_mktDepthOptions = m_orderDlg.options();
        boolean isSmartDepth = m_orderDlg.isSmartDepth();

        final Integer dialogId = m_orderDlg.id();
        MktDepthDlg depthDialog = null;

        if (isSmartDepth) {
            depthDialog = m_mapRequestToSmartDepthDlg.get(dialogId);
        } else {
            depthDialog = m_mapRequestToMktDepthDlg.get(dialogId);
        }
        if (depthDialog == null) {
            depthDialog = new MktDepthDlg((isSmartDepth ? "SMART" : "Market") + " Depth ID [" + dialogId + "]", this, isSmartDepth);
            if (isSmartDepth) {
                m_mapRequestToSmartDepthDlg.put(dialogId, depthDialog);
            } else {
                m_mapRequestToMktDepthDlg.put(dialogId, depthDialog);
            }

            // cleanup the map after depth dialog is closed so it does not linger or leak memory
            depthDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    if (isSmartDepth) {
                        m_mapRequestToSmartDepthDlg.remove(dialogId);
                    } else {
                        m_mapRequestToMktDepthDlg.remove(dialogId);
                    }
                }
            });
        }

        depthDialog.setParams(m_client, dialogId);

        // req mkt data
        m_client.reqMktDepth(dialogId, m_orderDlg.contract(), m_orderDlg.m_marketDepthRows, isSmartDepth, m_mktDepthOptions);
        depthDialog.setVisible(true);
    }

    private void onCancelMktData() {
        // run m_orderDlg
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        // cancel market data
        m_client.cancelMktData(m_orderDlg.id());
    }

    private void onCancelMktDepth() {
        // run m_orderDlg
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        // cancel market data
        m_client.cancelMktDepth(m_orderDlg.id(), m_orderDlg.isSmartDepth());
    }

    private void onReqOpenOrders() {
        m_client.reqOpenOrders();
    }

    private void onWhatIfOrder() {
        placeOrder(true);
    }

    private void onPlaceOrder() {
        placeOrder(false);
    }

    private void placeOrder(boolean whatIf) {
        // run m_orderDlg
        m_orderDlg.init("Order Misc Options", true, "Order Misc Options", m_orderDlg.m_order.orderMiscOptions());

        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        Order order = m_orderDlg.m_order;
        order.orderMiscOptions(m_orderDlg.options());

        // save old and set new value of whatIf attribute
        boolean savedWhatIf = order.whatIf();
        order.whatIf(whatIf);

        // place order
        m_client.placeOrder(m_orderDlg.id(), m_orderDlg.contract(), order);

        // restore whatIf attribute
        order.whatIf(savedWhatIf);
    }

    private void onExerciseOptions() {
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        // cancel order
        m_client.exerciseOptions(m_orderDlg.id(), m_orderDlg.contract(),
                m_orderDlg.m_exerciseAction, m_orderDlg.m_exerciseQuantity,
                m_orderDlg.m_order.account(), m_orderDlg.m_override);
    }

    private void onCancelOrder() {
        // run m_orderDlg
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        // cancel order
        m_client.cancelOrder(m_orderDlg.id(), m_extOrdDlg.manualOrderCancelTime());
    }

    private void onExtendedOrder() {
        //Show the extended order attributes dialog
        m_extOrdDlg.setVisible(true);
        if (!m_extOrdDlg.m_rc) {
            return;
        }

        // Copy over the extended order details
        copyExtendedOrderDetails(m_orderDlg.m_order, m_extOrdDlg.m_order);
    }

    private void onReqAcctData() {
        AcctUpdatesDlg dlg = new AcctUpdatesDlg(this);

        dlg.setVisible(true);

        if (dlg.m_subscribe) {
            m_acctDlg.accountDownloadBegin(dlg.m_acctCode);
        }

        m_client.reqAccountUpdates(dlg.m_subscribe, dlg.m_acctCode);

        if (m_client.isConnected() && dlg.m_subscribe) {
            m_acctDlg.reset();
            m_acctDlg.setVisible(true);
        }
    }

    private void onFinancialAdvisor() {
        faGroupXML = faProfilesXML = faAliasesXML = null;
        faError = false;
        m_client.requestFA(EClientSocket.GROUPS);
        m_client.requestFA(EClientSocket.PROFILES);
        m_client.requestFA(EClientSocket.ALIASES);
    }

    private void onServerLogging() {
        // get server logging level
        LogConfigDlg dlg = new LogConfigDlg(this);
        dlg.setVisible(true);
        if (!dlg.m_rc) {
            return;
        }

        // connect to TWS
        m_client.setServerLogLevel(dlg.m_serverLogLevel);
    }

    private void onReqAllOpenOrders() {
        // request list of all open orders
        m_client.reqAllOpenOrders();
    }

    private void onReqAutoOpenOrders() {
        // request to automatically bind any newly entered TWS orders
        // to this API client. NOTE: TWS orders can only be bound to
        // client's with clientId=0.
        m_client.reqAutoOpenOrders(true);
    }

    private void onReqManagedAccts() {
        // request the list of managed accounts
        m_client.reqManagedAccts();
    }

    private void onClear() {
        m_tickers.clear();
        m_TWS.clear();
        m_errors.clear();
    }

    private void onClose() {
        System.exit(1);
    }

    private void onReqExecutions() {
        ExecFilterDlg dlg = new ExecFilterDlg(this);

        dlg.setVisible(true);
        if (dlg.m_rc) {
            // request execution reports based on the supplied filter criteria
            m_client.reqExecutions(dlg.m_reqId, dlg.m_execFilter);
        }
    }

    private void onReqNewsBulletins() {
        // run m_newsBulletinDlg
        m_newsBulletinDlg.setVisible(true);
        if (!m_newsBulletinDlg.m_rc) {
            return;
        }

        if (m_newsBulletinDlg.m_subscribe) {
            m_client.reqNewsBulletins(m_newsBulletinDlg.m_allMsgs);
        } else {
            m_client.cancelNewsBulletins();
        }
    }

    private void onCalculateImpliedVolatility() {
        // run m_orderDlg
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }
        m_client.calculateImpliedVolatility(m_orderDlg.id(), m_orderDlg.contract(),
                m_orderDlg.m_order.lmtPrice(), m_orderDlg.m_order.auxPrice(), null);
    }

    private void onCancelCalculateImpliedVolatility() {
        // run m_orderDlg
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        m_client.cancelCalculateImpliedVolatility(m_orderDlg.id());
    }

    private void onCalculateOptionPrice() {
        // run m_orderDlg
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }
        m_client.calculateOptionPrice(m_orderDlg.id(), m_orderDlg.contract(),
                m_orderDlg.m_order.lmtPrice(), m_orderDlg.m_order.auxPrice(), null);
    }

    private void onCancelCalculateOptionPrice() {
        // run m_orderDlg
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        m_client.cancelCalculateOptionPrice(m_orderDlg.id());
    }

    private void onGlobalCancel() {
        m_client.reqGlobalCancel();
    }

    private void onReqMarketDataType() {
        // run m_orderDlg
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        // req mkt data type
        m_client.reqMarketDataType(m_orderDlg.m_marketDataType);

        if (m_client.isConnected()) {
            switch (m_orderDlg.m_marketDataType) {
                case MarketDataType.REALTIME:
                    m_TWS.add("Frozen, Delayed and Delayed-Frozen market data types are disabled");
                    break;
                case MarketDataType.FROZEN:
                    m_TWS.add("Frozen market data type is enabled");
                    break;
                case MarketDataType.DELAYED:
                    m_TWS.add("Delayed market data type is enabled, Delayed-Frozen market data type is disabled");
                    break;
                case MarketDataType.DELAYED_FROZEN:
                    m_TWS.add("Delayed and Delayed-Frozen market data types are enabled");
                    break;
                default:
                    m_errors.add("Unknown market data type");
                    break;
            }
        }
    }

    private void onRequestPositions() {
        m_client.reqPositions();
    }

    private void onCancelPositions() {
        m_client.cancelPositions();
    }

    private void onRequestAccountSummary() {
        AccountSummary dlg = new AccountSummary(this);

        dlg.setVisible(true);
        if (dlg.m_rc) {
            // request account summary
            m_client.reqAccountSummary(dlg.m_reqId, dlg.m_groupName, dlg.m_tags);
        }
    }

    private void onCancelAccountSummary() {
        AccountSummary dlg = new AccountSummary(this);

        dlg.setVisible(true);
        if (dlg.m_rc) {
            // cancel account summary
            m_client.cancelAccountSummary(dlg.m_reqId);
        }
    }

    private void onRequestPositionsMulti() {
        PositionsDlg dlg = new PositionsDlg(this);

        dlg.setVisible(true);
        if (dlg.m_rc) {
            // request positions multi
            m_client.reqPositionsMulti(dlg.m_retId, dlg.m_retAccount, dlg.m_retModelCode);
        }
    }

    private void onCancelPositionsMulti() {
        PositionsDlg dlg = new PositionsDlg(this);

        dlg.setVisible(true);
        if (dlg.m_rc) {
            // cancel positions multi
            m_client.cancelPositionsMulti(dlg.m_retId);
        }
    }

    private void onRequestAccountUpdatesMulti() {
        PositionsDlg dlg = new PositionsDlg(this);

        dlg.setVisible(true);
        if (dlg.m_rc) {
            // request account updates multi
            m_client.reqAccountUpdatesMulti(dlg.m_retId, dlg.m_retAccount, dlg.m_retModelCode, dlg.m_retLedgerAndNLV);
        }
    }

    private void onCancelAccountUpdatesMulti() {
        PositionsDlg dlg = new PositionsDlg(this);

        dlg.setVisible(true);
        if (dlg.m_rc) {
            // cancel account updates multi
            m_client.cancelAccountUpdatesMulti(dlg.m_retId);
        }
    }

    private void onGroups() {

        m_groupsDlg.setVisible(true);
    }

    private void onRequestFamilyCodes() {
        // request family codes
        m_client.reqFamilyCodes();
    }

    private void onRequestMatchingSymbols() {
        // run m_orderDlg
        m_orderDlg.init("Options", false);
        m_orderDlg.setVisible(true);
        if (!m_orderDlg.m_rc) {
            return;
        }

        // request matching symbols
        m_client.reqMatchingSymbols(m_orderDlg.id(), m_orderDlg.contract().symbol());
    }

    private void onRequestNewsProviders() {
        // request news providers
        m_client.reqNewsProviders();
    }

    private void onReqNewsArticle() {
        // run m_newsArticleDlg
        m_newsArticleDlg.init(m_newsArticleOptions);
        m_newsArticleDlg.setVisible(true);

        if (!m_newsArticleDlg.m_rc) {
            return;
        }

        m_newsArticleOptions = m_newsArticleDlg.getOptions();

        // request news article
        m_client.reqNewsArticle(m_newsArticleDlg.m_retRequestId, m_newsArticleDlg.m_retProviderCode,
                m_newsArticleDlg.m_retArticleId, m_newsArticleOptions);
    }


    private void onReqHistoricalNews() {
        // run m_historicalNewsDlg
        m_historicalNewsDlg.init(m_historicalNewsOptions);
        m_historicalNewsDlg.setVisible(true);

        if (!m_historicalNewsDlg.m_rc) {
            return;
        }

        m_historicalNewsOptions = m_historicalNewsDlg.getOptions();

        // reqHistoricalNews
        m_client.reqHistoricalNews(m_historicalNewsDlg.m_retRequestId, m_historicalNewsDlg.m_retConId,
                m_historicalNewsDlg.m_retProviderCodes, m_historicalNewsDlg.m_retStartDateTime,
                m_historicalNewsDlg.m_retEndDateTime, m_historicalNewsDlg.m_retTotalResults, m_historicalNewsOptions);
    }

    public void tickPrice(int tickerId, int field, double price, TickAttrib attribs) {
        // received price tick
        String msg = EWrapperMsgGenerator.tickPrice(tickerId, field, price, attribs);
        m_tickers.add(msg);
    }

    public void tickOptionComputation(int tickerId, int field, int tickAttrib, double impliedVol, double delta, double optPrice, double pvDividend,
                                      double gamma, double vega, double theta, double undPrice) {
        // received computation tick
        String msg = EWrapperMsgGenerator.tickOptionComputation(tickerId, field, tickAttrib, impliedVol, delta, optPrice, pvDividend,
                gamma, vega, theta, undPrice);
        m_tickers.add(msg);
    }

    public void tickSize(int tickerId, int field, Decimal size) {
        // received size tick
        String msg = EWrapperMsgGenerator.tickSize(tickerId, field, size);
        m_tickers.add(msg);
    }

    public void tickGeneric(int tickerId, int tickType, double value) {
        // received generic tick
        String msg = EWrapperMsgGenerator.tickGeneric(tickerId, tickType, value);
        m_tickers.add(msg);
    }

    public void tickString(int tickerId, int tickType, String value) {
        // received String tick
        String msg = EWrapperMsgGenerator.tickString(tickerId, tickType, value);
        m_tickers.add(msg);
    }

    public void tickSnapshotEnd(int tickerId) {
        String msg = EWrapperMsgGenerator.tickSnapshotEnd(tickerId);
        m_tickers.add(msg);
    }

    public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints,
                        double impliedFuture, int holdDays, String futureLastTradeDate, double dividendImpact,
                        double dividendsToLastTradeDate) {
        // received EFP tick
        String msg = EWrapperMsgGenerator.tickEFP(tickerId, tickType, basisPoints, formattedBasisPoints,
                impliedFuture, holdDays, futureLastTradeDate, dividendImpact, dividendsToLastTradeDate);
        m_tickers.add(msg);
    }

    public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining,
                            double avgFillPrice, int permId, int parentId,
                            double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
        // received order status
        String msg = EWrapperMsgGenerator.orderStatus(orderId, status, filled, remaining,
                avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld, mktCapPrice);
        m_TWS.add(msg);

        // make sure id for next order is at least orderId+1
        m_orderDlg.setIdAtLeast(orderId + 1);
    }

    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
        // received open order
        String msg = EWrapperMsgGenerator.openOrder(orderId, contract, order, orderState);
        m_TWS.add(msg);
    }

    public void openOrderEnd() {
        // received open order end
        String msg = EWrapperMsgGenerator.openOrderEnd();
        m_TWS.add(msg);
    }

    public void contractDetails(int reqId, ContractDetails contractDetails) {
        ContractDetailsCallback callback;
        synchronized (m_callbackMap) {
            callback = m_callbackMap.get(reqId);
        }
        if (callback != null) {
            callback.onContractDetails(contractDetails);
        }

        String msg = EWrapperMsgGenerator.contractDetails(reqId, contractDetails);
        m_TWS.add(msg);
    }

    public void contractDetailsEnd(int reqId) {
        ContractDetailsCallback callback;
        synchronized (m_callbackMap) {
            callback = m_callbackMap.get(reqId);
        }
        if (callback != null) {
            callback.onContractDetailsEnd();
        }

        String msg = EWrapperMsgGenerator.contractDetailsEnd(reqId);
        m_TWS.add(msg);
    }

    public void scannerData(int reqId, int rank, ContractDetails contractDetails,
                            String distance, String benchmark, String projection, String legsStr) {
        String msg = EWrapperMsgGenerator.scannerData(reqId, rank, contractDetails, distance,
                benchmark, projection, legsStr);
        m_tickers.add(msg);
    }

    public void scannerDataEnd(int reqId) {
        String msg = EWrapperMsgGenerator.scannerDataEnd(reqId);
        m_tickers.add(msg);
    }

    public void bondContractDetails(int reqId, ContractDetails contractDetails) {
        String msg = EWrapperMsgGenerator.bondContractDetails(reqId, contractDetails);
        m_TWS.add(msg);
    }

    public void execDetails(int reqId, Contract contract, Execution execution) {
        String msg = EWrapperMsgGenerator.execDetails(reqId, contract, execution);
        m_TWS.add(msg);
    }

    public void execDetailsEnd(int reqId) {
        String msg = EWrapperMsgGenerator.execDetailsEnd(reqId);
        m_TWS.add(msg);
    }

    public void updateMktDepth(int tickerId, int position, int operation,
                               int side, double price, Decimal size) {

        MktDepthDlg depthDialog = m_mapRequestToMktDepthDlg.get(tickerId);
        if (depthDialog != null) {
            depthDialog.updateMktDepth(tickerId, position, "", operation, side, price, size);
        } else {
            System.err.println("cannot find dialog that corresponds to request id [" + tickerId + "]");
        }


    }

    public void updateMktDepthL2(int tickerId, int position, String marketMaker,
                                 int operation, int side, double price, Decimal size, boolean isSmartDepth) {
        MktDepthDlg depthDialog = null;

        if (isSmartDepth) {
            depthDialog = m_mapRequestToSmartDepthDlg.get(tickerId);
        } else {
            depthDialog = m_mapRequestToMktDepthDlg.get(tickerId);
        }
        if (depthDialog != null) {
            depthDialog.updateMktDepth(tickerId, position, marketMaker, operation, side, price, size);
        } else {
            System.err.println("cannot find dialog that corresponds to request id [" + tickerId + "]");
        }
    }

    public void nextValidId(int orderId) {
        // received next valid order id
        String msg = EWrapperMsgGenerator.nextValidId(orderId);
        m_TWS.add(msg);
        m_orderDlg.setIdAtLeast(orderId);
    }

    public void error(Exception ex) {
        // do not report exceptions if we initiated disconnect
        if (!m_disconnectInProgress) {
            String msg = EWrapperMsgGenerator.error(ex);
            Main.inform(this, msg);
        }
    }

    public void error(String str) {
        String msg = EWrapperMsgGenerator.error(str);
        m_errors.add(msg);
    }

    public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
        // received error
        final ContractDetailsCallback callback;
        synchronized (m_callbackMap) {
            callback = m_callbackMap.get(id);
        }
        if (callback != null) {
            callback.onError(errorCode, errorMsg);
        } else if (id == -1) {
            final Collection<ContractDetailsCallback> callbacks;
            synchronized (m_callbackMap) {
                callbacks = new ArrayList<>(m_callbackMap.size());
                callbacks.addAll(m_callbackMap.values());
            }
            for (final ContractDetailsCallback cb : callbacks) {
                cb.onError(errorCode, errorMsg);
            }
        }

        String msg = EWrapperMsgGenerator.error(id, errorCode, errorMsg, advancedOrderRejectJson);
        m_errors.add(msg);
        for (int faErrorCode : faErrorCodes) {
            faError |= (errorCode == faErrorCode);
        }
        if (errorCode == MktDepthDlg.MKT_DEPTH_DATA_RESET) {

            MktDepthDlg depthDialog = m_mapRequestToMktDepthDlg.get(id);
            if (depthDialog != null) {
                depthDialog.reset();
            } else {
                System.err.println("cannot find dialog that corresponds to request id [" + id + "]");
            }
        }
    }

    public void connectionClosed() {
        String msg = EWrapperMsgGenerator.connectionClosed();
        Main.inform(this, msg);
    }

    public void updateAccountValue(String key, String value,
                                   String currency, String accountName) {
        m_acctDlg.updateAccountValue(key, value, currency, accountName);
    }

    public void updatePortfolio(Contract contract, Decimal position, double marketPrice,
                                double marketValue, double averageCost, double unrealizedPNL, double realizedPNL,
                                String accountName) {
        m_acctDlg.updatePortfolio(contract, position, marketPrice, marketValue,
                averageCost, unrealizedPNL, realizedPNL, accountName);
    }

    public void updateAccountTime(String timeStamp) {
        m_acctDlg.updateAccountTime(timeStamp);
    }

    public void accountDownloadEnd(String accountName) {
        m_acctDlg.accountDownloadEnd(accountName);

        String msg = EWrapperMsgGenerator.accountDownloadEnd(accountName);
        m_TWS.add(msg);
    }

    public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {
        String msg = EWrapperMsgGenerator.updateNewsBulletin(msgId, msgType, message, origExchange);
        JOptionPane.showMessageDialog(this, msg, "IB News Bulletin", JOptionPane.INFORMATION_MESSAGE);
    }

    public void managedAccounts(String accountsList) {
        m_bIsFAAccount = true;
        m_FAAcctCodes = accountsList;
        String msg = EWrapperMsgGenerator.managedAccounts(accountsList);
        m_TWS.add(msg);
    }

    public void historicalData(int reqId, Bar bar) {
        String msg = EWrapperMsgGenerator.historicalData(reqId, bar.time(), bar.open(), bar.high(), bar.low(), bar.close(), bar.volume(), bar.count(), bar.wap());
        m_tickers.add(msg);
    }

    public void historicalDataEnd(int reqId, String startDate, String endDate) {
        String msg = EWrapperMsgGenerator.historicalDataEnd(reqId, startDate, endDate);
        m_tickers.add(msg);
    }

    public void realtimeBar(int reqId, long time, double open, double high, double low, double close, Decimal volume, Decimal wap, int count) {
        String msg = EWrapperMsgGenerator.realtimeBar(reqId, time, open, high, low, close, volume, wap, count);
        m_tickers.add(msg);
    }

    public void scannerParameters(String xml) {
        displayXML(EWrapperMsgGenerator.SCANNER_PARAMETERS, xml);
    }

    public void currentTime(long time) {
        String msg = EWrapperMsgGenerator.currentTime(time);
        m_TWS.add(msg);
    }

    public void fundamentalData(int reqId, String data) {
        String msg = EWrapperMsgGenerator.fundamentalData(reqId, data);
        m_tickers.add(msg);
    }

    public void deltaNeutralValidation(int reqId, DeltaNeutralContract deltaNeutralContract) {
        String msg = EWrapperMsgGenerator.deltaNeutralValidation(reqId, deltaNeutralContract);
        m_TWS.add(msg);
    }

    private void displayXML(String title, String xml) {
        m_TWS.add(title);
        m_TWS.addText(xml);
    }

    public void receiveFA(int faDataType, String xml) {
        displayXML(EWrapperMsgGenerator.FINANCIAL_ADVISOR + " " + EClientSocket.faMsgTypeName(faDataType), xml);
        switch (faDataType) {
            case EClientSocket.GROUPS:
                faGroupXML = xml;
                break;
            case EClientSocket.PROFILES:
                faProfilesXML = xml;
                break;
            case EClientSocket.ALIASES:
                faAliasesXML = xml;
                break;
            default:
                return;
        }

        if (!faError &&
                !(faGroupXML == null || faProfilesXML == null || faAliasesXML == null)) {
            FinancialAdvisorDlg dlg = new FinancialAdvisorDlg(this);
            dlg.receiveInitialXML(faGroupXML, faProfilesXML, faAliasesXML);
            dlg.setVisible(true);

            if (!dlg.m_rc) {
                return;
            }

            m_client.replaceFA(0, EClientSocket.GROUPS, dlg.groupsXML);
            m_client.replaceFA(1, EClientSocket.PROFILES, dlg.profilesXML);
            m_client.replaceFA(2, EClientSocket.ALIASES, dlg.aliasesXML);

        }
    }

    public void marketDataType(int reqId, int marketDataType) {
        String msg = EWrapperMsgGenerator.marketDataType(reqId, marketDataType);
        m_tickers.add(msg);
    }

    public void commissionReport(CommissionReport commissionReport) {
        String msg = EWrapperMsgGenerator.commissionReport(commissionReport);
        m_TWS.add(msg);
    }

    private static void copyExtendedOrderDetails(Order destOrder, Order srcOrder) {
        destOrder.tif(srcOrder.getTif());
        destOrder.duration(srcOrder.duration());
        destOrder.postToAts(srcOrder.postToAts());
        destOrder.activeStartTime(srcOrder.activeStartTime());
        destOrder.activeStopTime(srcOrder.activeStopTime());
        destOrder.ocaGroup(srcOrder.ocaGroup());
        destOrder.ocaType(srcOrder.getOcaType());
        destOrder.openClose(srcOrder.openClose());
        destOrder.origin(srcOrder.origin());
        destOrder.orderRef(srcOrder.orderRef());
        destOrder.transmit(srcOrder.transmit());
        destOrder.parentId(srcOrder.parentId());
        destOrder.blockOrder(srcOrder.blockOrder());
        destOrder.sweepToFill(srcOrder.sweepToFill());
        destOrder.displaySize(srcOrder.displaySize());
        destOrder.triggerMethod(srcOrder.getTriggerMethod());
        destOrder.outsideRth(srcOrder.outsideRth());
        destOrder.hidden(srcOrder.hidden());
        destOrder.discretionaryAmt(srcOrder.discretionaryAmt());
        destOrder.goodAfterTime(srcOrder.goodAfterTime());
        destOrder.shortSaleSlot(srcOrder.shortSaleSlot());
        destOrder.designatedLocation(srcOrder.designatedLocation());
        destOrder.exemptCode(srcOrder.exemptCode());
        destOrder.ocaType(srcOrder.getOcaType());
        destOrder.rule80A(srcOrder.getRule80A());
        destOrder.allOrNone(srcOrder.allOrNone());
        destOrder.minQty(srcOrder.minQty());
        destOrder.percentOffset(srcOrder.percentOffset());
        destOrder.optOutSmartRouting(srcOrder.optOutSmartRouting());
        destOrder.auctionStrategy(srcOrder.auctionStrategy());
        destOrder.startingPrice(srcOrder.startingPrice());
        destOrder.stockRefPrice(srcOrder.stockRefPrice());
        destOrder.delta(srcOrder.delta());
        destOrder.stockRangeLower(srcOrder.stockRangeLower());
        destOrder.stockRangeUpper(srcOrder.stockRangeUpper());
        destOrder.overridePercentageConstraints(srcOrder.overridePercentageConstraints());
        destOrder.volatility(srcOrder.volatility());
        destOrder.volatilityType(srcOrder.getVolatilityType());
        destOrder.deltaNeutralOrderType(srcOrder.getDeltaNeutralOrderType());
        destOrder.deltaNeutralAuxPrice(srcOrder.deltaNeutralAuxPrice());
        destOrder.deltaNeutralConId(srcOrder.deltaNeutralConId());
        destOrder.deltaNeutralSettlingFirm(srcOrder.deltaNeutralSettlingFirm());
        destOrder.deltaNeutralClearingAccount(srcOrder.deltaNeutralClearingAccount());
        destOrder.deltaNeutralClearingIntent(srcOrder.deltaNeutralClearingIntent());
        destOrder.deltaNeutralOpenClose(srcOrder.deltaNeutralOpenClose());
        destOrder.deltaNeutralShortSale(srcOrder.deltaNeutralShortSale());
        destOrder.deltaNeutralShortSaleSlot(srcOrder.deltaNeutralShortSaleSlot());
        destOrder.deltaNeutralDesignatedLocation(srcOrder.deltaNeutralDesignatedLocation());
        destOrder.continuousUpdate(srcOrder.continuousUpdate());
        destOrder.referencePriceType(srcOrder.getReferencePriceType());
        destOrder.trailStopPrice(srcOrder.trailStopPrice());
        destOrder.trailingPercent(srcOrder.trailingPercent());
        destOrder.scaleInitLevelSize(srcOrder.scaleInitLevelSize());
        destOrder.scaleSubsLevelSize(srcOrder.scaleSubsLevelSize());
        destOrder.scalePriceIncrement(srcOrder.scalePriceIncrement());
        destOrder.scalePriceAdjustValue(srcOrder.scalePriceAdjustValue());
        destOrder.scalePriceAdjustInterval(srcOrder.scalePriceAdjustInterval());
        destOrder.scaleProfitOffset(srcOrder.scaleProfitOffset());
        destOrder.scaleAutoReset(srcOrder.scaleAutoReset());
        destOrder.scaleInitPosition(srcOrder.scaleInitPosition());
        destOrder.scaleInitFillQty(srcOrder.scaleInitFillQty());
        destOrder.scaleRandomPercent(srcOrder.scaleRandomPercent());
        destOrder.scaleTable(srcOrder.scaleTable());
        destOrder.hedgeType(srcOrder.getHedgeType());
        destOrder.hedgeParam(srcOrder.hedgeParam());
        destOrder.account(srcOrder.account());
        destOrder.modelCode(srcOrder.modelCode());
        destOrder.settlingFirm(srcOrder.settlingFirm());
        destOrder.clearingAccount(srcOrder.clearingAccount());
        destOrder.clearingIntent(srcOrder.clearingIntent());
        destOrder.solicited(srcOrder.solicited());
        destOrder.randomizePrice(srcOrder.randomizePrice());
        destOrder.randomizeSize(srcOrder.randomizeSize());
        destOrder.mifid2DecisionMaker(srcOrder.mifid2DecisionMaker());
        destOrder.mifid2DecisionAlgo(srcOrder.mifid2DecisionAlgo());
        destOrder.mifid2ExecutionTrader(srcOrder.mifid2ExecutionTrader());
        destOrder.mifid2ExecutionAlgo(srcOrder.mifid2ExecutionAlgo());
        destOrder.dontUseAutoPriceForHedge(srcOrder.dontUseAutoPriceForHedge());
        destOrder.isOmsContainer(srcOrder.isOmsContainer());
        destOrder.discretionaryUpToLimitPrice(srcOrder.discretionaryUpToLimitPrice());
        destOrder.notHeld(srcOrder.notHeld());
        destOrder.autoCancelParent(srcOrder.autoCancelParent());
        destOrder.advancedErrorOverride(srcOrder.advancedErrorOverride());
        destOrder.manualOrderTime(srcOrder.manualOrderTime());
        destOrder.minTradeQty(srcOrder.minTradeQty());
        destOrder.minCompeteSize(srcOrder.minCompeteSize());
        destOrder.competeAgainstBestOffset(srcOrder.competeAgainstBestOffset());
        destOrder.midOffsetAtWhole(srcOrder.midOffsetAtWhole());
        destOrder.midOffsetAtHalf(srcOrder.midOffsetAtHalf());
    }

    public void position(String account, Contract contract, Decimal pos, double avgCost) {
        String msg = EWrapperMsgGenerator.position(account, contract, pos, avgCost);
        m_TWS.add(msg);
    }

    public void positionEnd() {
        String msg = EWrapperMsgGenerator.positionEnd();
        m_TWS.add(msg);
    }

    public void accountSummary(int reqId, String account, String tag, String value, String currency) {
        String msg = EWrapperMsgGenerator.accountSummary(reqId, account, tag, value, currency);
        m_TWS.add(msg);
    }

    public void accountSummaryEnd(int reqId) {
        String msg = EWrapperMsgGenerator.accountSummaryEnd(reqId);
        m_TWS.add(msg);
    }

    public void positionMulti(int reqId, String account, String modelCode, Contract contract, Decimal pos, double avgCost) {
        String msg = EWrapperMsgGenerator.positionMulti(reqId, account, modelCode, contract, pos, avgCost);
        m_TWS.add(msg);
    }

    public void positionMultiEnd(int reqId) {
        String msg = EWrapperMsgGenerator.positionMultiEnd(reqId);
        m_TWS.add(msg);
    }

    public void accountUpdateMulti(int reqId, String account, String modelCode, String key, String value, String currency) {
        String msg = EWrapperMsgGenerator.accountUpdateMulti(reqId, account, modelCode, key, value, currency);
        m_TWS.add(msg);
    }

    public void accountUpdateMultiEnd(int reqId) {
        String msg = EWrapperMsgGenerator.accountUpdateMultiEnd(reqId);
        m_TWS.add(msg);
    }

    public void verifyMessageAPI(String apiData) { /* Empty */ }

    public void verifyCompleted(boolean isSuccessful, String errorText) { /* Empty */ }

    public void verifyAndAuthMessageAPI(String apiData, String xyzChallenge) { /* Empty */ }

    public void verifyAndAuthCompleted(boolean isSuccessful, String errorText) { /* Empty */ }

    public void displayGroupList(int reqId, String groups) {
        m_groupsDlg.displayGroupList(reqId, groups);
    }

    public void displayGroupUpdated(int reqId, String contractInfo) {
        m_groupsDlg.displayGroupUpdated(reqId, contractInfo);
    }

    public void connectAck() {
        if (m_client.isAsyncEConnect())
            m_client.startAPI();
    }

    @Override
    public void securityDefinitionOptionalParameter(int reqId, String exchange, int underlyingConId, String tradingClass,
                                                    String multiplier, Set<String> expirations, Set<Double> strikes) {
        String msg = EWrapperMsgGenerator.securityDefinitionOptionalParameter(reqId, exchange, underlyingConId, tradingClass, multiplier, expirations, strikes);
        m_TWS.add(msg);
    }

    @Override
    public void securityDefinitionOptionalParameterEnd(int reqId) {
    }

    @Override
    public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {
        String msg = EWrapperMsgGenerator.softDollarTiers(tiers);

        m_TWS.add(msg);
    }

    @Override
    public void familyCodes(FamilyCode[] familyCodes) {
        String msg = EWrapperMsgGenerator.familyCodes(familyCodes);
        m_TWS.add(msg);
    }

    @Override
    public void symbolSamples(int reqId, ContractDescription[] contractDescriptions) {
        String msg = EWrapperMsgGenerator.symbolSamples(reqId, contractDescriptions);
        m_TWS.add(msg);
    }

    @Override
    public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {
        String msg = EWrapperMsgGenerator.mktDepthExchanges(depthMktDataDescriptions);
        m_TWS.add(msg);
    }

    @Override
    public void tickNews(int tickerId, long timeStamp, String providerCode, String articleId, String headline, String extraData) {
        String msg = EWrapperMsgGenerator.tickNews(tickerId, timeStamp, providerCode, articleId, headline, extraData);
        m_TWS.add(msg);
    }

    @Override
    public void smartComponents(int reqId, Map<Integer, Entry<String, Character>> theMap) {
        String msg = EWrapperMsgGenerator.smartComponents(reqId, theMap);

        m_TWS.add(msg);
    }

    @Override
    public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {
        String msg = EWrapperMsgGenerator.tickReqParams(tickerId, minTick, bboExchange, snapshotPermissions);

        m_tickers.add(msg);
    }

    @Override
    public void newsProviders(NewsProvider[] newsProviders) {
        String msg = EWrapperMsgGenerator.newsProviders(newsProviders);
        m_TWS.add(msg);
    }

    @Override
    public void newsArticle(int requestId, int articleType, String articleText) {
        String msg = EWrapperMsgGenerator.newsArticle(requestId, articleType, articleText);
        m_TWS.add(msg);
        if (articleType == 1) {
            String path = m_newsArticleDlg.m_retPath;
            try {
                byte[] bytes = Base64.getDecoder().decode(articleText);
                FileOutputStream fos = new FileOutputStream(path);
                fos.write(bytes);
                fos.close();
                m_TWS.add("Binary/pdf article was saved to " + path);
            } catch (IOException ex) {
                m_TWS.add("Binary/pdf article was not saved to " + path + " due to error: " + ex.getMessage());
            }
        }
    }

    @Override
    public void historicalNews(int requestId, String time, String providerCode, String articleId, String headline) {
        String msg = EWrapperMsgGenerator.historicalNews(requestId, time, providerCode, articleId, headline);
        m_TWS.add(msg);
    }

    @Override
    public void historicalNewsEnd(int requestId, boolean hasMore) {
        String msg = EWrapperMsgGenerator.historicalNewsEnd(requestId, hasMore);
        m_TWS.add(msg);
    }

    @Override
    public void headTimestamp(int reqId, String headTimestamp) {
        String msg = EWrapperMsgGenerator.headTimestamp(reqId, headTimestamp);

        m_TWS.add(msg);
    }

    @Override
    public void histogramData(int reqId, List<HistogramEntry> items) {
        String msg = EWrapperMsgGenerator.histogramData(reqId, items);

        m_TWS.add(msg);
    }

    @Override
    public void historicalDataUpdate(int reqId, Bar bar) {
        historicalData(reqId, bar);
    }

    @Override
    public void rerouteMktDataReq(int reqId, int conId, String exchange) {
        String msg = EWrapperMsgGenerator.rerouteMktDataReq(reqId, conId, exchange);

        m_TWS.add(msg);
    }

    @Override
    public void rerouteMktDepthReq(int reqId, int conId, String exchange) {
        String msg = EWrapperMsgGenerator.rerouteMktDepthReq(reqId, conId, exchange);

        m_TWS.add(msg);
    }

    @Override
    public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {
        String msg = EWrapperMsgGenerator.marketRule(marketRuleId, priceIncrements);

        m_TWS.add(msg);
    }

    @Override
    public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {
        String msg = EWrapperMsgGenerator.pnl(reqId, dailyPnL, unrealizedPnL, realizedPnL);

        m_TWS.add(msg);
    }

    @Override
    public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value) {
        String msg = EWrapperMsgGenerator.pnlSingle(reqId, pos, dailyPnL, unrealizedPnL, realizedPnL, value);

        m_TWS.add(msg);
    }

    @Override
    public void historicalTicks(int reqId, List<HistoricalTick> ticks, boolean last) {
        StringBuilder msg = new StringBuilder();

        for (HistoricalTick tick : ticks) {
            msg.append(EWrapperMsgGenerator.historicalTick(reqId, tick.time(), tick.price(), tick.size()));
            msg.append("\n");
        }

        m_TWS.add(msg.toString());
    }

    @Override
    public void historicalTicksBidAsk(int reqId, List<HistoricalTickBidAsk> ticks, boolean done) {
        StringBuilder msg = new StringBuilder();

        for (HistoricalTickBidAsk tick : ticks) {
            msg.append(EWrapperMsgGenerator.historicalTickBidAsk(reqId, tick.time(), tick.tickAttribBidAsk(), tick.priceBid(), tick.priceAsk(), tick.sizeBid(),
                    tick.sizeAsk()));
            msg.append("\n");
        }

        m_TWS.add(msg.toString());
    }


    @Override
    public void historicalTicksLast(int reqId, List<HistoricalTickLast> ticks, boolean done) {
        StringBuilder msg = new StringBuilder();

        for (HistoricalTickLast tick : ticks) {
            msg.append(EWrapperMsgGenerator.historicalTickLast(reqId, tick.time(), tick.tickAttribLast(), tick.price(), tick.size(), tick.exchange(),
                    tick.specialConditions()));
            msg.append("\n");
        }

        m_TWS.add(msg.toString());
    }

    @Override
    public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size, TickAttribLast tickAttribLast,
                                  String exchange, String specialConditions) {
        String msg = EWrapperMsgGenerator.tickByTickAllLast(reqId, tickType, time, price, size, tickAttribLast, exchange, specialConditions);

        m_tickers.add(msg);
    }

    @Override
    public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice, Decimal bidSize, Decimal askSize,
                                 TickAttribBidAsk tickAttribBidAsk) {
        String msg = EWrapperMsgGenerator.tickByTickBidAsk(reqId, time, bidPrice, askPrice, bidSize, askSize, tickAttribBidAsk);

        m_tickers.add(msg);
    }

    @Override
    public void tickByTickMidPoint(int reqId, long time, double midPoint) {
        String msg = EWrapperMsgGenerator.tickByTickMidPoint(reqId, time, midPoint);

        m_tickers.add(msg);
    }

    @Override
    public void orderBound(long orderId, int apiClientId, int apiOrderId) {
        String msg = EWrapperMsgGenerator.orderBound(orderId, apiClientId, apiOrderId);

        m_TWS.add(msg);
    }

    @Override
    public void completedOrder(Contract contract, Order order, OrderState orderState) {
        String msg = EWrapperMsgGenerator.completedOrder(contract, order, orderState);
        m_TWS.add(msg);
    }

    @Override
    public void completedOrdersEnd() {
        String msg = EWrapperMsgGenerator.completedOrdersEnd();
        m_TWS.add(msg);
    }

    @Override
    public void replaceFAEnd(int reqId, String text) {
        String msg = EWrapperMsgGenerator.replaceFAEnd(reqId, text);
        m_TWS.add(msg);
    }

    @Override
    public void wshMetaData(int reqId, String dataJson) {
        String msg = EWrapperMsgGenerator.wshMetaData(reqId, dataJson);
        m_TWS.add(msg);
    }

    @Override
    public void wshEventData(int reqId, String dataJson) {
        String msg = EWrapperMsgGenerator.wshEventData(reqId, dataJson);
        m_TWS.add(msg);
    }

    @Override
    public void historicalSchedule(int reqId, String startDateTime, String endDateTime, String timeZone, List<HistoricalSession> sessions) {
        String msg = EWrapperMsgGenerator.historicalSchedule(reqId, startDateTime, endDateTime, timeZone, sessions);
        m_TWS.add(msg);
    }

    @Override
    public void userInfo(int reqId, String whiteBrandingId) {
        String msg = EWrapperMsgGenerator.userInfo(reqId, whiteBrandingId);
        m_TWS.add(msg);
    }
}
