package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.disruptor.DisruptorConnectorConfiguration;
import com.lambda.investing.connector.disruptor.DisruptorConnectorHelper;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PortfolioSnapshot;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Asynchronous notifier for algorithm observers (UI / monitoring).
 * <p>
 * Design goals for HFT hot-path safety:
 * - The caller (trading engine) NEVER blocks — all notifications are fire-and-forget.
 * - Uses LMAX Disruptor ring buffer for high-throughput, low-latency event dispatch.
 * - If the ring buffer is full, the notification is silently dropped. UI lag is acceptable;
 * trading latency is not.
 * - The background thread processes events asynchronously to avoid blocking the caller.
 */
public class AlgorithmNotifier {

    /**
     * Notification event types for algorithm observer callbacks.
     */
    private enum NotificationType {
        PORTFOLIO_SNAPSHOT,
        DEPTH,
        TRADE,
        PARAMETERS,
        CUSTOM_COLUMN,
        MESSAGE,
        ORDER_REQUEST,
        EXECUTION_REPORT
    }

    private final DisruptorConnectorHelper disruptorHelper;
    private final ConnectorConfiguration dummyConfig = new DisruptorConnectorConfiguration();
    private volatile String algorithmInfo;
    private final Algorithm algorithm;
    /**
     * Non-null only when this notifier owns (created) the disruptor and must shut it down.
     */
    private final String ownedDisruptorName;
    private final DisruptorConnectorHelper.EventConsumer consumer;

    private Map<String, Object> lastParams = new HashMap<>();
    private boolean firstParams = true;

    public AlgorithmNotifier(Algorithm algorithm, int threadsNotifier) {
        this.algorithmInfo = algorithm.algorithmInfo;
        this.algorithm = algorithm;
        this.consumer = this::handleNotification;

        String threadName = this.algorithmInfo + "_notifier_disruptor";
        this.ownedDisruptorName = threadName;
        this.disruptorHelper = DisruptorConnectorHelper.getInstance(
                threadName,
                Configuration.ConnectorProviderType.DISRUPTOR_HIGH_THROUGHPUT
        );
        this.disruptorHelper.init();
        this.disruptorHelper.addConsumer(this.consumer);
    }

    /**
     * Creates an {@link AlgorithmNotifier} that routes events through a pre-existing
     * (shared) {@link DisruptorConnectorHelper} instead of creating its own.
     * Use this in {@link MultiAlgorithm} when {@code useCommonNotifierDisruptor} is enabled
     * so all child algorithms share one ring-buffer thread.
     */
    public AlgorithmNotifier(Algorithm algorithm, DisruptorConnectorHelper sharedDisruptorHelper) {
        this.algorithmInfo = algorithm.algorithmInfo;
        this.algorithm = algorithm;
        this.ownedDisruptorName = null;
        this.consumer = this::handleNotification;
        this.disruptorHelper = sharedDisruptorHelper;
        this.disruptorHelper.addConsumer(this.consumer);
    }

    /**
     * Removes this notifier's consumer from the disruptor.
     * If this notifier owns the disruptor (created it), also shuts it down and removes it
     * from the global registry, freeing the background thread.
     */
    public void stop() {
        disruptorHelper.removeConsumer(consumer);
        if (ownedDisruptorName != null) {
            DisruptorConnectorHelper.removeInstance(ownedDisruptorName);
        }
    }

    public void setAlgorithmInfo(String algorithmInfo) {
        this.algorithmInfo = algorithmInfo;
    }

    /**
     * Non-blocking fire-and-forget publish to Disruptor. Drops event silently when ring buffer is full.
     */
    private void publishNotification(NotificationType notificationType, Object content) {
        NotificationWrapper wrapper = new NotificationWrapper(notificationType, content);
        boolean published = false;
        if (this.algorithm.isBacktest) {
            //if so much info blocking
            published = disruptorHelper.publish(dummyConfig, System.nanoTime(), TypeMessage.info, wrapper);
        } else {
            //avoid blocking , better to print
            published = disruptorHelper.tryPublish(dummyConfig, System.nanoTime(), TypeMessage.info, wrapper);
        }
        // Additional logging at AlgorithmNotifier level if needed
        if (!published) {
            // The DisruptorConnectorHelper already logs to System.err and logger,
            // but we can add algorithm-specific context here if desired
        }
    }

