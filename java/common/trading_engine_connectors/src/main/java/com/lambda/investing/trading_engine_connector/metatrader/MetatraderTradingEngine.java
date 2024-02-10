package com.lambda.investing.trading_engine_connector.metatrader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqPuller;
import com.lambda.investing.connector.zero_mq.ZeroMqPusher;
import com.lambda.investing.metatrader.MetatraderZeroBrokerConnector;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.portfolio.Portfolio;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.AbstractBrokerTradingEngine;
import com.lambda.investing.trading_engine_connector.metatrader.models.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static com.lambda.investing.model.portfolio.Portfolio.REQUESTED_POSITION_INFO;

public class MetatraderTradingEngine extends AbstractBrokerTradingEngine {
    private class RequestPortfolio implements Runnable {

        private long msRequest;

        public RequestPortfolio(long msRequest) {
            this.msRequest = msRequest;
        }

        @Override
        public void run() {
            while (true) {
                getPositionMT();

                try {
                    Thread.sleep(this.msRequest);
                } catch (InterruptedException e) {
                    logger.error("error sleeping RequestPortfolio MetatraderTradingEngine");
                    throw new RuntimeException(e);
                }

            }
        }
    }

    private static long REQUEST_PORTFOLIO_MS = 15000;
    protected Logger logger = LogManager.getLogger(MetatraderTradingEngine.class);
    public static List<String> CROSS_SPREAD_SYNONIMS = Arrays.asList("cross", "market");
    public static Gson GSON_STRING = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.FINAL)
            .serializeSpecialFloatingPointValues().create();
    private static int THREADS_PULLING = 0;
    private static int THREADS_PUSHING = 0;
    private MetatraderZeroBrokerConnector metatraderZeroBrokerConnector;

    private ZeroMqConfiguration zeroMqConfigurationPull;
    private ZeroMqConfiguration zeroMqConfigurationPush;
    private ZeroMqPusher zeroMqPusher;
    private ZeroMqPuller zeroMqPuller;
    private Map<String, OrderRequest> pendingExecutionReport;
    private Map<String, OrderRequest> origClOrdIdPendingExecutionReport;
    private Map<String, OrderRequest> activeOrders;
    private Map<String, ExecutionReport> executionReportMap;
    private ConnectorConfiguration zeroMqConfigurationER;
    //	private List<String> CfTradesNotified;
    public boolean nettingByEngine = false;//done in mt5 with Hedge_to_net
    protected static int DEFAULT_QUEUE_SIZE = 250;
    protected Queue<String> CfTradesNotified;
    protected Queue<String> ordersProcessed;

    private Map<String, Double> lastPosition;
    private Date lastPositionUpdate = new Date(0);
    protected final Object lockLatch = new Object();
    protected CountDownLatch lastPositionUpdateCountDown = new CountDownLatch(1);
    protected Map<String, OrderRequest> instrumentTolastBuyOrderSent;
    protected Map<String, OrderRequest> instrumentTolastSellOrderSent;

    public void setNettingByEngine(boolean nettingByEngine) {
        this.nettingByEngine = nettingByEngine;
    }

    public double getPosition(String instrumentPk) {
        return lastPosition.getOrDefault(instrumentPk, 0.);
    }

    private String broker;

    public void setBroker(String broker) {
        this.broker = broker;
    }


    public MetatraderTradingEngine(ConnectorConfiguration orderRequestConnectorConfiguration,
                                   ConnectorProvider orderRequestConnectorProvider,

                                   ConnectorConfiguration executionReportConnectorConfiguration,
                                   ConnectorPublisher executionReportConnectorPublisher,
                                   MetatraderZeroBrokerConnector metatraderZeroBrokerConnector) {

        super(orderRequestConnectorConfiguration, orderRequestConnectorProvider, executionReportConnectorConfiguration,
                executionReportConnectorPublisher);
        lastPosition = new HashMap<>();
        instrumentTolastBuyOrderSent = new HashMap<>();
        instrumentTolastSellOrderSent = new HashMap<>();

        CfTradesNotified = EvictingQueue.create(DEFAULT_QUEUE_SIZE);
        ordersProcessed = EvictingQueue.create(DEFAULT_QUEUE_SIZE);

        zeroMqConfigurationER = executionReportConnectorConfiguration;
        pendingExecutionReport = new HashMap<>();
        origClOrdIdPendingExecutionReport = new HashMap<>();
        executionReportMap = new HashMap<>();
        activeOrders = new HashMap<>();
        this.metatraderZeroBrokerConnector = metatraderZeroBrokerConnector;

        // WARNING HERE we cross PORTS fto maintain metatrader configuration
        //		//pull with metatrader push port
        zeroMqConfigurationPull = new ZeroMqConfiguration();
        zeroMqConfigurationPull.setPort(this.metatraderZeroBrokerConnector.getPortPush());
        //		zeroMqConfigurationPull.setPort(this.metatraderZeroBrokerConnector.getPortPull());//dont know why is the same
        zeroMqConfigurationPull.setHost("localhost");

        this.zeroMqPuller = ZeroMqPuller.getInstance(zeroMqConfigurationPull, THREADS_PULLING);
        this.zeroMqPuller.setParsedObjects(false);

        //push  with metatrader pull port
        zeroMqConfigurationPush = new ZeroMqConfiguration();
        zeroMqConfigurationPush.setPort(this.metatraderZeroBrokerConnector.getPortPull());
        zeroMqConfigurationPush.setHost("localhost");

        this.zeroMqPusher = new ZeroMqPusher("metatrader_push", THREADS_PUSHING);

        //portfolio file not on the broker side if we want to save it on file
        portfolio = Portfolio
                .getPortfolio(Configuration.OUTPUT_PATH + File.separator + "metatrader_broker_position.json");

        //start in background thread
        Thread portfolioSynchronizeThread = new Thread(new RequestPortfolio(REQUEST_PORTFOLIO_MS), "RequestPortfolioThread");
        portfolioSynchronizeThread.setPriority(Thread.MIN_PRIORITY);
        portfolioSynchronizeThread.start();

    }

    @Override
    public void start() {
        super.start();
        this.zeroMqPuller.start();
        this.zeroMqPuller.register(this.zeroMqConfigurationPull, this::onUpdateExecutionReport);
        updateLastPositionWithMT();
    }

    @Override
    public void setDemoTrading() {

    }

    public boolean closeTrade(OrderRequest orderRequest) {
        logger.debug("closeTrade push Metatrader {}", orderRequest);
        MTCommand closeOrder = new MTCommand(orderRequest);

        String ticket = MTCommand.getTicket(orderRequest.getOrigClientOrderId());
        if (ticket == null) {
            logger.warn("ticket not found for clOrdId {} to closeTrade", orderRequest.getOrigClientOrderId());
            return false;
        }
        closeOrder.setTicket(ticket);

        closeOrder.setAction(MtAction.POS_CLOSE);
        String message = closeOrder.formatMessage();
        pendingExecutionReport.put(orderRequest.getClientOrderId(), orderRequest);
        if (orderRequest.getOrigClientOrderId() != null) {
            origClOrdIdPendingExecutionReport.put(orderRequest.getOrigClientOrderId(), orderRequest);
        }
        return zeroMqPusher.publish(zeroMqConfigurationPush, TypeMessage.order_request, "", message);
    }

    private boolean checkNettingSendClose(OrderRequest orderRequest) {
        //check position
        double currentPosition = this.getPosition(orderRequest.getInstrument());
        boolean isExitBuy =
                (currentPosition) > 0 && orderRequest.getVerb().equals(Verb.Sell) && orderRequest.getQuantity() == Math
                        .abs(currentPosition);
        boolean isExitSell =
                (currentPosition) < 0 && orderRequest.getVerb().equals(Verb.Buy) && orderRequest.getQuantity() == Math
                        .abs(currentPosition);
        if (isExitBuy || isExitSell) {
            //is a close
            System.out.println("OrderRequest sent as closePosition detected   " + orderRequest.getClientOrderId());
            OrderRequest entryOrder = null;
            if (isExitBuy) {
                entryOrder = instrumentTolastBuyOrderSent.get(orderRequest.getInstrument());
            }
            if (isExitSell) {
                entryOrder = instrumentTolastSellOrderSent.get(orderRequest.getInstrument());
            }
            if (entryOrder == null) {
                logger.warn("orderRequest of  entry not found for {} in {} side -> set using market", orderRequest.getInstrument(), orderRequest.getVerb());
            } else {
                String clOrdId = entryOrder.getClientOrderId();
                if (clOrdId != null) {
                    orderRequest.setOrigClientOrderId(clOrdId);
                    pendingExecutionReport.put(orderRequest.getClientOrderId(), orderRequest);
                    if (orderRequest.getOrigClientOrderId() != null) {
                        pendingExecutionReport.put(orderRequest.getOrigClientOrderId(), orderRequest);
                    }
                    logger.info("{} detected to close position because of nettingByEngine",
                            orderRequest.getOrigClientOrderId());
                    ordersProcessed.add(orderRequest.getClientOrderId());
                    return closeTrade(orderRequest);
                } else {
                    logger.warn("entry order found but clOrdId is null? {} {} -> set using market {}",
                            orderRequest.getInstrument(), orderRequest.getVerb(), orderRequest);
                }
            }
        }
        return false;
    }

    @Override
    public boolean orderRequest(OrderRequest orderRequest) {
        logger.debug("orderRequest push Metatrader {}", orderRequest);
        if (ordersProcessed.contains(orderRequest.getClientOrderId())) {
            logger.warn("Order already processed {} on {}-> return", orderRequest.getClientOrderId(),
                    orderRequest.getInstrument());
            return false;
        }
        if (nettingByEngine) {
            //check position
            boolean sendClose = checkNettingSendClose(orderRequest);
            if (sendClose) {
                return sendClose;
            }
        }
        boolean isSendOrModify = (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Send));

        boolean freeTextToMarket = orderRequest.getFreeText() != null && CROSS_SPREAD_SYNONIMS
                .contains(orderRequest.getFreeText().toLowerCase());

        if (orderRequest.getOrderType() != null && orderRequest.getOrderType().equals(OrderType.Limit) && isSendOrModify && freeTextToMarket) {
            // check if its market order!!
            logger.info("MT freetext limit order {} to market order freeText:{} ", orderRequest.getClientOrderId(),
                    orderRequest.getFreeText());
            orderRequest.setOrderType(OrderType.Market);
        }

        String message = new MTCommand(orderRequest).formatMessage();
        pendingExecutionReport.put(orderRequest.getClientOrderId(), orderRequest);
        if (orderRequest.getOrigClientOrderId() != null) {
            origClOrdIdPendingExecutionReport.put(orderRequest.getOrigClientOrderId(), orderRequest);
        }
        if (orderRequest.getVerb() != null) {
            if (orderRequest.getVerb().equals(Verb.Buy)) {
                instrumentTolastBuyOrderSent.put(orderRequest.getInstrument(), orderRequest);
            }

            if (orderRequest.getVerb().equals(Verb.Sell)) {
                instrumentTolastSellOrderSent.put(orderRequest.getInstrument(), orderRequest);
            }
        }
        ordersProcessed.offer(orderRequest.getClientOrderId());
        return zeroMqPusher.publish(zeroMqConfigurationPush, TypeMessage.order_request, "", message);

    }

    private void updateLastPositionWithMT() {
        getPositionMT();
    }

    public boolean getPositionMT(boolean synchronous) {
        logger.debug("getPosition push Metatrader");
        String message = String.format("%d", MtAction.GET_POSITIONS.ordinal());
        if (synchronous) {
            synchronized (lockLatch) {
                if (lastPositionUpdateCountDown == null || lastPositionUpdateCountDown.getCount() == 0) {
                    lastPositionUpdateCountDown = new CountDownLatch(1);
                }
            }
        }

        boolean output = zeroMqPusher.publish(zeroMqConfigurationPush, TypeMessage.info, "", message);

        if (synchronous) {
            try {
                lastPositionUpdateCountDown.await();
            } catch (Exception e) {
                ;
            }
        }


        return output;

    }

    public boolean getPositionMT() {
        return getPositionMT(false);
    }

    @Override
    public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
                         TypeMessage typeMessage, String content) {
        //here comes the orderRequest
        logger.debug("onUpdate pull Metatrader  {}", content);
        super.onUpdate(configuration, timestampReceived, typeMessage, content);
    }

    @Override
    public void requestInfo(String info) {
        //algorithm.info
        logger.info("requestInfo: {} ", info);
        if (info.endsWith(REQUESTED_POSITION_INFO)) {
            String message = GSON_STRING.toJson(lastPosition);
            String topic = Configuration.formatLog("{}.{}", REQUESTED_POSITION_INFO, TypeMessage.info.name());
            this.executionReportConnectorPublisher
                    .publish(executionReportConnectorConfiguration, TypeMessage.info, topic, message);
        }
    }

    private String getInstrumentPk(String symbol) {
        String instrumentPK = Configuration.formatLog("{}_{}", symbol.toLowerCase(), broker);
        Instrument instrument = Instrument.getInstrument(instrumentPK);
        if (instrument == null) {
            logger.warn("instrument_pk {} not found on Instrument.getInstrument", instrumentPK);
        }
        return instrumentPK;
    }

    public void onOpenPositions(ConnectorConfiguration configuration, long timestampReceived, String content) {
        logger.debug("onOpenPositions Metatrader  {}", content);

        //{'_action': 'OPEN_POSITIONS', '_positions': {2015962186: {'_magic': 123456, '_symbol': 'EURCHF', '_lots': 0.01000000, '_type': 0, '_open_price': 0.98049000, '_open_time': '2023.03.10 23:54:21', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -0.38000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015962185: {'_magic': 123456, '_symbol': 'EURUSD', '_lots': 0.01000000, '_type': 1, '_open_price': 1.06385000, '_open_time': '2023.03.10 23:54:21', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -0.11000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015962153: {'_magic': 123456, '_symbol': 'EURAUD', '_lots': 0.01000000, '_type': 0, '_open_price': 1.61656000, '_open_time': '2023.03.10 23:51:18', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -0.24000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015962152: {'_magic': 123456, '_symbol': 'EURCHF', '_lots': 0.01000000, '_type': 0, '_open_price': 0.98046000, '_open_time': '2023.03.10 23:51:18', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -0.35000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015962150: {'_magic': 123456, '_symbol': 'EURUSD', '_lots': 0.01000000, '_type': 1, '_open_price': 1.06392000, '_open_time': '2023.03.10 23:51:18', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -0.05000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015962078: {'_magic': 123456, '_symbol': 'EURCHF', '_lots': 0.01000000, '_type': 0, '_open_price': 0.98054000, '_open_time': '2023.03.10 23:43:10', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -0.43000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015961537: {'_magic': 123456, '_symbol': 'EURAUD', '_lots': 0.01000000, '_type': 0, '_open_price': 1.61614000, '_open_time': '2023.03.10 23:08:36', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': 0.02000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015959313: {'_magic': 123456, '_symbol': 'EURNZD', '_lots': 0.01000000, '_type': 1, '_open_price': 1.73640000, '_open_time': '2023.03.10 21:25:56', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': 0.92000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015955887: {'_magic': 123456, '_symbol': 'EURNZD', '_lots': 0.02000000, '_type': 1, '_open_price': 1.73364000, '_open_time': '2023.03.10 19:15:48', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -1.35000000, '_comment': 'MarketFactorInvestingAlgorithm_'}}}
        MtPosition mtPosition = null;
        try {
            mtPosition = GSON_STRING.fromJson(content, MtPosition.class);
        } catch (Exception e) {
            logger.error("error parsing open positions message {}", content, e);
            synchronized (lockLatch) {
                if (lastPositionUpdateCountDown != null) {
                    lastPositionUpdateCountDown.countDown();
                }
            }
            return;
        }
        lastPosition.clear();

        if (mtPosition == null || mtPosition.getLength() == 0) {
            logger.warn("no positions received from MT");
            synchronized (lockLatch) {
                if (lastPositionUpdateCountDown != null) {
                    lastPositionUpdateCountDown.countDown();
                }
            }
            return;
        }


        for (String id : mtPosition.get_positions().keySet()) {
            MtPosition.MtPositionInstrument positionInstrument = mtPosition.get_positions().get(id);

            String symbol = positionInstrument.get_symbol();
            String instrumentPK = getInstrumentPk(symbol);

            double prevPosition = lastPosition.getOrDefault(instrumentPK, 0.0);
            double lots = Double.valueOf(positionInstrument.get_lots());
            int orderType = Integer.valueOf(positionInstrument.get_type());
            if (orderType == MTOrderType.ORDER_TYPE_SELL.ordinal()) {
                lots = -1 * lots;
            }
            double newPosition = prevPosition + lots;
            lastPosition.put(instrumentPK, newPosition);


        }
        if (lastPosition.size() > 0) {
            logger.info("onOpenPositions broker:{} {} instruments positions", broker, lastPosition.size());
            for (String instrumentPK : lastPosition.keySet()) {
                logger.info("{} : {}", instrumentPK, lastPosition.get(instrumentPK));
            }
        }

        synchronized (lockLatch) {
            if (lastPositionUpdateCountDown != null) {
                lastPositionUpdateCountDown.countDown();
            }
        }

    }

    private Pair<Boolean, ExecutionReport> notifyActiveIfSuccess(OrderRequest pendingOrderRequest, MTMessageER mtMessageER) {
        String responseValue = mtMessageER.get_response_value();
        String response = mtMessageER.get_response();
        String magic = mtMessageER.get_magic();

        boolean isSuccess = responseValue != null && responseValue.equalsIgnoreCase("SUCCESS");
        boolean isCloseTrade = isCloseTrade(mtMessageER);
        if (isCloseTrade) {
            isSuccess = true;
        }

        ExecutionReport executionReport = new ExecutionReport(pendingOrderRequest);

        if (CfTradesNotified.contains(pendingOrderRequest.getClientOrderId())) {
            System.err.println("discard notify on close Cf origClOrdId " + pendingOrderRequest.getClientOrderId()
                    + " already processed");
            logger.info("discard notify on close Cf {} already processed trade {} ",
                    pendingOrderRequest.getInstrument(), pendingOrderRequest.getClientOrderId());
            return ImmutablePair.of(false, executionReport);
        }

        executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
        String closePrice = mtMessageER.get_close_price();
        if (closePrice == null) {
            //Position partially close but corresponding deal can not be selected
            double lastPriceSend = pendingOrderRequest.getPrice();
            logger.warn("mtExecutionReport without close_price 1 => get close price from OrderRequest {} {}", pendingOrderRequest.getClientOrderId(), lastPriceSend);
            closePrice = String.valueOf(lastPriceSend);
        }
        executionReport.setPrice(Double.valueOf(closePrice));


//            if (closePrice != null) {
//
//            } else {
//                String messageError = "";
//                if (mtExecutionReport.get_response_value() != null && mtExecutionReport.get_response_value()
//                        .equalsIgnoreCase("ERROR")) {
//                    int firstIndex = content.indexOf("_response") + "_response".length();
//                    int nextIndex = content.substring(firstIndex, content.length()).indexOf("_response") + firstIndex;
//                    messageError = content.substring(firstIndex, nextIndex);
//                    response += " " + messageError;
//                }
//                System.err.println("" + "can't get close price on ER!  " + mtExecutionReport + " " + response);
//                logger.warn("can't get close price on ER! {} -> {}", mtExecutionReport, messageError);
//                if (message != null) {
//                    logger.warn("response:{}", response);
//                }
//            }


        if (!isSuccess) {
            logger.error("ER error {}  order! {} ", response, magic);
            System.err.println("ER error close " + response);
            executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
            executionReport.setRejectReason(response + ":" + mtMessageER.get_response_value());
            notifyExecutionReportById(executionReport);
            return ImmutablePair.of(false, executionReport);
        } else {
            String lastQty = mtMessageER.get_close_lots();
            if (lastQty == null) {
                double qty = pendingOrderRequest.getQuantity();
                logger.warn("mtExecutionReport without close_lots => get lastQty from OrderRequest {} {}", pendingOrderRequest.getClientOrderId(), qty);
                lastQty = String.valueOf(qty);

            }
            double qtyMT = Double.valueOf(lastQty);
            boolean differentQty = Math.abs(qtyMT - pendingOrderRequest.getQuantity()) > 1E-6;
            if (differentQty) {
                logger.warn("{} different qty received {} from send it -> override with send it {}", executionReport.getClientOrderId(), qtyMT, pendingOrderRequest.getQuantity());
                qtyMT = pendingOrderRequest.getQuantity();
            }

            executionReport.setLastQuantity(qtyMT);
            executionReport.setQuantity(qtyMT);
            if (executionReport.getQuantity() != pendingOrderRequest.getQuantity()) {

            }
            updatePosition(executionReport);
            notifyExecutionReportById(executionReport);
            return ImmutablePair.of(true, executionReport);
        }

    }

    private boolean isPartiallyClosePosition(MTMessageER mtMessageER) {
        boolean isPartiallyClosePosition1 = mtMessageER.get_action().equalsIgnoreCase("EXECUTION") && (mtMessageER.get_response() != null
                && mtMessageER.get_response().contains("partially closed"));
        boolean isPartiallyClosePosition2 = mtMessageER.get_action().equalsIgnoreCase("EXECUTION") && (mtMessageER.get_response() != null
                && mtMessageER.get_response().contains("10009"));
        boolean isPartiallyClosePosition = isPartiallyClosePosition1 || isPartiallyClosePosition2;
        return isPartiallyClosePosition;
    }

    private boolean isCloseTrade(MTMessageER mtMessageER) {
        boolean isPartiallyClosePosition = isPartiallyClosePosition(mtMessageER);
        boolean isCloseOrderTrade = mtMessageER.get_action().equalsIgnoreCase("EXECUTION") &&
                mtMessageER.get_magic() != null && mtMessageER.get_close_price() != null
                && mtMessageER.get_close_lots() != null;
        boolean isCloseTrade = isCloseOrderTrade || isPartiallyClosePosition;
        return isCloseTrade;
    }

    private boolean isActiveConfirmation(MTMessageER mtMessageER) {
        boolean output = mtMessageER.get_action().equalsIgnoreCase("EXECUTION")
                && mtMessageER.get_close_price() == null || mtMessageER.get_action()
                .equalsIgnoreCase("ORDER_MODIFY");
        return output;
    }

    private void TreatExecutionReportWithErrors(MTMessageER mtMessageER, String content) {
        //something goes wrong
        logger.warn("something is general wrong {}", content);
        System.out.println("something is general wrong " + content);
        List<String> idsRemove = new ArrayList<>();
        List<String> idsOrigRemove = new ArrayList<>();
        for (OrderRequest orderRequest : pendingExecutionReport.values()) {
            String reason = mtMessageER.get_response() + ":" + mtMessageER.get_response_value();
            ExecutionReport executionReport = createRejectionExecutionReport(orderRequest, reason);
            idsRemove.add(executionReport.getClientOrderId());
            if (executionReport.getOrigClientOrderId() != null) {
                idsOrigRemove.add(executionReport.getOrigClientOrderId());
            }
            this.executionReportConnectorConfiguration = this.zeroMqConfigurationER;//overwrite
            notifyExecutionReportById(executionReport);
        }
        for (String id : idsRemove) {
            pendingExecutionReport.remove(id);
        }

        for (String id : idsOrigRemove) {
            origClOrdIdPendingExecutionReport.remove(id);
        }

    }

    private void treatRejection(MTMessageER mtMessageER) {
        String magic = mtMessageER.get_magic();
        String response = mtMessageER.get_response();
        String responseValue = mtMessageER.get_response_value();

        OrderRequest pendingOrderRequest = pendingExecutionReport.get(magic);
        ExecutionReport executionReport = new ExecutionReport(pendingOrderRequest);
        executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
        pendingExecutionReport.remove(magic);
        boolean isRejectionMessage = response != null || responseValue != null;
        if (isRejectionMessage) {
            String rejectionMessage = Configuration.formatLog("{}:{}", response, responseValue);
            logger.error("ER error {} order! {}", response, magic);
            executionReport.setRejectReason(rejectionMessage);
        }
        notifyExecutionReportById(executionReport);

    }

    private void treatConfirmation(MTMessageER mtMessageER) {
        String magic = mtMessageER.get_magic();
        String response = mtMessageER.get_response();
        String responseValue = mtMessageER.get_response_value();
        String ticket = mtMessageER.get_ticket();

        MTCommand.updateTicketOrder(magic, ticket);
        OrderRequest pendingOrderRequest = pendingExecutionReport.get(magic);
        if (pendingOrderRequest == null) {
            logger.error("pendingOrderRequest is null on magic {} mtExecutionReport:{} ", magic, mtMessageER);
            return;
        }
        ExecutionReport executionReport = new ExecutionReport(pendingOrderRequest);
        executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
        String openPrice = mtMessageER.get_open_price();
        if (openPrice != null) {
            executionReport.setPrice(Double.valueOf(openPrice));
            pendingOrderRequest.setPrice(Double.valueOf(openPrice));
        } else {
            if (!pendingOrderRequest.getOrderRequestAction().equals(OrderRequestAction.Modify)) {
                logger.warn("received ER without a price {}", executionReport);
            }
        }
        pendingExecutionReport.remove(magic);
        if (response != null) {
            logger.error("ER error {}  order! {} {} ", response, magic);
            executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
            executionReport.setRejectReason(response);
        }

        activeOrders.put(pendingOrderRequest.getClientOrderId(), pendingOrderRequest);
        notifyExecutionReportById(executionReport);

    }

    private void treatDeleted(MTMessageER mtMessageER) {
        String magic = mtMessageER.get_magic();
        String response = mtMessageER.get_response();
        String responseValue = mtMessageER.get_response_value();
        String ticket = mtMessageER.get_ticket();

        OrderRequest activeOrderRequest = activeOrders.get(magic);
        if (activeOrderRequest == null) {
            activeOrderRequest = pendingExecutionReport.get(magic);
        }
        ExecutionReport executionReportDeleted = null;
        try {
            executionReportDeleted = new ExecutionReport(activeOrderRequest);
        } catch (Exception e) {
            logger.error("cant created deleted ER on {} ", activeOrderRequest);
            return;
        }
        executionReportDeleted.setExecutionReportStatus(ExecutionReportStatus.Cancelled);

        //clean maps
        activeOrders.remove(magic);
        MTCommand.cleanTicketOrder(magic);
        executionReportMap.remove(magic);
        //notify
        notifyExecutionReportById(executionReportDeleted);
    }

    private void treatTrade(MTMessageER mtMessageER) {
        String ticket = mtMessageER.get_ticket();
        String clOrdId = MTCommand.getClientOrderId(ticket);
        if (clOrdId == null) {
            //after closing position + trade
            logger.warn("{} not found clOrdId -> not processed {}", ticket, mtMessageER);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return;
        }
        OrderRequest activeOrderRequest = activeOrders.get(clOrdId);
        if (activeOrderRequest == null) {
            logger.error("activeOrderRequest not found for clOrdId {}", clOrdId);
            return;
        }

        ExecutionReport executionReportExecuted = executionReportMap
                .getOrDefault(clOrdId, new ExecutionReport(activeOrderRequest));

        //			String closePrice = mtExecutionReport.get_close_price();
        //			if (closePrice != null) {
        //				executionReportExecuted.setPrice(Double.valueOf(closePrice));
        //			} else {
        //				logger.warn("received ER without a price {}", executionReportExecuted);
        //			}

        executionReportExecuted.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
        double qtyFilled = Double.valueOf(mtMessageER.get_last_qty());
        //			double qtyTotal = executionReportExecuted.getQuantity();//should be == Qty

        executionReportExecuted.setLastQuantity(qtyFilled);
        double qtyFill = qtyFilled + executionReportExecuted.getQuantityFill();
        executionReportExecuted.setQuantityFill(qtyFill);

        boolean isCF = executionReportExecuted.getQuantity() == executionReportExecuted.getQuantityFill();
        if (isCF) {
            executionReportExecuted.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
        }

        //update active orders map
        if (!isCF) {
            executionReportMap.put(clOrdId, executionReportExecuted);
        } else {

            if (CfTradesNotified.contains(executionReportExecuted.getClientOrderId())) {
                logger.info("discard notify Cf {} already processed trade {} ",
                        executionReportExecuted.getInstrument(), executionReportExecuted.getClientOrderId());
                return;
            }
            //CF clean maps
            executionReportMap.remove(clOrdId);
            if (executionReportExecuted.getOrigClientOrderId() != null) {
                origClOrdIdPendingExecutionReport.remove(executionReportExecuted.getOrigClientOrderId());
            }
            //we need the tickets to close position later!
            //				MTOrder.cleanTicketOrder(clOrdId);

        }
        updatePosition(executionReportExecuted);

        //notify it
        notifyExecutionReportById(executionReportExecuted);
        if (isCF) {
            CfTradesNotified.offer(executionReportExecuted.getClientOrderId());
        }

    }

    private void treatClosePositionOrTrade(MTMessageER mtMessageER) {
        ////{'_action': 'CLOSE', '_ticket': 2006095372, '_response': '10009', 'response_value': 'Request completed', '_close_price': 1.39119000, '_close_lots': 0.01000000, '_response': 'CLOSE_MARKET', '_response_value': 'SUCCESS'}
        // {'_action': 'EXECUTION', '_magic': 35ee9a40-7045-3c66-9a48-3f991305e260, '_response': 'Position partially closed, but corresponding deal cannot be selected'}
        //{'_action': 'EXECUTION', '_magic': a3c05110-6c28-3ac2-b955-4f693785c4fa, '_response': 'Position partially closed, but corresponding deal cannot be selected'}
        //{_response='null', _action='CLOSE_ALL', _response_value='SUCCESS', _magic='null', _ticket='null', _open_time='null', _open_price='null', _last_qty='null', _qty_total='null', _close_price='null', _close_lots='null'}
        String magic = mtMessageER.get_magic();
        String response = mtMessageER.get_response();
        String responseValue = mtMessageER.get_response_value();
        String ticket = mtMessageER.get_ticket();
        boolean isCloseTrade = isCloseTrade(mtMessageER);
        boolean isPartiallyClosePosition = isPartiallyClosePosition(mtMessageER);

        logger.info("ER close position detected");
        boolean overrideQtyWithRequest = mtMessageER.get_comment() != null && mtMessageER.get_comment().equals("closed_partially");

        String origClientOrderId = "";
        if (isCloseTrade) {
            origClientOrderId = mtMessageER.get_magic();
            ticket = MTCommand.getTicket(origClientOrderId);
        } else {
            origClientOrderId = MTCommand.getClientOrderId(ticket);
        }
        System.out.println(
                "ER close position detected ticket:" + ticket + "  origClientOrderId:" + origClientOrderId + "->" + mtMessageER
                        .toString());
        OrderRequest pendingOrderRequest = null;

        if (origClientOrderId != null) {
            ///something faster!
            pendingOrderRequest = origClOrdIdPendingExecutionReport.get(origClientOrderId);
            if (pendingOrderRequest == null) {
                //getting from pending
                pendingOrderRequest = pendingExecutionReport.get(origClientOrderId);
            }
        } else {
            //trying to rescue from maps
            String symbol = mtMessageER.get_comment();
            if (symbol != null) {
                String instrumentPK = getInstrumentPk(symbol);
                List<OrderRequest> LastBuyOrSell = new ArrayList<>();

                OrderRequest lastBuy = instrumentTolastBuyOrderSent.get(instrumentPK);
                if (lastBuy != null) {
                    LastBuyOrSell.add(instrumentTolastBuyOrderSent.get(instrumentPK));
                }
                OrderRequest lastSell = instrumentTolastSellOrderSent.get(instrumentPK);
                if (lastSell != null) {
                    LastBuyOrSell.add(instrumentTolastSellOrderSent.get(instrumentPK));
                }

                LastBuyOrSell.addAll(pendingExecutionReport.values());
                for (OrderRequest orderRequest : LastBuyOrSell) {
                    if (orderRequest.getInstrument().equals(instrumentPK)) {
                        if (CfTradesNotified.contains(orderRequest.getClientOrderId())) {
                            //if its something previously confirmed discard it
                            continue;
                        }
                        pendingOrderRequest = orderRequest;
                        break;
                    }
                }
            }
            if (pendingOrderRequest != null) {
                logger.warn("rescue pendingOrderRequest 1 {} from maps to notify ER {}", pendingOrderRequest.getClientOrderId(), pendingOrderRequest);
            }
            if (pendingOrderRequest == null) {
                Optional<String> firstOrig = origClOrdIdPendingExecutionReport.keySet().stream().findFirst();
                if (firstOrig.isPresent()) {
                    origClientOrderId = firstOrig.get();
                    pendingOrderRequest = origClOrdIdPendingExecutionReport.get(origClientOrderId);
                } else {
                    Optional<String> firstOrig1 = pendingExecutionReport.keySet().stream().findFirst();
                    if (firstOrig1.isPresent()) {
                        origClientOrderId = firstOrig1.get();
                        pendingOrderRequest = pendingExecutionReport.get(origClientOrderId);
                    }
                }
            }
            if (pendingOrderRequest != null) {
                logger.warn("rescue pendingOrderRequest 2  {} from maps to notify ER {}", pendingOrderRequest.getClientOrderId(), pendingOrderRequest);
            }
        }

        if (pendingOrderRequest == null) {
            logger.warn("ER of close not found origClOrdId {} to ticket {}", origClientOrderId, ticket);
            System.err.println("ER of close not found origClOrdId " + origClientOrderId + " to ticket " + ticket);
            return;
        }

        //notifyActiveFirst
        Pair<Boolean, ExecutionReport> tupleActiveOut = notifyActiveIfSuccess(pendingOrderRequest, mtMessageER);
        boolean weNotifyActive = tupleActiveOut.getLeft();
        if (weNotifyActive) {
            ExecutionReport executionReport = tupleActiveOut.getRight();
            //notify filled
            //wait
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                logger.error("can't sleep between reporting active and CF ", e);
                e.printStackTrace();
            }
            ///notify trade

//            double qtyFilled = Double.valueOf(mtExecutionReport.get_last_qty());
//            double qtyFilled = executionReport.getQuantity();
            double qtyFilled = pendingOrderRequest.getQuantity();//override with what we sent -> cosider everything is executed always todo change it
            //			double qtyTotal = executionReportExecuted.getQuantity();//should be == Qty
            executionReport.setLastQuantity(qtyFilled);
//            double qtyFill = qtyFilled + executionReport.getQuantityFill();
//            executionReport.setQuantityFill(qtyFill);
            executionReport.setQuantityFill(qtyFilled);
            executionReport.setQuantity(qtyFilled);//we are overriding what we receive here!
            //todo something better , we are considering every close position is done!!

            boolean isCF = executionReport.getQuantity() == executionReport.getQuantityFill();
            if (isCF) {
                executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
            } else {
                //should never be here...
                logger.warn("what is happening here?¿ partialFilled closing position? {}", mtMessageER);
                activeOrders.put(executionReport.getClientOrderId(), pendingOrderRequest);
            }


            String closePriceER = mtMessageER.get_close_price();
            if (closePriceER == null) {
                closePriceER = String.valueOf(pendingOrderRequest.getPrice());
                //todo use market price
                logger.warn("mtExecutionReport without close_price 2 => get close price from OrderRequest {} {}", pendingOrderRequest.getClientOrderId(), closePriceER);
            }
            executionReport.setPrice(Double.valueOf(closePriceER));
            executionReportMap.remove(origClientOrderId);
            notifyExecutionReportById(executionReport);
            CfTradesNotified.offer(executionReport.getClientOrderId());

            if (executionReport.getOrigClientOrderId() != null) {
                origClOrdIdPendingExecutionReport.remove(executionReport.getOrigClientOrderId());
            }
        } else {
            ExecutionReport executionReport = tupleActiveOut.getRight();
            if (executionReport.getExecutionReportStatus() == ExecutionReportStatus.Rejected) {
                //notify rejection at least
                notifyExecutionReportById(executionReport);
            } else {
                logger.warn("notActive and not rejected detected {}", executionReport);
            }


        }


    }

    public void onUpdateExecutionReport(ConnectorConfiguration configuration, long timestampReceived,
                                        TypeMessage typeMessage, String content) {
        logger.debug("onUpdateExecutionReport Metatrader  {} {}", typeMessage, content);

        MTMessageER mtMessageER = null;
        try {
            mtMessageER = GSON_STRING.fromJson(content, MTMessageER.class);
        } catch (Exception e) {
            logger.error("error parsing execution report message {}", content, e);
            return;
        }

        if (mtMessageER.get_action().equals("OPEN_POSITIONS")) {
            onOpenPositions(configuration, timestampReceived, content);
            return;
        }

        String magic = mtMessageER.get_magic();
        String ticket = mtMessageER.get_ticket();

        boolean thereAreErrorsFormat = (magic == null && ticket == null && mtMessageER.get_action() == null);
        if (thereAreErrorsFormat) {
            TreatExecutionReportWithErrors(mtMessageER, content);
            return;
        }

        boolean isActiveConfirmation = isActiveConfirmation(mtMessageER);
        //Position partially close but corresponding deal can not be selected
        boolean isPartiallyClosePosition = isPartiallyClosePosition(mtMessageER);


        boolean isTrade = mtMessageER.get_action().equalsIgnoreCase("TRADE");
        boolean isDelete = mtMessageER.get_action().equalsIgnoreCase("DELETE");
        boolean isClosePosition = mtMessageER.get_action().equalsIgnoreCase("CLOSE") ||
                mtMessageER.get_action().equalsIgnoreCase("CLOSE_ALL") ||
                mtMessageER.get_action().equalsIgnoreCase("CLOSE_PARTIAL");//create Active + Cf
        boolean isCloseTrade = isCloseTrade(mtMessageER);

        logger.info("ER received {}", mtMessageER);
        System.out.println("ER received " + mtMessageER);


        boolean isRejection = magic != null && ticket == null && isActiveConfirmation && !isPartiallyClosePosition;
        boolean isConfirmation = magic != null && ticket != null && isActiveConfirmation;
        boolean isDeleted = (magic != null && ticket != null && isDelete);
        boolean isClosePositionTrade = isClosePosition || isCloseTrade;
        if (isRejection) {
            //// rejected!
            treatRejection(mtMessageER);
            return;
        }
        if (isConfirmation) {
            treatConfirmation(mtMessageER);
            return;
        }
        if (isDeleted) {
            treatDeleted(mtMessageER);
            return;
        }
        if (isTrade) {
            treatTrade(mtMessageER);
            return;
        }

        if (isClosePositionTrade) {
            treatClosePositionOrTrade(mtMessageER);
            return;
        }
    }


    private void updatePosition(ExecutionReport executionReport) {
        //update positions
        double beforePos = getPosition(executionReport.getInstrument());
        double newPosition = 0;
        if (executionReport.getVerb().equals(Verb.Buy)) {
            newPosition = beforePos + executionReport.getLastQuantity();
        }
        if (executionReport.getVerb().equals(Verb.Sell)) {
            newPosition = beforePos - executionReport.getLastQuantity();
        }
        lastPosition.put(executionReport.getInstrument(), newPosition);

    }


}
