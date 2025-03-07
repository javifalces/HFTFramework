package com.lambda.investing.gym;


import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmParameters;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.SingleInstrumentRLAlgorithm;
import com.lambda.investing.backtest.InputConfiguration;
import com.lambda.investing.backtest_engine.BacktestConfiguration;
import com.lambda.investing.backtest_engine.ordinary.OrdinaryBacktestRLGym;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqReplier;
import com.lambda.investing.connector.zero_mq.ZeroMqRequester;
import com.lambda.investing.model.rl_gym.InputGymMessage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Map;

import static com.lambda.investing.Configuration.logger;
import static com.lambda.investing.model.Util.fromJsonString;
import static com.lambda.investing.model.Util.toJsonString;


@Getter
@Setter
public class DummyRlAgent implements Runnable {

    private long delayStepMs = 1000;
    private long initialSleepSeconds = 1;
    private boolean enable = true;
    private String launcherJson;
    private ZeroMqConfiguration zeroMqConfiguration = null;
    private Algorithm algorithm;
    private boolean dummyAgent = false;

    private InputConfiguration inputConfiguration = null;
    private ZeroMqRequester zeroMqRequester = null;
    private int actionColumns;
    private int numberActions;
    private boolean discreteAction = false;

    public DummyRlAgent(String launcherJson) {

        this.launcherJson = launcherJson;
        inputConfiguration = fromJsonString(launcherJson, InputConfiguration.class);
        try {
            BacktestConfiguration backtestConfiguration = inputConfiguration.getBacktestConfiguration();

            ZeroMqConfiguration rlGymConfig = backtestConfiguration.getRLZeroMqConfiguration();
            boolean IsRLAlgorithm = OrdinaryBacktestRLGym.IsRLAlgorithm(backtestConfiguration);

            if (rlGymConfig != null && IsRLAlgorithm) {
                algorithm = backtestConfiguration.getAlgorithm();
                actionColumns = ((SingleInstrumentRLAlgorithm) algorithm).getAction().getNumberActionColumns();
                numberActions = ((SingleInstrumentRLAlgorithm) algorithm).getAction().getNumberActions();
                Map<String, Object> parameters = algorithm.getParameters();
                this.dummyAgent = algorithm.getParameterIntOrDefault(parameters, "dummyAgent", "dummy_agent", 0) > 0;
                if (this.dummyAgent) {
                    String reinforcementLearningActionType = algorithm.getParameterStringOrDefault(parameters, "reinforcementLearningActionType", "discrete");
                    discreteAction = reinforcementLearningActionType.equalsIgnoreCase("discrete");
                    String messagePrint = Configuration.formatLog("DUMMY_AGENT {} ENABLED: actionColumns={} numberActions={} ", reinforcementLearningActionType, actionColumns, numberActions);
                    System.out.println(messagePrint);
                    logger.info(messagePrint);
                    zeroMqConfiguration = rlGymConfig;//its the same as configured
                }
            }

        } catch (Exception e) {
            logger.error("DummyRlAgent: error in constructor {} \n{}", e.getMessage(), launcherJson, e);
            e.printStackTrace();
        }

        if (dummyAgent) {
            zeroMqRequester = new ZeroMqRequester();
        }
    }

    private double[] randomDiscreteAction() {
        double[] action = new double[1];
        //set in action[0] a random integer between 0 and actionColumns
        action[0] = (int) (Math.random() * numberActions);
        return action;
    }
    private double[] randomAction() {
        if (isDiscreteAction()) {
            return randomDiscreteAction();
        }
        double[] action = new double[actionColumns];
        for (int i = 0; i < actionColumns; i++) {
            action[i] = Math.random() * 2 - 1;
            ;
        }
        return action;
    }

    @Override
    public void run() {
        if (!dummyAgent) {
            return;
        }
        try {
            Thread.sleep(initialSleepSeconds * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        InputGymMessage readyMessageRequest = new InputGymMessage();
        readyMessageRequest.setType("backtest_is_ready");
        readyMessageRequest.setValue(new double[0]);
        zeroMqRequester.request(zeroMqConfiguration, toJsonString(readyMessageRequest));

        InputGymMessage startMessageRequest = new InputGymMessage();
        startMessageRequest.setType("start");
        startMessageRequest.setValue(new double[0]);
        zeroMqRequester.request(zeroMqConfiguration, toJsonString(startMessageRequest));

        int iterations = 0;
        while (dummyAgent) {

            //do something sending requests

            InputGymMessage stepMessageRequest = new InputGymMessage();
            stepMessageRequest.setType("action");
            stepMessageRequest.setValue(randomAction());
            String receive = zeroMqRequester.request(zeroMqConfiguration, toJsonString(stepMessageRequest));
            MessageReceive messageReceive = fromJsonString(receive, MessageReceive.class);
            if (messageReceive.done) {
                String messagePrint = Configuration.formatLog("DUMMY_AGENT done : {} iterations", iterations);
                System.out.println(messagePrint);
                logger.info(messagePrint);
                return;
            }

            iterations++;
            if (delayStepMs > 0) {
                try {
                    Thread.sleep(delayStepMs);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Thread.onSpinWait();
            }

        }


    }

    @Setter
    @Getter
    @NoArgsConstructor

    private class MessageReceive {
        boolean done;
        double[] state;
        double reward;
        Map<String, Object> info;

        public MessageReceive(boolean done, double[] state, double reward, Map<String, Object> info) {
            this.done = done;
            this.state = state;
            this.reward = reward;
            this.info = info;
        }

        @Override
        public String toString() {
            return toJsonString(this);
        }
    }

}
