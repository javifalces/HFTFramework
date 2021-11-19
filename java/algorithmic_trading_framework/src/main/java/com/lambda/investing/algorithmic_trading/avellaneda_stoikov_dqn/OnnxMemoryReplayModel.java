package com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn;

import ai.onnxruntime.*;
import com.lambda.investing.algorithmic_trading.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * https://www.codeproject.com/Articles/5278506/Using-Portable-ONNX-AI-Models-in-Java
 */
public class OnnxMemoryReplayModel implements Cloneable, MemoryReplayModel {

	protected Logger logger = LogManager.getLogger(OnnxMemoryReplayModel.class);
	private static String CSV_SEPARATOR = " ";
	private static String DEFAULT_TRAINING_SCRIPT_PYTHON = "onnx_train_model.py";
	private String modelPath;
	private String trainingPythonScriptPath = DEFAULT_TRAINING_SCRIPT_PYTHON;

	OrtEnvironment env = OrtEnvironment.getEnvironment();
	OrtSession model = null;
	private UUID fileGenerator = UUID.randomUUID();
	private String pythonExePath = "python3";

	private static void launchPythonScript(String scriptPath, String... args) {

	}

	public OnnxMemoryReplayModel(String modelPath, String trainingPythonScriptPath) {
		this.modelPath = modelPath;
		if (trainingPythonScriptPath == null) {
			trainingPythonScriptPath = DEFAULT_TRAINING_SCRIPT_PYTHON;
		}
		this.trainingPythonScriptPath = trainingPythonScriptPath;

		loadModel();

	}

	@Override public void setModelPath(String modelPath) {
		this.modelPath = modelPath;
	}

	public void loadModel() {
		//todo onnx load
		File model = new File(this.modelPath);
		if (model.exists()) {
			try {
				//load it
				OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
				//				int gpuDeviceId = 0; // The GPU device ID to execute on
				//				sessionOptions.addCUDA(gpuDeviceId);
				this.model = env.createSession(modelPath, sessionOptions);
			} catch (OrtException e) {
				logger.error("error loadModel ", e);
				e.printStackTrace();
			}
		}

	}

	public void saveModel() {
		//done in python on training
	}

	@Override public boolean isTrained() {
		File model = new File(this.modelPath);
		return model.exists();
	}

	@Override public int getMaxBatchSize() {
		throw new NotImplementedException();
	}

	@Override public int getBatchSize() {
		throw new NotImplementedException();
	}

	@Override public void setSeed(long seed) {
		return;
	}

	public void persistArray(String filepath, double[][] persistArray) throws IOException {
		FileUtils.persistArray(persistArray, filepath, CSV_SEPARATOR);
	}

	/***
	 *  https://github.com/onnx/tutorials/blob/master/tutorials/TensorflowToOnnx-1.ipynb
	 * @param input
	 * @param target
	 */
	public void train(double[][] input, double[][] target) {
		//todo persist matrix to file
		String inputPath = fileGenerator.toString() + ".csv";
		String targetPath = fileGenerator.toString() + ".csv";
		File inputPathFile = new File(inputPath);
		File targetPathFile = new File(targetPath);
		try {
			persistArray(inputPath, input);
			persistArray(targetPath, target);
			//launch python command
			String[] cmd = { pythonExePath, this.trainingPythonScriptPath, inputPath, targetPath, this.modelPath };
			Process p = Runtime.getRuntime().exec(cmd);
			while (p.isAlive()) {
				Thread.sleep(100);
			}
			loadModel();//load onnx file
		} catch (IOException e) {
			logger.error("cant launch pythonm script to train", e);
			e.printStackTrace();
		} catch (InterruptedException e) {
			logger.error("cant sleep waiting python training script ", e);
		} finally {
			//delete inputFile and target
			if (inputPathFile.exists()) {
				inputPathFile.delete();
			}
			if (targetPathFile.exists()) {
				targetPathFile.delete();
			}

		}

		//update model
		return;
	}

	public double[] predict(double[] input) {

		if (model == null) {
			logger.error("cant predict with a model created!");
			return null;
		}

		Map<String, OnnxTensor> inputDataOnnx = null;
		try {
			inputDataOnnx = preprocessData(input);
			OrtSession.Result results = model.run(inputDataOnnx);

			return postprocessData(results);
		} catch (OrtException e) {
			e.printStackTrace();
		}
		return null;

	}

	private Map<String, OnnxTensor> preprocessData(double[] inputData) throws OrtException {
		Map<String, NodeInfo> inputMetaMap = model.getInputInfo();
		Map<String, OnnxTensor> container = new HashMap<>();
		NodeInfo inputMeta = inputMetaMap.values().iterator().next();
		OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
		container.put(inputMeta.getName(), inputTensor);
		return container;
	}

	private double[] postprocessData(OrtSession.Result results) throws OrtException {
		double[] output = new double[results.size()];
		int index = 0;
		for (Map.Entry<String, OnnxValue> r : results) {
			OnnxValue resultValue = r.getValue();
			OnnxTensor resultTensor = (OnnxTensor) resultValue;
			double prediction = (double) resultTensor.getValue();
			output[index] = prediction;
			index++;
		}
		return output;

	}

	public OnnxMemoryReplayModel cloneIt() {
		try {
			return (OnnxMemoryReplayModel) this.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

}
