package com.lambda.investing.market_data_connector.persist;

import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.data_manager.csv.CSVDataManager;
import com.lambda.investing.data_manager.csv.CSVUtils;
import com.lambda.investing.data_manager.parquet.ParquetDataManager;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.market_data_connector.Statistics;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.*;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.time.Period;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.lambda.investing.Configuration.FILE_CSV_DATE_FORMAT;
import static com.lambda.investing.data_manager.csv.CSVDataManager.removeEmptyLines;
import static com.lambda.investing.market_data_connector.AbstractMarketDataProvider.GSON;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/***
 * To create parquet from csv
 * http://tgrall.github.io/blog/2015/08/17/convert-csv-file-to-apache-parquet-dot-dot-dot-with-drill/
 */
public class PersistorMarketDataConnector implements Runnable, ConnectorListener {

	private static String SUFFIX_SEPARATOR = "@";
	private static int MAX_RETRIES_PARQUET = 3;
	private static boolean DELETE_PROCESSED_CSV = true;//TODO change when all is fine
	private List<String> ignoredInstruments = new ArrayList<>();
	private Calendar calendar;
	private Statistics statistics;
	protected Logger logger = LogManager.getLogger(PersistorMarketDataConnector.class);

	private static Long DEFAULT_PERIOD_CHECK_MS = 5000L;
	private Period period;
	private long periodCheck;
	Thread threadPersist;
	private SimpleDateFormat dateFormat;
	private SimpleDateFormat parquetDateFormat;
	Thread threadParquetPersist;

	private Map<Instrument, InstrumentCache> instrumentCacheMap;

	ConnectorProvider connectorProvider;
	ConnectorConfiguration connectorConfiguration;
	protected String dataPath;

	protected ParquetDataManager parquetDataManager;
	protected CSVDataManager csvDataManager;
	protected String parquetDataPath;

	private String name;
	private Map<String, Long> fileToErrorCounter;
	private boolean persistParquet = true;
	//	private String persistSuffix = null;

	public PersistorMarketDataConnector(String dataPath, String parquetDataPath, ConnectorProvider connectorProvider,
			ConnectorConfiguration connectorConfiguration) {
		this.dataPath = dataPath;
		this.parquetDataPath = parquetDataPath;
		this.connectorProvider = connectorProvider;
		this.connectorConfiguration = connectorConfiguration;
		constructor(Period.day, DEFAULT_PERIOD_CHECK_MS);
	}

	public void setPersistParquet(boolean persistParquet) {
		this.persistParquet = persistParquet;
	}

	//	public void setPersistSuffix(String persistSuffix) {
	//		this.persistSuffix = persistSuffix;
	//	}

	public PersistorMarketDataConnector(String dataPath, String parquetDataPath, ConnectorProvider connectorProvider,
			ConnectorConfiguration connectorConfiguration, Period period, long periodCheckMs) {
		this.dataPath = dataPath;
		this.parquetDataPath = parquetDataPath;
		this.connectorProvider = connectorProvider;
		this.connectorConfiguration = connectorConfiguration;
		constructor(period, periodCheckMs);
	}

	private String getParquetPath(Instrument instrument, Date date, String type) {
		//		if (persistSuffix != null) {
		//			return parquetDataPath + File.separator + "type=" + type + File.separator + "instrument=" + instrument
		//					.getPrimaryKey() + SUFFIX_SEPARATOR + persistSuffix + File.separator + "date=" + dateFormat
		//					.format(date) + File.separator + "data.parquet";
		//		} else {
		return parquetDataPath + File.separator + "type=" + type + File.separator + "instrument=" + instrument
				.getPrimaryKey() + File.separator + "date=" + dateFormat.format(date) + File.separator + "data.parquet";
		//		}
	}

	public void constructor(Period period, long periodCheckMs) {
		parquetDataManager = new ParquetDataManager();
		fileToErrorCounter = new ConcurrentHashMap<>();
		csvDataManager = new CSVDataManager();
		calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		statistics = new Statistics("PersistorMarketDataConnector", DEFAULT_PERIOD_CHECK_MS);
		instrumentCacheMap = new ConcurrentHashMap<>();
		this.period = period;
		this.periodCheck = periodCheckMs;
		setDateFormat(period);
		this.threadPersist = new Thread(this, "PersistorMarketDataConnector_" + this.period.name());
		this.threadParquetPersist = new Thread(new ParquetPersistor(5000),
				"PersistorParquetMarketDataConnector_" + this.period.name());

	}

