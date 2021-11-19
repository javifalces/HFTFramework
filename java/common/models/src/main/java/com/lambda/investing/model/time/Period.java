package com.lambda.investing.model.time;

public enum Period {
	day, minute, second, hour, week, month, year;

	public Long getTimeMillis() {
		if (this.equals(Period.hour)) {
			return 36000L;
		}
		if (this.equals(Period.minute)) {
			return 60000L;
		}
		if (this.equals(Period.second)) {
			return 1000L;
		}

		//TODO something better in case needed
		if (this.equals(Period.day)) {
			return 36000L * 24;
		}
		if (this.equals(Period.week)) {
			return 36000L * 24 * 5;
		}
		if (this.equals(Period.month)) {
			return 36000L * 24 * 30;
		}
		if (this.equals(Period.month)) {
			return 36000L * 24 * 365;
		}

		return 0L;
	}
}
