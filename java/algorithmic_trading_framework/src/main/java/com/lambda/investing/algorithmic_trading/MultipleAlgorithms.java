package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.algorithmic_trading.hedging.HedgeManager;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class MultipleAlgorithms extends Algorithm {

    @Getter
    private final List<Algorithm> algorithmsList = new ArrayList<>();
    private final Map<String, Algorithm> algorithmInfoMap = new HashMap<>();

    public MultipleAlgorithms(String algorithmInfo, List<Algorithm> algorithmsList) {
        super(algorithmInfo, new HashMap<>());
        if (algorithmsList != null) {
            for (Algorithm algorithm : algorithmsList) {
                addAlgorithm(algorithm);
            }
        }
    }

    private void addAlgorithm(Algorithm algorithm) {
        if (algorithm == null) {
            return;
        }
        algorithmsList.add(algorithm);
        algorithmInfoMap.put(algorithm.getAlgorithmInfo(), algorithm);
        this.instruments.addAll(algorithm.getInstruments());
    }

    @Override
    public void setAlgorithmConnectorConfiguration(AlgorithmConnectorConfiguration algorithmConnectorConfiguration) {
        super.setAlgorithmConnectorConfiguration(algorithmConnectorConfiguration);
        for (Algorithm algorithm : algorithmsList) {
            algorithm.setAlgorithmConnectorConfiguration(algorithmConnectorConfiguration);
        }
    }

    @Override
    public void init() {
        super.init();
        for (Algorithm algorithm : algorithmsList) {
            algorithm.init();
            algorithm.setExitOnStop(false);
        }
    }

    @Override
    public void start() {
        super.start();
        for (Algorithm algorithm : algorithmsList) {
            algorithm.start();
        }
    }

    @Override
    public void stop() {
        for (Algorithm algorithm : algorithmsList) {
            algorithm.stop();
        }
        super.stop();
    }

    @Override
    public void setHedgeManager(HedgeManager hedgeManager) {
        super.setHedgeManager(hedgeManager);
        for (Algorithm algorithm : algorithmsList) {
            algorithm.setHedgeManager(hedgeManager);
        }
    }

    @Override
    public String printAlgo() {
        String algorithms = algorithmsList.stream().map(Algorithm::getAlgorithmInfo).collect(Collectors.joining(", "));
        return String.format("MultipleAlgorithms[%d]: %s", algorithmsList.size(), algorithms);
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        boolean result = super.onDepthUpdate(depth);
        for (Algorithm algorithm : algorithmsList) {
            if (algorithm.getInstruments().isEmpty() || containsInstrument(algorithm, depth.getInstrument())) {
                result = algorithm.onDepthUpdate(depth) || result;
            }
        }
        return result;
    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        boolean result = super.onTradeUpdate(trade);
        for (Algorithm algorithm : algorithmsList) {
            if (algorithm.getInstruments().isEmpty() || containsInstrument(algorithm, trade.getInstrument())) {
                result = algorithm.onTradeUpdate(trade) || result;
            }
        }
        return result;
    }

    @Override
    public void onCandleUpdate(Candle candle) {
        for (Algorithm algorithm : algorithmsList) {
            if (algorithm.getInstruments().isEmpty() || containsInstrument(algorithm, candle.getInstrumentPk())) {
                algorithm.onCandleUpdate(candle);
            }
        }
    }

    @Override
    public boolean onCommandUpdate(Command command) {
        boolean result = super.onCommandUpdate(command);
        for (Algorithm algorithm : algorithmsList) {
            result = algorithm.onCommandUpdate(command) || result;
        }
        return result;
    }

    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        Algorithm targetAlgorithm = algorithmInfoMap.get(executionReport.getAlgorithmInfo());
        if (targetAlgorithm != null) {
            return targetAlgorithm.onExecutionReportUpdate(executionReport);
        }
        boolean result = false;
        for (Algorithm algorithm : algorithmsList) {
            result = algorithm.onExecutionReportUpdate(executionReport) || result;
        }
        return result;
    }

    private boolean containsInstrument(Algorithm algorithm, String instrumentPk) {
        Set<Instrument> algorithmInstruments = algorithm.getInstruments();
        for (Instrument instrument : algorithmInstruments) {
            if (instrument != null && instrument.getPrimaryKey().equals(instrumentPk)) {
                return true;
            }
        }
        return false;
    }
}
