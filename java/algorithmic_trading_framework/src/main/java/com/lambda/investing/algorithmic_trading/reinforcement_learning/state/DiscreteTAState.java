package com.lambda.investing.algorithmic_trading.reinforcement_learning.state;

import com.google.common.primitives.Doubles;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.TimeService;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreEnum;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.Verb;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.curator.shaded.com.google.common.collect.Queues;

import java.lang.reflect.Array;
import java.util.*;

import static com.lambda.investing.algorithmic_trading.technical_indicators.Calculator.*;

public class DiscreteTAState extends AbstractState {

	//every new state must be added on python too on python_lambda/backtest/rsi_dqn.py: _get_ta_state_columns
	public static boolean BINARY_STATE_OUTPUTS = false;
	private static boolean MARKET_MIDPRICE_RELATIVE = true;
	private static String[] STATE_COLUMNS_PATTERN = new String[] {
			////price
			"microprice_",//midpricee-microprice
			"vpin_", "rsi_",//rsi<30 -> 1 rsi>70->-1 else 0
			"sma_",//sma<price -> -1 sma>=price->1
			"ema_",//sma<price -> -1 sma>=price->1
			"max_",//price>max=1 else 0
			"min_",//price>max=1 else 0
			//volume
			"volume_rsi_", "volume_sma_", "volume_ema_", "volume_max_", "volume_min_",

	};

	private static String[] MARKET_CONDITIONS_ON_MAX_PERIOD_CANDLES = new String[] {
			//queueCandles
			"signedTransactionVolume_", "signedTransaction_", "microprice_candle_", "vpin_candle_" };

	private static String[] BINARY_STATE_COLUMNS_PATTERN = new String[] {
			////price
			"microprice_",//midpricee-microprice
			"vpin_", "rsi_",//rsi<30 -> 1 rsi>70->-1 else 0
			"sma_",//sma<price -> -1 sma>=price->1
			"ema_",//sma<price -> -1 sma>=price->1
			"max_",//price>max=1 else 0
			"min_",//price>max=1 else 0

	};

	private static String[] MARKET_COLUMNS_PATTERN = new String[] {
			//last ticks properties
			"bid_price", "ask_price", "bid_qty", "ask_qty", "spread", "imbalance", "microprice"

	};
	private static String[] STATE_SINGLE_COLUMNS = new String[] { "hour_of_the_day_utc", "minutes_from_start",
			"volume_from_start" };
	private static String[] BINARY_STATE_SINGLE_COLUMNS = new String[] { "hour_of_the_day_utc" };

	private int marketHorizonSave = 15;
	protected long marketTickMs = 10;
	private long lastMarketTickSave;
	private double volumeFromStart = 0.0;

	private static int[] defaultPeriods = new int[] { 3, 5, 7, 9, 11, 13, 15, 17, 21 };///used as reference
	private static int[] binaryDefaultPeriods = new int[] { 9 };///used as reference

	private static int[] rsiPeriods = defaultPeriods;
	private static int[] smaPeriods = defaultPeriods;
	private static int[] emaPeriods = defaultPeriods;
	private static int[] maxPeriods = defaultPeriods;
	private static int[] minPeriods = defaultPeriods;

	private static int maxPeriod = defaultPeriods[defaultPeriods.length - 1];

	private CandleType candleType;

	protected ScoreEnum scoreEnumColumn;
	protected boolean isReady = false;
	protected Depth lastDepth;
	//last element will be on last element of the queue!! older is on 0
	protected Queue<Candle> queueCandles;
	protected Queue<Double> queueCandlesOpenPrices;
	protected Queue<Double> queueCandlesClosePrices;
	protected Queue<Double> queueCandlesMaxPrices;
	protected Queue<Double> queueCandlesMinPrices;

	protected Queue<Double> queueCandlesOpenVolume;
	protected Queue<Double> queueCandlesCloseVolume;
	protected Queue<Double> queueCandlesHighVolume;
	protected Queue<Double> queueCandlesLowVolume;

	protected Queue<Double> queueCandlesSignedTransactionVolume;
	protected Queue<Double> queueCandlesSignedTransaction;
	protected Queue<Double> queueCandlesMicropriceSpread;
	protected Queue<Double> queueCandlesVPINValues;

