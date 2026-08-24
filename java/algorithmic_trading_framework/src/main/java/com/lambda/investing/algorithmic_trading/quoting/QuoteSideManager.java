package com.lambda.investing.algorithmic_trading.quoting;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.LogLevels;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.exception.LambdaException;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.trading.*;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.Collection;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.lambda.investing.algorithmic_trading.Algorithm.LOG_LEVEL;

public class QuoteSideManager {

    public static int MAX_SIZE_LAST_CLORDID_SENT = 200;
    private static final long MAX_TIME_ERROR_MS = 1000 * 30;
    private static final int MAX_CANCEL_REJ_DELETE = 5;
    private static final long SLEEP_AFTER_REJ_MS = 500;

    Logger logger = LogManager.getLogger(QuoteSideManager.class);
    private final Algorithm algorithm;
    private final Instrument instrument;
    private final Verb verb;

    /**
     * Gate: non-null while we are waiting for an ER for this clOrdId.
     * Volatile so the quoting thread can do a cheap read before entering synchronized.
     */
    private volatile String clientOrderIdSent;
    /**
     * OrderRequestAction (New/Modify/Cancel) associated with {@code clientOrderIdSent}.
     * Needed so that, if the ER for it never arrives and the stuck-state timeout fires,
     * we know whether the in-flight request we are abandoning was a Cancel. If it was,
     * {@code activeClientOrderId} cannot be trusted anymore (the broker may have already
     * cancelled it) and must not be used as the OrigClientOrderId of a subsequent Modify.
     */
    private volatile OrderRequestAction clientOrderIdSentAction;
    private volatile String activeClientOrderId;
    private volatile QuoteRequest lastQuoteSent;
    private volatile Double lastPrice, lastQuantity;

    // ---- O(1) concurrent lookup sets ----------------------------------------
    /**
     * Parallel set for lastClOrdIdSent EvictingQueue.
     * The queue is kept only for the external getter used by QuoteManager;
     * all contains() calls use this set instead (O(1) vs O(n)).
     */
    private Set<String> lastClOrdIdSentSet;
    /**
     * Evicting queue kept solely for {@link #getLastClOrdIdSent()} (QuoteManager read).
     */
    private Queue<String> lastClOrdIdSent;

    /**
     * Filled / partial-fill clOrdIds — O(1) lookup, replaces EvictingQueue.
     */
    private Set<String> cfTradesSet;

    /**
     * origClOrdIds whose Cancel was confirmed — never send Modify/Cancel against them again.
     */
    private Set<String> cancelConfirmedSet;

    /**
     * origClOrdIds for which we have already given up on repeated CancelRej.
     */
    private Set<String> cancelRejIgnoredSet;
    // -------------------------------------------------------------------------

    protected Queue<String> cfTradesClientOrderId;   // kept for getCfTradesClientOrderId() getter only
    volatile boolean isDisablePending = false;
    private Map<String, Integer> counterCancelRej;
    volatile boolean isDisable = false;
    boolean stopOnCf = false;
    private volatile String clOrdIdPending = null;

    private long timestampError = Long.MIN_VALUE;
    /**
     * Timestamp (ms) when {@code clientOrderIdSent} was last set.
     * Volatile so the timeout check in quoteRequest is always fresh.
     */
    private volatile long clientOrderIdSentTimestamp = Long.MIN_VALUE;
    private volatile Date sleepUntil = null;

    private boolean isResetting = false;

    public QuoteSideManager(Algorithm algorithm, Instrument instrument, Verb verb) {
        this.algorithm = algorithm;
        this.instrument = instrument;
        this.verb = verb;
        reset();
    }

    public Double getLastPrice() {
        return lastPrice;
    }

    public Double getLastQuantity() { return lastQuantity; }

