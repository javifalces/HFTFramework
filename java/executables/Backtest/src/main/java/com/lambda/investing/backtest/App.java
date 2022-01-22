package com.lambda.investing.backtest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.TrainType;
import com.lambda.investing.backtest_engine.BacktestConfiguration;
import com.lambda.investing.backtest_engine.ordinary.OrdinaryBacktest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;

import static com.lambda.investing.algorithmic_trading.reinforcement_learning.TrainNNUtils.DEFAULT_TRAIN_TYPE;
import static com.lambda.investing.algorithmic_trading.reinforcement_learning.TrainNNUtils.trainOnData;

public class App {

	private static boolean REMOVE_JSON_START = true;
	private static final String TRAIN_MODE = "train";
	protected final ApplicationContext ac;
	protected final Logger logger;
	public static Gson GSON = new GsonBuilder().setPrettyPrinting()
			.excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.FINAL)
			.serializeSpecialFloatingPointValues().disableHtmlEscaping().create();

	static {
		//		Asyn logs all
		System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
		System.setProperty("DAsyncLogger.ExceptionHandler", "com.twc.ctg.ecp.service.EcpExceptionHandler");
		System.setProperty("AsyncLogger.ExceptionHandler", "com.twc.ctg.ecp.service.EcpExceptionHandler");
		System.setProperty("org.bytedeco.javacpp.logger", "slf4j");

	}

	public static void main(String[] args) {
		try {
			new App(args);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	protected App(String[] args) throws IOException {

		try {

			boolean checkFile = true;
			if (args.length == 0) {
				System.out.println("EXAMPLE json!");
				//				String exampleJson = "{\n" + "\t\"backtest\": {\n" + "\t\t\"startDate\": \"20201208\",\n"
				//						+ "\t\t\"endDate\": \"20201208\",\n" + "\t\t\"instrument\": \"btcusdt_binance\"\n" + "\t},\n"
				//						+ "\t\"algorithm\": {\n" + "\t\t\"algorithmName\": \"AvellanedaStoikov\",\n"
				//						+ "\t\t\"parameters\": {\n" + "\t\t\t\"risk_aversion\": \"0.9\",\n"
				//						+ "\t\t\t\"position_multiplier\": \"100\",\n" + "\t\t\t\"window_tick\": \"100\",\n"
				//						+ "\t\t\t\"minutes_change_k\": \"10\",\n" + "\t\t\t\"quantity\": \"0.0001\",\n"
				//						+ "\t\t\t\"k_default\": \"0.00769\",\n" + "\t\t\t\"spread_multiplier\": \"5.0\",\n"
				//						+ "\t\t\t\"first_hour\": \"7\",\n" + "\t\t\t\"last_hour\": \"19\"\n" + "\t\t}\n" + "\t}\n"
				//						+ "\n" + "}\n";

				String exampleJson = "{\"backtest\":{\"startDate\": \"20201208\", \"endDate\": \"20201208\", \"instrument\": \"btcusdt_binance\"},\n\"algorithm\":{\"algorithmName\": \"AvellanedaQ_4_AvellanedaQ_4\", \"parameters\": {\"skewPricePctAction\": \"0.0\",\"riskAversionAction\": \"0.9,0.5,0.1\", \"windowsTickAction\": \"5,10\", \"numberDecimalsPrivateState\": \"4\", \"minPrivateState\": \"-0.001\", \"maxPrivateState\": \"0.001\", \"horizonTicksPrivateState\": \"1\", \"horizonMinMsTick\": \"10\", \"scoreEnum\": \"total_pnl\", \"timeHorizonSeconds\": \"5\", \"epsilon\": \"1.0\", \"risk_aversion\": \"0.9\", \"position_multiplier\": \"100\", \"window_tick\": \"5\", \"minutes_change_k\": \"10\", \"quantity\": \"0.0001\", \"k_default\": \"0.00769\", \"spread_multiplier\": \"5.0\", \"first_hour\": \"7\", \"last_hour\": \"19\"}}}";

				args = new String[] { exampleJson };
				checkFile = false;
			}
			boolean deleteFile = false;
			if (args.length == 2) {
				//from python
				deleteFile = true;
			}

			if (args.length > 2) {
				System.err.print("need a json file path as input argument to load backtest configuration");
				System.exit(-1);
			} else {

				//File detected
				if (checkFile) {
					File file = new File(args[0]);
					if (!file.exists()) {
						System.err.print("need valid a json file path as input argument to load backtest configuration "
								+ args[0]);
						System.exit(-1);
					}

					try {
						String content = new String(Files.readAllBytes(Paths.get(args[0])));
						args[0] = content;
					} catch (IOException e) {
						System.err.print("need valid a json file path as input argument to load backtest configuration "
								+ args[0]);
						if (REMOVE_JSON_START && deleteFile) {
							file.delete();
						}
						System.exit(-1);

					}

					if (REMOVE_JSON_START && deleteFile) {
						file.delete();
					}
				}

			}
			ac = new ClassPathXmlApplicationContext(new String[] { "classpath:beans.xml" });
		} catch (BeansException be) {
			logger = LogManager.getLogger();
			logger.fatal("Unable to load Spring application context files", be);

			throw be;
		}
		logger = LogManager.getLogger();

		//		logger.info("");
		//
		//		logger.info("");
		//		logger.info("JVM arguments: " + ManagementFactory.getRuntimeMXBean().getInputArguments());
		//
		//		logger.info("");
		//
		//		logger.info("System environment:");
		//		for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
		//			logger.info(entry.getKey() + " -> " + entry.getValue());
		//		}
		//
		//		logger.info("");
		//		logger.info("System properties: ");
		//		String lastElem;
		//		String elem;
		//		List<String> listPropertyValue;
		//		for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
		//			elem = entry.getKey().toString();
		//			listPropertyValue = Arrays.asList(elem.split("\\."));
		//			lastElem = listPropertyValue.get(listPropertyValue.size() - 1);
		//			logger.info(entry.getKey() + " -> " + entry.getValue());
		//		}
		//		logger.info("");

		try {
			TrainInputConfiguration trainInputConfiguration = GSON.fromJson(args[0], TrainInputConfiguration.class);
			if (trainInputConfiguration.getMemoryPath() != null
					&& trainInputConfiguration.getOutputModelPath() != null) {
				logger.info("trainInputConfiguration detected! -> training");
				logger.info("{}", GSON.toJson(trainInputConfiguration));
				TrainType trainType = DEFAULT_TRAIN_TYPE;

				if (trainInputConfiguration.getTrainType() != null) {
					trainType = trainInputConfiguration.getTrainType();
				}
				System.out.println(trainType + " training model");
				System.out.println("-----");
				System.out.println(args[0]);
				System.out.println("-----");

				trainOnData(trainType, trainInputConfiguration.getMemoryPath(),
						trainInputConfiguration.getActionColumns(), trainInputConfiguration.getStateColumns(),
						trainInputConfiguration.getOutputModelPath(), trainInputConfiguration.getLearningRate(),
						trainInputConfiguration.getMomentumNesterov(), trainInputConfiguration.getNEpoch(),
						trainInputConfiguration.getBatchSize(), trainInputConfiguration.getMaxBatchSize(),
						trainInputConfiguration.getL2(), trainInputConfiguration.getL1(),
						trainInputConfiguration.getTrainingStats(), trainInputConfiguration.isRNN(),
						trainInputConfiguration.isHyperparameterTuning(), trainInputConfiguration.getRnnHorizon());

				logger.info("finished training -> " + trainInputConfiguration.getOutputModelPath());

				if (trainInputConfiguration.getTrainingStats() != 0) {
					Scanner myObj = new Scanner(System.in);  // Create a Scanner object
					System.out.print("TrainingStats detected -> press enter key to close");
					String userName = myObj.nextLine();  // Read user input

				}
				return;
			}
		} catch (JsonSyntaxException e) {

		}

		InputConfiguration inputConfiguration = GSON.fromJson(args[0], InputConfiguration.class);
		System.out.println("-----");
		System.out.println(args[0]);
		System.out.println("-----");
		logger.info("----");
		logger.info("{}", GSON.toJson(inputConfiguration));
		logger.info("----");
		try {
			Resource resource = new FileSystemResource("application.properties");
			if (!resource.exists()) {
				//when running from ide
				resource = new ClassPathResource("application.properties");
			}
			Properties props = PropertiesLoaderUtils.loadProperties(resource);
			Properties environmentPro = System.getProperties();

			String outputPath = props.getProperty("output.path");
			if (environmentPro.getProperty("output.path") != null) {
				outputPath = environmentPro.getProperty("output.path");
			}

			if (outputPath != null) {
				System.out.println("Override OUTPUT_PATH(from output.path) to " + outputPath);
				Configuration.OUTPUT_PATH = outputPath;
			} else {
				System.out.println("default OUTPUT_PATH(from LAMBDA_OUTPUT_PATH) to " + Configuration.OUTPUT_PATH);
				outputPath = Configuration.OUTPUT_PATH;
			}
			new File(outputPath).mkdirs();

			String inputPath = props.getProperty("parquet.path");
			if (environmentPro.getProperty("parquet.path") != null) {
				inputPath = environmentPro.getProperty("parquet.path");
			}

			if (inputPath != null) {
				System.out.println("Override DATA_PATH(from parquet.path) to " + inputPath);
				Configuration.DATA_PATH = inputPath;
			} else {
				System.out.println("default DATA_PATH(from LAMBDA_DATA_PATH) to " + Configuration.DATA_PATH);
				inputPath = Configuration.DATA_PATH;
			}

			String tempPath = props.getProperty("temp.path");
			if (environmentPro.getProperty("temp.path") != null) {
				tempPath = environmentPro.getProperty("temp.path");
			}

			if (tempPath != null) {
				System.out.println("Override TEMP_PATH(from temp.path) to " + tempPath);
				Configuration.TEMP_PATH = tempPath;
			} else {
				System.out.println("default TEMP_PATH (from LAMBDA_TEMP_PATH) to " + Configuration.TEMP_PATH);

			}

			BacktestConfiguration backtestConfiguration = inputConfiguration.getBacktestConfiguration();

			OrdinaryBacktest ordinaryBacktest = new OrdinaryBacktest(backtestConfiguration);
			//			ordinaryBacktest.setSingleThread(true);//uncomment it if we want to backtest with latencies simulation
			ordinaryBacktest.start();

			//

		} catch (Exception e) {
			logger.error("error in backtest ", e);
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
