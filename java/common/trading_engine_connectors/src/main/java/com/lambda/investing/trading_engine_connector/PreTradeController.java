package com.lambda.investing.trading_engine_connector;

import com.lambda.investing.model.trading.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pre-trade risk controller that validates NewOrder (Send) and ReplaceOrder (Modify) requests
 * against configured thresholds before they reach the broker.
 *
 * <p>Three controls are supported, each configurable globally or per instrument:
 * <ul>
 *   <li>{@code pretrade.max_net_position} – maximum allowed intraday net position (buys add,
 *       sells subtract). Checked for orders that would increase the absolute net exposure.</li>
 *   <li>{@code pretrade.max_gross_position} – maximum allowed intraday gross position (buys and
 *       sells both add). Checked for any order that increases the gross traded size.</li>
 *   <li>{@code pretrade.max_quantity} – maximum allowed quantity for a single order.</li>
 * </ul>
 *
 * <p>Property keys follow the pattern:
 * <pre>
 *   pretrade.max_net_position=500
 *   pretrade.max_gross_position=2000
 *   pretrade.max_quantity=100
 *
 *   pretrade.max_net_position.btceur_kraken=600
 *   pretrade.max_gross_position.btceur_kraken=2002
 *   pretrade.max_quantity.btceur_kraken=104
 * </pre>
 * Instrument-specific properties override the general ones.
 *
 * <p>The controller tracks <em>intraday fills</em> from {@link ExecutionReport}s and resets
 * automatically at the start of each calendar day (UTC).
 */
public class PreTradeController {

    // ── property key constants ──────────────────────────────────────────────────
    public static final String PROP_PREFIX = "pretrade.";
    public static final String PROP_MAX_NET_POSITION = PROP_PREFIX + "max_net_position";
    public static final String PROP_MAX_GROSS_POSITION = PROP_PREFIX + "max_gross_position";
    public static final String PROP_MAX_QUANTITY = PROP_PREFIX + "max_quantity";

    private static final Logger logger = LogManager.getLogger(PreTradeController.class);

    // ── configured thresholds ───────────────────────────────────────────────────
    private final double defaultMaxNetPosition;
    private final double defaultMaxGrossPosition;
    private final double defaultMaxQuantity;

    /**
     * Per-instrument overrides; key = instrument primary-key (lower-case).
     */
    private final Map<String, Double> instrumentMaxNetPosition = new ConcurrentHashMap<>();
    private final Map<String, Double> instrumentMaxGrossPosition = new ConcurrentHashMap<>();
    private final Map<String, Double> instrumentMaxQuantity = new ConcurrentHashMap<>();

    // ── intraday position state ─────────────────────────────────────────────────

    /**
     * Net position per instrument (buy fills add, sell fills subtract).
     * Positive = net long, negative = net short.
     */
    private final Map<String, Double> intradayNetPosition = new ConcurrentHashMap<>();

    /**
     * Gross position per instrument (all fills add, regardless of side).
     */
    private final Map<String, Double> intradayGrossPosition = new ConcurrentHashMap<>();

    /**
     * Active (live) orders tracked so that Modify requests can compute the quantity delta.
     * Key = clientOrderId, value = last known order quantity.
     */
    private final Map<String, Double> activeOrderQuantity = new ConcurrentHashMap<>();

    /**
     * The UTC calendar day for which the current intraday data was accumulated.
     */
    private volatile LocalDate currentDay = LocalDate.now();

    // ── constructor ─────────────────────────────────────────────────────────────

    /**
     * Creates a controller loading thresholds from {@link System#getProperties()}.
     */
    public PreTradeController() {
        this(System.getProperties());
    }

    /**
     * Creates a controller loading thresholds from the supplied {@link Properties}.
     *
     * @param properties source of {@code pretrade.*} configuration
     */
    public PreTradeController(Properties properties) {
        defaultMaxNetPosition = parseDouble(properties.getProperty(PROP_MAX_NET_POSITION), Double.MAX_VALUE);
        defaultMaxGrossPosition = parseDouble(properties.getProperty(PROP_MAX_GROSS_POSITION), Double.MAX_VALUE);
        defaultMaxQuantity = parseDouble(properties.getProperty(PROP_MAX_QUANTITY), Double.MAX_VALUE);

        // scan for per-instrument overrides
        for (String name : properties.stringPropertyNames()) {
            if (name.startsWith(PROP_MAX_NET_POSITION + ".")) {
                String instrument = name.substring((PROP_MAX_NET_POSITION + ".").length());
                instrumentMaxNetPosition.put(instrument, parseDouble(properties.getProperty(name), Double.MAX_VALUE));
            } else if (name.startsWith(PROP_MAX_GROSS_POSITION + ".")) {
                String instrument = name.substring((PROP_MAX_GROSS_POSITION + ".").length());
                instrumentMaxGrossPosition.put(instrument, parseDouble(properties.getProperty(name), Double.MAX_VALUE));
            } else if (name.startsWith(PROP_MAX_QUANTITY + ".")) {
                String instrument = name.substring((PROP_MAX_QUANTITY + ".").length());
                instrumentMaxQuantity.put(instrument, parseDouble(properties.getProperty(name), Double.MAX_VALUE));
            }
        }

        logConfiguration();
    }

