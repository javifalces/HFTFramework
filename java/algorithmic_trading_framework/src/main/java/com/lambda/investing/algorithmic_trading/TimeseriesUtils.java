package com.lambda.investing.algorithmic_trading;

public class TimeseriesUtils {

	public static double GetMean(Double[] serie) {
		double sum = 0.;
		for (int i = 0; i < serie.length - 1; i++) {
			sum += serie[i];
		}
		double output = (double) sum / serie.length;
		return output;
	}

	public static double GetVariance(Double[] serie) {
		double mean = GetMean(serie);
		double sqDiff = 0;
		for (int i = 0; i < serie.length - 1; i++) {
			sqDiff += (serie[i] - mean) * (serie[i] - mean);
		}
		double output = (double) sqDiff / serie.length;
		return output;
	}

	public static double GetMedian(Double[] serie) {
		int length = serie.length;

		double output = 0;
		if (length % 2 == 1) {
			output = serie[(length + 1) / 2 - 1];
		} else {
			output = (serie[length / 2 - 1] + serie[length / 2]) / 2;
		}
		return output;
	}

	public static double GetStd(Double[] serie) {
		return Math.sqrt(GetVariance(serie));
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
