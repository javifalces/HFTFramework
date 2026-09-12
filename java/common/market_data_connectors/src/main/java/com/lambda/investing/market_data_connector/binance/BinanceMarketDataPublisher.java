package com.lambda.investing.market_data_connector.binance;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.domain.event.DepthEvent;
import com.binance.api.client.domain.market.OrderBook;
import com.lambda.investing.binance.BinanceBrokerConnector;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.market_data_connector.AbstractMarketDataConnectorPublisher;
import com.lambda.investing.market_data_connector.MarketDataConfiguration;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * https://github.com/binance-exchange/binance-java-api
 */
public class BinanceMarketDataPublisher extends AbstractMarketDataConnectorPublisher implements Runnable {

	private static boolean CHECK_SEND_TIMESTAMP = false;//send all to persist it!
	private static int MAX_DEPTH = Depth.MAX_DEPTH;
	//max levels allowed by Binance REST snapshot endpoint are 5,10,20,50,100,500,1000,5000
	private static int SNAPSHOT_DEPTH_LIMIT = 100;
	protected Logger logger = LogManager.getLogger(BinanceMarketDataPublisher.class);
	private BinanceBrokerConnector binanceConnector;
	private List<Instrument> instrumentList;
	private MarketDataConfiguration marketDataConfiguration;
	private Map<String, Instrument> symbolToInstrument;
	private Map<Instrument, Long> lastDepthSent;
	private Map<Instrument, Long> lastTradeSent;
	private Map<Instrument, BinanceLocalOrderBook> localOrderBooks;
	private ExecutorService snapshotExecutor = Executors
			.newCachedThreadPool(r -> new Thread(r, "binanceOrderBookSnapshotFetcher"));

	private AtomicLong counterReceives = new AtomicLong();
	private Long lastTimestampReceived = 0L;
	private Thread threadCheckConnection;

	public BinanceMarketDataPublisher(ConnectorConfiguration connectorConfiguration,
			ConnectorPublisher connectorPublisher, MarketDataConfiguration marketDataConfiguration,
			List<Instrument> instrumentList) {
		super(connectorConfiguration, connectorPublisher);
		this.marketDataConfiguration = marketDataConfiguration;
		if (marketDataConfiguration instanceof BinanceMarketDataConfiguration) {
			BinanceMarketDataConfiguration binanceMarketDataConfiguration = (BinanceMarketDataConfiguration) marketDataConfiguration;
			this.binanceConnector = BinanceBrokerConnector.getInstance(binanceMarketDataConfiguration.getApiKey(),
					binanceMarketDataConfiguration.getSecretKey());
		} else {
			logger.error("trying to construct BinanceMarketDataPublisher with a not BinanceMarketDataConfiguration");
		}
		this.instrumentList = instrumentList;
		symbolToInstrument = new ConcurrentHashMap<>();
		lastDepthSent = new ConcurrentHashMap<>();
		lastTradeSent = new ConcurrentHashMap<>();
		localOrderBooks = new ConcurrentHashMap<>();

	}

	public BinanceMarketDataPublisher(String name, ConnectorConfiguration connectorConfiguration,
			ConnectorPublisher connectorPublisher, MarketDataConfiguration marketDataConfiguration,
			List<Instrument> instrumentList) {
		super(name, connectorConfiguration, connectorPublisher);
		this.marketDataConfiguration = marketDataConfiguration;
		if (marketDataConfiguration instanceof BinanceMarketDataConfiguration) {
			BinanceMarketDataConfiguration binanceMarketDataConfiguration = (BinanceMarketDataConfiguration) marketDataConfiguration;
			this.binanceConnector = BinanceBrokerConnector.getInstance(binanceMarketDataConfiguration.getApiKey(),
					binanceMarketDataConfiguration.getSecretKey());
		} else {
			logger.error("trying to construct BinanceMarketDataPublisher with a not BinanceMarketDataConfiguration");
		}

		this.instrumentList = instrumentList;
		symbolToInstrument = new ConcurrentHashMap<>();
		lastDepthSent = new ConcurrentHashMap<>();
		lastTradeSent = new ConcurrentHashMap<>();
		localOrderBooks = new ConcurrentHashMap<>();

	}

