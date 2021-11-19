package com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn;

public interface MemoryReplayModel {

	void train(double[][] input, double[][] target);

	double[] predict(double[] input);

	void setModelPath(String modelPath);

	MemoryReplayModel cloneIt();

	void loadModel();

	void saveModel();

	boolean isTrained();

	int getMaxBatchSize();

	int getBatchSize();

	void setSeed(long seed);

}
