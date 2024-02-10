package com.lambda.investing.factor_investing_connector;

import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MockFactorProvider extends AbstractFactorProvider implements MarketDataListener {

    private MarketDataProvider marketDataProvider;
    private long updateMs;
    private List<String> instrumentPks;

    private long currentTimeStamp;
    public static Random RANDOM_GENERATOR = new Random();

    private Map<String, Double> lastFactorSent = new HashMap<>();

    public MockFactorProvider(MarketDataProvider marketDataProvider, long updateMs, List<String> instrumentPks) {
        this.marketDataProvider = marketDataProvider;
        this.updateMs = updateMs;
        this.instrumentPks = instrumentPks;
        this.marketDataProvider.register(this);
    }

    private Map<String, Double> NormalizeFactors(Map<String, Double> input) {
        //
        Map<String, Double> output = new HashMap<>();

        double positiveSum = 0.0;
        double negativeSum = 0.0;
        for (Double weight : input.values()) {
            if (weight >= 0) {
                positiveSum += weight;
            } else {
                negativeSum += Math.abs(weight);
            }
        }
        for (String instrumentPk : input.keySet()) {
            double weight = input.get(instrumentPk);
            double normalizeWeight = weight >= 0 ? weight / positiveSum : weight / negativeSum;
            output.put(instrumentPk, normalizeWeight);
        }
        return output;
    }

    private void notifyFactor(long timeStamp) {
        if (currentTimeStamp != 0 && timeStamp - currentTimeStamp > this.updateMs) {
            //
            Map<String, Double> factorSent = new HashMap<>();
            for (String instrumentPk : instrumentPks) {
                double lastWeight = 1.0;
                if (lastFactorSent.containsKey(instrumentPk)) {
                    lastWeight = lastFactorSent.get(instrumentPk);
                }
                double newWeight = lastWeight * (RANDOM_GENERATOR.nextDouble() - 0.5);
                factorSent.put(instrumentPk, newWeight);
            }
            lastFactorSent = NormalizeFactors(factorSent);
            notifyFactor(timeStamp, lastFactorSent);
            currentTimeStamp = timeStamp;
        }
        if (currentTimeStamp == 0) {
            currentTimeStamp = timeStamp;
        }

    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        notifyFactor(depth.getTimestamp());
        return false;
    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        notifyFactor(trade.getTimestamp());
        return false;
    }

    @Override
    public boolean onCommandUpdate(Command command) {
        return false;
    }

    @Override
    public boolean onInfoUpdate(String header, String message) {
        return false;
    }

//    @Override
//    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
//        return false;
//    }
//

}
