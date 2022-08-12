package com.lambda.investing.algorithmic_trading.reinforcement_learning.environment;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.AlgorithmState;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.RL4JAlgoritm;
import com.lambda.investing.market_data_connector.AbstractMarketDataProvider;
import com.lambda.investing.market_data_connector.parquet_file_reader.ParquetMarketDataConnectorPublisher;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.learning.IEpochTrainer;
import org.deeplearning4j.rl4j.learning.ILearning;
import org.deeplearning4j.rl4j.learning.configuration.QLearningConfiguration;
import org.deeplearning4j.rl4j.learning.listener.TrainingListener;
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.network.configuration.DQNDenseNetworkConfiguration;
import org.deeplearning4j.rl4j.policy.DQNPolicy;
import org.deeplearning4j.rl4j.space.Box;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.space.ObservationSpace;
import org.deeplearning4j.rl4j.util.DataManager;
import org.deeplearning4j.rl4j.util.IDataManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

public class EnvironmentDiscreteActionSpace implements MDP<Box, Integer, DiscreteSpace>, TrainingListener {
    protected Logger logger = LogManager.getLogger(EnvironmentDiscreteActionSpace.class);

    protected DataManager manager;
    @Getter
    private DiscreteSpace actionSpace;
    @Getter
    private ObservationSpace<Box> observationSpace;
    RL4JAlgoritm algorithm;
    QLearningDiscreteDense<Box> dql;

    DQNPolicy<Box> pol;

    private ThreadPoolExecutor executorAction;

    private boolean isStarted = false;
    private int trainingIterations = 0;
    protected StepReply<Box> defaultStepResponse;
    protected List<Double> rewardsTraining = new ArrayList<>();
    public EnvironmentDiscreteActionSpace(RL4JAlgoritm algorithm, DiscreteSpace actionSpace, ObservationSpace<Box> observationSpace) {
        this.actionSpace = actionSpace;
        this.observationSpace = observationSpace;
        this.algorithm = algorithm;


        dql = new QLearningDiscreteDense(this, (DQNDenseNetworkConfiguration) algorithm.getNetworkConfiguration(), (QLearningConfiguration) algorithm.getLearningConfiguration());
        try {
            manager = new DataManager(true);
        } catch (IOException e) {
            logger.error("error creating dataManager ", e);
        }
        dql.addListener(this);

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("StartEnv --%d").setPriority(Thread.MAX_PRIORITY).build();
        executorAction = (ThreadPoolExecutor) Executors.newFixedThreadPool(1, namedThreadFactory);

        int stateColumns = this.observationSpace.getShape()[0];
        Box defaultState = new Box(new int[stateColumns]);
        defaultStepResponse = new StepReply<>(defaultState, 0, false, null);
    }


    @Override
    public Box reset() {
        // reset backtest after initialized
        return this.algorithm.reset();
    }

    @Override
    public void close() {
        this.algorithm.close();
    }

    @Override
    public StepReply<Box> step(Integer integer) {
        try {
            return this.algorithm.step(integer);
        } catch (Exception e) {
            logger.error("error on step with action {}-> return default value", integer, e);
            return defaultStepResponse;
        }
    }

    @Override
    public boolean isDone() {
        return (isStarted && algorithm.isDone());
    }


    @Override
    public MDP<Box, Integer, DiscreteSpace> newInstance() {
        return new EnvironmentDiscreteActionSpace(algorithm, actionSpace, observationSpace);
    }

    private void train() {
        //define the training
        rewardsTraining.clear();
        ParquetMarketDataConnectorPublisher.BUCLE_RUN = true;//to not finished
        AbstractMarketDataProvider.CHECK_TIMESTAMPS_RECEIVED = false;

        algorithm.IsTraining();
        dql.train();
        pol = dql.getPolicy();
        try {
            pol.save(algorithm.getPolicyPath());
            logger.info("finished trained after {} iterations policy on {}", trainingIterations, algorithm.getPolicyPath());
        } catch (IOException e) {
            logger.error("cant save policy on {}", algorithm.getPolicyPath(), e);
        }
        trainingIterations = 0;
    }

