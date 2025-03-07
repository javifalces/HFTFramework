package com.lambda.investing.algorithmic_trading.hedging.synthetic_portfolio;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmObserver;
import com.lambda.investing.algorithmic_trading.candle_manager.CandleFromTickUpdater;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

public class DynamicSyntheticInstrument extends SyntheticInstrument implements AlgorithmObserver {

    public DynamicSyntheticInstrument(List<AssetSyntheticPortfolio> assetSyntheticPortfolioList, double mean, double std,
                                      double intercept, Algorithm algorithm, CandleFromTickUpdater candleFromTickUpdater, String regressionPriceType) {
        super(assetSyntheticPortfolioList, mean, std, 0.0, -1, -1, regressionPriceType);
        algorithm.register(this);
        register(algorithm, candleFromTickUpdater);

    }

    public DynamicSyntheticInstrument(String jsonFilePath, Algorithm algorithm, CandleFromTickUpdater candleFromTickUpdater) throws FileNotFoundException {
        super(jsonFilePath);
        register(algorithm, candleFromTickUpdater);
    }

    private void register(Algorithm algorithm, CandleFromTickUpdater candleFromTickUpdater) {
        algorithm.register(this);
        candleFromTickUpdater.register(this::onCandleUpdate);
    }


    @Override
    public void onUpdatePnlSnapshot(String algorithmInfo, PnlSnapshot pnlSnapshot) {

    }

    @Override
    public void onUpdateTrade(String algorithmInfo, Trade trade) {

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

    //TODO: implement the dynamic synthetic instrument
    private void onCandleUpdate(Candle candle) {
        //TODO: implement the dynamic synthetic instrument
        //instrumentToBeta update
    }

    @Override
    public void onUpdateDepth(String algorithmInfo, Depth depth) {
        //TODO: implement the dynamic synthetic instrument
        //instrumentToBeta update
    }
}
