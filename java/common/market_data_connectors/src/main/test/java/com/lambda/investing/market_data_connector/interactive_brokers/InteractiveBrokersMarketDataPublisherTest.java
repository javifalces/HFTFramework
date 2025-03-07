package com.lambda.investing.market_data_connector.interactive_brokers;


import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorPublisherProvider;
import com.lambda.investing.interactive_brokers.InteractiveBrokersBrokerConnector;
import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.market_data_connector.ordinary.OrdinaryMarketDataProvider;
import com.lambda.investing.model.Market;
import com.lambda.investing.model.asset.Currency;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.messaging.TypeMessage;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;


import static com.lambda.investing.model.Util.fromJsonString;
import static org.junit.jupiter.api.Assertions.*;

@Ignore("need to open interactive brokers")
@RunWith(MockitoJUnitRunner.class)
public class InteractiveBrokersMarketDataPublisherTest {
    private static InteractiveBrokersBrokerConnector interactiveBrokersBrokerConnector;
    private boolean isConnected = false;
    private Depth lastDepth = null;
    private Trade lastTrade = null;
    private InteractiveBrokersMarketDataPublisher interactiveBrokersMarketDataPublisher;

    private ConnectorListener marketDataListener;
    private OrdinaryConnectorPublisherProvider connector;


    private class MockMarketDataListener implements ConnectorListener {


        @Override
        public void onUpdate(ConnectorConfiguration configuration, long timestampReceived, TypeMessage typeMessage, String content) {
            if (typeMessage == TypeMessage.depth) {
                Depth depth = fromJsonString(content, Depth.class);
                lastDepth = depth;
            } else if (typeMessage == TypeMessage.trade) {
                Trade trade = fromJsonString(content, Trade.class);
                lastTrade = trade;
            }
        }
    }

    public InteractiveBrokersMarketDataPublisherTest() {
        this.interactiveBrokersBrokerConnector = InteractiveBrokersBrokerConnector.getInstance("localhost", 7497, 555);
        List<Instrument> instrumentList = new ArrayList<>();
        Instrument instrument = new Instrument();
        instrument.setPrimaryKey("btcusd_paxos");
        instrument.setSymbol("BTC");
        instrument.setMarket(Market.Paxos.name().toLowerCase());
        instrument.setCurrency(Currency.USD);
        instrument.setPriceTick(0.01);
        instrument.addMap();
        instrumentList.add(instrument);
        //configure connector local
        ConnectorConfiguration connectorConfiguration = new OrdinaryConnectorConfiguration();
        connector = new OrdinaryConnectorPublisherProvider("test", 0, Thread.NORM_PRIORITY);

        //publisher
        this.interactiveBrokersMarketDataPublisher = new InteractiveBrokersMarketDataPublisher(connectorConfiguration, connector, interactiveBrokersBrokerConnector, instrumentList);
        this.interactiveBrokersMarketDataPublisher.init();

        //provider
        //configure listener market data
        OrdinaryMarketDataProvider ordinaryMarketDataProvider = new OrdinaryMarketDataProvider(connector, (OrdinaryConnectorConfiguration) connectorConfiguration);
        marketDataListener = new MockMarketDataListener();
        connector.register(connectorConfiguration, marketDataListener);//subscribe to connector , not engine


    }

    @Test
    public void testUpdateMktDepth() throws InterruptedException {
        lastDepth = null;
        lastTrade = null;

        this.interactiveBrokersMarketDataPublisher.subscribeMarketData();


        while (lastDepth == null) {
            Thread.sleep(1000);
        }
        Assert.assertTrue(lastDepth != null);

        while (lastTrade == null) {
            Thread.sleep(1000);
        }
        Assert.assertTrue(lastTrade != null);
    }
}