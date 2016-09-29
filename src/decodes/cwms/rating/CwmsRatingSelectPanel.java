/**
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.4  2012/11/06 20:47:37  mmaloney
 * dev
 *
 * Revision 1.3  2012/10/30 15:46:37  mmaloney
 * dev
 *
 * Revision 1.2  2012/10/30 13:21:24  mmaloney
 * dev
 *
 * Revision 1.1  2012/10/30 01:59:27  mmaloney
 * First cut of rating GUI.
 *
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.cwms.rating;

import ilex.util.Logger;

import java.awt.BorderLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.gui.TopFrame;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * Displays a sorting-list of CWMS Rating objects in the database.
 */
@SuppressWarnings("serial")
public class CwmsRatingSelectPanel extends JPanel
{
	public String module = "CwmsRatingSelectPanel";
	private JScrollPane jScrollPane;
	private CwmsRatingTableModel model;
	private SortingListTable ratingListTable;

	/** Constructor. 
	 * @throws DbIoException 
	 */
	public CwmsRatingSelectPanel(TimeSeriesDb theTsDb)
		throws DbIoException
	{
		model = new CwmsRatingTableModel(theTsDb);
		ratingListTable = new SortingListTable(model, model.columnWidths);
		setMultipleSelection(false);

		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void setMultipleSelection(boolean ok)
	{
		ratingListTable.getSelectionModel().setSelectionMode(
			ok ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
				: ListSelectionModel.SINGLE_SELECTION);
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception
	{
		jScrollPane = new JScrollPane();
		
		setLayout(new BorderLayout());
		add(jScrollPane, BorderLayout.CENTER);
		jScrollPane.getViewport().add(ratingListTable, null);
	}

	/**
	 * @return the currently-selected rating, or null if none selected
	 */
	public CwmsRatingRef getSelectedRating()
	{
		int r = ratingListTable.getSelectedRow();
		if (r == -1)
			return null;
		return model.getAt(r);
	}

	public void refresh()
		throws DbIoException
	{
		model.refresh();
	}

	/** Resorts the list by the current column selection. */
	public void reSort()
	{
		model.reSort();
	}

	/**
	 * Deletes the currently-selected rating from the list.
	 */
	public void deleteRating()
	{
		int r = ratingListTable.getSelectedRow();
		if (r == -1)
			return;
		model.deleteAt(r);
	}

	public int[] getSelectedRows()
	{
		return ratingListTable.getSelectedRows();
	}
	
	public int getSelectedRowCount()
	{
		return ratingListTable.getSelectedRowCount();
	}
	
	public void clearSelection()
	{
		ratingListTable.clearSelection();
	}
}

/**
 * The DataDescriptorSelectTableModel class is used as the table model 
 * for SortingListTable derived from JTable. This class allows to fetch
 * the data descriptor info into the table and provides methods to access
 * the table data set from the table object.
 */
@SuppressWarnings("serial")
class CwmsRatingTableModel extends AbstractTableModel implements
	SortingListTableModel
{
	private TimeSeriesDb theTsDb;
	private String[] columnNames;
	int[] columnWidths;
	
	private int sortColumn = -1;
	
	private ArrayList<CwmsRatingRef> cwmsRatings = new ArrayList<CwmsRatingRef>();

	public CwmsRatingTableModel(TimeSeriesDb theTsDb)
		throws DbIoException
	{
		super();
		this.theTsDb = theTsDb;
		loadRatings();
		sortByColumn(0);
	}
	
	private void loadRatings()
		throws DbIoException
	{
		CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)theTsDb);
		cwmsRatings.clear();
		List<CwmsRatingRef> cr;
		cr = crd.listRatings(null);
		for(CwmsRatingRef crr : cr)
			cwmsRatings.add(crr);
		int maxIndep = 0;
		for(CwmsRatingRef crr : cwmsRatings)
		{
			if (crr.getIndep().length > maxIndep)
				maxIndep = crr.getIndep().length;
		}
		columnNames = new String[maxIndep + 5];
		columnNames[0] = "Location";
		for(int i=0; i<maxIndep; i++)
			columnNames[i+1] = "Indep" + (i+1);
		columnNames[maxIndep+1] = "Dep";
		columnNames[maxIndep+2] = "Tpl Version";
		columnNames[maxIndep+3] = "Spec Version";
		columnNames[maxIndep+4] = "Effective";
		
		columnWidths = new int[maxIndep+5];
		for(int i=0; i<maxIndep+5; i++)
			columnWidths[i] = 100/(maxIndep+5);
		RatingColumnizer.numIndep = maxIndep;
	}
	
	/**
	 * Refresh the list with the time series data descriptors 
	 * from the database.
	 */
	public void refresh()
		throws DbIoException
	{
		loadRatings();
		reSort();
		fireTableDataChanged();
	}

	public void deleteAt(int index)
	{
		CwmsRatingRef crr = cwmsRatings.remove(index);
		fireTableDataChanged();
	}

	public CwmsRatingRef getAt(int r)
	{
		return (CwmsRatingRef)getRowObject(r);
	}

	public int getColumnCount()
	{
		return columnNames.length;
	}

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public boolean isCellEditable(int r, int c)
	{
		return false;
	}

	public int getRowCount()
	{
		return cwmsRatings.size();
	}

	public Object getValueAt(int r, int c)
	{
		return RatingColumnizer.getColumn(getAt(r), c);
	}

	public Object getRowObject(int r)
	{
		return cwmsRatings.get(r);
	}

	public void sortByColumn(int c)
	{
		sortColumn = c;
		Collections.sort(cwmsRatings, new RatingComparator(c));
	}

	public void reSort()
	{
		if (sortColumn >= 0)
			sortByColumn(sortColumn);
	}
}

/**
 * Helper class to retrieve TimeSeries Data Descriptor fields by column number.
 * Used for displaying values in the table and for sorting.
 */
class RatingColumnizer
{
	static int numIndep;
	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	static
	{	sdf.setTimeZone(TimeZone.getTimeZone("UTC")); }
	
	static String getColumn(CwmsRatingRef crr, int c)
	{
		if (c == 0)
			return crr.getLocation();
		else if (c < 1 + numIndep)
		{
			if (c-1 < crr.getIndep().length)
				return crr.getIndep()[c-1];
			else
				return "";
		}
		c -= (1 + numIndep);
		switch(c)
		{
		case 0: return crr.getDep();
		case 1: return crr.getTplVersion();
		case 2: return crr.getSpecVersion();
		case 3: return crr.getEffectiveDate() != null ?
			sdf.format(crr.getEffectiveDate()) : "";
		default: return "";
		}
	}
}

class RatingComparator implements Comparator
{
	int col;

	RatingComparator(int col)
	{
		this.col = col;
	}

	public int compare(Object dd1, Object dd2)
	{
		if (dd1 == dd2)
			return 0;
		
		CwmsRatingRef d1 = (CwmsRatingRef) dd1;
		CwmsRatingRef d2 = (CwmsRatingRef) dd2;
		String s1 = RatingColumnizer.getColumn(d1, col).trim();
		String s2 = RatingColumnizer.getColumn(d2, col).trim();
		int r = s1.compareTo(s2);
		if (r != 0 || col != 0)
			return r;
		else
			return RatingColumnizer.getColumn(d1, 0).trim().compareTo(
				RatingColumnizer.getColumn(d2, 0).trim());
	}
}
