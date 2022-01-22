package com.lambda.investing.algorithmic_trading.reinforcement_learning;

import com.google.common.primitives.Doubles;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.DeepQLearning.DEFAULT_PREDICTION_ACTION_SCORE;
import static com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.QLearning.CSV_SEPARATOR;

public class TrainNNUtils {

	protected static Logger logger = LogManager.getLogger(TrainNNUtils.class);
	public static boolean TRAIN_FROM_FILE_MODEL = false;
	public static TrainType DEFAULT_TRAIN_TYPE = TrainType.standard;

	private static double[][] loadCSV(String filepath, int columnsStates) throws IOException {
		//only used on trainOnData
		File file = new File(filepath);
		if (!file.exists()) {
			logger.warn("memory not found {}-> start empty", filepath);
			return null;
		}

		BufferedReader csvReader = new BufferedReader(new FileReader(filepath));
		// we don't know the amount of data ahead of time so we use lists

		Map<Integer, List<Double>> colMap = new HashMap<>();
		String row;
		int rowsTotal = 0;

		while ((row = csvReader.readLine()) != null) {
			String[] data = row.split(CSV_SEPARATOR);
			double[] stateRow = new double[columnsStates];
			for (int column = 0; column < data.length; column++) {
				List<Double> columnList = colMap.getOrDefault(column, new ArrayList<>());
				double value = Double.parseDouble(data[column]);
				columnList.add(value);
				colMap.put(column, columnList);

				if (column < stateRow.length) {
					stateRow[column] = value;
				}

			}

			rowsTotal++;
		}
		csvReader.close();
		int columnsTotal = colMap.size();

		//transform colMap into array
		double[][] loadedQvalues = new double[rowsTotal][columnsTotal];//states rows , actions columns
		int rowsFilled = 0;
		for (int column : colMap.keySet()) {
			List<Double> rows = colMap.get(column);
			int rowIter = 0;
			for (double rowVal : rows) {
				loadedQvalues[rowIter][column] = rowVal;
				rowsFilled = rowIter;
				rowIter++;

			}
		}

		//		loadedQvalues=ArrayUtils.subarray(loadedQvalues, 0, rowsTotal);

		System.out.println(
				String.format("loaded a memory replay of %d/%d rows-states   and %d states-actions-next-states",
						rowsFilled, loadedQvalues.length, loadedQvalues[0].length));

		return loadedQvalues;

	}

	public static double[][] getColumnsArray(double[][] input, int firstColumn, int lastColumn) {
		double[][] output = new double[input.length][lastColumn - firstColumn];
		for (int row = 0; row < input.length; row++) {
			for (int column = firstColumn; column < lastColumn; column++) {
				// index starts from 0
				output[row][column - firstColumn] = input[row][column];
			}
		}
		return output;
	}

	public static int argmax(double[] array) {
		double max = array[0];
		int re = 0;
		for (int i = 1; i < array.length; i++) {
			if (array[i] > max) {
				max = array[i];
				re = i;
			}
		}
		return re;
	}

	public static int argmin(double[] array) {
		double min = array[0];
		int re = 0;
		for (int i = 1; i < array.length; i++) {
			if (array[i] < min) {
				min = array[i];
				re = i;
			}
		}
		return re;
	}

	public static double[][] getTargetClassification(double[][] targetArr) {
		double[][] targetArrOutput = new double[targetArr.length][targetArr[0].length];
		for (int row = 0; row < targetArr.length; row++) {
			double bestColumn = Doubles.max(targetArr[row]);
			int bestAction = argmax(targetArr[row]);
			boolean bestIsPositive = true;
			if (bestColumn == DEFAULT_PREDICTION_ACTION_SCORE || bestColumn < 0) {
				bestIsPositive = false;
			}


			if (bestIsPositive) {
				targetArrOutput[row][bestAction] = 1.0;
			} else {
				int worstAction = argmin(targetArr[row]);
				//rest are better
				double valueToSet = 1.0;/// (targetArr[row].length - 1);
				for (int column = 0; column < targetArr[row].length; column++) {
					if (column == worstAction || targetArrOutput[row][column] < 0) {
						targetArrOutput[row][column] = 0;
						continue;
					}
					if (targetArrOutput[row][column] == DEFAULT_PREDICTION_ACTION_SCORE) {
						targetArrOutput[row][column] = valueToSet;
					}

				}
			}

		}
		return targetArrOutput;

	}

