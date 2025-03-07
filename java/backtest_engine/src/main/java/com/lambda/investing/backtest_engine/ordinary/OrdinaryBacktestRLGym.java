package com.lambda.investing.backtest_engine.ordinary;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.SingleInstrumentRLAlgorithm;
import com.lambda.investing.backtest_engine.BacktestConfiguration;
import com.lambda.investing.connector.ConnectorReplier;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqReplier;
import com.lambda.investing.market_data_connector.AbstractMarketDataConnectorPublisher;
import com.lambda.investing.market_data_connector.MarketDataConnectorPublisherListener;
import com.lambda.investing.market_data_connector.parquet_file_reader.ParquetMarketDataConnectorPublisher;
import com.lambda.investing.model.exception.LambdaException;
import com.lambda.investing.model.rl_gym.InputGymMessage;
import com.lambda.investing.model.rl_gym.InputGymMessageValue;
import com.lambda.investing.model.rl_gym.OutputGymMessage;
import com.lambda.investing.model.rl_gym.StepOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static com.lambda.investing.Configuration.DELTA_REWARD_REINFORCEMENT_LEARNING;
import static com.lambda.investing.algorithmic_trading.reinforcement_learning.SingleInstrumentRLReplier.GetStateColumnsAlgorithm;
import static com.lambda.investing.model.Util.fromJsonString;
import static com.lambda.investing.model.Util.toJsonString;


public class OrdinaryBacktestRLGym implements MarketDataConnectorPublisherListener, ConnectorReplier {

    private static long TIMEOUT_START_BACKTEST_IS_READY_MS = 5 * 60 * 1000;//5 mins
    protected Logger logger = LogManager.getLogger(OrdinaryBacktestRLGym.class);
    private boolean isDone = false;
    private SingleInstrumentRLAlgorithm algorithm;

    private ZeroMqConfiguration zeroMqConfigurationReplier;//todo change it later
    private ZeroMqReplier zeroMqReplier;
    private OrdinaryBacktest ordinaryBacktest;

    private Date configured;
    private Date startReceived;
    private Date backtestIsReadyReceived;
    private Date lastActionReceived;

    private Thread timeoutThread;

    private int steps = 0;

    public OrdinaryBacktestRLGym(OrdinaryBacktest ordinaryBacktest) throws Exception {
//        super(backtestConfiguration);
        this.ordinaryBacktest = ordinaryBacktest;
        BacktestConfiguration backtestConfiguration = ordinaryBacktest.getBacktestConfiguration();
        if (!IsRLAlgorithm(backtestConfiguration)) {
            throw new LambdaException(Configuration.formatLog("algorithm {} in backtest configuration  must be instance of SingleInstrumentRLAlgorithm", backtestConfiguration.getAlgorithm().getAlgorithmInfo()));
        }
        zeroMqConfigurationReplier = backtestConfiguration.getRLZeroMqConfiguration();
        if (zeroMqConfigurationReplier == null) {
            throw new LambdaException(Configuration.formatLog("algorithm {} in backtest configuration with not valid rlPort or rlHost parameters", backtestConfiguration.getAlgorithm().getAlgorithmInfo()));
        }

        Algorithm algorithm = backtestConfiguration.getAlgorithm();

        this.algorithm = (SingleInstrumentRLAlgorithm) algorithm;

        this.algorithm.setTraining(true);
        //if instance of reinforcement learning algorithm , set the action
        zeroMqReplier = new ZeroMqReplier(zeroMqConfigurationReplier, this);
        logger.info("ZeroMqReplier created {}", zeroMqConfigurationReplier);
        configured = new Date();
        startTimeoutThread();
        pauseBacktest();
        printConfiguration();
    }

