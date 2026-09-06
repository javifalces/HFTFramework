package com.lambda.investing.trading_engine_connector.xchange;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.AbstractBrokerTradingEngine;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import com.lambda.investing.trading_engine_connector.TradingEngineConfiguration;
import com.lambda.investing.xchange.*;
import info.bitrich.xchangestream.core.StreamingExchange;
import io.reactivex.rxjava3.disposables.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;

import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.service.trade.TradeService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//https://github.com/knowm/XChange/blob/9198c3fb06151e680a3e93cade5aacbcb17d1742/xchange-examples/src/main/java/org/knowm/xchange/examples/bitstamp/trade/BitstampTradeDemo.java
public class XChangeTradingEngine extends AbstractBrokerTradingEngine {

	protected Logger logger = LogManager.getLogger(XChangeTradingEngine.class);
	protected TradingEngineConfiguration tradingEngineConfiguration;
	protected XChangeBrokerConnector brokerConnector;
	protected TradeService tradeService;

	private Map<String, OrderRequest> marketOrderIdToOrderRequest;///todo clean it
	private Map<String, ExecutionReport> marketOrderIdToER;///todo clean it

	private Map<String, String> clOrdIdToMarketOrderId;

	private Map<String, String> modificationCancelIdGenerated;

	protected Map<String, Map<ExecutionReportListener, String>> listenersManager;

	private boolean isDemo = false;

	protected StreamingExchange webSocketClient;

	protected List<Disposable> subscriptionTrades = new ArrayList<>();
	protected List<Disposable> subscriptionOrderChanges = new ArrayList<>();
	protected Set<Instrument> instrumentSet;

	public XChangeTradingEngine(ConnectorConfiguration orderRequestConnectorConfiguration,
			ConnectorProvider orderRequestConnectorProvider,
			ConnectorConfiguration executionReportConnectorConfiguration,
			ConnectorPublisher executionReportConnectorPublisher, TradingEngineConfiguration tradingEngineConfiguration,
			                    Set<Instrument> instrumentSet) {
		super(orderRequestConnectorConfiguration, orderRequestConnectorProvider, executionReportConnectorConfiguration,
				executionReportConnectorPublisher);
		this.tradingEngineConfiguration = tradingEngineConfiguration;
		setBrokerConnector();

		marketOrderIdToOrderRequest = new ConcurrentHashMap<>();
		clOrdIdToMarketOrderId = new ConcurrentHashMap<>();
		marketOrderIdToER = new ConcurrentHashMap<>();

		modificationCancelIdGenerated = new ConcurrentHashMap<>();
		listenersManager = new HashMap<>();

		this.instrumentSet = instrumentSet;

	}

	@Override public void start() {
		super.start();
		this.brokerConnector.connectWebsocket(instrumentSet);
		awaitAuthenticatedConnection();
		subscribeER();
	}

	/**
	 * Number of attempts and delay between attempts when subscribing to the authenticated
	 * user-trade/order-change streams right after connecting. Binance's user-data-stream
	 * authentication (ed25519 login) completes asynchronously right after the websocket connects,
	 * so subscribing immediately can transiently fail with "Not authenticated" even though the
	 * connection is otherwise healthy; a short retry avoids leaving every pair unsubscribed.
	 */
	private static final int SUBSCRIBE_ER_MAX_ATTEMPTS = 5;
	private static final long SUBSCRIBE_ER_RETRY_DELAY_MS = 1000L;

	/**
	 * How long to poll {@link XChangeBrokerConnector#isConnectionAlive()} before subscribing.
	 */
	private static final long AWAIT_CONNECTION_TIMEOUT_MS = 5000L;
	private static final long AWAIT_CONNECTION_POLL_MS = 250L;

