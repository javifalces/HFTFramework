package com.lambda.investing.trading_engine_connector;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.market_data_connector.ZeroMqMarketDataConnector;
import com.lambda.investing.market_data_connector.ordinary.OrdinaryMarketDataProvider;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.portfolio.Portfolio;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.OrderRequestAction;
import com.lambda.investing.trading_engine_connector.paper.PaperConnectorPublisher;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngineConfiguration;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import static com.lambda.investing.model.portfolio.Portfolio.REQUESTED_PORTFOLIO_INFO;
import static com.lambda.investing.trading_engine_connector.ZeroMqTradingEngineConnector.GSON;

public abstract class AbstractBrokerTradingEngine implements TradingEngineConnector, ConnectorListener {

	protected static int QUEUE_SIZE = 300;
	protected static String REJECT_ORIG_NOT_FOUND_FORMAT = "origClientOrderId %s not found for %s in %s";//origClientOrderId , action,instrument
	protected Logger logger = LogManager.getLogger(AbstractBrokerTradingEngine.class);
	protected ConnectorProvider orderRequestConnectorProvider;
	protected ConnectorConfiguration orderRequestConnectorConfiguration;

	protected ConnectorPublisher executionReportConnectorPublisher;
	protected ConnectorConfiguration executionReportConnectorConfiguration;

	protected Map<String, Map<ExecutionReportListener, String>> listenersManager;

	private boolean isPaperTrading = false;
	private boolean isDemoTrading = false;
	private PaperTradingEngine paperTradingEngine = null;
	protected Portfolio portfolio;
	protected Queue<String> lastOrderRequestClOrdId;

	public PaperTradingEngine getPaperTradingEngine() {
		return paperTradingEngine;
	}

	public AbstractBrokerTradingEngine(ConnectorConfiguration orderRequestConnectorConfiguration,
			ConnectorProvider orderRequestConnectorProvider,
			ConnectorConfiguration executionReportConnectorConfiguration,
			ConnectorPublisher executionReportConnectorPublisher) {
		this.orderRequestConnectorConfiguration = orderRequestConnectorConfiguration;
		this.orderRequestConnectorProvider = orderRequestConnectorProvider;
		this.executionReportConnectorConfiguration = executionReportConnectorConfiguration;
		this.executionReportConnectorPublisher = executionReportConnectorPublisher;
		portfolio = new Portfolio();//from file
		listenersManager = new ConcurrentHashMap<>();
		lastOrderRequestClOrdId = EvictingQueue.create(QUEUE_SIZE);
	}

	public void setInstrumentList(List<Instrument> instrumentList) {
		this.paperTradingEngine.setInstrumentsList(instrumentList);
		this.paperTradingEngine.init();
	}

	public abstract void setDemoTrading();

	public void setPaperTrading(MarketDataProvider marketDataProvider) {
		logger.info("starting BrokerTrading engine as paper trading");
		PaperTradingEngineConfiguration paperTradingEngineConfiguration = new PaperTradingEngineConfiguration();
		//create instance
		if (marketDataProvider instanceof ZeroMqMarketDataConnector) {
			ZeroMqMarketDataConnector zeroMqMarketDataConnector = (ZeroMqMarketDataConnector) marketDataProvider;

			this.paperTradingEngine = new PaperTradingEngine(this, marketDataProvider, orderRequestConnectorProvider,
					orderRequestConnectorConfiguration);

			//Connector configuration paper
			OrdinaryConnectorConfiguration ordinaryConnectorConfiguration = new OrdinaryConnectorConfiguration();

			PaperConnectorPublisher paperConnectorPublisher = new PaperConnectorPublisher(
					ordinaryConnectorConfiguration, this.executionReportConnectorPublisher);
			this.paperTradingEngine.setPaperConnectorMarketDataAndExecutionReportPublisher(paperConnectorPublisher);
			//			this.paperTradingEngine.init();//TODO enable it?

			//override this onUpdate
		} else if (marketDataProvider instanceof OrdinaryMarketDataProvider) {
			OrdinaryMarketDataProvider ordinaryMarketDataProvider = (OrdinaryMarketDataProvider) marketDataProvider;
			this.paperTradingEngine = new PaperTradingEngine(this, marketDataProvider, orderRequestConnectorProvider,
					orderRequestConnectorConfiguration);
			PaperConnectorPublisher paperConnectorPublisher = new PaperConnectorPublisher(
					ordinaryMarketDataProvider.getConnectorConfiguration(), this.executionReportConnectorPublisher);
			this.paperTradingEngine.setPaperConnectorMarketDataAndExecutionReportPublisher(paperConnectorPublisher);
			this.paperTradingEngine.init();

		} else {
			logger.error(
					"cant be paper trading on other type of MarketDataProvider as ZeroMqMarketDataConnector or OrdinaryMarketDataProvider");
		}
		this.isPaperTrading = true;
		this.portfolio.clear();//clean on startup

	}

