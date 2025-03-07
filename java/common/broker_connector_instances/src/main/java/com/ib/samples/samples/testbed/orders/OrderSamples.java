/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.samples.testbed.orders;

import java.util.ArrayList;
import java.util.List;

import com.ib.client.VolumeCondition;
import com.ib.client.Types.TimeInForce;
import com.ib.client.TimeCondition;
import com.ib.client.PercentChangeCondition;
import com.ib.client.MarginCondition;
import com.ib.client.Decimal;
import com.ib.client.ExecutionCondition;
import com.ib.client.PriceCondition;
import com.ib.client.Order;
import com.ib.client.OrderComboLeg;
import com.ib.client.OrderCondition;
import com.ib.client.OrderConditionType;
import com.ib.client.OrderType;
import com.ib.client.TagValue;

public class OrderSamples {

    public static Order AtAuction(String action, Decimal quantity, double price) {
        //! [auction]
        Order order = new Order();
        order.action(action);
        order.tif("AUC");
        order.orderType("MTL");
        order.totalQuantity(quantity);
        order.lmtPrice(price);
        //! [auction]
        return order;
    }

    public static Order Discretionary(String action, Decimal quantity, double price, double discretionaryAmt) {
        //! [discretionary]
        Order order = new Order();
        order.action(action);
        order.orderType("LMT");
        order.totalQuantity(quantity);
        order.lmtPrice(price);
        order.discretionaryAmt(discretionaryAmt);
        //! [discretionary]
        return order;
    }

    public static Order MarketOrder(String action, Decimal quantity) {
        //! [market]
        Order order = new Order();
        order.action(action);
        order.orderType("MKT");
        order.totalQuantity(quantity);
        //! [market]
        return order;
    }

    public static Order MarketIfTouched(String action, Decimal quantity, double price) {
        //! [market_if_touched]
        Order order = new Order();
        order.action(action);
        order.orderType("MIT");
        order.totalQuantity(quantity);
        order.auxPrice(price);
        //! [market_if_touched]
        return order;
    }

    public static Order MarketOnClose(String action, Decimal quantity) {
        //! [market_on_close]
        Order order = new Order();
        order.action(action);
        order.orderType("MOC");
        order.totalQuantity(quantity);
        //! [market_on_close]
        return order;
    }

    public static Order MarketOnOpen(String action, Decimal quantity) {
        //! [market_on_open]
        Order order = new Order();
        order.action(action);
        order.orderType("MKT");
        order.totalQuantity(quantity);
        order.tif("OPG");
        //! [market_on_open]
        return order;
    }

    public static Order MidpointMatch(String action, Decimal quantity) {
        //! [midpoint_match]
        Order order = new Order();
        order.action(action);
        order.orderType("MKT");
        order.totalQuantity(quantity);
        //! [midpoint_match]
        return order;
    }

    public static Order Midprice(String action, Decimal quantity, double priceCap) {
        //! [midprice]
        Order order = new Order();
        order.action(action);
        order.orderType("MIDPRICE");
        order.totalQuantity(quantity);
        order.lmtPrice(priceCap); // optional
        //! [midprice]
        return order;
    }

    public static Order PeggedToMarket(String action, Decimal quantity, double marketOffset) {
        //! [pegged_market]
        Order order = new Order();
        order.action(action);
        order.orderType("PEG MKT");
        order.totalQuantity(Decimal.ONE_HUNDRED);
        order.auxPrice(marketOffset);//Offset price
        //! [pegged_market]
        return order;
    }

    public static Order PeggedToStock(String action, Decimal quantity, double delta, double stockReferencePrice, double startingPrice) {
        //! [pegged_stock]
        Order order = new Order();
        order.action(action);
        order.orderType("PEG STK");
        order.totalQuantity(quantity);
        order.delta(delta);
        order.stockRefPrice(stockReferencePrice);
        order.startingPrice(startingPrice);
        //! [pegged_stock]
        return order;
    }

