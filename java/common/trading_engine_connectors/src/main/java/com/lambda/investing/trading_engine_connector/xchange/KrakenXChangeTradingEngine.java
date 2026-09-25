package com.lambda.investing.trading_engine_connector.xchange;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.trading_engine_connector.TradingEngineConfiguration;
import com.lambda.investing.trading_engine_connector.kraken.KrakenCandleConverter;
import com.lambda.investing.trading_engine_connector.kraken.KrakenCandleRestClient;
import com.lambda.investing.trading_engine_connector.kraken.KrakenOhlcResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class KrakenXChangeTradingEngine extends XChangeTradingEngine {

    //Kraken's public OHLC endpoint returns up to 720 rows per call; guard against runaway pagination.
    private static final int MAX_OHLC_PAGES_PER_INSTRUMENT = 50;

    //Caps how many instruments are downloaded concurrently, to avoid hammering Kraken's rate limits.
    private static final int MAX_PARALLEL_CANDLE_DOWNLOADS = 8;
    private static final int PROGRESS_BAR_WIDTH = 30;

    private final KrakenCandleRestClient krakenCandleRestClient = new KrakenCandleRestClient();

    public KrakenXChangeTradingEngine(ConnectorConfiguration orderRequestConnectorConfiguration,
                                      ConnectorProvider orderRequestConnectorProvider,
                                      ConnectorConfiguration executionReportConnectorConfiguration,
                                      ConnectorPublisher executionReportConnectorPublisher, TradingEngineConfiguration tradingEngineConfiguration,
                                      Set<Instrument> instrumentSet) {
        super(orderRequestConnectorConfiguration, orderRequestConnectorProvider, executionReportConnectorConfiguration, executionReportConnectorPublisher, tradingEngineConfiguration, instrumentSet);
    }


    @Override
    public Map<String, List<Candle>> requestCandles(Date startDate, Date endDate, Set<String> instruments,
                                                    CandleType candleType, int secondsCandles) {
        logger.info("requesting candles for instruments {} from {} to {} with candleType {} and secondsCandles {}", instruments, startDate, endDate, candleType, secondsCandles);

        int intervalMinutes = KrakenCandleConverter.resolveIntervalMinutes(secondsCandles);
        long startSeconds = startDate.getTime() / 1000L;
        long endSeconds = endDate.getTime() / 1000L;
        int totalInstruments = instruments.size();

        Map<String, List<Candle>> candlesByInstrument = new ConcurrentHashMap<>();
        AtomicInteger completedInstruments = new AtomicInteger(0);

        ExecutorService executorService = newCandleDownloadExecutor(totalInstruments);
        try {
            printProgress(0, totalInstruments);
            List<CompletableFuture<Void>> downloads = instruments.stream()
                    .map(instrumentPk -> CompletableFuture.runAsync(() -> downloadInstrumentCandles(instrumentPk,
                            candleType, intervalMinutes, startSeconds, endSeconds, candlesByInstrument,
                            completedInstruments, totalInstruments), executorService))
                    .collect(Collectors.toList());
            downloads.forEach(CompletableFuture::join);
        } finally {
            executorService.shutdown();
            System.out.println();
        }
        return candlesByInstrument;
    }

    private ExecutorService newCandleDownloadExecutor(int totalInstruments) {
        int poolSize = Math.max(1, Math.min(MAX_PARALLEL_CANDLE_DOWNLOADS, totalInstruments));
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "KrakenCandleDownloader");
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(poolSize, threadFactory);
    }

    private void downloadInstrumentCandles(String instrumentPk, CandleType candleType, int intervalMinutes,
                                           long startSeconds, long endSeconds,
                                           Map<String, List<Candle>> candlesByInstrument,
                                           AtomicInteger completedInstruments, int totalInstruments) {
        String krakenPair = KrakenCandleConverter.toKrakenPair(instrumentPk);
        List<Candle> candles = krakenPair == null ?
                onUnmappableInstrument(instrumentPk) :
                requestCandlesForInstrument(instrumentPk, krakenPair, candleType, intervalMinutes, startSeconds,
                        endSeconds);

        candlesByInstrument.put(instrumentPk, candles);
        printProgress(completedInstruments.incrementAndGet(), totalInstruments);
    }

    private List<Candle> onUnmappableInstrument(String instrumentPk) {
        logger.error("cant map instrument {} to a Kraken pair -> skipping candles request", instrumentPk);
        return new ArrayList<>();
    }

    private void printProgress(int completed, int total) {
        if (total == 0) {
            return;
        }
        double ratio = (double) completed / total;
        int filled = (int) Math.round(ratio * PROGRESS_BAR_WIDTH);
        String bar = "=".repeat(filled) + " ".repeat(PROGRESS_BAR_WIDTH - filled);
        System.out.print(String.format("\rKraken candles download [%s] %d/%d instruments (%.0f%%)", bar, completed,
                total, ratio * 100));
        System.out.flush();
    }

    private List<Candle> requestCandlesForInstrument(String instrumentPk, String krakenPair, CandleType candleType,
                                                     int intervalMinutes, long startSeconds, long endSeconds) {
        List<Candle> candles = fetchAllPages(instrumentPk, krakenPair, candleType, intervalMinutes, startSeconds,
                endSeconds);
        return filterToRange(candles, startSeconds, endSeconds);
    }

    private List<Candle> fetchAllPages(String instrumentPk, String krakenPair, CandleType candleType,
                                       int intervalMinutes, long startSeconds, long endSeconds) {
        List<Candle> candles = new ArrayList<>();
        long since = startSeconds;
        try {
            for (int page = 0; page < MAX_OHLC_PAGES_PER_INSTRUMENT; page++) {
                KrakenOhlcResponse response = krakenCandleRestClient.getOhlc(krakenPair, intervalMinutes, since);
                if (response.hasError()) {
                    logger.error("Kraken OHLC request failed for pair {} (instrument {}): {}", krakenPair,
                            instrumentPk, response.getError());
                    break;
                }

                List<Candle> pageCandles = KrakenCandleConverter.toCandles(response, candleType, instrumentPk,
                        intervalMinutes);
                if (pageCandles.isEmpty()) {
                    break;
                }
                candles.addAll(pageCandles);

                long lastCandleSeconds = pageCandles.get(pageCandles.size() - 1).getTimestamp() / 1000L;
                boolean noMoreNewData = response.getLast() <= since;
                boolean reachedEndDate = lastCandleSeconds >= endSeconds;
                if (noMoreNewData || reachedEndDate) {
                    break;
                }
                since = response.getLast();
            }
        } catch (Exception e) {
            logger.error("error requesting candles from Kraken for pair {} (instrument {})", krakenPair,
                    instrumentPk, e);
        }
        return candles;
    }

    private List<Candle> filterToRange(List<Candle> candles, long startSeconds, long endSeconds) {
        long startMillis = startSeconds * 1000L;
        long endMillis = endSeconds * 1000L;
        return candles.stream()
                .filter(candle -> candle.getTimestamp() >= startMillis && candle.getTimestamp() <= endMillis)
                .collect(Collectors.toList());
    }

}
