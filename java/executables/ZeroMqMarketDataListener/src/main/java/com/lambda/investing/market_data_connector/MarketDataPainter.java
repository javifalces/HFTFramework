package com.lambda.investing.market_data_connector;

import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class MarketDataPainter implements MarketDataListener {

	MarketDataProvider marketDataProvider;

	protected Logger logger = LogManager.getLogger(MarketDataPainter.class);

	public MarketDataPainter() {

	}

	public void setMarketDataProvider(MarketDataProvider marketDataProvider) {
		this.marketDataProvider = marketDataProvider;
	}

	public void init(){
		logger.info("init MarketDataPainter");
		//todo why is null?
		marketDataProvider.register(this);
	}

	@Override public boolean onDepthUpdate(Depth depth) {
		return false;
		//		logger.info("received {}",depth.toString());

	}

	@Override public boolean onTradeUpdate(Trade trade) {
		return false;
		//		logger.info("received {}",trade.toString());
	}

	@Override public boolean onCommandUpdate(Command command) {
		return false;
	}

	@Override public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
		return false;
	}

	@Override public boolean onInfoUpdate(String header, String message) {
		return false;
	}

}
