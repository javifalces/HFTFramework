package com.lambda.investing.algorithmic_trading.reinforcement_learning;

import com.google.common.primitives.Doubles;
import com.lambda.investing.Configuration;

import com.lambda.investing.ArrayUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.scorecalc.RegressionScoreCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxTimeIterationTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;

import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.nd4j.common.primitives.Pair;
import org.nd4j.evaluation.IMetric;
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
public class Dl4jRegressionMemoryReplayModel implements MemoryReplayModel, Cloneable {

    public static List<String> TENSORFLOW_SUFFIX = ArrayUtils
            .StringArrayList(new String[]{".h5", ".tf", ".tensorflow"});
    public static boolean CHECK_BEST_EARLY = false;
    private static int MAX_BATCH_SIZE = 10000;

    //ParameterTuning Values step 1
    public double[] learningRateParameterTuning = new double[]{0.00001, 0.0001, 0.001, 0.01};

    public int[] batchSizeParameterTuning = new int[]{32, 64, 128, 256};
    public double[] hiddenSizeNodesMultiplierParameterTuning = new double[]{2, 1, 0.5};
    public double[] epochMultiplierParameterTuning = new double[]{1.0, 2.0, 0.5};
    public double[] momentumParameterTuning = new double[]{0.0, 0.5, 0.8};

    //ParameterTuning Values step 2
    public double[] l1ParameterTuning = new double[]{0.0, 0.1, 0.01, 0.001};
    public double[] l2ParameterTuning = new double[]{0.0, 0.1, 0.01, 0.001};


    protected boolean parameterTuningBeforeTraining = false;//will disable if file already exist
    protected boolean earlyStoppingTraining = false;
    private static int COUNTER_RE_TRAIN = 0;
    protected int maxMinutesTraining = 5;

    //	https://deeplearning4j.konduit.ai/models/layers
    public static boolean EARLY_STOPPING_ENABLE_DEFAULT = false;//boolean on startup
    public static boolean CLEAN_NN_IF_EXIST = false;//delete nn every time we train.... makes no sense
    public static boolean PARAMETER_TUNING_ENABLE_DEFAULT = false;//boolean on startup

    protected static int EARLY_STOPPING_TOLERANCE_DECIMALS = 3;
    protected static double DEFAULT_DATA_SPLIT_TRAINING_PCT = 0.6;//Use 60% of data for training
    protected double earlyStoppingDataSplitTrainingPct;//Use 60% of data for training
    protected Logger logger = LogManager.getLogger(Dl4jRegressionMemoryReplayModel.class);
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

    protected long seed;


    protected double hiddenSizeNodesMultiplier = 2;

    protected ILossFunction lossFunction = new LossMSE();
    protected IActivation activationFunction = new ActivationIdentity();
    ;


    protected IMetric evaluationMetric = RegressionEvaluation.Metric.RMSE;


    public void setTrainingStats(boolean trainingStats) {
        this.trainingStats = trainingStats;
    }

    public Dl4jRegressionMemoryReplayModel(String modelPath, double learningRate, double momentumNesterov, int nEpoch,
                                           int batchSize, int maxBatchSize, double l2, double l1) {
        this.modelPath = modelPath;
        this.learningRate = learningRate;
        this.momentumNesterov = momentumNesterov;
        this.nEpoch = nEpoch;
        this.maxBatchSize = maxBatchSize;
        this.batchSize = batchSize;
        this.l2 = l2;
        this.l1 = l1;
        this.seed = (int) System.currentTimeMillis();
        earlyStoppingDataSplitTrainingPct = DEFAULT_DATA_SPLIT_TRAINING_PCT;
        loadModel();
        this.parameterTuningBeforeTraining = PARAMETER_TUNING_ENABLE_DEFAULT;
        this.earlyStoppingTraining = EARLY_STOPPING_ENABLE_DEFAULT;
        if (this.model != null) {
            logger.info("Disable parameter tuning because model loaded");
            this.parameterTuningBeforeTraining = false;
        }
    }

    public void setEarlyStoppingDataSplitTrainingPct(double earlyStoppingDataSplitTrainingPct) {
        this.earlyStoppingDataSplitTrainingPct = earlyStoppingDataSplitTrainingPct;
    }

