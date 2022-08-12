package com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn;

import com.google.common.primitives.Doubles;
import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.Dl4jRegressionMemoryReplayModel;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.exploration_policy.EpsilonGreedyExploration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class DeepQLearningTest {

	TestState state = new TestState(5, new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
	TestAction testAction = new TestAction();

	IExplorationPolicy explorationPolicy = new EpsilonGreedyExploration(0.8);
	int maxBatchSize = 10;
	int batchSize = 10;
	double l2 = 0.;
	double l1 = 0.0;
	double learningRate = 0.95;
	double discountFactor = 0.25;
	String predictionPath = Configuration.TEMP_PATH + File.separator + "predictionJunit.model";
	String targetPath = Configuration.TEMP_PATH + File.separator + "targetJunit.model";

	Dl4jRegressionMemoryReplayModel predictionModel = new Dl4jRegressionMemoryReplayModel(predictionPath, learningRate, 0.9, 50, batchSize,
			maxBatchSize, l2, l1, false);
	Dl4jRegressionMemoryReplayModel targetModel = new Dl4jRegressionMemoryReplayModel(targetPath, learningRate, 0.9, 50, batchSize,
			maxBatchSize, l2, l1, false);

	DeepQLearning deepQLearning;

	int trainingPredictIterationPeriod = 0;
	int trainingTargetIterationPeriod = 0;
	Date currentTime = new Date();

	public DeepQLearningTest() {

		try {
			deepQLearning = new DeepQLearning(state, testAction, explorationPolicy, 10, predictionModel, targetModel,
					false, discountFactor, learningRate, this.trainingPredictIterationPeriod,
					this.trainingTargetIterationPeriod);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Before public void setUp() throws Exception {

		try {
			deepQLearning = new DeepQLearning(state, testAction, explorationPolicy, 10, predictionModel, targetModel,
					false, discountFactor, learningRate, this.trainingPredictIterationPeriod,
					this.trainingTargetIterationPeriod);
		} catch (Exception e) {
			e.printStackTrace();
		}

		int action = 0;
		double reward = 1.0;
		double[] previousStateArr = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
		state.setCurrentState(previousStateArr);
		deepQLearning.updateState(currentTime, previousStateArr, action, reward, state);

		action = 1;
		reward = 1.0;
		previousStateArr = new double[] { 2.0, 3.0, 4.0, 5.0, 6.0, 7.0 };
		state.setCurrentState(previousStateArr);
		deepQLearning.updateState(currentTime, previousStateArr, action, reward, state);

	}

	@Test public void updateScore() {

		double[] previousStateArr = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
		int indexOfState = deepQLearning.stateExistRow(previousStateArr);
		Assert.assertTrue(indexOfState >= 0);

		double[] qValue = deepQLearning.getRewards(previousStateArr);
		double bestScore = Doubles.max(qValue);
		Assert.assertEquals(qValue[0], bestScore, 0.001);
		Assert.assertEquals(1.0, bestScore, 0.001);

		int action = 0;
		double reward = 2.0;
		state.setCurrentState(previousStateArr);
		deepQLearning.updateState(currentTime, previousStateArr, action, reward, state);

		qValue = deepQLearning.getRewards(previousStateArr);
		bestScore = Doubles.max(qValue);
		Assert.assertTrue(qValue[0] <= reward);
		Assert.assertTrue(bestScore <= reward);

		action = 0;
		double reward2 = 8.0;
		state.setCurrentState(previousStateArr);
		deepQLearning.updateState(currentTime, previousStateArr, action, reward2, state);

		qValue = deepQLearning.getRewards(previousStateArr);
		bestScore = Doubles.max(qValue);
		Assert.assertTrue(qValue[0] <= reward2);
		Assert.assertTrue(qValue[0] >= reward);
		Assert.assertTrue(bestScore <= reward2);

	}


	@Test public void getStateIndex() {
		double[] previousStateArr = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
		int indexOfState = deepQLearning.stateExistRow(previousStateArr);
		Assert.assertTrue(indexOfState == 0);

		previousStateArr = new double[] { 2.0, 3.0, 4.0, 5.0, 6.0, 7.0 };
		indexOfState = deepQLearning.stateExistRow(previousStateArr);
		Assert.assertTrue(indexOfState == 1);

		double[] newState = new double[] { 3.0, 3.0, 4.0, 5.0, 6.0, 7.0 };
		indexOfState = deepQLearning.stateExistRow(newState);
		Assert.assertTrue(indexOfState < 0);

		int action = 0;
		double reward = 1.0;
		deepQLearning.updateState(currentTime, newState, action, reward, state);
		indexOfState = deepQLearning.stateExistRow(newState);
		Assert.assertTrue(indexOfState == 2);

		deepQLearning.updateState(currentTime, newState, action, reward, state);
		indexOfState = deepQLearning.stateExistRow(newState);
		Assert.assertTrue(indexOfState == 2);

	}

	@Test public void updateNewActionScore() {
		double[] previousStateArr = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
		double[] qValue = deepQLearning.getRewards(previousStateArr);
		double bestScore = Doubles.max(qValue);
		Assert.assertEquals(qValue[0], bestScore, 0.001);
		Assert.assertEquals(1.0, bestScore, 0.001);
		int action = 2;
		double reward = 2.0;
		deepQLearning.updateState(currentTime, previousStateArr, action, reward, state);

		double[] qValueUpdated = deepQLearning.getRewards(previousStateArr);
		double bestScoreFin = Doubles.max(qValueUpdated);
		Assert.assertTrue(reward == bestScoreFin);
		Assert.assertTrue(qValueUpdated[action] == bestScoreFin);
		Assert.assertTrue(bestScoreFin > bestScore);

		double newReward = -6.0;
		deepQLearning.updateState(currentTime, previousStateArr, action, newReward, state);
		qValueUpdated = deepQLearning.getRewards(previousStateArr);

		double bestScoreFinLess = Doubles.max(qValueUpdated);
		Assert.assertTrue(newReward < bestScoreFinLess);
		Assert.assertTrue(qValueUpdated[action] < bestScoreFinLess);
		Assert.assertTrue(qValueUpdated[action] >= newReward);

	}

	@Test public void updateNewActionScoreFilled() {

		double[] previousStateArr = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
		int indexOfState = deepQLearning.stateExistRow(previousStateArr);
		Assert.assertTrue(indexOfState == 0);
		int numberOfRows = deepQLearning.getMemoryReplaySize();
		Assert.assertEquals(2, numberOfRows);
		double firstValue = previousStateArr[0];
		double[] lastStateAdd = null;
		for (int i = numberOfRows; i < maxBatchSize; i++) {
			firstValue++;
			lastStateAdd = new double[] { firstValue, 2.0, 3.0, 4.0, 5.0, 6.0 };
			int action = 0;
			double reward = 6.0;
			deepQLearning.updateState(currentTime, lastStateAdd, action, reward, state);
			indexOfState = deepQLearning.stateExistRow(lastStateAdd);
			Assert.assertTrue(indexOfState >= 0);
			Assert.assertTrue(deepQLearning.getMemoryReplaySize() == i + 1);
			Assert.assertArrayEquals(deepQLearning.getState(indexOfState), lastStateAdd, 0.001);

		}

		int numberRowsLater = deepQLearning.getMemoryReplaySize();
		Assert.assertTrue(numberRowsLater > numberOfRows);
		Assert.assertEquals(maxBatchSize, numberRowsLater);
		indexOfState = deepQLearning.stateExistRow(lastStateAdd);
		Assert.assertTrue(indexOfState == maxBatchSize - 1);

		//still here
		previousStateArr = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
		indexOfState = deepQLearning.stateExistRow(previousStateArr);
		Assert.assertTrue(indexOfState == 0);

		//new row will removed the previous
		double[] newStateOnFirstIndex = new double[] { 1.0, 1.0, 3.0, 4.0, 5.0, 6.0 };
		int action = 1;
		double reward = 7.0;
		deepQLearning.updateState(currentTime, newStateOnFirstIndex, action, reward, state);
		int numberRowsLater2 = deepQLearning.getMemoryReplaySize();
		Assert.assertEquals(maxBatchSize, numberRowsLater2);
		indexOfState = deepQLearning.stateExistRow(previousStateArr);
		Assert.assertTrue(indexOfState < 0);

		indexOfState = deepQLearning.stateExistRow(newStateOnFirstIndex);
		Assert.assertTrue(indexOfState == 0);

		indexOfState = deepQLearning.stateExistRow(lastStateAdd);
		Assert.assertTrue(indexOfState == maxBatchSize - 1);

	}

	@Test public void checkLoadMemoryIsWorkingAsExpected() throws IOException {
		String memoryPathTemp = Configuration.TEMP_PATH + File.separator + "memoryReplayJunit.csv";
		this.deepQLearning.saveMemory(memoryPathTemp);
		DeepQLearning newLoadedDeepQLearning = null;
		try {
			DeepQLearning.SHUFFLE_LOADING_ROWS = false;
			newLoadedDeepQLearning = new DeepQLearning(state, testAction, explorationPolicy, 10, predictionModel,
					targetModel, false, discountFactor, learningRate, this.trainingPredictIterationPeriod,
					this.trainingTargetIterationPeriod);
			double[] previousStateArr = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
			int indexOfState = newLoadedDeepQLearning.stateExistRow(previousStateArr);
			Assert.assertTrue(indexOfState < 0);

			newLoadedDeepQLearning.loadMemory(memoryPathTemp);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//check states are found
		double[] previousStateArr = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
		int indexOfState = newLoadedDeepQLearning.stateExistRow(previousStateArr);
		Assert.assertTrue(indexOfState == 0);

		previousStateArr = new double[] { 2.0, 3.0, 4.0, 5.0, 6.0, 7.0 };
		indexOfState = newLoadedDeepQLearning.stateExistRow(previousStateArr);
		Assert.assertTrue(indexOfState == 1);

		previousStateArr[0] = 66;
		indexOfState = newLoadedDeepQLearning.stateExistRow(previousStateArr);
		Assert.assertTrue(indexOfState < 0);

	}

	@Test public void checkNewRow() throws IOException {

		DeepQLearning newLoadedDeepQLearning = null;
		try {
			DeepQLearning.SHUFFLE_LOADING_ROWS = false;
			newLoadedDeepQLearning = new DeepQLearning(state, testAction, explorationPolicy, 3, predictionModel,
					targetModel, false, discountFactor, learningRate, this.trainingPredictIterationPeriod,
					this.trainingTargetIterationPeriod);

			newLoadedDeepQLearning.asQLearn = true;

			double[] previousStateArr = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
			int indexOfState = newLoadedDeepQLearning.stateExistRow(previousStateArr);
			Assert.assertTrue(indexOfState < 0);

			newLoadedDeepQLearning.updateState(currentTime, previousStateArr, 0, 1.0, state);
			Assert.assertEquals(0, newLoadedDeepQLearning.stateExistRow(previousStateArr));
			Assert.assertTrue(newLoadedDeepQLearning.getMemoryReplayIndex() == 1);

			double[] previousStateArr2 = new double[] { 0.0, 0.0, 1.0, 2.0, 3.0, 6.0 };
			newLoadedDeepQLearning.updateState(currentTime, previousStateArr2, 0, 1.0, state);
			Assert.assertEquals(0, newLoadedDeepQLearning.stateExistRow(previousStateArr));
			Assert.assertEquals(1, newLoadedDeepQLearning.stateExistRow(previousStateArr2));
			Assert.assertTrue(newLoadedDeepQLearning.getMemoryReplayIndex() == 2);

			double[] previousStateArr3 = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
			newLoadedDeepQLearning.updateState(currentTime, previousStateArr3, 0, 1.0, state);
			Assert.assertEquals(0, newLoadedDeepQLearning.stateExistRow(previousStateArr));
			Assert.assertEquals(1, newLoadedDeepQLearning.stateExistRow(previousStateArr2));
			Assert.assertEquals(2, newLoadedDeepQLearning.stateExistRow(previousStateArr3));
			Assert.assertTrue(newLoadedDeepQLearning.getMemoryReplayIndex() == 0);

			newLoadedDeepQLearning.updateState(currentTime, previousStateArr3, 1, 2.0, state);
			Assert.assertEquals(0, newLoadedDeepQLearning.stateExistRow(previousStateArr));
			Assert.assertEquals(1, newLoadedDeepQLearning.stateExistRow(previousStateArr2));
			Assert.assertEquals(2, newLoadedDeepQLearning.stateExistRow(previousStateArr3));
			Assert.assertTrue(newLoadedDeepQLearning.getMemoryReplayIndex() == 0);
			double[] actionRewards = newLoadedDeepQLearning.getPredictOutput(previousStateArr3);
			Assert.assertEquals(1, actionRewards[0], 0.001);
			Assert.assertEquals(2, actionRewards[1], 0.5);

			double[] previousStateArr4 = new double[] { -1.0, -1.0, -1.0, -1.0, -1.0, -1.0 };
			newLoadedDeepQLearning.updateState(currentTime, previousStateArr4, 0, 1.0, state);
			Assert.assertEquals(-1, newLoadedDeepQLearning.stateExistRow(previousStateArr));
			Assert.assertEquals(1, newLoadedDeepQLearning.stateExistRow(previousStateArr2));
			Assert.assertEquals(2, newLoadedDeepQLearning.stateExistRow(previousStateArr3));
			Assert.assertEquals(0, newLoadedDeepQLearning.stateExistRow(previousStateArr4));
			Assert.assertTrue(newLoadedDeepQLearning.getMemoryReplayIndex() == 1);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
