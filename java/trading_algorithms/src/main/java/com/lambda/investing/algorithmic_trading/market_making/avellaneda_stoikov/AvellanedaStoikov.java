package com.lambda.investing.algorithmic_trading.market_making.avellaneda_stoikov;

import com.lambda.investing.algorithmic_trading.*;
import com.lambda.investing.algorithmic_trading.market_making.MarketMakingAlgorithm;
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
 *
 * https://quant.stackexchange.com/questions/36400/avellaneda-stoikov-market-making-model?rq=1
 *
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

	private enum KCalculation {Quotes, Alridge}

	private enum SpreadCalculation {Alridge, GueantTapia, Avellaneda}
	public static double NON_SET_PARAMETER = Double.MIN_VALUE;
	private static boolean PHD_THESIS = false;//set to true to go back to configuration of thesis
	private static boolean DISABLE_ON_HIT = false;

	//Tresholds to prices!
	private KCalculation kCalculationType = KCalculation.Alridge;
	private SpreadCalculation spreadCalculationType = SpreadCalculation.Avellaneda;

	private static boolean CONTROL_NOT_CROSS_MIDPRICE = false;//if enable bid ask price will not be better than midprice!
	private static boolean CONTROL_MAX_SPREAD_TICKS_DEV = false;//if enable min bid will be midprice-MAX_TICKS_MIDPRICE_PRICE_DEV*PriceTick
	private static double MAX_TICKS_MIDPRICE_PRICE_DEV = 1E10;
	private static boolean SYMMETRIC_SPREAD_RESERVE = true;//on backtest will be similar

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
	protected Double kDefault, varianceMidPrice, aDefault, sigmaDefault, varianceDefault;

	private long lastMidUpdateTimestamp = 0;
	private long minMsMidUpdate = 500;

	private long stopTradeSideMs = 60 * 1000 * 5;//5 mins *60 seconds/min * 1000 ms /seconds
	protected Map<Verb, Boolean> sideActive = new ConcurrentHashMap<>();

	public AvellanedaStoikov(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo,
							 Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters);
		if (PHD_THESIS) {
			DISABLE_ON_HIT = false;
			CONTROL_NOT_CROSS_MIDPRICE = true;
			SYMMETRIC_SPREAD_RESERVE = true;
			kCalculationType = KCalculation.Quotes;
			spreadCalculationType = SpreadCalculation.Avellaneda;
			calculateTt = false;
		}

		setParameters(parameters);

	}

	public AvellanedaStoikov(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);
		setParameters(parameters);
	}

