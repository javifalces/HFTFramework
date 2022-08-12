package com.lambda.investing.algorithmic_trading.reinforcement_learning;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class Dl4JRegressionMemoryReplayModelTest {

	String path = this.getClass().getResource("/").getPath() + "Dl4jMemoryReplayModelTest.model";
	int epoch = 50;
	double learningRate = 0.01;

	Dl4jRegressionMemoryReplayModel dl4jMemoryReplayModel = new Dl4jRegressionMemoryReplayModel(path, learningRate, 0.5, epoch, 1, 200, 0,
			0, false);

	@Before
	public void setUp() throws Exception {
		dl4jMemoryReplayModel.loadModel();
	}

	private Dl4jRegressionMemoryReplayModel getAndModel(boolean forceTrain) {
		Dl4jRegressionMemoryReplayModel dl4jMemoryReplayModel = new Dl4jRegressionMemoryReplayModel(path, learningRate, 0.5, epoch, 1, 200,
				0, 0, false);

		if (!forceTrain && dl4jMemoryReplayModel.isTrained()) {
			System.out.println("returning model trained");
			return dl4jMemoryReplayModel;
		} else {
			//		MultiLayerNetwork andModel = dl4jMemoryReplayModel.createModel(2, 1, 1, learningRate, 0.0, 0, 0);
			double[][] input = {{0.0, 0.0}, {0.0, 1.0}, {1.0, 0.0}, {1.0, 1.0}};
			double[][] output = {{0.0}, {0.0}, {0.0}, {1.0}};
			dl4jMemoryReplayModel.train(input, output);
			return dl4jMemoryReplayModel;
		}
	}

	@Test @Ignore public void AndModel() {
		MultiLayerNetwork andModel = dl4jMemoryReplayModel.createModel(2, 1, 1, learningRate, 0.0, 0, 0);
		Assert.assertNotNull(andModel);
		double[][] input = { { 0.0, 0.0 }, { 0.0, 1.0 }, { 1.0, 0.0 }, { 1.0, 1.0 } };
		double[][] output = {{0.0}, {0.0}, {0.0}, {1.0}};
		Dl4jRegressionMemoryReplayModel model = getAndModel(true);
		for (int row = 0; row < input.length; row++) {

			double[] inputRow = input[row];
			double expectedOut = output[row][0];

			double[] outputTest = model.predict(inputRow);
			double outputPredict = outputTest[0];
			Assert.assertEquals(expectedOut, outputPredict, 0.5);
		}
		model.saveModel();

	}

	@Test @Ignore public void OrModelLearnOnTrainedAnd() {

		double[][] input = { { 0.0, 0.0 }, { 0.0, 1.0 }, { 1.0, 0.0 }, { 1.0, 1.0 } };

		Dl4jRegressionMemoryReplayModel model = getAndModel(false);

		double[][] andOutput = { { 0.0 }, { 0.0 }, { 0.0 }, { 1.0 } };
		double[][] orOutput = { { 0.0 }, { 1.0 }, { 1.0 }, { 1.0 } };
		boolean outputGradient = model.updateGradient(input, andOutput, orOutput);
		Assert.assertTrue(outputGradient);

		for (int row = 0; row < input.length; row++) {
			double[] inputRow = input[row];
			double expectedOut = orOutput[row][0];

			double[] outputTest = model.predict(inputRow);
			double outputPredict = outputTest[0];
			Assert.assertEquals(expectedOut, outputPredict, 0.5);
		}

	}

}
