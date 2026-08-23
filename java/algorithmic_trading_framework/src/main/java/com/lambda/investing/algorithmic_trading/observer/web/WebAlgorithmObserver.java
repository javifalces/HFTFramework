package com.lambda.investing.algorithmic_trading.observer.web;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.AlgorithmObserver;
import com.lambda.investing.algorithmic_trading.AlgorithmProvider;
import com.lambda.investing.algorithmic_trading.pnl_calculation.MultiAlgoPortfolioAggregator;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PortfolioSnapshot;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;

import static com.lambda.investing.model.Util.*;

/**
 * {@link AlgorithmObserver} implementation that starts an embedded HTTP + WebSocket
 * server and streams every algorithm update to connected browser clients.
 *
 * <p>To enable the web UI add {@code "uiWebPort": 9001} (or any free port) to the
 * JSON configuration of a backtest or a live-trading session.  Once the process is
 * running, open {@code http://localhost:9001} in a browser to see the real-time
 * dashboard.
 *
 * <p>Each update is serialised as a JSON object and pushed over WebSocket:
 * <pre>{@code
 * {
 *   "type": "PORTFOLIO_SNAPSHOT",
 *   "timestamp": 1234567890,
 *   "algorithmInfo": "AvellanedaStoikov",
 *   "data": { ... }
 * }
 * }</pre>
 *
 * <p>Supported message types: {@code STATE}, {@code PORTFOLIO_SNAPSHOT},
 * {@code TRADE}, {@code EXECUTION_REPORT},
 * {@code ORDER_REQUEST}, {@code PARAMS}, {@code CUSTOM_COLUMN}, {@code MESSAGE},
 * {@code DEPTH}.
 */
public class WebAlgorithmObserver implements AlgorithmObserver {

    private static final Logger logger = LogManager.getLogger(WebAlgorithmObserver.class);

    private final AlgorithmWebServer server;



    /**
     * Aggregates per-algorithm {@link PortfolioSnapshot} objects and provides a summed,
     * per-instrument view used to populate the Instruments tab on the dashboard.
     * On every {@link #onUpdatePortfolioSnapshot} call the aggregator is updated so that the
     * aggregated snapshot embedded in every outgoing portfolio message always
     * reflects the full cross-algorithm picture.
     *
     * @see MultiAlgoPortfolioAggregator
     */
    private final MultiAlgoPortfolioAggregator portfolioAggregator = new MultiAlgoPortfolioAggregator();
    private volatile Map<String, Object> latestParams;
    /**
     * Parameters per algorithm (algorithmInfo -> parameters map). Used for MultiAlgorithm scenarios.
     */
    private final Map<String, Map<String, Object>> paramsByAlgorithm = new ConcurrentHashMap<>();
    /**
     * Custom columns per algorithm (algorithmInfo -> custom columns map). Used for MultiAlgorithm scenarios.
     */
    private final Map<String, Map<String, Object>> customColumnsByAlgorithm = new ConcurrentHashMap<>();
    private final Map<String, Double> latestCustomColumns = new ConcurrentHashMap<>();
    /** Latest L2 depth snapshot per instrument (used to restore orderbook on reconnect). */
    private final Map<String, Map<String, Object>> latestDepths = new ConcurrentHashMap<>();

    // ── Execution-report trade history (persisted on backend, queried by frontend on reconnect) ──
    /**
     * Circular buffer of trade-status execution reports: {@code {ts, algorithmInfo, data}}.
     * Only {@code CompletelyFilled} and {@code PartialFilled} reports are stored.
     */
    private final Deque<Map<String, Object>> erHistory = new ConcurrentLinkedDeque<>();
    /**
     * Maximum number of trade execution-report samples to retain in memory.
     */
    private static final int MAX_ER_HISTORY = 1_000;

    // ── PnL history (persisted on the backend, queried by the frontend on reconnect) ──
    /**
     * Circular buffer of sampled PnL entries: {@code {ts, realized, unrealized, total}}.
     */
    private final Deque<Map<String, Object>> pnlHistory = new ConcurrentLinkedDeque<>();
    /**
     * Maximum number of PnL samples to retain in memory.
     */
    private static final int MAX_PNL_HISTORY = 10_000;
    /**
     * Minimum milliseconds between consecutive samples (default 10 s, overridable via {@link #setPnlSampleIntervalMs}).
     */
    private volatile long pnlSampleIntervalMs = 10_000;
    /**
     * Timestamp of the last recorded PnL sample.
     */
    private volatile long lastPnlSampleTs = 0;