    public void setHiddenSizeNodesMultiplier(double hiddenSizeNodesMultiplier) {
        this.hiddenSizeNodesMultiplier = hiddenSizeNodesMultiplier;
    }


    public double[] getLearningRateParameterTuning() {
        return learningRateParameterTuning;
    }

    public int[] getBatchSizeParameterTuning() {
        return batchSizeParameterTuning;
    }

    public void setBatchSizeParameterTuning(int[] batchSizeParameterTuning) {
        this.batchSizeParameterTuning = batchSizeParameterTuning;
    }

    public void setLearningRateParameterTuning(double[] learningRateParameterTuning) {
        this.learningRateParameterTuning = learningRateParameterTuning;
    }

    public double[] getHiddenSizeNodesMultiplierParameterTuning() {
        return hiddenSizeNodesMultiplierParameterTuning;
    }

    public void setHiddenSizeNodesMultiplierParameterTuning(double[] hiddenSizeNodesMultiplierParameterTuning) {
        this.hiddenSizeNodesMultiplierParameterTuning = hiddenSizeNodesMultiplierParameterTuning;
    }

    public double[] getEpochMultiplierParameterTuning() {
        return epochMultiplierParameterTuning;
    }

    public void setEpochMultiplierParameterTuning(double[] epochMultiplierParameterTuning) {
        this.epochMultiplierParameterTuning = epochMultiplierParameterTuning;
    }

    public double[] getMomentumParameterTuning() {
        return momentumParameterTuning;
    }

    public void setMomentumParameterTuning(double[] momentumParameterTuning) {
        this.momentumParameterTuning = momentumParameterTuning;
    }

    public double[] getL1ParameterTuning() {
        return l1ParameterTuning;
    }

    public void setL1ParameterTuning(double[] l1ParameterTuning) {
        this.l1ParameterTuning = l1ParameterTuning;
    }

    public double[] getL2ParameterTuning() {
        return l2ParameterTuning;
    }

    public void setL2ParameterTuning(double[] l2ParameterTuning) {
        this.l2ParameterTuning = l2ParameterTuning;
    }

    public void setParameterTuningBeforeTraining(boolean parameterTuningBeforeTraining) {
        this.parameterTuningBeforeTraining = parameterTuningBeforeTraining;
    }

    @Override
    public void setEarlyStoppingTraining(boolean earlyStoppingTraining) {
        this.earlyStoppingTraining = earlyStoppingTraining;

    }

    public IMetric getEvaluationMetric() {
        return evaluationMetric;
    }

    public void setEvaluationMetric(IMetric evaluationMetric) {
        this.evaluationMetric = evaluationMetric;
    }


    public void setSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public long getSeed() {
        return seed;
    }

    @Override
    public int getEpoch() {
        return nEpoch;
    }

