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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final Logger logger = LogManager.getLogger(AlgorithmNotifier.class);

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

    /**
     * Notification types that must NEVER be silently dropped, even in live trading.
     * <p>
     * {@code DEPTH}, {@code PARAMETERS} and {@code CUSTOM_COLUMN} are high-frequency,
     * UI-only ticks where losing an occasional update under load is acceptable (a newer
     * update will arrive shortly after). {@code EXECUTION_REPORT}/{@code TRADE}/
     * {@code PORTFOLIO_SNAPSHOT}/{@code ORDER_REQUEST}/{@code MESSAGE} are comparatively
     * rare, high-value events (fills, portfolio state, alerts) whose loss silently breaks
     * push/web dashboards — e.g. a {@code PartialFilled} execution report vanishing because
     * the shared ring buffer (see {@link MultiAlgorithm#COMMON_ALGO_NOTIFIER_DISRUPTOR}) was
     * momentarily saturated by depth ticks from sibling algorithms. These are published with
     * the blocking {@link DisruptorConnectorHelper#publish} instead of {@code tryPublish} so
     * they wait for a free slot rather than being dropped.
     */
    private static final Set<NotificationType> CRITICAL_TYPES = EnumSet.of(
            NotificationType.EXECUTION_REPORT,
            NotificationType.PORTFOLIO_SNAPSHOT,
            NotificationType.ORDER_REQUEST,
            NotificationType.MESSAGE
    );

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
     * Non-blocking fire-and-forget publish to Disruptor for high-frequency, droppable
     * notification types (DEPTH/PARAMETERS/CUSTOM_COLUMN). Critical types (see
     * {@link #CRITICAL_TYPES}) always use the blocking {@link DisruptorConnectorHelper#publish}
     * so they are never silently lost, even in live trading.
     */
    private void publishNotification(NotificationType notificationType, Object content) {
        NotificationWrapper wrapper = new NotificationWrapper(notificationType, content, this.algorithm);
        boolean published;
        boolean mustNotDrop = this.algorithm.isBacktest || CRITICAL_TYPES.contains(notificationType);
        if (mustNotDrop) {
            // Blocking publish: waits for a free ring-buffer slot instead of dropping.
            // Safe here because critical events (fills, portfolio snapshots, order requests,
            // messages) are comparatively rare compared to depth ticks, so the producer thread
            // (trading engine / market data callback) should essentially never actually block.
            published = disruptorHelper.publish(dummyConfig, System.nanoTime(), TypeMessage.info, wrapper);
        } else {
            //avoid blocking , better to print
            published = disruptorHelper.tryPublish(dummyConfig, System.nanoTime(), TypeMessage.info, wrapper);
        }
        if (!published) {
            logger.warn("[{}] Dropped {} notification – ring buffer not initialised or full (content={})",
                    algorithmInfo, notificationType, content);
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

        // When several algorithms share the same underlying Disruptor ring buffer
        // (e.g. MultiAlgorithm children via useSharedNotifierDisruptor), every consumer
        // registered on the buffer receives EVERY published event (fan-out semantics –
        // see DisruptorConnectorHelper#dispatchEvent). Without this guard, a single trade
        // published by one child algorithm would be re-delivered by every sibling child's
        // AlgorithmNotifier consumer to that sibling's own registered observers, producing
        // N duplicate notifications for a single event (N = number of sibling consumers
        // sharing the buffer). Only process events that originated from THIS notifier's
        // own algorithm.
        if (wrapper.sourceAlgorithm != this.algorithm) {
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
        /**
         * The {@link Algorithm} whose {@link AlgorithmNotifier} published this event.
         * Used by {@link #handleNotification} to discard events that were fanned-out
         * to it from a sibling algorithm sharing the same Disruptor ring buffer.
         */
        final Algorithm sourceAlgorithm;

        NotificationWrapper(NotificationType type, Object payload, Algorithm sourceAlgorithm) {
            this.type = type;
            this.payload = payload;
            this.sourceAlgorithm = sourceAlgorithm;
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
