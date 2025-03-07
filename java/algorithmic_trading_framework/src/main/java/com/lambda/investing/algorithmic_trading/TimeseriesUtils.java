package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.ArrayUtils;
import com.lambda.investing.TimeSeriesQueue;
import javafx.util.Pair;
import lombok.Getter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.*;

import static com.lambda.investing.ArrayUtils.GetPrimitiveArrayDouble;

public class TimeseriesUtils {

    @Getter
    public static class TupleQueue {

        private long timestamp;
        private double value;

        public TupleQueue(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }

    public static Double[] GetArrayInput(List<Double> input) {
        Double[] spreadArr = new Double[input.size()];
        spreadArr = input.toArray(spreadArr);
        return spreadArr;
    }

    public static double GetMean(TimeSeriesQueue<Double> serie) {
        return GetMean(serie.toArray(new Double[serie.size()]));
    }

    public static double GetMean(Double[] serie) {
        double sum = 0.;
        for (int i = 0; i < serie.length; i++) {
            sum += serie[i];
        }
        double output = (double) sum / serie.length;
        return output;
    }
    public static double GetExponentialWeightedMean(Double[] series) {
        double alpha = 2.0 / (series.length + 1);
        return GetExponentialWeightedMean(ArrayUtils.ArrayReverse(series), alpha);
    }

    public static double GetExponentialWeightedMean(Double[] series, double alpha) {
        double ema = series[series.length - 1]; // start with the first element
        for (int i = series.length - 2; i >= 0; i--) { // iterate from the end to the start
            ema = series[i] * alpha + ema * (1 - alpha);
        }
        return ema;
    }

    public static double GetVariance(TimeSeriesQueue<Double> serie) {
        return GetVariance(serie.toArray(new Double[serie.size()]));
    }

    public static double GetVariance(Double[] serie) {
        return GetVariance(serie, false);
    }

    public static double GetVariance(Double[] serie, boolean bias) {
        double mean = GetMean(serie);
        double sqDiff = 0;
        for (int i = 0; i < serie.length; i++) {
            sqDiff += Math.pow((serie[i] - mean), 2);
        }
        double den = bias ? serie.length - 1 : serie.length;
        double output = (double) sqDiff / den;
        return output;
    }
    public static double GetWeightedMeanVariance(Double[] serie, boolean bias) {
        double mean = GetExponentialWeightedMean(serie);
        double sqDiff = 0;
        for (int i = 0; i < serie.length; i++) {
            sqDiff += Math.pow((serie[i] - mean), 2);
        }
        double den = bias ? serie.length - 1 : serie.length;
        double output = (double) sqDiff / den;
        return output;
    }
    public static double GetWeightedMeanVariance(Double[] serie) {
        return GetWeightedMeanVariance(serie, false);
    }

    public static double GetMedian(Double[] serie) {
        int length = serie.length;
        Double[] sortedSerie = serie.clone();
        Arrays.sort(sortedSerie);

        double output = 0;
        if (length % 2 == 1) {
            output = sortedSerie[(length + 1) / 2 - 1];
        } else {
            output = (sortedSerie[length / 2 - 1] + sortedSerie[length / 2]) / 2;
        }
        return output;
    }

    public static double GetStd(TimeSeriesQueue<Double> serie) {
        return GetStd(serie.toArray(new Double[serie.size()]));
    }

    public static double GetStd(Double[] serie) {
        return GetStd(serie, true);
    }

    public static double GetStd(Double[] serie, boolean bias) {
        return Math.sqrt(GetVariance(serie, bias));
    }

    public static double GetWeightedMeanStd(Double[] serie) {
        return GetWeightedMeanStd(serie, false);
    }

    public static double GetWeightedMeanStd(Double[] serie, boolean bias) {
        return Math.sqrt(GetWeightedMeanVariance(serie, bias));
    }


    public static double GetHampelScore(Double[] serie, double value) {
        double med = GetMedian(serie);
        double std = GetStd(serie);
        double score = (value - med) / std;
        return score;
    }

    public static double GetZscore(Double[] serie, double value) {
        double mean = GetMean(serie);
        double std = GetStd(serie);
        double zscore = (value - mean) / std;
        return zscore;
    }

    public static double GetWeightedMeanZscore(Double[] serie, double value) {
        double mean = GetExponentialWeightedMean(serie);
        double std = GetWeightedMeanStd(serie);
        double zscore = (value - mean) / std;
        return zscore;
    }