    public Dl4jRegressionMemoryReplayModel(String modelPath, double learningRate, double momentumNesterov, int nEpoch,
                                           int batchSize, int maxBatchSize, double l2, double l1, boolean loadModel) {
        this.modelPath = modelPath;
        this.learningRate = learningRate;
        this.momentumNesterov = momentumNesterov;
        this.nEpoch = nEpoch;
        this.maxBatchSize = maxBatchSize;
        this.batchSize = batchSize;
        this.l2 = l2;
        this.l1 = l1;
        if (loadModel) {
            loadModel();
            if (this.model != null) {
                logger.info("Disable parameter tuning because model loaded");
                parameterTuningBeforeTraining = false;
            }

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

    @Override
    public String getModelPath() {
        return modelPath;
    }

    public void loadTensorflowModel(String suffixChosen) {
        File savedModelFile = new File(this.modelPath);
        if (!savedModelFile.exists()) {
            logger.info("Tensorflow model not found to load {}", this.modelPath);
            this.modelPath = this.modelPath.replace(suffixChosen, ".model");
            model = null;
            isTrained = false;
            return;
        }

        try {
            long start = System.currentTimeMillis();
            MultiLayerNetwork tensorflowModel = KerasModelImport.importKerasSequentialModelAndWeights(this.modelPath);
            //change modelsPath to the new one
            this.modelPath = this.modelPath.replace(suffixChosen, ".model");
            File newSavedModelFile = new File(this.modelPath);

            if (!newSavedModelFile.exists()) {
                logger.info("original model not found to load normalizer {}", this.modelPath);
                model = null;
                isTrained = false;
                return;
            }

            Pair<MultiLayerNetwork, Normalizer> pair = restoreMultiLayerNetworkAndNormalizer(savedModelFile, true);
            this.model = tensorflowModel;
            this.dataNormalization = pair.getSecond();
            isTrained = true;
            long elapsed = (System.currentTimeMillis() - start) / (1000);
            logger.info("loaded Tensorflow model {}", this.modelPath);
            logger.info("loaded in {} seconds , Tensorflow model {}", elapsed, this.modelPath);

        } catch (Exception e) {
            logger.error("error loading Tensorflow model  {}", this.modelPath, e);
            model = null;
            this.dataNormalization = null;
            isTrained = false;

        }

    }

    public void loadModel() {
        boolean isTensorflowModel = false;
        String suffixChosen = "";
        for (String suffix : TENSORFLOW_SUFFIX) {
            if (this.modelPath.toUpperCase().endsWith(suffix.toUpperCase())) {
                isTensorflowModel = true;
                suffixChosen = suffix;
            }
        }
        if (isTensorflowModel) {
            logger.info("model tensorflow detected load {} {}", suffixChosen, this.modelPath);
            loadTensorflowModel(suffixChosen);
            return;
        }

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
                System.err.println(Configuration.formatLog("cant load model {} -> GPU memory?", this.modelPath));
                logger.error("cant load model {} => GPU memory?", this.modelPath);
            }
//            catch(ExceptionInInitializerError e1){
//                System.err.println(Configuration.formatLog("cant load model {} -> loading cpu Model on GPU?(bad arquitecture)", this.modelPath));
//                logger.error("cant load model {} => bad arquitecture?", this.modelPath, e1);
//            }
        }

    }

    public void saveModel() {
        File savedModelFile = new File(this.modelPath);
        try {
            //			this.model.save(savedModelFile,true);
            writeModel(this.model, savedModelFile, true, (DataNormalization) this.dataNormalization);
            System.out.println(Configuration.formatLog("saved model {}", this.modelPath));
            logger.info("saved model {}", this.modelPath);
        } catch (IOException e) {
            System.err.println(Configuration.formatLog("cant save model {}", this.modelPath));
            logger.error("cant save model ", e);
        }

    }

    @Override
    public boolean isTrained() {
        return isTrained;
    }

