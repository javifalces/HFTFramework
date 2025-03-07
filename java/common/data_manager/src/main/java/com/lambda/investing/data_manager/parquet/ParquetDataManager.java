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
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

public abstract class ParquetDataManager implements DataManager {

    protected static CompressionCodecName PARQUET_COMPRESSION = CompressionCodecName.SNAPPY;

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
            logger.warn("HADOOP winutils.exe in {} , exist {}", file.getAbsolutePath(), file.exists());

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
            logger.info("WIN System detected with HADOOP_HOME on {}", System.getenv("HADOOP_HOME"));
        } else if (SystemUtils.IS_OS_WINDOWS && System.getenv("HADOOP_HOME") != null) {
            logger.info("WIN System detected with HADOOP_HOME on {}", System.getenv("HADOOP_HOME"));
        } else {
            //Install
            if (System.getenv("HADOOP_HOME") == null) {
                logger.warn("HADOOP_HOME not detected in unix!");
                logger.warn("wget https://www-eu.apache.org/dist/hadoop/common/hadoop-3.3.0/hadoop-3.3.0.tar.gz");
                logger.warn("sudo mv hadoop-3.3.0 /opt/hadoop");
                logger.warn("tar xzf hadoop-3.3.0.tar.gz");
                logger.warn("export HADOOP_HOME=/opt/hadoop");
            }
            logger.info("UNIX System detected with HADOOP_HOME on {}", System.getenv("HADOOP_HOME"));

        }

    }


    protected <T extends CSVable> GenericData getGenericData(Class<T> objectType) {
        GenericData genericData = new ReflectData(objectType.getClassLoader());
        return genericData;
    }

    protected String getInstrumentPK(Path dataFile) {

        String completePath = dataFile.toString();
        String searchPattern = "instrument=";
        int indexInstrument = dataFile.toString().lastIndexOf("instrument=") + searchPattern.length();
        int indexEndInstrument = dataFile.toString().indexOf(Path.SEPARATOR, indexInstrument);
        return completePath.substring(indexInstrument, indexEndInstrument);
    }

    static Logger logger = LogManager.getLogger(ParquetDataManager.class);


    /***
     *
     * @param filepath parquet file to read
     * @param objectType kind of object
     * @return Table type of data
     * @param <T>
     * @throws Exception
     */
    public abstract <T extends CSVable> Table getData(String filepath, Class<T> objectType)
            throws Exception;


    /***
     *
     * @param objectList List<CSVable> items to transform to table</>
     * @param objectType kind of object to transform
     * @param filepath destiny
     * @return true if success
     * @param <T>
     */
//    public abstract <T extends CSVable> boolean saveData(List<T> objectList, Class<T> objectType, String filepath);
    @Override
    public <T extends CSVable> boolean saveData(List<T> objectList, Class<T> objectType, String filepath) {
        //	https://stackoverflow.com/questions/35200988/writing-custom-java-objects-to-parquet
        File dataFile = new File(filepath);
        File parentPath = dataFile.getParentFile();
        if (parentPath != null && !parentPath.exists()) {
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
            logger.debug("writing parquet of {} rows with compression {} into {}", objectList.size(),
                    PARQUET_COMPRESSION.name(), filepath);
            for (T csvableRow : objectList) {
                if (csvableRow instanceof CSVable) {
                    writer.write(csvableRow.getParquetObject());
                }
            }
            logger.debug("written {} rows parquet finished {}", objectList.size(), filepath);
        } catch (IOException ex) {
            logger.error("Error saving {} rows to parquet {} ", objectList.size(), filepath, ex);
            return false;
        }
        return true;

    }

}
