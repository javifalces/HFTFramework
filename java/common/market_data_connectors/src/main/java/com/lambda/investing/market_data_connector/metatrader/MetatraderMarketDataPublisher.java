package com.lambda.investing.market_data_connector.metatrader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.market_data_connector.AbstractMarketDataConnectorPublisher;
import com.lambda.investing.metatrader.MetatraderZeroBrokerConnector;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Map;

public class MetatraderMarketDataPublisher extends AbstractMarketDataConnectorPublisher implements ConnectorListener {

	MetatraderZeroBrokerConnector metatraderZeroBrokerConnector;
	public static Gson GSON_STRING = new GsonBuilder()
			.excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.FINAL)
			.serializeSpecialFloatingPointValues().create();

	private String broker;
	private boolean depthReceived = false;//if broker doesnt have it

	public MetatraderMarketDataPublisher(ConnectorConfiguration connectorConfiguration,
			ConnectorPublisher connectorPublisher, MetatraderZeroBrokerConnector metatraderZeroBrokerConnector) {

		super(connectorConfiguration, connectorPublisher);
		this.metatraderZeroBrokerConnector = metatraderZeroBrokerConnector;

		ZeroMqConfiguration configuration = new ZeroMqConfiguration();
		configuration.setPort(this.metatraderZeroBrokerConnector.getPortPublisher());

	}

	public String getBroker() {
		return broker;
	}

	public void setBroker(String broker) {
		this.broker = broker;
	}

	@Override public void init() {
		this.metatraderZeroBrokerConnector.getPublisherProvider().start(false, false);
		this.metatraderZeroBrokerConnector.getPublisherProvider()
				.register(this.metatraderZeroBrokerConnector.getPublisherZeroMqConfiguration(), this);
	}

	//receiving Metatrader messages
	@Override public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
			TypeMessage typeMessage, String content) {
		//
		assert typeMessage == null;
		Map<String, Object> jsonReceived = null;
		try {
			jsonReceived = GSON_STRING.fromJson(content, new TypeToken<Map<String, Object>>() {

			}.getType());
		} catch (Exception e) {
			logger.error("cant parse message {}", content, e);
			return;
		}
		String type = (String) jsonReceived.get("type");
		String symbol = (String) jsonReceived.get("symbol");
		String primaryKey = symbol.toLowerCase() + "_metatrader";
		if (broker != null) {
			primaryKey = symbol.toLowerCase() + "_" + broker.toLowerCase();
		}
		Instrument instrument = Instrument.getInstrument(primaryKey);

		if (instrument == null) {
			logger.warn("instrument not found for symbol {}", symbol);
			return;
		}
		Double timestampD = (Double) jsonReceived.get("Time");
		long timestamp = timestampD.longValue() * 1000;
		Date date = new Date(timestamp);
		int maxLevelPermitted = 5;
		if (type.equalsIgnoreCase("DEPTH")) {
			//{type:DEPTH,symbol:EURCAD, Time:1613420042,ASK_PRICE_0: 1.53317,ASK_QTY_0: 1000000.0,BID_PRICE_1: 1.53298,BID_QTY_1: 100000.0,}
			depthReceived = true;
			Double[] bidQty = new Double[maxLevelPermitted];
			Double[] askQty = new Double[maxLevelPermitted];
			Double[] bidPrice = new Double[maxLevelPermitted];
			Double[] askPrice = new Double[maxLevelPermitted];
			int maxLevel = 0;
			for (String key : jsonReceived.keySet()) {
				if (!key.contains("_")) {
					continue;
				}

				String[] keySplit = key.split("_");
				if (keySplit.length != 3) {
					continue;
				}
				String side = keySplit[0];
				String typeData = keySplit[1];
				int level = Integer.valueOf(keySplit[2]);
				if (level > maxLevelPermitted - 1) {
					continue;
				}
				Double[] list = bidQty;

				if (side.equalsIgnoreCase("ask")) {
					if (typeData.equalsIgnoreCase("price")) {
						list = askPrice;
					}
					if (typeData.equalsIgnoreCase("qty")) {
						list = askQty;
					}
				}

				if (side.equalsIgnoreCase("bid")) {
					if (typeData.equalsIgnoreCase("price")) {
						list = bidPrice;
					}
					if (typeData.equalsIgnoreCase("qty")) {
						list = bidQty;
					}
				}
				maxLevel = Math.max(maxLevel, level);
				list[level] = (double) jsonReceived.get(key);

			}

			//setting object
			Depth depth = new Depth();
			depth.setBidsQuantities(ArrayUtils.subarray(bidQty, 0, maxLevel + 1));
			depth.setAsksQuantities(ArrayUtils.subarray(askQty, 0, maxLevel + 1));
			depth.setAsks(ArrayUtils.subarray(askPrice, 0, maxLevel + 1));
			depth.setBids(ArrayUtils.subarray(bidPrice, 0, maxLevel + 1));
			depth.setTimestamp(timestamp);
			depth.setInstrument(instrument.getPrimaryKey());
			depth.setLevelsFromData();

			notifyDepth(instrument.getPrimaryKey(), depth);

		} else if (type.equalsIgnoreCase("TICK")) {
			//{type:TICK,symbol:EURJPY, Time:1613418097,timestamp:1613425297514,last:0.000,last_volume:0.000}
			Trade trade = new Trade();
			trade.setInstrument(instrument.getPrimaryKey());
			trade.setTimestamp(timestamp);
			double price = (double) jsonReceived.get("last");
			double quantity = (double) jsonReceived.get("last_volume");
			if (price != 0 && quantity != 0) {
				trade.setQuantity(quantity);
				trade.setPrice(price);
				notifyTrade(instrument.getPrimaryKey(), trade);
			}

			//get tick
			if (!depthReceived) {
				Depth depth = new Depth();
				double bestBid = (double) jsonReceived.get("best_bid");
				double bestAsk = (double) jsonReceived.get("best_ask");
				if (bestAsk != 0 && bestBid != 0) {
					double bestQty = 0.0;//not coming from darwinex
					Double[] bids = new Double[] { bestBid };
					Double[] asks = new Double[] { bestAsk };
					Double[] quantities = new Double[] { bestQty };
					depth.setBids(bids);
					depth.setAsks(asks);

					///review this volume_real what it its and darwinex data has 2 values
					depth.setBidsQuantities(quantities);
					depth.setAsksQuantities(quantities);
					//

					depth.setTimestamp(timestamp);
					depth.setInstrument(instrument.getPrimaryKey());
					depth.setLevelsFromData();

					notifyDepth(instrument.getPrimaryKey(), depth);
				}
			}

		}

	}
}