    // ── Position history (persisted on backend, queried by frontend on reconnect) ──
    /**
     * Circular buffer of sampled position entries: {@code {ts, positions: {instrument: netPosition}}}.
     * Sampled at the same rate as pnlHistory.
     */
    private final Deque<Map<String, Object>> positionHistory = new ConcurrentLinkedDeque<>();
    private static final int MAX_POSITION_HISTORY = 10_000;

    private PortfolioSnapshot lastPortfolioSnapshot;

    /**
     * Overrides the minimum interval between backend PnL samples.
     * Call before the algorithm starts producing data. Default is 10 000 ms (10 s).
     *
     * @param intervalMs interval in milliseconds; must be &gt; 0
     */
    public void setPnlSampleIntervalMs(long intervalMs) {
        if (intervalMs > 0) this.pnlSampleIntervalMs = intervalMs;
    }
    /**
     * Active (live) execution reports per instrument, keyed by clientOrderId.
     * Populated on Active/PartialFilled, removed on CompletelyFilled/Cancelled/Rejected/CancelRejected.
     * Used to overlay own orders on the orderbook regardless of whether algo-info is available in depth.
     */
    private final Map<String, ConcurrentHashMap<String, Map<String, Object>>> activeOrdersByInstrument =
            new ConcurrentHashMap<>();

    /**
     * Creates and starts the web server on the given port.
     *
     * @param port TCP port to listen on (e.g. 9001)
     * @throws InterruptedException if the thread is interrupted while the server binds
     */
    public WebAlgorithmObserver(int port) throws InterruptedException {
        this.server = new AlgorithmWebServer(port);
        // Enable Grafana tab when Prometheus monitoring is configured
        if (!Configuration.PROMETHEUS_PORT.isEmpty()) {
            server.setGrafanaUrl(Configuration.GRAFANA_URL);
            logger.info("Grafana tab enabled at {}", Configuration.GRAFANA_URL);
        }
        logger.info("Web UI available at http://localhost:{}", port);
        System.out.println("[WebAlgorithmObserver] Web UI available at http://localhost:" + port
                + "  |  login: " + Configuration.WEB_UI_LOGIN
                + "  password: " + Configuration.WEB_UI_PASSWORD);


    }

    /**
     * Controls the PAPER TRADING banner shown in the frontend.
     * Call this with {@code true} when the algorithm is running in paper-trading mode.
     *
     * @param paperTrading {@code true} to show the banner
     */
    public void setPaperTrading(boolean paperTrading) {
        server.setPaperTrading(paperTrading);
    }

    public void setBacktest(boolean backtest) {
        server.setBacktest(backtest);
    }

    public void setProvider(AlgorithmProvider provider) {
        server.setAlgorithmProvider(provider);
    }

    // -----------------------------------------------------------------------
    // AlgorithmObserver implementation
    // -----------------------------------------------------------------------

    @Override
    public void onUpdateDepth(String algorithmInfo, Depth depth) {
        Map<String, Object> snapshot = null;
        if (depth != null && depth.getInstrument() != null) {
            snapshot = toDepthSnapshot(depth);
            latestDepths.put(depth.getInstrument(), snapshot);
        }
        // Broadcast the snapshot (with frontend-expected field names: bidsQty, asksQty,
        // bidsAlgoInfo, asksAlgoInfo) instead of the raw Depth object whose GSON field
        // names differ (bidsQuantities, asksQuantities, bidsAlgorithmInfo, asksAlgorithmInfo).
        String json = buildMessage("DEPTH", algorithmInfo, snapshot != null ? snapshot : depth, currentTimeMs());
        server.broadcastUpdate(json);
        refreshState();
    }


