/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lrgs.nledit;

import ilex.util.LoadResourceBundle;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.table.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.*;
import java.util.ResourceBundle;

import lrgs.common.NetworkListItem;

public class NetworkListTable extends JTable
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
				filename);
			log.atError().setCause(ioe).log(err);
			JOptionPane.showMessageDialog(this,
				ilex.util.AsciiUtil.wrapString(err + ": " + ioe, 60), "Error!",
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
					filename);
			log.atError().setCause(ioe).log(err);
			JOptionPane.showMessageDialog(this,
				ilex.util.AsciiUtil.wrapString(err + ": " + ioe, 60), "Error!",
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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
				log.error(err);
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