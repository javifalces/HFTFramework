package com.lambda.investing.trading_engine_connector;

import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.candle.CandlesInfoRequest;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.lambda.investing.model.Util.toJsonString;
import static com.lambda.investing.model.candle.Candle.REQUESTED_CANDLES_INFO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies that a candles info request received by {@link AbstractTradingEngineConnector#notifyInfo}
 * is resolved through {@link TradingEngineConnector#requestCandles} and forwarded only to the
 * requesting algorithm's listener(s), mirroring the full round trip triggered by
 * {@code Algorithm.downloadCandles}.
 */
public class AbstractTradingEngineConnectorCandlesTest {

    private static class StubConnector extends AbstractTradingEngineConnector {

        private Map<String, List<Candle>> candlesToReturn;

        StubConnector() {
            super("junitTestConnector");
        }

        @Override
        public void register(String algorithmInfo, ExecutionReportListener executionReportListener) {
            Map<ExecutionReportListener, String> insideMap = listenersManager
                    .getOrDefault(algorithmInfo, new java.util.concurrent.ConcurrentHashMap<>());
            insideMap.put(executionReportListener, "");
            listenersManager.put(algorithmInfo, insideMap);
        }

        @Override
        public boolean orderRequest(OrderRequest orderRequest) {
            return true;
        }

        @Override
        public void requestInfo(String info) { /* not exercised in this test */ }

        @Override
        public void setPaperTrading(MarketDataProvider marketDataProvider) { /* not exercised in this test */ }

        @Override
        public Map<String, List<Candle>> requestCandles(Date startDate, Date endDate, Set<String> instruments,
                                                        CandleType candleType, int secondsCandles) {
            return candlesToReturn;
        }
    }

    private static class RecordingListener implements ExecutionReportListener {
        String lastHeader;
        Object lastMessage;
        int updates = 0;

        @Override
        public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
            return true;
        }

        @Override
        public boolean onInfoUpdate(String header, Object message) {
            this.lastHeader = header;
            this.lastMessage = message;
            this.updates++;
            return true;
        }
    }

    @Test
    public void notifyInfo_resolvesCandlesForRequestingAlgorithmOnly() {
        StubConnector connector = new StubConnector();

        Candle candle = new Candle(CandleType.mid_time_seconds_threshold, "eurusd_darwinex", 1.1, 1.2, 1.0, 1.15,
                System.currentTimeMillis());
        Map<String, List<Candle>> expectedCandles = new HashMap<>();
        expectedCandles.put("eurusd_darwinex", Collections.singletonList(candle));
        connector.candlesToReturn = expectedCandles;

        RecordingListener requestingAlgoListener = new RecordingListener();
        RecordingListener otherAlgoListener = new RecordingListener();
        connector.register("algo1", requestingAlgoListener);
        connector.register("algo2", otherAlgoListener);

        CandlesInfoRequest request = new CandlesInfoRequest(new Date(0), new Date(),
                new HashSet<>(Collections.singletonList("eurusd_darwinex")),
                CandleType.mid_time_seconds_threshold, 60);

        connector.notifyInfo(REQUESTED_CANDLES_INFO + ".algo1", toJsonString(request));

        assertEquals(1, requestingAlgoListener.updates);
        assertEquals(0, otherAlgoListener.updates);
        assertNotNull(requestingAlgoListener.lastMessage);

        Map<?, ?> receivedCandles = com.alibaba.fastjson2.JSON.parseObject((String) requestingAlgoListener.lastMessage, Map.class);
        assertEquals(1, receivedCandles.size());
    }

    @Test
    public void notifyInfo_fallsBackToUnresolvedPayload_whenConnectorHasNoCandleSource() {
        StubConnector connector = new StubConnector();
        connector.candlesToReturn = null; //default -> no native candle source

        RecordingListener listener = new RecordingListener();
        connector.register("algo1", listener);

        CandlesInfoRequest request = new CandlesInfoRequest(new Date(0), new Date(), new HashSet<>(), CandleType.mid_time_seconds_threshold, 60);
        String requestJson = toJsonString(request);

        connector.notifyInfo(REQUESTED_CANDLES_INFO + ".algo1", requestJson);

        assertEquals(1, listener.updates);
        assertEquals(requestJson, listener.lastMessage);
    }
}
