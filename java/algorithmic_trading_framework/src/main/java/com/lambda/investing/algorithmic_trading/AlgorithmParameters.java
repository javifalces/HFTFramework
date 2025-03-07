package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlgorithmParameters {
    private static String SEPARATOR_ARRAY_PARAMETERS = ",";
    private static String START_ARRAY_PARAMETERS = "[";
    private static String END_ARRAY_PARAMETERS = "]";

    protected static Logger logger = LogManager.getLogger(AlgorithmParameters.class);

    public static double getParameterDoubleOrDefault(Map<String, Object> parameters, String key, double defaultValue) {
        String value = String.valueOf(parameters.getOrDefault(key, String.valueOf(defaultValue)));
        if (value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return defaultValue;
        }
        try {
            return Double.valueOf(value);
        } catch (Exception e) {
            System.err.println(String.format("wrong parameter %s with value %s", key, value));
            throw e;
        }
    }


    public static double getParameterDoubleOrDefault(Map<String, Object> parameters, String key, String secondKey,
                                                 double defaultValue) {
        if (!parameters.containsKey(key)) {
            double output = getParameterDoubleOrDefault(parameters, secondKey, defaultValue);
            if (output != defaultValue) {
                logger.warn("deprecated parameter key: {} better use {}", secondKey, key);
            }
            return output;
        }

        String value = String.valueOf(parameters.getOrDefault(key, String.valueOf(defaultValue)));
        if (value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return defaultValue;
        }
        try {
            return Double.valueOf(value);
        } catch (Exception e) {
            System.err.println(String.format("wrong parameter %s with value %s", key, value));
            throw e;
        }
    }

    public static double getParameterDouble(Map<String, Object> parameters, String key) {
        String value = String.valueOf(parameters.get(key));

        try {
            return Double.valueOf(value);
        } catch (Exception e) {
            System.err.println(String.format("wrong parameter %s with value %s", key, value));
            throw e;
        }
    }

    public static double getParameterDouble(Map<String, Object> parameters, String key, String secondKey) {
        if (!parameters.containsKey(key)) {
            logger.warn("deprecated parameter key: {} better use {}", secondKey, key);
            return getParameterDouble(parameters, secondKey);
        }

        String value = String.valueOf(parameters.get(key));

        try {
            return Double.valueOf(value);
        } catch (Exception e) {
            System.err.println(String.format("wrong parameter %s with value %s", key, value));
            throw e;
        }
    }

    public static int getParameterIntOrDefault(Map<String, Object> parameters, String key, int defaultValue) {
        String value = String.valueOf(parameters.getOrDefault(key, String.valueOf(defaultValue)));
        if (value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return defaultValue;
        }
        try {
            //boolean case
            if (value.equalsIgnoreCase("TRUE")) {
                return 1;
            }
            if (value.equalsIgnoreCase("FALSE")) {
                return 0;
            }

            return (int) Math.round(Double.valueOf(value));
        } catch (Exception e) {
            System.err.println(String.format("wrong parameter %s with value %s", key, value));
            throw e;
        }
    }

    public static int getParameterIntOrDefault(Map<String, Object> parameters, String key, String secondKey,
                                        int defaultValue) {
        if (!parameters.containsKey(key)) {
            int output = getParameterIntOrDefault(parameters, secondKey, defaultValue);
            if (output != defaultValue) {
                logger.warn("deprecated parameter key: {} better use {}", secondKey, key);
            }
            return output;
        }

        String value = String.valueOf(parameters.getOrDefault(key, String.valueOf(defaultValue)));
        if (value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return defaultValue;
        }
        try {
            return (int) Math.round(Double.valueOf(value));
        } catch (Exception e) {
            System.err.println(String.format("wrong parameter %s with value %s", key, value));
            throw e;
        }
    }

    public static int getParameterInt(Map<String, Object> parameters, String key) {
        String value = String.valueOf(parameters.get(key));
        try {
            if (key.equalsIgnoreCase("TRUE")) {
                return 1;
            }
            if (key.equalsIgnoreCase("FALSE")) {
                return 0;
            }

            return (int) Math.round(Double.valueOf(value));
        } catch (Exception e) {
            System.err.println(String.format("wrong parameter %s with value %s", key, value));
            throw e;
        }

    }

    public static int getParameterInt(Map<String, Object> parameters, String key, String secondKey) {
        if (!parameters.containsKey(key)) {
            int output = getParameterIntOrDefault(parameters, secondKey, -666);
            if (output != -666) {
                logger.warn("deprecated parameter key: {} better use {}", secondKey, key);
            }
            return output;
        }

        String value = String.valueOf(parameters.get(key));

        try {
            return (int) Math.round(Double.valueOf(value));
        } catch (Exception e) {
            System.err.println(String.format("wrong parameter %s with value %s", key, value));
            throw e;
        }

    }

    public static Object getParameterObject(Map<String, Object> parameters, String key) {
        return parameters.get(key);
    }

    public static String getParameterString(Map<String, Object> parameters, String key) {
        String output = String.valueOf(parameters.get(key));
        if (output.equalsIgnoreCase("null")) {
            return null;
        }
        return output;
    }

    public static String getParameterString(Map<String, Object> parameters, String key, String secondKey) {
        if (!parameters.containsKey(key)) {
            String output = getParameterString(parameters, secondKey);
            if (output != null) {
                logger.warn("deprecated parameter key: {} better use {}", secondKey, key);
            }
            return output;
        }


        String output = String.valueOf(parameters.get(key));
        if (output.equalsIgnoreCase("null")) {
            return null;
        }
        return output;
    }

    public static String getParameterStringOrDefault(Map<String, Object> parameters, String key, String defaultValue) {
        String value = String.valueOf(parameters.getOrDefault(key, defaultValue));
        if (value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return defaultValue;
        }
        try {
            return value;
        } catch (Exception e) {
            System.err.println(String.format("wrong parameter %s with value %s", key, value));
            throw e;
        }
    }

    public static String getParameterStringOrDefault(Map<String, Object> parameters, String key, String secondKey,
                                              String defaultValue) {
        if (!parameters.containsKey(key)) {
            String output = getParameterStringOrDefault(parameters, secondKey, defaultValue);
            if (defaultValue != null && !output.equals(defaultValue)) {
                logger.warn("deprecated parameter key: {} better use {}", secondKey, key);
            }
            return output;
        }

        String value = String.valueOf(parameters.getOrDefault(key, defaultValue));
        if (value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return defaultValue;
        }
        try {
            return value;
        } catch (Exception e) {
            System.err.println(String.format("wrong parameter %s with value %s", key, value));
            throw e;
        }
    }

    public static String[] getParameterArrayString(Map<String, Object> parameters, String key) {
        String value = String.valueOf(parameters.get(key));
        if (value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return null;
        }
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            String[] output = null;
            value = value.replace(START_ARRAY_PARAMETERS, "").replace(END_ARRAY_PARAMETERS, "");
            if (!value.contains(SEPARATOR_ARRAY_PARAMETERS)) {
                parameters.put(key, value);
                try {
                    String valueIn = getParameterString(parameters, key);
                    output = new String[]{valueIn};
                } catch (Exception e) {
                    logger.error("error reading getParameterArrayString on key {} and value {} -> return null", key,
                            value, e);
                    return null;
                }
            } else {
                String[] splitted = value.split(SEPARATOR_ARRAY_PARAMETERS);
                output = new String[splitted.length];
                for (int index = 0; index < splitted.length; index++) {
                    output[index] = splitted[index];
                }
            }
            return output;
        } catch (NullPointerException e) {
            System.err.println(String.format("wrong parameter %s with value %s", key, value));
            throw e;
        }
    }

    public static double[] getParameterArrayDouble(Map<String, Object> parameters, String key) {
        String value = String.valueOf(parameters.get(key));
        if (value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return null;
        }
        try {
            double[] output = null;
            value = value.replace(START_ARRAY_PARAMETERS, "").replace(END_ARRAY_PARAMETERS, "");
            if (!value.contains(SEPARATOR_ARRAY_PARAMETERS)) {
                parameters.put(key, value);
                try {
                    double valueIn = getParameterDouble(parameters, key);
                    output = new double[]{valueIn};
                } catch (Exception e) {
                    logger.error("error reading getParameterArrayDouble on key {} and value {} -> return null", key,
                            value, e);
                    return null;
                    //					output = new double[0];
                }
            } else {
                String[] splitted = value.split(SEPARATOR_ARRAY_PARAMETERS);
                output = new double[splitted.length];
                List<Double> outputList = new ArrayList<>();
                for (int index = 0; index < splitted.length; index++) {
                    if (splitted[index] != null)
                        outputList.add(Double.valueOf(splitted[index]));
                }
                output = ArrayUtils.DoubleListToPrimitiveArray(outputList);

            }
            return output;
        } catch (NullPointerException e) {
            System.err.println(String.format("wrong parameter %s with value %s", key, value));
            throw e;
        }
    }

    public static int[] getParameterArrayInt(Map<String, Object> parameters, String key) {
        String value = String.valueOf(parameters.get(key));
        if (value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return null;
        }
        try {
            int[] output = null;
            value = value.replace(START_ARRAY_PARAMETERS, "").replace(END_ARRAY_PARAMETERS, "");
            if (!value.contains(SEPARATOR_ARRAY_PARAMETERS)) {
                parameters.put(key, value);
                try {
                    int valueIn = getParameterInt(parameters, key);
                    output = new int[]{valueIn};
                } catch (Exception e) {
                    logger.error("error reading getParameterArrayInt on key {} and value {} -> return null", key, value,
                            e);
                    return null;
                }
            } else {
                String[] splitted = value.split(SEPARATOR_ARRAY_PARAMETERS);
                output = new int[splitted.length];
                for (int index = 0; index < splitted.length; index++) {
                    output[index] = (int) Math.round(Double.valueOf(splitted[index]));
                }
            }
            return output;
        } catch (NullPointerException e) {
            System.err.println(String.format("wrong parameter %s with value %s", key, value));
            throw e;
        }
    }

}
