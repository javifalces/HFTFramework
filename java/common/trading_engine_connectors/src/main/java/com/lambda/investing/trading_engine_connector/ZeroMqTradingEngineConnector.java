package com.lambda.investing.trading_engine_connector;

import com.lambda.investing.Configuration;
import com.lambda.investing.LatencyStatistics;
import com.lambda.investing.Statistics;
import com.lambda.investing.connector.AbstractConnectorPublisherConfiguration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqProviderFactory;
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
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.lambda.investing.Configuration.logger;
import static com.lambda.investing.model.Util.fromJsonString;
import static com.lambda.investing.model.Util.toJsonString;
import static com.lambda.investing.model.portfolio.Portfolio.REQUESTED_POSITION_INFO;


public class ZeroMqTradingEngineConnector extends AbstractTradingEngineConnector {

	@Getter
	private ZeroMqConfiguration zeroMqConfigurationExecutionReportListening, zeroMqConfigurationOrderRequest;
	private ZeroMqProvider zeroMqExecutionReportProvider;
	private ZeroMqPublisher zeroMqPublisher;

	/**
	 * HWM applied to the ER/order-request ZeroMQ channels when they still carry the low,
	 * market-data-oriented default (1). Unlike a stale depth tick, a dropped ExecutionReport
	 * can desync {@code QuoteSideManager}'s order-state machine forever, so these trading
	 * channels must never silently drop a message under ZMQ backpressure.
	 */
	private static final int TRADING_CHANNEL_HWM = 1000;

	private static void bumpHwmIfStillDefault(ZeroMqConfiguration configuration) {
		if (configuration.getHwm() <= 1) {
			configuration.setHwm(TRADING_CHANNEL_HWM);
		}
	}

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
		super(name);

		this.zeroMqConfigurationExecutionReportListening = zeroMqConfigurationExecutionReportListening;
		// Must happen BEFORE ZeroMqProviderFactory.create(): the SUB socket's HWM is set at
		// socket-creation time, so mutating the configuration after this point has no effect.
		bumpHwmIfStillDefault(this.zeroMqConfigurationExecutionReportListening);
		//listen the answers here
		zeroMqExecutionReportProvider = ZeroMqProviderFactory
				.create(this.zeroMqConfigurationExecutionReportListening, threadsListen);
		zeroMqExecutionReportProvider.register(this.zeroMqConfigurationExecutionReportListening, this);
		logger.info("Listening ExecutionReports on topic {}   in {}",
				zeroMqConfigurationExecutionReportListening.getTopic(),
				zeroMqConfigurationExecutionReportListening.getUrl());

		//publish the request here
		this.zeroMqConfigurationOrderRequest = zeroMqConfigurationOrderRequest;
		// ZeroMqPublisher's PUB socket is created lazily on first publish(), so this still
		// takes effect even though the publisher object below is already constructed.
		bumpHwmIfStillDefault(this.zeroMqConfigurationOrderRequest);
		this.zeroMqPublisher = new ZeroMqPublisher(name, threadsPublish);
		this.zeroMqPublisher.setServer(false);

		logger.info("Publishing OrderRequests on topic {}   in {}", this.zeroMqConfigurationOrderRequest.getTopic(),
				this.zeroMqConfigurationOrderRequest.getUrl());

		this.zeroMqPublisher
				.publish(this.zeroMqConfigurationOrderRequest, TypeMessage.command, TypeMessage.command.toString(), "starting publishing");

		//portfolio file not on the broker side
		//		portfolio = Portfolio.getPortfolio(Configuration.OUTPUT_PATH + File.separator + name + "_position.json");

	}

	public void start() {
		zeroMqExecutionReportProvider.start(true, true);
	}

	@Override
	public void register(String algorithmInfo, ExecutionReportListener executionReportListener) {
		Map<ExecutionReportListener, String> insideMap = listenersManager
				.getOrDefault(algorithmInfo, new ConcurrentHashMap<>());
		int previousSize = insideMap.size();
		insideMap.put(executionReportListener, "");
		listenersManager.put(algorithmInfo, insideMap);
		if (insideMap.size() > 1) {
			logger.warn("MULTIPLE LISTENERS registered for algorithmInfo={} count={} (was {})",
					algorithmInfo, insideMap.size(), previousSize);
		}
	}

	@Override public boolean orderRequest(OrderRequest orderRequest) {
		if (isPaperTrading && paperTradingEngine != null) {
			return this.paperTradingEngine.orderRequest(orderRequest);
		} else {
			String topic = TopicUtils.getTopic(orderRequest.getInstrument(), TypeMessage.order_request);
//			String message = toJsonString(orderRequest);
			this.zeroMqPublisher
					.publish(this.zeroMqConfigurationOrderRequest, TypeMessage.order_request, topic, orderRequest);
//			logger.info("ZeroMQ order request -> {}", orderRequest);
			return true;
		}
	}


	@Override
	public void requestInfo(String info) {
		if (isPaperTrading && paperTradingEngine != null) {
			this.paperTradingEngine.requestInfo(info);//simulate portfolio and position
		} else {
			this.zeroMqPublisher.publish(this.zeroMqConfigurationOrderRequest, TypeMessage.info, TypeMessage.info.toString(), info);
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
			paperTradingEngine.setPaperTrading(true);
			paperTradingEngine.setBacktest(false);

			//Connector configuration paper
			AbstractConnectorPublisherConfiguration ordinaryConnectorConfiguration = new OrdinaryConnectorConfiguration();

			PaperConnectorPublisher paperConnectorPublisher = new PaperConnectorPublisher(
					ordinaryConnectorConfiguration, this.zeroMqPublisher);
			paperTradingEngine.setPaperConnectorMarketDataAndExecutionReportPublisher(paperConnectorPublisher);

			//override this onUpdate
		} else if (marketDataProvider instanceof OrdinaryMarketDataProvider) {
			OrdinaryMarketDataProvider ordinaryMarketDataProvider = (OrdinaryMarketDataProvider) marketDataProvider;
			paperTradingEngine = new PaperTradingEngine(this, marketDataProvider, zeroMqExecutionReportProvider,
					zeroMqConfigurationOrderRequest);
			paperTradingEngine.setPaperTrading(true);
			paperTradingEngine.setBacktest(false);

			PaperConnectorPublisher paperConnectorPublisher = new PaperConnectorPublisher(
					ordinaryMarketDataProvider.getConnectorConfiguration(), this.zeroMqPublisher);
			paperTradingEngine.setPaperConnectorMarketDataAndExecutionReportPublisher(paperConnectorPublisher);

		} else {
			logger.error(
					"can't be paper trading on other type of MarketDataProvider as ZeroMqMarketDataConnector or OrdinaryMarketDataProvider");
		}
		this.isPaperTrading = true;

		if (this.instrumentList != null) {
			initPaperTrading();
		}

	}


}
