package com.lambda.investing.algorithmic_trading;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileUtils {

	protected static Logger logger = LogManager.getLogger(FileUtils.class);

	public static void persistArray(double[][] inputData, String filepath, String csvSeparator) {
		File file = new File(filepath);
		file.getParentFile().mkdirs();
		StringBuilder outputString = new StringBuilder();
		for (int row = 0; row < inputData.length; row++) {
			for (int column = 0; column < inputData[row].length; column++) {
				outputString.append(inputData[row][column]);
				outputString.append(csvSeparator);
			}
			outputString.append(System.lineSeparator());
		}

		outputString = outputString.delete(outputString.lastIndexOf(System.lineSeparator()),
				outputString.length());//remove last line separator

		String content = outputString.toString();
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filepath));

			try {
				writer.write(content);
			} catch (Exception e) {
				logger.error("error saving array values to file {} ", filepath, e);
			} finally {
				writer.close();
			}
		} catch (IOException e) {
			logger.error("error IOException on BuffredWriter {} ", filepath, e);
		}
	}

	public static double[][] loadArrayFromPath(String filepath, String csvSeparator) throws IOException {

		BufferedReader csvReader = new BufferedReader(new FileReader(filepath));
		// we don't know the amount of data ahead of time so we use lists

		Map<Integer, List<Double>> colMap = new HashMap<>();
		String row;
		int rowsTotal = 0;

		while ((row = csvReader.readLine()) != null) {
			String[] data = row.split(csvSeparator);
			for (int column = 0; column < data.length; column++) {
				List<Double> columnList = colMap.getOrDefault(column, new ArrayList<>());
				columnList.add(Double.parseDouble(data[column]));
				colMap.put(column, columnList);
			}
			rowsTotal++;
		}
		csvReader.close();
		int columnsTotal = colMap.size();

		//transform colMap into array
		double[][] output = new double[rowsTotal][columnsTotal];//states rows , actions columns

		for (int column : colMap.keySet()) {
			List<Double> rows = colMap.get(column);
			int rowIter = 0;
			for (double rowVal : rows) {
				output[rowIter][column] = rowVal;
				rowIter++;
			}
		}

		return output;

	}

	/*
	 * Concatenates 2 2D arrays
	 */
	public static double[][] arrayConcatVertical(double[][] a, double[][] b) {

		double[][] arr = new double[a.length][a[0].length + b[0].length];
		for (int i = 0; i < a.length; i++) {
			arr[i] = arrayConcat(a[i], b[i]);
		}
		return arr;

	}

	/*
	 * Concatenates 2 1D arrays
	 */
	public static double[] arrayConcat(double[] a, double[] b) {
		double[] arr = new double[a.length + b.length];
		System.arraycopy(a, 0, arr, 0, a.length);
		System.arraycopy(b, 0, arr, a.length, b.length);
		return arr;
	}

}
