package com.lambda.investing.market_data_connector;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.zero_mq.ZeroMqPublisher;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.messaging.TypeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.lambda.investing.market_data_connector.AbstractMarketDataProvider.GSON;

public abstract class AbstractMarketDataConnectorPublisher implements MarketDataConnectorPublisher {

    protected boolean enable = true;
    protected ConnectorConfiguration connectorConfiguration;
    protected ConnectorPublisher connectorPublisher;
    protected Statistics statistics;
    protected Logger logger = LogManager.getLogger(AbstractMarketDataConnectorPublisher.class);
    private boolean isZeroMq = false;
    protected String outputPath;
    private List<MarketDataConnectorPublisherListener> listenerList;

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    protected static CountDownLatch latchPauseTradingEngine;
    protected static final Object lockLatchPauseTradingEngine = new Object();
    protected static CountDownLatch latchPauseRlGym;
    protected static final Object lockLatchPauseRlGym = new Object();
    private static volatile boolean PAUSE_TRADING_ENGINE = false;
    private static volatile boolean PAUSE_RL_GYM = false;

    public static volatile boolean FORCE_STOP_BACKTEST = false;

    public static int SPEED = Integer.MIN_VALUE;
    protected static boolean BACKTEST_IS_READY = false;

    public AbstractMarketDataConnectorPublisher(ConnectorConfiguration connectorConfiguration, ConnectorPublisher connectorPublisher) {
        //		this.statistics = new Statistics(headerStatistics,sleepStatistics);
        this.connectorConfiguration = connectorConfiguration;
        this.connectorPublisher = connectorPublisher;
        if (connectorPublisher instanceof ZeroMqPublisher) {
            isZeroMq = true;
        }
        listenerList = new ArrayList<>();
    }

    public static boolean isBacktestReady() {
        return BACKTEST_IS_READY;
    }

    public static boolean isPause() {
        return PAUSE_TRADING_ENGINE || PAUSE_RL_GYM;
    }

    /**
     * Pause method to stop backtest when a new order is being processed nad is set to true wuen the ER is notified
     *
     * @param pause
     */
    public static void setPauseTradingEngine(boolean pause) {
        synchronized (lockLatchPauseTradingEngine) {
            if (pause != PAUSE_TRADING_ENGINE) {
                PAUSE_TRADING_ENGINE = pause;
                latchPauseTradingEngine = setPauseLatch(latchPauseTradingEngine, pause);
            }
        }
    }

    /**
     * Pause method to stop backtest when state is send to python until the new step is received
     * @param pause
     */
    public static void setPauseRLGym(boolean pause) {
        synchronized (lockLatchPauseRlGym) {
            if (pause != PAUSE_RL_GYM) {
                PAUSE_RL_GYM = pause;
                latchPauseRlGym = setPauseLatch(latchPauseRlGym, pause);
            }
        }
    }

    private static CountDownLatch setPauseLatch(CountDownLatch latchPauseInput, boolean pause) {
        if (pause) {
            if (latchPauseInput == null || latchPauseInput.getCount() == 0) {
                latchPauseInput = new CountDownLatch(1);
            }
        } else {
            if (latchPauseInput != null) {
                latchPauseInput.countDown();
            }
        }
        return latchPauseInput;
    }

    /**
     * @return false if timeout
     */

    protected static boolean waitIfPause(long timeoutMs) {

        if (PAUSE_TRADING_ENGINE && latchPauseTradingEngine != null && latchPauseTradingEngine.getCount() > 0) {
            try {
                boolean output = true;
                if (timeoutMs > 0) {
                    output = latchPauseTradingEngine.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                } else {
                    latchPauseTradingEngine.await();
                }
                return output;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (PAUSE_RL_GYM && latchPauseRlGym != null && latchPauseRlGym.getCount() > 0) {
            try {
                boolean output = true;
                if (timeoutMs > 0) {
                    output = latchPauseRlGym.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                } else {
                    latchPauseRlGym.await();
                }
                return output;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    protected static void resetPause() {
        setPauseTradingEngine(false);
        setPauseRLGym(false);
    }

    public AbstractMarketDataConnectorPublisher(String name, ConnectorConfiguration connectorConfiguration,
                                                ConnectorPublisher connectorPublisher) {
        //		this.statistics = new Statistics(name, sleepStatistics);
        this.connectorConfiguration = connectorConfiguration;
        this.connectorPublisher = connectorPublisher;
        if (connectorPublisher instanceof ZeroMqPublisher) {
            isZeroMq = true;
        }
        listenerList = new ArrayList<>();
    }

    public void register(MarketDataConnectorPublisherListener listener) {
        listenerList.add(listener);
    }

    public void notifyEndOfFile() {
        for (MarketDataConnectorPublisherListener listener : listenerList) {
            listener.notifyEndOfFile();
        }
    }

    public abstract void init();

    public ConnectorConfiguration getConnectorConfiguration() {
        return connectorConfiguration;
    }

    @Override
    public void start() {
        enable = true;
    }

    @Override
    public void stop() {
        enable = false;
    }


    @Override
    public void notifyDepth(String topic, Depth depth) {
        String depthJson = GSON.toJson(depth);
        topic = topic + "." + TypeMessage.depth.name();
        //		logger.debug("notify DEPTH {}",depth.toString());
        connectorPublisher.publish(connectorConfiguration, TypeMessage.depth, topic, depthJson);
        if (statistics != null)
            statistics.addStatistics(topic);
    }

    @Override
    public void notifyTrade(String topic, Trade trade) {
        String tradeJson = GSON.toJson(trade);
        topic = topic + "." + TypeMessage.trade.name();
        //		logger.debug("notify TRADE {}",trade.toString());
        connectorPublisher.publish(connectorConfiguration, TypeMessage.trade, topic, tradeJson);
        if (statistics != null)
            statistics.addStatistics(topic);
    }

    public synchronized void notifyCommand(String topic, Command command) {
        String commandJson = GSON.toJson(command);

        if (isZeroMq) {
            ZeroMqPublisher zeroMqPublisher = (ZeroMqPublisher) connectorPublisher;
            int beforeCounter = zeroMqPublisher.getOKReceived();
            while (zeroMqPublisher.getOKReceived() == beforeCounter) {
                connectorPublisher.publish(connectorConfiguration, TypeMessage.command, topic, commandJson);
                if (statistics != null)
                    statistics.addStatistics(topic);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        } else {
            connectorPublisher.publish(connectorConfiguration, TypeMessage.command, topic, commandJson);
            if (statistics != null)
                statistics.addStatistics(topic);
        }

        if (statistics != null)
            statistics.addStatistics(topic);

    }
}
