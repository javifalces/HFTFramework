package com.lambda.investing.algo_trading;


import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.ArrayUtils;
import com.lambda.investing.algorithmic_trading.SingleInstrumentAlgorithm;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.live_trading_engine.LiveTrading;
import com.lambda.investing.market_data_connector.ZeroMqMarketDataConnector;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.trading_engine_connector.ZeroMqTradingEngineConnector;
import org.apache.hadoop.fs.Path;
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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.lambda.investing.model.Util.fromJsonString;
import static com.lambda.investing.model.Util.toJsonString;

public class App {
    private static boolean DISABLED_WARNING = false;
    protected final ApplicationContext ac;
    protected final Logger logger;
    private static Algorithm ALGORITHM;

    static {
        //		Asyn logs all
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        System.setProperty("DAsyncLogger.ExceptionHandler", "com.twc.ctg.ecp.service.EcpExceptionHandler");
        System.setProperty("AsyncLogger.ExceptionHandler", "com.twc.ctg.ecp.service.EcpExceptionHandler");
        System.setProperty("org.bytedeco.javacpp.logger", "slf4j");


    }

    public static void main(String[] args) {
        try {
            System.setProperty("user.timezone", "GMT");

            new App(args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    public static void disableWarning() {
        if (DISABLED_WARNING) {
            return;
        }

        System.err.close();
        System.setErr(System.out);
        DISABLED_WARNING = true;
    }

    protected static void setEnv(Map<String, String> newenv) throws Exception {
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

    protected void configureMarketDataConnector(ZeroMqTradingConfiguration zeroMqTradingConfiguration) {
        //		ZeroMqConfiguration zeroMqConfiguration=ac.getBean("marketDataAndERconnectorConfiguration",ZeroMqConfiguration.class);
        //		zeroMqConfiguration.setHost(zeroMqConfiguration.getHost());
        //		zeroMqConfiguration.setPort(zeroMqConfiguration.getPort());
        //
        //		ZeroMqProvider zeroMqProvider=ac.getBean("marketDataAndERconnectorConfiguration",ZeroMqProvider.class);
        //		zeroMqProvider.

        Map<String, String> environment = new HashMap<>();
        environment.put("marketdata.port", String.valueOf(zeroMqTradingConfiguration.getMarketDataPort()));
        environment.put("marketdata.host", String.valueOf(zeroMqTradingConfiguration.getMarketDataHost()));
        disableWarning();
        try {
            setEnv(environment);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    protected void configureFactorPublisherConnector(ZeroMqTradingConfiguration zeroMqTradingConfiguration) {
        //		ZeroMqConfiguration zeroMqConfiguration=ac.getBean("marketDataAndERconnectorConfiguration",ZeroMqConfiguration.class);
        //		zeroMqConfiguration.setHost(zeroMqConfiguration.getHost());
        //		zeroMqConfiguration.setPort(zeroMqConfiguration.getPort());
        //
        //		ZeroMqProvider zeroMqProvider=ac.getBean("marketDataAndERconnectorConfiguration",ZeroMqProvider.class);
        //		zeroMqProvider.

        Map<String, String> environment = new HashMap<>();
        environment.put("factor.port", String.valueOf(zeroMqTradingConfiguration.getFactorPublisherPort()));
        environment.put("factor.host", String.valueOf(zeroMqTradingConfiguration.getFactorPublisherHost()));
        try {
            setEnv(environment);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    protected void configureOrderRequestConnector(ZeroMqTradingConfiguration zeroMqTradingConfiguration) {

        //		ZeroMqConfiguration zeroMqConfiguration=ac.getBean("orderRequestConnectorConfigurationPublisher",ZeroMqConfiguration.class);
        Map<String, String> environment = new HashMap<>();
        environment.put("tradeengine.port", String.valueOf(zeroMqTradingConfiguration.getTradeEnginePort()));
        environment.put("tradeengine.host", String.valueOf(zeroMqTradingConfiguration.getTradeEngineHost()));
        try {
            setEnv(environment);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void configurePaperTrading(ApplicationContext ac, ZeroMqTradingConfiguration zeroMqTradingConfiguration) {
        if (zeroMqTradingConfiguration.isPaperTrading()) {
            System.out.println("PAPER TRADING CONFIGURED!");
            ZeroMqTradingEngineConnector zeroMqTradingEngineConnector = ac.getBean(ZeroMqTradingEngineConnector.class);
            ZeroMqMarketDataConnector zeroMqMarketDataConnector = ac.getBean(ZeroMqMarketDataConnector.class);

            zeroMqTradingEngineConnector.setPaperTrading(zeroMqMarketDataConnector);
            String[] instrumentPkArr = zeroMqTradingConfiguration.getInstrumentPks();
            List<String> instrumentList = Arrays.asList(instrumentPkArr);
            List<Instrument> instruments = new ArrayList<>();
            for (String instrumentPk : instrumentList) {
                try {
                    Instrument instrument = Instrument.getInstrument(instrumentPk);
                    instruments.add(instrument);
                } catch (Exception e) {
                    logger.error("cant add {} to instrument list paper trading", instrumentPk, e);
                }

            }
            zeroMqTradingEngineConnector.setInstrumentList(instruments);
        }

    }


    protected void configureMarketDataConnectorInstrumentFilter(ApplicationContext ac,
                                                                ZeroMqTradingConfiguration zeroMqTradingConfiguration) {
        ZeroMqMarketDataConnector marketDataConnector = ac.getBean(ZeroMqMarketDataConnector.class);
        //set instrument?
        String[] instrumentPkArr = zeroMqTradingConfiguration.getInstrumentPks();
        List<String> instrumentList = ArrayUtils.StringArrayList(instrumentPkArr);

        //add hedgeManagerInstruments
        AlgorithmConnectorConfiguration algorithmConnectorConfiguration = ac
                .getBean(AlgorithmConnectorConfiguration.class);

        AlgorithmConfiguration algorithmConfiguration = zeroMqTradingConfiguration.getAlgorithm();
        Algorithm algorithm = ALGORITHM;
        if (algorithm == null) {
            algorithm = algorithmConfiguration.getAlgorithm(algorithmConnectorConfiguration);
            ALGORITHM = algorithm;
        }
        for (Instrument instrument : algorithm.getHedgeManager().getInstrumentsHedgeList()) {
            String instrumentPk = instrument.getPrimaryKey();
            if (!instrumentList.contains(instrumentPk)) {
                instrumentList.add(instrumentPk);
            }
        }
        for (Instrument instrument : algorithm.getInstruments()) {
            if (instrument == null) {
                continue;
            }
            String instrumentPk = instrument.getPrimaryKey();
            if (!instrumentList.contains(instrumentPk)) {
                instrumentList.add(instrumentPk);
            }
        }

        //Add all the rest
        List<String> instruments = new ArrayList<>();
        for (String instrumentPk : instrumentList) {
            try {
                if (instrumentPk == null) {
                    continue;
                }
                Instrument instrument = Instrument.getInstrument(instrumentPk);
                instruments.add(instrument.getPrimaryKey());
            } catch (Exception e) {
                logger.error("cant add {} to instrument list filter", instrumentPk, e);

            }

        }

        String instrumentListStr = ArrayUtils.PrintArrayListString(instruments, ",");
        System.out
                .println(String.format("FILTER TO RECEIVE %d instruments : %s", instruments.size(), instrumentListStr));
        marketDataConnector.setInstrumentPksList(instruments);

    }

    protected void setInstruments(LiveTrading liveTrading, ZeroMqTradingConfiguration zeroMqTradingConfiguration) {
        //set instrument?
        String[] instrumentPkArr = zeroMqTradingConfiguration.getInstrumentPks();
        List<String> instrumentList = Arrays.asList(instrumentPkArr);
        List<Instrument> instruments = new ArrayList<>();
        for (String instrumentPk : instrumentList) {
            try {
                if (instrumentPk == null) {
                    continue;
                }
                Instrument instrument = Instrument.getInstrument(instrumentPk);
                instruments.add(instrument);
            } catch (Exception e) {
                logger.error("cant add {} to instrument list paper trading", instrumentPk, e);
            }

        }

        //// configure ZeroMQMarketData Connector InsturmentList Filter
        liveTrading.setInstrumentList(instruments);
    }

    protected void configureLivetrading(ApplicationContext ac, ZeroMqTradingConfiguration zeroMqTradingConfiguration)
            throws Exception {


        AlgorithmConfiguration algorithmConfiguration = zeroMqTradingConfiguration.getAlgorithm();
        LiveTrading liveTrading = ac.getBean(LiveTrading.class);

        setInstruments(liveTrading, zeroMqTradingConfiguration);
        liveTrading.setDemoTrading(zeroMqTradingConfiguration.isDemoTrading());
        liveTrading.setPaperTrading(zeroMqTradingConfiguration.isPaperTrading());

        AlgorithmConnectorConfiguration algorithmConnectorConfiguration = ac
                .getBean(AlgorithmConnectorConfiguration.class);

        Algorithm algorithm = ALGORITHM;
        if (algorithm == null) {
            algorithm = algorithmConfiguration.getAlgorithm(algorithmConnectorConfiguration);
            ALGORITHM = algorithm;
        }


        Instrument firstInstrumentToSetAlgo = liveTrading.getInstrumentList().get(0);
        if (algorithm instanceof SingleInstrumentAlgorithm) {
            ((SingleInstrumentAlgorithm) algorithm).setInstrument(firstInstrumentToSetAlgo);
        }

        liveTrading.setAlgorithm(algorithm);
        liveTrading.init();

    }

    protected void setLogProperty(ZeroMqTradingConfiguration zeroMqTradingConfiguration) {
        System.setProperty("log.appName", zeroMqTradingConfiguration.getAlgorithm().getAlgorithmName());
    }

    private static ZeroMqTradingConfiguration loadJson(String[] args) {
        ZeroMqTradingConfiguration zeroMqTradingConfiguration = fromJsonString(args[0], ZeroMqTradingConfiguration.class);
        System.out.println("-----");
        System.out.println(args[0]);
        System.out.println("-----");

        return zeroMqTradingConfiguration;
    }

    private static String[] argsFileToString(String[] args) {
        boolean checkFile = true;
        if (args.length != 1) {
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
                    System.exit(-1);

                }
            }

        }
        return args;
    }

    protected App(String[] args) throws IOException {
        ZeroMqTradingConfiguration zeroMqTradingConfiguration = null;
        try {
            args = argsFileToString(args);
            //configure properties before
            zeroMqTradingConfiguration = loadJson(args);

            setLogProperty(zeroMqTradingConfiguration);
            configureMarketDataConnector(zeroMqTradingConfiguration);
            configureOrderRequestConnector(zeroMqTradingConfiguration);
            configureFactorPublisherConnector(zeroMqTradingConfiguration);
            //load all beans
            ac = new ClassPathXmlApplicationContext(new String[]{"classpath:beans.xml"});
        } catch (BeansException be) {
            be.printStackTrace();
            logger = LogManager.getLogger();
            logger.fatal("Unable to load Spring application context files", be);
            throw be;
        }
        logger = LogManager.getLogger();//load logger properties
        logger.info("----");
        logger.info("{}", toJsonString(zeroMqTradingConfiguration));
        logger.info("----");

        try {
            ConfigurationPropertiesLoader configurationProperties = new ConfigurationPropertiesLoader("application.properties");

            configureMarketDataConnectorInstrumentFilter(ac, zeroMqTradingConfiguration);//create the algo
            configurePaperTrading(ac, zeroMqTradingConfiguration);
            configureLivetrading(ac, zeroMqTradingConfiguration);/////create algo TOOO

            ZeroMqConfiguration zeroMqTradingEngine = ac
                    .getBean("orderRequestConnectorConfigurationPublisher", ZeroMqConfiguration.class);
            ZeroMqConfiguration zeroMqMarketData = ac
                    .getBean("marketDataAndERconnectorConfiguration", ZeroMqConfiguration.class);

            System.out.println(
                    String.format("MARKET DATA : %s:%d", zeroMqMarketData.getHost(), zeroMqMarketData.getPort()));
            System.out.println(String.format("TRADING ENGINE: %s:%d", zeroMqTradingEngine.getHost(),
                    zeroMqTradingEngine.getPort()));

        } catch (Exception e) {
            logger.error("error in backtest ", e);
            e.printStackTrace();
            System.exit(-1);
        }

    }

    private class ConfigurationPropertiesLoader {
        private String path;
        public String logPath;
        public String tempPath;
        public String inputPath;
        public String outputPath;

        Properties resourceProperties;
        Properties environmentProperties;

        public ConfigurationPropertiesLoader(String path) {
            try {
                this.path = path;
                Resource resource = new FileSystemResource("application.properties");
                if (!resource.exists()) {
                    //when running from ide
                    resource = new ClassPathResource("application.properties");
                }
                resourceProperties = PropertiesLoaderUtils.loadProperties(resource);
                environmentProperties = System.getProperties();
                setProperties();

            } catch (Exception e) {
                logger.error("error in backtest ", e);
                e.printStackTrace();
                System.exit(-1);
            }

        }

        private void setProperties() {
            outputPath = resourceProperties.getProperty("output.path");
            if (environmentProperties.getProperty("output.path") != null) {
                outputPath = environmentProperties.getProperty("output.path");
            }

            if (outputPath != null) {
                System.out.println("Override OUTPUT_PATH(from output.path) to " + outputPath);
                Configuration.OUTPUT_PATH = outputPath;
            } else {
                System.out.println("default OUTPUT_PATH(from LAMBDA_OUTPUT_PATH) to " + Configuration.OUTPUT_PATH);
                outputPath = Configuration.OUTPUT_PATH;
            }
            new File(outputPath).mkdirs();

            inputPath = resourceProperties.getProperty("parquet.path");
            if (environmentProperties.getProperty("parquet.path") != null) {
                inputPath = environmentProperties.getProperty("parquet.path");
            }

            if (inputPath != null) {
                if (inputPath.endsWith(Path.SEPARATOR)) {
                    inputPath = inputPath.substring(0, inputPath.length() - 2);
                }

                System.out.println("Override DATA_PATH(from parquet.path) to " + inputPath);
                Configuration.DATA_PATH = inputPath;
            } else {
                System.out.println("default DATA_PATH(from LAMBDA_DATA_PATH) to " + Configuration.DATA_PATH);
                inputPath = Configuration.DATA_PATH;
            }

            tempPath = resourceProperties.getProperty("temp.path");
            if (environmentProperties.getProperty("temp.path") != null) {
                tempPath = environmentProperties.getProperty("temp.path");
            }

            if (tempPath != null) {
                System.out.println("Override TEMP_PATH(from temp.path) to " + tempPath);
                Configuration.TEMP_PATH = tempPath;
            } else {
                System.out.println("default TEMP_PATH (from LAMBDA_TEMP_PATH) to " + Configuration.TEMP_PATH);

            }

            logPath = resourceProperties.getProperty("log.path");
            if (environmentProperties.getProperty("log.path") != null) {
                logPath = environmentProperties.getProperty("log.path");
            }
            String separator = System.getProperty("file.separator");
            if (logPath == null) {
                logPath = System.getProperty("user.dir") + separator + "LOG";
                System.out.println("LOG_PATH(from working dir) to " + logPath);
            } else {
                System.out.println("LOG_PATH(from log.path) to " + logPath);
            }
        }
    }


}