    public void setStopOnCf(boolean stopOnCf) {
        this.stopOnCf = stopOnCf;
        if (algorithm.isVerbose()) {
            logger.info("stopOnCf set as {} on {} QuoteSideManager", stopOnCf, this.verb);
        }
    }

    public void reset() {
        try {
            isResetting = true;
            unquoteSide();
        } catch (LambdaTradingException e) {
            throw new RuntimeException(e);
        }
        isResetting = false;

        clientOrderIdSent = null;
        clientOrderIdSentTimestamp = Long.MIN_VALUE;
        clientOrderIdSentAction = null;
        activeClientOrderId = null;
        lastQuoteSent = null;
        lastPrice = null;
        lastQuantity = null;
        isDisablePending = false;
        counterCancelRej = new ConcurrentHashMap<>();

        // Queues kept only for external getters
        cfTradesClientOrderId = EvictingQueue.create(60);
        lastClOrdIdSent = EvictingQueue.create(MAX_SIZE_LAST_CLORDID_SENT);

        // O(1) lookup sets (unbounded; trading sessions are finite and entry count is small)
        lastClOrdIdSentSet = ConcurrentHashMap.newKeySet(MAX_SIZE_LAST_CLORDID_SENT);
        cfTradesSet = ConcurrentHashMap.newKeySet(64);
        cancelConfirmedSet = ConcurrentHashMap.newKeySet(200);
        cancelRejIgnoredSet = ConcurrentHashMap.newKeySet(200);

        timestampError = Long.MIN_VALUE;
        sleepUntil = null;
    }

    public void sleepQuoting(Date wakeupTime) {
        // Racy but idempotent — worst case we set the same or a slightly older value,
        // which is harmless and avoids a synchronized block on this hot path.
        Date current = sleepUntil;
        if (current == null || current.getTime() < wakeupTime.getTime()) {
            sleepUntil = wakeupTime;
        }
    }

    /**
     * Returns the evicting queue used by QuoteManager to copy sent clOrdIds.
     */
    public Queue<String> getLastClOrdIdSent() {
        return lastClOrdIdSent;
    }

    /**
     * Returns the CF-trade collection.  Backed by a {@link ConcurrentHashMap} key-set
     * so contains() is O(1) and thread-safe.
     */
    public Collection<String> getCfTradesClientOrderId() { return cfTradesSet; }

    private OrderRequest createOrderRequest(Instrument instrument, Verb verb, double price, double quantity, long referenceTimestamp) {
        String newClientOrderId = algorithm.generateClientOrderId();
        OrderRequest output = new OrderRequest();
        output.setAlgorithmInfo(algorithm.getAlgorithmInfo());
        output.setInstrument(instrument.getPrimaryKey());
        output.setVerb(verb);
        output.setOrderRequestAction(OrderRequestAction.Send);
        output.setClientOrderId(newClientOrderId);
        output.setQuantity(quantity);
        output.setPrice(price);
        output.setReferenceTimestamp(referenceTimestamp);
        output.setTimestampCreation(algorithm.getCurrentTimestamp());
        output.setOrderType(OrderType.Limit);
        output.setMarketOrderType(MarketOrderType.FAS);
        return output;
    }

