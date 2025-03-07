/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.samples.testbed.contracts;

import java.util.ArrayList;
import java.util.List;

import com.ib.client.ComboLeg;
import com.ib.client.Contract;

public class ContractSamples {

    public static Contract USStockWithPrimaryExch() {
        //! [stkcontractwithprimary]
        Contract contract = new Contract();
        contract.symbol("SPY");
        contract.secType("STK");
        contract.currency("USD");
        contract.exchange("SMART");
        contract.primaryExch("ARCA");
        //! [stkcontractwithprimary]
        return contract;
    }

    public static Contract BondWithCusip() {
        //! [bondwithcusip]
        Contract contract = new Contract();
        // enter CUSIP as symbol
        contract.symbol("912828C57");
        contract.secType("BOND");
        contract.exchange("SMART");
        contract.currency("USD");
        //! [bondwithcusip]
        return contract;
    }

    public static Contract Bond() {
        //! [bond]
        Contract contract = new Contract();
        contract.conid(107179906);
        contract.exchange("SMART");
        //! [bond]
        return contract;
    }

    public static Contract MutualFund() {
        //! [fundcontract]
        Contract contract = new Contract();
        contract.symbol("VINIX");
        contract.secType("FUND");
        contract.exchange("FUNDSERV");
        contract.currency("USD");
        //! [fundcontract]
        return contract;
    }

    public static Contract Commodity() {
        //! [commoditycontract]
        Contract contract = new Contract();
        contract.symbol("XAUUSD");
        contract.secType("CMDTY");
        contract.exchange("SMART");
        contract.currency("USD");
        //! [commoditycontract]
        return contract;
    }


    public static Contract EurGbpFx() {
        //! [cashcontract]
        Contract contract = new Contract();
        contract.symbol("EUR");
        contract.secType("CASH");
        contract.currency("GBP");
        contract.exchange("IDEALPRO");
        //! [cashcontract]
        return contract;
    }

    public static Contract Index() {
        //! [indcontract]
        Contract contract = new Contract();
        contract.symbol("DAX");
        contract.secType("IND");
        contract.currency("EUR");
        contract.exchange("EUREX");
        //! [indcontract]
        return contract;
    }

    public static Contract CFD() {
        //! [cfdcontract]
        Contract contract = new Contract();
        contract.symbol("IBDE30");
        contract.secType("CFD");
        contract.currency("EUR");
        contract.exchange("SMART");
        //! [cfdcontract]
        return contract;
    }

    public static Contract USStockCFD() {
        //! [usstockcfd]
        Contract contract = new Contract();
        contract.symbol("IBM");
        contract.secType("CFD");
        contract.currency("USD");
        contract.exchange("SMART");
        //! [usstockcfd]
        return contract;
    }

    public static Contract EuropeanStockCFD() {
        //! [europeanstockcfd]
        Contract contract = new Contract();
        contract.symbol("BMW");
        contract.secType("CFD");
        contract.currency("EUR");
        contract.exchange("SMART");
        //! [europeanstockcfd]
        return contract;
    }

    public static Contract CashCFD() {
        //! [cashcfd]
        Contract contract = new Contract();
        contract.symbol("EUR");
        contract.secType("CFD");
        contract.currency("USD");
        contract.exchange("SMART");
        //! [cashcfd]
        return contract;
    }

    public static Contract EuropeanStock() {
        Contract contract = new Contract();
        contract.symbol("NOKIA");
        contract.secType("STK");
        contract.currency("EUR");
        contract.exchange("SMART");
        contract.primaryExch("HEX");
        return contract;
    }

    public static Contract OptionAtIse() {
        Contract contract = new Contract();
        contract.symbol("BPX");
        contract.secType("OPT");
        contract.currency("USD");
        contract.exchange("ISE");
        contract.lastTradeDateOrContractMonth("20160916");
        contract.right("C");
        contract.strike(65);
        contract.multiplier("100");
        return contract;
    }

    public static Contract USStock() {
        //! [stkcontract]
        Contract contract = new Contract();
        contract.symbol("SPY");
        contract.secType("STK");
        contract.currency("USD");
        contract.exchange("ARCA");
        //! [stkcontract]
        return contract;
    }

    public static Contract USStockAtSmart() {
        Contract contract = new Contract();
        contract.symbol("IBM");
        contract.secType("STK");
        contract.currency("USD");
        contract.exchange("SMART");
        return contract;
    }

    public static Contract etf() {
        Contract contract = new Contract();
        contract.symbol("QQQ");
        contract.secType("STK");
        contract.currency("USD");
        contract.exchange("SMART");
        return contract;
    }

