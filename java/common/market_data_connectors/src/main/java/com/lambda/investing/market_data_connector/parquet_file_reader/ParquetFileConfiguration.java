package com.lambda.investing.market_data_connector.parquet_file_reader;

import com.lambda.investing.Configuration;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.messaging.TypeMessage;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.lambda.investing.data_manager.csv.CSVUtils.PATH_TO_INSTRUMENT;

@Getter @Setter public class ParquetFileConfiguration {

	protected Logger logger = LogManager.getLogger(ParquetFileConfiguration.class);
	private List<Instrument> instruments;
	private int speed = -1;
	private long initialSleepSeconds = 0;
	private Date startTime, endTime;
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	private Map<String, Instrument> filePathToInstrument;
	private Map<Date, List<String>> depthFilesPath;
	private Map<Date, List<String>> tradeFilesPath;
	private List<Date> datesToLoad;
	private List<String> depthFiles;
	private List<String> tradeFiles;

	public ParquetFileConfiguration(List<Instrument> instruments, int speed, long initialSleepSeconds, Date startTime,
			Date endTime) {
		this.instruments = instruments;

		this.speed = speed;
		this.initialSleepSeconds = initialSleepSeconds;
		this.startTime = startTime;
		this.endTime = endTime;
		setParquetFilesPath();
	}

	public ParquetFileConfiguration(Instrument instrument, int speed, long initialSleepSeconds, Date startTime,
			Date endTime) {
		this.instruments = new ArrayList<>();
		instruments.add(instrument);
		this.speed = speed;
		this.initialSleepSeconds = initialSleepSeconds;
		this.startTime = startTime;
		this.endTime = endTime;
		setParquetFilesPath();
	}

	public ParquetFileConfiguration(Instrument instrument, int speed, long initialSleepSeconds, String startTime,
			String endTime) throws ParseException {
		this.instruments = new ArrayList<>();
		instruments.add(instrument);

		this.speed = speed;
		this.initialSleepSeconds = initialSleepSeconds;
		this.startTime = dateFormat.parse(startTime);
		this.endTime = dateFormat.parse(endTime);
		setParquetFilesPath();
	}

	public ParquetFileConfiguration(Instrument instrument, Date startTime, Date endTime) {
		this.instruments = new ArrayList<>();
		instruments.add(instrument);
		this.startTime = startTime;
		this.endTime = endTime;

		setParquetFilesPath();
	}

	public static List<Date> getDaysBetweenDates(Date startdate, Date enddate) {
		TreeSet<Date> dates = new TreeSet<>();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(startdate);
		dates.add(startdate);
		while (calendar.getTime().before(enddate)) {
			Date result = calendar.getTime();
			dates.add(result);
			calendar.add(Calendar.DATE, 1);
		}

		return new ArrayList<>(dates);
	}

	private String getPath(String type, String date, String instrumentPk) {
		return Configuration.getDataPath() + File.separator + "type=" + type + File.separator + "instrument="
				+ instrumentPk + File.separator + "date=" + date + File.separator + "data.parquet";
	}

	private void setParquetFilesPath() {
		depthFilesPath = new HashMap<>();
		tradeFilesPath = new HashMap<>();
		filePathToInstrument = new HashMap<>();

		depthFiles = new ArrayList<>();
		tradeFiles = new ArrayList<>();

		//btcusd_depth_20200819.csv
		this.datesToLoad = getDaysBetweenDates(startTime, endTime);
		for (Date date : this.datesToLoad) {
			for (Instrument instrument : instruments) {
				String depthFile = getPath(TypeMessage.depth.name(), dateFormat.format(date),
						instrument.getPrimaryKey());
				File depth = new File(depthFile);
				filePathToInstrument.put(depthFile, instrument);
				String tradeFile = getPath(TypeMessage.trade.name(), dateFormat.format(date),
						instrument.getPrimaryKey());
				File trade = new File(tradeFile);
				filePathToInstrument.put(tradeFile, instrument);
				if (depth.exists()) {
					List<String> files = depthFilesPath.getOrDefault(date, new ArrayList<>());
					files.add(depthFile);
					depthFilesPath.put(date, files);
					depthFiles.add(depthFile);

				} else {
					logger.warn("DEPTH File doesn't exist   {}", depthFile);
				}

				if (trade.exists()) {
					List<String> files = tradeFilesPath.getOrDefault(date, new ArrayList<>());
					files.add(tradeFile);
					tradeFilesPath.put(date, files);
					tradeFiles.add(tradeFile);
				} else {
					logger.warn("TRADE File doesn't exist   {}", tradeFile);
				}
			}

		}
		PATH_TO_INSTRUMENT = filePathToInstrument;
		logger.info("Setting File path with {} depths files and {} trades files", depthFilesPath.size(),
				tradeFilesPath.size());

	}

	public boolean isInDepthFiles(String name) {
		return depthFiles.contains(name);

	}

	public boolean isInTradeFiles(String name) {
		return tradeFiles.contains(name);

	}
}
