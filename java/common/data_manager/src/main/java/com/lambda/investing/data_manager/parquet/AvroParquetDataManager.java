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

import static com.lambda.investing.data_manager.FileDataUtils.TIMESTAMP_COL;
import static org.apache.parquet.hadoop.ParquetFileWriter.Mode.OVERWRITE;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import tech.tablesaw.api.Table;

public class AvroParquetDataManager extends ParquetDataManager {

    public AvroParquetDataManager() {

    }

    static Logger logger = LogManager.getLogger(AvroParquetDataManager.class);


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

    @Override
    public <T extends CSVable> Table getData(String filepath, Class<T> objectType)
            throws Exception {

        //Read Parquet
        //		https://github.com/rdblue/parquet-examples/blob/master/src/main/java/netflix/example/ReadParquetPojo.java
        Path dataFile = new Path(filepath);
        String name = dataFile.getName();
        String instrumentPK = getInstrumentPK(dataFile);
        Instrument instrument = Instrument.getInstrument(instrumentPK);

        GenericData genericData = getGenericData(objectType);
        Configuration conf = new Configuration();
        Table output = null;

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
                throw new Exception("Parquet can't be read " + dataFile);
            }

        } catch (Exception e) {
            logger.error("Error reading parquet {} ", dataFile, e);
            throw e;
        }
        return output;

    }


}
