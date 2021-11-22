package com.lambda.investing.backtest;

import com.lambda.investing.algorithmic_trading.reinforcement_learning.TrainType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class TrainInputConfiguration {

	//	int numberActions,int stateColumns,String outputModelPath,double learningRate,double momentumNesterov,int nEpoch,int batchSize,double l2,double l1
	private String memoryPath, outputModelPath;
	private int actionColumns, stateColumns, nEpoch, batchSize;

	private double l2 = 0.0001;
	private double l1 = 0.;
	private double learningRate = 0.25;
	private double momentumNesterov = 0.5;
	private int trainingStats = 0;
	private boolean isRNN = false;
	private int maxBatchSize = 5000;
	private boolean hyperparameterTuning = false;
	private int rnnHorizon = -1;
	private TrainType trainType = null;

	public TrainInputConfiguration() {
	}

}
