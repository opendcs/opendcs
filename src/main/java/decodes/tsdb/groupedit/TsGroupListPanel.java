/**
 * $Id$
 * 
 * $Log$
 * Revision 1.2  2017/01/06 16:42:10  mmaloney
 * Misc Bug Fixes
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.7  2013/08/18 19:49:37  mmaloney
 * Added clear button for compedit selection.
 *
 * Revision 1.6  2012/07/24 15:15:47  mmaloney
 * Cosmetic group-editor bugs for HDB.
 *
 * Revision 1.5  2011/02/04 21:30:33  mmaloney
 * Intersect groups
 *
 * Revision 1.4  2011/02/03 20:00:23  mmaloney
 * Time Series Group Editor Mods
 *
 * Revision 1.3  2011/02/01 15:32:39  gchen
 * *** empty log message ***
 *
 * Revision 1.2  2011/01/27 23:21:18  gchen
 * Make the TS group GUI available against cwms DB
 *
 * Revision 1.1  2010/12/08 13:50:09  mmaloney
 * Moved from decodes.groupedit
 *
 * Revision 1.1  2010/12/08 13:40:06  mmaloney
 * Renamed.
 *
 * Revision 1.1  2010/12/07 15:18:45  mmaloney
 * Created
 *
 */
package decodes.tsdb.groupedit;

import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import opendcs.dai.TsGroupDAI;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import decodes.gui.GuiDialog;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.gui.TopFrame;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsGroup;
import decodes.util.DecodesSettings;

/**
 * Displays a sorting-list of TimeSeries Group objects in the database
 * or in a TsGroup obj. Use by the TsGroupDefinitionPanel class
 * and the TsGroupSelectDialog class.
 */
public class TsGroupListPanel extends JPanel
{
	//Panel
	public String module = "TsGoupListPanel";
	//Panel Owner
	private TopFrame frameOwner = null;
	private GuiDialog dialogOwner = null;
	//Panel Components
	private JScrollPane jScrollPane = new JScrollPane();
	private SortingListTable tsGroupsListTable;
	private TsGroupsSelectTableModel model;
	//Time Series DB
	TimeSeriesDb theTsDb;
	//Titles, Labels defined here for internationalization
	private GroupSelector ctrlPanel = null;

	/** Constructor. */
	public TsGroupListPanel(TimeSeriesDb theTsDb, GuiDialog dialogOwner, GroupSelector ctrlPanel)
	{
		this(theTsDb, dialogOwner, null, ctrlPanel);
	}

	public TsGroupListPanel(TimeSeriesDb theTsDb, TopFrame frameOwner, GroupSelector ctrlPanel)
	{
		this(theTsDb, null, frameOwner, ctrlPanel);
	}

