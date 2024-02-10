package com.lambda.investing.market_data_connector.parquet_file_reader;

import com.google.common.base.Stopwatch;
import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.zero_mq.ZeroMqPublisher;
import com.lambda.investing.data_manager.DataManager;
import com.lambda.investing.data_manager.FileDataUtils;
import com.lambda.investing.data_manager.parquet.TableSawParquetDataManager;
import com.lambda.investing.market_data_connector.AbstractMarketDataConnectorPublisher;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.DepthParquet;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.market_data.TradeParquet;
import com.lambda.investing.model.messaging.Command;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.lambda.investing.Configuration.BACKTEST_SYNCHRONIZED_TRADES_DEPTH_MAX_MS;
import static com.lambda.investing.data_manager.FileDataUtils.*;


public class ParquetMarketDataConnectorPublisher extends AbstractMarketDataConnectorPublisher implements Runnable {
    private static boolean ENABLE_CACHE = true;
    public static String ALGORITHM_INFO_MM = Depth.ALGORITHM_INFO_MM;
    public static String TOPIC_COMMAND = "command";

    public static Command STOP_COMMAND = new Command(Command.ClassMessage.stop.name());

    public static Command FINISHED_BACKTEST_COMMAND = new Command(Command.ClassMessage.finishedBacktest.name());
    public static Command START_COMMAND = new Command(Command.ClassMessage.start.name());

    private static long TIMEOUT_WAIT_PAUSE_MS = 60000L * 5;
    private static boolean TIMEOUT_CLOSE_PROCESS = true;
    private ParquetFileConfiguration parquetFileConfiguration;
    protected Logger logger = LogManager.getLogger(ParquetMarketDataConnectorPublisher.class);
    private Thread readingThread;

    private DataManager dataManager;
    private Map<Date, Table> readingTable = new HashMap<>();
    private List<Date> dates;

    //static date format day/month/year hour:minute:second.millisecond
    private static DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM HH:mm:ss.SSS");//"dd/MM/yyyy HH:mm:ss.SSS"


    public ParquetMarketDataConnectorPublisher(ConnectorConfiguration connectorConfiguration,
                                               ConnectorPublisher connectorPublisher, ParquetFileConfiguration parquetFileConfiguration) {
        super("ParquetMarketDataConnectorPublisher", connectorConfiguration, connectorPublisher);

        enable = false;
        dataManager = new TableSawParquetDataManager();

        this.parquetFileConfiguration = parquetFileConfiguration;
        dates = this.parquetFileConfiguration.getDatesToLoad();
        readingThread = new Thread(this, "backtestReader");
        this.setStatistics(null);//disable statistics
    }

    @Override
    public void start() {
        //		enable=true;
    }

    public void init() {
        startBacktest();
    }

