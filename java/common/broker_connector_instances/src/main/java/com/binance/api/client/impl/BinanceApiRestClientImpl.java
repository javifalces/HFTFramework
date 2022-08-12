package com.binance.api.client.impl;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.config.BinanceApiConfig;
import com.binance.api.client.constant.BinanceApiConstants;
import com.binance.api.client.domain.account.*;
import com.binance.api.client.domain.account.request.*;
import com.binance.api.client.domain.general.Asset;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.market.*;
import retrofit2.Call;

import java.util.List;

import static com.binance.api.client.impl.BinanceApiServiceGenerator.createService;
import static com.binance.api.client.impl.BinanceApiServiceGenerator.executeSync;

/**
 * Implementation of Binance's REST API using Retrofit with synchronous/blocking method calls.
 */
public class BinanceApiRestClientImpl implements BinanceApiRestClient {

	private final BinanceApiService binanceApiService;

	public BinanceApiRestClientImpl(String apiKey, String secret) {
		binanceApiService = createService(BinanceApiService.class, apiKey, secret);
	}

	// General endpoints

	@Override public void ping() {
		executeSync(binanceApiService.ping());
	}

	@Override public Long getServerTime() {
		return executeSync(binanceApiService.getServerTime()).getServerTime();
	}

	@Override public ExchangeInfo getExchangeInfo() {
		return executeSync(binanceApiService.getExchangeInfo());
	}

	@Override public List<Asset> getAllAssets() {
		return executeSync(binanceApiService
				.getAllAssets(BinanceApiConfig.getAssetInfoApiBaseUrl() + "assetWithdraw/getAllAsset.html"));
	}

	// Market Data endpoints

	@Override public OrderBook getOrderBook(String symbol, Integer limit) {
		return executeSync(binanceApiService.getOrderBook(symbol, limit));
	}

	@Override public List<TradeHistoryItem> getTrades(String symbol, Integer limit) {
		return executeSync(binanceApiService.getTrades(symbol, limit));
	}

	@Override public List<TradeHistoryItem> getHistoricalTrades(String symbol, Integer limit, Long fromId) {
		return executeSync(binanceApiService.getHistoricalTrades(symbol, limit, fromId));
	}

	@Override public List<AggTrade> getAggTrades(String symbol, String fromId, Integer limit, Long startTime,
			Long endTime) {
		return executeSync(binanceApiService.getAggTrades(symbol, fromId, limit, startTime, endTime));
	}

	@Override public List<AggTrade> getAggTrades(String symbol) {
		return getAggTrades(symbol, null, null, null, null);
	}

	@Override public List<Candlestick> getCandlestickBars(String symbol, CandlestickInterval interval, Integer limit,
			Long startTime, Long endTime) {
		return executeSync(
				binanceApiService.getCandlestickBars(symbol, interval.getIntervalId(), limit, startTime, endTime));
	}

	@Override public List<Candlestick> getCandlestickBars(String symbol, CandlestickInterval interval) {
		return getCandlestickBars(symbol, interval, null, null, null);
	}

	@Override public TickerStatistics get24HrPriceStatistics(String symbol) {
		return executeSync(binanceApiService.get24HrPriceStatistics(symbol));
	}

	@Override public List<TickerStatistics> getAll24HrPriceStatistics() {
		return executeSync(binanceApiService.getAll24HrPriceStatistics());
	}

	@Override public TickerPrice getPrice(String symbol) {
		return executeSync(binanceApiService.getLatestPrice(symbol));
	}

	@Override public List<TickerPrice> getAllPrices() {
		return executeSync(binanceApiService.getLatestPrices());
	}

	@Override public List<BookTicker> getBookTickers() {
		return executeSync(binanceApiService.getBookTickers());
	}

	@Override public NewOrderResponse newOrder(NewOrder order) {
		final Call<NewOrderResponse> call;
		if (order.getQuoteOrderQty() == null) {
			call = binanceApiService
					.newOrder(order.getSymbol(), order.getSide(), order.getType(), order.getTimeInForce(),
							order.getQuantity(), order.getPrice(), order.getNewClientOrderId(), order.getStopPrice(),
							order.getIcebergQty(), order.getNewOrderRespType(), order.getRecvWindow(),
							order.getTimestamp());
		} else {
			call = binanceApiService
					.newOrderQuoteQty(order.getSymbol(), order.getSide(), order.getType(), order.getTimeInForce(),
							order.getQuoteOrderQty(), order.getPrice(), order.getNewClientOrderId(),
							order.getStopPrice(), order.getIcebergQty(), order.getNewOrderRespType(),
							order.getRecvWindow(), order.getTimestamp());
		}
		return executeSync(call);
	}

