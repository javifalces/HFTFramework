package com.lambda.investing.algorithmic_trading.reinforcement_learning.state;

import com.lambda.investing.TimeSeriesQueue;
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

import java.time.ZoneId;
import java.util.*;


import static com.lambda.investing.algorithmic_trading.reinforcement_learning.MatrixRoundUtils.*;

public class MarketState extends AbstractState {

    protected static boolean MARKET_MIDPRICE_RELATIVE = true;
    protected static boolean PRIVATE_QUANTITY_RELATIVE = true;
    protected static boolean PRIVATE_DELTA_STATES = true;
    public static boolean REMOVE_PRIVATE_STATE = false;

    public static String PERIOD_CANDLE_1MIN = "1Min";

    protected static double MARKET_MAX_NUMBER = 10.;
    protected static double MARKET_MIN_NUMBER = -10.;


    public static String[] MARKET_COLUMNS_PATTERN = new String[]{"bid_price", "ask_price", "bid_qty", "ask_qty",
            "spread", "midprice", "imbalance", "microprice", "last_close_price", "last_close_qty"};

    public static String[] CANDLE_COLUMNS_PATTERN = new String[]{"open", "high", "low", "close"};

    protected static String[] CANDLE_INDICATORS = new String[]{"ma", "std", "max", "min"};

    public static String[] PRIVATE_COLUMNS = new String[]{"inventory", "unrealized", "realized"};
    public static String[] INDIVIDUAL_COLUMNS = new String[]{"minutesToFinish"};

    private double lastCandlesMA, lastCandleStd, lastCandleMax, lastCandleMin = Double.NaN;
    //private buffer
    private ScoreEnum scoreEnumColumn;
    protected int privateHorizonSave, marketHorizonSave, candleHorizonSave;
    protected long privateTickMs, marketTickMs;
    private long lastPrivateTickSave, lastMarketTickSave;

    private int privateNumberDecimals, marketNumberDecimals, candleNumberDecimals;
    private double privateMinNumber, privateMaxNumber, marketMinNumber, marketMaxNumber, candleMinNumber, candleMaxNumber;
    //0 is the oldest , last element is the newest
    private TimeSeriesQueue<Double> inventoryBuffer, scoreBuffer, unrealizedPnlBuffer, realizedPnlBuffer;
    private double quantity;

    //market buffer 0 is the oldest , last element is the newest
    private TimeSeriesQueue<Double> bidPriceBuffer, askPriceBuffer, bidQtyBuffer, askQtyBuffer, spreadBuffer, midpriceBuffer, imbalanceBuffer, micropriceBuffer, lastClosePriceBuffer, lastCloseQuantityBuffer;

    private double lastAbsoluteClose;

    private TimeSeriesQueue<Double> candlesOpen;
    private TimeSeriesQueue<Double> candlesHigh;
    private TimeSeriesQueue<Double> candlesLow;
    private TimeSeriesQueue<Double> candlesClose;
    private CandleType candleType;
    private boolean disableLastClose = false;
    protected Instrument instrument;
    protected int lastHourOperatingIncluded;

    protected long lastTimestamp = 0;
    protected double lastMidPrice = 0.0;
    protected Depth lastDepth = null;

    public MarketState(Instrument instrument, ScoreEnum scoreEnumColumn, int privateHorizonSave, int marketHorizonSave,
                       int candleHorizonSave, long privateTickMs, long marketTickMs, int privateNumberDecimals,
                       int marketNumberDecimals, int candleNumberDecimals, double privateMinNumber, double privateMaxNumber,
                       double marketMinNumber, double marketMaxNumber, double candleMinNumber, double candleMaxNumber,
                       double quantity, CandleType candleType, int lastHourOperatingIncluded) {
        super(privateNumberDecimals);
        this.instrument = instrument;
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
            CANDLE_COLUMNS_PATTERN = new String[]{"high", "low", "close"};
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
        this.lastHourOperatingIncluded = lastHourOperatingIncluded;
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
            inventoryBuffer = new TimeSeriesQueue<Double>(this.privateHorizonSave + 1);
            scoreBuffer = new TimeSeriesQueue<Double>(this.privateHorizonSave + 1);
            realizedPnlBuffer = new TimeSeriesQueue<Double>(this.privateHorizonSave + 1);
            unrealizedPnlBuffer = new TimeSeriesQueue<Double>(this.privateHorizonSave + 1);
        } else {
            inventoryBuffer = new TimeSeriesQueue<Double>(this.privateHorizonSave);
            scoreBuffer = new TimeSeriesQueue<Double>(this.privateHorizonSave);
            realizedPnlBuffer = new TimeSeriesQueue<Double>(this.privateHorizonSave);
            unrealizedPnlBuffer = new TimeSeriesQueue<Double>(this.privateHorizonSave);
        }

