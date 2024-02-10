package com.lambda.investing.backtest_engine;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.lambda.investing.Configuration.*;

@Getter
@Setter
public class BacktestConfiguration {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    public static SimpleDateFormat dateFormatTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss");


    private Algorithm algorithm;//if backtest type is zero , is not a must

    private ZeroMqConfiguration rlZeroMqConfiguration;

    //instrument data file
    private List<Instrument> instruments = new ArrayList<>();
    private Date startTime;//start time included
    private Date endTime;//not included
    private int speed = -1;
    private long initialSleepSeconds = 5;
    private String multithreadConfiguration;
    private long delayOrderMs;
    private boolean feesCommissionsIncluded = true;
    private long seed;

    //backtest type
    private BacktestSource backtestSource;
    private BacktestExternalConnection backtestExternalConnection;
    private List<String> MULTITHREADING_CORE_CONTAINS = Arrays
            .asList(new String[] { "multi", "multithread", "multithreading", "multi_thread", "multi_threading" });
    private List<String> SINGLE_THREADING_CORE_CONTAINS = Arrays
            .asList(new String[] { "single", "no_thread", "single_thread", "single_threading", "singlethread" });

    public BacktestConfiguration() {
        this.multithreadConfiguration = Configuration.MULTITHREADING_CORE.toString();
        setMultithreadConfiguration(this.multithreadConfiguration);
        this.delayOrderMs = Configuration.DELAY_ORDER_BACKTEST_MS;
        setDelayOrderMs(this.delayOrderMs);
        this.feesCommissionsIncluded = Configuration.FEES_COMMISSIONS_INCLUDED;
        setFeesCommissionsIncluded(this.feesCommissionsIncluded);
    }

    /**
     * @param startTime included
     * @throws ParseException
     */
    public void setStartTime(String startTime) throws ParseException {
        if (startTime.trim().contains(" ")) {
            this.startTime = dateFormatTime.parse(startTime);
        } else {
            this.startTime = dateFormat.parse(startTime);
        }
    }

    /**
     * @param endTime not included
     * @throws ParseException
     */
    public void setEndTime(String endTime) throws ParseException {
        if (endTime.trim().contains(" ")) {
            this.endTime = dateFormatTime.parse(endTime);
        } else {
            this.endTime = dateFormat.parse(endTime);
        }
    }

    public ZeroMqConfiguration getRLZeroMqConfiguration() {
        if (rlZeroMqConfiguration == null && algorithm != null && algorithm.getParameters() != null) {
            int rlPort = algorithm.getParameterIntOrDefault(algorithm.getParameters(), "rlPort", "rl_port", -1);
            String rlHost = algorithm.getParameterStringOrDefault(algorithm.getParameters(), "rlHost", "rl_host", "localhost");
            // from algorithm parameters
            if (rlPort != -1 && rlHost != null) {
                rlZeroMqConfiguration = new ZeroMqConfiguration("tcp", rlHost, rlPort, "");
                if (USE_IPC_RL_TRAINING) {
                    String address = Configuration.formatLog("{}/{}", algorithm.getAlgorithmInfo(), rlPort);
                    rlZeroMqConfiguration.setIpc(address);
                }
            }
        }
        return rlZeroMqConfiguration;
    }


    public void setMultithreadConfiguration(String multithreadConfiguration) {
        if (!this.multithreadConfiguration.equals(multithreadConfiguration)) {
            this.multithreadConfiguration = multithreadConfiguration;

            if (MULTITHREADING_CORE_CONTAINS.contains(multithreadConfiguration.toLowerCase())) {
                SET_MULTITHREAD_CONFIGURATION(MULTITHREAD_CONFIGURATION.MULTITHREADING);
            }

            if (SINGLE_THREADING_CORE_CONTAINS.contains(multithreadConfiguration.toLowerCase())) {
                SET_MULTITHREAD_CONFIGURATION(MULTITHREAD_CONFIGURATION.SINGLE_THREADING);
                setDelayOrderMs(0);
            }
        }
    }

    public void setDelayOrderMs(long delayOrderMs) {
        if (delayOrderMs != this.delayOrderMs) {
            SET_DELAY_ORDER_BACKTEST_MS(delayOrderMs);
            this.delayOrderMs = delayOrderMs;
        }
    }


    public void setFeesCommissionsIncluded(boolean feesCommissionsIncluded) {
        if (feesCommissionsIncluded != this.feesCommissionsIncluded) {
            SET_FEES_COMMISSIONS_INCLUDED(feesCommissionsIncluded);
            this.feesCommissionsIncluded = feesCommissionsIncluded;
        }
    }

    public void setSeed(long seed) {
        if (seed != this.seed) {
            SET_RANDOM_SEED(seed);
        }
    }

    public void setBacktestSource(String backtestSource) {
        this.backtestSource = BacktestSource.valueOf(backtestSource);
    }

    public void setBacktestExternalConnection(String backtestExternalConnection) {
        this.backtestExternalConnection = BacktestExternalConnection.valueOf(backtestExternalConnection);
    }
}
