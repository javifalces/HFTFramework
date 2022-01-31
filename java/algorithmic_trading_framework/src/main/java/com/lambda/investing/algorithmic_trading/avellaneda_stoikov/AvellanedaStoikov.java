package com.lambda.investing.algorithmic_trading.avellaneda_stoikov;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.AlgorithmState;
import com.lambda.investing.algorithmic_trading.InstrumentManager;
import com.lambda.investing.algorithmic_trading.MarketMakingAlgorithm;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.*;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * https://www.math.nyu.edu/faculty/avellane/HighFrequencyTrading.pdf
 * https://github.com/mdibo/Avellaneda-Stoikov/blob/master/AvellanedaStoikov.py
 * T = inf
 * δa , δb  are the bid and ask spreads
 * γ is a risk aversion parameter
 * XT is the cash at time T
 * qT is the inventory at time T
 * ST is the stock price at time T
 * k intensity of to be "hit" by a market orders
 * Assuming the risk-free rate is zero and the mid-price of a stock follows a
 * standard brownian motion dSt = σdWt with initial value S0 = s and standard deviation σ,
 * <p>
 * Avellaneda Stoikov also provides a structure to the number of bid and ask executions by modeling them as a Poisson process. According to their framework, this Poisson
 * process should also depend on the market depth of our quote
 * <p>
 * λ(δ) = Ae(−κδ)
 * <p>
 * reservation price -> r(s, t) = s − qγσ2*(T − t)
 * spread around reservation -> δa + δb = γσ2*(T − t) + (2/γ)*ln(1 + γ/κ)
 * <p>
 * k can be estimated
 * https://quant.stackexchange.com/questions/36073/how-does-one-calibrate-lambda-in-a-avellaneda-stoikov-market-making-problem
 * <p>
 * from High Freq trading by Irene Alridge , page 139
 * k_bid = λb/delta(λb)  / delta(λb)=(λb-λb-1)/λb-1
 * :return: k_bid and k_ask tuple
 */
public class AvellanedaStoikov extends MarketMakingAlgorithm {

	private static boolean DISABLE_ON_HIT = false;

	//Tresholds to prices!
	private static boolean CONTROL_NOT_CROSS_MIDPRICE = true;//if enable bid ask price will not be better than midprice!
	private static boolean CONTROL_MAX_SPREAD_TICKS_DEV = false;//if enable min bid will be midprice-MAX_TICKS_MIDPRICE_PRICE_DEV*PriceTick
	private static double MAX_TICKS_MIDPRICE_PRICE_DEV = 1E10;
	private static boolean SYMMETRIC_SPREAD_RESERVE = false;//on backtest will be similar


	protected double riskAversion;
	protected double quantity;
	protected int windowTick;
	protected double skewPricePct;
	protected boolean autoEnableSideTime = true;
	protected boolean calculateTt = true;//if true T-t will be calculated

	protected int minutesChangeK = 1;
	private double targetPosition = 0.;
	private double positionMultiplier = 1.;

	private Queue<Double> midpricesQueue;
	private Queue<Long> counterTradesPerMinute;
	private Queue<Long> counterBuyTradesPerMinute;
	private Queue<Long> counterSellTradesPerMinute;

	private Queue<Long> counterQuotesPerMinute;
	private Queue<Long> counterBidQuotesPerMinute;
	private Queue<Long> counterAskQuotesPerMinute;

	private long counterStartingMinuteMs = 0;
	private long counterStartingQuoteMinuteMs = 0;

	private long counterTrades = 0;
	private long counterBuyTrades = 0;
	private long counterSellTrades = 0;
	private long counterQuotes = 0;
	private long counterAskQuotes = 0;
	private long counterBidQuotes = 0;
	private Depth lastDepthToCount = null;

	protected double spreadMultiplier = 1.;
	private Double kDefault, varianceMidPrice;

	private long stopTradeSideMs = 60 * 1000 * 5;//5 mins *60 seconds/min * 1000 ms /seconds
	protected Map<Verb, Boolean> sideActive = new ConcurrentHashMap<>();

	public void setKdefault(Double kDefault) {
		this.kDefault = kDefault;
	}

