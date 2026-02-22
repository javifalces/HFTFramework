package com.lambda.investing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.helpers.MessageFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class Configuration {

    public enum MULTITHREAD_CONFIGURATION {
        SINGLE_THREADING, MULTITHREADING
    }

    public enum LatencyEngineType {
        FIXED,
        POISSON
    }

    public static String BACKTEST_MESSAGE_PRINT = null;
    public static MULTITHREAD_CONFIGURATION MULTITHREADING_CORE = MULTITHREAD_CONFIGURATION.MULTITHREADING;//by default multithreading
    public static boolean FEES_COMMISSIONS_INCLUDED = true;//by default we have commissions set by instruments.xmls
    public static long DELAY_ORDER_BACKTEST_MS = 65;
    public static LatencyEngineType LATENCY_ENGINE_TYPE = LatencyEngineType.FIXED;

    //backtest engine
    public static int BACKTEST_THREADS_PUBLISHING_MARKETDATA = 0;//used to publish from parquet and csv file!
    public static int BACKTEST_THREADS_PUBLISHING_EXECUTION_REPORTS = 0;//publishing on backtest engine
    public static int BACKTEST_THREADS_LISTENING_ORDER_REQUEST = 0;//listening threads on backtest

    public static int BACKTEST_BUSY_THREADPOOL_TRESHOLD = 3;
    public static int BACKTEST_SYNCHRONIZED_TRADES_DEPTH_MAX_MS = 0;//Already Synchronizing in PersistorMarketDataConnector InstrumentCache

    public static boolean BACKTEST_REFRESH_DEPTH_ORDER_REQUEST = false;//if true algo requests are going to change the depth and notify it to the algos -> good for testing cyclical updates and check updates/removeMe
    public static boolean BACKTEST_REFRESH_DEPTH_TRADES = false;//if true trades are going to change the depth and notify it to the algos -> good for testing cyclical updates and check updates/removeMe
    public static long REFRESH_DEPTH_ORDER_REQUEST_MS = 1;//if >0, the depth is going to be refreshed with this ms later, 0 will be the same time and ignore in algorithm entry


    //algos engine
    public static int BACKTEST_THREADS_PUBLISHING_ORDER_REQUEST = 1;//required >0 for latency simulation
    public static int BACKTEST_THREADS_LISTENING_EXECUTION_REPORTS = 0;

    public static int THREADS_NOTIFY_ALGORITHM_OBSERVERS = 0;

    public static boolean IS_DEBUGGING_DEFAULT = false;//will disable latencies and muiltiThreading
    //			java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString()
    //					.indexOf("-agentlib:jdwp") > 0;

    public static long RANDOM_SEED = 0;
    public static long PORTFOLIO_MANAGER_UPDATE_FREQUENCY_MS = 15000;
    public static boolean USE_IPC_RL_TRAINING = getEnvOrDefault("USE_IPC_RL_TRAINING", "False").equalsIgnoreCase("True");//if true, the training is going to be done in the same process
    public static boolean DELTA_REWARD_REINFORCEMENT_LEARNING = true;//if true, the reward is the difference between the current and previous reward
    public static boolean DISCOUNT_REWARD_NO_TRADE = !getEnvOrDefault("DISCOUNT_REWARD_NO_TRADE", "").isEmpty();//if true delta reward is going to be negative if not operations -> force more trading
    public static boolean LOG_STATE_STEPS = !getEnvOrDefault("LOG_STATE_STEPS", "").isEmpty();//disable if not debugging
    public static Random RANDOM_GENERATOR = new Random();
    public static Logger logger = LogManager.getLogger(Configuration.class);
    public static boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");
    public static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    public static boolean USE_THREAD_AFFINITY = Boolean.parseBoolean(getEnvOrDefault("USE_THREAD_AFFINITY", "false"));//if true, the threads are going to be pinned to the cores
    public static String PUSHBULLET_TOKEN = getEnvOrDefault("PUSHBULLET_TOKEN", "");

    public static int[] GET_AFFINITY_CPUS() throws LambdaConfigurationException {
        if (!USE_THREAD_AFFINITY) {
            throw new LambdaConfigurationException("USE_AFFINITY disabled.");
        }

        if (IS_LINUX || NUMBER_OF_CORES < 32) {
            // Linux or less than 32 cores, use all available cores
            int[] cpus = new int[NUMBER_OF_CORES];
            for (int i = 0; i < NUMBER_OF_CORES; i++) {
                cpus[i] = i;
            }
            return cpus;
        }
        throw new LambdaConfigurationException("Unsupported OS or number of cores: " + System.getProperty("os.name") + " with " + NUMBER_OF_CORES + " cores. Only Linux or less than 32 cores are supported.");

    }

    public static void SET_MULTITHREAD_CONFIGURATION(MULTITHREAD_CONFIGURATION MULTITHREADING_CORE) {
        System.out.println("SET_MULTITHREAD_CONFIGURATION to " + MULTITHREADING_CORE.name());
        logger.info("SET_MULTITHREAD_CONFIGURATION to {}", MULTITHREADING_CORE.name());
        Configuration.MULTITHREADING_CORE = MULTITHREADING_CORE;
        if (Configuration.MULTITHREADING_CORE.equals(MULTITHREAD_CONFIGURATION.SINGLE_THREADING)) {
            BACKTEST_THREADS_PUBLISHING_MARKETDATA = 0;
            BACKTEST_THREADS_PUBLISHING_EXECUTION_REPORTS = 0;
            BACKTEST_THREADS_LISTENING_ORDER_REQUEST = 0;
            BACKTEST_THREADS_PUBLISHING_ORDER_REQUEST = 0;
            BACKTEST_THREADS_LISTENING_EXECUTION_REPORTS = 0;
            THREADS_NOTIFY_ALGORITHM_OBSERVERS = 0;
            DELAY_ORDER_BACKTEST_MS = 0;
        }

    }

    public static void LOG_STACKTRACE(Logger logger) {
        try {
            throw new Exception("LOG_STACKTRACE");
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public static void SET_DELAY_ORDER_BACKTEST_MS(long delayOrderMs) {
        System.out.println("SET_DELAY_ORDER_BACKTEST_MS to " + delayOrderMs);
        logger.info("SET_DELAY_ORDER_BACKTEST_MS to {}", delayOrderMs);
        DELAY_ORDER_BACKTEST_MS = delayOrderMs;
    }

    public static void SET_FEES_COMMISSIONS_INCLUDED(boolean feesCommissionsIncluded) {
        System.out.println("SET_FEES_COMMISSIONS_INCLUDED to " + feesCommissionsIncluded);
        logger.info("SET_FEES_COMMISSIONS_INCLUDED to {}", feesCommissionsIncluded);
        FEES_COMMISSIONS_INCLUDED = feesCommissionsIncluded;
    }

    public static void SET_LATENCY_ENGINE_TYPE(String latencyEngineType) {
        LATENCY_ENGINE_TYPE = LatencyEngineType.valueOf(latencyEngineType);
        System.out.println("SET_LATENCY_ENGINE_TYPE to " + LATENCY_ENGINE_TYPE);
        logger.info("SET_LATENCY_ENGINE_TYPE to {}", LATENCY_ENGINE_TYPE);
    }

    public static void SET_RANDOM_SEED(long seed) {
        if (seed != RANDOM_SEED) {
            System.out.println("SET SEED " + seed);
            logger.info("SET SEED {}", seed);
            RANDOM_SEED = seed;
            RANDOM_GENERATOR = new Random(RANDOM_SEED);
        }
    }

    public static String getEnvOrDefault(String name, String defaultValue) {
        String output = System.getenv(name);
        if (output == null) {
            output = System.getProperty(name, defaultValue);
        }
        return output;
    }

    public static String formatLog(String string, Object... objects) {
        return MessageFormatter.arrayFormat(string, objects).getMessage();
    }

    public static boolean isDebugging() {
        String defaultString = IS_DEBUGGING_DEFAULT ? "true" : "false";
        return getEnvOrDefault("DEBUG", defaultString).equalsIgnoreCase("true");
    }

    public static String getDataPath() {
        return getEnvOrDefault("LAMBDA_DATA_PATH", "X:\\");
    }


    public static String DATA_PATH = getDataPath();

    public static String INPUT_PATH = getEnvOrDefault("LAMBDA_INPUT_PATH",
            "D:\\javif\\Coding\\cryptotradingdesk\\java\\input");

    public static String OUTPUT_PATH = getEnvOrDefault("LAMBDA_OUTPUT_PATH",
            "D:\\javif\\Coding\\cryptotradingdesk\\java\\output");

    public static String TEMP_PATH = getEnvOrDefault("LAMBDA_TEMP_PATH",
            "D:\\javif\\Coding\\cryptotradingdesk\\java\\temp");

    public static SimpleDateFormat FILE_CSV_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    public static class LambdaConfigurationException extends Exception {
        public LambdaConfigurationException(String errorMessage) {
            super(errorMessage);
        }

        public LambdaConfigurationException(Exception errorMessage) {
            super(errorMessage);
        }

    }

    protected static DateFormat DAY_STR_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    public static String getDateFormat(Date date) {
        return DAY_STR_DATE_FORMAT.format(date);
    }


    public static String print() {
        //print all public fields of this class as json
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (java.lang.reflect.Field field : Configuration.class.getFields()) {
            try {
                sb.append("  \"").append(field.getName()).append("\": ");
                sb.append("\"").append(field.get(null)).append("\",\n");
            } catch (IllegalAccessException e) {
                sb.append("  \"").append(field.getName()).append("\": \"<access error>\",\n");
            }
        }
        sb.setLength(sb.length() - 2); // remove last comma
        sb.append("\n}");
        return sb.toString();
    }

}
