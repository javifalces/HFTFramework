package com.binance.api.client.impl;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.config.BinanceApiConfig;
import com.binance.api.client.constant.BinanceApiConstants;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.DepositAddress;
import com.binance.api.client.domain.account.DepositHistory;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.Trade;
import com.binance.api.client.domain.account.TradeHistoryItem;
import com.binance.api.client.domain.account.WithdrawHistory;
import com.binance.api.client.domain.account.WithdrawResult;
import com.binance.api.client.domain.account.request.AllOrdersRequest;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.CancelOrderResponse;
import com.binance.api.client.domain.account.request.OrderRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.event.ListenKey;
import com.binance.api.client.domain.general.Asset;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.general.ServerTime;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.BookTicker;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.TickerPrice;
import com.binance.api.client.domain.market.TickerStatistics;

import java.util.List;

import static com.binance.api.client.impl.BinanceApiServiceGenerator.createService;

/**
 * Implementation of Binance's REST API using Retrofit with asynchronous/non-blocking method calls.
 */
public class BinanceApiAsyncRestClientImpl implements BinanceApiAsyncRestClient {

	private final BinanceApiService binanceApiService;

	public BinanceApiAsyncRestClientImpl(String apiKey, String secret) {
		binanceApiService = createService(BinanceApiService.class, apiKey, secret);
	}

	// General endpoints

