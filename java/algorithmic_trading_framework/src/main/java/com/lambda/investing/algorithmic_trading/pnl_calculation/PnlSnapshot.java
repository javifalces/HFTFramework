package com.lambda.investing.algorithmic_trading.pnl_calculation;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.CustomColumn;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lambda.investing.algorithmic_trading.utils.TimeseriesUtils.GetStd;
import static com.lambda.investing.algorithmic_trading.utils.TimeseriesUtils.GetZscore;
import static com.lambda.investing.algorithmic_trading.utils.TimeseriesUtils.GetZscorePositive;
import static com.lambda.investing.model.asset.Instrument.round;


/****
 * DEPRECATED CLASS!!! not use it!! we are using PnlSnapshotOrders!
 *
 */
@Getter
public class PnlSnapshot {

    protected static double DEFAULT_QUANTITY_MULTIPLIER = 1.0;

    protected static boolean CHECK_OPEN_PNL = false;
    protected static double UNREALIZED_ZSCORE_WARNING = 100.0;
    protected static int UNREALIZED_CHECK_SIZE = 20;

    protected static boolean CHECK_SPREAD = false;
    protected static double SPREAD_ZSCORE_WARNING = 10.0;
    protected static int SPREAD_CHECK_SIZE = 20;

    public static final Object UPDATE_HISTORICAL_LOCK = new Object();
    protected static boolean SAVE_ZERO_TRADES = true;//if true we are going to start saving data before trades

    Logger logger = LogManager.getLogger(PnlSnapshot.class);
    public double netPosition, avgOpenPrice, netInvestment, realizedPnl, unrealizedPnl, totalPnl, totalFees, lastPriceForUnrealized, spread, realizedFees, unrealizedFees;
    public String algorithmInfo = "";
    public String instrumentPk;
    public Map<Double, Double> openPriceToVolume;
    public List<Long> historicalTimestamp;
    public Map<Long, Double> historicalNetPosition, historicalAvgOpenPrice, historicalNetInvestment, historicalRealizedPnl, historicalUnrealizedPnl, historicalSpread, historicalTotalPnl, historicalPrice, historicalFee, historicalQuantity, historicalMidPrice, historicalAskPrice, historicalBidPrice;
    public Map<Long, String> historicalAlgorithmInfo;
    public Map<Long, String> historicalInstrumentPk;
    public Map<Long, String> historicalClOrdId;
    protected Queue<Double> lastUnrealizedPnls;
    protected Queue<Double> lastSpreads;
    public Map<Long, List<CustomColumn>> historicalCustomColumns;
    public Map<Long, String> historicalVerb;
    public Map<Long, Integer> historicalNumberOfTrades;
    public Map<Long, Integer> historicalNumberOfAggressorTrades;
    protected Map<String, ExecutionReportStatus> processedClOrdId;
    protected boolean nextCustomReject = false;
    protected double lastPrice, lastQuantity, lastFee;
    protected long lastTimestampUpdate;
    protected long lastTimestampExecutionReportUpdate = 0;
    public String lastVerb = Verb.NotSet.name();
    protected String lastClOrdId = "";
    public AtomicInteger numberOfTrades = new AtomicInteger(0);
    public AtomicInteger numberOfAggressorTrades = new AtomicInteger(0);
    public AtomicInteger numberOfAggressedTrades = new AtomicInteger(0);
    protected boolean isBacktest = false;
    protected boolean isPaper = false;
    protected Queue<Double> midpricesQueue;
    protected double maxExecutionPriceValid = Double.MAX_VALUE;
    protected double minExecutionPriceValid = -Double.MAX_VALUE;
    protected int windowTick = 10;
    protected double stdMidPrice = 0.0;
    protected Depth lastDepth;

    List<Double> zerosList = Arrays.asList(0.0, -0.0);

    protected Calendar calendar = new GregorianCalendar();

