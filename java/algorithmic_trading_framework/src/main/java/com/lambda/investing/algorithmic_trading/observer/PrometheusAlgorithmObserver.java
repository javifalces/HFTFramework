package com.lambda.investing.algorithmic_trading.observer;

import com.lambda.investing.PrometheusMetricsExporter;
import com.lambda.investing.algorithmic_trading.AlgorithmObserver;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PortfolioSnapshot;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * {@link AlgorithmObserver} implementation that publishes algorithm metrics to Prometheus.
 *
 * <p>Metrics are exported on three events:
 * <ul>
 *   <li><b>onCustomColumns</b> – arbitrary key/value pairs emitted by the algorithm are exposed
 *       as Gauges under {@code algo_custom_column} with labels {@code algorithm}, {@code instrument},
 *       and {@code key}.</li>
 *   <li><b>onUpdatePortfolioSnapshot</b> – portfolio-level PnL / position metrics are exposed as
 *       Gauges under {@code algo_portfolio_*} (one label: {@code algorithm}) and per-instrument
 *       Gauges under {@code algo_instrument_*} (labels: {@code algorithm}, {@code instrument}).</li>
 *   <li><b>onExecutionReportUpdate</b> – only execution reports whose status is a trade
 *       ({@code CompletelyFilled} or {@code PartialFilled}) are counted.  A {@link Counter} tracks
 *       the number of fills and a {@link Gauge} tracks the last traded price and quantity, all
 *       labelled with {@code algorithm}, {@code instrument}, {@code verb}, and {@code status}.</li>
 * </ul>
 *
 * <p>All metrics are registered on the default {@link io.prometheus.client.CollectorRegistry} that
 * is managed by {@link PrometheusMetricsExporter}.  If Prometheus is disabled (i.e.
 * {@code PROMETHEUS_PORT} is not set) this observer is a no-op.
 *
 * <p><b>Usage</b>
 * <pre>{@code
 * PrometheusAlgorithmObserver prometheusObserver = new PrometheusAlgorithmObserver();
 * algorithm.register(prometheusObserver);
 * }</pre>
 */
public class PrometheusAlgorithmObserver implements AlgorithmObserver {

    private static final Logger logger = LogManager.getLogger(PrometheusAlgorithmObserver.class);

    // -----------------------------------------------------------------------
    // Portfolio-level gauges  (label: algorithm)
    // -----------------------------------------------------------------------
    private Gauge portfolioRealizedPnl;
    private Gauge portfolioUnrealizedPnl;
    private Gauge portfolioTotalPnl;
    private Gauge portfolioTotalFees;
    private Gauge portfolioNetPosition;
    private Gauge portfolioNetInvestment;

    // -----------------------------------------------------------------------
    // Per-instrument gauges  (labels: algorithm, instrument)
    // -----------------------------------------------------------------------
    private Gauge instrumentRealizedPnl;
    private Gauge instrumentUnrealizedPnl;
    private Gauge instrumentTotalPnl;
    private Gauge instrumentNetPosition;
    private Gauge instrumentNumberOfTrades;

    // -----------------------------------------------------------------------
    // Trade execution metrics  (labels: algorithm, instrument, verb, status)
    // -----------------------------------------------------------------------
    private Counter tradeCount;
    private Counter tradeVolume;
    private Gauge tradeLastPrice;
    private Gauge tradeLastQuantity;

    // -----------------------------------------------------------------------
    // Custom column gauge  (labels: algorithm, instrument, key)
    // -----------------------------------------------------------------------
    private Gauge customColumn;

    private final boolean enabled;

