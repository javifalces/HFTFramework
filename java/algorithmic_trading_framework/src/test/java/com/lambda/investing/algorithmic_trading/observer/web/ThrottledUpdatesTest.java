package com.lambda.investing.algorithmic_trading.observer.web;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertEquals;

public class ThrottledUpdatesTest {

    @Test
    public void skippedUpdatesAreDeferredAndOnlyLatestPerKeyIsKept() throws Exception {
        List<String> handled = new CopyOnWriteArrayList<>();
        ThrottledUpdates<String, String> throttler = new ThrottledUpdates<>(() -> 100, handled::add);

        throttler.submit("a", "a1");
        throttler.submit("a", "a2");
        throttler.submit("a", "a3");
        throttler.submit("b", "b1");
        assertEquals(List.of("a1", "b1"), handled);

        Thread.sleep(400);
        assertEquals(List.of("a1", "b1", "a3"), handled);
    }

    @Test
    public void zeroIntervalDisablesThrottling() {
        List<String> handled = new CopyOnWriteArrayList<>();
        ThrottledUpdates<String, String> throttler = new ThrottledUpdates<>(() -> 0, handled::add);
        throttler.submit("a", "a1");
        throttler.submit("a", "a2");
        assertEquals(List.of("a1", "a2"), handled);
    }
}
