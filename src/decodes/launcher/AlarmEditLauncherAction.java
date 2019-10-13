/**
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. No warranty is provided or implied 
 * other than specific contractual terms between COVE and the U.S. Government
 * 
 * Copyright 2019 U.S. Government.
 *
 * $Log$
 */
package decodes.launcher;

import ilex.util.EnvExpander;
import ilex.util.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import decodes.tsdb.alarm.editor.AlarmEditor;

public class AlarmEditLauncherAction
	extends LauncherAction
{
	public AlarmEditLauncherAction()
	{
		super("Alarm Editor", 
			new ImageIcon(EnvExpander.expand("$DCSTOOL_HOME/icons/alarms48x48.gif")));
	}
	
	@Override
	protected JFrame launchFrame()
	{
		AlarmEditor alarmEdit = new AlarmEditor();
		
		try
		{
			alarmEdit.setExitOnClose(false);
			alarmEdit.runApp();
			return alarmEdit.getFrame();
		}
		catch (Exception ex)
		{
			String msg = "Cannot run " + this.buttonLabel + ": " + ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		return null;
	}

}
