package com.lambda.investing.interactive_brokers;

import com.ib.client.Decimal;
import com.ib.client.TickAttribBidAsk;
import com.ib.client.TickAttribLast;
import com.lambda.investing.interactive_brokers.listener.IBListener;
import com.lambda.investing.model.Market;
import com.lambda.investing.model.asset.Currency;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import org.junit.*;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;


@Ignore("need to open interactive brokers")
@RunWith(MockitoJUnitRunner.class)
public class InteractiveBrokersBrokerConnectorTest extends IBListener {

    private static InteractiveBrokersBrokerConnector instance;
    private boolean isConnected = false;
    private Depth lastDepth = null;
    private Trade lastTrade = null;

    @Override
    public void connectAck() {
        System.out.println("IB connected");
    }

    @Override
    public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side, double price, Decimal size, boolean isSmartDepth) {
        System.out.println("updateMktDepthL2 test: " + tickerId + " " + position + " " + marketMaker + " " + operation + " " + side + " " + price + " " + size + " " + isSmartDepth);
    }

    @Override
    public void updateMktDepth(int tickerId, int position, int operation, int side, double price, Decimal size) {
        System.out.println("updateMktDepth test : " + tickerId + " " + position + " " + operation + " " + side + " " + price + " " + size);
    }


    @Override
    public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice, Decimal bidSize, Decimal askSize, TickAttribBidAsk tickAttribBidAsk) {
        Depth depth = new Depth();
        depth.setAsks(new Double[]{askPrice});
        depth.setBids(new Double[]{bidPrice});
        depth.setAsksQuantities(new Double[]{askSize.value().doubleValue()});
        depth.setBidsQuantities(new Double[]{bidSize.value().doubleValue()});
        depth.setTimestamp(time * 1000);
        depth.setLevels(1);
        depth.setInstrument(instance.getInstrumentPk(reqId));
        lastDepth = depth;
    }

    @Override
    public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size, TickAttribLast tickAttribLast, String exchange, String specialConditions) {
        Trade trade = new Trade();
        trade.setPrice(price);
        trade.setQuantity(size.value().doubleValue());
        trade.setTimestamp(time * 1000);
        trade.setInstrument(instance.getInstrumentPk(reqId));
        lastTrade = trade;

    }


    void getInstance() {
        instance = InteractiveBrokersBrokerConnector.getInstance("localhost", 7497, 555);
        instance.register(this);
    }

    @Before
    public void setUp() throws Exception {
        if (!isConnected) {
            init();

        }
    }

    @Test
    public void init() {
        getInstance();
        instance.init();
        Assert.assertTrue(instance.isConnected());
        isConnected = true;
        instance.register(this);
    }

    @Test
    public void subscribeMarketData() throws Exception {
        Instrument instrument = new Instrument();
        instrument.setPrimaryKey("btcusd_paxos");
        instrument.setSymbol("BTC");
        instrument.setMarket(Market.Paxos.name().toLowerCase());
        instrument.setCurrency(Currency.USD);
        instrument.setPriceTick(0.01);
        instrument.addMap();


        lastDepth = null;
        lastTrade = null;
        instance.subscribeMarketData(instrument.getPrimaryKey());
//        Assert.assertTrue(instance.getSubscriptionsTickTickerId().size() > 0);
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