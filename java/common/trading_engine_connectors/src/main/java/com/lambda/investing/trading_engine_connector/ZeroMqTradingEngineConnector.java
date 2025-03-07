package com.lambda.investing.trading_engine_connector;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqPublisher;
import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.market_data_connector.ZeroMqMarketDataConnector;
import com.lambda.investing.market_data_connector.ordinary.OrdinaryMarketDataProvider;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.messaging.TopicUtils;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.trading_engine_connector.paper.PaperConnectorPublisher;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngineConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.lambda.investing.model.Util.fromJsonString;
import static com.lambda.investing.model.Util.toJsonString;
import static com.lambda.investing.model.portfolio.Portfolio.REQUESTED_POSITION_INFO;

//Think how to add paper trading here!
public class ZeroMqTradingEngineConnector implements TradingEngineConnector, ConnectorListener {

	public static String ALL_ALGORITHMS_SUBSCRIPTION = "*";
	private String name;
	protected Logger logger = LogManager.getLogger(ZeroMqTradingEngineConnector.class);
	private ZeroMqConfiguration zeroMqConfigurationExecutionReportListening, zeroMqConfigurationOrderRequest;
	private ZeroMqProvider zeroMqExecutionReportProvider;
	private ZeroMqPublisher zeroMqPublisher;
	private ExecutionReportListener allAlgorithmsExecutionReportListener;

	protected Map<String, Map<ExecutionReportListener, String>> listenersManager;

	private boolean isPaperTrading = false;
	private PaperTradingEngine paperTradingEngine = null;
	private List<Instrument> instrumentList = null;//for paper trading only
	private List<String> cfTradesNotified;


	/***
	 * Trader engine for generic brokers
	 * @param name
	 * @param threadsPublish
	 * @param threadsListen
	 * @param zeroMqConfigurationExecutionReportListening
	 * @param zeroMqConfigurationOrderRequest
	 */
	public ZeroMqTradingEngineConnector(String name, int threadsPublish, int threadsListen,
										ZeroMqConfiguration zeroMqConfigurationExecutionReportListening,
										ZeroMqConfiguration zeroMqConfigurationOrderRequest) {
		this.name = name;
		this.zeroMqConfigurationExecutionReportListening = zeroMqConfigurationExecutionReportListening;
		//listen the answers here
		zeroMqExecutionReportProvider = ZeroMqProvider
				.getInstance(this.zeroMqConfigurationExecutionReportListening, threadsListen);
		zeroMqExecutionReportProvider.register(this.zeroMqConfigurationExecutionReportListening, this);
		logger.info("Listening ExecutionReports on topic {}   in {}",
				zeroMqConfigurationExecutionReportListening.getTopic(),
				zeroMqConfigurationExecutionReportListening.getUrl());

		//publish the request here
		this.zeroMqConfigurationOrderRequest = zeroMqConfigurationOrderRequest;
		this.zeroMqPublisher = new ZeroMqPublisher(name, threadsPublish);
		this.zeroMqPublisher.setServer(false);

		logger.info("Publishing OrderRequests on topic {}   in {}", this.zeroMqConfigurationOrderRequest.getTopic(),
				this.zeroMqConfigurationOrderRequest.getUrl());

		this.zeroMqPublisher
				.publish(this.zeroMqConfigurationOrderRequest, TypeMessage.command, "*", "starting publishing");

		listenersManager = new ConcurrentHashMap<>();
		cfTradesNotified = new ArrayList<>();
		//portfolio file not on the broker side
		//		portfolio = Portfolio.getPortfolio(Configuration.OUTPUT_PATH + File.separator + name + "_position.json");

	}

	@Override
	public boolean isBusy() {
		return false;
	}

	public void start() {
		zeroMqExecutionReportProvider.start(true, true);
	}

