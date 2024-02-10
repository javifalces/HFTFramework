package com.lambda.investing.algorithmic_trading.hedging;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.statistical_arbitrage.synthetic_portfolio.SyntheticInstrument;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.Verb;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LinearRegressionHedgeManager implements HedgeManager {

	protected Logger logger = LogManager.getLogger(LinearRegressionHedgeManager.class);
	protected Instrument instrument;
	protected SyntheticInstrument syntheticInstrument;
	protected Set<Instrument> interestedInstruments = new HashSet<>();
	protected String syntheticInstrumentFile;

	public LinearRegressionHedgeManager(String syntheticInstrumentFile) throws FileNotFoundException {
		this.syntheticInstrumentFile = syntheticInstrumentFile;
		setSyntheticInstrument(new SyntheticInstrument(this.syntheticInstrumentFile));
	}

	public void setSyntheticInstrument(SyntheticInstrument instrument) {
		this.syntheticInstrument = instrument;
		interestedInstruments.addAll(this.syntheticInstrument.getInstrumentToBeta().keySet());
	}

	@Override public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
		return false;
	}

	@Override public boolean onDepthUpdate(Depth depth) {
		return true;
	}

	@Override public boolean onTradeUpdate(Trade trade) {
		return true;
	}

	protected void tradeSpread(Algorithm algorithm, Verb verb, double quantity) {
		logger.info("hedge {} market order the spread on instrument {} - {}", verb, this.instrument,
				this.syntheticInstrument.getInstrumentToBeta().size());
		hedgeSyntheticInstrumentsMarket(algorithm, verb, quantity);

	}

	protected void hedgeSyntheticInstrumentsMarket(Algorithm algorithm, Verb verb, double quantityOnMain) {
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
			double minQty = underlyingInstrument.getQuantityTick();
			if (quantityTrade < minQty) {
				logger.warn("no hedging on {} with beta:{} because quantityToTrade {} < {}minQty instrument",
						underlyingInstrument, beta, quantityTrade, minQty);
			}

			Verb verbToHedge = syntheticVerb;
			if (beta > 0) {
				//operate the other side of the main instrument
				verbToHedge = syntheticVerb;
				orderRequestSynth = algorithm
						.createMarketOrderRequest(underlyingInstrument, verbToHedge, quantityTrade);
			}
			if (beta < 0) {
				//operate same side of the main instrument
				verbToHedge = verb;
				orderRequestSynth = algorithm
						.createMarketOrderRequest(underlyingInstrument, verbToHedge, quantityTrade);
			}

			logger.info("Hedging {} on {} with {} {} on {}", quantityOnMain, instrument, verbToHedge, quantityTrade,
					underlyingInstrument);
			try {
				algorithm.sendOrderRequest(orderRequestSynth);
			} catch (LambdaTradingException e) {
				logger.error("error sending {} order on {}", syntheticVerb, instrument, e);
			}

		}

	}

	@Override public boolean hedge(Algorithm algorithm, Instrument instrument, double quantityMain, Verb verbMain) {
		try {
			tradeSpread(algorithm, verbMain, quantityMain);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override public Set<Instrument> getInstrumentsHedgeList() {
		return interestedInstruments;
	}
}
