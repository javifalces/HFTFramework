package com.lambda.investing.trading_engine_connector.paper;

import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.lambda.investing.trading_engine_connector.paper.market.OrderbookManager.MARKET_MAKER_ALGORITHM_INFO;

/**
 * Listen to depth from source and fill synthetic orderbook => TRADE ENGINE can be only papertrading
 */
public class MarketMakerMarketDataExecutionReportListener implements MarketDataListener, ExecutionReportListener {

	Logger logger = LogManager.getLogger(PaperTradingEngine.class);
	private PaperTradingEngine tradingEngineConnector;

	public MarketMakerMarketDataExecutionReportListener(PaperTradingEngine tradingEngineConnector) {
		this.tradingEngineConnector = tradingEngineConnector;
	}

	private void refreshDepth(Depth depth) {
		//modify mm orders with new depth
		tradingEngineConnector.fillOrderbook(depth);
		//notification of new depth will be inside the method
	}

	private void refreshTrade(Trade trade) {
		//is acting as a market order
		tradingEngineConnector.fillMarketTrade(trade);
	}

	@Override public boolean onDepthUpdate(Depth depth) {
		//		logger.info("MM depth update receive");
		refreshDepth(depth);
		return true;

	}

	@Override public boolean onTradeUpdate(Trade trade) {
		//		logger.info("MM trade update receive");
		refreshTrade(trade);
		return true;
	}

	@Override public boolean onCommandUpdate(Command command) {
		//received command un market maker , not needed
		logger.debug("command received");
		tradingEngineConnector.notifyCommand(command);
		return true;
	}

	@Override public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
		//notify the rest if not market maker
		if (!executionReport.getAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
			tradingEngineConnector.notifyExecutionReport(executionReport);

		}
		return true;
	}

	@Override public boolean onInfoUpdate(String header, String message) {
		return false;
	}
}