    // ── public API ──────────────────────────────────────────────────────────────

    /**
     * Checks whether the given order request passes all pre-trade controls.
     *
     * <p>Only {@link OrderRequestAction#Send} and {@link OrderRequestAction#Modify} requests that
     * <em>increase</em> the order quantity are evaluated; Cancel requests are always allowed.
     *
     * @param orderRequest the order request to evaluate
     * @return {@code null} if the order passes all controls, or a rejection
     * {@link ExecutionReport} with the reject reason if a limit is breached
     */
    public ExecutionReport checkOrderRequest(OrderRequest orderRequest) {
        rolloverIfNewDay();

        OrderRequestAction action = orderRequest.getOrderRequestAction();
        if (action == null || action == OrderRequestAction.Cancel) {
            return null; // cancels always pass
        }

        String instrument = orderRequest.getInstrument();
        double orderQty = orderRequest.getQuantity();
        Verb verb = orderRequest.getVerb();

        // For Modify: only check if quantity is being increased
        double quantityDelta = orderQty; // default: full quantity (Send)
        if (action == OrderRequestAction.Modify) {
            String origClOrdId = orderRequest.getOrigClientOrderId();
            double previousQty = activeOrderQuantity.getOrDefault(origClOrdId, 0.0);
            quantityDelta = orderQty - previousQty;
            if (quantityDelta <= 0) {
                // quantity is not increasing – no pre-trade check needed
                logger.debug("Modify on {} does not increase quantity (prev={} new={}) – skip pretrade",
                        instrument, previousQty, orderQty);
                return null;
            }
            logger.debug("Modify on {} increases quantity by {} (prev={} new={})",
                    instrument, quantityDelta, previousQty, orderQty);
        }

        // ── 1. max_quantity check ────────────────────────────────────────────────
        double maxQty = getThreshold(instrument, instrumentMaxQuantity, defaultMaxQuantity);
        if (orderQty > maxQty) {
            String reason = String.format(
                    "pretrade max_quantity breach on %s: order_qty=%.4f > max_qty=%.4f",
                    instrument, orderQty, maxQty);
            logger.warn(reason);
            return buildRejection(orderRequest, reason);
        }

        // ── 2. max_net_position check ────────────────────────────────────────────
        double maxNet = getThreshold(instrument, instrumentMaxNetPosition, defaultMaxNetPosition);
        if (maxNet < Double.MAX_VALUE) {
            double currentNet = intradayNetPosition.getOrDefault(instrument, 0.0);
            double signedDelta = (verb == Verb.Buy) ? quantityDelta : -quantityDelta;
            double prospectiveNet = currentNet + signedDelta;
            if (Math.abs(prospectiveNet) > maxNet) {
                String reason = String.format(
                        "pretrade max_net_position breach on %s: prospective_net=%.4f > max_net=%.4f (current_net=%.4f, delta=%.4f, verb=%s)",
                        instrument, prospectiveNet, maxNet, currentNet, signedDelta, verb);
                logger.warn(reason);
                return buildRejection(orderRequest, reason);
            }
        }

        // ── 3. max_gross_position check ──────────────────────────────────────────
        double maxGross = getThreshold(instrument, instrumentMaxGrossPosition, defaultMaxGrossPosition);
        if (maxGross < Double.MAX_VALUE) {
            double currentGross = intradayGrossPosition.getOrDefault(instrument, 0.0);
            double prospectiveGross = currentGross + Math.abs(quantityDelta);
            if (prospectiveGross > maxGross) {
                String reason = String.format(
                        "pretrade max_gross_position breach on %s: prospective_gross=%.4f > max_gross=%.4f (current_gross=%.4f, delta=%.4f)",
                        instrument, prospectiveGross, maxGross, currentGross, quantityDelta);
                logger.warn(reason);
                return buildRejection(orderRequest, reason);
            }
        }

        // All checks passed – track the order as active
        activeOrderQuantity.put(orderRequest.getClientOrderId(), orderQty);
        return null;
    }

