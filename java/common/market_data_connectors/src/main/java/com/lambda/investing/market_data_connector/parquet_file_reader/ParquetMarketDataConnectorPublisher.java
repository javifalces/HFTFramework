package com.lambda.investing.market_data_connector.parquet_file_reader;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorPublisherProvider;
import com.lambda.investing.connector.zero_mq.ZeroMqPublisher;
import com.lambda.investing.data_manager.DataManager;
import com.lambda.investing.data_manager.csv.CSVDataManager;
import com.lambda.investing.data_manager.csv.CSVUtils;
import com.lambda.investing.data_manager.parquet.ParquetDataManager;
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
import tech.tablesaw.api.Table;

import javax.annotation.PostConstruct;
import java.io.File;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.lambda.investing.Configuration.TEMP_PATH;
import static com.lambda.investing.data_manager.csv.CSVUtils.NameRowPair;
import static com.lambda.investing.data_manager.csv.CSVUtils.sleepDifference;

public class ParquetMarketDataConnectorPublisher extends AbstractMarketDataConnectorPublisher implements Runnable {

	private static boolean PICKLE_CACHE = false;

	public static String ALGORITHM_INFO_MM = "MarketMaker_Parquet";
	public static String TOPIC_COMMAND = "command";
	public static Command STOP_COMMAND = new Command(Command.ClassMessage.stop.name());
	public static Command START_COMMAND = new Command(Command.ClassMessage.start.name());

	private ParquetFileConfiguration parquetFileConfiguration;
	protected Logger logger = LogManager.getLogger(ParquetMarketDataConnectorPublisher.class);
	private Thread readingThread;

	private DataManager dataManager;
	private Map<Date, NavigableMap<Long, NameRowPair>> readingTable = new HashMap<>();
	private List<Date> dates;

	public ParquetMarketDataConnectorPublisher(ConnectorConfiguration connectorConfiguration,
			ConnectorPublisher connectorPublisher, ParquetFileConfiguration parquetFileConfiguration) {
		super("ParquetMarketDataConnectorPublisher", connectorConfiguration, connectorPublisher);

		enable = false;
		dataManager = new ParquetDataManager();
		((ParquetDataManager) dataManager).setCacheBasePath(TEMP_PATH);

		this.parquetFileConfiguration = parquetFileConfiguration;
		dates = this.parquetFileConfiguration.getDatesToLoad();
		readingThread = new Thread(this, "ParquetMarketDataConnectorPublisher_reader");
		this.setStatistics(null);//disable statistics
	}

	@Override public void start() {
		//		enable=true;
	}

