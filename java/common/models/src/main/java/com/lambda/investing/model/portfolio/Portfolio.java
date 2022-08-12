package com.lambda.investing.model.portfolio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter @Setter public class Portfolio implements Runnable {

	private static Map<String, Portfolio> pathToInstance = new HashMap<>();
	public static Gson GSON_STRING = new GsonBuilder()
			.excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.FINAL)
			.serializeSpecialFloatingPointValues().create();
	public static String REQUESTED_PORTFOLIO_INFO = "portfolio";
	protected static Logger logger = LogManager.getLogger(Portfolio.class);
	private Map<String, PortfolioInstrument> portfolioInstruments;
	private String path;
	//	private double openPnl=0.;
	//	private double closePnl=0.;
	//	private double totalPnl=0.;
	private long timestampLastUpdate = 0;
	private boolean autosave = false;
	private long lastSaveTimestamp = 0;

	public static Portfolio getPortfolio(String path, boolean isBacktest) {
		if (pathToInstance.containsKey(path)) {
			return pathToInstance.get(path);
		}
		Portfolio portfolio = new Portfolio();
		File portfolioFile = new File(path);
		if (!isBacktest) {
			portfolio = new Portfolio(path);
		}

		if (!isBacktest && portfolioFile.exists()) {
			///read it
			try {
				String fileContent = new String(Files.readAllBytes(Paths.get(path)));
				portfolio = GSON_STRING.fromJson(fileContent, Portfolio.class);
				new Thread(portfolio, "portfolio_autosave").start();

			} catch (IOException e) {
				logger.error("error reading portfolio json from {}-> close app to not override anything", path, e);
				System.exit(-1);
			}

		}

		pathToInstance.put(path, portfolio);
		return portfolio;
	}

	public static Portfolio getPortfolio(String path) {
		return getPortfolio(path, false);
	}


	public void clear() {
		portfolioInstruments.clear();
		long timestampLastUpdate = 0;
		lastSaveTimestamp = 0;
	}

	private void savePortfolio() {
		String content = GSON_STRING.toJson(this);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(path));
			writer.write(content);
			writer.close();
		} catch (IOException e) {
			logger.error("error saving portfolio json {} in {}", content, path, e);
		}
	}

	public Portfolio() {
		//calling directly here will not save into path
		portfolioInstruments = new HashMap<>();
		this.autosave = false;
	}

	private Portfolio(String path) {
		//calling directly here will not save into path
		portfolioInstruments = new HashMap<>();
		this.path = path;
		//call autosave
		if (this.path != null) {
			this.autosave = true;
			new Thread(this, "portfolio_autosave").start();
		}
	}



	public void updateTrade(ExecutionReport executionReport) {
		if (executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled) || executionReport
				.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)) {
			//only in trades
			PortfolioInstrument portfolioInstrument = portfolioInstruments.getOrDefault(executionReport.getInstrument(),
					new PortfolioInstrument(executionReport.getInstrument()));
			portfolioInstrument.updateTrade(executionReport);
			portfolioInstruments.put(executionReport.getInstrument(), portfolioInstrument);
			timestampLastUpdate = executionReport.getTimestampCreation();
		}
	}

	@Override public void run() {
		while (this.autosave) {

			//saving if updates
			if (lastSaveTimestamp != timestampLastUpdate) {
				savePortfolio();
				lastSaveTimestamp = timestampLastUpdate;
			}

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
