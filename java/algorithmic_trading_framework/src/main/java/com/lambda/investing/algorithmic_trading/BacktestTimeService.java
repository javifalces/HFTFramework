package com.lambda.investing.algorithmic_trading;

public class BacktestTimeService extends TimeService {

	long startMs = 0;
	public BacktestTimeService(String timezone) {
		super(timezone);
	}

	public BacktestTimeService() {
		super();
	}

	@Override public void sleepMs(long msToSleep) throws InterruptedException {

		startMs = this.currentTimestamp;
		while (this.currentTimestamp - startMs > msToSleep) {
			Thread.sleep(1);//waiting until simulated ms to sleep are done
		}
	}

}
