package com.lambda.investing.algorithmic_trading.reinforcement_learning.action;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

public class ConstantSpreadActionTest {

	ConstantSpreadAction constantSpreadAction;

	public ConstantSpreadActionTest() {
		int[] levels = new int[]{1, 2};
		int[] skewLevels = new int[]{0, 1};

		this.constantSpreadAction = new ConstantSpreadAction(levels, skewLevels);
	}

	@Test
	public void testGetActionIndex() {
		for (int repeat = 0; repeat < 5; repeat++) {
			System.out.println("Repeat " + repeat);
			int randomNum = ThreadLocalRandom.current().nextInt(0, this.constantSpreadAction.getNumberActions());
			double[] actionValue = this.constantSpreadAction.getAction(randomNum);
			int positionGet = this.constantSpreadAction.getAction(actionValue);
			Assert.assertEquals(positionGet, randomNum);
		}

	}

}
