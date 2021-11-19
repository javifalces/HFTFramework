package com.lambda.investing.data_manager;

import com.lambda.investing.model.market_data.CSVable;
import tech.tablesaw.api.Table;

import java.util.List;

public interface DataManager {

	<T extends CSVable> Table getData(String filepath, Class<T> objectType) throws Exception;

	<T extends CSVable> boolean saveData(List<T> objectList, Class<T> objectType, String filepath);

}
