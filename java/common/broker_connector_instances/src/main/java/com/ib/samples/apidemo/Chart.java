/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.List;

import javax.swing.JComponent;

import com.ib.controller.Bar;

public class Chart extends JComponent {
    private static final int width = 5;
    private int height;
    private double min;
    private double max;
    private final List<Bar> m_rows;
    private double m_current = 118;

    public void current(double v) {
        m_current = v;
    }

    public Chart(List<Bar> rows) {
        m_rows = rows;
    }

    @Override
    protected void paintComponent(Graphics g) {
        height = getHeight();
        min = getMin();
        max = getMax();

        int x = 1;
        for (Bar bar : m_rows) {
            int high = getY(bar.high());
            int low = getY(bar.low());
            int open = getY(bar.open());
            int close = getY(bar.close());

            // draw high/low line
            g.setColor(Color.black);
            g.drawLine(x + 1, high, x + 1, low);

            if (bar.close() > bar.open()) {
                g.setColor(Color.green);
                g.fillRect(x, close, 3, open - close);
            } else {
                g.setColor(Color.red);
                g.fillRect(x, open, 3, close - open);
            }

            x += width;
        }

        // draw price line
        g.setColor(Color.black);
        int y = getY(m_current);
        g.drawLine(0, y, m_rows.size() * width, y);
    }

    /**
     * Convert bar value to y coordinate.
     */
    private int getY(double v) {
        double span = max - min;
        double pct = (v - min) / span;
        double val = pct * height + .5;
        return height - (int) val;
    }

    @Override
    public Dimension getPreferredSize() {// why on main screen 1 is okay but not here?
        return new Dimension(m_rows.size() * width, 100);
    }

    private double getMin() {
        double min = Double.MAX_VALUE;
        for (Bar bar : m_rows) {
            min = Math.min(min, bar.low());
        }
        return min;
    }

    private double getMax() {
        double max = 0;
        for (Bar bar : m_rows) {
            max = Math.max(max, bar.high());
        }
        return max;
    }
}