    @Override
    public void onUpdatePortfolioSnapshot(String algorithmInfo, PortfolioSnapshot portfolioSnapshot) {
        // Store per-algo so MultiAlgorithm setups (each child fires its own snapshot) are
        // all tracked and can be sent as a full set to reconnecting clients via STATE.

        // Update the cross-algorithm aggregator.  This keeps the per-instrument totals
        // correct even when different child algorithms trade overlapping instruments.
        portfolioAggregator.update(algorithmInfo, portfolioSnapshot);
        lastPortfolioSnapshot = portfolioAggregator.getAggregatedPortfolioSnapshot();
        // Record a PnL sample for the persistent history (rate-limited).
        // Aggregate across all known child algorithms so the chart reflects the total portfolio.
        long now = currentTimeMs();
        if (now - lastPnlSampleTs >= pnlSampleIntervalMs) {
            lastPnlSampleTs = now;
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("ts", now);
            sample.put("realized", lastPortfolioSnapshot.realizedPnl);
            sample.put("unrealized", lastPortfolioSnapshot.unrealizedPnl);
            sample.put("total", lastPortfolioSnapshot.totalPnl);
            pnlHistory.addLast(sample);
            while (pnlHistory.size() > MAX_PNL_HISTORY) pnlHistory.pollFirst();
            server.updatePnlHistory(sanitizeJson(toJsonString(new ArrayList<>(pnlHistory))));

            // Record per-instrument position snapshot
            Map<String, Object> posSample = new LinkedHashMap<>();
            posSample.put("ts", now);
            Map<String, Object> positions = new LinkedHashMap<>();
            if (lastPortfolioSnapshot.getInstrumentPnlSnapshotMap() != null) {
                for (Map.Entry<String, PnlSnapshot> e : lastPortfolioSnapshot.getInstrumentPnlSnapshotMap().entrySet()) {
                    if (e.getValue() != null) positions.put(e.getKey(), e.getValue().netPosition);
                }
            }
            posSample.put("positions", positions);
            positionHistory.addLast(posSample);
            while (positionHistory.size() > MAX_POSITION_HISTORY) positionHistory.pollFirst();
            server.updatePositionHistory(sanitizeJson(toJsonString(new ArrayList<>(positionHistory))));
        }

        String json = buildMessage("PORTFOLIO_SNAPSHOT", "AGGREGATED", toPortfolioDto(lastPortfolioSnapshot), currentTimeMs());
        server.broadcastUpdate(json);

        refreshState();
    }

    @Override
    public void onUpdateTrade(String algorithmInfo, Trade trade) {
        String json = buildMessage("TRADE", algorithmInfo, trade, currentTimeMs());
        server.broadcastUpdate(json);
        refreshState();
    }

    @Override
    public void onUpdateParams(String algorithmInfo, Map<String, Object> newParams) {
        this.latestParams = newParams;
        // Store parameters per algorithm (MultiAlgorithm support)
        if (algorithmInfo != null) {
            paramsByAlgorithm.put(algorithmInfo, newParams);
        }
        String json = buildMessage("PARAMS", algorithmInfo, newParams, currentTimeMs());
        server.broadcastUpdate(json);

        refreshState();
    }

    @Override
    public void onUpdateMessage(String algorithmInfo, String name, String body) {
        Map<String, String> data = new HashMap<>();
        data.put("name", name);
        data.put("body", body);
        String json = buildMessage("MESSAGE", algorithmInfo, data, currentTimeMs());
        server.broadcastUpdate(json);
    }

    @Override
    public void onOrderRequest(String algorithmInfo, OrderRequest orderRequest) {
        String json = buildMessage("ORDER_REQUEST", algorithmInfo, orderRequest, currentTimeMs());
        server.broadcastUpdate(json);
    }

    @Override
    public void onExecutionReportUpdate(String algorithmInfo, ExecutionReport executionReport) {
        updateActiveOrders(executionReport);
        if (ExecutionReport.isTradeStatus(executionReport)) {
            addToErHistory(algorithmInfo, executionReport);
        }
        String json = buildMessage("EXECUTION_REPORT", algorithmInfo, executionReport, currentTimeMs());
        server.broadcastUpdate(json);
    }

