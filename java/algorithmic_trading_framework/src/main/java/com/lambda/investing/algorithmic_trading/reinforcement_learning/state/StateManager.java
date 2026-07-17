package com.lambda.investing.algorithmic_trading.reinforcement_learning.state;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmObserver;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PortfolioSnapshot;
import com.lambda.investing.algorithmic_trading.candle_manager.CandleListener;
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

@Getter
@Setter
public class StateManager implements AlgorithmObserver, CandleListener, Runnable {

    protected Logger logger = LogManager.getLogger(StateManager.class);
    private static long MAX_WAIT_PORTFOLIO_SNAPSHOT_UPDATE_MS = 1000 * 60 * 5;//5 minutes without update
    AbstractState abstractState;
    Algorithm algorithm;
    Thread pnlSnapshotForceUpdate;
    boolean lastIsReady = false;

    PortfolioSnapshot lastPortfolioSnapshotSend = null;
    Long currentTimestamp;

    private boolean threadAutoFillPnlSnapshot = true;

    public StateManager(Algorithm algorithm, AbstractState abstractState) {
        this.algorithm = algorithm;
        this.abstractState = abstractState;
        this.algorithm.getCandleFromTickUpdater().register(this);
        this.algorithm.register(this);
        initThreadAutoFillPnlSnapshot();

    }

    private void initThreadAutoFillPnlSnapshot() {
        threadAutoFillPnlSnapshot = true;
        pnlSnapshotForceUpdate = new Thread(this, "pnlSnapshotForceUpdate");
        pnlSnapshotForceUpdate.start();
    }

    @Override
    public void onUpdateDepth(String algorithmInfo, Depth depth) {
        currentTimestamp = depth.getTimestamp();
        abstractState.updateDepthState(depth);
    }

    public boolean isReady() {
        boolean output = abstractState.isReady();
        if (!lastIsReady && output) {
            Date currentDate = new Date(currentTimestamp);
            logger.info("{} stateManager is ready", currentDate);
//			System.out.println(currentDate + "  StateManager is READY!");
            lastIsReady = true;
        }
        if (output) {
            threadAutoFillPnlSnapshot = false;//not needed anymore!
        }
        return output;
    }

    public void reset() {
        lastIsReady = false;
        currentTimestamp = 0L;
        lastPortfolioSnapshotSend = null;
        abstractState.reset();

        if (!threadAutoFillPnlSnapshot) {
            initThreadAutoFillPnlSnapshot();
        }

    }


    @Override
    public void onUpdatePortfolioSnapshot(String algorithmInfo, PortfolioSnapshot portfolioSnapshot) {
        abstractState.updatePrivateState(portfolioSnapshot);
        lastPortfolioSnapshotSend = portfolioSnapshot;
    }

    public void onCandleUpdate(Candle candle) {
        abstractState.updateCandle(candle);
    }

    @Override
    public void onUpdateTrade(String algorithmInfo, Trade trade) {
        abstractState.updateTrade(trade);
    }

    @Override
    public void onUpdateParams(String algorithmInfo, Map<String, Object> newParams) {

    }

    @Override
    public void onUpdateMessage(String algorithmInfo, String name, String body) {

    }

    @Override
    public void onOrderRequest(String algorithmInfo, OrderRequest orderRequest) {

    }

    @Override
    public void onExecutionReportUpdate(String algorithmInfo, ExecutionReport executionReport) {

    }

    @Override
    public void onCustomColumns(long timestamp, String algorithmInfo, String instrumentPk, String key, Double value) {

    }

    @Override
    public void run() {
        while (threadAutoFillPnlSnapshot) {
            if (lastPortfolioSnapshotSend != null) {
                if (this.algorithm.getCurrentTimestamp() - lastPortfolioSnapshotSend.getLastTimestampUpdate()
                        > MAX_WAIT_PORTFOLIO_SNAPSHOT_UPDATE_MS) {
                    //force update
                    lastPortfolioSnapshotSend.setLastTimestampUpdate(this.algorithm.getCurrentTimestamp());
                    onUpdatePortfolioSnapshot(algorithm.getAlgorithmInfo(), lastPortfolioSnapshotSend);
                }

            }
            Thread.onSpinWait();
        }
    }
}
