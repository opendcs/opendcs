/**
 * $Id$
 * 
 * $Log$
 * Revision 1.4  2015/10/22 14:04:55  mmaloney
 * Clean up debug: Old code was saying no match for site even when it did find match.
 *
 * Revision 1.3  2015/08/04 18:54:03  mmaloney
 * CWMS-6388 Display should show location ID, not public name. This fixes the issue
 * with evaluation.
 *
 * Revision 1.2  2014/09/25 18:10:40  mmaloney
 * Enum fields encapsulated.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.12  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 * Revision 1.11  2012/09/19 15:33:53  mmaloney
 * "StatCode" needs to be "StatisticsCode"
 *
 * Revision 1.10  2012/09/19 15:07:41  mmaloney
 * Fix bugs in detecting whether changes have occured.
 *
 * Revision 1.9  2012/08/01 16:55:58  mmaloney
 * dev
 *
 * Revision 1.8  2012/07/30 20:04:21  mmaloney
 * Don't ask 'are you sure' when removing time-series from list.
 *
 * Revision 1.7  2012/07/24 15:48:41  mmaloney
 * Cosmetic group-editor bugs for HDB.
 *
 * Revision 1.6  2012/07/24 15:15:47  mmaloney
 * Cosmetic group-editor bugs for HDB.
 *
 * Revision 1.5  2012/07/24 13:40:14  mmaloney
 * groupedit cosmetic bugs
 *
 * Revision 1.4  2012/07/18 20:41:44  mmaloney
 * Updated for USBR HDB.
 *
 * Revision 1.3  2011/10/19 14:27:04  gchen
 * Modify the addQueryParam() to use InputDialog (other than MessageDialog) for version to allow the cancel button displayed on the screen.
 *
 * Revision 1.2  2011/02/04 21:30:33  mmaloney
 * Intersect groups
 *
 * Revision 1.1  2011/02/03 20:00:23  mmaloney
 * Time Series Group Editor Mods
 *
 */
package decodes.tsdb.groupedit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.IntervalDAI;
import ilex.util.AsciiUtil;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.util.TextUtil;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DataType;
import decodes.db.DatabaseException;
import decodes.db.EnumValue;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.dbeditor.ChangeTracker;
import decodes.dbeditor.SiteSelectDialog;
import decodes.gui.EnumComboBox;
import decodes.gui.SortingListTable;
import decodes.gui.TopFrame;
import decodes.hdb.HdbTsId;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsGroupMember;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.groupedit.TsGroupListPanel;
import decodes.util.DecodesSettings;
import decodes.gui.SortingListTableModel;

/**
 * This class is the Ts Group tab of the Time Series GUI.
 * 
 */
