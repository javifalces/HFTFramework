/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo.util;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.border.Border;


public class NewTabbedPanel extends JPanel {
    private static final Color COLOR = new Color(184, 207, 229);
    private static final int V1 = 1;        // bottom line is raised this amount
    private static final int EXTRA_TAB_WIDTH = 14;        // extra width on each side
    private static final int BUFF = 22;    // extra width on each side (should be WID + BUF)
    private static final int TAB_HEIGHT = 20;    // total height of button
    private static final Border B1 = new TabBorder();
    private static final Border B2 = new UnderBorder();
    private static final TabIcon ICON1 = new TabIcon(false);
    private static final TabIcon ICON2 = new TabIcon(true);

    private final JPanel m_topPanel = new JPanel();
    private final CardLayout m_cardLayout = new CardLayout();
    private final JPanel m_cardPanel = new JPanel(m_cardLayout);
    private final Map<String, Tab> m_map = new HashMap<>();
    private final boolean m_underline;  // if true, draws a horizontal line all the way across
    private int m_count = 2;

    public NewTabbedPanel() {
        this(false);
    }

    public NewTabbedPanel(boolean underline) {
        m_underline = underline;

        setLayout(new BorderLayout());
        m_topPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 15));

        add(new JScrollPane(m_topPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.NORTH);
        add(m_cardPanel);
    }

    public void addTab(final String title, final JComponent tab) {
        addTab(title, tab, false, false);
    }

    public void addTab(final String titleIn, final JComponent comp, boolean select, boolean canClose) {
        final String title = m_map.containsKey(titleIn)
                ? titleIn + " " + m_count++ : titleIn;

        HtmlButton button = new But(title, canClose, e -> select(title));

        Tab tab = new Tab(title, comp, button);
        m_map.put(title, tab);

        m_topPanel.add(createSpacer(15));
        m_topPanel.add(button);
        m_cardPanel.add(comp, title);

        if (m_map.size() == 1 || select) {
            select(title);
        } else {
            button.setSelected(false);
        }
    }

    /**
     * Select the correct panel and underline the correct html button.
     */
    public void select(String title) {
        // show selected tab
        m_cardLayout.show(m_cardPanel, title);

        Tab selectedTab = m_map.get(title);

        // select or deselect all buttons
        for (Tab tab : m_map.values()) {
            tab.m_button.setSelected(tab == selectedTab);
        }

        // call activated() on selected tab?
        if (!selectedTab.m_activated && selectedTab.m_comp instanceof INewTab) {
            ((INewTab) selectedTab.m_comp).activated();
            selectedTab.m_activated = true;
        }
    }

    private Tab getSelectedTab() {
        for (Tab tab : m_map.values()) {
            if (tab.m_button.isSelected()) {
                return tab;
            }
        }
        return null;
    }

    private Component createSpacer(final int i) {
        JPanel h = new JPanel();
        h.setPreferredSize(new Dimension(i, TAB_HEIGHT));
        if (m_underline) {
            h.setBorder(B2);
        }
        return h;
    }

    public interface INewTab {
        void activated(); // called when the tab is first visited

        void closed();    // called when the tab is closed
    }

    public static abstract class NewTabPanel extends JPanel implements INewTab {
    }

    private static class Tab {
        String m_title;
        JComponent m_comp;
        HtmlButton m_button;
        boolean m_activated;

        Tab(String title, JComponent comp, HtmlButton button) {
            m_title = title;
            m_comp = comp;
            m_button = button;
        }
    }

    static class TabBorder implements Border {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(COLOR);
            int L = x + 4;
            int R = width - 5;
            int T = y;
            int B = height - V1;
            g.drawLine(L, T, R, T);
            g.drawLine(L, T, L, B);
            g.drawLine(R, T, R, B);
            g.drawLine(x, B, L, B);
            g.drawLine(R, B, width, B);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, 0, 0);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    static class UnderBorder implements Border {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(COLOR);
            int L = x;
            int R = width;
            int B = height - V1;
            g.drawLine(L, B, R, B);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, 0, 0);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    public static class TabIcon implements Icon {
        private static final int ICON_SIZE = 5;
        private final boolean m_under;

        TabIcon(boolean under) {
            m_under = under;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            int left = x + 1;
            int right = left + ICON_SIZE - 1;
            int top = y;
            int bot = top + ICON_SIZE - 1;
            g.drawLine(left, top, right, bot);
            g.drawLine(left, bot, right, top);
            if (m_under) {
                g.drawLine(left, bot + 2, right, bot + 2);
            }
        }

        @Override
        public int getIconWidth() {
            return ICON_SIZE + 4;
        }

        @Override
        public int getIconHeight() {
            return ICON_SIZE;
        }
    }

    class But extends HtmlButton {
        boolean m_canClose;

        But(String text, boolean canClose, ActionListener v) {
            super(text, v);
            m_canClose = canClose;
            setHorizontalAlignment(SwingConstants.CENTER);
            setHorizontalTextPosition(SwingConstants.LEFT);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.height = TAB_HEIGHT;
            d.width += EXTRA_TAB_WIDTH;
            return d;
        }

        @Override
        public void setSelected(boolean v) {
            super.setSelected(v);
            setBorder(v ? B1 : m_underline ? B2 : null);
            setIcon(v && m_canClose ? ICON1 : null);
        }

        protected void onPressed(MouseEvent e) {
            if (overX(e)) {
                setBackground(light);
            } else {
                super.onPressed(e);
            }
        }

        @Override
        protected void onClicked(MouseEvent e) {
            if (overX(e)) {
                onClosed();
            } else {
                super.onClicked(e);
            }
        }

        @Override
        protected void onEntered(MouseEvent e) {
            super.onEntered(e);
            if (overX(e)) {
                setIcon(ICON2);
            }
        }

        @Override
        protected void onMouseMoved(MouseEvent e) {
            super.onMouseMoved(e);
            if (overX(e)) {
                setIcon(ICON2);
            } else if (m_canClose && isSelected()) {
                setIcon(ICON1);
            }
        }

        @Override
        protected void onExited() {
            super.onExited();
            if (m_canClose && isSelected()) {
                setIcon(ICON1);
            }
        }

        private boolean overX(MouseEvent e) {
            return m_canClose && isSelected() && e.getX() > getWidth() - BUFF;
        }
    }

    void onClosed() {
        Tab tab = getSelectedTab();
        if (tab == null) return;
        if (tab.m_comp instanceof INewTab) {
            ((INewTab) tab.m_comp).closed();
        }
        m_topPanel.remove(Arrays.asList(m_topPanel.getComponents()).indexOf(tab.m_button) - 1);
        m_topPanel.remove(tab.m_button);
        m_topPanel.repaint();

        m_cardPanel.remove(tab.m_comp);
        m_map.remove(tab.m_title);

        if (!m_map.isEmpty()) {
            Entry<String, Tab> entry = m_map.entrySet().iterator().next();
            select(entry.getValue().m_title);
        }
    }

    public static void main(String[] args) {
        NewTabbedPanel p = new NewTabbedPanel();
        p.addTab("Tab1", new JLabel("tab 1"), true, true);
        p.addTab("Tab2", new JLabel("tab 2"), false, true);
        p.addTab("Tab3", new JLabel("tab 3"), false, true);

        JFrame f = new JFrame();
        f.add(p);
        f.setSize(200, 200);
        f.setVisible(true);
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

}