	@PostConstruct public void init() {
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

	private void loadPathMemory(Date date) throws Exception {
		List<Table> tablesList = new ArrayList<>();

		System.out.println("loading memory " + date.toString() + " parquet files");

		List<String> depthFiles = parquetFileConfiguration.getDepthFilesPath().get(date);
		if (depthFiles == null) {
			System.out.println("skipping reading depth for date:" + date.toString());
			return;
		}
		for (String depthFile : depthFiles) {
			if (!(new File(depthFile)).exists()) {
				System.out.println("skipping reading date:" + date + " " + depthFile);
				return;
			}
			try {
				//		for (String depthFile : parquetFileConfiguration.getDepthFilesPath().get(date)) {
				logger.info("reading {}...", depthFile);
				System.out.println("reading " + depthFile);
				Table tableDepth = (Table) dataManager.getData(depthFile, DepthParquet.class);
				tableDepth.setName(depthFile);
				tablesList.add(tableDepth);
				logger.info("added {} ", depthFile);
				System.out.println("added " + depthFile);
			} catch (Exception e) {
				logger.error("error reading {}", depthFile, e);
				System.err.println("error reading depth date:" + date + " " + depthFile);
				return;
			}
		}
		//		}

		List<String> tradesFile = parquetFileConfiguration.getTradeFilesPath().get(date);
		if (tradesFile != null) {
			//not needed -> darwinex
			for (String tradeFile : tradesFile) {
				if (!(new File(tradeFile)).exists()) {
					System.out.println("skipping reading date:" + date + " " + tradeFile);
					continue;
				}
				try {

					//		for (String tradeFile : parquetFileConfiguration.getTradeFilesPath()) {
					logger.info("reading {}...", tradeFile);
					System.out.println("reading " + tradeFile);
					Table tableTrade = (Table) dataManager.getData(tradeFile, TradeParquet.class);
					tableTrade.setName(tradeFile);
					tablesList.add(tableTrade);
					logger.info("added {} ", tradeFile);
					System.out.println("added " + tradeFile);
				} catch (Exception e) {
					logger.error("error reading {}", tradeFile, e);
					System.err.println("error reading trade date:" + date + " " + tradeFile);
				}
				//		}
			}
		}
		System.out.println("loading data... ");
		readingTable.put(date,
				CSVUtils.mergeTables(parquetFileConfiguration.getStartTime(), parquetFileConfiguration.getEndTime(),
						tablesList));

	}

	@Override public void run() {
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
		NavigableMap<Long, NameRowPair> readingTableDate = readingTable.get(firstDate);
		Map.Entry<Long, NameRowPair> entry = readingTableDate.entrySet().iterator().next();
		long timeStamp = entry.getKey();
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
				logger.error("cant sleep waiting ", e);
			}

		}
		long startTimeParquet = parquetFileConfiguration.getStartTime().getTime();
		long endTimeParquet = parquetFileConfiguration.getEndTime().getTime();
		Map<Instrument, Depth> lastDepth = new HashMap<>();
		for (Date date : dates) {
			readingTableDate = readingTable.get(date);
			//System.setErr(App.ERR_STREAM_CONSOLE);
			//			new ProgressBar("parquet_reader", readingTableDate.size(), 1000, System.err, ProgressBarStyle.ASCII, "", 1L, false, (DecimalFormat)null, ChronoUnit.SECONDS, 0L, Duration.ZERO))
			//			new ProgressBar("parquet_reader", readingTableDate.size())
			try (ProgressBar pb = new ProgressBar("parquet_reader", readingTableDate.size(), 1000, System.err,
					ProgressBarStyle.ASCII, "", 1L, false, (DecimalFormat) null, ChronoUnit.SECONDS, 0L,
					Duration.ZERO)) {

				for (Map.Entry<Long, NameRowPair> entrySet : readingTableDate.entrySet()) {
					try {
						timeStamp = entrySet.getKey();
						pb.step();
						pb.setExtraMessage(new Date(timeStamp).toString());

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

						NameRowPair nameRowPair = entrySet.getValue();

						String name = nameRowPair.getName();
						Row row = nameRowPair.getRow();

						Map.Entry<Long, CSVUtils.NameRowPair> nextEntrySet = readingTableDate.higherEntry(timeStamp);
						long timeToNextUpdateMs = Long.MIN_VALUE;
						if (nextEntrySet != null) {
							long nextTimeStamp = readingTableDate.higherEntry(timeStamp).getKey();
							timeToNextUpdateMs = nextTimeStamp - timeStamp;
						}

						try {
							if (parquetFileConfiguration.isInDepthFiles(name)) {
								Depth depth = createDepth(row, nameRowPair.getInstrument());
								if (timeToNextUpdateMs != Long.MIN_VALUE) {
									depth.setTimeToNextUpdateMs(timeToNextUpdateMs);
								}

								String topic = getTopic(nameRowPair.getInstrument());

								if (depth.isDepthValid()) {
									lastDepth.put(nameRowPair.getInstrument(), depth);
									notifyDepth(topic, depth);
								}
							}

							if (parquetFileConfiguration.isInTradeFiles(name)) {
								Trade trade = createTrade(row, nameRowPair.getInstrument());
								if (timeToNextUpdateMs != Long.MIN_VALUE) {
									trade.setTimeToNextUpdateMs(timeToNextUpdateMs);
								}
								String topic = getTopic(nameRowPair.getInstrument());
								;
								if (trade.isTradeValid(lastDepth.get(nameRowPair.getInstrument()))) {
									notifyTrade(topic, trade);
								}
							}

						} catch (Exception ex) {
							logger.error("Error reading row : {} ", row, ex);
						}

						sleepDifference(readingTableDate, timeStamp, parquetFileConfiguration.getSpeed());
					} catch (Exception ex) {
						logger.error("unknown error ", ex);
					}
				}
			}
		}

		System.out.println("Finished reading backtest CSV ");
		logger.info("************************* END OF CSV ***************");
		logger.info("End of {} reading table", this.getClass().getSimpleName());
		STOP_COMMAND.setTimestamp(timeStamp);
		notifyCommand(TOPIC_COMMAND, STOP_COMMAND);
		notifyEndOfFile();

	}

	private String getTopic(Instrument instrument) {
		return instrument.getPrimaryKey();
	}

	private Depth createDepth(Row row, Instrument instrument) {
		return CSVUtils.createDepth(row, instrument, ALGORITHM_INFO_MM);
	}

	private Trade createTrade(Row row, Instrument instrument) {
		return CSVUtils.createTrade(row, instrument, ALGORITHM_INFO_MM);
	}
}
