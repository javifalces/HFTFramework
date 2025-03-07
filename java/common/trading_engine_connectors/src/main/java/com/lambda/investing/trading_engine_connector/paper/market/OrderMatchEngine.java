package com.lambda.investing.trading_engine_connector.paper.market;

import com.lambda.investing.ArrayUtils;
import com.lambda.investing.Configuration;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * class that is implementing a complete orderbook  , just prices-qty references to backtest faster
 * change in PaperTradingEngine to use it
 */
public class OrderMatchEngine extends OrderbookManager {

    private static double ZERO_QTY_FILL = 1E-10;
    public static boolean REFRESH_DEPTH_ORDER_REQUEST = true;//if true can be Stackoverflow on orderRequest!
    public static boolean REFRESH_DEPTH_TRADES = true;//if true can be Stackoverflow on orderRequest!

    @Setter
    @Getter
    private class FastOrder {

        private double price;
        private double qty;
        private String algorithm;
        private OrderRequest orderRequest;

        public FastOrder() {
        }

    }

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private NavigableMap<Double, List<FastOrder>> bidSide = new ConcurrentSkipListMap<>(Collections.reverseOrder());
    private NavigableMap<Double, List<FastOrder>> askSide = new ConcurrentSkipListMap<>();
    private Map<String, ExecutionReport> executionReportMap = new ConcurrentHashMap<>();
    private Map<String, FastOrder> fastOrderMap = new ConcurrentHashMap<>();
    private volatile long currentTimestamp = 0L;
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
            currentTimestamp = 0L;
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

