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

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import static com.lambda.investing.algorithmic_trading.Algorithm.LOG_LEVEL;

public class QuoteSideManager {

    public static int MAX_SIZE_LAST_CLORDID_SENT = 200;
    private static long MAX_TIME_ERROR_MS = 1000 * 10;
    private static int MAX_CANCEL_REJ_DELETE = 5;
    private static long SLEEP_AFTER_REJ_MS = 500;

    Logger logger = LogManager.getLogger(QuoteSideManager.class);
    private Algorithm algorithm;
    private Instrument instrument;
    private Verb verb;

    private String clientOrderIdSent;
    private volatile String activeClientOrderId, activeClientOrderIdToBeCanceled;
    private QuoteRequest lastQuoteSent;

    private Double lastPrice, lastQuantity;

    protected Queue<String> cfTradesClientOrderId;
    boolean isDisablePending = false;
    private Map<String, Integer> counterCancelRej;
    private Queue<String> counterCancelRejIgnored;
    private Queue<String> cancelConfirmedOriginalClientOrderId;
    boolean isDisable = false;
    boolean stopOnCf = false;
    private volatile String clOrdIdPending = null;
    private Queue<String> lastClOrdIdSent;
    private long timestampError = Long.MIN_VALUE;
    /**
     * Timestamp (ms) when {@code clientOrderIdSent} was last set.
     * Used to detect orders whose ER was never received so we can recover
     * after {@link #MAX_TIME_ERROR_MS} without being permanently stuck.
     */
    private long clientOrderIdSentTimestamp = Long.MIN_VALUE;
    private Date sleepUntil = null;

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

