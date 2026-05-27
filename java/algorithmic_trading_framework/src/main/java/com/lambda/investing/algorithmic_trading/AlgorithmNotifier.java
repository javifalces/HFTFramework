package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.LambdaThreadFactory;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PortfolioSnapshot;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Asynchronous notifier for algorithm observers (UI / monitoring).
 * <p>
 * Design goals for HFT hot-path safety:
 * - The caller (trading engine) NEVER blocks — all notifications are fire-and-forget.
 * - A bounded queue with DiscardPolicy ensures the queue cannot grow unboundedly;
 * when it is full the notification is silently dropped. UI lag is acceptable;
 * trading latency is not.
 * - execute() is used instead of submit() to avoid Future allocation on every call.
 * - The background thread runs at MIN_PRIORITY so the OS scheduler favours
 * trading threads over UI notifications.
 */
public class AlgorithmNotifier {

    /** Max pending UI notifications. Excess tasks are silently dropped. */
    private static final int NOTIFIER_QUEUE_CAPACITY = 1024;

    private final ThreadPoolExecutor notifierPool;
    private volatile String algorithmInfo;
    private final Algorithm algorithm;

    private Map<String, Object> lastParams = new HashMap<>();
    private boolean firstParams = true;

    public AlgorithmNotifier(Algorithm algorithm, int threadsNotifier) {
        this.algorithmInfo = algorithm.algorithmInfo;
        this.algorithm = algorithm;

        // Always async — at least one background thread so the hot path is never blocked.
        int threads = Math.max(1, threadsNotifier);
        ThreadFactory namedThreadFactory = LambdaThreadFactory.createThreadFactory(
                this.algorithmInfo + "_notifier", Thread.MIN_PRIORITY);

        // Bounded queue: when full, DiscardPolicy silently drops the task.
        BlockingQueue<Runnable> boundedQueue = new ArrayBlockingQueue<>(NOTIFIER_QUEUE_CAPACITY);
        this.notifierPool = new ThreadPoolExecutor(
                threads, threads,
                0L, TimeUnit.MILLISECONDS,
                boundedQueue,
                namedThreadFactory,
                new ThreadPoolExecutor.DiscardPolicy()   // drop UI update, never block caller
        );
    }

    public void setAlgorithmInfo(String algorithmInfo) {
        this.algorithmInfo = algorithmInfo;
    }

    /** Non-blocking fire-and-forget submit. Drops task silently when queue is full. */
    private void submitTask(Runnable task) {
        notifierPool.execute(task); // execute() avoids Future allocation unlike submit()
    }

    private boolean hasObservers() {
        return !algorithm.getAlgorithmObservers().isEmpty();
    }

    // ── pnl snapshots ────────────────────────────────────────────────────────

    public void notifyObserversOnUpdatePnlSnapshot(PnlSnapshot pnlSnapshot) {
        if (!hasObservers()) return;
        final String info = algorithmInfo;
        final List<AlgorithmObserver> observers = algorithm.getAlgorithmObservers();
        submitTask(() -> {
            for (AlgorithmObserver obs : observers) {
                obs.onUpdatePnlSnapshot(info, pnlSnapshot);
            }
        });
    }

    // ── portfolio snapshots ───────────────────────────────────────────────────

    public void notifyObserversOnUpdatePortfolioSnapshot(PortfolioSnapshot portfolioSnapshot) {
        if (!hasObservers()) return;
        final String info = algorithmInfo;
        final List<AlgorithmObserver> observers = algorithm.getAlgorithmObservers();
        submitTask(() -> {
            for (AlgorithmObserver obs : observers) {
                obs.onUpdatePortfolioSnapshot(info, portfolioSnapshot);
            }
        });
    }

    // ── depth ─────────────────────────────────────────────────────────────────

    public void notifyObserversOnUpdateDepth(Depth depth) {
        if (!hasObservers()) return;
        final String info = algorithmInfo;
        final List<AlgorithmObserver> observers = algorithm.getAlgorithmObservers();
        submitTask(() -> {
            for (AlgorithmObserver obs : observers) {
                obs.onUpdateDepth(info, depth);
            }
        });
    }

    // ── trade ─────────────────────────────────────────────────────────────────

    public void notifyObserversOnUpdatePnlSnapshot(Trade trade) {
        if (!hasObservers()) return;
        final String info = algorithmInfo;
        final List<AlgorithmObserver> observers = algorithm.getAlgorithmObservers();
        submitTask(() -> {
            for (AlgorithmObserver obs : observers) {
                obs.onUpdateTrade(info, trade);
            }
        });
    }

    // ── params ────────────────────────────────────────────────────────────────

    public void notifyObserversOnUpdateParams(Map<String, Object> params) {
        // Cheap deduplication on the caller thread; Map#equals is avoided when
        // the reference is identical (same object) — the most common case.
        if (!firstParams && (lastParams == params || lastParams.equals(params))) {
            return;
        }
        firstParams = false;
        lastParams = params;

        if (!hasObservers()) return;
        final String info = algorithmInfo;
        final List<AlgorithmObserver> observers = algorithm.getAlgorithmObservers();
        submitTask(() -> {
            for (AlgorithmObserver obs : observers) {
                obs.onUpdateParams(info, params);
            }
        });
    }

    public void notifyLastParams() {
        if (!lastParams.isEmpty()) {
            firstParams = true;
            notifyObserversOnUpdateParams(lastParams);
        }
    }

    // ── custom columns ────────────────────────────────────────────────────────

    public void notifyObserversCustomColumns(long timestamp, String instrumentPk, String key, Double value) {
        if (!hasObservers()) return;
        final String info = algorithmInfo;
        final List<AlgorithmObserver> observers = algorithm.getAlgorithmObservers();
        submitTask(() -> {
            for (AlgorithmObserver obs : observers) {
                obs.onCustomColumns(timestamp, info, instrumentPk, key, value);
            }
        });
    }

    // ── message ───────────────────────────────────────────────────────────────

    public void notifyObserversOnUpdateMessage(String name, String body) {
        if (!hasObservers()) return;
        final String info = algorithmInfo;
        final List<AlgorithmObserver> observers = algorithm.getAlgorithmObservers();
        submitTask(() -> {
            for (AlgorithmObserver obs : observers) {
                obs.onUpdateMessage(info, name, body);
            }
        });
    }

    // ── order request ─────────────────────────────────────────────────────────

    public void notifyObserversOnOrderRequest(OrderRequest orderRequest) {
        if (!hasObservers()) return;
        final String info = algorithmInfo;
        final List<AlgorithmObserver> observers = algorithm.getAlgorithmObservers();
        submitTask(() -> {
            for (AlgorithmObserver obs : observers) {
                obs.onOrderRequest(info, orderRequest);
            }
        });
    }

    // ── execution report ──────────────────────────────────────────────────────

    public void notifyObserversOnExecutionReportUpdate(ExecutionReport executionReport) {
        if (!hasObservers()) return;
        final String info = algorithmInfo;
        final List<AlgorithmObserver> observers = algorithm.getAlgorithmObservers();
        submitTask(() -> {
            for (AlgorithmObserver obs : observers) {
                obs.onExecutionReportUpdate(info, executionReport);
            }
        });
    }
}
