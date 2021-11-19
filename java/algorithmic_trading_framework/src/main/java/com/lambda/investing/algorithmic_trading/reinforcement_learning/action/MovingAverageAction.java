package com.lambda.investing.algorithmic_trading.reinforcement_learning.action;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MovingAverageAction extends AbstractAction {

	public static Integer FAST_PERIODS_INDEX = 0;
	public static Integer SLOW_PERIODS_INDEX = 1;
	public static Integer CHANGESIDE_INDEX = 2;
	public static int SIZE_ARRAY_ACTION = 3;//0-4

	private int[] fastPeriods;
	private int[] slowPeriods;
	private int[] changeSides;

	private int numberOfActions;

	private BiMap<Integer, ActionRow> actionIndexToArr;

	public MovingAverageAction(int[] fastPeriods, int[] slowPeriods, int[] changeSides

	) {

		List<Integer> validInputsSize = new ArrayList<>();
		if (fastPeriods.length == 0) {
			fastPeriods = null;
		} else {
			validInputsSize.add(fastPeriods.length);
		}
		this.fastPeriods = fastPeriods;

		if (slowPeriods.length == 0) {
			slowPeriods = null;
		} else {
			validInputsSize.add(slowPeriods.length);
		}
		this.slowPeriods = slowPeriods;

		if (changeSides.length == 0) {
			changeSides = null;
		} else {
			validInputsSize.add(changeSides.length);
		}
		this.changeSides = changeSides;

		if (validInputsSize.size() > 0) {
			for (Integer length : validInputsSize) {
				if (this.numberOfActions == 0) {
					this.numberOfActions = length;
				} else {
					this.numberOfActions = this.numberOfActions * length;
				}
			}
		} else {
			logger.error("error initializing with 0 actions!!!");
			this.numberOfActions = 0;
		}
		actionIndexToArr = HashBiMap.create();

		//prefill the array
		fillCacheActions();
	}

	private void fillCacheActions() {
		int counter = 0;
		double[] inputArr = new double[SIZE_ARRAY_ACTION];
		for (int fastPeriodIndex = 0; fastPeriodIndex < fastPeriods.length; fastPeriodIndex++) {
			for (int slowPeriodIndex = 0; slowPeriodIndex < slowPeriods.length; slowPeriodIndex++) {
				for (int changeSideIndex = 0; changeSideIndex < changeSides.length; changeSideIndex++) {
					inputArr[FAST_PERIODS_INDEX] = fastPeriods[fastPeriodIndex];
					inputArr[SLOW_PERIODS_INDEX] = slowPeriods[slowPeriodIndex];
					inputArr[CHANGESIDE_INDEX] = changeSides[changeSideIndex];
					getAction(inputArr);
					counter++;

				}

			}
		}

		assert counter == getNumberActions();
		assert actionIndexToArr.size() == counter;
		System.out.println("MovingAverageAction has " + String.valueOf(actionIndexToArr.size()) + " actions");

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
		//						skewPricePct.length - ArrayUtils.indexOf(skewPricePct, skew) + 1, 1);
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

		private int fastPeriod, slowPeriod, changeSide;

		public ActionRow(int fastPeriod, int slowPeriod, int changeSide) {
			this.fastPeriod = fastPeriod;
			this.slowPeriod = slowPeriod;
			this.changeSide = changeSide;
		}

		public ActionRow(double[] arrayInput) {
			this.fastPeriod = (int) arrayInput[FAST_PERIODS_INDEX];
			this.slowPeriod = (int) arrayInput[SLOW_PERIODS_INDEX];
			this.changeSide = (int) arrayInput[CHANGESIDE_INDEX];

		}

		@Override public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof ActionRow))
				return false;
			ActionRow actionRow = (ActionRow) o;
			return fastPeriod == actionRow.fastPeriod && slowPeriod == actionRow.slowPeriod
					&& changeSide == actionRow.changeSide;
		}

		@Override public int hashCode() {

			return Objects.hash(fastPeriod, slowPeriod, changeSide);
		}

		public double[] getArray() {
			double[] output = new double[SIZE_ARRAY_ACTION];
			output[FAST_PERIODS_INDEX] = fastPeriod;
			output[SLOW_PERIODS_INDEX] = slowPeriod;
			output[CHANGESIDE_INDEX] = changeSide;
			return output;

		}
	}

}
