package com.lambda.investing.market_data_connector.parquet_file_reader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lambda.investing.ArrayUtils;
import com.lambda.investing.Configuration;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.hadoop.fs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.lambda.investing.Configuration.TEMP_PATH;

@Setter
@Getter
public class CacheManager implements Serializable {
    private static String CACHE_BASE_PATH = TEMP_PATH + "\\MarketDataCache";

    Logger logger = LogManager.getLogger(CacheManager.class);

    private String dateString;
    private String startDateString;
    private String endDateString;


    private String depthFilesString;
    private String tradeFilesString;

    public static Gson GSON = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.FINAL)
            .serializeSpecialFloatingPointValues().disableHtmlEscaping().create();
    private static String DATE_PATTERN = "ddMMyyyy";
    private static String START_END_DATE_PATTERN = "ddMMyyyyHH";

    public CacheManager(Date date, Date startDate, Date endDate, List<String> depthFiles, List<String> tradesFile) {
        Format dateFormatter = new SimpleDateFormat(DATE_PATTERN);
        Format startEndFormatter = new SimpleDateFormat(START_END_DATE_PATTERN);

        this.dateString = dateFormatter.format(date);
        this.startDateString = startEndFormatter.format(startDate);
        this.endDateString = startEndFormatter.format(endDate);

        List<String> depthFilesModified = setFilesString(depthFiles);

        List<String> tradesFileModified = setFilesString(tradesFile);

        this.depthFilesString = ArrayUtils.PrintArrayListString(depthFilesModified, ",");
        this.tradeFilesString = ArrayUtils.PrintArrayListString(tradesFileModified, ",");
    }

    private List<String> setFilesString(List<String> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> output = new ArrayList<>();
        String dataPath = Configuration.DATA_PATH;
        for (String file : files) {
            String fileOutput = file.replace(dataPath, "").toLowerCase();
            while (fileOutput.startsWith(Path.SEPARATOR) || fileOutput.startsWith("\\")) {
                fileOutput = fileOutput.substring(1);
            }
            output.add(fileOutput);
        }
        return output;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheManager that = (CacheManager) o;
        return Objects.equals(dateString, that.dateString) && Objects.equals(startDateString, that.startDateString) && Objects.equals(endDateString, that.endDateString) && Objects.equals(depthFilesString, that.depthFilesString) && Objects.equals(tradeFilesString, that.tradeFilesString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateString, startDateString, endDateString, depthFilesString, tradeFilesString);
    }

    public String getUUID() {
        byte[] bytes = SerializationUtils.serialize(this);
        return UUID.nameUUIDFromBytes(bytes).toString();
    }

    public File getCacheFile() {
        //https://stackoverflow.com/questions/10587506/creating-a-hash-from-several-java-string-objects
        String filenameCache = Configuration.formatLog("{}.csv", getUUID());
        File cacheFile = new File(CACHE_BASE_PATH + "\\" + filenameCache);

        return cacheFile;
    }

    @Override
    public String toString() {
        return "CacheManager{" +
                "dateString='" + dateString + '\'' +
                ", startDateString='" + startDateString + '\'' +
                ", endDateString='" + endDateString + '\'' +
                ", depthFilesString='" + depthFilesString + '\'' +
                ", tradeFilesString='" + tradeFilesString + '\'' +
                '}';
    }

    public Table loadCache(File cacheFile) {

        try {
            if (!cacheFile.exists()) {
                System.err.println(Configuration.formatLog("error reading cache {} not found", cacheFile.getAbsolutePath()));
                return null;
            }
            Table output = Table.read().csv(cacheFile);
            return output;

        } catch (Exception e) {
            System.err.println(Configuration.formatLog("error reading cache {} {}", cacheFile, e.getMessage()));
            logger.error("error reading cache {} {}", cacheFile, e.getMessage(), e);
        }
        return null;
    }

    public void saveCache(Table output, File cacheFile) {
        try {
            if (cacheFile.exists()) {
                cacheFile.delete();
            }
            cacheFile.getParentFile().mkdirs();

            output.write().toFile(cacheFile);

        } catch (Exception e) {
            System.err.println(Configuration.formatLog("error saving cache {} {}", cacheFile.getAbsolutePath(), e.getMessage()));
            logger.error("error saving cache {} {}", cacheFile.getAbsolutePath(), e.getMessage(), e);
        }
    }


}