    public static Contract USOptionContract() {
        Contract contract = new Contract();
        contract.symbol("GOOG");
        contract.secType("OPT");
        contract.currency("USD");
        contract.exchange("SMART");
        contract.lastTradeDateOrContractMonth("20170120");
        contract.strike(615);
        contract.right("C");
        contract.multiplier("100");
        return contract;
    }

    public static Contract OptionAtBOX() {
        //! [optcontract]
        Contract contract = new Contract();
        contract.symbol("GOOG");
        contract.secType("OPT");
        contract.currency("USD");
        contract.exchange("BOX");
        contract.lastTradeDateOrContractMonth("20170120");
        contract.right("C");
        contract.strike(615);
        contract.multiplier("100");
        //! [optcontract]
        return contract;
    }

    public static Contract OptionWithTradingClass() {
        //! [optcontract_tradingclass]
        Contract contract = new Contract();
        contract.symbol("SANT");
        contract.secType("OPT");
        contract.currency("EUR");
        contract.exchange("MEFFRV");
        contract.lastTradeDateOrContractMonth("20190621");
        contract.right("C");
        contract.strike(7.5);
        contract.multiplier("100");
        contract.tradingClass("SANEU");
        //! [optcontract_tradingclass]
        return contract;
    }

    public static Contract OptionWithLocalSymbol() {
        //! [optcontract_localsymbol]
        Contract contract = new Contract();
        //Watch out for the spaces within the local symbol!
        contract.localSymbol("P BMW  20221216 72 M");
        contract.secType("OPT");
        contract.exchange("EUREX");
        contract.currency("EUR");
        //! [optcontract_localsymbol]
        return contract;
    }

    public static Contract DutchWarrant() {
        //! [ioptcontract]
        Contract contract = new Contract();
        contract.localSymbol("B881G");
        contract.secType("IOPT");
        contract.exchange("SBF");
        contract.currency("EUR");
        //! [ioptcontract]
        return contract;
    }

    public static Contract SimpleFuture() {
        //! [futcontract]
        Contract contract = new Contract();
        contract.symbol("GBL");
        contract.secType("FUT");
        contract.currency("EUR");
        contract.exchange("EUREX");
        contract.lastTradeDateOrContractMonth("202303");
        //! [futcontract]
        return contract;
    }

    public static Contract FutureWithLocalSymbol() {
        //! [futcontract_local_symbol]
        Contract contract = new Contract();
        contract.localSymbol("FGBL MAR 23");
        contract.secType("FUT");
        contract.currency("EUR");
        contract.exchange("EUREX");
        //! [futcontract_local_symbol]
        return contract;
    }

    public static Contract FutureWithMultiplier() {
        //! [futcontract_multiplier]
        Contract contract = new Contract();
        contract.symbol("DAX");
        contract.secType("FUT");
        contract.currency("EUR");
        contract.exchange("EUREX");
        contract.lastTradeDateOrContractMonth("202303");
        contract.multiplier("1");
        //! [futcontract_multiplier]
        return contract;
    }

    public static Contract WrongContract() {
        Contract contract = new Contract();
        contract.localSymbol(" IJR ");
        contract.conid(9579976);
        contract.secType("STK");
        contract.currency("USD");
        contract.exchange("SMART");
        return contract;
    }

    public static Contract FuturesOnOptions() {
        //! [fopcontract]
        Contract contract = new Contract();
        contract.symbol("GBL");
        contract.secType("FOP");
        contract.currency("EUR");
        contract.exchange("EUREX");
        contract.lastTradeDateOrContractMonth("20230224");
        contract.right("C");
        contract.strike(138);
        contract.multiplier("1000");
        //! [fopcontract]
        return contract;
    }

    public static Contract Warrants() {
        //! [warcontract]
        Contract contract = new Contract();
        contract.symbol("GOOG");
        contract.secType("WAR");
        contract.currency("EUR");
        contract.exchange("FWB");
        contract.lastTradeDateOrContractMonth("20201117");
        contract.right("C");
        contract.strike(1500.0);
        contract.multiplier("0.01");
        //! [warcontract]
        return contract;
    }

    public static Contract ByISIN() {
        Contract contract = new Contract();
        contract.secIdType("ISIN");
        contract.secId("US45841N1072");
        contract.secType("STK");
        contract.currency("USD");
        contract.exchange("SMART");
        return contract;
    }

    public static Contract ByConId() {
        Contract contract = new Contract();
        contract.conid(12087792);
        contract.secType("CASH");
        contract.exchange("IDEALPRO");
        return contract;
    }

