package com.lambda.investing.connector.ordinary.thread_pool;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.junit.Assert;

public class ThreadPoolExecutorChannelsTest {
	ThreadPoolExecutorChannels executor;

	@Test
	public void testThrottling() {
		ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("name-%d").build();
		ThreadPoolExecutorChannels ex = new ThreadPoolExecutorChannels(null, 2, 2, 60, TimeUnit.SECONDS
				, new LinkedBlockingQueue<Runnable>(), namedThreadFactory, true);
		// Expected execution order
		List<String> expectedAAPL = new LinkedList<>();
		expectedAAPL.add("+AAPL1");
		expectedAAPL.add("-AAPL1");
		expectedAAPL.add("+AAPL3");
		expectedAAPL.add("-AAPL3");
		List<String> expectedTSLA = new LinkedList<>();
		expectedTSLA.add("+TSLA1");
		expectedTSLA.add("-TSLA1");
		expectedTSLA.add("+TSLA5");
		expectedTSLA.add("-TSLA5");

		// Request execution
		List<String> actualAAPL = new LinkedList<>();
		List<String> actualTSLA = new LinkedList<>();
		ex.execute(new MyRunnable("AAPL1", 2, actualAAPL), "AAPL");
		sleep(0.5);
		ex.execute(new MyRunnable("TSLA1", 1, actualTSLA), "TSLA");
		sleep(0.5);
		ex.execute(new MyRunnable("TSLA2", 1, actualTSLA), "TSLA");
		ex.execute(new MyRunnable("TSLA3", 1, actualTSLA), "TSLA");
		ex.execute(new MyRunnable("TSLA4", 1, actualTSLA), "TSLA");
		ex.execute(new MyRunnable("TSLA5", 1, actualTSLA), "TSLA");
		ex.execute(new MyRunnable("AAPL2", 2, actualAAPL), "AAPL");//its not processed
		ex.execute(new MyRunnable("AAPL3", 2, actualAAPL), "AAPL");

		// Assert execution order
		sleep(8);
		Assert.assertEquals(expectedAAPL, actualAAPL);
		Assert.assertEquals(expectedTSLA, actualTSLA);
	}

	@Test
	public void testNoThrottling() {
		ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("executor-%d").build();
		ThreadPoolExecutorChannels ex = new ThreadPoolExecutorChannels(null, 2, 2, 60, TimeUnit.SECONDS
				, new LinkedBlockingQueue<Runnable>(), namedThreadFactory, false);
		// Expected execution order
		List<String> expectedAAPL = new LinkedList<>();
		expectedAAPL.add("+AAPL1");
		expectedAAPL.add("-AAPL1");
		expectedAAPL.add("+AAPL2");
		expectedAAPL.add("-AAPL2");
		expectedAAPL.add("+AAPL3");
		expectedAAPL.add("-AAPL3");
		List<String> expectedMSFT = new LinkedList<>();
		expectedMSFT.add("+MSFT1");
		expectedMSFT.add("-MSFT1");
		expectedMSFT.add("+MSFT2");
		expectedMSFT.add("-MSFT2");
		expectedMSFT.add("+MSFT3");
		expectedMSFT.add("-MSFT3");
		expectedMSFT.add("+MSFT4");
		expectedMSFT.add("-MSFT4");
		expectedMSFT.add("+MSFT5");
		expectedMSFT.add("-MSFT5");

		// Request execution
		List<String> actualAAPL = new LinkedList<>();
		List<String> actualMSFT = new LinkedList<>();
		ex.execute(new MyRunnable("AAPL1", 2, actualAAPL), "AAPL");
		sleep(0.5);
		ex.execute(new MyRunnable("MSFT1", 1, actualMSFT), "MSFT");
		sleep(0.5);
		ex.execute(new MyRunnable("MSFT2", 1, actualMSFT), "MSFT");
		ex.execute(new MyRunnable("MSFT3", 1, actualMSFT), "MSFT");
		ex.execute(new MyRunnable("MSFT4", 1, actualMSFT), "MSFT");
		ex.execute(new MyRunnable("MSFT5", 1, actualMSFT), "MSFT");
		ex.execute(new MyRunnable("AAPL2", 2, actualAAPL), "AAPL");//is processed in same order
		ex.execute(new MyRunnable("AAPL3", 2, actualAAPL), "AAPL");

		// AAPLssert execution order
		sleep(8);
		Assert.assertEquals(expectedAAPL, actualAAPL);
		Assert.assertEquals(expectedMSFT, actualMSFT);
	}

	protected static void sleep(double delay) {
		try {
			Thread.sleep((int) (delay * 100));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	private static class MyRunnable implements Runnable {
		private final String id;
		private final double delay;
		private final List<String> result;

		public MyRunnable(String id, double delay, List<String> result) {
			this.id = id;
			this.delay = delay;
			this.result = result;
		}

		@Override
		public void run() {
			result.add("+" + this.id);
			sleep(this.delay);
			result.add("-" + this.id);
		}

	}

}
