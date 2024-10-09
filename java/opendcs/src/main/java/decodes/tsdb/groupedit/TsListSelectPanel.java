/**
 * $Id: TsListSelectPanel.java,v 1.9 2020/01/31 19:41:00 mmaloney Exp $
 * 
 * Open Source Software
 * 
 * $Log: TsListSelectPanel.java,v $
 * Revision 1.9  2020/01/31 19:41:00  mmaloney
 * Implement new TS Definition Panel for OpenTSDB and CWMS.
 *
 * Revision 1.8  2018/05/23 19:59:02  mmaloney
 * OpenTSDB Initial Release
 *
 * Revision 1.7  2017/01/24 15:37:32  mmaloney
 * CWMS-10060 Remove redundant filling of TSID and Location caches.
 *
 * Revision 1.6  2016/11/29 01:15:47  mmaloney
 * Refactor listTimeSeries to make refresh explicit.
 *
 * Revision 1.5  2016/11/21 16:04:03  mmaloney
 * Code Cleanup.
 *
 * Revision 1.4  2016/01/27 22:07:59  mmaloney
 * Debugs for weird error involving inconsistent compare for sort.
 *
 * Revision 1.3  2015/11/18 14:13:17  mmaloney
 * Bug fix: Make the TsIdColumnCaparator be consistent.
 *
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import opendcs.dai.TimeSeriesDAI;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.gui.TopFrame;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * Displays a sorting-list of TimeSeries objects in the database.
 */
@SuppressWarnings("serial")
public class TsListSelectPanel extends JPanel
{
	//Panel
	public String module = "TsListSelectPanel";
	//Panel Owner

	private final TsIdSelectTableModel model;
	private final List<JComboBox<String>> tsidFilterComboBoxes = new ArrayList<>();
	private final ItemListener comboBoxFilterListener = new ComboBoxFilterListener();
	SortingListTable tsIdListTable;

	/** Constructor. */
	public TsListSelectPanel(TimeSeriesDb theTsDb, boolean showDescription,
		boolean preloadAll)
	{
		model = new TsIdSelectTableModel(theTsDb, preloadAll);
		tsIdListTable = new SortingListTable(model, model.columnWidths);
		setMultipleSelection(false);
		jbInit(preloadAll);
		model.updateFilters();
	}
	
	public void setMultipleSelection(boolean ok)
	{
		tsIdListTable.getSelectionModel().setSelectionMode(
				ok ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
						: ListSelectionModel.SINGLE_SELECTION);
	}

	/** Initializes GUI components. */
	private void jbInit(boolean showFilterPanel)
	{
		//Panel Components
		JScrollPane jScrollPane = new JScrollPane();
		setLayout(new BorderLayout(0, 5));
		add(jScrollPane, BorderLayout.CENTER);
		jScrollPane.getViewport().add(tsIdListTable, null);
		if(showFilterPanel)
		{
			JPanel north = buildFilterComboBoxPanel(model.theTsDb);
			add(north, BorderLayout.NORTH);
		}
	}

