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

public class OnnxModel {

    protected boolean RANDOM_PREDICTOR = false;//for testing the rest
    protected double RANDOM_MIN_TREND_TRESHOLD = 0.6;
    protected static Logger logger = LogManager.getLogger(OnnxModel.class);

    private String[] modelsPath;
    private int outputLen = -1;

    private int models = -1;
    private OrtEnvironment env;
    private OrtSession[] sessions;
    private Random r;

    private boolean errorPrediction = false;

    private boolean isFloat = false;

    private String inputLabel;

    public OnnxModel(String[] modelsPath, int outputLen, String inputLabel) {
        r = new Random(Configuration.RANDOM_SEED);
        this.modelsPath = modelsPath;
        this.outputLen = outputLen;
        this.inputLabel = inputLabel;
        if (RANDOM_PREDICTOR) {
            return;
        }

        this.models = this.modelsPath.length;
        env = OrtEnvironment.getEnvironment();
        sessions = new OrtSession[this.models];
        for (int i = 0; i < this.models; i++) {
            try {
                OrtSession.SessionOptions options = new OrtSession.SessionOptions();
                options.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.PARALLEL);


                sessions[i] = env.createSession(this.modelsPath[i], options);
            } catch (Exception e) {
                logger.error("error loading model {}", modelsPath[i]);
                System.err.println(Configuration.formatLog("error loading model {} {} - {}", modelsPath[i], e.getMessage(), e.getCause()));

            }
        }

    }

    public void setFloat(boolean aFloat) {
        isFloat = aFloat;
    }

    public void nextSeed() {
        r = new Random(Configuration.RANDOM_SEED + 1);
    }

    public double[] predict(double[] state) {
        double[] probTrend = new double[outputLen];
        //RANDOM!
        if (RANDOM_PREDICTOR) {
            if (r.nextDouble() > RANDOM_MIN_TREND_TRESHOLD) {
                int index = r.nextInt(outputLen - 1);
                index = Math.max(index, 2);

                probTrend[index] = 0.8 + (r.nextDouble() / 10.0);
                probTrend[index - 1] = 0.7 + (r.nextDouble() / 10.0);
                probTrend[index - 2] = 0.5 + (r.nextDouble() / 10.0);

            }

            //USING MODEL!!!
        } else {
            Map<String, OnnxTensor> inputs = null;
            if (!isFloat) {
                inputs = getInputDouble(state, inputLabel);
            } else {
                inputs = getInputFloat(state, inputLabel);
            }


            for (int modelIndex = 0; modelIndex < this.models; modelIndex++) {
                try (OrtSession.Result results = sessions[modelIndex].run(inputs)) {
                    if (isFloat) {
                        float[][] valuesOutput = (float[][]) results.get(0).getValue();
                        for (int index = 0; index < valuesOutput[0].length; index++) {
                            probTrend[index] += valuesOutput[0][index];
                        }
                    } else {
                        double[][] valuesOutput = (double[][]) results.get(0).getValue();
                        for (int index = 0; index < valuesOutput[0].length; index++) {
                            probTrend[index] += valuesOutput[0][index];
                        }
                    }

                } catch (OrtException e1) {
                    String inferInput = InferInputLabelFromErrorMessage(e1);
                    if (inferInput != null) {
                        logger.warn("change inputLabel {} to {}", inputLabel, inferInput);
                        inputLabel = inferInput;
                        return predict(state);
                    }

                    if (!isFloat && InferInputFloat(e1)) {
                        isFloat = true;
                        return predict(state);
                    }

//                    if (!errorPrediction) {
//                        System.err.println(Configuration.formatLog("OrtException error prediction model index {}/{} {} :{}", modelIndex, models, this.modelsPath[modelIndex], e.getMessage()));
//                        errorPrediction = true;
//                    }
//                    logger.error("OrtException error prediction model index {}/{} {} :{}\n", modelIndex, models, this.modelsPath[modelIndex], e.getMessage(), e);

                } catch (Exception e) {
                    if (!errorPrediction) {
                        System.err.println(Configuration.formatLog("Exception: error prediction model index {}/{} {} :{}", modelIndex, models, this.modelsPath[modelIndex], e.getMessage()));
                        errorPrediction = true;
                    }
                    logger.error("Exception: error prediction model index {}/{} {} :{}\n", modelIndex, models, this.modelsPath[modelIndex], e.getMessage(), e);
                }
            }

            //average it
            if (models > 1) {
                for (int futurePredIndex = 0; futurePredIndex < this.outputLen; futurePredIndex++) {
                    probTrend[futurePredIndex] = probTrend[futurePredIndex] / models;
                }
            }

        }


        return probTrend;

    }


    public static String InferInputLabelFromErrorMessage(OrtException ortException) {
        String errorMessage = ortException.getMessage();
        if (errorMessage.startsWith("Unknown input name")) {
            String inferInput = errorMessage.substring(errorMessage.lastIndexOf("[") + 1, errorMessage.lastIndexOf("]"));
            return inferInput;
        }
        return null;
    }

    public static boolean InferInputFloat(OrtException ortException) {
        String errorMessage = ortException.getMessage();
        if (errorMessage.contains("Unexpected input data type")) {
            if (errorMessage.endsWith("expected: (tensor(float))")) {
                return true;
            }
//            if(errorMessage.endsWith("expected: (tensor(double))")){
//                return false;
//            }


        }
        return false;

    }

    /**
     * @param input
     * @param inputLabel inputLabel
     * @return
     */
    private Map<String, OnnxTensor> getInputFloat(double[] input, String inputLabel) {
        int inputLen = input.length;
        //float
        float[][] sourceArray = new float[1][inputLen];
        for (int i = 0; i < inputLen; i++) {
            sourceArray[0][i] = (float) input[i];
        }

        Map<String, OnnxTensor> inputs = null;
        try {
            OnnxTensor tensorFromArray = OnnxTensor.createTensor(env, sourceArray);
            inputs = new HashMap<>();
            inputs.put(inputLabel, tensorFromArray);
            return inputs;

        } catch (Exception e) {
            logger.error("error creating tensor from array!! ", e);
            return null;
        }

    }

    private Map<String, OnnxTensor> getInputDouble(double[] input, String inputLabel) {
        int inputLen = input.length;
        //double
        double[][] sourceArray = new double[1][inputLen];
        for (int i = 0; i < inputLen; i++) {
            sourceArray[0][i] = input[i];
        }

        Map<String, OnnxTensor> inputs = null;
        try {
            OnnxTensor tensorFromArray = OnnxTensor.createTensor(env, sourceArray);
            inputs = new HashMap<>();
            inputs.put(inputLabel, tensorFromArray);
            return inputs;

        } catch (Exception e) {
            logger.error("error creating tensor from array!! ", e);
            return null;
        }

    }

}
