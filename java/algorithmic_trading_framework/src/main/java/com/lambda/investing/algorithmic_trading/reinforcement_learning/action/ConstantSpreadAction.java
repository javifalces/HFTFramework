package com.lambda.investing.algorithmic_trading.reinforcement_learning.action;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConstantSpreadAction extends AbstractAction {

	public static Integer LEVEL_INDEX = 0;
	public static Integer SKEW_LEVEL_INDEX = 1;
	public static int SIZE_ARRAY_ACTION = 2;//0-4

	private int[] levels;
	private int[] skewLevels;

	private int numberOfActions;

	private BiMap<Integer, ActionRow> actionIndexToArr;

	public ConstantSpreadAction(int[] levels, int[] skewLevels) {

		List<Integer> validInputsSize = new ArrayList<>();
		if (levels.length == 0) {
			levels = null;
		} else {
			validInputsSize.add(levels.length);
		}
		this.levels = levels;

		if (skewLevels.length == 0) {
			skewLevels = null;
		} else {
			validInputsSize.add(skewLevels.length);
		}
		this.skewLevels = skewLevels;

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
		for (int level = 0; level < levels.length; level++) {
			for (int skewLevel = 0; skewLevel < skewLevels.length; skewLevel++) {
				inputArr[LEVEL_INDEX] = levels[level];
				inputArr[SKEW_LEVEL_INDEX] = skewLevels[skewLevel];
				getAction(inputArr);
				counter++;
			}
		}

		assert counter == getNumberActions();
		assert actionIndexToArr.size() == counter;
		System.out.println("ConstantSpreadAction has " + String.valueOf(actionIndexToArr.size()) + " actions");

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

		private int level, skewLevel;

		public ActionRow(int level, int skewLevel) {
			this.level = level;
			this.skewLevel = skewLevel;
		}

		public ActionRow(double[] arrayInput) {
			this.level = (int) arrayInput[LEVEL_INDEX];
			this.skewLevel = (int) arrayInput[SKEW_LEVEL_INDEX];
		}

		@Override public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof ActionRow))
				return false;
			ActionRow actionRow = (ActionRow) o;
			return level == actionRow.level && skewLevel == actionRow.skewLevel;
		}

		@Override public int hashCode() {
			return Objects.hash(level, skewLevel);
		}

		public double[] getArray() {
			double[] output = new double[SIZE_ARRAY_ACTION];
			output[LEVEL_INDEX] = level;
			output[SKEW_LEVEL_INDEX] = skewLevel;
			return output;

		}
	}

}
