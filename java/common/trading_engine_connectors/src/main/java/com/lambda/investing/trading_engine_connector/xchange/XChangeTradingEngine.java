package com.lambda.investing.trading_engine_connector.xchange;

import com.lambda.investing.binance.BinanceBrokerConnector;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.market_data_connector.xchange.CoinbaseMarketDataConfiguration;
import com.lambda.investing.model.Currency;
import com.lambda.investing.model.Market;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.AbstractBrokerTradingEngine;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import com.lambda.investing.trading_engine_connector.TradingEngineConfiguration;
import com.lambda.investing.trading_engine_connector.binance.BinanceBrokerTradingEngine;
import com.lambda.investing.trading_engine_connector.binance.BinanceTradingEngineConfiguration;
import com.lambda.investing.xchange.BinanceXchangeBrokerConnector;
import com.lambda.investing.xchange.CoinbaseBrokerConnector;
import com.lambda.investing.xchange.KrakenBrokerConnector;
import com.lambda.investing.xchange.XChangeBrokerConnector;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.disposables.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;

import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.service.marketdata.MarketDataService;
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
	protected List<Instrument> instrumentList;

	public XChangeTradingEngine(ConnectorConfiguration orderRequestConnectorConfiguration,
			ConnectorProvider orderRequestConnectorProvider,
			ConnectorConfiguration executionReportConnectorConfiguration,
			ConnectorPublisher executionReportConnectorPublisher, TradingEngineConfiguration tradingEngineConfiguration,
			List<Instrument> instrumentList) {
		super(orderRequestConnectorConfiguration, orderRequestConnectorProvider, executionReportConnectorConfiguration,
				executionReportConnectorPublisher);
		this.tradingEngineConfiguration = tradingEngineConfiguration;
		setBrokerConnector();

		marketOrderIdToOrderRequest = new ConcurrentHashMap<>();
		clOrdIdToMarketOrderId = new ConcurrentHashMap<>();
		marketOrderIdToER = new ConcurrentHashMap<>();

		modificationCancelIdGenerated = new ConcurrentHashMap<>();
		listenersManager = new HashMap<>();

		this.instrumentList = instrumentList;

	}

	@Override public void start() {
		super.start();
		this.brokerConnector.connectWebsocket(instrumentList);
		subscribeER();
	}

	protected void subscribeER() {
		if (this.brokerConnector != null) {
			this.webSocketClient = this.brokerConnector.getWebSocketClient();
		}

		for (CurrencyPair currencyPair : brokerConnector.getPairs()) {
			com.lambda.investing.model.asset.Instrument instrument = brokerConnector.getCurrencyPairToInstrument()
					.get(currencyPair);
			try {
				Disposable subscriptionTrade = webSocketClient.getStreamingTradeService().getUserTrades(currencyPair)
						.subscribe(userTrade -> onUserTrades(instrument, userTrade),
								throwable -> logger.error("Error in onUserTrades subscription", throwable));

				subscriptionTrades.add(subscriptionTrade);

			} catch (Exception e) {
				logger.error("error subscribing to onUserTrades on {} ", instrument, e);
				System.err.println("error subscribing to onUserTrades " + e.getMessage());
				//				e.printStackTrace();
			}

			try {
				Disposable subscriptionTrade = webSocketClient.getStreamingTradeService().getOrderChanges(currencyPair)
						.subscribe(order -> onOrderChange(instrument, order),
								throwable -> logger.error("Error in onOrderChange subscription", throwable));
				subscriptionOrderChanges.add(subscriptionTrade);

			} catch (Exception e) {
				logger.error("error subscribing to onOrderChange on {} ", instrument, e);
				System.err.println("error subscribing to onOrderChange " + e.getMessage());
				//				e.printStackTrace();
			}

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
				notifyExecutionReportById(executionReport);
				break;
			case CANCELED:
				executionReport.setExecutionReportStatus(ExecutionReportStatus.Cancelled);
				notifyExecutionReportById(executionReport);
				break;
			case FILLED:
				executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
				executionReport.setLastQuantity(executionReport.getQuantity() - executionReport.getQuantityFill());
				executionReport.setQuantityFill(order.getCumulativeAmount().doubleValue());//should be equal to qty
				notifyExecutionReportById(executionReport);
				break;
			case PARTIALLY_FILLED:
				executionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
				double previousCumQty = executionReport.getQuantityFill();
				double newCumQty = order.getCumulativeAmount().doubleValue();
				double lastQty = newCumQty - previousCumQty;
				executionReport.setLastQuantity(lastQty);
				executionReport.setQuantityFill(order.getCumulativeAmount().doubleValue());//ess than qty
				notifyExecutionReportById(executionReport);
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

		} else {
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
					notifyExecutionReportById(executionReport);
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
					notifyExecutionReportById(executionReport);
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
				logger.error("cant get order {} for {}", marketOrderId, orderRequest.getOrigClientOrderId(), e);
				ExecutionReport executionReportRej = createRejectionExecutionReport(orderRequest, e.getMessage());
				notifyExecutionReportById(executionReportRej);
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
					notifyExecutionReportById(executionReportRej);
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
					notifyExecutionReportById(executionReportRej);
					return false;
				}
			}

		}

		return true;
	}

}
