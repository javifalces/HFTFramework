package com.lambda.investing.algorithmic_trading.technical_indicators;

import com.lambda.investing.ArrayUtils;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Calculator {

    protected static Logger logger = LogManager.getLogger(Calculator.class);

    private static Core CORE = new Core();

    public static double Round(double input, double decimalDen) {
        return ((Math.round(input * decimalDen)) / decimalDen);
    }

    public static double Round(double input, int numberOfDecimals) {
        double value = input;
        if (numberOfDecimals > 0) {
            double decimalDen = Math.pow(10, numberOfDecimals);
            if (decimalDen != 0) {
                value = Round(value, decimalDen);
            }
        }
        return value;

    }

    public static double RSICalculate(double[] closePrices, int period) {
        double[] output = new double[closePrices.length];
        MInteger begin = new MInteger();
        MInteger length = new MInteger();

        RetCode retCode = CORE.rsi(0, closePrices.length - 1, closePrices, period, begin, length, output);
        if (retCode.equals(RetCode.Success)) {
            //get the last value
            double outputVal = 50.0;/// to avoid buying on error!
            for (int i = 0; i < output.length; i++) {
                if (output[i] == 0) {
                    break;
                }
                outputVal = output[i];
            }
            return outputVal;
        } else {
            logger.error("cant calculate RSI -> {}", retCode);
            return 50.0;
        }
    }

    public static double SMACalculate(double[] closePrices, int period) {
        double[] output = new double[closePrices.length];
        MInteger begin = new MInteger();
        MInteger length = new MInteger();

        RetCode retCode = CORE.sma(0, closePrices.length - 1, closePrices, period, begin, length, output);
        if (retCode.equals(RetCode.Success)) {
            //get the last value
            double outputVal = closePrices[closePrices.length - 1];/// to avoid buying on error!
            for (int i = 0; i < output.length; i++) {
                if (output[i] == 0) {
                    break;
                }
                outputVal = output[i];
            }
            return outputVal;
        } else {
            logger.error("cant calculate SMA -> {}", retCode);
            return closePrices[closePrices.length - 1];
        }
    }

    public static double EMACalculate(double[] closePrices, int period) {
        double[] output = new double[closePrices.length];
        MInteger begin = new MInteger();
        MInteger length = new MInteger();
        //ema(int startIdx, int endIdx, double[] inReal, int optInTimePeriod, MInteger outBegIdx, MInteger outNBElement, double[] outReal)
        RetCode retCode = CORE.ema(0, closePrices.length - 1, closePrices, period, begin, length, output);
        if (retCode.equals(RetCode.Success)) {
            //get the last value
            double outputVal = closePrices[closePrices.length - 1];/// to avoid buying on error!
            for (int i = 0; i < output.length; i++) {
                //take the last value != 0
                if (output[i] == 0) {
                    break;
                }
                outputVal = output[i];
            }
            return outputVal;
        } else {
            logger.error("cant calculate EMA -> {}", retCode);
            return closePrices[closePrices.length - 1];
        }
    }

    /***
     * stddev is a rolling biased stddev of period optInTimePeriod that is multiplied by optInNbDev. So this function results in an array of stdevs made for each input array value considering subarray with length of optInTimePeriod.
     * https://sourceforge.net/p/ta-lib/code/HEAD/tree/trunk/ta-lib/c/src/ta_func/ta_VAR.c#l302
     * @param closePrices
     * @param stds
     * @param period
     * @return
     */
    public static double StdCalculate(double[] closePrices, double stds, int period) {
        double[] output = new double[closePrices.length];
        MInteger begin = new MInteger();
        MInteger length = new MInteger();
        //		//ema(int startIdx, int endIdx, double[] inReal, int optInTimePeriod, MInteger outBegIdx, MInteger outNBElement, double[] outReal)
        //		  stdDev(int startIdx, int endIdx, double[] inReal, int optInTimePeriod, double optInNbDev, MInteger outBegIdx, MInteger outNBElement, double[] outReal)
        RetCode retCode = CORE.stdDev(0, closePrices.length - 1, closePrices, period, stds, begin, length, output);
        if (retCode.equals(RetCode.Success)) {
            //get the last value
            double outputVal = closePrices[closePrices.length - 1];/// to avoid buying on error!
            for (int i = 0; i < output.length; i++) {
                //take the last value != 0
                if (output[i] == 0) {
                    break;
                }
                outputVal = output[i];
            }
            return outputVal;
        } else {
            logger.error("cant calculate STD -> {}", retCode);
            return closePrices[closePrices.length - 1];
        }
    }

    /**
     * when the indicator switches from 0 to 1 it means that a new trend started, it entered a trending phase.
     * As such, when Hilbert Transforms switches to 1, it means that a possible trend starts. We must use this information together with the one given by the SINE Wave.
     *
     * @param closePrices
     * @param period
     * @return
     */
    public static int HilbertCycleTrendCalculate(double[] closePrices, int period) {
        int[] output = new int[closePrices.length];
        MInteger begin = new MInteger();
        MInteger length = new MInteger();
        int endIdx = closePrices.length - 1;
        int startIdx = endIdx - period;

        RetCode retCode = CORE.htTrendMode(startIdx, endIdx, closePrices, begin, length, output);
        if (retCode.equals(RetCode.Success)) {
            //get the last value
            int outputVal = output[output.length - period];/// to avoid buying on error!
            //			double sum = 0;
            //			for(int i = 0; i < output.length; i++) {
            //				sum += output[i];
            //			}
            //			if (sum!=0){
            //				System.out.print("");
            //			}
            return outputVal;

        } else {
            logger.error("cant calculate HilbertCycleTrendCalculate -> {}", retCode);
            return 0;
        }
    }

    public static double WMACalculate(double[] closePrices, int period) {
        double[] output = new double[closePrices.length];
        MInteger begin = new MInteger();
        MInteger length = new MInteger();

        RetCode retCode = CORE.wma(0, closePrices.length - 1, closePrices, period, begin, length, output);
        if (retCode.equals(RetCode.Success)) {
            //get the last value
            double outputVal = closePrices[closePrices.length - 1];/// to avoid buying on error!
            for (int i = 0; i < output.length; i++) {
                if (output[i] == 0) {
                    break;
                }
                outputVal = output[i];
            }
            return outputVal;
        } else {
            logger.error("cant calculate WMA -> {}", retCode);
            return closePrices[closePrices.length - 1];
        }
    }


    public static double ATRCalculate(double[] closePrices, double[] highPrices, double[] lowPrices, int period) {
        double[] output = new double[closePrices.length];
        MInteger begin = new MInteger();
        MInteger length = new MInteger();

        RetCode retCode = CORE
                .atr(0, closePrices.length - 1, highPrices, lowPrices, closePrices, period, begin, length, output);
        if (retCode.equals(RetCode.Success)) {
            //get the last value
            double outputVal = closePrices[closePrices.length - 1];/// to avoid buying on error!
            for (int i = 0; i < output.length; i++) {
                if (output[i] == 0) {
                    break;
                }
                outputVal = output[i];
            }
            return outputVal;
        } else {
            logger.error("cant calculate ATR -> {}", retCode);
            return closePrices[closePrices.length - 1];
        }
    }

    public static boolean IsMonotonicIncreasing(double[] numbers) {
        int lastCmp = 0;
        if (ArrayUtils.sum(numbers) == 0) {
            return false;
        }
        for (int i = 1; i < numbers.length; i++) {
            int cmp = Double.compare(numbers[i], numbers[i - 1]);

            if (lastCmp == 0) {
                lastCmp = cmp;
            } else if (cmp != 0 && ((cmp > 0) != (lastCmp > 0))) {
                return false;
            }
        }

        return true;
    }

    public static boolean IsMonotonicDecreasing(double[] numbers) {
        int lastCmp = 0;
        if (ArrayUtils.sum(numbers) == 0) {
            return false;
        }
        for (int i = 1; i < numbers.length; i++) {
            int cmp = Double.compare(numbers[i], numbers[i - 1]);

            if (lastCmp == 0) {
                lastCmp = cmp;
            } else if (cmp != 0 && ((cmp < 0) != (lastCmp < 0))) {
                return false;
            }
        }

        return true;
    }

}