	public void init() {
		this.connectorProvider.register(this.connectorConfiguration, this);

		this.threadPersist.start();
		this.threadParquetPersist.start();
		logger.info("Saving {} on {}", this.connectorConfiguration, dataPath);
	}

	private void setDateFormat(Period period) {
		if (period.equals(Period.day)) {
			this.dateFormat = FILE_CSV_DATE_FORMAT;
		}
		//need to be teste
		if (period.equals(Period.hour)) {
			this.dateFormat = new SimpleDateFormat("yyyyMMddHH");
		}
		if (period.equals(Period.minute)) {
			this.dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
		}

		parquetDateFormat = this.dateFormat;//20201230
	}

	private String getFilename(TypeMessage typeMessage, Instrument instrument) {
		//		if (persistSuffix != null) {
		//			return dataPath + File.separator + instrument.getPrimaryKey() + SUFFIX_SEPARATOR + this.persistSuffix + "_"
		//					+ typeMessage.name().toLowerCase() + "_" + dateFormat.format(calendar.getTime()) + ".csv";
		//		} else {
		return dataPath + File.separator + instrument.getPrimaryKey() + "_" + typeMessage.name().toLowerCase() + "_"
				+ dateFormat.format(calendar.getTime()) + ".csv";
		//		}
	}

	public void saveDepth(Depth depth) {
		//add to persistor
		calendar.setTimeInMillis(depth.getTimestamp());
		Instrument instrument = Instrument.getInstrument(depth.getInstrument());
		InstrumentCache instrumentCache = instrumentCacheMap.getOrDefault(instrument, new InstrumentCache(instrument));
		instrumentCache.updateDepth(depth);
		instrumentCacheMap.put(instrument, instrumentCache);
	}

	public void saveTrade(Trade trade) {
		//add to persistor map
		calendar.setTimeInMillis(trade.getTimestamp());
		Instrument instrument = Instrument.getInstrument(trade.getInstrument());
		InstrumentCache instrumentCache = instrumentCacheMap.getOrDefault(instrument, new InstrumentCache(instrument));
		instrumentCache.updateTrade(trade);
		instrumentCacheMap.put(instrument, instrumentCache);

	}

	private StringBuilder addPrevious(File file) {

		StringBuilder fileContent = new StringBuilder();
		if (file.isFile()) {
			try {
				fileContent.append(CSVDataManager.readCSV(file.getAbsolutePath()));
				fileContent.append(System.lineSeparator());

			} catch (IOException e) {
				logger.warn("{} cant be read=> create new", file);
				//				depthFileExist = false;
			}
		}
		return fileContent;
	}