        List<Double> pricesInDepth = null;
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
            List<FastOrder> ordersInLevel = new ArrayList<>(entry.getValue());
            List<FastOrder> ordersToRemove = new ArrayList<>();
            for (FastOrder fastOrder : ordersInLevel) {
                totLevelQty += fastOrder.qty;
                if (fastOrder.qty <= 0) {
                    ordersToRemove.add(fastOrder);

                    if (fastOrder.orderRequest != null) {
                        fastOrderMap.remove(fastOrder.orderRequest.getClientOrderId());
                    }

                }
            }
            if (totLevelQty > 0) {

                ordersInLevel.removeAll(ordersToRemove);
                side.put(entry.getKey(), ordersInLevel);

            } else {
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
     * @return
     */
    public void refreshMarketMakerDepth(Depth depth) {
        readLock.lock();
        try {
            // Check again in case the currentTimestamp was updated while waiting for the lock
            if (!depth.isDepthValid() || depth.getTimestamp() < currentTimestamp) {
                return;
            }
        } finally {
            readLock.unlock();
        }

        writeLock.lock();
        try {
            currentTimestamp = depth.getTimestamp();
            if (depth.getTimeToNextUpdateMs() != Long.MIN_VALUE) {
                timeToNextUpdateMs = depth.getTimeToNextUpdateMs();
            }

            // Process market maker orders for both sides
            removeMMOrdersDepth(Verb.Buy, depth);//remove first
            //add the bid side
            Double[] bids = depth.getBids();
            Double[] bidsQty = depth.getBidsQuantities();
            boolean thereAreChanges = false;
            for (int level = 0; level < bids.length; level++) {
                FastOrder order = new FastOrder();
                order.algorithm = MARKET_MAKER_ALGORITHM_INFO;
                order.price = bids[level];
                order.qty = bidsQty[level];

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
            Double[] asks = depth.getAsks();
            Double[] asksQty = depth.getAsksQuantities();
            for (int level = 0; level < asks.length; level++) {
                FastOrder order = new FastOrder();
                order.algorithm = MARKET_MAKER_ALGORITHM_INFO;
                order.price = asks[level];
                order.qty = asksQty[level];

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
            Depth finalDepth = getDepth();
            this.paperTradingEngineConnector.notifyDepth(finalDepth);
            lastDepthRefreshed = depth;
        } finally {
            writeLock.unlock();
        }
    }

    private ExecutionReport getExecutionReport(OrderRequest orderSent) {
        readLock.lock();
        try {
            ExecutionReport executionReportOut = executionReportMap
                    .getOrDefault(orderSent.getClientOrderId(), new ExecutionReport(orderSent));
            long timestamp = Math.max(currentTimestamp, orderSent.getTimestampCreation());
            executionReportOut.setTimestampCreation(timestamp);//add more time
            return executionReportOut;
        } finally {
            readLock.unlock();
        }
    }

    private Verb inferVerbFromTrade(com.lambda.investing.model.market_data.Trade trade) {
        try {
            Double bestBid = getSide(Verb.Buy).firstKey();
            Double bestAsk = getSide(Verb.Sell).firstKey();

            Verb output = null;
            if (bestAsk != null && bestBid != null) {
                Double mid = (bestBid + bestAsk) / 2;
                if (trade.getPrice() < mid) {
                    output = Verb.Sell;//cross the spread
                } else if (trade.getPrice() > mid) {
                    output = Verb.Buy;//cross the spread
                } else {
                    //we dont know
                    output = null;
                }
            }
            return output;
        } catch (Exception e) {
            return null;
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
            if (trade.getTimestamp() >= currentTimestamp) {
                currentTimestamp = trade.getTimestamp();
            } else {
                //warning?!
                logger.warn("refreshFillMarketTrade. Trade timestamp is lower than currentTimestamp {} < {}", trade.getTimestamp(), currentTimestamp);
                trade.setTimestamp(currentTimestamp);
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

            if (verb != null && verb.equals(Verb.Buy) && askSide.size() > 0) {
//                NavigableMap<Double, List<FastOrder>> mapToCheck = askSide;//new TreeMap<>(askSide);
                for (Map.Entry<Double, List<FastOrder>> entry : askSide.entrySet()) {
                    Double orderPrice = entry.getKey();
                    if (trade.getPrice() >= orderPrice) {
                        List<FastOrder> orderList = askSide.get(orderPrice);
                        for (FastOrder orderInLevel : orderList) {
                            if (orderInLevel.algorithm.equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
                                qtyTrade -= orderInLevel.qty;
                                continue;
                            }
                            if (qtyTrade <= 0) {
                                continue;
                            }
                            OrderRequest orderSent = orderInLevel.getOrderRequest();
                            if (orderSent == null) {
                                logger.error("error on order without OrderRequest saved!! ASK {} ", orderInLevel.algorithm);
                                continue;
                            }
                            //trade happened in the ask side
                            ExecutionReport executionReport = getExecutionReport(orderSent);
                            double qtyFill = Math.min(executionReport.getQuantity() - executionReport.getQuantityFill(),
                                    trade.getQuantity());

                            qtyTrade -= qtyFill;
                            orderInLevel.qty -= qtyFill;

                            executionReport.setQuantityFill(executionReport.getQuantityFill() + qtyFill);
                            if (executionReport.getQuantityFill() < ZERO_QTY_FILL) {
                                //ignore partial filled! probably already CF
                                continue;
                            }
                            executionReport.setLastQuantity(qtyFill);
                            executionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
                            if (executionReport.getQuantityFill() >= orderSent.getQuantity()) {
                                executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
                            }
                            executionReport.setTimestampCreation(currentTimestamp);

                            executionReportMap.put(executionReport.getClientOrderId(), executionReport);
                            tradeNotified = true;
                            notifyExecutionReport(executionReport);
                        }
                        if (tradeNotified) {
                            //depth has changed!
                            cleanEmptyLevels();
                            if (REFRESH_DEPTH_TRADES) {
                                Depth lastDepth = getDepth();
                                paperTradingEngineConnector.notifyDepth(lastDepth);
                            }
                        } else {
                            paperTradingEngineConnector.notifyTrade(trade);
                        }

                    }
                    if (qtyTrade <= 0) {
                        //will be notified from ER
                        return true;
                    }
                }
            } else if (verb != null && verb.equals(Verb.Sell) && bidSide.size() > 0) {
//                NavigableMap<Double, List<FastOrder>> mapToCheck = bidSide;//new TreeMap<>(bidSide);
                for (Map.Entry<Double, List<FastOrder>> entry : bidSide.entrySet()) {
                    Double orderPrice = entry.getKey();
                    if (trade.getPrice() <= orderPrice) {
                        List<FastOrder> orderList = bidSide.get(orderPrice);
                        for (FastOrder orderInLevel : orderList) {
                            if (orderInLevel.algorithm.equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
                                qtyTrade -= orderInLevel.qty;
                                continue;
                            }
                            if (qtyTrade <= 0) {
                                continue;
                            }
                            OrderRequest orderSent = orderInLevel.getOrderRequest();
                            if (orderSent == null) {
                                logger.error("error on order without OrderRequest saved!! BID {} ", orderInLevel.algorithm);
                                continue;
                            }
                            ExecutionReport executionReport = getExecutionReport(orderSent);
                            double qtyFill = Math.min(executionReport.getQuantity() - executionReport.getQuantityFill(),
                                    trade.getQuantity());

                            qtyTrade -= qtyFill;
                            orderInLevel.qty -= qtyFill;

                            executionReport.setQuantityFill(executionReport.getQuantityFill() + qtyFill);
                            if (executionReport.getQuantityFill() < ZERO_QTY_FILL) {
                                //ignore partial filled! probably already CF
                                continue;
                            }

                            executionReport.setLastQuantity(qtyFill);
                            executionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
                            if (executionReport.getQuantityFill() >= orderSent.getQuantity()) {
                                executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
                            }
                            executionReport.setTimestampCreation(currentTimestamp);
                            executionReportMap.put(orderSent.getClientOrderId(), executionReport);
                            tradeNotified = true;
                            notifyExecutionReport(executionReport);
                        }
                        if (tradeNotified) {
                            //depth has changed!
                            cleanEmptyLevels();
                            if (REFRESH_DEPTH_TRADES) {
                                Depth lastDepth = getDepth();
                                paperTradingEngineConnector.notifyDepth(lastDepth);
                            }
                        } else {
                            paperTradingEngineConnector.notifyTrade(trade);
                        }
                    }
                    if (qtyTrade <= 0) {
                        //will be notified from ER
                        return true;
                    }

                }

            }

            if (!tradeNotified && qtyTrade > 0) {
                //something not in algos
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
        previousOrders.add(newFastOrder);
        side.put(price, previousOrders);
    }

    /***
     *
     * @param orderRequest
     * @param asyncNotify if false , depth will not be notified=> for depth update in market maker algorithm
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

    private FastOrder searchFastOrder(String clOrderId, Verb verb) {
        readLock.lock();
        try {
            if (fastOrderMap.containsKey(clOrderId)) {
                return fastOrderMap.get(clOrderId);
            }
            return null;
        } finally {
            readLock.unlock();
        }
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
                        if (REFRESH_DEPTH_ORDER_REQUEST) {
                            Depth depth = getDepth();
                            paperTradingEngineConnector.notifyDepth(depth);
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

    protected ExecutionReport generateRejection(OrderRequest orderRequest, String reason) {
        readLock.lock();
        try {
            ExecutionReport executionReport = new ExecutionReport(orderRequest);
            long time = Math.max(orderRequest.getTimestampCreation(), currentTimestamp);
            executionReport.setTimestampCreation(time);
            executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
            executionReport.setRejectReason(reason);
            return executionReport;
        } finally {
            readLock.unlock();
        }
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

                if (orderRequest.getOrderType().equals(OrderType.Market)) {
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

                for (Map.Entry<Double, List<FastOrder>> entry : side.entrySet()) {
                    if (qtyOfOrder <= 0) {
                        break;
                    }
                    double priceLevel = entry.getKey();
                    boolean orderPriceCrossed = isSelling ? price <= priceLevel : price >= priceLevel;
                    if (orderPriceCrossed) {
                        //match
                        for (FastOrder fastOrder : side.get(priceLevel)) {
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
                                ExecutionReport executionReport = generateRejection(orderRequest,
                                        "can't trade with yourself " + fastOrder.algorithm);
                                notifyExecutionReport(executionReport);
                                return false;
                            } else {

                                //notifyActive
                                ExecutionReport executionReport = getExecutionReport(orderRequest);
                                executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
                                //check price active if directly filled!
                                executionReportMap.put(executionReport.getClientOrderId(), executionReport);
                                activeHasBeenSent = true;
                                notifyExecutionReport(executionReport);

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
                                    if (otherExecutionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)) {
                                        continue;
                                    }
                                    if (otherExecutionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected)) {
                                        continue;
                                    }
                                    double priceExecuted = isSelling ?
                                            Math.max(otherExecutionReport.getPrice(), orderRequest.getPrice()) :
                                            Math.min(otherExecutionReport.getPrice(), orderRequest.getPrice());
                                    otherExecutionReport.setPrice(priceExecuted);

                                    otherExecutionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
                                    otherExecutionReport.setQuantityFill(otherExecutionReport.getQuantityFill() + newFill);
                                    otherExecutionReport.setLastQuantity(newFill);

                                    if (otherExecutionReport.getQuantityFill() < ZERO_QTY_FILL) {
                                        //ignore partial filled! probably already CF
                                        continue;
                                    }

                                    if (otherExecutionReport.getQuantityFill() >= otherExecutionReport.getQuantity()) {
                                        otherExecutionReport
                                                .setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
                                    }
                                    changeOrderbook = true;
                                    executionReportMap.put(otherExecutionReport.getClientOrderId(), otherExecutionReport);
                                    notifyExecutionReport(otherExecutionReport);
                                }

                                //notifyMe
                                ExecutionReport orderER = getExecutionReport(orderRequest);
                                if (orderER.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)) {
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
                                    orderER.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
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
                    if (REFRESH_DEPTH_ORDER_REQUEST) {
                        Depth newDepth = getDepth();
                        paperTradingEngineConnector.notifyDepth(newDepth);
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

    protected Depth getDepth() {
        readLock.lock();
        try {
            Depth depth = new Depth();
            depth.setTimestamp(currentTimestamp);
            depth.setTimeToNextUpdateMs(timeToNextUpdateMs);
            depth.setInstrument(instrumentPk);
            //bid side
            Double[] bidsQuantities = new Double[bidSide.size()];
            Double[] bids = new Double[bidSide.size()];
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
            Double[] asksQuantities = new Double[askSide.size()];
            Double[] asks = new Double[askSide.size()];
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
        } finally {
            readLock.unlock();
        }
    }

    private ExecutionReport createExecutionReport(FastOrder aggressorOrder, FastOrder aggressedOrder, double qtyFill) {
        readLock.lock();
        try {


            ExecutionReport executionReport = getExecutionReport(aggressorOrder.orderRequest);

            if (executionReport.getExecutionReportStatus()
                    .equals(ExecutionReportStatus.CompletellyFilled)) {
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
            double priceExecuted = aggressorOrder.getPrice();

            //if the order is over market price
            if (aggressorOrder.orderRequest.getVerb() == Verb.Buy) {
                priceExecuted = Math.max(aggressorOrder.getPrice(), aggressedOrder.getPrice());
            }
            if (aggressorOrder.orderRequest.getVerb() == Verb.Sell) {
                priceExecuted = Math.min(aggressorOrder.getPrice(), aggressedOrder.getPrice());
            }

            executionReport.setPrice(priceExecuted);

            if (executionReport.getQuantityFill() >= executionReport.getQuantity()) {
                executionReport
                        .setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
            }
            return executionReport;
        } finally {
            readLock.unlock();
        }
    }

    private void crossOrders(List<FastOrder> bidLevelOrders, List<FastOrder> askLevelOrders) {
        readLock.lock();
        try {
            //match in orders!! remove of one side!
            for (FastOrder bidOrder : bidLevelOrders) {
                for (FastOrder askOrder : askLevelOrders) {
                    //check volume of the order sometimes is zero
                    double bidQty = bidOrder.qty;
                    double askQty = askOrder.qty;
                    boolean invalidQty = bidQty <= 0 || askQty <= 0;
                    if (invalidQty) {
//                        logger.error("previous filled? with qty<=0  bidQty:{}  askQty:{}", bidQty, askQty);
                        continue;
                    }
                    boolean isSameAlgo = bidOrder.algorithm.equalsIgnoreCase(askOrder.algorithm);
                    if (isSameAlgo) {
                        //rejection! of same algo trade!
                        logger.error("something must be wrong with trade bid algo {}@{} with ask algo {}@{}",
                                bidOrder.algorithm, bidOrder.price, askOrder.algorithm, askOrder.price);
                        continue;
                    }

                    //match non mm bid with ask
                    double qtyFill = Math.min(bidOrder.qty, askOrder.qty);
                    if (Math.abs(qtyFill) < 1E-10) {
                        //ignore partial filled! probably already CF
                        continue;
                    }
                    bidOrder.qty -= qtyFill;
                    askOrder.qty -= qtyFill;

                    ExecutionReport executionReport = null;
                    if (!bidOrder.algorithm.equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
                        executionReport = createExecutionReport(bidOrder, askOrder, qtyFill);
                    } else if (!askOrder.algorithm.equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
                        executionReport = createExecutionReport(askOrder, bidOrder, qtyFill);
                    }

                    if (executionReport == null) {
                        continue;
                    }


                    executionReportMap.put(executionReport.getClientOrderId(), executionReport);

                    notifyExecutionReport(executionReport);//trades will be notified here

                }

            }

        } finally {
            readLock.unlock();
        }
    }

    //cross orders
    //match in orders!! remove of one side!
    protected void checkExecutions() {
        readLock.lock();
        try {
            NavigableMap<Double, List<FastOrder>> bidSide = getSide(Verb.Buy);
            NavigableMap<Double, List<FastOrder>> askSide = getSide(Verb.Sell);
            if (bidSide == null || askSide == null || bidSide.isEmpty() || askSide.isEmpty()) {
                //avoid errors
                logger.warn("{} no orders to check match", new Date(currentTimestamp));
                return;
            }

            double bestBid = bidSide.firstKey();
            double bestAsk = askSide.firstKey();

            if (bestBid >= bestAsk) {

                //start matching trades!
                //check BID and ASK not crossed after depth update!
                for (Map.Entry<Double, List<FastOrder>> bidLevelEntry : bidSide.entrySet()) {
                    double bidPrice = bidLevelEntry.getKey();
                    List<FastOrder> bidOrdersLevel = bidLevelEntry.getValue();

                    for (Map.Entry<Double, List<FastOrder>> askLevelEntry : askSide.entrySet()) {
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
            readLock.unlock();
        }
    }

}