	public static double[][] getArrayValid(double[][] inputWithTarget, int stateNumberOfColumns, int actions,
			boolean cleanIt) {
		double[][] targetRaw = getColumnsArray(inputWithTarget, stateNumberOfColumns, stateNumberOfColumns + actions);

		double[][] outputArr = inputWithTarget.clone();
		if (cleanIt) {
			int rowsDeleted = 0;
			for (int row = 0; row < targetRaw.length; row++) {
				double sumRewardsState = 0.0;
				double bestColumn = Doubles.max(targetRaw[row]);
				for (int column = 0; column < targetRaw[row].length; column++) {
					sumRewardsState += targetRaw[row][column];
				}
				if (sumRewardsState == DEFAULT_PREDICTION_ACTION_SCORE
						|| bestColumn == DEFAULT_PREDICTION_ACTION_SCORE) {
					int indexToDelete = row - rowsDeleted;
					outputArr = ArrayUtils.remove(outputArr, indexToDelete);
					rowsDeleted++;
				}
			}
			logger.info("input {} rows -> output {} rows", inputWithTarget.length, outputArr.length);
		}
		return outputArr;
	}

	@Deprecated public static double[][] getArrayValid(double[][] inputWithTarget, int stateNumberOfColumns,
			int actions) {
		return getArrayValid(inputWithTarget, stateNumberOfColumns, actions, false);
	}

	public static boolean trainOnData(TrainType trainType, String memoryPath, int actionColumns, int stateColumns,
			String outputModelPath, double learningRateNN, double momentumNesterov, int nEpoch, int batchSize,
			int maxBatchSize, double l2, double l1, int trainingStats, boolean isRNN, boolean isHyperParameterTuning,
			int rnnHorizon) throws IOException {

		System.out.println(trainType.name() + " training");
		if (trainType == TrainType.standard) {
			return trainOnDataStandard(memoryPath, actionColumns, stateColumns, outputModelPath, learningRateNN,
					momentumNesterov, nEpoch, batchSize, maxBatchSize, l2, l1, trainingStats, isRNN,
					isHyperParameterTuning, rnnHorizon);
		} else if (trainType == TrainType.custom_actor_critic) {
			return trainOnDataCustomActorCritic(memoryPath, actionColumns, stateColumns, outputModelPath,
					learningRateNN, momentumNesterov, nEpoch, batchSize, maxBatchSize, l2, l1, trainingStats, isRNN,
					isHyperParameterTuning, rnnHorizon);
		} else {
			System.err.println("trainType method not found " + trainType);
		}

		System.err.println("trainType method not found " + trainType);
		logger.error("trainType method not found " + trainType);
		return false;

	}

	private static boolean trainOnDataStandard(String memoryPath, int actionColumns, int stateColumns,
			String outputModelPath, double learningRateNN, double momentumNesterov, int nEpoch, int batchSize,
			int maxBatchSize, double l2, double l1, int trainingStats, boolean isRNN, boolean isHyperParameterTuning,
			int rnnHorizon) throws IOException {

		File file = new File(memoryPath);
		if (!file.exists()) {
			System.err.println(memoryPath + " not exist to train");
			return false;
		}
		double[][] memoryData = loadCSV(memoryPath, stateColumns);
		//check load dimension
		int columnsRead = memoryData[0].length;
		assert columnsRead == (stateColumns * 2) + actionColumns;

		memoryData = getArrayValid(memoryData, stateColumns, actionColumns);//clean it

		if (batchSize <= 0 && memoryData != null) {
			batchSize = Math.min(512, memoryData.length / 2);
		}

		Dl4jMemoryReplayModel memoryReplayModel = new Dl4jMemoryReplayModel(outputModelPath, learningRateNN,
				momentumNesterov, nEpoch, batchSize, maxBatchSize, l2, l1, TRAIN_FROM_FILE_MODEL, isRNN);
		System.out.println("training standard prediction/target....");
		if (trainingStats != 0) {
			memoryReplayModel.setTrainingStats(true);
		}
		if (isHyperParameterTuning) {
			System.out.println("Hyperparameter tuning detected!  activate EARLY_STOPPING and disable training stats");
			Dl4jMemoryReplayModel.HYPERPARAMETER_TUNING = true;
			Dl4jMemoryReplayModel.EARLY_STOPPING = true;
			memoryReplayModel.setTrainingStats(false);
		}

		double[][] x = getColumnsArray(memoryData, 0, stateColumns);
		double[][] y = getColumnsArray(memoryData, stateColumns, stateColumns + actionColumns);
		logger.info("starting training model with {} epoch on {} batch", nEpoch, batchSize);
		//		System.out.println("training on data with   rows:"+x.length+"  columns:"+x[0].length+"  epochs:"+nEpoch+"  batchSize:"+batchSize+"  maxBatchSize:"+maxBatchSize);
		long start = System.currentTimeMillis();
		memoryReplayModel.train(x, y);
		long elapsed = (System.currentTimeMillis() - start) / (1000 * 60);
		logger.info("trained finished on {} minutes ,saving model {}", elapsed, outputModelPath);
		memoryReplayModel.saveModel();
		return true;

	}

