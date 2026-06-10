package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.algorithmic_trading.hedging.HedgeManager;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class MultiAlgorithm extends Algorithm {

    @Getter
    private final List<Algorithm> algorithms;

    @Getter
    private final Map<String, List<Algorithm>> instrumentToAlgorithms = new HashMap<>();

    public MultiAlgorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, List<Algorithm> algorithms) {
        super(algorithmConnectorConfiguration, "MultiAlgorithm", new HashMap<>());
        this.algorithms = new ArrayList<>(algorithms);
        rebuildInstrumentCache();
        propagateConstructorObservers();
    }

    public MultiAlgorithm(String algorithmInfo, List<Algorithm> algorithms) {
        super(algorithmInfo, new HashMap<>());
        this.algorithms = new ArrayList<>(algorithms);
        rebuildInstrumentCache();
        propagateConstructorObservers();
    }

    /**
     * Propagates observers that were registered during {@code super()} (before
     * {@link #algorithms} was assigned) to every child algorithm.
     * Called once at the end of each constructor, after {@code this.algorithms} is ready.
     */
    private void propagateConstructorObservers() {
        for (AlgorithmObserver obs : getAlgorithmObservers()) {
            for (Algorithm algorithm : algorithms) {
                algorithm.register(obs);
            }
        }
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

        // Deregister from parent connectors to avoid duplicate registrations from child algorithms
        this.algorithmConnectorConfiguration.getTradingEngineConnector().deregister(this.algorithmInfo, this);
        this.algorithmConnectorConfiguration.getMarketDataProvider().deregister(this);

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

    /**
     * Registers the observer on this MultiAlgorithm AND on every child algorithm so that
     * updates fired by each child's own {@link AlgorithmNotifier} are delivered to the observer.
     * Without this override only the MultiAlgorithm's (silent) notifier would be targeted and
     * the observer would never receive any data while algorithms are operating normally.
     */
    @Override
    public void register(AlgorithmObserver algorithmObserver) {
        super.register(algorithmObserver);
        // algorithms is null during super() constructor chain – skip propagation in that case;
        // the real registrations happen after construction is complete.
        if (algorithms == null) return;
        for (Algorithm algorithm : algorithms) {
            algorithm.register(algorithmObserver);
        }
    }

    /**
     * Deregisters the observer from this MultiAlgorithm AND from every child algorithm.
     */
    @Override
    public void deregister(AlgorithmObserver algorithmObserver) {
        super.deregister(algorithmObserver);
        if (algorithms == null) return;
        for (Algorithm algorithm : algorithms) {
            algorithm.deregister(algorithmObserver);
        }
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        return true;
//        boolean parentResult = super.onDepthUpdate(depth);
//        boolean childResult = false;
//        for (Algorithm algorithm : instrumentToAlgorithms.getOrDefault(depth.getInstrument(), Collections.emptyList())) {
//            childResult = algorithm.onDepthUpdate(depth) || childResult;
//        }
//        return parentResult || childResult;
    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        return true;
//        boolean parentResult = super.onTradeUpdate(trade);
//        boolean childResult = false;
//        for (Algorithm algorithm : instrumentToAlgorithms.getOrDefault(trade.getInstrument(), Collections.emptyList())) {
//            childResult = algorithm.onTradeUpdate(trade) || childResult;
//        }
//        return parentResult || childResult;
    }

    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        return true;
//        boolean result = false;
//        for (Algorithm algorithm : algorithms) {
//            if (Objects.equals(executionReport.getAlgorithmInfo(), algorithm.getAlgorithmInfo())) {
//                result = algorithm.onExecutionReportUpdate(executionReport) || result;
//            }
//        }
//        return result;
    }

    @Override
    public void onCandleUpdate(Candle candle) {
        return;
//        for (Algorithm algorithm : instrumentToAlgorithms.getOrDefault(candle.getInstrumentPk(), Collections.emptyList())) {
//            algorithm.onCandleUpdate(candle);
//        }
    }

    @Override
    public boolean onCommandUpdate(Command command) {
        return true;
//        boolean result = super.onCommandUpdate(command);
//        for (Algorithm algorithm : algorithms) {
//            result = algorithm.onCommandUpdate(command) || result;
//        }
//        return result;
    }

    public void manualStop() {
        if (algorithms == null) return;
        for (Algorithm algorithm : algorithms) {
            algorithm.manualStop();
        }
    }

    public void manualStart() {
        if (algorithms == null) return;
        for (Algorithm algorithm : algorithms) {
            algorithm.manualStart();
        }
    }

}
