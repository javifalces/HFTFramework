package com.lambda.investing.trading_engine_connector.paper.market;

import com.lambda.investing.ArrayUtils;
import com.lambda.investing.Configuration;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;
import gnu.trove.list.TDoubleList;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.lambda.investing.Configuration.BACKTEST_REFRESH_DEPTH_ORDER_REQUEST;
import static com.lambda.investing.Configuration.BACKTEST_REFRESH_DEPTH_TRADES;

/**
 * class that is implementing a complete orderbook  , just prices-qty references to backtest faster
 * change in PaperTradingEngine to use it
 */
public class OrderMatchEngine extends OrderbookManager {

    private static double ZERO_QTY_FILL = 1E-10;

    @Setter
    @Getter
    private class FastOrder {

        private double price;
        private double qty;
        private String algorithm;
        private OrderRequest orderRequest;
        private long timestamp;
        private Verb verb;

        public FastOrder() {
        }

    }

    private static final boolean REJECT_SELF_TRADES = false;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private NavigableMap<Double, List<FastOrder>> bidSide = new ConcurrentSkipListMap<>(Collections.reverseOrder());
    private NavigableMap<Double, List<FastOrder>> askSide = new ConcurrentSkipListMap<>();
    private Map<String, ExecutionReport> executionReportMap = new ConcurrentHashMap<>();
    private Map<String, FastOrder> fastOrderMap = new ConcurrentHashMap<>();

    private volatile long timeToNextUpdateMs = 0L;

    private enum FilledReactionNextDepthEnum {absolute, relative}

    private Depth lastDepthRefreshed = null;
    private static FilledReactionNextDepthEnum TRADE_REACTIVE_NEXT_DEPTH = FilledReactionNextDepthEnum.relative;

    public OrderMatchEngine(Orderbook orderbook, PaperTradingEngine paperTradingEngineConnector, String instrumentPk) {
        super(orderbook, paperTradingEngineConnector, instrumentPk);
        if (this.instrument != null && this.instrument.isFX()) {
            TRADE_REACTIVE_NEXT_DEPTH = FilledReactionNextDepthEnum.absolute;
        }

    }

    public void reset() {
        writeLock.lock();
        try {
            lastTimestamp = 0L;
            lastTimestampBroker = 0L;
            timeToNextUpdateMs = 0L;
            bidSide.clear();
            askSide.clear();
            executionReportMap.clear();
            fastOrderMap.clear();
        } finally {
            writeLock.unlock();
        }
    }

    private void removeMMOrdersDepth(Verb verb, Depth newDepth) {
        TDoubleList pricesInDepth = null;
//        List<Double> qtyInDepth = null;
        NavigableMap<Double, List<FastOrder>> navigableMapTo = null;
        boolean isRelative = TRADE_REACTIVE_NEXT_DEPTH.equals(FilledReactionNextDepthEnum.relative);
        if (verb.equals(Verb.Buy)) {
            navigableMapTo = bidSide;
            pricesInDepth = ArrayUtils.ArrayToList(newDepth.getBids());
        } else if (verb.equals(Verb.Sell)) {
            navigableMapTo = askSide;
            pricesInDepth = ArrayUtils.ArrayToList(newDepth.getAsks());
        } else {
            throw new RuntimeException("Verb not supported");
        }

        List<Double> pricesToRemove = new ArrayList<>();
        for (double mmPrice : navigableMapTo.keySet()) {
            List<FastOrder> ordersInLevel = navigableMapTo.get(mmPrice);
            List<FastOrder> levelsToRemove = new ArrayList<>();
            for (FastOrder order : ordersInLevel) {
                if (order.getAlgorithm().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
                    if (isRelative) {
                        boolean priceExistInNew = pricesInDepth.contains(order.getPrice());
                        if (priceExistInNew) {
                            //don't delete it then...
                            continue;
                        }
                    }
                    levelsToRemove.add(order);
                }
            }
            ordersInLevel.removeAll(levelsToRemove);

            if (ordersInLevel.size() == 0) {
                pricesToRemove.add(mmPrice);
            }
        }
        for (Double price : pricesToRemove) {
            navigableMapTo.remove(price);
        }
    }

    private void cleanEmptyLevels() {
        cleanEmptyPriceLevels(Verb.Buy);
        cleanEmptyPriceLevels(Verb.Sell);
    }

    private void cleanEmptyPriceLevels(Verb verb) {
        NavigableMap<Double, List<FastOrder>> side = getSide(verb);
        List<Double> pricesToRemove = new ArrayList<>();

        for (Map.Entry<Double, List<FastOrder>> entry : side.entrySet()) {
            double totLevelQty = 0.0;
            List<FastOrder> ordersInLevel = entry.getValue();
            Iterator<FastOrder> iterator = ordersInLevel.iterator();

            while (iterator.hasNext()) {
                FastOrder fastOrder = iterator.next();
                totLevelQty += fastOrder.qty;
                if (fastOrder.qty <= 0) {
                    iterator.remove(); // Safely remove the element
                    if (fastOrder.orderRequest != null) {
                        fastOrderMap.remove(fastOrder.orderRequest.getClientOrderId());
                    }
                }
            }

            if (totLevelQty <= 0) {
                pricesToRemove.add(entry.getKey());
            }
        }

        for (double price : pricesToRemove) {
            side.remove(price);
        }
    }