//	@Override public void resetAlgorithm() {
//		super.resetAlgorithm();
//		//		this.midpricesQueue = null;
//	}

	public void setInstrument(Instrument instrument) {
		super.setInstrument(instrument);
		//		this.algorithmInfo += "_" + instrument.getPrimaryKey();
		//		this.algorithmNotifier.setAlgorithmInfo(this.algorithmInfo);
	}

	@Override
	public void setParameters(Map<String, Object> parameters) {
		super.setParameters(parameters);
		this.quantity = getParameterDouble(parameters, "quantity");
		this.quantityBuy = quantity;
		this.quantitySell = quantity;

		this.calculateTt = getParameterIntOrDefault(parameters, "calculateTt", 1) == 1;
		this.riskAversion = getParameterDouble(parameters, "riskAversion",
				"risk_aversion");//0.0 low risk_aversion means risk_neutral investor.0.5 risk-averse investor

		this.quantity = getParameterDouble(parameters, "quantity");
		this.windowTick = getParameterInt(parameters, "windowTick", "window_tick");
		this.targetPosition = getParameterDoubleOrDefault(parameters, "targetPosition", "target_position", 0.);
		this.positionMultiplier = getParameterDoubleOrDefault(parameters, "positionMultiplier", "position_multiplier",
				1.);
		this.spreadMultiplier = getParameterDoubleOrDefault(parameters, "spreadMultiplier", "spread_multiplier",
				1.);//Double.valueOf((String) parameters.getOrDefault("spread_multiplier", "1."));

		this.kDefault = getParameterDoubleOrDefault(parameters, "kDefault", "k_default",
				-1.);//Double.valueOf((String) parameters.getOrDefault("k_default", "-1."));
		if (this.kDefault == -1) {
			this.kDefault = null;
		}

		this.aDefault = getParameterDoubleOrDefault(parameters, "aDefault", "a_default", -1.);
		if (this.aDefault == -1) {
			this.aDefault = null;
		}

		this.sigmaDefault = getParameterDoubleOrDefault(parameters, "sigmaDefault", "sigma_default", -1.);
		if (this.sigmaDefault == -1) {
			this.sigmaDefault = null;
		}

		String typeK = getParameterStringOrDefault(parameters, "kCalculation", "k_calculation", "Alridge");
		kCalculationType = KCalculation.valueOf(typeK);

		String typeSpread = getParameterStringOrDefault(parameters, "spreadCalculation", "spread_calculation",
				"Avellaneda");
		spreadCalculationType = SpreadCalculation.valueOf(typeSpread);

		if (spreadCalculationType == SpreadCalculation.GueantTapia && this.aDefault == null) {
			logger.error("cant create SpreadCalculation.GueantTapia without a_default !!");
			System.err.println("cant create SpreadCalculation.GueantTapia without a_default !!");
			System.exit(-1);
		}

	}

	public void init() {
		super.init();
		this.algorithmState = AlgorithmState.INITIALIZING;
		if (this.midpricesQueue == null) {
			//dont delete it if exists
			logger.info("creating midpricesQueue of len {}", this.windowTick);
			this.midpricesQueue = EvictingQueue.create(this.windowTick);
		}

		this.minutesChangeK = getParameterIntOrDefault(parameters, "minutesChangeK", "minutes_change_k", 1);
		int maxMinutesKCounter = this.minutesChangeK + 1;
		this.counterTradesPerMinute = EvictingQueue.create(maxMinutesKCounter);//last element in the -1 index
		this.counterBuyTradesPerMinute = EvictingQueue.create(maxMinutesKCounter);//last element in the -1 index
		this.counterSellTradesPerMinute = EvictingQueue.create(maxMinutesKCounter);//last element in the -1 index

		this.counterQuotesPerMinute = EvictingQueue.create(maxMinutesKCounter);//last element in the -1 index
		this.counterBidQuotesPerMinute = EvictingQueue.create(maxMinutesKCounter);//last element in the -1 index
		this.counterAskQuotesPerMinute = EvictingQueue.create(maxMinutesKCounter);//last element in the -1 index
		this.algorithmState = AlgorithmState.INITIALIZED;

	}



	@Override public void setParameter(String name, Object value) {
		super.setParameter(name, value);
	}

	@Override public String printAlgo() {
		return String
				.format("%s  \n\tspreadCalculationType=%s\n\tkCalculationType=%s\n\triskAversion=%.3f\n\tquantity=%.3f\n\tsigmaDefault=%.3f\n\tkDefault=%.3f\n\taDefault=%.3f\n\twindowTick=%d\n\tfirstHourOperatingIncluded=%d\n\tlastHourOperatingIncluded=%d\n\tminutesChangeK=%d",
						algorithmInfo, spreadCalculationType, kCalculationType, riskAversion, quantity, sigmaDefault,
						kDefault, aDefault, windowTick, firstHourOperatingIncluded, lastHourOperatingIncluded,
						minutesChangeK);
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
			counterTradesPerMinute.offer(counterTrades);//counter per minute
			counterBuyTradesPerMinute.offer(counterBuyTrades);//counter per minute
			counterSellTradesPerMinute.offer(counterSellTrades);//counter per minute
			counterStartingMinuteMs = getCurrentTimestamp();
			counterTrades = 0L;
			counterBuyTrades = 0L;
			counterSellTrades = 0L;

		} else {
			if (trade.getVerb() == null) {
				//infer the side
				InstrumentManager instrumentManager = getInstrumentManager(trade.getInstrument());
				Depth lastDepth = instrumentManager.getLastDepth();
				if (lastDepth != null && lastDepth.isDepthFilled()) {
					if (trade.getPrice() < lastDepth.getMidPrice()) {
						//buy
						counterBuyTrades++;
					}
					if (trade.getPrice() > lastDepth.getMidPrice()) {
						//buy
						counterSellTrades++;
					}

				}
			} else {
				if (trade.getVerb().equals(Verb.Buy)) {
					counterBuyTrades++;
				}
				if (trade.getVerb().equals(Verb.Sell)) {
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

	public double[] calculateKQuotes() {
		double kTotal = calculateK(this.counterQuotesPerMinute);
		if (!Double.isFinite(kTotal) || kTotal == 0) {
			return null;
		}
		//each side
		double kBuy = calculateK(this.counterBidQuotesPerMinute);
		if (!Double.isFinite(kBuy) || kBuy == 0) {
			return null;
		}
		double kSell = calculateK(this.counterAskQuotesPerMinute);
		if (!Double.isFinite(kSell) || kSell == 0) {
			return null;
		}
		return new double[] { kTotal, kBuy, kSell };
	}

	public double[] calculateKAlridge() {
		double kTotal = calculateK(this.counterTradesPerMinute);
		if (!Double.isFinite(kTotal) || kTotal == 0) {
			return null;
		}
		//each side
		double kBuy = calculateK(this.counterBuyTradesPerMinute);
		if (!Double.isFinite(kBuy) || kBuy == 0) {
			return null;
		}
		double kSell = calculateK(this.counterSellTradesPerMinute);
		if (!Double.isFinite(kSell) || kSell == 0) {
			return null;
		}
		return new double[] { kTotal, kBuy, kSell };
	}

	/**
	 * @return returns array of three with [Ktotal,Kbid,Kask] if kDefault exist , will return it on three
	 */
	public double[] calculateK() {

		if (kDefault != null && kDefault != NON_SET_PARAMETER) {
			return new double[]{kDefault, kDefault, kDefault};
		}

		switch (kCalculationType) {
			case Quotes:
				return calculateKQuotes();
			case Alridge:
				return calculateKAlridge();
			default:
				logger.error("kCalculationType not recognized {}", kCalculationType);
				return null;
		}

	}

	public double[] calculateSpread() {
		try {
			switch (spreadCalculationType) {
				case Alridge:
					//can return nan values on negatives!!!
					return calculateSpreadAlridge();
				case GueantTapia:
					return calculateSpreadGueantTapia();
				case Avellaneda:
					return calculateSpreadAvellaneda();
				default:
					logger.error("spreadCalculationType not recognized {}", spreadCalculationType);
					return null;
			}
		} catch (Exception e) {
			System.err.println("error calculating spread of type " + spreadCalculationType);
			e.printStackTrace();
			logger.error("error calculating spread {}", spreadCalculationType, e);
			return null;
		}
	}

	public double[] calculateSpreadAvellaneda() {
		Double varianceMidPrice = getVarianceMidPrice();
		if (varianceMidPrice == null || !Double.isFinite(varianceMidPrice)) {
			return null;
		}
		double T_t = getTt();

		double[] ks = calculateK();
		if (ks == null) {
			return null;
		}

		double kTotal = ks[0];
		double kBuy = ks[1];
		double kSell = ks[2];

		double spreadBid = 0.0;
		double spreadAsk = 0.0;
		//
		if (!SYMMETRIC_SPREAD_RESERVE) {

			double spreadBid_ = 0.5 * (riskAversion * varianceMidPrice * T_t) + (1 / riskAversion) * Math
					.log(1 + (riskAversion / kBuy));

			//alridge -> negative log is a sum
			//			double spreadBid_ = (1 / riskAversion) * Math.log(1 + (riskAversion * kBuy));
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
			//			double spreadBid_ = (1 / riskAversion) * Math.log(1 - (riskAversion * kTotal));
			spreadBid = Math.abs(spreadBid_ * spreadMultiplier);
			spreadAsk = spreadBid;
		}
		return new double[] { spreadBid, spreadAsk };
	}

	public double[] calculateSpreadAlridge() {
		double[] ks = calculateK();
		if (ks == null) {
			return null;
		}

		double kTotal = ks[0];
		double kBuy = ks[1];
		double kSell = ks[2];

		double spreadBid = 0.0;
		double spreadAsk = 0.0;
		//
		if (!SYMMETRIC_SPREAD_RESERVE) {
			//alridge -> negative log is a sum
			double spreadBid_ = (1 / riskAversion) * Math.log(1 + (riskAversion * kBuy));
			spreadBid = spreadBid_ * spreadMultiplier;

			//alridge -> negative log
			double spreadAsk_ = (1 / riskAversion) * Math.log(1 + (riskAversion * kSell));
			spreadAsk = spreadAsk_ * spreadMultiplier;

		} else {
			//alridge -> negative log
			double spreadBid_ = (1 / riskAversion) * Math.log(1 + (riskAversion * kTotal));
			spreadBid = Math.abs(spreadBid_ * spreadMultiplier);
			spreadAsk = spreadBid;
		}
		return new double[] { spreadBid, spreadAsk };
	}

	public Double calculateReservePrice(Depth depth) {

		double T_t = getTt();
		double position = (getPosition(this.instrument) - targetPosition) * positionMultiplier;
		double reservePrice = depth.getMidPrice() - (position * this.riskAversion * varianceMidPrice * T_t);
		return reservePrice;
	}

	public double getFirstTermSpreadGueantTapia(double k) {
		//(1/γ)*ln(1 + γ/κ)
		return (1 / riskAversion) * Math.log(1 + (riskAversion / k));
	}

	public double getSecondTermSpreadGueantTapia(double k) {
		//σ2γ/2kA
		double firstTerm = (this.varianceMidPrice * this.riskAversion) / (2 * k * this.aDefault);
		//(1 +γ/ k)^(1+γ/κ)
		double secondTerm = Math.pow((1 + (riskAversion / k)), 1 + (k / riskAversion));
		return Math.sqrt(firstTerm * secondTerm);
	}

	public double[] calculateSpreadGueantTapia() {
		//no market impact pg12 Dealing with inventory risk
		//delta_bid = (1/γ)*ln(1 + γ/κ) + [ ((2*position+1)/2)*]
		double position = (getPosition(this.instrument) - targetPosition) * positionMultiplier;

		double T_t = getTt();//not used here we dont want it
		double[] ks = calculateK();
		if (ks == null) {
			logger.error("something is wrong calculating kValues , returns null");
			return null;
		}
		double kTotal = ks[0];
		double kBid = ks[1];
		double kAsk = ks[2];

		double firstTermBid = getFirstTermSpreadGueantTapia(kBid);
		double secondTermBid = getSecondTermSpreadGueantTapia(kBid);
		double bidMultiplier = ((1 + (2 * position)) / 2);
		double spreadBid_ = firstTermBid + bidMultiplier * secondTermBid;        //s-sb

		double firstTermAsk = getFirstTermSpreadGueantTapia(kAsk);
		double secondTermAsk = getSecondTermSpreadGueantTapia(kAsk);
		double askMultiplier = (((2 * position) - 1) / 2);
		double spreadAsk_ = firstTermAsk + askMultiplier * secondTermAsk;    //sa-s

		double spreadBid = Math.abs(spreadBid_ * spreadMultiplier);
		double spreadAsk = Math.abs(spreadAsk_ * spreadMultiplier);

		return new double[]{spreadBid, spreadAsk};
	}

	private void updateMidPriceQueue(Depth depth) {
		long currentTime = getCurrentTimestamp();
		if (currentTime - lastMidUpdateTimestamp > minMsMidUpdate) {
			//to avoid same depth update
			lastMidUpdateTimestamp = getCurrentTimestamp();
			double midPrice = depth.getMidPrice();
			this.midpricesQueue.add(midPrice);
		}
	}

	@Override
	public boolean onDepthUpdate(Depth depth) {
		if (!super.onDepthUpdate(depth)) {
			return false;
		}

		if (counterStartingQuoteMinuteMs == 0) {
			counterStartingQuoteMinuteMs = getCurrentTimestamp();
		}
		boolean minuteHasPassed = getCurrentTimestamp() - counterStartingQuoteMinuteMs > 60 * 1000;
		if (minuteHasPassed && algorithmState.getNumber() > AlgorithmState.INITIALIZED.getNumber()) {
			counterAskQuotesPerMinute.offer(counterAskQuotes);//counter per minute
			counterBidQuotesPerMinute.offer(counterBidQuotes);//counter per minute
			counterQuotesPerMinute.offer(counterQuotes);//counter per minute
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

		if (getAlgorithmState() != AlgorithmState.STARTED && getAlgorithmState() != AlgorithmState.INITIALIZED) {
			//state is started!
			return false;
		}

		updateMidPriceQueue(depth);
		Double varianceMidPrice = getVarianceMidPrice();
		if (varianceMidPrice == null || !Double.isFinite(varianceMidPrice)) {
			return false;
		}
		this.varianceMidPrice = varianceMidPrice;

		Double reservePrice = calculateReservePrice(depth);//r
		double[] spreads = calculateSpread();//lambda_bid lambda_ask
		if (spreads == null || reservePrice == null) {
			return false;
		}
		double spreadAsk = spreads[1];
		double spreadBid = spreads[0];
		try {

			double askPrice = (reservePrice + spreadAsk);
			double bidPrice = (reservePrice - spreadBid);
			if (spreadCalculationType == SpreadCalculation.GueantTapia) {
				//reserved price is already included in the formulas
				double midPrice = depth.getMidPrice();
				askPrice = (midPrice + spreadAsk);
				bidPrice = (midPrice - spreadBid);
			}

			//apply skew
			if (Math.abs(skewPricePct) > 1E-9) {
				if (skewPricePct > 0.999) {
					askPrice += skewPricePct * instrument.getPriceTick();
					bidPrice += skewPricePct * instrument.getPriceTick();
				} else {
					askPrice *= (1 + skewPricePct);
					bidPrice *= (1 + skewPricePct);
				}

			}

			if (!Double.isFinite(askPrice) || !Double.isFinite(bidPrice)) {
				logger.warn("wrong calculation quoting non finite prices-> ask:{}  bid:{}  reservePrice:{} spreadBid:{} spreadAsk:{} mid:{} skew:{}", askPrice, bidPrice, reservePrice, spreadBid, spreadAsk, depth.getMidPrice(), skewPricePct);
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
		if (kDefault != null && kDefault != NON_SET_PARAMETER) {
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

		//included ion initialization
		if (sigmaDefault != null) {
			//no calculating it
			if (varianceDefault == null) {
				varianceDefault = Math.pow(sigmaDefault, 2);
			}
			return varianceDefault;
		}

		if (midpricesQueue.size() < windowTick) {
			return null;
		}
		Double[] midPricesArr = new Double[windowTick];
		try {
			Double[] midPricesArrtemp = new Double[midpricesQueue.size()];
			midPricesArrtemp = midpricesQueue.toArray(midPricesArrtemp);
			midPricesArr = ArrayUtils.ArrayLastElementsDouble(midPricesArrtemp, windowTick);

		} catch (IndexOutOfBoundsException e) {
			logger.error("error calculating variance on {} windows tick with size {}-> return last varianceMidPrice",
					windowTick, midpricesQueue.size());
			return this.varianceMidPrice;
		}

		return TimeseriesUtils.GetVariance(midPricesArr);
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
