package com.lambda.investing.algorithmic_trading.reinforcement_learning.state;

import com.lambda.investing.ArrayUtils;
import com.lambda.investing.algorithmic_trading.TimeseriesUtils;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreEnum;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.curator.shaded.com.google.common.collect.Queues;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.lambda.investing.ArrayUtils.*;

public class MultiMarketDiscreteTAStateFX extends DiscreteTAStateFX {

	protected static int MULTIPLIER_SIZE_QUEUE = 1;
	protected Set<Instrument> otherInstruments;
	protected Set<Instrument> allInstruments;
	protected Set<String> allInstrumentsString;

	protected static String[] MULTI_MARKET_COLUMNS_PATTERN_BASE = new String[]{"zscore_mid"};
	protected String[] multiMarketColumnsPattern;

	private int multiMarketNumberDecimals;
	private double multiMarketMinNumber, multiMarketMaxNumber;
	private int maxPeriodMarketHorizonSave;
	private int multiMarketNumberOfColumns;

	private Map<String, Queue<Double>> bidPriceBuffer, askPriceBuffer, spreadBuffer, midpriceBuffer, imbalanceBuffer, microPriceBuffer, timestampBuffer;//instrument to market values
	private Map<String, Queue<TimeseriesUtils.TupleQueue>> tupleZscore;
	private Map<String, Long> firstTimestampSaved;
	private Map<String, Long> lastMultiMarketTickSave;
	private long lastTimestamp;

	private Set<Integer> rollingPeriods;
	protected long multiTickMs = 250;

	public MultiMarketDiscreteTAStateFX(Instrument instrument, List<Instrument> otherInstruments,
										ScoreEnum scoreEnumColumn, CandleType candleType, int[] periods, int numberOfDecimals,
										int marketHorizonSave, int[] periodsMsZscore) {

		super(instrument, scoreEnumColumn, candleType, periods, numberOfDecimals, marketHorizonSave);

		this.rollingPeriods = new HashSet<>();
		this.rollingPeriods.addAll(ArrayUtils.IntArrayList(periodsMsZscore));

		this.maxPeriodMarketHorizonSave = Collections.max(this.rollingPeriods);

		this.multiMarketNumberDecimals = multiMarketNumberDecimals;
		this.multiMarketMinNumber = multiMarketMinNumber;
		this.multiMarketMaxNumber = multiMarketMaxNumber;
		this.otherInstruments = cleanOtherInstruments(otherInstruments);
		multiMarketNumberOfColumns = getMultiMarketColumns().size();

		allInstruments = new HashSet<>(otherInstruments);
		if (this.instrument != null)
			allInstruments.add(this.instrument);

		allInstrumentsString = new HashSet<>();
		for (Instrument instrument1 : allInstruments) {
			allInstrumentsString.add(instrument1.getPrimaryKey());
		}

		//		initializedQueues();
		numberOfColumns = getNumberOfColumns();//override it
	}

	public synchronized void reset() {

		bidPriceBuffer.clear();
		askPriceBuffer.clear();
		spreadBuffer.clear();
		midpriceBuffer.clear();
		imbalanceBuffer.clear();
		microPriceBuffer.clear();
		timestampBuffer.clear();
		super.reset();
	}


	public void setInstrument(Instrument instrument) {
		super.setInstrument(instrument);
		allInstruments.add(instrument);
		allInstrumentsString.add(instrument.getPrimaryKey());
		initializedQueues();
	}

	public int getNumberOfColumns() {

		if (numberOfColumns == 0) {
			calculateNumberOfColumns();
		}
		return numberOfColumns;//
	}

	private Queue<?> createQueue() {
		long multiplier = this.maxPeriodMarketHorizonSave / multiTickMs;
		return Queues.synchronizedQueue(EvictingQueue.create((int) multiplier * MULTIPLIER_SIZE_QUEUE));
	}

	private Map<String, Queue<Double>> createDictQueueBuffer() {
		Map<String, Queue<Double>> output = new ConcurrentHashMap<>();
		for (Instrument restInstrument : this.allInstruments) {
			output.put(restInstrument.getPrimaryKey(), (Queue<Double>) createQueue());
		}
		return output;
	}

	private void initializedQueues() {
		bidPriceBuffer = createDictQueueBuffer();
		askPriceBuffer = createDictQueueBuffer();
		spreadBuffer = createDictQueueBuffer();
		midpriceBuffer = createDictQueueBuffer();
		imbalanceBuffer = createDictQueueBuffer();
		microPriceBuffer = createDictQueueBuffer();
		timestampBuffer = createDictQueueBuffer();
		firstTimestampSaved = new ConcurrentHashMap<>();
		lastMultiMarketTickSave = new ConcurrentHashMap<>();

		tupleZscore = new ConcurrentHashMap<>();
		for (Instrument restInstrument : this.allInstruments) {
			tupleZscore.put(restInstrument.getPrimaryKey(), (Queue<TimeseriesUtils.TupleQueue>) createQueue());
		}

	}

	private String[] getMultiMarketColumnsPattern() {
		List<String> outputList = new ArrayList<>();
		for (Instrument instrument : otherInstruments) {
			for (String columnBase : MULTI_MARKET_COLUMNS_PATTERN_BASE) {
				outputList.add(columnBase + "_" + instrument);
			}
		}
		return StringListToArray(outputList);
	}