    public static Order RelativePeggedToPrimary(String action, Decimal quantity, double priceCap, double offsetAmount) {
        //! [relative_pegged_primary]
        Order order = new Order();
        order.action(action);
        order.orderType("REL");
        order.totalQuantity(quantity);
        order.lmtPrice(priceCap);
        order.auxPrice(offsetAmount);
        //! [relative_pegged_primary]
        return order;
    }

    public static Order SweepToFill(String action, Decimal quantity, double price) {
        //! [sweep_to_fill]
        Order order = new Order();
        order.action(action);
        order.orderType("LMT");
        order.totalQuantity(quantity);
        order.lmtPrice(price);
        order.sweepToFill(true);
        //! [sweep_to_fill]
        return order;
    }

    public static Order AuctionLimit(String action, Decimal quantity, double price, int auctionStrategy) {
        //! [auction_limit]
        Order order = new Order();
        order.action(action);
        order.orderType("LMT");
        order.totalQuantity(quantity);
        order.lmtPrice(price);
        order.auctionStrategy(auctionStrategy);
        //! [auction_limit]
        return order;
    }

    public static Order AuctionPeggedToStock(String action, Decimal quantity, double startingPrice, double delta) {
        //! [auction_pegged_stock]
        Order order = new Order();
        order.action(action);
        order.orderType("PEG STK");
        order.totalQuantity(quantity);
        order.delta(delta);
        order.startingPrice(startingPrice);
        //! [auction_pegged_stock]
        return order;
    }

    public static Order AuctionRelative(String action, Decimal quantity, double offset) {
        //! [auction_relative]
        Order order = new Order();
        order.action(action);
        order.orderType("REL");
        order.totalQuantity(quantity);
        order.auxPrice(offset);
        //! [auction_relative]
        return order;
    }

    public static Order Block(String action, Decimal quantity, double price) {
        // ! [block]
        Order order = new Order();
        order.action(action);
        order.orderType("LMT");
        order.totalQuantity(quantity);//Large volumes!
        order.lmtPrice(price);
        order.blockOrder(true);
        // ! [block]
        return order;
    }

    public static Order BoxTop(String action, Decimal quantity) {
        // ! [boxtop]
        Order order = new Order();
        order.action(action);
        order.orderType("BOX TOP");
        order.totalQuantity(quantity);
        // ! [boxtop]
        return order;
    }

    public static Order LimitOrder(String action, Decimal quantity, double limitPrice) {
        // ! [limitorder]
        Order order = new Order();
        order.action(action);
        order.orderType("LMT");
        order.totalQuantity(quantity);
        order.lmtPrice(limitPrice);
        order.tif(TimeInForce.DAY);
        // ! [limitorder]
        return order;
    }

    public static Order LimitOrderWithManualOrderTime(String action, Decimal quantity, double limitPrice, String manualOrderTime) {
        // ! [limitorderwithmanualordertime]
        Order order = OrderSamples.LimitOrder(action, quantity, limitPrice);
        order.manualOrderTime(manualOrderTime);
        // ! [limitorderwithmanualordertime]
        return order;
    }

    // Forex orders can be placed in denomination of second currency in pair using cashQty field
    // Requires TWS or IBG 963+
    // https://www.interactivebrokers.com/en/index.php?f=23876#963-02

    public static Order LimitOrderWithCashQty(String action, double limitPrice, double cashQty) {
        // ! [limitorderwithcashqty]
        Order order = new Order();
        order.action(action);
        order.orderType("LMT");
        order.lmtPrice(limitPrice);
        order.cashQty(cashQty);
        // ! [limitorderwithcashqty]
        return order;
    }


    public static Order LimitIfTouched(String action, Decimal quantity, double limitPrice, double triggerPrice) {
        // ! [limitiftouched]
        Order order = new Order();
        order.action(action);
        order.orderType("LIT");
        order.totalQuantity(quantity);
        order.lmtPrice(limitPrice);
        order.auxPrice(triggerPrice);
        // ! [limitiftouched]
        return order;
    }