	public AvellanedaStoikov(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo,
			Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters);
		setParameters(parameters);

	}

	public AvellanedaStoikov(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);
		setParameters(parameters);
	}

	@Override public void reset() {
		super.reset();
		//		this.midpricesQueue = null;
	}

	public void setInstrument(Instrument instrument) {
		super.setInstrument(instrument);
		//		this.algorithmInfo += "_" + instrument.getPrimaryKey();
		//		this.algorithmNotifier.setAlgorithmInfo(this.algorithmInfo);
	}

	@Override public void setParameters(Map<String, Object> parameters) {
		super.setParameters(parameters);
		this.quantity = getParameterDouble(parameters, "quantity");
		this.quantityBuy = quantity;
		this.quantitySell = quantity;

		this.calculateTt = getParameterIntOrDefault(parameters, "calculateTt", 1) == 1;
		this.riskAversion = getParameterDouble(parameters,
				"risk_aversion");//0.0 low risk_aversion means risk_neutral investor.0.5 risk-averse investor
		this.quantity = getParameterDouble(parameters, "quantity");
		this.windowTick = getParameterInt(parameters, "window_tick");
		this.targetPosition = getParameterDoubleOrDefault(parameters, "target_position", 0.);
		this.positionMultiplier = getParameterDoubleOrDefault(parameters, "position_multiplier", 1.);
		this.spreadMultiplier = getParameterDoubleOrDefault(parameters, "spread_multiplier",
				1.);//Double.valueOf((String) parameters.getOrDefault("spread_multiplier", "1."));
		this.kDefault = getParameterDoubleOrDefault(parameters, "k_default",
				-1.);//Double.valueOf((String) parameters.getOrDefault("k_default", "-1."));
		if (this.kDefault == -1) {
			this.kDefault = null;
		}


	}

	public void init() {
		super.init();
		if (this.midpricesQueue == null) {
			//dont delete it if exists
			logger.info("creating midpricesQueue of len {}", this.windowTick);
			this.midpricesQueue = EvictingQueue.create(this.windowTick);
		}

		this.minutesChangeK = getParameterIntOrDefault(parameters, "minutes_change_k", 1);
		int maxMinutesKCounter = this.minutesChangeK + 1;
		this.counterTradesPerMinute = EvictingQueue.create(maxMinutesKCounter);//last element in the -1 index
		this.counterBuyTradesPerMinute = EvictingQueue.create(maxMinutesKCounter);//last element in the -1 index
		this.counterSellTradesPerMinute = EvictingQueue.create(maxMinutesKCounter);//last element in the -1 index

		this.counterQuotesPerMinute = EvictingQueue.create(maxMinutesKCounter);//last element in the -1 index
		this.counterBidQuotesPerMinute = EvictingQueue.create(maxMinutesKCounter);//last element in the -1 index
		this.counterAskQuotesPerMinute = EvictingQueue.create(maxMinutesKCounter);//last element in the -1 index

	}

	protected void setMidPricesQueue(int windowTick) {
		//For RL
		this.midpricesQueue = EvictingQueue.create(windowTick);
	}


	@Override public void setParameter(String name, Object value) {
		super.setParameter(name, value);
	}

	@Override public String printAlgo() {
		return String
				.format("%s  \n\triskAversion=%.3f\n\tquantity=%.3f\n\twindowTick=%d\n\tfirstHourOperatingIncluded=%d\n\tlastHourOperatingIncluded=%d\n\tminutesChangeK=%d",
						algorithmInfo, riskAversion, quantity, windowTick, firstHourOperatingIncluded,
						lastHourOperatingIncluded, minutesChangeK);
	}

	@Override public boolean onTradeUpdate(Trade trade) {
		if (!super.onTradeUpdate(trade)) {
			return false;
		}
		if (counterStartingMinuteMs == 0) {
			counterStartingMinuteMs = getCurrentTimestamp();
		}
		boolean minuteHasPassed = getCurrentTimestamp() - counterStartingMinuteMs > 60 * 1000;
		if (minuteHasPassed) {
			counterTradesPerMinute.add(counterTrades);//counter per minute
			counterBuyTradesPerMinute.add(counterBuyTrades);//counter per minute
			counterSellTradesPerMinute.add(counterSellTrades);//counter per minute
			counterStartingMinuteMs = getCurrentTimestamp();
			counterTrades = 0L;
			counterBuyTrades = 0L;
			counterSellTrades = 0L;

		} else {
			InstrumentManager instrumentManager = getInstrumentManager(trade.getInstrument());
			Depth lastDepth = instrumentManager.getLastDepth();

			if (lastDepth != null && lastDepth.isDepthFilled()) {
				if (trade.getPrice() < lastDepth.getBestBid()) {
					//buy
					counterBuyTrades++;
				}
				if (trade.getPrice() > lastDepth.getBestAsk()) {
					//buy
					counterSellTrades++;
				}

			}
			counterTrades++;
		}
		return true;

	}

	@Override public boolean isReady() {
		boolean weAreReady = super.isReady();
		boolean varianceIsReady = (getVarianceMidPrice() != null && Double.isFinite(getVarianceMidPrice()));
		return weAreReady && varianceIsReady;
	}

	@Override public boolean onDepthUpdate(Depth depth) {
		if (!super.onDepthUpdate(depth)) {
			return false;
		}

		if (counterStartingQuoteMinuteMs == 0) {
			counterStartingQuoteMinuteMs = getCurrentTimestamp();
		}
		boolean minuteHasPassed = getCurrentTimestamp() - counterStartingQuoteMinuteMs > 60 * 1000;
		if (minuteHasPassed) {
			counterAskQuotesPerMinute.add(counterAskQuotes);//counter per minute
			counterBidQuotesPerMinute.add(counterBidQuotes);//counter per minute
			counterQuotesPerMinute.add(counterQuotes);//counter per minute
			counterStartingQuoteMinuteMs = getCurrentTimestamp();
			counterAskQuotes = 0L;
			counterBidQuotes = 0L;
			counterQuotes = 0L;
		} else {
			counterQuotes++;
			if (lastDepthToCount == null) {
				counterAskQuotes++;
				counterBidQuotes++;
			} else {
				if (!depth.equalsSide(lastDepthToCount, Verb.Buy)) {
					counterBidQuotes++;
				}
				if (!depth.equalsSide(lastDepthToCount, Verb.Sell)) {
					counterAskQuotes++;
				}
			}
			lastDepthToCount = depth;

		}
		checkSideDisable(getCurrentTimestamp());
		if (!depth.isDepthFilled() && inOperationalTime() && getAlgorithmState().equals(AlgorithmState.STARTED)) {
			//			logger.warn("Depth received incomplete! {}-> disable", depth.getInstrument());
			logger.info("stopping algorithm because depth is incomplete!");
			this.stop();
			return false;
		} else if (depth.isDepthFilled() && inOperationalTime() && getAlgorithmState().equals(AlgorithmState.STOPPED)) {
			this.start();
		}

		double midPrice = depth.getMidPrice();
		double currentSpread = depth.getSpread();
		this.midpricesQueue.add(midPrice);
		Double varianceMidPrice = getVarianceMidPrice();
		if (varianceMidPrice == null || !Double.isFinite(varianceMidPrice)) {
			return false;
		}
		this.varianceMidPrice = varianceMidPrice;

		double T_t = getTt();
		double position = (getPosition(this.instrument) - targetPosition) * positionMultiplier;
		double reservePrice = midPrice - (position * this.riskAversion * varianceMidPrice * T_t);
		//		if (reservePrice<depth.getBestBid() || reservePrice>depth.getBestAsk()){
		//			//warn > control it?
		//			logger.debug("");
		//		}

		double kTotal = calculateK(this.counterQuotesPerMinute);
		if (!Double.isFinite(kTotal) || kTotal == 0) {
			return false;
		}
		//each side
		double kBuy = calculateK(this.counterBidQuotesPerMinute);
		if (!Double.isFinite(kBuy) || kBuy == 0) {
			return false;
		}

		double kSell = calculateK(this.counterAskQuotesPerMinute);
		if (!Double.isFinite(kSell) || kSell == 0) {
			return false;
		}

		double spreadBid = 0.0;
		double spreadAsk = 0.0;
		//
		if (!SYMMETRIC_SPREAD_RESERVE) {

			double spreadBid_ = 0.5 * (riskAversion * varianceMidPrice * T_t) + (1 / riskAversion) * Math
					.log(1 + (riskAversion / kBuy));

			//alridge -> negative log is a sum
			//			double spreadBid_ = (1 / riskAversion) * Math.log(1 + (riskAversion / kBuy));
			spreadBid = spreadBid_ * spreadMultiplier;

			double spreadAsk_ = 0.5 * (riskAversion * varianceMidPrice * T_t) + (1 / riskAversion) * Math
					.log(1 + (riskAversion / kSell));

			//alridge -> negative log
			//			double spreadAsk_ = (1 / riskAversion) * Math.log(1 + (riskAversion * kSell));
			spreadAsk = spreadAsk_ * spreadMultiplier;

		} else {
			double spreadBid_ = 0.5 * (riskAversion * varianceMidPrice * T_t) + (1 / riskAversion) * Math
					.log(1 + (riskAversion / kTotal));
			//alridge -> negative log
			//			double spreadBid_ = (1 / riskAversion) * Math.log(1 + (riskAversion * kTotal));
			spreadBid = spreadBid_ * spreadMultiplier;
			spreadAsk = spreadBid;
		}

		try {

			double askPrice = (reservePrice + spreadAsk);
			askPrice *= (1 + skewPricePct);

			double bidPrice = (reservePrice - spreadBid);
			bidPrice *= (1 + skewPricePct);

			if (!Double.isFinite(askPrice) || !Double.isFinite(bidPrice)) {
				logger.warn("wrong calculation ask-bid");
				return false;
			}
			//Check not crossing the mid price!
			if (CONTROL_NOT_CROSS_MIDPRICE) {
				askPrice = Math.max(askPrice, depth.getMidPrice() + instrument.getPriceTick());
				bidPrice = Math.min(bidPrice, depth.getMidPrice() - instrument.getPriceTick());
			}
			if (CONTROL_MAX_SPREAD_TICKS_DEV) {
				//			Check worst price
				double maxAskPrice = depth.getMidPrice() + MAX_TICKS_MIDPRICE_PRICE_DEV * instrument.getPriceTick();
				askPrice = Math.min(askPrice, maxAskPrice);
				double minBidPrice = depth.getMidPrice() - MAX_TICKS_MIDPRICE_PRICE_DEV * instrument.getPriceTick();
				bidPrice = Math.max(bidPrice, minBidPrice);
			}

			bidPrice = instrument.roundPrice(bidPrice);
			askPrice = instrument.roundPrice(askPrice);
			//create quote request
			QuoteRequest quoteRequest = createQuoteRequest(this.instrument);
			quoteRequest.setQuoteRequestAction(QuoteRequestAction.On);
			quoteRequest.setBidPrice(bidPrice);
			quoteRequest.setAskPrice(askPrice);
			quoteRequest.setBidQuantity(this.quantity);
			quoteRequest.setAskQuantity(this.quantity);

			//remove side disable!
			for (Map.Entry<Verb, Boolean> entry : sideActive.entrySet()) {
				boolean isActive = entry.getValue();
				Verb verb = entry.getKey();
				if (!isActive) {
					if (verb.equals(Verb.Buy)) {
						quoteRequest.setBidQuantity(0.);
					}
					if (verb.equals(Verb.Sell)) {
						quoteRequest.setAskQuantity(0.);
					}
				}
			}

			try {
				sendQuoteRequest(quoteRequest);

				//				logger.info("quoting  {} bid {}@{}   ask {}@{}", instrument.getPrimaryKey(), quantity, bidPrice,
				//						quantity, askPrice);

			} catch (LambdaTradingException e) {
				logger.error("can't quote {} bid {}@{}   ask {}@{}", instrument.getPrimaryKey(), quantity, bidPrice,
						quantity, askPrice, e);
			}
		} catch (Exception e) {
			logger.error("error onDepth constant Spread : ", e);
		}

		return true;
	}

	private void checkSideDisable(long currentTimestamp) {
		if (!autoEnableSideTime) {
			return;
		}
		Map<Verb, Boolean> sideActiveOutput = new ConcurrentHashMap<>(sideActive);
		for (Map.Entry<Verb, Boolean> entry : sideActive.entrySet()) {
			Verb verb = entry.getKey();
			Boolean isActive = entry.getValue();
			long disableTime = 0L;
			if (!isActive) {
				try {
					disableTime = getInstrumentManager().getLastTradeTimestamp().get(verb);
				} catch (NullPointerException e) {
					//in case of nullpointer -> enable it again
				}

				long elapsedTimeMs = currentTimestamp - disableTime;
				if (elapsedTimeMs > stopTradeSideMs) {
					//enable again
					logger.info("enable side {} at {}", verb, getCurrentTime());
					sideActiveOutput.put(verb, true);
				}

			}

		}
		this.sideActive = sideActiveOutput;

	}

	/***
	 * from High Freq trading by Irene Alridge , page 139
	 *         k_bid = λb/delta(λb)  / delta(λb)=(λb-λb-1)/λb-1
	 *
	 *
	 * @return K_total
	 */
	private double calculateK(Queue<Long> counterEventsPerMinute) {
		if (kDefault != null) {
			return kDefault;
		}
		if (counterEventsPerMinute.size() < minutesChangeK + 1) {
			return 0.;
		}
		Long[] counterTradesPerMinute = new Long[counterEventsPerMinute.size()];
		counterTradesPerMinute = counterEventsPerMinute.toArray(counterTradesPerMinute);

		double lastMinuteTrades = Double.valueOf(counterTradesPerMinute[counterEventsPerMinute.size() - 1]);
		double initialMinuteTrades = Double
				.valueOf(counterTradesPerMinute[counterEventsPerMinute.size() - (minutesChangeK + 1)]);

		//		 k_total = count_total / (
		//                    (count_total - count_total_before) / count_total_before
		//                )
		double denominator = (lastMinuteTrades - initialMinuteTrades) / initialMinuteTrades;
		return lastMinuteTrades / denominator;
	}

	private Double getVarianceMidPrice() {

		if (midpricesQueue.size() < windowTick) {
			return null;
		}
		Double[] midPricesArr = new Double[windowTick];
		try {
			Double[] midPricesArrtemp = new Double[midpricesQueue.size()];
			midPricesArrtemp = midpricesQueue.toArray(midPricesArrtemp);
			if (midpricesQueue.size() == windowTick) {
				midPricesArr = midPricesArrtemp;
			} else {
				int indexArr = windowTick - 1;
				for (int lastElements = midPricesArrtemp.length - 1; lastElements >= 0; lastElements--) {
					midPricesArr[indexArr] = midPricesArrtemp[lastElements];
					indexArr--;
					if (indexArr < 0) {
						break;
					}
				}
			}

		} catch (IndexOutOfBoundsException e) {
			logger.error("error calculating variance on {} windows tick with size {}-> return last varianceMidPrice",
					windowTick, midpricesQueue.size());
			return this.varianceMidPrice;
		}
		double sum = 0.;
		for (int i = 0; i < windowTick; i++) {
			sum += midPricesArr[i];
		}
		double mean = sum / (double) windowTick;
		double sqDiff = 0;
		for (int i = 0; i < windowTick; i++) {
			sqDiff += (midPricesArr[i] - mean) * (midPricesArr[i] - mean);
		}
		double var = (double) sqDiff / (windowTick - 1);
		return var;
	}

	/**
	 * 1 at the start of the session -> linearly decreasing to zero at the end of session
	 *
	 * @return
	 */
	private double getTt() {
		if (!calculateTt) {
			return 1.0;
		}
		int currentTimeMins = getCurrentTimeHour() * 100 + getCurrentTimeMinute();
		int lastHourMins = lastHourOperatingIncluded * 100;
		int firstHourMins = firstHourOperatingIncluded * 100;

		double num = Math.max((lastHourMins - currentTimeMins), 0);
		double den = lastHourMins - firstHourMins;
		return num / den;
	}

	@Override public void sendOrderRequest(OrderRequest orderRequest) throws LambdaTradingException {
		//		logger.info("sendOrderRequest {} {}", orderRequest.getOrderRequestAction(), orderRequest.getClientOrderId());
		super.sendOrderRequest(orderRequest);

	}

	@Override public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
		boolean output = super.onExecutionReportUpdate(executionReport);
		if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)
				|| executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled)) {
			logger.debug("trade arrived");
			try {
				getQuoteManager(executionReport.getInstrument()).unquoteSide(executionReport.getVerb());
			} catch (LambdaTradingException e) {
				logger.error("cant unquote verb {} => cancel manual", executionReport.getVerb(), e);
				//cancel all this side active
				cancelAllVerb(instrument, executionReport.getVerb());
			}
			//disable this side
			if (DISABLE_ON_HIT) {
				autoEnableSideTime = true;//need to be enable to autodisable it in time
				logger.info("disable {} side at {}", executionReport.getVerb(), getCurrentTime());
				sideActive.put(executionReport.getVerb(), false);
			}

		}
		return output;
	}
}
