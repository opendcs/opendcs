/**
 * $Id: TsGroupDefinitionPanel.java,v 1.15 2019/12/11 14:43:38 mmaloney Exp $
 * 
 * $Log: TsGroupDefinitionPanel.java,v $
 * Revision 1.15  2019/12/11 14:43:38  mmaloney
 * Don't explicitly require CwmsTimeSeriesDb. This module is also used with OpenTsdb.
 *
 * Revision 1.14  2018/02/05 15:52:39  mmaloney
 * Added ObjectType filter for USBR HDB.
 *
 * Revision 1.13  2017/05/31 21:34:27  mmaloney
 * GUI improvements for HDB
 *
 * Revision 1.12  2017/05/08 12:34:53  mmaloney
 * For CWMS, TSIDs may contain param values that CCP does not have in its DataType
 * table. If this is the case, save as an other "param" value.
 *
 * Revision 1.11  2017/04/19 19:27:45  mmaloney
 * CWMS-10609 nested group evaluation in group editor bugfix.
 *
 * Revision 1.10  2017/01/10 21:11:35  mmaloney
 * Enhanced wildcard processing for CWMS as per punchlist for comp-depends project
 * for NWP.
 *
 * Revision 1.9  2016/11/21 16:04:03  mmaloney
 * Code Cleanup.
 *
 * Revision 1.8  2016/11/03 19:08:05  mmaloney
 * Implement new Location, Param, and Version dialogs for CWMS.
 *
 * Revision 1.7  2016/10/11 17:40:36  mmaloney
 * Final GUI Prototype
 *
 * Revision 1.6  2016/07/20 15:45:46  mmaloney
 * Special code for HDB to show data type common names.
 *
 * Revision 1.5  2016/04/22 14:42:53  mmaloney
 * Code cleanup.
 *
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
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
import opendcs.dai.SiteDAI;
import opendcs.dai.TsGroupDAI;
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
import decodes.hdb.HdbDataType;
import decodes.hdb.HdbObjectType;
import decodes.hdb.HdbTimeSeriesDb;
import decodes.hdb.HdbTsId;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsGroupMember;
import decodes.tsdb.TsGroupMemberType;
import decodes.tsdb.TsdbAppTemplate;
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
	/*	Other group members */
	private JPanel queryPanel;
	
	//Time Series DB
	private TimeSeriesDb tsdb;
	//Miscellaneous
	private boolean newType;
	private decodes.db.DbEnum en;
	private EnumValue ev;
	
	/** The group as it exists in the database, either upon entry to the editor or after a save. */
	private TsGroup origTsGroup;

	/** The local copy being edited. */
	private TsGroup editedGroup;

	private SubGroupTableModel subgroupModel = new SubGroupTableModel();
	private SortingListTable subgroupTable = new SortingListTable(subgroupModel,
		SubGroupTableModel.colwidth);
	
	private TsListSelectPanel tsListSelectPanel;
	
//	private ArrayList<Site> knownSites = new ArrayList<Site>();
	private ArrayList<DataType> dataTypeList = new ArrayList<DataType>();