    /**
     * Consumer callback invoked by DisruptorConnectorHelper on the dedicated consumer thread.
     */
    private void handleNotification(ConnectorConfiguration config, long timestamp,
                                    TypeMessage typeMessage, Object content) {
        if (!(content instanceof NotificationWrapper wrapper)) {
            return;
        }

        final String info = algorithmInfo;
        final List<AlgorithmObserver> observers = algorithm.getAlgorithmObservers();

        if (observers.isEmpty()) return;

        switch (wrapper.type) {
            case PORTFOLIO_SNAPSHOT -> {
                PortfolioSnapshot portfolioSnapshot = (PortfolioSnapshot) wrapper.payload;
                for (AlgorithmObserver obs : observers) {
                    obs.onUpdatePortfolioSnapshot(info, portfolioSnapshot);
                }
            }
            case DEPTH -> {
                Depth depth = (Depth) wrapper.payload;
                for (AlgorithmObserver obs : observers) {
                    obs.onUpdateDepth(info, depth);
                }
            }
            case TRADE -> {
                Trade trade = (Trade) wrapper.payload;
                for (AlgorithmObserver obs : observers) {
                    obs.onUpdateTrade(info, trade);
                }
            }
            case PARAMETERS -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) wrapper.payload;
                for (AlgorithmObserver obs : observers) {
                    obs.onUpdateParams(info, params);
                }
            }
            case CUSTOM_COLUMN -> {
                CustomColumnData data = (CustomColumnData) wrapper.payload;
                for (AlgorithmObserver obs : observers) {
                    obs.onCustomColumns(data.timestamp, info, data.instrumentPk, data.key, data.value);
                }
            }
            case MESSAGE -> {
                MessageData data = (MessageData) wrapper.payload;
                for (AlgorithmObserver obs : observers) {
                    obs.onUpdateMessage(info, data.name, data.body);
                }
            }
            case ORDER_REQUEST -> {
                OrderRequest orderRequest = (OrderRequest) wrapper.payload;
                for (AlgorithmObserver obs : observers) {
                    obs.onOrderRequest(info, orderRequest);
                }
            }
            case EXECUTION_REPORT -> {
                ExecutionReport executionReport = (ExecutionReport) wrapper.payload;
                for (AlgorithmObserver obs : observers) {
                    obs.onExecutionReportUpdate(info, executionReport);
                }
            }
        }
    }

    private boolean hasObservers() {
        return !algorithm.getAlgorithmObservers().isEmpty();
    }

    // Helper classes to wrap notification data
    private static class NotificationWrapper {
        final NotificationType type;
        final Object payload;

        NotificationWrapper(NotificationType type, Object payload) {
            this.type = type;
            this.payload = payload;
        }
    }

    private static class CustomColumnData {
        final long timestamp;
        final String instrumentPk;
        final String key;
        final Double value;

        CustomColumnData(long timestamp, String instrumentPk, String key, Double value) {
            this.timestamp = timestamp;
            this.instrumentPk = instrumentPk;
            this.key = key;
            this.value = value;
        }
    }

    private static class MessageData {
        final String name;
        final String body;

        MessageData(String name, String body) {
            this.name = name;
            this.body = body;
        }
    }

    // ── portfolio snapshots ───────────────────────────────────────────────────

    public void notifyObserversOnUpdatePortfolioSnapshot(PortfolioSnapshot portfolioSnapshot) {
        if (!hasObservers()) return;
        publishNotification(NotificationType.PORTFOLIO_SNAPSHOT, portfolioSnapshot);
    }

    // ── depth ─────────────────────────────────────────────────────────────────

    public void notifyObserversOnUpdateDepth(Depth depth) {
        if (!hasObservers()) return;
        publishNotification(NotificationType.DEPTH, depth);
    }

    // ── trade ─────────────────────────────────────────────────────────────────

    public void notifyObserversOnUpdateTrade(Trade trade) {
        if (!hasObservers()) return;
        publishNotification(NotificationType.TRADE, trade);
    }

    // ── params ────────────────────────────────────────────────────────────────

    public void notifyObserversOnUpdateParams(Map<String, Object> params) {
        // Cheap deduplication on the caller thread; Map#equals is avoided when
        // the reference is identical (same object) — the most common case.
        if (!firstParams && (lastParams == params || lastParams.equals(params))) {
            return;
        }
        firstParams = false;
        lastParams = new HashMap<>(params);

        if (!hasObservers()) return;
        publishNotification(NotificationType.PARAMETERS, params);
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
        publishNotification(NotificationType.CUSTOM_COLUMN,
                new CustomColumnData(timestamp, instrumentPk, key, value));
    }

    // ── message ───────────────────────────────────────────────────────────────

    public void notifyObserversOnUpdateMessage(String name, String body) {
        if (!hasObservers()) return;
        publishNotification(NotificationType.MESSAGE, new MessageData(name, body));
    }

    // ── order request ─────────────────────────────────────────────────────────

    public void notifyObserversOnOrderRequest(OrderRequest orderRequest) {
        if (!hasObservers()) return;
        publishNotification(NotificationType.ORDER_REQUEST, orderRequest);
    }

    // ── execution report ──────────────────────────────────────────────────────

    public void notifyObserversOnExecutionReportUpdate(ExecutionReport executionReport) {
        if (!hasObservers()) return;
        publishNotification(NotificationType.EXECUTION_REPORT, executionReport);
    }
}