	@Override
	public void register(String algorithmInfo, ExecutionReportListener executionReportListener) {

		Map<ExecutionReportListener, String> insideMap = listenersManager
				.getOrDefault(algorithmInfo, new ConcurrentHashMap<>());
		insideMap.put(executionReportListener, "");
		if (algorithmInfo.equalsIgnoreCase(ALL_ALGORITHMS_SUBSCRIPTION)) {
			allAlgorithmsExecutionReportListener = executionReportListener;
		}
		listenersManager.put(algorithmInfo, insideMap);

	}

	@Override public void deregister(String algorithmInfo, ExecutionReportListener executionReportListener) {
		Map<ExecutionReportListener, String> insideMap = listenersManager
				.getOrDefault(algorithmInfo, new ConcurrentHashMap<>());
		insideMap.remove(executionReportListener);
		listenersManager.put(algorithmInfo, insideMap);
	}

	@Override public boolean orderRequest(OrderRequest orderRequest) {
		if (isPaperTrading && paperTradingEngine != null) {
			return this.paperTradingEngine.orderRequest(orderRequest);
		} else {
			String topic = TopicUtils.getTopic(orderRequest.getInstrument(), TypeMessage.order_request);
			String message = toJsonString(orderRequest);
			this.zeroMqPublisher
					.publish(this.zeroMqConfigurationOrderRequest, TypeMessage.order_request, topic, message);
			logger.info("ZeroMQ order request -> {}", orderRequest);
			return true;
		}
	}

	@Override
	public void requestInfo(String info) {
		if (isPaperTrading && paperTradingEngine != null) {
			this.paperTradingEngine.requestInfo(info);
		} else {
			this.zeroMqPublisher.publish(this.zeroMqConfigurationOrderRequest, TypeMessage.info, TypeMessage.info.toString(), info);
		}
	}

	@Override
	public void reset() {
		this.paperTradingEngine.reset();
	}

	public void notifyExecutionReport(ExecutionReport executionReport) {
		boolean isCfTrade = executionReport.getExecutionReportStatus().name()
				.equalsIgnoreCase(ExecutionReportStatus.CompletellyFilled.name());
		if (isCfTrade) {
			logger.info("Cf ER on {}  {}@{} {} ", executionReport.getInstrument(), executionReport.getLastQuantity(),
					executionReport.getPrice(), executionReport.getClientOrderId());
		}
		if (isCfTrade && cfTradesNotified.contains(executionReport.getClientOrderId())) {
			logger.info("discard update of already notified cf trade {}", executionReport.getClientOrderId());
			return;
		}
		String algorithmInfo = executionReport.getAlgorithmInfo();
		Map<ExecutionReportListener, String> insideMap = listenersManager
				.getOrDefault(algorithmInfo, new ConcurrentHashMap<>());
		if (insideMap.size() > 0) {
			for (ExecutionReportListener executionReportListener : insideMap.keySet()) {
				executionReportListener.onExecutionReportUpdate(executionReport);
			}
		}
		if (allAlgorithmsExecutionReportListener != null && !isPaperTrading) {
			//on paper trading will stack over flow
			allAlgorithmsExecutionReportListener.onExecutionReportUpdate(executionReport);
		}
		if (isCfTrade) {
			cfTradesNotified.add(executionReport.getClientOrderId());
		}
	}

	public void notifyInfo(String header, String message) {
		String algorithmInfo = header.split("[.]")[0];

		Map<ExecutionReportListener, String> insideMap = listenersManager
				.getOrDefault(algorithmInfo, new ConcurrentHashMap<>());

		if (algorithmInfo.equals(REQUESTED_POSITION_INFO)) {
			insideMap = new HashMap<>();
			for (Map<ExecutionReportListener, String> insideMapIter : listenersManager.values()) {
				insideMap.putAll(insideMapIter);
			}
		}


		if (insideMap.size() > 0) {
			for (ExecutionReportListener executionReportListener : insideMap.keySet()) {
				executionReportListener.onInfoUpdate(header, message);
			}
		}
	}

