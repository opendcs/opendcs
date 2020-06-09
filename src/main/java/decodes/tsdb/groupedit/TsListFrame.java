/*
 * $Id:
 * 
 * $Log:
 * 
 */
package decodes.tsdb.groupedit;

import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import opendcs.opentsdb.OpenTsdbSettings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import decodes.gui.PropertiesEditDialog;
import decodes.gui.TopFrame;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
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
	private JPanel tsTab;
	private JPanel tsListTab;
	private TsDbGrpEditorTabbedPane tsListTabbedPane;
	private TsListPanel tsListPanel;
	private String frameTitle;
	private String listTabLabel;
	private TimeSeriesDb theDb = null;
	private ResourceBundle groupResources;
	private static boolean warnedAboutProps = false;

	
	/**
	 * Construct the frame
	 */
	public TsListFrame(TimeSeriesDb theDb)
	{
		this.theDb = theDb;
		groupResources = LoadResourceBundle.getLabelDescriptions(
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

	/** @return the tabbed pane for TS List */
	@Override
	public TsDbGrpEditorTabbedPane getTsGroupsListTabbedPane()
	{
		return tsListTabbedPane;
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
		tsTab = new JPanel();
		tsListTab = new JPanel();
		tsListTabbedPane = new TsDbGrpEditorTabbedPane();
		tsListPanel = new TsListPanel(this, theDb);

		//Set up the frame dimension
		this.setSize(new Dimension(863, 768));//763, 803 763, 760
		this.setTitle(frameTitle);   
		contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(tsTab, BorderLayout.CENTER);
		tsTab.setLayout(new BorderLayout());
		tsTab.add(tsListTabbedPane, BorderLayout.CENTER);
		tsListTab.setLayout(new BorderLayout());
		tsListTabbedPane.add(tsListTab, listTabLabel);
		tsListTab.add(tsListPanel, BorderLayout.CENTER);
		
		JButton tsdbPropsButton = 
			new JButton(groupResources.getString("TsdbListPanel.TsdbProperties"));
		tsdbPropsButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					tsdbPropsPressed();
				}
			});
		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
		p.add(tsdbPropsButton);
		tsTab.add(p, BorderLayout.NORTH);
	}

	protected void tsdbPropsPressed()
	{
		if (!warnedAboutProps)
		{
			JOptionPane.showMessageDialog(this, "Changing TSDB Properties will require restart "
				+ "of any background processes using these properties.");
		}
		Properties props = OpenTsdbSettings.instance().getPropertiesSet();
		PropertiesEditDialog dlg = new PropertiesEditDialog(
			groupResources.getString("TsdbListPanel.TsdbProperties"), props);
		dlg.setPropertiesOwner(OpenTsdbSettings.instance());
		launchDialog(dlg);
		if (dlg.isOkPressed())
		{
			OpenTsdbSettings.instance().setFromProperties(props);
			try
			{
				theDb.writeTsdbProperties(props);
			}
			catch (DbIoException ex)
			{
				showError("Error saving TSDB Properties: " + ex);
			}
		}
	}
	
	public void removeEditPane(TsSpecEditPanel panel)
	{
		tsListTabbedPane.remove(panel);
		tsListTabbedPane.setSelectedIndex(0);
	}

	public void addEditTab(TsSpecEditPanel editPanel, String tabName)
	{
		tsListTabbedPane.add(editPanel, tabName);
		tsListTabbedPane.setSelectedComponent(editPanel);
	}
	
	/**
	 * Called when Open is selected before constructing a new tab. If an edit
	 * tab for this TSID already exists, it is made active and true is returned.
	 * Otherwise, false is returned.
	 * @param tsid the selected tsid
	 * @return true if an edit tab already exists, false if not.
	 */
	
	public boolean makeEditPaneActive(TimeSeriesIdentifier tsid)
	{
		// Skip tab 0, which is the list tab
		for(int idx = 1; idx < tsListTabbedPane.getTabCount(); idx++)
		{
			Component c = tsListTabbedPane.getComponentAt(idx);
			if (!(c instanceof TsSpecEditPanel))
				System.out.println("wrong class in tabbed pane");
			else
			{
				TsSpecEditPanel editPanel = (TsSpecEditPanel)c;
				if (tsid == editPanel.getTsid())
				{
					tsListTabbedPane.setSelectedIndex(idx);
					return true;
				}
			}
		}
		return false;
	}
	
	public void setTabLabel(TsSpecEditPanel editPanel, String tabTitle)
	{
		// Skip tab 0, which is the list tab
		for(int idx = 1; idx < tsListTabbedPane.getTabCount(); idx++)
		{
			Component c = tsListTabbedPane.getComponentAt(idx);
			if (c == editPanel)
			{
				tsListTabbedPane.setTitleAt(idx, tabTitle);
				return;
			}
		}
	}
}
