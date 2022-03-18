package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.lambda.investing.algorithmic_trading.Algorithm.EXECUTION_REPORT_LOCK;
import static com.lambda.investing.algorithmic_trading.QuoteSideManager.MAX_SIZE_LAST_CLORDID_SENT;

public class QuoteManager implements ExecutionReportListener, Runnable {

	private static boolean CHECK_PENDING_ORDERS_QUOTING = false;

	private static int MAX_LIMIT_WAITING_ER = 5;
	private static boolean UPDATE_QUOTE_BUFFER_THREAD = false;//if true will be update timely on a different thread

	Logger logger = LogManager.getLogger(QuoteManager.class);
	private Algorithm algorithm;
	private Instrument instrument;

	private QuoteRequest lastQuoteRequest;
	private QuoteSideManager bidQuoteSideManager;
	private QuoteSideManager askQuoteSideManager;
	private Queue<String> lastClOrdIdsSent;

	private int limitOrders = 2;

	public void setLimitOrders(int limitOrders) {
		this.limitOrders = limitOrders;
	}

	Thread quoteThread;
	private int counterWithoutResponse = 0;
	boolean stopOnCf = false;

	public void setStopOnCf(boolean stopOnCf) {
		this.stopOnCf = stopOnCf;
		this.bidQuoteSideManager.setStopOnCf(stopOnCf);
		this.askQuoteSideManager.setStopOnCf(stopOnCf);
	}

	public QuoteManager(Algorithm algorithm, Instrument instrument) {
		lastClOrdIdsSent = EvictingQueue.create(MAX_SIZE_LAST_CLORDID_SENT * 2);
		this.algorithm = algorithm;
		this.instrument = instrument;
		this.bidQuoteSideManager = new QuoteSideManager(this.algorithm, this.instrument, Verb.Buy);
		this.askQuoteSideManager = new QuoteSideManager(this.algorithm, this.instrument, Verb.Sell);

		if (UPDATE_QUOTE_BUFFER_THREAD) {
			quoteThread = new Thread(this, "quoteManager");
			quoteThread.setPriority(Thread.MIN_PRIORITY);
			quoteThread.start();
		}

	}

	public Queue<String> getLastClOrdIdsSent() {
		return lastClOrdIdsSent;
	}

	public void reset() {
		this.bidQuoteSideManager.reset();
		this.askQuoteSideManager.reset();
		lastClOrdIdsSent.clear();
	}

	public void quoteRequest(QuoteRequest quoteRequest) throws LambdaTradingException {
		this.lastQuoteRequest = quoteRequest;
		if (!UPDATE_QUOTE_BUFFER_THREAD) {
			try {
				updateQuote();
			} catch (Exception e) {
				logger.error("quoteRequest error", e);
			}
		}
	}

	public boolean isClientOrderSent(String clientOrderId) {
		boolean onGeneralList = getLastClOrdIdsSent().contains(clientOrderId);
		if (onGeneralList) {
			return true;
		}

		boolean onAskSideList =
				askQuoteSideManager.getLastClOrdIdSent() != null && askQuoteSideManager.getLastClOrdIdSent()
						.contains(clientOrderId);
		if (onAskSideList) {
			return true;
		}

		boolean onAskCfList =
				askQuoteSideManager.getCfTradesClientOrderId() != null && askQuoteSideManager.getCfTradesClientOrderId()
						.contains(clientOrderId);
		if (onAskCfList) {
			return true;
		}

		boolean onBidSideList =
				bidQuoteSideManager.getLastClOrdIdSent() != null && bidQuoteSideManager.getLastClOrdIdSent()
						.contains(clientOrderId);
		if (onBidSideList) {
			return true;
		}

		boolean onBidCfList =
				bidQuoteSideManager.getCfTradesClientOrderId() != null && bidQuoteSideManager.getCfTradesClientOrderId()
						.contains(clientOrderId);
		if (onBidCfList) {
			return true;
		}
		return false;
	}

