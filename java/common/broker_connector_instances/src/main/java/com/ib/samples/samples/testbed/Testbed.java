/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.samples.testbed;

import java.text.SimpleDateFormat;
import java.util.*;

import com.ib.client.*;
import com.ib.samples.samples.testbed.advisor.FAMethodSamples;
import com.ib.samples.samples.testbed.contracts.ContractSamples;
import com.ib.samples.samples.testbed.orders.AvailableAlgoParams;
import com.ib.samples.samples.testbed.orders.OrderSamples;
import com.ib.samples.samples.testbed.scanner.ScannerSubscriptionSamples;

import com.ib.client.Types.FADataType;

public class Testbed {

    public static void main(String[] args) throws InterruptedException {
        EWrapperImpl wrapper = new EWrapperImpl();

        final EClientSocket m_client = wrapper.getClient();
        final EReaderSignal m_signal = wrapper.getSignal();
        //! [connect]
        m_client.eConnect("127.0.0.1", 7497, 2);
        //! [connect]
        //! [ereader]
        final EReader reader = new EReader(m_client, m_signal);

        reader.start();
        //An additional thread is created in this program design to empty the messaging queue
        new Thread(() -> {
            while (m_client.isConnected()) {
                m_signal.waitForSignal();
                try {
                    reader.processMsgs();
                } catch (Exception e) {
                    System.out.println("Exception: " + e.getMessage());
                }
            }
        }).start();
        //! [ereader]
        // A pause to give the application time to establish the connection
        // In a production application, it would be best to wait for callbacks to confirm the connection is complete
        Thread.sleep(1000);

        //tickByTickOperations(wrapper.getClient());
        //tickDataOperations(wrapper.getClient());
        //tickOptionComputations(wrapper.getClient());
        //optionsOperations(wrapper.getClient());
        //orderOperations(wrapper.getClient(), wrapper.getCurrentOrderId());
        contractOperations(wrapper.getClient());
        //hedgeSample(wrapper.getClient(), wrapper.getCurrentOrderId());
        //testAlgoSamples(wrapper.getClient(), wrapper.getCurrentOrderId());
        //bracketSample(wrapper.getClient(), wrapper.getCurrentOrderId());
        //bulletins(wrapper.getClient());
        //fundamentals(wrapper.getClient());
        //marketScanners(wrapper.getClient());
        //marketDataType(wrapper.getClient());
        //historicalDataRequests(wrapper.getClient());
        //accountOperations(wrapper.getClient());
        //newsOperations(wrapper.getClient());
        //marketDepthOperations(wrapper.getClient());
        //rerouteCFDOperations(wrapper.getClient());
        //marketRuleOperations(wrapper.getClient());
        //tickDataOperations(wrapper.getClient());
        //pnlSingle(wrapper.getClient());
        //continuousFuturesOperations(wrapper.getClient());
        //pnlSingle(wrapper.getClient());
        //histogram(wrapper.getClient());
        //whatIfSamples(wrapper.getClient(), wrapper.getCurrentOrderId());
        //historicalTicks(wrapper.getClient());
        //financialAdvisorOperations(wrapper.getClient());
        //realTimeBars(wrapper.getClient());
        //wshCalendarOperations(wrapper.getClient());

        Thread.sleep(100000);
        m_client.eDisconnect();
    }

    private static void histogram(EClientSocket client) {
        client.reqHistogramData(4002, ContractSamples.USStock(), false, "3 days");

        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        client.cancelHistogramData(4002);
    }

    private static void historicalTicks(EClientSocket client) {
        //! [reqhistoricalticks]
        client.reqHistoricalTicks(18001, ContractSamples.USStockAtSmart(), "20220808 10:00:00 US/Eastern", null, 10, "TRADES", 1, true, null);
        client.reqHistoricalTicks(18002, ContractSamples.USStockAtSmart(), "20220808 10:00:00 US/Eastern", null, 10, "BID_ASK", 1, true, null);
        client.reqHistoricalTicks(18003, ContractSamples.USStockAtSmart(), "20220808 10:00:00 US/Eastern", null, 10, "MIDPOINT", 1, true, null);
        //! [reqhistoricalticks]
    }

    private static void pnl(EClientSocket client) throws InterruptedException {
        //! [reqpnl]
        client.reqPnL(17001, "DUD00029", "");
        //! [reqpnl]
        Thread.sleep(1000);
        //! [cancelpnl]
        client.cancelPnL(17001);
        //! [cancelpnl]
    }

    private static void pnlSingle(EClientSocket client) throws InterruptedException {
        //! [reqpnlsingle]
        client.reqPnLSingle(17001, "DUD00029", "", 268084);
        //! [reqpnlsingle]
        Thread.sleep(1000);
        //! [cancelpnlsingle]
        client.cancelPnLSingle(17001);
        //! [cancelpnlsingle]
    }

