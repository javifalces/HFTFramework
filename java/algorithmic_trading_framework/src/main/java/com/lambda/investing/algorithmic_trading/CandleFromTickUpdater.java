package com.lambda.investing.algorithmic_trading;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CandleFromTickUpdater {

	protected static double DEFAULT_MAX_PRICE = -9E9;
	protected static double DEFAULT_MIN_PRICE = 9E9;
	public static double VOLUME_TRESHOLD_DEFAULT = 200E6;//200M
	protected List<CandleListener> observers;
	protected Map<String, CandleFromTickUpdaterInstrument> instrumentPkToTickCreator;
	protected double volumeTreshold = VOLUME_TRESHOLD_DEFAULT;

	public CandleFromTickUpdater() {
		observers = new ArrayList<>();
		instrumentPkToTickCreator = new ConcurrentHashMap();
	}

	public void setVolumeTreshold(double volumeTreshold) {
		this.volumeTreshold = volumeTreshold;
	}

	public void register(CandleListener candleListener) {
		observers.add(candleListener);
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

		private double lastCumVolumeDepthCandle = DEFAULT_MIN_PRICE;
		private double maxPriceVolumeDepthCandle = DEFAULT_MAX_PRICE;
		private double minPriceVolumeDepthCandle = DEFAULT_MIN_PRICE;
		private double openPriceVolumeDepthCandle = -1.;

		private Date lastTimestampDepthCandle = null;
		private final String instrumentPk;

		public CandleFromTickUpdaterInstrument(String instrumentPk) {
			this.instrumentPk = instrumentPk;

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

		private void generateMidMinuteCandle(Depth depth) {

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

			Candle candle = new Candle(CandleType.mid_time_1_min, instrumentPk, openPriceMinuteMid, maxPriceMinuteMid,
					minPriceMinuteMid, depth.getMidPrice());

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
				lastCumVolumeDepthCandle = 0;//restart
				return;
			}

			maxPriceVolumeDepthCandle = Math.max(maxPriceVolumeDepthCandle, depth.getMidPrice());
			minPriceVolumeDepthCandle = Math.min(minPriceVolumeDepthCandle, depth.getMidPrice());
			assert maxPriceVolumeDepthCandle >= minPriceVolumeDepthCandle;
			assert maxPriceVolumeDepthCandle >= openPriceVolumeDepthCandle;
			assert maxPriceVolumeDepthCandle >= depth.getMidPrice();
			assert maxPriceVolumeDepthCandle <= openPriceVolumeDepthCandle;
			assert maxPriceVolumeDepthCandle <= depth.getMidPrice();

			Candle candle = new Candle(CandleType.volume_treshold_depth, instrumentPk, openPriceVolumeDepthCandle,
					maxPriceVolumeDepthCandle, minPriceVolumeDepthCandle, depth.getMidPrice());

			//		algorithmToNotify.onUpdateCandle(candle);
			notifyListeners(candle);
			lastCumVolumeDepthCandle = 0;
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

			if (lastTimestampMinuteMidCandle == null || diffSeconds >= 56) {//to be earlier than the rest
				generateMidMinuteCandle(depth);
			} else {
				try {
					maxPriceMinuteMid = Math.max(maxPriceMinuteMid, depth.getMidPrice());
					minPriceMinuteMid = Math.min(minPriceMinuteMid, depth.getMidPrice());
				} catch (IndexOutOfBoundsException e) {

				}
			}

			//// volume candle
			if (lastCumVolumeDepthCandle == DEFAULT_MIN_PRICE) {
				//initial update
				generateDepthVolumeCandle(depth);
			}

			double totalVolume = depth.getBestAskQty() + depth.getBestBidQty();
			lastCumVolumeDepthCandle += totalVolume;
			if (lastCumVolumeDepthCandle > volumeTreshold) {
				generateDepthVolumeCandle(depth);
				lastCumVolumeDepthCandle = 0;//just in case
			}

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

			return true;
		}
	}
}
