package com.lambda.investing.market_data_connector.interactive_brokers;

import com.ib.client.*;
import com.lambda.investing.interactive_brokers.listener.IBListener;


public class EWrapperListener extends IBListener {
    InteractiveBrokersMarketDataPublisher publisher;

    public EWrapperListener(InteractiveBrokersMarketDataPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void updateMktDepth(int tickerId, int position, int operation, int side, double price, Decimal size) {
        publisher.updateMktDepth(tickerId, position, "", operation, side, price, size);
    }


    @Override
    public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side, double price, Decimal size, boolean isSmartDepth) {
        publisher.updateMktDepth(tickerId, position, marketMaker, operation, side, price, size);
    }


    @Override
    public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size, TickAttribLast tickAttribLast, String exchange, String specialConditions) {
        publisher.tickByTickAllLast(reqId, tickType, time, price, size, tickAttribLast, exchange, specialConditions);
    }

    @Override
    public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice, Decimal bidSize, Decimal askSize, TickAttribBidAsk tickAttribBidAsk) {
        publisher.tickByTickBidAsk(reqId, time, bidPrice, askPrice, bidSize, askSize, tickAttribBidAsk);
    }

    @Override
    public void tickByTickMidPoint(int reqId, long time, double midPoint) {
        publisher.tickByTickMidPoint(reqId, time, midPoint);
    }

    @Override
    public void realtimeBar(int reqId, long time, double open, double high, double low, double close, Decimal volume, Decimal wap, int count) {
        publisher.realtimeBar(reqId, time, open, high, low, close, volume, wap, count);
    }

    @Override
    public void historicalData(int reqId, Bar bar) {
        publisher.historicalData(reqId, bar);
    }

    @Override
    public void updatePortfolio(Contract contract, Decimal position, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
        publisher.updatePortfolio(contract, position, marketPrice, marketValue, averageCost, unrealizedPNL, realizedPNL, accountName);
    }


}
