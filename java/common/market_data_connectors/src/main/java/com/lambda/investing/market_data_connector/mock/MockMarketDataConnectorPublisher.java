package com.lambda.investing.market_data_connector.mock;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.market_data_connector.*;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.trading.Verb;
import org.apache.commons.math3.util.Precision;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static com.lambda.investing.market_data_connector.AbstractMarketDataProvider.GSON;

public class MockMarketDataConnectorPublisher extends AbstractMarketDataConnectorPublisher implements Runnable{

	protected Logger logger = LogManager.getLogger(MockMarketDataConnectorPublisher.class);
	private Thread threadCreatingRandom;
	private long sleepUpdatesMs;
	List<MockMarketDataConfiguration> marketDataConfigurationList;
	Map<MockMarketDataConfiguration,Depth> lastDepthSent;
	Map<MockMarketDataConfiguration,Trade> lastTradeSent;
	Map<MockMarketDataConfiguration,Double> lastMidPrice;
	long seed;
	Random random = new Random();

	int precisionPrice = 3;
	int precisionQty = 2;

	public MockMarketDataConnectorPublisher(ConnectorConfiguration connectorConfiguration,ConnectorPublisher connectorPublisher,long sleepUpdatesMs) {
		super("MockMarketDataConnectorPublisher", connectorConfiguration, connectorPublisher);

		lastDepthSent = new ConcurrentHashMap<>();
		lastTradeSent = new ConcurrentHashMap<>();
		lastMidPrice = new ConcurrentHashMap<>();
		this.sleepUpdatesMs=sleepUpdatesMs;






	}

	public void setMarketDataConfigurationList(List<MockMarketDataConfiguration> marketDataConfigurationList) {
		this.marketDataConfigurationList = marketDataConfigurationList;

		for(MockMarketDataConfiguration marketDataConfiguration:this.marketDataConfigurationList){
			lastDepthSent.put(marketDataConfiguration,createDepth(marketDataConfiguration));
			lastTradeSent.put(marketDataConfiguration,createTrade(marketDataConfiguration));
			lastMidPrice.put(marketDataConfiguration,marketDataConfiguration.getStartMidPrice());
		}
		this.threadCreatingRandom=new Thread(this,"MockMarketDataProvider_runnable");
		this.threadCreatingRandom.start();


	}

	public void setSeed(long seed) {
		this.seed = seed;
		this.random = new Random(this.seed);
	}



	private Depth createDepth(MockMarketDataConfiguration mockMarketDataConfiguration) {
		Depth depth = new Depth();
		depth.setInstrument(mockMarketDataConfiguration.getInstrument().getPrimaryKey());
		depth.setLevels(mockMarketDataConfiguration.getLevels());

		double midPrice = mockMarketDataConfiguration.getStartMidPrice();
		double midQuantity = mockMarketDataConfiguration.getStartMidQuantity();

		Double[] askPrice = new Double[mockMarketDataConfiguration.getLevels()];
		Double[] bidPrice = new Double[mockMarketDataConfiguration.getLevels()];
		Double[] askQuantity = new Double[mockMarketDataConfiguration.getLevels()];
		Double[] bidQuantity = new Double[mockMarketDataConfiguration.getLevels()];


		double prev_bid = midPrice;
		double prev_ask = midPrice;

		for (int levelIterator=0;levelIterator<mockMarketDataConfiguration.getLevels();levelIterator++){
			askPrice[levelIterator] = prev_ask+(mockMarketDataConfiguration.getDelta()*random.nextDouble());
			bidPrice[levelIterator] = prev_bid-(mockMarketDataConfiguration.getDelta()*random.nextDouble());

			askQuantity[levelIterator] = midQuantity+(levelIterator+1)*(mockMarketDataConfiguration.getDelta()*random.nextDouble());
			bidQuantity[levelIterator] = askQuantity[levelIterator]*random.nextDouble();

			askPrice[levelIterator] = Precision.round(askPrice[levelIterator],precisionPrice);
			bidPrice[levelIterator] = Precision.round(bidPrice[levelIterator],precisionPrice);

			askQuantity[levelIterator] = Precision.round(askQuantity[levelIterator],precisionQty);
			bidQuantity[levelIterator] = Precision.round(bidQuantity[levelIterator],precisionQty);

			prev_bid = bidPrice[levelIterator];
			prev_ask=askPrice[levelIterator];

		}
		depth.setAsks(askPrice);
		depth.setBids(bidPrice);
		depth.setAsksQuantities(askQuantity);
		depth.setBidsQuantities(bidQuantity);
		depth.setTimestamp(System.currentTimeMillis());

		return depth;


	}

	private Trade createTrade(MockMarketDataConfiguration mockMarketDataConfiguration) {

		Trade newTrade = new Trade();
		newTrade.setInstrument(mockMarketDataConfiguration.getInstrument().getPrimaryKey());
		newTrade.setQuantity(mockMarketDataConfiguration.getStartMidQuantity());
		newTrade.setPrice(mockMarketDataConfiguration.getStartMidPrice());
		newTrade.setAlgorithmInfo("");
		newTrade.setTimestamp(System.currentTimeMillis());
		newTrade.setVerb(Verb.Buy);
		return newTrade;


	}