    public static Order LimitOnClose(String action, Decimal quantity, double limitPrice) {
        // ! [limitonclose]
        Order order = new Order();
        order.action(action);
        order.orderType("LOC");
        order.totalQuantity(quantity);
        order.lmtPrice(limitPrice);
        // ! [limitonclose]
        return order;
    }

    public static Order LimitOnOpen(String action, Decimal quantity, double limitPrice) {
        // ! [limitonopen]
        Order order = new Order();
        order.action(action);
        order.tif("OPG");
        order.orderType("LOC");
        order.totalQuantity(quantity);
        order.lmtPrice(limitPrice);
        // ! [limitonopen]
        return order;
    }

    public static Order PassiveRelative(String action, Decimal quantity, double offset) {
        // ! [passive_relative]
        Order order = new Order();
        order.action(action);
        order.orderType("PASSV REL");
        order.totalQuantity(quantity);
        order.auxPrice(offset);
        // ! [passive_relative]
        return order;
    }

    public static Order PeggedToMidpoint(String action, Decimal quantity, double offset, double limitPrice) {
        // ! [pegged_midpoint]
        Order order = new Order();
        order.action(action);
        order.orderType("PEG MID");
        order.totalQuantity(quantity);
        order.auxPrice(offset);
        order.lmtPrice(limitPrice);
        // ! [pegged_midpoint]
        return order;
    }

    //! [bracket]
    public static List<Order> BracketOrder(int parentOrderId, String action, Decimal quantity, double limitPrice, double takeProfitLimitPrice, double stopLossPrice) {
        //This will be our main or "parent" order
        Order parent = new Order();
        parent.orderId(parentOrderId);
        parent.action(action);
        parent.orderType("LMT");
        parent.totalQuantity(quantity);
        parent.lmtPrice(limitPrice);
        //The parent and children orders will need this attribute set to false to prevent accidental executions.
        //The LAST CHILD will have it set to true.
        parent.transmit(false);

        Order takeProfit = new Order();
        takeProfit.orderId(parent.orderId() + 1);
        takeProfit.action(action.equals("BUY") ? "SELL" : "BUY");
        takeProfit.orderType("LMT");
        takeProfit.totalQuantity(quantity);
        takeProfit.lmtPrice(takeProfitLimitPrice);
        takeProfit.parentId(parentOrderId);
        takeProfit.transmit(false);

        Order stopLoss = new Order();
        stopLoss.orderId(parent.orderId() + 2);
        stopLoss.action(action.equals("BUY") ? "SELL" : "BUY");
        stopLoss.orderType("STP");
        //Stop trigger price
        stopLoss.auxPrice(stopLossPrice);
        stopLoss.totalQuantity(quantity);
        stopLoss.parentId(parentOrderId);
        //In this case, the low side order will be the last child being sent. Therefore, it needs to set this attribute to true
        //to activate all its predecessors
        stopLoss.transmit(true);

        List<Order> bracketOrder = new ArrayList<>();
        bracketOrder.add(parent);
        bracketOrder.add(takeProfit);
        bracketOrder.add(stopLoss);

        return bracketOrder;
    }
    //! [bracket]

    public static Order MarketToLimit(String action, Decimal quantity) {
        // ! [markettolimit]
        Order order = new Order();
        order.action(action);
        order.orderType("MTL");
        order.totalQuantity(quantity);
        // ! [markettolimit]
        return order;
    }

    public static Order MarketWithProtection(String action, Decimal quantity) {
        // ! [marketwithprotection]
        Order order = new Order();
        order.action(action);
        order.orderType("MKT PRT");
        order.totalQuantity(quantity);
        // ! [marketwithprotection]
        return order;
    }

    public static Order Stop(String action, Decimal quantity, double stopPrice) {
        // ! [stop]
        Order order = new Order();
        order.action(action);
        order.orderType("STP");
        order.auxPrice(stopPrice);
        order.totalQuantity(quantity);
        // ! [stop]
        return order;
    }

