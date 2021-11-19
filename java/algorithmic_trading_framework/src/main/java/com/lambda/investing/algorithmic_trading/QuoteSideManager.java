package com.lambda.investing.algorithmic_trading;

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

import static com.lambda.investing.algorithmic_trading.Algorithm.EXECUTION_REPORT_LOCK;
import static com.lambda.investing.algorithmic_trading.Algorithm.LOG_LEVEL;

public class QuoteSideManager {

	private static long MAX_TIME_ERROR_MS = 1000 * 10;
	private static int MAX_CANCEL_REJ_DELETE = 5;

	Logger logger = LogManager.getLogger(QuoteSideManager.class);
	private Algorithm algorithm;
	private Instrument instrument;
	private Verb verb;

	private String clientOrderIdSent, clientOrderIdSentBackup;
	private volatile String activeClientOrderId, activeClientOrderIdToBeCanceled;
	private QuoteRequest lastQuoteSent, lastQuoteSentBackup;

	private Double lastPrice, lastQuantity;

	private Queue<String> cfTradesClientOrderId;
	boolean isDisablePending = false;
	private Map<String, Integer> counterCancelRej;
	private Queue<String> counterCancelRejIgnored;
	private Queue<String> cancelConfirmedOriginalClientOrderId;
	boolean isDisable = false;
	boolean stopOnCf = false;
	private volatile String clOrdIdPending = null;
	private Queue<String> lastClOrdIdSent;
	private long timestampError = Long.MIN_VALUE;

	public QuoteSideManager(Algorithm algorithm, Instrument instrument, Verb verb) {
		this.algorithm = algorithm;
		this.instrument = instrument;
		this.verb = verb;
		reset();
	}

	public void setStopOnCf(boolean stopOnCf) {
		this.stopOnCf = stopOnCf;
		logger.info("stopOnCf set as {} on {} QuoteSideManager", stopOnCf, this.verb);
	}

	public void reset() {
		clientOrderIdSent = null;
		activeClientOrderId = null;
		lastQuoteSent = null;
		lastPrice = null;
		lastQuantity = null;
		isDisablePending = false;
		counterCancelRej = new ConcurrentHashMap<>();
		cfTradesClientOrderId = EvictingQueue.create(60);
		counterCancelRejIgnored = EvictingQueue.create(200);
		lastClOrdIdSent = EvictingQueue.create(100);
		cancelConfirmedOriginalClientOrderId = EvictingQueue.create(200);
		timestampError = Long.MIN_VALUE;
	}

	private OrderRequest createOrderRequest(Instrument instrument, Verb verb, double price, double quantity) {
		String newClientOrderId = algorithm.generateClientOrderId();
		OrderRequest output = new OrderRequest();
		output.setAlgorithmInfo(algorithm.algorithmInfo);
		output.setInstrument(instrument.getPrimaryKey());
		output.setVerb(verb);
		output.setOrderRequestAction(OrderRequestAction.Send);
		output.setClientOrderId(newClientOrderId);
		output.setQuantity(quantity);
		output.setPrice(price);

		output.setTimestampCreation(algorithm.getCurrentTimestamp());

		output.setOrderType(OrderType.Limit);//limit for quoting
		output.setMarketOrderType(MarketOrderType.FAS);//default FAS

		return output;
	}