    public PrometheusAlgorithmObserver() {
        PrometheusMetricsExporter exporter = PrometheusMetricsExporter.getInstance();
        if (!exporter.isEnabled()) {
            logger.info("PrometheusAlgorithmObserver: Prometheus is disabled – observer is a no-op.");
            this.enabled = false;
            return;
        }

        try {
            // Portfolio gauges
            portfolioRealizedPnl = Gauge.build()
                    .name("algo_portfolio_realized_pnl")
                    .help("Realized PnL at portfolio level")
                    .labelNames("algorithm")
                    .register(exporter.getRegistry());

            portfolioUnrealizedPnl = Gauge.build()
                    .name("algo_portfolio_unrealized_pnl")
                    .help("Unrealized PnL at portfolio level")
                    .labelNames("algorithm")
                    .register(exporter.getRegistry());

            portfolioTotalPnl = Gauge.build()
                    .name("algo_portfolio_total_pnl")
                    .help("Total PnL (realized + unrealized) at portfolio level")
                    .labelNames("algorithm")
                    .register(exporter.getRegistry());

            portfolioTotalFees = Gauge.build()
                    .name("algo_portfolio_total_fees")
                    .help("Total fees paid at portfolio level")
                    .labelNames("algorithm")
                    .register(exporter.getRegistry());

            portfolioNetPosition = Gauge.build()
                    .name("algo_portfolio_net_position")
                    .help("Net position across the portfolio")
                    .labelNames("algorithm")
                    .register(exporter.getRegistry());

            portfolioNetInvestment = Gauge.build()
                    .name("algo_portfolio_net_investment")
                    .help("Net investment (capital at risk) at portfolio level")
                    .labelNames("algorithm")
                    .register(exporter.getRegistry());

            // Per-instrument gauges
            instrumentRealizedPnl = Gauge.build()
                    .name("algo_instrument_realized_pnl")
                    .help("Realized PnL per instrument")
                    .labelNames("algorithm", "instrument")
                    .register(exporter.getRegistry());

            instrumentUnrealizedPnl = Gauge.build()
                    .name("algo_instrument_unrealized_pnl")
                    .help("Unrealized PnL per instrument")
                    .labelNames("algorithm", "instrument")
                    .register(exporter.getRegistry());

            instrumentTotalPnl = Gauge.build()
                    .name("algo_instrument_total_pnl")
                    .help("Total PnL per instrument")
                    .labelNames("algorithm", "instrument")
                    .register(exporter.getRegistry());

            instrumentNetPosition = Gauge.build()
                    .name("algo_instrument_net_position")
                    .help("Net position per instrument")
                    .labelNames("algorithm", "instrument")
                    .register(exporter.getRegistry());

            instrumentNumberOfTrades = Gauge.build()
                    .name("algo_instrument_number_of_trades")
                    .help("Number of trades executed per instrument")
                    .labelNames("algorithm", "instrument")
                    .register(exporter.getRegistry());

            // Trade execution metrics
            tradeCount = Counter.build()
                    .name("algo_trade_count_total")
                    .help("Total number of trade fills")
                    .labelNames("algorithm", "instrument", "verb", "status")
                    .register(exporter.getRegistry());

            tradeVolume = Counter.build()
                    .name("algo_trade_volume_total")
                    .help("Total traded volume (quantity) per instrument")
                    .labelNames("algorithm", "instrument", "verb")
                    .register(exporter.getRegistry());

            tradeLastPrice = Gauge.build()
                    .name("algo_trade_last_price")
                    .help("Last trade execution price")
                    .labelNames("algorithm", "instrument", "verb")
                    .register(exporter.getRegistry());

            tradeLastQuantity = Gauge.build()
                    .name("algo_trade_last_quantity")
                    .help("Last trade filled quantity")
                    .labelNames("algorithm", "instrument", "verb")
                    .register(exporter.getRegistry());

            // Custom column gauge
            customColumn = Gauge.build()
                    .name("algo_custom_column")
                    .help("Custom algorithm column value")
                    .labelNames("algorithm", "instrument", "key")
                    .register(exporter.getRegistry());

            this.enabled = true;
            logger.info("PrometheusAlgorithmObserver initialised and metrics registered.");

        } catch (Exception e) {
            logger.error("PrometheusAlgorithmObserver: failed to register Prometheus metrics – observer disabled.", e);
            // set local enabled flag to false; the field is not final here so we use a workaround
            throw new IllegalStateException("Failed to initialise PrometheusAlgorithmObserver", e);
        }
    }

    // -----------------------------------------------------------------------
    // AlgorithmObserver callbacks
    // -----------------------------------------------------------------------

    @Override
    public void onUpdateDepth(String algorithmInfo, Depth depth) {
        // not published to Prometheus
    }

    @Override
    public void onUpdatePnlSnapshot(String algorithmInfo, PnlSnapshot pnlSnapshot) {
        // not published to Prometheus – handled via onUpdatePortfolioSnapshot
    }

