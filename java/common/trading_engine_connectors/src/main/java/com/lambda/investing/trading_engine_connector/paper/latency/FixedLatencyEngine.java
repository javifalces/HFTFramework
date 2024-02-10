package com.lambda.investing.trading_engine_connector.paper.latency;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedLatencyEngine implements LatencyEngine {


	protected long latencyMs;
	protected Date currentTime = new Date(0);
	protected long nextUpdateMs = Long.MIN_VALUE;
	protected Logger logger = LogManager.getLogger(FixedLatencyEngine.class);
	protected AtomicInteger counterTimeSet = new AtomicInteger(0);
	protected CountDownLatch latch;
	protected final Object lockLatch = new Object();

	public FixedLatencyEngine(long latencyMs) {
		this.latencyMs = latencyMs;
	}

	@Override public void setTime(Date currentDate) {
		if (currentDate.getTime() > currentTime.getTime()) {
			currentTime = currentDate;
			counterTimeSet.incrementAndGet();
			freeLock();
		}
	}

	public Date getCurrentTime() {
		return currentTime;
	}

	@Override public void setNextUpdateMs(long nextUpdateMs) {
		this.nextUpdateMs = nextUpdateMs;
	}

	protected void delayThread(Date currentDate, long delayMs) {
		if (delayMs <= 0) {
			return;
		} else if (nextUpdateMs != Long.MIN_VALUE && delayMs < this.nextUpdateMs) {
			return;
		} else {
			Date startTime = currentDate;
			long delayIter = delayMs;
			long lastTimeEval = 0L;
			int initialCounter = counterTimeSet.get();
			while (currentTime.getTime() - startTime.getTime() < delayMs) {

				//check next iteration when is it and discount already waited!
				if (nextUpdateMs != Long.MIN_VALUE) {
					if (lastTimeEval != currentTime.getTime()) {
						if (delayIter < this.nextUpdateMs) {
							break;
						}
						delayIter -= nextUpdateMs;
						lastTimeEval = currentTime.getTime();
					}
				}


				synchronized (lockLatch) {
					if (latch == null || latch.getCount() == 0) {
						latch = new CountDownLatch(1);
					}
				}

				try {
					latch.await();
				} catch (Exception e) {
					;
				}


			}
			//end while sleeping
			int finalCounter = counterTimeSet.get();
			int elapsedUpdates = finalCounter - initialCounter;
			long elapsedSleep = (currentTime.getTime() - startTime.getTime());
			if ((elapsedSleep > delayMs * 2 && elapsedUpdates > 2) || elapsedSleep > delayMs * 10) {
				logger.warn("delayThread sleep on {} ms with {} updates from {} to {} ", elapsedSleep, elapsedUpdates,
						startTime, currentTime);
			}

		}
	}

	@Override
	public void delay(Date currentDate) {
		delayThread(currentDate, latencyMs);
	}

	@Override
	public void reset() {
		freeLock();

		currentTime = new Date(0);
		nextUpdateMs = Long.MIN_VALUE;
		counterTimeSet = new AtomicInteger(0);

		synchronized (lockLatch) {
			latch = null;
		}
	}

	@Override
	public void freeLock() {
		synchronized (lockLatch) {
			if (latch != null) {
				latch.countDown();
			}
		}
	}

}

