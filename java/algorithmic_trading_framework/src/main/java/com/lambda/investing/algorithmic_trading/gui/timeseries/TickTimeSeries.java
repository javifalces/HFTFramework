package com.lambda.investing.algorithmic_trading.gui.timeseries;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.time.Month;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

public class TickTimeSeries extends ApplicationFrame {

	//	https://github.com/jfree/jfree-demos/blob/master/src/main/java/org/jfree/chart/demo/TimeSeriesChartDemo1.java
	private static final long serialVersionUID = 1L;
	private Map<String, TimeSeries> nameToTimeSerie;

	private TimeSeriesCollection dataset;
	private JFreeChart chart;
	private ChartPanel chartPanel;

	public TickTimeSeries(JTabbedPane depthsTab, String title) {
		super(title);
		nameToTimeSerie = new ConcurrentHashMap<>();

		dataset = new TimeSeriesCollection();
		chart = createChart(dataset);

		chartPanel = createJPanel();
		//		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		//		setContentPane(chartPanel);

		//Add JChart
		depthsTab.add(title, chartPanel);


	}

	private ChartPanel createJPanel() {
		ChartPanel panel = new ChartPanel(this.chart, false);
		panel.setFillZoomRectangle(true);
		panel.setMouseWheelEnabled(true);
		return panel;
	}

	private JFreeChart createChart(TimeSeriesCollection dataset) {
		JFreeChart chart = ChartFactory.createTimeSeriesChart(this.getTitle(),  // title
				"Date",             // x-axis label
				"Price",   // y-axis label
				dataset);

		chart.setBackgroundPaint(Color.WHITE);

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.LIGHT_GRAY);
		plot.setDomainGridlinePaint(Color.WHITE);
		plot.setRangeGridlinePaint(Color.WHITE);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);

		XYItemRenderer r = plot.getRenderer();
		if (r instanceof XYLineAndShapeRenderer) {
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
			renderer.setDefaultShapesVisible(true);
			renderer.setDefaultShapesFilled(true);
			renderer.setDrawSeriesLineAsPath(true);
		}

		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss.SSS"));

		return chart;

	}

	public void updateTimeSerie(String name, long timestamp, double value) {
		TimeSeries timeSeries = null;
		if (!nameToTimeSerie.containsKey(name)) {
			timeSeries = new TimeSeries(name);
			dataset.addSeries(timeSeries);
		} else {
			timeSeries = nameToTimeSerie.get(name);
		}
		timeSeries.add(new Millisecond(new Date(timestamp)), value);

		updateChart(name);
	}

	private void updateChart(String name) {
		//todo something to update
		chart = ChartFactory.createTimeSeriesChart(this.getTitle(),  // title
				"Date",             // x-axis label
				"Price",   // y-axis label
				dataset);

	}

}
