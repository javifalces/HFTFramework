package com.lambda.investing.algorithmic_trading;

public enum AlgorithmState {
	NOT_INITIALIZED(-1), INITIALIZING(0), INITIALIZED(1), STARTING(2), STARTED(3), STOPPING(2), STOPPED(3);

	private int number;

	AlgorithmState(int number) {
		this.number = number;
	}

	public int getNumber() {
		return number;
	}
}
