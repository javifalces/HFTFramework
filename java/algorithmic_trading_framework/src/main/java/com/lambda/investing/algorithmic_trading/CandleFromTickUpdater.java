package com.lambda.investing.algorithmic_trading;


import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.StateManager;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CandleFromTickUpdater {

	protected static double DEFAULT_MAX_PRICE = -9E9;
	protected static double DEFAULT_MIN_PRICE = 9E9;
	protected static double DEFAULT_MAX_VOLUME = -9E9;
	protected static double DEFAULT_MIN_VOLUME = 9E9;

	public static double VOLUME_THRESHOLD_DEFAULT = 200E6;//200M
	public static int SECONDS_THRESHOLD_DEFAULT = 56;//to be faster than the rest
	protected List<CandleListener> observers;
	protected Map<String, CandleFromTickUpdaterInstrument> instrumentPkToTickCreator;
	protected double volumeThreshold = VOLUME_THRESHOLD_DEFAULT;

	protected int secondsThreshold = SECONDS_THRESHOLD_DEFAULT;
	protected static Logger logger = LogManager.getLogger(CandleFromTickUpdater.class);

	public CandleFromTickUpdater() {
		observers = new ArrayList<>();
		instrumentPkToTickCreator = new ConcurrentHashMap();
		logger = LogManager.getLogger(CandleFromTickUpdater.class);
	}

	public void setVolumeThreshold(double volumeThreshold) {
		logger.info("set volumeThreshold candles={} ", volumeThreshold);
		this.volumeThreshold = volumeThreshold;
	}

	public void setSecondsThreshold(int secondsThreshold) {
		logger.info("set setSecondsThreshold time={} ", secondsThreshold);
		this.secondsThreshold = secondsThreshold;
	}

	public double getVolumeThreshold() {
		return volumeThreshold;
	}

	public int getSecondsThreshold() {
		return secondsThreshold;
	}

	public void register(CandleListener candleListener) {

		observers.add(candleListener);

		if (observers.size() > 1) {
			int initialSize = observers.size();
			//register in order -> first state Managers
			List<CandleListener> listenersOut = new ArrayList<>();
			for (CandleListener candleListener1 : observers) {
				if (candleListener1 instanceof StateManager) {
					listenersOut.add(candleListener1);
				}
			}
			//then the rest
			for (CandleListener candleListener1 : observers) {
				if (!listenersOut.contains(candleListener1)) {
					listenersOut.add(candleListener1);
				}
			}
			assert initialSize == listenersOut.size();
			this.observers = listenersOut;
		}

	}

	protected void notifyListeners(Candle candle) {
		for (CandleListener candleListener : observers) {
			candleListener.onUpdateCandle(candle);
		}
	}

	public boolean onDepthUpdate(Depth depth) {
		CandleFromTickUpdaterInstrument candleFromTickUpdaterInstrument = instrumentPkToTickCreator
				.getOrDefault(depth.getInstrument(), new CandleFromTickUpdaterInstrument(depth.getInstrument()));
		instrumentPkToTickCreator.put(depth.getInstrument(), candleFromTickUpdaterInstrument);
		return candleFromTickUpdaterInstrument.onDepthUpdate(depth);
	}

	public boolean onTradeUpdate(Trade trade) {
		CandleFromTickUpdaterInstrument candleFromTickUpdaterInstrument = instrumentPkToTickCreator
				.getOrDefault(trade.getInstrument(), new CandleFromTickUpdaterInstrument(trade.getInstrument()));
		instrumentPkToTickCreator.put(trade.getInstrument(), candleFromTickUpdaterInstrument);
		return candleFromTickUpdaterInstrument.onTradeUpdate(trade);
	}

	private class CandleFromTickUpdaterInstrument {

		private Date lastTimestampMinuteTradeCandle = null;
		private double maxPriceMinuteTrade = DEFAULT_MAX_PRICE;
		private double minPriceMinuteTrade = DEFAULT_MIN_PRICE;
		private double openPriceMinuteTrade = -1.;

		private Date lastTimestampHourTradeCandle = null;
		private double maxPriceHourTrade = DEFAULT_MAX_PRICE;
		private double minPriceHourTrade = DEFAULT_MIN_PRICE;
		private double openPriceHourTrade = -1.;

		private Date lastTimestampMinuteMidCandle = null;
		private double maxPriceMinuteMid = DEFAULT_MAX_PRICE;
		private double minPriceMinuteMid = DEFAULT_MIN_PRICE;
		private double openPriceMinuteMid = -1.;

		private double lastCumVolumeCandle = DEFAULT_MIN_PRICE;
		private double maxPriceVolumeDepthCandle = DEFAULT_MAX_PRICE;
		private double minPriceVolumeDepthCandle = DEFAULT_MIN_PRICE;
		private double maxVolumeDepthCandle = DEFAULT_MAX_VOLUME;
		private double minVolumeDepthCandle = DEFAULT_MAX_VOLUME;

		private double openPriceVolumeDepthCandle = -1.;
		private double openVolumeVolumeDepthCandle = -1.;

		private Date lastTimestampDepthCandle = null;
		private final String instrumentPk;
		private Instrument instrument;
		private Depth lastDepth = null;

		public CandleFromTickUpdaterInstrument(String instrumentPk) {
			this.instrumentPk = instrumentPk;
			this.instrument = Instrument.getInstrument(this.instrumentPk);

		}

		private void generateTradeMinuteCandle(Trade trade) {
			Date date = new Date(trade.getTimestamp());
			if (openPriceMinuteTrade == -1) {
				//first candle
				openPriceMinuteTrade = trade.getPrice();
				maxPriceMinuteTrade = trade.getPrice();
				minPriceMinuteTrade = trade.getPrice();
				lastTimestampMinuteTradeCandle = date;
				return;
			}

			maxPriceMinuteTrade = Math.max(maxPriceMinuteTrade, trade.getPrice());
			minPriceMinuteTrade = Math.min(minPriceMinuteTrade, trade.getPrice());
			assert maxPriceMinuteTrade >= minPriceMinuteTrade;
			assert maxPriceMinuteTrade >= openPriceMinuteTrade;
			assert maxPriceMinuteTrade >= trade.getPrice();
			assert minPriceMinuteTrade <= openPriceMinuteTrade;
			assert minPriceMinuteTrade <= trade.getPrice();

			Candle candle = new Candle(CandleType.time_1_min, instrumentPk, openPriceMinuteTrade, maxPriceMinuteTrade,
					minPriceMinuteTrade, trade.getPrice());

			notifyListeners(candle);
			//		algorithmToNotify.onUpdateCandle(candle);
			lastTimestampMinuteTradeCandle = date;
			openPriceMinuteTrade = trade.getPrice();
			maxPriceMinuteTrade = trade.getPrice();
			minPriceMinuteTrade = trade.getPrice();
		}

		private void generateMidSecondsCandle(Depth depth) {

			Date date = new Date(depth.getTimestamp());
			if (openPriceMinuteMid == -1) {
				//first candle
				openPriceMinuteMid = depth.getMidPrice();
				maxPriceMinuteMid = depth.getMidPrice();
				minPriceMinuteMid = depth.getMidPrice();
				lastTimestampMinuteMidCandle = date;
				return;
			}

			maxPriceMinuteMid = Math.max(maxPriceMinuteMid, depth.getMidPrice());
			minPriceMinuteMid = Math.min(minPriceMinuteMid, depth.getMidPrice());
			assert maxPriceMinuteMid >= minPriceMinuteMid;
			assert maxPriceMinuteMid >= openPriceMinuteMid;
			assert maxPriceMinuteMid >= depth.getMidPrice();
			assert minPriceMinuteMid <= openPriceMinuteMid;
			assert minPriceMinuteMid <= depth.getMidPrice();

			Candle candle = new Candle(CandleType.mid_time_seconds_threshold, instrumentPk, openPriceMinuteMid,
					maxPriceMinuteMid, minPriceMinuteMid, depth.getMidPrice());

			//		algorithmToNotify.onUpdateCandle(candle);
			notifyListeners(candle);
			lastTimestampMinuteMidCandle = date;
			openPriceMinuteMid = depth.getMidPrice();
			maxPriceMinuteMid = depth.getMidPrice();
			minPriceMinuteMid = depth.getMidPrice();
		}

		private void generateDepthVolumeCandle(Depth depth) {

			Date date = new Date(depth.getTimestamp());
			if (openPriceVolumeDepthCandle == -1) {
				//first candle
				openPriceVolumeDepthCandle = depth.getMidPrice();
				maxPriceVolumeDepthCandle = depth.getMidPrice();
				minPriceVolumeDepthCandle = depth.getMidPrice();

				openVolumeVolumeDepthCandle = depth.getTotalVolume();
				maxVolumeDepthCandle = depth.getTotalVolume();
				minVolumeDepthCandle = depth.getTotalVolume();

				lastCumVolumeCandle = 0;//restart
				return;
			}

			maxPriceVolumeDepthCandle = Math.max(maxPriceVolumeDepthCandle, depth.getMidPrice());
			minPriceVolumeDepthCandle = Math.min(minPriceVolumeDepthCandle, depth.getMidPrice());

			maxVolumeDepthCandle = Math.max(maxVolumeDepthCandle, depth.getTotalVolume());
			minVolumeDepthCandle = Math.max(minVolumeDepthCandle, depth.getTotalVolume());

			assert maxPriceVolumeDepthCandle >= minPriceVolumeDepthCandle;
			assert maxPriceVolumeDepthCandle >= openPriceVolumeDepthCandle;
			assert maxPriceVolumeDepthCandle >= depth.getMidPrice();
			assert maxPriceVolumeDepthCandle <= openPriceVolumeDepthCandle;
			assert maxPriceVolumeDepthCandle <= depth.getMidPrice();

			assert maxVolumeDepthCandle >= depth.getTotalVolume();
			assert maxVolumeDepthCandle >= minVolumeDepthCandle;
			assert maxVolumeDepthCandle >= openVolumeVolumeDepthCandle;
			assert minVolumeDepthCandle <= depth.getTotalVolume();
			assert minVolumeDepthCandle <= openVolumeVolumeDepthCandle;

			Candle candle = new Candle(CandleType.volume_threshold_depth, instrumentPk, openPriceVolumeDepthCandle,
					maxPriceVolumeDepthCandle, minPriceVolumeDepthCandle, depth.getMidPrice(), maxVolumeDepthCandle,
					minVolumeDepthCandle, openVolumeVolumeDepthCandle, depth.getTotalVolume());

			//		algorithmToNotify.onUpdateCandle(candle);
			notifyListeners(candle);
			lastCumVolumeCandle = 0;
			openVolumeVolumeDepthCandle = depth.getTotalVolume();
			maxVolumeDepthCandle = depth.getTotalVolume();
			minVolumeDepthCandle = depth.getTotalVolume();

			openPriceVolumeDepthCandle = depth.getMidPrice();
			maxPriceVolumeDepthCandle = depth.getMidPrice();
			minPriceVolumeDepthCandle = depth.getMidPrice();
		}

		private void generateTradeHourCandle(Trade trade) {

			Date date = new Date(trade.getTimestamp());
			if (openPriceHourTrade == -1) {
				//first candle
				openPriceHourTrade = trade.getPrice();
				maxPriceHourTrade = trade.getPrice();
				minPriceHourTrade = trade.getPrice();
				lastTimestampHourTradeCandle = date;
				return;
			}

			maxPriceHourTrade = Math.max(maxPriceHourTrade, trade.getPrice());
			minPriceHourTrade = Math.min(minPriceHourTrade, trade.getPrice());
			assert maxPriceHourTrade >= minPriceHourTrade;
			assert maxPriceHourTrade >= openPriceHourTrade;
			assert maxPriceHourTrade >= trade.getPrice();
			assert minPriceHourTrade <= openPriceHourTrade;
			assert minPriceHourTrade <= trade.getPrice();

			Candle candle = new Candle(CandleType.time_1_hour, instrumentPk, openPriceHourTrade, maxPriceHourTrade,
					minPriceHourTrade, trade.getPrice());

			//		algorithmToNotify.onUpdateCandle(candle);
			notifyListeners(candle);
			lastTimestampHourTradeCandle = date;
			openPriceHourTrade = trade.getPrice();
			maxPriceHourTrade = trade.getPrice();
			minPriceHourTrade = trade.getPrice();
		}

		public boolean onDepthUpdate(Depth depth) {
			//minute candle
			if (!depth.getInstrument().equals(instrumentPk)) {
				return false;
			}

			Date date = new Date(depth.getTimestamp());
			long diffSeconds = 500;
			if (lastTimestampMinuteMidCandle != null) {
				long diffInMillies = Math.abs(date.getTime() - lastTimestampMinuteMidCandle.getTime());
				diffSeconds = diffInMillies / 1000;
			}

			if (lastTimestampMinuteMidCandle == null || diffSeconds >= secondsThreshold) {//to be earlier than the rest
				generateMidSecondsCandle(depth);
			} else {
				try {
					maxPriceMinuteMid = Math.max(maxPriceMinuteMid, depth.getMidPrice());
					minPriceMinuteMid = Math.min(minPriceMinuteMid, depth.getMidPrice());
				} catch (IndexOutOfBoundsException e) {

				}
			}

			//// volume candle
			if (lastCumVolumeCandle == DEFAULT_MIN_PRICE) {
				//initial update
				generateDepthVolumeCandle(depth);
			}

			if (instrument.isFX()) {
				double totalVolume = depth.getBestAskQty() + depth.getBestBidQty();
				lastCumVolumeCandle += totalVolume;
				if (lastCumVolumeCandle > volumeThreshold) {
					generateDepthVolumeCandle(depth);
					lastCumVolumeCandle = 0;//just in case
				}
			}

			lastDepth = depth;
			return true;
		}

		public boolean onTradeUpdate(Trade trade) {
			if (!trade.getInstrument().equals(instrumentPk)) {
				return false;
			}
			Date date = new Date(trade.getTimestamp());

			//minute candle
			if (lastTimestampMinuteTradeCandle == null || date.getMinutes() != lastTimestampMinuteTradeCandle
					.getMinutes()) {
				generateTradeMinuteCandle(trade);
			} else {
				maxPriceMinuteTrade = Math.max(maxPriceMinuteTrade, trade.getPrice());
				minPriceMinuteTrade = Math.min(minPriceMinuteTrade, trade.getPrice());
			}

			//hour candle
			if (lastTimestampHourTradeCandle == null || date.getHours() != lastTimestampHourTradeCandle.getHours()) {
				generateTradeHourCandle(trade);
			} else {
				maxPriceHourTrade = Math.max(maxPriceHourTrade, trade.getPrice());
				minPriceHourTrade = Math.min(minPriceHourTrade, trade.getPrice());
			}

			//volume candle

			boolean weCreatedFirstDepth = lastCumVolumeCandle != DEFAULT_MIN_PRICE;
			if (!instrument.isFX() && weCreatedFirstDepth) {
				double totalVolume = trade.getQuantity();
				if (totalVolume <= 0) {
					logger.warn("ignored trade volume {}<=0  {}", totalVolume, trade);
				} else {
					lastCumVolumeCandle += totalVolume;
					if (lastCumVolumeCandle > volumeThreshold) {

						generateDepthVolumeCandle(lastDepth);
						lastCumVolumeCandle = 0;//just in case
					}
				}
			}

			return true;
		}
	}
}
