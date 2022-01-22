package com.lambda.investing.algorithmic_trading.reinforcement_learning;

import com.google.common.primitives.Doubles;
import com.lambda.investing.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.scorecalc.RegressionScoreCalculator;
import org.deeplearning4j.earlystopping.termination.EpochTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxTimeIterationTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.nd4j.common.primitives.Pair;
import org.nd4j.evaluation.regression.RegressionEvaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.activations.IActivation;
import org.nd4j.linalg.activations.impl.ActivationIdentity;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.MiniBatchFileDataSetIterator;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.Normalizer;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.lossfunctions.impl.LossMSE;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.deeplearning4j.util.ModelSerializer.restoreMultiLayerNetworkAndNormalizer;
import static org.deeplearning4j.util.ModelSerializer.writeModel;

/***
 * https://www.baeldung.com/deeplearning4j
 * https://deeplearning4j.konduit.ai/models/layers
 *
 */
public class Dl4jMemoryReplayModel implements MemoryReplayModel, Cloneable {

	public static boolean CHECK_BEST_EARLY = false;
	private static int MAX_BATCH_SIZE = 5000;
	public static double[] LEARNING_RATES_HYPERPARAMETER = new double[] { 0.00001, 0.0001, 0.001, 0.01 };
	public static double[] HIDDEN_SIZE_NODES_MULTIPLIER_HYPERPARAMETER = new double[] { 2 };
	public static int[] EPOCHS_HYPERPARAMETER = new int[] { 100 };
	public static double[] L1_HYPERPARAMETER = new double[] { 0.0, 0.1, 0.01, 0.001 };
	public static double[] L2_HYPERPARAMETER = new double[] { 0.0, 0.1, 0.01, 0.001 };
	public static double[] MOMENTUM_HYPERPARAMETER = new double[] { 0.5, 0.8 };
	public static boolean HYPERPARAMETER_TUNING = false;
	private static int COUNTER_RE_TRAIN = 0;
	private int maxMinutesTraining = 15;
	private int defaultMaxMinutesTraining = maxMinutesTraining;
	//	https://deeplearning4j.konduit.ai/models/layers
	public static boolean EARLY_STOPPING = false;//automatize learningRate to avoid overfitting or underfitting
	public static boolean CLEAN_NN_IF_EXIST = false;

	protected static int EARLY_STOPPING_TOLERANCE_DECIMALS = 3;
	protected Logger logger = LogManager.getLogger(Dl4jMemoryReplayModel.class);
	private String modelPath;

	protected double learningRate, momentumNesterov;
	protected int nEpoch = 100;

	private int batchSize, maxBatchSize;
	private double l2, l1;

	private Normalizer dataNormalization;
	protected MultiLayerNetwork model = null;

	protected boolean isTrained = false;
	protected boolean trainingStats = false;
	ScoreIterationListener scoreIterationListener = new ScoreIterationListener(10);
	protected boolean isRNN = false;
	protected long seed;
	protected int hiddenSizeNodesMultiplier = 2;

	protected ILossFunction lossFunction = new LossMSE();
	protected IActivation activationFunction = new ActivationIdentity();
	;

	public void setTrainingStats(boolean trainingStats) {
		this.trainingStats = trainingStats;
	}

