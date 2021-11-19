package com.lambda.investing.algorithmic_trading.reinforcement_learning.state;

import com.google.common.collect.BiMap;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreEnum;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

public class AbstractStateTest {

	PrivateState privateState;
	String cacheFile = "testcacheFile.csv";

	public AbstractStateTest() {
		ScoreEnum scoreEnum = ScoreEnum.realized_pnl;

		privateState = new PrivateState(scoreEnum, 1, 1, 0, 2, 0., 0.001);
		privateState.loadCacheFile(cacheFile);
	}

	@After public void tearDown() throws Exception {
		privateState.deleteCacheFile(cacheFile);
	}

	@Test public void testIncrement() {
		double[] inputArray = new double[] { 1.9, 2.0 };
		privateState.incrementArray(inputArray, 1);
		Assert.assertEquals(2.0, inputArray[0], 0.01);
		Assert.assertEquals(2.0, inputArray[1], 0.01);

		privateState.incrementArray(inputArray, 1);
		Assert.assertEquals(0.0, inputArray[0], 0.01);
		Assert.assertEquals(0.0, inputArray[1], 0.01);

		privateState.incrementArray(inputArray, -1);
		Assert.assertEquals(2.0, inputArray[0], 0.01);
		Assert.assertEquals(2.0, inputArray[1], 0.01);

		privateState.incrementArray(inputArray, -1);
		Assert.assertEquals(1.9, inputArray[0], 0.01);
		Assert.assertEquals(2.0, inputArray[1], 0.01);

		privateState.incrementArray(inputArray, -1);
		Assert.assertEquals(1.8, inputArray[0], 0.01);
		Assert.assertEquals(2.0, inputArray[1], 0.01);

	}

	@Test public void testGetStateIndex() {

		int numberStates = this.privateState.getNumberStates();
		Assert.assertEquals(numberStates, this.privateState.getStateIndexToArr().size());

		for (int retry = 0; retry < 5; retry++) {
			int randomNum = ThreadLocalRandom.current().nextInt(0, this.privateState.getNumberStates() + 1);
			double[] state = this.privateState.getState(randomNum);
			int stateGet = this.privateState.getStateFromArray(state);
			Assert.assertEquals(stateGet, randomNum);
		}

		double[] inputArray = new double[] { 1.9, 2.0 };
		int stateGet = this.privateState.getStateFromArray(inputArray);
		double[] inputArray2 = new double[] { 2.0, 1.9 };
		int stateGet2 = this.privateState.getStateFromArray(inputArray2);
		Assert.assertNotEquals(stateGet, stateGet2);

		double[] inputArray5 = new double[] { 2.0, 1.8 };
		int stateGet5 = this.privateState.getStateFromArray(inputArray5);
		Assert.assertNotEquals(stateGet2, stateGet5);

		double[] inputArray3 = new double[] { 2.0, 2.0 };
		int stateGet3 = this.privateState.getStateFromArray(inputArray3);
		Assert.assertNotEquals(stateGet3, stateGet2);
		Assert.assertEquals(440, stateGet3);

		double[] inputArray4 = new double[] { 0.0, 0.0 };
		int stateGet4 = this.privateState.getStateFromArray(inputArray4);
		Assert.assertEquals(0, stateGet4);

	}

	@Test public void testSaveFile() {
		String filepath = "testFileCache.csv";
		File file = new File(filepath);
		if (file.exists()) {
			file.delete();
		}

		Assert.assertFalse(file.exists());

		BiMap<Integer, AbstractState.StateRow> beforeCache = this.privateState.getStateIndexToArr();
		this.privateState.saveCacheFile(filepath);
		Assert.assertTrue(file.exists());
		BiMap<Integer, AbstractState.StateRow> readCache = AbstractState.loadMapFromFile(filepath);
		Assert.assertEquals(beforeCache.size(), readCache.size());
		this.privateState.loadCacheFile(filepath);
		this.privateState.deleteCacheFile(filepath);

		double[] inputArray3 = new double[] { 2.0, 2.0 };
		int stateGet3 = this.privateState.getStateFromArray(inputArray3);
		Assert.assertEquals(440, stateGet3);

		double[] inputArray4 = new double[] { 0.0, 0.0 };
		int stateGet4 = this.privateState.getStateFromArray(inputArray4);
		Assert.assertEquals(0, stateGet4);

	}
}
