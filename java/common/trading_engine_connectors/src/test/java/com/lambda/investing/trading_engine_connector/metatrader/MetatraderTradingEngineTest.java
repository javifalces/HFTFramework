package com.lambda.investing.trading_engine_connector.metatrader;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorPublisherProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqPublisher;
import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.market_data_connector.metatrader.MetatraderMarketDataPublisher;
import com.lambda.investing.metatrader.MetatraderZeroBrokerConnector;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.AbstractBrokerTradingEngine;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.lambda.investing.model.Util.fromJsonString;
import static com.lambda.investing.model.Util.fromJsonStringGSON;


@Ignore("need to open metatrader for testing")
public class MetatraderTradingEngineTest {

    protected static String ALGORITHM_INFO = "JUNIT_MetatraderTradingEngineTest";
    protected static String INSTRUMENT_PK = "gbpusd_darwinex";
    protected static String INSTRUMENT_PK2 = "eurusd_darwinex";
    protected static String INSTRUMENT_PK3 = "eurgbp_darwinex";
    protected static String HOST = "localhost";
    protected static int marketDataPort = 666;
    protected static int tradingEnginePort = 677;

    protected static String MT_HOST = "localhost";
    protected static int MT_PORT_PUBLISHER = 32770;
    protected static int MT_PORT_PUSH = 32769;
    protected static int MT_PORT_PULL = 32768;

    protected AbstractBrokerTradingEngine tradingEngine;
    protected List<ExecutionReport> lastExecutionReport = new ArrayList<>();
    protected AtomicLong counterER = new AtomicLong(0);

    protected Listener listener = new Listener();

    private class Listener implements ConnectorListener {

        @Override
        public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
                             TypeMessage typeMessage, String content) {
            if (typeMessage.equals(TypeMessage.execution_report)) {
                ExecutionReport executionReport = fromJsonStringGSON(content, ExecutionReport.class);
                onExecutionReportUpdate(executionReport);
            }
        }

