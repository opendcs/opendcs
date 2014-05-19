/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:03  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2004/08/31 16:30:25  mjmaloney
*  javadocs
*
*  Revision 1.4  2003/06/19 20:48:14  mjmaloney
*  Added code to adjust column widths better.
*
*  Revision 1.3  2002/04/06 21:15:26  mike
*  Added code to preserve selections after a sort.
*
*  Revision 1.2  2001/10/20 23:58:29  mike
*  Config Edit Screens.
*
*  Revision 1.1  2001/04/24 00:40:58  mike
*  created.
*
*/
package decodes.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.table.*;

/**
This class handles much of the dirty work for tables that appear within
the DECODES database editor.
It allows you to specify width-weights for the colums.
It allows sorting of the table by clicking in the column headers.
*/
public class SortingListTable extends JTable
{
 	SortingListTableModel model = null;
	TableColumnModel columns = null;
	JTableHeader header = null;
	float widthFactor[];
	private boolean firstPaint;

	/** Default constructor */
    public SortingListTable()
	{
		super();
		widthFactor = null;
		firstPaint = true;
	}

	/**
	  Constructor.
	  @param model the table model.
	  @param widths the column width-weights.
	*/
	public SortingListTable(SortingListTableModel model, int widths[])
	{
		this();
		init(model, widths);
	}

	/**
	  Post-initialization.
	  @param model the table model.
	  @param widths the column width-weights.
	*/
	public void init(SortingListTableModel model, int widths[])
	{
		if (widths != null && widths.length > 0)
		{
			int totalWidth = 0;
			for(int i=0; i<widths.length; i++)
				totalWidth += widths[i];
			if (totalWidth > 0)
			{
				widthFactor = new float[widths.length];
				for(int i=0; i<widths.length; i++)
					widthFactor[i] = (float)widths[i] / (float)totalWidth;
			}
		}
		this.model = model;
		setModel(model);
		this.setAutoCreateColumnsFromModel(false);

		columns = new DefaultTableColumnModel();
		ButtonHeaderRenderer renderer = new ButtonHeaderRenderer();

		for(int i = 0; i < model.getColumnCount(); i++)
		{
	    	TableColumn c = new TableColumn(i);
			c.setHeaderValue(model.getColumnName(i));
			c.setIdentifier("NESS-ID");
			if (widths != null)
			{
		    	c.setPreferredWidth(widths[i]);
				c.setMinWidth(widths[i]);
			}
//			c.setCellEditor(new DcpAddressEditor(new JTextField(), this));
			c.setHeaderRenderer(renderer);
			columns.addColumn(c);
		}

		this.setColumnModel(columns);

		header = getTableHeader();
		header.addMouseListener(new HeaderListener(this, header, renderer));
		setAutoResizeMode(AUTO_RESIZE_NEXT_COLUMN);
		this.getTableHeader().setReorderingAllowed(false);
    }

	/**
	  We need to sets after the object has been activated. The approach is
	  to do it on the first call to the paint method.
	  param g delegated to super
	*/
	public void paint(java.awt.Graphics g)
	{
		if (firstPaint && widthFactor != null)
		{
			firstPaint = false;
			int totWidth = getWidth();
			TableColumnModel colModel = getColumnModel();
			for(int i=0; i<widthFactor.length; i++)
				colModel.getColumn(i).setPreferredWidth(
					(int)(totWidth * widthFactor[i]));
		}
		
		super.paint(g);
	}

	/**
	  This inner-class uses a JButton for the column header, allowing us
	  to act when the column header is clicked.
	*/
	class ButtonHeaderRenderer extends JButton
		implements TableCellRenderer
	{
		int pushedColumn;

		public ButtonHeaderRenderer()
		{
		    pushedColumn   = -1;
		    setMargin(new java.awt.Insets(0,0,0,0));
		}

		public java.awt.Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus, int row,
			int column)
		{
			setText((value ==null) ? "" : value.toString());
		    boolean isPressed = (column == pushedColumn);
		    getModel().setPressed(isPressed);
		    getModel().setArmed(isPressed);
		    return this;
		}

		public void setPressedColumn(int col)
		{
		    pushedColumn = col;
		}
	}

	/**
	  This class allows us to capture the mouse clicks on the header.
	*/
	class HeaderListener extends MouseAdapter
	{
		JTableHeader   header;
		ButtonHeaderRenderer renderer;
		SortingListTable table;

		HeaderListener(SortingListTable table, JTableHeader header,
			ButtonHeaderRenderer renderer)
		{
			this.table = table;
		    this.header = header;
		    this.renderer = renderer;
		}

		public void mousePressed(MouseEvent e)
		{
			// Determine which row objects were selected before sort.
			int selectedRows[] = table.getSelectedRows();
			Object selectedObjects[] = new Object[selectedRows.length];
			for(int i=0; i<selectedRows.length; i++)
				selectedObjects[i] = model.getRowObject(selectedRows[i]);

		    int col = header.columnAtPoint(e.getPoint());
		    renderer.setPressedColumn(col);
		    header.repaint();
			model.sortByColumn(col);

			// Now re-select the objects that were selected before the sort.
			table.clearSelection();
			int rowCount = model.getRowCount(); 
			for(int i=0; i<selectedObjects.length; i++)
			{
				for (int r = 0; r < rowCount; r++)
					if (selectedObjects[i] == model.getRowObject(r))
					{
						table.setRowSelectionInterval(r, r);
						break;
					}
			}

			table.invalidate();
			table.repaint();
		}

		public void mouseReleased(MouseEvent e)
		{
		    int col = header.columnAtPoint(e.getPoint());
		    renderer.setPressedColumn(-1);                // clear
		    header.repaint();
		}
	}
}
