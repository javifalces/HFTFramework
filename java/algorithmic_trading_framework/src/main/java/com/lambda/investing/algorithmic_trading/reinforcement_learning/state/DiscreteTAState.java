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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;

import java.util.*;

import static com.lambda.investing.algorithmic_trading.technical_indicators.Calculator.*;

public class DiscreteTAState extends AbstractState {

	//every new state must be added on python too on python_lambda/backtest/rsi_dqn.py: _get_ta_state_columns
	public static boolean BINARY_STATE_OUTPUTS = true;

	private static String[] STATE_COLUMNS_PATTERN = new String[] {
			////one per period
			"microprice_",//midpricee-microprice
			"vpin_", "rsi_",//rsi<30 -> 1 rsi>70->-1 else 0
			"sma_",//sma<price -> -1 sma>=price->1
			"max_",//price>max=1 else 0
			"min_",//price>max=1 else 0

	};
	private static String[] STATE_SINGLE_COLUMNS = new String[] { "hour_of_the_day_utc" };

	private static int[] rsiPeriods = new int[] { 3, 6, 9, 13 };///used as reference
	private static int[] smaPeriods = rsiPeriods;
	private static int[] maxPeriods = rsiPeriods;
	private static int[] minPeriods = rsiPeriods;
	private static int maxPeriod = rsiPeriods[rsiPeriods.length - 1];

	private CandleType candleType;

	protected ScoreEnum scoreEnumColumn;
	protected boolean isReady = false;
	protected Depth lastDepth;
	protected Queue<Candle> queueCandles;
	protected Queue<Double> queueCandlesOpenPrices;
	protected Queue<Double> queueCandlesClosePrices;
	protected Queue<Double> queueCandlesMaxPrices;
	protected Queue<Double> queueCandlesMinPrices;

	protected Queue<Double> queueCandlesMicroPricesSpreads;
	protected Queue<Double> queueCandlesVPIN;
	//	protected Queue<Double> queueCandlesRelativeBidAskQtyDiff;
	protected Instrument instrument = Instrument.getInstrument("eurusd_darwinex");

	private TimeService timeService;

	public DiscreteTAState(ScoreEnum scoreEnumColumn, CandleType candleType, int[] periods) {
		super(0);//all integer
		this.candleType = candleType;
		this.scoreEnumColumn = scoreEnumColumn;

		this.rsiPeriods = periods;
		this.smaPeriods = periods;
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
		this.numberOfColumns = getColumns().size();//

	}

	public DiscreteTAState(ScoreEnum scoreEnumColumn, CandleType candleType) {
		super(0);//all integer

		this.candleType = candleType;
		this.scoreEnumColumn = scoreEnumColumn;
		maxPeriod = 13;
		queueCandles = EvictingQueue.create(maxPeriod);
		timeService = new TimeService("UTC");
		queueCandlesOpenPrices = EvictingQueue.create(maxPeriod);
		queueCandlesClosePrices = EvictingQueue.create(maxPeriod);
		queueCandlesMaxPrices = EvictingQueue.create(maxPeriod);
		queueCandlesMinPrices = EvictingQueue.create(maxPeriod);
		queueCandlesMicroPricesSpreads = EvictingQueue.create(maxPeriod);
		queueCandlesVPIN = EvictingQueue.create(maxPeriod);
		//		queueCandlesRelativeBidAskQtyDiff = EvictingQueue.create(maxPeriod);

		this.numberOfColumns = getColumns().size();//
	}

	public void setInstrument(Instrument instrument) {
		this.instrument = instrument;
	}

	//	public void setCurrentVerb(Verb currentVerb) {
	//		this.currentVerb = currentVerb;
	//	}

	@Override public List<String> getColumns() {

		List<String> outputColumns = new ArrayList<>();

		for (String stateColumn : STATE_COLUMNS_PATTERN) {
			for (int period : rsiPeriods) {
				outputColumns.add(stateColumn + period);
			}

		}
		outputColumns.addAll(Arrays.asList(STATE_SINGLE_COLUMNS));
		/// microprice_7,microprice_9,microprice_13,qtydiff_7,..rsi_7,rsi_9,rsi_13,sma_7,sma_9,sma_13......

		return outputColumns;
	}

