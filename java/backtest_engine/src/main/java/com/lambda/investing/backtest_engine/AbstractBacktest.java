package com.lambda.investing.backtest_engine;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.factor_investing.AbstractFactorInvestingAlgorithm;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorConfiguration;
import com.lambda.investing.connector.ordinary.OrdinaryConnectorPublisherProvider;
import com.lambda.investing.factor_investing_connector.BacktestFactorProvider;
import com.lambda.investing.factor_investing_connector.FactorProvider;
import com.lambda.investing.factor_investing_connector.MockFactorProvider;
import com.lambda.investing.market_data_connector.MarketDataConnectorPublisher;
import com.lambda.investing.market_data_connector.MarketDataProvider;
import com.lambda.investing.market_data_connector.csv_file_reader.CSVFileConfiguration;
import com.lambda.investing.market_data_connector.csv_file_reader.CSVMarketDataConnectorPublisher;
import com.lambda.investing.market_data_connector.ordinary.OrdinaryMarketDataProvider;
import com.lambda.investing.market_data_connector.parquet_file_reader.ParquetFileConfiguration;
import com.lambda.investing.market_data_connector.parquet_file_reader.ParquetMarketDataConnectorPublisher;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.exception.LambdaException;
import com.lambda.investing.trading_engine_connector.TradingEngineConfiguration;
import com.lambda.investing.trading_engine_connector.TradingEngineConnector;
import com.lambda.investing.trading_engine_connector.paper.PaperConnectorPublisher;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngineConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBacktest {

	protected Logger logger = LogManager.getLogger(AbstractBacktest.class);

	protected BacktestConfiguration backtestConfiguration;
	protected MarketDataConnectorPublisher ordinaryMarketDataConnectorPublisher;//from file to zero ordinary...

	protected OrdinaryConnectorConfiguration ordinaryConnectorConfiguration;
	protected OrdinaryConnectorPublisherProvider ordinaryConnectorPublisherProvider;
	protected OrdinaryMarketDataProvider ordinaryMarketDataConnectorProvider;//from file to zero ordinary...

	protected TradingEngineConfiguration tradingEngineConfiguration;
	protected ConnectorConfiguration marketDataConnectorConfiguration;
	protected ConnectorConfiguration tradingEngineConnectorConfiguration;
	protected ConnectorPublisher backtestMarketDataAndExecutionReportPublisher;
	protected ConnectorProvider backtestOrderRequestProvider;

	protected PaperConnectorPublisher paperConnectorMarketDataAndExecutionReportPublisher;//market data and ER publisher
	protected TradingEngineConnector paperTradingEngineConnector;
	protected PaperTradingEngine paperTradingEngine;

	protected MarketDataProvider algorithmMarketDataProvider;

	protected FactorProvider algorithmFactorProvider;
	protected AlgorithmConnectorConfiguration algorithmConnectorConfiguration;

	public AbstractBacktest(BacktestConfiguration backtestConfiguration) throws Exception {
		this.backtestConfiguration = backtestConfiguration;

		//reading file publication and transform to internal market updates
		ordinaryConnectorConfiguration = new OrdinaryConnectorConfiguration();
		ordinaryConnectorPublisherProvider = new OrdinaryConnectorPublisherProvider(
				"OrdinaryConnectorPublisher_MarketDataProvider", 0, Thread.NORM_PRIORITY);
		ordinaryMarketDataConnectorProvider = new OrdinaryMarketDataProvider(ordinaryConnectorPublisherProvider,
				ordinaryConnectorConfiguration);

		//external providers and publishers connectors
		tradingEngineConfiguration = new PaperTradingEngineConfiguration();//empty

	}

	public BacktestConfiguration getBacktestConfiguration() {
		return backtestConfiguration;
	}

	protected void afterConstructor() {
		ordinaryMarketDataConnectorPublisher = getOrdinaryMarketDataConnectorPublisher();//CSVMarketDataConnectorPublisher
		marketDataConnectorConfiguration = getMarketDataConnectorConfiguration();
		tradingEngineConnectorConfiguration = getTradingEngineConnectorConfiguration();
		backtestMarketDataAndExecutionReportPublisher = getBacktestMarketDataAndExecutionReportConnectorPublisher();//MD ER and commands publisher
		backtestOrderRequestProvider = getBacktestOrderRequestProvider();//OrderRequest provider

		//Paper trading
		paperConnectorMarketDataAndExecutionReportPublisher = new PaperConnectorPublisher(
				marketDataConnectorConfiguration, backtestMarketDataAndExecutionReportPublisher);

		constructPaperExecutionReportConnectorPublisher();

		//trading algorithm
		algorithmMarketDataProvider = getAlgorithmMarketDataProvider();

		algorithmConnectorConfiguration = new AlgorithmConnectorConfiguration(paperTradingEngineConnector,
				algorithmMarketDataProvider);

		backtestConfiguration.getAlgorithm().setAlgorithmConnectorConfiguration(algorithmConnectorConfiguration);


		readFiles();
	}

	protected abstract void constructPaperExecutionReportConnectorPublisher();

	protected abstract void readFiles();

	protected List<Instrument> getInstruments() {
		return paperTradingEngine.getInstrumentsList();
	}

	/**
	 * @param mockIt if true weights will be randomly chosen
	 * @return factorProvider if algorithm is a AbstractFactorInvestingAlgorithm type
	 */
	protected FactorProvider getFactorProvider(boolean mockIt) {

		List<Instrument> instrumentList = getInstruments();
		Algorithm backtestAlgorith = backtestConfiguration.getAlgorithm();
		FactorProvider factorProvider = null;
		if (backtestAlgorith instanceof AbstractFactorInvestingAlgorithm) {
			AbstractFactorInvestingAlgorithm factorInvestingAlgorithm = (AbstractFactorInvestingAlgorithm) backtestAlgorith;
			String modelName = factorInvestingAlgorithm.getModelName();

			if (mockIt) {
				//returns a random weight each factorProviderPeriodMs
				long factorProviderPeriodMs = 15000;
				List<String> instrumentPkList = new ArrayList<>();
				for (Instrument instrument : instrumentList) {
					instrumentPkList.add(instrument.getPrimaryKey());
				}
				factorProvider = new MockFactorProvider(getAlgorithmMarketDataProvider(), factorProviderPeriodMs, instrumentPkList);
			} else {
				//read persitance data
				factorProvider = new BacktestFactorProvider(getAlgorithmMarketDataProvider(), instrumentList, modelName, backtestConfiguration.getStartTime(), backtestConfiguration.getEndTime());
			}
		}

		return factorProvider;
	}

	protected abstract MarketDataProvider getAlgorithmMarketDataProvider();

	protected abstract TradingEngineConnector getPaperTradingEngineConnector();

	protected abstract ConnectorProvider getBacktestOrderRequestProvider();

	/**
	 * Method that set the CSVMarketDataConnectorPublisher or next data provider in future
	 *
	 * @return
	 */
	protected MarketDataConnectorPublisher getOrdinaryMarketDataConnectorPublisher() {
		if (backtestConfiguration.getBacktestSource().equals(BacktestSource.csv)) {
			CSVFileConfiguration csvFileConfiguration = new CSVFileConfiguration(backtestConfiguration.getInstruments(),
					backtestConfiguration.getSpeed(), backtestConfiguration.getInitialSleepSeconds(),
					backtestConfiguration.getStartTime(), backtestConfiguration.getEndTime());

			CSVMarketDataConnectorPublisher csvMarketDataConnectorPublisher = new CSVMarketDataConnectorPublisher(
					ordinaryConnectorConfiguration, ordinaryConnectorPublisherProvider, csvFileConfiguration);
			return csvMarketDataConnectorPublisher;
		} else if (backtestConfiguration.getBacktestSource().equals(BacktestSource.parquet)) {

			ParquetFileConfiguration parquetFileConfiguration = new ParquetFileConfiguration(
					backtestConfiguration.getInstruments(), backtestConfiguration.getSpeed(),
					backtestConfiguration.getInitialSleepSeconds(), backtestConfiguration.getStartTime(),
					backtestConfiguration.getEndTime());

			ParquetMarketDataConnectorPublisher parquetMarketDataConnectorPublisher = new ParquetMarketDataConnectorPublisher(
					ordinaryConnectorConfiguration, ordinaryConnectorPublisherProvider, parquetFileConfiguration);
			return parquetMarketDataConnectorPublisher;
		} else {
			logger.error("backtest source not found {} in backtestConfiguration-> return null ",
					backtestConfiguration.getBacktestSource());
			return null;
		}

	}

	protected abstract ConnectorConfiguration getMarketDataConnectorConfiguration();

	protected abstract ConnectorConfiguration getTradingEngineConnectorConfiguration();

	protected abstract ConnectorPublisher getBacktestMarketDataAndExecutionReportConnectorPublisher();

	public void start() throws LambdaException {
		afterConstructor();

		ordinaryMarketDataConnectorPublisher.start();

		ordinaryMarketDataConnectorProvider.init();

		paperTradingEngine.setInstrumentsList(backtestConfiguration.getInstruments());
		paperTradingEngine.setPaperConnectorMarketDataAndExecutionReportPublisher(
				paperConnectorMarketDataAndExecutionReportPublisher);

		FactorProvider factorProvider = getFactorProvider(false);
		if (factorProvider != null) {
			algorithmConnectorConfiguration.setFactorProvider(factorProvider);//adding factorProvider
		}

		paperTradingEngine.init();

		backtestConfiguration.getAlgorithm().init();

	}

	public void stop() {
		ordinaryMarketDataConnectorPublisher.stop();
		backtestConfiguration.getAlgorithm().stop();
	}

	public void reset() {
		backtestConfiguration.getAlgorithm().resetAlgorithm();

	}

}