    /**
     * Appends a trade-status execution report to the in-memory history buffer and
     * pushes the updated list to the REST endpoint cache via
     * {@link AlgorithmWebServer#updateErHistory}.
     *
     * @param algorithmInfo the algorithm that produced the fill
     * @param er            the execution report (must be {@code CompletelyFilled} or {@code PartialFilled})
     */
    private void addToErHistory(String algorithmInfo, ExecutionReport er) {
        long ts = er.getTimestampCreation() > 0 ? er.getTimestampCreation() : currentTimeMs();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("instrument", er.getInstrument());
        data.put("verb", er.getVerb() != null ? er.getVerb().name() : null);
        data.put("lastQuantity", er.getLastQuantity());
        data.put("quantity", er.getQuantity());
        data.put("quantityFill", er.getQuantityFill());
        data.put("price", er.getPrice());
        data.put("executionReportStatus", er.getExecutionReportStatus() != null ? er.getExecutionReportStatus().name() : null);
        data.put("timestampCreation", ts);

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("ts", ts);
        entry.put("algorithmInfo", algorithmInfo);
        entry.put("data", data);

        erHistory.addLast(entry);
        while (erHistory.size() > MAX_ER_HISTORY) erHistory.pollFirst();
        server.updateErHistory(sanitizeJson(toJsonString(new ArrayList<>(erHistory))));
    }

    /**
     * Maintains the {@link #activeOrdersByInstrument} map from incoming execution reports.
     * <ul>
     *   <li>Active / PartialFilled → add / update the order entry.</li>
     *   <li>CompletelyFilled / Cancelled / Rejected / CancelRejected → remove the entry.</li>
     * </ul>
     * Also removes the old entry keyed by {@code origClientOrderId} on modify-confirm flows.
     * After every mutation the flat active-order list is pushed to the server and broadcast
     * to all connected WebSocket clients so the Live Orders card updates in real-time.
     */
    private void updateActiveOrders(ExecutionReport er) {
        if (er == null || er.getInstrument() == null || er.getClientOrderId() == null) return;
        ExecutionReportStatus status = er.getExecutionReportStatus();
        if (status == null) return;

        String instrument = er.getInstrument();
        String clientOrderId = er.getClientOrderId();

        ConcurrentHashMap<String, Map<String, Object>> instrOrders =
                activeOrdersByInstrument.computeIfAbsent(instrument, k -> new ConcurrentHashMap<>());

        if (ExecutionReport.isLiveStatus(er)) {
            Map<String, Object> orderInfo = new LinkedHashMap<>();
            orderInfo.put("clientOrderId", clientOrderId);
            orderInfo.put("instrument", instrument);
            orderInfo.put("verb", er.getVerb() != null ? er.getVerb().name() : null);
            orderInfo.put("price", er.getPrice());
            orderInfo.put("quantity", er.getQuantity());
            orderInfo.put("quantityFill", er.getQuantityFill());
            orderInfo.put("status", status.name());
            long ts = er.getTimestampCreation() > 0 ? er.getTimestampCreation() : currentTimeMs();
            orderInfo.put("timestampCreation", ts);
            instrOrders.put(clientOrderId, orderInfo);
            // On modify, remove the superseded original order
            if (er.getOrigClientOrderId() != null && !er.getOrigClientOrderId().isEmpty()
                    && !er.getOrigClientOrderId().equals(clientOrderId)) {
                instrOrders.remove(er.getOrigClientOrderId());
            }
            syncActiveOrdersToServer();
            refreshState();
        } else if (ExecutionReport.isRemovedStatus(er)
                || status == ExecutionReportStatus.Rejected
                || status == ExecutionReportStatus.CancelRejected) {
            instrOrders.remove(clientOrderId);
            if (er.getOrigClientOrderId() != null && !er.getOrigClientOrderId().isEmpty()) {
                instrOrders.remove(er.getOrigClientOrderId());
            }
            syncActiveOrdersToServer();
            refreshState();
        }
    }

