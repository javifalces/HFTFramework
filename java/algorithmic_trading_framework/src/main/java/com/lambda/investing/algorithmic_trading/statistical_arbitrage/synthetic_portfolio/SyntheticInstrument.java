package com.lambda.investing.algorithmic_trading.statistical_arbitrage.synthetic_portfolio;

import com.google.gson.reflect.TypeToken;
import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lambda.investing.model.Util.GSON;


@Getter public class SyntheticInstrument extends Instrument {

	protected Logger logger = LogManager.getLogger(SyntheticInstrument.class);
	private List<AssetSyntheticPortfolio> assetSyntheticPortfolioList;
	private Map<Instrument, Double> instrumentToBeta = new HashMap<>();
	private double mean, std, intercept;

	private String name = "SyntheticInstrument";

	public SyntheticInstrument(List<AssetSyntheticPortfolio> assetSyntheticPortfolioList, double mean, double std,
			double intercept) {
		this.assetSyntheticPortfolioList = assetSyntheticPortfolioList;
		this.mean = mean;
		this.std = std;
		this.intercept = intercept;
		setPortfolio();
	}

	public SyntheticInstrument(String jsonFilePath) throws FileNotFoundException {

		Reader reader = new FileReader(jsonFilePath);

		// Convert JSON to Java Object
		java.lang.reflect.Type empMapType = new TypeToken<Map<String, Double>>() {

		}.getType();
		Map<String, Double> map = GSON.fromJson(reader, empMapType);

		List<AssetSyntheticPortfolio> assetSyntheticPortfolioListRead = new ArrayList<>();
		for (Map.Entry<String, Double> entry : map.entrySet()) {
			Instrument instrument = Instrument.getInstrument(entry.getKey());
			if (instrument == null) {
				continue;
			}
			AssetSyntheticPortfolio assetSyntheticPortfolio = new AssetSyntheticPortfolio(instrument, entry.getValue());
			assetSyntheticPortfolioListRead.add(assetSyntheticPortfolio);
		}

		this.assetSyntheticPortfolioList = assetSyntheticPortfolioListRead;
		logger.info("SyntheticInstrument created with {} instruments", this.assetSyntheticPortfolioList.size());
		this.mean = map.get("mean");//real - synthetic
		this.std = map.get("std");
		this.intercept = map.get("intercept");
		setPortfolio();
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

	public boolean isSynthetic(Instrument instrument) {
		return instrumentToBeta.containsKey(instrument);
	}

	public double getBeta(Instrument instrument) {
		return instrumentToBeta.get(instrument);
	}

	@Override public String toString() {
		return this.name;
	}
}
