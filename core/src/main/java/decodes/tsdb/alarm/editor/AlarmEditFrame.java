/**
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.2  2019/06/10 19:27:49  mmaloney
 * Added Screenings to Alarm Editor
 *
 * Revision 1.1  2019/03/05 14:52:59  mmaloney
 * Checked in partial implementation of Alarm classes.
 *
 * Revision 1.2  2017/05/18 12:29:00  mmaloney
 * Code cleanup. Remove System.out debugs.
 *
 * Revision 1.1  2017/05/17 20:36:57  mmaloney
 * First working version.
 *
 */
package decodes.tsdb.alarm.editor;

import ilex.util.LoadResourceBundle;
import ilex.util.TextUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import decodes.gui.TopFrame;
import decodes.polling.DacqEvent;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.AlarmScreening;
import decodes.util.DecodesSettings;


/**
 * Main frame for alarm editor GUI
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
@SuppressWarnings("serial")
public class AlarmEditFrame 
	extends TopFrame
{
	private JTabbedPane mainTab = new JTabbedPane();
	private JTabbedPane emailGroupsTab = new JTabbedPane();
	private JTabbedPane screeningsTab = new JTabbedPane();
	
	ResourceBundle genericLabels = null;
	ResourceBundle eventmonLabels = null;
	
	private String timeFormat = "yyyy/MM/dd-HH:mm:ss";
	private SimpleDateFormat sdf = new SimpleDateFormat(timeFormat);
	
	GroupListPanel groupListPanel;
	ScreeningListPanel screeningListPanel;
	AlarmEditor parentTsdbApp = null;
	
	/**
	 * Constructor
	 */
	public AlarmEditFrame(AlarmEditor parentFrame)
	{
		this.parentTsdbApp = parentFrame;
		sdf.setTimeZone(TimeZone.getTimeZone(DecodesSettings.instance().guiTimeZone));
		DecodesSettings settings = DecodesSettings.instance();
		genericLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/generic", settings.language);
		eventmonLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/eventmon", settings.language);
		DacqEvent.setTimeFormat(timeFormat, DecodesSettings.instance().guiTimeZone);
		guiInit();
		pack();
		trackChanges("EventMonitorFrame");
	}
	
	public TimeSeriesDb getTsDb()
	{
		if (parentTsdbApp == null)
			return null;
		return parentTsdbApp.getTsdb();
	}
	
	private void guiInit()
	{
		this.setTitle(eventmonLabels.getString("alarmEdit.frameTitle"));
	
		JPanel mainPanel = new JPanel(new BorderLayout());
		this.setContentPane(mainPanel);
		mainPanel.add(mainTab, BorderLayout.CENTER);
		
		groupListPanel = new GroupListPanel(this);
		emailGroupsTab.addTab(eventmonLabels.getString("alarmListTab"), null, groupListPanel, null);
		mainTab.addTab("Email Groups", null, emailGroupsTab, null);

		screeningListPanel = new ScreeningListPanel(this);
		screeningsTab.addTab("List", screeningListPanel);
		mainTab.addTab("Screenings", screeningsTab);
		
		groupListPanel.refreshPressed();
		screeningListPanel.refreshPressed();
	}

	public void editAlarmGroup(AlarmGroup alarmGroup)
	{
		// if this alarm group is already being edited, make that tab active
		// else create a new AlarmEditDefPanel for the passed group and add it to the tabbed pane.
		for(int idx = 0; idx < emailGroupsTab.getComponentCount(); idx++)
		{
			Component c = emailGroupsTab.getComponentAt(idx);
			if (c instanceof AlarmEditPanel)
			{
				AlarmEditPanel aep = (AlarmEditPanel)c;
				if (aep.getEditedGroup() == alarmGroup)
				{
					emailGroupsTab.setSelectedComponent(c);
					return;
				}
			}
		}
		AlarmEditPanel editPanel = new AlarmEditPanel(this);
		editPanel.setData(alarmGroup);
		emailGroupsTab.addTab(alarmGroup.getName(), null, editPanel, null);
		emailGroupsTab.setSelectedComponent(editPanel);
	}
	
	public void closeEditPanel(AlarmEditPanel panel)
	{
		emailGroupsTab.remove(panel);
	}
	
	public void cleanupBeforeExit()
	{
	}
	
	public void launch( int x, int y, int w, int h )
	{
		setBounds(x,y,w,h);
		setVisible(true);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		final AlarmEditFrame myframe = this;
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					myframe.cleanupBeforeExit();
					if (exitOnClose)
						System.exit(0);
				}
			});
	}

	public void setExitOnClose(boolean exitOnClose)
	{
		this.exitOnClose = exitOnClose;
	}

	public void setTitleFor(AlarmEditPanel p, String title)
	{
		for(int idx = 0; idx < emailGroupsTab.getComponentCount(); idx++)
		{
			Component c = emailGroupsTab.getComponentAt(idx);
			if (c == p)
			{
				emailGroupsTab.setTitleAt(idx, title);
				return;
			}
		}
	}
	
	/**
	 * Return true if the passed group is currently being edited.
	 * @param grp
	 * @return
	 */
	public boolean isBeingEdited(AlarmGroup grp)
	{
		for(int idx = 0; idx < emailGroupsTab.getComponentCount(); idx++)
		{
			Component c = emailGroupsTab.getComponentAt(idx);
			if (c instanceof AlarmEditPanel)
			{
				AlarmEditPanel aep = (AlarmEditPanel)c;
				if (aep.getEditedGroup() == grp)
					return true;
			}
		}

		return false;
	}

	public boolean isBeingEdited(AlarmScreening scrn)
	{
		for(int idx = 0; idx < screeningsTab.getTabCount(); idx++)
		{
			Component x = screeningsTab.getComponentAt(idx);
			if (!(x instanceof ScreeningEditPanel))
				continue;
			ScreeningEditPanel sep = (ScreeningEditPanel)x;
			if (sep.getScreening() == null)
				continue;
			if (TextUtil.strEqual(scrn.getScreeningName(), sep.getScreening().getScreeningName()))
				return true;
		}
		return false;
	}

	public void editAlarmScreening(AlarmScreening scrn)
	{
		for(int idx = 0; idx < screeningsTab.getTabCount(); idx++)
		{
			Component x = screeningsTab.getComponentAt(idx);
			if (!(x instanceof ScreeningEditPanel))
				continue;
			ScreeningEditPanel sep = (ScreeningEditPanel)x;
			if (sep.getScreening() == null)
				continue;
			if (TextUtil.strEqual(scrn.getScreeningName(), sep.getScreening().getScreeningName()))
			{
				screeningsTab.setSelectedComponent(sep);
				return;
			}
		}
		ScreeningEditPanel editTab = new ScreeningEditPanel(this);
		editTab.setScreening(scrn);
		screeningsTab.add(editTab, scrn.getScreeningName());
		screeningsTab.setSelectedComponent(editTab);
	}

	public void closeScreening(ScreeningEditPanel editPanel)
	{
		screeningsTab.remove(editPanel);
		screeningsTab.setSelectedIndex(0);
	}
	
	public void setTabLabel(ScreeningEditPanel editPanel, String newName)
	{
		for(int idx = 0; idx < screeningsTab.getTabCount(); idx++)
		{
			Component x = screeningsTab.getComponentAt(idx);
			if (!(x instanceof ScreeningEditPanel))
				continue;
			ScreeningEditPanel sep = (ScreeningEditPanel)x;
			if (editPanel == sep)
			{
				screeningsTab.setTitleAt(idx, newName);
				return;
			}
		}
	}
}

