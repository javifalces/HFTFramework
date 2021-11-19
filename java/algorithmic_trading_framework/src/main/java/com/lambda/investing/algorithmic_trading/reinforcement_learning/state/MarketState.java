package com.lambda.investing.algorithmic_trading.reinforcement_learning.state;

import com.google.common.primitives.Doubles;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreEnum;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreUtils;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.curator.shaded.com.google.common.collect.Queues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import static com.lambda.investing.algorithmic_trading.reinforcement_learning.MatrixRoundUtils.*;

public class MarketState extends AbstractState {

	private static boolean MARKET_MIDPRICE_RELATIVE = true;
	private static boolean PRIVATE_QUANTITY_RELATIVE = true;
	private static boolean PRIVATE_DELTA_STATES = false;
	public static boolean REMOVE_PRIVATE_STATE = false;

	public static String PERIOD_CANDLE_1MIN = "1Min";

	private static double MARKET_MAX_NUMBER = 10.;
	private static double MARKET_MIN_NUMBER = -10.;

	private static String[] PRIVATE_COLUMNS_PATTERN = new String[] { "inventory", "score" };

	private static String[] MARKET_COLUMNS_PATTERN = new String[] { "bid_price", "ask_price", "bid_qty", "ask_qty",
			"spread", "midprice", "imbalance", "microprice", "last_close_price", "last_close_qty" };

	private static String[] CANDLE_COLUMNS_PATTERN = new String[] { "open", "high", "low", "close" };

	private static String[] CANDLE_INDICATORS = new String[] { "ma", "std", "max", "min" };
	private double lastCandlesMA, lastCandleStd, lastCandleMax, lastCandleMin = Double.NaN;
	//private buffer
	private ScoreEnum scoreEnumColumn;
	private int privateHorizonSave, marketHorizonSave, candleHorizonSave;
	private long privateTickMs, marketTickMs;
	private long lastPrivateTickSave, lastMarketTickSave;

	private int privateNumberDecimals, marketNumberDecimals, candleNumberDecimals;
	private double privateMinNumber, privateMaxNumber, marketMinNumber, marketMaxNumber, candleMinNumber, candleMaxNumber;

	private Queue<Double> inventoryBuffer, scoreBuffer;
	private double quantity;

	//market buffer
	private Queue<Double> bidPriceBuffer, askPriceBuffer, bidQtyBuffer, askQtyBuffer, spreadBuffer, midpriceBuffer, imbalanceBuffer, micropriceBuffer, lastClosePriceBuffer, lastCloseQuantityBuffer;

	private Queue<Double> candlesOpen;
	private Queue<Double> candlesHigh;
	private Queue<Double> candlesLow;
	private Queue<Double> candlesClose;
	private CandleType candleType;
	private boolean disableLastClose = false;

