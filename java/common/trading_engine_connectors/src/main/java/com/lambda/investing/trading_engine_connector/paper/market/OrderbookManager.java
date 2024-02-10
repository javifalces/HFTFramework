package com.lambda.investing.trading_engine_connector.paper.market;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lambda.investing.market_data_connector.csv_file_reader.CSVMarketDataConnectorPublisher.ALGORITHM_INFO_MM;

public class OrderbookManager {

    private static final int MAX_SIZE_CACHE_MARKETMAKER_CLIENT_ORDER_ID = 2500;
    private static final int MAX_SIZE_CACHE_ALGOS_CLIENT_ORDER_ID = 250;
    private static final boolean NOTIFY_DEPTH_ONLY_ON_MM = true;//if false slower backtest and recursive errors can happened

    public static String MARKET_MAKER_ALGORITHM_INFO = ALGORITHM_INFO_MM;
    Logger logger = LogManager.getLogger(OrderbookManager.class);
    private static String NOT_FOUND_REJECT_REASON_FORMAT = "ClientOrderId %s not found for %s";
    private Orderbook orderbook;
    private Map<String, Integer> clientOrderIdToOrdId;
    private Map<String, OrderOrderbook> clientOrderIdToOrderOrderbook;
    private Map<Integer, String> ordIdToClientOrderId;
    private Map<String, ExecutionReport> executionReportMap;

    private Map<String, OrderRequest> marketMakerActiveOrders;
    private Map<Integer, String> mmBidLevelToClientOrderId;
    private Map<Integer, String> mmAskLevelToClientOrderId;

    private Map<String, OrderRequest> marketMakerCacheOrderRequest;
    private Map<String, OrderRequest> algosCacheOrderRequest;

    private AtomicInteger orderId;
    private boolean verbose = false;
    protected PaperTradingEngine paperTradingEngineConnector;
    private long lastTimestamp = -1;
    private Map<String, Depth> asyncNotification;
    protected String instrumentPk;

    private Object lockOrderRequest = new Object();

