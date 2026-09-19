package com.lambda.investing.trading_engine_connector.xchange;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.AbstractConnectorPublisherProvider;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorPublisherProviderFactory;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.xchange.KrakenBrokerConnector;
import com.lambda.investing.xchange.XChangeBrokerConnector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.lambda.investing.connector.ConnectorPublisherProviderFactory.DEFAULT_PRIORITY;
import static com.lambda.investing.model.Util.fromObject;
import static org.junit.Assert.assertEquals;

@Ignore("Needs valid Kraken API credentials to run")
@RunWith(MockitoJUnitRunner.class)
public class KrakenBrokerTradingEngineTest {

    @BeforeClass
    public static void setupLogging() {
        Configurator.setRootLevel(Level.INFO);
        Configurator.setLevel("com.lambda.investing", Level.INFO);
        Configurator.setLevel("com.lambda.investing.trading_engine_connector.xchange", Level.INFO);
        System.out.println("Logger configured to INFO level with console output");
    }

    private static String API_KEY = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    private static String SECRET_KEY = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    private static String SYMBOL = "btceur";

    private static KrakenBrokerConnector brokerConnector;
    private XChangeTradingEngine tradingEngine;

    private ConnectorListener executionReportListener;
    private AbstractConnectorPublisherProvider connector;

    private String algorithmName = "junitAlgorithm_KrakenBrokerTradingEngineTest";
    private List<ExecutionReport> lastExecutionReport = new ArrayList<>();
    private Instrument instrument;
    double lastBid;
    double lastAsk;
    double amount;

    private class MockExecutionReportListener implements ConnectorListener {
        @Override
        public void onUpdate(ConnectorConfiguration configuration, long timestampReceived, TypeMessage typeMessage, Object content) {
            if (typeMessage == TypeMessage.execution_report) {
                ExecutionReport executionReport = fromObject(content, ExecutionReport.class);
                lastExecutionReport.add(executionReport);
            }
        }
    }

    public KrakenBrokerTradingEngineTest() {
        instrument = new Instrument();
        instrument.setSymbol(SYMBOL);
        instrument.setMarket("kraken");
        instrument.setPriceTick(0.1);
        instrument.setQuantityTick(0.00000001);
        instrument.addMap();
        amount = 0.00005;//0.00005 BTC ~ 3.5 EUR now

        this.brokerConnector = KrakenBrokerConnector.getInstance(API_KEY, SECRET_KEY);

        CurrencyPair currencyPair = XChangeBrokerConnector.getCurrencyPair(instrument.getPrimaryKey());
        OrderBook orderBook = null;
        try {
            orderBook = this.brokerConnector.getMarketDataService().getOrderBook(currencyPair);
        } catch (Exception e) {
            throw new RuntimeException("cant get orderbook for " + currencyPair, e);
        }
        List<LimitOrder> bids = orderBook.getBids();
        List<LimitOrder> asks = orderBook.getAsks();
        lastBid = bids.isEmpty() ? 0.0 : bids.get(0).getLimitPrice().doubleValue();
        lastAsk = asks.isEmpty() ? 0.0 : asks.get(0).getLimitPrice().doubleValue();

        //configure connector local
        ConnectorConfiguration connectorConfiguration = new OrdinaryConnectorConfiguration();
        connector = ConnectorPublisherProviderFactory.createConnectorPublisherProvider(Configuration.BACKTEST_CONNECTOR_PUBLISHER_PROVIDER, "test", 0, DEFAULT_PRIORITY);
        // trading engine
        Set<Instrument> instrumentSet = new HashSet<>();
        instrumentSet.add(instrument);
        KrakenTradingEngineConfiguration krakenTradingEngineConfiguration = new KrakenTradingEngineConfiguration(API_KEY, SECRET_KEY);
        this.tradingEngine = new XChangeTradingEngine(connectorConfiguration, connector, connectorConfiguration, connector, krakenTradingEngineConfiguration, instrumentSet);
        this.tradingEngine.start();
        //Execution report listener
        this.executionReportListener = new MockExecutionReportListener();
        connector.register(connectorConfiguration, executionReportListener);//subscribe to connector , not engine
    }

    private List<LimitOrder> getOpenOrders() {
        CurrencyPair currencyPair = XChangeBrokerConnector.getCurrencyPair(instrument.getPrimaryKey());
        try {
            OpenOrders openOrders = brokerConnector.getExchange().getTradeService().getOpenOrders();
            return openOrders.getOpenOrders().stream().filter(order -> order.getCurrencyPair().equals(currencyPair))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("cant get open orders for " + currencyPair, e);
        }
    }