	public void start() {
		//listening orderRequest
		this.orderRequestConnectorProvider.register(this.orderRequestConnectorConfiguration, this);

	}

	@Override public void register(String algorithmInfo, ExecutionReportListener executionReportListener) {
		//no sense on broker that are going to send the ER to connector publisher
	}

	@Override public void deregister(String id, ExecutionReportListener executionReportListener) {
		//no sense on broker that are going to send the ER to connector publisher
	}

	protected ExecutionReport createRejectionExecutionReport(OrderRequest orderRequest, String reason) {
		ExecutionReport executionReport = new ExecutionReport(orderRequest);
		if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {
			executionReport.setExecutionReportStatus(ExecutionReportStatus.CancelRejected);
		} else {

			executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
		}
		executionReport.setRejectReason(reason);
		return executionReport;
	}

	//called by extension when filled /partial filled
	protected void notifyExecutionReportById(ExecutionReport executionReport) {
		String id = executionReport.getAlgorithmInfo() + "." + TypeMessage.execution_report;
		this.executionReportConnectorPublisher
				.publish(executionReportConnectorConfiguration, TypeMessage.execution_report, id,
						GSON.toJson(executionReport));

	}

	//receiving OrderRequest
	@Override public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
			TypeMessage typeMessage, String content) {
		if (typeMessage.equals(TypeMessage.order_request)) {

			OrderRequest orderRequest = GSON.fromJson(content, OrderRequest.class);
			if (lastOrderRequestClOrdId.contains(orderRequest.getClientOrderId())) {
				//
				logger.warn("order already processed {}-> reject", orderRequest.getClientOrderId());
				return;
			} else {
				lastOrderRequestClOrdId.add(orderRequest.getClientOrderId());
			}

			if (isPaperTrading && paperTradingEngine != null) {
				//paper trading engine
				this.paperTradingEngine.orderRequest(orderRequest);
			} else {
				//directly
				orderRequest(orderRequest);
			}
		}

		if (typeMessage.equals(TypeMessage.execution_report)) {
			ExecutionReport executionReport = GSON.fromJson(content, ExecutionReport.class);

			//but here is for brokers only----> not so much sense
			portfolio.updateTrade(executionReport);

			//			if (isPaperTrading && paperTradingEngine != null) {
			//				this.paperTradingEngine.notifyExecutionReport(executionReport);
			//			}
		}

	}

	protected void notifyInfo(String topic, String message) {
		this.executionReportConnectorPublisher
				.publish(executionReportConnectorConfiguration, TypeMessage.info, topic, message);
	}

	@Override public void requestInfo(String info) {
		//algorithm.info
		if (info.endsWith(REQUESTED_PORTFOLIO_INFO)) {
			//return portfolio on execution Report
			notifyInfo(info, GSON.toJson(portfolio));
		}
	}
}
