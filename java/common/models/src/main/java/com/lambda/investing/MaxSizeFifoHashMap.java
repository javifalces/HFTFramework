package com.lambda.investing;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.StampedLock;

/**
 * A high-performance, thread-safe, bounded HashMap with FIFO (First-In-First-Out) eviction policy.
 * Optimized for low-latency, high-frequency trading scenarios.
 * <p>
 * When the map exceeds the specified maximum size, the oldest entry is automatically removed.
 * <p>
 * This class uses StampedLock with optimistic reads for maximum performance:
 * - Read operations use optimistic locking (zero contention in read-heavy scenarios)
 * - Write operations use exclusive locking
 * - Minimal object allocation to reduce GC pressure
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class MaxSizeFifoHashMap<K, V> {

    private final Map<K, V> map;
    private final int maxSize;
    private final StampedLock lock = new StampedLock();

    // Shared scheduler for lazy deletions across all instances
    private static final ScheduledExecutorService LAZY_DELETE_SCHEDULER =
            Executors.newScheduledThreadPool(1, r -> {
                Thread thread = new Thread(r, "MaxSizeFifoHashMap-LazyDelete");
                thread.setDaemon(true);
                return thread;
            });

    // Store pending deletion tasks so they can be cancelled if needed
    private final Map<K, ScheduledFuture<?>> pendingDeletions = new ConcurrentHashMap<>();

    /**
     * Creates a new MaxSizeFifoHashMap with the specified maximum size.
     * Pre-sizes the map to avoid rehashing during normal operation.
     *
     * @param maxSize the maximum number of entries allowed in the map
     * @throws IllegalArgumentException if maxSize is less than 1
     */
    public MaxSizeFifoHashMap(int maxSize) {
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize must be at least 1");
        }
        this.maxSize = maxSize;
        // Pre-size to avoid rehashing: initialCapacity = (maxSize / loadFactor) + 1
        int initialCapacity = (int) ((maxSize / 0.75f) + 1);
        this.map = new LinkedHashMap<>(initialCapacity, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > MaxSizeFifoHashMap.this.maxSize;
            }
        };
    }

    /**
     * Creates a new MaxSizeFifoHashMap with the specified maximum size and initial capacity.
     * Pre-sizes the map to avoid rehashing during normal operation.
     *
     * @param maxSize         the maximum number of entries allowed in the map
     * @param initialCapacity the initial capacity of the underlying map
     * @throws IllegalArgumentException if maxSize is less than 1 or initialCapacity is negative
     */
    public MaxSizeFifoHashMap(int maxSize, int initialCapacity) {
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize must be at least 1");
        }
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity cannot be negative");
        }
        this.maxSize = maxSize;
        // Use the larger of provided initialCapacity or calculated capacity
        int capacity = Math.max(initialCapacity, (int) ((maxSize / 0.75f) + 1));
        this.map = new LinkedHashMap<>(capacity, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > MaxSizeFifoHashMap.this.maxSize;
            }
        };
    }

    /**
     * Returns the underlying synchronized map.
     * This allows the MaxSizeFifoHashMap to be used wherever a Map is expected.
     *
     * @return the underlying map
     */
    public Map<K, V> getMap() {
        return map;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old value is replaced.
     * If adding this entry causes the map to exceed maxSize, the oldest entry is removed.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with key, or null if there was no mapping
     */
    public V put(K key, V value) {
        long stamp = lock.writeLock();
        try {
            return map.put(key, value);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * More efficient than calling put() multiple times as it acquires the write lock only once.
     *
     * @param m mappings to be stored in this map
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        long stamp = lock.writeLock();
        try {
            map.putAll(m);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or null if this map contains no mapping for the key.
     * Uses optimistic locking for minimal latency in read-heavy scenarios.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or null
     */
    public V get(Object key) {
        // Try optimistic read first (no locking overhead)
        long stamp = lock.tryOptimisticRead();
        V value = map.get(key);

        // Validate the optimistic read
        if (!lock.validate(stamp)) {
            // Fall back to read lock if validation failed
            stamp = lock.readLock();
            try {
                value = map.get(key);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return value;
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with key, or null if there was no mapping
     */
    public V remove(Object key) {
        long stamp = lock.writeLock();
        try {
            return map.remove(key);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Returns true if this map contains a mapping for the specified key.
     * Uses optimistic locking for minimal latency.
     *
     * @param key key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified key
     */
    public boolean containsKey(Object key) {
        // Try optimistic read first
        long stamp = lock.tryOptimisticRead();
        boolean result = map.containsKey(key);

        // Validate the optimistic read
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = map.containsKey(key);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    /**
     * Returns true if this map maps one or more keys to the specified value.
     * Uses optimistic locking for minimal latency.
     *
     * @param value value whose presence in this map is to be tested
     * @return true if this map maps one or more keys to the specified value
     */
    public boolean containsValue(Object value) {
        // Try optimistic read first
        long stamp = lock.tryOptimisticRead();
        boolean result = map.containsValue(value);

        // Validate the optimistic read
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = map.containsValue(value);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    /**
     * Returns the number of key-value mappings in this map.
     * Uses optimistic locking for minimal latency.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        // Try optimistic read first
        long stamp = lock.tryOptimisticRead();
        int result = map.size();

        // Validate the optimistic read
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = map.size();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    /**
     * Returns true if this map contains no key-value mappings.
     * Uses optimistic locking for minimal latency.
     *
     * @return true if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        // Try optimistic read first
        long stamp = lock.tryOptimisticRead();
        boolean result = map.isEmpty();

        // Validate the optimistic read
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = map.isEmpty();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
        long stamp = lock.writeLock();
        try {
            map.clear();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Returns the maximum size of this map.
     *
     * @return the maximum size
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Schedules a key to be removed from the map after the specified delay.
     * If a deletion is already scheduled for this key, the previous task is cancelled
     * and replaced with the new one. Optimized for minimal latency and GC pressure.
     * <p>
     * This method uses a shared thread pool to avoid creating new threads for each deletion.
     *
     * @param key         the key to be removed after the delay
     * @param delayMillis the delay in milliseconds before the key is removed
     * @return the ScheduledFuture representing the pending deletion, or null if delayMillis <= 0
     */
    public ScheduledFuture<?> lazyRemove(K key, long delayMillis) {
        if (delayMillis <= 0) {
            // Remove immediately if delay is zero or negative
            remove(key);
            return null;
        }

        // Atomically cancel existing task if present
        pendingDeletions.computeIfPresent(key, (k, oldTask) -> {
            if (!oldTask.isDone()) {
                oldTask.cancel(false);
            }
            return null; // Remove it so we can add the new task
        });

        // Schedule the new deletion with minimal lambda allocation
        ScheduledFuture<?> deletionTask = LAZY_DELETE_SCHEDULER.schedule(() -> {
            remove(key);
            pendingDeletions.remove(key);
        }, delayMillis, TimeUnit.MILLISECONDS);

        // Use putIfAbsent to handle race condition where another thread might have added a task
        ScheduledFuture<?> previousTask = pendingDeletions.putIfAbsent(key, deletionTask);
        if (previousTask != null) {
            // Another thread beat us to it, cancel our task and return the winner
            deletionTask.cancel(false);
            return previousTask;
        }

        return deletionTask;
    }

    /**
     * Schedules multiple keys to be removed from the map after the specified delay.
     * This is more efficient than calling lazyRemove multiple times.
     *
     * @param keys        the keys to be removed after the delay
     * @param delayMillis the delay in milliseconds before the keys are removed
     */
    public void lazyRemoveAll(Iterable<K> keys, long delayMillis) {
        for (K key : keys) {
            lazyRemove(key, delayMillis);
        }
    }

    /**
     * Cancels a pending lazy deletion for the specified key.
     *
     * @param key the key whose pending deletion should be cancelled
     * @return true if a pending deletion was cancelled, false if no deletion was pending
     */
    public boolean cancelLazyRemove(K key) {
        ScheduledFuture<?> task = pendingDeletions.remove(key);
        if (task != null && !task.isDone()) {
            return task.cancel(false);
        }
        return false;
    }

    /**
     * Returns the number of pending lazy deletions.
     *
     * @return the number of keys scheduled for lazy deletion
     */
    public int getPendingDeletionsCount() {
        return pendingDeletions.size();
    }

    /**
     * Cancels all pending lazy deletions.
     */
    public void cancelAllPendingDeletions() {
        for (ScheduledFuture<?> task : pendingDeletions.values()) {
            if (!task.isDone()) {
                task.cancel(false);
            }
        }
        pendingDeletions.clear();
    }

    /**
     * Clears all mappings and cancels all pending lazy deletions.
     */
    public void clearAll() {
        cancelAllPendingDeletions();
        clear();
    }

    @Override
    public String toString() {
        // toString is typically not performance-critical, but use optimistic read anyway
        long stamp = lock.tryOptimisticRead();
        String result = map.toString();

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = map.toString();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    /**
     * Shuts down the shared lazy delete scheduler.
     * This should only be called when the application is shutting down.
     * After calling this method, lazy delete operations will fail.
     */
    public static void shutdownLazyDeleteScheduler() {
        LAZY_DELETE_SCHEDULER.shutdown();
    }
}
