package com.lambda.investing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.helpers.MessageFormatter;

import java.text.SimpleDateFormat;
import java.util.Random;

public class Configuration {

	//backtest engine
	public static int BACKTEST_THREADS_PUBLISHING_MARKETDATA = 0;//used to publish from parquet and csv file!
	public static int BACKTEST_THREADS_PUBLISHING_EXECUTION_REPORTS = 0;//publishing on backtest engine
	public static int BACKTEST_THREADS_LISTENING_ORDER_REQUEST = 0;//listening threads on backtest

	//algos engine
	public static int BACKTEST_THREADS_PUBLISHING_ORDER_REQUEST = 5;//required >0 for latency simulation
	public static int BACKTEST_THREADS_LISTENING_EXECUTION_REPORTS = 0;

	public static boolean IS_DEBUGGING = false;//will disable latencies and muiltiThreading
	//			java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString()
	//					.indexOf("-agentlib:jdwp") > 0;

	public static long RANDOM_SEED = 0;
	public static Random RANDOM_GENERATOR = new Random();
	public static Logger logger = LogManager.getLogger(Configuration.class);

	public static void SET_RANDOM_GENERATOR(long seed) {
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
		return IS_DEBUGGING;
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

}