	public MarketState(ScoreEnum scoreEnumColumn, int privateHorizonSave, int marketHorizonSave, int candleHorizonSave,
			long privateTickMs, long marketTickMs, int privateNumberDecimals, int marketNumberDecimals,
			int candleNumberDecimals, double privateMinNumber, double privateMaxNumber, double marketMinNumber,
			double marketMaxNumber, double candleMinNumber, double candleMaxNumber, double quantity,
			CandleType candleType) {
		super(privateNumberDecimals);
		this.scoreEnumColumn = scoreEnumColumn;

		this.privateHorizonSave = privateHorizonSave;
		this.marketHorizonSave = marketHorizonSave;
		this.candleHorizonSave = candleHorizonSave;

		this.privateTickMs = privateTickMs;
		this.marketTickMs = marketTickMs;
		this.candleType = candleType;

		this.privateNumberDecimals = privateNumberDecimals;
		this.marketNumberDecimals = marketNumberDecimals;
		this.candleNumberDecimals = candleNumberDecimals;
		if (MARKET_MIDPRICE_RELATIVE) {
			//open is not on relative
			CANDLE_COLUMNS_PATTERN = new String[] { "high", "low", "close" };
		}
		if (this.privateNumberDecimals <= 0) {
			logger.warn("privateState are not going to be rounded when  privateNumberDecimals {}<=0",
					this.privateNumberDecimals);
		}

		if (this.marketNumberDecimals <= 0) {
			logger.warn("marketState are not going to be rounded when  marketNumberDecimals {}<=0",
					this.marketNumberDecimals);
		}

		if (this.candleNumberDecimals <= 0) {
			logger.warn("candleState are not going to be rounded when  candleNumberDecimals {}<=0",
					this.candleNumberDecimals);
		}

		if (privateMinNumber > privateMaxNumber) {
			logger.warn("privateState minNumber {} maxNumber {} is wrong -> set default ", privateMinNumber,
					privateMaxNumber);
			privateMaxNumber = MAX_NUMBER;
			privateMinNumber = MIN_NUMBER;
		}
		if (privateMinNumber == privateMaxNumber && privateMinNumber == -1) {
			logger.warn("privateState are not going to be bound {} {} when is -1 ", privateMinNumber, privateMaxNumber);
		}
		if (marketMinNumber > marketMaxNumber) {
			logger.warn("marketState minNumber {} maxNumber {} is wrong -> set default ", marketMinNumber,
					marketMaxNumber);
			marketMaxNumber = MARKET_MAX_NUMBER;
			marketMinNumber = MARKET_MIN_NUMBER;
		}
		if (marketMinNumber == marketMaxNumber && marketMinNumber == -1) {
			logger.warn("marketState are not going to be bound {} {} when is -1 ", marketMinNumber, marketMaxNumber);
		}

		if (candleMinNumber > candleMaxNumber) {
			logger.warn("candleState minNumber {} maxNumber {} is wrong -> set default ", candleMinNumber,
					candleMaxNumber);
			candleMaxNumber = MARKET_MAX_NUMBER;
			candleMinNumber = MARKET_MIN_NUMBER;
		}

		if (candleMinNumber == candleMaxNumber && candleMinNumber == -1) {
			logger.warn("candleState are not going to be bound {} {} when is -1 ", candleMinNumber, candleMaxNumber);
		}

		this.privateMinNumber = privateMinNumber;
		this.privateMaxNumber = privateMaxNumber;

		this.candleMinNumber = candleMinNumber;
		this.candleMaxNumber = candleMaxNumber;

		this.marketMinNumber = marketMinNumber;
		this.marketMaxNumber = marketMaxNumber;
		this.quantity = quantity;

		//private
		//save one more to diff it
		if (PRIVATE_DELTA_STATES) {
			inventoryBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.privateHorizonSave + 1));
			scoreBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.privateHorizonSave + 1));
		} else {
			inventoryBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.privateHorizonSave));
			scoreBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.privateHorizonSave));
		}

		//market
		bidPriceBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		askPriceBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		bidQtyBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		askQtyBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		spreadBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		midpriceBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		imbalanceBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		micropriceBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		lastClosePriceBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));
		lastCloseQuantityBuffer = Queues.synchronizedQueue(EvictingQueue.create(this.marketHorizonSave));

		//candle
		if (this.candleHorizonSave > 0) {
			candlesOpen = Queues.synchronizedQueue(EvictingQueue.create(this.candleHorizonSave));
			candlesHigh = Queues.synchronizedQueue(EvictingQueue.create(this.candleHorizonSave));
			candlesLow = Queues.synchronizedQueue(EvictingQueue.create(this.candleHorizonSave));
			candlesClose = Queues.synchronizedQueue(EvictingQueue.create(this.candleHorizonSave));
		}
		numberOfColumns = getColumns().size();

	}

	public void disableLastClose() {

		this.disableLastClose = true;
		MARKET_COLUMNS_PATTERN = new String[] { "bid_price", "ask_price", "bid_qty", "ask_qty", "spread", "midprice",
				"imbalance", "microprice" };

	}

	public List<String> getPrivateColumns() {
		if (REMOVE_PRIVATE_STATE) {
			return new ArrayList<>();
		}
		List<String> output = new ArrayList<>();
		//private
		for (int colIndex = 0; colIndex < PRIVATE_COLUMNS_PATTERN.length; colIndex++) {
			for (int horizonTick = privateHorizonSave - 1; horizonTick >= 0; horizonTick--) {
				output.add(PRIVATE_COLUMNS_PATTERN[colIndex] + "_" + String.valueOf(horizonTick));
			}
		}
		return output;
	}

	@Override public List<String> getColumns() {
		//private
		List<String> output = getPrivateColumns();

		//market
		for (int colIndex = 0; colIndex < MARKET_COLUMNS_PATTERN.length; colIndex++) {
			for (int horizonTick = marketHorizonSave - 1; horizonTick >= 0; horizonTick--) {

				output.add(MARKET_COLUMNS_PATTERN[colIndex] + "_" + String.valueOf(horizonTick));
			}
		}

		//candles
		if (this.candleHorizonSave > 0) {
			for (int colIndex = 0; colIndex < CANDLE_COLUMNS_PATTERN.length; colIndex++) {
				for (int horizonTick = candleHorizonSave - 1; horizonTick >= 0; horizonTick--) {
					output.add(CANDLE_COLUMNS_PATTERN[colIndex] + "_" + String.valueOf(horizonTick));
				}
			}
			//candle indicators
			for (int colIndex = 0; colIndex < CANDLE_INDICATORS.length; colIndex++) {
				output.add(CANDLE_INDICATORS[colIndex]);
			}
		}
		return output;
	}

	@Override public int getNumberStates() {
		logger.warn("we are asking number of states to marketState!!");
		return Integer.MAX_VALUE;
	}

	@Override public boolean isReady() {
		boolean privateIsReady = true;

		if (!REMOVE_PRIVATE_STATE) {
			///only check it if its false
			if (PRIVATE_DELTA_STATES) {
				if (this.inventoryBuffer.size() < this.privateHorizonSave + 1
						|| this.scoreBuffer.size() < this.privateHorizonSave + 1) {
					//				logger.error("not enough states received {}< {} ", this.inventoryBuffer.size(), this.horizonSave + 1);
					privateIsReady = false;
				}
			} else {
				if (this.inventoryBuffer.size() < this.privateHorizonSave
						|| this.scoreBuffer.size() < this.privateHorizonSave) {
					//				logger.error("not enough states received {}< {} ", this.inventoryBuffer.size(), this.horizonSave);
					privateIsReady = false;
				}
			}
		}

		boolean marketIsReady = true;
		if (!disableLastClose) {
			if (this.bidPriceBuffer.size() < this.marketHorizonSave
					|| this.askPriceBuffer.size() < this.marketHorizonSave
					|| this.bidQtyBuffer.size() < this.marketHorizonSave
					|| this.askQtyBuffer.size() < this.marketHorizonSave
					|| this.lastCloseQuantityBuffer.size() < this.marketHorizonSave
					|| this.lastClosePriceBuffer.size() < this.marketHorizonSave) {
				marketIsReady = false;
			}
		} else {
			if (this.bidPriceBuffer.size() < this.marketHorizonSave
					|| this.askPriceBuffer.size() < this.marketHorizonSave
					|| this.bidQtyBuffer.size() < this.marketHorizonSave
					|| this.askQtyBuffer.size() < this.marketHorizonSave) {
				marketIsReady = false;
			}
		}

		boolean candleIsReady = true;
		if (candleHorizonSave > 0) {
			if (Double.isNaN(lastCandleMax) || Double.isNaN(lastCandleMin) || Double.isNaN(lastCandlesMA) || Double
					.isNaN(lastCandleStd)) {
				candleIsReady = false;
			}
		}

		if (!privateIsReady && candleIsReady && marketIsReady) {
			logger.warn("private states initially not received-> set 0.0");
			for (int horizon = 0; horizon < this.privateHorizonSave; horizon++) {
				this.inventoryBuffer.add(0.0);
				this.scoreBuffer.add(0.0);
			}
			privateIsReady = true;
		}

		return candleIsReady && marketIsReady && privateIsReady;

	}

	@Override public void updateCandle(Candle candle) {
		//last update on last element
		if (this.candleHorizonSave <= 0) {
			return;
		}
		if (!candle.getCandleType().equals(this.candleType)) {
			return;
		}

		double open = candle.getOpen();
		double high = candle.getHigh();
		double low = candle.getLow();
		double close = candle.getClose();

		//relative candles
		if (MARKET_MIDPRICE_RELATIVE) {
			high = (high / open) - 1.;
			low = (low / open) - 1.;
			close = (close / open) - 1.;
			open = 0.;///not deleting!
		}

		candlesOpen.add(open);
		candlesHigh.add(high);
		candlesLow.add(low);
		candlesClose.add(close);

		if (candlesClose.size() >= candleHorizonSave) {
			Double[] highCandlesDouble = new Double[candleHorizonSave];
			candlesHigh.toArray(highCandlesDouble);
			double[] highCandles = ArrayUtils.toPrimitive(highCandlesDouble);
			lastCandleMax = maxValue(highCandles);

			Double[] lowCandlesDouble = new Double[candleHorizonSave];
			candlesLow.toArray(lowCandlesDouble);
			double[] lowCandles = ArrayUtils.toPrimitive(lowCandlesDouble);
			lastCandleMin = minValue(lowCandles);

			Double[] closeCandlesDouble = new Double[candleHorizonSave];
			candlesClose.toArray(closeCandlesDouble);
			double[] closeCandles = ArrayUtils.toPrimitive(closeCandlesDouble);

			lastCandlesMA = meanValue(closeCandles);
			lastCandleStd = stdValue(closeCandles);
		}

	}

	@Override public synchronized void updateTrade(Trade trade) {
		if (!disableLastClose) {
			lastClosePriceBuffer.add(trade.getPrice());
			lastCloseQuantityBuffer.add(trade.getQuantity());
		}
	}

	@Override public synchronized void updatePrivateState(PnlSnapshot pnlSnapshot) {
		if ((pnlSnapshot.getLastTimestampUpdate() - lastPrivateTickSave) < privateTickMs) {
			//not enough time to save it
			return;
		}

		double score = ScoreUtils.getReward(this.scoreEnumColumn, pnlSnapshot);
		if (PRIVATE_QUANTITY_RELATIVE) {
			score = score / quantity;
		}
		scoreBuffer.add(score);

		double position = pnlSnapshot.netPosition;
		if (PRIVATE_QUANTITY_RELATIVE) {
			position = position / quantity;
		}
		inventoryBuffer.add(position);

		lastPrivateTickSave = pnlSnapshot.getLastTimestampUpdate();
	}

	@Override public synchronized void updateDepthState(Depth depth) {
		if ((depth.getTimestamp() - lastMarketTickSave) < marketTickMs) {
			//not enough time to save it
			return;
		}

		//		Instrument instrument = Instrument.getInstrument(depth.getInstrument());
		//		if (instrument.isFX()) {
		//			//set all quantities of depth to zero just in case of backtest fill memory replay as in pro
		//			depth.setAllQuantitiesZero();
		//		}
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
		midpriceBuffer.add(midPrice);
		spreadBuffer.add(spread);
		bidPriceBuffer.add(bid);
		bidQtyBuffer.add(bidQty);
		askPriceBuffer.add(ask);
		askQtyBuffer.add(askQty);
		micropriceBuffer.add(microPrice);
		imbalanceBuffer.add(imbalance);

		lastMarketTickSave = depth.getTimestamp();

	}

	private double[] getPrivateState() {
		if (REMOVE_PRIVATE_STATE) {
			return new double[1];
		}
		List<Double> outputList = new ArrayList<>(this.numberOfColumns);
		if (PRIVATE_DELTA_STATES) {
			outputList.addAll(getDiffQueue(inventoryBuffer));
			outputList.addAll(getDiffQueue(scoreBuffer));
		} else {
			outputList.addAll(inventoryBuffer);
			outputList.addAll(scoreBuffer);
		}
		if (outputList.size() == 0) {
			return null;
		}

		double[] outputArr = outputList.stream().mapToDouble(Double::doubleValue).toArray();
		outputArr = getRoundedState(outputArr, privateMaxNumber, privateMinNumber, privateNumberDecimals);
		return outputArr;
	}

	private double[] getMarketState() {
		List<Double> outputList = new ArrayList<>(this.numberOfColumns);
		//		{ "bid_price", "ask_price", "bid_qty", "ask_qty",
		//			"spread", "midprice", "imbalance", "microprice", "last_close_price", "last_close_qty" };
		try {
			outputList.addAll(bidPriceBuffer);
			outputList.addAll(askPriceBuffer);
			outputList.addAll(bidQtyBuffer);
			outputList.addAll(askQtyBuffer);
			outputList.addAll(spreadBuffer);
			outputList.addAll(midpriceBuffer);
			outputList.addAll(imbalanceBuffer);
			outputList.addAll(micropriceBuffer);
			if (!disableLastClose) {
				outputList.addAll(lastClosePriceBuffer);
				outputList.addAll(lastCloseQuantityBuffer);
			}
		} catch (Exception e) {
			logger.error("error getting marketState ", e);
			outputList.clear();
		}
		if (outputList.size() == 0) {
			return null;
		}
		double[] outputArr = outputList.stream().mapToDouble(Double::doubleValue).toArray();
		outputArr = getRoundedState(outputArr, marketMaxNumber, marketMinNumber, marketNumberDecimals);
		return outputArr;

	}

	private double[] getCandleState() {
		List<Double> outputList = new ArrayList<>(this.numberOfColumns);

		//	private static String[] CANDLE_COLUMNS_PATTERN = new String[] {"open","high","low","close"};
		//
		//	private static String[] CANDLE_INDICATORS = new String[] { "ma", "std", "max", "min" };
		if (!MARKET_MIDPRICE_RELATIVE) {
			//dont add it ...is all zero
			outputList.addAll(candlesOpen);
		}
		outputList.addAll(candlesHigh);
		outputList.addAll(candlesLow);
		outputList.addAll(candlesClose);

		outputList.add(lastCandlesMA);
		outputList.add(lastCandleStd);
		outputList.add(lastCandleMax);
		outputList.add(lastCandleMin);
		if (outputList.size() == 0) {
			return null;
		}
		double[] outputArr = outputList.stream().mapToDouble(Double::doubleValue).toArray();
		outputArr = getRoundedState(outputArr, candleMaxNumber, candleMinNumber, candleNumberDecimals);
		return outputArr;
	}

	@Override public synchronized double[] getCurrentStateRounded() {
		//returns it rounded!
		if (!isReady()) {
			logger.error("not enough market states received");
			return null;
		}
		double[] privateState = null;
		double[] marketState = null;
		double[] candleState = null;
		try {
			privateState = getPrivateState();
			marketState = getMarketState();
			if (this.candleHorizonSave > 0) {
				candleState = getCandleState();
			} else {
				candleState = new double[] { 0.0 };
			}

		} catch (Exception e) {
			logger.error("error getting state", e);
		}
		if (privateState == null || marketState == null || candleState == null) {
			logger.error("something is wrong when one states is null and is ready");
		}

		privateState = getRoundedState(privateState, privateMaxNumber, privateMinNumber, privateNumberDecimals);
		marketState = getRoundedState(marketState, marketMaxNumber, marketMinNumber, marketNumberDecimals);
		if (this.candleHorizonSave > 0) {
			candleState = getRoundedState(candleState, candleMaxNumber, candleMinNumber, candleNumberDecimals);
		}

		double[] output = null;
		if (REMOVE_PRIVATE_STATE) {
			output = marketState;
		} else {
			output = ArrayUtils.addAll(privateState, marketState);
		}

		if (this.candleHorizonSave > 0) {
			output = ArrayUtils.addAll(output, candleState);
		}
		assert output.length == numberOfColumns;

		output = getFilteredState(output);//get filter
		return output;

	}

	//Sort it
	@Override public synchronized double[] getCurrentState() {
		//returns it rounded!
		if (!isReady()) {
			logger.error("not enough market states received");
			return null;
		}
		double[] privateState = null;
		double[] marketState = null;
		double[] candleState = null;
		try {
			privateState = getPrivateState();
			marketState = getMarketState();
			if (this.candleHorizonSave > 0) {
				candleState = getCandleState();
			} else {
				candleState = new double[] { 0.0 };
			}

		} catch (Exception e) {
			logger.error("error getting state", e);
		}
		if (privateState == null || marketState == null || candleState == null) {
			logger.error("something is wrong when one states is null and is ready");
		}

		double[] output = null;
		if (REMOVE_PRIVATE_STATE) {
			output = marketState;
		} else {
			output = ArrayUtils.addAll(privateState, marketState);
		}

		if (this.candleHorizonSave > 0) {
			output = ArrayUtils.addAll(output, candleState);
		}
		assert output.length == numberOfColumns;
		output = getFilteredState(output);//get filter
		return output;

	}

	@Override public int getCurrentStatePosition() {
		double[] currentStateArr = getCurrentStateRounded();//filtered
		return getStateFromArray(currentStateArr);
	}

	protected double[] getRoundedState(double[] state) {
		//here returs the same
		return state;
	}

	@Override public void enumerateStates(String cachePermutationsPath) {

	}

}
