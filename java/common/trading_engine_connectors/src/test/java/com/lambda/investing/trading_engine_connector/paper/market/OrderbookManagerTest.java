package com.lambda.investing.trading_engine_connector.paper.market;//package com.lambda.investing.trading_engine_connector.paper.market;
//
//import com.lambda.investing.Configuration;
//import com.lambda.investing.model.asset.Instrument;
//import com.lambda.investing.model.market_data.Depth;
//import com.lambda.investing.model.market_data.Trade;
//import com.lambda.investing.model.trading.ExecutionReport;
//import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;
//import org.junit.Before;
//import org.junit.Test;
//
//import org.mockito.Mockito;
//import org.mockito.invocation.InvocationOnMock;
//import org.mockito.stubbing.Answer;
//import org.openjdk.jmh.annotations.*;
//import org.openjdk.jmh.infra.Blackhole;
//import org.openjdk.jmh.runner.Runner;
//import org.openjdk.jmh.runner.RunnerException;
//import org.openjdk.jmh.runner.options.Options;
//import org.openjdk.jmh.runner.options.OptionsBuilder;
//import org.openjdk.jmh.runner.options.TimeValue;
//
//import java.io.IOException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//import static org.mockito.Matchers.any;
//import static org.mockito.Mockito.doAnswer;
//
//
//@State(Scope.Thread)
//public class OrderbookManagerTest {
//    //    https://www.baeldung.com/java-microbenchmark-harness
//    private String instrument = "unitTestInstrument_binance";
//    private String market = "binance";
//
//    private String instrumentPk= Configuration.formatLog("{}_{}",instrument,market);
//    private double priceTick=0.01;
//    private Orderbook orderbook;
//    private PaperTradingEngine paperTradingEngine;
//
//    private OrderbookManager orderbookManager;
//    private Trade lastTradeListen;
//    private Depth lastDepthListen;
//    private ExecutionReport lastExecutionReportListen;
//    private AtomicBoolean flipFlop = new AtomicBoolean(false);
//    @Before
//    public void setUp() throws Exception {
//        orderbook = new Orderbook(0.00001);
//        paperTradingEngine = Mockito.mock(PaperTradingEngine.class);
//        lastTradeListen = null;
//        lastDepthListen = null;
//        doAnswer(new Answer<Void>() {
//
//            public Void answer(InvocationOnMock invocation) {
//                //				Object[] args = invocation.getArguments();
//                Trade trade = invocation.getArgumentAt(0, Trade.class);
//                lastTradeListen = trade;
//                return null;
//            }
//        }).when(paperTradingEngine).notifyTrade(any(Trade.class));
//
//        doAnswer(new Answer<Void>() {
//
//            public Void answer(InvocationOnMock invocation) {
//                //				Object[] args = invocation.getArguments();
//                Depth depth = invocation.getArgumentAt(0, Depth.class);
//                lastDepthListen = depth;
//                return null;
//            }
//        }).when(paperTradingEngine).notifyDepth(any(Depth.class));
//
//        doAnswer(new Answer<Void>() {
//
//            public Void answer(InvocationOnMock invocation) {
//                //				Object[] args = invocation.getArguments();
//                ExecutionReport executionReport = invocation.getArgumentAt(0, ExecutionReport.class);
//                lastExecutionReportListen = executionReport;
//                return null;
//            }
//        }).when(paperTradingEngine).notifyExecutionReport(any(ExecutionReport.class));
//        orderbookManager = new OrderbookManager(orderbook, paperTradingEngine, instrumentPk);
//
//        Instrument instrument = new Instrument();
//        instrument.setPriceTick(priceTick);
//        instrument.setSymbol(this.instrument);
//        instrument.setMarket(this.market);
//        instrument.addMap();
//
//    }
//    private Depth createDepth(double bestBid, double bestAsk, double bestBidQty, double bestAskQty) {
//        Depth depth = new Depth();
//        depth.setTimestamp(System.currentTimeMillis());
//        depth.setInstrument(instrumentPk);
//        depth.setLevels(1);
//        Double[] asks = new Double[]{bestAsk, bestAsk + 0.01};
//        depth.setAsks(asks);
//
//        Double[] bids = new Double[]{bestBid, bestBid - 0.01};
//        depth.setBids(bids);
//
//        Double[] asksQ = new Double[]{bestAskQty, bestAskQty};
//        depth.setAsksQuantities(asksQ);
//
//        Double[] bidsQ = new Double[]{bestBidQty, bestBidQty};
//        depth.setBidsQuantities(bidsQ);
//
//        String[] algorithms = new String[]{Depth.ALGORITHM_INFO_MM, Depth.ALGORITHM_INFO_MM};
//        depth.setAsksAlgorithmInfo(algorithms);
//        depth.setBidsAlgorithmInfo(algorithms);
//        depth.setLevelsFromData();
//        return depth;
//    }
//    @Setup(Level.Invocation)
//    public void setupInvokation() throws Exception {
//        // executed before each invocation of the benchmark
//    }
//
//    @Setup(Level.Iteration)
//    public void setupIteration() throws Exception {
//        // executed before each invocation of the iteration
//    }
//
//
//
//    @Benchmark
//    @BenchmarkMode(Mode.AverageTime)
//    @Fork(warmups = 1, value = 1)
//    @Warmup(batchSize = -1, iterations = 3, time = 10, timeUnit = TimeUnit.MILLISECONDS)
//    @Measurement(batchSize = -1, iterations = 10, time = 10, timeUnit = TimeUnit.MILLISECONDS)
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    public void benchmark1 (Blackhole bh){
//        orderbookManager.reset();
//        Depth depth=null;
//        if (flipFlop.get()) {
//            depth = createDepth(90.0, 92.0, 6, 5);
//            flipFlop.set(false);
//        } else {
//            depth = createDepth(91.0, 93.0, 6, 5);
//            flipFlop.set(true);
//        }
//
//        orderbookManager.refreshMarketMakerDepth(depth);
//    }
//
//    @Test
//    public void testUpdateDepth() throws IOException, RunnerException {
//        String[] argv = {};
//        org.openjdk.jmh.Main.main(argv);
//
//    }
//}