	private static boolean trainOnDataCustomActorCritic(String memoryPath, int actionColumns, int stateColumns,
			String outputModelPath, double learningRateNN, double momentumNesterov, int nEpoch, int batchSize,
			int maxBatchSize, double l2, double l1, int trainingStats, boolean isRNN, boolean isHyperParameterTuning,
			int rnnHorizon) throws IOException {

		File file = new File(memoryPath);
		if (!file.exists()) {
			System.err.println(memoryPath + " not exist to train");
			return false;
		}

		///// input DATA
		double[][] memoryData = loadCSV(memoryPath, stateColumns);
		//check load dimension
		int columnsRead = memoryData[0].length;
		assert columnsRead == (stateColumns * 2) + actionColumns;
		memoryData = getArrayValid(memoryData, stateColumns, actionColumns);//clean it
		if (batchSize <= 0 && memoryData != null) {
			batchSize = Math.min(512, memoryData.length / 2);
		}
		double[][] x = getColumnsArray(memoryData, 0, stateColumns);
		double[][] y = getColumnsArray(memoryData, stateColumns, stateColumns + actionColumns);
		double[][] yPredict = getTargetClassification(y);

		//PREDICT -> classification
		System.out.println("training prediction....");
		Dl4jClassificationMemoryReplayModel predictMemoryReplayModel = new Dl4jClassificationMemoryReplayModel(
				outputModelPath, learningRateNN, momentumNesterov, nEpoch, batchSize, maxBatchSize, l2, l1,
				TRAIN_FROM_FILE_MODEL, isRNN);
		if (trainingStats != 0) {
			predictMemoryReplayModel.setTrainingStats(true);
		}
		if (isHyperParameterTuning) {
			System.out.println("Hyperparameter tuning detected!  activate EARLY_STOPPING and disable training stats");
			Dl4jClassificationMemoryReplayModel.HYPERPARAMETER_TUNING = true;
			Dl4jClassificationMemoryReplayModel.EARLY_STOPPING = true;
			predictMemoryReplayModel.setTrainingStats(false);
		}

		logger.info("starting training predict model with {} epoch on {} batch", nEpoch, batchSize);
		//		System.out.println("training on data with   rows:"+x.length+"  columns:"+x[0].length+"  epochs:"+nEpoch+"  batchSize:"+batchSize+"  maxBatchSize:"+maxBatchSize);
		long start = System.currentTimeMillis();
		predictMemoryReplayModel.train(x, yPredict);
		long elapsed = (System.currentTimeMillis() - start) / (1000 * 60);
		logger.info("Predict trained finished on {} minutes ,saving model {}", elapsed, outputModelPath);
		predictMemoryReplayModel.saveModel();

		//TARGET -> classification
		System.out.println("training target....");
		String targetOutput = outputModelPath.replace("predict", "target");
		Dl4jMemoryReplayModel targetMemoryReplayModel = new Dl4jMemoryReplayModel(targetOutput, learningRateNN,
				momentumNesterov, nEpoch, batchSize, maxBatchSize, l2, l1, TRAIN_FROM_FILE_MODEL, isRNN);
		if (trainingStats != 0) {
			targetMemoryReplayModel.setTrainingStats(true);
		}
		if (isHyperParameterTuning) {
			System.out.println("Hyperparameter tuning detected!  activate EARLY_STOPPING and disable training stats");
			Dl4jMemoryReplayModel.HYPERPARAMETER_TUNING = true;
			Dl4jMemoryReplayModel.EARLY_STOPPING = true;
			targetMemoryReplayModel.setTrainingStats(false);
		}
		logger.info("starting training target model with {} epoch on {} batch", nEpoch, batchSize);
		//		System.out.println("training on data with   rows:"+x.length+"  columns:"+x[0].length+"  epochs:"+nEpoch+"  batchSize:"+batchSize+"  maxBatchSize:"+maxBatchSize);
		start = System.currentTimeMillis();
		targetMemoryReplayModel.train(x, y);
		elapsed = (System.currentTimeMillis() - start) / (1000 * 60);
		logger.info("Target trained finished on {} minutes ,saving model {}", elapsed, targetOutput);
		targetMemoryReplayModel.saveModel();

		return true;

	}

}