	private Trade modifyTrade(MockMarketDataConfiguration mockMarketDataConfiguration, Trade lastTrade) {

		if(random.nextDouble()>0.5){
			lastTrade.setVerb(Verb.Sell);
		}else{
			lastTrade.setVerb(Verb.Buy);
		}

		double quantity = lastTrade.getQuantity()*random.nextDouble();
		quantity = Precision.round(quantity,precisionQty);
		double price = lastMidPrice.getOrDefault(mockMarketDataConfiguration,mockMarketDataConfiguration.getStartMidPrice());
		price = Precision.round(price,precisionPrice);
		lastTrade.setQuantity(quantity);
		lastTrade.setPrice(price);
		lastTrade.setTimestamp(System.currentTimeMillis());
		return lastTrade;
	}

	private Depth modifyDepth(MockMarketDataConfiguration mockMarketDataConfiguration, Depth lastDepth) {

		Double[] askPrice = lastDepth.getAsks();
		Double[] bidPrice = lastDepth.getBids();
		Double[] askQuantity = lastDepth.getAsksQuantities();
		Double[] bidQuantity = lastDepth.getBidsQuantities();
		double midPrice = lastDepth.getMidPrice()*(1+((random.nextDouble()-0.5))/25);
		midPrice = Precision.round(midPrice,precisionPrice);
		double midQuantity = mockMarketDataConfiguration.getStartMidQuantity()*(1+((random.nextDouble()-0.5))/10);
		midQuantity = Precision.round(midQuantity,precisionPrice);
		lastMidPrice.put(mockMarketDataConfiguration,midPrice);

		double prev_bid = midPrice;
		double prev_ask = midPrice;

		for (int levelIterator=0;levelIterator<mockMarketDataConfiguration.getLevels();levelIterator++){
			askPrice[levelIterator] = prev_ask+(mockMarketDataConfiguration.getDelta()*random.nextDouble());
			bidPrice[levelIterator] = prev_bid-(mockMarketDataConfiguration.getDelta()*random.nextDouble());

			askQuantity[levelIterator] = midQuantity+(levelIterator+1)*(mockMarketDataConfiguration.getDelta()*random.nextDouble());
			bidQuantity[levelIterator] = askQuantity[levelIterator]*random.nextDouble();

			askPrice[levelIterator] = Precision.round(askPrice[levelIterator],precisionPrice);
			bidPrice[levelIterator] = Precision.round(bidPrice[levelIterator],precisionPrice);

			askQuantity[levelIterator] = Precision.round(askQuantity[levelIterator],precisionQty);
			bidQuantity[levelIterator] = Precision.round(bidQuantity[levelIterator],precisionQty);

			prev_bid = bidPrice[levelIterator];
			prev_ask=askPrice[levelIterator];

		}
		lastDepth.setTimestamp(System.currentTimeMillis());
		return lastDepth;
	}


	@Override public void run() {
		ZeroMqConfiguration zeroMqConfiguration = (ZeroMqConfiguration)connectorConfiguration;
		for(MockMarketDataConfiguration mockMarketDataConfiguration:lastDepthSent.keySet()) {
			logger.info("Starting publishing {}   in tcp://{}:{}",
					mockMarketDataConfiguration.getInstrument().getPrimaryKey(),zeroMqConfiguration.getHost(),zeroMqConfiguration.getPort());
		}


		while(true){
			if(enable) {

				for(MockMarketDataConfiguration mockMarketDataConfiguration:lastDepthSent.keySet()){
					if (this.random.nextDouble()>mockMarketDataConfiguration.getProbabilityTrade()) {
						//create orderbook depth
						Depth lastDepth = lastDepthSent.getOrDefault(mockMarketDataConfiguration, createDepth(mockMarketDataConfiguration));
						Depth newDepth = modifyDepth(mockMarketDataConfiguration, lastDepth);


						//publish Depth//
						String topic = mockMarketDataConfiguration.getInstrument().getPrimaryKey();
						notifyDepth(topic,newDepth);
						lastDepthSent.put(mockMarketDataConfiguration, newDepth);
					}
					else{
						//create last trade
						Trade lastTrade = lastTradeSent.getOrDefault(mockMarketDataConfiguration, createTrade(mockMarketDataConfiguration));
						Trade newTrade = modifyTrade(mockMarketDataConfiguration, lastTrade);

						String topic = mockMarketDataConfiguration.getInstrument().getPrimaryKey();
						notifyTrade(topic,newTrade);
						lastTradeSent.put(mockMarketDataConfiguration, newTrade);
					}



				}




			}


			try {
				Thread.sleep(sleepUpdatesMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	@Override public void init() {

	}
}
