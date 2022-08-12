package com.lambda.investing.algorithmic_trading.reinforcement_learning;

public interface MemoryReplayModel {

	boolean updateGradient(double[][] input, double[][] output, double[][] expected);

	boolean train(double[][] input, double[][] target);

	double[] predict(double[] input);

	void setModelPath(String modelPath);

	String getModelPath();

	MemoryReplayModel cloneIt(String modelPath);

	void loadModel();

	void saveModel();

	boolean isTrained();


	int getMaxBatchSize();

	int getBatchSize();

	void setSeed(long seed);

	long getSeed();

	int getEpoch();

	void setParameterTuningBeforeTraining(boolean value);

	void setEarlyStoppingTraining(boolean value);

	void setHiddenSizeNodesMultiplier(double value);

}
