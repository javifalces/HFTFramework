package com.lambda.investing.algorithmic_trading;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimeseriesUtilsTest {

    Double[] TimeSeriesIncreasing = {1.0, 2.0, 3.0, 4.0, 5.0};
    Double[] TimeSeriesDecreasing = {5.0, 4.0, 3.0, 2.0, 1.0};
    Double[] MeanReverting = {1.0, 2.0, 1.0, 2.0, 1.0};
    Double[] TimeSeriesIncreasingPermanent = {1.0, 2.0, 3.0, 4.0, 5.0, 5.0, 5.0, 5.0};

    @Test
    void getMean() {
        assertEquals(3.0, TimeseriesUtils.GetMean(TimeSeriesIncreasing));
        assertEquals(3.0, TimeseriesUtils.GetMean(TimeSeriesDecreasing));
        assertEquals(1.4, TimeseriesUtils.GetMean(MeanReverting));
    }

    @Test
    void getExponentialWeightedMean() {
        //increasing_list = [1.0, 2.0, 3.0, 4.0, 5.0]
        //series = pd.Series(increasing_list)
        //series.ewm(span=5, adjust=False).mean().iloc[-1] -> 3.3950617283
        assertEquals(3.395, TimeseriesUtils.GetExponentialWeightedMean(TimeSeriesIncreasing), 0.01);
        assertEquals(2.604, TimeseriesUtils.GetExponentialWeightedMean(TimeSeriesDecreasing), 0.01);
        assertEquals(1.32, TimeseriesUtils.GetExponentialWeightedMean(MeanReverting), 0.01);
        assertEquals(3.955, TimeseriesUtils.GetExponentialWeightedMean(TimeSeriesIncreasingPermanent), 0.01);
    }

    @Test
    void getVariance() {
        //decreasing_list = [5.0, 4.0, 3.0, 2.0, 1.0]
        //series = pd.Series(decreasing_list)
        //series.ewm(span=5, adjust=False).mean()
        //series.var()
        assertEquals(2.5, TimeseriesUtils.GetVariance(TimeSeriesIncreasing, true));
        assertEquals(2.5, TimeseriesUtils.GetVariance(TimeSeriesDecreasing, true));
        assertEquals(0.3, TimeseriesUtils.GetVariance(MeanReverting, true));
    }


}