/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;

import javax.swing.JLabel;
import javax.swing.border.Border;

public class HtmlButton extends JLabel {
    static Color light = new Color(220, 220, 220);

    private String m_text;
    protected boolean m_selected;
    private ActionListener m_al;
    private Color m_bg = getBackground();

    public boolean isSelected() {
        return m_selected;
    }

    public void setSelected(boolean v) {
        m_selected = v;
    }

    public void addActionListener(ActionListener v) {
        m_al = v;
    }

    public HtmlButton(String text) {
        this(text, null);
    }

    @Override
    public void setText(String text) {
        m_text = text;
        super.setText(text);
    }

    public HtmlButton(String text, ActionListener v) {
        super(text);
        m_text = text;
        m_al = v;
        setOpaque(true);
        setForeground(Color.blue);

        MouseAdapter a = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                onPressed(e);
            }

            public void mouseReleased(MouseEvent e) {
                onClicked(e);
                setBackground(m_bg);
            }

            public void mouseEntered(MouseEvent e) {
                onEntered(e);
            }

            public void mouseExited(MouseEvent e) {
                onExited();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                onMouseMoved(e);
            }
        };
        addMouseListener(a);
        addMouseMotionListener(a);
        setFont(getFont().deriveFont(Font.PLAIN));
    }

    protected void onMouseMoved(MouseEvent e) {
    }

    protected void onEntered(MouseEvent e) {
        if (!m_selected) {
            setText(underline(m_text));
        }
    }

    protected void onExited() {
        setText(m_text);
    }

    protected void onPressed(MouseEvent e) {
        if (!m_selected) {
            setBackground(light);
        }
    }

    protected void onClicked(MouseEvent e) {
        actionPerformed();
    }

    protected void actionPerformed() {
        if (m_al != null) {
            m_al.actionPerformed(null);
        }
    }

    public static class HtmlRadioButton extends HtmlButton {
        private HashSet<HtmlRadioButton> m_group;

        HtmlRadioButton(String text, HashSet<HtmlRadioButton> group) {
            super(text);
            m_group = group;
            group.add(this);
        }

        @Override
        protected void actionPerformed() {
            for (HtmlRadioButton but : m_group) {
                but.setSelected(false);
            }
            setSelected(true);
            super.actionPerformed();
        }
    }

    static String underline(String str) {
        return String.format("<html><u>%s</html>", str);
    }

    static String bold(String str) {
        return String.format("<html><b>%s</html>", str);
    }

    static class B implements Border {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return null;
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
