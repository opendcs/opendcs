/*
*  $Id$
*/
package decodes.tsdb.compedit;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.*;
import javax.swing.table.AbstractTableModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import decodes.gui.*;

public class DataTypeSelectDialog extends GuiDialog
{
    JButton selectButton = new JButton();
    JButton cancelButton = new JButton();

	JPanel dtSelectPanel = new JPanel();
	JScrollPane jScrollPane1 = new JScrollPane();
	JTable dtListTable;
	public static boolean isHdb = false;
	boolean okPressed = false;
	int selectedIndex = -1;
	String header[];
	ArrayList<String[]> dataTypes;
	private String siteName;

	/** 
	  Construct new dialog.
	*/
    public DataTypeSelectDialog(JDialog parent, String siteName, 
		ArrayList<String[]> dts)
	{
        super(parent, CAPEdit.instance().compeditDescriptions
	    		.getString("DataTypeSelectDialog.SelectDataType") + " " + siteName, true);
		this.siteName = siteName;
		this.header = dts.get(0);
		this.dataTypes = new ArrayList<String[]>();
		for(int i=1; i< dts.size(); i++)
			this.dataTypes.add(dts.get(i));
		allInit();
	}

    public DataTypeSelectDialog(JFrame parent, String siteName, 
    		ArrayList<String[]> dts)
	{
		super(parent, CAPEdit.instance().compeditDescriptions
	    		.getString("DataTypeSelectDialog.SelectDataType") + " " + siteName, true);
		this.siteName = siteName;
		this.header = dts.get(0);
		this.dataTypes = new ArrayList<String[]>();
		for(int i=1; i< dts.size(); i++)
		this.dataTypes.add(dts.get(i));
		allInit();
	}

	private void allInit()
	{
        try
		{
            jbInit();
            pack();
			
			dtSelectPanel.setLayout(new BorderLayout());
//			jScrollPane1.setMinimumSize(new Dimension(453, 300));
        	dtSelectPanel.add(jScrollPane1, BorderLayout.CENTER);

			SortingListTableModel tabmod = 
				new DTListTableModel(header, dataTypes, isHdb);
			int widths[] = new int[header.length];
			for(int i=0; i< header.length; i++)
				widths[i] = 100 / header.length;
			dtListTable = new SortingListTable(tabmod, widths);

			jScrollPane1.getViewport().add(dtListTable, null);
			getRootPane().setDefaultButton(selectButton);
            pack();
        }
        catch(Exception ex) 
		{
            ex.printStackTrace();
        }
    }

	/** Initializes GUI components */
    void jbInit() throws Exception 
	{
        JPanel overallPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 35, 10));
        selectButton.setText(CAPEdit.instance().genericDescriptions
        		.getString("select"));
        selectButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
            	public void actionPerformed(ActionEvent e) 
				{
					selectButtonPressed(); 
				}
        	});
        cancelButton.setText(CAPEdit.instance().genericDescriptions
        		.getString("cancel"));
        cancelButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
            	public void actionPerformed(ActionEvent e) 
				{
					cancelButtonPressed(); 
				}
        	});
        buttonPanel.add(selectButton, null);
        buttonPanel.add(cancelButton, null);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(new TitledBorder("Matching Time Series for site '" + siteName + "'"));
        tablePanel.add(dtSelectPanel, BorderLayout.CENTER);

        overallPanel.add(buttonPanel, BorderLayout.SOUTH);
        overallPanel.add(tablePanel, BorderLayout.CENTER);
        getContentPane().add(overallPanel);
    }

	/**
	  Called when the Select button is pressed.
	*/
    void selectButtonPressed()
	{
		int row = dtListTable.getSelectedRow();
		if (row == -1)
		{
			return;
		}
		//Get the correct row from the table model
		selectedIndex = dtListTable.convertRowIndexToModel(row);
		okPressed = true;
		closeDlg();
    }

	/** Closes the dialog */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	  Called when Cancel the button is pressed.
	*/
    void cancelButtonPressed()
	{
		selectedIndex = -1;
		okPressed = false;
		closeDlg();
    }

	/** Returns the selected index or -1 if none selected. */
	public String[] getSelection()
	{
		if (selectedIndex == -1)
			return null;
		return dataTypes.get(selectedIndex);
	}
	
	public void allowMultipleSelection(boolean tf)
	{
		dtListTable.setSelectionMode(
			tf ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION :
				 ListSelectionModel.SINGLE_SELECTION);
	}
}

class DTListTableModel 
	extends AbstractTableModel 
	implements SortingListTableModel 
{
	ArrayList<String[]> dataTypes;
	String header[];

	int sortedBy = -1;
	int columns, rows;
	private boolean isHdb = false;

	DTListTableModel(String[] header, ArrayList<String[]> dataTypes, boolean isHdb)
	{
		this.header = header;
		columns = header.length;
		this.dataTypes = dataTypes;
		rows = dataTypes.size();
		this.isHdb = isHdb;
	}

	public void sortByColumn(int c)
	{
		Collections.sort(dataTypes, new DTListComparator(c, isHdb));
		sortedBy = c;
		fireTableDataChanged();
	}
	
	public Object getRowObject(int arg0) 
	{
		return dataTypes.get(arg0);
	}

	public int getRowCount() 
	{
		return rows;
	}

	public int getColumnCount() 
	{
		return columns;
	}
	
	public String getColumnName(int col)
	{
		return header[col];
	}

	public Object getValueAt(int rowIndex, int columnIndex) 
	{
		return dataTypes.get(rowIndex)[columnIndex];
	}
	
	public static String getObjectColumn(Object obj, int columnIndex) 
	{
		String[] row = (String[])obj;
		return row[columnIndex];
	}
}

class DTListComparator implements Comparator
{
	int column;
	boolean isHdb = false;

	public DTListComparator(int column, boolean isHdb)
	{
		this.column = column;
		this.isHdb = isHdb;
//System.out.println("DTListComparator column=" + column + ", isHdb=" + isHdb);
	}

	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		String r1[] = (String[])ob1;
		String r2[] = (String[])ob2;
		if (column == 0 && isHdb)
		{
			try
			{
				return Integer.parseInt(r1[0]) - Integer.parseInt(r2[0]);
			}
			catch(Exception ex)
			{
//System.out.println("numeric compare failed ob1='" + r1[0] + "', ob2='" + r2[0] + "'");
			} // Fall through and compare strings
		}

		return r1[column].compareTo(r2[column]);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