	private TsGroupListPanel(TimeSeriesDb theTsDb, GuiDialog dialogOwner,
		TopFrame frameOwner, GroupSelector ctrlPanel)
	{
		this.theTsDb = theTsDb;
		this.dialogOwner = dialogOwner;
		this.frameOwner = frameOwner;
		this.ctrlPanel = ctrlPanel;

		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/** Initializes GUI components. */
	private void jbInit() throws Exception
	{
		//Initialize components for jScrollPane and tsGroupsListTable
		jScrollPane = new JScrollPane();
		model = new TsGroupsSelectTableModel(this);
		tsGroupsListTable = new SortingListTable(model, new int[] { 17, 22, 24, 48, 20, 20 });
		tsGroupsListTable.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
		setMultipleSelection(false);
		if (ctrlPanel != null)
			tsGroupsListTable.addMouseListener(
				new MouseAdapter()
				{
					public void mouseClicked(MouseEvent e)
					{
						if (e.getClickCount() == 2)
						{
							ctrlPanel.groupSelected();
						}
					}
				});

		//Setup the layout
		setLayout(new BorderLayout());
		add(jScrollPane, BorderLayout.CENTER);
		jScrollPane.setViewportView(tsGroupsListTable);
	}

	public void setMultipleSelection(boolean ok)
	{
		tsGroupsListTable.getSelectionModel().setSelectionMode(
				ok ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
						: ListSelectionModel.SINGLE_SELECTION);
	}

	/**
	 * This method will do two things: Add a new ts group to the list if obj
	 * does not exists -or- Modify a ts group obj if already in the list
	 * 
	 * @param oldG
	 *            the old object
	 * @param newG
	 *            the new object
	 */
	public void modifyTsGroupList(TsGroup oldG, TsGroup newG)
	{
		model.modifyTsGroupList(oldG, newG);
	}

	/**
	 * @return the currently-selected Ts Group, or null if none selected
	 */
	public TsGroup getSelectedTsGroup()
	{
		int r = tsGroupsListTable.getSelectedRow();
		if (r == -1)
		{
			return null;
		}
		//Get the correct selected row from the model
		int rowModel = this.tsGroupsListTable.convertRowIndexToModel(r);
		TsGroupsSelectTableModel model = (TsGroupsSelectTableModel) tsGroupsListTable.getModel();
		return model.getTsGroupAt(rowModel);
	}

	public int[] getSelectedRows()
	{
		return tsGroupsListTable.getSelectedRows();
	}
	
	public TsGroup getTsGroupAt(int index)
	{
		return model.getTsGroupAt(index);
	}
	
	/**
	 * @return all currently-selected TS Groups, or empty array if none.
	 */
	public TsGroup[] getSelectedTsGroups()
	{
		int idx[] = tsGroupsListTable.getSelectedRows();
		TsGroup ret[] = new TsGroup[idx.length];
		for (int i = 0; i < idx.length; i++)
		{
			int modelRow = tsGroupsListTable.convertRowIndexToModel(idx[i]);
			ret[i] = model.getTsGroupAt(modelRow);
		}
		return ret;
	}

	public void refreshTsGroupList()
	{
		model.refresh();
	}

	/** Resorts the list by the current column selection. */
	public void reSort()
	{
		model.reSort();
	}

	public TimeSeriesDb getTsDb()
	{
		return theTsDb;
	}

	public void setTsDb(TimeSeriesDb theTsDb)
	{
		this.theTsDb = theTsDb;
	}

	/**
	 * Deletes the specified Ts Group from the list.
	 * 
	 * @param ob
	 *            the object to delete.
	 */
	public void deleteTsGroup(TsGroup ob)
	{
		model.deleteTsGroup(ob);
	}

	/**
	 * Delete Ts Group of the given array list
	 * 
	 * @param tsGroups
	 *            list of Ts Groups to delete
	 */
	//public void deleteTsGroups(ArrayList<TsGroup> tsGroups)
	public void deleteTsGroups(TsGroup[] tsGroups)
	{
		for (TsGroup g : tsGroups)
		{
			deleteTsGroup(g);
		}
	}

	/**
	 * Verify is the given group name exists in the current list or not
	 * 
	 * @param groupName
	 * @return true if the group name exitst in the list, false otherwise
	 */
	public boolean tsGroupExistsInList(String groupName)
	{
		return model.tsGroupExistsInList(groupName);
	}
	
	public void addSubgroups(ArrayList<TsGroup> groupList)
	{
		model.addSubgroups(groupList);
	}
	
	public void setTsGroupListFromDb()
	{
		model.setTsGroupListFromDb();	
	}
	
	/**
	 * This method is used from the TsGroupDefinitionPanel when
	 * adding new sub groups.
	 * 
	 * @param tsGroupToAdd
	 */
	public void addTsGroup(TsGroup tsGroupToAdd)
	{
		model.addTsGroup(tsGroupToAdd);
	}
	
	public ArrayList<TsGroup> getAllTsGroupsInList()
	{
		return model.getAllTsGroupsInList();
	}
	
	public int getSelectedRowCount()
	{
		return tsGroupsListTable.getSelectedRowCount();
	}
	
	public void clearSelection()
	{
		tsGroupsListTable.clearSelection();
	}
	
	void showError(String msg)
	{
		if (frameOwner != null)
			frameOwner.showError(msg);
		else if (dialogOwner != null)
			dialogOwner.showError(msg);
	}
	
	public TsGroupsSelectTableModel getModel()
	{
		return model;
	}
}


class TsGroupsSelectTableModel extends AbstractTableModel implements
		SortingListTableModel
{
	private static String[] columnNames;
	private TsGroupListPanel panel;
	private ArrayList<TsGroup> theGroupList = new ArrayList<TsGroup>();
	private Map<TsGroup, Integer> compCount = new HashMap<TsGroup, Integer>();
	private int sortColumn = -1;
	private String module;
//	private ArrayList<TsGroup> tsGroupList;

	public TsGroupsSelectTableModel(TsGroupListPanel panel)
	{
		super();
		this.panel = panel;
		module = panel.module + ":" + "TSGroupsSelectTableModel";

		ResourceBundle groupResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/groupedit",
			DecodesSettings.instance().language);

		columnNames = new String[6];
		
		columnNames[0] = groupResources.getString("TsGroupsListSelectPanel.groupIdColumnLabel");
		columnNames[1] = groupResources.getString("TsGroupsListSelectPanel.nameColumnLabel");
		columnNames[2] = groupResources.getString("TsGroupsListSelectPanel.typeColumnLabel");
		columnNames[3] = groupResources.getString("TsGroupsListSelectPanel.descriptionColumnLabel");
		columnNames[4] = groupResources.getString("TsGroupsListSelectPanel.tsCountColumnLabel");
		columnNames[5] = groupResources.getString("TsGroupsListSelectPanel.compsUsedColumnLabel");
	}

