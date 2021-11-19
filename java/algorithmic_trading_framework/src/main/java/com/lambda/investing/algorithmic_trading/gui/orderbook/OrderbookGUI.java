package com.lambda.investing.algorithmic_trading.gui.orderbook;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.gui.timeseries.TickTimeSeries;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

public class OrderbookGUI {

	private Logger logger = LogManager.getLogger(Algorithm.class);

	private static final String TAB_NAME = "Full Market Depth";

	private DepthTableModel tableModel;
	private JTable table;
	private TickTimeSeries tickTimeSeries;

	public OrderbookGUI(JTabbedPane depthTabs, Instrument instrument) {
		depthTabs.setLayout(new BorderLayout());

		tableModel = new DepthTableModel();
		table = new JTable(tableModel);
		JScrollPane jScrollPane = new JScrollPane(table);
		depthTabs.addTab(instrument.getPrimaryKey(), jScrollPane);

		tickTimeSeries = new TickTimeSeries(depthTabs, "depth");

	}

	public void updateDepth(Depth depth) {

		try {
			SwingUtilities.invokeAndWait(new Runnable() {

				public void run() {
					setDepth(depth.getAsks(), depth.getAsksQuantities(), depth.getBids(), depth.getBidsQuantities());
					tickTimeSeries.updateTimeSerie("ask", depth.getTimestamp(), depth.getBestAsk());
					tickTimeSeries.updateTimeSerie("bid", depth.getTimestamp(), depth.getBestBid());
				}
			});
		} catch (Exception e) {
			logger.error("error plotting ", e);
		}

	}

	public void setDepth(Double[] asks, Double[] askVols, Double[] bids, Double[] bidVols) {

		double[][] data = new double[asks.length > bids.length ? asks.length * 2 + 1 : bids.length * 2 + 1][4];
		for (int i = 0; i < asks.length; i++) {
			data[i][3] = askVols[i] / 1000000;
			data[i][2] = asks[i];
		}
		for (int i = 0; i < bids.length; i++) {
			int indexToWrite = i + bids.length;
			data[indexToWrite][0] = bidVols[i] / 1000000;
			data[indexToWrite][1] = bids[i];
		}
		tableModel.setData(data);
	}

	private class DepthTableModel extends AbstractTableModel {

		private double[][] data = new double[0][0];

		public void setData(double[][] data) {
			this.data = data;
			fireTableDataChanged();
		}

		public int getRowCount() {
			return data.length;
		}

		public int getColumnCount() {
			return 4;
		}

		public Object getValueAt(int row, int column) {
			if (data[row][column] == 0.0) {
				return "";
			} else {
				return Double.toString(data[row][column]);
			}
		}

		public String getColumnName(int column) {

			switch (column) {
				case 0:
					return "Vol";
				case 1:
					return "Bid";
				case 2:
					return "Ask";
				case 3:
					return "Vol";
				default:
					return "";
			}
		}
	}
}
