package com.lambda.investing.algo_trading.core;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.hedging.HedgeManager;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;

import java.util.*;
import java.util.stream.Collectors;

public class MultiAlgorithm extends Algorithm {

    private final List<Algorithm> algorithms;
    private final Map<String, List<Algorithm>> instrumentToAlgorithms = new HashMap<>();

    public MultiAlgorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, List<Algorithm> algorithms) {
        super(algorithmConnectorConfiguration, "MultiAlgorithm", new HashMap<>());
        this.algorithms = new ArrayList<>(algorithms);
        rebuildInstrumentCache();
    }

    private void rebuildInstrumentCache() {
        instrumentToAlgorithms.clear();
        instruments.clear();
        for (Algorithm algorithm : algorithms) {
            for (Instrument instrument : algorithm.getInstruments()) {
                if (instrument == null) {
                    continue;
                }
                instruments.add(instrument);
                instrumentToAlgorithms.computeIfAbsent(instrument.getPrimaryKey(), key -> new ArrayList<>()).add(algorithm);
            }
        }
    }

    @Override
    public void setAlgorithmConnectorConfiguration(AlgorithmConnectorConfiguration algorithmConnectorConfiguration) {
        super.setAlgorithmConnectorConfiguration(algorithmConnectorConfiguration);
        for (Algorithm algorithm : algorithms) {
            algorithm.setAlgorithmConnectorConfiguration(algorithmConnectorConfiguration);
        }
    }

    @Override
    public void setHedgeManager(HedgeManager hedgeManager) {
        super.setHedgeManager(hedgeManager);
        for (Algorithm algorithm : algorithms) {
            algorithm.setHedgeManager(hedgeManager);
        }
    }

    @Override
    public void init() {
        super.init();
        for (Algorithm algorithm : algorithms) {
            algorithm.setExitOnStop(false);
            algorithm.init();
        }
        rebuildInstrumentCache();
    }

    @Override
    public void start() {
        super.start();
        for (Algorithm algorithm : algorithms) {
            algorithm.start();
        }
    }

    @Override
    public void stop() {
        for (Algorithm algorithm : algorithms) {
            algorithm.stop();
        }
        super.stop();
    }

    @Override
    public String printAlgo() {
        return "MultiAlgorithm[" + algorithms.stream().map(Algorithm::getAlgorithmInfo).collect(Collectors.joining(",")) + "]";
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        boolean parentResult = super.onDepthUpdate(depth);
        boolean childResult = false;
        for (Algorithm algorithm : instrumentToAlgorithms.getOrDefault(depth.getInstrument(), Collections.emptyList())) {
            childResult = algorithm.onDepthUpdate(depth) || childResult;
        }
        return parentResult || childResult;
    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        boolean parentResult = super.onTradeUpdate(trade);
        boolean childResult = false;
        for (Algorithm algorithm : instrumentToAlgorithms.getOrDefault(trade.getInstrument(), Collections.emptyList())) {
            childResult = algorithm.onTradeUpdate(trade) || childResult;
        }
        return parentResult || childResult;
    }

    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        boolean result = false;
        for (Algorithm algorithm : algorithms) {
            if (executionReport.getAlgorithmInfo().equals(algorithm.getAlgorithmInfo())) {
                result = algorithm.onExecutionReportUpdate(executionReport) || result;
            }
        }
        return result;
    }

    @Override
    public void onCandleUpdate(Candle candle) {
        for (Algorithm algorithm : instrumentToAlgorithms.getOrDefault(candle.getInstrumentPk(), Collections.emptyList())) {
            algorithm.onCandleUpdate(candle);
        }
    }

    @Override
    public boolean onCommandUpdate(Command command) {
        boolean result = super.onCommandUpdate(command);
        for (Algorithm algorithm : algorithms) {
            result = algorithm.onCommandUpdate(command) || result;
        }
        return result;
    }
}