	public synchronized void quoteRequest(QuoteRequest quoteRequest) throws LambdaTradingException {
		//		synchronized (EXECUTION_REPORT_LOCK) {
		//			Already check on algorithm
		//			if(quoteRequest.getQuoteRequestAction().equals(QuoteRequestAction.On) && !algorithm.getAlgorithmState().equals(AlgorithmState.STARTED)){
		//				throw new LambdaTradingException("cant quote with algo not started");
		//			}
		if (clientOrderIdSent != null) {
			return;
			//				throw new LambdaTradingException("cant quote " + verb + " waiting to ER of " + clientOrderIdSent);
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
			//				throw new LambdaTradingException(
			//						"cant quote " + verb + " same price/quantity as before " + clientOrderIdSent);
		}

		OrderRequest orderRequest = createOrderRequest(instrument, verb, price, quantity);
		if (activeClientOrderId != null) {
			orderRequest.setOrderRequestAction(OrderRequestAction.Modify);
			orderRequest.setOrigClientOrderId(activeClientOrderId);
		}
		if (quantity == 0) {
			if (orderRequest.getOrigClientOrderId() != null && cancelConfirmedOriginalClientOrderId
					.contains(orderRequest.getOrigClientOrderId())) {
				isDisablePending = false;
				return;
				//					throw new LambdaTradingException(
				//							"trying to cancel already cancelled order " + orderRequest.getOrigClientOrderId());
			}
			orderRequest.setOrderRequestAction(OrderRequestAction.Cancel);
			if (activeClientOrderId == null) {
				//					makes no sense to do anything => return
				return;
				//					isDisablePending = true;
				//					throw new LambdaTradingException("trying to cancel quote not confirmed ");
			} else {
				orderRequest.setOrigClientOrderId(activeClientOrderId);
			}
		} else {
			isDisable = false;
		}

		//send the order
		//update variables late
		lastQuoteSentBackup = lastQuoteSent;
		clientOrderIdSentBackup = clientOrderIdSent;

		lastQuoteSent = quoteRequest;
		clientOrderIdSent = orderRequest.getClientOrderId();

		lastQuantity = quantity;
		lastPrice = price;

		try {
			if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
				logger.info("[{}] {}", new Date(orderRequest.getTimestampCreation()), orderRequest);
			}
			lastClOrdIdSent.add(orderRequest.getClientOrderId());
			clOrdIdPending = orderRequest.getClientOrderId();
			algorithm.sendOrderRequest(orderRequest);
			timestampError = Long.MIN_VALUE;
		} catch (LambdaTradingException e) {
			logger.warn("[{}] can't send {} {}", new Date(orderRequest.getTimestampCreation()),
					orderRequest.getClientOrderId(), e.getMessage());
			//update variables late
			clOrdIdPending = null;
			lastQuoteSent = lastQuoteSentBackup;
			clientOrderIdSent = clientOrderIdSentBackup;
			lastQuantity = null;
			lastPrice = null;
			if (timestampError == Long.MIN_VALUE) {
				timestampError = orderRequest.getTimestampCreation();
			} else if (orderRequest.getTimestampCreation() - timestampError > MAX_TIME_ERROR_MS) {
				//desperate measure!
				//send cancel!
				logger.error("time in error >MAX_TIME_ERROR_MS {} -> cancel and restart side  ", MAX_TIME_ERROR_MS);
				orderRequest.setOrderRequestAction(OrderRequestAction.Cancel);
				logger.error("[{}] {}", new Date(orderRequest.getTimestampCreation()), orderRequest);
				try {
					algorithm.sendOrderRequest(orderRequest);
				} catch (LambdaException ex) {

				}
				reset();

			}
		} catch (Exception e) {
			logger.error("[{}] Error sending quote {} ", new Date(orderRequest.getTimestampCreation()),
					orderRequest.getClientOrderId(), e);
			throw e;
		}

		//		}
	}

	public void unquoteSide() throws LambdaTradingException {
		QuoteRequest lastQuote = lastQuoteSent;
		if (isDisable) {
			return;
		}
		if (clOrdIdPending != null) {
			//reject this update
			return;
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

		}
		if (LOG_LEVEL > LogLevels.SOME_ITERATION_LOG.ordinal()) {
			logger.info("[{}] {}-{}  {}", new Date(executionReport.getTimestampCreation()),
					executionReport.getClientOrderId(), executionReport.getExecutionReportStatus(), executionReport);
		}

		boolean isActive =
				executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Active) || executionReport
						.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled);
		boolean isInactive =
				executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Cancelled) || executionReport
						.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected) || executionReport
						.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled);

		boolean isCancelRej = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CancelRejected);

		boolean isFilled = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled)
				|| executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled);

		String clientOrderIdRecevied = executionReport.getClientOrderId();

		if (clientOrderId.equalsIgnoreCase(clientOrderIdSent)) {
			//remove the client
			clientOrderIdSent = null;
		}

		if (isActive) {
			if (!cfTradesClientOrderId.contains(clientOrderIdRecevied)) {
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

		if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Cancelled)) {
			cancelConfirmedOriginalClientOrderId.add(executionReport.getOrigClientOrderId());
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
				cfTradesClientOrderId.add(executionReport.getClientOrderId());
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
					counterCancelRejIgnored.add(executionReport.getOrigClientOrderId());
					counterCancelRej.remove(executionReport.getOrigClientOrderId());
				} else {
					counterCancelRej.put(executionReport.getOrigClientOrderId(), counter + 1);
				}
			}

		}

		return true;

	}

}
