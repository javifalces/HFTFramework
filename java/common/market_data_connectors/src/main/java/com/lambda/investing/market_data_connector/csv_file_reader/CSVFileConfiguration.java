package com.lambda.investing.market_data_connector.csv_file_reader;

import com.lambda.investing.Configuration;
import com.lambda.investing.data_manager.FileDataUtils;
import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;
import org.apache.kerby.config.Conf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Getter
@Setter
public class CSVFileConfiguration {


	protected Logger logger = LogManager.getLogger(CSVFileConfiguration.class);
	private List<Instrument> instruments;
	private List<String> depthFilesPath;
	private List<String> tradeFilesPath;
	private int speed=-1;
	private long initialSleepSeconds=0;
	private Date startTime,endTime;
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	private Map<String, Instrument> filePathToInstrument;

	public CSVFileConfiguration(List<Instrument> instruments, int speed, long initialSleepSeconds, Date startTime,
								Date endTime) {
		this.instruments = instruments;

		this.speed = speed;
		this.initialSleepSeconds = initialSleepSeconds;
		this.startTime = startTime;
		this.endTime = endTime;
		setCSVFilesPath();
	}


	public CSVFileConfiguration(Instrument instrument,  int speed, long initialSleepSeconds, Date startTime,
								Date endTime) {
		instruments = new ArrayList<>();
		instruments.add(instrument);

		this.speed = speed;
		this.initialSleepSeconds = initialSleepSeconds;
		this.startTime = startTime;
		this.endTime = endTime;
		setCSVFilesPath();
	}


	public CSVFileConfiguration(Instrument instrument,  int speed, long initialSleepSeconds, String startTime,
								String endTime) throws ParseException {
		instruments = new ArrayList<>();
		instruments.add(instrument);

		this.speed = speed;
		this.initialSleepSeconds = initialSleepSeconds;
		this.startTime = dateFormat.parse(startTime);
		this.endTime =  dateFormat.parse(endTime);
		setCSVFilesPath();
	}

	public List<Date> getDays() {
		return getDaysBetweenDates(this.startTime, this.endTime);
	}

	public CSVFileConfiguration(Instrument instrument,Date startTime,
								Date endTime) {
		instruments = new ArrayList<>();
		instruments.add(instrument);
		this.startTime=startTime;
		this.endTime=endTime;

		setCSVFilesPath();
	}

	public static List<Date> getDaysBetweenDates(Date startdate, Date enddate) {
		List<Date> dates = new ArrayList<>();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(startdate);
		dates.add(startdate);
		while (calendar.getTime().before(enddate)) {
			Date result = calendar.getTime();
			dates.add(result);
			calendar.add(Calendar.DATE, 1);
		}

		return new ArrayList<>(new HashSet<>(dates));
	}

	private void setCSVFilesPath(){
		depthFilesPath=new ArrayList<>();
		tradeFilesPath = new ArrayList<>();
		//btcusd_depth_20200819.csv
		List<Date> listOfDates = getDaysBetweenDates(startTime, endTime);
		filePathToInstrument = new HashMap<>();
		for (Instrument instrument : instruments) {
			String pathPrefix = Configuration.getDataPath() + File.separator + instrument.getPrimaryKey();
			for (Date date : listOfDates) {
				String depthFile = pathPrefix + "_depth_" + dateFormat.format(date) + ".csv";
				File depth = new File(depthFile);
				filePathToInstrument.put(depthFile, instrument);
				String tradeFile = pathPrefix + "_trade_" + dateFormat.format(date) + ".csv";
				File trade = new File(tradeFile);
				filePathToInstrument.put(tradeFile, instrument);
				if (depth.exists()) {
					depthFilesPath.add(depthFile);
				} else {
					logger.warn("DEPTH File doesn't exist   {}", depthFile);
				}

				if (trade.exists()) {
					tradeFilesPath.add(tradeFile);
				} else {
					logger.warn("TRADE File doesn't exist   {}", tradeFile);
				}

			}
		}
		FileDataUtils.PATH_TO_INSTRUMENT = filePathToInstrument;
		logger.info("Setting File path with {} depths files and {} trades files", depthFilesPath.size(), tradeFilesPath.size());

	}

	public boolean isInDepthFiles(String name) {
		String pathComplete = Configuration.getDataPath() + File.separator + name;
		return depthFilesPath.contains(pathComplete);

	}

	public boolean isInTradeFiles(String name) {
		String pathComplete = Configuration.getDataPath() + File.separator + name;
		return tradeFilesPath.contains(pathComplete);

	}
}
