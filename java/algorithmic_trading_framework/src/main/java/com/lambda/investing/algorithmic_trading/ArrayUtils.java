package com.lambda.investing.algorithmic_trading;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArrayUtils {

	public static double sum(double[] array1) {
		double sum = 0; // initialize sum
		int i;

		// Iterate through all elements and add them to sum
		for (i = 0; i < array1.length; i++)
			sum += array1[i];

		return sum;

	}

	public static String PrintArrayListString(List<String> input, String delimiter) {
		return input.stream().collect(Collectors.joining(delimiter));
	}

	public static double[] ArrayFirstElementsDouble(double[] array1, int elements) {
		return org.apache.commons.lang3.ArrayUtils.subarray(array1, 0, elements);
	}

	public static double[] ArrayLastElementsDouble(double[] array1, int elements) {
		return org.apache.commons.lang3.ArrayUtils.subarray(array1, array1.length - elements, array1.length);
	}

	public static Double[] ArrayLastElementsDouble(Double[] array1, int elements) {
		return org.apache.commons.lang3.ArrayUtils.subarray(array1, array1.length - elements, array1.length);
	}

	public static List<Double> ListLastElementsDouble(List<Double> list, int elements) {
		return list.subList(list.size() - elements, list.size());
	}

	public static double[] DoubleMergeArrays(double[] array1, double[] array2) {
		double[] output = new double[array1.length + array2.length];
		System.arraycopy(array1, 0, output, 0, array1.length);
		System.arraycopy(array2, 0, output, array1.length, array2.length);
		return output;
	}

	public static double[] DoubleListToPrimitiveArray(List<Double> input) {
		return input.stream().mapToDouble(Double::doubleValue).toArray();
	}

	public static long[] LongListToPrimitiveArray(List<Long> input) {
		return input.stream().mapToLong(Long::longValue).toArray();
	}

	public static int[] IntegerListToPrimitiveArray(List<Integer> input) {
		return input.stream().mapToInt(Integer::intValue).toArray();
	}

	public static List<Integer> IntArrayList(int[] input) {
		return Arrays.stream(input).boxed().collect(Collectors.toList());

	}

	public static Integer[] IntegerListToArray(List<Integer> input) {
		Integer[] spreadArr = new Integer[input.size()];
		spreadArr = input.toArray(spreadArr);
		return spreadArr;
	}

	public static Double[] DoubleListToArray(List<Double> input) {
		Double[] spreadArr = new Double[input.size()];
		spreadArr = input.toArray(spreadArr);
		return spreadArr;
	}

	public static Double[] DoubleToNonPrimitiveArray(double[] input) {
		Double[] output = new Double[input.length];
		System.arraycopy(input, 0, output, 0, input.length);
		return output;
	}

	public static Long[] LongListToArray(List<Long> input) {
		Long[] spreadArr = new Long[input.size()];
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
