package com.lambda.investing.model.market_data;

import com.alibaba.fastjson2.annotation.JSONField;
import com.lambda.investing.ArrayUtils;
import com.lambda.investing.model.CSVable;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

import static com.lambda.investing.ArrayUtils.ArrayReverse;
import static com.lambda.investing.model.Util.*;

@Getter
@Setter
public class Depth extends CSVable implements Cloneable {

    public static double DEFAULT_VALUE = Double.NaN;


    public static String ALGORITHM_INFO_MM = "MarketMaker_Parquet";

    private static Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    public static int MAX_DEPTH = 5;
    public static int MAX_DEPTH_CSV = 5;

    //	private transient Instrument instrument;
    private String instrument;

    private long timestamp;//set from exchange if possible
    private long timestampBrokerConnector;// set in AbstractMarketDataConnectorPublisher.notifyDepth just before publish
    private long timestampAlgoConnector;//set in ZeroMqMarketDataConnector.onUpdate // OrdinaryMarketDataProvider.onUpdate
    private long timestampStrategy;//set in Algorithm.OnDepthUpdate when received


    private double[] bidsQuantities, asksQuantities, bids, asks;

    private List<String>[] bidsAlgorithmInfo, asksAlgorithmInfo;//just for backtesting


    private int levels, askLevels, bidLevels;//best is 0 , worst usually is 4


    private long timeToNextUpdateMs = Long.MIN_VALUE;

    private static DepthPool DEPTH_POOL = new DepthPool();

    public static boolean isDefaultValue(double value) {
        return Double.isNaN(value);
    }

    public static Depth getInstance() {
        return new Depth();
    }

    public static synchronized Depth getInstancePool() {
        Depth depth = DEPTH_POOL.checkOut();
        depth.reset();
        return depth;
    }

    public void delayTimestamp(long delay) {
        if (delay <= 0)
            return;
        this.timestamp += delay;
        this.timeToNextUpdateMs -= delay;
        if (this.timeToNextUpdateMs < 0) {
            this.timeToNextUpdateMs = 0;
        }
    }

    public static Depth removeLevel(Depth depth, double price, Verb verb, double deltaDiffThreshold) {
        if (verb == Verb.Buy) {
            //find index to remove depth.getBids()
            double[] bids = depth.getBids();
            int indexToRemove = java.util.stream.IntStream.range(0, bids.length)
                    .filter(i -> Math.abs(bids[i] - price) < deltaDiffThreshold)
                    .findFirst()
                    .orElse(-1);

            if (indexToRemove == -1) {
                //not found
                return depth;
            }
            //copy arrays without that index
            int bidLevels = Math.max(depth.getBidLevels() - 1, 0);
            double[] newBids = new double[bidLevels];
            double[] newBidQty = new double[bidLevels];
            System.arraycopy(depth.getBids(), 0, newBids, 0, indexToRemove);
            System.arraycopy(depth.getBidsQuantities(), 0, newBidQty, 0, indexToRemove);
            System.arraycopy(depth.getBids(), indexToRemove + 1, newBids, indexToRemove, depth.getBidLevels() - indexToRemove - 1);
            System.arraycopy(depth.getBidsQuantities(), indexToRemove + 1, newBidQty, indexToRemove, depth.getBidLevels() - indexToRemove - 1);
            depth.setBids(newBids);
            depth.setBidsQuantities(newBidQty);
            depth.setBidLevels(newBids.length);
            return depth;
        } else {
            double[] asks = depth.getAsks();
            int indexToRemove = java.util.stream.IntStream.range(0, asks.length)
                    .filter(i -> Math.abs(asks[i] - price) < deltaDiffThreshold)
                    .findFirst()
                    .orElse(-1);

            if (indexToRemove == -1) {
                //not found
                return depth;
            }
            //copy arrays without that index
            int askLevels = Math.max(depth.getAskLevels() - 1, 0);
            double[] newAsks = new double[askLevels];
            double[] newAskQty = new double[askLevels];
            System.arraycopy(depth.getAsks(), 0, newAsks, 0, indexToRemove);
            System.arraycopy(depth.getAsksQuantities(), 0, newAskQty, 0, indexToRemove);
            System.arraycopy(depth.getAsks(), indexToRemove + 1, newAsks, indexToRemove, depth.getAskLevels() - indexToRemove - 1);
            System.arraycopy(depth.getAsksQuantities(), indexToRemove + 1, newAskQty, indexToRemove, depth.getAskLevels() - indexToRemove - 1);
            depth.setAsks(newAsks);
            depth.setAsksQuantities(newAskQty);
            depth.setAskLevels(newAsks.length);
            return depth;
        }
    }

