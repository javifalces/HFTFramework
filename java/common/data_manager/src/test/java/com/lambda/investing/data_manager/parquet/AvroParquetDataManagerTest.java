package com.lambda.investing.data_manager.parquet;

import com.lambda.investing.ArrayUtils;
import com.lambda.investing.Configuration;
import com.lambda.investing.model.asset.Currency;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.DepthParquet;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.market_data.TradeParquet;
import org.junit.Assert;
import org.junit.Test;
import tech.tablesaw.api.Table;

import java.io.File;
import java.util.*;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;


public class AvroParquetDataManagerTest {

    String lambdaDataPath = "lambda_data";
    AvroParquetDataManager avroParquetDataManager;

    private String getPath(String type, String date, String instrumentPk) {
        return Configuration.getDataPath() + File.separator + "type=" + type + File.separator + "instrument=" + instrumentPk
                + File.separator + "date=" + date + File.separator + "data.parquet";
    }


    public static void AddTestInstruments() {
        Instrument instrument = new Instrument();
        instrument.setPrimaryKey("btcusdt_binance");
        instrument.setSymbol("btcusdt");
        instrument.setMarket("binance");
        instrument.setCurrency(Currency.USDT);
        instrument.setPriceTick(0.01);
        instrument.setQuantityTick(0.00001);
        instrument.setMakerFeePct(0.1);
        instrument.setTakerFeePct(0.1);
        instrument.addMap();


        Instrument instrument2 = new Instrument();
        instrument2.setPrimaryKey("eurusd_darwinex");
        instrument2.setSymbol("eurusd");
        instrument2.setMarket("darwinex");
        instrument2.setCurrency(Currency.USD);
        instrument2.setPriceTick(0.00001);
        instrument2.setQuantityTick(0.01);
        instrument2.addMap();

    }

    public AvroParquetDataManagerTest() throws Exception {
        java.net.URL lambdaDataPathRsrs = AvroParquetDataManager.class.getClassLoader().getResource(lambdaDataPath);
        if (lambdaDataPathRsrs != null) {
            // Set environment variable using system-lambda (Java 17 compatible)
            // Note: This sets it for the current test execution context
            System.setProperty("LAMBDA_DATA_PATH", lambdaDataPathRsrs.getPath());
        }
        AddTestInstruments();
        avroParquetDataManager = new AvroParquetDataManager();
    }

    @Test
    public void testReadDepth() throws Exception {
        java.net.URL lambdaDataPathRsrs = AvroParquetDataManager.class.getClassLoader().getResource(lambdaDataPath);
        if (lambdaDataPathRsrs == null) {
            Assert.fail("Lambda data path resource not found");
            return;
        }

        withEnvironmentVariable("LAMBDA_DATA_PATH", lambdaDataPathRsrs.getPath())
                .execute(() -> {
                    String depthFile = getPath("depth", "20220115", "btcusdt_binance");
                    Table depthParquet = avroParquetDataManager.getData(depthFile, DepthParquet.class);
                    Assert.assertNotNull(depthParquet);
                    System.out.println("Columns: " + ArrayUtils.PrintArrayListString(depthParquet.columnNames(), ","));
                    Assert.assertTrue(depthParquet.rowCount() > 0);

                    Assert.assertEquals(87275, depthParquet.rowCount());
                    Assert.assertEquals(22, depthParquet.columnCount());
                });
    }

    @Test
    public void testReadTrade() throws Exception {
        java.net.URL lambdaDataPathRsrs = AvroParquetDataManager.class.getClassLoader().getResource(lambdaDataPath);
        if (lambdaDataPathRsrs == null) {
            Assert.fail("Lambda data path resource not found");
            return;
        }

        withEnvironmentVariable("LAMBDA_DATA_PATH", lambdaDataPathRsrs.getPath())
                .execute(() -> {
                    String tradeFile = getPath("trade", "20220115", "btcusdt_binance");
                    Table tradeParquet = avroParquetDataManager.getData(tradeFile, TradeParquet.class);
                    Assert.assertNotNull(tradeParquet);
                    System.out.println("Columns: " + ArrayUtils.PrintArrayListString(tradeParquet.columnNames(), ","));
                    Assert.assertTrue(tradeParquet.rowCount() > 0);
                    Assert.assertEquals(451466, tradeParquet.rowCount());
                    Assert.assertEquals(4, tradeParquet.columnCount());
                });
    }

    @Test
    public void saveTable() {
        String filepathTest = "test_avro.parquet";
        File file = new File(filepathTest);
        if (file.exists()) {
            boolean deleted = file.delete();
            Assert.assertTrue("Failed to delete existing test file", deleted);
        }


        List<Trade> listToPersist = new ArrayList<>();
        Trade tradeParquet = Trade.getInstance();
        tradeParquet.setQuantity(1.5);
        tradeParquet.setPrice(2.5);

        listToPersist.add(tradeParquet);
        avroParquetDataManager.saveData(listToPersist, Trade.class, filepathTest);

        Assert.assertTrue(file.exists());
        boolean deleted = file.delete();
        Assert.assertTrue("Failed to delete test file", deleted);
    }

}

