package decodes.tsdb.groupedit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import opendcs.dai.TsGroupDAI;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.gui.TopFrame;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsGroup;
import decodes.util.DecodesSettings;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;

/**
 * Displays a sorting-list of TS Group objects in the database.
 */
public class TsDbGrpListPanel 
	extends JPanel
	implements TsListControllers, GroupSelector
{
	//Panel
	private String module = "TsDbGrpListPanel";
	//Panel Owner
	private TopFrame parent;
	//Panel Components
	private JLabel listLabel;
	private TsListControlsPanel controlsPanel;
	private TsGroupListPanel tsGroupsListSelectPanel;
	//Time Series DB

	//Miscellaneous
	private String tabNameUnknown;
	private String openErrorMsg;
	private String openErrorMsgEx;
	private String deleteErrorMsg;
	private String deleteErrorMsg2;
	private String deleteConfirmMsg;
	private String deleteConfirmMsg2;
	private String groupNameRequired;
	private String groupNameErrorMsg;
	private String groupNameExistsErrorMsg;
	private String listTitle;

	// Define the modal dialog here so it doesn't have to be refreshed every
	// time the user wants to select a time-series. With 1000s of time-series
	// in a database, the refresh may take time.
	TimeSeriesSelectDialog timeSeriesSelectDialog = null;

	/** Constructor. */
	public TsDbGrpListPanel()
	{
		parent = null;
		ResourceBundle groupResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/groupedit",
			DecodesSettings.instance().language);
		if (groupResources != null)
		{
			//For internationalization, get the title description and
			//all other descriptions from properties file
			listTitle = groupResources
				.getString("TsGroupsListPanel.tsGroupsListTitle");
			openErrorMsg = groupResources
				.getString("TsGroupsListPanel.openErrorMsg");
			openErrorMsgEx = groupResources
				.getString("TsGroupsListPanel.openErrorMsgEx");
			tabNameUnknown = groupResources
				.getString("TsGroupsListPanel.tabNameUnknown");
			deleteErrorMsg = groupResources
				.getString("TsGroupsListPanel.deleteErrorMsg");
			deleteErrorMsg2 = groupResources
				.getString("TsGroupsListPanel.deleteErrorMsg2");
			deleteConfirmMsg = groupResources
				.getString("TsGroupsListPanel.deleteConfirmMsg");
			deleteConfirmMsg2 = groupResources
				.getString("TsGroupsListPanel.deleteConfirmMsg2");
			groupNameRequired = groupResources
				.getString("TsGroupDefinitionPanel.groupNameRequired");
			groupNameExistsErrorMsg = groupResources
				.getString("TsGroupDefinitionPanel.groupNameExistsErrorMsg");
			groupNameErrorMsg = groupResources
				.getString("TsGroupDefinitionPanel.groupNameErrorMsg");
		}

		try
		{
			jbInit();
			timeSeriesSelectDialog = new TimeSeriesSelectDialog(
				TsdbAppTemplate.theDb, true, parent);
			timeSeriesSelectDialog.setMultipleSelection(true);

		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * Sets the parent frame object. Each list panel needs to know this.
	 * 
	 * @param parent
	 *            the TsDbGrpEditorFrame, the TsDbEditorFrame 
	 */
	public void setParent(TopFrame parent)
	{
		this.parent = parent;
	}

	/** Initialize GUI components. */
	private void jbInit() throws Exception
	{
		//Initiate components for listLabel, controlsPanel, and tsGroupsListSelectPanel
		listLabel = new JLabel(listTitle, SwingConstants.CENTER);
		controlsPanel = new TsListControlsPanel(this);
		tsGroupsListSelectPanel = new TsGroupListPanel(
			TsdbAppTemplate.theDb, TopFrame.instance(), this);
		tsGroupsListSelectPanel.setTsGroupListFromDb();
		
		//Setup the layout
		setLayout(new BorderLayout());
		add(listLabel, BorderLayout.NORTH);
		add(controlsPanel, BorderLayout.SOUTH);
		add(tsGroupsListSelectPanel, BorderLayout.CENTER);
	}

	/** @return type of entity that this panel edits. */
	public String getEntityType()
	{
		return "TsGroupsList";
	}

	public void groupSelected()
	{
		openPressed();
	}
	
	/** Called when the 'Open' button is pressed. */
	public void openPressed()
	{
		TsGroup tsGroup = tsGroupsListSelectPanel.getSelectedTsGroup();
		if (tsGroup == null)
		{
			TopFrame.instance().showError(openErrorMsg);
		} else
		{
			try
			{
				doOpen(tsGroup);
			} catch (Exception ex)
			{
				String groupName = tsGroup.getGroupName();
				TopFrame.instance().showError(
						openErrorMsgEx + " '" + groupName + "' : "
								+ ex.toString());
			}
		}
	}

	/** Called when the 'New' button is pressed. */
	public void newPressed()
	{
		doNew();
	}

	public TsGroupDefinitionPanel doNew()
	{
		TsGroup groupObj = new TsGroup();
		//Ask user to enter a group name
		String newGroupName = JOptionPane.showInputDialog(this,
				groupNameRequired);
		if (newGroupName == null)
			return null; // cancel pressed.
		newGroupName = newGroupName.trim();
		if (newGroupName.length() == 0)
		{ // Check in case user enters a blank group name.
			TopFrame.instance().showError(groupNameErrorMsg);
			return null;
		}
		//Verify that the group name does not exist in the group list
		if (tsGroupsListSelectPanel.tsGroupExistsInList(newGroupName))
		{
			TopFrame.instance().showError(groupNameExistsErrorMsg);
			return null;
		}
		//set groupObj with the name enter by user
		groupObj.setGroupName(newGroupName);
		return doOpen(groupObj);
	}

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed()
	{
		TsGroup tsGroup = tsGroupsListSelectPanel.getSelectedTsGroup();
//		TempestTsdb tsDb = TsDbEditorFrame.getTsDb();
		TimeSeriesDb tsDb = TsdbAppTemplate.theDb;

		if (tsGroup == null)
		{
			TopFrame.instance().showError(deleteErrorMsg);
			return;
		}

		JTabbedPane tsGroupListTabbedPane = parent.getTsGroupsListTabbedPane();
		String groupTabName = tsGroup.getGroupName();
		TsDbGrpEditorTab tab = ((TsDbGrpEditorTabbedPane) tsGroupListTabbedPane).findEditorFor(groupTabName);
		if (tab != null)
		{
			TopFrame.instance().showError(deleteErrorMsg2);
			return;
		}

		int numComps = 0;
		try { numComps = tsDb.countCompsUsingGroup(tsGroup.getGroupId()); }
		catch(Exception ex)
		{
			System.err.println(ex);
			ex.printStackTrace(System.err);
			return;
		}
		String msg = deleteConfirmMsg;
		if (numComps > 0)
		{
			msg = "There are " + numComps + " computations using the "
				+ "selected group. If you continue, these computations will be "
				+ "disabled. Confirm Deletion of Group?";
		}
		
		int ok = JOptionPane.showConfirmDialog(this, msg,
			deleteConfirmMsg2, JOptionPane.YES_NO_OPTION);

		if (ok == JOptionPane.YES_OPTION)
		{
			// Remove from Database
			TsGroupDAI tsGroupDAO = tsDb.makeTsGroupDAO();
			try
			{
				tsGroupDAO.deleteTsGroup(tsGroup.getGroupId());
			} 
			catch (DbIoException ex)
			{
				Logger.instance().failure(
						module + " Can not delete Ts Group from "
								+ "the Database " + ex.getMessage());
				TopFrame.instance().showError(ex.toString());
			}
			finally
			{
				tsGroupDAO.close();
			}
			// Remove from Ts Group List
			tsGroupsListSelectPanel.deleteTsGroup(tsGroup);
		}
	}

	/** Called when the 'Refresh' button is pressed. */
	public void refresh()
	{
		tsGroupsListSelectPanel.refreshTsGroupList();
		timeSeriesSelectDialog.refresh();
	}

	/**
	 * Opens a TsGroupDefinitionPanel for the passed Ts Group.
	 * 
	 * @param ob - the object to be edited.
	 * @return the TsGroupDefinitionPanel opened.
	 */
	public TsGroupDefinitionPanel doOpen(TsGroup tsGroup)
	{
		String groupTabName = "";
		if (tsGroup.getGroupName() != null)
			groupTabName = tsGroup.getGroupName();
		else
			// user pressed new button
			groupTabName = tabNameUnknown;

		JTabbedPane theGrpTabbedPanel = parent.getTsGroupsListTabbedPane();
		TsGroupDefinitionPanel groupTab = null;

	  //The group tab opened already
		if (theGrpTabbedPanel.indexOfTab(groupTabName) != -1)
		{
			groupTab = (TsGroupDefinitionPanel)theGrpTabbedPanel.getComponentAt(
					theGrpTabbedPanel.indexOfTab(groupTabName));
			theGrpTabbedPanel.setSelectedComponent(groupTab);
		}
		//The group tab not opened yet
		else
		{
			groupTab = new TsGroupDefinitionPanel();
			groupTab.setTabName(groupTabName);
			groupTab.setTsGroup(tsGroup);
			groupTab.setParent(parent);
			timeSeriesSelectDialog.clearSelection();
			groupTab.setTimeSeriesSelectDialog(timeSeriesSelectDialog);
			theGrpTabbedPanel.add(groupTabName, groupTab);
			theGrpTabbedPanel.setSelectedComponent(groupTab);
			groupTab.setGroupListModel(tsGroupsListSelectPanel.getModel());
			groupTab.setParentListPanel(this);
		}
		return groupTab;
	}

	/**
	 * After saving, and edit panel will need to replace the old object 
	 * with the newly modified one. It calls this method to do this.
	 * 
	 * @param gold
	 *            the old object
	 * @param gnew
	 *            the new object
	 */
	public void modifyTsGroupList(TsGroup gold, TsGroup gnew)
	{
		tsGroupsListSelectPanel.modifyTsGroupList(gold, gnew);
	}

	/**
	 * Verify is the given ts group name exists in the current list or not
	 * 
	 * @param groupName
	 * @return true if the group name exitst in the list, false otherwise
	 */
	public boolean tsGroupExistsInList(String groupName)
	{
		return tsGroupsListSelectPanel.tsGroupExistsInList(groupName);
	}

	/**
	 * Called when a group is modified and saved to the database by the definition
	 * panel. Need to go through any other open definition panels that might have
	 * this group as a subgroup.
	 * @param savedGroup the group just modified and saved.
	 */
	public void updateSubGroups(TsGroup savedGroup)
	{
		// Go through all open group tabs.
		JTabbedPane theGrpTabbedPanel = parent.getTsGroupsListTabbedPane();
		for(int idx = 0; idx < theGrpTabbedPanel.getComponentCount(); idx++)
		{
			Component tp = theGrpTabbedPanel.getComponentAt(idx);
			if (tp instanceof TsGroupDefinitionPanel)
			{
				// If this is NOT the panel for the just-saved group
				TsGroupDefinitionPanel defPanel = (TsGroupDefinitionPanel)tp;
				if (!defPanel.getEditedGroup().getGroupId().equals(savedGroup.getGroupId()))
				{
					defPanel.replaceSubGroup(savedGroup);
				}
			}
		}


	}
}