	@Override public void run() {

		while (true) {
			//depth iteration
			synchronized (this) {
				Map<Instrument, InstrumentCache> instrumentCacheMapCopy = new HashMap<>(instrumentCacheMap);
				try {
					for (Map.Entry<Instrument, InstrumentCache> entryInstrumentCache : instrumentCacheMapCopy
							.entrySet()) {

						Instrument instrument = entryInstrumentCache.getKey();
						String filename = getFilename(TypeMessage.depth, instrument);
						File depthFile = new File(filename);
						//depth management
						StringBuilder fileContentDepth = addPrevious(depthFile);
						boolean depthFileExist = !fileContentDepth.toString().isEmpty();

						if (depthFileExist && !fileContentDepth.toString().contains(Depth.headerCSV().toString())) {
							logger.warn("depth file {} without header -> append it", depthFile.toString());
							StringBuilder fileContentDepth2 = new StringBuilder();
							fileContentDepth2.append(Depth.headerCSV());
							fileContentDepth2.append(fileContentDepth);
							fileContentDepth = fileContentDepth2;
						}
						NavigableMap<Long, Depth> timeToDepth = new TreeMap<>(
								entryInstrumentCache.getValue().getDepthCache());
						List<Long> timestampToCleanDepth = new ArrayList<>();
						for (Map.Entry<Long, Depth> entry : timeToDepth.entrySet()) {
							Depth depth = entry.getValue();
							String newRow = depth.toCSV(!depthFileExist);
							depthFileExist = true;
							boolean validRow = newRow != null && !newRow.isEmpty();
							if (validRow) {
								fileContentDepth.append(newRow);
								fileContentDepth.append(System.lineSeparator());
							}
							timestampToCleanDepth.add(entry.getKey());
						}
						if (timeToDepth.size() > 0 && fileContentDepth.toString().contains(System.lineSeparator())) {

							fileContentDepth = fileContentDepth
									.delete(fileContentDepth.lastIndexOf(System.lineSeparator()),
											fileContentDepth.length());//remove last line separator
						}
						String textToWriteDepth = fileContentDepth.toString();
						try {
							if (textToWriteDepth.trim().length() > 0) {
								CSVDataManager.saveCSV(filename, removeEmptyLines(textToWriteDepth));
							}
						} catch (IOException e) {
							logger.error("{} cant be write it!", filename, e);
						}
						statistics.setStatistics(instrument.getPrimaryKey() + "" + ".depth",
								textToWriteDepth.split(System.lineSeparator()).length);

						entryInstrumentCache.getValue().cleanDepth(timestampToCleanDepth);

						//trade management
						String filenameTrade = getFilename(TypeMessage.trade, instrument);
						File tradeFile = new File(filenameTrade);

						StringBuilder fileContentTrade = addPrevious(tradeFile);
						boolean tradeFileExist = !fileContentTrade.toString().isEmpty();

						if (tradeFileExist && !fileContentTrade.toString().contains(Trade.headerCSV().toString())) {
							logger.warn("trade file {} without header -> append it", tradeFile.toString());
							StringBuilder fileContentTrade2 = new StringBuilder();
							fileContentTrade2.append(Trade.headerCSV());
							fileContentTrade2.append(fileContentTrade);
							fileContentTrade = fileContentTrade2;
						}

						NavigableMap<Long, Trade> timeToTrade = new TreeMap<>(
								entryInstrumentCache.getValue().getTradeCache());

						List<Long> timestampToCleanTrade = new ArrayList<>();
						for (Map.Entry<Long, Trade> entry : timeToTrade.entrySet()) {
							Trade trade = entry.getValue();
							String newRow = trade.toCSV(!tradeFileExist);
							tradeFileExist = true;

							boolean validRow = newRow != null && !newRow.isEmpty();
							if (validRow) {
								fileContentTrade.append(newRow);
								fileContentTrade.append(System.lineSeparator());
							}
							timestampToCleanTrade.add(entry.getKey());
						}
						if (timeToTrade.size() > 0 && fileContentTrade.toString().contains(System.lineSeparator())) {
							fileContentTrade = fileContentTrade
									.delete(fileContentTrade.lastIndexOf(System.lineSeparator()),
											fileContentTrade.length());//remove last line separator
						}
						String textToWriteTrade = fileContentTrade.toString();

						try {
							if (textToWriteTrade.trim().length() > 0) {
								CSVDataManager.saveCSV(filenameTrade, removeEmptyLines(textToWriteTrade));
							}
						} catch (IOException e) {
							logger.error("{} cant be write it!", filenameTrade);
						}
						statistics.setStatistics(instrument.getPrimaryKey() + "" + ".trade",
								textToWriteTrade.split(System.lineSeparator()).length);
						entryInstrumentCache.getValue().cleanTrade(timestampToCleanTrade);
					}

				} catch (Exception e) {
					logger.error("Exception running persistor ", e);
				}
			}

			try {
				Thread.sleep(this.periodCheck);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	@Override public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
			TypeMessage typeMessage, String content) {

		if (typeMessage.equals(TypeMessage.depth)) {
			Depth depth = GSON.fromJson(content, Depth.class);
			saveDepth(depth);

		} else if (typeMessage.equals(TypeMessage.trade)) {
			Trade trade = GSON.fromJson(content, Trade.class);
			saveTrade(trade);
		}

	}

	/**
	 * Search csv to create Parquets
	 */
	private class ParquetPersistor implements Runnable {

		private boolean enable = true;
		private long sleepMs;

		public ParquetPersistor(long sleepMs) {
			this.sleepMs = sleepMs;

		}

		@Override public void run() {
			//			try {
			//				Thread.sleep(30000);//initial sleep
			//			} catch (InterruptedException e) {
			//				logger.error("cant initial sleep PersistorMarketDataConnector");
			//			}

			while (enable) {

				try {
					List<String> csvFilesFound = getCSVFilesFinished();
					List<String> filesToProcess = null;
					if (persistParquet) {
						filesToProcess = createParquets(csvFilesFound);
					} else {
						filesToProcess = moveFilesParquetFolder(csvFilesFound);
					}

					markAsProcessed(filesToProcess);

					try {
						Thread.sleep(this.sleepMs);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				} catch (Exception e) {
					logger.error("Error creating parquet creator thread", e);
				}
			}

		}

	}

	private List<String> moveFilesParquetFolder(List<String> csvFilesFound) {
		if (csvFilesFound.size() > 0 && calendar.getTime().getHours() == 0) {
			logger.info("change of day detected => waiting 30 seconds");
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (csvFilesFound.size() == 0) {
			return new ArrayList<>();
		}

		String outputPath = parquetDataPath + File.separator + "csv_raw";
		logger.info("moving {} csv to final raw folder {}", csvFilesFound.size(), outputPath);
		File filePathParent = new File(outputPath);
		if (!filePathParent.exists()) {
			filePathParent.mkdirs();
		}
		List<String> output = new ArrayList<>();
		for (String filepath : csvFilesFound) {
			File file = new File(filepath);
			Path filePath = file.toPath();
			String outputPathFile = outputPath + File.separator + (filePath.getFileName().toString());

			//			using rename
			//			boolean outputRename = file.renameTo(new File(outputPathFile));

			//using move
			boolean outputRename = true;
			try {
				Files.copy(filePath, new File(outputPathFile).toPath(), REPLACE_EXISTING);
				//				Files.delete(filePath);//will delete later
			} catch (Exception e) {
				outputRename = false;
				logger.warn("error move ", e);
			}

			if (outputRename) {
				logger.info("moved raw  {} {}", filePath, outputPathFile);
				output.add(filepath);
			} else {
				logger.error("cant move raw  {} {}", filePath, outputPathFile);

			}
		}
		return output;

	}

	private List<String> getCSVFilesFinished() {
		List<String> output = new ArrayList<>();
		String path = dataPath;

		File f = new File(dataPath);
		String todayFormat = dateFormat.format(calendar.getTime());
		File[] matchingFiles = f.listFiles(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				return !name.endsWith(todayFormat + ".csv") && new File(dir + File.separator + name).isFile();
			}
		});
		if (matchingFiles != null) {
			for (File file : matchingFiles) {
				output.add(file.getAbsolutePath());
			}
		}
		return output;

	}

	private void markAsProcessed(List<String> filesProcessed) {
		if (filesProcessed.size() == 0) {
			return;
		}
		Map<String, String> env = new HashMap<>();
		// Create the zip file if it doesn't exist
		env.put("create", "true");
		String processedPath = parquetDataPath + File.separator + "processed" + File.separator;
		//create it if not exist
		File basePath = new File(processedPath);
		if (!basePath.exists()) {
			basePath.mkdirs();
		}
		logger.info("markAsProcessed {} files", filesProcessed.size());
		Map<String, ZipOutputStream> dateToZos = new HashMap<>();
		for (String filepath : filesProcessed) {
			File file = new File(filepath);
			Path filePath = file.toPath();

			File outputPathFile = new File(processedPath + (filePath.getFileName().toString()));

			boolean outputRename = true;
			try {
				Files.copy(filePath, outputPathFile.toPath(), REPLACE_EXISTING);
				Files.delete(filePath);
			} catch (Exception e) {
				outputRename = false;
				logger.warn("error move ", e);
			}

			logger.info("processed  {} ", filePath);
			//			String zipName = getDateFromCSVFilename(filepath);
			//			String zipFileName = zipName.concat(".zip");
			//
			//			FileOutputStream fos = null;
			//			String zipFileCompletePath = processedPath + zipFileName;
			//			logger.info("markAsProcessed {} -> {}", filepath, zipFileCompletePath);
			//			try {
			//				fos = new FileOutputStream(zipFileCompletePath);
			//				ZipOutputStream zos = dateToZos.getOrDefault(zipName, new ZipOutputStream(fos));
			//				dateToZos.put(zipName, zos);
			//				try {
			//					zos.putNextEntry(new ZipEntry(file.getName()));
			//					byte[] bytes = Files.readAllBytes(Paths.get(filepath));
			//					zos.write(bytes, 0, bytes.length);
			//					zos.closeEntry();
			//				} catch (Exception e) {
			//					logger.error("error saving to zip {} ", file, e);
			//				}
			//			} catch (IOException e) {
			//				logger.error("{} IO exception", zipFileName, e);
			//			}
			//		}
			//
			//		for (ZipOutputStream zos : dateToZos.values()) {
			//			try {
			//				zos.close();
			//			} catch (IOException e) {
			//				logger.error("cant close zip file {} ", zos, e);
			//			}
		}

		//delete  the previous csv?
		if (DELETE_PROCESSED_CSV) {
			for (String filepath : filesProcessed) {
				File fileToDelete = new File(filepath);
				fileToDelete.delete();
			}
		}

	}

	private String getDateFromCSVFilename(String filename) {
		File file = new File(filename);
		String filenameName = file.getName();
		String name = filenameName.split("\\.")[0];
		String[] csvFileSplitted = name.split("_");
		return csvFileSplitted[csvFileSplitted.length - 1];
	}

	private List<String> createParquets(List<String> csvFilesFound) {
		List<String> filesProcessed = new ArrayList<>();
		if (csvFilesFound.size() > 0 && calendar.getTime().getHours() == 0) {
			logger.info("change of day detected => waiting 30 seconds");
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		for (String csvFile : csvFilesFound) {
			File file = new File(csvFile);
			String filename = file.getName();
			String name = filename.split("\\.")[0];
			String[] csvFileSplitted = name.split("_");

			//TODO something better on instruments with isin!
			int index = 0;
			String instrumentPK = csvFileSplitted[index];

			if (csvFileSplitted.length == 4) {
				instrumentPK = csvFileSplitted[index] + "_" + csvFileSplitted[index + 1];
				index = 2;
			}
			if (csvFileSplitted.length == 5) {
				instrumentPK =
						csvFileSplitted[index] + "_" + csvFileSplitted[index + 1] + "_" + csvFileSplitted[index + 2];
				index = 3;
			}

			//			if (persistSuffix != null && instrumentPK.contains(SUFFIX_SEPARATOR)) {
			//				//split with broker
			//				String[] splitInstrumentPk = instrumentPK.split(SUFFIX_SEPARATOR);
			//				instrumentPK = splitInstrumentPk[0];
			//				String suffix = splitInstrumentPk[1].trim();
			//				if (!persistSuffix.equalsIgnoreCase(suffix)) {
			//					//not my file
			//					continue;
			//				}
			//			} else if (persistSuffix != null && !instrumentPK.contains(SUFFIX_SEPARATOR)) {
			//				//not my file
			//				continue;
			//			}
			if (ignoredInstruments.contains(instrumentPK)) {
				continue;
			}
			Instrument instrument = Instrument.getInstrument(instrumentPK);
			if (instrument == null) {
				logger.warn("cant find instrument for pk {} -> add to ignore list", instrumentPK);
				ignoredInstruments.add(instrumentPK);
				continue;
			}
			//TODO something better

			String type = csvFileSplitted[index];
			String dateStr = csvFileSplitted[index + 1];
			Date date = null;
			try {
				date = dateFormat.parse(dateStr);
			} catch (ParseException e) {
				logger.error("cant parse date in {}", csvFile);
				continue;
			}

			String pathOutput = getParquetPath(instrument, date, type);

			logger.info("generating parquet file in {}", pathOutput);
			File f = new File(pathOutput);
			if (!f.getParentFile().exists()) {
				logger.debug("creating path {}", f.getParentFile());
				f.getParentFile().mkdirs();//create directory if i doesnt exist
			}
			try {
				logger.debug("reading type {}", type);
				Class classToPersist = null;
				TypeMessage typeMessage = null;
				if (type.equalsIgnoreCase(TypeMessage.depth.name())) {
					typeMessage = TypeMessage.depth;
					classToPersist = Depth.class;
				} else if (type.equalsIgnoreCase(TypeMessage.trade.name())) {
					typeMessage = TypeMessage.trade;
					classToPersist = Trade.class;
				} else {
					logger.warn("unknown type of message to persist {} => skip it", type);
					continue;
				}
				logger.debug("parquet file as {} -> reading csv {}", typeMessage.name(), csvFile);

				Table readTable = null;
				try {
					readTable = (Table) csvDataManager.getData(csvFile, classToPersist);
					logger.debug("original csv file read {}", csvFile);
				} catch (Exception e) {
					logger.error("error reading csv {} to save in parquet", csvFile, e);
					long currentCounter = fileToErrorCounter.getOrDefault(csvFile, 0L);
					if (currentCounter > MAX_RETRIES_PARQUET) {
						logger.error("reach the limit of {} {} => mas as processed to delete it", MAX_RETRIES_PARQUET,
								csvFile);
						filesProcessed.add(csvFile);
						fileToErrorCounter.remove(csvFile);
						continue;
					}
					fileToErrorCounter.put(csvFile, currentCounter + 1);
					continue;

				}
				if (readTable == null) {
					logger.error("{}  as {} reading result is null", csvFile, classToPersist);
					long currentCounter = fileToErrorCounter.getOrDefault(csvFile, 0L);
					if (currentCounter > MAX_RETRIES_PARQUET) {
						logger.error("reach the limit of {} {} => mas as processed to delete it", MAX_RETRIES_PARQUET,
								csvFile);
						filesProcessed.add(csvFile);
						fileToErrorCounter.remove(csvFile);
						continue;
					}
					fileToErrorCounter.put(csvFile, currentCounter + 1);
					continue;
				}
				List<CSVable> listToPersist = new ArrayList<>();
				logger.debug("reading {} rows of csv to list persist", readTable.rowCount());
				for (Row row : readTable) {
					if (typeMessage == (TypeMessage.depth)) {
						listToPersist.add(CSVUtils.createDepth(row, instrument, ""));
					} else if (typeMessage == (TypeMessage.trade)) {
						listToPersist.add(CSVUtils.createTrade(row, instrument, ""));
					} else {
						logger.warn("unknown type of message to persist {} => skip it", typeMessage);
					}
				}

				logger.debug("saving {} into parquet file {}", type, pathOutput);
				parquetDataManager.saveData(listToPersist, classToPersist, pathOutput);
				logger.debug("saved {} ", pathOutput);

				//Check is the same size
				Table parquetCreated = parquetDataManager.getData(pathOutput, classToPersist);
				//same size as readTable?
				if (parquetCreated.rowCount() != readTable.rowCount()) {
					logger.error("different number of rows output {} than input file {}", parquetCreated.rowCount(),
							readTable.rowCount());
				}
				filesProcessed.add(csvFile);

			} catch (Exception e) {
				logger.error("cant read CSV {} to transform to parquet", csvFile, e);

			}

		}
		return filesProcessed;
	}

	@Getter @Setter private class InstrumentCache {

		private NavigableMap<Long, Depth> depthCache;//last timestamp to depth
		private NavigableMap<Long, Trade> tradeCache;//last timestamp to trade
		private Instrument instrument;

		public InstrumentCache(Instrument instrument) {
			this.instrument = instrument;
			depthCache = Collections.synchronizedNavigableMap(new TreeMap<>());
			tradeCache = Collections.synchronizedNavigableMap(new TreeMap<>());
		}

		public synchronized void updateTrade(Trade trade) {
			tradeCache.put(trade.getTimestamp(), trade);
		}

		public synchronized void updateDepth(Depth depth) {
			depthCache.put(depth.getTimestamp(), depth);
		}

		public synchronized void cleanDepth(List<Long> timestamps) {
			for (Long timestamp : timestamps) {
				depthCache.remove(timestamp);
			}
		}

		public synchronized void cleanTrade(List<Long> timestamps) {
			for (Long timestamp : timestamps) {
				tradeCache.remove(timestamp);
			}
		}
	}
}
