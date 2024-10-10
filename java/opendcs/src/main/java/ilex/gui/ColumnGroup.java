package ilex.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * 
 * This class is used by the Table on the TimeSeriesChartPanel class
 * 
 */
public class ColumnGroup
{
	protected TableCellRenderer renderer;
	protected Vector v;
	protected String text;
	protected int margin = 0;

	public ColumnGroup(String text)
	{
		this(null, text);
	}

	public ColumnGroup(TableCellRenderer renderer, String text)
	{
		if (renderer == null)
		{
			this.renderer = new DefaultTableCellRenderer()
			{
				public Component getTableCellRendererComponent(JTable table,
						Object value, boolean isSelected, boolean hasFocus,
						int row, int column)
				{
					JTableHeader header = table.getTableHeader();
					if (header != null)
					{
						setForeground(header.getForeground());
						setBackground(header.getBackground());
						setFont(header.getFont());
					}
					setHorizontalAlignment(JLabel.CENTER);
					setText((value == null) ? "" : value.toString());
					setBorder(UIManager.getBorder("TableHeader.cellBorder"));
					return this;
				}
			};
		} else
		{
			this.renderer = renderer;
		}
		this.text = text;
		v = new Vector();
	}

	/**
	 * @param obj
	 *            TableColumn or ColumnGroup
	 */
	public void add(Object obj)
	{
		if (obj == null)
		{
			return;
		}
		v.addElement(obj);
	}

	/**
	 * @param c
	 *            TableColumn
	 * @param v
	 *            ColumnGroups
	 */
	public Vector getColumnGroups(TableColumn c, Vector g)
	{
		g.addElement(this);
		if (v.contains(c))
			return g;
		Enumeration e = v.elements();
		while (e.hasMoreElements())
		{
			Object obj = e.nextElement();
			if (obj instanceof ColumnGroup)
			{
				Vector groups = (Vector) ((ColumnGroup) obj).getColumnGroups(c,
						(Vector) g.clone());
				if (groups != null)
					return groups;
			}
		}
		return null;
	}

	public TableCellRenderer getHeaderRenderer()
	{
		return renderer;
	}

	public void setHeaderRenderer(TableCellRenderer renderer)
	{
		if (renderer != null)
		{
			this.renderer = renderer;
		}
	}

	public Object getHeaderValue()
	{
		return text;
	}

	public Dimension getSize(JTable table)
	{
		Component comp = renderer.getTableCellRendererComponent(table,
				getHeaderValue(), false, false, -1, -1);
		int height = comp.getPreferredSize().height;
		int width = 0;
		Enumeration e = v.elements();
		while (e.hasMoreElements())
		{
			Object obj = e.nextElement();
			if (obj instanceof TableColumn)
			{
				TableColumn aColumn = (TableColumn) obj;
				width += aColumn.getWidth();
				// width += margin;
				width += margin - 1;// Josue to make header align with table
									// model columns
			} else
			{
				width += ((ColumnGroup) obj).getSize(table).width;
			}
		}
		return new Dimension(width, height);
	}

	public void setColumnMargin(int margin)
	{
		this.margin = margin;
		Enumeration e = v.elements();
		while (e.hasMoreElements())
		{
			Object obj = e.nextElement();
			if (obj instanceof ColumnGroup)
			{
				((ColumnGroup) obj).setColumnMargin(margin);
			}
		}
	}
}
