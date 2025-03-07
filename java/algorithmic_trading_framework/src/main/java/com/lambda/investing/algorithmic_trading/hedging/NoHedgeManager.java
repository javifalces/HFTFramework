package com.lambda.investing.algorithmic_trading.hedging;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.Verb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoHedgeManager implements HedgeManager {

	@Override public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
		return true;
	}

	@Override public boolean onDepthUpdate(Depth depth) {
		return true;
	}

	@Override public boolean onTradeUpdate(Trade trade) {
		return true;
	}

	@Override public boolean hedge(Algorithm algorithm, Instrument instrument, double quantityMain, Verb verbMain) {
		return true;
	}

	@Override public Set<Instrument> getInstrumentsHedgeList() {
		return new HashSet<>();
	}
}