	public Dl4jMemoryReplayModel(String modelPath, double learningRate, double momentumNesterov, int nEpoch,
			int batchSize, int maxBatchSize, double l2, double l1, boolean isRNN) {
		this.modelPath = modelPath;
		this.learningRate = learningRate;
		this.momentumNesterov = momentumNesterov;
		this.nEpoch = nEpoch;
		this.maxBatchSize = maxBatchSize;
		this.batchSize = batchSize;
		this.l2 = l2;
		this.l1 = l1;
		this.isRNN = isRNN;
		this.seed = (int) System.currentTimeMillis();
		loadModel();
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	@Override public long getSeed() {
		return seed;
	}

	@Override public int getEpoch() {
		return nEpoch;
	}

	public Dl4jMemoryReplayModel(String modelPath, double learningRate, double momentumNesterov, int nEpoch,
			int batchSize, int maxBatchSize, double l2, double l1, boolean loadModel, boolean isRNN) {
		this.modelPath = modelPath;
		this.learningRate = learningRate;
		this.momentumNesterov = momentumNesterov;
		this.nEpoch = nEpoch;
		this.maxBatchSize = maxBatchSize;
		this.batchSize = batchSize;
		this.l2 = l2;
		this.l1 = l1;
		this.isRNN = isRNN;
		if (loadModel) {
			loadModel();
		}
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public void setMaxBatchSize(int maxBatchSize) {
		this.maxBatchSize = maxBatchSize;
	}

	public void setModelPath(String modelPath) {
		this.modelPath = modelPath;
	}

	@Override public String getModelPath() {
		return modelPath;
	}

	public void loadModel() {
		File savedModelFile = new File(this.modelPath);
		if (!savedModelFile.exists()) {
			logger.info("model not found to load {}", this.modelPath);
			model = null;
			isTrained = false;
			return;
		} else {
			long start = System.currentTimeMillis();

			try {
				Pair<MultiLayerNetwork, Normalizer> pair = restoreMultiLayerNetworkAndNormalizer(savedModelFile, true);
				this.model = pair.getFirst();
				this.dataNormalization = pair.getSecond();
				//				this.model = MultiLayerNetwork.load(savedModelFile, true);
				isTrained = true;
				long elapsed = (System.currentTimeMillis() - start) / (1000);
				logger.info("loaded model {}", this.modelPath);
				logger.info("loaded in {} seconds , model {}", elapsed, this.modelPath);
			} catch (IOException e) {
				System.err.println(String.format("cant load model %s -> GPU memory?", this.modelPath));
				logger.error("cant load model %s => GPU memory?", this.modelPath, e.getMessage());
			}
		}

	}

	public void saveModel() {
		File savedModelFile = new File(this.modelPath);
		try {
			//			this.model.save(savedModelFile,true);
			writeModel(this.model, savedModelFile, true, (DataNormalization) this.dataNormalization);
			System.out.println(String.format("saved model %s", this.modelPath));
			logger.info(String.format("saved model %s", this.modelPath));
		} catch (IOException e) {
			System.err.println(String.format("cant save model %s", this.modelPath));
			logger.error("cant save model ", e);
		}

	}

	@Override public boolean isTrained() {
		return isTrained;
	}

	@Override public int getMaxBatchSize() {
		return maxBatchSize;
	}

	@Override public int getBatchSize() {
		return batchSize;
	}

	protected IUpdater getUpdater(double momentumNesterov, double learningRate) {
		if (momentumNesterov > 0) {
			return new Nesterovs(learningRate, momentumNesterov);
		} else {
			return new Adam.Builder().learningRate(learningRate).build();
		}
	}

	private MultiLayerConfiguration createRNNModel(int numInputs, int numHiddenNodes, int numOutputs,
			double learningRate, double momentumNesterov, double l1, double l2) {

		// https://deeplearning4j.konduit.ai/models/recurrent
		if (momentumNesterov == 0) {

		}
		int secondLayerNodes = numInputs / 2;
		WeightInit weightInit = WeightInit.XAVIER_UNIFORM;//a form of Gaussian distribution (WeightInit.XAVIER),
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.updater(getUpdater(momentumNesterov, learningRate)).l2(l2).l1(l1)

				//input layer
				.list().layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes).weightInit(weightInit)
						.activation(Activation.SIGMOID).build()).

				//hidden layer 1
				//						layer(1, new LSTM.Builder().nIn(numHiddenNodes).nOut(secondLayerNodes).weightInit(weightInit)
				//						.activation(Activation.SIGMOID).build()).
				//hidden layer 2
				//						layer(2, new LSTM.Builder().nIn(numHiddenNodes).nOut(numOutputs).weightInit(weightInit)
				//						.activation(Activation.SIGMOID).build()).

				//output layer
						layer(1,
						new OutputLayer.Builder(lossFunction).weightInit(weightInit).activation(activationFunction)
								.weightInit(weightInit).nIn(secondLayerNodes).nOut(numOutputs).
								build()).build();
		//The above code snippet will cause any network training (i.e., calls to MultiLayerNetwork.fit() methods) to use truncated BPTT with segments of length 100 steps.
		return conf;
	}

