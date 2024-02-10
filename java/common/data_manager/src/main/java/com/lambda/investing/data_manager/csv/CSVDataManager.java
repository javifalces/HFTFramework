package com.lambda.investing.data_manager.csv;

import com.lambda.investing.data_manager.DataManager;
import com.lambda.investing.data_manager.FileDataUtils;
import com.lambda.investing.model.market_data.CSVable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.Table;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class CSVDataManager implements DataManager {
	Logger logger = LogManager.getLogger(CSVDataManager.class);

	public static String readCSV(String filepath) throws IOException {
		return new String(Files.readAllBytes(Paths.get(filepath)));
	}

	public static void saveCSV(String filepath, String content) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filepath));
		writer.write(content);
		writer.close();
	}

	public static String removeEmptyLines(String content) {
		StringBuilder stringBuffer = new StringBuilder();
		Scanner scanner = new Scanner(content);

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			// process the line
			if (line.isEmpty()) {
				continue;
			}
			stringBuffer.append(line);
			stringBuffer.append(System.lineSeparator());
		}
		stringBuffer = stringBuffer.delete(stringBuffer.lastIndexOf(System.lineSeparator()),
				stringBuffer.length());//remove last line separator

		return stringBuffer.toString();
	}

	@Override public <T extends CSVable> Table getData(String filepath, Class<T> objectType) throws Exception {
		//Read CSV file
		//		https://jtablesaw.github.io/tablesaw/userguide/importing_data.html
		Table output = FileDataUtils.readCSV(filepath);
		//		output = output.sortAscendingOn(TIMESTAMP_COL);
		return output;
	}

	@Override public <T extends CSVable> boolean saveData(List<T> objectList, Class<T> objectType, String filepath) {
		//TODO test it
		StringBuffer stringBuffer = new StringBuffer();

		for (T object : objectList) {
			boolean withHeader = stringBuffer.length() == 0;
			String csvString = object.toCSV(withHeader);
			if (csvString != null) {
				stringBuffer.append(csvString);
				stringBuffer.append(System.lineSeparator());
			}
		}
		stringBuffer = stringBuffer.delete(stringBuffer.lastIndexOf(System.lineSeparator()),
				stringBuffer.length());//remove last line separator
		try {
			saveCSV(filepath, stringBuffer.toString());
		} catch (IOException e) {
			logger.error("cant save into csv {} ", filepath, e);
			return false;
		}

		return true;
	}

}