//	private String[] dataTypeArray = null;
	private String[] intervalArray = null;
	private String[] durationArray = null;
	private String[] paramTypes = null;

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
	private String saveChangesLabel;
	private String tsDeleteMsg;
	private String groupNameEmptyMsg;
	private String groupTEmptyMsg;
	private TimeSeriesSelectDialog timeSeriesSelectDialog = null;
	private QuerySelectorTableModel queryModel = new QuerySelectorTableModel();
	private SortingListTable queryTable = null;
	private TsGroupsSelectTableModel parentGroupListModel = null;
	private TsDbGrpListPanel parentListPanel = null;
	
	public TsGroupDefinitionPanel()
	{	
		//For internationalization, get the descriptions of 
		//title and all labels from properties file
		setAllLabels();

		try
		{	
			//Set the time series database
			tsdb = TsdbAppTemplate.theDb;
			
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
			
//			
//			dataTypeArray = new String[dataTypeList.size()];
//			for(int i=0; i<dataTypeList.size(); i++)
//				dataTypeArray[i] = dataTypeList.get(i).getCode();
//			Arrays.sort(dataTypeArray);
			
			IntervalDAI intervalDAO = tsdb.makeIntervalDAO();
			try
			{
				intervalArray = intervalDAO.getValidIntervalCodes();
				durationArray = intervalDAO.getValidDurationCodes();
			}
			finally
			{
				intervalDAO.close();
			}
			paramTypes = tsdb.getParamTypes();
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
		saveChangesLabel = genericResources.getString("saveChanges");
		tsDeleteMsg = groupResources
					.getString("TsGroupDefinitionPanel.tsDeleteMsg");
		groupNameEmptyMsg = groupResources
					.getString("TsGroupDefinitionPanel.groupNameEmptyMsg");
		groupTEmptyMsg = groupResources
					.getString("TsGroupDefinitionPanel.groupTEmptyMsg");
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
		descriptionTextArea = new JTextArea(4, 0);
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
				new Insets(7, 30, 0, 0), 2, 5));
		northPanel.add(groupIdTextField, new GridBagConstraints(1, 0, 1, 1,
				1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, 
				new Insets(7, 0, 0, 1), 62, 5));
		northPanel.add(groupNameLabel, new GridBagConstraints(0, 1, 1, 1, 0.0,
				0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 30, 0, 0), 2, 5));
		northPanel.add(groupNameTextField, new GridBagConstraints(1, 1, 1, 1,
				1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(3, 0, 0, 1), 62, 5));
		northPanel.add(renameButton, new GridBagConstraints(2, 1, 1, 1, 0.0,
				0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 20, 0, 300), 0, 0));
		northPanel.add(groupTypeLabel, new GridBagConstraints(0, 2, 1, 1, 0.0,
				0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 30, 0, 0), 2, 5));
		northPanel.add(groupTypeComboBox, new GridBagConstraints(1, 2, 1, 1,
				1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(3, 0, 0, 1), 62, 5));
		northPanel.add(newTypeButton, new GridBagConstraints(2, 2, 1, 1, 0.0,
				0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 20, 0, 300), 0, 0));
		northPanel.add(descriptionLabel, new GridBagConstraints(0, 3, 1, 1,
				0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 30, 0, 0), 2, 5));
		northPanel.add(descJScrollPane, new GridBagConstraints(1, 3, 2, 1, 1.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 0, 110), 62, 25));
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
		tsListSelectPanel = new TsListSelectPanel(tsdb, false, true);
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
						new Insets(3, 12, 2, 12), 16, 0));
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
		
		JPanel subgroupPanel = new JPanel(new BorderLayout());
		JScrollPane subgroupScrollPane = new JScrollPane(
			subgroupTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		subgroupTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		subgroupPanel.add(subgroupScrollPane, BorderLayout.CENTER);

		JButton addSGButton = new JButton("Add SubGroup");
		addSGButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					addSubgroupPressed();
				}
			});
		JButton subtractSGButton = new JButton("Subtract SubGroup");
		subtractSGButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					subtractSubgroupPressed();
				}
			});
		
		JButton intersectSubgroupButton = new JButton("Intersect SubGroup");
		intersectSubgroupButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					intersectSubgroupPressed();
				}
			});
		JButton deleteSubgroupButton = new JButton("Delete");
		deleteSubgroupButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					deleteSubgroupPressed();
				}
			});

		subGroupMembersPanel.add(subgroupPanel,
			new GridBagConstraints(0, 0, 1, 4, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(4, 5, 4, 5), 0, 0));
		subGroupMembersPanel.add(addSGButton,
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
				new Insets(1, 12, 1, 12), 55, 0));
		subGroupMembersPanel.add(subtractSGButton,
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 12, 1, 12), 55, 0));
		subGroupMembersPanel.add(intersectSubgroupButton,
			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 12, 1, 12), 55, 0));
		subGroupMembersPanel.add(deleteSubgroupButton,
			new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(1, 12, 1, 12), 55, 0));
	}

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
		
		queryTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
						modifyQueryParam();
				}
			});

		listPane.getViewport().add(queryTable, null);

		queryPanel.add(buttonPanel, BorderLayout.EAST);
		queryPanel.add(listPane, BorderLayout.CENTER);
		queryPanel.setBorder(new TitledBorder(queryPanelLabelStr));

		// Now put buttons for each of the time-series identifier parts.
		String[] tsIdParts = tsdb.getTsIdParts();
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
					new Insets(2, 10, 2, 10), 0, 0));
		}
		if (tsdb.isHdb())
		{
			final String tag = "ObjectType";
			JButton button = new JButton(tag);
			button.addActionListener(
				new java.awt.event.ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						addQueryParam(tag);
					}
				});
			buttonPanel.add(button,
				new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(2, 10, 2, 10), 0, 0));
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
				new Insets(2, 10, 2, 10), 0, 0));
	}

	/**
	 * @param parent the enclosing TsDbEditorFrame.
	 */
	public void setParent(TopFrame parent)
	{
		this.parent = parent;
	}

	/**
	 * Set the group to be edited
	 * @param tsGroup the group to be edited
	 */
	public void setTsGroup(TsGroup tsGroupIn)
	{
		origTsGroup = tsGroupIn;
		editedGroup = origTsGroup.copy(origTsGroup.getGroupName());
		editedGroup.setGroupId(origTsGroup.getGroupId());
		fillFields();
	}

	private void fillFields()
	{
		groupIdTextField.setText("");
		if (editedGroup.getGroupId() != Constants.undefinedId)
			groupIdTextField.setText("" + editedGroup.getGroupId());

		groupNameTextField.setText(editedGroup.getGroupName() == null ? ""
				: editedGroup.getGroupName());

		if (editedGroup.getGroupType() != null)
			groupTypeComboBox.setSelection(editedGroup.getGroupType());

		descriptionTextArea.setText(editedGroup.getDescription() == null ? ""
				: editedGroup.getDescription());

		// Populate sub lists
		ArrayList<TimeSeriesIdentifier> ddlist = new ArrayList<TimeSeriesIdentifier>();
		for(TimeSeriesIdentifier tsid : editedGroup.getTsMemberList())
			ddlist.add(tsid);
		tsListSelectPanel.setTimeSeriesList(ddlist);
		
		for(TsGroup g : editedGroup.getIncludedSubGroups())
			subgroupModel.add(g, "Add");
		for(TsGroup g : editedGroup.getExcludedSubGroups())
			subgroupModel.add(g, "Subtract");
		for(TsGroup g : editedGroup.getIntersectedGroups())
			subgroupModel.add(g, "Intersect");

		//Load other group members from the TsDB
		queryModel.clear();
		String[] tsIdParts = tsdb.getTsIdParts();

		for(DbKey siteId: editedGroup.getSiteIdList())
		{
			try
			{
				Site st = tsdb.getSiteById(siteId);
				SiteName sn = null;
				if (tsdb.isCwms())
					sn = st.getName(Constants.snt_CWMS);
				if (sn == null)
					sn = st.getPreferredName();
				
//MJM 20161027
//				if (!knownSites.contains(st))
//					knownSites.add(st);
				// Location or Site is always the first part of the TSID.
				queryModel.items.add(new StringPair(tsIdParts[0], sn.getNameValue()));
			}
			catch(Exception ex)
			{
				Logger.instance().warning("Cannot read site with id=" 
					+ siteId + ": " + ex);
			}
		}
		
		if (tsdb.isHdb())
		{
			ArrayList<HdbDataType> hdts = ((HdbTimeSeriesDb)tsdb).getHdbDataTypes();
			for(DbKey dtId: editedGroup.getDataTypeIdList())
			{
				HdbDataType hdt = null;
				for (HdbDataType thdt : hdts)
					if (dtId.equals(thdt.getDataTypeId()))
					{
						hdt = thdt;
						break;
					}
				queryModel.items.add(new StringPair("DataType",
					dtId.toString() + (hdt==null 
						? "" : " (" + hdt.getName() + ")")));
			}
		}
		else // cwms
		{
			for(DbKey dtId: editedGroup.getDataTypeIdList())
			{
				try
				{
					DataType dt = DataType.getDataType(dtId);
					queryModel.items.add(new StringPair(tsIdParts[1], dt.getCode()));
				}
				catch(Exception ex)
				{
					Logger.instance().warning("Cannot read datatype with id=" 
						+ dtId + ": " + ex);
				}
			}
		}
		for(TsGroupMember member : editedGroup.getOtherMembers())
		{
			queryModel.items.add(new StringPair(member.getMemberType(), member.getMemberValue()));
//			queryListModel.addElement(member.getMemberType() + ": "
//				+ member.getMemberValue());
		}
		queryModel.sortByColumn(0);
	}

	/**
	 * Collect data from the GUI controls and store into editedGroup.
	 * @param validationFlag true to do basic evaluation.
	 * @return true if passed validation and OK to save.
	 */
	private boolean getDataFromFields(boolean validationFlag)
	{
		TsGroup tempGroup = null;
		tempGroup = new TsGroup();
		tempGroup.setGroupId(editedGroup.getGroupId());
		tempGroup.setIsExpanded(editedGroup.getIsExpanded());

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
		for(TimeSeriesIdentifier dd: tsListSelectPanel.getAllTSIDsInList())
		{	
			tempGroup.addTsMember(dd);
		}


		//Feed other group members to the TsDb
		SiteDAI siteDAO = tsdb.makeSiteDAO();
		DataType dt = null;
		try
		{
			for(StringPair sp : queryModel.items)
			{
				String label = sp.first;
				String value = sp.second;
				
				if (label.equalsIgnoreCase("site") 
				 || (label.equalsIgnoreCase("location") && !value.contains("*")))
				{
					DbKey siteId = siteDAO.lookupSiteID(sp.second);
					if (siteId != null)
						tempGroup.addSiteId(siteId);
					else
						Logger.instance().warning("No match for sitename '" + value + "' -- ignored.");
				}
				else if (tsdb.isHdb() && label.equalsIgnoreCase("datatype"))
				{
					int paren = value.indexOf('(');
					if (paren > 0)
						value = value.substring(0, paren).trim();
					try
					{
						tempGroup.addDataTypeId(DbKey.createDbKey(Long.parseLong(value.trim())));
					}
					catch(NumberFormatException ex)
					{
						if ((dt = lookupDataType(value)) != null)
							tempGroup.addDataTypeId(dt.getId());
						else
							Logger.instance().warning("Unrecognized data type '" 
								+ value + "' -- ignored.");
					}
				}
				else if (label.equalsIgnoreCase("param") 
					&& !value.contains("*")
					&& (dt = lookupDataType(value)) != null)
				{
					tempGroup.addDataTypeId(dt.getId());
				}
				else 
					tempGroup.addOtherMember(label, value);
			}
		}
		catch(DbIoException ex)
		{
			Logger.instance().warning(module + " Error looking up site: " + ex);
		}
		finally
		{
			siteDAO.close();
		}
		
		TsGroupDAI tsGroupDAO = tsdb.makeTsGroupDAO();
		try
		{
			for(SubGroupReference ref : subgroupModel.subgroups)
			{
				TsGroup subgroup = tsGroupDAO.getTsGroupById(ref.groupId);
				if (subgroup != null)
					tempGroup.addSubGroup(subgroup, 
						ref.combine.equalsIgnoreCase("Intersect") ? 'I' :
						ref.combine.equalsIgnoreCase("Subtract") ? 'S' : 'A');
			}
		}
		catch (DbIoException ex)
		{
			Logger.instance().warning(module + " Error finding subgroup: " + ex);
		}
		finally
		{
			tsGroupDAO.close();
		}
		
		editedGroup = tempGroup;
		return true;
	}

	private DataType lookupDataType(String value)
	{
		for(DataType dt : dataTypeList)
			if (dt.getCode().equalsIgnoreCase(value))
				return dt;
		return null;
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
		if (editedGroup != null)
		{ // Verify if the name entered is the same that we have on this
			// group, first check the textfield
			if (newGroupName.equalsIgnoreCase((groupNameTextField.getText())
					.trim()))
				return;
			if (newGroupName.equalsIgnoreCase(editedGroup.getGroupName()))
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
			obs[i] = tsListSelectPanel.getTSIDAt(rows[i]);

		for (int i = 0; i < nrows; i++)
			tsListSelectPanel.deleteTSID(obs[i]);
	}

	/**
	 * User Presses Sub Group Members Add button
	 */
	public void addSubgroupPressed()
	{
		TsGroupSelectDialog dlg = new TsGroupSelectDialog(parent);
		dlg.setMultipleSelection(false);
		parent.launchDialog(dlg);
		
		TsGroup selectedGrps[] = dlg.getSelectedTsGroups();
		if (selectedGrps.length == 0)
			return;
		
		for (TsGroup g: selectedGrps)
		{
			if (g.getGroupName().equals(editedGroup.getGroupName()))
			{
				String msgStr = "Group " + editedGroup.getGroupName()
					+" cannot add itself as a subgroup!";
				JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msgStr, 60),
						subGroupMembersTitleLabelStr, JOptionPane.WARNING_MESSAGE);
				continue;
			}
			
//			g.setInclusion("Add");
			subgroupModel.add(g, "Add");
		}
	}

	/**
	 * User Presses Sub Group Members Add button
	 */
	public void subtractSubgroupPressed()
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
			if (g.getGroupName().equals(editedGroup.getGroupName()))
			{
				String msgStr = "Group "+editedGroup.getGroupName()
					+ " cannot add itself as a subgroup!";
				JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msgStr, 60),
					subGroupMembersTitleLabelStr, JOptionPane.WARNING_MESSAGE);
				continue;
			}