    @Override
    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    protected IUpdater getUpdater(double momentumNesterov, double learningRate) {
        if (momentumNesterov > 0) {
            return new Nesterovs(learningRate, momentumNesterov);
        } else {
            return new Adam.Builder().learningRate(learningRate).build();
        }
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

        MultiLayerConfiguration conf = null;

        logger.info(
                "Creating nn model inputs:{}  hiddenNodes:{}  outputs:{}  learningRate:{}  momentumNesterov:{} l1:{} l2:{}",
                numInputs, numHiddenNodes, numOutputs, learningRate, momentumNesterov, l1, l2);

        conf = createFeedForwardModel(numInputs, numHiddenNodes, numOutputs, learningRate, momentumNesterov, l1,
                l2);

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
                        new RegressionScoreCalculatorRounded((RegressionEvaluation.Metric) evaluationMetric, testDataIterator,
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
    @Override
    public boolean updateGradient(double[][] input, double[][] output, double[][] expected) {
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

    protected RegressionEvaluation getRegressionEval(MultiLayerNetwork model, DataSetIterator dataSetIterator) {
        return model.evaluateRegression(dataSetIterator);
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

    protected MiniBatchFileDataSetIterator getDataSetIterator(DataSet dataSet, int batchSize) {
        try {
//			RecordReaderDataSetIterator dataSetIterator = new RecordReaderDataSetIterator.Builder(dataSet,batchSize).build();
            MiniBatchFileDataSetIterator dataIterator = new MiniBatchFileDataSetIterator(dataSet, batchSize);
//		DataSetIterator dataIterator = new ListDataSetIterator<>(dataSet.asList(), batchSize);
            return dataIterator;
        } catch (Exception e) {
            logger.error("error getDataSetIterator ", e);
            e.printStackTrace();
        }
        return null;
    }

    public boolean trainWithEarlyStopping(double[][] input, double[][] target) {
        DataSet allData = CreateDataSet(input, target);
        DataSet[] splittedData = splitTrainingTestData(allData);
        DataSet trainingData = splittedData[0];
        DataSet testData = splittedData[1];
        int trainSizeData = trainingData.numExamples();
        int testSizeData = testData.numExamples();//(int) (sizeData*(1-fractionTrainSplit));
        //https://stackoverflow.com/questions/33902896/deeplearning4j-splitting-datasets-for-test-and-train
        System.out.println("starting training nn with early stop " + allData.numExamples() + " samples   -> train:" + trainSizeData + " test:" + testSizeData);


        dataNormalization = new NormalizerStandardize();
        ((NormalizerStandardize) dataNormalization).fit(trainingData);

        int batchSizeTempTest = Math.min(this.batchSize, MAX_BATCH_SIZE);//at least 500 iterations per epoch
        int batchSizeTempTrain = (int) Math
                .min(this.batchSize, MAX_BATCH_SIZE);        //at least 500 iterations per epoch


        DataSetIterator testDataIterator = getDataSetIterator(testData, batchSizeTempTest);
        DataSetIterator trainDataIterator = getDataSetIterator(trainingData, batchSizeTempTest);

        trainDataIterator.setPreProcessor((DataSetPreProcessor) dataNormalization);
        testDataIterator.setPreProcessor((DataSetPreProcessor) dataNormalization);

        EarlyStoppingConfiguration earlyStoppingConfiguration = getEarlyStoppingConfiguration(testDataIterator);

        System.out.println(new Date() + " " + toString() + " training on data with   rows:" + allData.numExamples() + "  columns:"
                + input[0].length + "  epochs:" + nEpoch + "  batchSizeTempTrain:" + batchSizeTempTrain
                + "  batchSizeTempTest:" + batchSizeTempTest + "  trainSizeData:" + trainSizeData
                + " testSizeData:" + testSizeData);

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

        //get the best model
        this.model = result.getBestModel();

        isTrained = true;

        return true;
    }

    public boolean trainNormal(DataSet allData, double[][] input) throws IOException {

        int maxBatchSizeTemp = Math.min(this.maxBatchSize, allData.numExamples());//it doesn't matter here
        allData = allData.sample(Math.min(this.maxBatchSize, maxBatchSizeTemp));
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

        System.out.println(new Date() + " " + toString() + " training on data with   rows:" + sizeData + "  columns:"
                + input[0].length + "  epochs:" + nEpoch + "  batchSize:" + batchSizeTemp
                + "  maxBatchSize:" + maxBatchSizeTemp + "  learningRate:" + learningRate
                + " momentumNesterov:" + momentumNesterov);


        DataSetIterator trainIter = getDataSetIterator(allData, batchSizeTemp);
        trainIter.setPreProcessor((DataSetPreProcessor) dataNormalization);//normalize it
        long startTime = System.currentTimeMillis();
        this.model.addListeners(new TrainingListener() {

            @Override
            public void iterationDone(Model model, int iteration, int epoch) {

            }

            @Override
            public void onEpochStart(Model model) {
                long msElapsed = System.currentTimeMillis() - startTime;
                int minutesTraining = (int) (msElapsed / (60000));
                if (minutesTraining > maxMinutesTraining) {
                    logger.info("timeout Stop training! ");
                }
            }

            @Override
            public void onEpochEnd(Model model) {
                long msElapsed = System.currentTimeMillis() - startTime;
                int minutesTraining = (int) (msElapsed / (60000));
                if (minutesTraining > maxMinutesTraining) {
                    logger.info("timeout Stop training! ");
                }
            }

            @Override
            public void onForwardPass(Model model, List<INDArray> activations) {

            }

            @Override
            public void onForwardPass(Model model, Map<String, INDArray> activations) {

            }

            @Override
            public void onGradientCalculation(Model model) {

            }

            @Override
            public void onBackwardPass(Model model) {

            }
        });

        this.model.fit(trainIter, this.nEpoch);

        if (!evalSet(trainIter)) {
            return false;
        }
        return true;

    }


    protected DataSet CreateDataSet(double[][] input, double[][] target) {
        INDArray x = Nd4j.create(input);
        INDArray y = Nd4j.create(target);

        DataSet allData = new DataSet(x, y);
        return allData;
    }

    @Override
    public boolean train(double[][] input, double[][] target) {

        DataSet allData = CreateDataSet(input, target);

        try {
            if (this.model == null) {
                int numInput = input[0].length;
                int numOutput = target[0].length;
                int hiddenNodes = (numInput + numOutput);
                //				Hyperparameter tuning finished in 3978 seconds with 3 iterations  with score=0.04  with epochs=750 learningRate=0.1 and hiddenNodesSizeMultiplier=2.0  l1=0.0  l2=0.0
                int hiddenNodesModel = (int) Math.round(hiddenSizeNodesMultiplier * hiddenNodes);
                String entryMessage = "Creating new empty FeedForward nn " + toString();

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

            if (parameterTuningBeforeTraining) {

                DataSet trainingData = allData;
                if (earlyStoppingTraining) {
                    DataSet[] splittedData = splitTrainingTestData(allData);
                    trainingData = splittedData[0];
                }

                hyperparameterTuning(input[0].length, target[0].length, trainingData);
            }


            if (this.trainingStats) {
                UIServer uiServer = UIServer.getInstance();
                StatsStorage statsStorage = new InMemoryStatsStorage();
                //			StatsStorage statsStorage = new FileStatsStorage();//in case of memory restrictions
                logger.info("starting training nn UI at localhost:9000");
                System.out.println("starting training nn UI  at localhost:9000");
                model.addListeners(new StatsListener(statsStorage));
                uiServer.attach(statsStorage);
            }

            if (earlyStoppingTraining) {
                boolean trained = trainWithEarlyStopping(input, target);
                if (!trained) {
                    logger.warn("trainWithEarlyStopping returned a false on result -> retry train and disable earlyStoppingTraining");
                    earlyStoppingTraining = false;
                    return train(input, target);
                }
            } else {
                //training without earlystopping
                trainNormal(allData, input);
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

    protected DataSet[] splitTrainingTestData(DataSet allData) {
        SplitTestAndTrain testAndTrain = allData.splitTestAndTrain(earlyStoppingDataSplitTrainingPct);
        DataSet trainingData = testAndTrain.getTrain();
        DataSet testData = testAndTrain.getTest();
        return new DataSet[]{trainingData, testData};

    }

    @Override
    public String toString() {
        return "Dl4jMemoryReplayModel";
    }


    @Setter
    @Getter
    private class ParameterTuningResults {

        private double bestLearningRate;
        private double bestEpoch;
        private double bestEpochMultiplier;
        private double bestHiddenSizeNodes;
        private double bestHiddenSizeNodesMultiplier;
        private double bestL1;
        private double bestL2;
        private double bestMomentum;
        private double trainScore;
        private double testScore;
        private int bestBatchSize;
        private MultiLayerNetwork bestModel;
    }

    public ParameterTuningResults hyperParameterTuningBestValues(double[] learningRateParameterTuning, double[] hiddenSizeNodesMultiplierParameterTuning, double[] epochMultiplierParameterTuning,
                                                                 double[] l1ParameterTuning, double[] l2ParameterTuning, double[] momentumParameterTuning, int[] batchSizeParameterTuning, int featureInputs, int outputColumns, int iterations, DataSet trainingData) {

        int hiddenNodes = (featureInputs + outputColumns);
        long startTime = System.currentTimeMillis();

        int totalCases = learningRateParameterTuning.length * hiddenSizeNodesMultiplierParameterTuning.length
                * epochMultiplierParameterTuning.length
                * l1ParameterTuning.length * l2ParameterTuning.length * momentumParameterTuning.length * batchSizeParameterTuning.length;

        //initial values
        double bestTestScore = Double.MAX_VALUE;
        double bestTrainScore = Double.MAX_VALUE;
        System.out.println("Hyperparameter tuning NN starting " + totalCases + " cases");
        ParameterTuningResults result = null;

        int counter = 0;

        for (int batchSize : batchSizeParameterTuning) {
            DataSetIterator trainDataIterator = getDataSetIterator(trainingData, batchSize);
            trainDataIterator.setPreProcessor((DataSetPreProcessor) dataNormalization);

            for (double epochMultiplier : epochMultiplierParameterTuning) {
                int epoch = (int) Math.round(epochMultiplier * this.nEpoch);

                for (double learningRate : learningRateParameterTuning) {
                    for (double hiddenSizeNodesMultiplier : hiddenSizeNodesMultiplierParameterTuning) {
                        for (double momentumNesterov : momentumParameterTuning) {
                            for (double l1 : l1ParameterTuning) {
                                for (double l2 : l2ParameterTuning) {

                                    double totalScore = 0;
                                    double totalTrainScore = 0;
                                    MultiLayerNetwork modelTemp = null;
                                    int hiddenNodesTemp = (int) Math.ceil(hiddenNodes * hiddenSizeNodesMultiplier);

                                    //iterations bucle
                                    for (int iteration = 0; iteration < iterations; iteration++) {
                                        System.out.print(Configuration.formatLog(
                                                "[{}/{}] starting training metric:{} epoch:{} batchSize:{} learningRate:{} hiddenSizeNodesMultiplier:{} momentumNesterov:{} l1:{} l2:{}",
                                                counter, totalCases, evaluationMetric, epoch, batchSize, learningRate, hiddenSizeNodesMultiplier,
                                                momentumNesterov, l1, l2));

                                        String iterationsMsg = "";
                                        if (iterations > 1) {
                                            this.seed = System.currentTimeMillis();
                                            iterationsMsg = Configuration.formatLog("[{}/{}]", iteration, iterations);
                                        }

                                        modelTemp = createModel(featureInputs, hiddenNodesTemp, outputColumns,
                                                learningRate, momentumNesterov, l1, l2);
                                        modelTemp.fit(trainDataIterator);

                                        RegressionEvaluation trainEval = getRegressionEval(modelTemp, trainDataIterator);
                                        RegressionEvaluation testEval = getRegressionEval(modelTemp, trainDataIterator);
                                        double trainScore = trainEval.getValue(evaluationMetric);
                                        double testScore = testEval.getValue(evaluationMetric);

                                        System.out.println(
                                                "-> " + iterationsMsg + " finished	trainScore: " + trainScore + "  testScore: " + testScore);

                                        logger.info(
                                                "{} Hyperparameter epoch:{} learningRate:{}   hiddenSizeNodesMultiplier:{}  momentumNesterov:{} l1:{}   l2:{}  ->  trainScore:{} testScore:{}",
                                                iterationsMsg, epoch, learningRate, hiddenSizeNodesMultiplier, momentumNesterov, l1, l2, trainScore, testScore);
                                        totalTrainScore += trainScore;
                                        totalScore += testScore;
                                    }
                                    double score = totalScore / iterations;
                                    double trainScore = totalTrainScore / iterations;

                                    if (bestTestScore == Double.MAX_VALUE || score < bestTestScore) {
                                        if (counter > 0) {
                                            //avoid first training
                                            System.out.println(Configuration.formatLog(
                                                    "[{}/{}] bestFound score={}  epoch={} learningRate:{} hiddenSizeNodesMultiplier:{} momentumNesterov:{} l1:{} l2:{}",
                                                    counter, totalCases, score, epoch,
                                                    learningRate, hiddenSizeNodesMultiplier, momentumNesterov, l1, l2));

                                        }

                                        result = new ParameterTuningResults();
                                        result.setBestL1(l1);
                                        result.setBestL2(l2);
                                        result.setBestMomentum(momentumNesterov);
                                        result.setBestHiddenSizeNodesMultiplier(hiddenSizeNodesMultiplier);
                                        result.setBestHiddenSizeNodes(hiddenNodesTemp);
                                        result.setBestEpoch(epoch);
                                        result.setBestEpochMultiplier(epochMultiplier);
                                        result.setBestLearningRate(learningRate);
                                        result.setBestModel(modelTemp);
                                        result.setTestScore(score);
                                        result.setTrainScore(trainScore);
                                        result.setBestBatchSize(batchSize);

                                        bestTestScore = score;

                                    }


                                    counter++;
                                }
                            }
                        }
                    }
                }
            }
        }
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        if (result == null) {
            logger.error("something is wrong on parametertuning!!! result is null!!");
        }

        String message = Configuration.formatLog(
                "Hyperparameter tuning finished in {} seconds with {} iterations  with score={}  with epochs={} batchSize={} learningRate={} and hiddenSizeNodesMultiplier={} momentumNesterov:{}   l1={}  l2={}",
                elapsedSeconds, iterations, bestTestScore, result.getBestEpoch(), result.getBestBatchSize(), result.getBestLearningRate(), result.getBestHiddenSizeNodesMultiplier(),
                result.getBestMomentum(), result.getBestL1(), result.getBestL2());
        logger.info(message);
        System.out.println(message);

        return result;

    }

    public void hyperparameterTuning(int featureInputs, int outputColumns, DataSet trainingData) {

        long startTime = System.currentTimeMillis();
        int iterations = 1;
        ///HYPERPARAMETER TUNING out of sample
        ParameterTuningResults firstStageResults = hyperParameterTuningBestValues(learningRateParameterTuning, hiddenSizeNodesMultiplierParameterTuning,
                epochMultiplierParameterTuning, new double[]{0.0}, new double[]{0.0}, momentumParameterTuning, batchSizeParameterTuning, featureInputs, outputColumns, iterations, trainingData);

        System.out.println(Configuration.formatLog("Second state Start optimizing l1 and l2 with momentumNesterov={}  learningRate:{} hiddenSizeNodesMultiplier:{} ", firstStageResults.getBestMomentum(), firstStageResults.getBestLearningRate(), firstStageResults.getBestHiddenSizeNodesMultiplier()));

        double[] learningRateParameterTuningTemp = new double[]{firstStageResults.bestLearningRate};
        double[] hiddenSizeNodesMultiplierParameterTuningTemp = new double[]{firstStageResults.bestHiddenSizeNodesMultiplier};
        double[] epochMultiplierParameterTuningTemp = new double[]{firstStageResults.bestEpochMultiplier};
        double[] momentumParameterTuningTemp = new double[]{firstStageResults.bestMomentum};
        int[] batchSizeParameterTuningTemp = new int[]{firstStageResults.bestBatchSize};
        ParameterTuningResults secondStageResults = hyperParameterTuningBestValues(learningRateParameterTuningTemp, hiddenSizeNodesMultiplierParameterTuningTemp,
                epochMultiplierParameterTuningTemp, l1ParameterTuning, l2ParameterTuning, momentumParameterTuningTemp, batchSizeParameterTuningTemp, featureInputs, outputColumns, iterations, trainingData);

        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        String message = Configuration.formatLog(
                "Hyperparameter tuning finished in {} seconds with {} iterations  with score={}  with epochs={} batchSize={} learningRate={} and hiddenNodesSizeMultiplier={} momentumNesterov:{}   l1={}  l2={}",
                elapsedSeconds, iterations, secondStageResults.getTestScore(), secondStageResults.getBestEpoch(), secondStageResults.getBestBatchSize(), secondStageResults.getBestLearningRate(), secondStageResults.getBestHiddenSizeNodesMultiplier(),
                secondStageResults.getBestMomentum(), secondStageResults.getBestL1(), secondStageResults.getBestL2());
        logger.info(message);
        System.out.println(message);

        //set values tuned
        this.model = secondStageResults.getBestModel();
        this.batchSize = secondStageResults.getBestBatchSize();
        this.hiddenSizeNodesMultiplier = secondStageResults.getBestHiddenSizeNodesMultiplier();
        this.l1 = secondStageResults.getBestL1();
        this.l2 = secondStageResults.getBestL2();
        this.momentumNesterov = secondStageResults.getBestMomentum();
        this.nEpoch = (int) Math.round(secondStageResults.getBestEpoch());
        this.learningRate = secondStageResults.getBestLearningRate();
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

    @Override
    public double[] predict(double[] input) {
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

    @Override
    public MemoryReplayModel cloneIt(String modelPath) {
        try {
            MemoryReplayModel output = (Dl4jRegressionMemoryReplayModel) this.clone();
            output.setModelPath(modelPath);
            return output;
        } catch (CloneNotSupportedException e) {
            logger.error("cant clone Dl4jMemoryReplayModel ", e);
        }
        return null;

    }
}