    public PnlSnapshot(String instrumentPk) {
        this.instrumentPk = instrumentPk;
        historicalTimestamp = new ArrayList<>();
        openPriceToVolume = new ConcurrentHashMap<>();
        historicalCustomColumns = new ConcurrentHashMap<>();
//		lastDepth ;
        historicalNetPosition = new ConcurrentHashMap<>();
        historicalAlgorithmInfo = new ConcurrentHashMap<>();
        historicalInstrumentPk = new ConcurrentHashMap<>();
        historicalClOrdId = new ConcurrentHashMap<>();
        historicalAvgOpenPrice = new ConcurrentHashMap<>();
        historicalNetInvestment = new ConcurrentHashMap<>();
        historicalRealizedPnl = new ConcurrentHashMap<>();
        historicalUnrealizedPnl = new ConcurrentHashMap<>();
        historicalSpread = new ConcurrentHashMap<>();
        historicalTotalPnl = new ConcurrentHashMap<>();
        processedClOrdId = new ConcurrentHashMap<>();

        historicalFee = new ConcurrentHashMap<>();
        historicalPrice = new ConcurrentHashMap<>();
        historicalBidPrice = new ConcurrentHashMap<>();
        historicalAskPrice = new ConcurrentHashMap<>();
        historicalMidPrice = new ConcurrentHashMap<>();
        historicalQuantity = new ConcurrentHashMap<>();
        historicalVerb = new ConcurrentHashMap<>();
        historicalNumberOfTrades = new ConcurrentHashMap<>();
        historicalNumberOfAggressorTrades = new ConcurrentHashMap<>();

        lastUnrealizedPnls = EvictingQueue.create(UNREALIZED_CHECK_SIZE * 2);//more to later clean zeros
        lastSpreads = EvictingQueue.create(SPREAD_CHECK_SIZE * 2);//more to later clean zeros
        midpricesQueue = EvictingQueue.create(windowTick);
    }

    public void setNumberOfTrades(int numberOfTrades) {
        this.numberOfTrades = new AtomicInteger(numberOfTrades);
    }

    public void setNetPosition(double netPosition) {
        this.netPosition = netPosition;
    }

    public void setAlgorithmInfo(String algorithmInfo) {
        this.algorithmInfo = algorithmInfo;
    }

    public void setBacktest(boolean backtest) {
        isBacktest = backtest;
    }

    public void setPaper(boolean paper) {
        isPaper = paper;
    }

    protected boolean checkSpreadHistorical(double spreadProposal) {
        if (lastSpreads.size() < 25) {
            return true;
        }
        List<Double> spreadList = null;
        try {
            spreadList = new ArrayList<Double>(lastSpreads);
        } catch (Exception e) {
            return false;
        }
        spreadList.removeAll(zerosList);//
        //subsample it
        int startIndex = Math.max(0, spreadList.size() - SPREAD_CHECK_SIZE);//its inclusive
        int endIndex = spreadList.size();//its exclusive
        spreadList = spreadList.subList(startIndex, endIndex);
        if (spreadList.size() < SPREAD_CHECK_SIZE / 2) {
            return true;
        }
        Double[] spreadArr = new Double[spreadList.size()];
        spreadArr = spreadList.toArray(spreadArr);
        double zscore = GetZscore(spreadArr, spreadProposal);

        if (Math.abs(zscore) > SPREAD_ZSCORE_WARNING) {
            return false;
        }
        return true;
    }