	private void updateQuote() throws LambdaTradingException {
		checkQuoteRequest(this.lastQuoteRequest);
		LambdaTradingException ex = null;
		try {
			bidQuoteSideManager.quoteRequest(this.lastQuoteRequest);
			lastClOrdIdsSent.addAll(askQuoteSideManager.getLastClOrdIdSent());

		} catch (Exception e) {
			logger.error("bidQuoteSideManager error", e);
		}
		try {
			askQuoteSideManager.quoteRequest(this.lastQuoteRequest);
			lastClOrdIdsSent.addAll(askQuoteSideManager.getLastClOrdIdSent());

		} catch (Exception e) {
			logger.error("askQuoteSideManager error", e);
		}
	}

	private void checkQuoteRequest(QuoteRequest quoteRequest) throws LambdaTradingException {
		if (Double.isNaN(quoteRequest.getAskPrice()) || Double.isInfinite(quoteRequest.getAskPrice())) {
			throw new LambdaTradingException(
					"Wrong ask price " + quoteRequest.getAskPrice() + " in " + quoteRequest.getInstrument());
		}

		if (Double.isNaN(quoteRequest.getBidPrice()) || Double.isInfinite(quoteRequest.getBidPrice())) {
			throw new LambdaTradingException(
					"Wrong bid price " + quoteRequest.getBidPrice() + " in " + quoteRequest.getInstrument());
		}

		Map<String, ExecutionReport> instrumentActiveOrders = algorithm.getActiveOrders(this.instrument);
		Map<String, OrderRequest> instrumentRequestOrders = algorithm.getRequestOrders(this.instrument);
		if (CHECK_PENDING_ORDERS_QUOTING) {
			if (instrumentRequestOrders.size() > limitOrders) {
				String requestOrders = "";
				int counter = 0;
				for (OrderRequest orderRequest : instrumentRequestOrders.values()) {
					requestOrders += String.format("\n%s [%s]  %s %.5f@%.5f", orderRequest.getOrderRequestAction(),
							orderRequest.getClientOrderId(), orderRequest.getVerb(), orderRequest.getQuantity(),
							orderRequest.getPrice());
					counter++;
				}
				//double check again
				if (counter > limitOrders) {
					logger.error("more than {} request pending! {} {}", limitOrders, instrumentRequestOrders.size(),
							requestOrders);
					logger.error(algorithm.getLastDepth(instrument).prettyPrint());

					if (counterWithoutResponse > MAX_LIMIT_WAITING_ER) {
						//cancel all and reset status
						logger.error("CANCELLING EVERYTHING due to lack of ER");
						unquote();

						this.counterWithoutResponse = 0;
					} else {
						this.counterWithoutResponse++;
					}
					throw new LambdaTradingException("cant quote with more than limitOrders request orders pending ER");
				}
			}
			if (instrumentActiveOrders.size() > limitOrders) {
				String activeOrders = "";
				int counter = 0;
				for (ExecutionReport executionReport : instrumentActiveOrders.values()) {
					if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Active)) {
						activeOrders += String
								.format("\n%s [%s]  %s %.5f@%.5f", executionReport.getExecutionReportStatus(),
										executionReport.getClientOrderId(), executionReport.getVerb(),
										executionReport.getQuantity(), executionReport.getPrice());
						counter++;
					}
				}
				if (counter > limitOrders) {
					logger.error(algorithm.getLastDepth(instrument).prettyPrint());
					logger.error("more than {} request active! {} {}", limitOrders, instrumentActiveOrders.size(),
							activeOrders);
					throw new LambdaTradingException("cant quote with more than limitOrders orders active");
				}
			}
		}

	}

	@Override public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
		if (executionReport.getVerb().equals(Verb.Buy)) {
			bidQuoteSideManager.onExecutionReportUpdate(executionReport);
		}
		if (executionReport.getVerb().equals(Verb.Sell)) {
			askQuoteSideManager.onExecutionReportUpdate(executionReport);
		}
		return true;

	}

	@Override public boolean onInfoUpdate(String header, String message) {
		return false;
	}

	public void unquote() throws LambdaTradingException {
		bidQuoteSideManager.unquoteSide();
		askQuoteSideManager.unquoteSide();
	}

	public void unquoteSide(Verb verb) throws LambdaTradingException {
		if (verb.equals(Verb.Buy)) {
			bidQuoteSideManager.unquoteSide();
		} else {
			askQuoteSideManager.unquoteSide();
		}

	}

	@Override public void run() {
		while (true) {

			try {
				synchronized (EXECUTION_REPORT_LOCK) {
					updateQuote();
				}
			} catch (Exception e) {

			}

			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
