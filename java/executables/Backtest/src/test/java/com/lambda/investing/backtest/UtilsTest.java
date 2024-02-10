package com.lambda.investing.backtest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.backtest_engine.BacktestConfiguration;
import com.lambda.investing.backtest_engine.ordinary.OrdinaryBacktest;
import com.lambda.investing.market_data_connector.MarketDataConnectorPublisherListener;
import com.lambda.investing.model.asset.Currency;
import com.lambda.investing.model.asset.Instrument;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UtilsTest {


    private static void setEnv(Map<String, String> newenv) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass
                    .getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }

    public static void ConfigureEnvironment(String lambdaDataPath, String tempPath) throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("LAMBDA_DATA_PATH", lambdaDataPath);
        String outputPath = tempPath + "\\output";
        String logPath = tempPath + "\\logs";
        env.put("LAMBDA_OUTPUT_PATH", outputPath);
        env.put("LAMBDA_LOGS_PATH", logPath);//has to be before!
        env.put("LAMBDA_TEMP_PATH", tempPath);
        setEnv(env);
    }

    public static Gson GSON = new GsonBuilder().setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.FINAL)
            .serializeSpecialFloatingPointValues().disableHtmlEscaping().create();

    private static InputConfiguration loadJson(String jsonString, Logger logger) {

        InputConfiguration inputConfiguration = GSON.fromJson(jsonString, InputConfiguration.class);

        System.out.println("-----");
        System.out.println(jsonString);
        System.out.println("-----");
        logger.info("----");
        logger.info("{}", GSON.toJson(inputConfiguration));
        logger.info("----");
        return inputConfiguration;
    }

    public static String ReadFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.err.print("need valid a json file path as input argument to load backtest configuration "
                    + path);
            System.exit(-1);
        }
        try {
            String content = new String(Files.readAllBytes(Paths.get(path)));
            return content;
        } catch (IOException e) {
            System.err.print("need valid a json file path as input argument to load backtest configuration "
                    + path);

        }
        return null;
    }

    public static void AddTestInstruments() {
        Instrument instrument = new Instrument();
        instrument.setPrimaryKey("btcusdt_binance");
        instrument.setSymbol("btcusdt");
        instrument.setMarket("binance");
        instrument.setCurrency(Currency.USDT);
        instrument.setPriceTick(0.01);
        instrument.setQuantityTick(0.00001);
        instrument.setMakerFeePct(0.1);
        instrument.setTakerFeePct(0.1);
        instrument.addMap();


        Instrument instrument2 = new Instrument();
        instrument2.setPrimaryKey("eurusd_darwinex");
        instrument2.setSymbol("eurusd");
        instrument2.setMarket("darwinex");
        instrument2.setCurrency(Currency.USD);
        instrument2.setPriceTick(0.00001);
        instrument2.setQuantityTick(0.01);
        instrument2.addMap();

    }

    public static class BacktestLauncher {

        //        private final String jsonString;
        private boolean backtestIsRunning = false;
        private InputConfiguration inputConfiguration;

        public BacktestLauncher(InputConfiguration inputConfiguration) {
            this.inputConfiguration = inputConfiguration;
        }

        public BacktestConfiguration start() {
            System.setProperty("user.timezone", "GMT");
            Date startDate = new Date();
            AddTestInstruments();
//            InputConfiguration inputConfiguration = loadJson(jsonString, logger);
            BacktestConfiguration backtestConfiguration = null;
            Algorithm algorithm = null;


            try {
//            App.ConfigurationPropertiesLoader configurationProperties = new App.ConfigurationPropertiesLoader("application.properties");
                backtestConfiguration = inputConfiguration.getBacktestConfiguration();

                OrdinaryBacktest ordinaryBacktest = new OrdinaryBacktest(backtestConfiguration);
                ordinaryBacktest.start();

                backtestIsRunning = true;
                ordinaryBacktest.registerEndOfFile(this::stop);

                while (backtestIsRunning) {
                    Thread.onSpinWait();
                }


            } catch (Exception e) {
                System.err.println("BacktestLauncher:error in backtest " + e.getMessage());
                e.printStackTrace();
            }
            Date endDate = new Date();
            long elapsedMs = endDate.getTime() - startDate.getTime();
            long minutes = elapsedMs / (60 * 1000);
            System.out.println("BacktestLauncher: backtest finished in " + minutes + " minutes");
            return backtestConfiguration;
        }

        private void stop() {
            backtestIsRunning = false;
        }
    }

}
