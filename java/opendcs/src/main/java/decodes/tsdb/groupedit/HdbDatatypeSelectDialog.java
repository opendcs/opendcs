/*
 *  $Id$
 *  
 *  Copyright 2017 U.S. Bureau of Reclamation
 */
package decodes.tsdb.groupedit;


import ilex.util.StringPair;
import ilex.util.TextUtil;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import decodes.gui.GuiDialog;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.hdb.HdbDataType;
import decodes.hdb.HdbTimeSeriesDb;
import decodes.sql.DbKey;

/**
 * Dialog for selecting CWMS Location Specification.
 */
@SuppressWarnings("serial")
public class HdbDatatypeSelectDialog 
	extends GuiDialog
{
	private JButton okButton = new JButton("  OK  ");
	private JButton cancelButton = new JButton("Cancel");
	private boolean cancelled;
	private SortingListTable paramTable = null;
	private hdbDatatypeTabkeModel model = null;
	HdbTimeSeriesDb hdbDb;
	private JFrame owner = null;

	public HdbDatatypeSelectDialog(JFrame owner, HdbTimeSeriesDb hdbDb)
	{
		super(owner, "Param Specification", true);
		this.owner = owner;
		this.hdbDb = hdbDb;
		guiInit();
		trackChanges("ParamSelectDialog");

		cancelled = false;
	}

	/** Initialize GUI components. */
	void guiInit()
	{
		JPanel overallPanel = new JPanel(new BorderLayout());
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.setBorder(new TitledBorder(" Select Data Type "));
		
		mainPanel.add(listPanel, BorderLayout.CENTER);
		overallPanel.add(mainPanel, BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
		overallPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		okButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					okPressed();
				}
			});
		cancelButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					cancelPressed();
				}
			});
		buttonPanel.add(okButton, null);
		buttonPanel.add(cancelButton, null);

		getContentPane().add(overallPanel);
		
		model = new hdbDatatypeTabkeModel(this);
		paramTable = new SortingListTable(model, model.colWidths);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().add(paramTable);
		listPanel.add(scrollPane, BorderLayout.CENTER);
		paramTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		paramTable.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					okPressed();
				}
			}
		} );


		getRootPane().setDefaultButton(okButton);
		pack();
	}

	/**
	 * Called when a double click on the selection
	 */

	void okPressed()
	{
		closeDlg();
	}

	/** Closes dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	 * Called when Cancel button is pressed.
	 * 
	 * @param e
	 *            ignored
	 */
	void cancelPressed()
	{
		cancelled = true;
		closeDlg();
	}

	public boolean isCancelled()
	{
		return cancelled;
	}
	
	public StringPair getResult()
	{
		if (isCancelled())
			return null;
		
		int idx = paramTable.getSelectedRow();
		if (idx < 0)
			return null;
		
		HdbDataType hdt = (HdbDataType)model.getRowObject(idx);
		
		return new StringPair(hdt.getDataTypeId().toString(), hdt.getName());
	}
	
	public void setCurrentValue(String dtId)
	{
		if (dtId != null && dtId.trim().length() > 0)
		{
			try
			{
				long x = Long.parseLong(dtId);
				int row = model.getRowOf(DbKey.createDbKey(x));
				if (row >= 0)
					paramTable.setRowSelectionInterval(row, row);
			}
			catch(NumberFormatException ex)
			{}
		}
	}

}

@SuppressWarnings("serial")
class hdbDatatypeTabkeModel extends javax.swing.table.AbstractTableModel
	implements SortingListTableModel
{
	String colNames[] = { "Datatype ID", "Common Name", "Storage Units" };
	int colWidths[] = { 20, 50, 30 };
	private ArrayList<HdbDataType> theList = new ArrayList<HdbDataType>();
	int sortColumn = 0;

	public hdbDatatypeTabkeModel(HdbDatatypeSelectDialog dlg)
	{
		theList.addAll(dlg.hdbDb.getHdbDataTypes());
	}
	
	@Override
	public int getRowCount()
	{
		return theList.size();
	}
	@Override
	public int getColumnCount()
	{
		return colNames.length;
	}
	@Override
	public String getColumnName(int c)
	{
		return colNames[c];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (rowIndex < 0 || rowIndex >= theList.size())
			return "";
		HdbDataType dt = theList.get(rowIndex);
		switch(columnIndex)
		{
		case 0: return "" + dt.getDataTypeId();
		case 1: return dt.getName();
		case 2: return dt.getUnitsAbbr();
		}
		return "";
	}
	
	@Override
	public void sortByColumn(int column)
	{
		sortColumn = column;
		Collections.sort(theList,
			new Comparator<HdbDataType>()
			{
				@Override
				public int compare(HdbDataType o1, HdbDataType o2)
				{
					if (sortColumn == 0)
						return o1.getDataTypeId().compareTo(o2.getDataTypeId());
					if (sortColumn == 1)
					{
						int r = o1.getName().compareTo(o2.getName());
						if (r != 0)
							return r;
					}
					int r = TextUtil.strCompareIgnoreCase(o1.getUnitsAbbr(), o2.getUnitsAbbr());
					if (r != 0)
						return r;
					return o1.getDataTypeId().compareTo(o2.getDataTypeId());
				}
			});

		fireTableDataChanged();
	}
	@Override
	public Object getRowObject(int row)
	{
		if (row < 0)
			return null;
		return theList.get(row);
	}
	
	int getRowOf(DbKey dtId)
	{
		for(int row = 0; row < theList.size(); row++)
			if (theList.get(row).getDataTypeId().equals(dtId))
				return row;
		return -1;
	}
}