	protected void connectWebsocket() {
		//connect to websocket depth trade updates

		BinanceApiWebSocketClient webSocketClient = this.binanceConnector.getWebSocketClient();
		StringBuilder symbolsList = new StringBuilder();
		for (Instrument instrument : instrumentList) {
			symbolsList.append(instrument.getSymbol().toLowerCase());
			symbolToInstrument.put(instrument.getSymbol().toLowerCase(), instrument);
			symbolsList.append(',');
		}

		logger.info("subscribing to {}", symbolsList.toString());
		//register to trades
		webSocketClient.onAggTradeEvent(symbolsList.toString(), new BinanceApiCallback<AggTradeEvent>() {

			@Override public void onResponse(final AggTradeEvent response) {
				Instrument instrument = symbolToInstrument.get(response.getSymbol().toLowerCase());
				onBinanceTradeUpdate(instrument, response);
				counterReceives.incrementAndGet();
				lastTimestampReceived = System.currentTimeMillis();
			}

			@Override public void onFailure(final Throwable cause) {
				logger.error("Web socket trade failed {} ", cause.getMessage(), cause);
			}

		});
		//register to depth
		webSocketClient.onDepthEvent(symbolsList.toString(), new BinanceApiCallback<DepthEvent>() {

			@Override public void onResponse(final DepthEvent response) {
				Instrument instrument = symbolToInstrument.get(response.getSymbol().toLowerCase());
				onBinanceDepthUpdate(instrument, response);
				counterReceives.incrementAndGet();
				lastTimestampReceived = System.currentTimeMillis();
			}

			@Override public void onFailure(final Throwable cause) {
				logger.error("Web socket depth failed {} ", cause.getMessage(), cause);
			}

		});
	}

	@Override public void init() {

	}

	@Override public void start() {
		threadCheckConnection = new Thread(this, "threadCheckConnection");
		super.start();
		connectWebsocket();
		threadCheckConnection.start();
	}

	@Override
	public void stop() {
		super.stop();
		snapshotExecutor.shutdownNow();
	}

	private synchronized void onBinanceTradeUpdate(Instrument instrument, AggTradeEvent aggTradeEvent) {
		//change from AddTradeEvent to Trade and notify
		try {
			Long lastTradeSentTimestamp = lastTradeSent.getOrDefault(instrument, 0L);
			if (CHECK_SEND_TIMESTAMP && aggTradeEvent.getEventTime() < lastTradeSentTimestamp) {
				return;
			}

			Trade tradeToNotify = Trade.getInstance();
			tradeToNotify.setInstrument(instrument.getPrimaryKey());
			tradeToNotify.setTimestamp(aggTradeEvent.getEventTime());
			tradeToNotify.setTimestampBrokerConnector(System.currentTimeMillis());
			try {
				tradeToNotify.setPrice(
						BinanceBrokerConnector.NUMBER_FORMAT.parse(aggTradeEvent.getPrice().toUpperCase())
								.doubleValue());
				tradeToNotify.setQuantity(
						BinanceBrokerConnector.NUMBER_FORMAT.parse(aggTradeEvent.getQuantity().toUpperCase())
								.doubleValue());
			} catch (Exception e) {
				logger.error("Error parsing trade event {} ", instrument, e);
				return;
			}

			notifyTrade(instrument.getPrimaryKey(), tradeToNotify);
			lastTradeSent.put(instrument, aggTradeEvent.getEventTime());
		} catch (Exception e) {
			logger.error("Error onBinanceTradeUpdate {}", instrument, e);
		}

	}