    /**
     * Removes stale orders that no longer exist in activeOrdersByInstrument.
     * Called by the frontend to clean up order rows when an order is removed.
     */
    private void cleanupStaleLiveOrders() {
        // Iterate through all instruments and remove any empty order maps
        activeOrdersByInstrument.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    /**
     * Serialises the current {@link #activeOrdersByInstrument} map into a flat JSON array,
     * pushes it to the REST endpoint cache via {@link AlgorithmWebServer#updateActiveOrders},
     * and broadcasts an {@code ACTIVE_ORDERS} WebSocket message to all connected clients.
     * Also cleans up any stale orders before syncing.
     */
    private void syncActiveOrdersToServer() {
        // First, clean up any stale orders (empty maps get removed)
        cleanupStaleLiveOrders();

        java.util.List<Map<String, Object>> allOrders = new ArrayList<>();
        for (Map.Entry<String, ConcurrentHashMap<String, Map<String, Object>>> entry :
                activeOrdersByInstrument.entrySet()) {
            for (Map<String, Object> order : entry.getValue().values()) {
                // Each order already carries the instrument field (added in updateActiveOrders)
                allOrders.add(order);
            }
        }
        String json = sanitizeJson(toJsonString(allOrders));
        server.updateActiveOrders(json);
        server.broadcastUpdate(buildMessage("ACTIVE_ORDERS", null, allOrders, currentTimeMs()));
    }

    @Override
    public void onCustomColumns(long timestamp, String algorithmInfo, String instrumentPk, String key, Double value) {
        latestCustomColumns.put((instrumentPk != null ? instrumentPk + "." : "") + key, value);

        // Store custom column in per-algorithm map for STATE restore
        if (algorithmInfo != null) {
            customColumnsByAlgorithm.computeIfAbsent(algorithmInfo, k -> new ConcurrentHashMap<>())
                    .put((instrumentPk != null ? instrumentPk + "." : "") + key, value);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("instrumentPk", instrumentPk);
        data.put("key", key);
        data.put("value", value);
        // Include algorithmInfo in the custom column data so the frontend knows which algorithm it came from
        data.put("algorithmInfo", algorithmInfo);
        String json = buildMessage("CUSTOM_COLUMN", algorithmInfo, data, timestamp);
        server.broadcastUpdate(json);
        refreshState();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static long currentTimeMs() {
        return System.currentTimeMillis();
    }

    /**
     * Replaces GSON-written {@code NaN} / {@code Infinity} / {@code -Infinity} tokens
     * (which are not valid JSON) with {@code null} so that {@code JSON.parse()} in the
     * browser does not throw.  The negative look-behind/ahead for {@code "} ensures we
     * do not corrupt legitimate string values that happen to contain those words.
     */
    private static final Pattern NAN_PATTERN =
            Pattern.compile("(?<![\"\\w])(NaN|-?Infinity)(?![\"\\w])");

    private static String sanitizeJson(String json) {
        return NAN_PATTERN.matcher(json).replaceAll("null");
    }

    /**
     * Serialises an update to the typed JSON envelope format expected by the
     * dashboard frontend.  All NaN / Infinity values are replaced with {@code null}
     * to produce valid JSON.
     */
    private static String buildMessage(String type, String algorithmInfo, Object data, long timestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"").append(type).append("\"");
        sb.append(",\"timestamp\":").append(timestamp);
        if (algorithmInfo != null) {
            sb.append(",\"algorithmInfo\":").append(GSON.toJson(algorithmInfo));
        }
        sb.append(",\"data\":").append(toJsonString(data));
        sb.append("}");
        return sanitizeJson(sb.toString());
    }


    /**
     * Creates a lightweight portfolio map containing only the fields the dashboard
     * frontend needs.  Avoids serialising the enormous per-instrument historical maps
     * stored inside each {@link PnlSnapshot}.
     */
    private static Map<String, Object> toPortfolioDto(PortfolioSnapshot ps) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("realizedPnl", ps.realizedPnl);
        m.put("unrealizedPnl", ps.unrealizedPnl);
        m.put("totalPnl", ps.totalPnl);
        m.put("netPosition", ps.netPosition);
        m.put("totalFees", ps.totalFees);
        m.put("netInvestment", ps.netInvestment);

        Map<String, Object> instrMap = new LinkedHashMap<>();
        if (ps.getInstrumentPnlSnapshotMap() != null) {
            for (Map.Entry<String, PnlSnapshot> e : ps.getInstrumentPnlSnapshotMap().entrySet()) {
                PnlSnapshot s = e.getValue();
                Map<String, Object> sm = new LinkedHashMap<>();
                // Include all fields from the PnL snapshot for instrument cards
                sm.put("instrumentPk", s.getInstrumentPk());
                sm.put("realizedPnl", s.realizedPnl);
                sm.put("unrealizedPnl", s.unrealizedPnl);
                sm.put("totalPnl", s.totalPnl);
                sm.put("netPosition", s.netPosition);
                sm.put("totalFees", s.totalFees);
                sm.put("netInvestment", s.netInvestment);
                sm.put("numberOfTrades", s.numberOfTrades.get());
                sm.put("numberOfAggressorTrades", s.numberOfAggressorTrades.get());
                sm.put("numberOfAggressedTrades", s.numberOfAggressedTrades.get());
                instrMap.put(e.getKey(), sm);
            }
        }
        m.put("instrumentPnlSnapshotMap", instrMap);
        return m;
    }

    /**
     * Rebuilds the REST state snapshot from the latest known values.
     */
    private void refreshState() {
        // Clean up any stale orders before building state
        cleanupStaleLiveOrders();

        Map<String, Object> state = new HashMap<>();
        if (lastPortfolioSnapshot != null) {
            state.put("portfolio", toPortfolioDto(lastPortfolioSnapshot));
        }
        if (latestParams != null) {
            state.put("params", latestParams);
        }
        // Include per-algorithm parameters for MultiAlgorithm scenarios
        if (!paramsByAlgorithm.isEmpty()) {
            state.put("paramsByAlgorithm", paramsByAlgorithm);
        }
        if (!latestCustomColumns.isEmpty()) {
            state.put("customColumns", latestCustomColumns);
        }
        // Include per-algorithm custom columns for MultiAlgorithm scenarios
        if (!customColumnsByAlgorithm.isEmpty()) {
            state.put("customColumnsByAlgorithm", customColumnsByAlgorithm);
        }
        if (!latestDepths.isEmpty()) {
            state.put("depths", latestDepths);
        }
        // Active orders per instrument – used by the frontend to overlay own orders on the book
        Map<String, Object> activeOrdersDto = new LinkedHashMap<>();
        for (Map.Entry<String, ConcurrentHashMap<String, Map<String, Object>>> e :
                activeOrdersByInstrument.entrySet()) {
            if (!e.getValue().isEmpty()) {
                activeOrdersDto.put(e.getKey(), new ArrayList<>(e.getValue().values()));
            }
        }
        if (!activeOrdersDto.isEmpty()) {
            state.put("activeOrders", activeOrdersDto);
        }
        server.updateState(sanitizeJson(toJsonString(state)));

        // Update the /api/portfolio-snapshot endpoint with the latest aggregated portfolio snapshot
        updatePortfolioSnapshotEndpoint();

        // Update the /api/parameters endpoint with the latest parameters
        updateParametersEndpoint();

        // Update the /api/instruments endpoint with the latest instrument PnL data
        updateInstrumentsEndpoint();

        // Update the /api/custom-metrics endpoint with the latest custom metrics
        updateCustomMetricsEndpoint();
    }

    /**
     * Updates the {@code GET /api/portfolio-snapshot} REST endpoint with the latest
     * aggregated portfolio snapshot from all algorithms.
     * The endpoint returns a complete cross-algorithm portfolio view ready for
     * display on the dashboard.
     */
    private void updatePortfolioSnapshotEndpoint() {
        PortfolioSnapshot aggregatedSnapshot = portfolioAggregator.getAggregatedPortfolioSnapshot();
        String snapshotJson = sanitizeJson(toJsonString(toPortfolioDto(aggregatedSnapshot)));
        server.updatePortfolioSnapshot(snapshotJson);
    }

    /**
     * Updates the {@code GET /api/parameters} REST endpoint with the latest
     * parameters from all algorithms.
     * The endpoint returns both global parameters and per-algorithm parameters.
     */
    private void updateParametersEndpoint() {
        Map<String, Object> paramsData = new LinkedHashMap<>();
        if (latestParams != null) {
            paramsData.put("params", latestParams);
        }
        if (!paramsByAlgorithm.isEmpty()) {
            paramsData.put("paramsByAlgorithm", paramsByAlgorithm);
        }
        String json = sanitizeJson(toJsonString(paramsData));
        server.updateParameters(json);
    }

    /**
     * Updates the {@code GET /api/instruments} REST endpoint with the latest
     * instrument PnL data from all algorithms.
     * The endpoint returns per-instrument snapshots for displaying in the Instruments tab.
     */
    private void updateInstrumentsEndpoint() {
        if (lastPortfolioSnapshot == null) {
            server.updateInstruments("{}");
            return;
        }

        Map<String, Object> instrumentData = new LinkedHashMap<>();
        if (lastPortfolioSnapshot.getInstrumentPnlSnapshotMap() != null) {
            Map<String, Object> instrMap = new LinkedHashMap<>();
            for (Map.Entry<String, PnlSnapshot> e : lastPortfolioSnapshot.getInstrumentPnlSnapshotMap().entrySet()) {
                PnlSnapshot s = e.getValue();
                if (s != null) {
                    Map<String, Object> sm = new LinkedHashMap<>();
                    sm.put("instrumentPk", s.getInstrumentPk());
                    sm.put("realizedPnl", s.realizedPnl);
                    sm.put("unrealizedPnl", s.unrealizedPnl);
                    sm.put("totalPnl", s.totalPnl);
                    sm.put("netPosition", s.netPosition);
                    sm.put("totalFees", s.totalFees);
                    sm.put("netInvestment", s.netInvestment);
                    sm.put("numberOfTrades", s.numberOfTrades != null ? s.numberOfTrades.get() : 0);
                    sm.put("numberOfAggressorTrades", s.numberOfAggressorTrades != null ? s.numberOfAggressorTrades.get() : 0);
                    sm.put("numberOfAggressedTrades", s.numberOfAggressedTrades != null ? s.numberOfAggressedTrades.get() : 0);
                    instrMap.put(e.getKey(), sm);
                }
            }
            instrumentData.put("instrumentPnlSnapshotMap", instrMap);
        }
        String json = sanitizeJson(toJsonString(instrumentData));
        server.updateInstruments(json);
    }

    /**
     * Updates the {@code GET /api/custom-metrics} REST endpoint with the latest
     * custom metrics from all algorithms.
     * The endpoint returns both global custom metrics and per-algorithm custom metrics.
     */
    private void updateCustomMetricsEndpoint() {
        Map<String, Object> metricsData = new LinkedHashMap<>();
        if (!latestCustomColumns.isEmpty()) {
            metricsData.put("customColumns", latestCustomColumns);
        }
        if (!customColumnsByAlgorithm.isEmpty()) {
            metricsData.put("customColumnsByAlgorithm", customColumnsByAlgorithm);
        }
        String json = sanitizeJson(toJsonString(metricsData));
        server.updateCustomMetrics(json);
    }

    /**
     * Converts a {@link Depth} snapshot into a plain {@code Map} that can be
     * serialised to JSON.  Only the fields needed by the orderbook frontend are
     * included; the heavy per-event latency fields are omitted.
     */
    private static Map<String, Object> toDepthSnapshot(Depth depth) {
        Map<String, Object> m = new HashMap<>();
        m.put("instrument", depth.getInstrument());
        m.put("timestamp",  depth.getTimestamp());
        m.put("receivedAt", System.currentTimeMillis());
        m.put("bids",       depth.getBids());
        m.put("asks",       depth.getAsks());
        m.put("bidsQty",    depth.getBidsQuantities());
        m.put("asksQty",    depth.getAsksQuantities());
        m.put("bidLevels",  depth.getBidLevels());
        m.put("askLevels",  depth.getAskLevels());
        // bidsAlgorithmInfo / asksAlgorithmInfo are only populated during backtests
        if (depth.getBidsAlgorithmInfo() != null) {
            m.put("bidsAlgoInfo", depth.getBidsAlgorithmInfo());
        }
        if (depth.getAsksAlgorithmInfo() != null) {
            m.put("asksAlgoInfo", depth.getAsksAlgorithmInfo());
        }
        return m;
    }
}
