package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.portfolio.Portfolio;
import com.lambda.investing.model.portfolio.PortfolioInstrument;
import com.lambda.investing.model.trading.ExecutionReport;
import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.*;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.api.TimeSeriesPlot;
import tech.tablesaw.plotly.components.Figure;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

	protected Map<String, String> linkCustomPk;
	protected Map<String, String> linkCustomPkInversed;

	public PortfolioManager(Algorithm algorithm) {
		this.algorithm = algorithm;
		this.isBacktest = this.algorithm.isBacktest;
		this.isPaper = this.algorithm.isPaper;
		reset();
	}

	public PnlSnapshot getLastPnlSnapshot(String instrumentPk) {
		String key = linkCustomPk.getOrDefault(instrumentPk, instrumentPk);
		return instrumentPnlSnapshotMap.get(key);
	}

	public void linkInstruments(String instrumentPk, String customInstrumentPk) {
		//TODO fix end result is not working very fine
		linkCustomPk.put(instrumentPk, customInstrumentPk);
		linkCustomPkInversed.put(customInstrumentPk, instrumentPk);
	}

	public static Table MERGE_TABLES(Map<Instrument, Table> input) {
		Table output = null;
		for (Instrument instrument : input.keySet()) {

			Table table = input.get(instrument);
			if (output == null) {
				output = table;
			} else {
				output = output.append(table);
			}

		}
		return output.sortAscendingOn("date");
	}

	public String getInstrumentKey(String intrumentPk) {
		String key = linkCustomPk.getOrDefault(intrumentPk, intrumentPk);
		return key;
	}

	public String getInstrumentPK(String keyInstrument) {
		String key = linkCustomPkInversed.getOrDefault(keyInstrument, keyInstrument);
		return key;
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
		linkCustomPk = new ConcurrentHashMap<>();
		linkCustomPkInversed = new ConcurrentHashMap<>();
		numberOfTrades = 0;
	}

	public void updateDepth(Depth depth) {
		String key = getInstrumentKey(depth.getInstrument());
		PnlSnapshot pnlSnapshot = instrumentPnlSnapshotMap.getOrDefault(key, new PnlSnapshotOrders());
		pnlSnapshot.setBacktest(isBacktest);
		pnlSnapshot.setPaper(isPaper);
		pnlSnapshot.updateDepth(depth);
		instrumentPnlSnapshotMap.put(key, pnlSnapshot);

		updateCustomHistoricals(depth.getInstrument(), depth.getTimestamp(), pnlSnapshot);
	}

	public void addCurrentCustomColumn(String instrument, String key, Double value) {
		String keyInstrument = getInstrumentKey(instrument);

		Map<String, Double> customColumnsInstrument = customColumns.getOrDefault(keyInstrument, new HashMap<>());
		customColumnsInstrument.put(key, value);
		customColumns.put(keyInstrument, customColumnsInstrument);
		customColumnsKeys.add(key);
	}

	private void updateCustomHistoricals(String instrumentPk, long timestamp, PnlSnapshot pnlSnapshot) {

		//check to update customColumns
		String keyInstrument = getInstrumentKey(instrumentPk);
		Map<String, Double> customColumnsInstrument = customColumns.get(keyInstrument);
		if (customColumnsInstrument != null) {
			timestamp = pnlSnapshot.getTimestamp(timestamp);
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
		String keyInstrument = getInstrumentKey(executionReport.getInstrument());

		PnlSnapshot pnlSnapshot = instrumentPnlSnapshotMap.getOrDefault(keyInstrument, new PnlSnapshotOrders());
		pnlSnapshot.setBacktest(isBacktest);
		pnlSnapshot.setPaper(isPaper);
		pnlSnapshot.setAlgorithmInfo(executionReport.getAlgorithmInfo());
		pnlSnapshot.updateExecutionReport(executionReport);

		updateCustomHistoricals(keyInstrument, executionReport.getTimestampCreation(), pnlSnapshot);

		instrumentPnlSnapshotMap.put(keyInstrument, pnlSnapshot);
		numberOfTrades++;

		return pnlSnapshot;
	}

	public String summary(Instrument instrument) {
		String keyInstrument = getInstrumentKey(instrument.getPrimaryKey());

		PnlSnapshot pnlSnapshot = instrumentPnlSnapshotMap.get(keyInstrument);
		if (pnlSnapshot == null) {
			logger.info("No pnl in {}", keyInstrument);
			return "";
		}
		String output = String
				.format("\n\ttrades:%d  position:%.3f totalPnl:%.3f totalFees:%.3f\n\trealizedPnl:%.3f  realizedFees:%.3f \n\tunrealizedPnl:%.3f  unrealizedFees:%.3f ",
						pnlSnapshot.numberOfTrades.get(), pnlSnapshot.netPosition, pnlSnapshot.totalPnl,
						pnlSnapshot.totalFees, pnlSnapshot.realizedPnl, pnlSnapshot.realizedFees,
						pnlSnapshot.unrealizedPnl, pnlSnapshot.unrealizedFees);

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
		Map<Instrument, Table> tradesTable = getTradesTableAndSave(null);
		Table tradeTable = tradesTable.get(instrument);
		if (tradeTable == null || tradeTable.rowCount() <= 0) {
			//nothing saved here

			return;
		}
		String keyInstrument = getInstrumentKey(instrument.getPrimaryKey());

		PnlSnapshot pnlSnapshot = instrumentPnlSnapshotMap.get(keyInstrument);

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
		File htmlFilePnl = new File(keyInstrument + "_" + algorithm.getAlgorithmInfo() + "_pnl.html");
		File htmlFilePosition = new File(keyInstrument + "_" + algorithm.getAlgorithmInfo() + "_position.html");
		Plot.show(figureRealizedPnl, keyInstrument, htmlFilePnl);
		Plot.show(figureRPosition, keyInstrument, htmlFilePosition);
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

	protected List<Double> getSortedValuesDouble(Map<Long, Double> input) {
		List<Double> output = new ArrayList<>(new TreeMap<Long, Double>(input).values());
		return output;
	}

	protected List<Integer> getSortedValuesInteger(Map<Long, Integer> input) {
		List<Integer> output = new ArrayList<>(new TreeMap<Long, Integer>(input).values());
		return output;
	}

	protected List<String> getSortedValuesString(Map<Long, String> input) {
		List<String> output = new ArrayList<>(new TreeMap<Long, String>(input).values());
		return output;
	}

	/***
	 *
	 * @param basePath path to save trades CSV ,if null is only getting the table , not persisting
	 * @return Table of results per instrument
	 */
	public synchronized Map<Instrument, Table> getTradesTableAndSave(String basePath) {
		synchronized (UPDATE_HISTORICAL_LOCK) {
			if (basePath != null) {
				File file = new File(basePath).getParentFile();
				file.mkdirs();//create if not exist
			}

			Map<Instrument, Table> output = new ConcurrentHashMap<>();
			try {
				List<String> instrumentsIterated = new ArrayList<>();

				for (String instrumentPk : instrumentPnlSnapshotMap.keySet()) {
					String instrumentKey = getInstrumentKey(instrumentPk);
					if (instrumentsIterated.contains(instrumentKey)) {
						continue;
					}

					//			summary(instrument);
					PnlSnapshot pnlSnapshot = instrumentPnlSnapshotMap.get(instrumentKey);

					if (numberOfTrades == 0) {
						logger.warn("no trades detected!");
						return output;
					}

					TreeMap<Long, Double> historicalAvgOpenPriceTemp = new TreeMap<Long, Double>(
							pnlSnapshot.historicalAvgOpenPrice);

					List<Long> timestamp = new ArrayList<>(
							new TreeMap<Long, Double>(pnlSnapshot.historicalAvgOpenPrice).keySet());
					List<Double> netPosition = getSortedValuesDouble(pnlSnapshot.historicalNetPosition);
					List<Double> avgOpenPrice = getSortedValuesDouble(pnlSnapshot.historicalAvgOpenPrice);
					List<Double> netInvestment = getSortedValuesDouble(pnlSnapshot.historicalNetInvestment);
					List<Double> historicalRealizedPnl = getSortedValuesDouble(pnlSnapshot.historicalRealizedPnl);
					List<Double> historicalUnrealizedPnl = getSortedValuesDouble(pnlSnapshot.historicalUnrealizedPnl);
					List<Double> historicalTotalPnl = getSortedValuesDouble(pnlSnapshot.historicalTotalPnl);
					List<Double> historicalFee = getSortedValuesDouble(pnlSnapshot.historicalFee);
					List<Double> historicalPrice = getSortedValuesDouble(pnlSnapshot.historicalPrice);
					List<Double> historicalQuantity = getSortedValuesDouble(pnlSnapshot.historicalQuantity);
					List<String> historicalAlgorithmInfo = getSortedValuesString(pnlSnapshot.historicalAlgorithmInfo);
					List<String> historicalInstrumentPk = getSortedValuesString(pnlSnapshot.historicalInstrumentPk);
					List<Integer> numberTrades = getSortedValuesInteger(pnlSnapshot.historicalNumberOfTrades);
					List<String> historicalVerb = getSortedValuesString(pnlSnapshot.historicalVerb);
					List<String> historicalClOrdId = getSortedValuesString(pnlSnapshot.historicalClOrdId);
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
							"getTradesTable has {} rows -> timestamp:{} verb:{} algorithmInfo:{} fee:{} price:{} quantity:{} netPosition:{} avgOpenPrice:{} netInvestment:{} historicalRealizedPnl:{} historicalUnrealizedPnl:{} historicalTotalPnl:{} numberTrades:{} instrumentPk:{}",
							dates.length, timestamp.size(), historicalVerb.size(), historicalAlgorithmInfo.size(),
							historicalFee.size(), historicalPrice.size(), historicalQuantity.size(), netPosition.size(),
							avgOpenPrice.size(), netInvestment.size(), historicalRealizedPnl.size(),
							historicalUnrealizedPnl.size(), historicalTotalPnl.size(), numberTrades.size(),
							historicalInstrumentPk.size());

					LongColumn timestampColumn = LongColumn.create("timestamp", ArrayUtils.toPrimitive(timestampArr));
					DateTimeColumn dateTimeColumn = DateTimeColumn.create("date", dates);
					StringColumn instrumentColumn = StringColumn.create("instrument", historicalInstrumentPk);
					StringColumn verbColumn = StringColumn.create("verb", historicalVerb);
					StringColumn algoInfoColumn = StringColumn
							.create("algorithmInfo", fromStrList(historicalAlgorithmInfo));
					StringColumn clOrdIdColumn = StringColumn.create("clientOrderId", fromStrList(historicalClOrdId));
					DoubleColumn priceColumn = DoubleColumn.create("price", fromList(historicalPrice));
					DoubleColumn feeColumn = DoubleColumn.create("fee", fromList(historicalFee));
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
									quantityColumn, feeColumn, netPositionColumn, avgOpenPriceColumn,
									netInvestmentColumn, historicalRealizedPnlColumn, historicalUnrealizedPnltColumn,
									historicalTotalPnlColumn, numberTradesColumn, algoInfoColumn, instrumentColumn);

					//add custom columns
					if (customColumnsKeys.size() > 0) {
						ffillHistoricalCustomColumns(pnlSnapshot);
						Map<Long, List<CustomColumn>> historicalsCustoms = new TreeMap<Long, List<CustomColumn>>(
								pnlSnapshot.historicalCustomColumns);
						int size = 0;
						int tradesSize = output1.rowCount();
						for (String customKey : customColumnsKeys) {
							try {
								List<Double> customDouble = new ArrayList<>();
								for (List<CustomColumn> customColumn : historicalsCustoms.values()) {
									for (CustomColumn customColumn1 : customColumn) {
										if (customKey.equalsIgnoreCase(customColumn1.getKey())) {
											if (customDouble.size() + 1 > tradesSize) {
												//												logger.warn(
												//														"trying to add more rows on {} than in trades length skip this row  size={} totalLen={} !",
												//														customKey, tradesSize, customDouble.size());
												break;
											} else {
												customDouble.add(customColumn1.getValue());
											}
										}
									}
								}
								size = customDouble.size();
								DoubleColumn customColumn = DoubleColumn.create(customKey, fromList(customDouble));
								output1 = output1.addColumns(customColumn);
							} catch (Exception e) {
								logger.error("error adding custom column {} of len {}  to trades of len {}-> skip it",
										customKey, size, tradesSize, e);
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
						} catch (Exception e) {
							logger.error("can't save tradestable to {} ", filename, e);
						}
					}
					Instrument instrument = Instrument.getInstrument(getInstrumentPK(instrumentKey));
					output.put(instrument, output1);
					instrumentsIterated.add(instrumentKey);
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
