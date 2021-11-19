package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.portfolio.Portfolio;
import com.lambda.investing.model.portfolio.PortfolioInstrument;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.Verb;
import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.api.*;

import java.io.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.api.BubblePlot;
import tech.tablesaw.plotly.api.TimeSeriesPlot;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.selection.Selection;

import static com.lambda.investing.algorithmic_trading.PnlSnapshot.UPDATE_HISTORICAL_LOCK;

public class PortfolioManager {

	private static boolean TRADES_CSV_COMPLETE = false;
	private Logger logger = LogManager.getLogger(PortfolioManager.class);
	private Algorithm algorithm;
	//	private Map<String, List<ExecutionReport>> instrumentToExecutionReportsFilled;
	protected Map<String, PnlSnapshot> instrumentPnlSnapshotMap;
	private Map<String, Map<String, Double>> customColumns;
	private Set<String> customColumnsKeys;
	private boolean isBacktest;
	private boolean isPaper;
	private Portfolio portfolio;

	public long numberOfTrades = 0;

	public PortfolioManager(Algorithm algorithm) {
		this.algorithm = algorithm;
		this.isBacktest = this.algorithm.isBacktest;
		this.isPaper = this.algorithm.isPaper;
		reset();
	}

	public PnlSnapshot getLastPnlSnapshot(String instrumentPk) {
		return instrumentPnlSnapshotMap.get(instrumentPk);

	}

	public void setPortfolio(Portfolio portfolio) {
		//set initial values
		for (String instrumentPk : portfolio.getPortfolioInstruments().keySet()) {
			PortfolioInstrument portfolioInstrument = portfolio.getPortfolioInstruments().get(instrumentPk);
			PnlSnapshot pnlSnapshot = instrumentPnlSnapshotMap.getOrDefault(instrumentPk, new PnlSnapshotOrders());
			pnlSnapshot.setNetPosition(portfolioInstrument.getPosition());
			pnlSnapshot.setAlgorithmInfo(this.algorithm.algorithmInfo);
			pnlSnapshot.setNumberOfTrades((int) portfolioInstrument.getNumberTrades());
			String msg = String
					.format("setting initial portfolio %s : position:%.2f   numberTrades:%.2f ", instrumentPk,
							portfolioInstrument.getPosition(), portfolioInstrument.getNumberTrades());
			System.out.println(msg);
			logger.info(msg);
		}

	}

	public void reset() {
		//		instrumentToExecutionReportsFilled = new ConcurrentHashMap<>();
		instrumentPnlSnapshotMap = new ConcurrentHashMap<>();
		customColumns = new ConcurrentHashMap<>();
		customColumnsKeys = new HashSet<>();
	}

	public void updateDepth(Depth depth) {
		PnlSnapshot pnlSnapshot = instrumentPnlSnapshotMap.getOrDefault(depth.getInstrument(), new PnlSnapshotOrders());
		pnlSnapshot.setBacktest(isBacktest);
		pnlSnapshot.setPaper(isPaper);
		pnlSnapshot.updateDepth(depth);
		instrumentPnlSnapshotMap.put(depth.getInstrument(), pnlSnapshot);

		updateCustomHistoricals(depth.getInstrument(), depth.getTimestamp(), pnlSnapshot);
	}

	public void addCurrentCustomColumn(String instrument, String key, Double value) {
		Map<String, Double> customColumnsInstrument = customColumns.getOrDefault(instrument, new HashMap<>());
		customColumnsInstrument.put(key, value);
		customColumns.put(instrument, customColumnsInstrument);
		customColumnsKeys.add(key);
	}

	private void updateCustomHistoricals(String instrumentPk, long timestamp, PnlSnapshot pnlSnapshot) {

		//check to update customColumns
		Map<String, Double> customColumnsInstrument = customColumns.get(instrumentPk);
		if (customColumnsInstrument != null) {
			for (Map.Entry<String, Double> entry : customColumnsInstrument.entrySet()) {
				pnlSnapshot.updateHistoricalsCustom(timestamp, entry.getKey(), entry.getValue());
			}
		}

	}

	public PnlSnapshot addTrade(ExecutionReport executionReport) {
		//		List<ExecutionReport> executionReportList = instrumentToExecutionReportsFilled
		//				.getOrDefault(executionReport.getInstrument(), new ArrayList<>());
		//		executionReportList.add(executionReport);
		//		instrumentToExecutionReportsFilled.put(executionReport.getInstrument(), executionReportList);

		PnlSnapshot pnlSnapshot = instrumentPnlSnapshotMap
				.getOrDefault(executionReport.getInstrument(), new PnlSnapshotOrders());
		pnlSnapshot.setBacktest(isBacktest);
		pnlSnapshot.setPaper(isPaper);
		pnlSnapshot.setAlgorithmInfo(executionReport.getAlgorithmInfo());
		pnlSnapshot.updateExecutionReport(executionReport);

		updateCustomHistoricals(executionReport.getInstrument(), executionReport.getTimestampCreation(), pnlSnapshot);

		instrumentPnlSnapshotMap.put(executionReport.getInstrument(), pnlSnapshot);
		numberOfTrades++;

		return pnlSnapshot;
	}

