package com.lambda.investing.market_data_connector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lambda.investing.connector.zero_mq.ZeroMqPublisher;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractMarketDataProvider implements MarketDataProvider {

	protected static int QUEUE_FINAL_STATES_SIZE = 300;
	private static boolean CHECK_TIMESTAMPS_RECEIVED = true;//checking if the last timestamp is this one!
	private Map<String, Long> lastDepthReceived;
	private Map<String, Long> lastTradeSentReceived;

	protected Queue<String> lastActiveClOrdId;
	protected Queue<String> lastCfClOrdId;
	protected Queue<String> lastRejClOrdId;

	protected Statistics statisticsReceived;//= new Statistics("Data received", 15 * 1000);

	public static Gson GSON = new GsonBuilder()
			.excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.FINAL)
			.serializeSpecialFloatingPointValues().disableHtmlEscaping().create();

	protected Logger logger = LogManager.getLogger(AbstractMarketDataProvider.class);
	protected Map<MarketDataListener, String> listenersManager;

	public AbstractMarketDataProvider() {
		listenersManager = new ConcurrentHashMap<>();
		lastDepthReceived = new ConcurrentHashMap<>();
		lastTradeSentReceived = new ConcurrentHashMap<>();

		lastActiveClOrdId = EvictingQueue.create(QUEUE_FINAL_STATES_SIZE);
		lastCfClOrdId = EvictingQueue.create(QUEUE_FINAL_STATES_SIZE);
		lastRejClOrdId = EvictingQueue.create(QUEUE_FINAL_STATES_SIZE);
	}

	public void setStatisticsReceived(Statistics statisticsReceived) {
		this.statisticsReceived = statisticsReceived;
	}

	@Override public void register(MarketDataListener listener) {

		listenersManager.put(listener, "");
	}

	@Override public void deregister(MarketDataListener listener) {
		listenersManager.remove(listener);
	}

	public void notifyDepth(Depth depth) {
		if (CHECK_TIMESTAMPS_RECEIVED && depth.getTimestamp() < lastDepthReceived
				.getOrDefault(depth.getInstrument(), 0L)) {
			//not the last snapshot
			return;
		}

		Set<MarketDataListener> listeners = listenersManager.keySet();
		for (MarketDataListener marketDataListener : listeners) {
			marketDataListener.onDepthUpdate(depth);
		}

		lastDepthReceived.put(depth.getInstrument(), depth.getTimestamp());

	}

	public void notifyTrade(Trade trade) {
		if (CHECK_TIMESTAMPS_RECEIVED && trade.getTimestamp() < lastTradeSentReceived
				.getOrDefault(trade.getInstrument(), 0L)) {
			//not the last snapshot
			return;
		}
		Set<MarketDataListener> listeners = listenersManager.keySet();
		for (MarketDataListener marketDataListener : listeners) {
			marketDataListener.onTradeUpdate(trade);
		}

		lastTradeSentReceived.put(trade.getInstrument(), trade.getTimestamp());

	}

	public void notifyCommand(Command command) {
		Set<MarketDataListener> listeners = listenersManager.keySet();
		for (MarketDataListener marketDataListener : listeners) {
			marketDataListener.onCommandUpdate(command);
		}

	}

	public void notifyExecutionReport(ExecutionReport executionReport) {
		if (checkER(executionReport)) {
			Set<MarketDataListener> listeners = listenersManager.keySet();
			for (MarketDataListener marketDataListener : listeners) {
				marketDataListener.onExecutionReportUpdate(executionReport);
			}
		}
	}

	protected boolean checkER(ExecutionReport executionReport) {
		/// void double notifications
		Queue<String> checkList = lastActiveClOrdId;
		if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Active)) {
			checkList = lastActiveClOrdId;
		}
		if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)) {
			checkList = lastCfClOrdId;
		}
		if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected) || executionReport
				.getExecutionReportStatus().equals(ExecutionReportStatus.CancelRejected)) {
			checkList = lastRejClOrdId;
		}

		if (checkList.contains(executionReport.getClientOrderId())) {
			//already processed
			return false;
		} else {
			checkList.add(executionReport.getClientOrderId());
			return true;
		}

	}

	public void notifyInfo(String header, String message) {
		Set<MarketDataListener> listeners = listenersManager.keySet();
		for (MarketDataListener marketDataListener : listeners) {
			marketDataListener.onInfoUpdate(header, message);
		}

	}

}
