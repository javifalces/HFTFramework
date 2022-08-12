package com.lambda.investing.backtest_engine.zero_mq;

import com.lambda.investing.backtest_engine.AbstractBacktest;
import com.lambda.investing.backtest_engine.BacktestConfiguration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqPublisher;
import com.lambda.investing.market_data_connector.AbstractMarketDataConnectorPublisher;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.market_data_connector.ZeroMqMarketDataConnector;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;
import com.lambda.investing.trading_engine_connector.ZeroMqTradingEngineConnector;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;

public class CSVZeroMqBacktest extends AbstractBacktest {

	public static int THREADS_PUBLISHING_MARKET_DATA_FILE = 1;//
	public static int THREADS_PUBLISHING_ORDER_REQUEST = 0;
	public static int THREADS_PUBLISHING_MARKETDATA = 0;
	public static int THREADS_PUBLISHING_EXECUTION_REPORTS = 1;//not used
	public static int THREADS_LISTENING_EXECUTION_REPORTS = 1;
	public static int THREADS_LISTENING_ORDER_REQUEST = 1;

	private int marketDataPort, tradingEnginePort;
	private String host = "localhost";
	private boolean isSingleThread = true;

	public CSVZeroMqBacktest(BacktestConfiguration backtestConfiguration, int marketDataPort, int tradingEnginePort)
			throws Exception {
		super(backtestConfiguration);
		this.tradingEnginePort = tradingEnginePort;
		this.marketDataPort = marketDataPort;

	}

	public void setSingleThread(boolean singleThread) {
		isSingleThread = singleThread;
		if (singleThread) {
			THREADS_PUBLISHING_MARKET_DATA_FILE = 0;
			THREADS_PUBLISHING_ORDER_REQUEST = 0;
			THREADS_PUBLISHING_MARKETDATA = 0;
			THREADS_PUBLISHING_EXECUTION_REPORTS = 0;
			THREADS_LISTENING_EXECUTION_REPORTS = 0;
			THREADS_LISTENING_ORDER_REQUEST = 0;
		}
	}
	public void setHost(String host) {
		this.host = host;
	}

	@Override protected MarketDataProvider getAlgorithmMarketDataProvider() {
		ZeroMqMarketDataConnector zeroMqMarketDataConnector = new ZeroMqMarketDataConnector(
				(ZeroMqConfiguration) marketDataConnectorConfiguration,
				//todo fix it if need more instruments
				THREADS_PUBLISHING_MARKET_DATA_FILE);
		zeroMqMarketDataConnector.start();
		return zeroMqMarketDataConnector;
	}

	@Override protected TradingEngineConnector getPaperTradingEngineConnector() {
		ZeroMqTradingEngineConnector zeroMqTradingEngineConnector = new ZeroMqTradingEngineConnector(
				this.getClass().getName(), THREADS_PUBLISHING_ORDER_REQUEST, THREADS_LISTENING_EXECUTION_REPORTS,
				(ZeroMqConfiguration) marketDataConnectorConfiguration,
				(ZeroMqConfiguration) tradingEngineConnectorConfiguration);

		return zeroMqTradingEngineConnector;
	}

	@Override protected ConnectorProvider getBacktestOrderRequestProvider() {
		ZeroMqProvider zeroMqProvider = ZeroMqProvider
				.getInstance((ZeroMqConfiguration) tradingEngineConnectorConfiguration,
						THREADS_LISTENING_ORDER_REQUEST);

		return zeroMqProvider;
	}

	@Override protected void constructPaperExecutionReportConnectorPublisher() {
		paperTradingEngineConnector = getPaperTradingEngineConnector();

		paperTradingEngine = new PaperTradingEngine(paperTradingEngineConnector,
				ordinaryMarketDataConnectorProvider, backtestOrderRequestProvider, tradingEngineConnectorConfiguration);

	}

	protected void readFiles() {
		if (algorithmMarketDataProvider instanceof ZeroMqMarketDataConnector) {
			ZeroMqMarketDataConnector zeroMqMarketDataConnector = (ZeroMqMarketDataConnector) algorithmMarketDataProvider;
			zeroMqMarketDataConnector.start();
		}

		if (paperTradingEngineConnector instanceof ZeroMqTradingEngineConnector) {
			ZeroMqTradingEngineConnector zeroMqTradingEngineConnector = (ZeroMqTradingEngineConnector) paperTradingEngineConnector;
			zeroMqTradingEngineConnector.start();
		}

		if (ordinaryMarketDataConnectorPublisher instanceof AbstractMarketDataConnectorPublisher) {
			AbstractMarketDataConnectorPublisher marketDataConnectorPublisher = (AbstractMarketDataConnectorPublisher) this.ordinaryMarketDataConnectorPublisher;
			marketDataConnectorPublisher.init();
		} else {
			logger.error(
					"cant read files : ordinaryMarketDataConnectorPublisher in CSVZeroMqBacktest is not CSVMarketDataConnectorPublisher");
		}
	}


	@Override protected ConnectorConfiguration getMarketDataConnectorConfiguration() {
		ZeroMqConfiguration zeroMqConfiguration = new ZeroMqConfiguration();
		zeroMqConfiguration.setHost(host);
		zeroMqConfiguration.setPort(marketDataPort);
		return zeroMqConfiguration;
	}

	@Override protected ConnectorConfiguration getTradingEngineConnectorConfiguration() {
		ZeroMqConfiguration zeroMqConfiguration = new ZeroMqConfiguration();
		zeroMqConfiguration.setHost(host);
		zeroMqConfiguration.setPort(tradingEnginePort);
		return zeroMqConfiguration;
	}

	@Override protected ConnectorPublisher getBacktestMarketDataAndExecutionReportConnectorPublisher() {
		return new ZeroMqPublisher("zeroMqPublisher_" + this.getClass().getName(), THREADS_PUBLISHING_MARKETDATA);
	}
}
