package com.lambda.investing.market_data_connector.ordinary;


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

	OrdinaryConnectorPublisherProvider ordinaryConnectorPublisherProvider;
	OrdinaryConnectorConfiguration connectorConfiguration;

	public OrdinaryMarketDataProvider(OrdinaryConnectorPublisherProvider ordinaryConnectorPublisherProvider,
			OrdinaryConnectorConfiguration connectorConfiguration) {
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
			Depth depth = Depth.copyFrom(fromObject(content, Depth.class));//new copy from pool
			depth.setLevelsFromData();
            depth.setTimestampAlgoConnector(System.currentTimeMillis());
			notifyDepth(depth);
			depth.delete();//delete from pool
		} else if (typeMessage.equals(TypeMessage.trade)) {
			Trade trade = Trade.copyFrom(fromObject(content, Trade.class));
            trade.setTimestampAlgoConnector(System.currentTimeMillis());
			notifyTrade(trade);
			trade.delete();
		} else if (typeMessage.equals(TypeMessage.command)) {
			Command command = fromJsonString((String) content, Command.class);
			notifyCommand(command);
			//All is set => start backtest
		}

	}
}
