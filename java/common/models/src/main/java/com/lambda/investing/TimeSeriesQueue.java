package com.lambda.investing;

import java.util.*;

public class TimeSeriesQueue<K> extends ArrayList<K> implements Queue<K> {

    private int maxSize;
    protected final Object lockAdd = new Object();

    public TimeSeriesQueue(int size) {
        this.maxSize = size;
    }

    public TimeSeriesQueue(TimeSeriesQueue other) {
        this.maxSize = other.maxSize;
        this.addAll(other);
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

    public static TimeSeriesQueue<Double> substract(TimeSeriesQueue<Double> series1, TimeSeriesQueue<Double> series2) {
        //return a timeseries of candlesAsk- candlesBid
        assert series1.size() > 0;
        assert series1.size() == series2.size();
        TimeSeriesQueue<Double> spread = new TimeSeriesQueue<>(series1.size());
        for (int i = 0; i < series1.size(); i++) {
            spread.offer(series1.get(i) - series2.get(i));
        }
        return spread;
    }

    @Override
    public boolean offer(K k) {
        return add(k);
    }

    @Override
    public K remove() {
        if (isEmpty()) {
            throw new NoSuchElementException("TimeSeriesQueue is empty");
        }
        K oldest = getOldest();
        remove(0);
        return oldest;
    }

    @Override
    public K poll() {
        if (isEmpty()) {
            return null;
        }
        K oldest = getOldest();
        remove(0);
        return oldest;
    }

    @Override
    public K element() {
        if (isEmpty()) {
            throw new NoSuchElementException("TimeSeriesQueue is empty");
        }
        return getOldest();
    }

    @Override
    public K peek() {
        if (isEmpty()) {
            return null;
        }
        return getOldest();
    }

    public K getNewest() {
        int currentSize = size();
        if (currentSize == 0) {
            return null;
        }
        return get(currentSize - 1);
    }

    public K getBeforeNewest() {
        int currentSize = size();
        if (currentSize < 2) {
            return null;
        }
        return get(currentSize - 2);
    }

    public TimeSeriesQueue<K> getLastNew(int n) {
        if (isEmpty()) {
            return null;
        }
        TimeSeriesQueue<K> last = new TimeSeriesQueue<>(n);
        last.addAll(subList(size() - n, size()));
        return last;
    }

    public TimeSeriesQueue<K> getFirstOld(int n) {
        if (isEmpty()) {
            return null;
        }
        TimeSeriesQueue<K> first = new TimeSeriesQueue<>(n);
        first.addAll(subList(0, size() - n));
        return first;
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

    public List<K> getListFirstOldest() {
        if (isEmpty()) {
            return null;
        }
        //newest is the last element of the index
        return new ArrayList<>(subList(0, size()));
    }

    public List<K> getListFirstNewest() {
        if (isEmpty()) {
            return null;
        }
        //newest is the last element of the index
        List<K> output = getListFirstOldest();
        Collections.reverse(output);
        return output;
    }

    public List<Integer> getIndex(List<Date> listElements, long delta) {
        if (isEmpty()) {
            return new TimeSeriesQueue<>(0);
        }

        List<Date> thisListSorted = (List<Date>) getListFirstOldest();
        List<Integer> index = new ArrayList<>();
        for (Date dateSearched : listElements) {
            if (delta <= 0) {
                index.add(indexOf(dateSearched));
                continue;
            }
            //find the element that is closest to the date dateElement by delta and return the index
            int foundIndex = -1;
            Date lastFound = null;
            for (int i = 0; i < thisListSorted.size(); i++) {
                Date dateInList = thisListSorted.get(i);

                if (Math.abs(dateInList.getTime() - dateSearched.getTime()) <= delta) {
                    if (lastFound == null) {
                        lastFound = dateInList;
                    } else {
                        if (Math.abs(dateInList.getTime() - dateSearched.getTime()) < Math.abs(lastFound.getTime() - dateSearched.getTime())) {
                            //new value is closer
                            lastFound = dateInList;
                        } else {
                            //new value is further
                            continue;
                        }
                    }
                    foundIndex = i;
                }

                if (dateInList.getTime() - dateSearched.getTime() > delta) {
                    //we are going too far -> dont lose time
                    break;
                }
            }
            index.add(foundIndex);

        }
        return index;
    }
    public List<Integer> getIndex(List<K> listElements) {
        if (isEmpty()) {
            return new TimeSeriesQueue<>(0);
        }

        List<Integer> index = new ArrayList<>();
        for (K element : listElements) {
            index.add(indexOf(element));
        }
        return index;
    }

    public TimeSeriesQueue<K> getSubList(List<Integer> index) {
        if (isEmpty()) {
            return new TimeSeriesQueue<>(0);
        }
        TimeSeriesQueue<K> output = new TimeSeriesQueue<>(size());
        for (int indexPosition : index) {
            if (indexPosition < 0) {
                continue;
            }
            output.offer(get(indexPosition));
        }
        return output;
    }

    public List<K> getUnique() {
        return ArrayUtils.unique(this);
    }

    public static TimeSeriesQueue<Double> log(TimeSeriesQueue<Double> prices) {
        if (prices.isEmpty()) {
            return prices;
        }
        TimeSeriesQueue<Double> logInstrument = new TimeSeriesQueue<>(prices.size());
        for (int i = 0; i < prices.size(); i++) {
            double logReturn = Math.log(prices.get(i));
            logInstrument.offer(logReturn);
        }
        return logInstrument;
    }

    public static TimeSeriesQueue<Double> pctChange(TimeSeriesQueue<Double> prices) {
        if (prices.isEmpty()) {
            return prices;
        }
        TimeSeriesQueue<Double> returnsInstrument = new TimeSeriesQueue<>(prices.size() - 1);
        for (int i = 1; i < prices.size(); i++) {
            double returnCandle = (prices.get(i) / prices.get(i - 1)) - 1;//like in python : stat_arb.stat_arb_instrument.StatArbInstrument.get_candle_returns
            returnsInstrument.offer(returnCandle);
        }
        return returnsInstrument;
    }

    public static TimeSeriesQueue<Double> ffillZeros(TimeSeriesQueue<Double> prices) {
        if (prices.isEmpty()) {
            return prices;
        }
        TimeSeriesQueue<Double> output = new TimeSeriesQueue<>(prices.size());
        output.offer(prices.getOldest());
        for (int i = 1; i < prices.size(); i++) {
            double value = prices.get(i) == 0 ? prices.get(i - 1) : prices.get(i);
            output.offer(value);
        }
        return output;
    }

    @Override
    public Object clone() {
        return new TimeSeriesQueue(this);
    }
}