    public static Contract OptionForQuery() {
        //! [optionforquery]
        Contract contract = new Contract();
        contract.symbol("FISV");
        contract.secType("OPT");
        contract.currency("USD");
        contract.exchange("SMART");
        //! [optionforquery]
        return contract;
    }

    public static Contract OptionComboContract() {
        //! [bagoptcontract]
        Contract contract = new Contract();
        contract.symbol("DBK");
        contract.secType("BAG");
        contract.currency("EUR");
        contract.exchange("EUREX");

        ComboLeg leg1 = new ComboLeg();
        ComboLeg leg2 = new ComboLeg();

        List<ComboLeg> addAllLegs = new ArrayList<>();

        leg1.conid(577164786);//DBK Jun21'24 2 CALL @EUREX
        leg1.ratio(1);
        leg1.action("BUY");
        leg1.exchange("EUREX");

        leg2.conid(577164767);//DBK Dec15'23 2 CALL @EUREX
        leg2.ratio(1);
        leg2.action("SELL");
        leg2.exchange("EUREX");

        addAllLegs.add(leg1);
        addAllLegs.add(leg2);

        contract.comboLegs(addAllLegs);
        //! [bagoptcontract]

        return contract;
    }

    public static Contract StockComboContract() {
        //! [bagstkcontract]
        Contract contract = new Contract();
        contract.symbol("MCD");
        contract.secType("BAG");
        contract.currency("USD");
        contract.exchange("SMART");

        ComboLeg leg1 = new ComboLeg();
        ComboLeg leg2 = new ComboLeg();

        List<ComboLeg> addAllLegs = new ArrayList<>();

        leg1.conid(43645865);//IBKR STK
        leg1.ratio(1);
        leg1.action("BUY");
        leg1.exchange("SMART");

        leg2.conid(9408);//MCD STK
        leg2.ratio(1);
        leg2.action("SELL");
        leg2.exchange("SMART");

        addAllLegs.add(leg1);
        addAllLegs.add(leg2);

        contract.comboLegs(addAllLegs);
        //! [bagstkcontract]

        return contract;
    }

    public static Contract FutureComboContract() {
        //! [bagfutcontract]
        Contract contract = new Contract();
        contract.symbol("VIX");
        contract.secType("BAG");
        contract.currency("USD");
        contract.exchange("CFE");

        ComboLeg leg1 = new ComboLeg();
        ComboLeg leg2 = new ComboLeg();

        List<ComboLeg> addAllLegs = new ArrayList<>();

        leg1.conid(195538625);//VIX FUT 20160217
        leg1.ratio(1);
        leg1.action("BUY");
        leg1.exchange("CFE");

        leg2.conid(197436571);//VIX FUT 20160316
        leg2.ratio(1);
        leg2.action("SELL");
        leg2.exchange("CFE");

        addAllLegs.add(leg1);
        addAllLegs.add(leg2);

        contract.comboLegs(addAllLegs);
        //! [bagfutcontract]

        return contract;
    }

    public static Contract SmartFutureComboContract() {
        //! [smartfuturespread]
        Contract contract = new Contract();
        contract.symbol("WTI");  // WTI,COIL spread. Symbol can be defined as first leg symbol ("WTI") or currency ("USD").
        contract.secType("BAG");
        contract.currency("USD");
        contract.exchange("SMART"); // smart-routed rather than direct routed

        ComboLeg leg1 = new ComboLeg();
        ComboLeg leg2 = new ComboLeg();

        List<ComboLeg> addAllLegs = new ArrayList<>();

        leg1.conid(55928698);// WTI future June 2017
        leg1.ratio(1);
        leg1.action("BUY");
        leg1.exchange("IPE");

        leg2.conid(55850663);// COIL future June 2017
        leg2.ratio(1);
        leg2.action("SELL");
        leg2.exchange("IPE");

        addAllLegs.add(leg1);
        addAllLegs.add(leg2);

        contract.comboLegs(addAllLegs);
        //! [smartfuturespread]

        return contract;
    }

    public static Contract InterCmdtyFuturesContract() {
        //! [intcmdfutcontract]
        Contract contract = new Contract();
        contract.symbol("COIL.WTI");
        contract.secType("BAG");
        contract.currency("USD");
        contract.exchange("IPE");

        ComboLeg leg1 = new ComboLeg();
        ComboLeg leg2 = new ComboLeg();

        List<ComboLeg> addAllLegs = new ArrayList<>();

        leg1.conid(183405603); //WTI�Dec'23�@IPE
        leg1.ratio(1);
        leg1.action("BUY");
        leg1.exchange("IPE");

        leg2.conid(254011009); //COIL�Dec'23�@IPE
        leg2.ratio(1);
        leg2.action("SELL");
        leg2.exchange("IPE");

        addAllLegs.add(leg1);
        addAllLegs.add(leg2);

        contract.comboLegs(addAllLegs);
        //! [intcmdfutcontract]

        return contract;
    }