	public String summary(Instrument instrument) {
		PnlSnapshot pnlSnapshot = instrumentPnlSnapshotMap.get(instrument.getPrimaryKey());
		if (pnlSnapshot == null) {
			logger.info("No pnl in {}", instrument.getPrimaryKey());
			return "";
		}
		String output = String
				.format("\n\ttrades:%d  position:%.3f totalPnl:%.3f\n\trealizedPnl:%.3f\n\tunrealizedPnl:%.3f",
						pnlSnapshot.numberOfTrades.get(), pnlSnapshot.netPosition, pnlSnapshot.totalPnl,
						pnlSnapshot.realizedPnl, pnlSnapshot.unrealizedPnl);

		logger.info("\n\ttrades:{}  position:{} totalPnl:{}\n\trealizedPnl:{}\n\tunrealizedPnl:{}",
				pnlSnapshot.numberOfTrades, pnlSnapshot.netPosition, pnlSnapshot.totalPnl, pnlSnapshot.realizedPnl,
				pnlSnapshot.unrealizedPnl);

		return output;

	}

	private Double[] fromList(List<Double> input) {
		Double[] output = new Double[input.size()];
		return input.toArray(output);
	}

	private String[] fromStrList(List<String> input) {
		String[] output = new String[input.size()];
		return input.toArray(output);
	}

