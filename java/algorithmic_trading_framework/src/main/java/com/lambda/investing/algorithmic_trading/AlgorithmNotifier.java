package com.lambda.investing.algorithmic_trading;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lambda.investing.connector.ThreadUtils;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

public class AlgorithmNotifier {

	ThreadFactory namedThreadFactory;
	private ThreadPoolExecutor notifierPool;
	private int threadsNotifier;
	private String algorithmInfo;
	private Algorithm algorithm;

	public AlgorithmNotifier(Algorithm algorithm, int threadsNotifier) {
		this.threadsNotifier = threadsNotifier;
		this.algorithmInfo = algorithm.algorithmInfo;
		this.algorithm = algorithm;

		ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
		threadFactoryBuilder.setNameFormat(this.algorithmInfo + "_notifier" + "-%d");
		threadFactoryBuilder.setPriority(Thread.MIN_PRIORITY);
		namedThreadFactory = threadFactoryBuilder.build();

		notifierPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.threadsNotifier, namedThreadFactory);
	}

	public void setAlgorithmInfo(String algorithmInfo) {
		this.algorithmInfo = algorithmInfo;
	}

	//trades
	private void _notifyObserversOnUpdateTrade(PnlSnapshot pnlSnapshot) {
		for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
			algorithmObserver.onUpdateTrade(this.algorithmInfo, pnlSnapshot);
		}
	}

	public void notifyObserversOnUpdateTrade(PnlSnapshot pnlSnapshot) {
		if (algorithm.getAlgorithmObservers().size() > 0) {
			notifierPool.submit(() -> {
				_notifyObserversOnUpdateTrade(pnlSnapshot);
			});
		}
	}

	//depth
	private void _notifyObserversOnUpdateDepth(Depth depth) {
		for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
			algorithmObserver.onUpdateDepth(this.algorithmInfo, depth);
		}
	}

	public void notifyObserversOnUpdateDepth(Depth depth) {
		notifierPool.submit(() -> {
			_notifyObserversOnUpdateDepth(depth);
		});
	}

	//lastClose
	private void _notifyObserversOnUpdateClose(Trade trade) {
		for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
			algorithmObserver.onUpdateClose(this.algorithmInfo, trade);
		}
	}

	public void notifyObserversOnUpdateClose(Trade trade) {
		if (algorithm.getAlgorithmObservers().size() > 0) {
			notifierPool.submit(() -> {
				_notifyObserversOnUpdateClose(trade);
			});
		}
	}

	//params
	private void _notifyObserversOnUpdateParams(Map<String, Object> params) {
		for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
			algorithmObserver.onUpdateParams(this.algorithmInfo, params);
		}
	}

	public void notifyObserversOnUpdateParams(Map<String, Object> params) {
		if (algorithm.getAlgorithmObservers().size() > 0) {
			notifierPool.submit(() -> {
				_notifyObserversOnUpdateParams(params);
			});
		}
	}

	//message
	private void _notifyObserversOnUpdateMessage(String name, String body) {
		for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
			algorithmObserver.onUpdateMessage(this.algorithmInfo, name, body);
		}
	}

	public void notifyObserversOnUpdateMessage(String name, String body) {
		if (algorithm.getAlgorithmObservers().size() > 0) {
			notifierPool.submit(() -> {
				_notifyObserversOnUpdateMessage(name, body);
			});
		}
	}

	public void notifyObserversOnOrderRequest(OrderRequest orderRequest) {
		if (algorithm.getAlgorithmObservers().size() > 0) {
			notifierPool.submit(() -> {
				_notifyObserversOnOrderRequest(orderRequest);
			});
		}
	}

	private void _notifyObserversOnOrderRequest(OrderRequest orderRequest) {
		for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
			algorithmObserver.onOrderRequest(this.algorithmInfo, orderRequest);
		}
	}

	public void notifyObserversonExecutionReportUpdate(ExecutionReport executionReport) {
		if (algorithm.getAlgorithmObservers().size() > 0) {
			notifierPool.submit(() -> {
				_notifyObserversonExecutionReportUpdate(executionReport);
			});
		}
	}

	private void _notifyObserversonExecutionReportUpdate(ExecutionReport executionReport) {
		for (AlgorithmObserver algorithmObserver : algorithm.getAlgorithmObservers()) {
			algorithmObserver.onExecutionReportUpdate(this.algorithmInfo, executionReport);
		}
	}
}
