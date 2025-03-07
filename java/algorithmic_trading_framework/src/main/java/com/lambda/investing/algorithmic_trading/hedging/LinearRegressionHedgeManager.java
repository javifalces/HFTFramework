package com.lambda.investing.algorithmic_trading.hedging;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.hedging.synthetic_portfolio.SyntheticInstrument;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LinearRegressionHedgeManager implements HedgeManager {

    protected Logger logger = LogManager.getLogger(LinearRegressionHedgeManager.class);
    protected Instrument instrument;
    protected SyntheticInstrument syntheticInstrument;
    protected Set<Instrument> interestedInstruments = new HashSet<>();
    protected String syntheticInstrumentFile;

    public LinearRegressionHedgeManager(Instrument instrument, String syntheticInstrumentFile) throws FileNotFoundException {
        this.instrument = instrument;
        this.syntheticInstrumentFile = syntheticInstrumentFile;
        setSyntheticInstrument(new SyntheticInstrument(this.syntheticInstrumentFile));
    }

    public void setSyntheticInstrument(SyntheticInstrument instrument) {
        this.syntheticInstrument = instrument;
        interestedInstruments.addAll(this.syntheticInstrument.getInstruments());
    }

    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        return false;
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        return true;
    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        return true;
    }

    protected void tradeSpread(Algorithm algorithm, Verb verbMain, double quantity) {
        logger.info("hedge {} market order the spread on instrument {} with {} hedge instruments", verbMain, this.instrument,
                this.syntheticInstrument.getInstruments().size());
        hedgeSyntheticInstrumentsMarket(algorithm, verbMain, quantity);

    }

    protected void hedgeSyntheticInstrumentsMarket(Algorithm algorithm, Verb verbMain, double quantityOnMain) {
        Verb verbHedge = Verb.OtherSideVerb(verbMain);

        for (Instrument underlyingInstrument : this.syntheticInstrument.getInstruments()) {
            double beta = this.syntheticInstrument.getBeta(underlyingInstrument);
            OrderRequest orderRequestSynth = null;

            double quantityTrade = Math.abs(quantityOnMain * (1 / beta));
            quantityTrade = underlyingInstrument.roundQty(quantityTrade);

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

            Verb verbHedgeUnderlyingInstrument = verbHedge;
            if (beta < 0) {
                //reversed
                verbHedgeUnderlyingInstrument = Verb.OtherSideVerb(verbHedge);
            }

            orderRequestSynth = algorithm
                    .createMarketOrderRequest(underlyingInstrument, verbHedgeUnderlyingInstrument, quantityTrade);


            logger.info("Hedging {} {} on {} with {} {} on {}", verbMain, quantityOnMain, instrument, verbHedgeUnderlyingInstrument, quantityTrade,
                    underlyingInstrument);
            try {
                algorithm.sendOrderRequest(orderRequestSynth);
            } catch (LambdaTradingException e) {
                logger.error("error sending {} order on {}", verbHedge, underlyingInstrument, e);
            }

        }

    }

    @Override
    public boolean hedge(Algorithm algorithm, Instrument instrument, double quantityMain, Verb verbMain) {
        try {
            tradeSpread(algorithm, verbMain, quantityMain);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Set<Instrument> getInstrumentsHedgeList() {
        return interestedInstruments;
    }
}
