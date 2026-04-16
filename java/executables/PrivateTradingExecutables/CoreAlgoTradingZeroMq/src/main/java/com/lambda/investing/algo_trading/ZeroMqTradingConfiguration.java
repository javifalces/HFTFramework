package com.lambda.investing.algo_trading;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    /**
     * Optional: if set, overrides the {@code prometeus.port} system property.
     */
    private String prometheusPort;

    /**
     * Optional: if set, overrides the {@code loki.host} system property.
     */
    private String lokiHost;

    /**
     * Optional: if set, overrides the {@code loki.port} system property.
     */
    private Integer lokiPort;

    private AlgorithmConfiguration algorithm;

}
