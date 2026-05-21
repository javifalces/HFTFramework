package com.lambda.investing.algorithmic_trading.observer.web;

import com.lambda.investing.algorithmic_trading.AlgorithmObserver;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PortfolioSnapshot;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.lambda.investing.model.Util.GSON;
import static com.lambda.investing.model.Util.toJsonStringGSON;

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
 * {@code PNL_SNAPSHOT}, {@code TRADE}, {@code EXECUTION_REPORT},
 * {@code ORDER_REQUEST}, {@code PARAMS}, {@code CUSTOM_COLUMN}, {@code MESSAGE},
 * {@code DEPTH}.
 */
public class WebAlgorithmObserver implements AlgorithmObserver {

    private static final Logger logger = LogManager.getLogger(WebAlgorithmObserver.class);

    private final AlgorithmWebServer server;

    // Mutable current-state snapshot kept for newly connecting clients
    private volatile PortfolioSnapshot latestPortfolio;
    private volatile Map<String, Object> latestParams;
    private final Map<String, Double> latestCustomColumns = new ConcurrentHashMap<>();

    /**
     * Creates and starts the web server on the given port.
     *
     * @param port TCP port to listen on (e.g. 9001)
     * @throws IOException if the server cannot bind to the port
     */
    public WebAlgorithmObserver(int port) throws IOException {
        this.server = new AlgorithmWebServer(port);
        logger.info("Web UI available at http://localhost:{}", port);
        System.out.println("[WebAlgorithmObserver] Web UI available at http://localhost:" + port);
    }

    // -----------------------------------------------------------------------
    // AlgorithmObserver implementation
    // -----------------------------------------------------------------------

    @Override
    public void onUpdateDepth(String algorithmInfo, Depth depth) {
        String json = buildMessage("DEPTH", algorithmInfo, depth, currentTimeMs());
        server.broadcastUpdate(json);
    }

    @Override
    public void onUpdatePnlSnapshot(String algorithmInfo, PnlSnapshot pnlSnapshot) {
        String json = buildMessage("PNL_SNAPSHOT", algorithmInfo, pnlSnapshot, currentTimeMs());
        server.broadcastUpdate(json);
        refreshState();
    }

    @Override
    public void onUpdatePortfolioSnapshot(String algorithmInfo, PortfolioSnapshot portfolioSnapshot) {
        this.latestPortfolio = portfolioSnapshot;
        String json = buildMessage("PORTFOLIO_SNAPSHOT", algorithmInfo, portfolioSnapshot, currentTimeMs());
        server.broadcastUpdate(json);
        refreshState();
    }

    @Override
    public void onUpdateTrade(String algorithmInfo, Trade trade) {
        String json = buildMessage("TRADE", algorithmInfo, trade, currentTimeMs());
        server.broadcastUpdate(json);
    }

    @Override
    public void onUpdateParams(String algorithmInfo, Map<String, Object> newParams) {
        this.latestParams = newParams;
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
        String json = buildMessage("EXECUTION_REPORT", algorithmInfo, executionReport, currentTimeMs());
        server.broadcastUpdate(json);
    }

    @Override
    public void onCustomColumns(long timestamp, String algorithmInfo, String instrumentPk, String key, Double value) {
        latestCustomColumns.put((instrumentPk != null ? instrumentPk + "." : "") + key, value);
        Map<String, Object> data = new HashMap<>();
        data.put("instrumentPk", instrumentPk);
        data.put("key", key);
        data.put("value", value);
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
     * Serialises an update to the typed JSON envelope format expected by the
     * dashboard frontend.
     */
    private static String buildMessage(String type, String algorithmInfo, Object data, long timestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"").append(type).append("\"");
        sb.append(",\"timestamp\":").append(timestamp);
        if (algorithmInfo != null) {
            sb.append(",\"algorithmInfo\":").append(GSON.toJson(algorithmInfo));
        }
        sb.append(",\"data\":").append(toJsonStringGSON(data));
        sb.append("}");
        return sb.toString();
    }

    /**
     * Rebuilds the REST state snapshot from the latest known values.
     */
    private void refreshState() {
        Map<String, Object> state = new HashMap<>();
        if (latestPortfolio != null) {
            state.put("portfolio", latestPortfolio);
        }
        if (latestParams != null) {
            state.put("params", latestParams);
        }
        if (!latestCustomColumns.isEmpty()) {
            state.put("customColumns", latestCustomColumns);
        }
        server.updateState(toJsonStringGSON(state));
    }
}