    public void quoteRequest(QuoteRequest quoteRequest) throws LambdaTradingException {
        // ----------------------------------------------------------------
        // PHASE 1 – state mutation under the lock, NO external calls.
        // ----------------------------------------------------------------
        final OrderRequest orderRequest;
        final QuoteRequest lastQuoteSentBackupLocal;
        final String clientOrderIdSentBackupLocal;
        final OrderRequestAction clientOrderIdSentActionBackupLocal;

        synchronized (this) {
            if (clientOrderIdSent != null) {
                long now = algorithm.getCurrentTimestamp();
                if (clientOrderIdSentTimestamp != Long.MIN_VALUE
                        && now - clientOrderIdSentTimestamp > MAX_TIME_ERROR_MS) {
                    logger.warn("[{}] {} clientOrderIdSent={} ({}) stuck >{}ms without ER — clearing stuck state",
                            algorithm.getCurrentTime(), verb, clientOrderIdSent, clientOrderIdSentAction, MAX_TIME_ERROR_MS);
                    if (clientOrderIdSentAction == OrderRequestAction.Cancel && activeClientOrderId != null) {
                        // The stuck request was a Cancel of activeClientOrderId: we never got the ER,
                        // so we cannot know for certain whether the broker accepted it or not. Since a
                        // Modify against an order that the broker has (or is about to have) cancelled
                        // will be rejected (as the broker no longer considers it "active"), it is safer
                        // to assume the cancel went through and forget this order id. The next
                        // quoteRequest() will then send a brand-new order instead of an invalid Modify.
                        logger.warn("[{}] {} stuck request was a Cancel of {} — assuming it was accepted, "
                                        + "dropping activeClientOrderId to avoid an invalid Modify",
                                algorithm.getCurrentTime(), verb, activeClientOrderId);
                        activeClientOrderId = null;
                        lastPrice = null;
                        lastQuantity = null;
                    }
                    clientOrderIdSent = null;
                    clientOrderIdSentTimestamp = Long.MIN_VALUE;
                    clientOrderIdSentAction = null;
                    clOrdIdPending = null;
                } else {
                    return;
                }
            }

            Instrument instrument = quoteRequest.getInstrument();
            double price = quoteRequest.getBidPrice();
            double quantity = quoteRequest.getBidQuantity();
            if (verb.equals(Verb.Sell)) {
                price = quoteRequest.getAskPrice();
                quantity = quoteRequest.getAskQuantity();
            }

            if (lastPrice != null && lastPrice == price && lastQuantity != null && lastQuantity == quantity) {
                return;
            }

            orderRequest = createOrderRequest(instrument, verb, price, quantity, quoteRequest.getReferenceTimestamp());
            orderRequest.setFreeText(quoteRequest.getFreeText());
            if (activeClientOrderId != null) {
                orderRequest.setOrderRequestAction(OrderRequestAction.Modify);
                orderRequest.setOrigClientOrderId(activeClientOrderId);
            }
            if (Math.abs(quantity) < 1e-6) {
                if (orderRequest.getOrigClientOrderId() != null
                        && cancelConfirmedSet.contains(orderRequest.getOrigClientOrderId())) {   // O(1)
                    isDisablePending = false;
                    return;
                }
                orderRequest.setOrderRequestAction(OrderRequestAction.Cancel);
                if (activeClientOrderId == null) {
                    return;
                } else {
                    orderRequest.setOrigClientOrderId(activeClientOrderId);
                }
            } else {
                isDisable = false;
            }

            lastQuoteSentBackupLocal = lastQuoteSent;
            clientOrderIdSentBackupLocal = clientOrderIdSent;
            clientOrderIdSentActionBackupLocal = clientOrderIdSentAction;

            lastQuoteSent = quoteRequest;
            clientOrderIdSent = orderRequest.getClientOrderId();
            clientOrderIdSentTimestamp = algorithm.getCurrentTimestamp();
            clientOrderIdSentAction = orderRequest.getOrderRequestAction();
            lastQuantity = quantity;
            lastPrice = price;

            if (orderRequest.getClientOrderId() != null) {
                lastClOrdIdSent.offer(orderRequest.getClientOrderId());
                lastClOrdIdSentSet.add(orderRequest.getClientOrderId());   // O(1) insert
                clOrdIdPending = orderRequest.getClientOrderId();
            }
        } // <-- lock released here, before any external call

        // ----------------------------------------------------------------
        // PHASE 2 – send the order WITHOUT holding "this".
        // ----------------------------------------------------------------
        if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
            logger.info("[{}] {}", orderRequest.getDateCreation(), orderRequest);
        }

