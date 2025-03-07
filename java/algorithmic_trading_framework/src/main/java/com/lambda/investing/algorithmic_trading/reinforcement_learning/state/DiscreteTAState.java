package com.lambda.investing.algorithmic_trading.reinforcement_learning.state;

import com.google.common.primitives.Doubles;
import com.lambda.investing.Configuration;
import com.lambda.investing.TimeSeriesQueue;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.TimeService;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreEnum;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import org.apache.commons.lang3.ArrayUtils;
import java.util.*;

import static com.lambda.investing.algorithmic_trading.technical_indicators.Calculator.*;

public class DiscreteTAState extends AbstractState {

    //every new state must be added on python too on python_lambda/backtest/rsi_dqn.py: _get_ta_state_columns
    public static boolean BINARY_STATE_OUTPUTS = false;
    private static boolean MARKET_MIDPRICE_RELATIVE = true;
    protected static String[] STATE_COLUMNS_PATTERN = new String[]{
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

    protected static String[] MARKET_CONDITIONS_ON_MAX_PERIOD_CANDLES = new String[]{
            //queueCandles
            "signedTransactionVolume_", "signedTransaction_", "microprice_candle_", "vpin_candle_"};

    protected static String[] BINARY_STATE_COLUMNS_PATTERN = new String[]{
            ////price
            "microprice_",//midpricee-microprice
            "vpin_", "rsi_",//rsi<30 -> 1 rsi>70->-1 else 0
            "sma_",//sma<price -> -1 sma>=price->1
            "ema_",//sma<price -> -1 sma>=price->1
            "max_",//price>max=1 else 0
            "min_",//price>max=1 else 0

    };

    protected static String[] MARKET_COLUMNS_PATTERN = new String[]{
            //last ticks properties
            "bid_price_", "ask_price_", "bid_qty_", "ask_qty_", "spread_", "imbalance_", "microprice_"

    };
    protected static String[] STATE_SINGLE_COLUMNS = new String[]{"hour_of_the_day_utc", "minutes_from_start",
            "volume_from_start"};
    protected static String[] BINARY_STATE_SINGLE_COLUMNS = new String[]{"hour_of_the_day_utc", "minutes_from_start"};

    protected int marketHorizonSave = 15;
    protected long marketTickMs = 10;
    private long lastMarketTickSave;
    private double volumeFromStart = 0.0;

    protected static int[] defaultPeriods = new int[]{3, 5, 7, 9, 11, 13, 15, 17, 21};///used as reference
    protected static int[] binaryDefaultPeriods = new int[]{9};///used as reference

    protected static int[] rsiPeriods = defaultPeriods;
    private static int[] smaPeriods = defaultPeriods;
    private static int[] emaPeriods = defaultPeriods;
    private static int[] maxPeriods = defaultPeriods;
    private static int[] minPeriods = defaultPeriods;

    public static int maxPeriod = defaultPeriods[defaultPeriods.length - 1];

    private CandleType candleType;

    protected ScoreEnum scoreEnumColumn;

    protected Depth lastDepth;
    protected Trade lastTrade;
    //last element will be on last element of the queue!! older is on 0
    protected TimeSeriesQueue<Candle> queueCandles;
    protected TimeSeriesQueue<Double> queueCandlesOpenPrices;
    protected TimeSeriesQueue<Double> queueCandlesClosePrices;
    protected TimeSeriesQueue<Double> queueCandlesMaxPrices;
    protected TimeSeriesQueue<Double> queueCandlesMinPrices;

    protected TimeSeriesQueue<Double> queueCandlesOpenVolume;
    protected TimeSeriesQueue<Double> queueCandlesCloseVolume;
    protected TimeSeriesQueue<Double> queueCandlesHighVolume;
    protected TimeSeriesQueue<Double> queueCandlesLowVolume;

    protected TimeSeriesQueue<Double> queueCandlesSignedTransactionVolume;
    protected TimeSeriesQueue<Double> queueCandlesSignedTransaction;


    protected TimeSeriesQueue<Double> queueCandlesMicroPricesSpreads;
    protected TimeSeriesQueue<Double> queueCandlesVPIN;

    protected TimeSeriesQueue<Double> bidPriceBuffer, askPriceBuffer, bidQtyBuffer, askQtyBuffer, spreadBuffer, imbalanceBuffer, micropriceBuffer;

    //	protected Queue<Double> queueCandlesRelativeBidAskQtyDiff;
    protected Instrument instrument = Instrument.getInstrument("eurusd_darwinex");

    private TimeService timeService;


    public static DiscreteTAState create(Instrument instrument, ScoreEnum scoreEnumColumn, CandleType candleType, int[] periods,
                                         int numberOfDecimals, int marketHorizonSave) {

        if (instrument != null && instrument.isFX()) {
            System.out.println(Configuration.formatLog("{} DiscreteTAState detected as FX -> DiscreteTAStateFX", instrument.getPrimaryKey()));
            return new DiscreteTAStateFX(instrument, scoreEnumColumn, candleType, periods, numberOfDecimals, marketHorizonSave);
        } else {
//			System.out.println(Configuration.formatLog("{} DiscreteTAState detected as not FX",instrument.getPrimaryKey()));
            return new DiscreteTAState(instrument, scoreEnumColumn, candleType, periods, numberOfDecimals, marketHorizonSave);
        }

    }

    protected DiscreteTAState(Instrument instrument, ScoreEnum scoreEnumColumn, CandleType candleType, int[] periods,
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

        queueCandles = new TimeSeriesQueue<>(maxPeriod);
        timeService = new TimeService("UTC");
        queueCandlesOpenPrices = new TimeSeriesQueue<>(maxPeriod);
        queueCandlesClosePrices = new TimeSeriesQueue<>(maxPeriod);
        queueCandlesMaxPrices = new TimeSeriesQueue<>(maxPeriod);
        queueCandlesMinPrices = new TimeSeriesQueue<>(maxPeriod);
        queueCandlesMicroPricesSpreads = new TimeSeriesQueue<>(maxPeriod);
        queueCandlesVPIN = new TimeSeriesQueue<>(maxPeriod);
        //		queueCandlesRelativeBidAskQtyDiff = new TimeSeriesQueue<>(maxPeriod);
        queueCandlesOpenVolume = new TimeSeriesQueue<>(maxPeriod);
        queueCandlesCloseVolume = new TimeSeriesQueue<>(maxPeriod);
        queueCandlesHighVolume = new TimeSeriesQueue<>(maxPeriod);
        queueCandlesLowVolume = new TimeSeriesQueue<>(maxPeriod);

        queueCandlesSignedTransaction = new TimeSeriesQueue<>(maxPeriod);
        queueCandlesSignedTransactionVolume = new TimeSeriesQueue<>(maxPeriod);
        //market

        bidPriceBuffer = new TimeSeriesQueue<>(this.marketHorizonSave);
        askPriceBuffer = new TimeSeriesQueue<>(this.marketHorizonSave);
        bidQtyBuffer = new TimeSeriesQueue<>(this.marketHorizonSave);
        askQtyBuffer = new TimeSeriesQueue<>(this.marketHorizonSave);
        spreadBuffer = new TimeSeriesQueue<>(this.marketHorizonSave);
        imbalanceBuffer = new TimeSeriesQueue<>(this.marketHorizonSave);
        micropriceBuffer = new TimeSeriesQueue<>(this.marketHorizonSave);

        volumeFromStart = 0.0;

    }


    public void setMarketTickMs(long marketTickMs) {
        this.marketTickMs = marketTickMs;
    }

    @Override
    public synchronized void reset() {
        logger.info("resetting DiscreteTAState");
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

    private List<String> getCandleColumns() {
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
        return outputColumns;
    }

    private List<String> getMarketColumns() {
        List<String> outputColumns = new ArrayList<>();
        if (marketHorizonSave > 0) {
            for (String marketColumn : MARKET_COLUMNS_PATTERN) {
                for (int lag = 0; lag < marketHorizonSave; lag++) {
                    outputColumns.add(marketColumn + lag);
                }
            }
        }
        return outputColumns;
    }

    private List<String> getSingleColumns() {
        List<String> outputColumns = new ArrayList<>();
        if (!BINARY_STATE_OUTPUTS) {
            //single state
            outputColumns.addAll(Arrays.asList(STATE_SINGLE_COLUMNS));
        } else {
            outputColumns.addAll(Arrays.asList(BINARY_STATE_SINGLE_COLUMNS));
        }
        return outputColumns;
    }

    @Override
    public List<String> getColumns() {
        List<String> outputColumns = new ArrayList<>();

        outputColumns.addAll(getCandleColumns());
        outputColumns.addAll(getMarketColumns());
        outputColumns.addAll(getSingleColumns());

        return outputColumns;
    }

    @Override
    public int getNumberStates() {
        logger.warn("we are asking number of states to DiscreteTAState!!");
        return 6666;//2^(6)*(3^3)
    }

    @Override
    public synchronized boolean isReady() {
        return (queueCandles.size() == maxPeriod) && bidPriceBuffer.size() == marketHorizonSave && this.getCurrentState().length == this.getNumberOfColumns();
    }

    @Override
    public double[] getCurrentState() {
        List<Double> candlesState = getCurrentCandlesState();
        List<Double> marketState = getCurrentMarketState();
        if (marketState == null) {
            logger.error("marketState is null-> return null");
            return null;
        }
        List<Double> candleMarketState = getLastCandlesStates();

        List<Double> singleState = getSingleState();
        List<Double> outputList = new ArrayList(candlesState);
        outputList.addAll(candleMarketState);
        outputList.addAll(marketState);
        outputList.addAll(singleState);
        if ((getClass().equals(DiscreteTAState.class) || getClass().equals(DiscreteTAStateFX.class)) && outputList.size() != this.numberOfColumns) {
            logger.warn("getCurrentState with output size = {} != {} numberOfColumns ", outputList.size(), this.numberOfColumns);
        }

        double[] outputArr = outputList.stream().mapToDouble(Double::doubleValue).toArray();
        return outputArr;
    }

    protected List<Double> getLastCandlesStates() {
        List<Double> stateList = new ArrayList<>();
        //add Signed transaction
        stateList.addAll(queueCandlesSignedTransactionVolume);
        stateList.addAll(queueCandlesSignedTransaction);
        stateList.addAll(queueCandlesMicroPricesSpreads);
        stateList.addAll(queueCandlesVPIN);
        return stateList;
    }

    protected List<Double> getCurrentMarketState() {
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

    protected List<Double> getSingleState() {
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

    protected List<Double> getCurrentCandlesState() {
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
        double lastMidVol = 0;
        try {
            if (queueCandlesCloseVolume != null && !queueCandlesCloseVolume.isEmpty()) {
                lastMidVol = queueCandlesCloseVolume.getNewest();
            }

        } catch (Exception e) {
        }

        double lastMid = lastDepth.getMidPrice();

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

    protected double getRsiIndicator(int period, Double[] inputArr) {
        double rsiValue = RSICalculate(ArrayUtils.toPrimitive(inputArr), period);
        double indicator = Round(rsiValue / 10, numberOfDecimals);
        indicator -= 5;//centedered on 0
        //binary output
        if (BINARY_STATE_OUTPUTS) {
            //						double scaler=10.0/5.0;
            //						indicator = ((indicator - 5.0) / 2.0);
            //						indicator = Math.round(indicator * 20) / 20;

            if (indicator < -2) {//centered on 0
                indicator = 1;
            } else if (indicator > 2) {
                indicator = -1;
            } else {
                indicator = 0;
            }
        }
        return indicator;
    }

    protected double getSmaIndicator(int period, Double[] inputArr) {
        double lastMid = inputArr[inputArr.length - 1];
        double smaValue = SMACalculate(ArrayUtils.toPrimitive(inputArr), period);
        double distanceToSma = lastMid - smaValue;
        double ticksDistance = distanceToSma / instrument.getPriceTick();//TODO take a look on volume!!

        double indicator = Round(ticksDistance, numberOfDecimals);

        if (BINARY_STATE_OUTPUTS) {
            if (indicator < 0) {
                indicator = 1;
            } else {
                indicator = -1;
            }
        }
        return indicator;
    }

    protected double getEmaIndicator(int period, Double[] inputArr) {
        double lastMid = inputArr[inputArr.length - 1];
        double emaValue = EMACalculate(ArrayUtils.toPrimitive(inputArr), period);
        double distanceToEma = lastMid - emaValue;
        double ticksDistance = distanceToEma / instrument.getPriceTick();//TODO take a look on volume!!

        double indicator = Round(ticksDistance, numberOfDecimals);

        if (BINARY_STATE_OUTPUTS) {
            if (indicator < 0) {
                indicator = 1;
            } else {
                indicator = -1;
            }
        }
        return indicator;
    }

    protected double getMaxIndicator(int period, Double[] inputArr, double lastMid) {
        try {
            double[] highPeriod = Arrays
                    .copyOfRange(ArrayUtils.toPrimitive(inputArr), inputArr.length - period, inputArr.length);
            double resistance = Doubles.max(highPeriod);

            double distanceToResist = lastMid - resistance;
            double ticksDistance = distanceToResist / instrument.getPriceTick();//TODO take a look on volume!!
//		double indicator = Math.round(ticksDistance);
            double indicator = Round(ticksDistance, numberOfDecimals);

            if (BINARY_STATE_OUTPUTS) {
                if (indicator < 0) {
                    indicator = -1;
                } else {
                    indicator = 1;
                }
            }
            return indicator;
        } catch (Exception e) {
            logger.error("error getting maxIndicator lastMid:{} period:{} maxPeriod:{} isReady:{}", lastMid, period, maxPeriod, isReady(), e);
            throw e;
        }

    }

    protected double getMinIndicator(int period, Double[] inputArr, double lastMid) {
        double[] lowPeriod = Arrays
                .copyOfRange(ArrayUtils.toPrimitive(inputArr), inputArr.length - period, inputArr.length);
        double support = Doubles.min(lowPeriod);

        double distanceToSupport = lastMid - support;
        double ticksDistance = distanceToSupport / instrument.getPriceTick();//TODO take a look on volume!!
//		double indicator = Math.round(ticksDistance);
        double indicator = Round(ticksDistance, numberOfDecimals);

        if (BINARY_STATE_OUTPUTS) {
            if (indicator < 0) {
                indicator = -1;
            } else {
                indicator = 1;
            }
        }
        return indicator;
    }

    @Override
    public int getCurrentStatePosition() {
        double[] currentStateArr = getCurrentStateRounded();//filtered
        return getStateFromArray(currentStateArr);
    }

    @Override
    public void enumerateStates(String cachePermutationsPath) {

    }

    public void updateCandleTemp(double price) {
        if (queueCandlesClosePrices.size() < maxPeriod) {
            return;
        }
        synchronized (this) {
            queueCandlesClosePrices.changeNewest(price);

            double lastHigh = queueCandlesMaxPrices.getNewest();
            queueCandlesMaxPrices.changeNewest(Math.max(lastHigh, price));

            double lastLow = queueCandlesMinPrices.getNewest();
            queueCandlesMinPrices.changeNewest(Math.min(lastLow, price));

            queueCandlesMicroPricesSpreads.changeNewest(calculateMicropriceSpread());
            queueCandlesVPIN.changeNewest(lastDepth.getVPIN());
        }

    }

    @Override
    public void updateCandle(Candle candle) {
        if (!candle.getCandleType().equals(candleType) || lastDepth == null) {
            return;
        }

        synchronized (this) {
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

            cleanTrades();//reset trades buffering
        }

    }

    protected double calculateMicropriceSpread() {
        double currentMicroSpreadDiff = (lastDepth.getMicroPrice() / lastDepth.getMidPrice()) - 1;
        currentMicroSpreadDiff = Math.round(currentMicroSpreadDiff * 10) / 10.0;//only one decimal permitted
        return currentMicroSpreadDiff;
    }

    @Override
    public void updateTrade(Trade trade) {
        //
        updateTradesBuffer(trade);
        lastTrade = trade;
    }

    @Override
    public void updatePrivateState(PnlSnapshot pnlSnapshot) {
    }

    @Override
    public void updateDepthState(Depth depth) {

        timeService.setCurrentTimestamp(depth.getTimestamp());
        volumeFromStart += depth.getTotalVolume();
        if ((depth.getTimestamp() - lastMarketTickSave) < marketTickMs) {
            //not enough time to save it
            return;
        }
        double bid = depth.getBestBid();
        double ask = depth.getBestAsk();

        double bidQty = 0;
        double askQty = 0;
        if (lastDepth != null) {
            bidQty = depth.getBestBidQty() - lastDepth.getBestBidQty();
            askQty = depth.getBestAskQty() - lastDepth.getBestAskQty();
        }
        double midPrice = depth.getMidPrice();
        double microPrice = depth.getMicroPrice();
        double imbalance = depth.getImbalance();
        double spread = depth.getSpread();
        if (MARKET_MIDPRICE_RELATIVE) {
            bid = Math.abs(bid / midPrice) - 1;
            ask = Math.abs(ask / midPrice) - 1;
            microPrice = Math.abs(microPrice / midPrice) - 1;
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
        lastDepth = depth;
        updateCandleTemp(midPrice);
    }
}
