package com.lambda.investing.market_data_connector.binance;

import com.binance.api.client.domain.event.DepthEvent;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.lambda.investing.binance.BinanceBrokerConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.Map;
import java.util.TreeMap;

/**
 * Maintains a full local order book for a single Binance symbol, built from the exchange
 * diff-depth ("@depth") update stream following the official synchronization procedure:
 * https://developers.binance.com/docs/binance-spot-api-docs/web-socket-streams#how-to-manage-a-local-order-book-correctly
 * <p>
 * 1) buffer depth events while a REST snapshot is fetched 2) drop buffered events already
 * contained in the snapshot 3) apply the remaining buffered events in order 4) keep applying
 * live events, each level update replaces the absolute quantity for that price (qty==0 removes
 * the level) 5) detect update-id gaps and trigger a resync when one is found.
 */
class BinanceLocalOrderBook {

    private final Logger logger = LogManager.getLogger(BinanceLocalOrderBook.class);
    private final String symbol;

    //bids sorted highest price first, asks sorted lowest price first
    private final TreeMap<Double, Double> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Double, Double> asks = new TreeMap<>();
    private final Deque<DepthEvent> pendingEvents = new ArrayDeque<>();

    private volatile boolean initialized = false;
    private volatile boolean snapshotRequestInFlight = false;
    private long lastUpdateId = -1L;

    BinanceLocalOrderBook(String symbol) {
        this.symbol = symbol;
    }

    synchronized boolean isInitialized() {
        return initialized;
    }

    synchronized boolean isSnapshotRequestInFlight() {
        return snapshotRequestInFlight;
    }

    synchronized void markSnapshotRequested() {
        snapshotRequestInFlight = true;
    }

    synchronized void resetSnapshotRequest() {
        snapshotRequestInFlight = false;
    }

    synchronized void bufferEvent(DepthEvent event) {
        pendingEvents.addLast(event);
    }

    /**
     * Applies a REST snapshot and replays any buffered events received while it was being
     * fetched. If there is a gap between the snapshot and the buffered events the book is left
     * uninitialized so a new snapshot will be requested on the next update.
     */
    synchronized void initFromSnapshot(OrderBook snapshot) {
        bids.clear();
        asks.clear();
        for (OrderBookEntry entry : snapshot.getBids()) {
            putLevel(bids, entry);
        }
        for (OrderBookEntry entry : snapshot.getAsks()) {
            putLevel(asks, entry);
        }
        lastUpdateId = snapshot.getLastUpdateId();
        snapshotRequestInFlight = false;

        boolean firstEventApplied = false;
        DepthEvent event;
        while ((event = pendingEvents.poll()) != null) {
            if (event.getFinalUpdateId() <= lastUpdateId) {
                continue; //stale event, already contained in the snapshot
            }
            if (!firstEventApplied) {
                if (event.getFirstUpdateId() > lastUpdateId + 1) {
                    logger.warn(
                            "gap between snapshot and first buffered event on {} (event.U:{} snapshot.lastUpdateId:{})"
                                    + " -> resync required", symbol, event.getFirstUpdateId(), lastUpdateId);
                    initialized = false;
                    lastUpdateId = -1L;
                    return;
                }
                firstEventApplied = true;
            }
            applyUpdateUnsafe(event);
        }
        initialized = true;
    }

    /**
     * @return false when an update-id gap is detected and the book needs to be resynchronized
     * from a fresh snapshot; the event is buffered in that case.
     */
    synchronized boolean applyUpdate(DepthEvent event) {
        if (!initialized) {
            pendingEvents.addLast(event);
            return true;
        }
        if (event.getFinalUpdateId() <= lastUpdateId) {
            return true; //already applied / older than current state
        }
        if (event.getFirstUpdateId() > lastUpdateId + 1) {
            logger.warn("update-id gap detected on {} local order book (event.U:{} previous.u:{}) -> resync required",
                    symbol, event.getFirstUpdateId(), lastUpdateId);
            initialized = false;
            lastUpdateId = -1L;
            pendingEvents.clear();
            pendingEvents.addLast(event);
            return false;
        }
        applyUpdateUnsafe(event);
        return true;
    }

    private void applyUpdateUnsafe(DepthEvent event) {
        for (OrderBookEntry entry : event.getBids()) {
            putLevel(bids, entry);
        }
        for (OrderBookEntry entry : event.getAsks()) {
            putLevel(asks, entry);
        }
        lastUpdateId = event.getFinalUpdateId();
    }

    private void putLevel(Map<Double, Double> book, OrderBookEntry entry) {
        try {
            double price = BinanceBrokerConnector.NUMBER_FORMAT.parse(entry.getPrice().toUpperCase()).doubleValue();
            double qty = BinanceBrokerConnector.NUMBER_FORMAT.parse(entry.getQty().toUpperCase()).doubleValue();
            if (qty == 0) {
                book.remove(price);
            } else {
                book.put(price, qty);
            }
        } catch (Exception e) {
            logger.error("Error parsing order book level {} on {}", entry, symbol, e);
        }
    }

    synchronized double[] getTopBidPrices(int maxDepth) {
        return toArray(bids.keySet(), maxDepth);
    }

    synchronized double[] getTopBidQuantities(int maxDepth) {
        return toArray(bids.values(), maxDepth);
    }

    synchronized double[] getTopAskPrices(int maxDepth) {
        return toArray(asks.keySet(), maxDepth);
    }

    synchronized double[] getTopAskQuantities(int maxDepth) {
        return toArray(asks.values(), maxDepth);
    }

    private double[] toArray(Iterable<Double> values, int maxDepth) {
        double[] output = new double[maxDepth];
        int index = 0;
        for (double value : values) {
            if (index >= maxDepth) {
                break;
            }
            output[index++] = value;
        }
        return index < maxDepth ? Arrays.copyOf(output, index) : output;
    }
}