	@Override public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
								   TypeMessage typeMessage, String content) {
		//ER read

		if (typeMessage.equals(TypeMessage.execution_report)) {
			ExecutionReport executionReport = fromJsonString(content, ExecutionReport.class);
			notifyExecutionReport(executionReport);
		}
		if (typeMessage.equals(TypeMessage.info)) {
			String header = REQUESTED_POSITION_INFO;
			if (configuration instanceof ZeroMqConfiguration) {
				ZeroMqConfiguration config = (ZeroMqConfiguration) configuration;
				header = config.getTopic();
			}
			notifyInfo(header, content);
		}

	}

	public boolean isPaperTrading() {
		return isPaperTrading;
	}

	private void initPaperTrading() {
		paperTradingEngine.setInstrumentsList(this.instrumentList);
		paperTradingEngine.init();

		PaperMarketDataListener paperMarketDataListener = new PaperMarketDataListener();
		this.paperTradingEngine.getMarketDataProviderIn().register(paperMarketDataListener);
	}

	public void setInstrumentList(List<Instrument> instrumentList) {
		this.instrumentList = instrumentList;
		if (paperTradingEngine != null) {
			initPaperTrading();
		}
	}

	public void setPaperTrading(MarketDataProvider marketDataProvider) {
		System.out.println("#### PAPER TRADING " + this.name);
		logger.info("starting ZeroMqTradingEngine as paper trading");
		PaperTradingEngineConfiguration paperTradingEngineConfiguration = new PaperTradingEngineConfiguration();

		//create instance
		if (marketDataProvider instanceof ZeroMqMarketDataConnector) {
			ZeroMqMarketDataConnector zeroMqMarketDataConnector = (ZeroMqMarketDataConnector) marketDataProvider;

			paperTradingEngine = new PaperTradingEngine(this, marketDataProvider, zeroMqExecutionReportProvider,
					zeroMqConfigurationOrderRequest);

			//Connector configuration paper
			OrdinaryConnectorConfiguration ordinaryConnectorConfiguration = new OrdinaryConnectorConfiguration();

			PaperConnectorPublisher paperConnectorPublisher = new PaperConnectorPublisher(
					ordinaryConnectorConfiguration, this.zeroMqPublisher);
			paperTradingEngine.setPaperConnectorMarketDataAndExecutionReportPublisher(paperConnectorPublisher);

			//override this onUpdate
		} else if (marketDataProvider instanceof OrdinaryMarketDataProvider) {
			OrdinaryMarketDataProvider ordinaryMarketDataProvider = (OrdinaryMarketDataProvider) marketDataProvider;
			paperTradingEngine = new PaperTradingEngine(this, marketDataProvider, zeroMqExecutionReportProvider,
					zeroMqConfigurationOrderRequest);
			PaperConnectorPublisher paperConnectorPublisher = new PaperConnectorPublisher(
					ordinaryMarketDataProvider.getConnectorConfiguration(), this.zeroMqPublisher);
			paperTradingEngine.setPaperConnectorMarketDataAndExecutionReportPublisher(paperConnectorPublisher);

		} else {
			logger.error(
					"cant be paper trading on other type of MarketDataProvider as ZeroMqMarketDataConnector or OrdinaryMarketDataProvider");
		}
		this.isPaperTrading = true;

		if (this.instrumentList != null) {
			initPaperTrading();
		}

	}

	private class PaperMarketDataListener implements MarketDataListener {

		@Override public boolean onDepthUpdate(Depth depth) {
			return true;
		}

		@Override public boolean onTradeUpdate(Trade trade) {
			return true;
		}

		@Override public boolean onCommandUpdate(Command command) {
			return true;
		}

		@Override public boolean onInfoUpdate(String header, String message) {
			notifyInfo(header, message);
			return true;
		}
	}
}
