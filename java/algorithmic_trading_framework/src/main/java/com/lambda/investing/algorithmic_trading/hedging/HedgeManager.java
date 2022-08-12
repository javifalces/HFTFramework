package com.lambda.investing.algorithmic_trading.hedging;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.Verb;

import java.util.List;
import java.util.Set;

public interface HedgeManager {

	boolean onExecutionReportUpdate(ExecutionReport executionReport);

	boolean onDepthUpdate(Depth depth);

	boolean onTradeUpdate(Trade trade);

	boolean hedge(Algorithm algorithm, Instrument instrument, double quantityMain, Verb verbMain);

	Set<Instrument> getInstrumentsHedgeList();

}