    private static double[] limitArray(double[] array, int maxLength) {
        double[] limited = new double[maxLength];
        System.arraycopy(array, 0, limited, 0, maxLength);
        return limited;
    }

    public static Depth insertNewLevel(Depth depth, double price, double size, Verb verb) {
        if (verb == Verb.Buy) {
            double[] newBids = new double[depth.getBidLevels() + 1];
            double[] newBidQty = new double[depth.getBidLevels() + 1];
            newBids[0] = price;
            newBidQty[0] = size;
            System.arraycopy(depth.getBids(), 0, newBids, 1, depth.getBidLevels());
            System.arraycopy(depth.getBidsQuantities(), 0, newBidQty, 1, depth.getBidLevels());

            if (newBids.length > Depth.MAX_DEPTH) {
                newBids = limitArray(newBids, Depth.MAX_DEPTH);
                newBidQty = limitArray(newBidQty, Depth.MAX_DEPTH);
            }

            depth.setBids(newBids);
            depth.setBidsQuantities(newBidQty);
            depth.setBidLevels(newBids.length);
            return depth;
        } else {
            double[] newAsks = new double[depth.getAskLevels() + 1];
            double[] newAskQty = new double[depth.getAskLevels() + 1];
            newAsks[0] = price;
            newAskQty[0] = size;
            System.arraycopy(depth.getAsks(), 0, newAsks, 1, depth.getAskLevels());
            System.arraycopy(depth.getAsksQuantities(), 0, newAskQty, 1, depth.getAskLevels());

            if (newAsks.length > Depth.MAX_DEPTH) {
                newAsks = limitArray(newAsks, Depth.MAX_DEPTH);
                newAskQty = limitArray(newAskQty, Depth.MAX_DEPTH);
            }

            depth.setAsks(newAsks);
            depth.setAsksQuantities(newAskQty);
            depth.setAskLevels(newAsks.length);
            return depth;
        }
    }


    public static Depth copyFromWithoutPool(Depth depth) {
        Depth newDepth = getInstance();
        newDepth.setInstrument(depth.getInstrument());
        newDepth.setTimestamp(depth.getTimestamp());
        newDepth.setTimestampStrategy(depth.getTimestampStrategy());
        newDepth.setTimestampBrokerConnector(depth.getTimestampBrokerConnector());
        newDepth.setTimestampAlgoConnector(depth.getTimestampAlgoConnector());

        newDepth.setBidsQuantities(depth.getBidsQuantities());
        newDepth.setAsksQuantities(depth.getAsksQuantities());
        newDepth.setBids(depth.getBids());
        newDepth.setAsks(depth.getAsks());
        newDepth.setBidsAlgorithmInfo(depth.getBidsAlgorithmInfo());
        newDepth.setAsksAlgorithmInfo(depth.getAsksAlgorithmInfo());
        newDepth.setLevels(depth.getLevels());
        newDepth.setAskLevels(depth.getAskLevels());
        newDepth.setBidLevels(depth.getBidLevels());
        newDepth.setTimeToNextUpdateMs(depth.getTimeToNextUpdateMs());
        newDepth.setLevelsFromData();
        return newDepth;
    }

    public static Depth copyFrom(Depth depth) {
        Depth newDepth = getInstancePool();
        newDepth.setInstrument(depth.getInstrument());
        newDepth.setTimestamp(depth.getTimestamp());
        newDepth.setTimestampStrategy(depth.getTimestampStrategy());
        newDepth.setTimestampBrokerConnector(depth.getTimestampBrokerConnector());
        newDepth.setTimestampAlgoConnector(depth.getTimestampAlgoConnector());
        newDepth.setBidsQuantities(depth.getBidsQuantities());
        newDepth.setAsksQuantities(depth.getAsksQuantities());
        newDepth.setBids(depth.getBids());
        newDepth.setAsks(depth.getAsks());
        newDepth.setBidsAlgorithmInfo(depth.getBidsAlgorithmInfo());
        newDepth.setAsksAlgorithmInfo(depth.getAsksAlgorithmInfo());
        newDepth.setLevels(depth.getLevels());
        newDepth.setAskLevels(depth.getAskLevels());
        newDepth.setBidLevels(depth.getBidLevels());
        newDepth.setTimeToNextUpdateMs(depth.getTimeToNextUpdateMs());
        newDepth.setLevelsFromData();
        return newDepth;
    }

    public void delete() {
        delete(-1);
    }

    public void delete(int milliseconds) {
        if (milliseconds > 0) {
            DEPTH_POOL.lazyCheckIn(this, milliseconds);
        } else {
            DEPTH_POOL.checkIn(this);
        }
    }


    public static String logPool() {
        return DEPTH_POOL.toString();
    }

