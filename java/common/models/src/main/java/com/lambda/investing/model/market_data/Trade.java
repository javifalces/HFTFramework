package com.lambda.investing.model.market_data;

import com.alibaba.fastjson2.annotation.JSONField;
import com.lambda.investing.model.CSVable;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import static com.lambda.investing.model.Util.*;

@Getter
@Setter
public class Trade extends CSVable implements Cloneable {
    public static double DEFAULT_VALUE = Double.NaN;
    private String id;
    private String instrument;

    private long timestamp;//from exchange if possible
    private long timestampBrokerConnector;// set AbstractMarketDataConnectorPublisher.notifyTrade just before publish
    private long timestampAlgoConnector;//set in ZeroMqMarketDataConnector.onUpdate // OrdinaryMarketDataProvider.onUpdate
    private long timestampStrategy;//set in Algorithm.OnTradeUpdate when received

    private double quantity, price = DEFAULT_VALUE;
    private String algorithmInfo;//just for backtesting
    private Verb verb;//a buyer here is lifting the ask , a seller is hitting the bid
    private long timeToNextUpdateMs = Long.MIN_VALUE;
    private static TradePool TRADE_POOL = new TradePool();

    @JSONField(serialize = false, deserialize = false)
    public String getLatenciesTable() {
        return getLatenciesTable(getTimestamp(), getTimestampBrokerConnector(), getTimestampAlgoConnector(), getTimestampStrategy());
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    public static Trade getInstance() {
        Trade trade = new Trade();
        trade.id = generateId();
        return trade;
    }

    public static Trade getInstancePool() {
        Trade trade = TRADE_POOL.checkOut();
        trade.reset();
        trade.id = generateId();
        return trade;
    }

    public static Trade copyFrom(Trade trade) {
        Trade newTrade = getInstancePool();
        newTrade.id = trade.id;
        newTrade.instrument = trade.instrument;
        newTrade.quantity = trade.quantity;
        newTrade.price = trade.price;
        newTrade.algorithmInfo = trade.algorithmInfo;
        newTrade.verb = trade.verb;
        newTrade.timeToNextUpdateMs = trade.timeToNextUpdateMs;

        newTrade.timestamp = trade.timestamp;
        newTrade.timestampStrategy = trade.timestampStrategy;
        newTrade.timestampBrokerConnector = trade.timestampBrokerConnector;
        newTrade.timestampAlgoConnector = trade.timestampAlgoConnector;
        return newTrade;
    }

    public void delete() {
        delete(-1);
    }

    public void delete(int milliseconds) {
        if (milliseconds > 0) {
            TRADE_POOL.lazyCheckIn(this, milliseconds);
        } else {
            TRADE_POOL.checkIn(this);
        }
    }

    public Verb inferVerb(Depth depth) {
        double bestBid = depth.getBestBid();
        double bestAsk = depth.getBestAsk();
        if (Double.isNaN(bestBid) || Double.isNaN(bestAsk) || bestBid <= 0 || bestAsk <= 0) {
            return null;
        }
        double mid = (bestBid + bestAsk) / 2.0;
        if (getPrice() < mid) {
            return Verb.Sell; // trade crossed the spread on the sell side
        } else if (getPrice() > mid) {
            return Verb.Buy; // trade crossed the spread on the buy side
        } else {
            return null; // price exactly at mid — direction unknown
        }
    }

    public static String logPool() {
        return TRADE_POOL.toString();
    }

    private void reset() {
        id = null;
        instrument = null;
        quantity = DEFAULT_VALUE;
        price = DEFAULT_VALUE;
        algorithmInfo = null;
        verb = null;
        timeToNextUpdateMs = Long.MIN_VALUE;
        timestamp = 0;
        timestampStrategy = 0;
        timestampBrokerConnector = 0;
        timestampAlgoConnector = 0;
    }

    private Trade() {

    }

    public void setTradeFromParquet(TradeParquet tradeParquet, Instrument instrument) {
        this.instrument = instrument.getPrimaryKey();
        this.timestamp = tradeParquet.getTimestamp();
        this.quantity = tradeParquet.getQuantity();
        this.price = tradeParquet.getPrice();
        this.id = generateId();

    }

    public void setTradeFromExecutionReport(ExecutionReport executionReport) {
        this.instrument = executionReport.getInstrument();
        this.timestamp = executionReport.getTimestampCreation();
        this.quantity = executionReport.getLastQuantity();
        this.price = executionReport.getPrice();
        this.algorithmInfo = executionReport.getAlgorithmInfo();
        this.verb = Verb.OtherSideVerb(executionReport.getVerb());//our buy , is a sell for the market
        id = generateId();

    }

    public boolean isTradeValid(Depth lastDepth) {
        boolean priceIsInBounds = true;
        if (lastDepth != null) {
            try {
                Double worstAsk = lastDepth.getWorstAsk();
                Double worstBid = lastDepth.getWorstBid();
                if (worstAsk != Double.MAX_VALUE && worstBid != Double.MIN_VALUE) {
                    priceIsInBounds = this.price < worstAsk && this.price > worstBid;
                }
            } catch (Exception e) {
                priceIsInBounds = true;
            }
        }
        return priceIsInBounds;
    }

    @Override
    public String toString() {
        if (price == DEFAULT_VALUE) {
            return "trade is empty";
        }
        return toJsonString(this);
    }

    public static StringBuilder headerCSV() {
        //,price,quantity
        StringBuilder stringBuffer = new StringBuilder();
        return stringBuffer.append(",timestamp,price,quantity");
    }

    public String toCSV(boolean withHeader) {
        StringBuilder stringBuffer = new StringBuilder();
        if (withHeader) {
            //,price,quantity
            stringBuffer.append(headerCSV());
            stringBuffer.append(System.lineSeparator());
        }
        //2019-11-09 08:42:24.142302
        stringBuffer.append(getDatePythonUTC(timestamp));
        stringBuffer.append(",");
        stringBuffer.append(timestamp);
        stringBuffer.append(",");
        stringBuffer.append(price);
        stringBuffer.append(",");
        stringBuffer.append(quantity);
        return stringBuffer.toString();
    }

    @JSONField(serialize = false, deserialize = false)
    @Override
    public Object getParquetObject() {
        return new TradeParquet(this);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static class TradePool extends ObjectPool<Trade> {
        @Override
        protected Trade create() {
            return new Trade();
        }
    }
}