    protected boolean checkUnrealizedHistorical(double unrealizedPnlProposal) {
        if (lastUnrealizedPnls.size() < 25) {
            return true;
        }
        List<Double> unrealizedPnlList = null;
        try {
            unrealizedPnlList = new ArrayList<>(lastUnrealizedPnls);
        } catch (Exception e) {
            return false;
        }
        unrealizedPnlList.removeAll(zerosList);//

        //		//subsample it
        int startIndex = Math.max(0, unrealizedPnlList.size() - UNREALIZED_CHECK_SIZE);//its inclusive
        int endIndex = unrealizedPnlList.size();//its exclusive
        unrealizedPnlList = unrealizedPnlList.subList(startIndex, endIndex);
        if (unrealizedPnlList.size() < 10) {
            return true;
        }

        Double[] unrealizedPnlArr = new Double[unrealizedPnlList.size()];
        unrealizedPnlArr = unrealizedPnlList.toArray(unrealizedPnlArr);
        double zscore = GetZscorePositive(unrealizedPnlArr, unrealizedPnlProposal);

        if (unrealizedPnlArr.length > UNREALIZED_CHECK_SIZE / 2 && Math.abs(zscore) > UNREALIZED_ZSCORE_WARNING) {
            //			logger.warn("something is wrong on this unrealized Pnl {} zscore {}>{}", unrealizedPnlProposal, zscore,
            //					UNREALIZED_ZSCORE_WARNING);
            return false;
        }
        return true;
    }

    public long getTimestamp(Long timestampIn) {
        if (timestampIn < lastTimestampUpdate) {
            timestampIn = lastTimestampUpdate;
        }
//		if (historicalNumberOfTrades.containsKey(timestampIn)) {
//			//			if (historicalNumberOfTrades.containsKey(timestampIn)) {
//			int currentNumberOfTrades = numberOfTrades.get();
//			int previousNumberOfTrades = historicalNumberOfTrades.get(timestampIn);
//			if (previousNumberOfTrades > currentNumberOfTrades) {
//				timestampIn -= 1;
//			} else {
//				timestampIn += 1;
//			}
//
//		} else {
//			//modify it by default
//			timestampIn += 1;
//		}
//		//		}
        return timestampIn;
    }

    protected void updateHistoricals(Long timestamp) {
        synchronized (UPDATE_HISTORICAL_LOCK) {
            if ((SAVE_ZERO_TRADES || numberOfTrades.get() > 0) && timestamp > 0) {
                //			if (!checkUnrealizedHistorical(unrealizedPnl)) {
                //				//unacceptable mare than 15 zscores directly should be an error
                //				nextCustomReject=true;
                //				return;
                //			}
                calendar.setTime(new Date(timestamp));
                int year = calendar.get(Calendar.YEAR) - 1900;
                if (year < 80) {//lower 1980 is strange
                    logger.warn("trying to update historicals with year {}", new Date(timestamp));
                }
                timestamp = getTimestamp(timestamp);
                lastTimestampUpdate = timestamp;
                historicalTimestamp.add(timestamp);
                historicalNetPosition.put(timestamp, netPosition);
                historicalAvgOpenPrice.put(timestamp, avgOpenPrice);
                historicalNetInvestment.put(timestamp, netInvestment);
                historicalRealizedPnl.put(timestamp, realizedPnl);
                historicalUnrealizedPnl.put(timestamp, unrealizedPnl);

                historicalTotalPnl.put(timestamp, totalPnl);
                historicalSpread.put(timestamp, spread);
                historicalAlgorithmInfo.put(timestamp, algorithmInfo);
                historicalInstrumentPk.put(timestamp, instrumentPk);
                historicalPrice.put(timestamp, lastPrice);

                Double bestBid = lastDepth != null ? lastDepth.getBestBid() : Double.NaN;
                Double bestAsk = lastDepth != null ? lastDepth.getBestAsk() : Double.NaN;
                Double midPrice = lastDepth != null ? lastDepth.getMidPrice() : Double.NaN;
                historicalBidPrice.put(timestamp, bestBid);
                historicalAskPrice.put(timestamp, bestAsk);
                historicalMidPrice.put(timestamp, midPrice);

                historicalQuantity.put(timestamp, lastQuantity);
                historicalVerb.put(timestamp, lastVerb);
                historicalClOrdId.put(timestamp, lastClOrdId);
                historicalFee.put(timestamp, lastFee);

                historicalNumberOfTrades.put(timestamp, numberOfTrades.get());
                historicalNumberOfAggressorTrades.put(timestamp, numberOfAggressorTrades.get());
                try {
                    lastSpreads.offer(historicalSpread.get(timestamp));
                    lastUnrealizedPnls.offer(historicalUnrealizedPnl.get(timestamp));
                } catch (Exception e) {
                    logger.error("error adding lastSpreads or lastUnrealizedPnls ", e);
                }

            }
        }
    }


