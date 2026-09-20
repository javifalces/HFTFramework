package com.lambda.investing.trading_engine_connector;

import com.alibaba.fastjson2.JSON;
import com.lambda.investing.LatencyStatistics;
import com.lambda.investing.Statistics;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.candle.CandlesInfoRequest;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.lambda.investing.model.Util.*;
import static com.lambda.investing.model.candle.Candle.REQUESTED_CANDLES_INFO;
import static com.lambda.investing.model.portfolio.Portfolio.REQUESTED_POSITION_INFO;

public abstract class AbstractTradingEngineConnector implements TradingEngineConnector, ConnectorListener {

    public static final String ALL_ALGORITHMS_SUBSCRIPTION = "*";

    protected String name;
    protected Logger logger = LogManager.getLogger(ZeroMqTradingEngineConnector.class);

    protected Map<String, Map<ExecutionReportListener, String>> listenersManager;

    protected boolean isPaperTrading = false;
    protected PaperTradingEngine paperTradingEngine = null;
    protected List<Instrument> instrumentList = null;//for paper trading only
    protected List<String> cfTradesNotified;

    protected Statistics statisticsReceived;//= new Statistics("Data received", 15 * 1000);
    protected LatencyStatistics latencyStatistics;


    public void setStatisticsReceived(Statistics statisticsReceived) {
        this.statisticsReceived = statisticsReceived;
    }

    public void setLatencyStatistics(LatencyStatistics latencyStatistics) {
        this.latencyStatistics = latencyStatistics;
    }

    public AbstractTradingEngineConnector(String name) {
        this.name = name;
        listenersManager = new ConcurrentHashMap<>();
        cfTradesNotified = new ArrayList<>();
    }

