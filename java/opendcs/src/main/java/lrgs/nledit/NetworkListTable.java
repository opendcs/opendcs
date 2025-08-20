/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2008/08/19 16:38:15  mjmaloney
*  DcpAddress stores internal value as String.
*
*  Revision 1.1  2008/04/04 18:21:15  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2008/01/25 14:44:10  mmaloney
*  modified files for internationalization
*
*  Revision 1.4  2002/02/09 21:25:34  mike
*  Fixed bug: close cell editors before exit so last change is saved.
*
*  Revision 1.3  2001/02/23 03:04:01  mike
*  Working version.
*
*  Revision 1.2  2001/02/21 14:49:29  mike
*  dev
*
*  Revision 1.1  2001/02/21 13:19:29  mike
*  Created nleditor
*
*/
package lrgs.nledit;

import ilex.util.LoadResourceBundle;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.table.*;
import java.io.*;
import java.util.ResourceBundle;

import lrgs.common.DcpAddress;
import lrgs.common.NetworkListItem;

public class NetworkListTable extends JTable
{
	private static ResourceBundle labels = 
		NetlistEditor.getLabels();
	private static ResourceBundle genericLabels = 
		NetlistEditor.getGenericLabels();
	NetworkListTableModel model = null;
	TableColumnModel columns;
	JTableHeader header;

    public NetworkListTable()
	{
		super();
		model = new NetworkListTableModel();
		setModel(model);
		this.setAutoCreateColumnsFromModel(false);
		columns = new DefaultTableColumnModel();
		ButtonHeaderRenderer renderer = new ButtonHeaderRenderer();

		TableColumn c = new TableColumn(0);
		c.setHeaderValue(
				labels.getString("NetworkListTable.NESSIdColumn"));
		c.setIdentifier("NESS-ID");
		c.setPreferredWidth(80);
		c.setMinWidth(80);
		c.setCellEditor(new DcpAddressEditor(new JTextField(), this));
		c.setHeaderRenderer(renderer);
		columns.addColumn(c);

		c = new TableColumn(1);
		c.setHeaderValue(genericLabels.getString("name"));
		c.setIdentifier("Name");
		c.setPreferredWidth(200);
		c.setCellEditor(new DcpNameEditor(new JTextField(), this));
		c.setHeaderRenderer(renderer);
		columns.addColumn(c);

		c = new TableColumn(2);
		c.setHeaderValue(genericLabels.getString("description"));
		c.setIdentifier("Description");
		c.setPreferredWidth(400);
		c.setHeaderRenderer(renderer);
		columns.addColumn(c);

		this.setColumnModel(columns);

		header = getTableHeader();
		header.addMouseListener(new HeaderListener(this, header,renderer));
		setAutoResizeMode(AUTO_RESIZE_NEXT_COLUMN);

    }

	public void mergeFile(String filename)
	{
		try
		{
			model.mergeFile(new File(filename));
			invalidate();
			repaint();
		}
		catch(IOException ioe)
		{
			String err = 
				LoadResourceBundle.sprintf(
				labels.getString("NetworkListTable.cannotLoadErr"),
				filename) + ioe.toString();
			System.err.println(err);
			JOptionPane.showMessageDialog(this,
				ilex.util.AsciiUtil.wrapString(err, 60), "Error!",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	public void loadFile(String filename)
	{
		try
		{
			model.loadFile(new File(filename));
			invalidate();
			repaint();
		}
		catch(IOException ioe)
		{
			String err = LoadResourceBundle.sprintf(
					labels.getString("NetworkListTable.cannotLoadErr"),
					filename) + ioe.toString();
			System.err.println(err);
			JOptionPane.showMessageDialog(this,
				ilex.util.AsciiUtil.wrapString(err, 60), "Error!",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	public void saveFile(String filename)
	{
		TableCellEditor tce = getCellEditor();
		if (tce != null)
			tce.stopCellEditing();
		try
		{
			model.saveFile(new File(filename));
		}
		catch(IOException ioe)
		{
			String err = LoadResourceBundle.sprintf(
					labels.getString("NetworkListTable.cannotSaveErr"),
					filename) + ioe.toString();
			System.err.println(err);
			JOptionPane.showMessageDialog(this,
				ilex.util.AsciiUtil.wrapString(err, 60), "Error!",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	public boolean isModified() 
	{
		TableCellEditor tce = getCellEditor();
		if (tce != null)
			tce.stopCellEditing();

		return model.isModified(); 
	}

	public void clear()
	{
		model.clear();
		invalidate();
		repaint();
	}

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

	class HeaderListener extends MouseAdapter
	{
		JTableHeader   header;
		ButtonHeaderRenderer renderer;
		NetworkListTable table;

		HeaderListener(NetworkListTable table, JTableHeader header,
			ButtonHeaderRenderer renderer)
		{
			this.table = table;
		    this.header   = header;
		    this.renderer = renderer;
		}

		public void mousePressed(MouseEvent e)
		{
		    int col = header.columnAtPoint(e.getPoint());
		    renderer.setPressedColumn(col);
		    header.repaint();
			if (col == 0)
				table.model.sortByAddress();
			else if (col == 1)
				table.model.sortByName();
			else if (col == 2)
				table.model.sortByDescription();
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

	NetworkListItem getItemAt(int row)
	{
		return model.getItemAt(row);
	}
	void deleteItemAt(int row)
	{
		model.deleteItemAt(row);
	}

	public void insertBefore()
	{
		int n = getRowCount();
		for(int i = 0; i < n; i++)
			if (isRowSelected(i))
			{
				model.insertBefore(i);
				return;
			}
		model.insertBefore(n);
	}

	public void insertAfter()
	{
		int n = getRowCount();
		for(int i = 0; i < n; i++)
			if (isRowSelected(i))
			{
				model.insertAfter(i);
				return;
			}
		model.insertBefore(n);
	}
}

class DcpAddressEditor extends DefaultCellEditor
{
	JTextField textField;
	JTable table;
	private static ResourceBundle labels = 
		NetlistEditor.getLabels();
	
	public DcpAddressEditor(JTextField textField, JTable table)
	{
		super(textField);
		this.textField = textField;
		this.table = table;
	}
	public boolean stopCellEditing()
	{
		String v = textField.getText();
		fireEditingStopped();
		return true;
	}

}

class DcpNameEditor extends DefaultCellEditor
{
	JTextField textField;
	JTable table;
	private static ResourceBundle labels = 
		NetlistEditor.getLabels();
	public DcpNameEditor(JTextField textField, JTable table)
	{
		super(textField);
		this.textField = textField;
		this.table = table;
	}
	public boolean stopCellEditing()
	{
		String v = textField.getText();
		int n = v.length();
		for(int i=0; i<n; i++)
		{
			char c = v.charAt(i);
			if (c == ':' || Character.isWhitespace(c))
			{
				String err = LoadResourceBundle.sprintf(
						labels.getString("NetworkListTable.badDCPNameErr"),
						v);
				System.err.println(err);
	    		JOptionPane.showMessageDialog(table,
	    			ilex.util.AsciiUtil.wrapString(err, 60), "Error!",
	    			JOptionPane.ERROR_MESSAGE);

		    	return false;
		    }
		}

		fireEditingStopped();
		return true;
	}
}
