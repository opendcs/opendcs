/*
 *  $Id$
 *  
 *  Copyright 2016 U.S. Army Corps of Engineers, Institute for Water Resources, Hydrologic Engineering Center (HEC).
 */
package decodes.tsdb.groupedit;


import ilex.util.Logger;
import ilex.util.StringPair;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import opendcs.dai.TimeSeriesDAI;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.gui.GuiDialog;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroupMemberType;

/**
 * Dialog for selecting CWMS Location Specification.
 */
@SuppressWarnings("serial")
public class VersionSelectDialog 
	extends GuiDialog
{
	private JButton okButton = new JButton("  OK  ");
	private JButton cancelButton = new JButton("Cancel");
	private boolean cancelled;
	private SortingListTable versionTable = null;
	private VersionTableModel model = null;
	CwmsTimeSeriesDb cwmsDb;
	private JRadioButton fullRadio = new JRadioButton("Full Version");
	private JRadioButton baseRadio = new JRadioButton("Base Version");
	private JRadioButton subRadio = new JRadioButton("Sub Version");
	private JTextField resultField = new JTextField(15);
//	private JTextField baseField = new JTextField();
//	private JTextField subField = new JTextField();
	private CwmsBaseSubPartSpec selectedRow = null;
	private JFrame owner = null;
//	private boolean allowBaseSub = true;
	SelectionMode selectionMode = SelectionMode.GroupEdit;

	public VersionSelectDialog(JFrame owner, CwmsTimeSeriesDb cwmsDb, SelectionMode selectionMode)
	{
		super(owner, "Version Specification", true);
		this.owner = owner;
		this.cwmsDb = cwmsDb;
		this.selectionMode = selectionMode;
		guiInit();
		cancelled = false;
	}

	/** Initialize GUI components. */
	void guiInit()
	{
		JPanel overallPanel = new JPanel(new BorderLayout());
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.setBorder(new TitledBorder(" Select Version "));
		JPanel radioPanel = new JPanel(new GridBagLayout());
		
		radioPanel.setBorder(new TitledBorder(
			(owner instanceof TsDbGrpEditorFrame) ? " Add Filter By " : " Add Mask By "));

		mainPanel.add(listPanel, BorderLayout.CENTER);
		mainPanel.add(radioPanel, BorderLayout.SOUTH);
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
		
		model = new VersionTableModel(this);
		versionTable = 
			new SortingListTable(model, model.colWidths)
			{
			
			};
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().add(versionTable);
		listPanel.add(scrollPane, BorderLayout.CENTER);
		versionTable.getSelectionModel().addListSelectionListener(
			new ListSelectionListener()
			{
				@Override
				public void valueChanged(ListSelectionEvent e)
				{
					selectionMade();
				}
			});

		ButtonGroup filterRadios = new ButtonGroup();
		filterRadios.add(fullRadio);
		filterRadios.add(baseRadio);
		filterRadios.add(subRadio);
		fullRadio.setSelected(true);
		fullRadio.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					selectionMade();
				}
			});
		baseRadio.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					selectionMade();
				}
			});
		subRadio.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					selectionMade();
				}
			});

		radioPanel.add(fullRadio,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 1), 0, 0));
		radioPanel.add(new JLabel("Result:"),
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.SOUTH, GridBagConstraints.NONE,
				new Insets(2, 40, 2, 20), 10, 0));
		radioPanel.add(baseRadio,
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 1), 0, 0));
		radioPanel.add(resultField,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 40, 2, 10), 10, 0));
		radioPanel.add(subRadio,
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 1), 0, 0));
		
		versionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		if (selectionMode == SelectionMode.CompEditNoGroup)
		{
			baseRadio.setEnabled(false);
			subRadio.setEnabled(false);
		}

		resultField.setEditable(selectionMode != SelectionMode.CompEditNoGroup);

		getRootPane().setDefaultButton(okButton);
		pack();
	}
	
	private boolean inSelectionMade = false;

	protected void selectionMade()
	{
		if (inSelectionMade)
			return;
		inSelectionMade = true;
		
		int r = versionTable.getSelectedRow();
		if (r >= 0)
		{
			CwmsBaseSubPartSpec spec = (CwmsBaseSubPartSpec)model.getRowObject(r);
			if (spec != selectedRow)
			{
				selectedRow = spec;
				if (selectionMode == SelectionMode.GroupEdit || selectionMode == SelectionMode.CompEditGroup)
				{
					if (spec.getSub() == null || spec.getSub().length() == 0)
					{
						if (subRadio.isSelected())
						{
							subRadio.setSelected(false);
							fullRadio.setSelected(true);
						}
						subRadio.setEnabled(false);
					}
					else if (subRadio.isEnabled() == false)
						subRadio.setEnabled(true);
				}
			}
			switch(selectionMode)
			{
			case GroupEdit:
				if (fullRadio.isSelected())
					resultField.setText(spec.getFull());
				else if (baseRadio.isSelected())
					resultField.setText(spec.getBase());
				else //sub radio
					resultField.setText(spec.getSub());
				break;
			case CompEditNoGroup:
				resultField.setText(spec.getFull());
				break;
			case CompEditGroup:
				if (fullRadio.isSelected())
					resultField.setText(spec.getFull());
				else if (baseRadio.isSelected())
					resultField.setText(spec.getBase() + "-*");
				else //sub radio
					resultField.setText("*-" + spec.getSub());
				break;
			}
		}
		
		inSelectionMade = false;
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
		String label, value;
		if (fullRadio.isSelected())
			label = TsGroupMemberType.Version.toString();
		else if (baseRadio.isSelected())
			label = TsGroupMemberType.BaseVersion.toString();
		else
			label = TsGroupMemberType.SubVersion.toString();
		
		value = resultField.getText();
		if (value.length() == 0)
			return null;
		return new StringPair(label, value);
	}

	public void setCurrentValue(String value)
	{
		if (value == null)
			return;
		String v = value;
		v = v.trim();
		if (v.length() == 0)
			return;
		if (v.startsWith("*-"))
		{
			v = v.substring(2);
			subRadio.setSelected(true);
			for(int idx = 0; idx < model.theList.size(); idx++)
				if (model.theList.get(idx).sub.equalsIgnoreCase(v))
				{
					versionTable.setRowSelectionInterval(idx, idx);
					break;
				}
		}
		else if (v.endsWith("-*"))
		{
			v = v.substring(0, v.length()-2);
			baseRadio.setSelected(true);
			for(int idx = 0; idx < model.theList.size(); idx++)
				if (model.theList.get(idx).base.equalsIgnoreCase(v))
				{
					versionTable.setRowSelectionInterval(idx, idx);
					break;
				}
		}
		else
		{
			fullRadio.setSelected(true);
			for(int idx = 0; idx < model.theList.size(); idx++)
				if (model.theList.get(idx).full.equalsIgnoreCase(v))
				{
					versionTable.setRowSelectionInterval(idx, idx);
					break;
				}
		}
		selectionMade();
		resultField.setText(value);
	}


}

