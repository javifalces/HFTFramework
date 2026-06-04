package com.lambda.investing.algorithmic_trading;

public interface AlgorithmProvider {
    void stopAlgo();

    void startAlgo();

    boolean changeBacktestSpeed(int speed);
    boolean changeParameters(String jsonInput);
}
