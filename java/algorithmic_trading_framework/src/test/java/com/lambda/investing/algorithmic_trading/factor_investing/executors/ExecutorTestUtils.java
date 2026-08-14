package com.lambda.investing.algorithmic_trading.factor_investing.executors;

import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.trading_engine_connector.ExecutionReportListener;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared test infrastructure for executor unit tests.
 * Contains stub implementations of TradingEngineConnector and MarketDataProvider
 * that record calls without real networking.
 */
public class ExecutorTestUtils {

    /**
     * A stub {@link TradingEngineConnector} that:
     * <ul>
     *   <li>Records all {@link OrderRequest}s sent to it.</li>
     *   <li>Allows tests to manually dispatch {@link ExecutionReport}s to registered listeners.</li>
     * </ul>
     */
    public static class StubTradingEngineConnector implements TradingEngineConnector {

        private final Map<String, ExecutionReportListener> listeners = new HashMap<>();
        private final List<OrderRequest> sentOrders = new ArrayList<>();

        @Override
        public void register(String id, ExecutionReportListener listener) {
            listeners.put(id, listener);
        }

        @Override
        public void deregister(String id, ExecutionReportListener listener) {
            listeners.remove(id);
        }

        @Override
        public boolean orderRequest(OrderRequest orderRequest) {
            sentOrders.add(orderRequest);
            return true;
        }

        @Override
        public void notifyExecutionReport(ExecutionReport executionReport) {
            for (ExecutionReportListener listener : listeners.values()) {
                listener.onExecutionReportUpdate(executionReport);
            }
        }

        @Override
        public void requestInfo(String info) { /* no-op */ }

        @Override
        public void reset() {
            sentOrders.clear();
        }

        @Override
        public boolean isBusy() { return false; }

        @Override
        public boolean cancelAll(Instrument instrument) {
            return false;
        }

        @Override
        public List<OrderRequest> activeOrders() {
            return null;
        }

        /** Returns all orders sent to this connector in order. */
        public List<OrderRequest> getSentOrders() {
            return sentOrders;
        }

        /** Returns the most recently sent OrderRequest or null if none. */
        public OrderRequest getLastSentOrder() {
            return sentOrders.isEmpty() ? null : sentOrders.get(sentOrders.size() - 1);
        }

        /** Clears the sent-orders list. */
        public void clearSentOrders() {
            sentOrders.clear();
        }

        /** Convenience: dispatch an ER to all registered listeners. */
        public void dispatchExecutionReport(ExecutionReport er) {
            notifyExecutionReport(er);
        }
    }

    /**
     * A stub {@link MarketDataProvider} that records registered listeners but never
     * delivers any data (tests drive data directly by calling the executor methods).
     */
    public static class StubMarketDataProvider implements MarketDataProvider {

        private final List<MarketDataListener> listeners = new ArrayList<>();

        @Override
        public void register(MarketDataListener listener) {
            listeners.add(listener);
        }

        @Override
        public void deregister(MarketDataListener listener) {
            listeners.remove(listener);
        }

        @Override
        public void reset() {
            listeners.clear();
        }

        public List<MarketDataListener> getListeners() {
            return listeners;
        }
    }
}
