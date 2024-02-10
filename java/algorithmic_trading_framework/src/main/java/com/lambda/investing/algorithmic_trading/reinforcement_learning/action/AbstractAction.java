package com.lambda.investing.algorithmic_trading.reinforcement_learning.action;

import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.AbstractState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractAction {

	protected Logger logger = LogManager.getLogger(AbstractState.class);

	public int getNumberActionColumns() {
		return 1;
	}

	public abstract int getNumberActions();

	public abstract int getAction(double[] actionArr);

	public abstract double[] getAction(int actionPos);

}
