package com.lambda.investing.algorithmic_trading;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimeService implements TimeServiceIfc {

	//	System.setProperty("user.timezone", "GMT");
	protected Logger logger = LogManager.getLogger(TimeService.class);
	public static String DEFAULT_TIMEZONE = "UTC";
	public static ZoneId DEFAULT_ZONEID = ZoneId.of(DEFAULT_TIMEZONE);
	protected String timezone;
	protected Calendar calendar;

	protected long currentTimestamp = 0L;

	public TimeService(String timezone) {
		this.timezone = timezone;
		this.calendar = Calendar.getInstance(TimeZone.getTimeZone(this.timezone));
		this.calendar.setTimeInMillis(currentTimestamp);
	}

	public TimeService() {
		this.timezone = DEFAULT_TIMEZONE;
		this.calendar = Calendar.getInstance(TimeZone.getTimeZone(this.timezone));
		this.calendar.setTimeInMillis(currentTimestamp);
	}

	private void updateTimestamp() {
		this.calendar.setTimeInMillis(this.currentTimestamp);
	}

	public void setCurrentTimestamp(long currentTimestamp) {
		if (currentTimestamp < this.currentTimestamp) {
			//			logger.warn("trying to go back to the past!  {}< current {}", currentTimestamp, this.currentTimestamp);
			return;
		}
		this.currentTimestamp = currentTimestamp;
		this.calendar.setTimeInMillis(currentTimestamp);
	}

	@Override
	public Calendar getCalendar() {
		return calendar;
	}

	@Override
	public void sleepMs(long msToSleep) throws InterruptedException {
		Thread.sleep(msToSleep);
	}

	@Override
	public void reset() {
		this.currentTimestamp = 0;
	}

	@Override
	public String getCurrentTimezone() {
		return this.timezone;
	}

	@Override
	public Date getCurrentTime() {
		updateTimestamp();
		return calendar.getTime();
	}

	@Override public long getCurrentTimestamp() {
		updateTimestamp();

		return currentTimestamp;
	}

	@Override public int getCurrentTimeHour() {
		updateTimestamp();
		return calendar.get(Calendar.HOUR_OF_DAY);
	}

	@Override public int getCurrentTimeMinute() {
		updateTimestamp();
		return calendar.get(Calendar.MINUTE);
	}

	@Override public int getCurrentTimeDay() {
		updateTimestamp();
		return calendar.get(Calendar.DAY_OF_MONTH);
	}

	@Override public int getCurrentTimeMonth() {
		updateTimestamp();
		return calendar.get(Calendar.MONTH);
	}

	@Override public int getDayOfWeek() {
		updateTimestamp();
		return calendar.get(Calendar.DAY_OF_WEEK);//
	}

	@Override public String toString() {
		return String.valueOf(getCurrentTime());
	}
}