@SuppressWarnings("serial")
class VersionTableModel extends javax.swing.table.AbstractTableModel
	implements SortingListTableModel
{
	String colNames[] = { "Base", "Sub", "Num TSIDs" };
	int colWidths[] = { 40, 30, 20 };
	private HashMap<StringPair, CwmsBaseSubPartSpec> bs2spec = new HashMap<StringPair, CwmsBaseSubPartSpec>();
	ArrayList<CwmsBaseSubPartSpec> theList = new ArrayList<CwmsBaseSubPartSpec>();

	private Comparator<CwmsBaseSubPartSpec> baseComparator = 
		new Comparator<CwmsBaseSubPartSpec>()
		{
			@Override
			public int compare(CwmsBaseSubPartSpec o1, CwmsBaseSubPartSpec o2)
			{
				int r = o1.base.compareTo(o2.base);
				if (r != 0) return r;
				return o1.sub.compareTo(o2.sub);
				// note: base,sub must be unique.
			}
		};
		
	private Comparator<CwmsBaseSubPartSpec> subComparator = 
		new Comparator<CwmsBaseSubPartSpec>()
		{
			@Override
			public int compare(CwmsBaseSubPartSpec o1, CwmsBaseSubPartSpec o2)
			{
				if (o1.sub.length() == 0)
				{
					if (o2.sub.length() > 0)
						return 1;
				}
				else if (o2.sub.length() == 0)
					return -1;
				int r = o1.sub.compareTo(o2.sub);
				if (r != 0) return r;
				return o1.base.compareTo(o2.base);
				// note: base,sub must be unique.
			}
		};
	private Comparator<CwmsBaseSubPartSpec> numComparator = 
		new Comparator<CwmsBaseSubPartSpec>()
		{
			@Override
			public int compare(CwmsBaseSubPartSpec o1, CwmsBaseSubPartSpec o2)
			{
				int r = o1.numTsids - o2.numTsids;
				if (r != 0) return r;
				r = o1.base.compareTo(o2.base);
				if (r != 0) return r;
				return o1.sub.compareTo(o2.sub);
				// note: base,sub must be unique.
			}
		};
	
	public VersionTableModel(VersionSelectDialog dlg)
	{
		TimeSeriesDAI tsDao = dlg.cwmsDb.makeTimeSeriesDAO();
		
		try
		{
			ArrayList<TimeSeriesIdentifier> tslist = tsDao.listTimeSeries();
			for(TimeSeriesIdentifier tsid : tslist)
			{
				CwmsTsId cwmsTsId = (CwmsTsId)tsid;
				String sub = cwmsTsId.getSubVersion();
				if (sub == null)
					sub = "";
				StringPair sp = new StringPair(cwmsTsId.getBaseVersion(), sub);
				CwmsBaseSubPartSpec spec = bs2spec.get(sp);
				if (spec != null)
					spec.incNumTsids();
				else
				{
					spec = new CwmsBaseSubPartSpec(cwmsTsId.getBaseVersion(), sub, 1,
						cwmsTsId.getVersion());
					bs2spec.put(sp, spec);
					theList.add(spec);
				}
			}
		}
		catch (DbIoException ex)
		{
			Logger.instance().warning("Error listing time series: " + ex);
		}
		finally
		{
			tsDao.close();
		}
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
		CwmsBaseSubPartSpec spec = theList.get(rowIndex);
		switch(columnIndex)
		{
		case 0: return spec.base;
		case 1: return spec.sub;
		case 2: return "" + spec.numTsids;
		}
		return "";
	}
	
	@Override
	public void sortByColumn(int column)
	{
		Collections.sort(theList,
			column == 0 ? baseComparator
			: column == 1 ? subComparator
			: numComparator);
		fireTableDataChanged();
	}
	@Override
	public Object getRowObject(int row)
	{
		if (row < 0)
			return null;
		return theList.get(row);
	}	
}

