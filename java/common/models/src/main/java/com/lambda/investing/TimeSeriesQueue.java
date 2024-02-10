package com.lambda.investing;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Queue;

public class TimeSeriesQueue<K> extends ArrayList<K> implements Queue<K> {

    private int maxSize;
    protected final Object lockAdd = new Object();

    public TimeSeriesQueue(int size) {
        this.maxSize = size;
    }

    public boolean add(K k) {
        synchronized (lockAdd) {
            boolean r = super.add(k);
            if (size() > maxSize) {
                removeRange(0, size() - maxSize);
            }
            return r;
        }
    }

    @Override
    public boolean offer(K k) {
        return add(k);
    }

    @Override
    public K remove() {
        if (size() == 0) {
            throw new NoSuchElementException("TimeSeriesQueue is empty");
        }
        K oldest = getOldest();
        remove(0);
        return oldest;
    }

    @Override
    public K poll() {
        if (size() == 0) {
            return null;
        }
        K oldest = getOldest();
        remove(0);
        return oldest;
    }

    @Override
    public K element() {
        if (size() == 0) {
            throw new NoSuchElementException("TimeSeriesQueue is empty");
        }
        return getOldest();
    }

    @Override
    public K peek() {
        if (size() == 0) {
            return null;
        }
        return getOldest();
    }

    public K getNewest() {
        if (size() == 0) {
            return null;
        }
        return get(size() - 1);
    }

    public K getBeforeNewest() {
        if (size() < 2) {
            return null;
        }
        return get(size() - 2);
    }

    public void changeNewest(K value) {
        synchronized (lockAdd) {
            int size = size();
            if (size == 0) {
                offer(value);
                return;
            }
            int index = size - 1;
            super.set(index, value);
        }
    }

    public K getOldest() {
        return get(0);
    }


}