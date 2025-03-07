package com.lambda.investing.trading_engine_connector.interactive_brokers;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorPublisherProvider;
import com.lambda.investing.interactive_brokers.InteractiveBrokersBrokerConnector;
import com.lambda.investing.model.Market;
import com.lambda.investing.model.asset.Currency;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.*;
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
public class InteractiveBrokersTradingEngineTest {
    private static InteractiveBrokersBrokerConnector interactiveBrokersBrokerConnector;
    private boolean isConnected = false;
    private InteractiveBrokersTradingEngine interactiveBrokersTradingEngine;

    private ConnectorListener executionReportListener;
    private OrdinaryConnectorPublisherProvider connector;
    private String algorithmName = "junitAlgorithm_InteractiveBrokersTradingEngineTest";
    private List<ExecutionReport> lastExecutionReport = new ArrayList<>();
    private Instrument cryptoInstrument;
    private Instrument stkInstrument;
    private Instrument fxInstrument;

    private class MockExecutionReportListener implements ConnectorListener {
        @Override
        public void onUpdate(ConnectorConfiguration configuration, long timestampReceived, TypeMessage typeMessage, String content) {
            if (typeMessage == TypeMessage.execution_report) {
                ExecutionReport executionReport = fromJsonString(content, ExecutionReport.class);
                lastExecutionReport.add(executionReport);
            }
        }
    }

    public InteractiveBrokersTradingEngineTest() {
        this.interactiveBrokersBrokerConnector = InteractiveBrokersBrokerConnector.getInstance("localhost", 7497, 555);
        //configure connector local
        ConnectorConfiguration connectorConfiguration = new OrdinaryConnectorConfiguration();
        connector = new OrdinaryConnectorPublisherProvider("test", 0, Thread.NORM_PRIORITY);
        // trading engine
        this.interactiveBrokersTradingEngine = new InteractiveBrokersTradingEngine(connectorConfiguration, connector, connectorConfiguration, connector, interactiveBrokersBrokerConnector);
        this.interactiveBrokersTradingEngine.start();
        //Execution report listener
        this.executionReportListener = new MockExecutionReportListener();
        connector.register(connectorConfiguration, executionReportListener);//subscribe to connector , not engine

        //create instrument
        cryptoInstrument = new Instrument();
        cryptoInstrument.setPrimaryKey("btcusd_paxos");
        cryptoInstrument.setSymbol("BTC");
        cryptoInstrument.setMarket(Market.Paxos.name().toLowerCase());
        cryptoInstrument.setCurrency(Currency.USD);
        cryptoInstrument.setPriceTick(0.01);
        cryptoInstrument.addMap();

        stkInstrument = new Instrument();
        stkInstrument.setPrimaryKey("aapl_smart");
        stkInstrument.setSymbol("AAPL");
        stkInstrument.setMarket(Market.Smart.name().toLowerCase());
        stkInstrument.setCurrency(Currency.USD);
        stkInstrument.setPriceTick(0.01);
        stkInstrument.addMap();

        fxInstrument = new Instrument();
        fxInstrument.setPrimaryKey("eurusd_idealpro");
        fxInstrument.setSymbol("eurusd");
        fxInstrument.setMarket(Market.Idealpro.name().toLowerCase());
        fxInstrument.setCurrency(Currency.USD);
        fxInstrument.setPriceTick(0.00001);
        fxInstrument.addMap();
        //
    }


