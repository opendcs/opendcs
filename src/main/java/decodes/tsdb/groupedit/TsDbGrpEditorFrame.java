/*
 * $Id:
 * 
 * $Log:
 * 
 */
package decodes.tsdb.groupedit;

import ilex.util.LoadResourceBundle;
import ilex.util.Logger;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ResourceBundle;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import decodes.db.Site;
import decodes.gui.TopFrame;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;

/**
 * This is the main frame for the Time Series Database Group Editor.
 * The frame can contain the Time Series Groups Tab, or may include
 * the Time Series Data Descriptor Tab and the Alarms Tab.
 */
@SuppressWarnings("serial")
public class TsDbGrpEditorFrame extends TopFrame
{
	//Frame 
	private String module = "TsDbGrpEditorFrame";
	//Frame Owner 

	//Frame Components
	private JPanel contentPane;
//	private JTabbedPane topLevelTabs;
	private JPanel tsGroupTab;
	private JPanel tsGroupListTab;
	private TsDbGrpEditorTabbedPane tsGroupListTabbedPane;
	private TsDbGrpListPanel tsGroupsListPanel;
	//Time Series DB
	private static TimeSeriesDb theTsDb;
	//Miscellaneous
	private static String dataTypeStandard;
	private String frameTitle;
	private String listTabLabel;
	
	/**
	 * Construct the frame
	 */
	public TsDbGrpEditorFrame(TimeSeriesDb theTsDb)
	{
		ResourceBundle groupResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/groupedit",
			DecodesSettings.instance().language);
		ResourceBundle genericResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic",
			DecodesSettings.instance().language);

		//Set the time series DB
		TsDbGrpEditorFrame.theTsDb = null;
		setTsDb(theTsDb);
		
		//Set the data type standard
		dataTypeStandard = null;
		setDataTypeStandard(DecodesSettings.instance().dataTypeStdPreference);
		Logger.instance().info(module + 
			" dataTypeStdPreference From Decodes.properties = " + dataTypeStandard);
		
		//Set all labels
		if (groupResources != null)
		{
			frameTitle = groupResources
				.getString("TsDbGrpEditorFrame.frameTitle")
				+ ": " + (DecodesSettings.instance()).editDatabaseLocation;
			listTabLabel = genericResources.getString("list");
		}

		try
		{
			jbInit();
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		trackChanges("TsGroupEditor");
	}
	
	/**
	 * Return the frame instance
	 */
	public static TsDbGrpEditorFrame getInstance()
	{
		return (TsDbGrpEditorFrame) instance();
	}

	/**
	 * Returns the time series DB object.
	 */
	public static TimeSeriesDb getTsDb()
	{
		return theTsDb;
	}

	/**
	 * Set the time series DB 
	 */
	public static void setTsDb(TimeSeriesDb theTsDb)
	{
		TsDbGrpEditorFrame.theTsDb = theTsDb;
	}

	/**
	 * Get data type standard
	 * 
	 * @return String
	 */
	public static String getDataTypeStandard()
	{
		return TsDbGrpEditorFrame.dataTypeStandard;
	}

	/**
	 * Set data type standard
	 * 
	 * @param dataTypeStandard
	 */
	public static void setDataTypeStandard(String dataTypeStandard)
	{
		TsDbGrpEditorFrame.dataTypeStandard = dataTypeStandard;
	}
	
	/** @return the tabbed pane for TS Groups */
	@Override
	public TsDbGrpEditorTabbedPane getTsGroupsListTabbedPane()
	{
		return tsGroupListTabbedPane;
	}

	/** @return the TS Groups List Panel. */
	@Override
	public TsDbGrpListPanel getTsGroupsListPanel()
	{
		return tsGroupsListPanel;
	}

	/**
	 * Component initialization
	 * @throws Exception
	 */
	private void jbInit() throws Exception
	{
		//Initialize the components
		tsGroupTab = new JPanel();
		tsGroupListTab = new JPanel();
		tsGroupListTabbedPane = new TsDbGrpEditorTabbedPane();
		tsGroupsListPanel = new TsDbGrpListPanel();

		//Set up the frame dimension
		this.setSize(new Dimension(863, 768));//763, 803 763, 760
		this.setTitle(frameTitle);   
		contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(tsGroupTab, BorderLayout.CENTER);
		tsGroupTab.setLayout(new BorderLayout());
		tsGroupTab.add(tsGroupListTabbedPane, BorderLayout.CENTER);
		tsGroupListTab.setLayout(new BorderLayout());
		tsGroupListTabbedPane.add(tsGroupListTab, listTabLabel);
		tsGroupListTab.add(tsGroupsListPanel, BorderLayout.CENTER);
		
		tsGroupsListPanel.setParent(this);
	}
}
