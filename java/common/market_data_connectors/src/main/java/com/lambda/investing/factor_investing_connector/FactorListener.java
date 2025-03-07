package com.lambda.investing.factor_investing_connector;

import com.lambda.investing.model.market_data.Depth;

import java.util.Map;

public interface FactorListener {
    boolean onWeightsUpdate(long timestamp, Map<String, Double> instrumentPkWeights);
}
