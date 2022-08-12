package com.lambda.investing.algorithmic_trading.reinforcement_learning;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;

public class Dl4jLSTMMemoryReplayModel extends Dl4jRegressionMemoryReplayModel {
    public Dl4jLSTMMemoryReplayModel(String modelPath, double learningRate, double momentumNesterov, int nEpoch, int batchSize, int maxBatchSize, double l2, double l1) {
        super(modelPath, learningRate, momentumNesterov, nEpoch, batchSize, maxBatchSize, l2, l1);
    }

    public Dl4jLSTMMemoryReplayModel(String modelPath, double learningRate, double momentumNesterov, int nEpoch, int batchSize, int maxBatchSize, double l2, double l1, boolean loadModel) {
        super(modelPath, learningRate, momentumNesterov, nEpoch, batchSize, maxBatchSize, l2, l1, loadModel);
    }

    private MultiLayerConfiguration createRNNModel(int numInputs, int numHiddenNodes, int numOutputs,
                                                   double learningRate, double momentumNesterov, double l1, double l2) {

        // https://deeplearning4j.konduit.ai/models/recurrent
        int secondLayerNodes = numInputs / 2;
        WeightInit weightInit = WeightInit.XAVIER_UNIFORM;//a form of Gaussian distribution (WeightInit.XAVIER),
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(getUpdater(momentumNesterov, learningRate)).l2(l2).l1(l1)

                //input layer
                .list().layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes).weightInit(weightInit)
                        .activation(Activation.SIGMOID).build()).

                //hidden layer 1 (required if we dont receive 3d data!)
                        layer(1, new LSTM.Builder().nIn(numHiddenNodes).nOut(secondLayerNodes).weightInit(weightInit)
                        .activation(Activation.SIGMOID).build()).
                //hidden layer 2
                //						layer(2, new LSTM.Builder().nIn(numHiddenNodes).nOut(numOutputs).weightInit(weightInit)
                //						.activation(Activation.SIGMOID).build()).

                //output layer
                        layer(2,
                        new OutputLayer.Builder(lossFunction).weightInit(weightInit).activation(activationFunction)
                                .weightInit(weightInit).nIn(secondLayerNodes).nOut(numOutputs).
                                build()).build();
        //The above code snippet will cause any network training (i.e., calls to MultiLayerNetwork.fit() methods) to use truncated BPTT with segments of length 100 steps.
        return conf;
    }

    @Override
    public String toString() {
        return "Dl4jLSTMMemoryReplayModel";
    }
    public MultiLayerNetwork createModel(int numInputs, int numHiddenNodes, int numOutputs, double learningRate,
                                         double momentumNesterov, double l1, double l2) {
        logger.info(
                "Creating nn model inputs:{}  hiddenNodes:{}  outputs:{}  learningRate:{}  momentumNesterov:{} l1:{} l2:{}",
                numInputs, numHiddenNodes, numOutputs, learningRate, momentumNesterov, l1, l2);
        MultiLayerConfiguration conf = null;

        conf = createRNNModel(numInputs, numHiddenNodes, numOutputs, learningRate, momentumNesterov, l1, l2);

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        return model;
    }
}
