package com.lambda.investing.algorithmic_trading.hedging.synthetic_portfolio;

import com.google.gson.reflect.TypeToken;
import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lambda.investing.model.Util.GSON;

@Getter
public class SyntheticInstrument extends Instrument {

    protected Logger logger = LogManager.getLogger(SyntheticInstrument.class);
    private List<AssetSyntheticPortfolio> assetSyntheticPortfolioList;
    private Map<Instrument, Double> instrumentToBeta = new HashMap<>();
    private double mean, std, intercept;
    private double halfLifeSeconds = -1;
    private double halfLife = -1;
    private int secondsCandles = -1;

    //Min Period for the regression -> only as warning
    private int period = -1;
    private String regressionPriceType = null;

    private String name = "SyntheticInstrument";

    public SyntheticInstrument(List<AssetSyntheticPortfolio> assetSyntheticPortfolioList, double mean, double std,
                               double intercept, int halfLifeSeconds, int period, String regressionPriceType) {
        this.assetSyntheticPortfolioList = assetSyntheticPortfolioList;
        this.mean = mean;
        this.std = std;
        this.intercept = intercept;
        this.halfLifeSeconds = halfLifeSeconds;
        this.period = period;
        this.regressionPriceType = regressionPriceType;
        setPortfolio();
    }

    public SyntheticInstrument(String jsonFilePath) throws FileNotFoundException {

        Reader reader = new FileReader(jsonFilePath);
        printContent(jsonFilePath);
        // Convert JSON to Java Object
        java.lang.reflect.Type empMapType = new TypeToken<Map<String, Object>>() {

        }.getType();
        Map<String, Object> map = GSON.fromJson(reader, empMapType);

        List<AssetSyntheticPortfolio> assetSyntheticPortfolioListRead = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Instrument instrument = Instrument.getInstrument(entry.getKey());
            if (instrument == null) {
                continue;
            }
            AssetSyntheticPortfolio assetSyntheticPortfolio = new AssetSyntheticPortfolio(instrument, (Double) entry.getValue());
            assetSyntheticPortfolioListRead.add(assetSyntheticPortfolio);
        }

        this.assetSyntheticPortfolioList = assetSyntheticPortfolioListRead;
        logger.info("SyntheticInstrument created with {} instruments", this.assetSyntheticPortfolioList.size());
        this.mean = (Double) map.getOrDefault("mean", 0.0);//real - synthetic
        this.std = (Double) map.getOrDefault("std", 0.0);
        this.intercept = (Double) map.getOrDefault("intercept", 0.0);
        this.halfLifeSeconds = (Double) map.getOrDefault("half_life_seconds", -1.0);
        this.period = (int) Math.round((Double) map.getOrDefault("period", -1.0));
        this.secondsCandles = (int) Math.round((Double) map.getOrDefault("seconds_candles", -1.0));
        this.regressionPriceType = (String) map.get("regression_price_type");
        setPortfolio();
    }

    private void printContent(String jsonFilePath) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            System.out.println("--SyntheticInstrument---");
            System.out.println(content);
            System.out.println("-----");
            logger.info("SyntheticInstrument created with content: {}", content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setPortfolio() {
        Map<Instrument, Double> assetToBeta = new HashMap<>();
        double sumWeights = 0.0;
        for (AssetSyntheticPortfolio assetPortfolio : assetSyntheticPortfolioList) {
            sumWeights += assetPortfolio.getBeta();
            assetToBeta.put(assetPortfolio.getInstrument(), assetPortfolio.getBeta());
            name += assetPortfolio.getInstrument().getPrimaryKey() + "_synth;";
        }
        //		if(sumWeights!=1.0)
        //		{
        //			//readjust it
        //			for(AssetSyntheticPortfolio assetPortfolio:assetSyntheticPortfolioList){
        //				double newWeight=assetPortfolio.getBeta()/sumWeights;
        //				assetPortfolio.setBeta(newWeight);
        //				assetToBeta.put(assetPortfolio.getInstrument(),newWeight);
        //			}
        //		}
        this.instrumentToBeta = assetToBeta;
    }

    public List<Instrument> getInstruments() {
        return new ArrayList<>(instrumentToBeta.keySet());
    }

    public double getBeta(Instrument instrument) {
        return instrumentToBeta.get(instrument);
    }

    public double getBetaDefault(Instrument instrument) {
        return instrumentToBeta.getOrDefault(instrument, 1.0);
    }
    @Override
    public String toString() {
        return this.name;
    }
}
