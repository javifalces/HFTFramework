package com.lambda.investing.algorithmic_trading.statistical_arbitrage;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.*;

import java.util.Map;

public class StatisticalArbitrageQuotingAlgorithm extends StatisticalArbitrageAlgorithm {

	private enum Status {
		init, quoting, expose, quoting_exit, stop
	}

	private static long PRINT_EVERY_NTH_UPDATE = 20;

	private Verb lastVerbQuoted = null;
	private long lastQuoteOnTimestamp = 0;
	private long lastQuoteOffTimestamp = 0;
	private static long MIN_TIME_QUOTING_MS = 500;
	private QuoteOffClass quoteOffClass;

	private Status currentStatus = Status.init;

	private long counterPrint = 0;

	protected double zscoreExitBuyAggressive, zscoreExitSellAggressive;

	public StatisticalArbitrageQuotingAlgorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
			String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters);
	}

	@Override public void setParameters(Map<String, Object> parameters) {
		super.setParameters(parameters);

		double zscoreExitBuyAggressiveDefault = zscoreExitSell / 2;
		double zscoreExitSellAggressiveDefault = zscoreExitBuy / 2;
		this.zscoreExitBuyAggressive = getParameterDoubleOrDefault(parameters, "zscoreExitBuyAggressive",
				zscoreExitBuyAggressiveDefault);
		this.zscoreExitSellAggressive = getParameterDoubleOrDefault(parameters, "zscoreExitSellAggressive",
				zscoreExitSellAggressiveDefault);

		//quoting off async
		quoteOffClass = new QuoteOffClass(this.instrument);
		new Thread(quoteOffClass, "quoteOffClass_" + this.instrument.getPrimaryKey()).start();

	}

	public StatisticalArbitrageQuotingAlgorithm(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);
	}

	private void sendQuote(QuoteRequest quoteRequest) {
		try {
			sendQuoteRequest(quoteRequest);

			//				logger.info("quoting  {} bid {}@{}   ask {}@{}", instrument.getPrimaryKey(), quantity, bidPrice,
			//						quantity, askPrice);

		} catch (LambdaTradingException e) {
			if (quoteRequest.getQuoteRequestAction().equals(QuoteRequestAction.On)) {
				logger.error("can't quote {} bid {}@{}   ask {}@{}", instrument.getPrimaryKey(),
						quoteRequest.getBidQuantity(), quoteRequest.getBidPrice(), quoteRequest.getAskQuantity(),
						quoteRequest.getAskPrice(), e);
			}

			if (quoteRequest.getQuoteRequestAction().equals(QuoteRequestAction.Off)) {
				logger.error("can't unquote {} bid {}@{}   ask {}@{}", instrument.getPrimaryKey(),
						quoteRequest.getBidQuantity(), quoteRequest.getBidPrice(), quoteRequest.getAskQuantity(),
						quoteRequest.getAskPrice(), e);
			}
		}
	}

	private void quoteOff() {
		lastVerbQuoted = null;
		lastQuoteOffTimestamp = getCurrentTimestamp();
		quoteOffClass.addQuoteOff();
	}

	@Override protected synchronized void businessLogic(Instrument instrument) {
		//update syntheticBid syntheticAsk syntheticMid
		updateSyntheticPrices();

		mainTradePriceReturn = lastTradeReturnMap.get(this.instrument);
		double residual = mainTradePriceReturn - syntheticTradePriceReturn;
		zscore = (residual - this.syntheticInstrument.getMean()) / this.syntheticInstrument.getStd();

		double currentPosition = this.getPosition(this.instrument);

		boolean isMinTimeQuoting =
				lastQuoteOnTimestamp != 0 && getCurrentTimestamp() - lastQuoteOnTimestamp > MIN_TIME_QUOTING_MS;

		boolean isMinTimeQuotingOff =
				lastQuoteOffTimestamp == 0 || getCurrentTimestamp() - lastQuoteOffTimestamp > MIN_TIME_QUOTING_MS;

		if (currentPosition != 0 && Math.abs(currentPosition) < this.quantity / 5) {
			currentPosition = 0;
		}
		//entry strategies
		boolean buyEntry = (currentStatus == Status.init && this.lastVerbQuoted == null && zscore < zscoreEntryBuy
				&& currentPosition == 0) && isMinTimeQuotingOff;

		if (buyEntry) {
			//entry buy -> start quoting buy
			currentStatus = Status.quoting;
			tradeSpread(instrument, Verb.Buy, this.quantity, true, false);
			return;
		}

		boolean buyEntryNotFilled = currentStatus == Status.quoting && this.lastVerbQuoted != null && isMinTimeQuoting
				&& this.lastVerbQuoted
						.equals(Verb.Buy) && zscore > zscoreEntryBuy && currentPosition == 0;
		if (buyEntryNotFilled) {
			//entry buy not hit
			logger.info("unquote buy side because no arbitrage condition with zscore {}", zscore);
			quoteOff();
			currentStatus = Status.init;
			return;
		}

		boolean sellEntry = currentStatus == Status.init && this.lastVerbQuoted == null && zscore > zscoreEntrySell
				&& currentPosition == 0 && isMinTimeQuotingOff;
		if (sellEntry) {
			//entry sell -> start quoting sell
			currentStatus = Status.quoting;
			tradeSpread(instrument, Verb.Sell, this.quantity, true, false);
			return;
		}
		boolean sellEntryNotFilled = currentStatus == Status.quoting && this.lastVerbQuoted != null && isMinTimeQuoting
				&& this.lastVerbQuoted
						.equals(Verb.Sell) && zscore < zscoreEntrySell && currentPosition == 0;
		if (sellEntryNotFilled) {
			//entry sell not hit
			logger.info("unquote sell side because no arbitrage condition with zscore {}", zscore);
			quoteOff();
			currentStatus = Status.init;
			return;
		}

		//Exit strategies

		//first most aggressive
		boolean buyExitNotFilled =
				(currentStatus == Status.expose || currentStatus == Status.quoting_exit) && currentPosition > 0
						&& zscore > zscoreExitBuyAggressive && isMinTimeQuoting;
		if (buyExitNotFilled) {
			//exit aggressive!! revert the position aggreessive
			//			if (currentStatus == Status.quoting_exit) {
			//				logger.info("unquote sell side to exit aggressive");
			//				quoteOff();
			//			}
			tradeSpread(instrument, Verb.Sell, Math.abs(currentPosition), false, true);
			return;
		}

		boolean buyExit =
				currentStatus == Status.expose && currentPosition > 0 && zscore > zscoreExitBuy && isMinTimeQuoting;
		if (buyExit) {
			//exit passive
			//if entry was hit-> start quoting other side
			currentStatus = Status.quoting_exit;
			tradeSpread(instrument, Verb.Sell, Math.abs(currentPosition), false, false);
			return;
		}

		boolean sellExitNotFilled =
				(currentStatus == Status.expose || currentStatus == Status.quoting_exit) && currentPosition < 0
						&& zscore < zscoreExitSellAggressive && isMinTimeQuoting;
		if (sellExitNotFilled) {
			//exit aggressive!! revert the position aggreessive
			//			if (currentStatus == Status.quoting_exit) {
			//				logger.info("unquote buy side to exit aggressive");
			//				quoteOff();
			//			}
			tradeSpread(instrument, Verb.Buy, Math.abs(currentPosition), false, true);
			return;
		}

		boolean sellExit =
				currentStatus == Status.expose && currentPosition < 0 && zscore < zscoreExitSell && isMinTimeQuoting;
		if (sellExit) {
			//if entry was hit-> start quoting other side
			currentStatus = Status.quoting_exit;
			tradeSpread(instrument, Verb.Buy, Math.abs(currentPosition), false, false);
			return;
		}



	}

	protected void tradeSpread(Instrument instrument, Verb verb, double quantity, boolean isEntry,
			boolean isAggressive) {
		if (isAggressive) {
			lastVerbQuoted = null;
			this.currentStatus = Status.init;
			super.tradeSpread(instrument, verb, quantity, isEntry);
			return;
		}
		//start quoting on the side coming from verb
		if (lastVerbQuoted == null) {
			String entryMessage = isEntry ? "Entry" : "Exit";
			logger.info("{} {} quote the spread on instrument {} - {}SynthPortfolio with zScore:{}", entryMessage, verb,
					this.instrument, this.syntheticInstrument.getInstrumentToBeta().size(), this.zscore);

		}
		if (lastVerbQuoted != null && verb != lastVerbQuoted) {
			String entryMessage = isEntry ? "Entry" : "Exit";
			logger.info("{} change to {} the spread on instrument {} - {}SynthPortfolio with zScore:{}", entryMessage,
					verb, this.instrument, this.syntheticInstrument.getInstrumentToBeta().size(), this.zscore);
		}
		if (lastVerbQuoted == null || verb != lastVerbQuoted) {
			QuoteRequest quoteRequest = createQuoteRequest(this.instrument);
			quoteRequest.setQuoteRequestAction(QuoteRequestAction.On);
			lastQuoteOnTimestamp = getCurrentTimestamp();
			if (verb.equals(Verb.Buy)) {
				quoteRequest.setBidPrice(getLastDepth(this.instrument).getBestBid());
				quoteRequest.setBidQuantity(quantity);
				lastVerbQuoted = verb;
				quoteRequest.setAskPrice(0.0);
				quoteRequest.setAskQuantity(0);

			}

			if (verb.equals(Verb.Sell)) {
				quoteRequest.setAskPrice(getLastDepth(this.instrument).getBestAsk());
				quoteRequest.setAskQuantity(quantity);
				lastVerbQuoted = verb;
				quoteRequest.setBidPrice(0.0);
				quoteRequest.setBidQuantity(0);

			}

			boolean invalidSend = (quoteRequest.getBidQuantity() == 0.0 && quoteRequest.getAskQuantity() == 0.0) || (
					quoteRequest.getBidQuantity() == 0.0 && verb.equals(Verb.Buy)) || (
					quoteRequest.getAskQuantity() == 0.0 && verb.equals(Verb.Sell));
			if (invalidSend) {
				logger.error("trying to quote on {} with zero quantities when verb was {}", instrument, verb);

			}
			sendQuote(quoteRequest);
		}

	}

	private void updateStatusOnCompleteFill(ExecutionReport executionReport) {
		if (currentStatus == Status.quoting) {
			//entry was executed
			currentStatus = Status.expose;
			return;
		}

		if (currentStatus == Status.expose || currentStatus == Status.quoting_exit) {
			//exit was executed
			currentStatus = Status.init;
			return;
		}

	}

	@Override public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
		//if filled coming from main instrument=> hedge it
		boolean output = super.onExecutionReportUpdate(executionReport);

		boolean isFilled = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)
				|| executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled);
		boolean isAgressiveConfirmation = this.lastVerbQuoted == null && currentStatus.equals(Status.init);

		boolean isRejected = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CancelRejected)
				|| executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected);
		if (isRejected) {
			logger.warn("Rejection {} received=> {}", executionReport.getExecutionReportStatus(),
					executionReport.getRejectReason());
			quoteOff();
			return false;
		}

		if (isFilled && executionReport.getInstrument().equalsIgnoreCase(this.instrument.getPrimaryKey())
				&& !isAgressiveConfirmation) {
			//we are in the market
			//hedge with the synthetic if enable
			if (StatisticalArbitrageAlgorithm.OPERATE_SYNTHETIC_INSTRUMENT) {
				hedgeSyntheticInstrumentsMarket(executionReport.getVerb(), executionReport.getLastQuantity());
			}

			if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)) {
				//dont update quoting more
				updateStatusOnCompleteFill(executionReport);
				lastVerbQuoted = null;
				//unquote - automatically
			}

		}

		boolean isAggressiveConfirmationFilled =
				isFilled && executionReport.getInstrument().equalsIgnoreCase(this.instrument.getPrimaryKey())
						&& isAgressiveConfirmation;
		if (isAggressiveConfirmationFilled) {
			quoteOff();//disable quoting
		}

		return output;
	}

	@Override public boolean onDepthUpdate(Depth depth) {
		counterPrint++;
		boolean output = super.onDepthUpdate(depth);
		if (lastVerbQuoted != null && depth.getInstrument().equalsIgnoreCase(this.instrument.getPrimaryKey())) {
			QuoteRequest quoteRequest = createQuoteRequest(this.instrument);
			quoteRequest.setQuoteRequestAction(QuoteRequestAction.On);
			if (lastVerbQuoted.equals(Verb.Buy)) {                //update bid

				quoteRequest.setBidPrice(getLastDepth(this.instrument).getBestBid());
				quoteRequest.setBidQuantity(this.quantity);
				quoteRequest.setAskPrice(0.0);
				quoteRequest.setAskQuantity(0);
			}
			if (lastVerbQuoted.equals(Verb.Sell)) {                //update ask
				quoteRequest.setAskPrice(getLastDepth(this.instrument).getBestAsk());
				quoteRequest.setAskQuantity(this.quantity);
				quoteRequest.setBidPrice(0.0);
				quoteRequest.setBidQuantity(0);
			}
			sendQuote(quoteRequest);
		}
		if (counterPrint % PRINT_EVERY_NTH_UPDATE == 0) {
			printRowTrade(null);
		}

		return output;
	}

	private class QuoteOffClass implements Runnable {

		private Instrument instrument;
		private boolean unquote = false;

		public QuoteOffClass(Instrument instrument) {
			this.instrument = instrument;
		}

		public void addQuoteOff() {
			unquote = true;
		}

		private void quoteOff() {
			QuoteRequest quoteRequest = createQuoteRequest(this.instrument);
			quoteRequest.setQuoteRequestAction(QuoteRequestAction.Off);

			sendQuote(quoteRequest);
		}

		@Override public void run() {
			while (true) {

				if (unquote) {
					quoteOff();
					unquote = false;
				}

				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
