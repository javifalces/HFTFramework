package com.lambda.investing.algorithmic_trading.reinforcement_learning;

import org.apache.commons.math3.util.MathUtils;
import org.paukov.combinatorics3.Generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import static org.apache.commons.math3.util.CombinatoricsUtils.binomialCoefficient;

public class MatrixRoundUtils {

	public static int getValuesPerColumn(int numberOfDecimals, double maxNumber, double minNumber) {
		double intNumbers = (maxNumber) - (minNumber);
		return (int) ((intNumbers * Math.pow(10, numberOfDecimals))) + 1;
	}

	public static int getNumberStates(int valuesPerColumn, int numberOfColumns) {
		//permutation with repetition
		return (int) Math.pow(valuesPerColumn, numberOfColumns);
	}

	public static List<Double> getDiffQueue(Queue<Double> input) {
		Double firstElement = null;
		List<Double> output = new ArrayList<>(input.size() - 1);

		for (double inventory : input) {
			if (firstElement == null) {
				firstElement = inventory;
			} else {
				output.add(inventory - firstElement);
				firstElement = inventory;
			}
		}
		return output;
	}

	public static double maxValue(double[] array) {
		double max = Arrays.stream(array).max().getAsDouble();
		return max;
	}

	public static double minValue(double[] array) {
		double min = Arrays.stream(array).min().getAsDouble();
		return min;
	}

	public static double meanValue(double[] array) {
		double sum = 0;
		for (double value : array) {
			sum += value;
		}
		return sum / array.length;
	}

	public static double stdValue(double[] array) {
		double mean = meanValue(array);

		double sum = 0;
		for (double value : array) {
			sum += Math.pow((value - mean), 2.);
		}
		return sum / array.length;
	}


}