	/**
	 * depthEvent carries a delta update, not a full book; it is applied on a per-instrument
	 * {@link BinanceLocalOrderBook} that is kept in sync with the exchange (REST snapshot +
	 * ordered replay of buffered updates, following Binance's official procedure), and the
	 * resulting full snapshot is what gets published downstream.
	 */
	private void onBinanceDepthUpdate(Instrument instrument, DepthEvent depthEvent) {
		try {
			BinanceLocalOrderBook localOrderBook = localOrderBooks
					.computeIfAbsent(instrument, i -> new BinanceLocalOrderBook(i.getSymbol()));

			if (!localOrderBook.isInitialized()) {
				localOrderBook.bufferEvent(depthEvent);
				requestSnapshotIfNeeded(instrument, localOrderBook);
				return;
			}

			boolean applied = localOrderBook.applyUpdate(depthEvent);
			if (!applied) {
				//gap detected -> event already buffered inside localOrderBook, trigger a resync
				requestSnapshotIfNeeded(instrument, localOrderBook);
				return;
			}

			Long lastDepthSentTimestamp = lastDepthSent.getOrDefault(instrument, 0L);
			if (CHECK_SEND_TIMESTAMP && depthEvent.getEventTime() < lastDepthSentTimestamp) {
				return;
			}

			Depth depth = Depth.getInstancePool();
			depth.setInstrument(instrument.getPrimaryKey());
			depth.setTimestamp(depthEvent.getEventTime());
			depth.setTimestampBrokerConnector(System.currentTimeMillis());
			depth.setAsks(localOrderBook.getTopAskPrices(MAX_DEPTH));
			depth.setAsksQuantities(localOrderBook.getTopAskQuantities(MAX_DEPTH));
			depth.setBids(localOrderBook.getTopBidPrices(MAX_DEPTH));
			depth.setBidsQuantities(localOrderBook.getTopBidQuantities(MAX_DEPTH));


			notifyDepth(instrument.getPrimaryKey(), depth);
			lastDepthSent.put(instrument, depthEvent.getEventTime());
		} catch (Exception ex) {
			logger.error("Error onDepthUpdate {} ", instrument, ex);
		}
	}

	/**
	 * Fetches a REST snapshot to (re)build the local order book, buffering live depth events
	 * meanwhile so none are lost while the snapshot request is in flight.
	 */
	private void requestSnapshotIfNeeded(Instrument instrument, BinanceLocalOrderBook localOrderBook) {
		if (localOrderBook.isSnapshotRequestInFlight()) {
			return;
		}
		localOrderBook.markSnapshotRequested();
		snapshotExecutor.submit(() -> {
			try {
				BinanceApiRestClient restClient = binanceConnector.getRestClient();
				OrderBook snapshot = restClient.getOrderBook(instrument.getSymbol().toUpperCase(),
						SNAPSHOT_DEPTH_LIMIT);
				localOrderBook.initFromSnapshot(snapshot);
				if (!localOrderBook.isInitialized()) {
					//gap between snapshot and buffered events -> retry immediately
					requestSnapshotIfNeeded(instrument, localOrderBook);
				}
			} catch (Exception e) {
				logger.error("Error fetching order book snapshot for {}", instrument, e);
				localOrderBook.resetSnapshotRequest();
			}
		});
	}

	@Override public void run() {
		long lastSeeReceiverCounter = 0;
		while (enable) {
			long elapsedTime = System.currentTimeMillis() - lastTimestampReceived;
			boolean conditionToReconnect =
					lastTimestampReceived != 0 && lastSeeReceiverCounter != 0 && elapsedTime > 1000
							&& lastSeeReceiverCounter == counterReceives.get();
			if (conditionToReconnect) {
				logger.warn("reconnecting  websocket -> conditions met to launch again");
				this.binanceConnector.resetClient();
				localOrderBooks.clear(); //force a fresh snapshot resync for every instrument
				connectWebsocket();
			}

			lastSeeReceiverCounter = counterReceives.get();

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