    public OrderbookManager(Orderbook orderbook, PaperTradingEngine paperTradingEngineConnector, String instrument) {
        this.instrumentPk = instrument;
        this.orderbook = orderbook;
        this.paperTradingEngineConnector = paperTradingEngineConnector;

        mmBidLevelToClientOrderId = new HashMap<>();
        mmAskLevelToClientOrderId = new HashMap<>();
        clientOrderIdToOrdId = new HashMap<>();
        ordIdToClientOrderId = new HashMap<>();
        clientOrderIdToOrderOrderbook = new HashMap<>();
        executionReportMap = new HashMap<>();
        asyncNotification = new HashMap<>();
        marketMakerActiveOrders = new HashMap<>();

        marketMakerCacheOrderRequest = new LinkedHashMap<String, OrderRequest>(
                MAX_SIZE_CACHE_MARKETMAKER_CLIENT_ORDER_ID) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, OrderRequest> entry) {
                return size() > MAX_SIZE_CACHE_MARKETMAKER_CLIENT_ORDER_ID;
            }
        };

        algosCacheOrderRequest = new LinkedHashMap<String, OrderRequest>(MAX_SIZE_CACHE_ALGOS_CLIENT_ORDER_ID) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, OrderRequest> entry) {
                return size() > MAX_SIZE_CACHE_ALGOS_CLIENT_ORDER_ID;
            }
        };

        orderId = new AtomicInteger();
    }

    public void reset() {
        this.orderbook.reset();
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public Orderbook getOrderbook() {
        return orderbook;
    }

    private int generateOrderId(String clientOrderId) {
        int output = orderId.getAndIncrement();
        clientOrderIdToOrdId.put(clientOrderId, output);
        ordIdToClientOrderId.put(output, clientOrderId);
        return output;
    }

    private String getSide(Verb verb) {
        String side = verb.equals(Verb.Buy) ? "bid" : "ask";
        return side;
    }

    private OrderOrderbook createOrderOrderbook(OrderRequest orderRequest) {
        boolean isLimit = orderRequest.getOrderType().equals(OrderType.Limit);
        int newOrderId = generateOrderId(orderRequest.getClientOrderId());
        String side = getSide(orderRequest.getVerb());
        OrderOrderbook orderOrderbook = new OrderOrderbook(orderRequest.getTimestampCreation(), isLimit,
                orderRequest.getQuantity(), newOrderId, side, orderRequest.getPrice(), orderRequest.getAlgorithmInfo(),
                orderRequest.getClientOrderId());
        clientOrderIdToOrderOrderbook.put(orderRequest.getClientOrderId(), orderOrderbook);
        return orderOrderbook;
    }

    private OrderRequest generateMarketMakerOrderRequest(String instrumentPk, Verb verb, double price,
                                                         double quantity) {
        String algorithmInfo = MARKET_MAKER_ALGORITHM_INFO;
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setVerb(verb);
        orderRequest.setAlgorithmInfo(algorithmInfo);
        orderRequest.setClientOrderId(generateClientOrderId());
        orderRequest.setOrderRequestAction(OrderRequestAction.Send);
        orderRequest.setOrderType(OrderType.Limit);
        orderRequest.setPrice(price);
        orderRequest.setQuantity(quantity);
        orderRequest.setFreeText("backtest market maker");
        orderRequest.setInstrument(instrumentPk);

        return orderRequest;
    }

    private String generateClientOrderId() {
        return UUID.randomUUID().toString();
    }

    private void fillOrderbook(Depth depth) {
        //send bids
        if (depth.getTimestamp() < lastTimestamp) {
            return;
        }
        try {
            depth = (Depth) depth.clone();
        } catch (CloneNotSupportedException e) {
            logger.error("cant clone depth ", e);
            //			e.printStackTrace();
        }
        lastTimestamp = depth.getTimestamp();

        int maxLevels = depth.getLevels();
        List<String> newBidClientOrderId = new ArrayList<>();
        List<String> newAskClientOrderId = new ArrayList<>();

        int bidsLevel = depth.getBidLevels();
        if (depth.getBids() == null) {
            bidsLevel = 0;
        }
        for (int level = 0; level < bidsLevel; level++) {

            double priceBid = depth.getBids()[level];
            double quantityBid = depth.getBidsQuantities()[level];
            if (Double.isFinite(priceBid) && Double.isFinite(quantityBid)) {
                OrderRequest bidOrderRequest = generateMarketMakerOrderRequest(depth.getInstrument(), Verb.Buy,
                        priceBid, quantityBid);
                bidOrderRequest.setTimestampCreation(depth.getTimestamp());

                //if possible modify it
                String bidOrigClientOrderId = mmBidLevelToClientOrderId.get(level);
                if (bidOrigClientOrderId != null) {
                    bidOrderRequest.setOrigClientOrderId(bidOrigClientOrderId);
                    bidOrderRequest.setOrderRequestAction(OrderRequestAction.Modify);
                }

                //not notify yet
                orderRequest(bidOrderRequest, true, false);
                mmBidLevelToClientOrderId.put(level, bidOrderRequest.getClientOrderId());
                newBidClientOrderId.add(bidOrderRequest.getClientOrderId());
            }
        }

        int asksLevel = depth.getAskLevels();
        if (depth.getAsks() == null) {
            asksLevel = 0;
        }
        for (int level = 0; level < asksLevel; level++) {
            double priceAsk = depth.getAsks()[level];
            double quantityAsk = depth.getAsksQuantities()[level];
            if (Double.isFinite(priceAsk) && Double.isFinite(quantityAsk)) {
                OrderRequest askOrderRequest = generateMarketMakerOrderRequest(depth.getInstrument(), Verb.Sell,
                        priceAsk, quantityAsk);
                askOrderRequest.setTimestampCreation(depth.getTimestamp());

                //if possible modify it
                String askOrigClientOrderId = mmAskLevelToClientOrderId.get(level);
                if (askOrigClientOrderId != null) {
                    askOrderRequest.setOrigClientOrderId(askOrigClientOrderId);
                    askOrderRequest.setOrderRequestAction(OrderRequestAction.Modify);
                }

                orderRequest(askOrderRequest, true, false);
                mmAskLevelToClientOrderId.put(level, askOrderRequest.getClientOrderId());
                newAskClientOrderId.add(askOrderRequest.getClientOrderId());
            }
        }
        //clean the rest of market makers,just in case
        cleanOrderbookDepth(newBidClientOrderId, Verb.Buy, depth.getTimestamp());
        cleanOrderbookDepth(newAskClientOrderId, Verb.Sell, depth.getTimestamp());
        //		logger.debug("orderbook fill depth ->  {}",asyncNotification.get(depth.getInstrument()).toString());
        //notify now!

        try {
            notifyDepthAsync(depth.getInstrument());
        } catch (Exception e) {
            asyncNotification.remove(depth.getInstrument());
            logger.error("error notifying depth async {}", depth.getInstrument(), e);
            return;
        }

    }

    private void cleanOrderbookDepth(List<String> mmOrdersNotToDelete, Verb verb, Long timestamp) {
        OrderTree orderTree = null;
        if (verb.equals(Verb.Sell)) {
            orderTree = orderbook.getAsks();
        }
        if (verb.equals(Verb.Buy)) {
            orderTree = orderbook.getBids();
        }
        String side = getSide(verb);
        Map<Integer, OrderOrderbook> orders = orderTree.orderMap;
        for (Map.Entry<Integer, OrderOrderbook> entry : orders.entrySet()) {
            Integer orderId = entry.getKey();
            OrderOrderbook orderOrderbook = entry.getValue();
            if (mmOrdersNotToDelete.contains(orderOrderbook.getClientOrderId())) {
                continue;
            }
            if (!orderOrderbook.getAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
                continue;
            }
            orderbook.cancelOrder(side, orderOrderbook.getNextOrderOrderId(), timestamp);
        }

    }

    /**
     * When a depth is read clean previous orders from MM and send the new snapshost
     *
     * @param depth new depth to refresh
     * @return
     */
    @Deprecated()
    public void refreshMarketMakerDepth(Depth depth) {
        //clean market making orders
//        "USE refreshMarketMakerDepth from OrderMatchEngine"
        this.fillOrderbook(depth);
    }

    /**
     * When a trade is read check if match any limit order
     *
     * @param trade
     * @return
     */
    public boolean refreshFillMarketTrade(com.lambda.investing.model.market_data.Trade trade) {
        //detect if trade can happen modifying orderbook
        if (trade.getTimestamp() < lastTimestamp) {
            return true;
        }
        lastTimestamp = trade.getTimestamp();

        Verb verbDetection = inferVerbFromTrade(trade);
        if (verbDetection != null) {

            if (!canTradeWithAlgo(trade, verbDetection)) {
                return false;
            }
            //can trade with algo => send the order and modify the orderbook
            OrderRequest orderRequest = generateMarketMakerOrderRequest(trade.getInstrument(), verbDetection,
                    trade.getPrice(), trade.getQuantity());
            //			orderRequest.setOrderType(OrderType.Market);
            orderRequest.setTimestampCreation(trade.getTimestamp());
            //			logger.debug("refreshFillMarketTrade sends {}",orderRequest);
            boolean output = orderRequest(orderRequest, false, true);

            return output;
        }

        //if trade can happen return true
        return false;

    }

    private Verb inferVerbFromTrade(com.lambda.investing.model.market_data.Trade trade) {
        try {
            Double bestBid = this.orderbook.getBestBid();
            Double bestAsk = this.orderbook.getBestOffer();
            Verb output = null;
            if (bestAsk != null && bestBid != null) {
                if (trade.getPrice() <= bestBid) {
                    output = Verb.Sell;//cross the spread
                }
                if (trade.getPrice() >= bestAsk) {
                    output = Verb.Buy;//cross the spread
                }
            }
            return output;
        } catch (Exception e) {
            return null;
        }

    }

    private boolean canTradeWithAlgo(com.lambda.investing.model.market_data.Trade trade, Verb verbOfTheMarketOrder) {
        OrderTree orderTree = null;

        if (verbOfTheMarketOrder.equals(Verb.Buy)) {
            orderTree = this.orderbook.getAsks();
        }

        if (verbOfTheMarketOrder.equals(Verb.Sell)) {
            orderTree = this.orderbook.getBids();
        }

        if (orderTree == null) {
            return false;
        }

        double estimatedFilled = 0.;
        int level = 0;
        for (OrderOrderbook orderOrderbook : orderTree.orderMap.values()) {
            if (orderOrderbook.getAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
                estimatedFilled += orderOrderbook.getQuantity();
                level++;
                continue;
            }

            if (estimatedFilled >= trade.getQuantity()) {
                break;
            }
            //only algorithms not market maker
            if (verbOfTheMarketOrder.equals(Verb.Buy) && trade.getPrice() >= orderOrderbook.getPrice()) {
                return true;
            }

            if (verbOfTheMarketOrder.equals(Verb.Sell) && trade.getPrice() <= orderOrderbook.getPrice()) {
                return true;
            }
        }
        return false;
    }

    private boolean isMMOnlyTrade(Trade trade) {
        return trade.getBuyerAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO) && trade
                .getSellerAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO);
    }

    private void updateOrderResult(ExecutionReport executionReport, Trade trade) {
        executionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
        executionReport.setLastQuantity(trade.getQty());//set Last Quantity in case of trade
        executionReport.setQuantityFill(executionReport.getQuantityFill() + trade.getQty());

        long orderbookTime = orderbook.getTime();
        long timestampTrade = Math.max(orderbookTime, trade.getTimestamp());
        executionReport.setTimestampCreation(timestampTrade);//get current orderbook time
        if (executionReport.getQuantityFill() == executionReport.getQuantity()) {
            executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
        }
        if (executionReport.getPrice() == 0) {
            //for market orders
            executionReport.setPrice(trade.getPrice());
        }


    }

    private void notifyMarketTradeOnMarketMakerOnlyTrade(Trade trade, OrderReport orderReport,
                                                         OrderRequest orderRequest) {
        com.lambda.investing.model.market_data.Trade tradeNotify = new com.lambda.investing.model.market_data.Trade();
        tradeNotify.setInstrument(orderRequest.getInstrument());
        tradeNotify.setAlgorithmInfo(MARKET_MAKER_ALGORITHM_INFO);

        tradeNotify.setPrice(trade.getPrice());
        tradeNotify.setQuantity(trade.getQty());
        tradeNotify.setTimestamp(trade.getTimestamp());

        paperTradingEngineConnector.notifyTrade(tradeNotify);
    }

    private boolean treatOrderReport(OrderReport orderReport, OrderRequest orderRequest,
                                     boolean isMarketMakerDepthUpdate) {
        boolean isNewOrModifyOrder =
                (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Send)) || (orderRequest
                        .getOrderRequestAction().equals(OrderRequestAction.Modify));

        if (isNewOrModifyOrder) {
            ExecutionReport executionReport = executionReportMap
                    .getOrDefault(orderRequest.getClientOrderId(), new ExecutionReport(orderRequest));
            executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
            long orderBookTime = orderbook.getTime();
            long execReportTime = Math.max(orderBookTime, orderRequest.getTimestampCreation());
            executionReport.setTimestampCreation(execReportTime);
            //analyze trades
            List<Trade> tradeList = orderReport.getTrades();

            if (orderReport.isOrderInBook()) {
                //notify active
                notifyExecutionReport(executionReport);
            }

            if (tradeList.size() > 0) {
                //trade happened
                for (Trade trade : tradeList) {

                    int buyId = trade.getBuyer();
                    int sellId = trade.getSeller();

                    if (isMMOnlyTrade(trade)) {

                        if (!isMarketMakerDepthUpdate) {
                            //notify Market trades only if not depth update
                            notifyMarketTradeOnMarketMakerOnlyTrade(trade, orderReport, orderRequest);
                        }
                        continue;
                    }
                    String buyAlgorithm = trade.getBuyerAlgorithmInfo();
                    String sellAlgorithm = trade.getSellerAlgorithmInfo();

                    List<String> clientOrderIdToNotify = new ArrayList<>();

                    if (!buyAlgorithm.equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
                        clientOrderIdToNotify.add(trade.getBuyerClientOrderId());
                    }
                    if (!sellAlgorithm.equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
                        clientOrderIdToNotify.add(trade.getSellerClientOrderId());
                    }

                    for (String clientOrderId : clientOrderIdToNotify) {
                        OrderRequest orderRequest1 = algosCacheOrderRequest.get(clientOrderId);
                        if (orderRequest1 == null) {
                            logger.error("cant find algo orderRequest {} ", clientOrderId);
                            continue;
                        }
                        ExecutionReport executionReport1 = executionReportMap
                                .getOrDefault(clientOrderId, new ExecutionReport(orderRequest1));

                        updateOrderResult(executionReport1, trade);
                        //update my map
                        executionReportMap.put(clientOrderId, executionReport1);

                        boolean isExecuted = (
                                executionReport1.getExecutionReportStatus() == (ExecutionReportStatus.PartialFilled)
                                        || executionReport1.getExecutionReportStatus()
                                        == (ExecutionReportStatus.CompletellyFilled));

                        if (isExecuted) {
                            //// fix error wrong timestamp!!
                            executionReport1.setTimestampCreation(execReportTime);
                            //cancels -> deletion are already notified
                            //new orders -> active are already notified
                            notifyExecutionReport(executionReport1);
                            //notify position updates
                            //						String refdataId = instrumentManager.getIdFromInstrument(isin);
                            //						notifyPosition(orderFromMap.getClientId(), refdataId, isin, quantity, this.bookId);

                        }

                    }
                }
            }
        }
        return true;
    }

    public void notifyExecutionReportReject(OrderRequest orderRequest, String reason) {
        ExecutionReport executionReport1 = new ExecutionReport(orderRequest);
        executionReport1.setRejectReason(reason);
        executionReport1.setExecutionReportStatus(ExecutionReportStatus.Rejected);
        if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {
            executionReport1.setExecutionReportStatus(ExecutionReportStatus.CancelRejected);
        }
        notifyExecutionReport(executionReport1);
    }

    protected void notifyExecutionReport(ExecutionReport executionReport) {
        if (!executionReport.getAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
            paperTradingEngineConnector.notifyExecutionReport(executionReport);
        }

        boolean isTrade = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)
                || executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled);

        if (isTrade) {
            //notify trade
            com.lambda.investing.model.market_data.Trade trade = new com.lambda.investing.model.market_data.Trade(
                    executionReport);
            paperTradingEngineConnector.notifyTrade(trade);

        }
    }

    protected ExecutionReport generateRejection(OrderRequest orderRequest, String reason) {
        ExecutionReport executionReport = executionReportMap
                .getOrDefault(orderRequest.getClientOrderId(), new ExecutionReport(orderRequest));

        long orderbookTime = orderbook.getTime();
        long timestampTrade = Math.max(orderbookTime, orderRequest.getTimestampCreation());
        executionReport.setTimestampCreation(timestampTrade);
        executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
        if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {
            executionReport.setExecutionReportStatus(ExecutionReportStatus.CancelRejected);
        }
        executionReport.setRejectReason(reason);
        return executionReport;
    }

    public boolean treatCanceledOrderReport(boolean isCanceled, OrderRequest orderRequest) {
        boolean isCancelOrder = (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Cancel));

        if (isCancelOrder) {
            ExecutionReport executionReport = executionReportMap
                    .getOrDefault(orderRequest.getClientOrderId(), new ExecutionReport(orderRequest));

            long orderbookTime = orderbook.getTime();
            long timestampTrade = Math.max(orderbookTime, orderRequest.getTimestampCreation());
            executionReport.setTimestampCreation(timestampTrade);
            if (isCanceled) {
                executionReport.setExecutionReportStatus(ExecutionReportStatus.Cancelled);
                //update the map
                executionReportMap.put(orderRequest.getClientOrderId(), executionReport);
            } else {
                String reason = String.format(NOT_FOUND_REJECT_REASON_FORMAT, orderRequest.getOrigClientOrderId(),
                        orderRequest.getInstrument());
                executionReport = generateRejection(orderRequest, reason);
                executionReportMap.put(orderRequest.getClientOrderId(), executionReport);
            }

            notifyExecutionReport(executionReport);

        }

        return true;
    }

    /**
     * Default method to orderRequest with notification of depth change
     *
     * @param orderRequest
     * @return
     */
    public boolean orderRequest(OrderRequest orderRequest) {
        return orderRequest(orderRequest, false, false);
    }

    /***
     *
     * @param orderRequest
     * @param asyncNotify if false , depth will not be notified=> for depth update in market maker algorithm
     * @return
     */
    public boolean orderRequest(OrderRequest orderRequest, boolean asyncNotify, boolean fromTradeFill) {
        synchronized (lockOrderRequest) {
            OrderReport orderReport = null;
            boolean output = false;
            boolean algoTrades = false;
            boolean isMarketMaker = orderRequest.getAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO);
            String beforeOrderbook = "";
            String orderRequestStr = "";
            if (isMarketMaker) {
                marketMakerActiveOrders.put(orderRequest.getClientOrderId(), orderRequest);
                marketMakerCacheOrderRequest.put(orderRequest.getClientOrderId(), orderRequest);
            } else {
                //			beforeOrderbook = orderbook.toString();
                //			orderRequestStr = orderRequest.toString();
                if (algosCacheOrderRequest.containsKey(orderRequest.getClientOrderId())) {
                    logger.warn("OrderRequest {} already processed {}-> return ", orderRequest.getClientOrderId(),
                            orderRequest);
                    return true;
                }
                algosCacheOrderRequest.put(orderRequest.getClientOrderId(), orderRequest);
            }
            if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Send)) {
                //new order
                OrderOrderbook orderOrderbook = createOrderOrderbook(orderRequest);
                try {
                    orderReport = orderbook.processOrder(orderOrderbook, verbose, fromTradeFill);
                    if (!isMarketMaker && orderReport.getTrades().size() > 0) {
                        algoTrades = true;
                    }
                    output = treatOrderReport(orderReport, orderRequest, asyncNotify);
                } catch (LambdaTradingException e) {
                    if (isMarketMaker) {
                        return false;
                    }
                    ExecutionReport executionReportRejected = generateRejection(orderRequest, e.getMessage());
                    notifyExecutionReport(executionReportRejected);
                }

            } else if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Modify)) {
                //modifyOrder
                int originalClientOrderInt = clientOrderIdToOrdId.getOrDefault(orderRequest.getOrigClientOrderId(), -1);
                if (originalClientOrderInt >= 0) {
                    int newOrderId = generateOrderId(orderRequest.getClientOrderId());
                    OrderOrderbook orderOrderbook = createOrderOrderbook(orderRequest);
                    //				orderReport = orderbook.modifyOrder(orderRequest.getOrigClientOrderId(), newOrderId, orderOrderbook);
                    OrderOrderbook orderOrderbook1 = clientOrderIdToOrderOrderbook.get(orderRequest.getOrigClientOrderId());
                    try {
                        orderReport = orderbook
                                .modifyOrder(orderOrderbook1.getNextOrderOrderId(), newOrderId, orderOrderbook);
                        if (!isMarketMaker && orderReport.getTrades().size() > 0) {
                            algoTrades = true;
                        }
                        output = treatOrderReport(orderReport, orderRequest, asyncNotify);
                    } catch (LambdaTradingException e) {
                        if (isMarketMaker) {
                            return false;
                        }
                        ExecutionReport executionReportRejected = generateRejection(orderRequest, e.getMessage());
                        notifyExecutionReport(executionReportRejected);
                    }

                } else {
                    //notify rejection of modify not found
                    String reason = String.format(NOT_FOUND_REJECT_REASON_FORMAT, orderRequest.getOrigClientOrderId(),
                            orderRequest.getInstrument());
                    ExecutionReport executionReportFailedModify = generateRejection(orderRequest, reason);
                    notifyExecutionReport(executionReportFailedModify);
                    output = false;
                }

            } else if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {
                //cancel order
                int originalClientOrderInt = clientOrderIdToOrdId.getOrDefault(orderRequest.getOrigClientOrderId(), -1);
                OrderRequest prevOrder = algosCacheOrderRequest.getOrDefault(orderRequest.getOrigClientOrderId(),
                        marketMakerCacheOrderRequest.get(orderRequest.getOrigClientOrderId()));
                if (originalClientOrderInt >= 0 && prevOrder != null) {
                    String side = getSide(prevOrder.getVerb());
                    OrderOrderbook orderOrderbook1 = clientOrderIdToOrderOrderbook.get(orderRequest.getOrigClientOrderId());
                    output = orderbook
                            .cancelOrder(side, orderOrderbook1.getNextOrderOrderId(), orderRequest.getTimestampCreation());
                    output = treatCanceledOrderReport(output, orderRequest);
                } else {
                    //notify rejection of modify not found
                    String reason = String.format(NOT_FOUND_REJECT_REASON_FORMAT, orderRequest.getOrigClientOrderId(),
                            orderRequest.getInstrument());
                    ExecutionReport executionReportFailedCancel = generateRejection(orderRequest, reason);
                    notifyExecutionReport(executionReportFailedCancel);
                    output = false;
                }

            } else {
                logger.error("{} not recognized RequestAction {}", orderRequest.getClientOrderId(),
                        orderRequest.getOrderRequestAction());
                output = false;
            }

            if (algoTrades) {
                logger.debug("before \n\n\n{}", beforeOrderbook);
                //			logger.info("ORDER -> {}",orderRequestStr);
                //			logger.info("actual \n\n\n{}",this.orderbook);
            }

            if (!isMarketMaker && NOTIFY_DEPTH_ONLY_ON_MM) {
                //if NOTIFY_DEPTH_ONLY_ON_MM just notifies only on depth updates
                return output;
            }
            //just notify if boolean is true
            notifyDepth(Instrument.getInstrument(orderRequest.getInstrument()), asyncNotify);
            return output;
        }
    }

    private void notifyDepth(Instrument instrument) {
        notifyDepth(instrument, false);
    }

    private void notifyDepth(Instrument instrument, boolean async) {
        boolean notifyIt = true;
        synchronized (orderbook) {

            Depth lastOrderbookDepth = orderbook.getOrderbookDepth(instrument);
            if (async) {
                asyncNotification.put(instrument.getPrimaryKey(), lastOrderbookDepth);
            } else {

                //check depth
                if (lastOrderbookDepth.isDepthFilled()) {
                    if (lastOrderbookDepth.getBestAsk() != orderbook.getAsks().minPrice()) {
                        //				not same ask generated!
                        logger.warn("ask is not the same... no possible!");
                        notifyIt = false;
                    }
                    if (lastOrderbookDepth.getBestBid() != orderbook.getBids().maxPrice()) {
                        logger.warn("bid is not the same... no possible!");
                        notifyIt = false;
                    }
                    if (lastOrderbookDepth.getBestBid() > lastOrderbookDepth.getBestAsk()) {
                        //					logger.warn("Wrong depth makes no sense to notify");
                        //					notifyDepth(instrument,false);
                        notifyIt = false;
                    }
                } else {
                    logger.debug("");
                }
                if (notifyIt) {
                    this.paperTradingEngineConnector.notifyDepth(lastOrderbookDepth);
                }
            }
        }

    }

    //to be called after market making new depth clean-depth
    private void notifyDepthAsync(String instrumentPk) {
        Depth lastSnapshotToNotify = asyncNotification.get(instrumentPk);
        if ((lastSnapshotToNotify == null) || (lastSnapshotToNotify.getBids().length == 0
                || lastSnapshotToNotify.getAsks().length == 0)) {
            //something is wrong .... not all levels
            return;
        }
        if (lastSnapshotToNotify.getBestBid() > lastSnapshotToNotify.getBestAsk()) {
            //			logger.warn("Wrong depth makes no sense to notify");
            return;
        }

        //print depth status
        //		logger.info(lastSnapshotToNotify.prettyPrint());

        this.paperTradingEngineConnector.notifyDepth(lastSnapshotToNotify);
        asyncNotification.remove(instrumentPk);
    }

}
