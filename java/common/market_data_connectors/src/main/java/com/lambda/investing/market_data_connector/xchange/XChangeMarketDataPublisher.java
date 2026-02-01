package com.lambda.investing.market_data_connector.xchange;

import com.lambda.investing.Configuration;
import com.lambda.investing.LatencyStatistics;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.ThreadUtils;
import com.lambda.investing.market_data_connector.AbstractMarketDataConnectorPublisher;
import com.lambda.investing.market_data_connector.MarketDataConfiguration;
import com.lambda.investing.Statistics;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.xchange.*;
import info.bitrich.xchangestream.core.StreamingExchange;
import io.reactivex.rxjava3.disposables.Disposable;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * https://github.com/binance-exchange/binance-java-api
 */
public class XChangeMarketDataPublisher extends AbstractMarketDataConnectorPublisher implements Runnable {

	private static boolean CHECK_SEND_TIMESTAMP = false;//send all to persist it!
	private static long CHECK_LATENCY_MS = 500;
	private static int MIN_VALID_DEPTH = 2;
	private static int MAX_DEPTH = Depth.MAX_DEPTH;
	protected Logger logger = LogManager.getLogger(XChangeMarketDataPublisher.class);

	protected XChangeBrokerConnector brokerConnector;
	protected List<Instrument> instrumentList;
	protected MarketDataConfiguration marketDataConfiguration;
	protected Map<String, Instrument> symbolToInstrument;
	protected Map<Instrument, Long> lastDepthSent;
	protected Map<Instrument, Long> lastTradeSent;

	private AtomicLong counterReceives = new AtomicLong();
	private Long lastTimestampReceived = 0L;
	private Thread threadCheckConnection;

	protected List<Disposable> subscriptionTrades = new ArrayList<>();
	protected List<Disposable> subscriptionDepths = new ArrayList<>();
	protected StreamingExchange webSocketClient;
	protected LatencyStatistics latencyStatistics;

	public XChangeMarketDataPublisher(ConnectorConfiguration connectorConfiguration,
			ConnectorPublisher connectorPublisher, MarketDataConfiguration marketDataConfiguration,
			List<Instrument> instrumentList) {
		super(connectorConfiguration, connectorPublisher);
		this.marketDataConfiguration = marketDataConfiguration;
		this.instrumentList = instrumentList;
		symbolToInstrument = new ConcurrentHashMap<>();
		lastDepthSent = new ConcurrentHashMap<>();
		lastTradeSent = new ConcurrentHashMap<>();
		latencyStatistics = new LatencyStatistics("XChangeMarketDataPublisherLatency", 60 * 1000);

		setBrokerConnector();
	}

	public XChangeMarketDataPublisher(String name, ConnectorConfiguration connectorConfiguration,
			ConnectorPublisher connectorPublisher, MarketDataConfiguration marketDataConfiguration,
			List<Instrument> instrumentList) {
		super(name, connectorConfiguration, connectorPublisher);
		this.marketDataConfiguration = marketDataConfiguration;
		this.instrumentList = instrumentList;
		symbolToInstrument = new ConcurrentHashMap<>();
		lastDepthSent = new ConcurrentHashMap<>();
		lastTradeSent = new ConcurrentHashMap<>();
		statistics = new Statistics("XChangeMarketDataPublisher", 60000);//useful to see if we are receiving data
		latencyStatistics = new LatencyStatistics("XChangeMarketDataPublisherLatency", 60 * 1000);
		setBrokerConnector();


	}

	public void setBrokerConnector() {
		if (marketDataConfiguration instanceof CoinbaseMarketDataConfiguration) {
			CoinbaseMarketDataConfiguration coinbaseMarketDataConfiguration = (CoinbaseMarketDataConfiguration) marketDataConfiguration;
			this.brokerConnector = CoinbaseBrokerConnector.getInstance(coinbaseMarketDataConfiguration.getApiKey(),
					coinbaseMarketDataConfiguration.getSecretKey());


		} else if (marketDataConfiguration instanceof KrakenMarketDataConfiguration) {
			KrakenMarketDataConfiguration krakenMarketDataConfiguration = (KrakenMarketDataConfiguration) marketDataConfiguration;
			this.brokerConnector = KrakenBrokerConnector.getInstance(krakenMarketDataConfiguration.getApiKey(),
					krakenMarketDataConfiguration.getSecretKey());

		} else if (marketDataConfiguration instanceof BinanceXchangeMarketDataConfiguration) {
			BinanceXchangeMarketDataConfiguration binanceMarketDataConfiguration = (BinanceXchangeMarketDataConfiguration) marketDataConfiguration;
			this.brokerConnector = BinanceXchangeBrokerConnector.getInstance(binanceMarketDataConfiguration.getApiKey(),
					binanceMarketDataConfiguration.getSecretKey());

		} else if (marketDataConfiguration instanceof BybitMarketDataConfiguration) {
			BybitMarketDataConfiguration bybitMarketDataConfiguration = (BybitMarketDataConfiguration) marketDataConfiguration;
			this.brokerConnector = BybitBrokerConnector.getInstance(bybitMarketDataConfiguration.getApiKey(),
					bybitMarketDataConfiguration.getSecretKey());

		} else {
			logger.error("trying to construct setBrokerConnector with a not recognized marketDataConfiguration {}",
					marketDataConfiguration);
		}
	}

