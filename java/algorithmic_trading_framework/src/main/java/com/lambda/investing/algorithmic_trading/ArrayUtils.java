package com.lambda.investing.algorithmic_trading;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayUtils {

	public static double[] DoubleMergeArrays(double[] array1, double[] array2) {
		double[] output = new double[array1.length + array2.length];
		System.arraycopy(array1, 0, output, 0, array1.length);
		System.arraycopy(array2, 0, output, array1.length, array2.length);
		return output;
	}

	public static double[] DoubleListToPrimitiveArray(List<Double> input) {
		return input.stream().mapToDouble(Double::doubleValue).toArray();
	}

	public static int[] IntegerListToPrimitiveArray(List<Integer> input) {
		return input.stream().mapToInt(Integer::intValue).toArray();
	}

	public static List<Integer> IntArrayList(int[] input) {
		return Arrays.stream(input).boxed().collect(Collectors.toList());

	}

	public static Double[] DoubleListToArray(List<Double> input) {
		Double[] spreadArr = new Double[input.size()];
		spreadArr = input.toArray(spreadArr);
		return spreadArr;
	}

	public static String[] StringListToArray(List<String> input) {
		String[] output = new String[input.size()];
		return input.toArray(output);
	}

	public static List<String> StringArrayList(String[] input) {
		return Arrays.stream(input).collect(Collectors.toList());
	}

}