    private void addFastOrder(FastOrder order, Verb verb) {
        NavigableMap<Double, List<FastOrder>> navigableMapTo = bidSide;
        if (verb.equals(Verb.Sell)) {
            navigableMapTo = askSide;
        }

        List<FastOrder> ordersInLevel = navigableMapTo.getOrDefault(order.price, new ArrayList<>());
        ordersInLevel.add(order);
        navigableMapTo.put(order.price, ordersInLevel);
    }

    private NavigableMap<Double, List<FastOrder>> getSide(Verb verb) {
        if (verb.equals(Verb.Sell)) {
            return askSide;
        } else {
            return bidSide;
        }
    }

    /**
     * When a depth is read clean previous orders from MM and send the new snapshost
     *
     * @param depth new depth to refresh
     */
    public void refreshMarketMakerDepth(Depth depth) {
        writeLock.lock();
        try {
            // Check inside writeLock so the guard and the write are atomic
            if (!depth.isDepthValid() || depth.getTimestamp() < lastTimestamp) {
                return;
            }
            lastTimestamp = Math.max(depth.getTimestamp(), lastTimestamp);//take market time
            lastTimestamp = Math.max(depth.getTimestampBrokerConnector(), lastTimestamp);//take broker connector time

            lastTimestampBroker = System.currentTimeMillis();

            if (depth.getTimeToNextUpdateMs() != Long.MIN_VALUE) {
                timeToNextUpdateMs = depth.getTimeToNextUpdateMs();
            }

            // Process market maker orders for both sides
            removeMMOrdersDepth(Verb.Buy, depth);//remove first
            //add the bid side
            double[] bids = depth.getBids();
            double[] bidsQty = depth.getBidsQuantities();
            boolean thereAreChanges = false;
            for (int level = 0; level < bids.length; level++) {
                FastOrder order = new FastOrder();
                order.algorithm = MARKET_MAKER_ALGORITHM_INFO;
                order.price = bids[level];
                order.qty = bidsQty[level];
                order.timestamp = depth.getTimestamp();
                order.verb = Verb.Buy;

                if (TRADE_REACTIVE_NEXT_DEPTH.equals(FilledReactionNextDepthEnum.relative) && lastDepthRefreshed != null) {
                    //difference in qty
                    double newQty = bidsQty[level];
                    double oldQty = lastDepthRefreshed.getBidVolume(order.price);
                    double deltaQty = newQty - oldQty;
                    if (deltaQty <= 0) {
                        continue;
                    }
                    order.qty = deltaQty;
                }

                thereAreChanges = true;
                addFastOrder(order, Verb.Buy);
            }


            removeMMOrdersDepth(Verb.Sell, depth);//remove first
            //add the ask side
            double[] asks = depth.getAsks();
            double[] asksQty = depth.getAsksQuantities();
            for (int level = 0; level < asks.length; level++) {
                FastOrder order = new FastOrder();
                order.algorithm = MARKET_MAKER_ALGORITHM_INFO;
                order.price = asks[level];
                order.qty = asksQty[level];
                order.timestamp = depth.getTimestamp();
                order.verb = Verb.Sell;

                if (TRADE_REACTIVE_NEXT_DEPTH.equals(FilledReactionNextDepthEnum.relative) && lastDepthRefreshed != null) {
                    //difference in qty
                    double newQty = asksQty[level];
                    double oldQty = lastDepthRefreshed.getAskVolume(order.price);
                    double deltaQty = newQty - oldQty;
                    if (deltaQty <= 0) {
                        continue;
                    }
                    order.qty = deltaQty;
                }

                thereAreChanges = true;
                addFastOrder(order, Verb.Sell);
            }

//            if (thereAreChanges) {
//                checkExecutions();
//                cleanEmptyLevels();
//                Depth getFinalDepth = getDepth();
//                this.paperTradingEngineConnector.notifyDepth(getFinalDepth);
//            }

            // After processing, check for executions and clean empty levels
            checkExecutions();
            cleanEmptyLevels();

            // Notify the new depth if there are changes
            Depth finalDepth = getDepth();//from pool notifyDepth is going to delete it
            this.paperTradingEngineConnector.notifyDepth(finalDepth);
            lastDepthRefreshed = depth;
        } finally {
            writeLock.unlock();
        }
    }

