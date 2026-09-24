package com.lambda.investing.trading_engine_connector.kraken;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Minimal REST client for Kraken's public market data API (no authentication required).
 *
 * @see <a href="https://docs.kraken.com/api/docs/rest-api/get-ohlc-data">Kraken OHLC docs</a>
 */
public class KrakenCandleRestClient {

    private static final String OHLC_ENDPOINT = "https://api.kraken.com/0/public/OHLC";

    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public KrakenCandleRestClient() {
        this(Duration.ofSeconds(10));
    }

    public KrakenCandleRestClient(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(requestTimeout).build();
    }

    /**
     * @param pair            Kraken asset pair code (eg. "XBTEUR")
     * @param intervalMinutes candle width in minutes, must be one of Kraken's supported intervals
     * @param sinceSeconds    return candles committed after this unix timestamp (seconds), may be null
     */
    public KrakenOhlcResponse getOhlc(String pair, int intervalMinutes, Long sinceSeconds)
            throws IOException, InterruptedException {
        StringBuilder urlBuilder = new StringBuilder(OHLC_ENDPOINT)
                .append("?pair=").append(URLEncoder.encode(pair, StandardCharsets.UTF_8))
                .append("&interval=").append(intervalMinutes);
        if (sinceSeconds != null) {
            urlBuilder.append("&since=").append(sinceSeconds);
        }

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlBuilder.toString()))
                .timeout(requestTimeout).GET().build();

        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (httpResponse.statusCode() != 200) {
            throw new IOException(
                    "Kraken OHLC request failed with HTTP " + httpResponse.statusCode() + ": " + httpResponse
                            .body());
        }
        return KrakenOhlcResponse.parse(httpResponse.body());
    }
}