    public void updateHistoricalsCustom(Long timestamp, String key, double value) {
        //		if(nextCustomReject){
        //			nextCustomReject=false;
        //			return;
        //		}
        synchronized (UPDATE_HISTORICAL_LOCK) {
            if ((SAVE_ZERO_TRADES || numberOfTrades.get() > 0) && timestamp > 0) {
                //				timestamp = getTimestamp(timestamp);

                CustomColumn customColumn = new CustomColumn(key, value);
                List<CustomColumn> columnsList = historicalCustomColumns.getOrDefault(timestamp, new ArrayList<>());
                if (!columnsList.contains(customColumn)) {
                    columnsList.add(customColumn);
                }
                historicalCustomColumns.put(timestamp, columnsList);
            }
        }
    }

    public void setLastTimestampUpdate(long lastTimestampUpdate) {
        this.lastTimestampUpdate = lastTimestampUpdate;
    }

    public synchronized void updateExecutionReportTrade(ExecutionReport executionReport) {

        boolean validQuantity = !(executionReport.getLastQuantity() == 0 || Double
                .isNaN(executionReport.getLastQuantity()) || Double.isInfinite(executionReport.getLastQuantity()));

        if (!validQuantity) {
            logger.warn("can't update trade in portfolio manager with lastQuantity {}",
                    executionReport.getLastQuantity());
            return;
        }
        boolean validPrice = !(Double.isNaN(executionReport.getPrice()) || Double
                .isInfinite(executionReport.getPrice()));

        if (!validPrice) {
            logger.warn("can't update trade in portfolio manager with not valid price {}", executionReport.getPrice());
            return;
        }

        boolean isValidVerb = executionReport.getVerb() != null;
        if (!isValidVerb) {
            logger.warn("cant update trade in portfolio manager with not valid verb {}", executionReport.getVerb());
            return;
        }

        if (lastTimestampExecutionReportUpdate != 0
                && executionReport.getTimestampCreation() < lastTimestampExecutionReportUpdate) {
            logger.warn("execution report received of the past {}", executionReport);
        }
        if (processedClOrdId.containsKey(executionReport.getClientOrderId()) && processedClOrdId
                .get(executionReport.getClientOrderId()).equals(ExecutionReportStatus.CompletelyFilled)) {
            logger.warn("cant update trade in portfolio manager {} already processed with status {} received {} ",
                    executionReport.getClientOrderId(), processedClOrdId.get(executionReport.getClientOrderId()),
                    executionReport.getExecutionReportStatus());
            return;
        }

        //check only for live trading
        long currentTime = System.currentTimeMillis();
        if (!isBacktest && !isPaper && (currentTime - executionReport.getTimestampCreation()) > 60 * 60 * 1000) {
            logger.error(
                    "something is wrong a lot of time since execution report was sent! , more than 1 hour?   {}>{}",
                    new Date(currentTime), new Date(executionReport.getTimestampCreation()));
        }
        //

        //		if ((currentTime - executionReport.getTimestampCreation()<1000*60)) {
        //			logger.error("something is wrong  currentTime less ER time  {}<{}",new Date(currentTime),new Date( executionReport.getTimestampCreation()));
        //
        //		}
        double previousPrice = lastPrice;
        double previousQty = lastQuantity;
        String previousVerb = lastVerb;
        String previousClOrdId = lastClOrdId;

        lastPrice = executionReport.getPrice();
        lastQuantity = executionReport.getLastQuantity();
        lastVerb = executionReport.getVerb().name();
        lastClOrdId = executionReport.getClientOrderId();

        //		lastPrice = Math.min(lastPrice, maxExecutionPriceValid);
        //		lastPrice = Math.max(lastPrice, minExecutionPriceValid);
        //		if (lastPrice == maxExecutionPriceValid || lastPrice == minExecutionPriceValid) {
        //			logger.warn("{} ER price {} bounded to max or min value {}", executionReport.getClientOrderId(),
        //					executionReport.getPrice(), lastPrice);
        //		}
        Instrument instrument = Instrument.getInstrument(executionReport.getInstrument());
        boolean isTaker = isTaker(instrument, executionReport.getPrice(), executionReport.getVerb());
        lastFee = instrument.calculateFee(isTaker, executionReport.getPrice(), executionReport.getLastQuantity());

        double quantityMultiplier = DEFAULT_QUANTITY_MULTIPLIER;
        if (instrument != null) {
            quantityMultiplier = instrument.getQuantityMultiplier();
        }
        double quantityWithDirection = executionReport.getLastQuantity();
        if (executionReport.getVerb().equals(Verb.Sell)) {
            quantityWithDirection = -1 * quantityWithDirection;
        }
        double newPosition = netPosition + quantityWithDirection;

        boolean isClosePosition = newPosition == 0;//close pnl is saved when we have position ==0
        boolean isChangeSide = netPosition != 0 && Math.signum(netPosition) != Math.signum(newPosition);
        boolean partialClosePosition =
                netPosition != 0 && Math.signum(netPosition) != Math.signum(quantityWithDirection);
        //			realizedPnl
        if (isClosePosition || isChangeSide || partialClosePosition) {
            //closed position
            double minPositionAbs = Math.min(Math.abs(quantityWithDirection), Math.abs(netPosition));
            double sidePosition = Math.signum(netPosition);
            double initialPrice = avgOpenPrice;//TODO use openBids or openAsks
            double realizedPnlTemp = (lastPrice - initialPrice) * minPositionAbs * sidePosition * quantityMultiplier;
            if (Double.isNaN(realizedPnlTemp)) {
                realizedPnlTemp = 0.0;
            }
            realizedPnl += realizedPnlTemp;
            netPosition = newPosition;
            lastQuantity = 0.0;
        }

        //			totalPnl

        //			avg open price
        double prevAvgOpenPrice = avgOpenPrice;
        if (!isClosePosition) {
            double newAvgOpenPrice = lastPrice;
            if (!Double.isFinite(newAvgOpenPrice)) {
                newAvgOpenPrice = prevAvgOpenPrice;
            }
            double lastNominal = prevAvgOpenPrice * Math.abs(netPosition);
            double newNominal = newAvgOpenPrice * Math.abs(lastQuantity);
            double newOpenCapital = lastNominal + newNominal;

            double divisor = Math.abs(netPosition) + Math.abs(lastQuantity);
            if (lastNominal == 0) {
                divisor = Math.abs(lastQuantity);
            }
            avgOpenPrice = newOpenCapital / divisor;

        } else {
            if (executionReport.getLastQuantity() > Math.abs(netPosition)) {
                avgOpenPrice = lastPrice;
            }
        }

        //			net investment
        netInvestment = Math.abs(newPosition * avgOpenPrice);
        //net position
        netPosition = newPosition;
        algorithmInfo = executionReport.getAlgorithmInfo();
        //number of trades
        numberOfTrades.incrementAndGet();
        if (executionReport.isAggressor()) {
            numberOfAggressorTrades.incrementAndGet();
        } else {
            numberOfAggressedTrades.incrementAndGet();
        }
        lastTimestampExecutionReportUpdate = executionReport.getTimestampCreation();

        //

        updateDepth(lastDepth);//update unrealized pnl
        totalPnl = realizedPnl + unrealizedPnl;
        totalFees = realizedFees + unrealizedFees;

        //historical
        updateHistoricals(executionReport.getTimestampCreation());
        processedClOrdId.put(executionReport.getClientOrderId(), executionReport.getExecutionReportStatus());

    }

