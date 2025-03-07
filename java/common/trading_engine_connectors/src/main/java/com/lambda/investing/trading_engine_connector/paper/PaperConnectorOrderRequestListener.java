package com.lambda.investing.trading_engine_connector.paper;


import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.OrderRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Modifier;

import static com.lambda.investing.model.Util.fromJsonString;

public class PaperConnectorOrderRequestListener implements ConnectorListener {

	private Logger logger = LogManager.getLogger(PaperConnectorOrderRequestListener.class);

	private PaperTradingEngine paperTradingEngineConnector;
	private ConnectorProvider orderRequestConnectorProvider;
	private ConnectorConfiguration orderRequestConnectorConfiguration;

	public PaperConnectorOrderRequestListener(PaperTradingEngine paperTradingEngineConnector,
			ConnectorProvider orderRequestConnectorProvider,
			ConnectorConfiguration orderRequestConnectorConfiguration) {
		this.paperTradingEngineConnector = paperTradingEngineConnector;
		this.orderRequestConnectorProvider = orderRequestConnectorProvider;
		this.orderRequestConnectorConfiguration = orderRequestConnectorConfiguration;
	}

	public void start() {
		this.orderRequestConnectorProvider.register(this.orderRequestConnectorConfiguration, this);

	}

	@Override public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
			TypeMessage typeMessage, String content) {
		if (typeMessage.equals(TypeMessage.order_request)) {
			try {
                OrderRequest orderRequestParsed = fromJsonString(content, OrderRequest.class);
				this.paperTradingEngineConnector.orderRequest(orderRequestParsed);
			} catch (Exception ex) {
				logger.error(
						"Error onUpdate parsing PaperConnectorOrderRequestListener typeMessage received {} -> discard {}",
						typeMessage, content, ex);
			}
		} else {
			//			logger.error("Error onUpdate PaperConnectorOrderRequestListener typeMessage received {} -> discard {}",
			//					typeMessage, content);
		}

	}
}