    // Caller must hold readLock or writeLock
    private ExecutionReport getExecutionReport(OrderRequest orderSent) {
        ExecutionReport executionReportOut = executionReportMap
                .getOrDefault(orderSent.getClientOrderId(), new ExecutionReport(orderSent));
        long timestamp = Math.max(lastTimestamp, orderSent.getTimestampBrokerConnector());
        executionReportOut.setTimestampCreation(timestamp);//add more time
        executionReportOut.setTimestampBrokerConnector(System.currentTimeMillis());
        return executionReportOut;
    }

    private Verb inferVerbFromTrade(com.lambda.investing.model.market_data.Trade trade) {
        try {
            return trade.inferVerb(lastDepthRefreshed);
        } catch (Exception e) {
            return null;
        }
    }

    private void notifyRefreshedDepth() {
        Depth lastDepth = getDepth();
        if (lastDepth.getTimeToNextUpdateMs() >= Configuration.REFRESH_DEPTH_ORDER_REQUEST_MS) {
            //if there's no time , we are not notifying it
            lastDepth.delayTimestamp(Configuration.REFRESH_DEPTH_ORDER_REQUEST_MS);
            paperTradingEngineConnector.notifyDepth(lastDepth);
        }
    }

    /**
     * When a trade is read check if match any limit order
     *
     * @param trade
     * @return
     */
    public boolean refreshFillMarketTrade(com.lambda.investing.model.market_data.Trade trade) {

        writeLock.lock();
        try {
            if (trade.getTimestamp() >= lastTimestamp || trade.getTimestampBrokerConnector() >= lastTimestamp) {
                lastTimestamp = Math.max(trade.getTimestamp(), lastTimestamp);//take market time
                lastTimestamp = Math.max(trade.getTimestampBrokerConnector(), lastTimestamp);//take broker connector time
                lastTimestampBroker = System.currentTimeMillis();

            } else {
                //warning?!
                logger.warn("refreshFillMarketTrade. Trade timestamp is lower than currentTimestamp {} < {}", trade.getTimestamp(), lastTimestamp);
                trade.setTimestamp(lastTimestamp);
                trade.setTimestampBrokerConnector(System.currentTimeMillis());
            }

            if (trade.getTimeToNextUpdateMs() != Long.MIN_VALUE) {
                timeToNextUpdateMs = trade.getTimeToNextUpdateMs();
            }

            double qtyTrade = trade.getQuantity();
            boolean tradeNotified = false;

            Verb verb = trade.getVerb();
            if (verb == null) {
                verb = inferVerbFromTrade(trade);
                trade.setVerb(verb);//inferring verb side from trade
            }

            NavigableMap<Double, List<FastOrder>> sideToCheck = verb == Verb.Buy ? askSide : bidSide;

            if (verb != null && !sideToCheck.isEmpty()) {
                Iterator<Map.Entry<Double, List<FastOrder>>> sideIterator = sideToCheck.entrySet().iterator();

                while (sideIterator.hasNext() && qtyTrade > 0) {
                    Map.Entry<Double, List<FastOrder>> entry = sideIterator.next();
                    Double orderPrice = entry.getKey();

                    if ((verb == Verb.Buy && trade.getPrice() >= orderPrice) || (verb == Verb.Sell && trade.getPrice() <= orderPrice)) {
                        List<FastOrder> orderList = entry.getValue();
                        Iterator<FastOrder> orderIterator = orderList.iterator();

                        while (orderIterator.hasNext() && qtyTrade > 0) {
                            FastOrder orderInLevel = orderIterator.next();

                            if (orderInLevel.algorithm.equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
                                qtyTrade -= orderInLevel.qty;
                                continue;
                            }

                            if (qtyTrade <= 0) {
                                continue;
                            }

                            OrderRequest orderSent = orderInLevel.getOrderRequest();
                            if (orderSent == null) {
                                logger.error("error on order without OrderRequest saved!! {} {}", verb, orderInLevel.algorithm);
                                continue;
                            }

                            ExecutionReport executionReport = getExecutionReport(orderSent);
                            executionReport.setAggressor(false);
                            double qtyFill = Math.min(executionReport.getQuantity() - executionReport.getQuantityFill(), qtyTrade);

                            qtyTrade -= qtyFill;
                            orderInLevel.qty -= qtyFill;

                            if (orderInLevel.qty <= 0) {
                                orderIterator.remove(); // Safely remove the order
                            }

                            executionReport.setQuantityFill(executionReport.getQuantityFill() + qtyFill);
                            if (executionReport.getQuantityFill() < ZERO_QTY_FILL) {
                                continue;
                            }

                            executionReport.setLastQuantity(qtyFill);
                            executionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
                            if (executionReport.getQuantityFill() >= orderSent.getQuantity()) {
                                executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletelyFilled);
                            }
                            executionReport.setTimestampCreation(lastTimestamp);
                            executionReport.setTimestampBrokerConnector(System.currentTimeMillis());

                            executionReportMap.put(executionReport.getClientOrderId(), executionReport);
                            tradeNotified = true;
                            notifyExecutionReport(executionReport);
                        }

                        if (orderList.isEmpty()) {
                            sideIterator.remove(); // Safely remove the price level if empty
                        }
                    }
                }

                if (tradeNotified) {
                    cleanEmptyLevels();
                    if (BACKTEST_REFRESH_DEPTH_TRADES) {
                        notifyRefreshedDepth();
                    }
                } else {
                    paperTradingEngineConnector.notifyTrade(trade);
                }
            }

            if (!tradeNotified && qtyTrade > 0) {
                trade.setQuantity(qtyTrade);
                paperTradingEngineConnector.notifyTrade(trade);
            }
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    private void updateSide(Verb verb, String algorithmInfo, double price, double qty) {
        NavigableMap<Double, List<FastOrder>> side = bidSide;

        if (verb.equals(Verb.Sell)) {
            side = askSide;
        }
        List<FastOrder> previousOrders = side.getOrDefault(price, new ArrayList<>());
        FastOrder newFastOrder = new FastOrder();
        newFastOrder.price = price;
        newFastOrder.qty = qty;
        newFastOrder.algorithm = algorithmInfo;
        newFastOrder.timestamp = lastDepthRefreshed.getTimestamp();
        newFastOrder.verb = verb;
        previousOrders.add(newFastOrder);
        side.put(price, previousOrders);
    }

    /***
     *
     * @param orderRequest
     * @param asyncNotify if false , depth will not be notified-&gt; for depth update in market maker algorithm
     * @return
     */
    public boolean orderRequest(OrderRequest orderRequest, boolean asyncNotify, boolean fromTradeFill) {
        if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Send)) {
            return orderRequestSend(orderRequest, asyncNotify, fromTradeFill);
        } else if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {
            return orderRequestCancel(orderRequest, asyncNotify, fromTradeFill, false);
        } else if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Modify)) {
            return orderRequestModify(orderRequest, asyncNotify, fromTradeFill);
        } else {
            logger.error("unknown OrderRequestAction!! {}", orderRequest);
            return false;
        }


    }

    // Caller must hold readLock or writeLock
    private FastOrder searchFastOrder(String clOrderId, Verb verb) {
        if (fastOrderMap.containsKey(clOrderId)) {
            return fastOrderMap.get(clOrderId);
        }
        return null;
    }

    private boolean orderRequestCancel(OrderRequest orderRequest, boolean asyncNotify, boolean fromTradeFill,
                                       boolean fromModify) {
        writeLock.lock();
        try {
            boolean cancelFound = false;
            //previous check
            if (orderRequest.getOrigClientOrderId() == null) {
                if (!fromModify) {
                    ExecutionReport executionReport = getExecutionReport(orderRequest);
                    executionReport.setExecutionReportStatus(ExecutionReportStatus.CancelRejected);
                    executionReport.setRejectReason(Configuration.formatLog("OrigClientOrderId is null"));
                    notifyExecutionReport(executionReport);
                }
                return cancelFound;
            }

            if (orderRequest.getAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
                //shouldnt be here
                logger.warn("what this case ->market maker is not sending orderRequest!!! -> add to the depth");
                updateSide(orderRequest.getVerb(), orderRequest.getAlgorithmInfo(), orderRequest.getPrice(),
                        orderRequest.getQuantity());

            } else {

                FastOrder fastOrder = searchFastOrder(orderRequest.getOrigClientOrderId(), orderRequest.getVerb());

                if (fastOrder != null) {
                    fastOrder.qty = 0;
                    fastOrderMap.remove(orderRequest.getOrigClientOrderId());
                    if (!fromModify) {
                        ExecutionReport executionReport = getExecutionReport(fastOrder.orderRequest);
                        executionReport.setOrigClientOrderId(orderRequest.getOrigClientOrderId());
                        executionReport.setClientOrderId(orderRequest.getClientOrderId());
                        executionReport.setExecutionReportStatus(ExecutionReportStatus.Cancelled);
                        notifyExecutionReport(executionReport);
                    }

                    cleanEmptyLevels();
                    if (!fromModify) {
                        //from modify not update Depth yet
                        if (BACKTEST_REFRESH_DEPTH_ORDER_REQUEST) {
                            notifyRefreshedDepth();
                        }
                    }

                    cancelFound = true;
                } else {
                    if (!fromModify) {
                        ExecutionReport executionReport = getExecutionReport(orderRequest);
                        executionReport.setExecutionReportStatus(ExecutionReportStatus.CancelRejected);
                        executionReport.setRejectReason(
                                Configuration.formatLog("{} not found to cancel", orderRequest.getOrigClientOrderId()));
                        notifyExecutionReport(executionReport);

                    }

                }
            }
            return cancelFound;
        } finally {
            writeLock.unlock();
        }
    }

    private boolean orderRequestModify(OrderRequest orderRequest, boolean asyncNotify, boolean fromTradeFill) {
        writeLock.lock();
        try {
            boolean output = false;
            if (orderRequestCancel(orderRequest, asyncNotify, fromTradeFill, true)) {
                output = orderRequestSend(orderRequest, asyncNotify, fromTradeFill);//active will be here!
            }
            if (!output) {
                //send rejection
                ExecutionReport executionReport = getExecutionReport(orderRequest);
                executionReport.setRejectReason("orderRequestModify can't cancel previous order " + orderRequest.getOrigClientOrderId());
                executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
                notifyExecutionReport(executionReport);
            }
            return output;
        } finally {
            writeLock.unlock();
        }
    }

    // Caller must hold readLock or writeLock
    protected ExecutionReport generateRejection(OrderRequest orderRequest, String reason) {
        ExecutionReport executionReport = new ExecutionReport(orderRequest);
        long time = Math.max(orderRequest.getTimestampCreation(), lastTimestamp);
        executionReport.setTimestampCreation(time);
        executionReport.setTimestampBrokerConnector(time);
        executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
        executionReport.setRejectReason(reason);
        return executionReport;
    }

    private boolean orderRequestSend(OrderRequest orderRequest, boolean asyncNotify, boolean fromTradeFill) {
        writeLock.lock();
        try {
            if (orderRequest.getAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
                //shouldnt be here
                logger.warn("what this case ->market maker is not sending orderRequest!!! -> add to the depth");
                updateSide(orderRequest.getVerb(), orderRequest.getAlgorithmInfo(), orderRequest.getPrice(),
                        orderRequest.getQuantity());
            } else {

                NavigableMap<Double, List<FastOrder>> side = getSide(
                        Verb.OtherSideVerb(orderRequest.getVerb()));//get the other side of the order
                boolean isSelling = true;
                if (orderRequest.getVerb().equals(Verb.Buy)) {
                    isSelling = false;
                }

                if (orderRequest.getOrderType().equals(OrderType.Market) && orderRequest.getPrice() == OrderRequest.NOT_SET_PRICE_VALUE) {
                    if (!isSelling) {
                        orderRequest.setPrice(Double.MAX_VALUE);//will buy from lowest to max
                    } else {
                        orderRequest.setPrice(Double.MIN_VALUE);////will sell from highest to min
                    }
                }


                if (orderRequest.getOrderType().equals(OrderType.Stop)) {
                    //TODO add implementation if needed!!!
                    ExecutionReport executionReport = generateRejection(orderRequest, "Stop order not implemented! ");
                    notifyExecutionReport(executionReport);
                    return false;
                }
                String messageRejection = checkOrderRequestSend(orderRequest);
                if (messageRejection != null) {
                    ExecutionReport executionReport = generateRejection(orderRequest, messageRejection);
                    notifyExecutionReport(executionReport);
                    return false;
                }

                double price = orderRequest.getPrice();
                double qtyOfOrder = orderRequest.getQuantity();
                boolean changeOrderbook = false;
                boolean activeHasBeenSent = false;

                // Create a copy of the entries to avoid concurrent modification
                List<Map.Entry<Double, List<FastOrder>>> sideEntries = new ArrayList<>(side.entrySet());

                for (Map.Entry<Double, List<FastOrder>> entry : sideEntries) {
                    if (qtyOfOrder <= 0) {
                        break;
                    }
                    double priceLevel = entry.getKey();
                    boolean orderPriceCrossed = isSelling ? price <= priceLevel : price >= priceLevel;
                    if (orderPriceCrossed) {
                        //match
                        // Create a copy of the orders at this level to avoid concurrent modification
                        List<FastOrder> ordersInLevel = new ArrayList<>(side.get(priceLevel));

                        for (FastOrder fastOrder : ordersInLevel) {
                            //orders in the level
                            if (qtyOfOrder <= 0) {
                                break;
                            }

                            if (fastOrder.qty <= 0) {
                                changeOrderbook = true;//force clean empty levels
                                continue;
                            }
                            if (fastOrder.algorithm.equalsIgnoreCase(orderRequest.getAlgorithmInfo())) {
                                //cant trade with myself!
                                if (REJECT_SELF_TRADES) {
                                    ExecutionReport executionReport = generateRejection(orderRequest,
                                            "can't trade with yourself " + fastOrder.algorithm);
                                    notifyExecutionReport(executionReport);
                                    return false;
                                }
                                continue;//avoid this trade but continue with the next one
                            } else {

                                //notifyActive
                                ExecutionReport executionReport = getExecutionReport(orderRequest);
                                executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
                                //check price active if directly filled!
                                executionReportMap.put(executionReport.getClientOrderId(), executionReport);
                                if (!activeHasBeenSent) {
                                    activeHasBeenSent = true;
                                    notifyExecutionReport(executionReport);
                                }

                                //send ER filled to counterparty
                                double sizeRemainingAtThisLevel = fastOrder.qty - executionReport.getQuantityFill();
                                if (sizeRemainingAtThisLevel <= 0) {
                                    //already filled
                                    sizeRemainingAtThisLevel = fastOrder.qty;
                                }
                                double newFill = Math.min(sizeRemainingAtThisLevel, qtyOfOrder);

                                qtyOfOrder -= newFill;
                                fastOrder.qty -= newFill;//update book

                                //notify counterparty
                                if (!fastOrder.algorithm.equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
                                    ExecutionReport otherExecutionReport = getExecutionReport(fastOrder.orderRequest);
                                    if (otherExecutionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletelyFilled)) {
                                        continue;
                                    }
                                    if (otherExecutionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected)) {
                                        continue;
                                    }
                                    double priceExecuted = isSelling ?
                                            Math.max(otherExecutionReport.getPrice(), orderRequest.getPrice()) :
                                            Math.min(otherExecutionReport.getPrice(), orderRequest.getPrice());
                                    otherExecutionReport.setPrice(priceExecuted);
                                    otherExecutionReport.setAggressor(false);

                                    otherExecutionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
                                    otherExecutionReport.setQuantityFill(otherExecutionReport.getQuantityFill() + newFill);
                                    otherExecutionReport.setLastQuantity(newFill);

                                    if (otherExecutionReport.getQuantityFill() < ZERO_QTY_FILL) {
                                        //ignore partial filled! probably already CF
                                        continue;
                                    }

                                    if (otherExecutionReport.getQuantityFill() >= otherExecutionReport.getQuantity()) {
                                        otherExecutionReport
                                                .setExecutionReportStatus(ExecutionReportStatus.CompletelyFilled);
                                    }
                                    changeOrderbook = true;
                                    executionReportMap.put(otherExecutionReport.getClientOrderId(), otherExecutionReport);
                                    notifyExecutionReport(otherExecutionReport);
                                }

                                //notifyMe
                                ExecutionReport orderER = getExecutionReport(orderRequest);
                                orderER.setAggressor(true);

                                if (orderER.getExecutionReportStatus().equals(ExecutionReportStatus.CompletelyFilled)) {
                                    continue;
                                }
                                if (orderER.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected)) {
                                    continue;
                                }

                                if (newFill == 0.0) {
                                    //rest of the order is not filled
                                    continue;
                                }

                                double priceExecuted = isSelling ?
                                        Math.max(fastOrder.getPrice(), orderRequest.getPrice()) :
                                        Math.min(fastOrder.getPrice(), orderRequest.getPrice());
                                orderER.setPrice(priceExecuted);

                                orderER.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);

                                orderER.setQuantityFill(newFill + orderER.getQuantityFill());
                                orderER.setLastQuantity(newFill);

                                if (executionReport.getQuantityFill() < ZERO_QTY_FILL) {
                                    //ignore partial filled! probably already CF
                                    continue;
                                }

                                if (orderER.getQuantityFill() >= orderER.getQuantity()) {
                                    orderER.setExecutionReportStatus(ExecutionReportStatus.CompletelyFilled);
                                }
                                changeOrderbook = true;
                                executionReportMap.put(orderER.getClientOrderId(), orderER);
                                notifyExecutionReport(orderER);
                            }
                        }
                    }
                }

                if (qtyOfOrder > 0) {
                    //not trade but can be passive! -> send active!
                    NavigableMap<Double, List<FastOrder>> sideToAdd = getSide(orderRequest.getVerb());
                    List<FastOrder> orders = sideToAdd.getOrDefault(orderRequest.getPrice(), new ArrayList<>());
                    FastOrder remainFastOrder = new FastOrder();
                    remainFastOrder.orderRequest = orderRequest;
                    remainFastOrder.algorithm = orderRequest.getAlgorithmInfo();
                    remainFastOrder.price = orderRequest.getPrice();
                    remainFastOrder.qty = qtyOfOrder;
                    remainFastOrder.timestamp = lastTimestamp;
                    remainFastOrder.verb = orderRequest.getVerb();

                    orders.add(remainFastOrder);
                    sideToAdd.put(orderRequest.getPrice(), orders);
                    updateFastOrderMap(remainFastOrder);

                    //send active if not send it before
                    if (!activeHasBeenSent) {
                        ExecutionReport executionReport = getExecutionReport(orderRequest);
                        executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
                        executionReportMap.put(executionReport.getClientOrderId(), executionReport);
                        notifyExecutionReport(executionReport);
                    }
                    changeOrderbook = true;
                }

                if (changeOrderbook) {
                    //notify last status depth Depth!!
                    cleanEmptyLevels();
                    if (BACKTEST_REFRESH_DEPTH_ORDER_REQUEST) {
                        notifyRefreshedDepth();
                    }
                }
            }
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    private static String checkOrderRequestSend(OrderRequest orderRequest) {
        if (!Double.isFinite(orderRequest.getPrice()) || orderRequest.getPrice() == 0) {
            return "price is not valid " + orderRequest.getPrice();
        }
        if (!Double.isFinite(orderRequest.getQuantity()) || orderRequest.getQuantity() <= 0) {
            return "quantity is not valid " + orderRequest.getQuantity();
        }
        if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Modify)
                && orderRequest.getOrigClientOrderId() == null) {
            return "OrigClientOrderId is null in modify ";
        }
        return null;
    }


    private void updateFastOrderMap(FastOrder fastOrder) {
        if (fastOrder.orderRequest.getOrderRequestAction().equals(OrderRequestAction.Send) || fastOrder.orderRequest
                .getOrderRequestAction().equals(OrderRequestAction.Modify)) {
            fastOrderMap.put(fastOrder.orderRequest.getClientOrderId(), fastOrder);
        }
    }

    // Caller must hold readLock or writeLock
    protected Depth getDepth() {
        Depth depth = Depth.getInstancePool();//this is going to the algo directly
        depth.setTimestamp(lastTimestamp);
        depth.setTimeToNextUpdateMs(timeToNextUpdateMs);
        depth.setTimestampBrokerConnector(Math.max(lastTimestampBroker, lastTimestamp));
        depth.setTimestampAlgoConnector(System.currentTimeMillis());
        depth.setInstrument(instrumentPk);
        //bid side
        double[] bidsQuantities = new double[bidSide.size()];
        double[] bids = new double[bidSide.size()];
        List<String>[] bidsAlgorithmInfo = new List[bidSide.size()];

        int bidIndex = 0;
        for (Map.Entry<Double, List<FastOrder>> bidEntry : getSide(Verb.Buy).entrySet()) {
            Double price = bidEntry.getKey();
            Double qty = 0.0;
            List<String> algoInfo = new ArrayList<>();
            for (FastOrder fastOrder : bidEntry.getValue()) {
                qty += fastOrder.getQty();
                algoInfo.add(fastOrder.algorithm);
            }
            bidsQuantities[bidIndex] = qty;
            bids[bidIndex] = price;
            bidsAlgorithmInfo[bidIndex] = algoInfo;

            bidIndex++;
        }
        depth.setBids(bids);
        depth.setBidsQuantities(bidsQuantities);
        depth.setBidsAlgorithmInfo(bidsAlgorithmInfo);
        depth.setBidLevels(bidSide.size());

        //ASK side
        double[] asksQuantities = new double[askSide.size()];
        double[] asks = new double[askSide.size()];
        List<String>[] asksAlgorithmInfo = new List[askSide.size()];

        int askIndex = 0;
        for (Map.Entry<Double, List<FastOrder>> askEntry : getSide(Verb.Sell).entrySet()) {
            Double price = askEntry.getKey();
            Double qty = 0.0;
            List<String> algoInfo = new ArrayList<>();
            for (FastOrder fastOrder : askEntry.getValue()) {
                qty += fastOrder.getQty();
                algoInfo.add(fastOrder.algorithm);
            }
            asksQuantities[askIndex] = qty;
            asks[askIndex] = price;
            asksAlgorithmInfo[askIndex] = algoInfo;
            askIndex++;
        }
        depth.setAsks(asks);
        depth.setAsksQuantities(asksQuantities);
        depth.setAsksAlgorithmInfo(asksAlgorithmInfo);
        depth.setAskLevels(askSide.size());

        depth.setLevelsFromData();
        return depth;
    }


    // Caller must hold writeLock
    private ExecutionReport createExecutionReport(FastOrder aggressorOrder, FastOrder aggressedOrder, double qtyFill) {
        OrderRequest algoOrderRequest = aggressedOrder.orderRequest;
        boolean isAlgoAggressor = false;
        if (algoOrderRequest == null) {
            algoOrderRequest = aggressorOrder.orderRequest;
            isAlgoAggressor = true;
        }

        ExecutionReport executionReport = getExecutionReport(algoOrderRequest);
        executionReport.setAggressor(isAlgoAggressor);
        executionReport.setTimestampBrokerConnector(System.currentTimeMillis());


        if (executionReport.getExecutionReportStatus()
                .equals(ExecutionReportStatus.CompletelyFilled)) {
            return null;
        }
        if (executionReport.getExecutionReportStatus()
                .equals(ExecutionReportStatus.Rejected)) {
            return null;
        }

        executionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
        //filled logic

        executionReport.setQuantityFill(executionReport.getQuantityFill() + qtyFill);
        executionReport.setLastQuantity(qtyFill);

        if (executionReport.getQuantityFill() < ZERO_QTY_FILL) {
            //ignore partial filled! probably already CF
            return null;
        }
        double priceExecuted = aggressedOrder.getPrice();
        if (priceExecuted == Double.MAX_VALUE || priceExecuted == Double.MIN_VALUE) {
            //if market order that is living in the book
            //use aggressor order price
            logger.warn("[{}] {} Market order {} as aggressed {}@{} executed with aggressor {}@{} DEPTH_BidVolume:{} DEPTH_AskVolume:{}", new Date(lastTimestamp), this.instrumentPk, aggressedOrder.getVerb(), aggressedOrder.getQty(), aggressedOrder.getPrice(), aggressorOrder.getQty(), aggressorOrder.getPrice(), lastDepthRefreshed.getBidVolume(), lastDepthRefreshed.getAskVolume());
            priceExecuted = aggressorOrder.getPrice();
        }

        executionReport.setPrice(priceExecuted);

        if (executionReport.getQuantityFill() >= executionReport.getQuantity()) {
            executionReport
                    .setExecutionReportStatus(ExecutionReportStatus.CompletelyFilled);
        }
        return executionReport;
    }

    // Caller must hold writeLock (called from checkExecutions which holds writeLock)
    private void crossOrders(List<FastOrder> bidLevelOrders, List<FastOrder> askLevelOrders) {
        // Create defensive copies of the collections to avoid concurrent modification
        List<FastOrder> bidOrdersCopy = new ArrayList<>(bidLevelOrders);
        List<FastOrder> askOrdersCopy = new ArrayList<>(askLevelOrders);

        // Track orders to remove after processing to avoid ConcurrentModificationException
        List<FastOrder> bidsToRemove = new ArrayList<>();
        List<FastOrder> asksToRemove = new ArrayList<>();
        // Collect execution reports to notify after processing (outside inner loops)
        List<ExecutionReport> reportsToNotify = new ArrayList<>();

        for (FastOrder bidOrder : bidOrdersCopy) {
            if (bidOrder.qty <= 0) continue;

            for (FastOrder askOrder : askOrdersCopy) {
                if (askOrder.qty <= 0) continue;

                // Skip self-trades
                if (bidOrder.algorithm.equalsIgnoreCase(askOrder.algorithm)) {
                    if (REJECT_SELF_TRADES) {
                        logger.error("Trade between same algorithm: bid {}@{} and ask {}@{}",
                                bidOrder.algorithm, bidOrder.price, askOrder.algorithm, askOrder.price);
                    }
                    //if reject just dont trade the error
                    continue;
                }

                double qtyFill = Math.min(bidOrder.qty, askOrder.qty);
                if (qtyFill <= 1E-10) {
                    continue; // Ignore tiny fills
                }

                bidOrder.qty -= qtyFill;
                askOrder.qty -= qtyFill;

                // Mark orders for removal if qty is depleted
                if (bidOrder.qty <= 1E-10 && !bidsToRemove.contains(bidOrder)) {
                    bidsToRemove.add(bidOrder);
                }
                if (askOrder.qty <= 1E-10 && !asksToRemove.contains(askOrder)) {
                    asksToRemove.add(askOrder);
                }

                // Determine which order is the aggressor
                FastOrder aggressorOrder = bidOrder.getTimestamp() < askOrder.getTimestamp() ? askOrder : bidOrder;
                FastOrder aggressedOrder = bidOrder.getTimestamp() < askOrder.getTimestamp() ? bidOrder : askOrder;
                ExecutionReport executionReport = createExecutionReport(aggressorOrder, aggressedOrder, qtyFill);

                if (executionReport != null) {
                    executionReportMap.put(executionReport.getClientOrderId(), executionReport);
                    reportsToNotify.add(executionReport);
                }

                // Stop processing this bid if its quantity is depleted
                if (bidOrder.qty <= 1E-10) {
                    break;
                }
            }
        }

        // Now safely remove orders after all processing is complete
        bidLevelOrders.removeAll(bidsToRemove);
        askLevelOrders.removeAll(asksToRemove);

        // Notify execution reports after all book updates are done, still holding writeLock
        for (ExecutionReport er : reportsToNotify) {
            notifyExecutionReport(er);
        }
    }

    protected void checkExecutions() {
        writeLock.lock();
        try {
            NavigableMap<Double, List<FastOrder>> bidSide = getSide(Verb.Buy);
            NavigableMap<Double, List<FastOrder>> askSide = getSide(Verb.Sell);
            if (bidSide == null || askSide == null || bidSide.isEmpty() || askSide.isEmpty()) {
                //avoid errors
                logger.warn("[{}] {} no orders in orderbook to check match", new Date(lastTimestamp), instrumentPk);
                return;
            }

            double bestBid = bidSide.firstKey();
            double bestAsk = askSide.firstKey();

            if (bestBid >= bestAsk) {
                //start matching trades!
                //check BID and ASK not crossed after depth update!
                for (Map.Entry<Double, List<FastOrder>> bidLevelEntry : new HashMap<>(bidSide).entrySet()) {
                    double bidPrice = bidLevelEntry.getKey();
                    List<FastOrder> bidOrdersLevel = bidLevelEntry.getValue();

                    for (Map.Entry<Double, List<FastOrder>> askLevelEntry : new HashMap<>(askSide).entrySet()) {
                        double askPrice = askLevelEntry.getKey();
                        List<FastOrder> askOrdersLevel = askLevelEntry.getValue();
                        boolean crossed = bidPrice >= askPrice;

                        if (crossed) {
                            crossOrders(bidOrdersLevel, askOrdersLevel);
                        }
                    }
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

}