    public boolean isTaker(Instrument instrument, double price, Verb verb) {
        if (lastDepth == null) {
            logger.warn("not possible to know if we are takers! without depth -> return false");
            return false;
        }

        if (verb.equals(Verb.Buy)) {
            //			if(price>lastDepth.getMidPrice()){
            if (price >= lastDepth.getBestAsk()) {
                return true;
            }
        }

        if (verb.equals(Verb.Sell)) {
            //			if(price<lastDepth.getMidPrice()){
            if (price <= lastDepth.getBestBid()) {
                return true;
            }
        }
        return false;

    }

    public void setLastDepth(Depth lastDepth) {
        this.lastDepth = lastDepth;
    }

    public synchronized void updateDepth(Depth depth) {
        if (depth == null || !depth.isDepthFilled()) {
            return;
        }
        Instrument instrument = Instrument.getInstrument(depth.getInstrument());
        double quantityMultiplier = DEFAULT_QUANTITY_MULTIPLIER;
        if (instrument != null) {
            quantityMultiplier = instrument.getQuantityMultiplier();
        }

        double lastPrice = depth.getMidPrice();
        if (lastPrice != 0 && avgOpenPrice != 0 && Double.isFinite(lastPrice) && Double.isFinite(avgOpenPrice)) {
            lastPriceForUnrealized = lastPrice;
            boolean lastPriceSideFound = false;

            if (netPosition > 0) {
                int levelFill = 0;
                double qtyLeft = Math.abs(netPosition);
                double priceTotal = 0.;
                while (levelFill < depth.getBidLevels()) {
                    qtyLeft -= depth.getBidsQuantities()[levelFill];
                    priceTotal += depth.getBids()[levelFill];
                    levelFill++;
                    if (qtyLeft <= 0) {
                        break;
                    }
                }
                if (levelFill > 0) {
                    lastPriceForUnrealized = priceTotal / levelFill;
                    lastPriceSideFound = true;
                }

            } else if (netPosition < 0) {
                int levelFill = 0;
                double qtyLeft = Math.abs(netPosition);
                double priceTotal = 0.;
                while (levelFill < depth.getAskLevels()) {
                    qtyLeft -= depth.getAsksQuantities()[levelFill];
                    priceTotal += depth.getAsks()[levelFill];
                    levelFill++;
                    if (qtyLeft <= 0) {
                        break;
                    }
                }
                if (levelFill > 0) {
                    lastPriceForUnrealized = priceTotal / levelFill;
                    lastPriceSideFound = true;
                }
            }

            if (!lastPriceSideFound) {
                lastPriceForUnrealized = depth.getMidPrice();//to have something negative at least
            }
            double initialPrice = avgOpenPrice;//TODO use openBids or openAsks
            double unrealizedPnlProposal = (lastPriceForUnrealized - initialPrice) * netPosition;
            boolean isOnLimitsOfPnl = true;
            if (CHECK_OPEN_PNL) {
                isOnLimitsOfPnl = checkUnrealizedHistorical(unrealizedPnlProposal);//remove here
            }
            if (!isOnLimitsOfPnl) {
                //dont update unrealized to not distort results open pnl plots
                //				logger.warn("unrealizedPnlProposal is out of bounds {} => using previous {}", unrealizedPnlProposal,
                //						unrealizedPnl);
            } else {
                unrealizedPnl = unrealizedPnlProposal * quantityMultiplier;
                midpricesQueue.offer(lastPrice);
                calculateBoundariesPrice(lastPriceForUnrealized);
            }

        }
        totalPnl = unrealizedPnl + realizedPnl;
        totalFees = realizedFees + unrealizedFees;
        lastDepth = depth;
        spread = depth.getSpread();
//        updateHistoricals(depth.getTimestamp());

    }

