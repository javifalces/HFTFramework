package com.lambda.investing.algorithmic_trading.reinforcement_learning;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorReplier;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqReplier;
import com.lambda.investing.model.rl_gym.InputGymMessage;
import com.lambda.investing.model.rl_gym.InputGymMessageValue;
import com.lambda.investing.model.rl_gym.OutputGymMessage;
import com.lambda.investing.model.rl_gym.StepOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.lambda.investing.model.portfolio.Portfolio.GSON_STRING;

public class SingleInstrumentRLReplier implements ConnectorReplier {
    protected static Map<ZeroMqConfiguration, SingleInstrumentRLReplier> INSTANCES = new HashMap<>();
    protected Logger logger = LogManager.getLogger(SingleInstrumentRLReplier.class);

    private ZeroMqConfiguration zeroMqConfiguration;
    private ZeroMqReplier zeroMqReplier;
    private SingleInstrumentRLAlgorithm algorithm;

    public static SingleInstrumentRLReplier GetInstance(ZeroMqConfiguration zeroMqConfiguration, SingleInstrumentRLAlgorithm algorithm) throws IOException {
        if (INSTANCES.containsKey(zeroMqConfiguration)) {
            return INSTANCES.get(zeroMqConfiguration);
        } else {
            SingleInstrumentRLReplier instance = new SingleInstrumentRLReplier(zeroMqConfiguration, algorithm);
            INSTANCES.put(zeroMqConfiguration, instance);
            return instance;
        }
    }

    private SingleInstrumentRLReplier(ZeroMqConfiguration zeroMqConfiguration, SingleInstrumentRLAlgorithm algorithm) throws IOException {
        this.zeroMqConfiguration = zeroMqConfiguration;
        this.algorithm = algorithm;
        this.zeroMqReplier = new ZeroMqReplier(this.zeroMqConfiguration, this);
        logger.info("ZeroMqReplier created {}", this.zeroMqConfiguration);
        this.zeroMqReplier.start();
    }

    public static int GetStateColumnsAlgorithm(SingleInstrumentRLAlgorithm algorithm) {
        long timeoutSeconds = 30;
        int stateColumns = algorithm.state.getNumberOfColumns();
        long start = System.currentTimeMillis();
        while (stateColumns == 0 || algorithm.state == null) {
            if (algorithm.state != null) {
                stateColumns = algorithm.state.getNumberOfColumns();
            }
            if (System.currentTimeMillis() - start > timeoutSeconds * 1000) {
                System.err.println("error in getStateColumnsAlgorithm timeout- > return -1");
                return -1;
            }
            Thread.onSpinWait();
        }

        return stateColumns;
    }

    public OutputGymMessage getDefaultOutputGymState() {
        int stateColumns = GetStateColumnsAlgorithm(algorithm);
        if (stateColumns == -1) {
            logger.error("error getting stateColumns in SingleInstrumentRLReplier.getDefaultOutputGymState , stateColumns == -1");
        }

        double[] currentState = new double[stateColumns];
        Arrays.fill(currentState, 0);

        Map<String, String> info = this.algorithm.getInfo();
        int actionColumns = this.algorithm.action.getNumberActionColumns();
        info.put("action", String.valueOf(actionColumns));

        return new OutputGymMessage(false, currentState, 0.0, info);
    }

    @Override
    public String reply(long timestampReceived, String content) {
        String output = null;
        while (this.algorithm.state == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (algorithm.isReady()) {
            logger.info("received {}[{}] content {}", new Date(timestampReceived), timestampReceived, content);
        }
        //map json content into InputGymMessage object
        InputGymMessage inputGymMessage = null;
        try {
            InputGymMessageValue inputGymMessageValue = GSON_STRING.fromJson(content, InputGymMessageValue.class);
            inputGymMessage = new InputGymMessage(inputGymMessageValue);
        } catch (Exception e) {
            inputGymMessage = GSON_STRING.fromJson(content, InputGymMessage.class);
        }
        if (inputGymMessage.getType().equals("action") && algorithm.isReady()) {
            double[] action = inputGymMessage.getValue();
            StepOutput stepOutput = this.algorithm.step(action);
            Map<String, String> info = this.algorithm.getInfo();
            output = GSON_STRING.toJson(new OutputGymMessage(false, stepOutput.getState(), stepOutput.getReward(), info));
        } else {
            //return something to keep the connection alive
            OutputGymMessage defaultOutputGymMessage = getDefaultOutputGymState();
            output = GSON_STRING.toJson(defaultOutputGymMessage);
        }

        if (Configuration.LOG_STATE_STEPS) {
            logger.info("LOG_STATE_STEPS received {} content {}", new Date(timestampReceived), content);
            logger.info("LOG_STATE_STEPS reply answered {} content {}", new Date(), output);
        }

        return output;

    }


}
