package com.lambda.investing;

import org.slf4j.helpers.MessageFormatter;
import java.text.SimpleDateFormat;

public class Configuration {

	//backtest engine
	public static int BACKTEST_THREADS_PUBLISHING_MARKETDATA = 0;//used to publish from parquet and csv file!
	public static int BACKTEST_THREADS_PUBLISHING_EXECUTION_REPORTS = 1;//publishing on backtest engine
	public static int BACKTEST_THREADS_LISTENING_ORDER_REQUEST = 2;//listening threads on backtest

	//algos engine
	public static int BACKTEST_THREADS_PUBLISHING_ORDER_REQUEST = 1;//required >0 for latency simulation
	public static int BACKTEST_THREADS_LISTENING_EXECUTION_REPORTS = 1;

	public static boolean IS_DEBUGGING = false;
	//			java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString()
	//					.indexOf("-agentlib:jdwp") > 0;

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
