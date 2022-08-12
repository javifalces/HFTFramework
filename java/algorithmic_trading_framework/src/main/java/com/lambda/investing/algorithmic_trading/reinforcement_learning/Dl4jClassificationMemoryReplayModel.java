package com.lambda.investing.algorithmic_trading.reinforcement_learning;

import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.scorecalc.ClassificationScoreCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxTimeIterationTerminationCondition;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.activations.impl.ActivationSoftmax;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.impl.LossNegativeLogLikelihood;

import java.util.concurrent.TimeUnit;

public class Dl4jClassificationMemoryReplayModel extends Dl4jRegressionMemoryReplayModel {

    public Dl4jClassificationMemoryReplayModel(String modelPath, double learningRate, double momentumNesterov,
                                               int nEpoch, int batchSize, int maxBatchSize, double l2, double l1) {
        super(modelPath, learningRate, momentumNesterov, nEpoch, batchSize, maxBatchSize, l2, l1);
        this.lossFunction = new LossNegativeLogLikelihood();
        this.activationFunction = new ActivationSoftmax();
        this.evaluationMetric = Evaluation.Metric.RECALL;
    }

    public Dl4jClassificationMemoryReplayModel(String modelPath, double learningRate, double momentumNesterov,
                                               int nEpoch, int batchSize, int maxBatchSize, double l2, double l1, boolean loadModel) {
        super(modelPath, learningRate, momentumNesterov, nEpoch, batchSize, maxBatchSize, l2, l1, loadModel);
        this.lossFunction = new LossNegativeLogLikelihood();
        this.activationFunction = new ActivationSoftmax();
    }

    protected EarlyStoppingConfiguration getEarlyStoppingConfiguration(DataSetIterator testDataIterator) {
        EarlyStoppingConfiguration earlyStoppingConfiguration = new EarlyStoppingConfiguration.Builder()
                .iterationTerminationConditions(new MaxTimeIterationTerminationCondition(maxMinutesTraining, TimeUnit.MINUTES))
                .epochTerminationConditions(new MaxEpochsTerminationCondition(nEpoch)).scoreCalculator(
                        //								new DataSetLossCalculator(testDataIterator,true),

                        //						https://github.com/eclipse/deeplearning4j/issues/8831
                        //												new ROCScoreCalculator(ROCScoreCalculator.ROCType.BINARY, ROCScoreCalculator.Metric.AUC,
                        //														testDataIterator))

                        new ClassificationScoreCalculator((Evaluation.Metric) this.evaluationMetric,
                                testDataIterator))//Others are not working!?
                //						.modelSaver(new LocalFileModelSaver(this.modelPath))
                .evaluateEveryNEpochs(5).
                build();
        return earlyStoppingConfiguration;

    }

    public MultiLayerNetwork createModel(int numInputs, int numHiddenNodes, int numOutputs, double learningRate,
                                         double momentumNesterov, double l1, double l2) {
        logger.info(
                "Creating classification nn model inputs:{}  hiddenNodes:{}  outputs:{}  learningRate:{}  momentumNesterov:{} l1:{} l2:{}",
                numInputs, numHiddenNodes, numOutputs, learningRate, momentumNesterov, l1, l2);
        MultiLayerConfiguration conf = null;


        conf = createFeedForwardModel(numInputs, numHiddenNodes, numOutputs, learningRate, momentumNesterov, l1,
                l2);

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        return model;
    }

    @Override
    public String toString() {
        return "Dl4jClassificationMemoryReplayModel";
    }

    protected boolean evalSet(DataSetIterator dataSetIterator) {
        Evaluation eval = model.evaluate(dataSetIterator);
        System.out.println(eval.stats());
        if (Double.isFinite(eval.accuracy())) {
            isTrained = true;
            return true;
        } else {
            System.out.println(
                    "training was not okey---> please consider shutdown some other java process or clean gpu memory");
            logger.error("something was wrong training when {} is not finite", eval.accuracy());
            logger.error(eval.stats());
            return false;
            //					System.out.println("clear previous nn");
            //					this.model.clear();
            //					train(input,target);
            //					return;
        }
    }

    private MultiLayerConfiguration createFeedForwardModel(int numInputs, int numHiddenNodes, int numOutputs,
                                                           double learningRate, double momentumNesterov, double l1, double l2) {
        int secondLayerNodes = numInputs / 2;
        WeightInit weightInit = WeightInit.XAVIER_UNIFORM;//a form of Gaussian distribution (WeightInit.XAVIER),
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(getUpdater(momentumNesterov, learningRate)).l2(l2).l1(l1)
                //				.updater((new Adam.Builder().learningRate(learningRate)).build()).l2(l2).l1(l1)

                //input layer
                .list().layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes).weightInit(weightInit)
                        .activation(Activation.SIGMOID).build()).
                //hidden layer 1
                        layer(1,
                        new DenseLayer.Builder().nIn(numHiddenNodes).nOut(secondLayerNodes).weightInit(weightInit)
                                .activation(Activation.SIGMOID).build()).
                //hidden layer 2
                //						layer(2,
                //						new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes).weightInit(weightInit)
                //								.activation(Activation.SIGMOID).build()).
                //output layer
                        layer(2, new OutputLayer.Builder(lossFunction)
                        .activation(activationFunction) //for binary multi-label classification, use sigmoid + XENT.
                        .weightInit(weightInit).weightInit(weightInit).nIn(secondLayerNodes).nOut(numOutputs).build())

                .build();
        return conf;
    }

}
