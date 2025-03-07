package com.lambda.investing.algorithmic_trading.hedging.synthetic_portfolio;

import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetSyntheticPortfolio {

    private Instrument instrument;
    private double beta;

    public AssetSyntheticPortfolio(Instrument instrument, double beta) {
        this.instrument = instrument;
        this.beta = beta;
    }
}