        public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
            lastExecutionReport.add(executionReport);
            counterER.incrementAndGet();
            return true;
        }

    }


    public MetatraderTradingEngineTest() throws InterruptedException {
        ConnectorConfiguration orderRequestConfiguration = new ZeroMqConfiguration();
        ((ZeroMqConfiguration) orderRequestConfiguration).setHost(HOST);
        ((ZeroMqConfiguration) orderRequestConfiguration).setPort(tradingEnginePort);

        ConnectorProvider orderRequestConnectorProvider = ZeroMqProvider
                .getInstance((ZeroMqConfiguration) orderRequestConfiguration, 0);
        ((ZeroMqProvider) orderRequestConnectorProvider).start();

//		ZEROMQ publisher
//		ConnectorConfiguration marketDataConfiguration = new ZeroMqConfiguration();
//		((ZeroMqConfiguration) marketDataConfiguration).setHost(HOST);
//		((ZeroMqConfiguration) marketDataConfiguration).setPort(marketDataPort);
//		ConnectorPublisher marketDataPublisher = new ZeroMqPublisher("metatraderMarketDataConnectorPublisher", 0);

        OrdinaryConnectorConfiguration marketDataConfiguration = new OrdinaryConnectorConfiguration();
        ConnectorPublisher marketDataPublisher = new OrdinaryConnectorPublisherProvider("metatraderMarketDataConnectorPublisher", 0, Thread.NORM_PRIORITY);


        MetatraderZeroBrokerConnector metatraderZeroBrokerConnector = MetatraderZeroBrokerConnector
                .getInstance(MT_HOST, MT_PORT_PUBLISHER, MT_PORT_PUSH, MT_PORT_PULL);


        MetatraderMarketDataPublisher metatraderMarketDataPublisher = new MetatraderMarketDataPublisher(
                marketDataConfiguration, marketDataPublisher, metatraderZeroBrokerConnector);
        metatraderMarketDataPublisher.setBroker("darwinex");
        metatraderMarketDataPublisher.init();

        tradingEngine = new MetatraderTradingEngine(orderRequestConfiguration, orderRequestConnectorProvider,
                marketDataConfiguration, marketDataPublisher, metatraderZeroBrokerConnector);
        ((MetatraderTradingEngine) tradingEngine).setBroker("darwinex");
        tradingEngine.start();

//		OrdinaryConnectorPublisherProvider marketDataProvider = new OrdinaryConnectorPublisherProvider(,0);
        ((OrdinaryConnectorPublisherProvider) marketDataPublisher).register(marketDataConfiguration, listener);

//
//		ConnectorProvider marketDataProvider = ZeroMqProvider
//				.getInstance((ZeroMqConfiguration) marketDataConfiguration, 0);
//
//		marketDataProvider.register(marketDataConfiguration, listener);
//
//
//		((ZeroMqProvider) marketDataProvider).start();


    }

    @Before
    public void setUp() throws Exception {
        lastExecutionReport.clear();
        counterER = new AtomicLong(0);
        ((MetatraderTradingEngine) tradingEngine).setNettingByEngine(true);
    }

    private double getPosition(String instrument) {
        ((MetatraderTradingEngine) tradingEngine).getPositionMT(true);
        double position = ((MetatraderTradingEngine) tradingEngine).getPosition(instrument);
        return position;
    }

    protected String generateClientOrderId() {
        return UUID.randomUUID().toString();
    }

    private OrderRequest createMarketOrderRequest(String instrument, Verb verb, double quantity) {
        ///controled market request
        String newClientOrderId = this.generateClientOrderId();
        OrderRequest output = new OrderRequest();
        output.setAlgorithmInfo(ALGORITHM_INFO);
        output.setInstrument(instrument);
        output.setVerb(verb);
        output.setOrderRequestAction(OrderRequestAction.Send);
        output.setClientOrderId(newClientOrderId);
        output.setQuantity(Math.abs(quantity));
        //		output.setPrice(price);
        output.setTimestampCreation(System.currentTimeMillis());
        output.setOrderType(OrderType.Market);//limit for quoting
        output.setMarketOrderType(MarketOrderType.FAS);//default FAS
        return output;
    }

    private OrderRequest createLimitOrderRequest(String instrument, Verb verb, double quantity, double price) {
        ///controled market request
        String newClientOrderId = this.generateClientOrderId();
        OrderRequest output = new OrderRequest();
        output.setAlgorithmInfo(ALGORITHM_INFO);
        output.setInstrument(instrument);
        output.setVerb(verb);
        output.setOrderRequestAction(OrderRequestAction.Send);
        output.setClientOrderId(newClientOrderId);
        output.setQuantity(quantity);
        output.setPrice(price);
        output.setTimestampCreation(System.currentTimeMillis());
        output.setOrderType(OrderType.Limit);//limit for quoting
        output.setMarketOrderType(MarketOrderType.FAS);//default FAS
        return output;
    }

    private void resetPositionToZero(String instrument) {
        double position = getPosition(instrument);
        if (position != 0) {
            Verb verb = position > 0 ? Verb.Sell : Verb.Buy;

            OrderRequest orderRequest = createMarketOrderRequest(instrument, verb, position);
            lastExecutionReport.clear();
            tradingEngine.orderRequest(orderRequest);
            List<ExecutionReport> output = new ArrayList<>(lastExecutionReport);
            while (output.size() < 2) {
                try {
                    for (ExecutionReport executionReport : lastExecutionReport) {
                        if (executionReport.getClientOrderId().equals(orderRequest.getClientOrderId())) {
                            output.add(executionReport);
                        }
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            lastExecutionReport.clear();

//			while (lastExecutionReport.size() < 2) {
//				try {
//					Thread.sleep(500);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
        }
    }

    private synchronized List<ExecutionReport> changePosition(String instrument, double positionDelta) {

        Verb verb = positionDelta > 0 ? Verb.Buy : Verb.Sell;
        OrderRequest orderRequest = createMarketOrderRequest(instrument, verb, Math.abs(positionDelta));
        lastExecutionReport.clear();
        tradingEngine.orderRequest(orderRequest);
        List<ExecutionReport> output = new ArrayList<>(lastExecutionReport);
        while (output.size() < 2) {
            try {
                for (ExecutionReport executionReport : lastExecutionReport) {
                    if (executionReport.getClientOrderId().equals(orderRequest.getClientOrderId())) {
                        if (!output.contains(executionReport)) {
                            output.add(executionReport);
                        }
                    }
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lastExecutionReport.clear();
        return output;
    }

    @Ignore
    @Test
    public void testLimitOrder() {
        Verb verb = Verb.Buy;
        OrderRequest orderRequest = createLimitOrderRequest(INSTRUMENT_PK, verb, 0.01, 1.3825);
        tradingEngine.orderRequest(orderRequest);

        while (lastExecutionReport.size() == 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
        Assert.assertNotNull(last);
        Assert.assertEquals(last.getVerb(), verb);
    }


    @Test
    public void testMktOrder() {
        Verb verb = Verb.Buy;
        double position = getPosition(INSTRUMENT_PK);
        OrderRequest orderRequest = createMarketOrderRequest(INSTRUMENT_PK, verb, 0.01);
        tradingEngine.orderRequest(orderRequest);
        while (lastExecutionReport.size() < 2) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
        Assert.assertNotNull(last);
        Assert.assertEquals(last.getVerb(), verb);
        Assert.assertEquals(ExecutionReportStatus.CompletellyFilled, last.getExecutionReportStatus());

        ExecutionReport first = lastExecutionReport.get(0);
        Assert.assertNotNull(first);
        Assert.assertEquals(first.getVerb(), verb);
        Assert.assertEquals(ExecutionReportStatus.Active, first.getExecutionReportStatus());

        Assert.assertEquals(position + orderRequest.getQuantity(), getPosition(INSTRUMENT_PK), 1E-6);

        lastExecutionReport.clear();
        OrderRequest orderRequest1 = createMarketOrderRequest(INSTRUMENT_PK, Verb.OtherSideVerb(verb), 0.01);
        tradingEngine.orderRequest(orderRequest1);
        while (lastExecutionReport.size() < 2) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ExecutionReport last1 = lastExecutionReport.get(lastExecutionReport.size() - 1);
        Assert.assertNotNull(last1);
        Assert.assertEquals(last1.getVerb(), Verb.OtherSideVerb(verb));
        Assert.assertEquals(ExecutionReportStatus.CompletellyFilled, last1.getExecutionReportStatus());

        ExecutionReport first1 = lastExecutionReport.get(0);
        Assert.assertNotNull(first1);
        Assert.assertEquals(first1.getVerb(), Verb.OtherSideVerb(verb));
        Assert.assertEquals(ExecutionReportStatus.Active, first1.getExecutionReportStatus());

        Assert.assertEquals(position, getPosition(INSTRUMENT_PK), 1E-6);

    }

    @Test
    public void testNettingBuyMktOrder() {
        resetPositionToZero(INSTRUMENT_PK);
        double initialPosition = getPosition(INSTRUMENT_PK);
        double lots = 0.01;
        changePosition(INSTRUMENT_PK, lots);
        Assert.assertEquals(initialPosition + lots, getPosition(INSTRUMENT_PK), 1E-3);

        List<ExecutionReport> lastExecutionReport = changePosition(INSTRUMENT_PK, -lots);
        ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
        Assert.assertNotNull(last);
        Assert.assertEquals(last.getVerb(), Verb.Sell);
        Assert.assertEquals(initialPosition, getPosition(INSTRUMENT_PK), 1E-3);
    }

    @Test
    public void testNettingBuyTwiceMktOrder() {
        resetPositionToZero(INSTRUMENT_PK);
        double initialPosition = getPosition(INSTRUMENT_PK);
        double lots = 0.01;
        changePosition(INSTRUMENT_PK, lots);
        Assert.assertEquals(initialPosition + lots, getPosition(INSTRUMENT_PK), 1E-3);
        changePosition(INSTRUMENT_PK, lots);
        Assert.assertEquals(initialPosition + 2 * lots, getPosition(INSTRUMENT_PK), 1E-3);

        List<ExecutionReport> lastExecutionReport = changePosition(INSTRUMENT_PK, -lots);
        ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
        Assert.assertNotNull(last);
        Assert.assertEquals(last.getVerb(), Verb.Sell);
        Assert.assertEquals(initialPosition + lots, getPosition(INSTRUMENT_PK), 1E-3);

        List<ExecutionReport> lastExecutionReport1 = changePosition(INSTRUMENT_PK, -lots);
        ExecutionReport last1 = lastExecutionReport1.get(lastExecutionReport.size() - 1);
        Assert.assertNotNull(last1);
        Assert.assertEquals(last1.getVerb(), Verb.Sell);
        Assert.assertEquals(initialPosition, getPosition(INSTRUMENT_PK), 1E-3);
    }


    @Test
    public void testNettingBuyIncreaseSlowMktOrder() {
        resetPositionToZero(INSTRUMENT_PK);
        double initialPosition = getPosition(INSTRUMENT_PK);
        double lots = 0.01;
        int stepsIncrease = 5;
        for (int i = 0; i < stepsIncrease; i++) {
            changePosition(INSTRUMENT_PK, lots);
        }
        Assert.assertEquals(initialPosition + lots * stepsIncrease, getPosition(INSTRUMENT_PK), 1E-3);
        changePosition(INSTRUMENT_PK, -(lots * stepsIncrease));
        Assert.assertEquals(initialPosition, getPosition(INSTRUMENT_PK), 1E-3);
    }

    @Test
    public void testNettingBuyDecreaseSlowMktOrder() {
        resetPositionToZero(INSTRUMENT_PK);
        double initialPosition = getPosition(INSTRUMENT_PK);
        double lots = 0.01;
        int stepsIncrease = 5;
        changePosition(INSTRUMENT_PK, (lots * stepsIncrease));
        Assert.assertEquals(initialPosition + lots * stepsIncrease, getPosition(INSTRUMENT_PK), 1E-3);
        for (int i = 0; i < stepsIncrease; i++) {
            changePosition(INSTRUMENT_PK, -lots);
        }
        Assert.assertEquals(initialPosition, getPosition(INSTRUMENT_PK), 1E-3);
    }

    @Test
    public void testNettingBuyDecreaseBiggerMktOrder() {
        resetPositionToZero(INSTRUMENT_PK);
        double initialPosition = getPosition(INSTRUMENT_PK);
        double lots = 0.01;
        int stepsIncrease = 5;
        changePosition(INSTRUMENT_PK, (lots));
        //increase 0.01
        Assert.assertEquals(initialPosition + lots, getPosition(INSTRUMENT_PK), 1E-3);

        //reduce 0.05
        List<ExecutionReport> lastExecutionReport = changePosition(INSTRUMENT_PK, (-stepsIncrease * lots));

        //position must be -0.04
        //close the previus one and leave one stepIncreaseLess
        Assert.assertEquals(initialPosition - ((stepsIncrease - 1) * lots), getPosition(INSTRUMENT_PK), 1E-3);
        ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
        Assert.assertNotNull(last);
        Assert.assertEquals(last.getVerb(), Verb.Sell);

//		Assert.assertEquals(stepsIncrease * lots, last.getLastQuantity(), 1E-3);
//		Assert.assertEquals(ExecutionReportStatus.CompletellyFilled, last.getExecutionReportStatus());

        resetPositionToZero(INSTRUMENT_PK);
    }

    @Test
    public void testNettingBuyAndReducingWitDifferentTranches() {
        resetPositionToZero(INSTRUMENT_PK);
        double initialPosition = getPosition(INSTRUMENT_PK);

        changePosition(INSTRUMENT_PK, (0.02));
        changePosition(INSTRUMENT_PK, (0.02));
        changePosition(INSTRUMENT_PK, (0.01));
        Assert.assertEquals(initialPosition + 0.05, getPosition(INSTRUMENT_PK), 1E-3);

        List<ExecutionReport> lastExecutionReport = changePosition(INSTRUMENT_PK, -0.03);
        //close the previous one and leave one stepIncreaseLess
        Assert.assertEquals(initialPosition + 0.02, getPosition(INSTRUMENT_PK), 1E-3);
        ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
        Assert.assertNotNull(last);
        Assert.assertEquals(last.getVerb(), Verb.Sell);
//		Assert.assertEquals(0.03, last.getLastQuantity(), 1E-3);
//		Assert.assertEquals(ExecutionReportStatus.CompletellyFilled, last.getExecutionReportStatus());

        resetPositionToZero(INSTRUMENT_PK);
    }

    @Test
    public void testStatArb() {
        resetPositionToZero(INSTRUMENT_PK);
        resetPositionToZero(INSTRUMENT_PK2);
        double initialPosition = getPosition(INSTRUMENT_PK);
        double initialPosition2 = getPosition(INSTRUMENT_PK2);

        changePosition(INSTRUMENT_PK, (0.02));
        changePosition(INSTRUMENT_PK2, (0.01));
        Assert.assertEquals(initialPosition + 0.02, getPosition(INSTRUMENT_PK), 1E-3);
        Assert.assertEquals(initialPosition2 + 0.01, getPosition(INSTRUMENT_PK2), 1E-3);

        changePosition(INSTRUMENT_PK, (0.01));
        changePosition(INSTRUMENT_PK2, (0.02));
        Assert.assertEquals(initialPosition + 0.03, getPosition(INSTRUMENT_PK), 1E-3);
        Assert.assertEquals(initialPosition2 + 0.03, getPosition(INSTRUMENT_PK2), 1E-3);


        List<ExecutionReport> lastExecutionReport = changePosition(INSTRUMENT_PK, -0.02);
        List<ExecutionReport> lastExecutionReport2 = changePosition(INSTRUMENT_PK2, -0.01);
        List<ExecutionReport> lastExecutionReport3 = changePosition(INSTRUMENT_PK, -0.01);
        List<ExecutionReport> lastExecutionReport4 = changePosition(INSTRUMENT_PK2, -0.02);
        //close the previous one and leave one stepIncreaseLess

        Assert.assertEquals(initialPosition, getPosition(INSTRUMENT_PK), 1E-3);
        Assert.assertEquals(initialPosition2, getPosition(INSTRUMENT_PK2), 1E-3);

        ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
        Assert.assertNotNull(last);
        Assert.assertEquals(last.getVerb(), Verb.Sell);
        Assert.assertEquals(ExecutionReportStatus.CompletellyFilled, last.getExecutionReportStatus());
        Assert.assertEquals(0.02, last.getLastQuantity(), 0.0001);
        Assert.assertEquals(INSTRUMENT_PK, last.getInstrument());

        ExecutionReport last2 = lastExecutionReport2.get(lastExecutionReport2.size() - 1);
        Assert.assertNotNull(last2);
        Assert.assertEquals(last2.getVerb(), Verb.Sell);
        Assert.assertEquals(ExecutionReportStatus.CompletellyFilled, last2.getExecutionReportStatus());
        Assert.assertEquals(0.01, last2.getLastQuantity(), 0.0001);
        Assert.assertEquals(INSTRUMENT_PK2, last2.getInstrument());

        ExecutionReport last3 = lastExecutionReport3.get(lastExecutionReport3.size() - 1);
        Assert.assertNotNull(last3);
        Assert.assertEquals(last3.getVerb(), Verb.Sell);
        Assert.assertEquals(INSTRUMENT_PK, last3.getInstrument());
        Assert.assertEquals(ExecutionReportStatus.CompletellyFilled, last3.getExecutionReportStatus());
        Assert.assertEquals(0.01, last3.getLastQuantity(), 0.0001);


        ExecutionReport last4 = lastExecutionReport4.get(lastExecutionReport4.size() - 1);
        Assert.assertNotNull(last4);
        Assert.assertEquals(last4.getVerb(), Verb.Sell);
        Assert.assertEquals(INSTRUMENT_PK2, last4.getInstrument());
        Assert.assertEquals(ExecutionReportStatus.CompletellyFilled, last4.getExecutionReportStatus());
        Assert.assertEquals(0.02, last4.getLastQuantity(), 0.0001);


        resetPositionToZero(INSTRUMENT_PK);
        resetPositionToZero(INSTRUMENT_PK2);
    }

    @Test
    public void testStatArb2() {
        resetPositionToZero(INSTRUMENT_PK);
        resetPositionToZero(INSTRUMENT_PK2);
        resetPositionToZero(INSTRUMENT_PK3);
        double initialPosition = getPosition(INSTRUMENT_PK);
        double initialPosition2 = getPosition(INSTRUMENT_PK2);
        double initialPosition3 = getPosition(INSTRUMENT_PK3);

        changePosition(INSTRUMENT_PK, (0.02));
        changePosition(INSTRUMENT_PK2, (-0.06));
        changePosition(INSTRUMENT_PK3, (-0.04));
        Assert.assertEquals(initialPosition + 0.02, getPosition(INSTRUMENT_PK), 1E-3);
        Assert.assertEquals(initialPosition2 - 0.06, getPosition(INSTRUMENT_PK2), 1E-3);
        Assert.assertEquals(initialPosition3 - 0.04, getPosition(INSTRUMENT_PK3), 1E-3);

        changePosition(INSTRUMENT_PK, (0.02));
        changePosition(INSTRUMENT_PK2, (-0.06));
        changePosition(INSTRUMENT_PK3, (-0.04));
        Assert.assertEquals(initialPosition + 0.02 * 2, getPosition(INSTRUMENT_PK), 1E-3);
        Assert.assertEquals(initialPosition2 - 0.06 * 2, getPosition(INSTRUMENT_PK2), 1E-3);
        Assert.assertEquals(initialPosition3 - 0.04 * 2, getPosition(INSTRUMENT_PK3), 1E-3);

        changePosition(INSTRUMENT_PK, (-0.02));
        changePosition(INSTRUMENT_PK2, (+0.06));
        changePosition(INSTRUMENT_PK3, (+0.04));
        Assert.assertEquals(initialPosition + 0.02, getPosition(INSTRUMENT_PK), 1E-3);
        Assert.assertEquals(initialPosition2 - 0.06, getPosition(INSTRUMENT_PK2), 1E-3);
        Assert.assertEquals(initialPosition3 - 0.04, getPosition(INSTRUMENT_PK3), 1E-3);

        resetPositionToZero(INSTRUMENT_PK);
        resetPositionToZero(INSTRUMENT_PK2);
        resetPositionToZero(INSTRUMENT_PK3);
        Assert.assertEquals(0.0, getPosition(INSTRUMENT_PK), 1E-3);
        Assert.assertEquals(0.0, getPosition(INSTRUMENT_PK2), 1E-3);
        Assert.assertEquals(0.0, getPosition(INSTRUMENT_PK3), 1E-3);
    }

    @Test
    public void testStatArb3() {
        resetPositionToZero(INSTRUMENT_PK);
        resetPositionToZero(INSTRUMENT_PK2);
        resetPositionToZero(INSTRUMENT_PK3);
        double initialPosition = getPosition(INSTRUMENT_PK);
        double initialPosition2 = getPosition(INSTRUMENT_PK2);
        double initialPosition3 = getPosition(INSTRUMENT_PK3);

        changePosition(INSTRUMENT_PK, (-0.02));
        changePosition(INSTRUMENT_PK2, (+0.06));
        changePosition(INSTRUMENT_PK3, (+0.04));
        Assert.assertEquals(initialPosition - 0.02, getPosition(INSTRUMENT_PK), 1E-3);
        Assert.assertEquals(initialPosition2 + 0.06, getPosition(INSTRUMENT_PK2), 1E-3);
        Assert.assertEquals(initialPosition3 + 0.04, getPosition(INSTRUMENT_PK3), 1E-3);

        changePosition(INSTRUMENT_PK, (-0.02));
        changePosition(INSTRUMENT_PK2, (+0.06));
        changePosition(INSTRUMENT_PK3, (+0.04));
        Assert.assertEquals(initialPosition - 0.02 * 2, getPosition(INSTRUMENT_PK), 1E-3);
        Assert.assertEquals(initialPosition2 + 0.06 * 2, getPosition(INSTRUMENT_PK2), 1E-3);
        Assert.assertEquals(initialPosition3 + 0.04 * 2, getPosition(INSTRUMENT_PK3), 1E-3);

        changePosition(INSTRUMENT_PK, (0.02));
        changePosition(INSTRUMENT_PK2, (-0.06));
        changePosition(INSTRUMENT_PK3, (-0.04));
        Assert.assertEquals(initialPosition - 0.02, getPosition(INSTRUMENT_PK), 1E-3);
        Assert.assertEquals(initialPosition2 + 0.06, getPosition(INSTRUMENT_PK2), 1E-3);
        Assert.assertEquals(initialPosition3 + 0.04, getPosition(INSTRUMENT_PK3), 1E-3);

        resetPositionToZero(INSTRUMENT_PK);
        resetPositionToZero(INSTRUMENT_PK2);
        resetPositionToZero(INSTRUMENT_PK3);
        Assert.assertEquals(0.0, getPosition(INSTRUMENT_PK), 1E-3);
        Assert.assertEquals(0.0, getPosition(INSTRUMENT_PK2), 1E-3);
        Assert.assertEquals(0.0, getPosition(INSTRUMENT_PK3), 1E-3);
    }

    @Test
    public void limitModifyAndCancel() {
        resetPositionToZero(INSTRUMENT_PK);

        lastExecutionReport.clear();

        double bidPrice = 1.0;
        double lots = 0.02;

        OrderRequest orderRequest = createLimitOrderRequest(INSTRUMENT_PK, Verb.Buy, lots, bidPrice);
        tradingEngine.orderRequest(orderRequest);
        while (lastExecutionReport.size() < 1) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ExecutionReport last = lastExecutionReport.get(lastExecutionReport.size() - 1);
        Assert.assertNotNull(last);
        Assert.assertEquals(last.getVerb(), Verb.Buy);
        Assert.assertEquals(ExecutionReportStatus.Active, last.getExecutionReportStatus());
        lastExecutionReport.clear();
        int steps = 1;
        try {
            while (steps < 5) {
                double newPrice = bidPrice + 0.01 * steps;
                OrderRequest orderRequest2 = createLimitOrderRequest(INSTRUMENT_PK, Verb.Buy, lots, newPrice);
                orderRequest2.setOrderRequestAction(OrderRequestAction.Modify);
                orderRequest2.setOrigClientOrderId(orderRequest.getClientOrderId());

                tradingEngine.orderRequest(orderRequest2);
                while (lastExecutionReport.size() < 1) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                ExecutionReport last2 = lastExecutionReport.get(lastExecutionReport.size() - 1);
                Assert.assertNotNull(last2);
                Assert.assertEquals(last2.getVerb(), Verb.Buy);
                Assert.assertEquals(ExecutionReportStatus.Active, last2.getExecutionReportStatus());
                lastExecutionReport.clear();
                orderRequest = orderRequest2;

                steps++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        //cancel
        OrderRequest orderRequest2 = createLimitOrderRequest(INSTRUMENT_PK, Verb.Buy, lots, bidPrice + 0.01 * steps);
        orderRequest2.setOrderRequestAction(OrderRequestAction.Cancel);
        orderRequest2.setOrigClientOrderId(orderRequest.getClientOrderId());
        tradingEngine.orderRequest(orderRequest2);
        while (lastExecutionReport.size() < 1) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ExecutionReport last2 = lastExecutionReport.get(lastExecutionReport.size() - 1);
        Assert.assertNotNull(last2);
        Assert.assertEquals(last2.getVerb(), Verb.Buy);
        Assert.assertEquals(ExecutionReportStatus.Cancelled, last2.getExecutionReportStatus());


    }
}
