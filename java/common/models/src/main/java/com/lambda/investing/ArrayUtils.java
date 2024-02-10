package com.lambda.investing;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayUtils {

//    public static Queue<Double> changeLastElementQueue(Queue<Double> input, double newValue) {
//        Double[] outputArray = input.toArray(new Double[input.size()]);
//        outputArray[outputArray.length - 1] = newValue;
//        input = Queues.newArrayDeque(Arrays.asList(outputArray));
//        return input;
//    }
//    public static Queue<Long> changeLastElementQueue(Queue<Long> input, Long newValue) {
//        Long[] outputArray = input.toArray(new Long[input.size()]);
//        outputArray[outputArray.length - 1] = newValue;
//        input = Queues.newArrayDeque(Arrays.asList(outputArray));
//        return input;
//    }
//
//    public static double getLastElementQueue(Queue<Double> input) {
//        double lastElement = input.toArray(new Double[input.size()])[input.size() - 1];
//        return lastElement;
//    }

    public static double sum(double[] array1) {
        double sum = 0; // initialize sum
        int i;

        // Iterate through all elements and add them to sum
        for (i = 0; i < array1.length; i++)
            sum += array1[i];

        return sum;
    }

    public static Long sum(Long[] array1) {
        Long sum = 0L; // initialize sum
        int i;

        // Iterate through all elements and add them to sum
        for (i = 0; i < array1.length; i++)
            sum += array1[i];

        return sum;
    }

    public static <T> int getNonNullLength(T[] arr) {
        int count = 0;
        for (T el : arr)
            if (el != null)
                ++count;
        return count;
    }
    public static <T> String PrintArrayListString(List<T> input, String delimiter) {
        return input.stream().map(String::valueOf)
                .collect(Collectors.joining(delimiter));
    }

    public static String PrintListDoubleArrayString(List<double[]> input, String delimiterIn, String delimiterOut) {
        StringBuffer bufferAction = new StringBuffer();
        for( double[] action :input){
            bufferAction.append("[");
            bufferAction.append(PrintDoubleArrayString(action, delimiterIn));
            bufferAction.append("]");
            bufferAction.append(delimiterOut);
        }
        return bufferAction.toString();

    }

    public static String PrintDoubleArrayString(double[] input, String delimiter) {
        StringBuffer output = new StringBuffer();
        for (int i = 0; i < input.length; i++) {
            output.append(input[i]);
            if (i < input.length - 1) {
                output.append(delimiter);
            }
        }
        return output.toString();
    }

    public static Double[] ArrayReverse(Double[] array1) {
        //copy array1 into new array
        Double[] array2 = new Double[array1.length];
        System.arraycopy(array1, 0, array2, 0, array1.length);

        org.apache.commons.lang3.ArrayUtils.reverse(array2);
        return array2;
    }

    public static double[] ArrayReverse(double[] array1) {
        double[] array2 = new double[array1.length];
        System.arraycopy(array1, 0, array2, 0, array1.length);

        org.apache.commons.lang3.ArrayUtils.reverse(array1);
        return array1;
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

    public static <T> List<T> ArrayToList(T[] input) {
        return Arrays.stream(input).collect(Collectors.toList());
    }

    public static List<String> StringArrayList(String[] input) {
        return Arrays.stream(input).collect(Collectors.toList());
    }

    public static Object[] RemoveLevelAndShiftToLeft(Object[] input, int level) {
        for (int nextLevel = level; nextLevel < input.length - 1; nextLevel++) {
            input[nextLevel] = input[nextLevel + 1];
        }
        input[input.length - 1] = null; //remove last element
        return input;
    }


}
