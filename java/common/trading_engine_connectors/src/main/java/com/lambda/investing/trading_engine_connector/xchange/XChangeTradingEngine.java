package com.lambda.investing.trading_engine_connector.xchange;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
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
	//cumulative filled quantity per market order id, used to compute lastQuantity on (partial) fills.
	//NOTE: each notified ExecutionReport must be a fresh instance (see onOrderChange) -
	//notifyExecutionReport() dispatches synchronously and by reference to registered listeners, so
	//reusing/mutating a cached ExecutionReport instance across statuses would let a later mutation
	//(eg. Active -> CompletelyFilled) retroactively corrupt an already-notified report still held by a listener.
	private Map<String, Double> marketOrderIdToCumulativeQtyFilled;

	private Map<String, String> clOrdIdToMarketOrderId;

	private Map<String, String> modificationCancelIdGenerated;

	private Set<String> activeNotifiedOrderIds;

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
		marketOrderIdToCumulativeQtyFilled = new ConcurrentHashMap<>();

		modificationCancelIdGenerated = new ConcurrentHashMap<>();
		activeNotifiedOrderIds = ConcurrentHashMap.newKeySet();
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

	/**
	 * Delay between per-pair subscribe requests so we don't burst more "subscribe" websocket frames
	 * than the exchange's message-rate limit allows (e.g. Kraken silently drops a random subset of
	 * subscriptions and replies "Exceeded msg rate" once a connection sends too many too fast).
	 */
	private static final long SUBSCRIBE_ER_PACING_MS = 150L;

	protected void subscribeER() {
		if (this.brokerConnector != null) {
			this.webSocketClient = this.brokerConnector.getWebSocketClient();
		}

		for (CurrencyPair currencyPair : brokerConnector.getPairs()) {
			Instrument instrument = brokerConnector.getCurrencyPairToInstrument()
					.get(currencyPair);
			subscribeUserTrades(currencyPair, instrument);
			subscribeOrderChanges(currencyPair, instrument);
			try {
				Thread.sleep(SUBSCRIBE_ER_PACING_MS);
			} catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
				return;
			}
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
				//an uncaught exception in onOrderChange would call the Rx onError handler below and
				//permanently terminate this subscription (no more order updates for the pair), so any
				//unexpected exception must be swallowed here and only logged.
				Disposable subscriptionTrade = webSocketClient.getStreamingTradeService().getOrderChanges(currencyPair)
						.subscribe(order -> {
							try {
								onOrderChange(instrument, order);
							} catch (Exception e) {
								logger.error("unexpected error in onOrderChange for {} {}", instrument, order, e);
							}
						}, throwable -> logger.error("Error in onOrderChange subscription", throwable));
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

	/**
	 * onOrderChange runs on the websocket thread and can race with the REST call thread that is
	 * still populating {@link #marketOrderIdToOrderRequest} for the very same order id (eg. Kraken
	 * pushes the CANCELED/NEW websocket updates of a replace before the blocking cancelOrder/
	 * placeLimitOrder REST calls making up {@link #orderRequest} have returned). Retry briefly
	 * instead of immediately treating the order id as unknown.
	 */
	private static final int ON_ORDER_CHANGE_UNKNOWN_RETRY_ATTEMPTS = 20;
	private static final long ON_ORDER_CHANGE_UNKNOWN_RETRY_DELAY_MS = 100L;

	public void onOrderChange(Instrument instrument, Order order) {
		String orderId = order.getId();
		OrderRequest orderRequest = marketOrderIdToOrderRequest.get(orderId);
		for (int attempt = 1; orderRequest == null && attempt <= ON_ORDER_CHANGE_UNKNOWN_RETRY_ATTEMPTS; attempt++) {
			try {
				Thread.sleep(ON_ORDER_CHANGE_UNKNOWN_RETRY_DELAY_MS);
			} catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
				break;
			}
			orderRequest = marketOrderIdToOrderRequest.get(orderId);
		}
		if (orderRequest == null) {
			logger.warn("onOrderChange received unknown orderid {} {}", orderId, order.toString());
			return;
		}

		if (order.getStatus() == null) {
			//some exchanges (eg. Kraken) push order-change events without a status (eg. amend
			//acknowledgements); nothing actionable to report, just ignore them.
			logger.debug("onOrderChange received orderid {} with null status {}", orderId, order);
			return;
		}

		//always build a fresh ExecutionReport per notification: notifyExecutionReport() dispatches
		//synchronously and by reference to registered listeners, so reusing/mutating a single cached
		//instance across statuses would let a later mutation (eg. FILLED) retroactively corrupt an
		//already-notified report (eg. Active) still held by a listener.
		ExecutionReport executionReport = new ExecutionReport(orderRequest);
		switch (order.getStatus()) {
			case NEW:
			case REPLACED:
				executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
				notifyExecutionReport(executionReport);
				activeNotifiedOrderIds.add(orderId);
				break;
			case CANCELED:
				//some exchanges (eg. Kraken) emit CANCELED for the old order id right before NEW/REPLACED
				//for the new order id when replacing an order; that CANCELED is an implementation detail
				//of the replace and must not be forwarded as a real cancellation.
				if (modificationCancelIdGenerated.remove(orderId) != null) {
					logger.debug("ignoring CANCELED on {} generated by a replace", orderId);
					break;
				}
				executionReport.setExecutionReportStatus(ExecutionReportStatus.Cancelled);
				notifyExecutionReport(executionReport);
				activeNotifiedOrderIds.remove(orderId);
				marketOrderIdToCumulativeQtyFilled.remove(orderId);
				break;
			case FILLED: {
				notifyActiveIfMissing(orderId, orderRequest);
				double previousCumQty = marketOrderIdToCumulativeQtyFilled.getOrDefault(orderId, 0.0);
				double newCumQty = order.getCumulativeAmount().doubleValue();
				executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletelyFilled);
				executionReport.setLastQuantity(newCumQty - previousCumQty);
				executionReport.setQuantityFill(newCumQty);//should be equal to qty
				notifyExecutionReport(executionReport);
				activeNotifiedOrderIds.remove(orderId);
				marketOrderIdToCumulativeQtyFilled.remove(orderId);
				break;
			}
			case PARTIALLY_FILLED: {
				notifyActiveIfMissing(orderId, orderRequest);
				double previousCumQty = marketOrderIdToCumulativeQtyFilled.getOrDefault(orderId, 0.0);
				double newCumQty = order.getCumulativeAmount().doubleValue();
				executionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
				executionReport.setLastQuantity(newCumQty - previousCumQty);
				executionReport.setQuantityFill(newCumQty);//less than qty
				notifyExecutionReport(executionReport);
				marketOrderIdToCumulativeQtyFilled.put(orderId, newCumQty);
				break;
			}
		}
	}

	/**
	 * Some exchanges (eg. Kraken) can fill a marketable replace/limit order so fast that the
	 * NEW/REPLACED (Active) websocket update is skipped or arrives after the fill; downstream
	 * consumers still expect to observe an Active report before a fill, so synthesize one here.
	 */
	private void notifyActiveIfMissing(String orderId, OrderRequest orderRequest) {
		if (activeNotifiedOrderIds.add(orderId)) {
			ExecutionReport activeExecutionReport = new ExecutionReport(orderRequest);
			activeExecutionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
			notifyExecutionReport(activeExecutionReport);
		}
	}

	public void onUserTrades(Instrument instrument, UserTrade userTrade) {
		//		String orderId = userTrade.getOrderId();
		//		OrderRequest orderRequest = marketOrderIdToOrderRequest.get(orderId);
		//		if(orderRequest==null){
		//			logger.warn("onUserTrades received unknown orderid {} {}",orderId,userTrade.toString());
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
		Instrument instrumentModel = Instrument.getInstrument(orderRequest.getInstrument());
		//send new order
		if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Send)) {
			if (orderRequest.getOrderType().equals(OrderType.Market)) {
				Order.OrderType orderType = orderRequest.getVerb().equals(Verb.Buy) ?
						Order.OrderType.BID :
						Order.OrderType.ASK;
				CurrencyPair instrument = XChangeBrokerConnector.getCurrencyPair(orderRequest.getInstrument());
				BigDecimal quantity = BigDecimal.valueOf(instrumentModel.roundQty(orderRequest.getQuantity()));
				MarketOrder marketOrder = new MarketOrder(orderType, quantity,
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

				//round to the instrument tick to avoid sending double-precision artifacts (eg. 70581.099999999999)
				//that the exchange rejects for having too many decimals
				BigDecimal quantity = BigDecimal.valueOf(instrumentModel.roundQty(orderRequest.getQuantity()));
				BigDecimal price = BigDecimal.valueOf(instrumentModel.roundPrice(orderRequest.getPrice()));
				LimitOrder limitOrder = new LimitOrder(orderType, quantity, instrument, null, null, price);
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
				//register before calling cancelOrder: the exchange can push the CANCELED websocket
				//event concurrently with (or even before) the blocking REST call returning, so the
				//execution report must already reflect this cancel request when onOrderChange fires.
				marketOrderIdToOrderRequest.put(marketOrderId, orderRequest);
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

				//register before calling changeOrder: Kraken's changeOrder cancels the old order then
				//places a new one, and the CANCELED websocket push for the old order id can race ahead
				//of this REST call returning, so it must already be ignorable when it arrives.
				modificationCancelIdGenerated.put(marketOrderId, "");
				try {
					CurrencyPair instrument = XChangeBrokerConnector.getCurrencyPair(orderRequest.getInstrument());
					Order.OrderType orderType = orderRequest.getVerb().equals(Verb.Buy) ?
							Order.OrderType.BID :
							Order.OrderType.ASK;

					//round to the instrument tick to avoid sending double-precision artifacts that the
					//exchange rejects for having too many decimals
					BigDecimal quantity = BigDecimal.valueOf(instrumentModel.roundQty(orderRequest.getQuantity()));
					BigDecimal price = BigDecimal.valueOf(instrumentModel.roundPrice(orderRequest.getPrice()));
					LimitOrder limitOrder = new LimitOrder(orderType, quantity, instrument, marketOrderId, null, price);

					String newOrderId = tradeService.changeOrder(limitOrder);
					marketOrderIdToOrderRequest.put(newOrderId, orderRequest);
					clOrdIdToMarketOrderId.put(orderRequest.getClientOrderId(), newOrderId);
					modificationCancelIdGenerated.put(marketOrderId, newOrderId);

					//					ExecutionReport executionReport = new ExecutionReport(orderRequest);
					//					executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
					//					notifyExecutionReportById(executionReport);
					return true;
				} catch (Exception e) {
					modificationCancelIdGenerated.remove(marketOrderId);
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
