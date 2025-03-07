/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.TestJavaClient;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

class IBGridBagPanel extends JPanel {
    private static final Insets oneInsets = new Insets(1, 1, 1, 1);
    private GridBagLayout m_layout = new GridBagLayout();

    IBGridBagPanel() {
        setLayout(m_layout);
    }

    public void addGBComponent(Component comp,
                               GridBagConstraints gbc, int weightx, int gridwidth) {
        gbc.weightx = weightx;
        gbc.gridwidth = gridwidth;

        setConstraints(comp, gbc);
        add(comp, gbc);
    }

    public void setConstraints(Component comp, GridBagConstraints constraints) {
        m_layout.setConstraints(comp, constraints);
    }

    public void SetObjectPlacement(Component c, int x, int y) {
        addToPane(c, x, y, 1, 1, 100, 100, oneInsets);
    }

    public void SetObjectPlacement(Component c, int x, int y, int w, int h) {
        addToPane(c, x, y, w, h, 100, 100, oneInsets);
    }

    public void SetObjectPlacement(Component c, int x, int y, int w, int h, int xGrow, int yGrow) {
        addToPane(c, x, y, w, h, xGrow, yGrow, oneInsets);
    }

    public void SetObjectPlacement(Component c, int x, int y, int w, int h, int xGrow, int yGrow, int fill) {
        addToPane(c, x, y, w, h, xGrow, yGrow, GridBagConstraints.WEST, fill, oneInsets);
    }

    public void SetObjectPlacement(Component c, int x, int y, int w, int h, int xGrow, int yGrow, int anchor, int fill) {
        addToPane(c, x, y, w, h, xGrow, yGrow, anchor, fill, oneInsets);
    }

    public void SetObjectPlacement(Component c, int x, int y, int w, int h, int xGrow, int yGrow, Insets insets) {
        addToPane(c, x, y, w, h, xGrow, yGrow, insets);
    }

    private void addToPane(Component c, int x, int y, int w, int h,
                           int xGrow, int yGrow, Insets insets) {
        addToPane(c, x, y, w, h, xGrow, yGrow, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets);
    }

    private void addToPane(Component c, int x, int y, int w, int h, int xGrow,
                           int yGrow, int anchor, int fill, Insets insets) {
        GridBagConstraints gbc = new GridBagConstraints();

        // the coordinates of the cell in the layout that contains
        // the upper-left corner of the component
        gbc.gridx = x;
        gbc.gridy = y;

        // the number of cells that this entry is going to take up
        gbc.gridwidth = w;
        gbc.gridheight = h;

        // drive how extra space is distributed among components.
        gbc.weightx = xGrow;
        gbc.weighty = yGrow;

        // drive how component is made larger if extra space is available for it
        gbc.fill = fill;

        // drive where, within the display area, to place the component when it
        // is larger than its display area.
        gbc.anchor = anchor;

        // drive the minimum amount of space between the component and the edges
        // of its display area
        gbc.insets = insets;

        add(c, gbc);
    }
}
