package com.lambda.investing.trading_engine_connector.paper;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import com.lambda.investing.market_data_connector.AbstractMarketDataProvider;
import com.lambda.investing.market_data_connector.MarketDataConnectorPublisher;
import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.portfolio.Portfolio;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.trading_engine_connector.AbstractPaperExecutionReportConnectorPublisher;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;
import com.lambda.investing.trading_engine_connector.paper.latency.FixedLatencyEngine;
import com.lambda.investing.trading_engine_connector.paper.latency.LatencyEngine;
import com.lambda.investing.trading_engine_connector.paper.latency.PoissonLatencyEngine;
import com.lambda.investing.trading_engine_connector.paper.market.OrderMatchEngine;
import com.lambda.investing.trading_engine_connector.paper.market.Orderbook;
import com.lambda.investing.trading_engine_connector.paper.market.OrderbookManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormattedMessage;

import javax.annotation.PostConstruct;
import javax.sound.sampled.Port;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static com.lambda.investing.model.portfolio.Portfolio.REQUESTED_PORTFOLIO_INFO;
import static com.lambda.investing.trading_engine_connector.ZeroMqTradingEngineConnector.ALL_ALGORITHMS_SUBSCRIPTION;
import static com.lambda.investing.trading_engine_connector.ZeroMqTradingEngineConnector.GSON;

