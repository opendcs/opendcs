/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.2  2015/10/26 12:47:03  mmaloney
 * Added setSelection method
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.11  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 * Revision 1.10  2012/08/01 16:55:58  mmaloney
 * dev
 *
 * Revision 1.9  2012/08/01 16:40:03  mmaloney
 * dev
 *
 * Revision 1.8  2012/06/12 17:47:02  mmaloney
 * clarify object names
 *
 * Revision 1.7  2011/02/03 20:00:23  mmaloney
 * Time Series Group Editor Mods
 *
 */
package decodes.tsdb.groupedit;

import ilex.util.Logger;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.TimeSeriesDAI;

import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.gui.TopFrame;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * Displays a sorting-list of TimeSeries objects in the database.
 * Used by the TsListPanel class, the TsGroupDefinitionPanel class
 * and the TsDataDescriptorSelectDialog class.
 */
@SuppressWarnings("serial")
public class TsListSelectPanel extends JPanel
{
	//Panel
	public String module = "TsListSelectPanel";
	//Panel Owner
	
	//Panel Components
	private JScrollPane jScrollPane;
	private TsIdSelectTableModel model;
	private SortingListTable tsIdListTable;

	/** Constructor. */
	public TsListSelectPanel(TimeSeriesDb theTsDb, boolean showDescription,
		boolean preloadAll)
	{
		model = new TsIdSelectTableModel(theTsDb, preloadAll);
		tsIdListTable = new SortingListTable(model, model.columnWidths);
		setMultipleSelection(false);

		try
		{
			jbInit();
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void setMultipleSelection(boolean ok)
	{
		tsIdListTable.getSelectionModel().setSelectionMode(
				ok ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
						: ListSelectionModel.SINGLE_SELECTION);
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception
	{
		jScrollPane = new JScrollPane();
		
		setLayout(new BorderLayout());
		add(jScrollPane, BorderLayout.CENTER);
		jScrollPane.getViewport().add(tsIdListTable, null);
	}

	/**
	 * This method will do two things:
	 * 			Add a new data descriptor to the list if obj does not exists
	 * -or- Modify a data descriptor obj if already in the list
	 * 
	 * @param newdd
	 *            the new object
	 */
	public void modifyDataDescriptorList(TimeSeriesIdentifier olddd, 
			TimeSeriesIdentifier newdd)
	{
		model.modifyDataDescriptorList(olddd, newdd);
	}

	/**
	 * @return the currently-selected data descriptor, or null if none selected
	 */
	public TimeSeriesIdentifier getSelectedDataDescriptor()
	{
		int r = tsIdListTable.getSelectedRow();
		if (r == -1)
			return null;
		return model.getDataDescriptorAt(r);
	}

	/**
	 * @return all currently-selected data descriptors, or empty array if none.
	 */
	public TimeSeriesIdentifier[] getSelectedDataDescriptors()
	{
		int idx[] = tsIdListTable.getSelectedRows();
		TimeSeriesIdentifier ret[] = new TimeSeriesIdentifier[idx.length];
		for (int i = 0; i < idx.length; i++)
			ret[i] = model.getDataDescriptorAt(idx[i]);
		return ret;
	}

	public void refreshDataDescriptorList()
	{
		model.refresh();
	}

	/** Resorts the list by the current column selection. */
	public void reSort()
	{
		model.reSort();
	}

	/**
	 * Deletes the specified Data Descriptor from the list.
	 * 
	 * @param ob
	 *            the object to delete.
	 */
	public void deleteDataDescriptor(TimeSeriesIdentifier ob)
	{
		model.deleteDataDescriptor(ob);
	}

	/**
	 * Delete Data Descriptors of the given array list
	 * 
	 * @param dds
	 *            list of Data descriptors to delete
	 */
	//public void deleteDataDescriptors(ArrayList<DataDescriptor> dds)
	public void deleteDataDescriptors(TimeSeriesIdentifier[] dds)
	{
		for (TimeSeriesIdentifier dd : dds)
		{
			deleteDataDescriptor(dd);
		}
	}
	
	/**
	 * Verify is the given site name and param name combination
	 * exists in the current list or not
	 * 
	 * @param siteName
	 * @param paramName
	 * @return true if the site name and paramName combination
	 * exitst in the list, false otherwise
	 */
	public boolean ddExistsInList(String siteName, String paramName)
	{
		return model.ddExistsInList(siteName, paramName);
	}
	
	/**
	 * Make sure we do not have this combination in the DB already.
	 * 
	 * @param siteId
	 * @param dataTypeId
	 * @param intervalCode
	 * @param statisticsCode
	 * @return true if found a record with the save values, false othewise.
	 */
	public boolean verifyConstraints(int siteId, int dataTypeId, 
							String intervalCode, String statisticsCode)
	{
		return model.verifyConstraints(siteId, dataTypeId, 
										intervalCode, statisticsCode);
	}
	
	public void setTimeSeriesList(Collection<TimeSeriesIdentifier> ddsIn)
	{
		model.setDataDescriptorListFromTsGroup(ddsIn);
	}
	public void addTsDd(TimeSeriesIdentifier tsDdToAdd)
	{
		model.addTsDd(tsDdToAdd);
	}
	
	public int[] getSelectedRows()
	{
		return tsIdListTable.getSelectedRows();
	}
	
	public TimeSeriesIdentifier getDataDescriptorAt(int index)
	{
		return model.getDataDescriptorAt(index);
	}
	
	public ArrayList<TimeSeriesIdentifier> getAllDataDescriptorsInList()
	{
		return model.getTsDataDescriptorsInList();
	}

	public int getSelectedRowCount()
	{
		return tsIdListTable.getSelectedRowCount();
	}
	
	public void clearSelection()
	{
		tsIdListTable.clearSelection();
	}
	
	public void setSelection(TimeSeriesIdentifier tsid)
	{
		for(int idx = 0; idx < model.getRowCount(); idx++)
			if (tsid.equals(model.getRowObject(idx)))
			{
				tsIdListTable.setRowSelectionInterval(idx, idx);
				return;
			}
	}
}

/**
 * The DataDescriptorSelectTableModel class is used as the table model 
 * for SortingListTable derived from JTable. This class allows to fetch
 * the data descriptor info into the table and provides methods to access
 * the table data set from the table object.
 */
@SuppressWarnings("serial")
class TsIdSelectTableModel extends AbstractTableModel implements
		SortingListTableModel
{
	private String module;
	private TimeSeriesDb theTsDb;
	private String[] columnNames;
	int[] columnWidths;
	
	private int sortColumn = -1;
	
	private ArrayList<TimeSeriesIdentifier> tsidList;

	/**
	 * This constructor is used when calling TsListSelectPanel
	 * from TsGroupDefinitionPanel.
	 * 
	 * @param theTsDb
	 * @param columnNames
	 */
	public TsIdSelectTableModel(TimeSeriesDb theTsDb, boolean preloadAll)
	{
		super();
		
		String[] parts = theTsDb.getTsIdParts();
		TsIdSelectColumnizer.parts = parts;
		columnNames = new String[parts.length + 2];
		columnNames[0] = "Key";
		columnWidths = new int[parts.length+2];
		columnWidths[0] = 75/(parts.length+1);
		for(int i=0; i<parts.length; i++)
		{
			columnNames[i+1] = parts[i];
			columnWidths[i+1] = 75/(parts.length+1);
		}
		columnNames[parts.length+1] = "Description";
		columnWidths[parts.length+1] = 25;
		
		module = "DataDescriptorSelectTableModel";
		this.theTsDb = theTsDb;
		
		if (preloadAll)
			tsidList = loadTsIds();
		else
			tsidList = new ArrayList<TimeSeriesIdentifier>();
		sortByColumn(0);
	}
	
	private ArrayList<TimeSeriesIdentifier> loadTsIds()
	{
		TimeSeriesDAI timeSeriesDAO = theTsDb.makeTimeSeriesDAO();
		try
		{
			//Fetch the time series identifiers for the tsDb
			tsidList = timeSeriesDAO.listTimeSeries();
			
			if (tsidList == null)
			{
				Logger.instance().warning(
					module + " The Time Series ID List is null.");
				TopFrame.instance().showError(
					"The Time Series ID List is empty.");
			}
		}
		catch (DbIoException ex)
		{
			String msg = module + " Can not get the Time Series ID List "
				+ ex.getMessage();
			Logger.instance().failure(msg);
			TopFrame.instance().showError(msg);
		}
		finally
		{
			timeSeriesDAO.close();
		}
		return tsidList;
	}
	
	/**
	 * Refresh the list with the time series data descriptors 
	 * from the database.
	 */
	public void refresh()
	{
		tsidList = loadTsIds();
		reSort();
		fireTableDataChanged();
	}

	/**
	 * Get the time series data descriptors from the table list
	 * 
	 * @return ArrayList<TimeSeriesIdentifier>
	 */
	public ArrayList<TimeSeriesIdentifier> getTsDataDescriptorsInList()
	{
		ArrayList<TimeSeriesIdentifier> dds = new ArrayList<TimeSeriesIdentifier>();
		for(TimeSeriesIdentifier vecItem: tsidList)
			dds.add(vecItem);
		return dds;
	}
	
	/**
	 * This method is used from the TsGroupDefinition so that
	 * we set the Time Series group members from the TsGroup obj.
	 * @param groupList
	 */
	public void setDataDescriptorListFromTsGroup(
		Collection<TimeSeriesIdentifier> ddsIn)
	{
		tsidList.clear();
		tsidList.addAll(ddsIn);
		sortByColumn(0);
		fireTableDataChanged();
	}
	
	
	/**
	 * Find out if we have a Data Descriptor with the given name and
	 * param name on the list.
	 */	
	public boolean ddExistsInList(String siteNameIn, String paramNameIn)
	{
		if (siteNameIn != null && paramNameIn != null)
		{
			siteNameIn = siteNameIn.trim();
			paramNameIn = paramNameIn.trim();
			for(Object vecItem : tsidList)
			{
				TimeSeriesIdentifier dd = (TimeSeriesIdentifier) vecItem;
				
				String siteName = "";
				if (dd.getSite() != null)
					siteName = dd.getSite().getDisplayName();
				String paramName = dd.getPart("param");
				if (siteName != null)
					siteName = siteName.trim();
				if (paramName != null)
					paramName = paramName.trim();
				if (siteNameIn.equalsIgnoreCase(siteName) && 
									paramNameIn.equalsIgnoreCase(paramName))
					return true;
			}
		}
		return false;
	}
	
	/**
	 * Make sure we do not have this combination in the DB already.
	 * 
	 * @param siteId
	 * @param dataTypeId
	 * @param intervalCode
	 * @param statisticsCode
	 * @return true if found a record with the save values, false othewise.
	 */
	public boolean verifyConstraints(int siteId, int dataTypeId, 
							String intervalCode, String statisticsCode)
	{
		for(TimeSeriesIdentifier dd : tsidList)
		{
			if (dd.getSite().getId().equals(siteId) && 
				dd.getDataTypeId().equals(dataTypeId) &&
				dd.getPart("interval").equalsIgnoreCase(intervalCode) &&
				dd.getPart("statcode").equalsIgnoreCase(statisticsCode)
				)
			{
				return true;
			}
		}
		return false;
	}
	
	public void modifyDataDescriptorList(TimeSeriesIdentifier olddd, 
			TimeSeriesIdentifier newdd)
	{
		int ddIndex = tsidList.indexOf(olddd); 
		if (ddIndex != -1)
		{
			tsidList.set(ddIndex, newdd);
		}
		else
		{
			tsidList.add(newdd);
		}
		//reSort();
		fireTableDataChanged();
	}

	/**
	 * This method is used from the TsGroupDefinitionPanel when
	 * adding new time series data descriptor group member.
	 * 
	 * @param tsDdToAdd
	 */
	public void addTsDd(TimeSeriesIdentifier tsDdToAdd)
	{
		for(Iterator<TimeSeriesIdentifier> it = tsidList.iterator(); it.hasNext(); )
		{
			TimeSeriesIdentifier dd = it.next();
			if (tsDdToAdd.getKey() == dd.getKey())
			{
				it.remove();
				break;
			}
		}
		tsidList.add(tsDdToAdd);
		fireTableDataChanged();
	}
	
	public void deleteDataDescriptorAt(int index)
	{
		TimeSeriesIdentifier dd = tsidList.get(index);
		deleteDataDescriptor(dd);
	}

	public void deleteDataDescriptor(TimeSeriesIdentifier ddObj)
	{
		int ddIndex = tsidList.indexOf(ddObj); 
		if (ddIndex != -1)
		{
			tsidList.remove(ddIndex);
		}
		sortByColumn(0);
		fireTableDataChanged();
	}

	public TimeSeriesIdentifier getDataDescriptorAt(int r)
	{
		return (TimeSeriesIdentifier) getRowObject(r);
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
		return tsidList.size();
	}

	public Object getValueAt(int r, int c)
	{
		return TsIdSelectColumnizer.getColumn(
			getDataDescriptorAt(r), c);
	}

	public Object getRowObject(int r)
	{
		return tsidList.get(r);
	}

	public void sortByColumn(int c)
	{
		sortColumn = c;
		Collections.sort(tsidList, new TsIdColumnComparator(c));
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
class TsIdSelectColumnizer
{
	static String []parts = null;
	
	static String getColumn(TimeSeriesIdentifier dd, int c)
	{
		String r = "";
		if (c == 0)
		{
			if (dd != null)
				r = "" + dd.getKey();
		}
		else if (c <= parts.length)
			return r = dd.getPart(parts[c-1]);
		else
			return r = dd.getBriefDescription();
	
		return r == null ? "" : r;
	}
}

class TsIdColumnComparator implements Comparator<TimeSeriesIdentifier>
{
	int col, numCols;

	TsIdColumnComparator(int col)
	{
		this.col = col;
	}

	public int compare(TimeSeriesIdentifier d1, TimeSeriesIdentifier d2)
	{
		if (d1 == d2)
			return 0;
		
		if (col == 0)// sort integers ascendingly
			return compIds(d1, d2);
		String s1 = TsIdSelectColumnizer.getColumn(d1, col).trim();
		String s2 = TsIdSelectColumnizer.getColumn(d2, col).trim();
		int ret = s1.compareToIgnoreCase(s2);
		if (ret != 0)
			return ret;
		return compIds(d1, d2);
	}
	
	int compIds(TimeSeriesIdentifier d1, TimeSeriesIdentifier d2)
	{
		String s1 = TsIdSelectColumnizer.getColumn(d1, 0).trim();
		String s2 = TsIdSelectColumnizer.getColumn(d2, 0).trim();

		try
		{
			int i1 = Integer.parseInt(s1);
			int i2 = Integer.parseInt(s2);
			return i1 - i2;
		} catch (Exception ex)
		{
			Logger.instance().warning(
					" TsListSelectPanel:DataDescriptorColumnComparator" +
					" Can not sort column by data id.");
			return s1.compareToIgnoreCase(s2);
		}

	}
}