    private double[] RemoveLevelAndShiftToLeft(double[] input, int level) {
        for (int nextLevel = level; nextLevel < input.length - 1; nextLevel++) {
            input[nextLevel] = input[nextLevel + 1];
        }
        input[input.length - 1] = DEFAULT_VALUE; //remove last element
        return input;
    }

    public static int getNonNullLength(double[] arr) {
        int count = 0;
        for (double el : arr)
            if (!Depth.isDefaultValue(el))
                ++count;
        return count;
    }

    private void reset() {
        bidsQuantities = null;
        asksQuantities = null;
        bids = null;
        asks = null;
        bidsAlgorithmInfo = null;
        asksAlgorithmInfo = null;
        timeToNextUpdateMs = Long.MIN_VALUE;
        timestamp = 0;
        timestampStrategy = 0;
        timestampBrokerConnector = 0;
        timestampAlgoConnector = 0;
    }

    private Depth() {
    }

    public void setDepthFromParquet(DepthParquet depthParquet, Instrument instrument) {
        UTC_CALENDAR.setTimeInMillis(depthParquet.getTimestamp());
        this.instrument = instrument.getPrimaryKey();
        this.timestamp = depthParquet.getTimestamp();

        //from csv
        if (levels == 0) {
            levels = MAX_DEPTH_CSV;
        }
        this.bidsQuantities = new double[levels];
        this.asksQuantities = new double[levels];
        this.bids = new double[levels];
        this.asks = new double[levels];
        Arrays.fill(bidsQuantities, DEFAULT_VALUE);
        Arrays.fill(asksQuantities, DEFAULT_VALUE);
        Arrays.fill(bids, DEFAULT_VALUE);
        Arrays.fill(asks, DEFAULT_VALUE);

        if (depthParquet.getBidPrice0() != null) {
            this.bids[0] = depthParquet.getBidPrice0();
        }
        if (depthParquet.getBidPrice1() != null) {
            this.bids[1] = depthParquet.getBidPrice1();
        }
        if (depthParquet.getBidPrice2() != null) {
            this.bids[2] = depthParquet.getBidPrice2();
        }
        if (depthParquet.getBidPrice3() != null) {
            this.bids[3] = depthParquet.getBidPrice3();
        }
        if (depthParquet.getBidPrice4() != null) {
            this.bids[4] = depthParquet.getBidPrice4();
        }
        //		if (depthParquet.getBidPrice5() != null) {
        //			this.bids[5] = depthParquet.getBidPrice5();
        //		}

        if (depthParquet.getAskPrice0() != null) {
            this.asks[0] = depthParquet.getAskPrice0();
        }
        if (depthParquet.getAskPrice1() != null) {
            this.asks[1] = depthParquet.getAskPrice1();
        }
        if (depthParquet.getAskPrice2() != null) {
            this.asks[2] = depthParquet.getAskPrice2();
        }
        if (depthParquet.getAskPrice3() != null) {
            this.asks[3] = depthParquet.getAskPrice3();
        }
        if (depthParquet.getAskPrice4() != null) {
            this.asks[4] = depthParquet.getAskPrice4();
        }
        //		if (depthParquet.getAskPrice5() != null) {
        //			this.asks[5] = depthParquet.getAskPrice5();
        //		}

        if (depthParquet.getBidQuantity0() != null) {
            this.bidsQuantities[0] = depthParquet.getBidQuantity0();
        }
        if (depthParquet.getBidQuantity1() != null) {
            this.bidsQuantities[1] = depthParquet.getBidQuantity1();
        }
        if (depthParquet.getBidQuantity2() != null) {
            this.bidsQuantities[2] = depthParquet.getBidQuantity2();
        }
        if (depthParquet.getBidQuantity3() != null) {
            this.bidsQuantities[3] = depthParquet.getBidQuantity3();
        }
        if (depthParquet.getBidQuantity4() != null) {
            this.bidsQuantities[4] = depthParquet.getBidQuantity4();
        }
        //		if (depthParquet.getBidQuantity5() != null) {
        //			this.bidsQuantities[5] = depthParquet.getBidQuantity5();
        //		}

        if (depthParquet.getAskQuantity0() != null) {
            this.asksQuantities[0] = depthParquet.getAskQuantity0();
        }
        if (depthParquet.getAskQuantity1() != null) {
            this.asksQuantities[1] = depthParquet.getAskQuantity1();
        }
        if (depthParquet.getAskQuantity2() != null) {
            this.asksQuantities[2] = depthParquet.getAskQuantity2();
        }
        if (depthParquet.getAskQuantity3() != null) {
            this.asksQuantities[3] = depthParquet.getAskQuantity3();
        }
        if (depthParquet.getAskQuantity4() != null) {
            this.asksQuantities[4] = depthParquet.getAskQuantity4();
        }

        this.setLevelsFromData();
        //		if (depthParquet.getAskQuantity5() != null) {
        //			this.asksQuantities[5] = depthParquet.getAskQuantity5();
        //		}
    }


