package com.lambda.investing.data_manager.parquet;

import com.lambda.investing.model.market_data.CSVable;
import net.tlabs.tablesaw.parquet.TablesawParquetReadOptions;
import net.tlabs.tablesaw.parquet.TablesawParquetReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.io.File;

import static com.lambda.investing.data_manager.FileDataUtils.TIMESTAMP_COL;

public class TableSawParquetDataManager extends ParquetDataManager {

    public TableSawParquetDataManager() {

    }

    static Logger logger = LogManager.getLogger(TableSawParquetDataManager.class);


    @Override
    public <T extends CSVable> Table getData(String filepath, Class<T> objectType)
            throws Exception {

        //Read Parquet
        File file = new File(filepath);
        if (!file.exists()) {
            logger.error("readParquetTableSaw File {} does not exist", filepath);
            return null;
        }

        TablesawParquetReadOptions.Builder options = TablesawParquetReadOptions.builder(file);


        Table table = new TablesawParquetReader().read(options.build());
        table = renameColumns(table);
        table = table.sortAscendingOn(TIMESTAMP_COL);

        return table;
    }

    private Table renameColumns(Table table) {
        for (Column<?> column : table.columns()) {
            String name = column.name();
            if (name.startsWith("askPrice")) {
                column.setName(name.replace("askPrice", "ask"));
            }
            if (name.startsWith("bidPrice")) {
                column.setName(name.replace("bidPrice", "bid"));
            }
            if (name.startsWith("askQuantity")) {
                column.setName(name.replace("askQuantity", "ask_quantity"));
            }
            if (name.startsWith("bidQuantity")) {
                column.setName(name.replace("bidQuantity", "bid_quantity"));
            }

        }
        return table;
    }

//    private <T extends CSVable> Table transformToTable(List<T> objectList){
//        Table table = Table.create();
//        for (T object : objectList) {
//
//            if (parquetObject instanceof GenericData.Record) {
//                parquetObject = castToModel((GenericData.Record) parquetObject, objectType);
//            }
//
//            table.ad(object.getParquetObject());
//        }
//        return table;
//    }
//    @Override
//    public <T extends CSVable> boolean saveData(List<T> objectList, Class<T> objectType, String filepath) {
//        //	https://stackoverflow.com/questions/35200988/writing-custom-java-objects-to-parquet
//        File dataFile = new File(filepath);
//        File parentPath = dataFile.getParentFile();
//        if (!parentPath.exists()) {
//            logger.warn("{} doesn't exist -> create it", parentPath);
//            parentPath.mkdirs();
//        }
//
//        // Write as Parquet file.
//        Class<?> objectType2 = objectList.get(0).getParquetObject().getClass();
//
//        Schema schema = ReflectData.AllowNull.get().getSchema(objectType2);
//        Path dataFilePath = new Path(filepath);
//        Table table = transformToTable(objectList);
//        new TablesawParquetWriter().write(table, TablesawParquetWriteOptions.builder(filepath).build());
//        //todo save it using this lib
//        return false;
//    }


}
