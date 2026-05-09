package com.lambda.investing.algo_trading;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ZeroMqTradingConfiguration {

    private String tradeEngineHost;
    private String marketDataHost;
    private String factorPublisherHost;

    private int tradeEnginePort;
    private int marketDataPort;
    private int factorPublisherPort;

    private boolean paperTrading;
    private boolean demoTrading;
    private String[] instrumentPks;
    private AlgorithmConfiguration algorithm;
    private List<AlgorithmInstanceConfiguration> algorithms;

    public List<AlgorithmInstanceConfiguration> getEffectiveAlgorithms() {
        if (algorithms != null && !algorithms.isEmpty()) {
            return algorithms;
        }
        if (algorithm == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new AlgorithmInstanceConfiguration(instrumentPks, algorithm));
    }

}