//			g.setInclusion("Subtract");
			subgroupModel.add(g, "Subtract");
		}
	}
	
	private void intersectSubgroupPressed()
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
			if (g.getGroupName().equals(editedGroup.getGroupName()))
			{
				String msgStr = "Group "+editedGroup.getGroupName()
					+ " cannot add itself as a subgroup!";
				JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msgStr, 60),
						subGroupMembersTitleLabelStr, JOptionPane.WARNING_MESSAGE);
				continue;
			}
			
//			g.setInclusion("Intersect");
			subgroupModel.add(g, "Intersect");
		}
		
	}

	/**
	 * User Presses Sub Group Members Delete button
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void deleteSubgroupPressed()
	{
		// Find out how many Data Descriptors were selected
		int rows[] = this.subgroupTable.getSelectedRows();
		if (rows == null || rows.length == 0)
			return;
		
		Arrays.sort(rows);
		for (int i = rows.length-1; i >= 0; i--)
			this.subgroupModel.removeAt(rows[i]);
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
		if (!TextUtil.strEqualIgnoreCase(origTsGroup.getGroupName(), editedGroup
				.getGroupName()))
		{
			Logger.instance().debug3("group name has changed");
			return true;
		}
		if (!TextUtil.strEqualIgnoreCase(origTsGroup.getGroupType(), editedGroup.getGroupType()))
		{
			Logger.instance().debug3("group type has changed, was '"
				+ origTsGroup.getGroupType() + "' is now '" + editedGroup.getGroupType() + "'");
			return true;
		}
		if (!TextUtil.strEqualIgnoreCase(origTsGroup.getDescription(), editedGroup
				.getDescription()))
		{
			Logger.instance().debug3("group description has changed");
			return true;
		}

		int oldSizeTs = origTsGroup.getTsMemberList().size();
		int newSizeTs = editedGroup.getTsMemberList().size();
		int oldSizeDT = origTsGroup.getDataTypeIdList().size();
		int newSizeDT = editedGroup.getDataTypeIdList().size();
		int oldSizeS = origTsGroup.getSiteIdList().size();
		int newSizeS = editedGroup.getSiteIdList().size();
		int oldOtherS = origTsGroup.getOtherMembers().size();
		int newOtherS = editedGroup.getOtherMembers().size();
		
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
			ArrayList<TimeSeriesIdentifier> oldTs = origTsGroup.getTsMemberList();
			ArrayList<TimeSeriesIdentifier> newTs = editedGroup.getTsMemberList();
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
		int oldSizeIncluded = origTsGroup.getIncludedSubGroups().size();
		int newSizeIncluded = editedGroup.getIncludedSubGroups().size();
		if (oldSizeIncluded != newSizeIncluded)
		{
			Logger.instance().debug3("Different number of included.");
			return true;
		}
		else
		{
			ArrayList<TsGroup> oldSubG = origTsGroup.getIncludedSubGroups();
			ArrayList<TsGroup> newSubG = editedGroup.getIncludedSubGroups();
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
		int oldSizeExcluded = origTsGroup.getExcludedSubGroups().size();
		int newSizeExcluded = editedGroup.getExcludedSubGroups().size();
		if (oldSizeExcluded != newSizeExcluded)
		{
			Logger.instance().debug3("Different number of excluded.");
			return true;
		}
		else
		{
			ArrayList<TsGroup> oldSubG = origTsGroup.getExcludedSubGroups();
			ArrayList<TsGroup> newSubG = editedGroup.getExcludedSubGroups();
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
		int oldSizeIntersected = origTsGroup.getIntersectedGroups().size();
		int newSizeIntersected = editedGroup.getIntersectedGroups().size();
		if (oldSizeIntersected != newSizeIntersected)
		{
			Logger.instance().debug3("Different number of Intersected.");
			return true;
		}
		else
		{
			ArrayList<TsGroup> oldSubG = origTsGroup.getIntersectedGroups();
			ArrayList<TsGroup> newSubG = editedGroup.getIntersectedGroups();
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

		ArrayList<DbKey> oldDT = origTsGroup.getDataTypeIdList();
		ArrayList<DbKey> newDT = editedGroup.getDataTypeIdList();
		if (!oldDT.equals(newDT))
		{
			Logger.instance().debug3("DataType Lists different.");
			return true;
		}

		ArrayList<DbKey> oldS = origTsGroup.getSiteIdList();
		ArrayList<DbKey> newS = editedGroup.getSiteIdList();
		if (!oldS.equals(newS))
		{
			Logger.instance().debug3("Site Lists different.");
			return true;
		}

		ArrayList<TsGroupMember> oldMembers = origTsGroup.getOtherMembers();
		ArrayList<TsGroupMember> newMembers = editedGroup.getOtherMembers();
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
		TsGroupDAI tsGroupDAO = tsdb.makeTsGroupDAO();
		try
		{
			// Write to DB and replace in cache.
			tsGroupDAO.writeTsGroup(editedGroup);
			
			// update group id
			groupIdTextField.setText("" + editedGroup.getGroupId());
			
			// update ts group list
			((TsDbGrpListPanel) parent.getTsGroupsListPanel()).modifyTsGroupList(origTsGroup, editedGroup);
			resetTabName(editedGroup.getGroupName());
			
			// The edited group is now the orig group.
			// Make a new copy of the obj in case user wants to
			// continue editing.
			origTsGroup = editedGroup;
			editedGroup = origTsGroup.copy(origTsGroup.getGroupName());
			editedGroup.setGroupId(origTsGroup.getGroupId());

			// If user added a new Group Type save it
			// to the Database
			if (newType)
			{
				en.replaceValue(ev.getValue(), ev.getDescription(), ev.getExecClassName(), "");
				Database.getDb().enumList.write();
			}
			
			// MJM CWMS-10609 Replace the group in the model with the new one.
			parentGroupListModel.replaceTsGroup(editedGroup);
			
			// Update the table models of subgroups in any open tabs besides this one.
			parentListPanel.updateSubGroups(editedGroup);
		} 
		catch (DbIoException ex)
		{
			Logger.instance().failure(
					module + " Can not write Ts Group to the "
							+ "Database " + ex.getMessage());
			TopFrame.instance().showError(ex.toString());
			return false;
		}
		catch (DatabaseException ex)
		{
			Logger.instance().failure(
					module + " Can not add new Group Type to the "
							+ "Database " + ex.getMessage());
			TopFrame.instance().showError(ex.toString());
			return false;
		}
		finally
		{
			tsGroupDAO.close();
		}
		return true;
	}

	private void deleteQueryParam()
	{
		int rows[] = queryTable.getSelectedRows();
		if (rows == null || rows.length == 0)
			return;
		for(int idx = rows.length-1; idx >= 0; idx--)
			queryModel.deleteItemAt(rows[idx]);
	}

	private void addQueryParam(String keyStr)
	{
		String selection = null;
		if (keyStr.equalsIgnoreCase("site"))
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
//				if (!knownSites.contains(aSite))
//					knownSites.add(aSite);
				// MJM 20150731 - don't use getDisplayName, but rather the location ID.
				SiteName siteName = tsdb.isCwms() ? aSite.getName(Constants.snt_CWMS) : null;
				if (siteName == null)
					siteName = aSite.getPreferredName();
				//MJM20150803 Instead of a list item with keyStr prefix, keyStr is the left column, 
				// nameValue is the right column
				queryModel.addItem(keyStr, siteName.getNameValue());
			}
		}
		else if (keyStr.equalsIgnoreCase("location"))
		{
			// CWMS Location Selection
			LocSelectDialog locSelectDialog = new LocSelectDialog(this.parent, tsdb,
				SelectionMode.GroupEdit);
			parent.launchDialog(locSelectDialog);
			if (!locSelectDialog.isCancelled())
			{
				StringPair result = locSelectDialog.getResult();
				if (result != null)
				{
					queryModel.addItem(result.first, result.second);
				}
			}
		}
		else if (keyStr.equalsIgnoreCase("datatype"))
		{
			if (!tsdb.isHdb())
				return;
			
			HdbDatatypeSelectDialog dlg = new HdbDatatypeSelectDialog(parent, (HdbTimeSeriesDb)tsdb);
			
			parent.launchDialog(dlg);
			StringPair sel = dlg.getResult();
			if (sel != null)
				selection = sel.first + " (" + sel.second + ")";
		}
		else if (keyStr.equalsIgnoreCase("param"))
		{
			// CWMS Param Selection
			ParamSelectDialog paramSelectDialog = new ParamSelectDialog(this.parent, tsdb,
				SelectionMode.GroupEdit);
			parent.launchDialog(paramSelectDialog);
			if (!paramSelectDialog.isCancelled())
			{
				StringPair result = paramSelectDialog.getResult();
				if (result != null)
					queryModel.addItem(result.first, result.second);
			}
		}
		else if (keyStr.equalsIgnoreCase(TsGroupMemberType.Interval.toString()))
		{
			selection = (String)JOptionPane.showInputDialog(this, 
				enterIntervalLabel, enterIntervalLabel, JOptionPane.PLAIN_MESSAGE, null, 
				intervalArray, null);
		}
		else if (keyStr.equalsIgnoreCase(TsGroupMemberType.ParamType.toString()))
		{
			selection = (String)JOptionPane.showInputDialog(this, enterParamTypeLabel,
				enterParamTypeLabel, JOptionPane.PLAIN_MESSAGE, null, 
				paramTypes, null);
		}
		else if (keyStr.equalsIgnoreCase(TsGroupMemberType.Duration.toString()))
		{
			selection = (String)JOptionPane.showInputDialog(this, enterDurationLabel,
				enterDurationLabel, JOptionPane.PLAIN_MESSAGE, null, 
				durationArray, null);
		}
		else if (keyStr.equalsIgnoreCase(TsGroupMemberType.Version.toString()))
		{
			// CWMS Param Selection
			VersionSelectDialog versionSelectDialog = new VersionSelectDialog(this.parent, tsdb,
				SelectionMode.GroupEdit);
			parent.launchDialog(versionSelectDialog);
			if (!versionSelectDialog.isCancelled())
			{
				StringPair result = versionSelectDialog.getResult();
				if (result != null)
					queryModel.addItem(result.first, result.second);
			}
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
		else if (keyStr.equalsIgnoreCase("ObjectType"))
		{
			ArrayList<HdbObjectType> hots = ((HdbTimeSeriesDb)tsdb).getHdbObjectTypes();
			String hotnames[] = new String[hots.size()];
			for(int idx = 0; idx < hotnames.length; idx++)
				hotnames[idx] = hots.get(idx).getName();
			
			selection = (String)JOptionPane.showInputDialog(this, 
				"Select Object Type:", "Select Object Type", JOptionPane.PLAIN_MESSAGE, null, 
				hotnames, null);
		}
		
		// selection may be set from above, or it may be null if user cancelled.
		if (selection != null)
		{
			queryModel.addItem(keyStr, selection);
		}
	}
	protected void modifyQueryParam()
	{
		int r = queryTable.getSelectedRow();
		if (r == -1)
			return;
		StringPair query = (StringPair)queryModel.getRowObject(r);
		if (query == null)
			return;
		String keyStr = query.first;
		if (keyStr.toLowerCase().contains("location"))
		{
			// CWMS Location Selection
			LocSelectDialog locSelectDialog = new LocSelectDialog(this.parent, tsdb,
				SelectionMode.GroupEdit);
			locSelectDialog.setResult(query);
			
			parent.launchDialog(locSelectDialog);
			if (!locSelectDialog.isCancelled())
			{
				StringPair result = locSelectDialog.getResult();
				if (result != null)
					queryModel.setValueAt(r, result);
			}
		}
		else if (keyStr.toLowerCase().endsWith("param"))
		{
			// CWMS Param Selection
			ParamSelectDialog paramSelectDialog = new ParamSelectDialog(this.parent, tsdb,
				SelectionMode.GroupEdit);
			paramSelectDialog.setResult(query);
			parent.launchDialog(paramSelectDialog);
			if (!paramSelectDialog.isCancelled())
			{
				StringPair result = paramSelectDialog.getResult();
				if (result != null)
					queryModel.setValueAt(r, result);
			}
		}
		else if (keyStr.toLowerCase().contains("version"))
		{
			// CWMS Param Selection
			VersionSelectDialog versionSelectDialog = new VersionSelectDialog(this.parent, tsdb,
				SelectionMode.GroupEdit);
			versionSelectDialog.setResult(query);
			parent.launchDialog(versionSelectDialog);
			if (!versionSelectDialog.isCancelled())
			{
				StringPair result = versionSelectDialog.getResult();
				if (result != null)
					queryModel.setValueAt(r, result);
			}
		}
		else
			return;
		
	}



	public void setTimeSeriesSelectDialog(
			TimeSeriesSelectDialog timeSeriesSelectDialog)
	{
		this.timeSeriesSelectDialog = timeSeriesSelectDialog;
	}

	@Override
	public void evaluateEntity()
	{
		if (getDataFromFields(true))
		{
			try
			{
				GroupEvalTsidsDialog dlg = new GroupEvalTsidsDialog(parent, tsdb, 
					tsdb.expandTsGroup(editedGroup));
				parent.launchDialog(dlg);
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

	public void setGroupListModel(TsGroupsSelectTableModel model)
	{
		parentGroupListModel = model;
	}

	public void setParentListPanel(TsDbGrpListPanel tsDbGrpListPanel)
	{
		parentListPanel  = tsDbGrpListPanel;
	}

	public TsGroup getEditedGroup()
	{
		return editedGroup;
	}

	/**
	 * If the passed just-savedGroup is a sub-group to the one being edited
	 * in this panel, replace it in the model.
	 * @param savedGroup
	 */
	public void replaceSubGroup(TsGroup savedGroup)
	{
		for(int idx = 0; idx < subgroupModel.getRowCount(); idx++)
		{
			SubGroupReference ref = subgroupModel.subgroups.get(idx);
			if (savedGroup.getGroupId().equals(ref.groupId))
			{
				ref.groupName = savedGroup.getGroupName();
				ref.groupType = savedGroup.getGroupType();
				ref.groupDesc = savedGroup.getDescription();
				break;
			}
		}
	}

}

@SuppressWarnings("serial")
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
	
	public void setValueAt(int row, StringPair v)
	{
		items.set(row, v);
		fireTableDataChanged();
	}
	
	void clear()
	{
		items.clear();
	}
	
}