    public void startBacktest() {
        try {
            for (Date date : this.parquetFileConfiguration.getDatesToLoad()) {
                loadPathMemory(date);
            }
            enable = false;
            readingThread.start();
        } catch (Exception e) {
            logger.error("Error loading files !!", e);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static Table readTrade(String tradeFile, Date date, Logger logger, DataManager dataManager) {
        File file = new File(tradeFile);
        if (!file.exists()) {
            System.out.println("skipping reading date:" + date + " " + tradeFile);
            return null;
        }
        try {

            //		for (String tradeFile : parquetFileConfiguration.getTradeFilesPath()) {
            logger.info("reading {}...", tradeFile);
            int sizeMb = (int) (file.length() / 1E6);
            System.out.println(Configuration.formatLog("reading {} ({} MB)", tradeFile, sizeMb));
            Table tableTrade = (Table) dataManager.getData(tradeFile, TradeParquet.class);
            tableTrade.setName(tradeFile);

            Column<String> typeColumn = StringColumn.create(TYPE_COLUMN, tableTrade.rowCount());
            typeColumn.setMissingTo("trade");
            tableTrade = tableTrade.addColumns(typeColumn);

            Column<String> pathColumn = StringColumn.create(PATH_COLUMN, tableTrade.rowCount());
            pathColumn.setMissingTo(tradeFile);
            tableTrade = tableTrade.addColumns(pathColumn);


            Column<String> instrument = StringColumn.create(INSTRUMENT_COLUMN, tableTrade.rowCount());
            Instrument instrumentObject = PATH_TO_INSTRUMENT.get(tableTrade.name());
            instrument.setMissingTo(instrumentObject.getPrimaryKey());
            tableTrade = tableTrade.addColumns(instrument);


            logger.info("added {} ", tradeFile);
            System.out.println("added " + tradeFile);
            return tableTrade;
        } catch (Exception e) {
            logger.error("error reading {}", tradeFile, e);
            System.err.println("error reading trade date:" + date + " " + tradeFile);
            return null;
        }
    }

    private static Table readDepth(String depthFile, Date date, Logger logger, DataManager dataManager) {
        File file = new File(depthFile);
        if (!file.exists()) {
            System.out.println("skipping reading date:" + date + " " + depthFile);
            return null;
        }
        try {
            //		for (String depthFile : parquetFileConfiguration.getDepthFilesPath().get(date)) {
            logger.info("reading {}...", depthFile);
            int sizeMb = (int) (file.length() / 1E6);
            System.out.println(Configuration.formatLog("reading {} ({} MB)", depthFile, sizeMb));
            Table tableDepth = (Table) dataManager.getData(depthFile, DepthParquet.class);
            tableDepth = tableDepth.setName(depthFile);

            Column<String> typeColumn = StringColumn.create(TYPE_COLUMN, tableDepth.rowCount());
            typeColumn.setMissingTo("depth");
            tableDepth = tableDepth.addColumns(typeColumn);

            Column<String> pathColumn = StringColumn.create(PATH_COLUMN, tableDepth.rowCount());
            typeColumn.setMissingTo(depthFile);
            tableDepth = tableDepth.addColumns(pathColumn);

            Column<String> instrument = StringColumn.create(INSTRUMENT_COLUMN, tableDepth.rowCount());
            Instrument instrumentObject = PATH_TO_INSTRUMENT.get(tableDepth.name());
            instrument.setMissingTo(instrumentObject.getPrimaryKey());
            tableDepth = tableDepth.addColumns(instrument);


            logger.info("added {} ", depthFile);
            System.out.println("added " + depthFile);
            return tableDepth;
        } catch (Exception e) {
            logger.error("error reading {}", depthFile, e);
            System.err.println("error reading depth date:" + date + " " + depthFile);
            return null;
        }
    }

    private static Table readCacheFile(File cacheFile, Logger logger, CacheManager cacheManager) {
        logger.info("reading cache {}...", cacheFile.getAbsolutePath());
        int sizeMb = (int) (cacheFile.length() / 1E6);
        long currentTime = new Date().getTime();
        System.out.println(Configuration.formatLog("reading cache {} ({} MB) ...", cacheFile.getAbsolutePath(), sizeMb));
        Table output = cacheManager.loadCache(cacheFile);
        if (output != null) {
            long elapsedSeconds = (new Date().getTime() - currentTime) / 1000;
            System.out.println(Configuration.formatLog("read cache done in {} seconds", elapsedSeconds));

            logger.info("loaded cache {}...", cacheFile.getAbsolutePath());
            return output;
        }
        return null;
    }

    private static Table loadMarketData(Date date, Date startDateTotal, Date endDateTotal, DataManager dataManager, List<String> depthFiles, List<String> tradesFile) {
        CacheManager cacheManager = new CacheManager(date, startDateTotal, endDateTotal, depthFiles, tradesFile);
        File cacheFile = cacheManager.getCacheFile();
        Logger logger = LogManager.getLogger(ParquetMarketDataConnectorPublisher.class);

        if (ENABLE_CACHE && cacheFile.exists()) {
            System.out.println(Configuration.formatLog("Cache file found {} {} reading...", cacheManager.toString(), cacheFile.getAbsolutePath()));
            Table output = readCacheFile(cacheFile, logger, cacheManager);
            if (output != null) {
                return output;
            } else {
                logger.error("error reading cache {} -> regenerate it", cacheFile.getAbsolutePath());
            }
        } else if (ENABLE_CACHE && !cacheFile.exists()) {
            System.out.println(Configuration.formatLog("Cache file not found {} {}", cacheManager.toString(), cacheFile.getAbsolutePath()));
        }

        List<Table> tablesList = new ArrayList<>();
        for (String depthFile : depthFiles) {
            Table depth = readDepth(depthFile, date, logger, dataManager);
            if (depth != null)
                tablesList.add(depth);
        }

        if (tradesFile != null) {
            //not needed -> darwinex
            for (String tradeFile : tradesFile) {
                Table trade = readTrade(tradeFile, date, logger, dataManager);
                if (trade != null)
                    tablesList.add(trade);
            }
        }
        System.out.println(Configuration.formatLog("Loading data(merging/sorting {} files of {} )... ", tablesList.size(), startDateTotal, endDateTotal));
        Table output = FileDataUtils.createTableMerged(startDateTotal, endDateTotal, tablesList);

        if (BACKTEST_SYNCHRONIZED_TRADES_DEPTH_MAX_MS > 0 && tradesFile != null) {
            System.out.println(Configuration.formatLog("Synchronizing trades with depth until {} ms... ", BACKTEST_SYNCHRONIZED_TRADES_DEPTH_MAX_MS));
            output = FileDataUtils.AdjustTradeTimestamp(output, BACKTEST_SYNCHRONIZED_TRADES_DEPTH_MAX_MS);
        }

        if (ENABLE_CACHE) {
            System.out.println(Configuration.formatLog("Cache file saving {} {} ...", cacheManager.toString(), cacheFile.getAbsolutePath()));
            cacheManager.saveCache(output, cacheFile);
        }
        return output;

    }


    private void loadPathMemory(Date date) throws Exception {
        System.out.println("loading memory " + date.toString() + " parquet files");
        List<String> depthFiles = parquetFileConfiguration.getDepthFilesPath().get(date);
        if (depthFiles == null) {
            System.out.println("skipping reading depth for date:" + date.toString());
            return;
        }
        List<String> tradesFile = parquetFileConfiguration.getTradeFilesPath().get(date);

        Table dayData = loadMarketData(date, parquetFileConfiguration.getStartTime(), parquetFileConfiguration.getEndTime(), dataManager, depthFiles, tradesFile);

        readingTable.put(date, dayData);

    }


    private long getTimeToNextUpdate(int currentRowNumber, Table readingTableDate) {
        long currentTimestamp = readingTableDate.row(currentRowNumber).getLong(TIMESTAMP_COL);
        int nextRowNumber = currentRowNumber + 1;
        if (nextRowNumber > readingTableDate.rowCount() - 1) {
            return currentTimestamp;
        }

        long nextTimestamp = readingTableDate.row(currentRowNumber + 1).getLong(TIMESTAMP_COL);
        long timeToNextUpdateMs = nextTimestamp - currentTimestamp;
        return timeToNextUpdateMs;
    }

    public int getSpeed() {
        if (SPEED != Integer.MIN_VALUE) {
            return SPEED;
        }

        return parquetFileConfiguration.getSpeed();
    }

    public static void setSpeed(int speed) {
        SPEED = speed;
    }

    @Override
    public void run() {
        int iterations = 0;
        BACKTEST_IS_READY = false;

        try {
            if (connectorPublisher instanceof ZeroMqPublisher) {
                Thread.sleep(10000);
            } else {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Date firstDate = this.dates.get(0);
        Table readingTableDate = readingTable.get(firstDate);
        if (readingTableDate == null || readingTableDate.rowCount() == 0) {
            System.err.println(
                    "Error readingTableDate is null or size 0!! and readingTable size is " + readingTable.size()
                            + " check LAMBDA_DATA_PATH to " + Configuration.getDataPath());

            logger.error("Error readingTableDate is null  or size 0!! and readingTable size is " + readingTable.size()
                    + " check LAMBDA_DATA_PATH to " + Configuration.getDataPath());
            System.exit(-1);
        }

        long timeStamp = readingTableDate.row(0).getLong(TIMESTAMP_COL);
        START_COMMAND.setTimestamp(timeStamp);
        notifyCommand(TOPIC_COMMAND, START_COMMAND);

        long startTime = System.currentTimeMillis();
        boolean firstStart = true;

        if (parquetFileConfiguration.getInitialSleepSeconds() <= 0) {
            enable = true;
        }
        while (!enable) {
            if (firstStart) {
                firstStart = false;
                logger.info("waiting start in ParquetMarketDataConnectorPublisher to start until {} seconds",
                        parquetFileConfiguration.getInitialSleepSeconds());
            }
            long elapsedMs = System.currentTimeMillis() - startTime;

            if (elapsedMs >= parquetFileConfiguration.getInitialSleepSeconds() * 1000) {
                enable = true;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.error("can't sleep waiting ", e);
            }

        }
        long startTimeParquet = parquetFileConfiguration.getStartTime().getTime();
        long endTimeParquet = parquetFileConfiguration.getEndTime().getTime();
        Map<Instrument, Depth> lastDepth = new HashMap<>();
        Stopwatch stopwatch = Stopwatch.createStarted();

        for (Date date : dates) {
            readingTableDate = readingTable.get(date);
            readingTableDate = readingTableDate.where(readingTableDate.longColumn(TIMESTAMP_COL).isBetweenInclusive(startTimeParquet, endTimeParquet));
            //System.setErr(App.ERR_STREAM_CONSOLE);
            //			new ProgressBar("parquet_reader", readingTableDate.size(), 1000, System.err, ProgressBarStyle.ASCII, "", 1L, false, (DecimalFormat)null, ChronoUnit.SECONDS, 0L, Duration.ZERO))
            //			new ProgressBar("parquet_reader", readingTableDate.size())
            try (ProgressBar pb = new ProgressBar("backtest", readingTableDate.rowCount(), 1000, System.err,
                    ProgressBarStyle.ASCII, "", 1L, false, (DecimalFormat) null, ChronoUnit.SECONDS, 0L,
                    Duration.ZERO)) {

                for (int rowNumber = 0; rowNumber < readingTableDate.rowCount(); rowNumber++) {
                    Row row = readingTableDate.row(rowNumber);
                    BACKTEST_IS_READY = true;
                    boolean timeout = waitIfPause(TIMEOUT_WAIT_PAUSE_MS);
                    if (!timeout) {
                        logger.warn("timeout waiting paused reached step not received after {} ms , resetPause and continue ", TIMEOUT_WAIT_PAUSE_MS);
                        System.err.println("WARNING: timeout waiting paused reached step not received after " + TIMEOUT_WAIT_PAUSE_MS + " ms");
                        if (TIMEOUT_CLOSE_PROCESS) {
                            System.err.println("WARNING: TIMEOUT_CLOSE_PROCESS is true, FORCE_STOP_BACKTEST");
                            logger.warn("WARNING: TIMEOUT_CLOSE_PROCESS is true, FORCE_STOP_BACKTEST");
                            FORCE_STOP_BACKTEST = true;
                        }

                        resetPause();
                    }

                    try {
                        timeStamp = row.getLong(TIMESTAMP_COL);
                        pb.step();
                        Date dateRow = new Date(timeStamp);
                        //format dateRow to string with format DATE_FORMAT
                        String dateRowString = DATE_FORMAT.format(dateRow);

                        if (Configuration.BACKTEST_MESSAGE_PRINT != null) {
                            String extraMessage = Configuration.formatLog("{}{}", dateRowString, Configuration.BACKTEST_MESSAGE_PRINT);
                            //WARNING max len of 23 characters for extraMessage, 18 in date print ->
                            pb.setExtraMessage(extraMessage);
                        } else {
                            pb.setExtraMessage(dateRowString);
                        }
                        if (startTimeParquet != endTimeParquet) {
                            if (timeStamp < startTimeParquet) {
                                //check time start in same day
                                continue;
                            }
                            if (timeStamp > endTimeParquet) {
                                //check time start in same day
                                continue;
                            }
                        }


                        String type = row.getString(TYPE_COLUMN);


                        long timeToNextUpdateMs = getTimeToNextUpdate(rowNumber, readingTableDate);
                        String instrumentPk = row.getString(INSTRUMENT_COLUMN);
                        Instrument instrument = Instrument.getInstrument(instrumentPk);

                        try {
                            if (type.equalsIgnoreCase("depth")) {
                                Depth depth = createDepth(row, instrument);
                                if (timeToNextUpdateMs != Long.MIN_VALUE) {
                                    depth.setTimeToNextUpdateMs(timeToNextUpdateMs);
                                }

                                String topic = getTopic(instrument);

                                if (depth.isDepthValid()) {
                                    lastDepth.put(instrument, depth);
                                    notifyDepth(topic, depth);
                                }
                            }

                            if (type.equalsIgnoreCase("trade")) {
                                Trade trade = createTrade(row, instrument);
                                if (timeToNextUpdateMs != Long.MIN_VALUE) {
                                    trade.setTimeToNextUpdateMs(timeToNextUpdateMs);
                                }
                                String topic = getTopic(instrument);
                                ;
                                if (trade.isTradeValid(lastDepth.get(instrument))) {
                                    notifyTrade(topic, trade);
                                }
                            }

                        } catch (Exception ex) {
                            logger.error("Error reading row : {} ", row, ex);
                            logger.error(ex.getStackTrace());
                        }


                        sleepDifference(readingTableDate, rowNumber, timeStamp, getSpeed());
                    } catch (Exception ex) {
                        logger.error("unknown error ", ex);
                    } finally {
                        //check it always after every iteration
                        if (FORCE_STOP_BACKTEST) {
                            System.out.println("FORCE_STOP_BACKTEST detected : " + new Date(timeStamp));
                            logger.info("FORCE_STOP_BACKTEST detected in ParquetMarketDataConnectorPublisher {} ", new Date(timeStamp));
                            FORCE_STOP_BACKTEST = false;
                            break;
                        }

                    }
                }


            }


        }
        endOfBacktest(stopwatch, timeStamp);


        System.exit(0);

    }

    private void endOfBacktest(Stopwatch stopwatch, long timeStamp) {
        System.out.println("Finished reading backtest Parquet: " + stopwatch.stop());
        logger.info("************************* END OF Parquets ***************");
        logger.info("End of {} reading table", this.getClass().getSimpleName());

        //stop the algorithm
        STOP_COMMAND.setTimestamp(timeStamp);
        notifyCommand(TOPIC_COMMAND, STOP_COMMAND);
        //notify end of file => RL is going to set isDone
        notifyEndOfFile();

        //save backtest results + if exitOnStop will exit the process
        FINISHED_BACKTEST_COMMAND.setTimestamp(timeStamp);
        notifyCommand(TOPIC_COMMAND, FINISHED_BACKTEST_COMMAND);//kill the process if setExitOnStop Algorithm
    }

    private String getTopic(Instrument instrument) {
        return instrument.getPrimaryKey();
    }

    private Depth createDepth(Row row, Instrument instrument) {
        return FileDataUtils.createDepth(row, instrument, ALGORITHM_INFO_MM);
    }

    private Trade createTrade(Row row, Instrument instrument) {
        return FileDataUtils.createTrade(row, instrument, ALGORITHM_INFO_MM);
    }
}