    @Test
    public void sendNewLimitTestStk() throws InterruptedException {
        double price = 200;
        //new order
        OrderRequest orderRequest = OrderRequest.createLimitOrderRequest(System.currentTimeMillis(), algorithmName, stkInstrument, Verb.Buy, 1.0, price);
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest);
        interactiveBrokersTradingEngine.orderRequest(orderRequest);
        while (lastExecutionReport.size() == 0) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequest.getQuantity(), lastExecutionReport.get(0).getQuantity());
        assertEquals(orderRequest.getPrice(), lastExecutionReport.get(0).getPrice());
        assertEquals(orderRequest.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequest.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        //replace it
        OrderRequest orderRequestReplace = OrderRequest.modifyOrder(System.currentTimeMillis(), orderRequest.getAlgorithmInfo(), stkInstrument, orderRequest.getVerb(), orderRequest.getQuantity(), price - 1, orderRequest.getClientOrderId());
        lastExecutionReport.clear();
        System.out.println("orderRequestReplace: " + orderRequestReplace);
        interactiveBrokersTradingEngine.orderRequest(orderRequestReplace);
        while (lastExecutionReport.size() == 0) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequestReplace.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequestReplace.getOrigClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());

        assertEquals(orderRequestReplace.getQuantity(), lastExecutionReport.get(0).getQuantity());
        assertEquals(orderRequestReplace.getPrice(), lastExecutionReport.get(0).getPrice());
        assertEquals(orderRequestReplace.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequestReplace.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequestReplace.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        //cancel it
        OrderRequest orderRequestCancel = OrderRequest.createCancel(System.currentTimeMillis(), orderRequest.getAlgorithmInfo(), stkInstrument, orderRequestReplace.getClientOrderId());
        lastExecutionReport.clear();
        System.out.println("orderRequestCancel: " + orderRequestCancel);
        interactiveBrokersTradingEngine.orderRequest(orderRequestCancel);
        while (lastExecutionReport.size() == 0) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Cancelled, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequestCancel.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequestCancel.getOrigClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());
        assertEquals(orderRequestReplace.getClientOrderId(), lastExecutionReport.get(0).getOrigClientOrderId());

        assertEquals(orderRequestCancel.getQuantity(), lastExecutionReport.get(0).getQuantity());
        assertEquals(orderRequestCancel.getPrice(), lastExecutionReport.get(0).getPrice());
        assertEquals(orderRequestCancel.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequestCancel.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequestCancel.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

    }

    @Test
    public void sendNewMarketTestStk() throws InterruptedException {
        //new order
        double quantity = 1;
        OrderRequest orderRequest = OrderRequest.createMarketOrderRequest(System.currentTimeMillis(), algorithmName, stkInstrument, Verb.Buy, quantity);
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest);
        interactiveBrokersTradingEngine.orderRequest(orderRequest);
        while (lastExecutionReport.size() < 2) {
            Thread.sleep(1000);
        }

        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequest.getQuantity(), lastExecutionReport.get(0).getQuantity());
        assertEquals(orderRequest.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequest.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        assertEquals(ExecutionReportStatus.CompletellyFilled, lastExecutionReport.get(1).getExecutionReportStatus());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(1).getClientOrderId());
        assertEquals(orderRequest.getQuantity(), lastExecutionReport.get(1).getQuantity());
        assertNotEquals(0.0, lastExecutionReport.get(1).getPrice());
        assertEquals(orderRequest.getVerb(), lastExecutionReport.get(1).getVerb());
        assertEquals(orderRequest.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        //undo it
        OrderRequest orderRequest1 = OrderRequest.createMarketOrderRequest(System.currentTimeMillis(), algorithmName, stkInstrument, Verb.Sell, quantity);
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest1);
        interactiveBrokersTradingEngine.orderRequest(orderRequest1);
        while (lastExecutionReport.size() < 2) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest1.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequest1.getQuantity(), lastExecutionReport.get(0).getQuantity());
        assertEquals(orderRequest1.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequest1.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest1.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        assertEquals(ExecutionReportStatus.CompletellyFilled, lastExecutionReport.get(1).getExecutionReportStatus());
        assertEquals(orderRequest1.getClientOrderId(), lastExecutionReport.get(1).getClientOrderId());
        assertEquals(orderRequest1.getQuantity(), lastExecutionReport.get(1).getQuantity());
        assertNotEquals(0.0, lastExecutionReport.get(1).getPrice());
        assertEquals(orderRequest1.getVerb(), lastExecutionReport.get(1).getVerb());
        assertEquals(orderRequest1.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest1.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

    }

    @Test
    public void sendNewMarketTestCrypto() throws InterruptedException {
        //new order
        double quantity = 65000;
        OrderRequest orderRequest = OrderRequest.createMarketOrderRequest(System.currentTimeMillis(), algorithmName, cryptoInstrument, Verb.Buy, quantity);
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest);
        interactiveBrokersTradingEngine.orderRequest(orderRequest);
        while (lastExecutionReport.size() < 2) {
            Thread.sleep(1000);
        }

        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequest.getQuantity(), lastExecutionReport.get(0).getQuantity());
        assertEquals(orderRequest.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequest.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        assertEquals(ExecutionReportStatus.CompletellyFilled, lastExecutionReport.get(1).getExecutionReportStatus());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(1).getClientOrderId());
        assertEquals(orderRequest.getQuantity(), lastExecutionReport.get(1).getQuantity());
        assertNotEquals(0.0, lastExecutionReport.get(1).getPrice());
        assertEquals(orderRequest.getVerb(), lastExecutionReport.get(1).getVerb());
        assertEquals(orderRequest.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        //undo it
        OrderRequest orderRequest1 = OrderRequest.createMarketOrderRequest(System.currentTimeMillis(), algorithmName, cryptoInstrument, Verb.Sell, quantity);
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest1);
        interactiveBrokersTradingEngine.orderRequest(orderRequest1);
        while (lastExecutionReport.size() < 2) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest1.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequest1.getQuantity(), lastExecutionReport.get(0).getQuantity());
        assertEquals(orderRequest1.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequest1.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest1.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        assertEquals(ExecutionReportStatus.CompletellyFilled, lastExecutionReport.get(1).getExecutionReportStatus());
        assertEquals(orderRequest1.getClientOrderId(), lastExecutionReport.get(1).getClientOrderId());
        assertEquals(orderRequest1.getQuantity(), lastExecutionReport.get(1).getQuantity());
        assertNotEquals(0.0, lastExecutionReport.get(1).getPrice());
        assertEquals(orderRequest1.getVerb(), lastExecutionReport.get(1).getVerb());
        assertEquals(orderRequest1.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest1.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

    }

    @Test
    public void sendNewMarketTestFX() throws InterruptedException {
        //new order
        double quantity = 0.01;//SELL 100 USD EUR.USD Forex Warning: Your order size is below the EUR 20000 IdealPro minimum and will be routed as an odd lot order.
        OrderRequest orderRequest = OrderRequest.createMarketOrderRequest(System.currentTimeMillis(), algorithmName, fxInstrument, Verb.Buy, quantity);
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest);
        interactiveBrokersTradingEngine.orderRequest(orderRequest);
        while (lastExecutionReport.size() < 2) {
            Thread.sleep(1000);
        }

        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequest.getQuantity(), lastExecutionReport.get(0).getQuantity());
        assertEquals(orderRequest.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequest.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        assertEquals(ExecutionReportStatus.CompletellyFilled, lastExecutionReport.get(1).getExecutionReportStatus());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(1).getClientOrderId());
        assertEquals(orderRequest.getQuantity(), lastExecutionReport.get(1).getQuantity());
        assertNotEquals(0.0, lastExecutionReport.get(1).getPrice());
        assertEquals(orderRequest.getVerb(), lastExecutionReport.get(1).getVerb());
        assertEquals(orderRequest.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        //undo it
        OrderRequest orderRequest1 = OrderRequest.createMarketOrderRequest(System.currentTimeMillis(), algorithmName, fxInstrument, Verb.Sell, quantity);
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest1);
        interactiveBrokersTradingEngine.orderRequest(orderRequest1);
        while (lastExecutionReport.size() < 2) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest1.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequest1.getQuantity(), lastExecutionReport.get(0).getQuantity());
        assertEquals(orderRequest1.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequest1.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest1.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        assertEquals(ExecutionReportStatus.CompletellyFilled, lastExecutionReport.get(1).getExecutionReportStatus());
        assertEquals(orderRequest1.getClientOrderId(), lastExecutionReport.get(1).getClientOrderId());
        assertEquals(orderRequest1.getQuantity(), lastExecutionReport.get(1).getQuantity());
        assertNotEquals(0.0, lastExecutionReport.get(1).getPrice());
        assertEquals(orderRequest1.getVerb(), lastExecutionReport.get(1).getVerb());
        assertEquals(orderRequest1.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest1.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

    }

    @Test
    public void sendNewLimitTestFX() throws InterruptedException {
        //new order
        double quantity = 0.01;//SELL 100 USD EUR.USD Forex Warning: Your order size is below the EUR 20000 IdealPro minimum and will be routed as an odd lot order.
        OrderRequest orderRequest = OrderRequest.createLimitOrderRequest(System.currentTimeMillis(), algorithmName, fxInstrument, Verb.Buy, quantity, 1.02);
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest);
        interactiveBrokersTradingEngine.orderRequest(orderRequest);
        while (lastExecutionReport.size() < 1) {
            Thread.sleep(1000);
        }

        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequest.getQuantity(), lastExecutionReport.get(0).getQuantity());
        assertEquals(orderRequest.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequest.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());


        //undo it
        OrderRequest orderRequest1 = OrderRequest.modifyOrder(System.currentTimeMillis(), algorithmName, fxInstrument, Verb.Buy, quantity, 1.025, lastExecutionReport.get(0).getClientOrderId());
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest1);
        interactiveBrokersTradingEngine.orderRequest(orderRequest1);
        while (lastExecutionReport.size() < 1) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest1.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequest1.getQuantity(), lastExecutionReport.get(0).getQuantity());
        assertEquals(orderRequest1.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequest1.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest1.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

        //cancel it
        OrderRequest orderRequest2 = OrderRequest.createCancel(System.currentTimeMillis(), algorithmName, fxInstrument, lastExecutionReport.get(0).getClientOrderId());
        lastExecutionReport.clear();
        System.out.println("orderRequest: " + orderRequest2);
        interactiveBrokersTradingEngine.orderRequest(orderRequest2);
        while (lastExecutionReport.size() < 1) {
            Thread.sleep(1000);
        }
        assertEquals(ExecutionReportStatus.Cancelled, lastExecutionReport.get(0).getExecutionReportStatus());
        assertEquals(orderRequest2.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
        assertEquals(orderRequest2.getQuantity(), lastExecutionReport.get(0).getQuantity());
        assertEquals(orderRequest2.getVerb(), lastExecutionReport.get(0).getVerb());
        assertEquals(orderRequest2.getInstrument(), lastExecutionReport.get(0).getInstrument());
        assertEquals(orderRequest2.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());

    }
//    @Test
//    public void sendNewLimitTestCrypto() throws InterruptedException {
//        //new order
//        double quantity = 0.00005;
//        OrderRequest orderRequest = OrderRequest.createLimitOrderRequest(System.currentTimeMillis(), algorithmName, cryptoInstrument, Verb.Buy, quantity,97800);
//        lastExecutionReport.clear();
//        System.out.println("orderRequest: " + orderRequest);
//        interactiveBrokersTradingEngine.orderRequest(orderRequest);
//        while (lastExecutionReport.size() < 1) {
//            Thread.sleep(1000);
//        }
//
//        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
//        assertEquals(orderRequest.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
//        assertEquals(orderRequest.getQuantity(), lastExecutionReport.get(0).getQuantity());
//        assertEquals(orderRequest.getVerb(), lastExecutionReport.get(0).getVerb());
//        assertEquals(orderRequest.getInstrument(), lastExecutionReport.get(0).getInstrument());
//        assertEquals(orderRequest.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());
//
//
//        //undo it
//        OrderRequest orderRequest1 = OrderRequest.modifyOrder(System.currentTimeMillis(), algorithmName, cryptoInstrument,Verb.Buy, quantity,97801,lastExecutionReport.get(0).getClientOrderId());
//        lastExecutionReport.clear();
//        System.out.println("orderRequest: " + orderRequest1);
//        interactiveBrokersTradingEngine.orderRequest(orderRequest1);
//        while (lastExecutionReport.size() < 1) {
//            Thread.sleep(1000);
//        }
//        assertEquals(ExecutionReportStatus.Active, lastExecutionReport.get(0).getExecutionReportStatus());
//        assertEquals(orderRequest1.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
//        assertEquals(orderRequest1.getQuantity(), lastExecutionReport.get(0).getQuantity());
//        assertEquals(orderRequest1.getVerb(), lastExecutionReport.get(0).getVerb());
//        assertEquals(orderRequest1.getInstrument(), lastExecutionReport.get(0).getInstrument());
//        assertEquals(orderRequest1.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());
//
//        //cancel it
//        OrderRequest orderRequest2 = OrderRequest.createCancel(System.currentTimeMillis(), algorithmName, cryptoInstrument, lastExecutionReport.get(0).getClientOrderId());
//        lastExecutionReport.clear();
//        System.out.println("orderRequest: " + orderRequest2);
//        interactiveBrokersTradingEngine.orderRequest(orderRequest2);
//        while (lastExecutionReport.size() < 1) {
//            Thread.sleep(1000);
//        }
//        assertEquals(ExecutionReportStatus.Cancelled, lastExecutionReport.get(0).getExecutionReportStatus());
//        assertEquals(orderRequest2.getClientOrderId(), lastExecutionReport.get(0).getClientOrderId());
//        assertEquals(orderRequest2.getQuantity(), lastExecutionReport.get(0).getQuantity());
//        assertEquals(orderRequest2.getVerb(), lastExecutionReport.get(0).getVerb());
//        assertEquals(orderRequest2.getInstrument(), lastExecutionReport.get(0).getInstrument());
//        assertEquals(orderRequest2.getAlgorithmInfo(), lastExecutionReport.get(0).getAlgorithmInfo());
//
//    }
}