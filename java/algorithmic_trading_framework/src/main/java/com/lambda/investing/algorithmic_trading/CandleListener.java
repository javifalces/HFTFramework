package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.model.candle.Candle;

public interface CandleListener {

	void onUpdateCandle(Candle candle);

}
