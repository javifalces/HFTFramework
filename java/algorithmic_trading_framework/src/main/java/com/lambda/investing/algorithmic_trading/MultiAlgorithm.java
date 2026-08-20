package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.hedging.HedgeManager;
import com.lambda.investing.connector.disruptor.DisruptorConnectorHelper;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.OrderRequestAction;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class MultiAlgorithm extends Algorithm {

    private static final String COMMON_NOTIFIER_DISRUPTOR_NAME = "MultiAlgorithm_common_notifier_disruptor";

    @Getter
    private final List<Algorithm> algorithms;

    @Getter
    private final Map<String, List<Algorithm>> instrumentToAlgorithms = new HashMap<>();

    public static DisruptorConnectorHelper COMMON_ALGO_NOTIFIER_DISRUPTOR = null;



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

    public OrderRequest createActiveCancel(String origClientOrderId) {
        for (Algorithm algorithm : algorithms) {
            OrderRequest orderRequest = algorithm.createActiveCancel(origClientOrderId);
            if (orderRequest != null) {
                return orderRequest;
            }
        }
        return null;
    }

    public void sendOrderRequest(OrderRequest orderRequest) throws LambdaTradingException {
        if (orderRequest.getOrderRequestAction() == OrderRequestAction.Cancel) {
            for (Algorithm algorithm : algorithms) {
                try {
                    if (algorithm.containsOrder(orderRequest.getOrigClientOrderId())) {
                        algorithm.sendOrderRequest(orderRequest);
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("Error sending cancel order request " + orderRequest.getInstrument() + " to algorithm " + algorithm.getAlgorithmInfo());
                    logger.error("Error sending cancel order request {} to algorithm {}: {}", orderRequest.getInstrument(), algorithm.getAlgorithmInfo(), e.getMessage());
                }
            }
        } else {
            super.sendOrderRequest(orderRequest);
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
//        super.setHedgeManager(hedgeManager);
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

        DisruptorConnectorHelper sharedHelper = DisruptorConnectorHelper.getInstance(
                COMMON_NOTIFIER_DISRUPTOR_NAME,
                Configuration.ConnectorProviderType.DISRUPTOR_HIGH_THROUGHPUT
        );
        sharedHelper.init();
        COMMON_ALGO_NOTIFIER_DISRUPTOR = sharedHelper;

        for (Algorithm algorithm : algorithms) {
            algorithm.useSharedNotifierDisruptor(sharedHelper);
        }

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
//        super.register(algorithmObserver);//if multi is register this is going to republish everything twice
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
//        super.deregister(algorithmObserver);
        if (algorithms == null) return;
        for (Algorithm algorithm : algorithms) {
            algorithm.deregister(algorithmObserver);
        }
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        return true;
    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        return true;
    }

    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        return true;
    }

    @Override
    public void onCandleUpdate(Candle candle) {
    }

    @Override
    public boolean onCommandUpdate(Command command) {
        return true;
    }

    @Override
    public boolean onPosition(Map<String, Double> positions) {
        return true;
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
