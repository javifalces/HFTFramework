package com.lambda.investing.trading_engine_connector.binance;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.domain.ExecutionType;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.CancelOrderResponse;
import com.binance.api.client.domain.event.OrderTradeUpdateEvent;
import com.binance.api.client.domain.event.UserDataUpdateEvent;
import com.lambda.investing.Configuration;
import com.lambda.investing.binance.BinanceBrokerConnector;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.portfolio.Portfolio;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.AbstractBrokerTradingEngine;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import com.lambda.investing.trading_engine_connector.TradingEngineConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BinanceBrokerTradingEngine extends AbstractBrokerTradingEngine
		implements BinanceApiCallback<UserDataUpdateEvent> {

	public static String NUMBER_TO_STRING_FORMAT = "%.5f";
	private static String CANCELED_STATUS_RECEIVED = "CANCELED";//"status": "CANCELED",

	private static String API_KEY_DEMO = "ZL1eQLxg9dEHpmJub1vlCxzyDHk0GL8IF2SEIS6Go9ie5cqo8z5dcZMjWEEIvGG4";
	private static String SECRET_KEY_DEMO = "ELiJbJ4Hdu3uZdKbw6skDrQNuVcGSz3dn6cj2QASyI0LNZyxAPT5BOMjC4z8KG6V";

	protected Logger logger = LogManager.getLogger(BinanceBrokerTradingEngine.class);
	private TradingEngineConfiguration tradingEngineConfiguration;
	private BinanceBrokerConnector binanceBrokerConnector;
	BinanceTradingEngineConfiguration binanceTradingEngineConfiguration;

	private Map<String, OrderRequest> clientOrderIdToOrderRequest;

	private Map<String, String> modificationCancelIdGenerated;

	protected Map<String, Map<ExecutionReportListener, String>> listenersManager;

	private boolean isDemo = false;

	public BinanceBrokerTradingEngine(ConnectorConfiguration orderRequestListenerConnectorConfiguration,
			ConnectorProvider orderRequestListenerConnectorProvider,
			ConnectorConfiguration executionReportConnectorConfiguration,
			ConnectorPublisher executionReportConnectorPublisher,
			TradingEngineConfiguration tradingEngineConfiguration) {

		super(orderRequestListenerConnectorConfiguration, orderRequestListenerConnectorProvider,
				executionReportConnectorConfiguration,
				executionReportConnectorPublisher);

		clientOrderIdToOrderRequest = new ConcurrentHashMap<>();
		modificationCancelIdGenerated = new ConcurrentHashMap<>();

		this.tradingEngineConfiguration = tradingEngineConfiguration;
		if (this.tradingEngineConfiguration instanceof BinanceTradingEngineConfiguration) {
			binanceTradingEngineConfiguration = (BinanceTradingEngineConfiguration) this.tradingEngineConfiguration;
			this.binanceBrokerConnector = BinanceBrokerConnector
					.getInstance(binanceTradingEngineConfiguration.getApiKey(),
							binanceTradingEngineConfiguration.getSecretKey());
		} else {
			logger.error("trying to BinanceBrokerTradingEngine with a not binanceTradingEngineConfiguration");
		}
		//portfolio file not on the broker side if we want to save it on file
		portfolio = Portfolio.getPortfolio(Configuration.OUTPUT_PATH + File.separator + "binance_broker_position.json");
	}

	@Override public void setDemoTrading() {
		isDemo = true;
		logger.info("setting binance DEMO account");

		this.binanceBrokerConnector = BinanceBrokerConnector.getInstance(API_KEY_DEMO, SECRET_KEY_DEMO);
		//		subscribe new User DEMO Data Event
		this.binanceBrokerConnector.getWebSocketClient()
				.onUserDataUpdateEvent(this.binanceBrokerConnector.getApiKey(), this);

	}

	@Override public void start() {
		super.start();
		this.binanceBrokerConnector.getWebSocketClient()
				.onUserDataUpdateEvent(binanceTradingEngineConfiguration.getApiKey(), this);
	}

	private OrderType getOrderType(OrderRequest orderRequest) {
		OrderType orderType = OrderType.LIMIT;
		if (orderRequest.getOrderType().equals(com.lambda.investing.model.trading.OrderType.Market)) {
			orderType = OrderType.MARKET;
		}
		if (orderRequest.getOrderType().equals(com.lambda.investing.model.trading.OrderType.Stop)) {
			orderType = OrderType.STOP_LOSS;
		}
		return orderType;
	}

	private TimeInForce getTimeInForce(OrderRequest orderRequest) {
		TimeInForce timeInForce = TimeInForce.GTC;
		return timeInForce;
	}

	@Override public boolean orderRequest(OrderRequest orderRequest) {
		try {
			//send new order
			if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Send)) {

				//			NewOrder(String symbol, OrderSide side, OrderType type, TimeInForce timeInForce, String quantity,String price)
				Instrument instrument = Instrument.getInstrument(orderRequest.getInstrument());
				String symbol = instrument.getSymbol().toLowerCase();
				OrderSide orderSide = orderRequest.getVerb().equals(Verb.Buy) ? OrderSide.BUY : OrderSide.SELL;
				OrderType orderType = getOrderType(orderRequest);
				TimeInForce timeInForce = getTimeInForce(orderRequest);
				String quantity = String.format(NUMBER_TO_STRING_FORMAT, orderRequest.getQuantity())
						.replaceAll(",", ".");
				String price = String.format(NUMBER_TO_STRING_FORMAT, orderRequest.getPrice()).replaceAll(",", ".");
				NewOrder newOrder = new NewOrder(symbol, orderSide, orderType, timeInForce, quantity, price);
				newOrder.newClientOrderId(orderRequest.getClientOrderId());//set my clientOrderId

				//			newOrder.quoteOrderQty(quantity);//research it
				clientOrderIdToOrderRequest.put(orderRequest.getClientOrderId(), orderRequest);
				NewOrderResponse response = this.binanceBrokerConnector.getRestClient().newOrder(newOrder);

				return true;
			}

			if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Modify)) {
				if (!clientOrderIdToOrderRequest.containsKey(orderRequest.getOrigClientOrderId())) {
					//not found
					String reason = String.format(REJECT_ORIG_NOT_FOUND_FORMAT, orderRequest.getOrigClientOrderId(),
							orderRequest.getOrderRequestAction(), orderRequest.getInstrument());
					notifyExecutionReportById(createRejectionExecutionReport(orderRequest, reason));
					return false;
				}

				//cancel the original order
				Instrument instrument = Instrument.getInstrument(orderRequest.getInstrument());
				CancelOrderRequest cancelOrderRequest = new CancelOrderRequest(instrument.getSymbol(),
						orderRequest.getOrigClientOrderId());
				String modifyCancelClientOrderId = cancelOrderRequest.getNewClientOrderId();
				cancelOrderRequest.newClientOrderId(modifyCancelClientOrderId);

				modificationCancelIdGenerated.put(modifyCancelClientOrderId, "");
				CancelOrderResponse cancelOrderResponse = this.binanceBrokerConnector.getRestClient()
						.cancelOrder(cancelOrderRequest);
				if (!cancelOrderResponse.getStatus().equalsIgnoreCase(CANCELED_STATUS_RECEIVED)) {
					logger.warn("can't cancel {} in {} status received {} ", cancelOrderRequest.getOrigClientOrderId(),
							orderRequest.getInstrument(), cancelOrderResponse.getStatus());
					return false;
				}

				//send the new order
				String symbol = instrument.getSymbol().toUpperCase();
				OrderSide orderSide = orderRequest.getVerb().equals(Verb.Buy) ? OrderSide.BUY : OrderSide.SELL;
				OrderType orderType = getOrderType(orderRequest);
				TimeInForce timeInForce = getTimeInForce(orderRequest);
				String quantity = String.format(NUMBER_TO_STRING_FORMAT, orderRequest.getQuantity())
						.replaceAll(",", ".");
				String price = String.format(NUMBER_TO_STRING_FORMAT, orderRequest.getPrice()).replaceAll(",", ".");
				NewOrder newOrder = new NewOrder(symbol, orderSide, orderType, timeInForce, quantity, price);
				newOrder.newClientOrderId(orderRequest.getClientOrderId());//set my clientOrderId

				clientOrderIdToOrderRequest.put(orderRequest.getClientOrderId(), orderRequest);
				NewOrderResponse response = this.binanceBrokerConnector.getRestClient().newOrder(newOrder);

				return true;
			}

			if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {

				if (!clientOrderIdToOrderRequest.containsKey(orderRequest.getOrigClientOrderId())) {
					//not found
					String reason = String.format(REJECT_ORIG_NOT_FOUND_FORMAT, orderRequest.getOrigClientOrderId(),
							orderRequest.getOrderRequestAction(), orderRequest.getInstrument());
					notifyExecutionReportById(createRejectionExecutionReport(orderRequest, reason));
					return false;
				}
				Instrument instrument = Instrument.getInstrument(orderRequest.getInstrument());
				CancelOrderRequest cancelOrderRequest = new CancelOrderRequest(instrument.getSymbol(),
						orderRequest.getOrigClientOrderId());
				cancelOrderRequest.newClientOrderId(orderRequest.getClientOrderId());
				clientOrderIdToOrderRequest.put(orderRequest.getClientOrderId(), orderRequest);
				CancelOrderResponse cancelOrderResponse = this.binanceBrokerConnector.getRestClient()
						.cancelOrder(cancelOrderRequest);

				return true;

			}
		} catch (Exception e) {
			ExecutionReport executionReport = new ExecutionReport(orderRequest);
			executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
			if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {
				executionReport.setExecutionReportStatus(ExecutionReportStatus.CancelRejected);
			}
			executionReport.setRejectReason(e.getMessage());
			logger.error("order rejected in binance", e);
			notifyExecutionReportById(executionReport);
		}
		return false;
	}

	@Override public void onResponse(UserDataUpdateEvent response) {
		if (response.getEventType().equals(UserDataUpdateEvent.UserDataUpdateEventType.ORDER_TRADE_UPDATE)) {
			//execution reports arrived here
			OrderTradeUpdateEvent orderTradeUpdateEvent = response.getOrderTradeUpdateEvent();
			OrderRequest orderRequestSent = clientOrderIdToOrderRequest
					.get(orderTradeUpdateEvent.getNewClientOrderId());

			if (orderRequestSent == null && modificationCancelIdGenerated
					.containsKey(orderTradeUpdateEvent.getNewClientOrderId())) {
				//cancel of the replace found don't do nothing
				modificationCancelIdGenerated.remove(orderTradeUpdateEvent.getNewClientOrderId());
				return;
			}

			if (orderRequestSent == null) {
				logger.error("received event of not found request  {}\n{}\n{}",
						orderTradeUpdateEvent.getNewClientOrderId(), orderTradeUpdateEvent, response);
				return;
			}
			ExecutionReport executionReport = new ExecutionReport(orderRequestSent);
			executionReport = setExecutionReportStatus(orderTradeUpdateEvent, orderRequestSent, executionReport);
			notifyExecutionReportById(executionReport);

		}
	}

	private ExecutionReport setExecutionReportStatus(OrderTradeUpdateEvent orderTradeUpdateEvent,
			OrderRequest orderRequestSent, ExecutionReport executionReport) {

		ExecutionReportStatus executionReportStatus = ExecutionReportStatus.Active;
		ExecutionType executionType = orderTradeUpdateEvent.getExecutionType();
		if (executionType.equals(ExecutionType.CANCELED)) {
			executionReportStatus = ExecutionReportStatus.Cancelled;
		}
		if (executionType.equals(ExecutionType.NEW)) {
			executionReportStatus = ExecutionReportStatus.Active;
		}
		if (executionType.equals(ExecutionType.REJECTED)) {
			executionReportStatus = ExecutionReportStatus.Rejected;

			if (orderRequestSent.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {
				executionReportStatus = ExecutionReportStatus.CancelRejected;
			}
			String reason = orderTradeUpdateEvent.getOrderRejectReason().toString();
			executionReport.setRejectReason(reason);
		}

		if (executionType.equals(ExecutionType.TRADE)) {
			try {
				double qtyFilled = BinanceBrokerConnector.NUMBER_FORMAT
						.parse(orderTradeUpdateEvent.getAccumulatedQuantity()).doubleValue();
				double lastQty = BinanceBrokerConnector.NUMBER_FORMAT
						.parse(orderTradeUpdateEvent.getQuantityLastFilledTrade()).doubleValue();

				executionReport.setLastQuantity(Math.abs(lastQty));
				executionReport.setQuantityFill(Math.abs(qtyFilled));

				if (Math.abs(qtyFilled) >= orderRequestSent.getQuantity()) {
					executionReportStatus = ExecutionReportStatus.CompletellyFilled;
				} else {
					executionReportStatus = ExecutionReportStatus.PartialFilled;
				}

			} catch (Exception e) {
				logger.error("qtyFilled cant be parsed {} -> mark as partial filled");
				executionReportStatus = ExecutionReportStatus.PartialFilled;
			}

		}
		if (executionType.equals(ExecutionType.REPLACED)) {
			executionReportStatus = ExecutionReportStatus.Active;
		}

		executionReport.setExecutionReportStatus(executionReportStatus);
		return executionReport;
	}

	@Override public void onFailure(Throwable cause) {

	}

	@Override public void requestInfo(String info) {
		super.requestInfo(info);
		//		//todo asking broker
		//		if (info.endsWith(REQUESTED_PORTFOLIO_INFO)) {
		//
		//		}
	}
}
