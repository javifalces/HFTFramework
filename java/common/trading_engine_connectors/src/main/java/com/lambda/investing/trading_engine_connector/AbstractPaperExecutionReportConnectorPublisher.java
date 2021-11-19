package com.lambda.investing.trading_engine_connector;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter public abstract class AbstractPaperExecutionReportConnectorPublisher implements ExecutionReportPublisher {

	private TradingEngineConnector tradingEngineConnector;

	public AbstractPaperExecutionReportConnectorPublisher(TradingEngineConnector tradingEngineConnector) {
		this.tradingEngineConnector = tradingEngineConnector;

	}

	public void setTradingEngineConnector(TradingEngineConnector tradingEngineConnector) {
		this.tradingEngineConnector = tradingEngineConnector;
	}

	public void register(String algorithmInfo, ExecutionReportListener executionReportListener) {
		this.tradingEngineConnector.register(algorithmInfo, executionReportListener);
	}

	public void deregister(String algorithmInfo, ExecutionReportListener executionReportListener) {
		this.tradingEngineConnector.deregister(algorithmInfo, executionReportListener);
	}

	//in memory , makes no sense with zeromq arquitecture
	//	public void notifyExecutionReport(ExecutionReport executionReport) {
	//		String algorithmInfo = executionReport.getAlgorithmInfo();
	//		Map<ExecutionReportListener, String> insideMap = listenersManager
	//				.getOrDefault(algorithmInfo, new ConcurrentHashMap<>());
	//		if (insideMap.size() > 0) {
	//			for (ExecutionReportListener executionReportListener : insideMap.keySet()) {
	//				executionReportListener.onExecutionReportUpdate(executionReport);
	//			}
	//		}
	//		if (allAlgorithmsExecutionReportListener != null) {
	//			allAlgorithmsExecutionReportListener.onExecutionReportUpdate(executionReport);
	//		}
	//	}
}
