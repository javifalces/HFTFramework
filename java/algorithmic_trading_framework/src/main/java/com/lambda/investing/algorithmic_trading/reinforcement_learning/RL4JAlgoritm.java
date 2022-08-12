package com.lambda.investing.algorithmic_trading.reinforcement_learning;

import com.lambda.investing.algorithmic_trading.AlgorithmState;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.learning.configuration.LearningConfiguration;
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning;
import org.deeplearning4j.rl4j.network.configuration.NetworkConfiguration;
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense;
import org.deeplearning4j.rl4j.space.Box;

public interface RL4JAlgoritm {
    /**
     * @param integer action number
     * @return Box with states array + reward + isDone + info
     */
    StepReply<Box> step(Integer integer);

    String getPolicyPath();

    LearningConfiguration getLearningConfiguration();

    NetworkConfiguration getNetworkConfiguration();

    Box reset();

    void close();

    AlgorithmState getAlgorithmState();

    void IsTraining();

    void setDone(boolean done);

    boolean isDone();

    int getMaxTrainingIterations();

    int getMaxSteps();

    boolean isForceTraining();

    void restartPortfolioManager();

    void finishedTrainingIteration();

}
