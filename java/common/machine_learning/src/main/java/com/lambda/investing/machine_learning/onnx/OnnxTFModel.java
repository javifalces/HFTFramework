package com.lambda.investing.machine_learning.onnx;

import ai.onnxruntime.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


@Getter
@Setter
public class OnnxTFModel {
    protected Logger logger = LogManager.getLogger(OnnxTFModel.class);
    private String modelPath;
    private static OrtEnvironment ENV = OrtEnvironment.getEnvironment();
    OrtSession model = null;
    public boolean inputFloat = true;
    public String inputLabel = "dense_input";

    public OnnxTFModel(String modelPath) {
        this.modelPath = modelPath;
    }

    public void init() {
        loadModel();
    }

    private void loadModel() {
        File model = new File(this.modelPath);
        if (model.exists()) {
            try {
                //load it
                OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
                //				int gpuDeviceId = 0; // The GPU device ID to execute on
                //				sessionOptions.addCUDA(gpuDeviceId);
                this.model = ENV.createSession(modelPath, sessionOptions);
            } catch (OrtException e) {
                logger.error("error loadModel ", e);
                e.printStackTrace();
            }
        }

    }

    private Map<String, OnnxTensor> getInputFloat(double[] input, String inputLabel) {
        int inputLen = input.length;
        //float
        float[][] sourceArray = new float[1][inputLen];
        for (int i = 0; i < inputLen; i++) {
            sourceArray[0][i] = (float) input[i];
        }

        Map<String, OnnxTensor> inputs = null;
        try {
            OnnxTensor tensorFromArray = OnnxTensor.createTensor(ENV, sourceArray);
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
            OnnxTensor tensorFromArray = OnnxTensor.createTensor(ENV, sourceArray);
            inputs = new HashMap<>();
            inputs.put(inputLabel, tensorFromArray);
            return inputs;

        } catch (Exception e) {
            logger.error("error creating tensor from array!! ", e);
            return null;
        }

    }


    private static String InferInputLabelFromErrorMessage(OrtException ortException) {
        String errorMessage = ortException.getMessage();
        if (errorMessage.startsWith("Unknown input name")) {
            String inferInput = errorMessage.substring(errorMessage.lastIndexOf("[") + 1, errorMessage.lastIndexOf("]"));
            return inferInput;
        }
        return null;
    }

    private static boolean InferInputFloat(OrtException ortException) {
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

    public double[] predict(double[] input) {

        if (model == null) {
            logger.error("cant predict with a model created!");
            return null;
        }
        Map<String, OnnxTensor> inputs = null;
        if (!isInputFloat()) {
            inputs = getInputDouble(input, inputLabel);
        } else {
            inputs = getInputFloat(input, inputLabel);
        }
        double[] output = null;

        try (OrtSession.Result results = this.model.run(inputs)) {
            if (isInputFloat()) {
                float[][] valuesOutput = (float[][]) results.get(0).getValue();
                output = new double[valuesOutput[0].length];

                for (int index = 0; index < valuesOutput[0].length; index++) {
                    double value = valuesOutput[0][index];
                    if (Double.isNaN(value)) {
                        value = 0;
                    }
                    output[index] = value;
                }
            } else {
                double[][] valuesOutput = (double[][]) results.get(0).getValue();
                output = new double[valuesOutput[0].length];

                for (int index = 0; index < valuesOutput[0].length; index++) {
                    double value = valuesOutput[0][index];
                    if (Double.isNaN(value)) {
                        value = 0;
                    }
                    output[index] = value;
                }
            }

        } catch (OrtException e1) {
            String inferInput = InferInputLabelFromErrorMessage(e1);
            if (inferInput != null) {
                logger.warn("change inputLabel {} to {}", inputLabel, inferInput);
                inputLabel = inferInput;
                return predict(input);
            }

            if (!isInputFloat() && InferInputFloat(e1)) {
                inputFloat = true;
                return predict(input);
            }

//                    if (!errorPrediction) {
//                        System.err.println(Configuration.formatLog("OrtException error prediction model index {}/{} {} :{}", modelIndex, models, this.modelsPath[modelIndex], e.getMessage()));
//                        errorPrediction = true;
//                    }
//                    logger.error("OrtException error prediction model index {}/{} {} :{}\n", modelIndex, models, this.modelsPath[modelIndex], e.getMessage(), e);

        } catch (Exception e) {
            logger.error("Exception: error prediction model  {}:{}", model, e.getMessage(), e);
        }
        return output;
    }


}
