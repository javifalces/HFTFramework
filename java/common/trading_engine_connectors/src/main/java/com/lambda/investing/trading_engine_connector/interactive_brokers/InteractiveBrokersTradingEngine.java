package com.lambda.investing.trading_engine_connector.interactive_brokers;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ib.client.*;
import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.interactive_brokers.ContractFactory;
import com.lambda.investing.interactive_brokers.InteractiveBrokersBrokerConnector;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.model.trading.OrderType;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import com.lambda.investing.trading_engine_connector.interactive_brokers.EWrapperListener;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.trading_engine_connector.AbstractBrokerTradingEngine;
import com.lambda.investing.trading_engine_connector.TradingEngineConfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InteractiveBrokersTradingEngine extends AbstractBrokerTradingEngine {
    //create a list of error codes that we want to ignore in one line
    //202 is OrderCanceled
    //2104 Market data farm connection is OK:uscrypto
    //399 sent as oddlot
    private static List<Integer> ERROR_CODES_IGNORE = Arrays.asList(2104, 202, 399);

    protected static final double FOREX_LOT_SIZE = 1e5;
    protected Logger logger = LogManager.getLogger(InteractiveBrokersTradingEngine.class);

    private InteractiveBrokersBrokerConnector interactiveBrokersBrokerConnector;
    private EWrapperListener eWrapperListener;
    private EClientSocket clientSocket;
    private AtomicInteger nextOrderId = new AtomicInteger(-1);

    private Map<Integer, Contract> contractMap = new ConcurrentHashMap<>();
    private Map<Integer, OrderRequest> orderIdToOrderRequest = new ConcurrentHashMap<>();
    private BiMap<Integer, String> orderIdToClOrdId = HashBiMap.create();
    private Map<String, ExecutionReport> lastExecutionReport = new ConcurrentHashMap<>();


    public InteractiveBrokersTradingEngine(ConnectorConfiguration orderRequestConnectorConfiguration,
                                           ConnectorProvider orderRequestConnectorProvider,
                                           ConnectorConfiguration executionReportConnectorConfiguration,
                                           ConnectorPublisher executionReportConnectorPublisher,
                                           InteractiveBrokersBrokerConnector interactiveBrokersBrokerConnector) {

        super(orderRequestConnectorConfiguration, orderRequestConnectorProvider, executionReportConnectorConfiguration, executionReportConnectorPublisher);
        this.interactiveBrokersBrokerConnector = interactiveBrokersBrokerConnector;

    }

    @Override
    public void start() {
        super.start();
        this.interactiveBrokersBrokerConnector.init();
        this.eWrapperListener = new EWrapperListener(this);
        this.interactiveBrokersBrokerConnector.register(this.eWrapperListener);
        this.clientSocket = this.interactiveBrokersBrokerConnector.getClientSocket();
    }

    private int getNextOrderId() {
        if (nextOrderId.get() < 0) {
            nextOrderId.set(this.interactiveBrokersBrokerConnector.firstOrderId);
        }
        return nextOrderId.incrementAndGet();
    }

    @Override
    public boolean orderRequest(OrderRequest orderRequest) {
        switch (orderRequest.getOrderRequestAction()) {
            case Send:
            case Modify:
                return sendOrder(orderRequest);
            case Cancel:
                return cancelOrder(orderRequest);
            default:
                logger.error("OrderRequestAction not supported: " + orderRequest.getOrderRequestAction());
                return false;
        }
    }

    private boolean cancelOrder(OrderRequest orderRequest) {

        //send a cancel order using the IB API
        if (!orderIdToClOrdId.inverse().containsKey(orderRequest.getOrigClientOrderId())) {
            logger.error("Order {} not found to cancel it", orderRequest.getOrigClientOrderId());
            ExecutionReport executionReport = this.createRejectionExecutionReport(orderRequest, Configuration.formatLog("Order {} not found to cancel it", orderRequest.getOrigClientOrderId()));
            executionReport.setExecutionReportStatus(ExecutionReportStatus.CancelRejected);
            notifyExecutionReportById(executionReport);
            return false;
        }

        int orderId = orderIdToClOrdId.inverse().get(orderRequest.getOrigClientOrderId());
        OrderRequest orderRequestSent = orderIdToOrderRequest.get(orderId);
        if (orderRequestSent == null) {
            logger.error("Order {} not found", orderRequest.getOrigClientOrderId());
            return false;
        }
        if (orderRequestSent.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {
            logger.error("Order {} was already cancelled", orderRequest.getOrigClientOrderId());
            return false;
        }
        orderIdToOrderRequest.put(orderId, orderRequest);
        orderIdToClOrdId.put(orderId, orderRequest.getClientOrderId());
        this.clientSocket.cancelOrder(orderId, "");
        return true;
    }

    private boolean sendOrder(OrderRequest orderRequest) {
        //send a limit order using the IB API
        Instrument instrument = Instrument.getInstrument(orderRequest.getInstrument());
        Contract contract = ContractFactory.createContract(instrument);
        int orderId = -1;

        if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Modify)) {
            if (!orderIdToClOrdId.inverse().containsKey(orderRequest.getOrigClientOrderId())) {
                logger.error("Order {} not found to modify it", orderRequest.getOrigClientOrderId());
                ExecutionReport executionReport = this.createRejectionExecutionReport(orderRequest, Configuration.formatLog("Order {} not found to modify it", orderRequest.getOrigClientOrderId()));
                notifyExecutionReportById(executionReport);
                return false;
            }

            orderId = orderIdToClOrdId.inverse().get(orderRequest.getOrigClientOrderId());
            orderIdToClOrdId.put(orderId, orderRequest.getClientOrderId());//add the new one!
        } else {
            orderId = getNextOrderId();
            orderIdToClOrdId.put(orderId, orderRequest.getClientOrderId());
            contractMap.put(orderId, contract);
        }


        Order order = new Order();
        order.orderId(orderId);
        Types.Action action = Types.Action.get(orderRequest.getVerb().name().toString().toUpperCase());
        order.action(action);
        //Quantity can be modified only for new orders

        Decimal quantity = Decimal.get(orderRequest.getQuantity());
        boolean isCashQty = instrument.isFX();
        if (isCashQty) {
            double cashQty = orderRequest.getQuantity();
            if (instrument.isFX()) {
                cashQty = orderRequest.getQuantity() * FOREX_LOT_SIZE; //0.01 lot = 1000 units  1.00 lot = 100000 units
            }
            order.cashQty(cashQty);
        } else {
            order.totalQuantity(quantity);
        }


        OrderType orderType = orderRequest.getOrderType();
        order.lmtPrice(orderRequest.getPrice());
        order.tif(Types.TimeInForce.DAY);
        switch (orderType) {
            case Limit:
                order.orderType("LMT");
                order.tif(Types.TimeInForce.DAY);
                if (instrument.isCrypto()) {
                    order.tif(Types.TimeInForce.Minutes);
                }
                break;
            case Market:
                order.orderType("MKT");
                break;
            case Stop:
                order.orderType("STP");
                order.tif(Types.TimeInForce.DAY);
                break;
            case InmediateOrCancel:
                order.orderType("LMT");
                order.tif(Types.TimeInForce.IOC);
                break;
            case Minutes:
                order.orderType("LMT");
                order.tif(Types.TimeInForce.Minutes);
                break;
            default:
                logger.error("Order type not supported: " + orderType);
                return false;
        }
        orderIdToOrderRequest.put(orderId, orderRequest);
        this.clientSocket.placeOrder(orderId, contract, order);
        return true;
    }


    /**
     * Feeds in completed orders.
     *
     * @param contract   the order's Contract.
     * @param order      the completed Order.
     * @param orderState the order's OrderState
     */
    public void completedOrder(Contract contract, Order order, OrderState orderState) {
        //create ExecutionReport and send it to the ExecutionReportConnector
        OrderRequest orderRequestSent = orderIdToOrderRequest.get(order.orderId());
        ExecutionReport executionReport = lastExecutionReport.getOrDefault(orderRequestSent.getClientOrderId(),
                new ExecutionReport(orderRequestSent));
        lastExecutionReport.put(orderRequestSent.getClientOrderId(), executionReport);
        Instrument instrument = Instrument.getInstrument(executionReport.getInstrument());

        boolean statusRecognized = false;
        switch (orderState.status()) {
            case Filled:
                double qtyFilled = order.totalQuantity().value().doubleValue();
                if (instrument.isFX()) {
                    //adapt all quantities to lots
                    qtyFilled = qtyFilled / FOREX_LOT_SIZE;
                }
                double lastQty = qtyFilled - executionReport.getQuantityFill();
                executionReport.setLastQuantity(lastQty);
                executionReport.setQuantityFill(qtyFilled);
                ExecutionReportStatus status = ExecutionReportStatus.PartialFilled;
                if (qtyFilled == orderRequestSent.getQuantity()) {
                    status = ExecutionReportStatus.CompletellyFilled;
                }
                executionReport.setExecutionReportStatus(status);
                break;

            case Cancelled:
                executionReport.setExecutionReportStatus(ExecutionReportStatus.Cancelled);
                statusRecognized = true;
                break;
            case Submitted:
                executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
                statusRecognized = true;
                break;
            default:
                logger.warn("completedOrder {} : Order status not supported: {} \n{}\n{}", order.orderId(), orderState.status(), order, orderState);
                return;
        }

        if (!orderState.status().isActive()) {
            orderIdToOrderRequest.remove(order.orderId());
        }

        if (statusRecognized) {
            notifyExecutionReportById(executionReport);
        }


    }

    /**
     * This method is called when the order status changes
     * https://interactivebrokers.github.io/tws-api/interfaceIBApi_1_1EWrapper.html#a27ec36f07dff982f50968c8a8887d676
     *
     * @param orderId       the order's client id.
     * @param status        the current status of the order. Possible values: Submitted PendingSubmit - indicates that you have transmitted the order, but have not yet received confirmation that it has been accepted by the order destination. PendingCancel - indicates that you have sent a request to cancel the order but have not yet received cancel confirmation from the order destination. At this point, your order is not confirmed canceled. It is not guaranteed that the cancellation will be successful. PreSubmitted - indicates that a simulated order type has been accepted by the IB system and that this order has yet to be elected. The order is held in the IB system until the election criteria are met. At that time the order is transmitted to the order destination as specified . Submitted - indicates that your order has been accepted by the system. ApiCancelled - after an order has been submitted and before it has been acknowledged, an API client client can request its cancelation, producing this state. Cancelled - indicates that the balance of your order has been confirmed canceled by the IB system. This could occur unexpectedly when IB or the destination has rejected your order. Filled - indicates that the order has been completely filled. Market orders executions will not always trigger a Filled status. Inactive - indicates that the order was received by the system but is no longer active because it was rejected or canceled.
     * @param filled        number of filled positions.
     * @param remaining     the remnant positions.
     * @param avgFillPrice  average filling price.
     * @param permId        the order's permId used by the TWS to identify orders.
     * @param parentId      parent's id. Used for bracket and auto trailing stop orders.
     * @param lastFillPrice price at which the last positions were filled.
     * @param clientId      API client which submitted the order.
     * @param whyHeld       this field is used to identify an order held when TWS is trying to locate shares for a short sell. The value used to indicate this is 'locate'.
     * @param mktCapPrice   If an order has been capped, this indicates the current capped price. Requires TWS 967+ and API v973.04+. Python API specifically requires API v973.06+.
     */
    public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
        //New order/Replace
        boolean isLive = status.equals(OrderStatus.Submitted.toString());
        OrderRequest request = orderIdToOrderRequest.get(orderId);
        Instrument instrument = Instrument.getInstrument(request.getInstrument());
        if (isLive) {
            if (lastExecutionReport.containsKey(request.getClientOrderId())) {
                //already notified
                return;
            }
            ExecutionReport executionReport = new ExecutionReport(request);
            executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
            lastExecutionReport.put(request.getClientOrderId(), executionReport);
            notifyExecutionReportById(executionReport);
            return;
        }

        //Cancelled
        boolean isCancelled = status.equals(OrderStatus.Cancelled.toString());
        if (isCancelled) {
            if (request.getOrderRequestAction() != OrderRequestAction.Cancel) {
                logger.error("Order {} was cancelled but it was not a cancel request -> sent cancelled", orderId);
            }
            ExecutionReport executionReport = lastExecutionReport.getOrDefault(request.getClientOrderId(), new ExecutionReport(request));
            executionReport.setExecutionReportStatus(ExecutionReportStatus.Cancelled);
            lastExecutionReport.put(request.getClientOrderId(), executionReport);
            notifyExecutionReportById(executionReport);
            return;
        }


        //Trade

        boolean isFilled = status.equals(OrderStatus.Filled.toString());
        if (isFilled) {

            if (!lastExecutionReport.containsKey(request.getClientOrderId())) {
                //create active report
                ExecutionReport executionReport = new ExecutionReport(request);
                executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
                executionReport.setPrice(lastFillPrice);
                lastExecutionReport.put(request.getClientOrderId(), executionReport);
                notifyExecutionReportById(executionReport);
            }

            ExecutionReport executionReport = lastExecutionReport.getOrDefault(request.getClientOrderId(), new ExecutionReport(request));
            double lastFillQty = filled.value().doubleValue();
            if (instrument.isFX()) {
                //adapt all quantities to lots
                lastFillQty = lastFillQty / FOREX_LOT_SIZE;
            }
            executionReport.setLastQuantity(lastFillQty);
            executionReport.setQuantityFill(executionReport.getQuantityFill() + lastFillQty);
            executionReport.setPrice(lastFillPrice);

            double remainingCalc = executionReport.getQuantity() - executionReport.getQuantityFill();
            double brokerRemaining = remaining.value().doubleValue();
            if (remainingCalc != brokerRemaining) {
                logger.warn("Remaining quantity mismatch: calculated {} vs broker {}", remainingCalc, brokerRemaining);
            }
            executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
            if (remainingCalc > 0) {
                executionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
            }
            notifyExecutionReportById(executionReport);
            return;
        }

    }

    public void completedOrdersEnd() {
        //do i need it?
        return;
    }

    public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
        if (id == -1 || ERROR_CODES_IGNORE.contains(errorCode)) {
            return;
        }
        OrderRequest request = orderIdToOrderRequest.get(id);

        logger.error("Error id: " + id + " code: " + errorCode + " msg: " + errorMsg);

        boolean orderIsStillActive = request.getOrderRequestAction().equals(OrderRequestAction.Modify) || request.getOrderRequestAction().equals(OrderRequestAction.Cancel);
        ExecutionReport executionReport = null;
        if (orderIsStillActive) {
            //replace or cancel is rejected
            executionReport = new ExecutionReport(request);
        } else {
            //new request is rejected
            executionReport = lastExecutionReport.getOrDefault(request.getClientOrderId(),
                    new ExecutionReport(request));
            orderIdToOrderRequest.remove(request.getClientOrderId());
        }


        executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
        executionReport.setRejectReason(Configuration.formatLog("{}: {}", errorCode, errorMsg));
        notifyExecutionReportById(executionReport);
    }

    @Override
    public void setDemoTrading() {
    }


}
