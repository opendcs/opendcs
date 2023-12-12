package org.opendcs.gui.tables;

import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Given a SimpleDateFormat instance handles rendering date in {@link javax.swing.JTable}
 * cells.
 */
public class DateRenderer extends JLabel implements TableCellRenderer
{
    private final SimpleDateFormat lmtFormat;

    public DateRenderer(SimpleDateFormat format)
    {
        lmtFormat = format;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column)
    {
        setText(lmtFormat.format((Date)value));
        return this;
    }
}