    public void setAllQuantitiesZero() {
        //use only in metatrader info from backtesting
        Arrays.fill(bidsQuantities, 0.0);
        Arrays.fill(asksQuantities, 0.0);
    }


    public void cleanNullLevels() {

        List<Integer> bidLevelsToRemove = new ArrayList<>();
        List<Integer> askLevelsToRemove = new ArrayList<>();
        for (int level = 0; level < bids.length; level++) {
            if (bidsQuantities.length > level && (Depth.isDefaultValue(bids[level]) || Depth.isDefaultValue(bidsQuantities[level]))) {
                bidLevelsToRemove.add(level);
            }
        }
        for (int level = 0; level < asks.length; level++) {
            if (asksQuantities.length > level && (Depth.isDefaultValue(asks[level]) || Depth.isDefaultValue(asksQuantities[level]))) {
                askLevelsToRemove.add(level);
            }
        }

        // Process removals in reverse order to avoid shifting problems
        Collections.sort(bidLevelsToRemove, Collections.reverseOrder());
        Collections.sort(askLevelsToRemove, Collections.reverseOrder());

        //shift from worst price to best
        for (int levelToRemove : bidLevelsToRemove) {
            bids = RemoveLevelAndShiftToLeft(bids, levelToRemove);
            bidsQuantities = RemoveLevelAndShiftToLeft(bidsQuantities, levelToRemove);
            if (bidsAlgorithmInfo != null) {
                bidsAlgorithmInfo = (List<String>[]) ArrayUtils.RemoveLevelAndShiftToLeft(bidsAlgorithmInfo, levelToRemove);
            }
        }
        for (int levelToRemove : askLevelsToRemove) {
            asks = RemoveLevelAndShiftToLeft(asks, levelToRemove);
            asksQuantities = RemoveLevelAndShiftToLeft(asksQuantities, levelToRemove);
            if (asksAlgorithmInfo != null) {
                asksAlgorithmInfo = (List<String>[]) ArrayUtils.RemoveLevelAndShiftToLeft(asksAlgorithmInfo, levelToRemove);
            }
        }
        //now the worst places are with default value -> remove them
        int maxLevelBid = bids.length - bidLevelsToRemove.size();
        bidsQuantities = Arrays.copyOf(bidsQuantities, maxLevelBid);
        bids = Arrays.copyOf(bids, maxLevelBid);
        int maxLevelAsk = asks.length - askLevelsToRemove.size();
        asks = Arrays.copyOf(asks, maxLevelAsk);
        asksQuantities = Arrays.copyOf(asksQuantities, maxLevelAsk);
    }


    public void setLevelsFromData() {
        //		cleanNullLevels();
        int bidLevels = Math.min(getNonNullLength(bids), Depth.MAX_DEPTH);
        int askLevels = Math.min(getNonNullLength(asks), Depth.MAX_DEPTH);
        this.bidLevels = bidLevels;
        this.askLevels = askLevels;
        this.levels = getLevels();
    }

    public int getLevels() {
        this.levels = Math.max(this.bidLevels, this.askLevels);
        return levels;
    }

    @JSONField(serialize = false, deserialize = false)
    public double getMidPrice() {
        return (getBestBid() + getBestAsk()) / 2;
    }

    @JSONField(serialize = false, deserialize = false)
    public double getSpread() {
        double bestBid = getBestBid();
        double bestAsk = getBestAsk();

        return Math.abs(bestBid - bestAsk);
    }

    public boolean isDepthFilled() {
        return bids.length > 0 && asks.length > 0;
    }

    public boolean isDepthValid() {
        boolean isFilledAtLeastOneLevel = bids.length > 0 || asks.length > 0;
        boolean isRightPrice = true;
        try {
            isRightPrice = getBestBid() < getBestAsk();
        } catch (Exception e) {
//			logger.error("some error on checking isRightPrice ", e);
            isRightPrice = true;
        }
        return isFilledAtLeastOneLevel && isRightPrice;
    }

    @JSONField(serialize = false, deserialize = false)
    public double getBestBid() {
        if (bids == null || bids.length == 0) {
            return Double.MIN_VALUE;
        }
        return bids[0];
    }


    @JSONField(serialize = false, deserialize = false)
    public double getBestAsk() {
        if (asks == null || asks.length == 0) {
            return Double.MAX_VALUE;
        }
        return asks[0];
    }