    public static Order StopLimit(String action, Decimal quantity, double limitPrice, double stopPrice) {
        // ! [stoplimit]
        Order order = new Order();
        order.action(action);
        order.orderType("STP LMT");
        order.lmtPrice(limitPrice);
        order.auxPrice(stopPrice);
        order.totalQuantity(quantity);
        // ! [stoplimit]
        return order;
    }

    public static Order StopWithProtection(String action, Decimal quantity, double stopPrice) {
        // ! [stopwithprotection]
        Order order = new Order();
        order.action(action);
        order.orderType("STP PRT");
        order.auxPrice(stopPrice);
        order.totalQuantity(quantity);
        // ! [stopwithprotection]
        return order;
    }

    public static Order TrailingStop(String action, Decimal quantity, double trailingPercent, double trailStopPrice) {
        // ! [trailingstop]
        Order order = new Order();
        order.action(action);
        order.orderType("TRAIL");
        order.trailingPercent(trailingPercent);
        order.trailStopPrice(trailStopPrice);
        order.totalQuantity(quantity);
        // ! [trailingstop]
        return order;
    }

    public static Order TrailingStopLimit(String action, Decimal quantity, double lmtPriceOffset, double trailingAmount, double trailStopPrice) {
        // ! [trailingstoplimit]
        Order order = new Order();
        order.action(action);
        order.orderType("TRAIL LIMIT");
        order.lmtPriceOffset(lmtPriceOffset);
        order.auxPrice(trailingAmount);
        order.trailStopPrice(trailStopPrice);
        order.totalQuantity(quantity);
        // ! [trailingstoplimit]
        return order;
    }

    public static Order ComboLimitOrder(String action, Decimal quantity, boolean nonGuaranteed, double limitPrice) {
        // ! [combolimit]
        Order order = new Order();
        order.action(action);
        order.orderType("LMT");
        order.lmtPrice(limitPrice);
        order.totalQuantity(quantity);
        if (nonGuaranteed) {
            order.smartComboRoutingParams().add(new TagValue("NonGuaranteed", "1"));
        }
        // ! [combolimit]
        return order;
    }

    public static Order ComboMarketOrder(String action, Decimal quantity, boolean nonGuaranteed) {
        // ! [combomarket]
        Order order = new Order();
        order.action(action);
        order.orderType("MKT");
        order.totalQuantity(quantity);
        if (nonGuaranteed) {
            order.smartComboRoutingParams().add(new TagValue("NonGuaranteed", "1"));
        }
        // ! [combomarket]
        return order;
    }

    public static Order LimitOrderForComboWithLegPrices(String action, Decimal quantity, boolean nonGuaranteed, double[] legPrices) {
        // ! [limitordercombolegprices]
        Order order = new Order();
        order.action(action);
        order.orderType("LMT");
        order.totalQuantity(quantity);
        order.orderComboLegs(new ArrayList<>());

        for (double price : legPrices) {
            OrderComboLeg comboLeg = new OrderComboLeg();
            comboLeg.price(5.0);
            order.orderComboLegs().add(comboLeg);
        }

        if (nonGuaranteed) {
            order.smartComboRoutingParams().add(new TagValue("NonGuaranteed", "1"));
        }
        // ! [limitordercombolegprices]
        return order;
    }

    public static Order RelativeLimitCombo(String action, Decimal quantity, boolean nonGuaranteed, double limitPrice) {
        // ! [relativelimitcombo]
        Order order = new Order();
        order.action(action);
        order.orderType("REL + LMT");
        order.totalQuantity(quantity);
        order.lmtPrice(limitPrice);

        if (nonGuaranteed) {
            order.smartComboRoutingParams().add(new TagValue("NonGuaranteed", "1"));
        }
        // ! [relativelimitcombo]
        return order;
    }

    public static Order RelativeMarketCombo(String action, Decimal quantity, boolean nonGuaranteed) {
        // ! [relativemarketcombo]
        Order order = new Order();
        order.action(action);
        order.orderType("REL + MKT");
        order.totalQuantity(quantity);
        if (nonGuaranteed) {
            order.smartComboRoutingParams().add(new TagValue("NonGuaranteed", "1"));
        }
        // ! [relativemarketcombo]
        return order;
    }

