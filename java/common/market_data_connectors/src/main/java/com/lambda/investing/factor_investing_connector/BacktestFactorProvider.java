package com.lambda.investing.factor_investing_connector;

import com.lambda.investing.Configuration;
import com.lambda.investing.market_data_connector.MarketDataListener;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.market_data_connector.parquet_file_reader.ParquetFileConfiguration;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import org.apache.avro.generic.GenericData;
import org.apache.avro.reflect.ReflectData;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class BacktestFactorProvider extends AbstractFactorProvider implements MarketDataListener {
    private static long TRESHOLD_WARNING_MS = 60 * 60 * 1000;//60 minutes far
    private MarketDataProvider marketDataProvider;
    private List<Instrument> instruments;
    private String model;

    private Date startTime;
    private Date endTime;

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    private List<Date> datesToLoad;

    private SortedMap<Long, Map<String, Double>> factorHistoricalData;
    private Long[] factorHistoricalDataKeys;

    private int lastNotifyIndex;
    private long lastNotifyTimestamp;

    public BacktestFactorProvider(MarketDataProvider marketDataProvider, List<Instrument> instruments, String model, Date startTime, Date endTime) {
        this.marketDataProvider = marketDataProvider;
        this.startTime = startTime;
        this.endTime = endTime;
        this.instruments = instruments;
        this.model = model;
        this.datesToLoad = ParquetFileConfiguration.getDaysBetweenDates(this.startTime, this.endTime);
        this.lastNotifyIndex = -1;
        this.lastNotifyTimestamp = 0;
        readFactorFiles();
        this.marketDataProvider.register(this);
    }


    private String getPath(String date) {
        return Configuration.getDataPath() + File.separator + "type=factor" + File.separator + "model=" + this.model
                + File.separator + "date=" + date + File.separator + "factor.parquet";
    }

    private SortedMap<Long, Map<String, Double>> readFactorParquet(String dataPath) {
        SortedMap<Long, Map<String, Double>> output = new TreeMap<>();
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();

        GenericData genericData = new ReflectData(Map.class.getClass().getClassLoader());
//        Schema schema = ReflectData.AllowNull.get().getSchema(type);
        Path dataFile = new Path(dataPath);

        try (ParquetReader<Object> reader = AvroParquetReader.<Object>builder(dataFile).withDataModel(genericData)
                .disableCompatibility() // always use this (since this is a new project)
                .withConf(conf).build()) {
            Object parquetObject;

            while ((parquetObject = reader.read()) != null) {
                //write Table rows
                try {
                    GenericData.Record record = (GenericData.Record) parquetObject;
                    Long timestamp = (Long) record.get("timestamp");
//                    Date dateKey = new Date(timestamp);
                    Map<String, Double> insideMap = new HashMap();

                    for (Instrument instrument : instruments) {
                        String instrumentPk = instrument.getPrimaryKey();
                        Double weightInstrument = (Double) record.get(instrumentPk);
                        insideMap.put(instrumentPk, weightInstrument);
                    }
                    output.put(timestamp, insideMap);
                } catch (Exception e) {
                    continue;
                }
            }

        } catch (Exception e) {
            logger.error("Error reading factor parquet {}  return it with {} rows", dataFile, output.size(), e);
            System.err.println(Configuration.formatLog("ERROR loading factor historical data {} ({}) :{}", model, dataPath, e.getMessage()));
        }

        return output;
    }

    private void readFactorFiles() {
        SortedMap<Long, Map<String, Double>> historicalData = new TreeMap<>();
        System.out.println(Configuration.formatLog("loading {} factor data {} dates between {} and {} ", this.model, this.datesToLoad.size(), startTime, endTime));
        for (Date date : this.datesToLoad) {
            String dateStr = dateFormat.format(date);
            String dataPath = getPath(dateStr);
            SortedMap<Long, Map<String, Double>> dateFactors = readFactorParquet(dataPath);
            historicalData.putAll(dateFactors);
        }

        factorHistoricalData = historicalData;
        factorHistoricalDataKeys = new Long[factorHistoricalData.size()];
        factorHistoricalDataKeys = factorHistoricalData.keySet().toArray(factorHistoricalDataKeys);
        String datesList = StringUtils.join(datesToLoad, ", ");
        String message = Configuration.formatLog("loaded factor files on {} dates[{}] with {} rows", datesToLoad.size(), datesList, factorHistoricalData.size());
        System.out.println(message);
        logger.info(message);

    }

    //just in case.....
    private Map<String, Double> NormalizeFactors(Map<String, Double> input) {
        //
        Map<String, Double> output = new HashMap<>();

        double positiveSum = 0.0;
        double negativeSum = 0.0;
        for (Double weight : input.values()) {
            if (weight >= 0) {
                positiveSum += weight;
            } else {
                negativeSum += Math.abs(weight);
            }
        }
        for (String instrumentPk : input.keySet()) {
            double weight = input.get(instrumentPk);
            double normalizeWeight = weight >= 0 ? weight / positiveSum : weight / negativeSum;
            output.put(instrumentPk, normalizeWeight);
        }
        return output;
    }

    /***
     *
     * @param timestamp
     * @return previous index of factorHistoricalData where key>timestamp  ex:at 9:00:00.137 returns index of factor at 9:00:00.000 , -1 if value not found
     */
    private int searchFirstIndexBefore(long timestamp) {
        for (int i = lastNotifyIndex + 1; i < factorHistoricalData.size(); i++) {
            Long timestampMap = factorHistoricalDataKeys[i];
            if (timestampMap >= timestamp) {
                if (timestampMap - timestamp > TRESHOLD_WARNING_MS) {
                    logger.warn("notify Factor too far in dates {} searching {} > {} ms ", new Date(timestampMap), new Date(timestamp), TRESHOLD_WARNING_MS);
                }
                return Math.max(i - 1, -1);
            }
        }
        return -1;
    }

    private void notifyFactor(long timestamp, long nextTimestampMs) {
        //read parquet and wait until timestamp is the previous to our factor data
        Map<String, Double> notificationMap = null;

        if (lastNotifyTimestamp > timestamp + nextTimestampMs) {
            //dont search anything , we have to wait
            return;
        }
        //find the row
        int index = searchFirstIndexBefore(timestamp);
        if (index == lastNotifyIndex) {
            //not found or already notified
            return;
        }


        Long timestampToNotify = factorHistoricalDataKeys[index];

        notificationMap = factorHistoricalData.get(timestampToNotify);
        //update temps
        lastNotifyTimestamp = timestampToNotify;
        lastNotifyIndex = index;

        if (notificationMap != null) {
            if (notificationMap.size() != this.instruments.size()) {
                logger.warn("we have {} instruments on BacktestFactorProvider and factor map has {} instruments -> normalize it", this.instruments.size(), notificationMap.size());
                notificationMap = NormalizeFactors(notificationMap);
            }
//            logger.info("{} notifyFactor -> {} weights",new Date(timestampToNotify),notificationMap.size());
            notifyFactor(timestamp, notificationMap);
        } else {
            logger.error("something strange happen timestampToNotify found {} on index {} but notificationMap is null", timestampToNotify, index);
        }

    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        notifyFactor(depth.getTimestamp(), depth.getTimeToNextUpdateMs());
        return false;
    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        notifyFactor(trade.getTimestamp(), trade.getTimeToNextUpdateMs());
        return false;
    }

    @Override
    public boolean onCommandUpdate(Command command) {
        return false;
    }

    @Override
    public boolean onInfoUpdate(String header, String message) {
        return false;
    }
}




