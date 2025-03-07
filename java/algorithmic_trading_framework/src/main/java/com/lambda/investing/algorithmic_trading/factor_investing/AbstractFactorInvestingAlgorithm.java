package com.lambda.investing.algorithmic_trading.factor_investing;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.factor_investing.executors.Executor;
import com.lambda.investing.data_manager.FileDataUtils;
import com.lambda.investing.factor_investing_connector.FactorListener;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
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

public abstract class AbstractFactorInvestingAlgorithm extends Algorithm implements FactorListener {

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
            executorsPerInstrument.put(instrumentPk, executor);
        }
        return executor;
    }

    private void createExecutorsPerInstrument() {
        executorsPerInstrument = new HashMap<>();
        for (Instrument instrument : getInstruments()) {
            Executor executor = createExecutor(instrument);
            executorsPerInstrument.put(instrument.getPrimaryKey(), executor);
        }
    }

    private void setInstruments() {
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
        boolean weAreReady = true;
        for (String instrumentPk : instrumentsPkToInstrument.keySet()) {
            double price = getPrice(instrumentPk);
            boolean instrumentIsReady = !Double.isNaN(price);
            weAreReady &= instrumentIsReady;
        }
        return weAreReady;
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
     *
     * Para calcular cuántos lotes debes comprar en el par de divisas EUR/JPY con una inversión de 2500 euros, asumiendo que estás operando con un lote estándar de 100,000 unidades, puedes utilizar la siguiente fórmula:
     *
     * Lotes a comprar = (Cantidad a invertir) / (Tamaño de un lote estándar x Precio actual del EUR/JPY)
     *
     * Donde:
     * Cantidad a invertir: 2500 euros
     * Precio actual del EUR/JPY: 147.535
     * Tamaño de un lote estándar: 100,000 unidades
     *
     * Sustituyendo los valores en la fórmula:
     *
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
        this.setInstruments();
    }

    @Override
    public boolean onDepthUpdate(Depth depth) {
        if (!instrumentsPkToInstrument.containsKey(depth.getInstrument())) {
            return false;
        }
        if (!depth.isDepthValid()) {
            return false;
        }
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
        }

        instrumentsPkToLastMarketDataSnapshot.put(depth.getInstrument(), lastMarketDataSnapshot);
        return super.onDepthUpdate(depth);
    }

    protected double getPriceIncreasePosition(String instrumentPk) {
        //override in other implementations if required
        return getPrice(instrumentPk);
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
            logger.warn("{} instruments are not ready -> return", instrumentPkWeights.size());
            return false;
        }

        requestUpdatePosition(true);

        boolean output = true;
        try {
            for (String instrumentPk : instrumentPkWeights.keySet()) {
                try {
                    Instrument instrument = Instrument.getInstrument(instrumentPk);
                    double weight = Math.round(instrumentPkWeights.get(instrumentPk) * 100.0) / 100.0;
                    if (Math.abs(weight) < 1E-6) {
                        logger.info("ignore {} with weight {}", instrumentPk, weight);
                        continue;
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
                        output &= getExecutor(instrumentPk).increasePosition(getCurrentTimestamp(), verb, quantityToExecute, price);
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
            String message = weightsUpdate.toString();
            logger.info(message);
            if (!isBacktest) {
                System.out.println(message);
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
