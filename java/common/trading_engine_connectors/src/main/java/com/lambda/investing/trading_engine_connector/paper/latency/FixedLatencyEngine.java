package com.lambda.investing.trading_engine_connector.paper.latency;

import java.util.Date;

public class FixedLatencyEngine implements LatencyEngine {

	protected static long THREAD_MS_WAITING = 1;//less than 1 is not possible and can cause some problems on backtest!
	protected long latencyMs;
	protected Date currentTime = new Date(0);
	protected long nextUpdateMs = Long.MIN_VALUE;

	public FixedLatencyEngine(long latencyMs) {
		this.latencyMs = latencyMs;
	}

	@Override public void setTime(Date currentDate) {
		if (currentDate.getTime() > currentTime.getTime()) {
			currentTime = currentDate;
		}
	}

	public Date getCurrentTime() {
		return currentTime;
	}

	@Override public void setNextUpdateMs(long nextUpdateMs) {
		this.nextUpdateMs = nextUpdateMs;
	}

	protected void delayThread(long delayMs) {
		if (delayMs <= 0) {
			return;
		} else if (nextUpdateMs != Long.MIN_VALUE && delayMs < this.nextUpdateMs) {
			return;
		} else {
			Date startTime = currentTime;
			long delayIter = delayMs;
			long lastTimeEval = 0L;
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
				if (THREAD_MS_WAITING > 0) {
					try {
						Thread.sleep(THREAD_MS_WAITING);
					} catch (Exception e) {
						;
					}
				}
			}
		}
	}

	@Override public void delay() {
		delayThread(latencyMs);
	}

}