	protected Set<Instrument> cleanOtherInstruments(List<Instrument> otherInstruments) {
		Set<Instrument> output = new HashSet<>();
		for (Instrument instrument : otherInstruments) {
			if (!instrument.equals(this.instrument)) {
				output.add(instrument);
			}
		}
		return output;
	}

	protected List<String> getMultiMarketColumns() {
		List<String> output = new ArrayList<>();
		multiMarketColumnsPattern = getMultiMarketColumnsPattern();
		for (int colIndex = 0; colIndex < multiMarketColumnsPattern.length; colIndex++) {
			for (int horizonMs : rollingPeriods) {
				output.add(multiMarketColumnsPattern[colIndex] + "_" + horizonMs);
			}
		}
		return output;
	}

	@Override
	public List<String> getColumns() {
		//market + private + candles
		List<String> output = super.getColumns();
		//multi market
		output.addAll(getMultiMarketColumns());
		return output;
	}

	@Override
	public synchronized void updateDepthState(Depth depth) {
		if (depth.getTimestamp() > lastTimestamp) {
			lastTimestamp = depth.getTimestamp();
		}
		super.updateDepthState(depth);
		long lastUpdate = lastMultiMarketTickSave.getOrDefault(depth.getInstrument(), 0L);
		if ((depth.getTimestamp() - lastUpdate) < multiTickMs) {
			//not enough time to save it
			return;
		}

		String instrumentPk = depth.getInstrument();
		if (!allInstrumentsString.contains(instrumentPk)) {
			return;
		}

		if (!firstTimestampSaved.containsKey(depth.getInstrument())) {
			firstTimestampSaved.put(depth.getInstrument(), lastTimestamp);
		}

		//update buffers
		bidPriceBuffer.get(instrumentPk).offer(depth.getBestBid());
		askPriceBuffer.get(instrumentPk).offer(depth.getBestAsk());
		midpriceBuffer.get(instrumentPk).offer(depth.getMidPrice());
		spreadBuffer.get(instrumentPk).offer(depth.getSpread());
		imbalanceBuffer.get(instrumentPk).offer(depth.getImbalance());
		microPriceBuffer.get(instrumentPk).offer(depth.getMicroPrice());
		timestampBuffer.get(instrumentPk).offer((double) depth.getTimestamp());

		tupleZscore.get(instrumentPk).offer(new TimeseriesUtils.TupleQueue(depth.getTimestamp(), depth.getMidPrice()));
		lastMultiMarketTickSave.put(depth.getInstrument(), depth.getTimestamp());

	}

	@Override
	public synchronized boolean isReady() {
		boolean marketReady = super.isReady();
		boolean weAreReady = true;
		if (lastTimestamp == 0) {
			return false;
		}
		for (String instrumentPk : allInstrumentsString) {
			if (!firstTimestampSaved.containsKey(instrumentPk)) {
				return false;
			}
			long firstTimeSaved = firstTimestampSaved.get(instrumentPk);
			if (lastTimestamp < firstTimeSaved + maxPeriodMarketHorizonSave) {
				logger.debug("{} is not ready with {} horizon save and firstTimeSaved:{} lastTimestamp:{} ",
						instrumentPk, this.maxPeriodMarketHorizonSave, new Date(firstTimeSaved),
						new Date(lastTimestamp));
				weAreReady = false;
			}
		}

		return marketReady && weAreReady;
	}

	@Override
	public synchronized void updateTrade(Trade trade) {
		super.updateTrade(trade);
		////nothing here yet!
	}

	@Override
	public synchronized double[] getCurrentState() {
		double[] marketState = super.getCurrentState();

		double[] multiMarketState = getMultiMarketState();
		multiMarketState = getRoundedState(multiMarketState, multiMarketMaxNumber, multiMarketMinNumber,
				multiMarketNumberDecimals);
		double[] output = DoubleMergeArrays(marketState, multiMarketState);


		return output;
	}

	protected double getZscore(String instrument, String otherInstrument, int period,
							   Map<String, Queue<TimeseriesUtils.TupleQueue>> values) {

		double zscore = TimeseriesUtils
				.GetLastZscoreTimeseries(instrument, otherInstrument, lastTimestamp, period, values);
		double output = Math.round(zscore * 100) / 100.0;
		if (BINARY_STATE_OUTPUTS) {
			output = Math.round(zscore);//1-0-1
			output = Math.min(output, 1);
			output = Math.max(output, -1);

		}
		return output;

	}

	private double[] getMultiMarketState() {
		List<Double> outputList = new ArrayList<>(this.multiMarketNumberOfColumns);

		for (Instrument otherInstrument : otherInstruments) {
			for (String columnBase : MULTI_MARKET_COLUMNS_PATTERN_BASE) {
				for (Integer period : rollingPeriods) {

					if (columnBase.equalsIgnoreCase("zscore_mid")) {
						double zscore = getZscore(instrument.getPrimaryKey(), otherInstrument.getPrimaryKey(), period,
								tupleZscore);
						outputList.add(zscore);
					}
				}
			}
		}
		return DoubleListToPrimitiveArray(outputList);

	}

	public Map<Integer, Double> getZScores(Instrument otherInstrument) {
		Map<Integer, Double> zscores = new HashMap<>();
		for (Integer period : rollingPeriods) {
			double zscore = getZscore(instrument.getPrimaryKey(), otherInstrument.getPrimaryKey(), period,
					tupleZscore);
			zscores.put(period, zscore);
		}
		return zscores;
	}

}