    @Test
    public void sendNewLimitTestBuyCancelAll() throws InterruptedException {
        double price = lastBid - instrument.getPriceStep() * 10;//send very low to avoid filling
        double quantity = amount;
        //new order
        OrderRequest orderRequest = OrderRequest.createLimitOrderRequest(System.currentTimeMillis(), algorithmName, instrument, Verb.Buy, quantity, price);
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest);
        tradingEngine.orderRequest(orderRequest);
        while (lastExecutionReport.size() == 0) {
            Thread.sleep(1000);
        }

        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequest.getQuantity(), lastExecutionReport.get(0).getQuantity(), 1e-6);
        assertEquals(orderRequest.getPrice(), lastExecutionReport.get(0).getPrice(), 1e-6);
        assertEquals(orderRequest.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequest.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        List<LimitOrder> openOrders = getOpenOrders();
        assertEquals(1, openOrders.size());

        OrderRequest orderRequestCancel = OrderRequest.createCancel(System.currentTimeMillis(), algorithmName, instrument, orderRequest.getClientOrderId());
        lastExecutionReport.clear();
        tradingEngine.orderRequest(orderRequestCancel);
        while (lastExecutionReport.size() == 0) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Cancelled, lastExecutionReport.get(0).getExecutionReportStatus());

        //Kraken's open orders REST endpoint can lag briefly behind the websocket CANCELED confirmation
        List<LimitOrder> openOrdersAfterCancel = getOpenOrders();
        int attempts = 0;
        while (!openOrdersAfterCancel.isEmpty() && attempts < 10) {
            Thread.sleep(1000);
            openOrdersAfterCancel = getOpenOrders();
            attempts++;
        }
        assertEquals(0, openOrdersAfterCancel.size());
    }

    @Test
    public void sendNewLimitTestBuy() throws InterruptedException {
        double price = lastBid - instrument.getPriceStep() * 1000;//send very low to avoid filling
        double quantity = amount;
        //new order
        OrderRequest orderRequest = OrderRequest.createLimitOrderRequest(System.currentTimeMillis(), algorithmName, instrument, Verb.Buy, quantity, price);
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest);
        tradingEngine.orderRequest(orderRequest);
        while (lastExecutionReport.size() == 0) {
            Thread.sleep(1000);
        }

        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequest.getQuantity(), lastExecutionReport.get(0).getQuantity(), 1e-6);
        assertEquals(orderRequest.getPrice(), lastExecutionReport.get(0).getPrice(), 1e-6);
        assertEquals(orderRequest.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequest.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        List<LimitOrder> openOrders = getOpenOrders();
        assertEquals(1, openOrders.size());

        //replace it
        OrderRequest orderRequestReplace = OrderRequest.modifyOrder(System.currentTimeMillis(), orderRequest.getAlgorithmInfo(), instrument, orderRequest.getVerb(), orderRequest.getQuantity(), price - 10 * instrument.getPriceStep(), orderRequest.getClientOrderId());
        lastExecutionReport.clear();
        System.out.println("orderRequestReplace: " + orderRequestReplace);
        tradingEngine.orderRequest(orderRequestReplace);
        while (lastExecutionReport.size() == 0) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequestReplace.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequestReplace.getOrigClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());

        assertEquals(orderRequestReplace.getQuantity(), lastExecutionReport.get(0).getQuantity(), 1e-6);
        assertEquals(orderRequestReplace.getPrice(), lastExecutionReport.get(0).getPrice(), 1e-6);
        assertEquals(orderRequestReplace.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequestReplace.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequestReplace.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        //cancel it
        OrderRequest orderRequestCancel = OrderRequest.createCancel(System.currentTimeMillis(), orderRequest.getAlgorithmInfo(), instrument, orderRequestReplace.getClientOrderId());
        lastExecutionReport.clear();
        System.out.println("orderRequestCancel: " + orderRequestCancel);
        tradingEngine.orderRequest(orderRequestCancel);
        while (lastExecutionReport.size() == 0) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Cancelled, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequestCancel.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequestCancel.getOrigClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());
        assertEquals(orderRequestReplace.getClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());

        assertEquals(orderRequestCancel.getQuantity(), lastExecutionReport.get(0).getQuantity(), 1e-6);
        assertEquals(orderRequestCancel.getPrice(), lastExecutionReport.get(0).getPrice(), 1e-6);
        assertEquals(orderRequestCancel.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequestCancel.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequestCancel.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());
    }

    @Test
    public void sendNewLimitTestSell() throws InterruptedException {
        double price = lastAsk + instrument.getPriceStep() * 1000;//send very high to avoid filling
        double quantity = amount;
        //new order
        OrderRequest orderRequest = OrderRequest.createLimitOrderRequest(System.currentTimeMillis(), algorithmName, instrument, Verb.Sell, quantity, price);
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest);
        tradingEngine.orderRequest(orderRequest);
        while (lastExecutionReport.size() == 0) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequest.getQuantity(), lastExecutionReport.get(0).getQuantity(), 1e-6);
        assertEquals(orderRequest.getPrice(), lastExecutionReport.get(0).getPrice(), 1e-6);
        assertEquals(orderRequest.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequest.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        //replace it
        OrderRequest orderRequestReplace = OrderRequest.modifyOrder(System.currentTimeMillis(), orderRequest.getAlgorithmInfo(), instrument, orderRequest.getVerb(), orderRequest.getQuantity(), price + instrument.getPriceStep(), orderRequest.getClientOrderId());
        lastExecutionReport.clear();
        System.out.println("orderRequestReplace: " + orderRequestReplace);
        tradingEngine.orderRequest(orderRequestReplace);
        while (lastExecutionReport.size() == 0) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequestReplace.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequestReplace.getOrigClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());

        assertEquals(orderRequestReplace.getQuantity(), lastExecutionReport.get(0).getQuantity(), 1e-6);
        assertEquals(orderRequestReplace.getPrice(), lastExecutionReport.get(0).getPrice(), 1e-6);
        assertEquals(orderRequestReplace.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequestReplace.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequestReplace.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        //cancel it
        OrderRequest orderRequestCancel = OrderRequest.createCancel(System.currentTimeMillis(), orderRequest.getAlgorithmInfo(), instrument, orderRequestReplace.getClientOrderId());
        lastExecutionReport.clear();
        System.out.println("orderRequestCancel: " + orderRequestCancel);
        tradingEngine.orderRequest(orderRequestCancel);
        while (lastExecutionReport.size() == 0) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Cancelled, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequestCancel.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequestCancel.getOrigClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());
        assertEquals(orderRequestReplace.getClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());

        assertEquals(orderRequestCancel.getQuantity(), lastExecutionReport.get(0).getQuantity(), 1e-6);
        assertEquals(orderRequestCancel.getPrice(), lastExecutionReport.get(0).getPrice(), 1e-6);
        assertEquals(orderRequestCancel.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequestCancel.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequestCancel.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());
    }

    @Ignore("This test is making trades")
    @Test
    public void sendNewLimitFillTest() throws InterruptedException {
        double price = lastBid - instrument.getPriceStep() * 1000;//send very low to avoid filling
        double quantity = amount;
        //new order
        OrderRequest orderRequest = OrderRequest.createLimitOrderRequest(System.currentTimeMillis(), algorithmName, instrument, Verb.Buy, quantity, price);
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest);
        tradingEngine.orderRequest(orderRequest);
        while (lastExecutionReport.size() == 0) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());

        //replace it to cross the spread and fill
        OrderRequest orderRequestReplace = OrderRequest.modifyOrder(System.currentTimeMillis(), orderRequest.getAlgorithmInfo(), instrument, orderRequest.getVerb(), orderRequest.getQuantity(), lastAsk + instrument.getPriceStep() * 5, orderRequest.getClientOrderId());
        lastExecutionReport.clear();
        System.out.println("orderRequestReplace to fill: " + orderRequestReplace);
        tradingEngine.orderRequest(orderRequestReplace);
        //waiting for both active and fill
        while (lastExecutionReport.size() < 2) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequestReplace.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequestReplace.getOrigClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());

        assertEquals(ExecutionReportStatus.CompletelyFilled, lastExecutionReport.get(1).getExecutionReportStatus());
        assertEquals(orderRequestReplace.getClientOrderId(), lastExecutionReport.get(1).getClientOrderId());

        //undo it: send sell at bid to flatten the position again
        OrderRequest orderRequestUndo = OrderRequest.createLimitOrderRequest(System.currentTimeMillis(), algorithmName, instrument, Verb.Sell, quantity, lastBid - instrument.getPriceStep() * 5);
        lastExecutionReport.clear();
        System.out.println("orderRequestUndo to fill: " + orderRequestUndo);
        tradingEngine.orderRequest(orderRequestUndo);
        //waiting for both active and fill
        while (lastExecutionReport.size() < 2) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequestUndo.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());

        assertEquals(ExecutionReportStatus.CompletelyFilled, lastExecutionReport.get(1).getExecutionReportStatus());
        assertEquals(orderRequestUndo.getClientOrderId(), lastExecutionReport.get(1).getClientOrderId());
    }
}
