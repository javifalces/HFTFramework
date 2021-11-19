package com.lambda.investing.algorithmic_trading.reinforcement_learning.state;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmObserver;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.Map;

@Getter @Setter public class StateManager implements AlgorithmObserver, Runnable {

	protected Logger logger = LogManager.getLogger(StateManager.class);
	private static long MAX_WAIT_PNL_SNAPSHOT_UPDATE_MS = 1000 * 60 * 5;//5 minutes without update
	AbstractState abstractState;
	Algorithm algorithm;
	Thread pnlSnapshotForceUpdate;
	boolean lastIsReady = false;
	PnlSnapshot lasPnlSnapshotSend = null;
	Long currentTimestamp;

	private boolean threadAutoFillPnlSnapshot = true;

	public StateManager(Algorithm algorithm, AbstractState abstractState) {
		this.algorithm = algorithm;
		this.abstractState = abstractState;
		this.algorithm.getCandleFromTickUpdater().register(this::onUpdateCandle);
		pnlSnapshotForceUpdate = new Thread(this, "pnlSnapshotForceUpdate");
		this.algorithm.register(this);
		pnlSnapshotForceUpdate.start();
	}

	@Override public void onUpdateDepth(String algorithmInfo, Depth depth) {
		currentTimestamp = depth.getTimestamp();
		abstractState.updateDepthState(depth);
	}

	public boolean isReady() {
		boolean output = abstractState.isReady();
		if (!lastIsReady && output) {
			Date currentDate = new Date(currentTimestamp);
			logger.info("{} stateManager is ready", currentDate);
			System.out.println(currentDate + "  StateManager is READY!");
			lastIsReady = true;
		}
		if (output) {
			threadAutoFillPnlSnapshot = false;//not needed anymore!
		}
		return output;
	}

	@Override public void onUpdateTrade(String algorithmInfo, PnlSnapshot pnlSnapshot) {
		abstractState.updatePrivateState(pnlSnapshot);
		lasPnlSnapshotSend = pnlSnapshot;

	}

	public void onUpdateCandle(Candle candle) {
		abstractState.updateCandle(candle);
	}

	@Override public void onUpdateClose(String algorithmInfo, Trade trade) {
		abstractState.updateTrade(trade);
	}

	@Override public void onUpdateParams(String algorithmInfo, Map<String, Object> newParams) {

	}

	@Override public void onUpdateMessage(String algorithmInfo, String name, String body) {

	}

	@Override public void onOrderRequest(String algorithmInfo, OrderRequest orderRequest) {

	}

	@Override public void onExecutionReportUpdate(String algorithmInfo, ExecutionReport executionReport) {

	}

	@Override public void run() {
		while (threadAutoFillPnlSnapshot) {
			if (lasPnlSnapshotSend != null) {
				if (this.algorithm.getCurrentTimestamp() - lasPnlSnapshotSend.getLastTimestampUpdate()
						> MAX_WAIT_PNL_SNAPSHOT_UPDATE_MS) {
					//force update
					lasPnlSnapshotSend.setLastTimestampUpdate(this.algorithm.getCurrentTimestamp());
					onUpdateTrade(algorithm.getAlgorithmInfo(), lasPnlSnapshotSend);
				}

			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