    public static Contract NewsFeedForQuery() {
        //! [newsfeedforquery]
        Contract contract = new Contract();
        contract.secType("NEWS");
        contract.exchange("BRF"); //Briefing Trader
        //! [newsfeedforquery]
        return contract;
    }

    public static Contract BTbroadtapeNewsFeed() {
        //! [newscontractbt]
        Contract contract = new Contract();
        contract.symbol("BRF:BRF_ALL"); //BroadTape All News
        contract.secType("NEWS");
        contract.exchange("BRF"); //Briefing Trader
        //! [newscontractbt]
        return contract;
    }

    public static Contract BZbroadtapeNewsFeed() {
        //! [newscontractbz]
        Contract contract = new Contract();
        contract.symbol("BZ:BZ_ALL"); //BroadTape All News
        contract.secType("NEWS");
        contract.exchange("BZ"); //Benzinga Pro
        //! [newscontractbz]
        return contract;
    }

    public static Contract FLYbroadtapeNewsFeed() {
        //! [newscontractfly]
        Contract contract = new Contract();
        contract.symbol("FLY:FLY_ALL"); //BroadTape All News
        contract.secType("NEWS");
        contract.exchange("FLY"); //Fly on the Wall
        //! [newscontractfly]
        return contract;
    }

    public static Contract ContFut() {
        //! [continuousfuturescontract]
        Contract contract = new Contract();
        contract.symbol("GBL");
        contract.secType("CONTFUT");
        contract.exchange("EUREX");
        //! [continuousfuturescontract]
        return contract;
    }

    public static Contract ContAndExpiringFut() {
        //! [contandexpiringfut]
        Contract contract = new Contract();
        contract.symbol("GBL");
        contract.secType("FUT+CONTFUT");
        contract.exchange("EUREX");
        //! [contandexpiringfut]
        return contract;
    }

    public static Contract JefferiesContract() {
        //! [jefferies_contract]
        Contract contract = new Contract();
        contract.symbol("AAPL");
        contract.secType("STK");
        contract.exchange("JEFFALGO"); // must be direct-routed to JEFFALGO
        contract.currency("USD");    // only available for US stocks
        //! [jefferies_contract]
        return contract;
    }

    public static Contract CSFBContract() {
        //! [csfb_contract]
        Contract contract = new Contract();
        contract.symbol("IBKR");
        contract.secType("STK");
        contract.exchange("CSFBALGO"); // must be direct-routed to CSFBALGO
        contract.currency("USD");    // only available for US stocks
        //! [csfb_contract]
        return contract;
    }

    public static Contract QBAlgoContract() {
        //! [qbalgo_contract]
        Contract contract = new Contract();
        contract.symbol("ES");
        contract.secType("FUT");
        contract.exchange("QBALGO");
        contract.currency("USD");
        contract.lastTradeDateOrContractMonth("202003");
        //! [qbalgo_contract]
        return contract;
    }

    public static Contract IBKRATSContract() {
        //! [ibkrats_contract]
        Contract contract = new Contract();
        contract.symbol("SPY");
        contract.secType("STK");
        contract.exchange("IBKRATS");
        contract.currency("USD");
        //! [ibkrats_contract]
        return contract;
    }

    public static Contract CryptoContract() {
        //! [crypto_contract]
        Contract contract = new Contract();
        contract.symbol("ETH");
        contract.secType("CRYPTO");
        contract.exchange("PAXOS");
        contract.currency("USD");
        //! [crypto_contract]
        return contract;
    }

    public static Contract StockWithIPOPrice() {
        //! [stock_with_IPO_price]
        Contract contract = new Contract();
        contract.symbol("EMCGU");
        contract.secType("STK");
        contract.currency("USD");
        contract.exchange("SMART");
        //! [stock_with_IPO_price]
        return contract;
    }

    public static Contract ByFIGI() {
        //! [ByFIGI]
        Contract contract = new Contract();
        contract.secIdType("FIGI");
        contract.secId("BBG000B9XRY4");
        contract.exchange("SMART");
        //! [ByFIGI]
        return contract;
    }

    public static Contract ByIssuerId() {
        //! [ByIssuerId]
        Contract contract = new Contract();
        contract.issuerId("e1453318");
        //! [ByIssuerId]
        return contract;
    }


}