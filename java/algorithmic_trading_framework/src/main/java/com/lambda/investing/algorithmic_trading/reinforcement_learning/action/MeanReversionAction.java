package com.lambda.investing.algorithmic_trading.reinforcement_learning.action;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.Objects;

public class MeanReversionAction extends AbstractAction {

    public static Integer PERIODS_INDEX = 0;
    public static Integer UPPER_BOUND_INDEX = 1;
    public static Integer UPPER_BOUND_EXIT_INDEX = 2;
    public static Integer LOWER_BOUND_INDEX = 3;
    public static Integer LOWER_BOUND_EXIT_INDEX = 4;
    public static Integer CHANGESIDE_INDEX = 5;
    public static Integer LEVELQUOTE_INDEX = 6;
    public static int SIZE_ARRAY_ACTION = 7;//0-6

    private int[] periods;
    private int[] changeSides;
    private double[] upperBounds;
    private double[] upperBoundsExits;
    private double[] lowerBounds;
    private double[] lowerBoundsExits;
    private int[] levelToQuotes;

    private int numberOfActions;

    private BiMap<Integer, ActionRow> actionIndexToArr;

    public MeanReversionAction(int[] periods, double[] upperBounds, double[] upperBoundsExits, double[] lowerBounds,
                               double[] lowerBoundsExits, int[] changeSides, int[] levelToQuotes

    ) {


        this.periods = periods;
        this.upperBounds = upperBounds;
        this.upperBoundsExits = upperBoundsExits;
        this.lowerBounds = lowerBounds;
        this.lowerBoundsExits = lowerBoundsExits;
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
        for (int period = 0; period < periods.length; period++) {
            for (int upperBoundIndex = 0; upperBoundIndex < upperBounds.length; upperBoundIndex++) {
                for (int upperBoundExitIndex = 0;
                     upperBoundExitIndex < upperBoundsExits.length; upperBoundExitIndex++) {
                    for (int lowerBoundIndex = 0; lowerBoundIndex < lowerBounds.length; lowerBoundIndex++) {
                        for (int lowerBoundExitIndex = 0;
                             lowerBoundExitIndex < lowerBoundsExits.length; lowerBoundExitIndex++) {
                            for (int changeSideIndex = 0; changeSideIndex < changeSides.length; changeSideIndex++) {
                                for (int levelToQuoteIndex = 0;
                                     levelToQuoteIndex < levelToQuotes.length; levelToQuoteIndex++) {

                                    inputArr[PERIODS_INDEX] = periods[period];
                                    inputArr[UPPER_BOUND_INDEX] = upperBounds[upperBoundIndex];
                                    inputArr[UPPER_BOUND_EXIT_INDEX] = upperBoundsExits[upperBoundExitIndex];
                                    inputArr[LOWER_BOUND_INDEX] = lowerBounds[lowerBoundIndex];
                                    inputArr[LOWER_BOUND_EXIT_INDEX] = lowerBoundsExits[lowerBoundExitIndex];
                                    inputArr[CHANGESIDE_INDEX] = changeSides[changeSideIndex];
                                    inputArr[LEVELQUOTE_INDEX] = levelToQuotes[levelToQuoteIndex];
                                    getAction(inputArr);
                                    counter++;
                                }
                            }

                        }
                    }
                }

            }
        }
//        assert counter == getNumberActions();
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

        private int period, changeSide, levelToQuote;
        private double upperBound, upperBoundExit, lowerBound, lowerBoundExit;

        public ActionRow(int period, double upperBound, double upperBoundExit, double lowerBound, double lowerBoundExit,
                         int changeSide, int levelToQuote) {
            this.period = period;
            this.upperBound = upperBound;
            this.upperBoundExit = upperBoundExit;
            this.lowerBound = lowerBound;
            this.lowerBoundExit = lowerBoundExit;
            this.changeSide = changeSide;
            this.levelToQuote = levelToQuote;
        }

        public ActionRow(double[] arrayInput) {
            this.period = (int) arrayInput[PERIODS_INDEX];
            this.upperBound = arrayInput[UPPER_BOUND_INDEX];
            this.upperBoundExit = arrayInput[UPPER_BOUND_EXIT_INDEX];

            this.lowerBound = arrayInput[LOWER_BOUND_INDEX];
            this.lowerBoundExit = arrayInput[LOWER_BOUND_EXIT_INDEX];
            this.changeSide = (int) arrayInput[CHANGESIDE_INDEX];
            this.levelToQuote = (int) arrayInput[LEVELQUOTE_INDEX];

        }

        @Override public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof ActionRow))
                return false;
            ActionRow actionRow = (ActionRow) o;
            return period == actionRow.period && Double.compare(actionRow.upperBound, upperBound) == 0
                    && Double.compare(actionRow.upperBoundExit, upperBoundExit) == 0
                    && Double.compare(actionRow.lowerBound, lowerBound) == 0
                    && Double.compare(actionRow.lowerBoundExit, lowerBoundExit) == 0
                    && changeSide == actionRow.changeSide && levelToQuote == actionRow.levelToQuote;
        }

        @Override public int hashCode() {

            return Objects
                    .hash(period, upperBound, upperBoundExit, lowerBound, lowerBoundExit, changeSide, levelToQuote);
        }

        public double[] getArray() {
            double[] output = new double[SIZE_ARRAY_ACTION];
            output[PERIODS_INDEX] = period;
            output[UPPER_BOUND_INDEX] = upperBound;
            output[UPPER_BOUND_EXIT_INDEX] = upperBoundExit;
            output[LOWER_BOUND_INDEX] = lowerBound;
            output[LOWER_BOUND_EXIT_INDEX] = lowerBoundExit;
            output[CHANGESIDE_INDEX] = changeSide;
            output[LEVELQUOTE_INDEX] = levelToQuote;
            return output;

        }
    }

}