    protected Double getStdMidPrice() {
        if (midpricesQueue == null) {
            return 0.0;
        }
        if (midpricesQueue.size() < windowTick) {
            return 0.0;
        }
        if (stdMidPrice != 0) {
            //calculate once only
            return stdMidPrice;
        }
        Double[] midPricesArr = new Double[windowTick];
        try {
            Double[] midPricesArrtemp = new Double[midpricesQueue.size()];
            midPricesArrtemp = midpricesQueue.toArray(midPricesArrtemp);
            if (midpricesQueue.size() == windowTick) {
                midPricesArr = midPricesArrtemp;
            } else {
                int indexArr = windowTick - 1;
                for (int lastElements = midPricesArrtemp.length - 1; lastElements >= 0; lastElements--) {
                    midPricesArr[indexArr] = midPricesArrtemp[lastElements];
                    indexArr--;
                    if (indexArr < 0) {
                        break;
                    }
                }
            }

        } catch (IndexOutOfBoundsException e) {
            logger.error("error calculating std on {} windows tick with size {}-> return last stdMidPrice", windowTick,
                    midpricesQueue.size());
            return this.stdMidPrice;
        }

        double std = GetStd(midPricesArr);
        //		double sum = 0.;
        //		for (int i = 0; i < windowTick; i++) {
        //			sum += midPricesArr[i];
        //		}
        //		double mean = sum / (double) windowTick;
        //		double sqDiff = 0;
        //		for (int i = 0; i < windowTick; i++) {
        //			sqDiff += (midPricesArr[i] - mean) * (midPricesArr[i] - mean);
        //		}
        //		double var = (double) sqDiff / windowTick;
        //		double std = Math.sqrt(var);
        return std;
    }

