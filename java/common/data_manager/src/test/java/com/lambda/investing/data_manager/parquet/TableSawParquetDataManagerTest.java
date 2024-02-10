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
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import tech.tablesaw.api.Table;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;


@RunWith(MockitoJUnitRunner.class)
public class TableSawParquetDataManagerTest {

    String lambdaDataPath = "lambda_data";
    TableSawParquetDataManager tableSawParquetDataManager;

    private String getPath(String type, String date, String instrumentPk) {
        return Configuration.getDataPath() + File.separator + "type=" + type + File.separator + "instrument=" + instrumentPk
                + File.separator + "date=" + date + File.separator + "data.parquet";
    }

    private static void setEnv(Map<String, String> newenv) throws Exception {
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

    public TableSawParquetDataManagerTest() throws Exception {
        Map<String, String> env = new HashMap<>();
        String lambdaDataPathRsrs = ParquetDataManager.class.getClassLoader().getResource(lambdaDataPath).getPath();
        env.put("LAMBDA_DATA_PATH", lambdaDataPathRsrs);
        setEnv(env);
        AddTestInstruments();
        tableSawParquetDataManager = new TableSawParquetDataManager();
    }

    @Test
    public void testReadDepth() throws Exception {
        String depthFile = getPath("depth", "20220115", "btcusdt_binance");
        Table depthParquet = tableSawParquetDataManager.getData(depthFile, DepthParquet.class);
        Assert.assertNotNull(depthParquet);
        System.out.println("Columns: " + ArrayUtils.PrintArrayListString(depthParquet.columnNames(), ","));
        Assert.assertTrue(depthParquet.rowCount() > 0);
        Assert.assertEquals(87275, depthParquet.rowCount());
        Assert.assertEquals(22 - 1, depthParquet.columnCount());
    }

    @Test
    public void testReadTrade() throws Exception {
        String tradeFile = getPath("trade", "20220115", "btcusdt_binance");
        Table tradeParquet = tableSawParquetDataManager.getData(tradeFile, TradeParquet.class);
        Assert.assertNotNull(tradeParquet);
        System.out.println("Columns: " + ArrayUtils.PrintArrayListString(tradeParquet.columnNames(), ","));
        Assert.assertTrue(tradeParquet.rowCount() > 0);
        Assert.assertEquals(451466, tradeParquet.rowCount());
        Assert.assertEquals(4 - 1, tradeParquet.columnCount());
    }

    @Test
    public void saveTable() {
        String filepathTest = "test_tablesaw.parquet";
        File file = new File(filepathTest);
        if (file.exists()) {
            file.delete();
        }


        List<Trade> listToPersist = new ArrayList<>();
        Trade tradeParquet = new Trade();
        tradeParquet.setQuantity(1.5);
        tradeParquet.setPrice(2.5);

        listToPersist.add(tradeParquet);
        tableSawParquetDataManager.saveData(listToPersist, Trade.class, filepathTest);

        Assert.assertTrue(file.exists());
        file.delete();

    }

}