    // ! [oca]
    public static List<Order> OneCancelsAll(String ocaGroup, List<Order> ocaOrders, int ocaType) {

        for (Order o : ocaOrders) {
            o.ocaGroup(ocaGroup);
            o.ocaType(ocaType);
        }
        return ocaOrders;
    }
    // ! [oca]

    public static Order Volatility(String action, Decimal quantity, double volatilityPercent, int volatilityType) {
        // ! [volatility]
        Order order = new Order();
        order.action(action);
        order.orderType("VOL");
        order.volatility(volatilityPercent);//Expressed in percentage (40%)
        order.volatilityType(volatilityType);// 1=daily, 2=annual
        order.totalQuantity(quantity);
        // ! [volatility]
        return order;
    }

    //! [fhedge]
    public static Order MarketFHedge(int parentOrderId, String action) {
        //FX Hedge orders can only have a quantity of 0
        Order order = MarketOrder(action, Decimal.ZERO);
        order.parentId(parentOrderId);
        order.hedgeType("F");
        return order;
    }
    //! [fhedge]

    public static Order PeggedToBenchmark(String action, Decimal quantity, double startingPrice, boolean peggedChangeAmountDecrease, double peggedChangeAmount, double referenceChangeAmount, int referenceConId, String referenceExchange, double stockReferencePrice,
                                          double referenceContractLowerRange, double referenceContractUpperRange) {
        //! [pegged_benchmark]
        Order order = new Order();
        order.orderType("PEG BENCH");
        //BUY or SELL
        order.action(action);
        order.totalQuantity(quantity);
        //Beginning with price...
        order.startingPrice(startingPrice);
        //increase/decrease price...
        order.isPeggedChangeAmountDecrease(peggedChangeAmountDecrease);
        //by... (and likewise for price moving in opposite direction)
        order.peggedChangeAmount(peggedChangeAmount);
        //whenever there is a price change of...
        order.referenceChangeAmount(referenceChangeAmount);
        //in the reference contract...
        order.referenceContractId(referenceConId);
        //being traded at...
        order.referenceExchangeId(referenceExchange);
        //starting reference price is...
        order.stockRefPrice(stockReferencePrice);
        //Keep order active as long as reference contract trades between...
        order.stockRangeLower(referenceContractLowerRange);
        //and...
        order.stockRangeUpper(referenceContractUpperRange);
        //! [pegged_benchmark]
        return order;
    }

    public static Order AttachAdjustableToStop(Order parent, double attachedOrderStopPrice, double triggerPrice, double adjustStopPrice) {
        //! [adjustable_stop]
        Order order = new Order();
        //Attached order is a conventional STP order in opposite direction
        order.action("BUY".equals(parent.getAction()) ? "SELL" : "BUY");
        order.totalQuantity(parent.totalQuantity());
        order.auxPrice(attachedOrderStopPrice);
        order.parentId(parent.orderId());
        //When trigger price is penetrated
        order.triggerPrice(triggerPrice);
        //The parent order will be turned into a STP order
        order.adjustedOrderType(OrderType.STP);
        //With the given STP price
        order.adjustedStopPrice(adjustStopPrice);
        //! [adjustable_stop]
        return order;
    }

    public static Order AttachAdjustableToStopLimit(Order parent, double attachedOrderStopPrice, double triggerPrice, double adjustStopPrice, double adjustedStopLimitPrice) {
        //! [adjustable_stop_limit]
        Order order = new Order();
        //Attached order is a conventional STP order
        order.action("BUY".equals(parent.getAction()) ? "SELL" : "BUY");
        order.totalQuantity(parent.totalQuantity());
        order.auxPrice(attachedOrderStopPrice);
        order.parentId(parent.orderId());
        //When trigger price is penetrated
        order.triggerPrice(triggerPrice);
        //The parent order will be turned into a STP LMT order
        order.adjustedOrderType(OrderType.STP_LMT);
        //With the given stop price
        order.adjustedStopPrice(adjustStopPrice);
        //And the given limit price
        order.adjustedStopLimitPrice(adjustedStopLimitPrice);
        //! [adjustable_stop_limit]
        return order;
    }