	public void setTsGroupListFromDb()
	{
		theGroupList = getTsGroupListFromDb();
		sortByColumn(0);
	}

//TODO Is the following method necessary?
	/**
	 * This method is used from the TsGroupDefinition so that
	 * we add the included sub group members from the TsGroup obj.
	 * @param groupList
	 */
	public void addSubgroups(ArrayList<TsGroup> groupList)
	{
		for (TsGroup g: groupList)
		{
			if (g != null) 
				addTsGroup(g);
		}
	}
	
	/**
	 * This method is used from the TsGroupDefinitionPanel when
	 * adding new sub groups.
	 * 
	 * @param tsGroupToAdd
	 */
	public void addTsGroup(TsGroup tsGroupToAdd)
	{
		for(Iterator<TsGroup> it = theGroupList.iterator(); it.hasNext(); )
		{
			TsGroup group = it.next();
			if (tsGroupToAdd.getGroupId() == group.getGroupId())
			{
				it.remove();
				break;
			}
		}
		theGroupList.add(tsGroupToAdd);
		reSort();
		fireTableDataChanged();
	}
	
	public ArrayList<TsGroup> getTsGroupListFromDb()
	{
		ArrayList<TsGroup> tsGroups = new ArrayList<TsGroup>();
		if (panel.theTsDb != null)
		{
			try(TsGroupDAI groupDAO = panel.theTsDb.makeTsGroupDAO())
			{
				tsGroups = groupDAO.getTsGroupList(null);
				if (tsGroups == null)
				{
					Logger.instance().warning(
							module + " The Ts Group List is null.");
					panel.showError("The Ts Group List is empty.");
				}
				for(TsGroup tsGroup : tsGroups)
				{
					int count = groupDAO.countCompsUsingGroup(tsGroup.getGroupId());
					this.compCount.put(tsGroup, count);
				}
			}
			catch (DbIoException ex)
			{
				String msg = module + " Can not get the Ts Group List "
						+ ex.getMessage();
				Logger.instance().failure(msg);
				panel.showError(msg);
			}
		}
		else
		{
			Logger.instance().failure(module + " The TsDb obj is null.");
		}
		return tsGroups;
	}

	/**
	 * Find out if we have a Ts Group with the given name on the list.
	 */
	public boolean tsGroupExistsInList(String groupNameIn)
	{
		if (groupNameIn != null)
		{
			String groupName = groupNameIn.trim();
			for (TsGroup group : theGroupList)
			{
				String groupNameonList = group.getGroupName();
				if (groupNameonList != null)
					groupNameonList = groupNameonList.trim();
				if (groupName.equalsIgnoreCase(groupNameonList))
					return true;
			}
		}
		return false;
	}

	void modifyTsGroupList(TsGroup oldG, TsGroup newG)
	{
		int gIndex = theGroupList.indexOf(oldG);
		if (gIndex != -1)
		{
			theGroupList.set(gIndex, newG);
		} else
		{
			theGroupList.add(newG);
		}
		//reSort();
		fireTableDataChanged();
	}

	//TODO the model shouldn't be doing database IO. The client should do this.
	public void refresh()
	{
		theGroupList.clear();
		theGroupList.addAll(getTsGroupListFromDb());
		reSort();
		fireTableDataChanged();
	}

	//TODO Where is this used from, and why?
	public ArrayList<TsGroup> getAllTsGroupsInList()
	{
		ArrayList<TsGroup> ret;
		ret = new ArrayList<TsGroup>();
		ret.addAll(theGroupList);
		return ret;
	}
	
	void deleteTsGroupAt(int index)
	{
		if (index >= 0 && index < theGroupList.size())
		{
			theGroupList.remove(index);
			fireTableDataChanged();
		}
	}

	//TODO Why does the client need the following. It should always delete by index.
	void deleteTsGroup(TsGroup tsGObj)
	{
		int ddIndex = theGroupList.indexOf(tsGObj);
		if (ddIndex != -1)
			theGroupList.remove(ddIndex);
		fireTableDataChanged();
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
		return theGroupList.size();
	}

	public TsGroup getTsGroupAt(int r)
	{
		return (TsGroup) getRowObject(r);
	}

