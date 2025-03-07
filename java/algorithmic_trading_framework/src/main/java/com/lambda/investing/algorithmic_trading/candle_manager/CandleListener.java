package com.lambda.investing.algorithmic_trading.candle_manager;

import com.lambda.investing.model.candle.Candle;

public interface CandleListener {

	void onCandleUpdate(Candle candle);

}
