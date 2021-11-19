package com.lambda.investing.algorithmic_trading;

import java.util.Calendar;
import java.util.Date;

public interface TimeServiceIfc {

	String getCurrentTimezone();

	Date getCurrentTime();

	long getCurrentTimestamp();

	void setCurrentTimestamp(long timestamp);

	int getCurrentTimeHour();

	int getCurrentTimeMinute();

	int getCurrentTimeDay();

	int getCurrentTimeMonth();

	int getDayOfWeek();

	Calendar getCalendar();

	void sleepMs(long msToSleep) throws InterruptedException;
}