	/**
	 * Waits for the (authenticated) connection to come up before subscribing to user-trade/order
	 * streams. If it never reports alive within the timeout, forces one full reconnect via
	 * {@link XChangeBrokerConnector#resetClient()}: the shared/singleton connector's very first
	 * connect can race with the underlying login handshake, leaving the connection permanently
	 * unauthenticated; a clean reconnect works around that instead of retrying the same broken
	 * socket in {@link #subscribeER()}.
	 */
	private void awaitAuthenticatedConnection() {
		if (brokerConnector == null) {
			return;
		}
		long deadline = System.currentTimeMillis() + AWAIT_CONNECTION_TIMEOUT_MS;
		while (!brokerConnector.isConnectionAlive() && System.currentTimeMillis() < deadline) {
			try {
				Thread.sleep(AWAIT_CONNECTION_POLL_MS);
			} catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		if (!brokerConnector.isConnectionAlive()) {
			logger.warn("connection not alive after {} ms, forcing a reconnect before subscribing", AWAIT_CONNECTION_TIMEOUT_MS);
			brokerConnector.resetClient();
			deadline = System.currentTimeMillis() + AWAIT_CONNECTION_TIMEOUT_MS;
			while (!brokerConnector.isConnectionAlive() && System.currentTimeMillis() < deadline) {
				try {
					Thread.sleep(AWAIT_CONNECTION_POLL_MS);
				} catch (InterruptedException interruptedException) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	protected void subscribeER() {
		if (this.brokerConnector != null) {
			this.webSocketClient = this.brokerConnector.getWebSocketClient();
		}

		for (CurrencyPair currencyPair : brokerConnector.getPairs()) {
			Instrument instrument = brokerConnector.getCurrencyPairToInstrument()
					.get(currencyPair);
			subscribeUserTrades(currencyPair, instrument);
			subscribeOrderChanges(currencyPair, instrument);
		}
	}

	private void subscribeUserTrades(CurrencyPair currencyPair, Instrument instrument) {
		for (int attempt = 1; attempt <= SUBSCRIBE_ER_MAX_ATTEMPTS; attempt++) {
			try {
				Disposable subscriptionTrade = webSocketClient.getStreamingTradeService().getUserTrades(currencyPair)
						.subscribe(userTrade -> onUserTrades(instrument, userTrade),
								throwable -> logger.error("Error in onUserTrades subscription", throwable));

				subscriptionTrades.add(subscriptionTrade);
				return;

			} catch (Exception e) {
				if (attempt == SUBSCRIBE_ER_MAX_ATTEMPTS) {
					logger.error("error subscribing to onUserTrades on {} after {} attempts", instrument, attempt, e);
					System.err.println("error subscribing to onUserTrades " + e.getMessage());
				} else {
					logger.warn("error subscribing to onUserTrades on {} (attempt {}/{}), retrying: {}", instrument,
							attempt, SUBSCRIBE_ER_MAX_ATTEMPTS, e.getMessage());
					sleepBeforeRetry();
				}
			}
		}
	}

	private void subscribeOrderChanges(CurrencyPair currencyPair, Instrument instrument) {
		for (int attempt = 1; attempt <= SUBSCRIBE_ER_MAX_ATTEMPTS; attempt++) {
			try {
				Disposable subscriptionTrade = webSocketClient.getStreamingTradeService().getOrderChanges(currencyPair)
						.subscribe(order -> onOrderChange(instrument, order),
								throwable -> logger.error("Error in onOrderChange subscription", throwable));
				subscriptionOrderChanges.add(subscriptionTrade);
				return;

			} catch (Exception e) {
				if (attempt == SUBSCRIBE_ER_MAX_ATTEMPTS) {
					logger.error("error subscribing to onOrderChange on {} after {} attempts", instrument, attempt, e);
					System.err.println("error subscribing to onOrderChange " + e.getMessage());
				} else {
					logger.warn("error subscribing to onOrderChange on {} (attempt {}/{}), retrying: {}", instrument,
							attempt, SUBSCRIBE_ER_MAX_ATTEMPTS, e.getMessage());
					sleepBeforeRetry();
				}
			}
		}
	}

	private void sleepBeforeRetry() {
		try {
			Thread.sleep(SUBSCRIBE_ER_RETRY_DELAY_MS);
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
		}
	}


	public void reset() {
        for (Disposable disposable : subscriptionTrades) {
            disposable.dispose();
        }
        for (Disposable disposable : subscriptionOrderChanges) {
            disposable.dispose();
        }
        this.brokerConnector.resetClient();
        subscribeER();
    }

	public void onOrderChange(Instrument instrument, Order order) {
		String orderId = order.getId();
		OrderRequest orderRequest = marketOrderIdToOrderRequest.get(orderId);
		if (orderRequest == null) {
			logger.warn("onOrderChange received uknown orderid {} {}", orderId, order.toString());
			return;
		}

		ExecutionReport executionReport = marketOrderIdToER.getOrDefault(orderId, new ExecutionReport(orderRequest));
		switch (order.getStatus()) {
			case NEW:
			case REPLACED:
				executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
				notifyExecutionReport(executionReport);
				break;
			case CANCELED:
				executionReport.setExecutionReportStatus(ExecutionReportStatus.Cancelled);
				notifyExecutionReport(executionReport);
				break;
			case FILLED:
				executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletelyFilled);
				executionReport.setLastQuantity(executionReport.getQuantity() - executionReport.getQuantityFill());
				executionReport.setQuantityFill(order.getCumulativeAmount().doubleValue());//should be equal to qty
				notifyExecutionReport(executionReport);
				break;
			case PARTIALLY_FILLED:
				executionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
				double previousCumQty = executionReport.getQuantityFill();
				double newCumQty = order.getCumulativeAmount().doubleValue();
				double lastQty = newCumQty - previousCumQty;
				executionReport.setLastQuantity(lastQty);
				executionReport.setQuantityFill(order.getCumulativeAmount().doubleValue());//ess than qty
				notifyExecutionReport(executionReport);
				break;
		}
		marketOrderIdToER.put(orderId, executionReport);
	}

	public void onUserTrades(Instrument instrument, UserTrade userTrade) {
		//		String orderId = userTrade.getOrderId();
		//		OrderRequest orderRequest = marketOrderIdToOrderRequest.get(orderId);
		//		if(orderRequest==null){
		//			logger.warn("onUserTrades received uknown orderid {} {}",orderId,userTrade.toString());
		//			return;
		//		}
		//not needed?

	}

	public void setBrokerConnector() {
		if (tradingEngineConfiguration instanceof CoinbaseTradingEngineConfiguration) {
			CoinbaseTradingEngineConfiguration coinbaseTradingEngineConfiguration = (CoinbaseTradingEngineConfiguration) tradingEngineConfiguration;
			this.brokerConnector = CoinbaseBrokerConnector.getInstance(coinbaseTradingEngineConfiguration.getApiKey(),
					coinbaseTradingEngineConfiguration.getSecretKey());
			tradeService = brokerConnector.getStreamingExchange().getTradeService();
		} else if (tradingEngineConfiguration instanceof KrakenTradingEngineConfiguration) {
			KrakenTradingEngineConfiguration krakenTradingEngineConfiguration = (KrakenTradingEngineConfiguration) tradingEngineConfiguration;
			this.brokerConnector = KrakenBrokerConnector.getInstance(krakenTradingEngineConfiguration.getApiKey(),
					krakenTradingEngineConfiguration.getSecretKey());
			tradeService = brokerConnector.getStreamingExchange().getTradeService();
		} else if (tradingEngineConfiguration instanceof BinanceXchangeTradingEngineConfiguration) {
			BinanceXchangeTradingEngineConfiguration binanceTradingEngineConfiguration = (BinanceXchangeTradingEngineConfiguration) tradingEngineConfiguration;
			this.brokerConnector = BinanceXchangeBrokerConnector
					.getInstance(binanceTradingEngineConfiguration.getApiKey(), binanceTradingEngineConfiguration.getSecretKey());
			tradeService = brokerConnector.getStreamingExchange().getTradeService();
		} else if (tradingEngineConfiguration instanceof BybitTradingEngineConfiguration) {
			BybitTradingEngineConfiguration bybitTradingEngineConfiguration = (BybitTradingEngineConfiguration) tradingEngineConfiguration;
			this.brokerConnector = BybitBrokerConnector
					.getInstance(bybitTradingEngineConfiguration.getApiKey(), bybitTradingEngineConfiguration.getSecretKey());
			tradeService = brokerConnector.getStreamingExchange().getTradeService();

		} else {
			System.err.println("trying to construct setBrokerConnector with a not recognized marketDataConfiguration {}" +
					tradingEngineConfiguration);
			logger.error("trying to construct setBrokerConnector with a not recognized marketDataConfiguration {}",
					tradingEngineConfiguration);
		}
	}

	@Override public void setDemoTrading() {
		isDemo = true;
	}

	@Override public boolean orderRequest(OrderRequest orderRequest) {
		//send new order
		if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Send)) {
			if (orderRequest.getOrderType().equals(OrderType.Market)) {
				Order.OrderType orderType = orderRequest.getVerb().equals(Verb.Buy) ?
						Order.OrderType.BID :
						Order.OrderType.ASK;
				CurrencyPair instrument = XChangeBrokerConnector.getCurrencyPair(orderRequest.getInstrument());
				MarketOrder marketOrder = new MarketOrder(orderType, BigDecimal.valueOf(orderRequest.getQuantity()),
						instrument);
				try {
					String orderId = tradeService.placeMarketOrder(marketOrder);
					marketOrderIdToOrderRequest.put(orderId, orderRequest);
					clOrdIdToMarketOrderId.put(orderRequest.getClientOrderId(), orderId);

					//					ExecutionReport executionReport = new ExecutionReport(orderRequest);
					//					executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
					//					notifyExecutionReportById(executionReport);
					return true;

				} catch (Exception e) {
					logger.error("error sending market ->reject {} {}", orderRequest.getClientOrderId(), orderRequest,
							e);
					ExecutionReport executionReport = createRejectionExecutionReport(orderRequest,
							e.getMessage() + " " + e);
					notifyExecutionReport(executionReport);
					return false;
				}
			}

			if (orderRequest.getOrderType().equals(OrderType.Limit)) {
				Order.OrderType orderType = orderRequest.getVerb().equals(Verb.Buy) ?
						Order.OrderType.BID :
						Order.OrderType.ASK;
				CurrencyPair instrument = XChangeBrokerConnector.getCurrencyPair(orderRequest.getInstrument());

				LimitOrder limitOrder = new LimitOrder(orderType, BigDecimal.valueOf(orderRequest.getQuantity()),
						instrument, null, null, BigDecimal.valueOf(orderRequest.getPrice()));
				try {
					String orderId = tradeService.placeLimitOrder(limitOrder);
					marketOrderIdToOrderRequest.put(orderId, orderRequest);
					clOrdIdToMarketOrderId.put(orderRequest.getClientOrderId(), orderId);

					//					ExecutionReport executionReport = new ExecutionReport(orderRequest);
					//					executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
					//					notifyExecutionReportById(executionReport);
					return true;

				} catch (Exception e) {
					logger.error("error sending limit ->reject {} {}", orderRequest.getClientOrderId(), orderRequest,
							e);
					ExecutionReport executionReport = createRejectionExecutionReport(orderRequest,
							e.getMessage() + " " + e);
					notifyExecutionReport(executionReport);
					return false;
				}

			}

		}
		if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Modify) || orderRequest
				.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {

			String marketOrderId = clOrdIdToMarketOrderId.get(orderRequest.getOrigClientOrderId());
			OrderRequest originalOrder = marketOrderIdToOrderRequest.get(marketOrderId);
			List<Order> orders = null;
			try {
				orders = (List<Order>) tradeService.getOrder(String.valueOf(marketOrderId));
			} catch (IOException e) {
				logger.error("can't get order {} for {}", marketOrderId, orderRequest.getOrigClientOrderId(), e);
				ExecutionReport executionReportRej = createRejectionExecutionReport(orderRequest, e.getMessage());
				notifyExecutionReport(executionReportRej);
				return false;
			}

			///

			if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {
				try {
					tradeService.cancelOrder(marketOrderId);
					//					ExecutionReport executionReport = new ExecutionReport(orderRequest);
					//					executionReport.setExecutionReportStatus(ExecutionReportStatus.Cancelled);
					//					notifyExecutionReportById(executionReport);
					return true;
				} catch (Exception e) {
					logger.error("cant get order {} for {}", marketOrderId, orderRequest.getOrigClientOrderId(), e);
					ExecutionReport executionReportRej = createRejectionExecutionReport(orderRequest, e.getMessage());
					executionReportRej.setExecutionReportStatus(ExecutionReportStatus.CancelRejected);
					notifyExecutionReport(executionReportRej);
					return false;
				}
			}
			//
			if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Modify)) {

				try {
					CurrencyPair instrument = XChangeBrokerConnector.getCurrencyPair(orderRequest.getInstrument());
					Order.OrderType orderType = orderRequest.getVerb().equals(Verb.Buy) ?
							Order.OrderType.BID :
							Order.OrderType.ASK;

					LimitOrder limitOrder = new LimitOrder(orderType, BigDecimal.valueOf(orderRequest.getQuantity()),
							instrument, marketOrderId, null, BigDecimal.valueOf(orderRequest.getPrice()));

					String newOrderId = tradeService.changeOrder(limitOrder);
					marketOrderIdToOrderRequest.put(newOrderId, orderRequest);
					clOrdIdToMarketOrderId.put(orderRequest.getClientOrderId(), newOrderId);

					//					ExecutionReport executionReport = new ExecutionReport(orderRequest);
					//					executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
					//					notifyExecutionReportById(executionReport);
					return true;
				} catch (Exception e) {
					logger.error("cant get order {} for {}", marketOrderId, orderRequest.getOrigClientOrderId(), e);
					ExecutionReport executionReportRej = createRejectionExecutionReport(orderRequest, e.getMessage());
					executionReportRej.setExecutionReportStatus(ExecutionReportStatus.Rejected);
					notifyExecutionReport(executionReportRej);
					return false;
				}
			}

		}

		return true;
	}

}