    public static Order AttachAdjustableToTrail(Order parent, double attachedOrderStopPrice, double triggerPrice, double adjustStopPrice, double adjustedTrailAmount, int trailUnit) {
        //! [adjustable_trail]
        Order order = new Order();
        //Attached order is a conventional STP order
        order.action("BUY".equals(parent.getAction()) ? "SELL" : "BUY");
        order.totalQuantity(parent.totalQuantity());
        order.auxPrice(attachedOrderStopPrice);
        order.parentId(parent.orderId());
        //When trigger price is penetrated
        order.triggerPrice(triggerPrice);
        //The parent order will be turned into a TRAIL order
        order.adjustedOrderType(OrderType.TRAIL);
        //With a stop price of...
        order.adjustedStopPrice(adjustStopPrice);
        //trailing by and amount (0) or a percent (100)...
        order.adjustableTrailingUnit(trailUnit);
        //of...
        order.adjustedTrailingAmount(adjustedTrailAmount);
        //! [adjustable_trail]
        return order;
    }

    public static PriceCondition PriceCondition(int conId, String exchange, double price, boolean isMore, boolean isConjunction) {
        //! [price_condition]
        //Conditions have to be created via the OrderCondition.Create
        PriceCondition priceCondition = (PriceCondition) OrderCondition.create(OrderConditionType.Price);
        //When this contract...
        priceCondition.conId(conId);
        //traded on this exchange
        priceCondition.exchange(exchange);
        //has a price above/below
        priceCondition.isMore(isMore);
        //this quantity
        priceCondition.price(price);
        //AND | OR next condition (will be ignored if no more conditions are added)
        priceCondition.conjunctionConnection(isConjunction);
        //! [price_condition]
        return priceCondition;
    }

    public static ExecutionCondition ExecutionCondition(String symbol, String secType, String exchange, boolean isConjunction) {
        //! [execution_condition]
        ExecutionCondition execCondition = (ExecutionCondition) OrderCondition.create(OrderConditionType.Execution);
        //When an execution on symbol
        execCondition.symbol(symbol);
        //at exchange
        execCondition.exchange(exchange);
        //for this secType
        execCondition.secType(secType);
        //AND | OR next condition (will be ignored if no more conditions are added)
        execCondition.conjunctionConnection(isConjunction);
        //! [execution_condition]
        return execCondition;
    }

    public static MarginCondition MarginCondition(int percent, boolean isMore, boolean isConjunction) {
        //! [margin_condition]
        MarginCondition marginCondition = (MarginCondition) OrderCondition.create(OrderConditionType.Margin);
        //If margin is above/below
        marginCondition.isMore(isMore);
        //given percent
        marginCondition.percent(percent);
        //AND | OR next condition (will be ignored if no more conditions are added)
        marginCondition.conjunctionConnection(isConjunction);
        //! [margin_condition]
        return marginCondition;
    }

    public static PercentChangeCondition PercentageChangeCondition(double pctChange, int conId, String exchange, boolean isMore, boolean isConjunction) {
        //! [percentage_condition]
        PercentChangeCondition pctChangeCondition = (PercentChangeCondition) OrderCondition.create(OrderConditionType.PercentChange);
        //If there is a price percent change measured against last close price above or below...
        pctChangeCondition.isMore(isMore);
        //this amount...
        pctChangeCondition.changePercent(pctChange);
        //on this contract
        pctChangeCondition.conId(conId);
        //when traded on this exchange...
        pctChangeCondition.exchange(exchange);
        //AND | OR next condition (will be ignored if no more conditions are added)
        pctChangeCondition.conjunctionConnection(isConjunction);
        //! [percentage_condition]
        return pctChangeCondition;
    }