        int index = 0;
        while (index < this.privateHorizonSave + 1) {
            inventoryBuffer.offer(0.0);
            scoreBuffer.offer(0.0);
            realizedPnlBuffer.offer(0.0);
            unrealizedPnlBuffer.offer(0.0);
            index++;
        }

        createBuffers();
        calculateNumberOfColumns();

    }

    private void createBuffers() {
        //market
        bidPriceBuffer = new TimeSeriesQueue<Double>(this.marketHorizonSave);
        askPriceBuffer = new TimeSeriesQueue<Double>(this.marketHorizonSave);
        bidQtyBuffer = new TimeSeriesQueue<Double>(this.marketHorizonSave);
        askQtyBuffer = new TimeSeriesQueue<Double>(this.marketHorizonSave);
        spreadBuffer = new TimeSeriesQueue<Double>(this.marketHorizonSave);
        midpriceBuffer = new TimeSeriesQueue<Double>(this.marketHorizonSave);
        imbalanceBuffer = new TimeSeriesQueue<Double>(this.marketHorizonSave);
        micropriceBuffer = new TimeSeriesQueue<Double>(this.marketHorizonSave);
        lastClosePriceBuffer = new TimeSeriesQueue<Double>(this.marketHorizonSave);
        lastCloseQuantityBuffer = new TimeSeriesQueue<Double>(this.marketHorizonSave);

        //candle
        if (this.candleHorizonSave > 0) {
            candlesOpen = new TimeSeriesQueue<Double>(this.candleHorizonSave);
            candlesHigh = new TimeSeriesQueue<Double>(this.candleHorizonSave);
            candlesLow = new TimeSeriesQueue<Double>(this.candleHorizonSave);
            candlesClose = new TimeSeriesQueue<Double>(this.candleHorizonSave);
        }
    }

    public void disableLastClose() {

        this.disableLastClose = true;
        MARKET_COLUMNS_PATTERN = new String[]{"bid_price", "ask_price", "bid_qty", "ask_qty", "spread", "midprice",
                "imbalance", "microprice"};

    }

    protected void updateMaxHorizon() {
        if (this.columnsFilter != null && this.columnsFilter.length > 0) {
            boolean somethingChanged = false;
            List<String> columnsFilterList = new ArrayList<>(Arrays.asList(this.columnsFilter));
            int horizonTicksMarketState = getMaxHorizon(columnsFilterList, MarketState.MARKET_COLUMNS_PATTERN);
            if (horizonTicksMarketState > 0) {
                horizonTicksMarketState = horizonTicksMarketState + 1;
            }

            if (horizonTicksMarketState != this.marketHorizonSave) {
                logger.warn("marketHorizonSave {} is different from the one configured {}",
                        horizonTicksMarketState, this.marketHorizonSave);
                this.marketHorizonSave = horizonTicksMarketState;
                somethingChanged = true;
            }

            int horizonCandlesState = getMaxHorizon(columnsFilterList, MarketState.CANDLE_COLUMNS_PATTERN);
            if (horizonCandlesState > 0) {
                horizonCandlesState = horizonCandlesState + 1;
            }
            if (horizonCandlesState != this.candleHorizonSave) {
                logger.warn("candleHorizonSave {} is different from the one configured {}",
                        horizonCandlesState, this.candleHorizonSave);
                this.candleHorizonSave = horizonCandlesState;
                somethingChanged = true;
            }

            if (somethingChanged) {
                createBuffers();
                calculateNumberOfColumns();
            }
        }
    }

    public List<String> getPrivateColumns() {
        if (REMOVE_PRIVATE_STATE) {
            return new ArrayList<>();
        }
        List<String> output = new ArrayList<>();
        //private
        for (int colIndex = 0; colIndex < PRIVATE_COLUMNS.length; colIndex++) {
            for (int horizonTick = privateHorizonSave - 1; horizonTick >= 0; horizonTick--) {
                output.add(PRIVATE_COLUMNS[colIndex] + "_" + String.valueOf(horizonTick));
            }
        }
        return output;
    }

    public List<String> getIndividualColumns() {
        List<String> output = new ArrayList<>();
        output.addAll(Arrays.asList(INDIVIDUAL_COLUMNS));
        return output;
    }

    @Override
    public void calculateNumberOfColumns() {
        setNumberOfColumns(getColumns().size());
    }

    @Override
    public List<String> getFilteredColumns() {
        if (isFiltered()) {
            //private
            List<String> output = getPrivateColumns();
            //individual
            output.addAll(getIndividualColumns());
            //filtered market states
            List<String> columnsFilteredList = new ArrayList<>(List.of(columnsFilter));
            output.addAll(columnsFilteredList);
            return columnsFilteredList;
        } else {
            return getColumns();
        }

    }

    @Override
    public List<String> getColumns() {
        //private
        List<String> output = getPrivateColumns();
        //individual
        output.addAll(getIndividualColumns());


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

    @Override
    public int getNumberStates() {
        logger.warn("we are asking number of states to marketState!!");
        return Integer.MAX_VALUE;
    }

    @Override
    public synchronized void reset() {
        logger.info("resetting market state");
        inventoryBuffer.clear();
        scoreBuffer.clear();
        unrealizedPnlBuffer.clear();
        realizedPnlBuffer.clear();
        bidPriceBuffer.clear();
        askPriceBuffer.clear();
        bidQtyBuffer.clear();
        askQtyBuffer.clear();
        spreadBuffer.clear();
        midpriceBuffer.clear();
        imbalanceBuffer.clear();
        micropriceBuffer.clear();
        lastClosePriceBuffer.clear();
        lastCloseQuantityBuffer.clear();
        candlesOpen.clear();
        candlesHigh.clear();
        candlesLow.clear();
        candlesClose.clear();
        lastTimestamp = 0;
        lastMarketTickSave = 0;
        lastPrivateTickSave = 0;
        lastCandlesMA = Double.NaN;
        lastCandleStd = Double.NaN;
        lastCandleMax = Double.NaN;
        lastCandleMin = Double.NaN;
        //fill private states
        int index = 0;
        while (index < this.privateHorizonSave + 1) {
            inventoryBuffer.offer(0.0);
            scoreBuffer.offer(0.0);
            realizedPnlBuffer.offer(0.0);
            unrealizedPnlBuffer.offer(0.0);
            index++;
        }


    }

    @Override
    public synchronized boolean isReady() {
        boolean privateIsReady = true;

        if (!REMOVE_PRIVATE_STATE) {
            ///only check it if its false
            if (PRIVATE_DELTA_STATES) {
                if (this.inventoryBuffer.size() < this.privateHorizonSave + 1
                        || this.scoreBuffer.size() < this.privateHorizonSave + 1
                        || this.unrealizedPnlBuffer.size() < this.privateHorizonSave + 1
                        || this.realizedPnlBuffer.size() < this.privateHorizonSave + 1
                ) {
                    //				logger.error("not enough states received {}< {} ", this.inventoryBuffer.size(), this.horizonSave + 1);
                    privateIsReady = false;
                }
            } else {
                if (this.inventoryBuffer.size() < this.privateHorizonSave
                        || this.scoreBuffer.size() < this.privateHorizonSave
                        || this.unrealizedPnlBuffer.size() < this.privateHorizonSave
                        || this.realizedPnlBuffer.size() < this.privateHorizonSave
                ) {
                    //				logger.error("not enough states received {}< {} ", this.inventoryBuffer.size(), this.horizonSave);
                    privateIsReady = false;
                }
            }
        }

        boolean individualIsReady = this.lastTimestamp != 0;

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
            if (this.candlesClose.size() < this.candleHorizonSave
                    || this.candlesOpen.size() < this.candleHorizonSave
                    || this.candlesHigh.size() < this.candleHorizonSave
                    || this.candlesLow.size() < this.candleHorizonSave) {
                candleIsReady = false;
            }
        }

        //		if (!privateIsReady && candleIsReady && marketIsReady) {
        //			logger.warn("private states initially not received-> set 0.0");
        //			for (int horizon = 0; horizon < this.privateHorizonSave; horizon++) {
        //				this.inventoryBuffer.add(0.0);
        //				this.scoreBuffer.add(0.0);
        //			}
        //			privateIsReady = true;
        //		}

        return candleIsReady && marketIsReady && privateIsReady && individualIsReady;

    }

    /**
     * Update current candle with the last price
     *
     * @param price
     */
    private void updateCandleTemp(double price) {
        if (this.candleHorizonSave <= 0) {
            return;
        }

        if (candlesClose.size() < this.candleHorizonSave) {
            return;
        }

        double close = price;
        double open = lastAbsoluteClose;
        if (MARKET_MIDPRICE_RELATIVE) {
            open = 0.0;
            close = (price / lastAbsoluteClose) - 1.;
        }
        candlesClose.changeNewest(close);
        candlesOpen.changeNewest(open);


        double lastHigh = candlesHigh.getNewest();
        candlesHigh.changeNewest(Math.max(lastHigh, close));

        double lastLow = candlesLow.getNewest();
        candlesLow.changeNewest(Math.min(lastLow, close));

        if (candlesClose.size() >= candleHorizonSave) {
            Double[] highCandlesDouble = new Double[candleHorizonSave];
            candlesHigh.toArray(highCandlesDouble);
            double[] highCandles = ArrayUtils.toPrimitive(highCandlesDouble);
            lastCandleMax = maxValue(highCandles);

            Double[] lowCandlesDouble = new Double[candleHorizonSave];
            candlesLow.toArray(lowCandlesDouble);
            double[] lowCandles = ArrayUtils.toPrimitive(lowCandlesDouble);
            lastCandleMin = minValue(lowCandles);

        }

    }

    @Override
    public void updateCandle(Candle candle) {
        //last update on last element
        if (this.candleHorizonSave <= 0) {
            return;
        }
        if (!candle.getCandleType().equals(this.candleType)) {
            return;
        }
        if (!candle.getInstrumentPk().equals(instrument.getPrimaryKey())) {
            return;
        }

        double open = candle.getOpen();
        double high = candle.getHigh();
        double low = candle.getLow();
        double close = candle.getClose();
        lastAbsoluteClose = close;


        //relative candles
        if (MARKET_MIDPRICE_RELATIVE) {
            high = (high / open) - 1.;
            low = (low / open) - 1.;
            close = (close / open) - 1.;
            open = 0.;///not deleting!
        }

        candlesOpen.offer(open);
        candlesHigh.offer(high);
        candlesLow.offer(low);
        candlesClose.offer(close);



    }

    @Override
    public synchronized void updateTrade(Trade trade) {
        if (!disableLastClose) {
            if (!trade.getInstrument().equals(instrument.getPrimaryKey())) {
                return;
            }
            if (trade.getTimestamp() > lastTimestamp) {
                lastTimestamp = trade.getTimestamp();
            }
            double price = trade.getPrice();
            if (MARKET_MIDPRICE_RELATIVE) {
                price = (trade.getPrice() / lastMidPrice) - 1;
            }
            lastClosePriceBuffer.offer(price);
            lastCloseQuantityBuffer.offer(trade.getQuantity());
        }
    }

    @Override
    public synchronized void updatePrivateState(PnlSnapshot pnlSnapshot) {
        if ((pnlSnapshot.getLastTimestampUpdate() - lastPrivateTickSave) < privateTickMs) {
            //not enough time to save it
            return;
        }

        double score = ScoreUtils.getReward(this.scoreEnumColumn, pnlSnapshot);
        if (PRIVATE_QUANTITY_RELATIVE) {
            score = score / quantity;
        }
        scoreBuffer.offer(score);

        unrealizedPnlBuffer.offer(pnlSnapshot.unrealizedPnl / quantity);
        realizedPnlBuffer.offer(pnlSnapshot.realizedPnl / quantity);

        double position = pnlSnapshot.netPosition;
        if (PRIVATE_QUANTITY_RELATIVE) {
            position = position / quantity;
        }
        inventoryBuffer.offer(position);

        lastPrivateTickSave = pnlSnapshot.getLastTimestampUpdate();
    }

    private int getTt(long timestamp) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("UTC")));
        calendar.setTimeInMillis(timestamp);

        int currentTimeMins = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        int lastHourMins = (lastHourOperatingIncluded + 1) * 60;
        int output = lastHourMins - currentTimeMins;
        return output;
    }

    @Override
    public synchronized void updateDepthState(Depth depth) {
        if ((depth.getTimestamp() - lastMarketTickSave) < marketTickMs) {
            //not enough time to save it
            return;
        }

        if (!depth.getInstrument().equals(instrument.getPrimaryKey())) {
            return;
        }

        if (depth.getTimestamp() > lastTimestamp) {
            lastTimestamp = depth.getTimestamp();
        }

        //		Instrument instrument = Instrument.getInstrument(depth.getInstrument());
        //		if (instrument.isFX()) {
        //			//set all quantities of depth to zero just in case of backtest fill memory replay as in pro
        //			depth.setAllQuantitiesZero();
        //		}
        double bid = depth.getBestBid();
        double bidQty = 0;
        double askQty = 0;
        if (lastDepth != null) {
            bidQty = depth.getBestBidQty() - lastDepth.getBestBidQty();
            askQty = depth.getBestAskQty() - lastDepth.getBestAskQty();
        }
        double ask = depth.getBestAsk();
        double midPrice = depth.getMidPrice();
        double microPrice = depth.getMicroPrice();
        double imbalance = depth.getImbalance();
        double spread = depth.getSpread();
        lastMidPrice = midPrice;
        if (MARKET_MIDPRICE_RELATIVE) {
            bid = Math.abs(bid / midPrice) - 1;
            ask = Math.abs(ask / midPrice) - 1;
            microPrice = Math.abs(microPrice / midPrice) - 1;
        }
        midpriceBuffer.offer(midPrice);
        spreadBuffer.offer(spread);
        bidPriceBuffer.offer(bid);
        bidQtyBuffer.offer(bidQty);
        askPriceBuffer.offer(ask);
        askQtyBuffer.offer(askQty);
        micropriceBuffer.offer(microPrice);
        imbalanceBuffer.offer(imbalance);

        lastMarketTickSave = depth.getTimestamp();
        lastDepth = depth;
        updateCandleTemp(midPrice);
    }

    private double[] getPrivateState() {
        if (REMOVE_PRIVATE_STATE) {
            return new double[1];
        }
        List<Double> outputList = new ArrayList<>(this.numberOfColumns);
        if (PRIVATE_DELTA_STATES) {
            outputList.addAll(getDiffQueue(inventoryBuffer));
            //			outputList.addAll(getDiffQueue(scoreBuffer));
            outputList.addAll(getDiffQueue(unrealizedPnlBuffer));
            outputList.addAll(getDiffQueue(realizedPnlBuffer));
        } else {
            outputList.addAll(inventoryBuffer);
            //			outputList.addAll(scoreBuffer);
            outputList.addAll(unrealizedPnlBuffer);
            outputList.addAll(realizedPnlBuffer);
        }
        if (outputList.isEmpty()) {
            return null;
        }


        double[] outputArr = outputList.stream().mapToDouble(Double::doubleValue).toArray();
        outputArr = getRoundedState(outputArr, privateMaxNumber, privateMinNumber, privateNumberDecimals);
        return outputArr;
    }

    private double[] getIndividualState() {
        int minutesToFinish = getTt(lastTimestamp);
        List<Double> outputList = new ArrayList<>();

        outputList.add((double) minutesToFinish);
        return com.lambda.investing.ArrayUtils.DoubleListToPrimitiveArray(outputList);
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
        if (outputList.isEmpty()) {
            return null;
        }
        double[] outputArr = com.lambda.investing.ArrayUtils.DoubleListToPrimitiveArray(outputList);//.stream().mapToDouble(Double::doubleValue).toArray();
        outputArr = getRoundedState(outputArr, marketMaxNumber, marketMinNumber, marketNumberDecimals);
        return outputArr;

    }

    private double[] getCandleState() {
        List<Double> outputList = new ArrayList<>(this.numberOfColumns);

        outputList.addAll(candlesClose);
        if (!MARKET_MIDPRICE_RELATIVE) {
            //dont add it ...is all zero
            outputList.addAll(candlesOpen);
        }
        outputList.addAll(candlesHigh);
        outputList.addAll(candlesLow);

        Double[] closeCandlesDouble = new Double[candleHorizonSave];
        candlesClose.toArray(closeCandlesDouble);
        double[] closeCandles = ArrayUtils.toPrimitive(closeCandlesDouble);

        lastCandlesMA = meanValue(closeCandles);
        lastCandleStd = stdValue(closeCandles);


        outputList.add(lastCandlesMA);
        outputList.add(lastCandleStd);

        Double[] highCandlesDouble = new Double[candleHorizonSave];
        candlesHigh.toArray(highCandlesDouble);
        double[] highCandles = ArrayUtils.toPrimitive(highCandlesDouble);
        lastCandleMax = maxValue(highCandles);

        Double[] lowCandlesDouble = new Double[candleHorizonSave];
        candlesLow.toArray(lowCandlesDouble);
        double[] lowCandles = ArrayUtils.toPrimitive(lowCandlesDouble);
        lastCandleMin = minValue(lowCandles);

        outputList.add(lastCandleMax);
        outputList.add(lastCandleMin);

        if (outputList.isEmpty()) {
            return null;
        }
        double[] outputArr = com.lambda.investing.ArrayUtils.DoubleListToPrimitiveArray(outputList);
        ;
        outputArr = getRoundedState(outputArr, candleMaxNumber, candleMinNumber, candleNumberDecimals);
        return outputArr;
    }

    @Override
    public synchronized double[] getCurrentStateRounded() {
        //returns it rounded!
        if (!isReady()) {
            logger.error("not enough market states received");
            return null;
        }
        double[] privateState = null;
        double[] individualStates = null;
        double[] marketState = null;
        double[] candleState = null;
        try {
            privateState = getPrivateState();
            individualStates = getIndividualState();
            marketState = getMarketState();
            if (this.candleHorizonSave > 0) {
                candleState = getCandleState();
            } else {
                candleState = new double[]{0.0};
            }

        } catch (Exception e) {
            logger.error("error getting state", e);
        }
        if (privateState == null || individualStates == null || marketState == null || candleState == null) {
            logger.error("something is wrong when one states is null and is ready");
            return null;
        }

        privateState = getRoundedState(privateState, privateMaxNumber, privateMinNumber, privateNumberDecimals);
        marketState = getRoundedState(marketState, marketMaxNumber, marketMinNumber, marketNumberDecimals);
        if (this.candleHorizonSave > 0) {
            candleState = getRoundedState(candleState, candleMaxNumber, candleMinNumber, candleNumberDecimals);
        }

        double[] output = null;
        if (REMOVE_PRIVATE_STATE) {
            output = individualStates;
        } else {
            output = ArrayUtils.addAll(privateState, individualStates);
        }

        output = ArrayUtils.addAll(output, marketState);

        if (this.candleHorizonSave > 0) {
            output = ArrayUtils.addAll(output, candleState);
        }
        assert output.length == numberOfColumns;

        output = getFilteredState(output);//get filter
        return output;

    }

    //Sort it
    @Override
    public synchronized double[] getCurrentState() {
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
                candleState = new double[]{0.0};
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

    @Override
    public int getCurrentStatePosition() {
        double[] currentStateArr = getCurrentStateRounded();//filtered
        return getStateFromArray(currentStateArr);
    }

    protected double[] getRoundedState(double[] state) {
        //here returs the same
        return state;
    }

    @Override
    public void enumerateStates(String cachePermutationsPath) {

    }

}