    @JSONField(serialize = false, deserialize = false)
    public double getWorstAsk() {
        if (asks == null || asks.length == 0) {
            return Double.MAX_VALUE;
        }
        double output = -1;
        int levelCounter = Math.max(asks.length - 1, 0);
        while (output == -1) {
            if (!Depth.isDefaultValue(asks[levelCounter])) {
                output = asks[levelCounter];
            } else {
                levelCounter--;
            }
        }
        return output;
    }

    @JSONField(serialize = false, deserialize = false)
    public double getWorstBid() {
        if (bids == null || bids.length == 0) {
            return Double.MIN_VALUE;
        }
        double output = -1;
        int levelCounter = Math.max(bids.length - 1, 0);
        while (output == -1) {
            if (!Depth.isDefaultValue(bids[levelCounter])) {
                output = bids[levelCounter];
            } else {
                levelCounter--;
            }
        }
        return output;

    }

    @JSONField(serialize = false, deserialize = false)
    public double getBestBidQty() {
        if (bidsQuantities == null || bidsQuantities.length == 0) {
            return 0;
        }
        return bidsQuantities[0];
    }

    @JSONField(serialize = false, deserialize = false)
    public double getBestAskQty() {
        if (asksQuantities == null || asksQuantities.length == 0) {
            return 0;
        }
        return asksQuantities[0];
    }

    public boolean equalsSide(Depth depth, Verb verb) {
        double[] prices = bids;
        double[] quantities = bidsQuantities;

        double[] otherPrices = depth.getBids();
        double[] otherQuantities = depth.getBidsQuantities();

        if (verb.equals(Verb.Sell)) {
            prices = asks;
            quantities = asksQuantities;

            otherPrices = depth.getAsks();
            otherQuantities = depth.getAsksQuantities();
        }

        return Arrays.equals(prices, otherPrices) && Arrays.equals(quantities, otherQuantities);
    }


    public boolean removeOrder(double price, double quantity, Verb verb, String algorithmInfo) {
        try {
            double[] prices = bids;
            double[] quantities = bidsQuantities;
            List<String>[] algorithmInfos = bidsAlgorithmInfo;

            if (verb.equals(Verb.Sell)) {
                prices = asks;
                quantities = asksQuantities;
                algorithmInfos = asksAlgorithmInfo;
            }
            if (prices == null || prices.length == 0) {
                return false;
            }

            if (quantities == null || quantities.length == 0) {
                return false;
            }
            List<Integer> levelsToRemove = new ArrayList<>();
            for (int level = 0; level < prices.length; level++) {
                Double priceDepth = prices[level];
                Double quantityDepth = quantities[level];
                if (priceDepth == null || quantityDepth == null || quantityDepth == 0) {
                    continue;
                }

                if (priceDepth == price) {
                    double newQuantity = quantityDepth - quantity;
                    newQuantity = Math.max(0.0, newQuantity);
                    if (newQuantity < 1E-6) {
                        //level disappear
                        levelsToRemove.add(level);
                    } else {
                        if (algorithmInfo != null && algorithmInfos != null && algorithmInfos[level] != null) {
                            algorithmInfos[level].remove(algorithmInfo);
                        }
                    }
                }

            }
            if (levelsToRemove.size() > 0) {
                for (int level : levelsToRemove) {
                    prices = RemoveLevelAndShiftToLeft(prices, level);
                    quantities = RemoveLevelAndShiftToLeft(quantities, level);
                    algorithmInfos = (List<String>[]) ArrayUtils.RemoveLevelAndShiftToLeft(algorithmInfos, level);
                }
            }
            setLevelsFromData();
            return true;
        } catch (Exception e) {
            logger.error("error on removeOrder", e);
            return false;
        }
    }


    @Override
    public String toString() {
        if (bids == null || asks == null) {
            return "Depth is empty";
        }
        return toJsonString(this);
    }