    public static Pair<List<TupleQueue>, List<TupleQueue>> MergeTupleFfillQueue(List<TupleQueue> instrument1,
                                                                                List<TupleQueue> instrument2) {
        boolean biggerIsOne = true;
        List<TupleQueue> biggerQueue = instrument1;
        List<TupleQueue> smallerQueue = instrument2;
        if (instrument1.size() < instrument2.size()) {
            biggerQueue = instrument2;
            smallerQueue = instrument1;
            biggerIsOne = false;
        }
        long startingTimestamp = Math.max(instrument1.get(0).getTimestamp(), instrument2.get(0).getTimestamp());
        biggerQueue.get(0).timestamp = startingTimestamp;
        smallerQueue.get(0).timestamp = startingTimestamp;

        TupleQueue[] smallerQueueOutputArray = new TupleQueue[biggerQueue.size()];
        int counter = 0;
        for (TupleQueue tuple : biggerQueue) {
            for (TupleQueue tupleSmall : smallerQueue) {
                if (tupleSmall.getTimestamp() >= tuple.getTimestamp()) {
                    smallerQueueOutputArray[counter] = tupleSmall;
                    break;
                }
            }
            counter++;
        }
        //ffill
        counter = 0;
        TupleQueue lastTupleSmall = null;
        TupleQueue[] smallerQueueOutputArrayFilled = smallerQueueOutputArray.clone();
        if (smallerQueueOutputArrayFilled[0] == null) {
            smallerQueueOutputArrayFilled[0] = smallerQueue.get(0);
        }
        for (TupleQueue tupleSmall : smallerQueueOutputArray) {
            if (tupleSmall == null) {
                smallerQueueOutputArrayFilled[counter] = lastTupleSmall;
            } else {
                lastTupleSmall = tupleSmall;
            }
            counter++;
        }
        smallerQueue = Arrays.asList(smallerQueueOutputArrayFilled.clone());
        if (biggerIsOne) {
            instrument1 = biggerQueue;
            instrument2 = smallerQueue;
        } else {
            instrument2 = biggerQueue;
            instrument1 = smallerQueue;
        }
        assert instrument2.size() == instrument1.size();
        return new Pair<>(instrument1, instrument2);
    }

    private static List<TupleQueue> getLastValues(long takeUntil,
                                                  Map<String, Queue<TupleQueue>> values, String instrumentPk) {
        List<TupleQueue> instrumentValues = new ArrayList<>(values.get(instrumentPk));
        Collections.reverse(instrumentValues);
        List<TupleQueue> selected = new ArrayList<>();
        selected.add(instrumentValues.get(0));//add the last one,on the first item
        for (TupleQueue item : instrumentValues.subList(1, instrumentValues.size())) {
            long timestamp = item.getTimestamp();
            if (timestamp < takeUntil) {
                break;
            }
            selected.add(item);
        }

        Collections.reverse(selected);

        return selected;
    }

    public static double GetLastZscoreTimeseries(String instrument, String otherInstrument, long currentTimestamp,
                                                 int periodMs, Map<String, Queue<TupleQueue>> values) {
        //only get the  last period values

        long takeUntil = (long) currentTimestamp - periodMs;//

        //instrument 1
        List<TupleQueue> instrumentValuesSelected = getLastValues(takeUntil, values, instrument);
        //instrument 2
        List<TupleQueue> otherValuesSelected = getLastValues(takeUntil, values, otherInstrument);

        if (instrumentValuesSelected.size() == 0 || otherValuesSelected.size() == 0) {
            //no values yet
            return 0.0;
        }
        Pair<List<TupleQueue>, List<TupleQueue>> merged = MergeTupleFfillQueue(
                instrumentValuesSelected, otherValuesSelected);

        if (merged.getKey().size() < 3) {
            //			logger.warn("otherValuesSelected size:{} instrumentValuesSelected size:{} ->return 0.0",
            //					otherValuesSelected.size(), instrumentValuesSelected.size());
            return 0.0;
        }

        List<TupleQueue> instrumentValuesSelectedOut = merged.getKey();
        List<TupleQueue> otherValuesSelectedOut = merged.getValue();
        int minSize = Math.min(instrumentValuesSelectedOut.size(), otherValuesSelectedOut.size());

        List<Double> distance = new ArrayList<>();
        double myPrice = Double.NaN;
        double otherPrice = Double.NaN;
        for (int i = 0; i < minSize; i++) {
            try {
                myPrice = instrumentValuesSelectedOut.get(i).getValue();
            } catch (Exception e) {
                if (Double.isNaN(myPrice)) {
                    return 0.0;
                }
            }
            try {
                otherPrice = otherValuesSelectedOut.get(i).getValue();
            } catch (Exception e) {
                if (Double.isNaN(otherPrice)) {
                    return 0.0;
                }
            }
            double spread = myPrice - otherPrice;
            distance.add(spread);
        }

        double lastValue = distance.get(distance.size() - 1);
        double zscore = TimeseriesUtils.GetZscore(TimeseriesUtils.GetArrayInput(distance), lastValue);
        return zscore;
    }

    public static double GetZscorePositive(Double[] serie, double value) {
        Double[] seriePos = new Double[serie.length];
        for (int i = 0; i < seriePos.length; i++) {
            seriePos[i] = Math.abs(serie[i]);
        }
        double valuePos = Math.abs(value);

        double mean = GetMean(seriePos);
        double std = GetStd(seriePos);
        double zscorePos = (valuePos - mean) / std;
        return zscorePos;
    }

}