    public Double getLastQuantity() {
        return lastQuantity;
    }

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
        activeClientOrderId = null;
        lastQuoteSent = null;
        lastPrice = null;
        lastQuantity = null;
        isDisablePending = false;
        counterCancelRej = new ConcurrentHashMap<>();
        cfTradesClientOrderId = EvictingQueue.create(60);
        counterCancelRejIgnored = EvictingQueue.create(200);
        lastClOrdIdSent = EvictingQueue.create(MAX_SIZE_LAST_CLORDID_SENT);
        cancelConfirmedOriginalClientOrderId = EvictingQueue.create(200);
        timestampError = Long.MIN_VALUE;
        sleepUntil = null;

    }

    public void sleepQuoting(Date wakeupTime) {
        if (sleepUntil == null) {
            sleepUntil = wakeupTime;
            return;
        }
        if (sleepUntil.getTime() < wakeupTime.getTime()) {
            sleepUntil = wakeupTime;
        }
    }

    public Queue<String> getLastClOrdIdSent() {
        return lastClOrdIdSent;
    }

    public Queue<String> getCfTradesClientOrderId() {
        return cfTradesClientOrderId;
    }

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

        output.setOrderType(OrderType.Limit);//limit for quoting
        output.setMarketOrderType(MarketOrderType.FAS);//default FAS

        return output;
    }

    public void quoteRequest(QuoteRequest quoteRequest) throws LambdaTradingException {
        // ----------------------------------------------------------------
        // PHASE 1 – state mutation under the lock, NO external calls.
        //
        // Holding "this" while calling algorithm.sendOrderRequest() caused a
        // classic lock-inversion deadlock:
        //   Thread A (quoting): holds "this", waits for algorithm lock inside sendOrderRequest.
        //   Thread B (ER recv): holds algorithm lock, calls onExecutionReportUpdate →
        //                       unquoteSide → quoteRequest → waits for "this".
        //
        // Fix: prepare the OrderRequest and commit state while holding "this",
        // then release the lock and send without holding it.  Rollback is done
        // in a second synchronized block if the send fails.
        // ----------------------------------------------------------------
        final OrderRequest orderRequest;
        final QuoteRequest lastQuoteSentBackupLocal;
        final String clientOrderIdSentBackupLocal;

        synchronized (this) {
            if (clientOrderIdSent != null) {
                // Safety net: if we have been waiting for an ER longer than MAX_TIME_ERROR_MS,
                // the ER was probably lost in transit.  Clear the stuck state so quoting can
                // resume; the active order on the exchange (if any) will be re-synced on the
                // next depth update cycle.
                long now = algorithm.getCurrentTimestamp();
                if (clientOrderIdSentTimestamp != Long.MIN_VALUE
                        && now - clientOrderIdSentTimestamp > MAX_TIME_ERROR_MS) {
                    logger.warn("[{}] {} clientOrderIdSent={} stuck for >{}ms without ER — clearing stuck state",
                            new Date(now), verb, clientOrderIdSent, MAX_TIME_ERROR_MS);
                    clientOrderIdSent = null;
                    clientOrderIdSentTimestamp = Long.MIN_VALUE;
                    clOrdIdPending = null;
                    // Do NOT clear activeClientOrderId: the order may still be live on the
                    // exchange; the next quoteRequest will issue a Modify against it.
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
                //if same price dont send the same!
                return;
            }

            orderRequest = createOrderRequest(instrument, verb, price, quantity, quoteRequest.getReferenceTimestamp());
            orderRequest.setFreeText(quoteRequest.getFreeText());
            if (activeClientOrderId != null) {
                orderRequest.setOrderRequestAction(OrderRequestAction.Modify);
                orderRequest.setOrigClientOrderId(activeClientOrderId);
            }
            if (Math.abs(quantity) < 1e-6) {
                if (orderRequest.getOrigClientOrderId() != null && cancelConfirmedOriginalClientOrderId
                        .contains(orderRequest.getOrigClientOrderId())) {
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

            // Commit state — lock is still held here, no external call yet.
            lastQuoteSentBackupLocal = lastQuoteSent;
            clientOrderIdSentBackupLocal = clientOrderIdSent;

            lastQuoteSent = quoteRequest;
            clientOrderIdSent = orderRequest.getClientOrderId();
            clientOrderIdSentTimestamp = algorithm.getCurrentTimestamp();

            lastQuantity = quantity;
            lastPrice = price;

            if (orderRequest.getClientOrderId() != null) {
                lastClOrdIdSent.offer(orderRequest.getClientOrderId());
                clOrdIdPending = orderRequest.getClientOrderId();
            }
        } // <-- lock released here, before any external call

        // ----------------------------------------------------------------
        // PHASE 2 – send the order WITHOUT holding "this".
        // ----------------------------------------------------------------
        if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
            logger.info("[{}] {}", new Date(orderRequest.getTimestampCreation()), orderRequest);
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
            logger.warn("[{}] can't send {} {}", new Date(orderRequest.getTimestampCreation()),
                    orderRequest.getClientOrderId(), e.getMessage());

            final boolean shouldReset;
            synchronized (this) {
                // Rollback state
                clOrdIdPending = null;
                lastQuoteSent = lastQuoteSentBackupLocal;
                clientOrderIdSent = clientOrderIdSentBackupLocal;
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
                // Desperate measure: send a cancel, then reset the side.
                logger.error("time in error >MAX_TIME_ERROR_MS {} -> cancel and restart side  ", MAX_TIME_ERROR_MS);
                orderRequest.setOrderRequestAction(OrderRequestAction.Cancel);
                logger.error("[{}] {}", new Date(orderRequest.getTimestampCreation()), orderRequest);
                try {
                    algorithm.sendOrderRequest(orderRequest);
                } catch (LambdaException ex) {
                    // best-effort cancel
                }
                reset(); // avoid StackOverflow: guarded by shouldReset (!isResetting)
            }
        } catch (Exception e) {
            logger.error("[{}] Error sending quote {} ", new Date(orderRequest.getTimestampCreation()),
                    orderRequest.getClientOrderId(), e);
            throw e;
        }
    }

    public String getClientOrderIdSent() {
        return clientOrderIdSent;
    }

    public synchronized void unquoteSide() throws LambdaTradingException {
        QuoteRequest lastQuote = lastQuoteSent;
        if (isDisable) {
            return;
        }
        if (clOrdIdPending != null) {
            // If the pending order ER was lost, don't block unquote indefinitely.
            long now = algorithm.getCurrentTimestamp();
            if (clientOrderIdSentTimestamp != Long.MIN_VALUE
                    && now - clientOrderIdSentTimestamp > MAX_TIME_ERROR_MS) {
                logger.warn("[{}] {} clOrdIdPending={} stuck for >{}ms without ER — clearing to allow unquote",
                        new Date(now), verb, clOrdIdPending, MAX_TIME_ERROR_MS);
                clOrdIdPending = null;
                clientOrderIdSent = null;
                clientOrderIdSentTimestamp = Long.MIN_VALUE;
            } else {
                //reject this update
                return;
            }
        }
        if (lastQuote != null) {
            if (verb.equals(Verb.Buy)) {
                lastQuote.setBidQuantity(0.);
            }
            if (verb.equals(Verb.Sell)) {
                lastQuote.setAskQuantity(0.);
            }
            try {
                quoteRequest(lastQuote);
            } catch (LambdaTradingException e) {
                //				throw e;
            }
        }
    }

    public Date getSleepUntil() {
        return sleepUntil;
    }

    public synchronized boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        if (executionReport.getVerb() != null && !executionReport.getVerb().equals(verb)) {
            //is from the other side
            return false;
        }
        String clientOrderId = executionReport.getClientOrderId();
        if (!lastClOrdIdSent.contains(clientOrderId)) {
            //dont do nothing here! order not sent here!
            return false;
        }

        Instrument instrument = Instrument.getInstrument(executionReport.getInstrument());
        if (!instrument.equals(this.instrument)) {
            return false;
        }

        if (clOrdIdPending != null && executionReport.getClientOrderId().equalsIgnoreCase(clOrdIdPending)) {
            clOrdIdPending = null;
        }

        boolean isRejected = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected);

        if (isRejected) {
            logger.warn("[{}] {}-{}  {}", new Date(executionReport.getTimestampCreation()),
                    executionReport.getClientOrderId(), executionReport.getExecutionReportStatus(), executionReport);
            //maybe a trade is coming later
            Date wakeUpTime = new Date(algorithm.getCurrentTime().getTime() + SLEEP_AFTER_REJ_MS);
            sleepQuoting(wakeUpTime);

        } else {
            if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
                logger.info("[{}] {}-{}  {}", new Date(executionReport.getTimestampCreation()),
                        executionReport.getClientOrderId(), executionReport.getExecutionReportStatus(),
                        executionReport);
            }
        }
        boolean isActive =
                executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Active) || executionReport
                        .getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled);

        //replace rejected are active!
        boolean newOrderRejected = executionReport
                .getExecutionReportStatus().equals(ExecutionReportStatus.Rejected) && activeClientOrderId == null;

        boolean replaceOrderRejected = executionReport
                .getExecutionReportStatus().equals(ExecutionReportStatus.Rejected) && activeClientOrderId != null;

        boolean isInactive =
                executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Cancelled) || newOrderRejected || executionReport
                        .getExecutionReportStatus().equals(ExecutionReportStatus.CompletelyFilled);

        boolean isCancelRej = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CancelRejected);

        boolean isFilled = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled)
                || executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletelyFilled);

        String clientOrderIdRecevied = executionReport.getClientOrderId();

        // ----------------------------------------------------------------
        // IMPORTANT ordering: update activeClientOrderId BEFORE clearing
        // clientOrderIdSent so that the quoting thread — which spins on
        // clientOrderIdSent == null inside a synchronized block — always
        // sees a consistent (confirmed) activeClientOrderId as soon as it
        // is allowed to proceed.  Clearing clientOrderIdSent first created
        // a window where the quoting thread would pick up a stale
        // activeClientOrderId and send a Modify/Cancel against an order
        // that had already been replaced.
        // ----------------------------------------------------------------

        if (isActive) {
            if (!cfTradesClientOrderId.contains(clientOrderIdRecevied)
                    && !cancelConfirmedOriginalClientOrderId.contains(clientOrderIdRecevied)) {
                // do not resurrect an order that was already confirmed cancelled:
                // late PartialFilled reports can arrive after the Cancelled ER
                activeClientOrderId = executionReport.getClientOrderId();
                if (isDisablePending) {
                    logger.info("receive active to immediately cancel! {}", activeClientOrderId);
                    try {
                        isDisablePending = false;
                        unquoteSide();
                    } catch (LambdaTradingException e) {
                        logger.error("cant unquote side {} ", verb, e);
                        isDisablePending = true;
                    }

                }
            }

        }

        // Clear the "sent, waiting for ER" gate only after activeClientOrderId has
        // been updated above, ensuring atomicity for the quoting thread.
        if (clientOrderId.equalsIgnoreCase(clientOrderIdSent)) {
            clientOrderIdSent = null;
            clientOrderIdSentTimestamp = Long.MIN_VALUE;
        }

        if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Cancelled)) {
            cancelConfirmedOriginalClientOrderId.offer(executionReport.getOrigClientOrderId());
            isDisable = true;
        }
        if (isInactive) {
            //TODO add something to check verb!
            if (activeClientOrderId != null && activeClientOrderId
                    .equalsIgnoreCase(executionReport.getClientOrderId())) {
                activeClientOrderId = null;
                lastPrice = null;
                lastQuantity = null;
            }

            //in case of canceled
            if (activeClientOrderId != null && activeClientOrderId
                    .equalsIgnoreCase(executionReport.getOrigClientOrderId())) {
                activeClientOrderId = null;
                lastPrice = null;
                lastQuantity = null;
            }

            if (isFilled) {
                //here is only Cf
                cfTradesClientOrderId.offer(executionReport.getClientOrderId());
                if (stopOnCf) {
                    try {
                        unquoteSide();
                    } catch (LambdaTradingException e) {
                        logger.error("cant unquote side {} on Cf trade", this.verb, e);
                    }
                }

            }
        }
        if (isCancelRej) {
            if (!counterCancelRejIgnored.contains(executionReport.getOrigClientOrderId())) {
                int counter = counterCancelRej.getOrDefault(executionReport.getOrigClientOrderId(), 0);
                logger.warn("{} cancelRej {} on {} ", counter, executionReport.getExecutionReportStatus(),
                        executionReport.getClientOrderId(), executionReport.getOrigClientOrderId());

                if (counter > MAX_CANCEL_REJ_DELETE) {
                    // reset
                    logger.error("{} cancelRej clean! on {} ", counter, executionReport.getClientOrderId(),
                            executionReport.getOrigClientOrderId());
                    activeClientOrderId = null;
                    lastPrice = null;
                    lastQuantity = null;
                    counterCancelRejIgnored.offer(executionReport.getOrigClientOrderId());
                    counterCancelRej.remove(executionReport.getOrigClientOrderId());
                } else {
                    counterCancelRej.put(executionReport.getOrigClientOrderId(), counter + 1);
                }
            }

        }

        return true;

    }

}
