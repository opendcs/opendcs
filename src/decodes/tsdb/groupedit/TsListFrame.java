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

import decodes.gui.TopFrame;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;

/**
 * This is the main frame for the Time Series Database Group Editor.
 * The frame can contain the Time Series Groups Tab, or may include
 * the Time Series Data Descriptor Tab and the Alarms Tab.
 */
@SuppressWarnings("serial")
public class TsListFrame extends TopFrame
{
	private String module = "TsListFrame";
	private JPanel contentPane;
	private JPanel tsGroupTab;
	private JPanel tsGroupListTab;
	private TsDbGrpEditorTabbedPane tsGroupListTabbedPane;
	private TsListPanel tsListPanel;
	private String frameTitle;
	private String listTabLabel;
	private TimeSeriesDb theDb = null;

	
	/**
	 * Construct the frame
	 */
	public TsListFrame(TimeSeriesDb theDb)
	{
		this.theDb = theDb;
		ResourceBundle groupResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/groupedit",
			DecodesSettings.instance().language);
		ResourceBundle genericResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic",
			DecodesSettings.instance().language);

		frameTitle = "Time Series List";
		listTabLabel = genericResources.getString("list");

		try
		{
			jbInit();
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		trackChanges("TsListFrame");
	}
	
	/**
	 * Returns the time series DB object.
	 */
	public TimeSeriesDb getTsDb()
	{
		return theDb;
	}

	/** @return the tabbed pane for TS Groups */
	@Override
	public TsDbGrpEditorTabbedPane getTsGroupsListTabbedPane()
	{
		return tsGroupListTabbedPane;
	}

	/** @return the TS Groups List Panel. */
	@Override
	public TsListPanel getTsGroupsListPanel()
	{
		return tsListPanel;
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
		tsListPanel = new TsListPanel(this, theDb);

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
		tsGroupListTab.add(tsListPanel, BorderLayout.CENTER);
	}
}
