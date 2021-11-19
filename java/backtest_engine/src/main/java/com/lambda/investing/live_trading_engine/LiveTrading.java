package com.lambda.investing.live_trading_engine;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.SingleInstrumentAlgorithm;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.trading_engine_connector.AbstractBrokerTradingEngine;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class LiveTrading {

	protected Logger logger = LogManager.getLogger(LiveTrading.class);
	private MarketDataProvider marketDataProvider;
	private TradingEngineConnector tradingEngineConnector;
	private AlgorithmConnectorConfiguration algorithmConnectorConfiguration;
	private boolean paperTrading = false;
	private boolean demoTrading = false;

	private Algorithm algorithm;
	private List<Instrument> instrumentList;

	public LiveTrading(AlgorithmConnectorConfiguration algorithmConnectorConfiguration) {
		this.algorithmConnectorConfiguration = algorithmConnectorConfiguration;
		this.marketDataProvider = this.algorithmConnectorConfiguration.getMarketDataProvider();
		this.tradingEngineConnector = this.algorithmConnectorConfiguration.getTradingEngineConnector();
	}

	/**
	 * Set to use in paper trading
	 *
	 * @param instrumentList
	 */
	public void setInstrumentList(List<Instrument> instrumentList) {
		this.instrumentList = instrumentList;
	}

	public void setPaperTrading(boolean paperTrading) throws Exception {

		this.paperTrading = paperTrading;
		if (this.paperTrading) {
			if (instrumentList == null || instrumentList.size() == 0) {
				throw new Exception("if you want paperTrading set a instrumentList before!");
			}
			if (tradingEngineConnector instanceof AbstractBrokerTradingEngine) {
				AbstractBrokerTradingEngine tradingEngine = (AbstractBrokerTradingEngine) tradingEngineConnector;
				tradingEngine.setPaperTrading(this.marketDataProvider);
				tradingEngine.setInstrumentList(instrumentList);
				//set algorithm trading engine to paper trading
				PaperTradingEngine paperTradingEngine = tradingEngine.getPaperTradingEngine();
				this.algorithmConnectorConfiguration.setTradingEngineConnector(paperTradingEngine);

				//marketDataProvider
				this.algorithmConnectorConfiguration
						.setMarketDataProvider(paperTradingEngine.getMarketDataProviderIn());
				this.algorithm.setAlgorithmConnectorConfiguration(this.algorithmConnectorConfiguration);
			} else {
				logger.error("can't set paper trader on tradingEngine is not instanceof AbstractBrokerTradingEngine");
				this.paperTrading = false;
			}
		}
	}

	public void setDemoTrading(boolean demoTrading) {
		this.demoTrading = demoTrading;
		if (this.demoTrading) {
			if (tradingEngineConnector instanceof AbstractBrokerTradingEngine) {
				AbstractBrokerTradingEngine tradingEngine = (AbstractBrokerTradingEngine) tradingEngineConnector;
				tradingEngine.setDemoTrading();
			} else {
				logger.error("can't set demo on tradingEngine is not instanceof AbstractBrokerTradingEngine");
				this.demoTrading = false;
			}
		}

	}

	public void setAlgorithm(Algorithm algorithm) {
		this.algorithm = algorithm;
		if (instrumentList != null && instrumentList.size() > 0
				&& this.algorithm instanceof SingleInstrumentAlgorithm) {
			Instrument single = ((SingleInstrumentAlgorithm) algorithm).getInstrument();
			if (!instrumentList.contains(single)) {
				//just in case add one
				logger.warn(
						"{} {} not detected in instrument list for paper trading -> set the first one of the list {}",
						algorithm.getAlgorithmInfo(), single.getPrimaryKey(), instrumentList.get(0).getPrimaryKey());
				((SingleInstrumentAlgorithm) this.algorithm).setInstrument(instrumentList.get(0));
			}
		}

		if (this.paperTrading) {
			this.algorithmConnectorConfiguration.setTradingEngineConnector(this.tradingEngineConnector);
		}
		this.algorithm.setAlgorithmConnectorConfiguration(this.algorithmConnectorConfiguration);

	}

	public void init() {
		this.algorithm.setAlgorithmConnectorConfiguration(this.algorithmConnectorConfiguration);
		this.algorithm.init();
	}
}
