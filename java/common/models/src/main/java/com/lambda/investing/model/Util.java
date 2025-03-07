package com.lambda.investing.model;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.filter.SimplePropertyPreFilter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Util {
	public static Gson GSON = new GsonBuilder()
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

	//FastJson configuration

//	public static Filter filter = new SimplePropertyPreFilter() {
//		@Override
//		public boolean process(JSONWriter writer, Object object, String name) {
//			try {
//				java.lang.reflect.Field field = object.getClass().getDeclaredField(name);
//				int modifiers = field.getModifiers();
//				if (java.lang.reflect.Modifier.isStatic(modifiers) ||
//						java.lang.reflect.Modifier.isTransient(modifiers) ||
//						java.lang.reflect.Modifier.isVolatile(modifiers) ||
//						java.lang.reflect.Modifier.isFinal(modifiers)
//				) {
//					return false;
//				}
//				return true;
//			} catch (NoSuchFieldException e) {
//				// Field does not exist, ignore
//			}
//			return false;
//		}
//	};
//
//

	public static String toJsonString(Object object) {
		//return json string from object using Gson
//		return GSON.toJson(object);
		return JSON.toJSONString(object);
	}

	public static <T> T fromJsonString(String jsonString, Class<T> clazz) {
		//return object from json string using Gson
//		return GSON.fromJson(jsonString, clazz);
		return JSON.parseObject(jsonString, clazz);
	}

	public static String toJsonStringGSON(Object object) {
		//return json string from object using Gson
		return GSON.toJson(object);
	}

	public static <T> T fromJsonStringGSON(String jsonString, Class<T> clazz) {
		//return object from json string using Gson
		return GSON.fromJson(jsonString, clazz);
	}



}
