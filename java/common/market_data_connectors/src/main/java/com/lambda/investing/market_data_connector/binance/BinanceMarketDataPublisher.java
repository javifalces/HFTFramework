package com.lambda.investing.market_data_connector.binance;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.domain.event.DepthEvent;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.lambda.investing.binance.BinanceBrokerConnector;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.market_data_connector.AbstractMarketDataConnectorPublisher;
import com.lambda.investing.market_data_connector.MarketDataConfiguration;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * https://github.com/binance-exchange/binance-java-api
 */
public class BinanceMarketDataPublisher extends AbstractMarketDataConnectorPublisher implements Runnable {

	private static boolean CHECK_SEND_TIMESTAMP = false;//send all to persist it!
	private static int MIN_VALID_DEPTH = 2;
	private static int MAX_DEPTH = 10;
	protected Logger logger = LogManager.getLogger(BinanceMarketDataPublisher.class);
	private BinanceBrokerConnector binanceConnector;
	private List<Instrument> instrumentList;
	private MarketDataConfiguration marketDataConfiguration;
	private Map<String, Instrument> symbolToInstrument;
	private Map<Instrument, Long> lastDepthSent;
	private Map<Instrument, Long> lastTradeSent;

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

	private synchronized void onBinanceTradeUpdate(Instrument instrument, AggTradeEvent aggTradeEvent) {
		//change from AddTradeEvent to Trade and notify
		try {
			Long lastTradeSentTimestamp = lastTradeSent.getOrDefault(instrument, 0L);
			if (CHECK_SEND_TIMESTAMP && aggTradeEvent.getEventTime() < lastTradeSentTimestamp) {
				return;
			}

			Trade tradeToNotify = new Trade();
			tradeToNotify.setInstrument(instrument.getPrimaryKey());
			//			tradeToNotify.setTimestamp(System.currentTimeMillis());
			tradeToNotify.setTimestamp(aggTradeEvent.getEventTime());
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

	private synchronized void onBinanceDepthUpdate(Instrument instrument, DepthEvent depthEvent) {
		//change from depthEvent to Depth and notify
		try {
			Long lastDepthSentTimestamp = lastDepthSent.getOrDefault(instrument, 0L);
			if (CHECK_SEND_TIMESTAMP && depthEvent.getEventTime() < lastDepthSentTimestamp) {
				return;
			}
			Depth depth = new Depth();
			depth.setInstrument(instrument.getPrimaryKey());
			depth.setTimestamp(depthEvent.getEventTime());
			//			depth.setTimestamp(System.currentTimeMillis());
			boolean anyError = false;
			int askDepth = Math.min(depthEvent.getAsks().size(), MAX_DEPTH);
			Double asks[] = new Double[askDepth];
			Double askQtys[] = new Double[askDepth];
			int indexAsk = 0;
			try {
				for (OrderBookEntry askEntry : depthEvent.getAsks()) {
					if (indexAsk >= MAX_DEPTH) {
						break;
					}
					double qty = BinanceBrokerConnector.NUMBER_FORMAT.parse(askEntry.getQty().toUpperCase())
							.doubleValue();
					if (qty == 0) {
						continue;
					}
					asks[indexAsk] = BinanceBrokerConnector.NUMBER_FORMAT.parse(askEntry.getPrice().toUpperCase())
							.doubleValue();
					askQtys[indexAsk] = qty;
					indexAsk++;
				}
			} catch (Exception e) {
				logger.error("Error parsing ask depth event -> no more levels than {}", indexAsk, e);
				anyError = true;
			}

			if (indexAsk < askQtys.length) {
				askQtys = ArrayUtils.subarray(askQtys, 0, indexAsk);
				asks = ArrayUtils.subarray(asks, 0, indexAsk);
			}

			int bidDepth = Math.min(depthEvent.getBids().size(), MAX_DEPTH);
			Double bids[] = new Double[bidDepth];
			Double bidQtys[] = new Double[bidDepth];
			int indexBid = 0;
			try {
				for (OrderBookEntry bidEntry : depthEvent.getBids()) {
					if (indexBid >= MAX_DEPTH) {
						break;
					}
					double qty = BinanceBrokerConnector.NUMBER_FORMAT.parse(bidEntry.getQty().toUpperCase())
							.doubleValue();
					if (qty == 0) {
						continue;
					}

					bids[indexBid] = BinanceBrokerConnector.NUMBER_FORMAT.parse(bidEntry.getPrice().toUpperCase())
							.doubleValue();
					bidQtys[indexBid] = qty;
					indexBid++;
				}
			} catch (Exception e) {
				logger.error("Error parsing bid depth event  -> no more levels than {}", indexBid, e);
				anyError = true;
			}
			if (indexBid < bidQtys.length) {
				bidQtys = ArrayUtils.subarray(bidQtys, 0, indexBid);
				bids = ArrayUtils.subarray(bids, 0, indexBid);
			}


			if (anyError && (indexAsk < MIN_VALID_DEPTH || indexBid < MIN_VALID_DEPTH)) {
				logger.error("Depth is very small with errors bid_depth:{}  ask_depth:{} ", indexBid, indexAsk);
				return;
			}

			depth.setAsks(asks);
			depth.setAsksQuantities(askQtys);
			depth.setBids(bids);
			depth.setBidsQuantities(bidQtys);

			notifyDepth(instrument.getPrimaryKey(), depth);
			lastDepthSent.put(instrument, depthEvent.getEventTime());
		} catch (Exception ex) {
			logger.error("Error onDepthUpdate {} ", instrument, ex);
		}
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
