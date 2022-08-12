package com.lambda.investing.algorithmic_trading.statistical_arbitrage;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.statistical_arbitrage.synthetic_portfolio.SyntheticInstrument;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.portfolio.Portfolio;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.Verb;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.Table;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StatisticalArbitrageAlgorithm extends Algorithm {

	protected Logger logger = LogManager.getLogger(StatisticalArbitrageAlgorithm.class);
	protected Instrument instrument;
	protected SyntheticInstrument syntheticInstrument;
	protected double quantity;
	protected double zscoreEntryBuy = -2;
	protected double zscoreEntrySell = 2;
	protected double zscoreExitBuy = -1;
	protected double zscoreExitSell = 1;
	protected static boolean OPERATE_SYNTHETIC_INSTRUMENT = true;

	private boolean allDataReceived = false;


	private Map<Instrument, Double> lastBidMap = new ConcurrentHashMap<>();
	private Map<Instrument, Double> lastAskMap = new ConcurrentHashMap<>();
	private Map<Instrument, Double> lastMidMap = new ConcurrentHashMap<>();
	private Map<Instrument, Double> lastTradePriceMap = new ConcurrentHashMap<>();

	private Map<Instrument, Double> lastBidReturnMap = new ConcurrentHashMap<>();
	private Map<Instrument, Double> lastAskReturnMap = new ConcurrentHashMap<>();
	private Map<Instrument, Double> lastMidReturnMap = new ConcurrentHashMap<>();
	protected Map<Instrument, Double> lastTradeReturnMap = new ConcurrentHashMap<>();

	protected double syntheticBidReturn, syntheticAskReturn, mainBidReturn, mainAskReturn, syntheticTradePriceReturn, mainTradePriceReturn;
	protected volatile boolean weAreInTheMarket = false;
	protected Verb ourSideInTheMarket;

	protected double zscore;
	protected double action;

	@Override public String printAlgo() {
		return String
				.format("%s  \n\tmainInstrument=%s\n\tsyntheticInstrument=%s\n\tquantity=%.3f\n\tfirstHourOperatingIncluded=%d\n\tlastHourOperatingIncluded=%d",
						algorithmInfo, instrument, syntheticInstrument, quantity, firstHourOperatingIncluded,
						lastHourOperatingIncluded);
	}

	public StatisticalArbitrageAlgorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
			String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters);
		setParameters(parameters);

	}

	public StatisticalArbitrageAlgorithm(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);
		setParameters(parameters);
	}



	@Override public void setParameters(Map<String, Object> parameters) {
		super.setParameters(parameters);
		this.quantity = getParameterDouble(parameters, "quantity");

		this.zscoreEntryBuy = getParameterDoubleOrDefault(parameters, "zscoreEntryBuy", -2);
		this.zscoreEntrySell = getParameterDoubleOrDefault(parameters, "zscoreEntrySell", 2);
		this.zscoreExitBuy = getParameterDoubleOrDefault(parameters, "zscoreExitBuy", -1);
		this.zscoreExitSell = getParameterDoubleOrDefault(parameters, "zscoreExitSell", 1);

		//in case from backtest
		String instrumentPK = getParameterString(parameters, "instrument");
		if (instrumentPK != null) {
			Instrument instrument = Instrument.getInstrument(instrumentPK);
			setInstrument(instrument);
		}
		String syntheticInstrumentPath = getParameterString(parameters, "syntheticInstrumentFile");
		if (syntheticInstrumentPath != null) {
			try {
				setSyntheticInstrument(new SyntheticInstrument(syntheticInstrumentPath));
			} catch (FileNotFoundException e) {
				logger.error("error setting synthetic instrument on {}", syntheticInstrumentPath, e);
				System.err.println("error setting synthetic instrument on " + syntheticInstrumentPath);
				e.printStackTrace();
			}
		} else {
			System.err.println("synthetic instrument not received!");
		}

	}

	public void setInstrument(Instrument instrument) {
		this.instrument = instrument;
		instruments.add(this.instrument);

	}

	public void setSyntheticInstrument(SyntheticInstrument instrument) {
		this.syntheticInstrument = instrument;
		instruments.addAll(this.syntheticInstrument.getInstrumentToBeta().keySet());
	}

	public void init() {
		super.init();
	}

	@Override
	public void resetAlgorithm() {
		super.resetAlgorithm();

		this.allDataReceived = false;
		lastBidMap = new ConcurrentHashMap<>();
		lastAskMap = new ConcurrentHashMap<>();
		lastMidMap = new ConcurrentHashMap<>();

		lastMidReturnMap = new ConcurrentHashMap<>();
		lastAskReturnMap = new ConcurrentHashMap<>();
		lastBidReturnMap = new ConcurrentHashMap<>();

		weAreInTheMarket = false;
	}

	@Override public boolean onTradeUpdate(Trade trade) {
		String instrumentPk = trade.getInstrument();
		Instrument instrument = Instrument.getInstrument(instrumentPk);
		if (!this.instruments.contains(instrument)) {
			return false;
		}
		boolean outputSuper = super.onTradeUpdate(trade);
		if ((isBacktest || isPaper) && trade.getAlgorithmInfo() != null && trade.getAlgorithmInfo()
				.equalsIgnoreCase(algorithmInfo)) {
			return outputSuper;
		}
		try {
			double lastPrice = lastTradePriceMap.getOrDefault(instrument, trade.getPrice());
			double returnPrice = trade.getPrice() / lastPrice;
			lastTradePriceMap.put(instrument, trade.getPrice());
			lastTradeReturnMap.put(instrument, returnPrice);
		} catch (ArrayIndexOutOfBoundsException e) {
			lastTradePriceMap.remove(instrument);
		}

		this.allDataReceived = lastTradePriceMap.size() == (this.instruments.size());
		//add logic
		if (outputSuper && this.allDataReceived) {
			businessLogic(this.instrument);//force update on my main instrument
		}

		return outputSuper;
	}

	@Override public boolean onDepthUpdate(Depth depth) {
		String instrumentPk = depth.getInstrument();
		Instrument instrument = Instrument.getInstrument(instrumentPk);
		if (!this.instruments.contains(instrument)) {
			return false;
		}

		boolean outputSuper = super.onDepthUpdate(depth);

		for (Instrument instrumentIt : instruments) {
			String pk = instrumentIt.getPrimaryKey();
			addCurrentCustomColumn(pk, "zscore", zscore);
			//			addCurrentCustomColumn(pk, "tradeReturn", lastTradeReturnMap.getOrDefault(instrumentIt,1.0));
			//			addCurrentCustomColumn(pk, "synthTradeReturn", syntheticTradePriceReturn);
			if (!instrumentIt.equals(this.instrument)) {
				action *= -1;
			}
			addCurrentCustomColumn(pk, "action", action);
		}

		//		try {
		//			double lastBid = lastBidMap.getOrDefault(instrument, depth.getBestBid());
		//			double returnBid = depth.getBestBid() / lastBid;
		//			lastBidMap.put(instrument, depth.getBestBid());
		//			lastBidReturnMap.put(instrument, returnBid);
		//		} catch (ArrayIndexOutOfBoundsException e) {
		//			lastBidMap.remove(instrument);
		//		}
		//
		//		try {
		//			double lastAsk = lastAskMap.getOrDefault(instrument, depth.getBestAsk());
		//			double returnAsk = depth.getBestAsk() / lastAsk;
		//			lastAskMap.put(instrument, depth.getBestAsk());
		//			lastAskReturnMap.put(instrument, returnAsk);
		//		} catch (ArrayIndexOutOfBoundsException e) {
		//			lastAskMap.remove(instrument);
		//		}
		//
		//		try {
		//			double lastMid = lastMidMap.getOrDefault(instrument, depth.getMidPrice());
		//			double returnMid = depth.getMidPrice() / lastMid;
		//			lastMidMap.put(instrument, depth.getMidPrice());
		//			lastMidReturnMap.put(instrument, returnMid);
		//		} catch (ArrayIndexOutOfBoundsException e) {
		//			lastMidMap.remove(instrument);
		//		}
		//		this.allDataReceived =
		//				lastAskMap.size() == lastBidMap.size() && lastAskMap.size() == (this.interestedInstruments.size());
		//
		//		//add logic
		//		if (outputSuper && this.allDataReceived) {
		//			businessLogic(instrument);
		//		}
		return outputSuper;
	}

	protected void hedgeSyntheticInstrumentsMarket(Verb verb, double quantityOnMain) {
		Verb syntheticVerb = Verb.Buy;

		if (verb.equals(Verb.Buy)) {
			syntheticVerb = Verb.Sell;
		}

		for (Map.Entry<Instrument, Double> entry : this.syntheticInstrument.getInstrumentToBeta().entrySet()) {
			double beta = entry.getValue();
			Instrument underlyingInstrument = entry.getKey();
			OrderRequest orderRequestSynth = null;

			double quantityTrade = Math.abs(quantityOnMain * beta);
			quantityTrade = instrument.roundQty(quantityTrade);

			if (beta == 0) {
				continue;
			}
			if (quantityTrade == 0) {
				logger.warn(
						"something is wrong trying to trade zeroQuantity on synthetic {}  beta:{}   main qty:{}  => more qty on main or remove instrument",
						underlyingInstrument, beta, quantityOnMain);
				continue;
			}

			if (beta > 0) {
				//operate the other side of the main instrument
				orderRequestSynth = createMarketOrderRequest(underlyingInstrument, syntheticVerb, quantityTrade);
			}
			if (beta < 0) {
				//operate same side of the main instrument
				orderRequestSynth = createMarketOrderRequest(underlyingInstrument, verb, quantityTrade);
			}
			try {
				this.sendOrderRequest(orderRequestSynth);
			} catch (LambdaTradingException e) {
				logger.error("error sending {} order on {}", syntheticVerb, instrument, e);
			}

		}

	}

	protected void tradeSpread(Instrument instrument, Verb verb, double quantity, boolean isEntry) {
		String entryMessage = isEntry ? "Entry" : "Exit";
		logger.info("{} {} market order the spread on instrument {} - {}SynthPortfolio with zScore:{}", entryMessage,
				verb,
				this.instrument,
				this.syntheticInstrument.getInstrumentToBeta().size(), this.zscore);
		OrderRequest orderRequest = createMarketOrderRequest(instrument, verb, quantity);
		try {
			this.sendOrderRequest(orderRequest);
		} catch (LambdaTradingException e) {
			logger.error("error sending {} order on {}", verb, instrument, e);
		}
		if (OPERATE_SYNTHETIC_INSTRUMENT) {
			hedgeSyntheticInstrumentsMarket(verb, this.quantity);
		}

	}

	/**
	 * Synchronized to avoid double executions on multiple updates
	 *
	 * @param instrument
	 */
	protected synchronized void businessLogic(Instrument instrument) {
		//add stat arb logic

		//update syntheticBid syntheticAsk syntheticMid
		updateSyntheticPrices();

		mainTradePriceReturn = lastTradeReturnMap.get(this.instrument);
		double residual = mainTradePriceReturn - syntheticTradePriceReturn;
		zscore = (residual - this.syntheticInstrument.getMean()) / this.syntheticInstrument.getStd();

		//		System.out.println(String.format("zscore buy -> %.3f    zscore sell -> %.3f ",zscoreBuy,zscoreSell));
		//ENTRY side

		if (!weAreInTheMarket) {
			if (ourSideInTheMarket == null && zscore < this.zscoreEntryBuy) {
				this.tradeSpread(this.instrument, Verb.Buy, this.quantity, true);
				//				System.out.println(String.format("zscore entry buy spread-> %.3f< %.3f  ",zscoreBuy,this.zscoreEntryBuy));
				ourSideInTheMarket = Verb.Buy;
				weAreInTheMarket = true;
				action = 2;

			}
			if (ourSideInTheMarket == null && zscore > this.zscoreEntrySell) {
				this.tradeSpread(this.instrument, Verb.Sell, this.quantity, true);
				//				System.out.println(String.format("zscore entry sell spread-> %.3f> %.3f  ",zscoreSell,this.zscoreEntrySell));
				ourSideInTheMarket = Verb.Sell;
				weAreInTheMarket = true;
				action = -2;
			}
		} else {
			//exit side
			if (ourSideInTheMarket != null && ourSideInTheMarket.equals(Verb.Buy) && zscore > this.zscoreExitBuy) {
				this.tradeSpread(this.instrument, Verb.Sell, this.quantity, false);
				//				System.out.println(String.format("zscore exit buy spread-> %.3f> %.3f  ",zscoreBuy,this.zscoreExitBuy));
				weAreInTheMarket = false;
				ourSideInTheMarket = null;
				action = 1;
			}
			if (ourSideInTheMarket != null && ourSideInTheMarket.equals(Verb.Sell) && zscore < this.zscoreExitSell) {
				this.tradeSpread(this.instrument, Verb.Buy, this.quantity, false);
				//				System.out.println(String.format("zscore exit sell spread-> %.3f< %.3f  ",zscoreSell,this.zscoreExitSell));
				weAreInTheMarket = false;
				ourSideInTheMarket = null;
				action = -1;
			}

		}

	}

	protected void updateSyntheticPrices() {

		double synthReturnTemp = 0;
		//		double synthMidReturnTemp = 0;
		for (Map.Entry<Instrument, Double> entry : syntheticInstrument.getInstrumentToBeta().entrySet()) {
			double beta = entry.getValue();
			Instrument instrument = entry.getKey();
			synthReturnTemp += beta * lastTradeReturnMap.get(instrument);
		}

		this.syntheticTradePriceReturn = synthReturnTemp + this.syntheticInstrument.getIntercept();
	}

	@Override public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
		boolean output = super.onExecutionReportUpdate(executionReport);
		//TODO add retrials logic in case of rejection
		return output;
	}

	protected void setPortfolio(Portfolio portfolio) {
		//start empty always
	}

	@Override protected void printRowTrade(ExecutionReport executionReport) {
		String outputInstrument = "";
		int numberTrades = 0;
		double totalPnl = 0.;
		double realizedPnl = 0.;
		double unrealizedPnl = 0.;
		for (String instrumentPk : instrumentToManager.keySet()) {
			PnlSnapshot pnlSnapshot = portfolioManager.getLastPnlSnapshot(instrumentPk);
			if (pnlSnapshot == null || pnlSnapshot.numberOfTrades.get() == 0) {
				continue;
			}
			numberTrades += pnlSnapshot.numberOfTrades.get();
			realizedPnl += pnlSnapshot.realizedPnl;
			unrealizedPnl += pnlSnapshot.unrealizedPnl;

			outputInstrument += String.format(" %s[%.5f]", instrumentPk, pnlSnapshot.netPosition);

		}
		String side = "";
		if (getPosition(this.instrument) > 0) {
			side = "BUY";
		}
		if (getPosition(this.instrument) < 0) {
			side = "SELL";
		}

		String header = String
				.format("\r statArb-%s  %s  zScore:%.3f trades:%d totalPnl:%.3f realizedPnl:%.3f unrealizedPnl:%.3f",
						this.instrument, side, zscore,
						numberTrades, totalPnl, realizedPnl, unrealizedPnl);

		System.out.print(header + outputInstrument);
	}

	@Override protected void printBacktestResults(Map<Instrument, Table> tradesTable) {
		double totalPnl = 0.;
		double unrealizedPnl = 0.;
		double realizedPnl = 0.;
		for (Instrument instrument : tradesTable.keySet()) {
			String output = (portfolioManager.summary(instrument));
			Table tradesInstrument = tradesTable.get(instrument);
			if (isBacktest) {
				System.out.println(instrument);
				System.out.println(output);
			}
			totalPnl += portfolioManager.getLastPnlSnapshot(instrument.getPrimaryKey()).totalPnl;
			realizedPnl += portfolioManager.getLastPnlSnapshot(instrument.getPrimaryKey()).realizedPnl;
			unrealizedPnl += portfolioManager.getLastPnlSnapshot(instrument.getPrimaryKey()).unrealizedPnl;
		}
		if (isBacktest) {
			System.out.println("------------");
			System.out.println("totalPnl:" + totalPnl);
			System.out.println("realizedPnl:" + realizedPnl);
			System.out.println("unrealizedPnl:" + unrealizedPnl);
			System.out.println("------------");
		}

	}
}