    public void start() {

        executorAction.submit(() -> {
            isStarted = true;
            String policyPath = algorithm.getPolicyPath();
            boolean forceTrain = algorithm.isForceTraining();

            if (new File(policyPath).exists() && !forceTrain) {
                try {
                    System.out.println(Configuration.formatLog("Testing ({} found)....", algorithm.getPolicyPath()));
                    logger.info("loading testing from {} ", algorithm.getPolicyPath());
                    pol = DQNPolicy.load(algorithm.getPolicyPath());
                    double reward = pol.play(this);
                    logger.info("finished testing with reward :{}", reward);
                    System.out.println(Configuration.formatLog("Finished testing with reward {}-> {}", reward, algorithm.getPolicyPath()));

                } catch (Exception e) {
                    logger.error("cant load policy from {} -> train again!", algorithm.getPolicyPath(), e);
                }
            } else {
                long start = System.currentTimeMillis();
                logger.info(" forceTraining {} or policy not found on {} -> train", forceTrain, algorithm.getPolicyPath());
                System.out.println(Configuration.formatLog("Training ({} not found or forceTrain {})....", algorithm.getPolicyPath(), forceTrain));
                train();
                long elapsed = System.currentTimeMillis() - start;
                logger.info("finished training");
                System.out.println(Configuration.formatLog("Finished training after {} iterations {} minutes -> {}", trainingIterations, elapsed / (1000 * 60), algorithm.getPolicyPath()));
            }
            System.exit(0);
        });

    }
    //On the begining only,first iteration
    @Override
    public ListenerResponse onTrainingStart() {
        ParquetMarketDataConnectorPublisher.PAUSE = true;
        return null;
    }

    @Override
    public void onTrainingEnd() {
        System.out.println(Configuration.formatLog("training finished after {} iterations", trainingIterations));
        logger.info("training finished after {} iterations ", trainingIterations);
    }

    @Override
    public ListenerResponse onNewEpoch(IEpochTrainer trainer) {
//        System.out.println(Configuration.formatLog("[{}]starting new epoch training", trainingIterations));
        ParquetMarketDataConnectorPublisher.PAUSE = false;
        return null;
    }

    @Override
    public ListenerResponse onEpochTrainingResult(IEpochTrainer trainer, IDataManager.StatEntry statEntry) {
        System.out.println(Configuration.formatLog("[{}] onEpochTrainingResult new epoch training -> {}", trainingIterations, statEntry.toString()));
        logger.info("[{}] onEpochTrainingResult new epoch training -> {}", trainingIterations, statEntry.toString());
        algorithm.finishedTrainingIteration();
        algorithm.setDone(false);//
        trainingIterations++;
        double lastReward = statEntry.getReward();
        rewardsTraining.add(lastReward);
        ParquetMarketDataConnectorPublisher.PAUSE = true;
        boolean trainingIterationsFinished = trainingIterations >= algorithm.getMaxTrainingIterations();
        boolean epsilonMaxStepsFinished = statEntry.getStepCounter() >= algorithm.getMaxSteps();
        if (trainingIterationsFinished || epsilonMaxStepsFinished) {
            if (trainingIterationsFinished)
                logger.info("trainingIterations {} > MaxTrainingIterations {}", trainingIterations, algorithm.getMaxTrainingIterations());
            if (epsilonMaxStepsFinished)
                logger.info("steps {} > MaxSteps {}", statEntry.getStepCounter(), algorithm.getMaxSteps());

            logger.info("rewards per iterations:  {} ", rewardsTraining.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));

            return ListenerResponse.STOP;
        }
        algorithm.restartPortfolioManager();
        //if listenerResponse is status stop -> finished it
        return ListenerResponse.CONTINUE;
    }

    @Override
    public ListenerResponse onTrainingProgress(ILearning learning) {
        return null;
    }


}
