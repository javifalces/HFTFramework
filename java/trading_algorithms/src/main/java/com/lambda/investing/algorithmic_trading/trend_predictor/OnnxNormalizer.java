package com.lambda.investing.algorithmic_trading.trend_predictor;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.lambda.investing.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.lambda.investing.algorithmic_trading.trend_predictor.OnnxModel.InferInputLabelFromErrorMessage;

public class OnnxNormalizer {
    protected static Logger logger = LogManager.getLogger(OnnxNormalizer.class);
    private OrtEnvironment env;
    private OrtSession session;
    private Random r;

    private String inputLabel = "float_input";
    private String normalizerPath;

    public OnnxNormalizer(String normalizerPath) {
        r = new Random(Configuration.RANDOM_SEED);
        this.normalizerPath = normalizerPath;


        env = OrtEnvironment.getEnvironment();
        try {
            session = env.createSession(this.normalizerPath, new OrtSession.SessionOptions());
        } catch (Exception e) {
            logger.error("error loading normalized model {}", normalizerPath);
            System.err.println(Configuration.formatLog("error loading normalized model {} {} - {}", normalizerPath, e.getMessage(), e.getCause()));
        }


    }

    public void nextSeed() {
        r = new Random(Configuration.RANDOM_SEED + 1);
    }

    public double[] transform(double[] state) {
        int outputLen = state.length;
        double[] transformedOutput = new double[outputLen];

        int inputLen = state.length;
        float[][] sourceArray = new float[1][inputLen];
        for (int i = 0; i < inputLen; i++) {
            sourceArray[0][i] = (float) state[i];
        }

        //

        Map<String, OnnxTensor> inputs = null;
        try {
            OnnxTensor tensorFromArray = OnnxTensor.createTensor(env, sourceArray);
            inputs = new HashMap<>();
            inputs.put(inputLabel, tensorFromArray);

        } catch (Exception e) {
            logger.error("error creating tensor from array!! ", e);
            return transformedOutput;
        }

        try (OrtSession.Result results = session.run(inputs)) {
            float[][] valuesOutput = (float[][]) results.get(0).getValue();

            for (int index = 0; index < valuesOutput[0].length; index++) {
                transformedOutput[index] += valuesOutput[0][index];
            }
        } catch (OrtException e1) {
            String inferInput = InferInputLabelFromErrorMessage(e1);
            if (inferInput != null) {
                logger.warn("change inputLabel {} to {}", inputLabel, inferInput);
                inputLabel = inferInput;
                return transform(state);
            }


        } catch (Exception e) {
            System.err.println(Configuration.formatLog("error transforming normalizer : {}", e.getMessage()));
            logger.error("error transforming normalizer ", e);
        }


        return transformedOutput;

    }


}
