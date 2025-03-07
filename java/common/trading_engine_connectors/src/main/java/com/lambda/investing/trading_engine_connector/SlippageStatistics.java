package com.lambda.investing.trading_engine_connector;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.trading.Verb;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class SlippageStatistics implements Runnable {

    private static boolean RESET_STATISTICS_PER_UPDATE = true;
    private long sleepMs;
    private boolean enable;

    private Map<String, Double> keyToSendPrice;
    private Map<String, Verb> keyToVerb;
    private Map<String, Instrument> keyToInstrument;
    private List<Double> slippages;
    private String header;
    protected Logger logger = LogManager.getLogger(SlippageStatistics.class);

    public SlippageStatistics(String header, long sleepMs) {
        this.header = header;
        this.sleepMs = sleepMs;
        keyToSendPrice = new ConcurrentHashMap<>();
        keyToVerb = new ConcurrentHashMap<>();
        keyToInstrument = new ConcurrentHashMap<>();
        slippages = new ArrayList<>();

        enable = true;
        if (sleepMs > 0) {
            Thread thread = new Thread(this, "SlippageStatistics");
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    public void registerPriceSent(Verb side, Instrument instrument, String key, double priceSent) {
        keyToSendPrice.put(key, priceSent);
        keyToVerb.put(key, side);
        keyToInstrument.put(key, instrument);
    }

    public void registerPriceExecuted(String key, double priceExecuted) {

        Double priceSent = keyToSendPrice.get(key);
        if (priceSent == null) {
            return;
        }

        double slippage = priceExecuted - priceSent;

        Verb side = keyToVerb.get(key);
        if (side == Verb.Sell) {
            slippage = -slippage;
        }
        double slippagePriceTicks = Math.round(slippage / keyToInstrument.get(key).getPriceTick());
        slippages.add(slippagePriceTicks);

        keyToSendPrice.remove(key);
        keyToVerb.remove(key);
        keyToInstrument.remove(key);
    }


    private void printCurrentStatistics() {
        if (slippages.size() > 0) {
            int counter = slippages.size();
            double mean = slippages.stream().mapToDouble(a -> a).average().orElse(0.0);
            double maxSlippage = slippages.stream().mapToDouble(a -> a).max().orElse(0);
            //get percentile 50 75 90 95 99
            double percentile50 = 0;
            double percentile75 = 0;
            double percentile90 = 0;
            double percentile95 = 0;
            double percentile99 = 0;

            percentile50 = slippages.stream().sorted().skip((long) (slippages.size() * 0.5)).findFirst().orElse(0.0);
            percentile75 = slippages.stream().sorted().skip((long) (slippages.size() * 0.75)).findFirst().orElse(0.0);
            percentile90 = slippages.stream().sorted().skip((long) (slippages.size() * 0.9)).findFirst().orElse(0.0);
            percentile95 = slippages.stream().sorted().skip((long) (slippages.size() * 0.95)).findFirst().orElse(0.0);
            percentile99 = slippages.stream().sorted().skip((long) (slippages.size() * 0.99)).findFirst().orElse(0.0);

            logger.info("\tSlippage (ticks) :\tsize:{}\tmean:{}\t50pct:{}\t75pct:{}\t90pct:{}\t95pct:{}\t99pct:{}\tmax:{}", counter, mean,
                    percentile50, percentile75, percentile90, percentile95, percentile99, maxSlippage);

            if (RESET_STATISTICS_PER_UPDATE) {
                slippages.clear();
            }
        }
    }


    @Override
    public void run() {

        while (enable) {


            printCurrentStatistics();

            try {
                Thread.sleep(this.sleepMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


}
