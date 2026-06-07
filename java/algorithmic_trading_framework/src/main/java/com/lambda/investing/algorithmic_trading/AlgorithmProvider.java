package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.model.trading.Verb;

public interface AlgorithmProvider {
    void stopAlgo();

    void startAlgo();

    boolean changeBacktestSpeed(int speed);
    boolean changeParameters(String jsonInput);

    boolean cancelOrder(String clientOrderId);

    boolean closeTrade(String instrumentPk, Verb verb, double quantity);

    boolean closePosition(String instrumentPk, double position);
}
