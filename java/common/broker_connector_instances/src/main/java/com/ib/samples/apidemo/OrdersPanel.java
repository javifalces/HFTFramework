/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Util;
import com.ib.controller.ApiController.ILiveOrderHandler;

import com.ib.samples.apidemo.util.HtmlButton;
import com.ib.samples.apidemo.util.VerticalPanel;

public class OrdersPanel extends JPanel {
    private OrdersModel m_model = new OrdersModel();
    private JTable m_table = new JTable(m_model);

    OrdersPanel() {
        JScrollPane scroll = new JScrollPane(m_table);
        scroll.setBorder(new TitledBorder("Live Orders"));

        m_table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onDoubleClick();
                }
            }
        });

        HtmlButton ticket = new HtmlButton("Place New Order") {
            @Override
            public void actionPerformed() {
                onPlaceOrder();
            }
        };

        HtmlButton modify = new HtmlButton("Modify Selected Order") {
            @Override
            public void actionPerformed() {
                onDoubleClick();
            }
        };

        HtmlButton attach = new HtmlButton("Attach New Order to Selected Order") {
            @Override
            public void actionPerformed() {
                onAttachOrder();
            }
        };

        HtmlButton reqExisting = new HtmlButton("Take Over Existing TWS Orders") {
            @Override
            public void actionPerformed() {
                onTakeOverExisting();
            }
        };

        HtmlButton reqFuture = new HtmlButton("Take Over Future TWS Orders") {
            @Override
            public void actionPerformed() {
                onTakeOverFuture();
            }
        };

        HtmlButton cancel = new HtmlButton("Cancel Selected Order") {
            @Override
            public void actionPerformed() {
                onCancelOrder();
            }
        };

        HtmlButton cancelAll = new HtmlButton("Cancel All Orders") {
            @Override
            public void actionPerformed() {
                onCancelAll();
            }
        };

        HtmlButton refresh = new HtmlButton("Refresh") {
            @Override
            public void actionPerformed() {
                onRefresh();
            }
        };

        JPanel buts = new VerticalPanel();
        buts.add(ticket);
        buts.add(modify);
        buts.add(attach);
        buts.add(cancel);
        buts.add(cancelAll);
        buts.add(reqExisting);
        buts.add(reqFuture);
        buts.add(refresh);

        setLayout(new BorderLayout());
        add(buts, BorderLayout.EAST);
        add(scroll);
    }

    protected void onDoubleClick() {
        OrderRow order = getSelectedOrder();
        if (order != null) {
            TicketDlg dlg = new TicketDlg(order.m_contract, order.m_order);
            dlg.setVisible(true);
        }
    }

    protected void onTakeOverExisting() {
        ApiDemo.INSTANCE.controller().takeTwsOrders(m_model);
    }

    protected void onTakeOverFuture() {
        ApiDemo.INSTANCE.controller().takeFutureTwsOrders(m_model);
    }

    protected void onCancelOrder() {
        OrderRow order = getSelectedOrder();
        if (order != null) {
            TicketDlg dlg = new TicketDlg(order.m_contract, order.m_order, true);
            dlg.setVisible(true);
        }
    }

    protected void onCancelAll() {
        ApiDemo.INSTANCE.controller().cancelAllOrders();
    }

    private OrderRow getSelectedOrder() {
        int i = m_table.getSelectedRow();
        return i != -1 ? m_model.get(i) : null;
    }

    public void activated() {
        onRefresh();
    }

    private static void onPlaceOrder() {
        TicketDlg dlg = new TicketDlg(null, null);
        dlg.setVisible(true);
    }

    protected void onAttachOrder() {
        OrderRow row = getSelectedOrder();
        if (row != null) {
            Order parent = row.m_order;

            Order child = new Order();
            child.parentId(parent.orderId());
            child.action(parent.action());
            child.totalQuantity(parent.totalQuantity());
            child.orderType(OrderType.TRAIL);
            child.auxPrice(1);

            TicketDlg dlg = new TicketDlg(row.m_contract.clone(), child);
            dlg.setVisible(true);
        }
    }

    protected void onRefresh() {
        m_model.clear();
        m_model.fireTableDataChanged();
        ApiDemo.INSTANCE.controller().reqLiveOrders(m_model);
    }

    static class OrdersModel extends AbstractTableModel implements ILiveOrderHandler {
        private Map<Integer, OrderRow> m_map = new HashMap<>();
        private List<OrderRow> m_orders = new ArrayList<>();

        @Override
        public int getRowCount() {
            return m_orders.size();
        }

        public void clear() {
            m_orders.clear();
            m_map.clear();
        }

        public OrderRow get(int i) {
            return m_orders.get(i);
        }

        @Override
        public void openOrder(Contract contract, Order order, OrderState orderState) {
            OrderRow full = m_map.get(order.permId());

            if (full != null) {
                full.m_order = order;
                full.m_state = orderState;
                fireTableDataChanged();
            } else if (shouldAdd(contract, order, orderState)) {
                full = new OrderRow(contract, order, orderState);
                add(full);
                m_map.put(order.permId(), full);
                fireTableDataChanged();
            }
        }

        protected boolean shouldAdd(Contract contract, Order order, OrderState orderState) {
            return true;
        }

        protected void add(OrderRow full) {
            m_orders.add(full);
        }

        @Override
        public void openOrderEnd() {
        }

        @Override
        public void orderStatus(int orderId, OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
            OrderRow full = m_map.get(permId);
            if (full != null) {
                full.m_state.status(status);
            }
            fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return 10;
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return "Perm ID";
                case 1:
                    return "Client ID";
                case 2:
                    return "Order ID";
                case 3:
                    return "Account";
                case 4:
                    return "ModelCode";
                case 5:
                    return "Action";
                case 6:
                    return "Quantity";
                case 7:
                    return "Cash Qty";
                case 8:
                    return "Contract";
                case 9:
                    return "Status";
                default:
                    return null;
            }
        }

        @Override
        public Object getValueAt(int row, int col) {
            OrderRow fullOrder = m_orders.get(row);
            Order order = fullOrder.m_order;
            switch (col) {
                case 0:
                    return Util.IntMaxString(order.permId());
                case 1:
                    return Util.IntMaxString(order.clientId());
                case 2:
                    return Util.IntMaxString(order.orderId());
                case 3:
                    return order.account();
                case 4:
                    return order.modelCode();
                case 5:
                    return order.action();
                case 6:
                    return order.totalQuantity();
                case 7:
                    return Util.DoubleMaxString(order.cashQty());
                case 8:
                    return fullOrder.m_contract.textDescription();
                case 9:
                    return fullOrder.m_state.status();
                default:
                    return null;
            }
        }

        @Override
        public void handle(int orderId, int errorCode, String errorMsg) {
        }
    }

    static class OrderRow {
        Contract m_contract;
        Order m_order;
        OrderState m_state;

        OrderRow(Contract contract, Order order, OrderState state) {
            m_contract = contract;
            m_order = order;
            m_state = state;
        }
    }

    static class Key {
        int m_clientId;
        int m_orderId;

        Key(int clientId, int orderId) {
            m_clientId = clientId;
            m_orderId = orderId;
        }
    }
}