    private static void orderOperations(EClientSocket client, int nextOrderId) throws InterruptedException {

        /*** Requesting the next valid id ***/
        //! [reqids]
        //The parameter is always ignored.
        client.reqIds(-1);
        //! [reqids]
        //Thread.sleep(1000);
        /*** Requesting all open orders ***/
        //! [reqallopenorders]
        client.reqAllOpenOrders();
        //! [reqallopenorders]
        //Thread.sleep(1000);
        /*** Taking over orders to be submitted via TWS ***/
        //! [reqautoopenorders]
        client.reqAutoOpenOrders(true);
        //! [reqautoopenorders]
        //Thread.sleep(1000);
        /*** Requesting this API client's orders ***/
        //! [reqopenorders]
        client.reqOpenOrders();
        //! [reqopenorders]
        //Thread.sleep(1000);

        /*** Placing/modifying an order - remember to ALWAYS increment the nextValidId after placing an order so it can be used for the next one! ***/
        //! [order_submission]
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), OrderSamples.LimitOrder("SELL", Decimal.ONE, 50));
        //! [order_submission]

        //! [place_midprice]
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), OrderSamples.Midprice("BUY", Decimal.ONE, 150));
        //! [place_midprice]

        //! [faorderoneaccount]
        Order faOrderOneAccount = OrderSamples.MarketOrder("BUY", Decimal.ONE_HUNDRED);
        // Specify the Account Number directly
        faOrderOneAccount.account("DU119915");
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), faOrderOneAccount);
        //! [faorderoneaccount]

        //! [faordergroupequalquantity]
        Order faOrderGroupEQ = OrderSamples.LimitOrder("SELL", Decimal.get(200), 2000);
        faOrderGroupEQ.faGroup("Group_Equal_Quantity");
        faOrderGroupEQ.faMethod("EqualQuantity");
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), faOrderGroupEQ);
        //! [faordergroupequalquantity]

        //! [faordergrouppctchange]
        Order faOrderGroupPC = OrderSamples.MarketOrder("BUY", Decimal.ZERO);
        // You should not specify any order quantity for PctChange allocation method
        faOrderGroupPC.faGroup("Pct_Change");
        faOrderGroupPC.faMethod("PctChange");
        faOrderGroupPC.faPercentage("100");
        client.placeOrder(nextOrderId++, ContractSamples.EurGbpFx(), faOrderGroupPC);
        //! [faordergrouppctchange]

        //! [faorderprofile]
        Order faOrderProfile = OrderSamples.LimitOrder("BUY", Decimal.get(200), 100);
        faOrderProfile.faProfile("Percent_60_40");
        client.placeOrder(nextOrderId++, ContractSamples.EuropeanStock(), faOrderProfile);
        //! [faorderprofile]

        //! [modelorder]
        Order modelOrder = OrderSamples.LimitOrder("BUY", Decimal.get(200), 100);
        modelOrder.account("DF12345");  // master FA account number
        modelOrder.modelCode("Technology"); // model for tech stocks first created in TWS
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), modelOrder);
        //! [modelorder]

        //client.placeOrder(nextOrderId++, ContractSamples.USStock(), OrderSamples.PeggedToMarket("BUY", 10, 0.01));
        //client.placeOrder(nextOrderId++, ContractSamples.EurGbpFx(), OrderSamples.MarketOrder("BUY", 10));
        //client.placeOrder(nextOrderId++, ContractSamples.USStock(), OrderSamples.Discretionary("SELL", 1, 45, 0.5));

        //! [reqexecutions]
        client.reqExecutions(10001, new ExecutionFilter());
        //! [reqexecutions]

        int cancelID = nextOrderId - 1;
        //! [cancelorder]
        client.cancelOrder(cancelID, Order.EMPTY_STR);
        //! [cancelorder]

        //! [reqglobalcancel]
        client.reqGlobalCancel();
        //! [reqglobalcancel]

        /*** Completed orders ***/
        //! [reqcompletedorders]
        client.reqCompletedOrders(false);
        //! [reqcompletedorders]

        //! [crypto_order_submission]
        client.placeOrder(nextOrderId++, ContractSamples.CryptoContract(), OrderSamples.LimitOrder("BUY", Decimal.parse("0.00001234"), 3370));
        //! [crypto_order_submission]

        //! [manual_order_time]
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), OrderSamples.LimitOrderWithManualOrderTime("BUY", Decimal.get(100), 111.11, "20220314-13:00:00"));
        //! [manual_order_time]

        //! [manual_order_cancel_time]
        cancelID = nextOrderId - 1;
        client.cancelOrder(cancelID, "20220314-19:00:00");
        //! [manual_order_cancel_time]

        //! [pegbest_up_to_mid_order_submission]
        client.placeOrder(nextOrderId++, ContractSamples.IBKRATSContract(), OrderSamples.PegBestUpToMidOrder("BUY", Decimal.get(100), 111.11, 100, 200, 0.02, 0.025));
        //! [pegbest_up_to_mid_order_submission]

        //! [pegbest_order_submission]
        client.placeOrder(nextOrderId++, ContractSamples.IBKRATSContract(), OrderSamples.PegBestOrder("BUY", Decimal.get(100), 111.11, 100, 200, 0.03));
        //! [pegbest_order_submission]

        //! [pegmid_order_submission]
        client.placeOrder(nextOrderId++, ContractSamples.IBKRATSContract(), OrderSamples.PegMidOrder("BUY", Decimal.get(100), 111.11, 100, 0.02, 0.025));
        //! [pegmid_order_submission]

        Thread.sleep(10000);

    }

    private static void OcaSample(EClientSocket client, int nextOrderId) {

        //OCA order
        //! [ocasubmit]
        List<Order> OcaOrders = new ArrayList<>();
        OcaOrders.add(OrderSamples.LimitOrder("BUY", Decimal.ONE, 10));
        OcaOrders.add(OrderSamples.LimitOrder("BUY", Decimal.ONE, 11));
        OcaOrders.add(OrderSamples.LimitOrder("BUY", Decimal.ONE, 12));
        OcaOrders = OrderSamples.OneCancelsAll("TestOCA_" + nextOrderId, OcaOrders, 2);
        for (Order o : OcaOrders) {

            client.placeOrder(nextOrderId++, ContractSamples.USStock(), o);
        }
        //! [ocasubmit]

    }

    private static void tickDataOperations(EClientSocket client) throws InterruptedException {

        /*** Requesting real time market data ***/
        //Thread.sleep(1000);
        //! [reqmktdata]
        client.reqMktData(1001, ContractSamples.StockComboContract(), "", false, false, null);
        //! [reqmktdata]

        //! [reqsmartcomponents]
        client.reqSmartComponents(1013, "a6");
        //! [reqsmartcomponents]

        //! [reqmktdata_snapshot]
        client.reqMktData(1003, ContractSamples.FutureComboContract(), "", true, false, null);
        //! [reqmktdata_snapshot]

		/* 
		//! [regulatorysnapshot] 
		// Each regulatory snapshot request incurs a 0.01 USD fee
		client.reqMktData(1014, ContractSamples.USStock(), "", false, true, null);
		//! [regulatorysnapshot]
		*/

        //! [reqmktdata_genticks]
        //Requesting RTVolume (Time & Sales) and shortable generic ticks
        client.reqMktData(1004, ContractSamples.USStockAtSmart(), "233,236", false, false, null);
        //! [reqmktdata_genticks]
        //! [reqmktdata_contractnews]
        // Without the API news subscription this will generate an "invalid tick type" error
        client.reqMktData(1005, ContractSamples.USStock(), "mdoff,292:BZ", false, false, null);
        client.reqMktData(1006, ContractSamples.USStock(), "mdoff,292:BT", false, false, null);
        client.reqMktData(1007, ContractSamples.USStock(), "mdoff,292:FLY", false, false, null);
        client.reqMktData(1008, ContractSamples.USStock(), "mdoff,292:DJ-RT", false, false, null);
        //! [reqmktdata_contractnews]
        //! [reqmktdata_broadtapenews]
        client.reqMktData(1009, ContractSamples.BTbroadtapeNewsFeed(), "mdoff,292", false, false, null);
        client.reqMktData(1010, ContractSamples.BZbroadtapeNewsFeed(), "mdoff,292", false, false, null);
        client.reqMktData(1011, ContractSamples.FLYbroadtapeNewsFeed(), "mdoff,292", false, false, null);
        //! [reqmktdata_broadtapenews]
        //! [reqoptiondatagenticks]
        //Requesting data for an option contract will return the greek values
        client.reqMktData(1002, ContractSamples.OptionWithLocalSymbol(), "", false, false, null);
        //! [reqoptiondatagenticks]
        //! [reqfuturesopeninterest]
        //Requesting data for a futures contract will return the futures open interest
        client.reqMktData(1014, ContractSamples.SimpleFuture(), "mdoff,588", false, false, null);
        //! [reqfuturesopeninterest]

        //! [reqmktdata_preopenbidask]
        //Requesting data for a futures contract will return the pre-open bid/ask flag
        client.reqMktData(1015, ContractSamples.SimpleFuture(), "", false, false, null);
        //! [reqmktData_preopenbidask]

        //! [reqavgoptvolume]
        //Requesting data for a stock will return the average option volume
        client.reqMktData(1016, ContractSamples.USStockAtSmart(), "mdoff,105", false, false, null);
        //! [reqavgoptvolume]

        //! [reqetfticks]
        client.reqMktData(1017, ContractSamples.etf(), "mdoff,576,577,578,614,623", false, false, null);
        //! [reqetfticks]

        //! [IPOPrice]
        client.reqMktData(1018, ContractSamples.StockWithIPOPrice(), "mdoff,586", false, false, null);
        //! [IPOPrice]

        Thread.sleep(10000);
        //! [cancelmktdata]
        client.cancelMktData(1001);
        client.cancelMktData(1002);
        client.cancelMktData(1003);
        client.cancelMktData(1014);
        client.cancelMktData(1015);
        client.cancelMktData(1016);
        client.cancelMktData(1017);
        client.cancelMktData(1018);
        //! [cancelmktdata]

    }

    private static void tickOptionComputations(EClientSocket client) throws InterruptedException {

        /*** Requesting real time market data ***/
        client.reqMarketDataType(4);

        //! [reqmktdata]
        client.reqMktData(2001, ContractSamples.OptionWithLocalSymbol(), "", false, false, null);
        //! [reqmktdata]

        Thread.sleep(10000);

        //! [cancelmktdata]
        client.cancelMktData(2001);
        //! [cancelmktdata]
    }

    private static void historicalDataRequests(EClientSocket client) throws InterruptedException {

        /*** Requesting historical data ***/

        //! [reqHeadTimeStamp]
        client.reqHeadTimestamp(4003, ContractSamples.USStock(), "TRADES", 1, 1);
        //! [reqHeadTimeStamp]

        //! [cancelHeadTimestamp]
        client.cancelHeadTimestamp(4003);
        //! [cancelHeadTimestamp]

        //! [reqhistoricaldata]
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.MONTH, -6);
        SimpleDateFormat form = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
        String formatted = form.format(cal.getTime());
        client.reqHistoricalData(4001, ContractSamples.EurGbpFx(), formatted, "1 M", "1 day", "MIDPOINT", 1, 1, false, null);
        client.reqHistoricalData(4002, ContractSamples.EuropeanStock(), formatted, "10 D", "1 min", "TRADES", 1, 1, false, null);
        client.reqHistoricalData(4003, ContractSamples.USStockAtSmart(), formatted, "1 M", "1 day", "SCHEDULE", 1, 1, false, null);
        Thread.sleep(2000);
        /*** Canceling historical data requests ***/
        client.cancelHistoricalData(4001);
        client.cancelHistoricalData(4002);
        client.cancelHistoricalData(4003);
        //! [reqhistoricaldata]

        //! [reqHistogramData]
		/*client.reqHistogramData(4004, ContractSamples.USStock(), false, "3 days");
        //! [reqHistogramData]
		Thread.sleep(5);
		
		//! [cancelHistogramData]
        client.cancelHistogramData(4004);*/
        //! [cancelHistogramData]
    }

    private static void realTimeBars(EClientSocket client) throws InterruptedException {

        /*** Requesting real time bars ***/
        //! [reqrealtimebars]
        client.reqRealTimeBars(3001, ContractSamples.EurGbpFx(), 5, "MIDPOINT", true, null);
        //! [reqrealtimebars]
        Thread.sleep(2000);
        /*** Canceling real time bars ***/
        //! [cancelrealtimebars]
        client.cancelRealTimeBars(3001);
        //! [cancelrealtimebars]

    }

    private static void marketDepthOperations(EClientSocket client) throws InterruptedException {

        /*** Requesting the Deep Book ***/

        //! [reqMktDepthExchanges]
        client.reqMktDepthExchanges();
        //! [reqMktDepthExchanges]

        //! [reqmarketdepth]
        client.reqMktDepth(2001, ContractSamples.EurGbpFx(), 5, false, null);
        //! [reqmarketdepth]
        Thread.sleep(2000);
        /*** Canceling the Deep Book request ***/
        //! [cancelmktdepth]
        client.cancelMktDepth(2001, false);
        //! [cancelmktdepth]

        //! [reqmarketdepth]
        client.reqMktDepth(2002, ContractSamples.EuropeanStock(), 5, true, null);
        //! [reqmarketdepth]
        Thread.sleep(5000);
        //! [cancelmktdepth]
        client.cancelMktDepth(2002, true);
        //! [cancelmktdepth]
    }

    private static void accountOperations(EClientSocket client) throws InterruptedException {

        //client.reqAccountUpdatesMulti(9002, null, "EUstocks", true);
        //! [reqpositionsmulti]
        client.reqPositionsMulti(9003, "DU74649", "EUstocks");
        //! [reqpositionsmulti]
        Thread.sleep(10000);

        /*** Requesting managed accounts***/
        //! [reqmanagedaccts]
        client.reqManagedAccts();
        //! [reqmanagedaccts]

        /*** Requesting family codes***/
        //! [reqfamilycodes]
        client.reqFamilyCodes();
        //! [reqfamilycodes]

        /*** Requesting accounts' summary ***/
        Thread.sleep(2000);
        //! [reqaaccountsummary]
        client.reqAccountSummary(9001, "All", "AccountType,NetLiquidation,TotalCashValue,SettledCash,AccruedCash,BuyingPower,EquityWithLoanValue,PreviousEquityWithLoanValue,GrossPositionValue,ReqTEquity,ReqTMargin,SMA,InitMarginReq,MaintMarginReq,AvailableFunds,ExcessLiquidity,Cushion,FullInitMarginReq,FullMaintMarginReq,FullAvailableFunds,FullExcessLiquidity,LookAheadNextChange,LookAheadInitMarginReq ,LookAheadMaintMarginReq,LookAheadAvailableFunds,LookAheadExcessLiquidity,HighestSeverity,DayTradesRemaining,Leverage");
        //! [reqaaccountsummary]

        //! [reqaaccountsummaryledger]
        client.reqAccountSummary(9002, "All", "$LEDGER");
        //! [reqaaccountsummaryledger]
        Thread.sleep(2000);
        //! [reqaaccountsummaryledgercurrency]
        client.reqAccountSummary(9003, "All", "$LEDGER:EUR");
        //! [reqaaccountsummaryledgercurrency]
        Thread.sleep(2000);
        //! [reqaaccountsummaryledgerall]
        client.reqAccountSummary(9004, "All", "$LEDGER:ALL");
        //! [reqaaccountsummaryledgerall]

        //! [cancelaaccountsummary]
        client.cancelAccountSummary(9001);
        client.cancelAccountSummary(9002);
        client.cancelAccountSummary(9003);
        client.cancelAccountSummary(9004);
        //! [cancelaaccountsummary]

        /*** Subscribing to an account's information. Only one at a time! ***/
        Thread.sleep(2000);
        //! [reqaaccountupdates]
        client.reqAccountUpdates(true, "U150462");
        //! [reqaaccountupdates]
        Thread.sleep(2000);
        //! [cancelaaccountupdates]
        client.reqAccountUpdates(false, "U150462");
        //! [cancelaaccountupdates]

        //! [reqaaccountupdatesmulti]
        client.reqAccountUpdatesMulti(9002, "U150462", "EUstocks", true);
        //! [reqaaccountupdatesmulti]
        Thread.sleep(2000);
        /*** Requesting all accounts' positions. ***/
        //! [reqpositions]
        client.reqPositions();
        //! [reqpositions]
        Thread.sleep(2000);
        //! [cancelpositions]
        client.cancelPositions();
        //! [cancelpositions]

        /*** Requesting user info. ***/
        //! [requserinfo]
        client.reqUserInfo(0);
        //! [requserinfo]
    }

    private static void newsOperations(EClientSocket client) throws InterruptedException {

        /*** Requesting news ticks ***/
        //! [reqNewsTicks]
        client.reqMktData(10001, ContractSamples.USStockAtSmart(), "mdoff,292", false, false, null);
        //! [reqNewsTicks]

        Thread.sleep(10000);

        /*** Canceling news ticks ***/
        //! [cancelNewsTicks]
        client.cancelMktData(10001);
        //! [cancelNewsTicks]

        Thread.sleep(2000);

        /*** Requesting news providers ***/
        //! [reqNewsProviders]
        client.reqNewsProviders();
        //! [reqNewsProviders]

        Thread.sleep(2000);

        /*** Requesting news article ***/
        //! [reqNewsArticle]
        client.reqNewsArticle(10002, "BZ", "BZ$04507322", null);
        //! [reqNewsArticle]

        Thread.sleep(5000);

        /*** Requesting historical news ***/
        //! [reqHistoricalNews]
        client.reqHistoricalNews(10003, 8314, "BZ+FLY", "", "", 10, null);
        //! [reqHistoricalNews]
    }

    private static void conditionSamples(EClientSocket client, int nextOrderId) {

        //! [order_conditioning_activate]
        Order mkt = OrderSamples.MarketOrder("BUY", Decimal.ONE_HUNDRED);
        //Order will become active if conditioning criteria is met
        mkt.conditionsCancelOrder(true);
        mkt.conditions().add(OrderSamples.PriceCondition(208813720, "SMART", 600, false, false));
        mkt.conditions().add(OrderSamples.ExecutionCondition("EUR.USD", "CASH", "IDEALPRO", true));
        mkt.conditions().add(OrderSamples.MarginCondition(30, true, false));
        mkt.conditions().add(OrderSamples.PercentageChangeCondition(15.0, 208813720, "SMART", true, true));
        mkt.conditions().add(OrderSamples.TimeCondition("20220909 10:00:00 US/Eastern", true, false));
        mkt.conditions().add(OrderSamples.VolumeCondition(208813720, "SMART", false, 100, true));
        client.placeOrder(nextOrderId++, ContractSamples.EuropeanStock(), mkt);
        //! [order_conditioning_activate]

        //Conditions can make the order active or cancel it. Only LMT orders can be conditionally canceled.
        //! [order_conditioning_cancel]
        Order lmt = OrderSamples.LimitOrder("BUY", Decimal.ONE_HUNDRED, 20);
        //The active order will be cancelled if conditioning criteria is met
        lmt.conditionsCancelOrder(true);
        lmt.conditions().add(OrderSamples.PriceCondition(208813720, "SMART", 600, false, false));
        client.placeOrder(nextOrderId++, ContractSamples.EuropeanStock(), lmt);
        //! [order_conditioning_cancel]

    }

    private static void contractOperations(EClientSocket client) {

        //! [reqcontractdetails]
        client.reqContractDetails(210, ContractSamples.OptionForQuery());
        client.reqContractDetails(211, ContractSamples.EurGbpFx());
        client.reqContractDetails(212, ContractSamples.Bond());
        client.reqContractDetails(213, ContractSamples.FuturesOnOptions());
        client.reqContractDetails(214, ContractSamples.SimpleFuture());
        client.reqContractDetails(215, ContractSamples.USStockAtSmart());
        client.reqContractDetails(216, ContractSamples.CryptoContract());
        client.reqContractDetails(217, ContractSamples.ByIssuerId());
        //! [reqcontractdetails]

        //! [reqmatchingsymbols]
        client.reqMatchingSymbols(211, "IB");
        //! [reqmatchingsymbols]

    }

    private static void contractNewsFeed(EClientSocket client) {

        //! [reqcontractdetailsnews]
        client.reqContractDetails(211, ContractSamples.NewsFeedForQuery());
        //! [reqcontractdetailsnews]

    }

    private static void hedgeSample(EClientSocket client, int nextOrderId) {

        //F Hedge order
        //! [hedgesubmit]
        //Parent order on a contract which currency differs from your base currency
        Order parent = OrderSamples.LimitOrder("BUY", Decimal.ONE_HUNDRED, 10);
        parent.orderId(nextOrderId++);
        parent.transmit(false);
        //Hedge on the currency conversion
        Order hedge = OrderSamples.MarketFHedge(parent.orderId(), "BUY");
        //Place the parent first...
        client.placeOrder(parent.orderId(), ContractSamples.EuropeanStock(), parent);
        //Then the hedge order
        client.placeOrder(nextOrderId++, ContractSamples.EurGbpFx(), hedge);
        //! [hedgesubmit]

    }

    private static void testAlgoSamples(EClientSocket client, int nextOrderId) throws InterruptedException {

        //! [scale_order]
        Order scaleOrder = OrderSamples.RelativePeggedToPrimary("BUY", Decimal.get(70000), 189, 0.01);
        AvailableAlgoParams.FillScaleParams(scaleOrder, 2000, 500, true, .02, 189.00, 3600, 2.00, true, 10, 40);
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), scaleOrder);
        //! [scale_order]

        Thread.sleep(500);

        //! [algo_base_order]
        Order baseOrder = OrderSamples.LimitOrder("BUY", Decimal.get(1000), 1);
        //! [algo_base_order]

        //! [arrivalpx]
        AvailableAlgoParams.FillArrivalPriceParams(baseOrder, 0.1, "Aggressive", "09:00:00 US/Eastern", "16:00:00 US/Eastern", true, true);
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
        //! [arrivalpx]

        Thread.sleep(500);

        //! [darkice]
        AvailableAlgoParams.FillDarkIceParams(baseOrder, 10, "09:00:00 US/Eastern", "16:00:00 US/Eastern", true);
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
        //! [darkice]

        Thread.sleep(500);

        //! [ad]
        // The Time Zone in "startTime" and "endTime" attributes is ignored and always defaulted to GMT
        AvailableAlgoParams.FillAccumulateDistributeParams(baseOrder, 10, 60, true, true, 1, true, true, "12:00:00", "16:00:00");
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
        //! [ad]

        Thread.sleep(500);

        //! [twap]
        AvailableAlgoParams.FillTwapParams(baseOrder, "Marketable", "10:00:00 US/Eastern", "11:00:00 US/Eastern", true);
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
        //! [twap]

        Thread.sleep(500);

        //! [vwap]
        AvailableAlgoParams.FillVwapParams(baseOrder, 0.2, "09:00:00 US/Eastern", "16:00:00 US/Eastern", true, true, true);
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
        //! [vwap]

        Thread.sleep(500);

        //! [balanceimpactrisk]
        AvailableAlgoParams.FillBalanceImpactRiskParams(baseOrder, 0.1, "Aggressive", true);
        client.placeOrder(nextOrderId++, ContractSamples.USOptionContract(), baseOrder);
        //! [balanceimpactrisk]

        Thread.sleep(500);

        //! [minimpact]
        AvailableAlgoParams.FillMinImpactParams(baseOrder, 0.3);
        client.placeOrder(nextOrderId++, ContractSamples.USOptionContract(), baseOrder);
        //! [minimpact]

        //! [adaptive]
        AvailableAlgoParams.FillAdaptiveParams(baseOrder, "Normal");
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
        //! [adaptive]

        //! [closepx]
        AvailableAlgoParams.FillClosePriceParams(baseOrder, 0.5, "Neutral", "12:00:00 US/Eastern", true);
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
        //! [closepx]

        //! [pctvol]
        AvailableAlgoParams.FillPctVolParams(baseOrder, 0.5, "12:00:00 US/Eastern", "14:00:00 US/Eastern", true);
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
        //! [pctvol]

        //! [pctvolpx]
        AvailableAlgoParams.FillPriceVariantPctVolParams(baseOrder, 0.1, 0.05, 0.01, 0.2, "12:00:00 US/Eastern", "14:00:00 US/Eastern", true);
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
        //! [pctvolpx]

        //! [pctvolsz]
        AvailableAlgoParams.FillSizeVariantPctVolParams(baseOrder, 0.2, 0.4, "12:00:00 US/Eastern", "14:00:00 US/Eastern", true);
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
        //! [pctvolsz]

        //! [pctvoltm]
        AvailableAlgoParams.FillTimeVariantPctVolParams(baseOrder, 0.2, 0.4, "12:00:00 US/Eastern", "14:00:00 US/Eastern", true);
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
        //! [pctvoltm]

        //! [jeff_vwap_algo]
        AvailableAlgoParams.FillJefferiesVWAPParams(baseOrder, "10:00:00 US/Eastern", "16:00:00 US/Eastern", 10, 10, "Exclude_Both", 130, 135, 1, 10, "Patience", false, "Midpoint");
        client.placeOrder(nextOrderId++, ContractSamples.JefferiesContract(), baseOrder);
        //! [jeff_vwap_algo]

        //! [csfb_inline_algo]
        AvailableAlgoParams.FillCSFBInlineParams(baseOrder, "10:00:00 US/Eastern", "16:00:00 US/Eastern", "Patient", 10, 20, 100, "Default", false, 40, 100, 100, 35);
        client.placeOrder(nextOrderId++, ContractSamples.CSFBContract(), baseOrder);
        //! [csfb_inline_algo]

        //! [qbalgo_strobe_algo]
        AvailableAlgoParams.FillQBAlgoInlineParams(baseOrder, "10:00:00 US/Eastern", "16:00:00 US/Eastern", -99, "TWAP", 0.25, true);
        client.placeOrder(nextOrderId++, ContractSamples.QBAlgoContract(), baseOrder);
        //! [qbalgo_strobe_algo]

    }

    private static void bracketSample(EClientSocket client, int nextOrderId) {

        //BRACKET ORDER
        //! [bracketsubmit]
        List<Order> bracket = OrderSamples.BracketOrder(nextOrderId++, "BUY", Decimal.ONE_HUNDRED, 30, 40, 20);
        for (Order o : bracket) {
            client.placeOrder(o.orderId(), ContractSamples.EuropeanStock(), o);
        }
        //! [bracketsubmit]

    }

    private static void bulletins(EClientSocket client) throws InterruptedException {

        //! [reqnewsbulletins]
        client.reqNewsBulletins(true);
        //! [reqnewsbulletins]

        Thread.sleep(2000);

        //! [cancelnewsbulletins]
        client.cancelNewsBulletins();
        //! [cancelnewsbulletins]

    }

    private static void fundamentals(EClientSocket client) throws InterruptedException {

        //! [reqfundamentaldata]
        client.reqFundamentalData(8001, ContractSamples.USStock(), "ReportsFinSummary", null);
        //! [reqfundamentaldata]

        Thread.sleep(2000);

        //! [fundamentalexamples]
        client.reqFundamentalData(8002, ContractSamples.USStock(), "ReportSnapshot", null); //for company overview
        client.reqFundamentalData(8003, ContractSamples.USStock(), "ReportRatios", null); //for financial ratios
        client.reqFundamentalData(8004, ContractSamples.USStock(), "ReportsFinStatements", null); //for financial statements
        client.reqFundamentalData(8005, ContractSamples.USStock(), "RESC", null); //for analyst estimates
        client.reqFundamentalData(8006, ContractSamples.USStock(), "CalendarReport", null); //for company calendar
        //! [fundamentalexamples]

        //! [cancelfundamentaldata]
        client.cancelFundamentalData(8001);
        //! [cancelfundamentaldata]

    }

    private static void marketScanners(EClientSocket client) throws InterruptedException {

        /*** Requesting all available parameters which can be used to build a scanner request ***/
        //! [reqscannerparameters]
        client.reqScannerParameters();
        //! [reqscannerparameters]
        Thread.sleep(2000);

        /*** Triggering a scanner subscription ***/
        //! [reqscannersubscription]
        client.reqScannerSubscription(7001, ScannerSubscriptionSamples.HighOptVolumePCRatioUSIndexes(), null, null);

        TagValue t1 = new TagValue("usdMarketCapAbove", "10000");
        TagValue t2 = new TagValue("optVolumeAbove", "1000");
        TagValue t3 = new TagValue("avgVolumeAbove", "100000000");

        List<TagValue> TagValues = Arrays.asList(t1, t2, t3);
        client.reqScannerSubscription(7002, ScannerSubscriptionSamples.HotUSStkByVolume(), null, TagValues); // requires TWS v973+

        //! [reqscannersubscription]

        //! [reqcomplexscanner]
        List<TagValue> AAPLConIDTag = Collections.singletonList(new TagValue("underConID", "265598")); // 265598 is the conID for AAPL stock
        client.reqScannerSubscription(7003, ScannerSubscriptionSamples.ComplexOrdersAndTrades(), null, AAPLConIDTag); // requires TWS v975+

        //! [reqcomplexscanner]


        Thread.sleep(4000);
        /*** Canceling the scanner subscription ***/
        //! [cancelscannersubscription]
        client.cancelScannerSubscription(7001);
        client.cancelScannerSubscription(7002);
        client.cancelScannerSubscription(7003);
        //! [cancelscannersubscription]

    }

    private static void wshCalendarOperations(EClientSocket client) throws InterruptedException {

        //! [reqmetadata]
        client.reqWshMetaData(1100);
        //! [reqmetadata]

        Thread.sleep(1000);

        client.cancelWshMetaData(1100);

        //! [reqeventdata]
        client.reqWshEventData(1101, new WshEventData(8314, false, false, false, "20220511", "", 5));
        //! [reqeventdata]

        Thread.sleep(3000);

        //! [reqeventdata]
        client.reqWshEventData(1102, new WshEventData("{\"watchlist\":[\"8314\"]}", false, false, false, "", "20220512", Integer.MAX_VALUE));
        //! [reqeventdata]

        Thread.sleep(1000);

        client.cancelWshEventData(1101);
        client.cancelWshEventData(1102);
    }

    private static void financialAdvisorOperations(EClientSocket client) {

        /*** Requesting FA information ***/
        //! [requestfaaliases]
        client.requestFA(FADataType.ALIASES.ordinal());
        //! [requestfaaliases]

        //! [requestfagroups]
        client.requestFA(FADataType.GROUPS.ordinal());
        //! [requestfagroups]

        //! [requestfaprofiles]
        client.requestFA(FADataType.PROFILES.ordinal());
        //! [requestfaprofiles]

        /*** Replacing FA information - Fill in with the appropriate XML string. ***/
        //! [replacefaonegroup]
        client.replaceFA(1000, FADataType.GROUPS.ordinal(), FAMethodSamples.FA_ONE_GROUP);
        //! [replacefaonegroup]

        //! [replacefatwogroups]
        client.replaceFA(1001, FADataType.GROUPS.ordinal(), FAMethodSamples.FA_TWO_GROUPS);
        //! [replacefatwogroups]

        //! [replacefaoneprofile]
        client.replaceFA(1002, FADataType.PROFILES.ordinal(), FAMethodSamples.FA_ONE_PROFILE);
        //! [replacefaoneprofile]

        //! [replacefatwoprofiles]
        client.replaceFA(1003, FADataType.PROFILES.ordinal(), FAMethodSamples.FA_TWO_PROFILES);
        //! [replacefatwoprofiles]

        //! [reqSoftDollarTiers]
        client.reqSoftDollarTiers(4001);
        //! [reqSoftDollarTiers]
    }

    private static void testDisplayGroups(EClientSocket client) throws InterruptedException {

        //! [querydisplaygroups]
        client.queryDisplayGroups(9001);
        //! [querydisplaygroups]

        Thread.sleep(500);

        //! [subscribetogroupevents]
        client.subscribeToGroupEvents(9002, 1);
        //! [subscribetogroupevents]

        Thread.sleep(500);

        //! [updatedisplaygroup]
        client.updateDisplayGroup(9002, "8314@SMART");
        //! [updatedisplaygroup]

        Thread.sleep(500);

        //! [subscribefromgroupevents]
        client.unsubscribeFromGroupEvents(9002);
        //! [subscribefromgroupevents]

    }

    private static void marketDataType(EClientSocket client) {

        //! [reqmarketdatatype]
        /*** Switch to live (1) frozen (2) delayed (3) or delayed frozen (4)***/
        client.reqMarketDataType(2);
        //! [reqmarketdatatype]

    }

    private static void optionsOperations(EClientSocket client) {

        //! [reqsecdefoptparams]
        client.reqSecDefOptParams(0, "IBM", "", "STK", 8314);
        //! [reqsecdefoptparams]

        //! [calculateimpliedvolatility]
        client.calculateImpliedVolatility(5001, ContractSamples.OptionWithLocalSymbol(), 0.6, 55, null);
        //! [calculateimpliedvolatility]

        //** Canceling implied volatility ***
        client.cancelCalculateImpliedVolatility(5001);

        //! [calculateoptionprice]
        client.calculateOptionPrice(5002, ContractSamples.OptionWithLocalSymbol(), 0.5, 55, null);
        //! [calculateoptionprice]

        //** Canceling option's price calculation ***
        client.cancelCalculateOptionPrice(5002);

        //! [exercise_options]
        //** Exercising options ***
        client.exerciseOptions(5003, ContractSamples.OptionWithTradingClass(), 1, 1, "", 1);
        //! [exercise_options]
    }

    private static void rerouteCFDOperations(EClientSocket client) throws InterruptedException {

        //! [reqmktdatacfd]
        client.reqMktData(16001, ContractSamples.USStockCFD(), "", false, false, null);
        Thread.sleep(1000);
        client.reqMktData(16002, ContractSamples.EuropeanStockCFD(), "", false, false, null);
        Thread.sleep(1000);
        client.reqMktData(16003, ContractSamples.CashCFD(), "", false, false, null);
        Thread.sleep(1000);
        //! [reqmktdatacfd]

        //! [reqmktdepthcfd]
        client.reqMktDepth(16004, ContractSamples.USStockCFD(), 10, false, null);
        Thread.sleep(1000);
        client.reqMktDepth(16005, ContractSamples.EuropeanStockCFD(), 10, false, null);
        Thread.sleep(1000);
        client.reqMktDepth(16006, ContractSamples.CashCFD(), 10, false, null);
        Thread.sleep(1000);
        //! [reqmktdepthcfd]
    }

    private static void marketRuleOperations(EClientSocket client) throws InterruptedException {
        client.reqContractDetails(17001, ContractSamples.USStock());
        client.reqContractDetails(17002, ContractSamples.Bond());

        Thread.sleep(2000);

        //! [reqmarketrule]
        client.reqMarketRule(26);
        client.reqMarketRule(240);
        //! [reqmarketrule]
    }

    private static void continuousFuturesOperations(EClientSocket client) throws InterruptedException {

        /*** Requesting continuous futures contract details ***/
        client.reqContractDetails(18001, ContractSamples.ContFut());

        /*** Requesting historical data for continuous futures ***/
        //! [reqhistoricaldatacontfut]
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat form = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
        String formatted = form.format(cal.getTime());
        client.reqHistoricalData(18002, ContractSamples.ContFut(), formatted, "1 Y", "1 month", "TRADES", 0, 1, false, null);
        Thread.sleep(10000);
        /*** Canceling historical data request for continuous futures ***/
        client.cancelHistoricalData(18002);
        //! [reqhistoricaldatacontfut]
    }

    private static void tickByTickOperations(EClientSocket client) throws InterruptedException {

        /*** Requesting tick-by-tick data (only refresh) ***/
        //! [reqtickbytick]
        client.reqTickByTickData(19001, ContractSamples.USStockAtSmart(), "Last", 0, false);
        client.reqTickByTickData(19002, ContractSamples.USStockAtSmart(), "AllLast", 0, false);
        client.reqTickByTickData(19003, ContractSamples.USStockAtSmart(), "BidAsk", 0, true);
        client.reqTickByTickData(19004, ContractSamples.EurGbpFx(), "MidPoint", 0, false);
        //! [reqtickbytick]

        Thread.sleep(10000);

        //! [canceltickbytick]
        client.cancelTickByTickData(19001);
        client.cancelTickByTickData(19002);
        client.cancelTickByTickData(19003);
        client.cancelTickByTickData(19004);
        //! [canceltickbytick]

        /*** Requesting tick-by-tick data (refresh + historical ticks) ***/
        //! [reqtickbytick]
        client.reqTickByTickData(19005, ContractSamples.EuropeanStock(), "Last", 10, false);
        client.reqTickByTickData(19006, ContractSamples.EuropeanStock(), "AllLast", 10, false);
        client.reqTickByTickData(19007, ContractSamples.EuropeanStock(), "BidAsk", 10, false);
        client.reqTickByTickData(19008, ContractSamples.EurGbpFx(), "MidPoint", 10, true);
        //! [reqtickbytick]

        Thread.sleep(10000);

        //! [canceltickbytick]
        client.cancelTickByTickData(19005);
        client.cancelTickByTickData(19006);
        client.cancelTickByTickData(19007);
        client.cancelTickByTickData(19008);
        //! [canceltickbytick]
    }

    private static void whatIfSamples(EClientSocket client, int nextOrderId) throws InterruptedException {

        /*** Placing what-if order ***/
        //! [whatiforder]
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), OrderSamples.WhatIfLimitOrder("BUY", Decimal.get(200), 120));
        //! [whatiforder]
    }

    private static void ibkratsSample(EClientSocket client, int nextOrderId) {

        //! [ibkratssubmit]
        Order ibkratsOrder = OrderSamples.LimitIBKRATS("BUY", Decimal.ONE_HUNDRED, 330);
        client.placeOrder(nextOrderId++, ContractSamples.IBKRATSContract(), ibkratsOrder);
        //! [ibkratssubmit]

    }
}
