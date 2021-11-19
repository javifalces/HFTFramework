package com.lambda.investing.algorithmic_trading.reinforcement_learning.action;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

public class AvellanedaActionTest {

	AvellanedaAction avellanedaAction;

	public AvellanedaActionTest() {
		int[] windowsTicks = new int[] { 1, 5, 6 };
		double[] skewPricePct = new double[] { 1., 0.9, 1.1 };
		double[] riskAversion = new double[] { 0.1, 0.5, 0.9 };
		this.avellanedaAction = new AvellanedaAction(windowsTicks, skewPricePct, riskAversion);
	}

	@Test public void testGetActionIndex() {
		for (int repeat = 0; repeat < 5; repeat++) {
			System.out.println("Repeat " + repeat);
			int randomNum = ThreadLocalRandom.current().nextInt(0, this.avellanedaAction.getNumberActions());
			double[] actionValue = this.avellanedaAction.getAction(randomNum);
			int positionGet = this.avellanedaAction.getAction(actionValue);
			Assert.assertEquals(positionGet, randomNum);
		}

	}

}
