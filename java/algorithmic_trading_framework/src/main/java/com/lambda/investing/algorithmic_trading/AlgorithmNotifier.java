package com.lambda.investing.algorithmic_trading;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

public class AlgorithmNotifier {

    ThreadFactory namedThreadFactory;
    private ThreadPoolExecutor notifierPool;
    private int threadsNotifier;
    private String algorithmInfo;
    private Algorithm algorithm;

    private Map<String, Object> lastParams = new HashMap<>();
    private boolean firstParams = true;
    public AlgorithmNotifier(Algorithm algorithm, int threadsNotifier) {
        this.threadsNotifier = threadsNotifier;
        this.algorithmInfo = algorithm.algorithmInfo;
        this.algorithm = algorithm;
        if (isMultithreaded()) {
            ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
            threadFactoryBuilder.setNameFormat(this.algorithmInfo + "_notifier" + "-%d");
            threadFactoryBuilder.setPriority(Thread.MIN_PRIORITY);
            namedThreadFactory = threadFactoryBuilder.build();
            notifierPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.threadsNotifier, namedThreadFactory);
        }
    }

    private boolean isMultithreaded() {
        return this.threadsNotifier > 0;
    }

    public void setAlgorithmInfo(String algorithmInfo) {
        this.algorithmInfo = algorithmInfo;
    }

    //trades
    private void _notifyObserversOnUpdatePnlSnapshot(PnlSnapshot pnlSnapshot) {
        for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
            algorithmObserver.onUpdatePnlSnapshot(this.algorithmInfo, pnlSnapshot);
        }
    }

    public void notifyObserversOnUpdatePnlSnapshot(PnlSnapshot pnlSnapshot) {
        if (algorithm.getAlgorithmObservers().size() > 0) {
            if (isMultithreaded()) {
                notifierPool.submit(() -> {
                    _notifyObserversOnUpdatePnlSnapshot(pnlSnapshot);
                });
            } else {
                _notifyObserversOnUpdatePnlSnapshot(pnlSnapshot);
            }
        }
    }

    public void notifyObserversOnUpdateDepth(Depth depth) {
        if (algorithm.getAlgorithmObservers().size() > 0) {
            if (isMultithreaded()) {
                notifierPool.submit(() -> {
                    _notifyObserversOnUpdateDepth(depth);
                });
            } else {
                _notifyObserversOnUpdateDepth(depth);
            }
        }
    }


    //depth
    private void _notifyObserversOnUpdateDepth(Depth depth) {
        for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
            algorithmObserver.onUpdateDepth(this.algorithmInfo, depth);
        }
    }


    //lastClose
    private void _notifyObserversOnUpdatePnlSnapshot(Trade trade) {
        for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
            algorithmObserver.onUpdateTrade(this.algorithmInfo, trade);
        }
    }

    public void notifyObserversOnUpdatePnlSnapshot(Trade trade) {
        if (algorithm.getAlgorithmObservers().size() > 0) {
            if (isMultithreaded()) {
                notifierPool.submit(() -> {
                    _notifyObserversOnUpdatePnlSnapshot(trade);
                });
            } else {
                _notifyObserversOnUpdatePnlSnapshot(trade);
            }
        }
    }

    //params
    private void _notifyObserversOnUpdateParams(Map<String, Object> params) {
        for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
            algorithmObserver.onUpdateParams(this.algorithmInfo, params);
        }
    }

    public void notifyObserversOnUpdateParams(Map<String, Object> params) {
        if (!firstParams && lastParams.equals(params)) {
            return;
        }
        firstParams = false;

        if (algorithm.getAlgorithmObservers().size() > 0) {
            if (isMultithreaded()) {
                notifierPool.submit(() -> {
                    _notifyObserversOnUpdateParams(params);
                });
            } else {
                _notifyObserversOnUpdateParams(params);
            }
        }
        lastParams = params;

    }

    private void _notifyObserversCustomColumns(long timestamp, String instrumentPk, String key, Double value) {
        for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
            algorithmObserver.onCustomColumns(timestamp, this.algorithmInfo, instrumentPk, key, value);
        }
    }

    public void notifyObserversCustomColumns(long timestamp, String instrumentPk, String key, Double value) {
        if (algorithm.getAlgorithmObservers().size() > 0) {
            if (isMultithreaded()) {
                notifierPool.submit(() -> {
                    _notifyObserversCustomColumns(timestamp, instrumentPk, key, value);
                });
            } else {
                _notifyObserversCustomColumns(timestamp, instrumentPk, key, value);
            }
        }
    }

    public void notifyLastParams() {
        if (lastParams.size() > 0) {
            firstParams = true;
            notifyObserversOnUpdateParams(lastParams);
        }
    }

    //message
    private void _notifyObserversOnUpdateMessage(String name, String body) {
        for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
            algorithmObserver.onUpdateMessage(this.algorithmInfo, name, body);
        }
    }

    public void notifyObserversOnUpdateMessage(String name, String body) {
        if (algorithm.getAlgorithmObservers().size() > 0) {
            if (isMultithreaded()) {
                notifierPool.submit(() -> {
                    _notifyObserversOnUpdateMessage(name, body);
                });
            } else {
                _notifyObserversOnUpdateMessage(name, body);
            }
        }
    }

    public void notifyObserversOnOrderRequest(OrderRequest orderRequest) {
        if (algorithm.getAlgorithmObservers().size() > 0) {
            if (isMultithreaded()) {
                notifierPool.submit(() -> {
                    _notifyObserversOnOrderRequest(orderRequest);
                });
            } else {
                _notifyObserversOnOrderRequest(orderRequest);
            }
        }
    }

    private void _notifyObserversOnOrderRequest(OrderRequest orderRequest) {
        for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
            algorithmObserver.onOrderRequest(this.algorithmInfo, orderRequest);
        }
    }

    public void notifyObserversonExecutionReportUpdate(ExecutionReport executionReport) {
        if (algorithm.getAlgorithmObservers().size() > 0) {
            if (isMultithreaded()) {
                notifierPool.submit(() -> {
                    _notifyObserversonExecutionReportUpdate(executionReport);
                });
            } else {
                _notifyObserversonExecutionReportUpdate(executionReport);
            }
        }
    }

    private void _notifyObserversonExecutionReportUpdate(ExecutionReport executionReport) {
        for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
            algorithmObserver.onExecutionReportUpdate(this.algorithmInfo, executionReport);
        }
    }
}
