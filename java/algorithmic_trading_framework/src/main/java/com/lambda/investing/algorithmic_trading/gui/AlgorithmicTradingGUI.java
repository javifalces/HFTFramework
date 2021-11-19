package com.lambda.investing.algorithmic_trading.gui;

import com.binance.api.client.domain.market.OrderBook;
import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmObserver;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.SingleInstrumentAlgorithm;
import com.lambda.investing.algorithmic_trading.gui.orderbook.OrderbookGUI;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
//import sun.swing.JLightweightFrame;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.*;

public class AlgorithmicTradingGUI extends JFrame implements AlgorithmObserver {

	private static String TITLE = "[%s]Lambda Algotrading  %d algorithms";
	private List<SingleInstrumentAlgorithm> algorithmsList;
	private Map<String, OrderbookGUI> orderBookGUIMap;

	JTabbedPane depthTabs;

	public AlgorithmicTradingGUI(List<SingleInstrumentAlgorithm> algorithmsList) {
		//		https://www.fdi.ucm.es/profesor/jpavon/poo/tema6resumido.pdf
		super(String.format(TITLE, new Date(), algorithmsList.size()));
		this.algorithmsList = algorithmsList;

		orderBookGUIMap = new ConcurrentHashMap<>();
	}

	public AlgorithmicTradingGUI(SingleInstrumentAlgorithm algorithm) {
		this.algorithmsList = new ArrayList<>();
		this.algorithmsList.add(algorithm);
		orderBookGUIMap = new ConcurrentHashMap<>();
	}

	private void startGUI() {
		depthTabs = new JTabbedPane();
		this.add(depthTabs);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	public void start() {
		startGUI();

		for (SingleInstrumentAlgorithm algorithm : algorithmsList) {
			algorithm.register(this);
			orderBookGUIMap.put(algorithm.getAlgorithmInfo(), new OrderbookGUI(depthTabs, algorithm.getInstrument()));

		}
		this.pack();
		setSize(800, 600);
		setVisible(true);
	}

	@Override public void onUpdateDepth(String algorithmInfo, Depth depth) {
		orderBookGUIMap.get(algorithmInfo).updateDepth(depth);
	}

	@Override public void onUpdateTrade(String algorithmInfo, PnlSnapshot pnlSnapshot) {

	}

	@Override public void onUpdateClose(String algorithmInfo, Trade trade) {

	}

	@Override public void onUpdateParams(String algorithmInfo, Map<String, Object> newParams) {

	}

	@Override public void onUpdateMessage(String algorithmInfo, String name, String body) {

	}

	@Override public void onOrderRequest(String algorithmInfo, OrderRequest orderRequest) {

	}

	@Override public void onExecutionReportUpdate(String algorithmInfo, ExecutionReport executionReport) {

	}
}