        if (orderRequest.getClientOrderId() == null) {
            logger.warn("QuoteRequest {} has null clientOrderId, this should not happen", orderRequest);
            return;
        }

        try {
            algorithm.sendOrderRequest(orderRequest);
            synchronized (this) {
                timestampError = Long.MIN_VALUE;
            }
        } catch (LambdaTradingException e) {
            logger.warn("[{}] can't send {} {}", orderRequest.getDateCreation(),
                    orderRequest.getClientOrderId(), e.getMessage());

            final boolean shouldReset;
            synchronized (this) {
                clOrdIdPending = null;
                lastQuoteSent = lastQuoteSentBackupLocal;
                clientOrderIdSent = clientOrderIdSentBackupLocal;
                clientOrderIdSentAction = clientOrderIdSentActionBackupLocal;
                lastQuantity = null;
                lastPrice = null;
                if (timestampError == Long.MIN_VALUE) {
                    timestampError = orderRequest.getTimestampCreation();
                    shouldReset = false;
                } else if (orderRequest.getTimestampCreation() - timestampError > MAX_TIME_ERROR_MS) {
                    shouldReset = !isResetting;
                } else {
                    shouldReset = false;
                }
            }

            if (shouldReset) {
                logger.error("time in error >MAX_TIME_ERROR_MS {} -> cancel and restart side", MAX_TIME_ERROR_MS);
                orderRequest.setOrderRequestAction(OrderRequestAction.Cancel);
                logger.error("[{}] {}", orderRequest.getDateCreation(), orderRequest);
                try {
                    algorithm.sendOrderRequest(orderRequest);
                } catch (LambdaException ex) {
                    // best-effort cancel
                }
                reset();
            }
        } catch (Exception e) {
            logger.error("[{}] Error sending quote {}", orderRequest.getDateCreation(),
                    orderRequest.getClientOrderId(), e);
            throw e;
        }
    }

    public String getClientOrderIdSent() {
        return clientOrderIdSent;
    }

    /**
     * Cancels the active quote on this side.
     * <p>
     * All fields accessed here are {@code volatile} so no method-level lock is needed.
     * {@link #quoteRequest} acquires {@code synchronized(this)} when it commits state,
     * providing the necessary memory barrier on the write path.
     */
    public void unquoteSide() throws LambdaTradingException {
        if (isDisable) {
            return;
        }
        if (clOrdIdPending != null) {
            long now = algorithm.getCurrentTimestamp();
            if (clientOrderIdSentTimestamp != Long.MIN_VALUE
                    && now - clientOrderIdSentTimestamp > MAX_TIME_ERROR_MS) {
                logger.warn("[{}] {} clOrdIdPending={} stuck >{}ms without ER — clearing to allow unquote",
                        algorithm.getCurrentTime(), verb, clOrdIdPending, MAX_TIME_ERROR_MS);
                synchronized (this) {
                    clOrdIdPending = null;
                    clientOrderIdSent = null;
                    clientOrderIdSentTimestamp = Long.MIN_VALUE;
                    clientOrderIdSentAction = null;
                }
            } else {
                return;
            }
        }
        QuoteRequest lastQuote = lastQuoteSent;
        if (lastQuote != null) {
            if (verb.equals(Verb.Buy)) {
                lastQuote.setBidQuantity(0.);
            } else {
                lastQuote.setAskQuantity(0.);
            }
            try {
                quoteRequest(lastQuote);
            } catch (LambdaTradingException e) {
                // best-effort
            }
        }
    }

    public Date getSleepUntil() {
        return sleepUntil;
    }

    /**
     * Processes an incoming execution report.
     * <p>
     * Hot-path design notes:
     * <ul>
     *   <li>Fast-path rejects (verb, instrument, clOrdId membership) run without any lock.</li>
     *   <li>clOrdId membership check is O(1) via {@link #lastClOrdIdSentSet} (was O(n) on EvictingQueue).</li>
     *   <li>Only the tiny critical section that updates {@code activeClientOrderId} and
     *       clears {@code clientOrderIdSent} holds {@code synchronized(this)}, so the quoting
     *       thread is not blocked during logging or counter updates.</li>
     *   <li>{@code activeClientOrderId} is written BEFORE {@code clientOrderIdSent} is cleared,
     *       ensuring the quoting thread always sees a consistent origClientOrderId.</li>
     *   <li>{@code new Date()} is never allocated; raw epoch-ms is passed to the logger.</li>
     * </ul>
     */
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        // ---- Lock-free fast-path filters ----
        if (executionReport.getVerb() != null && !executionReport.getVerb().equals(verb)) {
            return false;
        }
        final String clientOrderId = executionReport.getClientOrderId();
        final String origClientOrderId = executionReport.getOrigClientOrderId();

        // Check if this ER belongs to us: either clientOrderId or origClientOrderId must be in our sent set
        boolean belongsToUs = lastClOrdIdSentSet.contains(clientOrderId);
        if (!belongsToUs && origClientOrderId != null && !origClientOrderId.isEmpty()) {
            belongsToUs = lastClOrdIdSentSet.contains(origClientOrderId);
            if (belongsToUs) {
                logger.warn("onExecutionReportUpdate clientOrderId:{} not found in lastClOrdIdSentSet but found origClientOrderId: {} ", clientOrderId, origClientOrderId);
            }
        }
        if (!belongsToUs) {
            return false;
        }

        final Instrument erInstrument = Instrument.getInstrument(executionReport.getInstrument());
        if (!erInstrument.equals(this.instrument)) {
            return false;
        }

        // Volatile write — safe without a full lock; worst-case double-clear is harmless.
        if (clientOrderId.equalsIgnoreCase(clOrdIdPending)) {
            clOrdIdPending = null;
        }

        // ---- Status flags (pure computation, no side effects) ----
        final ExecutionReportStatus status = executionReport.getExecutionReportStatus();
        final boolean isRejected = status.equals(ExecutionReportStatus.Rejected);
        final boolean isActive = status.equals(ExecutionReportStatus.Active)
                || status.equals(ExecutionReportStatus.PartialFilled);
        final boolean newOrderRejected = isRejected && activeClientOrderId == null;
        final boolean isInactive = status.equals(ExecutionReportStatus.Cancelled)
                || newOrderRejected
                || status.equals(ExecutionReportStatus.CompletelyFilled);
        final boolean isCancelRej = status.equals(ExecutionReportStatus.CancelRejected);
        final boolean isFilled = status.equals(ExecutionReportStatus.PartialFilled)
                || status.equals(ExecutionReportStatus.CompletelyFilled);

        // ---- Logging (avoid new Date() allocation on the hot path) ----
        if (isRejected) {
            boolean previousCompleteCanceledOrderTrade = cfTradesSet.contains(clientOrderId) || cancelConfirmedSet.contains(clientOrderId);
            if (!previousCompleteCanceledOrderTrade) {
                if (logger.isWarnEnabled()) {
                    logger.warn("[{}] {}-{}  {}", executionReport.getDateCreation(),
                            clientOrderId, status, executionReport);
                }
                sleepQuoting(new Date(algorithm.getCurrentTime().getTime() + SLEEP_AFTER_REJ_MS));
            } else {
                logger.info("[{}] rejection of previous trade/canceled {}-{}  {}", executionReport.getDateCreation(),
                        clientOrderId, status, executionReport);
            }
        } else if (status.equals(ExecutionReportStatus.Cancelled)) {
            logger.warn("[{}] {}-{} order is no longer alive — a fill may still arrive for this clOrdId  {}",
                    executionReport.getDateCreation(), clientOrderId, status, executionReport);
        } else if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal() && logger.isInfoEnabled()) {
            logger.info("[{}] {}-{}  {}", executionReport.getDateCreation(),
                    clientOrderId, status, executionReport);
        }

        // ----------------------------------------------------------------
        // CRITICAL SECTION
        // Only the state that the quoting thread reads (activeClientOrderId,
        // clientOrderIdSent) is updated here.  All other state is updated
        // outside the lock using volatile writes or thread-safe collections.
        //
        // ORDER MATTERS:
        //   1. Update activeClientOrderId (and handle isDisablePending).
        //   2. Handle isInactive → clear activeClientOrderId if needed.
        //   3. Clear clientOrderIdSent LAST.
        //
        // The quoting thread holds synchronized(this) in quoteRequest() and
        // reads both fields; it must never see clientOrderIdSent==null with a
        // stale (already-replaced) activeClientOrderId.
        // ----------------------------------------------------------------
        synchronized (this) {
            if (isActive) {
                if (!cfTradesSet.contains(clientOrderId) && !cancelConfirmedSet.contains(clientOrderId)) {
                    activeClientOrderId = clientOrderId;
                    if (isDisablePending) {
                        logger.info("receive active to immediately cancel! {}", activeClientOrderId);
                        try {
                            isDisablePending = false;
                            unquoteSide();   // reentrant — quoteRequest will acquire this lock again
                        } catch (LambdaTradingException e) {
                            logger.error("cant unquote side {} ", verb, e);
                            isDisablePending = true;
                        }
                    }
                }
            }

            if (isInactive) {
                if (activeClientOrderId != null
                        && activeClientOrderId.equalsIgnoreCase(clientOrderId)) {
                    activeClientOrderId = null;
                    lastPrice = null;
                    lastQuantity = null;
                }
                if (activeClientOrderId != null
                        && activeClientOrderId.equalsIgnoreCase(executionReport.getOrigClientOrderId())) {
                    activeClientOrderId = null;
                    lastPrice = null;
                    lastQuantity = null;
                }
            }

            // Clear the "sent, awaiting ER" gate AFTER activeClientOrderId is stable.
            if (clientOrderId.equalsIgnoreCase(clientOrderIdSent)) {
                clientOrderIdSent = null;
                clientOrderIdSentTimestamp = Long.MIN_VALUE;
                clientOrderIdSentAction = null;
            }
        } // end critical section

        // ---- Non-critical updates (outside lock, thread-safe collections / volatile) ----
        if (status.equals(ExecutionReportStatus.Cancelled)) {
            cancelConfirmedSet.add(executionReport.getOrigClientOrderId()); // ConcurrentHashSet — O(1)
            isDisable = true;   // volatile write
        }

        if (isInactive && isFilled) {
            cfTradesClientOrderId.offer(clientOrderId); // getter-only queue, written by ER thread only
            cfTradesSet.add(clientOrderId);              // O(1)
            if (stopOnCf) {
                try {
                    unquoteSide();
                } catch (LambdaTradingException e) {
                    logger.error("cant unquote side {} on Cf trade", this.verb, e);
                }
            }
        }

        if (isCancelRej) {
            final String origId = executionReport.getOrigClientOrderId();
            if (!cancelRejIgnoredSet.contains(origId)) {   // O(1) — was O(n)
                int counter = counterCancelRej.getOrDefault(origId, 0);
                if (logger.isWarnEnabled()) {
                    logger.warn("{} cancelRej {} on {} {}", counter, status, clientOrderId, origId);
                }
                if (counter > MAX_CANCEL_REJ_DELETE) {
                    logger.error("{} cancelRej clean! on {} {}", counter, clientOrderId, origId);
                    synchronized (this) {
                        activeClientOrderId = null;
                        lastPrice = null;
                        lastQuantity = null;
                    }
                    cancelRejIgnoredSet.add(origId);
                    counterCancelRej.remove(origId);
                } else {
                    counterCancelRej.put(origId, counter + 1);
                }
            }
        }

        return true;
    }

}
