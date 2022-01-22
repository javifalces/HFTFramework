package com.lambda.investing.algorithmic_trading.reinforcement_learning.action;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AvellanedaAction extends AbstractAction {

	public static Integer WINDOWS_INDEX = 0;
	public static Integer RISK_AVERSION_INDEX = 1;
	public static Integer SKEW_PRICE_INDEX = 2;

	private int[] windowsTick;
	private double[] riskAversion;
	private double[] skewPricePct;

	private int numberOfActions;

	private BiMap<Integer, ActionRow> actionIndexToArr;

	public AvellanedaAction(int[] windowsTick, double[] riskAversion, double[] skewPricePct) {

		List<Integer> validInputsSize = new ArrayList<>();
		if (windowsTick.length == 0) {
			windowsTick = null;
		} else {
			validInputsSize.add(windowsTick.length);
		}
		this.windowsTick = windowsTick;

		if (riskAversion.length == 0) {
			riskAversion = null;
		} else {
			validInputsSize.add(riskAversion.length);
		}
		this.riskAversion = riskAversion;

		if (skewPricePct.length == 0) {
			skewPricePct = null;
		} else {
			validInputsSize.add(skewPricePct.length);
		}
		this.skewPricePct = skewPricePct;

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
		double[] inputArr = new double[3];
		for (int windowIndex = 0; windowIndex < windowsTick.length; windowIndex++) {
			for (int riskIndex = 0; riskIndex < riskAversion.length; riskIndex++) {
				for (int skewIndex = 0; skewIndex < skewPricePct.length; skewIndex++) {
					//					double[] inputArr = new double[3];
					inputArr[WINDOWS_INDEX] = windowsTick[windowIndex];
					inputArr[RISK_AVERSION_INDEX] = riskAversion[riskIndex];
					inputArr[SKEW_PRICE_INDEX] = skewPricePct[skewIndex];
					getAction(inputArr);
					counter++;
				}
			}
		}
		assert counter == getNumberActions();
		assert actionIndexToArr.size() == counter;
		System.out.println("AvellanedaAction has " + String.valueOf(actionIndexToArr.size()) + " actions");
	}

	@Override public int getNumberActions() {
		return this.numberOfActions;
	}

	@Override public int getAction(double[] actionArr) {
		assert actionArr.length == 3;
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
		ActionRow actionRow = actionIndexToArr.get(actionPos);
		if (actionRow == null) {
			logger.error("something is wrong!! actionRow is null on action {}", actionPos);
		}
		return actionRow.getArray();

	}

	private class ActionRow {

		private int window;
		private double risk, skew;

		public ActionRow(int window, double risk, double skew) {
			this.window = window;
			this.risk = risk;
			this.skew = skew;
		}

		public ActionRow(double[] arrayInput) {
			this.window = (int) arrayInput[WINDOWS_INDEX];
			this.risk = arrayInput[RISK_AVERSION_INDEX];
			this.skew = arrayInput[SKEW_PRICE_INDEX];
		}

		@Override public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof ActionRow))
				return false;
			ActionRow actionRow = (ActionRow) o;
			return window == actionRow.window && Double.compare(actionRow.risk, risk) == 0
					&& Double.compare(actionRow.skew, skew) == 0;
		}

		@Override public int hashCode() {

			return Objects.hash(window, risk, skew);
		}

		public double[] getArray() {
			double[] output = new double[3];
			output[WINDOWS_INDEX] = window;
			output[RISK_AVERSION_INDEX] = risk;
			output[SKEW_PRICE_INDEX] = skew;
			return output;

		}
	}

}