    public String prettyPrint() {
        UTC_CALENDAR.setTimeInMillis(getTimestamp());
        String header = "\n----------------------------------------------\n";
        header += String.format("|\t[%s] %s\t\t\t\t|\n", UTC_CALENDAR.getTime(), getInstrument());
        header += "|\tBID\t\t\t\t\t|\t\tASK\t\t\t|\n";
        header += "----------------------------------------------\n";

        String tail = "----------------------------------------------";

        String bidRow = "|\t%.5f\t%.5f\t|\t\t\t\t\t|\n";
        String bidRowAlgos = "|\t*%.5f\t%.5f\t|\t\t\t\t\t|\n";

        String askRow = "|\t\t\t\t\t\t|\t%.5f\t%.5f\t|\n";
        String askRowAlgos = "|\t\t\t\t\t\t|\t%.5f\t%.5f*\t|\n";

        StringBuilder askSideOutput = new StringBuilder();
        askSideOutput.append(header);

        StringBuilder bidSideOutput = new StringBuilder();

        for (int level = 0; level < getLevels(); level++) {
            if (bids.length >= level + 1) {
                Double bidPrice = bids[level];
                if (bidPrice == null) {
                    continue;
                }
                Double bidQty = bidsQuantities[level];
                if (bidQty == null) {
                    continue;
                }

                boolean isBidAlgo = !bidsAlgorithmInfo[level].contains(ALGORITHM_INFO_MM);
                String bidString = String.format(bidRow, bidQty, bidPrice);
                if (isBidAlgo) {
                    bidString = String.format(bidRowAlgos, bidQty, bidPrice);
                }
                bidSideOutput = bidSideOutput.append(bidString);
            }

            if (asks.length >= level + 1) {
                Double askPrice = asks[level];
                if (askPrice == null) {
                    continue;
                }
                Double askQty = asksQuantities[level];
                if (askQty == null) {
                    continue;
                }

                boolean isAskAlgo = !asksAlgorithmInfo[level].contains(ALGORITHM_INFO_MM);
                String askString = String.format(askRow, askPrice, askQty);
                if (isAskAlgo) {
                    askString = String.format(askRowAlgos, askPrice, askQty);
                }
                askSideOutput = askSideOutput.append(askString);
            }

        }
        StringBuilder output = askSideOutput;
        output = output.append(bidSideOutput);
        output.append(tail);

        return output.toString();
    }

    public static StringBuilder headerCSV() {
        //			,ask0,ask1,ask2,ask3,ask4,ask_quantity0,ask_quantity1,ask_quantity2,ask_quantity3,ask_quantity4,bid0,bid1,bid2,bid3,bid4,bid_quantity0,bid_quantity1,bid_quantity2,bid_quantity3,bid_quantity4

        StringBuilder stringBuffer = new StringBuilder();
        return stringBuffer
                .append(",timestamp,ask0,ask1,ask2,ask3,ask4,ask_quantity0,ask_quantity1,ask_quantity2,ask_quantity3,ask_quantity4,bid0,bid1,bid2,bid3,bid4,bid_quantity0,bid_quantity1,bid_quantity2,bid_quantity3,bid_quantity4");

    }

    public String toCSV(boolean withHeader) {
        if (!this.isDepthFilled()) {
            return null;
        }
        StringBuilder stringBuffer = new StringBuilder();
        if (withHeader) {
            stringBuffer.append(headerCSV());
            stringBuffer.append(System.lineSeparator());
        }

        //2019-11-09 08:42:24.142302
        stringBuffer.append(getDatePythonUTC(timestamp));
        stringBuffer.append(',');
        stringBuffer.append(timestamp);
        stringBuffer.append(',');

        //ask side
        for (int level = 0; level < MAX_DEPTH_CSV; level++) {
            if (level >= asks.length || Depth.isDefaultValue(asks[level])) {
                stringBuffer.append("");
            } else {
                stringBuffer.append(asks[level]);
            }
            stringBuffer.append(',');
        }
        for (int level = 0; level < MAX_DEPTH_CSV; level++) {
            if (level >= asksQuantities.length || Depth.isDefaultValue(asksQuantities[level])) {
                stringBuffer.append("");
            } else {
                stringBuffer.append(asksQuantities[level]);
            }
            stringBuffer.append(',');
        }

        //bid side
        for (int level = 0; level < MAX_DEPTH_CSV; level++) {
            if (level >= bids.length || Depth.isDefaultValue(bids[level])) {
                stringBuffer.append("");
            } else {
                stringBuffer.append(bids[level]);
            }
            stringBuffer.append(',');
        }
        for (int level = 0; level < MAX_DEPTH_CSV; level++) {
            if (level >= bidsQuantities.length || Depth.isDefaultValue(bidsQuantities[level])) {
                stringBuffer.append("");
            } else {
                stringBuffer.append(bidsQuantities[level]);
            }

            stringBuffer.append(',');
        }
        stringBuffer = stringBuffer.deleteCharAt(stringBuffer.length() - 1);//remove last comma

        return stringBuffer.toString();
    }


    @JSONField(serialize = false, deserialize = false)
    @Override
    public Object getParquetObject() {
        return getDepthParquet();
    }

    @JSONField(serialize = false, deserialize = false)
    public DepthParquet getDepthParquet() {
        return new DepthParquet(this);
    }


    @JSONField(serialize = false, deserialize = false)
    public double getVPIN() {
        //is the same?
        return getImbalance();
    }