	@Override public int getNumberStates() {
		logger.warn("we are asking number of states to DiscreteTAState!!");
		return 6666;//2^(6)*(3^3)
	}

	@Override public boolean isReady() {
		return (queueCandles.size() == maxPeriod);
	}

	@Override public double[] getCurrentState() {

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

		double lastMid = closePrices[closePrices.length - 1];
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
					double rsiValue = RSICalculate(ArrayUtils.toPrimitive(closePrices), period);
					indicator = Math.round(rsiValue / 10);

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

				}

				//sma
				if (stateColumn.startsWith("sma")) {
					double smaValue = EMACalculate(ArrayUtils.toPrimitive(closePrices), period);
					double distanceToSma = lastMid - smaValue;
					double ticksDistance = distanceToSma / instrument.getPriceTick();
					indicator = Math.round(ticksDistance);

					if (BINARY_STATE_OUTPUTS) {
						if (indicator < 0) {
							indicator = 1;
						} else {
							indicator = -1;
						}
					}
				}
				//max
				if (stateColumn.startsWith("max")) {
					double[] highPeriod = Arrays
							.copyOfRange(ArrayUtils.toPrimitive(highPrices), highPrices.length - period,
									highPrices.length);
					double resistance = Doubles.max(highPeriod);

					double distanceToResist = lastMid - resistance;
					double ticksDistance = distanceToResist / instrument.getPriceTick();
					indicator = Math.round(ticksDistance);
					if (BINARY_STATE_OUTPUTS) {
						if (indicator < 0) {
							indicator = -1;
						} else {
							indicator = 1;
						}
					}

				}

				//min
				if (stateColumn.startsWith("min")) {
					double[] lowPeriod = Arrays
							.copyOfRange(ArrayUtils.toPrimitive(lowPrices), lowPrices.length - period,
									lowPrices.length);
					double support = Doubles.min(lowPeriod);

					double distanceToSupport = lastMid - support;
					double ticksDistance = distanceToSupport / instrument.getPriceTick();
					indicator = Math.round(ticksDistance);
					if (BINARY_STATE_OUTPUTS) {
						if (indicator < 0) {
							indicator = -1;
						} else {
							indicator = 1;
						}
					}
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

					} else {
						indicator = Math.round(indicator * 2) / 2.0;//0.5 rounded
						indicator = Math.round(indicator * 10) / 10.0;//one decimal
					}
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

					} else {
						indicator = Math.round(indicator * 2) / 2.0;//0.5 rounded
						indicator = Math.round(indicator * 10) / 10.0;//one decimal
					}
				}

				stateList.add(indicator);
			}
		}

		for (String stateColumn : STATE_SINGLE_COLUMNS) {

			//single state columns
			if (stateColumn.equalsIgnoreCase("hour_of_the_day_utc")) {
				int hour = timeService.getCurrentTimeHour();
				stateList.add((double) hour);
			}

		}

		Double[] output = new Double[stateList.size()];
		output = stateList.toArray(output);
		return ArrayUtils.toPrimitive(output);
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
		queueCandles.add(candle);

		queueCandlesMinPrices.add(candle.getLow());
		queueCandlesClosePrices.add(candle.getClose());
		queueCandlesMaxPrices.add(candle.getHigh());
		queueCandlesOpenPrices.add(candle.getOpen());
		queueCandlesMicroPricesSpreads.add(calculateMicropriceSpread());
		queueCandlesVPIN.add(lastDepth.getVPIN());

	}

	private double calculateMicropriceSpread() {
		double currentMicroSpreadDiff =
				(lastDepth.getMidPrice() - lastDepth.getMicroPrice()) / instrument.getPriceTick();
		currentMicroSpreadDiff = Math.round(currentMicroSpreadDiff * 10) / 10.0;//only one decimal permitted
		return currentMicroSpreadDiff;
	}

	@Override public void updateTrade(Trade trade) {

	}

	@Override public void updatePrivateState(PnlSnapshot pnlSnapshot) {
	}

	@Override public void updateDepthState(Depth depth) {
		lastDepth = depth;
		timeService.setCurrentTimestamp(lastDepth.getTimestamp());
	}
}