	private MultiLayerConfiguration createFeedForwardModel(int numInputs, int numHiddenNodes, int numOutputs,
			double learningRate, double momentumNesterov, double l1, double l2) {
		WeightInit weightInit = WeightInit.XAVIER_UNIFORM;//a form of Gaussian distribution (WeightInit.XAVIER),
		int secondLayerNodes = numInputs / 2;

		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.updater(getUpdater(momentumNesterov, learningRate)).l2(l2).l1(l1)
				//				.updater((new Adam.Builder().learningRate(learningRate)).build()).l2(l2).l1(l1)

				//input layer
				.list().layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes).weightInit(weightInit)
						.activation(Activation.SIGMOID).build()).
				//hidden layer 1
				//						layer(1,
				//						new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes).weightInit(weightInit)
				//								.activation(Activation.SIGMOID).build()).
				//hidden layer 2
				//						layer(2,
				//						new DenseLayer.Builder().nIn(secondLayerNodes).nOut(secondLayerNodes).weightInit(weightInit)
				//								.activation(Activation.RELU).build()).
				//output layer
						layer(1,
						new OutputLayer.Builder(lossFunction).activation(activationFunction).weightInit(weightInit)
								.weightInit(weightInit).nIn(numHiddenNodes).nOut(numOutputs).build())

				.build();
		return conf;
	}

	public MultiLayerNetwork createModel(int numInputs, int numHiddenNodes, int numOutputs, double learningRate,
			double momentumNesterov, double l1, double l2) {
		logger.info(
				"Creating nn model inputs:{}  hiddenNodes:{}  outputs:{}  learningRate:{}  momentumNesterov:{} l1:{} l2:{}",
				numInputs, numHiddenNodes, numOutputs, learningRate, momentumNesterov, l1, l2);
		MultiLayerConfiguration conf = null;
		if (isRNN) {
			conf = createRNNModel(numInputs, numHiddenNodes, numOutputs, learningRate, momentumNesterov, l1, l2);
		} else {
			conf = createFeedForwardModel(numInputs, numHiddenNodes, numOutputs, learningRate, momentumNesterov, l1,
					l2);
		}
		conf.setBackpropType(BackpropType.Standard);

		MultiLayerNetwork model = new MultiLayerNetwork(conf);
		model.init();

		return model;
	}

	protected EarlyStoppingConfiguration getEarlyStoppingConfiguration(DataSetIterator testDataIterator) {
		EarlyStoppingConfiguration earlyStoppingConfiguration = new EarlyStoppingConfiguration.Builder()
				.iterationTerminationConditions(
						new MaxTimeIterationTerminationCondition(maxMinutesTraining, TimeUnit.MINUTES))
				.epochTerminationConditions(new MaxEpochsTerminationCondition(nEpoch)).scoreCalculator(
						//								new DataSetLossCalculator(testDataIterator,true),
						new RegressionScoreCalculatorRounded(RegressionEvaluation.Metric.R2, testDataIterator,
								EARLY_STOPPING_TOLERANCE_DECIMALS))
				//						.modelSaver(new LocalFileModelSaver(this.modelPath))
				.evaluateEveryNEpochs(5).
						build();
		return earlyStoppingConfiguration;
	}

	protected Gradient getGradient(double[] input, double[] output, double[] expected) {
		//TODO
		////https://github.com/deeplearning4j/rl4j/blob/f6d9ff674c6d8e4c4de812dbd7469d8a01b78e26/rl4j-core/src/main/java/org/deeplearning4j/rl4j/network/ac/ActorCriticSeparate.java#L102
		INDArray expectedArr = Nd4j.create(expected);//what we should have
		INDArray outputArr = Nd4j.create(output);

		Pair<Double, INDArray> tuple = lossFunction
				.computeGradientAndScore(expectedArr, outputArr, activationFunction, this.model.getMask(), true);

		//		return tuple.getRight();

		INDArray inputArr = Nd4j.create(input);
		model.setInput(inputArr);
		model.setLabels(expectedArr);
		model.computeGradientAndScore();
		Collection<TrainingListener> valueIterationListeners = model.getListeners();
		if (valueIterationListeners != null && valueIterationListeners.size() > 0) {
			for (TrainingListener l : valueIterationListeners) {
				l.onGradientCalculation(model);
			}
		}

		return model.gradient();

	}

	protected void backpropGradients(Gradient gradient, int batchSize) {
		//TODO
		MultiLayerConfiguration valueConf = model.getLayerWiseConfigurations();
		int valueIterationCount = valueConf.getIterationCount();
		int valueEpochCount = valueConf.getEpochCount();
		model.getUpdater().update(model, gradient, valueIterationCount, valueEpochCount, batchSize,
				LayerWorkspaceMgr.noWorkspaces());
		model.params().subi(gradient.gradient());
		Collection<TrainingListener> valueIterationListeners = model.getListeners();
		if (valueIterationListeners != null && valueIterationListeners.size() > 0) {
			for (TrainingListener listener : valueIterationListeners) {
				listener.iterationDone(model, valueIterationCount, valueEpochCount);
			}
		}
		valueConf.setIterationCount(valueIterationCount + 1);

	}

	/**
	 * * 	 * # Compute loss
	 * * 	 * loss = F.mse_loss(Q_expected, Q_targets)
	 * * 	 * # Minimize the loss
	 * * 	 * self.optimizer.zero_grad()
	 * * 	 * loss.backward()
	 * * 	 * self.optimizer.step()
	 * * 	 * <p>
	 * * 	 * # Update target network
	 * * 	 * self.soft_update(self.qnetwork_local, self.qnetwork_target, TAU)
	 *
	 * @param input
	 * @param expected
	 */
	@Override public boolean updateGradient(double[][] input, double[][] output, double[][] expected) {
		//https://github.com/deeplearning4j/rl4j/blob/f6d9ff674c6d8e4c4de812dbd7469d8a01b78e26/rl4j-core/src/main/java/org/deeplearning4j/rl4j/network/ac/ActorCriticSeparate.java#L102
		//		double[][] expectedMatrix = new double [][]{expected};
		//		double[][] targetMatrix = new double [][]{targets};
		try {

			//apply gradients of output/expected
			//			for(int row =0;row<input.length;row++) {
			//				double[] inputRow = input[row];
			//				double[] outputRow = output[row];
			//				double[] expectedRow=expected[row];
			//
			//				Gradient gradients = getGradient(inputRow,outputRow, expectedRow);
			//				backpropGradients(gradients, this.batchSize);
			//			}

			//Train directly from inputs to outputs
			return this.train(input, expected);
		} catch (Exception e) {
			logger.error("uknown error calculating gradient ", e);
			return false;
		}
	}

	protected boolean evalSet(DataSetIterator dataSetIterator) {
		RegressionEvaluation eval = model.evaluateRegression(dataSetIterator);
		System.out.println(eval.stats());
		if (Double.isFinite(eval.averageMeanAbsoluteError())) {
			isTrained = true;
		} else {
			System.out.println(
					"training was not okey---> please consider shutdown some other java process or clean gpu memory");
			logger.error("something was wrong training when {} is not finite", eval.averageMeanAbsoluteError());
			logger.error(eval.stats());
			return false;
			//					System.out.println("clear previous nn");
			//					this.model.clear();
			//					train(input,target);
			//					return;
		}
		return true;
	}

	@Override public boolean train(double[][] input, double[][] target) {

		INDArray x = Nd4j.create(input);
		INDArray y = Nd4j.create(target);

		DataSet allData = new DataSet(x, y);

		try {
			if (this.model == null) {
				int numInput = input[0].length;
				int numOutput = target[0].length;
				int hiddenNodes = (numInput + numOutput);
				//				Hyperparameter tuning finished in 3978 seconds with 3 iterations  with score=0.04  with epochs=750 learningRate=0.1 and hiddenNodesSizeMultiplier=2.0  l1=0.0  l2=0.0
				int hiddenNodesModel = hiddenSizeNodesMultiplier * hiddenNodes;
				String entryMessage = "Creating new empty FeedForward nn";
				if (isRNN) {
					entryMessage = "Creating new empty RNN nn";

				}
				String message = Configuration.formatLog(
						"{} inputNodes:{}  hiddenNodes:{} outputNodes:{}  learningRate:{} momentumNesterov:{}  l1:{} l2:{}",
						entryMessage, numInput, hiddenNodesModel, numOutput, learningRate, momentumNesterov, l1, l2);
				System.out.println(message);

				this.model = createModel(numInput, hiddenNodesModel, numOutput, learningRate, momentumNesterov, l1, l2);

			} else {
				//clear the model
				if (CLEAN_NN_IF_EXIST) {
					System.out.println("clear previous nn");
					this.model.clear();
				}
			}

			model.addListeners(scoreIterationListener);  //Print score every 10 parameter updates
			if (this.trainingStats) {
				UIServer uiServer = UIServer.getInstance();
				StatsStorage statsStorage = new InMemoryStatsStorage();
				//			StatsStorage statsStorage = new FileStatsStorage();//in case of memory restrictions
				logger.info("starting training nn UI at localhost:9000");
				System.out.println("starting training nn UI  at localhost:9000");
				model.addListeners(new StatsListener(statsStorage));
				uiServer.attach(statsStorage);
			}
			//			if (allData.numExamples() < 100) {
			//				System.out.println("not enough data! not training " + allData.numExamples() + " samples");
			//				logger.info("skipping training with {} samples ",allData.numExamples());
			//				return;
			//			}
			EarlyStoppingConfiguration earlyStoppingConfiguration = null;
			double defaultLearningRate = learningRate;
			int defaultEpoch = nEpoch;

			//get maxBatchSizeData shuffled!
			allData.shuffle(seed);//// mix before split
			int maxBatchSizeTemp = Math.min(this.maxBatchSize, allData.numExamples());//it doesn't matter here
			allData = allData.sample(Math.min(this.maxBatchSize, maxBatchSizeTemp));

			if (EARLY_STOPPING) {
				System.out.println("starting training nn with early stop " + allData.numExamples() + " samples");

				double fractionTrainSplit = 0.75; //Use 75% of data for training
				SplitTestAndTrain testAndTrain = allData.splitTestAndTrain(fractionTrainSplit);

				DataSet trainingData = testAndTrain.getTrain();
				DataSet testData = testAndTrain.getTest();

				dataNormalization = new NormalizerStandardize();
				((NormalizerStandardize) dataNormalization).fit(trainingData);
				//				((NormalizerStandardize) dataNormalization).transform(trainingData);
				//				((NormalizerStandardize) dataNormalization).transform(testData);

				int trainSizeData = trainingData.numExamples();
				int testSizeData = testData.numExamples();//(int) (sizeData*(1-fractionTrainSplit));

				int batchSizeTempTest = Math.min(this.batchSize, MAX_BATCH_SIZE);//at least 500 iterations per epoch
				int batchSizeTempTrain = (int) Math
						.min(this.batchSize, MAX_BATCH_SIZE);        //at least 500 iterations per epoch

				List<DataSet> testDataIteratorlistDs = testData.asList();
				Collections.shuffle(testDataIteratorlistDs, new Random(seed));
				DataSetIterator testDataIterator = new ListDataSetIterator<>(testDataIteratorlistDs, batchSizeTempTest);

				List<DataSet> trainDataIteratorlistDs = trainingData.asList();
				Collections.shuffle(trainDataIteratorlistDs, new Random(seed));
				DataSetIterator trainDataIterator = new ListDataSetIterator<>(trainDataIteratorlistDs,
						batchSizeTempTrain);

				//				DataSetIterator testDataIterator = new MiniBatchFileDataSetIterator(testData, batchSizeTempTest);
				//				DataSetIterator trainDataIterator = new MiniBatchFileDataSetIterator(trainingData, batchSizeTempTrain);

				trainDataIterator.setPreProcessor((DataSetPreProcessor) dataNormalization);
				testDataIterator.setPreProcessor((DataSetPreProcessor) dataNormalization);

				earlyStoppingConfiguration = getEarlyStoppingConfiguration(testDataIterator);
				if (earlyStoppingConfiguration != null) {

					if (!isRNN) {
						System.out.println(
								"training on data with   rows:" + input.length + "  columns:" + input[0].length
										+ "  epochs:" + nEpoch + "  batchSizeTempTrain:" + batchSizeTempTrain
										+ "  batchSizeTempTest:" + batchSizeTempTest + "  trainSizeData:"
										+ trainSizeData + " testSizeData:" + testSizeData);
					} else {
						System.out.println(
								"training RNN on data with  rows:" + input.length + "  columns:" + input[0].length
										+ "  epochs:" + nEpoch + "  batchSizeTempTrain:" + batchSizeTempTrain
										+ "  batchSizeTempTest:" + batchSizeTempTest + "  trainSizeData:"
										+ trainSizeData + " testSizeData:" + testSizeData);
					}

					if (HYPERPARAMETER_TUNING) {
						hyperparameterTuning(input, target, earlyStoppingConfiguration, trainDataIterator);
					} else {
						EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(earlyStoppingConfiguration, this.model,
								trainDataIterator);

						EarlyStoppingResult<MultiLayerNetwork> result = trainer.fit();
						System.out.println("Termination reason: " + result.getTerminationReason());
						System.out.println("Termination details: " + result.getTerminationDetails());
						System.out.println("Total epochs: " + result.getTotalEpochs());
						System.out.println("Best epoch number: " + result.getBestModelEpoch());
						System.out.println("Score at best epoch: " + result.getBestModelScore());
						logger.info("Termination reason: " + result.getTerminationReason());
						logger.info("Termination details: " + result.getTerminationDetails());
						logger.info("Total epochs: " + result.getTotalEpochs());
						logger.info("Best epoch number: " + result.getBestModelEpoch());
						logger.info("Score at best epoch: " + result.getBestModelScore());
						System.out.println("------");
						System.out.println("TRAINING set");
						evalSet(trainDataIterator);
						System.out.println("");
						System.out.println("------");
						System.out.println("TEST set");
						evalSet(testDataIterator);
						System.out.println("------");

						//check learning rate
						if (CHECK_BEST_EARLY && !trainingStats && (result.getBestModelEpoch() == 0
								|| result.getBestModelEpoch() >= nEpoch - 5)) {
							if (result.getBestModelEpoch() == 0) {
								learningRate /= 10.0;
							} else {
								//								nEpoch *= 2;
								learningRate *= 10.0;
							}

							COUNTER_RE_TRAIN++;
							System.err.println(
									"WARNING training with best score at 0 => retrain with less learningRate="
											+ learningRate + " or more epoch=" + nEpoch);
							logger.warn(
									"training with best score at 0 => retrain with less learningRate={} more epoch={}",
									learningRate, nEpoch);
							if (COUNTER_RE_TRAIN < 5) {
								return train(input, target);
							} else {
								System.err.println(
										"ERROR training cant find good fit on earlyStopping after some attempts reducing learningRate="
												+ learningRate + " restore it to " + defaultLearningRate
												+ " epoch restore to " + defaultEpoch);
								logger.error(
										"ERROR training cant find good fit on earlyStopping after some attempts reducing learningRate={} restore it to {} epoch retore to {}",
										learningRate, defaultLearningRate, defaultEpoch);
								learningRate = defaultLearningRate;
								nEpoch = defaultEpoch;
							}

						}
						if (COUNTER_RE_TRAIN > 1) {
							System.out.println(Configuration
									.formatLog("**** Please change learningRate to {} for faster training!",
											learningRate));
							logger.info(Configuration
									.formatLog("Please change learningRate to {} for faster training! from {}",
											learningRate, defaultLearningRate));
						}
						COUNTER_RE_TRAIN = 0;

						//check time
						//						if (result.getTotalEpochs() - 5 < result.getBestModelEpoch()) {
						//							//underfit
						//							maxMinutesTraining++;
						//							COUNTER_RE_TRAIN++;
						//							System.err.println("WARNING training with best epoch very close to total epoch=" + result
						//									.getBestModelEpoch());
						//							logger.warn("WARNING training with best epoch very close to total epoch={}",
						//									result.getBestModelEpoch());
						//							if (COUNTER_RE_TRAIN < 5) {
						//								train(input, target);
						//								return;
						//							} else {
						//								System.err.println(
						//										"ERROR training cant find good fit on earlyStopping after some attempts increasing maxMinutesTraining="
						//												+ maxMinutesTraining + " restore it to " + defaultMaxMinutesTraining);
						//								logger.error(
						//										"ERROR training cant find good fit on earlyStopping after some attempts increasing maxMinutesTraining="
						//												+ maxMinutesTraining + " restore it to " + defaultMaxMinutesTraining);;
						//								maxMinutesTraining = defaultMaxMinutesTraining;
						//							}
						//
						//
						//						}
						//						COUNTER_RE_TRAIN = 0;

						//get the best model
						this.model = result.getBestModel();
					}
					isTrained = true;

				}
			}

			if (earlyStoppingConfiguration == null) {
				//normalize before
				int sizeData = allData.numExamples();
				System.out.println("starting training nn " + sizeData + " samples");

				dataNormalization = new NormalizerStandardize();
				((NormalizerStandardize) dataNormalization).fit(allData);

				int batchSizeTemp = Math.min(this.batchSize, MAX_BATCH_SIZE);//at least 500 iterations per epoch
				if (sizeData < this.batchSize) {
					logger.warn("can't train yet! with len {} < batchSize {}!", sizeData, batchSizeTemp);
					return false;
				}
				if (isRNN) {
					System.out.println(
							"training RNN on data with  rows:" + input.length + "  columns:" + input[0].length
									+ "  epochs:" + nEpoch + "  batchSize:" + batchSizeTemp + "  maxBatchSize:"
									+ maxBatchSizeTemp + " learningRate:" + learningRate + " momentum:"
									+ momentumNesterov);
				} else {
					System.out.println("training on data with   rows:" + input.length + "  columns:" + input[0].length
							+ "  epochs:" + nEpoch + "  batchSize:" + batchSizeTemp + "  maxBatchSize:"
							+ maxBatchSizeTemp + "  learningRate:" + learningRate + " momentumNesterov:"
							+ momentumNesterov);
				}

				DataSetIterator trainIter = null;
				trainIter = new MiniBatchFileDataSetIterator(allData, batchSizeTemp);
				//				if (batchSizeTemp <= 1) {
				//					final List<DataSet> list = allData.batchBy(1);
				//					trainIter = new ListDataSetIterator<>(list);
				//				} else {
				//					trainIter = new MiniBatchFileDataSetIterator(allData, batchSizeTemp);
				//				}

				trainIter.setPreProcessor((DataSetPreProcessor) dataNormalization);//normalize it

				this.model.fit(trainIter, this.nEpoch);

				if (!evalSet(trainIter)) {
					return false;
				}

			}

		} catch (Exception e) {
			System.err.println("error training model , delete it? " + this.modelPath);
			e.printStackTrace();
			logger.error("error training model ,delete it? {}", this.modelPath, e);
			return false;
		}

		//persist it
		saveModel();
		return true;
	}

	public void hyperparameterTuning(double[][] input, double[][] target,
			EarlyStoppingConfiguration earlyStoppingConfiguration, DataSetIterator trainDataIterator) {
		///HYPERPARAMETER TUNING out of sample
		int numInput = input[0].length;
		int numOutput = target[0].length;
		int hiddenNodes = (numInput + numOutput);

		double bestScore = -6666;
		MultiLayerNetwork bestModel = null;
		double bestLearningRate = 0;
		double bestHiddenSizeNodesMultiplier = 0.0;
		double bestL1 = 0.;
		double bestL2 = 0;
		int bestEpoch = 0;
		double bestMomentum = 0.0;
		int iterations = 3;
		System.out.println("Hyperparameter tuning NN starting");
		long startTime = System.currentTimeMillis();

		//two stages
		int totalCases = LEARNING_RATES_HYPERPARAMETER.length * HIDDEN_SIZE_NODES_MULTIPLIER_HYPERPARAMETER.length
				* EPOCHS_HYPERPARAMETER.length
				+ L1_HYPERPARAMETER.length * L2_HYPERPARAMETER.length * MOMENTUM_HYPERPARAMETER.length;
		int counter = 0;
		for (int epoch : EPOCHS_HYPERPARAMETER) {
			for (double learningRate : LEARNING_RATES_HYPERPARAMETER) {
				for (double hiddenSizeNodesMultiplier : HIDDEN_SIZE_NODES_MULTIPLIER_HYPERPARAMETER) {
					for (double momentumNesterov : MOMENTUM_HYPERPARAMETER) {
						double totalScore = 0;
						EarlyStoppingResult<MultiLayerNetwork> result = null;
						for (int iteration = 0; iteration < iterations; iteration++) {
							System.out.print(Configuration.formatLog(
									"[{}/{}] starting training epoch:{} learningRate:{} hiddenSizeNodesMultiplier:{} momentumNesterov:{} l1:{} l2:{}",
									counter, totalCases, epoch, learningRate, hiddenSizeNodesMultiplier,
									momentumNesterov, l1, l2));

							int hiddenNodesTemp = (int) (hiddenNodes * hiddenSizeNodesMultiplier);
							this.seed = (int) System.currentTimeMillis();
							MultiLayerNetwork modelTemp = createModel(numInput, hiddenNodesTemp, numOutput,
									learningRate, momentumNesterov, l1, l2);
							//epoch substitution
							List<EpochTerminationCondition> epochTerminationConditionList = new ArrayList<>();
							epochTerminationConditionList.add(new MaxEpochsTerminationCondition(epoch));
							earlyStoppingConfiguration.setEpochTerminationConditions(epochTerminationConditionList);

							EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(earlyStoppingConfiguration,
									modelTemp, trainDataIterator);

							result = trainer.fit();
							double score = result.getBestModelScore();
							System.out.println(
									"->finished	Score " + score + " at epoch " + result.getBestModelEpoch() + "/"
											+ result.getTotalEpochs());

							logger.info(
									"Hyperparameter learningRate:{}   hiddenSizeNodesMultiplier:{}  momentumNesterov:{} l1:{}   l2:{}",
									learningRate, hiddenSizeNodesMultiplier, momentumNesterov, l1, l2);
							logger.info("Termination reason: " + result.getTerminationReason());
							logger.info("Termination details: " + result.getTerminationDetails());
							logger.info("Total epochs: " + result.getTotalEpochs());
							logger.info("Best epoch number: " + result.getBestModelEpoch());
							logger.info("Score at best epoch: " + result.getBestModelScore());
							totalScore += score;
						}
						double score = totalScore / iterations;

						if (bestScore == -6666 || score < bestScore) {
							if (counter > 0) {
								System.out.println(Configuration.formatLog(
										"[{}/{}] bestFound score={} bestEpochNumber:{}/{} learningRate:{} hiddenSizeNodesMultiplier:{} momentumNesterov:{} l1:{} l2:{}",
										counter, totalCases, score, result.getBestModelEpoch(), result.getTotalEpochs(),
										learningRate, hiddenSizeNodesMultiplier, momentumNesterov, l1, l2));

							}
							bestMomentum = momentumNesterov;
							bestEpoch = epoch;
							bestScore = score;
							bestModel = result.getBestModel();
							bestLearningRate = learningRate;
							bestHiddenSizeNodesMultiplier = hiddenSizeNodesMultiplier;
						}
						counter++;
					}
				}
			}
		}
		System.out.println("Second state Start optimizing");
		bestScore = -6666;
		for (double l1 : L1_HYPERPARAMETER) {
			for (double l2 : L2_HYPERPARAMETER) {
				learningRate = bestLearningRate;
				EarlyStoppingResult<MultiLayerNetwork> result = null;
				double bestScore2 = 0.0;
				for (int iteration = 0; iteration < iterations; iteration++) {

					System.out.print(Configuration.formatLog(
							"[{}/{}] starting training learningRate:{} hiddenSizeNodesMultiplier:{} momentumNesterov:{} l1:{} l2:{}",
							counter, totalCases, bestLearningRate, bestHiddenSizeNodesMultiplier, bestMomentum, l1,
							l2));
					System.out.print(Configuration.formatLog(
							"[{}/{}] starting training epoch:{} learningRate:{} hiddenSizeNodesMultiplier:{} momentumNesterov:{} l1:{} l2:{}",
							counter, totalCases, bestEpoch, bestLearningRate, bestHiddenSizeNodesMultiplier,
							bestMomentum, l1, l2));

					int hiddenNodesTemp = (int) (hiddenNodes * bestHiddenSizeNodesMultiplier);
					this.seed = (int) System.currentTimeMillis();
					MultiLayerNetwork modelTemp = createModel(numInput, hiddenNodesTemp, numOutput, bestLearningRate,
							bestMomentum, l1, l2);

					EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(earlyStoppingConfiguration, modelTemp,
							trainDataIterator);

					result = trainer.fit();
					double score = result.getBestModelScore();
					System.out.println(
							"->finished	Score " + score + " at epoch " + result.getBestModelEpoch() + "/" + result
									.getTotalEpochs());

					logger.info(
							"Hyperparameter learningRate:{}   hiddenSizeNodesMultiplier:{} momentumNesterov:{}  l1:{}   l2:{}",
							bestLearningRate, bestHiddenSizeNodesMultiplier, bestMomentum, l1, l2);

					logger.info("Termination reason: " + result.getTerminationReason());
					logger.info("Termination details: " + result.getTerminationDetails());
					logger.info("Total epochs: " + result.getTotalEpochs());
					logger.info("Best epoch number: " + result.getBestModelEpoch());
					logger.info("Score at best epoch: " + result.getBestModelScore());
					bestScore2 += score;
				}
				bestScore2 = bestScore2 / iterations;
				if (bestScore == -6666 || bestScore2 < bestScore) {
					if (counter > 0) {
						System.out.println(Configuration.formatLog(
								"[{}/{}] bestFound score={} bestEpochNumber:{}/{} learningRate:{} hiddenSizeNodesMultiplier:{} momentumNesterov:{} l1:{} l2:{}",
								counter, totalCases, bestScore2, result.getBestModelEpoch(), result.getTotalEpochs(),
								bestLearningRate, bestHiddenSizeNodesMultiplier, bestMomentum, l1, l2));
					}
					bestScore = bestScore2;
					bestModel = result.getBestModel();
					bestL1 = l1;
					bestL2 = l2;
				}
				counter++;

			}
		}

		long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
		String message = Configuration.formatLog(
				"Hyperparameter tuning finished in {} seconds with {} iterations  with score={}  with epochs={} learningRate={} and hiddenNodesSizeMultiplier={} momentumNesterov:{}   l1={}  l2={}",
				elapsedSeconds, iterations, bestScore, bestEpoch, bestLearningRate, bestHiddenSizeNodesMultiplier,
				bestMomentum, bestL1, bestL2);
		logger.info(message);
		System.out.println(message);
		this.model = bestModel;
	}

	private class RegressionScoreCalculatorRounded extends RegressionScoreCalculator {

		private int numberDecimals;
		private double baseMultiplier;

		public RegressionScoreCalculatorRounded(RegressionEvaluation.Metric metric, DataSetIterator iterator,
				int numberDecimals) {
			super(metric, iterator);
			this.numberDecimals = numberDecimals;
			this.baseMultiplier = Math.pow(10, this.numberDecimals);

		}

		protected double finalScore(RegressionEvaluation eval) {
			double output = super.finalScore(eval);
			return Math.round(output * this.baseMultiplier) / this.baseMultiplier;
		}
	}

	@Override public double[] predict(double[] input) {
		if (this.model == null || dataNormalization == null || !isTrained) {
			//			logger.error("to predict you need to fit it first!");
			return null;
		}

		double[] output = null;
		try {
			double[][] inputArr = new double[1][input.length];
			inputArr[0] = input;
			INDArray inputND = Nd4j.create(inputArr);

			((NormalizerStandardize) dataNormalization).transform(inputND);
			INDArray outputNd = this.model.output(inputND);
			//inverse transform output?
			//			((NormalizerStandardize) dataNormalization).revertLabels(outputNd);

			output = outputNd.toDoubleVector();
			if (Double.isNaN(Doubles.max(output))) {
				logger.warn("{} output of model is NaN when is trained!", this.modelPath);
				output = null;
			}
		} catch (Exception e) {
			logger.error("error predicting ", e);
		}

		return output;
	}

	@Override public MemoryReplayModel cloneIt(String modelPath) {
		try {
			MemoryReplayModel output = (Dl4jMemoryReplayModel) this.clone();
			output.setModelPath(modelPath);
			return output;
		} catch (CloneNotSupportedException e) {
			logger.error("cant clone Dl4jMemoryReplayModel ", e);
		}
		return null;

	}
}