    public static TimeCondition TimeCondition(String time, boolean isMore, boolean isConjunction) {
        //! [time_condition]
        TimeCondition timeCondition = (TimeCondition) OrderCondition.create(OrderConditionType.Time);
        //Before or after...
        timeCondition.isMore(isMore);
        //this time...
        timeCondition.time(time);
        //AND | OR next condition (will be ignored if no more conditions are added)
        timeCondition.conjunctionConnection(isConjunction);
        //! [time_condition]
        return timeCondition;
    }

    public static VolumeCondition VolumeCondition(int conId, String exchange, boolean isMore, int volume, boolean isConjunction) {
        //! [volume_condition]
        VolumeCondition volCon = (VolumeCondition) OrderCondition.create(OrderConditionType.Volume);
        //Whenever contract...
        volCon.conId(conId);
        //When traded at
        volCon.exchange(exchange);
        //reaches a volume higher/lower
        volCon.isMore(isMore);
        //than this...
        volCon.volume(volume);
        //AND | OR next condition (will be ignored if no more conditions are added)
        volCon.conjunctionConnection(isConjunction);
        //! [volume_condition]
        return volCon;
    }

    public static Order WhatIfLimitOrder(String action, Decimal quantity, double limitPrice) {
        // ! [whatiflimitorder]
        Order order = LimitOrder(action, quantity, limitPrice);
        order.whatIf(true);
        // ! [whatiflimitorder]
        return order;
    }

    public static Order LimitIBKRATS(String action, Decimal quantity, double limitPrice) {
        // ! [limit_ibkrats]
        Order order = new Order();
        order.action(action);
        order.orderType("LMT");
        order.lmtPrice(limitPrice);
        order.totalQuantity(quantity);
        order.notHeld(true);
        // ! [limit_ibkrats]
        return order;
    }

    public static Order PegBestUpToMidOrder(String action, Decimal quantity, double limitPrice, int minTradeQty,
                                            int minCompeteSize, double midOffsetAtWhole, double midOffsetAtHalf) {
        // ! [peg_best_up_to_mid_order]
        Order order = new Order();
        order.action(action);
        order.orderType("PEG BEST");
        order.lmtPrice(limitPrice);
        order.totalQuantity(quantity);
        order.notHeld(true);
        order.minTradeQty(minTradeQty);
        order.minCompeteSize(minCompeteSize);
        order.competeAgainstBestOffset(Order.COMPETE_AGAINST_BEST_OFFSET_UP_TO_MID);
        order.midOffsetAtWhole(midOffsetAtWhole);
        order.midOffsetAtHalf(midOffsetAtHalf);
        // ! [peg_best_up_to_mid_order]
        return order;
    }

    public static Order PegBestOrder(String action, Decimal quantity, double limitPrice, int minTradeQty,
                                     int minCompeteSize, double competeAgainstBestOffset) {
        // ! [peg_best_order]
        Order order = new Order();
        order.action(action);
        order.orderType("PEG BEST");
        order.lmtPrice(limitPrice);
        order.totalQuantity(quantity);
        order.notHeld(true);
        order.minTradeQty(minTradeQty);
        order.minCompeteSize(minCompeteSize);
        order.competeAgainstBestOffset(competeAgainstBestOffset);
        // ! [peg_best_order]
        return order;
    }

    public static Order PegMidOrder(String action, Decimal quantity, double limitPrice, int minTradeQty,
                                    double midOffsetAtWhole, double midOffsetAtHalf) {
        // ! [peg_mid_order]
        Order order = new Order();
        order.action(action);
        order.orderType("PEG MID");
        order.lmtPrice(limitPrice);
        order.totalQuantity(quantity);
        order.notHeld(true);
        order.minTradeQty(minTradeQty);
        order.midOffsetAtWhole(midOffsetAtWhole);
        order.midOffsetAtHalf(midOffsetAtHalf);
        // ! [peg_mid_order]
        return order;
    }

}