	@Override
	public Object getValueAt(int r, int c)
	{
		TsGroup tsGroupAt = getTsGroupAt(r);
		if(c == 5)
		{
			return compCount.get(tsGroupAt);
		}
	  return TsGroupsSelectColumnizer.getColumn(tsGroupAt, c);
	}

	public Object getRowObject(int r)
	{
		return theGroupList.get(r);
	}

	public void sortByColumn(int c)
	{
		sortColumn = c;
		Collections.sort(theGroupList, new TsGroupsColumnComparator(c));
	}

	public void reSort()
	{
		if (sortColumn >= 0)
			sortByColumn(sortColumn);
	}

	public void replaceTsGroup(TsGroup modifiedGroup)
	{
		for(Iterator<TsGroup> it = theGroupList.iterator(); it.hasNext(); )
		{
			TsGroup group = it.next();
			if (modifiedGroup.getGroupId().equals(group.getGroupId()))
			{
				it.remove();
				continue;
			}
			
			TsGroup subgroup = null;
			for(TsGroup sg : group.getIncludedSubGroups())
				if (sg.getGroupId().equals(modifiedGroup.getGroupId()))
				{
					subgroup = sg;
					break;
				}
			if (subgroup != null)
			{
				group.getIncludedSubGroups().remove(subgroup);
				group.addSubGroup(modifiedGroup, 'A');
			}
			
			subgroup = null;
			for(TsGroup sg : group.getExcludedSubGroups())
				if (sg.getGroupId().equals(modifiedGroup.getGroupId()))
				{
					subgroup = sg;
					break;
				}
			if (subgroup != null)
			{
				group.getExcludedSubGroups().remove(subgroup);
				group.addSubGroup(modifiedGroup, 'S');
			}

			subgroup = null;
			for(TsGroup sg : group.getIntersectedGroups())
				if (sg.getGroupId().equals(modifiedGroup.getGroupId()))
				{
					subgroup = sg;
					break;
				}
			if (subgroup != null)
			{
				group.getIntersectedGroups().remove(subgroup);
				group.addSubGroup(modifiedGroup, 'I');
			}
		}
		theGroupList.add(modifiedGroup);
		reSort();
		fireTableDataChanged();
	}

}

/**
 * Helper class to retrieve Ts Group fields by column number. Used for
 * displaying values in the table and for sorting.
 */
class TsGroupsSelectColumnizer
{

	static String getColumn(TsGroup tsGroup, int c)
	{
		switch (c)
		{
		case 0: // Group ID
		{
			if (tsGroup != null)
				return "" + tsGroup.getGroupId();
			else
				return "";
		}
		case 1: // Name
		{
			if (tsGroup != null)
			{
				return tsGroup.getGroupName() == null ? "" : tsGroup
						.getGroupName();
			} else
				return "";
		}
		case 2: // Type
			if (tsGroup != null)
			{
				return tsGroup.getGroupType() == null ? "" : tsGroup
						.getGroupType();
			} else
				return "";
		case 3: // Description
			if (tsGroup != null)
			{
				return tsGroup.getDescription() == null ? ""
						: getFirstLine(tsGroup.getDescription());
			} else
				return "";
		case 4: // TS Count
			return tsGroup.getTsMemberList().size() + "";
		default:
			return "";
		}
	}

	private static String getFirstLine(String tmp)
	{
		if (tmp == null)
			return "";
		int len = tmp.length();
		int ci = len;
		if (ci > 60)
			ci = 60;
		int i = tmp.indexOf('\r');
		if (i > 0 && i < ci)
			ci = i;
		i = tmp.indexOf('\n');
		if (i > 0 && i < ci)
			ci = i;
		i = tmp.indexOf('.');
		if (i > 0 && i < ci)
			ci = i;

		if (ci < len)
			return tmp.substring(0, ci);
		else
			return tmp;
	}
}

class TsGroupsColumnComparator implements Comparator
{
	int col;

	TsGroupsColumnComparator(int col)
	{
		this.col = col;
	}

	public int compare(Object tsG1, Object tsG2)
	{
		if (tsG1 == tsG2)
			return 0;
		TsGroup g1 = (TsGroup) tsG1;
		TsGroup g2 = (TsGroup) tsG2;
		if (col == 0)// sort integers assendingly
		{
			try
			{
				int i1 = Integer.parseInt(TsGroupsSelectColumnizer.getColumn(
						g1, col).trim());
				int i2 = Integer.parseInt(TsGroupsSelectColumnizer.getColumn(
						g2, col).trim());
				return i1 - i2;
			} catch (Exception ex)
			{
			}
		}
		return TsGroupsSelectColumnizer.getColumn(g1, col).compareToIgnoreCase(
				TsGroupsSelectColumnizer.getColumn(g2, col));
	}
}