	private JPanel buildFilterComboBoxPanel(TimeSeriesDb theTsDb)
	{
		String[] tsIdParts = theTsDb.getTsIdParts();
		JPanel retval = new JPanel(new GridLayout(0, 1));
		JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER));
		for(int i = 0; i < tsIdParts.length; i++)
		{
			//Only add three ComboBoxes per row
			if(i % 3 == 0)
			{
				row = new JPanel(new FlowLayout(FlowLayout.CENTER));
				retval.add(row);
			}
			JLabel label = new JLabel(tsIdParts[i]);
			JComboBox<String> comboBox = new JComboBox<>();
			label.setLabelFor(comboBox);
			row.add(label);
			row.add(comboBox);
			comboBox.addItemListener(comboBoxFilterListener);
			this.tsidFilterComboBoxes.add(comboBox);
		}
		return retval;
	}

	/**
	 * This method will do two things:
	 * 			Add a new TSID to the list if obj does not exists
	 * -or- Modify a TSID obj if already in the list
	 * 
	 * @param newdd
	 *            the new object
	 */
	public void modifyTSIDList(TimeSeriesIdentifier olddd, 
			TimeSeriesIdentifier newdd)
	{
		model.modifyTSIDList(olddd, newdd);
	}

	/**
	 * @return the currently-selected TSID, or null if none selected
	 */
	public TimeSeriesIdentifier getSelectedTSID()
	{
		int r = tsIdListTable.getSelectedRow();
		if (r == -1)
			return null;
		//Get the correct row from the table model
		int modelrow = tsIdListTable.convertRowIndexToModel(r);
		TsIdSelectTableModel tablemodel = (TsIdSelectTableModel)tsIdListTable.getModel();
		return tablemodel.getTSIDAt(modelrow);
	}

	/**
	 * @return all currently-selected TSIDs, or empty array if none.
	 */
	public TimeSeriesIdentifier[] getSelectedTSIDs()
	{
		int idx[] = tsIdListTable.getSelectedRows();
		TimeSeriesIdentifier ret[] = new TimeSeriesIdentifier[idx.length];
		for (int i = 0; i < idx.length; i++)
			//Get the correct row from the table model
			ret[i] = model.getTSIDAt(tsIdListTable.convertRowIndexToModel(idx[i]));
		return ret;
	}

	public void refreshTSIDList()
	{
		model.refresh();
	}

	/** Resorts the list by the current column selection. */
	public void reSort()
	{
		model.reSort();
	}

	/**
	 * Deletes the specified TSID from the list.
	 * 
	 * @param ob the object to delete.
	 */
	public void deleteTSID(TimeSeriesIdentifier ob)
	{
		model.deleteTSID(ob);
	}

	/**
	 * Delete TSID of the given array list
	 * 
	 * @param dds list of TSIDs to delete
	 */
	public void deleteTSIDs(TimeSeriesIdentifier[] dds)
	{
		for (TimeSeriesIdentifier dd : dds)
		{
			deleteTSID(dd);
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
		model.setTSIDListFromTsGroup(ddsIn);
	}
	public void addTsDd(TimeSeriesIdentifier tsDdToAdd)
	{
		model.addTsDd(tsDdToAdd);
	}
	
	public int[] getSelectedRows()
	{
		return tsIdListTable.getSelectedRows();
	}
	
	public TimeSeriesIdentifier getTSIDAt(int index)
	{
		return model.getTSIDAt(index);
	}
	
	public ArrayList<TimeSeriesIdentifier> getAllTSIDsInList()
	{
		return model.getTsIdsInList();
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
	
	/**
	 * Build and return an array of distinct TSID components.
	 * @param part the column name, corresponding to the TSID component
	 * @return the list of distinct parts.
	 */
	public Collection<String> getDistinctPart(String part)
	{
		TreeSet<String> ret = new TreeSet<String>();
		
		int column=-1;
		for(int c = 0; c < model.columnNames.length; c++)
			if (part.equalsIgnoreCase(model.columnNames[c]))
			{
				column = c;
				break;
			}
		if (column >= 0)
			for(TimeSeriesIdentifier tsid : model.tsidList)
			{
				String s = TsIdSelectColumnizer.getColumn(tsid, column);
				if (s != null && s.length() > 0)
					ret.add(s);
			}
		
		return ret;
	}

	private class ComboBoxFilterListener implements ItemListener
	{
		@Override
		public void itemStateChanged(ItemEvent e)
		{
			Object source = e.getSource();
			int index = tsidFilterComboBoxes.indexOf(source);
			if(index >= 0)
			{
				model.updateFilters();
			}
		}
	}

	/**
	 * The TsIdSelectTableModel class is used as the table model
	 * for SortingListTable derived from JTable. This class allows to fetch
	 * the TSID info into the table and provides methods to access
	 * the table data set from the table object.
	 */
	@SuppressWarnings("serial")
	private class TsIdSelectTableModel extends AbstractTableModel implements
																  SortingListTableModel
	{
		private String module;
		private TimeSeriesDb theTsDb;
		String[] columnNames;
		int[] columnWidths;

		private int sortColumn = -1;

		ArrayList<TimeSeriesIdentifier> tsidList = new ArrayList<>();

		/**
		 * This constructor is used when calling TsListSelectPanel
		 * from TsGroupDefinitionPanel.
		 *
		 * @param theTsDb
		 * @param columnNames
		 */
		private TsIdSelectTableModel(TimeSeriesDb theTsDb, boolean preloadAll)
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

			module = "TSIDSelectTableModel";
			this.theTsDb = theTsDb;

			if (preloadAll)
				loadTsIds();
			sortByColumn(0);
		}

		private void loadTsIds()
		{
			try(TimeSeriesDAI timeSeriesDAO = theTsDb.makeTimeSeriesDAO())
			{
				//Fetch the time series identifiers for the tsDb
				tsidList = timeSeriesDAO.listTimeSeries(false);
				if (tsidList == null)
				{
					Logger.instance().warning(module + " The Time Series ID List is null.");
					TopFrame.instance().showError("The Time Series ID List is empty.");
					tsidList = new ArrayList<>();
				}
			}
			catch (DbIoException ex)
			{
				String msg = module + " Can not get the Time Series ID List "
						+ ex.getMessage();
				Logger.instance().failure(msg);
				TopFrame.instance().showError(msg);
			}
			updateFilters();
			reSort();
			fireTableDataChanged();
		}

		private void updateFilters()
		{
			if(tsidFilterComboBoxes.isEmpty())
			{
				return;
			}
			final List<TimeSeriesIdentifier> filteredIdentifiers = filterTsids();
			filterTable(filteredIdentifiers);
			List<NavigableSet<String>> comboBoxValues = new ArrayList<>();
			for(String part : theTsDb.getTsIdParts())
			{
				NavigableSet<String> items = new TreeSet<>();
				for(TimeSeriesIdentifier tsid : filteredIdentifiers)
				{
					for(int i = 0; i < tsid.getParts().length; i++)
					{
						items.add(tsid.getPart(part));
					}
				}
				comboBoxValues.add(items);
			}
			for(int i = 0; i < tsidFilterComboBoxes.size() && i < comboBoxValues.size(); i++)
			{
				JComboBox<String> comboBox = tsidFilterComboBoxes.get(i);
				Dimension preferredSize = comboBox.getPreferredSize();
				List<String> strings = new ArrayList<>(comboBoxValues.get(i));
				strings.add(0, null);
				String[] values = strings.toArray(new String[0]);
				DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>(values);
				comboBoxModel.setSelectedItem(comboBox.getSelectedItem());
				comboBox.setModel(comboBoxModel);
				if(filteredIdentifiers.size() != tsidList.size())
				{
					//Keeps the ComboBoxes from growing/shrinking when updating the items
					comboBox.setPreferredSize(preferredSize);
				}
			}
		}

		private void filterTable(final List<TimeSeriesIdentifier> filteredIdentifiers)
		{
			//use the keys for row filter optimization
			final Set<Long> keys = new HashSet<>();
			for(TimeSeriesIdentifier tsid : filteredIdentifiers)
			{
				keys.add(tsid.getKey().getValue());
			}
			TableRowSorter<TsIdSelectTableModel> sorter = new TableRowSorter<TsIdSelectTableModel>(model);
			sorter.setRowFilter(new RowFilter<TableModel, Integer>()
			{
				@Override
				public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
				{
					TimeSeriesIdentifier tsidAt = tsidList.get(entry.getIdentifier());
					return keys.contains(tsidAt.getKey().getValue());
				}
			});
			if(tsIdListTable != null)
			{
				tsIdListTable.setRowSorter(sorter);
			}
		}

		private List<TimeSeriesIdentifier> filterTsids()
		{
			List<String> filteredPaths = new ArrayList<>();
			for(JComboBox<String> comboBox : tsidFilterComboBoxes)
			{
				filteredPaths.add((String) comboBox.getSelectedItem());
			}
			List<TimeSeriesIdentifier> retval = new ArrayList<>();
			for(TimeSeriesIdentifier timeSeriesIdentifier : tsidList)
			{
				String[] parts = timeSeriesIdentifier.getParts();
				boolean add = true;
				for(int i = 0; i < filteredPaths.size() && i < parts.length; i++)
				{
					String filter = filteredPaths.get(i);
					add = filter == null || filter.equals(timeSeriesIdentifier.getPart(parts[i]));
					if(!add)
					{
						break;
					}
				}
				if(add)
				{
					retval.add(timeSeriesIdentifier);
				}
			}
			return retval;
		}

		/**
		 * Refresh the list with the time series IDs
		 * from the database.
		 */
		private void refresh()
		{
			loadTsIds();
		}

		/**
		 * Get the time series IDs from the table list
		 *
		 * @return ArrayList<TimeSeriesIdentifier>
		 */
		public ArrayList<TimeSeriesIdentifier> getTsIdsInList()
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
		public void setTSIDListFromTsGroup(
				Collection<TimeSeriesIdentifier> ddsIn)
		{
			tsidList.clear();
			tsidList.addAll(ddsIn);
			sortByColumn(0);
			fireTableDataChanged();
		}


		/**
		 * Find out if we have a TSID with the given name and
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

		public void modifyTSIDList(TimeSeriesIdentifier olddd,
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
		 * adding new time series ID group member.
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

		public void deleteTSIDAt(int index)
		{
			TimeSeriesIdentifier dd = tsidList.get(index);
			deleteTSID(dd);
		}

		public void deleteTSID(TimeSeriesIdentifier ddObj)
		{
			int ddIndex = tsidList.indexOf(ddObj);
			if (ddIndex != -1)
			{
				tsidList.remove(ddIndex);
			}
			sortByColumn(0);
			fireTableDataChanged();
		}

		public TimeSeriesIdentifier getTSIDAt(int r)
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
					getTSIDAt(r), c);
		}

		public Object getRowObject(int r)
		{
			return tsidList.get(r);
		}

		public void sortByColumn(int c)
		{
			sortColumn = c;
			TsIdColumnComparator cmp = new TsIdColumnComparator(c);
			try { Collections.sort(tsidList, cmp); }
			catch(IllegalArgumentException ex)
			{
				System.err.println("Inconsistent comparator. Last comparison was: ");
				System.err.println(cmp.lastd1.getKey().toString() + ": " + cmp.lastd1.getUniqueString());
				System.err.println(cmp.lastd2.getKey().toString() + ": " + cmp.lastd2.getUniqueString());
				System.err.println(ex.toString());
				ex.printStackTrace(System.err);
			}
		}

		public void reSort()
		{
			if (sortColumn >= 0)
				sortByColumn(sortColumn);
		}
	}

	/**
	 * Helper class to retrieve TimeSeries ID fields by column number.
	 * Used for displaying values in the table and for sorting.
	 */
	private static class TsIdSelectColumnizer
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

	private static class TsIdColumnComparator implements Comparator<TimeSeriesIdentifier>
	{
		int col, numCols;
		TimeSeriesIdentifier lastd1, lastd2;

		TsIdColumnComparator(int col)
		{
			this.col = col;
		}

		@Override
		public int compare(TimeSeriesIdentifier tsid1, TimeSeriesIdentifier tsid2)
		{
			lastd1 = tsid1;
			lastd2 = tsid2;

			if (tsid1 == tsid2)
				return 0;

			if (col == 0)// sort integers ascendingly
				return compIds(tsid1, tsid2);
			String s1 = TsIdSelectColumnizer.getColumn(tsid1, col).trim();
			String s2 = TsIdSelectColumnizer.getColumn(tsid2, col).trim();
			int ret = s1.compareToIgnoreCase(s2);
			if (ret != 0)
				return ret;

			// The selected column is the same in both tsids.
			// Try string sort of entire tsid
			s1 = tsid1.getUniqueString();
			s2 = tsid2.getUniqueString();
			ret = s1.compareToIgnoreCase(s2);
			if (ret != 0)
				return ret;

			// Last resort, sort by surrogate keys, which must be different.
			return compIds(tsid1, tsid2);
		}

		int compIds(TimeSeriesIdentifier d1, TimeSeriesIdentifier d2)
		{
			return d1.getKey().compareTo(d2.getKey());
		}
	}
}
