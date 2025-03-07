package com.lambda.investing.algorithmic_trading.reinforcement_learning.state;

import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreEnum;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.CandleType;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

import static com.lambda.investing.algorithmic_trading.technical_indicators.Calculator.EMACalculate;

public class DiscreteTAStateFX extends DiscreteTAState {
    public DiscreteTAStateFX(Instrument instrument, ScoreEnum scoreEnumColumn, CandleType candleType, int[] periods, int numberOfDecimals, int marketHorizonSave) {
        super(instrument, scoreEnumColumn, candleType, periods, numberOfDecimals, marketHorizonSave);
        changeColumns();

    }

    private void changeColumns() {
        STATE_COLUMNS_PATTERN = new String[]{
                ////price
//                "microprice_",//midpricee-microprice
//                "vpin_",
                "rsi_",//rsi<30 -> 1 rsi>70->-1 else 0
                "sma_",//sma<price -> -1 sma>=price->1
                "ema_",//sma<price -> -1 sma>=price->1
                "max_",//price>max=1 else 0
                "min_",//price>max=1 else 0
//                //volume
//                "volume_rsi_", "volume_sma_", "volume_ema_", "volume_max_", "volume_min_",
        };
        MARKET_CONDITIONS_ON_MAX_PERIOD_CANDLES = new String[]{
                //queueCandles
//                "signedTransactionVolume_", "signedTransaction_",
//                "microprice_candle_", "vpin_candle_"
        };

        STATE_SINGLE_COLUMNS = new String[]{"hour_of_the_day_utc", "minutes_from_start"};

        MARKET_COLUMNS_PATTERN = new String[]{
                //last ticks properties
                "bid_price_", "ask_price_",
//                "bid_qty", "ask_qty",
                "spread_",
//                "imbalance", "microprice"

        };

        BINARY_STATE_COLUMNS_PATTERN = new String[]{
                ////price
//                "microprice_",//midpricee-microprice
//                "vpin_",
                "rsi_",//rsi<30 -> 1 rsi>70->-1 else 0
                "sma_",//sma<price -> -1 sma>=price->1
                "ema_",//sma<price -> -1 sma>=price->1
                "max_",//price>max=1 else 0
                "min_",//price>max=1 else 0

        };


    }

    protected List<Double> getCurrentMarketState() {
        List<Double> stateList = new ArrayList<>();

        if (marketHorizonSave > 0) {
            try {
                stateList.addAll(bidPriceBuffer);
                stateList.addAll(askPriceBuffer);
                stateList.addAll(spreadBuffer);
            } catch (Exception e) {
                logger.error("error getting marketState ", e);
                stateList.clear();
            }
            if (stateList.size() == 0) {
                return null;
            }
        }
        return stateList;

    }

    protected List<Double> getLastCandlesStates() {
        List<Double> stateList = new ArrayList<>();
        return stateList;
    }


}
