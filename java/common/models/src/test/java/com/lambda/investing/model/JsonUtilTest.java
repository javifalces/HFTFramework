package com.lambda.investing.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.*;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.lambda.investing.model.Util.GSON;

class JsonUtilTest {

    String instrumentPk = "BTCUSD";

    private Depth createDepth(double bestBid, double bestAsk, double bestBidQty, double bestAskQty) {
        Depth depth = new Depth();
        depth.setTimestamp(System.currentTimeMillis());
        depth.setInstrument(instrumentPk);
        depth.setLevels(1);
        Double[] asks = new Double[]{bestAsk, bestAsk + 0.01};
        depth.setAsks(asks);

        Double[] bids = new Double[]{bestBid, bestAsk - 0.01};
        depth.setBids(bids);

        Double[] asksQ = new Double[]{bestAskQty, bestAskQty};
        depth.setAsksQuantities(asksQ);

        Double[] bidsQ = new Double[]{bestBidQty, bestBidQty};
        depth.setBidsQuantities(bidsQ);

        String[] algorithms = new String[]{Depth.ALGORITHM_INFO_MM, Depth.ALGORITHM_INFO_MM};
        List<String>[] algorithmsList = new List[]{Arrays.asList(algorithms), Arrays.asList(algorithms)};
        depth.setAsksAlgorithmInfo(algorithmsList);
        depth.setBidsAlgorithmInfo(algorithmsList);
        depth.setLevelsFromData();
        return depth;

    }

    private Trade createTrade(double price, double quantity, Verb verb) {
        Trade trade = new Trade();
        trade.setTimestamp(System.currentTimeMillis());
        trade.setInstrument(instrumentPk);
        trade.setPrice(price);
        trade.setQuantity(quantity);
        trade.setVerb(verb);
        trade.setAlgorithmInfo(Depth.ALGORITHM_INFO_MM);
        return trade;
    }

    @Test
    public void depthToFromJsonString() {
        Depth depth = createDepth(10000, 10001, 1, 1);
        String jsonString = Util.toJsonString(depth);
        System.out.println(jsonString);
        Assert.assertNotNull(jsonString);

        String oldJsonString = GSON.toJson(depth);
        Depth depthOld = Util.fromJsonString(oldJsonString, Depth.class);
        Assert.assertEquals(depthOld.toString(), depth.toString());

        Depth depth2 = Util.fromJsonString(jsonString, Depth.class);
        Assert.assertEquals(jsonString, depth2.toString());
    }

    @Test
    public void tradeToFromJsonString() {
        Trade trade = createTrade(1000, 5, Verb.Buy);
        String jsonString = Util.toJsonString(trade);
        System.out.println(jsonString);
        Assert.assertNotNull(jsonString);

        String oldJsonString = GSON.toJson(trade);
        Trade tradeOld = Util.fromJsonString(oldJsonString, Trade.class);
        Assert.assertEquals(tradeOld.toString(), trade.toString());

//        Assert.assertEquals(jsonString,oldJsonString);

        Trade trade2 = Util.fromJsonString(jsonString, Trade.class);
        Assert.assertEquals(jsonString, trade2.toString());


    }

    private ExecutionReport createExecutionReport(Verb verb, double price, double quantity) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setOrderType(OrderType.Limit);
        orderRequest.setVerb(verb);
        orderRequest.setPrice(price);
        orderRequest.setQuantity(quantity);
        orderRequest.setQuantity(quantity);
        orderRequest.setInstrument(instrumentPk);
        orderRequest.setClientOrderId(UUID.randomUUID().toString());

        ExecutionReport executionReport = new ExecutionReport(orderRequest);
        executionReport.setLastQuantity(quantity);
        executionReport.setTimestampCreation(System.currentTimeMillis());
        executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
        return executionReport;
    }

    @Test
    public void executionReportToFromJsonString() {
        ExecutionReport executionReport = createExecutionReport(Verb.Buy, 100, 5);
        String jsonString = Util.toJsonString(executionReport);
        System.out.println(jsonString);
        Assert.assertNotNull(jsonString);

        String oldJsonString = GSON.toJson(executionReport);
        ExecutionReport executionReport1 = Util.fromJsonString(oldJsonString, ExecutionReport.class);
        Assert.assertEquals(executionReport1.toJsonString(), executionReport.toJsonString());

//        Assert.assertEquals(jsonString,oldJsonString);

        ExecutionReport trade2 = Util.fromJsonString(jsonString, ExecutionReport.class);
        Assert.assertEquals(jsonString, trade2.toJsonString());

    }

    private OrderRequest createOrderRequest(double price, double quantity, Verb verb) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setOrderType(OrderType.Limit);
        orderRequest.setVerb(verb);
        orderRequest.setPrice(price);
        orderRequest.setQuantity(quantity);
        orderRequest.setInstrument(instrumentPk);
        orderRequest.setClientOrderId(UUID.randomUUID().toString());
        orderRequest.setAlgorithmInfo(Depth.ALGORITHM_INFO_MM);
        orderRequest.setOrderRequestAction(OrderRequestAction.Send);
        return orderRequest;

    }

    @Test
    public void orderRequestToFromJsonString() {
        OrderRequest orderRequest = createOrderRequest(100, 5, Verb.Buy);
        String jsonString = Util.toJsonString(orderRequest);
        System.out.println(jsonString);
        Assert.assertNotNull(jsonString);

        String oldJsonString = GSON.toJson(orderRequest);
        OrderRequest orderRequest1 = Util.fromJsonString(oldJsonString, OrderRequest.class);
        Assert.assertEquals(orderRequest1.toJsonString(), orderRequest.toJsonString());

//        Assert.assertEquals(jsonString,oldJsonString);

        OrderRequest trade2 = Util.fromJsonString(jsonString, OrderRequest.class);
        Assert.assertEquals(jsonString, trade2.toJsonString());

    }

}