	@Override public void newOrderTest(NewOrder order) {
		executeSync(binanceApiService
				.newOrderTest(order.getSymbol(), order.getSide(), order.getType(), order.getTimeInForce(),
						order.getQuantity(), order.getPrice(), order.getNewClientOrderId(), order.getStopPrice(),
						order.getIcebergQty(), order.getNewOrderRespType(), order.getRecvWindow(),
						order.getTimestamp()));
	}

	// Account endpoints

	@Override public Order getOrderStatus(OrderStatusRequest orderStatusRequest) {
		return executeSync(binanceApiService
				.getOrderStatus(orderStatusRequest.getSymbol(), orderStatusRequest.getOrderId(),
						orderStatusRequest.getOrigClientOrderId(), orderStatusRequest.getRecvWindow(),
						orderStatusRequest.getTimestamp()));
	}

	@Override public CancelOrderResponse cancelOrder(CancelOrderRequest cancelOrderRequest) {
		return executeSync(binanceApiService
				.cancelOrder(cancelOrderRequest.getSymbol(), cancelOrderRequest.getOrderId(),
						cancelOrderRequest.getOrigClientOrderId(), cancelOrderRequest.getNewClientOrderId(),
						cancelOrderRequest.getRecvWindow(), cancelOrderRequest.getTimestamp()));
	}

	@Override public List<Order> getOpenOrders(OrderRequest orderRequest) {
		return executeSync(binanceApiService
				.getOpenOrders(orderRequest.getSymbol(), orderRequest.getRecvWindow(), orderRequest.getTimestamp()));
	}

	@Override public List<Order> getAllOrders(AllOrdersRequest orderRequest) {
		return executeSync(binanceApiService
				.getAllOrders(orderRequest.getSymbol(), orderRequest.getOrderId(), orderRequest.getLimit(),
						orderRequest.getRecvWindow(), orderRequest.getTimestamp()));
	}

	@Override public Account getAccount(Long recvWindow, Long timestamp) {
		return executeSync(binanceApiService.getAccount(recvWindow, timestamp));
	}

	@Override public Account getAccount() {
		return getAccount(BinanceApiConstants.DEFAULT_RECEIVING_WINDOW, System.currentTimeMillis());
	}

	@Override public List<Trade> getMyTrades(String symbol, Integer limit, Long fromId, Long recvWindow,
			Long timestamp) {
		return executeSync(binanceApiService.getMyTrades(symbol, limit, fromId, recvWindow, timestamp));
	}

	@Override public List<Trade> getMyTrades(String symbol, Integer limit) {
		return getMyTrades(symbol, limit, null, BinanceApiConstants.DEFAULT_RECEIVING_WINDOW,
				System.currentTimeMillis());
	}

	@Override public List<Trade> getMyTrades(String symbol) {
		return getMyTrades(symbol, null, null, BinanceApiConstants.DEFAULT_RECEIVING_WINDOW,
				System.currentTimeMillis());
	}

	@Override public WithdrawResult withdraw(String asset, String address, String amount, String name,
			String addressTag) {
		return executeSync(binanceApiService
				.withdraw(asset, address, amount, name, addressTag, BinanceApiConstants.DEFAULT_RECEIVING_WINDOW,
						System.currentTimeMillis()));
	}

	@Override public DepositHistory getDepositHistory(String asset) {
		return executeSync(binanceApiService
				.getDepositHistory(asset, BinanceApiConstants.DEFAULT_RECEIVING_WINDOW, System.currentTimeMillis()));
	}

	@Override public WithdrawHistory getWithdrawHistory(String asset) {
		return executeSync(binanceApiService
				.getWithdrawHistory(asset, BinanceApiConstants.DEFAULT_RECEIVING_WINDOW, System.currentTimeMillis()));
	}

	@Override public DepositAddress getDepositAddress(String asset) {
		return executeSync(binanceApiService
				.getDepositAddress(asset, BinanceApiConstants.DEFAULT_RECEIVING_WINDOW, System.currentTimeMillis()));
	}

	// User stream endpoints

	@Override public String startUserDataStream() {
		return executeSync(binanceApiService.startUserDataStream()).toString();
	}

	@Override public void keepAliveUserDataStream(String listenKey) {
		executeSync(binanceApiService.keepAliveUserDataStream(listenKey));
	}

	@Override public void closeUserDataStream(String listenKey) {
		executeSync(binanceApiService.closeAliveUserDataStream(listenKey));
	}
}
