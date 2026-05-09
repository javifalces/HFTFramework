package com.lambda.investing.algo_trading.core;


import com.lambda.investing.Configuration;
import com.lambda.investing.algo_trading.AlgorithmConfiguration;
import com.lambda.investing.algo_trading.ZeroMqTradingConfiguration;
import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.ArrayUtils;
import com.lambda.investing.algorithmic_trading.SingleInstrumentAlgorithm;
import com.lambda.investing.algorithmic_trading.utils.AppUtils;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
        // For Java 9+, direct environment variable modification is blocked by the module system
        // We use System properties as the primary mechanism since Spring and most frameworks read from there


        // Java 9+: Set System properties directly (Spring beans will read from System.getProperty)
        for (Map.Entry<String, String> entry : newenv.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
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

    /**
     * Returns the effective instrument PKs: uses {@code instrumentPks} from the configuration if present,
     * otherwise falls back to the algorithm parameters.
     * Priority for parameter fallback: "instrumentPks" (array or comma-separated) then "instrument" (backward compat).
     */
    protected String[] getEffectiveInstrumentPks(ZeroMqTradingConfiguration zeroMqTradingConfiguration) {
        String[] instrumentPks = zeroMqTradingConfiguration.getInstrumentPks();
        if (instrumentPks != null && instrumentPks.length > 0) {
            return instrumentPks;
        }
        // Fall back to algorithm parameters
        AlgorithmConfiguration algorithmConfiguration = zeroMqTradingConfiguration.getAlgorithm();
        if (algorithmConfiguration != null && algorithmConfiguration.getParameters() != null) {
            Map<String, Object> params = algorithmConfiguration.getParameters();
            // Check "instrumentPks" first (supports JSON array or comma-separated string)
            String instrumentStr = resolveInstrumentParam(params, "instrumentPks");
            if (instrumentStr == null) {
                // Backward compat: fall back to "instrument"
                instrumentStr = resolveInstrumentParam(params, "instrument");
            }
            if (instrumentStr != null) {
                logger.info("instrumentPks not set in ZeroMqTradingConfiguration, loaded from algorithm parameters: {}", instrumentStr);
                String[] pks = instrumentStr.split(",");
                for (int i = 0; i < pks.length; i++) {
                    pks[i] = pks[i].trim();
                }
                return pks;
            }
        }
        return new String[0];
    }

    /**
     * Reads a parameter from the map as a comma-separated string.
     * The value may be a {@link List} (from JSON array deserialization) or a plain String.
     * Returns {@code null} if the key is absent, null, or empty.
     */
    private String resolveInstrumentParam(Map<String, Object> params, String key) {
        if (!params.containsKey(key)) {
            return null;
        }
        Object value = params.get(key);
        String str;
        if (value instanceof List) {
            str = ((List<?>) value).stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        } else {
            str = String.valueOf(value);
        }
        if (str == null || str.equalsIgnoreCase("null") || str.isEmpty()) {
            return null;
        }
        return str;
    }

    protected void configurePaperTrading(ApplicationContext ac, ZeroMqTradingConfiguration zeroMqTradingConfiguration) {
        if (zeroMqTradingConfiguration.isPaperTrading()) {
            System.out.println("PAPER TRADING CONFIGURED!");
            ZeroMqTradingEngineConnector zeroMqTradingEngineConnector = ac.getBean(ZeroMqTradingEngineConnector.class);
            ZeroMqMarketDataConnector zeroMqMarketDataConnector = ac.getBean(ZeroMqMarketDataConnector.class);

            zeroMqTradingEngineConnector.setPaperTrading(zeroMqMarketDataConnector);
            String[] instrumentPkArr = getEffectiveInstrumentPks(zeroMqTradingConfiguration);
            List<String> instrumentList = Arrays.asList(instrumentPkArr);
            List<Instrument> instruments = new ArrayList<>();
            for (String instrumentPk : instrumentList) {
                try {
                    Instrument instrument = Instrument.getInstrument(instrumentPk);
                    if (instrument == null) {
                        System.err.println("Instrument " + instrumentPk + " not found to configurePaperTrading");
                        logger.warn("Instrument {} not found to configurePaperTrading", instrumentPk);
                        continue;
                    }
                    instruments.add(instrument);
                } catch (Exception e) {
                    logger.error("can't add {} to instrument list paper trading", instrumentPk, e);
                }

            }
            zeroMqTradingEngineConnector.setInstrumentList(instruments);
        }

    }


    protected void configureMarketDataConnectorInstrumentFilter(ApplicationContext ac,
                                                                ZeroMqTradingConfiguration zeroMqTradingConfiguration) throws Exception {
        ZeroMqMarketDataConnector marketDataConnector = ac.getBean(ZeroMqMarketDataConnector.class);
        //set instrument?
        String[] instrumentPkArr = getEffectiveInstrumentPks(zeroMqTradingConfiguration);
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

        if (algorithm == null) {
            logger.error("Algorithm not configured " + algorithmConfiguration.getAlgorithmName());
            throw new Exception("Algorithm not configured " + algorithmConfiguration.getAlgorithmName());
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
                logger.error("can't add {} to instrument list filter", instrumentPk, e);
                System.err.println("can't add " + instrumentPk + " to instrument list filter" + e.getMessage());

            }

        }

        String instrumentListStr = ArrayUtils.PrintArrayListString(instruments, ",");
        System.out
                .println(String.format("FILTER TO RECEIVE %d instruments : %s", instruments.size(), instrumentListStr));
        marketDataConnector.setInstrumentPksList(instruments);

    }

    protected void setInstruments(LiveTrading liveTrading, ZeroMqTradingConfiguration zeroMqTradingConfiguration) {
        //set instrument?
        String[] instrumentPkArr = getEffectiveInstrumentPks(zeroMqTradingConfiguration);
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
                logger.error("can't add {} to instrument list paper trading", instrumentPk, e);
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

    /**
     * Registers OS-level signal handlers so that a Windows {@code taskkill /PID <pid>}
     * (graceful kill, without {@code /F}) results in {@link #onShutdown()} being called.
     * <p>
     * On Windows the JVM maps both SIGINT (Ctrl+C / taskkill) and SIGTERM to the
     * {@code sun.misc.Signal} facility.  The shutdown hook alone is sufficient when
     * the process owns a console window; the signal handlers cover the case where it
     * runs without one (e.g. launched via {@code javaw.exe} or a service wrapper).
     */
    private static void registerSignalHandlers() {
        String[] signals = {"INT", "TERM"};


        for (String sigName : signals) {
            try {
                sun.misc.Signal.handle(new sun.misc.Signal(sigName), signal -> {
                    LogManager.getLogger(App.class)
                            .info("Received OS signal SIG{} – initiating graceful shutdown", sigName);
                    onShutdown();
                    // System.exit triggers the JVM shutdown hook as well; the
                    // AtomicBoolean in onShutdown() guarantees it runs only once.
                    System.exit(0);
                });
            } catch (IllegalArgumentException e) {
                // Signal not supported on this platform – ignore silently
            } catch (Exception e) {
                LogManager.getLogger(App.class)
                        .warn("Could not register SIG{} handler: {}", sigName, e.getMessage());
            }
        }
    }

    /**
     * Starts a lightweight daemon thread that polls for a stop-file named {@code <pid>.stop}
     * in the input directory (same folder where {@code <name>.pid} files live).
     * <p>
     * This is the reliable cross-platform IPC mechanism for graceful shutdown:
     * the PowerShell stop script writes the file; the watcher calls {@link #onShutdown()}
     * and then {@code System.exit(0)}, which triggers the registered JVM shutdown hook.
     * Unlike {@code taskkill /PID} (WM_CLOSE), this works for any console process.
     *
     * @param inputDir directory to watch (typically {@code LAMBDA_INPUT_PATH} or {@code ./input})
     * @param pid      the current process PID
     */
    private static void startStopFileWatcher(final String inputDir, final long pid) {
        // Pre-resolve the path once — no allocation inside the polling loop
        final String stopFilePath = inputDir + File.separator + pid + ".stop";
        final java.io.File stopFile = new java.io.File(stopFilePath);

        Thread watcher = new Thread(() -> {
            LogManager.getLogger(App.class).info("Stop-file watcher started — watching for: {}", stopFilePath);
            System.out.println("Stop-file watcher started — watching for: " + stopFilePath);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(15_000);   // check every 15 s — coarse enough to be invisible to the OS scheduler
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (stopFile.exists()) {
                    LogManager.getLogger(App.class).info("Stop file detected: {} — initiating graceful shutdown", stopFilePath);
                    System.out.println("Stop file detected: " + stopFilePath + " — initiating graceful shutdown");
                    stopFile.delete();
                    onShutdown();
                    System.exit(0);
                }
            }
        }, "stop-file-watcher");
        watcher.setDaemon(true);
        watcher.setPriority(Thread.MIN_PRIORITY); // never compete with trading threads for CPU time
        watcher.start();
    }

    protected static void onShutdown() {
        Logger shutdownLogger = LogManager.getLogger(App.class);
        shutdownLogger.info("Shutdown hook triggered - controlling shutdown sequence...");
        System.out.println("Shutdown hook triggered - controlling shutdown sequence...");
        if (ALGORITHM != null) {
            try {
                shutdownLogger.info("Cancelling all orders and stopping algorithm: {}", ALGORITHM.getAlgorithmInfo());
                System.out.println("Cancelling all orders and stopping algorithm: " + ALGORITHM.getAlgorithmInfo());
                ALGORITHM.manualStop();
                shutdownLogger.info("Algorithm stopped successfully.");
                System.out.println("Algorithm stopped successfully.");
            } catch (Exception e) {
                shutdownLogger.error("Error during algorithm shutdown", e);
                e.printStackTrace();
            }
        } else {
            shutdownLogger.warn("No algorithm instance found during shutdown.");
            System.out.println("No algorithm instance found during shutdown.");
        }
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
        AppUtils.LogLibraryVersions();
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
            ac = new ClassPathXmlApplicationContext(new String[]{"classpath:core_zero_beans.xml"});

            // Start stop-file watcher: the PowerShell stop script writes <pid>.stop
            // in the input directory; this watcher triggers onShutdown() reliably even
            // when taskkill without /F fails (java.exe has no message queue for WM_CLOSE).
            long currentPid = ProcessHandle.current().pid();
            String inputDir = System.getenv("LAMBDA_INPUT_PATH");
            if (inputDir == null || inputDir.isEmpty()) {
                inputDir = System.getProperty("user.dir") + File.separator + "input";
            }
            new File(inputDir).mkdirs();
            startStopFileWatcher(inputDir, currentPid);
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
            logger.error("error in algoTrading ", e);
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

