package com.lambda.investing;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Single shared {@link ScheduledExecutorService} for all statistics printers
 * ({@link Statistics}, {@link LatencyStatistics}, and {@code SlippageStatistics}).
 * One low-priority daemon thread is sufficient because the tasks are infrequent log flushes.
 */
public final class StatisticsScheduler {

    public static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Statistics_printer");
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        return t;
    });

    private StatisticsScheduler() {
    }
}
