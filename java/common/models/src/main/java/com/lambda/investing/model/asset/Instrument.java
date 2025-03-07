package com.lambda.investing.model.asset;

import com.lambda.investing.model.Market;
import com.lambda.investing.model.exception.ModelException;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class Instrument {

    private static List<String> FX_MARKETS_LIST = Arrays.asList(new String[]{Market.Darwinex.name().toLowerCase(), Market.Idealpro.name().toLowerCase()});
    private static List<String> CRYPTO_MARKETS_LIST = Arrays.asList(new String[]{Market.Binance.name().toLowerCase(), Market.Kraken.name().toLowerCase(), Market.Paxos.name().toLowerCase()});
    private static Map<String, Instrument> INSTRUMENT_PK_TO_INSTRUMENT = new ConcurrentHashMap<>();
    private static final double DEFAULT_LEVERAGE_FX = 100;
    private static double DEFAULT_PRICE_TICK = 0.00001;
    private static double DEFAULT_QTY_TICK = 0.00001;

    private static double DEFAULT_CONSTANT_FEE = 0.0;// 0.0 €
    private static double DEFAULT_PCT_FEE = 0.0;// 0.0 €

    private static double DEFAULT_MAKER_FEE_PCT = 0.0;// 0.08%
    private static double DEFAULT_TAKER_FEE_PCT = 0.0;//0.18%
    //	private static double DEFAULT_PRICE_STEP = DEFAULT_PRICE_TICK;
    //	private static double DEFAULT_QTY_STEP = DEFAULT_QTY_TICK;

    private String isin, symbol, market, primaryKey;
    private Currency currency;
    protected static Logger logger = LogManager.getLogger(Instrument.class);

    //excluding serialization
    private double priceTick = DEFAULT_PRICE_TICK;
    private double quantityTick = DEFAULT_QTY_TICK;

    //// All fees are cumulative!
    private double pctFee = DEFAULT_PCT_FEE;
    private double volumeFeePct = DEFAULT_PCT_FEE;
    private double constantFee = DEFAULT_CONSTANT_FEE;
    private double makerFeePct = DEFAULT_MAKER_FEE_PCT;
    private double takerFeePct = DEFAULT_TAKER_FEE_PCT;

    //	private double priceStep = DEFAULT_PRICE_STEP;
    //	private double quantityStep = DEFAULT_QTY_STEP;

    //
    private double leverage = 1;
    private double quantityMultiplier = 1;

    public void addMap() {
        INSTRUMENT_PK_TO_INSTRUMENT.put(getPrimaryKey(), this);
        if (isFX()) {
            leverage = DEFAULT_LEVERAGE_FX;
            quantityMultiplier = 1E5;//1 lot = 100k
        }
    }

    public void setPriceStep(double priceStep) {
        priceTick = priceStep;
    }

    public double getPriceStep() {
        return getPriceTick();
    }

    public void setQuantityStep(double qtyStep) {
        quantityTick = qtyStep;
    }

    public double calculateFee(boolean isTaker, double price, double quantity) {
        double feePct = getPctFee(isTaker);

        quantity = quantity * getQuantityMultiplier();
        double variableFeePct = feePct * quantity * price;
        double variableFeeVolume = (volumeFeePct / 100) * quantity;

        double totalCost = Math.abs(variableFeePct) + Math.abs(variableFeeVolume) + Math.abs(getConstantFee());
        return totalCost;
    }

    public double getPctFee(boolean isTaker) {
        double feePct = getMakerFeePct() / 100.0;
        if (isTaker) {
            feePct = getTakerFeePct() / 100.0;
        }

        feePct += (getPctFee() / 100.0);
        return feePct;
    }

    public double calculateSpreadPriceAfterFee(boolean isTaker, double price, double quantity) {
        double feePctPrice = getPctFee(isTaker) * price * quantity * getQuantityMultiplier();
        double feePctVolume = (volumeFeePct / 100) * quantity * getQuantityMultiplier();
        double costCommission = feePctVolume + feePctPrice + getConstantFee();
        //pnl_cost = delta_price*(quantity*multiplier) => delta_price = pnl_cost/(quantity*multiplier)
        double rawSpread = costCommission / (quantity * getQuantityMultiplier());
        return rawSpread;//
    }

    public double calculatePriceAfterFee(boolean isTaker, Verb verb, double price, double quantity) {
        double priceWithFees = price;
        double spreadFees = calculateSpreadPriceAfterFee(isTaker, price, quantity);
        if (verb.equals(Verb.Buy)) {
            priceWithFees += spreadFees;
        }

        if (verb.equals(Verb.Sell)) {
            priceWithFees -= spreadFees;

        }
        return roundPrice(priceWithFees);
    }

    public double getQuantityStep() {
        return getQuantityTick();
    }

    public static Instrument getInstrument(String pk) {
        return INSTRUMENT_PK_TO_INSTRUMENT.get(pk);
    }

    public boolean isFX() {
        return FX_MARKETS_LIST.contains(getMarket().toLowerCase());
    }

    public boolean isCrypto() {
        return CRYPTO_MARKETS_LIST.contains(getMarket().toLowerCase());
    }

    public String getPrimaryKey() {
        if (this.primaryKey != null) {
            return this.primaryKey;
        } else {
            setPrimaryKey();
            if (this.primaryKey == null) {
                logger.error("can't get primary key {}", this);
                return null;
            }
            INSTRUMENT_PK_TO_INSTRUMENT.put(this.primaryKey, this);
            return this.primaryKey;
        }
    }

    /**
     * Is the method to identify the instrument for equals/Maps/name it ...etc
     *
     * @return
     */
    private void setPrimaryKey() {
        if (primaryKey == null) {
            String primaryKey = null;
            if (isin != null && market != null && symbol != null) {
                primaryKey = String.format("%s_%s_%s", symbol, market, isin);
            } else if (symbol != null && isin == null && market == null) {
                primaryKey = symbol;
            } else if (symbol != null && isin == null && market != null) {
                primaryKey = String.format("%s_%s", symbol, market);
            } else if (symbol == null && isin != null && market != null) {
                primaryKey = String.format("%s_%s", isin, market);
            }
            if (primaryKey == null) {
                logger.error("not enough data in instrument to get primary key -> return null");
            }
            this.primaryKey = primaryKey;
        }
    }

    public double getPriceIncrement(double priceReference, int tickIncrement) {
        double output = roundPrice(priceReference) + priceTick * tickIncrement;
        return roundPrice(output);
    }

    public int getNumberDecimalsPrice() {
        return (int) Math.round(Math.abs(Math.log10(1.0 / priceTick)));
    }

    public double roundPrice(double price) {
        //		return Math.round(price / getPriceTick()) * getPriceTick();
//		double scale = Math.pow(10, ((double) (getNumberDecimalsPrice())));
//		return Math.round(price * scale) / scale;

        int places = getNumberDecimalsPrice();
        return round(price, places);

    }

    public double roundQty(double qty) {
        return roundQty(qty, RoundingMode.HALF_UP);
    }

    public double roundQty(double qty, RoundingMode roundingMode) {
//		return Math.round(price / getQuantityTick()) * getQuantityTick();

        int places = getNumberDecimalsQty();
        return round(qty, places, roundingMode);
    }

    public static double round(double value, int places) {
        return round(value, places, RoundingMode.HALF_UP);
    }

    public static double round(double value, int places, RoundingMode roundingMode) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, roundingMode);
        return bd.doubleValue();
    }


    public int getNumberDecimalsQty() {
        return (int) Math.round(Math.abs(Math.log10(1.0 / quantityTick)));
    }

    @Override
    public String toString() {
        return getPrimaryKey();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Instrument) {
            Instrument otherInstrument = (Instrument) obj;
            return otherInstrument.getPrimaryKey().equals(getPrimaryKey());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getPrimaryKey().hashCode();
    }

}