    /**
     * \mbox{Microprice}_t = \frac{V_t^b P_t^a + V_t^a P_t^b}{V_t^b + V_t^a},
     * <p>
     * https://www.quantstart.com/articles/high-frequency-trading-ii-limit-order-book/
     * [1] Cartea, A., Sebastian, J. and Penalva, J. (2015) Algorithmic and High-Frequency Trading. Cambridge University Press
     * <p>
     * For example, if the volume of limit orders posted at the best bid price is significantly larger than the volume of limit orders at the best ask price, the microprice will be pushed towards the ask price.
     *
     * @return
     */
    @JSONField(serialize = false, deserialize = false)
    public double getMicroPrice() {
        //only on the first level
        double sumQty = getBestAskQty() + getBestBidQty();

        double out = ((getBestBid() * getBestAskQty()) / sumQty) + ((getBestAsk() * getBestBidQty()) / sumQty);
        return out;
    }

    /**
     * Cartea et al. (2015), and define the order (-1,1)
     *
     * @return
     */
    @JSONField(serialize = false, deserialize = false)
    public double getImbalance() {
        double bidVolTotal = 0.;
        double askVolTotal = 0.;
        for (int level = 0; level < getBidLevels(); level++) {
            try {
                bidVolTotal += bidsQuantities[level];
            } catch (IndexOutOfBoundsException e) {

            }
        }
        for (int level = 0; level < getAskLevels(); level++) {
            try {
                askVolTotal += asksQuantities[level];
            } catch (IndexOutOfBoundsException e) {

            }
        }
        if ((bidVolTotal + askVolTotal) == 0) {
            return 0.0;
        }
        return (bidVolTotal - askVolTotal) / (bidVolTotal + askVolTotal);
    }

    @JSONField(serialize = false, deserialize = false)
    public double getTotalVolume() {
        double bidVolTotal = 0.;
        double askVolTotal = 0.;
        for (int level = 0; level < getBidLevels(); level++) {
            try {
                bidVolTotal += bidsQuantities[level];
            } catch (IndexOutOfBoundsException e) {

            }
        }

        for (int level = 0; level < getAskLevels(); level++) {
            try {
                askVolTotal += asksQuantities[level];
            } catch (IndexOutOfBoundsException e) {

            }
        }

        if ((bidVolTotal + askVolTotal) == 0) {
            return 0.0;
        }
        return bidVolTotal + askVolTotal;
    }

    @JSONField(serialize = false, deserialize = false)
    public int getLevelBidFromPrice(double price) {
        int lastLevel = getBidLevels();
        if (bids != null) {
            for (int level = 0; level < lastLevel; level++) {
                try {
                    if (!Depth.isDefaultValue(bids[level]) && bids[level] <= price) {
                        return level;
                    }
                } catch (IndexOutOfBoundsException e) {

                }
            }
        }
        return lastLevel;
    }

    @JSONField(serialize = false, deserialize = false)
    public double getBidPriceFromLevel(int level) {
        return getBidPriceFromLevel(level, 0);
    }

    @JSONField(serialize = false, deserialize = false)
    public double getBidPriceFromLevel(int level, double qty) {
        if (qty <= 1E-9 && level == 0) {
            return getBestBid();
        }
        if (bids == null || bids.length == 0 || bidsQuantities == null || bidsQuantities.length == 0 || level >= bids.length) {
            return Double.MIN_VALUE;
        }

        if (qty <= 1E-9) {
            return bids[level];
        }

        double cumQty = 0;
        int levelIt = 0;
        for (int i = 0; i < bidLevels; i++) {
            cumQty += bidsQuantities[i];
            if (cumQty >= qty) {
                if (levelIt == level) {
                    return bids[i];
                }
                levelIt++;
            }
        }
        return getWorstBid();//worst Bid
    }

    @JSONField(serialize = false, deserialize = false)
    public double getAskPriceFromLevel(int level) {
        return getAskPriceFromLevel(level, 0);
    }

    @JSONField(serialize = false, deserialize = false)
    public double getAskPriceFromLevel(int level, double qty) {
        if (qty <= 1E-9 && level == 0) {
            return getBestAsk();
        }

        if (asks == null || asks.length == 0 || asksQuantities == null || asksQuantities.length == 0 || level >= asks.length) {
            return Double.MAX_VALUE;
        }
        if (qty <= 1E-9) {
            return asks[level];
        }

        double cumQty = 0;
        int levelIt = 0;
        for (int i = 0; i < askLevels; i++) {
            cumQty += asksQuantities[i];
            if (cumQty >= qty) {
                if (levelIt == level) {
                    return asks[i];
                }
                levelIt++;
            }
        }
        return getWorstAsk();//worst Ask
    }

    @JSONField(serialize = false, deserialize = false)
    public int getLevelAskFromPrice(double price) {
        int lastLevel = getAskLevels();
        if (asks != null) {
            for (int level = 0; level < lastLevel; level++) {
                try {
                    if (!Depth.isDefaultValue(asks[level]) && asks[level] >= price) {
                        return level;
                    }
                } catch (IndexOutOfBoundsException e) {

                }
            }
        }
        return lastLevel;
    }

