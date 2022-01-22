package com.lambda.investing.backtest;

import com.lambda.investing.algorithmic_trading.reinforcement_learning.TrainType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class TrainInputConfiguration {

	private static double learningRateDefault = 0.25;
	//	int numberActions,int stateColumns,String outputModelPath,double learningRate,double momentumNesterov,int nEpoch,int batchSize,double l2,double l1
	private String memoryPath, outputModelPath;
	private int actionColumns, stateColumns, nEpoch, batchSize;

	private double l2 = 0.0001;
	private double l1 = 0.;
	private double learningRate = learningRateDefault;
	private double learningRateNN = 0.25;
	private double momentumNesterov = 0.5;
	private int trainingStats = 0;
	private boolean isRNN = false;
	private int maxBatchSize = 5000;
	private boolean hyperparameterTuning = false;
	private int rnnHorizon = -1;
	private TrainType trainType = null;

	public double getLearningRate() {
		if (learningRate == learningRateDefault) {
			return getLearningRateNN();
		}
		return learningRate;
	}

	public TrainInputConfiguration() {
	}

}