	protected Queue<Double> queueCandlesMicroPricesSpreads;
	protected Queue<Double> queueCandlesVPIN;

	private Queue<Double> bidPriceBuffer, askPriceBuffer, bidQtyBuffer, askQtyBuffer, spreadBuffer, imbalanceBuffer, micropriceBuffer;

	//	protected Queue<Double> queueCandlesRelativeBidAskQtyDiff;
	protected Instrument instrument = Instrument.getInstrument("eurusd_darwinex");

	private TimeService timeService;

	public DiscreteTAState(Instrument instrument, ScoreEnum scoreEnumColumn, CandleType candleType, int[] periods,
			int numberOfDecimals, int marketHorizonSave) {
		super(numberOfDecimals);//all integer
		this.instrument = instrument;

		this.marketHorizonSave = marketHorizonSave;
		maxNumber = -1;
		minNumber = -1;
		if (BINARY_STATE_OUTPUTS) {
			STATE_COLUMNS_PATTERN = BINARY_STATE_COLUMNS_PATTERN;
		}
		this.candleType = candleType;
		this.scoreEnumColumn = scoreEnumColumn;

		this.rsiPeriods = periods;
		this.smaPeriods = periods;
		this.emaPeriods = periods;
		this.maxPeriods = periods;
		this.minPeriods = periods;
		maxPeriod = Arrays.stream(periods).max().getAsInt();

		queueCandles = EvictingQueue.create(maxPeriod);
		timeService = new TimeService("UTC");
		queueCandlesOpenPrices = EvictingQueue.create(maxPeriod);
		queueCandlesClosePrices = EvictingQueue.create(maxPeriod);
		queueCandlesMaxPrices = EvictingQueue.create(maxPeriod);
		queueCandlesMinPrices = EvictingQueue.create(maxPeriod);
		queueCandlesMicroPricesSpreads = EvictingQueue.create(maxPeriod);
		queueCandlesVPIN = EvictingQueue.create(maxPeriod);
		//		queueCandlesRelativeBidAskQtyDiff = EvictingQueue.create(maxPeriod);
		queueCandlesOpenVolume = EvictingQueue.create(maxPeriod);
		queueCandlesCloseVolume = EvictingQueue.create(maxPeriod);
		queueCandlesHighVolume = EvictingQueue.create(maxPeriod);
		queueCandlesLowVolume = EvictingQueue.create(maxPeriod);

		queueCandlesSignedTransaction = Queues.synchronizedQueue(EvictingQueue.create(maxPeriod));
		queueCandlesSignedTransactionVolume = Queues.synchronizedQueue(EvictingQueue.create(maxPeriod));
		queueCandlesMicropriceSpread = Queues.synchronizedQueue(EvictingQueue.create(maxPeriod));
		queueCandlesVPINValues = Queues.synchronizedQueue(EvictingQueue.create(maxPeriod));
		//market

		bidPriceBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		askPriceBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		bidQtyBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		askQtyBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		spreadBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		imbalanceBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		micropriceBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));

		volumeFromStart = 0.0;

	}

	public void setMarketTickMs(long marketTickMs) {
		this.marketTickMs = marketTickMs;
	}

	@Override
	public synchronized void reset() {

		queueCandles.clear();
		queueCandlesOpenPrices.clear();
		queueCandlesClosePrices.clear();
		queueCandlesMaxPrices.clear();
		queueCandlesMinPrices.clear();

		queueCandlesOpenVolume.clear();
		queueCandlesCloseVolume.clear();
		queueCandlesHighVolume.clear();
		queueCandlesLowVolume.clear();

		queueCandlesSignedTransactionVolume.clear();
		queueCandlesSignedTransaction.clear();
		queueCandlesMicropriceSpread.clear();
		queueCandlesVPINValues.clear();

		queueCandlesMicroPricesSpreads.clear();
		queueCandlesVPIN.clear();

		bidPriceBuffer.clear();
		askPriceBuffer.clear();
		bidQtyBuffer.clear();
		askQtyBuffer.clear();
		spreadBuffer.clear();
		imbalanceBuffer.clear();
		micropriceBuffer.clear();
		lastMarketTickSave = 0;

	}

	@Override
	public void calculateNumberOfColumns() {
		setNumberOfColumns(getColumns().size());
	}

	public int getNumberOfColumns() {
		if (this.numberOfColumns == 0) {
			calculateNumberOfColumns();
		}
		return this.numberOfColumns;

	}

	public void setInstrument(Instrument instrument) {
		this.instrument = instrument;
	}

	//	public void setCurrentVerb(Verb currentVerb) {
	//		this.currentVerb = currentVerb;
	//	}

	@Override public List<String> getColumns() {
		//candles
		List<String> outputColumns = new ArrayList<>();

		for (String stateColumn : STATE_COLUMNS_PATTERN) {
			for (int period : rsiPeriods) {
				outputColumns.add(stateColumn + period);
			}
		}

		for (String stateColumn : MARKET_CONDITIONS_ON_MAX_PERIOD_CANDLES) {
			for (int i = 0; i < maxPeriod; i++) {
				outputColumns.add(stateColumn + i);
			}
		}

		//market columns
		if (marketHorizonSave > 0) {
			for (String marketColumn : MARKET_COLUMNS_PATTERN) {
				for (int lag = 0; lag < marketHorizonSave; lag++) {
					outputColumns.add(marketColumn + "_" + lag);
				}
			}
		}

		/// microprice_7,microprice_9,microprice_13,qtydiff_7,..rsi_7,rsi_9,rsi_13,sma_7,sma_9,sma_13......
		if (!BINARY_STATE_OUTPUTS) {
			//single state
			outputColumns.addAll(Arrays.asList(STATE_SINGLE_COLUMNS));
		} else {
			outputColumns.addAll(Arrays.asList(BINARY_STATE_SINGLE_COLUMNS));
		}

		return outputColumns;
	}

	@Override public int getNumberStates() {
		logger.warn("we are asking number of states to DiscreteTAState!!");
		return 6666;//2^(6)*(3^3)
	}

	@Override
	public synchronized boolean isReady() {
		return (queueCandles.size() == maxPeriod) && bidPriceBuffer.size() == marketHorizonSave;
	}

	@Override public double[] getCurrentState() {
		List<Double> candlesState = getCurrentCandlesState();
		List<Double> marketState = getCurrentMarketState();
		if (marketState == null) {
			logger.error("marketState is null!!!-> return null");
			return null;
		}
		List<Double> candleMarketState = getLastCandlesStates();

		List<Double> singleState = getSingleState();
		List<Double> outputList = new ArrayList(candlesState);
		outputList.addAll(candleMarketState);
		outputList.addAll(marketState);
		outputList.addAll(singleState);
		double[] outputArr = outputList.stream().mapToDouble(Double::doubleValue).toArray();
		return outputArr;
	}

	private List<Double> getLastCandlesStates() {
		List<Double> stateList = new ArrayList<>();
		//add Signed transaction
		stateList.addAll(queueCandlesSignedTransactionVolume);
		stateList.addAll(queueCandlesSignedTransaction);
		stateList.addAll(queueCandlesMicropriceSpread);
		stateList.addAll(queueCandlesVPINValues);
		return stateList;
	}

	private List<Double> getCurrentMarketState() {
		List<Double> stateList = new ArrayList<>();

		if (marketHorizonSave > 0) {
			try {
				stateList.addAll(bidPriceBuffer);
				stateList.addAll(askPriceBuffer);
				stateList.addAll(bidQtyBuffer);
				stateList.addAll(askQtyBuffer);
				stateList.addAll(spreadBuffer);
				stateList.addAll(imbalanceBuffer);
				stateList.addAll(micropriceBuffer);

			} catch (Exception e) {
				logger.error("error getting marketState ", e);
				stateList.clear();
			}
			if (stateList.size() == 0) {
				return null;
			}
		}
		return stateList;
		//		double[] outputArr = stateList.stream().mapToDouble(Double::doubleValue).toArray();
		//		return outputArr;
	}

	private List<Double> getSingleState() {
		List<Double> stateList = new ArrayList<>();
		String[] singleColumns = STATE_SINGLE_COLUMNS;
		if (BINARY_STATE_OUTPUTS) {
			singleColumns = BINARY_STATE_SINGLE_COLUMNS;
		}

		for (String stateColumn : singleColumns) {
			//			hour_of_the_day_utc "minutes_from_start","volume_from_start"
			//single state columns
			if (stateColumn.equalsIgnoreCase("hour_of_the_day_utc")) {
				int hour = timeService.getCurrentTimeHour();
				stateList.add((double) hour);
			}
			if (stateColumn.equalsIgnoreCase("minutes_from_start")) {
				int hourMinutes = timeService.getCurrentTimeHour() * 60;
				int minutes = timeService.getCurrentTimeMinute();
				int minutesFromStart = minutes + hourMinutes;
				stateList.add((double) minutesFromStart);
			}
			if (stateColumn.equalsIgnoreCase("volume_from_start")) {
				stateList.add((double) volumeFromStart);
			}

		}
		return stateList;

	}

	private List<Double> getCurrentCandlesState() {

		Double[] closePrices = new Double[queueCandlesClosePrices.size()];
		closePrices = queueCandlesClosePrices.toArray(closePrices);

		Double[] highPrices = new Double[queueCandlesMaxPrices.size()];
		highPrices = queueCandlesMaxPrices.toArray(highPrices);

		Double[] lowPrices = new Double[queueCandlesMinPrices.size()];
		lowPrices = queueCandlesMinPrices.toArray(lowPrices);

		Double[] microPricesSpreads = new Double[queueCandlesMicroPricesSpreads.size()];
		microPricesSpreads = queueCandlesMicroPricesSpreads.toArray(microPricesSpreads);

		Double[] vpins = new Double[queueCandlesVPIN.size()];
		vpins = queueCandlesVPIN.toArray(vpins);

		//		Double[] signedTransaction = new Double[queueCandlesSignedTransaction.size()];
		//		signedTransaction = queueCandlesSignedTransaction.toArray(signedTransaction);

		///volume states
		Double[] highVolume = new Double[queueCandlesHighVolume.size()];
		highVolume = queueCandlesHighVolume.toArray(highVolume);
		Double[] lowVolume = new Double[queueCandlesLowVolume.size()];
		lowVolume = queueCandlesLowVolume.toArray(lowVolume);
		Double[] closeVolume = new Double[queueCandlesCloseVolume.size()];
		closeVolume = queueCandlesCloseVolume.toArray(closeVolume);

		Double[] openVolume = new Double[queueCandlesOpenVolume.size()];
		openVolume = queueCandlesOpenVolume.toArray(openVolume);

		double lastMid = closePrices[closePrices.length - 1];
		double lastMidVol = closeVolume[closeVolume.length - 1];

		if (lastDepth != null) {
			lastMid = lastDepth.getMidPrice();
			closePrices[closePrices.length - 1] = lastMid;
			microPricesSpreads[microPricesSpreads.length - 1] = calculateMicropriceSpread();
			vpins[vpins.length - 1] = lastDepth.getVPIN();
		}

		/// changeSide,microprice_7,microprice_9,microprice_13,..rsi_7,rsi_9,rsi_13,sma_7,sma_9,sma_13......
		List<Double> stateList = new ArrayList<>();

		for (String stateColumn : STATE_COLUMNS_PATTERN) {
			for (int period : rsiPeriods) {
				double indicator = 0.0;
				//rsi
				if (stateColumn.startsWith("rsi")) {
					indicator = getRsiIndicator(period, closePrices);
				}

				if (stateColumn.startsWith("volume_rsi")) {
					indicator = getRsiIndicator(period, closeVolume);
				}

				//sma
				if (stateColumn.startsWith("sma")) {
					indicator = getSmaIndicator(period, closePrices);
				}
				if (stateColumn.startsWith("volume_sma")) {
					indicator = getSmaIndicator(period, closeVolume);
				}

				//ema
				if (stateColumn.startsWith("ema")) {
					indicator = getEmaIndicator(period, closePrices);
				}
				if (stateColumn.startsWith("volume_ema")) {
					indicator = getEmaIndicator(period, closeVolume);
				}

				//max
				if (stateColumn.startsWith("max")) {
					indicator = getMaxIndicator(period, highPrices, lastMid);
				}
				if (stateColumn.startsWith("volume_max")) {
					indicator = getMaxIndicator(period, highVolume, lastMidVol);
				}

				//min
				if (stateColumn.startsWith("min")) {
					indicator = getMinIndicator(period, lowPrices, lastMid);
				}
				if (stateColumn.startsWith("volume_min")) {
					indicator = getMinIndicator(period, lowVolume, lastMidVol);
				}

				//"microprice_",
				if (stateColumn.startsWith("microprice")) {
					indicator = EMACalculate(ArrayUtils.toPrimitive(microPricesSpreads), period);
					if (BINARY_STATE_OUTPUTS) {
						if (indicator < 0) {
							indicator = -1;
						} else if (indicator > 0) {
							indicator = 1;
						} else {
							indicator = 0;
						}

					}
					//					else {
					//						indicator = Math.round(indicator * 4) / 4.0;//0.25 rounded
					//						indicator = Math.round(indicator * 10) / 10.0;//one decimal
					//					}
				}

				//"imbalance_",
				if (stateColumn.startsWith("vpin")) {
					indicator = EMACalculate(ArrayUtils.toPrimitive(vpins), period);
					if (BINARY_STATE_OUTPUTS) {
						if (indicator < 0) {
							indicator = -1;
						} else if (indicator > 0) {
							indicator = 1;
						} else {
							indicator = 0;
						}

					}
					//					else {
					//						indicator = Math.round(indicator * 4) / 4.0;//0.5 rounded
					//						indicator = Math.round(indicator * 10) / 10.0;//one decimal
					//					}
				}

				stateList.add(indicator);
			}
		}

		return stateList;
		//		Double[] output = new Double[stateList.size()];
		//		output = stateList.toArray(output);
		//		return ArrayUtils.toPrimitive(output);
	}

	private double getRsiIndicator(int period, Double[] inputArr) {
		double rsiValue = RSICalculate(ArrayUtils.toPrimitive(inputArr), period);
		double indicator = Math.round(rsiValue / 10);
		//binary output
		if (BINARY_STATE_OUTPUTS) {
			//						double scaler=10.0/5.0;
			//						indicator = ((indicator - 5.0) / 2.0);
			//						indicator = Math.round(indicator * 20) / 20;

			if (indicator < 3) {
				indicator = 1;
			} else if (indicator > 7) {
				indicator = -1;
			} else {
				indicator = 0;
			}
		}
		return indicator;
	}

	private double getSmaIndicator(int period, Double[] inputArr) {
		double lastMid = inputArr[inputArr.length - 1];
		double smaValue = SMACalculate(ArrayUtils.toPrimitive(inputArr), period);
		double distanceToSma = lastMid - smaValue;
		double ticksDistance = distanceToSma / instrument.getPriceTick();//TODO take a look on volume!!
		double indicator = Math.round(ticksDistance);

		if (BINARY_STATE_OUTPUTS) {
			if (indicator < 0) {
				indicator = 1;
			} else {
				indicator = -1;
			}
		}
		return indicator;
	}

	private double getEmaIndicator(int period, Double[] inputArr) {
		double lastMid = inputArr[inputArr.length - 1];
		double emaValue = EMACalculate(ArrayUtils.toPrimitive(inputArr), period);
		double distanceToEma = lastMid - emaValue;
		double ticksDistance = distanceToEma / instrument.getPriceTick();//TODO take a look on volume!!
		double indicator = Math.round(ticksDistance);

		if (BINARY_STATE_OUTPUTS) {
			if (indicator < 0) {
				indicator = 1;
			} else {
				indicator = -1;
			}
		}
		return indicator;
	}

	private double getMaxIndicator(int period, Double[] inputArr, double lastMid) {
		double[] highPeriod = Arrays
				.copyOfRange(ArrayUtils.toPrimitive(inputArr), inputArr.length - period, inputArr.length);
		double resistance = Doubles.max(highPeriod);

		double distanceToResist = lastMid - resistance;
		double ticksDistance = distanceToResist / instrument.getPriceTick();//TODO take a look on volume!!
		double indicator = Math.round(ticksDistance);
		if (BINARY_STATE_OUTPUTS) {
			if (indicator < 0) {
				indicator = -1;
			} else {
				indicator = 1;
			}
		}
		return indicator;
	}

	private double getMinIndicator(int period, Double[] inputArr, double lastMid) {
		double[] lowPeriod = Arrays
				.copyOfRange(ArrayUtils.toPrimitive(inputArr), inputArr.length - period, inputArr.length);
		double support = Doubles.min(lowPeriod);

		double distanceToSupport = lastMid - support;
		double ticksDistance = distanceToSupport / instrument.getPriceTick();//TODO take a look on volume!!
		double indicator = Math.round(ticksDistance);
		if (BINARY_STATE_OUTPUTS) {
			if (indicator < 0) {
				indicator = -1;
			} else {
				indicator = 1;
			}
		}
		return indicator;
	}

	@Override public int getCurrentStatePosition() {
		double[] currentStateArr = getCurrentStateRounded();//filtered
		return getStateFromArray(currentStateArr);
	}

	@Override public void enumerateStates(String cachePermutationsPath) {

	}

	@Override public void updateCandle(Candle candle) {
		if (!candle.getCandleType().equals(candleType)) {
			return;
		}
		queueCandles.offer(candle);

		queueCandlesMinPrices.offer(candle.getLow());
		queueCandlesClosePrices.offer(candle.getClose());
		queueCandlesMaxPrices.offer(candle.getHigh());
		queueCandlesOpenPrices.offer(candle.getOpen());

		queueCandlesCloseVolume.offer(candle.getCloseVolume());
		queueCandlesHighVolume.offer(candle.getHighVolume());
		queueCandlesLowVolume.offer(candle.getLowVolume());
		queueCandlesOpenVolume.offer(candle.getOpenVolume());

		queueCandlesMicroPricesSpreads.offer(calculateMicropriceSpread());
		queueCandlesVPIN.offer(lastDepth.getVPIN());

		queueCandlesSignedTransactionVolume.offer(getSignedTransactionVolume());    //adding last period signed volume
		queueCandlesSignedTransaction.offer(getSignedTransaction());    //adding last period signed volume
		queueCandlesMicropriceSpread
				.offer(lastDepth.getMicroPrice() - lastDepth.getMidPrice());    //adding last period signed volume
		queueCandlesVPINValues.offer(lastDepth.getVPIN());    //adding last period signed volume

		cleanTrades();//reset trades buffering

	}

	private double calculateMicropriceSpread() {
		double currentMicroSpreadDiff =
				(lastDepth.getMidPrice() - lastDepth.getMicroPrice()) / instrument.getPriceTick();
		currentMicroSpreadDiff = Math.round(currentMicroSpreadDiff * 10) / 10.0;//only one decimal permitted
		return currentMicroSpreadDiff;
	}

	@Override public void updateTrade(Trade trade) {
		//
		updateTradesBuffer(trade);
	}

	@Override public void updatePrivateState(PnlSnapshot pnlSnapshot) {
	}

	@Override public void updateDepthState(Depth depth) {
		lastDepth = depth;
		timeService.setCurrentTimestamp(lastDepth.getTimestamp());
		volumeFromStart += lastDepth.getTotalVolume();
		if ((depth.getTimestamp() - lastMarketTickSave) < marketTickMs) {
			//not enough time to save it
			return;
		}

		double bid = depth.getBestBid();
		double bidQty = depth.getBestBidQty();
		double ask = depth.getBestAsk();
		double askQty = depth.getBestAskQty();
		double midPrice = depth.getMidPrice();
		double microPrice = depth.getMicroPrice();
		double imbalance = depth.getImbalance();
		double spread = depth.getSpread();
		if (MARKET_MIDPRICE_RELATIVE) {
			bid = Math.abs(bid - midPrice);
			ask = Math.abs(ask - midPrice);
			microPrice = Math.abs(microPrice - midPrice);
		}
		//		midpriceBuffer.add(midPrice);
		spreadBuffer.offer(spread);
		bidPriceBuffer.offer(bid);
		bidQtyBuffer.offer(bidQty);
		askPriceBuffer.offer(ask);
		askQtyBuffer.offer(askQty);
		micropriceBuffer.offer(microPrice);
		imbalanceBuffer.offer(imbalance);

		lastMarketTickSave = depth.getTimestamp();

	}
}
