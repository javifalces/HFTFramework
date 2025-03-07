package com.ib.samples.apidemo;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class WSHMetaDataModel extends AbstractTableModel {

    private class EventDataItem {

        public EventDataItem(String dataJson) {
            m_dataJson = dataJson;
        }

        public String dataJSON() {
            return m_dataJson;
        }

        private String m_dataJson;

    }

    private List<EventDataItem> m_rows = new ArrayList<>();
    private static String[] columnNames = new String[]{"Data JSON"};

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public int getRowCount() {
        return m_rows.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        EventDataItem item = m_rows.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return item.dataJSON();
        }

        return null;
    }

    public void addRow(String dataJson) {
        m_rows.add(new EventDataItem(dataJson));

        fireTableDataChanged();
    }

}
