package com.lambda.investing.model.asset;

import com.lambda.investing.model.trading.Verb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentTest {
    Instrument instrument;
    double price = 1.06410;
    double quantity = 0.1;//lot

    @BeforeEach
    void setUp() {
        //generate instrument object based on eurusd_darwinex of type FX based on what is in darwinex_instruments.xml
//        <bean id="eurusd_darwinex" class="com.lambda.investing.model.asset.Instrument" init-method="addMap">
//        <property name="symbol" value="eurusd"></property>
//        <property name="market" value="darwinex"></property>
//        <property name="currency" value="USD"></property>
//        <property name="priceTick" value="0.00001"></property>
//        <property name="quantityTick" value="0.01"></property>
//
//        <property name="volumeFeePct" value="0.0025"></property>
//    </bean>

        instrument = new Instrument();
        instrument.setPrimaryKey("eurusd_darwinex");
        instrument.setSymbol("eurusd");
        instrument.setMarket("darwinex");
        instrument.setCurrency(Currency.USD);
        instrument.setPriceTick(0.00001);
        instrument.setQuantityTick(0.01);
        instrument.setVolumeFeePct(0.0025);//0.005% =>  0.0025% for buy and 0.0025% for sell

    }

    @Test
    void calculateFee() {
        assertEquals(0.0025, instrument.getVolumeFeePct());
        assertEquals((instrument.getVolumeFeePct() / 100) * quantity * instrument.getQuantityMultiplier(), instrument.calculateFee(true, price, quantity));
        assertEquals(0.0, instrument.getConstantFee());

    }

    @Test
    void calculateSpreadPriceAfterFee() {
        double spread = 1.06445 - 1.064425;
        double calculatedSpread = instrument.calculateSpreadPriceAfterFee(true, price, quantity);
        assertEquals(spread, calculatedSpread, 0.00001);

        double fee = instrument.calculateFee(true, price, quantity);
        double pnl = ((spread) * quantity);
        assertEquals(0.0, pnl - fee, 0.01);
    }

    @Test
    void calculatePriceAfterFee() {
        double initialPrice = 1.06410;
        assertEquals(1.06413, instrument.calculatePriceAfterFee(true, Verb.Buy, initialPrice, quantity));
        assertEquals(1.06408, instrument.calculatePriceAfterFee(true, Verb.Sell, initialPrice, quantity));
    }

    @Test
    void roundPrice() {
        assertEquals(1.064210, instrument.roundPrice(1.0642101));
        assertEquals(1.064210, instrument.roundPrice(1.0642095));
    }

    @Test
    void roundQty() {
        assertEquals(0.01, instrument.roundQty(0.009));
        assertEquals(0.01, instrument.roundQty(0.011));

    }


}