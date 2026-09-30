package com.lambda.investing.algorithmic_trading.observer.web;

import com.lambda.investing.algorithmic_trading.Algorithm;
import org.junit.Assert;
import org.junit.Test;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class WebFrontendRegistryTest {

    private static final String TEST_ALGORITHM_INFO = "TestFrontendAlgo_1";

    private static class TestFrontendProvider implements WebFrontendProvider {
        private final WebFrontend frontend = new ClasspathWebFrontend("test", "web/test_frontend");

        @Override
        public boolean supports(Algorithm algorithm) {
            return algorithm.getAlgorithmInfo().startsWith("TestFrontendAlgo");
        }

        @Override
        public WebFrontend getFrontend(Algorithm algorithm) {
            return frontend;
        }
    }

    private static class DummyAlgorithm extends Algorithm {
        DummyAlgorithm(String algorithmInfo) {
            super(algorithmInfo, new HashMap<>());
        }

        @Override
        public void init() {
        }

        @Override
        public boolean onDepthUpdate(Depth depth) {
            return true;
        }

        @Override
        public boolean onTradeUpdate(Trade trade) {
            return true;
        }

        @Override
        public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
            return true;
        }

        @Override
        public String printAlgo() {
            return getAlgorithmInfo();
        }
    }

    @Test
    public void defaultFrontendServesBundledDashboard() {
        WebFrontend frontend = new WebFrontendRegistry().getFrontend(null);
        Assert.assertSame(ClasspathWebFrontend.DEFAULT, frontend);
        Assert.assertFalse(frontend.getDashboardHtml().isEmpty());
        Assert.assertNotNull(frontend.getStaticAsset("css/base.css"));
    }

    @Test
    public void providerSelectsFrontendByAlgorithmType() {
        WebFrontendRegistry registry = new WebFrontendRegistry();
        registry.addProvider(new TestFrontendProvider());

        WebFrontend custom = registry.getFrontend(new DummyAlgorithm(TEST_ALGORITHM_INFO));
        Assert.assertEquals("test", custom.getName());
        Assert.assertTrue(custom.getDashboardHtml().contains("test frontend"));
        Assert.assertEquals(".custom{}", new String(custom.getStaticAsset("css/custom.css"), StandardCharsets.UTF_8).trim());
        Assert.assertNull(custom.getStaticAsset("css/base.css"));

        Assert.assertSame(ClasspathWebFrontend.DEFAULT, registry.getFrontend(new DummyAlgorithm("ConstantSpread_1")));
    }

    @Test
    public void failingProviderFallsBackToDefault() {
        WebFrontendRegistry registry = new WebFrontendRegistry();
        registry.addProvider(new WebFrontendProvider() {
            @Override
            public boolean supports(Algorithm algorithm) {
                throw new IllegalStateException("boom");
            }

            @Override
            public WebFrontend getFrontend(Algorithm algorithm) {
                return null;
            }
        });
        Assert.assertSame(ClasspathWebFrontend.DEFAULT, registry.getFrontend(new DummyAlgorithm(TEST_ALGORITHM_INFO)));
    }

    @Test(expected = IllegalStateException.class)
    public void missingDashboardThrows() {
        new ClasspathWebFrontend("missing", "web/does_not_exist");
    }

    @Test(expected = IllegalArgumentException.class)
    public void pathTraversalBasePathRejected() {
        new ClasspathWebFrontend("bad", "../web");
    }
}
