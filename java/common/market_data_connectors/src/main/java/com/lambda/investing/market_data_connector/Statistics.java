package com.lambda.investing.market_data_connector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Statistics implements Runnable{

	private static boolean RESET_STATISTICS_PER_UPDATE = true;
	private long sleepMs;
	private boolean enable;
	private Map<String, Long> topicToCounter;
	private Map<String, Long> topicToTotalCounter;
	private String header;
	protected Logger logger = LogManager.getLogger(Statistics.class);

	public Statistics(String header,long sleepMs) {
		this.header=header;
		this.sleepMs = sleepMs;
		topicToCounter = new ConcurrentHashMap<>();
		topicToTotalCounter = new ConcurrentHashMap<>();
		enable = true;
		if (sleepMs > 0) {
			Thread thread = new Thread(this, "Statistics");
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
	}

	public void addStatistics(String topic){
		long counter = topicToCounter.getOrDefault(topic,0L);
		long newCounter=counter+1;
		topicToCounter.put(topic, newCounter);
		topicToTotalCounter.put(topic, topicToTotalCounter.getOrDefault(topic, 0L) + 1);
	}

	public void setStatistics(String topic, long counter) {
		topicToCounter.put(topic, counter);
		topicToTotalCounter.put(topic, counter);
	}

	private void printCurrentStatistics() {
		if (topicToCounter.size()>0) {
			logger.info("******** {} ********", header);
			for (Map.Entry<String, Long> entry : topicToCounter.entrySet()) {
				long totalCounter = topicToTotalCounter.getOrDefault(entry.getKey(), 0L);
				logger.info("\t{}:\t{}\ttotal:{}", entry.getKey(), entry.getValue(), totalCounter);
			}
			logger.info("****************");

			if (RESET_STATISTICS_PER_UPDATE) {
				topicToCounter.clear();
			}

		}
	}


	@Override public void run() {

		while(enable){


			printCurrentStatistics();

			try {
				Thread.sleep(this.sleepMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}


}
