package com.lambda.investing.algorithmic_trading.observer.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Per-key, trailing-edge throttle for "latest value wins" updates.
 * <ul>
 *   <li>If the last run for a key was at least {@code intervalMs} ago the update is handled immediately.</li>
 *   <li>Otherwise it is stored (replacing any previously stored update of the same key) and handled
 *   once the interval has elapsed, so the final state of a burst is never lost.</li>
 *   <li>An interval {@code <= 0} disables throttling (every update is handled immediately).</li>
 * </ul>
 * Deferred updates run on a shared daemon thread, so the handler must be thread-safe.
 *
 * @param <K> key identifying updates that supersede each other (e.g. instrument)
 * @param <V> update payload; must not be {@code null}
 */
public class ThrottledUpdates<K, V> {

    private static final Logger logger = LogManager.getLogger(ThrottledUpdates.class);

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "throttled-updates");
        t.setDaemon(true);
        return t;
    });

    private static final class Slot<V> {
        long lastRunMs = 0;
        V pending;
        boolean flushScheduled;
    }

    private final LongSupplier intervalMs;
    private final Consumer<V> handler;
    private final Map<K, Slot<V>> slots = new ConcurrentHashMap<>();

    /**
     * @param intervalMs supplies the minimum milliseconds between handled updates of the same key
     * @param handler    processes an update
     */
    public ThrottledUpdates(LongSupplier intervalMs, Consumer<V> handler) {
        this.intervalMs = intervalMs;
        this.handler = handler;
    }

    public void submit(K key, V value) {
        long interval = intervalMs.getAsLong();
        if (interval <= 0) {
            handle(value);
            return;
        }
        Slot<V> slot = slots.computeIfAbsent(key, k -> new Slot<>());
        boolean runNow = false;
        long delayMs = -1;
        synchronized (slot) {
            long now = System.currentTimeMillis();
            if (!slot.flushScheduled && now - slot.lastRunMs >= interval) {
                slot.lastRunMs = now;
                runNow = true;
            } else {
                slot.pending = value;
                if (!slot.flushScheduled) {
                    slot.flushScheduled = true;
                    delayMs = Math.max(1, slot.lastRunMs + interval - now);
                }
            }
        }
        if (runNow) {
            handle(value);
        } else if (delayMs >= 0) {
            SCHEDULER.schedule(() -> flush(slot), delayMs, TimeUnit.MILLISECONDS);
        }
    }

    private void flush(Slot<V> slot) {
        V value;
        synchronized (slot) {
            value = slot.pending;
            slot.pending = null;
            slot.flushScheduled = false;
            if (value != null) {
                slot.lastRunMs = System.currentTimeMillis();
            }
        }
        if (value != null) {
            handle(value);
        }
    }

    private void handle(V value) {
        try {
            handler.accept(value);
        } catch (RuntimeException e) {
            logger.error("Throttled update handler failed", e);
        }
    }
}