    public PaperTradingEngine getPaperTradingEngine() {
        return paperTradingEngine;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @Override
    public boolean cancelAll(Instrument instrument) {
        return false;
    }


    @Override
    public List<OrderRequest> activeOrders() {
        return null;
    }

    public Map<String, List<Candle>> requestCandles(Date startDate, Date endDate, Set<String> instruments,
                                                    CandleType candleType, int secondsCandles) {
        return null;
    }


    @Override
    public void deregister(String algorithmInfo, ExecutionReportListener executionReportListener) {
        Map<ExecutionReportListener, String> insideMap = listenersManager
                .getOrDefault(algorithmInfo, new ConcurrentHashMap<>());
        insideMap.remove(executionReportListener);
        listenersManager.put(algorithmInfo, insideMap);
    }


    @Override
    public void reset() {
        this.paperTradingEngine.reset();
    }


    @Override
    public void notifyExecutionReport(ExecutionReport executionReport) {
        boolean isCfTrade = executionReport.getExecutionReportStatus().name()
                .equalsIgnoreCase(ExecutionReportStatus.CompletelyFilled.name());
        if (isCfTrade) {
            logger.info("Cf ER on {}  {}@{} {} ", executionReport.getInstrument(), executionReport.getLastQuantity(),
                    executionReport.getPrice(), executionReport.getClientOrderId());
        }
        if (isCfTrade && cfTradesNotified.contains(executionReport.getClientOrderId())) {
            logger.info("discard update of already notified cf trade {}", executionReport.getClientOrderId());
            return;
        }

        // Notify algorithm-specific listeners
        String algorithmInfo = executionReport.getAlgorithmInfo();
        Map<ExecutionReportListener, String> insideMap = listenersManager
                .getOrDefault(algorithmInfo, new ConcurrentHashMap<>());
//        if (insideMap.size() > 1) {
//            logger.warn("DUPLICATE LISTENERS for algorithmInfo={} count={} listeners={}",
//                    algorithmInfo, insideMap.size(), insideMap.keySet());
//        }
        if (insideMap.size() > 0) {
            for (ExecutionReportListener executionReportListener : insideMap.keySet()) {
                executionReportListener.onExecutionReportUpdate(executionReport);
            }
        }

        // Notify wildcard "*" listeners (for portfolio/aggregator that need ALL ERs) - skip in paper trading
        if (!isPaperTrading) {
            Map<ExecutionReportListener, String> allAlgoListeners = listenersManager
                    .getOrDefault(ALL_ALGORITHMS_SUBSCRIPTION, new ConcurrentHashMap<>());
            for (ExecutionReportListener listener : allAlgoListeners.keySet()) {
                listener.onExecutionReportUpdate(executionReport);
            }
        }

        if (isCfTrade) {
            cfTradesNotified.add(executionReport.getClientOrderId());
        }

    }

    public void notifyInfo(String header, Object message) {
        String[] headerParts = header.split("[.]", 2);
        String algorithmInfo = headerParts[0];

        Map<ExecutionReportListener, String> insideMap = listenersManager
                .getOrDefault(algorithmInfo, new ConcurrentHashMap<>());

        if (algorithmInfo.equals(REQUESTED_POSITION_INFO)) {
            insideMap = new HashMap<>();
            for (Map<ExecutionReportListener, String> insideMapIter : listenersManager.values()) {
                insideMap.putAll(insideMapIter);
            }
        }

        if (algorithmInfo.equals(REQUESTED_CANDLES_INFO)) {
            //message carries the (still unresolved) CandlesInfoRequest -> resolve it through this connector
            message = resolveCandlesInfo(fromObject(message, String.class));
            String requestingAlgorithmInfo = headerParts.length > 1 ? headerParts[1] : null;
            insideMap = requestingAlgorithmInfo != null
                    ? listenersManager.getOrDefault(requestingAlgorithmInfo, new ConcurrentHashMap<>())
                    : insideMap;
        }


        if (insideMap.size() > 0) {
            for (ExecutionReportListener executionReportListener : insideMap.keySet()) {
                executionReportListener.onInfoUpdate(header, fromObject(message, String.class));
            }
        }
    }

    /**
     * Resolves a candles info request by delegating to {@link #requestCandles}. Falls back to the
     * original (unresolved) request json when this connector has no native candle source.
     */
    private String resolveCandlesInfo(String requestJson) {
        if (requestJson == null || requestJson.isEmpty()) {
            return requestJson;
        }
        try {
            CandlesInfoRequest request = JSON.parseObject(requestJson, CandlesInfoRequest.class);
            Map<String, List<Candle>> candles = requestCandles(request.getStartDate(), request.getEndDate(),
                    request.getInstrumentPks(), request.getCandleType(), request.getSecondsCandles());
            if (candles != null) {
                return toJsonString(candles);
            }
        } catch (Exception e) {
            logger.error("Error resolving candles info request {}", requestJson, e);
        }
        return requestJson;
    }

    @Override
    public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
                         TypeMessage typeMessage, Object content) {
        //ER read

        if (typeMessage == null) {
            logger.warn("onUpdate received null typeMessage, ignoring message content={}", content);
            return;
        }

        if (typeMessage.equals(TypeMessage.execution_report)) {
            ExecutionReport executionReport = fromObject(content, ExecutionReport.class);
            executionReport.setTimestampAlgoConnector(timestampReceived);
            notifyExecutionReport(executionReport);
        }
        if (typeMessage.equals(TypeMessage.info)) {
            //Real broker (ZeroMq) responses can't use the wire topic to carry the header: the wire
            //topic is hardcoded to "info" so TopicUtils can classify the TypeMessage on receipt (see
            //AbstractBrokerTradingEngine#notifyInfo), so the header travels as a "<header>|<payload>"
            //prefix on the message content instead.
            String header = REQUESTED_POSITION_INFO;
            Object message = content;
            String contentAsString = fromObject(content, String.class);
            int headerSeparatorIdx = contentAsString != null ? contentAsString.indexOf('|') : -1;
            if (headerSeparatorIdx >= 0) {
                header = contentAsString.substring(0, headerSeparatorIdx);
                message = contentAsString.substring(headerSeparatorIdx + 1);
            } else if (configuration instanceof ZeroMqConfiguration) {
                ZeroMqConfiguration config = (ZeroMqConfiguration) configuration;
                header = config.getTopic();
            }
            notifyInfo(header, message);
        }

    }

    public boolean isPaperTrading() {
        return isPaperTrading;
    }

    protected void initPaperTrading() {
        paperTradingEngine.setInstrumentsList(this.instrumentList);
        paperTradingEngine.init();

        PaperMarketDataListener paperMarketDataListener = new PaperMarketDataListener();
        this.paperTradingEngine.getMarketDataProviderIn().register(paperMarketDataListener);
    }

    public void setInstrumentList(List<Instrument> instrumentList) {
        this.instrumentList = instrumentList;
        if (paperTradingEngine != null) {
            initPaperTrading();
        }
    }

    public abstract void setPaperTrading(MarketDataProvider marketDataProvider);

    private class PaperMarketDataListener implements MarketDataListener {

        @Override
        public boolean onDepthUpdate(Depth depth) {
            return true;
        }

        @Override
        public boolean onTradeUpdate(Trade trade) {
            return true;
        }

        @Override
        public boolean onCommandUpdate(Command command) {
            return true;
        }

        @Override
        public boolean onInfoUpdate(String header, Object message) {
            notifyInfo(header, message);
            return true;
        }
    }
}
