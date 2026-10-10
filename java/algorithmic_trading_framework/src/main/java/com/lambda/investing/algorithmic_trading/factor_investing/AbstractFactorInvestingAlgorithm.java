package com.lambda.investing.algorithmic_trading.factor_investing;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.factor_investing.executors.Executor;
import com.lambda.investing.data_manager.FileDataUtils;
import com.lambda.investing.factor_investing_connector.FactorListener;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractFactorInvestingAlgorithm extends Algorithm implements FactorListener {

    @Getter
    protected boolean backtestRequiredFactorParquet = true;

    @AllArgsConstructor
    @Getter
    protected class LastMarketDataSnapshot {
        private String instrumentPk;
        private double lastBid;
        private double lastAsk;

        private double lastBidQty;
        private double lastAskQty;

        public double getMid() {
            double output = (lastAsk + lastBid) / 2.0;
            return output;
        }


    }

    protected static Logger logger = LogManager.getLogger(AbstractFactorInvestingAlgorithm.class);
    protected String modelName;
    protected double capital;

    protected Map<String, Executor> executorsPerInstrument;

    protected Map<String, Instrument> instrumentsPkToInstrument;
    protected Set<Instrument> instruments;

    protected Map<String, LastMarketDataSnapshot> instrumentsPkToLastMarketDataSnapshot = new ConcurrentHashMap<>();

    /**
     * Minimum weight change (in percentage, e.g. 0.01 == 1%) required to trigger a rebalance
     * of an instrument on {@link #onWeightsUpdate(long, Map)}. Defaults to 0.0 (always update).
     */
    protected double weightChangeTolerance = 0.0;
    protected Map<String, Double> instrumentPkToLastWeight = new ConcurrentHashMap<>();

    /**
     * Interval (seconds) at which {@link #generateWeightsReport()} is scheduled in live trading;
     * 0/negative disables the wall-clock scheduler (the report is still generated once per
     * {@link #onWeightsUpdate(long, Map)} call, backtest included). Defaults to 60.
     */
    protected int reportIntervalSeconds;
    private volatile ScheduledExecutorService weightsReportScheduler;
    private final FactorInvestingWeightsReport weightsReport = new FactorInvestingWeightsReport();

    /**
     * Holds an {@link #onWeightsUpdate(long, Map)} call that arrived while {@link #weAreReady()} was
     * false (e.g. the warm-up factor computed from {@code JavaFactorPublisher.initializeCandleHistory()}
     * during {@code init()}, before any depth update has populated {@link #instrumentsPkToLastMarketDataSnapshot}).
     * Without this, that first set of weights was silently dropped and, depending on {@code secondsCandles},
     * positions could stay unrebalanced for hours until the next candle cycle. Retried from
     * {@link #onDepthUpdate(Depth)} once market data makes {@link #weAreReady()} true.
     */
    private static final class PendingWeightsUpdate {
        private final long timestamp;
        private final Map<String, Double> instrumentPkWeights;

        private PendingWeightsUpdate(long timestamp, Map<String, Double> instrumentPkWeights) {
            this.timestamp = timestamp;
            this.instrumentPkWeights = instrumentPkWeights;
        }
    }

    private final AtomicReference<PendingWeightsUpdate> pendingWeightsUpdate = new AtomicReference<>();

    /**
     * True once {@link #weAreReady()} has been observed true but the retry hasn't fired yet. Needed
     * because {@link com.lambda.investing.algorithmic_trading.factor_investing.executors.AbstractExecutor}
     * registers itself as its own, independent {@code MarketDataListener} with the market data provider
     * (after the algorithm), so on the very depth tick that makes an instrument's
     * {@link #instrumentsPkWithDepthReceived} entry (and hence {@link #weAreReady()}) become true for the
     * first time, that same instrument's executor has not processed this depth yet and its
     * {@code lastDepth} is still null. Arming here and only retrying on a later tick lets this depth
     * finish propagating to every listener (executors included) first.
     */
    private final AtomicBoolean retryArmed = new AtomicBoolean(false);

    /**
     * True once an {@link #onWeightsUpdate(long, Map)} call has been fully processed (i.e. past the
     * {@link #weAreReady()} gate) at least once. Used to avoid sending a push notification for the very
     * first ("initial") calibration, where every instrument is necessarily a new position and the
     * notification would be noisy/redundant; only subsequent recalibrations that actually send new
     * orders are notified.
     */
    private final AtomicBoolean initialCalibrationDone = new AtomicBoolean(false);

    /**
     * Instruments that have received at least one (valid) {@link #onDepthUpdate(Depth)} call.
     * Backs {@link #weAreReady()}, which now requires every instrument to have spoken at least
     * once, and in turn gates {@link #retryPendingWeightsUpdateIfReady()}.
     */
    private final Set<String> instrumentsPkWithDepthReceived = ConcurrentHashMap.newKeySet();

    /**
     * Wall-clock epoch millis of the last {@link #onWeightsUpdate(long, Map)} call that was actually
     * processed (i.e. past the {@link #weAreReady()} gate), which in turn is the last time
     * {@code AbstractFactorProvider#notifyFactor} was delivered and consumed. 0 until the first one.
     * Published as a stable "lastWeightsUpdateTimestamp" custom column (see {@link #generateWeightsReport()})
     * so the live dashboard can show the real last-processed time instead of inferring it from the
     * arrival of the periodically re-published "weight" column (which also fires on every scheduled
     * {@link #generateWeightsReport()} refresh, not only on actual weight updates).
     */
    private volatile long lastWeightsUpdateTimestamp = 0L;

    public AbstractFactorInvestingAlgorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo, Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);
        setParameters(parameters);
    }


    public Set<Instrument> getInstruments() {
        if (instruments.size() == 0) {
            setParameters(parameters);
        }
        return instruments;
    }

    public String getModelName() {
        return modelName;
    }

    protected abstract Executor createExecutor(Instrument instrument);

    @Override
    public void init() {
        super.init();

        if (this.algorithmConnectorConfiguration.getFactorProvider() == null) {
            logger.error("we need a FactorProvider in algorithmConnectorConfiguration for AbstractFactorInvestingAlgorithm");
            System.err.println("we need a FactorProvider in algorithmConnectorConfiguration for AbstractFactorInvestingAlgorithm\"");
            System.exit(-1);
        } else {
            this.algorithmConnectorConfiguration.getFactorProvider().register(this);
        }
        createExecutorsPerInstrument();


    }


    protected Executor getExecutor(String instrumentPk) {
        Executor executor = executorsPerInstrument.get(instrumentPk);
        if (executor == null) {
            Instrument instrument = Instrument.getInstrument(instrumentPk);
            logger.warn("executor not created for instrument {}", instrumentPk);
            executor = createExecutor(instrument);
            executor.setAlgorithm(this);
            executorsPerInstrument.put(instrumentPk, executor);
        }
        return executor;
    }

    private void createExecutorsPerInstrument() {
        executorsPerInstrument = new HashMap<>();
        for (Instrument instrument : getInstruments()) {
            Executor executor = createExecutor(instrument);
            executor.setAlgorithm(this);
            executorsPerInstrument.put(instrument.getPrimaryKey(), executor);
        }
    }

    /**
     * On top of the base per-day reset, also resets all {@link #executorsPerInstrument}
     * (e.g. clears their captured execution outcomes used for aggregated custom columns).
     */
    @Override
    public void resetAlgorithm() {
        super.resetAlgorithm();
        if (executorsPerInstrument != null) {
            for (Executor executor : executorsPerInstrument.values()) {
                executor.reset();
            }
        }
    }

    protected void setInstruments() {
        List<Instrument> instruments = AbstractFactorInvestingAlgorithm.getInstrumentsModel(modelName);
        if (instruments == null) {
            return;
        }
        this.instruments = new HashSet<>(instruments);
        setInstrumentsPkToInstrument(instruments);
    }


    public void setInstrumentsPkToInstrument(List<Instrument> instrumentsPkToInstrument) {
        this.instrumentsPkToInstrument = new HashMap<>();
        for (Instrument instrument : instrumentsPkToInstrument) {
            this.instrumentsPkToInstrument.put(instrument.getPrimaryKey(), instrument);
        }
    }

    protected static List<Instrument> getInstrumentsModel(String modelName) {
        //read modelname universe.csv and get instruments list strings

        String modelPath = Configuration.formatLog("{}\\factor_investing\\", Configuration.DATA_PATH) + modelName;
        String universePath = Configuration.formatLog("{}\\universe.csv", modelPath);
        File universeFile = new File(universePath);
        if (!universeFile.exists()) {
            logger.error("universe file not found for model {} in {}", modelName, universePath);
            return null;
        }
        Table universeTable = null;
        try {
            universeTable = FileDataUtils.readCSVRaw(universeFile.getPath());
        } catch (IOException e) {
            logger.error("universe file error reading file for model {} in {}", modelName, universePath);
            return null;
        }
        List<String> instrumentsStringList = universeTable.columnNames();
        if (instrumentsStringList.contains("date")) {
            instrumentsStringList.remove("date");
        }
        String instrumentsString = String.join(",", instrumentsStringList);
        logger.info("Detected {} instruments: {}", instrumentsStringList.size(), instrumentsString);

        //transform list pks to instruments
        List<Instrument> instruments = new ArrayList<>();
        for (String instrumentPk : instrumentsStringList) {
            if (instrumentPk.equalsIgnoreCase("C0") || instrumentPk.equalsIgnoreCase("datetime")) {
                continue;
            }

            Instrument instrument = Instrument.getInstrument(instrumentPk);
            if (instrument == null) {
                logger.error("{} instrumentPk not found to add it", instrumentPk);
                System.err.println(Configuration.formatLog("ERROR adding instrument {} of model {}", instrumentPk, modelName));
                continue;
            }
            instruments.add(instrument);
        }
        return instruments;
    }

    protected double getInstrumentCapital(double weight) {
        return (capital * weight);
    }

    protected boolean weAreReady() {
        return instrumentsPkWithDepthReceived.containsAll(instrumentsPkToInstrument.keySet());
    }

    protected double getPrice(String instrumentPk) {
        LastMarketDataSnapshot lastMarketDataSnapshot = instrumentsPkToLastMarketDataSnapshot.get(instrumentPk);
        if (lastMarketDataSnapshot == null) {
            return Double.NaN;
        }
        return lastMarketDataSnapshot.getMid();
    }

    /**
     * Cantidad a invertir: 55,000 euros
     * Precio actual del EUR/USD: 1.0955 dólares por euro
     * Tamaño de un lote estándar: 100,000 unidades
     * <p>
     * Lotes a comprar = (Cantidad a invertir) / (Tamaño de un lote estándar x Precio actual del EUR/USD)
     * <p>
     * Lotes a comprar = 55,000 euros / (100,000 unidades/lote x 1.0955 dólares/euro)
     * Lotes a comprar = 55,000 euros / 109,550 dólares
     * Lotes a comprar = 0.5015 lotes (redondeado)
     * <p>
     * Para calcular cuántos lotes debes comprar en el par de divisas EUR/JPY con una inversión de 2500 euros, asumiendo que estás operando con un lote estándar de 100,000 unidades, puedes utilizar la siguiente fórmula:
     * <p>
     * Lotes a comprar = (Cantidad a invertir) / (Tamaño de un lote estándar x Precio actual del EUR/JPY)
     * <p>
     * Donde:
     * Cantidad a invertir: 2500 euros
     * Precio actual del EUR/JPY: 147.535
     * Tamaño de un lote estándar: 100,000 unidades
     * <p>
     * Sustituyendo los valores en la fórmula:
     * <p>
     * Lotes a comprar = 2500 euros / (100,000 unidades/lote x 147.535)
     * Lotes a comprar = 2500 euros / 14,753,500 unidades
     * Lotes a comprar = 0.000169 lotes (redondeado)
     *
     * @param weight
     * @param instrument
     * @return
     */
    protected double getQuantity(double weight, Instrument instrument) {
        String instrumentPk = instrument.getPrimaryKey();
        double instrumentCapital = getInstrumentCapital(weight);//*instrument.getLeverage();

        double price = getPrice(instrumentPk);
        if (Double.isNaN(price)) {
            String messageNoPrice = Configuration.formatLog("last price of {} is nan {} -> return weight=0", instrumentPk, price);
            System.err.println(messageNoPrice);
            logger.error(messageNoPrice);
            return 0.0;
        }

        double contracts = Math.abs(instrumentCapital / (price * instrument.getQuantityMultiplier()));
        double output = contracts;


        return Math.signum(weight) * output;
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        super.setParameters(parameters);
        this.modelName = getParameterString(parameters, "modelName");
        this.capital = getParameterDouble(parameters, "capital");
        this.reportIntervalSeconds = getParameterIntOrDefault(parameters, "reportIntervalSeconds", 60);
        this.setInstruments();
    }

    @Override
    public void start() {
        super.start();
        if (!isBacktest && !isPaper && isReady()) {
            startWeightsReportScheduler();
        }
    }

    @Override
    public void stop() {
        super.stop();
        stopWeightsReportScheduler();
    }

    private void startWeightsReportScheduler() {
        if (reportIntervalSeconds > 0) {
            if (weightsReportScheduler == null || weightsReportScheduler.isShutdown()) {
                weightsReportScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "FactorInvestingWeightsReport-Scheduler");
                    t.setDaemon(true);
                    t.setPriority(Thread.MIN_PRIORITY);
                    return t;
                });
                weightsReportScheduler.scheduleAtFixedRate(() -> {
                    try {
                        generateWeightsReport();
                    } catch (Exception e) {
                        logger.error("Error generating factor investing weights report", e);
                    }
                }, reportIntervalSeconds, reportIntervalSeconds, TimeUnit.SECONDS);
                logger.info("Weights report scheduler started with interval {} seconds", reportIntervalSeconds);
            }
        }
    }

    private void stopWeightsReportScheduler() {
        ScheduledExecutorService scheduler = this.weightsReportScheduler;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException ie) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Snapshots, per instrument, the last requested weight, expected position (from that weight),
     * current position and its mark-to-market investment, plus the model's total investment; then
     * publishes each as an {@link #addCurrentCustomColumn(String, String, Double)} (for live
     * GUI/dashboard/Prometheus consumption) and appends a row per instrument to the
     * {@link FactorInvestingWeightsReport} CSV file, so instrument-level allocation can be followed
     * and audited over time.
     */
    protected void generateWeightsReport() {
        if (instrumentsPkToInstrument == null || instrumentsPkToInstrument.isEmpty()) {
            return;
        }

        Map<String, FactorInvestingWeightsReport.InstrumentReportRow> rowsPerInstrument = new LinkedHashMap<>();
        double totalInvestment = 0.0;
        for (Instrument instrument : instrumentsPkToInstrument.values()) {
            String instrumentPk = instrument.getPrimaryKey();
            double weight = instrumentPkToLastWeight.getOrDefault(instrumentPk, 0.0);
            double currentPosition = getPosition(instrument);
            double price = getPrice(instrumentPk);
            double expectedPosition = Double.isNaN(price) ? 0.0 : getQuantity(weight, instrument);
            double investment = Double.isNaN(price) ? 0.0 : currentPosition * price * instrument.getQuantityMultiplier();
            totalInvestment += investment;
            rowsPerInstrument.put(instrumentPk,
                    new FactorInvestingWeightsReport.InstrumentReportRow(weight, expectedPosition, currentPosition, investment));
        }

        long timestamp = getCurrentTimestamp();
        for (Map.Entry<String, FactorInvestingWeightsReport.InstrumentReportRow> entry : rowsPerInstrument.entrySet()) {
            String instrumentPk = entry.getKey();
            FactorInvestingWeightsReport.InstrumentReportRow row = entry.getValue();
            addCurrentCustomColumn(instrumentPk, "weight", row.weight);
            addCurrentCustomColumn(instrumentPk, "expectedPosition", row.expectedPosition);
            addCurrentCustomColumn(instrumentPk, "currentPosition", row.currentPosition);
            addCurrentCustomColumn(instrumentPk, "investment", row.investment);
            addCurrentCustomColumn(instrumentPk, "totalInvestment", totalInvestment);
            if (lastWeightsUpdateTimestamp > 0) {
                //value (not arrival time) is the signal: only moves forward when onWeightsUpdate actually
                //processes a new notifyFactor call, so re-publishing it on every scheduled refresh below
                //doesn't make the dashboard think a new weights update just happened.
                addCurrentCustomColumn(instrumentPk, "lastWeightsUpdateTimestamp", (double) lastWeightsUpdateTimestamp);
            }
        }

        weightsReport.report(timestamp, modelName, capital, totalInvestment, rowsPerInstrument);
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        if (!instrumentsPkToInstrument.containsKey(depth.getInstrument())) {
            return false;
        }
        if (!depth.isDepthValid()) {
            return false;
        }
        instrumentsPkWithDepthReceived.add(depth.getInstrument());
        //this depth (and everything derived from it: executors, candle updaters, InstrumentAlgorithmManager) can
        //outlive this call (e.g. cached as lastDepth and reused later on onWeightsUpdate/candle close).
        //Take a pool-independent snapshot so a concurrent AbstractMarketDataConnectorPublisher.deleteFromPool
        //returning the original Depth to the pool (and it being reset/reused for another update) can't turn
        //those cached fields null downstream. No pool cleanup is needed for this copy: it is never checked
        //out of Depth's pool, so there is nothing to return/cleanFromPool.
        depth = Depth.copyFromWithoutPool(depth);
        //get best Bid
        Double bestBid = Double.NaN;
        Double bestBidQty = Double.NaN;
        if (depth.getBidLevels() > 0) {
            bestBid = depth.getBestBid();
            bestBidQty = depth.getBestBidQty();
        }
        Double bestAsk = Double.NaN;
        Double bestAskQty = Double.NaN;
        if (depth.getAskLevels() > 0) {
            bestAsk = depth.getBestAsk();
            bestAskQty = depth.getBestAskQty();
        }

        //update side that changed! and is valid
        LastMarketDataSnapshot lastMarketDataSnapshot = instrumentsPkToLastMarketDataSnapshot.get(depth.getInstrument());
        if (lastMarketDataSnapshot != null) {
            //change side
            if (!Double.isNaN(bestBid)) {
                lastMarketDataSnapshot.lastBid = bestBid;
                lastMarketDataSnapshot.lastBidQty = bestBidQty;
            }
            if (!Double.isNaN(bestAsk)) {
                lastMarketDataSnapshot.lastAsk = bestAsk;
                lastMarketDataSnapshot.lastAskQty = bestAskQty;
            }

        } else {
            if (!Double.isNaN(bestBid) && !Double.isNaN(bestAsk)) {
                lastMarketDataSnapshot = new LastMarketDataSnapshot(depth.getInstrument(), bestBid, bestAsk, bestBidQty, bestAskQty);
            }
            //maybe we dont have both sides
        }

        if (lastMarketDataSnapshot != null) {
            instrumentsPkToLastMarketDataSnapshot.put(depth.getInstrument(), lastMarketDataSnapshot);
        }

        retryPendingWeightsUpdateIfReady();

        return super.onDepthUpdate(depth);
    }

    /**
     * Cheap on the hot depth path: a single volatile read when there is nothing pending, only
     * paying the {@link #weAreReady()} scan (now backed by {@link #instrumentsPkWithDepthReceived})
     * once a deferred update actually exists.
     * <p>
     * Deliberately waits one extra armed tick (see {@link #retryArmed}) before actually calling
     * {@link #onWeightsUpdate(long, Map)}: on the exact depth tick that first makes
     * {@link #weAreReady()} true, that instrument's {@code Executor} (a separately registered market
     * data listener, notified after the algorithm) may not have processed this depth yet, leaving its
     * {@code lastDepth} null and crashing {@code increasePosition}.
     */
    private void retryPendingWeightsUpdateIfReady() {
        PendingWeightsUpdate pending = pendingWeightsUpdate.get();
        if (pending == null || !weAreReady()) {
            return;
        }
        if (retryArmed.compareAndSet(false, true)) {
            return;
        }
        if (pendingWeightsUpdate.compareAndSet(pending, null)) {
            retryArmed.set(false);
            logger.info("{} market data ready -> retrying deferred onWeightsUpdate from {}", getCurrentTime(), new Date(pending.timestamp));
            onWeightsUpdate(pending.timestamp, pending.instrumentPkWeights);
        }
    }

    protected double getPriceIncreasePosition(String instrumentPk) {
        //override in other implementations if required
        return getPrice(instrumentPk);
    }

    @Override
    protected void printRowTrade(ExecutionReport executionReport) {
        //override in other implementations if required
    }

    public boolean onWeightsUpdate(long timestamp, Map<String, Double> instrumentPkWeights) {
        try {
            if (timestamp != 0 && isBacktest) {
                timeService.setCurrentTimestamp(timestamp);
            }
            if (!isBacktest) {
                timeService.setCurrentTimestamp(new Date().getTime());
            }
            //check depth
        } catch (Exception e) {
            logger.warn("error capture onWeightsUpdate on algorithm {} ", this.algorithmInfo, e);
        }

        logger.info("{} received onWeightsUpdate {} instruments", getCurrentTime(), instrumentPkWeights.size());

        StringBuilder weightsUpdate = new StringBuilder();
        weightsUpdate.append(Configuration.formatLog("{} onWeightsUpdate {}(capital:{}€)\n", this.getCurrentTime(), modelName, capital));

        if (!weAreReady()) {
            logger.warn("{} instruments are not ready -> caching weights from {} to retry once market data is ready",
                    instrumentPkWeights.size(), getCurrentTime());
            pendingWeightsUpdate.set(new PendingWeightsUpdate(timestamp, instrumentPkWeights));
            return false;
        }
        pendingWeightsUpdate.set(null);
        lastWeightsUpdateTimestamp = System.currentTimeMillis();

        //the very first onWeightsUpdate that passes weAreReady() is the initial calibration: every
        //instrument is a brand-new position, so we don't push-notify it; only later recalibrations are.
        boolean isInitialCalibration = initialCalibrationDone.compareAndSet(false, true);

        requestUpdatePosition(true);

        boolean output = true;
        double sumPositiveWeights = 0.0;
        double sumNegativeWeights = 0.0;
        StringBuilder recalibrationOrders = new StringBuilder();
        try {
            for (String instrumentPk : instrumentPkWeights.keySet().stream().sorted().toList()) {
                try {
                    Instrument instrument = Instrument.getInstrument(instrumentPk);
                    double weight = Math.round(instrumentPkWeights.get(instrumentPk) * 100.0) / 100.0;

                    Double lastWeight = instrumentPkToLastWeight.get(instrumentPk);
                    if (lastWeight != null && Math.abs(weight - lastWeight) < weightChangeTolerance) {
                        logger.info("not updating {} : weight change {} -> {} is below tolerance {} -> skip it",
                                instrumentPk, lastWeight, weight, weightChangeTolerance);
                        if (!isBacktest) {
                            String message = Configuration.formatLog("WARNING [capital:{}] not updating {} : weight change {} -> {} is below tolerance {} -> skip it",
                                    capital, instrumentPk, lastWeight, weight, weightChangeTolerance);
                            System.out.println(message);
                        }
                        continue;
                    }
                    instrumentPkToLastWeight.put(instrumentPk, weight);
                    if (weight > 0) {
                        sumPositiveWeights += weight;
                    } else {
                        sumNegativeWeights += weight;
                    }

                    double expectedPosition = Math.round(getQuantity(weight, instrument) * 1E6) / 1E6;
                    double currentPosition = getPosition(instrument);
                    double quantityWithSide = expectedPosition - currentPosition;
                    quantityWithSide = instrument.roundQty(Math.abs(quantityWithSide), RoundingMode.HALF_DOWN) * Math.signum(quantityWithSide);//always less

                    double price = instrument.roundPrice(getPriceIncreasePosition(instrumentPk));
                    price = instrument.roundPrice(price);

                    double expectedPositionRounded = instrument.roundQty(Math.abs(expectedPosition), RoundingMode.HALF_DOWN) * Math.signum(expectedPosition);
                    weightsUpdate.append(Configuration.formatLog("\t- {}:{}({} eur) -> {}[currentPosition:{} quantityWithSide:{}]\n", instrumentPk, weight, getInstrumentCapital(weight), expectedPositionRounded, currentPosition, quantityWithSide));
                    if (!instruments.contains(instrument)) {
                        logger.warn("received weight instrument {} not in instruments of the model {} -> skip it", instrument.getPrimaryKey(), modelName);
                        System.err.println("received weight instrument " + instrumentPk + " not in instruments of the model -> skip it");
                        continue;
                    }
                    Verb verb = quantityWithSide > 0 ? Verb.Buy : Verb.Sell;
                    double quantityToExecute = Math.abs(quantityWithSide);


                    if (quantityToExecute <= 1E-9) {
                        logger.info("skip increase position {} because quantity = {}", instrumentPk, quantityToExecute);
                        if (!isBacktest) {
                            String message = Configuration.formatLog("WARNING [capital:{}] skip increase position {} because quantity = {}", capital, instrumentPk, quantityToExecute);
                            System.out.println(message);
                        }
                        continue;
                    }

                    if (quantityToExecute < instrument.getQuantityTick()) {
                        logger.info("skip increase position {} because quantity:{} < quantityTick:{}", instrumentPk, quantityToExecute, instrument.getQuantityTick());
                        if (!isBacktest) {
                            String message = Configuration.formatLog("WARNING [capital:{}]skip increase position {} because quantity:{} < quantityTick:{}", capital, instrumentPk, quantityToExecute, instrument.getQuantityTick());
                            System.out.println(message);
                        }
                        continue;
                    }


                    try {
                        logger.info("{} weight: {} - {} -> {} {}€ (quantity:{} price:{} position:{} expected_position:{}) ", instrumentPk, lastWeight != null ? lastWeight : 0, weight, verb, quantityToExecute * price, quantityToExecute, price, currentPosition, expectedPosition);
                        boolean orderSent = getExecutor(instrumentPk).increasePosition(getCurrentTimestamp(), verb, quantityToExecute, price);
                        output &= orderSent;
                        if (orderSent && !isInitialCalibration) {
                            recalibrationOrders.append(Configuration.formatLog("\t- {}:{} {} quantity:{}@{}(position:{}->{})\n",
                                    instrumentPk, weight, verb, quantityToExecute, price, currentPosition, expectedPositionRounded));
                        }
                    } catch (Exception e) {
                        String message = Configuration.formatLog("Error executing {} verb:{} quantity:{} price:{} {}", instrumentPk, verb, quantityToExecute, price, e.getMessage());
                        logger.error(message, e);
                        System.err.println(message);
                        output = false;
                    }
                } catch (Exception e) {
                    logger.error("Error rebalancing instrument {} ", instrumentPk, e);
                }
            }
            weightsUpdate.append(Configuration
                    .formatLog("Sum positive weights: {}, negative weights: {}, total weights: {}\n",
                            sumPositiveWeights, sumNegativeWeights, sumPositiveWeights + sumNegativeWeights));

            String message = weightsUpdate.toString();
            logger.info(message);
            if (!isBacktest) {
                System.out.println(message);
            }

            if (!isInitialCalibration && !isBacktest && recalibrationOrders.length() > 0) {
                try {
                    String pushTitle = Configuration.formatLog("{} recalibration orders", modelName);
                    String pushBody = recalibrationOrders.toString();
                    sendPushNotificationMessage(pushTitle, pushBody);
                    logger.info("Push recalibration notification sent: {} {}", pushTitle, pushBody);
                } catch (Exception e) {
                    logger.error("Error sending push recalibration notification: {}", e.getMessage());
                }
            }

            try {
                generateWeightsReport();
            } catch (Exception e) {
                logger.error("Error generating factor investing weights report on onWeightsUpdate", e);
            }

            return output;
        } catch (Exception e) {
            logger.error("Error onWeightsUpdate ", e);
            return false;
        }
    }

    @Override
    public String printAlgo() {
        return String
                .format("%s  \n\tmodelName=%s\n\tcapital=%.3f\n\tfirstHourOperatingIncluded=%d\n\tlastHourOperatingIncluded=%d",
                        algorithmInfo, modelName, capital, firstHourOperatingIncluded,
                        lastHourOperatingIncluded);
    }


}
