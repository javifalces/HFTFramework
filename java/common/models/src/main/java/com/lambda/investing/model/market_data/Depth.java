package com.lambda.investing.model.market_data;

import com.lambda.investing.ArrayUtils;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

import static com.lambda.investing.ArrayUtils.RemoveLevelAndShiftToLeft;
import static com.lambda.investing.model.Util.GSON_STRING;
import static com.lambda.investing.model.Util.getDatePythonUTC;

@Getter
@Setter

public class Depth extends CSVable implements Cloneable {

    public static String ALGORITHM_INFO_MM = "MarketMaker_Parquet";

    private static Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    public static int MAX_DEPTH = 5;
    public static int MAX_DEPTH_CSV = 5;

    //	private transient Instrument instrument;
    private String instrument;
    private long timestamp;
    private Double[] bidsQuantities, asksQuantities, bids, asks;//TODO change to Bigdecimal

    private List<String>[] bidsAlgorithmInfo, asksAlgorithmInfo;//just for backtesting

    private int levels, askLevels, bidLevels;
    private long timeToNextUpdateMs = Long.MIN_VALUE;

    public void setAllQuantitiesZero() {
        //use only in metatrader info from backtesting
        Arrays.fill(bidsQuantities, 0.0);
        Arrays.fill(asksQuantities, 0.0);
    }


    private void cleanNullLevels() {
        int maxLevelBid = 0;
        int maxLevelAsk = 0;

        for (int level = 0; level < Depth.MAX_DEPTH; level++) {
            if (bids[level] != null && bidsQuantities[level] != null) {
                maxLevelBid++;
            }

            if (asks[level] != null && asksQuantities[level] != null) {
                maxLevelAsk++;
            }
        }
        bidsQuantities = Arrays.copyOf(bidsQuantities, maxLevelBid);
        bids = Arrays.copyOf(bids, maxLevelBid);

        asks = Arrays.copyOf(asks, maxLevelAsk);
        asksQuantities = Arrays.copyOf(asksQuantities, maxLevelAsk);
    }

    public void setLevelsFromData() {
        //		cleanNullLevels();
        int bidLevels = Math.min(ArrayUtils.getNonNullLength(bids), Depth.MAX_DEPTH);
        int askLevels = Math.min(ArrayUtils.getNonNullLength(asks), Depth.MAX_DEPTH);
        this.bidLevels = bidLevels;
        this.askLevels = askLevels;
        this.levels = getLevels();
    }

    public int getLevels() {
        this.levels = Math.max(this.bidLevels, this.askLevels);
        return levels;
    }

    public double getMidPrice() {
        return (getBestBid() + getBestAsk()) / 2;
    }

    public double getSpread() {
        return Math.abs(bids[0] - asks[0]);
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

    public double getBestBid() {
        return bids[0];
    }

    public double getBestAsk() {
        return asks[0];
    }

    public double getWorstAsk() {
        double output = -1;
        int levelCounter = asks.length - 1;
        while (output == -1) {
            if (asks[levelCounter] != null) {
                output = asks[levelCounter];
            } else {
                levelCounter--;
            }
        }
        return output;
    }

    public double getWorstBid() {
        double output = -1;
        int levelCounter = bids.length - 1;
        while (output == -1) {
            if (bids[levelCounter] != null) {
                output = bids[levelCounter];
            } else {
                levelCounter--;
            }
        }
        return output;

    }

    public double getBestBidQty() {
        return bidsQuantities[0];
    }

    public double getBestAskQty() {
        return asksQuantities[0];
    }

    public boolean equalsSide(Depth depth, Verb verb) {
        Double[] prices = bids;
        Double[] quantities = bidsQuantities;

        Double[] otherPrices = depth.getBids();
        Double[] otherQuantities = depth.getBidsQuantities();

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
            Double[] prices = bids;
            Double[] quantities = bidsQuantities;
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
                        if (algorithmInfo != null) {
                            algorithmInfos[level].remove(algorithmInfo);
                        }
                    }
                }

            }
            if (levelsToRemove.size() > 0) {
                for (int level : levelsToRemove) {
                    prices = (Double[]) RemoveLevelAndShiftToLeft(prices, level);
                    algorithmInfos = (List<String>[]) RemoveLevelAndShiftToLeft(algorithmInfos, level);
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
        return GSON_STRING.toJson(this);
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
            if (level >= asks.length || asks[level] == null) {
                stringBuffer.append("");
            } else {
                stringBuffer.append(asks[level]);
            }
            stringBuffer.append(',');
        }
        for (int level = 0; level < MAX_DEPTH_CSV; level++) {
            if (level >= asksQuantities.length || asksQuantities[level] == null) {
                stringBuffer.append("");
            } else {
                stringBuffer.append(asksQuantities[level]);
            }
            stringBuffer.append(',');
        }

        //bid side
        for (int level = 0; level < MAX_DEPTH_CSV; level++) {
            if (level >= bids.length || bids[level] == null) {
                stringBuffer.append("");
            } else {
                stringBuffer.append(bids[level]);
            }
            stringBuffer.append(',');
        }
        for (int level = 0; level < MAX_DEPTH_CSV; level++) {
            if (level >= bidsQuantities.length || bidsQuantities[level] == null) {
                stringBuffer.append("");
            } else {
                stringBuffer.append(bidsQuantities[level]);
            }

            stringBuffer.append(',');
        }
        stringBuffer = stringBuffer.deleteCharAt(stringBuffer.length() - 1);//remove last comma

        return stringBuffer.toString();
    }

    @Override
    public Object getParquetObject() {
        return getDepthParquet();
    }

    public DepthParquet getDepthParquet() {
        return new DepthParquet(this);
    }

    public Depth() {
    }

    public Depth(DepthParquet depthParquet, Instrument instrument) {
        UTC_CALENDAR.setTimeInMillis(depthParquet.getTimestamp());
        this.instrument = instrument.getPrimaryKey();
        this.timestamp = depthParquet.getTimestamp();

        //from csv
        if (levels == 0) {
            levels = MAX_DEPTH_CSV;
        }
        this.bidsQuantities = new Double[levels];
        this.asksQuantities = new Double[levels];
        this.bids = new Double[levels];
        this.asks = new Double[levels];

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


    public int getLevelBidPrice(double price) {
        int lastLevel = getBidLevels();
        if (bids != null) {
            for (int level = 0; level < lastLevel; level++) {
                try {
                    if (bids[level] != null && bids[level] <= price) {
                        return level;
                    }
                } catch (IndexOutOfBoundsException e) {

                }
            }
        }
        return lastLevel;
    }

    public int getLevelAskPrice(double price) {
        int lastLevel = getAskLevels();
        if (asks != null) {
            for (int level = 0; level < lastLevel; level++) {
                try {
                    if (asks[level] != null && asks[level] >= price) {
                        return level;
                    }
                } catch (IndexOutOfBoundsException e) {

                }
            }
        }
        return lastLevel;
    }

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

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

