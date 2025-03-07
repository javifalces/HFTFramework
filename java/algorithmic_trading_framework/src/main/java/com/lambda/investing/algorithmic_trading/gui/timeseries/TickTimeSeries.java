package com.lambda.investing.algorithmic_trading.gui.timeseries;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.*;

import com.lambda.investing.ArrayUtils;
import lombok.Getter;
import org.jfree.chart.*;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;

@Getter
public class TickTimeSeries extends ApplicationFrame {

    //	https://github.com/jfree/jfree-demos/blob/master/src/main/java/org/jfree/chart/demo/TimeSeriesChartDemo1.java
    private static final long serialVersionUID = 1L;
    private Map<String, TimeSeries> nameToTimeSerie;

    private TimeSeriesCollection dataset;
    private JFreeChart chart;
    private ChartPanel chartPanel;

    private long lastTimestamp = 0;

    public static String BID_SERIE = "Bid";
    public static String ASK_SERIE = "Ask";
    public static ArrayList<String> MARKET_DATA_SERIES = (ArrayList<String>) ArrayUtils.ArrayToList(new String[]{BID_SERIE, ASK_SERIE});


    public TickTimeSeries(ChartTheme theme, JComponent panel, String title, String xAxis, String yAxis) {
        super(title);
        nameToTimeSerie = new HashMap<>();

        dataset = new TimeSeriesCollection();
        chart = createChart(theme, dataset, xAxis, yAxis);


        chartPanel = createJPanel();

        //add to jPanel
        panel.setLayout(new BorderLayout());
        panel.add(chartPanel);

    }

    private ChartPanel createJPanel() {
        ChartPanel panel = new ChartPanel(this.chart, false);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }

    private JFreeChart createChart(ChartTheme theme, TimeSeriesCollection dataset, String xAxis, String yAxis) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(this.getTitle(),  // title
                xAxis,             // x-axis label
                yAxis,   // y-axis label
                dataset);

//        chart.setBackgroundPaint(Color.WHITE);


        XYPlot plot = (XYPlot) chart.getPlot();
//        plot.setBackgroundPaint(Color.LIGHT_GRAY);
//        plot.setDomainGridlinePaint(Color.WHITE);
//        plot.setRangeGridlinePaint(Color.WHITE);


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
        updateTimeSerie(name, timestamp, value, new Minute(new Date(timestamp)));
    }

    public void updateTimeSerie(String name, long timestamp, double value, RegularTimePeriod timePeriod) {
        boolean timestampOk = timestamp != 0;
        if (!timestampOk) {
            return;
        }


        TimeSeries timeSeries = null;
        if (!nameToTimeSerie.containsKey(name)) {
            synchronized (this) {
                timeSeries = new TimeSeries(name);
                dataset.addSeries(timeSeries);
                nameToTimeSerie.put(name, timeSeries);
            }
        } else {
            timeSeries = nameToTimeSerie.get(name);
        }
        lastTimestamp = timestamp;
//        RegularTimePeriod timePeriod = new Minute(new Date(timestamp));
//        if (MARKET_DATA_SERIES.contains(name)) {
//            timePeriod = new Minute(new Date(timestamp));
//        }
        timeSeries.addOrUpdate(timePeriod, value);

    }


}
