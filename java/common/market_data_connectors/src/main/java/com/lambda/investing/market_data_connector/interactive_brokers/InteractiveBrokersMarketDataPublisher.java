package com.lambda.investing.market_data_connector.interactive_brokers;

import com.ib.client.*;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.interactive_brokers.InteractiveBrokersBrokerConnector;
import com.lambda.investing.market_data_connector.AbstractMarketDataConnectorPublisher;
import com.lambda.investing.market_data_connector.Statistics;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;

import java.util.List;

public class InteractiveBrokersMarketDataPublisher extends AbstractMarketDataConnectorPublisher {

    private InteractiveBrokersBrokerConnector interactiveBrokersBrokerConnector;
    private EWrapperListener eWrapperListener;
    private List<Instrument> instrumentList;

    /**
     * https://interactivebrokers.github.io/tws-api/classIBApi_1_1EClient.html#a3ab310450f1261accd706f69766b2263
     *
     * @param connectorConfiguration
     * @param connectorPublisher
     * @param interactiveBrokersBrokerConnector
     */
    public InteractiveBrokersMarketDataPublisher(ConnectorConfiguration connectorConfiguration,
                                                 ConnectorPublisher connectorPublisher, InteractiveBrokersBrokerConnector interactiveBrokersBrokerConnector,
                                                 List<Instrument> instrumentList
    ) {
        super(connectorConfiguration, connectorPublisher);
        this.interactiveBrokersBrokerConnector = interactiveBrokersBrokerConnector;
        this.instrumentList = instrumentList;
        this.statistics = new Statistics("InteractiveBrokersMarketDataPublisher", 60000);//useful to see if we are receiving data

    }

    @Override
    public void init() {
        this.interactiveBrokersBrokerConnector.init();
        this.eWrapperListener = new EWrapperListener(this);
        this.interactiveBrokersBrokerConnector.register(this.eWrapperListener);
        subscribeMarketData();
    }

    //register to instrument and subscribe to market data
    public void subscribeMarketData() {
        for (Instrument instrument : this.instrumentList) {
            try {
                interactiveBrokersBrokerConnector.subscribeMarketData(instrument.getPrimaryKey());
            } catch (Exception e) {
                logger.error("Error subscribing to market data for instrument: " + instrument.getPrimaryKey(), e);
            }
        }
    }


    public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size, TickAttribLast tickAttribLast, String exchange, String specialConditions) {
        Trade trade = new Trade();
        trade.setPrice(price);
        trade.setQuantity(size.value().doubleValue());
        trade.setTimestamp(time * 1000);
        String instrumentPK = interactiveBrokersBrokerConnector.getInstrumentPk(reqId);
        trade.setInstrument(instrumentPK);
        notifyTrade(instrumentPK, trade);

    }


    public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice, Decimal bidSize, Decimal askSize, TickAttribBidAsk tickAttribBidAsk) {
        Depth depth = new Depth();
        depth.setAsks(new Double[]{askPrice});
        depth.setBids(new Double[]{bidPrice});
        depth.setAsksQuantities(new Double[]{askSize.value().doubleValue()});
        depth.setBidsQuantities(new Double[]{bidSize.value().doubleValue()});
        depth.setTimestamp(time * 1000);

        String instrumentPK = interactiveBrokersBrokerConnector.getInstrumentPk(reqId);
        depth.setInstrument(instrumentPK);
        depth.setLevelsFromData();
        notifyDepth(instrumentPK, depth);
    }

    public void updateMktDepth(int tickerId, int position, String marketMaker, int operation, int side, double price, Decimal size) {
        //Cost money : L2 data set InteractiveBrokersBrokerConnector.subscribeAllDepth to true
        return;
    }

    public void tickByTickMidPoint(int reqId, long time, double midPoint) {
        //not required
        return;
    }

    public void realtimeBar(int reqId, long time, double open, double high, double low, double close, Decimal volume, Decimal wap, int count) {
        //not required
        return;
    }

    public void updatePortfolio(Contract contract, Decimal position, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
        //not required
        return;
    }

    public void historicalData(int reqId, Bar bar) {
        //not required
        return;
    }

}



