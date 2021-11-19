package com.lambda.investing.data_manager.parquet;

import com.lambda.investing.data_manager.DataManager;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.reflect.ReflectData;

import org.apache.commons.lang.SystemUtils;
import org.apache.hadoop.fs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;

import static com.lambda.investing.data_manager.csv.CSVUtils.TIMESTAMP_COL;
import static org.apache.parquet.hadoop.ParquetFileWriter.Mode.OVERWRITE;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import tech.tablesaw.api.Table;

public class ParquetDataManager implements DataManager {

	private String cacheBasePath = null;

	private static CompressionCodecName PARQUET_COMPRESSION = CompressionCodecName.GZIP;

	public static void disableWarning() {
		System.err.close();
		System.setErr(System.out);
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

	public ParquetDataManager() {
		//	D:\javif\Coding\cryptotradingdesk\java\common\data_manager\src\main\resources\apache-hadoop-3.1.0-winutils
		//	HADOOP_HOME or hadoop.home.dir
		if (SystemUtils.IS_OS_WINDOWS && System.getenv("HADOOP_HOME") == null) {
			ClassLoader loader = ParquetDataManager.class.getClassLoader();
			File file = new File(loader.getResource("apache-hadoop-3.1.0-winutils/bin/winutils.exe").getFile());
			String resourcesHadoop = file.getParentFile().getParentFile().getAbsolutePath();

			logger.warn("Setting HADOOP_HOME to resources path {}", resourcesHadoop);
			Map<String, String> env = new HashMap<>();
			env.put("HADOOP_HOME", resourcesHadoop);
			disableWarning();
			try {
				setEnv(env);
			} catch (Exception e) {
				logger.error("Error setEnv {} with {} ", "HADOOP_HOME", resourcesHadoop, e);
			}

		} else {
			//Install
			if (System.getenv("HADOOP_HOME") == null) {
				logger.warn("HADOOP_HOME not detected in unix!");
				logger.warn("wget https://www-eu.apache.org/dist/hadoop/common/hadoop-3.3.0/hadoop-3.3.0.tar.gz");
				logger.warn("sudo mv hadoop-3.3.0 /opt/hadoop");
				logger.warn("tar xzf hadoop-3.3.0.tar.gz");
				logger.warn("export HADOOP_HOME=/opt/hadoop");

			}

		}

	}

	public void setCacheBasePath(String cacheBasePath) {
		this.cacheBasePath = cacheBasePath;
		new File(cacheBasePath).mkdirs();
	}

	static Logger logger = LogManager.getLogger(ParquetDataManager.class);

	private <T extends CSVable> GenericData getGenericData(Class<T> objectType) {
		GenericData genericData = new ReflectData(objectType.getClassLoader());
		return genericData;
	}

	private String getInstrumentPK(Path dataFile) {

		String completePath = dataFile.toString();
		String searchPattern = "instrument=";
		int indexInstrument = dataFile.toString().lastIndexOf("instrument=") + searchPattern.length();
		int indexEndInstrument = dataFile.toString().indexOf(Path.SEPARATOR, indexInstrument);
		return completePath.substring(indexInstrument, indexEndInstrument);
	}

	private String createTempName(Path path) {
		String original = path.toString();
		String[] splitted = null;
		try {
			splitted = original.split(File.separator);
		} catch (Exception e) {
			splitted = original.split("/");
		}
		String outputName = "";
		for (int i = splitted.length - 2; i >= 1; i--) {
			if (splitted[i].contains("=")) {
				outputName += splitted[i].split("=")[1];
			}

		}
		return outputName + ".csv";
	}

	private static String ReplaceScientificNotation(String input) {
		String output = input;
		String rowWithouHeader = output;
		if (output.contains(System.lineSeparator())) {
			rowWithouHeader = output.split(System.lineSeparator())[1];
		}
		if (rowWithouHeader.toUpperCase().contains("E")) {
			String[] rowSeparated = output.split(",");
			for (int iteration = 0; iteration < rowSeparated.length; iteration++) {
				String number = rowSeparated[iteration];
				if (number.toUpperCase().contains("E")) {
					double value = Double.parseDouble(number);
					String newValue = String.format("%.0f", value);
					output = output.replace(number, newValue);
				}

			}
		}
		return output;

	}

	@Override public <T extends CSVable> tech.tablesaw.api.Table getData(String filepath, Class<T> objectType)
			throws Exception {

		//Read Parquet
		//		https://github.com/rdblue/parquet-examples/blob/master/src/main/java/netflix/example/ReadParquetPojo.java
		Path dataFile = new Path(filepath);
		String name = dataFile.getName();
		String temp_filename = createTempName(dataFile);
		//cache logic load
		if (cacheBasePath != null) {
			File cachePath = new File(cacheBasePath, temp_filename);
			if (cachePath.exists()) {
				logger.info("reading from cachePath: {}", cachePath.toString());
				tech.tablesaw.api.Table cachePathObj = Table.read().file(cachePath);
				return cachePathObj;
			}
		}

		Configuration conf = new Configuration();
		Table output = null;
		GenericData genericData = getGenericData(objectType);

		String instrumentPK = getInstrumentPK(dataFile);
		Instrument instrument = Instrument.getInstrument(instrumentPK);
		Schema schema = ReflectData.AllowNull.get().getSchema(objectType);
		try (ParquetReader<Object> reader = AvroParquetReader.<Object>builder(dataFile).withDataModel(genericData)
				.disableCompatibility() // always use this (since this is a new project)
				.withConf(conf).build()) {
			Object parquetObject;
			StringBuilder allParquetAsString = new StringBuilder();
			while ((parquetObject = reader.read()) != null) {
				//write Table rows
				try {
					//TODO fix to read depths faster
					if (parquetObject instanceof GenericData.Record) {
						parquetObject = castToModel((GenericData.Record) parquetObject, objectType);
					}
					CSVable objectCSVable = CSVable.getCSVAble(parquetObject, instrument);
					boolean withHeader = allParquetAsString.length() == 0;
					String newRow = objectCSVable.toCSV(withHeader);
					newRow = ReplaceScientificNotation(newRow);
					allParquetAsString.append(newRow);
					allParquetAsString.append(System.lineSeparator());
				} catch (Exception e) {
					continue;
				}

			}
			if (allParquetAsString.length() > 0) {
				allParquetAsString = allParquetAsString.delete(allParquetAsString.lastIndexOf(System.lineSeparator()),
						allParquetAsString.length());//remove last line separator

				output = Table.read().csv(allParquetAsString.toString(), name);
				output = output.sortAscendingOn(TIMESTAMP_COL);
			} else {
				logger.warn("{} is empty or can't be read", dataFile);
				throw new Exception("Parquet cant be read " + dataFile);
			}

		} catch (Exception e) {
			logger.error("Error reading parquet {} ", dataFile, e);
			throw e;
		}

		if (cacheBasePath != null) {
			File cachePath = new File(cacheBasePath, temp_filename);
			cachePath.getParentFile().mkdirs();
			logger.info("writing to cachePath: {}", cachePath.toString());
			output.write().csv(cachePath);
		}

		return output;
	}

	private <T extends CSVable> T castToModel(GenericData.Record parquetObject, Class<T> objectType) {
		if (objectType.equals(DepthParquet.class)) {
			DepthParquet depthParquet = new DepthParquet();
			depthParquet.setTimestamp((Long) parquetObject.get("timestamp"));
			depthParquet.setAskPrice0((Double) parquetObject.get("askPrice0"));
			depthParquet.setAskPrice1((Double) parquetObject.get("askPrice1"));
			depthParquet.setAskPrice2((Double) parquetObject.get("askPrice2"));
			depthParquet.setAskPrice3((Double) parquetObject.get("askPrice3"));
			depthParquet.setAskPrice4((Double) parquetObject.get("askPrice4"));

			depthParquet.setBidPrice0((Double) parquetObject.get("bidPrice0"));
			depthParquet.setBidPrice1((Double) parquetObject.get("bidPrice1"));
			depthParquet.setBidPrice2((Double) parquetObject.get("bidPrice2"));
			depthParquet.setBidPrice3((Double) parquetObject.get("bidPrice3"));
			depthParquet.setBidPrice4((Double) parquetObject.get("bidPrice4"));

			depthParquet.setAskQuantity0((Double) parquetObject.get("askQuantity0"));
			depthParquet.setAskQuantity1((Double) parquetObject.get("askQuantity1"));
			depthParquet.setAskQuantity2((Double) parquetObject.get("askQuantity2"));
			depthParquet.setAskQuantity3((Double) parquetObject.get("askQuantity3"));
			depthParquet.setAskQuantity4((Double) parquetObject.get("askQuantity4"));

			depthParquet.setBidQuantity0((Double) parquetObject.get("bidQuantity0"));
			depthParquet.setBidQuantity1((Double) parquetObject.get("bidQuantity1"));
			depthParquet.setBidQuantity2((Double) parquetObject.get("bidQuantity2"));
			depthParquet.setBidQuantity3((Double) parquetObject.get("bidQuantity3"));
			depthParquet.setBidQuantity4((Double) parquetObject.get("bidQuantity4"));
			return (T) depthParquet;
		}

		if (objectType.equals(TradeParquet.class)) {
			TradeParquet tradeParquet = new TradeParquet();
			tradeParquet.setTimestamp((Long) parquetObject.get("timestamp"));
			tradeParquet.setPrice((Double) parquetObject.get("price"));
			tradeParquet.setQuantity((Double) parquetObject.get("quantity"));
			return (T) tradeParquet;
		}
		logger.error("{} not found to cast ", objectType);
		return null;

	}

	@Override public <T extends CSVable> boolean saveData(List<T> objectList, Class<T> objectType, String filepath) {
		//	https://stackoverflow.com/questions/35200988/writing-custom-java-objects-to-parquet
		File dataFile = new File(filepath);
		File parentPath = dataFile.getParentFile();
		if (!parentPath.exists()) {
			logger.warn("{} doesn't exist -> create it", parentPath);
			parentPath.mkdirs();
		}

		// Write as Parquet file.
		Class<?> objectType2 = objectList.get(0).getParquetObject().getClass();

		Schema schema = ReflectData.AllowNull.get().getSchema(objectType2);
		Path dataFilePath = new Path(filepath);

		try (ParquetWriter<Object> writer = AvroParquetWriter.<Object>builder(dataFilePath).withSchema(schema)
				.withDataModel(ReflectData.get()).withConf(new Configuration())
				.withCompressionCodec(PARQUET_COMPRESSION).withWriteMode(OVERWRITE).build()) {
			logger.debug("writing parquet if {} rows with compression {} into ", objectList.size(),
					PARQUET_COMPRESSION.name(), filepath);
			for (T csvableRow : objectList) {
				if (csvableRow instanceof CSVable) {
					writer.write(csvableRow.getParquetObject());
				}
			}
			logger.debug("writing parquet finished {}", filepath);
		} catch (IOException ex) {
			logger.error("Error saving to parquet {} ", filepath, ex);
			return false;
		}
		return true;

	}

}