    /**
     * Updates intraday position tracking when an execution report is received.
     * This must be called for every execution report so that fill-based positions stay accurate.
     *
     * @param executionReport the execution report to process
     */
    public void onExecutionReport(ExecutionReport executionReport) {
        rolloverIfNewDay();

        String instrument = executionReport.getInstrument();
        ExecutionReportStatus status = executionReport.getExecutionReportStatus();
        String clOrdId = executionReport.getClientOrderId();

        // Track active orders ─────────────────────────────────────────────────────
        if (status == ExecutionReportStatus.Active) {
            activeOrderQuantity.put(clOrdId, executionReport.getQuantity());
        }

        // Remove from active tracking when the order is terminal ─────────────────
        if (status == ExecutionReportStatus.Cancelled
                || status == ExecutionReportStatus.CompletelyFilled
                || status == ExecutionReportStatus.Rejected) {
            activeOrderQuantity.remove(clOrdId);
        }

        // Update intraday position from fills ────────────────────────────────────
        if (!ExecutionReport.isTradeStatus(executionReport)) {
            return; // only PartialFilled / CompletelyFilled carry fill quantities
        }

        double filledQty = executionReport.getLastQuantity();
        if (filledQty <= 0) {
            return;
        }

        Verb verb = executionReport.getVerb();
        double signedFill = (verb == Verb.Buy) ? filledQty : -filledQty;

        intradayNetPosition.merge(instrument, signedFill, Double::sum);
        intradayGrossPosition.merge(instrument, Math.abs(filledQty), Double::sum);

        logger.debug("Position update {}: net={} gross={} (fill={} {})",
                instrument,
                intradayNetPosition.get(instrument),
                intradayGrossPosition.get(instrument),
                verb, filledQty);
    }

    /**
     * Resets all intraday position and active-order tracking. Useful at the start of a new
     * trading session when the controller is managed externally.
     */
    public void resetIntradayState() {
        intradayNetPosition.clear();
        intradayGrossPosition.clear();
        activeOrderQuantity.clear();
        currentDay = LocalDate.now();
        logger.info("PreTradeController intraday state reset for {}", currentDay);
    }

    // ── accessors (mainly for testing / monitoring) ─────────────────────────────

    public double getIntradayNetPosition(String instrument) {
        return intradayNetPosition.getOrDefault(instrument, 0.0);
    }

    public double getIntradayGrossPosition(String instrument) {
        return intradayGrossPosition.getOrDefault(instrument, 0.0);
    }

    public double getEffectiveMaxNetPosition(String instrument) {
        return getThreshold(instrument, instrumentMaxNetPosition, defaultMaxNetPosition);
    }

    public double getEffectiveMaxGrossPosition(String instrument) {
        return getThreshold(instrument, instrumentMaxGrossPosition, defaultMaxGrossPosition);
    }

    public double getEffectiveMaxQuantity(String instrument) {
        return getThreshold(instrument, instrumentMaxQuantity, defaultMaxQuantity);
    }

    // ── private helpers ─────────────────────────────────────────────────────────

    private void rolloverIfNewDay() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDay)) {
            logger.info("PreTradeController day rollover {} -> {}. Resetting intraday state.", currentDay, today);
            resetIntradayState();
            currentDay = today;
        }
    }

    private static double getThreshold(String instrument, Map<String, Double> perInstrumentMap, double defaultValue) {
        return perInstrumentMap.getOrDefault(instrument, defaultValue);
    }

    private static ExecutionReport buildRejection(OrderRequest orderRequest, String reason) {
        ExecutionReport rejection = new ExecutionReport(orderRequest);
        if (orderRequest.getOrderRequestAction() == OrderRequestAction.Cancel) {
            rejection.setExecutionReportStatus(ExecutionReportStatus.CancelRejected);
        } else {
            rejection.setExecutionReportStatus(ExecutionReportStatus.Rejected);
        }
        rejection.setRejectReason(reason);
        return rejection;
    }

    private static double parseDouble(String value, double defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Could not parse pretrade threshold '{}', using default {}", value, defaultValue);
            return defaultValue;
        }
    }

    private void logConfiguration() {
        logger.info("PreTradeController initialised:");
        logger.info("  default max_net_position   = {}", defaultMaxNetPosition == Double.MAX_VALUE ? "disabled" : defaultMaxNetPosition);
        logger.info("  default max_gross_position = {}", defaultMaxGrossPosition == Double.MAX_VALUE ? "disabled" : defaultMaxGrossPosition);
        logger.info("  default max_quantity       = {}", defaultMaxQuantity == Double.MAX_VALUE ? "disabled" : defaultMaxQuantity);
        instrumentMaxNetPosition.forEach((k, v) -> logger.info("  max_net_position[{}]   = {}", k, v));
        instrumentMaxGrossPosition.forEach((k, v) -> logger.info("  max_gross_position[{}] = {}", k, v));
        instrumentMaxQuantity.forEach((k, v) -> logger.info("  max_quantity[{}]       = {}", k, v));
    }
}

