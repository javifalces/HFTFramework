package com.lambda.investing.algorithmic_trading.reinforcement_learning.action;

import com.lambda.investing.algorithmic_trading.reinforcement_learning.MatrixRoundUtils;
import lombok.Getter;
import lombok.Setter;

@Getter public class ActionColumn extends AbstractAction {

	double maxValue, minValue;
	int numDecimals, numberOfPossibilites;

	public ActionColumn(double maxValue, double minValue, int numDecimals) {
		this.maxValue = maxValue;
		this.minValue = minValue;
		this.numDecimals = numDecimals;
		this.numberOfPossibilites = MatrixRoundUtils.getValuesPerColumn(this.numDecimals, this.maxValue, this.minValue);

	}

	@Override public int getNumberActions() {
		return MatrixRoundUtils.getNumberStates(this.numberOfPossibilites, 1);
	}

	@Override public int getAction(double[] actionArr) {
		//TODO
		return -1;
	}

	@Override public double[] getAction(int actionPos) {
		//TODO
		return new double[0];
	}
}