    @JSONField(serialize = false, deserialize = false)
    public double getMidPriceFromLevel(int level, double qty) {
        if (qty <= 1E-9 && level == 0) {
            return getMidPrice();
        }

        double bestBid = getBidPriceFromLevel(level, qty);
        if (bestBid == Double.MIN_VALUE) {
            return Double.NaN;
        }
        double bestAsk = getAskPriceFromLevel(level, qty);
        if (bestAsk == Double.MAX_VALUE) {
            return Double.NaN;
        }
        return (bestBid + bestAsk) / 2;
    }

    @JSONField(serialize = false, deserialize = false)
    public double getSpreadFromLevel(int level, double qty) {
        if (qty <= 1E-9 && level == 0) {
            return getSpread();
        }

        double bestBid = getBidPriceFromLevel(level, qty);
        if (bestBid == Double.MIN_VALUE) {
            return Double.NaN;
        }
        double bestAsk = getAskPriceFromLevel(level, qty);
        if (bestAsk == Double.MAX_VALUE) {
            return Double.NaN;
        }
        return bestAsk - bestBid;
    }


    @JSONField(serialize = false, deserialize = false)
    public double getBidVolume(double price) {
        double bidVolPrice = 0.;
        for (int level = 0; level < getBidLevels(); level++) {
            try {
                if (bids[level] == price) {
                    bidVolPrice += bidsQuantities[level];
                }
            } catch (IndexOutOfBoundsException e) {

            }
        }
        return bidVolPrice;
    }

    @JSONField(serialize = false, deserialize = false)
    public double getAskVolume(double price) {
        double askVolPrice = 0.;
        for (int level = 0; level < getAskLevels(); level++) {
            try {
                if (asks[level] == price) {
                    askVolPrice += asksQuantities[level];
                }
            } catch (IndexOutOfBoundsException e) {

            }
        }
        return askVolPrice;
    }

    @JSONField(serialize = false, deserialize = false)
    public double getBidVolume() {
        double bidVolTotal = 0.;
        for (int level = 0; level < getBidLevels(); level++) {
            try {
                bidVolTotal += bidsQuantities[level];
            } catch (IndexOutOfBoundsException e) {

            }
        }
        return bidVolTotal;
    }

    @JSONField(serialize = false, deserialize = false)
    public double getAskVolume() {
        double askVolTotal = 0.;
        for (int level = 0; level < getAskLevels(); level++) {
            try {
                askVolTotal += asksQuantities[level];
            } catch (IndexOutOfBoundsException e) {

            }
        }
        return askVolTotal;
    }

    @JSONField(serialize = false, deserialize = false)
    public boolean equalsContent(Depth otherDepth) {
        //check if the arrays are equal bids and asks and quantities
        if (otherDepth == null) {
            return false;
        }
        if (otherDepth.getBids() == null || otherDepth.getAsks() == null || otherDepth.getBidsQuantities() == null || otherDepth.getAsksQuantities() == null) {
            return false;
        }
        if (this.getBids() == null || this.getAsks() == null || this.getBidsQuantities() == null || this.getAsksQuantities() == null) {
            return false;
        }
        if (this.getBids().length != otherDepth.getBids().length || this.getAsks().length != otherDepth.getAsks().length || this.getBidsQuantities().length != otherDepth.getBidsQuantities().length || this.getAsksQuantities().length != otherDepth.getAsksQuantities().length) {
            return false;
        }
        //check levels
        if (this.getLevels() != otherDepth.getLevels()) {
            return false;
        }
        if (this.getBidLevels() != otherDepth.getBidLevels()) {
            return false;
        }
        if (this.getAskLevels() != otherDepth.getAskLevels()) {
            return false;
        }
        //check array contents
        if (!Arrays.equals(this.getBids(), otherDepth.getBids())) {
            return false;
        }
        if (!Arrays.equals(this.getAsks(), otherDepth.getAsks())) {
            return false;
        }
        if (!Arrays.equals(this.getBidsQuantities(), otherDepth.getBidsQuantities())) {
            return false;
        }
        if (!Arrays.equals(this.getAsksQuantities(), otherDepth.getAsksQuantities())) {
            return false;
        }

        return true;

    }

    @JSONField(serialize = false, deserialize = false)
    public String getLatenciesTable() {
        return getLatenciesTable(getTimestamp(), getTimestampBrokerConnector(), getTimestampAlgoConnector(), getTimestampStrategy());
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static class DepthPool extends ObjectPool<Depth> {
        @Override
        protected Depth create() {
            return new Depth();
        }
    }
}



