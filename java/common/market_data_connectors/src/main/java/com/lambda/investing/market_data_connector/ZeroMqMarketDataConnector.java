package com.lambda.investing.market_data_connector;


import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqProviderFactory;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.ExecutionReport;
import lombok.Getter;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import static com.lambda.investing.model.Util.fromJsonString;
import static com.lambda.investing.model.Util.fromObject;

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

	private static final Map<String, ZeroMqMarketDataConnector> INSTANCES = new ConcurrentHashMap<>();

	///////////////////// Factory methods ////////////////////

	/**
	 * Returns a cached or new instance for the given zeroMqConfiguration.
	 * Key is based on url + topic so configurations with different topics are treated as distinct.
	 */
	public static ZeroMqMarketDataConnector getInstance(ZeroMqConfiguration zeroMqConfiguration, int threadsListening) {
		String key = buildKey(zeroMqConfiguration);
		return INSTANCES.computeIfAbsent(key, k -> new ZeroMqMarketDataConnector(zeroMqConfiguration, threadsListening));
	}

	/**
	 * Returns a cached or new instance for the given zeroMqConfiguration and instruments list.
	 * Key is derived from host:port plus each instrument so the same combination is reused.
	 */
	public static ZeroMqMarketDataConnector getInstance(ZeroMqConfiguration zeroMqConfigurationIn,
	                                                    List<Instrument> instruments, int threadsListening) {
		String key = buildKey(zeroMqConfigurationIn, instruments);
		return INSTANCES.computeIfAbsent(key,
				k -> new ZeroMqMarketDataConnector(zeroMqConfigurationIn, instruments, threadsListening));
	}

	private static String buildKey(ZeroMqConfiguration cfg) {
		return cfg.getUrl() + "#" + cfg.getTopic();
	}

	private static String buildKey(ZeroMqConfiguration cfg, List<Instrument> instruments) {
		StringBuilder sb = new StringBuilder(cfg.getUrl());
		for (Instrument instrument : instruments) {
			sb.append("#").append(instrument.getPrimaryKey());
		}
		return sb.toString();
	}

	///////////////////// Constructors ////////////////////

	/**
	 * Listen to the market data on a zeroMqConfiguration style
	 *
	 * @param zeroMqConfiguration
	 */
	private ZeroMqMarketDataConnector(ZeroMqConfiguration zeroMqConfiguration, int threadsListening) {
		this.zeroMqConfiguration = new ZeroMqConfiguration(zeroMqConfiguration);
		zeroMqProvider = ZeroMqProviderFactory.create(this.zeroMqConfiguration, threadsListening);
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
			zeroMqProvider = ZeroMqProviderFactory.create(zeroMqConfiguration, threadsListening);
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
						 TypeMessage typeMessage, Object content) {
		processUpdate(configuration, timestampReceived, typeMessage, content);
	}

	/**
	 * Core business logic extracted so that subclasses (e.g. the Disruptor variant)
	 * can call it from a different thread without duplicating code.
	 */
	protected void processUpdate(ConnectorConfiguration configuration, long timestampReceived,
								 TypeMessage typeMessage, Object content) {
		ZeroMqConfiguration zeroMqConfigurationReceived = (ZeroMqConfiguration) configuration;
		String topicReceived = zeroMqConfigurationReceived.getTopic();

		if (content instanceof byte[]) {
			content = SerializationUtils.deserialize((byte[]) content);
		}

		if (typeMessage == TypeMessage.depth) {
			//DEPTH received
			Depth depth = fromObject(content, Depth.class);
			depth.setTimestampAlgoConnector(timestampReceived);

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
			Trade trade = fromObject(content, Trade.class);
			trade.setTimestampAlgoConnector(timestampReceived);
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
			Command command = fromJsonString((String) content, Command.class);
			notifyCommand(command);
		}

		if (statisticsReceived != null)
			statisticsReceived.addStatistics(topicReceived);

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

			case CompletelyFilled:
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
