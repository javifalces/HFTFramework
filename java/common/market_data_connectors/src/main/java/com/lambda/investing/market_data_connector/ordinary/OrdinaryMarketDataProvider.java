package com.lambda.investing.market_data_connector.ordinary;


import com.lambda.investing.connector.AbstractConnectorPublisherProvider;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorPublisherProvider;
import com.lambda.investing.market_data_connector.AbstractMarketDataProvider;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.messaging.TypeMessage;
import lombok.Getter;
import lombok.Setter;

import static com.lambda.investing.model.Util.fromJsonString;
import static com.lambda.investing.model.Util.fromObject;


@Getter @Setter public class OrdinaryMarketDataProvider extends AbstractMarketDataProvider
		implements ConnectorListener {

	AbstractConnectorPublisherProvider ordinaryConnectorPublisherProvider;
	ConnectorConfiguration connectorConfiguration;

	public OrdinaryMarketDataProvider(AbstractConnectorPublisherProvider ordinaryConnectorPublisherProvider,
									  ConnectorConfiguration connectorConfiguration) {
		super();
		this.ordinaryConnectorPublisherProvider = ordinaryConnectorPublisherProvider;
		this.connectorConfiguration = connectorConfiguration;
	}

	public void init() {

		this.ordinaryConnectorPublisherProvider.register(connectorConfiguration, this);

	}

	@Override public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
								   TypeMessage typeMessage, Object content) {

		if (typeMessage.equals(TypeMessage.depth)) {
			//content is the producer's own (possibly pooled) Depth: when OrdinaryConnectorPublisherProvider
			//publishes asynchronously (publishThreads!=0), AbstractMarketDataConnectorPublisher defers its
			//delete()/checkIn to us (see its constructor) precisely to avoid the producer's object being reset
			//and reused for a new update while this task is still reading it here. Copy its data out into an
			//independent pooled Depth first, then it's safe to return the producer's original to the pool.
			Depth originalDepth = fromObject(content, Depth.class);
			Depth depth = Depth.copyFrom(originalDepth);//new copy from pool
			depth.setLevelsFromData();
            depth.setTimestampAlgoConnector(timestampReceived);
			notifyDepth(depth);//already returns depth to the pool internally, don't delete it again here
			originalDepth.delete();//now safe: its data has already been copied out
		} else if (typeMessage.equals(TypeMessage.trade)) {
			Trade originalTrade = fromObject(content, Trade.class);
			Trade trade = Trade.copyFrom(originalTrade);
            trade.setTimestampAlgoConnector(timestampReceived);
			notifyTrade(trade);//already returns trade to the pool internally, don't delete it again here
			originalTrade.delete();//now safe: its data has already been copied out
		} else if (typeMessage.equals(TypeMessage.command)) {
			Command command = fromJsonString((String) content, Command.class);
			notifyCommand(command);
			//All is set => start backtest
		}

	}
}