    protected void calculateBoundariesPrice(double lastPrice) {
        this.stdMidPrice = getStdMidPrice();
        if (this.stdMidPrice == 0.0) {
            return;
        }
        minExecutionPriceValid = lastPrice - 10 * this.stdMidPrice;
        maxExecutionPriceValid = lastPrice + 10 * this.stdMidPrice;

    }

    public String dumpString() {
        String output = Configuration.formatLog("" +
                        "{}\n" +
                        "\tLastUpdate:{}\n" +
                        "\tTrades: {} (agg: {})\n" +
                        "\tUnrealized Pnl: {}\n" +
                        "\tRealized Pnl: {}\n" +
                        "\tFees: {}\n" +
                        "\tPosition: {}\n" +
                        "\tLast :{} {}@{}" +
                        "",
                getInstrumentPk(),
                new Date(getLastTimestampUpdate()).toString(),
                getNumberOfTrades(),
                getNumberOfAggressorTrades(),
                round(getTotalPnl(), 2),
                round(getRealizedPnl(), 2),
                round(getTotalFees(), 2),
                round(getNetPosition(), 4),
                getLastVerb(),
                getLastQuantity(),
                getLastPrice()
        );
        return output;
    }


    //clone object
    public PnlSnapshot clone() {
        PnlSnapshot clone = new PnlSnapshot(this.instrumentPk);
        clone.netPosition = this.netPosition;
        clone.avgOpenPrice = this.avgOpenPrice;
        clone.netInvestment = this.netInvestment;
        clone.realizedPnl = this.realizedPnl;
        clone.unrealizedPnl = this.unrealizedPnl;
        clone.totalPnl = this.totalPnl;
        clone.totalFees = this.totalFees;
        clone.lastPriceForUnrealized = this.lastPriceForUnrealized;
        clone.spread = this.spread;
        clone.realizedFees = this.realizedFees;
        clone.unrealizedFees = this.unrealizedFees;
        clone.algorithmInfo = this.algorithmInfo;
        //historical data is not cloned because we are going to sum it later
        return clone;
    }

    @Override
    public String toString() {
        return "PnlSnapshot{" +
                "instrumentPk='" + instrumentPk +
                '}';
    }
}