public class PaperTradingEngine extends AbstractPaperExecutionReportConnectorPublisher
		implements TradingEngineConnector {

	public static boolean USE_ORDER_MATCHING_ENGINE = true;

	private static String FORMAT_PORTFOLIO =
			Configuration.OUTPUT_PATH + File.separator + "%s_paperTradingEngine_position.json";
	private static final boolean NOTIFY_MARKET_TRADES_NOT_EXECUTED = true;
	private Logger logger = LogManager.getLogger(PaperTradingEngine.class);

	private MarketDataProvider marketDataProvider;
	private MarketMakerMarketDataExecutionReportListener marketMakerMarketDataExecutionReportListener;
	private ConnectorProvider orderRequestConnectorProvider;
	private ConnectorConfiguration orderRequestConnectorConfiguration;

	private PaperConnectorPublisher paperConnectorMarketDataAndExecutionReportPublisher;
	private PaperConnectorOrderRequestListener paperConnectorOrderRequestListener;

	private List<Instrument> instrumentsList = new ArrayList<>();
	private Map<String, OrderbookManager> orderbookManagerMap;
	MarketDataProviderIn marketDataProviderIn;

	Map<String, Portfolio> porfolioMap;

	protected LatencyEngine orderRequestLatencyEngine = new PoissonLatencyEngine(
			65);// //change to more ms on OrdinaryBacktest
	protected LatencyEngine marketDataLatencyEngine = new FixedLatencyEngine(0);
	protected LatencyEngine executionReportLatencyEngine = new FixedLatencyEngine(0);

	private boolean isBacktest = false;

	public List<Instrument> getInstrumentsList() {
		return instrumentsList;
	}

	public MarketDataProvider getMarketDataProviderIn() {
		return marketDataProviderIn;
	}

	public void setOrderRequestLatencyEngine(LatencyEngine orderRequestLatencyEngine) {
		this.orderRequestLatencyEngine = orderRequestLatencyEngine;
	}

	public void setExecutionReportLatencyEngine(LatencyEngine executionReportLatencyEngine) {
		this.executionReportLatencyEngine = executionReportLatencyEngine;
	}

	public PaperTradingEngine(TradingEngineConnector tradingEngineConnector, MarketDataProvider marketDataProvider,
			ConnectorProvider orderRequestConnectorProvider,
			ConnectorConfiguration orderRequestConnectorConfiguration) {
		super(tradingEngineConnector);
		this.marketDataProvider = marketDataProvider;
		this.marketMakerMarketDataExecutionReportListener = new MarketMakerMarketDataExecutionReportListener(this);
		this.orderRequestConnectorProvider = orderRequestConnectorProvider;
		this.orderRequestConnectorConfiguration = orderRequestConnectorConfiguration;

		//portfolio file not on the broker side

		//listen on this side
		porfolioMap = new ConcurrentHashMap<>();

		this.paperConnectorOrderRequestListener = new PaperConnectorOrderRequestListener(this,
				this.orderRequestConnectorProvider, this.orderRequestConnectorConfiguration);

		if (Configuration.isDebugging()) {
			logger.info("debugging detected => set latencies engines to no delay! ");
			setOrderRequestLatencyEngine(new FixedLatencyEngine(0));
			setExecutionReportLatencyEngine(new FixedLatencyEngine(0));
			marketDataLatencyEngine = new FixedLatencyEngine(0);
		}

		marketDataProviderIn = new MarketDataProviderIn(executionReportLatencyEngine, marketDataLatencyEngine,
				Configuration.BACKTEST_THREADS_PUBLISHING_MARKETDATA,
				Configuration.BACKTEST_THREADS_PUBLISHING_EXECUTION_REPORTS);
	}

	public void setBacktest(boolean backtest) {
		isBacktest = backtest;
	}

	public MarketMakerMarketDataExecutionReportListener getMarketMakerMarketDataExecutionReportListener() {
		return marketMakerMarketDataExecutionReportListener;
	}

	@PostConstruct public void init() {
		//subscribe to data
		this.paperConnectorOrderRequestListener.start();
		this.marketDataProvider.register(this.marketMakerMarketDataExecutionReportListener);
		this.register(ALL_ALGORITHMS_SUBSCRIPTION, this.marketMakerMarketDataExecutionReportListener);
		this.orderRequestConnectorProvider
				.register(this.orderRequestConnectorConfiguration, this.paperConnectorOrderRequestListener);

		logger.info("Starting PaperTrading Engine publishing md/er on {}   listening Orders on {}",
				//MD configuration
				this.paperConnectorMarketDataAndExecutionReportPublisher.getConnectorConfiguration()
						.getConnectionConfiguration(),
				this.orderRequestConnectorConfiguration.getConnectionConfiguration());

		//TODO something more generic on not ZeroMq
		if (this.orderRequestConnectorProvider instanceof ZeroMqProvider) {
			ZeroMqProvider orderRequestConnectorProviderZero = (ZeroMqProvider) this.orderRequestConnectorProvider;
			orderRequestConnectorProviderZero.start(false, false);//subscribed to all topics on that port
		}

	}

	public void setInstrumentsList(List<Instrument> instrumentsList) {
		this.instrumentsList = instrumentsList;
		//
		logger.info("creating {} orderbooks", instrumentsList.size());
		orderbookManagerMap = new ConcurrentHashMap<>();
		for (Instrument instrument : instrumentsList) {
			Orderbook orderbook = new Orderbook(instrument.getPriceTick());

			OrderbookManager orderbookManager = null;
			if (!USE_ORDER_MATCHING_ENGINE) {
				orderbookManager = new OrderbookManager(orderbook, this, instrument.getPrimaryKey());
			} else {
				//to avoid stack overflows!
				OrderMatchEngine.REFRESH_DEPTH_ORDER_REQUEST = false;
				OrderMatchEngine.REFRESH_DEPTH_TRADES = false;
				orderbookManager = new OrderMatchEngine(orderbook, this, instrument.getPrimaryKey());

			}
			orderbookManagerMap.put(instrument.getPrimaryKey(), orderbookManager);
		}

	}

	public void setInstrumentsList(Instrument instrument) {
		this.instrumentsList = new ArrayList<>();
		this.instrumentsList.add(instrument);
		setInstrumentsList(this.instrumentsList);

	}

	public void setPaperConnectorMarketDataAndExecutionReportPublisher(
			PaperConnectorPublisher paperConnectorMarketDataAndExecutionReportPublisher) {
		this.paperConnectorMarketDataAndExecutionReportPublisher = paperConnectorMarketDataAndExecutionReportPublisher;
	}

	private String getTopic(String instrumentPk) {
		return instrumentPk;
	}

	private String getTopic(Instrument instrument) {
		return instrument.getPrimaryKey();
	}

	public void notifyCommand(Command command) {
		String topic = "command";
		logger.debug("Notifying command -> \n{}", command.getMessage());

		this.marketDataProviderIn.notifyCommand(command);
	}

	protected void updateLatencyEngineTime(long timestamp, long nextUpdateMs) {
		Date date = new Date(timestamp);
		orderRequestLatencyEngine.setTime(date);
		if (nextUpdateMs != Long.MIN_VALUE) {
			orderRequestLatencyEngine.setNextUpdateMs(nextUpdateMs);
		}
		marketDataProviderIn.updateLatencyEngineTime(date, timestamp, nextUpdateMs);
	}

	/**
	 * Publish the new depth to paper
	 *
	 * @param depth
	 */
	public void notifyDepth(Depth depth) {
		if (!depth.isDepthFilled()) {
			//stop here
			logger.debug("");
		}
		updateLatencyEngineTime(depth.getTimestamp(), depth.getTimeToNextUpdateMs());
		//Orderbook is filled -> notify the rest of algos
		Instrument instrument = Instrument.getInstrument(depth.getInstrument());
		String topic = getTopic(instrument);
		logger.debug("Notifying depth -> \n{}", depth.toString());
		this.marketDataProviderIn.notifyDepth(depth);
	}

	/**
	 * Publish the last trade to paper
	 *
	 * @param trade
	 */
	public void notifyTrade(Trade trade) {
		String topic = getTopic(trade.getInstrument());
		logger.debug("Notifying trade -> \n{}", trade.toString());
		updateLatencyEngineTime(trade.getTimestamp(), trade.getTimeToNextUpdateMs());
		//trade is filled notify the rest
		this.marketDataProviderIn.notifyTrade(trade);

	}

	public void notifyExecutionReport(ExecutionReport executionReport) {
		logger.debug("Notifying execution report -> \n{}", executionReport.toString());
		this.marketDataProviderIn.notifyExecutionReport(executionReport);
		Portfolio portfolio = null;
		if (porfolioMap.containsKey(executionReport.getAlgorithmInfo())) {
			portfolio = porfolioMap.get(executionReport.getAlgorithmInfo());
		} else {
			portfolio = Portfolio
					.getPortfolio(String.format(FORMAT_PORTFOLIO, executionReport.getAlgorithmInfo()), isBacktest);
		}
		portfolio.updateTrade(executionReport);
		porfolioMap.put(executionReport.getAlgorithmInfo(), portfolio);

	}

	public MarketDataConnectorPublisher getMarketDataConnectorPublisher() {
		return paperConnectorMarketDataAndExecutionReportPublisher;
	}

	public boolean orderRequest(OrderRequest orderRequest) {
		//Send orders to the virtual orderbook
		OrderbookManager orderbookManager = orderbookManagerMap.get(orderRequest.getInstrument());
		if (orderbookManager == null) {
			logger.error("trying to send orderRequest on {} not found in manager", orderRequest.getInstrument());
			return false;
		}
		orderRequestLatencyEngine.delay(orderRequestLatencyEngine.getCurrentTime());
		orderRequest.setTimestampCreation(
				orderRequestLatencyEngine.getCurrentTime().getTime());//update OrderRequestTime if required
		return orderbookManager.orderRequest(orderRequest);
	}

	@Override public void requestInfo(String info) {
		if (info.endsWith(REQUESTED_PORTFOLIO_INFO)) {
			//return portfolio on execution Report
			String algorithmInfo = info.split("[.]")[0];
			Portfolio portfolio = porfolioMap.getOrDefault(algorithmInfo,
					Portfolio.getPortfolio(String.format(FORMAT_PORTFOLIO, algorithmInfo), isBacktest));

			porfolioMap.put(algorithmInfo, portfolio);
			this.marketDataProviderIn.notifyInfo(info, GSON.toJson(portfolio));
		}
	}

	public void fillOrderbook(Depth depth) {
		OrderbookManager orderbookManager = orderbookManagerMap.get(depth.getInstrument());
		if (orderbookManager == null) {
			return;
		}
		orderbookManager.refreshMarketMakerDepth(depth);
	}

	public boolean fillMarketTrade(Trade trade) {
		OrderbookManager orderbookManager = orderbookManagerMap.get(trade.getInstrument());
		boolean isNotifiedByExecution = false;
		if (orderbookManager != null) {
			isNotifiedByExecution = orderbookManager.refreshFillMarketTrade(trade);
		}
		if (NOTIFY_MARKET_TRADES_NOT_EXECUTED && !isNotifiedByExecution) {
			//notify by market
			//			logger.debug("orderbook fill trade ->  {}",trade.toString());
			notifyTrade(trade);
		}
		return true;
	}

	private class MarketDataProviderIn extends AbstractMarketDataProvider {

		LatencyEngine executionReportLatencyEngine = null;
		LatencyEngine marketDataLatencyEngine = null;

		private int threadsPublishingMd, threadsPublishingER;
		ThreadFactory namedThreadFactoryMarketData = new ThreadFactoryBuilder()
				.setNameFormat("MarketDataProviderIn-MarketData-%d").build();
		ThreadFactory namedThreadFactoryExecutionReport = new ThreadFactoryBuilder()
				.setNameFormat("MarketDataProviderIn-ExecutionReport-%d").build();
		ThreadPoolExecutor marketDataPool, executionReportPool;

		public MarketDataProviderIn(LatencyEngine executionReportLatencyEngine, LatencyEngine marketDataLatencyEngine,
				int threadsPublishingMd, int threadsPublishingER) {
			this.executionReportLatencyEngine = executionReportLatencyEngine;
			this.marketDataLatencyEngine = marketDataLatencyEngine;

			this.threadsPublishingER = threadsPublishingER;
			this.threadsPublishingMd = threadsPublishingMd;

			if (this.threadsPublishingER > 0) {
				executionReportPool = (ThreadPoolExecutor) Executors
						.newFixedThreadPool(this.threadsPublishingER, namedThreadFactoryExecutionReport);
			}
			if (this.threadsPublishingER < 0) {
				executionReportPool = (ThreadPoolExecutor) Executors
						.newCachedThreadPool(namedThreadFactoryExecutionReport);
			}

			if (this.threadsPublishingMd > 0) {
				marketDataPool = (ThreadPoolExecutor) Executors
						.newFixedThreadPool(this.threadsPublishingMd, namedThreadFactoryMarketData);
			}
			if (this.threadsPublishingMd < 0) {
				marketDataPool = (ThreadPoolExecutor) Executors.newCachedThreadPool(namedThreadFactoryMarketData);
			}

		}

		@Override public void notifyDepth(Depth depth) {
			if (threadsPublishingMd == 0) {
				super.notifyDepth(depth);
			} else {
				marketDataPool.submit(() -> {
					marketDataLatencyEngine.delay(new Date(depth.getTimestamp()));
					super.notifyDepth(depth);
				});
			}

		}

		@Override public void notifyTrade(Trade trade) {
			if (threadsPublishingMd == 0) {
				super.notifyTrade(trade);
			} else {
				marketDataPool.submit(() -> {
					marketDataLatencyEngine.delay(new Date(trade.getTimestamp()));
					super.notifyTrade(trade);
				});
			}

		}

		@Override public void notifyExecutionReport(ExecutionReport executionReport) {
			if (threadsPublishingER == 0) {
				super.notifyExecutionReport(executionReport);
			} else {
				executionReportPool.submit(() -> {
					marketDataLatencyEngine.delay(new Date(executionReport.getTimestampCreation()));
					super.notifyExecutionReport(executionReport);
				});
			}

		}

		protected void updateLatencyEngineTime(Date date, long timestamp, long nextUpdateMs) {
			marketDataLatencyEngine.setTime(date);
			if (nextUpdateMs != Long.MIN_VALUE) {
				marketDataLatencyEngine.setNextUpdateMs(nextUpdateMs);
			}

			executionReportLatencyEngine.setTime(date);
			if (nextUpdateMs != Long.MIN_VALUE) {
				executionReportLatencyEngine.setNextUpdateMs(nextUpdateMs);
			}
		}
	}

}