	protected synchronized void onTradeResponse(Instrument instrument, org.knowm.xchange.dto.marketdata.Trade trade) {

		try {
			Long lastTradeSentTimestamp = lastTradeSent.getOrDefault(instrument, 0L);
			Date currentDate = trade.getTimestamp();
			if (currentDate == null) {
				return;
			}

			if (CHECK_SEND_TIMESTAMP && currentDate.getTime() < lastTradeSentTimestamp) {
				return;
			}
			long latency = new Date().getTime() - currentDate.getTime();
			latencyStatistics.addLatencyStatistics("trade." + instrument.getPrimaryKey(), latency);

			Trade tradeToNotify = Trade.getInstance();
			tradeToNotify.setInstrument(instrument.getPrimaryKey());
			//			tradeToNotify.setTimestamp(System.currentTimeMillis());
			tradeToNotify.setTimestamp(currentDate.getTime());
			try {
				tradeToNotify.setPrice(trade.getPrice().doubleValue());
				tradeToNotify.setQuantity(trade.getOriginalAmount().doubleValue());
			} catch (Exception e) {
				logger.error("Error parsing trade event {} ", instrument, e);
				return;
			}

			notifyTrade(instrument.getPrimaryKey(), tradeToNotify);
			lastTradeSent.put(instrument, currentDate.getTime());
		} catch (Exception e) {
			logger.error("Error onTradeResponse {}", instrument, e);
		}

	}

	protected void onDepthResponse(Instrument instrument, org.knowm.xchange.dto.marketdata.OrderBook orderbook) {

		//		logger.info("depth from {} received {} {}", instrument.toString(), orderbook.getAsks().get(0),
		//				orderbook.getBids().get(0));

		try {
			Long lastDepthSentTimestamp = lastDepthSent.getOrDefault(instrument, 0L);
			Date currentDate = orderbook.getTimeStamp();
			if (currentDate == null) {
				return;
			}

			long latency = new Date().getTime() - currentDate.getTime();
			latencyStatistics.addLatencyStatistics("depth." + instrument.getPrimaryKey(), latency);

			if (CHECK_SEND_TIMESTAMP && currentDate.getTime() < lastDepthSentTimestamp) {
				return;
			}
			Depth depth = Depth.getInstancePool();
			depth.setInstrument(instrument.getPrimaryKey());
			depth.setTimestamp(currentDate.getTime());
			//			depth.setTimestamp(System.currentTimeMillis());
			boolean anyError = false;
			int askDepth = Math.min(orderbook.getAsks().size(), MAX_DEPTH);
            double asks[] = new double[askDepth];
            double askQtys[] = new double[askDepth];
			int indexAsk = 0;
			try {
				for (LimitOrder askEntry : orderbook.getAsks()) {
					if (indexAsk >= MAX_DEPTH) {
						break;
					}
					double qty = askEntry.getRemainingAmount().doubleValue();
					if (qty == 0) {
						continue;
					}
					asks[indexAsk] = askEntry.getLimitPrice().doubleValue();
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

			int bidDepth = Math.min(orderbook.getBids().size(), MAX_DEPTH);
            double bids[] = new double[bidDepth];
            double bidQtys[] = new double[bidDepth];
			int indexBid = 0;
			try {
				for (LimitOrder bidEntry : orderbook.getBids()) {
					if (indexBid >= MAX_DEPTH) {
						break;
					}
					double qty = bidEntry.getRemainingAmount().doubleValue();
					if (qty == 0) {
						continue;
					}

					bids[indexBid] = bidEntry.getLimitPrice().doubleValue();
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
			lastDepthSent.put(instrument, currentDate.getTime());
		} catch (Exception ex) {
			logger.error("Error onDepthUpdate {} ", instrument, ex);
		}

	}

	protected void subscribeMarketData() {
		//connect to websocket depth trade updates
		if (this.brokerConnector != null) {
			this.webSocketClient = this.brokerConnector.getWebSocketClient();
		}

		for (CurrencyPair currencyPair : brokerConnector.getPairs()) {
			Instrument instrument = brokerConnector.getCurrencyPairToInstrument().get(currencyPair);
			try {
				Disposable subscriptionTrade = webSocketClient.getStreamingMarketDataService().getTrades(currencyPair)
						.subscribe(trade -> onTradeResponse(instrument, trade),
								throwable -> logger.error("Error in trade subscription", throwable));

				subscriptionTrades.add(subscriptionTrade);
			} catch (Exception e) {
				logger.error("error subscribing to trades on {} ", instrument, e);
			}

		}

		//register to depth
		for (CurrencyPair currencyPair : brokerConnector.getPairs()) {
			Instrument instrument = brokerConnector.getCurrencyPairToInstrument().get(currencyPair);
			try {
				Disposable subscriptionDepth = webSocketClient.getStreamingMarketDataService()
						.getOrderBook(currencyPair).subscribe(orderBook -> onDepthResponse(instrument, orderBook),
								throwable -> logger.error("Error in depth subscription", throwable));

				subscriptionDepths.add(subscriptionDepth);
			} catch (Exception e) {
				logger.error("error subscribing to depth on {} ", instrument, e);
			}

		}

	}

	@Override public void init() {

	}

	@Override public void start() {
		threadCheckConnection = new Thread(this, "threadCheckConnection");
		super.start();
		brokerConnector.connectWebsocket(instrumentList);
		subscribeMarketData();
		threadCheckConnection.start();
	}

	protected void reset() {
		for (Disposable disposable : subscriptionTrades) {
			disposable.dispose();
		}
		for (Disposable disposable : subscriptionDepths) {
			disposable.dispose();
		}
		this.brokerConnector.resetClient();
		subscribeMarketData();
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
				reset();
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
