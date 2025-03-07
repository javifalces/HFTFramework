package com.lambda.investing.market_data_connector;


import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.ExecutionReport;
import lombok.Getter;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static com.lambda.investing.model.Util.fromJsonString;

@Getter
public class ZeroMqMarketDataConnector extends AbstractMarketDataProvider implements ConnectorListener {
	private static int BUFFER_ER_ORDER_ID = 128;
	Logger logger = LogManager.getLogger(ZeroMqMarketDataConnector.class);
	private ZeroMqConfiguration zeroMqConfiguration;
	private ZeroMqProvider zeroMqProvider;

	private List<String> instrumentPksList;
	private boolean listenER = false;

	private Queue<String> ActiveOrderIdNotified = EvictingQueue.create(BUFFER_ER_ORDER_ID);
	private Queue<String> FinishedOrderIdNotified = EvictingQueue.create(BUFFER_ER_ORDER_ID);
	///////////////////// Constructors ////////////////////

	/**
	 * Listen to the market data on a zeroMqConfiguration style
	 *
	 * @param zeroMqConfiguration
	 */
	public ZeroMqMarketDataConnector(ZeroMqConfiguration zeroMqConfiguration, int threadsListening) {
		this.zeroMqConfiguration = new ZeroMqConfiguration(zeroMqConfiguration);
		zeroMqProvider = ZeroMqProvider.getInstance(this.zeroMqConfiguration, threadsListening);
		zeroMqProvider.register(this.zeroMqConfiguration, this);
		logger.info("Listening MarketData {}   in {}", this.zeroMqConfiguration.getTopic(), this.zeroMqConfiguration.getUrl());
	}

	public void setListenER(boolean listenER) {
		this.listenER = listenER;
		if (this.listenER) {
			System.out.println("DEPRECATED setListenER is true!please change it! deprecated!!! ER should be on tradingEngine!!!");
		}
	}

	public void setInstrumentPksList(List<String> instrumentPksList) {
		if (instrumentPksList.size() == 0) {
			logger.warn("setting zero size instrumentPksList!! -> filtering all");
		}
		this.instrumentPksList = instrumentPksList;
	}

	//	public ZeroMqMarketDataConnector(ZeroMqConfiguration zeroMqConfigurationIn, Instrument instrument,
	//			int threadsListening) {
	//		List<ZeroMqConfiguration> zeroMqConfigurationList = ZeroMqConfiguration
	//				.getMarketDataZeroMqConfiguration(zeroMqConfigurationIn.getHost(), zeroMqConfigurationIn.getPort(),
	//						instrument);
	//
	//		for (ZeroMqConfiguration zeroMqConfiguration : zeroMqConfigurationList) {
	//			zeroMqProvider = ZeroMqProvider.getInstance(zeroMqConfiguration, threadsListening);
	//			zeroMqProvider.register(zeroMqConfiguration, this);
	//
	//			logger.info("Listening {}   in tcp://{}:{}", zeroMqConfiguration.getTopic(), zeroMqConfiguration.getHost(),
	//					zeroMqConfiguration.getPort());
	//		}

	//	}

	public ZeroMqMarketDataConnector(ZeroMqConfiguration zeroMqConfigurationIn, List<Instrument> instruments,
									 int threadsListening) {
		List<ZeroMqConfiguration> zeroMqConfigurationList = new ArrayList<>();
		for (Instrument instrument : instruments) {
			List<ZeroMqConfiguration> zeroMqConfigurationListTemp = ZeroMqConfiguration
					.getMarketDataZeroMqConfiguration(zeroMqConfigurationIn.getHost(), zeroMqConfigurationIn.getPort(),
							instrument);
			zeroMqConfigurationList.addAll(zeroMqConfigurationListTemp);
		}

		for (ZeroMqConfiguration zeroMqConfiguration : zeroMqConfigurationList) {
			zeroMqProvider = ZeroMqProvider.getInstance(zeroMqConfiguration, threadsListening);
			zeroMqProvider.register(zeroMqConfiguration, this);

			logger.info("Listening {}   in {}}", zeroMqConfiguration.getTopic(), zeroMqConfiguration.getUrl());
		}

	}

	public void start() {
		this.statisticsReceived = null;
		zeroMqProvider.start(true, true);
	}

	///////////////////// Constructors ////////////////////

	@Override
	public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
						 TypeMessage typeMessage, String content) {
		//
		ZeroMqConfiguration zeroMqConfigurationReceived = (ZeroMqConfiguration) configuration;
		String topicReceived = zeroMqConfigurationReceived.getTopic();

		if (statisticsReceived != null)
			statisticsReceived.addStatistics(topicReceived);

		if (typeMessage == TypeMessage.depth) {
			//DEPTH received
			Depth depth = fromJsonString(content, Depth.class);
			if (instrumentPksList != null) {
				//filtering
				if (!instrumentPksList.contains(depth.getInstrument())) {
					return;
				}
			}
			depth.setLevelsFromData();
			notifyDepth(depth);

		}

		if (typeMessage == TypeMessage.trade) {
			//TRADE received
			Trade trade = fromJsonString(content, Trade.class);
			if (instrumentPksList != null) {
				//filtering
				if (!instrumentPksList.contains(trade.getInstrument())) {
					return;
				}
			}
			notifyTrade(trade);
		}

		if (typeMessage == TypeMessage.command) {
			//Command received
			Command command = fromJsonString(content, Command.class);
			notifyCommand(command);
		}

		//// ER should come from the trading engine!
		//		when trading is coming here
//		if (typeMessage == TypeMessage.execution_report && listenER) {
//			//ExecutionReport received
//			ExecutionReport executionReport = fromJsonString(content, ExecutionReport.class);
//			logger.warn("ER received on ZeroMqMarketDataConnector -> please do another thing!! {}",executionReport.getClientOrderId());
//
//			notifyExecutionReport(executionReport);
//		}

	}

	private boolean IsExecutionReportAlreadyNotified(ExecutionReport executionReport) {
		String clordId = executionReport.getClientOrderId();
		switch (executionReport.getExecutionReportStatus()) {
			case Active:
				if (ActiveOrderIdNotified.contains(clordId)) {
					return true;
				} else {
					ActiveOrderIdNotified.offer(clordId);
					return false;
				}

			case CompletellyFilled:
				if (FinishedOrderIdNotified.contains(clordId)) {
					return true;
				} else {
					FinishedOrderIdNotified.offer(clordId);
					return false;
				}
			default:
				return false;
		}
	}
}
