package com.lambda.investing.model.trading;

import com.alibaba.fastjson2.annotation.JSONField;
import com.lambda.investing.model.Util;
import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import static com.lambda.investing.PrintUtils.PrintDate;

@Getter
@Setter
public class OrderRequest implements Cloneable, Serializable {

    public static double NOT_SET_PRICE_VALUE = Double.MIN_VALUE;
    private String instrument;
    private OrderRequestAction orderRequestAction;
    private double price = NOT_SET_PRICE_VALUE;
    private double quantity;//todo change to bigdecimal or integer
    private Verb verb;
    private OrderType orderType;
    private MarketOrderType marketOrderType;
    private String clientOrderId, origClientOrderId;


    private String algorithmInfo;
    private String freeText;
    public static Random RANDOM_GENERATOR = new Random();

    private long referenceTimestamp;//strategy reference time , last depth received
    private long timestampCreation;//strategy create the object
    private long timestampAlgoConnector;//Ordinary Trading Engine receive the order request from strategy
    private long timestampBrokerConnector; //Abstract BrokerConnector receive the order request and send to each broker implementation


    public Object clone() {
        OrderRequest orderRequest;
        try {
            orderRequest = (OrderRequest) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
        return orderRequest;
    }


    @Override
    public String toString() {
        if (orderRequestAction.equals(OrderRequestAction.Send) && quantity > 0) {
            String output = String
                    .format("Send %s %s %s ->%s [%s]%.4f@%f", orderType.toString(), instrument, algorithmInfo, verb, clientOrderId, quantity,
                            price);
            return output;
        } else if (orderRequestAction.equals(OrderRequestAction.Modify) && quantity > 0) {
            String output = String
                    .format("Modification %s %s %s->%s [%s]%.4f@%f", instrument, algorithmInfo, origClientOrderId, verb,
                            clientOrderId, quantity, price);
            return output;
        } else if (quantity == 0 || orderRequestAction.equals(OrderRequestAction.Cancel)) {
            String output = String
                    .format("Cancel %s %s %s->%s cancel", instrument, algorithmInfo, origClientOrderId, clientOrderId);
            return output;
        } else {
            return "uknown action " + orderRequestAction + " " + clientOrderId + " " + super.toString();
        }

    }

    public String toJsonString() {
        return Util.toJsonString(this);
    }

    protected static String generateClientOrderId() {
        byte[] dataInput = new byte[10];
        RANDOM_GENERATOR.nextBytes(dataInput);
        return UUID.nameUUIDFromBytes(dataInput).toString();
    }

    public static OrderRequest createMarketOrderRequest(long timestamp, String algorithmInfo, Instrument instrument, Verb verb, double quantity) {
        ///controled market request
        String newClientOrderId = OrderRequest.generateClientOrderId();
        OrderRequest output = new OrderRequest();
        output.setAlgorithmInfo(algorithmInfo);
        output.setInstrument(instrument.getPrimaryKey());
        output.setVerb(verb);
        output.setOrderRequestAction(OrderRequestAction.Send);
        output.setClientOrderId(newClientOrderId);
        output.setQuantity(quantity);
        //		Depth lastDepth = getLastDepth(instrument);
        //		if (verb.equals(Verb.Sell)){
        //			output.setPrice(lastDepth.getBestBid()-lastDepth.getSpread());
        //		}
        //		if (verb.equals(Verb.Buy)){
        //			output.setPrice(lastDepth.getBestAsk()+lastDepth.getSpread());
        //		}

        output.setTimestampCreation(timestamp);
        output.setOrderType(OrderType.Market);//limit for quoting
        output.setMarketOrderType(MarketOrderType.FAS);//default FAS
        return output;
    }

    public static OrderRequest createLimitOrderRequest(long timestamp, String algorithmInfo, Instrument instrument, Verb verb, double quantity, double price) {
        ///controled market request
        String newClientOrderId = OrderRequest.generateClientOrderId();
        OrderRequest output = new OrderRequest();
        output.setAlgorithmInfo(algorithmInfo);
        output.setInstrument(instrument.getPrimaryKey());
        output.setVerb(verb);
        output.setOrderRequestAction(OrderRequestAction.Send);
        output.setClientOrderId(newClientOrderId);
        output.setQuantity(quantity);
        //		Depth lastDepth = getLastDepth(instrument);
        //		if (verb.equals(Verb.Sell)){
        //			output.setPrice(lastDepth.getBestBid()-lastDepth.getSpread());
        //		}
        //		if (verb.equals(Verb.Buy)){
        //			output.setPrice(lastDepth.getBestAsk()+lastDepth.getSpread());
        //		}
        output.setPrice(price);
        output.setTimestampCreation(timestamp);
        output.setOrderType(OrderType.Limit);//limit for quoting
        output.setMarketOrderType(MarketOrderType.FAS);//default FAS
        return output;
    }

    public static OrderRequest createCancel(long timestamp, String algorithmInfo, Instrument instrument, String origClientOrderId) {
        OrderRequest cancelOrderRequest = new OrderRequest();
        cancelOrderRequest.setOrderRequestAction(OrderRequestAction.Cancel);
        cancelOrderRequest.setOrigClientOrderId(origClientOrderId);
        cancelOrderRequest.setAlgorithmInfo(algorithmInfo);
        cancelOrderRequest.setClientOrderId(generateClientOrderId());
        cancelOrderRequest.setInstrument(instrument.getPrimaryKey());
        cancelOrderRequest.setTimestampCreation(timestamp);
        return cancelOrderRequest;
    }

    public static OrderRequest modifyOrder(long timestamp, String algorithmInfo, Instrument instrument, Verb verb, double quantity, double price, String origClientOrderId) {
        OrderRequest modifyOrderRequest = new OrderRequest();
        modifyOrderRequest.setOrderRequestAction(OrderRequestAction.Modify);
        modifyOrderRequest.setOrigClientOrderId(origClientOrderId);
        modifyOrderRequest.setAlgorithmInfo(algorithmInfo);
        modifyOrderRequest.setClientOrderId(generateClientOrderId());
        modifyOrderRequest.setInstrument(instrument.getPrimaryKey());
        modifyOrderRequest.setTimestampCreation(timestamp);
        modifyOrderRequest.setVerb(verb);
        modifyOrderRequest.setQuantity(quantity);
        modifyOrderRequest.setPrice(price);
        modifyOrderRequest.setOrderType(OrderType.Limit);//limit for quoting

        return modifyOrderRequest;
    }

    @JSONField(serialize = false, deserialize = false)
    public String getLatenciesTable() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-30s %-30s %-20s\n", "Event", "Timestamp", "Latency (ms)"));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("%-30s %-30s %-20d\n", "referenceTimestamp", PrintDate(new Date(getReferenceTimestamp())), 0));
        sb.append(String.format("%-30s %-30s %-20d\n", "creationTimestamp", PrintDate(new Date(getTimestampCreation())), getTimestampCreation() - getReferenceTimestamp()));
        long lastReference = getTimestampCreation();
        if (getTimestampBrokerConnector() > 0) {
            sb.append(String.format("%-30s %-30s %-20d\n", "timestampBrokerConnector", PrintDate(new Date(getTimestampBrokerConnector())), getTimestampBrokerConnector() - lastReference));
            lastReference = getTimestampBrokerConnector();
        }
        if (getTimestampAlgoConnector() > 0) {
            sb.append(String.format("%-30s %-30s %-20d\n", "timestampAlgoConnector", PrintDate(new Date(getTimestampAlgoConnector())), getTimestampAlgoConnector() - lastReference));
            lastReference = getTimestampAlgoConnector();
        }
        sb.append(String.format("%-30s %-30s %-20d\n", "now", PrintDate(new Date()), System.currentTimeMillis() - lastReference));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("%-30s %-30s %-20d\n", "Total", "", System.currentTimeMillis() - getReferenceTimestamp()));
        return sb.toString();
    }


}
