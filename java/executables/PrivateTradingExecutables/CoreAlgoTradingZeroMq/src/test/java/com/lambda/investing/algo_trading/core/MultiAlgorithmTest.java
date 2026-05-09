package com.lambda.investing.algo_trading.core;

import com.lambda.investing.algo_trading.AlgorithmConfiguration;
import com.lambda.investing.algo_trading.ZeroMqTradingConfiguration;
import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static com.lambda.investing.model.Util.fromJsonString;

public class MultiAlgorithmTest {

    private static class DummyAlgorithm extends Algorithm {
        private final String instrumentPk;
        int depthUpdates = 0;
        int tradeUpdates = 0;
        int erUpdates = 0;

        DummyAlgorithm(String algorithmInfo, String instrumentPk) {
            super(algorithmInfo, new HashMap<>());
            this.instrumentPk = instrumentPk;
            Instrument instrument = Instrument.getInstrument(instrumentPk);
            Assert.assertNotNull("Instrument should exist in test universe: " + instrumentPk, instrument);
            this.instruments.add(instrument);
        }

        @Override
        public void init() {
            // no-op for test
        }

        @Override
        public boolean onDepthUpdate(Depth depth) {
            if (instrumentPk.equals(depth.getInstrument())) {
                depthUpdates++;
                return true;
            }
            return false;
        }

        @Override
        public boolean onTradeUpdate(Trade trade) {
            if (instrumentPk.equals(trade.getInstrument())) {
                tradeUpdates++;
                return true;
            }
            return false;
        }

        @Override
        public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
            erUpdates++;
            return true;
        }

        @Override
        public String printAlgo() {
            return getAlgorithmInfo();
        }
    }

    @Test
    public void testBackwardCompatibleSingleAlgorithmConfiguration() {
        String json = "{\n" +
                "\"marketDataPort\":666,\n" +
                "\"tradeEnginePort\":677,\n" +
                "\"algorithm\":{\n" +
                "  \"algorithmName\":\"AvellanedaStoikov_test\",\n" +
                "  \"parameters\":{\"instrumentPks\":[\"btceur_kraken\"]}\n" +
                " }\n" +
                "}";

        ZeroMqTradingConfiguration configuration = fromJsonString(json, ZeroMqTradingConfiguration.class);
        Assert.assertEquals(1, configuration.getEffectiveAlgorithms().size());
        AlgorithmConfiguration algorithmConfiguration = configuration.getEffectiveAlgorithms().get(0).getEffectiveAlgorithm();
        Assert.assertNotNull(algorithmConfiguration);
        Assert.assertEquals("AvellanedaStoikov_test", algorithmConfiguration.getAlgorithmName());
    }

    @Test
    public void testMultipleAlgorithmsConfiguration() {
        String json = "{\n" +
                "\"algorithms\":[\n" +
                " {\"algorithmName\":\"AvellanedaStoikov_a\",\"parameters\":{\"instrumentPks\":[\"btceur_kraken\"]}},\n" +
                " {\"algorithm\":{\"algorithmName\":\"AvellanedaStoikov_b\",\"parameters\":{\"instrumentPks\":[\"etheur_kraken\"]}}}\n" +
                "]\n" +
                "}";

        ZeroMqTradingConfiguration configuration = fromJsonString(json, ZeroMqTradingConfiguration.class);
        Assert.assertEquals(2, configuration.getEffectiveAlgorithms().size());
        Assert.assertEquals("AvellanedaStoikov_a",
                configuration.getEffectiveAlgorithms().get(0).getEffectiveAlgorithm().getAlgorithmName());
        Assert.assertEquals("AvellanedaStoikov_b",
                configuration.getEffectiveAlgorithms().get(1).getEffectiveAlgorithm().getAlgorithmName());
    }

    @Test
    public void testMultiAlgorithmRoutesByInstrumentAndAlgorithmInfo() {
        DummyAlgorithm algoA = new DummyAlgorithm("AlgoA", "btceur_kraken");
        DummyAlgorithm algoB = new DummyAlgorithm("AlgoB", "etheur_kraken");

        MultiAlgorithm multiAlgorithm = new MultiAlgorithm(null, Arrays.asList(algoA, algoB));
        multiAlgorithm.init();

        Depth depthA = new Depth();
        depthA.setInstrument("btceur_kraken");
        multiAlgorithm.onDepthUpdate(depthA);

        Trade tradeB = new Trade();
        tradeB.setInstrument("etheur_kraken");
        multiAlgorithm.onTradeUpdate(tradeB);

        ExecutionReport erB = new ExecutionReport();
        erB.setAlgorithmInfo("AlgoB");
        multiAlgorithm.onExecutionReportUpdate(erB);

        Assert.assertEquals(1, algoA.depthUpdates);
        Assert.assertEquals(0, algoB.depthUpdates);
        Assert.assertEquals(0, algoA.tradeUpdates);
        Assert.assertEquals(1, algoB.tradeUpdates);
        Assert.assertEquals(0, algoA.erUpdates);
        Assert.assertEquals(1, algoB.erUpdates);
    }
}
