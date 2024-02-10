package com.lambda.investing.factor_investing_connector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lambda.investing.market_data_connector.Statistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AbstractFactorProvider implements FactorProvider {

    protected Logger logger = LogManager.getLogger(AbstractFactorProvider.class);
    protected Map<FactorListener, String> listenersManager;
    protected Statistics statisticsReceived;//= new Statistics("Data received", 15 * 1000);

    public static Gson GSON = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.FINAL)
            .serializeSpecialFloatingPointValues().disableHtmlEscaping().create();


    public AbstractFactorProvider() {
        listenersManager = new ConcurrentHashMap<>();
    }

    @Override
    public void register(FactorListener listener) {
        listenersManager.put(listener, "");
    }

    @Override
    public void deregister(FactorListener listener) {
        listenersManager.remove(listener);
    }

    @Override
    public void reset() {

    }

    public void setStatisticsReceived(Statistics statisticsReceived) {
        this.statisticsReceived = statisticsReceived;
    }

    public void notifyFactor(long timestamp, Map<String, Double> instrumentWeightsMap) {
        Set<FactorListener> listeners = listenersManager.keySet();
        for (FactorListener factorListener : listeners) {
            factorListener.onWeightsUpdate(timestamp, instrumentWeightsMap);
        }

    }

}