    @Override
    public void onUpdatePortfolioSnapshot(String algorithmInfo, PortfolioSnapshot portfolioSnapshot) {
        if (!enabled) return;

        try {
            // Portfolio-level metrics
            portfolioRealizedPnl.labels(algorithmInfo).set(portfolioSnapshot.getRealizedPnl());
            portfolioUnrealizedPnl.labels(algorithmInfo).set(portfolioSnapshot.getUnrealizedPnl());
            portfolioTotalPnl.labels(algorithmInfo).set(portfolioSnapshot.getTotalPnl());
            portfolioTotalFees.labels(algorithmInfo).set(portfolioSnapshot.getTotalFees());
            portfolioNetPosition.labels(algorithmInfo).set(portfolioSnapshot.getNetPosition());
            portfolioNetInvestment.labels(algorithmInfo).set(portfolioSnapshot.getNetInvestment());

            // Per-instrument metrics
            if (portfolioSnapshot.getInstrumentPnlSnapshotMap() != null) {
                for (Map.Entry<String, PnlSnapshot> entry : portfolioSnapshot.getInstrumentPnlSnapshotMap().entrySet()) {
                    String instrument = entry.getKey();
                    PnlSnapshot pnl = entry.getValue();

                    instrumentRealizedPnl.labels(algorithmInfo, instrument).set(pnl.getRealizedPnl());
                    instrumentUnrealizedPnl.labels(algorithmInfo, instrument).set(pnl.getUnrealizedPnl());
                    instrumentTotalPnl.labels(algorithmInfo, instrument).set(pnl.getTotalPnl());
                    instrumentNetPosition.labels(algorithmInfo, instrument).set(pnl.getNetPosition());
                    instrumentNumberOfTrades.labels(algorithmInfo, instrument).set(pnl.getNumberOfTrades().get());
                }
            }
        } catch (Exception e) {
            logger.warn("PrometheusAlgorithmObserver: error updating portfolio snapshot metrics: {}", e.getMessage());
        }
    }

    @Override
    public void onUpdateTrade(String algorithmInfo, Trade trade) {
        // market trades are not published here
    }

    @Override
    public void onUpdateParams(String algorithmInfo, Map<String, Object> newParams) {
        // not published to Prometheus
    }

    @Override
    public void onUpdateMessage(String algorithmInfo, String name, String body) {
        // not published to Prometheus
    }

    @Override
    public void onOrderRequest(String algorithmInfo, OrderRequest orderRequest) {
        // not published to Prometheus
    }

    @Override
    public void onExecutionReportUpdate(String algorithmInfo, ExecutionReport executionReport) {
        if (!enabled) return;
        if (!ExecutionReport.isTradeStatus(executionReport)) {
            // Only count actual fills (CompletelyFilled / PartialFilled)
            return;
        }

        try {
            String instrument = executionReport.getInstrument();
            String verb = executionReport.getVerb() != null ? executionReport.getVerb().name() : "Unknown";
            String status = executionReport.getExecutionReportStatus() != null
                    ? executionReport.getExecutionReportStatus().name()
                    : "Unknown";
            double lastQty = executionReport.getLastQuantity();
            double price = executionReport.getPrice();

            tradeCount.labels(algorithmInfo, instrument, verb, status).inc();
            if (lastQty > 0) {
                tradeVolume.labels(algorithmInfo, instrument, verb).inc(lastQty);
            }
            tradeLastPrice.labels(algorithmInfo, instrument, verb).set(price);
            tradeLastQuantity.labels(algorithmInfo, instrument, verb).set(lastQty);

        } catch (Exception e) {
            logger.warn("PrometheusAlgorithmObserver: error updating execution report metrics: {}", e.getMessage());
        }
    }

    @Override
    public void onCustomColumns(long timestamp, String algorithmInfo, String instrumentPk, String key, Double value) {
        if (!enabled) return;
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) return;

        try {
            customColumn.labels(algorithmInfo, instrumentPk, key).set(value);
        } catch (Exception e) {
            logger.warn("PrometheusAlgorithmObserver: error updating custom column '{}': {}", key, e.getMessage());
        }
    }
}



