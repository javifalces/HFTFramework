package com.lambda.investing.market_data_connector;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import com.lambda.investing.market_data_connector.mock.MockMarketDataConfiguration;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.messaging.TopicUtils;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.ExecutionReport;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter public class ZeroMqMarketDataConnector extends AbstractMarketDataProvider implements ConnectorListener {

	Logger logger = LogManager.getLogger(ZeroMqMarketDataConnector.class);
	private ZeroMqConfiguration zeroMqConfiguration;
	private ZeroMqProvider zeroMqProvider;

	private List<String> instrumentPksList;
	private boolean listenER = true;
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
		logger.info("Listening MarketData {}   in tcp://{}:{}", this.zeroMqConfiguration.getTopic(),
				this.zeroMqConfiguration.getHost(), this.zeroMqConfiguration.getPort());
	}

	public void setListenER(boolean listenER) {
		this.listenER = listenER;
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

			logger.info("Listening {}   in tcp://{}:{}", zeroMqConfiguration.getTopic(), zeroMqConfiguration.getHost(),
					zeroMqConfiguration.getPort());
		}

	}

	public void start() {
		this.statisticsReceived = null;
		zeroMqProvider.start(true, true);
	}

	///////////////////// Constructors ////////////////////

	@Override public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
			TypeMessage typeMessage, String content) {
		//
		ZeroMqConfiguration zeroMqConfigurationReceived = (ZeroMqConfiguration) configuration;
		String topicReceived = zeroMqConfigurationReceived.getTopic();

		if (statisticsReceived != null)
			statisticsReceived.addStatistics(topicReceived);

		if (typeMessage == TypeMessage.depth) {
			//DEPTH received
			Depth depth = GSON.fromJson(content, Depth.class);
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
			Trade trade = GSON.fromJson(content, Trade.class);
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
			Command command = GSON.fromJson(content, Command.class);
			notifyCommand(command);
		}

		//// ER should come from the trading engine!
		//		when trading is coming here
		if (typeMessage == TypeMessage.execution_report && listenER) {
			//ExecutionReport received
			ExecutionReport executionReport = GSON.fromJson(content, ExecutionReport.class);
			notifyExecutionReport(executionReport);
		}

	}
}
