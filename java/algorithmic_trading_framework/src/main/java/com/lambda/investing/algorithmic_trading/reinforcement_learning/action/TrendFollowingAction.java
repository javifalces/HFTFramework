package com.lambda.investing.algorithmic_trading.reinforcement_learning.action;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.Objects;

public class TrendFollowingAction extends AbstractAction {

    public static Integer FAST_PERIODS_INDEX = 0;
    public static Integer SLOW_PERIODS_INDEX = 1;
    public static Integer CHANGESIDE_INDEX = 2;
    public static Integer LEVELQUOTE_INDEX = 3;
    public static int SIZE_ARRAY_ACTION = 4;//0-4

    private int[] fastPeriods;
    private int[] slowPeriods;
    private int[] changeSides;
    private int[] levelToQuotes;

    private int numberOfActions;

    private BiMap<Integer, ActionRow> actionIndexToArr;

    public TrendFollowingAction(int[] fastPeriods, int[] slowPeriods, int[] changeSides, int[] levelToQuotes

    ) {

        this.fastPeriods = fastPeriods;
        this.slowPeriods = slowPeriods;
        this.changeSides = changeSides;
        this.levelToQuotes = levelToQuotes;


        actionIndexToArr = HashBiMap.create();

        //prefill the array
        fillCacheActions();
    }

    public int getNumberActionColumns() {
        return SIZE_ARRAY_ACTION;
    }
    private void fillCacheActions() {
        int counter = 0;
        double[] inputArr = new double[SIZE_ARRAY_ACTION];
        for (int fastPeriodIndex = 0; fastPeriodIndex < fastPeriods.length; fastPeriodIndex++) {
            for (int slowPeriodIndex = 0; slowPeriodIndex < slowPeriods.length; slowPeriodIndex++) {
                for (int changeSideIndex = 0; changeSideIndex < changeSides.length; changeSideIndex++) {
                    for (int levelToQuoteIndex = 0; levelToQuoteIndex < levelToQuotes.length; levelToQuoteIndex++) {

                        inputArr[FAST_PERIODS_INDEX] = fastPeriods[fastPeriodIndex];
                        inputArr[SLOW_PERIODS_INDEX] = slowPeriods[slowPeriodIndex];
                        inputArr[CHANGESIDE_INDEX] = changeSides[changeSideIndex];
                        inputArr[LEVELQUOTE_INDEX] = levelToQuotes[levelToQuoteIndex];
                        getAction(inputArr);
                        counter++;
                    }
                }

            }
        }


        assert actionIndexToArr.size() == counter;
        numberOfActions = counter;
    }

    @Override public int getNumberActions() {
        return this.numberOfActions;
    }

    @Override public int getAction(double[] actionArr) {
        assert actionArr.length == SIZE_ARRAY_ACTION;
        /// iterative method
        ActionRow actionRow = new ActionRow(actionArr);
        Integer positionOut = actionIndexToArr.inverse().get(actionRow);
        if (positionOut == null) {
            positionOut = actionIndexToArr.size();
            actionIndexToArr.put(positionOut, actionRow);

        }
        return positionOut;

        //https://en.wikipedia.org/wiki/Combinatorial_number_system
        //		int window = (int) actionArr[WINDOWS_INDEX];
        //		double risk = actionArr[RISK_AVERSION_INDEX];
        //		double skew = actionArr[SKEW_PRICE_INDEX];
        //		try {
        //			double positionWindows = 0;
        //			try {
        //				positionWindows = binomialCoefficientDouble(
        //						windowsTick.length - ArrayUtils.indexOf(windowsTick, window) + 1, 3);
        //			} catch (NumberIsTooLargeException ex) {
        //			}
        //
        //			double positionRisk = 0;
        //			try {
        //				positionRisk = binomialCoefficientDouble(
        //						riskAversion.length - ArrayUtils.indexOf(riskAversion, risk) + 1, 2);
        //			} catch (NumberIsTooLargeException ex) {
        //			}
        //
        //			double positionSkew = 0;
        //			try {
        //				positionSkew = binomialCoefficientDouble(
        //						skew.length - ArrayUtils.indexOf(skew, skew) + 1, 1);
        //			} catch (NumberIsTooLargeException ex) {
        //			}
        //			int actionPosition = (int) (positionWindows + positionRisk + positionSkew);
        //			if (actionIndexToArr.containsKey(actionPosition)) {
        //				//repeating why?
        //				logger.warn("something is wrong");
        //			}
        //
        //			logger.info("[{}]  to {} {} {} ", actionPosition, window, risk, skew);
        //			actionIndexToArr.put(actionPosition, actionArr);
        //			return actionPosition;
        //
        //		} catch (Exception e) {
        //			logger.error("error calculating action to window {} risk {} skew {}", window, risk, skew, e);
        //			return -1;
        //		}

    }

    @Override public double[] getAction(int actionPos) {
        assert actionPos < getNumberActions();
        assert actionIndexToArr.containsKey(actionPos);
        return actionIndexToArr.get(actionPos).getArray();

    }

    private class ActionRow {

        private int fastPeriod, slowPeriod, changeSide, levelToQuote;

        public ActionRow(int fastPeriod, int slowPeriod, int changeSide, int levelToQuote) {
            this.fastPeriod = fastPeriod;
            this.slowPeriod = slowPeriod;
            this.changeSide = changeSide;
            this.levelToQuote = levelToQuote;
        }

        public ActionRow(double[] arrayInput) {
            this.fastPeriod = (int) arrayInput[FAST_PERIODS_INDEX];
            this.slowPeriod = (int) arrayInput[SLOW_PERIODS_INDEX];
            this.changeSide = (int) arrayInput[CHANGESIDE_INDEX];
            this.levelToQuote = (int) arrayInput[LEVELQUOTE_INDEX];
        }

        @Override public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof ActionRow))
                return false;
            ActionRow actionRow = (ActionRow) o;
            return fastPeriod == actionRow.fastPeriod && slowPeriod == actionRow.slowPeriod
                    && changeSide == actionRow.changeSide && levelToQuote == actionRow.levelToQuote;
        }

        @Override public int hashCode() {

            return Objects.hash(fastPeriod, slowPeriod, changeSide, levelToQuote);
        }

        public double[] getArray() {
            double[] output = new double[SIZE_ARRAY_ACTION];
            output[FAST_PERIODS_INDEX] = fastPeriod;
            output[SLOW_PERIODS_INDEX] = slowPeriod;
            output[CHANGESIDE_INDEX] = changeSide;
            output[LEVELQUOTE_INDEX] = levelToQuote;
            return output;

        }
    }

}
