package com.lambda.investing.backtest;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.provider.TradingAlgorithmsProvider;
import com.lambda.investing.backtest_engine.BacktestConfiguration;
import com.lambda.investing.model.asset.Instrument;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;

import static com.lambda.investing.model.Util.fromJsonString;

/**
 * Tests that instruments can be loaded from algorithm parameters when not set in the backtest section.
 */
@RunWith(MockitoJUnitRunner.class)
public class InstrumentFromParameterTest {

    @BeforeClass
    public static void setUpClass() {
        // Register trading algorithms so AlgorithmCreationUtils can find them by name
        TradingAlgorithmsProvider provider = new TradingAlgorithmsProvider();
        provider.init();
        // Register test instruments used in the test JSONs
        UtilsTest.AddTestInstruments();
    }

    /**
     * Validates that when 'instrument' is set in the backtest section, the configuration is loaded correctly.
     */
    @Test
    public void testInstrumentFromBacktestSection() throws Exception {
        InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream("test_LookForward.json");
        String json = new String(inputStream.readAllBytes());
        InputConfiguration inputConfiguration = fromJsonString(json, InputConfiguration.class);

        Assert.assertNotNull("backtest instrument should be set", inputConfiguration.getBacktest().getInstrument());
        Assert.assertEquals("btceur_kraken", inputConfiguration.getBacktest().getInstrument());

        BacktestConfiguration backtestConfiguration = inputConfiguration.getBacktestConfiguration();
        Assert.assertNotNull(backtestConfiguration);
        Assert.assertFalse("instrument list should not be empty", backtestConfiguration.getInstruments().isEmpty());

        Instrument firstInstrument = backtestConfiguration.getInstruments().get(0);
        Assert.assertEquals("btceur_kraken", firstInstrument.getPrimaryKey());
    }

    /**
     * Validates that when 'instrument' is NOT in the backtest section but 'instrumentPks' (array) IS in
     * algorithm parameters, the instrument is loaded from algorithm parameters.
     */
    @Test
    public void testInstrumentFromAlgorithmParameters() throws Exception {
        InputStream inputStream = Configuration.class.getClassLoader()
                .getResourceAsStream("test_LookForward_no_instrument_in_backtest.json");
        String json = new String(inputStream.readAllBytes());
        InputConfiguration inputConfiguration = fromJsonString(json, InputConfiguration.class);

        Assert.assertNull("backtest instrument should be null", inputConfiguration.getBacktest().getInstrument());

        BacktestConfiguration backtestConfiguration = inputConfiguration.getBacktestConfiguration();
        Assert.assertNotNull(backtestConfiguration);
        Assert.assertFalse("instrument list should not be empty", backtestConfiguration.getInstruments().isEmpty());

        Instrument firstInstrument = backtestConfiguration.getInstruments().get(0);
        Assert.assertEquals("btceur_kraken", firstInstrument.getPrimaryKey());
    }

    /**
     * Validates that the legacy 'instrument' parameter (comma-separated string) still works as a fallback.
     */
    @Test
    public void testInstrumentFallbackFromLegacyParameter() throws Exception {
        String json = "{\n" +
                "  \"backtest\": {\n" +
                "    \"startDate\": \"20250708 08:00:00\",\n" +
                "    \"endDate\": \"20250708 15:00:00\",\n" +
                "    \"delayOrderMs\": 0,\n" +
                "    \"multithreadConfiguration\": \"single_thread\"\n" +
                "  },\n" +
                "  \"algorithm\": {\n" +
                "    \"algorithmName\": \"LookForwardBiasAlgorithm_junit\",\n" +
                "    \"parameters\": {\n" +
                "      \"instrument\": \"btceur_kraken\",\n" +
                "      \"quantity\": 0.01,\n" +
                "      \"secondsCandles\": 60\n" +
                "    }\n" +
                "  }\n" +
                "}";
        InputConfiguration inputConfiguration = fromJsonString(json, InputConfiguration.class);

        Assert.assertNull("backtest instrument should be null", inputConfiguration.getBacktest().getInstrument());

        BacktestConfiguration backtestConfiguration = inputConfiguration.getBacktestConfiguration();
        Assert.assertNotNull(backtestConfiguration);
        Assert.assertFalse("instrument list should not be empty", backtestConfiguration.getInstruments().isEmpty());

        Instrument firstInstrument = backtestConfiguration.getInstruments().get(0);
        Assert.assertEquals("btceur_kraken", firstInstrument.getPrimaryKey());
    }

    /**
     * Validates that when 'instrument' is set in neither the backtest section nor algorithm parameters,
     * an exception is thrown.
     */
    @Test(expected = Exception.class)
    public void testMissingInstrumentThrowsException() throws Exception {
        String json = "{\n" +
                "  \"backtest\": {\n" +
                "    \"startDate\": \"20250708 08:00:00\",\n" +
                "    \"endDate\": \"20250708 15:00:00\",\n" +
                "    \"delayOrderMs\": 0,\n" +
                "    \"multithreadConfiguration\": \"single_thread\"\n" +
                "  },\n" +
                "  \"algorithm\": {\n" +
                "    \"algorithmName\": \"LookForwardBiasAlgorithm_junit\",\n" +
                "    \"parameters\": {\n" +
                "      \"quantity\": 0.01,\n" +
                "      \"secondsCandles\": 60\n" +
                "    }\n" +
                "  }\n" +
                "}";
        InputConfiguration inputConfiguration = fromJsonString(json, InputConfiguration.class);
        // Should throw because neither backtest.instrument nor algorithm.parameters.instrument is set
        inputConfiguration.getBacktestConfiguration();
    }
}
