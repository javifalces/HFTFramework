package com.lambda.investing.algorithmic_trading;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lambda.investing.market_data_connector.parquet_file_reader.ParquetMarketDataConnectorPublisher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.event.ChangeEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link AlgorithmProvider} that delegates lifecycle
 * and parameter-change requests to a concrete {@link Algorithm} instance.
 *
 * <p>Instances are singletons keyed by {@link Algorithm#getAlgorithmInfo()}.
 * Use {@link #getInstanceOrCreate(Algorithm)} to obtain or create the instance
 * for a given algorithm.
 */
public class AlgorithmProviderImpl implements AlgorithmProvider {

    private static final Logger logger = LogManager.getLogger(AlgorithmProviderImpl.class);

    private static final ConcurrentHashMap<String, AlgorithmProviderImpl> INSTANCES = new ConcurrentHashMap<>();

    public static AlgorithmProviderImpl getInstance(String algorithmInfo) {
        return INSTANCES.get(algorithmInfo);
    }

    /**
     * Returns the existing singleton for the algorithm identified by
     * {@code algorithm.getAlgorithmInfo()}, or creates and registers a new one.
     *
     * @param algorithm the algorithm instance; must not be {@code null}
     * @return the singleton {@link AlgorithmProviderImpl} for that algorithm
     */
    public static AlgorithmProviderImpl getInstanceOrCreate(Algorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm must not be null");
        }
        return INSTANCES.computeIfAbsent(algorithm.getAlgorithmInfo(),
                key -> new AlgorithmProviderImpl(algorithm));
    }

    private final Algorithm algorithm;

    private AlgorithmProviderImpl(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public void startAlgo() {
        logger.info("AlgorithmProviderImpl.startAlgo() called for {}", algorithm.getAlgorithmInfo());
        algorithm.manualStart();
    }

    @Override
    public void stopAlgo() {
        logger.info("AlgorithmProviderImpl.stopAlgo() called for {}", algorithm.getAlgorithmInfo());
        algorithm.manualStop();
    }

    /**
     * Parses {@code jsonInput} as a JSON object and applies each key-value pair to
     * the algorithm's parameter map.  For each entry:
     * <ul>
     *   <li>If the key does not currently exist in the algorithm's parameters a
     *       warning is printed / logged and the method returns {@code false}.</li>
     *   <li>If the supplied value cannot be cast to the type of the existing
     *       parameter a warning is printed / logged and the method returns
     *       {@code false}.</li>
     *   <li>Otherwise {@link Algorithm#setParameter(String, Object)} is called
     *       with a properly typed value.</li>
     * </ul>
     *
     * @param jsonInput a JSON object string, e.g. {@code {"spread":0.002,"levels":3}}
     * @return {@code true} if every key-value pair was applied successfully,
     * {@code false} otherwise
     */
    @Override
    public boolean changeParameters(String jsonInput) {
        if (jsonInput == null || jsonInput.trim().isEmpty()) {
            String msg = "changeParameters called with null or empty JSON input";
            System.out.println("WARNING: " + msg);
            logger.warn(msg);
            return false;
        }

        JSONObject parsed;
        try {
            parsed = JSON.parseObject(jsonInput);
        } catch (Exception e) {
            String msg = "changeParameters: failed to parse JSON input: " + jsonInput + " -> " + e.getMessage();
            System.out.println("WARNING: " + msg);
            logger.warn(msg);
            return false;
        }

        Map<String, Object> currentParams = algorithm.getParameters();
        if (currentParams == null) {
            String msg = "changeParameters: algorithm has no parameters map initialised";
            System.out.println("WARNING: " + msg);
            logger.warn(msg);
            return false;
        }

        boolean allOk = true;
        for (Map.Entry<String, Object> entry : parsed.entrySet()) {
            String key = entry.getKey();
            Object newRawValue = entry.getValue();

            if (!currentParams.containsKey(key)) {
                String msg = String.format(
                        "changeParameters: key '%s' not found in algorithm '%s' parameters – ignoring",
                        key, algorithm.getAlgorithmInfo());
                System.out.println("WARNING: " + msg);
                logger.warn(msg);
                allOk = false;
                continue;
            }

            Object existingValue = currentParams.get(key);
            Object castValue = tryCast(key, newRawValue, existingValue);
            if (castValue == null) {
                // tryCast already logged the warning
                allOk = false;
                continue;
            }

            algorithm.setParameter(key, castValue);
            logger.info("changeParameters: updated '{}' = {} (algo: {})", key, castValue,
                    algorithm.getAlgorithmInfo());
        }

        return allOk;
    }

    /**
     * Speed backtest between 0 and 100, where 0 is paused and 100 is max speed.
     *
     * @param speed
     * @return
     */
    public boolean changeBacktestSpeed(int speed) {
        String messagePrint = "Speed: " + speed;
        if (speed == 0) {
            messagePrint += " (Paused)";
            ParquetMarketDataConnectorPublisher.setPauseTradingEngine(true);
        } else {
            ParquetMarketDataConnectorPublisher.setPauseTradingEngine(false);
        }

        if (speed >= 100) {
            messagePrint = "Speed: max";
            ParquetMarketDataConnectorPublisher.setSpeed(-1);
        }
        if (speed > 0 && speed < 100) {
            int newSpeed = (int) Math.round(Math.exp(speed / 10.0));
            messagePrint = "Speed: " + newSpeed;
            ParquetMarketDataConnectorPublisher.setSpeed(newSpeed);
        }
        logger.info("changeSpeed: {}", messagePrint);
        System.out.println(messagePrint);
        return true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Attempts to convert {@code newRawValue} to the same type as
     * {@code existingValue}.  Returns the converted value, or {@code null} if
     * conversion is not possible (and logs a warning).
     */
    private Object tryCast(String key, Object newRawValue, Object existingValue) {
        if (existingValue == null) {
            // No type information – just store as-is
            return newRawValue;
        }

        Class<?> targetType = existingValue.getClass();

        try {
            String strVal = String.valueOf(newRawValue);

            if (targetType == Double.class) {
                return Double.parseDouble(strVal);
            }
            if (targetType == Float.class) {
                return Float.parseFloat(strVal);
            }
            if (targetType == Integer.class) {
                return (int) Math.round(Double.parseDouble(strVal));
            }
            if (targetType == Long.class) {
                return Math.round(Double.parseDouble(strVal));
            }
            if (targetType == Boolean.class) {
                if ("true".equalsIgnoreCase(strVal) || "1".equals(strVal)) return Boolean.TRUE;
                if ("false".equalsIgnoreCase(strVal) || "0".equals(strVal)) return Boolean.FALSE;
                throw new IllegalArgumentException("Cannot convert '" + strVal + "' to boolean");
            }
            if (targetType == String.class) {
                return strVal;
            }

            // Generic: try to assign directly if compatible
            if (targetType.isInstance(newRawValue)) {
                return newRawValue;
            }

            // Last resort: store as String (same behaviour as other parts of the framework)
            return strVal;

        } catch (Exception e) {
            String msg = String.format(
                    "changeParameters: cannot cast value '%s' for key '%s' to type %s – %s",
                    newRawValue, key, targetType.getSimpleName(), e.getMessage());
            System.out.println("WARNING: " + msg);
            logger.warn(msg);
            return null;
        }
    }
}