    private void startTimeoutThread() {
        Runnable methodRun = new Runnable() {
            @Override
            public void run() {
                //we check it until first action is received only
                while (lastActionReceived == null) {
                    try {
                        boolean isOutOfTime = isOutOfTime();
                        if (isOutOfTime) {
                            logger.warn("WARNING: timeout waiting first action -> kill backtest");
                            System.err.println("WARNING: timeout waiting first action -> kill backtest");
                            System.exit(-1);
                        }
                    } catch (Exception e) {
                        logger.error("error in timeoutThread", e);
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        timeoutThread = new Thread(methodRun, "OrdinaryBacktestRLGym_timeoutThread");
        timeoutThread.start();
    }

    private void printConfiguration() {
        if (Configuration.LOG_STATE_STEPS) {
            logger.info("LOG_STATE_STEPS is true -> logging all steps received/reply");
        }

        if (Configuration.DISCOUNT_REWARD_NO_TRADE) {
            logger.info("DISCOUNT_REWARD_NO_TRADE is true -> set reward to opportunity cost lost");
        }
        if (DELTA_REWARD_REINFORCEMENT_LEARNING) {
            logger.info("DELTA_REWARD_REINFORCEMENT_LEARNING is true -> set reward to change scoreColumn between steps");
        }
        logger.info("OrdinaryBacktestRLGym configured");
    }


    public OutputGymMessage getDefaultOutputGymState() {
        int stateColumns = GetStateColumnsAlgorithm(this.algorithm);
        if (stateColumns == -1) {
            logger.error("error getting stateColumns in OrdinaryBacktestRLGym.getDefaultOutputGymState , stateColumns == -1");
        }

        double[] currentState = new double[stateColumns];
        Arrays.fill(currentState, 0);

        int actionColumns = this.algorithm.action.getNumberActionColumns();
        Map<String, String> info = this.algorithm.getInfo();
        info.put("action", String.valueOf(actionColumns));

        double reward = 0.0;
        if (!DELTA_REWARD_REINFORCEMENT_LEARNING) {
            reward = algorithm.getLastReward();
        }

        return new OutputGymMessage(isDone, currentState, reward, info);
    }

    public void init() {
        logger.info("OrdinaryBacktestRLGym init...");
        this.ordinaryBacktest.registerEndOfFile(this);
        try {
            zeroMqReplier.start();
            System.out.println("OrdinaryBacktestRLGym : " + zeroMqConfigurationReplier.toString());
            System.out.println("waiting start ...");
        } catch (IOException e) {
            logger.error("error in OrdinaryBacktestRLGym.zeroMqReplier init ", e);
            System.err.println("error in OrdinaryBacktestRLGym.zeroMqReplier init ");
            e.printStackTrace();
        }

    }

    public static boolean IsRLAlgorithm(BacktestConfiguration backtestConfiguration) {
        Algorithm algorithm = backtestConfiguration.getAlgorithm();
        //if instance of reinforcement learning algorithm , set the action
        return algorithm instanceof SingleInstrumentRLAlgorithm;
    }

    public void notifyEndOfFile() {
        isDone = true;
        algorithm.endOfBacktest();
//        pauseBacktest();//its already stopped
        logger.info("OrdinaryBacktestRLGym notifyEndOfFile isDone sent received:{}", zeroMqConfigurationReplier.toString());
        System.out.println("OrdinaryBacktestRLGym notifyEndOfFile isDone sent received : " + zeroMqConfigurationReplier.toString());


    }

//    public void startBacktest() {
//        System.out.println("OrdinaryBacktestRLGym start received : " + zeroMqConfigurationReplier.toString());
//        resumeBacktest();
//    }

    public void reset() {
        logger.info("reset OrdinaryBacktestRLGym -> forceStopBacktest");
        forceStopBacktest();

        steps = 0;
        isDone = false;
        Configuration.BACKTEST_MESSAGE_PRINT = null;

//        algorithm.resetAlgorithm();//reset algorithm and dictionaries MarketDataProviders
        resumeBacktest();//continue with the next episode backtest
    }


    private void pauseBacktest() {
        AbstractMarketDataConnectorPublisher.setPauseRLGym(true);
    }

    private void resumeBacktest() {
        ParquetMarketDataConnectorPublisher.setPauseRLGym(false);
    }

    private void forceStopBacktest() {
        isDone = true;
        ParquetMarketDataConnectorPublisher.FORCE_STOP_BACKTEST = true;
        algorithm.setWaitDone(false);
    }

    public StepOutput step(double[] action) {
        resumeBacktest();//resume backtest to continue the backtest on the  action from python code
        StepOutput stepOutput = algorithm.step(action);
        if (algorithm.isDone()) {
            logger.info("algorithm is done detected because stop and out of time => FORCE_STOP_BACKTEST");
            forceStopBacktest();
        }
        pauseBacktest();//pause backtest to wait python code to process the step
        return stepOutput;
    }


    public String reply(long timestampReceived, String content) {
        if (algorithm.inOperationalTime()) {
            //only print it in operational time to avoid unnecessary logs
            logger.info("received {}[{}] content {}", new Date(timestampReceived), timestampReceived, content);
        }
        //map json content into InputGymMessage object
        InputGymMessage inputGymMessage = null;
        try {
            inputGymMessage = fromJsonString(content, InputGymMessage.class);
        } catch (Exception e) {
            InputGymMessageValue inputGymMessageValue = fromJsonString(content, InputGymMessageValue.class);
            inputGymMessage = new InputGymMessage(inputGymMessageValue);
        }

        String output = "KO";

        //if content is reset, reset the algorithm
        if (inputGymMessage.getType().equals("reset")) {
            logger.info("OrdinaryBacktestRLGym reset received");
            System.out.println("JAVA: reset received");
            reset();
            output = toJsonString(getDefaultOutputGymState());
        } else if (inputGymMessage.getType().equals("start")) {
            logger.info("OrdinaryBacktestRLGym start received");
            System.out.println("JAVA: start received");
            this.startReceived = new Date();
            output = toJsonString(getDefaultOutputGymState());
        } else if (inputGymMessage.getType().equals("action")) {
            this.lastActionReceived = new Date();
            double[] action = inputGymMessage.getValue();
            OutputGymMessage outputGymMessage = null;
            if (isDone) {
                logger.info("backtest isDone detected before setting action ->setWaitDone to false and return getDefaultOutputGymState");
                algorithm.setWaitDone(false);
                outputGymMessage = getDefaultOutputGymState();
            } else {
                StepOutput stepOutput = step(action);
                steps++;
                Configuration.BACKTEST_MESSAGE_PRINT = "[" + steps + "]";
                Map<String, String> info = algorithm.getInfo();
                outputGymMessage = new OutputGymMessage(isDone, stepOutput.getState(), stepOutput.getReward(), info);
                if (isDone) {
                    logger.info("backtest isDone detected after setting action ->setWaitDone to false to exit backtest");
                    algorithm.setWaitDone(false);
                }
            }

            output = toJsonString(outputGymMessage);

        } else if (inputGymMessage.getType().equals("backtest_is_ready")) {
            backtestIsReadyReceived = new Date();
            System.out.println("JAVA: backtest_is_ready received. Waiting backtestIsReady...");
            logger.info("backtest_is_ready received waiting ...", new Date(timestampReceived), timestampReceived, content);
            Date startTime = new Date();
            // wait backtest is ready
            while (!AbstractMarketDataConnectorPublisher.isBacktestReady()) {
                Thread.onSpinWait();
            }
            long elapsed = new Date().getTime() - startTime.getTime();
            System.out.println("JAVA:  backtest_is_ready finished");
            logger.info("backtest_is_ready answer after {} ms ", elapsed);
            output = toJsonString(getDefaultOutputGymState());

        } else {
            logger.error("unknown message type {} {}", inputGymMessage.getType(), content);
        }

        if (Configuration.LOG_STATE_STEPS) {
            long currentTimestamp = System.currentTimeMillis();
            long elapsedMs = currentTimestamp - timestampReceived;
            logger.info("LOG_STATE_STEPS received {}[{}] content {}", new Date(timestampReceived), timestampReceived, content);
            logger.info("LOG_STATE_STEPS reply {} [{}] after {} real ms content {}", new Date(), System.currentTimeMillis(), elapsedMs, output);
        }

        return output;
    }

    private boolean isOutOfTime() {
        if (lastActionReceived != null) {
            return false;
        }
        if (configured == null) {
            return false;
        }

        long elapsed = new Date().getTime() - configured.getTime();
        return elapsed > TIMEOUT_START_BACKTEST_IS_READY_MS;
    }

}
