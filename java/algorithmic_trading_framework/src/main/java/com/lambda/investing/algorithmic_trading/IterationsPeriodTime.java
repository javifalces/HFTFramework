package com.lambda.investing.algorithmic_trading;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public enum IterationsPeriodTime {
	HALF_HOUR(-10, 30 * 60 * 1000), HOUR(-11, 60 * 60 * 1000), TWO_HOURS(-12, 120 * 60 * 1000), THREE_HOURS(-13,
			180 * 60 * 1000), FOUR_HOURS(-14, 4 * 60 * 60 * 1000), FIVE_HOURS(-15, 5 * 60 * 60 * 1000), SIX_HOURS(-16,
			6 * 60 * 60 * 1000), SEVEN_HOURS(-17, 7 * 60 * 60 * 1000), EIGHT_HOURS(-18, 8 * 60 * 60 * 1000), DAILY(-24,
			12 * 60 * 60 * 1000), END_OF_SESSION(-25, 25 * 60 * 60 * 1000);

	private long msPeriod;
	private int value;
	private static Map<Integer, IterationsPeriodTime> map = new HashMap<Integer, IterationsPeriodTime>();

	private IterationsPeriodTime(int value, long msPeriod) {
		this.value = value;
		this.msPeriod = msPeriod;
	}

	static {
		for (IterationsPeriodTime pageType : IterationsPeriodTime.values()) {
			map.put(pageType.value, pageType);
		}
	}

	public static IterationsPeriodTime valueOf(int pageType) {
		return (IterationsPeriodTime) map.get(pageType);
	}

	public long getMsPeriod() {
		return msPeriod;
	}

	public int getValue() {
		return value;
	}

	public static boolean isPeriodicalPeriod(int iterationPeriod) {
		for (int period : map.keySet()) {
			if (iterationPeriod == period)
				return true;
		}
		return false;
	}

	public boolean hasPassed(Date startDate, Date currentDate) {
		long msPassed = (currentDate.getTime() - startDate.getTime());
		if (msPassed > getMsPeriod()) {
			return true;
		}
		return false;

	}

}
