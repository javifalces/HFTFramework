package com.lambda.investing.algorithmic_trading;


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlgorithmUtils {

	public static Map<String, Object> getParameters(Map<String, Object> inputParameters) {
		Map<String, Object> output = new HashMap<>();
		for (String parameterKey : inputParameters.keySet()) {
			Object value = inputParameters.get(parameterKey);

			if (value instanceof List) {
				String commaSeparated = (String) ((List) value).stream().map(String::valueOf)
						.collect(Collectors.joining(","));

				output.put(parameterKey, commaSeparated);

			} else {
				output.put(parameterKey, String.valueOf(value));
			}

		}
		return output;
	}


	public static class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {

		private final int maxSize;

		public MaxSizeHashMap(int maxSize) {
			this.maxSize = maxSize;
		}

		@Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
			return size() > maxSize;
		}
	}

}
