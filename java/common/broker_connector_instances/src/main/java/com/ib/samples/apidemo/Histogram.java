/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import com.ib.client.Decimal;
import com.ib.client.HistogramEntry;
import com.ib.client.Util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.List;

import javax.swing.JComponent;

public class Histogram extends JComponent {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static final int m_barHeight = 15;
    private final List<HistogramEntry> m_rows;
    private static final int m_x0 = 80;

    public Histogram(List<HistogramEntry> rows) {
        m_rows = rows;
    }

    @Override
    protected void paintComponent(Graphics g) {
        int y = 0;
        Decimal max = getMax();

        int width = getWidth() - m_x0;

        for (HistogramEntry bar : m_rows) {
            int x1 = (int) (bar.size().multiply(Decimal.get(width)).divide(max).longValue());

            String label = Util.DoubleMaxString(bar.price());

            g.setColor(Color.red);
            g.fillRect(m_x0, y, x1, m_barHeight);
            g.setColor(Color.black);
            g.drawString(label, 0, y + m_barHeight - 3);
            g.drawRect(m_x0, y, x1, m_barHeight);

            y += m_barHeight;
        }
    }

    Decimal getMax() {
        return m_rows.stream().map(entry -> entry.size()).max(Decimal::compare).orElse(Decimal.MINUS_ONE);
    }

    @Override
    public Dimension getPreferredSize() {// why on main screen 1 is okay but not here?
        return new Dimension(100, m_rows.size() * m_barHeight);
    }

}
