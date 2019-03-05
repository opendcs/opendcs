/**
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.2  2017/05/18 12:29:00  mmaloney
 * Code cleanup. Remove System.out debugs.
 *
 * Revision 1.1  2017/05/17 20:36:57  mmaloney
 * First working version.
 *
 */
package decodes.tsdb.alarm.editor;

import ilex.util.LoadResourceBundle;

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
import decodes.tsdb.alarm.AlarmGroup;
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
	
	ResourceBundle genericLabels = null;
	ResourceBundle eventmonLabels = null;
	
	private String timeFormat = "yyyy/MM/dd-HH:mm:ss";
	private SimpleDateFormat sdf = new SimpleDateFormat(timeFormat);
	
	AlarmListPanel listPanel;
	
	/**
	 * Constructor
	 */
	public AlarmEditFrame(AlarmEditor parent)
	{
		sdf.setTimeZone(TimeZone.getTimeZone(DecodesSettings.instance().guiTimeZone));
		DecodesSettings settings = DecodesSettings.instance();
		genericLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/generic", settings.language);
		eventmonLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/eventmon", settings.language);
		DacqEvent.setTimeFormat(timeFormat, DecodesSettings.instance().guiTimeZone);
		guiInit();
		pack();
		trackChanges("EventMonitorFrame");
	}
	
	private void guiInit()
	{
		this.setTitle(eventmonLabels.getString("alarmEdit.frameTitle"));
	
		JPanel mainPanel = new JPanel(new BorderLayout());
		this.setContentPane(mainPanel);
		
		listPanel = new AlarmListPanel(this);
		listPanel.refreshPressed();
		mainTab.addTab(eventmonLabels.getString("alarmListTab"), null, listPanel, null);
		mainPanel.add(mainTab, BorderLayout.CENTER);
	}

	public void editAlarmGroup(AlarmGroup alarmGroup)
	{
		// if this alarm group is already being edited, make that tab active
		// else create a new AlarmEditDefPanel for the passed group and add it to the tabbed pane.
		for(int idx = 0; idx < mainTab.getComponentCount(); idx++)
		{
			Component c = mainTab.getComponentAt(idx);
			if (c instanceof AlarmEditPanel)
			{
				AlarmEditPanel aep = (AlarmEditPanel)c;
				if (aep.getEditedGroup() == alarmGroup)
				{
					mainTab.setSelectedComponent(c);
					return;
				}
			}
		}
		AlarmEditPanel editPanel = new AlarmEditPanel(this);
		editPanel.setData(alarmGroup);
		mainTab.addTab(alarmGroup.getName(), null, editPanel, null);
		mainTab.setSelectedComponent(editPanel);
	}
	
	public void closeEditPanel(AlarmEditPanel panel)
	{
		mainTab.remove(panel);
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
		for(int idx = 0; idx < mainTab.getComponentCount(); idx++)
		{
			Component c = mainTab.getComponentAt(idx);
			if (c == p)
			{
				mainTab.setTitleAt(idx, title);
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
		for(int idx = 0; idx < mainTab.getComponentCount(); idx++)
		{
			Component c = mainTab.getComponentAt(idx);
			if (c instanceof AlarmEditPanel)
			{
				AlarmEditPanel aep = (AlarmEditPanel)c;
				if (aep.getEditedGroup() == grp)
					return true;
			}
		}

		return false;
	}
}