	public void plotHistorical(Instrument instrument) {
		Map<Instrument, Table> tradesTable = getTradesTable(null);
		Table tradeTable = tradesTable.get(instrument);
		if (tradeTable == null || tradeTable.rowCount() <= 0) {
			//nothing saved here

			return;
		}
		PnlSnapshot pnlSnapshot = instrumentPnlSnapshotMap.get(instrument.getPrimaryKey());

		//		Table historicalPnl = tradeTable.select("timestamp","historicalRealizedPnl","historicalUnrealizedPnl","historicalTotalPnl");

		String title = String.format("%s totalPnl:%.3f  realizedPnl:%.3f  unrealizedPnl:%.3f", algorithm.algorithmInfo,
				pnlSnapshot.totalPnl, pnlSnapshot.realizedPnl, pnlSnapshot.unrealizedPnl);
		Figure figureRealizedPnl = TimeSeriesPlot.create("Pnl " + title, tradeTable, // table name
				"date", // x variable column name
				"historicalTotalPnl" // y variable column name
		);

		//		Table historicalPosition = tradeTable.select("timestamp","netPosition");
		String title2 = String
				.format("%s position:%.3f   numberOfTrades:%d", algorithm.algorithmInfo, pnlSnapshot.netPosition,
						pnlSnapshot.numberOfTrades.get());
		Figure figureRPosition = TimeSeriesPlot.create("Position " + title2, tradeTable, // table name
				"date", // x variable column name
				"netPosition" // y variable column name
		);
		File htmlFilePnl = new File(instrument.getPrimaryKey() + "_" + algorithm.getAlgorithmInfo() + "_pnl.html");
		File htmlFilePosition = new File(
				instrument.getPrimaryKey() + "_" + algorithm.getAlgorithmInfo() + "_position.html");
		Plot.show(figureRealizedPnl, instrument.getPrimaryKey(), htmlFilePnl);
		Plot.show(figureRPosition, instrument.getPrimaryKey(), htmlFilePosition);
		//		try {
		//			Scanner myReader = new Scanner(htmlFilePnl);
		//			StringBuilder buffer = new StringBuilder();
		//			while (myReader.hasNext()) {
		//				buffer.append(myReader.next());
		//			}
		//			myReader.close();
		//
		//			myReader = new Scanner(htmlFilePosition);
		//			buffer.append("\n\n");
		//			while (myReader.hasNext()) {
		//				buffer.append(myReader.next());
		//			}
		//			myReader.close();
		//
		//			BufferedWriter writer = new BufferedWriter(new FileWriter(htmlFilePnl));
		//			writer.write(buffer.toString());
		//			writer.close();
		//
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}

	}

	public synchronized Map<Instrument, Table> getTradesTable(String basePath) {
		synchronized (UPDATE_HISTORICAL_LOCK) {
			Map<Instrument, Table> output = new ConcurrentHashMap<>();
			try {
				for (String instrumentPk : instrumentPnlSnapshotMap.keySet()) {
					Instrument instrument = Instrument.getInstrument(instrumentPk);
					//			summary(instrument);
					PnlSnapshot pnlSnapshot = instrumentPnlSnapshotMap.get(instrumentPk);

					if (numberOfTrades == 0) {
						logger.warn("no trades detected!");
						return output;
					}
					List<Long> timestamp = new ArrayList<>(pnlSnapshot.historicalAvgOpenPrice.keySet());
					List<Double> netPosition = new ArrayList<>(pnlSnapshot.historicalNetPosition.values());
					List<Double> avgOpenPrice = new ArrayList<>(pnlSnapshot.historicalAvgOpenPrice.values());
					List<Double> netInvestment = new ArrayList<>(pnlSnapshot.historicalNetInvestment.values());
					List<Double> historicalRealizedPnl = new ArrayList<>(pnlSnapshot.historicalRealizedPnl.values());
					List<Double> historicalUnrealizedPnl = new ArrayList<>(
							pnlSnapshot.historicalUnrealizedPnl.values());
					List<Double> historicalTotalPnl = new ArrayList<>(pnlSnapshot.historicalTotalPnl.values());
					List<Double> historicalPrice = new ArrayList<>(pnlSnapshot.historicalPrice.values());
					List<Double> historicalQuantity = new ArrayList<>(pnlSnapshot.historicalQuantity.values());
					List<String> historicalAlgorithmInfo = new ArrayList<>(
							pnlSnapshot.historicalAlgorithmInfo.values());
					List<Integer> numberTrades = new ArrayList<>(pnlSnapshot.historicalNumberOfTrades.values());
					List<String> historicalVerb = new ArrayList<>(pnlSnapshot.historicalVerb.values());
					List<String> historicalClOrdId = new ArrayList<>(pnlSnapshot.historicalClOrdId.values());

					//timestamp conversion
					Long[] timestampArr = new Long[timestamp.size()];
					timestampArr = timestamp.toArray(timestampArr);
					LocalDateTime[] dates = new LocalDateTime[timestamp.size()];
					int index = 0;
					for (Long timestam : timestamp) {
						LocalDateTime date = LocalDateTime
								.ofInstant(Instant.ofEpochMilli(timestam), TimeService.DEFAULT_ZONEID);
						dates[index] = date;
						index++;
					}

					logger.info(
							"getTradesTable has {} rows -> timestamp:{} verb:{} algorithmInfo:{} price:{} quantity:{} netPosition:{} avgOpenPrice:{} netInvestment:{} historicalRealizedPnl:{} historicalUnrealizedPnl:{} historicalTotalPnl:{} numberTrades:{}",
							dates.length, timestamp.size(), historicalVerb.size(), historicalAlgorithmInfo.size(),
							historicalPrice.size(), historicalQuantity.size(), netPosition.size(), avgOpenPrice.size(),
							netInvestment.size(), historicalRealizedPnl.size(), historicalUnrealizedPnl.size(),
							historicalTotalPnl.size(), numberTrades.size());

					LongColumn timestampColumn = LongColumn.create("timestamp", ArrayUtils.toPrimitive(timestampArr));
					DateTimeColumn dateTimeColumn = DateTimeColumn.create("date", dates);
					StringColumn verbColumn = StringColumn.create("verb", historicalVerb);
					StringColumn algoInfoColumn = StringColumn
							.create("algorithmInfo", fromStrList(historicalAlgorithmInfo));
					StringColumn clOrdIdColumn = StringColumn.create("clientOrderId", fromStrList(historicalClOrdId));
					DoubleColumn priceColumn = DoubleColumn.create("price", fromList(historicalPrice));
					DoubleColumn quantityColumn = DoubleColumn.create("quantity", fromList(historicalQuantity));
					DoubleColumn netPositionColumn = DoubleColumn.create("netPosition", fromList(netPosition));
					DoubleColumn avgOpenPriceColumn = DoubleColumn.create("avgOpenPrice", fromList(avgOpenPrice));
					DoubleColumn netInvestmentColumn = DoubleColumn.create("netInvestment", fromList(netInvestment));
					DoubleColumn historicalRealizedPnlColumn = DoubleColumn
							.create("historicalRealizedPnl", fromList(historicalRealizedPnl));
					DoubleColumn historicalUnrealizedPnltColumn = DoubleColumn
							.create("historicalUnrealizedPnl", fromList(historicalUnrealizedPnl));
					DoubleColumn historicalTotalPnlColumn = DoubleColumn
							.create("historicalTotalPnl", fromList(historicalTotalPnl));

					Integer[] numberTradesArr = new Integer[numberTrades.size()];
					numberTradesArr = numberTrades.toArray(numberTradesArr);
					IntColumn numberTradesColumn = IntColumn.create("numberTrades", numberTradesArr);

					Table output1 = Table.create(algorithm.algorithmInfo);
					output1 = output1
							.addColumns(timestampColumn, dateTimeColumn, clOrdIdColumn, verbColumn, priceColumn,
									quantityColumn, netPositionColumn, avgOpenPriceColumn, netInvestmentColumn,
									historicalRealizedPnlColumn, historicalUnrealizedPnltColumn,
									historicalTotalPnlColumn, numberTradesColumn, algoInfoColumn);

					//add custom columns
					if (customColumnsKeys.size() > 0) {
						ffillHistoricalCustomColumns(pnlSnapshot);
						Map<Long, List<CustomColumn>> historicalsCustoms = pnlSnapshot.historicalCustomColumns;

						int size = 0;
						for (String customKey : customColumnsKeys) {
							try {
								List<Double> customDouble = new ArrayList<>();
								for (List<CustomColumn> customColumn : historicalsCustoms.values()) {
									for (CustomColumn customColumn1 : customColumn) {
										if (customKey.equalsIgnoreCase(customColumn1.getKey())) {
											customDouble.add(customColumn1.getValue());
										}
									}
								}
								size = customDouble.size();
								DoubleColumn customColumn = DoubleColumn.create(customKey, fromList(customDouble));
								output1 = output1.addColumns(customColumn);
							} catch (Exception e) {
								logger.error("error adding custom column {} of len {}  to trades of len {}-> skip it",
										customKey, size, output1.rowCount(), e);
							}
						}
					}

					// filtered! to reduce noise

					output1 = output1.sortAscendingOn(dateTimeColumn.name());
					if (!TRADES_CSV_COMPLETE) {
						///only returns rows on trades
						IntColumn numberTradesSorted = output1.intColumn("numberTrades");
						Table output2 = output1.where(numberTradesSorted.difference().isNotEqualTo(0.0));//
						//			output2= firstRow.append(output2);
						//			double lastUnrealizedPnl = historicalUnrealizedPnl.get(historicalUnrealizedPnl.size() - 1);
						//			double lastRealizedPnl = historicalRealizedPnl.get(historicalRealizedPnl.size() - 1);
						//			if (lastUnrealizedPnl != 0.0) {
						//				Row lastRow = output2.row(output2.rowCount() - 1);
						//				lastRow.setDouble("historicalUnrealizedPnl", lastUnrealizedPnl);
						//				lastRow.setDouble("historicalRealizedPnl", lastRealizedPnl);
						//				double lastTotalPnl = lastRealizedPnl + lastUnrealizedPnl;
						//				lastRow.setDouble("historicalTotalPnl", lastTotalPnl);
						//			}
						output1 = output2;
						output1 = output1.sortAscendingOn(dateTimeColumn.name());
					}
					//filtere the trades
					if (basePath != null) {
						String filename = basePath + "_" + instrumentPk + ".csv";
						try {
							output1.write().csv(filename);
						} catch (IOException e) {
							logger.error("cant save tradestable to {} ", filename, e);
						}
					}
					output.put(instrument, output1);
				}
			} catch (Exception e) {
				System.err.println("Error getTradesTable in PortfolioManager , return empty!!! ");
				e.printStackTrace();
				logger.error("Error getTradesTable in PortfolioManager , return empty", e);
			}
			return output;
		}
	}

	private void ffillHistoricalCustomColumns(PnlSnapshot pnlSnapshot) {
		Map<Long, List<CustomColumn>> historicalsCustoms = pnlSnapshot.historicalCustomColumns;
		if (historicalsCustoms.size() < pnlSnapshot.historicalNetInvestment.size()) {
			//ffilll customs
			Long[] timestampsCustoms = historicalsCustoms.keySet().toArray(new Long[historicalsCustoms.size()]);
			Long[] timestampsOriginals = pnlSnapshot.historicalNetInvestment.keySet()
					.toArray(new Long[pnlSnapshot.historicalNetInvestment.size()]);
			List<CustomColumn> lastRowToFFILL = historicalsCustoms.get(timestampsCustoms[0]);
			for (Long originalTs : timestampsOriginals) {
				if (!historicalsCustoms.containsKey(originalTs)) {
					historicalsCustoms
							.put(originalTs, lastRowToFFILL);/////fffill the last row to the last timestamp row
				} else {
					lastRowToFFILL = historicalsCustoms.get(originalTs);
				}

			}

		}
	}
}