	@Override public void ping(BinanceApiCallback<Void> callback) {
		binanceApiService.ping().enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getServerTime(BinanceApiCallback<ServerTime> callback) {
		binanceApiService.getServerTime().enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getExchangeInfo(BinanceApiCallback<ExchangeInfo> callback) {
		binanceApiService.getExchangeInfo().enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getAllAssets(BinanceApiCallback<List<Asset>> callback) {
		binanceApiService.getAllAssets(BinanceApiConfig.getAssetInfoApiBaseUrl() + "assetWithdraw/getAllAsset.html")
				.enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	// Market Data endpoints

	@Override public void getOrderBook(String symbol, Integer limit, BinanceApiCallback<OrderBook> callback) {
		binanceApiService.getOrderBook(symbol, limit).enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getTrades(String symbol, Integer limit, BinanceApiCallback<List<TradeHistoryItem>> callback) {
		binanceApiService.getTrades(symbol, limit).enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getHistoricalTrades(String symbol, Integer limit, Long fromId,
			BinanceApiCallback<List<TradeHistoryItem>> callback) {
		binanceApiService.getHistoricalTrades(symbol, limit, fromId).enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getAggTrades(String symbol, String fromId, Integer limit, Long startTime, Long endTime,
			BinanceApiCallback<List<AggTrade>> callback) {
		binanceApiService.getAggTrades(symbol, fromId, limit, startTime, endTime)
				.enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getAggTrades(String symbol, BinanceApiCallback<List<AggTrade>> callback) {
		getAggTrades(symbol, null, null, null, null, callback);
	}

	@Override public void getCandlestickBars(String symbol, CandlestickInterval interval, Integer limit, Long startTime,
			Long endTime, BinanceApiCallback<List<Candlestick>> callback) {
		binanceApiService.getCandlestickBars(symbol, interval.getIntervalId(), limit, startTime, endTime)
				.enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getCandlestickBars(String symbol, CandlestickInterval interval,
			BinanceApiCallback<List<Candlestick>> callback) {
		getCandlestickBars(symbol, interval, null, null, null, callback);
	}

	@Override public void get24HrPriceStatistics(String symbol, BinanceApiCallback<TickerStatistics> callback) {
		binanceApiService.get24HrPriceStatistics(symbol).enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getAll24HrPriceStatistics(BinanceApiCallback<List<TickerStatistics>> callback) {
		binanceApiService.getAll24HrPriceStatistics().enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getAllPrices(BinanceApiCallback<List<TickerPrice>> callback) {
		binanceApiService.getLatestPrices().enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getPrice(String symbol, BinanceApiCallback<TickerPrice> callback) {
		binanceApiService.getLatestPrice(symbol).enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getBookTickers(BinanceApiCallback<List<BookTicker>> callback) {
		binanceApiService.getBookTickers().enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void newOrder(NewOrder order, BinanceApiCallback<NewOrderResponse> callback) {
		if (order.getQuoteOrderQty() == null) {
			binanceApiService.newOrder(order.getSymbol(), order.getSide(), order.getType(), order.getTimeInForce(),
					order.getQuantity(), order.getPrice(), order.getNewClientOrderId(), order.getStopPrice(),
					order.getIcebergQty(), order.getNewOrderRespType(), order.getRecvWindow(), order.getTimestamp())
					.enqueue(new BinanceApiCallbackAdapter<>(callback));
		} else {
			binanceApiService
					.newOrderQuoteQty(order.getSymbol(), order.getSide(), order.getType(), order.getTimeInForce(),
							order.getQuoteOrderQty(), order.getPrice(), order.getNewClientOrderId(),
							order.getStopPrice(), order.getIcebergQty(), order.getNewOrderRespType(),
							order.getRecvWindow(), order.getTimestamp())
					.enqueue(new BinanceApiCallbackAdapter<>(callback));
		}
	}

	@Override public void newOrderTest(NewOrder order, BinanceApiCallback<Void> callback) {
		binanceApiService.newOrderTest(order.getSymbol(), order.getSide(), order.getType(), order.getTimeInForce(),
				order.getQuantity(), order.getPrice(), order.getNewClientOrderId(), order.getStopPrice(),
				order.getIcebergQty(), order.getNewOrderRespType(), order.getRecvWindow(), order.getTimestamp())
				.enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	// Account endpoints

	@Override public void getOrderStatus(OrderStatusRequest orderStatusRequest, BinanceApiCallback<Order> callback) {
		binanceApiService.getOrderStatus(orderStatusRequest.getSymbol(), orderStatusRequest.getOrderId(),
				orderStatusRequest.getOrigClientOrderId(), orderStatusRequest.getRecvWindow(),
				orderStatusRequest.getTimestamp()).enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void cancelOrder(CancelOrderRequest cancelOrderRequest,
			BinanceApiCallback<CancelOrderResponse> callback) {
		binanceApiService.cancelOrder(cancelOrderRequest.getSymbol(), cancelOrderRequest.getOrderId(),
				cancelOrderRequest.getOrigClientOrderId(), cancelOrderRequest.getNewClientOrderId(),
				cancelOrderRequest.getRecvWindow(), cancelOrderRequest.getTimestamp())
				.enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getOpenOrders(OrderRequest orderRequest, BinanceApiCallback<List<Order>> callback) {
		binanceApiService
				.getOpenOrders(orderRequest.getSymbol(), orderRequest.getRecvWindow(), orderRequest.getTimestamp())
				.enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getAllOrders(AllOrdersRequest orderRequest, BinanceApiCallback<List<Order>> callback) {
		binanceApiService.getAllOrders(orderRequest.getSymbol(), orderRequest.getOrderId(), orderRequest.getLimit(),
				orderRequest.getRecvWindow(), orderRequest.getTimestamp())
				.enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getAccount(Long recvWindow, Long timestamp, BinanceApiCallback<Account> callback) {
		binanceApiService.getAccount(recvWindow, timestamp).enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getAccount(BinanceApiCallback<Account> callback) {
		long timestamp = System.currentTimeMillis();
		binanceApiService.getAccount(BinanceApiConstants.DEFAULT_RECEIVING_WINDOW, timestamp)
				.enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getMyTrades(String symbol, Integer limit, Long fromId, Long recvWindow, Long timestamp,
			BinanceApiCallback<List<Trade>> callback) {
		binanceApiService.getMyTrades(symbol, limit, fromId, recvWindow, timestamp)
				.enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getMyTrades(String symbol, Integer limit, BinanceApiCallback<List<Trade>> callback) {
		getMyTrades(symbol, limit, null, BinanceApiConstants.DEFAULT_RECEIVING_WINDOW, System.currentTimeMillis(),
				callback);
	}

	@Override public void getMyTrades(String symbol, BinanceApiCallback<List<Trade>> callback) {
		getMyTrades(symbol, null, null, BinanceApiConstants.DEFAULT_RECEIVING_WINDOW, System.currentTimeMillis(),
				callback);
	}

	@Override public void withdraw(String asset, String address, String amount, String name, String addressTag,
			BinanceApiCallback<WithdrawResult> callback) {
		binanceApiService
				.withdraw(asset, address, amount, name, addressTag, BinanceApiConstants.DEFAULT_RECEIVING_WINDOW,
						System.currentTimeMillis()).enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getDepositHistory(String asset, BinanceApiCallback<DepositHistory> callback) {
		binanceApiService
				.getDepositHistory(asset, BinanceApiConstants.DEFAULT_RECEIVING_WINDOW, System.currentTimeMillis())
				.enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getWithdrawHistory(String asset, BinanceApiCallback<WithdrawHistory> callback) {
		binanceApiService
				.getWithdrawHistory(asset, BinanceApiConstants.DEFAULT_RECEIVING_WINDOW, System.currentTimeMillis())
				.enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void getDepositAddress(String asset, BinanceApiCallback<DepositAddress> callback) {
		binanceApiService
				.getDepositAddress(asset, BinanceApiConstants.DEFAULT_RECEIVING_WINDOW, System.currentTimeMillis())
				.enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	// User stream endpoints

	@Override public void startUserDataStream(BinanceApiCallback<ListenKey> callback) {
		binanceApiService.startUserDataStream().enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void keepAliveUserDataStream(String listenKey, BinanceApiCallback<Void> callback) {
		binanceApiService.keepAliveUserDataStream(listenKey).enqueue(new BinanceApiCallbackAdapter<>(callback));
	}

	@Override public void closeUserDataStream(String listenKey, BinanceApiCallback<Void> callback) {
		binanceApiService.closeAliveUserDataStream(listenKey).enqueue(new BinanceApiCallbackAdapter<>(callback));
	}
}
