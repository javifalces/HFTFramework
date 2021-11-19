package com.lambda.investing.connector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ThreadUtils {

	private static final Logger logger = LogManager.getLogger();
	private static final Map<Long, Timer> timers = new HashMap<>();

	private static long counter = 0;

	public static long execute(final String name, final Runnable runnable) {
		return schedule(name, runnable, 0);
	}

	public static long schedule(final String name, final Runnable runnable, long delayMs) {
		Timer timer = new Timer(name);
		TimerTask timerTask = getTimerTask(name, runnable);
		timer.schedule(timerTask, delayMs);
		long id = store(timer);
		return id;
	}

	public static long schedule(final String name, final Runnable runnable, long delayMs, long periodMs) {
		Timer timer = new Timer(name);
		TimerTask timerTask = getTimerTask(name, runnable);
		timer.schedule(timerTask, delayMs, periodMs);
		long id = store(timer);
		return id;
	}

	public synchronized static void cancel(long id) {
		Timer timer = timers.get(id);
		if (timer != null) {
			timer.cancel();
			timer.purge();
		}
	}

	private static TimerTask getTimerTask(final String name, final Runnable runnable) {
		return new TimerTask() {

			@Override public void run() {

				try {
					runnable.run();
				} catch (Throwable t) {
					logger.error("Throwable caught during execution of thread " + name, t);
				}
			}
		};
	}

	private synchronized static long store(Timer timer) {
		long id = counter;
		timers.put(id, timer);
		counter++;
		return id;
	}

}
