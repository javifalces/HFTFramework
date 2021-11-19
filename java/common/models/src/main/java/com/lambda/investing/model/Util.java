package com.lambda.investing.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Util {

	public static Gson GSON_STRING = new GsonBuilder()
			.excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.FINAL)
			.serializeSpecialFloatingPointValues().create();
	public static Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

	public static SimpleDateFormat PYTHON_DATAFRAME_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.SSS");////2019-11-09 08:42:24.142302

	public static Date getDateUTC(long timestamp) {
		UTC_CALENDAR.setTimeInMillis(timestamp);
		return UTC_CALENDAR.getTime();
	}

	public static String getDatePythonUTC(long timestamp) {
		UTC_CALENDAR.setTimeInMillis(timestamp);
		Date date = UTC_CALENDAR.getTime();
		return PYTHON_DATAFRAME_FORMAT.format(date);
	}

}
