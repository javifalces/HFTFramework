package com.lambda.investing.algorithmic_trading.reinforcement_learning.action;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.util.MathUtils;

import java.util.*;

import static org.apache.commons.math3.util.CombinatoricsUtils.binomialCoefficientDouble;

public class AvellanedaAction extends AbstractAction {

    public static Integer WINDOWS_INDEX = 0;
    public static Integer RISK_AVERSION_INDEX = 1;
    public static Integer SKEW_PRICE_INDEX = 2;
    public static Integer K_DEFAULT_INDEX = 3;
    public static Integer A_DEFAULT_INDEX = 4;
    public static Integer K_PERIOD_DEFAULT_INDEX = 5;

    public static int ACTION_COLUMNS = 6;//WINDOWS_INDEX RISK_AVERSION_INDEX SKEW_PRICE_INDEX K_DEFAULT_INDEX A_DEFAULT_INDEX K_PERIOD_DEFAULT_INDEX

    private int[] windowsTick;
    private double[] riskAversion;
    private double[] skew;

    private double[] kDefault;
    private double[] aDefault;
    private int[] kPeriod;
    private int numberOfActions;

    private BiMap<Integer, ActionRow> actionIndexToArr;

    public AvellanedaAction(int[] windowsTick, double[] riskAversion, double[] skew, double[] kDefault,
                            double[] aDefault, int[] kPeriod) {


        this.windowsTick = windowsTick;
        this.riskAversion = riskAversion;
        this.skew = skew;
        this.kDefault = kDefault;
        this.aDefault = aDefault;
        this.kPeriod = kPeriod;

        actionIndexToArr = HashBiMap.create();

        //prefill the array
        fillCacheActions();

    }

    public int getNumberActionColumns() {
        return ACTION_COLUMNS;
    }
    private void fillCacheActions() {
        int counter = 0;
        double[] inputArr = new double[ACTION_COLUMNS];
        for (int windowIndex = 0; windowIndex < windowsTick.length; windowIndex++) {
            for (int riskIndex = 0; riskIndex < riskAversion.length; riskIndex++) {
                for (int skewIndex = 0; skewIndex < skew.length; skewIndex++) {
                    for (int kIndex = 0; kIndex < kDefault.length; kIndex++) {
                        for (int aIndex = 0; aIndex < aDefault.length; aIndex++) {
                            for (int kPeriodIndex = 0; kPeriodIndex < kPeriod.length; kPeriodIndex++) {
                                inputArr[WINDOWS_INDEX] = windowsTick[windowIndex];
                                inputArr[RISK_AVERSION_INDEX] = riskAversion[riskIndex];
                                inputArr[SKEW_PRICE_INDEX] = skew[skewIndex];
                                inputArr[K_DEFAULT_INDEX] = kDefault[kIndex];
                                inputArr[A_DEFAULT_INDEX] = aDefault[aIndex];
                                inputArr[K_PERIOD_DEFAULT_INDEX] = kPeriod[kPeriodIndex];
                                getAction(inputArr);
                                counter++;
                            }
                        }
                    }
                }
            }
        }
        assert actionIndexToArr.size() == counter;
        this.numberOfActions = actionIndexToArr.size();

    }

    @Override
    public int getNumberActions() {
        return this.numberOfActions;
    }

    @Override
    public int getAction(double[] actionArr) {
        assert actionArr.length == ACTION_COLUMNS;
        /// iterative method
        ActionRow actionRow = new ActionRow(actionArr);
        Integer positionOut = actionIndexToArr.inverse().get(actionRow);
        if (positionOut == null) {
            positionOut = actionIndexToArr.size();
            actionIndexToArr.put(positionOut, actionRow);

        }
        return positionOut;
    }

    @Override
    public double[] getAction(int actionPos) {
        assert actionPos < getNumberActions();
        assert actionIndexToArr.containsKey(actionPos);
        ActionRow actionRow = actionIndexToArr.get(actionPos);
        if (actionRow == null) {
            logger.error("something is wrong!! actionRow is null on action {}", actionPos);
        }
        return actionRow.getArray();

    }

    private class ActionRow {

        private int window, kPeriod;
        private double risk, skew, kDefault, aDefault;


        public ActionRow(double[] arrayInput) {
            this.window = (int) arrayInput[WINDOWS_INDEX];
            this.risk = arrayInput[RISK_AVERSION_INDEX];
            this.skew = arrayInput[SKEW_PRICE_INDEX];
            this.kDefault = arrayInput[K_DEFAULT_INDEX];
            this.aDefault = arrayInput[A_DEFAULT_INDEX];
            this.kPeriod = (int) arrayInput[K_PERIOD_DEFAULT_INDEX];
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof ActionRow))
                return false;
            ActionRow actionRow = (ActionRow) o;
            return window == actionRow.window && Double.compare(actionRow.risk, risk) == 0
                    && Double.compare(actionRow.skew, skew) == 0 && Double.compare(actionRow.kDefault, kDefault) == 0
                    && Double.compare(actionRow.aDefault, aDefault) == 0 && kPeriod == actionRow.kPeriod;
        }

        @Override
        public int hashCode() {

            return Objects.hash(window, risk, skew, kDefault, aDefault, kPeriod);
        }

        public double[] getArray() {
            double[] output = new double[ACTION_COLUMNS];
            output[WINDOWS_INDEX] = window;
            output[RISK_AVERSION_INDEX] = risk;
            output[SKEW_PRICE_INDEX] = skew;
            output[K_DEFAULT_INDEX] = kDefault;
            output[A_DEFAULT_INDEX] = aDefault;
            output[K_PERIOD_DEFAULT_INDEX] = kPeriod;
            return output;

        }
    }

}
