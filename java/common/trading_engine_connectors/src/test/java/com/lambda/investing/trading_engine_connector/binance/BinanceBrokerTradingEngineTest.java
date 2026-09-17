package com.lambda.investing.trading_engine_connector.binance;

import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.lambda.investing.Configuration;
import com.lambda.investing.binance.BinanceBrokerConnector;
import com.lambda.investing.connector.AbstractConnectorPublisherProvider;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorPublisherProviderFactory;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static com.lambda.investing.connector.ConnectorPublisherProviderFactory.DEFAULT_PRIORITY;
import static com.lambda.investing.model.Util.fromObject;
import static org.junit.Assert.assertEquals;

@Ignore("Needs valid Binance API credentials to run")
@RunWith(MockitoJUnitRunner.class)
public class BinanceBrokerTradingEngineTest {

    @BeforeClass
    public static void setupLogging() {
        Configurator.setRootLevel(Level.INFO);
        Configurator.setLevel("com.lambda.investing", Level.INFO);
        Configurator.setLevel("com.lambda.investing.trading_engine_connector.binance", Level.INFO);
        System.out.println("Logger configured to INFO level with console output");
    }

    private static String API_KEY = "TODO set a valid one";
    private static String SECRET_KEY = "TODO set a valid one";
    private static String SYMBOL = "btceur";

    private static BinanceBrokerConnector brokerConnector;
    private BinanceBrokerTradingEngine tradingEngine;

    private ConnectorListener executionReportListener;
    private AbstractConnectorPublisherProvider connector;

    private String algorithmName = "junitAlgorithm_BinanceBrokerTradingEngineTest";
    private List<ExecutionReport> lastExecutionReport = new ArrayList<>();
    private Instrument instrument;
    double lastBid;
    double lastAsk;
    double amount = 0.001;

    private class MockExecutionReportListener implements ConnectorListener {
        @Override
        public void onUpdate(ConnectorConfiguration configuration, long timestampReceived, TypeMessage typeMessage, Object content) {
            if (typeMessage == TypeMessage.execution_report) {
                ExecutionReport executionReport = fromObject(content, ExecutionReport.class);
                lastExecutionReport.add(executionReport);
            }
        }
    }

    public BinanceBrokerTradingEngineTest() {
        instrument = new Instrument();
        instrument.setSymbol(SYMBOL);
        instrument.setMarket("binance");
        instrument.setPriceTick(0.01);
        instrument.setQuantityTick(0.00001);
        instrument.addMap();

        this.brokerConnector = BinanceBrokerConnector.getInstance(API_KEY, SECRET_KEY);

        OrderBook orderBook = this.brokerConnector.getRestClient().getOrderBook(SYMBOL.toUpperCase(), 5);
        List<OrderBookEntry> bids = orderBook.getBids();
        List<OrderBookEntry> asks = orderBook.getAsks();
        lastBid = bids.isEmpty() ? 0.0 : Double.parseDouble(bids.get(0).getPrice());
        lastAsk = asks.isEmpty() ? 0.0 : Double.parseDouble(asks.get(0).getPrice());

        //configure connector local
        ConnectorConfiguration connectorConfiguration = new OrdinaryConnectorConfiguration();
        connector = ConnectorPublisherProviderFactory.createConnectorPublisherProvider(Configuration.BACKTEST_CONNECTOR_PUBLISHER_PROVIDER, "test", 0, DEFAULT_PRIORITY);
        // trading engine
        BinanceTradingEngineConfiguration binanceTradingEngineConfiguration = new BinanceTradingEngineConfiguration(API_KEY, SECRET_KEY);
        this.tradingEngine = new BinanceBrokerTradingEngine(connectorConfiguration, connector, connectorConfiguration, connector, binanceTradingEngineConfiguration);
        this.tradingEngine.start();
        //Execution report listener
        this.executionReportListener = new MockExecutionReportListener();
        connector.register(connectorConfiguration, executionReportListener);//subscribe to connector , not engine
    }

    private List<com.binance.api.client.domain.account.Order> getOpenOrders() {
        com.binance.api.client.domain.account.request.OrderRequest openOrdersRequest =
                new com.binance.api.client.domain.account.request.OrderRequest(SYMBOL.toUpperCase());
        return brokerConnector.getRestClient().getOpenOrders(openOrdersRequest);
    }

    @Test
    public void sendNewLimitTestBuyCancelAll() throws InterruptedException {
        double price = lastBid - instrument.getPriceStep() * 10;//send very low to avoid filling
        double quantity = instrument.roundQty(amount / price);
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

        List<com.binance.api.client.domain.account.Order> openOrders = getOpenOrders();
        assertEquals(1, openOrders.size());

        OrderRequest orderRequestCancel = OrderRequest.createCancel(System.currentTimeMillis(), algorithmName, instrument, orderRequest.getClientOrderId());
        lastExecutionReport.clear();
        tradingEngine.orderRequest(orderRequestCancel);
        while (lastExecutionReport.size() == 0) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Cancelled, lastExecutionReport.get(0).getExecutionReportStatus());

        List<com.binance.api.client.domain.account.Order> openOrdersAfterCancel = getOpenOrders();
        assertEquals(0, openOrdersAfterCancel.size());
    }

    @Test
    public void sendNewLimitTestBuy() throws InterruptedException {
        double price = lastBid - instrument.getPriceStep() * 10;//send very low to avoid filling
        double quantity = instrument.roundQty(amount / price);
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

        List<com.binance.api.client.domain.account.Order> openOrders = getOpenOrders();
        assertEquals(1, openOrders.size());

        //replace it
        OrderRequest orderRequestReplace = OrderRequest.modifyOrder(System.currentTimeMillis(), orderRequest.getAlgorithmInfo(), instrument, orderRequest.getVerb(), orderRequest.getQuantity(), price - instrument.getPriceStep(), orderRequest.getClientOrderId());
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
        double price = lastAsk + instrument.getPriceStep() * 10;//send very high to avoid filling
        double quantity = instrument.roundQty(amount / price);
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

    @Test
    public void sendNewLimitFillTest() throws InterruptedException {
        double price = lastBid - instrument.getPriceStep() * 10;//send very low to avoid filling
        double quantity = instrument.roundQty(amount / price);
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
        OrderRequest orderRequestReplace = OrderRequest.modifyOrder(System.currentTimeMillis(), orderRequest.getAlgorithmInfo(), instrument, orderRequest.getVerb(), orderRequest.getQuantity(), lastAsk + instrument.getPriceStep(), orderRequest.getClientOrderId());
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
        OrderRequest orderRequestUndo = OrderRequest.createLimitOrderRequest(System.currentTimeMillis(), algorithmName, instrument, Verb.Sell, quantity, lastBid - instrument.getPriceStep());
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
