package com.lambda.investing.trading_engine_connector;

import com.lambda.investing.StatisticsScheduler;
import com.lambda.investing.PrometheusMetricsExporter;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.trading.Verb;
import io.prometheus.client.Gauge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SlippageStatistics {

    private static boolean RESET_STATISTICS_PER_UPDATE = true;
    private long sleepMs;
    private volatile boolean enable;

    private Map<String, Double> keyToSendPrice;
    private Map<String, Verb> keyToVerb;
    private Map<String, Instrument> keyToInstrument;
    private List<Double> slippages;
    private String header;
    protected Logger logger = LogManager.getLogger(SlippageStatistics.class);

    // Prometheus metrics (lazily initialised when Prometheus is enabled)
    private Gauge prometheusCount;
    private Gauge prometheusMean;
    private Gauge prometheusMax;
    private Gauge prometheusP50;
    private Gauge prometheusP75;
    private Gauge prometheusP90;
    private Gauge prometheusP95;
    private Gauge prometheusP99;
    private final String prometheusPrefix;

    public SlippageStatistics(String header, long sleepMs) {
        this.header = header;
        this.sleepMs = sleepMs;
        keyToSendPrice = new ConcurrentHashMap<>();
        keyToVerb = new ConcurrentHashMap<>();
        keyToInstrument = new ConcurrentHashMap<>();
        slippages = new ArrayList<>();
        this.prometheusPrefix = toPrometheusName("slippage_statistics_" + header);
        initPrometheusMetrics();

        enable = true;
        if (sleepMs > 0) {
            StatisticsScheduler.EXECUTOR.scheduleAtFixedRate(this::printCurrentStatistics, sleepMs, sleepMs, TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        enable = false;
    }

    /**
     * Sanitises a string so it can be used as a Prometheus metric name.
     * Prometheus names must match {@code [a-zA-Z_:][a-zA-Z0-9_:]*}.
     */
    private static String toPrometheusName(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9_:]", "_").toLowerCase();
    }

    private void initPrometheusMetrics() {
        PrometheusMetricsExporter exporter = PrometheusMetricsExporter.getInstance();
        if (!exporter.isEnabled()) {
            return;
        }
        try {
            prometheusCount = Gauge.build()
                    .name(prometheusPrefix + "_count")
                    .help("Number of slippage samples for " + header)
                    .register(exporter.getRegistry());
            prometheusMean = Gauge.build()
                    .name(prometheusPrefix + "_mean_ticks")
                    .help("Mean slippage in ticks for " + header)
                    .register(exporter.getRegistry());
            prometheusMax = Gauge.build()
                    .name(prometheusPrefix + "_max_ticks")
                    .help("Max slippage in ticks for " + header)
                    .register(exporter.getRegistry());
            prometheusP50 = Gauge.build()
                    .name(prometheusPrefix + "_p50_ticks")
                    .help("50th percentile slippage in ticks for " + header)
                    .register(exporter.getRegistry());
            prometheusP75 = Gauge.build()
                    .name(prometheusPrefix + "_p75_ticks")
                    .help("75th percentile slippage in ticks for " + header)
                    .register(exporter.getRegistry());
            prometheusP90 = Gauge.build()
                    .name(prometheusPrefix + "_p90_ticks")
                    .help("90th percentile slippage in ticks for " + header)
                    .register(exporter.getRegistry());
            prometheusP95 = Gauge.build()
                    .name(prometheusPrefix + "_p95_ticks")
                    .help("95th percentile slippage in ticks for " + header)
                    .register(exporter.getRegistry());
            prometheusP99 = Gauge.build()
                    .name(prometheusPrefix + "_p99_ticks")
                    .help("99th percentile slippage in ticks for " + header)
                    .register(exporter.getRegistry());
        } catch (Exception e) {
            logger.warn("Could not register Prometheus metrics for SlippageStatistics '{}': {}", header, e.getMessage());
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
        if (!enable) return;
        if (slippages.size() > 0) {
            int counter = slippages.size();
            double mean = slippages.stream().mapToDouble(a -> a).average().orElse(0.0);
            double maxSlippage = slippages.stream().mapToDouble(a -> a).max().orElse(0);
            //get percentile 50 75 90 95 99
            double percentile50 = slippages.stream().sorted().skip((long) (slippages.size() * 0.5)).findFirst().orElse(0.0);
            double percentile75 = slippages.stream().sorted().skip((long) (slippages.size() * 0.75)).findFirst().orElse(0.0);
            double percentile90 = slippages.stream().sorted().skip((long) (slippages.size() * 0.9)).findFirst().orElse(0.0);
            double percentile95 = slippages.stream().sorted().skip((long) (slippages.size() * 0.95)).findFirst().orElse(0.0);
            double percentile99 = slippages.stream().sorted().skip((long) (slippages.size() * 0.99)).findFirst().orElse(0.0);

            logger.info("\tSlippage (ticks) :\tsize:{}\tmean:{}\t50pct:{}\t75pct:{}\t90pct:{}\t95pct:{}\t99pct:{}\tmax:{}", counter, mean,
                    percentile50, percentile75, percentile90, percentile95, percentile99, maxSlippage);

            // Push to Prometheus
            if (prometheusCount != null) prometheusCount.set(counter);
            if (prometheusMean != null) prometheusMean.set(mean);
            if (prometheusMax != null) prometheusMax.set(maxSlippage);
            if (prometheusP50 != null) prometheusP50.set(percentile50);
            if (prometheusP75 != null) prometheusP75.set(percentile75);
            if (prometheusP90 != null) prometheusP90.set(percentile90);
            if (prometheusP95 != null) prometheusP95.set(percentile95);
            if (prometheusP99 != null) prometheusP99.set(percentile99);

            if (RESET_STATISTICS_PER_UPDATE) {
                slippages.clear();
            }
        }
    }

}