@SuppressWarnings("serial")
public class TsGroupDefinitionPanel
	extends TsDbGrpEditorTab
	implements TsEntityOpsController, ChangeTracker, GroupSelector
{
	//Panel
	private String module = "TsGroupDefinitionPanel";
	//Panel Owner
	private TopFrame parent;
	//Panel Components
	/* North Panel has groupid upto description */
	private JPanel northPanel;
	private JLabel groupIdLabel;
	private JTextField groupIdTextField;
	private JLabel groupNameLabel;
	private JTextField groupNameTextField;
	private JLabel groupTypeLabel;
	private EnumComboBox groupTypeComboBox;
	private JLabel descriptionLabel;
	private JScrollPane descJScrollPane;
	private JTextArea descriptionTextArea;
	private JButton renameButton;
	private JButton newTypeButton;
	/* Entity Operations Panel has the save and close buttons */
	private TsEntityOpsPanel tsEntityOpsPanel;
	/* Center Panel has TS group members, Sub-group members, and other members */
	private JPanel centerPanel;
	/*	TS group members */
	private JPanel tsGroupMembersPanel;
	private JButton addTimeSeriesMemberButton;
	private JButton deleteTimeSeriesMemberButton;
	/*	Subgroup members */
	private JPanel subGroupMembersPanel;
	private JButton addIncludedSubgroupMemberButton;
	private JButton addExcludedSubgroupMemberButton;
	private JButton addIntersectedSubgroupMemberButton;
	private JButton deleteSubgroupMemberButton;
	/*	Other group members */
	private JPanel queryPanel;
//	private DefaultListModel queryListModel;
//	private JList queryList;
	
	//Time Series DB
	private TimeSeriesDb theTsDb;
	//Miscellaneous
	private boolean newType;
	private decodes.db.DbEnum en;
	private EnumValue ev;
	private TsGroup tsGroup;
	private TsGroup oldTsGroup;
	private TsGroupListPanel tsGroupsListSelectPanel;
	private TsListSelectPanel tsListSelectPanel;
	
	private ArrayList<Site> knownSites = new ArrayList<Site>();
	private ArrayList<DataType> dataTypeList = new ArrayList<DataType>();
	private String[] dataTypeArray = null;
	private String[] intervalArray = null;
	private String[] durationArray = null;
	private String[] paramTypes = null;
	private String[] versionArray = null;

	private static SiteSelectDialog siteSelectDlg = null;
	
	private String groupIdLabelStr;
	private String groupNameLabelStr;
	private String groupTypeLabelStr;
	private String descriptionLabelStr;
	private String renameLabelStr;
	private String newTypeLabelStr;
	private String tsGroupMembersTitleLabelStr;
	private String subGroupMembersTitleLabelStr;
	private String addButtonLabelStr;
	private String deleteButtonLabelStr;
	private String groupNameRequired;
	private String groupNameErrorMsg;
	private String groupNameExistsErrorMsg;
	private String queryPanelLabelStr;
	private String enterIntervalLabel;
	private String enterParamTypeLabel;
	private String enterDurationLabel;
	private String enterVersionLabel;
	private String saveChangesLabel;
	private String tsDeleteMsg;
	private String groupDeleteMsg;
	private String groupDeleteConfMsg1;
	private String groupDeleteConfMsg2;
	private String groupNameEmptyMsg;
	private String groupTEmptyMsg;
	private String addIncludedSubgroupButtonLabel;
	private String addExcludedSubgroupButtonLabel;
	private TimeSeriesSelectDialog timeSeriesSelectDialog = null;
	private QuerySelectorTableModel queryModel = new QuerySelectorTableModel();
	private SortingListTable queryTable = null;
	
	public TsGroupDefinitionPanel()
	{	
		//For internationalization, get the descriptions of 
		//title and all labels from properties file
		setAllLabels();

		try
		{	
			//Set the time series database
			theTsDb = TsdbAppTemplate.theDb;
			
			//en is used when user adds a new group type
			en = Database.getDb().getDbEnum("GroupType");
			newType = false;
			
//			//Set the tsIdPartIdentifier from the time series database
//			setTsIdPartIdentifier();
			
			//Initialize the components for this panel, northPanel, and centerPanel
			jbInit();
			dataTypeList.clear();
			String prefDtStd = DecodesSettings.instance().dataTypeStdPreference;
			for(Iterator<DataType> dtit = Database.getDb().dataTypeSet.iterator();
				dtit.hasNext(); )
			{
				DataType dt = dtit.next();
				if (dt.getStandard().equalsIgnoreCase(prefDtStd))
					dataTypeList.add(dt);
			}
			dataTypeArray = new String[dataTypeList.size()];
			for(int i=0; i<dataTypeList.size(); i++)
				dataTypeArray[i] = dataTypeList.get(i).getCode();
			Arrays.sort(dataTypeArray);
			
			IntervalDAI intervalDAO = theTsDb.makeIntervalDAO();
			try
			{
				intervalArray = intervalDAO.getValidIntervalCodes();
				durationArray = intervalDAO.getValidDurationCodes();
			}
			finally
			{
				intervalDAO.close();
			}
			paramTypes = theTsDb.getParamTypes();
			versionArray = theTsDb.getValidPartChoices("version");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * @param groupResources
	 */
	private void setAllLabels()
	{
		
		ResourceBundle groupResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/groupedit",
			DecodesSettings.instance().language);
		ResourceBundle genericResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic",
			DecodesSettings.instance().language);


		groupIdLabelStr = groupResources
				.getString("TsGroupDefinitionPanel.groupIdLabel")
				+ ":";
		groupNameLabelStr = groupResources
				.getString("TsGroupDefinitionPanel.groupNameLabel")
				+ ":";
		groupTypeLabelStr = groupResources
				.getString("TsGroupDefinitionPanel.groupTypeLabel")
				+ ":";
		descriptionLabelStr = groupResources
				.getString("TsGroupDefinitionPanel.descriptionLabel")
				+ ":";
		renameLabelStr = groupResources
				.getString("TsGroupDefinitionPanel.renameLabel");
		newTypeLabelStr = groupResources
				.getString("TsGroupDefinitionPanel.newTypeLabel");
		tsGroupMembersTitleLabelStr = groupResources
				.getString("TsGroupDefinitionPanel.tsGroupMembersTitleLabel");
		subGroupMembersTitleLabelStr = groupResources
				.getString("TsGroupDefinitionPanel.subGroupMembersTitleLabel");
		addButtonLabelStr = groupResources
				.getString("TsGroupDefinitionPanel.addButtonLabel");
		deleteButtonLabelStr = groupResources
				.getString("TsGroupDefinitionPanel.deleteButtonLabel");
		groupNameRequired = groupResources
				.getString("TsGroupDefinitionPanel.groupNameRequired");
		groupNameErrorMsg = groupResources
				.getString("TsGroupDefinitionPanel.groupNameErrorMsg");
		groupNameExistsErrorMsg = groupResources
				.getString("TsGroupDefinitionPanel.groupNameExistsErrorMsg");
		queryPanelLabelStr = groupResources
				.getString("TsGroupDefinitionPanel.queryPanelLabelStr");
		enterIntervalLabel = groupResources
			.getString("TsGroupDefinitionPanel.enterIntervalLabel");
		enterParamTypeLabel = groupResources
			.getString("TsGroupDefinitionPanel.enterParamTypeLabel");
		enterDurationLabel = groupResources
			.getString("TsGroupDefinitionPanel.enterDurationLabel");
		enterVersionLabel = groupResources
			.getString("TsGroupDefinitionPanel.enterVersionLabel");
		saveChangesLabel = genericResources.getString("saveChanges");
		tsDeleteMsg = groupResources
					.getString("TsGroupDefinitionPanel.tsDeleteMsg");
		groupDeleteMsg = groupResources
						.getString("TsGroupDefinitionPanel.groupDeleteMsg");
		groupDeleteConfMsg1 = groupResources
					.getString("TsGroupDefinitionPanel.groupDeleteConfMsg1");
		groupDeleteConfMsg2 = groupResources
					.getString("TsGroupDefinitionPanel.groupDeleteConfMsg2");
		groupNameEmptyMsg = groupResources
					.getString("TsGroupDefinitionPanel.groupNameEmptyMsg");
		groupTEmptyMsg = groupResources
					.getString("TsGroupDefinitionPanel.groupTEmptyMsg");
		addIncludedSubgroupButtonLabel = groupResources
					.getString("TsGroupDefinitionPanel.addIncludedSubgroupButtonLabel");
		addExcludedSubgroupButtonLabel = groupResources
					.getString("TsGroupDefinitionPanel.addExcludedSubgroupButtonLabel");
	}
	
	/** Initialize GUI components */
	private void jbInit() throws Exception
	{
		// Initialize this panel
		setLayout(new BorderLayout());

		// Initialize the components for northPanel, centerPanel, and tsEntityOpsPanel
		initControlPanel();
		initNorthPanel();
		initCenterPanel();
		
		// Add the northPanel, centerPanel, and tsEntityOpsPanel into this panel
		this.add(northPanel, BorderLayout.NORTH);
		this.add(tsEntityOpsPanel, BorderLayout.SOUTH);
		this.add(centerPanel, BorderLayout.CENTER);
	}

	private void initControlPanel()
	{
		//The control panel with Save and Close buttons
		tsEntityOpsPanel = new TsEntityOpsPanel(this);
	}

	private void initNorthPanel()
	{
		groupIdLabel = new JLabel(groupIdLabelStr);
		groupIdTextField = new JTextField();
		groupIdTextField.setEditable(false);
		groupNameLabel = new JLabel(groupNameLabelStr);
		groupNameTextField = new JTextField();
		groupNameTextField.setEditable(false);
		groupTypeLabel = new JLabel(groupTypeLabelStr);
		groupTypeComboBox = new EnumComboBox("GroupType");
		descriptionLabel = new JLabel(descriptionLabelStr);
		descriptionTextArea = new JTextArea();
		descriptionTextArea.setLineWrap(true);
		descJScrollPane = new JScrollPane();
		descJScrollPane.getViewport().add(descriptionTextArea, null);
		descJScrollPane.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		renameButton = new JButton(renameLabelStr);
		newTypeButton = new JButton(newTypeLabelStr);
		renameButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				renameButton_actionPerformed(e);
			}
		});
		newTypeButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				newTypeButton_actionPerformed(e);
			}
		});

		northPanel = new JPanel(new GridBagLayout());
		
		northPanel.add(groupIdLabel, new GridBagConstraints(0, 0, 1, 1, 0.0,
				0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(10, 30, 0, 0), 2, 5));
		northPanel.add(groupIdTextField, new GridBagConstraints(1, 0, 1, 1,
				1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, 
				new Insets(10, 0, 0, 1), 62, 5));
		northPanel.add(groupNameLabel, new GridBagConstraints(0, 1, 1, 1, 0.0,
				0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 30, 0, 0), 2, 5));
		northPanel.add(groupNameTextField, new GridBagConstraints(1, 1, 1, 1,
				1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 1), 62, 5));
		northPanel.add(renameButton, new GridBagConstraints(2, 1, 1, 1, 0.0,
				0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 20, 0, 300), 0, 0));
		northPanel.add(groupTypeLabel, new GridBagConstraints(0, 2, 1, 1, 0.0,
				0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 30, 0, 0), 2, 5));
		northPanel.add(groupTypeComboBox, new GridBagConstraints(1, 2, 1, 1,
				1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 1), 62, 5));
		northPanel.add(newTypeButton, new GridBagConstraints(2, 2, 1, 1, 0.0,
				0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 20, 0, 300), 0, 0));
		northPanel.add(descriptionLabel, new GridBagConstraints(0, 3, 1, 1,
				0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 30, 0, 0), 2, 5));
		northPanel.add(descJScrollPane, new GridBagConstraints(1, 3, 2, 1, 1.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 0, 110), 62, 25));
	}
	
	private void initCenterPanel()
	{
		centerPanel = new JPanel(new GridLayout(3,1));

		// Initiate time series group member panel
		createTsGroupMembersComponents();
		centerPanel.add(tsGroupMembersPanel);

		// Initiate subgroup member panel
		createSubGroupMembersComponents();
		centerPanel.add(subGroupMembersPanel);

		// Initiate other member panel
		createQueryPanelMembers();
		centerPanel.add(queryPanel);
	}
	
	
	/**
	 * Create and initialize all components in tsGroupMembersPanel
	 */
	private void createTsGroupMembersComponents()
	{
		tsGroupMembersPanel = new JPanel(new GridBagLayout());
		tsGroupMembersPanel.setBorder(new TitledBorder(
				BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
				tsGroupMembersTitleLabelStr));
		tsListSelectPanel = new TsListSelectPanel(theTsDb, false, true);
		tsListSelectPanel.setMultipleSelection(true);
		
		addTimeSeriesMemberButton = new JButton(addButtonLabelStr);
		deleteTimeSeriesMemberButton = new JButton(deleteButtonLabelStr);

		addTimeSeriesMemberButton
				.addActionListener(new java.awt.event.ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						addTimeSeriesMemberButton_actionPerformed(e);
					}
				});
		deleteTimeSeriesMemberButton
				.addActionListener(new java.awt.event.ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						deleteTimeSeriesMemberButton_actionPerformed(e);
					}
				});

		tsGroupMembersPanel.add(tsListSelectPanel, new GridBagConstraints(0, 0,
				1, 2, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 5, 4, 5), 0, 0));
		tsGroupMembersPanel.add(addTimeSeriesMemberButton,
				new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL,
						new Insets(2, 12, 0, 12), 0, 0));
		tsGroupMembersPanel.add(deleteTimeSeriesMemberButton,
				new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
						GridBagConstraints.NORTH,
						GridBagConstraints.HORIZONTAL,
						new Insets(5, 12, 2, 12), 16, 0));
	}

	/**
	 * Create and initialize all components in subGroupMembersPanel
	 */
	private void createSubGroupMembersComponents()
	{
		subGroupMembersPanel = new JPanel(new GridBagLayout());
		subGroupMembersPanel.setBorder(new TitledBorder(
				BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
				subGroupMembersTitleLabelStr));
		tsGroupsListSelectPanel = new TsGroupListPanel(theTsDb, TopFrame.instance(), true, this);
		tsGroupsListSelectPanel.setMultipleSelection(true);
		
		addIncludedSubgroupMemberButton = new JButton(addIncludedSubgroupButtonLabel);
		addExcludedSubgroupMemberButton = new JButton(addExcludedSubgroupButtonLabel);
		addIntersectedSubgroupMemberButton = new JButton("Intersect SubGroup");
		deleteSubgroupMemberButton = new JButton(deleteButtonLabelStr);
		
		addIncludedSubgroupMemberButton
			.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				addIncludedSubgroupMemberButton_actionPerformed(e);
			}
		});
		addExcludedSubgroupMemberButton
			.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				addExcludedSubgroupMemberButton_actionPerformed(e);
			}
		});
		addIntersectedSubgroupMemberButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					addIntersectedSubgroupMemberButton_actionPerformed(e);
				}
			});
		deleteSubgroupMemberButton
			.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				deleteSubgroupMemberButton_actionPerformed(e);
			}
		});

		subGroupMembersPanel.add(tsGroupsListSelectPanel,
			new GridBagConstraints(0, 0, 1, 4, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(4, 5, 4, 5), 0, 0));
		subGroupMembersPanel.add(addIncludedSubgroupMemberButton,
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 12, 2, 12), 55, 0));
		subGroupMembersPanel.add(addExcludedSubgroupMemberButton,
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 12, 2, 12), 55, 0));
		subGroupMembersPanel.add(addIntersectedSubgroupMemberButton,
			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 12, 2, 12), 55, 0));
		subGroupMembersPanel.add(deleteSubgroupMemberButton,
			new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 12, 2, 12), 55, 0));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void createQueryPanelMembers()
	{
		//Initialize the queryPanel and its components except for buttons
		queryPanel = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		JScrollPane listPane = new JScrollPane();
		
// Left col is the type of thing (i.e. the button label that was pressed to enter this, like "Location").
// Right col is the value.
//		queryListModel = new DefaultListModel();
//		queryList = new JList(queryListModel);
//		queryList.getSelectionModel().setSelectionMode(
//			ListSelectionModel.SINGLE_SELECTION);
		
		queryTable = new SortingListTable(queryModel, QuerySelectorTableModel.colWidths);
		
//		listPane.getViewport().add(queryList, null);
		listPane.getViewport().add(queryTable, null);

		queryPanel.add(buttonPanel, BorderLayout.EAST);
		queryPanel.add(listPane, BorderLayout.CENTER);
		queryPanel.setBorder(new TitledBorder(queryPanelLabelStr));

		// Now put buttons for each of the time-series identifier parts.
		String[] tsIdParts = theTsDb.getTsIdParts();
		for(int idx = 0; idx < tsIdParts.length; idx++)
		{
			final String tsPart = tsIdParts[idx];
			JButton button = new JButton(tsPart);
			button.addActionListener(
				new java.awt.event.ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						addQueryParam(tsPart);
					}
				});
			int x = idx % 2;
			int y = idx / 2;
			buttonPanel.add(button,
				new GridBagConstraints(x, y, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(5, 10, 5, 10), 0, 0));
		}

		//Initialize the buttons and their events
		JButton deleteButton = new JButton(deleteButtonLabelStr);
		deleteButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					deleteQueryParam();
				}
			});
		
		
		buttonPanel.add(deleteButton,
			new GridBagConstraints(0, (tsIdParts.length+1)/2, 2, 1, 0.0, 1.0,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(5, 10, 5, 10), 0, 0));
	}

	/**
	 * @param parent the enclosing TsDbEditorFrame.
	 */
	public void setParent(TopFrame parent)
	{
		this.parent = parent;
	}

	/**
	 * Set all components from the TsGroup read from DB. This method receives a
	 * group obj. With this group obj we need to get the time series group
	 * members and sub group members.
	 * 
	 * @param tsGroup
	 *            Ts Group obj
	 */
	public void setTsGroup(TsGroup tsGroupIn)
	{
		// Expand the group so that we read the time series
		// and the sub group members.
		oldTsGroup = null;
		oldTsGroup = tsGroupIn;
		if (oldTsGroup.getIsExpanded() == false)
		{
			if (oldTsGroup.getGroupId() != Constants.undefinedId)
			{
				try
				{
					// read group time series and sub groups
					if (theTsDb != null)
						theTsDb.readTsGroupMembers(oldTsGroup);
					else
					{
						Logger.instance().failure(
							module + ":setTsGroup() The TsDb obj is null.");
					}
				} catch (DbIoException ex)
				{
					Logger.instance().failure(
							module + " Can not read Ts Group information "
									+ "From Database " + ex.getMessage());
					TopFrame.instance().showError(ex.toString());
					// return false;
				}
			}
		}
		// Create the tsGroup obj that we'll modify
		copyTsGroup(oldTsGroup);
		fillFields();
	}

	@SuppressWarnings("unchecked")
	private void fillFields()
	{
		groupIdTextField.setText("");
		if (tsGroup.getGroupId() != Constants.undefinedId)
			groupIdTextField.setText("" + tsGroup.getGroupId());

		groupNameTextField.setText(tsGroup.getGroupName() == null ? ""
				: tsGroup.getGroupName());

		if (tsGroup.getGroupType() != null)
			groupTypeComboBox.setSelection(tsGroup.getGroupType());

		descriptionTextArea.setText(tsGroup.getDescription() == null ? ""
				: tsGroup.getDescription());

		// Populate sub lists
		ArrayList<TimeSeriesIdentifier> ddlist = new ArrayList<TimeSeriesIdentifier>();
		for(TimeSeriesIdentifier tsid : tsGroup.getTsMemberList())
			ddlist.add(tsid);
		tsListSelectPanel.setTimeSeriesList(ddlist);
		
		tsGroupsListSelectPanel.addSubgroups(tsGroup.getIncludedSubGroups());
		tsGroupsListSelectPanel.addSubgroups(tsGroup.getExcludedSubGroups());
		tsGroupsListSelectPanel.addSubgroups(tsGroup.getIntersectedGroups());

		//Load other group members from the TsDB
		queryModel.clear();
		String[] tsIdParts = theTsDb.getTsIdParts();

		for(DbKey siteId: tsGroup.getSiteIdList())
		{
			try
			{
				Site st = theTsDb.getSiteById(siteId);
				SiteName sn = null;
				if (theTsDb.isCwms())
					sn = st.getName(Constants.snt_CWMS);
				if (sn == null)
					sn = st.getPreferredName();
				
				if (!knownSites.contains(st))
					knownSites.add(st);
				// Location or Site is always the first part of the TSID.
				queryModel.items.add(new StringPair(tsIdParts[0], sn.getNameValue()));
//				queryListModel.addElement(tsIdParts[0] + ": " + st.getDisplayName());
			}
			catch(Exception ex)
			{
				Logger.instance().warning("Cannot read site with id=" 
					+ siteId + ": " + ex);
			}
		}
		for(DbKey dtId: tsGroup.getDataTypeIdList())
		{
			try
			{
				DataType dt = DataType.getDataType(dtId);
				// DataType or Param is always the 2nd part of the TSID
				queryModel.items.add(new StringPair(tsIdParts[1], dt.getCode()));
//				queryListModel.addElement(tsIdParts[1] + ": " + dt.getCode());
			}
			catch(Exception ex)
			{
				Logger.instance().warning("Cannot read datatype with id=" 
					+ dtId + ": " + ex);
			}
		}
		for(TsGroupMember member : tsGroup.getOtherMembers())
		{
			queryModel.items.add(new StringPair(member.getMemberType(), member.getMemberValue()));
//			queryListModel.addElement(member.getMemberType() + ": "
//				+ member.getMemberValue());
		}
		queryModel.sortByColumn(0);
	}

	private boolean getDataFromFields(boolean validationFlag)
	{
		TsGroup tempGroup = null;
		tempGroup = new TsGroup();
		tempGroup.setGroupId(tsGroup.getGroupId());
		tempGroup.setIsExpanded(tsGroup.getIsExpanded());

		// Get group name
		String groupName = (groupNameTextField.getText()).trim();
		tempGroup.setGroupName(groupName);
		if (groupName == null || groupName.equals(""))
		{
			if (validationFlag == true)
			{
				TopFrame.instance().showError(groupNameEmptyMsg);
				return false;
			}
		}
		// Get group type
		// This is required too
		String groupType = groupTypeComboBox.getSelection();
		tempGroup.setGroupType(groupType);
		if (groupType == null || groupType.equals(""))
		{
			if (validationFlag == true)
			{
				TopFrame.instance().showError(groupTEmptyMsg);
				return false;
			}
		}

		// Get description
		tempGroup.setDescription((descriptionTextArea.getText()).trim());
		// Get time series members
		for(TimeSeriesIdentifier dd: tsListSelectPanel.getAllDataDescriptorsInList())
		{	
			tempGroup.addTsMember(dd);
		}

		// Get sub group members
		for(TsGroup g : tsGroupsListSelectPanel.getAllTsGroupsInList())
			tempGroup.addSubGroup(g, g.getInclusion().charAt(0));

		//Feed other group members to the TsDb
		for(StringPair sp : queryModel.items)
		{
			String label = sp.first;
			String value = sp.second;
			
			if (label.equalsIgnoreCase("site") || label.equalsIgnoreCase("location"))
			{
				boolean found = false;
				for(Site st: knownSites)
					if (st.hasNameValue(sp.second))
					{
						tempGroup.addSiteId(st.getId());
						found = true;
						break;
					}
				if (!found)
					Logger.instance().warning("No match for sitename '" + value + "' -- ignored.");
			}
			else if (label.equalsIgnoreCase("datatype") || label.equalsIgnoreCase("param"))
			{
				boolean found = false;
				for(DataType dt : dataTypeList)
				{
					if (dt.getCode().equalsIgnoreCase(value))
					{
						tempGroup.addDataTypeId(dt.getId());
						found = true;
						break;
					}
				}
				if (!found)
					Logger.instance().warning("No match for param/datatype '" + value + "' -- ignored.");
			}
			else 
				tempGroup.addOtherMember(label, value);
		}			
		copyTsGroup(tempGroup);
		return true;
	}

	/**
	 * User Presses Rename button
	 * 
	 * @param e
	 *            ActionEvent
	 */
	private void renameButton_actionPerformed(ActionEvent e)
	{
		String newGroupName = JOptionPane.showInputDialog(this,
												groupNameRequired);
		if (newGroupName == null)
			return; // cancel pressed.
		newGroupName = newGroupName.trim();
		String groupNameInTextField = (groupNameTextField.getText()).trim();
		if (newGroupName.length() == 0)
		{ // Check in case user enters a blank group name.
			if (groupNameInTextField.length() == 0)
			{// if there is no data on field
				TopFrame.instance().showError(groupNameErrorMsg);
			}
			return;
		}
		// make sure that tsGroup is not null
		if (tsGroup != null)
		{ // Verify if the name entered is the same that we have on this
			// group, first check the textfield
			if (newGroupName.equalsIgnoreCase((groupNameTextField.getText())
					.trim()))
				return;
			if (newGroupName.equalsIgnoreCase(tsGroup.getGroupName()))
			{ // now, if it matches with the one in the tsGroup, reset it back
				// Set group Name text field
				groupNameTextField.setText(newGroupName);
				// Reset the tab name with the new name
				resetTabName(newGroupName);
				return;
			}
		}

		// Verify if the new group name is already in the
		// ts group list
		if (groupExistsInList(newGroupName))
			return; // group name exists in db

		// Set group Name text field
		groupNameTextField.setText(newGroupName);
		// Reset the tab name with the new name
		resetTabName(newGroupName);
	}

	/**
	 * Verify if the group name exists in the group list.
	 * 
	 * @param groupName
	 * 
	 * @return true if it exists, false otherwise
	 */
	private boolean groupExistsInList(String groupName)
	{
		boolean tsGroupExisted = false;
		tsGroupExisted = ((TsDbGrpListPanel) parent.getTsGroupsListPanel())
			.tsGroupExistsInList(groupName);
		
		if (tsGroupExisted)
		{
			TopFrame.instance().showError(groupNameExistsErrorMsg);
			return true;
		}
		return false;
	}

	private void resetTabName(String groupName)
	{
		String groupTabName = groupName;
		// Set the tabname so that we know which tab it is.
		if (groupName == null || groupName.equals(""))
			groupTabName = "unknown";

		// Reset the tab name of TsGroupDefinitionPanel
		// (use to keep track of the tabs)
		setTabName(groupTabName);
		// Reset the tab title
		JTabbedPane tab = parent.getTsGroupsListTabbedPane();
		if (tab != null)
		{
			int idx = tab.indexOfComponent(this);
			if (idx != -1)
				tab.setTitleAt(idx, groupTabName);
		}
	}

	/**
	 * User Presses New Type button
	 * 
	 * @param e
	 *            ActionEvent
	 */
	@SuppressWarnings("unchecked")
	public void newTypeButton_actionPerformed(ActionEvent e)
	{
		GroupTypeValueDialog evd = new GroupTypeValueDialog(this.parent);
		ev = new EnumValue(en, "", "", "", "");
		evd.fillValues(ev);
		launchDialog(evd);
		if (evd.wasChanged())
		{
			newType = true;
			groupTypeComboBox.addItem(ev.getValue());
			groupTypeComboBox.setSelection(ev.getValue());
		}
	}

	/**
	 * User Presses Time Series Data Descriptor Group Members Add button
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void addTimeSeriesMemberButton_actionPerformed(ActionEvent e)
	{
		TopFrame.instance().launchDialog(timeSeriesSelectDialog);
		TimeSeriesIdentifier toAdd[] = timeSeriesSelectDialog.getSelectedDataDescriptors();
		for (int i = 0; i < toAdd.length; i++)
		{
			TimeSeriesIdentifier dd = toAdd[i];
			tsListSelectPanel.addTsDd(dd);
		}
	}

	/**
	 * User Presses Time Series Data Descriptor Group Members Delete button
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void deleteTimeSeriesMemberButton_actionPerformed(ActionEvent e)
	{
		// Find out how many Data Descriptors were selected
		int nrows = tsListSelectPanel.getSelectedRowCount();
		if (nrows == 0)
		{
			TopFrame.instance().showError(tsDeleteMsg);
			return;
		}
		int rows[] = tsListSelectPanel.getSelectedRows();
		TimeSeriesIdentifier obs[] = new TimeSeriesIdentifier[nrows];
		for (int i = 0; i < nrows; i++)
			obs[i] = tsListSelectPanel.getDataDescriptorAt(rows[i]);

		for (int i = 0; i < nrows; i++)
			tsListSelectPanel.deleteDataDescriptor(obs[i]);
	}

	/**
	 * User Presses Sub Group Members Add button
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void addIncludedSubgroupMemberButton_actionPerformed(ActionEvent e)
	{
		TsGroupSelectDialog dlg = 
			new TsGroupSelectDialog(TopFrame.instance());
		dlg.setMultipleSelection(false);
		TopFrame.instance().launchDialog(dlg);
		
		TsGroup selectedGrps[] = dlg.getSelectedTsGroups();
		if (selectedGrps.length == 0)
			return;
		
		for (TsGroup g: selectedGrps)
		{
			if (g.getGroupName().equals(tsGroup.getGroupName()))
			{
				String msgStr = "Group "+tsGroup.getGroupName()+" cannot add itself as a subgroup, ignore it!";
				JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msgStr, 60),
						subGroupMembersTitleLabelStr, JOptionPane.WARNING_MESSAGE);
				continue;
			}
			
			if (tsGroup.hasSubgroup(g))
			{
				String msgStr = "Group "+g.getGroupName()+" is already in the subgroup list, ignore it!";
				JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msgStr, 60),
						subGroupMembersTitleLabelStr, JOptionPane.WARNING_MESSAGE);
				continue;
			}
			g.setInclusion("Add");
			tsGroupsListSelectPanel.addTsGroup(g);
		}
	}

	/**
	 * User Presses Sub Group Members Add button
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void addExcludedSubgroupMemberButton_actionPerformed(ActionEvent e)
	{
		TsGroupSelectDialog dlg = 
			new TsGroupSelectDialog(TopFrame.instance());
		dlg.setMultipleSelection(false);
		TopFrame.instance().launchDialog(dlg);

		TsGroup selectedGrps[] = dlg.getSelectedTsGroups();
		if (selectedGrps.length == 0)
			return;
		
		for (TsGroup g: selectedGrps)
		{
			if (g.getGroupName().equals(tsGroup.getGroupName()))
			{
				String msgStr = "Group "+tsGroup.getGroupName()+" cannot add itself as a subgroup, ignore it!";
				JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msgStr, 60),
						subGroupMembersTitleLabelStr, JOptionPane.WARNING_MESSAGE);
				continue;
			}
			
			if (tsGroup.hasSubgroup(g))
			{
				String msgStr = "Group "+g.getGroupName()+" is already in the subgroup list, ignore it!";
				JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msgStr, 60),
						subGroupMembersTitleLabelStr, JOptionPane.WARNING_MESSAGE);
				continue;
			}

			g.setInclusion("Subtract");
			tsGroupsListSelectPanel.addTsGroup(g);
		}
	}
	
	private void addIntersectedSubgroupMemberButton_actionPerformed(ActionEvent e)
	{
		TsGroupSelectDialog dlg = 
			new TsGroupSelectDialog(TopFrame.instance());
		dlg.setMultipleSelection(false);
		TopFrame.instance().launchDialog(dlg);

		TsGroup selectedGrps[] = dlg.getSelectedTsGroups();
		if (selectedGrps.length == 0)
			return;
		
		for (TsGroup g: selectedGrps)
		{
			if (g.getGroupName().equals(tsGroup.getGroupName()))
			{
				String msgStr = "Group "+tsGroup.getGroupName()+" cannot add itself as a subgroup, ignore it!";
				JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msgStr, 60),
						subGroupMembersTitleLabelStr, JOptionPane.WARNING_MESSAGE);
				continue;
			}
			
			if (tsGroup.hasSubgroup(g))
			{
				String msgStr = "Group "+g.getGroupName()+" is already in the subgroup list, ignore it!";
				JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msgStr, 60),
						subGroupMembersTitleLabelStr, JOptionPane.WARNING_MESSAGE);
				continue;
			}

			g.setInclusion("Intersect");
			tsGroupsListSelectPanel.addTsGroup(g);
		}
		
	}

	/**
	 * User Presses Sub Group Members Delete button
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void deleteSubgroupMemberButton_actionPerformed(ActionEvent e)
	{
		// Find out how many Data Descriptors were selected
		int nrows = tsGroupsListSelectPanel.getSelectedRowCount();
		if (nrows == 0)
		{
			TopFrame.instance().showError(groupDeleteMsg);
			return;
		}
		int rows[] = tsGroupsListSelectPanel.getSelectedRows();
		TsGroup obs[] = new TsGroup[nrows];
		for (int i = 0; i < nrows; i++)
			obs[i] = tsGroupsListSelectPanel.getTsGroupAt(rows[i]);

		String msg = nrows == 1 ? groupDeleteConfMsg1 : groupDeleteConfMsg2;
		int r = JOptionPane.showConfirmDialog(this, msg);
		if (r == JOptionPane.OK_OPTION)
			for (int i = 0; i < nrows; i++)
				tsGroupsListSelectPanel.deleteTsGroup(obs[i]);
	}

	/**
	 * (from TsEntityOpsController interface)
	 */
	public String getEntityName()
	{
		return "TsGroupDefinitionPanel";
	}

	/**
	 * (from EntityOpsController interface)
	 */
	public void saveEntity()
	{
		if (getDataFromFields(true))
			saveChanges(); // no erros keep going
	}

	/**
	 * (from TsEntityOpsController interface)
	 */
	public void closeEntity()
	{
		if (hasChanged())
		{
			int r = JOptionPane.showConfirmDialog(this, saveChangesLabel);
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
			{ // check for errors
				if (getDataFromFields(true))// pass true to validate
				{ // no errors found - validate
					if (!saveChanges())
						return;
				} else
					return; // errors stop
			} else if (r == JOptionPane.NO_OPTION)
			{
			}
		}
		
		JTabbedPane tsGroupsListTabbedPane = parent.getTsGroupsListTabbedPane();
		if (tsGroupsListTabbedPane != null)
			tsGroupsListTabbedPane.remove(this);
	}

	/**
	 * Called from the File - Close All menu item to close and possibly abandon
	 * any changes.
	 */
	public void forceClose()
	{
	}

	private void copyTsGroup(TsGroup groupIn)
	{
		tsGroup = groupIn.copy(groupIn.getGroupName());
		tsGroup.setGroupId(groupIn.getGroupId());
	}

	/**
	 * (from EntityOpsController interface)
	 */
	public void correctionsEntity()
	{ // DO nothing for Ts Group Panel
	}

	public boolean hasChanged()
	{
		// pass false so that we do not validate
		getDataFromFields(false);

		// Verify if the old TsGroup equals the current Ts Group
		if (!TextUtil.strEqualIgnoreCase(oldTsGroup.getGroupName(), tsGroup
				.getGroupName()))
		{
			Logger.instance().debug3("group name has changed");
			return true;
		}
		if (!TextUtil.strEqualIgnoreCase(oldTsGroup.getGroupType(), tsGroup.getGroupType()))
		{
			Logger.instance().debug3("group type has changed, was '"
				+ oldTsGroup.getGroupType() + "' is now '" + tsGroup.getGroupType() + "'");
			return true;
		}
		if (!TextUtil.strEqualIgnoreCase(oldTsGroup.getDescription(), tsGroup
				.getDescription()))
		{
			Logger.instance().debug3("group description has changed");
			return true;
		}

		int oldSizeTs = oldTsGroup.getTsMemberList().size();
		int newSizeTs = tsGroup.getTsMemberList().size();
		int oldSizeDT = oldTsGroup.getDataTypeIdList().size();
		int newSizeDT = tsGroup.getDataTypeIdList().size();
		int oldSizeS = oldTsGroup.getSiteIdList().size();
		int newSizeS = tsGroup.getSiteIdList().size();
		int oldOtherS = oldTsGroup.getOtherMembers().size();
		int newOtherS = tsGroup.getOtherMembers().size();
		
		if (oldSizeTs == 0 && newSizeTs == 0 &&
			oldSizeDT == 0 && newSizeDT == 0 &&
			oldSizeS == 0 && newSizeS == 0 &&
			oldOtherS == 0 && newOtherS == 0)
			return false;
		if (oldSizeTs != newSizeTs)
		{
			Logger.instance().debug3("sizes have changed (old,new): DT(" + oldSizeDT + "," + newSizeDT
				+ ") Site(" + oldSizeS + "," + newSizeS
				+ ") TS(" + oldSizeTs + "," + newSizeTs
				+ ") Other(" + oldOtherS + "," + newOtherS + ")");
			return true;
		}
		else
		{
			ArrayList<TimeSeriesIdentifier> oldTs = oldTsGroup.getTsMemberList();
			ArrayList<TimeSeriesIdentifier> newTs = tsGroup.getTsMemberList();
			for (TimeSeriesIdentifier ddold : oldTs)
			{
				boolean theSame = false;
				for (TimeSeriesIdentifier ddnew : newTs)
				{
					if (ddold.getKey() == ddnew.getKey())
					{
						theSame = true;
						break;
					}
				}
				if (theSame == false)
				{
					Logger.instance().debug3("TS Member Different: no match for key=" + ddold.getKey());
					return true;
				}
			}
		}
		
		// Included subgroups
		int oldSizeIncluded = oldTsGroup.getIncludedSubGroups().size();
		int newSizeIncluded = tsGroup.getIncludedSubGroups().size();
		if (oldSizeIncluded != newSizeIncluded)
		{
			Logger.instance().debug3("Different number of included.");
			return true;
		}
		else
		{
			ArrayList<TsGroup> oldSubG = oldTsGroup.getIncludedSubGroups();
			ArrayList<TsGroup> newSubG = tsGroup.getIncludedSubGroups();
			for (TsGroup tsold : oldSubG)
			{
				boolean theSame = false;
				for (TsGroup tsnew : newSubG)
				{
					if (tsold.getGroupId() == tsnew.getGroupId())
					{
						theSame = true;
						break;
					}
				}
				if (theSame == false)
				{
					Logger.instance().debug3("Included grp different, no match for id=" + tsold.getGroupId());
					return true;
				}
			}
		}

		// Excluded subgroups
		int oldSizeExcluded = oldTsGroup.getExcludedSubGroups().size();
		int newSizeExcluded = tsGroup.getExcludedSubGroups().size();
		if (oldSizeExcluded != newSizeExcluded)
		{
			Logger.instance().debug3("Different number of excluded.");
			return true;
		}
		else
		{
			ArrayList<TsGroup> oldSubG = oldTsGroup.getExcludedSubGroups();
			ArrayList<TsGroup> newSubG = tsGroup.getExcludedSubGroups();
			for (TsGroup tsold : oldSubG)
			{
				boolean theSame = false;
				for (TsGroup tsnew : newSubG)
				{
					if (tsold.getGroupId() == tsnew.getGroupId())
					{
						theSame = true;
						break;
					}
				}
				if (theSame == false)
				{
					Logger.instance().debug3("Excluded grp different, no match for id=" + tsold.getGroupId());
					return true;
				}
			}
		}
		
		// Intersected subgroups
		int oldSizeIntersected = oldTsGroup.getIntersectedGroups().size();
		int newSizeIntersected = tsGroup.getIntersectedGroups().size();
		if (oldSizeIntersected != newSizeIntersected)
		{
			Logger.instance().debug3("Different number of Intersected.");
			return true;
		}
		else
		{
			ArrayList<TsGroup> oldSubG = oldTsGroup.getIntersectedGroups();
			ArrayList<TsGroup> newSubG = tsGroup.getIntersectedGroups();
			for (TsGroup tsold : oldSubG)
			{
				boolean theSame = false;
				for (TsGroup tsnew : newSubG)
				{
					if (tsold.getGroupId() == tsnew.getGroupId())
					{
						theSame = true;
						break;
					}
				}
				if (theSame == false)
				{
					Logger.instance().debug3("Intersected grp different, no match for id=" + tsold.getGroupId());
					return true;
				}
			}
		}

		ArrayList<DbKey> oldDT = oldTsGroup.getDataTypeIdList();
		ArrayList<DbKey> newDT = tsGroup.getDataTypeIdList();
		if (!oldDT.equals(newDT))
		{
			Logger.instance().debug3("DataType Lists different.");
			return true;
		}

		ArrayList<DbKey> oldS = oldTsGroup.getSiteIdList();
		ArrayList<DbKey> newS = tsGroup.getSiteIdList();
		if (!oldS.equals(newS))
		{
			Logger.instance().debug3("Site Lists different.");
			return true;
		}

		ArrayList<TsGroupMember> oldMembers = oldTsGroup.getOtherMembers();
		ArrayList<TsGroupMember> newMembers = tsGroup.getOtherMembers();
		if (oldMembers.size() != newMembers.size())
		{
			Logger.instance().debug3("Different number of 'other' members.");
			return true;
		}
		for(int i=0; i<oldOtherS; i++)
		{
			TsGroupMember om = oldMembers.get(i);
			TsGroupMember nm = newMembers.get(i);
			if (!om.getMemberType().equalsIgnoreCase(nm.getMemberType())
			 || !om.getMemberValue().equalsIgnoreCase(nm.getMemberValue()))
			{
				Logger.instance().debug3("'Other' Lists different: element["
					+ i + "] old=" + om.getMemberType() + ":" + om.getMemberValue()
					+ ", new=" + nm.getMemberType() + ":" + nm.getMemberValue());
				return true;
			}
		}
		return false;
	}

	public boolean saveChanges()
	{
		if (theTsDb != null)
		{ // Create Status windows on main frame and write
			// "Saving", then at end write "Saved Done!"
//			parent.setStatusLabel("Saving!");
			// Save ts group
			try
			{
				theTsDb.writeTsGroup(tsGroup);
				// update group id
				groupIdTextField.setText("" + tsGroup.getGroupId());
				// Set isExpanded
				if (tsGroup.getTsMemberList().size() > 0
				 || tsGroup.getIncludedSubGroups().size() > 0)
					tsGroup.setIsExpanded(true);

				// update ts group list
				((TsDbGrpListPanel) parent.getTsGroupsListPanel())
					.modifyTsGroupList(oldTsGroup, tsGroup);
				resetTabName(tsGroup.getGroupName());
				// Make a new copy of the obj in case user wants to
				// continue editing.
				oldTsGroup = tsGroup;
				copyTsGroup(oldTsGroup);

				// If user added a new Group Type save it
				// to the Database
				if (newType)
				{
					en.replaceValue(ev.getValue(), ev.getDescription(), ev.getExecClassName(),
							"");
					Database.getDb().enumList.write();
				}
			} catch (DbIoException ex)
			{
				Logger.instance().failure(
						module + " Can not write Ts Group to the "
								+ "Database " + ex.getMessage());
				TopFrame.instance().showError(ex.toString());
				return false;
			} catch (DatabaseException ex)
			{
				Logger.instance().failure(
						module + " Can not add new Group Type to the "
								+ "Database " + ex.getMessage());
				TopFrame.instance().showError(ex.toString());
				return false;
			}
		} else
		{
			Logger.instance().failure(
					module + ":saveChanges() The TsDb obj is null.");
			return false;
		}
		return true;
	}

	private void deleteQueryParam()
	{
		int idx = queryTable.getSelectedRow();
//		int idx = queryList.getSelectedIndex();
		if (idx == -1)
			return;
		queryModel.deleteItemAt(idx);
//		queryListModel.remove(idx);
	}

	@SuppressWarnings("unchecked")
	private void addQueryParam(String keyStr)
	{
		String selection = null;
		if (keyStr.equalsIgnoreCase("site") || keyStr.equalsIgnoreCase("location"))
		{
			if (siteSelectDlg == null)
			{
				siteSelectDlg = new SiteSelectDialog(TopFrame.instance());
				siteSelectDlg.setMultipleSelection(true);
			}
			else
				siteSelectDlg.clearSelection();
			parent.launchDialog(siteSelectDlg);
			Site sites[] = siteSelectDlg.getSelectedSites();
			if (sites == null || sites.length == 0)
				return;
			
			for(Site aSite: sites)
			{
				if (!knownSites.contains(aSite))
					knownSites.add(aSite);
				// MJM 20150731 - don't use getDisplayName, but rather the location ID.
				SiteName siteName = theTsDb.isCwms() ? aSite.getName(Constants.snt_CWMS) : null;
				if (siteName == null)
					siteName = aSite.getPreferredName();
				//MJM20150803 Instead of a list item with keyStr prefix, keyStr is the left column, 
				// nameValue is the right column
				queryModel.addItem(keyStr, siteName.getNameValue());
//				String listItem = keyStr + ": " + siteName.getNameValue();
//				if (!queryListModel.contains(listItem))
//					queryListModel.addElement(listItem);
			}
		}
		else if (keyStr.equalsIgnoreCase("datatype") || keyStr.equalsIgnoreCase("param"))
		{
			String label = "Enter " + keyStr + ":";
			selection = (String)JOptionPane.showInputDialog(this, 
				label, label, JOptionPane.PLAIN_MESSAGE, null,
				dataTypeArray, null);
		}
		else if (keyStr.equalsIgnoreCase("Interval"))
		{
			selection = (String)JOptionPane.showInputDialog(this, 
				enterIntervalLabel, enterIntervalLabel, JOptionPane.PLAIN_MESSAGE, null, 
				intervalArray, null);
		}
		else if (keyStr.equalsIgnoreCase("ParamType"))
		{
			selection = (String)JOptionPane.showInputDialog(this, enterParamTypeLabel,
				enterParamTypeLabel, JOptionPane.PLAIN_MESSAGE, null, 
				paramTypes, null);
		}
		else if (keyStr.equalsIgnoreCase("Duration"))
		{
			selection = (String)JOptionPane.showInputDialog(this, enterDurationLabel,
				enterDurationLabel, JOptionPane.PLAIN_MESSAGE, null, 
				durationArray, null);
		}
		else if (keyStr.equalsIgnoreCase("Version"))
		{
			selection = (String)JOptionPane.showInputDialog(this, enterVersionLabel,
				enterVersionLabel, JOptionPane.PLAIN_MESSAGE, null, 
				versionArray, null);
		}
		else if (keyStr.equalsIgnoreCase(HdbTsId.TABSEL_PART))
		{
			String choices[] = { "R_", "M_" };
			String label = "Enter " + keyStr + ":";
			selection = (String)JOptionPane.showInputDialog(this, label,
				label, JOptionPane.PLAIN_MESSAGE, null, choices, null);
		}
		else if (keyStr.equalsIgnoreCase(HdbTsId.MODELID_PART))
		{
			selection = (String)JOptionPane.showInputDialog(this, "Enter " + keyStr + ":");
		}
		
		// selection may be set from above, or it may be null if user cancelled.
		if (selection != null)
		{
			queryModel.addItem(keyStr, selection);
//			String listItem = keyStr + ": " + selection;
//			if (!queryListModel.contains(listItem))
//				queryListModel.addElement(listItem);
		}
	}

	public void setTimeSeriesSelectDialog(
			TimeSeriesSelectDialog timeSeriesSelectDialog)
	{
		this.timeSeriesSelectDialog = timeSeriesSelectDialog;
	}

	@Override
	public void evaluateEntity()
	{
		if (hasChanged())
		{
			int r = JOptionPane.showConfirmDialog(this,
				"Evalute requires saving group definition to database. OK?");
			if (r != JOptionPane.YES_OPTION)
				return;
			saveChanges();
		}
		if (getDataFromFields(true))
		{
			try
			{
				TsListSelectPanel tlsp = new TsListSelectPanel(theTsDb, true, true);
				tlsp.setTimeSeriesList(theTsDb.expandTsGroup(tsGroup));
				JOptionPane jop = new JOptionPane(tlsp, JOptionPane.PLAIN_MESSAGE);
				JDialog dlg = jop.createDialog(this, "Current Time Series Members");
				dlg.setResizable(true);
				TopFrame.instance().launchDialog(dlg);
			}
			catch (DbIoException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void groupSelected()
	{
	}

}

class QuerySelectorTableModel 
	extends AbstractTableModel
	implements SortingListTableModel
{
	ArrayList<StringPair> items = new ArrayList<StringPair>();
	static int colWidths[] = { 20, 80 };
	int sortColumn = 0;
	Comparator<StringPair> partSorter = 
		new Comparator<StringPair>()
		{
			@Override
			public int compare(StringPair o1, StringPair o2)
			{
				int r = o1.first.compareTo(o2.first);
				if (r != 0)
					return r;
				return o1.second.compareTo(o2.second);
			}
		};
	Comparator<StringPair> valueSorter = 
		new Comparator<StringPair>()
		{
			@Override
			public int compare(StringPair o1, StringPair o2)
			{
				int r = o1.second.compareTo(o2.second);
				if (r != 0)
					return r;
				return o1.first.compareTo(o2.first);
			}
		};
	

	boolean addItem(String tsidPart, String tsidValue)
	{
		for(StringPair sp : items)
			if (sp.first.equalsIgnoreCase(tsidPart) && sp.second.equalsIgnoreCase(tsidValue))
				return false;
		items.add(new StringPair(tsidPart, tsidValue));
		if (sortColumn != -1)
			sortByColumn(sortColumn);
		else
			fireTableDataChanged();
		return true;
	}
	
	void deleteItemAt(int row)
	{
		if (row >= 0 && row < items.size())
		{
			items.remove(row);
			sortByColumn(0);
		}
	}
	
	@Override
	public int getRowCount()
	{
		return items.size();
	}

	@Override
	public int getColumnCount()
	{
		return 2;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		if (columnIndex == 0)
			return "TSID Part";
		else
			return "Value";
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (rowIndex >= items.size())
			return "";
		StringPair sp = items.get(rowIndex);
		return columnIndex == 0 ? sp.first : columnIndex == 1 ? sp.second : "";
	}

	@Override
	public void sortByColumn(int column)
	{
		Collections.sort(items, column == 0 ? partSorter : valueSorter);
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return row >= 0 && row < items.size() ? items.get(row) : null;
	}
	
	void clear()
	{
		items.clear();
	}
	
}
