package com.lambda.investing.market_data_connector.csv_file_reader;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.data_manager.DataManager;
import com.lambda.investing.data_manager.csv.CSVDataManager;
import com.lambda.investing.data_manager.csv.CSVUtils;
import com.lambda.investing.market_data_connector.AbstractMarketDataConnectorPublisher;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import me.tongfei.progressbar.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import me.tongfei.progressbar.*;

import javax.annotation.PostConstruct;
import java.util.*;

import static com.lambda.investing.data_manager.csv.CSVUtils.*;

public class CSVMarketDataConnectorPublisher extends AbstractMarketDataConnectorPublisher implements Runnable {

	public static String ALGORITHM_INFO_MM = Depth.ALGORITHM_INFO_MM;
	public static String TOPIC_COMMAND = "command";
	public static Command STOP_COMMAND = new Command(Command.ClassMessage.stop.name());
	public static Command START_COMMAND = new Command(Command.ClassMessage.start.name());

	private CSVFileConfiguration CSVFileConfiguration;
	protected Logger logger = LogManager.getLogger(CSVMarketDataConnectorPublisher.class);
	private Thread readingThread;

	private DataManager csvDataManager;
	NavigableMap<Long, CSVUtils.NameRowPair> readingTable;

	public CSVMarketDataConnectorPublisher(ConnectorConfiguration connectorConfiguration,
			ConnectorPublisher connectorPublisher, CSVFileConfiguration CSVFileConfiguration) {
		super("CSVMarketDataConnectorPublisher", connectorConfiguration, connectorPublisher);

		enable = false;
		csvDataManager = new CSVDataManager();
		this.CSVFileConfiguration = CSVFileConfiguration;
		readingThread = new Thread(this, "CSVMarketDataConnectorPublisher_reader");
		this.setStatistics(null);//disable statistics
	}

	@PostConstruct public void init() {
		try {
			loadFilesMemory();
			enable = false;
			readingThread.start();
		} catch (Exception e) {
			logger.error("Error loading files !!", e);
		}
	}

	private void loadFilesMemory() throws Exception {
		List<Table> tablesList = new ArrayList<>();
		for (String depthFile : CSVFileConfiguration.getDepthFilesPath()) {
			logger.info("reading {}...", depthFile);
			Table table = (Table) csvDataManager.getData(depthFile, Depth.class);
			tablesList.add(table);
			logger.info("added {} ", depthFile);
		}

		for (String tradeFile : CSVFileConfiguration.getTradeFilesPath()) {
			logger.info("reading {}...", tradeFile);
			Table table = (Table) csvDataManager.getData(tradeFile, Trade.class);
			tablesList.add(table);
			logger.info("added {} ", tradeFile);
		}
		System.out.println("Merging/sorting all memory tables");
		readingTable = CSVUtils
				.mergeTables(CSVFileConfiguration.getStartTime(), CSVFileConfiguration.getEndTime(), tablesList);
	}

	@Override public void run() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Map.Entry<Long, CSVUtils.NameRowPair> entry = readingTable.entrySet().iterator().next();
		long timeStamp = entry.getKey();
		START_COMMAND.setTimestamp(timeStamp);
		notifyCommand(TOPIC_COMMAND, START_COMMAND);

		long startTime = System.currentTimeMillis();
		boolean firstStart = true;

		while (!enable) {
			if (firstStart) {
				firstStart = false;
				logger.info("waiting start in CSVMarketDataConnectorPublisher to start until {} seconds",
						CSVFileConfiguration.getInitialSleepSeconds());
			}
			long elapsedMs = System.currentTimeMillis() - startTime;

			if (elapsedMs >= CSVFileConfiguration.getInitialSleepSeconds() * 1000) {
				start();
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				logger.error("cant sleep waiting ", e);
			}

		}

		//System.setErr(App.ERR_STREAM_CONSOLE);
		try (ProgressBar pb = new ProgressBar("csv_reader", readingTable.size())) {

			for (Map.Entry<Long, CSVUtils.NameRowPair> entrySet : readingTable.entrySet()) {
				try {
					timeStamp = entrySet.getKey();
					pb.step();
					pb.setExtraMessage(new Date(timeStamp).toString());

					NameRowPair nameRowPair = entrySet.getValue();

					String name = nameRowPair.getName();
					Row row = nameRowPair.getRow();
					Instrument instrument = nameRowPair.getInstrument();

					Map.Entry<Long, CSVUtils.NameRowPair> nextEntrySet = readingTable.higherEntry(timeStamp);
					long timeToNextUpdateMs = Long.MIN_VALUE;
					if (nextEntrySet != null) {
						long nextTimeStamp = readingTable.higherEntry(timeStamp).getKey();
						timeToNextUpdateMs = nextTimeStamp - timeStamp;
					}

					try {
						if (CSVFileConfiguration.isInDepthFiles(name)) {
							Depth depth = createDepth(row, instrument);
							if (timeToNextUpdateMs != Long.MIN_VALUE) {
								depth.setTimeToNextUpdateMs(timeToNextUpdateMs);
							}
							String topic = getTopic(instrument);
							notifyDepth(topic, depth);
						}

						if (CSVFileConfiguration.isInTradeFiles(name)) {
							Trade trade = createTrade(row, instrument);
							if (timeToNextUpdateMs != Long.MIN_VALUE) {
								trade.setTimeToNextUpdateMs(timeToNextUpdateMs);
							}
							String topic = getTopic(instrument);
							notifyTrade(topic, trade);
						}

					} catch (Exception ex) {
						logger.error("Error reading row : {} ", row, ex);
					}

					sleepDifference(readingTable, timeStamp, CSVFileConfiguration.getSpeed());
				} catch (Exception ex) {
					logger.error("unknown error ", ex);
				}
			}
		}
		System.out.println("Finished reading backtest CSV ");
		logger.info("************************* END OF CSV ***************");
		logger.info("End of {} reading table", this.getClass().getSimpleName());
		STOP_COMMAND.setTimestamp(timeStamp);
		notifyCommand(TOPIC_COMMAND, STOP_COMMAND);
		//		notifyEndOfFile(this.getClass().getSimpleName());

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
