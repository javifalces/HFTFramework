package com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn;

import com.lambda.investing.algorithmic_trading.reinforcement_learning.action.AbstractAction;

public class TestAction extends AbstractAction {

	@Override public int getNumberActions() {
		return 3;
	}

	@Override public int getAction(double[] actionArr) {
		return 0;
	}

	@Override public double[] getAction(int actionPos) {
		return new double[0];
	}
}
