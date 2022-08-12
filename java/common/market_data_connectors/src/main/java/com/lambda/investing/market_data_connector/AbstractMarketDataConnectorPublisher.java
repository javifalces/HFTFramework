package com.lambda.investing.market_data_connector;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.zero_mq.ZeroMqPublisher;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.lambda.investing.market_data_connector.AbstractMarketDataProvider.GSON;

public  abstract class AbstractMarketDataConnectorPublisher implements MarketDataConnectorPublisher {

	protected boolean enable=true;
	protected ConnectorConfiguration connectorConfiguration;
	protected ConnectorPublisher connectorPublisher;
	protected Statistics statistics;
	protected Logger logger = LogManager.getLogger(AbstractMarketDataConnectorPublisher.class);
	private boolean isZeroMq = false;
	protected String outputPath;
	private List<MarketDataConnectorPublisherListener> listenerList;

	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	public void setStatistics(Statistics statistics) {
		this.statistics = statistics;
	}

	public AbstractMarketDataConnectorPublisher(ConnectorConfiguration connectorConfiguration,ConnectorPublisher connectorPublisher) {
		//		this.statistics = new Statistics(headerStatistics,sleepStatistics);
		this.connectorConfiguration=connectorConfiguration;
		this.connectorPublisher=connectorPublisher;
		if (connectorPublisher instanceof ZeroMqPublisher) {
			isZeroMq = true;
		}
		listenerList = new ArrayList<>();
	}

	public AbstractMarketDataConnectorPublisher(String name, ConnectorConfiguration connectorConfiguration,
			ConnectorPublisher connectorPublisher) {
		//		this.statistics = new Statistics(name, sleepStatistics);
		this.connectorConfiguration = connectorConfiguration;
		this.connectorPublisher = connectorPublisher;
		if (connectorPublisher instanceof ZeroMqPublisher) {
			isZeroMq = true;
		}
		listenerList = new ArrayList<>();
	}

	public void register(MarketDataConnectorPublisherListener listener) {
		listenerList.add(listener);
	}

	public void notifyEndOfFile() {
		for (MarketDataConnectorPublisherListener listener : listenerList) {
			listener.notifyEndOfFile();
		}
	}

	public abstract void init();

	public ConnectorConfiguration getConnectorConfiguration() {
		return connectorConfiguration;
	}

	@Override public void start() {
		enable=true;
	}

	@Override public void stop() {
		enable=false;
	}



	@Override public void notifyDepth(String topic,Depth depth) {
		String depthJson =GSON.toJson(depth);
		topic=topic+"."+TypeMessage.depth.name();
		//		logger.debug("notify DEPTH {}",depth.toString());
		connectorPublisher.publish(connectorConfiguration, TypeMessage.depth, topic, depthJson);
		if (statistics != null)
			statistics.addStatistics(topic);
	}

	@Override public void notifyTrade(String topic,Trade trade) {
		String tradeJson =GSON.toJson(trade);
		topic=topic+"."+TypeMessage.trade.name();
		//		logger.debug("notify TRADE {}",trade.toString());
		connectorPublisher.publish(connectorConfiguration, TypeMessage.trade, topic, tradeJson);
		if (statistics != null)
			statistics.addStatistics(topic);
	}

	public synchronized void notifyCommand(String topic, Command command) {
		String commandJson = GSON.toJson(command);

		if (isZeroMq) {
			ZeroMqPublisher zeroMqPublisher = (ZeroMqPublisher) connectorPublisher;
			int beforeCounter = zeroMqPublisher.getOKReceived();
			while (zeroMqPublisher.getOKReceived() == beforeCounter) {
				connectorPublisher.publish(connectorConfiguration, TypeMessage.command, topic, commandJson);
				if (statistics != null)
					statistics.addStatistics(topic);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}

		} else {
			connectorPublisher.publish(connectorConfiguration, TypeMessage.command, topic, commandJson);
			if (statistics != null)
				statistics.addStatistics(topic);
		}

		if (statistics != null)
			statistics.addStatistics(topic);

	}
}
